package com.setycz.chickens.registry;

import com.setycz.chickens.liquidegg.LiquidEggFluidWrapper;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

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
    }
}
