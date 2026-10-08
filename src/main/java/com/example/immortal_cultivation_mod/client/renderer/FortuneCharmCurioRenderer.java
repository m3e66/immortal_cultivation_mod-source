package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.item.TribulationDollItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/** Small charms sit at separate waist positions, using the real item models without cords. */
public final class FortuneCharmCurioRenderer implements ICurioRenderer {
    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(
            ItemStack stack, SlotContext context, PoseStack poses, RenderLayerParent<T, M> parent,
            MultiBufferSource buffers, int light, float limbSwing, float limbSwingAmount,
            float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!context.visible() || context.entity().isInvisible()
                || !(parent.getModel() instanceof HumanoidModel<?> humanoid)) return;
        LivingEntity wearer = context.entity();
        int slot = Math.floorMod(context.index(), 3);
        double x = slot == 0 ? -.20 : slot == 2 ? .20 : 0;
        double y = .72;
        boolean doll = stack.getItem() instanceof TribulationDollItem;
        boolean coin = stack.is(ModItems.FORTUNE_GUARD_COIN.get());
        double z = doll || coin ? -.20 : -.26;
        float scale = doll ? .24F : coin ? .20F : .30F;
        poses.pushPose();
        // The torso already includes crouching, swimming, and riding. Applying the sneaking
        // helper again would shift the attachment twice.
        humanoid.body.translateAndRotate(poses);
        poses.translate(x, y, z);
        // Humanoid coordinates point down; item model coordinates point up. Keep the decorated
        // north face facing outward and avoid FIXED/GUI transforms intended for item frames.
        poses.mulPose(Axis.ZP.rotationDegrees(180));
        poses.scale(scale, scale, scale);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.NONE,
                light, OverlayTexture.NO_OVERLAY, poses, buffers, wearer.level(), wearer.getId() + context.index());
        poses.popPose();
    }
}
