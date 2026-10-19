package com.example.immortal_cultivation_mod.item;

import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.animation.AnimationProcessor;
import software.bernie.geckolib.animatable.GeoItem;
import com.example.immortal_cultivation_mod.client.WhipMesh;
import com.example.immortal_cultivation_mod.client.WhipRenderContext;
import com.example.immortal_cultivation_mod.physics.WhipRope;
import com.example.immortal_cultivation_mod.physics.WhipContacts;
import com.example.immortal_cultivation_mod.physics.WhipHeldPose;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;
import java.util.WeakHashMap;

public class WhipItemRenderer extends GeoItemRenderer<WhipItem> {
    private final Map<LivingEntity,EnumMap<ItemDisplayContext,Motion>> motions=new WeakHashMap<>();
    private Level world;
    private Motion motion;
    private LivingEntity holder;
    private double time;
    private GeoBone coilBone;
    private Vec3[] localCoil;
    private boolean icon;
    private static final class Motion {
        WhipRope rope=new WhipRope();long itemId=Long.MAX_VALUE;int slot=-1;AnimationProcessor.QueuedAnimation animation;
        double strike=-100,lastSeen;
        long collisionTick=Long.MIN_VALUE;
        WhipRope.Contact contacts=WhipRope.AIR;
    }
    public WhipItemRenderer() {
        super(new WhipItemModel());
    }
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource source,int light,int overlay) {
        if (WeaponInventorySprite.usesSprite(context)) {
            WeaponInventorySprite.render("whip", pose, source, light, overlay);
            return;
        }
        var mc=Minecraft.getInstance();
        if(world!=mc.level){motions.clear();world=mc.level;}
        holder=WhipRenderContext.holder();motion=null;icon=context==ItemDisplayContext.GUI;
        boolean held=context.firstPerson() || context==ItemDisplayContext.THIRD_PERSON_LEFT_HAND || context==ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
        if(held && holder!=null && holder.level()==world && world!=null) {
            time=(world.getGameTime()+mc.getTimer().getGameTimeDeltaPartialTick(true))*.05;
            var perView=motions.computeIfAbsent(holder,e->new EnumMap<>(ItemDisplayContext.class));
            motion=perView.computeIfAbsent(context,c->new Motion());
            long id=GeoItem.getId(stack);
            int slot=holder instanceof net.minecraft.world.entity.player.Player p?p.getInventory().selected:-1;
            boolean differentItem=motion.itemId!=Long.MAX_VALUE && id!=Long.MAX_VALUE && motion.itemId!=id;
            // Durability/Qi updates replace client ItemStacks. Keep momentum for the same item ID.
            if(differentItem || motion.slot!=slot || time-motion.lastSeen>.25){
                motion.rope=new WhipRope();motion.collisionTick=Long.MIN_VALUE;
                motion.strike=-100;motion.animation=null;
            }
            motion.itemId=id;motion.slot=slot;motion.lastSeen=time;
        }
        try {super.renderByItem(stack,context,pose,source,light,overlay);}
        finally {motion=null;holder=null;icon=false;}
    }
    @Override public void renderRecursively(PoseStack pose,WhipItem item,GeoBone bone,RenderType type,MultiBufferSource source,
                                            VertexConsumer buffer,boolean rerender,float partial,int light,int overlay,int color) {
        if(icon) {
            // GUI and held views share the animated model. Draw only the rest pose here,
            // then restore each bone so rendering the icon cannot interrupt a swing.
            float x=bone.getRotX(),y=bone.getRotY(),z=bone.getRotZ();
            var rest=bone.getInitialSnapshot();
            bone.setRotX(rest==null?0:rest.getRotX());
            bone.setRotY(rest==null?0:rest.getRotY());
            bone.setRotZ(rest==null?0:rest.getRotZ());
            try {super.renderRecursively(pose,item,bone,type,source,buffer,rerender,partial,light,overlay,color);}
            finally {bone.setRotX(x);bone.setRotY(y);bone.setRotZ(z);}
            return;
        }
        if(motion!=null && bone.getName().equals("handle")) {
            var controller=item.getAnimatableInstanceCache().getManagerForId(getInstanceId(item)).getAnimationControllers().get("main");
            var animation=controller==null?null:controller.getCurrentAnimation();
            // The queued clip can start a frame after its trigger arrives. Observe playback,
            // not the trigger edge, or that first transition would swallow the handle flick.
            if(animation!=motion.animation && animation!=null
                    && animation.animation().name().equals("animation.whip.attack")) {
                motion.strike=time;
            }
            motion.animation=animation;
            double t=time-motion.strike;
            float swing=t<0 || t>.65?0:t<.12?(float)-Math.sin(t/.12*Math.PI/2):t<.25?
                    (float)(-1+2.15*smooth((t-.12)/.13)):(float)(1.15*Math.exp(-(t-.25)*10)*Math.cos((t-.25)*12));
            float x=bone.getRotX(),z=bone.getRotZ();
            bone.setRotZ(z+swing);bone.setRotX(x+swing*.35F);
            try {super.renderRecursively(pose,item,bone,type,source,buffer,rerender,partial,light,overlay,color);}
            finally {bone.setRotX(x);bone.setRotZ(z);}
            return;
        }
        if(motion!=null && bone.getName().equals("lash_00")) {
            // Both vanilla held-item paths provide camera-relative world coordinates here.
            Vector3f local=new Vector3f(bone.getPivotX()/16F,bone.getPivotY()/16F,bone.getPivotZ()/16F);
            Vector3f end=pose.last().pose().transformPosition(local);
            Vec3 camera=Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            Vec3 anchor=new Vec3(end.x,end.y,end.z).add(camera);
            // Inventory entity previews also use THIRD_PERSON; their screen matrices are not world anchors.
            if(anchor.distanceToSqr(holder.getEyePosition(partial))<16) {
                // Rest pivots are model-space coordinates; ignore the old unfolding clip.
                // Rebuild after resource reloads, when the baked root bone changes.
                if(coilBone!=bone) {
                    var authored=new ArrayList<Vec3>();
                    for(GeoBone part=bone;part!=null;part=part.getChildBones().isEmpty()?null:part.getChildBones().getFirst())
                        authored.add(new Vec3(part.getPivotX()/16D,part.getPivotY()/16D,part.getPivotZ()/16D));
                    localCoil=WhipHeldPose.coil(authored);coilBone=bone;
                }
                Vector3f axis=pose.last().pose().transformDirection(new Vector3f(0,1,0)).normalize();
                Vec3 direction=new Vec3(axis.x,axis.y,axis.z);
                if(!motion.rope.initialized()) {
                    Vec3[] coil=new Vec3[WhipRope.SEGMENTS+1];
                    for(int i=0;i<coil.length;i++) {
                        Vec3 p=localCoil[i];
                        Vector3f transformed=pose.last().pose().transformPosition(new Vector3f((float)p.x,(float)p.y,(float)p.z));
                        coil[i]=new Vec3(transformed.x,transformed.y,transformed.z).add(camera);
                    }
                    // Seed once on equip; gravity and collisions stay active even without an attack.
                    motion.rope.hold(time,coil);
                }
                if(motion.collisionTick!=world.getGameTime()) {
                    AABB bounds=new AABB(anchor,anchor).inflate(.6);
                    if(motion.rope.initialized())for(int i=0;i<=WhipRope.SEGMENTS;i++)bounds=bounds.minmax(new AABB(motion.rope.point(i),motion.rope.predictedPoint(i)).inflate(.6));
                    var boxes=new ArrayList<AABB>();
                    for(var shape:world.getBlockCollisions(holder,bounds))boxes.addAll(shape.toAabbs());
                    motion.contacts=new WhipContacts(boxes);motion.collisionTick=world.getGameTime();
                }
                motion.rope.advance(time,anchor,direction,motion.contacts);
                Vec3 shift=anchor.subtract(motion.rope.point(0));
                Vec3[] displayed=new Vec3[WhipRope.SEGMENTS+1];
                for(int i=0;i<displayed.length;i++)displayed[i]=motion.rope.point(i).add(shift.scale(Math.exp(-i*.7)));
                WhipMesh.render(motion.rope,displayed,camera,new PoseStack(),buffer,light,overlay,color);
                com.example.immortal_cultivation_mod.client.qi.QiCoatingRenderer.physicsWhip(holder,displayed,camera,source,time);
                return;
            }
        }
        super.renderRecursively(pose,item,bone,type,source,buffer,rerender,partial,light,overlay,color);
    }
    private static double smooth(double x){return x*x*(3-2*x);}
}
