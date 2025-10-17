package com.setycz.chickens.item;

import com.setycz.chickens.entity.ChickensChicken;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;

import java.util.List;

/**
 * Portable chicken analyser that reports the target bird's tier and stats. The
 * original implementation printed chat messages; here we keep that behaviour but
 * switch to the modern Component API and durability tracking helpers.
 */
public class AnalyzerItem extends Item {
    public AnalyzerItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.chickens.analyzer.tooltip1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.chickens.analyzer.tooltip2").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof ChickensChicken chicken)) {
            return InteractionResult.PASS;
        }
        if (player.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        chicken.setStatsAnalyzed(true);
        player.sendSystemMessage(chicken.getName().copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        player.sendSystemMessage(Component.translatable("entity.ChickensChicken.tier", chicken.getTier()));
        player.sendSystemMessage(Component.translatable("entity.ChickensChicken.growth", chicken.getGrowth()));
        player.sendSystemMessage(Component.translatable("entity.ChickensChicken.gain", chicken.getGain()));
        player.sendSystemMessage(Component.translatable("entity.ChickensChicken.strength", chicken.getStrength()));
        if (!chicken.isBaby()) {
            int progress = chicken.getLayProgress();
            if (progress <= 0) {
                player.sendSystemMessage(Component.translatable("entity.ChickensChicken.nextEggSoon"));
            } else {
                player.sendSystemMessage(Component.translatable("entity.ChickensChicken.layProgress", progress));
            }
        }

        EquipmentSlot slot = hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
        stack.hurtAndBreak(1, player, slot);
        return InteractionResult.SUCCESS;
    }
}
