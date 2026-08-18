package strhercules.chickens;

import strhercules.chickens.entity.ChickensChicken;
import strhercules.chickens.registry.ModEffects;
import strhercules.chickens.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.TickEvent;

import java.util.Collection;

public final class LavaChickenGameplay {
    private LavaChickenGameplay() {
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(LavaChickenGameplay::onLivingDrops);
        MinecraftForge.EVENT_BUS.addListener(LavaChickenGameplay::onPlayerTick);
        MinecraftForge.EVENT_BUS.addListener(LavaChickenGameplay::onIncomingDamage);
    }

    public static void dropForModernChicken(ChickensChicken chicken, DamageSource source) {
        if (!isEligibleModernChicken(chicken) || !isLava(source) || !rollDrop(chicken)) {
            return;
        }
        chicken.spawnAtLocation(new ItemStack(ModRegistry.LAVA_CHICKEN.get()), 0.0F);
    }

    private static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.getType() != EntityType.CHICKEN
                || !isLava(event.getSource()) || !rollDrop(entity)) {
            return;
        }
        addDrop(event.getDrops(), entity);
    }

    private static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
        if (!player.hasEffect(ModEffects.BURNING.get())) {
            return;
        }

        // Keep the vanilla fire overlay active without allowing the fire timer to
        // expire before the beneficial effect does.
        player.setRemainingFireTicks(Math.max(player.getRemainingFireTicks(), 2));
        if (player.level().isClientSide || !player.onGround() || player.tickCount % 4 != 0) {
            return;
        }

        placeFire(player, BlockPos.containing(player.getX(), player.getY(), player.getZ()));

        Vec3 movement = player.getDeltaMovement();
        double horizontalSpeed = movement.horizontalDistanceSqr();
        if (horizontalSpeed < 0.0004D) {
            return;
        }

        double length = Math.sqrt(horizontalSpeed);
        BlockPos trailPos = BlockPos.containing(
                player.getX() - movement.x / length * 0.4D,
                player.getY(),
                player.getZ() - movement.z / length * 0.4D);
        placeFire(player, trailPos);
    }

    private static void placeFire(Player player, BlockPos pos) {
        BlockState state = player.level().getBlockState(pos);
        if (state.isAir() || state.canBeReplaced()) {
            player.level().setBlock(pos, ModRegistry.LAVA_CHICKEN_FIRE.get().defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    private static void onIncomingDamage(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof Player player) || !player.hasEffect(ModEffects.BURNING.get())) {
            return;
        }
        DamageSource source = event.getSource();
        if (source.is(DamageTypes.IN_FIRE) || source.is(DamageTypes.ON_FIRE)
                || source.is(DamageTypes.HOT_FLOOR) || source.is(DamageTypes.LAVA)) {
            event.setCanceled(true);
        }
    }

    private static boolean isEligibleModernChicken(ChickensChicken chicken) {
        ChickensRegistryItem description = ChickensRegistry.getByType(chicken.getChickenType());
        if (description == null) {
            return false;
        }
        String name = description.getEntityName();
        return name.equalsIgnoreCase("VanillaChicken") || name.equalsIgnoreCase("SmartChicken");
    }

    private static boolean isLava(DamageSource source) {
        return source.is(DamageTypes.LAVA);
    }

    private static boolean rollDrop(LivingEntity entity) {
        return entity.getRandom().nextFloat() < 0.5F;
    }

    private static void addDrop(Collection<ItemEntity> drops, LivingEntity entity) {
        drops.add(new ItemEntity(entity.level(), entity.getX(), entity.getY() + 0.2D, entity.getZ(),
                new ItemStack(ModRegistry.LAVA_CHICKEN.get())));
    }
}
