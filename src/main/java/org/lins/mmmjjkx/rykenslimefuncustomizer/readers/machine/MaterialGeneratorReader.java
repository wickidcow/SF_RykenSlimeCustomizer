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
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.AdvancedCustomMachine;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.CustomMaterialGenerator;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.menu.CustomMenu;
import org.lins.mmmjjkx.rykenslimefuncustomizer.readers.YamlReader;
import org.lins.mmmjjkx.rykenslimefuncustomizer.script.ScriptEval;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.CommonUtils;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.Constants;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.Debug;

import java.io.File;
import java.util.List;

public class MaterialGeneratorReader extends YamlReader<AdvancedCustomMachine> {

    @Override
    public String getFileName() {
        return Constants.MATERIAL_GENERATORS_FILE;
    }

    public MaterialGeneratorReader(File file, ProjectAddon addon) {
        super(file, addon);
    }

    @Override
    public AdvancedCustomMachine readEach(String s) {
        ConfigurationSection section = configuration.getConfigurationSection(s);
        if (section == null) return null;
        String id = getId(s);
        var base = getBase(section, s);
        if (base == null) return null;

        CustomMenu menu = CommonUtils.getIf(addon.getMenus(), m -> m.getId().equalsIgnoreCase(id));
        if (menu == null) {
            Debug.warn(file, section, "Not foundmenu " + id + " (menu), menu");
        }

        List<Integer> output = section.getIntegerList("output");

        if (output.isEmpty()) {
            Debug.error(file, section, "MissingConfiguration error '' (output)");
            return null;
        }

        if (isInvalidSlots(output, section, ItemTransportFlow.WITHDRAW)) {
            return null;
        }

        int capacity = section.getInt("capacity", 0);
        if (capacity < 0) {
            Debug.error(file, section, "Configuration error 'Source' (capacity)", 0, Integer.MAX_VALUE);
            return null;
        }

        int energy = section.getInt("per", 0);
        if (energy < 0) {
            Debug.error(file, section, "Configuration error '' (per)", 0, Integer.MAX_VALUE);
            return null;
        }

        int status = section.getInt("status", -1);

        ScriptEval eval = getScriptOrNull(section, section.getString("script"));

        AdvancedCustomMachine machine = new CustomMaterialGenerator(
            base,
            new int[0],
            output.stream().mapToInt(i -> i).toArray(),
            energy,
            capacity,
            eval,
            status
        );

        var ticker = MachineTicker.create(file, machine, section, menu, addon, MachineTicker.Type.MATERIAL_GENERATOR);
        if (ticker == null) return null;
        machine.setTicker(ticker);
        return machine;
    }

    @Override
    public List<SlimefunItemStack> preloadItems(String s) {
        return blockPreloadItems(s);
    }
}
