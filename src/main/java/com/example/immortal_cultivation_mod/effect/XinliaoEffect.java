package com.example.immortal_cultivation_mod.effect;

import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.entity.CultivationNpc;
import com.example.immortal_cultivation_mod.entity.NpcProfile;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import com.example.immortal_cultivation_mod.spell.ThoughtDamageHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;

public class XinliaoEffect extends MobEffect {
    private static final int QI_DRAIN_PER_SECOND = 100;
    private static final int THOUGHT_DRAIN_PER_SECOND = 10;

    public XinliaoEffect() {
        super(MobEffectCategory.HARMFUL, 0xE84816);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        entity.setRemainingFireTicks(Math.max(entity.getRemainingFireTicks(), 40));
        if (!entity.level().isClientSide && entity.tickCount % 20 == 0) {
            int thoughtDamage = ThoughtDamageHelper.total(THOUGHT_DRAIN_PER_SECOND, amplifier);
            if (entity instanceof ServerPlayer player) {
                var data = ModAttachments.getData(player);
                ModAttachments.setData(player, data.withQi(Math.max(0, data.qi() - QI_DRAIN_PER_SECOND)));
                ServerEvents.adjustThoughts(player, -thoughtDamage);
            } else if (entity instanceof PathfinderMob mob && entity instanceof CultivationNpc npc) {
                NpcProfile.drainQi(mob, npc.npcArchetype(), QI_DRAIN_PER_SECOND);
                NpcProfile.adjustThoughts(mob, npc.npcArchetype(), -thoughtDamage);
            }
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
