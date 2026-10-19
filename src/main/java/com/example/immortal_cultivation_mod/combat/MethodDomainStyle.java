package com.example.immortal_cultivation_mod.combat;

import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.particle.ModParticles;
import net.minecraft.core.particles.SimpleParticleType;

/** Registered methods map to an aura, accent and optional mastered-domain behavior. */
public enum MethodDomainStyle {
    BREATH("basic_breathing",0x73E8EB,0xD8FFFF,Kind.AIR,"spirit"),
    CLEAR("clear_heart",0xD4F7EE,0x81DDE6,Kind.CLEAR,"spirit"),
    BLOOD_THORN("blood_demon_jindan",0x8C1637,0xF17287,Kind.BLOOD,"spark"),
    BLOOD_DRAIN("blood_demon_yuanying",0x80132F,0xF07A90,Kind.DRAIN,"void"),
    BLOOD_CROWN("blood_demon_huashen",0x720D2D,0xFF9BAE,Kind.CROWN,"void"),
    REINCARNATION("reincarnation_true_art",0x9A71DB,0xB9EFEA,Kind.REINCARNATION,"void"),
    DEVOUR("tuntian_demon_art",0x351444,0xAB74CF,Kind.DEVOUR,"void"),
    WIND("pokong_jue",0x76EBC3,0xD8FFF0,Kind.WIND,"spark"),
    EVERGREEN("changqing_jue",0x48D875,0xBDFFAC,Kind.WOOD,"leaf"),
    RENEWAL_FIRE("fentian_life_renewal",0xFF5C32,0xFFCE76,Kind.FIRE,"ember"),
    ICE("hanti_bingqin",0x78DAFF,0xD2FFFF,Kind.ICE,"frost"),
    BONE("cuigu_lianshen_jue",0xDDD5B9,0xFFFFFF,Kind.BONE,"ash"),
    NETHER("jiuyou_lianshi_jue",0x6E63B7,0xB0FFD1,Kind.NETHER,"void"),
    SOLAR("dari_bati_jue",0xFF9D35,0xFFF1A4,Kind.SOLAR,"ember"),
    STORM("gangting_xiaokong_lu",0xA981FF,0x83F4D6,Kind.STORM,"spark"),
    MOUNTAIN("xuanyue_suifeng_jue",0xD6AB54,0xE1F1C4,Kind.MOUNTAIN,"ash"),
    COLD_TIDE("hanlan_ningyuan_jing",0x3FAEEB,0xBBF8FF,Kind.TIDE,"water");
    public enum Kind { AIR,CLEAR,BLOOD,DRAIN,CROWN,REINCARNATION,DEVOUR,WIND,WOOD,FIRE,ICE,BONE,NETHER,SOLAR,STORM,MOUNTAIN,TIDE }
    public final String method,accent;
    public final int color,secondary;
    public final Kind kind;
    MethodDomainStyle(String method,int color,int secondary,Kind kind,String accent) {
        this.method=method;this.color=color;this.secondary=secondary;this.kind=kind;this.accent=accent;
    }
    public static MethodDomainStyle of(String method) {
        for(var style:values())if(style.method.equals(method))return style;
        return BREATH;
    }
    public SimpleParticleType particle() {
        return switch(accent) {
            case "leaf" -> ModParticles.AMBIENT_LEAF.get();
            case "ember" -> ModParticles.AMBIENT_EMBER.get();
            case "frost" -> ModParticles.AMBIENT_FROST.get();
            case "water" -> ModParticles.METHOD_QI_WATER.get();
            case "void" -> ModParticles.SPELL_VOID.get();
            case "spark" -> ModParticles.SPELL_SPARK.get();
            case "ash" -> ModParticles.AMBIENT_ASH.get();
            default -> ModParticles.AMBIENT_SPIRIT.get();
        };
    }
    public static boolean coversAllMethods() {
        return CultivationMethods.allMethods().stream().allMatch(m -> java.util.Arrays.stream(values()).anyMatch(s->s.method.equals(m.id())));
    }
}
