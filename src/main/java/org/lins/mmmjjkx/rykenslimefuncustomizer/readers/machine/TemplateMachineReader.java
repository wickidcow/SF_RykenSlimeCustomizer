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
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.menu.CustomMenu;
import org.lins.mmmjjkx.rykenslimefuncustomizer.readers.YamlReader;
import org.lins.mmmjjkx.rykenslimefuncustomizer.script.ScriptEval;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.CommonUtils;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.Constants;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.Debug;

import java.io.File;
import java.util.List;

public class TemplateMachineReader extends YamlReader<AdvancedCustomMachine> {
    @Override
    public String getFileName() {
        return Constants.TEMPLATE_MACHINES_FILE;
    }

    public TemplateMachineReader(File file, ProjectAddon addon) {
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

        List<Integer> input = section.getIntegerList("input");
        List<Integer> output = section.getIntegerList("output");

        // Template machines may legitimately have no ordinary input slots.
        // Their templateSlot supplies the selector/template item and the
        // template recipe reader already explicitly allows empty inputs.
        if (output.isEmpty()) {
            Debug.error(file, section, "MissingConfiguration error '' (output)");
            return null;
        }

        if (isInvalidSlots(input, section, ItemTransportFlow.INSERT)
            || isInvalidSlots(output, section, ItemTransportFlow.WITHDRAW)) {
            return null;
        }

        int capacity = section.getInt("capacity", -1);
        if (capacity <= 0) {
            Debug.error(file, section, "MissingConfiguration error 'Source' (capacity)", 1, Integer.MAX_VALUE);
            return null;
        }

        int energy = section.getInt("consumption", -1);
        if (energy <= 0) {
            Debug.error(file, section, "MissingConfiguration error '' (consumption)", 1, Integer.MAX_VALUE);
            return null;
        }

        int speed = section.getInt("speed", 1);
        if (speed <= 0) {
            Debug.error(file, section, "Configuration error 'Speed' (speed)", 1, Integer.MAX_VALUE);
            return null;
        }

        ScriptEval eval = getScriptOrNull(section, section.getString("script"));

        var machine = new AdvancedCustomMachine(
            base,
            input.stream().mapToInt(x -> x).toArray(),
            output.stream().mapToInt(x -> x).toArray(),
            energy,
            capacity,
            speed,
            eval
        );
        var ticker = MachineTicker.create(file, machine, section, menu, addon, MachineTicker.Type.TEMPLATE_RECIPE);
        if (ticker == null) return null;
        machine.setTicker(ticker);
        return machine;
    }

    @Override
    public List<SlimefunItemStack> preloadItems(String s) {
        return blockPreloadItems(s);
    }
}
