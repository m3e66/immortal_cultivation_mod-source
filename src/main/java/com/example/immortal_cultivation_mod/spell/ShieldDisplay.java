package com.example.immortal_cultivation_mod.spell;

import java.util.ArrayList;
import java.util.List;

/** Shared shield identities survive aggregation, networking and HUD layout. */
public final class ShieldDisplay {
    public enum Kind {
        HUTI_QI(0xD9EDF5), DIELANG(0x409EFF), GUSHI(0xAE865B),
        BLOOD_PILL(0xF18DB5), EARTH(0xDCA52B), WIND(0x69DF9E),
        BLOOD(0xCD294C), MIRROR(0xB59AFF), DOMAIN(0x38D9E6),
        HEARTGUARD(0x258D81), BONE(0xE7CF9F),
        THIN_GOLD_TALISMAN(0xF5DF50), GOLDEN_BODY_TALISMAN(0xF28839),
        LEGACY_TALISMAN(0xA6A7D9), OTHER(0xA2AFBE);

        public final int color;
        Kind(int color){this.color=color;}
        public static Kind source(String source) {
            if(source.startsWith(ModSpells.YUANGANG_HUJIE+":"))return DOMAIN;
            return switch(source) {
                case "huti_qi" -> HUTI_QI;
                case "dielang_shield" -> DIELANG;
                case "gushi_shield" -> GUSHI;
                case "blood_qi_pill" -> BLOOD_PILL;
                case ModSpells.XUANYAN_HUTI -> EARTH;
                case ModSpells.LIUFENG_XIEJIN -> WIND;
                case ModSpells.XUESHA_HUSHEN -> BLOOD;
                case ModSpells.JINGLING_FANZHAO -> MIRROR;
                case ModSpells.YUANGANG_HUJIE -> DOMAIN;
                case "black_tortoise_heartguard_mirror" -> HEARTGUARD;
                case "undead_bone_growth" -> BONE;
                case "thin_gold_talisman" -> THIN_GOLD_TALISMAN;
                case "golden_body_talisman" -> GOLDEN_BODY_TALISMAN;
                case "talisman_shields" -> LEGACY_TALISMAN;
                default -> OTHER;
            };
        }
    }

    public record Layer(Kind kind,float amount,float max) {
        public Layer {
            if(kind==null||!Float.isFinite(amount)||!Float.isFinite(max)||amount<=0||max<=0)
                throw new IllegalArgumentException("Invalid shield layer");
            amount=Math.min(amount,max);
        }
    }
    /** Bottom-relative row, including reserved visibility for even a tiny shield. */
    public record Segment(Layer layer,int bottom,int height,int filled) { }
    private ShieldDisplay() { }

    public static float total(List<Layer> layers,boolean maximum) {
        double total=0;for(var layer:layers)total+=maximum?layer.max():layer.amount();
        return (float)Math.min(Float.MAX_VALUE,total);
    }

    public static List<Segment> segments(List<Layer> layers,int height) {
        if(layers.isEmpty()||height<=0)return List.of();
        int count=Math.min(layers.size(),height),gap=height>=count*3-1?1:0;
        int minimum=height>=count*2+gap*(count-1)?2:1;
        int spare=height-minimum*count-gap*(count-1),bottom=0;
        double total=0;for(int i=0;i<count;i++)total+=layers.get(i).max();
        double accumulated=0;int previous=0;
        var result=new ArrayList<Segment>(count);
        for(int i=0;i<count;i++) {
            var layer=layers.get(i);accumulated+=layer.max();
            int share=i==count-1?spare:(int)Math.floor(spare*accumulated/total);
            int size=minimum+share-previous;previous=share;
            int filled=Math.clamp(Math.round(size*(layer.amount()/layer.max())),1,size);
            result.add(new Segment(layer,bottom,size,filled));bottom+=size+gap;
        }
        return List.copyOf(result);
    }
}
