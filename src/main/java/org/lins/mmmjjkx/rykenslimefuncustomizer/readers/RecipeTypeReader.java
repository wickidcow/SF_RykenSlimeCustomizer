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
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.lins.mmmjjkx.rykenslimefuncustomizer.addon.ProjectAddon;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.CustomRecipeType;
import org.lins.mmmjjkx.rykenslimefuncustomizer.readers.machine.MultiBlockMachineReader;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.CommonUtils;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.Constants;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.Debug;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.Keys;

import java.io.File;
import java.util.List;

public class RecipeTypeReader extends YamlReader<RecipeType> {
    @Override
    public String getFileName() {
        return Constants.RECIPE_TYPES_FILE;
    }

    public RecipeTypeReader(File file, ProjectAddon addon) {
        super(file, addon);
    }

    @Override
    public RecipeType readEach(String s) {
        ConfigurationSection section = configuration.getConfigurationSection(s);
        if (section == null) return null;

        ItemStack item = CommonUtils.readItem(file, section, addon);
        if (item == null) {
            Debug.error(file, section, "MissingConfiguration error 'item' (item)");
            return null;
        }

        String bindToMultiblock = section.getString("bind-to-multiblock");
        if (bindToMultiblock != null) {
            Debug.debug(file, () -> "recipe " + s + " -> multiblock " + bindToMultiblock);
            return new CustomRecipeType(Keys.newKey(s), item, (recipe, result) -> {
                MultiBlockMachineReader.addPreaddRecipe(bindToMultiblock, recipe, result);
            }, (a, b) -> {/* unregister recipe is not supported yet*/});
        }

        return new CustomRecipeType(Keys.newKey(s), item);
    }

    // 配方类型不需要预加载物品
    @Override
    public List<SlimefunItemStack> preloadItems(String s) {
        return List.of();
    }
}
