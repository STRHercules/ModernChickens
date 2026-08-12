package strhercules.chickens.client.render.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import strhercules.chickens.ChemicalEggRegistryItem;
import strhercules.chickens.blockentity.AvianChemicalConverterBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.level.Level;

/** Renders the active chemical converter's stored chemical as a visible tank fill. */
public final class AvianChemicalConverterBlockEntityRenderer
        extends AvianMachineBlockEntityRenderer<AvianChemicalConverterBlockEntity> {
    public AvianChemicalConverterBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderStoredContents(AvianChemicalConverterBlockEntity machine, Level level, TextureAtlas atlas,
            VertexConsumer consumer, PoseStack.Pose pose, int packedLight, int packedOverlay) {
        ChemicalEggRegistryItem chemical = machine.getStoredEntry();
        TankContents contents = chemical == null
                ? null
                : new TankContents(chemical.getTexture(), chemical.getEggColor(),
                        machine.getChemicalAmount(), machine.getTankCapacity());
        renderTankContents(atlas, consumer, pose, contents, TANK_MIN, TANK_MAX, packedLight, packedOverlay);
    }
}
