package com.setycz.chickens.integration.wthit;

import com.setycz.chickens.ChemicalEggRegistry;
import com.setycz.chickens.ChemicalEggRegistryItem;
import com.setycz.chickens.blockentity.AvianDousingMachineBlockEntity;
import mcp.mobius.waila.api.IBlockAccessor;
import mcp.mobius.waila.api.IBlockComponentProvider;
import mcp.mobius.waila.api.IDataProvider;
import mcp.mobius.waila.api.IDataWriter;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.IServerAccessor;
import mcp.mobius.waila.api.ITooltip;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Streams the dousing machine's buffered resources and progress back to WTHIT so
 * players can confirm infusion readiness without opening the GUI.
 */
final class AvianDousingMachineProvider implements IBlockComponentProvider, IDataProvider<AvianDousingMachineBlockEntity> {
    private static final String ENERGY_KEY = "ChickensEnergy";
    private static final String ENERGY_CAPACITY_KEY = "ChickensEnergyCapacity";
    private static final String CHEMICAL_AMOUNT_KEY = "ChickensChemicalAmount";
    private static final String CHEMICAL_CAPACITY_KEY = "ChickensChemicalCapacity";
    private static final String CHEMICAL_ENTRY_KEY = "ChickensChemicalEntry";
    private static final String LIQUID_AMOUNT_KEY = "ChickensLiquidAmount";
    private static final String LIQUID_CAPACITY_KEY = "ChickensLiquidCapacity";
    private static final String LIQUID_NAME_KEY = "ChickensLiquidName";
    private static final String PROGRESS_KEY = "ChickensProgress";
    private static final String PROGRESS_MAX_KEY = "ChickensProgressMax";

    @Override
    public void appendData(IDataWriter writer, IServerAccessor<AvianDousingMachineBlockEntity> accessor, IPluginConfig config) {
        AvianDousingMachineBlockEntity machine = accessor.getTarget();
        CompoundTag tag = writer.raw();
        tag.putInt(ENERGY_KEY, machine.getEnergyStored());
        tag.putInt(ENERGY_CAPACITY_KEY, machine.getEnergyCapacity());
        tag.putInt(CHEMICAL_AMOUNT_KEY, machine.getChemicalAmount());
        tag.putInt(CHEMICAL_CAPACITY_KEY, machine.getChemicalCapacity());
        tag.putInt(CHEMICAL_ENTRY_KEY, machine.getChemicalEntryId());
        tag.putInt(LIQUID_AMOUNT_KEY, machine.getLiquidAmount());
        tag.putInt(LIQUID_CAPACITY_KEY, machine.getLiquidCapacity());
        FluidStack fluid = machine.getFluid();
        if (!fluid.isEmpty()) {
            ResourceLocation id = BuiltInRegistries.FLUID.getKey(fluid.getFluid());
            if (id != null) {
                tag.putString(LIQUID_NAME_KEY, id.toString());
            }
        }
        tag.putInt(PROGRESS_KEY, machine.getProgress());
        tag.putInt(PROGRESS_MAX_KEY, machine.getMaxProgress());
    }

    @Override
    public void appendBody(ITooltip tooltip, IBlockAccessor accessor, IPluginConfig config) {
        CompoundTag tag = accessor.getData().raw();
        appendChemicalLine(tooltip, tag);
        appendLiquidLine(tooltip, tag);
        appendEnergyLine(tooltip, tag);
        appendProgressLine(tooltip, tag);
    }

    private static void appendChemicalLine(ITooltip tooltip, CompoundTag tag) {
        if (!tag.contains(CHEMICAL_AMOUNT_KEY) || !tag.contains(CHEMICAL_CAPACITY_KEY)) {
            return;
        }
        int amount = Math.max(tag.getInt(CHEMICAL_AMOUNT_KEY), 0);
        int capacity = Math.max(tag.getInt(CHEMICAL_CAPACITY_KEY), 0);
        if (capacity <= 0) {
            return;
        }
        int entryId = tag.getInt(CHEMICAL_ENTRY_KEY);
        ChemicalEggRegistryItem entry = ChemicalEggRegistry.findById(entryId);
        Component name;
        if (amount <= 0) {
            name = Component.translatable("tooltip.chickens.avian_dousing_machine.empty");
        } else if (entry != null) {
            name = entry.getDisplayName();
        } else {
            name = Component.literal(String.valueOf(entryId));
        }
        tooltip.addLine(Component.translatable("tooltip.chickens.avian_dousing_machine.chemical",
                name,
                amount,
                capacity,
                AvianDousingMachineBlockEntity.CHEMICAL_COST,
                AvianDousingMachineBlockEntity.CHEMICAL_ENERGY_COST));
    }

    private static void appendLiquidLine(ITooltip tooltip, CompoundTag tag) {
        if (!tag.contains(LIQUID_AMOUNT_KEY) || !tag.contains(LIQUID_CAPACITY_KEY)) {
            return;
        }
        int amount = Math.max(tag.getInt(LIQUID_AMOUNT_KEY), 0);
        int capacity = Math.max(tag.getInt(LIQUID_CAPACITY_KEY), 0);
        if (capacity <= 0) {
            return;
        }
        Component display;
        if (amount <= 0 || !tag.contains(LIQUID_NAME_KEY)) {
            display = Component.translatable("tooltip.chickens.avian_dousing_machine.empty");
        } else {
            ResourceLocation id = ResourceLocation.tryParse(tag.getString(LIQUID_NAME_KEY));
            FluidStack stack = id != null && BuiltInRegistries.FLUID.containsKey(id)
                    ? new FluidStack(BuiltInRegistries.FLUID.get(id), amount)
                    : FluidStack.EMPTY;
            display = stack.isEmpty()
                    ? Component.literal(tag.getString(LIQUID_NAME_KEY))
                    : stack.getHoverName();
        }
        tooltip.addLine(Component.translatable("tooltip.chickens.avian_dousing_machine.liquid",
                display,
                amount,
                capacity,
                AvianDousingMachineBlockEntity.LIQUID_COST,
                AvianDousingMachineBlockEntity.LIQUID_ENERGY_COST));
    }

    private static void appendEnergyLine(ITooltip tooltip, CompoundTag tag) {
        if (!tag.contains(ENERGY_KEY) || !tag.contains(ENERGY_CAPACITY_KEY)) {
            return;
        }
        int capacity = Math.max(tag.getInt(ENERGY_CAPACITY_KEY), 0);
        if (capacity <= 0) {
            return;
        }
        int energy = Math.max(tag.getInt(ENERGY_KEY), 0);
        tooltip.addLine(Component.translatable("tooltip.chickens.avian_dousing_machine.energy", energy, capacity));
    }

    private static void appendProgressLine(ITooltip tooltip, CompoundTag tag) {
        if (!tag.contains(PROGRESS_KEY) || !tag.contains(PROGRESS_MAX_KEY)) {
            return;
        }
        int max = Math.max(tag.getInt(PROGRESS_MAX_KEY), 0);
        if (max <= 0) {
            return;
        }
        int progress = Math.max(Math.min(tag.getInt(PROGRESS_KEY), max), 0);
        int percent = progress * 100 / max;
        tooltip.addLine(Component.translatable("tooltip.chickens.avian_dousing_machine.progress", percent));
    }
}
