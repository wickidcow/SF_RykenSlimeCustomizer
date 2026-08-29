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
package org.lins.mmmjjkx.rykenslimefuncustomizer.addon;

import io.github.thebusybiscuit.slimefun4.api.geo.GEOResource;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.api.researches.Research;
import io.github.thebusybiscuit.slimefun4.core.attributes.Radioactive;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.AbstractEmptyMachine;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.AdvancedCustomMachine;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.CustomArmorPiece;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.CustomCapacitor;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.CustomFood;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.CustomGenerator;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.CustomGeoResource;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.CustomMobDrop;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.CustomMultiBlockMachine;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.CustomSolarGenerator;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.CustomSuperMultiBlockMachine;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.CustomWorkbench;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.generations.GenerationInfo;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.menu.CustomMenu;
import org.lins.mmmjjkx.rykenslimefuncustomizer.listeners.ScriptableEventListener;
import org.lins.mmmjjkx.rykenslimefuncustomizer.script.JavaScriptEval;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.DropFromBlock;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.RecipeTypeMap;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
@Setter(AccessLevel.PACKAGE)
@NullMarked
public final class ProjectAddon {
    // info.yml
    private final String addonId;
    private final String addonName;
    private final String addonVersion;
    private final List<String> pluginDepends;
    private final List<String> depends;
    private final String description;
    private final List<String> authors;
    private final File folder;
    //
    private final Map<String, SlimefunItemStack> preloadItems = new ConcurrentHashMap<>();
    //
    private @Nullable String gitHubRepo;
    private @Nullable String downloadZipName;
    private @Nullable String idPattern;
    //
    private @Nullable AddonConfig config;
    //
    private @Nullable ScriptableEventListener eventListener;
    //
    private List<JavaScriptEval> scriptEvals = new CopyOnWriteArrayList<>();
    // groups.yml
    private List<ItemGroup> itemGroups = new ArrayList<>();
    // menus.yml
    private List<CustomMenu> menus = new ArrayList<>();
    // geo_resources.yml
    private List<CustomGeoResource> geoResources = new ArrayList<>();
    // items.yml
    private List<SlimefunItem> items = new ArrayList<>();
    // machines.yml
    private List<AbstractEmptyMachine<?>> machines = new ArrayList<>();
    // researches.yml
    private List<Research> researches = new ArrayList<>();
    // generators.yml
    private List<CustomGenerator> generators = new ArrayList<>();
    // mat_generators.yml
    private List<AdvancedCustomMachine> materialGenerators = new ArrayList<>();
    // recipe_machines.yml
    private List<AdvancedCustomMachine> recipeMachines = new ArrayList<>();
    // mb_machines.yml
    private List<CustomMultiBlockMachine> multiBlockMachines = new ArrayList<>();
    // solar_generators.yml
    private List<CustomSolarGenerator> solarGenerators = new ArrayList<>();
    // mob_drops.yml
    private List<CustomMobDrop> mobDrops = new ArrayList<>();
    // capacitors.yml
    private List<CustomCapacitor> capacitors = new ArrayList<>();
    // recipe_types.yml
    private List<RecipeType> recipeTypes = new ArrayList<>();
    // simple_machines.yml
    private List<SlimefunItem> simpleMachines = new ArrayList<>();
    // foods.yml
    private List<CustomFood> foods = new ArrayList<>();
    // armors.yml
    private List<List<CustomArmorPiece>> armors = new ArrayList<>();
    // supers.yml
    private List<SlimefunItem> supers = new ArrayList<>();
    // template_machines.yml
    private List<AdvancedCustomMachine> templateMachines = new ArrayList<>();
    // linked_recipe_machines.yml
    private List<AdvancedCustomMachine> linkedRecipeMachines = new ArrayList<>();
    // workbenches.yml
    private List<CustomWorkbench> workbenches = new ArrayList<>();
    // super_multi_block_machines.yml
    private List<CustomSuperMultiBlockMachine> superMultiBlockMachines = new ArrayList<>();
    // generations.yml
    private List<GenerationInfo> generationInfos = new ArrayList<>();

    @Getter(AccessLevel.NONE)
    private final AtomicInteger loadedObjects = new AtomicInteger();
    @Getter(AccessLevel.NONE)
    private final AtomicInteger totalObjects = new AtomicInteger();

    public void addLoadedObject() {
        loadedObjects.incrementAndGet();
    }

    public void addTotalObjects(int totalObjects) {
        this.totalObjects.addAndGet(totalObjects);
    }

    public int getLoadedObjects() {
        return loadedObjects.get();
    }

    public int getTotalObjects() {
        return totalObjects.get();
    }

    public File getScriptsFolder() {
        File scripts = new File(folder, "scripts");
        if (!scripts.exists()) {
            scripts.mkdirs();
        }
        return scripts;
    }

    public File getSavedItemsFolder() {
        File savedItems = new File(folder, "saveditems");
        if (!savedItems.exists()) {
            savedItems.mkdirs();
        }
        return savedItems;
    }

    public void unregister() {
        itemGroups.forEach(ig -> Slimefun.getRegistry().getAllItemGroups().remove(ig));
        menus.forEach(m -> Slimefun.getRegistry().getMenuPresets().remove(m.getId()));
        items.forEach(this::unregisterItem);
        mobDrops.forEach(md -> {
            unregisterItem(md);
            var set = Slimefun.getRegistry().getMobDrops().get(md.getEntityType());
            if (set != null) {
                set.removeAll(md.getDrops());
            }
        });
        capacitors.forEach(this::unregisterItem);
        foods.forEach(this::unregisterItem);
        machines.forEach(this::unregisterItem);
        solarGenerators.forEach(this::unregisterItem);
        generators.forEach(this::unregisterItem);
        geoResources.forEach(this::unregisterItem);
        materialGenerators.forEach(this::unregisterItem);
        recipeMachines.forEach(this::unregisterItem);
        multiBlockMachines.forEach(this::unregisterItem);
        simpleMachines.forEach(this::unregisterItem);
        armors.forEach(l -> l.forEach(this::unregisterItem));
        supers.forEach(this::unregisterItem);
        templateMachines.forEach(this::unregisterItem);
        linkedRecipeMachines.forEach(this::unregisterItem);
        workbenches.forEach(this::unregisterItem);
        superMultiBlockMachines.forEach(this::unregisterItem);

        recipeTypes.forEach(r -> RecipeTypeMap.removeRecipeTypes(r.getKey().getKey()));

        // scripts.clear();
        scriptEvals.clear();
        items.clear();
        machines.clear();
        itemGroups.clear();
        menus.clear();
        geoResources.clear();
        generators.clear();
        materialGenerators.clear();
        recipeMachines.clear();
        multiBlockMachines.clear();
        capacitors.clear();
        solarGenerators.clear();
        mobDrops.clear();
        recipeTypes.clear();
        simpleMachines.clear();
        foods.clear();
        armors.clear();
        supers.clear();
        templateMachines.clear();
        linkedRecipeMachines.clear();
        workbenches.clear();
        superMultiBlockMachines.clear();
        generationInfos.clear();

        preloadItems.clear();

        DropFromBlock.unregisterAddonDrops(this);

        if (config != null) {
            if (config.onReloadHandler() != null) {
                config.onReloadHandler().close();
            }
        }

        if (eventListener != null) {
            HandlerList.unregisterAll(eventListener);
            eventListener = null;
        }
    }

    private void unregisterItem(SlimefunItem item) {
        if (item instanceof Radioactive) {
            Slimefun.getRegistry().getRadioactiveItems().remove(item);
        }

        if (item instanceof GEOResource resource) {
            Slimefun.getRegistry().getGEOResources().remove(resource.getKey());
        }

        Slimefun.getRegistry().getTickerBlocks().remove(item.getId());
        Slimefun.getRegistry().getEnabledSlimefunItems().remove(item);

        Slimefun.getRegistry().getSlimefunItemIds().remove(item.getId());
        Slimefun.getRegistry().getAllSlimefunItems().remove(item);
    }

    public String getId(String configuredId, @Nullable String id_alias) {
        String id = configuredId;
        if (id_alias != null) {
            id = id_alias;
        }

        if (idPattern != null) {
            // 当前使用的 id 可能是正常引用的 id，也可能是 idPattern 格式化后的 id
            // 如果找不到已初始化的 item，则尝试用 idPattern 格式化 id
            SlimefunItem item = SlimefunItem.getById(id.toUpperCase(Locale.ROOT));
            if (item == null) {
                id = idPattern.replaceAll("%0", id);
            }
        }

        return id.toUpperCase(Locale.ROOT);
    }

    @Nullable
    public SlimefunItemStack getSfStack(String id) {
        String normalizedId = id.toUpperCase(Locale.ROOT);

        // Exact registered and exact addon-owned IDs always win. This keeps RSC
        // internal IDs stable and only applies compatibility aliases when the
        // original item truly does not exist.
        var exact = toStack(SlimefunItem.getById(normalizedId));
        if (exact != null) return exact;

        var preload = getPreloadItems().get(normalizedId);
        if (preload != null) return preload;

        // InfinityExpansion2 renamed most IE1 IDs to IE_* and renamed a few
        // families entirely. Resolve those historical IDs only when the target
        // is actually registered, so unrelated missing-addon IDs still fail
        // normally instead of being replaced with the wrong item.
        String ie2Id = switch (normalizedId) {
            case "INFINITE_INGOT" -> "IE_INFINITY_INGOT";
            case "INFINITE_MACHINE_CIRCUIT" -> "IE_INFINITY_MACHINE_CIRCUIT";
            case "INFINITE_MACHINE_CORE" -> "IE_INFINITY_MACHINE_CORE";
            case "END_ESSENCE" -> "IE_ENDER_ESSENCE";
            case "INFINITY_FORGE" -> "IE_INFINITY_WORKBENCH";
            case "BASIC_STRAINER" -> "IE_STRAINER_1";
            case "ADVANCED_STRAINER" -> "IE_STRAINER_2";
            case "REINFORCED_STRAINER" -> "IE_STRAINER_3";
            case "BASIC_COBBLE_GEN" -> "IE_COBBLESTONE_GENERATOR";
            case "ADVANCED_COBBLE_GEN" -> "IE_COBBLESTONE_GENERATOR_2";
            case "INFINITY_COBBLE_GEN" -> "IE_COBBLESTONE_GENERATOR_4";
            case "BASIC_VIRTUAL_FARM" -> "IE_VIRTUAL_FARM";
            case "ADVANCED_VIRTUAL_FARM" -> "IE_VIRTUAL_FARM_2";
            case "INFINITY_VIRTUAL_FARM" -> "IE_VIRTUAL_FARM_4";
            case "BASIC_TREE_GROWER" -> "IE_TREE_GROWER";
            case "ADVANCED_TREE_GROWER" -> "IE_TREE_GROWER_2";
            case "INFINITY_TREE_GROWER" -> "IE_TREE_GROWER_4";
            case "BASIC_QUARRY" -> "IE_QUARRY";
            case "ADVANCED_QUARRY" -> "IE_QUARRY_2";
            case "VOID_QUARRY" -> "IE_QUARRY_3";
            case "INFINITY_QUARRY" -> "IE_QUARRY_4";
            case "INFINITE_VOID_HARVESTER" -> "IE_VOID_HARVESTER_3";
            case "INFINITY_CONSTRUCTOR" -> "IE_SINGULARITY_CONSTRUCTOR_2";
            case "INFINITY_DUST_EXTRACTOR" -> "IE_DUST_EXTRACTOR_4";
            case "INFINITY_INGOT_FORMER" -> "IE_INGOT_FORMER_4";
            case "BASIC_OBSIDIAN_GEN" -> "IE_OBSIDIAN_GENERATOR";
            case "POWERED_BEDROCK" -> "IE_POWERED_BEDROCK";
            case "HYDRO_GENERATOR" -> "IE_HYDRO_GENERATOR";
            case "REINFORCED_HYDRO_GENERATOR" -> "IE_HYDRO_GENERATOR_2";
            case "GEOTHERMAL_GENERATOR" -> "IE_GEOTHERMAL_GENERATOR";
            case "REINFORCED_GEOTHERMAL_GENERATOR" -> "IE_GEOTHERMAL_GENERATOR_2";
            case "BASIC_PANEL" -> "IE_SOLAR_PANEL";
            case "ADVANCED_PANEL" -> "IE_SOLAR_PANEL_2";
            case "CELESTIAL_PANEL" -> "IE_SOLAR_PANEL_3";
            case "VOID_PANEL" -> "IE_VOID_PANEL";
            case "INFINITE_PANEL" -> "IE_INFINITY_PANEL";
            case "EMPTY_DATA_CARD" -> "IE_MOB_DATA_CARD_EMPTY";
            case "DATA_INFUSER" -> "IE_MOB_DATA_INFUSER";
            case "BASIC_STORAGE" -> "IE_STORAGE_UNIT_2";
            case "ADVANCED_STORAGE" -> "IE_STORAGE_UNIT_3";
            case "REINFORCED_STORAGE" -> "IE_STORAGE_UNIT_4";
            case "VOID_STORAGE" -> "IE_STORAGE_UNIT_5";
            case "INFINITY_STORAGE" -> "IE_STORAGE_UNIT_6";
            default -> null;
        };

        var mapped = toStack(ie2Id == null ? null : SlimefunItem.getById(ie2Id));
        if (mapped != null) return mapped;

        if (normalizedId.endsWith("_DATA_CARD") && !normalizedId.equals("EMPTY_DATA_CARD")) {
            String mob = normalizedId.substring(0, normalizedId.length() - "_DATA_CARD".length());
            mapped = toStack(SlimefunItem.getById("IE_MOB_DATA_CARD_" + mob));
            if (mapped != null) return mapped;
        }

        if (normalizedId.startsWith("QUARRY_OSCILLATOR_")) {
            String resource = normalizedId.substring("QUARRY_OSCILLATOR_".length());
            mapped = toStack(SlimefunItem.getById("IE_OSCILLATOR_" + resource));
            if (mapped != null) return mapped;
        }

        if (!normalizedId.startsWith("IE_")) {
            mapped = toStack(SlimefunItem.getById("IE_" + normalizedId));
            if (mapped != null) return mapped;
        }

        return null;
    }

    private @Nullable SlimefunItemStack toStack(@Nullable SlimefunItem sf) {
        if (sf == null) return null;

        var item = sf.getItem();
        if (item instanceof SlimefunItemStack sfis) {
            return sfis;
        }

        // Slimefun Legacy and some addons can expose a plain ItemStack here.
        return new SlimefunItemStack(sf.getId(), item);
    }
}
