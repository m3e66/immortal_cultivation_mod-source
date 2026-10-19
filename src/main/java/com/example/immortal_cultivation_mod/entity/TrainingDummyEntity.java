package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.attachment.*;
import com.example.immortal_cultivation_mod.animation.SpiritPuppetAnimations;
import com.example.immortal_cultivation_mod.combat.*;
import com.example.immortal_cultivation_mod.spell.*;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Persistent, owner-controlled cultivator mannequin. No random knowledge, loot or autonomous targets. */
public final class TrainingDummyEntity extends PathfinderMob implements CultivationNpc, software.bernie.geckolib.animatable.GeoEntity {
    private final software.bernie.geckolib.animatable.instance.AnimatableInstanceCache animationCache =
            software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);
    private final SpiritPuppetAnimations puppetAnimations=new SpiritPuppetAnimations();
    private static final EntityDataAccessor<Integer> CAST=SynchedEntityData.defineId(TrainingDummyEntity.class,EntityDataSerializers.INT);
    public UUID controller;
    public boolean domainRequested,combatEnabled,mobile,immortal=true,unlimitedQi=true;
    public float magicAttack=12;
    private String pendingSpell="";
    private UUID pendingTarget,savedTarget;
    private long castAt;
    private int rotation;
    private Vec3 commandedDestination;
    private final Map<String,Long> cooldowns=new HashMap<>();
    public TrainingDummyEntity(EntityType<? extends TrainingDummyEntity> type,Level level) {
        super(type,level);setPersistenceRequired();xpReward=0;
        for(var slot:EquipmentSlot.values())setDropChance(slot,0);
    }
    public static AttributeSupplier.Builder createAttributes(){return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH,1500).add(Attributes.ATTACK_DAMAGE,12).add(Attributes.MOVEMENT_SPEED,.3)
            .add(Attributes.ARMOR,0).add(Attributes.FOLLOW_RANGE,64).add(Attributes.KNOCKBACK_RESISTANCE,1);}
    @Override protected void registerGoals() { }
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(CAST,0);}
    @Override public String npcArchetype(){return NpcProfile.JINDAN;}
    @Override public String getNpcSkinId(){return "";}
    @Override public void setNpcSkinId(String skin) { }
    @Override public void beginNpcCast(int ticks){entityData.set(CAST,Math.max(1,ticks));}
    @Override public void endNpcCast(){entityData.set(CAST,0);}
    @Override public int npcCastingTicks(){return entityData.get(CAST);}
    @Override public boolean removeWhenFarAway(double distance){return false;}
    public CompoundTag profile(){ensureProfile();return getPersistentData().getCompound("ImmortalCultivationNpc");}
    public static ListTag strings(Collection<String> values){var list=new ListTag();for(String value:values)list.add(StringTag.valueOf(value));return list;}
    public static List<String> strings(CompoundTag p,String key){return p.getList(key,Tag.TAG_STRING).stream().map(Tag::getAsString).toList();}
    public void ensureProfile() {
        var data=getPersistentData();if(data.getCompound("ImmortalCultivationNpc").getBoolean("Initialized"))return;
        var p=new CompoundTag();p.putBoolean("Initialized",true);p.putString("Name","Training Dummy");
        p.putString("Realm",CultivationLevels.REALM_JINDAN+CultivationLevels.STAGE_EARLY);
        p.putString("Method",CultivationMethods.BASIC_BREATHING);p.put("Methods",strings(List.of(CultivationMethods.BASIC_BREATHING)));
        var proficiency=new CompoundTag();proficiency.putInt(CultivationMethods.BASIC_BREATHING,500);p.put("MethodProficiencies",proficiency);
        p.put("Spells",new ListTag());p.put("SpiritRoots",strings(List.of(SpiritRoots.METAL,SpiritRoots.WOOD,SpiritRoots.WATER,SpiritRoots.FIRE,SpiritRoots.EARTH,
                SpiritRoots.WIND,SpiritRoots.THUNDER,SpiritRoots.ICE,SpiritRoots.YIN,SpiritRoots.YANG)));
        p.putString("Personality","aggressive");p.putString("Identity","training_dummy");p.putInt("MaxQi",100000);p.putInt("Qi",100000);p.putInt("Thoughts",ModAttachments.MAX_THOUGHTS);
        data.put("ImmortalCultivationNpc",p);
    }
    public void setDomainRequested(boolean enabled) {
        domainRequested=enabled;
        if(!enabled)QiDomainSystem.stop(this);
    }
    public void halt(){combatEnabled=false;pendingSpell="";endNpcCast();getNavigation().stop();setTarget(null);savedTarget=null;commandedDestination=null;setDeltaMovement(Vec3.ZERO);}
    public void moveToPoint(Vec3 destination) {
        halt();mobile=true;commandedDestination=destination;
        if(!NpcSpellcasting.hasCreativeFlight(this))getNavigation().moveTo(destination.x,destination.y,destination.z,1);
    }
    private void tickFlightMotion() {
        if(!mobile)commandedDestination=null;
        if(commandedDestination!=null&&position().distanceToSqr(commandedDestination)<.25*.25) {
            commandedDestination=null;getNavigation().stop();
        }
        Vec3 destination=mobile?commandedDestination:null;
        if(destination==null&&mobile&&combatEnabled&&getTarget()!=null) {
            Vec3 offset=getTarget().position().subtract(position());double distance=offset.length();
            if(distance>1.7)destination=getTarget().position().subtract(offset.scale(1.5/distance));
        }
        boolean groundClose=onGround()||!level().noCollision(this,getBoundingBox().move(0,-.12,0));
        boolean wantsFlight=!groundClose||destination!=null&&Math.abs(destination.y-getY())>.75;
        if(!NpcSpellcasting.hasCreativeFlight(this)||restricted()||isPassenger()||isInWaterOrBubble()||isInLava()
                ||!wantsFlight||!unlimitedQi&&!NpcFlight.payUpkeep(this)) {
            setNoGravity(false);
            if(mobile&&commandedDestination!=null&&!restricted())
                getNavigation().moveTo(commandedDestination.x,commandedDestination.y,commandedDestination.z,1);
            return;
        }
        getNavigation().stop();setSpeed(0);
        NpcSpellcasting.tickCreativeFlight(this,destination);
    }
    private boolean restricted(){return ColdTideFreeze.isFrozen(this)||hasEffect(com.example.immortal_cultivation_mod.effect.ModEffects.DAZE)
            ||hasEffect(com.example.immortal_cultivation_mod.effect.ModEffects.DINGSHEN)||hasEffect(com.example.immortal_cultivation_mod.effect.ModEffects.FROZEN)
            ||hasEffect(com.example.immortal_cultivation_mod.effect.ModEffects.FROZEN_QI);}
    public boolean cast(String id) {
        id=ModSpells.normalizeId(id);
        if(!pendingSpell.isEmpty()||level().isClientSide||ColdTideFreeze.isFrozen(this)||level().getGameTime()<cooldowns.getOrDefault(id,0L)
                ||!NpcSpellcasting.canCast(this,getTarget(),id)||getTarget()!=null&&!QiDomainSystem.canAffect(this,getTarget()))return false;
        pendingSpell=id;pendingTarget=getTarget()==null?null:getTarget().getUUID();
        int windup=NpcSpellcasting.windupTicks(id);castAt=level().getGameTime()+windup;beginNpcCast(windup);return true;
    }
    /** Clear target-owned afflictions without healing, refilling Qi or resetting practice settings. */
    public void clearDebuffs() {
        if(level().isClientSide)return;
        com.example.immortal_cultivation_mod.item.FuchenItem.releaseBoundTarget(this);
        com.example.immortal_cultivation_mod.item.HigherWeaponCombat.clearControl(this);
        QiDomainSystem.clearTargetDebuffs(this);
        com.example.immortal_cultivation_mod.curse.AnnihilationCurse.clearDebuffs(this);
        for(var effect:new ArrayList<>(getActiveEffects())) {
            if(effect.getEffect().value().getCategory()==net.minecraft.world.effect.MobEffectCategory.HARMFUL
                    ||effect.getEffect().is(net.minecraft.world.effect.MobEffects.GLOWING)
                    ||effect.getEffect().is(com.example.immortal_cultivation_mod.effect.ModEffects.DAMP))
                removeEffect(effect.getEffect());
        }
        clearFire();setTicksFrozen(0);
    }
    public void resetVitals(){pendingSpell="";endNpcCast();cooldowns.clear();clearDebuffs();removeAllEffects();
        getPersistentData().remove("ImmortalCultivationDomainReadyAt");setHealth(getMaxHealth());profile().putInt("Qi",profile().getInt("MaxQi"));clearFire();}
    @Override public void aiStep() {
        super.aiStep();
        // PathfinderMob does not advance humanoid swings; keep both client and server swing clocks running.
        updateSwingTime();
    }
    @Override public void tick() {
        super.tick();if(level().isClientSide)return;
        var p=profile();if(unlimitedQi)p.putInt("Qi",p.getInt("MaxQi"));
        if(npcCastingTicks()>0)entityData.set(CAST,npcCastingTicks()-1);
        if(getTarget()==null&&savedTarget!=null&&level() instanceof ServerLevel server&&server.getEntity(savedTarget) instanceof LivingEntity target){savedTarget=null;if(target.isAlive())setTarget(target);}
        if(getTarget()!=null&&(!getTarget().isAlive()||getTarget().level()!=level()||!QiDomainSystem.canAffect(this,getTarget())))setTarget(null);
        DielangShield.tick(this);
        if(!pendingSpell.isEmpty()&&level().getGameTime()>=castAt) {
            String spell=pendingSpell;pendingSpell="";endNpcCast();
            LivingEntity target=pendingTarget==null?null:level() instanceof ServerLevel s&&s.getEntity(pendingTarget) instanceof LivingEntity e?e:null;
            if(target!=null&&target!=getTarget())return;
            if(!ColdTideFreeze.isFrozen(this)&&NpcSpellcasting.cast(this,target,spell))cooldowns.put(spell,level().getGameTime()+Math.max(1,ModSpells.cooldownTicks(spell)));
        }
        if(combatEnabled&&getTarget()!=null&&!restricted()) {
            getLookControl().setLookAt(getTarget(),30,30);
            if(mobile&&distanceToSqr(getTarget())>6)getNavigation().moveTo(getTarget(),1);
            if(tickCount%8==0&&pendingSpell.isEmpty()) {
                var spells=strings(p,"Spells");
                for(int i=0;i<spells.size();i++){String spell=spells.get(Math.floorMod(rotation++,spells.size()));if(cast(spell))break;}
            }
            if(pendingSpell.isEmpty()&&tickCount%20==0&&distanceToSqr(getTarget())<6&&hasLineOfSight(getTarget())){swing(InteractionHand.MAIN_HAND);doHurtTarget(getTarget());}
        }
        tickFlightMotion();
        if(restricted())getNavigation().stop();
        if(!mobile){getNavigation().stop();setDeltaMovement(0,getDeltaMovement().y,0);}
    }
    @Override public void travel(Vec3 input){
        super.travel(isNoGravity()&&NpcSpellcasting.hasCreativeFlight(this)?Vec3.ZERO:mobile?input:new Vec3(0,input.y,0));
    }
    @Override public void knockback(double strength,double x,double z){if(mobile)super.knockback(strength,x,z);}
    @Override public boolean hurt(DamageSource source,float amount) {
        if(immortal&&!source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY))amount=Math.min(amount,Math.max(0,getHealth()-1));
        return amount>0&&super.hurt(source,amount);
    }
    @Override protected InteractionResult mobInteract(Player player,InteractionHand hand) {
        if(!level().isClientSide)player.displayClientMessage(Component.translatable("training_dummy.immortal_cultivation_mod.use_stick"),true);
        return InteractionResult.sidedSuccess(level().isClientSide);
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);NpcProfile.save(this,tag);
        if(controller!=null)tag.putUUID("DummyController",controller);
        tag.putBoolean("DummyDomain",domainRequested);tag.putBoolean("DummyCombat",combatEnabled);tag.putBoolean("DummyMobile",mobile);
        tag.putBoolean("DummyImmortal",immortal);tag.putBoolean("DummyUnlimitedQi",unlimitedQi);tag.putFloat("DummyMagicAttack",magicAttack);
        if(getTarget()!=null)tag.putUUID("DummyTarget",getTarget().getUUID());
        var saved=new CompoundTag();cooldowns.forEach(saved::putLong);tag.put("DummyCooldowns",saved);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);NpcProfile.load(this,tag);ensureProfile();
        controller=tag.hasUUID("DummyController")?tag.getUUID("DummyController"):null;
        domainRequested=tag.getBoolean("DummyDomain");combatEnabled=tag.getBoolean("DummyCombat");mobile=tag.getBoolean("DummyMobile");
        immortal=!tag.contains("DummyImmortal")||tag.getBoolean("DummyImmortal");unlimitedQi=!tag.contains("DummyUnlimitedQi")||tag.getBoolean("DummyUnlimitedQi");
        magicAttack=Math.clamp(tag.getFloat("DummyMagicAttack"),0,1000000);savedTarget=tag.hasUUID("DummyTarget")?tag.getUUID("DummyTarget"):null;
        cooldowns.clear();var saved=tag.getCompound("DummyCooldowns");for(String id:saved.getAllKeys())if(ModSpells.get(id)!=null)cooldowns.put(id,saved.getLong(id));
    }
    @Override public void remove(RemovalReason reason){if(!level().isClientSide){QiDomainSystem.stop(this);DielangShield.clear(this);}super.remove(reason);}
    @Override public software.bernie.geckolib.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache(){return animationCache;}
    public SpiritPuppetAnimations.Clip puppetAnimation(){return puppetAnimations.current();}
    @Override public void registerControllers(software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new software.bernie.geckolib.animation.AnimationController<>(this,"puppet",2,state -> {
            double dx=getX()-xo,dz=getZ()-zo,yaw=Math.toRadians(yBodyRot);
            boolean left=getMainArm()==HumanoidArm.LEFT;
            if(swingingArm==InteractionHand.OFF_HAND)left=!left;
            var selected=puppetAnimations.update(new SpiritPuppetAnimations.Snapshot(
                    (tickCount+state.getPartialTick())/20.0,isAlive(),onGround(),isNoGravity(),isInWaterOrBubble(),
                    getDeltaMovement().y,-dx*Math.sin(yaw)+dz*Math.cos(yaw),-dx*Math.cos(yaw)-dz*Math.sin(yaw),
                    isSprinting(),hurtTime,getAttackAnim(state.getPartialTick()),left,npcCastingTicks()));
            if(selected.restart())state.getController().forceAnimationReset();
            state.getController().transitionLength(selected.clip()==SpiritPuppetAnimations.Clip.HURT?1:2);
            state.getController().setAnimationSpeed(selected.speed());
            return state.setAndContinue(selected.clip().animation);
        }));
    }
}
