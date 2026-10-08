package com.example.immortal_cultivation_mod.spell;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.attachment.SpiritRoots;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.entity.CultivationNpc;
import com.example.immortal_cultivation_mod.entity.FlyingSwordEntity;
import com.example.immortal_cultivation_mod.entity.NpcProfile;
import com.example.immortal_cultivation_mod.entity.NpcSpellcasting;
import com.example.immortal_cultivation_mod.network.CultivationFlightPayloads;
import com.example.immortal_cultivation_mod.physics.CultivationFlightTrail;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid=ImmortalCultivationMod.MODID)
public final class CultivationFlightVisuals {
    public static final float MIN_TRAIL_FLYING_SPEED = 0.05F * 5;
    // Non-player glide movement has no ability slider: convert default x2 travel speed to x5.
    public static final double MIN_TRAIL_TRAVEL_SPEED = JindanCreativeFlight.BASE_TRAVEL_SPEED
            * MIN_TRAIL_FLYING_SPEED / JindanCreativeFlight.defaultFlyingSpeed();
    private static final Map<LivingEntity, Motion> MOTION = new WeakHashMap<>();
    private static final class Motion {
        Vec3 previous;
        ResourceLocation dimension;
        boolean emitting;
        float speed;
    }
    private CultivationFlightVisuals() {}

    /** Shared by every flight skill, independent of Jindan flight ownership or realm. */
    public static boolean eligible(LivingEntity flyer) {
        if (!flyer.isAlive() || flyer.isInvisible() || flyer.isSpectator()) return false;
        if (flyer.getVehicle() != null) {
            // The rider's onGround flag can remain set while the sword is carrying them.
            return flyer.getVehicle() instanceof FlyingSwordEntity sword && !sword.onGround();
        }
        if (flyer.onGround()) return false;
        if (flyer instanceof PathfinderMob mob && flyer instanceof CultivationNpc
                && flyer.isNoGravity() && NpcSpellcasting.hasCreativeFlight(mob)) return true;
        if (flyer.hasEffect(ModEffects.YUFENG_JUE) || flyer.hasEffect(ModEffects.FROST_FLIGHT)) return true;
        if (flyer instanceof Player player) {
            // Also accepts other skills/mods granting survival flight, without decorating vanilla creative flight.
            return !player.isCreative() && player.getAbilities().mayfly && player.getAbilities().flying;
        }
        return flyer.isNoGravity() && UndeadControl.hasUndeadPowers(flyer)
                && UndeadControl.isAtLeastRank(flyer, "飞僵");
    }

    public static boolean shouldEmit(LivingEntity flyer, double travelled) {
        if (!eligible(flyer) || !Double.isFinite(travelled) || travelled < 0
                || travelled > CultivationFlightTrail.MAX_STEP) return false;
        // NPCs keep their elemental wake at any flight speed, including a hovering aura.
        if (flyer instanceof CultivationNpc) return true;
        if (travelled < .05) return false;
        if (flyer instanceof Player player && player.getAbilities().flying) {
            float speed = player.getAbilities().getFlyingSpeed();
            return Float.isFinite(speed) && speed + 0.000001F >= MIN_TRAIL_FLYING_SPEED;
        }
        return travelled + 0.000001 >= MIN_TRAIL_TRAVEL_SPEED;
    }

    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        update(player);
    }

    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void tickMob(EntityTickEvent.Post event) {
        if (!event.getEntity().level().isClientSide && event.getEntity() instanceof LivingEntity living
                && !(living instanceof Player)) update(living);
    }

    private static void update(LivingEntity flyer) {
        boolean flying = eligible(flyer);
        Motion motion = MOTION.get(flyer);
        if (motion == null) {
            if (!flying) return;
            motion = new Motion();
            MOTION.put(flyer, motion);
        }
        var dimension = flyer.level().dimension().location();
        if (!dimension.equals(motion.dimension)) { motion.previous = null; motion.emitting = false; motion.speed = 0; motion.dimension = dimension; }
        Vec3 position = flyer.position();
        double step = motion.previous == null ? 0 : position.distanceTo(motion.previous);
        motion.previous = position;
        boolean active = shouldEmit(flyer, step);
        motion.speed += ((active ? (float) step : 0) - motion.speed) * .35F;
        if (active && (!motion.emitting || flyer.tickCount % 4 == 0)) {
            int color = color(flyer);
            float strength = Mth.clamp(.65F + motion.speed * .5F, .65F, 1.7F);
            send(flyer, color, strength);
        } else if (!active && motion.emitting) send(flyer, color(flyer), 0);
        motion.emitting = active;
        if (!flying) MOTION.remove(flyer);
    }

    public static int color(LivingEntity flyer) {
        if (flyer.hasEffect(ModEffects.FROST_FLIGHT)) return CultivationFlightTrail.color(SpiritRoots.ICE);
        java.util.List<String> roots = java.util.List.of();
        String methodId = "";
        if (flyer instanceof Player player) {
            var data = ModAttachments.getData(player);
            roots = data.spiritRoots();
            methodId = data.activeCultivationMethod();
        } else if (flyer instanceof PathfinderMob mob && flyer instanceof CultivationNpc npc) {
            roots = NpcProfile.spiritRootIds(mob, npc.npcArchetype());
            methodId = NpcProfile.methodId(mob, npc.npcArchetype());
        } else if (UndeadControl.hasUndeadPowers(flyer)) return CultivationFlightTrail.color(SpiritRoots.DARK);
        String element = roots.stream().filter(root -> !root.equals(SpiritRoots.NONE)).findFirst().orElse("");
        if (element.isBlank()) {
            var method = CultivationMethods.get(methodId);
            if (method != null) element = method.element();
        }
        return CultivationFlightTrail.color(element);
    }

    private static void send(LivingEntity flyer, int color, float strength) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(flyer, new CultivationFlightPayloads.Trail(
                flyer.level().dimension().location(), flyer.getId(), flyer.getUUID(), color, strength));
    }
}
