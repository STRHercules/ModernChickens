package com.setycz.chickens;

import com.setycz.chickens.LiquidEggRegistry;
import com.setycz.chickens.LiquidEggRegistryItem;
import com.setycz.chickens.client.render.ChickenItemModels;
import com.setycz.chickens.client.render.ChickenItemSpriteModels;
import com.setycz.chickens.client.render.ChickensChickenRenderer;
import com.setycz.chickens.client.render.DynamicChickenTextures;
import com.setycz.chickens.client.render.blockentity.BreederBlockEntityRenderer;
import com.setycz.chickens.client.render.blockentity.CollectorBlockEntityRenderer;
import com.setycz.chickens.client.render.blockentity.RoostBlockEntityRenderer;
import com.setycz.chickens.item.ChickenItemHelper;
import com.setycz.chickens.registry.ModBlockEntities;
import com.setycz.chickens.registry.ModEntityTypes;
import com.setycz.chickens.registry.ModMenuTypes;
import com.setycz.chickens.registry.ModRegistry;
import com.setycz.chickens.screen.BreederScreen;
import com.setycz.chickens.screen.CollectorScreen;
import com.setycz.chickens.screen.HenhouseScreen;
import com.setycz.chickens.screen.RoostScreen;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Client-only hooks for renderer and colour registration. Static event
 * subscribers keep server environments free from accidental client class loads.
 */
@EventBusSubscriber(modid = ChickensMod.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ChickensClient {
    private ChickensClient() {
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntityTypes.COLORED_EGG.get(), context -> new ThrownItemRenderer<>(context, 1.0F, true));
        event.registerEntityRenderer(ModEntityTypes.CHICKENS_CHICKEN.get(), ChickensChickenRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.ROOST.get(), RoostBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.BREEDER.get(), BreederBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.COLLECTOR.get(), CollectorBlockEntityRenderer::new);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemBlockRenderTypes.setRenderLayer(ModRegistry.BREEDER.get(), RenderType.cutout()));
    }

    @SubscribeEvent
    public static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tint) -> tint <= 0 ? getChickenColor(stack, true) : getChickenColor(stack, false), ModRegistry.SPAWN_EGG.get());
        event.register((stack, tint) -> getColoredEggColor(stack), ModRegistry.COLORED_EGG.get());
        event.register((stack, tint) -> getLiquidEggColor(stack), ModRegistry.LIQUID_EGG.get());
        event.register((stack, tint) -> getChickenItemColor(stack, tint == 0), ModRegistry.CHICKEN_ITEM.get());
    }

    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        // Bind the container to its screen so the henhouse GUI renders correctly on the client.
        event.register(ModMenuTypes.HENHOUSE.get(), HenhouseScreen::new);
        event.register(ModMenuTypes.ROOST.get(), RoostScreen::new);
        event.register(ModMenuTypes.BREEDER.get(), BreederScreen::new);
        event.register(ModMenuTypes.COLLECTOR.get(), CollectorScreen::new);
    }

    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(DynamicChickenTextures.reloadListener());
        event.registerReloadListener(ChickensChickenRenderer.textureAvailabilityReloader());
        event.registerReloadListener(ChickenItemSpriteModels.reloadListener());
    }

    @SubscribeEvent
    public static void onModifyModels(ModelEvent.ModifyBakingResult event) {
        // Wrap the baked chicken model with an override-aware version so JSON
        // configs can point items at bespoke sprites without bundling assets.
        ChickenItemModels.injectOverrides(event);
    }

    private static int getChickenColor(ItemStack stack, boolean primary) {
        ChickensRegistryItem chicken = ChickenItemHelper.resolve(stack);
        if (chicken == null) {
            return 0xFFFFFFFF;
        }
        return encodeChickenColor(chicken, primary);
    }

    private static int getChickenItemColor(ItemStack stack, boolean primaryLayer) {
        ChickensRegistryItem chicken = ChickenItemHelper.resolve(stack);
        if (chicken == null) {
            return 0xFFFFFFFF;
        }
        if (!chicken.shouldTintItem()) {
            // Preserve the custom sprite exactly as drawn when the config provides
            // a bespoke item texture.
            return 0xFFFFFFFF;
        }
        return encodeChickenColor(chicken, primaryLayer);
    }

    private static int encodeChickenColor(ChickensRegistryItem chicken, boolean primaryLayer) {
        int color = primaryLayer ? chicken.getBgColor() : chicken.getFgColor();
        return 0xFF000000 | color;
    }

    private static int getColoredEggColor(ItemStack stack) {
        ChickensRegistryItem chicken = ChickenItemHelper.resolve(stack);
        if (chicken == null) {
            return 0xFFFFFFFF;
        }
        DyeColor dye = chicken.getDyeColor();
        int color = dye != null ? dye.getTextColor() : chicken.getFgColor();
        return 0xFF000000 | color;
    }

    private static int getLiquidEggColor(ItemStack stack) {
        LiquidEggRegistryItem liquid = LiquidEggRegistry.findById(ChickenItemHelper.getChickenType(stack));
        int color = liquid != null ? liquid.getEggColor() : 0xFFFFFF;
        return 0xFF000000 | color;
    }
}
