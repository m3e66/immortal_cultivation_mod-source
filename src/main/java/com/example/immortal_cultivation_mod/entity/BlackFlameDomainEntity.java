package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.network.BlackFlamePayloads;
import com.example.immortal_cultivation_mod.particle.ModParticles;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Persistent, fuel-fed 玄焰 domain. Terrain work grows with the square of the
 * flame radius, but remains capped, so a larger domain consumes more blocks
 * without ever iterating its whole volume in one tick.
 */
public final class BlackFlameDomainEntity extends Entity {
    private static final EntityDataAccessor<Float> DATA_RADIUS =
            SynchedEntityData.defineId(BlackFlameDomainEntity.class,
                    EntityDataSerializers.FLOAT);
    public static final float DAMAGE_PER_SECOND = 100.0F;
    private static final double START_RADIUS = 0.35D;
    private static final double BASE_RADIUS = 5.0D;
    private static final double MAX_RADIUS = 48.0D;
    private static final int SEAL_IGNITION_TICKS = 12;
    private static final int EXPANSION_TICKS = 50;
    private static final int ACTIVE_TICK = SEAL_IGNITION_TICKS + EXPANSION_TICKS;
    private static final double FUEL_EXPANSION_PER_TICK = 0.08D;
    private static final int BASE_SCAN_PROBES_PER_TICK = 384;
    private static final int MAX_SCAN_PROBES_PER_TICK = 2_048;
    private static final int BASE_CORROSION_WORK_PER_TICK = 48;
    private static final int MAX_CORROSION_WORK_PER_TICK = 256;
    private static final int BASE_BLOCK_DESTRUCTIONS_PER_TICK = 18;
    private static final int MAX_BLOCK_DESTRUCTIONS_PER_TICK = 72;
    private static final int CORROSION_QUEUE_LIMIT = 8_192;
    private static final int ITEM_CONSUMPTION_TICKS = 8;
    private static final int STARVATION_TICKS = 30 * 20;
    private static final int MIN_STARVATION_TICKS = 10 * 20;
    private static final double DURATION_LOSS_PER_GROWTH_BLOCK = 6.0D;
    private static final int DEATH_SEQUENCE_TICKS = 13;
    private static final int MAX_BLOCK_VISUALS_PER_TICK = 12;
    private static final int MAX_CORROSION_PARTICLE_EVENTS_PER_TICK = 12;
    private static final int MAX_COLLAPSE_PARTICLE_EVENTS_PER_TICK = 12;

    @Nullable
    private UUID ownerUuid;
    private double radius = START_RADIUS;
    private double fuelRadius;
    private int destroyedBlocks;
    private long scanCursor;
    private int scanDiameter;
    private int lastFuelTick;
    private final Map<Long, Float> corrosion = new HashMap<>();
    private final ArrayDeque<Long> corrosionQueue = new ArrayDeque<>();
    private final Set<Long> queuedCorrosion = new HashSet<>();
    private final Set<UUID> creditedDeaths = new HashSet<>();
    private final Map<UUID, Integer> itemConsumption = new HashMap<>();
    private final Map<UUID, Integer> lastObservedQi = new HashMap<>();
    private final Map<UUID, PendingDeath> pendingDeaths = new HashMap<>();
    private int growthPulseTicks;
    private int growthPulseDuration;
    private double growthPulseStrength;
    private Vec3 growthSource = Vec3.ZERO;
    private boolean growthSourceValid;

    public BlackFlameDomainEntity(
            EntityType<? extends BlackFlameDomainEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
        noCulling = true;
    }

    public static void spawn(
            ServerLevel level, @Nullable LivingEntity owner, Vec3 impact) {
        BlackFlameDomainEntity domain = ModEntities.BLACK_FLAME_DOMAIN.get().create(level);
        if (domain == null) return;
        domain.ownerUuid = owner == null ? null : owner.getUUID();
        domain.setPos(impact.x, impact.y, impact.z);
        domain.lastFuelTick = 0;
        level.addFreshEntity(domain);
        level.playSound(null, BlockPos.containing(impact),
                SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.PLAYERS,
                0.75F, 0.34F);
    }

    /**
     * The entity itself is intentionally only 0.1 blocks wide, but its visual
     * domain can grow to a 48-block radius. Vanilla derives render distance
     * from the tiny entity hitbox, which otherwise removes the custom flame
     * mesh after the camera moves only a few blocks away and leaves only the
     * much sparser server particles visible.
     */
    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double visibleRadius = Math.max(radius, entityData.get(DATA_RADIUS));
        double renderDistance = Math.max(160.0D,
                Math.min(384.0D, visibleRadius * 5.0D + 128.0D));
        return distance < renderDistance * renderDistance;
    }

    @Override
    protected void defineSynchedData(
            net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        builder.define(DATA_RADIUS, (float) START_RADIUS);
    }

    @Override
    public void tick() {
        super.tick();
        setNoGravity(true);
        noPhysics = true;
        if (level().isClientSide) {
            radius = Mth.lerp(0.28D, radius, entityData.get(DATA_RADIUS));
            return;
        }
        ServerLevel level = (ServerLevel) level();

        double activationRadius = activationRadius();
        double targetRadius = Math.min(MAX_RADIUS, activationRadius + fuelRadius);
        radius = Math.max(radius, activationRadius);
        if (tickCount > ACTIVE_TICK && radius < targetRadius) {
            radius = Math.min(targetRadius, radius + FUEL_EXPANSION_PER_TICK);
        }
        float syncedRadius = entityData.get(DATA_RADIUS);
        if (Math.abs(syncedRadius - radius) >= 0.025D) {
            entityData.set(DATA_RADIUS, (float) radius);
        }

        if (tickCount < SEAL_IGNITION_TICKS) {
            emitSealIgnitionVfx(level);
        } else {
            if (tickCount == SEAL_IGNITION_TICKS) emitRuptureVfx(level);
            emitDomainVfx(level);
            if (tickCount <= ACTIVE_TICK) emitExpansionFrontVfx(level);
        }
        if (growthPulseTicks > 0) {
            emitGrowthVfx(level);
            growthPulseTicks--;
            if (growthPulseTicks == 0) {
                growthPulseStrength = 0.0D;
                growthSourceValid = false;
            }
        }

        // Consumption begins only after the staged inward ignition and the
        // five-block outward bloom have both finished.
        if (tickCount >= ACTIVE_TICK) {
            processPendingDeaths(level);
            corrodeTerrain(level);
            if ((tickCount & 1) == 0) consumeLooseMatter(level);
            if (tickCount % 6 == 0) emitAffectedEntityVfx(level);
            if (tickCount % 20 == 0) damageLiving(level);
        }

        int remainingDuration = starvationDurationTicks();
        if (tickCount > ACTIVE_TICK + remainingDuration
                && tickCount - lastFuelTick > remainingDuration) {
            level.sendParticles(blackAsh(), getX(), getY(), getZ(),
                    45, radius * 0.35D, 0.45D, radius * 0.35D, 0.015D);
            discard();
        }
    }

    private double activationRadius() {
        if (tickCount <= SEAL_IGNITION_TICKS) return START_RADIUS;
        double progress = Mth.clamp((tickCount - SEAL_IGNITION_TICKS)
                / (double) EXPANSION_TICKS, 0.0D, 1.0D);
        double eased = progress * progress * (3.0D - 2.0D * progress);
        return Mth.lerp(eased, START_RADIUS, BASE_RADIUS);
    }

    /**
     * Growing beyond the original five-block domain burns through its stable
     * lifetime. Fuel and freshly consumed terrain can still sustain it, but a
     * very large field loses that support sooner than a compact one.
     */
    private int starvationDurationTicks() {
        double grownRadius = Math.max(0.0D, radius - BASE_RADIUS);
        int expansionCost = Mth.ceil(grownRadius
                * DURATION_LOSS_PER_GROWTH_BLOCK);
        return Math.max(MIN_STARVATION_TICKS,
                STARVATION_TICKS - expansionCost);
    }

    private void consumeLooseMatter(ServerLevel level) {
        AABB bounds = getBoundingBox().inflate(radius);
        double radiusSquared = radius * radius;
        Set<UUID> touchedItems = new HashSet<>();
        int processedItems = 0;
        for (ItemEntity item : level.getEntitiesOfClass(
                ItemEntity.class, bounds,
                candidate -> candidate.isAlive()
                        && candidate.position().distanceToSqr(position()) <= radiusSquared)) {
            if (processedItems++ >= 24) break;
            UUID itemId = item.getUUID();
            touchedItems.add(itemId);
            int progress = itemConsumption.merge(itemId, 1, Integer::sum);
            emitItemConsumptionVfx(level, item, progress);
            if (progress < ITEM_CONSUMPTION_TICKS) continue;

            int count = item.getItem().getCount();
            String namespace = BuiltInRegistries.ITEM.getKey(
                    item.getItem().getItem()).getNamespace();
            double spiritualMultiplier = ImmortalCultivationMod.MODID.equals(namespace)
                    ? 1.75D : 1.0D;
            emitConsumedItemVfx(level, item.position());
            addFuelRadius(0.15D * Math.sqrt(Math.max(1, count))
                    * spiritualMultiplier, item.position());
            itemConsumption.remove(itemId);
            item.discard();
        }
        if (tickCount % 20 == 0) {
            itemConsumption.keySet().retainAll(touchedItems);
        }
        int processedProjectiles = 0;
        for (Projectile projectile : level.getEntitiesOfClass(
                Projectile.class, bounds,
                candidate -> candidate.isAlive()
                        && candidate.position().distanceToSqr(position()) <= radiusSquared)) {
            if (processedProjectiles++ >= 32) break;
            Vec3 source = projectile.position();
            level.sendParticles(blackCore(), source.x, source.y, source.z,
                    5, 0.12D, 0.12D, 0.12D, 0.01D);
            projectile.discard();
            addFuelRadius(0.025D, source);
        }
    }

    private void damageLiving(ServerLevel level) {
        AABB bounds = getBoundingBox().inflate(radius);
        double radiusSquared = radius * radius;
        LivingEntity owner = owner();
        for (LivingEntity target : level.getEntitiesOfClass(
                LivingEntity.class, bounds,
                candidate -> candidate.isAlive()
                        && !pendingDeaths.containsKey(candidate.getUUID())
                        && candidate.position().add(0.0D, candidate.getBbHeight() * 0.5D, 0.0D)
                        .distanceToSqr(position()) <= radiusSquared)) {
            if (target.isInvulnerable()) continue;
            corrodeEquipment(target);
            target.setRemainingFireTicks(0);
            if (target.getHealth() <= DAMAGE_PER_SECOND) {
                beginPendingDeath(level, target);
                continue;
            }
            boolean aliveBefore = target.isAlive();
            target.hurt(damageSources().indirectMagic(this, owner), DAMAGE_PER_SECOND);
            if (aliveBefore && (!target.isAlive() || target.getHealth() <= 0.0F)
                    && creditedDeaths.add(target.getUUID())) {
                Vec3 source = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
                emitConsumedDeathVfx(level, target, source);
                addFuelRadius(killFuel(target), source);
            }
        }
    }

    private void beginPendingDeath(ServerLevel level, LivingEntity target) {
        UUID targetId = target.getUUID();
        if (pendingDeaths.containsKey(targetId)) return;
        Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        PendingDeath death = new PendingDeath(
                target.isInvulnerable(), target.isNoGravity(),
                target instanceof Mob mob && mob.isNoAi(), killFuel(target), center);
        pendingDeaths.put(targetId, death);
        target.setHealth(Math.min(target.getHealth(), 1.0F));
        target.setInvulnerable(true);
        target.setNoGravity(true);
        target.setDeltaMovement(Vec3.ZERO);
        target.setRemainingFireTicks(0);
        if (target instanceof Mob mob) mob.setNoAi(true);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(target,
                new BlackFlamePayloads.ClientboundDeathStart(
                        target.getId(), DEATH_SEQUENCE_TICKS));
    }

    private void processPendingDeaths(ServerLevel level) {
        Iterator<Map.Entry<UUID, PendingDeath>> iterator =
                pendingDeaths.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PendingDeath> entry = iterator.next();
            Entity found = level.getEntity(entry.getKey());
            if (!(found instanceof LivingEntity target) || target.isRemoved()) {
                iterator.remove();
                continue;
            }
            PendingDeath death = entry.getValue();
            death.ticks++;
            target.setDeltaMovement(Vec3.ZERO);
            target.setNoGravity(true);
            target.setRemainingFireTicks(0);
            if (target instanceof Mob mob) mob.setNoAi(true);
            emitPendingDeathVfx(level, target, death);
            if (death.ticks < DEATH_SEQUENCE_TICKS) continue;

            target.setInvulnerable(death.wasInvulnerable);
            target.setNoGravity(death.wasNoGravity);
            if (target instanceof Mob mob) mob.setNoAi(death.wasNoAi);
            iterator.remove();
            if (death.wasInvulnerable) continue;
            target.hurt(damageSources().indirectMagic(this, owner()), Float.MAX_VALUE);
            if (target.isAlive()) target.setHealth(0.0F);
            if (creditedDeaths.add(target.getUUID())) {
                emitConsumedDeathVfx(level, target, death.center);
                addFuelRadius(death.fuel, death.center);
            }
        }
    }

    private void emitPendingDeathVfx(
            ServerLevel level, LivingEntity target, PendingDeath death) {
        Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        if (death.ticks <= 4) {
            level.sendParticles(blackAsh(), center.x, center.y, center.z,
                    3, target.getBbWidth() * 0.45D,
                    target.getBbHeight() * 0.42D,
                    target.getBbWidth() * 0.45D, 0.003D);
        } else if (death.ticks <= 6) {
            level.sendParticles(blackCore(), center.x, target.getY() + 0.04D, center.z,
                    5, target.getBbWidth() * 0.38D, 0.025D,
                    target.getBbWidth() * 0.38D, 0.004D);
        } else if (death.ticks <= 9) {
            // Cracks appear inside the body only after the outer fire goes dark.
            level.sendParticles(blackSpark(), center.x, center.y, center.z,
                    5, target.getBbWidth() * 0.28D,
                    target.getBbHeight() * 0.30D,
                    target.getBbWidth() * 0.28D, 0.012D);
        } else {
            double shrink = 1.0D - (death.ticks - 9) / 5.0D;
            for (int fragment = 0; fragment < 8; fragment++) {
                Vec3 start = center.add(
                        (random.nextDouble() - 0.5D) * target.getBbWidth() * shrink,
                        (random.nextDouble() - 0.5D) * target.getBbHeight() * shrink,
                        (random.nextDouble() - 0.5D) * target.getBbWidth() * shrink);
                Vec3 inward = center.subtract(start);
                movingParticle(level, fragment % 3 == 0 ? blackSpark() : blackCore(),
                        start, inward.lengthSqr() < 1.0E-5D
                                ? Vec3.ZERO : inward.normalize().scale(0.08D));
            }
        }
    }

    private static void corrodeEquipment(LivingEntity target) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = target.getItemBySlot(slot);
            if (!stack.isDamageableItem()) continue;
            int durabilityBurn = Math.max(2, Mth.ceil(stack.getMaxDamage() * 0.02D));
            stack.hurtAndBreak(durabilityBurn, target, slot);
        }
    }

    private double killFuel(LivingEntity target) {
        int realm = 0;
        int qi = 0;
        if (target instanceof Player player) {
            var data = ModAttachments.getData(player);
            realm = CultivationLevels.getRealmIndex(data.cultivationLevel());
            qi = data.qi();
        } else if (target instanceof CultivationNpc npc) {
            realm = CultivationLevels.getRealmIndex(npc.npcRealm());
            qi = npc.npcQi();
        } else if (target instanceof CrimsonSkyFlameFloodDragonEntity
                || target instanceof BloodApeDemonGeneralEntity
                || target instanceof EscapedYuanyingCoreEntity) {
            realm = 4;
        } else if (target instanceof CrimsonScaleFlameLizardEntity) {
            realm = 3;
        } else if (target instanceof KongmingBeetleEntity) {
            realm = 2;
        }
        double realmFuel = switch (realm) {
            case 1 -> 1.0D;
            case 2 -> 2.0D;
            case 3 -> 4.0D;
            default -> realm >= 4 ? 7.0D + (realm - 4) * 2.0D : 0.0D;
        };
        if (realmFuel > 0.0D) {
            return realmFuel
                    + Math.min(2.0D, target.getMaxHealth() / 10_000.0D)
                    + Math.min(1.0D, qi / 100_000.0D);
        }
        if (target instanceof Animal) return 0.5D;
        if (target instanceof Enemy) return 0.7D;
        return Mth.clamp(target.getMaxHealth() / 80.0D, 0.25D, 0.7D);
    }

    private void corrodeTerrain(ServerLevel level) {
        int wholeRadius = Math.max(1, Mth.ceil(radius));
        int diameter = wholeRadius * 2 + 1;
        int volume = diameter * diameter * diameter;
        if (scanDiameter != diameter) {
            scanDiameter = diameter;
            scanCursor = Math.floorMod(scanCursor, (long) volume);
        }

        int scanBudget = terrainScanBudget();
        int frontierAddLimit = Mth.clamp(12 + Mth.ceil(radius * 0.92D),
                16, 56);
        enqueueFrontierBlocks(level, Math.max(12, scanBudget / 10),
                frontierAddLimit);
        enqueueInteriorBlocks(level, wholeRadius, diameter, volume, scanBudget);

        List<BlackFlamePayloads.BlockVisual> visuals =
                new ArrayList<>(MAX_BLOCK_VISUALS_PER_TICK);
        processCorrosionQueue(level, visuals);

        if (!visuals.isEmpty()) {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(this,
                    new BlackFlamePayloads.ClientboundBlockCorrosion(
                            List.copyOf(visuals)));
        }
    }

    /**
     * Keeps a small amount of immediate pressure on the current edge while the
     * deterministic scanner below continues walking every position inside the
     * sphere. The edge can never consume the interior work budget.
     */
    private void enqueueFrontierBlocks(
            ServerLevel level, int probes, int addLimit) {
        int added = 0;

        // Keep the visible terrain front close to the expanding flame. A
        // heightmap-guided annulus finds actual surface blocks instead of
        // spending nearly every probe in empty sky at large radii.
        int columns = Mth.clamp(addLimit * 2, 24, 96);
        double horizontalRadius = Math.max(0.5D, radius * 0.965D);
        double phase = tickCount * 0.431D;
        double radiusSquared = radius * radius;
        for (int column = 0; column < columns && added < addLimit; column++) {
            double angle = phase + column * Mth.TWO_PI / columns;
            int x = Mth.floor(getX() + Math.cos(angle) * horizontalRadius);
            int z = Mth.floor(getZ() + Math.sin(angle) * horizontalRadius);
            BlockPos columnProbe = new BlockPos(x, blockPosition().getY(), z);
            if (!level.hasChunkAt(columnProbe)) continue;
            int surfaceY = level.getHeight(
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
            for (int depth = 0; depth < 3 && added < addLimit; depth++) {
                BlockPos block = new BlockPos(x, surfaceY - depth, z);
                double dx = block.getX() + 0.5D - getX();
                double dy = block.getY() + 0.5D - getY();
                double dz = block.getZ() + 0.5D - getZ();
                if (dx * dx + dy * dy + dz * dz > radiusSquared) continue;
                if (enqueueCorrosion(level, block, true)) added++;
            }
        }

        // Caves, overhangs, and the lower half of the sphere still use a
        // bounded spherical fallback. These entries receive the same priority
        // but cannot exceed the per-tick frontier allowance above.
        for (int probe = 0;
                probe < probes && added < addLimit
                        && corrosionQueue.size() < CORROSION_QUEUE_LIMIT;
                probe++) {
            Vec3 direction = randomUnitVector();
            double depth = random.nextDouble() * Math.min(1.1D, radius * 0.20D);
            double distance = Math.max(0.0D, radius - depth);
            BlockPos block = blockPosition().offset(
                    (int) Math.round(direction.x * distance),
                    (int) Math.round(direction.y * distance),
                    (int) Math.round(direction.z * distance));
            if (enqueueCorrosion(level, block, true)) added++;
        }
    }

    /**
     * Advances through the complete cubic lattice with a strict probe cap.
     * Air, unloaded chunks, and out-of-sphere positions all consume a probe,
     * preventing the unbounded empty-space rescans that previously hurt TPS.
     */
    private void enqueueInteriorBlocks(
            ServerLevel level, int wholeRadius, int diameter, int volume,
            int probes) {
        double radiusSquared = radius * radius;
        for (int probe = 0;
                probe < probes && corrosionQueue.size() < CORROSION_QUEUE_LIMIT;
                probe++) {
            int index = (int) Math.floorMod(scanCursor++, (long) volume);
            int x = index % diameter - wholeRadius;
            int z = index / diameter % diameter - wholeRadius;
            int y = index / (diameter * diameter) - wholeRadius;
            if (x * x + y * y + z * z > radiusSquared) continue;
            enqueueCorrosion(level, blockPosition().offset(x, y, z), false);
        }
    }

    private boolean enqueueCorrosion(
            ServerLevel level, BlockPos block, boolean priority) {
        long positionKey = block.asLong();
        if (queuedCorrosion.contains(positionKey) || !level.hasChunkAt(block)
                || !level.getWorldBorder().isWithinBounds(block)) {
            return false;
        }
        BlockState state = level.getBlockState(block);
        if (state.isAir()) {
            corrosion.remove(positionKey);
            return false;
        }
        queuedCorrosion.add(positionKey);
        // Prime ordinary surface material so the visual destruction front can
        // keep pace with expansion. Very durable blocks still require many
        // passes because the initial boost is capped at four corrosion units.
        if (priority) {
            float resistance = corrosionResistance(level, block, state);
            float primed = Math.min(4.0F, Math.max(0.0F, resistance - 1.0F));
            if (primed > corrosion.getOrDefault(positionKey, 0.0F)) {
                corrosion.put(positionKey, primed);
            }
        }
        // Fluids and the active surface front are processed first so the
        // growing boundary is never hidden behind the interior backlog.
        if (priority || !state.getFluidState().isEmpty()) {
            corrosionQueue.addFirst(positionKey);
        } else {
            corrosionQueue.addLast(positionKey);
        }
        return true;
    }

    private void processCorrosionQueue(
            ServerLevel level, List<BlackFlamePayloads.BlockVisual> visuals) {
        int availableAtStart = corrosionQueue.size();
        int work = Math.min(availableAtStart, corrosionWorkBudget());
        int destructionBudget = blockDestructionBudget();
        int destroyedThisTick = 0;
        int corrosionParticleEvents = 0;
        int collapseParticleEvents = 0;

        for (int index = 0; index < work; index++) {
            Long boxedKey = corrosionQueue.pollFirst();
            if (boxedKey == null) break;
            long positionKey = boxedKey;
            queuedCorrosion.remove(positionKey);
            BlockPos block = BlockPos.of(positionKey);
            if (!level.hasChunkAt(block)
                    || !level.getWorldBorder().isWithinBounds(block)) {
                corrosion.remove(positionKey);
                continue;
            }
            BlockState state = level.getBlockState(block);
            if (state.isAir()) {
                corrosion.remove(positionKey);
                continue;
            }

            float next = corrosion.getOrDefault(positionKey, 0.0F) + 1.0F;
            float resistance = corrosionResistance(level, block, state);
            float progress = Mth.clamp(next / resistance, 0.0F, 1.0F);
            if (visuals.size() < MAX_BLOCK_VISUALS_PER_TICK) {
                visuals.add(new BlackFlamePayloads.BlockVisual(positionKey, progress));
            }
            if (next < resistance || destroyedThisTick >= destructionBudget) {
                corrosion.put(positionKey, Math.min(next, resistance));
                enqueueKnownCorrosion(positionKey);
                if (corrosionParticleEvents
                        < MAX_CORROSION_PARTICLE_EVENTS_PER_TICK) {
                    emitBlockCorrosionVfx(level, block, state, progress);
                    corrosionParticleEvents++;
                }
                continue;
            }

            corrosion.remove(positionKey);
            Vec3 consumedAt = Vec3.atCenterOf(block);
            boolean containsFluid = !level.getFluidState(block).isEmpty();
            // Black flame annihilates matter without drops. Avoiding normal
            // neighbor updates makes large removals batch into section update
            // packets instead of triggering thousands of redstone, fluid,
            // leaf, and gravity cascades during the same tick.
            boolean consumed = com.example.immortal_cultivation_mod.spell.SpellDestruction.eraseNow(level, block, owner());
            if (consumed) {
                if (collapseParticleEvents
                        < MAX_COLLAPSE_PARTICLE_EVENTS_PER_TICK) {
                    emitBlockCollapseVfx(level, block, state, containsFluid);
                    collapseParticleEvents++;
                }
                destroyedThisTick++;
                destroyedBlocks++;
                lastFuelTick = tickCount;
                if (destroyedBlocks % 50 == 0) addFuelRadius(0.25D, consumedAt);
                if (ImmortalCultivationMod.MODID.equals(
                        BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace())) {
                    addFuelRadius(0.02D, consumedAt);
                }
            } else {
                corrosion.put(positionKey, resistance);
                enqueueKnownCorrosion(positionKey);
            }
        }
    }

    private void enqueueKnownCorrosion(long positionKey) {
        if (queuedCorrosion.add(positionKey)) {
            corrosionQueue.addLast(positionKey);
        }
    }

    private int terrainScanBudget() {
        double scale = Math.max(1.0D, radius / BASE_RADIUS);
        return Mth.clamp(Mth.ceil(BASE_SCAN_PROBES_PER_TICK * scale),
                BASE_SCAN_PROBES_PER_TICK, MAX_SCAN_PROBES_PER_TICK);
    }

    private int corrosionWorkBudget() {
        double scale = Math.max(1.0D, radius / BASE_RADIUS);
        return Mth.clamp(Mth.ceil(BASE_CORROSION_WORK_PER_TICK
                        * scale * Math.sqrt(scale)),
                BASE_CORROSION_WORK_PER_TICK,
                MAX_CORROSION_WORK_PER_TICK);
    }

    private int blockDestructionBudget() {
        double scale = Math.max(1.0D, radius / BASE_RADIUS);
        return Mth.clamp(Mth.ceil(BASE_BLOCK_DESTRUCTIONS_PER_TICK * scale),
                BASE_BLOCK_DESTRUCTIONS_PER_TICK,
                MAX_BLOCK_DESTRUCTIONS_PER_TICK);
    }

    private static float corrosionResistance(
            ServerLevel level, BlockPos pos, BlockState state) {
        if (!level.getFluidState(pos).isEmpty()
                || state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)
                || state.is(BlockTags.LEAVES) || state.is(BlockTags.FLOWERS)) {
            return 1.0F;
        }
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0.0F) return 24.0F;
        if (state.is(BlockTags.LOGS)) return 3.0F;
        return Mth.clamp(1.0F + hardness * 2.5F, 1.0F, 18.0F);
    }

    private void addFuelRadius(double amount, Vec3 source) {
        if (amount <= 0.0D) return;
        double previous = fuelRadius;
        fuelRadius = Math.min(MAX_RADIUS - BASE_RADIUS, fuelRadius + amount);
        double gained = fuelRadius - previous;
        lastFuelTick = tickCount;
        if (gained < 0.05D) return;

        int duration = gained >= 0.5D ? 12 : 6;
        boolean startingPulse = growthPulseTicks <= 0;
        if (duration >= growthPulseTicks) {
            growthSource = source;
            growthSourceValid = true;
        }
        growthPulseTicks = Math.max(growthPulseTicks, duration);
        growthPulseDuration = startingPulse
                ? growthPulseTicks
                : Math.max(growthPulseDuration, growthPulseTicks);
        growthPulseStrength = Math.max(growthPulseStrength,
                Mth.clamp(gained / 4.0D, 0.18D, 1.0D));
    }

    private void emitSealIgnitionVfx(ServerLevel level) {
        Vec3 center = position();
        level.sendParticles(blackCore(), center.x, center.y, center.z,
                2, 0.025D, 0.025D, 0.025D, 0.002D);
        int points = 8 + tickCount / 2;
        double shell = 2.2D + tickCount * 0.09D;
        for (int point = 0; point < points; point++) {
            Vec3 direction = sphereDirection(point, points, tickCount * 0.11D);
            Vec3 start = center.add(direction.scale(shell))
                    .add(0.0D, Math.sin(point * 1.7D + tickCount * 0.2D) * 0.18D, 0.0D);
            Vec3 inward = center.subtract(start).normalize()
                    .scale(0.075D + tickCount * 0.0025D);
            movingParticle(level, point % 3 == 0 ? violetEdge() : blackCore(),
                    start, inward);
        }
        if (tickCount % 3 == 0) {
            level.sendParticles(blackAsh(), center.x, center.y, center.z,
                    3, 0.12D, 0.12D, 0.12D, 0.005D);
        }
    }

    private void emitRuptureVfx(ServerLevel level) {
        Vec3 center = position();
        for (int shell = 0; shell < 3; shell++) {
            int points = 16 + shell * 4;
            double shellRadius = 0.65D + shell * 0.62D;
            for (int point = 0; point < points; point++) {
                Vec3 direction = sphereDirection(point, points, shell * 0.73D);
                Vec3 start = center.add(direction.scale(shellRadius));
                ParticleOptions particle = paletteParticle(point + shell * 17);
                movingParticle(level, particle, start,
                        direction.scale(0.12D + shell * 0.025D));
            }
        }
        level.sendParticles(blackAsh(), center.x, center.y, center.z,
                18, 0.28D, 0.28D, 0.28D, 0.018D);
        level.playSound(null, blockPosition(),
                SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.PLAYERS,
                1.65F, 0.27F);
    }

    private void emitExpansionFrontVfx(ServerLevel level) {
        if (tickCount % 4 != 0) return;
        int points = Mth.clamp(12 + Mth.ceil(radius * 0.8D), 12, 28);
        double phase = tickCount * 0.19D;
        for (int point = 0; point < points; point++) {
            // Gaps keep the growing front jagged instead of a solid bubble.
            int fragment = Math.floorMod(point + tickCount / 2, 9);
            if (fragment == 0 || fragment == 4 || fragment == 5) continue;
            Vec3 direction = sphereDirection(point, points, phase);
            Vec3 front = position().add(direction.scale(radius));
            movingParticle(level, paletteParticle(point + tickCount), front,
                    direction.scale(0.08D));
        }
    }

    private void emitDomainVfx(ServerLevel level) {
        double phase = tickCount * 0.11D;
        // The custom client renderer supplies the continuous black-purple
        // shell. Server particles are sparse accents only, keeping packet and
        // particle load bounded even when the domain grows very large.
        if (tickCount % 4 == 0) {
            int boundaryPoints = Mth.clamp(18 + Mth.ceil(radius * 2.2D),
                    24, 128);
            for (int point = 0; point < boundaryPoints; point++) {
                int fragment = Math.floorMod(point + tickCount / 3, 13);
                if (fragment == 0 || fragment == 7) {
                    continue;
                }
                Vec3 direction = sphereDirection(point, boundaryPoints, phase);
                double brokenEdge = radius + Math.sin(point * 1.91D + phase) * 0.17D;
                Vec3 edge = position().add(direction.scale(brokenEdge));
                Vec3 curl = new Vec3(-direction.z, direction.y * 0.15D, direction.x)
                        .scale(0.018D);
                movingParticle(level, paletteParticle(point + tickCount / 2),
                        edge, curl.add(direction.scale(random.nextBoolean() ? -0.018D : 0.026D)));
            }
        }

        if (tickCount % 6 == 0) {
            int inner = Mth.clamp(4 + Mth.ceil(radius * 0.42D), 6, 26);
            for (int point = 0; point < inner; point++) {
                Vec3 direction = randomUnitVector();
                double distance = radius * (0.18D + random.nextDouble() * 0.66D);
                Vec3 start = position().add(direction.scale(distance));
                Vec3 tangent = new Vec3(-direction.z, direction.y * 0.2D, direction.x)
                        .scale(0.025D);
                Vec3 inward = position().subtract(start).normalize().scale(0.018D);
                movingParticle(level, point % 3 == 0 ? violetEdge() : blackCore(),
                        start, inward.add(tangent));
            }
            Vec3 distortion = position().add(randomUnitVector()
                    .scale(radius * (0.35D + random.nextDouble() * 0.45D)));
            level.sendParticles(blackSpark(),
                    distortion.x, distortion.y, distortion.z,
                    1, 0.02D, 0.02D, 0.02D, 0.012D);
        }
        if (tickCount % 12 == 0) emitPredatoryTendrils(level);

        if (tickCount % 12 == 0) {
            level.sendParticles(blackAsh(), getX(), getY(), getZ(),
                    Mth.clamp(4 + Mth.ceil(radius * 0.42D), 5, 26),
                    radius * 0.58D, radius * 0.28D, radius * 0.58D, 0.003D);
        }
        if (tickCount % 18 == 0) {
            level.playSound(null, blockPosition(), SoundEvents.FIRE_AMBIENT,
                    SoundSource.PLAYERS, 0.44F,
                    0.31F + random.nextFloat() * 0.08F);
        }
    }

    private void emitPredatoryTendrils(ServerLevel level) {
        int tendrils = radius >= 20.0D ? 3 : 2;
        for (int tendril = 0; tendril < tendrils; tendril++) {
            Vec3 direction = randomUnitVector();
            Vec3 tangent = new Vec3(-direction.z, direction.y * 0.3D, direction.x);
            for (int segment = 0; segment < 4; segment++) {
                double progress = segment / 3.0D;
                double distance = radius * (0.25D + progress * 0.62D);
                double curl = Math.sin(progress * Math.PI) * radius * 0.075D;
                Vec3 point = position().add(direction.scale(distance))
                        .add(tangent.scale(curl));
                Vec3 lash = direction.scale(segment == 4 ? 0.065D : -0.012D)
                        .add(tangent.normalize().scale(0.018D));
                movingParticle(level,
                        segment >= 3 || tendril == 0 ? violetEdge() : blackCore(),
                        point, lash);
            }
        }
    }

    private void emitGrowthVfx(ServerLevel level) {
        int elapsed = Math.max(0, growthPulseDuration - growthPulseTicks);
        boolean contraction = elapsed < 2;
        int basePoints = growthPulseStrength >= 0.5D ? 28 : 18;
        int points = Mth.clamp(basePoints + Mth.ceil(radius * 0.95D),
                basePoints, 76);
        for (int point = 0; point < points; point++) {
            int fragment = Math.floorMod(point + tickCount, 8);
            if (fragment == 2 || fragment == 6) continue;
            Vec3 direction = sphereDirection(point, points, tickCount * 0.23D);
            double shell = Math.max(START_RADIUS,
                    radius - (contraction ? 0.3D + growthPulseStrength * 0.55D : 0.0D));
            Vec3 edge = position().add(direction.scale(shell));
            Vec3 velocity = direction.scale(contraction
                    ? -(0.07D + growthPulseStrength * 0.04D)
                    : 0.08D + growthPulseStrength * 0.08D);
            movingParticle(level, paletteParticle(point + elapsed * 7), edge, velocity);
        }

        if (growthSourceValid) {
            Vec3 towardCenter = position().subtract(growthSource);
            if (towardCenter.lengthSqr() > 0.01D) {
                Vec3 velocity = towardCenter.normalize()
                        .scale(0.055D + growthPulseStrength * 0.05D);
                for (int trail = 1; trail <= 4; trail++) {
                    Vec3 point = growthSource.lerp(position(), trail / 5.0D);
                    movingParticle(level, trail >= 3 ? violetEdge() : blackCore(),
                            point, velocity);
                }
            }
        }
        if (elapsed == 2) {
            level.playSound(null, blockPosition(),
                    SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.PLAYERS,
                    0.8F + (float) growthPulseStrength * 0.45F,
                    0.32F + (float) (1.0D - growthPulseStrength) * 0.12F);
        }
    }

    private void emitAffectedEntityVfx(ServerLevel level) {
        AABB bounds = getBoundingBox().inflate(radius);
        double radiusSquared = radius * radius;
        int processed = 0;
        for (LivingEntity target : level.getEntitiesOfClass(
                LivingEntity.class, bounds,
                candidate -> candidate.isAlive()
                        && !pendingDeaths.containsKey(candidate.getUUID())
                        && candidate.position().add(0.0D, candidate.getBbHeight() * 0.5D, 0.0D)
                        .distanceToSqr(position()) <= radiusSquared)) {
            if (processed++ >= 20) break;
            Vec3 bodyCenter = target.position()
                    .add(0.0D, target.getBbHeight() * 0.52D, 0.0D);
            for (int patch = 0; patch < 3; patch++) {
                Vec3 direction = randomUnitVector();
                Vec3 start = bodyCenter.add(
                        direction.x * Math.max(0.18D, target.getBbWidth() * 0.46D),
                        direction.y * Math.max(0.3D, target.getBbHeight() * 0.44D),
                        direction.z * Math.max(0.18D, target.getBbWidth() * 0.46D));
                Vec3 inward = bodyCenter.subtract(start).normalize().scale(0.038D);
                movingParticle(level, patch != 0 ? violetEdge() : blackCore(),
                        start, inward);
            }

            int qi = observedQi(target);
            if (qi >= 0) {
                Integer previous = lastObservedQi.put(target.getUUID(), qi);
                if (previous != null && qi < previous) {
                    level.sendParticles(blackSpark(),
                            bodyCenter.x, bodyCenter.y, bodyCenter.z,
                            9, target.getBbWidth() * 0.45D,
                            target.getBbHeight() * 0.35D,
                            target.getBbWidth() * 0.45D, 0.025D);
                }
            }
        }
        if (lastObservedQi.size() > 512) lastObservedQi.clear();
    }

    private static int observedQi(LivingEntity target) {
        if (target instanceof Player player) {
            return ModAttachments.getData(player).qi();
        }
        if (target instanceof CultivationNpc npc) return npc.npcQi();
        return -1;
    }

    private void emitItemConsumptionVfx(ServerLevel level, ItemEntity item, int progress) {
        Vec3 center = item.position().add(0.0D, 0.18D, 0.0D);
        if (progress % 2 == 0) {
            double ring = 0.38D * (1.0D - progress / (double) (ITEM_CONSUMPTION_TICKS + 3));
            for (int point = 0; point < 4; point++) {
                double angle = point * Math.PI * 0.5D + tickCount * 0.38D;
                Vec3 start = center.add(Math.cos(angle) * ring,
                        Math.sin(tickCount * 0.3D + point) * 0.05D,
                        Math.sin(angle) * ring);
                movingParticle(level, point % 2 == 0 ? violetEdge() : blackCore(),
                        start, center.subtract(start).normalize().scale(0.035D));
            }
        }
        level.sendParticles(violetEdge(), center.x, center.y, center.z,
                1, 0.035D, 0.035D, 0.035D, 0.002D);
    }

    private void emitConsumedItemVfx(ServerLevel level, Vec3 source) {
        level.sendParticles(blackCore(), source.x, source.y + 0.15D, source.z,
                7, 0.16D, 0.16D, 0.16D, 0.015D);
        level.sendParticles(blackAsh(), source.x, source.y + 0.15D, source.z,
                5, 0.1D, 0.1D, 0.1D, 0.01D);
        Vec3 inward = position().subtract(source);
        if (inward.lengthSqr() > 0.01D) {
            movingParticle(level, violetEdge(), source,
                    inward.normalize().scale(0.12D));
        }
    }

    private void emitConsumedDeathVfx(
            ServerLevel level, LivingEntity target, Vec3 source) {
        Vec3 flameCenter = position();
        for (int fragment = 0; fragment < 30; fragment++) {
            Vec3 start = source.add(
                    (random.nextDouble() - 0.5D) * target.getBbWidth(),
                    (random.nextDouble() - 0.5D) * target.getBbHeight() * 0.9D,
                    (random.nextDouble() - 0.5D) * target.getBbWidth());
            Vec3 pull = flameCenter.subtract(start);
            Vec3 velocity = pull.lengthSqr() < 1.0E-5D
                    ? Vec3.ZERO : pull.normalize().scale(0.09D + random.nextDouble() * 0.07D);
            ParticleOptions particle = fragment % 5 == 0
                    ? blackSpark() : fragment % 2 == 0 ? violetEdge() : blackAsh();
            movingParticle(level, particle, start, velocity);
        }
        level.playSound(null, BlockPos.containing(source),
                SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.PLAYERS,
                0.9F, 0.23F);
    }

    private void emitBlockCorrosionVfx(
            ServerLevel level, BlockPos block, BlockState state, float progress) {
        boolean fluid = !level.getFluidState(block).isEmpty();
        float chance = fluid ? 0.22F : progress >= 0.55F ? 0.16F : 0.055F;
        if (random.nextFloat() >= chance) return;
        Vec3 center = Vec3.atCenterOf(block);
        ParticleOptions stageParticle = progress < 0.30F
                ? blackAsh() : progress < 0.68F ? blackCore() : violetEdge();
        level.sendParticles(stageParticle, center.x, center.y, center.z,
                1, 0.31D, 0.31D, 0.31D, 0.004D);
        if (progress >= 0.72F || fluid) {
            Vec3 towardDomain = position().subtract(center);
            if (towardDomain.lengthSqr() > 0.01D) {
                movingParticle(level, fluid ? violetEdge() : blackCore(),
                        center.add(randomUnitVector().scale(0.38D)),
                        towardDomain.normalize().scale(0.035D));
            }
        }
    }

    private void emitBlockCollapseVfx(
            ServerLevel level, BlockPos block, BlockState state,
            boolean containedFluid) {
        Vec3 center = Vec3.atCenterOf(block);
        level.sendParticles(blackCore(), center.x, center.y, center.z,
                2, 0.34D, 0.34D, 0.34D, 0.012D);
        if (destroyedBlocks % 3 == 0) {
            level.sendParticles(blackAsh(), center.x, center.y, center.z,
                    3, 0.35D, 0.35D, 0.35D, 0.012D);
        }
        if (containedFluid) {
            ParticleOptions fluidEdge = state.is(Blocks.LAVA)
                    ? blackSpark() : violetEdge();
            level.sendParticles(fluidEdge, center.x, center.y, center.z,
                    5, 0.42D, 0.22D, 0.42D, 0.015D);
            level.sendParticles(blackAsh(), center.x, center.y, center.z,
                    3, 0.28D, 0.18D, 0.28D, 0.008D);
        }
    }

    private ParticleOptions paletteParticle(int seed) {
        int palette = Math.floorMod(seed, 50);
        if (palette < 14) return blackCore();
        if (palette < 43) return violetEdge();
        if (palette < 48) return blackSpark();
        return blackAsh();
    }

    private static SimpleParticleType blackCore() {
        return ModParticles.BLACK_FLAME_CORE.get();
    }

    private static SimpleParticleType violetEdge() {
        return ModParticles.BLACK_FLAME_EDGE.get();
    }

    private static SimpleParticleType blackAsh() {
        return ModParticles.BLACK_FLAME_ASH.get();
    }

    private static SimpleParticleType blackSpark() {
        return ModParticles.BLACK_FLAME_SPARK.get();
    }

    private Vec3 sphereDirection(int index, int count, double phase) {
        double y = 1.0D - 2.0D * (index + 0.5D) / Math.max(1, count);
        double horizontal = Math.sqrt(Math.max(0.0D, 1.0D - y * y));
        double angle = index * 2.399963229728653D + phase;
        return new Vec3(Math.cos(angle) * horizontal, y,
                Math.sin(angle) * horizontal);
    }

    private Vec3 randomUnitVector() {
        double y = random.nextDouble() * 2.0D - 1.0D;
        double horizontal = Math.sqrt(Math.max(0.0D, 1.0D - y * y));
        double angle = random.nextDouble() * Mth.TWO_PI;
        return new Vec3(Math.cos(angle) * horizontal, y,
                Math.sin(angle) * horizontal);
    }

    private static void movingParticle(
            ServerLevel level, ParticleOptions particle, Vec3 start, Vec3 velocity) {
        level.sendParticles(particle, start.x, start.y, start.z,
                0, velocity.x, velocity.y, velocity.z, 1.0D);
    }

    @Nullable
    private LivingEntity owner() {
        if (ownerUuid == null || !(level() instanceof ServerLevel level)) return null;
        Entity entity = level.getEntity(ownerUuid);
        return entity instanceof LivingEntity living ? living : null;
    }

    /** Smoothed, server-authoritative radius used by the custom client mesh. */
    public float visualRadius(float partialTick) {
        return (float) Math.max(START_RADIUS, radius);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) ownerUuid = tag.getUUID("Owner");
        radius = Mth.clamp(tag.getDouble("Radius"), START_RADIUS, MAX_RADIUS);
        fuelRadius = Mth.clamp(tag.getDouble("FuelRadius"),
                0.0D, MAX_RADIUS - BASE_RADIUS);
        destroyedBlocks = Math.max(0, tag.getInt("DestroyedBlocks"));
        scanCursor = Math.max(0L, tag.getLong("ScanCursor"));
        tickCount = Math.max(0, tag.getInt("Age"));
        lastFuelTick = Mth.clamp(tag.getInt("LastFuelTick"), 0, tickCount);
        entityData.set(DATA_RADIUS, (float) radius);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerUuid != null) tag.putUUID("Owner", ownerUuid);
        tag.putDouble("Radius", radius);
        tag.putDouble("FuelRadius", fuelRadius);
        tag.putInt("DestroyedBlocks", destroyedBlocks);
        tag.putLong("ScanCursor", scanCursor);
        tag.putInt("Age", tickCount);
        tag.putInt("LastFuelTick", lastFuelTick);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    private static final class PendingDeath {
        private final boolean wasInvulnerable;
        private final boolean wasNoGravity;
        private final boolean wasNoAi;
        private final double fuel;
        private final Vec3 center;
        private int ticks;

        private PendingDeath(
                boolean wasInvulnerable,
                boolean wasNoGravity,
                boolean wasNoAi,
                double fuel,
                Vec3 center) {
            this.wasInvulnerable = wasInvulnerable;
            this.wasNoGravity = wasNoGravity;
            this.wasNoAi = wasNoAi;
            this.fuel = fuel;
            this.center = center;
        }
    }
}
