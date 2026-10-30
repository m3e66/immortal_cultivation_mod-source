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
public final class SilverMistWolfEntity extends Wolf implements GeoEntity, WildlifeAnimal {
    private static final RawAnimation IDLE=clip("idle",true), WALK=clip("walk",true), RUN=clip("run",true);
    private static final RawAnimation BITE=clip("bite",false), HOWL=clip("howl",false);
    private static final RawAnimation DEATH=RawAnimation.begin().thenPlayAndHold("animation.silver_mist_wolf.death");
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    private int biteTicks, howlTicks, leapTicks;
    private WildlifeBrain brain;
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> ACTIVITY=net.minecraft.network.syncher.SynchedEntityData.defineId(SilverMistWolfEntity.class,net.minecraft.network.syncher.EntityDataSerializers.INT);
    @Override protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(ACTIVITY,0);}
    @Override public WildlifeBrain wildlife(){if(brain==null)brain=new WildlifeBrain(this,WildlifeBrain.Species.WOLF);return brain;}
    @Override public int activity(){return entityData.get(ACTIVITY);}
    @Override public void activity(int value){entityData.set(ACTIVITY,value);}
    @Override protected void registerGoals(){
        super.registerGoals();
        targetSelector.removeAllGoals(g->g instanceof net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<?>);
        goalSelector.addGoal(1,wildlife().goal());
    }

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
        else if(event==71)howlTicks=72;
        else if(event==72){leapTicks=12;howlTicks=0;}
        else super.handleEntityEvent(event);
    }
    @Override public void tick() {
        super.tick();wildlife().tick();
        if(biteTicks>0)biteTicks--;if(howlTicks>0)howlTicks--;if(leapTicks>0)leapTicks--;
    }
    @Override protected void tickDeath() {
        ++deathTime;
        if(deathTime>=41&&!level().isClientSide&&!isRemoved()) {
            level().broadcastEntityEvent(this,(byte)60);remove(RemovalReason.KILLED);
        }
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
            if((howlTicks>0||activity()==WildlifeBrain.HOWL)&&!state.isMoving())return state.setAndContinue(HOWL);
            if(state.isMoving())return state.setAndContinue(activity()==WildlifeBrain.DEFEND||activity()==WildlifeBrain.FLEE?RUN:WALK);
            if(activity()==WildlifeBrain.REST)return state.setAndContinue(clip("rest",true));
            if(activity()==WildlifeBrain.ALERT)return state.setAndContinue(clip("sniff",true));
            return state.setAndContinue(IDLE);
        }));
    }
    @Override public int getMaxFallDistance(){return 2;}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
}
