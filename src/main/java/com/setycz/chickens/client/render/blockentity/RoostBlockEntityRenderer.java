package com.setycz.chickens.client.render.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.setycz.chickens.block.RoostBlock;
import com.setycz.chickens.blockentity.AbstractChickenContainerBlockEntity.RenderData;
import com.setycz.chickens.blockentity.RoostBlockEntity;
import com.setycz.chickens.client.render.ChickenRenderHelper;
import com.setycz.chickens.entity.ChickensChicken;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Renders the chicken perched inside a roost. The legacy mod used a baked
 * model per chicken type; the NeoForge port mirrors that look by rendering the
 * actual Chickens entity with the correct texture and a gentle idle animation.
 */
public class RoostBlockEntityRenderer implements BlockEntityRenderer<RoostBlockEntity> {
    private final EntityRenderDispatcher dispatcher;

    public RoostBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.dispatcher = context.getEntityRenderer();
    }

    @Override
    public void render(RoostBlockEntity roost, float partialTicks, PoseStack poseStack, MultiBufferSource buffer,
            int packedLight, int packedOverlay) {
        RenderData data = roost.getRenderData(RoostBlockEntity.CHICKEN_SLOT);
        if (data == null || data.count() <= 0) {
            return;
        }
        ChickensChicken chicken = ChickenRenderHelper.getChicken(data.chicken().getId(), data.stats());
        if (chicken == null) {
            return;
        }
        BlockState state = roost.getBlockState();
        if (!(state.getBlock() instanceof RoostBlock)) {
            return;
        }
        Direction facing = state.getValue(RoostBlock.FACING);
        float bobTime = roost.getLevel() != null ? (roost.getLevel().getGameTime() + partialTicks) : 0.0F;
        float bobOffset = Mth.sin(bobTime * 0.1F) * 0.03F;

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.25D + bobOffset, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
        float scale = 0.4F + Math.min(data.count() - 1, 10) * 0.01F;
        poseStack.scale(scale, scale, scale);

        resetPose(chicken);
        dispatcher.render(chicken, 0.0D, 0.0D, 0.0D, 0.0F, partialTicks, poseStack, buffer, packedLight);
        poseStack.popPose();
    }

    static void resetPose(ChickensChicken chicken) {
        chicken.setYRot(0.0F);
        chicken.setXRot(0.0F);
        chicken.setYBodyRot(0.0F);
        chicken.yBodyRotO = 0.0F;
        chicken.setYHeadRot(0.0F);
        chicken.yHeadRotO = 0.0F;
        if (chicken.level() != null) {
            chicken.tickCount = (int) chicken.level().getGameTime();
        }
    }
}
