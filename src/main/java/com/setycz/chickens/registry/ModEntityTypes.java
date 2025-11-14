package com.setycz.chickens.registry;

import com.setycz.chickens.ChickensMod;
import com.setycz.chickens.entity.ChickensChicken;
import com.setycz.chickens.entity.ColoredEgg;
import com.setycz.chickens.entity.Rooster;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Hosts entity type registrations for the modernised Chickens mod. Keeping
 * entity setup in its own class keeps {@link ModRegistry} focused on items and
 * blocks while exposing convenient hooks for attribute and spawn registration.
 */
public final class ModEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, ChickensMod.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<ChickensChicken>> CHICKENS_CHICKEN = ENTITY_TYPES.register("chicken",
            () -> EntityType.Builder.<ChickensChicken>of(ChickensChicken::new, MobCategory.CREATURE)
                    .sized(0.4F, 0.7F)
                    .clientTrackingRange(10)
                    .build(ResourceLocation.fromNamespaceAndPath(ChickensMod.MOD_ID, "chicken").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<Rooster>> ROOSTER = ENTITY_TYPES.register("rooster",
            () -> EntityType.Builder.<Rooster>of(Rooster::new, MobCategory.CREATURE)
                    .sized(0.4F, 0.7F)
                    .clientTrackingRange(10)
                    .build(ResourceLocation.fromNamespaceAndPath(ChickensMod.MOD_ID, "rooster").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<ColoredEgg>> COLORED_EGG = ENTITY_TYPES.register("colored_egg",
            () -> EntityType.Builder.<ColoredEgg>of(ColoredEgg::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build(ResourceLocation.fromNamespaceAndPath(ChickensMod.MOD_ID, "colored_egg").toString()));

    private ModEntityTypes() {
    }

    public static void init(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
        modBus.addListener(ModEntityTypes::onEntityAttributeCreation);
    }

    private static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(CHICKENS_CHICKEN.get(), ChickensChicken.createAttributes().build());
        event.put(ROOSTER.get(), Rooster.createAttributes().build());
    }

}
