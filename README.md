# Modern Chickens

Modern Chickens is a NeoForge 1.21.1 port of the classic Chickens and Roost mods. It reintroduces the breeding-driven resource automation gameplay loop while embracing modern Forge-era tooling, data packs, and integrations.

### Mod Pages: 

- [CurseForge](https://www.curseforge.com/minecraft/mc-mods/modern-chickens)

- [Modrinth](https://modrinth.com/mod/modern-chickens)

## Gameplay Overview

1. **Collect basic chickens.** Use spawn eggs or natural spawns to gather Tier 1 breeds, then throw coloured eggs to obtain dyed variants.
2. **Analyse and breed.** Right-click chickens with the analyzer to view stats, use roosts for passive production, and combine chickens in the breeder to unlock higher tiers.
3. **Automate collection.** Set up collectors next to roosts and henhouses to sweep drops into inventories or item pipes.
4. **Scale production.** Tune `chickens.cfg` to adjust lay rates, breeder speed multipliers, vanilla egg suppression, and natural spawn toggles to match your pack’s balance goals.

## Feature Highlights

- **Comprehensive chicken roster** - Ports the entire legacy chicken catalogue with stats, drops, and breeding trees exposed through data-driven registries and a persistent `chickens.cfg` configuration file. Chickens can be customised, disabled, or reparented without recompiling the mod.
- **Dynamic material coverage** - Generates placeholder chickens for any ingot item detected at runtime, using a shared fallback texture and Smart Chicken lineage to keep mod packs covered without manual config tweaks.
- **Automation blocks** - Roosts, breeders, collectors, the Avian Flux Converter, and the new Avian Fluid Converter ship with their original block entities, menus, and renderers so farms can incubate, store, and harvest chickens hands-free.
- **Dedicated items** - Spawn eggs, coloured eggs, liquid eggs, chicken catchers, and analyzer tools keep the legacy progression loop intact while adopting modern capability and tooltip systems.
- **Fluid automation** - Liquid eggs can now be cracked into configurable fluid stacks with the Avian Fluid Converter, complete with tank overlays, JEI recipes, and WTHIT/Jade readouts—plus Modern Chickens now generates fluid chickens automatically for every registered liquid in your pack.
- **JEI and Jade integrations** - Recipe categories, item subtypes, and overlay tooltips surface roost, breeder, fluid converter, and chicken stats directly in-game when the companion mods are installed.
- **Server-friendly utilities** - `/chickens export breeding` regenerates the breeding graph on demand, and the mod respects headless server runs out of the box.
- **Modded Chicken Support** Modern Chickens will identify all 'ingot' resources in your minecraft instance and generate resource chickens for them. You are able to tune the `chicken.properties` to disable duplicate chickens, change their breed 'recipe' and laid resource. 

_Chickens available in ATM10!_

![Chickens Available in ATM10](https://i.imgur.com/Eq2NYOr.gif)


> Modded Chickens will choose random assets for the item version, and use a texture that is generated on the fly for them in the overworld


> Configuration and breeding data live in `config/chickens.cfg`. The file is generated on first run and can be safely edited while the game is stopped. Restart the client or server—or run `/chickens export breeding`—to reload breeding graphs after making changes. Existing `chickens.properties` files are loaded once (for migration) but are no longer written—feel free to delete them after confirming the upgrade.
> Need a quieter base? Flip `general.avianFluxEffectsEnabled=false` to disable the Avian Flux Converter's light and particle effects without touching code.
> Want different RF pacing? Adjust `general.fluxEggCapacityMultiplier` for Flux Egg storage or tweak `general.avianFluxCapacity`, `general.avianFluxMaxReceive`, and `general.avianFluxMaxExtract` to rebalance the converter.
> Prefer gentler handling? Set `general.liquidEggHazardsEnabled=false` to suppress the liquid egg status effects, and tune `general.avianFluidConverterCapacity` / `general.avianFluidConverterTransferRate` to balance fluid throughput.

### Avian Fluid Converter

Liquid eggs no longer require hand-placing to deploy their contents. Drop a liquid egg in the Avian Fluid Converter and it automatically cracks the shell, stores the fluid internally, and feeds adjacent tanks or pipes each tick.

- **Discovery**: JEI adds an “Avian Fluid Converter” category that lists every liquid egg and the fluid volume it produces. The converter block is registered as a catalyst, so recipes are only a click away.
- **Catch ’em all**: Modern Chickens now creates a dedicated chicken for every fluid detected at runtime—if a mod ships a new liquid, you get a matching bird and egg automatically.
- **Monitoring**: WTHIT and Jade overlays mirror the in-block gauge with the current fluid name, stored amount, and tank capacity. You can check the converter’s status without opening the GUI.
- **Balancing**: Server owners can adjust `general.avianFluidConverterCapacity`, `general.avianFluidConverterTransferRate`, and `general.liquidEggHazardsEnabled` in `config/chickens.cfg` to match the rest of their tech progression.

Pair the converter with standard fluid transport (pipes, tanks, or machines) to integrate chickens into modded processing lines—experience, biofuel, radioactive waste, and other tech fluids now flow straight from the coop.

### Custom Chicken Definitions

- After first run, the mod will generate a `chickens_custom.json` file in the `config` directory where you can add bespoke chickens without recompiling the mod. The starter file will also have an example baked in.
- Each entry in the `chickens` array controls the chicken name, texture, lay/drop items, breeding parents, lay coefficient, and optional display name. Any missing field falls back to the mod defaults so you can tweak as much or as little as you like.
- Custom chickens participate in the existing `chickens.cfg` flow, meaning you can still fine-tune them (enable/disable, change drops, reparent) alongside the built-in roster.

Example `chickens_custom.json` entries (place inside the top-level `chickens` array):


```json
{
  "name": "SteelChicken",
  "id": 425,
  "texture": "chickens:textures/entity/steelchicken.png",
  "item_texture": "chickens:textures/item/chicken/steelchicken.png",
  "lay_item": {
    "item": "examplemod:steel_ingot"
  },
  "drop_item": {
    "item": "examplemod:steel_ingot",
    "count": 2
  },
  "background_color": "#4a4a4a",
  "foreground_color": "#b7b7b7",
  "parents": ["IronChicken", "CoalChicken"],
  "spawn_type": "hell",
  "lay_coefficient": 1.25,
  "display_name": "Steel Chicken",
  "generated_texture": false,
  "enabled": true
}
```

```json
{
  "name": "WhippedCreamChicken",
  "id": 425,
  "item_texture": "chickens:textures/item/chicken/steelchicken.png",
  "lay_item": {
    "item": "minecraft:cake"
  },
  "drop_item": {
    "item": "minecraft:cake",
    "count": 1
  },
  "background_color": "#ef30f2",
  "foreground_color": "#472447",
  "parents": ["SlimeChicken", "SnowballChicken"],
  "spawn_type": "none",
  "lay_coefficient": 0.25,
  "display_name": "Whipped Cream Chicken",
  "generated_texture": true,
  "enabled": true
}
```

> **Note:** Minecraft resource locations are lowercase by convention. The loader automatically normalises uppercase letters or
> Windows-style backslash (`\`) separators, but shipping textures in lowercase avoids surprises on dedicated servers.
>
> Omitting the `texture` field will force the chicken to default to bone chicken sprite.
>
> When `generated_texture` is enabled, the renderer tints the referenced texture so in-world chickens match the colours used for the item and spawn egg. If the texture cannot be loaded, the base bone chicken sprite is used instead, and tinted.
>
> If `item_texture` is supplied the chicken item and JEI icons prefer that sprite. When the referenced texture cannot be loaded a warning is logged and Minecraft’s missing-texture placeholder is shown until the asset becomes available; otherwise tinting stays disabled so the art is rendered exactly as authored. The runtime now bakes a vanilla `minecraft:item/generated` model around the sprite so even textures that only exist in your datapack (and therefore lack a prebuilt model) display correctly.
>
> When `generated_texture` is disabled the renderer expects the texture to exist in a resource pack or datapack. Missing assets trigger a warning and fall back to the tinted bone chicken variant so players never see the purple-and-black placeholder in game.

### `chickens_custom.json` Field Reference:

| Field | Required | Type | Accepted values and behaviour |
|-------|----------|------|-------------------------------|
| `name` | Yes | String | Unique registry name for the chicken (matches the entity id fragment used elsewhere in the mod). |
| `id` | No | Integer | Positive numeric id. Omit to let the loader pick the next free id automatically. |
| `texture` | No | Resource location | Namespaced path (`namespace:path/to.png`) to the chicken texture. Paths are normalised to lowercase automatically; provide the lowercase form used inside resource packs/datapacks. When the texture is missing, a warning is logged and the renderer falls back to the tinted bone chicken template. |
| `lay_item.item` | Yes | Resource location | Namespaced item id that the chicken lays. Must exist in the item registry. |
| `lay_item.count` | No | Integer | Stack size to lay each cycle. Defaults to `1`; values below `1` are clamped up. |
| `lay_item.type` | No | Integer | Only used with the liquid egg item to select the chicken variant encoded in the stack. |
| `drop_item` | No | Object | Same shape as `lay_item`. When omitted the chicken drops its lay item when killed. |
| `background_color` | No | String/Integer | Hex string (`#RRGGBB` or `RRGGBB`) or decimal value between `0` and `16777215`. Defaults to white (`0xFFFFFF`). |
| `foreground_color` | No | String/Integer | Hex string (`#RRGGBB` or `RRGGBB`) or decimal value between `0` and `16777215`. Defaults to yellow (`0xFFFF00`). |
| `parents` | No | Array[String] | Up to two chicken names that must already exist. Leave empty or omit for Tier 1 chickens. Only the first two entries are used. |
| `spawn_type` | No | String | Case-insensitive values drawn from `normal`, `snow`, `hell`, or `none`. Defaults to `normal`. |
| `lay_coefficient` | No | Float | Multiplier applied to lay times. Values below `0` are clamped to `0`. Defaults to `1.0`. |
| `display_name` | No | String | Overrides the in-game display name. Defaults to the translated name derived from `name`. |
| `generated_texture` | No | Boolean | Set to `true` to tint the configured texture (or the base white chicken if the texture is missing) using the `background_color`/`foreground_color` pair. When `false`, the renderer uses the texture as-is and only falls back to tinting if that texture cannot be loaded. Defaults to `false`. |
| `enabled` | No | Boolean | Toggles whether the chicken participates in registries and breeding. Defaults to `true` and cascades with parent availability. |
| `item_texture` | No | Resource location | Optional namespaced path pointing at the item sprite (`namespace:textures/item/...png`). When omitted, the loader assumes a sprite lives at `chickens:textures/item/chicken/<lowercase name>.png`. Custom sprites supplied through the JSON file remain authoritative; missing resources log a warning and display Minecraft’s purple-and-black placeholder instead of swapping back to the tinted fallback. When the referenced art already ships with a baked model (for example, reusing an existing Modern Chickens texture), the runtime reuses that model directly; otherwise it now generates a vanilla `minecraft:item/generated` quad on the fly so datapack-only textures render as expected. |

## Redstone Flux

![RF Chicken Generator!](https://i.imgur.com/lkeGThc.png)


Modern Chickens reintroduces power generation through a dedicated Redstone Flux progression line:

- **Redstone Flux Chicken** – A tier 3 breed unlocked by pairing Redstone and Glowstone chickens. Every drop is a charged Flux Egg so energy farms can start as soon as the bird hatches.
- **Flux Egg** – Stores Redstone Flux directly on the item stack. Freshly laid eggs hold 1,000 RF and gain another 100 RF for every growth, gain, or strength point above 1 (maxing out at 3,700 RF per egg from a 10/10/10 chicken).
- **Avian Flux Converter** – A single-slot machine that drains Flux Eggs into a 50,000 RF internal buffer while exporting energy to adjacent consumers. Empty shells are discarded automatically once their payload is exhausted.

### Flux Egg charge scaling

Flux Eggs inherit the stats of the chicken that laid them, so every breed and breeding investment matters:

| Chicken stats | Stored RF per egg | Notes |
| --- | --- | --- |
| 1/1/1 (base) | 1,000 RF | Entry-level output straight from newly bred birds. |
| 10/10/10 (max) | 3,700 RF | Gains also triple the stack size, dropping **three** eggs per cycle. |

The Avian Flux Converter converts each egg’s stored energy on a one-to-one basis, so farms can bank or route the full payload without transmission loss.

### Roost throughput examples

Roost production scales with both chicken stats and stack size. Each cycle takes roughly 27,000 server ticks (the midpoint between the Redstone Flux Chicken’s 18,000–36,000 tick lay window) divided by the number of working chickens times their growth stat. The figures below assume default config values, infinite storage, and continuous feeding so every roost stays active.

| Installation | RF/t per roost | 10 roosts | 20 roosts | 30 roosts |
| --- | ---: | ---: | ---: | ---: |
| 1× base Redstone Flux Chicken (stats 1/1/1) | ≈0.04 | ≈0.37 | ≈0.74 | ≈1.11 |
| 1× max-stat Redstone Flux Chicken (stats 10/10/10) | ≈4.11 | ≈41.11 | ≈82.22 | ≈123.33 |
| 16× max-stat Redstone Flux Chickens (full roost of 10/10/10) | ≈65.78 | ≈657.78 | ≈1,315.56 | ≈1,973.33 |

> **Assumptions:** RF/t values use the average lay time and treat each Flux Egg as delivering its entire stored energy (1,000 RF for base birds, 3,700 RF ×3 eggs for maxed birds). Actual outputs fluctuate slightly with the random lay timer and any custom `roostSpeedMultiplier` tweaks in `chickens.cfg`.

### Same graph in RF/s:

| Installation | RF/s per roost | 10 roosts | 20 roosts | 30 roosts |
|---|---:|---:|---:|---:|
| 1× base Redstone Flux Chicken (1/1/1) | ≈0.8 | ≈7.4 | ≈14.8 | ≈22.2 |
| 1× max-stat Redstone Flux Chicken (10/10/10) | ≈82.2 | ≈822.2 | ≈1,644.4 | ≈2,466.6 |
| 16× max-stat Redstone Flux Chickens (full roost of 10/10/10) | ≈1,315.6 | ≈13,155.6 | ≈26,311.2 | ≈39,466.6 |


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

## Build Prerequisites

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


## Support and Contributions

- File gameplay bugs or crash reports through the project issue tracker (link in `neoforge.mods.toml`).
- Keep pull requests focused and ensure `./gradlew build` succeeds before submitting.
- Use `TRACELOG.md` to document development steps, and update `SUGGESTIONS.md` with follow-up ideas or refactors discovered during implementation.

*Modern Chickens inherits the MIT license from the original project. See `LICENSE` (when present) or the mod metadata for details.*

Happy hatching!
