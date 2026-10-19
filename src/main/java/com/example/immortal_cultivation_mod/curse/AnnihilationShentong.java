package com.example.immortal_cultivation_mod.curse;

import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.shentong.ShentongRegistry.Definition;
import com.example.immortal_cultivation_mod.shentong.ShentongRegistry.Field;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;

/** Ability-specific adapter; the editor and its packets contain no curse-specific fields. */
public final class AnnihilationShentong {
    private static final String LABEL = "screen.immortal_cultivation_mod.debug_shentong_";
    private AnnihilationShentong() {}
    public static Definition definition() {
        return new Definition(AnnihilationCurse.ABILITY, "spell.immortal_cultivation_mod." + AnnihilationCurse.ABILITY,
                AnnihilationCurse::owned, AnnihilationShentong::grant, AnnihilationShentong::remove,
                AnnihilationCurse::sync, List.of(
                new Field("cooldown", LABEL + "cooldown", 0, AnnihilationCurse.RESCUE_COOLDOWN / 20, false,
                        p -> (AnnihilationCurse.cooldown(p) + 19) / 20,
                        (p,v) -> AnnihilationCurse.state(p).putInt("Cooldown", v * 20)),
                new Field("coverage", LABEL + "coverage", 0, 100, false,
                        p -> Math.round(AnnihilationCurse.coverage(p) * 100), (p,v) -> {
                    var state = AnnihilationCurse.state(p);
                    state.putFloat("Coverage", v / 100F); state.putFloat("Ceiling", v > 0 ? 1 : 0);
                    state.putFloat("Growth", 0); state.putInt("Bloom", 0); state.putInt("Cleanse", 0);
                }),
                new Field("black_hp", LABEL + "black_hp", 0, 100, false,
                        p -> Math.clamp(Math.round(AnnihilationCurse.blackHp(p) * 100 / Math.max(1,p.getMaxHealth())),0,100),
                        (p,v) -> {
                    var state = AnnihilationCurse.state(p);
                    float hp = Math.min(p.getHealth(), p.getMaxHealth() * v / 100F);
                    state.putFloat("BlackHp", hp); state.putFloat("LastHp", p.getHealth());
                    if (hp > 0) state.putBoolean("NoHealing", true);
                }),
                new Field("healing_lock", LABEL + "healing", 0, 1, true,
                        p -> AnnihilationCurse.healingLocked(p) ? 1 : 0, (p,v) -> {
                    var state = AnnihilationCurse.state(p);
                    state.putBoolean("NoHealing", v != 0);
                    if (v == 0) state.putFloat("BlackHp", 0);
                })));
    }
    private static void grant(ServerPlayer target) {
        AnnihilationCurse.acquire(target);
        var data = ModAttachments.getData(target);
        if (!data.knownSpells().contains(AnnihilationCurse.ABILITY))
            ModAttachments.setData(target, data.withKnownSpells(java.util.stream.Stream.concat(
                    data.knownSpells().stream(), java.util.stream.Stream.of(AnnihilationCurse.ABILITY)).toList()));
    }
    private static void remove(ServerPlayer target) {
        target.getPersistentData().remove(AnnihilationCurse.TAG);
        var data = ModAttachments.getData(target);
        ModAttachments.setData(target, data.withKnownSpells(data.knownSpells().stream()
                .filter(id -> !AnnihilationCurse.ABILITY.equals(id)).toList()));
    }
}
