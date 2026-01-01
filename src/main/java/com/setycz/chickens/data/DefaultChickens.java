package com.setycz.chickens.data;

import com.setycz.chickens.ChickensMod;
import com.setycz.chickens.ChickensRegistryItem;
import com.setycz.chickens.LiquidEggRegistry;
import com.setycz.chickens.LiquidEggRegistryItem;
import com.setycz.chickens.SpawnType;
import com.setycz.chickens.item.FluxEggItem;
import com.setycz.chickens.item.LiquidEggItem;
import com.setycz.chickens.registry.ModRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Recreates the legacy {@code generateDefaultChickens} method from the 1.10
 * release. The data has been massaged to use modern item constants (for
 * example, the generic log block has become the dedicated {@code oak_log})
 * but the relationships and identifiers are otherwise identical.
 */
public final class DefaultChickens {
    private static final Logger log = LoggerFactory.getLogger(DefaultChickens.class);

    private DefaultChickens() {
    }

    public static List<ChickensRegistryItem> create() {
        List<ChickensRegistryItem> chickens = new ArrayList<>();

        chickens.add(new ChickensRegistryItem(
                com.setycz.chickens.ChickensRegistry.SMART_CHICKEN_ID,
                "SmartChicken",
                texture("SmartChicken"),
                new ItemStack(Items.EGG),
                0xffffff, 0xffff00).setSpawnType(SpawnType.NONE));

        // Configure the base dye chickens to lay the legacy resource items instead of modern dyes.
        ChickensRegistryItem whiteChicken = createDyeChicken(DyeColor.WHITE, "WhiteChicken");
        whiteChicken.setLayItem(new ItemStack(Items.BONE));
        whiteChicken.setDropItem(new ItemStack(Items.BONE));
        whiteChicken.setSpawnType(SpawnType.NORMAL);
        chickens.add(whiteChicken);
        ChickensRegistryItem yellowChicken = createDyeChicken(DyeColor.YELLOW, "YellowChicken");
        chickens.add(yellowChicken);
        ChickensRegistryItem blueChicken = createDyeChicken(DyeColor.BLUE, "BlueChicken");
        blueChicken.setLayItem(new ItemStack(Items.LAPIS_LAZULI));
        chickens.add(blueChicken);
        ChickensRegistryItem greenChicken = createDyeChicken(DyeColor.GREEN, "GreenChicken");
        chickens.add(greenChicken);
        ChickensRegistryItem redChicken = createDyeChicken(DyeColor.RED, "RedChicken");
        chickens.add(redChicken);
        ChickensRegistryItem blackChicken = createDyeChicken(DyeColor.BLACK, "BlackChicken");
        blackChicken.setLayItem(new ItemStack(Items.INK_SAC));
        chickens.add(blackChicken);

        ChickensRegistryItem pinkChicken = createDyeChicken(DyeColor.PINK, "PinkChicken")
                .setParentsNew(redChicken, whiteChicken);
        chickens.add(pinkChicken);
        ChickensRegistryItem purpleChicken = createDyeChicken(DyeColor.PURPLE, "PurpleChicken")
                .setParentsNew(blueChicken, redChicken);
        chickens.add(purpleChicken);
        chickens.add(createDyeChicken(DyeColor.ORANGE, "OrangeChicken")
                .setParentsNew(redChicken, yellowChicken));
        chickens.add(createDyeChicken(DyeColor.LIGHT_BLUE, "LightBlueChicken")
                .setParentsNew(whiteChicken, blueChicken));
        chickens.add(createDyeChicken(DyeColor.LIME, "LimeChicken")
                .setParentsNew(greenChicken, whiteChicken));
        ChickensRegistryItem grayChicken = createDyeChicken(DyeColor.GRAY, "GrayChicken")
                .setParentsNew(blackChicken, whiteChicken);
        chickens.add(grayChicken);
        chickens.add(createDyeChicken(DyeColor.CYAN, "CyanChicken")
                .setParentsNew(blueChicken, greenChicken));

        chickens.add(createDyeChicken(DyeColor.LIGHT_GRAY, "SilverDyeChicken")
                .setParentsNew(grayChicken, whiteChicken));
        chickens.add(createDyeChicken(DyeColor.MAGENTA, "MagentaChicken")
                .setParentsNew(purpleChicken, pinkChicken));

        ChickensRegistryItem flintChicken = new ChickensRegistryItem(
                101, "FlintChicken", texture("FlintChicken"),
                new ItemStack(Items.FLINT),
                0x6b6b47, 0xa3a375)
                .allowNaturalSpawn();
        chickens.add(flintChicken);

        ChickensRegistryItem quartzChicken = new ChickensRegistryItem(
                104, "QuartzChicken", texture("QuartzChicken"),
                new ItemStack(Items.QUARTZ),
                0x4d0000, 0x1a0000).setSpawnType(SpawnType.HELL)
                .allowNaturalSpawn();
        chickens.add(quartzChicken);

        ChickensRegistryItem logChicken = new ChickensRegistryItem(
                108, "LogChicken", texture("LogChicken"),
                new ItemStack(Blocks.OAK_LOG),
                0x98846d, 0x528358)
                .allowNaturalSpawn();
        chickens.add(logChicken);

        ChickensRegistryItem sandChicken = new ChickensRegistryItem(
                105, "SandChicken", texture("SandChicken"),
                new ItemStack(Blocks.SAND),
                0xece5b1, 0xa7a06c);
        chickens.add(sandChicken);

        ChickensRegistryItem stringChicken = new ChickensRegistryItem(
                303, "StringChicken", texture("StringChicken"),
                new ItemStack(Items.STRING),
                0x331a00, 0x800000,
                blackChicken, logChicken
        ).setDropItem(new ItemStack(Items.SPIDER_EYE));
        chickens.add(stringChicken);

        ChickensRegistryItem glowstoneChicken = new ChickensRegistryItem(
                202, "GlowstoneChicken", texture("GlowstoneChicken"),
                new ItemStack(Items.GLOWSTONE_DUST),
                0xffff66, 0xffff00,
                quartzChicken, yellowChicken);
        chickens.add(glowstoneChicken);

        ChickensRegistryItem gunpowderChicken = new ChickensRegistryItem(
                100, "GunpowderChicken", texture("GunpowderChicken"),
                new ItemStack(Items.GUNPOWDER),
                0x999999, 0x404040,
                sandChicken, flintChicken)
                .allowNaturalSpawn();
        chickens.add(gunpowderChicken);

        ChickensRegistryItem redstoneChicken = new ChickensRegistryItem(
                201, "RedstoneChicken", texture("RedstoneChicken"),
                new ItemStack(Items.REDSTONE),
                0xe60000, 0x800000,
                redChicken,
                sandChicken);
        chickens.add(redstoneChicken);

        ChickensRegistryItem redstoneFluxChicken = new ChickensRegistryItem(
                404, "RedstoneFluxChicken", texture("RedstoneFluxChicken"),
                // Pre-charge the egg so freshly bred birds immediately output RF.
                FluxEggItem.create(FluxEggItem.BASE_CAPACITY),
                0xff3c3c, 0xffb347,
                redstoneChicken,
                glowstoneChicken);
        redstoneFluxChicken.setDropItem(FluxEggItem.create(FluxEggItem.BASE_CAPACITY));
        applyItemTextureOverride(redstoneFluxChicken);
        chickens.add(redstoneFluxChicken);

        ChickensRegistryItem glassChicken = new ChickensRegistryItem(
                106, "GlassChicken", texture("GlassChicken"),
                new ItemStack(Blocks.GLASS),
                0xffffff, 0xeeeeff,
                quartzChicken, redstoneChicken);
        chickens.add(glassChicken);

        ChickensRegistryItem ironChicken = new ChickensRegistryItem(
                203, "IronChicken", texture("IronChicken"),
                new ItemStack(Items.IRON_INGOT),
                0xffffcc, 0xffcccc,
                flintChicken, whiteChicken);
        chickens.add(ironChicken);

        ChickensRegistryItem coalChicken = new ChickensRegistryItem(
                204, "CoalChicken", texture("CoalChicken"),
                new ItemStack(Items.COAL),
                0x262626, 0x000000,
                flintChicken, logChicken);
        chickens.add(coalChicken);

        ChickensRegistryItem brownChicken = createDyeChicken(DyeColor.BROWN, "BrownChicken");
        brownChicken.setParentsNew(redChicken, greenChicken);
        brownChicken.setLayItem(new ItemStack(Items.COCOA_BEANS));
        chickens.add(brownChicken);

        ChickensRegistryItem goldChicken = new ChickensRegistryItem(
                300, "GoldChicken", texture("GoldChicken"),
                new ItemStack(Items.GOLD_NUGGET),
                0xcccc00, 0xffff80,
                ironChicken, yellowChicken);
        chickens.add(goldChicken);

        ChickensRegistryItem snowballChicken = new ChickensRegistryItem(
                102, "SnowballChicken", texture("SnowballChicken"),
                new ItemStack(Items.SNOWBALL),
                0x33bbff, 0x0088cc,
                blueChicken, logChicken).setSpawnType(SpawnType.SNOW)
                .allowNaturalSpawn();
        chickens.add(snowballChicken);

        LiquidEggRegistryItem waterLiquid = LiquidEggRegistry.findById(0);
        ItemStack waterEgg = waterLiquid != null
                ? LiquidEggItem.createFor(waterLiquid)
                : new ItemStack(ModRegistry.LIQUID_EGG.get());
        ChickensRegistryItem waterChicken = new ChickensRegistryItem(
                206, "WaterChicken", texture("WaterChicken"),
                // Attach the liquid egg id directly so chickens lay the
                // correct fluid variant without additional lookups.
                waterEgg,
                0x000099, 0x8080ff,
                gunpowderChicken, snowballChicken);
        chickens.add(waterChicken);

        LiquidEggRegistryItem lavaLiquid = LiquidEggRegistry.findById(1);
        ItemStack lavaEgg = lavaLiquid != null
                ? LiquidEggItem.createFor(lavaLiquid)
                : new ItemStack(ModRegistry.LIQUID_EGG.get());
        ChickensRegistryItem lavaChicken = new ChickensRegistryItem(
                103, "LavaChicken", texture("LavaChicken"),
                // The lava variant uses the second registered liquid id.
                lavaEgg,
                0xcc3300, 0xffff00,
                coalChicken, quartzChicken).setSpawnType(SpawnType.HELL)
                .allowNaturalSpawn();
        chickens.add(lavaChicken);

        ChickensRegistryItem clayChicken = new ChickensRegistryItem(
                200, "ClayChicken", texture("ClayChicken"),
                new ItemStack(Items.CLAY_BALL),
                0xcccccc, 0xbfbfbf,
                snowballChicken, sandChicken);
        chickens.add(clayChicken);

        ChickensRegistryItem leatherChicken = new ChickensRegistryItem(
                107, "LeatherChicken", texture("LeatherChicken"),
                new ItemStack(Items.LEATHER),
                0xA7A06C, 0x919191,
                stringChicken, brownChicken);
        chickens.add(leatherChicken);

        ChickensRegistryItem netherwartChicken = new ChickensRegistryItem(
                207, "NetherwartChicken", texture("NetherwartChicken"),
                new ItemStack(Items.NETHER_WART),
                0x800000, 0x331a00,
                brownChicken, glowstoneChicken);
        chickens.add(netherwartChicken);

        ChickensRegistryItem diamondChicken = new ChickensRegistryItem(
                301, "DiamondChicken", texture("DiamondChicken"),
                new ItemStack(Items.DIAMOND),
                0x99ccff, 0xe6f2ff,
                glassChicken, goldChicken);
        chickens.add(diamondChicken);

        ChickensRegistryItem blazeChicken = new ChickensRegistryItem(
                302, "BlazeChicken", texture("BlazeChicken"),
                new ItemStack(Items.BLAZE_ROD),
                0xffff66, 0xff3300,
                goldChicken, lavaChicken);
        chickens.add(blazeChicken);

        ChickensRegistryItem slimeChicken = new ChickensRegistryItem(
                205, "SlimeChicken", texture("SlimeChicken"),
                new ItemStack(Items.SLIME_BALL),
                0x009933, 0x99ffbb,
                clayChicken, greenChicken);
        chickens.add(slimeChicken);

        ChickensRegistryItem enderChicken = new ChickensRegistryItem(
                401, "EnderChicken", texture("EnderChicken"),
                new ItemStack(Items.ENDER_PEARL),
                0x001a00, 0x001a33,
                diamondChicken, netherwartChicken);
        chickens.add(enderChicken);

        ChickensRegistryItem ghastChicken = new ChickensRegistryItem(
                402, "GhastChicken", texture("GhastChicken"),
                new ItemStack(Items.GHAST_TEAR),
                0xffffcc, 0xffffff,
                whiteChicken, blazeChicken);
        chickens.add(ghastChicken);

        ChickensRegistryItem emeraldChicken = new ChickensRegistryItem(
                400, "EmeraldChicken", texture("EmeraldChicken"),
                new ItemStack(Items.EMERALD),
                0x00cc00, 0x003300,
                diamondChicken, greenChicken);
        chickens.add(emeraldChicken);

        ChickensRegistryItem magmaChicken = new ChickensRegistryItem(
                403, "MagmaChicken", texture("MagmaChicken"),
                new ItemStack(Items.MAGMA_CREAM),
                0x1a0500, 0x000000,
                slimeChicken, blazeChicken);
        chickens.add(magmaChicken);

        Map<String, ChickensRegistryItem> byName = new HashMap<>();
        for (ChickensRegistryItem chicken : chickens) {
            byName.put(chicken.getEntityName().toLowerCase(Locale.ROOT), chicken);
        }
        ModdedChickens.register(chickens, byName);

        ChickensRegistryItem witherChicken = byName.get("witherchicken");

        // is after the modded chickens since it relies on a chicken registered in there
        ChickensRegistryItem netheriteChicken = new ChickensRegistryItem(
                306, "NetheriteChicken", texture("NetheriteChicken"),
                new ItemStack(Items.ANCIENT_DEBRIS),
                0xd6d4d4, 0x31292a,
                witherChicken, blazeChicken).setLayCoefficient(0.25f);
        chickens.add(netheriteChicken);

        byName.put(netheriteChicken.getEntityName().toLowerCase(Locale.ROOT), netheriteChicken);


        DynamicMaterialChickens.register(chickens, byName);
        DynamicFluidChickens.register(chickens, byName);
        DynamicChemicalChickens.register(chickens, byName);
        DynamicGasChickens.register(chickens, byName);



        return chickens;
    }

    private static ChickensRegistryItem createDyeChicken(DyeColor color, String name) {
        return new ChickensRegistryItem(
                color.getId(),
                name,
                texture(name),
                new ItemStack(DyeColorUtils.itemFor(color)),
                0xf2f2f2, color.getTextColor());
    }

    // Map legacy entity identifiers onto bespoke texture assets supplied with the modern pack.
    private static final Map<String, ResourceLocation> ENTITY_TEXTURE_OVERRIDES = Map.of(
            "redstonefluxchicken",
            ResourceLocation.fromNamespaceAndPath(ChickensMod.MOD_ID, "textures/entity/redstone_crystal_chicken.png"));

    // Mirror the entity override onto the item sprite so inventory icons match the in-world model.
    private static final Map<String, ResourceLocation> ITEM_TEXTURE_OVERRIDES = Map.of(
            "redstonefluxchicken",
            ResourceLocation.fromNamespaceAndPath(ChickensMod.MOD_ID, "textures/item/chicken/redstonecrystalchicken.png"));

    private static ResourceLocation texture(String name) {
        String path = name.toLowerCase(Locale.ROOT);
        ResourceLocation override = ENTITY_TEXTURE_OVERRIDES.get(path);
        if (override != null) {
            return override;
        }
        return ResourceLocation.fromNamespaceAndPath(ChickensMod.MOD_ID, "textures/entity/" + path + ".png");
    }

    private static void applyItemTextureOverride(ChickensRegistryItem chicken) {
        String key = chicken.getEntityName().toLowerCase(Locale.ROOT);
        ResourceLocation override = ITEM_TEXTURE_OVERRIDES.get(key);
        if (override != null) {
            chicken.setItemTexture(override);
        }
    }

    /**
     * Minor helper that converts modern dye colours back into their item
     * representations. Pulling this out keeps {@link #createDyeChicken}
     * readable even with the metadata removal that happened after 1.12.
     */
    private static final class DyeColorUtils {
        private DyeColorUtils() {
        }

        private static net.minecraft.world.item.Item itemFor(DyeColor color) {
            return switch (color) {
                case WHITE -> Items.WHITE_DYE;
                case ORANGE -> Items.ORANGE_DYE;
                case MAGENTA -> Items.MAGENTA_DYE;
                case LIGHT_BLUE -> Items.LIGHT_BLUE_DYE;
                case YELLOW -> Items.YELLOW_DYE;
                case LIME -> Items.LIME_DYE;
                case PINK -> Items.PINK_DYE;
                case GRAY -> Items.GRAY_DYE;
                case LIGHT_GRAY -> Items.LIGHT_GRAY_DYE;
                case CYAN -> Items.CYAN_DYE;
                case PURPLE -> Items.PURPLE_DYE;
                case BLUE -> Items.BLUE_DYE;
                case BROWN -> Items.BROWN_DYE;
                case GREEN -> Items.GREEN_DYE;
                case RED -> Items.RED_DYE;
                case BLACK -> Items.BLACK_DYE;
            };
        }
    }
}
