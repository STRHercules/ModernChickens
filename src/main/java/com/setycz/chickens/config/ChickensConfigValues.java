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
    private final boolean avianFluxEffectsEnabled;
    private final double fluxEggCapacityMultiplier;
    private final int avianFluxCapacity;
    private final int avianFluxMaxReceive;
    private final int avianFluxMaxExtract;
    private final int avianFluidCapacity;
    private final int avianFluidTransferRate;
    private final boolean avianFluidEffectsEnabled;
    private final boolean liquidEggHazardsEnabled;
    private final boolean fluidChickensEnabled;
    private final boolean chemicalChickensEnabled;
    private final boolean gasChickensEnabled;

    public ChickensConfigValues(int spawnProbability, int minBroodSize, int maxBroodSize,
            float netherSpawnChanceMultiplier, boolean alwaysShowStats,
            double roostSpeedMultiplier, double breederSpeedMultiplier,
            boolean disableVanillaEggLaying, int collectorScanRange,
            boolean avianFluxEffectsEnabled, double fluxEggCapacityMultiplier,
            int avianFluxCapacity, int avianFluxMaxReceive, int avianFluxMaxExtract,
            int avianFluidCapacity, int avianFluidTransferRate, boolean avianFluidEffectsEnabled,
            boolean liquidEggHazardsEnabled, boolean fluidChickensEnabled,
            boolean chemicalChickensEnabled, boolean gasChickensEnabled) {
        this.spawnProbability = spawnProbability;
        this.minBroodSize = minBroodSize;
        this.maxBroodSize = maxBroodSize;
        this.netherSpawnChanceMultiplier = netherSpawnChanceMultiplier;
        this.alwaysShowStats = alwaysShowStats;
        this.roostSpeedMultiplier = roostSpeedMultiplier;
        this.breederSpeedMultiplier = breederSpeedMultiplier;
        this.disableVanillaEggLaying = disableVanillaEggLaying;
        this.collectorScanRange = collectorScanRange;
        this.avianFluxEffectsEnabled = avianFluxEffectsEnabled;
        this.fluxEggCapacityMultiplier = fluxEggCapacityMultiplier;
        this.avianFluxCapacity = avianFluxCapacity;
        this.avianFluxMaxReceive = avianFluxMaxReceive;
        this.avianFluxMaxExtract = avianFluxMaxExtract;
        this.avianFluidCapacity = avianFluidCapacity;
        this.avianFluidTransferRate = avianFluidTransferRate;
        this.avianFluidEffectsEnabled = avianFluidEffectsEnabled;
        this.liquidEggHazardsEnabled = liquidEggHazardsEnabled;
        this.fluidChickensEnabled = fluidChickensEnabled;
        this.chemicalChickensEnabled = chemicalChickensEnabled;
        this.gasChickensEnabled = gasChickensEnabled;
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

    public boolean isAvianFluxEffectsEnabled() {
        return avianFluxEffectsEnabled;
    }

    public double getFluxEggCapacityMultiplier() {
        return fluxEggCapacityMultiplier;
    }

    public int getAvianFluxCapacity() {
        return avianFluxCapacity;
    }

    public int getAvianFluxMaxReceive() {
        return avianFluxMaxReceive;
    }

    public int getAvianFluxMaxExtract() {
        return avianFluxMaxExtract;
    }

    public int getAvianFluidConverterCapacity(int fallback) {
        return avianFluidCapacity > 0 ? avianFluidCapacity : fallback;
    }

    public int getAvianFluidConverterTransfer(int fallback) {
        return avianFluidTransferRate > 0 ? avianFluidTransferRate : fallback;
    }

    public boolean isAvianFluidConverterEffectsEnabled() {
        return avianFluidEffectsEnabled;
    }

    public boolean isLiquidEggHazardsEnabled() {
        return liquidEggHazardsEnabled;
    }

    public boolean isFluidChickensEnabled() {
        return fluidChickensEnabled;
    }

    public boolean isChemicalChickensEnabled() {
        return chemicalChickensEnabled;
    }

    public boolean isGasChickensEnabled() {
        return gasChickensEnabled;
    }
}
