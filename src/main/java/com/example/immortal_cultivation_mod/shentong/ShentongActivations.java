package com.example.immortal_cultivation_mod.shentong;

import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.network.ShentongPayloads;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/** Presentation follows real server activations; inspection, acquisition and toggling off are silent. */
public final class ShentongActivations {
    public static final double VIEW_RADIUS=32;
    private static final Set<String> INNATE=Set.copyOf(ModSpells.innateSpellIds());
    private ShentongActivations() {}
    public static boolean isAbility(String id) {
        id=ModSpells.normalizeId(id);
        var spell=ModSpells.get(id);
        return INNATE.contains(id)||ShentongRegistry.get(id)!=null||spell!=null&&"innate".equals(spell.tier());
    }
    public static void announce(ServerPlayer player,String id) {
        id=ModSpells.normalizeId(id);if(!isAbility(id))return;
        if(ModSpells.WEIYA.equals(id)&&!player.hasEffect(ModEffects.WEIYA)
                ||ModSpells.FENGYA.equals(id)&&!player.hasEffect(ModEffects.FENGYA)
                ||ModSpells.SUISHUANG_LINGXIAO.equals(id)&&!player.hasEffect(ModEffects.FROST_FLIGHT))return;
        PacketDistributor.sendToPlayersNear(player.serverLevel(),null,player.getX(),player.getY(),player.getZ(),VIEW_RADIUS,
                new ShentongPayloads.Activated(player.getUUID(),id));
    }
}
