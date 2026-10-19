package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.item.QiInfusedWeapon;
import com.example.immortal_cultivation_mod.item.WhipItem;
import com.example.immortal_cultivation_mod.effect.ExternalQiVfxShape;
import com.example.immortal_cultivation_mod.magicritual.center.ForcefieldInterceptable;
import com.example.immortal_cultivation_mod.particle.MethodQiParticleEffects;
import com.example.immortal_cultivation_mod.combat.CombatStateManager;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class WeaponQiSlashEntity extends Entity implements ForcefieldInterceptable {
    private static final double SPEED = 1.4D;
    private static final int MAX_LIFETIME_TICKS = 100;
    private static final float MAX_DAMAGE = 1_000_000_000.0F;
    private static final int MAX_BLOCK_BREAKS = 65_536;

    private final Set<UUID> hitEntities = new HashSet<>();
    private final Set<Long> checkedBlocks = new HashSet<>();
    private ItemStack bindingWeapon = ItemStack.EMPTY;
    private UUID ownerUuid;
    private UUID restrictedTargetUuid;
    private Vec3 direction = new Vec3(0.0D, 0.0D, 1.0D);
    private double speed = SPEED;
    private float damage = 1.0F;
    private double radius = 1.25D;
    private double maxDistance = 8.0D;
    private double distanceTraveled;
    private int remainingBlocks;
    private int[] sectionBlocks;
    private ExternalQiVfxShape.BladeMask activeSections = ExternalQiVfxShape.ALL_SECTIONS;
    private int sectionImpactsThisTick;
    private int slashStage;
    private boolean blockBreaking = true;
    private boolean cone;
    private boolean expandingCone;
    private boolean heavyAttack;
    private boolean whip;
    private float whipQiDamage;
    private boolean whipHitRegistered;
    private String element = "any";

    public WeaponQiSlashEntity(EntityType<? extends WeaponQiSlashEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
    }

    /** Lifetime terrain capacity; actual work still shares SpellDestruction's global tick budget. */
    public static int damageScaledBlockBudget(float damage) {
        double strength = Float.isNaN(damage) ? 0 : Math.max(0, damage);
        return (int) Math.min(MAX_BLOCK_BREAKS, Math.ceil(strength * 16));
    }

    @Override
    public Entity forcefieldOwner() {
        return ownerUuid != null && level() instanceof ServerLevel serverLevel
                ? serverLevel.getEntity(ownerUuid)
                : null;
    }

    @Override
    public Vec3 forcefieldMovement() {
        return direction.scale(Math.min(speed,
                Math.max(0.0D, maxDistance - distanceTraveled)));
    }

    @Override
    public float forcefieldBarrierDamage() {
        return damage;
    }

    @Override
    public boolean forcefieldPassThrough() {
        return false;
    }

    @Override
    public void forcefieldBounce(Vec3 reflectedMovement) {
        if (reflectedMovement.lengthSqr() > 1.0E-9D) {
            direction = safeDirection(reflectedMovement);
            speed = reflectedMovement.length();
        }
        setDeltaMovement(direction.scale(speed));
        hurtMarked = true;
        hasImpulse = true;
    }

    @Override
    public double forcefieldSupport(Vec3 normal) {
        double forwardReach = speed + 0.75D;
        SlashBasis slashBasis = basis();
        if (cone) {
            Vec3 up = direction.cross(slashBasis.right).normalize();
            double radialProjection = Math.sqrt(
                    Math.pow(normal.dot(slashBasis.right), 2.0D)
                            + Math.pow(normal.dot(up), 2.0D));
            return Math.abs(normal.dot(direction)) * forwardReach
                    + radialProjection * currentConeRadius();
        }
        double thickness = ExternalQiVfxShape.halfThickness(radius);
        return Math.abs(normal.dot(direction)) * forwardReach
                + Math.abs(normal.dot(slashBasis.axis)) * ExternalQiVfxShape.bladeReach(radius)
                + Math.abs(normal.dot(slashBasis.perpendicular)) * thickness;
    }

    @Override
    public double forcefieldMaximumSupport() {
        double forwardReach = speed + 0.75D;
        double radialReach = cone ? currentConeRadius() : ExternalQiVfxShape.bladeReach(radius);
        if (cone) {
            return Math.sqrt(forwardReach * forwardReach + radialReach * radialReach);
        }
        double thickness = ExternalQiVfxShape.halfThickness(radius);
        return Math.sqrt(forwardReach * forwardReach + radialReach * radialReach
                + thickness * thickness);
    }

    public static boolean release(ServerPlayer owner, int slashStage, boolean heavyCone) {
        return release(owner, slashStage, heavyCone, heavyCone);
    }

    @Override public ItemStack getWeaponItem() { return bindingWeapon; }

    public boolean allowsTerrainBreaking() { return blockBreaking; }

    public static boolean release(ServerPlayer owner, int slashStage, boolean heavyCone, boolean heavyWeaponAttack) {
        ItemStack weapon = owner.getMainHandItem();
        if (!com.example.immortal_cultivation_mod.item.WeaponBinding.checkUse(weapon, owner)) return false;
        com.example.immortal_cultivation_mod.item.WeaponBinding.prepare(weapon);
        boolean fistWeapon = QiInfusedWeapon.isFistWeapon(weapon);
        if (!CombatStateManager.isInCombat(owner)
                || !QiInfusedWeapon.outwardQiReleaseEnabled(owner)
                || !QiInfusedWeapon.supportsOutwardQi(weapon)) {
            return false;
        }

        double injectedQi = QiInfusedWeapon.getQi(weapon);
        if (injectedQi <= 0.0D) {
            return false;
        }

        WeaponQiSlashEntity slash = ModEntities.WEAPON_QI_SLASH.get().create(owner.serverLevel());
        if (slash == null) {
            return false;
        }

        double qiPower = injectedQi / 10.0D;
        float injectedQiDamage =
                QiInfusedWeapon.injectedQiDamage(weapon);
        double baseDamage = Math.max(1.0D, QiInfusedWeapon.baseAttackDamage(weapon));
        com.example.immortal_cultivation_mod.item.WeaponBinding.prepare(owner.getMainHandItem());
        slash.bindingWeapon = owner.getMainHandItem().copy();
        slash.ownerUuid = owner.getUUID();
        slash.direction = safeDirection(owner.getLookAngle());
        slash.damage = (float) Math.min(
                MAX_DAMAGE,
                baseDamage + injectedQiDamage);
        slash.cone = fistWeapon || heavyCone;
        slash.expandingCone = fistWeapon;
        slash.heavyAttack = fistWeapon && heavyCone;
        if (fistWeapon) {
            if (heavyCone) {
                slash.maxDistance = Math.min(
                        36.0D, 7.0D + Math.sqrt(qiPower) * 0.90D);
            } else {
                slash.maxDistance = Math.min(
                        24.0D, 4.0D + Math.sqrt(qiPower) * 0.60D);
            }
        } else if (slash.cone) {
            slash.maxDistance = Math.min(
                    128.0D, 4.0D + Math.sqrt(qiPower) * 1.4D);
        } else {
            slash.maxDistance = Math.min(
                    128.0D, 40.0D + Math.sqrt(qiPower) * 1.2D);
        }
        slash.remainingBlocks = damageScaledBlockBudget(slash.damage);
        slash.slashStage = Math.floorMod(slashStage, 3);
        slash.blockBreaking = heavyWeaponAttack && QiInfusedWeapon.outwardQiBlockBreakingEnabled(owner);
        slash.radius = com.example.immortal_cultivation_mod.combat.ExternalQiSize.radius(weapon,
                com.example.immortal_cultivation_mod.combat.ExternalQiSize.scale(owner), slash.cone, heavyCone, false);
        double originalRadius = slash.radius;
        slash.radius = ExternalQiSpaceFit.fit(owner, slash.direction, slash.slashStage, slash.radius, slash.cone);
        boolean fittedToSpace = slash.radius < originalRadius;


        Vec3 right = rightVector(slash.direction);
        Vec3 up = slash.direction.cross(right).normalize();
        Vec3 startOffset = (slash.cone || fittedToSpace)
                ? Vec3.ZERO
                : switch (slash.slashStage) {
                    case 0 -> right.scale(0.32D).add(up.scale(0.24D));
                    case 1 -> right.scale(-0.28D).add(up.scale(-0.20D));
                    default -> right.scale(0.34D);
                };
        Vec3 start = owner.getEyePosition()
                .add(0.0D, -0.42D, 0.0D)
                .add(slash.direction.scale(1.05D))
                .add(startOffset);
        slash.moveTo(start.x, start.y, start.z, owner.getYRot(), owner.getXRot());
        slash.setDeltaMovement(slash.direction.scale(SPEED));

        QiInfusedWeapon.consumeInjectedQiDamage(
                weapon,
                injectedQiDamage);
        owner.serverLevel().addFreshEntity(slash);
        com.example.immortal_cultivation_mod.guidance.DaoGuidanceSystem.record(
                owner,
                com.example.immortal_cultivation_mod.guidance.DaoGuidanceContent.EXTERNAL_QI);
        return true;
    }

    public static boolean releaseWhip(
            ServerPlayer owner,
            float damage,
            float qiDamage,
            double range) {
        if (!com.example.immortal_cultivation_mod.item.WeaponBinding.checkUse(owner.getMainHandItem(), owner)) return false;
        WeaponQiSlashEntity slash = ModEntities.WEAPON_QI_SLASH.get()
                .create(owner.serverLevel());
        if (slash == null) {
            return false;
        }

        com.example.immortal_cultivation_mod.item.WeaponBinding.prepare(owner.getMainHandItem());
        slash.bindingWeapon = owner.getMainHandItem().copy();
        slash.ownerUuid = owner.getUUID();
        slash.direction = safeDirection(owner.getLookAngle());
        slash.damage = Math.max(
                1.0F, Math.min(MAX_DAMAGE, damage));
        slash.radius = com.example.immortal_cultivation_mod.combat.ExternalQiSize.radius(owner.getMainHandItem(),
                com.example.immortal_cultivation_mod.combat.ExternalQiSize.scale(owner), false, false, true);
        slash.maxDistance = Math.max(1.0D, range);
        slash.remainingBlocks = damageScaledBlockBudget(slash.damage);
        slash.slashStage = Math.floorMod(owner.tickCount, 3);
        // Whip external Qi is emitted by its normal attack, not its charged strike.
        slash.blockBreaking = false;
        slash.cone = false;
        slash.whip = true;
        slash.whipQiDamage = Math.max(0.0F, qiDamage);
        slash.radius = ExternalQiSpaceFit.fit(owner, slash.direction, slash.slashStage, slash.radius, false);

        Vec3 start = owner.getEyePosition()
                .add(0.0D, -0.35D, 0.0D)
                .add(slash.direction.scale(1.0D));
        slash.moveTo(
                start.x,
                start.y,
                start.z,
                owner.getYRot(),
                owner.getXRot());
        slash.setDeltaMovement(slash.direction.scale(SPEED));
        owner.serverLevel().addFreshEntity(slash);
        return true;
    }

    public static boolean releaseNpc(
            PathfinderMob owner,
            LivingEntity target,
            double range,
            float damage,
            double ignoredLegacyRadius,
            String element,
            int slashStage,
            boolean heavyCone) {
        if (!(owner.level() instanceof ServerLevel serverLevel)
                || !com.example.immortal_cultivation_mod.item.WeaponBinding.canUse(owner.getMainHandItem(), owner)
                || target == null
                || !isEnemy(owner, target)) {
            return false;
        }

        WeaponQiSlashEntity slash = ModEntities.WEAPON_QI_SLASH.get().create(serverLevel);
        if (slash == null) {
            return false;
        }

        Vec3 ownerEye = owner.getEyePosition();
        Vec3 targetEye = target.getEyePosition();
        double predictionTicks = Math.min(
                12.0D,
                ownerEye.distanceTo(targetEye) / SPEED);
        Vec3 predictedTargetEye = targetEye.add(
                target.getDeltaMovement().scale(predictionTicks));

        com.example.immortal_cultivation_mod.item.WeaponBinding.prepare(owner.getMainHandItem());
        slash.bindingWeapon = owner.getMainHandItem().copy();
        slash.ownerUuid = owner.getUUID();
        slash.restrictedTargetUuid = heavyCone ? null : target.getUUID();
        slash.direction = safeDirection(predictedTargetEye.subtract(ownerEye));
        slash.damage = Math.max(1.0F, Math.min(MAX_DAMAGE, damage));
        // NPCs use the same weapon-level size as players; caster damage does not enlarge it.
        slash.radius = com.example.immortal_cultivation_mod.combat.ExternalQiSize.radius(owner.getMainHandItem(),
                1, QiInfusedWeapon.isFistWeapon(owner.getMainHandItem()) || heavyCone, heavyCone, false);
        slash.maxDistance = Math.max(1.0D, Math.min(128.0D, range));
        slash.remainingBlocks = damageScaledBlockBudget(slash.damage);
        slash.slashStage = Math.floorMod(slashStage, 3);
        slash.blockBreaking = serverLevel.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING);
        boolean fistWeapon = QiInfusedWeapon.isFistWeapon(owner.getMainHandItem());
        slash.cone = fistWeapon || heavyCone;
        slash.expandingCone = fistWeapon;
        slash.element = normalizeElement(element);

        Vec3 right = rightVector(slash.direction);
        Vec3 up = slash.direction.cross(right).normalize();
        Vec3 startOffset = slash.cone
                ? Vec3.ZERO
                : switch (slash.slashStage) {
                    case 0 -> right.scale(0.32D).add(up.scale(0.24D));
                    case 1 -> right.scale(-0.28D).add(up.scale(-0.20D));
                    default -> right.scale(0.34D);
                };
        Vec3 start = ownerEye
                .add(0.0D, -0.42D, 0.0D)
                .add(slash.direction.scale(1.05D))
                .add(startOffset);
        slash.moveTo(start.x, start.y, start.z, owner.getYRot(), owner.getXRot());
        slash.setDeltaMovement(slash.direction.scale(SPEED));
        return serverLevel.addFreshEntity(slash);
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        setNoGravity(true);
        noPhysics = true;
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Entity resolvedOwner = ownerUuid == null
                ? null
                : serverLevel.getEntity(ownerUuid);
        LivingEntity owner = resolvedOwner instanceof LivingEntity living
                ? living
                : null;
        if (owner == null || !owner.isAlive() || !com.example.immortal_cultivation_mod.item.WeaponBinding.canUse(bindingWeapon, owner) || tickCount > MAX_LIFETIME_TICKS
                || distanceTraveled >= maxDistance) {
            discard();
            return;
        }

        if (usesBladeSections()) {
            tickBladeSections(serverLevel, owner);
            return;
        }

        Vec3 movement = direction.scale(Math.min(
                speed, maxDistance - distanceTraveled));
        BlockHitResult whipBlockHit = null;
        boolean stoppedBySolidBlock = false;
        Vec3 destination = position().add(movement);
        BlockHitResult hit = serverLevel.clip(new ClipContext(
                position(),
                destination,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                this));
        if (hit.getType() != HitResult.Type.MISS) {
            if (whip) {
                whipBlockHit = hit;
                destination = hit.getLocation();
                movement = destination.subtract(position());
            } else if (!blockBreaking) {
                stoppedBySolidBlock = true;
                destination = hit.getLocation();
                movement = destination.subtract(position());
            }
        }

        setPos(destination);
        setDeltaMovement(movement);
        distanceTraveled += movement.length();

        SlashBasis basis = basis();
        if (cone) {
            renderCone(serverLevel, owner, basis);
            hitConeTargets(serverLevel, owner, basis);
            if (blockBreaking) {
                breakConeBlocks(serverLevel, owner, basis);
            }
        } else {
            renderSlash(serverLevel, owner, basis);
            hitTargets(serverLevel, owner, basis);
            if (blockBreaking) {
                breakBlocks(serverLevel, owner, basis);
            }
        }
        if (whipBlockHit != null || stoppedBySolidBlock) {
            sendVisual(serverLevel, owner, com.example.immortal_cultivation_mod.network.HigherWeaponPayloads.Visual.QI_IMPACT,
                    destination, destination.add(direction), Math.min(1.4, .45 + radius * .06));
        }
        if (whipBlockHit != null) {
            if (random.nextFloat() < 0.05F) {
                if (owner instanceof ServerPlayer playerOwner) {
                    WhipItem.createQiContactPop(
                            playerOwner, whipBlockHit.getLocation(), bindingWeapon);
                }
            }
            discard();
        } else if (stoppedBySolidBlock) {
            discard();
        }
    }

    private void renderCone(ServerLevel level, LivingEntity owner, SlashBasis basis) {
        sendVisual(level, owner, com.example.immortal_cultivation_mod.network.HigherWeaponPayloads.Visual.QI_CONE,
                position().subtract(getDeltaMovement()), position(), currentConeRadius());
    }

    private boolean usesBladeSections() {
        return !cone && !whip;
    }

    private void initBladeSections() {
        if (sectionBlocks != null) return;
        int count = ExternalQiVfxShape.sectionCount(radius);
        sectionBlocks = new int[count];
        activeSections = activeSections.and(ExternalQiVfxShape.allSections(radius));
        // Distribute the original capacity evenly across many local strips, without adding capacity.
        for (int i = 0; i < count; i++) {
            sectionBlocks[i] = remainingBlocks / count
                    + ((i + 1) * (remainingBlocks % count) / count - i * (remainingBlocks % count) / count);
        }
    }

    private void tickBladeSections(ServerLevel level, LivingEntity owner) {
        initBladeSections();
        sectionImpactsThisTick = 0;
        Vec3 movement = direction.scale(Math.min(speed, maxDistance - distanceTraveled));
        setPos(position().add(movement));
        setDeltaMovement(movement);
        distanceTraveled += movement.length();
        SlashBasis basis = basis();
        traceBladeSections(level, owner, basis);
        if (activeSections.empty()) {
            // Clear the last client fragments immediately, rather than leaving an old full slash behind.
            renderSlash(level, owner, basis);
            discard();
            return;
        }
        // Terrain resolves first so a consumed section cannot render or hurt through the obstacle.
        renderSlash(level, owner, basis);
        hitTargets(level, owner, basis);
    }

    private void stopSection(ServerLevel level, LivingEntity owner, int section, Vec3 at) {
        activeSections = activeSections.without(section);
        // Large walls can consume dozens of strips at once; keep contact VFX traffic bounded.
        if (sectionImpactsThisTick++ >= 4) return;
        sendVisual(level, owner, com.example.immortal_cultivation_mod.network.HigherWeaponPayloads.Visual.QI_IMPACT,
                at, at.add(direction), Math.min(1.4, .45 + radius * .06));
    }

    private void traceBladeSections(ServerLevel level, LivingEntity owner, SlashBasis basis) {
        double reach = ExternalQiVfxShape.bladeReach(radius);
        int count = sectionBlocks.length;
        // Sample each strip separately, including both boundaries, with at most half-block gaps.
        int samples = Math.max(2, (int) Math.ceil(reach * 4 / count));
        Vec3 movement = getDeltaMovement();
        int forwardSteps = Math.max(1, Math.min(8, (int) Math.ceil(movement.length() * 2)));
        for (int step = 0; step <= forwardSteps; step++) {
            Vec3 center = position().subtract(movement.scale(1 - step / (double) forwardSteps));
            for (int section = 0; section < count; section++) {
                for (int sampleIndex = 0; sampleIndex <= samples; sampleIndex++) {
                    if (!activeSections.active(section)) break;
                    double t = net.minecraft.util.Mth.lerp(sampleIndex / (double) samples,
                            ExternalQiVfxShape.particleStart(radius, section), ExternalQiVfxShape.particleEnd(radius, section));
                    double thickness = ExternalQiVfxShape.halfThickness(radius) * ExternalQiVfxShape.bladeTaper(radius, t);
                    int bands = Math.max(1, (int) Math.ceil(thickness * 2));
                    Vec3 point = ExternalQiVfxShape.crescent(center, direction, basis.axis, radius, t);
                    for (int band = 0; band <= bands; band++) {
                        Vec3 sample = point.add(basis.perpendicular.scale(thickness * (-1 + 2.0 * band / bands)));
                        BlockPos pos = BlockPos.containing(sample);
                        if (!level.hasChunkAt(pos)) {
                            stopSection(level, owner, section, sample);
                            break;
                        }
                        if (checkedBlocks.contains(pos.asLong())) continue;
                        BlockState state = level.getBlockState(pos);
                        if (state.isAir()) continue;
                        if (!blockBreaking) {
                            if (!state.getCollisionShape(level, pos).isEmpty()) {
                                stopSection(level, owner, section, sample);
                                break;
                            }
                            continue;
                        }
                        if (sectionBlocks[section] <= 0 || state.getDestroySpeed(level, pos) < 0
                                || (owner instanceof ServerPlayer player && !player.mayUseItemAt(pos, Direction.UP, player.getMainHandItem()))
                                || !com.example.immortal_cultivation_mod.spell.SpellDestruction.canExternalQiBreakBlock(level, pos, owner)
                                || !com.example.immortal_cultivation_mod.spell.SpellDestruction.breakExternalQiBlock(level, pos, false, owner)) {
                            stopSection(level, owner, section, sample);
                            break;
                        }
                        // Accepted queue entries count once, even if a neighbouring section samples the same block.
                        checkedBlocks.add(pos.asLong());
                        remainingBlocks--;
                        if (--sectionBlocks[section] == 0) {
                            stopSection(level, owner, section, sample);
                            break;
                        }
                    }
                }
            }
        }
    }

    private void sendVisual(ServerLevel level, LivingEntity owner,
            com.example.immortal_cultivation_mod.network.HigherWeaponPayloads.Visual shape,
            Vec3 from, Vec3 to, double size) {
        var particles = MethodQiParticleEffects.particlesForOwner(owner, element);
        String particle = particles.isEmpty() ? "method_qi_neutral"
                : net.minecraft.core.registries.BuiltInRegistries.PARTICLE_TYPE.getKey(particles.getFirst()).getPath();
        com.example.immortal_cultivation_mod.network.HigherWeaponPayloads.send(level, shape, from, to, size,
                com.example.immortal_cultivation_mod.effect.ExternalQiVfxShape.color(particle),
                usesBladeSections() ? activeSections : ExternalQiVfxShape.ALL_SECTIONS,
                shape == com.example.immortal_cultivation_mod.network.HigherWeaponPayloads.Visual.QI_IMPACT ? -1 : getId());
    }

    private void hitConeTargets(ServerLevel level, LivingEntity owner, SlashBasis basis) {
        double coneRadius = currentConeRadius();
        Vec3 up = direction.cross(basis.right).normalize();
        AABB search = new AABB(position(), position()).inflate(coneRadius + 2.0D);
        for (LivingEntity target : level.getEntitiesOfClass(
                LivingEntity.class,
                search,
                living -> canHit(owner, living) && !hitEntities.contains(living.getUUID()))) {
            Vec3 offset = target.getBoundingBox().getCenter().subtract(position());
            double padding = Math.max(target.getBbWidth(), target.getBbHeight()) * 0.55D;
            double forwardDistance = offset.dot(direction);
            double rightDistance = offset.dot(basis.right);
            double upDistance = offset.dot(up);
            double radialDistance = Math.sqrt(
                    rightDistance * rightDistance + upDistance * upDistance);
            if (Math.abs(forwardDistance) > speed + 0.75D + padding
                    || radialDistance > coneRadius + padding
                    || !hasClearPath(level, target)) {
                continue;
            }

            hitEntities.add(target.getUUID());
            if (whip) {
                if (!(owner instanceof ServerPlayer playerOwner)) {
                    continue;
                }
                boolean hit = WhipItem.hitFromQiLine(
                        playerOwner,
                        target,
                        damage,
                        whipQiDamage,
                        !whipHitRegistered, bindingWeapon);
                if (hit) {
                    whipHitRegistered = true;
                }
                if (random.nextFloat() < 0.05F) {
                    WhipItem.createQiContactPop(
                            playerOwner,
                            target.getBoundingBox().getCenter(), bindingWeapon);
                }
                continue;
            }
            target.invulnerableTime = 0;
            hurtTarget(owner, target, damage);
        }
    }

    private void breakConeBlocks(ServerLevel level, LivingEntity owner, SlashBasis basis) {
        double coneRadius = currentConeRadius();
        Vec3 up = direction.cross(basis.right).normalize();
        int radialSteps = Math.max(1, (int) Math.ceil(coneRadius));
        for (int radialStep = 0; radialStep <= radialSteps; radialStep++) {
            double sampleRadius = Math.min(coneRadius, radialStep);
            int points = radialStep == 0
                    ? 1
                    : Math.max(8, (int) Math.ceil(Math.PI * 2.0D * sampleRadius));
            for (int pointIndex = 0; pointIndex < points; pointIndex++) {
                double angle = Math.PI * 2.0D * pointIndex / points;
                Vec3 sample = position()
                        .add(basis.right.scale(Math.cos(angle) * sampleRadius))
                        .add(up.scale(Math.sin(angle) * sampleRadius));
                BlockPos pos = BlockPos.containing(sample);
                if (!level.hasChunkAt(pos) || !checkedBlocks.add(pos.asLong())) {
                    continue;
                }
                BlockState state = level.getBlockState(pos);
                if (state.isAir()) {
                    continue;
                }
                if (remainingBlocks <= 0
                        || state.getDestroySpeed(level, pos) < 0.0F
                        || (owner instanceof ServerPlayer player && !player.mayUseItemAt(
                                pos, Direction.UP, player.getMainHandItem()))) {
                    discard();
                    return;
                }
                if (com.example.immortal_cultivation_mod.spell.SpellDestruction.breakExternalQiBlock(level,pos, false, owner)) {
                    remainingBlocks--;
                }
            }
        }
    }

    private double currentConeRadius() {
        double progress = maxDistance <= 0.0D
                ? 1.0D
                : Math.max(0.0D, Math.min(1.0D, distanceTraveled / maxDistance));
        if (expandingCone) {
            return Math.max(0.16D, radius * (0.18D + progress * 0.82D));
        }
        return Math.max(0.16D, radius * (1.0D - progress));
    }

    private void renderSlash(ServerLevel level, LivingEntity owner, SlashBasis basis) {
        var shape = switch (slashStage) {
            case 0 -> com.example.immortal_cultivation_mod.network.HigherWeaponPayloads.Visual.QI_RISING;
            case 1 -> com.example.immortal_cultivation_mod.network.HigherWeaponPayloads.Visual.QI_FALLING;
            default -> com.example.immortal_cultivation_mod.network.HigherWeaponPayloads.Visual.QI_LEVEL;
        };
        sendVisual(level, owner, shape, position().subtract(getDeltaMovement()), position(), radius);
    }

    private void hitTargets(ServerLevel level, LivingEntity owner, SlashBasis basis) {
        double reach = ExternalQiVfxShape.bladeReach(radius);
        AABB search = new AABB(position(), position()).inflate(reach + 2.0D);
        for (LivingEntity target : level.getEntitiesOfClass(
                LivingEntity.class,
                search,
                living -> canHit(owner, living) && !hitEntities.contains(living.getUUID()))) {
            Vec3 offset = target.getBoundingBox().getCenter().subtract(position());
            double padding = projectedHalfSize(target, basis.axis);
            double forwardDistance = offset.dot(direction);
            double axisDistance = offset.dot(basis.axis);
            double perpendicularDistance = offset.dot(basis.perpendicular);
            if (usesBladeSections() && !overlapsActiveSection(axisDistance, padding, reach)) continue;
            double thickness = ExternalQiVfxShape.halfThickness(radius) * ExternalQiVfxShape.bladeTaper(
                    radius, Math.max(0, Math.abs(axisDistance) - padding) / reach);
            if (Math.abs(forwardDistance) > speed + 0.75D + projectedHalfSize(target, direction)
                    || Math.abs(axisDistance) > reach + padding
                    || Math.abs(perpendicularDistance) > thickness + projectedHalfSize(target, basis.perpendicular)
                    || !(usesBladeSections() ? hasClearBladePath(level, target, basis, axisDistance, reach)
                            : hasClearPath(level, target))) {
                continue;
            }

            hitEntities.add(target.getUUID());
            target.invulnerableTime = 0;
            hurtTarget(owner, target, damage);
        }
    }

    private void breakBlocks(ServerLevel level, LivingEntity owner, SlashBasis basis) {
        // Sample the blade's thickness as well as its length and swept path.
        // Fixed size limits and the shared destruction queue bound the work.
        double reach = ExternalQiVfxShape.bladeReach(radius);
        int segments = Math.max(8, Math.min((int) (ExternalQiVfxShape.bladeReach(ExternalQiVfxShape.MAX_RADIUS) * 4),
                (int) Math.ceil(reach * 4.0D)));
        Vec3 movement = getDeltaMovement();
        int forwardSteps = Math.max(1, Math.min(8, (int) Math.ceil(movement.length() * 2)));
        for (int step = 0; step <= forwardSteps; step++) {
            Vec3 center = position().subtract(movement.scale(1 - step / (double) forwardSteps));
            for (int index = 0; index <= segments; index++) {
                double t = -1.0D + 2.0D * index / segments;
                double thickness = ExternalQiVfxShape.halfThickness(radius) * ExternalQiVfxShape.bladeTaper(radius, t);
                int thicknessSteps = Math.max(1, (int) Math.ceil(thickness * 2));
                Vec3 bladePoint = ExternalQiVfxShape.crescent(center, direction, basis.axis, radius, t);
                for (int band = 0; band <= thicknessSteps; band++) {
                    double offset = thickness * (-1 + 2 * band / (double) thicknessSteps);
                    BlockPos pos = BlockPos.containing(bladePoint.add(basis.perpendicular.scale(offset)));
                    if (!level.hasChunkAt(pos) || checkedBlocks.contains(pos.asLong())) continue;
                    BlockState state = level.getBlockState(pos);
                    // Do not retain every air sample for the entire flight.
                    if (state.isAir()) continue;
                    checkedBlocks.add(pos.asLong());
                    if (remainingBlocks <= 0 || state.getDestroySpeed(level, pos) < 0.0F
                            || (owner instanceof ServerPlayer player && !player.mayUseItemAt(
                                    pos, Direction.UP, player.getMainHandItem()))) {
                        discard();
                        return;
                    }
                    if (com.example.immortal_cultivation_mod.spell.SpellDestruction.breakExternalQiBlock(level, pos, false, owner)) {
                        remainingBlocks--;
                    }
                }
            }
        }
    }

    private static double projectedHalfSize(LivingEntity target, Vec3 axis) {
        AABB box = target.getBoundingBox();
        return .5 * (box.getXsize() * Math.abs(axis.x) + box.getYsize() * Math.abs(axis.y)
                + box.getZsize() * Math.abs(axis.z));
    }

    private boolean overlapsActiveSection(double distance, double padding, double reach) {
        for (int section = 0; section < ExternalQiVfxShape.sectionCount(radius); section++) {
            if (!activeSections.active(section)) continue;
            double low = reach * ExternalQiVfxShape.particleStart(radius, section);
            double high = reach * ExternalQiVfxShape.particleEnd(radius, section);
            if (distance + padding >= low && distance - padding <= high) return true;
        }
        return false;
    }

    private boolean hasClearBladePath(ServerLevel level, LivingEntity target, SlashBasis basis,
                                      double axisDistance, double reach) {
        // Trace from the surviving section, not from the possibly obstructed middle of the slash.
        for (int section = 0; section < ExternalQiVfxShape.sectionCount(radius); section++) {
            if (!activeSections.active(section)) continue;
            double padding = projectedHalfSize(target, basis.axis);
            if (axisDistance + padding < reach * ExternalQiVfxShape.particleStart(radius, section)
                    || axisDistance - padding > reach * ExternalQiVfxShape.particleEnd(radius, section)) continue;
            double t = Math.clamp(axisDistance / reach, ExternalQiVfxShape.particleStart(radius, section), ExternalQiVfxShape.particleEnd(radius, section));
            Vec3 start = ExternalQiVfxShape.crescent(position(), direction, basis.axis, radius, t);
            if (level.clip(new ClipContext(start, target.getBoundingBox().getCenter(),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS) return true;
        }
        return false;
    }

    private SlashBasis basis() {
        Vec3 right = rightVector(direction);
        Vec3 axis = com.example.immortal_cultivation_mod.effect.ExternalQiVfxShape.axis(direction, slashStage);
        Vec3 perpendicular = direction.cross(axis).normalize();
        return new SlashBasis(right, axis, perpendicular);
    }

    private static Vec3 safeDirection(Vec3 direction) {
        return direction.lengthSqr() < 1.0E-6D
                ? new Vec3(0.0D, 0.0D, 1.0D)
                : direction.normalize();
    }

    private static Vec3 rightVector(Vec3 forward) {
        Vec3 right = new Vec3(0.0D, 1.0D, 0.0D).cross(forward);
        if (right.lengthSqr() < 1.0E-6D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        }
        return right.normalize();
    }

    private static boolean isEnemy(LivingEntity owner, LivingEntity target) {
        return target != owner
                && target.isAlive()
                && (owner instanceof CultivationNpc
                ? NpcProfile.isCombatEnemy(owner, target)
                : !NpcProfile.areCombatAllies(owner, target))
                && owner.canAttack(target);
    }

    private boolean canHit(LivingEntity owner, LivingEntity target) {
        return (restrictedTargetUuid == null || restrictedTargetUuid.equals(target.getUUID()))
                && isEnemy(owner, target);
    }

    private boolean hasClearPath(ServerLevel level, LivingEntity target) {
        HitResult hit = level.clip(new ClipContext(
                position(),
                target.getBoundingBox().getCenter(),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                this));
        return hit.getType() == HitResult.Type.MISS;
    }

    private void hurtTarget(
            LivingEntity owner,
            LivingEntity target,
            float damage) {
        boolean damaged;
        if (owner instanceof ServerPlayer player) {
            var source = new net.minecraft.world.damagesource.DamageSource(
                    owner.damageSources().playerAttack(player).typeHolder(), this, player, position());
            if (heavyAttack) {
                damaged = CombatStateManager.hurtAsHeavy(
                        owner, target, source, damage);
            } else {
                damaged = target.hurt(source, damage);
            }
        } else if (owner instanceof Mob mob) {
            damaged = target.hurt(owner.damageSources().indirectMagic(this, mob), damage);
        } else {
            damaged = target.hurt(owner.damageSources().indirectMagic(this, owner), damage);
        }
        if (damaged && level() instanceof ServerLevel serverLevel) {
            Vec3 at = target.getBoundingBox().getCenter();
            sendVisual(serverLevel, owner, com.example.immortal_cultivation_mod.network.HigherWeaponPayloads.Visual.QI_IMPACT,
                    at, at.add(direction), Math.min(1.4, .45 + radius * .06));
        }
    }

    private static String normalizeElement(String element) {
        return element == null || element.isBlank() ? "any"
                : com.example.immortal_cultivation_mod.attachment.SpiritRoots.normalizeElementSpec(element);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        bindingWeapon = ItemStack.parseOptional(registryAccess(), tag.getCompound("BindingWeapon"));
        if (tag.hasUUID("Owner")) {
            ownerUuid = tag.getUUID("Owner");
        }
        restrictedTargetUuid = tag.hasUUID("RestrictedTarget") ? tag.getUUID("RestrictedTarget") : null;
        direction = safeDirection(new Vec3(
                tag.getDouble("DirectionX"),
                tag.getDouble("DirectionY"),
                tag.getDouble("DirectionZ")));
        speed = tag.contains("Speed")
                ? Math.max(0.05D, tag.getDouble("Speed"))
                : SPEED;
        damage = Math.max(1.0F, tag.getFloat("Damage"));
        radius = Math.clamp(tag.getDouble("Radius"), 0.05D, ExternalQiVfxShape.MAX_RADIUS);
        maxDistance = Math.max(1.0D, tag.getDouble("MaxDistance"));
        distanceTraveled = Math.max(0.0D, tag.getDouble("DistanceTraveled"));
        remainingBlocks = Math.max(0, tag.getInt("RemainingBlocks"));
        restoreBladeSections(tag);
        slashStage = Math.floorMod(tag.getInt("SlashStage"), 3);
        blockBreaking = !tag.contains("BlockBreaking") || tag.getBoolean("BlockBreaking");
        cone = tag.getBoolean("Cone");
        expandingCone = tag.getBoolean("ExpandingCone");
        heavyAttack = tag.getBoolean("HeavyAttack");
        whip = tag.getBoolean("Whip");
        whipQiDamage = Math.max(0.0F, tag.getFloat("WhipQiDamage"));
        whipHitRegistered = tag.getBoolean("WhipHitRegistered");
        element = normalizeElement(tag.getString("Element"));
        setNoGravity(true);
        noPhysics = true;
    }

    private void restoreBladeSections(CompoundTag tag) {
        int[] saved = tag.getIntArray("SectionBlocks");
        sectionBlocks = null;
        activeSections = ExternalQiVfxShape.ALL_SECTIONS;
        if (saved.length == 0 || saved.length > 128) return;
        var oldMask = new ExternalQiVfxShape.BladeMask(tag.getLong("ActiveSections"), tag.getLong("ActiveSectionsHigh"));
        int count = ExternalQiVfxShape.sectionCount(radius);
        if (saved.length == count) {
            sectionBlocks = saved;
            activeSections = oldMask.and(ExternalQiVfxShape.allSections(radius));
            return;
        }
        if (count < saved.length) {
            sectionBlocks = new int[count];
            activeSections = ExternalQiVfxShape.allSections(radius);
            for (int old = 0; old < saved.length; old++) {
                int piece = Math.min(count - 1, (int) ((old + .5) * count / saved.length));
                sectionBlocks[piece] += Math.max(0, saved[old]);
                if (!oldMask.active(old)) activeSections = activeSections.without(piece);
            }
            return;
        }
        // Subdivide in-flight older slashes without reviving consumed pieces or adding capacity.
        int[] children = new int[saved.length];
        int[] assigned = new int[saved.length];
        for (int i = 0; i < count; i++) children[Math.min(saved.length - 1, (int) ((i + .5) * saved.length / count))]++;
        sectionBlocks = new int[count];
        activeSections = new ExternalQiVfxShape.BladeMask(0, 0);
        for (int i = 0; i < count; i++) {
            int parent = Math.min(saved.length - 1, (int) ((i + .5) * saved.length / count));
            int capacity = Math.max(0, saved[parent]);
            sectionBlocks[i] = capacity / children[parent] + (assigned[parent]++ < capacity % children[parent] ? 1 : 0);
            if (oldMask.active(parent)) activeSections = activeSections.with(i);
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (!bindingWeapon.isEmpty()) tag.put("BindingWeapon", bindingWeapon.saveOptional(registryAccess()));
        if (ownerUuid != null) {
            tag.putUUID("Owner", ownerUuid);
        }
        if (restrictedTargetUuid != null) {
            tag.putUUID("RestrictedTarget", restrictedTargetUuid);
        }
        tag.putDouble("DirectionX", direction.x);
        tag.putDouble("DirectionY", direction.y);
        tag.putDouble("DirectionZ", direction.z);
        tag.putDouble("Speed", speed);
        tag.putFloat("Damage", damage);
        tag.putDouble("Radius", radius);
        tag.putDouble("MaxDistance", maxDistance);
        tag.putDouble("DistanceTraveled", distanceTraveled);
        tag.putInt("RemainingBlocks", remainingBlocks);
        if (sectionBlocks != null) {
            tag.putIntArray("SectionBlocks", sectionBlocks);
            tag.putLong("ActiveSections", activeSections.low());
            tag.putLong("ActiveSectionsHigh", activeSections.high());
        }
        tag.putInt("SlashStage", slashStage);
        tag.putBoolean("BlockBreaking", blockBreaking);
        tag.putBoolean("Cone", cone);
        tag.putBoolean("ExpandingCone", expandingCone);
        tag.putBoolean("HeavyAttack", heavyAttack);
        tag.putBoolean("Whip", whip);
        tag.putFloat("WhipQiDamage", whipQiDamage);
        tag.putBoolean("WhipHitRegistered", whipHitRegistered);
        tag.putString("Element", element);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    private record SlashBasis(Vec3 right, Vec3 axis, Vec3 perpendicular) {
    }
}
