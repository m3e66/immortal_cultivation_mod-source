package com.example.immortal_cultivation_mod.hair.client;

import com.example.immortal_cultivation_mod.hair.HairLookAdjustment;
import java.util.List;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Back locks hang downward from their head attachments, with static chest clearance. */
record HairChestPose(float pitch,float yaw,float roll,HairLookAdjustment.Collider chest) {
    static HairChestPose of(AbstractClientPlayer player,PlayerModel<AbstractClientPlayer> model){
        float pad=player.getItemBySlot(EquipmentSlot.CHEST).isEmpty()?.4F:.9F;
        var headToChest=matrix(model.body).invert().mul(matrix(model.head));
        return new HairChestPose(model.head.xRot,model.head.yRot,model.head.zRot,new HairLookAdjustment.Collider(headToChest,
                new Vector3f(-4-pad,-pad,-2-pad),new Vector3f(4+pad,12+pad,2+pad)));
    }
    private static Matrix4f matrix(ModelPart p){
        return HairLookAdjustment.transform(p.x,p.y,p.z,p.xRot,p.yRot,p.zRot).scale(p.xScale,p.yScale,p.zScale);
    }
    Vector3f target(ModelPart part,List<Vector3f[]> boxes){
        var rotation=HairLookAdjustment.hangingRotation(pitch,yaw,roll,new Vector3f(part.xRot,part.yRot,part.zRot));
        return HairLookAdjustment.attachedRotation(boxes,new Vector3f(part.x,part.y,part.z),rotation,chest);
    }
    void apply(ModelPart part,List<Vector3f[]> boxes){
        var rotation=target(part,boxes);
        part.xRot=rotation.x;part.yRot=rotation.y;part.zRot=rotation.z;
    }
}
