package com.setycz.chickens.config;

/**
 * Immutable snapshot of the global configuration options that used to live
 * in the legacy configuration file. Only a tiny subset of the mod reads
 * these values right now, but keeping the structure mirrors the original
 * behaviour and lets future ports reference the numbers without touching
 * disk parsing logic again.
 */
public final class ChickensConfigValues {
    private final int spawnProbability;
    private final int minBroodSize;
    private final int maxBroodSize;
    private final float netherSpawnChanceMultiplier;
    private final boolean alwaysShowStats;
    private final double roostSpeedMultiplier;
    private final double breederSpeedMultiplier;
    private final boolean disableVanillaEggLaying;
    private final int collectorScanRange;

    public ChickensConfigValues(int spawnProbability, int minBroodSize, int maxBroodSize,
            float netherSpawnChanceMultiplier, boolean alwaysShowStats,
            double roostSpeedMultiplier, double breederSpeedMultiplier,
            boolean disableVanillaEggLaying, int collectorScanRange) {
        this.spawnProbability = spawnProbability;
        this.minBroodSize = minBroodSize;
        this.maxBroodSize = maxBroodSize;
        this.netherSpawnChanceMultiplier = netherSpawnChanceMultiplier;
        this.alwaysShowStats = alwaysShowStats;
        this.roostSpeedMultiplier = roostSpeedMultiplier;
        this.breederSpeedMultiplier = breederSpeedMultiplier;
        this.disableVanillaEggLaying = disableVanillaEggLaying;
        this.collectorScanRange = collectorScanRange;
    }

    public int getSpawnProbability() {
        return spawnProbability;
    }

    public int getMinBroodSize() {
        return minBroodSize;
    }

    public int getMaxBroodSize() {
        return maxBroodSize;
    }

    public float getNetherSpawnChanceMultiplier() {
        return netherSpawnChanceMultiplier;
    }

    public boolean isAlwaysShowStats() {
        return alwaysShowStats;
    }

    public double getRoostSpeedMultiplier() {
        return roostSpeedMultiplier;
    }

    public double getBreederSpeedMultiplier() {
        return breederSpeedMultiplier;
    }

    public boolean isVanillaEggLayingDisabled() {
        return disableVanillaEggLaying;
    }

    public int getCollectorScanRange() {
        return collectorScanRange;
    }
}
