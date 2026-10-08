package com.example.immortal_cultivation_mod.combat;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Control pressure drains one Thought per second. Only an empty bar permits the attempted restriction. */
@EventBusSubscriber(modid = ImmortalCultivationMod.MODID)
public final class ThoughtControlGuard {
    public static final int BASE_DAMAGE = 1;
    public static final int REFRESH_INTERVAL = 20;
    public static final TagKey<MobEffect> CONTROL_EFFECTS = TagKey.create(Registries.MOB_EFFECT,
            ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "thought_control_effects"));
    public static final TagKey<MobEffect> EXEMPT_EFFECTS = TagKey.create(Registries.MOB_EFFECT,
            ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "thought_control_exempt"));
    private static final Pattern CONTROL_NAME = Pattern.compile(
            "(^|_)(stun(?:ned|ning)?|daze(?:d)?|paraly[sz](?:ed|is|e|ing)|frozen|freeze|freezing|"
            + "immobili[sz](?:ed|ation|e)|immobile|root(?:ed)?|petrif(?:ied|ication|y)|incapacitated|silenc(?:e|ed)|"
            + "disarm(?:ed)?|sleep|asleep|unconscious|knockout|stagger(?:ed)?|fear|terrified|charm(?:ed)?|"
            + "confusion|inverted_control|mind_control|time_stop|time_stopped|time_lock|action_lock|"
            + "input_lock|keybind_lock|control_lock|movement_lock|spell_lock|attack_lock|stasis|"
            + "no_controls|no_input|no_movement|no_attack|no_cast|jump_disabled|bind|binding)(_|$)");
    private static final class Pressure {
        long until, lastDrain;
        boolean charged;
    }
    // All control sources share one clock; refreshing an effect only extends its pressure window.
    private static final Map<Player, Pressure> PRESSURE = new WeakHashMap<>();
    private static final Set<Player> APPLYING_EXHAUSTION = Collections.newSetFromMap(new WeakHashMap<>());
    private static final String EXHAUSTION_DAZE = ImmortalCultivationMod.MODID + ":thought_exhaustion_daze";
    // Installed by the client entry point; common/server code never loads a Minecraft client class.
    public static Predicate<Player> clientHasThoughts = player -> false;

    private ThoughtControlGuard() {}

    public static boolean hasProtection(LivingEntity entity) {
        if (!(entity instanceof Player player) || !player.isAlive()) return false;
        return player.level().isClientSide ? clientHasThoughts.test(player)
                : ModAttachments.getData(player).thoughts() > 0;
    }

    public static boolean matchesControlName(String path) {
        return CONTROL_NAME.matcher(path.toLowerCase(Locale.ROOT).replace('-', '_')).find();
    }

    public static boolean isControl(MobEffectInstance instance) {
        if (instance == null) return false;
        var holder = instance.getEffect();
        if (holder.is(EXEMPT_EFFECTS)) return false;
        if (holder.is(CONTROL_EFFECTS)) return true;
        MobEffect effect = holder.value();
        if (effect.getCategory() != MobEffectCategory.HARMFUL) return false;
        var id = BuiltInRegistries.MOB_EFFECT.getKey(effect);
        if (id != null && matchesControlName(id.getPath())) return true;
        String className = effect.getClass().getSimpleName().replaceAll("([a-z])([A-Z])", "$1_$2");
        if (matchesControlName(className)) return true;
        // This also detects Slowness VII+ and unnamed third-party effects that zero movement speed.
        boolean[] immobilizes = {false};
        effect.createModifiers(instance.getAmplifier(), (attribute, modifier) -> {
            if (attribute.equals(Attributes.MOVEMENT_SPEED)
                    && modifier.amount() <= (modifier.operation() == AttributeModifier.Operation.ADD_VALUE ? -.1 : -1))
                immobilizes[0] = true;
        });
        return immobilizes[0];
    }

    public static boolean shouldSuppress(LivingEntity entity, MobEffectInstance instance) {
        return hasProtection(entity) && isControl(instance);
    }

    /** Compatibility entry point for instantaneous restraints. Control damage is always fixed, independent of attack stats. */
    public static boolean resist(LivingEntity target, String sourceId, Entity attacker, int baseDamage) {
        return resistForDuration(target, sourceId, attacker, 1);
    }

    /** Shared entry point for timed restraints. Durations overlap; they never multiply the drain. */
    public static boolean resistForDuration(LivingEntity target, String sourceId, Entity attacker, int durationTicks) {
        if (!(target instanceof Player player) || !player.isAlive()) return false;
        boolean protectedNow = hasProtection(player);
        if (player.level().isClientSide || durationTicks == 0) return protectedNow;
        long now = player.level().getGameTime();
        Pressure pressure = remember(player, durationTicks, now);
        if (protectedNow) drain(player, pressure, now);
        else if (!pressure.charged) {
            pressure.charged = true;
            pressure.lastDrain = now;
        }
        return hasProtection(player);
    }

    public static boolean resistEffect(LivingEntity target, MobEffectInstance instance, Entity source) {
        if (!isControl(instance)) return false;
        if (target instanceof Player player && !player.level().isClientSide && instance.getEffect().value() == ModEffects.DAZE.get()) {
            // The empty-bar penalty is not an incoming control attack and must not drain recovery.
            if (APPLYING_EXHAUSTION.contains(player)) return false;
        }
        ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(instance.getEffect().value());
        return resistForDuration(target, "effect/" + id, source, instance.getDuration());
    }

    public static void applyExhaustion(Player player) {
        player.getPersistentData().putLong(EXHAUSTION_DAZE, player.level().getGameTime() + 40);
        APPLYING_EXHAUSTION.add(player);
        try {
            player.addEffect(new MobEffectInstance(ModEffects.DAZE, 40, 0, false, true, true));
        } finally {
            APPLYING_EXHAUSTION.remove(player);
        }
    }

    private static Pressure remember(Player player, int durationTicks, long now) {
        Pressure pressure = PRESSURE.computeIfAbsent(player, ignored -> new Pressure());
        if (pressure.charged && now < pressure.lastDrain) {
            pressure.until = now;
            pressure.lastDrain = now;
        }
        long until = durationTicks < 0 ? Long.MAX_VALUE : now + durationTicks;
        pressure.until = Math.max(pressure.until, until);
        return pressure;
    }

    private static void drain(Player player, Pressure pressure, long now) {
        if (!pressure.charged || now - pressure.lastDrain >= REFRESH_INTERVAL) {
            pressure.charged = true;
            pressure.lastDrain = now;
            ServerEvents.adjustThoughts(player, -BASE_DAMAGE);
            if (player instanceof net.minecraft.server.level.ServerPlayer server) { com.example.immortal_cultivation_mod.guidance.DaoGuidanceObservations.record(server, "control_pressure"); com.example.immortal_cultivation_mod.guidance.DaoGuidanceObservations.record(server, "thought_spent"); }
        }
    }

    /** Tick attempted control durations even when the actual effect was resisted and never installed. */
    public static void tick(Player player) {
        if (player.level().isClientSide) return;
        if (!player.isAlive()) {
            PRESSURE.remove(player);
            return;
        }
        releaseRecoveredControls(player);
        Pressure pressure = PRESSURE.get(player);
        if (pressure == null) return;
        long now = player.level().getGameTime();
        if (now < pressure.lastDrain) {
            PRESSURE.remove(player);
        } else if (now >= pressure.until) {
            // Keep the most recent charge through its entire cooldown, including short stuns.
            if (now - pressure.lastDrain >= REFRESH_INTERVAL) PRESSURE.remove(player);
        } else if (hasProtection(player)) {
            drain(player, pressure, now);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEffect(MobEffectEvent.Applicable event) {
        if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof Player)
                || event.getResult() == MobEffectEvent.Applicable.Result.DO_NOT_APPLY) return;
        // Immunity rejected by vanilla/another mod must not charge Thoughts.
        if (event.getResult() == MobEffectEvent.Applicable.Result.DEFAULT
                && !event.getEntity().canBeAffected(event.getEffectInstance())) return;
        if (resistEffect(event.getEntity(), event.getEffectInstance(), event.getEffectSource()))
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void beforePlayerTick(PlayerTickEvent.Pre event) {
        tick(event.getEntity());
    }

    /** Loaded effects and recovery from zero cannot leave a stale input lock behind. */
    public static void releaseRecoveredControls(Player player) {
        if (player.level().isClientSide || !hasProtection(player)) return;
        for (MobEffectInstance effect : new ArrayList<>(player.getActiveEffects())) {
            if (isControl(effect)) {
                long now = player.level().getGameTime();
                long exhaustionUntil = player.getPersistentData().getLong(EXHAUSTION_DAZE);
                boolean exhaustion = effect.getEffect().value() == ModEffects.DAZE.get()
                        && effect.getAmplifier() == 0 && !effect.isInfiniteDuration()
                        // Effect duration is decremented during the entity tick, after PlayerTick.Pre.
                        && exhaustionUntil >= now && effect.getDuration() <= exhaustionUntil - now + 1;
                if (!exhaustion && effect.getDuration() != 0) {
                    Pressure pressure = remember(player, effect.getDuration(), now);
                    // Recovery lifts the lock immediately without consuming the newly recovered Thought.
                    if (!pressure.charged) {
                        pressure.charged = true;
                        pressure.lastDrain = now;
                    }
                }
                player.removeEffect(effect.getEffect());
                if (effect.getEffect().value() == ModEffects.DAZE.get()) player.getPersistentData().remove(EXHAUSTION_DAZE);
            }
        }
    }
}
