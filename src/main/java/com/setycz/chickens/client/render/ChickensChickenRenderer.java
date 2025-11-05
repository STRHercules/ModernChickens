package com.setycz.chickens.client.render;

import com.setycz.chickens.ChickensRegistry;
import com.setycz.chickens.ChickensRegistryItem;
import com.setycz.chickens.entity.ChickensChicken;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ChickenModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.ChickenRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.animal.Chicken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Renderer that mirrors the vanilla chicken visuals but swaps the texture based
 * on the chicken registry entry. This restores the per-breed skins from the
 * original mod.
 */
public class ChickensChickenRenderer extends ChickenRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger("ChickensChickenRenderer");
    private static final Set<ResourceLocation> VERIFIED_TEXTURES = new HashSet<>();
    private static final Set<ResourceLocation> LOGGED_MISSING_TEXTURES = new HashSet<>();

    public ChickensChickenRenderer(EntityRendererProvider.Context context) {
        super(context);
        // Mirror the Fluid Cows setup by decorating the base model with a
        // translucent, animated fluid layer for liquid chickens.
        this.addLayer(new LiquidChickenOverlayLayer(this, new ChickenModel<>(context.bakeLayer(ModelLayers.CHICKEN))));
    }

    @Override
    public ResourceLocation getTextureLocation(Chicken chicken) {
        if (chicken instanceof ChickensChicken modChicken) {
            ChickensRegistryItem description = ChickensRegistry.getByType(modChicken.getChickenType());
            if (description != null) {
                if (description.hasGeneratedTexture()) {
                    return DynamicChickenTextures.textureFor(description);
                }

                ResourceLocation texture = description.getTexture();
                if (hasTexture(texture)) {
                    return texture;
                }

                if (LOGGED_MISSING_TEXTURES.add(texture)) {
                    LOGGER.warn(
                            "Falling back to generated texture for chicken {} because {} was unavailable", description.getEntityName(),
                            texture);
                }

                // Fallback to the tint pipeline so players see a coloured chicken instead of
                // the purple/black missing-texture placeholder when a resource pack is absent
                // or a path is mis-typed.
                return DynamicChickenTextures.textureFor(description);
            }
        }
        return super.getTextureLocation(chicken);
    }

    private static boolean hasTexture(ResourceLocation texture) {
        if (VERIFIED_TEXTURES.contains(texture)) {
            return true;
        }
        boolean present = Minecraft.getInstance().getResourceManager().getResource(texture).isPresent();
        if (present) {
            VERIFIED_TEXTURES.add(texture);
        }
        return present;
    }

    public static SimplePreparableReloadListener<Void> textureAvailabilityReloader() {
        return new SimplePreparableReloadListener<>() {
            @Override
            protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
                return null;
            }

            @Override
            protected void apply(Void object, ResourceManager resourceManager, ProfilerFiller profiler) {
                clearTextureCaches();
            }
        };
    }

    private static void clearTextureCaches() {
        VERIFIED_TEXTURES.clear();
        LOGGED_MISSING_TEXTURES.clear();
    }
}
