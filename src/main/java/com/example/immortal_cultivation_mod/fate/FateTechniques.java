package com.example.immortal_cultivation_mod.fate;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.entity.CultivationNpc;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import com.example.immortal_cultivation_mod.network.ModPayloads;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

@EventBusSubscriber(modid = ImmortalCultivationMod.MODID)
public final class FateTechniques {
    public static final String THEFT = "jietian_duoyun_shu";
    public static final String AVOID = "bijie_shu";
    public static final String SIGHT = "guanyun_shu";
    public static final String BLESSING = "yinfu_shu";
    public static final String MISFORTUNE_WARD = "bihuo_jue";

    private static final int BLESSING_QI = 35;
    private static final int BLESSING_LUCK = 3;
    private static final int BLESSING_DURATION = 60 * 20;
    private static final int BLESSING_COOLDOWN = 30 * 20;

    private static final int MISFORTUNE_WARD_QI = 60;
    private static final int MISFORTUNE_WARD_DURATION = 45 * 20;
    private static final int MISFORTUNE_WARD_COOLDOWN = 45 * 20;

    private static final DustParticleOptions GOLD =
            new DustParticleOptions(new Vector3f(0.65F, 0.45F, 0.12F), 0.7F);
    private static final DustParticleOptions PALE_GOLD =
            new DustParticleOptions(new Vector3f(0.88F, 0.80F, 0.48F), 0.65F);
    private static final DustParticleOptions RED =
            new DustParticleOptions(new Vector3f(0.5F, 0.02F, 0.02F), 0.8F);

    private FateTechniques() {}

    public static boolean isTechnique(String id) {
        return THEFT.equals(id)
                || AVOID.equals(id)
                || SIGHT.equals(id)
                || BLESSING.equals(id)
                || MISFORTUNE_WARD.equals(id);
    }

    public static LivingEntity lookedAt(ServerPlayer player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(range));
        LivingEntity best = null;
        double distance = range * range;

        for (LivingEntity candidate : player.serverLevel().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().expandTowards(player.getLookAngle().scale(range)).inflate(1),
                e -> e != player && e.isAlive() && LuckSystem.hasLuck(e))) {
            var hit = candidate.getBoundingBox().inflate(0.25).clip(start, end);
            if (hit.isPresent()
                    && start.distanceToSqr(hit.get()) < distance
                    && player.hasLineOfSight(candidate)) {
                distance = start.distanceToSqr(hit.get());
                best = candidate;
            }
        }
        return best;
    }

    public static boolean start(ServerPlayer player, String id) {
        var tag = LuckSystem.data(player);
        long now = player.serverLevel().getGameTime();

        if (tag.contains("Channel") || now < tag.getLong("Cooldown_" + id)) {
            return false;
        }

        var stats = ModAttachments.getData(player);
        String requirement = switch (id) {
            case THEFT -> CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_LATE;
            case AVOID -> CultivationLevels.REALM_ZHUJI + CultivationLevels.STAGE_LATE;
            case BLESSING -> CultivationLevels.REALM_LIANQI + CultivationLevels.STAGE_MID;
            case MISFORTUNE_WARD -> CultivationLevels.REALM_LIANQI + CultivationLevels.STAGE_LATE;
            default -> "";
        };

        if (!requirement.isEmpty()
                && CultivationLevels.getLevelDef(stats.cultivationLevel()).maxQi()
                < CultivationLevels.getLevelDef(requirement).maxQi()) {
            return false;
        }

        LivingEntity target = THEFT.equals(id) || SIGHT.equals(id)
                ? lookedAt(player, 8)
                : null;

        if (SIGHT.equals(id)) {
            if (stats.qi() < 100) {
                return false;
            }
            ModAttachments.setData(player, stats.withQi(stats.qi() - 100));
            LuckSystem.inspect(player, target == null ? player : target, true);
            cooldown(player, id, 100);
            ServerEvents.syncPlayerData(player);
            return true;
        }

        if (BLESSING.equals(id)) {
            return castBlessing(player, stats, tag, now);
        }

        if (MISFORTUNE_WARD.equals(id)) {
            return castMisfortuneWard(player, stats, tag, now);
        }

        if (THEFT.equals(id) && !validTarget(player, target)) {
            return false;
        }

        int fullCost = THEFT.equals(id) ? 1800 : 500;
        if (stats.qi() < fullCost) {
            return false;
        }

        // Reserve half of theft up front. Interruption therefore costs exactly 900 Qi.
        ModAttachments.setData(
                player,
                stats.withQi(stats.qi() - (THEFT.equals(id) ? 900 : 500)));

        CompoundTag channel = new CompoundTag();
        channel.putString("Id", id);
        channel.putLong("Start", now);
        channel.putString("Dimension", player.level().dimension().location().toString());
        if (target != null) {
            channel.putUUID("Target", target.getUUID());
        }
        tag.put("Channel", channel);

        DebugEyeDiagnostics.note(player, "action", id, "channel started; upfront Qi paid");
        ServerEvents.syncPlayerData(player);
        return true;
    }

    private static boolean castBlessing(
            ServerPlayer player,
            ModAttachments.CultivationData stats,
            CompoundTag tag,
            long now) {
        if (stats.qi() < BLESSING_QI) {
            return false;
        }

        boolean alreadyActive = tag.getLong("BlessingUntil") > now;
        int appliedBonus = alreadyActive ? Math.max(0, tag.getInt("BlessingBonus")) : 0;

        ModAttachments.setData(player, stats.withQi(stats.qi() - BLESSING_QI));

        if (!alreadyActive) {
            int currentLuck = LuckSystem.luck(player);
            appliedBonus = Math.max(0, Math.min(BLESSING_LUCK, 100 - currentLuck));
            if (appliedBonus > 0) {
                LuckSystem.change(player, appliedBonus);
            }
            tag.putInt("BlessingBonus", appliedBonus);
        }

        // Refresh duration without stacking the Luck bonus.
        tag.putLong("BlessingUntil", now + BLESSING_DURATION);
        cooldown(player, BLESSING, BLESSING_COOLDOWN);

        DebugEyeDiagnostics.note(
                player,
                "action",
                BLESSING,
                alreadyActive
                        ? "duration refreshed; Luck bonus remains +" + appliedBonus
                        : "Luck +" + appliedBonus + " for " + BLESSING_DURATION + " ticks");

        ring(player, GOLD, 0.9, now * 0.03);
        ServerEvents.syncPlayerData(player);
        return true;
    }

    private static boolean castMisfortuneWard(
            ServerPlayer player,
            ModAttachments.CultivationData stats,
            CompoundTag tag,
            long now) {
        if (stats.qi() < MISFORTUNE_WARD_QI) {
            return false;
        }

        ModAttachments.setData(player, stats.withQi(stats.qi() - MISFORTUNE_WARD_QI));
        tag.putLong("MisfortuneWardUntil", now + MISFORTUNE_WARD_DURATION);
        cooldown(player, MISFORTUNE_WARD, MISFORTUNE_WARD_COOLDOWN);

        DebugEyeDiagnostics.note(
                player,
                "action",
                MISFORTUNE_WARD,
                "negative Luck-event chance x0.75 for " + MISFORTUNE_WARD_DURATION + " ticks");

        ring(player, PALE_GOLD, 0.9, -now * 0.025);
        ServerEvents.syncPlayerData(player);
        return true;
    }

    private static boolean validTarget(ServerPlayer caster, LivingEntity target) {
        return target != null
                && target.isAlive()
                && caster.distanceToSqr(target) <= 64
                && (target instanceof net.minecraft.world.entity.player.Player
                || target instanceof CultivationNpc
                || LuckSystem.boss(target))
                && !target.getTags().contains("icm_no_fortune_theft")
                && !LuckSystem.scarred(target)
                && !(target instanceof net.minecraft.world.entity.player.Player p
                && (p.isCreative() || p.isSpectator()))
                && LuckRules.theftAmount(
                LuckSystem.luck(caster),
                LuckSystem.luck(target),
                LuckSystem.boss(target)) > 0;
    }

    public static void tick(ServerPlayer player) {
        var tag = LuckSystem.data(player);
        long now = player.serverLevel().getGameTime();

        expireBlessingIfNeeded(player, now);

        if (tag.getLong("AvoidUntil") > now && player.tickCount % 12 == 0) {
            ring(player, GOLD, 0.8, now * 0.02);
        }
        if (tag.getLong("BlessingUntil") > now && player.tickCount % 15 == 0) {
            ring(player, GOLD, 0.72, now * 0.025);
        }
        if (tag.getLong("MisfortuneWardUntil") > now && player.tickCount % 15 == 0) {
            ring(player, PALE_GOLD, 0.82, -now * 0.02);
        }

        if (!tag.contains("Channel")) {
            return;
        }

        CompoundTag channel = tag.getCompound("Channel");
        String id = channel.getString("Id");
        long age = now - channel.getLong("Start");
        LivingEntity target = channel.hasUUID("Target")
                && player.serverLevel().getEntity(channel.getUUID("Target")) instanceof LivingEntity living
                ? living
                : null;

        if (!player.isAlive()
                || player.isSpectator()
                || !player.level().dimension().location().toString().equals(channel.getString("Dimension"))
                || (THEFT.equals(id)
                && (!validTarget(player, target) || player.distanceToSqr(target) > 64))) {
            fail(player);
            return;
        }

        if (age % 4 == 0) {
            ring(
                    player,
                    THEFT.equals(id)
                            ? GOLD
                            : new DustParticleOptions(new Vector3f(0.72F, 0.72F, 0.64F), 0.7F),
                    1.2,
                    -age * 0.04);

            if (target != null && age >= 40) {
                Vec3 cloud = target.position().add(0, target.getBbHeight() + 0.5, 0);
                player.serverLevel().sendParticles(
                        GOLD, cloud.x, cloud.y, cloud.z, 12, 0.4, 0.1, 0.4, 0.02);
                if (age >= 60) {
                    thread(player, cloud, player.position().add(0, 1, 0), GOLD);
                }
            } else if (AVOID.equals(id)) {
                Vec3 origin = player.position().add(2, 1, 0);
                thread(player, origin, player.position().add(0.5, 1.5, 1.5), RED);
            }
        }

        if (age < (THEFT.equals(id) ? 120 : 30)) {
            return;
        }

        if (THEFT.equals(id)) {
            int before = LuckSystem.luck(target);
            int amount = LuckRules.theftAmount(
                    LuckSystem.luck(player),
                    before,
                    LuckSystem.boss(target));
            var stats = ModAttachments.getData(player);

            if (amount <= 0 || stats.qi() < 900) {
                fail(player);
                return;
            }

            ModAttachments.setData(player, stats.withQi(stats.qi() - 900));
            LuckSystem.change(target, -amount);
            LuckSystem.change(player, amount);
            LuckSystem.addKarma(player, LuckRules.theftKarma(amount, before));

            DebugEyeDiagnostics.note(
                    player,
                    "action",
                    id,
                    "transferred " + amount + " Luck; added "
                            + LuckRules.theftKarma(amount, before) + " Karma");

            LuckSystem.data(target).putLong("ScarUntil", now + LuckSystem.SCAR_DURATION);
            LuckSystem.data(target).putLong(
                    "NextDrift",
                    now + LuckSystem.DAY * (LuckSystem.luck(target) < 50 ? 2 : 1));

            player.serverLevel().sendParticles(
                    ParticleTypes.END_ROD,
                    target.getX(),
                    target.getY() + 1,
                    target.getZ(),
                    40,
                    0.7,
                    1,
                    0.2,
                    0.15);

            ring(player, GOLD, 1.7, -age * 0.06);
            if (before >= 90) {
                thread(
                        player,
                        player.position().add(-5, 12, 0),
                        player.position().add(5, 14, 1),
                        RED);
            }
            cooldown(player, id, 6000);
        } else {
            tag.putLong("AvoidUntil", now + 2400);
            DebugEyeDiagnostics.note(player, "action", id, "avoidance active for 2400 ticks");
            cooldown(player, id, 4800);
        }

        tag.remove("Channel");
        ServerEvents.syncPlayerData(player);
    }

    private static void expireBlessingIfNeeded(ServerPlayer player, long now) {
        CompoundTag tag = LuckSystem.data(player);
        long until = tag.getLong("BlessingUntil");

        if (until <= 0 || until > now) {
            return;
        }

        int appliedBonus = Math.max(0, tag.getInt("BlessingBonus"));
        tag.remove("BlessingUntil");
        tag.remove("BlessingBonus");

        if (appliedBonus > 0) {
            LuckSystem.change(player, -appliedBonus);
        }

        DebugEyeDiagnostics.note(
                player,
                "action",
                BLESSING,
                "temporary Luck bonus expired; removed " + appliedBonus);
        ServerEvents.syncPlayerData(player);
    }

    public static boolean hasBlessing(ServerPlayer player) {
        return LuckSystem.data(player).getLong("BlessingUntil")
                > player.serverLevel().getGameTime();
    }

    public static boolean hasMisfortuneWard(ServerPlayer player) {
        return LuckSystem.data(player).getLong("MisfortuneWardUntil")
                > player.serverLevel().getGameTime();
    }

    public static double applyMisfortuneWard(ServerPlayer player, double baseChance) {
        return hasMisfortuneWard(player)
                ? LuckRules.misfortuneWardChance(baseChance)
                : baseChance;
    }

    public static void fail(ServerPlayer player) {
        var tag = LuckSystem.data(player);
        if (!tag.contains("Channel")) {
            return;
        }

        String id = tag.getCompound("Channel").getString("Id");
        DebugEyeDiagnostics.note(
                player,
                "action",
                id,
                "channel interrupted; failure cooldown applied");
        tag.remove("Channel");
        cooldown(player, id, THEFT.equals(id) ? 1200 : 4800);
        player.displayClientMessage(
                Component.translatable("luck.immortal_cultivation_mod.interrupted"), true);
    }

    private static void cooldown(ServerPlayer player, String id, int ticks) {
        LuckSystem.data(player).putLong(
                "Cooldown_" + id,
                player.serverLevel().getGameTime() + ticks);
        PacketDistributor.sendToPlayer(
                player,
                new ModPayloads.ClientboundSpellCooldownPayload(id, ticks));
    }

    @SubscribeEvent
    public static void hurt(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player
                && event.getNewDamage() > player.getMaxHealth() * 0.10F) {
            fail(player);
        }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            fail(player);
        }
    }

    public static void ring(
            ServerPlayer player,
            DustParticleOptions particle,
            double radius,
            double phase) {
        for (int i = 0; i < 24; i++) {
            double a = phase + i * Math.PI / 12;
            player.serverLevel().sendParticles(
                    particle,
                    player.getX() + Math.cos(a) * radius,
                    player.getY() + 0.12,
                    player.getZ() + Math.sin(a) * radius,
                    1,
                    0,
                    0,
                    0,
                    0);
        }
    }

    private static void thread(
            ServerPlayer player,
            Vec3 from,
            Vec3 to,
            DustParticleOptions particle) {
        for (int i = 0; i <= 24; i++) {
            Vec3 p = from.lerp(to, i / 24.0);
            player.serverLevel().sendParticles(
                    particle,
                    p.x,
                    p.y,
                    p.z,
                    1,
                    0.025,
                    0.025,
                    0.025,
                    0);
        }
    }
}
