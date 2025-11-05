package com.setycz.chickens.integration.mekanism;

import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    static {
        boolean present = false;
        Registry<Object> registry = null;
        ResourceLocation empty = null;
        Method getIcon = null;
        Method getTint = null;
        Method getTextComponent = null;
        Method isGaseous = null;
        Method isRadioactive = null;
        try {
            Class<?> apiClass = Class.forName("mekanism.api.MekanismAPI");
            Field chemicalRegistryField = apiClass.getField("CHEMICAL_REGISTRY");
            @SuppressWarnings("unchecked")
            Registry<Object> castRegistry = (Registry<Object>) chemicalRegistryField.get(null);
            registry = castRegistry;

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

            Class<?> chemicalClass = Class.forName("mekanism.api.chemical.Chemical");
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
