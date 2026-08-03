package strhercules.chickens.entity;

import strhercules.chickens.ChickensRegistry;
import strhercules.chickens.ChickensRegistryItem;
import strhercules.chickens.item.ChickenItemHelper;
import strhercules.chickens.item.ChickenStats;
import strhercules.chickens.item.MegaChickenItem;
import strhercules.chickens.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import strhercules.chickens.menu.MegaChickenMenu;

import javax.annotation.Nullable;

/**
 * The rare, rideable wild chicken. Horse inheritance supplies the native saddle,
 * cargo inventory, owner persistence, and passenger control.
 */
public class MegaChicken extends AbstractChestedHorse implements MenuProvider {
    private static final EntityDataAccessor<Boolean> DATA_RIGHT_CHEST = SynchedEntityData.defineId(
            MegaChicken.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_LEFT_CHEST = SynchedEntityData.defineId(
            MegaChicken.class, EntityDataSerializers.BOOLEAN);

    public static final int RIGHT_CHEST_SLOT = 0;
    public static final int LEFT_CHEST_SLOT = 1;
    private static final String TAG_RIGHT_CHEST = "RightChest";
    private static final String TAG_LEFT_CHEST = "LeftChest";
    private static final String TAG_CHEST_EQUIPMENT_DATA = "MegaChickenChestEquipment";
    private static final String TAG_APPEARANCE_TYPE = "MegaChickenAppearanceType";
    private static final int DEFAULT_APPEARANCE_TYPE = -1;
    private static final EntityDataAccessor<Integer> DATA_APPEARANCE_TYPE = SynchedEntityData.defineId(
            MegaChicken.class, EntityDataSerializers.INT);

    private final SimpleContainer chestEquipment = new SimpleContainer(2);

    public float flap;
    public float flapSpeed;
    public float oFlapSpeed;
    public float oFlap;
    public float flapping = 1.0F;

    public MegaChicken(EntityType<? extends MegaChicken> type, Level level) {
        super(type, level);
        this.chestEquipment.addListener(container -> this.syncChestFlags());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_RIGHT_CHEST, false);
        builder.define(DATA_LEFT_CHEST, false);
        builder.define(DATA_APPEARANCE_TYPE, DEFAULT_APPEARANCE_TYPE);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.4D));
        this.goalSelector.addGoal(2, new TemptGoal(this, 1.0D, stack -> stack.is(ItemTags.CHICKEN_FOOD), false));
        this.goalSelector.addGoal(3, new FollowParentGoal(this, 1.1D));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ItemTags.CHICKEN_FOOD);
    }

    @Override
    protected boolean handleEating(Player player, ItemStack stack) {
        if (!this.isFood(stack)) {
            return false;
        }
        if (!this.isTamed() && !this.level().isClientSide) {
            this.tameWithName(player);
        } else if (this.isTamed() && !this.level().isClientSide && this.getHealth() < this.getMaxHealth()) {
            this.heal(1.0F);
        }
        return true;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        ChickensRegistryItem appearance = getAppearanceChicken(held);
        if (this.isTamed() && player.getUUID().equals(this.getOwnerUUID()) && appearance != null) {
            if (!this.level().isClientSide) {
                this.setAppearanceType(appearance.getId());
                held.consume(1, player);
                this.spawnAppearanceParticles();
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        if (!this.isVehicle() && this.isTamed() && !this.isBaby() && held.is(Items.CHEST)) {
            int slot = !this.hasRightChest() ? RIGHT_CHEST_SLOT
                    : !this.hasLeftChest() ? LEFT_CHEST_SLOT : -1;
            if (slot >= 0) {
                this.chestEquipment.setItem(slot, new ItemStack(Items.CHEST));
                held.consume(1, player);
                this.playChestEquipsSound();
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
        }
        return super.mobInteract(player, hand);
    }

    private static ChickensRegistryItem getAppearanceChicken(ItemStack stack) {
        if (!ChickenItemHelper.isChicken(stack) || ChickenItemHelper.isRooster(stack)) {
            return null;
        }
        ChickenStats stats = ChickenItemHelper.getStats(stack);
        if (stats.growth() != 10 || stats.gain() != 10 || stats.strength() != 10) {
            return null;
        }
        return ChickenItemHelper.resolve(stack);
    }

    public int getAppearanceType() {
        return this.entityData.get(DATA_APPEARANCE_TYPE);
    }

    public void setAppearanceType(int type) {
        this.entityData.set(DATA_APPEARANCE_TYPE, type);
    }

    private void spawnAppearanceParticles() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        serverLevel.sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + 1.0D, this.getZ(),
                24, this.getBbWidth() * 0.6D, this.getBbHeight() * 0.35D, this.getBbWidth() * 0.6D, 0.08D);
    }

    public boolean hasRightChest() {
        return this.level().isClientSide
                ? this.entityData.get(DATA_RIGHT_CHEST)
                : this.chestEquipment.getItem(RIGHT_CHEST_SLOT).is(Items.CHEST);
    }

    public boolean hasLeftChest() {
        return this.level().isClientSide
                ? this.entityData.get(DATA_LEFT_CHEST)
                : this.chestEquipment.getItem(LEFT_CHEST_SLOT).is(Items.CHEST);
    }

    public SimpleContainer getChestEquipment() {
        return this.chestEquipment;
    }

    private void syncChestFlags() {
        if (this.level().isClientSide) {
            return;
        }
        boolean right = this.chestEquipment.getItem(RIGHT_CHEST_SLOT).is(Items.CHEST);
        boolean left = this.chestEquipment.getItem(LEFT_CHEST_SLOT).is(Items.CHEST);
        this.entityData.set(DATA_RIGHT_CHEST, right);
        this.entityData.set(DATA_LEFT_CHEST, left);

        boolean hasStoredItems = right || left;
        for (int i = 1; !hasStoredItems && i < this.inventory.getContainerSize(); i++) {
            hasStoredItems = !this.inventory.getItem(i).isEmpty();
        }
        this.setChest(hasStoredItems);
    }

    @Override
    public void containerChanged(Container container) {
        super.containerChanged(container);
        if (container == this.inventory) {
            this.syncChestFlags();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        this.syncChestFlags();
        super.addAdditionalSaveData(compound);
        compound.putInt(TAG_APPEARANCE_TYPE, this.getAppearanceType());
        compound.putBoolean(TAG_CHEST_EQUIPMENT_DATA, true);
        ItemStack right = this.chestEquipment.getItem(RIGHT_CHEST_SLOT);
        ItemStack left = this.chestEquipment.getItem(LEFT_CHEST_SLOT);
        if (!right.isEmpty()) {
            compound.put(TAG_RIGHT_CHEST, right.save(this.registryAccess()));
        }
        if (!left.isEmpty()) {
            compound.put(TAG_LEFT_CHEST, left.save(this.registryAccess()));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.setAppearanceType(compound.contains(TAG_APPEARANCE_TYPE, Tag.TAG_INT)
                ? compound.getInt(TAG_APPEARANCE_TYPE) : DEFAULT_APPEARANCE_TYPE);
        boolean wasChested = this.hasChest();
        boolean hasEquipmentData = compound.getBoolean(TAG_CHEST_EQUIPMENT_DATA);
        this.chestEquipment.clearContent();
        boolean loadedChest = false;
        if (compound.contains(TAG_RIGHT_CHEST, Tag.TAG_COMPOUND)) {
            ItemStack stack = ItemStack.parse(this.registryAccess(), compound.getCompound(TAG_RIGHT_CHEST))
                    .orElse(ItemStack.EMPTY);
            if (stack.is(Items.CHEST)) {
                this.chestEquipment.setItem(RIGHT_CHEST_SLOT, new ItemStack(Items.CHEST));
                loadedChest = true;
            }
        }
        if (compound.contains(TAG_LEFT_CHEST, Tag.TAG_COMPOUND)) {
            ItemStack stack = ItemStack.parse(this.registryAccess(), compound.getCompound(TAG_LEFT_CHEST))
                    .orElse(ItemStack.EMPTY);
            if (stack.is(Items.CHEST)) {
                this.chestEquipment.setItem(LEFT_CHEST_SLOT, new ItemStack(Items.CHEST));
                loadedChest = true;
            }
        }
        if (wasChested && !loadedChest && !hasEquipmentData) {
            // Migrate the old single-chest horse state to the right visual slot.
            this.chestEquipment.setItem(RIGHT_CHEST_SLOT, new ItemStack(Items.CHEST));
        }
        this.syncChestFlags();
    }

    @Override
    protected void dropEquipment() {
        for (int i = 0; i < this.inventory.getContainerSize(); i++) {
            this.dropEquipmentStack(this.inventory.getItem(i));
        }
        this.dropEquipmentStack(this.chestEquipment.getItem(RIGHT_CHEST_SLOT));
        this.dropEquipmentStack(this.chestEquipment.getItem(LEFT_CHEST_SLOT));
    }

    private void dropEquipmentStack(ItemStack stack) {
        if (!stack.isEmpty() && !EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP)) {
            this.spawnAtLocation(stack);
        }
    }

    @Override
    public void openCustomInventoryScreen(Player player) {
        if (!this.level().isClientSide && (!this.isVehicle() || this.hasPassenger(player)) && this.isTamed()) {
            player.openMenu(this, buffer -> buffer.writeVarInt(this.getId()));
        }
    }

    @Override
    public Component getDisplayName() {
        return this.getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new MegaChickenMenu(id, playerInventory, this);
    }

    @Override
    public void aiStep() {
        super.aiStep();

        // Keep the same flap values and downward velocity reduction as vanilla chickens.
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
    }

    @Override
    public boolean canMate(Animal other) {
        return false;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    @Override
    public Component getName() {
        Component customName = this.getCustomName();
        return customName != null ? customName : Component.translatable("entity.chickens.mega_chicken");
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        // The 2.8-block collision height is taller than the model's saddle.
        return super.getPassengerAttachmentPoint(passenger, dimensions, scale).add(0.0D, -0.5D, 0.0D);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.CHICKEN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
        return SoundEvents.CHICKEN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.CHICKEN_DEATH;
    }

    @Override
    protected SoundEvent getEatingSound() {
        return SoundEvents.CHICKEN_AMBIENT;
    }

    @Override
    protected SoundEvent getAngrySound() {
        return SoundEvents.CHICKEN_HURT;
    }

    @Override
    protected void dropAllDeathLoot(ServerLevel level, DamageSource source) {
        level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(level, this.getX(), this.getY() + 0.2D,
                this.getZ(), MegaChickenItem.createFromDeath(this, ModRegistry.MEGA_CHICKEN_ITEM.get())));
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.CHICKEN_STEP, 0.3F, 1.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseChestedHorseAttributes();
    }

    @Override
    protected void randomizeAttributes(RandomSource random) {
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(generateMaxHealth(random::nextInt));
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(generateSpeed(random::nextDouble));
        this.getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(generateJumpStrength(random::nextDouble));
    }

    public static boolean checkSpawnRules(EntityType<MegaChicken> type, LevelAccessor level,
                                           MobSpawnType reason, BlockPos pos, RandomSource random) {
        return random.nextInt(128) == 0 && Animal.checkAnimalSpawnRules(type, level, reason, pos, random);
    }

    @Override
    public int getInventoryColumns() {
        // AbstractHorse allocates three rows per column; the custom screen renders
        // those 54 cargo slots as a 9x6 grid.
        return 18;
    }
}
