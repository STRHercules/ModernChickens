package com.setycz.chickens.integration.wthit;

import com.setycz.chickens.ChemicalEggRegistry;
import com.setycz.chickens.ChemicalEggRegistryItem;
import com.setycz.chickens.GasEggRegistry;
import com.setycz.chickens.blockentity.AvianChemicalConverterBlockEntity;
import mcp.mobius.waila.api.IBlockAccessor;
import mcp.mobius.waila.api.IBlockComponentProvider;
import mcp.mobius.waila.api.IDataProvider;
import mcp.mobius.waila.api.IDataWriter;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.IServerAccessor;
import mcp.mobius.waila.api.ITooltip;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Mirrors the chemical converter tooltip in WTHIT so Mekanism integration
 * details are available from the HUD overlay.
 */
final class AvianChemicalConverterProvider implements IBlockComponentProvider, IDataProvider<AvianChemicalConverterBlockEntity> {
    private static final String AMOUNT_KEY = "ChickensChemical";
    private static final String CAPACITY_KEY = "ChickensChemicalCapacity";
    private static final String NAME_KEY = "ChickensChemicalName";

    @Override
    public void appendData(IDataWriter writer, IServerAccessor<AvianChemicalConverterBlockEntity> accessor, IPluginConfig config) {
        AvianChemicalConverterBlockEntity blockEntity = accessor.getTarget();
        CompoundTag tag = writer.raw();
        tag.putInt(AMOUNT_KEY, blockEntity.getChemicalAmount());
        tag.putInt(CAPACITY_KEY, blockEntity.getTankCapacity());
        ResourceLocation id = blockEntity.getChemicalId();
        if (id != null) {
            tag.putString(NAME_KEY, id.toString());
        }
    }

    @Override
    public void appendBody(ITooltip tooltip, IBlockAccessor accessor, IPluginConfig config) {
        CompoundTag tag = accessor.getData().raw();
        if (!tag.contains(AMOUNT_KEY) || !tag.contains(CAPACITY_KEY)) {
            return;
        }
        int amount = Math.max(tag.getInt(AMOUNT_KEY), 0);
        int capacity = Math.max(tag.getInt(CAPACITY_KEY), 0);
        if (capacity <= 0) {
            return;
        }
        Component name;
        if (!tag.contains(NAME_KEY) || amount <= 0) {
            name = Component.translatable("tooltip.chickens.avian_chemical_converter.empty");
        } else {
            ResourceLocation id = ResourceLocation.tryParse(tag.getString(NAME_KEY));
            ChemicalEggRegistryItem entry = id != null ? ChemicalEggRegistry.findByChemical(id) : null;
            if (entry == null && id != null) {
                entry = GasEggRegistry.findByChemical(id);
            }
            name = entry != null ? entry.getDisplayName() : Component.literal(tag.getString(NAME_KEY));
        }
        tooltip.addLine(Component.translatable("tooltip.chickens.avian_chemical_converter.level", name, amount, capacity));
    }
}
