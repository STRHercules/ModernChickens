package com.setycz.chickens;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import javax.annotation.Nullable;

/**
 * Registry entry describing a single chicken type. Ported from the
 * original 1.10 implementation with modern item helpers. Each instance
 * stores breeding information, presentation data and the drop/lay
 * behaviour that the entity class consumes at runtime.
 */
public class ChickensRegistryItem {
    private final int id;
    private final String entityName;
    private ItemStack layItem;
    private ItemStack dropItem;
    private final int bgColor;
    private final int fgColor;
    private final ResourceLocation texture;
    @Nullable
    private ResourceLocation itemTexture;
    private ChickensRegistryItem parent1;
    private ChickensRegistryItem parent2;
    private SpawnType spawnType;
    private boolean enabled = true;
    private float layCoefficient = 1.0f;
    @Nullable
    private Component displayName;
    private boolean generatedTexture;
    private boolean tintItem = true;
    private boolean custom;

    public ChickensRegistryItem(int id, String entityName, ResourceLocation texture, ItemStack layItem, int bgColor, int fgColor) {
        this(id, entityName, texture, layItem, bgColor, fgColor, null, null);
    }

    public ChickensRegistryItem(int id, String entityName, ResourceLocation texture, ItemStack layItem, int bgColor, int fgColor,
            @Nullable ChickensRegistryItem parent1, @Nullable ChickensRegistryItem parent2) {
        this.id = id;
        this.entityName = entityName;
        this.layItem = layItem.copy();
        this.bgColor = bgColor;
        this.fgColor = fgColor;
        this.texture = texture;
        this.spawnType = SpawnType.NORMAL;
        this.parent1 = parent1;
        this.parent2 = parent2;
    }

    public ChickensRegistryItem setItemTexture(ResourceLocation texture) {
        itemTexture = texture;
        // Custom item sprites should render exactly as authored rather than
        // being recoloured by the legacy tint pipeline.
        tintItem = false;
        return this;
    }

    /**
     * Marks the registry item as originating from {@code chickens_custom.json}.
     * The flag lets the client-side rendering pipeline apply stricter texture
     * handling rules so bespoke resource packs remain untouched by legacy
     * fallbacks.
     */
    public ChickensRegistryItem markCustom() {
        custom = true;
        return this;
    }

    public ChickensRegistryItem setDropItem(ItemStack stack) {
        dropItem = stack.copy();
        return this;
    }

    public ChickensRegistryItem setSpawnType(SpawnType type) {
        spawnType = type;
        return this;
    }

    public ChickensRegistryItem setLayCoefficient(float coefficient) {
        layCoefficient = coefficient;
        return this;
    }

    public ChickensRegistryItem setDisplayName(Component name) {
        displayName = name;
        return this;
    }

    public ChickensRegistryItem setGeneratedTexture(boolean value) {
        generatedTexture = value;
        return this;
    }

    public String getEntityName() {
        return entityName;
    }

    @Nullable
    public ChickensRegistryItem getParent1() {
        return parent1;
    }

    @Nullable
    public ChickensRegistryItem getParent2() {
        return parent2;
    }

    public int getBgColor() {
        return bgColor;
    }

    public int getFgColor() {
        return fgColor;
    }

    public ResourceLocation getTexture() {
        return texture;
    }

    @Nullable
    public ResourceLocation getItemTexture() {
        return itemTexture;
    }

    public ItemStack createLayItem() {
        return layItem.copy();
    }

    public ItemStack createDropItem() {
        if (dropItem != null) {
            return dropItem.copy();
        }
        return createLayItem();
    }

    public int getTier() {
        if (parent1 == null || parent2 == null) {
            return 1;
        }
        return Math.max(parent1.getTier(), parent2.getTier()) + 1;
    }

    public boolean isChildOf(ChickensRegistryItem possibleParent1, ChickensRegistryItem possibleParent2) {
        return parent1 == possibleParent1 && parent2 == possibleParent2 || parent1 == possibleParent2 && parent2 == possibleParent1;
    }

    public boolean isDye() {
        return layItem.getItem() instanceof DyeItem;
    }

    public boolean isDye(Ingredient colour) {
        return isDye() && colour.test(layItem);
    }

    public boolean isDye(DyeColor colour) {
        DyeColor dyeColor = getDyeColor();
        return dyeColor != null && dyeColor == colour;
    }

    @Nullable
    public DyeColor getDyeColor() {
        if (layItem.getItem() instanceof DyeItem dyeItem) {
            return dyeItem.getDyeColor();
        }
        return null;
    }

    public int getId() {
        return id;
    }

    public boolean canSpawn() {
        return getTier() == 1 && spawnType != SpawnType.NONE;
    }

    public int getMinLayTime() {
        return (int) Math.max(6000 * getTier() * layCoefficient, 1.0f);
    }

    public int getMaxLayTime() {
        return 2 * getMinLayTime();
    }

    public SpawnType getSpawnType() {
        return spawnType;
    }

    public boolean isImmuneToFire() {
        return spawnType == SpawnType.HELL;
    }

    public void setEnabled(boolean value) {
        enabled = value;
    }

    public boolean isEnabled() {
        return enabled && (parent1 == null || parent1.isEnabled()) && (parent2 == null || parent2.isEnabled());
    }

    public void setLayItem(ItemStack itemStack) {
        layItem = itemStack.copy();
    }

    public void setNoParents() {
        parent1 = null;
        parent2 = null;
    }

    public ChickensRegistryItem setParentsNew(ChickensRegistryItem newParent1, ChickensRegistryItem newParent2) {
        parent1 = newParent1;
        parent2 = newParent2;
        return this;
    }

    public boolean isBreedable() {
        return parent1 != null && parent2 != null;
    }

    public Component getDisplayName() {
        if (displayName != null) {
            return displayName.copy();
        }
        return Component.translatable("entity." + entityName + ".name");
    }

    public boolean hasGeneratedTexture() {
        return generatedTexture;
    }

    public boolean shouldTintItem() {
        return tintItem;
    }

    public void setTintItem(boolean value) {
        tintItem = value;
    }

    public boolean isCustom() {
        return custom;
    }
}
