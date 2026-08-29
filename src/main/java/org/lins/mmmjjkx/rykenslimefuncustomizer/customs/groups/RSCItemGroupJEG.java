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
package org.lins.mmmjjkx.rykenslimefuncustomizer.customs.groups;

import com.balugaq.jeg.utils.GuideUtil;
import com.balugaq.jeg.utils.clickhandler.OnClick;
import com.balugaq.jeg.utils.clickhandler.OnDisplay;
import com.balugaq.jeg.utils.formatter.Format;
import com.balugaq.jeg.utils.formatter.Formats;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.groups.FlexItemGroup;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideMode;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ClickAction;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;
import org.lins.mmmjjkx.rykenslimefuncustomizer.addon.ProjectAddon;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.Debug;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RSCItemGroupJEG extends FlexItemGroup implements BaseRSCItemGroup {
    private List<Object> contents;
    private final ProjectAddon addon;
    private final GroupType type;
    private final Visible visible;
    private final boolean forceHidden;
    private final boolean hasParent;
    private final int page;

    @Override
    public ProjectAddon getProjectAddon() {
        return addon;
    }

    public RSCItemGroupJEG(NamespacedKey key, ItemStack item, int tier, ProjectAddon addon, GroupType type, Visible visible, boolean forceHidden, boolean hasParent, int page) {
        super(key, item, tier);

        Debug.debug(() -> "item group: " + key + " type=" + type.name());

        contents = new ArrayList<>();
        this.addon = addon;
        this.type = type;
        this.visible = visible;
        this.forceHidden = forceHidden;
        this.hasParent = hasParent;
        this.page = page;
    }

    public void addContent(SlimefunItem sf) {
        Debug.debug(() -> "item " + sf.getId() + "RSC message" + getKey());
        contents.add(sf);
    }

    public void addContent(ItemGroup itemGroup) {
        Debug.debug(() -> "item group " + itemGroup.getKey().getKey() + "RSC message" + getKey());
        contents.add(itemGroup);
    }

    public void addContent(String action) {
        Debug.debug(() -> " Action " + action + "RSC message" + getKey());
        contents.add(action);
    }

    @Override
    public boolean isVisible/*InMainMenu*/(@NonNull Player p, @NonNull PlayerProfile profile, @NonNull SlimefunGuideMode layout) {
        if (forceHidden || hasParent || type == GroupType.sub || type == GroupType.button) return false;
        if (type == GroupType.nested || type == GroupType.normal) {
            return true; // compatibility
        }
        // type == GroupType.seasonal && !hasParent
        return visible.apply(p, profile, layout);
    }

    public boolean isVisibleInNested(@NonNull Player p, @NonNull PlayerProfile profile, @NonNull SlimefunGuideMode layout) {
        if (forceHidden) return false;

        return visible.apply(p, profile, layout);
    }

    @Override
    public void open(Player p, PlayerProfile profile, SlimefunGuideMode mode) {
        profile.getGuideHistory().add(this, page); // no matter survival or cheat mode.
        openPage(p, profile, mode, page);
    }

    private ChestMenu setup(Player p, PlayerProfile profile, SlimefunGuideMode mode) {
        ChestMenu menu = new ChestMenu(GuideUtil.getGuideTitle(mode));

        Format format = type == GroupType.nested ? Formats.nested : Formats.sub;
        char c = type == GroupType.nested ? Formats.Char.ITEM_GROUP : Formats.Char.CONTENT;
        List<Object> validContent = this.contents.stream().filter(content -> isContentVisibleInGroup(content, p, profile, mode)).toList();
        int pages = (validContent.size() - 1) / format.getChars(c).size() + 1;
        GuideUtil.commonRender(menu, format, profile, p, this, page, pages, np -> {
            openPage(p, profile, mode, np);
        });

        for (int i = 0; i < format.getChars(c).size(); i++) {
            int s = format.getChars(c).get(i);
            if ((page - 1) * format.getChars(c).size() + i >= validContent.size()) {
                menu.addItem(s, null);
                menu.addMenuClickHandler(s, (clicker, slot, item, action) -> false);
                continue;
            }

            Object content = validContent.get((page - 1) * format.getChars(c).size() + i);
            handleContent(s, content, menu, p, profile, mode);
        }

        return menu;
    }

    protected void handleContent(int s, Object content, ChestMenu menu, Player p, PlayerProfile profile, SlimefunGuideMode mode) {
        var impl = GuideUtil.getLastGuide(p);
        switch (content) {
            case RSCItemGroupJEG itemGroup -> {
                OnDisplay.ItemGroup.display(p, itemGroup, OnDisplay.ItemGroup.DisplayType.Normal, impl)
                    .at(menu, s, 1);

                OnClick.BaseClickHandler c = (OnClick.BaseClickHandler) menu.getMenuClickHandler(s);
                menu.addMenuClickHandler(s, new ChestMenu.AdvancedMenuClickHandler() {
                    @Override
                    public boolean onClick(InventoryClickEvent e, Player p, int slot, ItemStack cursor, ClickAction action) {
                        if (itemGroup.type == GroupType.button) {
                            // Don't open the item group, but run the scripts
                            for (var o : itemGroup.contents) {
                                if (o instanceof String ac) {
                                    readAction(ac, mode, p, slot, cursor, action);
                                }
                            }
                            return false;
                        }

                        return c.onClick(e, p, slot, item, action);
                    }

                    @Override
                    public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
                        return false;
                    }
                });
            }
            case SlimefunItem sf -> {
                OnDisplay.Item.display(p, sf, OnDisplay.Item.DisplayType.Normal, impl)
                    .at(menu, s, 1);
            }
            default -> throw new IllegalStateException("Unexpected value: " + content);
        }
    }

    private void openPage(Player p, PlayerProfile profile, SlimefunGuideMode mode, int page) {
        var group = new RSCItemGroupJEG(getKey(), getItem(p), getTier(), getProjectAddon(), type, visible, forceHidden, hasParent, page);
        group.contents = contents;
        ChestMenu menu = group.setup(p, profile, mode);
        menu.open(p);
    }

    @Override
    public ItemStack getItem(Player p) {
        if (!item.hasItemMeta()) return super.getItem(p);
        var meta = item.getItemMeta();
        if (meta == null) return super.getItem(p);
        var lore = meta.getLore();
        if (lore == null || lore.isEmpty()) return super.getItem(p);
        return item;
    }
}
