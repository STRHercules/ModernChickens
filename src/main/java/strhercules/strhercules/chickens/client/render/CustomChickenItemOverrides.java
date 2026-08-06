package strhercules.chickens.client.render;

import strhercules.chickens.ChickensMod;
import strhercules.chickens.ChickensRegistryItem;
import strhercules.chickens.item.ChickenItemHelper;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;


final class CustomChickenItemOverrides extends ItemOverrides {
    private static final Logger LOGGER = LoggerFactory.getLogger("ChickensCustomItemModels");

    private final ModelBakery bakery;
    private final Map<Integer, BakedModel> cache = new HashMap<>();

    CustomChickenItemOverrides(@Nullable ItemOverrides delegate, ModelBakery bakery) {
        super();
        this.bakery = bakery;
    }

    @Override
    public BakedModel resolve(BakedModel originalModel, ItemStack stack, @Nullable ClientLevel level,
            @Nullable LivingEntity entity, int seed) {
        if (ChickenItemHelper.isRooster(stack)) {
            BakedModel cachedRooster = cache.get(ChickenItemHelper.ROOSTER_MODEL_ID);
            if (cachedRooster != null) {
                return cachedRooster;
            }
            ChickensRegistryItem stub = new ChickensRegistryItem(
                    ChickenItemHelper.ROOSTER_MODEL_ID,
                    "Rooster",
                    ResourceLocation.withDefaultNamespace("textures/entity/chicken.png"),
                    ItemStack.EMPTY,
                    0xFFFFFF,
                    0xFFFFFF
            );
            stub.setItemTexture(ResourceLocation.fromNamespaceAndPath(ChickensMod.MOD_ID, "textures/item/rooster.png"));
            BakedModel bakedRooster = ChickenItemSpriteModels.bake(stub, bakery);
            if (bakedRooster != null) {
                cache.put(ChickenItemHelper.ROOSTER_MODEL_ID, bakedRooster);
                return bakedRooster;
            }
            return originalModel;
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
            chicken.setTintItem(true);
            LOGGER.warn("Falling back to default chicken item model for {} due to missing sprite", chicken.getEntityName());
            return originalModel;
        }

        cache.put(chicken.getId(), baked);
        return baked;
    }
}