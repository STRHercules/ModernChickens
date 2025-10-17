package com.setycz.chickens.registry;

import com.mojang.serialization.MapCodec;
import com.setycz.chickens.ChickensMod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.registries.RegisterEvent.RegisterHelper;
import net.neoforged.neoforge.registries.NeoForgeRegistries.Keys;
import net.minecraft.resources.ResourceLocation;

/**
 * Registers the biome modifier codec and instance that handle natural chicken spawns.
 */
public final class ModBiomeModifiers {
    private static final ResourceLocation SPAWN_ID = ResourceLocation.fromNamespaceAndPath(ChickensMod.MOD_ID, "chickens_spawns");

    private ModBiomeModifiers() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(ModBiomeModifiers::onRegisterSerializers);
        modBus.addListener(ModBiomeModifiers::onRegisterModifiers);
    }

    private static void onRegisterSerializers(RegisterEvent event) {
        event.register(Keys.BIOME_MODIFIER_SERIALIZERS, helper -> registerSerializer(helper));
    }

    private static void registerSerializer(RegisterHelper<MapCodec<? extends BiomeModifier>> helper) {
        helper.register(SPAWN_ID, ChickensSpawnBiomeModifier.CODEC);
    }

    private static void onRegisterModifiers(RegisterEvent event) {
        event.register(Keys.BIOME_MODIFIERS, helper -> registerModifier(helper));
    }

    private static void registerModifier(RegisterHelper<BiomeModifier> helper) {
        helper.register(SPAWN_ID, ChickensSpawnBiomeModifier.INSTANCE);
    }
}
