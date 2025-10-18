package com.setycz.chickens;

import com.setycz.chickens.config.ChickensConfigHolder;
import net.minecraft.world.entity.animal.Chicken;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Disables vanilla egg laying so the Roost gameplay loop mirrors the legacy
 * mod. Vanilla chickens would otherwise spam eggs, trivialising the new
 * automation blocks.
 */
public final class RoostEggPreventer {
    private RoostEggPreventer() {
    }

    /**
     * Registers the living tick listener once the mod finishes bootstrapping.
     */
    public static void init() {
        NeoForge.EVENT_BUS.addListener(RoostEggPreventer::onEntityTick);
    }

    private static void onEntityTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof Chicken chicken)) {
            return;
        }
        if (!ChickensConfigHolder.get().isVanillaEggLayingDisabled()) {
            return;
        }
        if (chicken.getClass() != Chicken.class) {
            // Respect modded chicken behaviour. The Chickens entity retains its own
            // lay timers and should not be clamped by the Roost rule.
            return;
        }
        if (chicken.eggTime <= 1) {
            chicken.eggTime = 20 * 60 * 60; // Effectively disable natural egg laying (~1 hour in ticks).
        }
    }
}
