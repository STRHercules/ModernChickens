package strhercules.chickens;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

public final class ChickenFood {
    public static final Ingredient INGREDIENT = Ingredient.of(
            Items.WHEAT_SEEDS, Items.MELON_SEEDS, Items.PUMPKIN_SEEDS, Items.BEETROOT_SEEDS);

    private ChickenFood() {
    }

    public static boolean test(ItemStack stack) {
        return INGREDIENT.test(stack);
    }
}
