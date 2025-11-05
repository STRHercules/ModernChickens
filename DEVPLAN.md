## Development Plan

1. **Expand the liquid egg infrastructure**
   - Refactor `LiquidEggRegistryItem` to capture richer metadata (fluid volume, optional hazard flags, suppliers for block state/fluids) instead of the current fixed block-and-fluid tuple so the registry can describe tech fluids and special handling requirements.


   - Extend `ChickensDataLoader.registerLiquidEggs` to build a data-driven table of fluids (water, lava, fuel oils, XP, biofuel, radioactive waste, etc.), probing the fluid registry and mod presence before registering each entry so packs without a given mod skip its eggs.


   - Update `LiquidEggItem` to consume the richer metadata: delegate placement to NeoForge fluid helpers (so fluids without dedicated blocks still work), emit context-sensitive tooltips, and apply custom behaviour such as leaving behind a spent shell or triggering safety effects for hazardous eggs.


   - Keep automation compatibility by adapting `LiquidEggFluidWrapper` to the new metadata while maintaining its single-drain bucket semantics for existing pipes and tanks.



2. **Define new fluid eggs and chickens**
   - Introduce base-game fluid chickens (e.g., water, lava, XP) by wiring new registry items into the default data set alongside the existing water/lava birds, ensuring they lay the correct fluid eggs via `LiquidEggItem.createFor`.


   - Add conditional definitions for Immersive Engineering, BuildCraft, Mekanism, and other tech mods using the existing `ModdedChickens` pattern so each fluid chicken only spawns when its fluid item exists.


   - Ensure the creative tab, JEI, and other item listings automatically surface the new eggs by iterating the expanded registry instead of hard-coded IDs.



3. **Introduce the Avian Fluid Converter machine**
   - Implement an `AvianFluidConverterBlockEntity` mirroring the RF converter’s architecture—single-slot inventory, server tick draining, comparator output—but backed by a `FluidTank` and `Capabilities.FluidHandler` to output stored fluids to adjacent inventories.



   - Register the new block, block item, menu, screen, and block entity alongside existing content in `ModRegistry`, `ModBlockEntities`, and `ModMenuTypes`, and hook the screen into the client setup and colour handlers where appropriate.




   - Provide crafting recipes, loot tables, and advancements for the converter, using the energy converter’s assets as a template.

4. **Gameplay hooks and safety mechanics**
   - Add configuration entries for fluid converter tank size, transfer rate, and safety toggles by extending `ChickensConfigValues` and the general settings loader so pack makers can balance throughput alongside the existing RF settings.



   - Teach `LiquidEggItem` (and possibly entity interactions) to apply debuffs or damage when handling hazardous eggs, gated by the new config flags.

5. **Integration and automation support**
   - Extend the WTHIT/Jade plugins with a fluid converter provider that mirrors the RF tooltip logic, exposing tank volume and capacity to overlays.


   - Add a JEI recipe type and category for fluid conversion so players can preview which eggs yield which fluids and how the converter operates, reusing the existing JEI integration scaffolding.



   - Update automation-facing APIs (capability registration, item handlers) to include the new block so pipes can insert eggs and extract fluids without extra adapters.



6. **Assets, localisation, and documentation**
   - Create textures/models for each fluid egg variant, the Avian Fluid Converter block, GUI, and JEI backgrounds; add translation keys for tooltips, warnings, and converter UI strings.
   - Document new mechanics in `README`/in-game guidebooks and refresh any existing diagrams or breeding charts affected by the new chickens.

7. **Validation**
   - Manually verify fluid placement, converter draining, automation compatibility, JEI/WTHIT overlays, and config toggles across combinations of supported tech mods.
   - Run the existing build/test suite once assets and gameplay code compile cleanly, ensuring no regressions in legacy liquid egg behaviour.

## Testing
⚠️ Tests not run (read-only QA scope).
