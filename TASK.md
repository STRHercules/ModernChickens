# Codex Task: Add Optional Mekanism Radiation Integration for Radioactive Chickens

Implement Mekanism radiation support for the radioactive chicken systems in this project.

The Mekanism source code is available locally at:

`Mekanism/`

Use the actual Mekanism source as the reference for its radiation APIs, radioactive capability/storage behavior, radiation emission, protection checks, particles, and any relevant integration patterns.

## CRITICAL REQUIREMENT: Mekanism Must Remain Optional

**Mekanism is an OPTIONAL dependency and must remain optional.**

The mod must:

* Load normally when Mekanism is not installed.
* Never directly classload Mekanism classes from common/core classes that are loaded without Mekanism present.
* Keep Mekanism-specific code isolated behind proper mod-loaded checks, compatibility classes, optional integration hooks, or another safe NeoForge-compatible mechanism.
* Never require Mekanism for radioactive chickens themselves to exist.
* Never crash during startup, registry loading, data generation, world loading, inventory handling, entity ticking, or rendering when Mekanism is absent.

Use the existing project architecture where possible. Do not introduce Mekanism as a mandatory dependency just to simplify implementation.

---

# Desired Radiation Behavior

## 1. Radioactive Chickens

Radioactive chicken variants should emit a **very small Mekanism radiation field** around themselves.

Requirements:

* Radiation should be localized to approximately **1–2 blocks around the chicken**.
* This should be intentionally mild compared with major Mekanism radioactive leaks.
* The chicken should behave like a small mobile radiation source.
* Radiation emission must not create runaway contamination or continuously spam permanent radiation into chunks.
* Use Mekanism's intended radiation systems/API rather than creating an unrelated custom damage effect.

The implementation should be performant even if multiple radioactive chickens exist in the same area.

Do not perform expensive world/entity scans every tick if Mekanism already provides a better mechanism.

---

# 2. Radioactive Chicken Eggs

Eggs produced by radioactive chickens should also be radioactive.

They need radiation behavior in BOTH cases:

### Dropped in the World

When a radioactive egg exists as an `ItemEntity`:

* It should emit Mekanism radiation around itself.
* The radiation radius/intensity should remain relatively small.
* Multiple eggs should be handled sensibly without creating absurd radiation amplification or excessive ticking overhead.

### Inside a Player Inventory

When a player carries a radioactive chicken egg:

* The player should be exposed to Mekanism radiation from the egg.
* Radiation should behave consistently with Mekanism's existing radiation mechanics.
* Multiple radioactive eggs may increase exposure where appropriate, but apply sensible limits/scaling if required to prevent pathological behavior from large stacks.

This should apply regardless of inventory slot unless there is a strong technical reason to handle specific inventory types differently.

---

# 3. Mekanism Hazmat Protection

**Mekanism radiation protection must work normally against ALL radiation introduced by Modern Chickens.**

A player wearing the appropriate full Mekanism radiation-protective equipment, including Mekanism Hazmat gear, should receive the same protection they would against native Mekanism radiation.

Do NOT implement a separate custom "hazmat immunity" check if using Mekanism's actual radiation system naturally provides this behavior.

Prefer integration with Mekanism's existing radiation calculations so:

* Mekanism Hazmat protection works.
* Other Mekanism-compatible radiation protection continues working.
* Future compatible equipment has the best chance of working automatically.
* Modern Chickens does not duplicate Mekanism's protection logic.

---

# 4. Avian Dousing Machines

Avian Dousing machines should visually indicate radioactive contents.

If an Avian Dousing machine currently contains:

* radioactive chemicals/materials supported by its processing system,
* a radioactive chicken,
* a radioactive chicken egg,

then the machine should emit **radiation-themed particles** around itself.

These particles are primarily a visual warning that the machine currently contains radioactive material.

Requirements:

* Particles should appear intermittently rather than every tick in excessive quantities.
* Keep them readable but subtle enough not to overwhelm the area.
* Prefer Mekanism radiation particles/effects if an appropriate reusable implementation exists.
* If Mekanism does not expose an appropriate particle cleanly, implement a compatibility-safe visual equivalent.
* Particle spawning must remain safe when Mekanism is not installed.

If the machine's radioactive contents are removed, the radiation particles should stop.

---

# 5. Any Machine Containing Radioactive Chicken Eggs

Extend the radioactive visual warning beyond the Avian Dousing machine.

**Any Modern Chickens machine/block entity capable of storing a radioactive chicken egg should emit radiation particles while such an egg is present.**

This should be implemented using shared logic rather than duplicating radioactive-item checks individually across every machine.

For example, prefer something architecturally similar to:

* a shared radioactive-content utility,
* an interface for machines containing radioactive material,
* a centralized Mekanism compatibility handler,
* or another reusable system appropriate to the existing codebase.

Do not scatter hardcoded `instanceof`/item checks through unrelated machine classes unless necessary.

---

# Radioactivity Classification

Create or reuse a centralized method for determining whether a chicken or egg is radioactive.

There should be a **single authoritative radioactive classification system** so that:

* chicken entity radiation,
* dropped egg radiation,
* inventory radiation,
* machine particle effects,
* and future radioactive integrations

all agree on which chicken types/items are radioactive.

Do not independently maintain several lists of radioactive chickens unless the existing architecture absolutely requires it.

If radioactive chicken data already exists in configuration, definitions, recipes, chicken types, tags, or registries, build from that instead.

---

# Mekanism Integration Architecture

Create a clean compatibility layer, for example conceptually:

`compat/mekanism/...`

or whatever matches the project's current organization.

Responsibilities can include:

* Detecting whether Mekanism is loaded.
* Registering Mekanism-specific integration.
* Emitting radiation from chickens.
* Emitting radiation from dropped eggs.
* Applying inventory radiation.
* Querying/using Mekanism's radiation systems.
* Supporting radioactive machine visuals.
* Handling Mekanism-specific particles where appropriate.

Core Modern Chickens code should call its own compatibility abstraction rather than directly referencing Mekanism implementation classes whenever doing so would risk optional-dependency classloading.

Example conceptual structure only:

```text
ModernChickens
    |
    +-- RadioactiveContentHelper
    |
    +-- RadiationCompat
            |
            +-- NoOp / Default implementation
            |
            +-- MekanismRadiationCompat
```

Do not blindly implement this exact structure if the existing project has a better established compatibility pattern.

---

# Performance Requirements

Radiation should NOT be implemented with unnecessarily expensive per-tick logic.

Be particularly careful with:

* every chicken ticking,
* every dropped egg ticking,
* every player inventory being scanned,
* every machine scanning its complete inventory,
* particle networking,
* chunk radiation updates.

Where possible:

* use staggered/intermittent updates,
* cache radioactive-content state,
* only reevaluate inventories when contents change,
* use existing item/entity hooks,
* use Mekanism's established radiation infrastructure,
* avoid repeatedly adding identical radiation every game tick.

A flock of radioactive chickens should not become a server performance disaster.

---

# Configuration

Where appropriate, expose reasonable configuration values rather than burying magic numbers in the implementation.

Potential configurable values include:

* radioactive chicken radiation magnitude,
* radioactive chicken emission interval,
* radioactive egg radiation magnitude,
* radioactive egg inventory exposure strength,
* machine radiation particle frequency.

Keep sensible defaults matching the intended behavior:

**Chickens should produce only a very small 1–2 block radiation hazard.**

Do not expose configuration merely for the sake of adding configuration if the project already has a cleaner balancing system.

---

# Behavior Without Mekanism

When Mekanism is not installed:

* Avian Dousing machines still work.
* All Modern Chickens machines still work.
* No missing-class crashes occur.
* No Mekanism radiation is produced.
* Mekanism-specific particle behavior can safely become a no-op.
* Saves containing radioactive chickens/eggs remain loadable.

---

# Source Inspection

Before implementing this, inspect the Mekanism source in:

`Mekanism/`

Specifically locate the current APIs/implementations for:

* radiation emission,
* radiation sources,
* radiation magnitude/radius behavior,
* environmental radiation,
* player radiation exposure,
* radioactive inventory/item handling,
* Hazmat protection,
* radiation particles,
* capability attachment if applicable,
* radiation decay/cleanup,
* optional-mod integration patterns if useful.

**Do not guess Mekanism API names from memory.**

Use the actual version of Mekanism included in `Mekanism/` as the source of truth.

Also inspect Modern Chickens for:

* radioactive chicken definitions,
* chicken entity ticking,
* egg creation/item classes,
* machine inventories,
* Avian Dousing machine/block entity,
* existing compatibility modules,
* NeoForge event handlers,
* config infrastructure.

---

# Important Implementation Constraint

I specifically want **real Mekanism radiation integration**, not a simulation using:

* custom poison effects,
* generic damage,
* arbitrary potion effects,
* fake radiation counters,
* or Modern Chickens-only Hazmat detection.

If Mekanism is installed, use Mekanism's actual radiation ecosystem wherever technically possible.

This is important because Modern Chickens radiation should naturally participate in the same system as Mekanism radiation.

---

# Acceptance Criteria

The implementation is complete when all of the following work:

1. Modern Chickens loads successfully without Mekanism installed.
2. Modern Chickens loads successfully with Mekanism installed.
3. A radioactive chicken emits a very small localized Mekanism radiation hazard.
4. The hazard is approximately limited to the chicken's immediate 1–2 block area.
5. A radioactive egg dropped into the world emits radiation.
6. Carrying a radioactive egg exposes an unprotected player to radiation.
7. Mekanism radiation-protective/Hazmat equipment protects the player normally.
8. An Avian Dousing machine containing radioactive material produces radiation particles.
9. An Avian Dousing machine containing a radioactive chicken produces radiation particles.
10. An Avian Dousing machine containing a radioactive chicken egg produces radiation particles.
11. Any other applicable Modern Chickens machine containing a radioactive egg produces radiation particles.
12. Removing the radioactive contents stops the machine particle warning.
13. Radiation functionality does not create obvious server tick/performance problems.
14. Mekanism-specific classes cannot crash a non-Mekanism installation through accidental classloading.
15. Radioactive classification is centralized rather than independently hardcoded across every feature.

---

# Deliverables

Implement the feature completely.

After implementation, report:

* Which files were added.
* Which files were modified.
* Which Mekanism APIs/classes were used.
* How optional dependency isolation is handled.
* How chicken radiation is emitted.
* How dropped egg radiation is emitted.
* How inventory radiation is handled.
* How Mekanism Hazmat protection applies.
* How radioactive machine contents are detected.
* How radiation particle effects are triggered.
* Any configuration values added.
* Any important performance safeguards.
* Any assumptions or limitations discovered while inspecting Mekanism's source.

Do not modify Mekanism itself unless absolutely necessary.

Treat `Mekanism/` as reference source code. The integration should live on the Modern Chickens side.
