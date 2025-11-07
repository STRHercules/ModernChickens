package com.setycz.chickens.integration.wthit;

import com.setycz.chickens.blockentity.AvianFluxConverterBlockEntity;
import com.setycz.chickens.integration.wthit.overlay.HudOverlayHelper;
import mcp.mobius.waila.api.IDataProvider;
import mcp.mobius.waila.api.IDataWriter;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.IServerAccessor;

/**
 * Shares the converter's internal RF buffer with WTHIT so players can read the
 * current charge directly from the overlay without opening the GUI.
 */
final class AvianFluxConverterProvider implements IDataProvider<AvianFluxConverterBlockEntity> {

    @Override
    public void appendData(IDataWriter writer, IServerAccessor<AvianFluxConverterBlockEntity> accessor, IPluginConfig config) {
        AvianFluxConverterBlockEntity blockEntity = accessor.getTarget();
        if (blockEntity == null) {
            return;
        }
        HudOverlayHelper helper = new HudOverlayHelper();
        helper.addEnergy(blockEntity.getEnergyStored(), blockEntity.getEnergyCapacity());
        writer.add(HudOverlayHelper.TYPE, result -> result.add(helper));
    }
}
