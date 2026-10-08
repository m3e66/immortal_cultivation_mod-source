package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.config.BlockBreakingSettings;

import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.item.ForgingSystem;
import com.example.immortal_cultivation_mod.magicritual.center.CenterEffectSupport;
import com.example.immortal_cultivation_mod.particle.ModParticles;
import com.example.immortal_cultivation_mod.system.SystemShopPrices;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Shared, data-driven combat implementation for the five realm-bestiary beasts.
 * Each registered entity type supplies an immutable kind, while combat state is
 * synchronized so the matching GeckoLib model can animate the real action.
 */
public final class CultivationBestiaryBeastEntity extends Monster implements GeoEntity {
    public static final String QI_REGEN_SUPPRESSION_UNTIL = "SpiritDevouringRegenSuppressionUntil";

    public static int adjustQiRegeneration(LivingEntity entity, int amount) {
        if (entity.getPersistentData().getLong(QI_REGEN_SUPPRESSION_UNTIL)
                > entity.level().getGameTime()) {
            return Math.max(0, Mth.floor(Math.max(0, amount) * 0.85D));
        }
        return Math.max(0, amount);
    }

    private static final EntityDataAccessor<Byte> ACTION =
            SynchedEntityData.defineId(CultivationBestiaryBeastEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> ACTION_TICKS =
            SynchedEntityData.defineId(CultivationBestiaryBeastEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> RESOURCE =
            SynchedEntityData.defineId(CultivationBestiaryBeastEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ARMOR_MASK =
            SynchedEntityData.defineId(CultivationBestiaryBeastEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SHELL_DAMAGE_PACKED =
            SynchedEntityData.defineId(CultivationBestiaryBeastEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SHELL_BREAK_EVENT =
            SynchedEntityData.defineId(CultivationBestiaryBeastEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> VISUAL_FLAGS =
            SynchedEntityData.defineId(CultivationBestiaryBeastEntity.class, EntityDataSerializers.BYTE);

    public static final byte ACTION_NONE = 0;
    public static final byte ACTION_PRIMARY = 1;
    public static final byte ACTION_SECONDARY = 2;
    public static final byte ACTION_TERTIARY = 3;
    public static final byte ACTION_QUATERNARY = 4;
    public static final byte ACTION_QUINARY = 5;
    public static final byte ACTION_ENRAGE = 6;

    private static final int FLAG_BURROWED = 1;
    private static final int FLAG_FORTIFIED = 2;
    private static final int FLAG_INCANDESCENT = 4;
    private static final int FLAG_COLD = 8;
    private static final int FLAG_PANIC = 16;
    private static final int SCORPION_BURROW_ENTRY_TICKS = 16;
    private static final float TORTOISE_SHELL_DURABILITY = 140.0F;
    private static final int TORTOISE_SHELL_FRACTURE_STAGES = 4;
    private static final int TORTOISE_SHELL_RECOIL_TICKS = 12;

    private final BestiaryBeastKind kind;
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private final int[] cooldowns = new int[5];
    private final int[] plateTimers = new int[4];
    private final float[] shellDamage = new float[4];
    private final Set<UUID> chargeVictims = new HashSet<>();

    private int resourceTimeout;
    private int passiveTicks;
    private int counterTicks;
    private int chargeTicks;
    private int incandescentTicks;
    private int coldTicks;
    private int panicTicks;
    private int dodgeInvulnerableTicks;
    private long normalAttackRecovery;
    private long nextThreatCheck;
    private int burrowTicks;
    private UUID burrowTargetId;
    private boolean panicTriggered;
    private boolean enrageTriggered;
    private Vec3 chargeDirection = Vec3.ZERO;
    private int clientShellBreakSection = -1;
    private int clientShellBreakTicks;
    private int clientShellBreakTicksO;

    public CultivationBestiaryBeastEntity(EntityType<? extends CultivationBestiaryBeastEntity> type,
            Level level, BestiaryBeastKind kind) {
        super(type, level);
        this.kind = kind;
        noCulling = true;
        xpReward = switch (kind) {
            case SPIRIT_DEVOURING_RAT -> 18;
            case IRON_SCALE_SCORPION -> 32;
            case WIND_FEATHER_CLIFF_DEER -> 80;
            case STONE_DEVOURING_TORTOISE -> 150;
            case EMBER_FEATHER_FIRE_OWL -> 320;
        };
        if (kind == BestiaryBeastKind.EMBER_FEATHER_FIRE_OWL) {
            moveControl = new FlyingMoveControl(this, 24, true);
            FlyingPathNavigation flying = new FlyingPathNavigation(this, level);
            flying.setCanFloat(true);
            flying.setCanOpenDoors(false);
            flying.setCanPassDoors(false);
            navigation = flying;
            setNoGravity(true);
        }
    }

    public BestiaryBeastKind kind() {
        return kind;
    }

    public byte action() {
        return entityData.get(ACTION);
    }

    public int resource() {
        return entityData.get(RESOURCE);
    }

    public boolean hasArmorSection(int index) {
        return (entityData.get(ARMOR_MASK) & (1 << index)) != 0;
    }

    public float shellDamageProgress(int index) {
        if (index < 0 || index >= 4) {
            return 0.0F;
        }
        int encoded = entityData.get(SHELL_DAMAGE_PACKED) >>> (index * 8) & 0xFF;
        return encoded / 255.0F;
    }

    public float shellBreakRecoil(int index, float partialTick) {
        if (index != clientShellBreakSection || clientShellBreakTicks <= 0) {
            return 0.0F;
        }
        float remaining = Mth.lerp(
                partialTick, clientShellBreakTicksO, clientShellBreakTicks);
        float life = Mth.clamp(
                remaining / TORTOISE_SHELL_RECOIL_TICKS, 0.0F, 1.0F);
        float age = 1.0F - life;
        return Mth.sin(age * Mth.PI * 3.0F) * life;
    }

    public boolean hasVisualFlag(int flag) {
        return (entityData.get(VISUAL_FLAGS) & flag) != 0;
    }

    public static AttributeSupplier.Builder ratAttributes() {
        return attributes(85.0D, 16.0D, 3.0D, 0.38D, 14.0D, 0.05D, 0.75D);
    }

    public static AttributeSupplier.Builder scorpionAttributes() {
        return attributes(150.0D, 30.0D, 9.0D, 0.27D, 15.0D, 0.30D, 0.65D);
    }

    public static AttributeSupplier.Builder deerAttributes() {
        return attributes(310.0D, 60.0D, 8.0D, 0.40D, 18.0D, 0.20D, 1.35D);
    }

    public static AttributeSupplier.Builder tortoiseAttributes() {
        return attributes(950.0D, 75.0D, 26.0D, 0.18D, 20.0D, 0.80D, 0.75D)
                .add(Attributes.ARMOR_TOUGHNESS, 8.0D);
    }

    public static AttributeSupplier.Builder owlAttributes() {
        return attributes(1900.0D, 190.0D, 22.0D, 0.34D, 22.0D, 0.35D, 1.0D)
                .add(Attributes.FLYING_SPEED, 0.48D)
                .add(Attributes.ARMOR_TOUGHNESS, 6.0D);
    }

    private static AttributeSupplier.Builder attributes(double health, double attack, double armor,
            double speed, double range, double knockbackResistance, double stepHeight) {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, health)
                .add(Attributes.ATTACK_DAMAGE, attack)
                .add(Attributes.ARMOR, armor)
                .add(Attributes.MOVEMENT_SPEED, speed)
                .add(Attributes.ATTACK_SPEED, 4.0D)
                .add(Attributes.FOLLOW_RANGE, range)
                .add(Attributes.KNOCKBACK_RESISTANCE, knockbackResistance)
                .add(Attributes.STEP_HEIGHT, stepHeight);
    }

    public static boolean checkSpawnRules(EntityType<CultivationBestiaryBeastEntity> type,
            ServerLevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        boolean owl = type == ModEntities.EMBER_FEATHER_FIRE_OWL.get();
        if (!owl && !Mob.checkMobSpawnRules(type, level, spawnType, pos, random)) {
            return false;
        }
        if (!isNaturalSpawn(spawnType)) {
            return true;
        }
        if (level.getDifficulty() == Difficulty.PEACEFUL
                || (owl && !level.getWorldBorder().isWithinBounds(pos))) {
            return false;
        }

        if (type == ModEntities.GREEN_PATTERNED_SPIRIT_DEVOURING_RAT.get()) {
            return checkRatSpawn(level, pos)
                    && belowNearbyCap(type, level, pos, 64.0D, 8);
        }
        if (type == ModEntities.RED_BACKED_IRON_SCALE_SCORPION.get()) {
            return checkScorpionSpawn(level, pos)
                    && belowNearbyCap(type, level, pos, 64.0D, 5);
        }
        if (type == ModEntities.WIND_FEATHER_CLIFF_DEER.get()) {
            return checkDeerSpawn(level, pos)
                    && belowNearbyCap(type, level, pos, 96.0D, 6);
        }
        if (type == ModEntities.INK_ARMORED_STONE_DEVOURING_TORTOISE.get()) {
            return checkTortoiseSpawn(level, pos)
                    && belowNearbyCap(type, level, pos, 128.0D, 2);
        }
        if (owl) {
            return checkOwlSpawn(level, pos);
        }
        return false;
    }

    private static boolean isNaturalSpawn(MobSpawnType spawnType) {
        return spawnType == MobSpawnType.NATURAL
                || spawnType == MobSpawnType.CHUNK_GENERATION;
    }

    private static boolean checkRatSpawn(ServerLevelAccessor level, BlockPos pos) {
        BlockState floor = level.getBlockState(pos.below());
        if (!(floor.is(BlockTags.DIRT)
                || floor.is(Blocks.MUD)
                || floor.is(Blocks.MOSS_BLOCK)
                || floor.is(Blocks.ROOTED_DIRT)
                || floor.is(Blocks.STONE))) {
            return false;
        }
        if (!hasGroundSupport(level, pos) || !hasClearSpawnVolume(level, pos, 0, 2)) {
            return false;
        }
        return level.canSeeSky(pos)
                ? level.getLevel().isNight()
                : level.getBrightness(LightLayer.SKY, pos) == 0;
    }

    private static boolean checkScorpionSpawn(ServerLevelAccessor level, BlockPos pos) {
        BlockState floor = level.getBlockState(pos.below());
        if (!(floor.is(Blocks.SAND)
                || floor.is(Blocks.RED_SAND)
                || floor.is(BlockTags.TERRACOTTA)
                || floor.is(Blocks.GRAVEL)
                || floor.is(Blocks.DRIPSTONE_BLOCK)
                || floor.is(Blocks.POINTED_DRIPSTONE))) {
            return false;
        }
        if (!hasGroundSupport(level, pos) || !hasClearSpawnVolume(level, pos, 0, 2)) {
            return false;
        }
        return !level.canSeeSky(pos) || level.getLevel().isNight();
    }

    private static boolean checkDeerSpawn(ServerLevelAccessor level, BlockPos pos) {
        return pos.getY() > 90
                && level.canSeeSky(pos)
                && isDayOrDawn(level)
                && hasGroundSupport(level, pos)
                && hasClearSpawnVolume(level, pos, 1, 5);
    }

    private static boolean checkTortoiseSpawn(ServerLevelAccessor level, BlockPos pos) {
        boolean highPeak = pos.getY() > 100 && level.canSeeSky(pos);
        boolean deepCave = pos.getY() < 40
                && level.getBrightness(LightLayer.SKY, pos) == 0;
        if (!highPeak && !deepCave) {
            return false;
        }

        BlockState floor = level.getBlockState(pos.below());
        if (!(floor.is(Blocks.STONE)
                || floor.is(Blocks.DEEPSLATE)
                || floor.is(Blocks.TUFF)
                || floor.is(Blocks.CALCITE)
                || floor.is(Blocks.GRAVEL)
                || floor.is(BlockTags.TERRACOTTA))) {
            return false;
        }
        return hasGroundSupport(level, pos)
                && hasClearSpawnVolume(level, pos, 2, 3);
    }

    private static boolean checkOwlSpawn(ServerLevelAccessor level, BlockPos pos) {
        boolean nether = level.getLevel().dimension().equals(Level.NETHER);
        if (!nether && (pos.getY() <= 140 || !level.getLevel().isNight())) {
            return false;
        }
        return isTenToTwentyFourBlocksAboveTerrain(level, pos)
                && hasCenteredClearVolume(level, pos, 3, 2);
    }

    private static boolean isDayOrDawn(ServerLevelAccessor level) {
        long dayTime = Math.floorMod(level.getLevel().getDayTime(), 24_000L);
        return dayTime < 13_000L || dayTime >= 23_000L;
    }

    private static boolean hasGroundSupport(ServerLevelAccessor level, BlockPos pos) {
        BlockPos floor = pos.below();
        return level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)
                && level.getFluidState(pos).isEmpty();
    }

    private static boolean hasClearSpawnVolume(ServerLevelAccessor level, BlockPos pos,
            int horizontalRadius, int height) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = -horizontalRadius; x <= horizontalRadius; x++) {
            for (int z = -horizontalRadius; z <= horizontalRadius; z++) {
                for (int y = 0; y < height; y++) {
                    cursor.set(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
                    if (!level.getBlockState(cursor).getCollisionShape(level, cursor).isEmpty()
                            || !level.getFluidState(cursor).isEmpty()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean hasCenteredClearVolume(ServerLevelAccessor level, BlockPos pos,
            int horizontalRadius, int verticalRadius) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = -horizontalRadius; x <= horizontalRadius; x++) {
            for (int z = -horizontalRadius; z <= horizontalRadius; z++) {
                for (int y = -verticalRadius; y <= verticalRadius; y++) {
                    cursor.set(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
                    if (!level.getBlockState(cursor).getCollisionShape(level, cursor).isEmpty()
                            || !level.getFluidState(cursor).isEmpty()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean isTenToTwentyFourBlocksAboveTerrain(
            ServerLevelAccessor level, BlockPos pos) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int distance = 1; distance <= 24; distance++) {
            cursor.set(pos.getX(), pos.getY() - distance, pos.getZ());
            BlockState state = level.getBlockState(cursor);
            if (!state.getCollisionShape(level, cursor).isEmpty()
                    || !level.getFluidState(cursor).isEmpty()) {
                return distance >= 10
                        && state.isFaceSturdy(level, cursor, Direction.UP)
                        && level.getFluidState(cursor).isEmpty();
            }
        }
        return false;
    }

    private static boolean belowNearbyCap(EntityType<CultivationBestiaryBeastEntity> type,
            ServerLevelAccessor level, BlockPos pos, double radius, int maximum) {
        double centerX = pos.getX() + 0.5D;
        double centerY = pos.getY() + 0.5D;
        double centerZ = pos.getZ() + 0.5D;
        double radiusSquared = radius * radius;
        AABB area = new AABB(pos).inflate(radius);
        int nearby = level.getEntitiesOfClass(
                CultivationBestiaryBeastEntity.class,
                area,
                beast -> beast.isAlive()
                        && beast.getType() == type
                        && beast.distanceToSqr(centerX, centerY, centerZ) <= radiusSquared
        ).size();
        return nearby < maximum;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        if (kind == BestiaryBeastKind.EMBER_FEATHER_FIRE_OWL) {
            goalSelector.addGoal(5, new WaterAvoidingRandomFlyingGoal(this, 1.0D));
        } else {
            goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9D));
        }
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15D, true) {
            @Override public boolean canUse() {
                return action() == ACTION_NONE && super.canUse();
            }
            @Override public boolean canContinueToUse() {
                return action() == ACTION_NONE && super.canContinueToUse();
            }
        });
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 14.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ACTION, ACTION_NONE);
        builder.define(ACTION_TICKS, 0);
        builder.define(RESOURCE, 0);
        builder.define(ARMOR_MASK, 0b1111);
        builder.define(SHELL_DAMAGE_PACKED, 0);
        builder.define(SHELL_BREAK_EVENT, 0);
        builder.define(VISUAL_FLAGS, (byte) 0);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (!SHELL_BREAK_EVENT.equals(key) || !level().isClientSide) {
            return;
        }
        int event = entityData.get(SHELL_BREAK_EVENT);
        int stage = event >>> 2 & 0b111;
        if (stage <= 0) {
            return;
        }
        clientShellBreakSection = event & 0b11;
        clientShellBreakTicks = TORTOISE_SHELL_RECOIL_TICKS;
        clientShellBreakTicksO = TORTOISE_SHELL_RECOIL_TICKS;
    }

    @Override
    public void tick() {
        super.tick();
        if (kind == BestiaryBeastKind.EMBER_FEATHER_FIRE_OWL && isAlive()) {
            setNoGravity(true);
            fallDistance = 0.0F;
        }
        if (level().isClientSide && isAlive()) {
            clientShellBreakTicksO = clientShellBreakTicks;
            if (clientShellBreakTicks > 0) {
                clientShellBreakTicks--;
            }
            spawnClientParticles();
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide || !isAlive() || isNoAi()) return;
        if (BeastDazeControl.blocksMoves(this)) {
            cancelMovesForDaze();
            return;
        }
        tickTimers();
        tickBurrow();
        tickAction();
        tickResourcesAndPassives();
        tickCharge();

        LivingEntity target = getTarget();
        if (action() == ACTION_NONE && chargeTicks <= 0 && level().getGameTime() >= normalAttackRecovery
                && target != null && target.isAlive()
                && hasLineOfSight(target)) {
            if (!reactToIncomingAttack()) chooseAbility(target);
        }
        if (kind == BestiaryBeastKind.SPIRIT_DEVOURING_RAT && target == null) {
            followValuableSpiritItem();
        }
    }

    /** Shared trajectory recognition; each beast responds only with its own existing move. */
    boolean reactToIncomingAttack() {
        long now = level().getGameTime();
        if (action() != ACTION_NONE || chargeTicks > 0 || now < normalAttackRecovery || now < nextThreatCheck
                || BeastDazeControl.blocksMoves(this)) return false;
        nextThreatCheck = now + 4;
        Entity attack = NpcCombatReactions.projectile(this);
        if (attack == null) return false;
        switch (kind) {
            case SPIRIT_DEVOURING_RAT -> {
                if (cooldowns[1] > 0) return false;
                if (!shadowDodge(damageSources().indirectMagic(attack, null))) return false;
            }
            case IRON_SCALE_SCORPION -> {
                if (cooldowns[2] > 0 || distanceToSqr(attack) > 16) return false;
                armorCounter();
            }
            case WIND_FEATHER_CLIFF_DEER -> {
                if (cooldowns[1] > 0 || distanceToSqr(attack) > 16) return false;
                antlerGale();
            }
            case STONE_DEVOURING_TORTOISE -> {
                if (cooldowns[3] > 0) return false;
                defensiveShell();
            }
            case EMBER_FEATHER_FIRE_OWL -> {
                if (cooldowns[3] > 0 || distanceToSqr(attack) > 36 || resource() < 30) return false;
                heatBurst();
            }
        }
        return true;
    }

    private void tickTimers() {
        for (int i = 0; i < cooldowns.length; i++) {
            if (cooldowns[i] > 0) cooldowns[i]--;
        }
        if (counterTicks > 0) counterTicks--;
        if (dodgeInvulnerableTicks > 0) dodgeInvulnerableTicks--;
        if (resourceTimeout > 0 && --resourceTimeout == 0
                && kind == BestiaryBeastKind.SPIRIT_DEVOURING_RAT) {
            setResource(0);
        }
        if (panicTicks > 0 && --panicTicks == 0) setFlag(FLAG_PANIC, false);
        for (int i = 0; i < plateTimers.length; i++) {
            if (plateTimers[i] > 0 && --plateTimers[i] == 0
                    && kind == BestiaryBeastKind.IRON_SCALE_SCORPION) {
                setArmorSection(i, true);
            }
        }
    }

    private void tickAction() {
        int remaining = entityData.get(ACTION_TICKS);
        if (remaining > 0) {
            entityData.set(ACTION_TICKS, remaining - 1);
            if (remaining == 1) {
                entityData.set(ACTION, ACTION_NONE);
                setFlag(FLAG_BURROWED, false);
                if (kind == BestiaryBeastKind.STONE_DEVOURING_TORTOISE)
                    setFlag(FLAG_FORTIFIED, false);
                noPhysics = false;
                setInvisible(false);
            }
        }
    }

    private void tickResourcesAndPassives() {
        passiveTicks++;
        switch (kind) {
            case SPIRIT_DEVOURING_RAT -> {
                updateRatValuableDetection();
                if (!panicTriggered && getHealth() <= getMaxHealth() * 0.30F) {
                    panicTriggered = true;
                    panicTicks = 120;
                    setFlag(FLAG_PANIC, true);
                    startAction(ACTION_ENRAGE, 20);
                    addEffect(new MobEffectInstance(MobEffects.JUMP, 120, 1, false, true));
                    cooldowns[0] = Math.max(cooldowns[0], 40);
                }
                if ((resource() >= 3 || panicTicks > 0) && getTarget() != null) {
                    fleeFromTarget(11.0D, 1.35D);
                }
            }
            case WIND_FEATHER_CLIFF_DEER -> {
                double horizontal = getDeltaMovement().horizontalDistanceSqr();
                setResource(resource() + (horizontal > 0.020D ? 2 : -1), 0, 100);
                if (!panicTriggered && getHealth() <= getMaxHealth() * 0.40F) {
                    panicTriggered = true;
                    panicTicks = 160;
                    setFlag(FLAG_PANIC, true);
                    setResource(Math.max(50, resource()), 0, 100);
                    startAction(ACTION_ENRAGE, 24);
                }
                if (panicTicks > 0 && resource() < 50) setResource(50);
                if (panicTicks > 0 && getTarget() != null) fleeFromTarget(14.0D, 1.35D);
            }
            case EMBER_FEATHER_FIRE_OWL -> tickOwlHeat();
            default -> { }
        }
        updateDynamicAttributes();
    }

    private void tickOwlHeat() {
        if (incandescentTicks > 0) {
            incandescentTicks--;
            if (incandescentTicks == 0) {
                setFlag(FLAG_INCANDESCENT, false);
                coldTicks = 80;
                setFlag(FLAG_COLD, true);
                setResource(20);
            }
        } else if (coldTicks > 0) {
            coldTicks--;
            if (coldTicks == 0) setFlag(FLAG_COLD, false);
        } else if (getDeltaMovement().horizontalDistanceSqr() > 0.015D && passiveTicks % 8 == 0) {
            addResource(1, 100);
        } else if (passiveTicks % 20 == 0) {
            addResource(-1, 100);
        }
        if (resource() >= 100 && incandescentTicks <= 0 && coldTicks <= 0) {
            incandescentTicks = 120;
            setFlag(FLAG_INCANDESCENT, true);
        }
        if (!enrageTriggered && getHealth() <= getMaxHealth() * 0.25F) {
            enrageTriggered = true;
            panicTicks = 200;
            setFlag(FLAG_PANIC, true);
            addResource(50, 100);
            startAction(ACTION_ENRAGE, 30);
        }
    }

    private void updateDynamicAttributes() {
        AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeInstance armor = getAttribute(Attributes.ARMOR);
        if (speed == null || armor == null) return;
        switch (kind) {
            case SPIRIT_DEVOURING_RAT -> speed.setBaseValue(0.38D
                    * (1.0D + resource() * 0.08D)
                    * (panicTicks > 0 ? 1.25D : 1.0D));
            case WIND_FEATHER_CLIFF_DEER -> speed.setBaseValue(0.40D
                    * (resource() >= 100 ? 1.15D : 1.0D)
                    * (panicTicks > 0 ? 1.30D : 1.0D));
            case STONE_DEVOURING_TORTOISE -> {
                int broken = 4 - Integer.bitCount(entityData.get(ARMOR_MASK) & 0b1111);
                speed.setBaseValue(0.18D * (1.0D + broken * 0.03D));
                armor.setBaseValue(26.0D - broken * 3.0D);
            }
            case EMBER_FEATHER_FIRE_OWL -> {
                double multiplier = hasVisualFlag(FLAG_INCANDESCENT) ? 1.15D
                        : hasVisualFlag(FLAG_COLD) ? 0.80D : 1.0D;
                if (panicTicks > 0) multiplier *= 1.20D;
                speed.setBaseValue(0.34D * multiplier);
                armor.setBaseValue(enrageTriggered && panicTicks > 0 ? 18.7D : 22.0D);
            }
            default -> { }
        }
    }

    private void chooseAbility(LivingEntity target) {
        double distance = distanceTo(target);
        switch (kind) {
            case SPIRIT_DEVOURING_RAT -> {
                if (cooldowns[0] <= 0 && resource() < 3 && distance <= 2.2D) {
                    spiritDevouringBite(target);
                }
            }
            case IRON_SCALE_SCORPION -> {
                if (cooldowns[3] <= 0 && getHealth() > getMaxHealth() * 0.40F
                        && distance > 7.0D && canBurrowHere() && random.nextFloat() < 0.08F) {
                    burrowAmbush(target);
                }
                else if (cooldowns[2] <= 0 && distance < 4.0D && random.nextFloat() < 0.05F) armorCounter();
                else if (cooldowns[1] <= 0 && distance <= 3.5D && random.nextBoolean()) tailSting(target);
                else if (cooldowns[0] <= 0 && distance <= 2.3D) pincerClamp(target);
            }
            case WIND_FEATHER_CLIFF_DEER -> {
                if (cooldowns[2] <= 0 && distance > 6.0D && distance < 16.0D) windFeatherScatter(target);
                else if (cooldowns[1] <= 0 && distance <= 4.5D) antlerGale();
                else if (cooldowns[0] <= 0 && distance > 3.5D && distance < 10.0D) cliffLeap(target);
            }
            case STONE_DEVOURING_TORTOISE -> {
                if (cooldowns[4] <= 0 && distance <= 5.0D) shellSlam();
                else if (cooldowns[3] <= 0 && getHealth() < getMaxHealth() * 0.65F && random.nextFloat() < 0.04F) defensiveShell();
                else if (cooldowns[2] <= 0 && distance < 8.0D && random.nextFloat() < 0.05F) earthArmorRising();
                else if (cooldowns[1] <= 0 && distance >= 6.0D && distance <= 17.0D) stonePelletSpit(target);
                else if (cooldowns[0] <= 0 && distance > 2.5D && distance <= 7.0D) profoundArmorCharge(target);
            }
            case EMBER_FEATHER_FIRE_OWL -> {
                if (cooldowns[4] <= 0 && getY() < target.getY() + 2.0D) featherAscent();
                else if (cooldowns[3] <= 0 && resource() >= 30 && distance <= 6.5D) heatBurst();
                else if (cooldowns[2] <= 0 && distance <= 5.5D) flameWindSweep();
                else if (cooldowns[1] <= 0 && distance > 5.0D && distance <= 19.0D) emberWindDive(target);
                else if (cooldowns[0] <= 0 && distance <= 23.0D) fireFeatherScatter(target);
            }
        }
    }

    private void spiritDevouringBite(LivingEntity target) {
        cooldowns[0] = 100;
        startAction(ACTION_PRIMARY, 18);
        if (hurtTarget(target, 28.0F, false)) {
            CenterEffectSupport.drainQi(target, 8);
            target.getPersistentData().putLong(QI_REGEN_SUPPRESSION_UNTIL,
                    level().getGameTime() + 40L);
            addResource(1, 3);
            resourceTimeout = 240;
            playSound(SoundEvents.GENERIC_EAT, 0.9F, 1.35F);
        }
    }

    private boolean shadowDodge(DamageSource source) {
        Vec3 away = source.getSourcePosition() == null ? getLookAngle().reverse()
                : position().subtract(source.getSourcePosition()).normalize();
        Vec3 side = new Vec3(-away.z, 0.15D, away.x).scale(random.nextBoolean() ? 1.0D : -1.0D);
        double distance = 5.0D + resource() * 0.5D;
        if (!dashAlongClearPath(side.normalize(), distance)
                && !dashAlongClearPath(side.reverse().normalize(), distance)) return false;
        cooldowns[1] = 160;
        startAction(ACTION_SECONDARY, 10);
        removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 12, 1, false, false));
        dodgeInvulnerableTicks = 4;
        return true;
    }

    private void pincerClamp(LivingEntity target) {
        cooldowns[0] = 120;
        startAction(ACTION_PRIMARY, 20);
        if (hurtTarget(target, 55.0F, false)) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 10, false, true));
            Vec3 dir = target.position().subtract(position()).normalize();
            target.push(dir.x * 0.65D, 0.18D, dir.z * 0.65D);
        }
    }

    private void tailSting(LivingEntity target) {
        cooldowns[1] = 180;
        startAction(ACTION_SECONDARY, 24);
        if (hurtTarget(target, 42.0F, false)) {
            target.addEffect(new MobEffectInstance(ModEffects.SHIDU, 80, 1, false, true, true), this);
            target.addEffect(new MobEffectInstance(ModEffects.TECHNIQUE_SLOW, 80, 9, false, true, true), this);
        }
    }

    private void armorCounter() {
        cooldowns[2] = 260;
        counterTicks = 40;
        startAction(ACTION_TERTIARY, 40);
    }

    private void burrowAmbush(LivingEntity target) {
        cooldowns[3] = 320;
        startAction(ACTION_QUATERNARY, 100);
        setFlag(FLAG_BURROWED, true);
        setInvisible(false);
        burrowTicks = 100;
        burrowTargetId = target.getUUID();
    }

    private void cliffLeap(LivingEntity target) {
        cooldowns[0] = 140;
        startAction(ACTION_PRIMARY, 26);
        Vec3 direction = target.position().subtract(position()).normalize();
        double power = resource() >= 100 ? 1.25D : 1.05D;
        setDeltaMovement(direction.x * power, 0.82D, direction.z * power);
        hasImpulse = true;
        if (resource() >= 100) {
            setResource(0);
            damageArea(position().add(direction.scale(6.0D)), 3.0D, 40.0F, 1.1D, false);
        }
    }

    private void antlerGale() {
        cooldowns[1] = 200;
        startAction(ACTION_SECONDARY, 24);
        clearProjectiles(4.0D);
        damageArea(position(), 4.0D, 85.0F, 1.45D, false);
    }

    private void windFeatherScatter(LivingEntity target) {
        cooldowns[2] = 220;
        startAction(ACTION_TERTIARY, 28);
        for (int i = 0; i < 5; i++) {
            spawnProjectile(CultivationBeastProjectileEntity.Kind.WIND_FEATHER,
                    target, 24.0F, 1.25F, 4.0F + i * 1.5F);
        }
    }

    private void profoundArmorCharge(LivingEntity target) {
        cooldowns[0] = 160;
        startAction(ACTION_PRIMARY, 28);
        chargeTicks = 9;
        chargeDirection = target.position().subtract(position()).multiply(1.0D, 0.0D, 1.0D).normalize();
        chargeVictims.clear();
    }

    private void stonePelletSpit(LivingEntity target) {
        cooldowns[1] = 200;
        startAction(ACTION_SECONDARY, 32);
        for (int i = 0; i < 6; i++) {
            float damage = i >= 4 ? 42.0F : 31.5F;
            spawnProjectile(CultivationBeastProjectileEntity.Kind.STONE_PELLET,
                    target, damage, 1.05F, 5.0F + i * 0.8F);
        }
    }

    private void earthArmorRising() {
        cooldowns[2] = 280;
        startAction(ACTION_TERTIARY, 30);
        if (!(level() instanceof ServerLevel server)) return;
        Map<BlockPos, BlockState> ridges = new LinkedHashMap<>();
        for (int i = 0; i < 22; i++) {
            double angle = Math.PI * 2.0D * i / 22.0D + random.nextDouble() * 0.2D;
            double radius = 2.0D + random.nextDouble() * 3.0D;
            int x = Mth.floor(getX() + Math.cos(angle) * radius);
            int z = Mth.floor(getZ() + Math.sin(angle) * radius);
            int y = server.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            ridges.put(pos, (i % 4 == 0 ? Blocks.GILDED_BLACKSTONE : Blocks.COBBLED_DEEPSLATE)
                    .defaultBlockState());
            if (i % 5 == 0) ridges.put(pos.above(), Blocks.TUFF.defaultBlockState());
        }
        BlockBreakingSettings.runWithSource(
                BlockBreakingSettings.Category.COMPANIONS, this,
                () -> TemporaryTerrainEntity.place(server, ridges, 140));
        for (LivingEntity target : hostileTargets(getBoundingBox().inflate(5.0D))) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 140, 1, false, true));
        }
    }

    private void defensiveShell() {
        cooldowns[3] = 360;
        startAction(ACTION_QUATERNARY, 60);
        setFlag(FLAG_FORTIFIED, true);
        addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 10, false, false));
    }

    private void shellSlam() {
        cooldowns[4] = 320;
        startAction(ACTION_QUINARY, 28);
        damageArea(position(), 5.0D, 100.0F, 1.65D, false);
    }

    private void fireFeatherScatter(LivingEntity target) {
        cooldowns[0] = 180;
        startAction(ACTION_PRIMARY, 30);
        int count = enrageTriggered && panicTicks > 0 ? 9 : 7;
        for (int i = 0; i < count; i++) {
            spawnProjectile(CultivationBeastProjectileEntity.Kind.FIRE_FEATHER,
                    target, owlDamage(70.0F), 1.35F, 6.0F + i * 0.7F);
        }
        addResource(12, 100);
    }

    private void emberWindDive(LivingEntity target) {
        cooldowns[1] = 240;
        startAction(ACTION_SECONDARY, 32);
        Vec3 direction = target.position().subtract(position()).normalize();
        setDeltaMovement(direction.scale(resource() >= 70 ? 1.75D : 1.58D));
        hasImpulse = true;
        hurtTarget(target, owlDamage(enrageTriggered ? 670.0F : 420.0F), true);
        damageArea(target.position(), 3.0D, owlDamage(180.0F), 1.35D, true);
        if (level() instanceof ServerLevel server)
            BeastMoveBlockBreaker.breakSphere(server, this, target.position(), 3.2, 2.2, 64, 12);
        addResource(25, 100);
    }

    private void flameWindSweep() {
        cooldowns[2] = 200;
        if (level() instanceof ServerLevel server)
            BeastMoveBlockBreaker.breakSphere(server, this, position(), 2.8, 1.8, 48, 12);
        startAction(ACTION_TERTIARY, 24);
        Vec3 look = getLookAngle().multiply(1.0D, 0.0D, 1.0D).normalize();
        for (LivingEntity target : hostileTargets(getBoundingBox().inflate(5.5D))) {
            Vec3 toward = target.position().subtract(position()).multiply(1.0D, 0.0D, 1.0D).normalize();
            if (look.dot(toward) >= 0.50D && hurtTarget(target, owlDamage(240.0F), true)) {
                Vec3 side = new Vec3(-look.z, 0.0D, look.x)
                        .scale(Math.signum(sideOfLook(target.position())) * 1.2D);
                target.push(side.x, 0.25D, side.z);
            }
        }
        addResource(10, 100);
        clearWeakFrost();
    }

    private void heatBurst() {
        cooldowns[3] = 320;
        if (level() instanceof ServerLevel server)
            BeastMoveBlockBreaker.breakSphere(server, this, position(), 4, 2.5, 96, 12);
        startAction(ACTION_QUATERNARY, 30);
        clearProjectiles(6.0D);
        for (LivingEntity target : hostileTargets(getBoundingBox().inflate(6.0D))) {
            if (target.position().distanceToSqr(position()) > 36.0D) continue;
            if (hurtTarget(target, owlDamage(300.0F), true)) {
                Vec3 away = target.position().subtract(position())
                        .multiply(1.0D, 0.0D, 1.0D).normalize();
                target.push(away.x * 1.2D, 0.3D, away.z * 1.2D);
                target.addEffect(new MobEffectInstance(ModEffects.EMBER_BURN,
                        60, 0, false, true, true), this);
            }
        }
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.5D, getZ(),
                    48, 2.2D, 0.8D, 2.2D, 0.12D);
        }
        addResource(-30, 100);
    }

    private void featherAscent() {
        cooldowns[4] = 280;
        startAction(ACTION_QUINARY, 16);
        setDeltaMovement(getDeltaMovement().x, 1.25D, getDeltaMovement().z);
        removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        hasImpulse = true;
    }

    private float owlDamage(float base) {
        double multiplier = hasVisualFlag(FLAG_INCANDESCENT) ? 1.25D
                : hasVisualFlag(FLAG_COLD) ? 0.85D : resource() >= 70 ? 1.15D : 1.0D;
        if (enrageTriggered && panicTicks > 0) multiplier *= 1.15D;
        return (float) (base * multiplier);
    }

    private void tickCharge() {
        if (chargeTicks <= 0) return;
        chargeTicks--;
        setDeltaMovement(chargeDirection.x * 0.72D, getDeltaMovement().y, chargeDirection.z * 0.72D);
        hasImpulse = true;
        for (LivingEntity target : hostileTargets(getBoundingBox().inflate(0.65D))) {
            if (chargeVictims.add(target.getUUID()) && hurtTarget(target, 150.0F, false)) {
                target.push(chargeDirection.x * 1.5D, 0.45D, chargeDirection.z * 1.5D);
            }
        }
        if (horizontalCollision) chargeTicks = 0;
    }

    private void tickBurrow() {
        if (burrowTicks <= 0 || !(level() instanceof ServerLevel server)) return;
        burrowTicks--;
        if (burrowTicks > 100 - SCORPION_BURROW_ENTRY_TICKS) {
            navigation.stop();
            if ((burrowTicks & 1) == 0) {
                BlockState surface = level().getBlockState(blockPosition().below());
                server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, surface),
                        getX(), getY() + 0.12D, getZ(), 8,
                        0.65D, 0.12D, 0.65D, 0.08D);
            }
            return;
        }
        if (!isInvisible()) setInvisible(true);
        Entity resolved = burrowTargetId == null ? null : server.getEntity(burrowTargetId);
        LivingEntity target = resolved instanceof LivingEntity living && living.isAlive() ? living : null;
        if (target == null || burrowTicks <= 0
                || (burrowTicks <= 88 && distanceToSqr(target) <= 6.25D)) {
            emergeFromBurrow(target);
            return;
        }
        navigation.moveTo(target, 0.72D);
        if ((burrowTicks & 3) == 0) {
            BlockState surface = level().getBlockState(blockPosition().below());
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, surface),
                    getX(), getY() + 0.08D, getZ(), 6, 0.45D, 0.08D, 0.45D, 0.05D);
        }
    }

    private void emergeFromBurrow(LivingEntity target) {
        burrowTicks = 0;
        burrowTargetId = null;
        setFlag(FLAG_BURROWED, false);
        setInvisible(false);
        navigation.stop();
        startAction(ACTION_QUATERNARY, 12);
        if (target != null && distanceToSqr(target) <= 16.0D
                && hurtTarget(target, 60.0F, false)) {
            Vec3 away = target.position().subtract(position()).normalize();
            target.push(away.x * 0.8D, 0.35D, away.z * 0.8D);
        }
    }

    private boolean canBurrowHere() {
        BlockState state = level().getBlockState(blockPosition().below());
        return state.is(Blocks.SAND) || state.is(Blocks.RED_SAND)
                || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.PODZOL) || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.GRAVEL);
    }

    private boolean dashAlongClearPath(Vec3 direction, double requestedDistance) {
        Vec3 flat = direction.multiply(1.0D, 0.0D, 1.0D).normalize();
        if (flat.lengthSqr() < 1.0E-4D) return false;
        double clear = 0.0D;
        for (double distance = 0.25D; distance <= requestedDistance; distance += 0.25D) {
            Vec3 candidate = position().add(flat.scale(distance));
            BlockPos feet = BlockPos.containing(candidate);
            if (!level().hasChunkAt(feet) || !level().getWorldBorder().isWithinBounds(feet)
                    || !level().getFluidState(feet).isEmpty()
                    || !level().getBlockState(feet.below()).isFaceSturdy(level(), feet.below(), Direction.UP)
                    || CenterEffectSupport.isDangerousPosition(this, candidate)
                    || CenterEffectSupport.isForcefieldRouteBlocked(this, position(), candidate)) break;
            if (!level().noCollision(this, getBoundingBox().move(flat.scale(distance)))) break;
            clear = distance;
        }
        if (clear < 1) return false;
        {
            Vec3 dashStart = position();
            teleportTo(getX() + flat.x * clear, getY(), getZ() + flat.z * clear);
            com.example.immortal_cultivation_mod.combat.CombatFeedbackEvents.dashTravel(this, dashStart, position(), .45F);
        }
        setDeltaMovement(flat.scale(0.32D));
        hasImpulse = true;
        return true;
    }

    private void fleeFromTarget(double distance, double speed) {
        LivingEntity target = getTarget();
        if (target == null || passiveTicks % 4 != 0) return;
        Vec3 away = position().subtract(target.position()).multiply(1.0D, 0.0D, 1.0D).normalize();
        if (away.lengthSqr() < 1.0E-4D) away = getLookAngle().reverse();
        Vec3 side = new Vec3(-away.z, 0.0D, away.x)
                .scale((random.nextDouble() - 0.5D) * 4.0D);
        Vec3 destination = position().add(away.scale(distance)).add(side);
        navigation.moveTo(destination.x, destination.y, destination.z, speed);
        setAggressive(false);
    }

    private void updateRatValuableDetection() {
        if (passiveTicks % 10 != 0) return;
        Player nearest = null;
        double nearestDistance = 22.0D * 22.0D;
        for (Player player : level().getEntitiesOfClass(Player.class,
                getBoundingBox().inflate(22.0D), player -> player.isAlive() && !player.isSpectator()
                        && carriesValuableSpiritItem(player))) {
            double distance = distanceToSqr(player);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = player;
            }
        }
        if (nearest != null && (getTarget() == null || nearestDistance < distanceToSqr(getTarget()))) {
            startRatScentAnimation();
            setTarget(nearest);
            if (level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        getX(), getEyeY(), getZ(), 3, 0.12D, 0.08D, 0.12D, 0.01D);
            }
        }
    }

    private void startRatScentAnimation() {
        if (kind != BestiaryBeastKind.SPIRIT_DEVOURING_RAT
                || cooldowns[2] > 0 || action() != ACTION_NONE) return;
        navigation.stop();
        startAction(ACTION_TERTIARY, 20);
        cooldowns[2] = 100;
    }

    private static boolean carriesValuableSpiritItem(Player player) {
        for (ItemStack stack : player.getAllSlots()) {
            if (stack.isEmpty()) continue;
            String path = net.minecraft.core.registries.BuiltInRegistries.ITEM
                    .getKey(stack.getItem()).getPath();
            if (path.endsWith("spirit_stone") || path.contains("pill")
                    || (ForgingSystem.isPurityMaterial(stack)
                    && SystemShopPrices.price(stack) >= 100)) {
                return true;
            }
        }
        return false;
    }

    private void clearWeakFrost() {
        MobEffectInstance frozen = getEffect(ModEffects.FROZEN);
        if (frozen != null && frozen.getAmplifier() <= 0) removeEffect(ModEffects.FROZEN);
        MobEffectInstance frozenQi = getEffect(ModEffects.FROZEN_QI);
        if (frozenQi != null && frozenQi.getAmplifier() <= 0) removeEffect(ModEffects.FROZEN_QI);
        if (getTicksFrozen() < getTicksRequiredToFreeze()) setTicksFrozen(0);
    }

    private void spawnProjectile(CultivationBeastProjectileEntity.Kind projectileKind,
            LivingEntity target, float damage, float speed, float inaccuracy) {
        if (!(level() instanceof ServerLevel server)) return;
        CultivationBeastProjectileEntity projectile = ModEntities.CULTIVATION_BEAST_PROJECTILE.get().create(server);
        if (projectile == null) return;
        projectile.configure(this, projectileKind, damage);
        projectile.setPos(getX(), getEyeY() - 0.12D, getZ());
        Vec3 aim = target.getBoundingBox().getCenter().subtract(projectile.position());
        projectile.shoot(aim.x, aim.y, aim.z, speed, inaccuracy);
        server.addFreshEntity(projectile);
    }

    private void damageArea(Vec3 center, double radius, float damage, double knockback, boolean fire) {
        for (LivingEntity target : hostileTargets(new AABB(center, center).inflate(radius))) {
            if (target.position().distanceToSqr(center) > radius * radius) continue;
            if (hurtTarget(target, damage, true)) {
                Vec3 away = target.position().subtract(center).multiply(1.0D, 0.0D, 1.0D).normalize();
                target.push(away.x * knockback, Math.min(0.5D, knockback * 0.25D), away.z * knockback);
                if (fire) target.igniteForSeconds(3.0F);
            }
        }
        if (level() instanceof ServerLevel server) {
            server.sendParticles(fire ? ParticleTypes.FLAME : ParticleTypes.CLOUD,
                    center.x, center.y + 0.25D, center.z, 28, radius * 0.35D, 0.35D,
                    radius * 0.35D, 0.08D);
        }
    }

    private List<LivingEntity> hostileTargets(AABB box) {
        return level().getEntitiesOfClass(LivingEntity.class, box,
                target -> target.isAlive() && target != this
                        && !(target instanceof CultivationBestiaryBeastEntity));
    }

    private boolean hurtTarget(LivingEntity target, float damage, boolean magic) {
        if (target == null || !target.isAlive()) return false;
        return target.hurt(magic ? damageSources().indirectMagic(this, this)
                : damageSources().mobAttack(this), damage);
    }

    private void clearProjectiles(double radius) {
        for (Projectile projectile : level().getEntitiesOfClass(Projectile.class,
                getBoundingBox().inflate(radius), projectile -> projectile.getOwner() != this)) {
            projectile.discard();
        }
    }

    private double sideOfLook(Vec3 target) {
        Vec3 look = getLookAngle().multiply(1.0D, 0.0D, 1.0D).normalize();
        Vec3 toward = target.subtract(position()).multiply(1.0D, 0.0D, 1.0D).normalize();
        return look.x * toward.z - look.z * toward.x;
    }

    private void followValuableSpiritItem() {
        if (passiveTicks % 20 != 0 || navigation.isInProgress()) return;
        ItemEntity nearest = null;
        double best = 22.0D * 22.0D;
        for (ItemEntity item : level().getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(22.0D),
                item -> !item.getItem().isEmpty() && ForgingSystem.isPurityMaterial(item.getItem()))) {
            double distance = distanceToSqr(item);
            if (distance < best) {
                best = distance;
                nearest = item;
            }
        }
        if (nearest != null) {
            if (cooldowns[2] <= 0 && action() == ACTION_NONE) {
                startRatScentAnimation();
                return;
            }
            if (action() == ACTION_NONE) navigation.moveTo(nearest, 1.15D);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || amount <= 0.0F) return super.hurt(source, amount);
        if (dodgeInvulnerableTicks > 0 && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        if (kind == BestiaryBeastKind.EMBER_FEATHER_FIRE_OWL
                && action() == ACTION_QUINARY && source.is(DamageTypeTags.IS_PROJECTILE)) {
            return false;
        }
        Entity attacker = source.getEntity();
        if (attacker instanceof LivingEntity living) setTarget(living);
        boolean scorpionCounterTriggered = false;

        boolean movesBlocked = BeastDazeControl.blocksMoves(this);
        if (!movesBlocked && kind == BestiaryBeastKind.SPIRIT_DEVOURING_RAT && cooldowns[1] <= 0
                && (source.is(DamageTypeTags.IS_PROJECTILE) || random.nextFloat() < 0.18F)) {
            if (shadowDodge(source)) return false;
        }
        if (!movesBlocked && kind == BestiaryBeastKind.WIND_FEATHER_CLIFF_DEER
                && source.is(DamageTypeTags.IS_PROJECTILE)) {
            float dodge = resource() >= 50 ? 0.10F : 0.0F;
            if (panicTicks > 0) dodge += 0.15F;
            if (random.nextFloat() < dodge) return false;
        }
        if (kind == BestiaryBeastKind.EMBER_FEATHER_FIRE_OWL && source.is(DamageTypeTags.IS_FIRE)) {
            amount *= 0.70F;
        }
        if (kind == BestiaryBeastKind.IRON_SCALE_SCORPION) {
            int section = directionalSection(source, 3);
            if (section >= 0 && hasArmorSection(section)) amount *= 0.75F;
            if (section >= 0 && (amount >= 25.0F || source.is(DamageTypeTags.IS_EXPLOSION))) {
                setArmorSection(section, false);
                plateTimers[section] = 100;
            }
            if (!movesBlocked && counterTicks > 0) {
                counterTicks = 0;
                scorpionCounterTriggered = true;
                amount *= 0.50F;
                if (attacker instanceof LivingEntity living) {
                    living.hurt(damageSources().mobAttack(this), 20.0F);
                    Vec3 away = living.position().subtract(position()).normalize();
                    living.push(away.x * 1.4D, 0.35D, away.z * 1.4D);
                }
            }
        }
        if (kind == BestiaryBeastKind.STONE_DEVOURING_TORTOISE) {
            if (hasVisualFlag(FLAG_FORTIFIED)) {
                amount *= 0.45F;
                setDeltaMovement(Vec3.ZERO);
            }
            int section = directionalSection(source, 4);
            if (section >= 0 && hasArmorSection(section)) {
                int previousStage = tortoiseShellFractureStage(shellDamage[section]);
                shellDamage[section] = Math.min(
                        TORTOISE_SHELL_DURABILITY, shellDamage[section] + amount);
                syncTortoiseShellDamage(section);
                int newStage = tortoiseShellFractureStage(shellDamage[section]);
                if (newStage > previousStage) {
                    emitTortoiseShellFracture(section, newStage);
                }
                if (shellDamage[section] >= TORTOISE_SHELL_DURABILITY) {
                    setArmorSection(section, false);
                }
            } else if (section >= 0) {
                amount *= 1.15F;
            }
        }
        boolean damaged = super.hurt(source, amount);
        if (isAlive() && kind == BestiaryBeastKind.IRON_SCALE_SCORPION) {
            if (scorpionCounterTriggered) {
                startAction(ACTION_QUINARY, 8);
            } else if (damaged && action() == ACTION_NONE) {
                startAction(ACTION_ENRAGE, 7);
            }
        }
        if (damaged && isAlive() && kind == BestiaryBeastKind.SPIRIT_DEVOURING_RAT
                && action() == ACTION_NONE) {
            startAction(ACTION_QUATERNARY, 7);
        }
        if (damaged && isAlive() && kind == BestiaryBeastKind.WIND_FEATHER_CLIFF_DEER
                && action() == ACTION_NONE) {
            startAction(ACTION_QUINARY, 7);
        }
        return damaged;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (BeastDazeControl.blocksMoves(this)
                || action() != ACTION_NONE || hasVisualFlag(FLAG_FORTIFIED)
                || chargeTicks > 0 || level().getGameTime() < normalAttackRecovery) return false;
        if (kind == BestiaryBeastKind.SPIRIT_DEVOURING_RAT
                && (resource() >= 3 || panicTicks > 0) && !horizontalCollision) {
            return false;
        }
        boolean hit = super.doHurtTarget(target);
        normalAttackRecovery = level().getGameTime() + 10;
        if (hit && kind == BestiaryBeastKind.WIND_FEATHER_CLIFF_DEER) {
            startAction(ACTION_QUATERNARY, 12);
        }
        return hit;
    }

    private int directionalSection(DamageSource source, int count) {
        Vec3 origin = source.getSourcePosition();
        if (origin == null) return -1;
        Vec3 local = origin.subtract(position()).multiply(1.0D, 0.0D, 1.0D);
        if (local.lengthSqr() < 0.01D) return 0;
        if (count == 4) {
            double yaw = getYRot() * Mth.DEG_TO_RAD;
            Vec3 forward = new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
            Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
            Vec3 direction = local.normalize();
            double forwardAmount = direction.dot(forward);
            double rightAmount = direction.dot(right);
            if (Math.abs(forwardAmount) >= Math.abs(rightAmount)) {
                return forwardAmount >= 0.0D ? 0 : 2;
            }
            return rightAmount >= 0.0D ? 1 : 3;
        }
        double attackAngle = Mth.atan2(local.z, local.x) * Mth.RAD_TO_DEG - getYRot();
        float wrapped = Mth.wrapDegrees((float) attackAngle);
        if (count == 3) {
            if (Math.abs(wrapped) <= 60.0F) return 0;
            return wrapped > 0.0F ? 1 : 2;
        }
        return Mth.floor((Mth.wrapDegrees(wrapped) + 180.0F) / 90.0F) & 3;
    }

    private void startAction(byte action, int ticks) {
        if (BeastDazeControl.blocksMoves(this)) {
            return;
        }
        entityData.set(ACTION, action);
        entityData.set(ACTION_TICKS, Math.max(1, ticks));
    }

    private void cancelMovesForDaze() {
        entityData.set(ACTION, ACTION_NONE);
        entityData.set(ACTION_TICKS, 0);
        chargeTicks = 0;
        chargeDirection = Vec3.ZERO;
        chargeVictims.clear();
        burrowTicks = 0;
        burrowTargetId = null;
        counterTicks = 0;
        dodgeInvulnerableTicks = 0;
        setFlag(FLAG_BURROWED, false);
        setFlag(FLAG_FORTIFIED, false);
        noPhysics = false;
        setInvisible(false);
        getNavigation().stop();
        Vec3 motion = getDeltaMovement();
        setDeltaMovement(0.0D, motion.y, 0.0D);
    }

    private void setResource(int value) {
        setResource(value, 0, kind == BestiaryBeastKind.SPIRIT_DEVOURING_RAT ? 3 : 100);
    }

    private void setResource(int value, int minimum, int maximum) {
        entityData.set(RESOURCE, Mth.clamp(value, minimum, maximum));
    }

    private void addResource(int amount, int maximum) {
        setResource(resource() + amount, 0, maximum);
    }

    private void setArmorSection(int index, boolean present) {
        int mask = entityData.get(ARMOR_MASK);
        entityData.set(ARMOR_MASK, present ? mask | (1 << index) : mask & ~(1 << index));
    }

    private static int tortoiseShellFractureStage(float damage) {
        return Mth.clamp(
                Mth.floor(damage * TORTOISE_SHELL_FRACTURE_STAGES
                        / TORTOISE_SHELL_DURABILITY),
                0, TORTOISE_SHELL_FRACTURE_STAGES);
    }

    private void syncTortoiseShellDamage(int section) {
        int shift = section * 8;
        int encoded = Mth.clamp(
                Math.round(shellDamage[section] * 255.0F
                        / TORTOISE_SHELL_DURABILITY),
                0, 255);
        int packed = entityData.get(SHELL_DAMAGE_PACKED);
        entityData.set(SHELL_DAMAGE_PACKED,
                packed & ~(0xFF << shift) | encoded << shift);
    }

    private void emitTortoiseShellFracture(int section, int stage) {
        int previousEvent = entityData.get(SHELL_BREAK_EVENT);
        int sequence = (previousEvent >>> 5) + 1 & 0x07FFFFFF;
        entityData.set(SHELL_BREAK_EVENT,
                sequence << 5 | (stage & 0b111) << 2 | section & 0b11);

        boolean destroyed = stage >= TORTOISE_SHELL_FRACTURE_STAGES;
        playSound(destroyed ? SoundEvents.DEEPSLATE_BREAK : SoundEvents.DEEPSLATE_HIT,
                destroyed ? 1.35F : 0.85F,
                destroyed ? 0.72F : 0.88F + stage * 0.05F);
        if (!(level() instanceof ServerLevel server)) {
            return;
        }

        Vec3 center = tortoiseShellSectionCenter(section);
        int fragments = destroyed ? 34 : 7 + stage * 4;
        server.sendParticles(
                new BlockParticleOption(
                        ParticleTypes.BLOCK,
                        (destroyed ? Blocks.GILDED_BLACKSTONE : Blocks.DEEPSLATE_TILES)
                                .defaultBlockState()),
                center.x, center.y, center.z,
                fragments,
                destroyed ? 0.55D : 0.32D,
                destroyed ? 0.36D : 0.22D,
                destroyed ? 0.55D : 0.32D,
                destroyed ? 0.16D : 0.09D);
        server.sendParticles(
                ParticleTypes.SMOKE,
                center.x, center.y, center.z,
                destroyed ? 12 : 3 + stage,
                0.35D, 0.22D, 0.35D, 0.025D);
    }

    private Vec3 tortoiseShellSectionCenter(int section) {
        double yaw = getYRot() * Mth.DEG_TO_RAD;
        Vec3 forward = new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
        Vec3 outward = switch (section) {
            case 0 -> forward;
            case 1 -> right;
            case 2 -> forward.reverse();
            case 3 -> right.reverse();
            default -> Vec3.ZERO;
        };
        return position().add(outward.scale(0.95D)).add(0.0D, 1.05D, 0.0D);
    }

    private void setFlag(int flag, boolean enabled) {
        int flags = Byte.toUnsignedInt(entityData.get(VISUAL_FLAGS));
        flags = enabled ? flags | flag : flags & ~flag;
        entityData.set(VISUAL_FLAGS, (byte) flags);
    }

    private void spawnClientParticles() {
        if (tickCount % 3 != 0) return;
        switch (kind) {
            case SPIRIT_DEVOURING_RAT -> {
                if (resource() > 0) level().addParticle(ParticleTypes.HAPPY_VILLAGER,
                        getRandomX(0.7D), getRandomY(), getRandomZ(0.7D), 0.0D, 0.015D, 0.0D);
            }
            case IRON_SCALE_SCORPION -> level().addParticle(ParticleTypes.SMALL_FLAME,
                    getRandomX(0.9D), getY() + 0.4D, getRandomZ(0.9D), 0.0D, 0.01D, 0.0D);
            case WIND_FEATHER_CLIFF_DEER -> {
                if (resource() >= 50) level().addParticle(ModParticles.METHOD_QI_WIND.get(),
                        getRandomX(0.8D), getRandomY(), getRandomZ(0.8D), 0.0D, 0.02D, 0.0D);
            }
            case STONE_DEVOURING_TORTOISE -> {
                if (hasVisualFlag(FLAG_FORTIFIED)) level().addParticle(
                        new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DEEPSLATE.defaultBlockState()),
                        getRandomX(1.0D), getY() + 0.4D, getRandomZ(1.0D), 0.0D, 0.03D, 0.0D);
            }
            case EMBER_FEATHER_FIRE_OWL -> level().addParticle(
                    hasVisualFlag(FLAG_INCANDESCENT) ? ParticleTypes.FLAME : ParticleTypes.SMALL_FLAME,
                    getRandomX(1.5D), getRandomY(), getRandomZ(1.5D), 0.0D, 0.03D, 0.0D);
        }
    }

    @Override
    public boolean fireImmune() {
        return kind == BestiaryBeastKind.EMBER_FEATHER_FIRE_OWL || super.fireImmune();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("BestiaryResource", resource());
        tag.putInt("BestiaryArmorMask", entityData.get(ARMOR_MASK));
        tag.putByte("BestiaryFlags", entityData.get(VISUAL_FLAGS));
        tag.putBoolean("PanicTriggered", panicTriggered);
        tag.putBoolean("EnrageTriggered", enrageTriggered);
        tag.putInt("PanicTicks", panicTicks);
        tag.putInt("IncandescentTicks", incandescentTicks);
        tag.putInt("ColdTicks", coldTicks);
        tag.putInt("DodgeInvulnerableTicks", dodgeInvulnerableTicks);
        for (int i = 0; i < cooldowns.length; i++) tag.putInt("Cooldown" + i, cooldowns[i]);
        for (int i = 0; i < shellDamage.length; i++) tag.putFloat("ShellDamage" + i, shellDamage[i]);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setResource(tag.getInt("BestiaryResource"));
        entityData.set(ARMOR_MASK, tag.contains("BestiaryArmorMask")
                ? tag.getInt("BestiaryArmorMask") & 0b1111 : 0b1111);
        entityData.set(VISUAL_FLAGS, tag.getByte("BestiaryFlags"));
        panicTriggered = tag.getBoolean("PanicTriggered");
        enrageTriggered = tag.getBoolean("EnrageTriggered");
        panicTicks = Math.max(0, tag.getInt("PanicTicks"));
        incandescentTicks = Math.max(0, tag.getInt("IncandescentTicks"));
        coldTicks = Math.max(0, tag.getInt("ColdTicks"));
        dodgeInvulnerableTicks = Math.max(0, tag.getInt("DodgeInvulnerableTicks"));
        burrowTicks = 0;
        burrowTargetId = null;
        for (int i = 0; i < cooldowns.length; i++) cooldowns[i] = Math.max(0, tag.getInt("Cooldown" + i));
        for (int i = 0; i < shellDamage.length; i++) {
            shellDamage[i] = Mth.clamp(
                    tag.getFloat("ShellDamage" + i),
                    0.0F, TORTOISE_SHELL_DURABILITY);
            if (kind == BestiaryBeastKind.STONE_DEVOURING_TORTOISE
                    && !hasArmorSection(i)) {
                shellDamage[i] = TORTOISE_SHELL_DURABILITY;
            }
            syncTortoiseShellDamage(i);
        }
        entityData.set(ACTION, ACTION_NONE);
        entityData.set(ACTION_TICKS, 0);
        setInvisible(false);
        noPhysics = false;
        if (kind == BestiaryBeastKind.EMBER_FEATHER_FIRE_OWL) setNoGravity(true);
        updateDynamicAttributes();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 3, state -> {
            String prefix = "animation." + kind.id() + ".";
            if (isDeadOrDying()) return state.setAndContinue(RawAnimation.begin().thenPlayAndHold(prefix + "death"));
            String actionName = switch (action()) {
                case ACTION_PRIMARY -> "primary";
                case ACTION_SECONDARY -> "secondary";
                case ACTION_TERTIARY -> kind == BestiaryBeastKind.SPIRIT_DEVOURING_RAT
                        ? "sniff" : "tertiary";
                case ACTION_QUATERNARY -> kind == BestiaryBeastKind.SPIRIT_DEVOURING_RAT
                        ? "hurt"
                        : kind == BestiaryBeastKind.IRON_SCALE_SCORPION
                                ? hasVisualFlag(FLAG_BURROWED) ? "burrow" : "emerge"
                                : kind == BestiaryBeastKind.WIND_FEATHER_CLIFF_DEER
                                        ? "horn_strike"
                                : "quaternary";
                case ACTION_QUINARY -> kind == BestiaryBeastKind.IRON_SCALE_SCORPION
                        ? "counter_hit"
                        : kind == BestiaryBeastKind.WIND_FEATHER_CLIFF_DEER
                                ? "hurt" : "quinary";
                case ACTION_ENRAGE -> kind == BestiaryBeastKind.IRON_SCALE_SCORPION
                        ? "hurt"
                        : kind == BestiaryBeastKind.WIND_FEATHER_CLIFF_DEER
                                ? "panic" : "enrage";
                default -> null;
            };
            if (actionName != null) return state.setAndContinue(RawAnimation.begin().thenPlay(prefix + actionName));
            if (state.isMoving()) {
                String movement = kind == BestiaryBeastKind.EMBER_FEATHER_FIRE_OWL ? "fly"
                        : (hasVisualFlag(FLAG_PANIC)
                                || (kind == BestiaryBeastKind.SPIRIT_DEVOURING_RAT && resource() >= 3)
                                || (kind == BestiaryBeastKind.WIND_FEATHER_CLIFF_DEER
                                        && getDeltaMovement().horizontalDistanceSqr() > 0.012D)
                                || (kind == BestiaryBeastKind.IRON_SCALE_SCORPION && isAggressive()))
                                        ? "run" : "walk";
                return state.setAndContinue(RawAnimation.begin().thenLoop(prefix + movement));
            }
            return state.setAndContinue(RawAnimation.begin().thenLoop(prefix + "idle"));
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}
