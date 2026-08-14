package strhercules.chickens;

import strhercules.chickens.client.ChickensConfigScreen;
import strhercules.chickens.entity.MegaChicken;
import strhercules.chickens.ChemicalEggRegistry;
import strhercules.chickens.ChemicalEggRegistryItem;
import strhercules.chickens.LiquidEggRegistry;
import strhercules.chickens.LiquidEggRegistryItem;
import strhercules.chickens.GasEggRegistry;
import strhercules.chickens.client.render.ChickenItemModels;
import strhercules.chickens.client.render.ChickenItemSpriteModels;
import strhercules.chickens.client.render.ChickensChickenRenderer;
import strhercules.chickens.client.render.DynamicChickenTextures;
import strhercules.chickens.client.render.LiquidChickenOverlayLayer;
import strhercules.chickens.client.render.MegaChickenModel;
import strhercules.chickens.client.render.MegaChickenRenderer;
import strhercules.chickens.client.render.RoosterModel;
import strhercules.chickens.client.render.RoosterRenderer;
import strhercules.chickens.client.render.blockentity.BreederBlockEntityRenderer;
import strhercules.chickens.client.render.blockentity.CollectorBlockEntityRenderer;
import strhercules.chickens.client.render.blockentity.AvianChemicalConverterBlockEntityRenderer;
import strhercules.chickens.client.render.blockentity.AvianDousingMachineBlockEntityRenderer;
import strhercules.chickens.client.render.blockentity.AvianFluxConverterBlockEntityRenderer;
import strhercules.chickens.client.render.blockentity.AvianFluidConverterBlockEntityRenderer;
import strhercules.chickens.client.render.blockentity.RoostBlockEntityRenderer;
import strhercules.chickens.client.render.blockentity.NestBlockEntityRenderer;
import strhercules.chickens.client.render.blockentity.MechanicalNestBlockEntityRenderer;
import strhercules.chickens.item.ChickenItemHelper;
import strhercules.chickens.network.MegaChickenFlightPayload;
import strhercules.chickens.registry.ModBlockEntities;
import strhercules.chickens.registry.ModEntityTypes;
import strhercules.chickens.registry.ModMenuTypes;
import strhercules.chickens.registry.ModRegistry;
import strhercules.chickens.screen.AvianChemicalConverterScreen;
import strhercules.chickens.screen.AvianDousingMachineScreen;
import strhercules.chickens.screen.AvianFluxConverterScreen;
import strhercules.chickens.screen.AvianFluidConverterScreen;
import strhercules.chickens.screen.BreederScreen;
import strhercules.chickens.screen.CollectorScreen;
import strhercules.chickens.screen.IncubatorScreen;
import strhercules.chickens.screen.HenhouseScreen;
import strhercules.chickens.screen.MegaChickenScreen;
import strhercules.chickens.screen.RoostScreen;
import strhercules.chickens.screen.MechanicalRoostScreen;
import strhercules.chickens.screen.NestScreen;
import strhercules.chickens.screen.MechanicalNestScreen;
import strhercules.chickens.screen.RoosterScreen;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Client-only hooks for renderer and colour registration. Static event
 * subscribers keep server environments free from accidental client class loads.
 */
@EventBusSubscriber(modid = ChickensMod.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ChickensClient {
    private static final KeyMapping MEGA_CHICKEN_DIVE = new KeyMapping(
            "key.chickens.mega_chicken_dive", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT,
            KeyMapping.CATEGORY_MOVEMENT);
    private static final KeyMapping MEGA_CHICKEN_AIRBRAKE = new KeyMapping(
            "key.chickens.mega_chicken_airbrake", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_CONTROL,
            KeyMapping.CATEGORY_MOVEMENT);
    private static boolean lastJumpDown;
    private static boolean lastDiveDown;
    private static boolean lastAirbrakeDown;

    private ChickensClient() {
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntityTypes.COLORED_EGG.get(), context -> new ThrownItemRenderer<>(context, 1.0F, true));
        event.registerEntityRenderer(ModEntityTypes.CHICKENS_CHICKEN.get(), ChickensChickenRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.ROOSTER.get(), RoosterRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.MEGA_CHICKEN.get(), MegaChickenRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.ROOST.get(), RoostBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.NEST.get(), NestBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.MECHANICAL_NEST.get(), MechanicalNestBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.BREEDER.get(), BreederBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.COLLECTOR.get(), CollectorBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.AVIAN_FLUX_CONVERTER.get(),
                AvianFluxConverterBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.AVIAN_FLUID_CONVERTER.get(),
                AvianFluidConverterBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.AVIAN_CHEMICAL_CONVERTER.get(),
                AvianChemicalConverterBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.AVIAN_DOUSING_MACHINE.get(),
                AvianDousingMachineBlockEntityRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(RoosterModel.LAYER_LOCATION, RoosterModel::createBodyLayer);
        event.registerLayerDefinition(MegaChickenModel.LAYER_LOCATION, MegaChickenModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        NeoForge.EVENT_BUS.addListener(ChickensClient::onClientTick);
        ModLoadingContext.get().getActiveContainer().registerExtensionPoint(IConfigScreenFactory.class,
                (container, parent) -> new ChickensConfigScreen(parent));
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(ModRegistry.BREEDER.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModRegistry.LAVA_CHICKEN_FIRE.get(), RenderType.cutout());
        });
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(MEGA_CHICKEN_DIVE);
        event.register(MEGA_CHICKEN_AIRBRAKE);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.getConnection() == null) {
            lastJumpDown = false;
            lastDiveDown = false;
            lastAirbrakeDown = false;
            return;
        }

        boolean jumping = minecraft.options.keyJump.isDown()
                && minecraft.player.getVehicle() instanceof MegaChicken chicken
                && chicken.hasFlyingEgg();
        boolean diving = MEGA_CHICKEN_DIVE.isDown()
                && minecraft.player.getVehicle() instanceof MegaChicken chicken
                && chicken.hasFlyingEgg()
                && !chicken.onGround();
        boolean airbraking = MEGA_CHICKEN_AIRBRAKE.isDown()
                && minecraft.player.getVehicle() instanceof MegaChicken chicken
                && chicken.hasFlyingEgg()
                && !chicken.onGround();
        if (minecraft.player.getVehicle() instanceof MegaChicken chicken) {
            chicken.setJumpRequested(jumping);
            chicken.setDiveRequested(diving);
            chicken.setAirbrakeRequested(airbraking);
        }
        if (jumping != lastJumpDown || diving != lastDiveDown || airbraking != lastAirbrakeDown) {
            PacketDistributor.sendToServer(new MegaChickenFlightPayload(jumping, diving, airbraking));
            lastJumpDown = jumping;
            lastDiveDown = diving;
            lastAirbrakeDown = airbraking;
        }
    }

    @SubscribeEvent
    public static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tint) -> tint <= 0 ? getChickenColor(stack, true) : getChickenColor(stack, false), ModRegistry.SPAWN_EGG.get());
        event.register((stack, tint) -> getColoredEggColor(stack), ModRegistry.COLORED_EGG.get());
        event.register((stack, tint) -> getLiquidEggColor(stack), ModRegistry.LIQUID_EGG.get());
        event.register((stack, tint) -> getChemicalEggColor(stack), ModRegistry.CHEMICAL_EGG.get());
        event.register((stack, tint) -> getGasEggColor(stack), ModRegistry.GAS_EGG.get());
        event.register((stack, tint) -> getChickenItemColor(stack, tint == 0), ModRegistry.CHICKEN_ITEM.get());
    }

    @SubscribeEvent
    public static void onRegisterItemDecorations(RegisterItemDecorationsEvent event) {
        event.register(ModRegistry.FLUX_EGG.get(), (graphics, font, stack, x, y) -> {
            int barX = x + 2;
            int barY = y + 1;
            int barWidth = stack.getBarWidth();
            graphics.fill(barX, barY, barX + 13, barY + 2, 0xFF000000);
            graphics.fill(barX, barY, barX + barWidth, barY + 1, stack.getBarColor() | 0xFF000000);
            return false;
        });
    }

    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        // Bind the container to its screen so the henhouse GUI renders correctly on the client.
        event.register(ModMenuTypes.HENHOUSE.get(), HenhouseScreen::new);
        event.register(ModMenuTypes.ROOST.get(), RoostScreen::new);
        event.register(ModMenuTypes.MECHANICAL_ROOST.get(), MechanicalRoostScreen::new);
        event.register(ModMenuTypes.NEST.get(), NestScreen::new);
        event.register(ModMenuTypes.MECHANICAL_NEST.get(), MechanicalNestScreen::new);
        event.register(ModMenuTypes.ROOSTER.get(), RoosterScreen::new);
        event.register(ModMenuTypes.BREEDER.get(), BreederScreen::new);
        event.register(ModMenuTypes.COLLECTOR.get(), CollectorScreen::new);
        event.register(ModMenuTypes.AVIAN_FLUX_CONVERTER.get(), AvianFluxConverterScreen::new);
        event.register(ModMenuTypes.AVIAN_FLUID_CONVERTER.get(), AvianFluidConverterScreen::new);
        event.register(ModMenuTypes.AVIAN_CHEMICAL_CONVERTER.get(), AvianChemicalConverterScreen::new);
        event.register(ModMenuTypes.AVIAN_DOUSING_MACHINE.get(), AvianDousingMachineScreen::new);
        event.register(ModMenuTypes.INCUBATOR.get(), IncubatorScreen::new);
        event.register(ModMenuTypes.MEGA_CHICKEN.get(), MegaChickenScreen::new);
    }

    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(DynamicChickenTextures.reloadListener());
        event.registerReloadListener(ChickensChickenRenderer.textureAvailabilityReloader());
        event.registerReloadListener(ChickenItemSpriteModels.reloadListener());
        event.registerReloadListener(LiquidChickenOverlayLayer.reloadListener());
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

    private static int getChemicalEggColor(ItemStack stack) {
        ChemicalEggRegistryItem chemical = ChemicalEggRegistry.findById(ChickenItemHelper.getChickenType(stack));
        int color = chemical != null ? chemical.getEggColor() : 0xFFFFFF;
        return 0xFF000000 | color;
    }

    private static int getGasEggColor(ItemStack stack) {
        ChemicalEggRegistryItem gas = GasEggRegistry.findById(ChickenItemHelper.getChickenType(stack));
        int color = gas != null ? gas.getEggColor() : 0xFFFFFF;
        return 0xFF000000 | color;
    }
}
