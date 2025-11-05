package com.modernfluidcows.entity;

import com.modernfluidcows.config.FCConfig;
import com.modernfluidcows.registry.FluidCowsRegistries;
import com.modernfluidcows.item.FluidCowSpawnEggItem;
import com.modernfluidcows.util.FCUtils;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import org.jetbrains.annotations.Nullable;

/**
 * Port of the legacy {@code EntityFluidCow} that keeps track of a bound fluid and milk cooldown.
 *
 * <p>The original implementation used data watchers and manual packets for synchronisation. The
 * NeoForge build relies on {@link SynchedEntityData} and optional {@link ResourceLocation}
 * tracking instead, which keeps server and client aligned automatically.</p>
 */
public class FluidCow extends Cow {
    private static final EntityDataAccessor<Integer> DATA_FLUID =
            SynchedEntityData.defineId(FluidCow.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_COOLDOWN =
            SynchedEntityData.defineId(FluidCow.class, EntityDataSerializers.INT);

    /** Shared key used by the stall and halter to reference the bound fluid. */
    public static final String HALTER_TAG_FLUID = "Fluid";
    /** Shared key used to persist the milk cooldown between entities and stalls. */
    public static final String HALTER_TAG_COOLDOWN = "Cooldown";
    public static final String HALTER_TAG_IN_LOVE = "InLove";
    public static final String HALTER_TAG_AGE = "Age";
    public static final String HALTER_TAG_FORCED_AGE = "ForcedAge";
    public static final String HALTER_TAG_HEALTH = "Health";

    private static final String NBT_FLUID = HALTER_TAG_FLUID;
    private static final String NBT_COOLDOWN = HALTER_TAG_COOLDOWN;
    private static final String NBT_IN_LOVE = HALTER_TAG_IN_LOVE;
    private static final String NBT_AGE = HALTER_TAG_AGE;
    private static final String NBT_FORCED_AGE = HALTER_TAG_FORCED_AGE;
    private static final String NBT_HEALTH = HALTER_TAG_HEALTH;

    private boolean acceleratedThisTick;

    public FluidCow(final EntityType<? extends Cow> type, final Level level) {
        super(type, level);
    }

    /** Reuses the vanilla cow attributes so hit points and speed match the legacy behaviour. */
    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder createAttributes() {
        return Cow.createAttributes();
    }

    @Override
    protected void defineSynchedData(final SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        // Default to an invalid id so client joins do not crash while the server assigns data.
        builder.define(DATA_FLUID, -1);
        builder.define(DATA_COOLDOWN, 0);
    }

    @Override
    public void tick() {
        acceleratedThisTick = false;
        super.tick();
        if (!this.level().isClientSide) {
            int cooldown = getCooldown();
            if (!this.isBaby() && cooldown > 0) {
                setCooldown(cooldown - 1);
            }
        }
    }

    /**
     * Applies the accelerator growth effect to the cow.
     */
    public boolean tryAccelerateGrowth() {
        if (this.level().isClientSide || acceleratedThisTick) {
            return false;
        }
        int age = getAge();
        if (age >= 0) {
            return false;
        }
        int newAge = Math.min(age + FCConfig.acceleratorMultiplier, 0);
        setAge(newAge);
        if (newAge >= 0) {
            setAge(0);
            setBaby(false);
        }
        acceleratedThisTick = true;
        return true;
    }

    @Override
    public boolean checkSpawnRules(final LevelAccessor level, final MobSpawnType spawnReason) {
        if (!super.checkSpawnRules(level, spawnReason)) {
            return false;
        }
        if (spawnReason == MobSpawnType.NATURAL && FCConfig.spawnWeight <= 0) {
            return false;
        }
        // The legacy configuration stores integer dimension ids. Map the default three to maintain parity.
        if (level instanceof Level serverLevel) {
            int legacyId = 0;
            if (serverLevel.dimension() == Level.NETHER) {
                legacyId = -1;
            } else if (serverLevel.dimension() == Level.END) {
                legacyId = 1;
            }
            if (FCConfig.blackListDimIds.contains(legacyId)) {
                return false;
            }
            ResourceLocation biomeKey = serverLevel.getBiome(blockPosition())
                    .unwrapKey()
                    .map(net.minecraft.resources.ResourceKey::location)
                    .orElse(null);
            if (biomeKey != null && FCConfig.isBiomeBlacklisted(biomeKey)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public SpawnGroupData finalizeSpawn(
            final ServerLevelAccessor level,
            final net.minecraft.world.DifficultyInstance difficulty,
            final MobSpawnType reason,
            @Nullable final SpawnGroupData spawnData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, spawnData);
        if (!level.getLevel().isClientSide) {
            Fluid assigned = getFluid();
            if (assigned == null) {
                // Pick a random enabled fluid; default to water so cows never end up fluid-less.
                Fluid fluid = Optional.ofNullable(FCUtils.getRandFluid()).orElse(Fluids.WATER);
                setFluid(fluid);
                refreshWorldCooldown();
            } else if (getCooldown() < 0) {
                // Spawn eggs can supply explicit cooldowns. Clamp any invalid data so production
                // timers never go negative when the entity joins the world.
                setCooldown(0);
            }
        }
        return data;
    }

    @Override
    public void readAdditionalSaveData(final CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(NBT_FLUID)) {
            ResourceLocation key = ResourceLocation.tryParse(tag.getString(NBT_FLUID));
            if (key != null) {
                BuiltInRegistries.FLUID.getOptional(key).ifPresent(this::setFluid);
            } else {
                setFluid(null);
            }
        } else {
            setFluid(null);
        }
        if (tag.contains(NBT_COOLDOWN)) {
            setCooldown(tag.getInt(NBT_COOLDOWN));
        }
    }

    @Override
    public void addAdditionalSaveData(final CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        Fluid fluid = getFluid();
        if (fluid != null) {
            ResourceLocation key = BuiltInRegistries.FLUID.getKey(fluid);
            if (key != null) {
                tag.putString(NBT_FLUID, key.toString());
            }
        }
        tag.putInt(NBT_COOLDOWN, getCooldown());
    }

    /**
     * Serialises the subset of entity data that the cow halter needs in order to recreate a cow.
     *
     * <p>The legacy mod persisted this information directly into the halter's item stack. We mirror
     * that so existing configuration values (breeding cooldowns, forced ages) continue to apply.</p>
     */
    public CompoundTag writeHalterData() {
        CompoundTag tag = new CompoundTag();
        Fluid fluid = getFluid();
        if (fluid != null) {
            ResourceLocation key = BuiltInRegistries.FLUID.getKey(fluid);
            if (key != null) {
                tag.putString(NBT_FLUID, key.toString());
            }
        }
        tag.putInt(NBT_COOLDOWN, getCooldown());
        tag.putInt(NBT_IN_LOVE, this.getInLoveTime());
        tag.putInt(NBT_AGE, this.getAge());
        tag.putInt(NBT_FORCED_AGE, this.forcedAge);
        tag.putFloat(NBT_HEALTH, this.getHealth());
        return tag;
    }

    /**
     * Restores entity state that was previously saved by {@link #writeHalterData()}.
     *
     * <p>This keeps captured cows faithful to their pre-halter stats so cooldowns and breeding
     * status resume exactly where the player left off.</p>
     */
    public void readHalterData(final CompoundTag tag) {
        if (tag.contains(HALTER_TAG_FLUID)) {
            ResourceLocation key = ResourceLocation.tryParse(tag.getString(HALTER_TAG_FLUID));
            if (key != null) {
                BuiltInRegistries.FLUID.getOptional(key).ifPresent(this::setFluid);
            }
        }
        setCooldown(tag.getInt(HALTER_TAG_COOLDOWN));
        if (tag.contains(HALTER_TAG_IN_LOVE)) {
            setInLoveTime(tag.getInt(HALTER_TAG_IN_LOVE));
        }
        if (tag.contains(HALTER_TAG_AGE)) {
            setAge(tag.getInt(HALTER_TAG_AGE));
        }
        if (tag.contains(HALTER_TAG_FORCED_AGE)) {
            this.forcedAge = tag.getInt(HALTER_TAG_FORCED_AGE);
        }
        if (tag.contains(HALTER_TAG_HEALTH)) {
            setHealth(tag.getFloat(HALTER_TAG_HEALTH));
        }
    }

    @Override
    public InteractionResult mobInteract(final Player player, final InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()) {
            return super.mobInteract(player, hand);
        }

        boolean isBucket = held.is(Items.BUCKET);
        Optional<IFluidHandlerItem> handler = Optional.empty();
        ItemStack single = ItemStack.EMPTY;
        if (!isBucket) {
            single = held.copy();
            single.setCount(1);
            handler = FluidUtil.getFluidHandler(single);
        }

        // Only fluid containers need special handling; fall back to vanilla logic for everything else.
        if (!isBucket && handler.isEmpty()) {
            return super.mobInteract(player, hand);
        }

        // Prevent vanilla milking from firing when the cow has not been configured yet.
        Fluid fluid = getFluid();
        if (fluid == null) {
            return InteractionResult.CONSUME;
        }

        // Mirror vanilla client behaviour so the hand animation still plays instantly.
        if (this.level().isClientSide) {
            return InteractionResult.sidedSuccess(true);
        }

        if (!canMilk() || held.getCount() <= 0) {
            return InteractionResult.CONSUME;
        }

        if (isBucket) {
            // Vanilla buckets lack a fluid capability, so mirror their classic behaviour by swapping
            // in the fluid's registered bucket item and leaving creative stacks intact.
            Item bucketItem = fluid.getBucket();
            if (bucketItem == Items.AIR) {
                return InteractionResult.CONSUME;
            }

            ItemStack filledBucket = new ItemStack(bucketItem);
            player.playSound(SoundEvents.COW_MILK, 1.0F, 1.0F);
            if (player.getAbilities().instabuild) {
                player.setItemInHand(hand, filledBucket);
            } else {
                held.shrink(1);
                if (held.isEmpty()) {
                    player.setItemInHand(hand, filledBucket);
                } else if (!player.addItem(filledBucket)) {
                    player.drop(filledBucket, false);
                }
            }
            refreshWorldCooldown();
            return InteractionResult.sidedSuccess(false);
        }

        // General fluid containers expose a capability, so hand the fluid stack to it and distribute
        // the filled container back to the player when the transfer succeeds.
        if (handler.isEmpty()) {
            return InteractionResult.CONSUME;
        }
        IFluidHandlerItem fluidHandler = handler.get();
        FluidStack milkStack = new FluidStack(fluid, FluidType.BUCKET_VOLUME);
        int filled = fluidHandler.fill(milkStack, IFluidHandler.FluidAction.EXECUTE);
        if (filled != FluidType.BUCKET_VOLUME) {
            return InteractionResult.CONSUME;
        }

        ItemStack container = fluidHandler.getContainer().copy();
        player.playSound(SoundEvents.COW_MILK, 1.0F, 1.0F);
        if (player.getAbilities().instabuild) {
            player.setItemInHand(hand, container);
        } else {
            held.shrink(1);
            if (held.isEmpty()) {
                player.setItemInHand(hand, container);
            } else if (!player.addItem(container)) {
                player.drop(container, false);
            }
        }
        refreshWorldCooldown();
        return InteractionResult.sidedSuccess(false);
    }

    @Override
    public boolean canMate(final Animal other) {
        if (!(other instanceof FluidCow fluidCow)) {
            return false;
        }
        Fluid self = getFluid();
        Fluid theirs = fluidCow.getFluid();
        if (self == null || theirs == null) {
            return false;
        }
        return FCConfig.canMateWith(self, theirs) && super.canMate(other);
    }

    @Override
    public FluidCow getBreedOffspring(final ServerLevel level, final AgeableMob partner) {
        FluidCow mate = (FluidCow) partner;
        Fluid first = Optional.ofNullable(getFluid()).orElse(Fluids.WATER);
        Fluid second = Optional.ofNullable(mate.getFluid()).orElse(first);
        Fluid result = FCConfig.pickBreedingResult(first, second, level.getRandom());
        FluidCow child = FluidCowsRegistries.FLUID_COW.get().create(level);
        if (child == null) {
            return null;
        }
        child.setFluid(result);
        ResourceLocation key = BuiltInRegistries.FLUID.getKey(result);
        if (key != null) {
            child.setAge(FCConfig.getGrowBaby(key));
        }
        child.refreshWorldCooldown();
        return child;
    }

    @Override
    public Component getName() {
        Component base = super.getName();
        Fluid fluid = getFluid();
        if (fluid == null) {
            return base;
        }
        return base.copy().append(Component.literal(" (" + FCUtils.getFluidName(fluid) + ")"));
    }

    @Override
    public ItemStack getPickedResult(final HitResult target) {
        // Mirror the vanilla cow by handing creative pick block a spawn egg that spawns fluid cows.
        ItemStack stack = new ItemStack(FluidCowsRegistries.FLUID_COW_SPAWN_EGG.get());
        Fluid fluid = getFluid();
        if (fluid != null) {
            FluidCowSpawnEggItem.applyCowData(stack, fluid, getCooldown());
        }
        return stack;
    }

    private boolean canMilk() {
        return !this.isBaby() && getCooldown() == 0;
    }

    /**
     * Exposes the fluid assignment so external systems (items, blocks) can rehydrate captured cows.
     *
     * <p>The legacy mod allowed the halter and displayer to assign arbitrary fluids to cows. Making
     * this method public keeps that behaviour while continuing to gate values through the registry
     * id lookup used by {@link SynchedEntityData}.</p>
     */
    public void setFluid(@Nullable final Fluid fluid) {
        int id = fluid == null ? -1 : BuiltInRegistries.FLUID.getId(fluid);
        this.entityData.set(DATA_FLUID, id);
    }

    /** Returns the currently assigned fluid, or {@code null} when the cow has not been configured. */
    @Nullable
    public Fluid getFluid() {
        int id = this.entityData.get(DATA_FLUID);
        if (id < 0) {
            return null;
        }
        Fluid fluid = BuiltInRegistries.FLUID.byId(id);
        return fluid == Fluids.EMPTY ? null : fluid;
    }

    /** Public so helpers can recompute the cooldown after they change the bound fluid. */
    public void refreshWorldCooldown() {
        Fluid fluid = getFluid();
        if (fluid == null) {
            setCooldown(0);
            return;
        }
        ResourceLocation key = BuiltInRegistries.FLUID.getKey(fluid);
        if (key == null) {
            setCooldown(0);
            return;
        }
        int worldCooldown = FCConfig.getWorldCD(key);
        if (worldCooldown == Integer.MAX_VALUE) {
            // The parsed config did not provide an entry for this fluid, so treat the default as
            // a free milk action instead of inheriting the legacy "never milk" sentinel value.
            worldCooldown = 0;
        }
        setCooldown(Math.max(0, worldCooldown));
    }

    /** Exposes the remaining world cooldown so integrations like WTHIT can mirror stall timings. */
    public int getMilkCooldown() {
        return getCooldown();
    }

    private void setCooldown(final int ticks) {
        this.entityData.set(DATA_COOLDOWN, Math.max(0, ticks));
    }

    private int getCooldown() {
        return this.entityData.get(DATA_COOLDOWN);
    }
}
