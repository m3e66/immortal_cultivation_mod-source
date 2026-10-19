package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.entity.*;
import com.example.immortal_cultivation_mod.network.BeastVfxPayloads;
import com.example.immortal_cultivation_mod.network.BeastVfxPayloads.*;
import com.example.immortal_cultivation_mod.physics.CultivationFlightTrail;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Vector3f;

/** Anatomy-driven attacks, physical matter and confirmed impact residue. No gameplay or terrain mutation. */
@EventBusSubscriber(modid=ImmortalCultivationMod.MODID,value=Dist.CLIENT)
public final class BeastAttackVfx {
    private static final int MAX_TRACKED=48, MAX_IMPACTS=48;
    private static final Map<UUID,Track> TRACKS=new HashMap<>();
    private static final List<BeastImpactVfx> IMPACTS=new ArrayList<>();
    private static ClientLevel world;
    private record Snapshot(Vec3 position,long tick) {}
    private static final Map<UUID,Snapshot> POSITIONS=new HashMap<>();
    private static final class Track {
        final Entity entity;
        final CultivationFlightTrail path=new CultivationFlightTrail();
        final Map<String,CultivationFlightTrail> anatomy=new HashMap<>();
        int action,remaining,groundColor=0x716354;
        long started,seen;
        Vec3 sensedItem;
        boolean onGround;
        Track(Entity entity){this.entity=entity;onGround=entity.onGround();}
    }
    private BeastAttackVfx(){}
    private static boolean useLevel(ClientLevel level){
        if(world!=level){TRACKS.clear();IMPACTS.clear();POSITIONS.clear();world=level;}
        return level!=null;
    }
    public static void accept(Impact packet){
        var mc=Minecraft.getInstance();
        if(!useLevel(mc.level) || !world.dimension().location().equals(packet.dimension())
                || !CultivationFlightTrail.finite(packet.point()) || !CultivationFlightTrail.finite(packet.direction())
                || packet.point().distanceToSqr(mc.gameRenderer.getMainCamera().getPosition())>96*96)return;
        if(IMPACTS.size()>=MAX_IMPACTS)IMPACTS.remove(0);
        IMPACTS.add(new BeastImpactVfx(packet,world));
    }
    static boolean projectile(Entity entity){
        return entity instanceof CultivationBeastProjectileEntity || entity instanceof PhantomFoxfireProjectileEntity
                || entity instanceof CrimsonDragonScaleProjectileEntity || entity instanceof CrimsonFlameFireballEntity
                || entity instanceof SpiritSilkProjectileEntity || entity instanceof ThunderRockProjectileEntity || entity instanceof KongmingCrystalProjectileEntity
                || entity instanceof Projectile shot && shot.getOwner() instanceof RealmBeast;
    }
    static int color(Style style){return switch(style){
        case SILK->0xB3F5EC;case RAT->0x5FAE81;case SCORPION->0x9F3440;case WIND->0xBDDCD6;
        case EARTH->0xAD8C56;case OWL->0xEB7A3D;case LIZARD->0xD65828;case SPACE->0x9D85BE;
        case THUNDER->0xF0D58D;case FROST->0xB8DAE7;case BLOOD->0x923242;
        case DRAGON->0xDE7B39;case FOX->0xDBE1E0;case GUARDIAN->0xD6B277;
    };}
    static Style style(Entity entity){
        if(entity instanceof CultivationBeastProjectileEntity projectile)return switch(projectile.projectileKind()){
            case WIND_FEATHER->Style.WIND;case STONE_PELLET->Style.EARTH;case FIRE_FEATHER->Style.OWL;};
        if(entity instanceof CrimsonFlameFireballEntity)return Style.LIZARD;
        if(entity instanceof SpiritSilkProjectileEntity)return Style.SILK;
        if(entity instanceof ThunderRockProjectileEntity)return Style.THUNDER;
        if(entity instanceof KongmingCrystalProjectileEntity)return Style.SPACE;
        if(entity instanceof PhantomFoxfireProjectileEntity)return Style.FOX;
        if(entity instanceof CrimsonDragonScaleProjectileEntity)return Style.DRAGON;
        if(entity instanceof CultivationBeastFirePatchEntity)return Style.OWL;
        if(entity instanceof Projectile shot)return BeastVfxPayloads.style(shot.getOwner());
        return BeastVfxPayloads.style(entity);
    }
    private static int action(Entity entity){
        if(entity instanceof AzureSpiritSilkwormEntity worm)return worm.isCocooned()?2:worm.getSpitTicks()>0?1:0;
        int action=BeastVfxPayloads.move(entity);
        if(action!=0)return action;
        if(entity instanceof KongmingBeetleEntity b && b.isEnraged())return 100;
        if(entity instanceof ThunderhornRockBadgerEntity b && b.isFrenzied())return 100;
        if(entity instanceof FrostspineCaveBearEntity b)return b.isFrenzied()?100:b.isInFog()?101:0;
        if(entity instanceof BloodApeDemonGeneralEntity b && b.isBerserk())return 100;
        if(entity instanceof CultivationBestiaryBeastEntity b && b.kind()==BestiaryBeastKind.SPIRIT_DEVOURING_RAT
                && b.hasVisualFlag(16))return 100;
        return entity instanceof LivingEntity living && living.swinging?-1:0;
    }
    private static int remaining(Entity entity){
        if(entity instanceof CultivationBestiaryBeastEntity b)return b.visualActionTicks();
        if(entity instanceof CrimsonScaleFlameLizardEntity b)return b.getActionTicks();
        if(entity instanceof KongmingBeetleEntity b)return b.getActionTicks();
        if(entity instanceof ThunderhornRockBadgerEntity b)return b.getActionTicks();
        if(entity instanceof FrostspineCaveBearEntity b)return b.getActionTicks();
        if(entity instanceof BloodApeDemonGeneralEntity b)return b.getActionTicks();
        if(entity instanceof CrimsonSkyFlameFloodDragonEntity b)return b.getActionTicks();
        if(entity instanceof PhantomVeilFoxEntity b)return b.visualActionTicks();
        if(entity instanceof VaultGuardianEntity b)return b.getActionTicks();
        if(entity instanceof AzureSpiritSilkwormEntity b)return b.isCocooned()?100:b.getSpitTicks();
        return 6;
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        var mc=Minecraft.getInstance();if(!useLevel(mc.level)||mc.isPaused())return;
        long now=world.getGameTime();
        IMPACTS.removeIf(i->i.expired(now));
        POSITIONS.entrySet().removeIf(e->now-e.getValue().tick>3);
        Vec3 camera=mc.gameRenderer.getMainCamera().getPosition();
        // Nearest active attacks first: idle beasts cannot consume slots intended for projectiles.
        List<Entity> nearby=new ArrayList<>();
        for(Entity entity:world.entitiesForRendering())
            if((entity instanceof RealmBeast||projectile(entity)||entity instanceof CultivationBeastFirePatchEntity)
                    &&entity.isAlive()&&!entity.isInvisible()&&entity.distanceToSqr(camera)<96*96)nearby.add(entity);
        nearby.sort(Comparator.comparingDouble(e->e.distanceToSqr(camera)));
        for(Entity entity:nearby){
            int move=action(entity);Track track=TRACKS.get(entity.getUUID());
            Snapshot previous=POSITIONS.get(entity.getUUID());
            if(POSITIONS.size()<64||previous!=null)POSITIONS.put(entity.getUUID(),new Snapshot(entity.position(),now));
            if(track!=null&&track.action!=0&&move==0&&style(entity)==Style.SILK)
                accept(new Impact(world.dimension().location(),Style.SILK,Phase.DISSOLVE,entity.getId(),track.action,
                        entity.getBoundingBox().getCenter(),entity.getLookAngle(),false,entity.getId()));
            if(move==0&&!projectile(entity)&&!(entity instanceof CultivationBeastFirePatchEntity)
                    &&(track==null||track.path.opacity(now)<=0)){TRACKS.remove(entity.getUUID());continue;}
            if(track==null){if(TRACKS.size()>=MAX_TRACKED)continue;track=new Track(entity);TRACKS.put(entity.getUUID(),track);}
            track.seen=now;int left=remaining(entity);
            if(move!=track.action||left>track.remaining){
                track.started=now;
                Style kind=style(entity);
                if(previous!=null&&entity.position().distanceToSqr(previous.position)>1
                        &&(kind==Style.RAT&&move==2||kind==Style.FOX&&move==1||kind==Style.SPACE&&move==5)){
                    accept(new Impact(world.dimension().location(),kind,Phase.DISSOLVE,entity.getId(),move,
                            previous.position.add(0,entity.getBbHeight()*.4,0),entity.getLookAngle(),false,entity.getId()));
                    track.path.sample(previous.position.add(0,entity.getBbHeight()*.5,0),now-1);
                }
            }
            track.action=move;track.remaining=left;
            if(move!=0||projectile(entity)){
                track.path.sample(entity.getBoundingBox().getCenter(),now);
                sample(track,"mouth",now,"jaw","head");sample(track,"left",now,"hand_l","pincer_left","front_leg_left","antler_l_s0");
                sample(track,"right",now,"hand_r","pincer_right","front_leg_right","antler_r_s0");
                sample(track,"tail",now,"stinger","tail_tip","tail_5","tail_0_tip","tail_3");
                sample(track,"wing_l",now,"wing_left_hand");sample(track,"wing_r",now,"wing_right_hand");
            }
            if(!track.onGround&&entity.onGround()&&move!=0){
                Style style=style(entity);
                if(style!=null)accept(new Impact(world.dimension().location(),style,Phase.LANDING,entity.getId(),move,
                        entity.position(),entity.getDeltaMovement(),false,entity.getId()*31+(int)now));
            }
            track.onGround=entity.onGround();
            if(move==3 && entity instanceof CultivationBestiaryBeastEntity b && b.kind()==BestiaryBeastKind.SPIRIT_DEVOURING_RAT && (now&7)==0){
                var items=world.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,entity.getBoundingBox().inflate(6),item->{
                    String path=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item.getItem().getItem()).getPath();
                    return item.isAlive()&&(path.endsWith("spirit_stone")||path.contains("pill")
                            || com.example.immortal_cultivation_mod.item.ForgingSystem.isPurityMaterial(item.getItem()));});
                track.sensedItem=items.stream().min(Comparator.comparingDouble(entity::distanceToSqr))
                        .map(Entity::position).orElse(null);
            }
            if((now&7)==0){BlockPos ground=entity.blockPosition().below();
                if(world.hasChunkAt(ground)){int value=world.getBlockState(ground).getMapColor(world,ground).col;
                    if(value!=0)track.groundColor=value;}}
            track.path.prune(now);track.anatomy.values().forEach(p->p.prune(now));
        }
        TRACKS.entrySet().removeIf(e->e.getValue().seen!=now);
    }
    private static void sample(Track track,String slot,long now,String... bones){
        track.anatomy.computeIfAbsent(slot,s->new CultivationFlightTrail()).sample(
                BeastVfxAnchors.point(track.entity,track.entity.getBoundingBox().getCenter(),bones),now);
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(event.getStage()==RenderLevelStageEvent.Stage.AFTER_SKY) { BeastVfxGeometry.beginEntities(); return; }
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES||ShaderPackCompatibility.isRenderingShadowPass())return;
        var mc=Minecraft.getInstance();if(!useLevel(mc.level))return;
        float partial=event.getPartialTick().getGameTimeDeltaPartialTick(false);
        double time=world.getGameTime()+partial;Vec3 camera=event.getCamera().getPosition();
        Vec3 right=new Vec3(new Vector3f(1,0,0).rotate(event.getCamera().rotation()));
        Vec3 up=new Vec3(new Vector3f(0,1,0).rotate(event.getCamera().rotation()));
        boolean minimal=mc.options.particles().get()==ParticleStatus.MINIMAL;
        BeastVfxGeometry.begin(minimal);
        var buffers=mc.renderBuffers().bufferSource();
        var geometry=new BeastVfxGeometry(buffers.getBuffer(FilamentVfx.TYPE),buffers.getBuffer(CombatImpactVfx.MATTER),
                event.getPoseStack().last().pose(),camera,right,up);
        int budget=minimal?6:18;
        var tracks=TRACKS.values().stream().filter(t->t.entity.distanceToSqr(camera)<96*96)
                .sorted(Comparator.comparingDouble(t->t.entity.distanceToSqr(camera))).limit(budget).toList();
        var impacts=IMPACTS.stream().filter(i->i.point().distanceToSqr(camera)<96*96)
                .sorted(Comparator.comparingDouble(i->i.point().distanceToSqr(camera))).limit(Math.min(8,budget)).toList();

        int impactBudget = minimal ? 350 : Math.min(1800, 5600 / Math.max(1, impacts.size()));
        for(var impact:impacts){if(BeastVfxGeometry.exhausted())break;BeastVfxGeometry.object(impactBudget);impact.draw(geometry,time);}
        for(Track track:tracks){
            if(BeastVfxGeometry.exhausted())break;
            BeastVfxGeometry.object(minimal?500:4000);
            Entity entity=track.entity;Style style=style(entity);if(style==null)continue;
            Vec3 center=entity.getPosition(partial).add(0,entity.getBbHeight()*.5,0);
            var paths=new HashMap<String,List<Vec3>>();
            for(var entry:track.anatomy.entrySet())paths.put(entry.getKey(),entry.getValue().points(null));
            if(track.sensedItem!=null)paths.put("scent",List.of(track.sensedItem));
            if(projectile(entity))BeastMoveVfx.projectile(geometry,entity,style,center,track.path.points(center),time,track.groundColor,false);
            else if(entity instanceof CultivationBeastFirePatchEntity)BeastMoveVfx.embers(geometry,center,1.2,time,.5F);
            else BeastMoveVfx.draw(geometry,entity,center,style,track.action,time-track.started,track.remaining,time,
                    track.groundColor,paths,track.path.points(center));
        }
        buffers.endBatch(CombatImpactVfx.MATTER);buffers.endBatch(FilamentVfx.TYPE);
    }
}
