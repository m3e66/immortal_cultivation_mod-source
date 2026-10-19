package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.entity.NpcSkinCatalog;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/** Reusable baked Minecraft models; never spawn entities or require a gameplay world. */
public final class LifeCardBattlefield {
    private PlayerModel<Player> human;
    private ModelPart fox;
    public void draw(GuiGraphics g,CompoundTag actor,int x,int feet,int size,float time,float lunge,String action,float progress,boolean hurt){
        Minecraft mc=Minecraft.getInstance();
        if(human==null){human=new PlayerModel<>(mc.getEntityModels().bakeLayer(ModelLayers.PLAYER),false);human.young=false;fox=mc.getEntityModels().bakeLayer(ModelLayers.FOX);}
        boolean enemy=actor.getBoolean("Enemy"),beast=actor.getString("Profile").equals("beast")||actor.getString("Sprite").startsWith("n");
        g.flush();RenderSystem.enableDepthTest();Lighting.setupForEntityInInventory();
        g.pose().pushPose();g.pose().translate(x+(enemy?-1:1)*lunge,feet,100);
        g.pose().scale(size,size,size);g.pose().mulPose(Axis.YP.rotationDegrees(enemy?125:-125));
        // Baked humanoid feet lie at y=1.5; put the boots on the arena floor.
        g.pose().translate(0,-1.5,0);
        int color=actor.getDouble("Hp")<=0?0xFF85858A:hurt?0xFFFFABAB:0xFFFFFFFF;
        if(beast){
            var texture=ResourceLocation.withDefaultNamespace("textures/entity/fox/fox.png");
            fox.render(g.pose(),g.bufferSource().getBuffer(RenderType.entityCutoutNoCull(texture)),LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY,color);
        }else{
            String identity=actor.getString("Sprite")+actor.getString("Id");
            var skins=NpcSkinCatalog.CULTIVATOR_SKINS;String skin=skins.get(Math.floorMod(identity.hashCode(),skins.size())).texture();
            var texture=NpcSkinCatalog.texture(skin);
            human.head.yRot=enemy?-.22F:.22F;human.head.xRot=0;
            float breathing=(float)Math.sin(time*.8)*.025F;
            human.rightArm.xRot=breathing-(lunge>1?.8F:0);human.leftArm.xRot=-breathing;
            human.rightArm.zRot=.06F;human.leftArm.zRot=-.06F;human.rightLeg.xRot=.025F;human.leftLeg.xRot=-.025F;
            float motion=(float)Math.sin(Math.PI*progress);
            if(action.equals("block")){human.rightArm.xRot=-1.15F*motion;human.leftArm.xRot=-1.15F*motion;human.rightArm.zRot=-.6F*motion;human.leftArm.zRot=.6F*motion;}
            else if(isSupport(action)){human.rightArm.xRot=-1.8F*motion;human.leftArm.xRot=-1.8F*motion;}
            else if(action.equals("attack")){human.rightArm.xRot=-2F*motion;human.leftArm.xRot=-.5F*motion;}
            else if(action.equals("dodge")||action.equals("run")){human.rightLeg.xRot=.8F*motion;human.leftLeg.xRot=-.8F*motion;}
            human.hat.copyFrom(human.head);human.rightSleeve.copyFrom(human.rightArm);human.leftSleeve.copyFrom(human.leftArm);
            human.rightPants.copyFrom(human.rightLeg);human.leftPants.copyFrom(human.leftLeg);human.jacket.copyFrom(human.body);
            human.renderToBuffer(g.pose(),g.bufferSource().getBuffer(RenderType.entityCutoutNoCull(texture)),LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY,color);
        }
        g.pose().popPose();g.flush();Lighting.setupFor3DItems();
    }
    private static boolean isSupport(String action){return java.util.List.of("heal","qi","heal_over_time","qi_over_time","buff","gather").contains(action);}
}
