package com.modernfluidcows.compat.wthit;

import com.modernfluidcows.entity.FluidCow;
import mcp.mobius.waila.api.IClientRegistrar;
import mcp.mobius.waila.api.IWailaClientPlugin;

/** Wires the Fluid Cow tooltip renderer into WTHIT on the client side. */
public final class FluidCowsWthitClientPlugin implements IWailaClientPlugin {

    @Override
    public void register(final IClientRegistrar registrar) {
        registrar.body(FluidCowTooltipProvider.INSTANCE, FluidCow.class);
    }
}
