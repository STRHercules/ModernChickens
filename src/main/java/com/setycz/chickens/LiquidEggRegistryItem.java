package com.setycz.chickens;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

/**
 * Data record describing a single liquid egg entry. The modern fluid API is
 * driven by {@link Fluid}, so we capture both the block that should be placed
 * in the world and the associated fluid for future container logic.
 */
public final class LiquidEggRegistryItem {
    private final int id;
    private final Block liquid;
    private final int eggColor;
    private final Fluid fluid;

    public LiquidEggRegistryItem(int id, Block liquid, int eggColor, Fluid fluid) {
        this.id = id;
        this.liquid = liquid;
        this.eggColor = eggColor;
        this.fluid = fluid;
    }

    public int getId() {
        return id;
    }

    public Block getLiquid() {
        return liquid;
    }

    public int getEggColor() {
        return eggColor;
    }

    public Fluid getFluid() {
        return fluid;
    }
}
