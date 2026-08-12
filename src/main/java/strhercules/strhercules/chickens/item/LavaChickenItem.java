package strhercules.chickens.item;

import strhercules.chickens.registry.ModEffects;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class LavaChickenItem extends Item {
    public static final int EFFECT_DURATION_TICKS = 15 * 20;
    public static final int COOLDOWN_TICKS = 30 * 20;

    private static final FoodProperties FOOD = new FoodProperties.Builder()
            .nutrition(4)
            .saturationModifier(1.2F)
            .alwaysEdible()
            .build();

    public LavaChickenItem(Properties properties) {
        super(properties.food(FOOD));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        return super.use(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (entity instanceof Player player) {
            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
            if (!level.isClientSide) {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, EFFECT_DURATION_TICKS, 2));
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, EFFECT_DURATION_TICKS, 3));
                player.addEffect(new MobEffectInstance(ModEffects.BURNING, EFFECT_DURATION_TICKS, 0));
            }
        }
        return result;
    }
}
