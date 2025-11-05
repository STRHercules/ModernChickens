package com.modernfluidcows.config;

import java.util.Objects;

/**
 * Order-insensitive pair used by the breeding table loader.
 */
public final class CustomPair<L, R> {
    private final L left;
    private final R right;

    private CustomPair(final L left, final R right) {
        this.left = left;
        this.right = right;
    }

    public static <L, R> CustomPair<L, R> of(final L left, final R right) {
        return new CustomPair<>(left, right);
    }

    public L left() {
        return left;
    }

    public R right() {
        return right;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CustomPair<?, ?> other)) {
            return false;
        }
        return (Objects.equals(left, other.left) && Objects.equals(right, other.right))
                || (Objects.equals(left, other.right) && Objects.equals(right, other.left));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(left) ^ Objects.hashCode(right);
    }
}
