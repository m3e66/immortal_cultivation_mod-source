package com.example.immortal_cultivation_mod.combat;

import com.example.immortal_cultivation_mod.config.BlockBreakingSettings;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.entity.CultivationNpc;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.entity.ElementalTechniqueAreaEntity;
import com.example.immortal_cultivation_mod.entity.ElementalTechniqueProjectileEntity;
import com.example.immortal_cultivation_mod.entity.SpellTriggerProjectileEntity;
import com.example.immortal_cultivation_mod.attachment.SpiritRoots;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import com.example.immortal_cultivation_mod.network.CombatFeedbackPayloads;
import com.example.immortal_cultivation_mod.network.CombatFeedbackPayloads.Effect;
import com.example.immortal_cultivation_mod.network.CombatFeedbackPayloads.Kind;
import com.example.immortal_cultivation_mod.spell.SpellDestruction;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Server-confirmed feedback only. Visual effects never determine damage or terrain permission. */
@EventBusSubscriber(modid = ImmortalCultivationMod.MODID)
public final class CombatFeedbackEvents {
    private static final Map<LivingEntity, State> STATES = new WeakHashMap<>();
    private static final Map<ServerLevel, Budget> BUDGETS = new WeakHashMap<>();
    private record TypedDamage(LivingEntity target, DamageSource source, int flags) {}
    private static final ThreadLocal<TypedDamage> TYPED_DAMAGE = new ThreadLocal<>();
    private static final int MAX_EVENTS_PER_WORLD_TICK = 128;
    private static final class Budget { long tick = Long.MIN_VALUE; int count; }
    private static final class State {
        long packetTick = Long.MIN_VALUE, movementTick = Long.MIN_VALUE, dashUntil;
        int kinds, packets, airTicks, emitted, lastFlags;
        Vec3 previous, previousStep = Vec3.ZERO, dashDirection = Vec3.ZERO;
        net.minecraft.resources.ResourceLocation dimension;
        boolean onGround, dash;
        double peakY, downwardSpeed;
        float dashIntensity;
    }
    private CombatFeedbackEvents() {}
    private static State state(LivingEntity entity) { return STATES.computeIfAbsent(entity, ignored -> new State()); }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void shieldBlock(LivingShieldBlockEvent event) {
        if (!event.isCanceled() && event.getBlocked() && event.getBlockedDamage() > 0)
            blocked(event.getEntity(), event.getDamageSource(), event.getBlockedDamage());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void hurt(LivingDamageEvent.Post event) {
        LivingEntity target = event.getEntity();
        if (!(target.level() instanceof ServerLevel) || !Float.isFinite(event.getNewDamage())
                || event.getNewDamage() <= 0) return;
        DamageSource source = event.getSource();
        // Hunger, drowning and ordinary falls are not weapon impacts. Landing has its own contact event.
        int flags = injuryFlags(target, source);
        if (flags == 0) return;
        Vec3 direction = incomingDirection(target, source);
        send(target, Kind.HIT, contactPoint(target, source, direction), direction,
                CombatFeedbackProfile.hitIntensity(event.getNewDamage(), target.getMaxHealth()), flags);
    }

    public static int injuryFlags(LivingEntity target, DamageSource source) {
        int flags = 0;
        if (source.is(DamageTypeTags.IS_FIRE)) flags |= CombatFeedbackPayloads.FIRE;
        if (source.is(DamageTypes.FREEZE)) flags |= CombatFeedbackPayloads.FROST;
        if (source.is(DamageTypes.LIGHTNING_BOLT)) flags |= CombatFeedbackPayloads.LIGHTNING;
        flags |= spellInjuryFlags(source);
        TypedDamage typed = TYPED_DAMAGE.get();
        if (typed != null && typed.target == target && typed.source == source) flags |= typed.flags;
        boolean physical = source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MOB_ATTACK)
                || source.is(DamageTypes.MOB_ATTACK_NO_AGGRO) || source.is(DamageTypeTags.IS_PROJECTILE);
        if (physical && flags == 0) flags |= CombatFeedbackPayloads.PHYSICAL;
        if (source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC)
                || source.is(DamageTypeTags.IS_EXPLOSION)) flags |= CombatFeedbackPayloads.AURA;
        if (flags != 0 && (target.hasEffect(ModEffects.FROZEN) || target.hasEffect(ModEffects.FROZEN_QI)))
            flags |= CombatFeedbackPayloads.FROST;
        if (flags != 0 && (target.hasEffect(ModEffects.QI_FIRE_BURN) || target.hasEffect(ModEffects.EMBER_BURN)))
            flags |= CombatFeedbackPayloads.FIRE;
        if (flags != 0 && lowQi(target)) flags |= CombatFeedbackPayloads.LOW_QI;
        return flags;
    }

    /** Some spell hitboxes intentionally use vanilla thrown/magic damage for combat rules. */
    private static int spellInjuryFlags(DamageSource source) {
        var direct = source.getDirectEntity();
        if (direct instanceof com.example.immortal_cultivation_mod.entity.LightningProjectileEntity
                || direct instanceof com.example.immortal_cultivation_mod.entity.ThunderRockProjectileEntity)
            return CombatFeedbackPayloads.LIGHTNING;
        if (direct instanceof com.example.immortal_cultivation_mod.entity.FireballProjectileEntity
                || direct instanceof com.example.immortal_cultivation_mod.entity.IgniteFlareProjectileEntity
                || direct instanceof com.example.immortal_cultivation_mod.entity.FireballTalismanProjectileEntity
                || direct instanceof com.example.immortal_cultivation_mod.entity.CrimsonFlameFireballEntity
                || direct instanceof com.example.immortal_cultivation_mod.entity.XinliaoFireballEntity)
            return CombatFeedbackPayloads.FIRE;
        if (direct instanceof com.example.immortal_cultivation_mod.entity.FrostCryProjectileEntity)
            return CombatFeedbackPayloads.FROST;
        if (direct instanceof com.example.immortal_cultivation_mod.entity.MethodTechniqueAreaEntity method
                && method.kind() == com.example.immortal_cultivation_mod.entity.MethodTechniqueAreaEntity.Kind.HANYUAN_DOMAIN)
            return CombatFeedbackPayloads.FROST;
        if (direct instanceof com.example.immortal_cultivation_mod.entity.CultivationBeastProjectileEntity beast
                && beast.projectileKind() == com.example.immortal_cultivation_mod.entity.CultivationBeastProjectileEntity.Kind.FIRE_FEATHER)
            return CombatFeedbackPayloads.FIRE;
        String spellId = direct instanceof SpellTriggerProjectileEntity trigger ? trigger.spellId()
                : direct instanceof ElementalTechniqueProjectileEntity projectile ? projectile.spellId()
                : direct instanceof ElementalTechniqueAreaEntity area ? area.kind().name().toLowerCase(java.util.Locale.ROOT) : null;
        var spell = spellId == null ? null : ModSpells.get(spellId);
        if (spell == null) return 0;
        int result = 0;
        for (String element : spell.element().split(",")) {
            result |= switch (element.trim()) {
                case SpiritRoots.FIRE -> CombatFeedbackPayloads.FIRE;
                case SpiritRoots.ICE -> CombatFeedbackPayloads.FROST;
                case SpiritRoots.THUNDER -> CombatFeedbackPayloads.LIGHTNING;
                default -> 0;
            };
        }
        return result;
    }

    /** Exact target/source scope prevents reflected or nested damage from inheriting the wrong element. */
    public static boolean hurtWithFlags(LivingEntity target, DamageSource source, float damage, int flags) {
        TypedDamage previous = TYPED_DAMAGE.get();
        TYPED_DAMAGE.set(new TypedDamage(target, source, flags & (CombatFeedbackPayloads.FIRE
                | CombatFeedbackPayloads.FROST | CombatFeedbackPayloads.LIGHTNING | CombatFeedbackPayloads.AURA)));
        try { return target.hurt(source, damage); }
        finally { if (previous == null) TYPED_DAMAGE.remove(); else TYPED_DAMAGE.set(previous); }
    }

    private static boolean lowQi(LivingEntity target) {
        if (target instanceof ServerPlayer player) {
            var data = ModAttachments.getData(player);
            if (CultivationLevels.isMortal(data.cultivationLevel())) return false;
            long maximum = Math.max(1L, (long) CultivationLevels.getLevelDef(data.cultivationLevel()).maxQi() + data.maxQiBonus());
            return maximum > 1 && data.qi() < maximum * .15;
        }
        if (target instanceof CultivationNpc npc && !CultivationLevels.isMortal(npc.npcRealm())) {
            int maximum = npc.npcMaxQi();
            return maximum > 1 && npc.npcQi() < maximum * .15;
        }
        return false;
    }

    /** Incoming trajectory continues through the target, so the energy tail appears behind it. */
    public static Vec3 incomingDirection(LivingEntity target, DamageSource source) {
        if (source != null && source.getDirectEntity() instanceof com.example.immortal_cultivation_mod.entity.WeaponQiSlashEntity slash
                && slash.getDeltaMovement().lengthSqr() > .0001) return slash.getDeltaMovement().normalize();
        if (source != null && source.getDirectEntity() instanceof Projectile projectile
                && projectile.getDeltaMovement().lengthSqr() > .0001) return projectile.getDeltaMovement().normalize();
        Vec3 sourcePosition = attackOrigin(source);
        if (sourcePosition != null) {
            Vec3 offset = target.getBoundingBox().getCenter().subtract(sourcePosition);
            if (offset.lengthSqr() > .0001) return offset.normalize();
        }
        return target.getLookAngle().reverse();
    }

    public static Vec3 contactPoint(LivingEntity target, DamageSource source, Vec3 direction) {
        Vec3 center = target.getBoundingBox().getCenter();
        Vec3 from = attackOrigin(source);
        if (from != null && !target.getBoundingBox().contains(from))
            return target.getBoundingBox().inflate(.035).clip(from, center).orElse(center);
        return center.subtract(direction.scale(target.getBbWidth() * .48));
    }

    private static Vec3 attackOrigin(DamageSource source) {
        if (source == null) return null;
        Vec3 position = source.getSourcePosition();
        // Melee origins are hand/shoulder height, not the attacker's feet.
        // Explicit spell contact origins (lightning strikes, remote beams) remain authoritative.
        if (source.getDirectEntity() instanceof LivingEntity attacker
                && (position == null || position.distanceToSqr(attacker.position()) < .0001))
            return attacker.getEyePosition().add(0, -.30, 0);
        return position;
    }

    public static void blocked(LivingEntity target, DamageSource source, float preventedDamage) {
        if (!Float.isFinite(preventedDamage) || preventedDamage <= 0) return;
        Vec3 direction = incomingDirection(target, source);
        send(target, Kind.BLOCK, contactPoint(target, source, direction), direction,
                CombatFeedbackProfile.hitIntensity(preventedDamage, target.getMaxHealth()), 0);
    }

    public static void shieldBreak(LivingEntity target, DamageSource source, float force) {
        if (force <= 0) return;
        Vec3 direction = incomingDirection(target, source);
        send(target, Kind.SHIELD_BREAK, contactPoint(target, source, direction), direction,
                Math.max(.45F, CombatFeedbackProfile.hitIntensity(force, target.getMaxHealth())), CombatFeedbackPayloads.AURA);
    }

    /** Call only after a dash ability succeeds, never from ordinary speed or walking detection. */
    public static void dashStarted(LivingEntity entity, Vec3 direction, float intensity, int maximumTicks) {
        if (!(entity.level() instanceof ServerLevel level) || direction.lengthSqr() < .0001) return;
        State state = state(entity);
        if (state.previous == null) {
            state.previous = entity.position(); state.dimension = level.dimension().location();
            state.onGround = entity.onGround(); state.peakY = entity.getY();
        }
        state.dash = true; state.dashDirection = direction.normalize();
        state.dashIntensity = Mth.clamp(intensity, .15F, 1F);
        state.dashUntil = level.getGameTime() + Mth.clamp(maximumTicks, 2, 30);
        send(entity, Kind.DASH_START, entity.position(), state.dashDirection, state.dashIntensity, 0);
    }

    public static void dashStopped(LivingEntity entity) {
        State state = STATES.get(entity);
        if (state == null || !state.dash) return;
        state.dash = false;
        send(entity, Kind.DASH_STOP, entity.position(), state.dashDirection, state.dashIntensity, 0);
    }

    /** Teleport-style dash has real start/end contacts; it must not be mistaken for a high fall. */
    public static void dashTravel(LivingEntity entity, Vec3 start, Vec3 end, float intensity) {
        dashTravel(entity, start, end, intensity, 0);
    }

    public static void spatialTravel(LivingEntity entity, Vec3 start, Vec3 end, float intensity) {
        dashTravel(entity, start, end, intensity, CombatFeedbackPayloads.SPATIAL);
    }

    private static void dashTravel(LivingEntity entity, Vec3 start, Vec3 end, float intensity, int flags) {
        if (start.distanceToSqr(end) < .16 || !(entity.level() instanceof ServerLevel)) return;
        Vec3 direction = end.subtract(start).normalize();
        send(entity, Kind.DASH_START, start, direction, intensity, flags);
        send(entity, Kind.DASH_STOP, end, direction, intensity, flags);
        State state = state(entity);
        state.previous = end; state.previousStep = Vec3.ZERO; state.airTicks = 0;
        state.downwardSpeed = 0; state.peakY = end.y;
    }

    @SubscribeEvent public static void movement(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof LivingEntity living && living.level() instanceof ServerLevel) tickMovement(living);
    }

    static void tickMovement(LivingEntity entity) {
        ServerLevel level = (ServerLevel) entity.level();
        State state = state(entity);
        long now = level.getGameTime();
        if (state.movementTick == now) return;
        boolean stale = state.movementTick != Long.MIN_VALUE && now - state.movementTick > 3;
        state.movementTick = now;
        Vec3 position = entity.position();
        Vec3 step = state.previous == null ? Vec3.ZERO : position.subtract(state.previous);
        boolean skip = !entity.isAlive() || entity.isPassenger() || entity.isInWaterOrBubble()
                || entity.isInLava() || entity.isSpectator();
        boolean discontinuity = stale || state.previous == null || state.dimension == null
                || !state.dimension.equals(level.dimension().location()) || step.lengthSqr() > 64
                || (step.lengthSqr() > 9 && step.subtract(state.previousStep).lengthSqr() > 9);
        if (skip || discontinuity) {
            state.airTicks = 0; state.downwardSpeed = 0; state.peakY = position.y;
            state.dash = false;
        } else {
            if (!entity.onGround()) {
                if (state.onGround) { state.peakY = state.previous.y; state.airTicks = 0; state.downwardSpeed = 0; }
                state.airTicks++;
                state.peakY = Math.max(state.peakY, position.y);
                state.downwardSpeed = Math.max(state.downwardSpeed,
                        Math.max(state.previous.y - position.y, -entity.getDeltaMovement().y));
            } else if (!state.onGround) {
                float intensity = CombatFeedbackProfile.landingIntensity(state.peakY - position.y,
                        state.downwardSpeed, state.airTicks);
                if (intensity > 0 && send(entity, Kind.LANDING, position, new Vec3(0, -1, 0), intensity, 0)
                        && CombatFeedbackProfile.heavy(intensity)) landingCrater(entity, position, intensity);
                state.airTicks = 0; state.downwardSpeed = 0; state.peakY = position.y;
            }
            if (state.dash && (now >= state.dashUntil || entity.horizontalCollision
                    || entity.getDeltaMovement().lengthSqr() < .025)) dashStopped(entity);
        }
        state.onGround = entity.onGround(); state.previous = position;
        state.previousStep = discontinuity || skip ? Vec3.ZERO : step;
        state.dimension = level.dimension().location();
    }

    /** Small irregular topsoil crater; all guardian, ruin, claim and mob-griefing checks stay authoritative. */
    public static int landingCrater(LivingEntity entity, Vec3 center, float intensity) {
        // Player landings are visual only, regardless of fall speed or impact intensity.
        if (entity instanceof net.minecraft.world.entity.player.Player) return 0;
        if (!BlockBreakingSettings.enabled(entity,
                BlockBreakingSettings.Category.COMPANIONS)) return 0;
        if (!(entity.level() instanceof ServerLevel level) || !CombatFeedbackProfile.heavy(intensity)) return 0;
        int removed = 0;
        BlockPos floor = BlockPos.containing(center.x, center.y - .15, center.z);
        double radius = 1.1 + intensity * 1.45;
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
            BlockPos pos = floor.offset(x, 0, z);
            double irregular = .82 + ((Mth.getSeed(pos) >>> 4) & 255) / 255.0 * .36;
            if (x * x + z * z > radius * radius * irregular || removed >= 18) continue;
            var block = level.getBlockState(pos);
            if (block.getDestroySpeed(level, pos) < 0 || block.getDestroySpeed(level, pos) > 3
                    || block.hasBlockEntity() || !block.getFluidState().isEmpty()) continue;
            if (BlockBreakingSettings.withSource(
                    BlockBreakingSettings.Category.COMPANIONS, entity,
                    () -> SpellDestruction.breakBlock(level, pos, false, entity))) removed++;
        }
        return removed;
    }

    static boolean send(LivingEntity entity, Kind kind, Vec3 position, Vec3 direction, float intensity, int flags) {
        if (!(entity.level() instanceof ServerLevel level) || intensity <= 0 || !Float.isFinite(intensity)) return false;
        State state = state(entity);
        long now = level.getGameTime();
        if (state.packetTick != now) { state.packetTick = now; state.packets = 0; state.kinds = 0; }
        int bit = 1 << kind.ordinal();
        if ((state.kinds & bit) != 0 || state.packets >= 4) return false;
        Budget budget = BUDGETS.computeIfAbsent(level, ignored -> new Budget());
        if (budget.tick != now) { budget.tick = now; budget.count = 0; }
        if (budget.count >= MAX_EVENTS_PER_WORLD_TICK) return false;
        state.kinds |= bit; state.packets++; state.emitted++; budget.count++;
        state.lastFlags = flags;
        Effect payload = new Effect(level.dimension().location(), kind, entity.getId(), position, direction,
                intensity, flags, (int) (Mth.getSeed(BlockPos.containing(position))
                        ^ now * 1664525L ^ entity.getId() * 1013904223L ^ kind.ordinal()));
        // Only viewers of the affected chunk receive feedback, further bounded to 64 blocks.
        for (ServerPlayer viewer : level.getChunkSource().chunkMap.getPlayers(
                new net.minecraft.world.level.ChunkPos(BlockPos.containing(position)), false)) {
            if (viewer.distanceToSqr(position) <= 64 * 64) PacketDistributor.sendToPlayer(viewer, payload);
        }
        return true;
    }

    static int emittedCount(LivingEntity entity) { State state = STATES.get(entity); return state == null ? 0 : state.emitted; }
    static int lastInjuryFlags(LivingEntity entity) { State state = STATES.get(entity); return state == null ? 0 : state.lastFlags; }
    @SubscribeEvent public static void stop(ServerStoppedEvent event) { STATES.clear(); BUDGETS.clear(); }
}
