package com.setycz.chickens.integration.wthit.overlay;

import com.setycz.chickens.ChemicalEggRegistry;
import com.setycz.chickens.ChemicalEggRegistryItem;
import com.setycz.chickens.GasEggRegistry;
import com.setycz.chickens.ChickensMod;
import com.setycz.chickens.integration.wthit.component.HudBarComponent;
import com.setycz.chickens.integration.wthit.overlay.HudOverlayHelper;
import mcp.mobius.waila.api.IBlockAccessor;
import mcp.mobius.waila.api.IBlockComponentProvider;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.ITooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Client-side renderer that converts {@link HudOverlayHelper} entries into tooltip components,
 * mirroring Mekanism's WTHIT overlay.
 */
public final class HudTooltipRenderer implements IBlockComponentProvider {
    public static final ResourceLocation HUD_TAG = ResourceLocation.fromNamespaceAndPath(ChickensMod.MOD_ID, "hud_overlay");

    @Override
    public void appendBody(ITooltip tooltip, IBlockAccessor accessor, IPluginConfig config) {
        HudOverlayHelper helper = accessor.getData().get(HudOverlayHelper.TYPE);
        if (helper == null || helper.isEmpty()) {
            return;
        }
        tooltip.setLine(HUD_TAG);
        for (HudOverlayHelper.Entry entry : helper.entries()) {
            switch (entry.type()) {
                case TEXT -> tooltip.addLine(((HudOverlayHelper.TextEntry) entry).text());
                case FLUID -> tooltip.addLine(buildFluidBar((HudOverlayHelper.FluidEntry) entry));
                case CHEMICAL -> tooltip.addLine(buildChemicalBar((HudOverlayHelper.ChemicalEntry) entry));
                case ENERGY -> tooltip.addLine(buildEnergyBar((HudOverlayHelper.EnergyEntry) entry));
            }
        }
    }

    private static HudBarComponent buildFluidBar(HudOverlayHelper.FluidEntry entry) {
        FluidStack stack = entry.stack();
        int capacity = Math.max(entry.capacity(), 1);
        int amount = Math.max(stack.getAmount(), 0);
        Component text = amount <= 0
                ? Component.translatable("tooltip.chickens.avian_dousing_machine.empty")
                : Component.literal(String.format("%s: %s mB", stack.getHoverName().getString(), formatCompact(amount)));
        TextureAtlasSprite sprite = resolveFluidSprite(stack);
        int tint = stack.isEmpty() ? 0xFF6F6F6F : (IClientFluidTypeExtensions.of(stack.getFluid()).getTintColor(stack) | 0xFF000000);
        return new HudBarComponent(text, sprite, tint, amount / (float) capacity);
    }

    private static HudBarComponent buildChemicalBar(HudOverlayHelper.ChemicalEntry entry) {
        ChemicalEggRegistryItem item = resolveChemical(entry.entryId());
        int capacity = Math.max(entry.capacity(), 1);
        int amount = Math.max(entry.amount(), 0);
        Component text;
        TextureAtlasSprite sprite;
        int tint;
        if (item == null || amount <= 0) {
            text = Component.translatable("tooltip.chickens.avian_dousing_machine.empty");
            sprite = getMissingSprite();
            tint = 0xFF6F6F6F;
        } else {
            text = Component.literal(String.format("%s: %s mB", item.getDisplayName().getString(), formatCompact(amount)));
            sprite = getAtlas().getSprite(item.getTexture());
            tint = 0xFF000000 | item.getEggColor();
        }
        return new HudBarComponent(text, sprite, tint, amount / (float) capacity);
    }

    private static HudBarComponent buildEnergyBar(HudOverlayHelper.EnergyEntry entry) {
        long capacity = Math.max(entry.capacity(), 1L);
        long energy = Math.max(entry.energy(), 0L);
        Component text = Component.literal(String.format("%s / %s", formatEnergy(energy), formatEnergy(capacity)));
        TextureAtlasSprite sprite = getAtlas().getSprite(ResourceLocation.withDefaultNamespace("block/redstone_block"));
        return new HudBarComponent(text, sprite, 0xFFE6C63E, (float) Math.min(1.0, energy / (double) capacity));
    }

    private static TextureAtlasSprite resolveFluidSprite(FluidStack stack) {
        if (stack.isEmpty()) {
            return getAtlas().getSprite(ResourceLocation.withDefaultNamespace("block/water_still"));
        }
        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(stack.getFluid());
        BlockPos pos = BlockPos.ZERO;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            pos = player.blockPosition();
        }
        ResourceLocation texture = null;
        if (Minecraft.getInstance().level != null) {
            texture = extensions.getStillTexture(stack.getFluid().defaultFluidState(), Minecraft.getInstance().level, pos);
            if (texture == null) {
                texture = extensions.getFlowingTexture(stack.getFluid().defaultFluidState(), Minecraft.getInstance().level, pos);
            }
        }
        if (texture == null) {
            texture = ResourceLocation.withDefaultNamespace("block/water_still");
        }
        return getAtlas().getSprite(texture);
    }

    private static TextureAtlas getAtlas() {
        return Minecraft.getInstance().getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS);
    }

    private static TextureAtlasSprite getMissingSprite() {
        return Minecraft.getInstance().getModelManager().getMissingModel().getParticleIcon();
    }

    private static ChemicalEggRegistryItem resolveChemical(int id) {
        ChemicalEggRegistryItem entry = ChemicalEggRegistry.findById(id);
        return entry != null ? entry : GasEggRegistry.findById(id);
    }

    private static String formatEnergy(long amount) {
        if (amount >= 1_000_000L) {
            return formatDecimal(amount / 1_000_000f) + " MRF";
        }
        if (amount >= 1_000L) {
            return formatDecimal(amount / 1_000f) + " kRF";
        }
        return amount + " RF";
    }

    private static String formatDecimal(float value) {
        if (value % 1.0F == 0.0F) {
            return Integer.toString((int) value);
        }
        return String.format(Locale.US, "%.1f", value);
    }

    private static String formatCompact(int amount) {
        if (amount >= 1_000_000) {
            return formatDecimal(amount / 1_000_000f) + "M";
        }
        if (amount >= 1_000) {
            return formatDecimal(amount / 1_000f) + "k";
        }
        return NUMBER_FORMAT.format(amount);
    }

    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getIntegerInstance(Locale.US);
}
