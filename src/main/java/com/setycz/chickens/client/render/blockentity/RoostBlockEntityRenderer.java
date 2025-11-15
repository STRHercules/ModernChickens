package com.setycz.chickens.client.render.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.setycz.chickens.block.RoostBlock;
import com.setycz.chickens.blockentity.AbstractChickenContainerBlockEntity.RenderData;
import com.setycz.chickens.blockentity.RoostBlockEntity;
import com.setycz.chickens.client.render.ChickenRenderHelper;
import com.setycz.chickens.entity.ChickensChicken;
import com.setycz.chickens.entity.Rooster;
import com.setycz.chickens.item.ChickenItemHelper;
import com.setycz.chickens.registry.ModEntityTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Renders the chicken sprite inside a roost by reusing the animated Chicken
 * entity. The pose and scaling are tuned to match the legacy Roost look.
 */
public class RoostBlockEntityRenderer implements BlockEntityRenderer<RoostBlockEntity> {
    private static final float BASE_SCALE = 0.9F;
    private static final float SCALE_PER_CHICKEN = 0.015F;
    private static final double FRONT_OFFSET = 0.04D;
    private static final double FLOOR_OFFSET = -0.11D;

    private final EntityRenderDispatcher dispatcher;
    private Rooster roosterPreview;

    public RoostBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.dispatcher = context.getEntityRenderer();
    }

    @Override
    public void render(RoostBlockEntity roost, float partialTicks, PoseStack poseStack, MultiBufferSource buffer,
            int packedLight, int packedOverlay) {
        ItemStack slotStack = roost.getItem(RoostBlockEntity.CHICKEN_SLOT);
        if (!slotStack.isEmpty() && ChickenItemHelper.isRooster(slotStack)) {
            renderRooster(roost, slotStack.getCount(), poseStack, buffer);
            return;
        }

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

        poseStack.pushPose();
        poseStack.translate(0.5D, FLOOR_OFFSET, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        poseStack.translate(0.0D, 0.0D, FRONT_OFFSET);

        float scale = Math.min(BASE_SCALE, BASE_SCALE + (data.count() - 1) * SCALE_PER_CHICKEN);
        poseStack.scale(scale, scale, scale);

        ChickenRenderHelper.resetPose(chicken);
        dispatcher.render(chicken, 0.0D, 0.0D, 0.0D, 180.0F, 0.0F, poseStack, buffer, LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }

    private void renderRooster(RoostBlockEntity roost, int count, PoseStack poseStack, MultiBufferSource buffer) {
        Level level = roost.getLevel();
        if (level == null) {
            return;
        }
        if (roosterPreview == null || roosterPreview.level() != level) {
            roosterPreview = ModEntityTypes.ROOSTER.get().create(level);
        }
        if (roosterPreview == null) {
            return;
        }

        BlockState state = roost.getBlockState();
        if (!(state.getBlock() instanceof RoostBlock)) {
            return;
        }
        Direction facing = state.getValue(RoostBlock.FACING);

        poseStack.pushPose();
        poseStack.translate(0.5D, FLOOR_OFFSET, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        poseStack.translate(0.0D, 0.0D, FRONT_OFFSET);

        float scale = Math.min(BASE_SCALE, BASE_SCALE + (count - 1) * SCALE_PER_CHICKEN);
        poseStack.scale(scale, scale, scale);

        dispatcher.render(roosterPreview, 0.0D, 0.0D, 0.0D, 180.0F, 0.0F, poseStack, buffer, LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(RoostBlockEntity blockEntity) {
        return true;
    }
}
