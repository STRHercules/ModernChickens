package com.setycz.chickens.item;

import com.setycz.chickens.entity.ChickensChicken;
import com.setycz.chickens.entity.Rooster;
import com.setycz.chickens.registry.ModRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Tool that converts a grown chicken entity into its portable item form.
 * Players can repeatedly capture chickens and ferry them between roosts while
 * slowly damaging the catcher to mirror the legacy durability cost.
 */
public class ChickenCatcherItem extends Item {
    public ChickenCatcherItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity entity, InteractionHand hand) {
        Level level = entity.level();
        Vec3 position = entity.position();

        if (entity instanceof Rooster rooster) {
            return catchRooster(stack, player, hand, level, position, rooster);
        }

        if (!(entity instanceof ChickensChicken chicken)) {
            return InteractionResult.PASS;
        }

        if (entity.isBaby()) {
            spawnParticles(level, position, true);
            playSound(level, position, SoundEvents.CHICKEN_HURT);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (level instanceof ServerLevel serverLevel) {
            ItemStack chickenStack = new ItemStack(ModRegistry.CHICKEN_ITEM.get());
            ChickenItemHelper.copyFromEntity(chickenStack, chicken);
            serverLevel.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(serverLevel, position.x,
                    position.y + 0.2D, position.z, chickenStack));
            chicken.discard();
            spawnParticles(level, position, false);
            playSound(level, position, SoundEvents.CHICKEN_EGG);
            EquipmentSlot slot = hand == net.minecraft.world.InteractionHand.MAIN_HAND
                    ? EquipmentSlot.MAINHAND
                    : EquipmentSlot.OFFHAND;
            stack.hurtAndBreak(1, player, slot);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private InteractionResult catchRooster(ItemStack catcher, Player player, InteractionHand hand,
                                           Level level, Vec3 position, Rooster rooster) {
        if (rooster.isBaby()) {
            spawnParticles(level, position, true);
            playSound(level, position, SoundEvents.CHICKEN_HURT);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (level instanceof ServerLevel serverLevel) {
            ItemStack roosterStack = new ItemStack(ModRegistry.CHICKEN_ITEM.get());
            ChickenItemHelper.setRooster(roosterStack, true);
            RoosterItemData.copyFromEntity(roosterStack, rooster);
            serverLevel.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(serverLevel,
                    position.x, position.y + 0.2D, position.z, roosterStack));
            rooster.discard();
            spawnParticles(level, position, false);
            playSound(level, position, SoundEvents.CHICKEN_EGG);
            EquipmentSlot slot = hand == InteractionHand.MAIN_HAND
                    ? EquipmentSlot.MAINHAND
                    : EquipmentSlot.OFFHAND;
            catcher.hurtAndBreak(1, player, slot);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void spawnParticles(Level level, Vec3 position, boolean failedCapture) {
        for (int i = 0; i < 20; i++) {
            double offsetX = level.random.nextDouble() * 0.6D - 0.3D;
            double offsetY = level.random.nextDouble() * 0.6D;
            double offsetZ = level.random.nextDouble() * 0.6D - 0.3D;
            double velocityX = level.random.nextGaussian() * 0.02D;
            double velocityY = level.random.nextGaussian() * (failedCapture ? 0.1D : 0.2D);
            double velocityZ = level.random.nextGaussian() * 0.02D;
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(failedCapture ? ParticleTypes.SMOKE : ParticleTypes.POOF,
                        position.x + offsetX, position.y + offsetY, position.z + offsetZ, 1,
                        velocityX, velocityY, velocityZ, 0.0D);
            } else {
                level.addParticle(failedCapture ? ParticleTypes.SMOKE : ParticleTypes.POOF, position.x + offsetX,
                        position.y + offsetY, position.z + offsetZ, velocityX, velocityY, velocityZ);
            }
        }
    }

    private static void playSound(Level level, Vec3 position, net.minecraft.sounds.SoundEvent sound) {
        level.playSound(null, position.x, position.y, position.z, sound, SoundSource.NEUTRAL, 1.0F, 1.0F);
    }
}
