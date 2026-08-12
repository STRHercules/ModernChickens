package strhercules.chickens.integration.kubejs;

import dev.latvian.mods.kubejs.event.KubeStartupEvent;
import strhercules.chickens.ChickensRegistryItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;


public class ChickenRegistryKubeEvent implements KubeStartupEvent {
    static final Logger LOGGER = LoggerFactory.getLogger("ChickensKubeJS");

 
    private static final int ID_BASE = 6_000_000;
    private static final int ID_SPAN = 1_000_000;

    private final List<ChickensRegistryItem> chickens;
    private final Map<String, ChickensRegistryItem> byName;
    private final List<KubeChickenBuilder> builders = new ArrayList<>();

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

    /** True when a chicken with this name is already registered by the mod, a config file or another script. */
    public boolean exists(String name) {
        return byName.containsKey(stripNamespace(name));
    }

    /** Names of every chicken known so far, useful for logging or conditional packs. */
    public List<String> getNames() {
        return List.copyOf(byName.keySet());
    }

    void apply() {
        if (builders.isEmpty()) {
            return;
        }

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

        for (KubeChickenBuilder builder : created) {
            ChickensRegistryItem chicken = byName.get(builder.getEntityName());
            if (chicken == null) {
                continue;
            }

            ChickensRegistryItem parent1 = resolveParent(builder.getEntityName(), builder.parent1Name());
            ChickensRegistryItem parent2 = resolveParent(builder.getEntityName(), builder.parent2Name());
            if (parent1 != null && parent2 != null) {
                chicken.setParentsNew(parent1, parent2);
            } else if (parent1 != null || parent2 != null) {
                LOGGER.warn("Script chicken '{}' only has one usable parent; clearing its breeding data",
                        builder.getEntityName());
                chicken.setNoParents();
            }
        }

        LOGGER.info("Registered {} chickens from KubeJS startup scripts ({} rejected)",
                created.size(), builders.size() - created.size());
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
    private ChickensRegistryItem resolveParent(String child, @Nullable String parentName) {
        if (parentName == null) {
            return null;
        }
        ChickensRegistryItem parent = byName.get(parentName);
        if (parent == null) {
            LOGGER.warn("Script chicken '{}' references unknown parent '{}'", child, parentName);
        }
        return parent;
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
