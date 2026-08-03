package strhercules.chickens.client.render;

import strhercules.chickens.entity.MegaChicken;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Renderer for the supplied 3x Mega Chicken model and its tame-state equipment. */
public final class MegaChickenRenderer extends MobRenderer<MegaChicken, MegaChickenModel> {
    private static final ResourceLocation UNTAMED_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "chickens", "textures/entity/megachicken/mega_chicken.png");
    private static final ResourceLocation TAMED_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "chickens", "textures/entity/megachicken/mega_chicken_tamed.png");

    public MegaChickenRenderer(EntityRendererProvider.Context context) {
        super(context, new MegaChickenModel(context.bakeLayer(MegaChickenModel.LAYER_LOCATION)), 0.9F);
    }

    @Override
    public ResourceLocation getTextureLocation(MegaChicken chicken) {
        return chicken.isTamed() ? TAMED_TEXTURE : UNTAMED_TEXTURE;
    }

    @Override
    protected float getBob(MegaChicken chicken, float partialTicks) {
        float flap = net.minecraft.util.Mth.lerp(partialTicks, chicken.oFlap, chicken.flap);
        float speed = net.minecraft.util.Mth.lerp(partialTicks, chicken.oFlapSpeed, chicken.flapSpeed);
        return (net.minecraft.util.Mth.sin(flap) + 1.0F) * speed;
    }
}
