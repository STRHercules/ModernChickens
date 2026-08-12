package strhercules.chickens;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Shared item-to-breed teaching mappings used by both chicken interaction paths.
 * KubeJS adds or replaces entries during the common setup registry event.
 */
public final class ChickenTeachingRegistry {
    private static final Map<Item, String> TARGETS = new HashMap<>();

    static {
        TARGETS.put(Items.BOOK, "SmartChicken");
        TARGETS.put(Items.CAKE, "chickenNosto");
        TARGETS.put(Blocks.GRASS_BLOCK.asItem(), "americanChicken");
        TARGETS.put(Blocks.DIRT.asItem(), "dirtChicken");
    }

    private ChickenTeachingRegistry() {
    }

    public static void register(Item trigger, String chickenName) {
        if (trigger != null && chickenName != null && !chickenName.isBlank()) {
            TARGETS.put(trigger, normaliseName(chickenName));
        }
    }

    @Nullable
    public static ChickensRegistryItem resolve(ItemStack held) {
        if (held.isEmpty()) {
            return null;
        }
        String chickenName = TARGETS.get(held.getItem());
        return chickenName == null ? null : ChickensRegistry.getByEntityName(chickenName);
    }

    private static String normaliseName(String name) {
        String trimmed = name.trim();
        int separator = trimmed.indexOf(':');
        return separator < 0 ? trimmed : trimmed.substring(separator + 1);
    }
}
