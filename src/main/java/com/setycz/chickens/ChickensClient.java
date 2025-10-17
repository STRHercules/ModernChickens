package com.setycz.chickens;

import com.setycz.chickens.ChickensRegistryItem;
import com.setycz.chickens.LiquidEggRegistry;
import com.setycz.chickens.LiquidEggRegistryItem;
import com.setycz.chickens.item.ChickenItemHelper;
import com.setycz.chickens.registry.ModEntityTypes;
import com.setycz.chickens.registry.ModMenuTypes;
import com.setycz.chickens.registry.ModRegistry;
import com.setycz.chickens.screen.HenhouseScreen;
import net.minecraft.client.renderer.entity.ChickenRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

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
        event.registerEntityRenderer(ModEntityTypes.CHICKENS_CHICKEN.get(), ChickenRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tint) -> tint <= 0 ? getChickenColor(stack, true) : getChickenColor(stack, false), ModRegistry.SPAWN_EGG.get());
        event.register((stack, tint) -> getColoredEggColor(stack), ModRegistry.COLORED_EGG.get());
        event.register((stack, tint) -> getLiquidEggColor(stack), ModRegistry.LIQUID_EGG.get());
    }

    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        // Bind the container to its screen so the henhouse GUI renders correctly on the client.
        event.register(ModMenuTypes.HENHOUSE.get(), HenhouseScreen::new);
    }

    private static int getChickenColor(ItemStack stack, boolean primary) {
        ChickensRegistryItem chicken = ChickenItemHelper.resolve(stack);
        if (chicken == null) {
            return 0xffffff;
        }
        return primary ? chicken.getBgColor() : chicken.getFgColor();
    }

    private static int getColoredEggColor(ItemStack stack) {
        ChickensRegistryItem chicken = ChickenItemHelper.resolve(stack);
        if (chicken == null) {
            return 0xffffff;
        }
        DyeColor dye = chicken.getDyeColor();
        return dye != null ? dye.getTextColor() : chicken.getFgColor();
    }

    private static int getLiquidEggColor(ItemStack stack) {
        LiquidEggRegistryItem liquid = LiquidEggRegistry.findById(ChickenItemHelper.getChickenType(stack));
        return liquid != null ? liquid.getEggColor() : 0xffffff;
    }
}
