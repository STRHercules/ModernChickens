package com.setycz.chickens.integration.wthit.overlay;

import com.setycz.chickens.ChickensMod;
import mcp.mobius.waila.api.IData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight payload that mirrors Mekanism's looking-at helper. The server collects a list of HUD
 * elements, and the client transforms them into tooltip components.
 */
public final class HudOverlayHelper implements IData {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ChickensMod.MOD_ID, "hud_overlay");
    public static final IData.Type<HudOverlayHelper> TYPE = () -> ID;

    public static final StreamCodec<RegistryFriendlyByteBuf, HudOverlayHelper> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public HudOverlayHelper decode(RegistryFriendlyByteBuf buf) {
            int size = buf.readVarInt();
            List<Entry> entries = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                EntryType type = buf.readEnum(EntryType.class);
                entries.add(readEntry(buf, type));
            }
            return new HudOverlayHelper(entries);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, HudOverlayHelper helper) {
            buf.writeVarInt(helper.entries.size());
            for (Entry entry : helper.entries) {
                buf.writeEnum(entry.type());
                writeEntry(buf, entry);
            }
        }
    };

    private final List<Entry> entries;

    public HudOverlayHelper() {
        this(new ArrayList<>());
    }

    private HudOverlayHelper(List<Entry> entries) {
        this.entries = entries;
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public List<Entry> entries() {
        return entries;
    }

    public void addText(Component component) {
        entries.add(new TextEntry(component));
    }

    public void addFluid(FluidStack stack, int capacity) {
        entries.add(new FluidEntry(stack, capacity));
    }

    public void addChemical(int entryId, int amount, int capacity) {
        entries.add(new ChemicalEntry(entryId, amount, capacity));
    }

    public void addEnergy(long stored, long capacity) {
        entries.add(new EnergyEntry(stored, capacity));
    }

    @Override
    public Type<? extends IData> type() {
        return TYPE;
    }

    private static Entry readEntry(RegistryFriendlyByteBuf buf, EntryType type) {
        return switch (type) {
            case TEXT -> new TextEntry(ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buf));
            case FLUID -> new FluidEntry(FluidStack.OPTIONAL_STREAM_CODEC.decode(buf), buf.readVarInt());
            case CHEMICAL -> new ChemicalEntry(buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
            case ENERGY -> new EnergyEntry(buf.readVarLong(), buf.readVarLong());
        };
    }

    private static void writeEntry(RegistryFriendlyByteBuf buf, Entry entry) {
        switch (entry.type()) {
            case TEXT -> ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, ((TextEntry) entry).text());
            case FLUID -> {
                FluidEntry fluid = (FluidEntry) entry;
                FluidStack.OPTIONAL_STREAM_CODEC.encode(buf, fluid.stack());
                buf.writeVarInt(fluid.capacity());
            }
            case CHEMICAL -> {
                ChemicalEntry chemical = (ChemicalEntry) entry;
                buf.writeVarInt(chemical.entryId());
                buf.writeVarInt(chemical.amount());
                buf.writeVarInt(chemical.capacity());
            }
            case ENERGY -> {
                EnergyEntry energy = (EnergyEntry) entry;
                buf.writeVarLong(energy.energy());
                buf.writeVarLong(energy.capacity());
            }
        }
    }

    public sealed interface Entry permits TextEntry, FluidEntry, ChemicalEntry, EnergyEntry {
        EntryType type();
    }

    public enum EntryType {
        TEXT,
        FLUID,
        CHEMICAL,
        ENERGY
    }

    public record TextEntry(Component text) implements Entry {
        @Override
        public EntryType type() {
            return EntryType.TEXT;
        }
    }

    public record FluidEntry(FluidStack stack, int capacity) implements Entry {
        @Override
        public EntryType type() {
            return EntryType.FLUID;
        }
    }

    public record ChemicalEntry(int entryId, int amount, int capacity) implements Entry {
        @Override
        public EntryType type() {
            return EntryType.CHEMICAL;
        }
    }

    public record EnergyEntry(long energy, long capacity) implements Entry {
        @Override
        public EntryType type() {
            return EntryType.ENERGY;
        }
    }
}
