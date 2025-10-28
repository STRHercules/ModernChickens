package com.setycz.chickens.debug;

import com.setycz.chickens.network.CollectorDebugTogglePayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Tracks which players requested the collector range overlay so the command can
 * support toggle semantics without introducing a full capability.
 */
public final class CollectorDebugState {
    private static final Set<UUID> ENABLED_PLAYERS = new HashSet<>();

    private CollectorDebugState() {
    }

    /**
     * Hooks the logout event to clean up any cached state when a player leaves
     * the server.
     */
    public static void init() {
        NeoForge.EVENT_BUS.addListener(CollectorDebugState::onPlayerLoggedOut);
    }

    /**
     * Returns whether the overlay is currently active for the provided player.
     */
    public static boolean isEnabled(ServerPlayer player) {
        return ENABLED_PLAYERS.contains(player.getUUID());
    }

    /**
     * Toggles the overlay state for the player and returns the new value so
     * commands can report the updated status.
     */
    public static boolean toggle(ServerPlayer player) {
        boolean enabled = !isEnabled(player);
        set(player, enabled);
        return enabled;
    }

    /**
     * Sets the overlay state for the player and immediately synchronises the
     * new value to the client.
     */
    public static void set(ServerPlayer player, boolean enabled) {
        if (enabled) {
            ENABLED_PLAYERS.add(player.getUUID());
        } else {
            ENABLED_PLAYERS.remove(player.getUUID());
        }
        PacketDistributor.sendToPlayer(player, new CollectorDebugTogglePayload(enabled));
    }

    private static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ENABLED_PLAYERS.remove(player.getUUID());
        }
    }
}
