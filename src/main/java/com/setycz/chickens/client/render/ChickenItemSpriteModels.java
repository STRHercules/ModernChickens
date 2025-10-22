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
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.inventory.InventoryMenu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
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
    private static final ResourceLocation GENERATED_PARENT = ResourceLocation.withDefaultNamespace("item/generated");

    private static final Map<Integer, BakedModel> CACHE = new HashMap<>();
    private static final Set<ResourceLocation> LOGGED_MISSING_TEXTURES = new HashSet<>();
    private static boolean loggedBakerFailure;
    private static boolean loggedParentFailure;
    @Nullable
    private static Constructor<?> bakerConstructor;
    @Nullable
    private static Method bakeryModelGetter;

    private ChickenItemSpriteModels() {
    }

    @Nullable
    static BakedModel bake(ChickensRegistryItem chicken, ModelBakery bakery) {
        return CACHE.computeIfAbsent(chicken.getId(), id -> bakeInternal(chicken, bakery));
    }

    @Nullable
    private static BakedModel bakeInternal(ChickensRegistryItem chicken, ModelBakery bakery) {
        ResourceLocation texture = selectTexture(chicken);
        ResourceLocation requestedTexture = texture;
        boolean hasExplicitTexture = chicken.getItemTexture() != null;
        boolean customDefinition = chicken.isCustom();
        boolean authoritativeTexture = customDefinition && hasExplicitTexture;

        ResourceLocation spriteLocation = toSpriteLocation(texture);
        if (hasExplicitTexture) {
            BakedModel prebaked = tryFetchExistingModel(spriteLocation);
            if (prebaked != null) {
                chicken.setTintItem(false);
                return prebaked;
            }
        }

        boolean disableTint = false;
        // Confirm the PNG exists before baking so missing datapack sprites fall
        // back to the default icon while still allowing custom chickens to show
        // Minecraft's missing-texture indicator when explicitly requested.
        boolean available = hasTexture(texture);
        if (!available) {
            if (authoritativeTexture) {
                if (LOGGED_MISSING_TEXTURES.add(requestedTexture)) {
                    LOGGER.warn("Unable to locate custom chicken item texture {}; leaving missing sprite in place", requestedTexture);
                }
                chicken.setTintItem(false);
            } else {
                if (LOGGED_MISSING_TEXTURES.add(texture)) {
                    LOGGER.warn("Unable to locate chicken item texture {}; falling back to {}", texture, DEFAULT_ITEM_TEXTURE);
                }
                chicken.setTintItem(true);
                texture = DEFAULT_ITEM_TEXTURE;
                spriteLocation = toSpriteLocation(texture);
            }
        } else if (hasExplicitTexture) {
            disableTint = true;
        }

        Material material = materialFor(spriteLocation);

        Function<Material, TextureAtlasSprite> sprites = key -> Minecraft.getInstance().getModelManager()
                .getAtlas(key.atlasLocation()).getSprite(key.texture());

        Map<String, Either<Material, String>> textures = Map.of("layer0", Either.left(material));
        BlockModel model = new BlockModel(GENERATED_PARENT, List.of(), textures, true, null, ItemTransforms.NO_TRANSFORMS,
                List.of());
        try {
            model.resolveParents(id -> fetchUnbakedModel(bakery, id));
        } catch (RuntimeException exception) {
            logParentFailure(exception);
            if (disableTint) {
                chicken.setTintItem(true);
            }
            return null;
        }

        ResourceLocation dynamicId = ResourceLocation.fromNamespaceAndPath(ChickensMod.MOD_ID,
                "dynamic/item/chicken_" + chicken.getId());
        ModelResourceLocation bakeLocation = new ModelResourceLocation(dynamicId, "inventory");
        ModelBaker baker = instantiateBaker(bakery, bakeLocation, sprites);
        if (baker == null) {
            // Allow the vanilla override pipeline to continue colouring the
            // fallback sprite when we cannot dynamically bake the bespoke
            // model. This keeps the item visible instead of showing a blank
            // icon.
            if (disableTint) {
                chicken.setTintItem(true);
            }
            return null;
        }
        BakedModel baked = model.bake(baker, model, sprites, IDENTITY, false);
        if (disableTint) {
            chicken.setTintItem(false);
        }
        return baked;
    }

    static void clear() {
        CACHE.clear();
        LOGGED_MISSING_TEXTURES.clear();
        loggedBakerFailure = false;
        loggedParentFailure = false;
        bakeryModelGetter = null;
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

    static ResourceLocation selectTexture(ChickensRegistryItem chicken) {
        if (chicken.getItemTexture() != null) {
            return chicken.getItemTexture();
        }
        String name = chicken.getEntityName().toLowerCase(Locale.ROOT);
        return ResourceLocation.fromNamespaceAndPath(ChickensMod.MOD_ID, "textures/item/chicken/" + name + ".png");
    }

    static ResourceLocation toSpriteLocation(ResourceLocation texture) {
        String path = texture.getPath();
        if (path.startsWith("textures/")) {
            path = path.substring("textures/".length());
        }
        if (path.endsWith(".png")) {
            path = path.substring(0, path.length() - ".png".length());
        }
        return ResourceLocation.fromNamespaceAndPath(texture.getNamespace(), path);
    }

    private static Material materialFor(ResourceLocation spriteLocation) {
        ResourceLocation atlas = resolveAtlas(spriteLocation);
        return new Material(atlas, spriteLocation);
    }

    private static ResourceLocation resolveAtlas(ResourceLocation spriteLocation) {
        // All chicken item sprites are stitched into the shared inventory
        // atlas via assets/chickens/atlases/blocks.json, so dynamic lookups
        // must target the same sheet to match the baked JSON models.
        return InventoryMenu.BLOCK_ATLAS;
    }

    private static boolean hasTexture(ResourceLocation texture) {
        return Minecraft.getInstance().getResourceManager().getResource(texture).isPresent();
    }

    @Nullable
    private static BakedModel tryFetchExistingModel(ResourceLocation spriteLocation) {
        ModelResourceLocation modelLocation = new ModelResourceLocation(spriteLocation, "inventory");
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(modelLocation);
        if (model != null && model != Minecraft.getInstance().getModelManager().getMissingModel()) {
            return model;
        }
        return null;
    }

    @Nullable
    private static ModelBaker instantiateBaker(ModelBakery bakery, ModelResourceLocation modelId,
            Function<Material, TextureAtlasSprite> sprites) {
        // Forge exposes the runtime baker as a package-private inner class, so
        // reflectively bridge to the official implementation rather than
        // duplicating the caching and missing-model handling logic.
        try {
            if (bakerConstructor == null) {
                Class<?> impl = Class.forName("net.minecraft.client.resources.model.ModelBakery$ModelBakerImpl");
                Constructor<?> ctor = impl.getDeclaredConstructor(ModelBakery.class, ModelBakery.TextureGetter.class,
                        ModelResourceLocation.class);
                ctor.setAccessible(true);
                bakerConstructor = ctor;
            }
            ModelBakery.TextureGetter getter = (location, material) -> sprites.apply(material);
            return (ModelBaker) bakerConstructor.newInstance(bakery, getter, modelId);
        } catch (ReflectiveOperationException exception) {
            if (!loggedBakerFailure) {
                LOGGER.error("Unable to create ModelBakery baker for {}; custom chicken item sprite will be skipped", modelId,
                        exception);
                loggedBakerFailure = true;
            }
            return null;
        }
    }

    private static void logParentFailure(RuntimeException exception) {
        if (!loggedParentFailure) {
            LOGGER.error("Failed to resolve minecraft:item/generated while baking a custom chicken item sprite", exception);
            loggedParentFailure = true;
        }
    }

    private static UnbakedModel fetchUnbakedModel(ModelBakery bakery, ResourceLocation id) {
        try {
            if (bakeryModelGetter == null) {
                Method method = ModelBakery.class.getDeclaredMethod("getModel", ResourceLocation.class);
                method.setAccessible(true);
                bakeryModelGetter = method;
            }
            return (UnbakedModel) bakeryModelGetter.invoke(bakery, id);
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException("Unable to access model '" + id + "' for custom chicken item sprite baking", exception);
        }
    }
}
