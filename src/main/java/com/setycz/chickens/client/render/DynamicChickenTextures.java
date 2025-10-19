package com.setycz.chickens.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.setycz.chickens.ChickensMod;
import com.setycz.chickens.ChickensRegistryItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Generates simple tinted textures for dynamically created chickens so the
 * overworld entity mirrors the item's colour scheme. Textures are derived
 * from the base white chicken sprite and cached per chicken id.
 */
public final class DynamicChickenTextures {
    private static final Logger LOGGER = LoggerFactory.getLogger("ChickensDynamicTextures");
    private static final ResourceLocation BASE_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ChickensMod.MOD_ID, "textures/entity/whitechicken.png");
    private static final Map<Integer, ResourceLocation> CACHE = new HashMap<>();

    private static NativeImage baseImage;

    private DynamicChickenTextures() {
    }

    public static ResourceLocation textureFor(ChickensRegistryItem chicken) {
        return CACHE.computeIfAbsent(chicken.getId(), id -> generateTexture(chicken));
    }

    private static ResourceLocation generateTexture(ChickensRegistryItem chicken) {
        NativeImage base = getBaseImage();
        if (base == null) {
            return ResourceLocation.fromNamespaceAndPath(
                    ChickensMod.MOD_ID, "textures/entity/unknownchicken.png");
        }

        NativeImage image = new NativeImage(base.getWidth(), base.getHeight(), false);
        int primary = chicken.getBgColor();
        int accent = chicken.getFgColor();
        for (int y = 0; y < base.getHeight(); y++) {
            for (int x = 0; x < base.getWidth(); x++) {
                int rgba = base.getPixelRGBA(x, y);
                int alpha = (rgba >>> 24) & 0xFF;
                if (alpha == 0) {
                    image.setPixelRGBA(x, y, 0);
                    continue;
                }
                int r = (rgba >> 16) & 0xFF;
                int g = (rgba >> 8) & 0xFF;
                int b = rgba & 0xFF;
                float max = Math.max(Math.max(r, g), b) / 255.0f;
                float min = Math.min(Math.min(r, g), b) / 255.0f;
                float saturation = max == 0.0f ? 0.0f : (max - min) / max;

                int resultRgb;
                if (saturation < 0.25f) {
                    float brightness = max;
                    resultRgb = lerpColor(primary, accent, brightness);
                } else {
                    resultRgb = (r << 16) | (g << 8) | b;
                }
                int finalColor = (alpha << 24) | (resultRgb & 0x00FFFFFF);
                image.setPixelRGBA(x, y, finalColor);
            }
        }

        DynamicTexture texture = new DynamicTexture(image);
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                ChickensMod.MOD_ID, "dynamic/chicken_" + chicken.getId());
        Minecraft.getInstance().getTextureManager().register(id, texture);
        return id;
    }

    private static int lerpColor(int start, int end, float amount) {
        float clamped = Math.max(0.0f, Math.min(1.0f, amount));
        int sr = (start >> 16) & 0xFF;
        int sg = (start >> 8) & 0xFF;
        int sb = start & 0xFF;

        int er = (end >> 16) & 0xFF;
        int eg = (end >> 8) & 0xFF;
        int eb = end & 0xFF;

        int r = (int) (sr + (er - sr) * clamped);
        int g = (int) (sg + (eg - sg) * clamped);
        int b = (int) (sb + (eb - sb) * clamped);
        return (r << 16) | (g << 8) | b;
    }

    private static NativeImage getBaseImage() {
        if (baseImage != null) {
            return baseImage;
        }
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(BASE_TEXTURE);
        if (resource.isEmpty()) {
            LOGGER.warn("Unable to load base chicken texture {}", BASE_TEXTURE);
            return null;
        }
        try (InputStream stream = resource.get().open()) {
            baseImage = NativeImage.read(stream);
        } catch (IOException e) {
            LOGGER.warn("Failed to read base chicken texture {}", BASE_TEXTURE, e);
            baseImage = null;
        }
        return baseImage;
    }

    public static void clear() {
        CACHE.clear();
        baseImage = null;
    }

    public static SimplePreparableReloadListener<Void> reloadListener() {
        return new SimplePreparableReloadListener<>() {
            @Override
            protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
                return null;
            }

            @Override
            protected void apply(Void object, ResourceManager resourceManager, ProfilerFiller profiler) {
                clear();
            }
        };
    }
}
