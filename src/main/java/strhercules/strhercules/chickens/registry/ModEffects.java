package strhercules.chickens.registry;

import strhercules.chickens.ChickensMod;
import strhercules.chickens.effect.BurningEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(
            Registries.MOB_EFFECT, ChickensMod.MOD_ID);

    public static final DeferredHolder<MobEffect, BurningEffect> BURNING = MOB_EFFECTS.register(
            "burning", BurningEffect::new);

    private ModEffects() {
    }

    public static void init(IEventBus modBus) {
        MOB_EFFECTS.register(modBus);
    }
}
