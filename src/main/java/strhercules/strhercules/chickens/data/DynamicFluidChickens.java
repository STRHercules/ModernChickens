package strhercules.chickens.data;

import strhercules.chickens.ChickensMod;
import strhercules.chickens.ChickensRegistry;
import strhercules.chickens.ChickensRegistryItem;
import strhercules.chickens.LiquidEggRegistry;
import strhercules.chickens.LiquidEggRegistryItem;
import strhercules.chickens.SpawnType;
import strhercules.chickens.item.LiquidEggItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.List;
import java.util.Set;

/**
 * Dynamically creates chickens for every registered liquid egg so mod packs
 * automatically gain coverage for fluid resources introduced by other mods.
 * The chickens inherit the placeholder texture pipeline used by the dynamic
 * material generator, ensuring they integrate cleanly with the existing
 * rendering systems.
 */
final class DynamicFluidChickens {
    private static final Logger LOGGER = LoggerFactory.getLogger("ChickensDynamicFluid");
    private static final ResourceLocation PLACEHOLDER_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ChickensMod.MOD_ID, "textures/entity/unknownchicken.png");
    private static final int ID_BASE = 3_000_000;
    private static final int ID_SPAN = 1_000_000;

    private DynamicFluidChickens() {
    }

    static void register(List<ChickensRegistryItem> chickens, Map<String, ChickensRegistryItem> byName) {}

    static void refresh() {}
   
   // disabled auregister fluid chickens
}
