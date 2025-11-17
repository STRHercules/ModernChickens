package com.setycz.chickens.integration.jade;

import com.setycz.chickens.ChickensMod;
import com.setycz.chickens.block.AvianDousingMachineBlock;
import com.setycz.chickens.block.AvianFluidConverterBlock;
import com.setycz.chickens.block.AvianFluxConverterBlock;
import com.setycz.chickens.block.BreederBlock;
import com.setycz.chickens.block.CollectorBlock;
import com.setycz.chickens.block.HenhouseBlock;
import com.setycz.chickens.block.IncubatorBlock;
import com.setycz.chickens.block.RoostBlock;
import com.setycz.chickens.blockentity.AvianDousingMachineBlockEntity;
import com.setycz.chickens.blockentity.AvianFluidConverterBlockEntity;
import com.setycz.chickens.blockentity.AvianFluxConverterBlockEntity;
import com.setycz.chickens.blockentity.BreederBlockEntity;
import com.setycz.chickens.blockentity.CollectorBlockEntity;
import com.setycz.chickens.blockentity.HenhouseBlockEntity;
import com.setycz.chickens.blockentity.IncubatorBlockEntity;
import com.setycz.chickens.blockentity.RoostBlockEntity;
import com.setycz.chickens.entity.ChickensChicken;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Native Jade plugin that mirrors the WTHIT HUD so players get identical overlay
 * information regardless of which tooltip mod they prefer.
 */
@WailaPlugin(ChickensMod.MOD_ID)
public final class JadeIntegration implements IWailaPlugin {

    /**
     * Legacy hook retained for compatibility. The native Jade plugin is
     * discovered via {@link WailaPlugin} and no longer needs explicit IMC.
     */
    public static void init() {
        // no-op
    }

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(AvianFluxConverterDataProvider.INSTANCE, AvianFluxConverterBlockEntity.class);
        registration.registerBlockDataProvider(AvianFluidConverterDataProvider.INSTANCE, AvianFluidConverterBlockEntity.class);
        registration.registerBlockDataProvider(AvianDousingMachineDataProvider.INSTANCE, AvianDousingMachineBlockEntity.class);
        registration.registerBlockDataProvider(ChickenContainerDataProvider.INSTANCE, RoostBlockEntity.class);
        registration.registerBlockDataProvider(ChickenContainerDataProvider.INSTANCE, BreederBlockEntity.class);
        registration.registerBlockDataProvider(ChickenContainerDataProvider.INSTANCE, CollectorBlockEntity.class);
        registration.registerBlockDataProvider(HenhouseDataProvider.INSTANCE, HenhouseBlockEntity.class);
        registration.registerBlockDataProvider(IncubatorDataProvider.INSTANCE, IncubatorBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(ChickensHudComponentProvider.INSTANCE, AvianFluxConverterBlock.class);
        registration.registerBlockComponent(ChickensHudComponentProvider.INSTANCE, AvianFluidConverterBlock.class);
        registration.registerBlockComponent(ChickensHudComponentProvider.INSTANCE, AvianDousingMachineBlock.class);
        registration.registerBlockComponent(ChickensHudComponentProvider.INSTANCE, RoostBlock.class);
        registration.registerBlockComponent(ChickensHudComponentProvider.INSTANCE, BreederBlock.class);
        registration.registerBlockComponent(ChickensHudComponentProvider.INSTANCE, CollectorBlock.class);
        registration.registerBlockComponent(ChickensHudComponentProvider.INSTANCE, HenhouseBlock.class);
        registration.registerBlockComponent(ChickensHudComponentProvider.INSTANCE, IncubatorBlock.class);

        registration.registerEntityComponent(ChickensChickenProvider.INSTANCE, ChickensChicken.class);
        registration.addTooltipCollectedCallback(9999, JadeOverlaySanitiser.INSTANCE);
    }
}
