package com.setycz.chickens.config;

/**
 * Thread-safe container for the currently active configuration snapshot.
 * NeoForge loads configuration data on the mod loading thread, so a simple
 * volatile reference is more than enough to safely publish values to any
 * gameplay systems that might query them later on.
 */
public final class ChickensConfigHolder {
    // Default values mirror the legacy configuration while introducing sensible
    // baselines for the new rooster nest options.
    private static volatile ChickensConfigValues values = new ChickensConfigValues(
            10, 3, 5,                // spawnProbability, minBroodSize, maxBroodSize
            1.0f, 0.02f, 0.05f, 0.015f, // netherSpawnChanceMultiplier, overworld, nether, end
            false,                   // alwaysShowStats
            1.0D,                    // roostSpeedMultiplier
            1.0D,                    // breederSpeedMultiplier
            1.25D,                   // roosterAuraMultiplier
            4,                       // roosterAuraRange
            1,                       // nestMaxRoosters (single rooster by default)
            20 * 60,                 // nestSeedDurationTicks (60 seconds per seed)
            false,                   // disableVanillaEggLaying
            4,                       // collectorScanRange
            true,
            1.0D, 50_000, 4_000, 4_000, 8_000, 2_000, true, 8_000, 2_000, true, true, true, true, true);

    private ChickensConfigHolder() {
    }

    public static ChickensConfigValues get() {
        return values;
    }

    public static void set(ChickensConfigValues newValues) {
        values = newValues;
    }
}
