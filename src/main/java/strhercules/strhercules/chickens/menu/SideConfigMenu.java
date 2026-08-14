package strhercules.chickens.menu;

import strhercules.chickens.blockentity.SideConfigurable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Menu contract used by the shared machine I/O window. */
public interface SideConfigMenu {
    SideConfigurable getSideConfigurable();

    boolean stillValid(Player player);

    default BlockPos getSideConfigPos() {
        if (getSideConfigurable() instanceof BlockEntity blockEntity) {
            return blockEntity.getBlockPos();
        }
        throw new IllegalStateException("Side-configurable menu is not backed by a block entity");
    }
}
