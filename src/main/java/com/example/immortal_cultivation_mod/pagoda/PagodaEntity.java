package com.example.immortal_cultivation_mod.pagoda;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class PagodaEntity extends PathfinderMob implements GeoEntity {
    private static final EntityDataAccessor<Boolean> GOURD=SynchedEntityData.defineId(PagodaEntity.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> CORE=SynchedEntityData.defineId(PagodaEntity.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> PROGRESS=SynchedEntityData.defineId(PagodaEntity.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SIZE=SynchedEntityData.defineId(PagodaEntity.class,EntityDataSerializers.FLOAT);
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    public UUID towerId;
    public PagodaEntity(EntityType<? extends PathfinderMob> type,Level level){super(type,level);setNoGravity(true);setPersistenceRequired();}
    public static AttributeSupplier.Builder attributes(){return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,500).add(Attributes.MOVEMENT_SPEED,0).add(Attributes.KNOCKBACK_RESISTANCE,1);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(GOURD,false);b.define(CORE,false);b.define(PROGRESS,0F);b.define(SIZE,1F);}
    public boolean gourd(){return entityData.get(GOURD);}
    public void setGourd(boolean value){entityData.set(GOURD,value);refreshDimensions();}
    public void setCore(boolean value){entityData.set(CORE,value);refreshDimensions();}
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key){super.onSyncedDataUpdated(key);if(CORE.equals(key)||GOURD.equals(key))refreshDimensions();}
    public boolean core(){return entityData.get(CORE);}
    public float breakout(){return entityData.get(PROGRESS);}
    public float visualScale(){return entityData.get(SIZE);}
    public void updateVisual(double progress,double scale){entityData.set(PROGRESS,(float)progress);entityData.set(SIZE,(float)scale);}
    @Override public EntityDimensions getDefaultDimensions(Pose pose){return core()?EntityDimensions.scalable(1.2F,1.8F):EntityDimensions.scalable(gourd()?3:6,gourd()?6:14);}
    @Override public boolean isPushable(){return false;}
    @Override public boolean hurt(DamageSource source,float amount){
        if(source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY))return super.hurt(source,amount);
        if(level().isClientSide||towerId==null)return false;
        return PagodaSystem.damage(this,source,amount);
    }
    @Override public void tick(){
        super.tick();setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        if(level().isClientSide)PagodaClientAccess.tick(this);
        else if(towerId!=null&&level() instanceof net.minecraft.server.level.ServerLevel l&&PagodaSystem.towerFor(l.getServer(),towerId)==null)discard();
    }
    @Override public void addAdditionalSaveData(CompoundTag t){super.addAdditionalSaveData(t);if(towerId!=null)t.putUUID("Pagoda",towerId);t.putBoolean("Core",core());t.putBoolean("Gourd",gourd());}
    @Override public void readAdditionalSaveData(CompoundTag t){super.readAdditionalSaveData(t);if(t.hasUUID("Pagoda"))towerId=t.getUUID("Pagoda");setCore(t.getBoolean("Core"));setGourd(t.getBoolean("Gourd"));}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c){c.add(new software.bernie.geckolib.animation.AnimationController<>(this,"gourd",0,state->{return state.setAndContinue(software.bernie.geckolib.animation.RawAnimation.begin().thenLoop(gourd()&&!core()?"capture_hover":"idle"));}));}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
}
