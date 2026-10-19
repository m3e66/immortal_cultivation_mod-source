package com.example.immortal_cultivation_mod.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

public abstract class RealmBeastMonster extends Monster implements RealmBeast {
    private static final EntityDataAccessor<Integer> REALM = SynchedEntityData.defineId(
            RealmBeastMonster.class, EntityDataSerializers.INT);

    protected RealmBeastMonster(EntityType<? extends Monster> type, Level level) { super(type, level); }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(REALM, -1);
    }
    @Override public int beastRealm() {
        int saved = entityData.get(REALM);
        return saved < 0 ? BeastRealms.baseRealm(getType()) : saved;
    }
    @Override public void setBeastRealm(int realm) {
        entityData.set(REALM, BeastRealms.clamp(getType(), realm));
        BeastRealms.applyAttributes(this);
        if (this instanceof PhantomVeilFoxEntity fox) xpReward = fox.realmTier().experienceReward();
    }
    @Override public Component getName() { return BeastRealms.prefix(beastRealm(), super.getName()); }
    @Override public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        if (com.example.immortal_cultivation_mod.combat.ColdTideFreeze.isFrozen(this)) return false;
        boolean hit = super.doHurtTarget(target);
        if (hit) swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        return hit;
    }
    @Override public void die(DamageSource source) {
        // NeoForge fires LivingDeathEvent before vanilla's dead check. Avoid duplicate
        // blood drops and kill rewards when another death call reaches the same body.
        if (dead || isRemoved()) return;
        super.die(source);
    }
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
            MobSpawnType reason, SpawnGroupData group) {
        group = super.finalizeSpawn(level, difficulty, reason, group);
        setBeastRealm(BeastSpawnRules.isNatural(reason)
                ? BeastRealms.naturalRealm(getType(), level, blockPosition())
                : BeastRealms.randomRealm(getType(), getRandom()));
        return group;
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(BeastRealms.NBT_KEY, beastRealm());
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setBeastRealm(BeastRealms.savedRealm(getType(), tag));
    }
}
