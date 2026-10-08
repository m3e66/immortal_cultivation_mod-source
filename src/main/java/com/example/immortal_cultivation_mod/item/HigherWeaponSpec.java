package com.example.immortal_cultivation_mod.item;

public enum HigherWeaponSpec {
    BLACK_IRON_JIAN("black_iron_jian", 12F, 1.8F, 2250, 3, false, 2),
    BLACK_IRON_GREATSWORD("black_iron_greatsword", 21F, 0.9F, 3200, 3.5, false, 2),
    MOON_CRESCENT_HALBERD("moon_crescent_halberd", 19F, 1.2F, 3000, 4.5, false, 2),
    SPIRIT_SILK_WHIP("spirit_silk_whip", 11F, 1.5F, 2400, 6, true, 2),
    SOUL_HOOK("soul_hook", 15F, 1.3F, 2500, 5, true, 2),
    WIND_FEATHER_FAN("wind_feather_fan", 11F, 1.5F, 2200, 3, false, 2),
    THUNDERWOOD_SWORD("thunderwood_sword", 17F, 1.6F, 2750, 3, false, 2),
    FLAME_PATTERN_DAO("flame_pattern_dao", 19F, 1.5F, 2800, 3, false, 2),
    EARTHBREAKER_HAMMER("earthbreaker_hammer", 27F, 0.75F, 4000, 3.5, true, 2),
    SEVEN_STAR_SWORD("seven_star_sword", 30F, 1.6F, 5000, 3, false, 3);
    public final String id;
    public final float damage, speed;
    public final int durability, realm;
    public final double reach;
    public final boolean geo;
    HigherWeaponSpec(String id,float damage,float speed,int durability,double reach,boolean geo,int realm) {
        this.id=id;this.damage=damage;this.speed=speed;this.durability=durability;
        this.reach=reach;this.geo=geo;this.realm=realm;
    }
    public static HigherWeaponSpec find(String id) {
        for(var spec:values())if(spec.id.equals(id))return spec;
        return null;
    }
}
