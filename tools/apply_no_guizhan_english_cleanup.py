#!/usr/bin/env python3
from __future__ import annotations

import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parents[1]
CJK = re.compile(r"[\u3400-\u4dbf\u4e00-\u9fff\uf900-\ufaff]")
JAVA_STRING = re.compile(r'"(?:\\.|[^"\\])*"')

PHRASES = [
    ("你没有权限去做这些", "You do not have permission to do that"),
    ("只有玩家才能执行此命令", "Only players can run this command"),
    ("找不到此子指令", "Unknown subcommand"),
    ("重载配置成功", "Configuration reloaded successfully"),
    ("重载成功", "Reload successful"),
    ("重载失败", "Reload failed"),
    ("没有这个附属", "Addon not found"),
    ("没有这个菜单", "Menu not found"),
    ("没有这个文件夹", "Folder not found"),
    ("附属加载失败", "Addon failed to load"),
    ("加载附属成功", "Addon loaded successfully"),
    ("卸载此附属成功", "Addon unloaded successfully"),
    ("你不能在控制台使用此指令", "This command cannot be used from the console"),
    ("保存成功", "Saved successfully"),
    ("保存失败", "Save failed"),
    ("物品已放入你的手中", "The item was placed in your hand"),
    ("物品已放入你的背包中", "The item was added to your inventory"),
    ("你不能保存空气", "You cannot save air"),
    ("无法读取此物品文件", "Unable to read this item file"),
    ("指向的物品文件没有内容", "The selected item file is empty"),
    ("请输入正确的参数", "Enter a valid argument"),
    ("电力不足", "Not enough power"),
    ("空间不足", "Not enough space"),
    ("生产中", "Processing"),
    ("速度", "Speed"),
    ("左键", "Left click"),
    ("右键", "Right click"),
    ("返回上一页", "Previous page"),
    ("返回主菜单", "Main menu"),
    ("点击查看", "Click to view"),
    ("多物品输入", "Multiple item input"),
    ("多物品输出", "Multiple item output"),
    ("强配方物品输入", "Linked recipe input"),
    ("强配方物品输出", "Linked recipe output"),
    ("模板物品不消耗", "Template item is not consumed"),
    ("该物品不消耗", "This item is not consumed"),
    ("总计物品", "Items"),
    ("有", "Has"),
    ("的概率产出", " output chance"),
    ("物品不消耗", " items are not consumed"),
    ("源", "Source"),
    ("名称", "Name"),
    ("作者(们)", "Authors"),
    ("版本", "Version"),
    ("插件依赖", "Plugin dependencies"),
    ("依赖", "Dependencies"),
    ("描述", "Description"),
    ("仓库", "Repository"),
    ("已加载的附属", "Loaded addons"),
    ("版本号", "Version"),
    ("成功", "successfully"),
    ("失败", "failed"),
    ("无法", "Unable to"),
    ("未找到", "Not found"),
    ("无效", "Invalid"),
    ("缺少", "Missing"),
    ("配置错误", "Configuration error"),
    ("已跳过", "skipped"),
    ("正在加载", "Loading"),
    ("开始加载", "Loading"),
    ("开始读取", "Reading"),
    ("读取完成", "Read complete"),
    ("已加载", "Loaded"),
    ("附属", "addon"),
    ("物品组", "item group"),
    ("物品", "item"),
    ("机器", "machine"),
    ("菜单", "menu"),
    ("配方", "recipe"),
    ("研究", "research"),
    ("槽位", "slot"),
    ("脚本", "script"),
    ("多方块", "multiblock"),
    ("方块", "block"),
    ("层", " layer"),
    ("所有层", "all layers"),
    ("已搭建完成", "formed successfully"),
    ("未搭建完成", "not fully formed"),
    ("已被破坏", "was broken"),
    ("附近存在其他多方块阻碍", "Another multiblock is obstructing this area"),
    ("无法搭建该多方块，请拆除后重试", "Cannot build this multiblock; remove the obstruction and try again"),
    ("单击此处打开链接", "Click here to open the link"),
]


def update(path: str, transform) -> None:
    p = ROOT / path
    text = p.read_text(encoding="utf-8")
    new = transform(text)
    if new != text:
        p.write_text(new, encoding="utf-8")
        print(f"updated {path}")


def replace_many(text: str, replacements: dict[str, str]) -> str:
    for old, new in replacements.items():
        text = text.replace(old, new)
    return text


def englishize_literal(literal: str) -> str:
    body = literal[1:-1]
    if not CJK.search(body):
        return literal
    original = body
    for zh, en in PHRASES:
        body = body.replace(zh, en)
    body = body.replace("：", ": ").replace("，", ", ").replace("。", ". ").replace("！", "!")
    body = body.replace("（", " (").replace("）", ") ").replace("、", ", ")
    body = CJK.sub("", body)
    body = re.sub(r"[ ]{2,}", " ", body)
    # If a Chinese-only message had no mapped terms, make it visibly English
    # rather than leaving a blank GUI/log string.
    visible = re.sub(r"(?:&[0-9A-FK-ORa-fk-or]|§[0-9A-FK-ORa-fk-or]|[^A-Za-z0-9])", "", body)
    if not visible and CJK.search(original):
        body = "RSC message"
    return '"' + body + '"'


def englishize_java_runtime_strings(text: str) -> str:
    # Comments are allowed to remain Chinese; string literals are what can reach
    # players/console. This pass therefore only rewrites Java string literals.
    return JAVA_STRING.sub(lambda m: englishize_literal(m.group(0)), text)


def plugin_yml(text: str) -> str:
    return re.sub(r"(?m)^\s*-\s*GuizhanLibPlugin\s*(?:#.*)?\r?\n", "", text)


def build_gradle(text: str) -> str:
    return re.sub(r"(?m)^\s*compileOnly\(libs\.guizhan\.lib\.plugin\)\s*\r?\n", "", text)


def main_java(text: str) -> str:
    text = re.sub(r"(?m)^import net\.guizhanss\.guizhanlib\.updater\.GuizhanBuildsUpdater;\r?\n", "", text)
    text = re.sub(
        r"\n\s*if \(getConfig\(\)\.getBoolean\(\"pluginUpdate\", false\).*?GuizhanBuildsUpdater\.start\(.*?\);\s*\}\s*\n",
        "\n",
        text,
        count=1,
        flags=re.DOTALL,
    )
    return replace_many(text, {
        "检测到 net.kyori.adventure.text.warnWhenLegacyFormattingDetected = false": "Detected net.kyori.adventure.text.warnWhenLegacyFormattingDetected = false",
        "为了避免大量无效日志刷屏，我们强烈建议您添加以下 JVM 参数以禁止警告:               ": "To avoid excessive legacy-format warnings, add the following JVM argument:               ",
        "参见 https://docs.papermc.io/paper/reference/system-properties/#netkyoriadventuretextwarnwhenlegacyformattingdetected": "See https://docs.papermc.io/paper/reference/system-properties/#netkyoriadventuretextwarnwhenlegacyformattingdetected",
        "JustEnoughGuide 版本过低，无法适配": "JustEnoughGuide is too old for this integration",
        "RykenSlimefunCustomizer加载成功！": "RykenSlimefunCustomizer loaded successfully!",
        "原作者: lijinhong11": "Original author: lijinhong11",
        "改作者: balugaq": "Maintainer: balugaq",
        "项目主页: https://github.com/balugaq/RykenSlimeCustomizer": "Project: https://github.com/balugaq/RykenSlimeCustomizer",
        "已检测到 JustEnoughGuide，正在适配...": "JustEnoughGuide detected; enabling integration...",
        "&c保存的物品 (RSC saveditems)": "&cSaved Items (RSC saveditems)",
        "RykenSlimeCustomizer 已卸载!": "RykenSlimeCustomizer unloaded!",
    })


def common_utils(text: str) -> str:
    text = re.sub(r"(?m)^import net\.guizhanss\.guizhanlib\.minecraft\.utils\.compatibility\.(?:EnchantmentX|ItemFlagX);\r?\n", "", text)
    text = text.replace("ItemFlagX.HIDE_ADDITIONAL_TOOLTIP", "ItemFlag.HIDE_ADDITIONAL_TOOLTIP")
    text = text.replace(
        "itemStack.addUnsafeEnchantment(EnchantmentX.LUCK_OF_THE_SEA, 1);",
        "Enchantment glowEnchantment = Enchantment.getByKey(NamespacedKey.minecraft(\"luck_of_the_sea\"));\n"
        "            if (glowEnchantment != null) {\n"
        "                itemStack.addUnsafeEnchantment(glowEnchantment, 1);\n"
        "            }",
    )
    marker = "    public static CustomItemStack createDefaultItem() {"
    if "public static String getItemName(ItemStack stack)" not in text and marker in text:
        helper = '''    public static String getItemName(ItemStack stack) {\n        if (stack == null || stack.getType().isAir()) return \"Air\";\n        ItemMeta meta = stack.getItemMeta();\n        if (meta != null && meta.hasDisplayName()) return meta.getDisplayName();\n        String key = stack.getType().getKey().getKey().replace('_', ' ');\n        StringBuilder name = new StringBuilder(key.length());\n        boolean upper = true;\n        for (char c : key.toCharArray()) {\n            if (upper && Character.isLetter(c)) {\n                name.append(Character.toUpperCase(c));\n                upper = false;\n            } else {\n                name.append(c);\n                if (c == ' ') upper = true;\n            }\n        }\n        return name.toString();\n    }\n\n'''
        text = text.replace(marker, helper + marker)
    return text


def remove_itemstack_helper(text: str) -> str:
    text = re.sub(r"(?m)^import net\.guizhanss\.minecraft\.guizhanlib\.gugu\.minecraft\.helpers\.inventory\.ItemStackHelper;\r?\n", "", text)
    text = text.replace("ItemStackHelper.getName(", "CommonUtils.getItemName(")
    text = text.replace("ItemStackHelper.getDisplayName(", "CommonUtils.getItemName(")
    return text


update("src/main/resources/plugin.yml", plugin_yml)
update("build.gradle.kts", build_gradle)
update("src/main/java/org/lins/mmmjjkx/rykenslimefuncustomizer/RykenSlimefunCustomizer.java", main_java)
update("src/main/java/org/lins/mmmjjkx/rykenslimefuncustomizer/utils/CommonUtils.java", common_utils)
update("src/main/java/org/lins/mmmjjkx/rykenslimefuncustomizer/bulit_in/recipes/Recipe.java", remove_itemstack_helper)
update("src/main/java/org/lins/mmmjjkx/rykenslimefuncustomizer/bulit_in/SaveditemsGroup.java", remove_itemstack_helper)

# Englishize Java runtime string literals across the core while leaving comments
# and identifiers outside literals untouched.
for p in sorted((ROOT / "src/main/java").rglob("*.java")):
    text = p.read_text(encoding="utf-8")
    new = englishize_java_runtime_strings(text)
    if new != text:
        p.write_text(new, encoding="utf-8")
        print(f"englishized {p.relative_to(ROOT)}")

# Remove the now-unused dependency alias if it is present in the version catalog.
versions = ROOT / "gradle/libs.versions.toml"
if versions.exists():
    text = versions.read_text(encoding="utf-8")
    new = re.sub(r"(?mi)^.*guizhan[-_. ]lib.*\r?\n", "", text)
    if new != text:
        versions.write_text(new, encoding="utf-8")
        print("updated gradle/libs.versions.toml")

# Report any CJK still present in Java string literals. Comments are not player-facing.
report: list[str] = []
for p in sorted((ROOT / "src/main/java").rglob("*.java")):
    rel = p.relative_to(ROOT)
    for n, line in enumerate(p.read_text(encoding="utf-8", errors="replace").splitlines(), 1):
        if any(CJK.search(m.group(0)) for m in JAVA_STRING.finditer(line)):
            report.append(f"{rel}:{n}: {line.strip()}")
report_path = ROOT / "tools/player-facing-cjk-report.txt"
report_path.write_text("\n".join(report) + ("\n" if report else ""), encoding="utf-8")
print(f"Java string literals with CJK remaining: {len(report)}")
for row in report[:250]:
    print(row)
if report:
    raise SystemExit(4)

# A true dependency removal must leave no executable GuizhanLib API references.
api_refs: list[str] = []
for p in sorted(ROOT.rglob("*")):
    if not p.is_file() or ".git" in p.parts or p.suffix.lower() not in {".java", ".kt", ".kts"}:
        continue
    for n, line in enumerate(p.read_text(encoding="utf-8", errors="replace").splitlines(), 1):
        low = line.lower()
        if "net.guizhanss.guizhanlib" in low or "net.guizhanss.minecraft.guizhanlib" in low or "libs.guizhan.lib.plugin" in low:
            api_refs.append(f"{p.relative_to(ROOT)}:{n}: {line.strip()}")
if api_refs:
    print("ERROR: GuizhanLib API references remain:")
    print("\n".join(api_refs))
    raise SystemExit(2)

print("GuizhanLib API/dependency references removed and Java-facing CJK scan passed.")
