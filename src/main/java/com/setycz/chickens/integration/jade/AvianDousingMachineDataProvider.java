package com.setycz.chickens.integration.jade;

import com.setycz.chickens.ChickensMod;
import com.setycz.chickens.blockentity.AvianDousingMachineBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

/**
 * Streams the dousing machine's buffers and progress to Jade.
 */
enum AvianDousingMachineDataProvider implements IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ChickensMod.MOD_ID, "avian_dousing_machine");

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof AvianDousingMachineBlockEntity machine)) {
            return;
        }
        HudData.Builder builder = HudData.builder();
        builder.addFluid(machine.getFluid().copy(), machine.getLiquidCapacity());
        builder.addEnergy(machine.getEnergyStored(), machine.getEnergyCapacity());
        int maxProgress = Math.max(machine.getMaxProgress(), 1);
        int percent = Math.max(machine.getProgress(), 0) * 100 / maxProgress;
        builder.addText(Component.translatable("tooltip.chickens.avian_dousing_machine.progress", percent));
        HudData.write(data, builder.build());
    }

    @Override
    public ResourceLocation getUid() {
        return ID;
    }
}
