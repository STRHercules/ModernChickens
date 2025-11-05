package com.modernfluidcows.client;

import com.modernfluidcows.ModernFluidCows;
import com.modernfluidcows.client.render.FluidCowRenderer;
import com.modernfluidcows.client.render.StallBlockEntityRenderer;
import com.modernfluidcows.client.screen.AcceleratorScreen;
import com.modernfluidcows.client.screen.FeederScreen;
import com.modernfluidcows.client.screen.SorterScreen;
import com.modernfluidcows.client.screen.StallScreen;
import com.modernfluidcows.registry.FluidCowsRegistries;
import java.util.List;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;

/** Client-only bootstrap that registers menu screens. */
public final class ModernFluidCowsClient {
    private ModernFluidCowsClient() {}

    public static void bootstrap(final IEventBus modBus) {
        modBus.addListener(ModernFluidCowsClient::onRegisterMenuScreens);
        // Register entity renderers during the dedicated NeoForge callback so the fluid cow gains
        // its bespoke overlay layer when the client model bakery initialises.
        modBus.addListener(ModernFluidCowsClient::onRegisterRenderers);
        // Ensure every bespoke model authored for the NeoForge port is queued ahead of the bakery
        // so they are available when block items and menus reference their resources.
        modBus.addListener(ModernFluidCowsClient::onRegisterAdditionalModels);
        // Surface the bundled legacy art as a built-in resource pack so players can toggle it
        // from the client resource-pack screen without copying files out of the mod jar.
        modBus.addListener(ModernFluidCowsClient::onAddPackFinders);
    }

    private static void onRegisterMenuScreens(final RegisterMenuScreensEvent event) {
        event.register(FluidCowsRegistries.STALL_MENU.get(), StallScreen::new);
        event.register(FluidCowsRegistries.ACCELERATOR_MENU.get(), AcceleratorScreen::new);
        event.register(FluidCowsRegistries.FEEDER_MENU.get(), FeederScreen::new);
        event.register(FluidCowsRegistries.SORTER_MENU.get(), SorterScreen::new);
    }

    private static void onRegisterRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(FluidCowsRegistries.FLUID_COW.get(), FluidCowRenderer::new);
        // Attach the stall renderer so captured cows are visible inside the block on the client.
        event.registerBlockEntityRenderer(
                FluidCowsRegistries.STALL_BLOCK_ENTITY.get(), StallBlockEntityRenderer::new);
    }

    private static final List<ModelResourceLocation> BLOCK_MODELS =
            List.of(
                    ModelResourceLocation.standalone(
                            ResourceLocation.fromNamespaceAndPath(
                                    ModernFluidCows.MOD_ID, "block/accelerator")),
                    ModelResourceLocation.standalone(
                            ResourceLocation.fromNamespaceAndPath(
                                    ModernFluidCows.MOD_ID, "block/feeder")),
                    ModelResourceLocation.standalone(
                            ResourceLocation.fromNamespaceAndPath(
                                    ModernFluidCows.MOD_ID, "block/sorter")),
                    ModelResourceLocation.standalone(
                            ResourceLocation.fromNamespaceAndPath(
                                    ModernFluidCows.MOD_ID, "block/model_open")),
                    ModelResourceLocation.standalone(
                            ResourceLocation.fromNamespaceAndPath(
                                    ModernFluidCows.MOD_ID, "block/model_closed")));

    private static final List<ModelResourceLocation> ITEM_MODELS =
            List.of(
                    ModelResourceLocation.standalone(
                            ResourceLocation.fromNamespaceAndPath(
                                    ModernFluidCows.MOD_ID, "item/accelerator")),
                    ModelResourceLocation.standalone(
                            ResourceLocation.fromNamespaceAndPath(
                                    ModernFluidCows.MOD_ID, "item/feeder")),
                    ModelResourceLocation.standalone(
                            ResourceLocation.fromNamespaceAndPath(
                                    ModernFluidCows.MOD_ID, "item/sorter")),
                    ModelResourceLocation.standalone(
                            ResourceLocation.fromNamespaceAndPath(
                                    ModernFluidCows.MOD_ID, "item/stall")),
                    ModelResourceLocation.standalone(
                            ResourceLocation.fromNamespaceAndPath(
                                    ModernFluidCows.MOD_ID, "item/cow_halter")),
                    ModelResourceLocation.standalone(
                            ResourceLocation.fromNamespaceAndPath(
                                    ModernFluidCows.MOD_ID, "item/cow_displayer")),
                    ModelResourceLocation.standalone(
                            ResourceLocation.fromNamespaceAndPath(
                                    ModernFluidCows.MOD_ID, "item/ranger")));

    private static final ResourceLocation LEGACY_PACK_ID =
            ResourceLocation.fromNamespaceAndPath(
                    ModernFluidCows.MOD_ID, "resourcepacks/fluidcows_legacy");

    private static void onAddPackFinders(final AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) {
            return;
        }

        event.addPackFinders(
                LEGACY_PACK_ID,
                PackType.CLIENT_RESOURCES,
                Component.translatable("pack.modernfluidcows.legacy"),
                PackSource.BUILT_IN,
                false,
                Pack.Position.TOP);
    }

    private static void onRegisterAdditionalModels(final ModelEvent.RegisterAdditional event) {
        BLOCK_MODELS.forEach(event::register);
        ITEM_MODELS.forEach(event::register);
    }
}
