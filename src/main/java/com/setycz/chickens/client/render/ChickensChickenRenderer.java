package com.setycz.chickens.client.render;

import com.setycz.chickens.ChickensRegistry;
import com.setycz.chickens.ChickensRegistryItem;
import com.setycz.chickens.entity.ChickensChicken;
import net.minecraft.client.renderer.entity.ChickenRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Chicken;

/**
 * Renderer that mirrors the vanilla chicken visuals but swaps the texture based
 * on the chicken registry entry. This restores the per-breed skins from the
 * original mod.
 */
public class ChickensChickenRenderer extends ChickenRenderer {
    public ChickensChickenRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(Chicken chicken) {
        if (chicken instanceof ChickensChicken modChicken) {
            ChickensRegistryItem description = ChickensRegistry.getByType(modChicken.getChickenType());
            if (description != null) {
                return description.getTexture();
            }
        }
        return super.getTextureLocation(chicken);
    }
}
