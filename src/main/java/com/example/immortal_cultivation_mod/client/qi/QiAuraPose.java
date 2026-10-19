package com.example.immortal_cultivation_mod.client.qi;

import com.example.immortal_cultivation_mod.client.JindanFlightEffects;
import com.example.immortal_cultivation_mod.client.SwordComboAnimations;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Anatomical torso attachment follows the player's actual flight pose, never the flame's orientation. */
public final class QiAuraPose {
    private QiAuraPose() { }

    public static float flightBlend(LivingEntity owner,float partial) {
        double speed=owner.getDeltaMovement().length();
        if(!Double.isFinite(speed)||speed<1E-5)return 0;
        float blend=0;
        if(owner.isFallFlying()) {
            float ticks=owner.getFallFlyingTicks()+partial;
            blend=Math.clamp(ticks*ticks/100F,0F,1F);
        }else if(owner instanceof Player player) {
            if(!SwordComboAnimations.isPlaying(player))blend=JindanFlightEffects.fastFlightPoseProgress(player,partial);
            // Hovering and sword flight keep an upright body, but moving air still bends its aura.
            if(blend<=.001F&&player.getAbilities().flying&&!player.onGround())
                blend=(float)(.35*Math.clamp((speed-.025)/.4,0,1));
        }
        return blend;
    }

    public static Vec3 bodyOffset(LivingEntity owner,float partial) {
        float height=owner.getDimensions(net.minecraft.world.entity.Pose.STANDING).height();
        Quaternionf body=alongMovement(owner.getDeltaMovement(),flightBlend(owner,partial),owner.isFallFlying()?90:82.5);
        return transform(body,new Vec3(0,height*.5,0));
    }

    public static Quaternionf alongMovement(Vec3 movement,float blend,double leanDegrees) {
        double horizontal=movement.horizontalDistance();
        if(blend<=0||movement.lengthSqr()<1E-10)return new Quaternionf();
        double angle=Math.toRadians(leanDegrees)-Math.atan2(movement.y,horizontal);
        // Straight climbs/dives have no meaningful yaw. A stable fixed axis avoids NaNs.
        double x=horizontal>1E-5?movement.x/horizontal:0;
        double z=horizontal>1E-5?movement.z/horizontal:1;
        Vector3f axis=new Vector3f((float)(x*Math.sin(angle)),(float)Math.cos(angle),(float)(z*Math.sin(angle)));
        Quaternionf target=new Quaternionf().rotationTo(new Vector3f(0,1,0),axis);
        return new Quaternionf().slerp(target,Math.clamp(blend,0F,1F));
    }

    public static Vec3 transform(Quaternionf rotation,Vec3 vector) {
        return new Vec3(rotation.transform(new Vector3f((float)vector.x,(float)vector.y,(float)vector.z)));
    }
}
