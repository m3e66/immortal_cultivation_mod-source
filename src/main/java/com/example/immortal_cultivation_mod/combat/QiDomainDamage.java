package com.example.immortal_cultivation_mod.combat;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.spell.SpellDamageHelper;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.world.entity.LivingEntity;

/** Shared contact damage and cooldown, including spikes split across several chunk anchors. */
public final class QiDomainDamage {
    public static final int HIT_COOLDOWN = 20;
    private static final Map<LivingEntity, Map<UUID, Long>> HITS = new WeakHashMap<>();
    private static final ThreadLocal<net.minecraft.world.damagesource.DamageSource> CONTACT_SOURCE=new ThreadLocal<>();
    public static boolean isContactDamage(net.minecraft.world.damagesource.DamageSource source){return CONTACT_SOURCE.get()==source;}

    public static float amount(LivingEntity owner, float power) {
        float base = Math.max(2, CultivationLevels.getLevelDef(QiDomainSystem.level(owner)).maxHp() * .02F);
        float amount = SpellDamageHelper.damage(owner, base, null) * power;
        return Float.isFinite(amount) ? Math.max(0, amount) : 0;
    }

    public static boolean hurt(LivingEntity owner, LivingEntity target, float amount, boolean physical) {
        if (owner.level().isClientSide || !owner.isAlive() || owner.isRemoved() || owner.level() != target.level()
                || !Float.isFinite(amount) || amount <= 0 || !QiDomainSystem.canDomainAffect(owner, target)) return false;
        long now = owner.level().getGameTime();
        var hits = HITS.computeIfAbsent(owner, ignored -> new HashMap<>());
        hits.entrySet().removeIf(entry -> entry.getValue() <= now);
        if (hits.getOrDefault(target.getUUID(), Long.MIN_VALUE) > now) return false;
        var source = physical ? owner.damageSources().mobAttack(owner) : owner.damageSources().indirectMagic(owner, owner);
        var previous=CONTACT_SOURCE.get();CONTACT_SOURCE.set(source);
        boolean accepted;
        try{accepted=target.hurt(source,amount);}
        finally{if(previous==null)CONTACT_SOURCE.remove();else CONTACT_SOURCE.set(previous);}
        if(!accepted)return false;
        hits.put(target.getUUID(), now + HIT_COOLDOWN);
        target.hurtMarked = true;
        return true;
    }

    public static void clear() { HITS.clear(); }
    private QiDomainDamage() {}
}
