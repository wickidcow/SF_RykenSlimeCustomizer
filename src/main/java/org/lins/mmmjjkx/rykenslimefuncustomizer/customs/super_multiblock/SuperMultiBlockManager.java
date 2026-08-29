/*
 * RykenSlimefunCustomizer
 * Copyright (C) 2026 lijinhong11(mmmjjjkx) and balugaq
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.lins.mmmjjkx.rykenslimefuncustomizer.customs.super_multiblock;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import lombok.Getter;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lins.mmmjjkx.rykenslimefuncustomizer.RykenSlimefunCustomizer;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.CustomSuperMultiBlockMachine;
import org.lins.mmmjjkx.rykenslimefuncustomizer.libraries.colors.CMIChatColor;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.Debug;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.Keys;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class SuperMultiBlockManager {
    public static SuperMultiBlockManager getInstance() {
        return RykenSlimefunCustomizer.INSTANCE.getSuperMultiBlockManager();
    }

    public static final NamespacedKey RSC_KEY = Keys.newKey("rsc_projectile");

    @Getter
    private static final Map<Location, SuperMultiBlock> coreStorage = new ConcurrentHashMap<>();
    @Getter
    private static final Map<Location, SuperMultiBlock> monitoringLocations = new ConcurrentHashMap<>();
    @Getter
    private static final Set<Location> correctLocations = new CopyOnWriteArraySet<>();
    @Getter
    private static final Map<Location, Display> projectiles = new ConcurrentHashMap<>();
    @Getter
    public static final float DEFAULT_DISPLAY_SCALE = 0.8f;
    @Getter
    public static final Transformation DEFAULT_TRANSFORMATION = getTransformation(DEFAULT_DISPLAY_SCALE);
    // 0.0f is a hack, which means invisible.
    @Getter
    public static final Transformation INVISIBLE_TRANSFORMATION = getTransformation(0.0f);
//    private final Map<Location, Interaction> interactions = new ConcurrentHashMap<>();
    @Getter
    private static @Nullable Field MENU_FIELD = null;

    public static final Queue<Entry> dirtyLocations = new ConcurrentLinkedQueue<>();

    public record Entry(Location location, boolean autoSwitchLayer) {
    }

    public static Entry entry(Location location, boolean autoSwitchLayer) {
        return new Entry(location, autoSwitchLayer);
    }

    static {
        try {
            MENU_FIELD = SlimefunBlockData.class.getDeclaredField("menu");
            MENU_FIELD.setAccessible(true);
        } catch (NoSuchFieldException e) {
            Debug.error("Failed to get menu field from SlimefunBlockData.class.", e);
        }

        Bukkit.getScheduler().runTaskTimerAsynchronously(RykenSlimefunCustomizer.INSTANCE, () -> {
            while (!dirtyLocations.isEmpty()) {
                var entry = dirtyLocations.poll();
                if (entry != null) {
                    var location = entry.location();
                    getInstance().processDirty(location, entry.autoSwitchLayer());
                }
            }
        }, 1L, 1L);
    }

    @Getter
    private static final Set<Location> menuModified = ConcurrentHashMap.newKeySet();

    public SuperMultiBlockManager() {}

    public boolean startSuperMultiBlock(@NonNull SuperMultiBlock superMultiBlock) {
        Set<Location> locations = superMultiBlock.getLocations();
        if (locations.stream().anyMatch(monitoringLocations::containsKey)) {
            // don't block the incoming SuperMultiBlock
            return false;
        }

        // start monitoring the locations
        for (Location location : locations) {
            monitoringLocations.put(location, superMultiBlock);
            correctLocations.remove(location);
        }
        coreStorage.put(superMultiBlock.getCoreLocation(), superMultiBlock);
        checkProjectiles(superMultiBlock);

        // generate cache
        superMultiBlock.generateCache();
        if (!superMultiBlock.isFullyFormedCached()) {
            if (superMultiBlock.getMachine().isDisplayProjectiles()) {
                addProjectiles(superMultiBlock);
            } else {
                removeProjectiles(superMultiBlock);
            }
        }
        return true;
    }

    public void removeProjectiles(SuperMultiBlock smb) {
        Bukkit.getScheduler().runTask(RykenSlimefunCustomizer.INSTANCE, () -> {
            for (Location location : smb.getLocations()) {
                removeProjectile(location);
            }
        });
    }

    public void checkProjectiles(SuperMultiBlock superMultiBlock) {
        Bukkit.getScheduler().runTask(RykenSlimefunCustomizer.INSTANCE, () -> {
            for (Location location : superMultiBlock.getLocations()) {
                for (Entity entity : location.getWorld().getNearbyEntities(location, 0.1, 0.1, 0.1)) {
                    if (!entity.getPersistentDataContainer().has(RSC_KEY, PersistentDataType.BOOLEAN)) {
                        continue;
                    }
                    if (entity.getType() == EntityType.BLOCK_DISPLAY || entity.getType() == EntityType.ITEM_DISPLAY) {
                        if (superMultiBlock.getMachine().isDisplayProjectiles()) {
                            projectiles.put(location, (Display) entity);
                        } else {
                            entity.remove();
                            projectiles.remove(location);
                        }
                    }
//                if (entity.getType() == EntityType.INTERACTION) {
//                    if (superMultiBlock.getMachine().isDisplayProjectiles()) {
//                        interactions.put(location, (Interaction) entity);
//                    } else {
//                        entity.remove();
//                        interactions.remove(location);
//                    }
//                }
                }
            }
        });
    }

    public void destroySuperMultiBlock(@Nullable SuperMultiBlock superMultiBlock) {
        if (superMultiBlock == null) return;
        Set<Location> locations = superMultiBlock.getLocations();
        for (Location location : locations) {
            if (monitoringLocations.get(location) == superMultiBlock) {
                monitoringLocations.remove(location);
                correctLocations.remove(location);
            }
        }

        removeProjectiles(superMultiBlock);
        CustomSuperMultiBlockMachine.firstTicks.remove(superMultiBlock.getCoreLocation());
        coreStorage.remove(superMultiBlock.getCoreLocation());
        superMultiBlock.onDestroy();
    }

    public void markDirty(@NonNull Location location, boolean autoSwitchLayer) {
        dirtyLocations.add(entry(location, autoSwitchLayer));
    }

    private void processDirty(@NonNull Location location, boolean autoSwitchLayer) {
        SuperMultiBlock superMultiBlock = monitoringLocations.get(location);
        if (superMultiBlock == null) {
            return;
        }

        boolean isFormedBefore = superMultiBlock.isFullyFormedCached();
    
        if (superMultiBlock.isFormed(location)) {
            correctLocations.add(location);
            var p = projectiles.get(location);
            if (p != null) p.setGlowing(false);
        } else {
            correctLocations.remove(location);
            var p = projectiles.get(location);
            if (p != null) p.setGlowing(true);
        }

        boolean isFormedNow = superMultiBlock.isFullyFormedCached();
        if (!isFormedNow && autoSwitchLayer) {
            // try switch layer
            int layerIndex = superMultiBlock.getMachine().getCurrentLayerIndex(superMultiBlock);
            if (layerIndex == CustomSuperMultiBlockMachine.DISPLAY_ALL) return;
            int layer = superMultiBlock.getLayers()[layerIndex];
            if (superMultiBlock.isLayerFormed(layer)) {
                superMultiBlock.formedLayer(layer);
                if (switchToUnformedLayer(superMultiBlock)) {


                    if (superMultiBlock.getMachine().isDefaultNotice()) {
                        findNearbyPlayers(location, 10, p -> {
                            p.sendMessage(CMIChatColor.colorize("&cformed successfully y=" + layer));
                        });
                    }
                }
            }
        }

        if (isFormedBefore && !isFormedNow) {
            superMultiBlock.onUnformed(location);
            destroySuperMultiBlock(superMultiBlock);
        }

        if (!isFormedBefore && isFormedNow) {
            superMultiBlock.onFormed(location);
            if (superMultiBlock.getMachine().isDisplayProjectiles()) {
                removeProjectiles(superMultiBlock);
            }
        }
    }

    public static void findNearbyPlayers(@NonNull Location location, double radius, Consumer<Player> consumer) {
        Bukkit.getScheduler().runTask(RykenSlimefunCustomizer.INSTANCE, () -> {
            location.getWorld().getNearbyPlayers(location, radius).forEach(consumer);
        });
    }

    private boolean switchToUnformedLayer(SuperMultiBlock instance) {
        for (int i = 0; i < instance.layerCount(); i++) {
            if (!instance.isLayerFormed(instance.getLayers()[i])) {
                int finalI = i;
                Bukkit.getScheduler().runTask(RykenSlimefunCustomizer.INSTANCE, () -> {
                    int layer = instance.getLayers()[finalI];
                    instance.autoSwitchedNewLayer(layer);
                    instance.setLayerIndex(finalI);
                    hideEntities(selectEntities(instance, instance.getLayers()[instance.getCurrentLayerIndex()]));
                    showEntities(selectEntities(instance, layer));
                });
                return true;
            }
        }
        return false;
    }

    @Nullable
    public SuperMultiBlock getSuperMultiBlock(@NonNull Location location) {
        return monitoringLocations.get(location);
    }

    public void addProjectiles(@NonNull SuperMultiBlock instance) {
        Bukkit.getScheduler().runTask(RykenSlimefunCustomizer.INSTANCE, () -> {
            Set<Location> locations = instance.getLocations();
            for (Location location : locations) {
                MultiBlockPart part = instance.getPart(location);
                if (part == null) {
                    continue;
                }

                DisplayDescriptor descriptor = part.getDisplayDescriptor(instance, location);
                boolean glow = !instance.isFormed(location);
                if (descriptor != null) {
                    addProjectile(location, descriptor, !instance.getMachine().isAllowSwitchDisplayLayer(), glow);
                } else {
                    Debug.error("Unable tomultiblock: machine:" + instance.getMachine().getId() + "RSC message" + location);
                }
            }

            showEntities(selectEntities(instance, instance.getCoreLocation().getBlockY()));
            StorageCacheUtils.setData(instance.getCoreLocation(), "LayerIndex", "" + instance.getCoreLayerIndex());
        });
    }

    public void addProjectile(@NonNull Location location, @NonNull DisplayDescriptor descriptor, boolean visible, boolean glow) {
        for (Entity entity : location.getWorld().getNearbyEntities(location, 0.1, 0.1, 0.1)) {
            if (entity.getType() == EntityType.BLOCK_DISPLAY || entity.getType() == EntityType.ITEM_DISPLAY) {
                if (entity.getPersistentDataContainer().has(RSC_KEY, PersistentDataType.BOOLEAN)) {
                    projectiles.put(location, (Display) entity);
                    return;
                }
            }
//            if (entity.getType() == EntityType.INTERACTION) {
//                if (entity.getPersistentDataContainer().has(RSC_KEY, PersistentDataType.BOOLEAN)) {
//                    interactions.put(location, (Interaction) entity);
//                    return;
//                }
//            }
        }
        Display display = descriptor.createDisplay(location);
        display.setTransformation(visible ? DEFAULT_TRANSFORMATION : INVISIBLE_TRANSFORMATION);
        display.getPersistentDataContainer().set(RSC_KEY, PersistentDataType.BOOLEAN, true);
        display.customName(Component.empty());
        display.setCustomNameVisible(false);
        display.setGlowing(glow);
        display.setBrightness(new Display.Brightness(15, 15));
        projectiles.put(location, display);
//        Interaction interaction = (Interaction) location.getWorld().spawnEntity(location.clone().add(0.5, 0.5, 0.5), EntityType.INTERACTION);
//        interaction.setInteractionHeight(scale);
//        interaction.setInteractionWidth(scale);
//        interaction.setResponsive(true);
//        interaction.getPersistentDataContainer().set(RSC_KEY, PersistentDataType.BOOLEAN, true);
//        interactions.put(location, interaction);
    }

    public static Transformation getTransformation(float scale) {
        float offset = (1.0f - scale) / 2f;
        return new Transformation(new Vector3f(offset, offset, offset), new AxisAngle4f(0, 0, 0, 0), new Vector3f(scale, scale, scale), new AxisAngle4f(0, 0, 0, 0));
    }

    public void removeProjectile(@NonNull Location location) {
        Display display = projectiles.remove(location);
        if (display != null && !display.isDead() && display.isValid()) {
            display.remove();
        }
//        Interaction interaction = interactions.remove(location);
//        if (interaction != null && !interaction.isDead() && interaction.isValid()) {
//            interaction.remove();
//        }
    }

    public void onPlayerInteract(PlayerInteractEvent event, Block b) {
        SuperMultiBlock superMultiBlock = monitoringLocations.get(b.getLocation());
        if (superMultiBlock != null) {
            superMultiBlock.onInteract(event, false);
        }

        SuperMultiBlock smb = getCoreStorage().get(b.getLocation());
        if (smb != null) {
            smb.onInteract(event, true);
        }
    }

    public Set<Display> selectEntities(@NonNull SuperMultiBlock instance) {
        return instance.getLocations().stream().map(projectiles::get).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    public Set<Display> selectEntities(@NonNull SuperMultiBlock instance, int layer) {
        return instance.getLocations().stream().filter(l -> l.getBlockY() == layer).map(projectiles::get).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    public void showEntities(Set<Display> entities) {
        entities.forEach(entity -> entity.setTransformation(DEFAULT_TRANSFORMATION));
    }

    public void hideEntities(Set<Display> entities) {
        entities.forEach(entity -> entity.setTransformation(INVISIBLE_TRANSFORMATION));
    }

    public void updateLayer(@NonNull SuperMultiBlock instance, int oldLayer, int newLayer) {
        Bukkit.getScheduler().runTask(RykenSlimefunCustomizer.INSTANCE, () -> {
            showEntities(selectEntities(instance, newLayer));
            hideEntities(selectEntities(instance, oldLayer));
        });
    }

    public static boolean canTick(Location location) {
        SuperMultiBlock smb = getInstance().getSuperMultiBlock(location);
        if (smb == null) return true;
        var part = smb.getPart(location);
        if (part == null) return true;
        if (!part.isOfPart(smb, location)) return true;
        return smb.isFullyFormedCached();
    }

    public void tryModifyMenu(Location location) {
        SuperMultiBlock smb = SuperMultiBlockManager.getInstance().getSuperMultiBlock(location);
        if (smb == null) return;
        if (!menuModified.add(location)) return;
        Location coreLocation = smb.getCoreLocation();
        var data = StorageCacheUtils.getBlock(coreLocation);
        if (data != null) {
            try {
                MENU_FIELD.set(data, new BlockMenu(data.getBlockMenu().getPreset(), coreLocation, data.getBlockMenu().getContents()) {
                    @Override
                    public void open(Player... players) {
                        if (!smb.getMachine().isNoMenuWhenNotFormed() || smb.isFullyFormedCached()) {
                            if (smb.getMachine().isNoMenu() && location.equals(coreLocation)) {
                                return;
                            }

                            super.open(players);
                        }
                    }
                });
            } catch (IllegalAccessException e2) {
                Debug.error("Failed to set menu field.", e2);
            }
        }
    }
}