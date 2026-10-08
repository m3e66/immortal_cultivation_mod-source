package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Paper and ink used by the talisman drawing table. Purity is stored by {@link ForgingSystem}. */
public final class TalismanMaterialItem extends Item {
    private final Kind kind;
    private final int tier;

    public TalismanMaterialItem(Properties properties, Kind kind, int tier) {
        super(properties);
        this.kind = kind;
        this.tier = Math.max(1, Math.min(kind == Kind.PAPER ? 5 : 8, tier));
    }

    public Kind kind() {
        return kind;
    }

    public int tier() {
        return tier;
    }

    public static boolean isPaper(ItemStack stack) {
        return stack.getItem() instanceof TalismanMaterialItem material
                && material.kind == Kind.PAPER;
    }

    public static boolean isInk(ItemStack stack) {
        return stack.getItem() instanceof TalismanMaterialItem material
                && material.kind == Kind.INK;
    }

    public static int tier(ItemStack stack) {
        return stack.getItem() instanceof TalismanMaterialItem material
                ? material.tier : 0;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(
                "tooltip." + ImmortalCultivationMod.MODID + ".talisman_material." + kind.id,
                tier).withColor(kind.color));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    public enum Kind {
        PAPER("paper", 0xD4B15E),
        INK("ink", 0xA83B2E);

        private final String id;
        private final int color;

        Kind(String id, int color) {
            this.id = id;
            this.color = color;
        }
    }
}
