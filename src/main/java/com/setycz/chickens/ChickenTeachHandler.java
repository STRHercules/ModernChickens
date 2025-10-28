package com.setycz.chickens;

import com.setycz.chickens.registry.ModRegistry;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Listens for players teaching vanilla chickens using a book. When triggered
 * the targeted vanilla chicken is removed and a Smart Chicken item stack is
 * dropped at the interaction point so players can deploy the upgraded bird on
 * their own terms.
 */
public final class ChickenTeachHandler {
    private ChickenTeachHandler() {
    }

    public static void init() {
        NeoForge.EVENT_BUS.addListener(ChickenTeachHandler::onEntityInteract);
        NeoForge.EVENT_BUS.addListener(ChickenTeachHandler::onEntityInteractSpecific);
    }

    private static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (handleTeach(event, event.getTarget())) {
            event.setCancellationResult(InteractionResult.CONSUME);
            event.setCanceled(true);
        }
    }

    private static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (handleTeach(event, event.getTarget())) {
            event.setCancellationResult(InteractionResult.CONSUME);
            event.setCanceled(true);
        }
    }

    private static boolean handleTeach(PlayerInteractEvent event, Entity target) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return false;
        }
        ItemStack stack = event.getItemStack();
        if (!stack.is(Items.BOOK)) {
            return false;
        }
        if (!(target instanceof Chicken chicken) || target.getType() != EntityType.CHICKEN) {
            return false;
        }

        if (event.getLevel().isClientSide()) {
            // Mirror the interaction result client-side so vanilla logic never spawns
            // a random chicken while we wait for the server to process the drop.
            return true;
        }

        ChickensRegistryItem smartChickenData = ChickensRegistry.getSmartChicken();
        if (smartChickenData == null || !smartChickenData.isEnabled()) {
            return false;
        }

        // Build a fresh Smart Chicken stack and drop it in place of the vanilla chicken.
        ItemStack smartChickenStack = ModRegistry.CHICKEN_ITEM.get().createFor(smartChickenData);
        ItemEntity drop = new ItemEntity(event.getLevel(), chicken.getX(), chicken.getY(), chicken.getZ(), smartChickenStack);
        drop.setDefaultPickUpDelay();
        event.getLevel().addFreshEntity(drop);
        chicken.remove(Entity.RemovalReason.DISCARDED);
        return true;
    }
}
