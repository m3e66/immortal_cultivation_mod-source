package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.block.ScaledDingInteractionSupport;
import com.example.immortal_cultivation_mod.network.ScaledDingInteractionPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

/** Redirects clicks on a ding's overhanging geometry to its anchor block at every size. */
public final class ScaledDingInteractionClient {
    private static boolean registered;

    private ScaledDingInteractionClient() {
    }

    public static void register() {
        if (registered) return;
        registered = true;
        NeoForge.EVENT_BUS.addListener(
                ScaledDingInteractionClient::onInteractionKey);
    }

    private static void onInteractionKey(
            InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null
                || minecraft.getConnection() == null
                || minecraft.screen != null) {
            return;
        }

        Vec3 start = minecraft.player.getEyePosition();
        Vec3 end = start.add(minecraft.player.getLookAngle().scale(
                minecraft.player.blockInteractionRange()));
        double maximumDistanceSquared = start.distanceToSqr(end);
        HitResult vanillaHit = minecraft.hitResult;
        if (vanillaHit != null
                && vanillaHit.getType() != HitResult.Type.MISS) {
            maximumDistanceSquared = Math.min(maximumDistanceSquared,
                    start.distanceToSqr(vanillaHit.getLocation())
                            + 1.0E-6D);
        }

        BlockHitResult scaledHit =
                ScaledDingInteractionSupport.findNearestScaledDing(
                        minecraft.level, start, end,
                        CollisionContext.of(minecraft.player),
                        maximumDistanceSquared);
        if (scaledHit == null) return;

        event.setCanceled(true);
        ThoughtInputGuard.handled(event);
        event.setSwingHand(true);
        Vec3 location = scaledHit.getLocation();
        PacketDistributor.sendToServer(
                new ScaledDingInteractionPayloads
                        .ServerboundUseScaledDingPayload(
                        scaledHit.getBlockPos(),
                        location.x, location.y, location.z));
    }
}
