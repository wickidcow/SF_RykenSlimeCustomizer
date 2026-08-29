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
package org.lins.mmmjjkx.rykenslimefuncustomizer.readers;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.lins.mmmjjkx.rykenslimefuncustomizer.addon.ProjectAddon;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.menu.CustomMenu;
import org.lins.mmmjjkx.rykenslimefuncustomizer.script.JavaScriptEval;
import org.lins.mmmjjkx.rykenslimefuncustomizer.script.ScriptEval;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.CommonUtils;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.Constants;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.Debug;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.Keys;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class MenuReader extends YamlReader<CustomMenu> {
    public static final int NOT_SET = -1;
    private static final NamespacedKey PROGRESS_KEY = Keys.newKey("progress");

    @Override
    public String getFileName() {
        return Constants.MENUS_FILE;
    }

    public MenuReader(File file, ProjectAddon addon) {
        super(file, addon);
    }


    @Override
    public CustomMenu readEach(String s) {
        ConfigurationSection section = configuration.getConfigurationSection(s);
        if (section == null) return null;

        if (!CommonUtils.passMenuIdConflictCheck(s, addon)) return null;

        String title = section.getString("title"); // nullable
        boolean playerInvClickable = section.getBoolean("playerInvClickable", true);
        int size = section.getInt("size", NOT_SET);

        if (section.contains("size") && size != NOT_SET && size % 9 != 0) {
            Debug.error(file, section, "menu 9 (size): " + size);
            return null;
        }

        if (size != NOT_SET && size < 9 || size > 54) {
            Debug.error(file, section, "menu (size): " + size, 9, 54);
            return null;
        }

        JavaScriptEval eval = getScriptOrNull(section, section.getString("script"));

        if (section.contains("import")) {
            String menuId = section.getString("import", "");
            BlockMenuPreset menuPreset = Slimefun.getRegistry().getMenuPresets().get(menuId);
            if (menuPreset == null) {
                CustomMenu menu =
                        CommonUtils.getIf(addon.getMenus(), m -> m.getId().equals(menuId));
                if (menu == null) {
                    Debug.error(file, section, "Unable tomenu (import): " + menuId);
                    return null;
                } else {
                    return new CustomMenu(s, title, menu);
                }
            }
            return new CustomMenu(s, title, menuPreset, eval);
        }

        if (section.contains("matrix")) {
            return readMatrix(section, s, title, playerInvClickable, eval);
        }

        int progress = 22;
        ItemStack progressItem = createDefaultProgressItem();

        Map<Integer, ItemStack> slotMap = new HashMap<>();
        ConfigurationSection slots = section.getConfigurationSection("slots");
        if (slots == null) {
            Debug.error(file, section, "Hasitem (slots)");
            return null;
        }

        for (String slot : slots.getKeys(false)) {
            try {
                int realSlot = Integer.parseInt(slot);
                if (realSlot > 53 || realSlot < 0) {
                    Debug.warn(file, section, "slot: " + slot + " skipped", 0, 53);
                    continue;
                }
                ConfigurationSection item = slots.getConfigurationSection(slot);
                ItemStack itemStack = CommonUtils.readItem(file, item, addon);
                if (itemStack == null) {
                    Debug.warn(file, section, "slotitemInvalid (slots): " + slot + " skipped");
                    continue;
                }
                if (item.getBoolean("progressbar", false)) {
                    progress = realSlot;
                    if (item.contains("progressBarItem")) {
                        progressItem = CommonUtils.readItem(file, item.getConfigurationSection("progressBarItem"), addon);
                    } else {
                        progressItem = itemStack;
                    }

                    ItemMeta meta = progressItem.getItemMeta();
                    if (meta != null) {
                        PersistentDataContainer pdc = meta.getPersistentDataContainer();
                        pdc.set(PROGRESS_KEY, PersistentDataType.INTEGER, 0);
                    }
                }
                slotMap.put(realSlot, itemStack);
            } catch (NumberFormatException e) {
                String[] range = slot.split("-");
                if (range.length != 2) {
                    Debug.error(file, section, "slot (slots): " + slot);
                    continue;
                }
                ConfigurationSection item = slots.getConfigurationSection(slot);
                ItemStack stack = CommonUtils.readItem(file, item, addon);
                if (stack == null) {
                    Debug.warn(file, section, "slotitemInvalid (slots): " + slot);
                    continue;
                }
                try {
                    IntStream.rangeClosed(Integer.parseInt(range[0]), Integer.parseInt(range[1]))
                        .forEach(i -> {
                        if (i < 0 || i > 53) {
                            Debug.warn(file, section, "slot: " + slot, 0, 53);
                            return;
                        }
                        slotMap.put(i, stack);
                    });
                } catch (NumberFormatException e2) {
                    Debug.error(file, section, "slot (slots): " + slot);
                    return null;
                }
            }
        }
        if (progressItem == null) progressItem = createDefaultProgressItem();

        return new CustomMenu(s, title, slotMap, playerInvClickable, progress, progressItem, eval).setSize(size);
    }

    private static ItemStack createDefaultProgressItem() {
        return new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
    }

    private CustomMenu readMatrix(ConfigurationSection section, String s, String title, boolean playerInvClickable, ScriptEval eval) {
        int progress = 22;
        ItemStack progressItem = createDefaultProgressItem();
        List<String> matrix = section.getStringList("matrix");
        Map<Integer, ItemStack> slotMap = new HashMap<>();
        Map<Character, ItemStack> mapping = new HashMap<>();
        int slot = 0;
        for (String line : matrix) {
            for (char c : line.toCharArray()) {
                if (!mapping.containsKey(c)) {
                    var k = section.getConfigurationSection("mapping." + c);
                    if (k == null) {
                        slot += 1;
                        continue;
                    }
                    mapping.put(c, CommonUtils.readItem(file, k, addon));
                    if (k.getBoolean("progressbar", false)) {
                        progress = slot;
                        if (k.contains("progressBarItem")) {
                            progressItem = CommonUtils.readItem(file, k.getConfigurationSection("progressBarItem"), addon);
                        } else {
                            progressItem = mapping.get(c);
                        }

                        ItemMeta meta = progressItem.getItemMeta();
                        if (meta != null) {
                            PersistentDataContainer pdc = meta.getPersistentDataContainer();
                            pdc.set(PROGRESS_KEY, PersistentDataType.INTEGER, 0);
                        }
                    }
                }
                slotMap.put(slot, mapping.get(c));
                slot += 1;
            }
        }
        if (progressItem == null) progressItem = createDefaultProgressItem();

        return new CustomMenu(s, title, slotMap, playerInvClickable, progress, progressItem, eval);
    }

    // 菜单不需要预加载物品
    @Override
    public List<SlimefunItemStack> preloadItems(String s) {
        return List.of();
    }
}
