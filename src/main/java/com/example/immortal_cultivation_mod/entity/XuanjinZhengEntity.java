package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.companion.XuanjinCombat;
import com.example.immortal_cultivation_mod.companion.XuanjinCompanion;
import com.example.immortal_cultivation_mod.companion.XuanjinRules;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import java.util.Optional;
import java.util.UUID;
import java.util.LinkedHashSet;

/** A timed projection while sealed; an indiscriminate, persistent hunter after release. */
public final class XuanjinZhengEntity extends PathfinderMob implements GeoEntity {
    private static final EntityDataAccessor<Integer> SEAL=SynchedEntityData.defineId(XuanjinZhengEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> FREE=SynchedEntityData.defineId(XuanjinZhengEntity.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<UUID>> OWNER=SynchedEntityData.defineId(XuanjinZhengEntity.class,EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<String> ACTION=SynchedEntityData.defineId(XuanjinZhengEntity.class,EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> ACTION_SERIAL=SynchedEntityData.defineId(XuanjinZhengEntity.class,EntityDataSerializers.INT);
    private int renderedActionSerial=-1;
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    private CompoundTag wildState=XuanjinCompanion.fresh();
    private int actionTicks, attackTicks, stuckTicks;
    private UUID huntTarget;
    private final LinkedHashSet<UUID> huntQueue = new LinkedHashSet<>();
    private Vec3 lastProgress=Vec3.ZERO;
    public XuanjinZhengEntity(EntityType<? extends PathfinderMob> type,Level level) {
        super(type,level); setPersistenceRequired();
        wildState.putInt("Seal",6); wildState.putInt("Energy",10000); wildState.putFloat("Health",50000);
        getNavigation().setCanFloat(true);
    }
    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH,50000).add(Attributes.ATTACK_DAMAGE,4500)
                .add(Attributes.ARMOR,350).add(Attributes.MOVEMENT_SPEED,.42).add(Attributes.FOLLOW_RANGE,32)
                .add(Attributes.KNOCKBACK_RESISTANCE,.9).add(Attributes.STEP_HEIGHT,1.5);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder); builder.define(SEAL,6); builder.define(FREE,true);
        builder.define(OWNER,Optional.empty()); builder.define(ACTION,"");builder.define(ACTION_SERIAL,0);
    }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(7,new WaterAvoidingRandomStrollGoal(this,.7));
        goalSelector.addGoal(8,new LookAtPlayerGoal(this,Player.class,8));
        goalSelector.addGoal(9,new RandomLookAroundGoal(this));
    }
    public int seal() { return entityData.get(SEAL); }
    public boolean isFree() { return entityData.get(FREE); }
    public UUID ownerId() { return entityData.get(OWNER).orElse(null); }
    public ServerPlayer owner() { return level().getServer()==null || ownerId()==null?null:level().getServer().getPlayerList().getPlayer(ownerId()); }
    public CompoundTag companionState() { var p=owner(); return !isFree() && p!=null?XuanjinCompanion.state(p):wildState; }
    public void bind(ServerPlayer p,int stage) {
        entityData.set(OWNER,Optional.of(p.getUUID())); entityData.set(FREE,false); setSeal(stage);
    }
    public void free(CompoundTag state) {
        wildState=state.copy(); wildState.remove("Body"); wildState.remove("PossessUntil"); wildState.remove("ManifestUntil");
        wildState.putInt("Seal",6);
        entityData.set(FREE,true); entityData.set(OWNER,Optional.empty()); setSeal(6); setTarget(null); huntTarget=null;
        action("roar",30); setPersistenceRequired();
    }
    public void setSeal(int value) {
        entityData.set(SEAL,Math.clamp(value,0,6)); var stage=XuanjinRules.stage(seal());
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(stage.health()); getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(stage.attack());
        getAttribute(Attributes.ARMOR).setBaseValue(stage.armor()); getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(seal()==6?.42:.30);
        refreshDimensions();
    }
    @Override public EntityDimensions getDefaultDimensions(Pose pose) {
        // Minecraft uses square horizontal collision boxes; model length is 6.5 blocks.
        float scale=XuanjinRules.stage(seal()).scale(); return EntityDimensions.scalable(2.2F*scale,3.2F*scale);
    }
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> data) {
        super.onSyncedDataUpdated(data); if(data.equals(SEAL)) refreshDimensions();
    }
    public void action(String name,int ticks) {
        entityData.set(ACTION,name);entityData.set(ACTION_SERIAL,entityData.get(ACTION_SERIAL)+1);actionTicks=Math.max(ticks,20);
    }
    public boolean isActing(){return actionTicks>0;}
    @Override public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (isAlive() && tickCount % 4 == 0) {
                float scale = XuanjinRules.stage(seal()).scale();
                Vec3 mouth = getLookAngle().scale(1.3 * scale).add(position()).add(0,2.5 * scale,0);
                level().addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE,mouth.x,mouth.y,mouth.z,0,.025,0);
            }
            return;
        }
        if(!isAlive()) return;
        if(!isFree()) {
            var p=owner();
            if(p==null || !p.isAlive() || p.level()!=level() || !XuanjinCompanion.carrying(p)
                    || XuanjinCompanion.state(p).getBoolean("Freed") || !XuanjinCompanion.state(p).hasUUID("Body")
                    || !getUUID().equals(XuanjinCompanion.state(p).getUUID("Body"))
                    || XuanjinCompanion.state(p).getLong("ManifestUntil")<=level().getGameTime()) {
                if(p!=null && getUUID().equals(XuanjinCompanion.state(p).hasUUID("Body")?XuanjinCompanion.state(p).getUUID("Body"):null)) {
                    XuanjinCompanion.state(p).putFloat("Health",getHealth()); XuanjinCompanion.state(p).remove("Body");
                }
                discard(); return;
            }
            companionState().putFloat("Health",getHealth());
        }
        if(actionTicks>0 && --actionTicks==0) entityData.set(ACTION,"");
        if(attackTicks>0) attackTicks--;
        XuanjinCombat.tick(this,companionState());
        if (isNoAi()) return;
        if (isFree() && tickCount % 5 == 0) {
            for (var detected : XuanjinCombat.enemies(this,30)) huntQueue.add(detected.getUUID());
        }
        LivingEntity target=getTarget();
        if(target==null && huntTarget!=null && level() instanceof net.minecraft.server.level.ServerLevel server
                && server.getEntity(huntTarget) instanceof LivingEntity remembered && XuanjinCombat.enemy(this,remembered)) target=remembered;
        if(target!=null && !XuanjinCombat.enemy(this,target)) {
            boolean unloaded = target.isRemoved() && target.getRemovalReason() != null && !target.getRemovalReason().shouldDestroy();
            if (!unloaded) { huntQueue.remove(target.getUUID()); huntTarget=null; }
            target=null;
        }
        if (isFree() && target == null && level() instanceof net.minecraft.server.level.ServerLevel server) {
            var pending = huntQueue.iterator();
            while (pending.hasNext()) {
                var candidate = server.getEntity(pending.next());
                if (candidate instanceof LivingEntity living) {
                    if (!XuanjinCombat.enemy(this,living)) { pending.remove(); continue; }
                    target = living; break;
                }
            }
        }
        if(target==null && tickCount%5==0) {
            var choices=XuanjinCombat.enemies(this,isFree()?30:48); if(!choices.isEmpty()) target=choices.getFirst();
        }
        setTarget(target);
        if(target!=null) {
            if (isFree()) huntQueue.add(target.getUUID());
            huntTarget=target.getUUID(); getLookControl().setLookAt(target,30,30);
            double reach=seal()==6?4:Math.max(1.3,4*XuanjinRules.stage(seal()).scale());
            if(distanceToSqr(target)>reach*reach || !hasLineOfSight(target)) {
                if(tickCount%5==0) getNavigation().moveTo(target,1.2);
                pursue(target);
            } else {
                getNavigation().stop(); stuckTicks=0;
                if(attackTicks==0 && !isActing()) { XuanjinCombat.basic(this,target); attackTicks=36; }
                long now=level().getGameTime();
                if(!isActing() && seal()>=2 && XuanjinCompanion.ready(companionState(),"claw",now)) XuanjinCombat.command(this,companionState(),"claw",owner());
                if(!isActing() && seal()==6) {
                    XuanjinCombat.command(this,companionState(),"tails",owner());
                    if(!isActing() && getHealth()<getMaxHealth()*.5F) XuanjinCombat.command(this,companionState(),"vortex",owner());
                }
            }
            if(!isActing() && seal()>=2 && distanceToSqr(target)>reach*reach
                    && distanceToSqr(target)<=XuanjinRules.CLAW_RANGE*XuanjinRules.CLAW_RANGE
                    && hasLineOfSight(target) && XuanjinCompanion.ready(companionState(),"claw",level().getGameTime()))
                XuanjinCombat.command(this,companionState(),"claw",owner());
        } else if(!isFree() && owner()!=null && distanceToSqr(owner())>16) getNavigation().moveTo(owner(),1.2);
        if(!isActing() && seal()>=5 && getHealth()<getMaxHealth()*.9F) XuanjinCombat.command(this,companionState(),"devour",owner());
        if(!isActing() && isFree() && companionState().getBoolean("VortexReady")) XuanjinCombat.command(this,companionState(),getHealth()<getMaxHealth()*.8F?"swallow":"return",null);
        if(tickCount%20==0) lastProgress=position();
    }
    private void pursue(LivingEntity target) {
        if(tickCount%20!=0) return;
        if(position().distanceToSqr(lastProgress)<.3) stuckTicks+=20; else stuckTicks=0;
        if(stuckTicks<80 || !isFree()) return;
        // A released disaster does not give up at doors or cliffs. Phase to a collision-free
        // nearby point in loaded chunks, without destroying the player's buildings.
        for(int i=0;i<8;i++) {
            Vec3 point=target.position().add(new Vec3(3,0,0).yRot((float)(i*Math.PI/4)));
            if(!level().hasChunkAt(net.minecraft.core.BlockPos.containing(point))) continue;
            if(level().noCollision(this,getBoundingBox().move(point.subtract(position()))) && level().getWorldBorder().isWithinBounds(net.minecraft.core.BlockPos.containing(point))) {
                teleportTo(point.x,point.y,point.z); stuckTicks=0; break;
            }
        }
    }
    @Override public void die(DamageSource source) {
        if(!level().isClientSide && !isFree() && owner()!=null) {
            var s=companionState(); s.putFloat("Health",0); s.putLong("RecoverUntil",level().getGameTime()+1200);
            s.remove("Body"); s.putLong("ManifestUntil",0); s.putInt("Trust",Math.max(0,s.getInt("Trust")-5));
            XuanjinCompanion.tell(owner(),"defeated");
        }
        super.die(source);
    }
    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag); tag.putInt("Seal",seal()); tag.putBoolean("Free",isFree());
        if(ownerId()!=null) tag.putUUID("Owner",ownerId());
        tag.put("Passenger",companionState().copy()); if(huntTarget!=null) tag.putUUID("HuntTarget",huntTarget);
        var queued = new net.minecraft.nbt.ListTag();
        for (UUID id : huntQueue) queued.add(net.minecraft.nbt.StringTag.valueOf(id.toString()));
        tag.put("HuntQueue",queued);
        tag.putInt("AttackTicks",attackTicks);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Passenger")) wildState=tag.getCompound("Passenger").copy();
        entityData.set(FREE,!tag.contains("Free") || tag.getBoolean("Free")); entityData.set(OWNER,tag.hasUUID("Owner")?Optional.of(tag.getUUID("Owner")):Optional.empty());
        setSeal(tag.contains("Seal") ? tag.getInt("Seal") : 6); huntTarget=tag.hasUUID("HuntTarget")?tag.getUUID("HuntTarget"):null; attackTicks=tag.getInt("AttackTicks");
        huntQueue.clear();
        for (var value : tag.getList("HuntQueue",net.minecraft.nbt.Tag.TAG_STRING)) {
            try { huntQueue.add(UUID.fromString(value.getAsString())); } catch (IllegalArgumentException ignored) { }
        }
    }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this,"body",3,state -> {
            String action=entityData.get(ACTION);
            if(renderedActionSerial!=entityData.get(ACTION_SERIAL)) {
                renderedActionSerial=entityData.get(ACTION_SERIAL);state.getController().forceAnimationReset();
            }
            String clip=isDeadOrDying()?"death":!action.isEmpty()?action:state.isMoving()?(state.getLimbSwingAmount()>.35F?"run":"walk"):"idle";
            // Attacks and death finish once and hold; a repeated server attack restarts explicitly.
            boolean once=isDeadOrDying() || !action.isEmpty();
            state.getController().transitionLength(once?2:5);
            return state.setAndContinue(once?RawAnimation.begin().thenPlayAndHold("animation.xuanjin_zheng."+clip)
                    :RawAnimation.begin().thenLoop("animation.xuanjin_zheng."+clip));
        }));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
