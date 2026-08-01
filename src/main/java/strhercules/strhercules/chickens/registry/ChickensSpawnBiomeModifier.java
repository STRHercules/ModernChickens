package strhercules.chickens.registry;

import com.mojang.serialization.MapCodec;
import strhercules.chickens.spawn.ChickensSpawnManager;
import strhercules.chickens.spawn.ChickensSpawnManager.SpawnPlan;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
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
        if (!hasVanillaChickenSpawn(builder)) {
            return;
        }
        ChickensSpawnManager.planFor(biome).ifPresent(plan -> addSpawn(builder, plan));
    }

    private static void addSpawn(ModifiableBiomeInfo.BiomeInfo.Builder builder, SpawnPlan plan) {
        builder.getMobSpawnSettings().addSpawn(plan.category(), plan.spawnerData());
        builder.getMobSpawnSettings().addMobCharge(plan.spawnerData().type, plan.spawnCharge(), plan.energyBudget());
        builder.getMobSpawnSettings().addSpawn(MobCategory.CREATURE,
                new MobSpawnSettings.SpawnerData(ModEntityTypes.ROOSTER.get(), 1, 1, 1));
    }

    private static boolean hasVanillaChickenSpawn(ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        return builder.getMobSpawnSettings().getSpawner(MobCategory.CREATURE).stream()
                .anyMatch(spawn -> spawn.type == EntityType.CHICKEN);
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return CODEC;
    }
}
