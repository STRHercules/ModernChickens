package com.setycz.chickens.item;

import com.setycz.chickens.ChickensRegistryItem;
import com.setycz.chickens.entity.ChickensChicken;
import com.setycz.chickens.registry.ModEntityTypes;
import com.setycz.chickens.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;

/**
 * Custom spawn egg that colours itself based on the chicken descriptor stored in NBT.
 */
public class ChickensSpawnEggItem extends net.minecraft.world.item.SpawnEggItem {
    public ChickensSpawnEggItem(Properties properties) {
        super(ModEntityTypes.CHICKENS_CHICKEN.get(), 0xffffff, 0xffffff, properties);
    }

    public static ItemStack createFor(ChickensRegistryItem chicken) {
        ItemStack stack = new ItemStack(ModRegistry.SPAWN_EGG.get());
        ChickenItemHelper.setChickenType(stack, chicken.getId());
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        ChickensRegistryItem chicken = ChickenItemHelper.resolve(stack);
        if (chicken == null) {
            return super.getName(stack);
        }
        return Component.translatable("item.chickens.spawn_egg.named",
                chicken.getDisplayName())
                .withStyle(ChatFormatting.YELLOW);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ChickensRegistryItem chicken = ChickenItemHelper.resolve(stack);
        if (chicken != null) {
            tooltip.add(Component.translatable("item.chickens.spawn_egg.tooltip", chicken.getTier())
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = context.getItemInHand();
        ChickensRegistryItem chicken = ChickenItemHelper.resolve(stack);
        if (chicken == null) {
            return InteractionResult.FAIL;
        }
        BlockPos clickedPos = context.getClickedPos();
        BlockState state = level.getBlockState(clickedPos);
        BlockPos spawnPos = state.getCollisionShape(level, clickedPos).isEmpty()
                ? clickedPos
                : clickedPos.relative(context.getClickedFace());

        ChickensChicken entity = ModEntityTypes.CHICKENS_CHICKEN.get().create(serverLevel);
        if (entity == null) {
            return InteractionResult.FAIL;
        }
        entity.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D,
                level.random.nextFloat() * 360.0F, 0.0F);
        entity.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(spawnPos),
                MobSpawnType.SPAWN_EGG, null);

        CustomData data = stack.get(DataComponents.ENTITY_DATA);
        if (data != null) {
            entity.load(data.copyTag());
        }

        // Assign the chicken type *after* finalizeSpawn so the random wild spawn
        // logic inside ChickensChicken does not overwrite the egg's metadata.
        entity.setChickenType(chicken.getId());

        serverLevel.addFreshEntity(entity);
        if (!(context.getPlayer() != null && context.getPlayer().getAbilities().instabuild)) {
            stack.shrink(1);
        }
        serverLevel.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.CHICKEN_AMBIENT, SoundSource.NEUTRAL, 0.5F, 1.0F);
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = Item.getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(stack);
        }
        InteractionResult result = this.useOn(new UseOnContext(player, hand, hit));
        if (result.consumesAction()) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        return new InteractionResultHolder<>(result, stack);
    }
}
