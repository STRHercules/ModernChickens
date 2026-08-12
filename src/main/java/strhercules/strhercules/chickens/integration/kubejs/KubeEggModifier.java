package strhercules.chickens.integration.kubejs;

import strhercules.chickens.LiquidEggRegistryItem;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.Locale;

/** Fluent KubeJS metadata override for a fluid, chemical, or gas egg. */
public final class KubeEggModifier {
    private final String eggId;
    @Nullable
    private Integer eggColor;
    @Nullable
    private Integer volume;
    @Nullable
    private EnumSet<LiquidEggRegistryItem.HazardFlag> hazards;

    KubeEggModifier(String eggId) {
        this.eggId = eggId;
    }

    public KubeEggModifier eggColor(Object value) {
        eggColor = parseColour(value);
        return this;
    }

    public KubeEggModifier volume(int value) {
        volume = Math.max(0, value);
        return this;
    }

    public KubeEggModifier hazards(Object... values) {
        EnumSet<LiquidEggRegistryItem.HazardFlag> parsed = EnumSet.noneOf(LiquidEggRegistryItem.HazardFlag.class);
        if (values == null) {
            values = new Object[0];
        }
        for (Object rawValue : values) {
            String value = rawValue == null ? null : rawValue.toString();
            if (value == null) {
                continue;
            }
            try {
                parsed.add(LiquidEggRegistryItem.HazardFlag.valueOf(value.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ex) {
                ChickenRegistryKubeEvent.LOGGER.warn("Egg modifier '{}' has an unknown hazard '{}'; ignoring",
                        eggId, value);
            }
        }
        hazards = parsed;
        return this;
    }

    public KubeEggModifier clearHazards() {
        hazards = EnumSet.noneOf(LiquidEggRegistryItem.HazardFlag.class);
        return this;
    }

    @Nullable
    Integer eggColor() {
        return eggColor;
    }

    @Nullable
    Integer volume() {
        return volume;
    }

    @Nullable
    EnumSet<LiquidEggRegistryItem.HazardFlag> hazards() {
        return hazards;
    }

    @Nullable
    private Integer parseColour(Object value) {
        if (value instanceof Number number) {
            return clamp(number.intValue());
        }
        if (value != null) {
            String raw = value.toString().trim();
            if (raw.startsWith("#")) {
                raw = raw.substring(1);
            } else if (raw.startsWith("0x") || raw.startsWith("0X")) {
                raw = raw.substring(2);
            }
            try {
                return clamp(Integer.parseUnsignedInt(raw, 16));
            } catch (NumberFormatException ignored) {
                // Fall through to the warning below.
            }
        }
        ChickenRegistryKubeEvent.LOGGER.warn("Egg modifier '{}' could not parse colour '{}'; ignoring", eggId, value);
        return null;
    }

    @Nullable
    private Integer clamp(int value) {
        if (value < 0 || value > 0xFFFFFF) {
            ChickenRegistryKubeEvent.LOGGER.warn("Egg modifier '{}' has an out-of-range colour '{}'; ignoring",
                    eggId, value);
            return null;
        }
        return value;
    }
}
