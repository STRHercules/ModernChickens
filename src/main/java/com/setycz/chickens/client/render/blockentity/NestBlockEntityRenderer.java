package com.setycz.chickens.client.render.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.setycz.chickens.block.NestBlock;
import com.setycz.chickens.blockentity.NestBlockEntity;
import com.setycz.chickens.entity.Rooster;
import com.setycz.chickens.registry.ModEntityTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Renders a rooster entity sitting inside the Nest. The pose and scaling are
 * tuned to match the Roost renderer so the two machines feel visually related.
 */
public class NestBlockEntityRenderer implements BlockEntityRenderer<NestBlockEntity> {
    private static final float BASE_SCALE = 0.9F;
    private static final float SCALE_PER_ROOSTER = 0.015F;
    private static final double FRONT_OFFSET = 0.04D;
    private static final double FLOOR_OFFSET = -0.11D;

    private final EntityRenderDispatcher dispatcher;
    private Rooster roosterPreview;

    public NestBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.dispatcher = context.getEntityRenderer();
    }

    @Override
    public void render(NestBlockEntity nest, float partialTicks, PoseStack poseStack, MultiBufferSource buffer,
            int packedLight, int packedOverlay) {
        int count = nest.getRoosterCount();
        if (count <= 0) {
            return;
        }
        Level level = nest.getLevel();
        if (level == null) {
            return;
        }
        if (roosterPreview == null || roosterPreview.level() != level) {
            roosterPreview = ModEntityTypes.ROOSTER.get().create(level);
        }
        if (roosterPreview == null) {
            return;
        }

        BlockState state = nest.getBlockState();
        if (!(state.getBlock() instanceof NestBlock)) {
            return;
        }
        // Treat the nest's FACING as the direction the opening points toward.
        // The renderer works with the opposite (back) direction so we can
        // reuse the same transform pattern as the Roost renderer.
        Direction facing = state.getValue(NestBlock.FACING).getOpposite();

        poseStack.pushPose();
        poseStack.translate(0.5D, FLOOR_OFFSET, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        poseStack.translate(0.0D, 0.0D, FRONT_OFFSET);

        float scale = Math.min(BASE_SCALE, BASE_SCALE + (count - 1) * SCALE_PER_ROOSTER);
        poseStack.scale(scale, scale, scale);

        // Mirror the Roost renderer's yaw so the rooster always looks out of
        // the opening regardless of block orientation.
        dispatcher.render(roosterPreview, 0.0D, 0.0D, 0.0D, 180.0F, 0.0F, poseStack, buffer, LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(NestBlockEntity blockEntity) {
        return true;
    }
}
