package com.example.immortal_cultivation_mod.spell;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.block.ModBlocks;
import com.example.immortal_cultivation_mod.entity.MethodTechniqueAreaEntity;
import com.example.immortal_cultivation_mod.entity.NpcProfile;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Complete 生机循环 runtime for 长青诀. */
public final class ChangqingJue {
    public static final float MAX_LIFE_FORCE = 100.0F;
    public static final float FLOURISHING_THRESHOLD = 60.0F;
    public static final float EVERGREEN_BREATH_THRESHOLD = 80.0F;
    private static final int ENVIRONMENT_SCAN_INTERVAL = 40;
    private static final String REVIVAL_COOLDOWN_UNTIL = "ChangqingRevivalCooldownUntil";
    private static final int REVIVAL_COOLDOWN_TICKS = 20 * 180;
    private static final float REVIVAL_LIFE_COST = 35.0F;

    private ChangqingJue() {
    }

    public static boolean hasMethod(LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            return CultivationMethods.isChangqingJue(
                    ModAttachments.getData(player).activeCultivationMethod());
        }
        if (entity instanceof PathfinderMob mob) {
            String archetype = mob instanceof com.example.immortal_cultivation_mod.entity.CultivationNpc npc
                    ? npc.npcArchetype() : NpcProfile.FANREN;
            return CultivationMethods.isChangqingJue(NpcProfile.methodId(mob, archetype));
        }
        return false;
    }

    public static float lifeForce(ServerPlayer player) {
        return CultivationMethods.changqingLifeForce(ModAttachments.getData(player));
    }

    public static void addLifeForce(ServerPlayer player, float amount) {
        if (amount <= 0.0F || !hasMethod(player)) return;
        ModAttachments.CultivationData data = ModAttachments.getData(player);
        float current = CultivationMethods.changqingLifeForce(data);
        float next = Mth.clamp(current + amount, 0.0F, MAX_LIFE_FORCE);
        if (Math.abs(next - current) >= 0.005F) {
            ModAttachments.setData(player,
                    CultivationMethods.withChangqingLifeForce(data, next));
        }
    }

    public static float consumeLifeForce(ServerPlayer player, float requested) {
        if (requested <= 0.0F) return 0.0F;
        ModAttachments.CultivationData data = ModAttachments.getData(player);
        float current = CultivationMethods.changqingLifeForce(data);
        float spent = Math.min(current, requested);
        if (spent > 0.0F) {
            ModAttachments.setData(player,
                    CultivationMethods.withChangqingLifeForce(data, current - spent));
        }
        return spent;
    }

    public static void tick(ServerPlayer player) {
        ModAttachments.CultivationData data = ModAttachments.getData(player);
        if (!CultivationMethods.isChangqingJue(data.activeCultivationMethod())) {
            if (player.tickCount % 40 == 0
                    && CultivationMethods.changqingLifeForce(data) > 0.0F) {
                ModAttachments.setData(player, CultivationMethods.withChangqingLifeForce(
                        data, CultivationMethods.changqingLifeForce(data) - 0.25F));
            }
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) return;

        if (player.tickCount % ENVIRONMENT_SCAN_INTERVAL == 0) {
            float gathered = environmentalGain(player, level);
            addLifeForce(player, gathered);
        }

        float reserve = lifeForce(player);
        float healthRatio = player.getHealth() / Math.max(1.0F, player.getMaxHealth());
        if (player.tickCount % 20 == 0 && healthRatio < 0.40F && reserve > 0.0F) {
            // 枯: the reserve burns faster as the body approaches death.
            float spend = healthRatio < 0.20F ? 2.5F : healthRatio < 0.30F ? 1.5F : 0.75F;
            healFromReserve(player, spend, 0.0045F);
        } else if (player.tickCount % 40 == 0
                && reserve >= FLOURISHING_THRESHOLD
                && player.getHealth() < player.getMaxHealth()) {
            // 荣: quiet, sustainable natural recovery.
            healFromReserve(player, 0.45F, 0.0035F);
        }

        if (player.tickCount % 60 == 0
                && lifeForce(player) >= EVERGREEN_BREATH_THRESHOLD
                && player.getHealth() < player.getMaxHealth()) {
            // 长青之息: a visible pulse of extra missing-health recovery.
            healFromReserve(player, 1.25F, 0.005F);
        }

        if (player.tickCount % 200 == 0 && lifeForce(player) >= FLOURISHING_THRESHOLD) {
            spreadVegetation(level, player);
        }
    }

    /** Final post-shield protection. Large bursts can still exceed the per-hit conversion cap. */
    public static float absorbWithLifeForce(ServerPlayer player, float incomingDamage) {
        if (incomingDamage <= 0.0F || !hasMethod(player) || lifeForce(player) <= 0.0F) {
            return incomingDamage;
        }
        float thresholdHealth = player.getMaxHealth() * 0.35F;
        float projectedHealth = player.getHealth() - incomingDamage;
        if (projectedHealth >= thresholdHealth) return incomingDamage;

        float hpPerLife = Math.max(1.0F, player.getMaxHealth() * 0.005F);
        float pressure = thresholdHealth - projectedHealth;
        float requestedLife = Math.min(12.0F, pressure / hpPerLife);
        float spent = consumeLifeForce(player, requestedLife);
        float remaining = Math.max(0.0F, incomingDamage - spent * hpPerLife);
        if (remaining < player.getHealth()
                || lifeForce(player) < REVIVAL_LIFE_COST
                || player.level().getGameTime()
                < player.getPersistentData().getLong(REVIVAL_COOLDOWN_UNTIL)) {
            return remaining;
        }

        consumeLifeForce(player, REVIVAL_LIFE_COST);
        player.getPersistentData().putLong(REVIVAL_COOLDOWN_UNTIL,
                player.level().getGameTime() + REVIVAL_COOLDOWN_TICKS);
        float recovery = Math.max(1.0F, player.getMaxHealth() * 0.12F);
        player.heal(recovery);
        player.displayClientMessage(Component.translatable(
                "message." + ImmortalCultivationMod.MODID + ".kumu_fengchun"), true);
        return 0.0F;
    }

    public static int adjustQiRegen(ServerPlayer player, int amount) {
        if (amount <= 0 || !hasMethod(player)) return amount;
        float reserve = lifeForce(player);
        if (reserve >= EVERGREEN_BREATH_THRESHOLD) {
            return Math.max(1, Math.round(amount * 1.25F));
        }
        return reserve >= FLOURISHING_THRESHOLD
                ? Math.max(1, Math.round(amount * 1.12F)) : amount;
    }

    public static float woodQiCostMultiplier(ServerPlayer player) {
        if (!hasMethod(player)) return 1.0F;
        float reserve = lifeForce(player);
        return reserve >= EVERGREEN_BREATH_THRESHOLD ? 0.85F
                : reserve >= FLOURISHING_THRESHOLD ? 0.90F : 1.0F;
    }

    public static float healingMultiplier(LivingEntity caster) {
        if (!(caster instanceof ServerPlayer player) || !hasMethod(player)) return 1.0F;
        float reserve = lifeForce(player);
        return reserve >= EVERGREEN_BREATH_THRESHOLD ? 1.30F
                : reserve >= FLOURISHING_THRESHOLD ? 1.15F : 1.0F;
    }

    public static void onHealingOther(
            LivingEntity caster, LivingEntity target, float restoredHealth) {
        if (!(caster instanceof ServerPlayer player) || target == caster
                || restoredHealth <= 0.0F || !hasMethod(player)) return;
        addLifeForce(player, Mth.clamp(restoredHealth / 35.0F, 0.25F, 4.0F));
    }

    public static void nourishFromGrowthField(ServerPlayer player, float amount) {
        if (hasMethod(player)) addLifeForce(player, amount);
    }

    private static float environmentalGain(ServerPlayer player, ServerLevel level) {
        BlockPos center = player.blockPosition();
        float terrain = 0.0F;
        int fireSources = player.isOnFire() ? 4 : 0;
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-7, -2, -7), center.offset(7, 3, 7))) {
            if (pos.distSqr(center) > 64.0D) continue;
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) continue;
            if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)
                    || state.is(Blocks.LAVA) || state.is(Blocks.MAGMA_BLOCK)) {
                fireSources++;
            }
            terrain += lifeValue(state);
        }
        terrain = Math.min(65.0F, terrain);

        AABB livingBounds = player.getBoundingBox().inflate(8.0D, 4.0D, 8.0D);
        int living = level.getEntitiesOfClass(LivingEntity.class, livingBounds,
                entity -> entity != player && entity.isAlive()
                        && !entity.getType().is(EntityTypeTags.UNDEAD)).size();
        float livingGain = Math.min(1.2F, living * 0.12F);
        float ambientGain = Math.min(2.0F,
                Math.max(0, ServerEvents.ambientQiValue(player)) / 35.0F);
        float gain = 0.08F + terrain * 0.025F + livingGain + ambientGain;
        if (fireSources > 0) {
            gain *= fireSources >= 5 ? 0.25F : 0.55F;
        }
        return gain;
    }

    private static float lifeValue(BlockState state) {
        if (state.is(ModBlocks.LINGMU.get())) return 2.0F;
        if (state.is(ModBlocks.LINGMU_LEAVES.get())) return 0.65F;
        if (state.is(ModBlocks.LINGMU_LOG.get())) return 0.40F;
        if (state.is(ModBlocks.QINGLING_GRASS.get())
                || state.is(ModBlocks.CHIYAN_FLOWER.get())
                || state.is(ModBlocks.NINGLU_ZHI.get())
                || state.is(ModBlocks.XUANBING_LOTUS.get())
                || state.is(ModBlocks.ZIYUN_VINE.get())
                || state.is(ModBlocks.SHIHUN_GRASS.get())
                || state.is(ModBlocks.JIUYE_XIANLAN.get())) return 1.25F;
        if (state.is(BlockTags.FLOWERS)) return 0.55F;
        if (state.is(BlockTags.SAPLINGS)) return 0.50F;
        if (state.is(BlockTags.LEAVES)) return 0.16F;
        if (state.is(BlockTags.LOGS)) return 0.10F;
        if (state.is(BlockTags.CROPS) || state.getBlock() instanceof CropBlock) return 0.28F;
        if (state.is(Blocks.SHORT_GRASS) || state.is(Blocks.TALL_GRASS)
                || state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN)) return 0.22F;
        if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.MOSS_BLOCK)) return 0.035F;
        return 0.0F;
    }

    private static void healFromReserve(
            ServerPlayer player, float requestedLife, float maxHealthPerLife) {
        float missing = player.getMaxHealth() - player.getHealth();
        if (missing <= 0.0F) return;
        float hpPerLife = Math.max(0.5F, player.getMaxHealth() * maxHealthPerLife);
        float needed = Math.min(requestedLife, missing / hpPerLife);
        float spent = consumeLifeForce(player, needed);
        if (spent <= 0.0F) return;
        player.heal(Math.min(missing, spent * hpPerLife));
    }

    private static void spreadVegetation(ServerLevel level, ServerPlayer player) {
        BlockPos center = player.blockPosition();
        int grown = 0;
        for (int attempt = 0; attempt < 18 && grown < 3; attempt++) {
            BlockPos pos = center.offset(
                    level.random.nextInt(13) - 6,
                    level.random.nextInt(5) - 2,
                    level.random.nextInt(13) - 6);
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof BonemealableBlock growable)
                    || !growable.isValidBonemealTarget(level, pos, state)
                    || !growable.isBonemealSuccess(level, level.random, pos, state)) {
                continue;
            }
            growable.performBonemeal(level, level.random, pos, state);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    pos.getX() + 0.5D, pos.getY() + 0.7D, pos.getZ() + 0.5D,
                    6, 0.25D, 0.25D, 0.25D, 0.02D);
            grown++;
        }
    }
}
