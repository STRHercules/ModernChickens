package com.setycz.chickens.client.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.setycz.chickens.ChickensMod;
import com.setycz.chickens.blockentity.CollectorBlockEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Draws wireframe cubes around each collector when the debug overlay is
 * enabled. The overlay renders on the level render event so builders can check
 * coverage without opening each block entity.
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = ChickensMod.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class CollectorDebugOverlay {
    private static volatile boolean enabled;

    private CollectorDebugOverlay() {
    }

    /**
     * Updates the overlay state when the server command toggles the range
     * visualisation for the local player.
     */
    public static void setEnabled(boolean enabledIn) {
        enabled = enabledIn;
    }

    /**
     * Handles the level render stage and draws the cube volume for every
     * collector in view when the overlay is active.
     */
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (!enabled || event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();

        Camera camera = event.getCamera();
        Vec3 cameraPosition = camera.getPosition();
        poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);

        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.lines());
        int range = CollectorBlockEntity.resolveConfiguredScanRange();
        ClientChunkCache chunkSource = level.getChunkSource();
        BlockPos cameraBlockPos = BlockPos.containing(cameraPosition);
        int chunkRadius = minecraft.options.getEffectiveRenderDistance();

        for (int chunkX = -chunkRadius; chunkX <= chunkRadius; chunkX++) {
            for (int chunkZ = -chunkRadius; chunkZ <= chunkRadius; chunkZ++) {
                int targetX = (cameraBlockPos.getX() >> 4) + chunkX;
                int targetZ = (cameraBlockPos.getZ() >> 4) + chunkZ;
                LevelChunk chunk = chunkSource.getChunk(targetX, targetZ, ChunkStatus.FULL, false);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (blockEntity instanceof CollectorBlockEntity collector) {
                        BlockPos pos = collector.getBlockPos();
                        AABB bounds = new AABB(pos.getX() - range, pos.getY() - range, pos.getZ() - range,
                                pos.getX() + range + 1, pos.getY() + range + 1, pos.getZ() + range + 1);
                        LevelRenderer.renderLineBox(poseStack, vertexConsumer, bounds, 1.0F, 0.6F, 0.0F, 0.4F);
                    }
                }
            }
        }

        poseStack.popPose();
        bufferSource.endBatch(RenderType.lines());
    }
}
