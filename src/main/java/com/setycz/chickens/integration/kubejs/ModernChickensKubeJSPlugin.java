package com.setycz.chickens.integration.kubejs;

import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.plugin.ClassFilter;
import dev.latvian.mods.kubejs.script.BindingRegistry;

public class ModernChickensKubeJSPlugin implements KubeJSPlugin {
    
    @Override
    public void registerClasses(ClassFilter filter) {
        filter.allow("com.setycz.chickens.integration.kubejs");
        filter.allow("com.setycz.chickens.ChickensRegistry");
        filter.allow("com.setycz.chickens.ChickensRegistryItem");
        filter.allow("com.setycz.chickens.SpawnType");
    }
    
    @Override
    public void registerBindings(BindingRegistry bindings) {
        bindings.add("ChickensEvents", ChickenRegistryType.ChickensEventsWrapper.class);
    }
}