package com.setycz.chickens.integration.jei;

import com.setycz.chickens.ChemicalEggRegistryItem;
import com.setycz.chickens.integration.mekanism.MekanismChemicalHelper;
import mezz.jei.api.ingredients.IIngredientType;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Reflection bridge that exposes Mekanism's JEI ingredient types so the mod can present
 * real chemical inputs inside JEI without a compile-time Mekanism dependency.
 */
public final class MekanismJeiChemicalHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger("ChickensJeiMekanismBridge");

    private static final List<Entry> ENTRIES = new ArrayList<>(4);
    private static final boolean AVAILABLE;

    static {
        boolean available = MekanismChemicalHelper.isAvailable();
        if (available) {
            available = discoverIngredientTypes();
        }
        AVAILABLE = available && !ENTRIES.isEmpty();
    }

    private MekanismJeiChemicalHelper() {
    }

    private static boolean discoverIngredientTypes() {
        try {
            Class<?> typesClass = Class.forName("mekanism.client.jei.MekanismJEIIngredientTypes");
            Field[] fields = typesClass.getFields();
            boolean discovered = false;
            for (Field field : fields) {
                if (!Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                Object value = field.get(null);
                if (!(value instanceof IIngredientType<?> ingredientType)) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                IIngredientType<Object> castType = (IIngredientType<Object>) ingredientType;
                Class<?> ingredientClass = castType.getIngredientClass();
                if (ingredientClass == null) {
                    continue;
                }
                ENTRIES.add(new Entry(ingredientClass, castType));
                discovered = true;
            }
            if (!discovered) {
                LOGGER.debug("Mekanism JEI ingredient types not discovered; fallback display will remain active.");
            }
            return discovered;
        } catch (ReflectiveOperationException ex) {
            LOGGER.debug("Unable to reflect Mekanism JEI ingredient types; chemical slots will fall back to eggs.", ex);
            return false;
        }
    }

    public static boolean isAvailable() {
        return AVAILABLE;
    }

    @Nullable
    public static JeiChemicalStack createStack(ChemicalEggRegistryItem entry, long amount) {
        if (!AVAILABLE || entry == null || amount <= 0) {
            return null;
        }
        ResourceLocation chemicalId = entry.getChemicalId();
        Object chemical = MekanismChemicalHelper.getChemical(chemicalId);
        if (chemical == null) {
            return null;
        }
        Object stack = MekanismChemicalHelper.createStack(chemical, amount);
        if (MekanismChemicalHelper.isStackEmpty(stack)) {
            return null;
        }
        IIngredientType<Object> ingredientType = resolveIngredientType(stack);
        if (ingredientType == null) {
            return null;
        }
        return new JeiChemicalStack(ingredientType, stack);
    }

    @Nullable
    private static IIngredientType<Object> resolveIngredientType(Object stack) {
        if (stack == null) {
            return null;
        }
        for (Entry entry : ENTRIES) {
            if (entry.stackClass().isInstance(stack)) {
                return entry.type();
            }
        }
        return null;
    }

    public record JeiChemicalStack(IIngredientType<Object> type, Object stack) {
    }

    private record Entry(Class<?> stackClass, IIngredientType<Object> type) {
    }
}
