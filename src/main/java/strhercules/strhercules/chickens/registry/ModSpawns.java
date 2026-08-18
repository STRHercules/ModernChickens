package strhercules.chickens.registry;

import strhercules.chickens.entity.ChickensChicken;
import strhercules.chickens.entity.MegaChicken;
import strhercules.chickens.entity.Rooster;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * Registers spawn placement rules for the custom chicken entity while reusing the
 * vanilla chicken's placement type and heightmap. This keeps natural spawning
 * aligned with Mojang's defaults but lets the mod tighten the biome restrictions
 * through its registry and configuration.
 */
public final class ModSpawns {
    private ModSpawns() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(ModSpawns::onCommonSetup);
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            SpawnPlacements.register(ModEntityTypes.CHICKENS_CHICKEN.get(),
                    SpawnPlacements.getPlacementType(EntityType.CHICKEN),
                    SpawnPlacements.getHeightmapType(EntityType.CHICKEN),
                    ChickensChicken::checkSpawnRules);
            SpawnPlacements.register(ModEntityTypes.ROOSTER.get(),
                    SpawnPlacements.getPlacementType(EntityType.CHICKEN),
                    SpawnPlacements.getHeightmapType(EntityType.CHICKEN),
                    Rooster::checkSpawnRules);
            SpawnPlacements.register(ModEntityTypes.MEGA_CHICKEN.get(),
                    SpawnPlacements.getPlacementType(EntityType.CHICKEN),
                    SpawnPlacements.getHeightmapType(EntityType.CHICKEN),
                    MegaChicken::checkSpawnRules);
        });
    }
}
