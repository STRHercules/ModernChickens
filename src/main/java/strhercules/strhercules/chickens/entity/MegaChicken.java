package strhercules.chickens.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.entity.Mob;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * The rare, rideable wild chicken. Horse inheritance supplies the native saddle,
 * chest inventory, owner persistence, passenger control, and horse inventory screen.
 */
public class MegaChicken extends AbstractChestedHorse {
    public float flap;
    public float flapSpeed;
    public float oFlapSpeed;
    public float oFlap;
    public float flapping = 1.0F;

    public MegaChicken(EntityType<? extends MegaChicken> type, Level level) {
        super(type, level);
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
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.CHICKEN_STEP, 0.3F, 1.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.JUMP_STRENGTH, 0.55D)
                .add(Attributes.STEP_HEIGHT, 1.0D)
                .add(Attributes.SAFE_FALL_DISTANCE, 6.0D)
                .add(Attributes.FALL_DAMAGE_MULTIPLIER, 0.5D);
    }

    public static boolean checkSpawnRules(EntityType<MegaChicken> type, LevelAccessor level,
                                           MobSpawnType reason, BlockPos pos, RandomSource random) {
        return random.nextInt(128) == 0 && Animal.checkAnimalSpawnRules(type, level, reason, pos, random);
    }
}
