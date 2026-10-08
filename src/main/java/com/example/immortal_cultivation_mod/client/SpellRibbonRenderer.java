package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.effect.SpellRibbonShapes;
import com.example.immortal_cultivation_mod.entity.*;
import com.example.immortal_cultivation_mod.network.SpellRibbonPayload;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/** Primary VFX for the thirty requested spells; previous particles provide the accents. */
@EventBusSubscriber(modid="immortal_cultivation_mod",value=Dist.CLIENT)
public final class SpellRibbonRenderer {
    private static ClientLevel level;
    private static final Map<Integer,ArrayDeque<Vec3>> TRAILS=new HashMap<>();
    private static final Map<String,Pulse> PULSES=new LinkedHashMap<>();
    private static long serial;
    private record Pulse(SpellRibbonPayload data,long born,long expires){}
    private static int quads;
    private record Surface(Vec3 a,Vec3 b,Vec3 c,Vec3 d,int color,float alpha){}
    private static final List<Surface> SURFACES=new ArrayList<>();
    private static final net.minecraft.client.renderer.MultiBufferSource.BufferSource SURFACE_BUFFER =
            net.minecraft.client.renderer.MultiBufferSource.immediate(new com.mojang.blaze3d.vertex.ByteBufferBuilder(65536));
    private static final net.minecraft.client.renderer.RenderType SURFACE_TYPE = net.minecraft.client.renderer.RenderType.create(
            "spell_rift_and_shield_surfaces",com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR,
            com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS,65536,false,true,
            net.minecraft.client.renderer.RenderType.CompositeState.builder()
                    .setShaderState(new net.minecraft.client.renderer.RenderStateShard.ShaderStateShard(net.minecraft.client.renderer.GameRenderer::getPositionColorShader))
                    .setTransparencyState(net.minecraft.client.renderer.RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(net.minecraft.client.renderer.RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(net.minecraft.client.renderer.RenderStateShard.COLOR_WRITE)
                    .setCullState(net.minecraft.client.renderer.RenderStateShard.NO_CULL)
                    .setOutputState(FilamentVfx.WORLD_TARGET).createCompositeState(false));
    private SpellRibbonRenderer(){}
    private static boolean use(ClientLevel current){
        if(current!=level){TRAILS.clear();PULSES.clear();level=current;}
        return current!=null;
    }
    public static void accept(SpellRibbonPayload p){
        if(!use(Minecraft.getInstance().level)||!p.dimension().equals(level.dimension().location())
                ||(!SpellRibbonShapes.SPELLS.contains(p.spell()) && !"fire_burst".equals(p.spell())))return;
        String key=p.entityId()>=0?p.spell()+":"+p.entityId():p.spell()+":"+(serial++);
        if(p.duration()<=0){PULSES.remove(key);return;}
        if(PULSES.size()>=128&&!PULSES.containsKey(key))return;
        Pulse old=PULSES.get(key);long now=level.getGameTime();
        PULSES.put(key,new Pulse(p,old==null?now:old.born,now+Math.min(80,p.duration())));
        if(old==null) SpellParticleAccents.burst(p.spell(),p.position(),p.direction(),p.radius());
    }
    public static String spell(Entity e){
        String id=null;
        if(e instanceof ElementalTechniqueAreaEntity a)id=a.kind().name().toLowerCase(Locale.ROOT);
        else if(e instanceof ElementalTechniqueProjectileEntity a)id=a.kind().name().toLowerCase(Locale.ROOT);
        else if(e instanceof SwordTechniqueProjectileEntity a)id=a.kind().name().toLowerCase(Locale.ROOT);
        else if(e instanceof JinmangChuanshaProjectileEntity)id="jinmang_chuansha";
        else if(e instanceof TianyunFenchengMeteorEntity)id="tianyun_fencheng";
        else if(e instanceof WindBladeProjectileEntity)id="wind_blade";
        else if(e instanceof LingzhiBulletProjectileEntity)id="lingzhi_bullet";
        else if(e instanceof LightBeamProjectileEntity)id="light_beam_attack";
        else if(e instanceof IgniteFlareProjectileEntity)id="ignite_flare";
        else if(e instanceof LightningProjectileEntity a)id=a.isGreater()?"wulei_zhengfa":"yinlei_jue";
        else if(e instanceof DingshenProjectileEntity)id="dingshen";
        else if(e instanceof YihenCiProjectileEntity)id="yihen_ci";
        else if(e instanceof XinliaoFireballEntity)id="jinhuo_xinliao";
        else if(e instanceof StarfallProjectileEntity)id="suigui_xingyun";
        else if(e instanceof DuanliuKongdunDomeEntity||e instanceof DuanliuKongdunProjectileEntity)id="duanliu_kongdun";
        else if(e instanceof YaoguangFlashEntity)id="yaoguang_shan";
        return SpellRibbonShapes.SPELLS.contains(id==null?"":id)?id:null;
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        var mc=Minecraft.getInstance();if(!use(mc.level)||mc.player==null||mc.isPaused())return;
        PULSES.values().removeIf(p->p.expires<=level.getGameTime());
        for(Pulse pulse:PULSES.values()) {
            var p=pulse.data; Entity owner=p.entityId()<0?null:level.getEntity(p.entityId());
            if(p.entityId()>=0&&(owner==null||!owner.isAlive())) continue;
            Vec3 anchor=owner==null?p.position():owner.position().add(0,owner.getBbHeight()*.5,0);
            SpellParticleAccents.trail(p.spell(),anchor,Vec3.ZERO,p.entityId(),p.radius());
        }
        Set<Integer> active=new HashSet<>();
        for(Entity e:level.entitiesForRendering()){
            if(active.size()>=128)break;
            if(e.isAlive() && e.distanceToSqr(mc.player)<=48*48) {
                String accentSpell=spell(e);
                if(e instanceof ZhenshanPalmEntity) accentSpell="zhenshan_palm";
                if(accentSpell!=null) SpellParticleAccents.trail(accentSpell,e.position(),e.getDeltaMovement(),e.getId(),.5);
            }
            if(!(e instanceof Projectile)||!e.isAlive()||e.distanceToSqr(mc.player)>112*112||spell(e)==null)continue;
            active.add(e.getId());var path=TRAILS.computeIfAbsent(e.getId(),key->new ArrayDeque<>());
            if(!path.isEmpty()&&path.getLast().distanceToSqr(e.position())>24*24)path.clear();
            path.addLast(e.position());while(path.size()>18)path.removeFirst();
        }
        TRAILS.keySet().retainAll(active);
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        // Terrain depth is still available here, including in Fabulous graphics.
        // AFTER_LEVEL runs after compositing, where that depth is no longer guaranteed.
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES||ShaderPackCompatibility.isRenderingShadowPass())return;
        var mc=Minecraft.getInstance();if(!use(mc.level)||mc.player==null)return;
        float partial=event.getPartialTick().getGameTimeDeltaPartialTick(false);
        var buffer=mc.renderBuffers().bufferSource();var out=buffer.getBuffer(FilamentVfx.TYPE);
        // The particle stage already applies the camera rotation in the shader's model-view
        // uniform. Apply only the local pose to camera-relative vertices, just like particles.
        Matrix4f matrix=event.getPoseStack().last().pose();Vec3 camera=event.getCamera().getPosition();quads=0;SURFACES.clear();
        for(Entity e:level.entitiesForRendering()){
            if(quads>42000)break;
            if(!e.isAlive()||e.position().distanceToSqr(camera)>112*112)continue;
            String id=spell(e);Vec3 pos=e.getPosition(partial);double age=e.tickCount+partial;
            if(id!=null){
                boolean moving=e instanceof Projectile;
                Vec3 dir=moving?e.getDeltaMovement():new Vec3(0,0,1);
                double scale=e instanceof ElementalTechniqueAreaEntity a&&a.fullyCharged()?1.3:1;
                // Trigger carriers must stay compact until their actual area or impact appears.
                if(e instanceof LightningProjectileEntity||e instanceof IgniteFlareProjectileEntity||e instanceof DingshenProjectileEntity)scale=.16;
                if(e instanceof DuanliuKongdunProjectileEntity)scale=.055;
                if(id.equals("tianfeng_zhui") && age>=44)continue;
                if(e instanceof TianyunFenchengMeteorEntity meteor&&meteor.impacted()){
                    id="fire_burst";age=meteor.impactAge()+partial;scale=Math.max(1,meteor.blastRadius()/3);
                }
                float intensity=1;
                if(e instanceof IgniteFlareProjectileEntity){scale=.10;intensity=.45F;}
                if(e instanceof ElementalTechniqueAreaEntity area && id.equals("tianguang_guansha")){
                    double warning=area.fullyCharged()?30:20;
                    if(age>=warning)intensity=(float)Mth.clamp(1-(age-warning-4)/4,0,1);
                    age=age<warning?age*20/warning:20+age-warning;
                }
                Vec3 origin=pos;
                if(e instanceof XinliaoFireballEntity){
                    Vec3 forward=dir.lengthSqr()<.00001?new Vec3(0,0,1):dir.normalize();
                    Vec3 right=(Math.abs(forward.y)>.95?new Vec3(1,0,0):new Vec3(0,1,0).cross(forward)).normalize();
                    origin=pos.subtract(forward.cross(right).normalize().scale(1.15*scale));
                }
                draw(out,matrix,camera,id,origin,dir,age,scale,intensity);
                if(moving)wake(out,matrix,camera,e,pos,id,partial);
            }
            if(e instanceof LivingEntity living){
                float opacity=e==mc.getCameraEntity()&&mc.options.getCameraType().isFirstPerson()?.23F:1;
                if(NativeSpellVfx.isMeditating(living))draw(out,matrix,camera,"meditating",pos,new Vec3(0,0,1),age,1,opacity);
                if(living.hasEffect(ModEffects.DINGSHEN))draw(out,matrix,camera,"dingshen",pos,new Vec3(0,0,1),age,1,opacity);
                if(living.hasEffect(ModEffects.MULING_SHENGXI))draw(out,matrix,camera,"muling_shengxi",pos,new Vec3(0,0,1),age,1,opacity);
                if(living.hasEffect(ModEffects.XUE_GUIYUAN_RECOVERY))draw(out,matrix,camera,"xue_guiyuan_shu",pos,new Vec3(0,0,1),age,1,opacity);
                if(living.hasEffect(ModEffects.SHIXU_CHILIU))draw(out,matrix,camera,"shixu_chiliu",pos,new Vec3(0,0,1),age,1,opacity);
            }
        }
        for(Pulse pulse:PULSES.values()){
            var p=pulse.data;Entity owner=p.entityId()<0?null:level.getEntity(p.entityId());
            if(p.entityId()>=0&&(owner==null||!owner.isAlive()))continue;
            Vec3 pos=owner==null?p.position():owner.getPosition(partial).add(p.spell().equals("yuangang_hujie")?new Vec3(0,owner.getBbHeight()*.5,0):Vec3.ZERO);
            if(pos.distanceToSqr(camera)>112*112)continue;
            double age=level.getGameTime()-pulse.born+partial;
            float fade=Math.min(1,(pulse.expires-level.getGameTime()-partial)/5F);
            // Compact flare pulses are cast/impact flashes; large meteor bursts keep their intensity.
            if(p.spell().equals("ignite_flare") && p.radius()<=.55F)fade*=.45F;
            draw(out,matrix,camera,p.spell(),pos,p.direction(),age,p.radius(),fade);
        }
        flushDetails(matrix,camera);
        buffer.endBatch(FilamentVfx.TYPE);
    }
    /** Also used by the isolated visual harness. */
    public static void draw(VertexConsumer out,Matrix4f matrix,Vec3 camera,String id,Vec3 origin,Vec3 direction,double age,double scale,float opacity){
        if(quads>42000)return;
        int color=SpellRibbonShapes.color(id);float r=(color>>16&255)/255F,g=(color>>8&255)/255F,b=(color&255)/255F;
        Vec3 forward=direction.lengthSqr()<.00001?new Vec3(0,0,1):direction.normalize();
        Vec3 right=(Math.abs(forward.y)>.95?new Vec3(1,0,0):new Vec3(0,1,0).cross(forward)).normalize();
        Vec3 up=forward.cross(right).normalize();
        var quality=Minecraft.getInstance().options.particles().get();int detail=quality==ParticleStatus.MINIMAL?12:quality==ParticleStatus.DECREASED?18:28;
        java.util.function.Function<Vec3,Vec3> world=p->origin.add(right.scale(p.x*scale)).add(up.scale(p.y*scale)).add(forward.scale(p.z*scale));
        Vec3 cameraRight=new Vec3(Minecraft.getInstance().gameRenderer.getMainCamera().rotation().transform(new org.joml.Vector3f(1,0,0)));
        Vec3 cameraUp=new Vec3(Minecraft.getInstance().gameRenderer.getMainCamera().rotation().transform(new org.joml.Vector3f(0,1,0)));
        SpellRibbonShapes.emit(id,age,new SpellRibbonShapes.Sink(){
            @Override public void segment(Vec3 a,Vec3 b,double width,float alpha){
                light(out,matrix,camera,world.apply(a),world.apply(b),width*Math.min(scale,2.5),r,g,SpellRibbonRenderer.blue(color),alpha*opacity);
            }
            @Override public void accent(Vec3 a,Vec3 b,double width,float alpha,int tint){
                light(out,matrix,camera,world.apply(a),world.apply(b),width*Math.min(scale,2.5),(tint>>16&255)/255F,(tint>>8&255)/255F,SpellRibbonRenderer.blue(tint),alpha*opacity);
            }
            @Override public void patch(Vec3 a,Vec3 b,Vec3 c,Vec3 d,int tint,float alpha){
                if(SURFACES.size()<2048)SURFACES.add(new Surface(world.apply(a),world.apply(b),world.apply(c),world.apply(d),tint,alpha*opacity));
            }
            @Override public void mote(Vec3 center,double radius,int tint,float alpha){
                if(quads>42000)return;
                Vec3 p=world.apply(center);double size=radius*Math.min(scale,2.5);
                float red=(tint>>16&255)/255F,green=(tint>>8&255)/255F,blue=SpellRibbonRenderer.blue(tint);
                quads+=FilamentVfx.halo(out,matrix,p,camera,cameraRight,cameraUp,size*4,red,green,blue,alpha*opacity*.16F);
                quads+=FilamentVfx.halo(out,matrix,p,camera,cameraRight,cameraUp,size,.45F+red*.55F,.45F+green*.55F,.45F+blue*.55F,alpha*opacity);
            }
        },detail);
    }
    private static float blue(int color){return (color&255)/255F;}
    public static void flushDetails(Matrix4f matrix,Vec3 camera){
        if(SURFACES.isEmpty())return;
        // Far-to-near alpha surfaces are composited before their additive rims.
        SURFACES.sort(java.util.Comparator.comparingDouble((Surface s)->s.a.distanceToSqr(camera)).reversed());
        var out=SURFACE_BUFFER.getBuffer(SURFACE_TYPE);
        for(var s:SURFACES)for(Vec3 point:List.of(s.a,s.b,s.c,s.d)){
            Vec3 p=point.subtract(camera);
            out.addVertex(matrix,(float)p.x,(float)p.y,(float)p.z).setColor((s.color>>16&255)/255F,(s.color>>8&255)/255F,blue(s.color),Mth.clamp(s.alpha,0,1));
        }
        SURFACE_BUFFER.endBatch(SURFACE_TYPE);SURFACES.clear();
    }

    private static void wake(VertexConsumer out,Matrix4f matrix,Vec3 camera,Entity e,Vec3 head,String id,float partial){
        var path=TRAILS.get(e.getId());if(path==null||path.size()<2)return;
        var points=new ArrayList<>(path);points.add(head);int color=SpellRibbonShapes.color(id);
        // Keep the ball silhouette dominant; only a short wake follows its spherical head.
        if(id.equals("light_beam_attack") && points.size()>3)points=new ArrayList<>(points.subList(points.size()-3,points.size()));
        float r=(color>>16&255)/255F,g=(color>>8&255)/255F,b=(color&255)/255F;
        for(int strand=0;strand<3;strand++)for(int i=1;i<points.size();i++){
            double u=(double)i/points.size();double phase=e.tickCount*.17+strand*2.1;
            Vec3 drift=new Vec3(Math.sin(phase+i*.4),Math.cos(phase+i*.3),0).scale(.11*(1-u));
            double width=id.equals("light_beam_attack")?.018:id.equals("ignite_flare")?.028:.055;
            float brightness=id.equals("ignite_flare")?.3F:.7F;
            light(out,matrix,camera,points.get(i-1).add(drift),points.get(i).add(drift),width*u,r,g,b,(float)(u*brightness));
        }
    }
    private static void light(VertexConsumer out,Matrix4f matrix,Vec3 camera,Vec3 a,Vec3 b,double width,float r,float g,float blue,float alpha){
        if(quads>42000)return;
        quads+=FilamentVfx.ribbon(out,matrix,a,b,camera,width*9,r,g,blue,alpha*.11F,alpha*.11F);
        quads+=FilamentVfx.ribbon(out,matrix,a,b,camera,width*3.3,r,g,blue,alpha*.36F,alpha*.36F);
        quads+=FilamentVfx.ribbon(out,matrix,a,b,camera,width,.4F+r*.6F,.4F+g*.6F,.4F+blue*.6F,alpha,alpha);
    }
}
