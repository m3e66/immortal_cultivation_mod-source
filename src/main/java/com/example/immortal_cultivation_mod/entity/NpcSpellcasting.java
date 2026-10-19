package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.SpiritRoots;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.spell.CultivatorSpellExecution;
import com.example.immortal_cultivation_mod.spell.ChangqingJue;
import com.example.immortal_cultivation_mod.spell.ChangqingTechniques;
import com.example.immortal_cultivation_mod.spell.DielangShield;
import com.example.immortal_cultivation_mod.spell.ElementalTechniques;
import com.example.immortal_cultivation_mod.spell.HealingTechniques;
import com.example.immortal_cultivation_mod.spell.HanlanTechniques;
import com.example.immortal_cultivation_mod.spell.HanlanNingyuanJing;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import com.example.immortal_cultivation_mod.spell.XuanyueTechniques;
import com.example.immortal_cultivation_mod.spell.XuanyueSuifengJue;
import com.example.immortal_cultivation_mod.spell.WindStep;
import com.example.immortal_cultivation_mod.spell.YufengJue;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;

/**
 * NPC-facing validator for the real registered spell implementations.
 * It owns learned/realm/range/LOS/qi checks; effects are delegated to the
 * same actor-neutral execution methods used by player spell handling.
 */
public final class NpcSpellcasting {
    private static final int LIGHT_BEAM_SHOT_COST = 30;
    private static final int LINGBENG_RELEASE_COST = 85;
    private static final Map<String, SpellProfile> PROFILES = createProfiles();

    private NpcSpellcasting() {
    }

    public enum Role {
        ATTACK,
        CONTROL,
        DEFENSE,
        HEAL,
        RECOVERY,
        MOVEMENT
    }

    public record SpellProfile(
            Role role,
            double minRange,
            double maxRange,
            boolean requiresLineOfSight,
            boolean area,
            boolean fast) {
        public boolean inRange(double distance) {
            // maxRange is now only the AI's preferred tactical distance. The
            // spell itself remains valid until its real projectile collides.
            return distance >= minRange;
        }
    }

    public static List<String> movementSpellIds() {
        return PROFILES.entrySet().stream()
                .filter(entry -> entry.getValue().role() == Role.MOVEMENT)
                .map(Map.Entry::getKey)
                .toList();
    }

    public static List<String> attackSpellIds() {
        return PROFILES.entrySet().stream()
                .filter(entry -> entry.getValue().role() == Role.ATTACK
                        || entry.getValue().role() == Role.CONTROL)
                .map(Map.Entry::getKey)
                .toList();
    }

    public static List<String> healingSpellIds() {
        return PROFILES.entrySet().stream()
                .filter(entry -> entry.getValue().role() == Role.HEAL)
                .map(Map.Entry::getKey)
                .toList();
    }

    public static SpellProfile profile(String spellId) {
        return PROFILES.get(ModSpells.normalizeId(spellId));
    }

    /** Real pre-release window. Fast spells remain quick; large area spells telegraph longer. */
    public static int windupTicks(String spellId) {
        SpellProfile profile = profile(spellId);
        if (profile == null) {
            return 8;
        }
        if (profile.role() == Role.MOVEMENT) {
            return 5;
        }
        if (profile.fast()) {
            return 4;
        }
        return profile.area() ? 10 : 7;
    }

    public static boolean isAttackSpell(String spellId) {
        SpellProfile profile = profile(spellId);
        return profile != null && (profile.role() == Role.ATTACK || profile.role() == Role.CONTROL);
    }

    public static boolean isNpcSupportedSpell(String spellId) {
        return PROFILES.containsKey(ModSpells.normalizeId(spellId));
    }

    public static boolean canAttackAtDistance(String spellId, double distance) {
        SpellProfile profile = profile(spellId);
        return profile != null && isAttackSpell(spellId) && profile.inRange(distance);
    }

    public static int qiCost(PathfinderMob caster, String spellId) {
        String normalized = ModSpells.normalizeId(spellId);
        ModSpells.SpellDef spell = ModSpells.get(normalized);
        if (spell == null) {
            return Integer.MAX_VALUE;
        }
        if (ModSpells.LIGHT_BEAM_ATTACK.equals(normalized)) {
            return Math.addExact(spell.qiCost(),
                    LIGHT_BEAM_SHOT_COST * LightBeamProjectileEntity.PROJECTILES_PER_STACK);
        }
        if (ModSpells.DIELANG_SHIELD.equals(normalized)) {
            return DielangShield.nextCost(caster);
        }
        int baseCost = Math.max(0, spell.qiCost());
        float multiplier = 1.0F;
        if (XuanyueSuifengJue.hasMethod(caster)
                && XuanyueSuifengJue.isGroundConnected(caster)
                && SpiritRoots.elementMatches(spell.element(), SpiritRoots.EARTH)) {
            multiplier *= 0.90F;
        }
        if (HanlanNingyuanJing.hasMethod(caster)
                && SpiritRoots.elementMatches(spell.element(), SpiritRoots.ICE)) {
            multiplier *= HanlanTechniques.qiCostMultiplier(caster);
        }
        int cost = baseCost <= 0 ? 0 : Math.max(1, Math.round(baseCost * multiplier));
        if (cost > 0 && caster.hasEffect(ModEffects.SPIRIT_SEAL)) {
            cost = Mth.ceil(cost * 1.30D);
        }
        return cost;
    }

    public static boolean canCast(
            PathfinderMob caster,
            LivingEntity target,
            String spellId) {
        if (caster.level().isClientSide || !caster.isAlive()
                || caster.hasEffect(ModEffects.DAZE)
                || caster.hasEffect(ModEffects.DINGSHEN)
                || caster.hasEffect(ModEffects.FROZEN)
                || caster.hasEffect(ModEffects.FROZEN_QI)) {
            return false;
        }
        String normalized = ModSpells.normalizeId(spellId);
        SpellProfile profile = PROFILES.get(normalized);
        ModSpells.SpellDef spell = ModSpells.get(normalized);
        if (profile == null || spell == null
                || !NpcProfile.knowsSpell(caster, archetype(caster), normalized)
                || !ModSpells.meetsRequirement(NpcProfile.realm(caster, archetype(caster)), spell)) {
            return false;
        }
        if (caster.hasEffect(ModEffects.QI_GATHERING) && !ModSpells.QI_GATHERING.equals(normalized)) {
            return false;
        }
        int cost = qiCost(caster, normalized);
        if (cost == Integer.MAX_VALUE || NpcProfile.qi(caster, archetype(caster)) < cost) {
            return false;
        }
        if (profile.role() == Role.ATTACK || profile.role() == Role.CONTROL) {
            if (target == null || !target.isAlive() || target.level() != caster.level()
                    || NpcProfile.isNonCombatPlayer(target)) {
                return false;
            }
            double distance = Math.sqrt(caster.distanceToSqr(target));
            if (!profile.inRange(distance)
                    || profile.requiresLineOfSight() && !caster.hasLineOfSight(target)) {
                return false;
            }
        }
        if (ModSpells.REGENERATION.equals(normalized)) {
            return caster.getHealth() < caster.getMaxHealth();
        }
        if (HealingTechniques.isTechnique(normalized)) {
            return HealingTechniques.canCast(caster, target, normalized);
        }
        if (XuanyueTechniques.isTechnique(normalized)
                && !com.example.immortal_cultivation_mod.spell.XuanyueSuifengJue
                .hasMethod(caster)) {
            return false;
        }
        if (HanlanTechniques.isTechnique(normalized)
                && !com.example.immortal_cultivation_mod.spell.HanlanNingyuanJing
                .hasMethod(caster)) {
            return false;
        }
        if (ChangqingTechniques.isTechnique(normalized)
                && !ChangqingJue.hasMethod(caster)) {
            return false;
        }
        if (ModSpells.CLEANSE.equals(normalized)) {
            return caster.getActiveEffects().stream()
                    .anyMatch(effect -> effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL);
        }
        if (ModSpells.QI_GATHERING.equals(normalized)) {
            return !caster.hasEffect(ModEffects.QI_GATHERING);
        }
        if (ModSpells.DINGSHEN.equals(normalized)) {
            return DingshenProjectileEntity.canBind(caster, target);
        }
        if (ModSpells.WIND_STEP.equals(normalized)) {
            var effect = caster.getEffect(ModEffects.WIND_STEP);
            return effect == null;
        }
        if (ModSpells.YUFENG_JUE.equals(normalized)) {
            var effect = caster.getEffect(ModEffects.YUFENG_JUE);
            return effect == null && YufengJue.hasFlightSupport(caster);
        }
        return true;
    }

    /** Validates, pays real qi, and runs the real effect. Cooldown is owned by the combat brain. */
    public static boolean cast(PathfinderMob caster, LivingEntity target, String spellId) {
        String normalized = ModSpells.normalizeId(spellId);
        if (!canCast(caster, target, normalized)) {
            return false;
        }
        int cost = qiCost(caster, normalized);
        if (cost > 0 && !NpcProfile.spendQi(caster, archetype(caster), cost)) {
            return false;
        }
        caster.lookAt(target == null ? caster : target, 45.0F, 45.0F);
        Vec3 ribbonCastOrigin = caster.position();
        boolean succeeded = execute(caster, target, normalized);
        if (succeeded) com.example.immortal_cultivation_mod.network.SpellRibbonPayload.cast(caster, normalized, ribbonCastOrigin);
        if (!succeeded && cost > 0) {
            NpcProfile.restoreQi(caster, archetype(caster), cost);
        }
        return succeeded;
    }

    public static boolean releaseLingbeng(PathfinderMob caster, LivingEntity target) {
        if (target == null || !target.isAlive() || NpcProfile.isNonCombatPlayer(target)
                || !caster.hasEffect(ModEffects.LINGBENG)
                || NpcProfile.qi(caster, archetype(caster)) < LINGBENG_RELEASE_COST
                || !caster.hasLineOfSight(target)) {
            return false;
        }
        if (!NpcProfile.spendQi(caster, archetype(caster), LINGBENG_RELEASE_COST)) {
            return false;
        }
        boolean released = CultivatorSpellExecution.releaseLingbeng(caster, target.position());
        if (!released) {
            NpcProfile.restoreQi(caster, archetype(caster), LINGBENG_RELEASE_COST);
        }
        return released;
    }

    public static boolean hasMovementEffect(PathfinderMob caster) {
        return caster.hasEffect(ModEffects.WIND_STEP) || caster.hasEffect(ModEffects.YUFENG_JUE);
    }

    public static boolean hasFlightEffect(PathfinderMob caster) {
        return caster.hasEffect(ModEffects.YUFENG_JUE);
    }

    public static boolean canFly(PathfinderMob caster) {
        return (hasCreativeFlight(caster) || hasFlightEffect(caster)) && NpcFlight.canSustain(caster);
    }

    /** Realm flight is innate, independent of learned movement spells and ground support. */
    public static boolean hasCreativeFlight(PathfinderMob caster) {
        return caster instanceof CultivationNpc
                && CultivationLevels.getRealmIndex(NpcProfile.realm(caster, archetype(caster)))
                >= 3;
    }

    public static final double CREATIVE_FLIGHT_SPEED =
            com.example.immortal_cultivation_mod.spell.JindanCreativeFlight.BASE_TRAVEL_SPEED * 3.0D;

    public static void tickCreativeFlight(PathfinderMob caster, Vec3 destination) {
        caster.setNoGravity(true);
        caster.resetFallDistance();
        Vec3 current = caster.getDeltaMovement();
        Vec3 delta = destination == null ? Vec3.ZERO : destination.subtract(caster.position());
        double distance = delta.length();
        // Begin braking well before arrival. The old distance clamp could erase most
        // of the velocity in one tick when a moving target changed direction.
        Vec3 desired = distance < .025D ? Vec3.ZERO
                : delta.scale(Math.min(CREATIVE_FLIGHT_SPEED, distance * .20D) / distance);
        double acceleration = desired.lengthSqr() < current.lengthSqr() ? .22D : .18D;
        caster.setDeltaMovement(NpcFlight.approachVelocity(current, desired, acceleration));
    }

    public static void tickYufengFlight(
            PathfinderMob caster,
            Vec3 destination,
            double flightSpeed) {
        Vec3 current = caster.getDeltaMovement();
        if (destination == null) {
            NpcFlight.brake(caster);
            return;
        }

        Vec3 horizontal = destination.subtract(caster.position());
        horizontal = new Vec3(horizontal.x, 0.0D, horizontal.z);
        double horizontalDistance = Math.sqrt(horizontal.lengthSqr());
        Vec3 desiredHorizontal = horizontalDistance < 0.001D
                ? Vec3.ZERO
                : horizontal.normalize().scale(Math.min(flightSpeed, horizontalDistance * .15D));
        double desiredY = Mth.clamp((destination.y - caster.getY()) * 0.08D, -0.18D, 0.18D);
        if (caster.horizontalCollision) {
            desiredY = Math.max(desiredY, 0.14D);
        }

        Vec3 nextHorizontal = NpcFlight.approachVelocity(current.multiply(1, 0, 1),
                desiredHorizontal, Math.max(.03D, flightSpeed * .12D));
        double nextY = current.y + Mth.clamp(desiredY - current.y, -.025D, .025D);
        caster.setDeltaMovement(nextHorizontal.x, nextY, nextHorizontal.z);
    }

    /** Compatibility for older callers; NPC cooldown truth now lives in ModSpells. */
    public static int cooldownTicks(String spellId) {
        return ModSpells.cooldownTicks(spellId);
    }

    private static boolean execute(PathfinderMob caster, LivingEntity target, String spellId) {
        if (HealingTechniques.isTechnique(spellId)) {
            return HealingTechniques.cast(caster, target, spellId);
        }
        if (XuanyueTechniques.isTechnique(spellId)) {
            return XuanyueTechniques.cast(caster, target, spellId);
        }
        if (HanlanTechniques.isTechnique(spellId)) {
            return HanlanTechniques.cast(caster, target, spellId);
        }
        if (ChangqingTechniques.isTechnique(spellId)) {
            return ChangqingTechniques.cast(caster, target, spellId);
        }
        if (ElementalTechniques.isTechnique(spellId)) {
            return ElementalTechniques.cast(caster, target, spellId);
        }
        return switch (spellId) {
            case ModSpells.FIREBALL -> CultivatorSpellExecution.fireball(caster, target, 1.0F);
            case ModSpells.LINGBENG -> CultivatorSpellExecution.applyLingbeng(
                    caster,
                    20 * (45 + Math.max(0, CultivationLevels.getRealmIndex(
                            NpcProfile.realm(caster, archetype(caster)))) * 5));
            case ModSpells.REGENERATION -> CultivatorSpellExecution.applyRegeneration(caster);
            case ModSpells.CLEANSE -> CultivatorSpellExecution.applyCleanse(caster);
            case ModSpells.QI_GATHERING -> CultivatorSpellExecution.applyQiGathering(caster);
            case ModSpells.ZHENSHAN_PALM -> CultivatorSpellExecution.zhenshanPalm(caster, 1.0F);
            case ModSpells.LIGHT_BEAM_ATTACK -> CultivatorSpellExecution.lightBeamVolley(caster, target);
            case ModSpells.DIELANG_SHIELD -> DielangShield.activateNext(caster);
            case ModSpells.LINGZHI_BULLET -> CultivatorSpellExecution.lingzhiBullet(caster, target, 1.0F);
            case ModSpells.JINMANG_CHUANSHA -> CultivatorSpellExecution.jinmangChuansha(caster, target);
            case ModSpells.WIND_BLADE -> CultivatorSpellExecution.windBlade(caster, target, 1.0F);
            case ModSpells.WIND_STEP -> WindStep.activateNpc(caster);
            case ModSpells.YUFENG_JUE -> YufengJue.activateNpc(caster);
            case ModSpells.SMOKE_ART -> CultivatorSpellExecution.smokeArt(caster, target, 1.0F);
            case ModSpells.SLIDING_WATER -> CultivatorSpellExecution.slidingWater(caster, target, 1.0F);
            case ModSpells.DINGSHEN -> CultivatorSpellExecution.dingshen(caster, target, 1.0F);
            case ModSpells.YINLEI_JUE -> CultivatorSpellExecution.lightning(caster, target, false, 1.0F);
            case ModSpells.WULEI_ZHENGFA -> CultivatorSpellExecution.lightning(caster, target, true, 1.0F);
            case ModSpells.LIUGUANG_JIANYING -> CultivatorSpellExecution.liuguangJianying(caster, 1.0F);
            case ModSpells.JINHUO_XINLIAO -> CultivatorSpellExecution.jinhuoXinliao(caster, target, 1.0F);
            case ModSpells.SUIGUI_XINGYUN -> CultivatorSpellExecution.suiguiXingyun(caster, target, 1.0F);
            default -> false;
        };
    }

    private static String archetype(PathfinderMob caster) {
        return caster instanceof CultivationNpc npc ? npc.npcArchetype() : NpcProfile.FANREN;
    }

    private static Map<String, SpellProfile> createProfiles() {
        Map<String, SpellProfile> profiles = new LinkedHashMap<>();
        profiles.put(ModSpells.FIREBALL, attack(2.5D, 28.0D, true, false));
        profiles.put(ModSpells.LINGBENG, attack(0.0D, 4.5D, true, true));
        profiles.put(ModSpells.REGENERATION, self(Role.HEAL));
        profiles.put(ModSpells.MULING_SHENGXI, self(Role.HEAL));
        profiles.put(ModSpells.XUE_GUIYUAN_SHU, self(Role.HEAL));
        profiles.put(ModSpells.SHENGXI_BAOFA, self(Role.HEAL));
        profiles.put(ModSpells.CLEANSE, self(Role.DEFENSE));
        profiles.put(ModSpells.QI_GATHERING, self(Role.RECOVERY));
        profiles.put(ModSpells.ZHENSHAN_PALM, attack(2.0D, 12.0D, true, false));
        profiles.put(ModSpells.LIGHT_BEAM_ATTACK, attack(3.0D, 30.0D, false, true));
        profiles.put(ModSpells.DIELANG_SHIELD, self(Role.DEFENSE));
        profiles.put(ModSpells.LINGZHI_BULLET, attack(2.0D, 30.0D, false, true));
        profiles.put(ModSpells.JINMANG_CHUANSHA, attack(2.0D, 24.0D, false, true));
        profiles.put(ModSpells.JINFENG_ZHI, attack(2.0D, 12.0D, false, true));
        profiles.put(ModSpells.WANREN_CHUANXIN, attack(3.0D, 20.0D, false, false));
        profiles.put(ModSpells.JINGJI_TUSHENG, control(1.5D, 10.0D, true));
        profiles.put(ModSpells.WANTENG_JIAOSHA, control(3.0D, 20.0D, true));
        profiles.put(ModSpells.YANBAO_DAN, attack(2.5D, 28.0D, true, true));
        profiles.put(ModSpells.CHIYAN_LONGJUAN, control(4.0D, 20.0D, true));
        profiles.put(ModSpells.TIANYUN_FENCHENG, attack(5.0D, 30.0D, true, false));
        profiles.put(ModSpells.LUOHUA_CHANSHEN, control(2.0D, 12.0D, false));
        profiles.put(ModSpells.BAIHUA_ZANG, control(4.0D, 20.0D, true));
        profiles.put(ModSpells.POKONG_JIANHEN, attack(1.5D, 14.0D, false, true));
        profiles.put(ModSpells.SANJIAN_ZHUIMING, attack(3.0D, 16.0D, false, false));
        profiles.put(ModSpells.DIYAN_QIANG, control(1.5D, 12.0D, true));
        profiles.put(ModSpells.SHANBENG_YIN, attack(4.0D, 24.0D, true, false));
        profiles.put(ModSpells.SHUIREN_LIANZHAN, attack(2.0D, 11.0D, false, true));
        profiles.put(ModSpells.CANGLANG_CHONG, control(2.0D, 14.0D, true));
        profiles.put(ModSpells.LEISHAN, attack(1.0D, 8.0D, true, true));
        profiles.put(ModSpells.WULEI_HONGDING, attack(3.0D, 25.0D, true, false));
        profiles.put(ModSpells.BINGZHUI_YU, attack(3.0D, 20.0D, true, false));
        profiles.put(ModSpells.HANYU_FENGDI, control(1.0D, 7.0D, true));
        profiles.put(ModSpells.YAOGUANG_SHAN, control(0.0D, 6.0D, true));
        profiles.put(ModSpells.TIANGUANG_GUANSHA, attack(3.0D, 25.0D, true, false));
        profiles.put(ModSpells.YINGXI, attack(1.5D, 8.0D, false, true));
        profiles.put(ModSpells.TIANFENG_ZHUI, control(3.0D, 20.0D, true));
        profiles.put(ModSpells.WIND_BLADE, attack(2.0D, 28.0D, false, true));
        profiles.put(ModSpells.WIND_STEP, self(Role.MOVEMENT));
        profiles.put(ModSpells.YUFENG_JUE, self(Role.MOVEMENT));
        profiles.put(ModSpells.SMOKE_ART, control(2.0D, 20.0D, false));
        profiles.put(ModSpells.SLIDING_WATER, control(2.0D, 24.0D, false));
        profiles.put(ModSpells.DINGSHEN, control(2.5D, 26.0D, false));
        profiles.put(ModSpells.YINLEI_JUE, attack(3.0D, 32.0D, false, true));
        profiles.put(ModSpells.WULEI_ZHENGFA, attack(4.0D, 32.0D, true, false));
        profiles.put(ModSpells.LIUGUANG_JIANYING, attack(0.0D, 10.0D, true, false));
        profiles.put(ModSpells.JINHUO_XINLIAO, attack(3.0D, 36.0D, false, false));
        profiles.put(ModSpells.SUIGUI_XINGYUN, attack(10.0D, 48.0D, true, false));
        profiles.put(ModSpells.XUANYUE_BI, control(2.0D, 14.0D, true));
        profiles.put(ModSpells.JINYAN_SHENGFENG, attack(2.0D, 24.0D, true, true));
        profiles.put(ModSpells.SHANSUO_ZHENYU, control(3.0D, 26.0D, true));
        profiles.put(ModSpells.WANFENG_YUEZANG, attack(5.0D, 30.0D, true, false));
        profiles.put(ModSpells.DONGLIU_SUO, control(2.0D, 24.0D, false));
        profiles.put(ModSpells.CANGYUAN_HONGLIU, control(2.0D, 18.0D, true));
        profiles.put(ModSpells.HANYUAN_FENGJIE, control(0.0D, 35.0D, true));
        profiles.put(ModSpells.WANMU_SHENG, self(Role.DEFENSE));
        profiles.put(ModSpells.QINGTENG_FU, control(2.0D, 24.0D, true));
        return Map.copyOf(profiles);
    }

    private static SpellProfile attack(double min, double max, boolean area, boolean fast) {
        return new SpellProfile(Role.ATTACK, min, max, true, area, fast);
    }

    private static SpellProfile control(double min, double max, boolean area) {
        return new SpellProfile(Role.CONTROL, min, max, true, area, true);
    }

    private static SpellProfile self(Role role) {
        return new SpellProfile(role, 0.0D, 0.0D, false, false, true);
    }
}
