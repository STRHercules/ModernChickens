# Modern Chickens — KubeJS examples

Verified against **KubeJS 2001.6.5** and **Rhino 2001.2.3** on Minecraft 1.20.1 /
Forge. KubeJS needs Architectury API to load.

Copy the scripts into your instance:

| File | Goes in |
| --- | --- |
| [`startup_scripts/modern_chickens_registry.js`](startup_scripts/modern_chickens_registry.js) | `kubejs/startup_scripts/` |
| [`server_scripts/modern_chickens_dousing.js`](server_scripts/modern_chickens_dousing.js) | `kubejs/server_scripts/` |

## What each one covers

**`modern_chickens_registry.js`** runs on `ChickensEvents.registry`, which fires
once during mod setup. It creates a chicken and shows the override entry points:
`teach`, `modify`, `fluid`, `chemical`, `modifyEgg`, plus `exists` and
`getNames` for guarding a script. A game restart is required after editing it —
`/kubejs reload_startup_scripts` does not re-fire the event.

**`modern_chickens_dousing.js`** adds `chickens:avian_dousing` recipes. The
schema takes `(result, input, reagent)` with an optional fourth `energy`
argument, so both of these are valid:

```js
event.recipes.chickens.avian_dousing('GoldChicken', 'IronChicken',
  { type: 'item', id: 'minecraft:gold_ingot', amount: 4 }).energy(10000)

event.recipes.chickens.avian_dousing('LavaChicken', 'MagmaChicken',
  { type: 'fluid', id: 'minecraft:lava', amount: 1000 }, 20000)
```

Reagent `type` is `item`, `fluid` or `chemical`; `amount` defaults to `1` and
`energy` to `10000`. Recipes reload with `/reload`.

## Gotchas

- Chicken names are registry names (`IronChicken`, `obsidianChicken`, …) matched
  case-insensitively, **not** item ids.
- A chicken only accepts dousing after `allowDousing(true)`.
- `fluid()` and `chemical()` retune the chicken that *lays* that egg, so the
  fluid or chemical must already have a chicken. Parents cannot be descendants
  of the chicken being retuned, or the breeding data is cleared with a
  "has unusable parents" warning.

The full reference — every builder and override method, load order, nest seed
tag, reagent types and troubleshooting — is in [`wiki.md`](../../wiki.md).
