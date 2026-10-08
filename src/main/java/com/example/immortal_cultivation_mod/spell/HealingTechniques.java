package com.example.immortal_cultivation_mod.spell;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.entity.NpcProfile;
import com.example.immortal_cultivation_mod.entity.SpellTriggerProjectileEntity;
import com.example.immortal_cultivation_mod.particle.ModParticles;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Actor-neutral execution for the focused healing technique set. */
public final class HealingTechniques {
    private static final int STABILIZED_DURATION = 20 * 30;
    private static final Consumer<Entity> NO_PROJECTILE_HOOK = ignored -> { };
    private static final Set<String> SPELLS = Set.of(
            ModSpells.MULING_SHENGXI,
            ModSpells.XUE_GUIYUAN_SHU,
            ModSpells.SHENGXI_BAOFA);

    private HealingTechniques() {
    }

    public static boolean isTechnique(String spellId) {
        return SPELLS.contains(ModSpells.normalizeId(spellId));
    }

    public static boolean canCast(LivingEntity caster, LivingEntity suppliedTarget, String spellId) {
        String normalized = ModSpells.normalizeId(spellId);
        if (!SPELLS.contains(normalized) || !caster.isAlive()) {
            return false;
        }
        if (!ModSpells.SHENGXI_BAOFA.equals(normalized)
                && caster instanceof Player && suppliedTarget == null) {
            return true;
        }
        LivingEntity target = validRecipient(caster, suppliedTarget) ? suppliedTarget : caster;
        return switch (normalized) {
            case ModSpells.MULING_SHENGXI -> target.getHealth() < target.getMaxHealth()
                    && !target.hasEffect(ModEffects.MULING_SHENGXI);
            case ModSpells.XUE_GUIYUAN_SHU -> (target.getHealth() < target.getMaxHealth()
                    || target.hasEffect(com.example.immortal_cultivation_mod.magicritual.effect.ModEffects.BLEEDING))
                    && !target.hasEffect(ModEffects.XUE_GUIYUAN_RECOVERY);
            case ModSpells.SHENGXI_BAOFA -> caster.getHealth() < caster.getMaxHealth();
            default -> false;
        };
    }

    public static boolean cast(LivingEntity caster, LivingEntity suppliedTarget, String spellId) {
        return cast(caster, suppliedTarget, spellId, NO_PROJECTILE_HOOK);
    }

    public static boolean cast(
            LivingEntity caster,
            LivingEntity suppliedTarget,
            String spellId,
            Consumer<Entity> projectileHook) {
        boolean selfCast = caster instanceof Player player && player.isShiftKeyDown();
        return cast(caster, suppliedTarget, spellId, projectileHook, selfCast);
    }

    public static boolean cast(
            LivingEntity caster,
            LivingEntity suppliedTarget,
            String spellId,
            Consumer<Entity> projectileHook,
            boolean selfCast) {
        if (!(caster.level() instanceof ServerLevel level)) {
            return false;
        }
        String normalized = ModSpells.normalizeId(spellId);
        LivingEntity requestedTarget = selfCast ? caster : suppliedTarget;
        if (!canCast(caster, requestedTarget, normalized)) {
            return false;
        }
        if (ModSpells.SHENGXI_BAOFA.equals(normalized)) {
            return castShengxiBurst(level, caster);
        }
        if (caster instanceof Player && selfCast
                || !(caster instanceof Player) && !validRecipient(caster, suppliedTarget)) {
            return activateHealing(level, caster, normalized, caster);
        }
        LivingEntity target = validRecipient(caster, suppliedTarget) ? suppliedTarget : null;
        Vec3 direction = target == null
                ? caster.getLookAngle().normalize()
                : target.getBoundingBox().getCenter().subtract(caster.getEyePosition()).normalize();
        SpellTriggerProjectileEntity projectile = new SpellTriggerProjectileEntity(
                level, caster, normalized, SpellCharge.NORMAL);
        Vec3 start = caster.getEyePosition().add(direction.scale(0.55D));
        projectile.setPos(start.x, start.y, start.z);
        projectile.shoot(direction.x, direction.y, direction.z, 2.4F, 0.0F);
        projectileHook.accept(projectile);
        return level.addFreshEntity(projectile);
    }

    public static boolean activateTriggeredSpell(
            ServerLevel level,
            LivingEntity caster,
            String spellId,
            LivingEntity hitTarget) {
        String normalized = ModSpells.normalizeId(spellId);
        return validRecipient(caster, hitTarget)
                && activateHealing(level, caster, normalized, hitTarget);
    }

    public static boolean canProjectileHit(LivingEntity caster, LivingEntity target) {
        return validRecipient(caster, target);
    }

    private static boolean activateHealing(
            ServerLevel level,
            LivingEntity caster,
            String spellId,
            LivingEntity target) {
        return switch (spellId) {
            case ModSpells.MULING_SHENGXI -> target.getHealth() < target.getMaxHealth()
                    && !target.hasEffect(ModEffects.MULING_SHENGXI)
                    && castMulingShengxi(level, caster, target);
            case ModSpells.XUE_GUIYUAN_SHU -> (target.getHealth() < target.getMaxHealth()
                    || target.hasEffect(com.example.immortal_cultivation_mod.magicritual.effect.ModEffects.BLEEDING))
                    && !target.hasEffect(ModEffects.XUE_GUIYUAN_RECOVERY)
                    && castXueGuiyuan(level, caster, target);
            default -> false;
        };
    }

    private static boolean castMulingShengxi(
            ServerLevel level, LivingEntity caster, LivingEntity target) {
        float multiplier = ChangqingJue.healingMultiplier(caster);
        int amplifier = healingAmplifier(multiplier);
        float missingBefore = target.getMaxHealth() - target.getHealth();
        boolean applied = target.addEffect(new MobEffectInstance(
                ModEffects.MULING_SHENGXI, 20 * 8, amplifier, false, true, true), caster);
        if (applied) {
            ChangqingJue.onHealingOther(caster, target,
                    Math.min(missingBefore, 140.0F * multiplier));
            emitLifeBurst(level, caster, target, 34);
        }
        return applied;
    }

    private static boolean castXueGuiyuan(
            ServerLevel level, LivingEntity caster, LivingEntity target) {
        float multiplier = ChangqingJue.healingMultiplier(caster);
        int amplifier = healingAmplifier(multiplier);
        float missingBefore = target.getMaxHealth() - target.getHealth();
        target.removeEffect(com.example.immortal_cultivation_mod.magicritual.effect.ModEffects.BLEEDING);
        boolean critical = target.getHealth() < target.getMaxHealth() * 0.05F;
        if (critical) {
            target.addEffect(new MobEffectInstance(
                    ModEffects.STABILIZED, STABILIZED_DURATION, 0, false, true, true), caster);
            if (caster instanceof ServerPlayer player) {
                player.sendSystemMessage(Component.translatable(
                        "message." + ImmortalCultivationMod.MODID + ".xue_guiyuan_stabilized"));
            }
        }
        target.heal(120.0F * multiplier);
        target.addEffect(new MobEffectInstance(
                ModEffects.XUE_GUIYUAN_RECOVERY, 20 * 5, amplifier, false, true, true), caster);
        ChangqingJue.onHealingOther(caster, target,
                Math.min(missingBefore, 220.0F * multiplier));
        emitBloodWoodBurst(level, caster, target, 52);
        return true;
    }

    private static boolean castShengxiBurst(ServerLevel level, LivingEntity caster) {
        float missing = Math.max(0.0F, caster.getMaxHealth() - caster.getHealth());
        float amount = Mth.clamp(missing * 0.35F, 100.0F, 250.0F)
                * ChangqingJue.healingMultiplier(caster);
        caster.heal(amount);
        emitLifeBurst(level, caster, caster, 70);
        return true;
    }

    private static int healingAmplifier(float multiplier) {
        return multiplier >= 1.25F ? 2 : multiplier > 1.001F ? 1 : 0;
    }

    private static boolean validRecipient(LivingEntity caster, Entity target) {
        if (!(target instanceof LivingEntity living) || !living.isAlive()
                || living.level() != caster.level() || living == caster) {
            return false;
        }
        return caster instanceof Player || NpcProfile.areCombatAllies(caster, living);
    }

    private static void emitLifeBurst(
            ServerLevel level, LivingEntity caster, LivingEntity target, int count) {
        level.sendParticles(ModParticles.METHOD_QI_WOOD.get(),
                target.getX(), target.getY() + target.getBbHeight() * 0.55D, target.getZ(),
                count, 0.8D, target.getBbHeight() * 0.45D, 0.8D, 0.09D);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                target.getX(), target.getY() + target.getBbHeight() * 0.45D, target.getZ(),
                Math.max(6, count / 5), 0.55D, 0.55D, 0.55D, 0.03D);
        play(level, caster, target, 1.25F);
    }

    private static void emitBloodWoodBurst(
            ServerLevel level, LivingEntity caster, LivingEntity target, int count) {
        level.sendParticles(ModParticles.METHOD_QI_BLOOD.get(),
                target.getX(), target.getY() + target.getBbHeight() * 0.55D, target.getZ(),
                count / 2, 0.7D, target.getBbHeight() * 0.4D, 0.7D, 0.07D);
        level.sendParticles(ModParticles.METHOD_QI_WOOD.get(),
                target.getX(), target.getY() + target.getBbHeight() * 0.55D, target.getZ(),
                count / 2, 0.7D, target.getBbHeight() * 0.4D, 0.7D, 0.07D);
        play(level, caster, target, 0.85F);
    }

    private static void play(
            ServerLevel level, LivingEntity caster, LivingEntity target, float pitch) {
        level.playSound(null, target.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                caster instanceof Player ? SoundSource.PLAYERS : SoundSource.HOSTILE,
                1.0F, pitch);
    }
}
