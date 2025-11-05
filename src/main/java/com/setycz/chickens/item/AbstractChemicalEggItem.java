package com.setycz.chickens.item;

import com.setycz.chickens.ChemicalEggRegistryItem;
import com.setycz.chickens.LiquidEggRegistryItem;
import com.setycz.chickens.config.ChickensConfigHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.List;

abstract class AbstractChemicalEggItem extends Item {
    private final String nameTranslationKey;
    private final String tooltipTranslationKey;

    protected AbstractChemicalEggItem(Properties properties, String nameKey, String tooltipKey) {
        super(properties);
        this.nameTranslationKey = nameKey;
        this.tooltipTranslationKey = tooltipKey;
    }

    @Override
    public Component getName(ItemStack stack) {
        ChemicalEggRegistryItem entry = resolve(stack);
        if (entry != null) {
            return Component.translatable(nameTranslationKey, entry.getDisplayName());
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ChemicalEggRegistryItem entry = resolve(stack);
        if (entry == null) {
            return;
        }
        tooltip.add(Component.translatable(tooltipTranslationKey, entry.getDisplayName(), entry.getVolume())
                .withStyle(ChatFormatting.GRAY));
        appendHazardTooltips(entry, tooltip);
    }

    protected void appendHazardTooltips(ChemicalEggRegistryItem entry, List<Component> tooltip) {
        if (!ChickensConfigHolder.get().isLiquidEggHazardsEnabled()) {
            return;
        }
        for (LiquidEggRegistryItem.HazardFlag hazard : entry.getHazards()) {
            tooltip.add(Component.translatable("item.chickens.liquid_egg.tooltip.hazard." + hazard.getTranslationKey())
                    .withStyle(ChatFormatting.DARK_RED));
        }
    }

    @Nullable
    protected abstract ChemicalEggRegistryItem resolve(ItemStack stack);
}
