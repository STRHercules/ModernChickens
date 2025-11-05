package com.modernfluidcows;

import com.modernfluidcows.client.ModernFluidCowsClient;
import com.modernfluidcows.config.FCConfig;
import com.modernfluidcows.registry.FluidCowsRegistries;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for the ModernFluidCows NeoForge mod.
 *
 * <p>This class wires the NeoForge lifecycle events to the registration code contained in
 * {@link FluidCowsRegistries}. The original FluidCows mod used static Forge registries that were
 * populated during the pre-initialisation stage. NeoForge 1.21.1 relies on {@link
 * net.neoforged.neoforge.registries.DeferredRegister} to safely enqueue registration work. The
 * constructor below sets up those registers and ensures that the common setup hook remains
 * available for future porting work.</p>
 */
@Mod(ModernFluidCows.MOD_ID)
public final class ModernFluidCows {
    /** Identifier that keeps compatibility with legacy FluidCows saves and datapacks. */
    public static final String MOD_ID = "fluidcows";

    /** Shared logger so that every subsystem reports under the same tag. */
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public ModernFluidCows(final IEventBus modEventBus) {
        // Register every DeferredRegister container on the mod event bus.
        FluidCowsRegistries.bootstrap(modEventBus);
        modEventBus.addListener(FluidCowsRegistries::registerAttributes);
        modEventBus.addListener(FluidCowsRegistries::registerSpawnPlacements);
        modEventBus.addListener(FluidCowsRegistries::registerCapabilities);

        // Initialise the legacy JSON configuration so defaults are written before gameplay code
        // attempts to query spawn tables or cooldowns.
        FCConfig.setFile(FMLPaths.CONFIGDIR.get().resolve(MOD_ID + ".json"));

        if (FMLEnvironment.dist == Dist.CLIENT) {
            ModernFluidCowsClient.bootstrap(modEventBus);
        }

        // Hook the common setup event so that gameplay initialisation logic can be migrated later.
        modEventBus.addListener(this::onCommonSetup);
    }

    /**
     * Placeholder common setup handler. The legacy mod uses this phase to register GUIs and
     * networking. The real implementation will be ported in follow-up commits.
     */
    private void onCommonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("ModernFluidCows initialising base registries.");
        event.enqueueWork(() -> {
            FCConfig.load();
            LOGGER.info("Loaded {} configured fluids ({} total weight).", FCConfig.FLUIDS.size(), FCConfig.sumWeight);
        });
    }
}
