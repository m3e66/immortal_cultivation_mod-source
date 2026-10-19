package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.client.model.TrainingDummyGeoModel;
import com.example.immortal_cultivation_mod.entity.TrainingDummyEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

/** Ivory puppet visuals, live cloth and the dummy's actual equipped weapons. */
public final class TrainingDummyRenderer extends GeoEntityRenderer<TrainingDummyEntity> {
    @Override protected float getDeathMaxRotation(TrainingDummyEntity entity){return 0;}
    public TrainingDummyRenderer(EntityRendererProvider.Context context) {
        super(context,new TrainingDummyGeoModel());
        withScale(TrainingDummyGeoModel.RENDER_SCALE);shadowRadius=.45F;
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        addRenderLayer(new BlockAndItemGeoLayer<TrainingDummyEntity>(this) {
            @Override protected ItemStack getStackForBone(GeoBone bone,TrainingDummyEntity e) {
                boolean right=bone.getName().equals("hand_r"),left=bone.getName().equals("hand_l");
                if(!right&&!left)return null;
                return right==(e.getMainArm()==HumanoidArm.RIGHT)?e.getMainHandItem():e.getOffhandItem();
            }
            @Override protected ItemDisplayContext getTransformTypeForStack(GeoBone bone,ItemStack stack,TrainingDummyEntity e) {
                return bone.getName().equals("hand_r")?ItemDisplayContext.THIRD_PERSON_RIGHT_HAND:ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
            }
            @Override protected void renderStackForBone(PoseStack poses,GeoBone bone,ItemStack stack,TrainingDummyEntity e,
                    MultiBufferSource source,float partial,int light,int overlay) {
                if(stack.isEmpty())return;
                int sign=bone.getName().equals("hand_l")?1:-1;
                var up=new Vector3f(-sign*.7F,2.2F,.3F).normalize();
                poses.mulPose(new Quaternionf().rotationTo(new Vector3f(0,1,0),up).rotateY(-sign*(float)Math.PI/2));
                poses.translate(0,-3.1/16.0,-.15/16.0);
                poses.mulPose(Axis.XP.rotationDegrees(-90));poses.mulPose(Axis.YP.rotationDegrees(180));
                super.renderStackForBone(poses,bone,stack,e,source,partial,light,overlay);
            }
        });
    }
}
