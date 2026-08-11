package strhercules.chickens.data;

import strhercules.chickens.ChickensMod;
import strhercules.chickens.ChickensRegistry;
import strhercules.chickens.ChickensRegistryItem;
import strhercules.chickens.item.ChickenItemHelper;
import strhercules.chickens.LiquidEggRegistry;
import strhercules.chickens.LiquidEggRegistryItem;
import strhercules.chickens.SpawnType;
import strhercules.chickens.config.ChickensConfigHolder;
import strhercules.chickens.item.LiquidEggItem;
import strhercules.chickens.registry.ModRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
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

    private static final Set<String> STARTER_FLUIDS = Set.of(
            "minecraft:water", "minecraft:lava");
    private static final List<String> FLUID_PREFIXES = List.of(
            "molten_", "liquid_", "fluid_", "plasma_", "solution_", "slurry_");
    private static final List<String> PROCESS_PREFIXES = List.of(
            "steam_cracked_", "high_pressure_", "pressurized_", "distilled_", "purified_",
            "concentrated_", "refined_", "raw_", "clean_", "dirty_", "cracked_", "still_",
            "depleted_", "spent_", "boosted_", "empowered_", "unrefined_");
    private static final List<String[]> MATERIAL_PARENT_ALIASES = List.of(
            new String[]{"refined_obsidian", "ObsidianChicken"},
            new String[]{"refined_glowstone", "GlowstoneChicken"},
            new String[]{"refined_redstone", "RedstoneChicken"},
            new String[]{"infused_iron", "IronChicken"},
            new String[]{"pig_iron", "IronChicken"},
            new String[]{"compressed_iron", "IronChicken"},
            new String[]{"black_iron", "IronChicken"},
            new String[]{"better_gold", "GoldChicken"},
            new String[]{"rose_gold", "GoldChicken"},
            new String[]{"redstone_ingot", "RedstoneChicken"},
            new String[]{"enhanced_redstone_ingot", "RedstoneChicken"},
            new String[]{"silicon_bronze", "bronzechicken"});
    private static final List<String> RESOURCE_FALLBACKS = List.of(
            "IronChicken", "CoalChicken", "QuartzChicken", "LogChicken",
            "SulfurChicken", "RedstoneChicken", "SandChicken", "DiamondChicken");

    static void register(List<ChickensRegistryItem> chickens, Map<String, ChickensRegistryItem> byName) {
        if (ChickensConfigHolder.get().isAutomaticFluidChickensEnabled()) {
            attemptRegistration(chickens, byName, false);
        }
    }

    static void refresh() {
        if (ChickensConfigHolder.get().isAutomaticFluidChickensEnabled()) {
            attemptRegistration(null, buildRegistryIndex(), true);
        }
    }

    private static void attemptRegistration(@Nullable List<ChickensRegistryItem> collector,
            Map<String, ChickensRegistryItem> byName, boolean registerImmediately) {
        Set<Integer> usedIds = collectUsedIds(collector, byName);
        Map<ResourceLocation, ChickensRegistryItem> generatedFluidChickens = new HashMap<>();
        List<FluidCandidate> candidates = new ArrayList<>();
        List<AliasCandidate> aliases = new ArrayList<>();
        collectCandidates(candidates, aliases);
        int created = 0;
        int retired = 0;

        for (FluidCandidate candidate : candidates) {
            ResourceLocation fluidId = candidate.fluidId();
            ItemStack layStack = LiquidEggItem.createFor(candidate.entry());
            if (layStack.isEmpty()) {
                continue;
            }

            ChickensRegistryItem existing = findExistingFluidChicken(byName.values(), fluidId);
            if (existing != null) {
                continue;
            }

            String entityName = buildEntityName(fluidId);
            String nameKey = entityName.toLowerCase(Locale.ROOT);
            if (byName.containsKey(nameKey)) {
                continue;
            }

            int primaryColor = candidate.entry().getEggColor();
            ChickensRegistryItem chicken = new ChickensRegistryItem(
                    allocateId(fluidId, usedIds),
                    entityName,
                    PLACEHOLDER_TEXTURE,
                    layStack,
                    primaryColor,
                    accentColor(primaryColor));
            chicken.setGeneratedTexture(true);
            chicken.setSpawnType(SpawnType.NONE);
            chicken.setDisplayName(buildDisplayName(candidate.entry()));
            chicken.setNoParents();
            chicken.setDousingAllowed(isStarter(fluidId));

            byName.put(nameKey, chicken);
            generatedFluidChickens.put(fluidId, chicken);
            if (collector != null) {
                collector.add(chicken);
            }
            if (registerImmediately) {
                ChickensRegistry.register(chicken);
            }
            created++;
        }

        for (AliasCandidate alias : aliases) {
            ChickensRegistryItem canonical = findExistingFluidChicken(byName.values(), alias.canonicalId());
            if (canonical != null && registerRetiredAlias(collector, byName, usedIds, alias, canonical,
                    registerImmediately)) {
                retired++;
            }
        }

        applyFluidProgression(candidates, generatedFluidChickens, byName);
        if (created > 0 || retired > 0) {
            LOGGER.info("Registered {} dynamic fluid chickens and retained {} hidden fluid aliases",
                    created, retired);
        }
    }

    private static void collectCandidates(List<FluidCandidate> candidates, List<AliasCandidate> aliases) {
        List<LiquidEggRegistryItem> entries = new ArrayList<>(LiquidEggRegistry.getAll());
        entries.sort(Comparator.comparing(DynamicFluidChickens::fluidKey));

        Map<ResourceLocation, LiquidEggRegistryItem> entriesByFluid = new HashMap<>();
        for (LiquidEggRegistryItem entry : entries) {
            ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(entry.getFluid());
            if (fluidId != null) {
                entriesByFluid.putIfAbsent(fluidId, entry);
            }
        }

        Set<ResourceLocation> seen = new HashSet<>();
        for (LiquidEggRegistryItem entry : entries) {
            ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(entry.getFluid());
            if (fluidId == null || !seen.add(fluidId)) {
                continue;
            }

            ResourceLocation canonicalId = FluidChickenAliases.canonicalId(fluidId);
            if (!canonicalId.equals(fluidId) && entriesByFluid.containsKey(canonicalId)) {
                aliases.add(new AliasCandidate(fluidId, entry, canonicalId));
                continue;
            }
            if (!canonicalId.equals(fluidId)) {
                LOGGER.warn("Fluid chicken alias {} -> {} has no registered canonical egg; retaining the source fluid",
                        fluidId, canonicalId);
            }
            candidates.add(new FluidCandidate(fluidId, entry));
        }
    }

    private static String fluidKey(LiquidEggRegistryItem entry) {
        ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(entry.getFluid());
        return fluidId == null ? "" : fluidId.toString();
    }

    private static void applyFluidProgression(List<FluidCandidate> candidates,
            Map<ResourceLocation, ChickensRegistryItem> newlyGeneratedFluidChickens,
            Map<String, ChickensRegistryItem> byName) {
        Map<ResourceLocation, ChickensRegistryItem> fluidById = new HashMap<>();
        Map<ResourceLocation, ChickensRegistryItem> dynamicFluidById = new HashMap<>();
        for (FluidCandidate candidate : candidates) {
            ResourceLocation fluidId = candidate.fluidId();
            ResourceLocation canonicalId = FluidChickenAliases.canonicalId(fluidId);
            ChickensRegistryItem chicken = newlyGeneratedFluidChickens.get(fluidId);
            if (chicken == null) {
                chicken = findExistingFluidChicken(byName.values(), canonicalId);
            }
            if (chicken == null) {
                continue;
            }
            fluidById.putIfAbsent(canonicalId, chicken);
            if (chicken.hasGeneratedTexture()) {
                dynamicFluidById.putIfAbsent(canonicalId, chicken);
            }
        }

        Map<String, List<FluidReference>> fluidByPath = indexFluidPaths(fluidById);
        for (Map.Entry<ResourceLocation, ChickensRegistryItem> entry : dynamicFluidById.entrySet()) {
            ResourceLocation fluidId = entry.getKey();
            ChickensRegistryItem chicken = entry.getValue();
            if (isStarter(fluidId)) {
                chicken.setNoParents();
                chicken.setDousingAllowed(true);
                continue;
            }

            ParentPair parents = resolveLineage(fluidId, chicken, fluidById, fluidByPath, byName);
            if (validParents(chicken, parents.parent1(), parents.parent2())) {
                chicken.setParentsNew(parents.parent1(), parents.parent2());
                chicken.setDousingAllowed(false);
            } else {
                LOGGER.warn("Unable to assign parents to dynamic fluid chicken {}; no valid breeding pair was available",
                        chicken.getEntityName());
                chicken.setNoParents();
                chicken.setDousingAllowed(false);
            }
        }
    }

    private static boolean isStarter(ResourceLocation fluidId) {
        return fluidId != null && STARTER_FLUIDS.contains(fluidId.toString());
    }

    private static ParentPair resolveLineage(ResourceLocation fluidId, ChickensRegistryItem child,
            Map<ResourceLocation, ChickensRegistryItem> fluidById,
            Map<String, List<FluidReference>> fluidByPath,
            Map<String, ChickensRegistryItem> byName) {
        String path = fluidId.getPath().toLowerCase(Locale.ROOT);

        if (isMoltenFluid(path)) {
            ParentPair compound = resolveCompoundMaterialLineage(path, child, byName);
            if (validParents(child, compound.parent1(), compound.parent2())) {
                return compound;
            }
            return new ParentPair(
                    firstResourceParent(findMaterialParent(fluidId, byName),
                            findResourceAnchor(fluidId, child, byName), byName),
                    resolveParent(byName, "LavaChicken"));
        }

        ParentPair named = resolveNamedLineage(path, fluidById, fluidByPath, byName);
        if (named != null && validParents(child, named.parent1(), named.parent2())) {
            return named;
        }

        if (path.startsWith("dirty_")) {
            ChickensRegistryItem material = firstResourceParent(findMaterialParent(fluidId, byName),
                    findResourceAnchor(fluidId, child, byName), byName);
            ChickensRegistryItem acid = findFluidByAnyPath(fluidId,
                    List.of("sulfuric_acid", "sulfuricacid"), fluidById, fluidByPath);
            ParentPair dirty = new ParentPair(acid, material);
            if (validParents(child, dirty.parent1(), dirty.parent2())) {
                return dirty;
            }
        }

        if (path.contains("sulfuric") && !path.equals("sulfuric_acid")) {
            ChickensRegistryItem acid = findFluidByAnyPath(fluidId,
                    List.of("sulfuric_acid", "sulfuricacid"), fluidById, fluidByPath);
            ChickensRegistryItem material = firstResourceParent(findMaterialParent(fluidId, byName),
                    findResourceAnchor(fluidId, child, byName), byName);
            ParentPair sulfuric = new ParentPair(acid, material);
            if (validParents(child, sulfuric.parent1(), sulfuric.parent2())) {
                return sulfuric;
            }
        }

        ChickensRegistryItem predecessor = findFluidPredecessor(fluidId, fluidById, fluidByPath);
        if (predecessor != null) {
            ChickensRegistryItem catalyst = findFamilyCatalyst(path, fluidId, child, fluidById, fluidByPath, byName);
            ParentPair process = new ParentPair(predecessor, catalyst);
            if (validParents(child, process.parent1(), process.parent2())) {
                return process;
            }
        }

        ChickensRegistryItem material = firstResourceParent(findMaterialParent(fluidId, byName),
                findResourceAnchor(fluidId, child, byName), byName);
        ChickensRegistryItem process = findProcessParent(path, child, byName);
        if (validParents(child, material, process)) {
            return new ParentPair(material, process);
        }

        ChickensRegistryItem fallback = findResourceAnchor(fluidId, child, byName);
        ChickensRegistryItem alternate = findProcessParent(path, child, byName);
        return new ParentPair(fallback, alternate);
    }

    private static boolean isMoltenFluid(String path) {
        return path.contains("molten") || path.contains("plasma");
    }

    private static ParentPair resolveNamedLineage(String path,
            Map<ResourceLocation, ChickensRegistryItem> fluidById,
            Map<String, List<FluidReference>> fluidByPath,
            Map<String, ChickensRegistryItem> byName) {
        if (path.equals("steam")) {
            return new ParentPair(resolveParent(byName, "WaterChicken"),
                    resolveParent(byName, "LavaChicken"));
        }
        if (path.equals("sulfuric_acid")) {
            return new ParentPair(resolveParent(byName, "SulfurChicken"),
                    resolveParent(byName, "WaterChicken"));
        }
        if (path.equals("oil")) {
            return new ParentPair(resolveParent(byName, "CoalChicken"),
                    resolveParent(byName, "WaterChicken"));
        }
        if (path.equals("ethanol")) {
            return new ParentPair(
                    findFluidByAnyPath(null, List.of("plant_oil", "plantoil"), fluidById, fluidByPath),
                    resolveParent(byName, "NetherwartChicken"));
        }
        if (path.equals("bioethanol")) {
            return new ParentPair(
                    findFluidByAnyPath(null, List.of("ethanol"), fluidById, fluidByPath),
                    resolveParent(byName, "GreenChicken"));
        }
        if (path.equals("biodiesel")) {
            return new ParentPair(
                    findFluidByAnyPath(null, List.of("ethanol"), fluidById, fluidByPath),
                    findFluidByAnyPath(null, List.of("plant_oil", "plantoil"), fluidById, fluidByPath));
        }
        if (path.equals("fuel")) {
            return new ParentPair(
                    findFluidByAnyPath(null, List.of("oil"), fluidById, fluidByPath),
                    resolveParent(byName, "BlazeChicken"));
        }
        return null;
    }

    @Nullable
    private static ChickensRegistryItem findMaterialParent(ResourceLocation fluidId,
            Map<String, ChickensRegistryItem> byName) {
        String material = materialName(fluidId.getPath());
        if (material.isEmpty()) {
            return null;
        }

        for (String[] alias : MATERIAL_PARENT_ALIASES) {
            if (material.equals(alias[0])) {
                ChickensRegistryItem parent = resolveParent(byName, alias[1]);
                if (isResourceParent(parent)) {
                    return parent;
                }
            }
        }

        ChickensRegistryItem direct = resolveParent(byName, toChickenName(material));
        if (isResourceParent(direct)) {
            return direct;
        }

        for (String token : material.split("_")) {
            if (token.length() < 3 || PROCESS_PREFIXES.contains(token + "_")) {
                continue;
            }
            ChickensRegistryItem parent = resolveParent(byName, toChickenName(token));
            if (isResourceParent(parent)) {
                return parent;
            }
        }
        return null;
    }

    private static String materialName(String path) {
        String value = path.toLowerCase(Locale.ROOT);
        boolean changed;
        do {
            changed = false;
            for (String prefix : FLUID_PREFIXES) {
                if (value.startsWith(prefix) && value.length() > prefix.length()) {
                    value = value.substring(prefix.length());
                    changed = true;
                    break;
                }
            }
            if (changed) {
                continue;
            }
            for (String prefix : PROCESS_PREFIXES) {
                if (value.startsWith(prefix) && value.length() > prefix.length()) {
                    value = value.substring(prefix.length());
                    changed = true;
                    break;
                }
            }
        } while (changed);

        for (String suffix : List.of("_molten", "_liquid", "_fluid", "_still", "_solution", "_slurry")) {
            if (value.endsWith(suffix) && value.length() > suffix.length()) {
                value = value.substring(0, value.length() - suffix.length());
                break;
            }
        }
        return value;
    }

    private static ParentPair resolveCompoundMaterialLineage(String path, ChickensRegistryItem child,
            Map<String, ChickensRegistryItem> byName) {
        List<String[]> rules = List.of(
                new String[]{"copper_alloy", "CopperChicken", "IronChicken"},
                new String[]{"silicon_bronze", "siliconchicken", "bronzechicken"},
                new String[]{"conductive_alloy", "RedstoneChicken", "IronChicken"},
                new String[]{"pulsating_alloy", "EnderChicken", "IronChicken"},
                new String[]{"end_steel", "EnderChicken", "IronChicken"},
                new String[]{"knightslime", "SlimeChicken", "IronChicken"},
                new String[]{"queenslime", "SlimeChicken", "GoldChicken"},
                new String[]{"slimesteel", "SlimeChicken", "IronChicken"},
                new String[]{"soulsteel", "soulSandChicken", "IronChicken"},
                new String[]{"enhanced_redstone_ingot", "RedstoneChicken", "GlowstoneChicken"});
        for (String[] rule : rules) {
            if (!path.contains(rule[0])) {
                continue;
            }
            ChickensRegistryItem parent1 = findResourceParent(byName, child, rule[1]);
            ChickensRegistryItem parent2 = findResourceParent(byName, child, rule[2]);
            return new ParentPair(parent1, parent2);
        }
        return new ParentPair(null, null);
    }

    @Nullable
    private static ChickensRegistryItem findFluidPredecessor(ResourceLocation fluidId,
            Map<ResourceLocation, ChickensRegistryItem> fluidById,
            Map<String, List<FluidReference>> fluidByPath) {
        for (String path : predecessorPaths(fluidId.getPath())) {
            ChickensRegistryItem predecessor = findFluidByPath(fluidId, path, fluidById, fluidByPath);
            if (predecessor != null) {
                return predecessor;
            }
        }
        return null;
    }

    private static List<String> predecessorPaths(String path) {
        String value = path.toLowerCase(Locale.ROOT);
        Set<String> paths = new LinkedHashSet<>();
        addPathVariant(paths, value, "fluid_");
        addPathVariant(paths, value, "still_");
        addPathVariant(paths, value, "steam_cracked_");
        addPathVariant(paths, value, "high_pressure_");
        addPathVariant(paths, value, "pressurized_");
        addPathVariant(paths, value, "distilled_");
        addPathVariant(paths, value, "purified_");
        addPathVariant(paths, value, "concentrated_");
        addPathVariant(paths, value, "refined_");
        addPathVariant(paths, value, "raw_");
        addPathVariant(paths, value, "clean_");
        addPathVariant(paths, value, "cracked_");
        addPathVariant(paths, value, "depleted_");
        addPathVariant(paths, value, "spent_");
        addPathVariant(paths, value, "boosted_");
        addPathVariant(paths, value, "empowered_");
        addPathVariant(paths, value, "unrefined_");

        if (value.endsWith("_still")) {
            String stillBase = value.substring(0, value.length() - "_still".length());
            paths.add(stillBase);
            if (stillBase.endsWith("_fuel")) {
                paths.add("fuel");
            }
        }
        if (value.endsWith("_steam")) {
            paths.add(value.substring(0, value.length() - "_steam".length()));
        }
        if (value.endsWith("_fuel")) {
            paths.add("fuel");
        }
        if (value.contains("sulfuric_") && !value.equals("sulfuric_acid")) {
            paths.add(value.substring(value.indexOf("sulfuric_") + "sulfuric_".length()));
            paths.add("sulfuric_acid");
        }
        if (value.endsWith("_acid") && !value.equals("sulfuric_acid")) {
            paths.add("sulfuric_acid");
        }
        switch (value) {
            case "bioethanol" -> paths.add("ethanol");
            case "biodiesel" -> {
                paths.add("ethanol");
                paths.add("plant_oil");
                paths.add("plantoil");
            }
            case "biofuel" -> {
                paths.add("fuel");
                paths.add("biodiesel");
            }
            case "fuel", "heavy_fuel", "light_fuel" -> paths.add("oil");
            case "diesel" -> {
                paths.add("naphtha");
                paths.add("fuel");
            }
            case "synthetic_oil" -> paths.add("crude_oil");
            default -> {
            }
        }
        paths.remove(value);
        return List.copyOf(paths);
    }

    private static void addPathVariant(Set<String> paths, String value, String prefix) {
        if (value.startsWith(prefix) && value.length() > prefix.length()) {
            paths.add(value.substring(prefix.length()));
        }
    }

    private static ChickensRegistryItem findFamilyCatalyst(String path, ResourceLocation fluidId,
            ChickensRegistryItem child, Map<ResourceLocation, ChickensRegistryItem> fluidById,
            Map<String, List<FluidReference>> fluidByPath,
            Map<String, ChickensRegistryItem> byName) {
        if (path.startsWith("clean_") || path.contains("wash")) {
            return resolveParent(byName, "WaterChicken");
        }
        if (path.contains("high_pressure") || path.contains("pressurized")) {
            return findResourceParent(byName, child, "IronChicken", "QuartzChicken");
        }
        if (path.contains("empowered")) {
            return findResourceParent(byName, child, "GlowstoneChicken", "RedstoneChicken");
        }
        if (path.contains("crystallized")) {
            return findResourceParent(byName, child, "QuartzChicken", "GlowstoneChicken");
        }
        if (path.contains("biodiesel")) {
            ChickensRegistryItem plantOil = findFluidByAnyPath(fluidId,
                    List.of("plant_oil", "plantoil"), fluidById, fluidByPath);
            return plantOil != null ? plantOil : findResourceParent(byName, child, "LogChicken");
        }
        if (path.contains("bioethanol") || path.equals("ethanol")) {
            return findResourceParent(byName, child, "NetherwartChicken", "GreenChicken");
        }
        if (path.contains("fuel") || path.contains("rocket")) {
            return findResourceParent(byName, child, "BlazeChicken", "CoalChicken");
        }
        if (isHotProcess(path)) {
            return resolveParent(byName, "LavaChicken");
        }
        if (path.contains("acid") || path.contains("solution") || path.contains("slurry")) {
            return firstResourceParent(findMaterialParent(fluidId, byName),
                    findResourceAnchor(fluidId, child, byName), byName);
        }
        return resolveParent(byName, isWaterProcess(path) ? "WaterChicken" : "LavaChicken");
    }

    private static ChickensRegistryItem findProcessParent(String path, ChickensRegistryItem child,
            Map<String, ChickensRegistryItem> byName) {
        ChickensRegistryItem process = resolveParent(byName, isHotProcess(path) ? "LavaChicken" : "WaterChicken");
        if (process != child) {
            return process;
        }
        return findResourceParent(byName, child, "IronChicken", "QuartzChicken");
    }

    private static boolean isHotProcess(String path) {
        return path.contains("molten") || path.contains("plasma") || path.contains("steam")
                || path.contains("fuel") || path.contains("oil") || path.contains("diesel")
                || path.contains("biodiesel") || path.contains("naphtha") || path.contains("rocket")
                || path.contains("fire") || path.contains("lava") || path.contains("blazing")
                || path.contains("hot");
    }

    private static boolean isWaterProcess(String path) {
        return path.contains("acid") || path.contains("solution") || path.contains("slurry")
                || path.contains("dirty") || path.contains("clean") || path.contains("water")
                || path.contains("milk") || path.contains("sewage") || path.contains("sludge")
                || path.contains("juice") || path.contains("resin") || path.contains("paste");
    }

    @Nullable
    private static ChickensRegistryItem findResourceAnchor(ResourceLocation fluidId,
            ChickensRegistryItem child, Map<String, ChickensRegistryItem> byName) {
        ChickensRegistryItem material = findMaterialParent(fluidId, byName);
        if (isResourceParent(material) && material != child) {
            return material;
        }

        String path = fluidId.getPath().toLowerCase(Locale.ROOT);
        String[][] hints = {
                {"dye", "WhiteChicken", "GrayChicken", "BlueChicken"},
                {"color", "WhiteChicken", "GrayChicken", "BlueChicken"},
                {"oil", "CoalChicken", "LogChicken", "BlazeChicken"},
                {"fuel", "CoalChicken", "BlazeChicken", "IronChicken"},
                {"diesel", "CoalChicken", "LogChicken", "BlazeChicken"},
                {"naphtha", "CoalChicken", "LogChicken", "BlazeChicken"},
                {"acid", "SulfurChicken", "QuartzChicken", "SandChicken"},
                {"sulfur", "SulfurChicken", "CoalChicken", "QuartzChicken"},
                {"solution", "SulfurChicken", "SandChicken", "QuartzChicken"},
                {"slurry", "SulfurChicken", "SandChicken", "WaterChicken"},
                {"cryo", "IceChicken", "QuartzChicken", "SnowballChicken"},
                {"ice", "IceChicken", "SnowballChicken", "QuartzChicken"},
                {"heavy_water", "IceChicken", "QuartzChicken", "SnowballChicken"},
                {"ender", "EnderChicken", "ObsidianChicken", "QuartzChicken"},
                {"void", "EnderChicken", "ObsidianChicken", "QuartzChicken"},
                {"dragon", "dragonChicken", "EnderChicken", "QuartzChicken"},
                {"fire", "BlazeChicken", "CoalChicken", "RedstoneChicken"},
                {"blaze", "BlazeChicken", "CoalChicken", "RedstoneChicken"},
                {"experience", "EmeraldChicken", "QuartzChicken", "RedstoneChicken"},
                {"xp", "EmeraldChicken", "QuartzChicken", "RedstoneChicken"},
                {"essence", "NetherwartChicken", "QuartzChicken", "GlowstoneChicken"},
                {"redstone", "RedstoneChicken", "IronChicken", "GlowstoneChicken"},
                {"glowstone", "GlowstoneChicken", "QuartzChicken", "RedstoneChicken"},
                {"quartz", "QuartzChicken", "IronChicken", "GlowstoneChicken"},
                {"obsidian", "ObsidianChicken", "DiamondChicken", "IronChicken"},
                {"gold", "GoldChicken", "IronChicken", "CopperChicken"},
                {"copper", "CopperChicken", "IronChicken", "GoldChicken"},
                {"iron", "IronChicken", "CoalChicken", "RedstoneChicken"},
                {"bio", "LogChicken", "SlimeChicken", "NetherwartChicken"},
                {"honey", "LogChicken", "GreenChicken", "SlimeChicken"},
                {"resin", "LogChicken", "SlimeChicken", "WaterChicken"},
                {"meat", "SlimeChicken", "LogChicken", "NetherwartChicken"},
                {"manure", "LogChicken", "GreenChicken", "SlimeChicken"},
                {"water", "IceChicken", "QuartzChicken", "IronChicken"}
        };
        for (String[] hint : hints) {
            if (path.contains(hint[0])) {
                ChickensRegistryItem parent = findResourceParent(byName, child,
                        hint[1], hint[2], hint[3]);
                if (parent != null) {
                    return parent;
                }
            }
        }

        int start = Math.floorMod(path.hashCode(), RESOURCE_FALLBACKS.size());
        for (int offset = 0; offset < RESOURCE_FALLBACKS.size(); offset++) {
            ChickensRegistryItem parent = findResourceParent(byName, child,
                    RESOURCE_FALLBACKS.get((start + offset) % RESOURCE_FALLBACKS.size()));
            if (parent != null) {
                return parent;
            }
        }
        return null;
    }

    @Nullable
    private static ChickensRegistryItem firstResourceParent(@Nullable ChickensRegistryItem first,
            @Nullable ChickensRegistryItem second, Map<String, ChickensRegistryItem> byName) {
        if (isResourceParent(first)) {
            return first;
        }
        if (isResourceParent(second)) {
            return second;
        }
        return findResourceParent(byName, null, RESOURCE_FALLBACKS.toArray(String[]::new));
    }

    @Nullable
    private static ChickensRegistryItem findResourceParent(Map<String, ChickensRegistryItem> byName,
            @Nullable ChickensRegistryItem excluded, String... names) {
        for (String name : names) {
            ChickensRegistryItem parent = resolveParent(byName, name);
            if (parent != null && parent != excluded && isResourceParent(parent)) {
                return parent;
            }
        }
        return null;
    }

    private static boolean isResourceParent(@Nullable ChickensRegistryItem chicken) {
        if (chicken == null) {
            return false;
        }
        ItemStack stack = chicken.createLayItem();
        return !stack.isEmpty()
                && stack.getItem() != ModRegistry.LIQUID_EGG.get()
                && stack.getItem() != ModRegistry.CHEMICAL_EGG.get()
                && stack.getItem() != ModRegistry.GAS_EGG.get();
    }

    private static Map<String, List<FluidReference>> indexFluidPaths(
            Map<ResourceLocation, ChickensRegistryItem> fluidById) {
        Map<String, List<FluidReference>> byPath = new HashMap<>();
        for (Map.Entry<ResourceLocation, ChickensRegistryItem> entry : fluidById.entrySet()) {
            byPath.computeIfAbsent(entry.getKey().getPath().toLowerCase(Locale.ROOT), ignored -> new ArrayList<>())
                    .add(new FluidReference(entry.getKey(), entry.getValue()));
        }
        for (List<FluidReference> entries : byPath.values()) {
            entries.sort(Comparator.comparing(reference -> reference.id().toString()));
        }
        return byPath;
    }

    @Nullable
    private static ChickensRegistryItem findFluidByAnyPath(@Nullable ResourceLocation childId,
            List<String> paths, Map<ResourceLocation, ChickensRegistryItem> fluidById,
            Map<String, List<FluidReference>> fluidByPath) {
        for (String path : paths) {
            ChickensRegistryItem parent = findFluidByPath(childId, path, fluidById, fluidByPath);
            if (parent != null) {
                return parent;
            }
        }
        return null;
    }

    @Nullable
    private static ChickensRegistryItem findFluidByPath(@Nullable ResourceLocation childId, String path,
            Map<ResourceLocation, ChickensRegistryItem> fluidById,
            Map<String, List<FluidReference>> fluidByPath) {
        if (childId != null) {
            ResourceLocation sameNamespace = ResourceLocation.tryParse(childId.getNamespace() + ":" + path);
            if (sameNamespace != null) {
                ChickensRegistryItem sameNamespaceParent = fluidById.get(
                        FluidChickenAliases.canonicalId(sameNamespace));
                if (sameNamespaceParent != null) {
                    return sameNamespaceParent;
                }
            }
        }
        List<FluidReference> matches = fluidByPath.get(path.toLowerCase(Locale.ROOT));
        return matches == null || matches.isEmpty() ? null : matches.get(0).chicken();
    }

    private record ParentPair(@Nullable ChickensRegistryItem parent1,
                               @Nullable ChickensRegistryItem parent2) {
    }

    private record FluidReference(ResourceLocation id, ChickensRegistryItem chicken) {
    }

    private static String toChickenName(String path) {
        String[] parts = path.split("[^a-z0-9]+");
        StringBuilder name = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                name.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
            }
        }
        return name.append("Chicken").toString();
    }

    @Nullable
    private static ChickensRegistryItem findExistingFluidChicken(Iterable<ChickensRegistryItem> chickens,
            ResourceLocation targetFluidId) {
        for (ChickensRegistryItem chicken : chickens) {
            if (!chicken.isEnabled()) {
                continue;
            }
            if (representsFluid(chicken.createLayItem(), targetFluidId)
                    || representsFluid(chicken.createDropItem(), targetFluidId)) {
                return chicken;
            }
        }
        return null;
    }

    private static boolean representsFluid(ItemStack stack, ResourceLocation targetFluidId) {
        if (stack.isEmpty() || stack.getItem() != ModRegistry.LIQUID_EGG.get()) {
            return false;
        }
        LiquidEggRegistryItem entry = LiquidEggRegistry.findById(ChickenItemHelper.getChickenType(stack));
        if (entry == null) {
            return false;
        }
        ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(entry.getFluid());
        return fluidId != null && FluidChickenAliases.canonicalId(fluidId).equals(targetFluidId);
    }

    private static boolean registerRetiredAlias(@Nullable List<ChickensRegistryItem> collector,
            Map<String, ChickensRegistryItem> byName, Set<Integer> usedIds, AliasCandidate alias,
            ChickensRegistryItem canonical, boolean registerImmediately) {
        String entityName = buildEntityName(alias.aliasId());
        String nameKey = entityName.toLowerCase(Locale.ROOT);
        if (byName.containsKey(nameKey)) {
            return false;
        }

        ChickensRegistryItem retired = new ChickensRegistryItem(
                allocateId(alias.aliasId(), usedIds),
                entityName,
                canonical.getTexture(),
                canonical.createLayItem(),
                canonical.getBgColor(),
                canonical.getFgColor());
        retired.setGeneratedTexture(true);
        retired.setSpawnType(SpawnType.NONE);
        retired.setDisplayName(buildDisplayName(alias.entry()).copy()
                .append(Component.literal(" (Legacy Alias)")));
        retired.setNoParents();
        retired.setDousingAllowed(false);
        retired.setEnabled(false);

        byName.put(nameKey, retired);
        if (collector != null) {
            collector.add(retired);
        }
        if (registerImmediately) {
            ChickensRegistry.register(retired);
        }
        return true;
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
    private static ChickensRegistryItem resolveParent(Map<String, ChickensRegistryItem> byName, String name) {
        return byName.get(name.toLowerCase(Locale.ROOT));
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

    private static Component buildDisplayName(LiquidEggRegistryItem entry) {
        return entry.getDisplayName().copy().append(Component.literal(" Chicken"));
    }

    private static int allocateId(ResourceLocation fluidId, Set<Integer> usedIds) {
        int candidate = ID_BASE + Math.floorMod(fluidId.hashCode(), ID_SPAN);
        while (usedIds.contains(candidate)) {
            candidate++;
        }
        usedIds.add(candidate);
        return candidate;
    }

    private static String buildEntityName(ResourceLocation fluidId) {
        return fluidId.getNamespace() + toChickenName(fluidId.getPath());
    }

    private static int accentColor(int base) {
        int r = Math.min(0xFF, ((base >> 16) & 0xFF) + 0x30);
        int g = Math.min(0xFF, ((base >> 8) & 0xFF) + 0x30);
        int b = Math.min(0xFF, (base & 0xFF) + 0x30);
        return (r << 16) | (g << 8) | b;
    }

    private record FluidCandidate(ResourceLocation fluidId, LiquidEggRegistryItem entry) {
    }

    private record AliasCandidate(ResourceLocation aliasId, LiquidEggRegistryItem entry,
                                  ResourceLocation canonicalId) {
    }
}
