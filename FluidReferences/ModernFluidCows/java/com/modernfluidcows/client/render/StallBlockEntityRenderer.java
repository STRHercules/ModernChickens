package com.modernfluidcows.client.render;

import com.modernfluidcows.block.StallBlock;
import com.modernfluidcows.blockentity.StallBlockEntity;
import com.modernfluidcows.entity.FluidCow;
import com.modernfluidcows.registry.FluidCowsRegistries;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block entity renderer that rehydrates the stored cow data and displays a static cow model inside
 * the stall. This mirrors the legacy mod which showed the captured cow rather than an empty shell.
 */
public class StallBlockEntityRenderer implements BlockEntityRenderer<StallBlockEntity> {
    private FluidCow cachedCow;
    private CompoundTag cachedData;

    public StallBlockEntityRenderer(final BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
            final StallBlockEntity stall,
            final float partialTick,
            final PoseStack poseStack,
            final MultiBufferSource buffer,
            final int packedLight,
            final int packedOverlay) {
        if (!stall.hasCow()) {
            // Reset cached state so removing the cow immediately hides the ghost entity.
            cachedCow = null;
            cachedData = null;
            return;
        }

        Level level = stall.getLevel();
        if (!(level instanceof ClientLevel clientLevel)) {
            return;
        }

        // Build or refresh the cached cow so we only pay the registry lookup when needed.
        if (cachedCow == null || cachedCow.level() != clientLevel) {
            cachedCow = FluidCowsRegistries.FLUID_COW.get().create(clientLevel);
            if (cachedCow != null) {
                cachedCow.setNoAi(true); // Keep the preview cow stationary inside the stall.
            }
            cachedData = null;
        }
        if (cachedCow == null) {
            return;
        }

        CompoundTag halterData = stall.getStoredCowData().orElse(null);
        if (halterData == null) {
            return;
        }
        if (cachedData == null || !cachedData.equals(halterData)) {
            // Replay the halter payload so the preview matches the server-owned cow exactly.
            cachedCow.readHalterData(halterData);
            cachedCow.setHealth(cachedCow.getMaxHealth());
            cachedCow.setYRot(0.0F);
            cachedCow.setBaby(false);
            cachedData = halterData.copy();
        }

        // Ticking the cached cow keeps tail/head idle animations alive without AI.
        cachedCow.tickCount = (int) level.getGameTime();

        BlockState state = stall.getBlockState();
        Direction facing = state.hasProperty(StallBlock.FACING)
                ? state.getValue(StallBlock.FACING)
                : Direction.NORTH;

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.2D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        poseStack.scale(0.75F, 0.75F, 0.75F);

        Minecraft.getInstance()
                .getEntityRenderDispatcher()
                .render(cachedCow, 0.0D, 0.0D, 0.0D, 0.0F, partialTick, poseStack, buffer, packedLight);
        poseStack.popPose();
    }
}
