package com.example.immortal_cultivation_mod.externalpower;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.companion.XuanjinRules;
import com.example.immortal_cultivation_mod.entity.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class ExternalPowerTargets {
    private ExternalPowerTargets() {}
    /** Negative means this entity has no cultivation-realm contract and must remain untouched. */
    public static int realm(LivingEntity target) {
        if (target instanceof RealmBeast beast) return beast.beastRealm();
        if (target instanceof net.minecraft.world.entity.animal.Wolf) return 0;
        if(target instanceof Player p)return CultivationLevels.getRealmIndex(ModAttachments.getData(p).cultivationLevel());
        if(target instanceof CultivationNpc npc)return CultivationLevels.getRealmIndex(npc.npcRealm());
        if(target instanceof PhantomVeilFoxEntity fox)return CultivationLevels.getRealmIndex(fox.cultivationRealm());
        if(target instanceof FrostspineCaveBearEntity bear)return CultivationLevels.getRealmIndex(bear.cultivationRealm());
        if(target instanceof ThunderhornRockBadgerEntity badger)return CultivationLevels.getRealmIndex(badger.cultivationRealm());
        if(target instanceof VaultGuardianEntity guardian)return CultivationLevels.getRealmIndex(guardian.cultivationRealm());
        if(target instanceof CrimsonSkyFlameFloodDragonEntity || target instanceof BloodApeDemonGeneralEntity)return 4;
        if(target instanceof CrimsonScaleFlameLizardEntity)return 3;
        if(target instanceof KongmingBeetleEntity)return 2;
        if(target instanceof AzureSpiritSilkwormEntity)return 1;
        if(target instanceof XuanjinZhengEntity zheng)return (XuanjinRules.stage(zheng.seal()).realmStage()+2)/3;
        if(target instanceof CultivationBestiaryBeastEntity beast)return switch(beast.kind()) {
            case SPIRIT_DEVOURING_RAT,IRON_SCALE_SCORPION -> 1;
            case WIND_FEATHER_CLIFF_DEER,STONE_DEVOURING_TORTOISE -> 2;
            case EMBER_FEATHER_FIRE_OWL -> 3;
        };
        return -1;
    }
    public static String name(int tier) {
        return switch(tier) {
            case 0 -> "Mortal";
            case 1 -> CultivationLevels.REALM_LIANQI;
            case 2 -> CultivationLevels.REALM_ZHUJI;
            case 3 -> CultivationLevels.REALM_JINDAN;
            case 4 -> CultivationLevels.REALM_YUANYING;
            case 5 -> CultivationLevels.REALM_HUASHEN;
            case 6 -> CultivationLevels.REALM_LIANXU;
            case 7 -> CultivationLevels.REALM_HETI;
            case 8 -> CultivationLevels.REALM_DACHENG;
            case 9 -> CultivationLevels.REALM_DUJIE;
            default -> "Extreme / special";
        };
    }
}
