package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.entity.NpcSkinCatalog;
import com.example.immortal_cultivation_mod.network.DisguiseMaskPayloads;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public class DisguiseMaskItem extends ArmorItem implements GeoItem, ICurioItem {
    private static final String DISGUISE_SKIN =
            ImmortalCultivationMod.MODID + ".disguise_skin";
    private static final String HELMET_EQUIPPED =
            ImmortalCultivationMod.MODID + ".disguise_helmet_equipped";
    private static final String DISGUISE_REALM =
            ImmortalCultivationMod.MODID + ".disguise_realm";

    private final AnimatableInstanceCache cache =
            GeckoLibUtil.createInstanceCache(this);

    public DisguiseMaskItem(Properties properties) {
        super(
                ModArmorMaterials.DAOFU,
                Type.HELMET,
                properties.stacksTo(1).durability(220));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            return super.use(level, player, hand);
        }

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            ensureDisguiseSkin(stack, player);
            DisguiseMaskPayloads.openSkinPicker(
                    serverPlayer, hand, selectedSkin(stack));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag) {
        tooltip.add(Component.translatable(
                        "tooltip." + ImmortalCultivationMod.MODID
                                + ".disguise_mask.choose_skin")
                .withStyle(ChatFormatting.GRAY));

        tooltip.add(Component.translatable(
                        "tooltip." + ImmortalCultivationMod.MODID
                                + ".disguise_mask.realm",
                        PhantomVeilFurItem.displayRealm(disguiseRealm(stack)))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable(
                        "tooltip." + ImmortalCultivationMod.MODID
                                + ".disguise_mask.qi_sense")
                .withStyle(ChatFormatting.DARK_GRAY));

        String selected = selectedSkin(stack);
        if (NpcSkinCatalog.contains(selected)) {
            tooltip.add(Component.translatable(
                            "tooltip." + ImmortalCultivationMod.MODID
                                    + ".disguise_mask.selected",
                            Component.translatable(skinNameKey(selected)))
                    .withStyle(ChatFormatting.AQUA));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public void inventoryTick(
            ItemStack stack,
            Level level,
            Entity entity,
            int slotId,
            boolean isSelected) {
        super.inventoryTick(
                stack,
                level,
                entity,
                slotId,
                isSelected);

        if (level.isClientSide
                || !(entity instanceof LivingEntity living)) {
            return;
        }

        CompoundTag tag = stack
                .getOrDefault(
                        DataComponents.CUSTOM_DATA,
                        CustomData.EMPTY)
                .copyTag();

        boolean equippedAsHelmet =
                living.getItemBySlot(EquipmentSlot.HEAD) == stack;

        boolean wasEquippedAsHelmet =
                tag.getBoolean(HELMET_EQUIPPED);

        if (equippedAsHelmet && !wasEquippedAsHelmet) {
            ensureDisguiseSkin(stack, living);
            tag = stack
                    .getOrDefault(
                            DataComponents.CUSTOM_DATA,
                            CustomData.EMPTY)
                    .copyTag();

            tag.putBoolean(
                    HELMET_EQUIPPED,
                    true);

            stack.set(
                    DataComponents.CUSTOM_DATA,
                    CustomData.of(tag));

            return;
        }

        if (!equippedAsHelmet && wasEquippedAsHelmet) {
            tag.remove(HELMET_EQUIPPED);

            stack.set(
                    DataComponents.CUSTOM_DATA,
                    CustomData.of(tag));

            return;
        }

        ensureDisguiseSkin(stack, living);
    }

    @Override
    public boolean canEquip(
            SlotContext slotContext,
            ItemStack stack) {
        return LayeredClothingSlots.HEAD_ACCESSORY.equals(
                slotContext.identifier());
    }

    @Override
    public void onEquip(
            SlotContext slotContext,
            ItemStack previousStack,
            ItemStack stack) {
        LivingEntity wearer =
                slotContext.entity();

        if (wearer.level().isClientSide
                || !LayeredClothingSlots.HEAD_ACCESSORY.equals(
                slotContext.identifier())) {
            return;
        }

        ensureDisguiseSkin(stack, wearer);
    }

    public static boolean ensureDisguiseSkin(
            ItemStack stack,
            LivingEntity wearer) {
        if (!stack.is(ModItems.DISGUISE_MASK.get())
                || wearer.level().isClientSide) {
            return false;
        }

        CompoundTag tag = stack
                .getOrDefault(
                        DataComponents.CUSTOM_DATA,
                        CustomData.EMPTY)
                .copyTag();

        String currentSkin =
                tag.getString(DISGUISE_SKIN);

        if (NpcSkinCatalog.contains(currentSkin)) {
            return false;
        }

        tag.putString(
                DISGUISE_SKIN,
                NpcSkinCatalog.randomSkinId(wearer.getRandom()));

        stack.set(
                DataComponents.CUSTOM_DATA,
                CustomData.of(tag));

        return true;
    }

    public static ItemStack getEquippedMask(
            LivingEntity entity) {
        ItemStack helmet =
                entity.getItemBySlot(EquipmentSlot.HEAD);

        if (helmet.is(ModItems.DISGUISE_MASK.get())) {
            ensureDisguiseSkin(
                    helmet,
                    entity);

            return helmet;
        }

        ItemStack[] result = {ItemStack.EMPTY};

        CuriosApi.getCuriosInventory(entity).ifPresent(handler ->
                handler.getStacksHandler(
                        LayeredClothingSlots.HEAD_ACCESSORY)
                        .ifPresent(stacksHandler -> {
                            var stacks =
                                    stacksHandler.getStacks();

                            for (int slot = 0;
                                 slot < stacks.getSlots();
                                 slot++) {
                                ItemStack stack =
                                        stacks.getStackInSlot(slot);

                                if (stack.is(
                                        ModItems.DISGUISE_MASK.get())) {
                                    ensureDisguiseSkin(
                                            stack,
                                            entity);

                                    result[0] = stack;
                                    return;
                                }
                            }
                        }));

        return result[0];
    }

    public static boolean isWearing(
            LivingEntity entity) {
        return !getEquippedMask(entity).isEmpty();
    }

    public static String disguiseRealm(ItemStack stack) {
        if (stack.isEmpty()) {
            return PhantomVeilFurItem.DEFAULT_REALM;
        }
        CompoundTag tag = stack.getOrDefault(
                DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return PhantomVeilFurItem.normalizeRealm(
                tag.getString(DISGUISE_REALM));
    }

    public static void setDisguiseRealm(ItemStack stack, String realm) {
        if (stack.isEmpty() || !stack.is(ModItems.DISGUISE_MASK.get())) {
            return;
        }
        CompoundTag tag = stack.getOrDefault(
                DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putString(DISGUISE_REALM,
                PhantomVeilFurItem.normalizeRealm(realm));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static boolean canQiSenseReveal(
            String viewerCultivationLevel, ItemStack mask) {
        if (mask.isEmpty()) {
            return true;
        }
        int viewerRealm = CultivationLevels.getRealmIndex(
                viewerCultivationLevel);
        int maskRealm = CultivationLevels.getRealmIndex(
                disguiseRealm(mask));
        return viewerRealm > maskRealm;
    }

    public static String selectedSkin(ItemStack stack) {
        return stack
                .getOrDefault(
                        DataComponents.CUSTOM_DATA,
                        CustomData.EMPTY)
                .copyTag()
                .getString(DISGUISE_SKIN);
    }

    public static boolean setDisguiseSkin(
            ItemStack stack, String skin) {
        if (!stack.is(ModItems.DISGUISE_MASK.get())
                || !NpcSkinCatalog.contains(skin)) {
            return false;
        }

        CompoundTag tag = stack
                .getOrDefault(
                        DataComponents.CUSTOM_DATA,
                        CustomData.EMPTY)
                .copyTag();
        if (skin.equals(tag.getString(DISGUISE_SKIN))) {
            return false;
        }

        tag.putString(DISGUISE_SKIN, skin);
        stack.set(
                DataComponents.CUSTOM_DATA,
                CustomData.of(tag));
        return true;
    }

    public static String skinNameKey(String skin) {
        return "screen." + ImmortalCultivationMod.MODID
                + ".disguise_mask_skins.skin." + skin;
    }

    public static ResourceLocation disguiseTexture(
            ItemStack stack) {
        CompoundTag tag = stack
                .getOrDefault(
                        DataComponents.CUSTOM_DATA,
                        CustomData.EMPTY)
                .copyTag();

        String skin =
                tag.getString(DISGUISE_SKIN);

        return NpcSkinCatalog.texture(skin);
    }

    @Override
    public void createGeoRenderer(
            Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private DisguiseMaskArmorRenderer renderer;

            @Override
            public <T extends LivingEntity>
            HumanoidModel<?> getGeoArmorRenderer(
                    T livingEntity,
                    ItemStack itemStack,
                    EquipmentSlot equipmentSlot,
                    HumanoidModel<T> original) {
                if (renderer == null) {
                    renderer =
                            new DisguiseMaskArmorRenderer();
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
    public AnimatableInstanceCache
    getAnimatableInstanceCache() {
        return cache;
    }
}
