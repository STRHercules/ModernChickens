package com.setycz.chickens.blockentity;

import com.setycz.chickens.menu.HenhouseMenu;
import com.setycz.chickens.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Storage block entity that emulates the Forge 1.10 henhouse logic. It exposes
 * sided inventory access for automation while keeping the energy/hay bale
 * mechanic intact so nearby chickens can funnel eggs inside.
 */
public class HenhouseBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {
    public static final int HAY_SLOT = 0;
    public static final int DIRT_SLOT = 1;
    public static final int FIRST_OUTPUT_SLOT = 2;
    private static final int LAST_OUTPUT_SLOT = 10;
    public static final int SLOT_COUNT = LAST_OUTPUT_SLOT + 1;

    public static final int HAY_BALE_ENERGY = 100;
    private static final double HENHOUSE_RADIUS = 0.5D;
    private static final double FENCE_THRESHOLD = 0.5D;
    private static final double SEARCH_RADIUS = 4.0D + HENHOUSE_RADIUS + FENCE_THRESHOLD;

    private static final int[] UP_SLOTS = new int[] { HAY_SLOT };
    private static final int[] DOWN_SLOTS;
    private static final int[] EMPTY_SLOTS = new int[0];

    static {
        int itemSlotCount = LAST_OUTPUT_SLOT - FIRST_OUTPUT_SLOT + 1;
        DOWN_SLOTS = new int[itemSlotCount + 1];
        DOWN_SLOTS[0] = DIRT_SLOT;
        for (int i = 0; i < itemSlotCount; i++) {
            DOWN_SLOTS[i + 1] = FIRST_OUTPUT_SLOT + i;
        }
    }

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            // Only one field (energy) needs to be synced to the menu progress bar.
            return index == 0 ? energy : 0;
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                energy = value;
            }
        }

        @Override
        public int getCount() {
            return 1;
        }
    };

    private int energy;
    @Nullable
    private Component customName;

    public HenhouseBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HENHOUSE.get(), pos, state);
    }

    /**
     * Attempts to push the provided stack into nearby henhouses. This mirrors the
     * static helper from the legacy tile entity so chickens can deliver eggs
     * before falling back to spawning them in the world.
     */
    public static ItemStack pushItemStack(ItemStack stack, Level level, Vec3 origin) {
        if (stack.isEmpty() || level.isClientSide) {
            return stack;
        }
        List<HenhouseBlockEntity> henhouses = findHenhouses(level, origin, SEARCH_RADIUS);
        ItemStack remaining = stack.copy();
        for (HenhouseBlockEntity henhouse : henhouses) {
            remaining = henhouse.pushIntoInventory(remaining);
            if (remaining.isEmpty()) {
                break;
            }
        }
        return remaining;
    }

    private static List<HenhouseBlockEntity> findHenhouses(Level level, Vec3 origin, double radius) {
        int minX = Mth.floor(origin.x - radius);
        int maxX = Mth.ceil(origin.x + radius);
        int minY = Mth.floor(origin.y - radius);
        int maxY = Mth.ceil(origin.y + radius);
        int minZ = Mth.floor(origin.z - radius);
        int maxZ = Mth.ceil(origin.z + radius);

        List<HenhouseBlockEntity> result = new ArrayList<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    cursor.set(x, y, z);
                    if (!isWithinRange(origin, cursor, radius)) {
                        continue;
                    }
                    BlockEntity blockEntity = level.getBlockEntity(cursor);
                    if (blockEntity instanceof HenhouseBlockEntity henhouse) {
                        result.add(henhouse);
                    }
                }
            }
        }
        result.sort(Comparator.comparingDouble(henhouse -> henhouse.distanceTo(origin)));
        return result;
    }

    private static boolean isWithinRange(Vec3 origin, BlockPos pos, double radius) {
        Vec3 target = Vec3.atLowerCornerOf(pos).add(HENHOUSE_RADIUS, HENHOUSE_RADIUS, HENHOUSE_RADIUS);
        return Math.abs(origin.x - target.x) <= radius && Math.abs(origin.y - target.y) <= radius
                && Math.abs(origin.z - target.z) <= radius;
    }

    private double distanceTo(Vec3 origin) {
        Vec3 target = Vec3.atLowerCornerOf(this.worldPosition).add(HENHOUSE_RADIUS, HENHOUSE_RADIUS, HENHOUSE_RADIUS);
        return target.distanceTo(origin);
    }

    private ItemStack pushIntoInventory(ItemStack stack) {
        ItemStack remaining = stack.copy();
        int capacity = getEffectiveCapacity();
        if (capacity <= 0) {
            return remaining;
        }

        for (int slot = FIRST_OUTPUT_SLOT; slot <= LAST_OUTPUT_SLOT; slot++) {
            ItemStack slotStack = items.get(slot);
            int canAdd = canAdd(slotStack, remaining);
            int willAdd = Math.min(canAdd, capacity);
            if (willAdd <= 0) {
                continue;
            }

            consumeEnergy(willAdd);
            capacity -= willAdd;

            if (slotStack.isEmpty()) {
                ItemStack placed = remaining.split(willAdd);
                items.set(slot, placed);
            } else {
                slotStack.grow(willAdd);
                remaining.shrink(willAdd);
            }

            if (remaining.isEmpty()) {
                setChanged();
                return ItemStack.EMPTY;
            }
        }

        setChanged();
        return remaining;
    }

    private void consumeEnergy(int amount) {
        while (amount > 0) {
            if (energy <= 0) {
                ItemStack hayStack = items.get(HAY_SLOT);
                if (!hayStack.isEmpty() && hayStack.is(Blocks.HAY_BLOCK.asItem())) {
                    hayStack.shrink(1);
                    if (hayStack.isEmpty()) {
                        items.set(HAY_SLOT, ItemStack.EMPTY);
                    }
                    energy += HAY_BALE_ENERGY;
                } else {
                    // Without hay bales we can no longer convert eggs into dirt.
                    break;
                }
            }

            int consumed = Math.min(amount, energy);
            energy -= consumed;
            amount -= consumed;

            if (energy <= 0) {
                ItemStack dirtStack = items.get(DIRT_SLOT);
                if (dirtStack.isEmpty()) {
                    items.set(DIRT_SLOT, new ItemStack(Blocks.DIRT));
                } else if (dirtStack.is(Blocks.DIRT.asItem()) && dirtStack.getCount() < dirtStack.getMaxStackSize()) {
                    dirtStack.grow(1);
                } else {
                    // Unexpected items block dirt production; halt to avoid loops.
                    break;
                }
            }
        }
    }

    private int canAdd(ItemStack slotStack, ItemStack input) {
        if (input.isEmpty()) {
            return 0;
        }
        if (slotStack.isEmpty()) {
            return Math.min(getMaxStackSize(), input.getCount());
        }
        if (!ItemStack.isSameItemSameComponents(slotStack, input)) {
            return 0;
        }
        int limit = Math.min(getMaxStackSize(), slotStack.getMaxStackSize());
        return Math.min(limit - slotStack.getCount(), input.getCount());
    }

    private int getEffectiveCapacity() {
        return Math.min(getInputCapacity(), getOutputCapacity());
    }

    private int getInputCapacity() {
        int potential = energy;
        ItemStack hayStack = items.get(HAY_SLOT);
        if (!hayStack.isEmpty() && hayStack.is(Blocks.HAY_BLOCK.asItem())) {
            potential += hayStack.getCount() * HAY_BALE_ENERGY;
        }
        return potential;
    }

    private int getOutputCapacity() {
        ItemStack dirtStack = items.get(DIRT_SLOT);
        if (dirtStack.isEmpty()) {
            return getMaxStackSize() * HAY_BALE_ENERGY;
        }
        if (!dirtStack.is(Blocks.DIRT.asItem())) {
            return 0;
        }
        return (dirtStack.getMaxStackSize() - dirtStack.getCount()) * HAY_BALE_ENERGY;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        ContainerHelper.saveAllItems(tag, items, provider);
        tag.putInt("Energy", energy);
        if (customName != null) {
            ComponentSerialization.CODEC.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), customName)
                    .result()
                    .ifPresent(serialized -> tag.put("CustomName", serialized));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        ContainerHelper.loadAllItems(tag, items, provider);
        energy = tag.getInt("Energy");
        customName = null;
        if (tag.contains("CustomName")) {
            ComponentSerialization.CODEC.parse(provider.createSerializationContext(NbtOps.INSTANCE), tag.get("CustomName"))
                    .result()
                    .ifPresent(component -> customName = component);
        }
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int index) {
        return items.get(index);
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        ItemStack stack = ContainerHelper.removeItem(items, index, count);
        if (!stack.isEmpty()) {
            setChanged();
        }
        return stack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        ItemStack stack = items.get(index);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        items.set(index, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        items.set(index, stack);
        if (!stack.isEmpty() && stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        if (level == null || level.getBlockEntity(worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D,
                worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < items.size(); i++) {
            items.set(i, ItemStack.EMPTY);
        }
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) {
            return Arrays.copyOf(DOWN_SLOTS, DOWN_SLOTS.length);
        }
        if (side == Direction.UP) {
            return Arrays.copyOf(UP_SLOTS, UP_SLOTS.length);
        }
        return EMPTY_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
        return isItemValid(index, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return true;
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        return isItemValid(index, stack);
    }

    private boolean isItemValid(int index, ItemStack stack) {
        if (index == HAY_SLOT) {
            return stack.is(Blocks.HAY_BLOCK.asItem());
        }
        if (index == DIRT_SLOT) {
            return false;
        }
        return true;
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public Component getDisplayName() {
        return customName != null ? customName : Component.translatable("container.chickens.henhouse");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        // Provide live container data so the GUI progress bar updates in real time.
        return new HenhouseMenu(containerId, inventory, this, dataAccess);
    }

    public ContainerData getDataAccess() {
        return dataAccess;
    }

    public void setCustomName(Component name) {
        customName = name;
    }

    public NonNullList<ItemStack> getItems() {
        // Expose the live inventory list so breaking the block can spill all
        // stored items into the world, just like the legacy implementation.
        return items;
    }

    public int getEnergy() {
        return energy;
    }
}
