package strhercules.chickens;

import strhercules.chickens.command.ChickensCommands;
import strhercules.chickens.data.ChickensDataLoader;
import strhercules.chickens.RoostEggPreventer;
import strhercules.chickens.entity.MegaChicken;
import strhercules.chickens.registry.ModRegistry;
import strhercules.chickens.spawn.SpawnPlanDataLoader;
import strhercules.chickens.integration.mekanism.MekanismRadiationCompat;
import strhercules.chickens.network.ChickensNetwork;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.common.MinecraftForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(ChickensMod.MOD_ID)
public final class ChickensMod {
    public static final String MOD_ID = "chickens";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public ChickensMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModRegistry.init(modBus);
        ChickensNetwork.init();
        modBus.addListener(this::onCommonSetup);
        ChickenTeachHandler.init();
        ChickensCommands.init();
        RoostEggPreventer.init();
        LavaChickenGameplay.init();
        MegaChickenLoot.init();
        MekanismRadiationCompat.init();
        MinecraftForge.EVENT_BUS.addListener(ChickensDataLoader::onTagsUpdated);
        MinecraftForge.EVENT_BUS.addListener(SpawnPlanDataLoader::onAddReloadListeners);
        MinecraftForge.EVENT_BUS.addListener(MegaChicken::preventRiderFallDamage);
        LOGGER.info("Modern Chickens mod initialised. Legacy content will be registered during later setup stages.");
    }

    public void onCommonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Running common setup for Modern Chickens");
        event.enqueueWork(ChickensDataLoader::bootstrap);
    }
}
