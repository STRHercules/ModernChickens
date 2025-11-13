package com.modernfluidcows.compat.wthit;

import com.modernfluidcows.entity.FluidCow;
import mcp.mobius.waila.api.ICommonRegistrar;
import mcp.mobius.waila.api.IWailaCommonPlugin;

/** Registers the Fluid Cow data provider and config toggles on the logical server. */
public final class FluidCowsWthitCommonPlugin implements IWailaCommonPlugin {

    @Override
    public void register(final ICommonRegistrar registrar) {
        registrar.featureConfig(WthitOptions.SHOW_FLUID, true);
        registrar.featureConfig(WthitOptions.SHOW_COOLDOWN, true);
        registrar.entityData(FluidCowDataProvider.INSTANCE, FluidCow.class);
    }
}
