package com.example.immortal_cultivation_mod.physics;

import net.minecraft.util.Mth;

/** Bounded vertical tracking shared by the rendered neck and its server collision parts. */
public final class DragonLookPose {
    public static float advance(float current,float desired) {
        if(!Float.isFinite(current))current=0;
        desired=Float.isFinite(desired)?Mth.clamp(desired,-40,50):0;
        float difference=desired-current;
        if(Math.abs(difference)<.02F)return desired;
        return current+Mth.clamp(difference*.25F,-4,4);
    }
    public static float rotation(String bone,float pitch) {
        // Minecraft positive pitch looks down; the model faces -Z, so its X rotation is negative.
        return -pitch*switch(bone) {
            case "neck_1" -> .25F;
            case "neck_2" -> .35F;
            case "head" -> .4F;
            default -> 0F;
        };
    }
    private DragonLookPose(){}
}
