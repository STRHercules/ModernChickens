# Modern Chickens — KubeJS wiki

Everything Modern Chickens exposes to KubeJS, with runnable examples.

Tested against `kubejs-neoforge-2101.7.2-build.368` and `rhino-2101.2.8-build.91`
(Minecraft 1.21.1 / NeoForge). Nothing extra to install: the KubeJS plugin ships
inside the Modern Chickens jar and only loads when KubeJS is present.

| What | Entry point | Script folder |
| --- | --- | --- |
| [Registering chickens](#1-registering-chickens) | `ChickensEvents.registry` | `kubejs/startup_scripts/` |
| [Avian Dousing recipes](#2-avian-dousing-recipes) | `event.recipes.chickens.avian_dousing` | `kubejs/server_scripts/` |

---

## 1. Registering chickens

```js
ChickensEvents.registry(event => {
    event.create('dirt_chicken')
        .displayName('Dirt Chicken')
        .layItem('minecraft:dirt')
        .dropItem('minecraft:dirt', 2)
        .spawnType('NORMAL')
        .primaryColor(0x8B4513)
        .secondaryColor(0x654321)
        .layCoefficient(1.0)
})
```

### When the event runs

The event is posted once during mod setup (`FMLCommonSetupEvent`), after every
item in the game exists and after `custom_chickens.toml` has been read. That is
much later than the startup scripts themselves, which is why item IDs from other
mods resolve correctly.

- **`/kubejs reload_startup_scripts` does not re-run it.** Restart the game after
  editing the file.
- Declaration order inside the event does not matter — parents are resolved after
  every chicken has been created.

### Load order

1. built-in chickens
2. `config/custom_chickens.toml`
3. **this event**
4. the per-chicken config pass
5. automatic fluid/chemical/gas chickens

So scripts can use built-in *and* TOML chickens as parents, and the config pass
still applies to script chickens — a partial table in `custom_chickens.toml` acts
as an override:

```toml
[chickens.dirt_chicken]
layCoefficient = 2.0
allowDousing = true
```

A *complete* table (one that includes `lay_item`) with the same name as a script
chicken wins outright and the script definition is skipped with a warning. Keep
TOML tables for script chickens to overrides only.

### `event.create(id)`

Creates a builder. The `id` is the entity name used by the breeding system and by
`parent1`/`parent2`. A namespace is accepted and stripped, so `'dirt_chicken'`
and `'chickens:dirt_chicken'` are the same chicken. Names are compared
case-insensitively; a name that already exists (built-in, TOML or another script)
is logged and skipped rather than replacing the original.

`event.exists(name)` and `event.getNames()` are available if a pack needs to
branch on what is already registered.

### Builder reference

#### `.layItem(item)` / `.layItem(item, count)` — required

What the chicken produces when it lays. Accepts any KubeJS item notation:

```js
.layItem('minecraft:diamond')
.layItem('minecraft:iron_ingot', 3)
.layItem('4x minecraft:gold_nugget')
```

A chicken without a lay item is rejected with a warning in the log.

#### `.dropItem(item)` / `.dropItem(item, count)`

Dropped when the chicken dies. Defaults to the lay item.

#### `.displayName(name)`

Name shown in game. When omitted it is derived from the id (`dirt_chicken` →
`Dirt Chicken`, `DirtChicken` → `Dirt Chicken`). Formatting codes with `§` work.

#### `.parent1(name)` / `.parent2(name)` / `.parents(a, b)`

Breeding parents, referenced by chicken name. Both are needed for the chicken to
be breedable; setting only one clears the breeding data with a warning. Using the
same parent twice is fine. Parents may be defined later in the same event, in
another script, in `custom_chickens.toml`, or by the mod itself.

#### `.tier(level)`

Optional. By default the tier is derived the vanilla way —
`max(parent1.tier, parent2.tier) + 1`, or `1` with no parents — and that is
usually what you want. Set it explicitly only to make a chicken rarer or more
common than its lineage implies.

Tier drives three things: breeding probability (higher tier = rarer offspring),
lay time (`6000 × tier × layCoefficient` ticks minimum) and natural spawning
(only tier 1 spawns, unless `.allowNaturalSpawn(true)`).

#### `.spawnType(type)`

Defaults to `'NONE'` (breeding/creative only). Unknown values fall back to
`'NONE'` with a warning.

| Type | Where |
| --- | --- |
| `NORMAL` | Overworld biomes that are not snowy |
| `SNOW` | Biomes tagged `#c:is_snowy` |
| `HELL` | The Nether (also makes the chicken fire-immune) |
| `END` | The End |
| `NONE` | Never spawns naturally |

#### `.allowNaturalSpawn(boolean)`

Lets a chicken above tier 1 spawn naturally. Without it, only tier 1 chickens
with a spawn type other than `NONE` appear in the world.

#### `.primaryColor(color)` / `.secondaryColor(color)`

Body and accent colours for the generated texture. Accepts `0xRRGGBB`,
`'#RRGGBB'` or `'0xRRGGBB'`. Defaults are white (`0xFFFFFF`) and yellow
(`0xFFFF00`). Out-of-range values fall back to the default with a warning.

#### `.layCoefficient(value)`

Multiplier applied to the **wait between eggs**, not to the rate:
`minLayTime = 6000 × tier × layCoefficient` ticks. So **lower is faster**.

```js
.layCoefficient(0.5)   // lays twice as often
.layCoefficient(3.0)   // lays a third as often
```

#### `.generatedTexture(boolean)` / `.texturePath(path)`

By default the chicken recolours the base sprite using the two colours. To ship
your own texture, put the PNG in `kubejs/assets/<namespace>/textures/entity/` and
point at it:

```js
.generatedTexture(false)
.texturePath('kubejs:textures/entity/my_chicken.png')
```

Textures are 64×32 and follow the vanilla chicken UV layout. Setting
`.texturePath(...)` without `.generatedTexture(...)` disables generation
automatically.

#### `.itemTexture(path)`

Custom inventory sprite for the chicken item. Setting it also disables the legacy
colour tint so the sprite renders exactly as authored.

#### `.allowDousing(boolean)` / `.liquidDousingCost(mB)`

Opts the chicken into the *automatic* Avian Dousing path, where the machine
derives the result from the stored fluid or chemical, and sets how much fluid a
cycle consumes (default 10 buckets). Recipes written by hand do **not** need this
— see [section 2](#2-avian-dousing-recipes).

#### `.enabled(boolean)`

Registers the chicken but keeps it disabled — useful for pack-level toggles. A
chicken is also implicitly disabled when any of its parents are.

#### `.id(number)`

Optional numeric registry id. **Leave it alone unless you are migrating an old
world.** Ids are otherwise derived from the chicken name (in the 6,000,000+
range, away from the built-in roster and the dynamic fluid/chemical/gas spans),
which keeps them stable across script edits, load order changes and mod updates.
A requested id that is already taken falls back to the derived one.

### Full example

```js
ChickensEvents.registry(event => {

    event.create('dirt_chicken')
        .displayName('Dirt Chicken')
        .layItem('minecraft:dirt')
        .dropItem('minecraft:dirt', 2)
        .spawnType('NORMAL')
        .primaryColor(0x8B4513)
        .secondaryColor(0x654321)

    event.create('grass_block_chicken')
        .displayName('Grass Block Chicken')
        .layItem('minecraft:grass_block')
        .parents('dirt_chicken', 'dirt_chicken')
        .primaryColor('#7CFC00')
        .secondaryColor('#228B22')
        .layCoefficient(0.8)

    event.create('crafting_table_chicken')
        .displayName('Crafting Table Chicken')
        .layItem('minecraft:crafting_table')
        .dropItem('4x minecraft:oak_planks')
        .parent1('grass_block_chicken')
        .parent2('dirt_chicken')
        .tier(4)
        .primaryColor('#C4A574')
        .secondaryColor('#8B4513')
        .layCoefficient(3.0)

    event.create('terracotta_chicken')
        .displayName('Terracotta Chicken')
        .layItem('minecraft:terracotta')
        .parents('dirt_chicken', 'dirt_chicken')
        .primaryColor('#985E43')
        .secondaryColor('#6D4534')
        .allowDousing(true)
        .liquidDousingCost(4000)
})
```

---

## 2. Avian Dousing recipes

`chickens:avian_dousing` is a normal server recipe type registered in
`ModRecipeTypes` and serialized by `DousingRecipe`. The mod's KubeJS plugin
registers a matching recipe schema, so the builder syntax works with no setup on
the pack side. Recipes reload with `/reload`.

```js
ServerEvents.recipes(event => {
  event.recipes.chickens.avian_dousing(
    'GoldChicken',                                            // result
    'IronChicken',                                            // input
    { type: 'item', id: 'minecraft:gold_ingot', amount: 4 }   // reagent
  ).energy(10000).id('mypack:iron_to_gold_chicken')
})
```

Arguments, in order:

| Argument | Type | Notes |
| --- | --- | --- |
| `result` | chicken name or item ID | Required |
| `input` | chicken name or item ID | Required |
| `reagent` | object | Required. `{ type, id, amount }` |
| `energy` | int | Optional 4th argument, defaults to `10000` RF. Aliases: `.energy(n)`, `.energyCost(n)` |

`reagent.type` is `item`, `fluid` or `chemical` (case-insensitive); `reagent.id`
is a namespaced registry ID; `reagent.amount` is an item count or a volume in mB
and defaults to `1`.

Omitting `.id(...)` is fine — the schema derives one from the result.

### Chicken names vs item IDs

`input` and `result` are resolved as a chicken name first and as an item ID
second, so both work in either position:

```js
event.recipes.chickens.avian_dousing(
  'GoldChicken', 'IronChicken',                                 // chicken -> chicken
  { type: 'item', id: 'minecraft:gold_ingot', amount: 4 }
)

event.recipes.chickens.avian_dousing(
  'minecraft:diamond', 'DiamondChicken',                        // chicken -> item
  { type: 'fluid', id: 'minecraft:lava', amount: 1000 }
).energy(20000)

event.recipes.chickens.avian_dousing(
  'minecraft:grass_block', 'minecraft:dirt',                    // item -> item
  { type: 'item', id: 'minecraft:bone_meal' }
).energy(2000)
```

Chicken names are registry names (`IronChicken`, `obsidianChicken`, …), matched
case-insensitively. There is no ambiguity in practice: chicken names are not
valid resource locations, and no chicken is named like an item ID.

The result is always a single item — the `result` field has no count. Chicken
results come out as spawn eggs.

### Reagent types

```js
{ type: 'item',     id: 'minecraft:gold_ingot',      amount: 4 }     // items consumed
{ type: 'fluid',    id: 'minecraft:lava',            amount: 1000 }  // mB from the tank
{ type: 'chemical', id: 'mekanism:sulfuric_acid',    amount: 500 }   // mB from the buffer
```

Chemical reagents need Mekanism at runtime. The recipe itself loads safely
without it, it just never resolves — guard it if your pack ships both ways:

```js
if (Platform.isLoaded('mekanism')) {
  event.recipes.chickens.avian_dousing(
    'FlintChicken', 'CoalChicken',
    { type: 'chemical', id: 'mekanism:sulfuric_acid', amount: 500 }
  ).energy(15000)
}
```

### Raw JSON

Identical output, and it needs no schema at all — useful in datapacks:

```js
event.custom({
  type: 'chickens:avian_dousing',
  input: 'IronChicken',
  result: 'GoldChicken',
  reagent: { type: 'item', id: 'minecraft:gold_ingot', amount: 4 },
  energy: 10000
}).id('mypack:iron_to_gold_chicken')
```

### Removing recipes

The mod ships two: `chickens:avian_dousing_dragon` (obsidianChicken + 10×
dragon_breath) and `chickens:avian_dousing_wither` (soulSandChicken + 10×
nether_star).

```js
event.remove({ id: 'chickens:avian_dousing_dragon' })
event.remove({ type: 'chickens:avian_dousing' })
```

### Generating a chain

```js
ServerEvents.recipes(event => {
  const chain = [
    ['SandChicken',   'ClayChicken',    'minecraft:clay_ball', 4, 8000],
    ['ClayChicken',   'QuartzChicken',  'minecraft:quartz',    4, 12000],
    ['QuartzChicken', 'DiamondChicken', 'minecraft:diamond',   2, 40000]
  ]

  chain.forEach(([input, result, item, amount, energy]) => {
    event.recipes.chickens.avian_dousing(
      result, input, { type: 'item', id: item, amount: amount }
    ).energy(energy).id(`mypack:dousing/${input.toLowerCase()}_to_${result.toLowerCase()}`)
  })
})
```

---

## Troubleshooting

- **Nothing was registered** — check the log for `ChickensKubeJS`. Every rejected
  chicken is logged with the reason (duplicate name, missing lay item, unknown
  parent).
- **`ChickensEvents is not defined`** — the script is not in `startup_scripts/`,
  or Modern Chickens failed to load.
- **`event.recipes.chickens` is undefined** — the schema is registered by the
  mod's KubeJS plugin during datapack load. Check `logs/kubejs/server.log` for
  plugin errors; `strhercules.chickens.integration.kubejs.ChickensKubeJSPlugin`
  should appear under `Loaded plugins`.
- **Changes to a startup script do nothing** — the registry event only runs at
  game start; `/kubejs reload_startup_scripts` will not re-post it.
- **A dousing recipe never fires** — one of the two names does not exist. Chicken
  names are registry names, not item IDs; item IDs must be registered items.
- **A chicken changed type in an existing world** — this happens if you renamed
  it or pinned `.id(...)` to a different value; ids are tied to the name.

Ready-to-run copies of these scripts live in
[`Examples/KubeJS/`](Examples/KubeJS/).
