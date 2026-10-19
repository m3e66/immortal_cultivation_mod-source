package com.example.immortal_cultivation_mod.screen;

import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/** Client-only categories applied after discovery checks, preserving server NPC indices. */
enum LifeRelationshipFilter {
    ALL, LIVING, DECEASED, FAMILY, FRIENDS, PROTECTORS, NEMESES;

    boolean matches(CompoundTag person) {
        return switch (this) {
            case ALL -> true;
            case LIVING -> person.getBoolean("Alive");
            case DECEASED -> !person.getBoolean("Alive");
            case FAMILY -> List.of("father", "mother", "sibling", "child", "dao_companion").contains(person.getString("Relation"));
            case FRIENDS -> person.getBoolean("Friend") || List.of("friend", "childhood_friend").contains(person.getString("Relation"));
            case PROTECTORS -> person.getBoolean("Protector");
            case NEMESES -> person.getBoolean("Nemesis");
        };
    }

    LifeRelationshipFilter next() { return values()[(ordinal() + 1) % values().length]; }
    Component label() {
        return Component.translatable("life.relationship.filter", Component.translatable(
                "life.relationship.filter." + name().toLowerCase(java.util.Locale.ROOT)));
    }
}
