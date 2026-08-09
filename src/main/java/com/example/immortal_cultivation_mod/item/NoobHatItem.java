package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.achievement.NoobAchievement;
import java.util.function.Consumer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.util.GeckoLibUtil;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public class NoobHatItem extends Item implements GeoItem, ICurioItem {
    private static final RawAnimation IDLE =
            RawAnimation.begin().thenLoop("animation.noob_hat.idle");
    private static final RawAnimation RUN =
            RawAnimation.begin().thenLoop("animation.noob_hat.run");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public NoobHatItem(Properties properties) {
        super(properties);
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private NoobHatItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (renderer == null) {
                    renderer = new NoobHatItemRenderer();
                }
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 0,
                state -> state.setAndContinue(isWearerMoving(state) ? RUN : IDLE)));
    }

    private static boolean isWearerMoving(AnimationState<NoobHatItem> state) {
        if (!(state.getData(DataTickets.ENTITY) instanceof LivingEntity wearer)) {
            return false;
        }
        double movedX = wearer.getX() - wearer.xOld;
        double movedZ = wearer.getZ() - wearer.zOld;
        return movedX * movedX + movedZ * movedZ > 1.0E-5D;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return NoobAchievement.HEAD_SLOT.equals(slotContext.identifier())
                && slotContext.entity() instanceof Player player
                && NoobAchievement.hasReward(player);
    }

    @Override
    public boolean canUnequip(SlotContext slotContext, ItemStack stack) {
        return false;
    }

    @Override
    public ICurio.DropRule getDropRule(
            SlotContext slotContext,
            DamageSource source,
            boolean recentlyHit,
            ItemStack stack) {
        return ICurio.DropRule.ALWAYS_KEEP;
    }
}
