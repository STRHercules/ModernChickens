package com.setycz.chickens.item;

import com.setycz.chickens.LiquidEggRegistry;
import com.setycz.chickens.LiquidEggRegistryItem;
import com.setycz.chickens.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;
import javax.annotation.Nullable;

/**
 * Places blocks of fluid based on the chicken id encoded in the item stack. The
 * logic mirrors the legacy behaviour closely, including support for nether
 * vaporisation, but leverages ClipContext and the updated block API.
 */
public class LiquidEggItem extends Item {
    public LiquidEggItem(Properties properties) {
        super(properties);
    }

    public static ItemStack createFor(LiquidEggRegistryItem liquid) {
        ItemStack stack = new ItemStack(ModRegistry.LIQUID_EGG.get());
        ChickenItemHelper.setChickenType(stack, liquid.getId());
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        LiquidEggRegistryItem liquid = resolve(stack);
        if (liquid != null) {
            // Translate the contained block into a readable variant name (e.g., Water Egg).
            return Component.translatable("item.chickens.liquid_egg.named",
                    Component.translatable(liquid.getLiquid().getDescriptionId()));
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        LiquidEggRegistryItem liquid = resolve(stack);
        if (liquid != null) {
            tooltip.add(Component.translatable("item.chickens.liquid_egg.tooltip", Component.translatable(liquid.getLiquid().getDescriptionId())).withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(stack);
        }
        LiquidEggRegistryItem liquid = resolve(stack);
        if (liquid == null) {
            return InteractionResultHolder.fail(stack);
        }
        BlockPos blockPos = hit.getBlockPos();
        Direction face = hit.getDirection();
        if (!level.mayInteract(player, blockPos) || !player.mayUseItemAt(blockPos.relative(face), face, stack)) {
            return InteractionResultHolder.fail(stack);
        }

        BlockState state = level.getBlockState(blockPos);
        BlockPos placePos = state.canBeReplaced() ? blockPos : blockPos.relative(face);
        if (!tryPlaceLiquid(level, placePos, liquid.getLiquid(), player)) {
            return InteractionResultHolder.fail(stack);
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private boolean tryPlaceLiquid(Level level, BlockPos pos, Block liquidBlock, @Nullable Player player) {
        BlockState stateAtPos = level.getBlockState(pos);
        boolean replaceable = stateAtPos.canBeReplaced();
        boolean hasFluid = !stateAtPos.getFluidState().isEmpty();
        if (!level.isEmptyBlock(pos) && !replaceable && !hasFluid) {
            return false;
        }

        if (level.dimensionType().ultraWarm() && liquidBlock == Blocks.WATER) {
            level.playSound(player, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F + (level.random.nextFloat() - level.random.nextFloat()) * 0.8F);
            for (int i = 0; i < 8; ++i) {
                level.addParticle(ParticleTypes.LARGE_SMOKE, pos.getX() + level.random.nextDouble(), pos.getY() + level.random.nextDouble(), pos.getZ() + level.random.nextDouble(), 0.0D, 0.0D, 0.0D);
            }
            return true;
        }

        if (!level.isClientSide) {
            if (replaceable && !hasFluid) {
                level.destroyBlock(pos, true);
            }
            level.setBlock(pos, liquidBlock.defaultBlockState(), Block.UPDATE_ALL);
        }
        return true;
    }

    @Nullable
    private LiquidEggRegistryItem resolve(ItemStack stack) {
        return LiquidEggRegistry.findById(ChickenItemHelper.getChickenType(stack));
    }
}
