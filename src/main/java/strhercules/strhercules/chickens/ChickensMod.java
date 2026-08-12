package strhercules.chickens;

import strhercules.chickens.command.ChickensCommands;
import strhercules.chickens.data.ChickensDataLoader;
import strhercules.chickens.RoostEggPreventer;
import strhercules.chickens.entity.MegaChicken;
import strhercules.chickens.registry.ModRegistry;
import strhercules.chickens.spawn.SpawnPlanDataLoader;
import strhercules.chickens.integration.mekanism.MekanismRadiationCompat;
import strhercules.chickens.network.MegaChickenFlightPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(ChickensMod.MOD_ID)
public final class ChickensMod {
    public static final String MOD_ID = "chickens";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public ChickensMod(IEventBus modBus) {
        ModRegistry.init(modBus);
        MegaChickenFlightPayload.init(modBus);
        modBus.addListener(this::onCommonSetup);
        ChickenTeachHandler.init();
        ChickensCommands.init();
        RoostEggPreventer.init();
        LavaChickenGameplay.init();
        MekanismRadiationCompat.init();
        NeoForge.EVENT_BUS.addListener(ChickensDataLoader::onTagsUpdated);
        NeoForge.EVENT_BUS.addListener(SpawnPlanDataLoader::onAddReloadListeners);
        NeoForge.EVENT_BUS.addListener(MegaChicken::preventRiderFallDamage);
        LOGGER.info("Modern Chickens mod initialised. Legacy content will be registered during later setup stages.");
    }

    public void onCommonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Running common setup for Modern Chickens");
        event.enqueueWork(ChickensDataLoader::bootstrap);
    }
}
