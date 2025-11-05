package com.setycz.chickens.item;

import com.setycz.chickens.ChemicalEggRegistryItem;
import com.setycz.chickens.GasEggRegistry;
import com.setycz.chickens.registry.ModRegistry;
import com.setycz.chickens.item.ChickenItemHelper;
import net.minecraft.world.item.ItemStack;

public class GasEggItem extends AbstractChemicalEggItem {
    public GasEggItem(Properties properties) {
        super(properties, "item.chickens.gas_egg.named", "item.chickens.gas_egg.tooltip");
    }

    public static ItemStack createFor(ChemicalEggRegistryItem entry) {
        ItemStack stack = new ItemStack(ModRegistry.GAS_EGG.get());
        ChickenItemHelper.setChickenType(stack, entry.getId());
        return stack;
    }

    @Override
    protected ChemicalEggRegistryItem resolve(ItemStack stack) {
        return GasEggRegistry.findById(ChickenItemHelper.getChickenType(stack));
    }
}
