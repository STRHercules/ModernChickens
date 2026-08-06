package strhercules.chickens.datagen.providers;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.functions.CopyNameFunction;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import net.minecraft.data.loot.BlockLootSubProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ChickensBlockLootSubProvider extends BlockLootSubProvider {

    private static final String[] SIMPLE_DROP_BLOCKS = {
            "chickens:breeder",
            "chickens:collector",
            "chickens:incubator",
            "chickens:roost",
            "chickens:avian_dousing_machine",
            "chickens:avian_fluid_converter",
            "chickens:avian_flux_converter"
    };

    private static final String[] HENHOUSE_BLOCKS = {
            "chickens:henhouse",
            "chickens:henhouse_acacia",
            "chickens:henhouse_birch",
            "chickens:henhouse_dark_oak",
            "chickens:henhouse_jungle",
            "chickens:henhouse_spruce"
    };

    protected ChickensBlockLootSubProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        for (String id : SIMPLE_DROP_BLOCKS) {
            add(block(id), simpleDrop(block(id)));
        }

        for (String id : HENHOUSE_BLOCKS) {
            add(block(id), dropWithCopyName(block(id)));
        }

        Block chemicalConverter = block("chickens:avian_chemical_converter");
        add(chemicalConverter, dropWithCopyComponents(chemicalConverter));
    }

    private LootTable.Builder simpleDrop(Block block) {
        return LootTable.lootTable().withPool(
                LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1))
                        .add(LootItem.lootTableItem(block))
                        .when(ExplosionCondition.survivesExplosion())
        );
    }

    private LootTable.Builder dropWithCopyName(Block block) {
        return LootTable.lootTable().withPool(
                LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1))
                        .add(LootItem.lootTableItem(block)
                                .apply(CopyNameFunction.copyName(CopyNameFunction.NameSource.BLOCK_ENTITY)))
                        .when(ExplosionCondition.survivesExplosion())
        );
    }

    private LootTable.Builder dropWithCopyComponents(Block block) {
        return LootTable.lootTable().withPool(
                LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1))
                        .setBonusRolls(ConstantValue.exactly(0))
                        .add(LootItem.lootTableItem(block)
                                .apply(CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)))
        );
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        List<Block> blocks = new ArrayList<>();
        for (String id : SIMPLE_DROP_BLOCKS) blocks.add(block(id));
        for (String id : HENHOUSE_BLOCKS) blocks.add(block(id));
        blocks.add(block("chickens:avian_chemical_converter"));
        return blocks;
    }

    private static Block block(String id) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));
    }
}