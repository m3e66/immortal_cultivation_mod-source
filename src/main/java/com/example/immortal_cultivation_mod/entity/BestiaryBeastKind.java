package com.example.immortal_cultivation_mod.entity;

/** The five cultivation beasts introduced by the realm bestiary. */
public enum BestiaryBeastKind {
    SPIRIT_DEVOURING_RAT("spirit_devouring_rat"),
    IRON_SCALE_SCORPION("iron_scale_scorpion"),
    WIND_FEATHER_CLIFF_DEER("wind_feather_cliff_deer"),
    STONE_DEVOURING_TORTOISE("stone_devouring_tortoise"),
    EMBER_FEATHER_FIRE_OWL("ember_feather_fire_owl");

    private final String id;

    BestiaryBeastKind(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
