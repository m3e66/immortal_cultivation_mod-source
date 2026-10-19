package com.example.immortal_cultivation_mod.client.qi;
import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.client.*;
import com.example.immortal_cultivation_mod.item.QiInfusedWeapon;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.event.GeoRenderEvent;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.RenderUtil;
@EventBusSubscriber(modid=ImmortalCultivationMod.MODID,value=Dist.CLIENT)
public final class QiGeoCoatingLayer<T extends Item & GeoAnimatable> extends GeoRenderLayer<T>{
    private final GeoItemRenderer<T> itemRenderer;
    public QiGeoCoatingLayer(GeoItemRenderer<T> renderer){super(renderer);itemRenderer=renderer;}
    @SubscribeEvent @SuppressWarnings({"rawtypes","unchecked"})
    public static void layers(GeoRenderEvent.Item.CompileRenderLayers event){event.addLayer(new QiGeoCoatingLayer(event.getRenderer()));}
    @Override public void renderForBone(PoseStack pose,T item,GeoBone bone,RenderType type,MultiBufferSource buffers,VertexConsumer original,float partial,int light,int overlay){
        var owner=WhipRenderContext.holder();
        if(owner==null||bone.isHidden()||bone.getCubes().isEmpty())return;
        var display=item.getAnimatableInstanceCache().getManagerForId(itemRenderer.getInstanceId(item)).getData(DataTickets.ITEM_RENDER_PERSPECTIVE);
        var stack=itemRenderer.getCurrentItemStack();
        if(!QiCoatingRenderer.held(display)||stack==null||!QiInfusedWeapon.isWeapon(stack)||QiInfusedWeapon.isFistWeapon(stack))return;
        var style=QiAuraRenderer.coating(owner,true);if(style==null||!QiAuraMaterial.ready()||!QiVfxQuality.takeCoating(bone.getCubes().size()*6))return;
        var look=QiElementLook.of(style);var out=buffers.getBuffer(QiAuraMaterial.ENERGY);
        Vec3 min=new Vec3(Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY);
        Vec3 max=new Vec3(Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY);
        var inverse=new org.joml.Matrix4f(pose.last().pose()).invert();
        for(var cube:bone.getCubes()){
            pose.pushPose();RenderUtil.translateToPivotPoint(pose,cube);RenderUtil.rotateMatrixAroundCube(pose,cube);RenderUtil.translateAwayFromPivotPoint(pose,cube);
            Vec3 center=Vec3.ZERO;int count=0;
            for(var q:cube.quads())if(q!=null)for(var vertex:q.vertices()){center=center.add(new Vec3(vertex.position()));count++;}
            if(count>0)center=center.scale(1D/count);
            float surge=QiCoatingRenderer.surge(owner);
            var local=new org.joml.Matrix4f(inverse).mul(pose.last().pose());
            for(var quad:cube.quads()){
                if(quad==null)continue;Vec3[] p=new Vec3[4];
                for(int i=0;i<4;i++){
                    p[i]=new Vec3(quad.vertices()[i].position());
                    var fitted=local.transformPosition(new org.joml.Vector3f((float)p[i].x,(float)p[i].y,(float)p[i].z));
                    min=new Vec3(Math.min(min.x,fitted.x),Math.min(min.y,fitted.y),Math.min(min.z,fitted.z));
                    max=new Vec3(Math.max(max.x,fitted.x),Math.max(max.y,fitted.y),Math.max(max.z,fitted.z));
                    double padding=(.025+surge*.04)*(1+.12*Math.sin((owner.level().getGameTime()+partial)*.32+p[i].y*12));
                    p[i]=p[i].add(Math.signum(p[i].x-center.x)*padding,Math.signum(p[i].y-center.y)*padding,Math.signum(p[i].z-center.z)*padding);
                }
                QiCoatingRenderer.quad(pose,out,p,new Vec3(quad.normal()),look,owner.level().getGameTime()+partial,QiCoatingRenderer.surge(owner));
            }
            pose.popPose();QiCoatingRenderer.geoCubes++;
        }
        if(Double.isFinite(min.x))QiWeaponFlames.bounds(pose,buffers,min,max,look,owner.level().getGameTime()+partial,
                QiCoatingRenderer.surge(owner),Math.floorMod(bone.getName().hashCode(),97)*.17);
        if(bone.getName().contains("tip")||bone.getName().contains("blade")||bone.getName().contains("head"))
            QiCoatingRenderer.trail(owner,"bone:"+bone.getName()+":"+display,
                    new Vec3(pose.last().pose().transformPosition(new org.joml.Vector3f())),buffers,look,
                    owner.level().getGameTime()+partial,QiCoatingRenderer.surge(owner));
    }
}
