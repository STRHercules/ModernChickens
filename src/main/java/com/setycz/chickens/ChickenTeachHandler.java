package com.setycz.chickens;

import com.setycz.chickens.registry.ModRegistry;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
    }

    private static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        ItemStack stack = event.getItemStack();
        if (!stack.is(Items.BOOK)) {
            return;
        }
        if (!(event.getTarget() instanceof Chicken chicken) || event.getTarget().getType() != EntityType.CHICKEN) {
            return;
        }
        if (event.getLevel().isClientSide()) {
            return;
        }
        ChickensRegistryItem smartChickenData = ChickensRegistry.getSmartChicken();
        if (smartChickenData == null || !smartChickenData.isEnabled()) {
            return;
        }
        // Replace the vanilla chicken with an item stack representing the smart variant so
        // players can teach it later without spawning the modded entity immediately.
        ItemStack smartChickenStack = ModRegistry.CHICKEN_ITEM.get().createFor(smartChickenData);
        chicken.spawnAtLocation(smartChickenStack, 0.0F);
        chicken.discard();
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}
