# Modern Chickens

Modern Chickens is a NeoForge 1.21.1 port of the classic **Chickens** and **Roost** mods. Rebuild the original breeding-driven resource automation loop with a large chicken roster, configurable breeding trees, automated coops, dynamic fluid and chemical content, RF generation, and optional Mekanism radiation integration.

## Requirements and optional integrations

- **Minecraft:** 1.21.1
- **Loader:** NeoForge 21.1.x
- **Mekanism:** Optional. Required for Mekanism chemical/gas chickens and the Mekanism radiation bridge.
- **JEI:** Optional. Adds breeding, chicken-stat, converter, and dousing recipe categories.
- **Jade/WTHIT:** Optional. Adds chicken, machine, tank, energy, and progress overlays.

Mod-specific chickens and dynamic content are registered only when the relevant parent mod or resource exists. The core breeding and Roost gameplay does not require every integration listed on this page.

## Gameplay overview

- **Collect basic chickens.** Use natural spawns, spawn eggs, and the early Tier 1 roster to start your collection. Throw coloured eggs to obtain dyed variants.
- **Analyse and breed.** Right-click chickens with the **Analyzer** to view Growth, Gain, and Strength. Combine chickens in the **Breeder** to unlock higher tiers.
- **Automate production.** Put chickens in **Roosts** and use **Collectors**, hoppers, and item pipes to move their drops into storage.
- **Scale the coop.** Use **Henhouses**, **Nests**, **Roosters**, the **Incubator**, and the Avian converters to build hands-free production lines.
- **Tune the pack.** Edit `config/chickens.cfg` to adjust lay rates, breeder speed, drop scaling, spawn behavior, vanilla egg suppression, and integration settings.

The roster, drops, parents, spawn types, textures, and lay coefficients are data-driven. Higher-tier chickens normally do not spawn naturally, so breeding remains the intended progression path.

## Feature highlights

- A large legacy-inspired chicken catalogue with stats, drops, breeding trees, spawn eggs, coloured eggs, Chicken Items, and Chicken Catchers.
- Configurable Roosts, Breeders, Henhouses, Collectors, Nests, Roosters, and an RF-powered **Incubator**.
- An **Avian Fluid Converter** for cracking liquid eggs into fluids and feeding adjacent tanks or pipes.
- An **Avian Chemical Converter** for cracking Mekanism chemical and gas eggs into buffered chemical storage.
- An **Avian Dousing Machine** for imprinting Smart Chickens with a fluid or Mekanism chemical and creating the matching chicken spawn egg.
- An **Avian Flux Converter** and **Redstone Flux Chickens** for converting Flux Eggs into stored and exportable RF.
- Runtime-generated chickens for registered fluids, Mekanism chemicals, Mekanism gases, and detected ingot resources.
- JEI recipe categories, item subtypes, and Jade/WTHIT overlays for chickens, stats, breeding, tanks, machines, and progress.
- Custom chicken definitions through `config/chickens_custom.json`, without recompiling the mod.
- Datapack spawn-plan overrides and commands for inspecting or testing spawn behavior.

## Progression and natural spawning

Modern Chickens adds modded birds to biomes that already have a vanilla chicken spawn. It uses the shared `CREATURE` mob cap rather than a separate population system. The default natural-spawn broods are 1-2 birds; roosters spawn alone at low weight. Most parent-based chickens remain breeding-only unless their configuration allows natural spawning.

Mega Chickens use their own rare spawn rule. See [Mega Chickens](#mega-chickens) below.

### Spawn-plan datapacks

Spawn overrides live under:

```text
data/<namespace>/chickens/spawn_plans/
```

Spawn plans can override these fields:

- `spawn_type` - `normal`, `snow`, `end`, or `hell`.
- `spawn_weight` - Absolute spawn weight. Use this or `weight_multiplier`, not both.
- `weight_multiplier` - Multiplies the configured base weight.
- `min_brood_size` and `max_brood_size` - Spawn group size bounds.
- `spawn_charge` and `energy_budget` - Mob-charge values used by the biome spawn settings.

### Useful commands

```text
/chickens spawn multiplier <value>
/chickens spawn debug <true|false>
/chickens spawn summon <chickenNameOrId>
/chickens spawn summon_random [normal|snow|end|hell]
/chickens custom list
/chickens export breeding
```

## Integrated mods and unlockable chickens

Prebuilt integrations register only when their parent mod is loaded. Runtime ingot detection and dynamic fluid/chemical registration extend the list further.

- **Applied Energistics 2:** Certus Quartz, Charged Certus, Silicon, Fluix, Sky Stone.
- **Extended AE / Advanced AE:** Entro Alloy, Quantum Alloy, Entro Crystal.
- **Applied Fluix / Applied Generators:** Applied Fluix Crystal, Ember Crystal.
- **Mekanism:** Fluorite, HDPE Pellet, Plutonium Pellet, Polonium Pellet, Antimatter Pellet.
- **Mekanism Extras:** Naquadah ingots, fluids, and chemicals.
- **EvilCraft:** Blood.
- **Just Dire Things:** Celestigem, Eclipse Alloy, Time Crystal.
- **Industrial Foregoing:** Plastic, Rubber.
- **Avaritia:** Neutron Pile, Infinity Ingot.
- **RFTools Base:** Dimensional Shard.
- **Beyond Dimensions:** Shattered Space-Time Crystal.
- **Applied Flux:** Redstone Crystal.
- **SpectreThings / Irregular Implements:** Ectoplasm, using one consolidated chicken when either mod is present.
- **Draconic Evolution:** Small Chaos Fragment.
- **Mystical Agriculture:** Inferium, Prudentium, Tertium, Imperium, Supremium, and Insanium essences.
- **Powah:** Uraninite.
- **Flux Networks:** Flux Dust.
- **Actually Additions:** Black Quartz, Restonia, Diamatine, Emeradic, Enori, Palis, and Void Crystals.
- **Extreme Reactors:** Yellorium, Graphite, Cyanite, and Blutonium through the normal material integration.
- **Ender IO:** Electrical Steel, Energetic Alloy, Vibrant Alloy, Redstone Alloy, Conductive Iron, Pulsating Iron, Dark Steel, and Soularium.
- **Immersive Engineering:** Registered ingot and fluid resources, including uranium-related content when present.
- **Vanilla:** Amethyst Shards, Nether Stars, and Dragon Eggs.

The exact roster depends on the mods and registered resources in your instance. JEI and the Analyzer are the authoritative in-game references.

![Chickens available in ATM10](https://i.imgur.com/YxIPuRC.gif)

![Look at all these chickens](https://media1.tenor.com/m/hRVtp7V06GQAAAAd/look-at.gif)

## Dynamic eggs, fluids, chemicals, and gases

Modern Chickens generates a dedicated chicken and egg variant for supported registered fluids, Mekanism chemicals, and Mekanism gases during setup. Generated eggs inherit the resource's name and colour. Dynamic fluid and chemical eggs also mimic the colour of their resource.

The complete automation chain is:

**Resource chicken -> liquid/chemical/gas egg -> converter buffer -> pipes, tanks, or machines**

![Dynamic fluid eggs](https://i.imgur.com/37OGGEr.png)

![Dynamic chemical eggs](https://i.imgur.com/jG71Xpp.png)

### Avian Fluid and Chemical Converters

Liquid and chemical eggs no longer need to be hand-placed to deploy their contents. Put a liquid egg into the **Avian Fluid Converter**, or a chemical/gas egg into the **Avian Chemical Converter**. The converter cracks the shell, stores the contents in an internal buffer, and feeds adjacent tanks, pipes, tubes, or machines each tick.

- **JEI discovery:** JEI lists every supported egg and the fluid or chemical volume it produces. The converter block is registered as a JEI catalyst.
- **Runtime coverage:** A new registered fluid or Mekanism chemical can automatically receive a matching chicken and egg without a hardcoded page entry or manual JSON edit.
- **Monitoring:** Jade and WTHIT show the resource name, stored amount, tank capacity, and machine state without opening the GUI.
- **Automation:** Pair the converter with normal fluid or chemical transport to move experience, biofuel, radioactive waste, and other technology resources directly from the coop.

Useful server configuration keys are:

- `general.avianFluidConverterCapacity` - Fluid buffer capacity; default `8000` mB.
- `general.avianFluidConverterTransferRate` - Fluid transfer rate; default `2000` mB/t.
- `general.avianChemicalConverterCapacity` - Chemical buffer capacity; default `8000` units.
- `general.avianChemicalConverterTransferRate` - Chemical transfer rate; default `2000` units/t.
- `general.liquidEggHazardsEnabled` - Enables liquid-egg hazard tooltips and placement effects; default `true`.

![Avian Fluid and Chemical Converters](https://i.imgur.com/kokLuyI.jpeg)

## Avian Dousing Machine

The **Avian Dousing Machine** connects the Smart Chicken lineage to the dynamic fluid and chemical chicken generators. It consumes a Smart Chicken, a stored reagent, and RF to create the matching chicken spawn egg, letting you unlock generated breeds in survival without commands or manual registry edits.

- **Input:** Place a Smart Chicken spawn egg or captured Smart Chicken in the input slot. Only Smart Chickens are accepted as blank templates.
- **Fluid reagent:** Pipe a fluid into the built-in tank, often from an Avian Fluid Converter.
- **Chemical reagent:** Feed a Mekanism chemical or gas into the internal buffer directly or through an Avian Chemical Converter.
- **Priority:** When both reagent types are available, the machine prefers the chemical buffer.
- **Output:** One Smart Chicken and the required reagent are consumed to create the corresponding liquid or chemical chicken spawn egg. That chicken lays the matching liquid or chemical egg.
- **Adjacent transport:** The machine can pull compatible fluids and Mekanism chemicals from adjacent tanks, pipes, tubes, or converters.
- **JEI:** The Avian Dousing Machine category lists supported reagents, costs, RF requirements, and output spawn eggs.
- **Monitoring:** Jade and WTHIT show stored reagent, energy, and infusion progress.

![Converting chemicals](https://i.imgur.com/fwZnEtI.jpeg)

![Converting liquids](https://i.imgur.com/Ge7zx4b.jpeg)

![Avian Dousing Machine process](https://i.imgur.com/kSI861D.gif)

The resulting progression is:

**Liquid or Mekanism chemical -> egg -> buffered converter -> Avian Dousing Machine -> dedicated chicken**

## Henhouse

The **Henhouse** collects nearby chicken drops into its internal inventory. It accepts either hay bales or FE/RF from a standard energy provider.

- One charge collects one item.
- A hay bale provides the internal charge and spent hay becomes dirt.
- Outputs can be extracted from below by hoppers or item pipes.
- It is useful for Dragon Eggs, Nether Stars, and other drops that should be collected without placing chickens in a Roost.

## Boss Chickens via Avian Dousing

Dragon and Wither Chickens cannot be bred. They must be infused in the **Avian Dousing Machine**.

### 1. Add the base chicken

- **Dragon Chicken:** Place an **Obsidian Chicken** in the input slot.
- **Wither Chicken:** Place a **Soul Sand Chicken** in the input slot.

### 2. Fill the special buffer

- **Dragon Chicken:** Insert Dragon's Breath Bottles.
- **Wither Chicken:** Insert Nether Stars.
- Each special item adds `100 mB`.
- The special buffer requires `1000 mB` total, or `10` items.

### 3. Supply power

Ensure the Avian Dousing Machine is powered. It consumes the configured RF during infusion.

### 4. Start the infusion

When the special buffer is full and enough RF is available, the base chicken is consumed and replaced by the appropriate boss chicken spawn egg.

- **Dragon Chicken -> lays Dragon Eggs**
- **Wither Chicken -> lays Nether Stars**

### 5. Automate production

Place the resulting chicken in a Roost or Henhouse to farm Dragon Eggs or Nether Stars hands-free.

## Roosters

![Rooster](https://i.imgur.com/d49iLC3.png)

Roosters are utility birds inspired by Hatchery's rooster. They never lay eggs themselves; their job is to support breeding and Roost production.

- **Normal chicken AI:** Roosters wander, follow chicken food, panic, and seek nearby adult hens.
- **Seed charge:** Right-click a rooster with an empty hand or chicken food to open its small inventory. Items in `#minecraft:chicken_food` are accepted. Every pair of seeds becomes two points of internal seed charge.
- **Fertilization:** A charged adult rooster seeks a nearby adult non-rooster chicken, spends two seed-charge points, and triggers breeding. The hen determines the offspring type and genetics.
- **Item form:** The Chicken Catcher turns a mature rooster into a Chicken Item marked as a rooster. The item can be placed back into the world or inserted into a Nest.
- **Roost synergy:** Roosts scan within the configured rooster-aura range for active Nests. Roosters in an active Nest add a production bonus on top of the base Roost speed.

Default rooster-related settings:

| Key | Default | Purpose |
| --- | ---: | --- |
| `general.roosterAuraMultiplier` | `1.25` | Roost production multiplier while a rooster aura applies. |
| `general.roosterAuraRange` | `4` | Range used when Roosts look for active Nests. |
| `general.roostSpeed` | `1.0` | Base Roost speed multiplier. |

## Nest

![Nest](https://i.imgur.com/0Pqh6ng.png)

The **Nest** turns captured roosters and seeds into an aura that boosts nearby Roosts. It does not produce items by itself.

- **Two slots:** The GUI has a seed slot on the left and a rooster slot on the right.
- **Accepted seeds:** Wheat, beetroot, melon, and pumpkin seeds.
- **Rooster slot:** Accepts only rooster-marked Chicken Items. Regular Chicken Items belong in Roosts or Breeders.
- **Automation:** The Nest exposes sided inventory access for hoppers and item pipes.
- **Aura fuel:** When at least one rooster is present, the Nest consumes one seed and turns it into aura time. It pauses seed consumption when no rooster is present.
- **Rooster cap:** Only up to the configured number of roosters contributes to the aura. Extra roosters do not increase its strength.

Default Nest settings:

| Key | Default | Purpose |
| --- | ---: | --- |
| `general.nestMaxRoosters` | `1` | Maximum roosters counted by one Nest. |
| `general.nestSeedDurationTicks` | `1200` ticks | Aura time provided by one seed; 60 seconds. Set to `0` to disable aura production. |

## Incubator

![Incubator](https://i.imgur.com/5pxcUQB.png)

![Incubator GUI](https://i.imgur.com/a7w4Cdl.png)

The **Incubator** is an RF-powered machine that turns mod chicken spawn eggs into portable Chicken Items with default `1/1/1` stats. These items can be inserted into Roosts or Breeders, or placed back into the world as entities.

- **Input:** The left slot accepts Modern Chickens spawn eggs.
- **Output:** The right slot produces the matching portable Chicken Item.
- **Energy:** The Incubator pulls RF from adjacent blocks through the NeoForge energy capability and displays its stored energy in the GUI.
- **Processing:** Each operation uses a 200-tick progress bar by default and reserves the configured RF cost as it advances.
- **Automation:** Eggs can be inserted from the top or sides; Chicken Items can be extracted from the bottom.

Default Incubator settings:

| Key | Default | Purpose |
| --- | ---: | --- |
| `general.incubatorEnergyCost` | `10000` RF | Energy cost per Chicken Item. |
| `general.incubatorCapacity` | `100000` RF | Internal energy capacity. |
| `general.incubatorMaxReceive` | `4000` RF/t | Maximum energy received from adjacent blocks per tick. |

## Mega Chickens

![Mega Chicken](https://i.imgur.com/2lmsG6r.png)

![Mega Chicken with equipment](https://i.imgur.com/iq5UhW9.png)

The entity is named **Mega Chicken** in-game. It is a rare, oversized, rideable utility chicken with taming, saddles, two-chest cargo, breed-based appearances, and recoverable portable storage.

### Finding and taming one

- **Rare natural spawn:** Mega Chickens are added to biomes that already spawn vanilla chickens. A normal animal spawn must pass, followed by a `1-in-128` Mega Chicken roll.
- **Spawn Egg:** The **Mega Chicken Spawn Egg** is available for controlled spawning and testing.
- **No breeding:** Mega Chickens cannot breed and are not part of the normal resource-chicken breeding tree.
- **Food-based taming:** Feed a wild Mega Chicken items in the `#minecraft:chicken_food` tag. The required amount is randomized between `16` and `64` items and is influenced by the individual chicken's health, movement speed, and jump strength. The remaining amount is shown to the player while taming.

### Riding and cargo

- A tamed Mega Chicken can be ridden with a normal saddle.
- It accepts up to two vanilla chests, with `27` cargo slots per chest and `54` total cargo slots.
- The inventory screen is a horse-style equipment and cargo screen.
- A chest cannot be removed while that chest's cargo is still occupied. Empty the relevant cargo section first.
- The owner can open the inventory and manage the saddle, chests, and cargo.

### Breed-based appearances

Mega Chickens can be skinned using a captured breed:

1. Capture a non-rooster chicken into a **Chicken Item**. The item must have exactly `10/10/10` Growth, Gain, and Strength.
2. The Mega Chicken's owner right-clicks the tamed Mega Chicken with that item.
3. The Chicken Item is consumed and the Mega Chicken stores that breed as its appearance.
4. If `textures/entity/megachicken/<breed>.png` exists, that texture is used. If it does not, the normal tamed Mega Chicken texture is used.

![Mega Chicken with a saddle and chests](https://i.imgur.com/T5yOSsl.png)

![Mega Chicken appearance examples](https://i.imgur.com/6imh12c.png)

### Capture, storage, and revival

- The Chicken Catcher can capture only a tamed, owned Mega Chicken that is not currently being ridden.
- A captured Mega Chicken becomes a stack-size-one **Mega Chicken** item containing its entity data: owner, appearance, health and attributes, saddle, chests, cargo, and other saved data.
- A live captured Mega Chicken item can be placed directly on a block or by using it while looking at a block.
- If a Mega Chicken dies, it creates a protected revival item instead of ordinary death loot. The item preserves the Mega Chicken's data and restores it at full health.
- To activate a death/revival item, hold a Nether Star in the player's offhand and right-click the item. The Nether Star is consumed.
- After activation, right-click a block with the item to restore the Mega Chicken.
- Death/revival item entities are fire-resistant, invulnerable, no-gravity, and have unlimited lifetime, so the recovery item is not destroyed by fire, despawning, or normal item damage.
- Live captured items do not need Nether Star activation; only the death/revival form does.

## Redstone Flux generation

![Redstone Flux Eggs](https://i.imgur.com/WklUeVL.png)

The **Redstone Flux Chicken** is a Tier 3 breed made from Redstone and Glowstone Chickens. It lays **Flux Eggs** that contain RF.

- A base Flux Egg stores `1000 RF`.
- Each Growth, Gain, or Strength point above `1` adds `100 RF` to the egg's stored energy.
- A `10/10/10` chicken produces up to `3700 RF` per egg.
- Gain also controls the normal drop stack scaling, so a `10/10/10` chicken can produce three Flux Eggs per cycle.
- The **Avian Flux Converter** processes one Flux Egg at a time, stores its payload in an internal battery, and exports RF to adjacent machines or batteries.
- The converter has `50000 RF` of storage by default, discards empty shells, and retains stored RF when broken.

Useful RF settings:

- `general.fluxEggCapacityMultiplier` - Scales stored RF in Flux Eggs.
- `general.avianFluxCapacity` - Avian Flux Converter capacity; default `50000 RF`.
- `general.avianFluxMaxReceive` - Maximum RF received per tick; default `4000`.
- `general.avianFluxMaxExtract` - Maximum RF exported per tick; default `4000`.
- `general.avianFluxEffectsEnabled` - Enables or disables the converter's light and particle effects without disabling energy transfer.

## Radioactive chickens and items

Radioactive content is an optional integration with **Mekanism's radiation system**. Modern Chickens does not add a separate radiation mechanic and does not require Mekanism for the core breeding mod.

### Radioactive breeds

The built-in radioactive resource breeds include:

- **Radioactive Waste Chicken** - produces Mekanism radioactive waste eggs.
- **Uranium Chicken**
- **Yellorium Chicken**
- **Blutonium Chicken**
- **Uraninite Chicken**
- **Plutonium Pellet Chicken**
- **Polonium Pellet Chicken**
- **Antimatter Pellet Chicken**

Mekanism chemicals and gases are inspected at runtime. If Mekanism reports a chemical as radioactive, its generated Chemical Egg or Gas Egg and matching chicken are treated as radioactive automatically. This also covers radioactive chemical content supplied by compatible integrations without a hardcoded list in this page or a manual config edit.

### What is radioactive?

The radiation bridge recognizes radioactive:

- Liquid, chemical, and gas eggs containing radioactive content.
- Spawn eggs and captured Chicken Items for radioactive breeds.
- Resource items produced by radioactive breeds.
- Radioactive fluids or chemicals stored in the Avian Fluid Converter, Avian Chemical Converter, or Avian Dousing Machine.

### Safety behavior

- **Living resource chickens are localized.** Radioactive resource-chicken entities show Mekanism radiation particles and are protected from Mekanism radiation damage. They do not continuously create an atmospheric radiation source simply by standing nearby.
- **Dropped radioactive items emit a small source.** Dropped radioactive eggs, Chicken Items, spawn eggs, and radioactive resource outputs create a low-level Mekanism radiation source and show warning particles.
- **Carried items can irradiate the player.** Radioactive items in the inventory, armor slots, or offhand contribute a small, capped amount of Mekanism radiation.
- **Stored contents can warn and radiate.** Supported Modern Chickens machines and loaded inventories containing radioactive chicken content emit a small source and warning particles while the content remains inside. Removing the content stops the warning.
- **Breaking a loaded Dousing Machine matters.** Radioactive chemical contents are spilled through Mekanism's radiation API when the machine is removed, so dismantle an active radioactive setup carefully.

Radioactive liquid eggs display a hazard tooltip and can apply Wither briefly when placed by a non-creative player while `general.liquidEggHazardsEnabled` is enabled. Chemical and gas eggs display their radioactive hazard in their tooltip. Disabling that setting removes the liquid-egg hazard tooltip and placement effects; it does not disable Mekanism radiation emitted by radioactive items or stored contents.

## Custom chicken creation

After the first run, Modern Chickens generates `config/chickens_custom.json`. Add entries to its top-level `chickens` array to define bespoke chickens without recompiling the mod. The generated starter file includes an example.

Each entry can define:

- `name` - Unique registry name.
- `id` - Optional positive numeric id; omitted values use the next free id.
- `texture` - In-world texture resource location.
- `item_texture` - Optional Chicken Item and JEI sprite resource location.
- `lay_item` and `drop_item` - Resource item ids and optional counts.
- `background_color` and `foreground_color` - Hex or decimal colours for generated/tinted textures.
- `parents` - Up to two existing chicken names.
- `spawn_type` - `normal`, `snow`, `end`, `hell`, or `none`.
- `lay_coefficient` - Lay-time multiplier; defaults to `1.0`.
- `display_name` - Optional in-game display name.
- `generated_texture` - Whether the configured texture should be colour-tinted.
- `enabled` - Whether the chicken participates in registries and breeding; defaults to `true`.
- `allowNaturalSpawn` - `chickens.cfg` per-chicken setting that can allow a parent-based chicken into natural spawn tables.

Missing fields use the mod defaults. Resource locations are normalized to lowercase; omitting a texture falls back to the bone-chicken sprite. Custom chickens also participate in the existing `chickens.cfg` flow, so they can be enabled, disabled, reparented, or retuned alongside built-in breeds.

## Configuration

The server configuration is generated at `config/chickens.cfg`. Stop the game before editing it, then restart the client/server to apply changes.

Common settings include:

- `general.enableFluidChickens`
- `general.enableChemicalChickens`
- `general.enableGasChickens`
- `general.disableEggLaying`
- `general.alwaysShowStats`
- `general.roostSpeed`
- `general.breederSpeed`
- `general.scalingDrops`
- `general.collectorScanRange`
- `general.liquidEggHazardsEnabled`
- `general.avianFluidConverterCapacity`
- `general.avianFluidConverterTransferRate`
- `general.avianChemicalConverterCapacity`
- `general.avianChemicalConverterTransferRate`
- `general.fluxEggCapacityMultiplier`
- `general.avianFluxCapacity`
- `general.incubatorEnergyCost`
- `general.incubatorCapacity`
- `general.incubatorMaxReceive`

The generated file is the source of truth for the available values and comments in your installed version. Existing legacy `chickens.properties` files are read once for migration and are no longer written by current versions.

## Links

- [Modern Chickens on GitHub](https://github.com/STRHercules/ModernChickens)
- [Chickens](https://www.curseforge.com/minecraft/mc-mods/chickens)
- [Roost](https://www.curseforge.com/minecraft/mc-mods/roost)

## Credits

- [setycz](https://www.curseforge.com/members/setycz/projects) - original [Chickens](https://www.curseforge.com/minecraft/mc-mods/chickens)
- [Timrwood](https://www.curseforge.com/members/timrwood/projects) - original [Roost](https://www.curseforge.com/minecraft/mc-mods/roost)
