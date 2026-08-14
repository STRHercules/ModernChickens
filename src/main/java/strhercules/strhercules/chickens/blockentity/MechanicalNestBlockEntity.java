package strhercules.chickens.blockentity;

import strhercules.chickens.block.MechanicalNestBlock;
import strhercules.chickens.config.ChickensConfigHolder;
import strhercules.chickens.item.ChickenItemHelper;
import strhercules.chickens.registry.ModBlockEntities;
import strhercules.chickens.registry.ModRegistry;
import strhercules.chickens.menu.MechanicalNestMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;

import javax.annotation.Nullable;

/** Stores one rooster and spends RF to maintain a rooster aura. */
public final class MechanicalNestBlockEntity extends BlockEntity
        implements WorldlyContainer, MenuProvider, SideConfigurable {
    public static final int ROOSTER_SLOT = 0;
    public static final int SPEED_UPGRADE_SLOT = 1;
    public static final int RF_UPGRADE_SLOT = 2;
    public static final int INVENTORY_SIZE = 3;
    private static final int MAX_SPEED_UPGRADES = 5;
    private static final int MAX_RF_UPGRADES = 3;
    private static final int INVENTORY_VERSION = 2;
    private static final int DEFAULT_CAPACITY = 100_000;
    private static final int DEFAULT_RECEIVE = 4_000;

    private final NonNullList<ItemStack> items = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private final MachineSideConfig sideConfig = new MachineSideConfig();
    private final EnergyStorage energyStorage = new EnergyStorage(DEFAULT_CAPACITY, DEFAULT_RECEIVE, DEFAULT_RECEIVE) {
        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            int accepted = Math.min(Math.max(amount, 0), Math.min(MechanicalNestBlockEntity.this.maxReceive,
                    MechanicalNestBlockEntity.this.capacity - MechanicalNestBlockEntity.this.energy));
            if (!simulate && accepted > 0) {
                MechanicalNestBlockEntity.this.energy += accepted;
                setChanged();
            }
            return accepted;
        }

        @Override
        public int extractEnergy(int amount, boolean simulate) {
            int extracted = Math.min(Math.max(amount, 0), Math.min(MechanicalNestBlockEntity.this.maxReceive,
                    MechanicalNestBlockEntity.this.energy));
            if (!simulate && extracted > 0) {
                MechanicalNestBlockEntity.this.energy -= extracted;
                setChanged();
            }
            return extracted;
        }

        @Override
        public int getEnergyStored() {
            return MechanicalNestBlockEntity.this.energy;
        }

        @Override
        public int getMaxEnergyStored() {
            return MechanicalNestBlockEntity.this.capacity;
        }
    };

    private int energy;
    private int capacity = DEFAULT_CAPACITY;
    private int maxReceive = DEFAULT_RECEIVE;
    private int activeBoostedRoosts;
    private boolean cachedActive;

    public MechanicalNestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MECHANICAL_NEST.get(), pos, state);
        syncCapacity();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MechanicalNestBlockEntity nest) {
        if (level.isClientSide) {
            return;
        }
        nest.tickServer(level);
    }

    private void tickServer(Level level) {
        syncCapacity();
        pullEnergyFromNeighbors(level);
        int boostedRoosts = getRoosterCount() > 0 ? countActiveBoostedRoosts(level) : 0;
        int energyCost = getEnergyCost(boostedRoosts);
        boolean wasActive = cachedActive;
        boolean active = boostedRoosts > 0 && energy >= energyCost;
        if (active) {
            energy -= energyCost;
            setChanged();
        }
        activeBoostedRoosts = active ? boostedRoosts : 0;
        cachedActive = active;
        if (active != wasActive) {
            updateActiveState(level, active);
            notifyBlockUpdate();
        }
    }

    private void pullEnergyFromNeighbors(Level level) {
        for (Direction direction : Direction.values()) {
            if (!sideConfig.allows(direction, MachineSideConfig.Channel.ENERGY, true)
                    || energy >= capacity) {
                continue;
            }
            IEnergyStorage neighbor = level.getCapability(Capabilities.EnergyStorage.BLOCK,
                    worldPosition.relative(direction), direction.getOpposite());
            if (neighbor == null) {
                continue;
            }
            int amount = neighbor.extractEnergy(Math.min(maxReceive, capacity - energy), true);
            if (amount > 0) {
                int accepted = energyStorage.receiveEnergy(amount, true);
                if (accepted > 0) {
                    energyStorage.receiveEnergy(neighbor.extractEnergy(accepted, false), false);
                }
            }
        }
    }

    private void updateActiveState(Level level, boolean active) {
        BlockState state = getBlockState();
        if (state.hasProperty(MechanicalNestBlock.LIT) && state.getValue(MechanicalNestBlock.LIT) != active) {
            level.setBlock(worldPosition, state.setValue(MechanicalNestBlock.LIT, active), Block.UPDATE_CLIENTS);
        }
    }

    public int getRoosterCount() {
        return ChickenItemHelper.isRobotRooster(items.get(ROOSTER_SLOT)) ? 1 : 0;
    }

    public boolean hasActiveAura() {
        return cachedActive && getRoosterCount() > 0;
    }

    public int getActiveBoostedRoostCount() {
        return hasActiveAura() ? activeBoostedRoosts : 0;
    }

    public int getAuraRange() {
        return Math.max(0, ChickensConfigHolder.get().getMechanicalNestRange());
    }

    static int getMaximumAuraRange() {
        return Math.max(0, ChickensConfigHolder.get().getMechanicalNestRange());
    }

    public double getBoostMultiplier() {
        return ChickensConfigHolder.get().getRoosterAuraMultiplier()
                * (1.0D + 0.2D * getUpgradeCount(SPEED_UPGRADE_SLOT));
    }

    public double getEffectiveBoostMultiplier() {
        if (!hasActiveAura()) {
            return 1.0D;
        }
        return Math.max(1.0D, getBoostMultiplier());
    }

    public int getConflictingActiveNestCount() {
        if (level == null) {
            return 0;
        }
        int range = getAuraRange();
        if (range <= 0) {
            return 0;
        }
        int conflicts = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -range; dz <= range; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    cursor.set(worldPosition.getX() + dx, worldPosition.getY() + dy, worldPosition.getZ() + dz);
                    BlockEntity other = level.getBlockEntity(cursor);
                    if (other instanceof NestBlockEntity nest && nest.hasActiveAura()
                            || other instanceof MechanicalNestBlockEntity mechanical && mechanical.hasActiveAura()) {
                        conflicts++;
                    }
                }
            }
        }
        return conflicts;
    }

    public int getEnergyCost() {
        return hasActiveAura() ? getEnergyCost(activeBoostedRoosts) : 0;
    }

    private int getEnergyCost(int boostedRoosts) {
        if (boostedRoosts <= 0 || getRoosterCount() <= 0) {
            return 0;
        }
        var config = ChickensConfigHolder.get();
        long baseCost = (long) Math.max(0, config.getMechanicalNestBaseEnergyPerTick())
                + (long) Math.max(0, config.getMechanicalNestEnergyPerRoostPerTick()) * boostedRoosts;
        double speedMultiplier = 1.0D + Math.max(0.0D,
                config.getMechanicalNestEnergyCostSpeedIncrease()) * getUpgradeCount(SPEED_UPGRADE_SLOT);
        long cost = Math.round(baseCost * speedMultiplier);
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1L, cost));
    }

    private int countActiveBoostedRoosts(Level level) {
        int range = getAuraRange();
        if (range <= 0) {
            return 0;
        }
        int total = 0;
        int minChunkX = (worldPosition.getX() - range) >> 4;
        int maxChunkX = (worldPosition.getX() + range) >> 4;
        int minChunkZ = (worldPosition.getZ() - range) >> 4;
        int maxChunkZ = (worldPosition.getZ() + range) >> 4;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                BlockPos probe = new BlockPos(chunkX << 4, worldPosition.getY(), chunkZ << 4);
                if (!level.hasChunkAt(probe)) {
                    continue;
                }
                LevelChunk chunk = level.getChunk(chunkX, chunkZ);
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    BlockPos roostPos = blockEntity.getBlockPos();
                    if (Math.abs(roostPos.getY() - worldPosition.getY()) > 1
                            || Math.abs(roostPos.getX() - worldPosition.getX()) > range
                            || Math.abs(roostPos.getZ() - worldPosition.getZ()) > range) {
                        continue;
                    }
                    if (blockEntity instanceof RoostBlockEntity roost
                            && roost.getProgressIncrementPerTick() > 0) {
                        total++;
                    } else if (blockEntity instanceof MechanicalRoostBlockEntity roost
                            && roost.getProgressIncrementPerTick() > 0) {
                        total++;
                    }
                }
            }
        }
        return total;
    }

    public int getEnergyStored() {
        return energy;
    }

    public int getEnergyCapacity() {
        return capacity;
    }

    public int getComparatorOutput() {
        return capacity <= 0 ? 0 : Math.round(15.0F * energy / capacity);
    }

    public IEnergyStorage getEnergyStorage(@Nullable Direction direction) {
        return MachineCapabilityWrappers.energy(energyStorage, sideConfig, direction);
    }

    public int getUpgradeCount(int slot) {
        return slot >= 0 && slot < INVENTORY_SIZE ? items.get(slot).getCount() : 0;
    }

    public int getUpgradeMaxStackSize(int slot) {
        return switch (slot) {
            case SPEED_UPGRADE_SLOT -> MAX_SPEED_UPGRADES;
            case RF_UPGRADE_SLOT -> MAX_RF_UPGRADES;
            default -> 1;
        };
    }

    public boolean canPlaceUpgrade(int slot, ItemStack stack) {
        return slot == SPEED_UPGRADE_SLOT && stack.is(ModRegistry.SPEED_UPGRADE.get())
                || slot == RF_UPGRADE_SLOT && stack.is(ModRegistry.RF_UPGRADE.get());
    }

    public boolean canInsertRooster(ItemStack stack) {
        return ChickenItemHelper.isRobotRooster(stack) && items.get(ROOSTER_SLOT).isEmpty();
    }

    public boolean putRooster(ItemStack stack) {
        if (!canInsertRooster(stack)) {
            return false;
        }
        setItem(ROOSTER_SLOT, stack.split(1));
        return true;
    }

    public boolean pullRoosterOut(Player player) {
        ItemStack rooster = items.get(ROOSTER_SLOT);
        if (rooster.isEmpty()) {
            return false;
        }
        items.set(ROOSTER_SLOT, ItemStack.EMPTY);
        if (!player.addItem(rooster.copy())) {
            player.drop(rooster.copy(), false);
        }
        activeBoostedRoosts = 0;
        cachedActive = false;
        setChanged();
        notifyBlockUpdate();
        return true;
    }

    @Override
    public MachineSideConfig sideConfig() {
        return sideConfig;
    }

    @Override
    public int getContainerSize() {
        return INVENTORY_SIZE;
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int index) {
        return index >= 0 && index < INVENTORY_SIZE ? items.get(index) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        if (index != ROOSTER_SLOT && !canRemoveUpgrade(index, count)) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = ContainerHelper.removeItem(items, index, count);
        if (!removed.isEmpty()) {
            activeBoostedRoosts = 0;
            cachedActive = false;
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        return removeItem(index, getItem(index).getCount());
    }

    private boolean canRemoveUpgrade(int slot, int count) {
        return slot >= SPEED_UPGRADE_SLOT && slot <= RF_UPGRADE_SLOT && count > 0;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        if (index < 0 || index >= INVENTORY_SIZE) {
            return;
        }
        if (level != null && level.isClientSide) {
            items.set(index, stack.copy());
            return;
        }
        if (index == ROOSTER_SLOT) {
            if (!stack.isEmpty() && (!ChickenItemHelper.isRobotRooster(stack) || !items.get(index).isEmpty())) {
                return;
            }
            if (!stack.isEmpty()) {
                stack = stack.copyWithCount(1);
            }
        } else {
            if (!stack.isEmpty() && !canPlaceUpgrade(index, stack)) {
                return;
            }
            if (!stack.isEmpty()) {
                stack = stack.copyWithCount(Math.min(stack.getCount(), getUpgradeMaxStackSize(index)));
            }
        }
        items.set(index, stack);
        activeBoostedRoosts = 0;
        cachedActive = false;
        syncCapacity();
        setChanged();
        notifyBlockUpdate();
    }

    @Override
    public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D,
                        worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        return index == ROOSTER_SLOT ? ChickenItemHelper.isRobotRooster(stack) && items.get(index).isEmpty()
                : canPlaceUpgrade(index, stack);
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
        return index == ROOSTER_SLOT
                && sideConfig.allows(direction, MachineSideConfig.Channel.ITEMS, true)
                && canInsertRooster(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return index == ROOSTER_SLOT && sideConfig.allows(direction, MachineSideConfig.Channel.ITEMS, false);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return sideConfig.allows(side, MachineSideConfig.Channel.ITEMS, true)
                || sideConfig.allows(side, MachineSideConfig.Channel.ITEMS, false)
                ? new int[] { ROOSTER_SLOT } : new int[0];
    }

    @Override
    public void clearContent() {
        for (int index = 0; index < items.size(); index++) {
            items.set(index, ItemStack.EMPTY);
        }
        energy = 0;
        activeBoostedRoosts = 0;
        cachedActive = false;
        setChanged();
    }

    public NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.chickens.mechanical_nest");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new MechanicalNestMenu(id, inventory, this);
    }

    private void syncCapacity() {
        int configured = Math.max(DEFAULT_CAPACITY, ChickensConfigHolder.get().getIncubatorEnergyCapacity());
        int rfUpgrades = Math.min(MAX_RF_UPGRADES, getUpgradeCount(RF_UPGRADE_SLOT));
        capacity = (int) Math.min(Integer.MAX_VALUE, (long) configured * (1L << rfUpgrades));
        maxReceive = Math.max(DEFAULT_RECEIVE, ChickensConfigHolder.get().getIncubatorEnergyMaxReceive());
        energy = Mth.clamp(energy, 0, capacity);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        ContainerHelper.saveAllItems(tag, items, provider);
        tag.putInt("InventoryVersion", INVENTORY_VERSION);
        tag.putInt("Energy", energy);
        sideConfig.save(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        if (tag.getInt("InventoryVersion") >= INVENTORY_VERSION) {
            ContainerHelper.loadAllItems(tag, items, provider);
        } else {
            // Version 1 stored Range Upgrades in slot 2 and RF Upgrades in
            // slot 3. Preserve the rooster, speed, and RF contents while
            // dropping the no-longer-supported range slot.
            NonNullList<ItemStack> legacyItems = NonNullList.withSize(4, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(tag, legacyItems, provider);
            items.set(ROOSTER_SLOT, legacyItems.get(ROOSTER_SLOT));
            items.set(SPEED_UPGRADE_SLOT, legacyItems.get(SPEED_UPGRADE_SLOT));
            items.set(RF_UPGRADE_SLOT, legacyItems.get(3));
        }
        sideConfig.load(tag);
        energy = Math.max(0, tag.getInt("Energy"));
        activeBoostedRoosts = 0;
        syncCapacity();
        cachedActive = false;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, provider);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider provider) {
        loadAdditional(tag, provider);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet,
            HolderLookup.Provider provider) {
        CompoundTag tag = packet.getTag();
        if (tag != null) {
            loadAdditional(tag, provider);
        }
    }

    private void notifyBlockUpdate() {
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }
}
