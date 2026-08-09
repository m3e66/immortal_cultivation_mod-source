package com.example.immortal_cultivation_mod.magicritual.center.behavior;

import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import com.example.immortal_cultivation_mod.magicritual.block.custom.MagicCenterBlockEntity;
import com.example.immortal_cultivation_mod.magicritual.center.CenterContext;
import com.example.immortal_cultivation_mod.magicritual.center.CenterFuelController;
import com.example.immortal_cultivation_mod.magicritual.center.CenterTargetPolicy;
import com.example.immortal_cultivation_mod.magicritual.center.MagicCenterBehavior;
import com.example.immortal_cultivation_mod.magicritual.center.MagicCenterTypeRegistry;
import com.example.immortal_cultivation_mod.magicritual.item.ModItems;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** A linked, on-demand formation. It has no passive fuel drain. */
public final class TeleportCenterBehavior implements MagicCenterBehavior {
    public static final int BASE_CHANNEL_TICKS = 5 * 20;
    public static final int HASTE_REDUCTION_TICKS = 2 * 20;
    public static final int MIN_CHANNEL_TICKS = 10;
    public static final int QI_COST_PER_ENTITY = 100;
    public static final long ENERGY_PER_BLOCK_PER_ENTITY = 10L;
    private static final int AUTOMATIC_RETRY_TICKS = 20;
    private static final Map<ServerLevel, Map<UUID, AutomaticTeleportState>>
            AUTOMATIC_TELEPORT_STATES = new WeakHashMap<>();

    @Override
    public void serverTick(CenterContext context) {
        if (context.center().isTemporary()) {
            tickAutomaticTeleport(context);
        } else {
            tickChannel(context);
        }
    }

    @Override
    public int getFuelCostPerMinute(CenterContext context) {
        return 0;
    }

    public static boolean beginChannel(MagicCenterBlockEntity center, ServerPlayer player) {
        if (!(center.getLevel() instanceof ServerLevel level)
                || !MagicCenterTypeRegistry.TELEPORT.equals(center.getCenterTypeId())
                || center.isTemporary()) {
            return false;
        }
        if (!CenterTargetPolicy.canEdit(center, player)) {
            player.displayClientMessage(Component.translatable(
                    "message.immortal_cultivation_mod.center.not_authorized"), true);
            return false;
        }
        if (!center.isEnabled()) {
            player.displayClientMessage(Component.translatable(
                    "message.immortal_cultivation_mod.teleport.disabled"), true);
            return false;
        }
        if (!center.isInsideRadius(player)) {
            player.displayClientMessage(Component.translatable(
                    "message.immortal_cultivation_mod.teleport.outside_radius"), true);
            return false;
        }
        if (!center.canTeleportOut()) {
            player.displayClientMessage(Component.translatable(
                    center.hasTeleportLink()
                            ? "message.immortal_cultivation_mod.teleport.one_way_blocked"
                            : "message.immortal_cultivation_mod.teleport.not_linked"), true);
            return false;
        }
        if (center.resolveTeleportTarget() == null) {
            player.displayClientMessage(Component.translatable(
                    "message.immortal_cultivation_mod.teleport.destination_missing"), true);
            return false;
        }
        if (center.hasTeleportChannel()) {
            player.displayClientMessage(Component.translatable(
                    "message.immortal_cultivation_mod.teleport.busy"), true);
            return false;
        }

        int duration = Math.max(MIN_CHANNEL_TICKS,
                BASE_CHANNEL_TICKS - center.countHasteFlags() * HASTE_REDUCTION_TICKS);
        center.beginTeleportChannel(player.getUUID(), level.getGameTime(), duration,
                player.getHealth(), player.hurtTime);
        level.playSound(null, center.getBlockPos(), SoundEvents.BEACON_ACTIVATE,
                SoundSource.BLOCKS, 0.9F, 1.45F);
        player.displayClientMessage(Component.translatable(
                "message.immortal_cultivation_mod.teleport.channel_started",
                String.format(java.util.Locale.ROOT, "%.1f", duration / 20.0D)), true);
        return true;
    }

    private static void tickAutomaticTeleport(CenterContext context) {
        MagicCenterBlockEntity source = context.center();
        if (!source.isEnabled() || !source.canTeleportOut()) {
            clearAutomaticState(context.level(), source.getCenterId());
            return;
        }
        MagicCenterBlockEntity.ResolvedTeleportTarget resolved = source.resolveTeleportTarget();
        if (resolved == null) {
            clearAutomaticState(context.level(), source.getCenterId());
            return;
        }

        List<Entity> roots = collectTeleportRoots(context.level(), source);
        Set<UUID> currentRoots = new HashSet<>();
        for (Entity root : roots) {
            currentRoots.add(root.getUUID());
        }

        AutomaticTeleportState state = automaticState(context.level(), source.getCenterId());
        state.insideRoots.retainAll(currentRoots);
        List<Entity> entrants = roots.stream()
                .filter(root -> !state.insideRoots.contains(root.getUUID()))
                .toList();
        if (entrants.isEmpty()) {
            return;
        }

        long gameTime = context.level().getGameTime();
        if (gameTime < state.nextAttemptTime) {
            return;
        }
        state.nextAttemptTime = gameTime + AUTOMATIC_RETRY_TICKS;

        UUID ownerId = source.getOwnerUuid();
        ServerPlayer payer = ownerId == null ? null
                : context.level().getServer().getPlayerList().getPlayer(ownerId);
        if (payer == null || !payer.isAlive()) {
            return;
        }

        if (!completeTeleport(context, payer, entrants, true)) {
            return;
        }

        Set<UUID> movedRoots = new HashSet<>();
        for (Entity entrant : entrants) {
            movedRoots.add(entrant.getUUID());
        }
        state.insideRoots.addAll(movedRoots);
        automaticState(resolved.level(), resolved.center().getCenterId())
                .insideRoots.addAll(movedRoots);
    }

    private static void tickChannel(CenterContext context) {
        MagicCenterBlockEntity center = context.center();
        if (!center.hasTeleportChannel()) {
            return;
        }
        ServerPlayer player = context.level().getServer().getPlayerList()
                .getPlayer(center.getTeleportChannelPlayer());
        if (player == null || player.serverLevel() != context.level() || !player.isAlive()) {
            center.clearTeleportChannel();
            return;
        }
        if (!center.isEnabled() || !CenterTargetPolicy.canEdit(center, player)
                || !center.isInsideRadius(player)) {
            cancel(center, player, "message.immortal_cultivation_mod.teleport.channel_moved");
            return;
        }
        if (player.getHealth() < center.getTeleportChannelStartHealth()
                || player.hurtTime > center.getTeleportChannelObservedHurtTime()) {
            cancel(center, player, "message.immortal_cultivation_mod.teleport.channel_damaged");
            return;
        }
        center.observeTeleportHurtTime(player.hurtTime);
        if (!center.canTeleportOut() || center.resolveTeleportTarget() == null) {
            cancel(center, player, "message.immortal_cultivation_mod.teleport.destination_missing");
            return;
        }

        long elapsed = context.level().getGameTime() - center.getTeleportChannelStartTime();
        if (elapsed < center.getTeleportChannelDurationTicks()) {
            if ((context.level().getGameTime() & 3L) == 0L) {
                double progress = Math.min(1.0D,
                        elapsed / (double)Math.max(1, center.getTeleportChannelDurationTicks()));
                context.level().sendParticles(ParticleTypes.REVERSE_PORTAL,
                        player.getX(), player.getY() + player.getBbHeight() * progress,
                        player.getZ(), 5, 0.45D, 0.08D, 0.45D, 0.015D);
            }
            return;
        }
        completeTeleport(context, player,
                collectTeleportRoots(context.level(), context.center()), false);
    }

    private static boolean completeTeleport(CenterContext context, ServerPlayer activator,
                                            List<Entity> roots, boolean automatic) {
        MagicCenterBlockEntity source = context.center();
        MagicCenterBlockEntity.ResolvedTeleportTarget resolved = source.resolveTeleportTarget();
        if (resolved == null) {
            reject(source, activator,
                    "message.immortal_cultivation_mod.teleport.destination_missing", automatic);
            return false;
        }

        int entityCount = roots.stream().mapToInt(TeleportCenterBehavior::entityTreeSize).sum();
        if (entityCount <= 0) {
            reject(source, activator,
                    "message.immortal_cultivation_mod.teleport.no_entities", automatic);
            return false;
        }

        long qiCost = saturatingMultiply(QI_COST_PER_ENTITY, entityCount);
        long distanceBlocks = (long)Math.ceil(source.centerPoint().distanceTo(
                resolved.center().centerPoint()));
        long baseEnergy = saturatingMultiply(
                saturatingMultiply(ENERGY_PER_BLOCK_PER_ENTITY, distanceBlocks), entityCount);
        long energyCost = adjustedEnergyCost(source, baseEnergy);

        if (!activator.getAbilities().instabuild) {
            int availableQi = Math.max(0, ModAttachments.getData(activator).qi());
            if (qiCost > availableQi) {
                reject(source, activator,
                        "message.immortal_cultivation_mod.teleport.insufficient_qi", automatic);
                return false;
            }
            if (!hasAvailableTeleportEnergy(source, activator, energyCost)) {
                reject(source, activator,
                        "message.immortal_cultivation_mod.teleport.insufficient_energy", automatic);
                return false;
            }
            var data = ModAttachments.getData(activator);
            ModAttachments.setData(activator, data.withQi(data.qi() - (int)qiCost));
            ServerEvents.syncPlayerData(activator);
            if (!consumeAvailableTeleportEnergy(source, activator, energyCost)) {
                ModAttachments.setData(activator, data);
                ServerEvents.syncPlayerData(activator);
                reject(source, activator,
                        "message.immortal_cultivation_mod.teleport.insufficient_energy", automatic);
                return false;
            }
        }

        Vec3 sourcePoint = source.centerPoint();
        Vec3 destinationPoint = resolved.center().centerPoint();
        source.clearTeleportChannel();
        context.level().sendParticles(ParticleTypes.PORTAL,
                sourcePoint.x, sourcePoint.y + 0.8D, sourcePoint.z,
                70, Math.max(0.5D, source.getRadius() * 0.35D), 0.8D,
                Math.max(0.5D, source.getRadius() * 0.35D), 0.15D);
        context.level().playSound(null, source.getBlockPos(), SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.BLOCKS, 1.2F, 0.75F);

        int movedCount = 0;
        for (int index = 0; index < roots.size(); index++) {
            Entity root = roots.get(index);
            if (root.isRemoved()) {
                continue;
            }
            Vec3 offset = root.position().subtract(sourcePoint);
            double horizontalOffset = Math.sqrt(offset.x * offset.x + offset.z * offset.z);
            double maxTargetOffset = Math.max(0.5D,
                    resolved.center().getRadius() - 0.75D);
            if (horizontalOffset > maxTargetOffset) {
                double scale = maxTargetOffset / horizontalOffset;
                offset = new Vec3(offset.x * scale, offset.y, offset.z * scale);
            }
            Vec3 preferred = destinationPoint.add(offset);
            Vec3 destination = findSafeDestination(
                    resolved.level(), root, preferred, resolved.center().getBlockPos(), index);
            DimensionTransition transition = new DimensionTransition(
                    resolved.level(), destination, Vec3.ZERO,
                    root.getYRot(), root.getXRot(), moved -> {
                        moved.setDeltaMovement(Vec3.ZERO);
                        moved.resetFallDistance();
                    });
            int treeSize = entityTreeSize(root);
            if (root.changeDimension(transition) != null) {
                movedCount += treeSize;
            }
        }

        resolved.level().sendParticles(ParticleTypes.REVERSE_PORTAL,
                destinationPoint.x, destinationPoint.y + 0.8D, destinationPoint.z,
                70, Math.max(0.5D, resolved.center().getRadius() * 0.35D), 0.8D,
                Math.max(0.5D, resolved.center().getRadius() * 0.35D), 0.15D);
        resolved.level().playSound(null, resolved.center().getBlockPos(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1.2F, 1.25F);
        activator.displayClientMessage(Component.translatable(
                "message.immortal_cultivation_mod.teleport.complete",
                movedCount, qiCost, energyCost), true);
        return true;
    }

    private static List<Entity> collectTeleportRoots(ServerLevel level,
                                                      MagicCenterBlockEntity center) {
        double radius = center.getRadius();
        AABB bounds = new AABB(center.getBlockPos()).inflate(radius);
        Set<Entity> roots = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Entity entity : level.getEntities((Entity)null, bounds,
                candidate -> candidate.isAlive()
                        && center.isInsideRadius(candidate)
                        && (!(candidate instanceof net.minecraft.world.entity.player.Player player)
                        || !player.isSpectator()))) {
            Entity root = entity.getRootVehicle();
            if (root.isAlive()) {
                roots.add(root);
            }
        }
        return new ArrayList<>(roots);
    }

    private static int entityTreeSize(Entity root) {
        return (int)Math.min(Integer.MAX_VALUE, root.getSelfAndPassengers().count());
    }

    private static long adjustedEnergyCost(MagicCenterBlockEntity center, long baseCost) {
        double cost = Math.max(0L, baseCost);
        int gathering = center.countSpiritGatheringFlags();
        int haste = center.countHasteFlags();
        int ordinary = Math.max(0, center.countFormationFlags() - gathering - haste);
        for (int i = 0; i < ordinary; i++) {
            cost *= 1.5D;
        }
        for (int i = 0; i < gathering; i++) {
            cost *= 0.75D;
        }
        for (int i = 0; i < haste; i++) {
            cost *= 1.30D;
        }
        return cost >= Long.MAX_VALUE ? Long.MAX_VALUE : (long)Math.ceil(cost);
    }

    private static boolean hasAvailableTeleportEnergy(MagicCenterBlockEntity source,
                                                      ServerPlayer activator,
                                                      long requestedEnergy) {
        long cost = Math.max(0L, requestedEnergy);
        if (source.hasTeleportEnergy(cost)) {
            return true;
        }
        if (!source.isTemporary()) {
            return false;
        }
        long available = sourceAvailableEnergy(source);
        for (int slot = 0; slot < activator.getInventory().getContainerSize(); slot++) {
            ItemStack stack = activator.getInventory().getItem(slot);
            if (stack.is(ModItems.INFINITE_SPIRIT_STONE.get())) {
                return true;
            }
            int value = CenterFuelController.fuelValue(stack);
            if (value > 0) {
                available = saturatingAdd(available,
                        saturatingMultiply(value, stack.getCount()));
                if (available >= cost) {
                    return true;
                }
            }
        }
        return available >= cost;
    }

    private static boolean consumeAvailableTeleportEnergy(MagicCenterBlockEntity source,
                                                          ServerPlayer activator,
                                                          long requestedEnergy) {
        long cost = Math.max(0L, requestedEnergy);
        if (source.consumeTeleportEnergy(cost)) {
            return true;
        }
        if (!source.isTemporary()
                || !hasAvailableTeleportEnergy(source, activator, cost)) {
            return false;
        }
        for (int slot = 0; slot < activator.getInventory().getContainerSize(); slot++) {
            if (activator.getInventory().getItem(slot)
                    .is(ModItems.INFINITE_SPIRIT_STONE.get())) {
                return true;
            }
        }

        transferTemporaryFuelTier(source, activator,
                ModItems.SPIRIT_STONE.get(), 1, cost);
        transferTemporaryFuelTier(source, activator,
                ModItems.MEDIUM_SPIRIT_STONE.get(), 10, cost);
        transferTemporaryFuelTier(source, activator,
                ModItems.HIGH_SPIRIT_STONE.get(), 100, cost);
        activator.getInventory().setChanged();
        return source.consumeTeleportEnergy(cost);
    }

    private static void transferTemporaryFuelTier(MagicCenterBlockEntity source,
                                                  ServerPlayer activator,
                                                  Item stone, int value,
                                                  long requestedEnergy) {
        for (int slot = 0; slot < activator.getInventory().getContainerSize(); slot++) {
            long missing = Math.max(0L,
                    requestedEnergy - sourceAvailableEnergy(source));
            if (missing == 0L) {
                return;
            }
            ItemStack stack = activator.getInventory().getItem(slot);
            if (!stack.is(stone)) {
                continue;
            }
            long neededItems = 1L + (missing - 1L) / value;
            int consumed = (int)Math.min(stack.getCount(), neededItems);
            if (consumed <= 0) {
                continue;
            }
            stack.shrink(consumed);
            source.addEnergy(saturatingMultiply(value, consumed));
        }
    }

    private static long sourceAvailableEnergy(MagicCenterBlockEntity source) {
        ItemStack storedFuel = source.getFuelSlot().getStackInSlot(0);
        int storedValue = CenterFuelController.fuelValue(storedFuel);
        return saturatingAdd(source.getEnergy(),
                saturatingMultiply(storedValue, storedFuel.getCount()));
    }

    private static Vec3 findSafeDestination(ServerLevel level, Entity entity,
                                            Vec3 preferred, BlockPos centerPos, int index) {
        for (int vertical = 0; vertical <= 5; vertical++) {
            for (int ring = 0; ring <= 4; ring++) {
                int samples = ring == 0 ? 1 : ring * 8;
                for (int sample = 0; sample < samples; sample++) {
                    double angle = samples == 1 ? 0.0D
                            : Math.PI * 2.0D * (sample + index * 0.37D) / samples;
                    Vec3 candidate = preferred.add(
                            Math.cos(angle) * ring, vertical, Math.sin(angle) * ring);
                    BlockPos candidatePos = BlockPos.containing(candidate);
                    if (!level.getWorldBorder().isWithinBounds(candidatePos)) {
                        continue;
                    }
                    level.getChunkAt(candidatePos);
                    AABB movedBox = entity.getBoundingBox().move(
                            candidate.subtract(entity.position()));
                    BlockState below = level.getBlockState(candidatePos.below());
                    if (level.noCollision(entity, movedBox)
                            && (entity.isNoGravity()
                            || below.isFaceSturdy(level, candidatePos.below(), Direction.UP))) {
                        return candidate;
                    }
                }
            }
        }
        return Vec3.atBottomCenterOf(centerPos.above());
    }

    private static void cancel(MagicCenterBlockEntity center, ServerPlayer player,
                               String translationKey) {
        center.clearTeleportChannel();
        if (center.getLevel() instanceof ServerLevel level) {
            level.playSound(null, center.getBlockPos(), SoundEvents.BEACON_DEACTIVATE,
                    SoundSource.BLOCKS, 0.8F, 0.8F);
            level.sendParticles(ParticleTypes.SMOKE,
                    player.getX(), player.getY() + 1.0D, player.getZ(),
                    12, 0.35D, 0.45D, 0.35D, 0.015D);
        }
        player.displayClientMessage(Component.translatable(translationKey), true);
    }

    private static void reject(MagicCenterBlockEntity center, ServerPlayer player,
                               String translationKey, boolean automatic) {
        if (automatic) {
            player.displayClientMessage(Component.translatable(translationKey), true);
        } else {
            cancel(center, player, translationKey);
        }
    }

    private static AutomaticTeleportState automaticState(ServerLevel level, UUID centerId) {
        return AUTOMATIC_TELEPORT_STATES
                .computeIfAbsent(level, ignored -> new HashMap<>())
                .computeIfAbsent(centerId, ignored -> new AutomaticTeleportState());
    }

    private static void clearAutomaticState(ServerLevel level, UUID centerId) {
        Map<UUID, AutomaticTeleportState> states = AUTOMATIC_TELEPORT_STATES.get(level);
        if (states == null) {
            return;
        }
        states.remove(centerId);
        if (states.isEmpty()) {
            AUTOMATIC_TELEPORT_STATES.remove(level);
        }
    }

    public static void forgetAutomaticCenter(MagicCenterBlockEntity center) {
        if (center.getLevel() instanceof ServerLevel level) {
            clearAutomaticState(level, center.getCenterId());
        }
    }

    private static final class AutomaticTeleportState {
        private final Set<UUID> insideRoots = new HashSet<>();
        private long nextAttemptTime;
    }

    private static long saturatingMultiply(long left, long right) {
        if (left <= 0L || right <= 0L) {
            return 0L;
        }
        return left > Long.MAX_VALUE / right ? Long.MAX_VALUE : left * right;
    }

    private static long saturatingAdd(long left, long right) {
        long safeLeft = Math.max(0L, left);
        long safeRight = Math.max(0L, right);
        return safeLeft > Long.MAX_VALUE - safeRight
                ? Long.MAX_VALUE : safeLeft + safeRight;
    }
}
