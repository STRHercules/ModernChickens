package com.modernfluidcows.item;

import com.modernfluidcows.blockentity.StallBlockEntity;
import com.modernfluidcows.config.FCConfig;
import com.modernfluidcows.entity.FluidCow;
import com.modernfluidcows.registry.FluidCowsRegistries;
import com.modernfluidcows.util.FCUtils;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Item that stores a captured {@link FluidCow} and allows players to relocate or convert the cow.
 *
 * <p>The legacy halter persisted a subset of the entity's data directly in NBT. This port mirrors
 * that behaviour with modern data components so we can reuse the same JSON configuration and
 * gameplay rules.</p>
 */
public class CowHalterItem extends Item {
    private static final String ROOT_TAG = "FluidCow";
    private static final String TAG_FLUID = "Fluid";
    private static final String TAG_COOLDOWN = "Cooldown";
    private static final String TAG_IN_LOVE = "InLove";
    private static final String TAG_AGE = "Age";
    private static final String TAG_FORCED_AGE = "ForcedAge";
    private static final String TAG_HEALTH = "Health";

    public CowHalterItem(final Properties properties) {
        // Restrict the stack size so each halter either remains empty or stores a single cow.
        super(properties.stacksTo(1));
    }

    /** Returns {@code true} when the supplied stack already carries a captured fluid cow. */
    public static boolean hasCapturedCow(final ItemStack stack) {
        return getStoredData(stack).isPresent();
    }

    @Override
    public Component getName(final ItemStack stack) {
        Optional<CompoundTag> data = getStoredData(stack);
        if (data.isEmpty()) {
            return super.getName(stack);
        }
        Fluid fluid = resolveFluid(data.get().getString(TAG_FLUID));
        if (fluid == null) {
            return super.getName(stack);
        }
        // Surface the captured fluid so players can identify the stored cow at a glance.
        return Component.translatable(
                "item.fluidcows.cow_halter.named",
                super.getName(stack),
                Component.literal(FCUtils.getFluidName(fluid)));
    }

    @Override
    public InteractionResult interactLivingEntity(
            final ItemStack stack,
            final Player player,
            final LivingEntity target,
            final InteractionHand usedHand) {
        if (!(target instanceof FluidCow fluidCow)) {
            return InteractionResult.PASS;
        }
        if (usedHand == InteractionHand.OFF_HAND) {
            return InteractionResult.PASS;
        }
        Level level = player.level();
        if (level.isClientSide) {
            // Early out on the client so the server performs the real capture logic.
            return InteractionResult.SUCCESS;
        }
        if (hasCapturedCow(stack) || fluidCow.isBaby() || !fluidCow.isAlive()) {
            if (fluidCow.isBaby()) {
                sendStatus(player, "message.fluidcows.cow_halter.baby");
            }
            return InteractionResult.FAIL;
        }

        ItemStack filled = new ItemStack(this);
        // Persist the entity attributes that influence breeding, cooldowns, and health state.
        writeStoredData(filled, fluidCow.writeHalterData());

        if (!player.addItem(filled)) {
            player.drop(filled, false);
        }

        fluidCow.discard();
        stack.shrink(1);
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = context.getPlayer();

        if (state.is(FluidCowsRegistries.STALL_BLOCK.get())) {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof StallBlockEntity stall)) {
                return InteractionResult.PASS;
            }

            if (hasCapturedCow(stack)) {
                Optional<CompoundTag> data = getStoredData(stack);
                if (data.isPresent() && stall.insertCow(data.get())) {
                    clearStoredData(stack);
                    return InteractionResult.SUCCESS;
                }
                if (player != null) {
                    sendStatus(player, "message.fluidcows.cow_halter.stall_full");
                }
                return InteractionResult.FAIL;
            }

            Optional<CompoundTag> extracted = stall.extractCow();
            if (extracted.isPresent()) {
                writeStoredData(stack, extracted.get());
                return InteractionResult.SUCCESS;
            }
            if (player != null) {
                sendStatus(player, "message.fluidcows.cow_halter.stall_empty");
            }
            return InteractionResult.FAIL;
        }

        if (!hasCapturedCow(stack)) {
            return InteractionResult.PASS;
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        Vec3 spawn =
                Vec3.atBottomCenterOf(context.getClickedPos().relative(context.getClickedFace()))
                        .add(0.0D, 0.5D, 0.0D);
        if (spawnCow(serverLevel, stack, spawn, context.getPlayer())) {
            clearStoredData(stack);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.FAIL;
    }

    @Override
    public boolean hurtEnemy(
            final ItemStack stack, final LivingEntity target, final LivingEntity attacker) {
        if (!(attacker instanceof Player player) || !(target instanceof FluidCow fluidCow)) {
            return super.hurtEnemy(stack, target, attacker);
        }
        Level level = attacker.level();
        if (level.isClientSide) {
            return true;
        }

        if (!FCConfig.loaded || !FCConfig.enableConvertCowToDisplayer) {
            sendStatus(player, "message.fluidcows.cow_halter.convert_disabled");
            return false;
        }
        if (fluidCow.isBaby()) {
            sendStatus(player, "message.fluidcows.cow_halter.baby");
            return false;
        }

        Fluid fluid = fluidCow.getFluid();
        if (fluid == null) {
            sendStatus(player, "message.fluidcows.cow_halter.empty");
            return false;
        }
        ResourceLocation key = BuiltInRegistries.FLUID.getKey(fluid);
        if (key != null && FCConfig.blackListCowToDisplayer.contains(key.toString())) {
            sendStatus(player, "message.fluidcows.cow_halter.blacklisted");
            return false;
        }

        ItemStack displayer =
                CowDisplayerItem.applyFluidToItemStack(
                        new ItemStack(FluidCowsRegistries.COW_DISPLAYER.get()), fluid);
        if (!player.addItem(displayer)) {
            player.drop(displayer, false);
        }
        fluidCow.discard();
        return true;
    }

    @Override
    public void appendHoverText(
            final ItemStack stack,
            final Item.TooltipContext context,
            final List<Component> tooltip,
            final TooltipFlag flag) {
        Optional<CompoundTag> data = getStoredData(stack);
        if (data.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.fluidcows.cow_halter.empty")
                    .withStyle(style -> style.withItalic(false)));
            tooltip.add(Component.translatable("tooltip.fluidcows.cow_halter.warning")
                    .withStyle(style -> style.withItalic(false)));
            tooltip.add(Component.translatable("tooltip.fluidcows.cow_halter.convert")
                    .withStyle(style -> style.withItalic(false)));
            tooltip.add(Component.translatable("tooltip.fluidcows.cow_halter.warning")
                    .withStyle(style -> style.withItalic(false)));
            return;
        }

        CompoundTag tag = data.get();
        Fluid fluid = resolveFluid(tag.getString(TAG_FLUID));
        if (fluid != null) {
            tooltip.add(Component.translatable(
                            "tooltip.fluidcows.cow_halter.fluid", FCUtils.getFluidName(fluid))
                    .withStyle(style -> style.withItalic(false)));
        }

        int cooldown = tag.getInt(TAG_COOLDOWN);
        tooltip.add(Component.translatable(
                        "tooltip.fluidcows.cow_halter.next_usage",
                        FCUtils.toTime(cooldown / 20, "Now"))
                .withStyle(style -> style.withItalic(false)));
        tooltip.add(Component.translatable("tooltip.fluidcows.cow_halter.warning")
                .withStyle(style -> style.withItalic(false)));
        tooltip.add(Component.translatable("tooltip.fluidcows.cow_halter.convert")
                .withStyle(style -> style.withItalic(false)));
        tooltip.add(Component.translatable("tooltip.fluidcows.cow_halter.warning")
                .withStyle(style -> style.withItalic(false)));
    }

    private static void writeStoredData(final ItemStack stack, final CompoundTag halterData) {
        CompoundTag root = new CompoundTag();
        root.put(ROOT_TAG, halterData);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    private static Optional<CompoundTag> getStoredData(final ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return Optional.empty();
        }
        CompoundTag root = data.copyTag();
        if (!root.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        return Optional.of(root.getCompound(ROOT_TAG).copy());
    }

    /** Removes the captured cow data so the halter becomes reusable. */
    private static void clearStoredData(final ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return;
        }
        CompoundTag root = data.copyTag();
        root.remove(ROOT_TAG);
        if (root.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        }
    }

    /** Spawns a new Fluid Cow using the state stored inside the halter. */
    private static boolean spawnCow(
            final ServerLevel level,
            final ItemStack stack,
            final Vec3 position,
            @Nullable final Player owner) {
        Optional<CompoundTag> data = getStoredData(stack);
        if (data.isEmpty()) {
            return false;
        }
        FluidCow cow = FluidCowsRegistries.FLUID_COW.get().create(level);
        if (cow == null) {
            return false;
        }

        cow.readHalterData(data.get());
        float yaw = owner == null ? 0.0F : owner.getYRot();
        cow.moveTo(position.x, position.y, position.z, yaw, 0.0F);
        cow.setYBodyRot(yaw);
        cow.setYHeadRot(yaw);
        level.addFreshEntity(cow);
        return true;
    }

    /** Resolves a registry-backed fluid instance for tooltip rendering and conversion checks. */
    private static Fluid resolveFluid(final String name) {
        if (name.isEmpty()) {
            return null;
        }
        ResourceLocation key = ResourceLocation.tryParse(name);
        return key == null ? null : BuiltInRegistries.FLUID.getOptional(key).orElse(null);
    }

    private static void sendStatus(final Player player, final String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }
}
