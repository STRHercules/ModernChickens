package com.setycz.chickens.registry;

import com.mojang.serialization.MapCodec;
import com.setycz.chickens.ChickensRegistry;
import com.setycz.chickens.SpawnType;
import com.setycz.chickens.config.ChickensConfigHolder;
import com.setycz.chickens.config.ChickensConfigValues;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;

/**
 * Adds the custom chicken entity to biomes that should naturally spawn modded birds.
 * The modifier inspects the runtime registry and configuration so changes to the
 * legacy properties file are honoured without needing data generation.
 */
public final class ChickensSpawnBiomeModifier implements BiomeModifier {
    public static final ChickensSpawnBiomeModifier INSTANCE = new ChickensSpawnBiomeModifier();
    public static final MapCodec<ChickensSpawnBiomeModifier> CODEC = MapCodec.unit(() -> INSTANCE);

    private ChickensSpawnBiomeModifier() {
    }

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase != Phase.ADD) {
            return;
        }
        SpawnType spawnType = ChickensRegistry.getSpawnType(biome);
        if (spawnType == SpawnType.NONE) {
            return;
        }
        if (ChickensRegistry.getPossibleChickensToSpawn(spawnType).isEmpty()) {
            return;
        }

        ChickensConfigValues config = ChickensConfigHolder.get();
        int weight = config.getSpawnProbability();
        if (spawnType == SpawnType.HELL) {
            weight = Math.round(weight * config.getNetherSpawnChanceMultiplier());
        }
        if (weight <= 0) {
            return;
        }

        MobSpawnSettings.SpawnerData entry = new MobSpawnSettings.SpawnerData(
                ModEntityTypes.CHICKENS_CHICKEN.get(), weight, config.getMinBroodSize(), config.getMaxBroodSize());
        builder.getMobSpawnSettings().addSpawn(MobCategory.CREATURE, entry);
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return CODEC;
    }
}
