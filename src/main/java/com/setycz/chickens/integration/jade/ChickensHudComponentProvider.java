package com.setycz.chickens.integration.jade;

import com.setycz.chickens.ChemicalEggRegistry;
import com.setycz.chickens.ChemicalEggRegistryItem;
import com.setycz.chickens.ChickensMod;
import com.setycz.chickens.GasEggRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.JadeIds;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Client-side renderer that mirrors the WTHIT HUD overlay for Jade.
 */
final class ChickensHudComponentProvider implements IBlockComponentProvider {
    static final ChickensHudComponentProvider INSTANCE = new ChickensHudComponentProvider();
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(ChickensMod.MOD_ID, "hud_overlay");

    private ChickensHudComponentProvider() {
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        HudData hud = HudData.read(accessor.getServerData());
        if (hud.isEmpty()) {
            return;
        }
        // Remove Jade's built-in energy line so only the custom HUD bar renders.
        tooltip.remove(JadeIds.UNIVERSAL_ENERGY_STORAGE);
        tooltip.remove(JadeIds.UNIVERSAL_ENERGY_STORAGE_DEFAULT);
        tooltip.remove(JadeIds.UNIVERSAL_ENERGY_STORAGE_DETAILED);
        // Remove Jade's built-in fluid line for the same reason.
        tooltip.remove(JadeIds.UNIVERSAL_FLUID_STORAGE);
        tooltip.remove(JadeIds.UNIVERSAL_FLUID_STORAGE_DEFAULT);
        tooltip.remove(JadeIds.UNIVERSAL_FLUID_STORAGE_DETAILED);
        for (HudData.Entry entry : hud.entries()) {
            switch (entry.type()) {
                case TEXT -> tooltip.add(((HudData.TextEntry) entry).text());
                case FLUID -> tooltip.add(buildFluidBar((HudData.FluidEntry) entry, accessor));
                case CHEMICAL -> tooltip.add(buildChemicalBar((HudData.ChemicalEntry) entry));
                case ENERGY -> tooltip.add(buildEnergyBar((HudData.EnergyEntry) entry));
                default -> { /* ignore */ }
            }
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    private HudBarElement buildFluidBar(HudData.FluidEntry entry, BlockAccessor accessor) {
        FluidStack stack = entry.toStack();
        int capacity = Math.max(entry.capacity(), 1);
        int amount = Math.max(stack.getAmount(), 0);
        Component text = amount <= 0 || stack.isEmpty()
                ? Component.translatable("tooltip.chickens.avian_dousing_machine.empty")
                : Component.literal(String.format("%s: %s mB", stack.getHoverName().getString(), formatCompact(amount)));
        TextureAtlasSprite sprite = resolveFluidSprite(stack, accessor);
        int tint = stack.isEmpty() ? 0xFF6F6F6F : (IClientFluidTypeExtensions.of(stack.getFluid()).getTintColor(stack) | 0xFF000000);
        HudBarElement element = new HudBarElement(text, sprite, tint, amount / (float) capacity);
        element.tag(UID);
        return element;
    }

    private HudBarElement buildChemicalBar(HudData.ChemicalEntry entry) {
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
        HudBarElement element = new HudBarElement(text, sprite, tint, amount / (float) capacity);
        element.tag(UID);
        return element;
    }

    private HudBarElement buildEnergyBar(HudData.EnergyEntry entry) {
        long capacity = Math.max(entry.capacity(), 1L);
        long energy = Math.max(entry.energy(), 0L);
        Component text = Component.literal(String.format("%s / %s", formatEnergy(energy), formatEnergy(capacity)));
        TextureAtlasSprite sprite = getAtlas().getSprite(ResourceLocation.withDefaultNamespace("block/redstone_block"));
        HudBarElement element = new HudBarElement(text, sprite, 0xFFE6C63E, (float) Math.min(1.0, energy / (double) capacity));
        element.tag(UID);
        return element;
    }

    private static TextureAtlasSprite resolveFluidSprite(FluidStack stack, BlockAccessor accessor) {
        if (stack.isEmpty()) {
            return getAtlas().getSprite(ResourceLocation.withDefaultNamespace("block/water_still"));
        }
        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(stack.getFluid());
        BlockPos pos = accessor.getPosition();
        ResourceLocation texture = extensions.getStillTexture(stack.getFluid().defaultFluidState(), accessor.getLevel(), pos);
        if (texture == null) {
            texture = extensions.getFlowingTexture(stack.getFluid().defaultFluidState(), accessor.getLevel(), pos);
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
