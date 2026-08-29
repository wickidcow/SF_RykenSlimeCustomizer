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
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.lins.mmmjjkx.rykenslimefuncustomizer.addon.ProjectAddon;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.generations.GenerationArea;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.generations.GenerationInfo;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.Constants;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.Debug;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.objects.Range;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GenerationReader extends YamlReader<GenerationInfo> {
    @Override
    public String getFileName() {
        return Constants.GENERATIONS_FILE;
    }

    public GenerationReader(File file, ProjectAddon addon) {
        super(file, addon);
    }

    @Override
    public GenerationInfo readEach(String s) {
        ConfigurationSection section = configuration.getConfigurationSection(s);
        if (section == null) return null;

        List<GenerationArea> areas = new ArrayList<>();
        ConfigurationSection areaSection = section.getConfigurationSection("areas");
        if (areaSection == null) return null;

        String id = addon.getId(s, section.getString("slimefun_id"));
        SlimefunItemStack slimefunItemStack = getPreloadItem(id);
        if (slimefunItemStack == null) return null;

        int c = 1;
        while (areaSection.contains(String.valueOf(c))) {
            var cfg = areaSection.getConfigurationSection(String.valueOf(c));
            if (cfg == null) {
                Debug.error(file, areaSection, "Invalid (areas): " + c);
                continue;
            }

            areas.add(readArea(cfg));

            c++;
        }

        return new GenerationInfo(slimefunItemStack, areas);
    }

    @Override
    public List<SlimefunItemStack> preloadItems(String s) {
        return new ArrayList<>();
    }

    public static GenerationArea readArea(@Nonnull ConfigurationSection section) {
        int maxHeight = section.getInt("maxHeight");
        int mixHeight = section.getInt("mixHeight");
        int most = section.getInt("most");
        int amount = section.getInt("amount");
        int maxSize = section.getInt("maxSize");
        int minSize = section.getInt("minSize");
        Optional<Material> materialOptional =
                Optional.ofNullable(Material.matchMaterial(section.getString("replacement", "")));
        Material replacement = Material.STONE;

        if (materialOptional.isPresent()) {
            replacement = materialOptional.get();
        }
        @Nonnull World.Environment environment = World.Environment.valueOf(section.getString("environment"));

        return new GenerationArea(
                new Range(mixHeight, maxHeight), most, amount, new Range(minSize, maxSize), replacement, environment);
    }
}
