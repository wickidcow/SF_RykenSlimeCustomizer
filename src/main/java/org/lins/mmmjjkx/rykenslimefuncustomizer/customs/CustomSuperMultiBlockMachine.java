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
package org.lins.mmmjjkx.rykenslimefuncustomizer.customs;

import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockPlaceHandler;
import io.github.thebusybiscuit.slimefun4.implementation.handlers.SimpleBlockBreakHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.lins.mmmjjkx.rykenslimefuncustomizer.RykenSlimefunCustomizer;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.super_multiblock.Asynchronized;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.super_multiblock.HorizonDirection;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.super_multiblock.SuperMultiBlock;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.super_multiblock.SuperMultiBlockDefinition;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.super_multiblock.SuperMultiBlockManager;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.super_multiblock.TickContextSuperMultiBlockMachine;
import org.lins.mmmjjkx.rykenslimefuncustomizer.libraries.colors.CMIChatColor;
import org.lins.mmmjjkx.rykenslimefuncustomizer.readers.YamlReader;
import org.lins.mmmjjkx.rykenslimefuncustomizer.script.ScriptEval;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JS:
 * onTick(block, machine, ctx)
 * onFormed(partLocation, machine)
 * onUnformed(partLocation, machine)
 * onDestroy(machine)
 * onInteract(event, machine)
 * isOfPart(location, multiblock)
 * cannotStartSuperMultiBlock(location, machine)
 * onClickedPartBlock(event, machine)
 * onClickedPartBlockNotFormed(event, machine)
 * autoSwitchedDisplayLayer(layer, machine)
 * switchDisplayLayer(layerIndex, machine)
 * onClickedPartBlockNotFormed(event, machine)
 * formedLayer(layer, machine)
 * -
 * ctx = TickContext
 * event = PlayerInteractEvent
 * machine = CustomSuperMultiBlockMachine
 * multiblock = SuperMultiBlock
 */
@SuppressWarnings("AccessStaticViaInstance")
@Slf4j
@Getter
@NullMarked
public class CustomSuperMultiBlockMachine extends AdvancedCustomMachine implements Asynchronized {
    public static final SuperMultiBlockManager instance = SuperMultiBlockManager.getInstance();
    public static final int DISPLAY_ALL = -999;
    private final SuperMultiBlockDefinition definition;
    private final boolean displayProjectiles;
    private final boolean checkFormed;
    private final boolean openMenuWhenClickedParts;
    private final boolean noMenu;
    private final boolean noMenuWhenNotFormed;
    private final boolean allowSwitchDisplayLayer;
    private final boolean defaultNotice;
    private final @Nullable String redirectMenu;
    @Getter
    private static final Map<Location, HorizonDirection> horizonDirections = new HashMap<>();
    private static final Map<Location, Integer> ticked = new ConcurrentHashMap<>();

    public CustomSuperMultiBlockMachine(
            YamlReader.BaseResult base,
            int[] input,
            int[] output,
            int energyPerCraft,
            int capacity,
            int speed,
            @Nullable ScriptEval eval,
            SuperMultiBlockDefinition definition,
            boolean displayProjectiles,
            boolean checkFormed,
            boolean openMenuWhenClickedParts,
            boolean noMenu,
            boolean noMenuWhenNotFormed,
            boolean allowSwitchDisplayLayer,
            boolean defaultNotice,
            @Nullable String redirectMenu) {
        super(base, input, output, energyPerCraft, capacity, speed, eval);

        this.definition = definition;
        this.displayProjectiles = displayProjectiles;
        this.checkFormed = checkFormed;
        this.openMenuWhenClickedParts = openMenuWhenClickedParts;
        this.noMenu = noMenu;
        this.noMenuWhenNotFormed = noMenuWhenNotFormed;
        this.allowSwitchDisplayLayer = allowSwitchDisplayLayer;
        this.defaultNotice = defaultNotice;
        this.redirectMenu = redirectMenu;

        addItemHandler(new BlockPlaceHandler(false) {
            @Override
            public void onPlayerPlace(BlockPlaceEvent e) {
                setHorizonDirection(e.getBlock().getLocation(), HorizonDirection.getFace(e.getPlayer()));
                runAsyncLater(() -> instance.tryModifyMenu(e.getBlock().getLocation()));
            }
        });
    }

    @Override
    protected BlockBreakHandler onBlockBreak() {
        return new SimpleBlockBreakHandler() {
            public void onBlockBreak(Block b) {
                var loc = b.getLocation();
                instance.destroySuperMultiBlock(instance.getCoreStorage().get(loc));
                BlockMenu inv = StorageCacheUtils.getMenu(loc);
                if (inv != null) {
                    inv.dropItems(loc, getInputSlots());
                    inv.dropItems(loc, getOutputSlots());
                }

                getTicker().getAdvancedMachineProcessor().endOperation(b);
            }
        };
    }

    public static final Set<Location> firstTicks = new HashSet<>();

    public static void setHorizonDirection(Location location, HorizonDirection direction) {
        horizonDirections.put(location, direction);
        StorageCacheUtils.setData(location, "HorizonDirection", direction.name());
    }

    public static HorizonDirection getHorizonDirection(Location location) {
        if (!horizonDirections.containsKey(location)) {
            String s = StorageCacheUtils.getData(location, "HorizonDirection");
            if (s == null) {
                setHorizonDirection(location, HorizonDirection.NORTH);
                return HorizonDirection.NORTH;
            }
            try {
                var v = HorizonDirection.valueOf(s);
                setHorizonDirection(location, v);
                return v;
            } catch (Exception e) {
                setHorizonDirection(location, HorizonDirection.NORTH);
                return HorizonDirection.NORTH;
            }
        }

        return horizonDirections.get(location);
    }

    @Override
    protected void tick(Block b) {
        var loc = b.getLocation();
        ticked.put(loc, ticked.getOrDefault(loc, 0) + 1);
        if (ticked.get(loc) % 500 == 0) {
            // force check self each 500 sft.
            var smb = instance.getCoreStorage().get(loc);
            if (smb != null) {
                smb.markDirty();
            }
        }

        var ctx = new TickContextSuperMultiBlockMachine();
        evalFunction("onTick", b, this, ctx);

        if (ctx.isCheckFirstTick() && firstTicks.add(loc)) {
            runAsyncLater(() -> instance.tryModifyMenu(loc));

            var smb = new SuperMultiBlock(CustomSuperMultiBlockMachine.this, loc, getHorizonDirection(loc));
            if (!instance.startSuperMultiBlock(smb)) {
                if (defaultNotice) {
                    SuperMultiBlockManager.findNearbyPlayers(loc, 10, p -> {
                        p.sendMessage(CMIChatColor.colorize("&cmultiblock, Unable tomultiblock, . "));
                    });
                }
                evalFunction("cannotStartSuperMultiBlock", b, this);
            } else {
                if (smb.isFullyFormedCached()) {
                    onFormed(smb.getCoreLocation());
                } else {
                    if (defaultNotice) {
                        String click = noMenuWhenNotFormed ? "Right click" : "Left click";
                        SuperMultiBlockManager.findNearbyPlayers(loc, 10, p -> {
                            p.sendMessage(CMIChatColor.colorize("RSC: " + getItemName() + ". &a" + click + " Shift+" + click + " layer."));
                        });
                    }
                }
            }
        }

        if (ctx.isCallSuper()) {
            // allow multiblock recursive building check
            instance.markDirty(loc, false);
            var smb = instance.getCoreStorage().get(loc);
            if (smb != null) {
                smb.tick();
                if (checkFormed && SuperMultiBlockManager.canTick(loc)) {
                    super.tick(b);
                }
            }
        }
    }

    public void onFormed(Location partLocation) {
        if (evalFunction("onFormed", partLocation, this) == null) {
            if (defaultNotice) {
                SuperMultiBlockManager.findNearbyPlayers(partLocation, 10, p -> {
                    p.sendMessage(CMIChatColor.colorize("&aformed successfully " + getItemName()));
                });
            }
        }
    }

    public void onUnformed(Location partLocation) {
        if (evalFunction("onUnformed", partLocation, this) == null) {
            SuperMultiBlockManager.findNearbyPlayers(partLocation, 10, p -> {
                p.sendMessage(CMIChatColor.colorize("&c" + getItemName() + "&cwas broken!"));
            });
        }
    }

    public void formedLayer(int layer) {
        if (getEval() != null) {
            Bukkit.getScheduler().runTask(RykenSlimefunCustomizer.INSTANCE, () -> {
                getEval().evalFunction("formedLayer", layer, this);
            });
        }
    }

    public void onDestroy() {
        evalFunction("onDestroy", this);
    }

    public void autoSwitchedNewLayer(int layer) {
        evalFunction("autoSwitchedDisplayLayer", layer, this);
    }

    public int getCurrentLayerIndex(SuperMultiBlock instance) {
        String layerS = StorageCacheUtils.getData(instance.getCoreLocation(), "LayerIndex");
        if (layerS == null) {
            return 0;
        }
        int layer;
        try {
            layer = Integer.parseInt(layerS);
        } catch (NumberFormatException ignored) {
            layer = 0;
        }
        return layer;
    }
    
    public void setLayerIndex(SuperMultiBlock instance, int layerIndex) {
        StorageCacheUtils.setData(instance.getCoreLocation(), "LayerIndex", String.valueOf(layerIndex));
    }

    public void switchLayer(SuperMultiBlock instance, Player p, boolean down) {
        int currentLayerIndex = getCurrentLayerIndex(instance);
        if (instance.getLayers().length > 1 && (evalFunction("switchDisplayLayer", instance, currentLayerIndex) == null)) {
            // default
            if (currentLayerIndex == DISPLAY_ALL) {
                // all -> first / all -> last
                instance.hideAllEntities();
                int newLayerIndex = down ? instance.layerCount() - 1 : 0;
                setLayerIndex(instance, newLayerIndex);
                int newLayer = instance.getLayers()[newLayerIndex];
                instance.showEntities(newLayer);
                if (defaultNotice) {
                    p.sendMessage(CMIChatColor.colorize("&amultiblock layer y=" + newLayer + "RSC: " + (newLayerIndex + 1) + "/" + (instance.layerCount()) + " layer)"));
                }
                return;
            }
            if ((currentLayerIndex == instance.layerCount() - 1 && !down) || (currentLayerIndex == 0 && down)) {
                // last -> all / first -> all
                instance.showAllEntities();
                setLayerIndex(instance, DISPLAY_ALL);
                if (defaultNotice) {
                    p.sendMessage(CMIChatColor.colorize("&amultiblock layerHas layer"));
                }
                return;
            }

            int oldLayer = instance.getLayers()[currentLayerIndex];
            int newLayerIndex = currentLayerIndex + (down ? -1 : 1);
            int newLayer = instance.getLayers()[newLayerIndex];
            StorageCacheUtils.setData(instance.getCoreLocation(), "LayerIndex", "" + newLayerIndex);
            instance.updateLayer(instance, oldLayer, newLayer);
            if (defaultNotice) {
                p.sendMessage(CMIChatColor.colorize("&amultiblock layer y=" + newLayer + "RSC: " + (newLayerIndex + 1) + "/" + (instance.layerCount()) + " layer)"));
            }
        }
    }

    public void onInteract(PlayerInteractEvent event, boolean clickedCore, SuperMultiBlock instance) {
        if (evalFunction("onInteract", event, this) != null) {
            return;
        }

        Player p = event.getPlayer();
        var tp = p.getInventory().getItemInMainHand().getType();
        boolean holdingBlock = tp.isBlock() && !tp.isAir();
        if (holdingBlock && p.isSneaking()) {
            // player wants to place block, don't handle
            return;
        }

        if (allowSwitchDisplayLayer && !instance.isFullyFormedCached() && clickedCore) {
            boolean left = event.getAction().isLeftClick();
            boolean right = event.getAction().isRightClick();
            boolean shift = p.isSneaking();
            if (!holdingBlock && (!noMenuWhenNotFormed && left) || (noMenuWhenNotFormed && right)) {
                // switch display layer
                // shift = down, !shift = up
                switchLayer(instance, p, shift);
                return;
            }
        }

        if (!openMenuWhenClickedParts && !clickedCore) {
            evalFunction("onClickedPartBlock", event, this);
            return;
        }
        if (noMenuWhenNotFormed && !instance.isFullyFormedCached()) {
            evalFunction("onClickedPartBlockNotFormed", event, this);
            return;
        }

        var mo = StorageCacheUtils.getMenu(event.getClickedBlock().getLocation());
        if (mo != null) {
            mo.open(event.getPlayer());
            event.setCancelled(true);
            return;
        }

        if (redirectMenu != null) {
            // open the target machine's menu, the uniqueness had been checked in reader.
            var offset = instance.getDefinition().findFirstValue(getHorizonDirection(instance.getCoreLocation()), redirectMenu);
            if (offset != null) {
                var menu = StorageCacheUtils.getMenu(instance.getCoreLocation().clone().add(offset));
                if (menu != null) {
                    menu.open(event.getPlayer());
                    event.setCancelled(true);
                }
                return;
            }
        }

        // fallback and default logic, open the machine's menu.
        var menu = StorageCacheUtils.getMenu(instance.getCoreLocation());
        if (menu != null) {
            menu.open(event.getPlayer());
            event.setCancelled(true);
        }
    }
}