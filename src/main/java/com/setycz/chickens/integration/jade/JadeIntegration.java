package com.setycz.chickens.integration.jade;

import com.setycz.chickens.config.ChickensConfigHolder;
import com.setycz.chickens.entity.ChickensChicken;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.function.Consumer;

/**
 * Wires Chickens back into Jade using the legacy Hwyla/Waila IMC bridge. Jade still
 * honours {@code register} callbacks, which lets us avoid a hard dependency on the
 * API while restoring the in-world stat overlay from 1.10.2.
 */
public final class JadeIntegration {
    private static final Logger LOGGER = LoggerFactory.getLogger("ChickensJade");

    private JadeIntegration() {
    }

    /**
     * Initialises the integration if Jade (or another Waila-compatible overlay)
     * is present in the current mod list.
     */
    public static void init() {
        ModList mods = ModList.get();
        String target = null;
        if (mods.isLoaded("jade")) {
            target = "jade";
        } else if (mods.isLoaded("waila")) {
            target = "waila";
        }

        if (target == null) {
            return;
        }

        LOGGER.info("Registering Chickens overlay with {} via legacy IMC", target);
        String finalTarget = target;
        InterModComms.sendTo(target, "register", () -> (Consumer<Object>) registrar ->
                registerProvider(finalTarget, registrar));
    }

    private static void registerProvider(String target, Object registrar) {
        try {
            Class<?> registrarClass = Class.forName("mcp.mobius.waila.api.IWailaRegistrar");
            Class<?> providerClass = Class.forName("mcp.mobius.waila.api.IWailaEntityProvider");

            Object provider = Proxy.newProxyInstance(providerClass.getClassLoader(), new Class<?>[]{providerClass},
                    new TooltipProvider());

            Method registerBody = registrarClass.getMethod("registerBodyProvider", providerClass, Class.class);
            registerBody.invoke(registrar, provider, ChickensChicken.class);
        } catch (ClassNotFoundException ex) {
            LOGGER.warn("{} advertised Waila compatibility but the API was missing", target);
        } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException ex) {
            LOGGER.warn("Unable to register Chickens Jade overlay", ex);
        }
    }

    /**
     * Reflection backed implementation of {@code IWailaEntityProvider}. Only the
     * {@code getWailaBody} method is used to add tooltip lines so the other hooks
     * simply defer to the default behaviour by returning {@code null}.
     */
    private static final class TooltipProvider implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            if ("getWailaBody".equals(name)) {
                return handleBody(args);
            }
            if ("getWailaHead".equals(name) || "getWailaTail".equals(name)) {
                return null;
            }
            if ("getWailaOverride".equals(name)) {
                return null;
            }
            if ("getNBTData".equals(name)) {
                // Preserve any existing tag provided by Jade instead of fabricating data
                // that might deserialise incorrectly on the client.
                return args != null && args.length > 2 ? args[2] : null;
            }
            return null;
        }

        private Object handleBody(Object[] args) {
            if (args == null || args.length < 2) {
                return null;
            }
            Object entity = args[0];
            Object tooltipObject = args[1];
            if (!(entity instanceof ChickensChicken chicken) || !(tooltipObject instanceof List<?> list)) {
                return tooltipObject;
            }

            @SuppressWarnings("unchecked")
            List<String> tooltip = (List<String>) list;
            tooltip.add(Component.translatable("entity.ChickensChicken.tier", chicken.getTier()).getString());

            boolean alwaysShow = ChickensConfigHolder.get().isAlwaysShowStats();
            if (chicken.getStatsAnalyzed() || alwaysShow) {
                tooltip.add(Component.translatable("entity.ChickensChicken.growth", chicken.getGrowth()).getString());
                tooltip.add(Component.translatable("entity.ChickensChicken.gain", chicken.getGain()).getString());
                tooltip.add(Component.translatable("entity.ChickensChicken.strength", chicken.getStrength()).getString());
            }

            if (!chicken.isBaby()) {
                int progress = chicken.getLayProgress();
                if (progress <= 0) {
                    tooltip.add(Component.translatable("entity.ChickensChicken.nextEggSoon").getString());
                } else {
                    tooltip.add(Component.translatable("entity.ChickensChicken.layProgress", progress).getString());
                }
            }

            return tooltip;
        }
    }
}

