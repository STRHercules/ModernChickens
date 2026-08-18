package strhercules.chickens;

import strhercules.chickens.config.ChickensConfigHolder;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;

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
        MinecraftForge.EVENT_BUS.addListener(RoostEggPreventer::onEntityTick);
    }

    private static void onEntityTick(LivingEvent.LivingTickEvent event) {
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
