package com.example.immortal_cultivation_mod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SuokongBarrierBlock extends Block {
    public static final int LIFETIME_TICKS = 20 * 20;
    private static final int DAMAGE_INTERVAL_TICKS = 3 * 20;
    private static final String NEXT_DAMAGE_TICK = "icmSuokongNextDamageTick";

    public SuokongBarrierBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                           CollisionContext context) {
        if (context instanceof EntityCollisionContext entityContext
                && entityContext.getEntity() instanceof Projectile) {
            return Shapes.empty();
        }
        return Shapes.block();
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && !level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, LIFETIME_TICKS);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockState(pos).is(this)) {
            level.removeBlock(pos, false);
        }
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || entity instanceof Projectile || !entity.isAlive()) {
            return;
        }

        long gameTime = level.getGameTime();
        var tag = entity.getPersistentData();
        if (!tag.contains(NEXT_DAMAGE_TICK) || tag.getLong(NEXT_DAMAGE_TICK) <= gameTime) {
            tag.putLong(NEXT_DAMAGE_TICK, gameTime + DAMAGE_INTERVAL_TICKS);
            if (entity instanceof LivingEntity living) {
                living.hurt(living.damageSources().inWall(), 100.0F);
            }
        }
    }
}
