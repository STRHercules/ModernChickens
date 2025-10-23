package com.setycz.chickens.integration.wthit;

import com.setycz.chickens.blockentity.AbstractChickenContainerBlockEntity;
import mcp.mobius.waila.api.IBlockAccessor;
import mcp.mobius.waila.api.IBlockComponentProvider;
import mcp.mobius.waila.api.IDataProvider;
import mcp.mobius.waila.api.IDataWriter;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.IServerAccessor;
import mcp.mobius.waila.api.ITooltip;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides shared tooltip content for roost-like containers. The provider
 * mirrors the server-side Jade tooltip builder and adds an ETA line so WTHIT
 * users can judge when the next egg or offspring will be produced.
 */
final class ChickenContainerProvider<T extends AbstractChickenContainerBlockEntity>
        implements IBlockComponentProvider, IDataProvider<T> {

    private static final String ETA_KEY = "ChickensEta";
    private static final String TOTAL_KEY = "ChickensTotal";

    @Override
    public void appendData(IDataWriter writer, IServerAccessor<T> accessor, IPluginConfig config) {
        T container = accessor.getTarget();
        CompoundTag tag = writer.raw();
        container.storeTooltipData(tag);
        tag.putInt(ETA_KEY, container.getRemainingLayTimeTicks());
        tag.putInt(TOTAL_KEY, container.getTotalLayTimeTicks());
    }

    @Override
    public void appendBody(ITooltip tooltip, IBlockAccessor accessor, IPluginConfig config) {
        @SuppressWarnings("unchecked")
        T container = (T) accessor.getBlockEntity();
        if (container == null) {
            return;
        }
        CompoundTag tag = accessor.getData().raw();
        if (tag.isEmpty()) {
            return;
        }

        List<Component> lines = new ArrayList<>();
        container.appendTooltip(lines, tag);
        for (Component line : lines) {
            tooltip.addLine(line);
        }

        boolean hasChickens = tag.getBoolean("HasChickens");
        boolean hasSeeds = tag.getBoolean("HasSeeds");
        int totalTicks = tag.getInt(TOTAL_KEY);
        int etaTicks = tag.getInt(ETA_KEY);
        if (hasChickens && hasSeeds && totalTicks > 0 && etaTicks > 0) {
            tooltip.addLine(Component.translatable("tooltip.chickens.wthit.eta", describeEta(etaTicks)));
        }
    }

    private static Component describeEta(int ticks) {
        if (ticks <= 0) {
            return Component.translatable("tooltip.chickens.wthit.time.less_than_second");
        }
        int seconds = Mth.ceil(ticks / 20.0F);
        if (seconds <= 0) {
            return Component.translatable("tooltip.chickens.wthit.time.less_than_second");
        }
        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;
        if (minutes > 0) {
            return Component.translatable("tooltip.chickens.wthit.time.minutes", minutes, remainingSeconds);
        }
        return Component.translatable("tooltip.chickens.wthit.time.seconds", seconds);
    }
}
