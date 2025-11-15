package com.setycz.chickens.menu;

import com.setycz.chickens.blockentity.NestBlockEntity;
import com.setycz.chickens.item.ChickenItemHelper;
import com.setycz.chickens.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Objects;

/**
 * Container menu for the rooster nest. The layout exposes a dedicated seed
 * slot and rooster slot near the top of the GUI, followed by the standard
 * player inventory grid.
 */
public class NestMenu extends AbstractContainerMenu {
    private final NestBlockEntity nest;
    private final ContainerLevelAccess access;

    public NestMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(id, playerInventory, resolveBlockEntity(playerInventory, buffer));
    }

    public NestMenu(int id, Inventory playerInventory, NestBlockEntity nest) {
        super(ModMenuTypes.NEST.get(), id);
        this.nest = nest;
        Level level = nest.getLevel();
        this.access = level != null ? ContainerLevelAccess.create(level, nest.getBlockPos())
                : ContainerLevelAccess.NULL;

        // Seed slot – aligned with nest.png coordinates (61,19 to 76,34).
        this.addSlot(new SeedSlot(nest, NestBlockEntity.SEED_SLOT, 61, 19));
        // Rooster slot – aligned with nest.png coordinates (98,19 to 113,34).
        this.addSlot(new RoosterSlot(nest, NestBlockEntity.ROOSTER_SLOT, 98, 19));

        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 9; ++column) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 51 + row * 18));
            }
        }
        for (int hotbar = 0; hotbar < 9; ++hotbar) {
            this.addSlot(new Slot(playerInventory, hotbar, 8 + hotbar * 18, 109));
        }
    }

    private static NestBlockEntity resolveBlockEntity(Inventory inventory, RegistryFriendlyByteBuf buffer) {
        Objects.requireNonNull(inventory, "playerInventory");
        Objects.requireNonNull(buffer, "buffer");
        BlockPos pos = buffer.readBlockPos();
        Level level = inventory.player.level();
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof NestBlockEntity nest) {
            return nest;
        }
        throw new IllegalStateException("Nest not found at " + pos);
    }

    @Override
    public boolean stillValid(Player player) {
        return nest.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack current = slot.getItem();
            original = current.copy();

            if (index < NestBlockEntity.INVENTORY_SIZE) {
                if (!this.moveItemStackTo(current, NestBlockEntity.INVENTORY_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Try rooster slot first, then seed slot.
                if (!this.moveItemStackTo(current, NestBlockEntity.ROOSTER_SLOT, NestBlockEntity.ROOSTER_SLOT + 1, false)
                        && !this.moveItemStackTo(current, NestBlockEntity.SEED_SLOT, NestBlockEntity.SEED_SLOT + 1,
                                false)) {
                    return ItemStack.EMPTY;
                }
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

    private static class SeedSlot extends Slot {
        public SeedSlot(NestBlockEntity nest, int index, int x, int y) {
            super(nest, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            // Reuse the same seed set recognised by the underlying block entity.
            return !stack.isEmpty()
                    && (stack.is(net.minecraft.world.item.Items.WHEAT_SEEDS)
                            || stack.is(net.minecraft.world.item.Items.BEETROOT_SEEDS)
                            || stack.is(net.minecraft.world.item.Items.MELON_SEEDS)
                            || stack.is(net.minecraft.world.item.Items.PUMPKIN_SEEDS));
        }
    }

    private static class RoosterSlot extends Slot {
        public RoosterSlot(NestBlockEntity nest, int index, int x, int y) {
            super(nest, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return ChickenItemHelper.isRooster(stack);
        }
    }
}

