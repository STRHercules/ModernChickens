package com.setycz.chickens.client.render;

import com.mojang.datafixers.util.Either;
import com.mojang.math.Transformation;
import com.setycz.chickens.ChickensMod;
import com.setycz.chickens.ChickensRegistryItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.inventory.InventoryMenu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Builds lightweight baked models for chicken items that do not have a static
 * JSON override. The generator mirrors the structure of the baked "generated"
 * item models so that standard tinting continues to function. When a custom
 * sprite cannot be found the handler falls back to the vanilla white chicken
 * icon so players see a coloured item rather than a missing-texture placeholder.
 */
public final class ChickenItemSpriteModels {
    private static final Logger LOGGER = LoggerFactory.getLogger("ChickensCustomItemSprites");
    private static final ResourceLocation DEFAULT_ITEM_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ChickensMod.MOD_ID, "textures/item/chicken/whitechicken.png");
    private static final ModelState IDENTITY = new ModelState() {
        @Override
        public Transformation getRotation() {
            return Transformation.identity();
        }

        @Override
        public boolean isUvLocked() {
            return false;
        }
    };

    private static final Map<Integer, BakedModel> CACHE = new HashMap<>();
    private static final Set<ResourceLocation> VERIFIED_TEXTURES = new HashSet<>();
    private static final Set<ResourceLocation> LOGGED_MISSING_TEXTURES = new HashSet<>();

    private ChickenItemSpriteModels() {
    }

    @Nullable
    static BakedModel bake(ChickensRegistryItem chicken, ModelBakery bakery) {
        return CACHE.computeIfAbsent(chicken.getId(), id -> bakeInternal(chicken, bakery));
    }

    @Nullable
    private static BakedModel bakeInternal(ChickensRegistryItem chicken, ModelBakery bakery) {
        ResourceLocation texture = selectTexture(chicken);
        if (!hasTexture(texture)) {
            if (LOGGED_MISSING_TEXTURES.add(texture)) {
                LOGGER.warn("Unable to locate chicken item texture {}; falling back to {}", texture, DEFAULT_ITEM_TEXTURE);
            }
            texture = DEFAULT_ITEM_TEXTURE;
            if (!hasTexture(texture)) {
                return null;
            }
        }

        ResourceLocation spriteLocation = toSpriteLocation(texture);
        Material material = new Material(InventoryMenu.BLOCK_ATLAS, spriteLocation);
        Function<Material, TextureAtlasSprite> sprites = key -> Minecraft.getInstance()
                .getModelManager()
                .getAtlas(key.atlasLocation())
                .getSprite(key.texture());

        Map<String, Either<Material, String>> textures = Map.of("layer0", Either.left(material));
        BlockModel model = new BlockModel(null, List.of(), textures, true, null, ItemTransforms.NO_TRANSFORMS,
                List.of());

        ResourceLocation bakedId = ResourceLocation.fromNamespaceAndPath(ChickensMod.MOD_ID,
                "dynamic/item/chicken_" + chicken.getId());
        return model.bake((ModelBaker) bakery, model, sprites, IDENTITY, false);
    }

    static void clear() {
        CACHE.clear();
        VERIFIED_TEXTURES.clear();
        LOGGED_MISSING_TEXTURES.clear();
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

    private static ResourceLocation selectTexture(ChickensRegistryItem chicken) {
        if (chicken.getItemTexture() != null) {
            return chicken.getItemTexture();
        }
        String name = chicken.getEntityName().toLowerCase(Locale.ROOT);
        return ResourceLocation.fromNamespaceAndPath(ChickensMod.MOD_ID, "textures/item/chicken/" + name + ".png");
    }

    private static ResourceLocation toSpriteLocation(ResourceLocation texture) {
        String path = texture.getPath();
        if (path.startsWith("textures/")) {
            path = path.substring("textures/".length());
        }
        if (path.endsWith(".png")) {
            path = path.substring(0, path.length() - ".png".length());
        }
        return ResourceLocation.fromNamespaceAndPath(texture.getNamespace(), path);
    }

    private static boolean hasTexture(ResourceLocation texture) {
        if (VERIFIED_TEXTURES.contains(texture)) {
            return true;
        }
        ResourceManager manager = Minecraft.getInstance().getResourceManager();
        boolean present = manager.getResource(texture).isPresent();
        if (present) {
            VERIFIED_TEXTURES.add(texture);
        }
        return present;
    }
}
