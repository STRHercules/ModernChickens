package com.modernfluidcows.menu;

import com.modernfluidcows.blockentity.StallBlockEntity;
import com.modernfluidcows.registry.FluidCowsRegistries;
import java.util.Optional;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

/**
 * Server/client menu for the Fluid Cow stall.
 *
 * <p>The legacy mod exposed a custom container with two slots – a bucket input and bottled output –
 * alongside progress bars for the internal tank and cooldown. NeoForge menus replace the old GUI
 * handler, so this class mirrors that behaviour with {@link ContainerData} backed by the stall
 * block entity.</p>
 */
public class StallMenu extends AbstractContainerMenu {
    private static final int SLOT_COUNT = 2;
    private static final int SLOT_INPUT = 0;
    private static final int SLOT_OUTPUT = 1;

    private final Container container;
    private final ContainerLevelAccess access;
    private final ContainerData data;
    @Nullable private final StallBlockEntity stall;

    public StallMenu(
            final int id, final Inventory playerInventory, final StallBlockEntity stall, final ContainerData data) {
        this(id, playerInventory, stall.getInventory(), ContainerLevelAccess.create(stall.getLevel(), stall.getBlockPos()), data, stall);
    }

    private StallMenu(
            final int id,
            final Inventory playerInventory,
            final Container container,
            final ContainerLevelAccess access,
            final ContainerData data,
            @Nullable final StallBlockEntity stall) {
        super(FluidCowsRegistries.STALL_MENU.get(), id);
        checkContainerSize(container, SLOT_COUNT);
        checkContainerDataCount(data, 2);
        this.container = container;
        this.access = access;
        this.data = data;
        this.stall = stall;

        container.startOpen(playerInventory.player);

        // Input bucket slot – mirrors the 1.12 stall allowing fluid containers only.
        addSlot(
                new Slot(container, SLOT_INPUT, 44, 35) {
                    @Override
                    public boolean mayPlace(final ItemStack stack) {
                        return container.canPlaceItem(SLOT_INPUT, stack);
                    }
                });

        // Output slot – players may only extract completed buckets.
        addSlot(
                new Slot(container, SLOT_OUTPUT, 116, 35) {
                    @Override
                    public boolean mayPlace(final ItemStack stack) {
                        return false;
                    }
                });

        // Player inventory slots (3 rows).
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }

        // Hotbar slots.
        for (int slot = 0; slot < 9; slot++) {
            addSlot(new Slot(playerInventory, slot, 8 + slot * 18, 142));
        }

        addDataSlots(data);
    }

    /** Client-side fallback so menus opened without a block entity still render safely. */
    public static StallMenu createFallback(final int id, final Inventory inventory) {
        return new StallMenu(
                id,
                inventory,
                new SimpleContainer(SLOT_COUNT),
                ContainerLevelAccess.NULL,
                new SimpleContainerData(2),
                null);
    }

    @Override
    public boolean stillValid(final Player player) {
        return stillValid(access, player, FluidCowsRegistries.STALL_BLOCK.get());
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            moved = stack.copy();

            if (index < SLOT_COUNT) {
                if (!moveItemStackTo(stack, SLOT_COUNT, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, SLOT_INPUT, SLOT_INPUT + 1, false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return moved;
    }

    @Override
    public void removed(final Player player) {
        super.removed(player);
        container.stopOpen(player);
    }

    public int getTankAmount() {
        return data.get(0);
    }

    public int getCooldownTicks() {
        return data.get(1);
    }

    public int getScaledFluidHeight(final int pixels) {
        int amount = getTankAmount();
        if (amount <= 0) {
            return 0;
        }
        return Mth.clamp((int) Math.ceil((double) amount * pixels / StallBlockEntity.TANK_CAPACITY), 0, pixels);
    }

    public Optional<FluidStack> getDisplayedFluidStack() {
        return Optional.ofNullable(stall)
                .map(StallBlockEntity::getStoredFluid)
                .filter(fluid -> fluid != Fluids.EMPTY)
                .map(fluid -> new FluidStack(fluid, getTankAmount()));
    }

    public int getCapacity() {
        return StallBlockEntity.TANK_CAPACITY;
    }
}
