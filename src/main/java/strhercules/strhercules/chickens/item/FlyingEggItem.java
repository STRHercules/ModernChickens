package strhercules.chickens.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class FlyingEggItem extends Item {
    public FlyingEggItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.chickens.flying_egg").withStyle(ChatFormatting.AQUA);
    }
}
