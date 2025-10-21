package com.setycz.chickens.client.render;

import com.setycz.chickens.ChickensMod;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

/**
 * Injects a runtime override into the chicken item model so that custom
 * chickens defined via configuration can point at their own sprite without
 * requiring a bespoke resource pack. The wrapper delegates to the baked
 * vanilla overrides first and only falls back to the dynamic lookup when no
 * JSON override matches the chicken id.
 */
@EventBusSubscriber(modid = ChickensMod.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ChickenItemModels {
    private static final ModelResourceLocation CHICKEN_MODEL = new ModelResourceLocation(
            ResourceLocation.fromNamespaceAndPath(ChickensMod.MOD_ID, "chicken"), "inventory");

    private ChickenItemModels() {
    }

    @SubscribeEvent
    public static void onModifyModels(ModelEvent.ModifyBakingResult event) {
        BakedModel existing = event.getModels().get(CHICKEN_MODEL);
        if (existing == null) {
            return;
        }
        ModelBakery bakery = event.getModelBakery();
        event.getModels().put(CHICKEN_MODEL, new ChickenItemOverridesModel(existing, bakery));
    }
}
