package com.setycz.chickens;

import com.setycz.chickens.registry.ModRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Listens for players teaching vanilla chickens using a book. The handler
 * now swaps the vanilla mob out for a Smart Chicken item drop so players can
 * stash or deploy the trained bird later instead of spawning it immediately.
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
        Level level = event.getLevel();
        if (level.isClientSide) {
            return;
        }
        ChickensRegistryItem smartChickenData = ChickensRegistry.getSmartChicken();
        if (smartChickenData == null || !smartChickenData.isEnabled()) {
            return;
        }
        // Swap the vanilla chicken entity for the matching item stack so the
        // player can place the smart chicken whenever they are ready, mirroring
        // the legacy "teach" behaviour while honouring the new requirement.
        ItemStack smartChickenItem = ModRegistry.CHICKEN_ITEM.get().createFor(smartChickenData);
        if (chicken.hasCustomName()) {
            smartChickenItem.set(DataComponents.CUSTOM_NAME, chicken.getCustomName());
        }
        chicken.spawnAtLocation(smartChickenItem, 0.0F);
        chicken.discard();
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}
