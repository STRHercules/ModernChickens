package strhercules.chickens.datagen.providers;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class ChickensItemTagProvider extends ItemTagsProvider {

    public ChickensItemTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(
                output,
                lookupProvider,
                CompletableFuture.completedFuture((TagsProvider.TagLookup<Block>) key -> Optional.empty())
        );
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(TagKey.create(Registries.ITEM, ResourceLocation.parse("chickens:chemical_egg")))
                .add(item("chickens:chemical_egg"));
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
    }
}