package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

public class JiuhuItem extends Item implements GeoItem {
    private static final int QI_COST = 100;
    private static final int DURATION = 20 * 60 * 1;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public JiuhuItem(Properties properties) {
        super(properties);
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private JiuhuItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (renderer == null) {
                    renderer = new JiuhuItemRenderer();
                }
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            var data = ModAttachments.getData(player);

            if (data.qi() < QI_COST) {
                player.sendSystemMessage(Component.translatable(
                        "message." + ImmortalCultivationMod.MODID + ".not_enough_qi"
                ));
                return InteractionResultHolder.fail(stack);
            }
        }

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && entity instanceof ServerPlayer sp) {
            var data = ModAttachments.getData(sp);

            if (data.qi() < QI_COST) {
                sp.sendSystemMessage(Component.translatable(
                        "message." + ImmortalCultivationMod.MODID + ".not_enough_qi"
                ));
                return stack;
            }

            ModAttachments.setData(sp, data.withQi(data.qi() - QI_COST));

            sp.addEffect(new MobEffectInstance(
                    ModEffects.SPELL_DAMAGE_BOOST,
                    DURATION,
                    0,
                    false,
                    true,
                    true
            ));

            com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);

            sp.sendSystemMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID + ".jiuhu_empowered"
            ));
        }

        return stack;
    }
}