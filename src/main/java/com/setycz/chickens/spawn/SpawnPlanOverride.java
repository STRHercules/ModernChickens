package com.setycz.chickens.spawn;

import com.setycz.chickens.SpawnType;

/**
 * Datapack-driven overrides that patch individual spawn plan fields. Absent values fall back to
 * the classic configuration-derived defaults.
 */
public record SpawnPlanOverride(SpawnType spawnType,
                                Integer absoluteWeight,
                                Double weightMultiplier,
                                Integer minBroodSize,
                                Integer maxBroodSize,
                                Double spawnCharge,
                                Double energyBudget) {

    public int applyWeight(int baseWeight) {
        if (absoluteWeight != null) {
            return Math.max(0, absoluteWeight);
        }
        if (weightMultiplier != null) {
            return Math.max(0, Math.round((float) (baseWeight * weightMultiplier)));
        }
        return baseWeight;
    }

    public int applyMinBrood(int base) {
        return minBroodSize != null ? Math.max(1, minBroodSize) : base;
    }

    public int applyMaxBrood(int base, int minValue) {
        if (maxBroodSize != null) {
            return Math.max(minValue, maxBroodSize);
        }
        return base;
    }

    public double applyCharge(double base) {
        return spawnCharge != null ? spawnCharge : base;
    }

    public double applyEnergy(double base) {
        return energyBudget != null ? energyBudget : base;
    }
}
