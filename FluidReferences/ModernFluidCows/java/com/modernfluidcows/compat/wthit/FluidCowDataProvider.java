package com.modernfluidcows.compat.wthit;

import com.modernfluidcows.entity.FluidCow;
import mcp.mobius.waila.api.IDataProvider;
import mcp.mobius.waila.api.IDataWriter;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.IServerAccessor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;

/**
 * Serialises the Fluid Cow's fluid assignment and cooldown for WTHIT's server->client sync pipeline.
 */
public enum FluidCowDataProvider implements IDataProvider<FluidCow> {
    INSTANCE;

    /** Distinct keys so multiple mods can coexist without overwriting each other's payload. */
    public static final String TAG_FLUID = "fluidcows:fluid";
    public static final String TAG_COOLDOWN = "fluidcows:cooldown";

    @Override
    public void appendData(
            final IDataWriter data,
            final IServerAccessor<FluidCow> accessor,
            final IPluginConfig config) {
        CompoundTag tag = data.raw();
        FluidCow cow = accessor.getTarget();

        if (config.getBoolean(WthitOptions.SHOW_FLUID)) {
            Fluid fluid = cow.getFluid();
            if (fluid != null) {
                ResourceLocation key = BuiltInRegistries.FLUID.getKey(fluid);
                if (key != null) {
                    tag.putString(TAG_FLUID, key.toString());
                } else {
                    tag.putString(TAG_FLUID, "");
                }
            } else {
                tag.putString(TAG_FLUID, "");
            }
        }

        if (config.getBoolean(WthitOptions.SHOW_COOLDOWN)) {
            tag.putInt(TAG_COOLDOWN, cow.getMilkCooldown());
        }
    }
}
