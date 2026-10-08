package com.example.immortal_cultivation_mod.fate;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.combat.CombatStateManager;
import com.example.immortal_cultivation_mod.item.UtilityTalismanItem;
import com.example.immortal_cultivation_mod.network.DebugEyePayloads;
import com.example.immortal_cultivation_mod.network.ModPayloads;
import com.example.immortal_cultivation_mod.spell.PlayerShieldManager;
import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Bounded, in-memory diagnostics. Observing a roll never consumes randomness or modifies fortune. */
@EventBusSubscriber(modid = ImmortalCultivationMod.MODID)
public final class DebugEyeDiagnostics {
    private static final Map<ServerPlayer, Session> SESSIONS = new WeakHashMap<>();
    private static final class Session {
        final Deque<Component> history = new ArrayDeque<>();
        final Map<String, int[]> rolls = new TreeMap<>();
    }
    private DebugEyeDiagnostics() {}
    public static Component text(String key, Object... args) {
        // Numeric translation arguments are decoded as doubles by the component codec. Format them
        // before transmission so counters/ticks retain the exact same displayed value on the client.
        Object[] supported = Arrays.stream(args).map(value -> value instanceof Component
                || value instanceof String ? value : String.valueOf(value)).toArray();
        return Component.translatable("debug.immortal_cultivation_mod." + key, supported);
    }
    public static String number(double n) { return String.format(Locale.ROOT, "%.2f", n); }
    public static String percent(double n) { return number(n * 100) + "%"; }
    public static void note(LivingEntity entity, String key, Object... args) {
        if (entity instanceof ServerPlayer player && ModPayloads.isDebugEyeEnabled(player)) {
            var history = SESSIONS.computeIfAbsent(player, ignored -> new Session()).history;
            history.addFirst(Component.literal("[" + player.level().getGameTime() + "] ").append(text(key, args)));
            while (history.size() > 64) history.removeLast();
        }
    }
    /** Receives the existing random draw, never draws a second number for debugging. */
    public static boolean roll(LivingEntity owner, String kind, double baseline, double adjusted, double draw) {
        boolean passed = draw < adjusted;
        if (owner instanceof ServerPlayer player && ModPayloads.isDebugEyeEnabled(player)) {
            int[] counts = SESSIONS.computeIfAbsent(player, ignored -> new Session()).rolls
                    .computeIfAbsent(kind, ignored -> new int[4]);
            counts[0]++; if (draw < baseline) counts[1]++; if (passed) counts[2]++;
            boolean changed = (draw < baseline) != passed;
            if (changed) counts[3]++;
            note(player, "roll", text("roll." + kind), percent(baseline), percent(adjusted), percent(draw),
                    text(passed ? "pass" : "miss"), text(changed ? "changed" : "same"));
        }
        return passed;
    }
    public static void sync(ServerPlayer player) {
        if (!ModPayloads.isDebugEyeEnabled(player)) { SESSIONS.remove(player); return; }
        SESSIONS.computeIfAbsent(player, ignored -> new Session());
        PacketDistributor.sendToPlayer(player, snapshot(player));
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) return;
        if (!ModPayloads.isDebugEyeEnabled(player)) {
            if (SESSIONS.remove(player) != null) ModPayloads.syncDebugEyeState(player);
            return;
        }
        sync(player);
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { SESSIONS.remove(event.getEntity()); }
    public static Component explorationGate(ServerPlayer player) {
        var fate = player.getPersistentData().getCompound(LuckSystem.DATA);
        if (!player.isAlive()) return text("gate.dead");
        if (player.isSpectator()) return text("gate.spectator");
        if (player.isCreative()) return text("gate.creative");
        if (fate.contains("LastExplore")) {
            double moved = Math.sqrt(player.blockPosition().distSqr(BlockPos.of(fate.getLong("LastExplore"))));
            if (moved < 32) return text("gate.movement", number(moved));
        }
        if (!fate.contains("NextCheck")) return text("gate.initializing");
        if (fate.getLong("NextCheck") > player.level().getGameTime()) return text("gate.timer", remaining(fate, "NextCheck", player));
        return text("gate.ready");
    }
    private static String remaining(CompoundTag tag, String key, LivingEntity entity) {
        if (!tag.contains(key)) return "—";
        return number(Math.max(0, tag.getLong(key) - entity.level().getGameTime()) / 20.0) + "s";
    }
    private static Component header(String key) { return text(key).copy().withStyle(ChatFormatting.GOLD); }
    public static DebugEyePayloads.Snapshot snapshot(ServerPlayer player) {
        var fate = player.getPersistentData().getCompound(LuckSystem.DATA);
        var data = ModAttachments.getData(player);
        int luck = data.luck();
        int temporary = UtilityTalismanItem.activeLuckBonus(player);
        double factor = LuckRules.rareMultiplier(luck), bad = LuckRules.misfortuneChance(luck);
        long now = player.level().getGameTime();
        boolean avoiding = fate.getLong("AvoidUntil") > now;
        List<Component> hud = new ArrayList<>(), fortune = new ArrayList<>(), stats = new ArrayList<>();
        hud.add(text("luck", luck, LuckSystem.state(luck), temporary, luck - temporary, LuckSystem.karma(player)));
        hud.add(text("chances", number(factor), percent(LuckRules.opportunityChance(luck)), percent(bad)));
        hud.add(explorationGate(player));
        hud.add(text("next_check", remaining(fate, "NextCheck", player)));
        hud.add(text("active_encounter", fate.contains("Encounter") ? fate.getCompound("Encounter").getString("Kind") : "—"));
        fortune.addAll(hud);
        fortune.add(text("probability_note"));
        fortune.add(text("loot_scope"));
        fortune.add(text("example_drop", percent(LuckRules.rareChance(.05, luck)), (luck - 50) / 10));
        fortune.add(text("craft_chances", percent(FateCrafting.QUALITY_CHANCE * factor), percent(FateCrafting.MUTATION_CHANCE * factor)));
        fortune.add(text("pill_chances", percent(FateCrafting.QUALITY_CHANCE * factor), percent(FateCrafting.EXTRA_PILL_CHANCE * factor), percent(FateCrafting.PATTERN_CHANCE * factor)));
        fortune.add(text("alchemy_skill", data.methodProficiency("craft_alchemy"), percent(Math.min(1, .8 + data.methodProficiency("craft_alchemy") * .005))));
        fortune.add(text("avoidance", avoiding, remaining(fate, "AvoidUntil", player),
                percent(avoiding ? LuckRules.avoidanceChance(bad, 0) : bad), percent(avoiding ? LuckRules.avoidanceChance(bad, 1) : bad),
                percent(avoiding ? LuckRules.avoidanceChance(bad, 2) : bad)));
        fortune.add(text("drift", remaining(fate, "NextDrift", player), remaining(fate, "ScarUntil", player), fate.getLong("TrendUntil") > now ? fate.getInt("Trend") : 0));
        fortune.add(text("lamp", remaining(fate, "LampAnchorUntil", player), remaining(fate, "NextLampGain", player)));
        fortune.add(text("rescue"));
        fortune.add(text("reckoning", LuckSystem.karma(player) >= 100, remaining(fate, "NextReckoning", player)));
        for (String id : List.of(FateTechniques.THEFT, FateTechniques.AVOID, FateTechniques.SIGHT))
            fortune.add(text("cooldown", Component.translatable("spell.immortal_cultivation_mod." + id), remaining(fate, "Cooldown_" + id, player)));
        fortune.add(text("coin", top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player)
                .flatMap(h -> h.findFirstCurio(com.example.immortal_cultivation_mod.item.ModItems.FORTUNE_GUARD_COIN.get())).isPresent()));
        fortune.add(header("state_detail"));
        for (String key : List.of("Channel", "Alchemy", "Encounter")) {
            fortune.add(Component.literal(key));
            if (fate.contains(key)) flatten(fortune, "", fate.get(key), 0); else fortune.add(text("none"));
        }
        fortune.add(Component.literal("Five Mountains"));
        Tag wards = player.getPersistentData().get(ImmortalCultivationMod.MODID + ":five_mountains_luck");
        if (wards != null) flatten(fortune, "", wards, 0); else fortune.add(text("none"));
        stats.add(text("vitals", number(player.getHealth()), number(player.getMaxHealth()), data.qi(),
                CultivationLevels.getLevelDef(data.cultivationLevel()).maxQi() + data.maxQiBonus(), number(PlayerShieldManager.totalAmount(player))));
        stats.add(text("posture", number(CombatStateManager.peekPosture(player)), number(CombatStateManager.maxPosture(player)),
                CombatStateManager.isPostureBroken(player)));
        stats.add(text("location", player.level().dimension().location(), player.blockPosition().toShortString(), player.gameMode.getGameModeForPlayer().getName(), now));
        stats.add(header("cultivation"));
        ModPayloads.ModPayloadsHandler.debugSpellCooldowns(player).forEach((id, ticks) ->
                stats.add(text("cooldown", id, number(ticks / 20.0) + "s")));
        ModAttachments.CultivationData.CODEC.encodeStart(NbtOps.INSTANCE, data).result().ifPresent(tag -> flatten(stats, "", tag, 0));
        stats.add(header("attributes"));
        for (var attribute : BuiltInRegistries.ATTRIBUTE.holders().toList()) {
            var instance = player.getAttribute(attribute);
            if (instance != null) {
                stats.add(Component.literal(attribute.key().location() + ": " + number(instance.getValue()) + " (base " + number(instance.getBaseValue()) + ")"));
            }
        }
        stats.add(header("effects"));
        for (var effect : player.getActiveEffects()) stats.add(Component.literal(
                BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value()) + " x" + (effect.getAmplifier() + 1) + " " + effect.getDuration() + " ticks"));
        List<Component> target = new ArrayList<>();
        LivingEntity looked = FateTechniques.lookedAt(player, 32);
        if (looked == null) target.add(text("target_none"));
        else {
            target.add(looked.getDisplayName());
            target.add(Component.literal(BuiltInRegistries.ENTITY_TYPE.getKey(looked.getType()) + " | " + looked.getUUID()));
            int targetLuck = LuckSystem.luck(looked);
            target.add(text("target_stats", number(player.distanceTo(looked)), number(looked.getHealth()), number(looked.getMaxHealth()), targetLuck, LuckSystem.karma(looked)));
            target.add(text("target_fate", luck - targetLuck, LuckRules.rescueEligible(luck, targetLuck),
                    LuckRules.rescueEligible(targetLuck, luck), LuckRules.theftAmount(luck, targetLuck, LuckSystem.boss(looked))));
            target.add(text("target_note"));
            target.add(Component.literal("boss=" + LuckSystem.boss(looked) + " scar=" + LuckSystem.scarred(looked)
                    + " tags=" + looked.getTags()));
            if (looked instanceof net.minecraft.world.entity.PathfinderMob mob
                    && mob instanceof com.example.immortal_cultivation_mod.entity.CultivationNpc) {
                target.addAll(com.example.immortal_cultivation_mod.entity.NpcCultivation.debugLines(mob));
            }
            flatten(target, "fate", looked.getPersistentData().getCompound(LuckSystem.DATA), 0);
        }
        List<Component> events = new ArrayList<>(), raw = new ArrayList<>();
        var session = SESSIONS.get(player);
        events.add(text("history_note"));
        if (session != null) {
            session.rolls.forEach((key, counts) -> events.add(text("roll_counts", text("roll." + key), counts[0], counts[1], counts[2], counts[3])));
            events.addAll(session.history);
        }
        raw.add(text("raw_note"));
        flatten(raw, "fate", fate, 0);
        raw.add(header("held_item"));
        raw.add(player.getMainHandItem().getHoverName());
        flatten(raw, "item", FateCrafting.tags(player.getMainHandItem()), 0);
        raw.add(header("persistent"));
        var persistent = player.getPersistentData().copy();
        persistent.remove(LuckSystem.DATA);
        flatten(raw, "player", persistent, 0);
        if (session != null && !session.history.isEmpty()) hud.add(session.history.getFirst());
        return new DebugEyePayloads.Snapshot(now, bounded(hud), bounded(fortune), bounded(stats), bounded(target), bounded(events), bounded(raw));
    }
    private static List<Component> bounded(List<Component> lines) { return List.copyOf(lines.subList(0, Math.min(240, lines.size()))); }
    private static void flatten(List<Component> lines, String path, Tag tag, int depth) {
        if (tag == null || lines.size() >= 230) return;
        if (tag instanceof CompoundTag compound && depth < 5) {
            for (String key : new TreeSet<>(compound.getAllKeys())) flatten(lines, path.isEmpty() ? key : path + "." + key, compound.get(key), depth + 1);
        } else {
            String value = tag.toString();
            if (value.length() > 512) value = value.substring(0, 512) + " …";
            lines.add(Component.literal(path + " = " + value));
        }
    }
}
