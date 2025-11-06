package com.setycz.chickens.integration.mekanism;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Reflection-driven bridge into Mekanism's chemical registry. The mod does not
 * depend on Mekanism at compile time, so this helper inspects the API at
 * runtime when it is present and extracts the data required to mirror gas and
 * chemical resources as chickens.
 */
public final class MekanismChemicalHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger("ChickensMekanismHook");

    private static final boolean AVAILABLE;
    private static final Registry<Object> CHEMICAL_REGISTRY;
    private static final ResourceLocation EMPTY_CHEMICAL_NAME;
    private static final Method CHEMICAL_GET_ICON;
    private static final Method CHEMICAL_GET_TINT;
    private static final Method CHEMICAL_GET_TEXT_COMPONENT;
    private static final Method CHEMICAL_IS_GASEOUS;
    private static final Method CHEMICAL_IS_RADIOACTIVE;

    private static final Class<?> CHEMICAL_CLASS;
    private static final Class<?> CHEMICAL_STACK_CLASS;
    private static final Class<?> CHEMICAL_HANDLER_CLASS;
    private static final Class<?> ACTION_CLASS;

    private static final Object ACTION_EXECUTE;
    private static final Object ACTION_SIMULATE;
    private static final Object EMPTY_STACK;

    private static final Method CHEMICAL_STACK_GET_AMOUNT;
    private static final Method CHEMICAL_STACK_IS_EMPTY;
    private static final Method CHEMICAL_STACK_GET_CHEMICAL;
    private static final Method CHEMICAL_HANDLER_INSERT;
    private static final Method CHEMICAL_HANDLER_EXTRACT_AMOUNT;
    private static final Method CHEMICAL_HANDLER_EXTRACT_STACK;
    private static final Method REGISTRY_WRAP_AS_HOLDER;

    @SuppressWarnings("rawtypes")
    private static final java.lang.reflect.Constructor CHEMICAL_STACK_CTOR;

    @SuppressWarnings("rawtypes")
    private static final java.lang.reflect.Constructor CHEMICAL_STACK_CTOR_LEGACY;

    private static final net.neoforged.neoforge.capabilities.BlockCapability<Object, Direction> CHEMICAL_BLOCK_CAPABILITY;

    static {
        boolean present = false;
        Registry<Object> registry = null;
        ResourceLocation empty = null;
        Method getIcon = null;
        Method getTint = null;
        Method getTextComponent = null;
        Method isGaseous = null;
        Method isRadioactive = null;
        Class<?> chemicalClass = null;
        Class<?> stackClass = null;
        Class<?> handlerClass = null;
        Class<?> actionClass = null;
        Object actionExecute = null;
        Object actionSimulate = null;
        Object emptyStack = null;
        Method getAmount = null;
        Method isEmpty = null;
        Method getChemical = null;
        Method insert = null;
        Method extractAmount = null;
        Method extractStack = null;
        java.lang.reflect.Constructor<?> stackCtor = null;
        java.lang.reflect.Constructor<?> legacyStackCtor = null;
        BlockCapability<Object, Direction> blockCapability = null;
        Method wrapAsHolderLocal = null;
        try {
            Class<?> apiClass = Class.forName("mekanism.api.MekanismAPI");
            Field chemicalRegistryField = apiClass.getField("CHEMICAL_REGISTRY");
            @SuppressWarnings("unchecked")
            Registry<Object> castRegistry = (Registry<Object>) chemicalRegistryField.get(null);
            registry = castRegistry;

            if (registry != null) {
                try {
                    wrapAsHolderLocal = registry.getClass().getMethod("wrapAsHolder", Object.class);
                } catch (NoSuchMethodException ignored) {
                    wrapAsHolderLocal = null;
                }
            }

            Field emptyChemicalField;
            try {
                emptyChemicalField = apiClass.getField("EMPTY_CHEMICAL_NAME");
                empty = (ResourceLocation) emptyChemicalField.get(null);
            } catch (NoSuchFieldException ignored) {
                // Mekanism 10.7.11+ renamed EMPTY_CHEMICAL_NAME to EMPTY_CHEMICAL_KEY.
                try {
                    Field emptyKeyField = apiClass.getField("EMPTY_CHEMICAL_KEY");
                    Object holderKey = emptyKeyField.get(null);
                    if (holderKey instanceof net.minecraft.resources.ResourceKey<?> resourceKey) {
                        empty = resourceKey.location();
                    }
                } catch (NoSuchFieldException secondary) {
                    // Ignore; fallback leaves empty null.
                }
            }

            chemicalClass = Class.forName("mekanism.api.chemical.Chemical");
            try {
                // Mekanism 10.7 renamed getTexture -> getIcon; prefer the modern name but keep legacy support.
                getIcon = chemicalClass.getMethod("getIcon");
            } catch (NoSuchMethodException missingIcon) {
                getIcon = chemicalClass.getMethod("getTexture");
            }
            getTint = chemicalClass.getMethod("getTint");
            getTextComponent = chemicalClass.getMethod("getTextComponent");
            isGaseous = chemicalClass.getMethod("isGaseous");
            isRadioactive = chemicalClass.getMethod("isRadioactive");

            stackClass = Class.forName("mekanism.api.chemical.ChemicalStack");
            handlerClass = Class.forName("mekanism.api.chemical.IChemicalHandler");
            actionClass = Class.forName("mekanism.api.Action");
            @SuppressWarnings("unchecked")
            Enum<?> execute = Enum.valueOf((Class<Enum>) actionClass, "EXECUTE");
            @SuppressWarnings("unchecked")
            Enum<?> simulate = Enum.valueOf((Class<Enum>) actionClass, "SIMULATE");
            actionExecute = execute;
            actionSimulate = simulate;
            emptyStack = stackClass.getField("EMPTY").get(null);
            try {
                stackCtor = stackClass.getConstructor(Class.forName("net.minecraft.core.Holder"), long.class);
            } catch (NoSuchMethodException ctorMissing) {
                stackCtor = null;
            }
            try {
                legacyStackCtor = stackClass.getConstructor(chemicalClass, long.class);
            } catch (NoSuchMethodException legacyMissing) {
                legacyStackCtor = null;
            }
            getAmount = stackClass.getMethod("getAmount");
            isEmpty = stackClass.getMethod("isEmpty");
            getChemical = stackClass.getMethod("getChemical");
            insert = handlerClass.getMethod("insertChemical", stackClass, actionClass);
            extractAmount = handlerClass.getMethod("extractChemical", long.class, actionClass);
            extractStack = handlerClass.getMethod("extractChemical", stackClass, actionClass);

            @SuppressWarnings("unchecked")
            BlockCapability<Object, Direction> capability = (BlockCapability<Object, Direction>) BlockCapability.createSided(
                    ResourceLocation.fromNamespaceAndPath("mekanism", "chemical_handler"), (Class<Object>) handlerClass);
            blockCapability = capability;
            present = true;
        } catch (ReflectiveOperationException ex) {
            LOGGER.debug("Mekanism API not detected; chemical chickens will stay disabled", ex);
        }
        AVAILABLE = present;
        CHEMICAL_REGISTRY = registry;
        EMPTY_CHEMICAL_NAME = empty;
        CHEMICAL_GET_ICON = getIcon;
        CHEMICAL_GET_TINT = getTint;
        CHEMICAL_GET_TEXT_COMPONENT = getTextComponent;
        CHEMICAL_IS_GASEOUS = isGaseous;
        CHEMICAL_IS_RADIOACTIVE = isRadioactive;
        CHEMICAL_CLASS = chemicalClass;
        CHEMICAL_STACK_CLASS = stackClass;
        CHEMICAL_HANDLER_CLASS = handlerClass;
        ACTION_CLASS = actionClass;
        ACTION_EXECUTE = actionExecute;
        ACTION_SIMULATE = actionSimulate;
        EMPTY_STACK = emptyStack;
        CHEMICAL_STACK_GET_AMOUNT = getAmount;
        CHEMICAL_STACK_IS_EMPTY = isEmpty;
        CHEMICAL_STACK_GET_CHEMICAL = getChemical;
        CHEMICAL_HANDLER_INSERT = insert;
        CHEMICAL_HANDLER_EXTRACT_AMOUNT = extractAmount;
        CHEMICAL_HANDLER_EXTRACT_STACK = extractStack;
        CHEMICAL_STACK_CTOR = stackCtor;
        CHEMICAL_STACK_CTOR_LEGACY = legacyStackCtor;
        CHEMICAL_BLOCK_CAPABILITY = blockCapability;
        REGISTRY_WRAP_AS_HOLDER = wrapAsHolderLocal;
    }

    private MekanismChemicalHelper() {
    }

    public static boolean isAvailable() {
        return AVAILABLE && CHEMICAL_REGISTRY != null
                && CHEMICAL_GET_ICON != null
                && CHEMICAL_GET_TINT != null
                && CHEMICAL_GET_TEXT_COMPONENT != null
                && CHEMICAL_IS_GASEOUS != null
                && CHEMICAL_IS_RADIOACTIVE != null;
    }

    public static Collection<ChemicalData> getChemicals() {
        if (!isAvailable()) {
            return Collections.emptyList();
        }
        List<ChemicalData> results = new ArrayList<>();
        for (Object chemical : CHEMICAL_REGISTRY) {
            try {
                ResourceLocation id = CHEMICAL_REGISTRY.getKey(chemical);
                if (id == null || id.equals(EMPTY_CHEMICAL_NAME)) {
                    continue;
                }
                ResourceLocation texture = (ResourceLocation) CHEMICAL_GET_ICON.invoke(chemical);
                if (texture == null) {
                    continue;
                }
                int tint = (int) CHEMICAL_GET_TINT.invoke(chemical);
                Component name = ((Component) CHEMICAL_GET_TEXT_COMPONENT.invoke(chemical)).copy();
                boolean gaseous = (boolean) CHEMICAL_IS_GASEOUS.invoke(chemical);
                boolean radioactive = (boolean) CHEMICAL_IS_RADIOACTIVE.invoke(chemical);
                results.add(new ChemicalData(id, texture, name, tint, gaseous, radioactive));
            } catch (IllegalAccessException | InvocationTargetException ex) {
                LOGGER.warn("Unable to read Mekanism chemical data", ex);
            }
        }
        return results;
    }

    /**
     * @return {@code true} when Mekanism's chemical capability classes are
     *         present on the classpath and reflective lookups succeeded.
     */
    public static boolean isChemicalCapabilityAvailable() {
        return isAvailable()
                && CHEMICAL_HANDLER_CLASS != null
                && CHEMICAL_BLOCK_CAPABILITY != null
                && ACTION_CLASS != null
                && (CHEMICAL_STACK_CTOR_LEGACY != null || CHEMICAL_STACK_CTOR != null);
    }

    @Nullable
    public static BlockCapability<Object, Direction> getChemicalBlockCapability() {
        return isChemicalCapabilityAvailable() ? CHEMICAL_BLOCK_CAPABILITY : null;
    }

    @Nullable
    public static Object createStack(ResourceLocation id, long amount) {
        if (id == null || amount <= 0) {
            return EMPTY_STACK;
        }
        Object chemical = getChemical(id);
        if (chemical == null) {
            return EMPTY_STACK;
        }
        return createStack(chemical, amount);
    }

    @Nullable
    public static Object createStack(Object chemical, long amount) {
        if (!isChemicalCapabilityAvailable() || chemical == null || amount <= 0) {
            return EMPTY_STACK;
        }
        try {
            if (CHEMICAL_STACK_CTOR_LEGACY != null && CHEMICAL_CLASS != null
                    && CHEMICAL_CLASS.isInstance(chemical)) {
                return CHEMICAL_STACK_CTOR_LEGACY.newInstance(chemical, amount);
            }
            if (CHEMICAL_STACK_CTOR != null && REGISTRY_WRAP_AS_HOLDER != null && CHEMICAL_REGISTRY != null) {
                Object holder = REGISTRY_WRAP_AS_HOLDER.invoke(CHEMICAL_REGISTRY, chemical);
                if (holder != null) {
                    return CHEMICAL_STACK_CTOR.newInstance(holder, amount);
                }
            }
        } catch (ReflectiveOperationException ex) {
            LOGGER.warn("Unable to construct Mekanism ChemicalStack", ex);
        }
        return EMPTY_STACK;
    }

    public static boolean isStackEmpty(@Nullable Object stack) {
        if (stack == null || CHEMICAL_STACK_CLASS == null || CHEMICAL_STACK_IS_EMPTY == null) {
            return true;
        }
        try {
            return (boolean) CHEMICAL_STACK_IS_EMPTY.invoke(stack);
        } catch (IllegalAccessException | InvocationTargetException ex) {
            LOGGER.warn("Unable to inspect Mekanism ChemicalStack emptiness", ex);
            return true;
        }
    }

    public static long getStackAmount(@Nullable Object stack) {
        if (stack == null || CHEMICAL_STACK_CLASS == null || CHEMICAL_STACK_GET_AMOUNT == null) {
            return 0L;
        }
        try {
            return (long) CHEMICAL_STACK_GET_AMOUNT.invoke(stack);
        } catch (IllegalAccessException | InvocationTargetException ex) {
            LOGGER.warn("Unable to read Mekanism ChemicalStack amount", ex);
            return 0L;
        }
    }

    @Nullable
    public static ResourceLocation getStackChemicalId(@Nullable Object stack) {
        if (stack == null || CHEMICAL_STACK_GET_CHEMICAL == null) {
            return null;
        }
        try {
            Object chemical = CHEMICAL_STACK_GET_CHEMICAL.invoke(stack);
            return getChemicalId(chemical);
        } catch (IllegalAccessException | InvocationTargetException ex) {
            LOGGER.warn("Unable to read Mekanism ChemicalStack chemical", ex);
            return null;
        }
    }

    public static Object emptyStack() {
        return EMPTY_STACK;
    }

    public static Object insertChemical(Object handler, Object stack, boolean simulate) {
        if (!isChemicalCapabilityAvailable() || handler == null || stack == null || CHEMICAL_HANDLER_INSERT == null) {
            return stack;
        }
        try {
            Object action = simulate ? ACTION_SIMULATE : ACTION_EXECUTE;
            return CHEMICAL_HANDLER_INSERT.invoke(handler, stack, action);
        } catch (IllegalAccessException | InvocationTargetException ex) {
            LOGGER.warn("Unable to insert chemical into Mekanism handler", ex);
            return stack;
        }
    }

    public static Object extractChemical(Object handler, long amount, boolean simulate) {
        if (!isChemicalCapabilityAvailable() || handler == null || CHEMICAL_HANDLER_EXTRACT_AMOUNT == null || amount <= 0) {
            return EMPTY_STACK;
        }
        try {
            Object action = simulate ? ACTION_SIMULATE : ACTION_EXECUTE;
            return CHEMICAL_HANDLER_EXTRACT_AMOUNT.invoke(handler, amount, action);
        } catch (IllegalAccessException | InvocationTargetException ex) {
            LOGGER.warn("Unable to extract chemical from Mekanism handler", ex);
            return EMPTY_STACK;
        }
    }

    public static Object extractChemical(Object handler, Object template, boolean simulate) {
        if (!isChemicalCapabilityAvailable() || handler == null || template == null
                || CHEMICAL_HANDLER_EXTRACT_STACK == null) {
            return EMPTY_STACK;
        }
        try {
            Object action = simulate ? ACTION_SIMULATE : ACTION_EXECUTE;
            return CHEMICAL_HANDLER_EXTRACT_STACK.invoke(handler, template, action);
        } catch (IllegalAccessException | InvocationTargetException ex) {
            LOGGER.warn("Unable to extract typed chemical from Mekanism handler", ex);
            return EMPTY_STACK;
        }
    }

    @Nullable
    public static Object getBlockChemicalHandler(@Nullable Level level, BlockPos pos, Direction direction) {
        if (!isChemicalCapabilityAvailable() || level == null || CHEMICAL_BLOCK_CAPABILITY == null) {
            return null;
        }
        return level.getCapability(CHEMICAL_BLOCK_CAPABILITY, pos, direction);
    }

    public static Object getAction(boolean execute) {
        return execute ? ACTION_EXECUTE : ACTION_SIMULATE;
    }

    @Nullable
    public static Object getChemical(ResourceLocation id) {
        if (!isAvailable() || id == null || CHEMICAL_REGISTRY == null) {
            return null;
        }
        return CHEMICAL_REGISTRY.get(id);
    }

    @Nullable
    public static ResourceLocation getChemicalId(Object chemical) {
        if (!isAvailable() || chemical == null || CHEMICAL_REGISTRY == null) {
            return null;
        }
        return CHEMICAL_REGISTRY.getKey(chemical);
    }

    public record ChemicalData(ResourceLocation id,
                                ResourceLocation texture,
                                Component displayName,
                                int tint,
                                boolean gaseous,
                                boolean radioactive) {
    }
}
