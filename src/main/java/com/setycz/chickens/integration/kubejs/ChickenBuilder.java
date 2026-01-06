package com.setycz.chickens.integration.kubejs;

import com.setycz.chickens.ChickensMod;
import com.setycz.chickens.ChickensRegistry;
import com.setycz.chickens.ChickensRegistryItem;
import com.setycz.chickens.SpawnType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;


public class ChickenBuilder {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ChickenBuilder.class);
    private static final ResourceLocation DEFAULT_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ChickensMod.MOD_ID, "textures/entity/unknownchicken.png");
    
    private static final Map<ResourceLocation, BuilderData> PENDING_CHICKENS = new HashMap<>();
    
    private final ResourceLocation id;
    private String parent1Name = "";
    private String parent2Name = "";
    private ItemStack layItem = ItemStack.EMPTY;
    private ItemStack dropItem = ItemStack.EMPTY;
    private Integer numericIdOverride = null;
    private Integer tierOverride = null;
    private SpawnType spawnType = SpawnType.NONE;
    private int primaryColor = 0xFFFFFF;
    private int secondaryColor = 0xFFFF00;
    private String displayName = "";
    private double layCoefficient = 1.0;
    private Boolean generatedTexture = null;
    private Boolean enabled = null;
    private Boolean allowNaturalSpawn = null;
    private Integer liquidDousingCost = null;
    private String texturePath = "";
    private String itemTexturePath = "";
    
    private static class BuilderData {
        ChickensRegistryItem chicken;
        String parent1Name;
        String parent2Name;
        
        BuilderData(ChickensRegistryItem chicken, String parent1Name, String parent2Name) {
            this.chicken = chicken;
            this.parent1Name = parent1Name;
            this.parent2Name = parent2Name;
        }
    }
    
    public ChickenBuilder(ResourceLocation id) {
        this.id = id;
    }
    
    /**
     * Public getter for the chicken ID
     * @return The ResourceLocation ID of this chicken
     */
    public ResourceLocation getId() {
        return this.id;
    }
 
    public ChickenBuilder parent1(String name) {
        this.parent1Name = name;
        return this;
    }
    
    public ChickenBuilder parent2(String name) {
        this.parent2Name = name;
        return this;
    }
    
    public ChickenBuilder layItem(String itemId) {
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
        this.layItem = new ItemStack(item, 1);
        return this;
    }
    
    public ChickenBuilder layItem(String itemId, int count) {
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
        this.layItem = new ItemStack(item, count);
        return this;
    }
    
    public ChickenBuilder dropItem(String itemId) {
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
        this.dropItem = new ItemStack(item, 1);
        return this;
    }
    
    public ChickenBuilder dropItem(String itemId, int count) {
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
        this.dropItem = new ItemStack(item, count);
        return this;
    }
 
    public ChickenBuilder tier(int tier) {
        this.tierOverride = Math.max(1, tier);
        return this;
    }

    public ChickenBuilder numericId(int id) {
        this.numericIdOverride = Math.max(0, id);
        return this;
    }
    
    public ChickenBuilder spawnType(String type) {
        try {
            this.spawnType = SpawnType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid spawn type '{}' for chicken '{}', using NONE", type, this.id);
            this.spawnType = SpawnType.NONE;
        }
        return this;
    }
 
    public ChickenBuilder primaryColor(int color) {
        this.primaryColor = color & 0xFFFFFF;
        return this;
    }
  
    public ChickenBuilder primaryColor(String hexColor) {
        this.primaryColor = parseColor(hexColor);
        return this;
    }
    
    public ChickenBuilder secondaryColor(int color) {
        this.secondaryColor = color & 0xFFFFFF;
        return this;
    }
    
    public ChickenBuilder secondaryColor(String hexColor) {
        this.secondaryColor = parseColor(hexColor);
        return this;
    }
    
    public ChickenBuilder displayName(String name) {
        this.displayName = name;
        return this;
    }
    
    public ChickenBuilder layCoefficient(double coefficient) {
        this.layCoefficient = Math.max(0.0, coefficient);
        return this;
    }
 
    public ChickenBuilder generatedTexture(boolean generated) {
        this.generatedTexture = generated;
        return this;
    }

    // Optional overrides that previously lived in chickens.cfg.
    public ChickenBuilder enabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public ChickenBuilder allowNaturalSpawn(boolean allowNaturalSpawn) {
        this.allowNaturalSpawn = allowNaturalSpawn;
        return this;
    }

    public ChickenBuilder liquidDousingCost(int amount) {
        this.liquidDousingCost = Math.max(1, amount);
        return this;
    }

    public ChickenBuilder texturePath(String path) {
        this.texturePath = path != null ? path : "";
        return this;
    }

    public ChickenBuilder itemTexturePath(String path) {
        this.itemTexturePath = path != null ? path : "";
        return this;
    }
    
    /**
     * Builds and registers the chicken
     * Called automatically by KubeJS at the end of the builder chain
     */
    public void build() {
        if (layItem.isEmpty()) {
            throw new IllegalStateException("Chicken '" + this.id + "' must have a lay item!");
        }
        
        // Use explicit IDs when provided to keep compatibility with legacy item data.
        int numericId = numericIdOverride != null
                ? numericIdOverride
                : Math.abs(this.id.toString().hashCode() % 1_000_000) + 10_000;
        
        boolean hasCustomTexture = !texturePath.trim().isEmpty();
        boolean hasCustomItemTexture = !itemTexturePath.trim().isEmpty();
        ResourceLocation texture = hasCustomTexture ? resolveTexturePath(texturePath) : DEFAULT_TEXTURE;
        
        ChickensRegistryItem chicken = new ChickensRegistryItem(
            numericId,
            this.id.getPath(),
            texture,
            layItem,
            primaryColor,
            secondaryColor
        );
        
        if (!dropItem.isEmpty()) {
            chicken.setDropItem(dropItem);
        }

        if (hasCustomItemTexture) {
            chicken.setItemTexture(resolveItemTexturePath(itemTexturePath));
        }
        
        chicken.setSpawnType(spawnType);
        
        if (!displayName.isEmpty()) {
            chicken.setDisplayName(Component.literal(displayName));
        } else {
            chicken.setDisplayName(Component.literal(generateDisplayName(this.id.getPath())));
        }
        
        if (layCoefficient != 1.0) {
            chicken.setLayCoefficient((float) layCoefficient);
        }

        // Apply KubeJS overrides that replace legacy per-chicken config entries.
        if (tierOverride != null) {
            chicken.setTierOverride(tierOverride);
        }
        if (enabled != null) {
            chicken.setEnabled(enabled);
        }
        if (allowNaturalSpawn != null) {
            chicken.setNaturalSpawnOverride(allowNaturalSpawn);
        }
        if (liquidDousingCost != null) {
            chicken.setLiquidDousingCost(liquidDousingCost);
        }

        boolean useGeneratedTexture = Boolean.TRUE.equals(generatedTexture);
        if (hasCustomTexture && useGeneratedTexture) {
            // Custom textures take priority; colors are reserved for generated recolors.
            useGeneratedTexture = false;
        }
        if (generatedTexture != null || hasCustomTexture) {
            chicken.setGeneratedTexture(useGeneratedTexture);
        }
        
        // Register immediately in ChickensRegistry
        ChickensRegistry.register(chicken);
        
        // Store for parent resolution later
        PENDING_CHICKENS.put(this.id, new BuilderData(chicken, parent1Name, parent2Name));
        
        LOGGER.info("Registered chicken: {} (ID: {})", this.id, numericId);
    }
    
    /**
     * Called after all chickens have been registered to resolve parent relationships
     */
    public static void resolveAllParents() {
        LOGGER.info("Resolving parent relationships for {} chickens", PENDING_CHICKENS.size());
        
        for (Map.Entry<ResourceLocation, BuilderData> entry : PENDING_CHICKENS.entrySet()) {
            ResourceLocation chickenId = entry.getKey();
            BuilderData data = entry.getValue();
            
            if (!data.parent1Name.isEmpty() || !data.parent2Name.isEmpty()) {
                ChickensRegistryItem parent1 = ChickensRegistry.getByEntityName(data.parent1Name);
                ChickensRegistryItem parent2 = ChickensRegistry.getByEntityName(data.parent2Name);
                
                if (parent1 == null && !data.parent1Name.isEmpty()) {
                    LOGGER.warn("Could not find parent1 '{}' for chicken '{}'", data.parent1Name, chickenId);
                }
                if (parent2 == null && !data.parent2Name.isEmpty()) {
                    LOGGER.warn("Could not find parent2 '{}' for chicken '{}'", data.parent2Name, chickenId);
                }
                
                if (parent1 != null && parent2 != null) {
                    data.chicken.setParentsNew(parent1, parent2);
                } else if (parent1 != null) {
                    data.chicken.setParentsNew(parent1, parent1);
                } else if (parent2 != null) {
                    data.chicken.setParentsNew(parent2, parent2);
                }
            }
        }
        
        PENDING_CHICKENS.clear();
        LOGGER.info("Parent resolution complete");
    }
    
    private int parseColor(String hex) {
        String cleaned = hex.trim();
        if (cleaned.startsWith("#")) {
            cleaned = cleaned.substring(1);
        } else if (cleaned.startsWith("0x") || cleaned.startsWith("0X")) {
            cleaned = cleaned.substring(2);
        }
        
        try {
            return Integer.parseInt(cleaned, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid color format '{}' for chicken '{}', using white", hex, this.id);
            return 0xFFFFFF;
        }
    }
    
    private String generateDisplayName(String path) {
        String[] parts = path.split("_");
        StringBuilder name = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                name.append(Character.toUpperCase(part.charAt(0)))
                    .append(part.substring(1).toLowerCase())
                    .append(" ");
            }
        }
        name.append("Chicken");
        return name.toString().trim();
    }

    private static ResourceLocation resolveTexturePath(String raw) {
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return DEFAULT_TEXTURE;
        }
        if (trimmed.contains(":")) {
            return ResourceLocation.parse(trimmed);
        }
        String normalised = trimmed.replace('\\', '/');
        if (!normalised.endsWith(".png")) {
            normalised = normalised + ".png";
        }
        if (!normalised.contains("/")) {
            normalised = "textures/entity/" + normalised;
        } else if (normalised.startsWith("entity/") || normalised.startsWith("item/")) {
            normalised = "textures/" + normalised;
        }
        return ResourceLocation.fromNamespaceAndPath(ChickensMod.MOD_ID, normalised);
    }

    private static ResourceLocation resolveItemTexturePath(String raw) {
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return ResourceLocation.fromNamespaceAndPath(ChickensMod.MOD_ID, "textures/item/chicken/whitechicken.png");
        }
        if (trimmed.contains(":")) {
            return ResourceLocation.parse(trimmed);
        }
        String normalised = trimmed.replace('\\', '/');
        if (!normalised.endsWith(".png")) {
            normalised = normalised + ".png";
        }
        if (!normalised.contains("/")) {
            normalised = "textures/item/chicken/" + normalised;
        } else if (normalised.startsWith("item/")) {
            normalised = "textures/" + normalised;
        } else if (normalised.startsWith("chicken/")) {
            normalised = "textures/item/" + normalised;
        }
        return ResourceLocation.fromNamespaceAndPath(ChickensMod.MOD_ID, normalised);
    }
}
