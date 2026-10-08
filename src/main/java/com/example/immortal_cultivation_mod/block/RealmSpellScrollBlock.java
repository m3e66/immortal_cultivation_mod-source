package com.example.immortal_cultivation_mod.block;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A one-use spell cache. Opening or breaking it releases one random spell
 * scroll whose cultivation requirement belongs to this block's realm.
 */
public class RealmSpellScrollBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    /**
     * Exact collision outline of realm_spell_scroll_block.geo.json after the
     * Model Kit X-axis conversion used by GeoRockBlockRenderer. The model dips
     * 0.45 pixels below its origin, so the collision is clamped to the floor.
     */
    private static final VoxelShape NORTH_SHAPE = Shapes.or(
            Block.box(9.6D, 0.0D, 2.25D, 12.6D, 2.55D, 13.75D),
            Block.box(7.5D, 2.55D, 2.25D, 10.5D, 5.55D, 13.75D),
            Block.box(5.4D, 0.0D, 2.25D, 8.4D, 2.55D, 13.75D)
    ).optimize();
    private static final VoxelShape EAST_SHAPE = rotateHorizontal(NORTH_SHAPE, Direction.EAST);
    private static final VoxelShape SOUTH_SHAPE = rotateHorizontal(NORTH_SHAPE, Direction.SOUTH);
    private static final VoxelShape WEST_SHAPE = rotateHorizontal(NORTH_SHAPE, Direction.WEST);

    private final Realm realm;

    public RealmSpellScrollBlock(Realm realm, Properties properties) {
        super(properties);
        this.realm = realm;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GeoRockBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                  CollisionContext context) {
        return shapeFor(state.getValue(FACING));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                           CollisionContext context) {
        return shapeFor(state.getValue(FACING));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        return open(level, pos, player);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                              BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hitResult) {
        open(level, pos, player);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private InteractionResult open(Level level, BlockPos pos, Player player) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        ItemStack reward = randomReward(level);
        if (!reward.isEmpty()) {
            popResource(level, pos, reward);
            level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS,
                    0.9F, 0.9F + level.getRandom().nextFloat() * 0.2F);
        }
        level.destroyBlock(pos, false, player);
        return InteractionResult.CONSUME;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        ItemStack reward = randomReward(params.getLevel());
        return reward.isEmpty() ? List.of() : List.of(reward);
    }

    private ItemStack randomReward(Level level) {
        List<Item> candidates = BuiltInRegistries.ITEM.stream()
                .filter(item -> item instanceof ModItems.SpellScrollItem scroll
                        && realm.matches(scroll.spellId()))
                .toList();
        if (candidates.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return candidates.get(level.getRandom().nextInt(candidates.size())).getDefaultInstance();
    }

    private static VoxelShape shapeFor(Direction facing) {
        return switch (facing) {
            case EAST -> EAST_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            default -> NORTH_SHAPE;
        };
    }

    private static VoxelShape rotateHorizontal(VoxelShape source, Direction facing) {
        VoxelShape[] rotated = {Shapes.empty()};
        source.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
            VoxelShape box = switch (facing) {
                // Match GeoRockBlockRenderer: east is 270 degrees, west is 90.
                case EAST -> Shapes.box(1.0D - maxZ, minY, minX,
                        1.0D - minZ, maxY, maxX);
                case SOUTH -> Shapes.box(1.0D - maxX, minY, 1.0D - maxZ,
                        1.0D - minX, maxY, 1.0D - minZ);
                case WEST -> Shapes.box(minZ, minY, 1.0D - maxX,
                        maxZ, maxY, 1.0D - minX);
                default -> Shapes.box(minX, minY, minZ, maxX, maxY, maxZ);
            };
            rotated[0] = Shapes.or(rotated[0], box);
        });
        return rotated[0].optimize();
    }

    public enum Realm {
        LIANQI(CultivationLevels.REALM_LIANQI),
        ZHUJI(CultivationLevels.REALM_ZHUJI),
        JINDAN(CultivationLevels.REALM_JINDAN),
        YUANYING(CultivationLevels.REALM_YUANYING),
        HUASHEN(CultivationLevels.REALM_HUASHEN),
        LIANXU(CultivationLevels.REALM_LIANXU),
        HETI(CultivationLevels.REALM_HETI),
        DACHENG(CultivationLevels.REALM_DACHENG),
        DUJIE(CultivationLevels.REALM_DUJIE);

        private final String realmName;

        Realm(String realmName) {
            this.realmName = realmName;
        }

        private boolean matches(String spellId) {
            ModSpells.SpellDef spell = ModSpells.get(spellId);
            return spell != null && spell.requiredLevel().startsWith(realmName);
        }
    }
}
