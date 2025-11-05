package com.setycz.chickens;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Immutable record describing a chemical or gas egg entry. The structure mirrors the fluid registry
 * but captures Mekanism chemical metadata instead of direct fluid objects so the mod can function
 * without a compile-time dependency on Mekanism.
 */
public final class ChemicalEggRegistryItem {
    private final int id;
    private final ResourceLocation chemicalId;
    private final ResourceLocation texture;
    private final Component displayName;
    private final int eggColor;
    private final int volume;
    private final EnumSet<LiquidEggRegistryItem.HazardFlag> hazards;
    private final boolean gaseous;

    public ChemicalEggRegistryItem(int id,
                                   ResourceLocation chemicalId,
                                   ResourceLocation texture,
                                   Component displayName,
                                   int eggColor,
                                   int volume,
                                   Set<LiquidEggRegistryItem.HazardFlag> hazards,
                                   boolean gaseous) {
        this.id = id;
        this.chemicalId = chemicalId;
        this.texture = texture;
        this.displayName = displayName;
        this.eggColor = eggColor;
        this.volume = volume;
        this.hazards = hazards.isEmpty()
                ? EnumSet.noneOf(LiquidEggRegistryItem.HazardFlag.class)
                : EnumSet.copyOf(hazards);
        this.gaseous = gaseous;
    }

    public int getId() {
        return id;
    }

    public ResourceLocation getChemicalId() {
        return chemicalId;
    }

    public ResourceLocation getTexture() {
        return texture;
    }

    public Component getDisplayName() {
        return displayName.copy();
    }

    public int getEggColor() {
        return eggColor;
    }

    public int getVolume() {
        return volume;
    }

    public Set<LiquidEggRegistryItem.HazardFlag> getHazards() {
        return Collections.unmodifiableSet(hazards);
    }

    public boolean isGaseous() {
        return gaseous;
    }

    public boolean hasHazard(LiquidEggRegistryItem.HazardFlag hazard) {
        return hazards.contains(hazard);
    }
}
