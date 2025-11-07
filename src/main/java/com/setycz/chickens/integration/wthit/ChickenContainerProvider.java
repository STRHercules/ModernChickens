package com.setycz.chickens.integration.wthit;

import com.setycz.chickens.blockentity.AbstractChickenContainerBlockEntity;
import com.setycz.chickens.integration.wthit.overlay.HudOverlayHelper;
import mcp.mobius.waila.api.IDataProvider;
import mcp.mobius.waila.api.IDataWriter;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.IServerAccessor;
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
        implements IDataProvider<T> {

    private static final String ETA_KEY = "ChickensEta";
    private static final String TOTAL_KEY = "ChickensTotal";
    private static final String STEP_KEY = "ChickensStep";

    @Override
    public void appendData(IDataWriter writer, IServerAccessor<T> accessor, IPluginConfig config) {
        T container = accessor.getTarget();
        if (container == null) {
            return;
        }
        CompoundTag tag = new CompoundTag();
        container.storeTooltipData(tag);
        tag.putInt(ETA_KEY, container.getRemainingLayTimeTicks());
        tag.putInt(TOTAL_KEY, container.getTotalLayTimeTicks());
        tag.putInt(STEP_KEY, container.getProgressIncrementPerTick());

        HudOverlayHelper helper = new HudOverlayHelper();
        List<Component> lines = new ArrayList<>();
        container.appendTooltip(lines, tag);
        lines.forEach(helper::addText);

        boolean hasChickens = tag.getBoolean("HasChickens");
        boolean hasSeeds = tag.getBoolean("HasSeeds");
        int totalTicks = tag.getInt(TOTAL_KEY);
        int etaTicks = tag.getInt(ETA_KEY);
        int step = tag.getInt(STEP_KEY);
        if (hasChickens && hasSeeds && totalTicks > 0 && etaTicks > 0 && step > 0) {
            helper.addText(Component.translatable("tooltip.chickens.wthit.eta",
                    describeEta(normaliseRemainingTicks(etaTicks, step))));
        }
        writer.add(HudOverlayHelper.TYPE, result -> result.add(helper));
    }

    private static int normaliseRemainingTicks(int remaining, int step) {
        if (remaining <= 0 || step <= 0) {
            return 0;
        }
        // Convert the accelerated progress counters back into real server ticks so
        // the tooltip communicates an accurate wall-clock ETA regardless of stack size.
        return Math.max(Mth.ceil(remaining / (float) step), 1);
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
