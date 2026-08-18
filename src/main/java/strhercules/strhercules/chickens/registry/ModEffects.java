package strhercules.chickens.registry;

import strhercules.chickens.ChickensMod;
import strhercules.chickens.effect.BurningEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(
            Registries.MOB_EFFECT, ChickensMod.MOD_ID);

    public static final RegistryObject<BurningEffect> BURNING = MOB_EFFECTS.register(
            "burning", BurningEffect::new);

    private ModEffects() {
    }

    public static void init(IEventBus modBus) {
        MOB_EFFECTS.register(modBus);
    }
}
