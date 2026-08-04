package strhercules.chickens.item;

import strhercules.chickens.ChickensMod;
import strhercules.chickens.entity.MegaChicken;
import strhercules.chickens.registry.ModEntityTypes;
import strhercules.chickens.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import javax.annotation.Nullable;
import java.util.List;

/** Portable storage for a Mega Chicken, including its complete entity NBT. */
@EventBusSubscriber(modid = ChickensMod.MOD_ID)
public class MegaChickenItem extends Item {
    private static final String ENTITY_TYPE_TAG = "id";
    private static final String REVIVAL_TAG = "RequiresActivation";
    private static final String ACTIVATED_TAG = "Activated";

    public MegaChickenItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static ItemStack createFromEntity(MegaChicken chicken, Item storedItem) {
        ItemStack stack = new ItemStack(storedItem);
        storeEntityData(stack, chicken);
        return stack;
    }

    public static ItemStack createFromDeath(MegaChicken chicken, Item storedItem) {
        chicken.removeAllEffects();
        ItemStack stack = createFromEntity(chicken, storedItem);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putBoolean(REVIVAL_TAG, true);
            tag.putBoolean(ACTIVATED_TAG, false);
        });
        stack.set(DataComponents.FIRE_RESISTANT, Unit.INSTANCE);
        return stack;
    }

    public static boolean requiresActivation(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getBoolean(REVIVAL_TAG);
    }

    public static boolean isActivated(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getBoolean(ACTIVATED_TAG);
    }

    public static void setActivated(ItemStack stack, boolean activated) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean(ACTIVATED_TAG, activated));
        if (activated) {
            stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        } else {
            stack.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
        }
    }

    private static void storeEntityData(ItemStack stack, MegaChicken chicken) {
        CompoundTag entityData = new CompoundTag();
        chicken.saveWithoutId(entityData);
        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(chicken.getType());
        entityData.putString(ENTITY_TYPE_TAG, typeId.toString());
        entityData.remove("Pos");
        entityData.remove("Motion");
        entityData.remove("Rotation");
        entityData.remove("DeathTime");
        entityData.remove("HurtTime");
        entityData.remove("HurtByTimestamp");
        CustomData.set(DataComponents.ENTITY_DATA, stack, entityData);
    }

    private static boolean hasEntityData(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.ENTITY_DATA, CustomData.EMPTY);
        return !data.isEmpty() && data.copyTag().contains(ENTITY_TYPE_TAG);
    }

    @Nullable
    private static ResourceLocation readEntityType(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.ENTITY_DATA, CustomData.EMPTY);
        return data.isEmpty() ? null : ResourceLocation.tryParse(data.copyTag().getString(ENTITY_TYPE_TAG));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        if (requiresActivation(stack) && !isActivated(stack)) {
            return tryActivate(stack, player) ? InteractionResult.CONSUME : InteractionResult.FAIL;
        }
        if (!hasEntityData(stack)) {
            notifyMissingData(player);
            return InteractionResult.FAIL;
        }

        Vec3 spawnPos = context.getClickLocation()
                .add(Vec3.atLowerCornerOf(context.getClickedFace().getNormal()).scale(0.02D));
        MegaChicken restored = restore(serverLevel, stack, spawnPos, player);
        if (restored == null) {
            notifyMissingData(player);
            return InteractionResult.FAIL;
        }
        consumeOne(stack, player);
        serverLevel.gameEvent(player, GameEvent.ENTITY_PLACE, BlockPos.containing(restored.position()));
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.success(stack);
        }

        if (requiresActivation(stack) && !isActivated(stack)) {
            return tryActivate(stack, player)
                    ? InteractionResultHolder.consume(stack)
                    : InteractionResultHolder.fail(stack);
        }

        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(stack);
        }

        Vec3 spawnPos = hit.getLocation()
                .add(Vec3.atLowerCornerOf(hit.getDirection().getNormal()).scale(0.02D));
        MegaChicken restored = restore(serverLevel, stack, spawnPos, player);
        if (restored == null) {
            notifyMissingData(player);
            return InteractionResultHolder.fail(stack);
        }
        consumeOne(stack, player);
        serverLevel.gameEvent(player, GameEvent.ENTITY_PLACE, BlockPos.containing(restored.position()));
        return InteractionResultHolder.consume(stack);
    }

    @Nullable
    private MegaChicken restore(ServerLevel level, ItemStack stack, Vec3 position, @Nullable Player player) {
        ResourceLocation typeId = readEntityType(stack);
        ResourceLocation megaChickenId = BuiltInRegistries.ENTITY_TYPE.getKey(ModEntityTypes.MEGA_CHICKEN.get());
        if (typeId == null || !typeId.equals(megaChickenId)) {
            return null;
        }

        MegaChicken chicken = ModEntityTypes.MEGA_CHICKEN.get().create(level);
        if (chicken == null) {
            return null;
        }

        CompoundTag entityData = stack.getOrDefault(DataComponents.ENTITY_DATA, CustomData.EMPTY).copyTag();
        chicken.load(entityData);
        double safeY = Math.max(position.y(), level.getMinBuildHeight() + 0.01D);
        chicken.moveTo(position.x(), safeY, position.z(), level.random.nextFloat() * 360.0F, 0.0F);
        if (requiresActivation(stack)) {
            chicken.removeAllEffects();
            chicken.setHealth(chicken.getMaxHealth());
        }
        chicken.setDeltaMovement(Vec3.ZERO);
        chicken.setOnGround(true);
        level.addFreshEntity(chicken);

        if (player != null) {
            player.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(this));
        }
        return chicken;
    }

    private boolean tryActivate(ItemStack stack, @Nullable Player player) {
        if (player == null || !requiresActivation(stack) || isActivated(stack)) {
            return false;
        }
        ItemStack offhand = player.getOffhandItem();
        if (!offhand.is(Items.NETHER_STAR)) {
            player.displayClientMessage(
                    Component.translatable("item.chickens.mega_chicken.needs_nether_star"), true);
            return false;
        }

        offhand.shrink(1);
        setActivated(stack, true);
        player.displayClientMessage(Component.translatable("item.chickens.mega_chicken.activated"), true);
        return true;
    }

    private static void consumeOne(ItemStack stack, @Nullable Player player) {
        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }

    private static void notifyMissingData(@Nullable Player player) {
        if (player != null) {
            player.displayClientMessage(Component.translatable("item.chickens.mega_chicken.empty"), true);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (!hasEntityData(stack)) {
            tooltip.add(Component.translatable("item.chickens.mega_chicken.empty"));
        } else if (requiresActivation(stack) && !isActivated(stack)) {
            tooltip.add(Component.translatable("item.chickens.mega_chicken.inactive"));
        } else if (requiresActivation(stack)) {
            tooltip.add(Component.translatable("item.chickens.mega_chicken.active"));
        }
    }

    @SubscribeEvent
    public static void protectRevivalItem(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof ItemEntity item)) {
            return;
        }
        ItemStack stack = item.getItem();
        if (!stack.is(ModRegistry.MEGA_CHICKEN_ITEM.get()) || !requiresActivation(stack)) {
            return;
        }

        stack.set(DataComponents.FIRE_RESISTANT, Unit.INSTANCE);
        item.setInvulnerable(true);
        item.setNoGravity(true);
        item.setDeltaMovement(Vec3.ZERO);
        item.setUnlimitedLifetime();
        item.clearFire();

        double floor = event.getLevel().getMinBuildHeight() + 0.25D;
        if (item.getY() < floor) {
            item.setPos(item.getX(), floor, item.getZ());
        }
    }
}
