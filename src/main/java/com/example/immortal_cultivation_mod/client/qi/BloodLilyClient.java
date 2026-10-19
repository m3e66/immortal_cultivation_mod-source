package com.example.immortal_cultivation_mod.client.qi;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.client.ShaderPackCompatibility;
import com.example.immortal_cultivation_mod.network.QiAuraPayloads;
import com.mojang.blaze3d.vertex.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.event.GeoRenderEvent;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Textured, bent petal meshes rooted in the actor's actual animated model, never camera-facing flowers. */
@EventBusSubscriber(modid=ImmortalCultivationMod.MODID,value=Dist.CLIENT)
public final class BloodLilyClient {
    public static final ResourceLocation ATLAS=ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID,"textures/particle/blood_lily_atlas.png");
    private static final RenderType MATERIAL=RenderType.entityCutoutNoCull(ATLAS);
    private static final class Bloom {
        final int id;float previous,target;long changed,seen;
        Bloom(int id,long now){this.id=id;changed=seen=now;}
        float progress(double now){return previous+(target-previous)*(float)Math.clamp((now-changed)/20,0,1);}
    }
    private static final Map<UUID,Bloom> BLOOMS=new HashMap<>();
    private static ClientLevel world;
    private static int actors,remainingFlowers;
    public static long vanillaDraws,geoDraws,flowers,petalQuads,hiddenFirstPerson;
    private BloodLilyClient() { }
    public static void clear(){BLOOMS.clear();world=null;}
    public static void beginFrame(){actors=0;remainingFlowers=QiVfxQuality.level()==0?144:QiVfxQuality.level()==1?512:768;}
    public static int active(){return BLOOMS.size();}
    public static void accept(QiAuraPayloads.BloodLilyStatus packet) {
        var level=Minecraft.getInstance().level;if(level!=world){clear();world=level;}
        if(world==null||!world.dimension().location().equals(packet.dimension()))return;
        var entity=world.getEntity(packet.entityId());if(!(entity instanceof LivingEntity)||!entity.getUUID().equals(packet.uuid()))return;
        if(packet.progress()==0){BLOOMS.remove(packet.uuid());return;}
        if(!BLOOMS.containsKey(packet.uuid())&&BLOOMS.size()>=96)return;
        long now=world.getGameTime();var bloom=BLOOMS.computeIfAbsent(packet.uuid(),id->new Bloom(packet.entityId(),now));
        bloom.previous=bloom.progress(now);bloom.target=packet.progress()/255F;bloom.changed=bloom.seen=now;
    }
    public static void tick() {
        var level=Minecraft.getInstance().level;if(level!=world){clear();world=level;}
        if(world==null)return;
        BLOOMS.entrySet().removeIf(e->{var entity=world.getEntity(e.getValue().id);
            return entity==null||!entity.isAlive()||!entity.getUUID().equals(e.getKey())||world.getGameTime()-e.getValue().seen>60;});
    }
    public static boolean hidden(LivingEntity entity) {
        var mc=Minecraft.getInstance();return entity==mc.getCameraEntity()&&mc.options.getCameraType().isFirstPerson();
    }
    private static float visible(LivingEntity entity,float partial) {
        if(hidden(entity)){hiddenFirstPerson++;return 0;}
        var bloom=BLOOMS.get(entity.getUUID());
        if(world!=entity.level()||bloom==null||bloom.id!=entity.getId()||entity.isInvisible()
                ||ShaderPackCompatibility.isRenderingShadowPass()||actors>=QiVfxQuality.actors()||remainingFlowers<=0
                ||entity.distanceToSqr(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition())>48*48)return 0;
        float progress=bloom.progress(world.getGameTime()+partial);if(progress>0)actors++;return progress;
    }
    /** Captures the parent's transformed quads without drawing another skin. Surfaces move with every posed limb. */
    private record Surface(Vec3 point,Vec3 normal,Vec3 tangent,double seed,double priority) { }
    private static final class Sampler implements VertexConsumer {
        final LivingEntity entity;final float progress;final double feet,height;final VertexConsumer out;
        final PriorityQueue<Surface> surfaces=new PriorityQueue<>(Comparator.comparingDouble(Surface::priority).reversed());
        final double[] px=new double[4],py=new double[4],pz=new double[4];int vertex,face;
        Sampler(LivingEntity entity,float progress,float partial,MultiBufferSource buffers) {
            this.entity=entity;this.progress=progress;out=buffers.getBuffer(MATERIAL);
            feet=entity.getPosition(partial).y-Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().y;
            height=Math.max(.1,entity.getBbHeight());
        }
        @Override public VertexConsumer addVertex(float x,float y,float z){int i=vertex++%4;px[i]=x;py[i]=y;pz[i]=z;return this;}
        @Override public VertexConsumer setColor(int r,int g,int b,int a){return this;}
        @Override public VertexConsumer setUv(float u,float v){return this;}
        @Override public VertexConsumer setUv1(int u,int v){return this;}
        @Override public VertexConsumer setUv2(int u,int v){return this;}
        @Override public VertexConsumer setNormal(float x,float y,float z) {
            if(vertex%4!=0)return this;
            int index=face++;double ax=px[1]-px[0],ay=py[1]-py[0],az=pz[1]-pz[0];
            double bx=px[3]-px[0],by=py[3]-py[0],bz=pz[3]-pz[0];
            double cx=ay*bz-az*by,cy=az*bx-ax*bz,cz=ax*by-ay*bx;
            double area=Math.sqrt(cx*cx+cy*cy+cz*cz);if(area<.005||x*x+y*y+z*z<.25)return this;
            int samples=Math.clamp((int)Math.ceil(area/.045),1,16);Vec3 normal=null,tangent=null;
            for(int j=0;j<samples;j++) {
                double priority=hash(index*19+j*43+entity.getId()*13);
                // Deterministic bounded reservoir visits every bone, even beasts with 18,000 faces.
                if(surfaces.size()>=512&&priority>=surfaces.peek().priority)continue;
                if(surfaces.size()>=512)surfaces.poll();
                if(normal==null){normal=new Vec3(x,y,z).normalize();tangent=new Vec3(ax,ay,az).normalize();}
                double u=.12+.76*hash(index*31+j*73+entity.getId()),v=.12+.76*hash(index*13+j*59+17);
                Vec3 root=new Vec3(px[0]+ax*u+bx*v,py[0]+ay*u+by*v,pz[0]+az*u+bz*v);
                surfaces.add(new Surface(root,normal,tangent,index*2.399+j,priority));
            }
            return this;
        }
        void finish() {
            var selected=new ArrayList<>(surfaces);selected.sort(Comparator.comparingDouble(Surface::seed));
            int count=Math.min(remainingFlowers,Math.min(selected.size(),QiVfxQuality.level()==0?48:128));
            // Sample across the whole mesh, so detailed heads cannot spend every flower before legs render.
            for(int i=0;i<count;i++) {
                var surface=selected.get(i*selected.size()/count);
                double threshold=Math.clamp((surface.point.y-feet)/height*.86+hash(surface.seed)*.12,0,.98);
                double growth=progress>=.999F?1:Math.clamp((progress-threshold)*9,0,1);if(growth<=.015)continue;
                double bodyScale=Math.clamp(Math.sqrt(entity.getBbWidth()*entity.getBbHeight()/1.08),1,2.8);
                double size=(.14+hash(surface.seed*3)*.06)*growth*bodyScale;
                flower(out,surface.point.add(surface.normal.scale(.018)),surface.normal,surface.tangent,size,surface.seed,0xF000F0);flowers++;remainingFlowers--;
            }
        }
    }
    /** Six individually curved petals and outward arching stamens give each lily real depth from every view. */
    private static void flower(VertexConsumer out,Vec3 root,Vec3 n,Vec3 u,double size,double spin,int light) {
        Vec3 v=n.cross(u).normalize();
        for(int i=0;i<6;i++) {
            double angle=spin+i*Math.PI/3;Vec3 radial=u.scale(Math.cos(angle)).add(v.scale(Math.sin(angle)));
            Vec3 side=n.cross(radial).normalize();
            for(int k=0;k<3;k++) {
                double t=k/3.0,s=(k+1)/3.0;
                Vec3 a=petalPoint(root,n,radial,t,size),b=petalPoint(root,n,radial,s,size);
                double wa=size*(.10+.22*Math.sin(t*Math.PI)),wb=size*(.10+.22*Math.sin(s*Math.PI));
                // Atlas upper-left petal: root at its lower edge, recurved tip at its upper edge.
                quad(out,a.subtract(side.scale(wa)),a.add(side.scale(wa)),b.add(side.scale(wb)),b.subtract(side.scale(wb)),
                        0,.5F,(float)(.5*(1-s)),(float)(.5*(1-t)),n,light);
            }
        }
        for(int i=0;i<8;i++) {
            double angle=spin+.3+i*Math.PI/4;Vec3 radial=u.scale(Math.cos(angle)).add(v.scale(Math.sin(angle)));
            Vec3 side=n.cross(radial).normalize().scale(size*.18);
            Vec3 a=root.add(n.scale(size*.20)),b=root.add(radial.scale(size*1.15)).add(n.scale(size*.90));
            quad(out,a.subtract(side),a.add(side),b.add(side),b.subtract(side),.5F,1,0,.5F,n,light);
        }
        Vec3 core=root.add(n.scale(size*.14)),a=u.scale(size*.22),b=v.scale(size*.22);
        quad(out,core.subtract(a).subtract(b),core.add(a).subtract(b),core.add(a).add(b),core.subtract(a).add(b),.5F,1,.5F,1,n,light);
    }
    private static Vec3 petalPoint(Vec3 root,Vec3 n,Vec3 radial,double t,double size) {
        return root.add(radial.scale(size*(.10+t*.90))).add(n.scale(size*(.15+Math.sin(t*Math.PI)*.72-t*.15)));
    }
    private static void quad(VertexConsumer out,Vec3 a,Vec3 b,Vec3 c,Vec3 d,float u0,float u1,float v0,float v1,Vec3 normal,int light) {
        vertex(out,a,u0,v1,normal,light);vertex(out,b,u1,v1,normal,light);vertex(out,c,u1,v0,normal,light);vertex(out,d,u0,v0,normal,light);petalQuads++;
    }
    private static void vertex(VertexConsumer out,Vec3 p,float u,float v,Vec3 n,int light) {
        out.addVertex((float)p.x,(float)p.y,(float)p.z).setColor(255,255,255,(int)(255*QiVfxQuality.opacity())).setUv(u,v).setOverlay(0)
                .setLight(light).setNormal((float)n.x,(float)n.y,(float)n.z);
    }
    private static double hash(double x){double value=Math.sin(x*127.1+311.7)*43758.5453;return value-Math.floor(value);}
    public static final class Layer<T extends LivingEntity,M extends EntityModel<T>> extends RenderLayer<T,M> {
        public Layer(RenderLayerParent<T,M> parent){super(parent);}
        @Override public void render(PoseStack pose,MultiBufferSource buffers,int packed,T entity,float limb,float amount,float partial,float age,float yaw,float pitch) {
            if(entity instanceof GeoAnimatable)return;float progress=visible(entity,partial);if(progress<=0)return;
            var sampler=new Sampler(entity,progress,partial,buffers);
            getParentModel().renderToBuffer(pose,sampler,packed,0,0xFFFFFFFF);sampler.finish();vanillaDraws++;
        }
    }
    @SubscribeEvent @SuppressWarnings({"rawtypes","unchecked"}) public static void geo(GeoRenderEvent.Entity.Post event) {
        if(!(event.getEntity() instanceof LivingEntity entity)||!(entity instanceof GeoAnimatable animatable))return;
        float progress=visible(entity,event.getPartialTick());if(progress<=0)return;
        GeoEntityRenderer renderer=event.getRenderer();
        var sampler=new Sampler(entity,progress,event.getPartialTick(),event.getBufferSource());
        renderer.reRender(event.getModel(),event.getPoseStack(),event.getBufferSource(),animatable,MATERIAL,
                sampler,event.getPartialTick(),event.getPackedLight(),0,0xFFFFFFFF);sampler.finish();geoDraws++;
    }
}
