package com.setycz.chickens.entity;

import com.setycz.chickens.ChickensRegistry;
import com.setycz.chickens.ChickensRegistryItem;
import com.setycz.chickens.SpawnType;
import com.setycz.chickens.blockentity.HenhouseBlockEntity;
import com.setycz.chickens.registry.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

import java.util.List;

/**
 * Modern NeoForge implementation of the chickens entity. The original mod
 * extended the vanilla chicken directly and added breeding/stat logic on top.
 * This class mirrors that behaviour but adapts it to the contemporary
 * SynchedEntityData API, component based text and loot utilities.
 */
public class ChickensChicken extends Chicken {
    private static final EntityDataAccessor<Integer> DATA_TYPE = SynchedEntityData.defineId(ChickensChicken.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_ANALYSED = SynchedEntityData.defineId(ChickensChicken.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_GROWTH = SynchedEntityData.defineId(ChickensChicken.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_GAIN = SynchedEntityData.defineId(ChickensChicken.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_STRENGTH = SynchedEntityData.defineId(ChickensChicken.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LAY_PROGRESS = SynchedEntityData.defineId(ChickensChicken.class, EntityDataSerializers.INT);

    private static final String TAG_TYPE = "Type";
    private static final String TAG_ANALYSED = "Analyzed";
    private static final String TAG_GROWTH = "Growth";
    private static final String TAG_GAIN = "Gain";
    private static final String TAG_STRENGTH = "Strength";

    private int layTime;

    public ChickensChicken(net.minecraft.world.entity.EntityType<? extends Chicken> type, Level level) {
        super(type, level);
        this.resetTimeUntilNextEgg();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // Mirror the legacy entity data parameters so that analyser GUIs and
        // renderers can access chicken stats just like the 1.10 release.
        super.defineSynchedData(builder);
        builder.define(DATA_TYPE, 0);
        builder.define(DATA_ANALYSED, Boolean.FALSE);
        builder.define(DATA_GROWTH, 1);
        builder.define(DATA_GAIN, 1);
        builder.define(DATA_STRENGTH, 1);
        builder.define(DATA_LAY_PROGRESS, 0);
    }

    public boolean getStatsAnalyzed() {
        return this.entityData.get(DATA_ANALYSED);
    }

    public void setStatsAnalyzed(boolean analysed) {
        this.entityData.set(DATA_ANALYSED, analysed);
    }

    public int getGain() {
        return this.entityData.get(DATA_GAIN);
    }

    private void setGain(int gain) {
        this.entityData.set(DATA_GAIN, gain);
    }

    public int getGrowth() {
        return this.entityData.get(DATA_GROWTH);
    }

    private void setGrowth(int growth) {
        this.entityData.set(DATA_GROWTH, growth);
    }

    public int getStrength() {
        return this.entityData.get(DATA_STRENGTH);
    }

    private void setStrength(int strength) {
        this.entityData.set(DATA_STRENGTH, strength);
    }

    public int getChickenType() {
        return this.entityData.get(DATA_TYPE);
    }

    public void setChickenType(int type) {
        this.entityData.set(DATA_TYPE, type);
        this.resetTimeUntilNextEgg();
    }

    public int getLayProgress() {
        return this.entityData.get(DATA_LAY_PROGRESS);
    }

    private void updateLayProgress() {
        this.entityData.set(DATA_LAY_PROGRESS, Math.max(this.layTime / 2400, 0));
    }

    private void setLayTime(int ticks) {
        this.layTime = ticks;
        this.updateLayProgress();
    }

    private void resetTimeUntilNextEgg() {
        ChickensRegistryItem description = this.getChickenDescription();
        if (description == null) {
            // If the registry is not populated yet we simply defer laying until
            // the first server tick updates the entity state.
            this.setLayTime(200);
            return;
        }
        int min = description.getMinLayTime();
        int max = description.getMaxLayTime();
        int base = min + this.random.nextInt(Math.max(max - min, 1));
        int adjusted = (int) Math.max(1.0f, (base * (10.0f - this.getGrowth() + 1.0f)) / 10.0f);
        this.setLayTime(adjusted * 2);
    }

    @Nullable
    private ChickensRegistryItem getChickenDescription() {
        return ChickensRegistry.getByType(this.getChickenType());
    }

    public int getTier() {
        ChickensRegistryItem description = this.getChickenDescription();
        return description != null ? description.getTier() : 1;
    }

    @Override
    public Component getName() {
        Component customName = this.getCustomName();
        if (customName != null) {
            return customName;
        }
        ChickensRegistryItem description = this.getChickenDescription();
        if (description == null) {
            return super.getName();
        }
        return description.getDisplayName();
    }

    @Override
    public void aiStep() {
        // Keep the vanilla egg timer out of range so only the custom resource
        // laying logic below produces drops.
        this.eggTime = Math.max(this.eggTime, 6000);
        super.aiStep();

        // Inline the vanilla wing animation updates so overriding this method lets us
        // replace the default egg drop without regressing movement visuals.
        this.oFlap = this.flap;
        this.oFlapSpeed = this.flapSpeed;
        this.flapSpeed += (this.onGround() ? -1.0F : 4.0F) * 0.3F;
        this.flapSpeed = Mth.clamp(this.flapSpeed, 0.0F, 1.0F);
        if (!this.onGround() && this.flapping < 1.0F) {
            this.flapping = 1.0F;
        }

        this.flapping *= 0.9F;
        Vec3 velocity = this.getDeltaMovement();
        if (!this.onGround() && velocity.y < 0.0D) {
            this.setDeltaMovement(velocity.multiply(1.0D, 0.6D, 1.0D));
        }

        this.flap += this.flapping * 2.0F;

        if (!this.level().isClientSide && this.isAlive() && !this.isBaby() && !this.isChickenJockey()) {
            this.tickResourceLaying();
        }
    }

    private void tickResourceLaying() {
        this.layTime--;
        // Keep the synced lay progress field in step with the ticking timer so
        // analysers and Jade overlays report the same countdown the legacy mod did.
        this.updateLayProgress();
        if (this.layTime > 0) {
            return;
        }

        ChickensRegistryItem description = this.getChickenDescription();
        if (description != null) {
            ItemStack toLay = description.createLayItem();
            // Deliver eggs to nearby henhouses before falling back to
            // item drops so long-running automation setups continue to work.
            this.spawnEggStack(toLay, description);
        }
        this.resetTimeUntilNextEgg();
    }

    private void spawnEggStack(ItemStack stack, ChickensRegistryItem description) {
        if (stack.isEmpty()) {
            return;
        }
        depositOrDrop(stack);
        int gain = this.getGain();
        if (gain >= 5) {
            depositOrDrop(description.createLayItem());
        }
        if (gain >= 10) {
            depositOrDrop(description.createLayItem());
        }
        this.playSound(SoundEvents.CHICKEN_EGG, 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
        this.gameEvent(GameEvent.ENTITY_PLACE, this);
    }

    private void depositOrDrop(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        // Try to offload the item stack into any henhouse before spawning it
        // directly so farms that depend on automation remain intact.
        ItemStack leftover = HenhouseBlockEntity.pushItemStack(stack.copy(), this.level(), this.position());
        if (!leftover.isEmpty()) {
            this.spawnAtLocation(leftover, 0.0F);
        }
    }

    @Nullable
    @Override
    public ChickensChicken getBreedOffspring(ServerLevel level, AgeableMob partner) {
        if (!(partner instanceof ChickensChicken mate)) {
            return null;
        }
        ChickensRegistryItem description = this.getChickenDescription();
        ChickensRegistryItem mateDescription = mate.getChickenDescription();
        if (description == null || mateDescription == null) {
            return null;
        }
        ChickensRegistryItem childDescription = ChickensRegistry.getRandomChild(description, mateDescription);
        if (childDescription == null) {
            return null;
        }
        ChickensChicken child = ModEntityTypes.CHICKENS_CHICKEN.get().create(level);
        if (child == null) {
            return null;
        }
        child.setChickenType(childDescription.getId());
        boolean mutating = description.getId() == mateDescription.getId() && childDescription.getId() == description.getId();
        if (mutating) {
            increaseStats(child, this, mate, this.random);
        } else if (description.getId() == childDescription.getId()) {
            inheritStats(child, this);
        } else if (mateDescription.getId() == childDescription.getId()) {
            inheritStats(child, mate);
        }
        return child;
    }

    private static void inheritStats(ChickensChicken child, ChickensChicken parent) {
        child.setGrowth(parent.getGrowth());
        child.setGain(parent.getGain());
        child.setStrength(parent.getStrength());
    }

    private static void increaseStats(ChickensChicken child, ChickensChicken parent1, ChickensChicken parent2, RandomSource random) {
        int strength1 = parent1.getStrength();
        int strength2 = parent2.getStrength();
        child.setGrowth(calculateNewStat(strength1, strength2, parent1.getGrowth(), parent2.getGrowth(), random));
        child.setGain(calculateNewStat(strength1, strength2, parent1.getGain(), parent2.getGain(), random));
        child.setStrength(calculateNewStat(strength1, strength2, strength1, strength2, random));
    }

    private static int calculateNewStat(int strength1, int strength2, int stat1, int stat2, RandomSource random) {
        int mutation = random.nextInt(2) + 1;
        int weighted = (stat1 * strength1 + stat2 * strength2) / Math.max(strength1 + strength2, 1);
        int value = weighted + mutation;
        if (value <= 1) {
            return 1;
        }
        if (value >= 10) {
            return 10;
        }
        return value;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 20 * 60;
    }

    @Override
    public boolean fireImmune() {
        ChickensRegistryItem description = this.getChickenDescription();
        return description != null && description.isImmuneToFire() || super.fireImmune();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        if (this.random.nextFloat() > 0.1F) {
            return;
        }
        super.playStepSound(pos, state);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, net.minecraft.world.damagesource.DamageSource source, boolean recentlyHit) {
        ChickensRegistryItem description = this.getChickenDescription();
        if (description != null) {
            ItemStack drop = description.createDropItem();
            drop.setCount(1 + this.random.nextInt(1 + this.getLooting(level, source)));
            this.spawnAtLocation(drop, 0.0F);
        }
        if (this.isOnFire()) {
            this.spawnAtLocation(new ItemStack(net.minecraft.world.item.Items.COOKED_CHICKEN), 0.0F);
        } else {
            this.spawnAtLocation(new ItemStack(net.minecraft.world.item.Items.CHICKEN), 0.0F);
        }
        super.dropCustomDeathLoot(level, source, recentlyHit);
    }

    private int getLooting(ServerLevel level, net.minecraft.world.damagesource.DamageSource source) {
        if (source.getEntity() instanceof Player player) {
            return level.registryAccess().registry(Registries.ENCHANTMENT)
                    .flatMap(registry -> registry.getHolder(Enchantments.LOOTING))
                    .map(player.getMainHandItem()::getEnchantmentLevel)
                    .orElse(0);
        }
        return 0;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(TAG_TYPE, this.getChickenType());
        tag.putBoolean(TAG_ANALYSED, this.getStatsAnalyzed());
        tag.putInt(TAG_GROWTH, this.getGrowth());
        tag.putInt(TAG_GAIN, this.getGain());
        tag.putInt(TAG_STRENGTH, this.getStrength());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setChickenType(tag.getInt(TAG_TYPE));
        this.setStatsAnalyzed(tag.getBoolean(TAG_ANALYSED));
        this.setGrowth(getStatusValue(tag, TAG_GROWTH));
        this.setGain(getStatusValue(tag, TAG_GAIN));
        this.setStrength(getStatusValue(tag, TAG_STRENGTH));
        this.updateLayProgress();
    }

    private static int getStatusValue(CompoundTag tag, String key) {
        return tag.contains(key) ? tag.getInt(key) : 1;
    }

    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevel level, DifficultyInstance difficulty, MobSpawnType spawnType,
            @Nullable SpawnGroupData spawnData) {
        spawnData = super.finalizeSpawn(level, difficulty, spawnType, spawnData);
        if (spawnData instanceof GroupData groupData) {
            this.setChickenType(groupData.type);
        } else {
            SpawnType biomeSpawnType = ChickensRegistry.getSpawnType(level.getBiome(this.blockPosition()));
            List<ChickensRegistryItem> options = ChickensRegistry.getPossibleChickensToSpawn(biomeSpawnType);
            if (!options.isEmpty()) {
                ChickensRegistryItem selected = options.get(this.random.nextInt(options.size()));
                int type = selected.getId();
                this.setChickenType(type);
                spawnData = new GroupData(type);
            }
        }
        if (this.random.nextInt(5) == 0) {
            this.setAge(-24000);
        }
        return spawnData;
    }

    @Override
    public void setAge(int age) {
        int childAge = -24000;
        if (age == childAge) {
            age = Math.min(-1, (childAge * (10 - this.getGrowth() + 1)) / 10);
        }
        int loveAge = 6000;
        if (age == loveAge) {
            age = Math.max(1, (loveAge * (10 - this.getGrowth() + 1)) / 10);
        }
        super.setAge(age);
    }

    public static AttributeSupplier.Builder createAttributes() {
        // Vanilla already configures sensible defaults; we simply forward the
        // builder so callers have a convenient entry point.
        return Chicken.createAttributes();
    }

    public static boolean checkSpawnRules(net.minecraft.world.entity.EntityType<ChickensChicken> type, LevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        boolean netherEnabled = ChickensRegistry.isAnyIn(SpawnType.HELL);
        boolean overworldEnabled = ChickensRegistry.isAnyIn(SpawnType.NORMAL) || ChickensRegistry.isAnyIn(SpawnType.SNOW);
        Holder<Biome> biome = level.getBiome(pos);
        boolean isNether = ChickensRegistry.getSpawnType(biome) == SpawnType.HELL;
        if (isNether && netherEnabled) {
            return net.minecraft.world.entity.animal.Animal.checkAnimalSpawnRules(type, level, reason, pos, random);
        }
        if (!isNether && overworldEnabled) {
            return net.minecraft.world.entity.animal.Animal.checkAnimalSpawnRules(type, level, reason, pos, random);
        }
        return false;
    }

    private static final class GroupData implements SpawnGroupData {
        private final int type;

        private GroupData(int type) {
            this.type = type;
        }
    }
}
