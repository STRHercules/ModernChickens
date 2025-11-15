package com.setycz.chickens.client.render;

import com.setycz.chickens.entity.Rooster;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Renderer for the rooster entity. Uses the ported {@link RoosterModel} so the
 * legacy Hatchery rooster texture maps correctly onto the geometry.
 */
public class RoosterRenderer extends MobRenderer<Rooster, RoosterModel> {
    private static final ResourceLocation ROOSTER_TEXTURE = ResourceLocation.fromNamespaceAndPath("chickens",
            "textures/entity/rooster.png");

    public RoosterRenderer(EntityRendererProvider.Context context) {
        super(context, new RoosterModel(context.bakeLayer(RoosterModel.LAYER_LOCATION)), 0.3F);
    }

    @Override
    public ResourceLocation getTextureLocation(Rooster rooster) {
        return ROOSTER_TEXTURE;
    }

    @Override
    protected float getBob(Rooster rooster, float partialTicks) {
        // Mirror the vanilla ChickenRenderer bobbing logic so the rooster's
        // flap animation feels identical to standard chickens.
        float flap = Mth.lerp(partialTicks, rooster.oFlap, rooster.flap);
        float speed = Mth.lerp(partialTicks, rooster.oFlapSpeed, rooster.flapSpeed);
        return (Mth.sin(flap) + 1.0F) * speed;
    }
}
