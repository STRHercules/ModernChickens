package com.setycz.chickens.liquidegg;

import com.setycz.chickens.LiquidEggRegistry;
import com.setycz.chickens.LiquidEggRegistryItem;
import com.setycz.chickens.item.ChickenItemHelper;
import com.setycz.chickens.registry.ModRegistry;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

import javax.annotation.Nullable;

/**
 * Legacy-style fluid handler that exposes liquid eggs as one-time buckets.
 * Automation can drain the contained fluid through NeoForge's deprecated fluid
 * capability layer, mirroring how the original mod integrated with pipes and
 * tanks.
 */
public final class LiquidEggFluidWrapper implements IFluidHandlerItem {
    private final ItemStack container;

    public LiquidEggFluidWrapper(ItemStack container) {
        this.container = container;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        if (tank != 0) {
            return FluidStack.EMPTY;
        }
        LiquidEggRegistryItem entry = resolve();
        if (entry == null) {
            return FluidStack.EMPTY;
        }
        return entry.createFluidStack();
    }

    @Override
    public int getTankCapacity(int tank) {
        if (tank != 0) {
            return 0;
        }
        LiquidEggRegistryItem entry = resolve();
        return entry != null ? entry.getVolume() : 0;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        if (tank != 0 || stack.isEmpty()) {
            return false;
        }
        LiquidEggRegistryItem entry = resolve();
        if (entry == null) {
            return false;
        }
        FluidStack contained = entry.createFluidStack();
        return !contained.isEmpty() && stack.isFluidEqual(contained);
    }

    @Override
    public int fill(FluidStack resource, IFluidHandler.FluidAction action) {
        return 0;
    }

    @Override
    public FluidStack drain(FluidStack resource, IFluidHandler.FluidAction action) {
        LiquidEggRegistryItem entry = resolve();
        if (entry == null || resource.isEmpty()) {
            return FluidStack.EMPTY;
        }
        FluidStack contained = entry.createFluidStack();
        if (!resource.isFluidEqual(contained)) {
            return FluidStack.EMPTY;
        }
        return drain(resource.getAmount(), action);
    }

    @Override
    public FluidStack drain(int maxDrain, IFluidHandler.FluidAction action) {
        LiquidEggRegistryItem entry = resolve();
        if (entry == null || container.isEmpty()) {
            return FluidStack.EMPTY;
        }
        FluidStack drained = entry.createFluidStack();
        if (drained.isEmpty() || maxDrain < drained.getAmount()) {
            return FluidStack.EMPTY;
        }
        if (action.execute()) {
            container.shrink(1);
        }
        return drained;
    }

    @Override
    public ItemStack getContainer() {
        return container;
    }

    @Nullable
    private LiquidEggRegistryItem resolve() {
        if (!container.is(ModRegistry.LIQUID_EGG.get())) {
            return null;
        }
        int type = ChickenItemHelper.getChickenType(container);
        return LiquidEggRegistry.findById(type);
    }
}
