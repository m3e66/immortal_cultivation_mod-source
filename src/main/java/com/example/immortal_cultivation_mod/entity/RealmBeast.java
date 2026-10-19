package com.example.immortal_cultivation_mod.entity;

/** One synchronized, saved realm per beast, independent of its species registry ID. */
public interface RealmBeast {
    int beastRealm();
    void setBeastRealm(int realm);
    default String beastCultivationLevel() { return BeastRealms.level(beastRealm()); }
}
