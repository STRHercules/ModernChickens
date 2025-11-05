package com.modernfluidcows.item;

import com.modernfluidcows.blockentity.FeederBlockEntity;
import com.modernfluidcows.blockentity.SorterBlockEntity;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Utility item that upgrades the work area of the feeder and sorter when right-clicked on them.
 */
public class CowRangerItem extends Item {
    public CowRangerItem(final Properties properties) {
        super(properties.stacksTo(7));
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        Level level = context.getLevel();
        BlockEntity entity = level.getBlockEntity(context.getClickedPos());
        if (entity instanceof FeederBlockEntity feeder) {
            return applyUpgrade(context, new RangerUpgradeable() {
                @Override
                public boolean canAccept() {
                    return feeder.canAcceptRangerUpgrade();
                }

                @Override
                public int addUpgrade() {
                    return feeder.addRangerUpgrade();
                }

                @Override
                public int getUpgradeCount() {
                    return feeder.getRangerUpgradeCount();
                }

                @Override
                public int getMaxUpgrades() {
                    return feeder.getRangerMaxUpgrades();
                }

                @Override
                public int getRange() {
                    return feeder.getRange();
                }
            });
        }
        if (entity instanceof SorterBlockEntity sorter) {
            return applyUpgrade(context, new RangerUpgradeable() {
                @Override
                public boolean canAccept() {
                    return sorter.canAcceptRangerUpgrade();
                }

                @Override
                public int addUpgrade() {
                    return sorter.addRangerUpgrade();
                }

                @Override
                public int getUpgradeCount() {
                    return sorter.getRangerUpgradeCount();
                }

                @Override
                public int getMaxUpgrades() {
                    return sorter.getRangerMaxUpgrades();
                }

                @Override
                public int getRange() {
                    return sorter.getRange();
                }
            });
        }
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(
            final ItemStack stack,
            final Item.TooltipContext context,
            final List<Component> tooltip,
            final TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.fluidcows.cow_ranger.usage")
                .withStyle(style -> style.withItalic(false)));
    }

    private InteractionResult applyUpgrade(final UseOnContext context, final RangerUpgradeable upgradeable) {
        Level level = context.getLevel();
        if (upgradeable == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return upgradeable.canAccept() ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }

        Player player = context.getPlayer();
        if (!upgradeable.canAccept()) {
            sendFullMessage(player, upgradeable.getUpgradeCount(), upgradeable.getMaxUpgrades());
            return InteractionResult.FAIL;
        }

        int newCount = upgradeable.addUpgrade();
        if (player != null && !player.getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        sendAppliedMessage(player, newCount, upgradeable.getMaxUpgrades(), upgradeable.getRange());
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private void sendAppliedMessage(final Player player, final int count, final int max, final int range) {
        if (player != null) {
            player.displayClientMessage(
                    Component.translatable("message.fluidcows.cow_ranger.applied", count, max, range),
                    true);
        }
    }

    private void sendFullMessage(final Player player, final int count, final int max) {
        if (player != null) {
            player.displayClientMessage(
                    Component.translatable("message.fluidcows.cow_ranger.full", count, max),
                    true);
        }
    }

    private interface RangerUpgradeable {
        boolean canAccept();

        int addUpgrade();

        int getUpgradeCount();

        int getMaxUpgrades();

        int getRange();
    }
}
