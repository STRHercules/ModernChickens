package com.modernfluidcows.compat.wthit;

import com.modernfluidcows.ModernFluidCows;
import net.minecraft.resources.ResourceLocation;

/**
 * Centralises the configuration keys shared between the common and client WTHIT plugins.
 *
 * <p>Keeping the {@link ResourceLocation} instances in one place avoids typos when
 * registering configs or checking them inside data providers.</p>
 */
public final class WthitOptions {
    /** Toggles the fluid line in the entity tooltip. */
    public static final ResourceLocation SHOW_FLUID =
            ResourceLocation.fromNamespaceAndPath(ModernFluidCows.MOD_ID, "wthit.fluid");

    /** Toggles the milk cooldown line in the entity tooltip. */
    public static final ResourceLocation SHOW_COOLDOWN =
            ResourceLocation.fromNamespaceAndPath(ModernFluidCows.MOD_ID, "wthit.cooldown");

    private WthitOptions() {
        // Utility holder.
    }
}
