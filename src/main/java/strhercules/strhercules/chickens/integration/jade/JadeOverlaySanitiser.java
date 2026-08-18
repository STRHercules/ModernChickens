package strhercules.chickens.integration.jade;

import snownee.jade.api.Identifiers;
import snownee.jade.api.callback.JadeTooltipCollectedCallback;

/**
 * Strips Jade's built-in universal bars after all providers have run so the
 * custom Chickens HUD bars remain the sole fluid/energy display.
 */
final class JadeOverlaySanitiser implements JadeTooltipCollectedCallback {
    static final JadeOverlaySanitiser INSTANCE = new JadeOverlaySanitiser();

    private JadeOverlaySanitiser() {
    }

    @Override
    public void onTooltipCollected(snownee.jade.api.ITooltip rootElement, snownee.jade.api.Accessor<?> accessor) {
        // Remove universal FE/energy lines
        purge(rootElement, Identifiers.UNIVERSAL_ENERGY_STORAGE);
        purge(rootElement, Identifiers.UNIVERSAL_ENERGY_STORAGE_DETAILED);
        // Remove universal fluid lines
        purge(rootElement, Identifiers.UNIVERSAL_FLUID_STORAGE);
        purge(rootElement, Identifiers.UNIVERSAL_FLUID_STORAGE_DETAILED);
    }

    private static void purge(snownee.jade.api.ITooltip root, net.minecraft.resources.ResourceLocation tag) {
        root.remove(tag);
    }
}
