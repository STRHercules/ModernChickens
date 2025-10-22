package com.setycz.chickens.client.render;

import com.setycz.chickens.ChickensRegistryItem;
import com.setycz.chickens.item.ChickenItemHelper;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Override handler that mirrors the behaviour of the baked chicken item model
 * but adds support for custom chickens defined in {@code chickens_custom.json}.
 * When the vanilla override list does not contain a matching entry the handler
 * falls back to dynamically baking a sprite model derived from the chicken's
 * configured item texture.
 */
final class CustomChickenItemOverrides extends ItemOverrides {
    private static final Logger LOGGER = LoggerFactory.getLogger("ChickensCustomItemModels");

    private final ItemOverrides delegate;
    private final ModelBakery bakery;
    private final Map<Integer, BakedModel> cache = new HashMap<>();

    CustomChickenItemOverrides(ItemOverrides delegate, ModelBakery bakery) {
        super();
        this.delegate = delegate;
        this.bakery = bakery;
    }

    @Override
    public BakedModel resolve(BakedModel originalModel, ItemStack stack, @Nullable ClientLevel level,
            @Nullable LivingEntity entity, int seed) {
        BakedModel resolved = delegate != null ? delegate.resolve(originalModel, stack, level, entity, seed) : null;
        if (resolved != null && resolved != originalModel) {
            return resolved;
        }

        ChickensRegistryItem chicken = ChickenItemHelper.resolve(stack);
        if (chicken == null) {
            return originalModel;
        }

        BakedModel cached = cache.get(chicken.getId());
        if (cached != null) {
            return cached;
        }

        BakedModel baked = ChickenItemSpriteModels.bake(chicken, bakery);
        if (baked == null) {
            LOGGER.warn("Falling back to default chicken item model for {} due to missing sprite", chicken.getEntityName());
            return originalModel;
        }

        cache.put(chicken.getId(), baked);
        return baked;
    }
}
