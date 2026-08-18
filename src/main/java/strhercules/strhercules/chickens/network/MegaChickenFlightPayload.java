package strhercules.chickens.network;

import strhercules.chickens.entity.MegaChicken;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record MegaChickenFlightPayload(boolean jumping, boolean diving, boolean airbraking) {

    public static void send(boolean jumping, boolean diving, boolean airbraking) {
        ChickensNetwork.CHANNEL.sendToServer(new MegaChickenFlightPayload(jumping, diving, airbraking));
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(jumping);
        buffer.writeBoolean(diving);
        buffer.writeBoolean(airbraking);
    }

    public static MegaChickenFlightPayload decode(FriendlyByteBuf buffer) {
        return new MegaChickenFlightPayload(buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean());
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> apply(ctx.getSender()));
        ctx.setPacketHandled(true);
    }

    private void apply(ServerPlayer player) {
        if (player != null
                && player.getVehicle() instanceof MegaChicken chicken
                && chicken.getOwnerUUID() != null
                && chicken.getOwnerUUID().equals(player.getUUID())
                && chicken.hasFlyingEgg()) {
            chicken.setJumpRequested(jumping);
            chicken.setDiveRequested(diving && !chicken.onGround());
            chicken.setAirbrakeRequested(airbraking && !chicken.onGround());
        }
    }
}
