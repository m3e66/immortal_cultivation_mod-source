package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class LiusanEntity extends JindanCultivatorEntity {
    private static final String INITIALIZED_TAG = "ImmortalCultivationLiusanInitialized";

    public LiusanEntity(EntityType<? extends LiusanEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public void tick() {
        if (!level().isClientSide && !getPersistentData().getBoolean(INITIALIZED_TAG)) {
            NpcProfile.configureFixedCultivator(
                    this,
                    NpcProfile.JINDAN,
                    "liusan",
                    "liusan",
                    "male",
                    CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_LATE);
            getPersistentData().putBoolean(INITIALIZED_TAG, true);
        }
        super.tick();
    }
}
