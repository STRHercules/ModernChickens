package com.setycz.chickens;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Data record describing a single liquid egg entry. The modern fluid API is
 * driven by {@link Fluid}, so we capture both the block that should be placed
 * in the world and the associated fluid for future container logic.
 */
public final class LiquidEggRegistryItem {
    private final int id;
    private final int eggColor;
    private final Supplier<Fluid> fluidSupplier;
    @Nullable
    private final Supplier<BlockState> blockStateSupplier;
    private final int volume;
    private final EnumSet<HazardFlag> hazardFlags;

    public LiquidEggRegistryItem(int id, Block liquid, int eggColor, Fluid fluid) {
        this(id,
                () -> liquid == null ? null : liquid.defaultBlockState(),
                eggColor,
                () -> fluid,
                FluidType.BUCKET_VOLUME,
                EnumSet.noneOf(HazardFlag.class));
    }

    public LiquidEggRegistryItem(int id,
                                 @Nullable Supplier<BlockState> blockStateSupplier,
                                 int eggColor,
                                 Supplier<Fluid> fluidSupplier,
                                 int volume,
                                 Set<HazardFlag> hazardFlags) {
        this.id = id;
        this.blockStateSupplier = blockStateSupplier;
        this.eggColor = eggColor;
        this.fluidSupplier = Objects.requireNonNull(fluidSupplier, "fluidSupplier");
        this.volume = Math.max(0, volume);
        this.hazardFlags = hazardFlags.isEmpty()
                ? EnumSet.noneOf(HazardFlag.class)
                : EnumSet.copyOf(hazardFlags);
    }

    public int getId() {
        return id;
    }

    @Nullable
    public BlockState getLiquidBlockState() {
        return blockStateSupplier != null ? blockStateSupplier.get() : null;
    }

    @Nullable
    public Block getLiquidBlock() {
        BlockState state = getLiquidBlockState();
        return state != null ? state.getBlock() : null;
    }

    public int getEggColor() {
        return eggColor;
    }

    public Fluid getFluid() {
        return fluidSupplier.get();
    }

    public FluidStack createFluidStack() {
        Fluid fluid = getFluid();
        return fluid == Fluids.EMPTY || fluid == null || volume <= 0
                ? FluidStack.EMPTY
                : new FluidStack(fluid, volume);
    }

    public int getVolume() {
        return volume;
    }

    public boolean hasHazard(HazardFlag flag) {
        return hazardFlags.contains(flag);
    }

    public Set<HazardFlag> getHazards() {
        return Collections.unmodifiableSet(hazardFlags);
    }

    public Component getDisplayName() {
        FluidStack stack = createFluidStack();
        if (!stack.isEmpty()) {
            return stack.getDisplayName();
        }
        Block block = getLiquidBlock();
        if (block != null) {
            return Component.translatable(block.getDescriptionId());
        }
        return Component.literal("?");
    }

    public enum HazardFlag {
        HOT,
        TOXIC,
        CORROSIVE,
        RADIOACTIVE,
        MAGICAL;

        public String getTranslationKey() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
