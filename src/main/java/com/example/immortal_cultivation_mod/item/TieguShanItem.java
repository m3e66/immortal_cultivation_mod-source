package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.util.GeckoLibUtil;

public class TieguShanItem extends SwordItem implements GeoItem {
    private static final String OPEN_TAG = "ImmortalCultivationTieguShanOpen";
    private static final String RESERVED_QI_TAG = "ImmortalCultivationTieguShanReservedQi";
    private static final float CLOSED_ATTACK_BONUS = 0.0F;
    private static final float OPEN_ATTACK_BONUS = 2.0F;
    private static final RawAnimation OPEN_ANIMATION = RawAnimation.begin()
            .thenPlayAndHold("animation.tiegu_shan.open");
    private static final RawAnimation CLOSE_ANIMATION = RawAnimation.begin()
            .thenPlayAndHold("animation.tiegu_shan.close");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public TieguShanItem(Properties properties) {
        super(Tiers.IRON, properties.stacksTo(1)
                .durability(Tiers.IRON.getUses())
                .attributes(SwordItem.createAttributes(
                        Tiers.IRON, CLOSED_ATTACK_BONUS, -2.4F)));
        GeoItem.registerSyncedAnimatable(this);
    }

    public static boolean isOpen(ItemStack stack) {
        return stack != null
                && stack.getItem() instanceof TieguShanItem
                && stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag()
                .getBoolean(OPEN_TAG);
    }

    public static boolean isClosed(ItemStack stack) {
        return stack != null
                && stack.getItem() instanceof TieguShanItem
                && !isOpen(stack);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }

        boolean open = !isOpen(stack);
        if (!level.isClientSide) {
            setOpen(stack, open);
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)
                com.example.immortal_cultivation_mod.network.WeaponUseAnimations.play(serverPlayer,
                        com.example.immortal_cultivation_mod.network.WeaponUseAnimations.Type.FAN);
            if (level instanceof ServerLevel serverLevel) {
                triggerAnim(player, GeoItem.getOrAssignId(stack, serverLevel),
                        "form", open ? "open" : "close");
            }
            player.displayClientMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID
                            + ".tiegu_shan." + (open ? "opened" : "closed")), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    public static void setOpen(ItemStack stack, boolean open) {
        CompoundTag tag = stack.getOrDefault(
                DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (open) {
            tag.putBoolean(OPEN_TAG, true);
        } else {
            tag.remove(OPEN_TAG);
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        applyFormDamage(stack, open);
        rebalanceQi(stack);
    }

    public static double getReservedQi(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof TieguShanItem)) {
            return 0.0D;
        }
        return Math.max(0.0D, stack.getOrDefault(
                DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag()
                .getDouble(RESERVED_QI_TAG));
    }

    static double remainingQiStorage(ItemStack stack) {
        return Math.max(0.0D, openStorageLimit(stack)
                - QiInfusedWeapon.getQi(stack)
                - getReservedQi(stack));
    }

    static double storeInjectedQi(ItemStack stack, double amount) {
        double stored = Math.min(Math.max(0.0D, amount), remainingQiStorage(stack));
        if (stored <= 0.001D) {
            return 0.0D;
        }
        QiInfusedWeapon.setQi(stack, QiInfusedWeapon.getQi(stack) + stored);
        rebalanceQi(stack);
        return stored;
    }

    private static void rebalanceQi(ItemStack stack) {
        double activeQi = QiInfusedWeapon.getQi(stack);
        double reservedQi = getReservedQi(stack);
        double safeLimit = QiInfusedWeapon.storageLimit(stack);
        double openLimit = openStorageLimit(stack);
        double originalActiveQi = activeQi;
        double originalReservedQi = reservedQi;

        if (activeQi > safeLimit + 0.001D) {
            double excess = activeQi - safeLimit;
            activeQi = safeLimit;
            reservedQi += excess;
        }

        double availableRoom = safeLimit - activeQi;
        if (reservedQi > 0.001D && availableRoom > 0.001D) {
            double restored = Math.min(reservedQi, availableRoom);
            activeQi += restored;
            reservedQi -= restored;
        }

        double reserveCapacity = Math.max(0.0D, openLimit - activeQi);
        reservedQi = Math.min(reservedQi, reserveCapacity);

        if (Math.abs(activeQi - originalActiveQi) > 0.001D) {
            QiInfusedWeapon.setQi(stack, activeQi);
        }
        if (Math.abs(reservedQi - originalReservedQi) > 0.001D) {
            setReservedQi(stack, reservedQi);
        }
    }

    private static double openStorageLimit(ItemStack stack) {
        double openDamage = QiInfusedWeapon.baseAttackDamage(stack);
        if (!isOpen(stack)) {
            openDamage += (OPEN_ATTACK_BONUS - CLOSED_ATTACK_BONUS)
                    * QiInfusedWeapon.attackDamageAddCoefficient(stack);
        }
        return Math.max(1.0D, openDamage) * 100.0D
                * WeaponRestrictionSystem.qiStorageMultiplier(stack);
    }

    private static void setReservedQi(ItemStack stack, double reservedQi) {
        CompoundTag tag = stack.getOrDefault(
                DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (reservedQi <= 0.001D) {
            tag.remove(RESERVED_QI_TAG);
        } else {
            tag.putDouble(RESERVED_QI_TAG, reservedQi);
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static void applyFormDamage(ItemStack stack, boolean open) {
        double damageModifier = Tiers.IRON.getAttackDamageBonus()
                + (open ? OPEN_ATTACK_BONUS : CLOSED_ATTACK_BONUS);
        ItemAttributeModifiers current = stack.getOrDefault(
                DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
        boolean foundBaseDamage = false;

        for (ItemAttributeModifiers.Entry entry : current.modifiers()) {
            if (entry.attribute().is(Attributes.ATTACK_DAMAGE)
                    && entry.modifier().id().equals(Item.BASE_ATTACK_DAMAGE_ID)) {
                builder.add(entry.attribute(), new AttributeModifier(
                        Item.BASE_ATTACK_DAMAGE_ID,
                        damageModifier,
                        AttributeModifier.Operation.ADD_VALUE), entry.slot());
                foundBaseDamage = true;
            } else {
                builder.add(entry.attribute(), entry.modifier(), entry.slot());
            }
        }

        if (!foundBaseDamage) {
            builder.add(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                    Item.BASE_ATTACK_DAMAGE_ID,
                    damageModifier,
                    AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
        }
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, builder.build());
    }

    @Override
    public void inventoryTick(
            ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!level.isClientSide) {
            rebalanceQi(stack);
        }
    }

    @Override
    public void appendHoverText(
            ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        double reservedQi = getReservedQi(stack);
        if (reservedQi > 0.001D) {
            tooltip.add(Component.translatable(
                    "tooltip." + ImmortalCultivationMod.MODID + ".tiegu_shan.reserved_qi",
                    QiInfusedWeapon.format(reservedQi)).withStyle(ChatFormatting.DARK_AQUA));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private TieguShanItemRenderer renderer;

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (renderer == null) {
                    renderer = new TieguShanItemRenderer();
                }
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "form", 0, state -> {
            ItemStack stack = state.getData(DataTickets.ITEMSTACK);
            return isOpen(stack)
                    ? state.setAndContinue(OPEN_ANIMATION)
                    : PlayState.STOP;
        }).triggerableAnim("open", OPEN_ANIMATION)
                .triggerableAnim("close", CLOSE_ANIMATION));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
