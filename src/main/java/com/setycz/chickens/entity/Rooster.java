package com.setycz.chickens.entity;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityType;
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
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.RegistryFriendlyByteBuf;
import com.setycz.chickens.registry.ModMenuTypes;
import com.setycz.chickens.menu.RoosterMenu;

/**
 * Lightweight NeoForge port of Hatchery's rooster entity. This class focuses on
 * two core behaviours from the original mod:
 * <ul>
 * <li>Roosters never lay eggs themselves; they are utility birds rather than
 * resource producers.</li>
 * <li>Roosters can store seeds internally and slowly convert them into an
 * abstract "seed charge" value that future AI and GUIs can consume.</li>
 * </ul>
 *
 * The more advanced mating AI and dedicated inventory GUI from Hatchery's
 * {@code EntityRooster} / {@code GuiRoosterInventory} have not been ported
 * yet. Those features will be layered on top of this skeleton once the base
 * entity is integrated and play‑tested inside ModernChickens.
 */
public class Rooster extends Chicken implements Container, MenuProvider {
    private static final EntityDataAccessor<Integer> DATA_SEEDS = SynchedEntityData.defineId(Rooster.class,
            EntityDataSerializers.INT);

    /** Single inventory slot used for storing wheat seeds and similar food. */
    private static final int SEED_SLOT = 0;
    /** Maximum number of "virtual" seeds that can be converted into charge. */
    private static final int MAX_SEEDS = 20;

    private static final String TAG_SEEDS = "Seeds";
    private static final String TAG_ITEMS = "Items";

    private final NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);

    public Rooster(EntityType<? extends Chicken> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // Track the converted seed charge so client GUIs and overlays can show a
        // simple progress bar, mirroring the legacy Hatchery rooster HUD.
        super.defineSynchedData(builder);
        builder.define(DATA_SEEDS, 0);
    }

    @Override
    protected void registerGoals() {
        // Start from the vanilla chicken goals but drop the generic BreedGoal so
        // this entity behaves as a utility rooster rather than a self‑breeding
        // chicken. Future work can reintroduce a dedicated mating goal that uses
        // the stored seed charge.
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.4D));
        this.goalSelector.addGoal(2, new TemptGoal(this, 1.0D, stack -> stack.is(ItemTags.CHICKEN_FOOD), false));
        this.goalSelector.addGoal(3, new FollowParentGoal(this, 1.1D));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
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
        // Every few ticks, convert stored seed items into abstract "seed charge"
        // so future mating logic has a simple integer resource to consume.
        if (this.tickCount % 5 == 0) {
            convertSeeds();
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        // Open the rooster inventory when the player interacts with an empty hand
        // or holds seeds, mirroring the legacy Hatchery behaviour.
        if (!level().isClientSide && hand == InteractionHand.MAIN_HAND
                && (held.isEmpty() || held.is(ItemTags.CHICKEN_FOOD))) {
            player.openMenu(this);
            return InteractionResult.CONSUME;
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
        if (!stack.is(ItemTags.CHICKEN_FOOD)) {
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

    @Override
    public Component getName() {
        Component custom = this.getCustomName();
        if (custom != null) {
            return custom;
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
        if (!stack.isEmpty() && stack.getCount() > getMaxStackSize(stack)) {
            stack.setCount(getMaxStackSize(stack));
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
            stack.save(registries, itemTag);
            list.add(itemTag);
        }
        tag.put(TAG_ITEMS, list);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setSeeds(tag.getInt(TAG_SEEDS));
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
                items.set(slot, ItemStack.parse(registries, itemTag).orElse(ItemStack.EMPTY));
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
