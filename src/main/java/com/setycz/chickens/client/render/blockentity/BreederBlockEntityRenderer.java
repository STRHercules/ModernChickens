package com.setycz.chickens.client.render.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.setycz.chickens.block.BreederBlock;
import com.setycz.chickens.blockentity.AbstractChickenContainerBlockEntity.RenderData;
import com.setycz.chickens.blockentity.BreederBlockEntity;
import com.setycz.chickens.client.render.ChickenRenderHelper;
import com.setycz.chickens.entity.ChickensChicken;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Visualises the breeder's parent chickens and seed pile. Rendering uses the
 * same animated chickens as the roost renderer so both blocks retain the
 * distinct look of the original Roost mod.
 */
public class BreederBlockEntityRenderer implements BlockEntityRenderer<BreederBlockEntity> {
    private final EntityRenderDispatcher dispatcher;
    private final ItemRenderer itemRenderer;

    public BreederBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.dispatcher = context.getEntityRenderer();
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(BreederBlockEntity breeder, float partialTicks, PoseStack poseStack, MultiBufferSource buffer,
            int packedLight, int packedOverlay) {
        BlockState state = breeder.getBlockState();
        if (!(state.getBlock() instanceof BreederBlock)) {
            return;
        }
        Direction facing = state.getValue(BreederBlock.FACING);

        renderChicken(breeder, BreederBlockEntity.LEFT_CHICKEN_SLOT, -0.25D, facing, partialTicks, poseStack, buffer,
                packedLight);
        renderChicken(breeder, BreederBlockEntity.RIGHT_CHICKEN_SLOT, 0.25D, facing, partialTicks, poseStack, buffer,
                packedLight);
        renderSeeds(breeder, facing, poseStack, buffer, packedLight, packedOverlay);
    }

    private void renderChicken(BreederBlockEntity breeder, int slot, double xOffset, Direction facing, float partialTicks,
            PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        RenderData data = breeder.getRenderData(slot);
        if (data == null) {
            return;
        }
        ChickensChicken chicken = ChickenRenderHelper.getChicken(data.chicken().getId(), data.stats());
        if (chicken == null) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.25D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
        poseStack.translate(xOffset, 0.0D, 0.1D);
        poseStack.scale(0.35F, 0.35F, 0.35F);

        ChickenRenderHelper.resetPose(chicken);
        dispatcher.render(chicken, 0.0D, 0.0D, 0.0D, 0.0F, partialTicks, poseStack, buffer, packedLight);
        poseStack.popPose();
    }

    private void renderSeeds(BreederBlockEntity breeder, Direction facing, PoseStack poseStack, MultiBufferSource buffer,
            int packedLight, int packedOverlay) {
        ItemStack seeds = breeder.getItem(BreederBlockEntity.SEED_SLOT);
        if (seeds.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.2D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
        poseStack.translate(0.0D, 0.1D, -0.1D);
        poseStack.scale(0.5F, 0.5F, 0.5F);
        itemRenderer.renderStatic(seeds, ItemDisplayContext.GROUND, packedLight, packedOverlay, poseStack, buffer,
                breeder.getLevel(), 0);
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(BreederBlockEntity blockEntity) {
        return true;
    }
}
