package com.setycz.chickens.integration.jade;

import com.setycz.chickens.ChickensMod;
import com.setycz.chickens.config.ChickensConfigHolder;
import com.setycz.chickens.entity.ChickensChicken;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * Recreates the Jade entity tooltip from the legacy Waila bridge without
 * relying on reflection.
 */
enum ChickensChickenProvider implements IEntityComponentProvider {
    INSTANCE;

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ChickensMod.MOD_ID, "chickens_chicken");

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        if (!(accessor.getEntity() instanceof ChickensChicken chicken)) {
            return;
        }
        tooltip.add(Component.translatable("entity.ChickensChicken.tier", chicken.getTier()));

        boolean alwaysShow = ChickensConfigHolder.get().isAlwaysShowStats();
        if (chicken.getStatsAnalyzed() || alwaysShow) {
            tooltip.add(Component.translatable("entity.ChickensChicken.growth", chicken.getGrowth()));
            tooltip.add(Component.translatable("entity.ChickensChicken.gain", chicken.getGain()));
            tooltip.add(Component.translatable("entity.ChickensChicken.strength", chicken.getStrength()));
        }

        if (!chicken.isBaby()) {
            int progress = chicken.getLayProgress();
            if (progress <= 0) {
                tooltip.add(Component.translatable("entity.ChickensChicken.nextEggSoon"));
            } else {
                tooltip.add(Component.translatable("entity.ChickensChicken.layProgress", progress));
            }
        }
    }

    @Override
    public ResourceLocation getUid() {
        return ID;
    }
}
