package strhercules.chickens.network;

import strhercules.chickens.ChickensMod;
import strhercules.chickens.blockentity.MachineSideConfig;
import strhercules.chickens.blockentity.SideConfigurable;
import strhercules.chickens.menu.SideConfigMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Server-validated changes made from a machine's in-GUI I/O window. */
public record SideConfigPayload(BlockPos pos, int direction, int channel, boolean reset) implements CustomPacketPayload {
    public static final Type<SideConfigPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ChickensMod.MOD_ID, "side_config"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SideConfigPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, SideConfigPayload::pos,
            ByteBufCodecs.VAR_INT, SideConfigPayload::direction,
            ByteBufCodecs.VAR_INT, SideConfigPayload::channel,
            ByteBufCodecs.BOOL, SideConfigPayload::reset,
            SideConfigPayload::new);

    public static void init(IEventBus modBus) {
        modBus.addListener(SideConfigPayload::register);
    }

    public static void send(BlockPos pos, Direction direction, MachineSideConfig.Channel channel) {
        PacketDistributor.sendToServer(new SideConfigPayload(pos, direction.ordinal(), channel.ordinal(), false));
    }

    public static void sendReset(BlockPos pos) {
        PacketDistributor.sendToServer(new SideConfigPayload(pos, -1, -1, true));
    }

    private static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                context.enqueueWork(() -> apply(payload, player));
            }
        });
    }

    private static void apply(SideConfigPayload payload, ServerPlayer player) {
        if (!(player.containerMenu instanceof SideConfigMenu menu)
                || !menu.getSideConfigPos().equals(payload.pos())
                || !menu.stillValid(player)) {
            return;
        }
        BlockEntity blockEntity = player.level().getBlockEntity(payload.pos());
        if (!(blockEntity instanceof SideConfigurable configurable)) {
            return;
        }

        if (payload.reset()) {
            configurable.sideConfig().reset();
        } else if (payload.direction() >= 0 && payload.direction() < Direction.values().length
                && payload.channel() >= 0 && payload.channel() < MachineSideConfig.Channel.values().length) {
            configurable.sideConfig().cycle(Direction.values()[payload.direction()],
                    MachineSideConfig.Channel.values()[payload.channel()]);
        } else {
            return;
        }

        blockEntity.setChanged();
        player.level().sendBlockUpdated(payload.pos(), blockEntity.getBlockState(), blockEntity.getBlockState(),
                Block.UPDATE_ALL);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
