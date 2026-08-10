package strhercules.chickens.data;

import strhercules.chickens.ChemicalEggRegistry;
import strhercules.chickens.ChemicalEggRegistryItem;
import strhercules.chickens.ChickensMod;
import strhercules.chickens.ChickensRegistry;
import strhercules.chickens.ChickensRegistryItem;
import strhercules.chickens.SpawnType;
import strhercules.chickens.config.ChickensConfigHolder;
import strhercules.chickens.item.ChemicalEggItem;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Creates the chicken that the Avian Dousing Machine targets for each
 * discovered Mekanism chemical, including radioactive chemicals.
 */
final class DynamicChemicalChickens {
    private static final Logger LOGGER = LoggerFactory.getLogger("ChickensDynamicChemical");
    private static final ResourceLocation PLACEHOLDER_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ChickensMod.MOD_ID, "textures/entity/unknownchicken.png");
    private static final int ID_BASE = 4_500_000;
    private static final int ID_SPAN = 500_000;
    // Hydrogen and oxygen are registered as gases and belong to the separate
    // gas-egg path. These three are cheap, non-gaseous Mekanism infusions and
    // are the only chemical chickens the dousing machine creates directly.
    private static final List<String> STARTER_CHEMICALS = List.of(
            "mekanism:bio",
            "mekanism:carbon",
            "mekanism:redstone");
    private static final List<String[]> MATERIAL_PARENT_HINTS = List.of(
            new String[]{"antimatter", "antimatterPelletChicken"},
            new String[]{"polonium", "poloniumPelletChicken"},
            new String[]{"plutonium", "plutoniumPelletChicken"},
            new String[]{"uranium", "uraniumChicken"},
            new String[]{"osmium", "osmiumChicken"},
            new String[]{"copper", "copperchicken"},
            new String[]{"gold", "GoldChicken"},
            new String[]{"iron", "IronChicken"},
            new String[]{"tin", "tinchicken"},
            new String[]{"lead", "leadchicken"},
            new String[]{"diamond", "DiamondChicken"},
            new String[]{"redstone", "RedstoneChicken"},
            new String[]{"carbon", "CoalChicken"},
            new String[]{"sulfur", "sulfurchicken"},
            new String[]{"fluor", "fluoriteChicken"},
            new String[]{"fungi", "NetherwartChicken"});
    private static final List<String> FALLBACK_PARENT_NAMES = List.of(
            "mekanismRefinedObsidianChicken", "mekanismOsmiumChicken", "mekanismGoldChicken",
            "mekanismDiamondChicken", "mekanismCarbonChicken", "mekanismBioChicken",
            "mekanismRedstoneChicken", "DiamondChicken", "WaterChicken", "LavaChicken");
    private static final Map<String, String[]> CHEMICAL_LINEAGE = createChemicalLineage();

    private DynamicChemicalChickens() {
    }

    static void register(List<ChickensRegistryItem> chickens, Map<String, ChickensRegistryItem> byName) {
        if (ChickensConfigHolder.get().isChemicalChickensEnabled()) {
            attemptRegistration(chickens, byName, false);
        }
    }

    static void refresh() {
        if (ChickensConfigHolder.get().isChemicalChickensEnabled()) {
            attemptRegistration(null, buildRegistryIndex(), true);
        }
    }

    private static void attemptRegistration(@Nullable List<ChickensRegistryItem> collector,
            Map<String, ChickensRegistryItem> byName, boolean registerImmediately) {
        Set<Integer> usedIds = collectUsedIds(collector, byName);
        int created = 0;
        Set<ResourceLocation> registeredChemicals = new HashSet<>();
        Map<ResourceLocation, ChickensRegistryItem> chemicalChickens = new HashMap<>();
        for (ChemicalEggRegistryItem entry : ChemicalEggRegistry.getAll()) {
            if (!registeredChemicals.add(entry.getChemicalId())) {
                continue;
            }

            ItemStack layStack = ChemicalEggItem.createFor(entry);
            if (layStack.isEmpty()) {
                continue;
            }

            ChickensRegistryItem existing = findByLayItem(byName.values(), layStack);
            if (existing != null) {
                chemicalChickens.put(entry.getChemicalId(), existing);
                continue;
            }
            if (alreadyRepresents(byName.values(), layStack)) {
                continue;
            }

            String entityName = buildEntityName(entry.getChemicalId());
            String nameKey = entityName.toLowerCase(Locale.ROOT);
            if (byName.containsKey(nameKey)) {
                continue;
            }

            int primaryColor = entry.getEggColor();
            ChickensRegistryItem chicken = new ChickensRegistryItem(
                    allocateId(entry.getChemicalId(), usedIds),
                    entityName,
                    PLACEHOLDER_TEXTURE,
                    layStack,
                    primaryColor,
                    accentColor(primaryColor));
            chicken.setGeneratedTexture(true);
            chicken.setSpawnType(SpawnType.NONE);
            chicken.setDisplayName(buildDisplayName(entry));
            chicken.setNoParents();
            chicken.setDousingAllowed(isStarter(entry.getChemicalId()));

            byName.put(nameKey, chicken);
            chemicalChickens.put(entry.getChemicalId(), chicken);
            if (collector != null) {
                collector.add(chicken);
            }
            if (registerImmediately) {
                ChickensRegistry.register(chicken);
            }
            created++;
        }

        applyChemicalProgression(chemicalChickens, byName);

        if (created > 0) {
            LOGGER.info("Registered {} dynamic chemical chickens", created);
        }
    }

    private static void applyChemicalProgression(Map<ResourceLocation, ChickensRegistryItem> chemicalChickens,
            Map<String, ChickensRegistryItem> byName) {
        int fallbackCount = 0;
        for (Map.Entry<ResourceLocation, ChickensRegistryItem> entry : chemicalChickens.entrySet()) {
            ResourceLocation chemicalId = entry.getKey();
            ChickensRegistryItem chicken = entry.getValue();
            if (isStarter(chemicalId)) {
                chicken.setNoParents();
                chicken.setDousingAllowed(true);
                continue;
            }

            String[] configuredParents = CHEMICAL_LINEAGE.get(chemicalId.toString());
            ChickensRegistryItem parent1 = configuredParents == null
                    ? null : resolveParent(byName, configuredParents[0]);
            ChickensRegistryItem parent2 = configuredParents == null
                    ? null : resolveParent(byName, configuredParents[1]);
            if (!validParents(chicken, parent1, parent2)) {
                fallbackCount++;
                parent1 = findFallbackMaterialParent(chemicalId, chicken, byName);
                parent2 = findFallbackProgressionParent(parent1, chicken, byName);
            }

            if (!validParents(chicken, parent1, parent2)) {
                parent1 = findFallbackProgressionParent(null, chicken, byName);
                parent2 = findFallbackProgressionParent(parent1, chicken, byName);
            }

            if (validParents(chicken, parent1, parent2)) {
                chicken.setParentsNew(parent1, parent2);
            } else {
                LOGGER.warn("Unable to assign parents to chemical chicken {}; no valid breeding pair was available",
                        chicken.getEntityName());
                chicken.setNoParents();
            }
            chicken.setDousingAllowed(false);
        }
        if (fallbackCount > 0) {
            LOGGER.info("Used generic progression parents for {} chemical chickens without a specific process rule",
                    fallbackCount);
        }
    }

    private static boolean isStarter(ResourceLocation chemicalId) {
        return chemicalId != null && STARTER_CHEMICALS.contains(chemicalId.toString());
    }

    @Nullable
    private static ChickensRegistryItem findByLayItem(Iterable<ChickensRegistryItem> chickens, ItemStack layStack) {
        for (ChickensRegistryItem chicken : chickens) {
            if (ItemStack.isSameItemSameComponents(chicken.createLayItem(), layStack)) {
                return chicken;
            }
        }
        return null;
    }

    @Nullable
    private static ChickensRegistryItem findFallbackMaterialParent(ResourceLocation chemicalId,
            ChickensRegistryItem child, Map<String, ChickensRegistryItem> byName) {
        String path = chemicalId.getPath();
        for (String[] hint : MATERIAL_PARENT_HINTS) {
            if (path.contains(hint[0])) {
                ChickensRegistryItem candidate = resolveParent(byName, hint[1]);
                if (candidate != null && candidate != child) {
                    return candidate;
                }
            }
        }
        return findFallbackProgressionParent(null, child, byName);
    }

    @Nullable
    private static ChickensRegistryItem findFallbackProgressionParent(
            @Nullable ChickensRegistryItem excluded, @Nullable ChickensRegistryItem child,
            Map<String, ChickensRegistryItem> byName) {
        for (String name : FALLBACK_PARENT_NAMES) {
            ChickensRegistryItem candidate = resolveParent(byName, name);
            if (candidate != null && candidate != excluded && candidate != child) {
                return candidate;
            }
        }
        return null;
    }

    private static boolean validParents(ChickensRegistryItem child,
            @Nullable ChickensRegistryItem parent1, @Nullable ChickensRegistryItem parent2) {
        return parent1 != null && parent2 != null
                && parent1 != child && parent2 != child && parent1 != parent2
                && !containsParent(parent1, child, new HashSet<>())
                && !containsParent(parent2, child, new HashSet<>());
    }

    private static boolean containsParent(@Nullable ChickensRegistryItem current,
            ChickensRegistryItem target, Set<ChickensRegistryItem> visited) {
        if (current == null || !visited.add(current)) {
            return false;
        }
        return current == target
                || containsParent(current.getParent1(), target, visited)
                || containsParent(current.getParent2(), target, visited);
    }

    @Nullable
    private static ChickensRegistryItem resolveParent(Map<String, ChickensRegistryItem> byName, @Nullable String name) {
        return name == null ? null : byName.get(name.toLowerCase(Locale.ROOT));
    }

    private static Map<String, String[]> createChemicalLineage() {
        Map<String, String[]> rules = new HashMap<>();

        // Mekanism's non-gaseous processing chemicals. Water, lava, brine
        // and sulfuric acid already have fluid chickens; they are process
        // parents rather than direct chemical starters.
        rule(rules, "mekanism:sulfuric_acid", "sulfurchicken", "WaterChicken");
        rule(rules, "mekanism:hydrofluoric_acid", "sulfuricAcidChicken", "fluoriteChicken");
        rule(rules, "mekanism:sodium", "brineChicken", "WaterChicken");
        chemicalRule(rules, "mekanism:lithium", "mekanism:sodium", "WaterChicken");

        // Foundational infusions and pigments. Only bio, carbon and redstone
        // are dousing starters; material infusions must be bred from the
        // matching resource chicken and an existing chemical branch.
        chemicalRule(rules, "mekanism:tin", "mekanism:carbon", "tinchicken");
        chemicalRule(rules, "mekanism:gold", "mekanism:redstone", "GoldChicken");
        chemicalRule(rules, "mekanism:osmium", "mekanism:redstone", "osmiumChicken");
        chemicalRule(rules, "mekanism:diamond", "mekanism:carbon", "DiamondChicken");
        chemicalRule(rules, "mekanism:refined_obsidian", "mekanism:diamond", "obsidianChicken");
        chemicalRule(rules, "mekanism:fungi", "mekanism:bio", "NetherwartChicken");
        for (String[] pigment : List.of(
                new String[]{"black", "BlackChicken"},
                new String[]{"dark_blue", "BlueChicken"},
                new String[]{"dark_green", "GreenChicken"},
                new String[]{"dark_aqua", "CyanChicken"},
                new String[]{"dark_red", "RedChicken"},
                new String[]{"purple", "PurpleChicken"},
                new String[]{"orange", "OrangeChicken"},
                new String[]{"gray", "GrayChicken"},
                new String[]{"light_gray", "SilverDyeChicken"},
                new String[]{"indigo", "LightBlueChicken"},
                new String[]{"bright_green", "LimeChicken"},
                new String[]{"aqua", "CyanChicken"},
                new String[]{"red", "RedChicken"},
                new String[]{"magenta", "MagentaChicken"},
                new String[]{"yellow", "YellowChicken"},
                new String[]{"white", "WhiteChicken"},
                new String[]{"brown", "BrownChicken"},
                new String[]{"bright_pink", "PinkChicken"})) {
            chemicalRule(rules, "mekanism:" + pigment[0], "mekanism:bio", pigment[1]);
        }

        // Ore dissolution and washing: the existing sulfuric-acid fluid
        // chicken unlocks dirty slurry, then water washes it clean.
        for (String ore : List.of("copper", "gold", "iron", "lead", "osmium", "tin", "uranium")) {
            String oreChicken = switch (ore) {
                case "copper" -> "copperchicken";
                case "gold" -> "GoldChicken";
                case "iron" -> "IronChicken";
                case "lead" -> "leadchicken";
                case "osmium" -> "osmiumChicken";
                case "tin" -> "tinchicken";
                case "uranium" -> "uraniumChicken";
                default -> "SmartChicken";
            };
            rule(rules, "mekanism:dirty_" + ore, "sulfuricAcidChicken", oreChicken);
            chemicalRule(rules, "mekanism:clean_" + ore, "mekanism:dirty_" + ore, "WaterChicken");
        }

        // Mekanism Elements atmospheric and industrial chemistry. The addon
        // registers these in the shared chemical registry, so they follow the
        // same breeding tree even though their names describe gases.
        rule(rules, "mekanismelements:nitrogen", "WaterChicken", "LavaChicken");
        chemicalRule(rules, "mekanismelements:ammonia", "mekanismelements:nitrogen", "WaterChicken");
        chemicalRule(rules, "mekanismelements:nitric_oxide", "mekanismelements:nitrogen", "LavaChicken");
        chemicalRule(rules, "mekanismelements:nitrogen_dioxide", "mekanismelements:nitric_oxide", "LavaChicken");
        chemicalRule(rules, "mekanismelements:nitric_acid", "mekanismelements:nitrogen_dioxide", "WaterChicken");
        chemicalPairRule(rules, "mekanismelements:ammonium_nitrate", "mekanismelements:ammonia", "mekanismelements:nitric_acid");
        chemicalRule(rules, "mekanismelements:ammonium_nitrate_solution", "mekanismelements:ammonium_nitrate", "WaterChicken");
        chemicalPairRule(rules, "mekanismelements:hydrogen_cyanide", "mekanism:carbon", "mekanismelements:ammonia");
        chemicalRule(rules, "mekanismelements:methane", "mekanism:carbon", "WaterChicken");
        rule(rules, "mekanismelements:potassium_chloride", "brineChicken", "WaterChicken");
        chemicalRule(rules, "mekanismelements:potassium_hydroxide", "mekanismelements:potassium_chloride", "WaterChicken");
        chemicalRule(rules, "mekanismelements:potassium_iodide", "mekanismelements:potassium_chloride", "mekanismelementsIodineChicken");
        chemicalRule(rules, "mekanismelements:potassium_cyanide", "mekanismelements:hydrogen_cyanide", "mekanismelementsPotassiumHydroxideChicken");
        rule(rules, "mekanismelements:bromine", "brineChicken", "WaterChicken");
        chemicalRule(rules, "mekanismelements:iodine", "mekanismelements:bromine", "brineChicken");
        rule(rules, "mekanismelements:seawater", "brineChicken", "WaterChicken");
        chemicalRule(rules, "mekanismelements:compressed_air", "mekanismelements:nitrogen", "LavaChicken");
        rule(rules, "mekanismelements:aqua_regia", "brineChicken", "mekanismelementsNitricAcidChicken");
        rule(rules, "mekanismelements:netherite_acid", "mekanismHydrofluoricAcidChicken", "sulfuricAcidChicken");
        rule(rules, "mekanismelements:helium", "WaterChicken", "LavaChicken");
        chemicalRule(rules, "mekanismelements:xenon", "mekanismelements:helium", "LavaChicken");
        chemicalRule(rules, "mekanismelements:superheated_helium", "mekanismelements:helium", "LavaChicken");

        // Nuclear processing and fusion fuel are deliberately at the far end of the line.
        rule(rules, "mekanism:uranium_oxide", "uraniumChicken", "LavaChicken");
        chemicalRule(rules, "mekanism:uranium_hexafluoride", "mekanism:uranium_oxide", "fluoriteChicken");
        chemicalRule(rules, "mekanism:fissile_fuel", "mekanism:uranium_hexafluoride", "uraniumChicken");
        chemicalRule(rules, "mekanism:nuclear_waste", "mekanism:fissile_fuel", "LavaChicken");
        chemicalRule(rules, "mekanism:spent_nuclear_waste", "mekanism:nuclear_waste", "sulfuricAcidChicken");
        chemicalRule(rules, "mekanismelements:dissolved_spent_nuclear_waste", "mekanism:spent_nuclear_waste", "sulfuricAcidChicken");
        chemicalRule(rules, "mekanism:plutonium", "mekanism:spent_nuclear_waste", "uraniumChicken");
        chemicalRule(rules, "mekanism:polonium", "mekanism:nuclear_waste", "LavaChicken");
        chemicalPairRule(rules, "mekanism:antimatter", "mekanism:polonium", "mekanism:plutonium");
        chemicalPairRule(rules, "mekanismelements:antimatter", "mekanism:antimatter", "mekanismelements:californium");
        chemicalRule(rules, "mekanismelements:americium", "mekanism:plutonium", "mekanismCarbonChicken");
        chemicalRule(rules, "mekanismelements:curium", "mekanismelements:americium", "mekanismPlutoniumChicken");
        chemicalRule(rules, "mekanismelements:californium", "mekanismelements:curium", "mekanismAntimatterChicken");
        chemicalRule(rules, "mekanismelements:beryllium", "mekanism:carbon", "mekanismOsmiumChicken");
        chemicalRule(rules, "mekanismelements:strontium", "mekanismelements:beryllium", "LavaChicken");
        chemicalRule(rules, "mekanismelements:yttrium", "mekanismelements:strontium", "mekanismCarbonChicken");
        // Evolved Mekanism's infusions are deliberately late branches rather
        // than additional dousing targets.
        rule(rules, "evolvedmekanism:uranium", "mekanismOsmiumChicken", "uraniumChicken");
        rule(rules, "evolvedmekanism:better_gold", "mekanismGoldChicken", "GoldChicken");
        rule(rules, "evolvedmekanism:plaslitherite", "mekanismRefinedObsidianChicken", "platinumchicken");

        return rules;
    }

    private static void rule(Map<String, String[]> rules, String child, String parent1, String parent2) {
        rules.put(child, new String[]{parent1, parent2});
    }

    private static void chemicalRule(Map<String, String[]> rules, String child,
            String chemicalParent, String otherParent) {
        rule(rules, child, buildEntityName(ResourceLocation.parse(chemicalParent)), otherParent);
    }

    private static void chemicalPairRule(Map<String, String[]> rules, String child,
            String parent1, String parent2) {
        rule(rules, child,
                buildEntityName(ResourceLocation.parse(parent1)),
                buildEntityName(ResourceLocation.parse(parent2)));
    }

    private static Map<String, ChickensRegistryItem> buildRegistryIndex() {
        Map<String, ChickensRegistryItem> index = new HashMap<>();
        for (ChickensRegistryItem chicken : ChickensRegistry.getItems()) {
            index.put(chicken.getEntityName().toLowerCase(Locale.ROOT), chicken);
        }
        for (ChickensRegistryItem chicken : ChickensRegistry.getDisabledItems()) {
            index.putIfAbsent(chicken.getEntityName().toLowerCase(Locale.ROOT), chicken);
        }
        return index;
    }

    private static Set<Integer> collectUsedIds(@Nullable List<ChickensRegistryItem> collector,
            Map<String, ChickensRegistryItem> byName) {
        Set<Integer> ids = new HashSet<>();
        if (collector != null) {
            for (ChickensRegistryItem chicken : collector) {
                ids.add(chicken.getId());
            }
        } else {
            for (ChickensRegistryItem chicken : ChickensRegistry.getItems()) {
                ids.add(chicken.getId());
            }
            for (ChickensRegistryItem chicken : ChickensRegistry.getDisabledItems()) {
                ids.add(chicken.getId());
            }
        }
        for (ChickensRegistryItem chicken : byName.values()) {
            ids.add(chicken.getId());
        }
        return ids;
    }

    private static boolean alreadyRepresents(Iterable<ChickensRegistryItem> chickens, ItemStack layStack) {
        for (ChickensRegistryItem chicken : chickens) {
            if (ItemStack.isSameItemSameComponents(chicken.createLayItem(), layStack)
                    || ItemStack.isSameItemSameComponents(chicken.createDropItem(), layStack)) {
                return true;
            }
        }
        return false;
    }

    private static Component buildDisplayName(ChemicalEggRegistryItem entry) {
        return entry.getDisplayName().copy().append(Component.literal(" Chicken"));
    }

    private static int allocateId(ResourceLocation chemicalId, Set<Integer> usedIds) {
        int candidate = ID_BASE + Math.floorMod(chemicalId.hashCode(), ID_SPAN);
        while (usedIds.contains(candidate)) {
            candidate++;
        }
        usedIds.add(candidate);
        return candidate;
    }

    private static String buildEntityName(ResourceLocation chemicalId) {
        String combined = chemicalId.getNamespace() + "_" + chemicalId.getPath();
        String[] parts = combined.split("[^a-z0-9]+");
        if (parts.length == 0) {
            return "chemicalChicken";
        }
        StringBuilder builder = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].isEmpty()) {
                continue;
            }
            builder.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i].substring(1));
        }
        return builder.append("Chicken").toString();
    }

    private static int accentColor(int base) {
        int r = Math.min(0xFF, ((base >> 16) & 0xFF) + 0x30);
        int g = Math.min(0xFF, ((base >> 8) & 0xFF) + 0x30);
        int b = Math.min(0xFF, (base & 0xFF) + 0x30);
        return (r << 16) | (g << 8) | b;
    }
}
