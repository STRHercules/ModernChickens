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
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Collection;

public final class LavaChickenGameplay {
    private LavaChickenGameplay() {
    }

    public static void init() {
        NeoForge.EVENT_BUS.addListener(LavaChickenGameplay::onLivingDrops);
        NeoForge.EVENT_BUS.addListener(LavaChickenGameplay::onPlayerTick);
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

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || !player.hasEffect(ModEffects.BURNING)
                || !player.onGround() || player.tickCount % 4 != 0) {
            return;
        }

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
        BlockState state = player.level().getBlockState(trailPos);
        if (state.isAir() || state.canBeReplaced()) {
            player.level().setBlock(trailPos, ModRegistry.LAVA_CHICKEN_FIRE.get().defaultBlockState(), Block.UPDATE_ALL);
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
