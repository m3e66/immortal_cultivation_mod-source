package com.example.immortal_cultivation_mod.network;

import com.example.immortal_cultivation_mod.item.HigherWeaponSpec;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/** Right-click motion belongs to the weapon family, not its element or skill variant. */
public final class WeaponUseAnimations {
    private WeaponUseAnimations() {}

    public enum Type {
        SWORD(-20, "weapon_skill_sword", false),
        SABER(-21, "weapon_skill_saber", false),
        GREATSWORD(-22, "weapon_skill_greatsword", true),
        SPEAR(-23, "weapon_skill_spear", true),
        HALBERD(-24, "weapon_skill_halberd", true),
        HAMMER(-25, "weapon_skill_hammer", true),
        FAN(-26, "weapon_skill_fan", false),
        WHIP(-27, "weapon_skill_whip", false),
        HOOK(-28, "weapon_skill_hook", false),
        BANNER(-29, "weapon_skill_banner", true),
        FUCHEN(-30, "weapon_skill_fuchen", true),
        KNIFE(-31, "weapon_skill_knife", false),
        PAGODA(-32, "weapon_skill_pagoda", true);

        public final int packetId;
        public final String clip;
        public final boolean bothArms;

        Type(int packetId, String clip, boolean bothArms) {
            this.packetId = packetId;
            this.clip = clip;
            this.bothArms = bothArms;
        }
    }

    public static Type type(HigherWeaponSpec spec) {
        return switch (spec) {
            case BLACK_IRON_JIAN, THUNDERWOOD_SWORD, SEVEN_STAR_SWORD -> Type.SWORD;
            case BLACK_IRON_GREATSWORD -> Type.GREATSWORD;
            case FLAME_PATTERN_DAO -> Type.SABER;
            case MOON_CRESCENT_HALBERD -> Type.HALBERD;
            case EARTHBREAKER_HAMMER -> Type.HAMMER;
            case WIND_FEATHER_FAN -> Type.FAN;
            case SPIRIT_SILK_WHIP -> Type.WHIP;
            case SOUL_HOOK -> Type.HOOK;
        };
    }

    public static Type fromPacket(int id) {
        for (Type type : Type.values()) if (type.packetId == id) return type;
        return null;
    }

    public static void play(ServerPlayer player, Type type) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                new CombatPayloads.ClientboundSwordComboAnimationPayload(player.getUUID(), type.packetId));
    }
}
