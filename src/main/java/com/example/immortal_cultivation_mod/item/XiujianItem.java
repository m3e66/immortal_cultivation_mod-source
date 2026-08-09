package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.entity.FlyingNeedleEntity;
import com.example.immortal_cultivation_mod.network.CombatPayloads;
import java.util.function.Consumer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import net.neoforged.neoforge.network.PacketDistributor;

public class XiujianItem extends Item implements GeoItem, ICurioItem {
    private static final String BACK_ACCESSORY = "back_accessory";
    private final AnimatableInstanceCache cache =
            GeckoLibUtil.createInstanceCache(this);

    public XiujianItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static boolean isEquipped(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .flatMap(handler -> handler.getStacksHandler(BACK_ACCESSORY)
                        .map(back -> {
                            for (int slot = 0;
                                 slot < back.getStacks().getSlots();
                                 slot++) {
                                if (back.getStacks().getStackInSlot(slot)
                                        .is(ModItems.XIUJIAN.get())) {
                                    return true;
                                }
                            }
                            return false;
                        }))
                .orElse(false);
    }

    public static boolean canFire(Player player) {
        return (isHeld(player)
                || (player.getMainHandItem().isEmpty()
                && isEquipped(player)))
                && findTopmostNeedleSlot(player) >= 0;
    }

    public static boolean isHeld(Player player) {
        return player.getMainHandItem().is(ModItems.XIUJIAN.get());
    }

    public static boolean fireNeedle(ServerPlayer player) {
        return fireNeedle(player, !isHeld(player));
    }

    public static boolean fireNeedle(
            ServerPlayer player,
            boolean explosive) {
        if (!canFire(player)) {
            return false;
        }
        int slot = findTopmostNeedleSlot(player);
        if (slot < 0) {
            return false;
        }

        ItemStack needle = player.getInventory().items.get(slot);
        ItemStack projectileStack = needle.copyWithCount(1);
        double launchQi = QiInfusedWeapon.getQi(needle);
        float injectedQiDamage =
                QiInfusedWeapon.injectedQiDamage(needle);

        FlyingNeedleEntity projectile = new FlyingNeedleEntity(
                player.serverLevel(),
                player,
                projectileStack,
                injectedQiDamage,
                launchQi,
                explosive);
        projectile.shootFromRotation(
                player,
                player.getXRot(),
                player.getYRot(),
                0.0F,
                3.35F,
                0.0F);
        player.serverLevel().addFreshEntity(projectile);

        if (!player.getAbilities().instabuild) {
            QiInfusedWeapon.consumeInjectedQiDamage(
                    needle, injectedQiDamage);
            damageNeedle(needle);
            player.getInventory().setChanged();
        }

        player.serverLevel().playSound(
                null,
                player.getX(),
                player.getEyeY(),
                player.getZ(),
                SoundEvents.CROSSBOW_SHOOT,
                SoundSource.PLAYERS,
                0.8F,
                1.55F);
        int firingAnimation = !isHeld(player)
                || player.getMainArm() == HumanoidArm.RIGHT
                ? CombatPayloads.XIUJIAN_FIRE_RIGHT_ANIMATION
                : CombatPayloads.XIUJIAN_FIRE_LEFT_ANIMATION;
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                player,
                new CombatPayloads.ClientboundSwordComboAnimationPayload(
                        player.getUUID(),
                        firingAnimation));
        player.swing(InteractionHand.MAIN_HAND, true);
        return true;
    }

    private static int findTopmostNeedleSlot(Player player) {
        int size = player.getInventory().items.size();
        for (int slot = 9; slot < size; slot++) {
            if (player.getInventory().items.get(slot)
                    .is(ModItems.FLYING_NEEDLE.get())) {
                return slot;
            }
        }
        for (int slot = 0; slot < Math.min(9, size); slot++) {
            if (player.getInventory().items.get(slot)
                    .is(ModItems.FLYING_NEEDLE.get())) {
                return slot;
            }
        }
        return -1;
    }

    private static void damageNeedle(ItemStack needle) {
        int nextDamage = needle.getDamageValue()
                + FlyingNeedleItem.DURABILITY_PER_SHOT;
        if (nextDamage >= needle.getMaxDamage()) {
            needle.shrink(1);
            return;
        }
        needle.setDamageValue(nextDamage);
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return BACK_ACCESSORY.equals(slotContext.identifier());
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private XiujianItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (renderer == null) {
                    renderer = new XiujianItemRenderer();
                }
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(
            AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
