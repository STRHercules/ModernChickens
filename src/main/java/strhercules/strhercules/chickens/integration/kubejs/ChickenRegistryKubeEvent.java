package strhercules.chickens.integration.kubejs;

import dev.latvian.mods.kubejs.event.StartupEventJS;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import strhercules.chickens.ChemicalEggRegistry;
import strhercules.chickens.ChemicalEggRegistryItem;
import strhercules.chickens.ChickenTeachingRegistry;
import strhercules.chickens.ChickensRegistryItem;
import strhercules.chickens.GasEggRegistry;
import strhercules.chickens.LiquidEggRegistry;
import strhercules.chickens.LiquidEggRegistryItem;
import strhercules.chickens.item.ChickenItemHelper;
import strhercules.chickens.registry.ModRegistry;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class ChickenRegistryKubeEvent extends StartupEventJS {
    static final Logger LOGGER = LoggerFactory.getLogger("ChickensKubeJS");

    private static final int ID_BASE = 6_000_000;
    private static final int ID_SPAN = 1_000_000;

    private final List<ChickensRegistryItem> chickens;
    private final Map<String, ChickensRegistryItem> byName;
    private final List<KubeChickenBuilder> builders = new ArrayList<>();
    private final Map<String, KubeChickenModifier> modifiers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final Map<ResourceLocation, KubeLineageBuilder> fluidRules = new HashMap<>();
    private final Map<ResourceLocation, KubeLineageBuilder> chemicalRules = new HashMap<>();
    private final Map<String, KubeEggModifier> eggModifiers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final Map<Item, String> teachingRules = new HashMap<>();

    ChickenRegistryKubeEvent(List<ChickensRegistryItem> chickens) {
        this.chickens = chickens;
        this.byName = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (ChickensRegistryItem chicken : chickens) {
            byName.put(chicken.getEntityName(), chicken);
        }
    }

    public KubeChickenBuilder create(String id) {
        String name = stripNamespace(id);
        KubeChickenBuilder builder = new KubeChickenBuilder(name);
        builders.add(builder);
        return builder;
    }

    public KubeChickenModifier modify(String id) {
        String name = stripNamespace(id);
        return modifiers.computeIfAbsent(name, KubeChickenModifier::new);
    }

    public KubeLineageBuilder fluid(String id) {
        ResourceLocation resourceId = parseResource(id, "fluid");
        KubeLineageBuilder builder = resourceId == null
                ? new KubeLineageBuilder(null, false)
                : fluidRules.computeIfAbsent(resourceId, key -> new KubeLineageBuilder(key, false));
        return builder;
    }

    public KubeLineageBuilder chemical(String id) {
        ResourceLocation resourceId = parseResource(id, "chemical");
        return resourceId == null
                ? new KubeLineageBuilder(null, true)
                : chemicalRules.computeIfAbsent(resourceId, key -> new KubeLineageBuilder(key, true));
    }

    public KubeEggModifier modifyEgg(String id) {
        String key = id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
        return eggModifiers.computeIfAbsent(key, KubeEggModifier::new);
    }

    /** Adds or replaces an item-to-breed teaching mapping. Accepts an item id,
     *  an Item or an ItemStack; a single overload keeps Rhino from having to
     *  disambiguate between them. */
    public void teach(Object trigger, String chickenName) {
        Item item = resolveItem(trigger);
        if (item == null) {
            LOGGER.warn("Ignoring teaching rule for unresolved item '{}'", trigger);
            return;
        }
        String name = stripNamespace(chickenName);
        if (name.isEmpty()) {
            LOGGER.warn("Ignoring teaching rule for item '{}' with an empty chicken name", trigger);
            return;
        }
        teachingRules.put(item, name);
    }

    /** True when a chicken with this name is already registered by the mod, a config file or another script. */
    public boolean exists(String name) {
        return byName.containsKey(stripNamespace(name));
    }

    /** Names of every chicken known so far, useful for logging or conditional packs. */
    public List<String> getNames() {
        return List.copyOf(byName.keySet());
    }

    void apply() {
        if (!builders.isEmpty()) {
            Set<Integer> usedIds = new HashSet<>();
            for (ChickensRegistryItem chicken : chickens) {
                usedIds.add(chicken.getId());
            }

            List<KubeChickenBuilder> created = new ArrayList<>(builders.size());
            for (KubeChickenBuilder builder : builders) {
                String name = builder.getEntityName();
                if (name.isEmpty()) {
                    LOGGER.warn("Skipping a script chicken with an empty name");
                    continue;
                }
                if (byName.containsKey(name)) {
                    LOGGER.warn("Skipping script chicken '{}' because that name is already registered", name);
                    continue;
                }

                try {
                    ChickensRegistryItem chicken = builder.build(resolveId(builder, usedIds));
                    chickens.add(chicken);
                    byName.put(name, chicken);
                    created.add(builder);
                } catch (IllegalArgumentException ex) {
                    LOGGER.warn("Skipping script chicken '{}': {}", name, ex.getMessage());
                }
            }

            applyBuilderParents(byName);
            LOGGER.info("Registered {} chickens from KubeJS startup scripts ({} rejected)",
                    created.size(), builders.size() - created.size());
        }
        applyEggModifiers();
    }

    /** Applies scripts after config and automatic chickens have completed. */
    void applyFinal(List<ChickensRegistryItem> currentChickens) {
        Map<String, ChickensRegistryItem> currentByName = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (ChickensRegistryItem chicken : currentChickens) {
            currentByName.put(chicken.getEntityName(), chicken);
        }

        applyBuilderParents(currentByName);
        for (KubeLineageBuilder rule : fluidRules.values()) {
            applyLineageRule(rule, currentChickens, currentByName);
        }
        for (KubeLineageBuilder rule : chemicalRules.values()) {
            applyLineageRule(rule, currentChickens, currentByName);
        }
        for (Map.Entry<String, KubeChickenModifier> entry : modifiers.entrySet()) {
            ChickensRegistryItem chicken = currentByName.get(entry.getKey());
            if (chicken == null) {
                LOGGER.warn("KubeJS modifier targets unknown chicken '{}'", entry.getKey());
                continue;
            }
            entry.getValue().apply(chicken, currentByName);
        }
        for (Map.Entry<Item, String> entry : teachingRules.entrySet()) {
            ChickenTeachingRegistry.register(entry.getKey(), entry.getValue());
        }
        applyEggModifiers();
    }

    private void applyBuilderParents(Map<String, ChickensRegistryItem> names) {
        for (KubeChickenBuilder builder : builders) {
            ChickensRegistryItem chicken = names.get(builder.getEntityName());
            if (chicken == null) {
                continue;
            }
            ChickensRegistryItem parent1 = resolveParent(builder.getEntityName(), builder.parent1Name(), names);
            ChickensRegistryItem parent2 = resolveParent(builder.getEntityName(), builder.parent2Name(), names);
            if (parent1 != null && parent2 != null
                    && parent1 != chicken && parent2 != chicken
                    && !containsParent(parent1, chicken, new HashSet<>())
                    && !containsParent(parent2, chicken, new HashSet<>())) {
                chicken.setParentsNew(parent1, parent2);
            } else if (parent1 != null || parent2 != null) {
                LOGGER.warn("Script chicken '{}' only has one usable parent; clearing its breeding data",
                        builder.getEntityName());
                chicken.setNoParents();
            } else if (builder.parent1Name() != null || builder.parent2Name() != null) {
                LOGGER.warn("Script chicken '{}' has unusable breeding parents; clearing its breeding data",
                        builder.getEntityName());
                chicken.setNoParents();
            }
        }
    }

    private void applyEggModifiers() {
        for (Map.Entry<String, KubeEggModifier> entry : eggModifiers.entrySet()) {
            ResourceLocation id = ResourceLocation.tryParse(entry.getKey());
            if (id == null) {
                LOGGER.warn("Egg modifier has a malformed resource id '{}'; ignoring", entry.getKey());
                continue;
            }

            LiquidEggRegistryItem liquid = LiquidEggRegistry.findByFluid(id);
            if (liquid != null) {
                apply(entry.getValue(), liquid);
                continue;
            }
            ChemicalEggRegistryItem chemical = ChemicalEggRegistry.findByChemical(id);
            if (chemical == null) {
                chemical = GasEggRegistry.findByChemical(id);
            }
            if (chemical != null) {
                apply(entry.getValue(), chemical);
            } else {
                LOGGER.warn("Egg modifier targets unknown fluid or chemical '{}'; ignoring", entry.getKey());
            }
        }
    }

    private static void apply(KubeEggModifier modifier, LiquidEggRegistryItem egg) {
        if (modifier.eggColor() != null) {
            egg.setEggColor(modifier.eggColor());
        }
        if (modifier.volume() != null) {
            egg.setVolume(modifier.volume());
        }
        if (modifier.hazards() != null) {
            egg.setHazards(modifier.hazards());
        }
    }

    private static void apply(KubeEggModifier modifier, ChemicalEggRegistryItem egg) {
        if (modifier.eggColor() != null) {
            egg.setEggColor(modifier.eggColor());
        }
        if (modifier.volume() != null) {
            egg.setVolume(modifier.volume());
        }
        if (modifier.hazards() != null) {
            egg.setHazards(modifier.hazards());
        }
    }

    private void applyLineageRule(KubeLineageBuilder rule,
            List<ChickensRegistryItem> currentChickens,
            Map<String, ChickensRegistryItem> currentByName) {
        ChickensRegistryItem child = findLineageTarget(rule, currentChickens);
        if (child == null) {
            LOGGER.warn("{} rule targets an unknown resource '{}'; ignoring",
                    rule.chemical() ? "Chemical" : "Fluid", rule.resourceId());
            return;
        }

        if (rule.parentsSet()) {
            ChickensRegistryItem parent1 = resolveParent(child.getEntityName(), rule.parent1(), currentByName);
            ChickensRegistryItem parent2 = resolveParent(child.getEntityName(), rule.parent2(), currentByName);
            if (parent1 != null && parent2 != null
                    && parent1 != child && parent2 != child
                    && !containsParent(parent1, child, new HashSet<>())
                    && !containsParent(parent2, child, new HashSet<>())) {
                child.setParentsNew(parent1, parent2);
            } else if (rule.parent1() == null && rule.parent2() == null) {
                child.setNoParents();
            } else {
                LOGGER.warn("{} rule for '{}' has unusable parents; clearing its breeding data",
                        rule.chemical() ? "Chemical" : "Fluid", rule.resourceId());
                child.setNoParents();
            }
        }
        if (rule.dousingAllowed() != null) {
            child.setDousingAllowed(rule.dousingAllowed());
        }
        if (rule.spawnWeight() != null) {
            child.setSpawnWeight(rule.spawnWeight());
        }
    }

    @Nullable
    private ChickensRegistryItem findLineageTarget(KubeLineageBuilder rule,
            List<ChickensRegistryItem> currentChickens) {
        if (rule.resourceId() == null) {
            return null;
        }
        int eggId;
        Item eggItem;
        if (rule.chemical()) {
            ChemicalEggRegistryItem entry = ChemicalEggRegistry.findByChemical(rule.resourceId());
            if (entry != null) {
                eggId = entry.getId();
                eggItem = entry.isGaseous() ? ModRegistry.GAS_EGG.get() : ModRegistry.CHEMICAL_EGG.get();
            } else {
                entry = GasEggRegistry.findByChemical(rule.resourceId());
                if (entry == null) {
                    return null;
                }
                eggId = entry.getId();
                eggItem = ModRegistry.GAS_EGG.get();
            }
        } else {
            LiquidEggRegistryItem entry = LiquidEggRegistry.findByFluid(rule.resourceId());
            if (entry == null) {
                return null;
            }
            eggId = entry.getId();
            eggItem = ModRegistry.LIQUID_EGG.get();
        }

        for (ChickensRegistryItem chicken : currentChickens) {
            ItemStack layItem = chicken.createLayItem();
            if (layItem.getItem() == eggItem && ChickenItemHelper.getChickenType(layItem) == eggId) {
                return chicken;
            }
        }
        return null;
    }

    @Nullable
    private ChickensRegistryItem resolveParent(String child, @Nullable String parentName,
            Map<String, ChickensRegistryItem> names) {
        if (parentName == null) {
            return null;
        }
        ChickensRegistryItem parent = names.get(parentName);
        if (parent == null) {
            LOGGER.warn("Script chicken '{}' references unknown parent '{}'", child, parentName);
        }
        return parent;
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

    private int resolveId(KubeChickenBuilder builder, Set<Integer> usedIds) {
        Integer requested = builder.requestedId();
        if (requested != null) {
            if (requested > 0 && usedIds.add(requested)) {
                return requested;
            }
            LOGGER.warn("Script chicken '{}' requested unusable id {}; deriving one from its name instead",
                    builder.getEntityName(), requested);
        }
        return deriveId(builder.getEntityName(), usedIds);
    }

    private static int deriveId(String name, Set<Integer> usedIds) {
        int seed = Math.floorMod(name.toLowerCase(Locale.ROOT).hashCode(), ID_SPAN);
        int candidate = ID_BASE + seed;
        while (!usedIds.add(candidate)) {
            candidate++;
        }
        return candidate;
    }

    @Nullable
    private static ResourceLocation parseResource(String value, String kind) {
        ResourceLocation id = value == null ? null : ResourceLocation.tryParse(value.trim());
        if (id == null) {
            LOGGER.warn("Ignoring {} rule with malformed resource id '{}'", kind, value);
        }
        return id;
    }

    @Nullable
    private static Item resolveItem(Object value) {
        if (value instanceof ItemStack stack) {
            return stack.getItem();
        }
        if (value instanceof Item item) {
            return item;
        }
        if (value == null) {
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(value.toString().trim());
        return id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
    }

    private static String stripNamespace(@Nullable String id) {
        if (id == null) {
            return "";
        }
        String trimmed = id.trim();
        int separator = trimmed.indexOf(':');
        return separator < 0 ? trimmed : trimmed.substring(separator + 1);
    }
}
