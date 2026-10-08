package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.item.CultivationGarmentItem;
import com.example.immortal_cultivation_mod.client.model.CultivationGarmentGeoModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.util.RenderUtil;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/** Uses the posed humanoid bones, including the current crouch and leg offsets. */
public final class CultivationGarmentRenderer implements ICurioRenderer {
    private final GarmentArmorRenderer renderer;

    public CultivationGarmentRenderer(String model) { renderer = new GarmentArmorRenderer(model); }

    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack,
            SlotContext context, PoseStack pose, RenderLayerParent<T, M> parent,
            MultiBufferSource buffers, int light, float limbSwing, float limbAmount,
            float partial, float age, float yaw, float pitch) {
        if (context.entity().isInvisible() || !(parent.getModel() instanceof HumanoidModel<?> humanoid)) return;
        pose.pushPose();
        ((CultivationGarmentGeoModel)renderer.getGeoModel()).prepare(context.entity(), partial, humanoid);
        renderer.prepForRender(context.entity(), stack, EquipmentSlot.CHEST, humanoid,
                buffers, partial, limbSwing, limbAmount, yaw, pitch);
        renderer.renderToBuffer(pose, null, light, OverlayTexture.NO_OVERLAY, -1);
        pose.popPose();
    }

    private static final class GarmentArmorRenderer extends GeoArmorRenderer<CultivationGarmentItem> {
        private final boolean translucentVeil;
        private final ResourceLocation[] wearTextures;
        GarmentArmorRenderer(String model) {
            super(new CultivationGarmentGeoModel(model));
            translucentVeil = model.equals("veiled_douli");
            wearTextures = com.example.immortal_cultivation_mod.item.ClothingWear.textures(model);
        }

        @Override
        public ResourceLocation getTextureLocation(CultivationGarmentItem item) {
            return wearTextures[com.example.immortal_cultivation_mod.item.ClothingWear.stage(getCurrentStack())];
        }

        private boolean renderingVeil;
        private boolean renderingCape;

        @Override
        public void renderRecursively(PoseStack pose, CultivationGarmentItem item,
                software.bernie.geckolib.cache.object.GeoBone bone,
                net.minecraft.client.renderer.RenderType type, MultiBufferSource buffers,
                VertexConsumer buffer, boolean reRender, float partialTick, int light, int overlay, int colour) {
            boolean previous=renderingVeil;
            boolean previousCape=renderingCape;
            renderingCape=bone.getName().equals("capeSheet");
            renderingVeil=translucentVeil && bone.getName().equals("veilCloth");
            if(renderingVeil) {
                type=SheerClothRenderPass.type(getTextureLocation(item));
                buffers=SheerClothRenderPass.buffers();
                buffer=buffers.getBuffer(type);
            }
            try {
                super.renderRecursively(pose,item,bone,type,buffers,buffer,reRender,partialTick,light,overlay,colour);
            } finally {
                renderingVeil=previous;
                renderingCape=previousCape;
            }
        }

        @Override
        public void renderCube(PoseStack pose, GeoCube cube, VertexConsumer buffer,
                               int light, int overlay, int colour) {
            CultivationGarmentGeoModel cloth = (CultivationGarmentGeoModel)getGeoModel();
            if (!renderingCape && !renderingVeil) {
                super.renderCube(pose, cube, buffer, light, overlay, colour);
                return;
            }
            PoseStack local = new PoseStack();
            RenderUtil.translateToPivotPoint(local, cube);
            RenderUtil.rotateMatrixAroundCube(local, cube);
            RenderUtil.translateAwayFromPivotPoint(local, cube);
            Matrix4f rest = local.last().pose();
            Matrix4f world = pose.last().pose();
            for (GeoQuad quad : cube.quads()) {
                if (quad == null) continue;
                // Subdivide long faces so the same smooth bend crosses every seam.
                int rows=renderingCape?24:6,columns=renderingCape?20:2;
                for (int row=0; row<rows; row++) for (int col=0; col<columns; col++) {
                    float u0=col/(float)columns, u1=(col+1)/(float)columns;
                    float v0=row/(float)rows, v1=(row+1)/(float)rows;
                    Vector3f a=point(quad,u0,v0,rest,world,cloth);
                    Vector3f b=point(quad,u1,v0,rest,world,cloth);
                    Vector3f c=point(quad,u1,v1,rest,world,cloth);
                    Vector3f d=point(quad,u0,v1,rest,world,cloth);
                    Vector3f normal=new Vector3f(b).sub(a).cross(new Vector3f(d).sub(a));
                    if (normal.lengthSquared()<1e-12F) continue;
                    normal.normalize();
                    Vector3f reference=rest.transformDirection(new Vector3f(quad.normal()));
                    pose.last().normal().transform(reference);
                    if (normal.dot(reference)<0) normal.negate();
                    emit(buffer,quad,a,u0,v0,normal,light,overlay,colour);
                    emit(buffer,quad,b,u1,v0,normal,light,overlay,colour);
                    emit(buffer,quad,c,u1,v1,normal,light,overlay,colour);
                    emit(buffer,quad,d,u0,v1,normal,light,overlay,colour);
                }
            }
        }

        private static Vector3f point(GeoQuad quad,float u,float v,Matrix4f rest,Matrix4f world,
                                      CultivationGarmentGeoModel cloth) {
            GeoVertex[] vertices=quad.vertices();
            Vector3f top=new Vector3f(vertices[0].position()).lerp(vertices[1].position(),u);
            Vector3f bottom=new Vector3f(vertices[3].position()).lerp(vertices[2].position(),u);
            Vector3f position=rest.transformPosition(top.lerp(bottom,v));
            return world.transformPosition(cloth.deform(position));
        }

        private static void emit(VertexConsumer buffer,GeoQuad quad,Vector3f p,float u,float v,
                                 Vector3f normal,int light,int overlay,int colour) {
            GeoVertex[] q=quad.vertices();
            float tu=(q[0].texU()*(1-u)+q[1].texU()*u)*(1-v)+(q[3].texU()*(1-u)+q[2].texU()*u)*v;
            float tv=(q[0].texV()*(1-u)+q[1].texV()*u)*(1-v)+(q[3].texV()*(1-u)+q[2].texV()*u)*v;
            buffer.addVertex(p.x,p.y,p.z,colour,tu,tv,overlay,light,normal.x,normal.y,normal.z);
        }

        @Override
        protected void applyBoneVisibilityBySlot(EquipmentSlot slot) {
            super.applyBoneVisibilityBySlot(slot);
            // A single Curio may cover the head, torso and both legs.
            setBoneVisible(head, true);
            setBoneVisible(body, true);
            setBoneVisible(rightArm, true);
            setBoneVisible(leftArm, true);
            setBoneVisible(rightLeg, true);
            setBoneVisible(leftLeg, true);
            setBoneVisible(rightBoot, true);
            setBoneVisible(leftBoot, true);
        }
    }
}
