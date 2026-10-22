package com.example.immortal_cultivation_mod.client.curse;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.client.ShaderPackCompatibility;
import com.example.immortal_cultivation_mod.curse.AnnihilationCurse;
import com.example.immortal_cultivation_mod.curse.CursePayloads;
import com.example.immortal_cultivation_mod.physics.RuneSurfaceSwarm;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.event.GeoRenderEvent;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Ink is laid onto sampled posed faces, not floating cubes or camera-facing body sprites. */
@EventBusSubscriber(modid=ImmortalCultivationMod.MODID,value=Dist.CLIENT)
public final class CurseClient {
    public static final ResourceLocation RUNE_ATLAS=ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID,"textures/particle/annihilation_runes/fuwen_atlas.png");
    private static final RenderType INK=RenderType.entityTranslucent(RUNE_ATLAS);
    private static final int MAX_RUNES=1080;
    private static final int OUTBREAK_RUNES=480;
    private static final double SPACING_X=.08,SPACING_Y=.10;
    // Opaque bounds in each 64px Aseprite frame, generated with the five sprites.
    private static final int[][] RUNE_BOUNDS={{20,2,49,64},{5,2,62,63},{15,3,51,63},{13,2,54,60},{14,3,51,62}};
    private record Snapshot(int id,float previous,float target,long changed) {
        float value(double now){return previous+(target-previous)*(float)Math.clamp((now-changed)/20,0,1);}
    }
    private record GroundKey(int x,int y,int z) { }
    private static final class Outbreak {
        final CursePayloads.Outbreak packet;final long start;
        final Map<GroundKey,Double> ground=new HashMap<>();
        Outbreak(CursePayloads.Outbreak packet,long start){this.packet=packet;this.start=start;}
    }
    private static final Map<UUID,Snapshot> STATES=new HashMap<>();
    private static final class SurfaceState {
        final RuneSurfaceSwarm swarm;long seen;
        SurfaceState(double width,double height){swarm=new RuneSurfaceSwarm(width,height);}
    }
    private static final Map<UUID,Map<Integer,SurfaceState>> SURFACES=new HashMap<>();
    private static final List<Outbreak> OUTBREAKS=new ArrayList<>();
    private static ClientLevel world;
    private static int actors;
    private static long groundProbeTick=Long.MIN_VALUE;
    private static int groundProbes;
    private static boolean rerender;
    public static long modelDraws,geoDraws,runesDrawn,outbreakFrames,outerRunesDrawn,handOuterRunes,physicsCollisions,physicsWallBounces,outbreakCrawlRunes,outbreakAbsorbRunes;
    public static int spriteVariantsSeen,densestModelRunes,outerPartsSeen,densestOutbreakRunes,outbreakCrawlPartsSeen;
    private CurseClient() { }
    private static void use() {
        var level=Minecraft.getInstance().level;
        if(world!=level){world=level;STATES.clear();OUTBREAKS.clear();SURFACES.clear();}
    }
    public static void accept(CursePayloads.Status p) {
        use();if(world==null||!world.dimension().location().equals(p.dimension()))return;
        var entity=world.getEntity(p.entityId());if(!(entity instanceof LivingEntity living)||!entity.getUUID().equals(p.uuid()))return;
        var s=AnnihilationCurse.state(living);s.putFloat("Coverage",p.coverage());s.putFloat("BlackHp",p.blackHp());
        s.putBoolean("NoHealing",p.noHealing());s.putBoolean("Owned",p.owned());s.putInt("Cooldown",p.cooldown());
        var previous=STATES.get(p.uuid());long now=world.getGameTime();
        if(p.coverage()>0&&(previous!=null||STATES.size()<512))STATES.put(p.uuid(),new Snapshot(p.entityId(),previous==null?0:previous.value(now),p.coverage(),now));
        else if(p.coverage()<=0)STATES.remove(p.uuid());
    }
    public static void outbreak(CursePayloads.Outbreak p) {
        use();if(world!=null&&world.dimension().location().equals(p.dimension())&&OUTBREAKS.size()<32)OUTBREAKS.add(new Outbreak(p,world.getGameTime()));
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        use();if(world==null)return;
        STATES.entrySet().removeIf(e->{var actor=world.getEntity(e.getValue().id);return actor==null||!actor.getUUID().equals(e.getKey());});
        SURFACES.keySet().retainAll(STATES.keySet());
        long now=world.getGameTime();
        for(var actor:SURFACES.values()) {
            actor.values().removeIf(face->now-face.seen>40);
            for(var face:actor.values())if(now-face.seen<=2){long hits=face.swarm.collisions,walls=face.swarm.wallBounces;
                face.swarm.tick(now);physicsCollisions+=face.swarm.collisions-hits;physicsWallBounces+=face.swarm.wallBounces-walls;}
        }
        OUTBREAKS.removeIf(e->world.getGameTime()-e.start>e.packet.duration()+10);
        var p=Minecraft.getInstance().player;
        if(p!=null&&p.getPersistentData().contains(AnnihilationCurse.TAG)) {
            var s=AnnihilationCurse.state(p);s.putInt("Cooldown",Math.max(0,s.getInt("Cooldown")-1));
        }
    }
    @SubscribeEvent public static void frame(RenderFrameEvent.Pre event){actors=0;}
    private static boolean visible(LivingEntity entity) {
        return RuneInkMaterial.ready()&&!rerender&&!entity.isInvisible()&&actors<32&&!ShaderPackCompatibility.isRenderingShadowPass()
                &&world==entity.level()&&STATES.containsKey(entity.getUUID())
                &&entity.distanceToSqr(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition())<64*64;
    }
    private static boolean outbreakVisible(LivingEntity entity,float partial) {
        if(!RuneInkMaterial.ready()||rerender||entity.isInvisible()||actors>=32||world!=entity.level()
                ||ShaderPackCompatibility.isRenderingShadowPass()
                ||entity.distanceToSqr(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition())>=80*80)return false;
        double now=world.getGameTime()+partial;
        for(var outbreak:OUTBREAKS)if(outbreak.packet.targetId()==entity.getId()&&outbreak.packet.targetUuid().equals(entity.getUUID())
                &&now-outbreak.start<outbreak.packet.duration())return true;
        return false;
    }
    private static double outbreakPhase(double progress,int index) {
        // Stagger arrival without extending the saved server timer or leaving late runes behind.
        return Math.clamp((progress-index*.12/(OUTBREAK_RUNES-1))/.88,0,1);
    }
    private static double smooth(double value){value=Math.clamp(value,0,1);return value*value*(3-2*value);}
    public static long motionFingerprint(UUID actor) {
        long value=1;for(var face:SURFACES.getOrDefault(actor,Map.of()).entrySet())value+=face.getKey()*31L+face.getValue().swarm.fingerprint();return value;
    }
    private record Face(Vec3 a,Vec3 b,Vec3 c,Vec3 d,double[] uv,Vec3 normal,double area,int serial,boolean outer) { }
    private record Stamp(int seed,int variant,double width,double height,double cos,double sin) { }
    private static final class SurfaceCollector implements VertexConsumer {
        private final PriorityQueue<Face> faces=new PriorityQueue<>(Comparator.comparingDouble((Face f)->Math.rint(f.area*100000)).thenComparingInt(Face::serial));
        private final Vec3[] points=new Vec3[4];
        private final double[] uv=new double[8];
        private int count,serial;private Vec3 point;private double textureU,textureV;private boolean outer;
        void part(int index,boolean outer){serial=index<<20;this.outer=outer;}
        @Override public VertexConsumer addVertex(float x,float y,float z){point=new Vec3(x,y,z);return this;}
        @Override public VertexConsumer setColor(int r,int g,int b,int a){return this;}
        @Override public VertexConsumer setUv(float u,float v){textureU=u;textureV=v;return this;}
        @Override public VertexConsumer setUv1(int u,int v){return this;}
        @Override public VertexConsumer setUv2(int u,int v){return this;}
        @Override public VertexConsumer setNormal(float x,float y,float z){
            points[count]=point;uv[count*2]=textureU;uv[count*2+1]=textureV;count++;
            if(count==4){
                count=0;double area=points[1].subtract(points[0]).cross(points[3].subtract(points[0])).length();
                int index=serial++;
                if(area>.0005){var face=new Face(points[0],points[1],points[2],points[3],uv.clone(),new Vec3(x,y,z).normalize(),area,index,outer);faces.add(face);if(faces.size()>256)faces.poll();}
            }
            return this;
        }
        void draw(PoseStack pose,MultiBufferSource buffers,LivingEntity entity,float partial,ResourceLocation skin,boolean hand) {
            var snapshot=STATES.get(entity.getUUID());if(snapshot==null)return;
            float growth=snapshot.value(world.getGameTime()+partial);
            Vec3 camera=Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            Vec3 origin=entity.getPosition(partial).subtract(camera);int emitted=0,prepared=0;
            var out=buffers.getBuffer(RuneInkMaterial.of(skin));var matrix=new Matrix4f();
            var ordered=faces.stream().sorted(Comparator.comparingInt(Face::serial)).toList();
            int total=0;for(var face:ordered)total+=columns(face.b.subtract(face.a).length(),SPACING_X)*columns(face.d.subtract(face.a).length(),SPACING_Y);
            double density=total<=MAX_RUNES?1:MAX_RUNES*.88/total;
            int identity=entity.getUUID().hashCode();
            var actor=SURFACES.computeIfAbsent(entity.getUUID(),ignored->new HashMap<>());
            for(var f:ordered) {
                Vec3 u=f.b.subtract(f.a),v=f.d.subtract(f.a),normal=f.normal.scale(.0007);
                double lengthU=u.length(),lengthV=v.length();Vec3 axisU=u.normalize(),axisV=v.normalize();
                int columns=columns(lengthU,SPACING_X),rows=columns(lengthV,SPACING_Y);
                var stamps=new ArrayList<Stamp>();var seeds=new ArrayList<RuneSurfaceSwarm.Seed>();
                for(int x=0;x<columns;x++)for(int y=0;y<rows;y++) {
                    int seed=mix(identity^f.serial*73856093^x*19349663^y*83492791);
                    if(sample(seed+19)>density||prepared>=MAX_RUNES)continue;
                    int variant=Math.floorMod(mix(seed+37),5);var bounds=RUNE_BOUNDS[variant];
                    double angle=(sample(seed+53)-.5)*.78,cos=Math.cos(angle),sin=Math.sin(angle);
                    double height=lengthV/rows*(.76+sample(seed+71)*.12);
                    double aspect=Math.clamp((bounds[2]-bounds[0])/(double)Math.max(1,bounds[3]-bounds[1]),.60,1.12);
                    double width=Math.min(lengthU/columns*.84,height*aspect);
                    // Fit the entire rotated quad, including narrow custom hair or limb faces.
                    double fit=Math.min(1,Math.min(lengthU*.96/(Math.abs(cos)*width+Math.abs(sin)*height),lengthV*.96/(Math.abs(sin)*width+Math.abs(cos)*height)));
                    width*=fit;height*=fit;
                    double radiusU=Math.min(lengthU*.49,(Math.abs(cos)*width+Math.abs(sin)*height)*.5);
                    double radiusV=Math.min(lengthV*.49,(Math.abs(sin)*width+Math.abs(cos)*height)*.5);
                    double atU=Math.clamp((x+.5+(sample(seed+97)-.5)*.42+((y&1)==0?-.10:.10))/columns,radiusU/lengthU,1-radiusU/lengthU);
                    double atV=Math.clamp((y+.5+(sample(seed+113)-.5)*.38)/rows,radiusV/lengthV,1-radiusV/lengthV);
                    Vec3 initial=f.a.add(u.scale(atU)).add(v.scale(atV));
                    double body=(initial.y-origin.y)/Math.max(.1,entity.getBbHeight());
                    double chestDistance=Math.abs(body-.68)*1.35+initial.subtract(origin).horizontalDistance()/Math.max(.2,entity.getBbWidth())*.10;
                    if(hand){if(growth<.25F||sample(seed+131)>(growth-.25F)/.75F)continue;}
                    else if(growth<.999F&&chestDistance>growth*.98+.02)continue;
                    double heading=sample(seed+149)*Math.PI*2,speed=.0035+sample(seed+163)*.0035;
                    seeds.add(new RuneSurfaceSwarm.Seed(seed,atU*lengthU,atV*lengthV,radiusU,radiusV,Math.cos(heading)*speed,Math.sin(heading)*speed));
                    stamps.add(new Stamp(seed,variant,width,height,cos,sin));prepared++;
                }
                var state=actor.computeIfAbsent(f.serial,ignored->new SurfaceState(lengthU,lengthV));state.seen=world.getGameTime();state.swarm.sync(lengthU,lengthV,seeds);
                for(var stamp:stamps) {
                    var body=state.swarm.sample(stamp.seed,partial);if(body==null)continue;
                    double atU=body.x()/lengthU,atV=body.y()/lengthV;
                    Vec3 center=f.a.add(u.scale(atU)).add(v.scale(atV)).add(normal);
                    Vec3 across=axisU.scale(stamp.cos*stamp.width).add(axisV.scale(stamp.sin*stamp.width));
                    Vec3 along=axisU.scale(-stamp.sin*stamp.height).add(axisV.scale(stamp.cos*stamp.height));
                    skinGlyph(out,matrix,center,across,along,stamp.variant,f,atU,atV,stamp.cos*stamp.width/lengthU,stamp.sin*stamp.width/lengthV,-stamp.sin*stamp.height/lengthU,stamp.cos*stamp.height/lengthV);
                    emitted++;runesDrawn++;if(f.outer){outerRunesDrawn++;outerPartsSeen|=1<<((f.serial>>>20)-6);if(hand)handOuterRunes++;}
                }
            }
            densestModelRunes=Math.max(densestModelRunes,emitted);
        }
        private static int columns(double length,double size){return Math.clamp((int)Math.ceil(Math.rint(length*1000)/1000/size),1,12);}
        void drawOutbreak(MultiBufferSource buffers,LivingEntity entity,float partial,ResourceLocation skin) {
            // Vanilla cube face 3 is the anatomical front. These vertices already include the
            // actual walking/crouching pose, body yaw, model scale and outer clothing deformation.
            Face[] front=new Face[12];for(var face:faces)if((face.serial&0xFFFFF)==3&&(face.serial>>>20)<front.length)front[face.serial>>>20]=face;
            var out=buffers.getBuffer(RuneInkMaterial.of(skin));var identity=new Matrix4f();
            double now=world.getGameTime()+partial;
            for(var outbreak:OUTBREAKS) {
                var packet=outbreak.packet;
                if(packet.targetId()!=entity.getId()||!packet.targetUuid().equals(entity.getUUID()))continue;
                double progress=Math.clamp((now-outbreak.start)/packet.duration(),0,1);
                for(int i=0;i<OUTBREAK_RUNES;i++) {
                    double phase=outbreakPhase(progress,i);if(phase<.48||phase>=1)continue;
                    int seed=mix(i*73856093+packet.targetUuid().hashCode()),leg=(i&1)==0?4:5;
                    double crawl=Math.pow(Math.clamp((phase-.48)/.40,0,1),.7+sample(seed+17)*.8),sink=smooth((phase-.88)/.12);
                    double lane=.5+(sample(seed+31)-.5)*.70;
                    double wander=Math.sin(crawl*17+sample(seed+43)*Math.PI*2)*.065*(1-sink);
                    double u=Math.clamp(lane+wander,.12,.88),v;
                    int part;
                    if(crawl<.56){part=leg;v=.96-.93*smooth(crawl/.56);}
                    else {part=1;v=.97-.66*smooth((crawl-.56)/.44);}
                    if(part==1) {
                        // Each glyph curls along its own rounded route, never filling a rectangular
                        // chest panel. The spiral contracts continuously into one entry point.
                        double approach=smooth((crawl-.65)/.35);
                        double turn=sample(seed+101)*Math.PI*2+(approach*.65+sink*.35)*Math.PI*2;
                        double radius=(.10+.20*Math.sqrt(sample(seed+113)))*(1-sink);
                        u=u*(1-approach)+(.5+Math.cos(turn)*radius)*approach;
                        v=v*(1-approach)+(.30+Math.sin(turn)*radius*.67)*approach;
                    }
                    double angle=Math.sin(crawl*11+seed)*.32,size=(.048+sample(seed+79)*.018)*(1-.94*sink);
                    float alpha=(float)(1-sink*sink);
                    boolean emitted=false;
                    // Paint both skin layers with the real texture's alpha mask: bare skin remains
                    // visible through transparent clothing pixels, without ink hovering in holes.
                    for(int layer=0;layer<2;layer++) {
                        var face=front[part+layer*6];if(face==null)continue;
                        Vec3 a=face.b.subtract(face.a),b=face.d.subtract(face.a);double width=a.length(),height=b.length();
                        double glyphH=Math.min(size,height*.7),glyphW=Math.min(size*.68,width*.7);
                        double cos=Math.cos(angle),sin=Math.sin(angle);
                        double radiusU=(Math.abs(cos)*glyphW+Math.abs(sin)*glyphH)/(2*width)+.002;
                        double radiusV=(Math.abs(sin)*glyphW+Math.abs(cos)*glyphH)/(2*height)+.002;
                        double atU=Math.clamp(u,radiusU,1-radiusU),atV=Math.clamp(v,radiusV,1-radiusV);
                        Vec3 across=a.normalize().scale(cos*glyphW).add(b.normalize().scale(sin*glyphW));
                        Vec3 along=a.normalize().scale(-sin*glyphH).add(b.normalize().scale(cos*glyphH));
                        Vec3 center=face.a.add(a.scale(atU)).add(b.scale(atV));
                        if(part==1&&crawl<.66&&front[leg+layer*6]!=null) {
                            var previous=front[leg+layer*6];Vec3 hip=previous.a.add(previous.b.subtract(previous.a).scale(u)).add(previous.d.subtract(previous.a).scale(.03));
                            center=hip.lerp(center,smooth((crawl-.56)/.10));
                        }
                        center=center.add(face.normal.scale(.004-.025*smooth((sink-.75)/.25)));
                        skinGlyph(out,identity,center,across,along,i,face,atU,atV,cos*glyphW/width,sin*glyphW/height,-sin*glyphH/width,cos*glyphH/height,alpha);
                        emitted=true;
                    }
                    if(emitted){outbreakCrawlRunes++;outbreakCrawlPartsSeen|=part==4?1:part==5?2:4;if(phase>=.88)outbreakAbsorbRunes++;}
                }
            }
        }
    }
    public static final class Layer<T extends LivingEntity,M extends EntityModel<T>> extends RenderLayer<T,M> {
        public Layer(RenderLayerParent<T,M> parent){super(parent);}
        @Override public void render(PoseStack pose,MultiBufferSource buffers,int light,T entity,float limb,float amount,float partial,float age,float yaw,float pitch) {
            if(entity instanceof GeoAnimatable)return;
            boolean cursed=visible(entity),crawling=outbreakVisible(entity,partial)&&getParentModel() instanceof net.minecraft.client.model.PlayerModel<?>;
            if(!cursed&&!crawling)return;
            actors++;var surface=new SurfaceCollector();
            if(getParentModel() instanceof net.minecraft.client.model.PlayerModel<?> model) {
                var parts=List.of(model.head,model.body,model.rightArm,model.leftArm,model.rightLeg,model.leftLeg,
                        model.hat,model.jacket,model.rightSleeve,model.leftSleeve,model.rightPants,model.leftPants);
                for(int i=0;i<parts.size();i++){surface.part(i,i>=6);parts.get(i).render(pose,surface,light,0,0xFFFFFFFF);}
            }else getParentModel().renderToBuffer(pose,surface,light,0,0xFFFFFFFF);
            if(cursed){surface.draw(pose,buffers,entity,partial,getTextureLocation(entity),false);modelDraws++;}
            if(crawling)surface.drawOutbreak(buffers,entity,partial,getTextureLocation(entity));
        }
    }
    public static void hand(PoseStack pose,MultiBufferSource buffers,LivingEntity player,
                            net.minecraft.client.model.geom.ModelPart arm,net.minecraft.client.model.geom.ModelPart sleeve,boolean right,ResourceLocation skin,int light,float partial) {
        if(!visible(player))return;
        var surface=new SurfaceCollector();surface.part(right?2:3,false);arm.render(pose,surface,light,0,0xFFFFFFFF);
        surface.part(right?8:9,true);sleeve.render(pose,surface,light,0,0xFFFFFFFF);
        surface.draw(pose,buffers,player,partial,skin,true);
    }
    @SubscribeEvent @SuppressWarnings({"rawtypes","unchecked"}) public static void geo(GeoRenderEvent.Entity.Post event) {
        if(!(event.getEntity() instanceof LivingEntity entity)||!(entity instanceof GeoAnimatable animatable)||!visible(entity))return;
        actors++;var surface=new SurfaceCollector();GeoEntityRenderer renderer=event.getRenderer();
        rerender=true;
        try{renderer.reRender(event.getModel(),event.getPoseStack(),event.getBufferSource(),animatable,
                net.minecraft.client.renderer.RenderType.entityCutoutNoCull(renderer.getTextureLocation(entity)),surface,event.getPartialTick(),event.getPackedLight(),0,0xFFFFFFFF);}
        finally{rerender=false;}
        surface.draw(event.getPoseStack(),event.getBufferSource(),entity,event.getPartialTick(),renderer.getTextureLocation(entity),false);geoDraws++;
    }
    private static int mix(int n){n=(n^(n>>>16))*0x7feb352d;n=(n^(n>>>15))*0x846ca68b;return n^(n>>>16);}
    private static double sample(int n){return (mix(n)&0xffffff)/16777216.;}
    private static void glyph(VertexConsumer out,Matrix4f matrix,Vec3 center,Vec3 u,Vec3 v,int index,float alpha) {
        int variant=Math.floorMod(index,5);var b=RUNE_BOUNDS[variant];spriteVariantsSeen|=1<<variant;
        float u0=(variant*64+b[0])/320F,u1=(variant*64+b[2])/320F,v0=b[1]/64F,v1=b[3]/64F;
        Vec3 halfU=u.scale(.5),halfV=v.scale(.5),normal=u.cross(v).normalize();
        Vec3[] points={center.subtract(halfU).subtract(halfV),center.add(halfU).subtract(halfV),center.add(halfU).add(halfV),center.subtract(halfU).add(halfV)};
        float[][] uv={{u0,v1},{u1,v1},{u1,v0},{u0,v0}};
        for(int i=0;i<4;i++)out.addVertex(matrix,(float)points[i].x,(float)points[i].y,(float)points[i].z)
                .setColor(255,255,255,Math.round(255*alpha)).setUv(uv[i][0],uv[i][1]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0)
                .setNormal((float)normal.x,(float)normal.y,(float)normal.z);
    }
    private static void skinGlyph(VertexConsumer out,Matrix4f matrix,Vec3 center,Vec3 across,Vec3 along,int index,Face face,
                                  double u,double v,double acrossU,double acrossV,double alongU,double alongV) {
        skinGlyph(out,matrix,center,across,along,index,face,u,v,acrossU,acrossV,alongU,alongV,1);
    }
    private static void skinGlyph(VertexConsumer out,Matrix4f matrix,Vec3 center,Vec3 across,Vec3 along,int index,Face face,
                                  double u,double v,double acrossU,double acrossV,double alongU,double alongV,float alpha) {
        int variant=Math.floorMod(index,5);var bounds=RUNE_BOUNDS[variant];spriteVariantsSeen|=1<<variant;
        float u0=(variant*64+bounds[0])/320F,u1=(variant*64+bounds[2])/320F,v0=bounds[1]/64F,v1=bounds[3]/64F;
        float[][] sprite={{u0,v1},{u1,v1},{u1,v0},{u0,v0}};int[][] signs={{-1,-1},{1,-1},{1,1},{-1,1}};
        for(int i=0;i<4;i++) {
            double a=signs[i][0]*.5,b=signs[i][1]*.5,atU=u+a*acrossU+b*alongU,atV=v+a*acrossV+b*alongV;
            double skinU=(face.uv[0]*(1-atU)+face.uv[2]*atU)*(1-atV)+(face.uv[6]*(1-atU)+face.uv[4]*atU)*atV;
            double skinV=(face.uv[1]*(1-atU)+face.uv[3]*atU)*(1-atV)+(face.uv[7]*(1-atU)+face.uv[5]*atU)*atV;
            Vec3 point=center.add(across.scale(a)).add(along.scale(b));
            out.addVertex(matrix,(float)point.x,(float)point.y,(float)point.z).setColor(255,255,255,Math.round(255*alpha)).setUv(sprite[i][0],sprite[i][1])
                    .setUv1((int)Math.round(Math.clamp(skinU,0,1)*32767),(int)Math.round(Math.clamp(skinV,0,1)*32767)).setLight(0xF000F0)
                    .setNormal((float)face.normal.x,(float)face.normal.y,(float)face.normal.z);
        }
    }
    @SubscribeEvent public static void renderOutbreak(RenderLevelStageEvent event) {
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES||world==null||ShaderPackCompatibility.isRenderingShadowPass())return;
        var buffers=Minecraft.getInstance().renderBuffers().bufferSource();var out=buffers.getBuffer(INK);
        var m=event.getPoseStack().last().pose();Vec3 camera=event.getCamera().getPosition();
        double time=world.getGameTime()+event.getPartialTick().getGameTimeDeltaPartialTick(true);
        for(var eruption:OUTBREAKS) {
            var p=eruption.packet;var target=world.getEntity(p.targetId());
            if(target==null||!target.getUUID().equals(p.targetUuid()))continue;
            double age=time-eruption.start,progress=Math.clamp(age/p.duration(),0,1);
            if(!(target instanceof LivingEntity living))continue;
            Vec3 start=new Vec3(p.x(),p.y(),p.z());
            float partial=event.getPartialTick().getGameTimeDeltaPartialTick(true);
            double yaw=Math.toRadians(Mth.rotLerp(partial,living.yBodyRotO,living.yBodyRot));
            Vec3 forward=new Vec3(-Math.sin(yaw),0,Math.cos(yaw)),side=new Vec3(Math.cos(yaw),0,Math.sin(yaw));
            int emitted=0;
            for(int i=0;i<OUTBREAK_RUNES;i++) {
                double phase=outbreakPhase(progress,i);if(phase>=.48)continue;
                double angle=i*2.399,radius=2+Math.sqrt((i+.5)/OUTBREAK_RUNES)*13;
                Vec3 scatter=start.add(Math.cos(angle)*radius,0,Math.sin(angle)*radius);
                double gather=smooth((phase-.14)/.34),spread=smooth(phase/.14);
                Vec3 feet=target.getPosition(partial).add(forward.scale(.17)).add(side.scale((i&1)==0?-.12:.12));
                Vec3 a=start.lerp(scatter,spread).lerp(feet,gather);
                Vec3 heading=feet.subtract(scatter).multiply(1,0,1).normalize();
                Vec3 across=new Vec3(-heading.z,0,heading.x);
                a=a.add(across.scale(Math.sin(phase*40+i)*.065*gather*(1-gather)));
                a=new Vec3(a.x,groundHeight(eruption,living,a)+.008,a.z);
                if(a.distanceToSqr(camera)>80*80)continue;
                double shrink=1-gather*.62;
                Vec3 right=across.scale(.16*shrink),up=heading.scale(.20*shrink);
                if(heading.lengthSqr()<.1){right=new Vec3(.16,0,0);up=new Vec3(0,0,.20);}
                glyph(out,m,a.subtract(camera),right,up,i,1);
                emitted++;
            }
            densestOutbreakRunes=Math.max(densestOutbreakRunes,emitted);
            outbreakFrames++;
        }
        buffers.endBatch(INK);
    }
    private static double groundHeight(Outbreak eruption,LivingEntity target,Vec3 point) {
        var key=new GroundKey(Mth.floor(point.x*4),Mth.floor(point.y),Mth.floor(point.z*4));
        var cached=eruption.ground.get(key);if(cached!=null)return cached;
        long now=world.getGameTime();if(groundProbeTick!=now){groundProbeTick=now;groundProbes=0;}
        if(groundProbes>=128)return point.y-.02;
        groundProbes++;
        var hit=world.clip(new ClipContext(point.add(0,1.25,0),point.add(0,-4,0),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,target));
        double height=hit.getType()==HitResult.Type.MISS?point.y-.02:hit.getLocation().y;
        if(eruption.ground.size()>=4096)eruption.ground.clear();
        eruption.ground.put(key,height);return height;
    }
}
