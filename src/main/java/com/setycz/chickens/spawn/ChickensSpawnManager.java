package com.setycz.chickens.spawn;

import com.setycz.chickens.ChickensRegistry;
import com.setycz.chickens.ChickensRegistryItem;
import com.setycz.chickens.SpawnType;
import com.setycz.chickens.config.ChickensConfigHolder;
import com.setycz.chickens.config.ChickensConfigValues;
import com.setycz.chickens.registry.ModEntityTypes;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Central manager that owns all runtime spawn tuning for Modern Chickens. NeoForge 1.21 lets us keep
 * {@link MobSpawnSettings.SpawnerData}, spawn charges, and weighted chicken pools cached so biome modifiers,
 * custom world population, and the entity itself can draw from the same modern data source.
 */
public final class ChickensSpawnManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("ChickensSpawns");
    private static final float SNOW_WEIGHT_MODIFIER = 0.75F;
    private static final float END_WEIGHT_MODIFIER = 0.5F;
    private static final double OVERWORLD_CHARGE = 0.12D;
    private static final double OVERWORLD_ENERGY = 0.32D;
    private static final double NETHER_CHARGE = 0.18D;
    private static final double NETHER_ENERGY = 0.45D;

    private static final Map<SpawnType, SpawnPlan> PLANS = new EnumMap<>(SpawnType.class);

    private ChickensSpawnManager() {
    }

    /**
     * Rebuilds the spawn tables from the registry and configuration snapshot. Call this once after the registry is
     * populated and whenever datapacks/config reloads mutate the chicken roster.
     */
    public static void refreshFromRegistry() {
        ChickensConfigValues config = ChickensConfigHolder.get();
        Map<SpawnType, SpawnPlan> rebuilt = new EnumMap<>(SpawnType.class);
        for (SpawnType spawnType : SpawnType.values()) {
            if (spawnType == SpawnType.NONE) {
                continue;
            }
            List<ChickensRegistryItem> candidates = ChickensRegistry.getPossibleChickensToSpawn(spawnType);
            if (candidates.isEmpty()) {
                continue;
            }

            List<WeightedEntry.Wrapper<ChickensRegistryItem>> weighted = new ArrayList<>();
            for (ChickensRegistryItem chicken : candidates) {
                int weight = spawnWeightFor(chicken);
                if (weight > 0) {
                    weighted.add(WeightedEntry.wrap(chicken, weight));
                }
            }
            if (weighted.isEmpty()) {
                continue;
            }

            float debugMultiplier = ChickensSpawnDebug.getSpawnWeightMultiplier();
            int spawnWeight = Math.round(computeSpawnWeight(spawnType, config) * debugMultiplier);
            if (spawnWeight <= 0) {
                continue;
            }

            int minBrood = Math.max(1, config.getMinBroodSize());
            int maxBrood = Math.max(minBrood, config.getMaxBroodSize());
            double charge = spawnType == SpawnType.HELL ? NETHER_CHARGE : OVERWORLD_CHARGE;
            double energy = spawnType == SpawnType.HELL ? NETHER_ENERGY : OVERWORLD_ENERGY;

            var override = SpawnPlanDataLoader.getOverride(spawnType);
            if (override.isPresent()) {
                SpawnPlanOverride data = override.get();
                spawnWeight = data.applyWeight(spawnWeight);
                minBrood = data.applyMinBrood(minBrood);
                maxBrood = data.applyMaxBrood(maxBrood, minBrood);
                charge = data.applyCharge(charge);
                energy = data.applyEnergy(energy);
            }

            if (spawnWeight <= 0) {
                continue;
            }

            MobSpawnSettings.SpawnerData spawner = new MobSpawnSettings.SpawnerData(
                    ModEntityTypes.CHICKENS_CHICKEN.get(),
                    spawnWeight,
                    minBrood,
                    maxBrood);

            rebuilt.put(spawnType, new SpawnPlan(
                    spawnType,
                    spawner,
                    charge,
                    energy,
                    WeightedRandomList.create(weighted)));
        }

        PLANS.clear();
        PLANS.putAll(rebuilt);
        LOGGER.debug("Spawn tables rebuilt: {}", PLANS.keySet());
    }

    private static int computeSpawnWeight(SpawnType type, ChickensConfigValues config) {
        int weight = Math.max(0, config.getSpawnProbability());
        if (type == SpawnType.HELL) {
            weight = Math.round(weight * config.getNetherSpawnChanceMultiplier());
        } else if (type == SpawnType.SNOW) {
            weight = Math.round(weight * SNOW_WEIGHT_MODIFIER);
        } else if (type == SpawnType.END) {
            weight = Math.round(weight * END_WEIGHT_MODIFIER);
        }
        return weight;
    }

    private static int spawnWeightFor(ChickensRegistryItem chicken) {
        // Lower tiers should dominate the overworld pool while still allowing rare high-tier spawns when enabled.
        return Math.max(1, 8 - Math.min(7, chicken.getTier()));
    }

    public static Optional<SpawnPlan> planFor(Holder<Biome> biome) {
        return planFor(ChickensRegistry.getSpawnType(biome));
    }

    public static Optional<SpawnPlan> planFor(SpawnType type) {
        return Optional.ofNullable(PLANS.get(type));
    }

    public static boolean hasPlan(SpawnType type) {
        return PLANS.containsKey(type);
    }

    public static Optional<ChickensRegistryItem> pickChicken(Holder<Biome> biome, RandomSource random) {
        return planFor(biome).flatMap(plan -> plan.pick(random));
    }

    public static Optional<ChickensRegistryItem> pickChicken(SpawnType type, RandomSource random) {
        return planFor(type).flatMap(plan -> plan.pick(random));
    }

    public static Optional<MobSpawnSettings.SpawnerData> spawnerFor(Holder<Biome> biome) {
        return planFor(biome).map(SpawnPlan::spawnerData);
    }

    /**
     * Immutable snapshot of the spawn metadata for a biome category. Consumers can reuse the pooled weighted list when
     * assigning chicken breeds rather than recomputing tiers every time a chunk spawns creatures.
     */
    public record SpawnPlan(SpawnType spawnType,
                            MobSpawnSettings.SpawnerData spawnerData,
                            double spawnCharge,
                            double energyBudget,
                            WeightedRandomList<WeightedEntry.Wrapper<ChickensRegistryItem>> chickens) {
        public Optional<ChickensRegistryItem> pick(RandomSource random) {
            return chickens.getRandom(random).map(WeightedEntry.Wrapper::data);
        }

        public MobCategory category() {
            return spawnerData.type.getCategory();
        }
    }
}
