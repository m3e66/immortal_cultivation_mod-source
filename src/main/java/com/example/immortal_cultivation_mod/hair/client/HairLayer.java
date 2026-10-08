package com.example.immortal_cultivation_mod.hair.client;

import com.example.immortal_cultivation_mod.hair.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EquipmentSlot;

public final class HairLayer extends RenderLayer<AbstractClientPlayer,PlayerModel<AbstractClientPlayer>> {
    public HairLayer(PlayerRenderer renderer) {super(renderer);}
    @Override public void render(PoseStack pose,MultiBufferSource buffer,int light,AbstractClientPlayer player,
            float limb,float amount,float partial,float age,float yaw,float pitch) {
        var mc=Minecraft.getInstance();
        if(player.isInvisible() || player==mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson())return;
        var hair=HairData.equipped(player,"hair");var accessory=HairData.equipped(player,"hair_accessory");
        if(hair.isEmpty()&&accessory.isEmpty())return;
        var motion=HairChestPose.of(player,getParentModel());
        float pressure = com.example.immortal_cultivation_mod.client.RealmPressureClient.strengthAt(player.position());
        pose.pushPose();getParentModel().head.translateAndRotate(pose);
        HairGeometry.render(pose,buffer,light,HairData.visible(player,"hair")?HairData.style(hair):-1,HairData.color(hair),
                HairData.visible(player,"hair_accessory")?HairData.accessory(accessory):-1,
                HairData.helmet(hair),!player.getItemBySlot(EquipmentSlot.HEAD).isEmpty(),age,amount,pitch,motion,pressure);
        pose.popPose();
    }
}
