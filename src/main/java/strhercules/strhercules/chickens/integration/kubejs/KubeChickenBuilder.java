package strhercules.chickens.integration.kubejs;

import strhercules.chickens.ChickensMod;
import strhercules.chickens.ChickensRegistryItem;
import strhercules.chickens.SpawnType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Locale;


public class KubeChickenBuilder {
    private static final ResourceLocation DEFAULT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ChickensMod.MOD_ID, "textures/entity/whitechicken.png");

    private final String entityName;

    @Nullable
    private Integer id;
    @Nullable
    private String displayName;
    private ItemStack layItem = ItemStack.EMPTY;
    private ItemStack dropItem = ItemStack.EMPTY;
    @Nullable
    private String parent1;
    @Nullable
    private String parent2;
    @Nullable
    private Integer tier;
    private SpawnType spawnType = SpawnType.NONE;
    private int primaryColor = 0xFFFFFF;
    private int secondaryColor = 0xFFFF00;
    private float layCoefficient = 1.0f;
    @Nullable
    private Boolean generatedTexture;
    @Nullable
    private String texturePath;
    @Nullable
    private String itemTexture;
    private boolean enabled = true;
    private boolean allowNaturalSpawn;
    private boolean allowDousing;
    @Nullable
    private Integer liquidDousingCost;

    KubeChickenBuilder(String entityName) {
        this.entityName = entityName;
    }

    public String getEntityName() {
        return entityName;
    }

    @Nullable
    String parent1Name() {
        return parent1;
    }

    @Nullable
    String parent2Name() {
        return parent2;
    }

    @Nullable
    Integer requestedId() {
        return id;
    }

   
    public KubeChickenBuilder id(int value) {
        this.id = value;
        return this;
    }

    public KubeChickenBuilder displayName(String value) {
        this.displayName = value;
        return this;
    }

   public KubeChickenBuilder layItem(ItemStack stack) {
        this.layItem = stack == null ? ItemStack.EMPTY : stack.copy();
        return this;
    }

    public KubeChickenBuilder layItem(ItemStack stack, int count) {
        return layItem(withCount(stack, count));
    }

    /** Item dropped on death. Defaults to the lay item. */
    public KubeChickenBuilder dropItem(ItemStack stack) {
        this.dropItem = stack == null ? ItemStack.EMPTY : stack.copy();
        return this;
    }

    public KubeChickenBuilder dropItem(ItemStack stack, int count) {
        return dropItem(withCount(stack, count));
    }

    public KubeChickenBuilder parent1(String value) {
        this.parent1 = normaliseName(value);
        return this;
    }

    public KubeChickenBuilder parent2(String value) {
        this.parent2 = normaliseName(value);
        return this;
    }

   public KubeChickenBuilder parents(String first, String second) {
        return parent1(first).parent2(second);
    }

    public KubeChickenBuilder tier(int value) {
        this.tier = Math.max(1, value);
        return this;
    }

    public KubeChickenBuilder spawnType(String value) {
        this.spawnType = parseSpawnType(value);
        return this;
    }

    public KubeChickenBuilder primaryColor(Object value) {
        this.primaryColor = parseColour(value, primaryColor);
        return this;
    }

    public KubeChickenBuilder secondaryColor(Object value) {
        this.secondaryColor = parseColour(value, secondaryColor);
        return this;
    }

    public KubeChickenBuilder layCoefficient(double value) {
        this.layCoefficient = (float) Math.max(0.0D, value);
        return this;
    }

    public KubeChickenBuilder generatedTexture(boolean value) {
        this.generatedTexture = value;
        return this;
    }

    /** Custom entity texture, e.g. {@code 'chickens:textures/entity/my_chicken.png'}. */
    public KubeChickenBuilder texturePath(String value) {
        this.texturePath = value;
        return this;
    }

    public KubeChickenBuilder itemTexture(String value) {
        this.itemTexture = value;
        return this;
    }

    public KubeChickenBuilder enabled(boolean value) {
        this.enabled = value;
        return this;
    }

    /** Lets a chicken above tier 1 spawn naturally. */
    public KubeChickenBuilder allowNaturalSpawn(boolean value) {
        this.allowNaturalSpawn = value;
        return this;
    }

    public KubeChickenBuilder allowDousing(boolean value) {
        this.allowDousing = value;
        return this;
    }

    /** Fluid cost in mB for dousing this chicken. */
    public KubeChickenBuilder liquidDousingCost(int value) {
        this.liquidDousingCost = Math.max(1, value);
        return this;
    }

  
    ChickensRegistryItem build(int resolvedId) {
        if (layItem.isEmpty()) {
            throw new IllegalArgumentException("chicken '" + entityName + "' has no layItem");
        }

        ResourceLocation texture = resolveTexture();
        boolean generated = generatedTexture != null ? generatedTexture : texturePath == null;

        ChickensRegistryItem chicken = new ChickensRegistryItem(resolvedId, entityName, texture, layItem,
                primaryColor, secondaryColor).markCustom();
        chicken.setGeneratedTexture(generated);
        chicken.setSpawnType(spawnType);
        chicken.setLayCoefficient(layCoefficient);
        chicken.setEnabled(enabled);
        chicken.setNaturalSpawnOverride(allowNaturalSpawn);
        chicken.setDousingAllowed(allowDousing);

        if (!dropItem.isEmpty()) {
            chicken.setDropItem(dropItem);
        }
       chicken.setDisplayName(Component.literal(
                displayName != null && !displayName.isEmpty() ? displayName : titleCase(entityName)));
        if (tier != null) {
            chicken.setTier(tier);
        }
        if (liquidDousingCost != null) {
            chicken.setLiquidDousingCost(liquidDousingCost);
        }
        if (itemTexture != null && !itemTexture.isEmpty()) {
            ResourceLocation sprite = ResourceLocation.tryParse(itemTexture);
            if (sprite != null) {
                chicken.setItemTexture(sprite);
            } else {
                ChickenRegistryKubeEvent.LOGGER.warn("Chicken '{}' has a malformed itemTexture '{}'; ignoring",
                        entityName, itemTexture);
            }
        }

        return chicken;
    }

    private ResourceLocation resolveTexture() {
        if (texturePath == null || texturePath.isEmpty()) {
            return DEFAULT_TEXTURE;
        }
        ResourceLocation parsed = ResourceLocation.tryParse(texturePath);
        if (parsed != null) {
            return parsed;
        }
        ChickenRegistryKubeEvent.LOGGER.warn("Chicken '{}' has a malformed texturePath '{}'; using the default sprite",
                entityName, texturePath);
        return DEFAULT_TEXTURE;
    }

    private static String titleCase(String name) {
        StringBuilder result = new StringBuilder(name.length() + 4);
        boolean capitalise = true;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '_' || c == '-' || c == ' ') {
                if (!result.isEmpty() && result.charAt(result.length() - 1) != ' ') {
                    result.append(' ');
                }
                capitalise = true;
                continue;
            }
            if (Character.isUpperCase(c) && !result.isEmpty() && result.charAt(result.length() - 1) != ' ') {
                result.append(' ');
                capitalise = true;
            }
            result.append(capitalise ? Character.toUpperCase(c) : c);
            capitalise = false;
        }
        return result.toString();
    }

    private static ItemStack withCount(ItemStack stack, int count) {
        if (stack == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack copy = stack.copy();
        copy.setCount(Math.max(1, Math.min(count, copy.getMaxStackSize())));
        return copy;
    }

    @Nullable
    private static String normaliseName(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        int separator = trimmed.indexOf(':');
        return separator < 0 ? trimmed : trimmed.substring(separator + 1);
    }

    private SpawnType parseSpawnType(@Nullable String value) {
        if (value == null || value.isEmpty()) {
            return SpawnType.NONE;
        }
        try {
            return SpawnType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            ChickenRegistryKubeEvent.LOGGER.warn("Chicken '{}' has an unknown spawnType '{}'; using NONE",
                    entityName, value);
            return SpawnType.NONE;
        }
    }

    private int parseColour(@Nullable Object value, int fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number number) {
            return clampColour(number.intValue(), fallback, value);
        }

        String raw = value.toString().trim();
        if (raw.startsWith("#")) {
            raw = raw.substring(1);
        } else if (raw.startsWith("0x") || raw.startsWith("0X")) {
            raw = raw.substring(2);
        }
        try {
            return clampColour(Integer.parseUnsignedInt(raw, 16), fallback, value);
        } catch (NumberFormatException ex) {
            ChickenRegistryKubeEvent.LOGGER.warn("Chicken '{}' could not parse colour '{}'; using default",
                    entityName, value);
            return fallback;
        }
    }

    private int clampColour(int value, int fallback, Object raw) {
        if (value < 0 || value > 0xFFFFFF) {
            ChickenRegistryKubeEvent.LOGGER.warn("Chicken '{}' has an out-of-range colour '{}'; using default",
                    entityName, raw);
            return fallback;
        }
        return value;
    }
}
