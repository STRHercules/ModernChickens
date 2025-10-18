package com.setycz.chickens.item;

import com.setycz.chickens.ChickensRegistryItem;
import com.setycz.chickens.blockentity.BreederBlockEntity;
import com.setycz.chickens.blockentity.RoostBlockEntity;
import com.setycz.chickens.client.render.item.ChickenItemRenderer;
import com.setycz.chickens.entity.ChickensChicken;
import com.setycz.chickens.registry.ModEntityTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.sounds.SoundSource;

import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * Item representation of a chicken. Players can drop stacks into roosts or
 * spawn the corresponding entity directly back into the world. The stack keeps
 * track of the chicken's stats so breeding progress is never lost.
 */
public class ChickenItem extends Item {
    public ChickenItem(Properties properties) {
        super(properties);
    }

    public ItemStack createFor(ChickensRegistryItem chicken) {
        ItemStack stack = new ItemStack(this);
        ChickenItemHelper.setChickenType(stack, chicken.getId());
        ChickenItemHelper.setStats(stack, ChickenStats.DEFAULT);
        return stack;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        ItemStack stack = context.getItemInHand();
        BlockPos pos = context.getClickedPos();
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof RoostBlockEntity roost) {
            if (!level.isClientSide && roost.putChicken(stack)) {
                return InteractionResult.CONSUME;
            }
            return InteractionResult.SUCCESS;
        }
        if (blockEntity instanceof BreederBlockEntity breeder) {
            if (!level.isClientSide && breeder.insertChicken(stack)) {
                return InteractionResult.CONSUME;
            }
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        spawnChicken(stack, context.getPlayer(), serverLevel, pos.relative(context.getClickedFace()));
        return InteractionResult.CONSUME;
    }

    private void spawnChicken(ItemStack stack, @Nullable net.minecraft.world.entity.player.Player player,
            ServerLevel level, BlockPos spawnPos) {
        ChickensRegistryItem description = ChickenItemHelper.resolve(stack);
        if (description == null) {
            return;
        }
        ChickensChicken chicken = ModEntityTypes.CHICKENS_CHICKEN.get().create(level);
        if (chicken == null) {
            return;
        }
        chicken.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D,
                level.random.nextFloat() * 360.0F, 0.0F);
        ChickenItemHelper.applyToEntity(stack, chicken);
        level.addFreshEntity(chicken);
        level.playSound(null, spawnPos, SoundEvents.CHICKEN_EGG, SoundSource.NEUTRAL, 0.5F, 1.0F);
        if (player == null || !player.getAbilities().instabuild) {
            // Consume a single chicken item when spawning the mob so stacks of
            // 16 behave exactly like the 1.12 port.
            stack.shrink(1);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ChickensRegistryItem chicken = ChickenItemHelper.resolve(stack);
        if (chicken != null) {
            tooltip.add(Component.translatable("item.chickens.chicken.type", chicken.getEntityName())
                    .withStyle(ChatFormatting.GRAY));
            ChickenStats stats = ChickenItemHelper.getStats(stack);
            tooltip.add(Component.translatable("item.chickens.chicken.stats", stats.gain(), stats.growth(), stats.strength())
                    .withStyle(ChatFormatting.DARK_GREEN));
            if (stats.analysed()) {
                tooltip.add(Component.translatable("item.chickens.chicken.analysed").withStyle(ChatFormatting.AQUA));
            }
        }
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        // Register a custom item renderer so inventory icons reuse the animated
        // chicken entity instead of falling back to a flat generated texture.
        consumer.accept(new IClientItemExtensions() {
            private ChickenItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    Minecraft minecraft = Minecraft.getInstance();
                    renderer = new ChickenItemRenderer(minecraft.getBlockEntityRenderDispatcher(), minecraft.getEntityModels());
                }
                return renderer;
            }
        });
    }
}
