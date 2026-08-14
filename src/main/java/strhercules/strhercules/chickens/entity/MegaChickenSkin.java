package strhercules.chickens.entity;

import strhercules.chickens.ChickensMod;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

/** Direct, validated Mega Chicken skin identifiers for the supplied skin atlas. */
public enum MegaChickenSkin {
    ZOMBIE("zombie", "Zombie Chicken"),
    VALENTINES("valentines", "Valentine's Chicken"),
    TOXIC("toxic", "Toxic Chicken"),
    REPTAR("reptar", "Reptar Chicken"),
    RAMBO("rambo", "Rambo Chicken"),
    PINK("pink", "Pink Chicken"),
    FOX("fox", "Fox Chicken"),
    DUCK("duck", "Duck Chicken"),
    DODO("dodo", "Dodo Chicken"),
    DEEPDARK("deepdark", "Deep Dark Chicken"),
    CREEPER("creeper", "Creeper Chicken"),
    BIGBRAIN("bigbrain", "Big Brain Chicken"),
    AVIATOR("aviator", "Aviator Chicken");

    private final String id;
    private final String fallbackName;

    MegaChickenSkin(String id, String fallbackName) {
        this.id = id;
        this.fallbackName = fallbackName;
    }

    public String id() {
        return id;
    }

    public Component displayName() {
        return Component.translatable("skin." + ChickensMod.MOD_ID + ".mega_chicken." + id,
                fallbackName);
    }

    public ResourceLocation texture() {
        return ResourceLocation.fromNamespaceAndPath(ChickensMod.MOD_ID,
                "textures/entity/megachicken/" + id + "_chicken.png");
    }

    @Nullable
    public static MegaChickenSkin byId(@Nullable String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        for (MegaChickenSkin skin : values()) {
            if (skin.id.equals(id)) {
                return skin;
            }
        }
        return null;
    }
}
