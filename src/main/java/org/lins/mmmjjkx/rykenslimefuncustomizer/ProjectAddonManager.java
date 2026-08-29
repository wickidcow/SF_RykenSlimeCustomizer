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
package org.lins.mmmjjkx.rykenslimefuncustomizer;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.lins.mmmjjkx.rykenslimefuncustomizer.addon.ProjectAddon;
import org.lins.mmmjjkx.rykenslimefuncustomizer.addon.ProjectAddonLoader;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.groups.BaseRSCItemGroup;
import org.lins.mmmjjkx.rykenslimefuncustomizer.events.AddonDisableEvent;
import org.lins.mmmjjkx.rykenslimefuncustomizer.events.AddonEnableEvent;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.Constants;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.Debug;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.RecipeTypeMap;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@NullMarked
public final class ProjectAddonManager {
    public static File ADDONS_DIRECTORY = new File(RykenSlimefunCustomizer.INSTANCE.getDataFolder(), "addons");
    public static File CONFIGS_DIRECTORY = new File(RykenSlimefunCustomizer.INSTANCE.getDataFolder(), "addon_configs");

    @Getter
    private final Map<String, File> projectIds = new ConcurrentHashMap<>(); // 查找所有文件夹
    private final Map<String, ProjectAddon> projectAddons = new ConcurrentHashMap<>(); // 查找已加载附属
    private final Map<String, Map<ItemStack[], ItemStack>> preaddRecipes = new ConcurrentHashMap<>();
    private final Set<File> scannedFiles = ConcurrentHashMap.newKeySet();

    @Getter
    @Setter
    @Nullable private String loadingAddon;

    @Getter
    @Setter
    private boolean lockingMainThread;

    public ProjectAddonManager() {
        if (!ADDONS_DIRECTORY.exists()) ADDONS_DIRECTORY.mkdirs();
        if (!CONFIGS_DIRECTORY.exists()) CONFIGS_DIRECTORY.mkdirs();
    }

    public void unloadAddon(ProjectAddon addon) {
        scannedFiles.remove(addon.getFolder());
        projectIds.remove(addon.getAddonId());
        projectAddons.remove(addon.getAddonId());
        addon.unregister();
        Bukkit.getPluginManager().callEvent(new AddonDisableEvent(addon));
    }

    public void checkSC(File prjFolder) {
        List<String> scFiles = List.of(
            "sc-addon.yml",
            "categories.yml",
            "mob-drops.yml",
            "geo-resources.yml",
            "solar-generators.yml",
            "material-generators.yml"
        );
        for (String scFile : scFiles) {
            File sc = new File(prjFolder, scFile);
            if (sc.exists()) {
                Debug.error(" SC RSC ! RSC addon! https://rsc.slimefun.cn/#/addon/sc-to-rsc");
                return;
            }
        }
        Debug.warn("Name " + prjFolder.getName() + " RSC addon!");
    }

    public boolean preloadAddon(File prjFolder) {
        File info = new File(prjFolder, Constants.INFO_FILE);
        if (!info.exists()) {
            checkSC(prjFolder);
            return false;
        }

        YamlConfiguration infoConfig = YamlConfiguration.loadConfiguration(info);
        String id = infoConfig.getString("id");
        if (id == null || id.isBlank()) {
            Debug.error(info, infoConfig, "Invalidaddon ID (id) !");
            return false;
        }

        if (projectIds.containsKey(id)) {
            Debug.error("ID : " + id + "RSC message" + projectIds.get(id).getName() + " addonaddon ID! addonUnable to!");
            return false;
        }
        projectIds.put(id, prjFolder);
        return true;
    }

    public boolean reloadAddon(ProjectAddon addon) {
        unloadAddon(addon);
        return loadAddon(addon.getFolder());
    }

    public boolean loadAddon(File prjFolder) {
        debug(() -> "Loading addon folder: " + prjFolder.getName());
        scannedFiles.add(prjFolder);
        ProjectAddonLoader loader = new ProjectAddonLoader(prjFolder);
        ProjectAddon addon = loader.load();
        setLoadingAddon(null);
        setLockingMainThread(false);
        if (addon == null) return false;
        projectAddons.put(addon.getAddonId(), addon);
        BaseRSCItemGroup.addItemsToGroups();
        Bukkit.getPluginManager().callEvent(new AddonEnableEvent(addon));
        return true;
    }

    public void setup() {
        checkFiles();
        File[] files = ADDONS_DIRECTORY.listFiles();
        if (files == null) return;

        List<File> folders = new ArrayList<>();
        for (File file : files) {
            if (file.isFile()) continue;
            if (preloadAddon(file)) folders.add(file);
        }

        for (File file : folders) {
            if (scannedFiles.contains(file)) continue; // has been scanned
            loadAddon(file);
        }

        Bukkit.getScheduler().runTaskLater(RykenSlimefunCustomizer.INSTANCE, BaseRSCItemGroup::addItemsToGroups, 1L);

        Debug.info("Loaded addons: ");
        for (ProjectAddon addon : projectAddons.values()) {
            Debug.info(addon.getAddonName() + " (" + addon.getAddonId() + ")" + " Version: " + addon.getAddonVersion());
        }
        Debug.info("RSC message" + projectAddons.size() + "addon");
    }

    public void checkFiles() {
        File folder = RykenSlimefunCustomizer.INSTANCE.getDataFolder();
        if (folder.listFiles() != null) {
            boolean b = Arrays.stream(Objects.requireNonNull(folder.listFiles()))
                    .anyMatch(f -> f.isFile() && f.getName().equals("info.yml"));
            if (b) {
                Debug.warn(" \"plugin/RykenSlimefunCustomizer/addons/addon\" , \"plugin/RykenSlimefunCustomizer\" ");
            }
        }
    }

    public void reload() {
        for (ProjectAddon addon : projectAddons.values()) {
            unloadAddon(addon);
        }

        RecipeTypeMap.clearRecipeTypes();

        setup();
    }

    public boolean isLoaded(File file) {
        return projectAddons.values().stream().map(ProjectAddon::getFolder).toList().contains(file);
    }

    public boolean isLoaded(String id) {
        return projectAddons.containsKey(id);
    }

    public boolean isLoaded(String... ids) {
        for (String id : ids) {
            if (!isLoaded(id)) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    public ProjectAddon get(String id) {
        return projectAddons.get(id);
    }

    public List<ProjectAddon> getAllAddons() {
        return new ArrayList<>(projectAddons.values());
    }

    public File getAddonFolder(String id) {
        return projectIds.get(id);
    }

    public Map<ItemStack[], ItemStack> getPreaddRecipes(String s) {
        return preaddRecipes.getOrDefault(s, new HashMap<>());
    }

    public void addPreaddRecipe(String s, ItemStack[] input, ItemStack output) {
        preaddRecipes.computeIfAbsent(s, k -> new HashMap<>()).put(input, output);
    }

    private void debug(Supplier<String> message) {
        Debug.debug(message.get());
    }
}
