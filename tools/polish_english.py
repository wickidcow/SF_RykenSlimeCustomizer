#!/usr/bin/env python3
from __future__ import annotations

import pathlib

ROOT = pathlib.Path(__file__).resolve().parents[1]


def replace(path: str, replacements: dict[str, str]) -> None:
    p = ROOT / path
    text = p.read_text(encoding="utf-8")
    new = text
    for old, value in replacements.items():
        new = new.replace(old, value)
    if new != text:
        p.write_text(new, encoding="utf-8")
        print(f"polished {path}")


replace(
    "src/main/java/org/lins/mmmjjkx/rykenslimefuncustomizer/bulit_in/recipes/Recipe.java",
    {
        'CMIChatColor.translate("RSC message" + wrapper.getNoConsume().getNoConsumeAmountExcludeLinked() + " items are not consumed")':
            'CMIChatColor.translate("&e" + wrapper.getNoConsume().getNoConsumeAmountExcludeLinked() + " item(s) are not consumed")',
        'CMIChatColor.translate("RSC message" + Arrays.toString(wrapper.getNoConsume().getLinkedNoConsume().toIntArray()) + " items are not consumed")':
            'CMIChatColor.translate("&eItems in slots " + Arrays.toString(wrapper.getNoConsume().getLinkedNoConsume().toIntArray()) + " are not consumed")',
        'CMIChatColor.translate("&aHas&b " + cs + "% &a output chance")':
            'CMIChatColor.translate("&b" + cs + "% &achance to output")',
        'CMIChatColor.translate("&aHas&b " + chance + "% &a output chance")':
            'CMIChatColor.translate("&b" + chance + "% &achance to output")',
    },
)

replace(
    "src/main/java/org/lins/mmmjjkx/rykenslimefuncustomizer/commands/MainCommand.java",
    {
        ''' &aRykenSlimeCustomizer
 &e/rsc (help) 
 &e/rsc reload addon
 &e/rsc reloadPlugin 
 &e/rsc list successfullyaddon
 &e/rsc enable <addonsName> addon
 &e/rsc disable <addonID> addon
 &e/rsc saveitem <addonID> <ID> item
 &e/rsc menupreview <ID> machinemenu
 &e/rsc getsaveditem <addonID> <ID> item
 &e/rsc resaveitems Hasitem
 &e/rsc clearScriptCache scriptfailed
 &e/rsc cleardisplayprojectiles multiblock''':
        ''' &aRykenSlimefunCustomizer
 &e/rsc &7- Show this help menu
 &e/rsc reload &7- Reload RSC addons
 &e/rsc reloadPlugin &7- Reload plugin configuration
 &e/rsc list &7- List loaded RSC addons
 &e/rsc enable <addonName> &7- Enable an addon
 &e/rsc disable <addonID> &7- Disable an addon
 &e/rsc saveitem <addonID> <ID> &7- Save the held item
 &e/rsc menupreview <ID> &7- Preview a machine menu
 &e/rsc getsaveditem <addonID> <ID> &7- Get a saved item
 &e/rsc resaveitems <start|end> &7- Resave stored items after a version change
 &e/rsc clearScriptCache &7- Clear cached scripts
 &e/rsc cleardisplayprojectiles &7- Clear multiblock display entities''',
    },
)

# The first translation pass used this token only when a Chinese-only literal
# had no safe automatic translation. Make any remaining fallback visibly useful
# rather than exposing a mechanical placeholder to players/admins.
remaining = []
for p in sorted((ROOT / "src/main/java").rglob("*.java")):
    text = p.read_text(encoding="utf-8")
    if "RSC message" in text:
        text = text.replace("RSC message", "RSC: ")
        p.write_text(text, encoding="utf-8")
        print(f"replaced fallback wording in {p.relative_to(ROOT)}")
    if "RSC message" in p.read_text(encoding="utf-8"):
        remaining.append(str(p.relative_to(ROOT)))

if remaining:
    raise SystemExit("Unpolished RSC message placeholders remain: " + ", ".join(remaining))

print("English polish pass complete.")
