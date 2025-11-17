package com.setycz.chickens.integration.wthit;

import com.setycz.chickens.blockentity.AvianFluxConverterBlockEntity;
import com.setycz.chickens.blockentity.AvianDousingMachineBlockEntity;
import com.setycz.chickens.blockentity.AvianFluidConverterBlockEntity;
import com.setycz.chickens.blockentity.BreederBlockEntity;
import com.setycz.chickens.blockentity.IncubatorBlockEntity;
import com.setycz.chickens.blockentity.HenhouseBlockEntity;
import com.setycz.chickens.blockentity.RoostBlockEntity;
import com.setycz.chickens.integration.wthit.overlay.HudOverlayHelper;
import com.setycz.chickens.integration.wthit.overlay.HudTooltipRenderer;
import mcp.mobius.waila.api.IRegistrar;
import mcp.mobius.waila.api.IWailaPlugin;
import mcp.mobius.waila.api.TooltipPosition;
import mcp.mobius.waila.api.IEventListener;
import mcp.mobius.waila.api.ICommonAccessor;
import mcp.mobius.waila.api.ITooltip;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.data.EnergyData;
import mcp.mobius.waila.api.data.FluidData;
import net.minecraft.network.chat.Component;

/**
 * Main entry point that wires Chickens block entities into WTHIT. The plugin
 * registers lightweight tooltip providers mirroring the existing Jade overlay
 * so players receive consistent in-world stats regardless of their HUD mod of
 * choice.
 */
public final class ChickensWthitPlugin implements IWailaPlugin {

    @Override
    public void register(IRegistrar registrar) {
        registrar.addDataType(HudOverlayHelper.TYPE, HudOverlayHelper.STREAM_CODEC);

        HudTooltipRenderer hudRenderer = new HudTooltipRenderer();
        registrar.addEventListener(new IEventListener() {
            @Override
            public void onHandleTooltip(ITooltip tooltip, ICommonAccessor accessor, IPluginConfig config) {
                // Strip WTHIT's universal fluid/energy lines when our custom HUD is present by replacing them with blanks.
                if (tooltip.getLine(FluidData.ID) != null) {
                    tooltip.setLine(FluidData.ID).with(Component.empty());
                }
                if (tooltip.getLine(HudTooltipRenderer.HUD_TAG) != null && tooltip.getLine(EnergyData.ID) != null) {
                    tooltip.setLine(EnergyData.ID).with(Component.empty());
                }
            }
        });

        AvianFluxConverterProvider fluxProvider = new AvianFluxConverterProvider();
        registrar.addBlockData(fluxProvider, AvianFluxConverterBlockEntity.class);
        registrar.addComponent(hudRenderer, TooltipPosition.BODY, AvianFluxConverterBlockEntity.class);

        AvianDousingMachineProvider dousingProvider = new AvianDousingMachineProvider();
        registrar.addBlockData(dousingProvider, AvianDousingMachineBlockEntity.class);
        registrar.addComponent(hudRenderer, TooltipPosition.BODY, AvianDousingMachineBlockEntity.class);

        AvianFluidConverterProvider fluidProvider = new AvianFluidConverterProvider();
        registrar.addBlockData(fluidProvider, AvianFluidConverterBlockEntity.class);
        registrar.addComponent(hudRenderer, TooltipPosition.BODY, AvianFluidConverterBlockEntity.class);

        ChickenContainerProvider<RoostBlockEntity> roostProvider = new ChickenContainerProvider<>();
        registrar.addBlockData(roostProvider, RoostBlockEntity.class);
        registrar.addComponent(hudRenderer, TooltipPosition.BODY, RoostBlockEntity.class);

        ChickenContainerProvider<BreederBlockEntity> breederProvider = new ChickenContainerProvider<>();
        registrar.addBlockData(breederProvider, BreederBlockEntity.class);
        registrar.addComponent(hudRenderer, TooltipPosition.BODY, BreederBlockEntity.class);

        HenhouseProvider henhouseProvider = new HenhouseProvider();
        registrar.addBlockData(henhouseProvider, HenhouseBlockEntity.class);
        registrar.addComponent(hudRenderer, TooltipPosition.BODY, HenhouseBlockEntity.class);

        IncubatorProvider incubatorProvider = new IncubatorProvider();
        registrar.addBlockData(incubatorProvider, IncubatorBlockEntity.class);
        registrar.addComponent(hudRenderer, TooltipPosition.BODY, IncubatorBlockEntity.class);
    }
}
