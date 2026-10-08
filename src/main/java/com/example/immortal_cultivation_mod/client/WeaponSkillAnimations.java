package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.network.WeaponUseAnimations;
import dev.kosmx.playerAnim.api.layered.modifier.FirstPersonModifier;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;

/** Player Animator skill layer, above ordinary swings and the persistent binding pose. */
public final class WeaponSkillAnimations {
    private WeaponSkillAnimations() {}

    public static void play(AbstractClientPlayer player, WeaponUseAnimations.Type type) {
        var id = ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, type.clip);
        var clip = PlayerAnimationRegistry.getAnimation(id);
        if (clip == null) {
            org.slf4j.LoggerFactory.getLogger(WeaponSkillAnimations.class)
                    .warn("Missing Player Animator weapon skill clip: {}", id);
            return;
        }
        var layer = PlayerCombatAnimations.get(player);
        layer.beginAttack();
        layer.setAnimation(null);
        layer.skillMirror.setEnabled(player.getMainArm() == HumanoidArm.LEFT);
        layer.skillFirstPerson.setCurrentFirstPersonConfig(type.bothArms
                ? FirstPersonModifier.FirstPersonConfigEnum.ENABLE_BOTH_ARMS
                : FirstPersonModifier.FirstPersonConfigEnum.ONLY_RIGHT_ARM_AND_ITEM);
        layer.skill.setAnimation(clip.playAnimation());
    }

    public static boolean isPlaying(Player player) {
        if (!(player instanceof AbstractClientPlayer clientPlayer)) return false;
        var layer = PlayerCombatAnimations.find(clientPlayer);
        return layer != null && layer.skill.isActive();
    }

    public static void stop(AbstractClientPlayer player) {
        var layer = PlayerCombatAnimations.find(player);
        if (layer != null) layer.skill.setAnimation(null);
    }
}
