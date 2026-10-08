package com.example.immortal_cultivation_mod.spell;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.combat.CombatStateManager;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.entity.CultivationNpc;
import com.example.immortal_cultivation_mod.entity.NpcProfile;
import com.example.immortal_cultivation_mod.entity.SpellImpactParticles;
import com.example.immortal_cultivation_mod.particle.ModParticles;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Ground-connection passive for 玄岳碎锋诀. */
public final class XuanyueSuifengJue {
    private static final String LAST_GROUND_CONTACT =
            "immortal_cultivation_mod:xuanyue_last_ground_contact";
    private static final ResourceLocation KNOCKBACK_MODIFIER =
            ResourceLocation.fromNamespaceAndPath(
                    ImmortalCultivationMod.MODID, "xuanyue_grounded_knockback");

    private XuanyueSuifengJue() {
    }

    public static void tick(ServerPlayer player) {
        tickLiving(player);
    }

    public static void tickNpc(PathfinderMob mob) {
        tickLiving(mob);
    }

    private static void tickLiving(LivingEntity entity) {
        if (!(entity.level() instanceof net.minecraft.server.level.ServerLevel level)) return;
        if (!hasMethod(entity)) {
            updateKnockback(entity, 0.0D);
            return;
        }
        if (touchingSolidGround(entity)) {
            entity.getPersistentData().putLong(
                    LAST_GROUND_CONTACT, level.getGameTime());
        }
        updateKnockback(entity, isGroundConnected(entity) ? 0.25D : 0.0D);
        if (isGroundConnected(entity) && entity.tickCount % 12 == 0) {
            level.sendParticles(
                    entity.tickCount % 24 == 0
                            ? ModParticles.METHOD_QI_METAL.get()
                            : ModParticles.METHOD_QI_EARTH.get(),
                    entity.getX(), entity.getY() + 0.08D, entity.getZ(),
                    2, 0.34D, 0.04D, 0.34D, 0.012D);
        }
    }

    public static float adjustIncomingDamage(
            LivingEntity target, DamageSource source, float amount) {
        if (amount <= 0.0F || !hasMethod(target) || !isGroundConnected(target)) {
            return amount;
        }
        if (target.level() instanceof ServerLevel level
                && amount >= target.getMaxHealth() * 0.08F) {
            SpellImpactParticles.shockRing(level,
                    target.position().add(0.0D, 0.06D, 0.0D),
                    isAtLeast(target, CultivationLevels.REALM_JINDAN
                            + CultivationLevels.STAGE_EARLY)
                            ? ModParticles.METHOD_QI_METAL.get()
                            : ModParticles.METHOD_QI_EARTH.get(),
                    0.62D, 30, 0.10D);
            level.sendParticles(ModParticles.METHOD_QI_EARTH.get(),
                    target.getX(), target.getY() + 0.08D, target.getZ(),
                    12, 0.46D, 0.05D, 0.46D, 0.08D);
        }
        return amount * (isAtLeast(target,
                CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_EARLY)
                ? 0.85F : 0.90F);
    }

    public static float armorPenetration(Entity attacker) {
        return attacker instanceof LivingEntity living
                && hasMethod(living)
                && isGroundConnected(living)
                && isAtLeast(living,
                CultivationLevels.REALM_YUANYING + CultivationLevels.STAGE_EARLY)
                ? 0.10F : 0.0F;
    }

    public static void onMeleeDamageApplied(
            LivingEntity attacker, LivingEntity target, DamageSource source, float damage) {
        if (damage <= 0.0F || !hasMethod(attacker) || !isGroundConnected(attacker)
                || !CombatStateManager.isHeavyAttack(source)) {
            return;
        }
        target.addEffect(new MobEffectInstance(
                ModEffects.DAZE,
                isAtLeast(attacker, CultivationLevels.REALM_JINDAN
                        + CultivationLevels.STAGE_EARLY) ? 14 : 8,
                0, false, true, true), attacker);
        if (attacker.level() instanceof net.minecraft.server.level.ServerLevel level) {
            level.sendParticles(ModParticles.METHOD_QI_EARTH.get(),
                    target.getX(), target.getY() + target.getBbHeight() * 0.45D, target.getZ(),
                    14, 0.38D, 0.30D, 0.38D, 0.08D);
            SpellImpactParticles.shockRing(level,
                    target.position().add(0.0D, 0.06D, 0.0D),
                    ModParticles.METHOD_QI_EARTH.get(), 0.55D,
                    isAtLeast(attacker, CultivationLevels.REALM_JINDAN
                            + CultivationLevels.STAGE_EARLY) ? 36 : 24, 0.12D);
        }
    }

    public static boolean hasMethod(LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            return CultivationMethods.isXuanyueSuifengJue(
                    ModAttachments.getData(player).activeCultivationMethod());
        }
        if (entity instanceof PathfinderMob mob
                && entity instanceof CultivationNpc npc) {
            return CultivationMethods.XUANYUE_SUIFENG_JUE.equals(
                    NpcProfile.methodId(mob, npc.npcArchetype()));
        }
        return false;
    }

    public static boolean isGroundConnected(LivingEntity entity) {
        if (touchingSolidGround(entity)) {
            return true;
        }
        return entity.getPersistentData().contains(LAST_GROUND_CONTACT)
                && isAtLeast(entity,
                CultivationLevels.REALM_HUASHEN + CultivationLevels.STAGE_EARLY)
                && entity.level().getGameTime()
                - entity.getPersistentData().getLong(LAST_GROUND_CONTACT) <= 60L;
    }

    public static boolean isAtLeast(LivingEntity entity, String level) {
        String realm;
        if (entity instanceof ServerPlayer player) {
            realm = ModAttachments.getData(player).cultivationLevel();
        } else if (entity instanceof PathfinderMob mob
                && entity instanceof CultivationNpc npc) {
            realm = NpcProfile.realm(mob, npc.npcArchetype());
        } else {
            return false;
        }
        return CultivationLevels.getStageIndex(realm)
                >= CultivationLevels.getStageIndex(level);
    }

    private static boolean touchingSolidGround(LivingEntity entity) {
        var pos = entity.blockPosition().below();
        return entity.onGround()
                && entity.level().getBlockState(pos)
                .isFaceSturdy(entity.level(), pos, Direction.UP);
    }

    private static void updateKnockback(LivingEntity entity, double amount) {
        AttributeInstance attribute = entity.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (attribute == null) return;
        AttributeModifier current = attribute.getModifier(KNOCKBACK_MODIFIER);
        if (current != null && Math.abs(current.amount() - amount) < 0.0001D) return;
        if (current != null) attribute.removeModifier(KNOCKBACK_MODIFIER);
        if (amount > 0.0D) {
            attribute.addTransientModifier(new AttributeModifier(
                    KNOCKBACK_MODIFIER, amount, AttributeModifier.Operation.ADD_VALUE));
        }
    }
}
