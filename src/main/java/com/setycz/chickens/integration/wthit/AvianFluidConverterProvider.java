package com.setycz.chickens.integration.wthit;

import com.setycz.chickens.blockentity.AvianFluidConverterBlockEntity;
import com.setycz.chickens.integration.wthit.overlay.HudOverlayHelper;
import mcp.mobius.waila.api.IDataProvider;
import mcp.mobius.waila.api.IDataWriter;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.IServerAccessor;

/**
 * Shares the converter's fluid tank contents with WTHIT so players can see the
 * buffered volume without opening the GUI.
 */
final class AvianFluidConverterProvider implements IDataProvider<AvianFluidConverterBlockEntity> {

    @Override
    public void appendData(IDataWriter writer, IServerAccessor<AvianFluidConverterBlockEntity> accessor, IPluginConfig config) {
        AvianFluidConverterBlockEntity blockEntity = accessor.getTarget();
        if (blockEntity == null) {
            return;
        }
        HudOverlayHelper helper = new HudOverlayHelper();
        helper.addFluid(blockEntity.getFluid().copy(), blockEntity.getTankCapacity());
        writer.add(HudOverlayHelper.TYPE, result -> result.add(helper));
    }
}
