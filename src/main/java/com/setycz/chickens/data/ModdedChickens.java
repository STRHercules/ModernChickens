package com.setycz.chickens.data;

import com.setycz.chickens.ChickensMod;
import com.setycz.chickens.ChickensRegistry;
import com.setycz.chickens.ChickensRegistryItem;
import com.setycz.chickens.SpawnType;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Registers the extended set of chickens that mirror the content shipped
 * in More Chickens. Each definition is only enabled if the required item
 * is present, which keeps compatibility with mod packs that selectively
 * include the referenced resources.
 */
final class ModdedChickens {
    private static final Logger LOGGER = LoggerFactory.getLogger("ChickensModded");
    private static final List<Definition> DEFINITIONS = buildDefinitions();
    private static final Set<Integer> PENDING = new HashSet<>();

    private ModdedChickens() {
    }

    static void register(List<ChickensRegistryItem> chickens, Map<String, ChickensRegistryItem> byName) {
        attemptRegistration(chickens, byName, false);
    }

    static void retryPending() {
        if (PENDING.isEmpty()) {
            return;
        }

        Map<String, ChickensRegistryItem> index = buildRegistryIndex();
        int before = PENDING.size();
        attemptRegistration(null, index, true);
        int after = PENDING.size();
        if (after < before) {
            LOGGER.info("Registered {} additional chickens after tag reload", before - after);
        }
    }

    private static ChickensRegistryItem resolveParent(Map<String, ChickensRegistryItem> byName, @Nullable String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        return byName.get(name.toLowerCase(Locale.ROOT));
    }

    private static ResourceLocation texture(String path) {
        return ResourceLocation.fromNamespaceAndPath(ChickensMod.MOD_ID, "textures/entity/" + path + ".png");
    }

    private static Map<String, ChickensRegistryItem> buildRegistryIndex() {
        Map<String, ChickensRegistryItem> map = new HashMap<>();
        for (ChickensRegistryItem chicken : ChickensRegistry.getItems()) {
            map.put(chicken.getEntityName().toLowerCase(Locale.ROOT), chicken);
        }
        for (ChickensRegistryItem chicken : ChickensRegistry.getDisabledItems()) {
            map.putIfAbsent(chicken.getEntityName().toLowerCase(Locale.ROOT), chicken);
        }
        return map;
    }

    private static void attemptRegistration(@Nullable List<ChickensRegistryItem> collector,
            Map<String, ChickensRegistryItem> byName, boolean registerImmediately) {
        for (Definition def : DEFINITIONS) {
            if (ChickensRegistry.getByType(def.id()) != null || byName.containsKey(def.entityName().toLowerCase(Locale.ROOT))) {
                PENDING.remove(def.id());
                continue;
            }

            Optional<ItemStack> layItem = def.layItemSupplier().get();
            if (layItem.isEmpty()) {
                PENDING.add(def.id());
                LOGGER.debug("Deferring {} because no lay item was found", def.entityName());
                continue;
            }
            ItemStack layStack = layItem.get();

            ChickensRegistryItem parent1 = resolveParent(byName, def.parent1());
            if (def.parent1() != null && parent1 == null) {
                PENDING.add(def.id());
                LOGGER.debug("Deferring {} because parent {} is unavailable", def.entityName(), def.parent1());
                continue;
            }
            ChickensRegistryItem parent2 = resolveParent(byName, def.parent2());
            if (def.parent2() != null && parent2 == null) {
                PENDING.add(def.id());
                LOGGER.debug("Deferring {} because parent {} is unavailable", def.entityName(), def.parent2());
                continue;
            }

            Optional<ChickensRegistryItem> existing = findByLayItem(byName.values(), layStack);
            ChickensRegistryItem dynamicToDisable = null;
            if (existing.isPresent()) {
                ChickensRegistryItem current = existing.get();
                if (current.hasGeneratedTexture()) {
                    dynamicToDisable = current;
                } else {
                    LOGGER.debug("Skipping {} because {} already represents {}", def.entityName(), describeItem(layStack), current.getEntityName());
                    continue;
                }
            }

            ChickensRegistryItem chicken = new ChickensRegistryItem(
                    def.id(),
                    def.entityName(),
                    texture(def.texturePath()),
                    layStack,
                    def.bgColor(),
                    def.fgColor(),
                    parent1,
                    parent2);
            chicken.setSpawnType(def.spawnType());
            if (def.dropItemSupplier() != null) {
                def.dropItemSupplier().get().ifPresent(chicken::setDropItem);
            }

            if (dynamicToDisable != null) {
                String existingKey = dynamicToDisable.getEntityName().toLowerCase(Locale.ROOT);
                byName.remove(existingKey);
                dynamicToDisable.setEnabled(false);
                dynamicToDisable.setNoParents();
                LOGGER.info("Replacing dynamic chicken {} with {} for {}", dynamicToDisable.getEntityName(), def.entityName(), describeItem(layStack));
            }

            String nameKey = def.entityName().toLowerCase(Locale.ROOT);
            byName.put(nameKey, chicken);
            if (collector != null) {
                collector.add(chicken);
            }
            if (registerImmediately) {
                ChickensRegistry.register(chicken);
            }
            PENDING.remove(def.id());
        }
    }

    private static List<Definition> buildDefinitions() {
        List<Definition> list = new ArrayList<>();

        // Vanilla/More Chickens extras
        list.add(new Definition(500, "pShardChicken", "pshard_chicken",
                () -> Optional.of(new ItemStack(Items.PRISMARINE_SHARD)),
                0x3aa6a4, 0x6fd6d4, SpawnType.NONE,
                "WaterChicken", "SandChicken", null));

        list.add(new Definition(501, "pCrystalChicken", "pcrystal_chicken",
                () -> Optional.of(new ItemStack(Items.PRISMARINE_CRYSTALS)),
                0x46b9c7, 0x94e5f2, SpawnType.NONE,
                "pShardChicken", "GlowstoneChicken", null));

        list.add(new Definition(502, "soulSandChicken", "soulsand_chicken",
                () -> Optional.of(new ItemStack(Blocks.SOUL_SAND)),
                0x5b3b2a, 0x362113, SpawnType.HELL,
                null, null, null));

        list.add(new Definition(503, "obsidianChicken", "obsidian_chicken",
                () -> Optional.of(new ItemStack(Blocks.OBSIDIAN)),
                0x221934, 0x5b3d7a, SpawnType.NONE,
                "WaterChicken", "LavaChicken", null));

        list.add(new Definition(504, "xpChicken", "xp_chicken",
                () -> Optional.of(new ItemStack(Items.EXPERIENCE_BOTTLE)),
                0x3dff1e, 0x3ff123, SpawnType.NONE,
                "EmeraldChicken", "GreenChicken", null));

        // Draconic Evolution
        list.add(new Definition(505, "draconiumChicken", "draconic/draconium_chicken",
                combine(oreTag("ingots", "draconium"),
                        item("draconicevolution:draconium_ingot")),
                0x301549, 0x1a0c27, SpawnType.NONE,
                "GunpowderChicken", "EnderChicken", null));

        list.add(new Definition(506, "draconiumAwakenedChicken", "draconic/draconium_awakened_chicken",
                combine(oreTag("ingots", "awakened_draconium"),
                        oreTag("ingots", "draconium_awakened"),
                        oreTag("nuggets", "awakened_draconium"),
                        item("draconicevolution:awakened_draconium_ingot")),
                0xcc440c, 0x9c691a, SpawnType.NONE,
                "draconiumChicken", "EnderChicken", null));

        // Botania
        list.add(new Definition(507, "manasteelchicken", "botania/manasteel_chicken",
                combine(item("botania:manasteel_ingot"),
                        oreTag("ingots", "manasteel")),
                0x69d7ff, 0x002c4b, SpawnType.NONE,
                "IronChicken", "GhastChicken", null));

        list.add(new Definition(508, "terrasteelchicken", "botania/terrasteel_chicken",
                combine(item("botania:terrasteel_ingot"),
                        oreTag("ingots", "terrasteel")),
                0x3ff123, 0xf5fcf1, SpawnType.NONE,
                "EnderChicken", "pCrystalChicken", null));

        list.add(new Definition(509, "elementiumchicken", "botania/elementium_chicken",
                combine(item("botania:elementium_ingot"),
                        oreTag("ingots", "elementium")),
                0xf655f3, 0xb407b7, SpawnType.NONE,
                "manasteelchicken", "terrasteelchicken", null));

        // Base metals (ore dictionary style)
        list.add(new Definition(510, "copperchicken", "basemetals/copper_chicken",
                oreTag("ingots", "copper"),
                0xc06a48, 0xff9d76, SpawnType.NONE,
                "YellowChicken", "BrownChicken", null));

        list.add(new Definition(511, "tinchicken", "basemetals/tin_chicken",
                oreTag("ingots", "tin"),
                0xfff7ee, 0xbbb1a7, SpawnType.NONE,
                "WhiteChicken", "ClayChicken", null));

        list.add(new Definition(512, "zincchicken", "basemetals/zinc_chicken",
                oreTag("ingots", "zinc"),
                0xb7b7b7, 0x868686, SpawnType.NONE,
                "WhiteChicken", "ClayChicken", null));

        list.add(new Definition(513, "leadchicken", "basemetals/lead_chicken",
                oreTag("ingots", "lead"),
                0x777777, 0x383838, SpawnType.NONE,
                "IronChicken", "CyanChicken", null));

        list.add(new Definition(514, "nickelchicken", "basemetals/nickel_chicken",
                oreTag("ingots", "nickel"),
                0xefffec, 0xa2b69f, SpawnType.NONE,
                "WhiteChicken", "GreenChicken", null));

        list.add(new Definition(515, "silverorechicken", "basemetals/silver_chicken",
                oreTag("ingots", "silver"),
                0xbebebe, 0xffffff, SpawnType.NONE,
                "IronChicken", "WhiteChicken", null));

        list.add(new Definition(516, "platinumchicken", "basemetals/platinum_chicken",
                oreTag("ingots", "platinum"),
                0xffffff, 0x8d9a96, SpawnType.NONE,
                "nickelchicken", "silverorechicken", null));

        list.add(new Definition(517, "invarchicken", "basemetals/invar_chicken",
                oreTag("ingots", "invar"),
                0x989585, 0xd1ccb6, SpawnType.NONE,
                "IronChicken", "nickelchicken", null));

        list.add(new Definition(518, "bronzechicken", "basemetals/bronze_chicken",
                oreTag("ingots", "bronze"),
                0x9a6731, 0xf6a44e, SpawnType.NONE,
                "copperchicken", "tinchicken", null));

        list.add(new Definition(519, "steelchicken", "basemetals/steel_chicken",
                oreTag("ingots", "steel"),
                0xd3e1e3, 0x8e9799, SpawnType.NONE,
                "IronChicken", "CoalChicken", null));

        list.add(new Definition(520, "siliconchicken", "basemetals/silicon_chicken",
                combine(tag("forge:silicon", "c:silicon"),
                        oreTag("gems", "silicon"),
                        oreTag("dusts", "silicon")),
                0x5f706b, 0x424242, SpawnType.NONE,
                "ClayChicken", "SandChicken", null));

        list.add(new Definition(521, "sulfurchicken", "basemetals/sulfur_chicken",
                combine(oreTag("dusts", "sulfur"),
                        oreTag("gems", "sulfur")),
                0xffe782, 0xad9326, SpawnType.NONE,
                "GunpowderChicken", "FlintChicken", null));

        list.add(new Definition(522, "saltpeterchicken", "basemetals/saltpeter_chicken",
                combine(oreTag("dusts", "saltpeter"),
                        oreTag("dusts", "niter")),
                0xddd6d6, 0xac9e9d, SpawnType.NONE,
                "sulfurchicken", "RedstoneChicken", null));

        list.add(new Definition(523, "brasschicken", "basemetals/brass_chicken",
                oreTag("ingots", "brass"),
                0xa99340, 0xffe377, SpawnType.NONE,
                "copperchicken", "zincchicken", null));

        list.add(new Definition(524, "cupronickelchicken", "basemetals/cupronickel_chicken",
                oreTag("ingots", "cupronickel"),
                0xd8ccb4, 0x98896c, SpawnType.NONE,
                "copperchicken", "nickelchicken", null));

        list.add(new Definition(525, "electrumchicken", "basemetals/electrum_chicken",
                oreTag("ingots", "electrum"),
                0xfff2b1, 0xd4be50, SpawnType.NONE,
                "silverorechicken", "GoldChicken", null));

        list.add(new Definition(526, "aluminumChicken", "basemetals/aluminium_chicken",
                combine(oreTag("ingots", "aluminum"),
                        oreTag("ingots", "aluminium")),
                0xd3dddc, 0xcbd7d6, SpawnType.NONE,
                "FlintChicken", "IronChicken", null));

        // Mekanism
        list.add(new Definition(527, "osmiumChicken", "mekanism/osmium_chicken",
                combine(oreTag("ingots", "osmium"),
                        item("mekanism:ingot_osmium")),
                0x989585, 0xd1ccb6, SpawnType.NONE,
                "IronChicken", "QuartzChicken", null));

        // Immersive Engineering
        list.add(new Definition(528, "uraniumChicken", "immersive_engineering/uranium_chicken",
                oreTag("ingots", "uranium"),
                0x91d76d, 0x9ce26c, SpawnType.NONE,
                "RedstoneChicken", "EnderChicken", null));

        list.add(new Definition(529, "constantanChicken", "immersive_engineering/consrtantan_chicken",
                oreTag("ingots", "constantan"),
                0xf98669, 0x795851, SpawnType.NONE,
                "copperchicken", "nickelchicken", null));

        // Extreme Reactors
        list.add(new Definition(530, "yelloriumChicken", "extreme_reactors/yellorium_chicken",
                oreTag("ingots", "yellorium"),
                0xa5b700, 0xd7ef00, SpawnType.NONE,
                "GlowstoneChicken", "EnderChicken", null));

        list.add(new Definition(531, "graphiteChicken", "extreme_reactors/graphite_chicken",
                combine(oreTag("ingots", "graphite"),
                        oreTag("dusts", "graphite"),
                        oreTag("gems", "graphite")),
                0x41453f, 0x595959, SpawnType.NONE,
                "CoalChicken", "BlackChicken", null));

        list.add(new Definition(532, "cyaniteChicken", "extreme_reactors/cyanite_chicken",
                oreTag("ingots", "cyanite"),
                0x0068b4, 0x5cafdb, SpawnType.NONE,
                "yelloriumChicken", "SandChicken", null));

        list.add(new Definition(533, "blutoniumChicken", "extreme_reactors/blutonium_chicken",
                oreTag("ingots", "blutonium"),
                0x4642d6, 0xf5fcf1, SpawnType.NONE,
                "cyaniteChicken", "WaterChicken", null));

        // Ender IO
        list.add(new Definition(534, "electricalSteelChicken", "enderio/electrical_steel_chicken",
                oreTag("ingots", "electrical_steel"),
                0x939393, 0x474747, SpawnType.NONE,
                "IronChicken", "siliconchicken", null));

        list.add(new Definition(535, "energeticAlloyChicken", "enderio/energetic_alloy_chicken",
                oreTag("ingots", "energetic_alloy"),
                0xea6c05, 0x65321b, SpawnType.NONE,
                "GoldChicken", "GlowstoneChicken", null));

        list.add(new Definition(536, "vibrantAlloyChicken", "enderio/vibrant_alloy_chicken",
                oreTag("ingots", "vibrant_alloy"),
                0xbcf239, 0x779c1d, SpawnType.NONE,
                "energeticAlloyChicken", "EnderChicken", null));

        list.add(new Definition(537, "redstoneAlloyChicken", "enderio/redstone_alloy_chicken",
                oreTag("ingots", "redstone_alloy"),
                0xd03939, 0x621919, SpawnType.NONE,
                "RedstoneChicken", "siliconchicken", null));

        list.add(new Definition(538, "conductiveIronChicken", "enderio/conductive_iron_chicken",
                oreTag("ingots", "conductive_iron"),
                0xcc9d96, 0x7e6764, SpawnType.NONE,
                "RedstoneChicken", "IronChicken", null));

        list.add(new Definition(539, "pulsatingIronChicken", "enderio/pulsating_iron_chicken",
                oreTag("ingots", "pulsating_iron"),
                0x6fe78b, 0x406448, SpawnType.NONE,
                "IronChicken", "EnderChicken", null));

        list.add(new Definition(540, "darkSteelChicken", "enderio/dark_steel_chicken",
                oreTag("ingots", "dark_steel"),
                0x4d4d4e, 0x242424, SpawnType.NONE,
                "IronChicken", "obsidianChicken", null));

        list.add(new Definition(541, "soulariumChicken", "enderio/soularium_chicken",
                oreTag("ingots", "soularium"),
                0x6f5c36, 0x4e371a, SpawnType.NONE,
                "soulSandChicken", "GoldChicken", null));

        // Thermal Foundation
        list.add(new Definition(542, "slagChicken", "thermal_foundation/slag_chicken",
                combine(oreTag("dusts", "slag"),
                        item("thermal:slag")),
                0x83715e, 0x443b31, SpawnType.NONE,
                "RedstoneChicken", "IronChicken", null));

        list.add(new Definition(543, "richSlagChicken", "thermal_foundation/rich_slag_chicken",
                combine(oreTag("dusts", "rich_slag"),
                        item("thermal:rich_slag")),
                0x5b3f20, 0x3d2915, SpawnType.NONE,
                "slagChicken", "slagChicken", null));

        list.add(new Definition(544, "basalzRodChicken", "thermal_foundation/basalz_rod_chicken",
                oreTag("rods", "basalz"),
                0x980000, 0x6e6664, SpawnType.NONE,
                "saltpeterchicken", "BlazeChicken", null));

        list.add(new Definition(545, "blitzRodChicken", "thermal_foundation/blitz_rod_chicken",
                oreTag("rods", "blitz"),
                0xece992, 0x66e5ef, SpawnType.NONE,
                "basalzRodChicken", "sulfurchicken", null));

        list.add(new Definition(546, "blizzRodChicken", "thermal_foundation/blizz_rod_chicken",
                oreTag("rods", "blizz"),
                0x88e0ff, 0x1d3b95, SpawnType.NONE,
                "blitzRodChicken", "SnowballChicken", null));

        list.add(new Definition(547, "cinnabarChicken", "thermal_foundation/cinnabar_chicken",
                combine(oreTag("gems", "cinnabar"),
                        oreTag("dusts", "cinnabar")),
                0xe49790, 0x9b3229, SpawnType.NONE,
                "richSlagChicken", "DiamondChicken", null));

        list.add(new Definition(548, "signalumChicken", "thermal_foundation/signalum_chicken",
                oreTag("ingots", "signalum"),
                0xffa424, 0xc63200, SpawnType.NONE,
                "copperchicken", "silverorechicken", null));

        list.add(new Definition(549, "enderiumChicken", "thermal_foundation/enderium_chicken",
                combine(oreTag("ingots", "enderium"),
                        oreTag("nuggets", "enderium")),
                0x127575, 0x0a4849, SpawnType.NONE,
                "platinumchicken", "EnderChicken", null));

        list.add(new Definition(550, "iridiumChicken", "thermal_foundation/iridium_chicken",
                combine(oreTag("ingots", "iridium"),
                        oreTag("nuggets", "iridium")),
                0xedebf1, 0xbbbcdd, SpawnType.NONE,
                "enderiumChicken", "blizzRodChicken", null));

        list.add(new Definition(551, "lumiumChicken", "thermal_foundation/lumium_chicken",
                oreTag("ingots", "lumium"),
                0xeef4df, 0xf4b134, SpawnType.NONE,
                "tinchicken", "GlowstoneChicken", null));

        list.add(new Definition(552, "mithrilChicken", "thermal_foundation/mithril_chicken",
                oreTag("ingots", "mithril"),
                0x5a89a8, 0xa7ffff, SpawnType.NONE,
                "iridiumChicken", "GoldChicken", null));

        return list;
    }

    private static Supplier<Optional<ItemStack>> oreTag(String category, String material) {
        String keyword = categoryKeyword(category);
        return combine(
                tag("forge:" + category + "/" + material, "c:" + category + "/" + material),
                searchRegistry(keyword, material),
                searchRegistryExact(material));
    }

    private static Supplier<Optional<ItemStack>> tag(String... tagNames) {
        return tag(1, tagNames);
    }

    private static Supplier<Optional<ItemStack>> tag(int count, String... tagNames) {
        return () -> {
            for (String tagName : tagNames) {
                Optional<ItemStack> stack = stackFromTag(count, tagName);
                if (stack.isPresent()) {
                    return stack;
                }
            }
            return Optional.empty();
        };
    }

    private static Optional<ItemStack> stackFromTag(int count, String tagName) {
        ResourceLocation id = ResourceLocation.parse(tagName);
        TagKey<Item> tag = TagKey.create(Registries.ITEM, id);
        Optional<ItemStack> fromHolderSet = BuiltInRegistries.ITEM.getTag(tag)
                .flatMap(set -> set.stream().findFirst())
                .map(Holder::value)
                .map(item -> new ItemStack(item, count));
        if (fromHolderSet.isPresent()) {
            return fromHolderSet;
        }
        for (Item item : BuiltInRegistries.ITEM) {
            if (item.builtInRegistryHolder().is(tag)) {
                return Optional.of(new ItemStack(item, count));
            }
        }
        return Optional.empty();
    }

    private static Supplier<Optional<ItemStack>> item(String... itemIds) {
        return item(1, itemIds);
    }

    private static Supplier<Optional<ItemStack>> item(int count, String... itemIds) {
        return () -> {
            for (String itemId : itemIds) {
                Optional<ItemStack> stack = stackFromItem(count, itemId);
                if (stack.isPresent()) {
                    return stack;
                }
            }
            return Optional.empty();
        };
    }

    private static Optional<ItemStack> stackFromItem(int count, String itemId) {
        ResourceLocation id = ResourceLocation.parse(itemId);
        return BuiltInRegistries.ITEM.getOptional(id)
                .map(item -> new ItemStack(item, count));
    }

    private static Supplier<Optional<ItemStack>> searchRegistry(@Nullable String keyword, String material) {
        return () -> {
            for (Item item : BuiltInRegistries.ITEM) {
                ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
                if (key == null) {
                    continue;
                }
                String path = key.getPath();
                if (keyword != null && !path.contains(keyword)) {
                    continue;
                }
                if (!path.contains(material)) {
                    continue;
                }
                return Optional.of(new ItemStack(item));
            }
            return Optional.empty();
        };
    }

    private static Supplier<Optional<ItemStack>> searchRegistryExact(String material) {
        return () -> {
            for (Item item : BuiltInRegistries.ITEM) {
                ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
                if (key == null) {
                    continue;
                }
                String path = key.getPath();
                if (path.equals(material)) {
                    return Optional.of(new ItemStack(item));
                }
            }
            return Optional.empty();
        };
    }

    @Nullable
    private static String categoryKeyword(String category) {
        return switch (category) {
            case "ingots" -> "ingot";
            case "nuggets" -> "nugget";
            case "dusts" -> "dust";
            case "gems" -> "gem";
            case "rods" -> "rod";
            default -> null;
        };
    }

    @SafeVarargs
    private static Supplier<Optional<ItemStack>> combine(Supplier<Optional<ItemStack>>... suppliers) {
        return () -> {
            for (Supplier<Optional<ItemStack>> supplier : suppliers) {
                Optional<ItemStack> stack = supplier.get();
                if (stack.isPresent()) {
                    return stack;
                }
            }
            return Optional.empty();
        };
    }

    private record Definition(int id,
                              String entityName,
                              String texturePath,
                              Supplier<Optional<ItemStack>> layItemSupplier,
                              int bgColor,
                              int fgColor,
                              SpawnType spawnType,
                              @Nullable String parent1,
                              @Nullable String parent2,
                              @Nullable Supplier<Optional<ItemStack>> dropItemSupplier) {
    }

    private static Optional<ChickensRegistryItem> findByLayItem(Collection<ChickensRegistryItem> chickens, ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        for (ChickensRegistryItem chicken : chickens) {
            if (ItemStack.isSameItemSameComponents(chicken.createLayItem(), stack)) {
                return Optional.of(chicken);
            }
            if (ItemStack.isSameItemSameComponents(chicken.createDropItem(), stack)) {
                return Optional.of(chicken);
            }
        }
        return Optional.empty();
    }

    private static String describeItem(ItemStack stack) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key != null ? key.toString() : stack.toString();
    }
}
