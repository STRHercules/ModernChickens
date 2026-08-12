**CHANGELOG**
# [2.4.0] - 11/8/2026

## Added

- **KubeJS integration** (optional; tested with `kubejs-neoforge-2101.7.2-build.368` / `rhino-2101.2.8-build.91`). The plugin ships inside the mod jar and only loads when KubeJS is installed, so nothing changes for packs without it.
  - **`ChickensEvents.registry`** — startup event for defining chickens from scripts:

    ```js
    ChickensEvents.registry(event => {
        event.create('dirt_chicken')
            .displayName('Dirt Chicken')
            .layItem('minecraft:dirt')
            .dropItem('minecraft:dirt', 2)
            .spawnType('NORMAL')
            .primaryColor(0x8B4513)
            .secondaryColor(0x654321)
    })
    ```

    Supports `displayName`, `layItem`, `dropItem`, `parent1`/`parent2`/`parents`, `tier`, `spawnType`, `allowNaturalSpawn`, `primaryColor`, `secondaryColor`, `layCoefficient`, `generatedTexture`, `texturePath`, `itemTexture`, `allowDousing`, `liquidDousingCost`, `enabled` and `id`. Registry ids are derived from the chicken name (6,000,000+ span) so they stay stable across script edits and load order changes.
  - **Recipe schema for `chickens:avian_dousing`** — `event.recipes.chickens.avian_dousing(result, input, reagent).energy(rf)`. The raw `event.custom({ type: 'chickens:avian_dousing', ... })` form keeps working and produces identical JSON.
  - Full reference and runnable examples in `wiki.md` and `Examples/KubeJS/`.
- **Avian Dousing recipes now accept item and block IDs** in `input` and `result`, not just chicken names, so the machine can convert items as well as chickens. Fields resolve as a chicken name first and an item ID second, which keeps every existing recipe working unchanged.

  ```js
  event.recipes.chickens.avian_dousing('minecraft:grass_block', 'minecraft:dirt',
    { type: 'item', id: 'minecraft:bone_meal' }).energy(2000)
  ```

## Fixed

- **Data-driven Avian Dousing recipes were invisible in JEI.** The category was built only from the chicken registry plus two hardcoded entries, so recipes added by datapacks or KubeJS never showed up. It now reads them from the recipe manager.
- **The Avian Dousing input slot rejected anything that was not a chicken item**, which blocked recipes that take an item as input even when the recipe itself was valid.

## Changed

- Chicken tiers can now be pinned explicitly instead of always being derived from the parent chain (`max(parentTier) + 1` remains the default).
- The built-in Dragon and Wither dousing entries in JEI are now generated from their recipe files instead of being hardcoded; costs are unchanged (10 items, 10,000 RF).
- The default dousing energy cost is exposed as `DousingRecipe.DEFAULT_ENERGY` so the recipe codec and the KubeJS schema cannot drift apart.

# [2.0.7] - 12/4/2025 - 18:30 (ARG)

## Added

- **Andesite Alloy Chicken** (`andesiteAlloyChicken`)
- **Chocolate Chicken** (`chocolatchicken`)
- **Ether Gas Chicken** (`ethergasChicken`)
- **Crystal Matrix Chicken** (`crystalmatrixChicken`)

## Changed

- Extended **Almost Unified** support to the drop of the following ingots: copper, tin, zinc, lead, nickel, silver, platinum, invar, bronze, steel, cupronickel, electrum, aluminum/aluminium, osmium, uranium, constantan, yellorium, graphite, cyanite, blutonium, electrical steel, energetic alloy, vibrant alloy, redstone alloy, conductive iron, pulsating iron, dark steel, soularium, signalum, enderium, iridium, lumium, mithril, entro, quantum alloy, black iron, draconium, awakened draconium, manasteel, terrasteel, elementium.