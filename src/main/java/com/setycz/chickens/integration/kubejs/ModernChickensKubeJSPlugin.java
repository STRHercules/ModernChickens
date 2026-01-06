package com.setycz.chickens.integration.kubejs;

import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.plugin.ClassFilter;
import dev.latvian.mods.kubejs.script.BindingRegistry;

public class ModernChickensKubeJSPlugin implements KubeJSPlugin {
    
    @Override
    public void registerClasses(ClassFilter filter) {
        filter.allow("com.setycz.chickens.integration.kubejs");
        // Allow access to the machine recipe registry helpers exposed to KubeJS scripts.
        filter.allow("com.setycz.chickens.integration.kubejs.MachineRecipeRegistry");
        filter.allow("com.setycz.chickens.integration.kubejs.MachineRecipeRegistryType");
        filter.allow("com.setycz.chickens.ChickensRegistry");
        filter.allow("com.setycz.chickens.ChickensRegistryItem");
        filter.allow("com.setycz.chickens.SpawnType");
    }
    
    @Override
    public void registerBindings(BindingRegistry bindings) {
        bindings.add("ChickensEvents", ChickenRegistryType.ChickensEventsWrapper.class);
        // Bind a dedicated machine-recipe event for KubeJS scripts.
        bindings.add("ChickensMachineRecipes", MachineRecipeRegistryType.MachineRecipesWrapper.class);
    }
}
