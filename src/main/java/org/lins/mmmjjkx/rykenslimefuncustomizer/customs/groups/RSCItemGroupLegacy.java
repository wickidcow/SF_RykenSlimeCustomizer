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

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.groups.FlexItemGroup;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.core.guide.GuideHistory;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuide;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideMode;
import io.github.thebusybiscuit.slimefun4.core.services.sounds.SoundEffect;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.guide.SurvivalSlimefunGuide;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;
import org.lins.mmmjjkx.rykenslimefuncustomizer.addon.ProjectAddon;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.Debug;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.ReflectionUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RSCItemGroupLegacy extends FlexItemGroup implements BaseRSCItemGroup {
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

    public RSCItemGroupLegacy(NamespacedKey key, ItemStack item, int tier, ProjectAddon addon, GroupType type, Visible visible, boolean forceHidden, boolean hasParent, int page) {
        super(key, item, tier);

        Debug.debug(() -> "item group: " + key + " type=" + type.name() + ", page=" + page);

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
        GuideHistory history = profile.getGuideHistory();
        if (mode == SlimefunGuideMode.SURVIVAL_MODE) {
            history.add(this, page);
        }
        openPage(p, profile, mode, page);
    }

    private void openPage(Player p, PlayerProfile profile, SlimefunGuideMode mode, int page) {
        var group = new RSCItemGroupLegacy(getKey(), getItem(p), getTier(), getProjectAddon(), type, visible, forceHidden, hasParent, page);
        group.contents = contents;
        ChestMenu menu = group.setup(p, profile, mode);
        menu.open(p);
    }
    
    private ChestMenu setup(Player p, PlayerProfile profile, SlimefunGuideMode mode) {
        GuideHistory history = profile.getGuideHistory();
        ChestMenu menu = new ChestMenu(Slimefun.getLocalization().getMessage(p, "guide.title.main"));
        SurvivalSlimefunGuide guide =
            (SurvivalSlimefunGuide) Slimefun.getRegistry().getSlimefunGuide(mode);
        menu.setEmptySlotsClickable(false);
        menu.addMenuOpeningHandler(SoundEffect.GUIDE_BUTTON_CLICK_SOUND::playFor);
        guide.createHeader(p, profile, menu);
        ItemStack backIcon;
        if (history.size() > 1) {
            backIcon = ChestMenuUtils.getBackButton(
                p,
                "",
                ChatColor.GRAY + Slimefun.getLocalization().getMessage(p, "guide.back.guide"));
        } else {
            backIcon = ChestMenuUtils.getBackButton(
                p,
                "",
                "&fLeft click: &7Previous page",
                "&fShift + Left click: &7Main menu"
            );
        }
        menu.addItem(1, backIcon, (pl, s, is, action) -> {
            GuideHistory guideHistory = profile.getGuideHistory();
            if (history.size() == 1 || action.isShiftClicked()) {
                SlimefunGuide.openMainMenu(profile, mode, history.getMainMenuPage());
                return false;
            }

            guideHistory.goBack(guide);
            return false;
        });

        int index = 9;
        int target = 36 * (page - 1) - 1;

        while (target < this.contents.size() - 1 && index < 45) {
            ++target;
            Object content = this.contents.get(target);
            switch (content) {
                case RSCItemGroupLegacy itemGroup -> {
                    if (itemGroup.isVisibleInNested(p, profile, mode)) {
                        menu.addItem(index, itemGroup.getItem(p));
                        menu.addMenuClickHandler(index, (pl, slot, item, action) -> {
                            // Don't open the item group, but run the scripts
                            if (itemGroup.type == GroupType.button) {
                                for (var o : itemGroup.contents) {
                                    if (o instanceof String ac) {
                                        readAction(ac, mode, pl, slot, item, action);
                                    }
                                }
                                return false;
                            }
                            SlimefunGuide.openItemGroup(profile, itemGroup, mode, 1);
                            return false;
                        });
                        ++index;
                    }
                }
                case SlimefunItem sf -> {
                    if (!sf.isDisabledIn(p.getWorld())) {
                        var impl = Slimefun.getRegistry().getSlimefunGuide(mode);
                        ReflectionUtil.invokeMethod(impl, "displaySlimefunItem", menu, this, p, profile, sf, page, index);
                        ++index;
                    }
                }
                default -> throw new IllegalStateException("Unexpected value: " + content);
            }
        }

        int validCount = (int) this.contents.stream().filter(content -> isContentVisibleInGroup(content, p, profile, mode)).count();
        int pages = target == validCount - 1 ? page : validCount / 36 + 1;
        menu.addItem(46, ChestMenuUtils.getPreviousButton(p, page, pages));
        menu.addMenuClickHandler(46, (pl, slot, item, action) -> {
            int previous = page - 1;
            if (previous > 0) {
                openPage(p, profile, mode, previous);
            }

            return false;
        });
        menu.addItem(52, ChestMenuUtils.getNextButton(p, page, pages));
        menu.addMenuClickHandler(52, (pl, slot, item, action) -> {
            int next = page + 1;
            if (next <= pages) {
                openPage(p, profile, mode, next);
            }

            return false;
        });

        return menu;
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
