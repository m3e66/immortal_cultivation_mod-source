package com.example.immortal_cultivation_mod.block;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class GeoRockBlockEntity extends BlockEntity implements GeoBlockEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public GeoRockBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.GEO_ROCK.get(), pos, blockState);
    }

    public String modelName() {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(getBlockState().getBlock());
        if (ImmortalCultivationMod.MODID.equals(key.getNamespace())) {
            return key.getPath();
        }
        return "jingtie_motherstone";
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public double getTick(Object object) {
        return level == null ? 0.0D : level.getGameTime();
    }
}
