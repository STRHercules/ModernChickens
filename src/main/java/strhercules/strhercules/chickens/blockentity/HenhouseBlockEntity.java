package strhercules.chickens.blockentity;

import strhercules.chickens.menu.HenhouseMenu;
import strhercules.chickens.registry.ModBlockEntities;
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
import java.util.List;

import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.minecraft.world.level.block.Block;

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
    public static final int ENERGY_CAPACITY = HAY_BALE_ENERGY * 64;
    private static final double HENHOUSE_RADIUS = 0.5D;
    private static final double FENCE_THRESHOLD = 0.5D;
    private static final double MAX_ENTITY_RADIUS = 2.0D;
    private static final double BASE_RADIUS = 4.0D;
    private static final double SEARCH_RADIUS = BASE_RADIUS + HENHOUSE_RADIUS + FENCE_THRESHOLD;

    private static final int[] IO_SLOTS = new int[] {
            HAY_SLOT, DIRT_SLOT, FIRST_OUTPUT_SLOT, FIRST_OUTPUT_SLOT + 1,
            FIRST_OUTPUT_SLOT + 2, FIRST_OUTPUT_SLOT + 3, FIRST_OUTPUT_SLOT + 4,
            FIRST_OUTPUT_SLOT + 5, FIRST_OUTPUT_SLOT + 6, FIRST_OUTPUT_SLOT + 7,
            LAST_OUTPUT_SLOT
    };

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
                energy = Mth.clamp(value, 0, ENERGY_CAPACITY);
                hayEnergy = Math.min(hayEnergy, energy);
            }
        }

        @Override
        public int getCount() {
            return 1;
        }
    };

    private int energy;
    /** Portion of the shared energy buffer that came from spent hay bales. */
    private int hayEnergy;
    private final IEnergyStorage energyStorage = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            int received = Math.min(Math.max(amount, 0), ENERGY_CAPACITY - energy);
            if (received > 0 && !simulate) {
                energy += received;
                sync();
            }
            return received;
        }

        @Override
        public int extractEnergy(int amount, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return energy;
        }

        @Override
        public int getMaxEnergyStored() {
            return ENERGY_CAPACITY;
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    };
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
        int minX = Mth.floor((origin.x - radius - MAX_ENTITY_RADIUS));
        int maxX = Mth.ceil((origin.x + radius + MAX_ENTITY_RADIUS));
        int minY = Mth.floor((origin.y - radius - MAX_ENTITY_RADIUS));
        int maxY = Mth.ceil((origin.y + radius + MAX_ENTITY_RADIUS));
        int minZ = Mth.floor((origin.z - radius - MAX_ENTITY_RADIUS));
        int maxZ = Mth.ceil((origin.z + radius + MAX_ENTITY_RADIUS));

        List<Double> distances = new ArrayList<>();
        List<HenhouseBlockEntity> result = new ArrayList<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    cursor.set(x, y, z);
                    BlockEntity blockEntity = level.getBlockEntity(cursor);
                    if (!(blockEntity instanceof HenhouseBlockEntity henhouse)) {
                        continue;
                    }
                    Vec3 target = Vec3.atLowerCornerOf(cursor).add(HENHOUSE_RADIUS, HENHOUSE_RADIUS, HENHOUSE_RADIUS);
                    if (!isWithinRange(origin, target, radius)) {
                        continue;
                    }
                    double distance = target.distanceTo(origin);
                    insertSorted(result, distances, henhouse, distance);
                }
            }
        }
        return result;
    }

    private static boolean isWithinRange(Vec3 origin, Vec3 target, double radius) {
        return Math.abs(origin.x - target.x) <= radius
                && Math.abs(origin.y - target.y) <= radius
                && Math.abs(origin.z - target.z) <= radius;
    }

    private static void insertSorted(List<HenhouseBlockEntity> henhouses, List<Double> distances,
            HenhouseBlockEntity candidate, double distance) {
        for (int i = 0; i < distances.size(); i++) {
            if (distance < distances.get(i)) {
                distances.add(i, distance);
                henhouses.add(i, candidate);
                return;
            }
        }
        distances.add(distance);
        henhouses.add(candidate);
    }

    private double distanceTo(Vec3 origin) {
        Vec3 target = Vec3.atLowerCornerOf(this.worldPosition).add(HENHOUSE_RADIUS, HENHOUSE_RADIUS, HENHOUSE_RADIUS);
        return target.distanceTo(origin);
    }

    private static boolean isHayFuel(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.is(Blocks.HAY_BLOCK.asItem()) || stack.is(Tags.Items.STORAGE_BLOCKS_WHEAT));
    }

    private ItemStack pushIntoInventory(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack remaining = stack.copy();
        int capacity = getEffectiveCapacity();
        if (capacity <= 0) {
            return remaining;
        }

        boolean modified = false;
        for (int slot = FIRST_OUTPUT_SLOT; slot <= LAST_OUTPUT_SLOT && capacity > 0 && !remaining.isEmpty(); slot++) {
            ItemStack slotStack = items.get(slot);
            int canAdd = canAdd(slotStack, remaining);
            if (canAdd <= 0) {
                continue;
            }
            int willAdd = Math.min(canAdd, capacity);
            if (willAdd <= 0) {
                continue;
            }

            if (!consumeEnergy(willAdd)) {
                break;
            }
            capacity -= willAdd;
            modified = true;

            if (slotStack.isEmpty()) {
                ItemStack placed = remaining.split(willAdd);
                items.set(slot, placed);
            } else {
                slotStack.grow(willAdd);
                remaining.shrink(willAdd);
            }
        }

        if (modified) {
            sync();
        }
        return remaining.isEmpty() ? ItemStack.EMPTY : remaining;
    }

    private boolean consumeEnergy(int amount) {
        if (amount <= 0) {
            return true;
        }
        // Refuse the whole transfer when the available energy or dirt capacity
        // cannot satisfy it. This keeps a failed insertion from consuming only
        // part of its fuel and then dropping the item into the world.
        if (amount > getEffectiveCapacity()) {
            return false;
        }

        int remaining = amount;
        boolean modified = false;
        while (remaining > 0) {
            if (energy == 0) {
                if (!chargeFromHay()) {
                    break;
                }
                modified = true;
            }

            // FE is consumed before hay energy. That makes the source boundary
            // deterministic and prevents FE-only operation from creating dirt.
            int externalEnergy = Math.max(energy - hayEnergy, 0);
            int consumed = Math.min(remaining, externalEnergy);
            if (consumed > 0) {
                energy -= consumed;
                remaining -= consumed;
                modified = true;
                continue;
            }

            if (hayEnergy <= 0) {
                break;
            }

            consumed = Math.min(remaining, hayEnergy);
            int hayEnergyBefore = hayEnergy;
            hayEnergy -= consumed;
            energy -= consumed;
            remaining -= consumed;

            int dirtCount = completedHayBales(hayEnergyBefore, hayEnergy);
            if (dirtCount > 0) {
                addDirt(dirtCount);
            }
            modified = true;
        }
        if (modified) {
            sync();
        }
        return remaining == 0;
    }

    private static int completedHayBales(int energyBefore, int energyAfter) {
        return representedHayBales(energyBefore) - representedHayBales(energyAfter);
    }

    private static int representedHayBales(int hayEnergy) {
        if (hayEnergy <= 0) {
            return 0;
        }
        return (hayEnergy + HAY_BALE_ENERGY - 1) / HAY_BALE_ENERGY;
    }

    private void addDirt(int count) {
        ItemStack dirtStack = items.get(DIRT_SLOT);
        if (dirtStack.isEmpty()) {
            items.set(DIRT_SLOT, new ItemStack(Blocks.DIRT, count));
        } else {
            dirtStack.grow(count);
        }
    }

    private boolean chargeFromHay() {
        ItemStack hayStack = items.get(HAY_SLOT);
        if (!isHayFuel(hayStack)) {
            return false;
        }
        hayStack.shrink(1);
        if (hayStack.isEmpty()) {
            items.set(HAY_SLOT, ItemStack.EMPTY);
        }
        int charged = Math.min(HAY_BALE_ENERGY, ENERGY_CAPACITY - energy);
        energy += charged;
        hayEnergy += charged;
        return charged > 0;
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
        int externalEnergy = Math.max(energy - hayEnergy, 0);
        int hayPotential = hayEnergy;
        ItemStack hayStack = items.get(HAY_SLOT);
        if (isHayFuel(hayStack)) {
            hayPotential += hayStack.getCount() * HAY_BALE_ENERGY;
        }
        // Only hay energy needs reserved dirt space. FE remains usable even
        // when the dirt byproduct slot is full.
        return externalEnergy + Math.min(hayPotential, getOutputCapacity());
    }

    private int getOutputCapacity() {
        return getAvailableDirtSlots() * HAY_BALE_ENERGY;
    }

    private int getAvailableDirtSlots() {
        ItemStack dirtStack = items.get(DIRT_SLOT);
        if (dirtStack.isEmpty()) {
            return getMaxStackSize();
        }
        if (!dirtStack.is(Blocks.DIRT.asItem())) {
            return 0;
        }
        int stackLimit = Math.min(getMaxStackSize(), dirtStack.getMaxStackSize());
        return Math.max(stackLimit - dirtStack.getCount(), 0);
    }

    private void sync() {
        setChanged();
        Level level = getLevel();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        ContainerHelper.saveAllItems(tag, items, provider);
        tag.putInt("Energy", energy);
        tag.putInt("HayEnergy", hayEnergy);
        if (customName != null) {
            ComponentSerialization.CODEC.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), customName)
                    .result()
                    .ifPresent(serialized -> tag.put("CustomName", serialized));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        ContainerHelper.loadAllItems(tag, items, provider);
        energy = Mth.clamp(tag.getInt("Energy"), 0, ENERGY_CAPACITY);
        hayEnergy = Mth.clamp(tag.getInt("HayEnergy"), 0, energy);
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
            sync();
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
        sync();
        return stack;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        items.set(index, stack);
        if (!stack.isEmpty() && stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        sync();
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
        sync();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return IO_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
        return isItemValid(index, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return index == DIRT_SLOT || index >= FIRST_OUTPUT_SLOT;
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        return isItemValid(index, stack);
    }

    private boolean isItemValid(int index, ItemStack stack) {
        // Hay is the only player/automation input. Dirt and the 3x3 grid are
        // machine-owned outputs and must not accept arbitrary inserted items.
        return index == HAY_SLOT && isHayFuel(stack);
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

    public IEnergyStorage getEnergyStorage(@Nullable Direction direction) {
        return energyStorage;
    }
}
