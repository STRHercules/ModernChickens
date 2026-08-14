package strhercules.chickens.integration.jade;

import strhercules.chickens.ChickensMod;
import strhercules.chickens.entity.Rooster;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.Locale;

enum RoosterProvider implements IEntityComponentProvider {
    INSTANCE;

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(
            ChickensMod.MOD_ID, "rooster");

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        if (!(accessor.getEntity() instanceof Rooster rooster)) {
            return;
        }
        int ticks = rooster.getVirusTicksRemaining();
        if (ticks <= 0 || rooster.isRobotRooster()) {
            return;
        }
        int seconds = (ticks + 19) / 20;
        String countdown = String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
        tooltip.add(Component.translatable("entity.chickens.robot_rooster.virus", countdown));
    }

    @Override
    public ResourceLocation getUid() {
        return ID;
    }
}
