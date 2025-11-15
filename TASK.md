# TASK: Better Mod Integration – Specialty Resource Chickens

## High Level Goal

Expand **ModernChickens** with new chickens for high value resources from popular mods that are **not** already covered by the normal categories (ingot, liquid, chemical).

Use the code and data under `ResourceInformation\` (and all subdirectories) as **read only** reference material in order to:

1. Discover correct item ids, block ids, tags and recipes for each target resource.
2. Design and implement new chickens that produce these resources.
3. Ensure each chicken is gated behind the presence of its parent mod and fits the existing balance and progression of ModernChickens.

This task is meant to be given to Codex to guide analysis and code generation.

---

## Constraints and Safety Rules

* **`ResourceInformation\` and all subdirectories are READ ONLY.**
  * Do not modify, delete or reformat anything under `ResourceInformation\`.
  * Treat `ResourceInformation\` as documentation and reference code only.

* All actual implementation work must happen only in the writable ModernChickens code and data areas, as defined in `AGENTS.md` and the repository directory policy.

* New chickens **must not** introduce hard mod dependencies.
  * Each integration must only activate when the corresponding mod is present.
  * Use existing ModernChickens patterns for conditional registration and integration.

---

## Scope

Create design and implementation plans for **specialty resource chickens** for the following mods and resources:

### Vanilla

- Amethyst

### EvilCraft

- Blood

### Mekanism

- Fluorite

### Just Dire Things

- Celestigem  
- Time Crystal

### Industrial Foregoing

- Plastic  
- Rubber

### Draconic Evolution

- Chaos Shards

### Mystical Agriculture

Create chickens that produce **essence tiers**, not just ores or raw materials:

- Inferium  
- Prudentium  
- Tertium  
- Imperium  
- Supremium  
- Insanium

### Powah

- Uraninite

### Applied Energistics (AE2)

- Certus Quartz (normal)  
- Charged Certus Quartz  
- Silicon  
- Fluix  
- Sky Stone

### Extended AE

- Entro

### Advanced AE

- Quantum Alloy

### Flux Networks

- Flux

### Applied Fluix

- Applied Fluix (confirm how this differs from standard Fluix in AE2)

### Applied Generators

- Ember Crystal

---

## Phase 1: Analyze `ResourceInformation\` and Build a Resource Map

For each mod group listed in the Scope:

1. **Locate relevant reference data**
   * Search within `ResourceInformation\` for:
     * The mod id and main package or namespace.
     * Item and block definitions (Java, JSON, data packs).
     * Tags, recipes, loot tables, and any integration hooks for each resource.

2. **For every target resource, extract:**
   * Mod id (for example `mekanism`, `powah`, `ae2`, etc).
   * Exact registry name(s), such as:
     * Item id: `modid:item_name`
     * Block id: `modid:block_name`
   * Primary type: item or block, plus any fluid or energy behavior if applicable.
   * Any important tags (ore tags, material tags, processing tags, etc).
   * Any obvious processing chain, if visible:
     * Example: ore → dust → ingot, or essence → resource.

3. **Produce an internal resource map** (even if only kept in comments) with entries like:

   ```text
   Mod: Powah
   Resource: Uraninite
   Type: Item
   ID: powah:uraninite
   Notes: Used as a fuel and central resource in Powah generators.
   ```

This map will guide the design of chickens and ensure correct ids are used during registration.

---

## Phase 2: Design Specialty Resource Chickens

For each resource in the Scope, design a corresponding chicken that fits ModernChickens progression and balance.

For every new chicken, define:

1. **Chicken identity**
   * Internal id (snake_case), for example:
     * `amethyst_chicken`
     * `evilcraft_blood_chicken`
     * `powah_uraninite_chicken`
     * `ae2_fluix_chicken`
   * Display name (user facing), such as:
     * `Amethyst Chicken`
     * `Blood Chicken`
     * `Uraninite Chicken`

2. **Category and tier**
   * Decide the approximate tier or difficulty:
     * Early, mid, late, or end game.
   * Align with existing ModernChickens tier logic and config settings where possible.
   * Higher tier resources such as **Chaos Shards**, **Supremium**, **Insanium**, **Quantum Alloy**, and **Flux** should be clearly end game.

3. **Acquisition method**
   * Decide whether each chicken:
     * Can be found in world generation (rare spawn).
     * Is obtained only through breeding of lower tier chickens.
     * Requires crafting or a special item to unlock.
   * Define **breeding parents** for each chicken where sensible, using thematic or progression based pairings.
     * Example: Mystical Agriculture essences may breed from lower tier essences or elemental chickens.
     * AE related chickens could breed from quartz, redstone, and nether themed chickens.

4. **Egg behavior and output**
   * Decide what each egg actually produces and how:
     * Directly lays the target item.
     * Lays an egg item that can be processed (smelted, crushed, dissolved) into the target resource.
   * Specify:
     * Output type: item, block, fluid, or custom.
     * Conversion rules:
       * For example: Blood Chicken eggs can be drained into EvilCraft blood fluid.
       * Mystical Agriculture essence chickens lay essence directly.
       * AE chickens lay the exact AE item (Fluix, Certus Quartz, Charged Certus, Sky Stone, etc).
   * Consider stacking and throughput:
     * Lay rate, amount per egg and any special interactions.
     * Late game resources can have lower lay rates to preserve balance.

5. **Mod presence and integration gates**
   * Each chicken must only register and function if the corresponding mod is loaded.
   * Reuse or extend existing ModernChickens mechanisms for:
     * Conditional registration by mod id.
     * Hiding unavailable chickens from JEI and other UI integrations.

Document these design decisions in comments or a short internal design section near the new registry entries.

---

## Phase 3: Implementation Plan

Using the resource map and designs:

1. **Register new chickens**
   * Add chicken definitions using the existing ModernChickens registration framework.
   * For each chicken, specify:
     * Internal id and display name.
     * Tier or progression values.
     * Lay rates and stats consistent with existing configuration logic.
     * Parent breeding combinations.
     * Any special flags (for example: fluid like behavior, special machine interactions).

2. **Hook up outputs**
   * Wire each chicken to the correct output resource:
     * When a chicken lays an egg, ensure it yields or can be processed into the correct mod item or block.
   * For fluid or energy related resources:
     * Integrate with existing ModernChickens converters where possible (for example, Avian Fluid or Flux converters).
     * If necessary, define new converters or handlers, but only within ModernChickens code.

3. **Conditional integration**
   * Ensure all registrations and recipe additions are wrapped in checks that only enable them when the target mod is present.
   * Use the same patterns that ModernChickens already uses for other integrations.

4. **Configuration support**
   * Add configuration entries if needed to:
     * Enable or disable specific specialty chickens.
     * Adjust their lay rates or breeding difficulty.
   * Keep naming and grouping consistent with existing config structure.

5. **Assets and visuals**
   * Add or reference textures for new chickens and eggs, matching the style of existing ModernChickens assets.
   * If placeholders are needed, keep them clearly identifiable so they can be upgraded later.

---

## Phase 4: Validation and Testing

For each mod integration and chicken:

1. **Build and run**
   * Confirm the project builds successfully after the new chickens are added.
   * Launch a dev instance with:
     * ModernChickens.
     * The relevant integration mods from the Scope.

2. **Presence checks**
   * With the mod loaded:
     * Confirm the new chickens are visible in JEI or other relevant UIs.
     * Confirm their eggs produce the intended resource.
   * With the mod **not** loaded:
     * Confirm the corresponding chickens do not register.
     * Confirm there are no missing item or class errors in logs.

3. **Progression sanity check**
   * Verify that:
     * Early and mid game resources are appropriately accessible.
     * High tier resources such as **Chaos Shards**, **Supremium**, **Insanium**, **Quantum Alloy**, **Flux**, and **Uraninite** feel like late or end game rewards.

4. **Config validation**
   * Test toggling any new config options to:
     * Disable one or more specialty chickens.
     * Adjust their rates.
   * Ensure no broken references or crashes occur when certain chickens are disabled.

5. **Log review**
   * Check logs for:
     * Missing texture or model warnings for new chickens and eggs.
     * Integration or registry errors related to conditional mod loading.

---

## Completion Criteria

This task is complete when:

1. All resources listed in the Scope have:
   * A documented chicken design.
   * An implemented chicken in code or data.
   * Correct, validated item or block outputs.

2. New chickens only exist and function when their respective mods are present.

3. Builds succeed without errors, and in game:
   * No missing textures or registry entries are reported for new chickens.
   * Specialty resource chickens feel balanced relative to existing ModernChickens content.

4. All changes respect the **read only** rule for `ResourceInformation\` and follow the repository directory policy described in `AGENTS.md`.
