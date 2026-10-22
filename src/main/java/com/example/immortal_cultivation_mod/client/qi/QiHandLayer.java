package com.example.immortal_cultivation_mod.client.qi;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;
public final class QiHandLayer<T extends LivingEntity,M extends EntityModel<T>> extends RenderLayer<T,M>{
    public QiHandLayer(RenderLayerParent<T,M> parent){super(parent);}
    @Override public void render(PoseStack pose,MultiBufferSource buffers,int light,T owner,float limbSwing,float limbAmount,float partial,float age,float yaw,float pitch){
        // Modded renderers may replace their model after layer registration or per entity.
        // Keep the generic bound broad so getParentModel() does not insert a cast first.
        if (!(getParentModel() instanceof HumanoidModel<?> humanoid)) return;
        QiCoatingRenderer.hand(pose,buffers,owner,humanoid.rightArm,true,partial,false);
        QiCoatingRenderer.hand(pose,buffers,owner,humanoid.leftArm,false,partial,false);
    }
}
