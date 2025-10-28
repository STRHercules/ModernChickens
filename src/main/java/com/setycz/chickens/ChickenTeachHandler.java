package com.setycz.chickens;

import com.setycz.chickens.entity.ChickensChicken;
import com.setycz.chickens.registry.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Listens for players teaching vanilla chickens using a book. The handler
 * mirrors the legacy behaviour by converting a vanilla chicken into the smart
 * chicken variant as soon as the player interacts with a book.
 */
public final class ChickenTeachHandler {
    private ChickenTeachHandler() {
    }

    public static void init() {
        NeoForge.EVENT_BUS.addListener(ChickenTeachHandler::onEntityInteract);
        NeoForge.EVENT_BUS.addListener(ChickenTeachHandler::onEntityInteractSpecific);
    }

    private static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Chicken chicken)) {
            return;
        }
        if (handleBookTeach(event, chicken)) {
            // Cancelling the event mirrors the legacy Forge behaviour where the
            // vanilla chicken disappears immediately after being taught.
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    private static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!(event.getTarget() instanceof Chicken chicken)) {
            return;
        }
        if (handleBookTeach(event, chicken)) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    /**
     * Shared interaction handler for both the generic and specific entity
     * interaction events. The vanilla event system fires either variant
     * depending on where the player clicks, so funnelling them through a single
     * method guarantees we process every successful book interaction. Returning
     * {@code true} signals the caller to cancel the event so vanilla logic does
     * not run after the conversion.
     */
    private static boolean handleBookTeach(PlayerInteractEvent event, Chicken chicken) {
        ItemStack stack = event.getItemStack();
        if (!stack.is(Items.BOOK) || chicken.getType() != EntityType.CHICKEN) {
            return false;
        }

        ChickensRegistryItem smartChickenData = ChickensRegistry.getSmartChicken();
        if (smartChickenData == null || !smartChickenData.isEnabled()) {
            return false;
        }

        Level level = event.getLevel();
        if (level instanceof ServerLevel serverLevel && !convertToSmartChicken(serverLevel, chicken, smartChickenData)) {
            return false;
        }

        return true;
    }

    /**
     * Spawns a fresh smart chicken on the server, mirroring the vanilla
     * chicken's state before removing the original entity. Returning a boolean
     * lets the caller keep client-side cancellation logic in sync with server
     * success.
     */
    private static boolean convertToSmartChicken(ServerLevel serverLevel, Chicken chicken, ChickensRegistryItem smartChickenData) {
        ChickensChicken smartChicken = ModEntityTypes.CHICKENS_CHICKEN.get().create(serverLevel);
        if (smartChicken == null) {
            return false;
        }

        smartChicken.moveTo(chicken.getX(), chicken.getY(), chicken.getZ(), chicken.getYRot(), chicken.getXRot());
        smartChicken.setYHeadRot(chicken.getYHeadRot());
        smartChicken.setChickenType(smartChickenData.getId());
        smartChicken.setAge(chicken.getAge());
        if (chicken.hasCustomName()) {
            smartChicken.setCustomName(chicken.getCustomName());
            smartChicken.setCustomNameVisible(chicken.isCustomNameVisible());
        }

        BlockPos blockPos = chicken.blockPosition();
        smartChicken.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(blockPos), MobSpawnType.CONVERSION, null);
        serverLevel.addFreshEntity(smartChicken);
        smartChicken.spawnAnim();
        chicken.discard();
        return true;
    }
}
