package com.setycz.chickens.network;

import com.setycz.chickens.ChickensMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * Clientbound payload that toggles the collector debug overlay for a specific
 * player.
 */
public record CollectorDebugTogglePayload(boolean enabled) implements CustomPacketPayload {
    public static final Type<CollectorDebugTogglePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ChickensMod.MOD_ID, "collector_debug_toggle"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CollectorDebugTogglePayload> STREAM_CODEC = StreamCodec
            .of((buffer, payload) -> buffer.writeBoolean(payload.enabled()),
                    buffer -> new CollectorDebugTogglePayload(buffer.readBoolean()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Applies the overlay state on the main client thread when the payload is
     * received.
     */
    public static void handle(CollectorDebugTogglePayload payload, IPayloadContext context) {
        if (context.flow() != PacketFlow.CLIENTBOUND) {
            return;
        }
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return;
        }
        // Guard the client-only overlay class behind a dist check so dedicated servers
        // can register the payload without loading Minecraft client classes.
        context.enqueueWork(
                () -> com.setycz.chickens.client.debug.CollectorDebugOverlay.setEnabled(payload.enabled()));
    }
}
