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
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import org.bukkit.configuration.ConfigurationSection;
import org.lins.mmmjjkx.rykenslimefuncustomizer.addon.ProjectAddon;
import org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.tickers.MachineTicker;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.CustomWorkbench;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.menu.CustomMenu;
import org.lins.mmmjjkx.rykenslimefuncustomizer.readers.YamlReader;
import org.lins.mmmjjkx.rykenslimefuncustomizer.script.JavaScriptEval;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.CommonUtils;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.Constants;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.Debug;

import java.io.File;
import java.util.List;

/*
 * RSC_EXAMPLE_WORKBENCH:
 *   item_group: RSC_EXAMPLE_NORMAL_GROUP
 *   item:
 *     material: GOLD_BLOCK
 *   input: [19, 20]
 *   output: [24, 25]
 *   click: 42
 *   capacity: 1000
 *   energyPerCraft: 1000
 *   hideAllRecipes: false
 *   script: your_script_name
 *   recipes:
 *     your_recipe_name:
 *       chooseOne: false
 *       forDisplay: false
 *       hide: false
 *       input:
 *         1:
 *           slot: 19
 *           material: IRON_INGOT
 *           amount: 64
 *         2:
 *           slot: 20
 *           material: IRON_INGOT
 *           amount: 64
 *       output:
 *         # free output
 *         1:
 *           material: GOLD_INGOT
 *         # linked output
 *         2:
 *           slot: 25
 *           material: OBSIDIAN
 *           amount: 7
 *         # chanced output
 *         3:
 *           material: DIAMOND
 *           chance: 50
 *
 */
public class WorkbenchReader extends YamlReader<CustomWorkbench> {
    @Override
    public String getFileName() {
        return Constants.WORKBENCHES_FILE;
    }

    public WorkbenchReader(File file, ProjectAddon addon) {
        super(file, addon);
    }

    @Override
    public CustomWorkbench readEach(String s) {
        ConfigurationSection section = configuration.getConfigurationSection(s);
        if (section == null) return null;
        String id = getId(s);
        var base = getBase(section, s);
        if (base == null) return null;

        CustomMenu menu = CommonUtils.getIf(addon.getMenus(), m -> m.getId().equalsIgnoreCase(id));
        if (menu == null) {
            Debug.warn(file, section, "Not foundmenu " + id + " (menu), menu");
        }

        List<Integer> input = section.getIntegerList("input");
        List<Integer> output = section.getIntegerList("output");

        if (input.isEmpty()) {
            Debug.error(file, section, "MissingConfiguration error '' (input)");
            return null;
        }

        if (output.isEmpty()) {
            Debug.error(file, section, "MissingConfiguration error '' (output)");
            return null;
        }

        if (isInvalidSlots(input, section, ItemTransportFlow.INSERT)
            || isInvalidSlots(output, section, ItemTransportFlow.WITHDRAW)) {
            return null;
        }

        int capacity = section.getInt("capacity", 0);
        if (capacity < 0) {
            Debug.error(file, section, "Configuration error 'Source' (capacity)", 0, Integer.MAX_VALUE);
            return null;
        }

        int energy = section.getInt("energyPerCraft", 0);
        if (energy < 0) {
            Debug.error(file, section, "Configuration error '' (energyPerCraft)", 0, Integer.MAX_VALUE);
            return null;
        }

        JavaScriptEval eval = getScriptOrNull(section, section.getString("script"));

        int click = section.getInt("click", -1);
        if (click < 0 || click > 53) {
            Debug.error(file, section, "MissingConfiguration error 'slot' (click)", 0, 53);
            return null;
        }

        var machine = new CustomWorkbench(
                base,
                input.stream().mapToInt(i -> i).toArray(),
                output.stream().mapToInt(i -> i).toArray(),
                energy,
                capacity,
                click,
                eval);
        var ticker = MachineTicker.create(file, machine, section, menu, addon, MachineTicker.Type.WORKBENCH);
        if (ticker == null) return null;
        machine.setTicker(ticker);
        return machine;
    }

    @Override
    public List<SlimefunItemStack> preloadItems(String s) {
        return blockPreloadItems(s);
    }
}
