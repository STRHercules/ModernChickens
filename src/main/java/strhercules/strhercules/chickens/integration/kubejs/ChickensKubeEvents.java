package strhercules.chickens.integration.kubejs;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;


public interface ChickensKubeEvents {
    EventGroup GROUP = EventGroup.of("ChickensEvents");

   
    EventHandler REGISTRY = GROUP.startup("registry", () -> ChickenRegistryKubeEvent.class);
}
