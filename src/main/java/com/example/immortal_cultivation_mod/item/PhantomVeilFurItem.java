package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

/** Fox fur whose cultivation realm is inherited by forged disguise masks. */
public final class PhantomVeilFurItem extends Item {
    public static final String DEFAULT_REALM =
            CultivationLevels.REALM_YUANYING + CultivationLevels.STAGE_MID;
    private static final String FUR_REALM =
            ImmortalCultivationMod.MODID + ".phantom_veil_fur_realm";

    public PhantomVeilFurItem(Properties properties) {
        super(properties);
    }

    public static String realm(ItemStack stack) {
        if (stack.isEmpty()) {
            return DEFAULT_REALM;
        }
        CompoundTag tag = stack.getOrDefault(
                DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return normalizeRealm(tag.getString(FUR_REALM));
    }

    public static int realmIndex(ItemStack stack) {
        return CultivationLevels.getRealmIndex(realm(stack));
    }

    public static void setRealm(ItemStack stack, String realm) {
        if (stack.isEmpty()) {
            return;
        }
        CompoundTag tag = stack.getOrDefault(
                DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putString(FUR_REALM, normalizeRealm(realm));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static String displayRealm(String realm) {
        return CultivationLevels.getLevelDef(normalizeRealm(realm)).realm();
    }

    public static String normalizeRealm(String realm) {
        return switch (CultivationLevels.getRealmIndex(realm)) {
            case 2 -> CultivationLevels.REALM_ZHUJI + CultivationLevels.STAGE_MID;
            case 3 -> CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_MID;
            case 4 -> CultivationLevels.REALM_YUANYING + CultivationLevels.STAGE_MID;
            default -> DEFAULT_REALM;
        };
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag) {
        tooltip.add(Component.translatable(
                        "tooltip." + ImmortalCultivationMod.MODID
                                + ".phantom_veil_fur.realm",
                        displayRealm(realm(stack)))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
