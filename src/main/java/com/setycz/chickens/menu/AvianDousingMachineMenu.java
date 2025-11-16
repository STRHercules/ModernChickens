package com.setycz.chickens.menu;

import com.setycz.chickens.ChemicalEggRegistry;
import com.setycz.chickens.ChemicalEggRegistryItem;
import com.setycz.chickens.blockentity.AvianDousingMachineBlockEntity;
import com.setycz.chickens.blockentity.AvianDousingMachineBlockEntity.InfusionMode;
import com.setycz.chickens.blockentity.AvianDousingMachineBlockEntity.SpecialInfusion;
import com.setycz.chickens.item.ChickenItem;
import com.setycz.chickens.item.ChickenItemHelper;
import com.setycz.chickens.item.ChickensSpawnEggItem;
import com.setycz.chickens.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Objects;

/**
 * Container wiring for the Avian Dousing Machine. Synchronises the internal
 * energy, liquid, and chemical buffers back to the client so the GUI can render
 * live progress and gauge information.
 */
public class AvianDousingMachineMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOTS = AvianDousingMachineBlockEntity.SLOT_COUNT;

    private final AvianDousingMachineBlockEntity machine;
    private final ContainerLevelAccess access;

    private int clientEnergy;
    private int clientEnergyCapacity;
    private int clientProgress;
    private int clientMaxProgress;
    private FluidStack clientFluid = FluidStack.EMPTY;
    private int clientFluidAmount;
    private int clientFluidCapacity;
    private int clientFluidId = -1;
    private int clientChemicalAmount;
    private int clientChemicalCapacity;
    private int clientChemicalEntryId = -1;
    private int clientSpecialAmount;
    private int clientSpecialType;
    private InfusionMode clientMode = InfusionMode.NONE;

    public AvianDousingMachineMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(id, playerInventory, resolveBlockEntity(playerInventory, buffer));
    }

    public AvianDousingMachineMenu(int id, Inventory playerInventory, AvianDousingMachineBlockEntity machine) {
        super(ModMenuTypes.AVIAN_DOUSING_MACHINE.get(), id);
        this.machine = machine;
        Level level = machine.getLevel();
        this.access = level != null ? ContainerLevelAccess.create(level, machine.getBlockPos()) : ContainerLevelAccess.NULL;

        this.clientEnergy = machine.getEnergyStored();
        this.clientEnergyCapacity = machine.getEnergyCapacity();
        this.clientProgress = machine.getProgress();
        this.clientMaxProgress = machine.getMaxProgress();
        this.clientFluid = machine.getFluid().copy();
        this.clientFluidAmount = machine.getLiquidAmount();
        this.clientFluidCapacity = machine.getLiquidCapacity();
        this.clientFluidId = machine.getFluid().isEmpty()
                ? -1
                : BuiltInRegistries.FLUID.getId(machine.getFluid().getFluid());
        this.clientChemicalAmount = machine.getChemicalAmount();
        this.clientChemicalCapacity = machine.getChemicalCapacity();
        this.clientChemicalEntryId = machine.getChemicalEntryId();
        this.clientSpecialAmount = machine.getSpecialAmount();
        this.clientSpecialType = machine.getSpecialInfusion().ordinal();
        this.clientMode = machine.getMode();

        this.addSlot(new SmartChickenSlot(machine, 0, 50, 35));
        this.addSlot(new OutputSlot(machine, 1, 116, 35));

        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 9; ++column) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int hotbar = 0; hotbar < 9; ++hotbar) {
            this.addSlot(new Slot(playerInventory, hotbar, 8 + hotbar * 18, 142));
        }

        // Energy stored
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return getServerEnergy() & 0xFFFF;
            }

            @Override
            public void set(int value) {
                clientEnergy = (clientEnergy & 0xFFFF0000) | (value & 0xFFFF);
            }
        });
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return (getServerEnergy() >>> 16) & 0xFFFF;
            }

            @Override
            public void set(int value) {
                clientEnergy = (clientEnergy & 0x0000FFFF) | ((value & 0xFFFF) << 16);
            }
        });

        // Energy capacity
        this.addDataSlot(splitGetter(
                () -> getServerEnergyCapacity(),
                value -> clientEnergyCapacity = (clientEnergyCapacity & 0xFFFF0000) | (value & 0xFFFF)));
        this.addDataSlot(splitGetter(
                () -> getServerEnergyCapacity() >>> 16,
                value -> clientEnergyCapacity = (clientEnergyCapacity & 0x0000FFFF) | ((value & 0xFFFF) << 16)));

        // Progress
        this.addDataSlot(splitGetter(
                () -> getServerProgress(),
                value -> clientProgress = (clientProgress & 0xFFFF0000) | (value & 0xFFFF)));
        this.addDataSlot(splitGetter(
                () -> getServerProgress() >>> 16,
                value -> clientProgress = (clientProgress & 0x0000FFFF) | ((value & 0xFFFF) << 16)));

        // Fluid amount and capacity
        this.addDataSlot(splitGetter(
                () -> getServerFluidAmount(),
                value -> {
                    clientFluidAmount = (clientFluidAmount & 0xFFFF0000) | (value & 0xFFFF);
                    updateClientFluid();
                }));
        this.addDataSlot(splitGetter(
                () -> getServerFluidAmount() >>> 16,
                value -> {
                    clientFluidAmount = (clientFluidAmount & 0x0000FFFF) | ((value & 0xFFFF) << 16);
                    updateClientFluid();
                }));
        this.addDataSlot(splitGetter(
                () -> getServerFluidCapacity(),
                value -> clientFluidCapacity = (clientFluidCapacity & 0xFFFF0000) | (value & 0xFFFF)));
        this.addDataSlot(splitGetter(
                () -> getServerFluidCapacity() >>> 16,
                value -> clientFluidCapacity = (clientFluidCapacity & 0x0000FFFF) | ((value & 0xFFFF) << 16)));

        // Special infusion amount and type
        this.addDataSlot(splitGetter(
                () -> getServerSpecialAmount(),
                value -> clientSpecialAmount = (clientSpecialAmount & 0xFFFF0000) | (value & 0xFFFF)));
        this.addDataSlot(splitGetter(
                () -> getServerSpecialAmount() >>> 16,
                value -> clientSpecialAmount = (clientSpecialAmount & 0x0000FFFF) | ((value & 0xFFFF) << 16)));
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return getServerSpecialType();
            }

            @Override
            public void set(int value) {
                clientSpecialType = Math.max(0, Math.min(value, SpecialInfusion.values().length - 1));
            }
        });

        // Fluid id
        this.addDataSlot(splitGetter(
                () -> getServerFluidId(),
                value -> {
                    clientFluidId = (clientFluidId & 0xFFFF0000) | (value & 0xFFFF);
                    updateClientFluid();
                }));
        this.addDataSlot(splitGetter(
                () -> getServerFluidId() >>> 16,
                value -> {
                    clientFluidId = (clientFluidId & 0x0000FFFF) | ((value & 0xFFFF) << 16);
                    updateClientFluid();
                }));

        // Chemical amount and capacity
        this.addDataSlot(splitGetter(
                () -> getServerChemicalAmount(),
                value -> clientChemicalAmount = (clientChemicalAmount & 0xFFFF0000) | (value & 0xFFFF)));
        this.addDataSlot(splitGetter(
                () -> getServerChemicalAmount() >>> 16,
                value -> clientChemicalAmount = (clientChemicalAmount & 0x0000FFFF) | ((value & 0xFFFF) << 16)));
        this.addDataSlot(splitGetter(
                () -> getServerChemicalCapacity(),
                value -> clientChemicalCapacity = (clientChemicalCapacity & 0xFFFF0000) | (value & 0xFFFF)));
        this.addDataSlot(splitGetter(
                () -> getServerChemicalCapacity() >>> 16,
                value -> clientChemicalCapacity = (clientChemicalCapacity & 0x0000FFFF) | ((value & 0xFFFF) << 16)));

        // Chemical entry id
        this.addDataSlot(splitGetter(
                () -> getServerChemicalEntryId(),
                value -> clientChemicalEntryId = (clientChemicalEntryId & 0xFFFF0000) | (value & 0xFFFF)));
        this.addDataSlot(splitGetter(
                () -> getServerChemicalEntryId() >>> 16,
                value -> clientChemicalEntryId = (clientChemicalEntryId & 0x0000FFFF) | ((value & 0xFFFF) << 16)));

        // Mode
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return getServerMode();
            }

            @Override
            public void set(int value) {
                clientMode = InfusionMode.values()[Math.max(0, Math.min(value, InfusionMode.values().length - 1))];
            }
        });
    }

    private static AvianDousingMachineBlockEntity resolveBlockEntity(Inventory inventory, RegistryFriendlyByteBuf buffer) {
        Objects.requireNonNull(inventory, "playerInventory");
        Objects.requireNonNull(buffer, "buffer");
        BlockPos pos = buffer.readBlockPos();
        Level level = inventory.player.level();
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof AvianDousingMachineBlockEntity machine) {
            return machine;
        }
        throw new IllegalStateException("Avian Dousing Machine not found at " + pos);
    }

    @Override
    public boolean stillValid(Player player) {
        return machine.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack current = slot.getItem();
            original = current.copy();
            if (index < MACHINE_SLOTS) {
                if (!this.moveItemStackTo(current, MACHINE_SLOTS, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(current, 0, MACHINE_SLOTS - 1, false)) {
                return ItemStack.EMPTY;
            }

            if (current.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            slot.onTake(player, current);
        }
        return original;
    }

    public ContainerLevelAccess getAccess() {
        return access;
    }

    public int getEnergyStored() {
        return isServerSide() ? machine.getEnergyStored() : clientEnergy;
    }

    public int getEnergyCapacity() {
        return isServerSide() ? machine.getEnergyCapacity() : clientEnergyCapacity;
    }

    public int getProgress() {
        return isServerSide() ? machine.getProgress() : clientProgress;
    }

    public int getMaxProgress() {
        return isServerSide() ? machine.getMaxProgress() : clientMaxProgress;
    }

    public FluidStack getFluid() {
        return isServerSide() ? machine.getFluid() : clientFluid;
    }

    public int getFluidAmount() {
        if (isServerSide()) {
            return machine.getLiquidAmount();
        }
        return hasClientSpecial() ? clientSpecialAmount : clientFluidAmount;
    }

    public int getFluidCapacity() {
        if (isServerSide()) {
            return machine.getLiquidCapacity();
        }
        return hasClientSpecial() ? AvianDousingMachineBlockEntity.SPECIAL_LIQUID_CAPACITY : clientFluidCapacity;
    }

    public int getChemicalAmount() {
        return isServerSide() ? machine.getChemicalAmount() : clientChemicalAmount;
    }

    public int getChemicalCapacity() {
        return isServerSide() ? machine.getChemicalCapacity() : clientChemicalCapacity;
    }

    public InfusionMode getMode() {
        return isServerSide() ? machine.getMode() : clientMode;
    }

    public SpecialInfusion getSpecialInfusion() {
        if (isServerSide()) {
            return machine.getSpecialInfusion();
        }
        return SpecialInfusion.values()[Math.max(0, Math.min(clientSpecialType, SpecialInfusion.values().length - 1))];
    }

    public int getSpecialAmount() {
        return isServerSide() ? machine.getSpecialAmount() : clientSpecialAmount;
    }

    public ChemicalEggRegistryItem getStoredChemical() {
        if (isServerSide()) {
            return ChemicalEggRegistry.findById(machine.getChemicalEntryId());
        }
        if (clientChemicalEntryId < 0) {
            return null;
        }
        return ChemicalEggRegistry.findById(clientChemicalEntryId);
    }

    private boolean isServerSide() {
        return machine != null && machine.getLevel() != null && !machine.getLevel().isClientSide;
    }

    private boolean hasClientSpecial() {
        return clientSpecialAmount > 0 && clientSpecialType != SpecialInfusion.NONE.ordinal();
    }

    private static DataSlot splitGetter(IntSupplier supplier, IntConsumer consumer) {
        return new DataSlot() {
            private int cached;

            @Override
            public int get() {
                cached = supplier.getAsInt() & 0xFFFF;
                return cached;
            }

            @Override
            public void set(int value) {
                cached = value & 0xFFFF;
                consumer.accept(cached);
            }
        };
    }

    private void updateClientFluid() {
        if (clientFluidId < 0 || clientFluidAmount <= 0) {
            clientFluid = FluidStack.EMPTY;
            return;
        }
        Fluid fluid = BuiltInRegistries.FLUID.byId(clientFluidId);
        if (fluid == null || fluid == Fluids.EMPTY) {
            clientFluid = FluidStack.EMPTY;
            return;
        }
        clientFluid = new FluidStack(fluid, clientFluidAmount);
    }

    private int getServerEnergy() {
        return machine != null ? machine.getEnergyStored() : 0;
    }

    private int getServerEnergyCapacity() {
        return machine != null ? machine.getEnergyCapacity() : 0;
    }

    private int getServerProgress() {
        return machine != null ? machine.getProgress() : 0;
    }

    private int getServerFluidAmount() {
        return machine != null ? machine.getLiquidAmount() : 0;
    }

    private int getServerFluidCapacity() {
        return machine != null ? machine.getLiquidCapacity() : 0;
    }

    private int getServerFluidId() {
        if (machine == null) {
            return -1;
        }
        FluidStack stack = machine.getFluid();
        if (stack.isEmpty()) {
            return -1;
        }
        return BuiltInRegistries.FLUID.getId(stack.getFluid());
    }

    private int getServerSpecialAmount() {
        return machine != null ? machine.getSpecialAmount() : 0;
    }

    private int getServerSpecialType() {
        return machine != null ? machine.getSpecialInfusion().ordinal() : 0;
    }

    private int getServerChemicalAmount() {
        return machine != null ? machine.getChemicalAmount() : 0;
    }

    private int getServerChemicalCapacity() {
        return machine != null ? machine.getChemicalCapacity() : 0;
    }

    private int getServerChemicalEntryId() {
        return machine != null ? machine.getChemicalEntryId() : -1;
    }

    private int getServerMode() {
        return machine != null ? machine.getMode().ordinal() : 0;
    }

    private interface IntSupplier {
        int getAsInt();
    }

    private interface IntConsumer {
        void accept(int value);
    }

    private static class SmartChickenSlot extends Slot {
        private final AvianDousingMachineBlockEntity machine;

        public SmartChickenSlot(AvianDousingMachineBlockEntity machine, int index, int x, int y) {
            super(machine, index, x, y);
            this.machine = machine;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (stack.isEmpty()) {
                return false;
            }
            if (!(stack.getItem() instanceof ChickensSpawnEggItem || stack.getItem() instanceof ChickenItem)) {
                return false;
            }
            return machine.isDousableChicken(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 16;
        }
    }

    private static class OutputSlot extends Slot {
        public OutputSlot(AvianDousingMachineBlockEntity machine, int index, int x, int y) {
            super(machine, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
