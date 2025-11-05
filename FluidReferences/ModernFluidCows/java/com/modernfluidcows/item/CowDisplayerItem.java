package com.modernfluidcows.item;

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
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Item that enumerates every configured fluid cow and allows players to spawn specific variants.
 *
 * <p>The legacy item stored the target fluid id directly in the stack NBT and spawned a configured
 * {@link FluidCow} when used. This NeoForge port mirrors that behaviour while delegating to modern
 * spawning helpers and {@link ResourceLocation} identifiers.</p>
 */
public class CowDisplayerItem extends Item {
    private static final String NBT_FLUID = "fluid";
    private static final RandomSource RANDOM = RandomSource.create();

    public CowDisplayerItem(final Properties properties) {
        super(properties);
    }

    /** Writes the given fluid id to the supplied stack so creative tabs can present preconfigured cows. */
    public static ItemStack applyFluidToItemStack(final ItemStack stack, final Fluid fluid) {
        ResourceLocation key = BuiltInRegistries.FLUID.getKey(fluid);
        if (key != null) {
            writeFluid(stack, key);
        }
        return stack;
    }

    /** Populates the creative tab with every available fluid variant. */
    public void addCreativeStacks(final CreativeModeTab.Output output) {
        for (Fluid fluid : FCUtils.getBucketFluids()) {
            output.accept(applyFluidToItemStack(new ItemStack(this), fluid));
        }
    }

    @Override
    public Component getName(final ItemStack stack) {
        Optional<ResourceLocation> fluidKey = getFluidKey(stack);
        if (fluidKey.isEmpty()) {
            return super.getName(stack);
        }
        Fluid fluid = resolveFluid(fluidKey.get());
        if (fluid == null) {
            return super.getName(stack);
        }
        return Component.translatable(
                "item.fluidcows.cow_displayer.named",
                Component.translatable("entity.fluidcows.fluid_cow"),
                Component.literal(FCUtils.getFluidName(fluid)));
    }

    @Override
    public void appendHoverText(
            final ItemStack stack,
            final Item.TooltipContext context,
            final List<Component> tooltip,
            final TooltipFlag flag) {
        Optional<ResourceLocation> fluidKey = getFluidKey(stack);
        if (fluidKey.isEmpty()) {
            return;
        }
        Fluid fluid = resolveFluid(fluidKey.get());
        if (fluid == null) {
            return;
        }

        tooltip.add(
                Component.translatable(
                                "tooltip.fluidcows.cow_displayer.fluid",
                                FCUtils.getFluidName(fluid))
                        .withStyle(style -> style.withItalic(false)));

        if (FCConfig.loaded) {
            int worldCooldownTicks = FCConfig.getWorldCD(fluidKey.get());
            int stallCooldownTicks = FCConfig.getStallCD(fluidKey.get());
            tooltip.add(Component.translatable(
                            "tooltip.fluidcows.cow_displayer.world_cooldown",
                            FCUtils.toTime(worldCooldownTicks / 20, "Now"))
                    .withStyle(style -> style.withItalic(false)));
            tooltip.add(Component.translatable(
                            "tooltip.fluidcows.cow_displayer.stall_cooldown",
                            FCUtils.toTime(stallCooldownTicks / 20, "Now"))
                    .withStyle(style -> style.withItalic(false)));

            tooltip.add(Component.translatable(
                            "tooltip.fluidcows.cow_displayer.spawn_info",
                            describeSpawnState(fluidKey.get()))
                    .withStyle(style -> style.withItalic(false)));
        }

        tooltip.add(Component.translatable("tooltip.fluidcows.cow_displayer.right_click")
                .withStyle(style -> style.withItalic(false)));
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        Level level = context.getLevel();
        Optional<ResourceLocation> fluidKey = getFluidKey(context.getItemInHand());
        if (fluidKey.isEmpty()) {
            return InteractionResult.FAIL;
        }
        Fluid fluid = resolveFluid(fluidKey.get());
        if (fluid == null) {
            return InteractionResult.FAIL;
        }

        Player player = context.getPlayer();
        if (!canSpawn(fluidKey.get(), player)) {
            return InteractionResult.FAIL;
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        BlockPos spawnPos = context.getClickedPos().relative(context.getClickedFace());
        double offset = getYOffset(level, spawnPos);
        Vec3 spawn = Vec3.atBottomCenterOf(spawnPos).add(0.0D, offset, 0.0D);
        if (spawnCow(serverLevel, spawn, fluid, player)) {
            consumeItem(player, context.getItemInHand());
            return InteractionResult.SUCCESS;
        }

        sendFailureMessage(player, "message.fluidcows.cow_displayer.failed");
        return InteractionResult.FAIL;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            final Level level, final Player player, final InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Optional<ResourceLocation> fluidKey = getFluidKey(stack);
        if (fluidKey.isEmpty()) {
            return InteractionResultHolder.fail(stack);
        }
        Fluid fluid = resolveFluid(fluidKey.get());
        if (fluid == null) {
            return InteractionResultHolder.fail(stack);
        }

        if (!canSpawn(fluidKey.get(), player)) {
            return InteractionResultHolder.fail(stack);
        }

        BlockHitResult hitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hitResult.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(stack);
        }

        BlockPos hitPos = hitResult.getBlockPos();
        if (!level.mayInteract(player, hitPos)) {
            return InteractionResultHolder.fail(stack);
        }

        FluidState fluidState = level.getFluidState(hitPos);
        if (!fluidState.isSource()) {
            return InteractionResultHolder.pass(stack);
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.success(stack);
        }

        if (spawnCow(serverLevel, Vec3.atCenterOf(hitPos), fluid, player)) {
            consumeItem(player, stack);
            return InteractionResultHolder.success(stack);
        }

        sendFailureMessage(player, "message.fluidcows.cow_displayer.failed");
        return InteractionResultHolder.fail(stack);
    }

    private boolean canSpawn(final ResourceLocation key, @Nullable final Player player) {
        if (FCConfig.loaded && !FCConfig.isEnable(key)) {
            sendFailureMessage(player, "message.fluidcows.cow_displayer.disabled");
            return false;
        }
        return true;
    }

    private boolean spawnCow(
            final ServerLevel level, final Vec3 position, final Fluid fluid, @Nullable final Player player) {
        FluidCow cow = FluidCowsRegistries.FLUID_COW.get().create(level);
        if (cow == null) {
            return false;
        }

        float yaw = RANDOM.nextFloat() * 360.0F;
        cow.moveTo(position.x, position.y, position.z, yaw, 0.0F);
        cow.setYHeadRot(yaw);
        cow.setYBodyRot(yaw);

        if (cow instanceof Mob mob) {
            mob.finalizeSpawn(
                    level,
                    level.getCurrentDifficultyAt(BlockPos.containing(position)),
                    MobSpawnType.SPAWN_EGG,
                    null);
        }

        cow.setFluid(fluid);
        cow.refreshWorldCooldown();
        level.addFreshEntity(cow);
        cow.playAmbientSound();

        return true;
    }

    private static void consumeItem(@Nullable final Player player, final ItemStack stack) {
        if (player == null || player.getAbilities().instabuild) {
            return;
        }
        stack.shrink(1);
    }

    private static double getYOffset(final Level level, final BlockPos pos) {
        AABB bounds = new AABB(pos);
        List<VoxelShape> collisions = level.getEntityCollisions(null, bounds);
        if (collisions.isEmpty()) {
            return 0.0D;
        }
        double y = bounds.minY;
        for (VoxelShape collision : collisions) {
            for (AABB shapeBox : collision.toAabbs()) {
                y = Math.max(shapeBox.maxY, y);
            }
        }
        return y - pos.getY();
    }

    private static Optional<ResourceLocation> getFluidKey(final ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return Optional.empty();
        }
        CompoundTag tag = data.copyTag();
        if (!tag.contains(NBT_FLUID)) {
            return Optional.empty();
        }
        return Optional.ofNullable(ResourceLocation.tryParse(tag.getString(NBT_FLUID)));
    }

    @Nullable
    private static Fluid resolveFluid(final ResourceLocation key) {
        return BuiltInRegistries.FLUID.getOptional(key).filter(fluid -> fluid != Fluids.EMPTY).orElse(null);
    }

    private static void writeFluid(final ItemStack stack, final ResourceLocation key) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = data != null ? data.copyTag() : new CompoundTag();
        tag.putString(NBT_FLUID, key.toString());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static void sendFailureMessage(@Nullable final Player player, final String translationKey) {
        if (player != null) {
            player.sendSystemMessage(Component.translatable(translationKey));
        }
    }

    private static Component describeSpawnState(final ResourceLocation key) {
        if (!FCConfig.loaded) {
            return Component.translatable("tooltip.fluidcows.cow_displayer.spawn_unknown");
        }

        if (!FCConfig.isEnable(key)) {
            return Component.translatable("tooltip.fluidcows.cow_displayer.spawn_disabled");
        }

        boolean canBreed = FCConfig.canBreed.contains(key);
        int rate = FCConfig.getRate(key);

        if (canBreed && rate == 0) {
            return Component.translatable("tooltip.fluidcows.cow_displayer.spawn_breed_only");
        }
        if (canBreed && rate > 0) {
            return Component.translatable("tooltip.fluidcows.cow_displayer.spawn_world_and_breed");
        }
        if (rate > 0) {
            return Component.translatable("tooltip.fluidcows.cow_displayer.spawn_world");
        }
        return Component.translatable("tooltip.fluidcows.cow_displayer.spawn_crafted");
    }
}
