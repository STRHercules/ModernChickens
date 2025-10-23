package com.setycz.chickens.integration.wthit;

import com.setycz.chickens.blockentity.AvianFluxConverterBlockEntity;
import mcp.mobius.waila.api.IBlockAccessor;
import mcp.mobius.waila.api.IBlockComponentProvider;
import mcp.mobius.waila.api.IDataProvider;
import mcp.mobius.waila.api.IDataWriter;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.IServerAccessor;
import mcp.mobius.waila.api.ITooltip;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/**
 * Shares the converter's internal RF buffer with WTHIT so players can read the
 * current charge directly from the overlay without opening the GUI.
 */
final class AvianFluxConverterProvider implements IBlockComponentProvider, IDataProvider<AvianFluxConverterBlockEntity> {
    private static final String ENERGY_KEY = "ChickensEnergy";
    private static final String CAPACITY_KEY = "ChickensEnergyCapacity";

    @Override
    public void appendData(IDataWriter writer, IServerAccessor<AvianFluxConverterBlockEntity> accessor, IPluginConfig config) {
        AvianFluxConverterBlockEntity blockEntity = accessor.getTarget();
        CompoundTag tag = writer.raw();
        tag.putInt(ENERGY_KEY, blockEntity.getEnergyStored());
        tag.putInt(CAPACITY_KEY, blockEntity.getEnergyCapacity());
    }

    @Override
    public void appendBody(ITooltip tooltip, IBlockAccessor accessor, IPluginConfig config) {
        CompoundTag tag = accessor.getData().raw();
        if (!tag.contains(ENERGY_KEY) || !tag.contains(CAPACITY_KEY)) {
            return;
        }
        int energy = tag.getInt(ENERGY_KEY);
        int capacity = Math.max(tag.getInt(CAPACITY_KEY), 0);
        if (capacity <= 0) {
            return;
        }
        tooltip.addLine(Component.translatable("tooltip.chickens.avian_flux_converter.energy", energy, capacity));
    }
}
