package strhercules.chickens.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

public final class ItemData {
    public static final String ENTITY_TAG = "EntityTag";
    public static final String BLOCK_ENTITY_TAG = "BlockEntityTag";

    private ItemData() {
    }

    public static void update(ItemStack stack, Consumer<CompoundTag> mutator) {
        mutator.accept(stack.getOrCreateTag());
    }

    public static CompoundTag read(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? new CompoundTag() : tag;
    }

    public static CompoundTag readEntity(ItemStack stack) {
        return read(stack).getCompound(ENTITY_TAG);
    }

    public static boolean hasEntity(ItemStack stack) {
        return read(stack).contains(ENTITY_TAG);
    }

    public static void writeEntity(ItemStack stack, CompoundTag entityData) {
        stack.getOrCreateTag().put(ENTITY_TAG, entityData);
    }

    public static void updateEntity(ItemStack stack, Consumer<CompoundTag> mutator) {
        CompoundTag root = stack.getOrCreateTag();
        CompoundTag entity = root.getCompound(ENTITY_TAG);
        mutator.accept(entity);
        root.put(ENTITY_TAG, entity);
    }

    public static CompoundTag readBlockEntity(ItemStack stack) {
        return read(stack).getCompound(BLOCK_ENTITY_TAG);
    }

    public static void writeBlockEntity(ItemStack stack, CompoundTag blockEntityData) {
        stack.getOrCreateTag().put(BLOCK_ENTITY_TAG, blockEntityData);
    }
}
