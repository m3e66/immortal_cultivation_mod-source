package com.example.immortal_cultivation_mod.fate;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.entity.CultivationNpc;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.item.UtilityTalismanItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Server-owned Luck and Karma. No damage/armor/speed modifiers are derived from Luck. */
@EventBusSubscriber(modid = ImmortalCultivationMod.MODID)
public final class LuckSystem {
    public static final String DATA = "icmFate";
    public static final long DAY = 24_000L;
    public static final long SCAR_DURATION = 7 * DAY;
    private LuckSystem() {}
    public static CompoundTag data(LivingEntity entity) {
        var root = entity.getPersistentData();
        if (!root.contains(DATA)) root.put(DATA, new CompoundTag());
        return root.getCompound(DATA);
    }
    public static boolean hasLuck(LivingEntity entity) { return entity instanceof Player || entity instanceof Mob; }
    public static int luck(LivingEntity entity) {
        if (entity instanceof Player player) return LuckRules.clamp(ModAttachments.getData(player).luck());
        CompoundTag tag = data(entity);
        if (!tag.contains("Luck")) {
            int value = 45 + entity.getRandom().nextInt(11);
            if (entity.getTags().contains("icm_destiny_child")) value = 95 + entity.getRandom().nextInt(6);
            else if (entity.getTags().contains("icm_prodigy")) value = 75 + entity.getRandom().nextInt(16);
            else if (entity.getTags().contains("icm_story_character"))
                value = 75 + entity.getRandom().nextInt(11);
            else if (entity instanceof CultivationNpc npc && !npc.npcArchetype().equals("fanren"))
                value = 40 + entity.getRandom().nextInt(21);
            tag.putInt("Luck", value);
        }
        return LuckRules.clamp(tag.getInt("Luck"));
    }
    public static int change(LivingEntity entity, int delta) {
        int before = luck(entity), after = LuckRules.clamp(before + delta);
        if (entity instanceof Player player) {
            var cultivation = ModAttachments.getData(player);
            ModAttachments.setData(player, cultivation.withLuck(after));
            if (player instanceof ServerPlayer serverPlayer) ServerEvents.syncPlayerData(serverPlayer);
        } else data(entity).putInt("Luck", after);
        if (after != before) {
            DebugEyeDiagnostics.note(entity, "luck_change", before, after, after - before);
            data(entity).putInt("Trend", Integer.signum(after - before));
            data(entity).putLong("TrendUntil", entity.level().getGameTime() + DAY);
        }
        return after - before;
    }
    public static int karma(LivingEntity entity) { return Math.max(0, data(entity).getInt("Karma")); }
    public static void addKarma(LivingEntity entity, int amount) {
        data(entity).putInt("Karma", (int)Math.max(0L, Math.min(Integer.MAX_VALUE, (long)karma(entity) + amount)));
    }
    public static boolean scarred(LivingEntity entity) {
        return data(entity).getLong("ScarUntil") > entity.level().getGameTime();
    }
    public static boolean boss(LivingEntity entity) {
        String id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
        return entity.getTags().contains("icm_fate_boss")
                || entity instanceof net.minecraft.world.entity.boss.wither.WitherBoss
                || entity instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon
                || id.contains("general") || id.contains("flood_dragon") || id.contains("guardian");
    }
    public static Component state(int value) {
        return Component.translatable("luck.immortal_cultivation_mod.state." + LuckRules.state(value));
    }
    public static Component ownDisplay(Player player, int value) {
        boolean precise = player != null && (player.getInventory().contains(new net.minecraft.world.item.ItemStack(
                ModItems.PRECISE_FORTUNE_MIRROR.get()))
                || ModAttachments.getData(player).knownSpells().contains("guanyun_shu"));
        return precise ? Component.literal(value + " · ").append(state(value)) : state(value);
    }
    public static void inspect(ServerPlayer observer, LivingEntity target, boolean precise) {
        int n = luck(target);
        Component reading = precise ? Component.literal(Integer.toString(n))
                : Component.literal(Math.max(0, n / 15 * 15) + "–" + Math.min(100, n / 15 * 15 + 14));
        observer.sendSystemMessage(Component.translatable("luck.immortal_cultivation_mod.reading",
                target.getDisplayName(), state(n), reading));
        if (precise) {
            int trend = data(target).getLong("TrendUntil") > target.level().getGameTime() ? data(target).getInt("Trend") : 0;
            observer.sendSystemMessage(Component.translatable("luck.immortal_cultivation_mod.detail",
                    trend > 0 ? "↑" : trend < 0 ? "↓" : "—", karma(target)));
        }
        if (scarred(target)) observer.sendSystemMessage(Component.translatable("luck.immortal_cultivation_mod.scar"));
    }
    /** Fate rewards are keyed to their actual completion, not monster kills. */
    public static void rewardOnce(ServerPlayer player, String key, int amount) {
        var records = data(player).getCompound("Resolved");
        if (records.getBoolean(key)) return;
        records.putBoolean(key, true);
        data(player).put("Resolved", records);
        change(player, amount);
    }
    public static void drift(LivingEntity entity) {
        if (!hasLuck(entity) || entity.getTags().contains("icm_fate_anchored")) return;
        long now = entity.level().getGameTime();
        var tag = data(entity);
        if (!tag.contains("NextDrift")) { tag.putLong("NextDrift", now + DAY); return; }
        if (now < tag.getLong("NextDrift")) return;
        int natural = luck(entity) - (entity instanceof Player player ? UtilityTalismanItem.activeLuckBonus(player) : 0);
        boolean lampAnchored = natural >= 50 && natural <= 65 && tag.getLong("LampAnchorUntil") > now;
        if (natural != 50 && !lampAnchored) change(entity, natural < 50 ? 1 : -1);
        tag.putLong("NextDrift", now + (natural < 50 && scarred(entity) ? DAY * 2 : DAY));
    }
    @SubscribeEvent
    public static void join(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof LivingEntity living)) return;
        if (living instanceof ServerPlayer player) {
            var tag = data(player);
            if (!tag.getBoolean("Initialized")) {
                // Existing characters retain their saved fortune; new players begin near the baseline.
                if (player.getStats().getValue(net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.PLAY_TIME)) == 0
                        && ModAttachments.getData(player).luck() == 50) change(player, player.getRandom().nextInt(11) - 5);
                tag.putBoolean("Initialized", true);
            }
        } else if (hasLuck(living)) luck(living);
    }
    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.tickCount % 20 == 0) drift(player);
        FateTechniques.tick(player);
        com.example.immortal_cultivation_mod.item.FateAlchemyFormulaItem.tick(player);
        FateEncounters.tick(player);
    }
    @SubscribeEvent
    public static void mobTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof Mob mob && !mob.level().isClientSide && mob.tickCount % 200 == 0) drift(mob);
    }
    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {
        var previous = event.getOriginal().getPersistentData();
        if (previous.contains(DATA)) event.getEntity().getPersistentData().put(DATA, previous.getCompound(DATA).copy());
        var fate = data(event.getEntity());
        if (fate.contains("Channel")) {
            String id = fate.getCompound("Channel").getString("Id");
            fate.putLong("Cooldown_" + id, event.getEntity().level().getGameTime() + (FateTechniques.THEFT.equals(id) ? 1200 : 4800));
            fate.remove("Channel");
        }
        fate.remove("Alchemy");
        UtilityTalismanItem.copyLuckWard(event.getOriginal(), event.getEntity());
    }
    /** Players can finish mobs regardless of Luck; player victims retain combat rescue. */
    public static boolean rescueMatchupAllowed(LivingEntity victim, LivingEntity attacker) {
        return victim != attacker && hasLuck(victim) && hasLuck(attacker)
                && (!(attacker instanceof Player) || victim instanceof Player);
    }

    // Equipment and revival effects get the first chance; Luck is the fallback.
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void miracle(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (event.isCanceled() || !(victim.level() instanceof ServerLevel level)
                || !(event.getSource().getEntity() instanceof LivingEntity attacker)
                || !rescueMatchupAllowed(victim, attacker)) return;
        int victimLuck = luck(victim), attackerLuck = luck(attacker);
        if (!LuckRules.rescueEligible(victimLuck, attackerLuck)) {
            DebugEyeDiagnostics.note(victim, "rescue_blocked", victimLuck, attackerLuck);
            return;
        }
        Vec3 origin = victim.position();
        Vec3 escape = safeEscape(victim, attacker.position());
        // A cramped arena may have no landing spot. Surviving the attack is still guaranteed.
        if (escape == null) {
            escape = origin;
            DebugEyeDiagnostics.note(victim, "no_escape");
        }
        int cost = Math.min(victimLuck, 10 + victim.getRandom().nextInt(6));
        victim.setHealth(Math.max(0.1F, victim.getMaxHealth() * 0.125F));
        victim.stopRiding();
        victim.removeEffect(com.example.immortal_cultivation_mod.effect.ModEffects.DINGSHEN);
        victim.removeEffect(com.example.immortal_cultivation_mod.effect.ModEffects.DAZE);
        victim.removeEffect(com.example.immortal_cultivation_mod.effect.ModEffects.FROZEN);
        com.example.immortal_cultivation_mod.item.FuchenItem.releaseBoundTarget(victim);
        victim.teleportTo(escape.x, escape.y, escape.z);
        victim.setDeltaMovement(Vec3.ZERO);
        victim.hurtMarked = true;
        victim.fallDistance = 0;
        victim.invulnerableTime = 40;
        victim.clearFire();
        // Old saves can contain this cooldown; it no longer gates or delays a rescue.
        data(victim).remove("RescueUntil");
        change(victim, -cost);
        int mode = victim.getRandom().nextInt(3);
        level.sendParticles(mode == 0 ? ParticleTypes.CLOUD : mode == 1 ? ParticleTypes.ENCHANT : ParticleTypes.END_ROD,
                origin.x, origin.y + 1, origin.z, 65, 0.6, 1, 0.6, 0.12);
        // Fortune rescue has its own gold threads; it never borrows the totem burst.
        var gold=new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(1F,.72F,.22F),1.15F);
        for(int i=0;i<40;i++) {
            double angle=i*Math.PI/10, radius=.65+.2*Math.sin(i*.6);
            level.sendParticles(gold,escape.x+Math.cos(angle)*radius,escape.y+.15+i*.045,
                    escape.z+Math.sin(angle)*radius,1,0,0,0,0);
        }
        if (victim instanceof ServerPlayer player) {
            com.example.immortal_cultivation_mod.combat.CombatStateManager.clear(player);
            player.sendSystemMessage(Component.translatable("luck.immortal_cultivation_mod.miracle." + mode));
        }
        event.setCanceled(true);
        DebugEyeDiagnostics.note(victim, "rescued", cost, escape.toString());
    }
    public static Vec3 safeEscape(LivingEntity entity, Vec3 danger) {
        ServerLevel level = (ServerLevel)entity.level();
        Vec3 away = entity.position().subtract(danger).multiply(1, 0, 1).normalize();
        double phase = Math.atan2(away.z, away.x);
        for (int i = 0; i < 48; i++) {
            double a = phase + i * Math.PI / 12, r = 4 + i / 12;
            for (int dy : new int[]{0, 1, -1, 2, -2}) {
                Vec3 point = entity.position().add(Math.cos(a) * r, dy, Math.sin(a) * r);
                point = new Vec3(point.x, Math.floor(point.y), point.z);
                BlockPos feet = BlockPos.containing(point);
                var box = entity.getBoundingBox().move(point.subtract(entity.position()));
                if (!level.hasChunkAt(feet) || !level.getWorldBorder().isWithinBounds(box)
                        || !level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), net.minecraft.core.Direction.UP)
                        || !level.noCollision(entity, box) || level.containsAnyLiquid(box)) continue;
                if (BlockPos.betweenClosedStream(box.inflate(0.1)).anyMatch(pos -> {
                    var block = level.getBlockState(pos).getBlock();
                    return block == net.minecraft.world.level.block.Blocks.FIRE || block == net.minecraft.world.level.block.Blocks.SOUL_FIRE
                            || block == net.minecraft.world.level.block.Blocks.MAGMA_BLOCK || block == net.minecraft.world.level.block.Blocks.CACTUS
                            || block == net.minecraft.world.level.block.Blocks.CAMPFIRE || block == net.minecraft.world.level.block.Blocks.POWDER_SNOW;
                })) continue;
                return point;
            }
        }
        return null;
    }
}
