package com.example.immortal_cultivation_mod.client.qi;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;
public final class QiHandLayer<T extends LivingEntity,M extends HumanoidModel<T>> extends RenderLayer<T,M>{
    public QiHandLayer(RenderLayerParent<T,M> parent){super(parent);}
    @Override public void render(PoseStack pose,MultiBufferSource buffers,int light,T owner,float limbSwing,float limbAmount,float partial,float age,float yaw,float pitch){
        QiCoatingRenderer.hand(pose,buffers,owner,getParentModel().rightArm,true,partial,false);
        QiCoatingRenderer.hand(pose,buffers,owner,getParentModel().leftArm,false,partial,false);
    }
}
