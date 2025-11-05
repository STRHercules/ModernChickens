package com.setycz.chickens.item;

import com.setycz.chickens.ChemicalEggRegistry;
import com.setycz.chickens.ChemicalEggRegistryItem;
import com.setycz.chickens.registry.ModRegistry;
import com.setycz.chickens.item.ChickenItemHelper;
import net.minecraft.world.item.ItemStack;

public class ChemicalEggItem extends AbstractChemicalEggItem {
    public ChemicalEggItem(Properties properties) {
        super(properties, "item.chickens.chemical_egg.named", "item.chickens.chemical_egg.tooltip");
    }

    public static ItemStack createFor(ChemicalEggRegistryItem entry) {
        ItemStack stack = new ItemStack(ModRegistry.CHEMICAL_EGG.get());
        ChickenItemHelper.setChickenType(stack, entry.getId());
        return stack;
    }

    @Override
    protected ChemicalEggRegistryItem resolve(ItemStack stack) {
        return ChemicalEggRegistry.findById(ChickenItemHelper.getChickenType(stack));
    }
}
