package com.example.immortal_cultivation_mod.item;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

public class GeoRockBlockItem extends BlockItem implements GeoItem {
    private static final RawAnimation QINGTONG_DING_IDLE =
            RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation CU_TIE_DING_IDLE =
            RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation RESTRICTION_TABLE_IDLE =
            RawAnimation.begin().thenLoop("animation.restriction_table.idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public GeoRockBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    public String modelName() {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(getBlock());
        return key.getPath();
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private GeoRockBlockItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (renderer == null) {
                    renderer = new GeoRockBlockItemRenderer();
                }
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(
                this,
                "qingtong_ding_item",
                0,
                state -> {
                    String model = modelName();
                    if ("qingtong_yinhuo_ding".equals(model) || "chi_jin_sanyang_ding".equals(model)) {
                        return state.setAndContinue(QINGTONG_DING_IDLE);
                    }
                    if ("cu_tie_xiaoding".equals(model)) {
                        return state.setAndContinue(CU_TIE_DING_IDLE);
                    }
                    if ("restriction_table".equals(model)) {
                        return state.setAndContinue(RESTRICTION_TABLE_IDLE);
                    }
                    return PlayState.STOP;
                }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
