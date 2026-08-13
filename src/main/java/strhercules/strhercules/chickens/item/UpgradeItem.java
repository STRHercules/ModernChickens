package strhercules.chickens.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Shared tooltip presentation for the machine upgrade items. */
public final class UpgradeItem extends Item {
    public enum Kind {
        SPEED("speedupgrade"),
        STACK("stackupgrade"),
        STORAGE("storagecapacity"),
        RANGE("rangeupgrade"),
        RF("rfupgrade");

        private final String id;

        Kind(String id) {
            this.id = id;
        }
    }

    private final Kind kind;

    public UpgradeItem(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.chickens." + kind.id + ".tooltip")
                .withStyle(ChatFormatting.GRAY));
    }
}
