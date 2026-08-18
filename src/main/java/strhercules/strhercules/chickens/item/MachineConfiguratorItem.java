package strhercules.chickens.item;

import net.minecraft.nbt.CompoundTag;
import javax.annotation.Nullable;
import strhercules.chickens.blockentity.MachineSideConfig;
import strhercules.chickens.blockentity.SideConfigurable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import strhercules.chickens.item.ItemData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

/** Mekanism-style face configurator with no Mekanism runtime dependency. */
public final class MachineConfiguratorItem extends Item {
    private static final String CHANNEL_TAG = "Channel";

    public MachineConfiguratorItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockEntity blockEntity = level.getBlockEntity(context.getClickedPos());
        if (!(blockEntity instanceof SideConfigurable configurable)) {
            return InteractionResult.PASS;
        }
        MachineSideConfig.Channel channel = channel(context.getItemInHand());
        if (!level.isClientSide) {
            MachineSideConfig.Mode mode = configurable.sideConfig().cycle(context.getClickedFace(), channel);
            blockEntity.setChanged();
            level.sendBlockUpdated(context.getClickedPos(), blockEntity.getBlockState(),
                    blockEntity.getBlockState(), 3);
            if (context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(Component.translatable(
                        "message.chickens.configurator.face", context.getClickedFace().getName(),
                        channelName(channel), modeName(mode)), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player,
            InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            MachineSideConfig.Channel next = MachineSideConfig.Channel.values()[(channel(stack).ordinal() + 1)
                    % MachineSideConfig.Channel.values().length];
            setChannel(stack, next);
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.chickens.configurator.channel",
                        channelName(next)), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip,
            TooltipFlag flag) {
        tooltip.add(Component.translatable("item.chickens.configurator.channel", channelName(channel(stack)))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.chickens.configurator.hint").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static MachineSideConfig.Channel channel(ItemStack stack) {
        CompoundTag data = ItemData.read(stack);
        int ordinal = data.getInt(CHANNEL_TAG);
        return ordinal >= 0 && ordinal < MachineSideConfig.Channel.values().length
                ? MachineSideConfig.Channel.values()[ordinal]
                : MachineSideConfig.Channel.ITEMS;
    }

    private static void setChannel(ItemStack stack, MachineSideConfig.Channel channel) {
        ItemData.update(stack, tag -> tag.putInt(CHANNEL_TAG, channel.ordinal()));
    }

    private static String channelName(MachineSideConfig.Channel channel) {
        return Component.translatable("item.chickens.configurator.channel." + channel.name().toLowerCase()).getString();
    }

    private static String modeName(MachineSideConfig.Mode mode) {
        return Component.translatable("item.chickens.configurator.mode." + mode.name().toLowerCase()).getString();
    }
}
