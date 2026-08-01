package strhercules.chickens;

import strhercules.chickens.entity.ChickensChicken;
import strhercules.chickens.registry.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import javax.annotation.Nullable;

/**
 * Listens for players teaching vanilla chickens using a trigger item.
 * Each item maps to a target breed; the vanilla chicken is replaced with
 * the corresponding ChickensChicken entity and the item is consumed.
 */
public final class ChickenTeachHandler {
    private ChickenTeachHandler() {
    }

    public static void init() {
        NeoForge.EVENT_BUS.addListener(ChickenTeachHandler::onEntityInteract);
    }

    private static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        ItemStack stack = event.getItemStack();
        ChickensRegistryItem targetBreed = resolveTeachingTarget(stack);
        if (targetBreed == null) {
            return;
        }
        Player player = event.getEntity();
        if (!(event.getTarget() instanceof Chicken chicken) || player == null || chicken.getType() != EntityType.CHICKEN) {
            return;
        }
        Level level = event.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!targetBreed.isEnabled()) {
            return;
        }
        BlockPos blockPos = chicken.blockPosition();
        ChickensChicken newChicken = ModEntityTypes.CHICKENS_CHICKEN.get().create(serverLevel);
        if (newChicken == null) {
            return;
        }
        newChicken.moveTo(chicken.getX(), chicken.getY(), chicken.getZ(), chicken.getYRot(), chicken.getXRot());
        newChicken.setYHeadRot(chicken.getYHeadRot());
        newChicken.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(blockPos), MobSpawnType.CONVERSION, null);
        newChicken.setChickenType(targetBreed.getId());
        newChicken.setAge(chicken.getAge());
        if (chicken.hasCustomName()) {
            newChicken.setCustomName(chicken.getCustomName());
            newChicken.setCustomNameVisible(chicken.isCustomNameVisible());
        }
        serverLevel.addFreshEntity(newChicken);
        newChicken.spawnAnim();
        chicken.discard();
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    /**
     * Maps a held ItemStack to the chicken breed it should teach on a vanilla chicken.
     * Returns {@code null} if the item is not a teaching trigger.
     */
    @Nullable
    private static ChickensRegistryItem resolveTeachingTarget(ItemStack held) {
        if (held.isEmpty()) {
            return null;
        }
        if (held.is(Items.BOOK)) {
            return ChickensRegistry.getSmartChicken();
        }
        if (held.is(Items.CAKE)) {
            return ChickensRegistry.getByEntityName("chickenNosto");
        }
        if (held.is(Blocks.GRASS_BLOCK.asItem())) {
            return ChickensRegistry.getByEntityName("americanChicken");
        }
        if (held.is(Blocks.DIRT.asItem())) {
            return ChickensRegistry.getByEntityName("dirtChicken");
        }
        return null;
    }
}