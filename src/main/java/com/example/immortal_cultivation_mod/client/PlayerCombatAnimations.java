package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.combat.CombatAnimationLayers;
import dev.kosmx.playerAnim.api.layered.AnimationStack;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Player Animator owns the state; the identity set only tracks instances for explicit cleanup.
 * Entity.equals/hashCode use the network ID, which changes after construction and can be reused
 * by a replacement player. Even a WeakHashMap keyed by player can therefore return a layer
 * attached to the previous player's stack. Keep layers in the player's associated data instead.
 */
public final class PlayerCombatAnimations {
    private static final ResourceLocation KEY = ResourceLocation.fromNamespaceAndPath(
            ImmortalCultivationMod.MODID, "combat_layers");
    private static final Set<AbstractClientPlayer> PLAYERS = Collections.newSetFromMap(new IdentityHashMap<>());

    private PlayerCombatAnimations() {}

    public static void register(AbstractClientPlayer player, AnimationStack stack) {
        var data = PlayerAnimationAccess.getPlayerAssociatedData(player);
        var state = data.get(KEY) instanceof CombatAnimationLayers layers ? layers : new CombatAnimationLayers();
        state.attach(stack);
        data.set(KEY, state);
        PLAYERS.add(player);
    }

    public static CombatAnimationLayers find(AbstractClientPlayer player) {
        return PlayerAnimationAccess.getPlayerAssociatedData(player).get(KEY) instanceof CombatAnimationLayers state
                ? state : null;
    }

    public static CombatAnimationLayers get(AbstractClientPlayer player) {
        var state = find(player);
        if (state == null) {
            register(player, PlayerAnimationAccess.getPlayerAnimLayer(player));
            state = find(player);
        }
        return state;
    }

    public static Set<AbstractClientPlayer> players() {
        var result = Collections.newSetFromMap(new IdentityHashMap<AbstractClientPlayer, Boolean>());
        result.addAll(PLAYERS);
        return result;
    }

    public static void clean(ClientLevel level) {
        PLAYERS.removeIf(player -> {
            if (level != null && player.level() == level && !player.isRemoved()) return false;
            var state = find(player);
            if (state != null) state.detach();
            PlayerAnimationAccess.getPlayerAssociatedData(player).set(KEY, null);
            return true;
        });
    }
}
