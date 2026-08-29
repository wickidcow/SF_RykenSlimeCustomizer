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
package org.lins.mmmjjkx.rykenslimefuncustomizer.utils;

import io.github.projectunified.uniitem.all.AllItemProvider;
import io.github.projectunified.uniitem.api.ItemKey;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.libraries.dough.skins.PlayerHead;
import io.github.thebusybiscuit.slimefun4.libraries.dough.skins.PlayerSkin;
import lombok.SneakyThrows;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkEffectMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.UnknownNullability;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.lins.mmmjjkx.rykenslimefuncustomizer.RykenSlimefunCustomizer;
import org.lins.mmmjjkx.rykenslimefuncustomizer.addon.ProjectAddon;
import org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.BuiltInItems;
import org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.wrappers.InputDesc;
import org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.wrappers.InputWrapper;
import org.lins.mmmjjkx.rykenslimefuncustomizer.libraries.colors.CMIChatColor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@NullMarked
public class CommonUtils {
    private static final Map<String, String> materialMappings = Map.of(
        "GRASS", "SHORT_GRASS",
        "SHORT_GRASS", "GRASS",
        "SCUTE", "TURTLE_SCUTE",
        "TURTLE_SCUTE", "SCUTE",
        "CHAIN", "IRON_CHAIN",
        "IRON_CHAIN", "CHAIN"
    );

    @Nullable
    public static <T> T getIf(@Nullable Iterable<T> iterable, Predicate<T> filter) {
        if (iterable == null) return null;

        for (T t : iterable) {
            if (filter.test(t)) {
                return t;
            }
        }
        return null;
    }

    public static Optional<Material> getMaterial(String s) {
        Material m = Material.matchMaterial(s.toUpperCase(Locale.ROOT));
        if (m == null) {
            var m2 = materialMappings.get(s.toUpperCase(Locale.ROOT));
            if (m2 == null) return Optional.empty();
            return Optional.ofNullable(Material.matchMaterial(m2));
        }
        return Optional.of(m);
    }

    public static @Nullable ItemStack[] readRecipe(File file, @Nullable ConfigurationSection section, ProjectAddon addon) {
        return readRecipe(file, section, addon, 9);
    }

    public static @Nullable ItemStack[] readRecipe(File file, @Nullable ConfigurationSection section, ProjectAddon addon, int size) {
        if (section == null) return new ItemStack[size];
        @Nullable ItemStack[] itemStacks = new ItemStack[size];
        for (int i = 0; i < size; i++) {
            ConfigurationSection item = section.getConfigurationSection(String.valueOf(i + 1));
            if (item == null) continue;

            ItemStack stack = readItem(file, item, addon);
            if (stack == null) {
                Debug.warn(file, item, "Skipping recipe because an ingredient could not be resolved.");
                return null;
            }
            itemStacks[i] = stack;
        }
        return itemStacks;
    }

    public static List<InputWrapper> readInputs(File file, @Nullable ConfigurationSection section, ProjectAddon addon, boolean allNoConsume) {
        if (section == null) return Collections.emptyList();
        List<InputDesc> descs = new ArrayList<>();
        for (String k : section.getKeys(false)) {
            ConfigurationSection item = section.getConfigurationSection(k);
            if (item == null) continue;
            var stack = readItem(file, item, addon);
            if (stack == null) {
                Debug.warn(file, item, "Skipping machine recipe because an input item could not be resolved.");
                return Collections.emptyList();
            }
            descs.add(new InputDesc(stack, item.getInt("slot", -1), allNoConsume || item.getBoolean("noConsume", false)));
        }

        List<InputWrapper> wrappers = new ArrayList<>();
        for (var desc : descs) {
            boolean matched = false;
            for (var wrapper : wrappers) {
                // pre-merge all items
                if (StackUtils.itemsMatch(wrapper.getStack(), desc.itemStack())) {
                    wrapper.merge(desc, true);
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                wrappers.add(InputWrapper.create(desc));
            }
        }
        return wrappers;
    }

    public static List<InputWrapper> readInputs(ItemStack[] stacks, boolean allNoConsume) {
        List<InputDesc> descs = new ArrayList<>();
        for (var stack : stacks) {
            descs.add(new InputDesc(stack, -1, allNoConsume));
        }

        List<InputWrapper> wrappers = new ArrayList<>();
        for (var desc : descs) {
            boolean matched = false;
            for (var wrapper : wrappers) {
                // pre-merge all items
                if (StackUtils.itemsMatch(wrapper.getStack(), desc.itemStack())) {
                    wrapper.merge(desc, true);
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                wrappers.add(InputWrapper.create(desc));
            }
        }
        return wrappers;
    }

    @SneakyThrows
    @Nullable public static ItemStack readItem(File file, @Nullable ConfigurationSection section, ProjectAddon addon) {
        if (section == null) return null;

        String type = section.getString("material_type", "mc");
        if (!type.equalsIgnoreCase("none") && (!section.contains("material") || section.getString("material") == null)) {
            Debug.error(file, section, ", Has! (material)");
            return null;
        }

        String material = section.getString("material", "");
        List<String> lore = CMIChatColor.translate(section.getStringList("lore"));
        String name = CMIChatColor.translate(section.getString("name", ""));
        boolean glow = section.getBoolean("glow", false);
        boolean hasEnchantment = section.contains("enchantments") && section.isList("enchantments");
        int modelId = section.getInt("modelId");
        int amount = section.getInt("amount", 1);

        return readItem(file, section, addon, type, material.trim(), name, lore, glow, hasEnchantment, modelId, amount);
    }

    private static void tryReadColor(File file, ConfigurationSection section, ItemMeta meta) {
        String color = section.getString("color");
        if (color == null) return; // skip

        String[] parts = color.split(",");
        if (parts.length != 3) {
            Debug.warn(file, section, "item (color) : " + Arrays.toString(parts) + " skipped");
            return;
        }

        try {
            Color bkcolor = Color.fromRGB(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));

            switch (meta) {
                case LeatherArmorMeta lam -> {
                    lam.setColor(bkcolor);
                }
                case PotionMeta pm -> {
                    pm.setColor(bkcolor);
                    pm.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
                }
                case FireworkEffectMeta fem -> {
                    fem.setEffect(FireworkEffect.builder()
                        .withColor(bkcolor)
                        .build());
                    fem.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
                }
                default -> {
                    Debug.warn(file, section, "itemitem (color): " + meta.getClass().getSimpleName() + " skipped");
                }
            }
        } catch (NumberFormatException e) {
            Debug.warn("item (color) : " + color + " skipped");
            return;
        }
    }

    @Nullable
    private static ItemStack getBaseItemStack(File file, ConfigurationSection section, String type, String material, ProjectAddon addon) {
        switch (type.toLowerCase(Locale.ROOT)) {
            case "none" -> {
                return new ItemStack(Material.AIR);
            }
            case "skull_hash" -> {
                PlayerSkin playerSkin = PlayerSkin.fromHashCode(material);
                ItemStack head = PlayerHead.getItemStack(playerSkin);
                return new CustomItemStack(head);
            }
            case "skull_base64", "skull" -> {
                PlayerSkin playerSkin = PlayerSkin.fromBase64(material);
                ItemStack head = PlayerHead.getItemStack(playerSkin);
                return new CustomItemStack(head);
            }
            case "skull_url" -> {
                PlayerSkin playerSkin = PlayerSkin.fromURL(material);
                ItemStack head = PlayerHead.getItemStack(playerSkin);
                return new CustomItemStack(head);
            }
            case "slimefun", "sf" -> {
                SlimefunItemStack sfis = addon.getSfStack(material);
                if (sfis != null) return sfis.clone();

                Debug.warn(file, section, "Unable toitem: " + material);
                return null;
            }
            case "uniitem" -> {
                try {
                    AllItemProvider provider = new AllItemProvider();
                    String[] split = material.split("::");
                    ItemStack item = provider.item(new ItemKey(split[0], split[1]));
                    if (item == null) {
                        Debug.warn(file, section, "Unable to UniItem item!");
                        return null;
                    }

                    item.setAmount(1);

                    return item;
                } catch (NoClassDefFoundError e) {
                    Debug.warn(file, section, "Unable to UniItem Dependencies! Unable toitem.", e);
                    return null;
                }
            }
            case "saveditem" -> {
                File saveditemFile = new File(addon.getSavedItemsFolder(), material + ".yml");
                if (!saveditemFile.exists()) {
                    Debug.warn(file, section, "item: " + material);
                    return null;
                }

                var cfg = YamlConfiguration.loadConfiguration(saveditemFile);

                fixVersionCode(saveditemFile);

                ItemStack itemStack = cfg.getItemStack("item");
                if (itemStack != null) {
                    return itemStack;
                } else {
                    Debug.warn(file, section, "Unable toitem: " + material);
                    return null;
                }
            }
            case "mc", "minecraft", "vanilla" -> {
                if (material.startsWith("minecraft:")) material = material.substring(10);
                Optional<Material> mat = getMaterial(material);
                if (mat.isEmpty()) {
                    Debug.warn(file, section, "Unable toitem: " + material);
                    return null;
                }

                CustomItemStack stack = new CustomItemStack(mat.get());
                stack.editMeta(meta -> {
                    tryReadColor(file, section, meta);
                });

                return stack;
            }
            case "built_in" -> {
                var stack = BuiltInItems.createStack(material);
                if (stack == null) {
                    Debug.warn(file, section, "Unable toitem: " + material);
                    return null;
                }
                return stack;
            }
            default -> {
                Debug.warn(file, section, "Unable to: " + type + " item...");
                var mc = getBaseItemStack(file, section, "mc", material, addon);
                if (mc != null) return mc;
                Debug.warn(file, section, "Unable to: " + type + " item...");
                var sf = getBaseItemStack(file, section, "slimefun", material, addon);
                if (sf != null) return sf;
                Debug.warn(file, section, "Unable to: " + type + " Unable to!");
                return null;
            }
        }
    }

    @SneakyThrows
    @SuppressWarnings("deprecation")
    public static @Nullable ItemStack readItem(
            File file,
            ConfigurationSection section,
            ProjectAddon addon,
            String type,
            String material,
            String name,
            List<String> lore,
            boolean glow,
            boolean hasEnchantment,
            int modelId,
            int amount) {

        if (material.startsWith("ey") || material.startsWith("ew")) {
            type = "skull";
        } else if (material.startsWith("http") || material.startsWith("https")) {
            type = "skull_url";
        } else if (material.matches("^[0-9A-Fa-f]{64}+$")) {
            type = "skull_hash";
        }

        String finalType = type;
        ItemStack itemStack;
        try {
            itemStack = CommonUtils.readPipe(material, s -> getBaseItemStack(file, section, finalType, s, addon));
            if (itemStack == null) {
                Debug.warn(file, section, "Unable to resolve item: " + material + ". The containing item or recipe will be skipped.");
                return null;
            }
        } catch (Exception e) {
            Debug.warn(file, section, "Failed to load item: " + material + ". The containing item or recipe will be skipped.", e);
            return null;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (modelId > 0) meta.setCustomModelData(modelId);
        if (!name.isBlank()) meta.setDisplayName(name);
        if (!lore.isEmpty()) meta.setLore(lore);

        itemStack.setItemMeta(meta);

        if (amount > 100 || amount < -1) {
            Debug.warn(file, section, "item (amount) : " + amount, -1, 100);
        } else {
            itemStack.setAmount(amount);
        }

        if (hasEnchantment) {
            List<String> enchants = section.getStringList("enchantments");
            for (String enchant : enchants) {
                String[] s2 = enchant.split(" ");
                if (s2.length != 2) {
                    Debug.warn(file, section, " (enchantments): " + enchant + " skipped");
                    continue;
                }

                String enchantName = s2[0];

                Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(enchantName.toLowerCase(Locale.ROOT)));
                if (enchantment == null) {
                    Debug.warn(file, section, " (enchantments): " + enchantName + " skipped");
                    continue;
                }

                try {
                    int lvl = Integer.parseInt(s2[1]);
                    itemStack.addUnsafeEnchantment(enchantment, lvl);
                } catch (NumberFormatException e) {
                    Debug.warn(file, section, " (enchantments): " + enchant + " skipped");
                    continue;
                }
            }
        }

        if (glow) {
            Enchantment glowEnchantment = Enchantment.getByKey(NamespacedKey.minecraft("luck_of_the_sea"));
            if (glowEnchantment != null) {
                itemStack.addUnsafeEnchantment(glowEnchantment, 1);
            }
            itemStack.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        return itemStack;
    }

    public static String getItemName(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return "Air";
        ItemMeta meta = stack.getItemMeta();
        if (meta != null && meta.hasDisplayName()) return meta.getDisplayName();
        String key = stack.getType().getKey().getKey().replace('_', ' ');
        StringBuilder name = new StringBuilder(key.length());
        boolean upper = true;
        for (char c : key.toCharArray()) {
            if (upper && Character.isLetter(c)) {
                name.append(Character.toUpperCase(c));
                upper = false;
            } else {
                name.append(c);
                if (c == ' ') upper = true;
            }
        }
        return name.toString();
    }

    public static CustomItemStack createDefaultItem() {
        return new CustomItemStack(Material.STONE);
    }

    @SuppressWarnings("deprecation")
    public static void addLore(ItemStack stack, boolean emptyLine, String... lore) {
        ItemMeta im = stack.getItemMeta();
        var lorel = im.getLore();
        if (lorel != null) {
            if (emptyLine) {
                lorel.add("");
            }
            lorel.addAll(CMIChatColor.translate(Arrays.asList(lore)));
        } else {
            lorel = CMIChatColor.translate(Arrays.asList(lore));
        }
        im.setLore(lorel);
        stack.setItemMeta(im);
    }

    public static void saveItem(ItemStack item, String fileName, ProjectAddon addon) {
        File folder = addon.getSavedItemsFolder();
        if (!folder.exists()) {
            folder.mkdirs();
        }
        File file = new File(folder, fileName + ".yml");
        if (!file.exists()) {
            try {
                Files.createFile(file.toPath());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        YamlConfiguration configuration = new YamlConfiguration();

        configuration.set("item", item);

        try {
            configuration.save(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void completeFile(String resourceFile) {
        JavaPlugin plugin = RykenSlimefunCustomizer.INSTANCE;

        InputStream stream = plugin.getResource(resourceFile);
        File file = new File(plugin.getDataFolder(), resourceFile);
        if (!file.exists()) {
            if (stream != null) {
                plugin.saveResource(resourceFile, false);
                return;
            }
            return;
        }
        if (stream == null) {
            Debug.error("Unable to " + resourceFile + "RSC message");
            return;
        }
        try {
            YamlConfiguration configuration = YamlConfiguration.loadConfiguration(new InputStreamReader(stream));
            YamlConfiguration configuration2 = new YamlConfiguration();
            configuration2.load(file);

            completeFile(configuration, configuration2);
            configuration2.save(file);
        } catch (Exception e) {
            Debug.error("Unable to " + resourceFile + "RSC message", e);
        }
    }

    private static void fixVersionCode(File file) {
        try {
            String fileContext = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            Pattern p = Pattern.compile("v: \\S\\d*");

            Matcher matcher = p.matcher(fileContext);
            if (matcher.find()) {
                int s = matcher.start();
                int e = matcher.end();
                String replace = fileContext.substring(s, e);
                int v = Integer.parseInt(replace.replace("v: ", ""));

                if (v > Bukkit.getUnsafe().getDataVersion()) {
                    String r2 = replace.replaceFirst(
                        String.valueOf(v),
                        String.valueOf(Bukkit.getUnsafe().getDataVersion()));
                    fileContext = fileContext.replace(replace, r2);
                    Files.writeString(
                        file.toPath(),
                        fileContext,
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING);
                }
            }
        } catch (IOException e) {
            Debug.error("", e);
        }
    }

    public static void completeFile(YamlConfiguration origin, YamlConfiguration dest) {
        for (String key : origin.getKeys(true)) {
            Object value = origin.get(key);
            if (value instanceof List<?>) {
                List<?> list2 = dest.getList(key);
                if (list2 == null) {
                    dest.set(key, value);
                    continue;
                }
            }

            if (!dest.contains(key)) {
                dest.set(key, value);
            }
        }
    }

    public static int versionToCode(String s) {
        String[] ver = s.split("\\.");
        String ver2 = "";
        for (String v : ver) {
            ver2 = ver2.concat(v);
        }

        if (ver.length == 2) {
            ver2 = ver2.concat("0");
        }

        return Integer.parseInt(ver2);
    }

    public static String richFormatSeconds(int seconds) {
        String lore = "RSC message" + seconds + "&es";
        if (seconds > 60) {
            lore = lore.concat("(" + CommonUtils.formatSeconds(seconds) + "&e)");
        }
        return lore;
    }

    public static String formatSeconds(int seconds) {
        if (seconds < 60) {
            return "&b" + seconds + "&es";
        } else if (seconds > 60 && seconds < 3600) {
            int m = seconds / 60;
            int s = seconds % 60;
            return "&b" + m + "&emin" + (s != 0 ? "&b" + s + "&es" : "");
        } else {
            int h = seconds / 3600;
            int m = (seconds % 3600) / 60;
            int s = (seconds % 3600) % 60;
            return "&b" + h + "&eh" + (m != 0 ? "&b" + m + "&emin" : "") + (s != 0 ? "&b" + s + "&es" : "");
        }
    }

    public static ItemStack[] removeNulls(@Nullable ItemStack[] origin) {
        int count = 0;
        for (ItemStack element : origin) {
            if (element != null) {
                count++;
            }
        }

        ItemStack[] newArray = new ItemStack[count];

        int index = 0;
        for (ItemStack element : origin) {
            if (element != null) {
                newArray[index] = element;
                index++;
            }
        }

        return newArray;
    }

    @Contract("null, _ -> null")
    @UnknownNullability
    public static <T> T readPipe(@Nullable String s, Function<String, @Nullable T> parser) {
        if (s == null) return null;
        for (String part : Arrays.stream(s.split("\\|")).map(String::trim).toList()) {
            T r = parser.apply(part);
            if (r != null) return r;
        }
        return null;
    }

    public static int clamp(int v, int a, int b, File file, ConfigurationSection section, String msg) {
        if (v < a) {
            Debug.warn(file, section, msg + "RSC message" + a, a, b);
            v = a;
        }

        if (v > b) {
            Debug.warn(file, section, msg + "RSC message" + b, a, b);
            v = b;
        }

        return v;
    }

    public static float clamp(float v, float a, float b, File file, ConfigurationSection section, String msg) {
        if (v < a) {
            Debug.warn(file, section, msg + "RSC message" + a, a, b);
            v = a;
        }

        if (v > b) {
            Debug.warn(file, section, msg + "RSC message" + b, a, b);
            v = b;
        }

        return v;
    }

    public static float clamp(float v, float a, float def, float b, File file, ConfigurationSection section, String msg) {
        if (v < a) {
            Debug.warn(file, section, msg + "RSC message" + def, a, b);
            v = a;
        }

        if (v > b) {
            Debug.warn(file, section, msg + "RSC message" + def, a, b);
            v = b;
        }

        return v;
    }

    public static <T extends Enum<T>> Optional<T> getEnum(Class<T> enumClass, @Nullable String name) {
        return Optional.ofNullable(readPipe(name, n -> {
            try {
                var values = enumClass.getEnumConstants();
                if (values == null) return null;
                T bValue = null;
                for (T enumValue : values) {
                    // 模糊匹配
                    if (enumValue.name().equalsIgnoreCase(name)) {
                        bValue = enumValue;
                    }
                    if (enumValue.name().equals(name)) {
                        return enumValue;
                    }
                }
                return bValue;
            } catch (NullPointerException | IllegalArgumentException ignored) {
                return null;
            }
        }));
    }

    public static boolean passItemIdConflictCheck(String id) {
        SlimefunItem sf = SlimefunItem.getById(id);
        if (sf == null) return true;
        Debug.error("ID : " + id + "RSC message" + sf.getAddon().getName() + " item ID ");
        return false;
    }

    public static boolean passItemGroupIdConflictCheck(String id) {
        ItemGroup ig = getIf(Slimefun.getRegistry().getAllItemGroups(),
            i -> i.getKey().getKey().equalsIgnoreCase(id));
        if (ig == null) return true;
        Debug.error("ID : " + id + " addon " + ig.getAddon().getName() + " item group ID ");
        return false;
    }

    public static @Nullable ItemGroup getItemGroup(ProjectAddon addon, @Nullable String id) {
        var v = readPipe(id, p -> {
            var v1 = getIf(addon.getItemGroups(), i -> i.getKey().getKey().equalsIgnoreCase(id));
            if (v1 == null) return getIf(Slimefun.getRegistry().getAllItemGroups(), i -> i.getKey().getKey().equalsIgnoreCase(id));
            return v1;
        });

        if (v == null) Debug.error("Unable toitem group (item_group): " + id);
        return v;
    }

    public static @Nullable RecipeType getRecipeType(String fieldName) {
        try {
            Field field = RecipeType.class.getDeclaredField(fieldName);
            return (RecipeType) field.get(null);
        } catch (NoSuchFieldException e) {
            return RecipeTypeMap.getRecipeType(fieldName);
        } catch (IllegalAccessException ignored) {
            // it doesn't happen
        }
        return null;
    }

    public static Component decorate(String message) {
        if (RykenSlimefunCustomizer.addonManager != null
            && RykenSlimefunCustomizer.addonManager.getLoadingAddon() != null) {
            message = "[" + RykenSlimefunCustomizer.addonManager.getLoadingAddon() + "] " + message;
        }
        return ComponentUtils.legacyDeserialize(CMIChatColor.translate(message));
    }

    public static boolean passMenuIdConflictCheck(String id, ProjectAddon addon) {
        if (getIf(addon.getMenus(), m -> m.getId().equalsIgnoreCase(id)) == null) return true;
        Debug.error("ID : menu ID " + id + "menu");
        return false;
    }
}
