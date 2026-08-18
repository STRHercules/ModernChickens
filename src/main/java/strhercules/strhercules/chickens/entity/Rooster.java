package strhercules.chickens.entity;

import strhercules.chickens.ChickenFood;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.FriendlyByteBuf;
import strhercules.chickens.registry.ModMenuTypes;
import strhercules.chickens.menu.RoosterMenu;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;

/**
 * Lightweight Forge port of Hatchery's rooster entity. This class focuses on
 * two core behaviours from the original mod:
 * <ul>
 * <li>Roosters never lay eggs themselves; they are utility birds rather than
 * resource producers.</li>
 * <li>Roosters can store seeds internally and slowly convert them into an
 * abstract "seed charge" value that future AI and GUIs can consume.</li>
 * </ul>
 *
 * Roosters use their stored seed charge to fertilize nearby hens; the hen
 * remains the sole source of the resulting chick's type and genetics.
 */
public class Rooster extends Chicken implements Container, MenuProvider {
    private static final EntityDataAccessor<Integer> DATA_SEEDS = SynchedEntityData.defineId(Rooster.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_ROBOT_ROOSTER = SynchedEntityData.defineId(
            Rooster.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_VIRUS_TICKS = SynchedEntityData.defineId(
            Rooster.class, EntityDataSerializers.INT);

    /** Single inventory slot used for storing wheat seeds and similar food. */
    private static final int SEED_SLOT = 0;
    /** Maximum number of "virtual" seeds that can be converted into charge. */
    private static final int MAX_SEEDS = 20;

    private static final String TAG_SEEDS = "Seeds";
    private static final String TAG_ITEMS = "Items";
    private static final String TAG_ROBOT_ROOSTER = "RobotRooster";
    private static final String TAG_VIRUS_TICKS = "VirusTicks";

    public static final int ROBOT_VIRUS_DURATION_TICKS = 2400;

    private final NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);

    public Rooster(EntityType<? extends Chicken> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        // Track the converted seed charge so client GUIs and overlays can show a
        // simple progress bar, mirroring the legacy Hatchery rooster HUD.
        super.defineSynchedData();
        this.entityData.define(DATA_SEEDS, 0);
        this.entityData.define(DATA_ROBOT_ROOSTER, false);
        this.entityData.define(DATA_VIRUS_TICKS, 0);
    }

    @Override
    protected void registerGoals() {
        // Start from the vanilla chicken goals but drop the generic BreedGoal so
        // this entity behaves as a utility rooster rather than a self‑breeding
        // chicken. Future work can reintroduce a dedicated mating goal that uses
        // the stored seed charge.
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.4D));
        this.goalSelector.addGoal(2, new RoosterMateGoal(this));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.0D, ChickenFood.INGREDIENT, false));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.1D));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public void aiStep() {
        // Keep the vanilla egg timer permanently above the lay threshold so this
        // entity never produces eggs on its own. This mirrors the behaviour of
        // Hatchery's rooster, which only facilitated breeding rather than acting
        // as a resource chicken.
        this.eggTime = Math.max(this.eggTime, 6000);
        super.aiStep();
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        // Every few ticks, convert stored seed items into the charge consumed
        // by the mating goal.
        if (this.tickCount % 5 == 0) {
            convertSeeds();
        }
        tickRobotVirus();
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        // Open the rooster inventory when the player interacts with an empty hand
        // or holds seeds, mirroring the legacy Hatchery behaviour.
        if (hand == InteractionHand.MAIN_HAND && (held.isEmpty() || ChickenFood.test(held))) {
            if (!level().isClientSide) {
                net.minecraftforge.network.NetworkHooks.openScreen((net.minecraft.server.level.ServerPlayer) player, this, buffer -> buffer.writeVarInt(this.getId()));
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    /**
     * Returns the current converted seed charge. Each two physical seeds in the
     * internal inventory contribute two points of charge when converted.
     */
    public int getSeeds() {
        return this.entityData.get(DATA_SEEDS);
    }

    /**
     * Updates the converted seed charge, clamping the value to the supported
     * range so NBT edits or future GUIs cannot overflow the counter.
     */
    public void setSeeds(int value) {
        this.entityData.set(DATA_SEEDS, Mth.clamp(value, 0, MAX_SEEDS));
    }

    public boolean isRobotRooster() {
        return this.entityData.get(DATA_ROBOT_ROOSTER);
    }

    public void setRobotRooster(boolean robotRooster) {
        this.entityData.set(DATA_ROBOT_ROOSTER, robotRooster);
        if (robotRooster) {
            this.entityData.set(DATA_VIRUS_TICKS, 0);
        }
    }

    public int getVirusTicksRemaining() {
        return this.entityData.get(DATA_VIRUS_TICKS);
    }

    public void setVirusTicksRemaining(int ticks) {
        this.entityData.set(DATA_VIRUS_TICKS, this.isRobotRooster()
                ? 0 : Mth.clamp(ticks, 0, ROBOT_VIRUS_DURATION_TICKS));
    }

    public boolean startRobotVirus() {
        if (this.isRobotRooster() || this.getVirusTicksRemaining() > 0
                || !(this.level() instanceof ServerLevel serverLevel)
                || serverLevel.dimension() != Level.OVERWORLD) {
            return false;
        }
        this.setVirusTicksRemaining(ROBOT_VIRUS_DURATION_TICKS);
        return true;
    }

    private void tickRobotVirus() {
        int remaining = this.getVirusTicksRemaining();
        if (remaining <= 0 || this.isRobotRooster()) {
            return;
        }
        if (this.tickCount % 10 == 0 && this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 0.6D,
                    this.getZ(), 4, 0.25D, 0.35D, 0.25D, 0.03D);
        }
        remaining--;
        if (remaining <= 0) {
            completeRobotVirus();
        } else {
            this.setVirusTicksRemaining(remaining);
        }
    }

    private void completeRobotVirus() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        this.setRobotRooster(true);
        serverLevel.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 0.65D, this.getZ(),
                28, 0.35D, 0.5D, 0.35D, 0.08D);
        serverLevel.sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + 0.65D, this.getZ(),
                20, 0.35D, 0.45D, 0.35D, 0.06D);
        serverLevel.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 0.9D, this.getZ(),
                18, 0.3D, 0.5D, 0.3D, 0.08D);
        serverLevel.playSound(null, this.blockPosition(), SoundEvents.CHICKEN_EGG, SoundSource.NEUTRAL,
                0.8F, 0.55F);
    }

    /**
     * Scales the current seed charge into an arbitrary GUI bar height.
     */
    public int getScaledSeeds(int scale) {
        int seeds = getSeeds();
        return seeds == 0 ? 0 : seeds * scale / MAX_SEEDS;
    }

    private boolean hasConvertibleSeeds() {
        ItemStack stack = items.get(SEED_SLOT);
        if (stack.isEmpty()) {
            return false;
        }
        if (!ChickenFood.test(stack)) {
            return false;
        }
        if (stack.getCount() < 2) {
            return false;
        }
        return getSeeds() <= MAX_SEEDS - 2;
    }

    /**
     * Converts two stored seeds into two points of abstract seed charge. This is
     * intentionally lightweight to keep ticking overhead small when many roosters
     * exist in a farm.
     */
    private void convertSeeds() {
        if (!hasConvertibleSeeds()) {
            return;
        }
        ItemStack stack = items.get(SEED_SLOT);
        stack.shrink(2);
        if (stack.isEmpty()) {
            items.set(SEED_SLOT, ItemStack.EMPTY);
        }
        setSeeds(getSeeds() + 2);
    }

    private void consumeSeeds(int amount) {
        setSeeds(getSeeds() - amount);
    }

    public static boolean checkSpawnRules(EntityType<Rooster> type, LevelAccessor level, MobSpawnType reason,
            BlockPos pos, RandomSource random) {
        return Animal.checkAnimalSpawnRules(type, level, reason, pos, random);
    }

    private static final class RoosterMateGoal extends Goal {
        private static final int SEED_COST = 2;
        private static final double SEARCH_RANGE = 8.0D;

        private final Rooster rooster;
        @Nullable
        private Chicken hen;
        private boolean startedLove;
        private int mateTime;

        private RoosterMateGoal(Rooster rooster) {
            this.rooster = rooster;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (rooster.isBaby() || rooster.isRobotRooster() || rooster.getVirusTicksRemaining() > 0
                    || rooster.getSeeds() < SEED_COST) {
                return false;
            }
            hen = findHen();
            return hen != null;
        }

        @Override
        public void start() {
            mateTime = 0;
            startedLove = hen != null && !hen.isInLove();
            if (startedLove) {
                hen.setInLove(null);
            }
        }

        @Override
        public boolean canContinueToUse() {
            return hen != null && rooster.getSeeds() >= SEED_COST && hen.isAlive() && !hen.isBaby()
                    && hen.isInLove() && mateTime < 60;
        }

        @Override
        public void tick() {
            rooster.getLookControl().setLookAt(hen, 10.0F, rooster.getMaxHeadXRot());
            rooster.getNavigation().moveTo(hen, 1.0D);
            if (++mateTime >= this.adjustedTickDelay(60) && rooster.distanceToSqr(hen) < 9.0D) {
                breed();
            }
        }

        @Override
        public void stop() {
            if (startedLove && hen != null) {
                hen.resetLove();
            }
            hen = null;
            startedLove = false;
            mateTime = 0;
        }

        @Nullable
        private Chicken findHen() {
            List<Chicken> chickens = rooster.level().getEntitiesOfClass(Chicken.class,
                    rooster.getBoundingBox().inflate(SEARCH_RANGE), candidate -> candidate != rooster
                            && !(candidate instanceof Rooster) && !candidate.isBaby()
                            && (candidate.isInLove() || candidate.canFallInLove()));
            return chickens.stream().min(java.util.Comparator.comparingDouble(rooster::distanceToSqr)).orElse(null);
        }

        private void breed() {
            if (!(rooster.level() instanceof ServerLevel level) || hen == null) {
                return;
            }
            if (hen instanceof ChickensChicken robotChicken && robotChicken.isRobotChicken()) {
                if (level.dimension() == Level.OVERWORLD && rooster.startRobotVirus()) {
                    rooster.consumeSeeds(SEED_COST);
                }
                return;
            }
            AgeableMob child = hen.getBreedOffspring(level, rooster);
            if (child == null) {
                return;
            }
            child.setBaby(true);
            child.moveTo(hen.getX(), hen.getY(), hen.getZ(), 0.0F, 0.0F);
            hen.finalizeSpawnChildFromBreeding(level, rooster, child);
            level.addFreshEntityWithPassengers(child);
            rooster.consumeSeeds(SEED_COST);
        }
    }

    @Override
    public Component getName() {
        Component custom = this.getCustomName();
        if (custom != null) {
            return custom;
        }
        if (this.isRobotRooster()) {
            return Component.translatable("entity.chickens.robot_rooster");
        }
        // Use a dedicated translation key so resource packs can localise roosters
        // independently of vanilla chickens.
        return Component.translatable("entity.chickens.rooster");
    }

    /**
     * Base attribute set for roosters. This mirrors the original Hatchery
     * configuration: slightly faster than a vanilla chicken with a small melee
     * attack so they feel a little more assertive when protecting flocks.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 8.0D);
    }

    // ---------------------------------------------------------------------
    // Container implementation – mirrors the simple one-slot inventory from
    // Hatchery's rooster so future GUIs and menus have a stable API surface.
    // ---------------------------------------------------------------------

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int index) {
        if (index < 0 || index >= items.size()) {
            return ItemStack.EMPTY;
        }
        return items.get(index);
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        if (index < 0 || index >= items.size() || count <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = items.get(index);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack result;
        if (stack.getCount() <= count) {
            result = stack;
            items.set(index, ItemStack.EMPTY);
        } else {
            result = stack.split(count);
        }
        if (!result.isEmpty()) {
            setChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        if (index < 0 || index >= items.size()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = items.get(index);
        items.set(index, ItemStack.EMPTY);
        if (!stack.isEmpty()) {
            setChanged();
        }
        return stack;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        if (index < 0 || index >= items.size()) {
            return;
        }
        items.set(index, stack);
        if (!stack.isEmpty() && stack.getCount() > Math.min(stack.getMaxStackSize(), getMaxStackSize())) {
            stack.setCount(Math.min(stack.getMaxStackSize(), getMaxStackSize()));
        }
        setChanged();
    }

    @Override
    public void setChanged() {
        // No-op for now. This hook exists so future GUI or networking code can
        // detect inventory changes without re-scanning the container every tick.
    }

    @Override
    public boolean stillValid(Player player) {
        // Reuse the standard distance check used by most container entities so
        // interaction feels consistent with vanilla minecarts and animals.
        return this.isAlive() && player.distanceToSqr(this) <= 64.0D;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < items.size(); i++) {
            items.set(i, ItemStack.EMPTY);
        }
        setChanged();
    }

    // ---------------------------------------------------------------------
    // Persistence – mirrors the legacy rooster's seed/inventory NBT layout
    // so future tools can read/write save data consistently.
    // ---------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(TAG_SEEDS, getSeeds());
        tag.putBoolean(TAG_ROBOT_ROOSTER, isRobotRooster());
        tag.putInt(TAG_VIRUS_TICKS, getVirusTicksRemaining());
        ListTag list = new ListTag();
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            CompoundTag itemTag = new CompoundTag();
            itemTag.putByte("Slot", (byte) i);
            // Persist the rooster's single-slot inventory using the modern
            // HolderLookup-aware ItemStack encoder so registry lookups stay
            // consistent with block entity containers.
            HolderLookup.Provider registries = level().registryAccess();
            stack.save(itemTag);
            list.add(itemTag);
        }
        tag.put(TAG_ITEMS, list);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setSeeds(tag.getInt(TAG_SEEDS));
        setRobotRooster(tag.getBoolean(TAG_ROBOT_ROOSTER));
        setVirusTicksRemaining(tag.getInt(TAG_VIRUS_TICKS));
        // Rebuild the single-slot inventory so saved seed stacks survive
        // across world reloads instead of being dropped due to a cleared list.
        for (int i = 0; i < items.size(); i++) {
            items.set(i, ItemStack.EMPTY);
        }
        ListTag list = tag.getList(TAG_ITEMS, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag itemTag = list.getCompound(i);
            int slot = itemTag.getByte("Slot") & 255;
            if (slot >= 0 && slot < items.size()) {
                HolderLookup.Provider registries = level().registryAccess();
                items.set(slot, ItemStack.of(itemTag));
            }
        }
    }

    // ---------------------------------------------------------------------
    // Menu provider – exposes a simple one-slot container menu so clients can
    // open a GUI when interacting with the rooster.
    // ---------------------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        // Use a lightweight menu that mirrors the original layout: a single seed
        // slot plus the player inventory. Seed progress is read from entity data.
        return new RoosterMenu(id, playerInventory, this);
    }
}
