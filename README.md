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

### Custom chicken definitions

- Drop a `chickens_custom.json` file in the `config` directory to add bespoke chickens without recompiling the mod. The loader creates a starter file with documentation the first time it runs.
- Each entry in the `chickens` array controls the chicken name, texture, lay/drop items, breeding parents, lay coefficient, and optional display name. Any missing field falls back to the mod defaults so you can tweak as much or as little as you like.
- Custom chickens participate in the existing `chickens.properties` flow, meaning you can still fine-tune them (enable/disable, change drops, reparent) alongside the built-in roster.

Example `chickens_custom.json` entry (place inside the top-level `chickens` array):

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

> **Note:** Minecraft resource locations are lowercase by convention. The loader automatically normalises uppercase letters or
> Windows-style backslash (`\`) separators, but shipping textures in lowercase avoids surprises on dedicated servers.
>
> When `generated_texture` is enabled, the renderer tints the referenced texture so in-world chickens match the colours used for the item and spawn egg. If the texture cannot be loaded, the base white chicken sprite is used instead.
>
> If `item_texture` is supplied the chicken item and JEI icons prefer that sprite. When the referenced texture cannot be loaded a warning is logged and Minecraft’s missing-texture placeholder is shown until the asset becomes available; otherwise tinting stays disabled so the art is rendered exactly as authored.
>
> When `generated_texture` is disabled the renderer expects the texture to exist in a resource pack or datapack. Missing assets trigger a warning and fall back to the tinted variant so players never see the purple-and-black placeholder in game.

### `chickens_custom.json` field reference

| Field | Required | Type | Accepted values and behaviour |
|-------|----------|------|-------------------------------|
| `name` | Yes | String | Unique registry name for the chicken (matches the entity id fragment used elsewhere in the mod). |
| `id` | No | Integer | Positive numeric id. Omit to let the loader pick the next free id automatically. |
| `texture` | Yes | Resource location | Namespaced path (`namespace:path/to.png`) to the chicken texture. Paths are normalised to lowercase automatically; provide the lowercase form used inside resource packs/datapacks. When the texture is missing, a warning is logged and the renderer falls back to the tinted white chicken template. |
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
| `item_texture` | No | Resource location | Optional namespaced path pointing at the item sprite (`namespace:textures/item/...png`). When omitted, the loader assumes a sprite lives at `chickens:textures/item/chicken/<lowercase name>.png`. Custom sprites supplied through the JSON file remain authoritative; missing resources log a warning and display Minecraft’s purple-and-black placeholder instead of swapping back to the tinted fallback. |

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
