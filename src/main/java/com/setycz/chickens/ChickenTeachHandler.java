package com.setycz.chickens;

import com.setycz.chickens.ChickensRegistry;
import com.setycz.chickens.ChickensRegistryItem;
import com.setycz.chickens.registry.ModRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import net.minecraft.network.chat.Component;

/**
 * Listens for players teaching vanilla chickens using a book. The handler
 * mirrors the legacy interaction but now swaps the target bird for a Smart
 * Chicken item so players can place the upgraded chicken wherever they like.
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
        if (level instanceof ServerLevel serverLevel && !dropSmartChickenItem(serverLevel, chicken, smartChickenData)) {
            return false;
        }

        return true;
    }

    /**
     * Drops a smart chicken item in place of the vanilla bird so players can
     * collect it and spawn the new entity wherever they please. Returning a
     * boolean keeps the client-side cancellation logic in sync with server
     * success.
     */
    private static boolean dropSmartChickenItem(ServerLevel serverLevel, Chicken chicken, ChickensRegistryItem smartChickenData) {
        ItemStack smartChickenStack = ModRegistry.CHICKEN_ITEM.get().createFor(smartChickenData);
        Component customName = chicken.getCustomName();
        if (customName != null) {
            smartChickenStack.set(DataComponents.CUSTOM_NAME, customName);
        }

        // Ensure anything riding the chicken dismounts before the entity vanishes so
        // passengers do not disappear alongside the bird.
        chicken.ejectPassengers();

        if (chicken.spawnAtLocation(smartChickenStack, 0.0F) == null) {
            return false;
        }

        chicken.discard();
        return true;
    }
}
