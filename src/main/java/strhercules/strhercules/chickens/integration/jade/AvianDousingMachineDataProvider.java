package strhercules.chickens.integration.jade;

import strhercules.chickens.ChickensMod;
import strhercules.chickens.blockentity.AvianDousingMachineBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

/**
 * Streams the dousing machine's buffers and progress to Jade.
 */
enum AvianDousingMachineDataProvider implements IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final ResourceLocation ID = new ResourceLocation(ChickensMod.MOD_ID, "avian_dousing_machine");

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof AvianDousingMachineBlockEntity machine)) {
            return;
        }
        HudData.Builder builder = HudData.builder();
        builder.addFluid(machine.getFluid().copy(), machine.getLiquidCapacity());
        if (machine.getChemicalAmount() > 0 || machine.getChemicalEntryId() >= 0) {
            builder.addChemical(machine.getChemicalEntryId(), machine.getChemicalAmount(), machine.getChemicalCapacity());
        }
        builder.addEnergy(machine.getEnergyStored(), machine.getEnergyCapacity());
        builder.addText(Component.translatable("tooltip.chickens.avian_dousing_machine.operation_cost",
                machine.getEnergyCostForCurrentOperation()));
        if (machine.getSpecialInfusion() != AvianDousingMachineBlockEntity.SpecialInfusion.NONE
                && machine.getSpecialAmount() > 0) {
            builder.addText(Component.translatable("tooltip.chickens.avian_dousing_machine.special",
                    machine.getSpecialInfusion().getDisplayName(), machine.getSpecialAmount()));
        }
        if (!machine.getItemReagent().isEmpty() && machine.getItemReagentCount() > 0) {
            builder.addText(Component.translatable("tooltip.chickens.avian_dousing_machine.item_reagent",
                    machine.getItemReagent().getHoverName(), machine.getItemReagentCount()));
        }
        int maxProgress = Math.max(machine.getMaxProgress(), 1);
        int percent = Math.max(machine.getProgress(), 0) * 100 / maxProgress;
        builder.addText(Component.translatable("tooltip.chickens.avian_dousing_machine.progress", percent));
        HudData.write(data, builder.build());
    }

    @Override
    public ResourceLocation getUid() {
        return ID;
    }
}
