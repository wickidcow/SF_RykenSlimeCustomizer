#!/usr/bin/env python3
from __future__ import annotations

import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parents[1]
CJK = re.compile(r"[\u3400-\u4dbf\u4e00-\u9fff\uf900-\ufaff]")


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
        "已删除 STACKMACHINE_LIST 中的": "Removed from STACKMACHINE_LIST: ",
        "已删除 STACKMGENERATOR_LIST 中的": "Removed from STACKMGENERATOR_LIST: ",
        "已自动禁用机器在逻辑工艺中的可堆叠属性! 共 ": "Disabled LogiTech stackability for ",
        " 个机器": " machines",
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
    return replace_many(text, {
        "你设置了材料类型，但没有设置对应的材料! (material)": "A material type was set without a matching material value! (material)",
        "物品颜色 (color) 非法: ": "Invalid item color (color): ",
        " 已跳过": " - skipped",
        "物品不支持使用物品颜色 (color): ": "This item type does not support item color (color): ",
        "无法找到粘液物品: ": "Unable to find Slimefun item: ",
        "无法读取 UniItem 物品!": "Unable to resolve UniItem item!",
        "无法加载 UniItem 依赖! 无法识别物品.": "Unable to load the UniItem integration; item cannot be resolved.",
        "保存物品对应的文件不存在: ": "Saved-item file does not exist: ",
        "无法识别对应的保存物品: ": "Unable to resolve saved item: ",
        "无法识别原版物品: ": "Unable to resolve vanilla item: ",
        "无法识别内置物品: ": "Unable to resolve built-in item: ",
        "无法识别的类型: ": "Unknown item type: ",
        " 尝试以原版物品重新加载...": " - trying as a vanilla item...",
        " 尝试以粘液物品重新加载...": " - trying as a Slimefun item...",
        " 无法加载!": " - unable to load!",
        "物品数量 (amount) 超出范围: ": "Item amount is outside the supported range: ",
        "附魔格式非法 (enchantments): ": "Invalid enchantment format (enchantments): ",
        "未知的附魔 (enchantments): ": "Unknown enchantment (enchantments): ",
        "无法找到文件 ": "Unable to find file ",
        " 请检查插件文件是否损坏!": " - check whether the plugin files are damaged!",
        " 的同步，请检查插件文件是否损坏!": " while synchronizing it; check whether the plugin files are damaged!",
        "&e制作时间: &b": "&eCrafting time: &b",
        "，已转为 ": ", clamped to ",
        "ID 冲突: ": "ID conflict: ",
        " 与 ": " conflicts with ",
        " 中的物品发生了 ID 冲突": " item ID",
        " 与粘液附属 ": " conflicts with Slimefun addon ",
        " 中的物品组发生 ID 冲突": " item-group ID",
    })


update("src/main/resources/plugin.yml", plugin_yml)
update("build.gradle.kts", build_gradle)
update("src/main/java/org/lins/mmmjjkx/rykenslimefuncustomizer/RykenSlimefunCustomizer.java", main_java)
update("src/main/java/org/lins/mmmjjkx/rykenslimefuncustomizer/utils/CommonUtils.java", common_utils)

# Remove the now-unused dependency alias if it is present in the version catalog.
versions = ROOT / "gradle/libs.versions.toml"
if versions.exists():
    text = versions.read_text(encoding="utf-8")
    new = re.sub(r"(?mi)^.*guizhan[-_. ]lib.*\r?\n", "", text)
    if new != text:
        versions.write_text(new, encoding="utf-8")
        print("updated gradle/libs.versions.toml")

# Produce an auditable report. Chinese comments/docs are allowed; Chinese in quoted
# runtime strings is listed so it can be translated without changing identifiers.
report: list[str] = []
for p in sorted((ROOT / "src").rglob("*")):
    if not p.is_file() or p.suffix.lower() not in {".java", ".yml", ".yaml"}:
        continue
    rel = p.relative_to(ROOT)
    for n, line in enumerate(p.read_text(encoding="utf-8", errors="replace").splitlines(), 1):
        if CJK.search(line) and ("\"" in line or "'" in line):
            report.append(f"{rel}:{n}: {line.strip()}")

report_path = ROOT / "tools/player-facing-cjk-report.txt"
report_path.write_text("\n".join(report) + ("\n" if report else ""), encoding="utf-8")
print(f"player-facing CJK candidates: {len(report)}")
for row in report[:250]:
    print(row)

# A true dependency removal must leave no executable GuizhanLib API references.
api_refs: list[str] = []
for p in sorted(ROOT.rglob("*")):
    if not p.is_file() or ".git" in p.parts or p.suffix.lower() not in {".java", ".kt", ".kts"}:
        continue
    for n, line in enumerate(p.read_text(encoding="utf-8", errors="replace").splitlines(), 1):
        if "net.guizhanss.guizhanlib" in line or "libs.guizhan.lib.plugin" in line:
            api_refs.append(f"{p.relative_to(ROOT)}:{n}: {line.strip()}")
if api_refs:
    print("ERROR: GuizhanLib API references remain:")
    print("\n".join(api_refs))
    raise SystemExit(2)

print("GuizhanLib API/dependency references removed.")
