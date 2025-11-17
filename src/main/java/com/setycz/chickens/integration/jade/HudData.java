package com.setycz.chickens.integration.jade;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Lightweight, WTHIT-free payload for Jade. Providers build a {@link Builder},
 * which serialises into the Jade server data tag. The client decodes the same
 * structure to render HUD bars and text.
 */
final class HudData {
    static final String NBT_KEY = "ChickensHud";

    private final List<Entry> entries;

    private HudData(List<Entry> entries) {
        this.entries = entries;
    }

    boolean isEmpty() {
        return entries.isEmpty();
    }

    List<Entry> entries() {
        return entries;
    }

    Tag toTag() {
        ListTag list = new ListTag();
        for (Entry entry : entries) {
            CompoundTag tag = new CompoundTag();
            tag.putString("Type", entry.type().name());
            switch (entry.type()) {
                case TEXT -> writeComponent(tag, "Text", ((TextEntry) entry).text());
                case FLUID -> {
                    FluidEntry fluid = (FluidEntry) entry;
                    if (fluid.id() != null) {
                        tag.putString("Id", fluid.id().toString());
                    }
                    tag.putInt("Amount", Math.max(fluid.amount(), 0));
                    tag.putInt("Capacity", Math.max(fluid.capacity(), 0));
                }
                case CHEMICAL -> {
                    ChemicalEntry chem = (ChemicalEntry) entry;
                    tag.putInt("Entry", Math.max(chem.entryId(), -1));
                    tag.putInt("Amount", Math.max(chem.amount(), 0));
                    tag.putInt("Capacity", Math.max(chem.capacity(), 0));
                }
                case ENERGY -> {
                    EnergyEntry energy = (EnergyEntry) entry;
                    tag.putLong("Energy", Math.max(energy.energy(), 0L));
                    tag.putLong("Capacity", Math.max(energy.capacity(), 0L));
                }
            }
            list.add(tag);
        }
        return list;
    }

    static HudData read(CompoundTag data) {
        if (!data.contains(NBT_KEY, Tag.TAG_LIST)) {
            return empty();
        }
        ListTag list = data.getList(NBT_KEY, Tag.TAG_COMPOUND);
        List<Entry> entries = new ArrayList<>(list.size());
        for (Tag element : list) {
            if (!(element instanceof CompoundTag tag)) {
                continue;
            }
            EntryType type = parseType(tag.getString("Type"));
            switch (type) {
                case TEXT -> {
                    Component component = readComponent(tag, "Text");
                    if (component != null) {
                        entries.add(new TextEntry(component));
                    }
                }
                case FLUID -> {
                    ResourceLocation id = tag.contains("Id", Tag.TAG_STRING)
                            ? ResourceLocation.tryParse(tag.getString("Id"))
                            : null;
                    int amount = tag.getInt("Amount");
                    int capacity = tag.getInt("Capacity");
                    entries.add(new FluidEntry(id, amount, capacity));
                }
                case CHEMICAL -> {
                    int entryId = tag.getInt("Entry");
                    int amount = tag.getInt("Amount");
                    int capacity = tag.getInt("Capacity");
                    entries.add(new ChemicalEntry(entryId, amount, capacity));
                }
                case ENERGY -> {
                    long energy = tag.getLong("Energy");
                    long capacity = tag.getLong("Capacity");
                    entries.add(new EnergyEntry(energy, capacity));
                }
                case UNKNOWN -> {
                    // Skip unrecognised data to remain forwards compatible.
                }
            }
        }
        return entries.isEmpty() ? empty() : new HudData(ImmutableList.copyOf(entries));
    }

    static void write(CompoundTag root, HudData data) {
        if (data == null || data.isEmpty()) {
            root.remove(NBT_KEY);
            return;
        }
        root.put(NBT_KEY, data.toTag());
    }

    static HudData empty() {
        return new HudData(List.of());
    }

    static Builder builder() {
        return new Builder();
    }

    enum EntryType {
        TEXT,
        FLUID,
        CHEMICAL,
        ENERGY,
        UNKNOWN
    }

    sealed interface Entry permits TextEntry, FluidEntry, ChemicalEntry, EnergyEntry {
        EntryType type();
    }

    record TextEntry(Component text) implements Entry {
        @Override
        public EntryType type() {
            return EntryType.TEXT;
        }
    }

    record FluidEntry(ResourceLocation id, int amount, int capacity) implements Entry {
        @Override
        public EntryType type() {
            return EntryType.FLUID;
        }

        FluidStack toStack() {
            if (id == null) {
                return FluidStack.EMPTY;
            }
            return BuiltInRegistries.FLUID.getOptional(id)
                    .map(fluid -> new FluidStack(fluid, amount))
                    .orElse(FluidStack.EMPTY);
        }
    }

    record ChemicalEntry(int entryId, int amount, int capacity) implements Entry {
        @Override
        public EntryType type() {
            return EntryType.CHEMICAL;
        }
    }

    record EnergyEntry(long energy, long capacity) implements Entry {
        @Override
        public EntryType type() {
            return EntryType.ENERGY;
        }
    }

    static final class Builder {
        private final List<Entry> entries = new ArrayList<>();

        void addText(Component component) {
            Objects.requireNonNull(component);
            entries.add(new TextEntry(component));
        }

        void addFluid(FluidStack stack, int capacity) {
            ResourceLocation id = stack.isEmpty() ? null : BuiltInRegistries.FLUID.getKey(stack.getFluid());
            entries.add(new FluidEntry(id, stack.getAmount(), capacity));
        }

        void addChemical(int entryId, int amount, int capacity) {
            entries.add(new ChemicalEntry(entryId, amount, capacity));
        }

        void addEnergy(long stored, long capacity) {
            entries.add(new EnergyEntry(stored, capacity));
        }

        HudData build() {
            if (entries.isEmpty()) {
                return empty();
            }
            return new HudData(ImmutableList.copyOf(entries));
        }
    }

    private static void writeComponent(CompoundTag tag, String key, Component value) {
        ComponentSerialization.CODEC.encodeStart(NbtOps.INSTANCE, value)
                .result()
                .ifPresent(serialized -> tag.put(key, serialized));
    }

    private static Component readComponent(CompoundTag tag, String key) {
        if (!tag.contains(key)) {
            return null;
        }
        return ComponentSerialization.CODEC.parse(NbtOps.INSTANCE, tag.get(key))
                .result()
                .orElse(null);
    }

    private static EntryType parseType(String raw) {
        try {
            return EntryType.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return EntryType.UNKNOWN;
        }
    }
}
