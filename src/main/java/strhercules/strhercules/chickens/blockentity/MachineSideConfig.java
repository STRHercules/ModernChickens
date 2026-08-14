package strhercules.chickens.blockentity;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;

/** Persisted per-face automation policy shared by the mod's machines. */
public final class MachineSideConfig {
    public enum Channel {
        ITEMS,
        FLUIDS,
        CHEMICALS,
        ENERGY
    }

    public enum Mode {
        NONE,
        INPUT,
        OUTPUT,
        BOTH;

        public boolean allowsInput() {
            return this == INPUT || this == BOTH;
        }

        public boolean allowsOutput() {
            return this == OUTPUT || this == BOTH;
        }

        public Mode next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    private static final String TAG = "MachineSideConfig";
    private final Mode[][] modes = new Mode[Direction.values().length][Channel.values().length];

    public MachineSideConfig() {
        reset();
    }

    public void reset() {
        for (Direction direction : Direction.values()) {
            for (Channel channel : Channel.values()) {
                modes[direction.ordinal()][channel.ordinal()] = Mode.BOTH;
            }
        }
    }

    public Mode get(@Nullable Direction direction, Channel channel) {
        if (direction == null) {
            return Mode.BOTH;
        }
        return modes[direction.ordinal()][channel.ordinal()];
    }

    public boolean allows(@Nullable Direction direction, Channel channel, boolean input) {
        Mode mode = get(direction, channel);
        return input ? mode.allowsInput() : mode.allowsOutput();
    }

    public Mode cycle(@Nullable Direction direction, Channel channel) {
        if (direction == null) {
            return Mode.BOTH;
        }
        Mode next = get(direction, channel).next();
        modes[direction.ordinal()][channel.ordinal()] = next;
        return next;
    }

    public void save(CompoundTag parent) {
        CompoundTag config = new CompoundTag();
        for (Direction direction : Direction.values()) {
            CompoundTag side = new CompoundTag();
            for (Channel channel : Channel.values()) {
                side.putByte(channel.name(), (byte) get(direction, channel).ordinal());
            }
            config.put(direction.getName(), side);
        }
        parent.put(TAG, config);
    }

    public void load(CompoundTag parent) {
        reset();
        if (!parent.contains(TAG, CompoundTag.TAG_COMPOUND)) {
            return;
        }
        CompoundTag config = parent.getCompound(TAG);
        for (Direction direction : Direction.values()) {
            if (!config.contains(direction.getName(), CompoundTag.TAG_COMPOUND)) {
                continue;
            }
            CompoundTag side = config.getCompound(direction.getName());
            for (Channel channel : Channel.values()) {
                if (!side.contains(channel.name(), CompoundTag.TAG_BYTE)) {
                    continue;
                }
                int ordinal = side.getByte(channel.name());
                if (ordinal >= 0 && ordinal < Mode.values().length) {
                    modes[direction.ordinal()][channel.ordinal()] = Mode.values()[ordinal];
                }
            }
        }
    }
}
