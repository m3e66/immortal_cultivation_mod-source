package com.example.immortal_cultivation_mod.spell;

import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Stronger self-cast tiers of 回春术, shared by players and NPCs. */
public final class HealingTechniques {
    private HealingTechniques() {}

    public static float realmHealingScale(int stageIndex) {
        // Stage indices are 1-based, with three small stages in every major realm.
        // 回春术 is 炼气中期 (index 2), the 1.00 baseline.
        int stage = Math.max(1, stageIndex) - 1;
        int majorDifference = stage / 3;
        int minorDifference = stage % 3 - 1;
        return 1.0F + majorDifference * 0.10F + minorDifference * 0.01F;
    }

    public static boolean isTechnique(String spellId) {
        String id = ModSpells.normalizeId(spellId);
        return ModSpells.MULING_SHENGXI.equals(id) || ModSpells.XUE_GUIYUAN_SHU.equals(id)
                || ModSpells.SHENGXI_BAOFA.equals(id);
    }

    public static boolean canCast(LivingEntity caster, LivingEntity suppliedTarget, String spellId) {
        return isTechnique(spellId) && CultivatorSpellExecution.canApplyRegeneration(caster);
    }

    public static boolean cast(LivingEntity caster, LivingEntity suppliedTarget, String spellId) {
        if (!(caster.level() instanceof ServerLevel) || !canCast(caster, suppliedTarget, spellId)) {
            return false;
        }
        return CultivatorSpellExecution.applyRegeneration(caster, spellId);
    }

    public static boolean cast(LivingEntity caster, LivingEntity suppliedTarget, String spellId,
                               Consumer<Entity> projectileHook) {
        return cast(caster, suppliedTarget, spellId);
    }

    public static boolean cast(LivingEntity caster, LivingEntity suppliedTarget, String spellId,
                               Consumer<Entity> projectileHook, boolean selfCast) {
        return cast(caster, suppliedTarget, spellId);
    }

    // Old healing projectiles saved before this change must not heal other entities on load.
    public static boolean activateTriggeredSpell(ServerLevel level, LivingEntity caster,
                                                  String spellId, LivingEntity hitTarget) {
        return false;
    }

    public static boolean canProjectileHit(LivingEntity caster, LivingEntity target) {
        return false;
    }
}
