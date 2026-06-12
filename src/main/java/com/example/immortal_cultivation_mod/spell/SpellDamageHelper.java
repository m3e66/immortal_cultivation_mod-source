package com.example.immortal_cultivation_mod.spell;

import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.attachment.SpiritRoots;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class SpellDamageHelper {
    public static float damage(Entity owner, float baseDamage, ModSpells.SpellDef spell) {
        float damage = baseDamage;

        if (owner instanceof Player player) {
            var data = ModAttachments.getData(player);

            damage += data.magicAttack();

            if (spell != null) {
                damage *= (float) SpiritRoots.damageMultiplier(data, spell);
            }

            if (player.hasEffect(ModEffects.SPELL_DAMAGE_BOOST)) {
                damage *= 1.5F;
            }
        }

        return damage;
    }
}