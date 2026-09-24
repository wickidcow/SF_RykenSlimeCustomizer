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

import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class SlimefunMultiBlockPart implements MultiBlockPart {
    protected final SlimefunItemStack target;
    protected final BlockData blockData;

    public SlimefunMultiBlockPart(@NonNull SlimefunItemStack target) {
        this.target = target;
        this.blockData = Bukkit.createBlockData(target.getType());
    }

    @Override
    public boolean isOfPart(@NonNull SuperMultiBlock superMultiBlockInstance, @NonNull Location partLocation) {
        SlimefunItem sfItem = StorageCacheUtils.getSlimefunItem(partLocation);
        return sfItem != null && sfItem.getId().equals(target.getItemId());
    }

    @Override
    @Nullable
    public DisplayDescriptor getDisplayDescriptor(@NonNull SuperMultiBlock superMultiBlockInstance, @NonNull Location partLocation) {
        return new BlockDisplayDescriptor(blockData);
    }
}