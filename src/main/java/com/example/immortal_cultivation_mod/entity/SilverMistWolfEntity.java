package com.example.immortal_cultivation_mod.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Mortal wolf species. All pack tactics are shared with the other Wolf variants. */
public final class SilverMistWolfEntity extends Wolf implements GeoEntity {
    private static final RawAnimation IDLE=clip("idle",true), WALK=clip("walk",true), RUN=clip("run",true);
    private static final RawAnimation BITE=clip("bite",false), HOWL=clip("howl",false);
    private static final RawAnimation DEATH=RawAnimation.begin().thenPlayAndHold("animation.silver_mist_wolf.death");
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    private int biteTicks, howlTicks, leapTicks;

    public SilverMistWolfEntity(EntityType<? extends SilverMistWolfEntity> type, Level level) { super(type,level); }
    public static AttributeSupplier.Builder createAttributes() {
        return Wolf.createAttributes().add(Attributes.MAX_HEALTH,20).add(Attributes.ATTACK_DAMAGE,4)
                .add(Attributes.MOVEMENT_SPEED,.3).add(Attributes.FOLLOW_RANGE,24);
    }
    private static RawAnimation clip(String name,boolean loop) {
        return loop?RawAnimation.begin().thenLoop("animation.silver_mist_wolf."+name)
                :RawAnimation.begin().thenPlay("animation.silver_mist_wolf."+name);
    }
    public void packHowl(){if(!level().isClientSide)level().broadcastEntityEvent(this,(byte)71);}
    public void packPounce(){if(!level().isClientSide)level().broadcastEntityEvent(this,(byte)72);}
    @Override public boolean doHurtTarget(Entity target) {
        boolean hit=super.doHurtTarget(target);
        if(hit&&!level().isClientSide)level().broadcastEntityEvent(this,(byte)70);
        return hit;
    }
    @Override public void handleEntityEvent(byte event) {
        if(event==70){biteTicks=16;howlTicks=0;}
        else if(event==71)howlTicks=60;
        else if(event==72){leapTicks=12;howlTicks=0;}
        else super.handleEntityEvent(event);
    }
    @Override public void tick() {
        super.tick();
        if(biteTicks>0)biteTicks--;if(howlTicks>0)howlTicks--;if(leapTicks>0)leapTicks--;
    }
    @Override public SilverMistWolfEntity getBreedOffspring(ServerLevel level, AgeableMob partner) {
        var pup=ModEntities.SILVER_MIST_WOLF.get().create(level);
        if(pup!=null&&isTame()){pup.setOwnerUUID(getOwnerUUID());pup.setTame(true,true);}
        return pup;
    }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this,"main",3,state->{
            if(isDeadOrDying())return state.setAndContinue(DEATH);
            if(biteTicks>0)return state.setAndContinue(BITE);
            if(leapTicks>0)return state.setAndContinue(RUN);
            if(howlTicks>0&&!state.isMoving())return state.setAndContinue(HOWL);
            if(state.isMoving())return state.setAndContinue(getTarget()!=null?RUN:WALK);
            return state.setAndContinue(IDLE);
        }));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
}
