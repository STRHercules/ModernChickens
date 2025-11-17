# Modern Chickens

Modern Chickens is a NeoForge port of the classic Chickens and Roost mods for Minecraft 1.21.1 . It reintroduces the breeding-driven resource automation gameplay loop while embracing modern Forge-era tooling, datapacks, and mod integrations while also introducing new modernized features!

### Mod Pages: 

- [CurseForge](https://www.curseforge.com/minecraft/mc-mods/modern-chickens)

- [Modrinth](https://modrinth.com/mod/modern-chickens)

## Gameplay Overview

1. **Collect basic chickens.** Use spawn eggs or natural spawns to gather Tier 1 breeds, then throw coloured eggs to obtain dyed variants.
2. **Analyse and breed.** Right-click chickens with the analyzer to view stats, use roosts for passive production, and combine chickens in the breeder to unlock higher tiers.
3. **Automate collection.** Set up collectors next to roosts and henhouses to sweep drops into inventories or item pipes.
4. **Scale production.** Tune [chickens.cfg](https://github.com/STRHercules/ModernChickens/blob/main/Examples/Config/chickens.cfg) to adjust lay rates, breeder speed multipliers, vanilla egg suppression, and natural spawn toggles to match your pack’s balance goals.
5. **Avian Domination!**

## Feature Highlights
	
- **Comprehensive and Customizable chicken roster** - Ports the entire legacy chicken catalogue with stats, drops, and breeding trees exposed through data-driven registries and a persistent [chickens.cfg](https://github.com/STRHercules/ModernChickens/blob/main/Examples/Config/chickens.cfg) configuration file. Chickens can be customised, disabled, or reparented without recompiling the mod.
- **Dynamic material coverage** - Generates placeholder chickens for any ingot item detected at runtime, using a shared fallback texture and Smart Chicken lineage to keep mod packs covered without manual config tweaks.
- **Automation blocks** - Roosts, breeders, collectors, the Avian Flux Converter, the Avian Fluid Converter, the Avian Chemical Converter, and the Avian Dousing Machine ship with their original block entities, menus, and renderers so farms can incubate, store, transmute, and harvest chickens hands-free.
- **Dedicated items** - Spawn eggs, coloured eggs, liquid eggs, chemical and gas eggs, chicken catchers, and analyzer tools keep the legacy progression loop intact while adopting modern capability and tooltip systems.
- **Fluid and chemical automation** - Liquid eggs can be cracked into configurable fluid stacks with the Avian Fluid Converter, and chemical or gas eggs feed the Avian Chemical Converter to build Mekanism-compatible buffers. Both machines ship tank overlays, JEI recipes, and Jade/WTHIT readouts—plus Modern Chickens now generates fluid chickens automatically for every registered liquid in your pack and chemical chickens for every discovered Mekanism chemical.
- **JEI and Jade integrations** - Recipe categories, item subtypes, and Jade/WTHIT overlay tooltips surface roost, breeder, converter, dousing, and chicken stats directly in-game when the companion mods are installed.
- **Modded Chicken Support** Modern Chickens will identify all 'ingot' resources in your minecraft instance and generate resource chickens for them.
- **Specialty resource integrations** - Prebuilt chickens now cover high-value items across popular tech/magic mods. Each registers only when its parent mod is loaded, keeping packs dependency-safe.

## Integrated Mods & Unlockable Chickens

- **Applied Energistics 2** — Certus Quartz, Charged Certus, Silicon, Fluix, Sky Stone.
- **Extended AE / Advanced AE** — Entro Alloy, Quantum Alloy.
- **Applied Fluix / Applied Generators** — Applied Fluix Crystal, Ember Crystal.
- **Mekanism** — Fluorite.
- **EvilCraft** — Blood.
- **Just Dire Things** — Celestigem, Time Crystal.
- **Industrial Foregoing** — Plastic, Rubber.
- **Draconic Evolution** — Chaos Shard.
- **Mystical Agriculture** — Inferium, Prudentium, Tertium, Imperium, Supremium, Insanium essences.
- **Powah** — Uraninite.
- **Flux Networks** — Flux Dust.
- **Actually Additions** — Black Quartz, Restonia, Diamatine, Emeradic, Enori, Palis, Void crystals.
- **Vanilla** — Amethyst (Amethyst Shard), Nether Stars, Dragon Eggs

## Boss Chickens via Avian Dousing (Dragon / Wither)
You cannot breed these; you must infuse them in the **Avian Dousing Machine**.

1. **Base chicken**
   - Dragon: place an **Obsidian Chicken** in the input slot.
   - Wither: place a **Soul Sand Chicken** in the input slot.

2. **Fill the special buffer**
   - Dragon: insert **Dragon’s Breath** bottles.
   - Wither: insert **Nether Stars**.
   - Each item adds **100 mB**; you need **1000 mB** total (10 items).

3. **Supply RF**
   - Ensure the dousing machine is powered; it consumes the listed RF during infusion.

4. **Start infusion**
   - When the special buffer is full and RF is available, start the process. The base chicken is consumed and replaced with the boss chicken spawn egg:
     - **Dragon Chicken** → lays **Dragon Eggs**.
     - **Wither Chicken** → lays **Nether Stars**.

5. **Automate**
   - Place the resulting chicken in a roost/henhouse to farm Dragon Eggs or Nether Stars hands‑free.

_Chickens available in ATM10!_

![Chickens Available in ATM10](https://i.imgur.com/YxIPuRC.gif)

![Look at all these CHICKENTHS!](https://media1.tenor.com/m/hRVtp7V06GQAAAAd/look-at.gif)


> Modded Chickens will choose random assets for the item version, and use a texture that is generated on the fly for them in the overworld


> Configuration and breeding data live in `config/chickens.cfg`. The file is generated on first run and can be safely edited while the game is stopped. Restart the client or server—or run `/chickens export breeding`—to reload breeding graphs after making changes. 


> Need a quieter base? Flip `general.avianFluxEffectsEnabled=false` to disable the Avian Flux Converter's light and particle effects without touching code.

> Want different RF pacing? Adjust `general.fluxEggCapacityMultiplier` for Flux Egg storage or tweak `general.avianFluxCapacity`, `general.avianFluxMaxReceive`, and `general.avianFluxMaxExtract` to rebalance the converter.

> Prefer gentler handling? Set `general.liquidEggHazardsEnabled=false` to suppress the liquid egg status effects, and tune `general.avianFluidConverterCapacity` / `general.avianFluidConverterTransferRate` to balance fluid throughput.

> Existing `chickens.properties` files are loaded once (for migration) but are no longer written—feel free to delete them after confirming the upgrade. (only if upgrading from an old version that used this file)

## New Egg Assets

_Dynamic Fluid Eggs!_

![Dynamic Fluid Eggs](https://i.imgur.com/37OGGEr.png)

_Dynamic Chemical Eggs!_

![Dynamic Chemical Eggs](https://i.imgur.com/jG71Xpp.png)

_New Redstone Flux Egg Asset!_

![Redstone Flux Eggs](https://i.imgur.com/WklUeVL.png)

> Dynamic Fluid and Chemical Eggs will mimic their respective resource's color.
	
## Avian Fluid/Chemical Converters

_Converting Eggs to Fluids and Chemicals!_

![Converters!](https://i.imgur.com/kokLuyI.jpeg)
	
Liquid eggs no longer require hand-placing to deploy their contents. Drop a liquid or Chemical egg in the respective Avian Converter and it automatically cracks the shell, stores the fluid/chemical internally, and feeds adjacent tanks or pipes each tick.
	
- **Discovery**: JEI adds an “Avian Fluid/Chemical Converter” category that lists every liquid/chemical egg and the fluid/chemical volume it produces. The converter block is registered as a catalyst, so recipes are only a click away.
- **Catch ’em all**: Modern Chickens now creates a dedicated chicken for every fluid and chemical detected at runtime—if a mod ships a new liquid or chemical, you get a matching bird and egg automatically.
- **Monitoring**: WTHIT and Jade overlays mirror the in-block gauge with the current fluid/chemical name, stored amount, and tank capacity. You can check the converter’s status without opening the GUI.
- **Balancing**: Server owners can adjust `general.avianFluidConverterCapacity`, `general.avianFluidConverterTransferRate`, and `general.liquidEggHazardsEnabled` in `config/chickens.cfg` to match the rest of their tech progression.
	
Pair the converter with standard fluid/chemical transport (pipes, tubes, tanks, or machines) to integrate chickens into modded processing lines—experience, biofuel, radioactive waste, and other tech fluids now flow straight from the coop.

## Avian Dousing Machine

The Avian Dousing Machine ties the Smart Chicken lineage into the new fluid and chemical chicken generators. It consumes stored reagents and Redstone Flux to mint spawn eggs for the dynamically generated liquid and chemical chickens, letting you unlock those breeds in survival without commands or JSON edits.

_Converting Chemicals!_

![Converting Chemicals](https://i.imgur.com/fwZnEtI.jpeg)

_Converting Liquids!_

![Converting Liquids](https://i.imgur.com/Ge7zx4b.jpeg)

- **Inputs**: Place a Smart Chicken spawn egg or captured Smart Chicken in the left slot. The machine only accepts Smart Chickens, treating them as blank templates to be imprinted with a reagent.
- **Reagents**: Pipe liquids into the built-in fluid tank (often from an Avian Fluid Converter) and feed Mekanism chemicals or gases into the internal chemical buffer directly or via an Avian Chemical Converter. The machine automatically prefers chemicals when both buffers are available.
- **Outputs**: When it has enough energy and reagent, the Dousing Machine consumes one Smart Chicken plus a chunk of stored liquid or chemical and creates the corresponding liquid or chemical chicken spawn egg in the output slot. Each egg hatches into a chicken that lays the matching liquid or chemical egg item.
- **Discovery tools**: With Mekanism and JEI installed, an “Avian Dousing Machine” recipe category lists each supported chemical egg, its Smart Chicken input, reagent cost, RF cost, and the resulting chemical chicken spawn egg so you can plan your automation chain.
- **Status at a glance**: WTHIT overlays mirror the machine’s stored fluid and energy plus its infusion progress, so you can confirm whether a cycle is about to complete without opening the GUI.

Combining the Dousing Machine with the Fluid and Chemical Converters lets you go from liquids or Mekanism chemicals → eggs → buffered tanks → dedicated chickens entirely inside the Modern Chickens ecosystem.

![Dousing Process](https://i.imgur.com/kSI861D.gif)

![Dousing WTHIT](https://i.imgur.com/V25C4td.png)

## Roosters

![Rooster](https://i.imgur.com/d49iLC3.png)

Roosters are utility birds inspired by Hatchery’s rooster: they never lay eggs themselves, but they store seeds and power nearby roosts when paired with nests.

- **Behaviour**: Roosters use chicken AI (wandering, following food, panic) but keep their internal egg timer above the lay threshold, so they never produce eggs directly.
- **Seed storage**: Right-clicking a rooster opens a small inventory where you can feed it seeds (anything tagged as `#minecraft:chicken_food`). Internally the rooster converts pairs of seeds into a lightweight “seed charge” used for GUIs and future breeding logic.
- **Item form**: Using the Chicken Catcher on a rooster turns it into a specialised chicken item marked as a rooster. That item can be placed back into the world as a rooster, or dropped into a Nest to contribute aura.
- **Roost synergy**: Roosts scan the area around them for active nests. Each rooster in an active nest adds a production bonus on top of the base roost speed.

Rooster-related configuration entries live in the `general` section of `config/chickens.cfg`:

| Key | Type | Default | Description |
| --- | --- | --- | --- |
| `general.roosterAuraMultiplier` | Double | `1.25` | Multiplier applied to roost production when exactly one active rooster is found. Additional roosters scale linearly on top of this (e.g., `1.25` with three roosters yields `1 + 3 × 0.25 = 1.75` times the base rate). Values at or below `1.0` effectively disable the aura bonus. |
| `general.roosterAuraRange` | Integer | `4` | Horizontal search radius (in blocks) used by roosts to find active nests. `0` disables rooster aura entirely; negative values are treated as `0`. |
| `general.roostSpeedMultiplier` | Double | `1.0` | Global speed multiplier applied to all roosts before the rooster aura is considered. Use this to fine-tune overall production pacing; roosters then stack on top of the adjusted baseline. |

## Nest

![Nest](https://i.imgur.com/0Pqh6ng.png)

The Nest is a small block that turns captured roosters and seeds into an aura that boosts nearby roosts. It does not produce items on its own; its sole job is to power the rooster aura.

- **Inventory layout**: The GUI exposes two slots – a **seed slot** on the left and a **rooster slot** on the right. The block entity also behaves as a sided inventory so hoppers and item pipes can automate both inputs.
- **Accepted items**: The seed slot accepts vanilla seeds (`wheat`, `beetroot`, `melon`, `pumpkin`). The rooster slot only accepts rooster-marked chicken items; regular chicken items must go into roosts or breeders instead.
- **Aura fuel**: When at least one rooster is present and seeds are available, the Nest consumes one seed at a time and converts it into “aura time”. As long as the internal timer has time remaining, the nest is considered active and will be picked up by nearby roosts.
- **Rooster cap**: Only a limited number of roosters are counted per nest; any extra birds above the configured cap are ignored for aura strength, though they still occupy item space.

Nest behaviour is controlled by these `general` entries in `config/chickens.cfg`:

| Key | Type | Default | Description |
| --- | --- | --- | --- |
| `general.nestMaxRoosters` | Integer | `1` | Maximum number of roosters a single nest will count towards aura strength. Values are clamped to the range `1–16` when read from the config, so malformed values cannot create excessively large stacks. |
| `general.nestSeedDurationTicks` | Integer | `1200` | How long (in ticks) a single consumed seed keeps the nest’s aura active. There are 20 ticks per real-time second, so the default of `1200` equals 60 seconds per seed. Setting this to `0` disables aura production from nests entirely (they will hold roosters but never turn seeds into aura). |

Nests and roosters together form a flexible tuning knob for roost-based farms: you can keep `roostSpeedMultiplier` near `1.0` for baseline balance, then use nests plus roosters to introduce optional “booster stations” for higher-end automation builds.

## Incubator

![Incubator](https://i.imgur.com/5pxcUQB.png)

![Incubator GUI](https://i.imgur.com/a7w4Cdl.png)

![Incubator WTHIT](https://i.imgur.com/tBZfKtz.png)

The Incubator is a compact RF-powered machine that turns chicken spawn eggs into portable chicken items that can be dropped into roosts, breeders, or back into the world as entities.

- **Inputs and outputs**: The left slot accepts chicken spawn eggs from the mod. When fully charged and supplied with an egg, the Incubator consumes RF and the egg to create a matching chicken item in the right slot using default stats for that breed.
- **Energy system**: The block maintains an internal RF buffer and periodically pulls energy from adjacent blocks using NeoForge’s energy capability. An on-screen energy bar and tooltip show the current and maximum stored RF.
- **Processing**: Each operation advances over a fixed progress bar (200 ticks by default). Once both the progress bar and the reserved energy meet the configured cost for the current egg, the machine outputs a chicken item and moves on to the next egg.
- **Automation**: The Incubator exposes a standard sided inventory – eggs can be inserted from the top or sides, and completed chicken items are extracted from the bottom. This makes it easy to chain breeders → incubators → roosts in fully automated setups.

All Incubator tuning lives under `general` in `config/chickens.cfg`:

| Key | Type | Default | Description |
| --- | --- | --- | --- |
| `general.incubatorEnergyCost` | Integer | `10000` | RF cost to incubate a single egg into a chicken item. Values less than `1` are treated as `1`. Higher values slow processing unless you also increase capacity and transfer rate. |
| `general.incubatorCapacity` | Integer | `100000` | Size of the Incubator’s internal RF buffer. This caps how much power the machine can hold at once and therefore how many eggs it can process back to back without recharging. |
| `general.incubatorMaxReceive` | Integer | `4000` | Maximum RF per tick the Incubator will pull from adjacent blocks. Raising this lets high-end generators refill the internal buffer more quickly; lowering it soft-caps the machine’s throughput even if a large buffer is configured. |

Together, the Incubator, Nests, and Roosters provide a smoother progression from early-game eggs to mid- and late-game automation: you can breed for the chickens you want, incubate their spawn eggs into portable items, and then use roosters and nests to push roost farms far beyond their vanilla throughput.

## Redstone Flux

![RF Chicken Generator!](https://i.imgur.com/Djoeb5P.jpeg)


Modern Chickens introduces power generation through a dedicated Redstone Flux progression line:

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

> **Assumptions:** RF/t values use the average lay time and treat each Flux Egg as delivering its entire stored energy (1,000 RF for base birds, 3,700 RF ×3 eggs for maxed birds). Actual outputs fluctuate slightly with the random lay timer and any custom `roostSpeedMultiplier` tweaks in [chickens.cfg](https://github.com/STRHercules/ModernChickens/blob/main/Examples/Config/chickens.cfg).

### Same graph in RF/s:

| Installation | RF/s per roost | 10 roosts | 20 roosts | 30 roosts |
|---|---:|---:|---:|---:|
| 1× base Redstone Flux Chicken (1/1/1) | ≈0.8 | ≈7.4 | ≈14.8 | ≈22.2 |
| 1× max-stat Redstone Flux Chicken (10/10/10) | ≈82.2 | ≈822.2 | ≈1,644.4 | ≈2,466.6 |
| 16× max-stat Redstone Flux Chickens (full roost of 10/10/10) | ≈1,315.6 | ≈13,155.6 | ≈26,311.2 | ≈39,466.6 |

	
## Custom Chicken Definitions

![Custom Chickens](https://i.imgur.com/3jxZfxf.gif)

- After first run, the mod will generate a `chickens_custom.json` file in the `config` directory where you can add bespoke chickens without recompiling the mod. The starter file will also have an example baked in.
- Each entry in the `chickens` array controls the chicken name, texture, lay/drop items, breeding parents, lay coefficient, and optional display name. Any missing field falls back to the mod defaults so you can tweak as much or as little as you like.
- Custom chickens participate in the existing [chickens.cfg](https://github.com/STRHercules/ModernChickens/blob/main/Examples/Config/chickens.cfg) flow, meaning you can still fine-tune them (enable/disable, change drops, reparent) alongside the built-in roster.

Example [chickens_custom.json](https://github.com/STRHercules/ModernChickens/blob/main/Examples/Custom%20Chickens/chickens_custom.json) entries (place inside the top-level `chickens` array):


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

### Spawn Plan Overrides

Natural spawning now reads tuning data from `ChickensSpawnManager`, and datapacks can override those values without editing configs or code. Place JSON files inside `data/<namespace>/chickens/spawn_plans/` with the following structure:

| Field | Type | Behaviour |
| --- | --- | --- |
| `spawn_type` | String (required) | One of `normal`, `snow`, or `hell`. Determines which biome bucket the override applies to. |
| `spawn_weight` | Integer | Absolute weight for the biome bucket. When omitted, the value derived from [chickens.cfg](https://github.com/STRHercules/ModernChickens/blob/main/Examples/Config/chickens.cfg) is used. |
| `weight_multiplier` | Float | Multiplies the config-derived weight instead of overriding it. Cannot be combined with `spawn_weight`. |
| `min_brood_size` | Integer | Overrides the minimum flock size inserted into the biome modifier. |
| `max_brood_size` | Integer | Overrides the maximum flock size. Automatically clamps to the configured minimum when smaller. |
| `spawn_charge` | Float | Replaces the mob charge applied to the biome modifier (vanilla uses this to cap spawn density). |
| `energy_budget` | Float | Overrides the spawn energy budget passed to the biome modifier alongside the mob charge. |

Example datapack snippet that boosts overworld chicken density while limiting flocks to two birds:

```json
{
  "spawn_type": "normal",
  "weight_multiplier": 1.5,
  "min_brood_size": 1,
  "max_brood_size": 2
}
```

Reloading datapacks (or restarting the server) automatically reapplies these overrides; removing the JSON restores the configuration defaults.

Global spawn helpers are also configurable via [chickens.cfg](https://github.com/STRHercules/ModernChickens/blob/main/Examples/Config/chickens.cfg):

- `general.overworldSpawnChance` (default `0.02`) controls the chance per check that the overworld spawn helper runs.
- `general.netherSpawnChance` (default `0.05`) works alongside `netherSpawnChanceMultiplier` to determine Nether burst frequency.
- `general.endSpawnChance` (default `0.015`) controls the forced spawns used in The End.
These values accept floats between `0` and `1`; set them lower for rarer spawns or increase them if you want denser testing without relying on the debug multiplier.

For on-the-fly testing, `/chickens spawn multiplier <value>` multiplies every biome weight (set back to `1` to restore defaults) and `/chickens spawn debug <true|false>` toggles chat spam that reports each natural chicken spawn with its breed and coordinates.
When you need an immediate test subject, `/chickens spawn summon <chickenNameOrId>` spawns that breed at your feet and `/chickens spawn summon_random [normal|snow|end|hell]` picks a random chicken from the requested biome bucket.

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
| `spawn_type` | No | String | Case-insensitive values drawn from `normal`, `snow`, `end`, `hell`, or `none`. Defaults to `normal`. |
| `lay_coefficient` | No | Float | Multiplier applied to lay times. Values below `0` are clamped to `0`. Defaults to `1.0`. |
| `display_name` | No | String | Overrides the in-game display name. Defaults to the translated name derived from `name`. |
| `generated_texture` | No | Boolean | Set to `true` to tint the configured texture (or the base white chicken if the texture is missing) using the `background_color`/`foreground_color` pair. When `false`, the renderer uses the texture as-is and only falls back to tinting if that texture cannot be loaded. Defaults to `false`. |
| `enabled` | No | Boolean | Toggles whether the chicken participates in registries and breeding. Defaults to `true` and cascades with parent availability. |
| `item_texture` | No | Resource location | Optional namespaced path pointing at the item sprite (`namespace:textures/item/...png`). When omitted, the loader assumes a sprite lives at `chickens:textures/item/chicken/<lowercase name>.png`. Custom sprites supplied through the JSON file remain authoritative; missing resources log a warning and display Minecraft’s purple-and-black placeholder instead of swapping back to the tinted fallback. When the referenced art already ships with a baked model (for example, reusing an existing Modern Chickens texture), the runtime reuses that model directly; otherwise it now generates a vanilla `minecraft:item/generated` quad on the fly so datapack-only textures render as expected. |
| *(config only)* `allowNaturalSpawn` | No | Boolean | When `true`, higher-tier chickens are allowed to join natural spawn tables even if they have parents. Only exposed through [chickens.cfg](https://github.com/STRHercules/ModernChickens/blob/main/Examples/Config/chickens.cfg); defaults to `false` for breeds with parents. |

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
./gradlew clean build
```

The build copies legacy resources from `OriginalChickens`, optionally mirrors Roost textures from `roost`, and produces a distributable JAR in `build/libs/`.

> **Tip:** On first launch the build may download NeoForge dependencies; subsequent runs complete much faster. Use `./gradlew --info build` if you need detailed logging while debugging build issues.


## Support and Contributions

- File gameplay bugs or crash reports through the project issue tracker.
- Keep pull requests focused and ensure `./gradlew build` succeeds before submitting.

*Modern Chickens inherits the MIT license from the original project. See `LICENSE` (when present) or the mod metadata for details.*

Happy hatching!
