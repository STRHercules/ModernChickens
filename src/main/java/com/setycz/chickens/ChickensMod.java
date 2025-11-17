package com.setycz.chickens;

import com.setycz.chickens.command.ChickensCommands;
import com.setycz.chickens.data.ChickensDataLoader;
import com.setycz.chickens.RoostEggPreventer;
import com.setycz.chickens.entity.NetherPopulationHandler;
import com.setycz.chickens.registry.ModRegistry;
import com.setycz.chickens.data.ChickenItemModelProvider;
import com.setycz.chickens.spawn.SpawnPlanDataLoader;
import com.setycz.chickens.entity.OverworldPopulationHandler;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.data.event.GatherDataEvent;
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
        NetherPopulationHandler.init();
        OverworldPopulationHandler.init();
        RoostEggPreventer.init();
        NeoForge.EVENT_BUS.addListener(ChickensDataLoader::onTagsUpdated);
        NeoForge.EVENT_BUS.addListener(SpawnPlanDataLoader::onAddReloadListeners);
        LOGGER.info("Modern Chickens mod initialised. Legacy content will be registered during later setup stages.");
        modBus.addListener(this::onGatherData);
    }

    public void onCommonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Running common setup for Modern Chickens");
        // Defer the heavy registry bootstrap so it runs on the correct thread
        // once NeoForge has finished initialising its data tables.
        event.enqueueWork(ChickensDataLoader::bootstrap);
    }

    private void onGatherData(GatherDataEvent event) {
        var generator = event.getGenerator();
        generator.getVanillaPack(true).addProvider(ChickenItemModelProvider::new);
    }
}
