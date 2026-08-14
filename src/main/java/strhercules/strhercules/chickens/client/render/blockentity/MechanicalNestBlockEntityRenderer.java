package strhercules.chickens.client.render.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import strhercules.chickens.block.MechanicalNestBlock;
import strhercules.chickens.blockentity.MechanicalNestBlockEntity;
import strhercules.chickens.entity.Rooster;
import strhercules.chickens.registry.ModEntityTypes;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

public final class MechanicalNestBlockEntityRenderer implements BlockEntityRenderer<MechanicalNestBlockEntity> {
    private final EntityRenderDispatcher dispatcher;
    private Rooster preview;

    public MechanicalNestBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        dispatcher = context.getEntityRenderer();
    }

    @Override
    public void render(MechanicalNestBlockEntity nest, float partialTicks, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (nest.getRoosterCount() <= 0 || nest.getLevel() == null) {
            return;
        }
        Level level = nest.getLevel();
        if (preview == null || preview.level() != level) {
            preview = ModEntityTypes.ROOSTER.get().create(level);
        }
        if (preview == null || !(nest.getBlockState().getBlock() instanceof MechanicalNestBlock)) {
            return;
        }
        preview.setRobotRooster(true);
        Direction facing = nest.getBlockState().getValue(MechanicalNestBlock.FACING);
        poseStack.pushPose();
        poseStack.translate(0.5D, -0.08D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        poseStack.translate(0.0D, 0.0D, 0.10D);
        poseStack.scale(0.78F, 0.78F, 0.78F);
        dispatcher.render(preview, 0.0D, 0.0D, 0.0D, 180.0F, 0.0F, poseStack, buffer,
                LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(MechanicalNestBlockEntity blockEntity) {
        return true;
    }
}
