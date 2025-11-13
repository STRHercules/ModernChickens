package com.setycz.chickens.entity;

import com.setycz.chickens.ChickensRegistry;
import com.setycz.chickens.ChickensRegistryItem;
import com.setycz.chickens.SpawnType;
import com.setycz.chickens.config.ChickensConfigHolder;
import com.setycz.chickens.config.ChickensConfigValues;
import com.setycz.chickens.registry.ModEntityTypes;
import com.setycz.chickens.spawn.ChickensSpawnDebug;
import com.setycz.chickens.spawn.ChickensSpawnManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import javax.annotation.Nullable;

/**
 * Optional overworld spawn helper that periodically spawns a burst of chickens around random players.
 * This gives testers a deterministic way to observe the new spawn system even when vanilla mob caps are crowded.
 */
public final class OverworldPopulationHandler {
    private static final int CHECK_INTERVAL_TICKS = 40;
    private static final int SPAWN_RADIUS = 32;

    private OverworldPopulationHandler() {
    }

    public static void init() {
        NeoForge.EVENT_BUS.addListener(OverworldPopulationHandler::onLevelTick);
    }

    private static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (level.dimensionType().ultraWarm() || level.dimensionType().natural() && level.dimensionType().piglinSafe()) {
            return;
        }
        boolean normalPlan = ChickensSpawnManager.hasPlan(SpawnType.NORMAL);
        boolean snowPlan = ChickensSpawnManager.hasPlan(SpawnType.SNOW);
        if (!normalPlan && !snowPlan) {
            return;
        }
        if (level.getGameTime() % CHECK_INTERVAL_TICKS != 0) {
            return;
        }

        ChickensConfigValues config = ChickensConfigHolder.get();
        float probability = Mth.clamp(config.getSpawnProbability() / 20.0F, 0.05F, 1.0F);
        probability *= Math.max(1.0F, ChickensSpawnDebug.getSpawnWeightMultiplier());
        if (level.random.nextFloat() >= probability) {
            return;
        }

        ServerPlayer player = level.getRandomPlayer();
        if (player == null) {
            return;
        }

        BlockPos origin = pickSpawnPosition(level, player.blockPosition(), level.random);
        if (origin == null) {
            return;
        }
        SpawnType spawnType = ChickensRegistry.getSpawnType(level.getBiome(origin));
        if (spawnType == SpawnType.NONE || !ChickensSpawnManager.hasPlan(spawnType)) {
            return;
        }

        ChickensRegistryItem chicken = ChickensSpawnManager.pickChicken(spawnType, level.random).orElse(null);
        if (chicken == null) {
            return;
        }

        int min = Math.max(1, config.getMinBroodSize());
        int max = Math.max(min, config.getMaxBroodSize());
        int count = Mth.nextInt(level.random, min, max);
        for (int i = 0; i < count; i++) {
            BlockPos attempt = origin.offset(level.random.nextInt(5) - 2, 0, level.random.nextInt(5) - 2);
            spawnChicken(level, attempt, chicken, level.random);
        }
    }

    @Nullable
    private static BlockPos pickSpawnPosition(ServerLevel level, BlockPos playerPos, RandomSource random) {
        int x = playerPos.getX() + random.nextInt(SPAWN_RADIUS * 2 + 1) - SPAWN_RADIUS;
        int z = playerPos.getZ() + random.nextInt(SPAWN_RADIUS * 2 + 1) - SPAWN_RADIUS;
        int topY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(x, Math.min(topY, level.getMaxBuildHeight() - 1), z);
        while (cursor.getY() > level.getMinBuildHeight() && level.isEmptyBlock(cursor)) {
            cursor.move(Direction.DOWN);
        }
        if (cursor.getY() <= level.getMinBuildHeight()) {
            return null;
        }
        cursor.move(Direction.UP);
        if (!SpawnPlacements.checkSpawnRules(ModEntityTypes.CHICKENS_CHICKEN.get(), level, MobSpawnType.NATURAL, cursor, random)) {
            return null;
        }
        return cursor.immutable();
    }

    private static void spawnChicken(ServerLevel level, BlockPos pos, ChickensRegistryItem chicken, RandomSource random) {
        if (!ChickensChicken.checkSpawnRules(ModEntityTypes.CHICKENS_CHICKEN.get(), level, MobSpawnType.NATURAL, pos, random)) {
            return;
        }
        ChickensChicken entity = ModEntityTypes.CHICKENS_CHICKEN.get().create(level);
        if (entity == null) {
            return;
        }
        entity.setChickenType(chicken.getId());
        entity.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, random.nextFloat() * 360.0F, 0.0F);
        entity.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.NATURAL, null);
        level.addFreshEntity(entity);
    }
}
