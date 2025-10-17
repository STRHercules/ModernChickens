package com.setycz.chickens.liquidegg;

import com.setycz.chickens.LiquidEggRegistry;
import com.setycz.chickens.LiquidEggRegistryItem;
import com.setycz.chickens.item.ChickenItemHelper;
import com.setycz.chickens.registry.ModRegistry;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
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
        return new FluidStack(entry.getFluid(), FluidType.BUCKET_VOLUME);
    }

    @Override
    public int getTankCapacity(int tank) {
        return tank == 0 ? FluidType.BUCKET_VOLUME : 0;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        if (tank != 0 || stack.isEmpty()) {
            return false;
        }
        LiquidEggRegistryItem entry = resolve();
        return entry != null && stack.getFluid() == entry.getFluid();
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
        FluidStack contained = new FluidStack(entry.getFluid(), FluidType.BUCKET_VOLUME);
        if (!resource.isFluidEqual(contained)) {
            return FluidStack.EMPTY;
        }
        return drain(resource.getAmount(), action);
    }

    @Override
    public FluidStack drain(int maxDrain, IFluidHandler.FluidAction action) {
        LiquidEggRegistryItem entry = resolve();
        if (entry == null || container.isEmpty() || maxDrain < FluidType.BUCKET_VOLUME) {
            return FluidStack.EMPTY;
        }
        FluidStack drained = new FluidStack(entry.getFluid(), FluidType.BUCKET_VOLUME);
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
