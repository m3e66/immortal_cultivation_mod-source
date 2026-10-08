package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.item.RigidSheathArmorRenderer;
import com.example.immortal_cultivation_mod.item.RigidSheathItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

public final class RigidSheathCurioRenderer implements ICurioRenderer {
    private final RigidSheathArmorRenderer swordEmpty =
            new RigidSheathArmorRenderer(RigidSheathItem.Kind.SWORD, false);
    private final RigidSheathArmorRenderer swordLoaded =
            new RigidSheathArmorRenderer(RigidSheathItem.Kind.SWORD, true);
    private final RigidSheathArmorRenderer daoEmpty =
            new RigidSheathArmorRenderer(RigidSheathItem.Kind.DAO, false);
    private final RigidSheathArmorRenderer daoLoaded =
            new RigidSheathArmorRenderer(RigidSheathItem.Kind.DAO, true);

    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(
            ItemStack stack, SlotContext slotContext, PoseStack poseStack,
            RenderLayerParent<T, M> renderLayerParent, MultiBufferSource buffer,
            int packedLight, float limbSwing, float limbSwingAmount, float partialTicks,
            float ageInTicks, float netHeadYaw, float headPitch) {
        if (!(stack.getItem() instanceof RigidSheathItem sheathItem)) {
            return;
        }
        if (!(renderLayerParent.getModel() instanceof HumanoidModel<?> humanoidModel)) {
            return;
        }

        boolean loaded = !RigidSheathItem.getStoredWeapon(stack, slotContext.entity().registryAccess()).isEmpty();
        RigidSheathArmorRenderer renderer = sheathItem.kind() == RigidSheathItem.Kind.DAO
                ? (loaded ? daoLoaded : daoEmpty)
                : (loaded ? swordLoaded : swordEmpty);

        poseStack.pushPose();
        ICurioRenderer.translateIfSneaking(poseStack, slotContext.entity());
        ICurioRenderer.followBodyRotations(slotContext.entity(), new HumanoidModel[]{renderer});
        renderer.prepForRender(slotContext.entity(), stack, EquipmentSlot.CHEST, humanoidModel,
                buffer, partialTicks, limbSwing, limbSwingAmount, netHeadYaw, headPitch);
        renderer.renderToBuffer(poseStack, (VertexConsumer) null, packedLight, OverlayTexture.NO_OVERLAY, -1);
        poseStack.popPose();
    }
}
