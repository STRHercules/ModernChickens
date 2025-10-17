package com.setycz.chickens.registry;

import com.setycz.chickens.ChickensMod;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredRegister.Blocks;
import net.neoforged.neoforge.registries.DeferredRegister.Items;
import net.neoforged.bus.api.IEventBus;

/**
 * Centralised registry bootstrap. The majority of the mod's content will
 * register through these deferred registers to ensure data is ready
 * before the game begins loading worlds.
 */
public final class ModRegistry {
    public static final Items ITEMS = DeferredRegister.createItems(ChickensMod.MOD_ID);
    public static final Blocks BLOCKS = DeferredRegister.createBlocks(ChickensMod.MOD_ID);

    private ModRegistry() {
    }

    public static void init(IEventBus modBus) {
        ITEMS.register(modBus);
        BLOCKS.register(modBus);
    }

    // These registrations are still simple placeholders; dedicated item
    // classes will follow once their behaviour has been ported from the
    // original mod.
    public static final DeferredItem<Item> SPAWN_EGG = ITEMS.register("spawn_egg",
            () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> COLORED_EGG = ITEMS.register("colored_egg",
            () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> LIQUID_EGG = ITEMS.register("liquid_egg",
            () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> ANALYZER = ITEMS.register("analyzer",
            () -> new Item(new Item.Properties().stacksTo(1)));
}
