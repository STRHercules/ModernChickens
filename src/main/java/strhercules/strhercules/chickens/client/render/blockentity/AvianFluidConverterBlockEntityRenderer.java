package strhercules.chickens.client.render.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import strhercules.chickens.blockentity.AvianFluidConverterBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.level.Level;

/** Renders the active fluid converter's stored fluid as a visible tank fill. */
public final class AvianFluidConverterBlockEntityRenderer
        extends AvianMachineBlockEntityRenderer<AvianFluidConverterBlockEntity> {
    public AvianFluidConverterBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderStoredContents(AvianFluidConverterBlockEntity machine, Level level, TextureAtlas atlas,
            VertexConsumer consumer, PoseStack.Pose pose, int packedLight, int packedOverlay) {
        TankContents contents = resolveFluidContents(machine.getFluid(), machine.getFluidAmount(),
                machine.getTankCapacity(), level, machine);
        renderTankContents(atlas, consumer, pose, contents, TANK_MIN, TANK_MAX, packedLight, packedOverlay);
    }
}
