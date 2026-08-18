package strhercules.chickens.integration.mekanism;

import strhercules.chickens.blockentity.NeighbourCaps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.registries.IForgeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Reflection-driven bridge into the Mekanism chemical registries. The mod does
 * not depend on Mekanism at compile time, so this helper inspects the API at
 * runtime when it is present and extracts the data required to mirror gas and
 * chemical resources as chickens.
 */
public final class MekanismChemicalHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger("ChickensMekanismHook");

    private record ChemicalType(String name,
                                IForgeRegistry<Object> registry,
                                boolean gaseous,
                                ResourceLocation emptyId,
                                Constructor<?> stackConstructor,
                                @Nullable Capability<Object> capability,
                                @Nullable Class<?> handlerInterface) {
    }

    private static final boolean AVAILABLE;
    private static final List<ChemicalType> TYPES;

    private static final Method CHEMICAL_GET_ICON;
    private static final Method CHEMICAL_GET_TINT;
    private static final Method CHEMICAL_GET_TEXT_COMPONENT;
    private static final Method CHEMICAL_HAS_ATTRIBUTE;
    private static final Class<?> RADIATION_ATTRIBUTE_CLASS;

    private static final Class<?> CHEMICAL_STACK_CLASS;
    private static final Class<?> ACTION_CLASS;

    private static final Object ACTION_EXECUTE;
    private static final Object ACTION_SIMULATE;
    private static final Object EMPTY_STACK;

    private static final Method CHEMICAL_STACK_GET_AMOUNT;
    private static final Method CHEMICAL_STACK_IS_EMPTY;
    private static final Method CHEMICAL_STACK_GET_TYPE;
    private static final Method CHEMICAL_HANDLER_INSERT;
    private static final Method CHEMICAL_HANDLER_EXTRACT_AMOUNT;
    private static final Method CHEMICAL_HANDLER_EXTRACT_STACK;

    static {
        boolean present = false;
        List<ChemicalType> types = List.of();
        Method getIcon = null;
        Method getTint = null;
        Method getTextComponent = null;
        Method hasAttribute = null;
        Class<?> radiationClass = null;
        Class<?> chemicalStackClass = null;
        Class<?> actionClass = null;
        Object actionExecute = null;
        Object actionSimulate = null;
        Object emptyStack = null;
        Method getAmount = null;
        Method isEmpty = null;
        Method getType = null;
        Method insert = null;
        Method extractAmount = null;
        Method extractStack = null;
        try {
            Class<?> apiClass = Class.forName("mekanism.api.MekanismAPI");
            Class<?> chemicalClass = Class.forName("mekanism.api.chemical.Chemical");

            getIcon = chemicalClass.getMethod("getIcon");
            getTint = chemicalClass.getMethod("getTint");
            getTextComponent = chemicalClass.getMethod("getTextComponent");
            hasAttribute = chemicalClass.getMethod("has", Class.class);
            radiationClass = Class.forName("mekanism.api.chemical.gas.attribute.GasAttributes$Radiation");

            chemicalStackClass = Class.forName("mekanism.api.chemical.ChemicalStack");
            actionClass = Class.forName("mekanism.api.Action");
            @SuppressWarnings("unchecked")
            Enum<?> execute = Enum.valueOf((Class<Enum>) actionClass, "EXECUTE");
            @SuppressWarnings("unchecked")
            Enum<?> simulate = Enum.valueOf((Class<Enum>) actionClass, "SIMULATE");
            actionExecute = execute;
            actionSimulate = simulate;

            getAmount = chemicalStackClass.getMethod("getAmount");
            isEmpty = chemicalStackClass.getMethod("isEmpty");
            getType = chemicalStackClass.getMethod("getType");

            Class<?> chemicalHandlerClass = Class.forName("mekanism.api.chemical.IChemicalHandler");
            insert = chemicalHandlerClass.getMethod("insertChemical", chemicalStackClass, actionClass);
            extractAmount = chemicalHandlerClass.getMethod("extractChemical", long.class, actionClass);
            extractStack = chemicalHandlerClass.getMethod("extractChemical", chemicalStackClass, actionClass);

            List<ChemicalType> resolved = new ArrayList<>(4);
            addType(resolved, apiClass, chemicalClass, "gas", "gasRegistry", "EMPTY_GAS",
                    "mekanism.api.chemical.gas.GasStack", "mekanism.api.providers.IGasProvider",
                    "GAS_HANDLER", "mekanism.api.chemical.gas.IGasHandler", true);
            addType(resolved, apiClass, chemicalClass, "infuse_type", "infuseTypeRegistry", "EMPTY_INFUSE_TYPE",
                    "mekanism.api.chemical.infuse.InfusionStack", "mekanism.api.providers.IInfuseTypeProvider",
                    "INFUSION_HANDLER", "mekanism.api.chemical.infuse.IInfusionHandler", false);
            addType(resolved, apiClass, chemicalClass, "pigment", "pigmentRegistry", "EMPTY_PIGMENT",
                    "mekanism.api.chemical.pigment.PigmentStack", "mekanism.api.providers.IPigmentProvider",
                    "PIGMENT_HANDLER", "mekanism.api.chemical.pigment.IPigmentHandler", false);
            addType(resolved, apiClass, chemicalClass, "slurry", "slurryRegistry", "EMPTY_SLURRY",
                    "mekanism.api.chemical.slurry.SlurryStack", "mekanism.api.providers.ISlurryProvider",
                    "SLURRY_HANDLER", "mekanism.api.chemical.slurry.ISlurryHandler", false);

            if (resolved.isEmpty()) {
                throw new IllegalStateException("No Mekanism chemical registries could be resolved");
            }
            types = List.copyOf(resolved);
            emptyStack = Class.forName("mekanism.api.chemical.gas.GasStack").getField("EMPTY").get(null);
            present = true;
        } catch (ReflectiveOperationException | LinkageError | IllegalStateException ex) {
            LOGGER.debug("Mekanism API not detected; chemical chickens will stay disabled", ex);
        }
        AVAILABLE = present;
        TYPES = types;
        CHEMICAL_GET_ICON = getIcon;
        CHEMICAL_GET_TINT = getTint;
        CHEMICAL_GET_TEXT_COMPONENT = getTextComponent;
        CHEMICAL_HAS_ATTRIBUTE = hasAttribute;
        RADIATION_ATTRIBUTE_CLASS = radiationClass;
        CHEMICAL_STACK_CLASS = chemicalStackClass;
        ACTION_CLASS = actionClass;
        ACTION_EXECUTE = actionExecute;
        ACTION_SIMULATE = actionSimulate;
        EMPTY_STACK = emptyStack;
        CHEMICAL_STACK_GET_AMOUNT = getAmount;
        CHEMICAL_STACK_IS_EMPTY = isEmpty;
        CHEMICAL_STACK_GET_TYPE = getType;
        CHEMICAL_HANDLER_INSERT = insert;
        CHEMICAL_HANDLER_EXTRACT_AMOUNT = extractAmount;
        CHEMICAL_HANDLER_EXTRACT_STACK = extractStack;
    }

    @SuppressWarnings("unchecked")
    private static void addType(List<ChemicalType> target,
                                Class<?> apiClass,
                                Class<?> chemicalClass,
                                String name,
                                String registryAccessor,
                                String emptyField,
                                String stackClassName,
                                String providerClassName,
                                String capabilityField,
                                String handlerClassName,
                                boolean gaseous) {
        try {
            IForgeRegistry<Object> registry =
                    (IForgeRegistry<Object>) apiClass.getMethod(registryAccessor).invoke(null);
            if (registry == null) {
                return;
            }
            Object empty = apiClass.getField(emptyField).get(null);
            ResourceLocation emptyId =
                    (ResourceLocation) chemicalClass.getMethod("getRegistryName").invoke(empty);
            Constructor<?> stackConstructor = Class.forName(stackClassName)
                    .getConstructor(Class.forName(providerClassName), long.class);
            target.add(new ChemicalType(name, registry, gaseous, emptyId, stackConstructor,
                    resolveCapability(capabilityField), findClass(handlerClassName)));
        } catch (ReflectiveOperationException | LinkageError ex) {
            LOGGER.debug("Mekanism {} registry unavailable", name, ex);
        }
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private static Capability<Object> resolveCapability(String fieldName) {
        try {
            Class<?> capabilities = Class.forName("mekanism.common.capabilities.Capabilities");
            return (Capability<Object>) capabilities.getField(fieldName).get(null);
        } catch (ReflectiveOperationException | LinkageError ex) {
            LOGGER.debug("Mekanism capability {} unavailable", fieldName, ex);
            return null;
        }
    }

    private MekanismChemicalHelper() {
    }

    public static boolean isAvailable() {
        return AVAILABLE && !TYPES.isEmpty()
                && CHEMICAL_GET_ICON != null
                && CHEMICAL_GET_TINT != null
                && CHEMICAL_GET_TEXT_COMPONENT != null
                && CHEMICAL_HAS_ATTRIBUTE != null
                && RADIATION_ATTRIBUTE_CLASS != null;
    }

    public static Collection<ChemicalData> getChemicals() {
        if (!isAvailable()) {
            return Collections.emptyList();
        }
        List<ChemicalData> results = new ArrayList<>();
        for (ChemicalType type : TYPES) {
            for (Object chemical : type.registry().getValues()) {
                try {
                    ResourceLocation id = type.registry().getKey(chemical);
                    if (id == null || id.equals(type.emptyId())) {
                        continue;
                    }
                    ResourceLocation texture = (ResourceLocation) CHEMICAL_GET_ICON.invoke(chemical);
                    if (texture == null) {
                        continue;
                    }
                    int tint = (int) CHEMICAL_GET_TINT.invoke(chemical);
                    Component name = ((Component) CHEMICAL_GET_TEXT_COMPONENT.invoke(chemical)).copy();
                    results.add(new ChemicalData(id, texture, name, tint, type.gaseous(), hasRadiation(chemical)));
                } catch (IllegalAccessException | InvocationTargetException ex) {
                    LOGGER.warn("Unable to read Mekanism chemical data", ex);
                }
            }
        }
        return results;
    }

    private static boolean hasRadiation(Object chemical) {
        try {
            return (boolean) CHEMICAL_HAS_ATTRIBUTE.invoke(chemical, RADIATION_ATTRIBUTE_CLASS);
        } catch (IllegalAccessException | InvocationTargetException ex) {
            LOGGER.debug("Unable to inspect Mekanism chemical radioactivity", ex);
            return false;
        }
    }

    @Nullable
    private static ChemicalType typeForId(ResourceLocation id) {
        for (ChemicalType type : TYPES) {
            if (type.registry().containsKey(id)) {
                return type;
            }
        }
        return null;
    }

    @Nullable
    private static ChemicalType typeForChemical(Object chemical) {
        for (ChemicalType type : TYPES) {
            if (type.registry().getKey(chemical) != null) {
                return type;
            }
        }
        return null;
    }

    /**
     * @return {@code true} when the Mekanism chemical capability classes are
     *         present on the classpath and reflective lookups succeeded.
     */
    public static boolean isChemicalCapabilityAvailable() {
        return isAvailable() && ACTION_CLASS != null && !getChemicalCapabilities().isEmpty();
    }

    public static List<Capability<Object>> getChemicalCapabilities() {
        if (!AVAILABLE) {
            return List.of();
        }
        List<Capability<Object>> capabilities = new ArrayList<>(TYPES.size());
        for (ChemicalType type : TYPES) {
            if (type.capability() != null) {
                capabilities.add(type.capability());
            }
        }
        return capabilities;
    }

    public static boolean isChemicalCapability(Capability<?> capability) {
        for (ChemicalType type : TYPES) {
            if (type.capability() != null && type.capability() == capability) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public static Object createStack(ResourceLocation id, long amount) {
        if (id == null || amount <= 0 || !isAvailable()) {
            return EMPTY_STACK;
        }
        ChemicalType type = typeForId(id);
        if (type == null) {
            return EMPTY_STACK;
        }
        return buildStack(type, type.registry().getValue(id), amount);
    }

    @Nullable
    public static Object createStack(Object chemical, long amount) {
        if (!isAvailable() || chemical == null || amount <= 0) {
            return EMPTY_STACK;
        }
        ChemicalType type = typeForChemical(chemical);
        return type == null ? EMPTY_STACK : buildStack(type, chemical, amount);
    }

    @Nullable
    private static Object buildStack(ChemicalType type, @Nullable Object chemical, long amount) {
        if (chemical == null) {
            return EMPTY_STACK;
        }
        try {
            return type.stackConstructor().newInstance(chemical, amount);
        } catch (ReflectiveOperationException ex) {
            LOGGER.warn("Unable to construct a Mekanism {} stack", type.name(), ex);
            return EMPTY_STACK;
        }
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
        if (stack == null || CHEMICAL_STACK_GET_TYPE == null) {
            return null;
        }
        try {
            return getChemicalId(CHEMICAL_STACK_GET_TYPE.invoke(stack));
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
            LOGGER.warn("Unable to insert chemical into a Mekanism handler", ex);
            return stack;
        }
    }

    public static Object extractChemical(Object handler, long amount, boolean simulate) {
        if (!isChemicalCapabilityAvailable() || handler == null || CHEMICAL_HANDLER_EXTRACT_AMOUNT == null
                || amount <= 0) {
            return EMPTY_STACK;
        }
        try {
            Object action = simulate ? ACTION_SIMULATE : ACTION_EXECUTE;
            return CHEMICAL_HANDLER_EXTRACT_AMOUNT.invoke(handler, amount, action);
        } catch (IllegalAccessException | InvocationTargetException ex) {
            LOGGER.warn("Unable to extract chemical from a Mekanism handler", ex);
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
            LOGGER.warn("Unable to extract a typed chemical from a Mekanism handler", ex);
            return EMPTY_STACK;
        }
    }

    @Nullable
    public static Object getBlockChemicalHandler(@Nullable Level level, BlockPos pos, Direction direction) {
        if (level == null) {
            return null;
        }
        for (Capability<Object> capability : getChemicalCapabilities()) {
            Object handler = NeighbourCaps.find(level, pos, capability, direction);
            if (handler != null) {
                return handler;
            }
        }
        return null;
    }

    public static Object getAction(boolean execute) {
        return execute ? ACTION_EXECUTE : ACTION_SIMULATE;
    }

    @Nullable
    public static Object getChemical(ResourceLocation id) {
        if (!isAvailable() || id == null) {
            return null;
        }
        ChemicalType type = typeForId(id);
        return type == null ? null : type.registry().getValue(id);
    }

    @Nullable
    public static ResourceLocation getChemicalId(Object chemical) {
        if (!isAvailable() || chemical == null) {
            return null;
        }
        ChemicalType type = typeForChemical(chemical);
        return type == null ? null : type.registry().getKey(chemical);
    }

    public static boolean isRadioactive(@Nullable ResourceLocation id) {
        if (id == null || CHEMICAL_HAS_ATTRIBUTE == null || RADIATION_ATTRIBUTE_CLASS == null) {
            return false;
        }
        Object chemical = getChemical(id);
        return chemical != null && hasRadiation(chemical);
    }

    /** The capability that matches a chemical family. Exposing a single tank under
     *  every family would make Mekanism report the same contents once per family. */
    @Nullable
    public static Capability<Object> capabilityForChemical(@Nullable ResourceLocation id) {
        if (id == null) {
            return null;
        }
        ChemicalType type = typeForId(id);
        return type == null ? null : type.capability();
    }

    /** The handler interface backing a chemical capability. Each Mekanism chemical
     *  family declares its own { getEmptyStack} return type, so a proxy may only
     *  implement one of them at a time. */
    @Nullable
    public static Class<?> handlerInterfaceFor(Capability<?> capability) {
        for (ChemicalType type : TYPES) {
            if (type.capability() != null && type.capability() == capability) {
                return type.handlerInterface();
            }
        }
        return null;
    }

    @Nullable
    private static Class<?> findClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException | LinkageError ex) {
            LOGGER.debug("Mekanism handler interface {} unavailable", name, ex);
            return null;
        }
    }

    /** Returns the Mekanism runtime tint for a chemical, or white when unavailable. */
    public static int getChemicalTint(@Nullable ResourceLocation id) {
        if (id == null || CHEMICAL_GET_TINT == null) {
            return 0xFFFFFF;
        }
        Object chemical = getChemical(id);
        if (chemical == null) {
            return 0xFFFFFF;
        }
        try {
            return (int) CHEMICAL_GET_TINT.invoke(chemical);
        } catch (IllegalAccessException | InvocationTargetException ex) {
            LOGGER.debug("Unable to inspect Mekanism chemical tint", ex);
            return 0xFFFFFF;
        }
    }

    public record ChemicalData(ResourceLocation id,
                                ResourceLocation texture,
                                Component displayName,
                                int tint,
                                boolean gaseous,
                                boolean radioactive) {
    }
}
