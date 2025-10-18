package com.setycz.chickens.entity;

import com.setycz.chickens.entity.ChickensChicken;
import com.setycz.chickens.item.ChickenItemHelper;
import com.setycz.chickens.registry.ModEntityTypes;
import com.setycz.chickens.registry.ModRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * Variant of the vanilla thrown egg that spawns the chicken type encoded in the
 * projectile's item stack. It preserves the legacy behaviour of occasionally
 * producing multiple baby chickens while adapting to the SynchedEntityData
 * framework used by newer Minecraft versions.
 */
public class ColoredEgg extends ThrownEgg {
    /**
     * Remember the encoded chicken id separately from the projectile's item
     * stack. The item stack already serialises the data for rendering, but the
     * cached copy lets the server resolve the chicken quickly even if the
     * stack is temporarily empty (e.g. after mods mutate the watched item).
     */
    private int chickenType = -1;

    public ColoredEgg(EntityType<? extends ColoredEgg> type, Level level) {
        super(type, level);
    }

    public ColoredEgg(Level level, LivingEntity owner) {
        super(level, owner);
    }

    public ColoredEgg(Level level, double x, double y, double z) {
        super(level, x, y, z);
    }

    public void setChickenType(int type) {
        this.chickenType = type;
        // Keep the thrown egg's item stack in sync so the client renders the
        // right tint and the information survives entity save/load cycles.
        ItemStack stack = this.getItem();
        if (!stack.isEmpty()) {
            ItemStack copy = stack.copy();
            ChickenItemHelper.setChickenType(copy, type);
            this.setItem(copy);
        }
    }

    public int getChickenType() {
        if (this.chickenType == -1) {
            this.chickenType = ChickenItemHelper.getChickenType(this.getItem());
        }
        return this.chickenType;
    }

    @Override
    public void setItem(ItemStack stack) {
        super.setItem(stack);
        // When the watched stack changes (e.g. after syncing from the server)
        // refresh the cached chicken id so both sides stay in agreement.
        if (!stack.isEmpty()) {
            this.chickenType = ChickenItemHelper.getChickenType(stack);
        }
    }

    @Override
    protected Item getDefaultItem() {
        return ModRegistry.COLORED_EGG.get();
    }

    @Override
    protected void onHitEntity(net.minecraft.world.phys.EntityHitResult result) {
        result.getEntity().hurt(this.damageSources().thrown(this, this.getOwner()), 0.0F);
    }

    @Override
    protected void onHit(net.minecraft.world.phys.HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            if (this.random.nextInt(8) == 0) {
                int count = 1;
                if (this.random.nextInt(32) == 0) {
                    count = 4;
                }
                for (int i = 0; i < count; ++i) {
                    ChickensChicken chick = ModEntityTypes.CHICKENS_CHICKEN.get().create(this.level());
                    if (chick != null) {
                        chick.setAge(-24000);
                        chick.setChickenType(getChickenType());
                        chick.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
                        this.level().addFreshEntity(chick);
                    }
                }
            }
            this.level().broadcastEntityEvent(this, (byte) 3);
            this.level().gameEvent(GameEvent.PROJECTILE_LAND, this.position(), GameEvent.Context.of(this));
            this.discard();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(ChickenItemHelper.TAG_CHICKEN_TYPE, getChickenType());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(ChickenItemHelper.TAG_CHICKEN_TYPE)) {
            setChickenType(tag.getInt(ChickenItemHelper.TAG_CHICKEN_TYPE));
        }
    }
}
