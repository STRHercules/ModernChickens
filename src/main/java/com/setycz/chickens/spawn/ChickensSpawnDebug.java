package com.setycz.chickens.spawn;

import com.setycz.chickens.ChickensRegistry;
import com.setycz.chickens.ChickensRegistryItem;
import com.setycz.chickens.SpawnType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds runtime-only debug knobs that boost spawn weights and optionally broadcast natural spawn events.
 * Intended for testing; the values reset when Minecraft restarts.
 */
public final class ChickensSpawnDebug {
    private static final Logger LOGGER = LoggerFactory.getLogger("ChickensSpawnDebug");
    private static volatile float spawnWeightMultiplier = 1.0F;
    private static volatile boolean loggingEnabled;

    private ChickensSpawnDebug() {
    }

    public static float getSpawnWeightMultiplier() {
        return spawnWeightMultiplier;
    }

    public static void setSpawnWeightMultiplier(float multiplier) {
        spawnWeightMultiplier = Math.max(0.0F, multiplier);
        ChickensSpawnManager.refreshFromRegistry();
    }

    public static boolean isLoggingEnabled() {
        return loggingEnabled;
    }

    public static void setLoggingEnabled(boolean enabled) {
        loggingEnabled = enabled;
    }

    public static void broadcastSpawn(ServerLevel level, BlockPos pos, ChickensRegistryItem chicken) {
        if (!loggingEnabled) {
            return;
        }
        Component name = chicken.getDisplayName();
        SpawnType spawnType = ChickensRegistry.getSpawnType(level.getBiome(pos));
        Component message = Component.translatable("debug.chickens.spawn",
                name,
                spawnType.name().toLowerCase(),
                pos.getX(),
                pos.getY(),
                pos.getZ()).withStyle(ChatFormatting.YELLOW);
        LOGGER.info("[Chickens] {} spawned at {} {} {} ({})", chicken.getEntityName(), pos.getX(), pos.getY(), pos.getZ(), spawnType);
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player.hasPermissions(2)) {
                player.sendSystemMessage(message);
            }
        }
    }
}
