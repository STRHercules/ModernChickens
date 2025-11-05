package com.modernfluidcows.client.render;

import com.modernfluidcows.entity.FluidCow;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.CowModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

/**
 * Client renderer that tints the vanilla cow model with the assigned fluid texture.
 *
 * <p>The legacy mod relied on a bespoke OpenGL texture matrix to map block atlas sprites across the
 * cow model. Modern Minecraft exposes sprite wrapping helpers, so the ported renderer composes a
 * dedicated render layer that reuses the baked cow model while colouring it with fluid data.</p>
 */
public class FluidCowRenderer extends MobRenderer<FluidCow, CowModel<FluidCow>> {
    private static final ResourceLocation BASE_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/cow/cow.png");

    public FluidCowRenderer(final EntityRendererProvider.Context context) {
        super(context, new CowModel<>(context.bakeLayer(ModelLayers.COW)), 0.7F);
        // Attach a fluid overlay layer so the entity reflects its configured fluid client-side.
        addLayer(new FluidOverlayLayer(this, new CowModel<>(context.bakeLayer(ModelLayers.COW))));
    }

    @Override
    public ResourceLocation getTextureLocation(final FluidCow entity) {
        return BASE_TEXTURE;
    }

    /**
     * Render layer that replays the cow model using the bound fluid's still texture and tint.
     */
    private static final class FluidOverlayLayer extends RenderLayer<FluidCow, CowModel<FluidCow>> {
        private final CowModel<FluidCow> overlayModel;

        FluidOverlayLayer(final FluidCowRenderer parent, final CowModel<FluidCow> overlayModel) {
            super(parent);
            this.overlayModel = overlayModel;
        }

        @Override
        public void render(
                final PoseStack poseStack,
                final MultiBufferSource buffer,
                final int packedLight,
                final FluidCow cow,
                final float limbSwing,
                final float limbSwingAmount,
                final float partialTick,
                final float ageInTicks,
                final float netHeadYaw,
                final float headPitch) {
            Fluid fluid = cow.getFluid();
            if (fluid == null) {
                return;
            }

            IClientFluidTypeExtensions clientExtensions = IClientFluidTypeExtensions.of(fluid);
            FluidState state = fluid.defaultFluidState();
            ResourceLocation texture = clientExtensions.getStillTexture(state, cow.level(), cow.blockPosition());
            if (texture == null) {
                return;
            }

            TextureAtlasSprite sprite =
                    Minecraft.getInstance().getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS).getSprite(texture);
            if (sprite == null) {
                return;
            }

            int tint = clientExtensions.getTintColor(state, cow.level(), cow.blockPosition());
            float red = ((tint >> 16) & 0xFF) / 255.0F;
            float green = ((tint >> 8) & 0xFF) / 255.0F;
            float blue = (tint & 0xFF) / 255.0F;
            float alpha = 0.6F;

            // Keep the overlay model synchronised with the parent renderer's limb and pose data.
            CowModel<FluidCow> parentModel = getParentModel();
            parentModel.copyPropertiesTo(overlayModel);
            overlayModel.prepareMobModel(cow, limbSwing, limbSwingAmount, partialTick);
            overlayModel.setupAnim(cow, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

            VertexConsumer vertexConsumer =
                    sprite.wrap(buffer.getBuffer(RenderType.entityTranslucentCull(TextureAtlas.LOCATION_BLOCKS)));
            int packedColor = FastColor.ARGB32.color(
                    Math.round(alpha * 255.0F), Math.round(red * 255.0F), Math.round(green * 255.0F), Math.round(blue * 255.0F));
            overlayModel.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, packedColor);
        }
    }
}
