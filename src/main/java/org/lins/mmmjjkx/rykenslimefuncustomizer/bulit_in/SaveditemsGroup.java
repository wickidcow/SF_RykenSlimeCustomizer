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
package org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in;

import com.balugaq.jeg.api.groups.MixedGroup;
import com.balugaq.jeg.api.interfaces.JEGSlimefunGuideImplementation;
import com.balugaq.jeg.utils.GuideUtil;
import com.balugaq.jeg.utils.clickhandler.OnDisplay;
import com.balugaq.jeg.utils.formatter.Format;
import com.balugaq.jeg.utils.formatter.Formats;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideImplementation;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideMode;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jspecify.annotations.NonNull;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.CommonUtils;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.Keys;

import java.util.List;

@SuppressWarnings("deprecation")
public class SaveditemsGroup extends MixedGroup<@NonNull SaveditemsGroup> {
    public static SaveditemsGroup instance;
    public static final NamespacedKey SOURCE_KEY = Keys.newKey("source");;

    public SaveditemsGroup(final NamespacedKey key, final ItemStack item) {
        super(key, item);
        this.pageMap.put(1, this);
    }

    @Override
    public boolean isVisible(
            final Player player,
            final @NonNull PlayerProfile playerProfile,
            final @NonNull SlimefunGuideMode slimefunGuideMode) {
        return player.isOp();
    }

    @Override
    public @NonNull ChestMenu generateMenu(
            @NonNull Player player,
            @NonNull PlayerProfile playerProfile,
            @NonNull SlimefunGuideMode slimefunGuideMode) {
        ChestMenu chestMenu = new ChestMenu(CommonUtils.getItemName(this.getItem(player)));

        Format format = Formats.sub;
        int pages = (this.objects.size() - 1) / format.getChars(Formats.Char.CONTENT).size() + 1;
        GuideUtil.commonRender(chestMenu, format, playerProfile, player, this, page, pages);

        List<Integer> contentSlots = format.getChars(Formats.Char.CONTENT);

        SlimefunGuideImplementation impl = GuideUtil.getGuide(player, GuideUtil.getLastGuideMode(player));

        for (int i = 0; i < contentSlots.size(); ++i) {
            int index = i + this.page * contentSlots.size() - contentSlots.size();
            if (index < this.objects.size()) {
                Object o = this.objects.get(index);
                switch (o) {
                    case final SlimefunItem slimefunItem ->
                        OnDisplay.Item.display(player, slimefunItem.getItem(), OnDisplay.Item.Normal, impl)
                            .at(chestMenu, contentSlots.get(i), this.page);
                    case final ItemGroup itemGroup -> {
                        if (impl instanceof final JEGSlimefunGuideImplementation guide) {
                            guide.showItemGroup0(chestMenu, player, playerProfile, itemGroup, contentSlots.get(i));
                        }
                    }
                    case final ItemStack itemStack -> {
                        ItemStack clone = itemStack.clone();
                        String source =
                            clone.getItemMeta().getPersistentDataContainer().get(SOURCE_KEY, PersistentDataType.STRING);
                        CommonUtils.addLore(clone, true, "&eSource: " + source);
                        OnDisplay.Item.display(player, clone, OnDisplay.Item.Normal, impl)
                            .at(chestMenu, contentSlots.get(i), this.page);

                        chestMenu.addMenuClickHandler(contentSlots.get(i), (p, s, ik, a) -> {
                            if (p.isOp()) {
                                p.getInventory().addItem(itemStack.clone());
                            }
                            return false;
                        });
                    }
                    default -> {
                    }
                }
            }
        }

        return chestMenu;
    }
}
