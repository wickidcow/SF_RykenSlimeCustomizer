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

import io.github.thebusybiscuit.slimefun4.api.geo.GEOResource;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.libraries.dough.collections.Pair;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;
import org.lins.mmmjjkx.rykenslimefuncustomizer.addon.ProjectAddon;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.CustomGeoResource;
import org.lins.mmmjjkx.rykenslimefuncustomizer.readers.YamlReader;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.CommonUtils;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.Constants;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.Debug;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.function.BiFunction;

public class GeoResourceReader extends YamlReader<CustomGeoResource> {
    @Override
    public String getFileName() {
        return Constants.GEO_RESOURCES_FILE;
    }

    public GeoResourceReader(File file, ProjectAddon addon) {
        super(file, addon);
    }

    @Override
    public CustomGeoResource readEach(String s) {
        ConfigurationSection section = configuration.getConfigurationSection(s);
        if (section == null) return null;
        String id = getId(s);

        if (!CommonUtils.passItemIdConflictCheck(id)) return null;
        ItemGroup group = CommonUtils.getItemGroup(addon, section.getString("item_group"));
        if (group == null) return null;

        SlimefunItemStack sfis = getPreloadItem(id);
        if (sfis == null) return null;

        int maxDeviation = section.getInt("max_deviation", 1);
        boolean obtainableFromGEOMiner = section.getBoolean("obtain_from_geo_miner", true);
        String name = section.getString("geo_name", "");

        Pair<RecipeType, ItemStack[]> recipePair = getRecipe(section, addon);
        RecipeType rt = recipePair.getFirstValue();
        ItemStack[] itemStacks = recipePair.getSecondValue();

        ConfigurationSection sup = section.getConfigurationSection("supply");

        BiFunction<World.Environment, Biome, Integer> supply = (e, b) -> {
            if (sup == null) {
                return 0;
            }

            if (e == World.Environment.CUSTOM) return 0;

            String env = e.toString().toLowerCase(Locale.ROOT);
            String path = b.toString().toLowerCase(Locale.ROOT);
            boolean isSection = sup.isConfigurationSection(env);

            if (!isSection) {
                return sup.getInt(env, 0);
            }

            ConfigurationSection biomes = sup.getConfigurationSection(env);
            if (biomes == null) return 0;
            if (biomes.contains(path)) {
                return biomes.getInt(path, 0);
            } else {
                return biomes.getInt("others", 0);
            }
        };

        if (section.contains("drop_from")) {
            resolveDropFrom(file, section, sfis, addon);
        }

        return new CustomGeoResource(group, sfis, rt, itemStacks, supply, maxDeviation, obtainableFromGEOMiner, name);
    }

    @Override
    public List<SlimefunItemStack> preloadItems(String id) {
        ConfigurationSection section = configuration.getConfigurationSection(id);

        if (section == null) return null;

        ConfigurationSection item = section.getConfigurationSection("item");
        ItemStack stack = CommonUtils.readItem(file, item, addon);
        if (stack == null) {
            Debug.error("addon" + addon.getAddonId() + "Source" + id + "RSC: " + "itemUnable to");
            return null;
        }

        return List.of(new SlimefunItemStack(addon.getId(id, section.getString("id_alias")), stack));
    }

    private GEOResource createGEO(
            BiFunction<World.Environment, Biome, Integer> supply,
            int maxDeviation,
            boolean obtainableFromGEOMiner,
            String name,
            SlimefunItemStack item,
            NamespacedKey key) {
        return new GEOResource() {
            @Override
            public int getDefaultSupply(World.@NonNull Environment environment, @NonNull Biome biome) {
                return supply.apply(environment, biome);
            }

            @Override
            public int getMaxDeviation() {
                return maxDeviation;
            }

            @NonNull @Override
            public String getName() {
                return name;
            }

            @NonNull @Override
            public ItemStack getItem() {
                return item;
            }

            @Override
            public boolean isObtainableFromGEOMiner() {
                return obtainableFromGEOMiner;
            }

            @Override
            public @NonNull NamespacedKey getKey() {
                return key;
            }
        };
    }
}
