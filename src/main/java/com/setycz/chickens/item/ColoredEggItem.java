package com.setycz.chickens.item;

import com.setycz.chickens.ChickensRegistryItem;
import com.setycz.chickens.entity.ColoredEgg;
import com.setycz.chickens.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Throwable egg item whose colour and spawned chicken type depend on the
 * underlying registry entry. The behaviour mirrors the legacy metadata driven
 * implementation but exposes modern tooltips and stats.
 */
public class ColoredEggItem extends Item {
    public ColoredEggItem(Properties properties) {
        super(properties);
    }

    public static ItemStack createFor(ChickensRegistryItem chicken) {
        ItemStack stack = new ItemStack(ModRegistry.COLORED_EGG.get());
        ChickenItemHelper.setChickenType(stack, chicken.getId());
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        ChickensRegistryItem chicken = ChickenItemHelper.resolve(stack);
        if (chicken != null) {
            // Keep per-chicken egg names so creative inventory and JEI entries mirror the legacy mod.
            return Component.translatable("item.chickens.colored_egg.named",
                    chicken.getDisplayName());
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ChickensRegistryItem chicken = ChickenItemHelper.resolve(stack);
        if (chicken != null) {
            tooltip.add(Component.translatable("item.chickens.colored_egg.tooltip", chicken.getDisplayName()).withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EGG_THROW, SoundSource.NEUTRAL, 0.5F, 0.4F / (player.getRandom().nextFloat() * 0.4F + 0.8F));
        if (!level.isClientSide) {
            ColoredEgg projectile = new ColoredEgg(level, player);
            projectile.setItem(stack.copyWithCount(1));
            projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
            projectile.setChickenType(ChickenItemHelper.getChickenType(stack));
            level.addFreshEntity(projectile);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
