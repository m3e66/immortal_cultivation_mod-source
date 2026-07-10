package com.example.immortal_cultivation_mod.block;

import com.example.immortal_cultivation_mod.effect.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

public class LingFireBlock extends Block {
    public LingFireBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        for (int i = 0; i < 2; i++) {
            level.addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                    pos.getX() + 0.25D + random.nextDouble() * 0.5D,
                    pos.getY() + 0.1D + random.nextDouble() * 0.7D,
                    pos.getZ() + 0.25D + random.nextDouble() * 0.5D,
                    0.0D, 0.02D, 0.0D);
        }
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        entity.igniteForSeconds(4.0F);
        if (!level.isClientSide && entity instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(ModEffects.QI_FIRE_BURN, 90, 0, false, false, false));
        }
        super.entityInside(state, level, pos, entity);
    }
}
