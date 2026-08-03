package strhercules.chickens.client.render;

import strhercules.chickens.ChickensMod;
import strhercules.chickens.entity.MegaChicken;
import net.minecraft.client.model.AgeableListModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.List;

/** 3x Java model conversion of vanilla chicken geometry and UVs. */
public final class MegaChickenModel extends AgeableListModel<MegaChicken> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(ChickensMod.MOD_ID, "mega_chicken"), "main");

    private final ModelPart head;
    private final ModelPart beak;
    private final ModelPart redThing;
    private final ModelPart body;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart rightWing;
    private final ModelPart leftWing;
    private final List<ModelPart> headParts;
    private final List<ModelPart> bodyParts;
    private final List<ModelPart> allParts;

    public MegaChickenModel(ModelPart root) {
        this.head = root.getChild("head");
        this.beak = root.getChild("beak");
        this.redThing = root.getChild("red_thing");
        this.body = root.getChild("body");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
        this.rightWing = root.getChild("right_wing");
        this.leftWing = root.getChild("left_wing");
        this.headParts = List.of(this.head, this.beak, this.redThing);
        this.bodyParts = List.of(this.body, this.rightLeg, this.leftLeg, this.rightWing, this.leftWing);
        this.allParts = List.of(this.head, this.beak, this.redThing, this.body,
                this.rightLeg, this.leftLeg, this.rightWing, this.leftWing);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // The supplied Blockbench model is 3x vanilla. The 48-pixel Y offset
        // aligns its 72-pixel feet with the standard renderer's 24-pixel floor.
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-6.0F, -18.0F, -6.0F, 12.0F, 18.0F, 9.0F),
                PartPose.offset(0.0F, -3.0F, -12.0F));
        root.addOrReplaceChild("beak",
                CubeListBuilder.create().texOffs(42, 0)
                        .addBox(-6.0F, -12.0F, -12.0F, 12.0F, 6.0F, 6.0F),
                PartPose.offset(0.0F, -3.0F, -12.0F));
        root.addOrReplaceChild("red_thing",
                CubeListBuilder.create().texOffs(42, 12)
                        .addBox(-3.0F, -6.0F, -9.0F, 6.0F, 6.0F, 6.0F),
                PartPose.offset(0.0F, -3.0F, -12.0F));
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 27)
                        .addBox(-9.0F, -12.0F, -9.0F, 18.0F, 24.0F, 18.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        CubeListBuilder leg = CubeListBuilder.create().texOffs(78, 0)
                .addBox(-3.0F, 0.0F, -9.0F, 9.0F, 15.0F, 9.0F);
        root.addOrReplaceChild("right_leg", leg, PartPose.offset(-6.0F, 9.0F, 3.0F));
        root.addOrReplaceChild("left_leg", leg, PartPose.offset(3.0F, 9.0F, 3.0F));

        root.addOrReplaceChild("right_wing",
                CubeListBuilder.create().texOffs(72, 39)
                        .addBox(0.0F, 0.0F, -9.0F, 3.0F, 12.0F, 18.0F),
                PartPose.offset(-12.0F, -9.0F, 0.0F));
        root.addOrReplaceChild("left_wing",
                CubeListBuilder.create().texOffs(72, 39)
                        .addBox(-3.0F, 0.0F, -9.0F, 3.0F, 12.0F, 18.0F),
                PartPose.offset(12.0F, -9.0F, 0.0F));

        return LayerDefinition.create(mesh, 192, 96);
    }

    @Override
    protected Iterable<ModelPart> headParts() {
        return this.headParts;
    }

    @Override
    protected Iterable<ModelPart> bodyParts() {
        return this.bodyParts;
    }

    @Override
    public void setupAnim(MegaChicken chicken, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        this.allParts.forEach(ModelPart::resetPose);

        this.head.xRot = headPitch * Mth.DEG_TO_RAD;
        this.head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        this.beak.xRot = this.head.xRot;
        this.beak.yRot = this.head.yRot;
        this.redThing.xRot = this.head.xRot;
        this.redThing.yRot = this.head.yRot;

        this.rightLeg.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        this.leftLeg.xRot = Mth.cos(limbSwing * 0.6662F + Mth.PI) * 1.4F * limbSwingAmount;
        this.rightWing.zRot = ageInTicks;
        this.leftWing.zRot = -ageInTicks;
    }
}
