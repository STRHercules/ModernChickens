package strhercules.chickens;

import strhercules.chickens.entity.MegaChickenSkin;
import strhercules.chickens.item.MegaChickenSkinCrateItem;
import strhercules.chickens.registry.ModRegistry;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDropsEvent;

/** Adds the rare supplied-skin crates without replacing vanilla boss loot tables. */
public final class MegaChickenLoot {
    private static final float DROP_CHANCE = 0.01F;

    private MegaChickenLoot() {
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(MegaChickenLoot::onLivingDrops);
    }

    private static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (!isEndGameTarget(entity.getType()) || entity.getRandom().nextFloat() >= DROP_CHANCE) {
            return;
        }
        MegaChickenSkin skin = MegaChickenSkin.values()[entity.getRandom().nextInt(MegaChickenSkin.values().length)];
        ItemStack crate = new ItemStack(ModRegistry.skinCrate(skin).get());
        event.getDrops().add(new ItemEntity(entity.level(), entity.getX(), entity.getY() + 0.2D,
                entity.getZ(), crate));
    }

    private static boolean isEndGameTarget(EntityType<?> type) {
        return type == EntityType.ENDER_DRAGON || type == EntityType.WITHER
                || type == EntityType.WARDEN || type == EntityType.ELDER_GUARDIAN;
    }
}
