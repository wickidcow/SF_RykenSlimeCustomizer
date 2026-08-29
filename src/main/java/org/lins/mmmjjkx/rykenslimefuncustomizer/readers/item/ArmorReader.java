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
package org.lins.mmmjjkx.rykenslimefuncustomizer.readers.item;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.ProtectionType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.collections.Pair;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.lins.mmmjjkx.rykenslimefuncustomizer.addon.ProjectAddon;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.CustomArmorPiece;
import org.lins.mmmjjkx.rykenslimefuncustomizer.readers.YamlReader;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.CommonUtils;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.Constants;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.Debug;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class ArmorReader extends YamlReader<List<CustomArmorPiece>> {
    private final List<String> CHECKS = List.of("helmet", "chestplate", "leggings", "boots");

    @Override
    public String getFileName() {
        return Constants.ARMORS_FILE;
    }

    public ArmorReader(File file, ProjectAddon addon) {
        super(file, addon);
    }


    @Override
    public List<CustomArmorPiece> readEach(String s) {
        ConfigurationSection section = configuration.getConfigurationSection(s);
        if (section == null) return null;

        boolean fullSet = section.getBoolean("fullSet", false);

        ItemGroup group = CommonUtils.getItemGroup(addon, section.getString("item_group"));
        if (group == null) return null;

        List<String> pt = section.getStringList("protection_types");
        List<ProtectionType> protectionTypes = new ArrayList<>();
        for (String type : pt) {
            Optional<ProtectionType> result = CommonUtils.getEnum(ProtectionType.class, type);
            if (result.isEmpty()) {
                Debug.warn(file, section, " (protection_types) : " + type);
                continue;
            }
            protectionTypes.add(result.get());
        }

        List<CustomArmorPiece> pieces = new ArrayList<>();
        for (String check : CHECKS) {
            ConfigurationSection pieceSection = section.getConfigurationSection(check);
            if (pieceSection == null) continue;

            String pieceId = addon.getId(s + "_" + check.toUpperCase(Locale.ROOT), section.getString("id_alias", pieceSection.getString("id", "")));
            if (!CommonUtils.passItemIdConflictCheck(pieceId)) return null;

            Pair<RecipeType, ItemStack[]> recipePair = getRecipe(pieceSection, addon);
            RecipeType rt = recipePair.getFirstValue();
            ItemStack[] recipe = recipePair.getSecondValue();

            SlimefunItemStack sfis = getPreloadItem(pieceId);
            if (sfis == null) return null;

            List<PotionEffect> potionEffects = new ArrayList<>();
            List<String> effects = pieceSection.getStringList("potion_effects");

            for (String effect : effects) {
                String[] split = effect.split(" ");
                if (split.length != 2) {
                    Debug.warn(file, pieceSection, " (potion_effects) " + effect);
                    continue;
                }
                String effectName = split[0];
                int amplifier = Integer.parseInt(split[1]);

                PotionEffectType type = PotionEffectType.getByName(effectName);
                if (type == null) {
                    Debug.warn(file, pieceSection, " (potion_effects) " + effectName);
                    continue;
                }

                if (amplifier < 0) {
                    Debug.warn(file, pieceSection, " (potion_effects) " + effect, 1, Integer.MAX_VALUE);
                    continue;
                }

                potionEffects.add(new PotionEffect(
                    type,
                    (Slimefun.getCfg().getInt("options.armor-update-interval") + 3) * 20,
                    amplifier)
                );
            }

            pieces.add(new CustomArmorPiece(
                    group,
                    sfis,
                    rt,
                    recipe,
                    potionEffects.toArray(new PotionEffect[0]),
                    fullSet,
                    s,
                    protectionTypes.toArray(new ProtectionType[0]),
                    addon.getAddonId()));
        }

        if (pieces.isEmpty()) {
            Debug.error(file, section, "Has");
            return null;
        }

        return pieces;
    }

    @Override
    public List<SlimefunItemStack> preloadItems(String s) {
        List<SlimefunItemStack> items = new ArrayList<>(4);
        ConfigurationSection section = configuration.getConfigurationSection(s);
        if (section == null) return null;

        for (String check : CHECKS) {
            ConfigurationSection piece = section.getConfigurationSection(check);
            if (piece == null) {
                Debug.warn(file, section, "RSC: " + check + ")");
                continue;
            }


            ItemStack stack = CommonUtils.readItem(file, piece, addon);

            if (stack == null) {
                Debug.warn(file, piece, "RSC: " + check + ")");
                continue;
            }

            SlimefunItemStack sfis = new SlimefunItemStack(
                addon.getId(
                    s + "_" + check.toUpperCase(Locale.ROOT),
                    section.getString("id_alias", piece.getString("id", ""))),
                stack);
            items.add(sfis);
        }

        return items;
    }
}
