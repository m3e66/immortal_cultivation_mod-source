package com.example.immortal_cultivation_mod.spell;

import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.attachment.SpiritRoots;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.entity.CultivationNpc;
import com.example.immortal_cultivation_mod.entity.NpcProfile;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
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

        } else if (owner instanceof PathfinderMob mob && mob instanceof CultivationNpc npc) {
            damage += (float) mob.getAttributeValue(Attributes.ATTACK_DAMAGE);
            if (spell != null && NpcProfile.spiritRootIds(mob, npc.npcArchetype())
                    .contains(spell.element())) {
                damage *= 2.0F;
            }
        }

        if (owner instanceof LivingEntity living && living.hasEffect(ModEffects.SPELL_DAMAGE_BOOST)) {
            damage *= 1.5F;
        }

        return damage;
    }
}
