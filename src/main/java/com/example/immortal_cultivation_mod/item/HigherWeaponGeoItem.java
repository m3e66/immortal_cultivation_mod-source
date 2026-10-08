package com.example.immortal_cultivation_mod.item;

import java.util.function.Consumer;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class HigherWeaponGeoItem extends HigherWeaponItem implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public HigherWeaponGeoItem(HigherWeaponSpec spec, Properties properties) {
        super(spec, properties);
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private HigherWeaponItemRenderer renderer;
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (renderer == null) renderer = new HigherWeaponItemRenderer();
                return renderer;
            }
        });
    }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 3, state -> {
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation." + spec.id + ".idle"));
        }).triggerableAnim("attack", RawAnimation.begin().thenPlay("animation." + spec.id + ".attack")));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
