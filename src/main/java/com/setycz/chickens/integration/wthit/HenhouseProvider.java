package com.setycz.chickens.integration.wthit;

import com.setycz.chickens.blockentity.HenhouseBlockEntity;
import com.setycz.chickens.integration.wthit.overlay.HudOverlayHelper;
import mcp.mobius.waila.api.IDataProvider;
import mcp.mobius.waila.api.IDataWriter;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.IServerAccessor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;

/**
 * Reports the henhouse hay buffer so WTHIT can display the current fuel and any
 * unprocessed hay bales queued in the input slot.
 */
final class HenhouseProvider implements IDataProvider<HenhouseBlockEntity> {

    @Override
    public void appendData(IDataWriter writer, IServerAccessor<HenhouseBlockEntity> accessor, IPluginConfig config) {
        HenhouseBlockEntity blockEntity = accessor.getTarget();
        if (blockEntity == null) {
            return;
        }
        HudOverlayHelper helper = new HudOverlayHelper();
        helper.addEnergy(blockEntity.getEnergy(), HenhouseBlockEntity.HAY_BALE_ENERGY * HenhouseBlockEntity.SLOT_COUNT);
        ItemStack hayStack = blockEntity.getItem(HenhouseBlockEntity.HAY_SLOT);
        int hayCount = isHayFuel(hayStack) ? hayStack.getCount() : 0;
        if (hayCount > 0) {
            helper.addText(Component.translatable("tooltip.chickens.henhouse.hay", hayCount));
        }
        writer.add(HudOverlayHelper.TYPE, result -> result.add(helper));
    }

    private static boolean isHayFuel(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.is(Blocks.HAY_BLOCK.asItem()) || stack.is(Tags.Items.STORAGE_BLOCKS_WHEAT));
    }
}
