package com.setycz.chickens;

import com.setycz.chickens.command.ChickensCommands;
import com.setycz.chickens.data.ChickensDataLoader;
import com.setycz.chickens.integration.jade.JadeIntegration;
import com.setycz.chickens.registry.ModRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for the modernised Chickens mod. At this stage we bootstrap
 * the NeoForge event listeners and leave placeholders for the extensive
 * content port that will follow.
 */
@Mod(ChickensMod.MOD_ID)
public final class ChickensMod {
    public static final String MOD_ID = "chickens";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public ChickensMod(IEventBus modBus) {
        ModRegistry.init(modBus);
        modBus.addListener(this::onCommonSetup);
        ChickenTeachHandler.init();
        ChickensCommands.init();
        JadeIntegration.init();
        LOGGER.info("Modern Chickens mod initialised. Legacy content will be registered during later setup stages.");
    }

    public void onCommonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Running common setup for Modern Chickens");
        // Defer the heavy registry bootstrap so it runs on the correct thread
        // once NeoForge has finished initialising its data tables.
        event.enqueueWork(ChickensDataLoader::bootstrap);
    }
}
