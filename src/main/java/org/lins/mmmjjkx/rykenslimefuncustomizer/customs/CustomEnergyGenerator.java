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
package org.lins.mmmjjkx.rykenslimefuncustomizer.customs;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetProvider;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNetComponentType;
import org.bukkit.Location;
import org.graalvm.polyglot.Value;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.menu.CustomMenu;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.script_machine.MachineRecord;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.script_machine.ScriptMachine;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.super_multiblock.SuperMultiBlockManager;
import org.lins.mmmjjkx.rykenslimefuncustomizer.readers.YamlReader;
import org.lins.mmmjjkx.rykenslimefuncustomizer.script.ScriptEval;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.Debug;

import java.util.List;

public class CustomEnergyGenerator extends ScriptMachine implements EnergyNetProvider {
    private final ScriptEval eval;
    private final int defaultOutput;

    public CustomEnergyGenerator(
            YamlReader.BaseResult base,
            @Nullable CustomMenu menu,
            List<Integer> input,
            List<Integer> output,
            MachineRecord record,
            EnergyNetComponentType type,
            @Nullable ScriptEval eval,
            int defaultOutput) {
        super(base, menu, input, output, record, type, eval);

        this.eval = eval;
        this.defaultOutput = defaultOutput;
    }

    @Override
    public int getGeneratedOutput(@NonNull Location l, @NonNull SlimefunBlockData data) {
        if (!SuperMultiBlockManager.canTick(l)) return 0;
        if (eval == null) {
            return defaultOutput;
        } else {
            try {
                Value result = eval.evalFunction("getGeneratedOutput", l, data);
                if (result != null) {
                    return result.asInt();
                } else {
                    Debug.warn(
                            "getGeneratedOutput() : " + result + " , addon!");
                    return defaultOutput;
                }
            } catch (Exception e) {
                return defaultOutput;
            }
        }
    }
}
