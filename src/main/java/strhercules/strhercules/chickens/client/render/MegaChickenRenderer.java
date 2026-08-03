package strhercules.chickens.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import strhercules.chickens.entity.MegaChicken;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Renderer for the supplied 3x Mega Chicken model and its native equipment. */
public final class MegaChickenRenderer extends MobRenderer<MegaChicken, MegaChickenModel> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "chickens", "textures/entity/mega_chicken.png");
    private static final ResourceLocation SADDLE_TEXTURE = ResourceLocation.withDefaultNamespace(
            "textures/entity/pig/pig_saddle.png");

    public MegaChickenRenderer(EntityRendererProvider.Context context) {
        super(context, new MegaChickenModel(context.bakeLayer(MegaChickenModel.LAYER_LOCATION)), 0.9F);
        this.addLayer(new SaddleRenderLayer(this,
                new MegaChickenSaddleModel(context.bakeLayer(MegaChickenSaddleModel.LAYER_LOCATION))));
        this.addLayer(new ChestLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(MegaChicken chicken) {
        return TEXTURE;
    }

    @Override
    protected float getBob(MegaChicken chicken, float partialTicks) {
        float flap = net.minecraft.util.Mth.lerp(partialTicks, chicken.oFlap, chicken.flap);
        float speed = net.minecraft.util.Mth.lerp(partialTicks, chicken.oFlapSpeed, chicken.flapSpeed);
        return (net.minecraft.util.Mth.sin(flap) + 1.0F) * speed;
    }

    private static final class SaddleRenderLayer extends RenderLayer<MegaChicken, MegaChickenModel> {
        private final MegaChickenSaddleModel model;

        private SaddleRenderLayer(MegaChickenRenderer renderer, MegaChickenSaddleModel model) {
            super(renderer);
            this.model = model;
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                           MegaChicken chicken, float limbSwing, float limbSwingAmount,
                           float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            if (!chicken.isSaddled()) {
                return;
            }
            this.model.setupAnim(chicken, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            VertexConsumer saddleBuffer = buffer.getBuffer(RenderType.entityCutoutNoCull(SADDLE_TEXTURE));
            this.model.renderToBuffer(poseStack, saddleBuffer, packedLight,
                    OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        }
    }

    private static final class ChestLayer extends RenderLayer<MegaChicken, MegaChickenModel> {
        private final ModelPart lid;
        private final ModelPart bottom;
        private final ModelPart lock;

        private ChestLayer(MegaChickenRenderer renderer) {
            super(renderer);
            ModelPart root = ChestRenderer.createSingleBodyLayer().bakeRoot();
            this.lid = root.getChild("lid");
            this.bottom = root.getChild("bottom");
            this.lock = root.getChild("lock");
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                           MegaChicken chicken, float limbSwing, float limbSwingAmount,
                           float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            if (!chicken.hasChest()) {
                return;
            }
            VertexConsumer chestBuffer = Sheets.CHEST_LOCATION.buffer(buffer, RenderType::entityCutout);
            renderChest(poseStack, chestBuffer, packedLight, -21.0F);
            renderChest(poseStack, chestBuffer, packedLight, 6.0F);
        }

        private void renderChest(PoseStack poseStack, VertexConsumer buffer, int packedLight, float x) {
            poseStack.pushPose();
            poseStack.translate(x / 16.0F, -5.0F / 16.0F, -7.0F / 16.0F);
            poseStack.scale(0.5F, 0.5F, 0.5F);
            this.lid.xRot = 0.0F;
            this.lock.xRot = 0.0F;
            this.lid.render(poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
            this.lock.render(poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
            this.bottom.render(poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }
    }
}
