package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.pagoda.*;
import com.example.immortal_cultivation_mod.item.WeaponInventorySprite;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class PagodaItemRenderer extends GeoItemRenderer<PagodaItem> {
    public PagodaItemRenderer(){super(new GeoModel<>(){
        @Override public ResourceLocation getModelResource(PagodaItem e){return PagodaWorlds.id("geo/item/baota.geo.json");}
        @Override public ResourceLocation getTextureResource(PagodaItem e){return PagodaWorlds.id("textures/item/baota_atlas.png");}
        @Override public ResourceLocation getAnimationResource(PagodaItem e){return PagodaWorlds.id("animations/item/baota.animation.json");}
        @Override public void setCustomAnimations(PagodaItem item,long instance,software.bernie.geckolib.animation.AnimationState<PagodaItem> state){
            super.setCustomAnimations(item,instance,state);getBone("breakout_cracks").ifPresent(bone->bone.setHidden(true));
        }
    });}
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(WeaponInventorySprite.usesSprite(context)||!PagodaRenderer.assetsReady()){WeaponInventorySprite.render("baota",pose,buffers,light,overlay);return;}
        super.renderByItem(stack,context,pose,buffers,light,overlay);
    }
    @Override public void preRender(PoseStack pose,PagodaItem item,BakedGeoModel model,MultiBufferSource buffers,
            com.mojang.blaze3d.vertex.VertexConsumer vertices,boolean reRender,float partial,int light,int overlay,int color){
        super.preRender(pose,item,model,buffers,vertices,reRender,partial,light,overlay,color);
        pose.scale(.075F,.075F,.075F);pose.translate(0,-71.75/16,0);
    }
}
