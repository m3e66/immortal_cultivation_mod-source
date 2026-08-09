package com.example.immortal_cultivation_mod.block;

import com.example.immortal_cultivation_mod.spell.UndeadControl;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class StarMarkBlock extends CarpetBlock {
    private static final int AURA_INTERVAL_TICKS = 20;
    private static final double AURA_RADIUS = 5.0D;
    private static final String LAST_SUPPRESSION_TICK = "icmStarMarkSuppressionTick";
    private static final DustParticleOptions GOLD_DUST =
            new DustParticleOptions(new Vector3f(1.0F, 0.68F, 0.08F), 1.0F);

    public StarMarkBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && !oldState.is(state.getBlock())
                && !level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, AURA_INTERVAL_TICKS);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        pulseAura(level, pos);
        level.scheduleTick(pos, this, AURA_INTERVAL_TICKS);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, 1);
        }
    }

    private void pulseAura(ServerLevel level, BlockPos pos) {
        Vec3 center = Vec3.atCenterOf(pos).add(0.0D, 0.15D, 0.0D);
        AABB area = new AABB(pos).inflate(AURA_RADIUS);
        long gameTime = level.getGameTime();
        boolean foundTarget = false;
        DamageSource auraSource = new DamageSource(
                level.damageSources().magic().typeHolder(), center);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area,
                target -> target.isAlive() && isEvilSpirit(target)
                        && target.position().distanceToSqr(center) <= AURA_RADIUS * AURA_RADIUS)) {
            long lastTick = living.getPersistentData().getLong(LAST_SUPPRESSION_TICK);
            if (gameTime - lastTick >= AURA_INTERVAL_TICKS) {
                living.getPersistentData().putLong(LAST_SUPPRESSION_TICK, gameTime);
                if (!living.hurt(auraSource, 10.0F)) {
                    continue;
                }
                foundTarget = true;
                living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 45, 1, false, true, true));
                living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 45, 0, false, false, true));
                level.sendParticles(GOLD_DUST, living.getX(), living.getY() + living.getBbHeight() * 0.5D,
                        living.getZ(), 18, 0.32D, 0.45D, 0.32D, 0.025D);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, living.getX(), living.getY() + 0.25D,
                        living.getZ(), 12, 0.35D, 0.2D, 0.35D, 0.08D);
            }
        }
        if (foundTarget) {
            level.sendParticles(GOLD_DUST, center.x, center.y, center.z,
                    28, 1.2D, 0.12D, 1.2D, 0.025D);
            level.sendParticles(ParticleTypes.END_ROD, center.x, center.y + 0.2D, center.z,
                    16, 0.8D, 0.3D, 0.8D, 0.045D);
        }
    }

    private static boolean isEvilSpirit(LivingEntity living) {
        return living.getType().is(EntityTypeTags.UNDEAD) || UndeadControl.isUndeadServantType(living);
    }
}
