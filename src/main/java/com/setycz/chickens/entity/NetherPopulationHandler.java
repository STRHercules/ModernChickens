package com.setycz.chickens.entity;

import com.setycz.chickens.ChickensRegistry;
import com.setycz.chickens.ChickensRegistryItem;
import com.setycz.chickens.SpawnType;
import com.setycz.chickens.config.ChickensConfigHolder;
import com.setycz.chickens.config.ChickensConfigValues;
import com.setycz.chickens.registry.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Reintroduces the legacy nether population burst that spawned chicken groups
 * while chunks were being decorated. Modern Minecraft no longer exposes the
 * exact event, so we approximate the behaviour on server tick with conservative
 * spawn checks to avoid flooding the Nether with birds.
 */
public final class NetherPopulationHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("ChickensNetherPopulate");
    private static final int CHECK_INTERVAL_TICKS = 20;
    private static final int SPAWN_RADIUS = 16;

    private NetherPopulationHandler() {
    }

    public static void init() {
        NeoForge.EVENT_BUS.addListener(NetherPopulationHandler::onLevelTick);
    }

    private static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!level.dimensionType().ultraWarm()) {
            return;
        }
        if (!ChickensRegistry.isAnyIn(SpawnType.HELL)) {
            return;
        }
        if (level.getGameTime() % CHECK_INTERVAL_TICKS != 0) {
            return;
        }

        ChickensConfigValues config = ChickensConfigHolder.get();
        float baseChance = 0.1F * config.getNetherSpawnChanceMultiplier();
        if (baseChance <= 0.0F || level.random.nextFloat() >= baseChance) {
            return;
        }

        ServerPlayer targetPlayer = level.getRandomPlayer();
        if (targetPlayer == null) {
            return;
        }

        BlockPos basePos = pickSpawnPosition(level, targetPlayer.blockPosition(), level.random);
        if (basePos == null) {
            return;
        }

        List<ChickensRegistryItem> netherChickens = ChickensRegistry.getPossibleChickensToSpawn(SpawnType.HELL);
        if (netherChickens.isEmpty()) {
            return;
        }
        ChickensRegistryItem selected = netherChickens.get(level.random.nextInt(netherChickens.size()));

        int min = Math.max(1, config.getMinBroodSize());
        int max = Math.max(min, config.getMaxBroodSize());
        int count = Mth.nextInt(level.random, min, max);

        int spawned = 0;
        for (int i = 0; i < count; i++) {
            BlockPos attemptPos = basePos.offset(level.random.nextInt(5) - 2, 0, level.random.nextInt(5) - 2);
            if (spawnChicken(level, attemptPos, selected, level.random)) {
                spawned++;
            }
        }

        if (spawned > 0 && LOGGER.isDebugEnabled()) {
            LOGGER.debug("Spawned {} nether chickens around {}", spawned, basePos);
        }
    }

    @Nullable
    private static BlockPos pickSpawnPosition(ServerLevel level, BlockPos origin, RandomSource random) {
        int x = origin.getX() + random.nextInt(SPAWN_RADIUS * 2 + 1) - SPAWN_RADIUS;
        int z = origin.getZ() + random.nextInt(SPAWN_RADIUS * 2 + 1) - SPAWN_RADIUS;

        int topY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(x, Math.min(topY, level.getMaxBuildHeight() - 1), z);
        while (cursor.getY() > level.getMinBuildHeight() && level.isEmptyBlock(cursor)) {
            cursor.move(Direction.DOWN);
        }
        if (cursor.getY() <= level.getMinBuildHeight()) {
            return null;
        }
        cursor.move(Direction.UP);
        if (!SpawnPlacements.checkSpawnRules(ModEntityTypes.CHICKENS_CHICKEN.get(),
                level, MobSpawnType.NATURAL, cursor, random)) {
            return null;
        }
        return cursor.immutable();
    }

    private static boolean spawnChicken(ServerLevel level, BlockPos pos, ChickensRegistryItem description, RandomSource random) {
        if (!ChickensChicken.checkSpawnRules(ModEntityTypes.CHICKENS_CHICKEN.get(), level, MobSpawnType.NATURAL, pos, random)) {
            return false;
        }
        ChickensChicken chicken = ModEntityTypes.CHICKENS_CHICKEN.get().create(level);
        if (chicken == null) {
            return false;
        }
        chicken.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
                random.nextFloat() * 360.0F, 0.0F);
        chicken.setChickenType(description.getId());
        chicken.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.NATURAL, null);
        level.addFreshEntity(chicken);
        return true;
    }
}
