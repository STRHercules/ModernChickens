package strhercules.chickens.blockentity;

import strhercules.chickens.ChickensRegistry;
import strhercules.chickens.ChickensRegistryItem;
import strhercules.chickens.config.ChickensConfigHolder;
import strhercules.chickens.item.ChickenItemHelper;
import strhercules.chickens.blockentity.NestBlockEntity;
import strhercules.chickens.item.ChickenStats;
import strhercules.chickens.menu.RoostMenu;
import strhercules.chickens.registry.ModBlockEntities;
import strhercules.chickens.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.util.RandomSource;

import java.util.List;

/**
 * Server-side logic for the roost block. The block entity manages a single
 * chicken stack plus four output slots that accumulate drops over time.
 */
public class RoostBlockEntity extends AbstractChickenContainerBlockEntity {
    public static final int INVENTORY_SIZE = 5;
    public static final int CHICKEN_SLOT = 0;
    public static final int SPEED_UPGRADE_SLOT = 0;
    public static final int STACK_UPGRADE_SLOT = 1;
    public static final int UPGRADE_SLOT_COUNT = 2;
    private static final int MAX_CHICKENS = 16;

    public RoostBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ROOST.get(), pos, state, INVENTORY_SIZE, 1, UPGRADE_SLOT_COUNT);
    }

    @Override
    protected boolean spawnChickenItem(RandomSource random) {
        ChickenContainerEntry entry = getChickenEntry(CHICKEN_SLOT);
        if (entry == null) {
            return false;
        }
        ItemStack item = entry.createLay(random, ChickensConfigHolder.get().isScalingDropsEnabled());
        return queueOutput(item);
    }

    @Override
    protected int requiredSeedsForDrop() {
        return 0;
    }

    @Override
    protected double speedMultiplier() {
        double base = ChickensConfigHolder.get().getRoostSpeedMultiplier();
        double upgradeMultiplier = 1.0D + 0.2D * getUpgradeCount(SPEED_UPGRADE_SLOT);

        // So the production rate can be changed per chicken in chicken.cfg
        double chickenLayCoeffient = 1.0;
        if (getChickenEntry(CHICKEN_SLOT) != null) {
            chickenLayCoeffient = getChickenEntry(CHICKEN_SLOT).chicken().getLayCoefficient();
        }


        double auraMultiplier = ChickensConfigHolder.get().getRoosterAuraMultiplier();
        int auraRange = ChickensConfigHolder.get().getRoosterAuraRange();
        if (auraRange <= 0 || auraMultiplier <= 1.0D || level == null) {
            return base * upgradeMultiplier;
        }
        int activeRoosters = countActiveRoostersInNests(level, worldPosition, auraRange);
        if (activeRoosters <= 0) {
            return base * upgradeMultiplier;
        }
        // Preserve the existing meaning of roosterAuraMultiplier for a single
        // rooster while scaling linearly with additional birds. For example,
        // a multiplier of 1.25 and three active roosters yields:
        // base * (1 + 3 * 0.25) = base * 1.75.
        double bonusPerRooster = auraMultiplier - 1.0D;
        double totalMultiplier = 1.0D + activeRoosters * bonusPerRooster;
        return (base * Math.max(totalMultiplier, 0.0D)) * chickenLayCoeffient * upgradeMultiplier;
    }

    @Override
    protected boolean hasStackUpgrade() {
        return getUpgradeCount(STACK_UPGRADE_SLOT) > 0;
    }

    @Override
    protected int getStackUpgradeCount() {
        return getUpgradeCount(STACK_UPGRADE_SLOT);
    }

    @Override
    public boolean canPlaceUpgrade(int slot, ItemStack stack) {
        return slot == SPEED_UPGRADE_SLOT && stack.is(ModRegistry.SPEED_UPGRADE.get())
                || slot == STACK_UPGRADE_SLOT && stack.is(ModRegistry.STACK_UPGRADE.get());
    }

    @Override
    public int getUpgradeMaxStackSize(int slot) {
        return slot == SPEED_UPGRADE_SLOT ? 5 : slot == STACK_UPGRADE_SLOT ? MAX_STACK_UPGRADE_COUNT : 1;
    }

    private static int countActiveRoostersInNests(net.minecraft.world.level.Level level, BlockPos origin, int range) {
        int total = 0;
        int scanRange = Math.max(range, MechanicalNestBlockEntity.getMaximumAuraRange());
        int minChunkX = (origin.getX() - scanRange) >> 4;
        int maxChunkX = (origin.getX() + scanRange) >> 4;
        int minChunkZ = (origin.getZ() - scanRange) >> 4;
        int maxChunkZ = (origin.getZ() + scanRange) >> 4;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                BlockPos probe = new BlockPos(chunkX << 4, origin.getY(), chunkZ << 4);
                if (!level.hasChunkAt(probe)) {
                    continue;
                }
                LevelChunk chunk = level.getChunk(chunkX, chunkZ);
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    BlockPos nestPos = blockEntity.getBlockPos();
                    if (Math.abs(nestPos.getY() - origin.getY()) > 1) {
                        continue;
                    }
                    if (blockEntity instanceof NestBlockEntity nest) {
                        if (nest.hasActiveAura()
                                && Math.abs(origin.getX() - nestPos.getX()) <= range
                                && Math.abs(origin.getZ() - nestPos.getZ()) <= range) {
                            total += Math.max(0, nest.getRoosterCount());
                        }
                    } else if (blockEntity instanceof MechanicalNestBlockEntity nest
                            && nest.hasActiveAura()
                            && Math.abs(origin.getX() - nestPos.getX()) <= nest.getAuraRange()
                            && Math.abs(origin.getZ() - nestPos.getZ()) <= nest.getAuraRange()) {
                        total += Math.max(0, nest.getRoosterCount());
                    }
                }
            }
        }
        return total;
    }

    @Override
    protected int getChickenSlotCount() {
        return 1;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("menu.chickens.roost");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory playerInventory, ContainerData dataAccess) {
        return new RoostMenu(id, playerInventory, this, dataAccess);
    }

    @Override
    protected ChickenContainerEntry createChickenData(int slot, ItemStack stack) {
        if (slot != CHICKEN_SLOT || !ChickenItemHelper.isChicken(stack)) {
            return null;
        }
        ChickensRegistryItem description = ChickenItemHelper.resolve(stack);
        if (description == null) {
            return null;
        }
        ChickenStats stats = ChickenItemHelper.getStats(stack);
        return new ChickenContainerEntry(description, stats, ChickenItemHelper.isRobotChicken(stack));
    }

    @Override
    protected int getMaxStackSizeForSlotWithStackUpgrades(int slot, ItemStack stack, int stackUpgradeCount) {
        if (slot == CHICKEN_SLOT) {
            int max = Math.min(MAX_CHICKENS, stack.getMaxStackSize());
            return Math.min(MAX_VIRTUAL_STACK_SIZE, max << stackUpgradeCount);
        }
        return super.getMaxStackSizeForSlotWithStackUpgrades(slot, stack, stackUpgradeCount);
    }

    @Override
    public boolean canRemoveUpgrade(int slot, int count) {
        return slot != STACK_UPGRADE_SLOT || canRemoveStackUpgrade(count);
    }

    public boolean putChicken(ItemStack newStack) {
        if (!ChickenItemHelper.isChicken(newStack) || level == null) {
            return false;
        }
        ItemStack current = getItem(CHICKEN_SLOT);
        int maxChickens = getMaxStackSizeForSlot(CHICKEN_SLOT, newStack);
        if (current.isEmpty()) {
            int toMove = Math.min(maxChickens, newStack.getCount());
            if (toMove <= 0) {
                return false;
            }
            setItem(CHICKEN_SLOT, newStack.split(toMove));
            playAddSound();
            return true;
        }
        if (!ItemStack.isSameItemSameComponents(current, newStack)) {
            return false;
        }
        int space = maxChickens - current.getCount();
        if (space <= 0) {
            return false;
        }
        int toMove = Math.min(space, newStack.getCount());
        if (toMove <= 0) {
            return false;
        }
        current.grow(toMove);
        newStack.shrink(toMove);
        setChanged();
        playAddSound();
        return true;
    }

    public boolean pullChickenOut(Player player) {
        ItemStack stack = getItem(CHICKEN_SLOT);
        if (stack.isEmpty()) {
            return false;
        }
        ItemStack remaining = stack.copy();
        setItem(CHICKEN_SLOT, ItemStack.EMPTY);
        int maxExternalStackSize = AbstractChickenContainerBlockEntity.getLegalExternalStackSize(stack);
        while (!remaining.isEmpty()) {
            ItemStack toGive = remaining.split(maxExternalStackSize);
            if (!player.addItem(toGive) && !toGive.isEmpty()) {
                player.drop(toGive, false);
            }
        }
        playRemoveSound();
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
        ItemStack stack = getItem(CHICKEN_SLOT);
        if (stack.isEmpty()) {
            return;
        }
        tag.putInt("ChickenId", ChickenItemHelper.getChickenType(stack));
        tag.putInt("ChickenCount", stack.getCount());
        ChickenStats stats = ChickenItemHelper.getStats(stack);
        tag.putInt("Gain", stats.gain());
        tag.putInt("Growth", stats.growth());
        tag.putInt("Strength", stats.strength());
    }

    @Override
    public void appendTooltip(List<Component> tooltip, CompoundTag data) {
        if (data.contains("ChickenId")) {
            ChickensRegistryItem chicken = ChickensRegistry.getByType(data.getInt("ChickenId"));
            if (chicken != null) {
                int chickens = data.getInt("ChickenCount");
                int gain = data.getInt("Gain");
                int growth = data.getInt("Growth");
                int strength = data.getInt("Strength");
                ItemStack drop = chicken.createLayItem();
                if (ChickensConfigHolder.get().isScalingDropsEnabled()) {
                    new ChickenStats(growth, gain, strength, false).scaleOutput(drop);
                }
                tooltip.add(Component.translatable("tooltip.chickens.roost.summary", chicken.getDisplayName(), chickens,
                        drop.getHoverName(), drop.getCount()));
            }
        }
        super.appendTooltip(tooltip, data);
    }
}
