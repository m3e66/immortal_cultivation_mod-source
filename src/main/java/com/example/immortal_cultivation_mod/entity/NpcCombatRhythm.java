package com.example.immortal_cultivation_mod.entity;

import java.util.UUID;

/** Short, target-specific exchanges. A miss or a long pause breaks the combination. */
public final class NpcCombatRhythm {
    private UUID target;
    private int hits;
    private long followupUntil;
    private long recoveryUntil;

    public boolean follows(UUID enemy, long now) {
        return enemy.equals(target) && hits > 0 && hits < 3 && now < followupUntil;
    }

    public boolean finisher(UUID enemy, long now) {
        return follows(enemy, now) && hits == 2;
    }

    public boolean recovering(long now) { return now < recoveryUntil; }

    public void strike(UUID enemy, long now, boolean hit) {
        hits = hit ? (follows(enemy, now) ? hits + 1 : 1) : 0;
        target = enemy;
        followupUntil = now + 32;
        if (hits == 3 || !hit) {
            recoveryUntil = now + (hit ? 22 : 12);
            hits = 0;
        }
    }

    public void reset() {
        target = null;
        hits = 0;
        followupUntil = recoveryUntil = 0;
    }
}
