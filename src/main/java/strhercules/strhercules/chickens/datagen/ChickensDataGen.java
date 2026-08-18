package strhercules.chickens.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.data.event.GatherDataEvent;

import strhercules.chickens.ChickensMod;
import strhercules.chickens.datagen.providers.ChickenItemModelProvider;
import strhercules.chickens.datagen.providers.ChickensBlockModelProvider;
import strhercules.chickens.datagen.providers.ChickensItemModelProvider;
import strhercules.chickens.datagen.providers.ChickensRecipeProvider;
import strhercules.chickens.datagen.providers.ChickensLootTableProvider;
import strhercules.chickens.datagen.providers.ChickensItemTagProvider;

import java.util.concurrent.CompletableFuture;

@Mod.EventBusSubscriber(modid = ChickensMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ChickensDataGen {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        PackOutput packOutput = event.getGenerator().getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        event.getGenerator().addProvider(
                event.includeServer(),
                new ChickensRecipeProvider(packOutput, lookupProvider)
        );

        event.getGenerator().addProvider(
                event.includeServer(),
                new ChickensLootTableProvider(packOutput, lookupProvider)
        );

        event.getGenerator().addProvider(
                event.includeServer(),
                new ChickensItemTagProvider(packOutput, lookupProvider)
        );

        event.getGenerator().getVanillaPack(true).addProvider(ChickenItemModelProvider::new);

        // Block models (previously assets/chickens/models/block/*.json), now datagen'd.
        event.getGenerator().addProvider(
                event.includeClient(),
                new ChickensBlockModelProvider(packOutput, event.getExistingFileHelper())
        );

        event.getGenerator().addProvider(
                event.includeClient(),
                new ChickensItemModelProvider(packOutput, event.getExistingFileHelper())
        );
    }
}
