package strhercules.chickens.blockentity;

import strhercules.chickens.integration.mekanism.MekanismChemicalHelper;
import net.minecraft.core.Direction;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;

/** Direction-aware capability views used by the independent side configurator. */
public final class MachineCapabilityWrappers {
    private MachineCapabilityWrappers() {
    }

    public static IEnergyStorage energy(IEnergyStorage delegate, MachineSideConfig config,
            @Nullable Direction side) {
        return new IEnergyStorage() {
            @Override
            public int receiveEnergy(int maxReceive, boolean simulate) {
                return config.allows(side, MachineSideConfig.Channel.ENERGY, true)
                        ? delegate.receiveEnergy(maxReceive, simulate) : 0;
            }

            @Override
            public int extractEnergy(int maxExtract, boolean simulate) {
                return config.allows(side, MachineSideConfig.Channel.ENERGY, false)
                        ? delegate.extractEnergy(maxExtract, simulate) : 0;
            }

            @Override
            public int getEnergyStored() {
                return delegate.getEnergyStored();
            }

            @Override
            public int getMaxEnergyStored() {
                return delegate.getMaxEnergyStored();
            }

            @Override
            public boolean canExtract() {
                return config.allows(side, MachineSideConfig.Channel.ENERGY, false) && delegate.canExtract();
            }

            @Override
            public boolean canReceive() {
                return config.allows(side, MachineSideConfig.Channel.ENERGY, true) && delegate.canReceive();
            }
        };
    }

    public static IFluidHandler fluid(IFluidHandler delegate, MachineSideConfig config,
            @Nullable Direction side) {
        return new IFluidHandler() {
            @Override
            public int getTanks() {
                return delegate.getTanks();
            }

            @Override
            public FluidStack getFluidInTank(int tank) {
                return delegate.getFluidInTank(tank);
            }

            @Override
            public int getTankCapacity(int tank) {
                return delegate.getTankCapacity(tank);
            }

            @Override
            public boolean isFluidValid(int tank, FluidStack stack) {
                return config.allows(side, MachineSideConfig.Channel.FLUIDS, true)
                        && delegate.isFluidValid(tank, stack);
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                return config.allows(side, MachineSideConfig.Channel.FLUIDS, true)
                        ? delegate.fill(resource, action) : 0;
            }

            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                return config.allows(side, MachineSideConfig.Channel.FLUIDS, false)
                        ? delegate.drain(resource, action) : FluidStack.EMPTY;
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                return config.allows(side, MachineSideConfig.Channel.FLUIDS, false)
                        ? delegate.drain(maxDrain, action) : FluidStack.EMPTY;
            }
        };
    }

    /** Adds side-aware input/output policy to Mekanism's reflective chemical handler. */
    @Nullable
    public static Object chemical(@Nullable Object delegate, MachineSideConfig config,
            @Nullable Direction side, @Nullable Class<?> handlerType) {
        if (delegate == null) {
            return null;
        }
        if (side == null) {
            return delegate;
        }
        if (handlerType == null) {
            return delegate;
        }
        try {
            return Proxy.newProxyInstance(handlerType.getClassLoader(), new Class<?>[] { handlerType },
                    (proxy, method, args) -> {
                        String name = method.getName();
                        if ("insertChemical".equals(name)
                                && !config.allows(side, MachineSideConfig.Channel.CHEMICALS, true)) {
                            return firstChemicalStack(args);
                        }
                        if ("extractChemical".equals(name)
                                && !config.allows(side, MachineSideConfig.Channel.CHEMICALS, false)) {
                            return MekanismChemicalHelper.emptyStack();
                        }
                        if ("isValid".equals(name)
                                && !config.allows(side, MachineSideConfig.Channel.CHEMICALS, true)) {
                            return false;
                        }
                        if ("setChemicalInTank".equals(name)
                                && !config.allows(side, MachineSideConfig.Channel.CHEMICALS, true)) {
                            return null;
                        }
                        if ("equals".equals(name)) {
                            return proxy == (args != null && args.length == 1 ? args[0] : null);
                        }
                        if ("hashCode".equals(name)) {
                            return System.identityHashCode(proxy);
                        }
                        if ("toString".equals(name)) {
                            return delegate.toString();
                        }
                        try {
                            return method.invoke(delegate, args);
                        } catch (InvocationTargetException exception) {
                            throw exception.getCause();
                        }
                    });
        } catch (LinkageError exception) {
            // The caller only reaches this path when a compatible handler was
            // already created. Keep that handler usable if a future Mekanism
            // API changes its interface name or class loader.
            return delegate;
        }
    }

    private static Object firstChemicalStack(@Nullable Object[] args) {
        if (args != null) {
            for (Object arg : args) {
                if (arg != null && arg.getClass().getName().contains("ChemicalStack")) {
                    return arg;
                }
            }
        }
        return MekanismChemicalHelper.emptyStack();
    }
}
