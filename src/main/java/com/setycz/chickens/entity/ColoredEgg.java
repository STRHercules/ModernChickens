package com.setycz.chickens.entity;

import com.setycz.chickens.entity.ChickensChicken;
import com.setycz.chickens.item.ChickenItemHelper;
import com.setycz.chickens.registry.ModEntityTypes;
import com.setycz.chickens.registry.ModRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * Variant of the vanilla thrown egg that spawns the chicken type encoded in the
 * projectile's item stack. It preserves the legacy behaviour of occasionally
 * producing multiple baby chickens while adapting to the SynchedEntityData
 * framework used by newer Minecraft versions.
 */
public class ColoredEgg extends ThrownEgg {
    private static final EntityDataAccessor<Integer> DATA_CHICKEN_TYPE = SynchedEntityData.defineId(ColoredEgg.class, EntityDataSerializers.INT);

    public ColoredEgg(EntityType<? extends ColoredEgg> type, Level level) {
        super(type, level);
    }

    public ColoredEgg(Level level, LivingEntity owner) {
        super(level, owner);
    }

    public ColoredEgg(Level level, double x, double y, double z) {
        super(level, x, y, z);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_CHICKEN_TYPE, 0);
    }

    public void setChickenType(int type) {
        this.entityData.set(DATA_CHICKEN_TYPE, type);
    }

    public int getChickenType() {
        return this.entityData.get(DATA_CHICKEN_TYPE);
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
