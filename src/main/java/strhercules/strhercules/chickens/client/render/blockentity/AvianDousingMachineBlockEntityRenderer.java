package strhercules.chickens.client.render.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import strhercules.chickens.ChemicalEggRegistry;
import strhercules.chickens.ChemicalEggRegistryItem;
import strhercules.chickens.GasEggRegistry;
import strhercules.chickens.blockentity.AvianDousingMachineBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/** Renders the active dousing machine's stored reagent as two visible tanks. */
public final class AvianDousingMachineBlockEntityRenderer
        extends AvianMachineBlockEntityRenderer<AvianDousingMachineBlockEntity> {
    private static final float TANK_MID = 8.0F / 16.0F;

    public AvianDousingMachineBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected boolean hasDivider() {
        return true;
    }

    @Override
    protected void renderStoredContents(AvianDousingMachineBlockEntity machine, Level level, TextureAtlas atlas,
            VertexConsumer consumer, PoseStack.Pose pose, int packedLight, int packedOverlay) {
        TankContents chemical = resolveChemicalContents(machine);
        TankContents fluid = resolveFluidContents(machine.getFluid(), machine.getLiquidAmount(),
                machine.getLiquidCapacity(), level, machine);
        renderTankContents(atlas, consumer, pose, chemical, TANK_MIN, TANK_MID, packedLight, packedOverlay);
        renderTankContents(atlas, consumer, pose, fluid, TANK_MID, TANK_MAX, packedLight, packedOverlay);
    }

    @Nullable
    private static TankContents resolveChemicalContents(AvianDousingMachineBlockEntity machine) {
        ResourceLocation chemicalId = machine.getChemicalId();
        if (chemicalId == null || machine.getChemicalAmount() <= 0) {
            return null;
        }
        ChemicalEggRegistryItem chemical = ChemicalEggRegistry.findByChemical(chemicalId);
        if (chemical == null) {
            chemical = GasEggRegistry.findByChemical(chemicalId);
        }
        return chemical == null
                ? null
                : new TankContents(chemical.getTexture(), chemical.getEggColor(),
                        machine.getChemicalAmount(), machine.getChemicalCapacity());
    }
}
