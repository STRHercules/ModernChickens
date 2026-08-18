package strhercules.chickens.item;

import javax.annotation.Nullable;
import net.minecraft.world.level.Level;
import strhercules.chickens.entity.MegaChickenSkin;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** End-game loot item that applies one validated Mega Chicken skin. */
public final class MegaChickenSkinCrateItem extends Item {
    private final MegaChickenSkin skin;

    public MegaChickenSkinCrateItem(Properties properties, MegaChickenSkin skin) {
        super(properties.stacksTo(1));
        this.skin = skin;
    }

    public MegaChickenSkin skin() {
        return skin;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(getDescriptionId(stack)).withStyle(ChatFormatting.AQUA);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip,
            TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.chickens.mega_chicken_skin_crate", skin.displayName())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.chickens.mega_chicken_skin_crate.ownership")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
