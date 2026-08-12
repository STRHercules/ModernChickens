package strhercules.chickens.effect;

import strhercules.chickens.ChickensMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

public final class BurningEffect extends MobEffect {
    private static final int HEALING_TICKS = 15 * 20;

    public BurningEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFF5A00);
        addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                ResourceLocation.fromNamespaceAndPath(ChickensMod.MOD_ID, "burning_speed"),
                0.2D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide && entity instanceof Player player) {
            player.heal(player.getMaxHealth() / (float) HEALING_TICKS);
        }
        return true;
    }
}
