# Modern Chickens Spawn Plan Template

Drop this folder into `.minecraft/saves/<world>/datapacks/` (or bundle inside a modpack) and rename the `example` namespace to something unique for your pack. Each JSON inside `data/<namespace>/chickens/spawn_plans/` overrides spawn weights and brood sizes for a single biome bucket.

## File layout
```
SpawnPlanTemplate/
├─ pack.mcmeta
└─ data/
   └─ example/
      └─ chickens/
         └─ spawn_plans/
            ├─ overworld_boost.json
            └─ snow_minimal.json
```

## Fields
- `spawn_type`: `normal`, `snow`, or `hell`.
- `spawn_weight`: absolute spawn weight.
- `weight_multiplier`: multiplier for the config weight (cannot use if `spawn_weight` supplied).
- `min_brood_size` / `max_brood_size`: overrides per-spawn flock counts.
- `spawn_charge` / `energy_budget`: optional NeoForge mob-charge hints.

Delete whatever files you do not need; multiple JSON entries can live side-by-side so long as each targets a different `spawn_type`.
