package strhercules.chickens.integration.jade;

import strhercules.chickens.ChickensMod;
import strhercules.chickens.blockentity.NestBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

import java.util.Locale;

/** Adds the nest's configured aura values and live fuel state to Jade. */
enum NestDataProvider implements IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ChickensMod.MOD_ID,
            "nest_data");

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof NestBlockEntity nest)) {
            return;
        }

        int configuredDuration = nest.getBoostDurationTicks();
        int remainingDuration = nest.getSeedTicksRemaining();
        int configuredSeconds = Mth.ceil(configuredDuration / 20.0F);
        int remainingSeconds = Mth.ceil(remainingDuration / 20.0F);
        double multiplier = nest.getBoostMultiplier();
        double effectiveMultiplier = nest.getEffectiveBoostMultiplier();

        HudData.Builder builder = HudData.builder();
        builder.addText(Component.translatable("tooltip.chickens.nest.range",
                Math.max(0, strhercules.chickens.config.ChickensConfigHolder.get().getRoosterAuraRange())));
        builder.addText(Component.translatable("tooltip.chickens.nest.duration",
                remainingSeconds, configuredSeconds));
        builder.addText(Component.translatable("tooltip.chickens.nest.multiplier",
                formatMultiplier(multiplier), formatMultiplier(effectiveMultiplier)));
        builder.addText(Component.translatable("tooltip.chickens.nest.conflict",
                nest.getConflictingActiveNestCount()));
        HudData.write(data, builder.build());
    }

    @Override
    public ResourceLocation getUid() {
        return ID;
    }

    private static String formatMultiplier(double multiplier) {
        return String.format(Locale.ROOT, "%.2f", Math.max(0.0D, multiplier));
    }
}
