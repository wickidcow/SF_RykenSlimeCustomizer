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

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.attributes.UniversalBlock;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lins.mmmjjkx.rykenslimefuncustomizer.RykenSlimefunCustomizer;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.CustomSuperMultiBlockMachine;
import org.lins.mmmjjkx.rykenslimefuncustomizer.libraries.colors.CMIChatColor;

import java.util.Set;

@Getter
public class SuperMultiBlock extends SuperMultiBlockManager implements Asynchronized {
    private final CustomSuperMultiBlockMachine machine;
    private final Location coreLocation;
    private final int minY;
    private final int maxY;
    private final int[] layers;
    private boolean ticked = false;
    private final HorizonDirection direction;

    public int getCoreLayerIndex() {
        for (int i = 0; i < layers.length; i++) {
            if (layers[i] == getCoreLocation().getBlockY()) {
                return i;
            }
        }
        throw new AssertionError("Core location is not in the layers.");
    }

    public int getCurrentLayerIndex() {
        return getMachine().getCurrentLayerIndex(this);
    }

    public void setLayerIndex(int layerIndex) {
        getMachine().setLayerIndex(this, layerIndex);
    }

    public int minY() {
        return minY;
    }

    public int maxY() {
        return maxY;
    }

    public int layerCount() {
        return getLayers().length;
    }

    public SuperMultiBlock(@NonNull CustomSuperMultiBlockMachine machine, @NonNull Location coreLocation, @NonNull HorizonDirection direction) {
        this.machine = machine;
        this.coreLocation = coreLocation;
        this.direction = direction;

        var layers = new IntArraySet();
        int minY_ = 9999, maxY_ = -9999;
        for (Location location : getLocations()) {
            minY_ = Math.min(minY_, location.getBlockY());
            maxY_ = Math.max(maxY_, location.getBlockY());
            layers.add(location.getBlockY());
        }
        this.minY = minY_;
        this.maxY = maxY_;
        this.layers = layers.intStream().sorted().toArray();
    }

    @NonNull
    public SuperMultiBlockDefinition getDefinition() {
        return machine.getDefinition();
    }

    public boolean isFullyFormedCached() {
        return getDefinition().isFullyFormedCached(coreLocation, direction);
    }

    public boolean isLayerFormed(int layer) {
        return getLocations().stream().allMatch(l -> l.getBlockY() != layer || l.getBlockY() == layer && isFormedCached(l));
    }

    public void generateCache() {
        for (Location location : getLocations()) {
            if (isFormed(location)) {
                SuperMultiBlockManager.getInstance().getCorrectLocations().add(location);
            }
        }
    }

    public boolean isFormedCached(Location location) {
        return SuperMultiBlockManager.getInstance().getCorrectLocations().contains(location);
    }

    public boolean isFormed(Location location) {
        var part = getPart(location);
        return part != null && part.isOfPart(this, location) && part.isBuilt(this, location);
    }

    @Nullable
    public MultiBlockPart getPart(@NonNull Location location) {
        Vector3i offset = new Vector3i(location.toVector().subtract(coreLocation.toVector()));
        return getDefinition().getMap(direction).get(offset);
    }

    @NonNull
    public Set<Location> getLocations() {
        return getDefinition().getLocations(coreLocation, direction);
    }

    public void onFormed(Location location) {
        machine.onFormed(location);
    }

    public void onUnformed(Location location) {
        machine.onUnformed(location);
    }

    public void onDestroy() {
        machine.onDestroy();
    }

    public void autoSwitchedNewLayer(int layer) {
        machine.autoSwitchedNewLayer(layer);
    }

    public void formedLayer(int layer) {
        machine.formedLayer(layer);
    }

    public void onInteract(PlayerInteractEvent event, boolean clickedCore) {
        // checked permission
        machine.onInteract(event, clickedCore, this);
    }

    public void buildMultiBlock(Player player) {
        // checked permission
        int created = 0, built = 0;
        for (var l : getLocations()) {
            var part = getPart(l);
            if (part == null) continue;
            if (this.isFormedCached(l)) {
                built ++;
                continue;
            }
            SuperMultiBlockManager.getInstance().markDirty(l, true);

            var b = l.getBlock();
            if (part instanceof VanillaMultiBlockPart vanilla) {
                BlockData data = vanilla.getBlockData();
                b.setType(data.getMaterial());
                b.setBlockData(data, true);
                created++;
                continue;
            }

            if (part instanceof SlimefunMultiBlockPart sf) {
                var sfItem = sf.target.getItem();
                if (sfItem == null) {
                    fail(player, l, "block: " + sfItem);
                    continue;
                }
                var mt = sf.blockData.getPlacementMaterial();
                if (!mt.isBlock()) {
                    fail(player, l, "block: " + sfItem + "/" + mt);
                    continue;
                }

                if (StorageCacheUtils.getSfItem(l) != null) {
                    Slimefun.getDatabaseManager().getBlockDataController().removeBlock(l);
                }

                if (Slimefun.getBlockDataService().isTileEntity(mt)) {
                    Slimefun.getBlockDataService().setBlockData(b, sfItem.getId());
                }

                if (sfItem instanceof UniversalBlock) {
                    var data = Slimefun.getDatabaseManager()
                        .getBlockDataController()
                        .createUniversalBlock(l, sfItem.getId());

                    if (Slimefun.getBlockDataService().isTileEntity(mt)) {
                        Slimefun.getBlockDataService().updateUniversalDataUUID(b, data.getKey());
                    }
                } else {
                    Slimefun.getDatabaseManager()
                        .getBlockDataController()
                        .createBlock(l, sfItem.getId());
                }
                created++;
            }

            if (part instanceof CustomMultiBlockPart) {
                fail(player, l, "blockscript, Unable to");
                continue;
            }
        }

        String color;
        if (created + built == 0) color = "&c";
        else if (created + built < getLocations().size()) color = "&e";
        else color = "&a";

        Bukkit.getScheduler().runTaskLaterAsynchronously(RykenSlimefunCustomizer.INSTANCE, () -> {
            for (var l : getLocations()) {
                if (!isFormedCached(l)) {
                    player.sendMessage(CMIChatColor.translate("&cblock (" + l.getBlockX() + ", " + l.getBlockY() + ", " + l.getBlockZ() + ") not fully formed"));
                }
            }
            player.sendMessage(CMIChatColor.translate(isFullyFormedCached() ? "&amultiblockformed successfully!" : "&cmultiblocknot fully formed!"));
            if (isFullyFormedCached()) {
                removeProjectiles(this);
            }
        }, 3L);

        player.sendMessage(CMIChatColor.translate("RSC message" + color + (created + built) + "/" + getLocations().size() + " &ablock"));
    }

    private void fail(Player player, Location l, String reason) {
        player.sendMessage(CMIChatColor.translate("&c" + reason + " &7(" + l.getBlockX() + ", " + l.getBlockY() + ", " + l.getBlockZ() + ")"));
    }

    public void tick() {
        if (ticked) return;
        ticked = true;
        runAsyncLater(() -> getLocations().forEach(this::tryModifyMenu));
    }

    public void hideAllEntities() {
        hideEntities(selectEntities(this));
    }

    public void showAllEntities() {
        showEntities(selectEntities(this));
    }

    public void showEntities(int layer) {
        showEntities(selectEntities(this, layer));
    }

    public void markDirty() {
        for (var l : getLocations()) {
            markDirty(l, false);
        }
    }
}