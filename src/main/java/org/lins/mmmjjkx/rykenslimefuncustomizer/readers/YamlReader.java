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
package org.lins.mmmjjkx.rykenslimefuncustomizer.readers;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.libraries.dough.collections.Pair;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.lins.mmmjjkx.rykenslimefuncustomizer.RykenSlimefunCustomizer;
import org.lins.mmmjjkx.rykenslimefuncustomizer.addon.AddonConfig;
import org.lins.mmmjjkx.rykenslimefuncustomizer.addon.ProjectAddon;
import org.lins.mmmjjkx.rykenslimefuncustomizer.addon.ProjectAddonLoader;
import org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.PluginStateCache;
import org.lins.mmmjjkx.rykenslimefuncustomizer.script.JavaScriptEval;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.CommonUtils;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.Debug;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.DropFromBlock;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.MinecraftVersion;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public abstract class YamlReader<T> {
    private final List<String> lateInits;
    protected final File dir;
    protected final File file;
    protected final ProjectAddon addon;
    protected final YamlConfiguration configuration;
    public abstract String getFileName();

    public YamlReader(File dir, ProjectAddon addon) {
        this.dir = dir;
        this.file = new File(dir, getFileName());
        this.configuration = ProjectAddonLoader.readYml(dir, getFileName());
        this.lateInits = new ArrayList<>();
        this.addon = addon;
    }

    public abstract T readEach(String section);

    public List<SlimefunItemStack> getPreloadedItems(String key) {
        return preloadItems(key);
    }

    public final void preload() {
        for (String key : configuration.getKeys(false)) {
            ConfigurationSection section = configuration.getConfigurationSection(key);
            if (section == null) continue;
            ConfigurationSection register = section.getConfigurationSection("register");
            String id = addon.getId(key, section.getString("id_alias"));
            if (!checkForRegistration(key, register, id)) continue;

            List<SlimefunItemStack> items = getPreloadedItems(key);

            if (items == null || items.isEmpty()) continue;

            for (SlimefunItemStack item : items) {
                addon.getPreloadItems().put(item.getItemId(), item);
                Debug.debug("&a已预加载物品: " + item.getItemId());
            }
        }
    }

    protected final Pair<RecipeType, ItemStack[]> getRecipe(ConfigurationSection section, ProjectAddon addon) {
        String recipeTypeStr = section.getString("recipe_type", "NULL");

        RecipeType rt;
        if (section.getBoolean("piglin_trade_chance")) {
            rt = RecipeType.BARTER_DROP;
        } else {
            rt = CommonUtils.getRecipeType(recipeTypeStr.toUpperCase(Locale.ROOT));
            if (rt == null) {
                Debug.error("错误的配方类型 (recipe_type): " + recipeTypeStr);
                return new Pair<>(RecipeType.NULL, new ItemStack[0]);
            }
        }

        int recipeSize = rt.getKey().getKey().equalsIgnoreCase("infinity_forge") ? 36 : 9;
        return new Pair<>(rt, CommonUtils.readRecipe(file, section.getConfigurationSection("recipe"), addon, recipeSize));
    }

    @Nullable protected final SlimefunItemStack getPreloadItem(String itemId) {
        return addon.getPreloadItems().get(itemId);
    }

    public final List<T> readAll() {
        Debug.info("正在加载" + addon.getAddonId() + "/"
                + this.getClass()
                        .getSimpleName()
                        .substring(0, this.getClass().getSimpleName().length() - 6));
        List<T> objects = new ArrayList<>();
        for (String key : configuration.getKeys(false)) {
            ConfigurationSection section = configuration.getConfigurationSection(key);
            if (section == null) continue;

            Debug.debug("开始读取配置: " + key);

            ConfigurationSection register = section.getConfigurationSection("register");
            String id = addon.getId(key, section.getString("id_alias"));
            if (!checkForRegistration(key, register, id)) continue;

            if (section.getBoolean("lateInit", false)) {
                putLateInit(key);
                Debug.debug("检查结果：延迟加载");
                continue;
            }

            try {
                var object = readEach(key);
                if (object != null) {
                    addon.addLoadedObject();
                    objects.add(object);
                    Debug.debug("&aSUCCESS | 读取项" + key + "成功！");
                } else {
                    Debug.debug("&cFAILURE | 读取项" + key + "失败！");
                }
            } catch (Exception e) {
                Debug.warn(file, configuration, "无法读取配置 (" + key + ") ! 已跳过", e);
            }
        }
        Debug.info("附属" + addon.getAddonId() + " 已加载 " + getLoadingProgress(addon));
        return objects;
    }

    protected void putLateInit(String key) {
        lateInits.add(key);
    }

    public List<T> loadLateInits() {
        Debug.info("正在加载延迟项 " + addon.getAddonId() + "/"
                + this.getClass()
                        .getSimpleName()
                        .substring(0, this.getClass().getSimpleName().length() - 6));
        List<T> objects = new ArrayList<>();
        lateInits.forEach(key -> {
            Debug.debug("开始读取配置：" + key);
            try {
                var object = readEach(key);
                if (object != null) {
                    addon.addLoadedObject();
                    objects.add(object);
                    Debug.debug("&aSUCCESS | 读取项" + key + "成功！");
                } else {
                    Debug.debug("&cFAILURE | 读取项" + key + "失败！");
                }
            } catch (Exception e) {
                Debug.warn(file, configuration, "无法读取配置 (" + key + ") ! 已跳过", e);
            }
        });
        Debug.info("附属" + addon.getAddonId() + " 已加载 " + getLoadingProgress(addon));

        lateInits.clear();

        return objects;
    }

    public abstract List<SlimefunItemStack> preloadItems(String s);

    private boolean checkForRegistration(String key, ConfigurationSection section, String id) {
        if (section == null) return true;

        List<String> conditions = section.getStringList("conditions");
        boolean warn = section.getBoolean("warn", false);
        boolean unfinished = section.getBoolean("unfinished", false);
        boolean logitech_stackable = section.getBoolean("logitech_stackable", true);

        if (unfinished) return false;
        if (!logitech_stackable && RykenSlimefunCustomizer.logitechNotStackableIds != null) {
            RykenSlimefunCustomizer.logitechNotStackableIds.add(id);
            Debug.debug(() -> "物品 " + id + " 将不能被逻辑工艺-堆叠机器堆叠!");
        }

        for (String condition : conditions) {
            String[] splits = condition.split(" ");
            String head = splits[0];
            if (head.equalsIgnoreCase("hasplugin")) {
                if (splits.length != 2) {
                    Debug.error("读取" + key + "的注册条件时发现问题: hasplugin 仅需要一个参数");
                    continue;
                }
                boolean b = PluginStateCache.isEnabled(splits[1]);
                if (!b) {
                    if (warn) {
                        Debug.warn(key + "需要服务端插件" + splits[1] + "才能被注册");
                    }
                    return false;
                }
            } else if (head.equalsIgnoreCase("itemexist")) {
                if (splits.length != 2) {
                    Debug.error("读取" + key + "的注册条件时发现问题: itemexist 仅需要一个参数");
                    continue;
                }

                String itemId = splits[1];

                if (addon.getSfStack(itemId) == null) {
                    if (warn) {
                        Debug.warn(key + "需要物品" + splits[1] + "才能被注册");
                    }

                    return false;
                }
            } else if (head.equalsIgnoreCase("!itemexist")) {
                if (splits.length != 2) {
                    Debug.error("读取" + key + "的注册条件时发现问题: !itemexist 仅需要一个参数");
                    continue;
                }

                String itemId = splits[1];

                if (addon.getSfStack(itemId) != null) {
                    if (warn) {
                        Debug.warn(key + "需要物品" + splits[1] + "不存在才能被注册");
                    }

                    return false;
                }
            } else if (head.equalsIgnoreCase("!hasplugin")) {
                if (splits.length != 2) {
                    Debug.error("读取" + key + "的注册条件时发现问题: !hasplugin 仅需要一个参数");
                    continue;
                }
                boolean b = PluginStateCache.isEnabled(splits[1]);
                if (b) {
                    if (warn) {
                        Debug.warn(key + "需要卸载服务端插件 " + splits[1] + " 才能被注册");
                    }
                    return false;
                }
            } else if (head.equalsIgnoreCase("version")) {
                if (splits.length != 3) {
                    Debug.error("读取" + key + "的注册条件时发现问题: version 需要两个参数");
                    continue;
                }

                MinecraftVersion version;
                try {
                    version = MinecraftVersion.of(splits[2]);
                } catch (IllegalArgumentException ignored) {
                    Debug.error("读取" + key + "的注册条件时发现问题: 版本号 " + splits[2] + " 不是正常的版本号！");
                    continue;
                }

                var curr = MinecraftVersion.current();
                boolean pass = false;
                switch (splits[1]) {
                    case ">" -> pass = curr.compareTo(version) > 0;
                    case "<" -> pass = curr.compareTo(version) < 0;
                    case ">=" -> pass = curr.compareTo(version) >= 0;
                    case "<=" -> pass = curr.compareTo(version) <= 0;
                    case "==" -> pass = curr.compareTo(version) == 0;
                    case "!=" -> pass = curr.compareTo(version) != 0;
                    default -> {
                        Debug.warn("读取" + key + "的注册条件时发现问题: version 需要合法的比较符! 已跳过");
                        pass = true;
                    }
                }
                if (!pass) {
                    if (warn) {
                        Debug.warn(key + "需要服务端版本" + splits[1] + " " + splits[2] + "才能被注册");
                    }
                    return false;
                }
            } else if (head.contains("config")) {
                AddonConfig config = addon.getConfig();
                if (config == null) {
                    Debug.error("读取" + key + "的注册条件时发现问题: 无法获取配置");
                    continue;
                }

                switch (head) {
                    case "config.boolean" -> {
                        if (splits.length != 2) {
                            Debug.error("读取" + key + "的注册条件时发现问题: config.boolean需要一个参数");
                            continue;
                        }

                        if (!config.config().getBoolean(splits[1])) {
                            if (warn) {
                                Debug.warn(key + "需要配置选项" + splits[1] + "为true才能被注册");
                            }
                            return false;
                        }
                    }
                    case "config.string" -> {
                        if (splits.length != 3) {
                            Debug.error("读取" + key + "的注册条件时发现问题: config.string需要两个参数");
                            continue;
                        }

                        if (!Objects.equals(config.config().getString(splits[1]), splits[2])) {
                            if (warn) {
                                Debug.warn(key + "需要配置选项" + splits[1] + "为" + splits[2] + "才能被注册");
                            }
                            return false;
                        }
                    }
                    case "config.int" -> {
                        if (splits.length != 4) {
                            Debug.error("读取" + key + "的注册条件时发现问题: config.int需要三个参数");
                            continue;
                        }

                        String configKey = splits[1];
                        int current = config.config().getInt(splits[2]);
                        int destination = Integer.parseInt(splits[3]);

                        if (!intCheck(
                                splits[1],
                                key,
                                "config.int",
                                current,
                                destination,
                                (op) -> "需要配置选项" + configKey + op + splits[3] + "才能被注册",
                                warn)) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    private boolean intCheck(
            String operator,
            String key,
            String regParam,
            int current,
            int destination,
            Function<String, String> msg,
            boolean warn) {
        String operation = "";
        boolean b =
                switch (operator) {
                    case ">" -> {
                        operation = "大于";
                        yield current > destination;
                    }
                    case "<" -> {
                        operation = "小于";
                        yield current < destination;
                    }
                    case ">=" -> {
                        operation = "大于或等于";
                        yield current >= destination;
                    }
                    case "<=" -> {
                        operation = "小于或等于";
                        yield current <= destination;
                    }
                    case "==" -> {
                        operation = "等于";
                        yield current == destination;
                    }
                    case "!=" -> {
                        operation = "不等于";
                        yield current != destination;
                    }
                    default -> {
                        Debug.error("读取" + key + "的注册条件时发现问题: " + regParam + "需要合法的比较符！");
                        yield true;
                    }
                };

        if (!b) {
            if (warn) {
                Debug.warn(key + msg.apply(operation));
            }
        }

        return b;
    }

    public int getSize() {
        return configuration.getKeys(false).size();
    }

    private static String getLoadingProgress(ProjectAddon addon) {
        return addon.getLoadedObjects() + "/" + addon.getTotalObjects() + " ("
                + ((int) (((double) addon.getLoadedObjects()) / addon.getTotalObjects() * 10000)) / 100.0D + "%)";
    }

    public String getId(String s) {
        return addon.getId(s, configuration.getConfigurationSection(s).getString("id_alias"));
    }

    public @Nullable BaseResult getBase(@Nullable ConfigurationSection section, String s) {
        if (section == null) return null;
        String id = getId(s);

        if (!CommonUtils.passItemIdConflictCheck(id)) return null;

        ItemGroup group = CommonUtils.getItemGroup(addon, section.getString("item_group"));
        if (group == null) return null;

        SlimefunItemStack slimefunItemStack = getPreloadItem(id);
        if (slimefunItemStack == null) return null;

        Pair<RecipeType, ItemStack[]> recipePair = getRecipe(section, addon);
        RecipeType rt = recipePair.getFirstValue();
        ItemStack[] recipe = recipePair.getSecondValue();
        if (recipe == null) {
            Debug.warn(file, section, "Skipping " + id + " because its recipe contains an unresolved item.");
            return null;
        }

        ItemStack output = null;
        if (section.contains("recipeOutput")) {
            output = CommonUtils.readItem(file, section.getConfigurationSection("recipeOutput"), addon);
            if (output == null) {
                Debug.warn(file, section, "你设置了物品输出，但是输出物品无效 (recipeOutput) 已跳过");
                return null;
            }
        }

        if (output == null) {
            output = slimefunItemStack.clone();
            int amount = section.getInt("item.amount", 1);
            output.setAmount(amount);
        }

        SlimefunItemStack sfis = new SlimefunItemStack(slimefunItemStack.getItemId(), slimefunItemStack.asOne());

        return new BaseResult(group, sfis, rt, recipe, output);
    }

    @NullMarked
        public record BaseResult(ItemGroup itemGroup, SlimefunItemStack sfis, RecipeType recipeType,
                                 @Nullable ItemStack[] recipe, ItemStack output) {
    }

    public boolean isInvalidSlots(List<Integer> slots, ConfigurationSection section, ItemTransportFlow flow) {
        for (int slot : slots) {
            if (slot < 0 || slot > 53) {
                Debug.warn(file, section, "槽位超出范围! " + (flow == ItemTransportFlow.INSERT ? "'输入槽' (input)" : "'输出槽' (output)"), 0, 53);
                return true;
            }
        }
        return false;
    }

    public List<SlimefunItemStack> blockPreloadItems(String s) {
        return internalPreloadItems(s, true, true, true);
    }

    public List<SlimefunItemStack> anyPreloadItems(String s) {
        return internalPreloadItems(s, true, true, false);
    }

    private List<SlimefunItemStack> internalPreloadItems(String s, boolean noAir, boolean noLegacy, boolean mustBeBlock) {
        ConfigurationSection section = configuration.getConfigurationSection(s);
        if (section == null) return null;

        ConfigurationSection item = section.getConfigurationSection("item");
        if (item == null) {
            Debug.error(file, section, "缺失配置 '物品' (item)");
            return null;
        }


        ItemStack stack = CommonUtils.readItem(file, item, addon);
        if (stack == null) {
            Debug.error(file, item, "配置错误 '物品' (item)");
            return null;
        }

        if ((noAir && stack.getType().isAir())
            || (noLegacy && stack.getType().isLegacy())
            || (mustBeBlock && !stack.getType().isBlock())) {
            Debug.error(file, item, "机器物品材质必须为方块 '物品' (item)");
            return null;
        }

        return List.of(new SlimefunItemStack(getId(s), stack));
    }

    @Contract("_, null -> null")
    @Nullable
    public JavaScriptEval getScriptOrNull(ConfigurationSection section, @Nullable String script) {
        if (script == null) return null;
        String scriptName = script + ".js";
        File file = new File(addon.getScriptsFolder(), scriptName);
        if (!file.exists()) {
            Debug.warn(file, section, "找不到对应的脚本文件 (script), file=" + file.getAbsolutePath());
            return null;
        } else {
            var js = JavaScriptEval.create(file, addon);
            if (js != null) {
                Debug.debug(file, () -> "成功加载了脚本文件 " + scriptName);
            }
            return js;
        }
    }

    public static void resolveDropFrom(File file, ConfigurationSection section, SlimefunItemStack sfis, ProjectAddon addon) {
        int chance = 100;
        if (section.contains("drop_chance")) {
            chance = CommonUtils.clamp(section.getInt("drop_chance", 100), 1, 100, file, section, "'掉落概率 (drop_chance) 非法'");
        } else if (section.contains("chance")) {
            chance = CommonUtils.clamp(section.getInt("chance", 100), 1, 100, file, section, "'掉落概率 (chance) 非法'");
        }
        int amount = section.isInt("drop_amount") ? section.getInt("drop_amount", 1) : -1;

        String dropMaterial = section.getString("drop_from", "");
        Optional<Material> xm = CommonUtils.getMaterial(dropMaterial);
        if (xm.isEmpty()) {
            Debug.warn(file, section, "掉落方块材料类型 (drop_from) 无效 已跳过");
            return;
        }
        Material material = xm.get();
        if (amount != -1) {
            DropFromBlock.addDrop(material, new DropFromBlock.Drop(sfis, chance, addon, amount, amount));
            return;
        }

        int min, max, amt = sfis.getAmount();
        resolve_amount:
        {
            String between = section.getString("drop_amount", "1");
            if (between.contains("-")) {
                String[] split = between.split("-");
                if (split.length != 2) {
                    Debug.warn(file, section, "掉落数量区间 (drop_amount) 非法，已将掉落数量转为 " + amt);
                    min = max = amt;
                    break resolve_amount;
                }

                try {
                    min = Integer.parseInt(split[0]);
                    max = Integer.parseInt(split[1]);
                } catch (NumberFormatException e) {
                    Debug.warn(file, section, "掉落数量区间 (drop_amount) 非法，已将掉落数量转为 " + amt);
                    min = max = amt;
                }
            } else {
                try {
                    min = max = Integer.parseInt(between);
                } catch (NumberFormatException e) {
                    Debug.warn(file, section, "掉落数量 (drop_amount) 非法，已将掉落数量转为 " + amt);
                    min = max = 1;
                }
            }
        }

        DropFromBlock.addDrop(material, new DropFromBlock.Drop(sfis, chance, addon, min, max));
    }
}
