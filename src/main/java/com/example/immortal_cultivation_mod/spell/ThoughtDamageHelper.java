package com.example.immortal_cultivation_mod.spell;

import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** Applies the player's Thought Damage stat to spells that drain Thoughts. */
public final class ThoughtDamageHelper {
    private ThoughtDamageHelper() {
    }

    public static int bonus(Entity source) {
        return source instanceof Player player
                ? Math.max(0, ModAttachments.getData(player).mentalAttack())
                : 0;
    }

    public static int total(Entity source, int baseDamage) {
        return (int)Math.round(total(baseDamage, bonus(source)) * com.example.immortal_cultivation_mod.pagoda.GourdSystem.multiplier(source));
    }

    public static int total(int baseDamage, int bonus) {
        return Math.max(0, baseDamage) + Math.max(0, bonus);
    }
}
