package com.example.immortal_cultivation_mod.combat;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.entity.CultivationNpc;
import com.example.immortal_cultivation_mod.entity.PhantomVeilFoxEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** Command-only immunity; ordinary damage and entity cleanup are unaffected. */
public final class RealmKillProtection {
    private RealmKillProtection() {}
    public static boolean protects(Entity entity) {
        String realm;
        if (entity instanceof Player player) realm = ModAttachments.getData(player).cultivationLevel();
        else if (entity instanceof CultivationNpc npc) realm = npc.npcRealm();
        else if (entity instanceof PhantomVeilFoxEntity fox) realm = fox.cultivationRealm();
        else return false;
        return entity.isAlive() && CultivationLevels.getRealmIndex(realm) >= 5;
    }
}
