package com.setycz.chickens.config;

/**
 * Thread-safe container for the currently active configuration snapshot.
 * NeoForge loads configuration data on the mod loading thread, so a simple
 * volatile reference is more than enough to safely publish values to any
 * gameplay systems that might query them later on.
 */
public final class ChickensConfigHolder {
    private static volatile ChickensConfigValues values = new ChickensConfigValues(10, 3, 5, 1.0f, 0.02f, 0.05f, 0.015f, false, 1.0D, 1.0D, false, 4, true,
            1.0D, 50_000, 4_000, 4_000, 8_000, 2_000, true, 8_000, 2_000, true, true, true, true, true, 10_000, 100_000, 4_000);

    private ChickensConfigHolder() {
    }

    public static ChickensConfigValues get() {
        return values;
    }

    public static void set(ChickensConfigValues newValues) {
        values = newValues;
    }
}
