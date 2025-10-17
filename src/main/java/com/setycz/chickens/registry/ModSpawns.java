package com.setycz.chickens.registry;

import com.setycz.chickens.entity.ChickensChicken;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacements;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

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
        modBus.addListener(ModSpawns::onRegisterSpawnPlacements);
    }

    private static void onRegisterSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(ModEntityTypes.CHICKENS_CHICKEN.get(),
                SpawnPlacements.getPlacementType(EntityType.CHICKEN),
                SpawnPlacements.getHeightmapType(EntityType.CHICKEN),
                ChickensChicken::checkSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
