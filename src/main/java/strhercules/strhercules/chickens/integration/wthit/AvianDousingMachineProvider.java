package strhercules.chickens.integration.wthit;

import strhercules.chickens.blockentity.AvianDousingMachineBlockEntity;
import strhercules.chickens.integration.wthit.overlay.HudOverlayHelper;
import mcp.mobius.waila.api.IDataProvider;
import mcp.mobius.waila.api.IDataWriter;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.IServerAccessor;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Streams the dousing machine's buffered resources and progress back to WTHIT so
 * players can confirm infusion readiness without opening the GUI.
 */
final class AvianDousingMachineProvider implements IDataProvider<AvianDousingMachineBlockEntity> {

    @Override
    public void appendData(IDataWriter writer, IServerAccessor<AvianDousingMachineBlockEntity> accessor, IPluginConfig config) {
        AvianDousingMachineBlockEntity machine = accessor.getTarget();
        if (machine == null) {
            return;
        }
        HudOverlayHelper helper = new HudOverlayHelper();
        helper.addFluid(machine.getFluid().copy(), machine.getLiquidCapacity());
        if (machine.getChemicalAmount() > 0 || machine.getChemicalEntryId() >= 0) {
            helper.addChemical(machine.getChemicalEntryId(), machine.getChemicalAmount(), machine.getChemicalCapacity());
        }
        helper.addEnergy(machine.getEnergyStored(), machine.getEnergyCapacity());
        helper.addText(Component.translatable("tooltip.chickens.avian_dousing_machine.operation_cost",
                machine.getEnergyCostForCurrentOperation()));
        if (machine.getSpecialInfusion() != AvianDousingMachineBlockEntity.SpecialInfusion.NONE
                && machine.getSpecialAmount() > 0) {
            helper.addText(Component.translatable("tooltip.chickens.avian_dousing_machine.special",
                    machine.getSpecialInfusion().getDisplayName(), machine.getSpecialAmount()));
        }
        if (!machine.getItemReagent().isEmpty() && machine.getItemReagentCount() > 0) {
            helper.addText(Component.translatable("tooltip.chickens.avian_dousing_machine.item_reagent",
                    machine.getItemReagent().getHoverName(), machine.getItemReagentCount()));
        }
        int maxProgress = Math.max(machine.getMaxProgress(), 1);
        int percent = Math.max(machine.getProgress(), 0) * 100 / maxProgress;
        helper.addText(Component.translatable("tooltip.chickens.avian_dousing_machine.progress", percent));
        writer.add(HudOverlayHelper.TYPE, result -> result.add(helper));
    }
}
