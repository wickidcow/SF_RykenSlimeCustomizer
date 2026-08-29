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

import com.balugaq.jeg.core.integrations.logitech.LogiTechIntegrationMain;
import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import me.matl114.logitech.core.Machines.AutoMachines.StackMGenerator;
import me.matl114.logitech.core.Machines.AutoMachines.StackMachine;
import me.matl114.logitech.core.Registries.RecipeSupporter;
import net.byteflux.libby.BukkitLibraryManager;
import net.byteflux.libby.Library;
import net.kyori.adventure.internal.properties.AdventureProperties;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lins.mmmjjkx.rykenslimefuncustomizer.addon.ProjectAddon;
import org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.PluginStateCache;
import org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.SaveditemsGroup;
import org.lins.mmmjjkx.rykenslimefuncustomizer.commands.MainCommand;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.CustomSuperMultiBlockMachine;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.generations.BlockPopulator;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.super_multiblock.SuperMultiBlockManager;
import org.lins.mmmjjkx.rykenslimefuncustomizer.listeners.DropFromBlockListener;
import org.lins.mmmjjkx.rykenslimefuncustomizer.listeners.RecipeViewListener;
import org.lins.mmmjjkx.rykenslimefuncustomizer.listeners.SuperMultiBlockListener;
import org.lins.mmmjjkx.rykenslimefuncustomizer.script.JavaScriptEval;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.CommonUtils;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.Debug;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.Keys;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.ReflectionUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * 　 　 　 　 　 　 　 　 ／＞　　フ
 * 　 　 　 　 　 　 　 　| 　_　 _|
 * 　 　 　 　 　 　 　 ／` ミ＿xノ
 * 　 　 　 　 　 　 /　　　 　 |
 * 　 　 　 　 　  /　 ヽ　　 ﾉ
 * 　 　 　 　 　 │　　|　|　|
 * 　／￣|　　 |　|　|　|　|
 *  | (￣ヽ＿_ヽ_)__)
 *  ＼二つ
 * 佛祖云：锁得住的是synchronized，锁不住的是我佛慈悲。
 * RSC 已然是庞然大物，任何功能都需要小心谨慎地添加
 * 一定要测试，一定要测试，一定要测试
 */
public final class RykenSlimefunCustomizer extends JavaPlugin implements SlimefunAddon {
    private static boolean runtime = false;

    public static RykenSlimefunCustomizer INSTANCE;
    public static ProjectAddonManager addonManager;
    public SuperMultiBlockManager smbm;
    public static boolean jeg = false;
    public static boolean logitech = false;
    public static @Nullable Set<String> logitechNotStackableIds = new HashSet<>();

    @Override
    public void onLoad() {
        setupLibraries();
        System.setProperty("polyglot.engine.WarnInterpreterOnly", "false");

        File cache = new File(getDataFolder(), "cache");
        System.setProperty("XDG_CACHE_HOME", cache.getAbsolutePath());
        System.setProperty("TRUFFLE_CACHE_DIR", cache.getAbsolutePath());
    }

    public static void clearScriptCache() {
        for (ProjectAddon addon : addonManager.getAllAddons()) {
            addon.getScriptEvals().forEach(JavaScriptEval::clearScriptCache);
        }
    }

    public SuperMultiBlockManager getSuperMultiBlockManager() {
        return INSTANCE.smbm;
    }

    public static void clearDisplayProjectiles() {
        for (World world : Bukkit.getWorlds()) {
            killRSCEntity(world.getEntitiesByClass(BlockDisplay.class));
//            killRSCEntity(world.getEntitiesByClass(Interaction.class));
        }
        SuperMultiBlockManager.getProjectiles().clear();
//        SuperMultiBlockManager.getInstance().getInteractions().clear();
    }

    public static void killRSCEntity(Collection<? extends Entity> entities) {
        for (Entity entity : entities) {
            if (!entity.getPersistentDataContainer().has(SuperMultiBlockManager.RSC_KEY) || !entity.isValid()) {
                return;
            }
            entity.remove();
        }
    }

    public static void reload() {
        INSTANCE.reloadConfig();
        addonManager.reload();

        if (INSTANCE.getConfig().getBoolean("saveExample")) {
            saveExample();
        }
    }

    @Override
    public void onEnable() {
        INSTANCE = this;

        if (!Boolean.TRUE.equals(AdventureProperties.TEXT_WARN_WHEN_LEGACY_FORMATTING_DETECTED.value())) {
            Debug.warn("=======================================================================");
            Debug.warn("Detected net.kyori.adventure.text.warnWhenLegacyFormattingDetected = false");
            Debug.warn("To avoid excessive legacy-format warnings, add the following JVM argument:               ");
            Debug.warn("-Dnet.kyori.adventure.text.warn_when_legacy_formatting_detected=false  ");
            Debug.warn("See https://docs.papermc.io/paper/reference/system-properties/#netkyoriadventuretextwarnwhenlegacyformattingdetected");
            Debug.warn("=======================================================================");
        }

        PluginStateCache.init();

        // Plugin startup logic
        CommonUtils.completeFile("config.yml");

        jeg = Bukkit.getPluginManager().isPluginEnabled("JustEnoughGuide");
        logitech = Bukkit.getPluginManager().isPluginEnabled("LogiTech");
        addonManager = new ProjectAddonManager();
        smbm = new SuperMultiBlockManager();

        if (getConfig().getBoolean("saveExample", false)) {
            saveExample();
        }

        getCommand("rykenslimefuncustomizer").setExecutor(new MainCommand());

        addonManager.setup();

        new DropFromBlockListener();
        new RecipeViewListener();
        new SuperMultiBlockListener();

        for (World world : Bukkit.getWorlds()) {
            world.getPopulators().add(new BlockPopulator());
        }

        if (jeg) {
            try {
                handleJEG();
            } catch (NoClassDefFoundError e) {
                Debug.warn("JustEnoughGuide is too old for this integration", e);
            } catch (IOException e) {
                Debug.warn("", e);
            }
        }
        getServer().getScheduler().runTaskLater(this, () -> runtime = true, 1);

        handleLogitech();

        Debug.info("============================");
        Debug.info("RykenSlimefunCustomizer loaded successfully!");
        Debug.info("Original author: lijinhong11");
        Debug.info("Maintainer: balugaq");
        Debug.info("Project: https://github.com/balugaq/RykenSlimeCustomizer");
        Debug.info("============================");
    }

    private void handleJEG() throws NoClassDefFoundError, IOException {
        Debug.info("JustEnoughGuide detected; enabling integration...");

        SaveditemsGroup itemGroup = new SaveditemsGroup(
            Keys.newKey("saveditems"),
            new CustomItemStack(Material.COMMAND_BLOCK, "&cSaved Items (RSC saveditems)"));

        SaveditemsGroup.instance = itemGroup;

        for (ProjectAddon addon : addonManager.getAllAddons()) {
            File savedItemsFolder = addon.getSavedItemsFolder();
            if (!savedItemsFolder.exists()) continue;

            String prjId = addon.getAddonId();

            try (var stream = Files.walk(savedItemsFolder.toPath());) {
                stream
                    .filter(path -> path.toFile().isFile() && (path.toString().endsWith(".yml") || path.toString().endsWith(".yaml")))
                    .forEach(path -> {
                        File file = path.toFile();
                        debug(() -> "Loading saveditem: " + file.toPath().toAbsolutePath());
                        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                        ItemStack item = config.getItemStack("item");
                        if (item == null) {
                            return;
                        }

                        // 计算相对于saveditems文件夹的路径
                        String relativePath = savedItemsFolder.toPath().relativize(path).toString();
                        // 移除文件扩展名
                        String pathWithoutExt =
                            relativePath.substring(0, relativePath.lastIndexOf("."));
                        // 格式: prjId;相对路径
                        String source = prjId + ";" + pathWithoutExt;

                        item.editMeta(meta -> {
                            meta.getPersistentDataContainer()
                                .set(SaveditemsGroup.SOURCE_KEY, PersistentDataType.STRING, source);
                        });
                        itemGroup.addItem(item);
                    });
            }
        }

        itemGroup.register(this);

    }

    private boolean isSMBStackable() {
        return getConfig().getBoolean("super-multi-block-stackable", false);
    }

    private boolean isNotStackable(SlimefunItem sf) {
        return (!isSMBStackable() && sf instanceof CustomSuperMultiBlockMachine)
            || (logitechNotStackableIds != null && logitechNotStackableIds.contains(sf.getId()));
    }

    private void handleLogitech() {
        if (!logitech) return;

        Bukkit.getScheduler().runTaskLaterAsynchronously(RykenSlimefunCustomizer.INSTANCE, () -> {
            // throws NoClassDefError
            // Don't allow CustomSuperMultiBlockMachine to be stackable
            if (!isSMBStackable()) {
                RecipeSupporter.BLACKLIST_MACHINECLASS.add(CustomSuperMultiBlockMachine.class);
            }

            Map<SlimefunItem, Integer> STACKMACHINE_LIST = RecipeSupporter.STACKMACHINE_LIST;
            Map<SlimefunItem, Integer> STACKMGENERATOR_LIST = RecipeSupporter.STACKMGENERATOR_LIST;
            var placeholder = new SlimefunItem(new ItemGroup(Keys.newKey("placeholder"), CommonUtils.createDefaultItem()), new SlimefunItemStack("RSC_PLACEHOLDER_ITEM", CommonUtils.createDefaultItem()), RecipeType.NULL, new ItemStack[0]);
            RecipeSupporter.MACHINE_RECIPELIST.put(placeholder, new ArrayList<>());

            List<SlimefunItem> bwm_instance = (List<SlimefunItem>) ReflectionUtil.getStaticValue(StackMachine.class, "BW_LIST", List.class);
            List<SlimefunItem> bwg_instance = (List<SlimefunItem>) ReflectionUtil.getStaticValue(StackMGenerator.class, "BW_LIST", List.class);

            int i = 0;
            for (var sf : new ArrayList<>(Slimefun.getRegistry().getAllSlimefunItems())) {
                if (sf.getAddon() != RykenSlimefunCustomizer.INSTANCE || !isNotStackable(sf)) continue;
                // only handle not stackable items
                if (STACKMACHINE_LIST.remove(sf) != null) {
                    Debug.debug(() -> " STACKMACHINE_LIST " + sf);
                    var idx = bwm_instance.indexOf(sf);
                    synchronized (bwm_instance) {
                        if (idx != -1) bwm_instance.set(idx, placeholder);
                    }
                    if (jeg) {
                        LogiTechIntegrationMain.stackableMachines.remove(sf);
                    }
                    i++;
                }
                if (STACKMGENERATOR_LIST.remove(sf) != null) {
                    Debug.debug(() -> " STACKMGENERATOR_LIST " + sf);
                    var idx = bwg_instance.indexOf(sf);
                    synchronized (bwg_instance) {
                        if (idx != -1) bwg_instance.set(idx, placeholder);
                    }
                    if (jeg) {
                        LogiTechIntegrationMain.stackableMaterialGenerators.remove(sf);
                    }
                    i++;
                }
            }

            logitechNotStackableIds = null; // gc
            Debug.info("machine! " + i + " machine");
        }, 300L); // wait recipe supporter
    }

    @Override
    public void onDisable() {
        for (World world : Bukkit.getWorlds()) {
            world.getPopulators().removeIf(x -> x instanceof BlockPopulator);
        }

        addonManager = null;
        smbm = null;

        // Plugin shutdown logic
        Debug.info("RykenSlimeCustomizer unloaded!");
    }

    @NonNull @Override
    public JavaPlugin getJavaPlugin() {
        return this;
    }

    @Override
    public String getBugTrackerURL() {
        return "https://github.com/balugaq/RykenSlimeCustomizer/issues";
    }

    private void setupLibraries() {
        String graalVersion = "25.1.3";
        BukkitLibraryManager libraryManager = new BukkitLibraryManager(this);

        for (String repo : getConfig().getStringList("repositories")) {
            libraryManager.addRepository(repo);
        }

        libraryManager.addMavenCentral();

        Library byteBuddy = Library.builder()
                .groupId("net{}bytebuddy")
                .artifactId("byte-buddy")
                .version("1.18.11")
                .build();
        Library graalJS = Library.builder()
                .groupId("org{}graalvm{}js")
                .artifactId("js-language")
                .version(graalVersion)
                .build();
        Library shadowedIcu4j = Library.builder()
                .groupId("org{}graalvm{}shadowed")
                .artifactId("icu4j")
                .version(graalVersion)
                .build();
        Library graalJSEngine = Library.builder()
                .groupId("org{}graalvm{}js")
                .artifactId("js-scriptengine")
                .version(graalVersion)
                .build();
        Library truffleAPI = Library.builder()
                .groupId("org{}graalvm{}truffle")
                .artifactId("truffle-api")
                .version(graalVersion)
                .build();
        Library truffleCompiler = Library.builder()
                .groupId("org{}graalvm{}truffle")
                .artifactId("truffle-compiler")
                .version(graalVersion)
                .build();
        Library truffleEnterprise = Library.builder()
                .groupId("org{}graalvm{}truffle")
                .artifactId("truffle-enterprise")
                .version(graalVersion)
                .build();
        Library truffleRuntime = Library.builder()
                .groupId("org{}graalvm{}truffle")
                .artifactId("truffle-runtime")
                .version(graalVersion)
                .build();
        Library polyglot = Library.builder()
                .groupId("org.graalvm.polyglot")
                .artifactId("polyglot")
                .version(graalVersion)
                .build();
        Library graalSdkCollections = Library.builder()
                .groupId("org{}graalvm{}sdk")
                .artifactId("collections")
                .version(graalVersion)
                .build();
        Library graalSdkNativeImage = Library.builder()
                .groupId("org{}graalvm{}sdk")
                .artifactId("nativeimage")
                .version(graalVersion)
                .build();
        Library graalSdkWord = Library.builder()
                .groupId("org{}graalvm{}sdk")
                .artifactId("word")
                .version(graalVersion)
                .build();
        Library graalSdkNativeBridge = Library.builder()
                .groupId("org{}graalvm{}sdk")
                .artifactId("nativebridge")
                .version(graalVersion)
                .build();
        Library graalJniUtils = Library.builder()
                .groupId("org{}graalvm{}sdk")
                .artifactId("jniutils")
                .version(graalVersion)
                .build();
        Library graalRegex = Library.builder()
                .groupId("org{}graalvm{}regex")
                .artifactId("regex")
                .version(graalVersion)
                .build();

        libraryManager.loadLibrary(byteBuddy);
        libraryManager.loadLibrary(graalJS);
        libraryManager.loadLibrary(graalJSEngine);
        libraryManager.loadLibrary(truffleAPI);
        libraryManager.loadLibrary(polyglot);
        libraryManager.loadLibrary(graalSdkCollections);
        libraryManager.loadLibrary(graalSdkNativeImage);
        libraryManager.loadLibrary(graalSdkWord);
        libraryManager.loadLibrary(shadowedIcu4j);
        libraryManager.loadLibrary(graalSdkNativeBridge);
        libraryManager.loadLibrary(graalJniUtils);
        libraryManager.loadLibrary(graalRegex);
        libraryManager.loadLibrary(truffleCompiler);
        libraryManager.loadLibrary(truffleEnterprise);
        libraryManager.loadLibrary(truffleRuntime);
    }

    public static boolean allowUpdate(String prjId) {
        if (runtime) return false;

        return INSTANCE.getConfig().getBoolean("update.auto")
                && !INSTANCE.getConfig().getStringList("update.blocks").contains(prjId);
    }

    public static void saveExample() {
        debug(() -> "Saving example addon");
        String head = "addons/example/info.yml";

        String filePath = new File(INSTANCE.getDataFolder(), head).getAbsolutePath();
        Path path = Paths.get(filePath);

        if (!Files.exists(path)) {
            INSTANCE.saveResource(head, true);
        }
    }

    public static void debug(Callable<String> callable) {
        Debug.debug(callable);
    }
}
