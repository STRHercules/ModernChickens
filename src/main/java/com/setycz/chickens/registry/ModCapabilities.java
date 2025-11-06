package com.setycz.chickens.registry;

import com.setycz.chickens.integration.mekanism.MekanismChemicalHelper;
import com.setycz.chickens.liquidegg.LiquidEggFluidWrapper;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Handles capability registration for the mod. Wiring the legacy fluid handler
 * keeps automation compatibility until the broader transfer API becomes
 * ubiquitous.
 */
public final class ModCapabilities {
    private ModCapabilities() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(ModCapabilities::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(Capabilities.FluidHandler.ITEM,
                (stack, context) -> new LiquidEggFluidWrapper(stack),
                ModRegistry.LIQUID_EGG.get());

        // Expose container inventories through the NeoForge capability bridge so
        // automation mods can interact with the roost-style blocks just like the
        // legacy item handler wrappers allowed.
        registerContainerCapability(event, ModBlockEntities.ROOST.get());
        registerContainerCapability(event, ModBlockEntities.BREEDER.get());
        registerContainerCapability(event, ModBlockEntities.COLLECTOR.get());
        registerContainerCapability(event, ModBlockEntities.HENHOUSE.get());
        registerContainerCapability(event, ModBlockEntities.AVIAN_FLUX_CONVERTER.get());
        registerContainerCapability(event, ModBlockEntities.AVIAN_FLUID_CONVERTER.get());
        registerContainerCapability(event, ModBlockEntities.AVIAN_CHEMICAL_CONVERTER.get());

        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.AVIAN_FLUX_CONVERTER.get(),
                (blockEntity, direction) -> blockEntity.getEnergyStorage(direction));
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.AVIAN_FLUID_CONVERTER.get(),
                (blockEntity, direction) -> blockEntity.getFluidTank(direction));

        var chemicalCapability = MekanismChemicalHelper.getChemicalBlockCapability();
        if (chemicalCapability != null) {
            event.registerBlockEntity(
                    chemicalCapability,
                    ModBlockEntities.AVIAN_CHEMICAL_CONVERTER.get(),
                    (blockEntity, direction) -> blockEntity.getChemicalHandler(direction));
        }
    }

    private static <T extends BlockEntity & WorldlyContainer> void registerContainerCapability(
            RegisterCapabilitiesEvent event,
            BlockEntityType<T> type) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                type,
                (blockEntity, direction) -> new SidedInvWrapper(blockEntity, direction));
    }
}
