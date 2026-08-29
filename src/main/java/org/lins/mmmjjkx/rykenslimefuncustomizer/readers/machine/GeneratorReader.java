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
package org.lins.mmmjjkx.rykenslimefuncustomizer.readers.machine;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import lombok.SneakyThrows;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineFuel;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.lins.mmmjjkx.rykenslimefuncustomizer.addon.ProjectAddon;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.CustomGenerator;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.menu.CustomMenu;
import org.lins.mmmjjkx.rykenslimefuncustomizer.readers.YamlReader;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.CommonUtils;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.Constants;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.Debug;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GeneratorReader extends YamlReader<CustomGenerator> {
    @Override
    public String getFileName() {
        return Constants.GENERATORS_FILE;
    }

    public GeneratorReader(File file, ProjectAddon addon) {
        super(file, addon);
    }

    @SneakyThrows
    @Override
    public CustomGenerator readEach(String s) {
        ConfigurationSection section = configuration.getConfigurationSection(s);
        if (section == null) return null;
        String id = getId(s);
        var base = getBase(section, s);
        if (base == null) return null;

        CustomMenu menu = CommonUtils.getIf(addon.getMenus(), m -> m.getId().equalsIgnoreCase(id));

        List<Integer> input = section.getIntegerList("input");
        List<Integer> output = section.getIntegerList("output");

        ConfigurationSection fuelsSection = section.getConfigurationSection("fuels");
        List<MachineFuel> fuels = readFuels(s, fuelsSection, addon);
        int capacity = section.getInt("capacity", 1);
        int production = section.getInt("production", -1);

        if (production < 1) {
            Debug.error(file, section, "MissingConfiguration error '' (production)", 1, Integer.MAX_VALUE);
            return null;
        }

        return new CustomGenerator(base, menu, capacity, input, output, production, fuels);
    }

    @Override
    public List<SlimefunItemStack> preloadItems(String s) {
        return blockPreloadItems(s);
    }

    private List<MachineFuel> readFuels(String s, ConfigurationSection fuelsCfg, ProjectAddon addon) {
        List<MachineFuel> fuels = new ArrayList<>();

        if (fuelsCfg == null) return fuels;

        for (String key : fuelsCfg.getKeys(false)) {
            ConfigurationSection fuelCfg = fuelsCfg.getConfigurationSection(key);
            if (fuelCfg == null) continue;
            ConfigurationSection item = fuelCfg.getConfigurationSection("item");
            ItemStack stack = CommonUtils.readItem(file, item, addon);
            if (stack == null) {
                Debug.error(file, fuelCfg, "Missing 'item' (input)");
                continue;
            }
            int seconds = fuelCfg.getInt("seconds", -1);

            if (seconds < 0) {
                Debug.warn(file, fuelCfg, "MissingConfiguration error 'recipe' (seconds) skipped");
                continue;
            }

            if (fuelCfg.contains("output")) {
                ConfigurationSection outputSet = fuelCfg.getConfigurationSection("output");
                ItemStack output = CommonUtils.readItem(file, outputSet, addon);
                if (output == null) {
                    Debug.error(file, fuelCfg, "Missing 'item' (output)");
                    continue;
                } else {
                    MachineFuel fuel = new MachineFuel(seconds, stack, output);
                    fuels.add(fuel);
                }
            } else {
                MachineFuel fuel = new MachineFuel(seconds, stack, null);
                fuels.add(fuel);
            }
        }
        return fuels;
    }
}
