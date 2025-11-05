package com.setycz.chickens.integration.wthit;

import com.setycz.chickens.blockentity.AvianFluxConverterBlockEntity;
import com.setycz.chickens.blockentity.AvianFluidConverterBlockEntity;
import com.setycz.chickens.blockentity.BreederBlockEntity;
import com.setycz.chickens.blockentity.HenhouseBlockEntity;
import com.setycz.chickens.blockentity.RoostBlockEntity;
import mcp.mobius.waila.api.IRegistrar;
import mcp.mobius.waila.api.IWailaPlugin;
import mcp.mobius.waila.api.TooltipPosition;

/**
 * Main entry point that wires Chickens block entities into WTHIT. The plugin
 * registers lightweight tooltip providers mirroring the existing Jade overlay
 * so players receive consistent in-world stats regardless of their HUD mod of
 * choice.
 */
public final class ChickensWthitPlugin implements IWailaPlugin {

    @Override
    public void register(IRegistrar registrar) {
        AvianFluxConverterProvider fluxProvider = new AvianFluxConverterProvider();
        registrar.addBlockData(fluxProvider, AvianFluxConverterBlockEntity.class);
        registrar.addComponent(fluxProvider, TooltipPosition.BODY, AvianFluxConverterBlockEntity.class);

        AvianFluidConverterProvider fluidProvider = new AvianFluidConverterProvider();
        registrar.addBlockData(fluidProvider, AvianFluidConverterBlockEntity.class);
        registrar.addComponent(fluidProvider, TooltipPosition.BODY, AvianFluidConverterBlockEntity.class);

        ChickenContainerProvider<RoostBlockEntity> roostProvider = new ChickenContainerProvider<>();
        registrar.addBlockData(roostProvider, RoostBlockEntity.class);
        registrar.addComponent(roostProvider, TooltipPosition.BODY, RoostBlockEntity.class);

        ChickenContainerProvider<BreederBlockEntity> breederProvider = new ChickenContainerProvider<>();
        registrar.addBlockData(breederProvider, BreederBlockEntity.class);
        registrar.addComponent(breederProvider, TooltipPosition.BODY, BreederBlockEntity.class);

        HenhouseProvider henhouseProvider = new HenhouseProvider();
        registrar.addBlockData(henhouseProvider, HenhouseBlockEntity.class);
        registrar.addComponent(henhouseProvider, TooltipPosition.BODY, HenhouseBlockEntity.class);
    }
}
