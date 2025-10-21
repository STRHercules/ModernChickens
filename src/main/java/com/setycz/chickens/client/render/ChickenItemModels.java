package com.setycz.chickens.client.render;

import com.setycz.chickens.ChickensMod;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ModelEvent;

/**
 * Utility that replaces the baked chicken item model with a version that
 * understands dynamically defined chickens. The helper stays free of event
 * annotations so callers can explicitly wire it into the client lifecycle and
 * avoid surprising class loads on the dedicated server.
 */
public final class ChickenItemModels {
    private static final ModelResourceLocation CHICKEN_MODEL = new ModelResourceLocation(
            ResourceLocation.fromNamespaceAndPath(ChickensMod.MOD_ID, "chicken"), "inventory");

    private ChickenItemModels() {
    }

    /**
     * Installs the {@link CustomChickenItemOverrides} wrapper when the base
     * chicken model finishes baking. If the vanilla model is missing we skip
     * the injection so the game keeps using whatever fallback NeoForge
     * provides.
     */
    public static void injectOverrides(ModelEvent.ModifyBakingResult event) {
        BakedModel existing = event.getModels().get(CHICKEN_MODEL);
        if (existing == null) {
            return;
        }
        ModelBakery bakery = event.getModelBakery();
        event.getModels().put(CHICKEN_MODEL, new ChickenItemOverridesModel(existing, bakery));
    }
}
