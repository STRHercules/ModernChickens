package strhercules.chickens.client.render.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import strhercules.chickens.blockentity.AvianFluxConverterBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/** Renders the active flux converter's stored RF as a red energy tank fill. */
public final class AvianFluxConverterBlockEntityRenderer
        extends AvianMachineBlockEntityRenderer<AvianFluxConverterBlockEntity> {
    private static final ResourceLocation ENERGY_TEXTURE =
            ResourceLocation.withDefaultNamespace("block/white_concrete");
    private static final int ENERGY_TINT = 0xFF3C3C;

    public AvianFluxConverterBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderStoredContents(AvianFluxConverterBlockEntity machine, Level level, TextureAtlas atlas,
            VertexConsumer consumer, PoseStack.Pose pose, int packedLight, int packedOverlay) {
        TankContents contents = machine.getEnergyStored() <= 0
                ? null
                : new TankContents(ENERGY_TEXTURE, ENERGY_TINT,
                        machine.getEnergyStored(), machine.getEnergyCapacity());
        renderTankContents(atlas, consumer, pose, contents, TANK_MIN, TANK_MAX, packedLight, packedOverlay);
    }
}
