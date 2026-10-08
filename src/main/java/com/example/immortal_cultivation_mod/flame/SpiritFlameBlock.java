package com.example.immortal_cultivation_mod.flame;

import com.example.immortal_cultivation_mod.block.LingFireBlock;
import com.example.immortal_cultivation_mod.block.ModBlockEntities;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** A non-spreading, short-lived forge fire requiring solid support. */
public final class SpiritFlameBlock extends LingFireBlock implements EntityBlock {
    private final FlameKind kind;

    public SpiritFlameBlock(FlameKind kind, Properties properties) {
        super(properties);
        if (!kind.special()) throw new IllegalArgumentException("Mortal fire uses vanilla blocks");
        this.kind = kind;
    }

    public FlameKind kind() { return kind; }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SpiritFlameBlockEntity(pos, state);
    }

    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != ModBlockEntities.SPIRIT_FLAME.get()) return null;
        return (world, position, current, entity) -> {
            if (entity instanceof SpiritFlameBlockEntity flame) {
                SpiritFlameBlockEntity.serverTick(world, position, current, flame);
            }
        };
    }

    @Override protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // The animated renderer schedules bounded kind-specific accents on the client.
    }

    @Override protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!(level instanceof ServerLevel server) || !(entity instanceof LivingEntity living)
                || !living.isAlive() || living.isRemoved() || living.isSpectator() || living.fireImmune()
                || living instanceof Player player && player.isCreative()) return;
        var source = server.damageSources().inFire();
        if (living.isInvulnerableTo(source)) return;
        if (!SpiritFlameBurnEffect.ready(living)) {
            if (living.isOnFire() && SpiritFlameBurnEffect.kind(living) != null)
                SpiritFlameBurnEffect.refresh(living, kind);
            return;
        }
        if (living.hurt(source, kind.contactDamagePerSecond()))
            SpiritFlameBurnEffect.ignite(living, kind);
    }

    @Override protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                               LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return state.canSurvive(level, pos)
                ? super.updateShape(state, direction, neighborState, level, pos, neighborPos)
                : Blocks.AIR.defaultBlockState();
    }
}
