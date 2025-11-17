package com.setycz.chickens.data;

import com.setycz.chickens.ChickensMod;
import com.setycz.chickens.ChickensRegistry;
import com.setycz.chickens.ChickensRegistryItem;
import com.setycz.chickens.SpawnType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Discovers resource items present in the runtime item registry and creates
 * placeholder chickens for any materials that are not covered by the static
 * data set. The goal is to provide reasonable coverage for mod packs whose
 * material set differs from the curated defaults without forcing players to
 * edit configuration files by hand.
 */
final class DynamicMaterialChickens {
    private static final Logger LOGGER = LoggerFactory.getLogger("ChickensDynamic");
    private static final ResourceLocation PLACEHOLDER_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ChickensMod.MOD_ID, "textures/entity/unknownchicken.png");
    private static final int ID_BASE = 2000;
    private static final int ID_SPAN = 1_000_000;
    private static final Set<String> REGISTERED_KEYS = new HashSet<>();

    private DynamicMaterialChickens() {
    }

    static void register(List<ChickensRegistryItem> chickens, Map<String, ChickensRegistryItem> byName) {
        attemptRegistration(chickens, byName, false);
    }

    static void refresh() {
        Map<String, ChickensRegistryItem> index = buildRegistryIndex();
        attemptRegistration(null, index, true);
    }

    private static void attemptRegistration(@Nullable List<ChickensRegistryItem> collector,
            Map<String, ChickensRegistryItem> byName, boolean registerImmediately) {
        REGISTERED_KEYS.clear();
        Set<Integer> usedIds = collectUsedIds(collector, byName);
        ChickensRegistryItem smartChicken = byName.get("smartchicken");
        registerExistingKeys(byName.values());

        List<MaterialCandidate> candidates = discoverMaterials();
        int created = 0;
        for (MaterialCandidate candidate : candidates) {
            MaterialKey key = candidate.key();
            if (REGISTERED_KEYS.contains(key.uniqueKey())) {
                continue;
            }
            ItemStack stack = new ItemStack(candidate.item());
            if (stack.isEmpty() || stack.getItem() == Items.AIR) {
                continue;
            }
            if (alreadyRepresents(byName.values(), stack)) {
                REGISTERED_KEYS.add(key.uniqueKey());
                continue;
            }

            String entityName = key.entityName();
            String nameKey = entityName.toLowerCase(Locale.ROOT);
            if (byName.containsKey(nameKey)) {
                REGISTERED_KEYS.add(key.uniqueKey());
                continue;
            }

            int id = allocateId(key, usedIds);
            int primary = primaryColor(key);
            int accent = accentColor(key);

            ChickensRegistryItem chicken = new ChickensRegistryItem(
                    id,
                    entityName,
                    PLACEHOLDER_TEXTURE,
                    stack,
                    primary,
                    accent);
            chicken.setSpawnType(SpawnType.NONE);
            if (smartChicken != null) {
                chicken.setParentsNew(smartChicken, smartChicken);
            }
            chicken.setDisplayName(buildDisplayName(stack, key)).setGeneratedTexture(true);

            byName.put(nameKey, chicken);
            if (collector != null) {
                collector.add(chicken);
            }
            if (registerImmediately) {
                ChickensRegistry.register(chicken);
            }
            REGISTERED_KEYS.add(key.uniqueKey());
            created++;
        }

        if (created > 0) {
            LOGGER.info("Registered {} dynamic material chickens", created);
        }
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

    private static List<MaterialCandidate> discoverMaterials() {
        Map<String, MaterialCandidate> unique = new LinkedHashMap<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
            if (itemId == null || item == Items.AIR) {
                continue;
            }
            // Prevent auto-generating a generic neutronium ingot chicken; we gate neutronium
            // through the curated neutron_pile definition instead.
            if (itemId.getNamespace().equals("avaritia") && itemId.getPath().equals("neutronium_ingot")) {
                continue;
            }

            for (MaterialKey key : collectKeys(item, itemId)) {
                if (key.isValid()) {
                    unique.putIfAbsent(key.uniqueKey(), new MaterialCandidate(key, item));
                }
            }
        }
        return new ArrayList<>(unique.values());
    }

    private static List<MaterialKey> collectKeys(Item item, ResourceLocation itemId) {
        List<MaterialKey> keys = new ArrayList<>();
        item.builtInRegistryHolder().tags().forEach(tag -> {
            ResourceLocation tagId = tag.location();
            for (MaterialCategory category : MaterialCategory.values()) {
                category.extractFromTag(tagId).ifPresent(material ->
                        keys.add(MaterialKey.fromTag(category, tagId.getNamespace(), material)));
            }
        });

        for (MaterialCategory category : MaterialCategory.values()) {
            category.extractFromName(itemId.getPath())
                    .ifPresent(material -> keys.add(MaterialKey.fromName(category, itemId.getNamespace(), material)));
        }
        return keys;
    }

    private static boolean alreadyRepresents(Collection<ChickensRegistryItem> chickens, ItemStack stack) {
        Item target = stack.getItem();
        for (ChickensRegistryItem chicken : chickens) {
            if (chicken.createLayItem().getItem() == target) {
                return true;
            }
            if (chicken.createDropItem().getItem() == target) {
                return true;
            }
        }
        return false;
    }

    private static int allocateId(MaterialKey key, Set<Integer> usedIds) {
        int seed = Math.floorMod(key.uniqueKey().hashCode(), ID_SPAN);
        int candidate = ID_BASE + seed;
        while (usedIds.contains(candidate)) {
            candidate++;
        }
        usedIds.add(candidate);
        return candidate;
    }

    private static int primaryColor(MaterialKey key) {
        long seed = Integer.toUnsignedLong(key.uniqueKey().hashCode());
        float hue = ((seed & 0xFFL) / 255.0f);
        float saturation = 0.55f + ((seed >> 8) & 0x3FL) / 255.0f;
        float brightness = 0.65f + ((seed >> 14) & 0x3FL) / 255.0f;
        return hsbToRgb(hue, clamp01(saturation), clamp01(brightness));
    }

    private static int accentColor(MaterialKey key) {
        long seed = Integer.toUnsignedLong(key.uniqueKey().hashCode()) ^ 0x5b_a1_3f_01L;
        float hue = (((seed & 0xFFL) / 255.0f) + 0.12f) % 1.0f;
        float saturation = 0.45f + ((seed >> 8) & 0x3FL) / 255.0f;
        float brightness = 0.75f + ((seed >> 14) & 0x3FL) / 255.0f;
        return hsbToRgb(hue, clamp01(saturation), clamp01(brightness));
    }

    private static float clamp01(float value) {
        if (value < 0.0f) {
            return 0.0f;
        }
        if (value > 1.0f) {
            return 1.0f;
        }
        return value;
    }

    private static int hsbToRgb(float hue, float saturation, float brightness) {
        if (saturation <= 0.0f) {
            int v = Math.round(clamp01(brightness) * 255.0f);
            return (v << 16) | (v << 8) | v;
        }

        float h = (hue % 1.0f + 1.0f) % 1.0f;
        float scaled = h * 6.0f;
        int sector = (int) Math.floor(scaled);
        float fraction = scaled - sector;

        float p = brightness * (1.0f - saturation);
        float q = brightness * (1.0f - saturation * fraction);
        float t = brightness * (1.0f - saturation * (1.0f - fraction));

        float r;
        float g;
        float b;
        switch (sector) {
            case 0 -> {
                r = brightness;
                g = t;
                b = p;
            }
            case 1 -> {
                r = q;
                g = brightness;
                b = p;
            }
            case 2 -> {
                r = p;
                g = brightness;
                b = t;
            }
            case 3 -> {
                r = p;
                g = q;
                b = brightness;
            }
            case 4 -> {
                r = t;
                g = p;
                b = brightness;
            }
            default -> {
                r = brightness;
                g = p;
                b = q;
            }
        }

        int ri = Math.round(clamp01(r) * 255.0f);
        int gi = Math.round(clamp01(g) * 255.0f);
        int bi = Math.round(clamp01(b) * 255.0f);
        return (ri << 16) | (gi << 8) | bi;
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

    private record MaterialCandidate(MaterialKey key, Item item) {
    }

    private static void registerExistingKeys(Collection<ChickensRegistryItem> chickens) {
        for (ChickensRegistryItem chicken : chickens) {
            registerStackKeys(chicken.createLayItem());
            registerStackKeys(chicken.createDropItem());
        }
    }

    private static void registerStackKeys(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        Item item = stack.getItem();
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
        if (itemId == null) {
            return;
        }
        for (MaterialKey key : collectKeys(item, itemId)) {
            if (key.isValid()) {
                REGISTERED_KEYS.add(key.uniqueKey());
            }
        }
    }

    private static Component buildDisplayName(ItemStack stack, MaterialKey key) {
        String resolved = stack.getHoverName().getString().trim();
        String descriptionId = stack.getDescriptionId();

        String baseName;
        if (resolved.isEmpty() || resolved.equals(descriptionId) || resolved.contains(".")) {
            baseName = friendlyMaterialName(key);
        } else {
            baseName = stripSuffix(resolved, "ingot");
            if (baseName.isEmpty()) {
                baseName = friendlyMaterialName(key);
            }
        }
        return Component.literal(baseName + " Chicken");
    }

    private static String stripSuffix(String input, String suffix) {
        String trimmed = input.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        String target = " " + suffix;
        if (lower.endsWith(target)) {
            return trimmed.substring(0, trimmed.length() - target.length()).trim();
        }
        String altTarget = "-" + suffix;
        if (lower.endsWith(altTarget)) {
            return trimmed.substring(0, trimmed.length() - altTarget.length()).trim();
        }
        String underscoreTarget = "_" + suffix;
        if (lower.endsWith(underscoreTarget)) {
            return trimmed.substring(0, trimmed.length() - underscoreTarget.length()).trim();
        }
        return trimmed;
    }

    private static String friendlyMaterialName(MaterialKey key) {
        List<String> tokens = new ArrayList<>();
        String namespaceToken = sanitizeToken(key.namespace());
        String[] materialTokens = key.material().split("[_\\-]+");
        for (String token : materialTokens) {
            if (token.isBlank()) {
                continue;
            }
            tokens.add(prettifyToken(token, namespaceToken));
        }

        if (tokens.isEmpty()) {
            tokens.add(prettifyToken(namespaceToken, namespaceToken));
        }

        return String.join(" ", tokens);
    }

    private static String sanitizeToken(String token) {
        return token == null ? "" : token.trim().toLowerCase(Locale.ROOT);
    }

    private static String prettifyToken(String token, String namespaceToken) {
        if (token.equals(namespaceToken)) {
            return lookupModDisplayName(namespaceToken).orElse(capitalize(token));
        }
        return capitalize(token);
    }

    private static Optional<String> lookupModDisplayName(String namespace) {
        if (namespace.isEmpty()) {
            return Optional.empty();
        }
        return ModList.get().getModContainerById(namespace)
                .map(container -> container.getModInfo().getDisplayName());
    }

    private static String capitalize(String token) {
        if (token.isEmpty()) {
            return token;
        }
        if (token.length() == 1) {
            return token.toUpperCase(Locale.ROOT);
        }
        if (isAllUpper(token)) {
            return token;
        }
        return Character.toUpperCase(token.charAt(0)) + token.substring(1);
    }

    private static boolean isAllUpper(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isLetter(c) && Character.isLowerCase(c)) {
                return false;
            }
        }
        return true;
    }

    private enum MaterialCategory {
        INGOT("ingot", "ingots");

        private final String singular;
        private final String plural;

        MaterialCategory(String singular, String plural) {
            this.singular = singular;
            this.plural = plural;
        }

        Optional<String> extractFromTag(ResourceLocation tagId) {
            String path = tagId.getPath();
            if (path.startsWith(plural + "/")) {
                return Optional.of(path.substring(plural.length() + 1));
            }
            return Optional.empty();
        }

        Optional<String> extractFromName(String path) {
            if (path.endsWith("_" + singular)) {
                return Optional.of(path.substring(0, path.length() - singular.length() - 1));
            }
            if (path.endsWith("_" + plural)) {
                return Optional.of(path.substring(0, path.length() - plural.length() - 1));
            }
            if (path.startsWith(singular + "_")) {
                return Optional.of(path.substring(singular.length() + 1));
            }
            return Optional.empty();
        }
    }

    private record MaterialKey(MaterialCategory category, String namespace, String material) {
        private static final Set<String> IGNORED_NAMESPACES = Set.of("minecraft");

        static MaterialKey fromTag(MaterialCategory category, String namespace, String material) {
            return new MaterialKey(category, namespace, material);
        }

        static MaterialKey fromName(MaterialCategory category, String namespace, String material) {
            return new MaterialKey(category, namespace, material);
        }

        boolean isValid() {
            return !canonicalName().isEmpty();
        }

        String uniqueKey() {
            return category.singular + ":" + namespace + ":" + sanitize(material);
        }

        String entityName() {
            return "auto_" + category.singular + "_" + canonicalName() + "_chicken";
        }

        private String canonicalName() {
            String base = sanitize(material);
            if (base.isEmpty()) {
                return base;
            }
            String ns = sanitize(namespace);
            if (!ns.isEmpty() && !namespace.equals("forge") && !namespace.equals("c") && !IGNORED_NAMESPACES.contains(ns)) {
                base = ns + "_" + base;
            }
            return base;
        }

        private static String sanitize(String raw) {
            String lower = raw.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
            int start = 0;
            int end = lower.length();
            while (start < end && lower.charAt(start) == '_') {
                start++;
            }
            while (end > start && lower.charAt(end - 1) == '_') {
                end--;
            }
            return start >= end ? "" : lower.substring(start, end);
        }
    }
}
