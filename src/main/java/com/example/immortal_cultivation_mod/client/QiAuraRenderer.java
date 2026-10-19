package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.combat.*;
import com.example.immortal_cultivation_mod.network.QiAuraPayloads;
import com.example.immortal_cultivation_mod.physics.QiFlowPhysics;
import com.example.immortal_cultivation_mod.physics.QiBodyFlamePhysics;
import com.example.immortal_cultivation_mod.client.qi.*;
import com.mojang.blaze3d.vertex.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

/** Presentation of confirmed combat state. No client-side damage, knockback or terrain edits. */
@EventBusSubscriber(modid=ImmortalCultivationMod.MODID,value=Dist.CLIENT)
public final class QiAuraRenderer {
    private static final Map<UUID,Flow> FLOWS=new HashMap<>();
    private static final List<Impact> IMPACTS=new ArrayList<>();
    private static final List<Launch> LAUNCHES=new ArrayList<>();
    private static final List<DomainTrace> DOMAIN_TRACES=new ArrayList<>();
    private static ClientLevel world;
    private static int rendered,woodMotes;
    public static long hitEvents,activationEvents,clashFrames,collisionEvents,launchEvents;
    public static long drainEvents,devouringEvents;
    public static long stormEvents,mountainEvents,frostEvents,healingEvents;
    public static long vitalityEvents,winterEvents,boneEvents,solarEvents,plumeEvents;
    private static long lastArcSound=Long.MIN_VALUE;
    /** Hide only the camera wearer's Bone Bell rings; other combatants remain readable. */
    public static boolean bodyRingsVisible(LivingEntity entity,int art) {
        var mc=Minecraft.getInstance();
        return art!=11||entity!=mc.getCameraEntity()||!mc.options.getCameraType().isFirstPerson();
    }
    private static final class Flow {
        QiAuraPayloads.State state;long until,ignited=-100,stopped=-100;
        final QiBodyFlamePhysics body=new QiBodyFlamePhysics();
        QiBodyFlamePhysics.Snapshot bodyFrame;
        float renderedHeight;
        boolean sounded;
        Flow(QiAuraPayloads.State state){this.state=state;}
    }
    private record Impact(int id,UUID uuid,Vec3 point,Vec3 offset,Vec3 direction,QiElementLook look,long start,float power){}
    private record Launch(int id,UUID uuid,QiElementLook look,long start){}
    private record DomainTrace(QiAuraPayloads.DomainEffect packet,long start,Vec3 origin){}
    private QiAuraRenderer(){}
    private static void use(ClientLevel level){
        if(world!=level){FLOWS.clear();IMPACTS.clear();LAUNCHES.clear();DOMAIN_TRACES.clear();lastArcSound=Long.MIN_VALUE;world=level;QiDomainDistortion.close();DomainClashVisuals.clear();QiEnergyParticles.clear();ElementalDomainVisuals.clear();IceCoatingClient.clear();BloodLilyClient.clear();ReincarnationEchoClient.clear();WoodBloomClient.clear();}
    }
    private static boolean valid(ResourceLocation dimension,Vec3 p,int style){
        return world!=null&&world.dimension().location().equals(dimension)&&QiFlowPhysics.finite(p)
                &&style>=0&&style<MethodDomainStyle.values().length
                &&p.distanceToSqr(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition())<128*128;
    }
    public static void accept(QiAuraPayloads.State packet){
        use(Minecraft.getInstance().level);
        if(world==null||!world.dimension().location().equals(packet.dimension())||!Float.isFinite(packet.power())
                ||packet.percent()<0||packet.percent()>200||packet.mode()<0||packet.mode()>4||packet.attackMode()<0||packet.attackMode()>3
                ||packet.power()<0||packet.power()>1||!Float.isFinite(packet.sunEnergy())||packet.sunEnergy()<0||packet.sunEnergy()>100)return;
        Flow old=FLOWS.get(packet.uuid());
        if(packet.mode()==0){if(old!=null&&old.stopped<0)old.stopped=world.getGameTime();return;}
        Entity e=world.getEntity(packet.entityId());
        if(!(e instanceof LivingEntity)||!e.getUUID().equals(packet.uuid()))return;
        if(old==null){if(FLOWS.size()>=64)return;old=new Flow(packet);FLOWS.put(packet.uuid(),old);}
        old.state=packet;old.until=world.getGameTime()+Math.clamp(packet.ttl(),1,40);old.stopped=-100;
    }
    public static void activate(QiAuraPayloads.Activation packet){
        use(Minecraft.getInstance().level);Vec3 p=new Vec3(packet.x(),packet.y(),packet.z());
        if(!valid(packet.dimension(),p,packet.style())||packet.percent()<100||packet.percent()>200)return;
        Flow f=FLOWS.get(packet.uuid());
        if(f==null)return;
        if(f.state.entityId()!=packet.entityId())return;
        f.ignited=world.getGameTime();f.sounded=false;activationEvents++;
        sound(p,SoundEvents.BEACON_POWER_SELECT,.8F,.5F);
    }
    public static void hit(QiAuraPayloads.Hit packet){
        use(Minecraft.getInstance().level);Vec3 p=new Vec3(packet.x(),packet.y(),packet.z());
        if(!valid(packet.dimension(),p,packet.style())||!Float.isFinite(packet.power())||packet.power()<0||packet.power()>1)return;
        Entity e=world.getEntity(packet.entityId());
        if(e==null||!e.getUUID().equals(packet.uuid()))return;
        Vec3 offset=p.subtract(e.position());
        Vec3 direction=e.getBoundingBox().getCenter().subtract(p).normalize();
        if(direction.lengthSqr()<.1)direction=new Vec3(0,1,0);
        addImpact(new Impact(e.getId(),e.getUUID(),p,offset.yRot((float)Math.toRadians(e.getYRot())),
                direction,QiElementLook.of(MethodDomainStyle.values()[packet.style()]),world.getGameTime(),packet.power()));
        hitEvents++;
    }
    public static void burst(QiAuraPayloads.Burst packet){
        use(Minecraft.getInstance().level);Vec3 p=new Vec3(packet.x(),packet.y(),packet.z());
        if(!valid(packet.dimension(),p,packet.style()))return;
        addImpact(new Impact(-1,new UUID(0,0),p,Vec3.ZERO,new Vec3(0,1,0),
                QiElementLook.of(MethodDomainStyle.values()[packet.style()]),world.getGameTime(),.6F));
        if(MethodDomainStyle.values()[packet.style()].kind==MethodDomainStyle.Kind.WIND) {
            BlockPos ground=BlockPos.containing(p).below();
            for(int depth=0;depth<6&&world.getBlockState(ground).isAir();depth++)ground=ground.below();
            var debris=world.getBlockState(ground);
            for(int i=0;i<12;i++)QiEnergyParticles.debris(p,new Vec3(world.random.nextGaussian()*.14,.12+world.random.nextDouble()*.18,
                    world.random.nextGaussian()*.14),debris);
        }
    }
    public static void domainEffect(QiAuraPayloads.DomainEffect packet) {
        use(Minecraft.getInstance().level);if(world==null||!world.dimension().location().equals(packet.dimension())
                ||packet.style()<0||packet.style()>=MethodDomainStyle.values().length
                ||packet.kind()<QiAuraPayloads.DomainEffect.DRAIN||packet.kind()>QiAuraPayloads.DomainEffect.TIDE_RIPPLE
                ||packet.strength()<1||packet.strength()>(packet.kind()==QiAuraPayloads.DomainEffect.SOLAR?100:
                        packet.kind()==QiAuraPayloads.DomainEffect.WINTER?255:8))return;
        var owner=world.getEntity(packet.ownerId());var victim=world.getEntity(packet.victimId());
        if(!(owner instanceof LivingEntity)||!(victim instanceof LivingEntity)||!owner.getUUID().equals(packet.ownerUuid())
                ||!victim.getUUID().equals(packet.victimUuid())||owner.distanceToSqr(victim)>24*24)return;
        var style=MethodDomainStyle.values()[packet.style()];
        if(packet.kind()==QiAuraPayloads.DomainEffect.WARP&&style!=MethodDomainStyle.DEVOUR)return;
        if(packet.kind()==QiAuraPayloads.DomainEffect.DRAIN&&style.kind!=MethodDomainStyle.Kind.BLOOD
                &&style.kind!=MethodDomainStyle.Kind.DRAIN&&style.kind!=MethodDomainStyle.Kind.CROWN)return;
        if(packet.kind()==QiAuraPayloads.DomainEffect.ARC&&style!=MethodDomainStyle.STORM)return;
        if(packet.kind()==QiAuraPayloads.DomainEffect.MOUNTAIN&&style!=MethodDomainStyle.MOUNTAIN)return;
        if(packet.kind()==QiAuraPayloads.DomainEffect.FROST&&style!=MethodDomainStyle.COLD_TIDE)return;
        if(packet.kind()==QiAuraPayloads.DomainEffect.HEAL&&style!=MethodDomainStyle.COLD_TIDE&&style!=MethodDomainStyle.ICE)return;
        if((packet.kind()==QiAuraPayloads.DomainEffect.TIDE_WATER||packet.kind()==QiAuraPayloads.DomainEffect.TIDE_RIPPLE)
                &&style!=MethodDomainStyle.COLD_TIDE)return;
        if((packet.kind()==QiAuraPayloads.DomainEffect.VITALITY||packet.kind()==QiAuraPayloads.DomainEffect.FIRE_PLUME)
                &&style!=MethodDomainStyle.RENEWAL_FIRE)return;
        if(packet.kind()==QiAuraPayloads.DomainEffect.WINTER&&style!=MethodDomainStyle.ICE)return;
        if(packet.kind()==QiAuraPayloads.DomainEffect.BONE&&style!=MethodDomainStyle.BONE)return;
        if(packet.kind()==QiAuraPayloads.DomainEffect.SOLAR&&style!=MethodDomainStyle.SOLAR)return;
        if(packet.kind()==QiAuraPayloads.DomainEffect.REBIRTH&&style!=MethodDomainStyle.REINCARNATION)return;
        if(packet.kind()==QiAuraPayloads.DomainEffect.BLOOD_MARK&&(style.ordinal()<2||style.ordinal()>4))return;
        if(packet.kind()==QiAuraPayloads.DomainEffect.WIND_LIFT&&style!=MethodDomainStyle.WIND)return;
        if(packet.kind()==QiAuraPayloads.DomainEffect.REBIRTH)ReincarnationEchoClient.accept((LivingEntity)victim);
        // Refresh status snapshots instead of stacking duplicate geometry every second.
        if(packet.kind()>=QiAuraPayloads.DomainEffect.MOUNTAIN&&packet.kind()!=QiAuraPayloads.DomainEffect.TIDE_RIPPLE)
            DOMAIN_TRACES.removeIf(trace->trace.packet.kind()==packet.kind()&&trace.packet.ownerUuid().equals(packet.ownerUuid())
                    &&trace.packet.victimUuid().equals(packet.victimUuid()));
        if(DOMAIN_TRACES.size()>=128)DOMAIN_TRACES.removeFirst();
        DOMAIN_TRACES.add(new DomainTrace(packet,world.getGameTime(),packet.kind()==QiAuraPayloads.DomainEffect.TIDE_RIPPLE?
                ElementalDomainVisuals.rippleOrigin((LivingEntity)victim):victim.position()));
        switch(packet.kind()) {
            case QiAuraPayloads.DomainEffect.DRAIN -> drainEvents++;
            case QiAuraPayloads.DomainEffect.WARP -> devouringEvents++;
            case QiAuraPayloads.DomainEffect.ARC -> {
                stormEvents++;
                long now=world.getGameTime();
                if(lastArcSound==Long.MIN_VALUE||now-lastArcSound>=8) {
                    lastArcSound=now;sound(victim.position(),SoundEvents.LIGHTNING_BOLT_IMPACT,.13F,1.7F);
                }
            }
            case QiAuraPayloads.DomainEffect.MOUNTAIN -> mountainEvents++;
            case QiAuraPayloads.DomainEffect.FROST -> frostEvents++;
            case QiAuraPayloads.DomainEffect.HEAL -> healingEvents++;
            case QiAuraPayloads.DomainEffect.VITALITY -> vitalityEvents++;
            case QiAuraPayloads.DomainEffect.WINTER -> winterEvents++;
            case QiAuraPayloads.DomainEffect.BONE -> boneEvents++;
            case QiAuraPayloads.DomainEffect.SOLAR -> solarEvents++;
            case QiAuraPayloads.DomainEffect.FIRE_PLUME -> plumeEvents++;
        }
    }
    public static void woodBloom(QiAuraPayloads.WoodBloom packet){use(Minecraft.getInstance().level);WoodBloomClient.accept(packet);}
    private static void addImpact(Impact impact){
        if(IMPACTS.size()>=32)IMPACTS.removeFirst();IMPACTS.add(impact);
        for(int i=0;i<10;i++){
            Vec3 scatter=new Vec3(world.random.nextGaussian(),world.random.nextDouble(),world.random.nextGaussian()).scale(.12);
            QiEnergyParticles.energy(impact.point,impact.direction.scale(.18+impact.power*.22).add(scatter),
                    impact.look.art()==5?ReincarnationVisuals.particle(i%2==0):impact.look,8);
        }
    }
    public static void motion(QiAuraPayloads.Motion packet){
        use(Minecraft.getInstance().level);Vec3 p=new Vec3(packet.x(),packet.y(),packet.z()),d=new Vec3(packet.dx(),packet.dy(),packet.dz());
        if(!valid(packet.dimension(),p,packet.style())||!QiFlowPhysics.finite(d)||d.lengthSqr()>64||packet.blockState()<0)return;
        var look=QiElementLook.of(MethodDomainStyle.values()[packet.style()]);
        var state=Block.stateById(packet.blockState());
        for(int i=0;i<(packet.collision()?16:8);i++){
            Vec3 v=d.normalize().scale(.15+world.random.nextDouble()*.3).add(
                    world.random.nextGaussian()*.15,world.random.nextDouble()*.25,world.random.nextGaussian()*.15);
            QiEnergyParticles.debris(p.add(world.random.nextDouble()-.5,world.random.nextDouble()*.4,world.random.nextDouble()-.5),v,state);
        }
        addImpact(new Impact(-1,new UUID(0,0),p,Vec3.ZERO,d.normalize(),look,world.getGameTime(),packet.collision()?1:.7F));
        if(packet.collision())collisionEvents++;
        else {if(LAUNCHES.size()>=24)LAUNCHES.removeFirst();LAUNCHES.add(new Launch(packet.entityId(),packet.uuid(),look,world.getGameTime()));launchEvents++;}
    }
    public static boolean isBlasting(UUID uuid){
        Flow f=FLOWS.get(uuid);return f!=null&&f.state.mode()==QiDomainSystem.BODY&&world!=null&&f.until>=world.getGameTime()&&f.stopped<0;
    }
    public static boolean hasFlow(UUID uuid){return FLOWS.containsKey(uuid);}
    public static int activeFlows(){return FLOWS.size();}
    public static float sunEnergy(LivingEntity owner){var flow=FLOWS.get(owner.getUUID());return flow==null?0:flow.state.sunEnergy();}
    public static int bloodMarks(LivingEntity owner) {
        if(world==null)return 0;
        return (int)DOMAIN_TRACES.stream().filter(trace->trace.packet.kind()==QiAuraPayloads.DomainEffect.BLOOD_MARK
                &&trace.packet.ownerUuid().equals(owner.getUUID())&&world.getGameTime()-trace.start<80).count();
    }
    public static boolean hasDomainFire(LivingEntity target) {
        if(world==null||world!=target.level())return false;
        for(var trace:DOMAIN_TRACES)if((trace.packet.kind()==QiAuraPayloads.DomainEffect.FIRE_PLUME
                ||trace.packet.kind()==QiAuraPayloads.DomainEffect.SOLAR)&&trace.packet.victimUuid().equals(target.getUUID())
                &&trace.packet.victimId()==target.getId()&&world.getGameTime()-trace.start<traceLifetime(trace.packet.kind()))return true;
        return false;
    }
    private static QiElementLook look(Flow flow) {
        var look=QiElementLook.of(MethodDomainStyle.of(flow.state.method()));
        if(look.art()!=13)return look;
        float brightness=.38F+flow.state.sunEnergy()*.0062F;
        int rgb=look.color();
        int color=((int)((rgb>>16&255)*brightness)<<16)|((int)((rgb>>8&255)*brightness)<<8)|(int)((rgb&255)*brightness);
        return new QiElementLook(color,look.highlight(),look.family(),look.speed(),look.art());
    }
    public static long activeDomainEffects(int kind) {
        return world==null?0:DOMAIN_TRACES.stream().filter(trace->trace.packet.kind()==kind
                &&world.getGameTime()-trace.start<traceLifetime(kind)).count();
    }
    public static int activeBlasts(){return (int)FLOWS.values().stream().filter(f->world!=null&&world.getGameTime()-f.ignited<QiEruption.READY).count();}
    public static int attachedImpacts(){return IMPACTS.size();}
    public static int renderedQuads(){return rendered;}
    public static int renderedWoodMotes(){return woodMotes;}
    public static Vec3 bodyAxis(UUID uuid){
        Flow f=FLOWS.get(uuid);return f==null||f.bodyFrame==null?new Vec3(0,1,0):new Vec3(0,f.renderedHeight*.55,0).add(f.bodyFrame.offset(1)).normalize();
    }
    private static Vec3 bodyBase(LivingEntity owner,Vec3 p,float height,float partial) {
        Vec3 torso=QiAuraPose.bodyOffset(owner,partial);
        return p.add(torso).add(0,Math.max(0,height*.45-torso.y)*(1-QiAuraPose.flightBlend(owner,partial))-height*.45-.08,0);
    }
    /** World point on an attached current follows the same elastic field as the visible body mesh. */
    public static Vec3 deformBodyPoint(LivingEntity owner,Vec3 point,float partial) {
        Flow f=FLOWS.get(owner.getUUID());if(f==null||f.state.mode()!=QiDomainSystem.BODY||f.bodyFrame==null||f.renderedHeight<=0)return point;
        Vec3 base=bodyBase(owner,owner.getPosition(partial),f.renderedHeight,partial),local=point.subtract(base);
        return base.add(f.bodyFrame.deform(local,local.y/f.renderedHeight));
    }
    public static float renderedBodyHeight(UUID uuid){Flow f=FLOWS.get(uuid);return f==null?0:f.renderedHeight;}
    public static Vec3 bodyDisplacement(UUID uuid,double t){Flow f=FLOWS.get(uuid);return f==null||f.bodyFrame==null?Vec3.ZERO:f.bodyFrame.offset(t);}
    public static double blastAge(UUID uuid){Flow f=FLOWS.get(uuid);return f==null||world==null||f.ignited<0?-1:world.getGameTime()-f.ignited;}
    public static MethodDomainStyle coating(LivingEntity entity,boolean weapon){
        if(ShaderPackCompatibility.isRenderingShadowPass())return null;
        Flow f=FLOWS.get(entity.getUUID());
        if(f==null||world==null||f.stopped>=0||f.until<world.getGameTime()||entity.isInvisible()||!entity.isAlive())return null;
        int mode=f.state.mode(),attack=f.state.attackMode();
        return mode==QiDomainSystem.BODY||(weapon?(mode==QiDomainSystem.WEAPON||attack==QiDomainSystem.WEAPON)
                :(mode==QiDomainSystem.FIST||attack==QiDomainSystem.FIST))?MethodDomainStyle.of(f.state.method()):null;
    }
    private static void sound(Vec3 p,SoundEvent event,float volume,float pitch){
        world.playLocalSound(p.x,p.y,p.z,event,SoundSource.PLAYERS,volume,pitch,false);
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event){use(null);}
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        var mc=Minecraft.getInstance();use(mc.level);if(world==null||mc.isPaused())return;
        long now=world.getGameTime();IceCoatingClient.tick();BloodLilyClient.tick();ReincarnationEchoClient.tick();WoodBloomClient.tick();
        FLOWS.entrySet().removeIf(entry->{
            Flow f=entry.getValue();Entity e=world.getEntity(f.state.entityId());
            boolean expired=now>f.until+6||(f.stopped>=0&&now-f.stopped>6)||e==null||!e.getUUID().equals(entry.getKey())||!e.isAlive()||e.isRemoved();
            return expired;
        });
        IMPACTS.removeIf(i->now-i.start>=20);
        LAUNCHES.removeIf(i->now-i.start>=12);
        DOMAIN_TRACES.removeIf(i->now-i.start>=traceLifetime(i.packet.kind()));
        for(var trace:DOMAIN_TRACES) {
            var packet=trace.packet;var target=world.getEntity(packet.victimId());
            if(packet.kind()==QiAuraPayloads.DomainEffect.WARP&&now-trace.start==22&&target!=null&&target.getUUID().equals(packet.victimUuid())) {
                for(int j=0;j<16;j++)QiEnergyParticles.energy(target.getBoundingBox().getCenter(),new Vec3(world.random.nextGaussian(),world.random.nextGaussian(),world.random.nextGaussian()).scale(.09),
                        new QiElementLook(0x120A1C,0x311141,QiElementLook.DARK,.4F),14);
            }
            if(!(target instanceof LivingEntity)||!target.getUUID().equals(packet.victimUuid())||now%5!=0)continue;
            if(packet.kind()==QiAuraPayloads.DomainEffect.MOUNTAIN&&target.position().subtract(new Vec3(target.xo,target.yo,target.zo)).horizontalDistanceSqr()>.0001) {
                var ground=world.getBlockState(target.getOnPos());
                for(int j=0;j<2;j++)QiEnergyParticles.debris(target.position().add(world.random.nextGaussian()*.25,.06,world.random.nextGaussian()*.25),
                        new Vec3(world.random.nextGaussian()*.05,.08,world.random.nextGaussian()*.05),ground);
            }else if(packet.kind()==QiAuraPayloads.DomainEffect.FROST) {
                var ice=new QiElementLook(0x347DC0,0xBBF8FF,QiElementLook.ICE,.6F);
                QiEnergyParticles.energy(target.position().add(world.random.nextGaussian()*.45,world.random.nextDouble()*target.getBbHeight(),world.random.nextGaussian()*.45),
                        new Vec3(world.random.nextGaussian()*.025,.025,world.random.nextGaussian()*.025),ice,16);
            }
        }
        int actors=0;
        var ordered=new ArrayList<>(FLOWS.values());
        ordered.sort(Comparator.comparingDouble(f->{var e=world.getEntity(f.state.entityId());return mc.player==null?0:e.distanceToSqr(mc.player);}));
        for(Flow f:ordered){
            var e=world.getEntity(f.state.entityId());if(e==null||e.isInvisible()||mc.player==null||e.distanceToSqr(mc.player)>64*64||++actors>QiVfxQuality.actors())continue;
            double age=now-f.ignited;
            if(e instanceof LivingEntity living&&f.state.mode()==QiDomainSystem.BODY){
                Vec3 delta=e.position().subtract(new Vec3(e.xo,e.yo,e.zo));
                Vec3 motion=delta.lengthSqr()>1E-8&&delta.lengthSqr()<64?delta:e.getDeltaMovement();
                if(e.onGround())motion=new Vec3(motion.x,0,motion.z);
                double yaw=Math.toRadians(living.yBodyRot);
                double airborne=Math.max(QiAuraPose.flightBlend(living,1),e.onGround()?0:Math.clamp((motion.length()-.25)/.7,0,1));
                f.renderedHeight=QiEruption.bodyHeight(living.getDimensions(Pose.STANDING).height(),f.state.percent(),age);
                f.body.tick(e.position().add(QiAuraPose.bodyOffset(living,1)),motion,new Vec3(Math.sin(yaw),0,-Math.cos(yaw)),airborne,f.renderedHeight,now+e.getId()*7);
                f.bodyFrame=f.body.snapshot(1);
            }
            var look=look(f);
            if(look.art()==14&&f.state.mode()==QiDomainSystem.BODY&&f.state.percent()==200&&f.state.mastered()&&f.state.power()>0
                    &&Math.floorMod(now-e.getId(),120)==0)
                sound(e.position().add(0,5,0),SoundEvents.LIGHTNING_BOLT_THUNDER,.10F,.65F);
            if(look.art()==11&&f.state.mode()==QiDomainSystem.BODY&&f.state.percent()==200&&f.state.mastered()&&f.state.power()>0
                    &&Math.floorMod(now-e.getId(),100)==0)sound(e.position(),SoundEvents.BELL_BLOCK,.16F,.55F);
            if(f.state.mode()==QiDomainSystem.MOTE&&!ClientData.showQiUseParticles())continue;
            Vec3 p=e.position();
            if(age>=QiEruption.GATHER&&age<QiEruption.READY&&!f.sounded){
                f.sounded=true;sound(p,SoundEvents.GENERIC_EXPLODE.value(),1.3F,.65F);
                sound(p,SoundEvents.WARDEN_SONIC_BOOM,.55F,1.6F);
                sound(p,SoundEvents.ELYTRA_FLYING,.7F,.6F);
                sound(p,SoundEvents.AMETHYST_BLOCK_RESONATE,.6F,.65F);
            }
            int count=age<QiEruption.GATHER?5:age<16?6:f.state.mode()==QiDomainSystem.MOTE?2:now%4==0?1:0;
            // Void Breaking's wind fragments belong to confirmed hits, never an idle open aura.
            if(look.art()==7)count=0;
            for(int i=0;i<count;i++){
                double a=world.random.nextDouble()*Math.PI*2,r=age<5?2.5:age<16?1.4:.5;
                Vec3 offset=new Vec3(Math.cos(a)*r,world.random.nextDouble()*1.6,Math.sin(a)*r);
                Vec3 point=p.add(offset);
                if(f.state.mode()==QiDomainSystem.BODY&&e instanceof LivingEntity living)point=deformBodyPoint(living,point,1);
                Vec3 velocity=age<5?p.add(0,.8,0).subtract(point).scale(.22):new Vec3(Math.cos(a)*.04,.10,Math.sin(a)*.04);
                if(look.art()==6&&age>=5)velocity=p.add(0,.8,0).subtract(point).scale(.16);
                if(look.art()==5&&age>=5) {
                    int direction=(i+now)%2==0?1:-1;
                    velocity=new Vec3(-Math.sin(a)*.075*direction,.055,Math.cos(a)*.075*direction);
                }
                if(age>=5&&f.state.mode()==QiDomainSystem.BODY)velocity=velocity.add(e.getDeltaMovement().scale(-.10));
                QiEnergyParticles.energy(point,velocity,look.art()==5?ReincarnationVisuals.particle((i+now)%2==0):look,age<16?8:10);
                if(age<16)QiEnergyParticles.debris(new Vec3(point.x,p.y+.07,point.z),velocity,world.getBlockState(BlockPos.containing(p).below()));
            }
            if(look.art()!=7&&f.state.mode()==QiDomainSystem.BODY&&f.state.percent()==200&&now%3==0){
                double a=world.random.nextDouble()*Math.PI*2,r=Math.sqrt(100+world.random.nextDouble()*(QiDomainSystem.DOMAIN_RADIUS*QiDomainSystem.DOMAIN_RADIUS-100));
                Vec3 point=p.add(Math.cos(a)*r,world.random.nextDouble()*3,Math.sin(a)*r);
                Vec3 velocity=look.art()==6?p.add(0,.9,0).subtract(point).normalize().scale(.32):new Vec3(-Math.sin(a)*.03,.025,Math.cos(a)*.03);
                if(look.art()==5&&now%2==0)velocity=velocity.multiply(-1,1,-1);
                QiEnergyParticles.energy(point,velocity,look.art()==5?ReincarnationVisuals.particle(now%2==0):look,12);
            }
        }
        for(Launch launch:LAUNCHES){
            var e=world.getEntity(launch.id);if(e==null||!e.getUUID().equals(launch.uuid))continue;
            Vec3 velocity=e.position().subtract(new Vec3(e.xo,e.yo,e.zo));
            if(velocity.lengthSqr()>.002&&velocity.lengthSqr()<16)
                QiEnergyParticles.energy(e.getBoundingBox().getCenter(),velocity.scale(-.16),launch.look,6);
        }
    }
    @SubscribeEvent public static void frame(RenderFrameEvent.Pre event){QiVfxQuality.beginFrame();IceCoatingClient.beginFrame();BloodLilyClient.beginFrame();ReincarnationEchoClient.beginFrame();}
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES||world==null||ShaderPackCompatibility.isRenderingShadowPass())return;
        var mc=Minecraft.getInstance();if(mc.player==null)return;
        var pose=event.getPoseStack();var buffers=mc.renderBuffers().bufferSource();Vec3 camera=event.getCamera().getPosition();
        float partial=event.getPartialTick().getGameTimeDeltaPartialTick(false);double time=world.getGameTime()+partial;
        int actors=0;rendered=0;woodMotes=0;
        var ordered=new ArrayList<>(FLOWS.values());
        ordered.sort(Comparator.comparingDouble(f->{var e=world.getEntity(f.state.entityId());return e==null?Double.MAX_VALUE:e.distanceToSqr(camera);}));
        for(Flow f:ordered){
            var entity=world.getEntity(f.state.entityId());
            if(!(entity instanceof LivingEntity living)||entity.isInvisible()||entity.distanceToSqr(camera)>80*80||++actors>QiVfxQuality.actors())continue;
            var look=look(f);Vec3 p=entity.getPosition(partial);
            double age=f.ignited<0?-1:time-f.ignited;
            float fade=(float)Math.clamp(f.stopped>=0?1-(time-f.stopped)/6:1-Math.max(0,time-f.until)/6,0,1);
            if(f.state.mode()==QiDomainSystem.BODY){
                float amplitude=QiEruption.amplitude(age),release=1+(f.state.percent()-100)*.001F;
                // Keep the broad eruption, but limit vertical stretch for players and NPCs alike.
                float eruptionBlend=Math.clamp((amplitude-1)/1.2F,0,1);
                float bodyScale=net.minecraft.util.Mth.lerp(eruptionBlend,1.2F,2.2F);
                float height=QiEruption.bodyHeight(living.getDimensions(Pose.STANDING).height(),f.state.percent(),age);
                float radius=Math.max(.6F,living.getBbWidth()*.95F)*release*bodyScale;
                f.renderedHeight=height;
                float attack=(float)Math.sin(living.getAttackAnim(partial)*Math.PI);
                f.bodyFrame=f.body.snapshot(partial);
                Vec3 base=bodyBase(living,p,height,partial);
                Vec3 center=base.add(0,height*.45,0).add(f.bodyFrame.offset(.65));
                Vec3 axis=bodyAxis(entity.getUUID());
                // The local first-person camera is inside its aura: show hands, ground pressure and distortion without a screen-sized shell.
                if(entity!=mc.getCameraEntity()||!mc.options.getCameraType().isFirstPerson()){
                    pose.pushPose();pose.translate(base.x-camera.x,base.y-camera.y,base.z-camera.z);
                    rendered+=QiEnergyMesh.drawBody(pose,buffers,look,time+entity.getId()*7,attack,fade*QiEruption.opacity(age),radius,height,f.bodyFrame);
                    pose.popPose();
                }
                var out=buffers.getBuffer(QiAuraMaterial.GLOW);
                QiMethodVisuals.aura(out,pose.last().pose(),living,p,camera,look,time,fade);
                boolean rings=bodyRingsVisible(living,look.art());
                if(age>=0&&age<5)for(int j=0;j<6;j++){
                    double a=time*.5+j*Math.PI/3;
                    Vec3 from=p.add(Math.cos(a)*1.1,.1,Math.sin(a)*1.1),to=p.add(Math.cos(a+.4)*.7,1.7,Math.sin(a+.4)*.7);
                    QiPressureMesh.ribbon(out,pose.last().pose(),from,to,camera,.014,look.highlight(),.5F*fade);
                }
                if(rings&&age>=5&&age<30)for(int j=0;j<3;j++){
                    double t=age-5-j*3;if(t>=0&&t<15)QiPressureMesh.wave(out,pose.last().pose(),p.add(0,.10+j*.06,0),new Vec3(0,1,0),camera,.6+t*.55,(float)(.55*(1-t/15))*fade,look.color());
                }
                double pulse=(time+entity.getId()*17)%95;
                if(rings&&pulse<12)QiPressureMesh.wave(out,pose.last().pose(),p.add(0,.12,0),new Vec3(0,1,0),camera,.8+pulse*.22,(float)(.17*(1-pulse/12))*fade,look.color());
                if(f.state.percent()==200){
                    DomainClashVisuals.domain(out,pose.last().pose(),living,camera,partial,time,look.color(),look.highlight(),fade);
                    var domainVolume=DomainClashVisuals.volume(living,partial);
                    if(f.state.mastered()&&f.state.power()>0) {
                        QiPressureMesh.clipDomain(domainVolume);
                        try {QiMethodVisuals.domain(out,pose.last().pose(),living,p,camera,look,time,fade*f.state.power());}
                        finally {QiPressureMesh.clipDomain(null);}
                    }
                    if(f.state.mastered()&&f.state.power()>0&&look.family()==QiElementLook.WOOD){
                        // Use the same moving pollen positions as server contact tests, not random ambient leaves.
                        long tick=world.getGameTime();
                        Vec3 movement=p.subtract(living.position());
                        for(int i=0;i<QiDomainSystem.MOTES;i++){
                            Vec3 point=QiDomainSystem.pollenPosition(living,i,tick)
                                    .lerp(QiDomainSystem.pollenPosition(living,i,tick+1),partial).add(movement);
                            Vec3 tail=QiDomainSystem.pollenPosition(living,i,tick-6).add(movement);
                            if(!domainVolume.intersects(new net.minecraft.world.phys.AABB(point,point).inflate(.35)))continue;
                            if(QiEnergyParticles.woodMote(pose,buffers,camera,point,tail,look,time+i*7,fade))woodMotes++;
                        }
                    }
                }
                // Pressure keeps bending the scene after the initial eruption, and grows at 200%.
                float warpStrength=(f.state.percent()==200?.95F:.65F)*Math.min(1.3F,amplitude)*fade;
                if(look.art()==13)warpStrength*=.20F+f.state.sunEnergy()*.008F;
                QiDomainDistortion.observe(center,warpStrength,height*1.42F,axis);
            }else if(f.state.mode()==QiDomainSystem.MOTE){
                double pulse=(time+entity.getId())%15;
                if(bodyRingsVisible(living,look.art()))QiPressureMesh.wave(buffers.getBuffer(QiAuraMaterial.GLOW),pose.last().pose(),p.add(0,.8,0),new Vec3(0,1,0),camera,
                        .25+pulse*.04,(float)(.12*(1-pulse/15))*fade,look.color());
            }
            if(f.state.mode()==QiDomainSystem.FIST||f.state.attackMode()==QiDomainSystem.FIST)
                QiMethodVisuals.aura(buffers.getBuffer(QiAuraMaterial.GLOW),pose.last().pose(),living,p,camera,look,time,fade);
        }
        for(Impact impact:IMPACTS){
            double age=time-impact.start;if(age<0||age>=20)continue;
            var target=world.getEntity(impact.id);Vec3 point=impact.point;
            if(impact.id>=0){
                if(target==null||!target.getUUID().equals(impact.uuid))continue;
                point=target.getPosition(partial).add(impact.offset.yRot((float)Math.toRadians(-target.getYRot())));
            }
            float alpha=(float)(1-age/20);
            if(QiMethodVisuals.impact(buffers.getBuffer(QiAuraMaterial.GLOW),pose.last().pose(),point,
                    impact.direction,camera,impact.look,age,impact.power))continue;
            pose.pushPose();pose.translate(point.x-camera.x,point.y-camera.y-.15,point.z-camera.z);
            float size=.28F+impact.power*.20F;
            pose.scale(size,(.9F+impact.power*.6F)*(float)(.7+Math.sin(Math.min(age,5)/5*Math.PI)*.3),size);
            rendered+=QiEnergyMesh.draw(pose,buffers,impact.look,time,impact.power,alpha,Vec3.ZERO,true);pose.popPose();
            if(age<7)QiPressureMesh.wave(buffers.getBuffer(QiAuraMaterial.GLOW),pose.last().pose(),point,impact.direction,camera,
                    .1+age*.16,(float)(.65*(1-age/7)),impact.look.highlight());
        }
        for(Launch launch:LAUNCHES){
            var e=world.getEntity(launch.id);if(e==null||!e.getUUID().equals(launch.uuid))continue;
            Vec3 d=e.position().subtract(new Vec3(e.xo,e.yo,e.zo));if(d.lengthSqr()>9)continue;
            Vec3 p=e.getPosition(partial).add(0,e.getBbHeight()*.5,0);
            QiPressureMesh.ribbon(buffers.getBuffer(QiAuraMaterial.GLOW),pose.last().pose(),p,p.subtract(d.scale(2)),camera,.06,launch.look.color(),.25F);
        }
        renderDomainTraces(pose,buffers,camera,time,partial);
        DomainClashVisuals.render(buffers.getBuffer(QiAuraMaterial.GLOW),pose.last().pose(),camera,time,partial);
        WoodBloomClient.render(pose,buffers,camera,time);
        QiEnergyParticles.render(pose,buffers,camera,time);
        buffers.endBatch(QiAuraMaterial.ENERGY);buffers.endBatch(QiAuraMaterial.HALO);buffers.endBatch(QiAuraMaterial.GLOW);
    }
    private static void renderDomainTraces(PoseStack pose,MultiBufferSource buffers,Vec3 camera,double time,float partial) {
        var out=buffers.getBuffer(QiAuraMaterial.GLOW);
        for(var trace:DOMAIN_TRACES) {
            var packet=trace.packet;var owner=world.getEntity(packet.ownerId());var victim=world.getEntity(packet.victimId());
            if(victim==null||!victim.getUUID().equals(packet.victimUuid()))continue;
            if((owner==null||!owner.getUUID().equals(packet.ownerUuid()))&&packet.kind()!=QiAuraPayloads.DomainEffect.REBIRTH)continue;
            double age=time-trace.start;int life=traceLifetime(packet.kind());
            if(age<0||age>=life||victim.distanceToSqr(camera)>64*64)continue;
            Vec3 from=victim.getPosition(partial).add(0,victim.getBbHeight()*.58,0),to=owner==null?from:owner.getPosition(partial).add(0,owner.getBbHeight()*.6,0);
            float fade=(float)Math.sin(age/24*Math.PI);
            var look=QiElementLook.of(MethodDomainStyle.values()[packet.style()]);
            if(packet.kind()==QiAuraPayloads.DomainEffect.ARC) {
                float flash=(float)(1-age/life);
                ElementalDomainVisuals.lightning(out,pose.last().pose(),to,from,camera,Math.floor(age/2)+trace.start,look,flash);
                QiPressureMesh.wave(out,pose.last().pose(),from,new Vec3(0,1,0),camera,.3+age*.10,flash*.30F,look.color());
            }else if(packet.kind()==QiAuraPayloads.DomainEffect.TIDE_RIPPLE) {
                ElementalDomainVisuals.waterRipple(out,pose.last().pose(),trace.origin,camera,look,packet.strength(),age);
            }else if(packet.kind()==QiAuraPayloads.DomainEffect.VITALITY) {
                AdditionalMethodVisuals.vitality(out,pose.last().pose(),from,to,camera,time,age,fade);
            }else if(packet.kind()>=QiAuraPayloads.DomainEffect.MOUNTAIN&&victim instanceof LivingEntity living) {
                float opacity=(float)Math.clamp((life-age)/5,0,1);
                ElementalDomainVisuals.status(out,pose.last().pose(),living,victim.getPosition(partial),camera,look,
                        packet.kind(),packet.strength(),age,time,opacity);
                AdditionalMethodVisuals.status(out,pose.last().pose(),living,victim.getPosition(partial),camera,look,
                        packet.kind(),packet.strength(),age,time,opacity);
                if(packet.kind()==QiAuraPayloads.DomainEffect.BONE)
                    QiDomainDistortion.observe(from.add(0,living.getBbHeight()*.2,0),.12F*opacity,living.getBbHeight(),new Vec3(0,-1,0));
                if(packet.kind()==QiAuraPayloads.DomainEffect.SOLAR)
                    QiDomainDistortion.observe(from,.8F*(packet.strength()/100F)*(float)Math.max(0,1-age/16),3);
                if(packet.kind()==QiAuraPayloads.DomainEffect.BLOOD_MARK)FoundationDomainVisuals.thorns(out,pose.last().pose(),living,victim.getPosition(partial),look,time,age,opacity);
                if(packet.kind()==QiAuraPayloads.DomainEffect.REBIRTH)FoundationDomainVisuals.rebirth(out,pose.last().pose(),living,victim.getPosition(partial),camera,time,age,opacity);
                if(packet.kind()==QiAuraPayloads.DomainEffect.WIND_LIFT)FoundationDomainVisuals.windLift(out,pose.last().pose(),living,victim.getPosition(partial),camera,age,opacity);
            }else if(packet.kind()==QiAuraPayloads.DomainEffect.WARP) {
                QiDomainDistortion.observeViolent(from,3.8F*fade,Math.max(4,victim.getBbHeight()*2.7F));
                FoundationDomainVisuals.devour(out,pose.last().pose(),from,camera,look,age,fade);
            }else {
                FoundationDomainVisuals.bloodFlow(out,pose.last().pose(),from,to,camera,look,age,fade,owner instanceof LivingEntity living?bloodMarks(living):0);
            }
        }
    }
    private static int traceLifetime(int kind) {
        if(kind==QiAuraPayloads.DomainEffect.TIDE_RIPPLE)return 32;
        if(kind==QiAuraPayloads.DomainEffect.REBIRTH)return 200;
        if(kind==QiAuraPayloads.DomainEffect.BLOOD_MARK)return 80;
        if(kind==QiAuraPayloads.DomainEffect.WIND_LIFT)return 18;
        return kind==QiAuraPayloads.DomainEffect.ARC?8:
                kind==QiAuraPayloads.DomainEffect.MOUNTAIN||kind==QiAuraPayloads.DomainEffect.FROST
                        ||kind==QiAuraPayloads.DomainEffect.BONE||kind==QiAuraPayloads.DomainEffect.WINTER
                        ||kind==QiAuraPayloads.DomainEffect.TIDE_WATER?30:24;
    }
}
