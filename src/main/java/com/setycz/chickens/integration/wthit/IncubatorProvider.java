package com.setycz.chickens.integration.wthit;

import com.setycz.chickens.blockentity.IncubatorBlockEntity;
import com.setycz.chickens.integration.wthit.overlay.HudOverlayHelper;
import mcp.mobius.waila.api.IDataProvider;
import mcp.mobius.waila.api.IDataWriter;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.IServerAccessor;
import net.minecraft.network.chat.Component;

/**
 * Streams the Incubator's RF buffer and incubation status to WTHIT.
 */
final class IncubatorProvider implements IDataProvider<IncubatorBlockEntity> {

    @Override
    public void appendData(IDataWriter writer, IServerAccessor<IncubatorBlockEntity> accessor, IPluginConfig config) {
        IncubatorBlockEntity incubator = accessor.getTarget();
        if (incubator == null) {
            return;
        }
        HudOverlayHelper helper = new HudOverlayHelper();
        helper.addEnergy(incubator.getEnergyStored(), incubator.getEnergyCapacity());
        int maxProgress = Math.max(incubator.getMaxProgress(), 1);
        int percent = Math.max(0, incubator.getProgress()) * 100 / maxProgress;
        helper.addText(Component.translatable("tooltip.chickens.incubator.progress", percent));
        helper.addText(Component.translatable("tooltip.chickens.incubator.cost", incubator.getEnergyCost()));
        writer.add(HudOverlayHelper.TYPE, result -> result.add(helper));
    }
}
