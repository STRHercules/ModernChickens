package com.setycz.chickens;

import com.setycz.chickens.entity.ChickensChicken;
import com.setycz.chickens.registry.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Listens for players teaching vanilla chickens using a book. The handler
 * now performs a strict 1:1 conversion that always consumes the book and
 * replaces the interacted chicken with a smart chicken entity.
 */
public final class ChickenTeachHandler {
    private ChickenTeachHandler() {
    }

    public static void init() {
        NeoForge.EVENT_BUS.addListener(ChickenTeachHandler::onEntityInteract);
    }

    private static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        ItemStack stack = event.getItemStack();
        if (!stack.is(Items.BOOK)) {
            return;
        }
        Player player = event.getEntity();
        if (!(event.getTarget() instanceof Chicken chicken) || player == null || chicken.getType() != EntityType.CHICKEN) {
            return;
        }
        Level level = event.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        ChickensRegistryItem smartChickenData = ChickensRegistry.getSmartChicken();
        if (smartChickenData == null || !smartChickenData.isEnabled()) {
            return;
        }
        BlockPos blockPos = chicken.blockPosition();
        ChickensChicken smartChicken = ModEntityTypes.CHICKENS_CHICKEN.get().create(serverLevel);
        if (smartChicken == null) {
            return;
        }
        smartChicken.moveTo(chicken.getX(), chicken.getY(), chicken.getZ(), chicken.getYRot(), chicken.getXRot());
        smartChicken.setYHeadRot(chicken.getYHeadRot());
        smartChicken.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(blockPos), MobSpawnType.CONVERSION, null);
        // Reset the descriptor after spawn so the neo spawn hooks cannot randomise the chicken type.
        smartChicken.setChickenType(smartChickenData.getId());
        smartChicken.setAge(chicken.getAge());
        if (chicken.hasCustomName()) {
            smartChicken.setCustomName(chicken.getCustomName());
            smartChicken.setCustomNameVisible(chicken.isCustomNameVisible());
        }
        serverLevel.addFreshEntity(smartChicken);
        smartChicken.spawnAnim();
        chicken.discard();
        if (!player.getAbilities().instabuild) {
            // Consume the teaching book so every conversion remains a deliberate 1:1 action.
            stack.shrink(1);
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}
