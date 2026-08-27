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
    private final String addonId;
    private final String addonName;
    private final String addonVersion;
    private final List<String> pluginDepends;
    private final List<String> depends;
    private final String description;
    private final List<String> authors;
    private final File folder;
    private final Map<String, SlimefunItemStack> preloadItems = new ConcurrentHashMap<>();
    private @Nullable String gitHubRepo;
    private @Nullable String downloadZipName;
    private @Nullable String idPattern;
    private @Nullable AddonConfig config;
    private @Nullable ScriptableEventListener eventListener;
    private List<JavaScriptEval> scriptEvals = new CopyOnWriteArrayList<>();
    private List<ItemGroup> itemGroups = new ArrayList<>();
    private List<CustomMenu> menus = new ArrayList<>();
    private List<CustomGeoResource> geoResources = new ArrayList<>();
    private List<SlimefunItem> items = new ArrayList<>();
    private List<AbstractEmptyMachine<?>> machines = new ArrayList<>();
    private List<Research> researches = new ArrayList<>();
    private List<CustomGenerator> generators = new ArrayList<>();
    private List<AdvancedCustomMachine> materialGenerators = new ArrayList<>();
    private List<AdvancedCustomMachine> recipeMachines = new ArrayList<>();
    private List<CustomMultiBlockMachine> multiBlockMachines = new ArrayList<>();
    private List<CustomSolarGenerator> solarGenerators = new ArrayList<>();
    private List<CustomMobDrop> mobDrops = new ArrayList<>();
    private List<CustomCapacitor> capacitors = new ArrayList<>();
    private List<RecipeType> recipeTypes = new ArrayList<>();
    private List<SlimefunItem> simpleMachines = new ArrayList<>();
    private List<CustomFood> foods = new ArrayList<>();
    private List<CustomArmorPiece> armors = new ArrayList<>();
    private List<GenerationInfo> generationInfos = new ArrayList<>();
    private List<CustomWorkbench> workbenches = new ArrayList<>();
    private List<AdvancedCustomMachine> linkedRecipeMachines = new ArrayList<>();
    private List<AdvancedCustomMachine> templateMachines = new ArrayList<>();
    private List<CustomSuperMultiBlockMachine> superMultiBlockMachines = new ArrayList<>();
    private List<SlimefunItem> supers = new ArrayList<>();
    private List<DropFromBlock> dropFromBlocks = new ArrayList<>();
    private final AtomicInteger read = new AtomicInteger();
    private final AtomicInteger total = new AtomicInteger();

    public void unregisterListeners() {
        if (eventListener != null) {
            HandlerList.unregisterAll(eventListener);
        }
    }

    public void unregisterAll() {
        unregisterListeners();
        for (var item : new ArrayList<>(Slimefun.getRegistry().getAllSlimefunItems())) {
            if (item.getAddon() == null || item.getAddon() != org.lins.mmmjjkx.rykenslimefuncustomizer.RykenSlimefunCustomizer.INSTANCE) continue;
            unregister(item);
        }
    }

    private void unregister(SlimefunItem item) {
        if (item instanceof Radioactive resource) {
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
        var sf = SlimefunItem.getById(normalizedId);
        if (sf != null) {
            var item = sf.getItem();
            if (item instanceof SlimefunItemStack sfis) {
                return sfis;
            }

            // Slimefun Legacy and some addons may expose a plain ItemStack here.
            // RSC historically assumed every registered item was a SlimefunItemStack,
            // which caused ClassCastException while resolving cross-addon recipes.
            return new SlimefunItemStack(sf.getId(), item);
        }
        return getPreloadItems().get(normalizedId);
    }
}
