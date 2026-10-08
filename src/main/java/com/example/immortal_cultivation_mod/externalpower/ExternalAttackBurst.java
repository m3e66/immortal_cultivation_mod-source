package com.example.immortal_cultivation_mod.externalpower;

import java.util.UUID;

/** Observation accumulator only. Never cancels hits, changes cooldowns, or limits damage. */
public record ExternalAttackBurst(long firstTick, long lastTick, int hits, double totalDamage) {
    public record Key(ExternalAttackProfile.Key attack, UUID attacker, UUID direct) {}
    public static ExternalAttackBurst observe(ExternalAttackBurst previous, long now, double damage, int window) {
        if(previous==null || now<previous.lastTick || now-previous.lastTick>window || now-previous.firstTick>Math.min(200,window*4))
            return new ExternalAttackBurst(now,now,1,Math.max(0,damage));
        return new ExternalAttackBurst(previous.firstTick,now,previous.hits+1,previous.totalDamage+Math.max(0,damage));
    }
    public double hitsPerSecond(){return hits*20D/Math.max(1,lastTick-firstTick+1);}
}
