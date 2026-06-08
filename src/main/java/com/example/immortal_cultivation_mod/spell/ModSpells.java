package com.example.immortal_cultivation_mod.spell;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ModSpells {
    public static final String FIREBALL = "fireball";
    public static final String LINGBENG = "lingbeng";
    public static final String REGENERATION = "regeneration";
    public static final String BEAM = "beam";

    private static final Map<String, SpellDef> SPELLS = createSpells();

    private static Map<String, SpellDef> createSpells() {
        Map<String, SpellDef> spells = new LinkedHashMap<>();
        spells.put(FIREBALL, new SpellDef(
                FIREBALL,
                "fireball",
                CultivationLevels.REALM_LIANQI + CultivationLevels.STAGE_EARLY,
                10,
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/gui/spell_fireball.png")
        ));
        spells.put(LINGBENG, new SpellDef(
                LINGBENG,
                "lingbeng",
                CultivationLevels.REALM_LIANQI + CultivationLevels.STAGE_LATE,
                50,
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/gui/spell_lingbeng.png")
        ));
        spells.put(REGENERATION, new SpellDef(
                REGENERATION,
                "regeneration",
                CultivationLevels.REALM_LIANQI + CultivationLevels.STAGE_MID,
                20,
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/gui/spell_regeneration.png")
        ));
        spells.put(BEAM, new SpellDef(
                BEAM,
                "beam",
                CultivationLevels.REALM_LIANQI + CultivationLevels.STAGE_EARLY,
                30,
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/gui/spell_beam.png")
        ));
        return Map.copyOf(spells);
    }

    public static List<SpellDef> all() {
        return SPELLS.values().stream().toList();
    }

    public static SpellDef get(String id) {
        return SPELLS.get(normalizeId(id));
    }

    public static String normalizeId(String id) {
        if (id == null) {
            return "";
        }
        if ("Lingbeng".equalsIgnoreCase(id) || "lingbeng".equalsIgnoreCase(id) || "\u7075\u5d29".equals(id)) {
            return LINGBENG;
        }
        if ("Fireball".equalsIgnoreCase(id) || "fireball".equalsIgnoreCase(id) || "\u706b\u7403\u672f".equals(id)) {
            return FIREBALL;
        }
        if ("Regeneration".equalsIgnoreCase(id) || "regen".equalsIgnoreCase(id) || "regeneration".equalsIgnoreCase(id) || "\u56de\u6625\u672f".equals(id)) {
            return REGENERATION;
        }
        if ("Beam".equalsIgnoreCase(id) || "beam".equalsIgnoreCase(id) || "\u5149\u675f\u672f".equals(id)) {
            return BEAM;
        }
        return id;
    }

    public static boolean meetsRequirement(String currentLevel, SpellDef spell) {
        return CultivationLevels.getStageIndex(currentLevel) >= CultivationLevels.getStageIndex(spell.requiredLevel());
    }

    public record SpellDef(String id, String translationKey, String requiredLevel, int qiCost, ResourceLocation icon) {
        public Component name() {
            return Component.translatable("spell." + ImmortalCultivationMod.MODID + "." + translationKey);
        }

        public Component description() {
            return Component.translatable("spell." + ImmortalCultivationMod.MODID + "." + translationKey + ".description");
        }
    }
}
