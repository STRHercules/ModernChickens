package strhercules.chickens.integration.kubejs;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

/** Fluent KubeJS override for an automatically generated fluid or chemical chicken. */
public final class KubeLineageBuilder {
    @Nullable
    private final ResourceLocation resourceId;
    private final boolean chemical;
    @Nullable
    private String parent1;
    @Nullable
    private String parent2;
    private boolean parentsSet;
    @Nullable
    private Boolean dousingAllowed;
    @Nullable
    private Integer spawnWeight;

    KubeLineageBuilder(@Nullable ResourceLocation resourceId, boolean chemical) {
        this.resourceId = resourceId;
        this.chemical = chemical;
    }

    @Nullable
    ResourceLocation resourceId() {
        return resourceId;
    }

    boolean chemical() {
        return chemical;
    }

    @Nullable
    String parent1() {
        return parent1;
    }

    @Nullable
    String parent2() {
        return parent2;
    }

    boolean parentsSet() {
        return parentsSet;
    }

    @Nullable
    Boolean dousingAllowed() {
        return dousingAllowed;
    }

    @Nullable
    Integer spawnWeight() {
        return spawnWeight;
    }

    public KubeLineageBuilder parents(String first, String second) {
        parent1 = normaliseName(first);
        parent2 = normaliseName(second);
        parentsSet = true;
        return this;
    }

    public KubeLineageBuilder parent1(String value) {
        parent1 = normaliseName(value);
        parentsSet = true;
        return this;
    }

    public KubeLineageBuilder parent2(String value) {
        parent2 = normaliseName(value);
        parentsSet = true;
        return this;
    }

    public KubeLineageBuilder clearParents() {
        parent1 = null;
        parent2 = null;
        parentsSet = true;
        return this;
    }

    public KubeLineageBuilder allowDousing(boolean value) {
        dousingAllowed = value;
        return this;
    }

    public KubeLineageBuilder spawnWeight(int value) {
        spawnWeight = Math.max(0, value);
        return this;
    }

    @Nullable
    private static String normaliseName(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        int separator = trimmed.indexOf(':');
        return separator < 0 ? trimmed : trimmed.substring(separator + 1);
    }
}
