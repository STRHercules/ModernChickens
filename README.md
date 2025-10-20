# Modern Chickens

Modern Chickens is a NeoForge 1.21.1 port of the classic Chickens and Roost mods. It reintroduces the breeding-driven resource automation gameplay loop while embracing modern Forge-era tooling, data packs, and integrations.

## Feature highlights

- **Comprehensive chicken roster** - Ports the entire legacy chicken catalogue with stats, drops, and breeding trees exposed through data-driven registries and a persistent `chickens.properties` configuration file. Chickens can be customised, disabled, or reparented without recompiling the mod.
- **Dynamic material coverage** - Generates placeholder chickens for any ingot item detected at runtime, using a shared fallback texture and Smart Chicken lineage to keep mod packs covered without manual config tweaks.
- **Automation blocks** - Roosts, breeders, collectors, and henhouses ship with their original block entities, menus, and renderers so farms can incubate, store, and harvest chickens hands-free.
- **Dedicated items** - Spawn eggs, coloured eggs, liquid eggs, chicken catchers, and analyzer tools keep the legacy progression loop intact while adopting modern capability and tooltip systems.
- **JEI and Jade integrations** - Recipe categories, item subtypes, and overlay tooltips surface roost, breeder, and chicken stats directly in-game when the companion mods are installed.
- **Server-friendly utilities** - `/chickens export breeding` regenerates the breeding graph on demand, and the mod respects headless server runs out of the box.
- **Modded Chicken Support** Modern Chickens will identify all 'ingot' resources in your minecraft instance and generate resource chickens for them. You are able to tune the `chicken.properties` to disable duplicate chickens, change their breed 'recipe' and laid resource. 

> Modded Chickens will choose random assets for the item version, and use a texture that is generated on the fly for them in the overworld


> Configuration and breeding data live in `config/chickens.properties`. The file is generated on first run and can be safely edited while the game is stopped. Restart the client or server—or run `/chickens export breeding`—to reload breeding graphs after making changes. The `chickens.cfg` is a compatability holdover which is unused. If you wish to make changes to `chickens.properties`, delete the `chickens.cfg` before launching the client.

## Project layout

```
ModernChickens/
├─ src/main/java               # Gameplay code and integrations
├─ src/main/resources          # Pack metadata; runtime assets merged from OriginalChickens
├─ OriginalChickens/           # Legacy assets copied during resource processing (read-only)
├─ roost/                      # Legacy Roost textures used when available (read-only)
├─ ModDevGradle-main/gradle/   # Wrapper and plugin bootstrap
└─ build.gradle                # NeoForge configuration and custom asset generation tasks
```

Only files under `ModernChickens/src` and root documentation (like this README) should be edited; the legacy projects remain read-only snapshots.

## Build prerequisites

- Java Development Kit 21 (the build uses Gradle toolchains to target Java 21 automatically)
- Git and a 64-bit operating system capable of running Minecraft 1.21.1
- (Optional) A local installation of [JEI](https://www.curseforge.com/minecraft/mc-mods/jei) or [Jade](https://www.curseforge.com/minecraft/mc-mods/jade) for integration testing

The project uses the bundled Gradle wrapper; no global Gradle installation is required.

## Building the mod

```bash
# Clone and enter the project
git clone https://example.com/ModernChickens.git
cd ModernChickens

# Run a clean build
./gradlew --console=plain build
```

The build copies legacy resources from `OriginalChickens`, optionally mirrors Roost textures from `roost`, and produces a distributable JAR in `build/libs/`.

> **Tip:** On first launch the build may download NeoForge dependencies; subsequent runs complete much faster. Use `./gradlew --info build` if you need detailed logging while debugging build issues.


## Gameplay overview

1. **Collect basic chickens.** Use spawn eggs or natural spawns to gather Tier 1 breeds, then throw coloured eggs to obtain dyed variants.
2. **Analyse and breed.** Right-click chickens with the analyzer to view stats, use roosts for passive production, and combine chickens in the breeder to unlock higher tiers.
3. **Automate collection.** Set up collectors next to roosts and henhouses to sweep drops into inventories or item pipes.
4. **Scale production.** Tune `chickens.properties` to adjust lay rates, breeder speed multipliers, vanilla egg suppression, and natural spawn toggles to match your pack’s balance goals.


## Support and contributions

- File gameplay bugs or crash reports through the project issue tracker (link in `neoforge.mods.toml`).
- Keep pull requests focused and ensure `./gradlew build` succeeds before submitting.
- Use `TRACELOG.md` to document development steps, and update `SUGGESTIONS.md` with follow-up ideas or refactors discovered during implementation.

*Modern Chickens inherits the MIT license from the original project. See `LICENSE` (when present) or the mod metadata for details.*

Happy hatching!
