package com.example.immortal_cultivation_mod.config;

import java.util.Locale;

/** Independently movable HUD components; full-screen visual effects are not widgets. */
public enum HudElement {
    QI, METHOD, ZOMBIE, POSTURE, HEAVY_ATTACK, PREPARED_SPELL, SPELL_CHARGE,
    SWORD_CHARGE, SHIELD, QI_RELEASE, GUIDANCE, GUIDANCE_COMPLETE, GUIDANCE_NOTICE,
    RING_DIALOGUE, TOXICITY, SENSE_COMPASS, SENSE_DETAILS, DEBUG_EYE, HEALTH;

    public String id() { return name().toLowerCase(Locale.ROOT); }
    public String translationKey() { return "hud.layout.element." + id(); }
}
