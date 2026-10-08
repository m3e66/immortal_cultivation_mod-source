package com.example.immortal_cultivation_mod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

public final class GoldenGourdCurioRenderer implements ICurioRenderer {
    private final WaistModelRenderer renderer=new WaistModelRenderer();
    @Override public <T extends LivingEntity,M extends EntityModel<T>> void render(ItemStack stack,SlotContext context,PoseStack pose,
            RenderLayerParent<T,M> parent,MultiBufferSource buffers,int light,float limbSwing,float limbAmount,float partial,float age,float yaw,float pitch){
        if(!(parent.getModel() instanceof HumanoidModel<?> model))return;
        pose.pushPose();
        model.body.translateAndRotate(pose);
        // Positive body-local X moves right in a front view; tuck the gourd closer to the belt.
        pose.translate(.14,.62,-.23);
        // Hang upright while keeping the decorative straps facing outward.
        pose.mulPose(Axis.ZP.rotationDegrees(180));
        // Render the original geometry directly; no baked-item, sprite, or hand transforms.
        renderer.renderByItem(stack,ItemDisplayContext.FIXED,pose,buffers,light,OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
    private static final class WaistModelRenderer extends software.bernie.geckolib.renderer.GeoItemRenderer<com.example.immortal_cultivation_mod.pagoda.GoldenGourdItem> {
        WaistModelRenderer(){super(new software.bernie.geckolib.model.GeoModel<>(){
            @Override public net.minecraft.resources.ResourceLocation getModelResource(com.example.immortal_cultivation_mod.pagoda.GoldenGourdItem item){return com.example.immortal_cultivation_mod.pagoda.PagodaWorlds.id("geo/item/jin_hulu.geo.json");}
            @Override public net.minecraft.resources.ResourceLocation getTextureResource(com.example.immortal_cultivation_mod.pagoda.GoldenGourdItem item){return com.example.immortal_cultivation_mod.pagoda.PagodaWorlds.id("textures/item/jin_hulu_atlas.png");}
            @Override public net.minecraft.resources.ResourceLocation getAnimationResource(com.example.immortal_cultivation_mod.pagoda.GoldenGourdItem item){return com.example.immortal_cultivation_mod.pagoda.PagodaWorlds.id("animations/item/jin_hulu.animation.json");}
        });}
        @Override public void preRender(PoseStack pose,com.example.immortal_cultivation_mod.pagoda.GoldenGourdItem item,
                software.bernie.geckolib.cache.object.BakedGeoModel model,MultiBufferSource buffers,com.mojang.blaze3d.vertex.VertexConsumer vertices,
                boolean reRender,float partial,int light,int overlay,int color){
            super.preRender(pose,item,model,buffers,vertices,reRender,partial,light,overlay,color);
            if(!reRender)pose.translate(-.5,-.51,-.5);
            pose.scale(.26F,.26F,.26F);pose.translate(0,-1.7,0);
        }
    }
}
