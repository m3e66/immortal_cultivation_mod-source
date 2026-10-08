package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import com.example.immortal_cultivation_mod.magicritual.center.CenterEffectSupport;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Server-authoritative safe destination search and folded-space VFX for 遁空符. */
public final class VoidEscapeTalismanLogic {
    private static final String DISORDER_UNTIL =
            ImmortalCultivationMod.MODID + ":void_escape_disorder_until";
    private static final String DISORDER_STACKS =
            ImmortalCultivationMod.MODID + ":void_escape_disorder_stacks";
    private static final int FAILURE_QI_COST = 20;
    private static final int DISORDER_TICKS = 5 * 20;
    private static final double MIN_DISTANCE = 12.0D;
    private static final double MAX_DISTANCE = 20.0D;
    private static final DustParticleOptions SPACE_DUST = new DustParticleOptions(
            new Vector3f(0.62F, 0.96F, 1.0F), 0.72F);

    private VoidEscapeTalismanLogic() {
    }

    /** Finds the furthest safe point on the unobstructed part of the look ray. */
    @Nullable
    public static BlockPos findDestination(ServerLevel level, ServerPlayer player) {
        long now = level.getGameTime();
        int stacks = now < player.getPersistentData().getLong(DISORDER_UNTIL)
                ? Mth.clamp(player.getPersistentData().getInt(DISORDER_STACKS), 1, 3)
                : 0;
        double maximum = Math.max(MIN_DISTANCE, MAX_DISTANCE - stacks * 3.0D);
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 requestedEnd = eye.add(look.scale(maximum));
        BlockHitResult obstruction = level.clip(new ClipContext(
                eye, requestedEnd, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.ANY, player));
        double clearDistance = obstruction.getType() == HitResult.Type.MISS
                ? maximum
                : Math.max(1.0D, eye.distanceTo(obstruction.getLocation()) - 0.85D);

        // A nearby wall is allowed to shorten the jump; it must never be crossed.
        for (double distance = clearDistance; distance >= 1.0D; distance -= 0.5D) {
            Vec3 point = eye.add(look.scale(distance));
            BlockPos base = BlockPos.containing(point.x, point.y - 0.85D, point.z);
            for (BlockPos candidate : nearbyCandidates(base)) {
                Vec3 feet = Vec3.atBottomCenterOf(candidate);
                if (isSafe(level, candidate)
                        && !CenterEffectSupport.isForcefieldRouteBlocked(
                        player, player.position(), feet)) {
                    return candidate.immutable();
                }
            }
        }
        return null;
    }

    private static List<BlockPos> nearbyCandidates(BlockPos base) {
        List<BlockPos> candidates = new ArrayList<>();
        for (int vertical = 3; vertical >= -3; vertical--) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    candidates.add(base.offset(x, vertical, z));
                }
            }
        }
        candidates.sort(Comparator.comparingDouble(candidate ->
                candidate.distSqr(base)));
        return candidates;
    }

    private static boolean isSafe(ServerLevel level, BlockPos feet) {
        if (!level.getWorldBorder().isWithinBounds(feet)
                || feet.getY() <= level.getMinBuildHeight() + 1
                || feet.getY() >= level.getMaxBuildHeight() - 2) {
            return false;
        }
        BlockPos head = feet.above();
        BlockPos floor = feet.below();
        BlockState feetState = level.getBlockState(feet);
        BlockState headState = level.getBlockState(head);
        BlockState floorState = level.getBlockState(floor);
        if (!feetState.getCollisionShape(level, feet).isEmpty()
                || !headState.getCollisionShape(level, head).isEmpty()
                || floorState.getCollisionShape(level, floor).isEmpty()) {
            return false;
        }
        return level.getFluidState(feet).isEmpty()
                && level.getFluidState(head).isEmpty()
                && !level.getFluidState(floor).is(FluidTags.LAVA);
    }

    public static void consumeFailureQi(ServerPlayer player) {
        var data = ModAttachments.getData(player);
        int spent = Math.min(FAILURE_QI_COST, Math.max(0, data.qi()));
        if (spent > 0) {
            ModAttachments.setData(player, data.withQi(data.qi() - spent));
            ServerEvents.syncPlayerData(player);
        }
        player.displayClientMessage(Component.translatable(
                "message." + ImmortalCultivationMod.MODID
                        + ".talisman.void_escape.no_destination", spent), true);
    }

    public static boolean teleport(
            ServerLevel level, ServerPlayer player, @Nullable BlockPos destination) {
        if (destination == null || !isSafe(level, destination)) {
            consumeFailureQi(player);
            return false;
        }
        Vec3 target = Vec3.atBottomCenterOf(destination);
        if (CenterEffectSupport.isForcefieldRouteBlocked(
                player, player.position(), target)) {
            consumeFailureQi(player);
            return false;
        }

        Vec3 origin = player.position();
        emitFoldedSilhouette(level, origin, player.getBbWidth(), player.getBbHeight());
        level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS, 0.42F, 1.55F);
        player.teleportTo(level, target.x, target.y, target.z,
                player.getYRot(), player.getXRot());
        player.resetFallDistance();
        player.setDeltaMovement(player.getDeltaMovement().scale(0.35D));
        player.hurtMarked = true;
        emitFoldedSilhouette(level, target, player.getBbWidth(), player.getBbHeight());
        level.playSound(null, destination, SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS, 0.52F, 1.72F);

        long now = level.getGameTime();
        boolean alreadyDisordered = now < player.getPersistentData().getLong(DISORDER_UNTIL);
        int stacks = alreadyDisordered
                ? Mth.clamp(player.getPersistentData().getInt(DISORDER_STACKS) + 1, 1, 3)
                : 1;
        player.getPersistentData().putInt(DISORDER_STACKS, stacks);
        player.getPersistentData().putLong(DISORDER_UNTIL, now + DISORDER_TICKS);
        return true;
    }

    /** Thin curved spatial seams shown during the paper's one-second wind-up. */
    public static void emitPreparation(ServerLevel level, ServerPlayer player, int age) {
        if (age % 3 != 0) return;
        double phase = age * 0.19D;
        for (int seam = 0; seam < 3; seam++) {
            double base = phase + seam * Mth.TWO_PI / 3.0D;
            for (int point = 0; point < 5; point++) {
                double t = point / 4.0D;
                double angle = base + t * 0.72D;
                double radius = 0.58D + Math.sin(t * Math.PI) * 0.34D;
                level.sendParticles(SPACE_DUST,
                        player.getX() + Math.cos(angle) * radius,
                        player.getY() + 0.24D + t * player.getBbHeight() * 0.74D,
                        player.getZ() + Math.sin(angle) * radius,
                        1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    private static void emitFoldedSilhouette(
            ServerLevel level, Vec3 position, float width, float height) {
        for (int layer = 0; layer < 8; layer++) {
            double y = position.y + 0.10D + height * layer / 8.0D;
            double radius = width * (0.48D - Math.abs(layer - 3.5D) * 0.025D);
            for (int side = 0; side < 5; side++) {
                double angle = Mth.TWO_PI * side / 5.0D + layer * 0.22D;
                level.sendParticles(SPACE_DUST,
                        position.x + Math.cos(angle) * radius,
                        y,
                        position.z + Math.sin(angle) * radius,
                        1, 0.015D, 0.02D, 0.015D, 0.006D);
            }
        }
        level.sendParticles(ParticleTypes.END_ROD,
                position.x, position.y + height * 0.5D, position.z,
                8, width * 0.3D, height * 0.35D, width * 0.3D, 0.01D);
    }
}
