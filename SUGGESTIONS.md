# Suggestions

- Flesh out the placeholder registry scaffolding by porting entity, block, GUI, and integration classes from the legacy project using the new NeoForge data-driven patterns.
- Audit any remaining runtime recipe and loot registrations and migrate them to JSON data packs compatible with 1.21.1.
- Replace the temporary debug item with the real Chickens content and add automated tests (e.g., data generators or unit tests) to guard the future porting work.
- Restore the bespoke behaviours for the analyzer, spawn egg, colored egg, and liquid egg items so the placeholders regain their legacy functionality.
- Once the custom chicken entity is settled, wire up dedicated spawn placement registration and biome modifiers so natural spawning matches the original mod instead of relying solely on spawn eggs.
- Reinstate the fluid capability wrapper for liquid eggs and port the analyzer GUI/henhouse inventories so item interactions can mirror the original mod’s storage and automation features.
- Consider exposing spawn weight overrides per chicken so datapacks or configs can fine-tune how frequently specific breeds appear in different biome categories.
- Rebuild the Jade integration hooks so in-world overlays recognise the analyzer and henhouse now that the JEI plugin has been modernised.
- Add simple data generators for henhouse models, loot, and recipes so future variants stay consistent without hand-maintaining dozens of JSON files.
- Add a regression test or data audit that verifies liquid egg chickens produce the correct typed items after configuration reloads, preventing future regressions when extending the registry.
- Mirror the legacy creative tab ordering by adding a comparator that sorts chickens by tier/id before populating display stacks, keeping the new tab consistent with nostalgic expectations.
- Replace the reflective Jade bridge with a direct API dependency once a stable Maven coordinate is available so the overlay keeps working if the compatibility shim is ever removed.
- Allow configuring the breeding graph export path or filename so dedicated servers can segregate outputs per world without manual cleanup.
- Add a simple asset lint or data-driven test that loads each spawn egg model to catch future texture regressions before they make it into a release build.
- Introduce a gameplay regression test (or QA checklist) that spawns and throws each coloured egg in a multiplayer environment to ensure entity data sync stays within vanilla bounds after future refactors.
