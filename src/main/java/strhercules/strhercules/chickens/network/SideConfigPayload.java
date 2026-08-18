package strhercules.chickens.network;

import strhercules.chickens.blockentity.MachineSideConfig;
import strhercules.chickens.blockentity.SideConfigurable;
import strhercules.chickens.menu.SideConfigMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server-validated changes made from a machine's in-GUI I/O window. */
public record SideConfigPayload(BlockPos pos, int direction, int channel, boolean reset) {

    public static void send(BlockPos pos, Direction direction, MachineSideConfig.Channel channel) {
        ChickensNetwork.CHANNEL.sendToServer(new SideConfigPayload(pos, direction.ordinal(), channel.ordinal(), false));
    }

    public static void sendReset(BlockPos pos) {
        ChickensNetwork.CHANNEL.sendToServer(new SideConfigPayload(pos, -1, -1, true));
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeVarInt(direction);
        buffer.writeVarInt(channel);
        buffer.writeBoolean(reset);
    }

    public static SideConfigPayload decode(FriendlyByteBuf buffer) {
        return new SideConfigPayload(buffer.readBlockPos(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readBoolean());
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                apply(player);
            }
        });
        ctx.setPacketHandled(true);
    }

    private void apply(ServerPlayer player) {
        if (!(player.containerMenu instanceof SideConfigMenu menu)
                || !menu.getSideConfigPos().equals(pos)
                || !menu.stillValid(player)) {
            return;
        }
        BlockEntity blockEntity = player.level().getBlockEntity(pos);
        if (!(blockEntity instanceof SideConfigurable configurable)) {
            return;
        }

        if (reset) {
            configurable.sideConfig().reset();
        } else if (direction >= 0 && direction < Direction.values().length
                && channel >= 0 && channel < MachineSideConfig.Channel.values().length) {
            configurable.sideConfig().cycle(Direction.values()[direction],
                    MachineSideConfig.Channel.values()[channel]);
        } else {
            return;
        }

        blockEntity.setChanged();
        player.level().sendBlockUpdated(pos, blockEntity.getBlockState(), blockEntity.getBlockState(),
                Block.UPDATE_ALL);
    }
}
