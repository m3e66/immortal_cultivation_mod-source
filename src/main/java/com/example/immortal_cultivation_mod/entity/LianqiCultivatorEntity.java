package com.example.immortal_cultivation_mod.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/** A Qi Refining cultivator backed by the same social and combat systems as higher-realm NPCs. */
public class LianqiCultivatorEntity extends ZhujiCultivatorEntity {
    public LianqiCultivatorEntity(EntityType<? extends LianqiCultivatorEntity> entityType, Level level) {
        super(entityType, level);
        xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 80.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.26D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 28.0D)
                .add(Attributes.ARMOR, 1.5D);
    }

    @Override
    public String npcArchetype() {
        return NpcProfile.LIANQI;
    }
}
