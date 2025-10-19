package com.setycz.chickens.blockentity;

import com.setycz.chickens.ChickensRegistry;
import com.setycz.chickens.ChickensRegistryItem;
import com.setycz.chickens.block.BreederBlock;
import com.setycz.chickens.config.ChickensConfigHolder;
import com.setycz.chickens.entity.ChickensChicken;
import com.setycz.chickens.item.ChickenItemHelper;
import com.setycz.chickens.item.ChickenStats;
import com.setycz.chickens.menu.BreederMenu;
import com.setycz.chickens.registry.ModBlockEntities;
import com.setycz.chickens.registry.ModEntityTypes;
import com.setycz.chickens.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

import java.util.List;

/**
 * Server-side logic for the breeder. Two parent chickens consume seeds to
 * create a new chicken item that inherits stats using the same algorithm as the
 * Chickens entities themselves.
 */
public class BreederBlockEntity extends AbstractChickenContainerBlockEntity {
    public static final int INVENTORY_SIZE = 6;
    public static final int LEFT_CHICKEN_SLOT = 0;
    public static final int RIGHT_CHICKEN_SLOT = 1;
    public static final int SEED_SLOT = 2;
    private static final int REQUIRED_SEEDS = 2;

    public BreederBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BREEDER.get(), pos, state, INVENTORY_SIZE, 2);
    }

    @Override
    protected void spawnChickenDrop(RandomSource random) {
        Level level = getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        ChickensChicken parentA = createParentFromSlot(serverLevel, LEFT_CHICKEN_SLOT);
        ChickensChicken parentB = createParentFromSlot(serverLevel, RIGHT_CHICKEN_SLOT);
        if (parentA == null || parentB == null) {
            return;
        }
        ChickensChicken child = parentA.getBreedOffspring(serverLevel, parentB);
        if (child == null) {
            return;
        }
        ItemStack stack = new ItemStack(ModRegistry.CHICKEN_ITEM.get());
        ChickenItemHelper.copyFromEntity(stack, child);
        ItemStack remaining = pushIntoOutput(stack);
        if (!remaining.isEmpty()) {
            Containers.dropItemStack(serverLevel, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), remaining);
        } else {
            playSpawnEffects(serverLevel);
        }
    }

    @Nullable
    private ChickensChicken createParentFromSlot(ServerLevel level, int slot) {
        ItemStack stack = getItem(slot);
        if (!ChickenItemHelper.isChicken(stack) || stack.isEmpty()) {
            return null;
        }
        ChickensChicken parent = ModEntityTypes.CHICKENS_CHICKEN.get().create(level);
        if (parent == null) {
            return null;
        }
        ChickenItemHelper.applyToEntity(stack, parent);
        return parent;
    }

    private void playSpawnEffects(ServerLevel level) {
        level.playSound(null, worldPosition, SoundEvents.CHICKEN_EGG, SoundSource.BLOCKS, 0.5F, 0.8F);
        spawnHeart(level, 0.2D, 0.0D);
        spawnHeart(level, 0.8D, 0.0D);
        spawnHeart(level, 0.5D, 0.2D);
        spawnHeart(level, 0.5D, -0.2D);
    }

    private void spawnHeart(ServerLevel level, double xOffset, double zOffset) {
        level.sendParticles(ParticleTypes.HEART, worldPosition.getX() + xOffset, worldPosition.getY() + 0.8D,
                worldPosition.getZ() + zOffset, 2, 0.05D, 0.05D, 0.05D, 0.01D);
    }

    @Override
    protected int requiredSeedsForDrop() {
        return REQUIRED_SEEDS;
    }

    @Override
    protected double speedMultiplier() {
        return ChickensConfigHolder.get().getBreederSpeedMultiplier();
    }

    @Override
    protected int getChickenSlotCount() {
        return 2;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("menu.chickens.breeder");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory playerInventory, ContainerData dataAccess) {
        return new BreederMenu(id, playerInventory, this, dataAccess);
    }

    @Nullable
    @Override
    protected ChickenContainerEntry createChickenData(int slot, ItemStack stack) {
        if (slot != LEFT_CHICKEN_SLOT && slot != RIGHT_CHICKEN_SLOT) {
            return null;
        }
        if (!ChickenItemHelper.isChicken(stack)) {
            return null;
        }
        ChickensRegistryItem chicken = ChickenItemHelper.resolve(stack);
        if (chicken == null) {
            return null;
        }
        ChickenStats stats = ChickenItemHelper.getStats(stack);
        return new ChickenContainerEntry(chicken, stats);
    }

    @Override
    protected int getMaxStackSizeForSlot(int slot, ItemStack stack) {
        if (slot == LEFT_CHICKEN_SLOT || slot == RIGHT_CHICKEN_SLOT) {
            return 1;
        }
        if (slot == SEED_SLOT) {
            return 64;
        }
        return super.getMaxStackSizeForSlot(slot, stack);
    }

    @Override
    protected void onFullnessChanged(Level level, boolean hasRequiredChickens, boolean hasRequiredSeeds) {
        BlockState state = level.getBlockState(worldPosition);
        if (state.getBlock() instanceof BreederBlock) {
            boolean changed = state.getValue(BreederBlock.BREEDING) != hasRequiredChickens
                    || state.getValue(BreederBlock.HAS_SEEDS) != hasRequiredSeeds;
            if (changed) {
                level.setBlock(worldPosition, state
                        .setValue(BreederBlock.BREEDING, hasRequiredChickens)
                        .setValue(BreederBlock.HAS_SEEDS, hasRequiredSeeds),
                        Block.UPDATE_ALL);
            }
        }
    }

    /**
     * Attempts to place a chicken item into the breeder when players interact
     * with the block directly instead of the menu.
     */
    public boolean insertChicken(ItemStack stack) {
        if (!ChickenItemHelper.isChicken(stack)) {
            return false;
        }
        if (tryInsertIntoSlot(stack, LEFT_CHICKEN_SLOT)) {
            return true;
        }
        return tryInsertIntoSlot(stack, RIGHT_CHICKEN_SLOT);
    }

    private boolean tryInsertIntoSlot(ItemStack stack, int slot) {
        ItemStack existing = getItem(slot);
        if (existing.isEmpty()) {
            setItem(slot, stack.split(1));
            playAddSound();
            return true;
        }
        return false;
    }

    /**
     * Removes the most recently inserted chicken and hands it back to the
     * interacting player.
     */
    public boolean extractChicken(Player player) {
        for (int slot : new int[] { RIGHT_CHICKEN_SLOT, LEFT_CHICKEN_SLOT }) {
            ItemStack stack = getItem(slot);
            if (!stack.isEmpty()) {
                setItem(slot, ItemStack.EMPTY);
                if (!player.addItem(stack)) {
                    player.drop(stack, false);
                }
                playRemoveSound();
                return true;
            }
        }
        return false;
    }

    /**
     * Inserts seeds from the held stack so automation-free farms can be kept
     * stocked without opening the menu.
     */
    public boolean addSeeds(ItemStack stack) {
        if (!stack.is(Items.WHEAT_SEEDS) && !stack.is(Items.BEETROOT_SEEDS) && !stack.is(Items.MELON_SEEDS)
                && !stack.is(Items.PUMPKIN_SEEDS)) {
            return false;
        }
        ItemStack seedSlot = getItem(SEED_SLOT);
        if (seedSlot.isEmpty()) {
            setItem(SEED_SLOT, stack.split(Math.min(stack.getCount(), stack.getMaxStackSize())));
            return true;
        }
        int canMove = Math.min(seedSlot.getMaxStackSize() - seedSlot.getCount(), stack.getCount());
        if (canMove <= 0) {
            return false;
        }
        seedSlot.grow(canMove);
        stack.shrink(canMove);
        setChanged();
        return true;
    }

    private void playAddSound() {
        if (level != null) {
            level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    private void playRemoveSound() {
        if (level != null) {
            level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    @Override
    public void storeTooltipData(CompoundTag tag) {
        super.storeTooltipData(tag);
        ItemStack left = getItem(LEFT_CHICKEN_SLOT);
        ItemStack right = getItem(RIGHT_CHICKEN_SLOT);
        if (!left.isEmpty()) {
            tag.putInt("LeftChickenId", ChickenItemHelper.getChickenType(left));
        }
        if (!right.isEmpty()) {
            tag.putInt("RightChickenId", ChickenItemHelper.getChickenType(right));
        }
        tag.putInt("SeedCount", getItem(SEED_SLOT).getCount());
    }

    @Override
    public void appendTooltip(List<Component> tooltip, CompoundTag data) {
        if (data.contains("LeftChickenId") && data.contains("RightChickenId")) {
            ChickensRegistryItem left = ChickensRegistry.getByType(data.getInt("LeftChickenId"));
            ChickensRegistryItem right = ChickensRegistry.getByType(data.getInt("RightChickenId"));
            if (left != null && right != null) {
                tooltip.add(Component.translatable("tooltip.chickens.breeder.parents", left.getDisplayName(),
                        right.getDisplayName()));
            }
        }
        tooltip.add(Component.translatable("tooltip.chickens.breeder.seeds", data.getInt("SeedCount")));
        super.appendTooltip(tooltip, data);
    }
}
