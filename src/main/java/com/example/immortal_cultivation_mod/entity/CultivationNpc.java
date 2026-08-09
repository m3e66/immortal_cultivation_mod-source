package com.example.immortal_cultivation_mod.entity;

import java.util.List;
import net.minecraft.world.entity.PathfinderMob;

public interface CultivationNpc {
    String npcArchetype();

    String getNpcSkinId();

    void setNpcSkinId(String skinId);

    /** Starts the synced pose for an actual, interruptible spell wind-up. */
    default void beginNpcCast(int ticks) {
    }

    /** Ends the wind-up pose when the spell releases or is interrupted. */
    default void endNpcCast() {
    }

    /** Remaining real wind-up ticks, used by other combatants for interrupt decisions. */
    default int npcCastingTicks() {
        return 0;
    }

    default PathfinderMob asNpcMob() {
        return (PathfinderMob) this;
    }

    default String npcName() {
        return NpcProfile.name(asNpcMob(), npcArchetype());
    }

    default String npcIdentity() {
        return NpcProfile.identity(asNpcMob(), npcArchetype());
    }

    default String npcGender() {
        return NpcProfile.gender(asNpcMob(), npcArchetype());
    }

    default String npcPersonality() {
        return NpcProfile.personality(asNpcMob(), npcArchetype());
    }

    default String npcRealm() {
        return NpcProfile.realm(asNpcMob(), npcArchetype());
    }

    default long npcCultivationProgress() {
        return NpcProfile.cultivationProgress(asNpcMob(), npcArchetype());
    }

    default int npcQi() {
        return NpcProfile.qi(asNpcMob(), npcArchetype());
    }

    default int npcMaxQi() {
        return NpcProfile.maxQi(asNpcMob(), npcArchetype());
    }

    default int npcThoughts() {
        return NpcProfile.thoughts(asNpcMob(), npcArchetype());
    }

    default int npcMaxThoughts() {
        return NpcProfile.maxThoughts(asNpcMob(), npcArchetype());
    }

    default String npcSpiritRoots() {
        return NpcProfile.spiritRootsDisplay(asNpcMob(), npcArchetype());
    }

    default String npcCultivationMethod() {
        return NpcProfile.methodDisplay(asNpcMob(), npcArchetype());
    }

    default List<String> npcSpells() {
        return NpcProfile.spells(asNpcMob(), npcArchetype());
    }

    default int npcFriendship() {
        return NpcProfile.relationshipValue(asNpcMob(), "Friendship");
    }

    default int npcTrust() {
        return NpcProfile.relationshipValue(asNpcMob(), "Trust");
    }

    default int npcAffection() {
        return NpcProfile.relationshipValue(asNpcMob(), "Affection");
    }

    default int npcHatred() {
        return NpcProfile.relationshipValue(asNpcMob(), "Hatred");
    }
}
