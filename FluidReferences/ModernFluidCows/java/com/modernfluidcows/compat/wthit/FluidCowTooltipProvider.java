package com.modernfluidcows.compat.wthit;

import com.modernfluidcows.entity.FluidCow;
import com.modernfluidcows.util.FCUtils;
import mcp.mobius.waila.api.IEntityAccessor;
import mcp.mobius.waila.api.IEntityComponentProvider;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.ITooltip;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Injects Fluid Cow details into the WTHIT tooltip using the data synchronised by the common plugin.
 */
public enum FluidCowTooltipProvider implements IEntityComponentProvider {
    INSTANCE;

    @Override
    public void appendBody(final ITooltip tooltip, final IEntityAccessor accessor, final IPluginConfig config) {
        CompoundTag data = accessor.getData().raw();

        if (config.getBoolean(WthitOptions.SHOW_FLUID)) {
            Component fluidLine = resolveFluidLine(data);
            if (fluidLine != null) {
                tooltip.addLine(fluidLine);
            }
        }

        if (config.getBoolean(WthitOptions.SHOW_COOLDOWN)) {
            Component cooldownLine = resolveCooldownLine(accessor, data);
            if (cooldownLine != null) {
                tooltip.addLine(cooldownLine);
            }
        }
    }

    /** Builds the fluid tooltip entry based on the synced registry id. */
    private static Component resolveFluidLine(final CompoundTag data) {
        if (!data.contains(FluidCowDataProvider.TAG_FLUID, Tag.TAG_STRING)) {
            return null;
        }
        String stored = data.getString(FluidCowDataProvider.TAG_FLUID);
        if (stored.isEmpty()) {
            return Component.translatable("tooltip.fluidcows.wthit.fluid_unassigned");
        }
        ResourceLocation key = ResourceLocation.tryParse(stored);
        if (key == null) {
            return Component.translatable("tooltip.fluidcows.wthit.fluid_unassigned");
        }
        Fluid fluid = BuiltInRegistries.FLUID.getOptional(key).orElse(Fluids.EMPTY);
        if (fluid == Fluids.EMPTY) {
            return Component.translatable("tooltip.fluidcows.wthit.fluid_unassigned");
        }
        return Component.translatable("tooltip.fluidcows.wthit.fluid", FCUtils.getFluidName(fluid));
    }

    /** Converts the synced cooldown to a human-readable timer string. */
    private static Component resolveCooldownLine(final IEntityAccessor accessor, final CompoundTag data) {
        if (!data.contains(FluidCowDataProvider.TAG_COOLDOWN, Tag.TAG_INT)) {
            return null;
        }
        FluidCow cow = accessor.getEntity();
        if (cow.isBaby()) {
            return Component.translatable("tooltip.fluidcows.wthit.cooldown_baby");
        }
        int cooldown = Math.max(0, data.getInt(FluidCowDataProvider.TAG_COOLDOWN));
        if (cooldown <= 0) {
            return Component.translatable("tooltip.fluidcows.wthit.cooldown_ready");
        }
        int seconds = Mth.ceil(cooldown / 20.0F);
        String formatted = FCUtils.toTime(seconds, "0s");
        return Component.translatable("tooltip.fluidcows.wthit.cooldown_wait", formatted);
    }
}
