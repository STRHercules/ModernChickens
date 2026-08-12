package strhercules.chickens.integration.kubejs;

import dev.latvian.mods.kubejs.script.ScriptType;
import strhercules.chickens.ChickensRegistryItem;

import java.util.List;


public final class KubeJSChickenRegistrar {
    private static ChickenRegistryKubeEvent activeEvent;

    private KubeJSChickenRegistrar() {
    }

  
    public static void register(List<ChickensRegistryItem> chickens) {
        if (!ChickensKubeEvents.REGISTRY.hasListeners()) {
            return;
        }

        ChickenRegistryKubeEvent event = new ChickenRegistryKubeEvent(chickens);
        ChickensKubeEvents.REGISTRY.post(ScriptType.STARTUP, event);
        event.apply();
        activeEvent = event;
    }

    public static void finalizeRegistry(List<ChickensRegistryItem> chickens) {
        if (activeEvent != null) {
            activeEvent.applyFinal(chickens);
        }
    }
}
