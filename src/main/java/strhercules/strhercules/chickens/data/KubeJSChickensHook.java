package strhercules.chickens.data;

import strhercules.chickens.ChickensRegistryItem;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.List;


final class KubeJSChickensHook {
    private static final Logger LOGGER = LoggerFactory.getLogger("ChickensKubeJSHook");
    private static final String REGISTRAR = "strhercules.chickens.integration.kubejs.KubeJSChickenRegistrar";

    private KubeJSChickensHook() {
    }

    static void register(List<ChickensRegistryItem> chickens) {
        if (!ModList.get().isLoaded("kubejs")) {
            return;
        }

        try {
            Method register = Class.forName(REGISTRAR).getMethod("register", List.class);
            register.invoke(null, chickens);
        } catch (Throwable throwable) {
            LOGGER.warn("KubeJS is installed but the chicken registry event could not be posted", throwable);
        }
    }

    static void finalizeRegistry(List<ChickensRegistryItem> chickens) {
        if (!ModList.get().isLoaded("kubejs")) {
            return;
        }

        try {
            Method finalize = Class.forName(REGISTRAR).getMethod("finalizeRegistry", List.class);
            finalize.invoke(null, chickens);
        } catch (Throwable throwable) {
            LOGGER.warn("KubeJS is installed but the chicken registry finalization could not be applied", throwable);
        }
    }
}
