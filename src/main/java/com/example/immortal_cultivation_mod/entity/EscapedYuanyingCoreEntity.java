package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.item.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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

/** The vulnerable Yuanying that escapes when the dragon's body dies. */
public final class EscapedYuanyingCoreEntity extends PathfinderMob {
    public static final int ESCAPE_TICKS = 20 * 10;
    public static final float CORE_HEALTH = 18_000.0F * 0.20F;

    private int revivalTicks = ESCAPE_TICKS;
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
        setHealth(CORE_HEALTH);
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
        CrimsonSkyFlameFloodDragonEntity dragon =
                ModEntities.CRIMSON_SKY_FLAME_FLOOD_DRAGON.get().create(level);
        if (dragon != null) {
            dragon.moveTo(revivalX, revivalY, revivalZ, revivalYaw, 0.0F);
            dragon.setHealth(dragon.getMaxHealth() * 0.20F);
            dragon.markRevivedFromCore();
            dragon.setTarget(level.getNearestPlayer(dragon, 48.0D));
            level.addFreshEntity(dragon);
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

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
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

        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        ItemStack captured = new ItemStack(ModItems.YUANYING_YAODAN.get());
        if (!player.addItem(captured)) {
            player.drop(captured, false);
        }
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
        if (originalDragon != null) tag.putUUID("OriginalDragon", originalDragon);
        tag.putDouble("RevivalX", revivalX);
        tag.putDouble("RevivalY", revivalY);
        tag.putDouble("RevivalZ", revivalZ);
        tag.putFloat("RevivalYaw", revivalYaw);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
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
