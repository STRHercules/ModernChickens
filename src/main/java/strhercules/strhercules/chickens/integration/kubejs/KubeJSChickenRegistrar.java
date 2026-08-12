package strhercules.chickens.integration.kubejs;

import dev.latvian.mods.kubejs.script.ScriptType;
import strhercules.chickens.ChickensRegistryItem;

import java.util.List;


public final class KubeJSChickenRegistrar {
    private KubeJSChickenRegistrar() {
    }

  
    public static void register(List<ChickensRegistryItem> chickens) {
        if (!ChickensKubeEvents.REGISTRY.hasListeners()) {
            return;
        }

        ChickenRegistryKubeEvent event = new ChickenRegistryKubeEvent(chickens);
        ChickensKubeEvents.REGISTRY.post(ScriptType.STARTUP, event);
        event.apply();
    }
}
