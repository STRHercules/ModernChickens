package strhercules.chickens.registry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class ModTags {
    public static final TagKey<Item> STORAGE_BLOCKS_WHEAT =
            ItemTags.create(new ResourceLocation("forge", "storage_blocks/wheat"));

    private ModTags() {
    }
}
