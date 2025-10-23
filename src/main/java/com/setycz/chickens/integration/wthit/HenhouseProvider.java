package com.setycz.chickens.integration.wthit;

import com.setycz.chickens.blockentity.HenhouseBlockEntity;
import mcp.mobius.waila.api.IBlockAccessor;
import mcp.mobius.waila.api.IBlockComponentProvider;
import mcp.mobius.waila.api.IDataProvider;
import mcp.mobius.waila.api.IDataWriter;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.IServerAccessor;
import mcp.mobius.waila.api.ITooltip;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;

/**
 * Reports the henhouse hay buffer so WTHIT can display the current fuel and any
 * unprocessed hay bales queued in the input slot.
 */
final class HenhouseProvider implements IBlockComponentProvider, IDataProvider<HenhouseBlockEntity> {
    private static final String ENERGY_KEY = "ChickensHayEnergy";
    private static final String HAY_COUNT_KEY = "ChickensHayCount";

    @Override
    public void appendData(IDataWriter writer, IServerAccessor<HenhouseBlockEntity> accessor, IPluginConfig config) {
        HenhouseBlockEntity blockEntity = accessor.getTarget();
        CompoundTag tag = writer.raw();
        tag.putInt(ENERGY_KEY, blockEntity.getEnergy());

        ItemStack hayStack = blockEntity.getItem(HenhouseBlockEntity.HAY_SLOT);
        int hayCount = isHayFuel(hayStack) ? hayStack.getCount() : 0;
        tag.putInt(HAY_COUNT_KEY, hayCount);
    }

    @Override
    public void appendBody(ITooltip tooltip, IBlockAccessor accessor, IPluginConfig config) {
        CompoundTag tag = accessor.getData().raw();
        if (!tag.contains(ENERGY_KEY)) {
            return;
        }
        int energy = tag.getInt(ENERGY_KEY);
        tooltip.addLine(Component.translatable("tooltip.chickens.henhouse.fuel", energy,
                HenhouseBlockEntity.HAY_BALE_ENERGY));

        int hayCount = tag.getInt(HAY_COUNT_KEY);
        if (hayCount > 0) {
            tooltip.addLine(Component.translatable("tooltip.chickens.henhouse.hay", hayCount));
        }
    }

    private static boolean isHayFuel(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.is(Blocks.HAY_BLOCK.asItem()) || stack.is(Tags.Items.STORAGE_BLOCKS_WHEAT));
    }
}
