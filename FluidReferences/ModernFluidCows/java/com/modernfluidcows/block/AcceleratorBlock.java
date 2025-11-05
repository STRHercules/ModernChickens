package com.modernfluidcows.block;

import com.modernfluidcows.blockentity.AcceleratorBlockEntity;
import com.modernfluidcows.registry.FluidCowsRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;
import org.jetbrains.annotations.Nullable;

/**
 * Cow accelerator block that consumes wheat and water to hasten juvenile growth.
 *
 * <p>The legacy 1.12 implementation exposed fluid interactions, GUI opening, and
 * water bottle shortcuts directly from the block. NeoForge keeps the same
 * behaviour here so existing automation setups still work.</p>
 */
public class AcceleratorBlock extends Block implements EntityBlock {
    public AcceleratorBlock(final Properties properties) {
        super(properties);
    }

    public InteractionResult use(
            final BlockState state,
            final Level level,
            final BlockPos pos,
            final Player player,
            final InteractionHand hand,
            final BlockHitResult hit) {
        BlockEntity entity = level.getBlockEntity(pos);
        if (!(entity instanceof AcceleratorBlockEntity accelerator)) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);

        // Allow quick-filling the tank with vanilla water bottles to mirror the
        // legacy quality-of-life shortcut.
        if (!held.isEmpty()
                && held.is(Items.POTION)
                && held.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(Potions.WATER)
                && accelerator.tryConsumeWaterBottle(player, held)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // Pipe-friendly fluid interaction: buckets and tanks can exchange with
        // the accelerator directly just like in 1.12.
        if (FluidUtil.interactWithFluidHandler(player, hand, accelerator.getTank())) {
            level.gameEvent(player, GameEvent.FLUID_PLACE, pos);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(accelerator, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(
            final BlockState state,
            final Level level,
            final BlockPos pos,
            final BlockState newState,
            final boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof AcceleratorBlockEntity accelerator) {
                Containers.dropContents(level, pos, accelerator.getInventory());
                accelerator.clearContent();
            }
            super.onRemove(state, level, pos, newState, isMoving);
            return;
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public RenderShape getRenderShape(final BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new AcceleratorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            final Level level, final BlockState state, final BlockEntityType<T> type) {
        if (type != FluidCowsRegistries.ACCELERATOR_BLOCK_ENTITY.get()) {
            return null;
        }
        return level.isClientSide
                ? (lvl, blockPos, blockState, be) ->
                        AcceleratorBlockEntity.clientTick(lvl, blockPos, blockState, (AcceleratorBlockEntity) be)
                : (lvl, blockPos, blockState, be) ->
                        AcceleratorBlockEntity.serverTick(lvl, blockPos, blockState, (AcceleratorBlockEntity) be);
    }
}
