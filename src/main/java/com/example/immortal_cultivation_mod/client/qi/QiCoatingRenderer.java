package com.example.immortal_cultivation_mod.client.qi;

import com.example.immortal_cultivation_mod.client.*;
import com.example.immortal_cultivation_mod.item.QiInfusedWeapon;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.*;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Attaches to the actual arm and weapon transforms, including every physics-whip segment. */
public final class QiCoatingRenderer {
    public static long hands,items,geoCubes,ropeSegments;
    public static int maxWhipSegments;
    private record Trace(Vec3 point,double time){}
    private static final java.util.Map<LivingEntity,java.util.Map<String,Trace>> TRACES=new java.util.WeakHashMap<>();
    private QiCoatingRenderer(){}
    public static boolean held(ItemDisplayContext context){return context!=null&&(context.firstPerson()
            ||context==ItemDisplayContext.THIRD_PERSON_LEFT_HAND||context==ItemDisplayContext.THIRD_PERSON_RIGHT_HAND);}
    private static boolean weapon(ItemStack stack){return QiInfusedWeapon.isWeapon(stack)&&!QiInfusedWeapon.isFistWeapon(stack);}
    public static void hand(PoseStack pose,MultiBufferSource buffers,LivingEntity owner,ModelPart arm,boolean right,float partial,boolean firstPerson){
        var style=QiAuraRenderer.coating(owner,false);if(style==null)return;
        var stack=right==(owner.getMainArm()==HumanoidArm.RIGHT)?owner.getMainHandItem():owner.getOffhandItem();
        if(!stack.isEmpty()&&!QiInfusedWeapon.isFistWeapon(stack))return;
        float attack=(float)Math.sin(owner.getAttackAnim(partial)*Math.PI);
        var look=QiElementLook.of(style);
        double time=owner.level().getGameTime()+partial;
        pose.pushPose();arm.translateAndRotate(pose);
        if(firstPerson){
            // This stack already contains equip, swing, bob and arm transforms. Keep
            // its complete basis: first-person hands are not world-space entities.
            pose.translate(right?-.05F:.05F,.52F,0);
            Vector3f center=pose.last().pose().transformPosition(new Vector3f());
            // Arm-local +Y points through the knuckles. Flip around the palm so
            // the flame tapers back toward the wrist instead of projecting forward.
            pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(180));
            pose.translate(0,-.24,0);
            pose.scale((look.art()==13?.35F:.24F)*(1+attack*.35F),(look.art()==13?.45F:.72F)+attack*.32F,(look.art()==13?.35F:.24F)*(1+attack*.35F));
            QiEnergyMesh.draw(pose,buffers,look,time,attack,.86F,Vec3.ZERO,true);
            pose.popPose();hands++;
            trail(owner,"hand:"+right+":true",new Vec3(center),buffers,look,time,attack,true);
            return;
        }
        pose.translate(right?-.05F:.05F,.62F,0);
        Vector3f center=pose.last().pose().transformPosition(new Vector3f());
        // Third-person fists use the same wristward direction as first-person fists,
        // retaining the real arm and flight transforms instead of resetting to world-up.
        pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(180));pose.translate(0,-.24,0);
        pose.scale((look.art()==13?.35F:.24F)*(1+attack*.35F),(look.art()==13?.45F:.72F)+attack*.32F,(look.art()==13?.35F:.24F)*(1+attack*.35F));
        QiEnergyMesh.draw(pose,buffers,look,time,attack,.86F,Vec3.ZERO,true);pose.popPose();hands++;
        trail(owner,"hand:"+right+":false",new Vec3(center),buffers,look,time,attack);
    }
    public static void baked(PoseStack pose,MultiBufferSource buffers,ItemStack stack,ItemDisplayContext display,BakedModel model){
        LivingEntity owner=WhipRenderContext.holder();
        if(owner==null||!held(display)||model.isCustomRenderer()||!weapon(stack))return;
        var style=QiAuraRenderer.coating(owner,true);if(style==null)return;
        var look=QiElementLook.of(style);double time=owner.level().getGameTime()+Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        float surge=surge(owner);var random=RandomSource.create(42);
        var quads=new java.util.ArrayList<net.minecraft.client.renderer.block.model.BakedQuad>();
        for(var pass:model.getRenderPasses(stack,true)){
            for(Direction d:Direction.values()){random.setSeed(42);quads.addAll(pass.getQuads(null,d,random));}
            random.setSeed(42);quads.addAll(pass.getQuads(null,null,random));
        }
        if(!QiAuraMaterial.ready()||!QiVfxQuality.takeCoating(quads.size()))return;
        var out=buffers.getBuffer(QiAuraMaterial.ENERGY);
        for(var q:quads){
            int[] data=q.getVertices();int stride=data.length/4;Vec3[] vertices=new Vec3[4];
            for(int i=0;i<4;i++){
                vertices[i]=new Vec3(Float.intBitsToFloat(data[i*stride]),Float.intBitsToFloat(data[i*stride+1]),Float.intBitsToFloat(data[i*stride+2]));
            }
            var n=q.getDirection().getNormal();
            for(int i=0;i<4;i++){
                Vec3 p=vertices[i].add(n.getX()*.025,n.getY()*.025,n.getZ()*(.025+surge*.035));
                out.addVertex(pose.last(),(float)p.x,(float)p.y,(float)p.z)
                    .setColor((look.color()>>16&255)/255F,(look.color()>>8&255)/255F,(look.color()&255)/255F,.85F*QiVfxQuality.opacity())
                    .setUv(Float.intBitsToFloat(data[i*stride+4]),Float.intBitsToFloat(data[i*stride+5]))
                    .setOverlay((int)(time%1500*20)|(look.shaderCode(32)<<16)).setLight((int)((p.y+2)*1300))
                    .setNormal(pose.last(),n.getX(),n.getY(),n.getZ());
            }
        }
        var silhouette=QiWeaponSilhouette.fit(quads);
        if(silhouette!=null){
            QiWeaponFlames.wrap(pose,buffers,silhouette.start(),silhouette.end(),
                    silhouette.width()+.045,silhouette.depth()+.045,look,time,surge,0);
            trail(owner,"baked:"+display,new Vec3(pose.last().pose().transformPosition(
                    new Vector3f((float)silhouette.end().x,(float)silhouette.end().y,(float)silhouette.end().z))),buffers,look,time,surge);
        }
        items++;
    }
    public static float surge(LivingEntity owner){
        float partial=Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        float swing=(float)Math.sin(owner.getAttackAnim(partial)*Math.PI);
        float charge=owner==Minecraft.getInstance().player?ClientData.heavyAttackChargeProgress():0;
        return Math.max(charge,Math.max(swing,owner.isUsingItem()?Math.min(1,owner.getTicksUsingItem()/20F):0));
    }
    public static void trail(LivingEntity owner,String key,Vec3 relative,MultiBufferSource buffers,QiElementLook look,double time,float surge){
        trail(owner,key,relative,buffers,look,time,surge,false);
    }
    private static void trail(LivingEntity owner,String key,Vec3 relative,MultiBufferSource buffers,QiElementLook look,double time,float surge,boolean firstPerson){
        // A first-person trace is in the hand renderer's view space, just like its
        // current palm. Adding a world camera position would make it drift on turns.
        Vec3 camera=firstPerson?Vec3.ZERO:Minecraft.getInstance().gameRenderer.getMainCamera().getPosition(),point=relative.add(camera);
        var traces=TRACES.computeIfAbsent(owner,e->new java.util.HashMap<>());
        if(traces.size()>96)traces.clear();
        var previous=traces.put(key,new Trace(point,time));
        if(previous==null||surge<.15||time-previous.time<.01||time-previous.time>2.5)return;
        Vec3 delta=point.subtract(previous.point);if(delta.lengthSqr()<.0002||delta.lengthSqr()>2.25)return;
        Vec3 end=point.subtract(delta.scale(Math.min(3,1.6/(time-previous.time))));
        QiPressureMesh.ribbon(buffers.getBuffer(QiAuraMaterial.GLOW),new Matrix4f(),point,end,camera,.035+surge*.025,look.highlight(),.35F*surge);
    }
    public static void quad(PoseStack pose,VertexConsumer out,Vec3[] vertices,Vec3 normal,QiElementLook look,double time,float surge){
        for(int i=0;i<4;i++){
            Vec3 p=vertices[i];
            out.addVertex(pose.last(),(float)p.x,(float)p.y,(float)p.z)
                .setColor((look.color()>>16&255)/255F,(look.color()>>8&255)/255F,(look.color()&255)/255F,(.75F+surge*.2F)*QiVfxQuality.opacity())
                .setUv(-1,(float)(p.y*.7)).setOverlay((int)(time%1500*20)|(look.shaderCode(16)<<16)).setLight((int)((p.x+2)*1300))
                .setNormal(pose.last(),(float)normal.x,(float)normal.y,(float)normal.z);
        }
    }
    public static void physicsWhip(LivingEntity owner,Vec3[] points,Vec3 camera,MultiBufferSource buffers,double seconds){
        var style=QiAuraRenderer.coating(owner,true);if(style==null||!QiAuraMaterial.ready()||!QiVfxQuality.takeCoating(points.length*8))return;
        var look=QiElementLook.of(style);var out=buffers.getBuffer(QiAuraMaterial.ENERGY);PoseStack pose=new PoseStack();
        float surge=surge(owner);double radius=.045+surge*.035;
        maxWhipSegments=Math.max(maxWhipSegments,points.length-1);
        Vec3[][] ring=new Vec3[points.length][8];
        for(int j=0;j<points.length;j++){
            Vec3 axis=points[Math.min(points.length-1,j+1)].subtract(points[Math.max(0,j-1)]).normalize();
            Vec3 u=axis.cross(Math.abs(axis.y)<.8?new Vec3(0,1,0):new Vec3(1,0,0)).normalize(),v=axis.cross(u);
            double width=radius*(1+.15*Math.sin(j*.4-seconds*5));
            for(int i=0;i<8;i++)ring[j][i]=points[j].subtract(camera).add(u.scale(Math.cos(i*Math.PI/4)*width)).add(v.scale(Math.sin(i*Math.PI/4)*width));
        }
        for(int j=0;j<points.length-1;j++){
            for(int i=0;i<8;i++){
                int next=(i+1)%8;
                Vec3 normal=ring[j][i].add(ring[j][next]).scale(.5).subtract(points[j].subtract(camera)).normalize();
                // Adjacent segments reuse the exact same ring, even as the rope bends.
                quad(pose,out,new Vec3[]{ring[j][i],ring[j+1][i],ring[j+1][next],ring[j][next]},normal,look,seconds*20,surge);
            }
            ropeSegments++;
            if(j%2==0){
                QiWeaponFlames.wrap(pose,buffers,points[j].subtract(camera),points[Math.min(points.length-1,j+2)].subtract(camera),
                        radius*1.9,radius*1.9,look,seconds*20,surge,j*.8);
            }
        }
    }
}
