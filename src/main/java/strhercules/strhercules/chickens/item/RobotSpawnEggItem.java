package strhercules.chickens.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeSpawnEggItem;

import java.util.function.Supplier;

/**
 * Spawn egg whose stacks always carry a fixed entity payload, mirroring the
 * default entity data component the mod relied on before the 1.20.1 port.
 */
public class RobotSpawnEggItem extends ForgeSpawnEggItem {
    private final Supplier<CompoundTag> entityDefaults;

    public RobotSpawnEggItem(Supplier<? extends EntityType<? extends Mob>> type,
                             int backgroundColor,
                             int highlightColor,
                             Properties properties,
                             Supplier<CompoundTag> entityDefaults) {
        super(type, backgroundColor, highlightColor, properties);
        this.entityDefaults = entityDefaults;
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        applyDefaults(stack);
        return stack;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        applyDefaults(context.getItemInHand());
        return super.useOn(context);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        applyDefaults(player.getItemInHand(hand));
        return super.use(level, player, hand);
    }

    private void applyDefaults(ItemStack stack) {
        CompoundTag root = stack.getOrCreateTag();
        if (!root.contains(ItemData.ENTITY_TAG)) {
            root.put(ItemData.ENTITY_TAG, entityDefaults.get());
        }
    }
}
