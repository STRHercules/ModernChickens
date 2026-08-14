package strhercules.chickens.integration.jade;

import strhercules.chickens.ChickensMod;
import strhercules.chickens.blockentity.MechanicalNestBlockEntity;
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
        HudData.Builder builder = HudData.builder();
        if (accessor.getBlockEntity() instanceof NestBlockEntity nest) {
            appendNestData(builder,
                    Math.max(0, strhercules.chickens.config.ChickensConfigHolder.get().getRoosterAuraRange()),
                    nest.getSeedTicksRemaining(), nest.getBoostDurationTicks(), nest.getBoostMultiplier(),
                    nest.getEffectiveBoostMultiplier(), nest.getConflictingActiveNestCount());
        } else if (accessor.getBlockEntity() instanceof MechanicalNestBlockEntity nest) {
            builder.addText(Component.translatable("tooltip.chickens.nest.range", nest.getAuraRange()));
            builder.addText(Component.translatable("tooltip.chickens.nest.active_roosts",
                    nest.getActiveBoostedRoostCount()));
            builder.addText(Component.translatable("tooltip.chickens.nest.energy_usage", nest.getEnergyCost()));
            builder.addText(Component.translatable("tooltip.chickens.nest.multiplier",
                    formatMultiplier(nest.getBoostMultiplier()), formatMultiplier(nest.getEffectiveBoostMultiplier())));
            builder.addText(Component.translatable("tooltip.chickens.nest.conflict",
                    nest.getConflictingActiveNestCount()));
            builder.addEnergy(nest.getEnergyStored(), nest.getEnergyCapacity());
        } else {
            return;
        }
        HudData.write(data, builder.build());
    }

    private static void appendNestData(HudData.Builder builder, int range, int remainingDuration, int configuredDuration,
            double multiplier, double effectiveMultiplier, int conflictingNestCount) {
        int configuredSeconds = Mth.ceil(configuredDuration / 20.0F);
        int remainingSeconds = Mth.ceil(remainingDuration / 20.0F);

        builder.addText(Component.translatable("tooltip.chickens.nest.range", Math.max(0, range)));
        builder.addText(Component.translatable("tooltip.chickens.nest.duration",
                remainingSeconds, configuredSeconds));
        builder.addText(Component.translatable("tooltip.chickens.nest.multiplier",
                formatMultiplier(multiplier), formatMultiplier(effectiveMultiplier)));
        builder.addText(Component.translatable("tooltip.chickens.nest.conflict", conflictingNestCount));
    }

    @Override
    public ResourceLocation getUid() {
        return ID;
    }

    private static String formatMultiplier(double multiplier) {
        return String.format(Locale.ROOT, "%.2f", Math.max(0.0D, multiplier));
    }
}
