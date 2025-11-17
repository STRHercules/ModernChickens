package com.setycz.chickens.integration.jade;

import com.setycz.chickens.ChickensMod;
import com.setycz.chickens.blockentity.AbstractChickenContainerBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides shared tooltip content for roost-like containers and adds an ETA
 * line so Jade mirrors the WTHIT overlay.
 */
enum ChickenContainerDataProvider implements IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ChickensMod.MOD_ID, "chicken_container");
    private static final String ETA_KEY = "ChickensEta";
    private static final String TOTAL_KEY = "ChickensTotal";
    private static final String STEP_KEY = "ChickensStep";

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof AbstractChickenContainerBlockEntity container)) {
            return;
        }
        CompoundTag tag = new CompoundTag();
        container.storeTooltipData(tag);
        tag.putInt(ETA_KEY, container.getRemainingLayTimeTicks());
        tag.putInt(TOTAL_KEY, container.getTotalLayTimeTicks());
        tag.putInt(STEP_KEY, container.getProgressIncrementPerTick());

        HudData.Builder builder = HudData.builder();
        List<Component> lines = new ArrayList<>();
        container.appendTooltip(lines, tag);
        lines.forEach(builder::addText);

        boolean hasChickens = tag.getBoolean("HasChickens");
        boolean hasSeeds = tag.getBoolean("HasSeeds");
        int totalTicks = tag.getInt(TOTAL_KEY);
        int etaTicks = tag.getInt(ETA_KEY);
        int step = tag.getInt(STEP_KEY);
        if (hasChickens && hasSeeds && totalTicks > 0 && etaTicks > 0 && step > 0) {
            builder.addText(Component.translatable("tooltip.chickens.wthit.eta",
                    describeEta(normaliseRemainingTicks(etaTicks, step))));
        }
        HudData.write(data, builder.build());
    }

    @Override
    public ResourceLocation getUid() {
        return ID;
    }

    private static int normaliseRemainingTicks(int remaining, int step) {
        if (remaining <= 0 || step <= 0) {
            return 0;
        }
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
