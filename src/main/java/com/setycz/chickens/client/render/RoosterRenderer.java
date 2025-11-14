package com.setycz.chickens.client.render;

import com.setycz.chickens.entity.Rooster;
import net.minecraft.client.renderer.entity.ChickenRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer for the rooster entity. It reuses the vanilla chicken model but
 * points at a dedicated rooster texture so resource packs (or ported assets)
 * can supply bespoke artwork.
 */
public class RoosterRenderer extends ChickenRenderer {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("chickens",
            "textures/entity/rooster.png");

    public RoosterRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(net.minecraft.world.entity.animal.Chicken chicken) {
        return chicken instanceof Rooster ? TEXTURE : super.getTextureLocation(chicken);
    }
}
