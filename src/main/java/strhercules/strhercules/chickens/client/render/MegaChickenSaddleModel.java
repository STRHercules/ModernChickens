package strhercules.chickens.client.render;

import strhercules.chickens.ChickensMod;
import strhercules.chickens.entity.MegaChicken;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/** A small saddle overlay using the vanilla pig saddle texture. */
public final class MegaChickenSaddleModel extends EntityModel<MegaChicken> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(ChickensMod.MOD_ID, "mega_chicken_saddle"), "main");

    private final ModelPart saddle;

    public MegaChickenSaddleModel(ModelPart root) {
        this.saddle = root.getChild("saddle");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("saddle",
                CubeListBuilder.create().texOffs(28, 8)
                        .addBox(-5.0F, -8.0F, -4.0F, 10.0F, 16.0F, 8.0F, new CubeDeformation(0.5F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, Mth.HALF_PI, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(MegaChicken chicken, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        this.saddle.resetPose();
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                               int packedOverlay, int color) {
        this.saddle.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
