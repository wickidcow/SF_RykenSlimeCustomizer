#!/usr/bin/env python3
"""Apply strict missing-item handling to RSC.

Missing Slimefun/optional-addon items must invalidate the recipe or item that
uses them. They must never be silently replaced with stone or allow a partial
machine recipe to remain active.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def replace_exact(path: str, old: str, new: str) -> None:
    file = ROOT / path
    text = file.read_text(encoding="utf-8")
    if old not in text:
        raise SystemExit(f"Expected source block not found in {path}")
    text = text.replace(old, new, 1)
    file.write_text(text, encoding="utf-8", newline="\n")
    print(f"Patched {path}")


common = "src/main/java/org/lins/mmmjjkx/rykenslimefuncustomizer/utils/CommonUtils.java"
yaml_reader = "src/main/java/org/lins/mmmjjkx/rykenslimefuncustomizer/readers/YamlReader.java"
ticker = "src/main/java/org/lins/mmmjjkx/rykenslimefuncustomizer/bulit_in/tickers/RecipeMachineTickerCreator.java"

replace_exact(
    common,
    '''        @Nullable ItemStack[] itemStacks = new ItemStack[size];
        for (int i = 0; i < size; i++) {
            ConfigurationSection item = section.getConfigurationSection(String.valueOf(i + 1));
            itemStacks[i] = readItem(file, item, addon);
        }
        return itemStacks;''',
    '''        @Nullable ItemStack[] itemStacks = new ItemStack[size];
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
        return itemStacks;'''
)

replace_exact(
    common,
    '''            var stack = readItem(file, item, addon);
            if (stack == null) continue;
            descs.add(new InputDesc(stack, item.getInt("slot", -1), allNoConsume || item.getBoolean("noConsume", false)));''',
    '''            var stack = readItem(file, item, addon);
            if (stack == null) {
                Debug.warn(file, item, "Skipping machine recipe because an input item could not be resolved.");
                return Collections.emptyList();
            }
            descs.add(new InputDesc(stack, item.getInt("slot", -1), allNoConsume || item.getBoolean("noConsume", false)));'''
)

replace_exact(
    common,
    '''    public static ItemStack readItem(
            File file,''',
    '''    public static @Nullable ItemStack readItem(
            File file,'''
)

replace_exact(
    common,
    '''        try {
            itemStack = CommonUtils.readPipe(material, s -> getBaseItemStack(file, section, finalType, s, addon));
            if (itemStack == null) {
                Debug.warn("无法识别 " + material + " ，已转为石头.");
                itemStack = createDefaultItem();
            }
        } catch (Exception e) {
            itemStack = createDefaultItem();
            Debug.warn(file, section, "加载物品失败! 已转为石头!", e);
        }

        ItemMeta meta = itemStack.getItemMeta();''',
    '''        try {
            itemStack = CommonUtils.readPipe(material, s -> getBaseItemStack(file, section, finalType, s, addon));
            if (itemStack == null) {
                Debug.warn(file, section, "Unable to resolve item: " + material + ". The containing item or recipe will be skipped.");
                return null;
            }
        } catch (Exception e) {
            Debug.warn(file, section, "Failed to load item: " + material + ". The containing item or recipe will be skipped.", e);
            return null;
        }

        ItemMeta meta = itemStack.getItemMeta();'''
)

replace_exact(
    yaml_reader,
    '''        ItemStack[] recipe = recipePair.getSecondValue();

        ItemStack output = null;''',
    '''        ItemStack[] recipe = recipePair.getSecondValue();
        if (recipe == null) {
            Debug.warn(file, section, "Skipping " + id + " because its recipe contains an unresolved item.");
            return null;
        }

        ItemStack output = null;'''
)

replace_exact(
    ticker,
    '''            List<InputWrapper> input = CommonUtils.readInputs(file, recipe.getConfigurationSection("input"), addon, recipe.getBoolean("noConsume", false));
            if (!canInputEmpty && input.isEmpty()) {
                Debug.error(file, recipe, "缺少 '输入物品' (input)");
                continue;
            }
            ConfigurationSection outputs = recipe.getConfigurationSection("output");''',
    '''            ConfigurationSection inputs = recipe.getConfigurationSection("input");
            List<InputWrapper> input = CommonUtils.readInputs(file, inputs, addon, recipe.getBoolean("noConsume", false));
            boolean configuredInputs = inputs != null && !inputs.getKeys(false).isEmpty();
            if ((configuredInputs && input.isEmpty()) || (!canInputEmpty && input.isEmpty())) {
                Debug.error(file, recipe, configuredInputs
                    ? "Skipping recipe because one or more configured inputs could not be resolved."
                    : "Missing recipe input (input).");
                continue;
            }
            ConfigurationSection outputs = recipe.getConfigurationSection("output");'''
)

replace_exact(
    ticker,
    '''            List<ItemStack> output = new ArrayList<>();
            IntList chances = new IntArrayList();
            for (String k : outputs.getKeys(false)) {
                ConfigurationSection outputCfg = outputs.getConfigurationSection(k);
                if (outputCfg == null) break;
                var item = CommonUtils.readItem(file, outputCfg, addon);
                if (item == null) {
                    Debug.error(file, outputCfg, "物品配置错误 (output)");
                    continue;
                }

                int chance = CommonUtils.clamp(outputCfg.getInt("chance", 100), 1, 100,
                    file, outputCfg, "'概率 (chance) 非法'");

                output.add(item);
                chances.add(chance);
            }

            RecipeReader.addToList(list, recipe, seconds, input, chances, output.toArray(new ItemStack[0]));''',
    '''            List<ItemStack> output = new ArrayList<>();
            IntList chances = new IntArrayList();
            boolean invalidOutput = false;
            for (String k : outputs.getKeys(false)) {
                ConfigurationSection outputCfg = outputs.getConfigurationSection(k);
                if (outputCfg == null) {
                    invalidOutput = true;
                    break;
                }
                var item = CommonUtils.readItem(file, outputCfg, addon);
                if (item == null) {
                    Debug.error(file, outputCfg, "Skipping recipe because an output item could not be resolved.");
                    invalidOutput = true;
                    break;
                }

                int chance = CommonUtils.clamp(outputCfg.getInt("chance", 100), 1, 100,
                    file, outputCfg, "'概率 (chance) 非法'");

                output.add(item);
                chances.add(chance);
            }

            if (invalidOutput || output.isEmpty()) {
                continue;
            }

            RecipeReader.addToList(list, recipe, seconds, input, chances, output.toArray(new ItemStack[0]));'''
)

print("Strict missing-item handling applied successfully")
