package com.example.immortal_cultivation_mod.client.model;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.animation.SpiritPuppetAnimations.Clip;
import com.example.immortal_cultivation_mod.entity.TrainingDummyEntity;
import com.example.immortal_cultivation_mod.physics.SpiritClothPhysics;
import com.example.immortal_cultivation_mod.physics.SpiritClothPhysics.Point;
import com.example.immortal_cultivation_mod.physics.SpiritClothPhysics.Capsule;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;

/** Applies independent per-entity cloth simulation after skeletal animation. */
public final class TrainingDummyGeoModel extends GeoModel<TrainingDummyEntity> {
    public static final float RENDER_SCALE=.61F;
    private static final double UNIT=RENDER_SCALE/16.0;
    private record Strip(String root,List<String> bones,Point[] nodes,double radius) {}
    private final WeakHashMap<TrainingDummyEntity,Motion> motions=new WeakHashMap<>();
    private List<Strip> strips;
    private static final class Motion {SpiritClothPhysics simulation;double time,x,y,z,yaw;int hurt;}
    private static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID,path);}
    @Override public ResourceLocation getModelResource(TrainingDummyEntity e){return id("geo/entity/spirit_puppet.geo.json");}
    @Override public ResourceLocation getTextureResource(TrainingDummyEntity e){return id("textures/entity/spirit_puppet.png");}
    @Override public ResourceLocation getAnimationResource(TrainingDummyEntity e){return id("animations/entity/spirit_puppet.animation.json");}
    private void loadStrips() {
        if(strips!=null)return;
        var loaded=new ArrayList<Strip>();
        try(var stream=Minecraft.getInstance().getResourceManager().open(id("physics/spirit_puppet.cloth.json"));
            var reader=new InputStreamReader(stream,StandardCharsets.UTF_8)) {
            for(var row:JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("chains")) {
                var data=row.getAsJsonObject();var names=new ArrayList<String>();var points=new ArrayList<Point>();
                data.getAsJsonArray("bones").forEach(n->names.add(n.getAsString()));
                for(var node:data.getAsJsonArray("nodes")) {var p=node.getAsJsonArray();points.add(new Point(p.get(0).getAsDouble(),p.get(1).getAsDouble(),p.get(2).getAsDouble()));}
                loaded.add(new Strip(data.get("root").getAsString(),names,points.toArray(Point[]::new),data.get("radius").getAsDouble()));
            }
        }catch(java.io.IOException error){throw new IllegalStateException("Missing puppet cloth rig",error);}
        strips=List.copyOf(loaded);
    }
    @Override public void setCustomAnimations(TrainingDummyEntity e,long instanceId,AnimationState<TrainingDummyEntity> state) {
        super.setCustomAnimations(e,instanceId,state);loadStrips();
        for(Strip s:strips)for(String name:s.bones)getBone(name).ifPresent(b->b.updateRotation(0,0,0));
        var headData=state.getData(DataTickets.ENTITY_MODEL_DATA);
        // Keep authored anticipation and hand poses. Look tracking is only a small additive adjustment.
        if(headData!=null&&e.puppetAnimation()!=Clip.DEATH)getBone("head").ifPresent(b->b.updateRotation(
                b.getRotX()+Mth.clamp(headData.headPitch(),-20,20)*Mth.DEG_TO_RAD,
                b.getRotY()+Mth.clamp(headData.netHeadYaw(),-28,28)*Mth.DEG_TO_RAD,b.getRotZ()));
        Map<GeoBone,Matrix4f> poses=new IdentityHashMap<>();List<Point[]> rest=new ArrayList<>();
        for(Strip s:strips) {
            Matrix4f m=matrix(getBone(s.root).orElseThrow(),poses);
            rest.add(Arrays.stream(s.nodes).map(p->transform(m,p)).toArray(Point[]::new));
        }
        double partial=state.getPartialTick(),now=(e.tickCount+partial)/20.0;
        double x=Mth.lerp(partial,e.xo,e.getX()),y=Mth.lerp(partial,e.yo,e.getY()),z=Mth.lerp(partial,e.zo,e.getZ());
        double yaw=Math.toRadians(180-Mth.rotLerp((float)partial,e.yBodyRotO,e.yBodyRot));
        Motion motion=motions.computeIfAbsent(e,ignored->new Motion());
        double dt=now-motion.time,dx=x-motion.x,dy=y-motion.y,dz=z-motion.z;
        if(motion.simulation==null)motion.simulation=new SpiritClothPhysics(rest,strips.stream().map(Strip::radius).toList());
        else if(dt<0||dt>.35||dx*dx+dy*dy+dz*dz>16)motion.simulation.reset(rest);
        else if(dt>0) {
            final double oldYaw=motion.yaw;
            motion.simulation.rebase(p->{Point world=rotateY(p,oldYaw).scale(UNIT).add(new Point(motion.x-x,motion.y-y,motion.z-z));return rotateY(world,-yaw).scale(1/UNIT);});
            if(e.hurtTime>motion.hurt)motion.simulation.impulse(new Point(10,18,30));
            Point wind=rotateY(new Point(Math.sin(now*.71)*8,1,10+Math.sin(now*.43)*5),-yaw);
            motion.simulation.advance(dt,rest,colliders(poses),wind,e.isInWaterOrBubble());
        }
        motion.time=now;motion.x=x;motion.y=y;motion.z=z;motion.yaw=yaw;motion.hurt=e.hurtTime;
        for(int c=0;c<strips.size();c++) {
            Strip strip=strips.get(c);Matrix4f parent=matrix(getBone(strip.root).orElseThrow(),poses);
            for(int i=0;i<strip.bones.size();i++) {
                GeoBone bone=getBone(strip.bones.get(i)).orElseThrow();
                Point delta=motion.simulation.point(c,i+1).subtract(motion.simulation.point(c,i));
                Vector3f desired=new Matrix4f(parent).invert().transformDirection(vector(delta)).normalize();
                Vector3f original=vector(strip.nodes[i+1].subtract(strip.nodes[i])).normalize();
                Vector3f a=new Quaternionf().rotationTo(original,desired).getEulerAnglesZYX(new Vector3f());
                if(Float.isFinite(a.x)&&Float.isFinite(a.y)&&Float.isFinite(a.z))bone.updateRotation(a.x,a.y,a.z);
                parent=new Matrix4f(parent).mul(local(bone));
            }
        }
    }
    private List<Capsule> colliders(Map<GeoBone,Matrix4f> m) {
        var list=new ArrayList<Capsule>();
        list.add(new Capsule(posed("torso",new Point(0,29,0),m),posed("torso",new Point(0,37,0),m),2.7));
        list.add(new Capsule(posed("pelvis",new Point(0,23.5,0),m),posed("pelvis",new Point(0,25.5,0),m),2.8));
        list.add(new Capsule(posed("head",new Point(0,42.5,.2),m),posed("head",new Point(0,46.5,.2),m),1.9));
        for(String side:List.of("l","r")) {
            double s=side.equals("l")?1:-1;
            list.add(new Capsule(posed("thigh_"+side,new Point(s*2.6,23,0),m),posed("thigh_"+side,new Point(s*3.6,14,0),m),1.65));
            list.add(new Capsule(posed("shin_"+side,new Point(s*3.6,13,0),m),posed("shin_"+side,new Point(s*4.2,4,0),m),1.4));
        }
        return list;
    }
    private Point posed(String name,Point p,Map<GeoBone,Matrix4f> cache){return transform(matrix(getBone(name).orElseThrow(),cache),p);}
    private static Matrix4f matrix(GeoBone b,Map<GeoBone,Matrix4f> cache) {
        Matrix4f found=cache.get(b);if(found!=null)return found;
        Matrix4f parent=b.getParent()==null?new Matrix4f():new Matrix4f(matrix(b.getParent(),cache));
        Matrix4f result=parent.mul(local(b));cache.put(b,result);return result;
    }
    private static Matrix4f local(GeoBone b) {
        return new Matrix4f().translate(-b.getPosX(),b.getPosY(),b.getPosZ()).translate(b.getPivotX(),b.getPivotY(),b.getPivotZ())
                .rotateZ(b.getRotZ()).rotateY(b.getRotY()).rotateX(b.getRotX()).scale(b.getScaleX(),b.getScaleY(),b.getScaleZ()).translate(-b.getPivotX(),-b.getPivotY(),-b.getPivotZ());
    }
    private static Vector3f vector(Point p){return new Vector3f((float)p.x(),(float)p.y(),(float)p.z());}
    private static Point transform(Matrix4f m,Point p){var v=m.transformPosition(vector(p));return new Point(v.x,v.y,v.z);}
    private static Point rotateY(Point p,double a){double c=Math.cos(a),s=Math.sin(a);return new Point(c*p.x()+s*p.z(),p.y(),-s*p.x()+c*p.z());}
}
