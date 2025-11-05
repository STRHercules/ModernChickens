package com.modernfluidcows.blockentity;

import com.modernfluidcows.config.FCConfig;
import com.modernfluidcows.entity.FluidCow;
import com.modernfluidcows.menu.AcceleratorMenu;
import com.modernfluidcows.registry.FluidCowsRegistries;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Clearable;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

/**
 * Block entity backing the accelerator machine.
 *
 * <p>It mirrors the legacy behaviour: wheat is consumed alongside water to
 * accumulate "substance" that accelerates nearby {@link FluidCow} growth.</p>
 */
public class AcceleratorBlockEntity extends BlockEntity implements Clearable, MenuProvider {
    private static final String TAG_TANK = "Tank";
    private static final String TAG_SUBSTANCE = "Substance";

    public static final int SLOT_WHEAT = 0;
    public static final int SLOT_INPUT = 1;
    public static final int SLOT_OUTPUT = 2;
    private static final int SLOT_COUNT = 3;

    public static final int TANK_CAPACITY = FluidType.BUCKET_VOLUME * 10;
    public static final int MAX_SUBSTANCE = FluidType.BUCKET_VOLUME * 10;
    private static final int WATER_BOTTLE_VOLUME = FluidType.BUCKET_VOLUME / 4;

    private final SimpleContainer inventory =
            new SimpleContainer(SLOT_COUNT) {
                @Override
                public boolean canPlaceItem(final int slot, final ItemStack stack) {
                    if (slot == SLOT_WHEAT) {
                        return stack.is(Items.WHEAT);
                    }
                    if (slot == SLOT_INPUT) {
                        return FluidUtil.getFluidHandler(stack)
                                .map(handler ->
                                        handler.drain(FluidType.BUCKET_VOLUME, FluidAction.SIMULATE)
                                                        .getFluid()
                                                == Fluids.WATER)
                                .orElse(false);
                    }
                    return false;
                }

                @Override
                public void setChanged() {
                    super.setChanged();
                    setChangedAndSync();
                }
            };

    private final FluidTank tank = new FluidTank(TANK_CAPACITY) {
        @Override
        protected void onContentsChanged() {
            super.onContentsChanged();
            setChangedAndSync();
        }
    };

    private final ContainerData dataAccess =
            new ContainerData() {
                @Override
                public int get(final int index) {
                    return switch (index) {
                        case 0 -> tank.getFluidAmount();
                        case 1 -> currentWheatSubstance;
                        default -> 0;
                    };
                }

                @Override
                public void set(final int index, final int value) {
                    if (index == 1) {
                        currentWheatSubstance = value;
                    }
                }

                @Override
                public int getCount() {
                    return 2;
                }
            };

    private int currentWheatSubstance;

    public AcceleratorBlockEntity(final BlockPos pos, final BlockState state) {
        super(FluidCowsRegistries.ACCELERATOR_BLOCK_ENTITY.get(), pos, state);
    }

    public SimpleContainer getInventory() {
        return inventory;
    }

    public FluidTank getTank() {
        return tank;
    }

    public int getCurrentWheatSubstance() {
        return currentWheatSubstance;
    }

    public ContainerData getDataAccess() {
        return dataAccess;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.fluidcows.accelerator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(
            final int id, final Inventory playerInventory, final Player player) {
        return new AcceleratorMenu(
                id,
                playerInventory,
                this,
                inventory,
                ContainerLevelAccess.create(level, worldPosition),
                dataAccess);
    }

    /**
     * Inserts a water bottle into the internal tank, returning the empty glass bottle.
     */
    public boolean tryConsumeWaterBottle(final Player player, final ItemStack bottle) {
        if (tank.fill(new FluidStack(Fluids.WATER, WATER_BOTTLE_VOLUME), FluidAction.SIMULATE)
                != WATER_BOTTLE_VOLUME) {
            return false;
        }

        tank.fill(new FluidStack(Fluids.WATER, WATER_BOTTLE_VOLUME), FluidAction.EXECUTE);
        setChangedAndSync();

        if (!player.getAbilities().instabuild) {
            bottle.shrink(1);
        }
        ItemStack empty = new ItemStack(Items.GLASS_BOTTLE);
        if (!player.addItem(empty)) {
            player.drop(empty, false);
        }
        return true;
    }

    public static void serverTick(
            final Level level, final BlockPos pos, final BlockState state, final AcceleratorBlockEntity accelerator) {
        accelerator.transferInputFluid();
        if (level.hasNeighborSignal(pos)) {
            return;
        }
        accelerator.consumeWheat(level);
        accelerator.boostNearbyCows(level);
    }

    public static void clientTick(
            final Level level, final BlockPos pos, final BlockState state, final AcceleratorBlockEntity accelerator) {
        // Client syncing is handled through ContainerData and packets; nothing required per tick.
    }

    private void transferInputFluid() {
        ItemStack input = inventory.getItem(SLOT_INPUT);
        if (input.isEmpty() || !inventory.getItem(SLOT_OUTPUT).isEmpty()) {
            return;
        }
        Optional<IFluidHandlerItem> handlerOptional = FluidUtil.getFluidHandler(input.copy());
        if (handlerOptional.isEmpty()) {
            return;
        }
        IFluidHandlerItem handler = handlerOptional.get();
        FluidStack drained = handler.drain(new FluidStack(Fluids.WATER, FluidType.BUCKET_VOLUME), FluidAction.SIMULATE);
        if (drained.isEmpty() || drained.getFluid() != Fluids.WATER) {
            return;
        }
        if (tank.fill(drained, FluidAction.SIMULATE) != drained.getAmount()) {
            return;
        }

        handler.drain(drained, FluidAction.EXECUTE);
        tank.fill(drained, FluidAction.EXECUTE);

        ItemStack remainder = handler.getContainer().copy();
        inventory.removeItem(SLOT_INPUT, 1);
        inventory.setItem(SLOT_OUTPUT, remainder);
        setChangedAndSync();
    }

    private void consumeWheat(final Level level) {
        ItemStack wheat = inventory.getItem(SLOT_WHEAT);
        if (wheat.isEmpty() || currentWheatSubstance >= MAX_SUBSTANCE) {
            return;
        }

        int toConsume = Math.min(wheat.getCount(), 8);
        if (toConsume <= 0) {
            return;
        }

        int middle = Math.max(1, FCConfig.acceleratorMax / 2);
        boolean hasWater = tank.getFluidAmount() >= FCConfig.acceleratorWater;
        if (hasWater) {
            int waterLimited = tank.getFluidAmount() / Math.max(1, FCConfig.acceleratorWater);
            toConsume = Math.min(Math.min(toConsume, waterLimited), 32);
        }

        if (toConsume <= 0) {
            return;
        }

        int potential = hasWater ? toConsume * FCConfig.acceleratorMax : toConsume * middle;
        if (currentWheatSubstance + potential > MAX_SUBSTANCE) {
            if (currentWheatSubstance + (hasWater ? FCConfig.acceleratorMax : middle) > MAX_SUBSTANCE) {
                return;
            }
            toConsume = 1;
            potential = hasWater ? FCConfig.acceleratorMax : middle;
        }

        if (hasWater) {
            int drainedWater = toConsume * FCConfig.acceleratorWater;
            tank.drain(drainedWater, FluidAction.EXECUTE);
            currentWheatSubstance = Math.min(MAX_SUBSTANCE, currentWheatSubstance + toConsume * middle);
        }

        int bonus = level.random.nextInt(Math.max(1, toConsume * middle)) + 1;
        currentWheatSubstance = Math.min(MAX_SUBSTANCE, currentWheatSubstance + bonus);

        inventory.removeItem(SLOT_WHEAT, toConsume);
        setChangedAndSync();
    }

    private void boostNearbyCows(final Level level) {
        if (currentWheatSubstance <= FCConfig.acceleratorPerCow) {
            return;
        }
        AABB bounds = new AABB(worldPosition).inflate(FCConfig.acceleratorRadius);
        for (FluidCow cow : level.getEntitiesOfClass(FluidCow.class, bounds)) {
            if (cow.tryAccelerateGrowth()) {
                currentWheatSubstance = Math.max(0, currentWheatSubstance - FCConfig.acceleratorPerCow);
                setChangedAndSync();
                if (currentWheatSubstance < FCConfig.acceleratorPerCow) {
                    break;
                }
            }
        }
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, provider);
        for (int i = 0; i < items.size(); i++) {
            inventory.setItem(i, items.get(i));
        }
        if (tag.contains(TAG_TANK)) {
            tank.readFromNBT(provider, tag.getCompound(TAG_TANK));
        }
        currentWheatSubstance = tag.getInt(TAG_SUBSTANCE);
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        for (int i = 0; i < items.size(); i++) {
            items.set(i, inventory.getItem(i));
        }
        ContainerHelper.saveAllItems(tag, items, provider);
        tag.put(TAG_TANK, tank.writeToNBT(provider, new CompoundTag()));
        tag.putInt(TAG_SUBSTANCE, currentWheatSubstance);
    }

    @Override
    public void handleUpdateTag(final CompoundTag tag, final HolderLookup.Provider provider) {
        loadAdditional(tag, provider);
    }

    @Override
    public CompoundTag getUpdateTag(final HolderLookup.Provider provider) {
        return saveWithoutMetadata(provider);
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void clearContent() {
        inventory.clearContent();
        tank.setFluid(FluidStack.EMPTY);
        currentWheatSubstance = 0;
    }

    private void setChangedAndSync() {
        setChanged();
        Level level = getLevel();
        if (level != null) {
            BlockState state = level.getBlockState(worldPosition);
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }
}
