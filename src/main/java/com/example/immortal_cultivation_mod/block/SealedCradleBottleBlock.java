package com.example.immortal_cultivation_mod.block;

import com.example.immortal_cultivation_mod.curse.CurseOutbreaks;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Every destruction route breaks the seal, including explosions; no item can be recovered. */
public final class SealedCradleBottleBlock extends Block {
    public static final MapCodec<SealedCradleBottleBlock> CODEC=simpleCodec(SealedCradleBottleBlock::new);
    private static final VoxelShape SHAPE=Block.box(2,0,2,14,16,14);
    public SealedCradleBottleBlock(Properties properties){super(properties);}
    @Override protected MapCodec<? extends Block> codec(){return CODEC;}
    @Override protected VoxelShape getShape(BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos,CollisionContext context){return SHAPE;}
    @Override protected List<ItemStack> getDrops(BlockState state,LootParams.Builder params){return List.of();}
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving) {
        if(!state.is(next.getBlock())&&level instanceof ServerLevel server)CurseOutbreaks.release(server,pos);
        super.onRemove(state,level,pos,next,moving);
    }
}
