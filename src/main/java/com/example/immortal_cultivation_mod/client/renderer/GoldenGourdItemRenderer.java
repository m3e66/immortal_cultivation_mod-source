package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.pagoda.*;
import com.example.immortal_cultivation_mod.item.WeaponInventorySprite;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class GoldenGourdItemRenderer extends GeoItemRenderer<GoldenGourdItem> {
    public GoldenGourdItemRenderer(){super(new GeoModel<>(){
        @Override public ResourceLocation getModelResource(GoldenGourdItem item){return PagodaWorlds.id("geo/item/jin_hulu.geo.json");}
        @Override public ResourceLocation getTextureResource(GoldenGourdItem item){return PagodaWorlds.id("textures/item/jin_hulu_atlas.png");}
        @Override public ResourceLocation getAnimationResource(GoldenGourdItem item){return PagodaWorlds.id("animations/item/jin_hulu.animation.json");}
    });}
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(WeaponInventorySprite.usesSprite(context)){WeaponInventorySprite.render("jin_hulu",pose,buffers,light,overlay);return;}
        super.renderByItem(stack,context,pose,buffers,light,overlay);
    }
    @Override public void preRender(PoseStack pose,GoldenGourdItem item,software.bernie.geckolib.cache.object.BakedGeoModel model,
            MultiBufferSource buffers,com.mojang.blaze3d.vertex.VertexConsumer vertices,boolean reRender,float partial,int light,int overlay,int color){
        super.preRender(pose,item,model,buffers,vertices,reRender,partial,light,overlay,color);
        // GeckoLib already centers the item. The original model spans 0..32 pixels in Y.
        pose.scale(.35F,.35F,.35F);pose.translate(0,-1,0);
    }
}
