package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.alchemy.AlchemyHerbBlock;
import com.example.immortal_cultivation_mod.block.CultivationPlantBlock;
import com.example.immortal_cultivation_mod.particle.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.client.ParticleStatus;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Sparse local atmosphere: no chunk loads, packets, block edits or persistent world references. */
@EventBusSubscriber(modid = ImmortalCultivationMod.MODID, value = Dist.CLIENT)
public final class EnvironmentalAtmosphere {
    private EnvironmentalAtmosphere() {}
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        var level = mc.level;
        if (level == null || mc.player == null || mc.isPaused()
                || mc.options.particles().get() == ParticleStatus.MINIMAL || (level.getGameTime() & 1) != 0) return;
        var random = level.random;
        BlockPos center = mc.gameRenderer.getMainCamera().getBlockPosition();
        int budget = mc.options.particles().get() == ParticleStatus.DECREASED ? 1 : 3;
        for (int sample = 0; sample < 24 && budget > 0; sample++) {
            BlockPos pos = center.offset(random.nextInt(17)-8, random.nextInt(11)-6, random.nextInt(17)-8);
            if (!level.hasChunkAt(pos) || !level.isEmptyBlock(pos.above())) continue;
            var state = level.getBlockState(pos);
            SimpleParticleType type;
            double lift = .008;
            if (state.getBlock() instanceof AlchemyHerbBlock || state.getBlock() instanceof CultivationPlantBlock) {
                type = ModParticles.AMBIENT_SPIRIT.get();
            } else if (state.is(BlockTags.LEAVES) || state.is(BlockTags.FLOWERS)) {
                type = ModParticles.AMBIENT_LEAF.get(); lift = -.006;
            } else if (state.is(BlockTags.ICE) || state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK)) {
                type = ModParticles.AMBIENT_FROST.get();
            } else if (state.is(BlockTags.FIRE) || state.getFluidState().is(FluidTags.LAVA)
                    || state.getBlock() instanceof CampfireBlock && state.getValue(CampfireBlock.LIT)) {
                type = ModParticles.AMBIENT_EMBER.get(); lift = .026;
            } else continue;
            budget--;
            level.addParticle(type, pos.getX()+random.nextDouble(), pos.getY()+1.03,
                    pos.getZ()+random.nextDouble(), (random.nextDouble()-.5)*.014, lift,
                    (random.nextDouble()-.5)*.014);
        }
    }
}
