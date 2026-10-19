package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.client.WhipRenderContext;
import com.example.immortal_cultivation_mod.physics.WhipContacts;
import com.example.immortal_cultivation_mod.physics.WhipRope;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.RenderUtil;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;
import java.util.WeakHashMap;

public final class HigherWeaponItemRenderer extends GeoItemRenderer<HigherWeaponGeoItem> {
    private final Map<LivingEntity,EnumMap<ItemDisplayContext,Motion>> motions=new WeakHashMap<>();
    private boolean icon;
    private Level world;
    private LivingEntity holder;
    private Motion motion;
    private double time;
    private BakedGeoModel iconModel;
    private Vector3f iconCenter;
    private float iconScale=1;

    private static final class Motion {
        WhipRope rope;
        long itemId=Long.MAX_VALUE,collisionTick=Long.MIN_VALUE;
        int slot=-1;
        double lastSeen;
        WhipRope.Contact contacts=WhipRope.AIR;
    }
    private record BonePose(float x,float y,float z,float px,float py,float pz) {
        BonePose(GeoBone b){this(b.getRotX(),b.getRotY(),b.getRotZ(),b.getPosX(),b.getPosY(),b.getPosZ());}
        void restore(GeoBone b){b.setRotX(x);b.setRotY(y);b.setRotZ(z);b.setPosX(px);b.setPosY(py);b.setPosZ(pz);}
    }

    public HigherWeaponItemRenderer() {
        super(new GeoModel<>() {
            @Override public ResourceLocation getModelResource(HigherWeaponGeoItem item) {
                return HigherWeaponItem.id("geo/item/"+item.spec.id+".geo.json");
            }
            @Override public ResourceLocation getTextureResource(HigherWeaponGeoItem item) {
                return HigherWeaponItem.id("textures/item/"+item.spec.id+"_atlas.png");
            }
            @Override public ResourceLocation getAnimationResource(HigherWeaponGeoItem item) {
                return HigherWeaponItem.id("animations/item/"+item.spec.id+".animation.json");
            }
        });
    }

    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource source,int light,int overlay) {
        HigherWeaponSpec spriteSpec = ((HigherWeaponGeoItem) stack.getItem()).spec;
        if (WeaponInventorySprite.usesSprite(context)
                && (spriteSpec == HigherWeaponSpec.EARTHBREAKER_HAMMER
                || spriteSpec == HigherWeaponSpec.SPIRIT_SILK_WHIP
                || spriteSpec == HigherWeaponSpec.SOUL_HOOK)) {
            WeaponInventorySprite.render(spriteSpec.id, pose, source, light, overlay);
            return;
        }
        var mc=Minecraft.getInstance();
        if(world!=mc.level){motions.clear();world=mc.level;}
        icon=context==ItemDisplayContext.GUI;
        holder=WhipRenderContext.holder();motion=null;
        HigherWeaponSpec spec=((HigherWeaponGeoItem)stack.getItem()).spec;
        boolean held=context.firstPerson() || context==ItemDisplayContext.THIRD_PERSON_LEFT_HAND || context==ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
        if(held && holder!=null && holder.level()==world && world!=null
                && (spec==HigherWeaponSpec.SPIRIT_SILK_WHIP || spec==HigherWeaponSpec.SOUL_HOOK)) {
            time=(world.getGameTime()+mc.getTimer().getGameTimeDeltaPartialTick(true))*.05;
            motion=motions.computeIfAbsent(holder,e->new EnumMap<>(ItemDisplayContext.class)).computeIfAbsent(context,c->new Motion());
            long id=GeoItem.getId(stack);
            int slot=holder instanceof net.minecraft.world.entity.player.Player p?p.getInventory().selected:-1;
            if((motion.itemId!=Long.MAX_VALUE && id!=Long.MAX_VALUE && motion.itemId!=id)
                    || motion.slot!=slot || time-motion.lastSeen>.25) {
                motion.rope=null;motion.collisionTick=Long.MIN_VALUE;
            }
            motion.itemId=id;motion.slot=slot;motion.lastSeen=time;
        }
        try {super.renderByItem(stack,context,pose,source,light,overlay);}
        finally {icon=false;motion=null;holder=null;}
    }

    @Override public void preRender(PoseStack pose,HigherWeaponGeoItem item,BakedGeoModel model,MultiBufferSource source,
            VertexConsumer buffer,boolean rerender,float partial,int light,int overlay,int color) {
        super.preRender(pose,item,model,source,buffer,rerender,partial,light,overlay,color);
        if(!icon)return;
        if(iconModel!=model) {
            // Fit the actual, projected rest silhouette, including the stored coil or folded fan.
            Quaternionf view=iconRotation(item.spec);
            float[] bounds={Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,
                    Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY};
            PoseStack local=new PoseStack();local.mulPose(view);
            for(GeoBone bone:model.topLevelBones())measureIcon(local,item.spec,bone,bounds);
            iconScale=0.86F/Math.max(bounds[3]-bounds[0],bounds[4]-bounds[1]);
            iconCenter=new Vector3f((bounds[0]+bounds[3])*.5F,(bounds[1]+bounds[4])*.5F,(bounds[2]+bounds[5])*.5F);
            new Quaternionf(view).conjugate().transform(iconCenter);
            iconModel=model;
        }
        pose.scale(iconScale,iconScale,iconScale);
        pose.translate(-iconCenter.x,-iconCenter.y,-iconCenter.z);
    }

    private static Quaternionf iconRotation(HigherWeaponSpec spec) {
        float y=spec==HigherWeaponSpec.SOUL_HOOK || spec==HigherWeaponSpec.EARTHBREAKER_HAMMER?145:180;
        float z=spec==HigherWeaponSpec.SPIRIT_SILK_WHIP?20:-25;
        return new Quaternionf().rotationXYZ(0,(float)Math.toRadians(y),(float)Math.toRadians(z));
    }
    private static void restPose(HigherWeaponSpec spec,GeoBone bone) {
        var rest=bone.getInitialSnapshot();
        bone.setRotX(rest==null?0:rest.getRotX());bone.setRotY(rest==null?0:rest.getRotY());bone.setRotZ(rest==null?0:rest.getRotZ());
        bone.setPosX(0);bone.setPosY(0);bone.setPosZ(0);
        if(spec==HigherWeaponSpec.SPIRIT_SILK_WHIP && bone.getName().startsWith("silk_")) {
            bone.setRotZ((float)Math.toRadians(bone.getName().equals("silk_00")?85:48));
            bone.setPosZ(bone.getName().equals("silk_00")?0:.15F);
        }
        // Fan ribs remain at their authored closed angles, regardless of its held form.
    }
    private static void measureIcon(PoseStack pose,HigherWeaponSpec spec,GeoBone bone,float[] bounds) {
        BonePose previous=new BonePose(bone);restPose(spec,bone);pose.pushPose();RenderUtil.prepMatrixForBone(pose,bone);
        for(var cube:bone.getCubes()) {
            pose.pushPose();RenderUtil.translateToPivotPoint(pose,cube);RenderUtil.rotateMatrixAroundCube(pose,cube);RenderUtil.translateAwayFromPivotPoint(pose,cube);
            for(var quad:cube.quads())if(quad!=null)for(var vertex:quad.vertices()) {
                Vector3f p=pose.last().pose().transformPosition(new Vector3f(vertex.position()));
                for(int axis=0;axis<3;axis++){bounds[axis]=Math.min(bounds[axis],p.get(axis));bounds[axis+3]=Math.max(bounds[axis+3],p.get(axis));}
            }
            pose.popPose();
        }
        for(var child:bone.getChildBones())measureIcon(pose,spec,child,bounds);
        pose.popPose();previous.restore(bone);
    }

    @Override public void renderRecursively(PoseStack pose,HigherWeaponGeoItem item,GeoBone bone,RenderType type,
            MultiBufferSource source,VertexConsumer buffer,boolean rerender,float partial,int light,int overlay,int color) {
        if(icon) {
            BonePose previous=new BonePose(bone);restPose(item.spec,bone);
            try {super.renderRecursively(pose,item,bone,type,source,buffer,rerender,partial,light,overlay,color);}
            finally {previous.restore(bone);}
            return;
        }
        if(motion!=null && (bone.getName().equals("silk_00") || bone.getName().equals("chain_0"))
                && renderFlexible(pose,item,bone,type,source,buffer,rerender,partial,light,overlay,color))return;
        super.renderRecursively(pose,item,bone,type,source,buffer,rerender,partial,light,overlay,color);
    }

    private boolean renderFlexible(PoseStack pose,HigherWeaponGeoItem item,GeoBone first,RenderType type,
            MultiBufferSource source,VertexConsumer buffer,boolean rerender,float partial,int light,int overlay,int color) {
        if(item.spec==HigherWeaponSpec.SPIRIT_SILK_WHIP) {
            var binding=HigherWeaponItem.data(currentItemStack);
            if(binding.getLong("SilkBindingUntil")>world.getGameTime()
                    && world.getEntity(binding.getInt("SilkBindingTarget")) instanceof LivingEntity target && target.isAlive()) {
                // Fuchen's shared plume renders the complete hand-to-target tether while binding.
                // Leave only our handle visible, so a second coiled lash cannot overlap it.
                motion.rope=null;
                return true;
            }
        }
        Vec3 camera=Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vector3f socket=pose.last().pose().transformPosition(new Vector3f(first.getPivotX()/16,first.getPivotY()/16,first.getPivotZ()/16));
        Vec3 anchor=new Vec3(socket.x,socket.y,socket.z).add(camera);
        if(anchor.distanceToSqr(holder.getEyePosition(partial))>=16)return false;
        Vector3f axis=pose.last().pose().transformDirection(new Vector3f(0,1,0));
        float scale=axis.length();axis.normalize();
        Vec3 direction=new Vec3(axis.x,axis.y,axis.z);
        boolean silk=item.spec==HigherWeaponSpec.SPIRIT_SILK_WHIP;
        double length=(silk?44:11)/16D*scale;
        if(motion.rope==null || Math.abs(motion.rope.length()-length)>.05)
            motion.rope=silk?WhipRope.spiritSilk(scale)
                    :new WhipRope(length,.045*scale,.07*scale,1.7,1.4,.12);
        // Held silk is a free lash, not a supported inventory coil. Advance every frame so
        // walking, turning, and missed swings keep their momentum without an attack trigger.
        if(motion.collisionTick!=world.getGameTime()) {
            AABB bounds=new AABB(anchor,anchor).inflate(length+.5);
            if(motion.rope.initialized())for(int i=0;i<=WhipRope.SEGMENTS;i++)
                bounds=bounds.minmax(new AABB(motion.rope.point(i),motion.rope.predictedPoint(i)).inflate(.1));
            var boxes=new ArrayList<AABB>();
            for(var shape:world.getBlockCollisions(holder,bounds))boxes.addAll(shape.toAabbs());
            motion.contacts=new WhipContacts(boxes);motion.collisionTick=world.getGameTime();
        }
        motion.rope.advance(time,anchor,direction,motion.contacts);
        Vec3[] points=new Vec3[WhipRope.SEGMENTS+1];Vec3 shift=anchor.subtract(motion.rope.point(0));
        for(int i=0;i<points.length;i++) {
            points[i]=motion.rope.point(i).add(shift.scale(Math.exp(-i*.7)));
        }
        int index=0,count=silk?20:7;
        for(GeoBone part=first;part!=null;part=part.getChildBones().isEmpty()?null:part.getChildBones().getFirst(),index++) {
            boolean hook=part.getName().equals("hook");
            Vec3 start=sample(points,Math.min(index,count)/(double)count);
            Vec3 end=hook?start.add(sample(points,1).subtract(sample(points,.94)).normalize().scale(.3))
                    :sample(points,(index+1)/(double)count);
            Vector3f tangent=new Vector3f((float)(end.x-start.x),(float)(end.y-start.y),(float)(end.z-start.z));
            if(tangent.lengthSquared()<1e-9)continue;
            PoseStack link=new PoseStack();Vec3 offset=start.subtract(camera);link.translate(offset.x,offset.y,offset.z);
            link.mulPose(new Quaternionf().rotationTo(new Vector3f(0,1,0),tangent.normalize()));
            // Preserve the authored link/hook details while rotating each connected segment in world space.
            double authoredLength=silk?2.2/16D:(index==6?2:1.5)/16D;
            link.scale(scale,hook?scale:(float)(start.distanceTo(end)/authoredLength),scale);
            link.translate(-part.getPivotX()/16D,-part.getPivotY()/16D,-part.getPivotZ()/16D);
            buffer=checkAndRefreshBuffer(rerender,buffer,source,type);
            renderCubesOfBone(link,part,buffer,light,overlay,color);
            // Layer effects must use the same physics-deformed segment pose as the whip.
            if(!rerender)applyRenderLayersForBone(link,item,part,type,source,buffer,partial,light,overlay);
        }
        return true;
    }
    private static Vec3 sample(Vec3[] points,double fraction) {
        double index=Math.clamp(fraction,0,1)*WhipRope.SEGMENTS;int first=(int)index;
        return points[first].lerp(points[Math.min(first+1,WhipRope.SEGMENTS)],index-first);
    }
}
