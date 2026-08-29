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

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import org.bukkit.configuration.ConfigurationSection;
import org.lins.mmmjjkx.rykenslimefuncustomizer.RykenSlimefunCustomizer;
import org.lins.mmmjjkx.rykenslimefuncustomizer.addon.ProjectAddon;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.simple_machine.SimpleMachineFactory;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.simple_machine.SimpleMachineType;
import org.lins.mmmjjkx.rykenslimefuncustomizer.readers.YamlReader;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.CommonUtils;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.Constants;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.Debug;

import java.io.File;
import java.util.List;
import java.util.Optional;

public class SimpleMachineReader extends YamlReader<SlimefunItem> {
    @Override
    public String getFileName() {
        return Constants.SIMPLE_MACHINES_FILE;
    }

    public SimpleMachineReader(File file, ProjectAddon addon) {
        super(file, addon);
    }

    @Override
    public SlimefunItem readEach(String s) {
        ConfigurationSection section = configuration.getConfigurationSection(s);
        if (section == null) return null;
        var base = getBase(section, s);
        if (base == null) return null;

        String machineTypeStr = section.getString("type");

        Optional<SimpleMachineType> machineType = CommonUtils.getEnum(SimpleMachineType.class, machineTypeStr);
        if (machineType.isEmpty()) {
            Debug.error(file, section, "machine (type): " + machineTypeStr);
            return null;
        }

        ConfigurationSection settings = section.getConfigurationSection("settings");

        if (settings == null) {
            Debug.error(file, section, "Missingmachine (settings)");
            return null;
        }

        int capacity = 0;
        int consumption = 0;
        int speed = 1;
        int radius = 1;
        int repairFactor = 10;

        if (machineType.get().isEnergy()) {
            capacity = settings.getInt("capacity");
            if (capacity <= 0) {
                Debug.error(file, settings, "MissingConfiguration error 'Source' (capacity)", 1, Integer.MAX_VALUE);
                return null;
            }

            consumption = settings.getInt("consumption");
            if (consumption <= 0) {
                Debug.error(file, settings, "MissingConfiguration error 'Source' (consumption)", 1, Integer.MAX_VALUE);
                return null;
            }

            if (!isAccelerator(machineType.get())) {
                speed = settings.getInt("speed", 1);
                if (speed <= 0) {
                    Debug.error(file, settings, "Configuration error 'Speed' (speed)", 1, Integer.MAX_VALUE);
                    return null;
                }
            } else {
                radius = settings.getInt("radius", 1);
                if (radius < 1) {
                    Debug.error(file, settings, "Configuration error '' (radius)", 1, Integer.MAX_VALUE);
                    return null;
                }

                if (machineType.get() == SimpleMachineType.CROP_GROWTH_ACCELERATOR) {
                    speed = settings.getInt("speed", 1);
                    if (speed <= 0) {
                        Debug.error(file, settings, "Configuration error 'Speed' (speed)", 1, Integer.MAX_VALUE);
                        return null;
                    }
                }
            }

            if (machineType.get() == SimpleMachineType.AUTO_ANVIL) {
                repairFactor = settings.getInt("repair_factor", 10);
                if (repairFactor <= 0) {
                    Debug.error(file, settings, "Configuration error '' (repair_factor)", 1, Integer.MAX_VALUE);
                    return null;
                }
            }
        }

        SlimefunItem instance = SimpleMachineFactory.create(
                base,
                machineType.get(),
                capacity,
                consumption,
                speed,
                radius,
                repairFactor);

        instance.register(RykenSlimefunCustomizer.INSTANCE);

        return instance;
    }

    @Override
    public List<SlimefunItemStack> preloadItems(String s) {
        return blockPreloadItems(s);
    }

    private boolean isAccelerator(SimpleMachineType type) {
        return type == SimpleMachineType.TREE_GROWTH_ACCELERATOR
                || type == SimpleMachineType.CROP_GROWTH_ACCELERATOR
                || type == SimpleMachineType.ANIMAL_GROWTH_ACCELERATOR;
    }
}
