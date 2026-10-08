package com.example.immortal_cultivation_mod.effect;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.entity.CultivationNpc;
import com.example.immortal_cultivation_mod.entity.NpcProfile;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;

public class QiFireBurnEffect extends MobEffect {
    public QiFireBurnEffect() {
        super(MobEffectCategory.HARMFUL, 0x1B4DFF);
    }

    @Override public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        // Existing ordinary spirit-fire contact uses amplifier zero and keeps its old behavior.
        return amplifier > 0;
    }

    @Override public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide || amplifier <= 0) return true;
        long now = entity.level().getGameTime();
        var tag = entity.getPersistentData();
        String stamp = "icmDragonQiBurnTick";
        long last = tag.getLong(stamp);
        if (tag.contains(stamp) && now >= last && now - last < 20) return true;
        tag.putLong(stamp, now);
        if (entity instanceof ServerPlayer player && !player.isCreative() && !player.isSpectator()) {
            var data = ModAttachments.getData(player);
            long maximum = Math.max(1L, (long) CultivationLevels.getLevelDef(data.cultivationLevel()).maxQi() + data.maxQiBonus());
            int drain = (int) Math.min(Integer.MAX_VALUE, Math.max(1L, (maximum + 49) / 50));
            ModAttachments.setData(player, data.withQi(Math.max(0, data.qi() - drain)));
            ServerEvents.syncPlayerData(player);
        } else if (entity instanceof PathfinderMob mob && entity instanceof CultivationNpc npc) {
            int maximum = NpcProfile.maxQi(mob, npc.npcArchetype());
            int drain = (int) Math.min(Integer.MAX_VALUE, Math.max(1L, ((long) maximum + 49) / 50));
            NpcProfile.drainQi(mob, npc.npcArchetype(), drain);
        }
        return true;
    }
}
