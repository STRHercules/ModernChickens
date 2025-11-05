package com.setycz.chickens.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.setycz.chickens.ChemicalEggRegistry;
import com.setycz.chickens.ChemicalEggRegistryItem;
import com.setycz.chickens.ChickensRegistry;
import com.setycz.chickens.ChickensRegistryItem;
import com.setycz.chickens.GasEggRegistry;
import com.setycz.chickens.LiquidEggRegistry;
import com.setycz.chickens.LiquidEggRegistryItem;
import com.setycz.chickens.entity.ChickensChicken;
import com.setycz.chickens.item.ChickenItemHelper;
import com.setycz.chickens.item.ChemicalEggItem;
import com.setycz.chickens.item.GasEggItem;
import com.setycz.chickens.item.LiquidEggItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ChickenModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Render layer that mirrors ModernFluidCows' overlay: it replays the chicken model using the bound
 * fluid's atlas sprite and tint, letting animated textures supply the liquid motion naturally.
 */
public final class LiquidChickenOverlayLayer extends RenderLayer<Chicken, ChickenModel<Chicken>> {
    private static final Map<Integer, Optional<LiquidEggRegistryItem>> LIQUID_CACHE = new HashMap<>();

    private final ChickenModel<Chicken> overlayModel;

    public LiquidChickenOverlayLayer(RenderLayerParent<Chicken, ChickenModel<Chicken>> parent,
                                     ChickenModel<Chicken> overlayModel) {
        super(parent);
        this.overlayModel = overlayModel;
    }

    @Override
    public void render(PoseStack poseStack,
                       MultiBufferSource buffer,
                       int packedLight,
                       Chicken chicken,
                       float limbSwing,
                       float limbSwingAmount,
                       float partialTick,
                       float ageInTicks,
                       float netHeadYaw,
                       float headPitch) {
        if (!(chicken instanceof ChickensChicken modChicken) || chicken.isInvisible()) {
            return;
        }

        ChickensRegistryItem description = ChickensRegistry.getByType(modChicken.getChickenType());
        if (description == null) {
            return;
        }

        OverlayInfo overlay = resolveOverlay(modChicken, description);
        if (overlay == null) {
            return;
        }

        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getModelManager()
                .getAtlas(TextureAtlas.LOCATION_BLOCKS)
                .getSprite(overlay.texture());
        if (sprite == null) {
            return;
        }

        int tint = overlay.tint();
        int red = (tint >> 16) & 0xFF;
        int green = (tint >> 8) & 0xFF;
        int blue = tint & 0xFF;
        float alpha = 0.8F;
        int packedColor = FastColor.ARGB32.color(
                Math.round(alpha * 255.0F),
                red,
                green,
                blue);

        // Keep the overlay model in sync with the parent renderer so limb poses and animations line up.
        ChickenModel<Chicken> parentModel = this.getParentModel();
        parentModel.copyPropertiesTo(overlayModel);
        overlayModel.prepareMobModel(chicken, limbSwing, limbSwingAmount, partialTick);
        overlayModel.setupAnim(chicken, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        var vertexConsumer = sprite.wrap(buffer.getBuffer(RenderType.entityTranslucentCull(TextureAtlas.LOCATION_BLOCKS)));
        overlayModel.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, packedColor);
    }

    private static LiquidEggRegistryItem resolveLiquid(ChickensRegistryItem description) {
        Optional<LiquidEggRegistryItem> cached = LIQUID_CACHE.computeIfAbsent(description.getId(), id -> {
            ItemStack layStack = description.createLayItem();
            if (!(layStack.getItem() instanceof LiquidEggItem)) {
                return Optional.empty();
            }
            return Optional.ofNullable(LiquidEggRegistry.findById(ChickenItemHelper.getChickenType(layStack)));
        });
        return cached.orElse(null);
    }

    @Nullable
    private static OverlayInfo resolveOverlay(ChickensChicken chicken, ChickensRegistryItem description) {
        ItemStack layStack = description.createLayItem();
        if (layStack.isEmpty()) {
            return null;
        }
        if (layStack.getItem() instanceof LiquidEggItem) {
            LiquidEggRegistryItem liquid = resolveLiquid(description);
            if (liquid == null) {
                return null;
            }
            FluidStack fluidStack = liquid.createFluidStack();
            if (fluidStack.isEmpty()) {
                return null;
            }
            Fluid fluid = fluidStack.getFluid();
            IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid);
            FluidState state = fluid.defaultFluidState();
            ResourceLocation texture = extensions.getStillTexture(state, chicken.level(), chicken.blockPosition());
            if (texture == null) {
                texture = extensions.getFlowingTexture(state, chicken.level(), chicken.blockPosition());
            }
            if (texture == null) {
                return null;
            }
            int tint = extensions.getTintColor(state, chicken.level(), chicken.blockPosition());
            return new OverlayInfo(texture, brightenTint(tint));
        }
        int id = ChickenItemHelper.getChickenType(layStack);
        if (layStack.getItem() instanceof ChemicalEggItem) {
            ChemicalEggRegistryItem chemical = ChemicalEggRegistry.findById(id);
            if (chemical != null) {
                return new OverlayInfo(chemical.getTexture(), brightenTint(chemical.getEggColor()));
            }
        } else if (layStack.getItem() instanceof GasEggItem) {
            ChemicalEggRegistryItem gas = GasEggRegistry.findById(id);
            if (gas != null) {
                return new OverlayInfo(gas.getTexture(), brightenTint(gas.getEggColor()));
            }
        }
        return null;
    }

    private static int brightenTint(int tint) {
        int red = Mth.clamp((int) (((tint >> 16) & 0xFF) * 1.2F + 24.0F), 0, 255);
        int green = Mth.clamp((int) (((tint >> 8) & 0xFF) * 1.2F + 24.0F), 0, 255);
        int blue = Mth.clamp((int) ((tint & 0xFF) * 1.2F + 24.0F), 0, 255);
        return (red << 16) | (green << 8) | blue;
    }

    public static void clearCaches() {
        LIQUID_CACHE.clear();
    }

    public static SimplePreparableReloadListener<Void> reloadListener() {
        return new SimplePreparableReloadListener<>() {
            @Override
            protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
                return null;
            }

            @Override
            protected void apply(Void object, ResourceManager resourceManager, ProfilerFiller profiler) {
                clearCaches();
            }
        };
    }

    private record OverlayInfo(ResourceLocation texture, int tint) {
    }
}
