package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.network.XuanjinPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Vector3f;
import java.util.ArrayList;

@EventBusSubscriber(modid=ImmortalCultivationMod.MODID,value=Dist.CLIENT)
public final class XuanjinEffects {
    private static final DustParticleOptions ASH=new DustParticleOptions(new Vector3f(.13F,.12F,.15F),1.65F);
    private static final ArrayList<Burst> ACTIVE=new ArrayList<>();
    private static ClientLevel world;
    private static final class Burst {
        final XuanjinPayloads.Effect effect;int age;
        Burst(XuanjinPayloads.Effect effect){this.effect=effect;}
    }
    public static void accept(XuanjinPayloads.Effect effect) {
        var level=Minecraft.getInstance().level;
        if(level==null || !level.dimension().location().equals(effect.dimension()))return;
        if(world!=level){ACTIVE.clear();world=level;}
        if(ACTIVE.size()>=32)ACTIVE.removeFirst();
        ACTIVE.add(new Burst(effect));
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var mc=Minecraft.getInstance();
        if(mc.level!=world){ACTIVE.clear();world=mc.level;}
        if(world==null || mc.isPaused())return;
        ACTIVE.removeIf(b->{draw(b);return ++b.age>=Math.clamp(b.effect.duration(),1,120);});
    }
    private static void point(Vec3 p,boolean hot,int index) {
        // Full-bright colored sparks keep the shape readable at night; short lives avoid a foggy blob.
        var particle=Minecraft.getInstance().particleEngine.createParticle(index%7==0?ASH:ParticleTypes.END_ROD,p.x,p.y,p.z,0,.004,0);
        if(particle!=null) {
            particle.setLifetime(index%7==0?12:8);
            if(index%7!=0){particle.setColor(hot?1F:.18F,hot?.35F:.9F,hot?.12F:.78F);particle.scale(.8F);}
        }
        if(index%17==0)world.addParticle(ParticleTypes.SOUL_FIRE_FLAME,p.x,p.y,p.z,0,.012,0);
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES || world==null
                || world!=Minecraft.getInstance().level || ShaderPackCompatibility.isRenderingShadowPass())return;
        float partial=event.getPartialTick().getGameTimeDeltaPartialTick(false);
        for(var burst:ACTIVE)if(burst.effect.kind()==XuanjinPayloads.EffectKind.ROAR)
            XuanjinRoarRender.render(burst.effect,Math.max(0,burst.age-1)+partial,event);
    }
    private static void draw(Burst b) {
        var e=b.effect;double t=b.age/(double)Math.max(1,e.duration()-1);
        Vec3 start=e.origin(),end=e.target();
        var actor=world.getEntity(e.actorId());
        if(actor!=null && (e.kind()==XuanjinPayloads.EffectKind.VORTEX || e.kind()==XuanjinPayloads.EffectKind.DEVOUR))
            start=actor.position().add(0,actor.getBbHeight()*.65,0);
        Vec3 forward=end.subtract(start).normalize(),side=forward.cross(new Vec3(0,1,0)).normalize();
        if(side.lengthSqr()<.1)side=new Vec3(1,0,0);
        Vec3 up=side.cross(forward).normalize();
        switch(e.kind()) {
            case ROAR -> { /* The oriented sonic-boom cone is rendered between ticks. */ }
            case CLAW, BITE -> {
                if(t>.65)return;
                Vec3 center=start.lerp(end,e.kind()==XuanjinPayloads.EffectKind.CLAW?Math.min(1,t*2.5):.65);
                for(int claw=0;claw<3;claw++)for(int i=0;i<5;i++) {
                    double a=-1.2+(t+i*.018)*3.5;
                    Vec3 p=center.add(side.scale(Math.sin(a)*1.15+(claw-1)*.27)).add(up.scale(Math.cos(a)*.85-.3));
                    point(p,true,claw*5+i+b.age);
                }
            }
            case TAILS -> {
                for(int tail=0;tail<5;tail++) {
                    double travel=Math.clamp(t*1.7-tail*.12,0,1);
                    for(int i=0;i<4;i++) {
                        double u=Math.clamp(travel-i*.025,0,1);
                        Vec3 p=start.lerp(end,u).add(side.scale(Math.sin(u*Math.PI)*(tail-2)*.65))
                                .add(0,Math.sin(u*Math.PI)*(1.1+tail*.18),0);
                        point(p,true,tail*4+i+b.age);
                    }
                }
            }
            case VORTEX, DEVOUR, REFINE -> {
                double radius=e.kind()==XuanjinPayloads.EffectKind.VORTEX?2.6:1.6;
                for(int strand=0;strand<3;strand++)for(int i=0;i<6;i++) {
                    double u=(t*2+i*.045)%1,a=u*Math.PI*5+strand*Math.PI*2/3+b.age*.12,r=radius*(1-u);
                    Vec3 p=start.add(side.scale(Math.cos(a)*r)).add(up.scale(Math.sin(a)*r)).add(forward.scale(u*.7));
                    point(p,e.kind()==XuanjinPayloads.EffectKind.REFINE,strand*6+i+b.age);
                }
            }
            case DASH -> {
                for(int i=0;i<8;i++) {
                    double u=Math.clamp(t+i*.025,0,1);
                    point(start.lerp(end,u).add(side.scale(Math.sin(u*20)*.22)),false,i+b.age);
                }
            }
            default -> {
                Vec3 center=e.kind()==XuanjinPayloads.EffectKind.RETURN?end:start;
                double radius=.25+t*3;
                for(int i=0;i<24;i++) {
                    double a=i*Math.PI/12+b.age*.045;
                    Vec3 p=center.add(Math.cos(a)*radius,Math.sin(a*3+b.age*.2)*.12,Math.sin(a)*radius);
                    point(p,e.kind()==XuanjinPayloads.EffectKind.RETURN,i+b.age);
                    if(e.kind()==XuanjinPayloads.EffectKind.MANIFEST)point(p.add(0,t*2-1,0),false,i);
                }
            }
        }
    }
}
