package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.item.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** The vulnerable beast spirit carries the dragon's rewards until defeated or captured. */
public final class EscapedYuanyingCoreEntity extends PathfinderMob {
    @Override public void die(DamageSource source) {
        if (dead || isRemoved() || resolved) return;
        if (!ownsEscape()) { discard(); return; }
        super.die(source);
        if (dead) resolveEscape();
    }

    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> CORE_REALM =
            net.minecraft.network.syncher.SynchedEntityData.defineId(EscapedYuanyingCoreEntity.class,
                    net.minecraft.network.syncher.EntityDataSerializers.INT);

    @Override protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CORE_REALM, 4);
    }
    public int coreRealm() { return entityData.get(CORE_REALM); }
    @Override public net.minecraft.network.chat.Component getName() {
        return BeastRealms.prefix(coreRealm(), super.getName());
    }
    public static final int ESCAPE_TICKS = 20 * 10;
    public static final float CORE_HEALTH = 18_000.0F * 0.20F;

    private int revivalTicks = ESCAPE_TICKS;
    private boolean resolved;
    private java.util.UUID originalDragon;

    public java.util.UUID originalDragon() { return originalDragon; }
    private double revivalX;
    private double revivalY;
    private double revivalZ;
    private float revivalYaw;

    public EscapedYuanyingCoreEntity(
            EntityType<? extends EscapedYuanyingCoreEntity> entityType, Level level) {
        super(entityType, level);
        setNoGravity(true);
        setPersistenceRequired();
        xpReward = 0;
    }

    public EscapedYuanyingCoreEntity(
            ServerLevel level, CrimsonSkyFlameFloodDragonEntity dragon) {
        this(ModEntities.ESCAPED_YUANYING_CORE.get(), level);
        setPos(dragon.getX(), dragon.getY() + dragon.getBbHeight() * 0.7D, dragon.getZ());
        entityData.set(CORE_REALM, dragon.beastRealm());
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(dragon.getMaxHealth() * .2);
        setHealth(getMaxHealth());
        originalDragon = dragon.getUUID();
        revivalX = dragon.getX();
        revivalY = dragon.getY();
        revivalZ = dragon.getZ();
        revivalYaw = dragon.getYRot();
        setDeltaMovement((random.nextDouble() - 0.5D) * 0.22D,
                0.16D, (random.nextDouble() - 0.5D) * 0.22D);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, CORE_HEALTH)
                .add(Attributes.ARMOR, 0.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.25D);
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public void tick() {
        if (isRemoved()) return;
        if (!level().isClientSide && (resolved || !ownsEscape())) { discard(); return; }
        super.tick();
        setNoGravity(true);
        if (level().isClientSide || !isAlive()) {
            return;
        }

        Vec3 velocity = getDeltaMovement();
        double swirl = tickCount * 0.17D;
        velocity = velocity.scale(0.97D).add(
                Math.cos(swirl) * 0.006D,
                tickCount < 35 ? 0.004D : Math.sin(swirl * 0.5D) * 0.002D,
                Math.sin(swirl) * 0.006D);
        if (velocity.lengthSqr() > 0.08D) {
            velocity = velocity.normalize().scale(Math.sqrt(0.08D));
        }
        setDeltaMovement(velocity);
        move(MoverType.SELF, velocity);

        if (level() instanceof ServerLevel serverLevel) {
            emitParticleSphere(serverLevel);
            revivalTicks--;
            if (revivalTicks <= 0) {
                reviveDragon(serverLevel);
            }
        }
    }

    private void emitParticleSphere(ServerLevel level) {
        double centerY = getY() + getBbHeight() * 0.5D;
        double phase = tickCount * 0.24D;
        for (int index = 0; index < 18; index++) {
            double latitude = Math.asin(-1.0D + 2.0D * (index + 0.5D) / 18.0D);
            double longitude = phase + index * 2.399963229728653D;
            double horizontal = Math.cos(latitude) * 0.68D;
            double x = getX() + Math.cos(longitude) * horizontal;
            double y = centerY + Math.sin(latitude) * 0.68D;
            double z = getZ() + Math.sin(longitude) * horizontal;
            level.sendParticles(index % 6 == 0 ? ParticleTypes.LAVA : ParticleTypes.FLAME,
                    x, y, z, 1, 0.01D, 0.01D, 0.01D, 0.0D);
        }
        level.sendParticles(ParticleTypes.END_ROD, getX(), centerY, getZ(),
                3, 0.18D, 0.18D, 0.18D, 0.01D);
    }

    private void reviveDragon(ServerLevel level) {
        if (resolved || isRemoved() || !ownsEscape()) { discard(); return; }
        var original = originalDragon == null ? null : level.getEntity(originalDragon);
        if (original != null) {
            if (original.isAlive()) {
                // Never create another body beside a surviving/recovered original.
                resolveEscape();
                discard();
            } else revivalTicks = 20; // Allow the original corpse to finish its death animation.
            return;
        }
        CrimsonSkyFlameFloodDragonEntity dragon =
                ModEntities.CRIMSON_SKY_FLAME_FLOOD_DRAGON.get().create(level);
        if (dragon != null) {
            if (originalDragon != null) dragon.setUUID(originalDragon);
            dragon.moveTo(revivalX, revivalY, revivalZ, revivalYaw, 0.0F);
            dragon.setBeastRealm(coreRealm());
            dragon.setHealth(dragon.getMaxHealth() * 0.20F);
            dragon.markRevivedFromCore();
            dragon.setTarget(level.getNearestPlayer(dragon, 48.0D));
            // Claim before spawning, so callbacks and duplicate saved spirits cannot revive twice.
            var ownership = DragonSpiritData.get(level);
            if (!ownership.finish(escapeSource(), getUUID())) { discard(); return; }
            if (!level.addFreshEntity(dragon)) {
                ownership.begin(escapeSource(), getUUID());
                revivalTicks = 20;
                return;
            }
            resolved = true;
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    revivalX, revivalY + 1.0D, revivalZ,
                    2, 0.0D, 0.0D, 0.0D, 0.0D);
            level.sendParticles(ParticleTypes.FLAME,
                    revivalX, revivalY + 1.0D, revivalZ,
                    120, 2.0D, 1.5D, 2.0D, 0.12D);
            level.playSound(null, dragon.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL,
                    SoundSource.HOSTILE, 3.0F, 0.6F);
        }
        discard();
    }

    private java.util.UUID escapeSource() { return originalDragon == null ? getUUID() : originalDragon; }

    private boolean ownsEscape() {
        return !(level() instanceof ServerLevel serverLevel)
                || DragonSpiritData.get(serverLevel).owns(escapeSource(), getUUID());
    }

    private void resolveEscape() {
        resolved = true;
        if (level() instanceof ServerLevel serverLevel)
            DragonSpiritData.get(serverLevel).finish(escapeSource(), getUUID());
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!isAlive() || isRemoved() || resolved) return InteractionResult.PASS;
        if (!ownsEscape()) { discard(); return InteractionResult.PASS; }
        ItemStack held = player.getItemInHand(hand);
        boolean glassBottle = held.is(Items.GLASS_BOTTLE);
        boolean storageCrystal = held.is(
                com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_STORAGE_CRYSTAL.get());
        if (!glassBottle && !storageCrystal) {
            return InteractionResult.PASS;
        }
        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        var drops = new java.util.ArrayList<net.minecraft.world.entity.item.ItemEntity>();
        captureDrops(drops);
        try {
            lastHurtByPlayer = player;
            if (level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOMOBLOOT))
                dropFromLootTable(damageSources().playerAttack(player), true);
            else spawnAtLocation(BeastRealmEvents.realmDrop(new ItemStack(ModItems.YUANYING_YAODAN.get()), coreRealm()));
        } finally { captureDrops(null); }
        if (net.neoforged.neoforge.common.CommonHooks.onLivingDrops(this,
                damageSources().playerAttack(player), drops, true)) return InteractionResult.FAIL;
        resolveEscape();
        if (!player.getAbilities().instabuild) held.shrink(1);
        for (var drop : drops) {
            ItemStack stack = drop.getItem();
            if (BeastRealmEvents.isCore(stack)) {
                if (!player.addItem(stack)) player.drop(stack, false);
            } else level().addFreshEntity(drop);
        }
        com.example.immortal_cultivation_mod.flame.FlameAcquisition.onDragonSpiritCaptured(this);
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    getX(), getY() + getBbHeight() * 0.5D, getZ(),
                    48, 0.65D, 0.65D, 0.65D, 0.08D);
            serverLevel.playSound(null, blockPosition(), SoundEvents.BOTTLE_FILL_DRAGONBREATH,
                    SoundSource.PLAYERS, 1.0F, 0.8F);
        }
        discard();
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(net.minecraft.world.entity.Entity entity) {
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("RevivalTicks", revivalTicks);
        tag.putBoolean("Resolved", resolved);
        tag.putInt(BeastRealms.NBT_KEY, coreRealm());
        if (originalDragon != null) tag.putUUID("OriginalDragon", originalDragon);
        tag.putDouble("RevivalX", revivalX);
        tag.putDouble("RevivalY", revivalY);
        tag.putDouble("RevivalZ", revivalZ);
        tag.putFloat("RevivalYaw", revivalYaw);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(CORE_REALM, tag.contains(BeastRealms.NBT_KEY)
                ? BeastRealms.clamp(ModEntities.CRIMSON_SKY_FLAME_FLOOD_DRAGON.get(), tag.getInt(BeastRealms.NBT_KEY)) : 4);
        resolved = tag.getBoolean("Resolved") || tag.getBoolean("LootClaimed");
        revivalTicks = Math.max(1, tag.getInt("RevivalTicks"));
        originalDragon = tag.hasUUID("OriginalDragon") ? tag.getUUID("OriginalDragon") : null;
        revivalX = tag.getDouble("RevivalX");
        revivalY = tag.getDouble("RevivalY");
        revivalZ = tag.getDouble("RevivalZ");
        revivalYaw = tag.getFloat("RevivalYaw");
        setNoGravity(true);
        setPersistenceRequired();
    }
}
