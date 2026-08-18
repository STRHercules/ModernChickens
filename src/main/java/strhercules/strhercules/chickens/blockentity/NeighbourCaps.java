package strhercules.chickens.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;

public final class NeighbourCaps {
    private NeighbourCaps() {
    }

    @Nullable
    public static <T> T find(@Nullable Level level, BlockPos pos, Capability<T> capability,
            @Nullable Direction side) {
        if (level == null || !level.isLoaded(pos)) {
            return null;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null || blockEntity.isRemoved()) {
            return null;
        }
        return blockEntity.getCapability(capability, side).resolve().orElse(null);
    }

    @Nullable
    public static IEnergyStorage energy(@Nullable Level level, BlockPos pos, @Nullable Direction side) {
        return find(level, pos, ForgeCapabilities.ENERGY, side);
    }

    @Nullable
    public static IFluidHandler fluid(@Nullable Level level, BlockPos pos, @Nullable Direction side) {
        return find(level, pos, ForgeCapabilities.FLUID_HANDLER, side);
    }
}
