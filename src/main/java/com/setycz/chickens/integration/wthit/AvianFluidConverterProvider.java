package com.setycz.chickens.integration.wthit;

import com.setycz.chickens.blockentity.AvianFluidConverterBlockEntity;
import mcp.mobius.waila.api.IBlockAccessor;
import mcp.mobius.waila.api.IBlockComponentProvider;
import mcp.mobius.waila.api.IDataProvider;
import mcp.mobius.waila.api.IDataWriter;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.IServerAccessor;
import mcp.mobius.waila.api.ITooltip;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Shares the converter's fluid tank contents with WTHIT so players can see the
 * buffered volume without opening the GUI.
 */
final class AvianFluidConverterProvider implements IBlockComponentProvider, IDataProvider<AvianFluidConverterBlockEntity> {
    private static final String AMOUNT_KEY = "ChickensFluid";
    private static final String CAPACITY_KEY = "ChickensFluidCapacity";
    private static final String NAME_KEY = "ChickensFluidName";

    @Override
    public void appendData(IDataWriter writer, IServerAccessor<AvianFluidConverterBlockEntity> accessor, IPluginConfig config) {
        AvianFluidConverterBlockEntity blockEntity = accessor.getTarget();
        CompoundTag tag = writer.raw();
        tag.putInt(AMOUNT_KEY, blockEntity.getFluidAmount());
        tag.putInt(CAPACITY_KEY, blockEntity.getTankCapacity());
        FluidStack stack = blockEntity.getFluid();
        ResourceLocation id = stack.isEmpty() ? null : BuiltInRegistries.FLUID.getKey(stack.getFluid());
        if (id != null) {
            tag.putString(NAME_KEY, id.toString());
        }
    }

    @Override
    public void appendBody(ITooltip tooltip, IBlockAccessor accessor, IPluginConfig config) {
        CompoundTag tag = accessor.getData().raw();
        if (!tag.contains(AMOUNT_KEY) || !tag.contains(CAPACITY_KEY)) {
            return;
        }
        int amount = Math.max(tag.getInt(AMOUNT_KEY), 0);
        int capacity = Math.max(tag.getInt(CAPACITY_KEY), 0);
        if (capacity <= 0) {
            return;
        }
        Component name;
        if (!tag.contains(NAME_KEY) || amount <= 0) {
            name = Component.translatable("tooltip.chickens.avian_fluid_converter.empty");
        } else {
            ResourceLocation id = ResourceLocation.tryParse(tag.getString(NAME_KEY));
            FluidStack stack = id != null && BuiltInRegistries.FLUID.containsKey(id)
                    ? new FluidStack(BuiltInRegistries.FLUID.get(id), amount)
                    : FluidStack.EMPTY;
            name = stack.isEmpty() ? Component.literal(tag.getString(NAME_KEY)) : stack.getHoverName();
        }
        tooltip.addLine(Component.translatable("tooltip.chickens.avian_fluid_converter.level", name, amount, capacity));
    }
}
