package com.setycz.chickens.integration.jade;

import com.setycz.chickens.blockentity.AbstractChickenContainerBlockEntity;
import com.setycz.chickens.blockentity.BreederBlockEntity;
import com.setycz.chickens.blockentity.CollectorBlockEntity;
import com.setycz.chickens.blockentity.RoostBlockEntity;
import com.setycz.chickens.config.ChickensConfigHolder;
import com.setycz.chickens.entity.ChickensChicken;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
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
            Class<?> entityProviderClass = Class.forName("mcp.mobius.waila.api.IWailaEntityProvider");
            Class<?> blockProviderClass = Class.forName("mcp.mobius.waila.api.IWailaDataProvider");

            Object entityProvider = Proxy.newProxyInstance(entityProviderClass.getClassLoader(),
                    new Class<?>[]{entityProviderClass}, new TooltipProvider());
            Object blockProvider = Proxy.newProxyInstance(blockProviderClass.getClassLoader(),
                    new Class<?>[]{blockProviderClass}, new BlockTooltipProvider());

            Method registerBody = registrarClass.getMethod("registerBodyProvider", entityProviderClass, Class.class);
            registerBody.invoke(registrar, entityProvider, ChickensChicken.class);

            Method registerTail = registrarClass.getMethod("registerTailProvider", blockProviderClass, Class.class);
            registerTail.invoke(registrar, blockProvider, RoostBlockEntity.class);
            registerTail.invoke(registrar, blockProvider, BreederBlockEntity.class);
            registerTail.invoke(registrar, blockProvider, CollectorBlockEntity.class);

            Method registerNbt = registrarClass.getMethod("registerNBTProvider", blockProviderClass, Class.class);
            registerNbt.invoke(registrar, blockProvider, RoostBlockEntity.class);
            registerNbt.invoke(registrar, blockProvider, BreederBlockEntity.class);
            registerNbt.invoke(registrar, blockProvider, CollectorBlockEntity.class);
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

    /**
     * Reflection backed block provider used to show Roost/Breeder/Collector state
     * in Jade. The implementation mirrors the legacy 1.12 logic but translates it
     * into modern {@link Component} strings.
     */
    private static final class BlockTooltipProvider implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            if ("getWailaTail".equals(name)) {
                return handleTail(args);
            }
            if ("getNBTData".equals(name)) {
                return handleNbt(args);
            }
            return null;
        }

        private Object handleTail(Object[] args) throws Throwable {
            if (args == null || args.length < 3) {
                return args != null && args.length > 1 ? args[1] : null;
            }
            Object tooltipObject = args[1];
            Object accessor = args[2];
            if (!(tooltipObject instanceof List<?> list)) {
                return tooltipObject;
            }
            Method getTileEntity = accessor.getClass().getMethod("getTileEntity");
            Method getNbt = accessor.getClass().getMethod("getNBTData");
            Object tile = getTileEntity.invoke(accessor);
            Object nbtObj = getNbt.invoke(accessor);
            if (!(tile instanceof AbstractChickenContainerBlockEntity container) || !(list instanceof List<?>)) {
                return tooltipObject;
            }
            CompoundTag tag = nbtObj instanceof CompoundTag compound ? compound : new CompoundTag();
            List<Component> components = new java.util.ArrayList<>();
            container.appendTooltip(components, tag);
            @SuppressWarnings("unchecked")
            List<String> tooltip = (List<String>) list;
            for (Component component : components) {
                tooltip.add(component.getString());
            }
            return tooltip;
        }

        private Object handleNbt(Object[] args) {
            if (args == null || args.length < 3) {
                return args != null && args.length > 2 ? args[2] : null;
            }
            Object tile = args[1];
            Object tagObject = args[2];
            if (!(tile instanceof AbstractChickenContainerBlockEntity container)) {
                return tagObject;
            }
            CompoundTag tag = tagObject instanceof CompoundTag compound ? compound : new CompoundTag();
            container.storeTooltipData(tag);
            return tag;
        }
    }
}

