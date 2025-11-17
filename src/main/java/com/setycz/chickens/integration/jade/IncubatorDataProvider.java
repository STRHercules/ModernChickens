package com.setycz.chickens.integration.jade;

import com.setycz.chickens.ChickensMod;
import com.setycz.chickens.blockentity.IncubatorBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

/**
 * Streams the incubator RF buffer and status to Jade.
 */
enum IncubatorDataProvider implements IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ChickensMod.MOD_ID, "incubator");

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof IncubatorBlockEntity incubator)) {
            return;
        }
        HudData.Builder builder = HudData.builder();
        builder.addEnergy(incubator.getEnergyStored(), incubator.getEnergyCapacity());
        int maxProgress = Math.max(incubator.getMaxProgress(), 1);
        int percent = Math.max(0, incubator.getProgress()) * 100 / maxProgress;
        builder.addText(Component.translatable("tooltip.chickens.incubator.progress", percent));
        builder.addText(Component.translatable("tooltip.chickens.incubator.cost", incubator.getEnergyCost()));
        HudData.write(data, builder.build());
    }

    @Override
    public ResourceLocation getUid() {
        return ID;
    }
}
