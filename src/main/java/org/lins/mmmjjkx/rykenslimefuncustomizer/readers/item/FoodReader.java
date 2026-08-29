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

import de.tr7zw.nbtapi.NBT;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.lins.mmmjjkx.rykenslimefuncustomizer.addon.ProjectAddon;
import org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.PluginStateCache;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.CustomFood;
import org.lins.mmmjjkx.rykenslimefuncustomizer.readers.YamlReader;
import org.lins.mmmjjkx.rykenslimefuncustomizer.script.JavaScriptEval;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.CommonUtils;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.Constants;

import java.io.File;
import java.util.List;

public class FoodReader extends YamlReader<CustomFood> {
    @Override
    public String getFileName() {
        return Constants.FOODS_FILE;
    }

    public FoodReader(File file, ProjectAddon addon) {
        super(file, addon);
    }

    @Override
    public CustomFood readEach(String s) {
        ConfigurationSection section = configuration.getConfigurationSection(s);
        if (section == null) return null;
        var id = getId(s);
        var base = getBase(section, s);
        if (base == null) return null;

        JavaScriptEval eval = getScriptOrNull(section, section.getString("script"));

        if (CommonUtils.versionToCode(Bukkit.getMinecraftVersion()) >= 1205 && PluginStateCache.isEnabled("NBTAPI")) {
            nbtApply(id, section, base.sfis());
        }

        return new CustomFood(base, eval);
    }

    private void nbtApply(String s, ConfigurationSection section, SlimefunItemStack sfis) {
        int nutrition = CommonUtils.clamp(section.getInt("nutrition"), 1, Integer.MAX_VALUE, file, section, " (nutrition) ");
        float saturation = CommonUtils.clamp(section.getInt("saturation"), 0f, Float.MAX_VALUE, file, section, " (saturation) ");
        boolean alwaysEatable = section.getBoolean("always_eatable", false);
        float eatSeconds = CommonUtils.clamp((float) section.getDouble("eat_seconds", 1.6d), 0f, 1.6f, Float.MAX_VALUE, file, section, " (eat_seconds) ");

        NBT.modify(sfis, c -> {
            c.setInteger("nutrition", nutrition);
            c.setFloat("saturation", saturation);
            c.setBoolean("can_always_eat", alwaysEatable);
            c.setFloat("eat_seconds", eatSeconds);
        });
    }

    @Override
    public List<SlimefunItemStack> preloadItems(String s) {
        return anyPreloadItems(s);
    }
}
