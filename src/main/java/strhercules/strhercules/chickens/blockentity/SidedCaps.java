package strhercules.chickens.blockentity;

import net.minecraft.core.Direction;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Caches one {@link LazyOptional} per side so repeated capability queries hand
 * out the same instance and can be invalidated together when the block entity
 * is removed.
 */
public final class SidedCaps<T> {
    private final Function<Direction, T> factory;
    private final Map<Direction, LazyOptional<T>> perSide = new EnumMap<>(Direction.class);
    private LazyOptional<T> sideless;

    public SidedCaps(Function<Direction, T> factory) {
        this.factory = factory;
    }

    public LazyOptional<T> get(@Nullable Direction side) {
        if (side == null) {
            if (sideless == null || !sideless.isPresent()) {
                sideless = create(null);
            }
            return sideless;
        }
        LazyOptional<T> cached = perSide.get(side);
        if (cached == null || !cached.isPresent()) {
            cached = create(side);
            perSide.put(side, cached);
        }
        return cached;
    }

    private LazyOptional<T> create(@Nullable Direction side) {
        T value = factory.apply(side);
        return value == null ? LazyOptional.empty() : LazyOptional.of(() -> value);
    }

    public void invalidate() {
        if (sideless != null) {
            sideless.invalidate();
            sideless = null;
        }
        perSide.values().forEach(LazyOptional::invalidate);
        perSide.clear();
    }
}
