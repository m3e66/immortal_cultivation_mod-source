package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;

import static com.example.immortal_cultivation_mod.entity.BoneHitboxAnimation.ZERO;
import static com.example.immortal_cultivation_mod.entity.BoneHitboxAnimation.clip;
import static com.example.immortal_cultivation_mod.entity.BoneHitboxAnimation.curve;
import static com.example.immortal_cultivation_mod.entity.BoneHitboxAnimation.positionAndRotation;
import static com.example.immortal_cultivation_mod.entity.BoneHitboxAnimation.rotation;
import static com.example.immortal_cultivation_mod.entity.BoneHitboxAnimation.scaleAndRotation;

public final class AzureSpiritSilkwormEntity extends Animal implements GeoEntity {
    public static final float MODEL_SCALE = 0.65F;

    private static final EntityDataAccessor<Boolean> COCOONED =
            SynchedEntityData.defineId(AzureSpiritSilkwormEntity.class,
                    EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> SPIT_TICKS =
            SynchedEntityData.defineId(AzureSpiritSilkwormEntity.class,
                    EntityDataSerializers.INT);
    private static final int SPIT_ANIMATION_TICKS = 15;

    private static final RawAnimation IDLE = RawAnimation.begin()
            .thenLoop("animation.immortal_cultivation_mod.azure_spirit_silkworm.idle");
    private static final RawAnimation CRAWL = RawAnimation.begin()
            .thenLoop("animation.immortal_cultivation_mod.azure_spirit_silkworm.crawl");
    private static final RawAnimation SPIT = RawAnimation.begin()
            .thenPlay("animation.immortal_cultivation_mod.azure_spirit_silkworm.spit");
    private static final RawAnimation COCOON = RawAnimation.begin()
            .thenLoop("animation.immortal_cultivation_mod.azure_spirit_silkworm.cocoon");

    private static final BoneHitboxAnimation.Clip IDLE_HITBOX_ANIMATION = clip(2.0F, true,
            positionAndRotation("body_mid", ZERO, ZERO, ZERO,
                    ZERO, curve(0.0F, 0.0F, 1.0F, 0.25F, 2.0F, 0.0F), ZERO),
            rotation("tail", ZERO,
                    curve(0.0F, -3.0F, 1.0F, 3.0F, 2.0F, -3.0F), ZERO));

    private static final BoneHitboxAnimation.Clip CRAWL_HITBOX_ANIMATION = clip(0.8F, true,
            rotation("body_front", ZERO,
                    curve(0.0F, -8.0F, 0.4F, 8.0F, 0.8F, -8.0F), ZERO),
            positionAndRotation("body_mid", ZERO, ZERO, ZERO,
                    ZERO, curve(0.0F, 0.0F, 0.2F, 0.4F, 0.4F, 0.0F,
                            0.6F, 0.4F, 0.8F, 0.0F), ZERO),
            rotation("body_back", ZERO,
                    curve(0.0F, 10.0F, 0.4F, -10.0F, 0.8F, 10.0F), ZERO),
            rotation("tail", ZERO,
                    curve(0.0F, 16.0F, 0.4F, -16.0F, 0.8F, 16.0F), ZERO));

    private static final BoneHitboxAnimation.Clip SPIT_HITBOX_ANIMATION = clip(0.75F, false,
            rotation("body_front",
                    curve(0.0F, 0.0F, 0.2F, -18.0F, 0.42F, 12.0F, 0.75F, 0.0F),
                    ZERO, ZERO),
            rotation("head",
                    curve(0.0F, 0.0F, 0.2F, -20.0F, 0.42F, 18.0F, 0.75F, 0.0F),
                    ZERO, ZERO));

    private static final BoneHitboxAnimation.Clip COCOON_HITBOX_ANIMATION = clip(1.5F, true,
            scaleAndRotation("root", ZERO,
                    curve(0.0F, -2.0F, 0.75F, 2.0F, 1.5F, -2.0F), ZERO,
                    curve(0.0F, 0.9F, 0.75F, 1.0F, 1.5F, 0.9F),
                    curve(0.0F, 1.15F, 0.75F, 1.25F, 1.5F, 1.15F),
                    curve(0.0F, 0.9F, 0.75F, 1.0F, 1.5F, 0.9F)));

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ModelBonePartEntity<AzureSpiritSilkwormEntity> bodyMidPart;
    private final ModelBonePartEntity<AzureSpiritSilkwormEntity> bodyFrontPart;
    private final ModelBonePartEntity<AzureSpiritSilkwormEntity> headPart;
    private final ModelBonePartEntity<AzureSpiritSilkwormEntity> bodyBackPart;
    private final ModelBonePartEntity<AzureSpiritSilkwormEntity> tailPart;
    private final ModelBonePartEntity<?>[] boneParts;
    private int spitCooldown;
    private int cocoonTicks;
    private int cocoonCooldown;
    private DamageSource lastExplosionDamageSource;
    private long lastExplosionDamageTick = Long.MIN_VALUE;

    public AzureSpiritSilkwormEntity(
            EntityType<? extends AzureSpiritSilkwormEntity> entityType, Level level) {
        super(entityType, level);
        bodyMidPart = new ModelBonePartEntity<>(this, "body_mid", 0.36F, 0.30F);
        bodyFrontPart = new ModelBonePartEntity<>(this, "body_front", 0.34F, 0.28F);
        headPart = new ModelBonePartEntity<>(this, "head", 0.31F, 0.26F);
        bodyBackPart = new ModelBonePartEntity<>(this, "body_back", 0.34F, 0.28F);
        tailPart = new ModelBonePartEntity<>(this, "tail", 0.28F, 0.22F);
        boneParts = new ModelBonePartEntity<?>[]{
                bodyMidPart, bodyFrontPart, headPart, bodyBackPart, tailPart
        };
        setId(ENTITY_COUNTER.getAndAdd(boneParts.length + 1) + 1);
        noCulling = true;
        xpReward = 12;
        updateBoneHitboxes();
    }

    @Override
    public void setId(int id) {
        super.setId(id);
        if (boneParts != null) {
            for (int index = 0; index < boneParts.length; index++) {
                boneParts[index].setId(id + index + 1);
            }
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 180.0D)
                .add(Attributes.ARMOR, 6.0D)
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.18D)
                .add(Attributes.FOLLOW_RANGE, 20.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.15D);
    }

    public static boolean checkAzureSpawnRules(
            EntityType<AzureSpiritSilkwormEntity> type,
            ServerLevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random) {
        if (!Mob.checkMobSpawnRules(type, level, spawnType, pos, random)) {
            return false;
        }
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = -9; x <= 9; x++) {
            for (int z = -9; z <= 9; z++) {
                for (int y = -2; y <= 3; y++) {
                    cursor.set(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
                    var state = level.getBlockState(cursor);
                    if (state.is(ModBlocks.LINGMU.get())
                            || state.is(Blocks.BAMBOO)
                            || state.is(BlockTags.LEAVES)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 1.15D));
        goalSelector.addGoal(2, new SeekLingmuGoal());
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.65D));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 7.0F));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(COCOONED, false);
        builder.define(SPIT_TICKS, 0);
    }

    @Override
    public void tick() {
        super.tick();
        updateBoneHitboxes();
        if (level().isClientSide) {
            if (tickCount % 12 == 0 && random.nextBoolean()) {
                level().addParticle(ParticleTypes.END_ROD,
                        getX() + (random.nextDouble() - 0.5D) * 0.5D,
                        getY() + 0.2D + random.nextDouble() * 0.25D,
                        getZ() + (random.nextDouble() - 0.5D) * 0.5D,
                        0.0D, 0.005D, 0.0D);
            }
            return;
        }
        if (BeastDazeControl.blocksMoves(this)) {
            entityData.set(SPIT_TICKS, 0);
            cocoonTicks = 0;
            entityData.set(COCOONED, false);
            return;
        }

        if (spitCooldown > 0) {
            spitCooldown--;
        }
        if (cocoonCooldown > 0) {
            cocoonCooldown--;
        }
        int spitTicks = entityData.get(SPIT_TICKS);
        if (spitTicks > 0) {
            entityData.set(SPIT_TICKS, spitTicks - 1);
        }
        if (cocoonTicks > 0) {
            cocoonTicks--;
            entityData.set(COCOONED, true);
            navigation.stop();
            Vec3 movement = getDeltaMovement();
            setDeltaMovement(0.0D, movement.y, 0.0D);
            if (cocoonTicks == 0) {
                entityData.set(COCOONED, false);
            }
        }
    }

    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Override
    public PartEntity<?>[] getParts() {
        return boneParts;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    private void updateBoneHitboxes() {
        BoneHitboxAnimation.Clip animation = currentHitboxAnimation();
        float time = animation.normalizedTime(currentHitboxAnimationTime());
        ModelBoneHitboxPose entityRoot = ModelBoneHitboxPose.root(
                position(), getYRot(), 0.0F, MODEL_SCALE);
        ModelBoneHitboxPose root = entityRoot.animatedChild(
                animation.motion("root"), time,
                0.0D, 0.0D, 0.0D, 0.0F, 0.0F, 0.0F);
        ModelBoneHitboxPose bodyMid = root.animatedChild(
                animation.motion("body_mid"), time,
                0.0D, 5.0D, 0.0D, 0.0F, 0.0F, 0.0F);
        ModelBoneHitboxPose bodyFront = bodyMid.animatedChild(
                animation.motion("body_front"), time,
                0.0D, 0.0D, -2.5D, 0.0F, 0.0F, 0.0F);
        ModelBoneHitboxPose head = bodyFront.animatedChild(
                animation.motion("head"), time,
                0.0D, 0.0D, -4.0D, 0.0F, 0.0F, 0.0F);
        ModelBoneHitboxPose bodyBack = bodyMid.animatedChild(
                animation.motion("body_back"), time,
                0.0D, 0.0D, 2.5D, 0.0F, 0.0F, 0.0F);
        ModelBoneHitboxPose tail = bodyBack.animatedChild(
                animation.motion("tail"), time,
                0.0D, 0.0D, 4.0D, 0.0F, 0.0F, 0.0F);

        bodyMidPart.moveCenterTo(bodyMid.point(0.0D, -0.5D, 0.0D));
        bodyFrontPart.moveCenterTo(bodyFront.point(0.0D, -0.5D, -2.25D));
        headPart.moveCenterTo(head.point(0.0D, -0.3D, -1.75D));
        bodyBackPart.moveCenterTo(bodyBack.point(0.0D, -0.5D, 2.25D));
        tailPart.moveCenterTo(tail.point(0.0D, -0.25D, 1.75D));
    }

    private BoneHitboxAnimation.Clip currentHitboxAnimation() {
        if (isCocooned()) {
            return COCOON_HITBOX_ANIMATION;
        }
        if (getSpitTicks() > 0) {
            return SPIT_HITBOX_ANIMATION;
        }
        return getDeltaMovement().horizontalDistanceSqr() > 0.001D
                ? CRAWL_HITBOX_ANIMATION
                : IDLE_HITBOX_ANIMATION;
    }

    private float currentHitboxAnimationTime() {
        if (getSpitTicks() > 0) {
            return (SPIT_ANIMATION_TICKS - getSpitTicks()) / 20.0F;
        }
        return tickCount / 20.0F;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            long gameTime = level().getGameTime();
            if (lastExplosionDamageSource == source && lastExplosionDamageTick == gameTime) {
                return false;
            }
            lastExplosionDamageSource = source;
            lastExplosionDamageTick = gameTime;
        }
        if (isCocooned()) {
            amount *= 0.25F;
        }
        boolean hurt = super.hurt(source, amount);
        if (!hurt || level().isClientSide || !isAlive()) {
            return hurt;
        }
        if (BeastDazeControl.blocksMoves(this)) {
            return hurt;
        }
        if (source.getEntity() instanceof LivingEntity attacker && attacker != this) {
            if (spitCooldown <= 0) {
                spitAt(attacker);
            }
            if (cocoonCooldown <= 0 && random.nextFloat() < 0.25F) {
                startCocoon();
            }
        }
        return true;
    }

    private void spitAt(LivingEntity attacker) {
        if (BeastDazeControl.blocksMoves(this)) {
            return;
        }
        SpiritSilkProjectileEntity projectile =
                new SpiritSilkProjectileEntity(level(), this);
        projectile.setPos(getX(), getEyeY() - 0.1D, getZ());
        double dx = attacker.getX() - getX();
        double dz = attacker.getZ() - getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        double dy = attacker.getY() + attacker.getBbHeight() * 0.55D
                - projectile.getY() + horizontal * 0.12D;
        projectile.shoot(dx, dy, dz, 1.15F, 3.0F);
        level().addFreshEntity(projectile);
        entityData.set(SPIT_TICKS, SPIT_ANIMATION_TICKS);
        spitCooldown = 70;
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ENCHANT,
                    getX(), getEyeY(), getZ(),
                    12, 0.2D, 0.15D, 0.2D, 0.05D);
        }
    }

    private void startCocoon() {
        if (BeastDazeControl.blocksMoves(this)) {
            return;
        }
        cocoonTicks = 20 * 5;
        cocoonCooldown = 20 * 25;
        entityData.set(COCOONED, true);
    }

    public boolean isCocooned() {
        return entityData.get(COCOONED);
    }

    public int getSpitTicks() {
        return entityData.get(SPIT_TICKS);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ModBlocks.LINGMU.get().asItem());
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return ModEntities.AZURE_SPIRIT_SILKWORM.get().create(level);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 4, state -> {
            if (isCocooned()) {
                return state.setAndContinue(COCOON);
            }
            if (getSpitTicks() > 0) {
                return state.setAndContinue(SPIT);
            }
            if (state.isMoving()) {
                return state.setAndContinue(CRAWL);
            }
            return state.setAndContinue(IDLE);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    private final class SeekLingmuGoal extends Goal {
        private BlockPos target;
        private int nextSearch;

        private SeekLingmuGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (isCocooned() || nextSearch-- > 0) {
                return false;
            }
            nextSearch = 60 + random.nextInt(80);
            target = findLingmu();
            return target != null;
        }

        @Override
        public boolean canContinueToUse() {
            return !isCocooned() && target != null
                    && target.distToCenterSqr(getX(), getY(), getZ()) > 2.0D
                    && !navigation.isDone();
        }

        @Override
        public void start() {
            if (target != null) {
                navigation.moveTo(target.getX() + 0.5D, target.getY(),
                        target.getZ() + 0.5D, 0.72D);
            }
        }

        @Override
        public void stop() {
            target = null;
        }

        private BlockPos findLingmu() {
            BlockPos origin = blockPosition();
            BlockPos best = null;
            double bestDistance = Double.MAX_VALUE;
            for (BlockPos candidate : BlockPos.betweenClosed(
                    origin.offset(-10, -3, -10), origin.offset(10, 3, 10))) {
                if (!level().getBlockState(candidate).is(ModBlocks.LINGMU.get())) {
                    continue;
                }
                double distance = candidate.distSqr(origin);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = candidate.immutable();
                }
            }
            return best;
        }
    }
}
