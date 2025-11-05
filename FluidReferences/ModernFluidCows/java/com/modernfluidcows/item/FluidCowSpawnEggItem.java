package com.modernfluidcows.item;

import com.modernfluidcows.entity.FluidCow;
import com.modernfluidcows.util.FCUtils;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

/**
 * Spawn egg that mirrors the legacy mod by storing the configured fluid and cooldown directly in
 * the stack. NeoForge will merge the {@code EntityTag} payload into the spawned cow so the entity
 * inherits the previewed data without custom packets.
 */
public class FluidCowSpawnEggItem extends SpawnEggItem {
    private static final String ENTITY_TAG = "EntityTag";

    public FluidCowSpawnEggItem(
            final EntityType<? extends Mob> type,
            final int primaryColor,
            final int secondaryColor,
            final Item.Properties properties) {
        super(type, primaryColor, secondaryColor, properties);
    }

    @Override
    public void appendHoverText(
            final ItemStack stack,
            final Item.TooltipContext context,
            final java.util.List<Component> tooltip,
            final TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        Optional<CompoundTag> data = getEntityData(stack);
        if (data.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.fluidcows.spawn_egg.random")
                    .withStyle(style -> style.withItalic(false)));
            return;
        }

        CompoundTag tag = data.get();
        Fluid fluid = resolveFluid(tag.getString(FluidCow.HALTER_TAG_FLUID));
        if (fluid == null) {
            tooltip.add(Component.translatable("tooltip.fluidcows.spawn_egg.random")
                    .withStyle(style -> style.withItalic(false)));
            return;
        }

        tooltip.add(Component.translatable(
                        "tooltip.fluidcows.spawn_egg.fluid",
                        FCUtils.getFluidName(fluid))
                .withStyle(style -> style.withItalic(false)));

        int cooldown = Math.max(0, tag.getInt(FluidCow.HALTER_TAG_COOLDOWN));
        tooltip.add(Component.translatable(
                        "tooltip.fluidcows.spawn_egg.cooldown",
                        FCUtils.toTime(cooldown / 20, "Now"))
                .withStyle(style -> style.withItalic(false)));
    }

    /** Writes the supplied fluid/cooldown pair to the spawn egg so JEI can surface the details. */
    public static ItemStack applyCowData(
            final ItemStack stack, final Fluid fluid, final int cooldownTicks) {
        ResourceLocation key = BuiltInRegistries.FLUID.getKey(fluid);
        if (key == null) {
            return stack;
        }

        CompoundTag entityTag = getOrCreateEntityTag(stack);
        entityTag.putString(FluidCow.HALTER_TAG_FLUID, key.toString());
        if (cooldownTicks > 0) {
            entityTag.putInt(FluidCow.HALTER_TAG_COOLDOWN, Math.max(0, cooldownTicks));
        } else {
            entityTag.remove(FluidCow.HALTER_TAG_COOLDOWN);
        }
        persistEntityTag(stack, entityTag);
        return stack;
    }

    private static Optional<CompoundTag> getEntityData(final ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return Optional.empty();
        }
        CompoundTag root = data.copyTag();
        if (!root.contains(ENTITY_TAG, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        return Optional.of(root.getCompound(ENTITY_TAG).copy());
    }

    private static CompoundTag getOrCreateEntityTag(final ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null) {
            CompoundTag root = data.copyTag();
            if (root.contains(ENTITY_TAG, Tag.TAG_COMPOUND)) {
                return root.getCompound(ENTITY_TAG).copy();
            }
        }
        return new CompoundTag();
    }

    private static void persistEntityTag(final ItemStack stack, final CompoundTag entityTag) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag root = data != null ? data.copyTag() : new CompoundTag();
        root.put(ENTITY_TAG, entityTag.copy());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    @Nullable
    private static Fluid resolveFluid(final String key) {
        if (key.isEmpty()) {
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(key);
        if (id == null) {
            return null;
        }
        return BuiltInRegistries.FLUID.getOptional(id).orElse(null);
    }
}

