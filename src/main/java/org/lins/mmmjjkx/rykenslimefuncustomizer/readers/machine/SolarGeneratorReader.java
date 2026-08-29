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
import org.bukkit.configuration.ConfigurationSection;
import org.lins.mmmjjkx.rykenslimefuncustomizer.addon.ProjectAddon;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.CustomSolarGenerator;
import org.lins.mmmjjkx.rykenslimefuncustomizer.readers.YamlReader;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.Constants;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.Debug;

import java.io.File;
import java.util.List;

public class SolarGeneratorReader extends YamlReader<CustomSolarGenerator> {
    @Override
    public String getFileName() {
        return Constants.SOLAR_GENERATORS_FILE;
    }

    public SolarGeneratorReader(File file, ProjectAddon addon) {
        super(file, addon);
    }

    @Override
    public CustomSolarGenerator readEach(String s) {
        ConfigurationSection section = configuration.getConfigurationSection(s);
        if (section == null) return null;
        var base = getBase(section, s);
        if (base == null) return null;

        int dayEnergy = section.getInt("dayEnergy");
        int nightEnergy = section.getInt("nightEnergy");

        if (dayEnergy < 1) {
            Debug.error(file, section, "MissingConfiguration error '' (dayEnergy)", 1, Integer.MAX_VALUE);
            return null;
        }

        if (nightEnergy < 1) {
            Debug.error(file, section, "MissingConfiguration error '' (nightEnergy)", 1, Integer.MAX_VALUE);
            return null;
        }

        int capacity = section.getInt("capacity", 1);
        int lightLevel = section.getInt("lightLevel", 15);

        if (lightLevel < 0 || lightLevel > 15) {
            Debug.error(file, section, "MissingConfiguration error '' (lightLevel)", 1, 15);
            return null;
        }

        return new CustomSolarGenerator(base, dayEnergy, nightEnergy, capacity, lightLevel);
    }

    @Override
    public List<SlimefunItemStack> preloadItems(String s) {
        return blockPreloadItems(s);
    }
}
