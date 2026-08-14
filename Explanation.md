# Modern Chickens TOML configuration

Modern Chickens uses three player-owned TOML files:

- `config/chickens.toml` contains global gameplay, spawning, machine, and integration options under `[general]`.
- `config/custom_chickens.toml` contains all packaged stock chicken tables and every player-defined chicken under `[chickens.<name>]`.
- `config/fluid_chicken_aliases.toml` contains optional pack-owned equivalence rules for automatic fluid-chicken discovery.

The installed files are the source of truth for that client or server. The repository copies are starter examples:

- [`Examples/Config/chickens.toml`](Examples/Config/chickens.toml)
- [`Examples/Config/fluid_chicken_aliases.toml`](Examples/Config/fluid_chicken_aliases.toml)
- [`Examples/Custom Chickens/custom_chickens.toml`](Examples/Custom%20Chickens/custom_chickens.toml)

## Basic workflow

1. Start the client or dedicated server once. The configuration files are created in its `config` directory.
2. Stop Minecraft completely before editing either file.
3. Keep TOML types correct: booleans are `true`/`false`, numbers are unquoted, and text is quoted.
4. Save the file and restart the client or server. Chicken definitions and gameplay configuration are read during startup.
5. For a dedicated server, edit the server's files. The server's `custom_chickens.toml` controls the roster seen by connected players.

The in-game configuration screen edits `chickens.toml` for global options and `custom_chickens.toml` for chicken tables. It validates numbers and booleans, but a restart is still required.

## File layout

`chickens.toml` contains only global values:

~~~toml
[general]
roostSpeed = 1.0
enableFluidChickens = true
autoRegisterFluidChickens = false
liquidEggHazardsEnabled = true
~~~

`custom_chickens.toml` contains stock data and new definitions:

~~~toml
[chickens.WhiteChicken]
enabled = true
layCoefficient = 1.0
spawnType = "NORMAL"
allowNaturalSpawn = true
parent1 = ""
parent2 = ""
layItemName = "minecraft:bone"
layItemAmount = 1
layItemMeta = 0
dropItemName = "minecraft:bone"
dropItemAmount = 1
dropItemMeta = 0
liquidDousingCost = 10000

[chickens."Steel Chicken"]
texture = "chickens:textures/entity/steelchicken.png"
itemTexture = "chickens:textures/item/chicken/steelchicken.png"
layItemName = "examplemod:steel_ingot"
layItemAmount = 1
dropItemName = "examplemod:steel_ingot"
dropItemAmount = 2
parent1 = "IronChicken"
parent2 = "CoalChicken"
spawnType = "NONE"
layCoefficient = 1.25
displayName = "Steel Chicken"
generatedTexture = false
enabled = true
~~~

The table name is the registry name. Quote it when it contains spaces or punctuation. Stock names already exist, so editing a stock table modifies that existing chicken; it does not create a duplicate. A new table with a new name is constructed and registered during startup.

## Global options in chickens.toml

These are the packaged defaults. Values are validated or clamped by the runtime where noted.

| Option | Type | Default | Behavior |
| --- | --- | ---: | --- |
| `spawnProbability` | Integer | `10` | Base weight for Modern Chickens entries added to compatible vanilla creature spawn pools. Higher values increase the mod's share. |
| `minBroodSize` | Integer | `1` | Minimum natural-spawn brood size. The effective value is at least 1 and capped at 2. |
| `maxBroodSize` | Integer | `2` | Maximum natural-spawn brood size. It cannot be below the minimum or above 2. |
| `netherSpawnChanceMultiplier` | Decimal | `1.0` | Multiplies the configured weight for `HELL` spawns. |
| `overworldSpawnChance` | Decimal | `0.02` | Legacy compatibility value. Modern spawning uses normal biome spawn pools instead of forced player-adjacent spawns. |
| `netherSpawnChance` | Decimal | `0.05` | Legacy compatibility value retained for old pack settings. |
| `endSpawnChance` | Decimal | `0.015` | Legacy compatibility value retained for old pack settings. |
| `alwaysShowStats` | Boolean | `false` | Always shows chicken statistics in supported Jade/WTHIT-style overlays. |
| `roostSpeed` | Decimal | `1.0` | Global Roost production multiplier. Above 1 is faster; below 1 is slower. |
| `breederSpeed` | Decimal | `1.0` | Global Breeder speed multiplier. Above 1 is faster. |
| `roosterAuraMultiplier` | Decimal | `1.25` | Production multiplier supplied by an active nearby rooster aura. Set to 1 to remove the bonus. |
| `roosterAuraRange` | Integer | `4` | Maximum block distance for a Roost to find a rooster aura. Negative values become 0. |
| `nestMaxRoosters` | Integer | `1` | Maximum roosters in one Nest that contribute to the aura. Effective range is 1-16. |
| `nestSeedDurationTicks` | Integer | `1200` | Seed-powered aura duration. Minecraft runs at 20 ticks per second, so 1200 is 60 seconds. Zero disables seed-powered aura time. |
| `mechanicalNestBaseEnergyPerTick` | Integer | `500` | Base FE/t used by a Mechanical Nest while it is actively boosting at least one roost. Negative values become 0. |
| `mechanicalNestEnergyPerRoostPerTick` | Integer | `1000` | Additional FE/t for each active roost being boosted by a Mechanical Nest. Negative values become 0. |
| `mechanicalNestEnergyCostSpeedIncrease` | Decimal | `0.35` | Additive energy-cost increase per Mechanical Nest Speed Upgrade. Five upgrades therefore multiply Nest usage by `2.75`. |
| `mechanicalNestRange` | Integer | `5` | Fixed horizontal aura range for Mechanical Nests. Range Upgrades do not apply to Mechanical Nests. |
| `mechanicalRoostTier1EnergyCost` | Integer | `12500` | Mechanical Roost operation cost for a full slot of 16 tier-1 chickens. Values below 1 become 1. |
| `mechanicalRoostTier10EnergyCost` | Integer | `650000` | Mechanical Roost operation cost for a full slot of 16 tier-10 chickens. Tiers 1-10 interpolate linearly between the tier endpoints. |
| `mechanicalRoostEnergyCostSpeedIncrease` | Decimal | `0.15` | Compounded energy-cost increase per Mechanical Roost Speed Upgrade. The default preserves the existing 15% per-upgrade cost increase. |
| `disableEggLaying` | Boolean | `false` | Prevents vanilla chickens from laying vanilla eggs. It does not disable Modern Chickens production. |
| `collectorScanRange` | Integer | `4` | Search range used by Collectors for collectable output. |
| `avianFluxEffectsEnabled` | Boolean | `true` | Enables Avian Flux Converter light and particle effects without disabling energy transfer. |
| `fluxEggCapacityMultiplier` | Decimal | `1.0` | Multiplies the energy capacity represented by Flux Eggs. Negative values become 0. |
| `avianFluxCapacity` | Integer | `50000` | Avian Flux Converter internal RF/FE capacity. Values below 1 become 1. |
| `avianFluxMaxReceive` | Integer | `4000` | Maximum RF/FE accepted per tick. Negative values become 0. |
| `avianFluxMaxExtract` | Integer | `4000` | Maximum RF/FE output per tick. Negative values become 0. |
| `avianFluidConverterCapacity` | Integer | `8000` | Avian Fluid Converter buffer capacity, normally millibuckets. Values below 1 become 1. |
| `avianFluidConverterTransferRate` | Integer | `2000` | Maximum fluid transfer per tick. Negative values become 0. |
| `avianFluidConverterEffectsEnabled` | Boolean | `true` | Enables Avian Fluid Converter visual effects without disabling processing. |
| `avianChemicalConverterCapacity` | Integer | `8000` | Avian Chemical Converter buffer capacity. Values below 1 become 1. |
| `avianChemicalConverterTransferRate` | Integer | `2000` | Maximum chemical transfer per tick. Negative values become 0. |
| `avianChemicalConverterEffectsEnabled` | Boolean | `true` | Enables Avian Chemical Converter visual effects without disabling processing. |
| `liquidEggHazardsEnabled` | Boolean | `true` | Enables hazard effects and hazard tooltips for hazardous liquid, chemical, and gas eggs. |
| `incubatorEnergyCost` | Integer | `10000` | Energy required for one Incubator operation. Values below 1 become 1. |
| `incubatorCapacity` | Integer | `100000` | Incubator internal energy capacity. Values below 1 become 1. |
| `incubatorMaxReceive` | Integer | `4000` | Maximum Incubator energy received per tick. Values below 1 become 1. |
| `scalingDrops` | Boolean | `true` | Applies chicken gain to production output. Default gains 1-10 produce 1, 3, 6, 10, 16, 23, 31, 41, 52, or 64 items. |
| `enableFluidChickens` | Boolean | `true` | Registers predefined and discovered fluid eggs. |
| `autoRegisterFluidChickens` | Boolean | `false` | Opts into automatic fluid-chicken discovery. Existing authored fluid chickens win, and configured aliases share one canonical chicken. |
| `enableChemicalChickens` | Boolean | `true` | Registers Mekanism chemical eggs/chickens when Mekanism is available. |
| `enableGasChickens` | Boolean | `true` | Registers Mekanism gas eggs/chickens when Mekanism is available. |
| `roostDropCount` | Integer | `64` | Legacy fixed Roost output setting retained for compatibility. Current production uses configured lay stacks and gain scaling. |

### Spawning notes

Modern natural spawning uses the chicken's `spawnType`, the shared creature cap, and the generated spawn plan. Birds are added only to biomes that already support vanilla chickens. The three legacy chance values above do not issue player-adjacent spawn commands.

For precise spawn tuning, use datapack files in `data/<namespace>/chickens/spawn_plans/`. Those files can override weight, brood size, spawn charge, and energy budget.

## Fluid discovery and aliases

Automatic fluid discovery is disabled by default because different mods can register separate fluid IDs for the same named material. Enable `general.autoRegisterFluidChickens` only when the pack wants registry-wide discovery.

When discovery is enabled, Modern Chickens keeps the first authored or code-defined chicken for a canonical fluid and does not create another visible chicken for an alias. Alias rules live in `config/fluid_chicken_aliases.toml`:

~~~toml
[aliases]
"evolvedmekanism:molten_*" = "alltheores:molten_*"
~~~

The left side is an alias registry ID and the right side is the canonical registry ID. A trailing `*` matches the remainder of a fluid path. Rules are exact unless a trailing wildcard is used. If the canonical fluid is not loaded, the source fluid remains independent so optional mods do not silently lose coverage.

Alias rules use registry IDs, not localized display names. Existing generated alias IDs are retained as disabled compatibility descriptors after a canonical rule is added, so old saved Chicken Items and entities can still resolve without adding another JEI-visible chicken.

## Per-chicken options in custom_chickens.toml

These options are valid in every `[chickens.<name>]` table. If a key is omitted, the built-in value or the new definition's fallback is used.

| Option | Type | Default/fallback | Behavior |
| --- | --- | --- | --- |
| `enabled` | Boolean | `true` | Enables the chicken in registries, breeding, and production. A disabled parent also disables dependent breeds. |
| `layCoefficient` | Decimal | `1.0` | Multiplies lay time. 1 is normal, below 1 is faster, above 1 is slower. Negative custom values are clamped. |
| `spawnType` | String | Definition value | Natural-spawn bucket: `NORMAL`, `SNOW`, `END`, `HELL`, or `NONE`. |
| `allowNaturalSpawn` | Boolean | Definition value | Allows a parent-based chicken to enter natural spawn candidates. This is separate from `spawnType`. |
| `allowDousing` | Boolean | `false` | Allows the Avian Dousing Machine to create this chicken from its registered fluid or chemical. Keep this limited to intentional progression starters. |
| `parent1` | String | Definition value or empty | First breeding parent. Empty means no parent. Names are case-insensitive. |
| `parent2` | String | Definition value or empty | Second breeding parent. Both parents must resolve to keep the lineage. |
| `layItemName` | String | Definition value | Namespaced item id produced by the chicken, such as `minecraft:bone` or `chickens:liquid_egg`. It must exist in the item registry. |
| `layItemAmount` | Integer | `1` or definition value | Items produced per lay, clamped to the item's stack limit. |
| `layItemMeta` | Integer | `0` or definition value | Liquid-egg variant/type value. Ordinary items ignore it. |
| `dropItemName` | String | Definition value or lay item | Namespaced item id dropped when the chicken dies. |
| `dropItemAmount` | Integer | `1` or definition value | Death-drop count, clamped to the item's stack limit. |
| `dropItemMeta` | Integer | `0` or definition value | Variant/type value for the death-drop item when supported. |
| `liquidDousingCost` | Integer | `10000` | Fluid amount consumed by the Avian Dousing Machine, normally millibuckets. Always at least 1. |

When editing existing stock entries, these fields are the authoritative editable stock values. Texture and colour fields are creation fields for new chickens; existing built-in presentation remains code-defined unless the entry is a new custom chicken.

## New chicken definition options

A new table uses the table name instead of a JSON `name` field.

| Option | Required | Type | Behavior |
| --- | --- | --- | --- |
| Table name | Yes | String | Unique registry name, for example `[chickens."Steel Chicken"]`. |
| `id` | No | Integer | Positive registry id. Omit it to allocate the next free id. Duplicate ids are rejected. |
| `texture` | Required unless generated | Resource location | In-world texture. Paths are normalized to legal lowercase resource locations. |
| `itemTexture` | No | Resource location | Optional Chicken Item/JEI sprite. Supplying it preserves the authored sprite without legacy tinting. |
| `layItemName` | Yes | Resource location | Item the new chicken produces. It must already exist in the item registry. |
| `layItemAmount` | No | Integer | Production count; defaults to 1 and is capped at the item stack limit. |
| `layItemMeta` | No | Integer | Liquid-egg type/variant. Defaults to 0. |
| `dropItemName` | No | Resource location | Death-drop item. If omitted, the lay item is used. |
| `dropItemAmount` | No | Integer | Death-drop count; defaults to 1 and is capped at the item stack limit. |
| `dropItemMeta` | No | Integer | Death-drop liquid-egg type/variant. Defaults to 0. |
| `backgroundColor` | No | Hex or decimal | RGB colour from `#RRGGBB`, `RRGGBB`, or a decimal integer. Defaults to white. |
| `foregroundColor` | No | Hex or decimal | RGB colour. Defaults to yellow. |
| `parent1` / `parent2` | No | String | Existing chicken names. Both must resolve for breeding data to remain. |
| `spawnType` | No | String | Case-insensitive `NORMAL`, `SNOW`, `END`, `HELL`, or `NONE`. Defaults to `NORMAL`. |
| `layCoefficient` | No | Decimal | Lay-time multiplier. Defaults to 1.0; negative values become 0. |
| `displayName` | No | String | Literal in-game display name. Otherwise the normal translation key is used. |
| `generatedTexture` | No | Boolean | When true, permits the white-chicken texture fallback and generated/tinted presentation. |
| `enabled` | No | Boolean | Enables the new chicken. Defaults to true. |
| `allowNaturalSpawn` | No | Boolean | Allows a parent-based custom chicken into natural spawn candidates. |
| `allowDousing` | No | Boolean | Allows the Avian Dousing Machine to create this custom chicken from its registered fluid or chemical. |

The parser also accepts the legacy JSON-style nested keys `lay_item`, `drop_item`, `background_color`, `foreground_color`, `spawn_type`, `lay_coefficient`, `display_name`, `generated_texture`, and `item_texture` when they are represented as TOML tables/keys. The shipped TOML examples use the flatter camelCase names above because they match the stock tables and are easier to edit.

## Working examples

Change global production and disable one stock breed:

~~~toml
# config/chickens.toml
[general]
roostSpeed = 2.0
avianFluxEffectsEnabled = false
~~~

~~~toml
# config/custom_chickens.toml
[chickens.DiamondChicken]
enabled = false

[chickens.GoldChicken]
enabled = true
spawnType = "NORMAL"
allowNaturalSpawn = true
~~~

Make the basic White Chicken produce three bones:

~~~toml
[chickens.WhiteChicken]
layItemAmount = 3
dropItemAmount = 1
~~~

Add a custom chicken:

~~~toml
[chickens."Copper Chicken"]
texture = "chickens:textures/entity/copperchicken.png"
itemTexture = "chickens:textures/item/chicken/copperchicken.png"
layItemName = "minecraft:copper_ingot"
layItemAmount = 4
dropItemName = "minecraft:copper_ingot"
dropItemAmount = 2
backgroundColor = "#b87333"
foregroundColor = "#f8cfa9"
parent1 = "IronChicken"
parent2 = "WaterChicken"
spawnType = "NONE"
layCoefficient = 1.0
displayName = "Copper Chicken"
generatedTexture = false
enabled = true
~~~

## Load order and validation

Startup follows this order:

1. Create/load `chickens.toml` and import legacy global values if needed.
2. Create/load `custom_chickens.toml`.
3. If an old `chickens.toml` contains per-chicken tables, move those values into the matching tables in `custom_chickens.toml` and remove the old tables.
4. Build the built-in roster.
5. Read new TOML tables. Existing names are treated as stock overrides; new names are constructed and appended.
6. Apply per-chicken options and resolve parents in a second pass.
7. Register the roster and refresh natural spawn plans.

Invalid new entries are rejected with a warning instead of preventing the rest of the roster from loading. Common causes are missing item ids, malformed resource locations, duplicate names, duplicate ids, incomplete parent pairs, and invalid spawn types. Check the server log for the first warning.

The file is not hot-reloaded. If a custom definition is not visible, verify the file is in the active server's `config` directory, the item/texture resource exists, the table name is unique, and Minecraft was fully restarted.

## Migration and recovery

- A new installation creates both TOML files from packaged defaults.
- If an older `chickens.toml` contains `[chickens.<name>]` tables, the first startup moves them to `custom_chickens.toml` so edits are not lost. The old file is rewritten to retain only `[general]`.
- If an older `chickens.cfg` exists before TOML creation, global values are imported into `chickens.toml`. The legacy file remains as a backup.
- Older `chickens.properties` values are accepted as a migration fallback and are not rewritten.
- Existing `chickens_custom.json` files are read as legacy custom definitions. New installations do not create that JSON file; copy its entries into TOML when convenient.
- After migration, edit only the TOML files. When files disagree, `chickens.toml` wins for global values and `custom_chickens.toml` wins for chicken values.
- Keep a backup before large edits. If a change causes unexpected behavior, restore the previous TOML file, restart, and check the server log.

TOML basics: tables use brackets, quoted strings use quotes, comments start with `#`, and each assignment uses `key = value`. Do not use legacy `B:`, `I:`, `D:`, or `S:` prefixes in the new files.
