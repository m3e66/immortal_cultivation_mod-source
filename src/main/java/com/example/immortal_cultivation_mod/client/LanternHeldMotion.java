package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.physics.LanternSwing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import java.util.EnumMap;
import java.util.WeakHashMap;

public final class LanternHeldMotion {
    private static final WeakHashMap<LivingEntity,EnumMap<ItemDisplayContext,LanternSwing>> MOTION=new WeakHashMap<>();
    private static Object world;
    public static void apply(ItemStack stack,ItemDisplayContext display,PoseStack poses) {
        var mc=Minecraft.getInstance();var holder=WhipRenderContext.holder();
        if(world!=mc.level){MOTION.clear();world=mc.level;}
        if(!stack.is(ModItems.XUANJIN_SEAL.get()) || holder==null || mc.level==null || holder.level()!=mc.level)return;
        if(!display.firstPerson() && display!=ItemDisplayContext.THIRD_PERSON_LEFT_HAND && display!=ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)return;
        float partial=mc.getTimer().getGameTimeDeltaPartialTick(true);
        // The item origin is the fist. Keep the handle crown there and hang the body below it.
        float pivot=15.425F/16F-.5F;
        var matrix=poses.last().pose();var at=matrix.transformPosition(new Vector3f());
        Vec3 anchor=new Vec3(at.x,at.y,at.z).add(mc.gameRenderer.getMainCamera().getPosition());
        if(anchor.distanceToSqr(holder.getEyePosition(partial))>16)return;
        // Remove hand/camera tilt from the rest orientation: gravity keeps a lantern upright.
        // Work around the grip, so turning the wrist never moves the handle out of the fist.
        var localUp=new Matrix4f(matrix).invert().transformDirection(new Vector3f(0,1,0)).normalize();
        poses.mulPose(new Quaternionf().rotationTo(new Vector3f(0,1,0),localUp));
        matrix=poses.last().pose();
        var x=matrix.transformDirection(new Vector3f(1,0,0)).normalize();
        var z=matrix.transformDirection(new Vector3f(0,0,1)).normalize();
        var swing=MOTION.computeIfAbsent(holder,e->new EnumMap<>(ItemDisplayContext.class)).computeIfAbsent(display,d->new LanternSwing());
        swing.advance((mc.level.getGameTime()+partial)*.05,anchor,new Vec3(x.x,x.y,x.z),new Vec3(z.x,z.y,z.z));
        poses.mulPose(Axis.XP.rotationDegrees((float)swing.pitch()));
        poses.mulPose(Axis.ZP.rotationDegrees((float)swing.roll()));
        poses.translate(0,-pivot,0);
    }
}
