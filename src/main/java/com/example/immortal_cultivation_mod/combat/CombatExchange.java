package com.example.immortal_cultivation_mod.combat;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.UUID;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import com.example.immortal_cultivation_mod.entity.CultivationNpc;

/** Target-specific, confirmed contact chains shared by players and cultivators. */
public final class CombatExchange {
    private record Chain(UUID target, long tick, int hits) {}
    private static final Map<LivingEntity, Chain> CHAINS = new WeakHashMap<>();
    private CombatExchange() {}
    public static boolean direct(DamageSource source) {
        return source.getEntity() instanceof LivingEntity actor
            && (actor instanceof Player || actor instanceof CultivationNpc)
            && source.getDirectEntity() == actor
            && (source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MOB_ATTACK));
    }
    public static double confirm(LivingEntity actor, LivingEntity target, boolean heavy) {
        long now = actor.level().getGameTime();
        Chain old = CHAINS.get(actor);
        boolean follow = old != null && old.target.equals(target.getUUID()) && now >= old.tick && now-old.tick <= 24;
        int hits = follow ? old.hits+1 : 1;
        if (heavy) { CHAINS.remove(actor); return 1.5; }
        CHAINS.put(actor, new Chain(target.getUUID(), now, hits));
        return follow ? .01 : .25;
    }
    public static void reset(LivingEntity actor) { CHAINS.remove(actor); }
    public static void clear() { CHAINS.clear(); }
}
