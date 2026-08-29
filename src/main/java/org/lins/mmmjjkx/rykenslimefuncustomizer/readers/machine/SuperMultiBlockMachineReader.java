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
import io.github.thebusybiscuit.slimefun4.libraries.dough.collections.Pair;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.lins.mmmjjkx.rykenslimefuncustomizer.addon.ProjectAddon;
import org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.tickers.MachineTicker;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.CustomSuperMultiBlockMachine;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.menu.CustomMenu;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.super_multiblock.BlockDisplayDescriptor;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.super_multiblock.CustomMultiBlockPart;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.super_multiblock.DisplayDescriptor;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.super_multiblock.HorizonDirection;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.super_multiblock.ItemDisplayDescriptor;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.super_multiblock.MultiBlockMultiBlockPart;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.super_multiblock.MultiBlockPart;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.super_multiblock.SlimefunMultiBlockPart;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.super_multiblock.SuperMultiBlockDefinition;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.super_multiblock.VanillaMultiBlockPart;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.super_multiblock.Vector3i;
import org.lins.mmmjjkx.rykenslimefuncustomizer.readers.YamlReader;
import org.lins.mmmjjkx.rykenslimefuncustomizer.script.JavaScriptEval;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.CommonUtils;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.Constants;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.Debug;

import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SuperMultiBlockMachineReader extends YamlReader<CustomSuperMultiBlockMachine> {
    @Override
    public String getFileName() {
        return Constants.SUPER_MULTI_BLOCK_MACHINES_FILE;
    }

    public SuperMultiBlockMachineReader(File file, ProjectAddon addon) {
        super(file, addon);
    }

    @Override
    public CustomSuperMultiBlockMachine readEach(String s) {
        ConfigurationSection section = configuration.getConfigurationSection(s);
        if (section == null) return null;
        String id = getId(s);
        var base = getBase(section, s);
        if (base == null) return null;

        CustomMenu menu = CommonUtils.getIf(addon.getMenus(), m -> m.getId().equalsIgnoreCase(id));
        if (menu == null) {
            Debug.warn("Not foundmenu " + id + " menu");
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

        int speed = section.getInt("speed", 1);
        if (speed <= 0) {
            Debug.error(file, section, "Configuration error 'Speed' (speed)", 1, Integer.MAX_VALUE);
            return null;
        }

        JavaScriptEval eval = getScriptOrNull(section, section.getString("script"));

        boolean displayProjectiles = section.getBoolean("displayProjectiles", true);
        boolean checkFormed = section.getBoolean("checkFormed", true);
        boolean openMenuWhenClickedParts = section.getBoolean("openMenuWhenClickedParts", true);
        boolean noMenu = section.getBoolean("noMenu", false);
        boolean noMenuWhenNotFormed = section.getBoolean("noMenuWhenNotFormed", true);
        boolean allowSwitchDisplayLayer = section.getBoolean("allowSwitchDisplayLayer", true);
        boolean defaultNotice = section.getBoolean("defaultNotice", true);

        SuperMultiBlockDefinition definition = readMultiBlockDefinition(section, s, eval);
        if (definition == null) return null;

        String redirectMenu = section.getString("redirectMenu");

        if (redirectMenu != null) {
            if (definition.getMapping().get(redirectMenu) == null) {
                Debug.error(file, section, "RSC: " + redirectMenu);
                return null;
            }

            if (definition.count(redirectMenu) != 1) {
                Debug.error(file, section, "menu " + redirectMenu + " Has 1 !");
                return null;
            }
        }

        var machine = new CustomSuperMultiBlockMachine(
                base,
                input.stream().mapToInt(x -> x).toArray(),
                output.stream().mapToInt(x -> x).toArray(),
                energy,
                capacity,
                speed,
                eval,
                definition,
                displayProjectiles,
                checkFormed,
                openMenuWhenClickedParts,
                noMenu,
                noMenuWhenNotFormed,
                allowSwitchDisplayLayer,
                defaultNotice,
                redirectMenu
        );

        var ticker = MachineTicker.create(file, machine, section, menu, addon, null);
        if (ticker == null) return null;
        machine.setTicker(ticker);
        return machine;
    }

    @Override
    public List<SlimefunItemStack> preloadItems(String s) {
        return blockPreloadItems(s);
    }

    @Nullable
    private SuperMultiBlockDefinition readMultiBlockDefinition(ConfigurationSection section, String s, @Nullable JavaScriptEval eval) {
        if (section == null) return null;
        if (!section.contains("structure")) {
            Debug.error(file, section, " (structure)");
            return null;
        }

        // structure是一个List<List<String>>，方块用一个字符串来表示，一个字符串里包含若干个字符串
        // 每个字符串空格分隔， 其中只以 "_" 构成的字符串，特殊，表示无方块，
        // List<String>表示一个平面结构
        // 和 structure 同级，有 mapping.字符串，表示每个字符串对应的方块描述
        // 例如：
        // structure:
        // - 
        //  - "__ b1 __"
        //  - "a1 b1 c1"
        //  - "__ b1 __"
        // -
        //  - "c1 b1 a1"
        //  - "c1 o  a1"
        //  - "c1 b1 a1"
        // -
        //  - "b1 a1 c1"
        //  - "b1 a1 c1"
        //  - "b1 a1 c1"
        // 考虑到多方块结构不一定是规则图形
        // 需要先确定 o 即core的位置，然后对其他进行向量化 -> Vector3i
        //

        var mps = section.getConfigurationSection("mapping");
        if (mps == null) {
            Debug.error(file, section, "block (mapping) ");
            return null;
        }
        Pair<Map<String, MultiBlockPart>, String> mappingAndCore = readMapping(mps, s, eval);
        if (mappingAndCore == null) return null;
        Map<String, MultiBlockPart> mapping = mappingAndCore.getFirstValue();
        String core = mappingAndCore.getSecondValue();

        List<?> structure0 = section.getList("structure");
        if (structure0 == null) {
            Debug.error(file, section, " (structure)");
            return null;
        }
        List<List<String>> structure = new ArrayList<>();
        for (Object o : structure0) {
            if (!(o instanceof List<?> st)) {
                Debug.error(file, section, " (structure) ");
                return null;
            }
            if (!(st.getFirst() instanceof String)) {
                Debug.error(file, section, " (structure) ");
                return null;
            }
            structure.add((List<String>) st);
        }
        if (structure.isEmpty()) {
            Debug.error(file, section, " (structure) ");
            return null;
        }

        Vector3i corePos = null;
        Map<Vector3i, String> blockPositions = new HashMap<>();
        for (int i = 0; i < structure.size(); i++) {
            List<String> layer = structure.get(i);
            for (int j = 0; j < layer.size(); j++) {
                String line = layer.get(j);
                String[] blocks = Arrays.stream(line.split(" ")).filter(k -> !k.isEmpty()).toArray(String[]::new);
                for (int k = 0; k < blocks.length; k++) {
                    String block = blocks[k];
                    if (isValidBlockDesc(block)) {
                        blockPositions.put(new Vector3i(j, -i, k), block);
                        if (block.equals(core)) {
                            if (corePos != null) {
                                Debug.error(file, section, " (structure) (core), Unable to (core)");
                                return null;
                            }

                            corePos = new Vector3i(j, -i, k);
                        }
                    }
                }
            }
        }
        
        if (corePos == null) {
            Debug.error(file, section, " (structure) (core)");
            return null;
        }

        MultiBlockPart corePart = mapping.get(core);
        if (corePart == null) {
            Debug.error(file, section, "block (mapping) (core)");
            return null;
        }
        Map<Vector3i, MultiBlockPart> blockParts = new HashMap<>();
        for (Vector3i pos : blockPositions.keySet()) {
            String blockDesc = blockPositions.get(pos);
            if (!mapping.containsKey(blockDesc)) {
                Debug.error(file, section, "block (mapping) : " + blockDesc);
                return null;
            }
            blockParts.put(pos.subtract(corePos), mapping.get(blockDesc));
        }

        blockParts.remove(corePos.subtract(corePos)); // 移除 core，避免影响多方块嵌套的情况
        var baseDirection = CommonUtils.getEnum(HorizonDirection.class, section.getString("baseDirection", "north"));
        if (baseDirection.isEmpty()) {
            Debug.error(file, section, "Invalid (baseDirection): " + baseDirection);
            return null;
        }

        return new SuperMultiBlockDefinition(mapping, blockParts, baseDirection.get());
    }

    public static boolean isValidBlockDesc(String block) {
        for (char c : block.toCharArray()) {
            if (c != '_') return true;
        }
        return false;
    }

    @Nullable
    private Pair<Map<String, MultiBlockPart>, String> readMapping(ConfigurationSection section, String s, @Nullable JavaScriptEval eval) {
        Map<String, MultiBlockPart> mapping = new HashMap<>();
        String core = null;
        for (String key : section.getKeys(false)) {
            var partSection = section.getConfigurationSection(key);
            if (partSection == null) {
                Debug.error(file, section, "block (mapping) Invalidblock: " + key);
                return null;
            }
            if (partSection.contains("core")) {
                if (core != null) {
                    Debug.error(file, section, "block (mapping) (core), Unable to (core)");
                    return null;
                }
                core = key;
            }
            MultiBlockPart part = readMultiBlockPart(partSection, s, eval, key);
            if (part == null) return null;
            mapping.put(key, part);
        }

        if (core == null) {
            Debug.error(file, section, "block (mapping) (core)");
            return null;
        }

        return new Pair<>(mapping, core);
    }

    @Nullable
    private MultiBlockPart readMultiBlockPart(ConfigurationSection section, String s, @Nullable JavaScriptEval eval, String mappingLocation) {
        // 读取多方块结构定义
        // material_type: mc / slimefun / custom
        // material: 方块/BlockData/粘液id
        // 对于 custom，由脚本代理检查
        String type = section.getString("material_type");
        if (type == null) {
            Debug.error(file, section, mappingLocation + " / block (material_type)");
            return null;
        }

        switch (type) {
            case "mc" -> {
                String material = section.getString("material");
                if (material == null) {
                    Debug.error(file, section, mappingLocation + " / block/block (material)");
                    return null;
                }

                VanillaMultiBlockPart r = CommonUtils.readPipe(material, part -> {
                    if (part.contains("[")) {
                        try {
                            BlockData blockData = Bukkit.createBlockData(part);
                            return new VanillaMultiBlockPart(blockData, readDisplayDescriptor(s, section, part, mappingLocation));
                        } catch (IllegalArgumentException e) {
                            Debug.error(file, section, mappingLocation + " / block (material) Invalid:" + part);
                            return null;
                        }
                    }

                    Optional<Material> m = CommonUtils.getMaterial(part);
                    if (m.isEmpty() || !m.get().isBlock() || m.get().isLegacy()) {
                        Debug.error(file, section, mappingLocation + " / (material) Invalid:" + part);
                        return null;
                    }

                    BlockData blockData = m.get().createBlockData();
                    return new VanillaMultiBlockPart(blockData, readDisplayDescriptor(s, section, part, mappingLocation));
                });

                if (r == null) {
                    Debug.error(file, section, mappingLocation + " / (material) Invalid:" + material);
                    return null;
                }

                return r;
            }
            case "slimefun" -> {
                String material = section.getString("material");
                if (material == null) {
                    Debug.error(file, section, mappingLocation + " / block (material)");
                    return null;
                }
                SlimefunMultiBlockPart r = CommonUtils.readPipe(material, part -> {
                    SlimefunItemStack item = addon.getSfStack(part);
                    if (item == null) {
                        Debug.error(file, section, mappingLocation + " / Not found (material): " + part);
                        return null;
                    }
                    if (!item.getType().isBlock()) {
                        Debug.error(file, section, mappingLocation + " / (material) block: " + part);
                        return null;
                    }
                    if (getPreloadedItems(s).contains(item)) {
                        return new MultiBlockMultiBlockPart(item);
                    } else {
                        return new SlimefunMultiBlockPart(item);
                    }
                });
                if (r == null) {
                    Debug.error(file, section, mappingLocation + " / (material) Invalid: " + material);
                    return null;
                }
                return r;
            }
            case "custom" -> {
                if (eval == null) {
                    Debug.error(file, section, mappingLocation + " / Missingscript, Unable tomultiblock");
                    return null;
                }
                return new CustomMultiBlockPart(eval, readDisplayDescriptor(s, section, mappingLocation));
            }
            default -> {
                Debug.error(file, section, mappingLocation + " / Invalidblock (material_type): " + type);
                return null;
            }
        }
    }

    @Nullable
    private DisplayDescriptor readDisplayDescriptor(String s, ConfigurationSection section, String material, String mappingLocation) {
        return CommonUtils.readPipe(material, part -> {
            if (material.contains("[")) {
                // blockdata
                try {
                    BlockData blockData = Bukkit.createBlockData(part);
                    return new BlockDisplayDescriptor(blockData);
                } catch (IllegalArgumentException e) {
                    Debug.error(file, section, mappingLocation + "/ block (material) Invalid:" + part);
                    return null;
                }
            }

            Material mt = Material.matchMaterial(part);
            if (mt == null || mt.isLegacy()) {
                Debug.error(file, section, mappingLocation + "/ (material) Invalid:" + part);
                return null;
            }

            // fallback
            if (mt == Material.WATER) return new ItemDisplayDescriptor(new ItemStack(Material.WATER_BUCKET));
            if (mt == Material.LAVA) return new ItemDisplayDescriptor(new ItemStack(Material.LAVA_BUCKET));
            if (mt == Material.AIR) return new ItemDisplayDescriptor(new ItemStack(Material.BUCKET));

            if (mt.isBlock()) return new BlockDisplayDescriptor(mt.createBlockData());

            Debug.error(file, section, mappingLocation + "/ (material) :" + part);
            return null;
        });
    }

    @Nullable
    private DisplayDescriptor readDisplayDescriptor(String s, ConfigurationSection section, String mappingLocation) {
        if (section.get("material") instanceof String material) {
            return readDisplayDescriptor(s, section, material, mappingLocation);
        }
        return null;
    }
}