package strhercules.chickens.network;

import strhercules.chickens.ChickensMod;
import strhercules.chickens.entity.MegaChicken;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record MegaChickenFlightPayload(boolean jumping, boolean diving, boolean airbraking)
        implements CustomPacketPayload {
    public static final Type<MegaChickenFlightPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ChickensMod.MOD_ID, "mega_chicken_flight"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MegaChickenFlightPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, MegaChickenFlightPayload::jumping,
                    ByteBufCodecs.BOOL, MegaChickenFlightPayload::diving,
                    ByteBufCodecs.BOOL, MegaChickenFlightPayload::airbraking, MegaChickenFlightPayload::new);

    public static void init(IEventBus modBus) {
        modBus.addListener(MegaChickenFlightPayload::register);
    }

    private static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player
                    && player.getVehicle() instanceof MegaChicken chicken
                    && chicken.getOwnerUUID() != null
                    && chicken.getOwnerUUID().equals(player.getUUID())
                    && chicken.hasFlyingEgg()) {
                chicken.setJumpRequested(payload.jumping());
                chicken.setDiveRequested(payload.diving() && !chicken.onGround());
                chicken.setAirbrakeRequested(payload.airbraking() && !chicken.onGround());
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
