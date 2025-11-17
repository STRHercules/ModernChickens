package com.setycz.chickens.integration.jade;

import com.setycz.chickens.ChickensMod;
import com.setycz.chickens.blockentity.HenhouseBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

/**
 * Reports the henhouse fuel buffer and queued hay bales.
 */
enum HenhouseDataProvider implements IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ChickensMod.MOD_ID, "henhouse");

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof HenhouseBlockEntity henhouse)) {
            return;
        }
        HudData.Builder builder = HudData.builder();
        builder.addEnergy(henhouse.getEnergy(), HenhouseBlockEntity.HAY_BALE_ENERGY * HenhouseBlockEntity.SLOT_COUNT);
        ItemStack hayStack = henhouse.getItem(HenhouseBlockEntity.HAY_SLOT);
        int hayCount = isHayFuel(hayStack) ? hayStack.getCount() : 0;
        if (hayCount > 0) {
            builder.addText(Component.translatable("tooltip.chickens.henhouse.hay", hayCount));
        }
        HudData.write(data, builder.build());
    }

    @Override
    public ResourceLocation getUid() {
        return ID;
    }

    private static boolean isHayFuel(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.is(Blocks.HAY_BLOCK.asItem()) || stack.is(Tags.Items.STORAGE_BLOCKS_WHEAT));
    }
}
