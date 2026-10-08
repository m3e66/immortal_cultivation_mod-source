package com.example.immortal_cultivation_mod.combat;

import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.layered.AnimationStack;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.FirstPersonModifier;
import dev.kosmx.playerAnim.api.layered.modifier.MirrorModifier;

/** Owned by one player instance, never indexed by Minecraft's mutable/reused entity ID. */
public final class CombatAnimationLayers extends ModifierLayer<IAnimation> {
    public final ModifierLayer<IAnimation> binding = new ModifierLayer<>();
    public final ModifierLayer<IAnimation> skill = new ModifierLayer<>();
    public final FirstPersonModifier firstPerson = firstPerson();
    public final FirstPersonModifier skillFirstPerson = firstPerson();
    public final MirrorModifier skillMirror = new MirrorModifier(false);
    public Boolean blockPose;
    public boolean bindingApplied;
    private AnimationStack stack;

    public CombatAnimationLayers() {
        addModifierLast(firstPerson);
        binding.addModifierLast(firstPerson());
        skill.addModifierLast(skillMirror);
        skill.addModifierLast(skillFirstPerson);
    }

    private static FirstPersonModifier firstPerson() {
        var modifier = new FirstPersonModifier();
        modifier.setCurrentFirstPersonMode(FirstPersonMode.THIRD_PERSON_MODEL);
        modifier.setCurrentFirstPersonConfig(FirstPersonModifier.FirstPersonConfigEnum.ONLY_RIGHT_ARM_AND_ITEM);
        return modifier;
    }

    public void attach(AnimationStack target) {
        if (stack == target) return;
        detach();
        stack = target;
        stack.addAnimLayer(990, binding);
        stack.addAnimLayer(1000, this);
        stack.addAnimLayer(1100, skill);
    }

    public void detach() {
        setAnimation(null);
        binding.setAnimation(null);
        skill.setAnimation(null);
        blockPose = null;
        bindingApplied = false;
        if (stack != null) {
            stack.removeLayer(binding);
            stack.removeLayer(this);
            stack.removeLayer(skill);
            stack = null;
        }
    }

    public void beginAttack() {
        blockPose = null;
        skill.setAnimation(null);
    }

    /** A late guard-stop packet must not erase an attack that already replaced the guard. */
    public void stopBlock() {
        if (blockPose == null) return;
        blockPose = null;
        setAnimation(null);
        firstPerson.setCurrentFirstPersonConfig(FirstPersonModifier.FirstPersonConfigEnum.ONLY_RIGHT_ARM_AND_ITEM);
    }

    public boolean playing() {
        return isActive() || binding.isActive() || skill.isActive();
    }
}
