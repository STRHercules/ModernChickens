package com.setycz.chickens.menu;

import com.setycz.chickens.blockentity.AvianFluidConverterBlockEntity;
import com.setycz.chickens.item.LiquidEggItem;
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
 * Menu wiring for the Avian Fluid Converter. Reuses the furnace-style layout
 * while syncing the tank contents back to the client so the GUI can render
 * fluid gauges without polling the world every frame.
 */
public class AvianFluidConverterMenu extends AbstractContainerMenu {
    private static final int INVENTORY_SIZE = AvianFluidConverterBlockEntity.SLOT_COUNT;

    private final AvianFluidConverterBlockEntity converter;
    private final ContainerLevelAccess access;
    private FluidStack clientFluid = FluidStack.EMPTY;
    private int clientAmount;
    private int clientCapacity;
    private int clientFluidId = -1;

    public AvianFluidConverterMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(id, playerInventory, resolveBlockEntity(playerInventory, buffer));
    }

    public AvianFluidConverterMenu(int id, Inventory playerInventory, AvianFluidConverterBlockEntity converter) {
        super(ModMenuTypes.AVIAN_FLUID_CONVERTER.get(), id);
        this.converter = converter;
        Level level = converter.getLevel();
        this.access = level != null ? ContainerLevelAccess.create(level, converter.getBlockPos()) : ContainerLevelAccess.NULL;
        FluidStack serverFluid = converter.getFluid();
        this.clientFluid = serverFluid.copy();
        this.clientAmount = serverFluid.getAmount();
        this.clientCapacity = converter.getTankCapacity();
        this.clientFluidId = serverFluid.isEmpty() ? -1 : BuiltInRegistries.FLUID.getId(serverFluid.getFluid());

        this.addSlot(new LiquidEggSlot(converter, 0, 52, 34));

        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 9; ++column) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int hotbar = 0; hotbar < 9; ++hotbar) {
            this.addSlot(new Slot(playerInventory, hotbar, 8 + hotbar * 18, 142));
        }

        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return getServerAmount() & 0xFFFF;
            }

            @Override
            public void set(int value) {
                clientAmount = (clientAmount & 0xFFFF0000) | (value & 0xFFFF);
                updateClientFluid();
            }
        });
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return (getServerAmount() >>> 16) & 0xFFFF;
            }

            @Override
            public void set(int value) {
                clientAmount = (clientAmount & 0x0000FFFF) | ((value & 0xFFFF) << 16);
                updateClientFluid();
            }
        });
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return getServerCapacity() & 0xFFFF;
            }

            @Override
            public void set(int value) {
                clientCapacity = (clientCapacity & 0xFFFF0000) | (value & 0xFFFF);
            }
        });
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return (getServerCapacity() >>> 16) & 0xFFFF;
            }

            @Override
            public void set(int value) {
                clientCapacity = (clientCapacity & 0x0000FFFF) | ((value & 0xFFFF) << 16);
            }
        });
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return getServerFluidId() & 0xFFFF;
            }

            @Override
            public void set(int value) {
                clientFluidId = (clientFluidId & 0xFFFF0000) | (value & 0xFFFF);
                updateClientFluid();
            }
        });
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return (getServerFluidId() >>> 16) & 0xFFFF;
            }

            @Override
            public void set(int value) {
                clientFluidId = (clientFluidId & 0x0000FFFF) | ((value & 0xFFFF) << 16);
                updateClientFluid();
            }
        });
    }

    private static AvianFluidConverterBlockEntity resolveBlockEntity(Inventory inventory, RegistryFriendlyByteBuf buffer) {
        Objects.requireNonNull(inventory, "playerInventory");
        Objects.requireNonNull(buffer, "buffer");
        BlockPos pos = buffer.readBlockPos();
        Level level = inventory.player.level();
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof AvianFluidConverterBlockEntity converter) {
            return converter;
        }
        throw new IllegalStateException("Avian Fluid Converter not found at " + pos);
    }

    @Override
    public boolean stillValid(Player player) {
        return converter.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack current = slot.getItem();
            original = current.copy();
            if (index < INVENTORY_SIZE) {
                if (!this.moveItemStackTo(current, INVENTORY_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(current, 0, INVENTORY_SIZE, false)) {
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

    public FluidStack getFluid() {
        return isServerSide() ? converter.getFluid() : clientFluid;
    }

    public int getFluidAmount() {
        return isServerSide() ? converter.getFluidAmount() : clientAmount;
    }

    public int getCapacity() {
        return isServerSide() ? converter.getTankCapacity() : clientCapacity;
    }

    private boolean isServerSide() {
        return converter != null && converter.getLevel() != null && !converter.getLevel().isClientSide;
    }

    private int getServerAmount() {
        return converter != null ? converter.getFluidAmount() : 0;
    }

    private int getServerCapacity() {
        return converter != null ? converter.getTankCapacity() : 0;
    }

    private int getServerFluidId() {
        if (converter == null) {
            return -1;
        }
        FluidStack stack = converter.getFluid();
        if (stack.isEmpty()) {
            return -1;
        }
        return BuiltInRegistries.FLUID.getId(stack.getFluid());
    }

    private void updateClientFluid() {
        if (clientFluidId < 0) {
            clientFluid = FluidStack.EMPTY;
            return;
        }
        Fluid fluid = BuiltInRegistries.FLUID.byId(clientFluidId);
        if (fluid == null || fluid == Fluids.EMPTY || clientAmount <= 0) {
            clientFluid = FluidStack.EMPTY;
            return;
        }
        clientFluid = new FluidStack(fluid, clientAmount);
    }

    private static class LiquidEggSlot extends Slot {
        public LiquidEggSlot(AvianFluidConverterBlockEntity converter, int index, int x, int y) {
            super(converter, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof LiquidEggItem;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}
