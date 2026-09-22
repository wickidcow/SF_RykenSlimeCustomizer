# SF RykenSlimeCustomizer

A Slimefun Legacy compatibility fork of **RykenSlimeCustomizer**, the config-driven engine used by projects such as `Magic_RSC`.

## Legacy target

- Slimefun Legacy **4.1.58**
- Paper **26.2** primary target
- Java **25** build toolchain
- Java **21** bytecode target
- Minecraft **1.21.11**

The Bukkit plugin identity remains `RykenSlimefunCustomizer` so existing RSC projects and integrations continue to detect it normally. Only the distributed JAR uses the `SF_` Legacy naming convention.

## Release JAR

`SF_RykenSlimeCustomizer3.1.11.jar`

## Upstream

Original project and ongoing upstream development:
- SlimefunReloadingProject/RykenSlimeCustomizer
- balugaq/RykenSlimeCustomizer

This fork preserves the upstream GPL-3.0-or-later licensing and credits while maintaining compatibility with Slimefun Legacy.
## Externally managed addon runtimes

RykenSlimeCustomizer normally checks configured RSC addons for GitHub updates after they finish loading. Magic Legacy 2.x owns its embedded `addons/Magic` runtime itself and writes a `.magiclegacy-managed` marker before RSC starts.

Starting with **3.1.9**, RSC detects that marker and skips its own post-load updater for the managed Magic folder. This prevents RSC from replacing Magic after it has already parsed an older runtime and preserves the intended startup order: `Networks -> MagicLegacy -> RykenSlimefunCustomizer`.

Do not remove `.magiclegacy-managed` from a plugin-managed Magic folder. The MagicLegacy plugin replaces that managed runtime during normal startup and preserves an unmanaged pre-existing Magic folder as a backup on first adoption.


## Registration reporting in 3.1.11

- Condition-gated addon definitions are tracked separately from actual registration failures.
- Final startup reporting now shows registered, conditionally skipped, failed, and total definition counts.
- Empty late-init phases no longer print duplicate-looking reader load messages.
- Registration order and condition behavior are unchanged; this is a diagnostics and startup-log clarity update.
