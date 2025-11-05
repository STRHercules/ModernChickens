package com.modernfluidcows.blockentity;

import com.modernfluidcows.config.FCConfig;
import com.modernfluidcows.menu.StallMenu;
import com.modernfluidcows.entity.FluidCow;
import com.modernfluidcows.registry.FluidCowsRegistries;
import com.modernfluidcows.util.FCUtils;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Clearable;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import org.jetbrains.annotations.Nullable;

/**
 * Block entity that captures a single {@link FluidCow} and bottles its fluid over time.
 */
public class StallBlockEntity extends BlockEntity implements Clearable, MenuProvider {
    private static final String TAG_ITEMS = "Items";
    private static final String TAG_STORED = "StoredCow";
    private static final String TAG_COOLDOWN = "Cooldown";
    private static final String TAG_TANK_AMOUNT = "TankAmount";
    private static final String TAG_TANK_FLUID = "TankFluid";

    public static final int TANK_CAPACITY = FluidType.BUCKET_VOLUME * 10;
    private static final int SLOT_INPUT = 0;
    private static final int SLOT_OUTPUT = 1;

    private final SimpleContainer inventory =
            new SimpleContainer(2) {
                @Override
                public boolean canPlaceItem(final int slot, final ItemStack stack) {
                    return slot == SLOT_INPUT && FluidUtil.getFluidHandler(stack).isPresent();
                }

                @Override
                public void setChanged() {
                    super.setChanged();
                    setChangedAndSync();
                }
            };

    private CompoundTag storedCow;
    private Fluid storedFluid = Fluids.EMPTY;
    private int tankAmount;
    private int cooldownTicks;

    /**
     * Dedicated fluid handler that exposes the stall's internal tank to automation while keeping the
     * legacy block entity state authoritative.
     */
    private final IFluidHandler fluidHandler = new StallFluidHandler();

    /**
     * ContainerData bridge used by the {@link com.modernfluidcows.menu.StallMenu} to mirror the
     * tank contents and cooldown timer to the client screen. NeoForge automatically relays changes
     * written through {@link ContainerData#set(int, int)} so both sides stay in sync.
     */
    private final ContainerData dataAccess =
            new ContainerData() {
                @Override
                public int get(final int index) {
                    return switch (index) {
                        case 0 -> tankAmount;
                        case 1 -> cooldownTicks;
                        default -> 0;
                    };
                }

                @Override
                public void set(final int index, final int value) {
                    switch (index) {
                        case 0 -> tankAmount = value;
                        case 1 -> cooldownTicks = value;
                        default -> {}
                    }
                }

                @Override
                public int getCount() {
                    return 2;
                }
            };

    public StallBlockEntity(final BlockPos pos, final BlockState state) {
        super(FluidCowsRegistries.STALL_BLOCK_ENTITY.get(), pos, state);
    }

    public SimpleContainer getInventory() {
        return inventory;
    }

    /** Returns a defensive copy of the stored cow payload for safe client-side use. */
    public Optional<CompoundTag> getStoredCowData() {
        return storedCow == null ? Optional.empty() : Optional.of(storedCow.copy());
    }

    public Fluid getStoredFluid() {
        return storedFluid;
    }

    public int getTankAmount() {
        return tankAmount;
    }

    public int getCooldown() {
        return cooldownTicks;
    }

    public boolean hasCow() {
        return storedCow != null;
    }

    public ContainerData getDataAccess() {
        return dataAccess;
    }

    public boolean insertCow(final CompoundTag halterData) {
        if (halterData == null || hasCow()) {
            return false;
        }
        storedCow = halterData.copy();
        storedFluid = resolveFluid(storedCow);
        if (storedFluid == Fluids.EMPTY) {
            // Fall back to a deterministic fluid so corrupted halter data still inserts a cow.
            storedFluid = Optional.ofNullable(FCUtils.getRandFluid()).orElse(Fluids.WATER);
        }
        updateStoredCowFluidTag();
        if (storedFluid == Fluids.EMPTY) {
            storedCow = null;
            return false;
        }
        cooldownTicks = Math.max(0, storedCow.getInt(FluidCow.HALTER_TAG_COOLDOWN));
        syncCowState(true);
        setChangedAndSync();
        return true;
    }

    public Optional<CompoundTag> extractCow() {
        if (!hasCow()) {
            return Optional.empty();
        }
        CompoundTag data = storedCow.copy();
        data.putInt(FluidCow.HALTER_TAG_COOLDOWN, cooldownTicks);
        data.putString(FluidCow.HALTER_TAG_FLUID, BuiltInRegistries.FLUID.getKey(storedFluid).toString());
        clearCow();
        return Optional.of(data);
    }

    public void spawnCow(final Level level) {
        if (!hasCow() || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        FluidCow cow = FluidCowsRegistries.FLUID_COW.get().create(serverLevel);
        if (cow == null) {
            return;
        }
        cow.readHalterData(storedCow);
        BlockPos pos = getBlockPos();
        cow.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
        serverLevel.addFreshEntity(cow);
        clearCow();
    }

    public boolean giveOutput(final Player player) {
        ItemStack output = inventory.getItem(SLOT_OUTPUT);
        if (output.isEmpty()) {
            return false;
        }
        ItemStack copy = output.copy();
        if (!player.addItem(copy)) {
            player.drop(copy, false);
        }
        inventory.setItem(SLOT_OUTPUT, ItemStack.EMPTY);
        return true;
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state, final StallBlockEntity stall) {
        if (!stall.hasCow()) {
            return;
        }
        if (stall.cooldownTicks > 0) {
            stall.cooldownTicks--;
        }
        if (stall.cooldownTicks <= 0) {
            stall.produceFluid();
        }
        stall.fillBuckets();
    }

    public static void clientTick(final Level level, final BlockPos pos, final BlockState state, final StallBlockEntity stall) {
        if (stall.cooldownTicks > 0) {
            stall.cooldownTicks--;
        }
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        NonNullList<ItemStack> items = NonNullList.withSize(inventory.getContainerSize(), ItemStack.EMPTY);
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            items.set(i, inventory.getItem(i));
        }
        ContainerHelper.saveAllItems(tag, items, provider);
        if (storedCow != null) {
            tag.put(TAG_STORED, storedCow.copy());
        }
        tag.putInt(TAG_COOLDOWN, cooldownTicks);
        tag.putInt(TAG_TANK_AMOUNT, tankAmount);
        if (storedFluid != Fluids.EMPTY) {
            tag.putString(TAG_TANK_FLUID, BuiltInRegistries.FLUID.getKey(storedFluid).toString());
        }
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        NonNullList<ItemStack> items = NonNullList.withSize(inventory.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, provider);
        for (int i = 0; i < items.size(); i++) {
            inventory.setItem(i, items.get(i));
        }
        cooldownTicks = tag.getInt(TAG_COOLDOWN);
        tankAmount = tag.getInt(TAG_TANK_AMOUNT);
        storedCow = tag.contains(TAG_STORED) ? tag.getCompound(TAG_STORED).copy() : null;
        storedFluid = Fluids.EMPTY;
        if (tag.contains(TAG_TANK_FLUID)) {
            ResourceLocation key = ResourceLocation.tryParse(tag.getString(TAG_TANK_FLUID));
            if (key != null) {
                storedFluid = BuiltInRegistries.FLUID.getOptional(key).orElse(Fluids.EMPTY);
            }
        }
        Fluid configured = resolveFluid(storedCow);
        if (configured != Fluids.EMPTY) {
            storedFluid = configured;
        }
        updateStoredCowFluidTag();
        syncCowState(hasCow());
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(final CompoundTag tag, final HolderLookup.Provider provider) {
        loadAdditional(tag, provider);
    }

    @Override
    public CompoundTag getUpdateTag(final HolderLookup.Provider provider) {
        return saveWithoutMetadata(provider);
    }

    @Override
    public void clearContent() {
        inventory.setItem(SLOT_INPUT, ItemStack.EMPTY);
        inventory.setItem(SLOT_OUTPUT, ItemStack.EMPTY);
        tankAmount = 0;
        clearCow();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.fluidcows.stall");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(
            final int windowId, final Inventory inventory, final Player player) {
        return new StallMenu(windowId, inventory, this, dataAccess);
    }

    /** Returns the automation-facing fluid handler so pipes can drain the stall tank. */
    public IFluidHandler getFluidHandler() {
        return fluidHandler;
    }

    private void fillBuckets() {
        ItemStack input = inventory.getItem(SLOT_INPUT);
        ItemStack output = inventory.getItem(SLOT_OUTPUT);
        if (input.isEmpty() || !output.isEmpty() || storedFluid == Fluids.EMPTY || tankAmount < FluidType.BUCKET_VOLUME) {
            return;
        }

        ItemStack single = input.copy();
        single.setCount(1);
        FluidUtil.getFluidHandler(single).ifPresent(handler -> {
            FluidStack toFill = new FluidStack(storedFluid, FluidType.BUCKET_VOLUME);
            int accepted = handler.fill(toFill, FluidAction.EXECUTE);
            if (accepted < FluidType.BUCKET_VOLUME) {
                return;
            }
            tankAmount -= FluidType.BUCKET_VOLUME;
            inventory.removeItem(SLOT_INPUT, 1);
            inventory.setItem(SLOT_OUTPUT, handler.getContainer());
            setChangedAndSync();
        });
    }

    private void produceFluid() {
        if (storedFluid == Fluids.EMPTY || tankAmount > TANK_CAPACITY - FluidType.BUCKET_VOLUME) {
            return;
        }
        tankAmount += FluidType.BUCKET_VOLUME;
        ResourceLocation key = BuiltInRegistries.FLUID.getKey(storedFluid);
        if (key != null) {
            cooldownTicks = FCConfig.getStallCD(key);
        }
        setChangedAndSync();
    }

    private void clearCow() {
        storedCow = null;
        storedFluid = Fluids.EMPTY;
        cooldownTicks = 0;
        syncCowState(false);
        setChangedAndSync();
    }

    private void setChangedAndSync() {
        setChanged();
        Level level = getLevel();
        if (level != null) {
            BlockState state = level.getBlockState(getBlockPos());
            level.sendBlockUpdated(getBlockPos(), state, state, 3);
        }
    }

    private void syncCowState(final boolean hasCow) {
        Level level = getLevel();
        if (level == null) {
            return;
        }
        BlockState state = level.getBlockState(getBlockPos());
        if (!state.is(FluidCowsRegistries.STALL_BLOCK.get())) {
            return;
        }
        if (state.getValue(com.modernfluidcows.block.StallBlock.HAS_COW) != hasCow) {
            level.setBlock(
                    getBlockPos(),
                    state.setValue(com.modernfluidcows.block.StallBlock.HAS_COW, hasCow),
                    3);
        }
    }

    private static Fluid resolveFluid(@Nullable final CompoundTag tag) {
        if (tag == null || !tag.contains(FluidCow.HALTER_TAG_FLUID)) {
            return Fluids.EMPTY;
        }
        ResourceLocation key = ResourceLocation.tryParse(tag.getString(FluidCow.HALTER_TAG_FLUID));
        if (key == null || !FCConfig.isEnable(key)) {
            return Optional.ofNullable(FCUtils.getRandFluid()).orElse(Fluids.WATER);
        }
        Fluid resolved = BuiltInRegistries.FLUID.getOptional(key).orElse(Fluids.EMPTY);
        if (resolved == Fluids.EMPTY || !FCUtils.getBucketFluids().contains(resolved)) {
            return Optional.ofNullable(FCUtils.getRandFluid()).orElse(Fluids.WATER);
        }
        return resolved;
    }

    /** Ensures the stored cow data mirrors the resolved fluid id for future respawns. */
    private void updateStoredCowFluidTag() {
        if (storedCow == null || storedFluid == Fluids.EMPTY) {
            return;
        }
        ResourceLocation key = BuiltInRegistries.FLUID.getKey(storedFluid);
        if (key != null) {
            storedCow.putString(FluidCow.HALTER_TAG_FLUID, key.toString());
        }
    }

    /**
     * Minimal fluid handler that mirrors the stall's 10-bucket reservoir for pipes while blocking
     * insertion, matching the legacy stall automation behaviour.
     */
    private final class StallFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(final int tank) {
            if (tank != 0 || storedFluid == Fluids.EMPTY || tankAmount <= 0) {
                return FluidStack.EMPTY;
            }
            return new FluidStack(storedFluid, tankAmount);
        }

        @Override
        public int getTankCapacity(final int tank) {
            return tank == 0 ? TANK_CAPACITY : 0;
        }

        @Override
        public boolean isFluidValid(final int tank, final FluidStack stack) {
            return tank == 0 && stack.getFluid() == storedFluid;
        }

        @Override
        public int fill(final FluidStack resource, final FluidAction action) {
            return 0;
        }

        @Override
        public FluidStack drain(final FluidStack resource, final FluidAction action) {
            if (resource.isEmpty() || resource.getFluid() != storedFluid) {
                return FluidStack.EMPTY;
            }
            return drain(resource.getAmount(), action);
        }

        @Override
        public FluidStack drain(final int maxDrain, final FluidAction action) {
            if (storedFluid == Fluids.EMPTY || maxDrain <= 0 || tankAmount <= 0) {
                return FluidStack.EMPTY;
            }
            int drained = Math.min(maxDrain, tankAmount);
            FluidStack stack = new FluidStack(storedFluid, drained);
            if (action == FluidAction.EXECUTE) {
                tankAmount -= drained;
                setChangedAndSync();
            }
            return stack;
        }
    }
}
