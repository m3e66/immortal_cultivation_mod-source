package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.client.ClientData;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

public final class TechniqueDescriptions {
    private static final String SCREEN_PREFIX =
            "screen." + ImmortalCultivationMod.MODID + ".description.";

    private TechniqueDescriptions() {
    }

    public static Component spell(ModSpells.SpellDef spell) {
        String baseKey = "spell." + ImmortalCultivationMod.MODID + "."
                + spell.translationKey() + ".description";
        if (!ClientData.chinesePoemDescriptions()) {
            return Component.translatable(baseKey);
        }

        String poemKey = baseKey + ".poem";
        if (I18n.exists(poemKey)) {
            return Component.translatable(poemKey);
        }
        return Component.translatable(
                SCREEN_PREFIX + "spell_poem_fallback",
                spell.name(),
                element(spell.element()));
    }

    public static Component method(String methodId) {
        CultivationMethods.MethodDef method = CultivationMethods.get(methodId);
        if (method == null) {
            return Component.translatable("screen." + ImmortalCultivationMod.MODID + ".unknown_method");
        }
        if (ClientData.chinesePoemDescriptions()) {
            String poemKey = "method_poem." + ImmortalCultivationMod.MODID + "." + method.id();
            if (I18n.exists(poemKey)) {
                return Component.translatable(poemKey);
            }
            return Component.translatable(
                    SCREEN_PREFIX + "method_poem_fallback",
                    Component.translatable(method.nameKey()));
        }
        return Component.translatable(methodEffectKey(methodId));
    }

    public static Component element(String element) {
        if (element == null || element.isBlank() || "any".equals(element) || "none".equals(element)) {
            return Component.translatable(SCREEN_PREFIX + "none");
        }
        return Component.literal(element);
    }

    public static String methodEffectKey(String methodId) {
        CultivationMethods.MethodDef method = CultivationMethods.get(methodId);
        if (method == null) {
            return "method_special." + ImmortalCultivationMod.MODID + ".none";
        }
        if (method.bloodDemon()) {
            return "method_special." + ImmortalCultivationMod.MODID + ".blood_demon";
        }
        if (CultivationMethods.isReincarnationTrueArt(methodId)) {
            return "method_special." + ImmortalCultivationMod.MODID + ".reincarnation_true_art";
        }
        if (CultivationMethods.isTuntianDemonArt(methodId)) {
            return "method_special." + ImmortalCultivationMod.MODID + ".tuntian_demon_art";
        }
        if (CultivationMethods.isPokongJue(methodId)) {
            return "method_special." + ImmortalCultivationMod.MODID + ".pokong_jue";
        }
        if (CultivationMethods.isChangqingJue(methodId)) {
            return "method_special." + ImmortalCultivationMod.MODID + ".changqing_jue";
        }
        if (CultivationMethods.isFentianLifeRenewal(methodId)) {
            return "method_special." + ImmortalCultivationMod.MODID + ".fentian_life_renewal";
        }
        if (CultivationMethods.isHantiBingqin(methodId)) {
            return "method_special." + ImmortalCultivationMod.MODID + ".hanti_bingqin";
        }
        if (CultivationMethods.isCuiguLianshenJue(methodId)) {
            return "method_special." + ImmortalCultivationMod.MODID + ".cuigu_lianshen_jue";
        }
        if (CultivationMethods.isJiuyouLianshiJue(methodId)) {
            return "method_special." + ImmortalCultivationMod.MODID + ".jiuyou_lianshi_jue";
        }
        return "method_special." + ImmortalCultivationMod.MODID + ".none";
    }
}
