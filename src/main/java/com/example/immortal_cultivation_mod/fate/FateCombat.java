package com.example.immortal_cultivation_mod.fate;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.combat.CombatStateManager;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.entity.CultivationNpc;
import com.example.immortal_cultivation_mod.entity.NpcProfile;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** These are earned equipment mutations. Current Luck is never consulted during combat. */
@EventBusSubscriber(modid = ImmortalCultivationMod.MODID)
public final class FateCombat {
    private static final ResourceLocation EROSION = ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "fortune_armor_erosion");
    private FateCombat() {}
    private static boolean melee(DamageSource source) {
        return source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MOB_ATTACK);
    }
    public static String mutation(DamageSource source) {
        return melee(source) && source.getEntity() instanceof LivingEntity attacker
                ? FateCrafting.tags(attacker.getMainHandItem()).getString(FateCrafting.MUTATION) : "";
    }
    public static float shieldPiercing(DamageSource source, float damage) {
        return "guan_ling".equals(mutation(source)) ? damage * .10F : 0;
    }
    public static float barrierDamage(DamageSource source, float damage) {
        return damage * switch (mutation(source)) {
            case "po_zhen" -> 1.20F;
            case "lie_ling" -> 1.15F;
            case "beng_yue" -> 1.10F;
            default -> 1.0F;
        };
    }
    private static boolean structure(LivingEntity target) {
        String name = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).getPath();
        return name.contains("formation") || name.contains("ritual") || name.contains("glyph") || name.contains("guardian");
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void incoming(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker) || event.getEntity().level().isClientSide) return;
        String trait = mutation(event.getSource());
        LivingEntity target = event.getEntity();
        float factor = 1;
        long now = target.level().getGameTime();
        var history = LuckSystem.data(attacker);
        boolean heavy = attacker instanceof ServerPlayer player && CombatStateManager.isHeavyAttackInProgress(player);
        switch (trait) {
            case "feng_rui" -> { if (target.getArmorValue() > 0) erosion(target, .08, now + 2); }
            case "lie_ling" -> { if (structure(target) || target instanceof ServerPlayer player
                    && com.example.immortal_cultivation_mod.spell.PlayerShieldManager.totalAmount(player) > 0) factor = 1.15F; }
            case "duan_shi" -> { if (target instanceof CultivationNpc npc && npc.npcCastingTicks() > 0
                    || target instanceof ServerPlayer player && (CombatStateManager.isPreparingSpell(player)
                    || CombatStateManager.isPreparingHeavyAttack(player) || player.getVehicle() != null)) factor = 1.12F; }
            case "zhui_ming" -> { if (target.getHealth() < target.getMaxHealth() * .25F) factor = 1.10F; }
            case "zhan_hun" -> { String id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).getPath();
                if (id.contains("soul") || id.contains("ghost") || id.contains("yuanshen")) factor = 1.15F; }
            case "mie_xie" -> { if (target.getType().is(net.minecraft.tags.EntityTypeTags.UNDEAD)
                    || target instanceof CultivationNpc npc && npc.npcIdentity().contains("demonic")) factor = 1.12F; }
            case "po_zhen" -> { if (structure(target)) factor = 1.20F; }
            case "beng_yue" -> { if (target.getBbWidth() >= 1.5F || target.getArmorValue() >= 15 || structure(target)) factor = 1.10F; }
            case "lian_feng" -> { if (history.hasUUID("ComboTarget") && history.getUUID("ComboTarget").equals(target.getUUID())
                    && now - history.getLong("LastWeaponHit") <= 40) factor += Math.min(.10F, history.getInt("ComboHits") * .02F); }
            case "xu_shi" -> { if (now - history.getLong("LastWeaponHit") >= 100) factor = 1.12F; }
        }
        event.setAmount(event.getAmount() * factor);
    }
    private static void erosion(LivingEntity target, double strength, long until) {
        var armor = target.getAttribute(Attributes.ARMOR);
        if (armor == null) return;
        armor.removeModifier(EROSION);
        armor.addTransientModifier(new AttributeModifier(EROSION, -strength, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        LuckSystem.data(target).putLong("ErosionUntil", until);
    }
    @SubscribeEvent
    public static void afterHit(LivingDamageEvent.Post event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker) || event.getNewDamage() <= 0
                || !(event.getEntity().level() instanceof ServerLevel level)) return;
        String trait = mutation(event.getSource());
        if (trait.isBlank()) return;
        LivingEntity target = event.getEntity();
        long now = level.getGameTime();
        var history = LuckSystem.data(attacker);
        boolean same = history.hasUUID("ComboTarget") && history.getUUID("ComboTarget").equals(target.getUUID())
                && now - history.getLong("LastWeaponHit") <= 40;
        history.putInt("ComboHits", same ? Math.min(5, history.getInt("ComboHits") + 1) : 1);
        long previousHit = history.getLong("LastWeaponHit");
        history.putLong("LastWeaponHit", now);
        history.putUUID("ComboTarget", target.getUUID());
        switch (trait) {
            case "po_jia" -> erosion(target, Math.min(.10, history.getInt("ComboHits") * .02), now + 100);
            case "zhen_mai" -> { if (attacker.getRandom().nextFloat() < .06F) target.addEffect(new MobEffectInstance(ModEffects.FROZEN_QI, 20)); }
            case "shi_xue" -> attacker.heal(Math.min(attacker.getMaxHealth() * .005F, event.getNewDamage() * .01F));
            case "yin_ling" -> {
                int take = 0;
                if (target instanceof ServerPlayer player) {
                    var data = ModAttachments.getData(player); take = Math.min(10, data.qi());
                    ModAttachments.setData(player, data.withQi(data.qi() - take));
                } else if (target instanceof CultivationNpc npc) {
                    take = Math.min(10, npc.npcQi()); NpcProfile.spendQi(npc.asNpcMob(), npc.npcArchetype(), take);
                }
                if (take > 0 && attacker instanceof ServerPlayer player) {
                    var data = ModAttachments.getData(player);
                    int max = data.maxQi();
                    ModAttachments.setData(player, data.withQi(Math.min(max, data.qi() + take)));
                }
            }
            case "po_fa" -> {
                if (attacker.getRandom().nextFloat() < .06F) for (var effect : java.util.List.copyOf(target.getActiveEffects())) {
                    if (effect.getEffect().value().getCategory() == MobEffectCategory.BENEFICIAL && !effect.isInfiniteDuration()) {
                        target.removeEffect(effect.getEffect());
                        target.addEffect(new MobEffectInstance(effect.getEffect(), Math.max(1, effect.getDuration() / 2), effect.getAmplifier())); break;
                    }
                }
            }
            case "zhen_shan" -> { if (attacker instanceof ServerPlayer player && CombatStateManager.isHeavyAttackInProgress(player))
                target.knockback(.5, attacker.getX() - target.getX(), attacker.getZ() - target.getZ()); }
            case "can_ying" -> { if (now - previousHit < 12) level.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,
                    attacker.getX(), attacker.getY() + 1, attacker.getZ(), 8, .25, .6, .25, .02); }
            case "ling_bao" -> {
                boolean heavy = attacker instanceof ServerPlayer player && CombatStateManager.isHeavyAttackInProgress(player);
                if ((heavy || !attacker.onGround()) && attacker.getRandom().nextFloat() < .05F) {
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION, target.getX(), target.getY() + .7, target.getZ(), 1, 0, 0, 0, 0);
                    for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(1.5),
                            e -> e != attacker && e != target && !attacker.isAlliedTo(e)))
                        nearby.hurt(attacker.damageSources().indirectMagic(attacker, attacker), event.getNewDamage() * .05F);
                }
            }
        }
    }
    @SubscribeEvent
    public static void restoreArmor(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity target) || target.level().isClientSide) return;
        var tag = target.getPersistentData().getCompound(LuckSystem.DATA);
        if (tag.contains("ErosionUntil") && target.level().getGameTime() >= tag.getLong("ErosionUntil")) {
            var armor = target.getAttribute(Attributes.ARMOR);
            if (armor != null) armor.removeModifier(EROSION);
            tag.remove("ErosionUntil");
        }
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void kill(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player && "hui_feng".equals(mutation(event.getSource())))
            com.example.immortal_cultivation_mod.network.ModPayloads.ModPayloadsHandler.reduceAttackCooldowns(player, 10);
    }
}
