package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

/** Shared tier and crafted-quality data for every cultivation talisman. */
public class TalismanItem extends Item {
    private static final String QUALITY_TAG =
            ImmortalCultivationMod.MODID + ":talisman_quality";
    private static final String DRAWING_DATA_TAG =
            ImmortalCultivationMod.MODID + ":talisman_drawing";

    // Easy-to-tune crafted quality weights. Creative/unmarked stacks are MID.
    public static final int LOW_QUALITY_WEIGHT = 40;
    public static final int MID_QUALITY_WEIGHT = 35;
    public static final int HIGH_QUALITY_WEIGHT = 20;
    public static final int SUPREME_QUALITY_WEIGHT = 5;

    private final Tier tier;

    public TalismanItem(Properties properties, Tier tier) {
        super(properties);
        this.tier = tier;
    }

    public Tier tier() {
        return tier;
    }

    public static boolean hasStoredQuality(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().contains(QUALITY_TAG);
    }

    public static Quality quality(ItemStack stack) {
        CompoundTag root = stack.getOrDefault(
                DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return root.contains(QUALITY_TAG)
                ? Quality.byId(root.getInt(QUALITY_TAG))
                : Quality.MID;
    }

    public static void setQuality(ItemStack stack, Quality quality) {
        if (!(stack.getItem() instanceof TalismanItem)) return;
        CompoundTag root = stack.getOrDefault(
                DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        root.putInt(QUALITY_TAG, quality.id());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    public static void rollCraftedQuality(ItemStack stack, RandomSource random) {
        int total = LOW_QUALITY_WEIGHT + MID_QUALITY_WEIGHT
                + HIGH_QUALITY_WEIGHT + SUPREME_QUALITY_WEIGHT;
        int roll = random.nextInt(Math.max(1, total));
        Quality result;
        if (roll < LOW_QUALITY_WEIGHT) {
            result = Quality.LOW;
        } else if ((roll -= LOW_QUALITY_WEIGHT) < MID_QUALITY_WEIGHT) {
            result = Quality.MID;
        } else if ((roll -= MID_QUALITY_WEIGHT) < HIGH_QUALITY_WEIGHT) {
            result = Quality.HIGH;
        } else {
            result = Quality.SUPREME;
        }
        setQuality(stack, result);
    }

    public static void setDrawingData(ItemStack stack, String formulaId,
                                      int paperQuality, int inkPurity,
                                      int drawingAccuracy, int qiControl,
                                      int finalScore) {
        if (!(stack.getItem() instanceof TalismanItem)) return;
        CompoundTag root = stack.getOrDefault(
                DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag drawing = new CompoundTag();
        drawing.putString("formula", formulaId == null ? "" : formulaId);
        drawing.putInt("paper_quality", clampPercent(paperQuality));
        drawing.putInt("ink_purity", clampPercent(inkPurity));
        drawing.putInt("drawing_accuracy", clampPercent(drawingAccuracy));
        drawing.putInt("qi_control", clampPercent(qiControl));
        drawing.putInt("final_score", clampPercent(finalScore));
        drawing.putBoolean("law_seal", true);
        root.put(DRAWING_DATA_TAG, drawing);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    private static int clampPercent(int value) {
        return Math.max(0, Math.min(100, value));
    }

    @Override
    public void appendHoverText(
            ItemStack stack, TooltipContext context,
            List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(
                "tooltip." + ImmortalCultivationMod.MODID + ".talisman.tier",
                Component.translatable(tier.translationKey()))
                .withColor(tier.color()));
        Quality quality = quality(stack);
        tooltip.add(Component.translatable(
                "tooltip." + ImmortalCultivationMod.MODID + ".talisman.quality",
                Component.translatable(quality.translationKey()))
                .withColor(quality.color()));
        CompoundTag root = stack.getOrDefault(
                DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (root.contains(DRAWING_DATA_TAG)) {
            CompoundTag drawing = root.getCompound(DRAWING_DATA_TAG);
            tooltip.add(Component.translatable(
                    "tooltip." + ImmortalCultivationMod.MODID + ".talisman.craft_score",
                    drawing.getInt("final_score")).withColor(0xB58A3E));
            tooltip.add(Component.translatable(
                    "tooltip." + ImmortalCultivationMod.MODID + ".talisman.craft_details",
                    drawing.getInt("paper_quality"), drawing.getInt("ink_purity"),
                    drawing.getInt("drawing_accuracy"), drawing.getInt("qi_control"))
                    .withColor(0x756F63));
            if (drawing.getBoolean("law_seal")) {
                tooltip.add(Component.translatable(
                        "tooltip." + ImmortalCultivationMod.MODID + ".talisman.law_seal")
                        .withColor(0xA12A21));
            }
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }

    public enum Tier {
        ONE("one", 0xB7B7B7),
        TWO("two", 0x55C878),
        THREE("three", 0x58A8E8),
        FOUR("four", 0xB77AE8),
        FIVE("five", 0xE8B84F),
        SIX("six", 0xE8874F),
        SEVEN("seven", 0xE85D5D),
        EIGHT("eight", 0xF0E8C0),
        ANCIENT("ancient", 0xC89B53);

        private final String id;
        private final int color;

        Tier(String id, int color) {
            this.id = id;
            this.color = color;
        }

        public String translationKey() {
            return "talisman_tier." + ImmortalCultivationMod.MODID + "." + id;
        }

        public int color() {
            return color;
        }
    }

    public enum Quality {
        LOW(0, "low", 0xAAAAAA),
        MID(1, "mid", 0x55C878),
        HIGH(2, "high", 0x58A8E8),
        SUPREME(3, "supreme", 0xE8B84F);

        private final int id;
        private final String name;
        private final int color;

        Quality(int id, String name, int color) {
            this.id = id;
            this.name = name;
            this.color = color;
        }

        public int id() {
            return id;
        }

        public String translationKey() {
            return "talisman_quality." + ImmortalCultivationMod.MODID + "." + name;
        }

        public int color() {
            return color;
        }

        public static Quality byId(int id) {
            for (Quality quality : values()) {
                if (quality.id == id) return quality;
            }
            return MID;
        }
    }
}
