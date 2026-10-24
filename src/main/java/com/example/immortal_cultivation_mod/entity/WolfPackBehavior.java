package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.WolfVariant;
import net.minecraft.world.entity.animal.WolfVariants;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.SpawnClusterSizeEvent;

/** Shared by every Wolf variant, without replacing its model, tame behavior or attributes. */
@EventBusSubscriber(modid = ImmortalCultivationMod.MODID)
public final class WolfPackBehavior {
    public static final int NATURAL_PACK_SIZE = 5;
    public static final int MAX_COMBAT_PACK_SIZE = 12;
    public static final String PACK_ID = "CultivationWolfPack";
    private WolfPackBehavior() {}

    public static int realm(Wolf wolf) {
        return wolf instanceof RealmBeast beast ? beast.beastRealm() : 0;
    }

    public static boolean wildAdult(Wolf wolf) {
        return wolf.isAlive() && !wolf.isTame() && !wolf.isBaby() && !wolf.isNoAi()
                && !wolf.isLeashed() && !wolf.isPassenger() && !wolf.isOrderedToSit();
    }

    public static boolean validTarget(Wolf wolf, LivingEntity target) {
        return target != null && target.isAlive() && !target.isSpectator()
                && !(target instanceof Player player && player.isCreative())
                && !(target instanceof Wolf) && !wolf.isAlliedTo(target) && wolf.canAttack(target)
                && (wolf.level().getDifficulty() != Difficulty.PEACEFUL || !(target instanceof Player));
    }

    private static final Comparator<Wolf> LEADER_ORDER = Comparator
            .comparingInt(WolfPackBehavior::realm).reversed()
            .thenComparing(Comparator.comparingDouble(Wolf::getMaxHealth).reversed())
            .thenComparing(Wolf::getUUID);

    /** At most one local entity query per active wolf per second; no world-wide registry. */
    public static List<Wolf> combatPack(Wolf wolf, LivingEntity target) {
        return wolf.level().getEntitiesOfClass(Wolf.class, target.getBoundingBox().inflate(24),
                other -> wildAdult(other) && !BeastDazeControl.blocksMoves(other)
                        && other.distanceToSqr(target) <= 24 * 24
                        && (other.getTarget() == target || other == wolf))
                .stream().sorted(LEADER_ORDER).limit(MAX_COMBAT_PACK_SIZE).toList();
    }

    public static Wolf leader(List<Wolf> pack) {
        return pack.stream().min(LEADER_ORDER).orElse(null);
    }

    /** One attacker in small packs, two in large packs; the others hold escape lanes. */
    public static boolean attackTurn(List<Wolf> pack, Wolf wolf, long tick) {
        if (pack.size() <= 1) return true;
        int count = 0, slot = -1;
        for (Wolf member : pack) {
            if (!wildAdult(member) || BeastDazeControl.blocksMoves(member)
                    || member.getHealth() <= member.getMaxHealth() * .35F) continue;
            if (member == wolf) slot = count;
            count++;
        }
        if (slot < 0) return false;
        int first = (int)Math.floorMod(tick / 60, count);
        int attackers = count >= 5 ? 2 : 1;
        return Math.floorMod(slot - first, count) < attackers;
    }

    /** Measured motion predicts a short escape lead; teleports must never produce huge cutoffs. */
    public static Vec3 escapeLead(Vec3 motionPerTick) {
        Vec3 flat = motionPerTick.multiply(1, 0, 1).scale(8);
        return flat.lengthSqr() > 6.25 ? flat.normalize().scale(2.5) : flat;
    }

    /** Calls recruit loaded, idle wolves only. They never spawn reinforcements or steal other fights. */
    public static int rally(Wolf caller, LivingEntity target) {
        if (!wildAdult(caller) || BeastDazeControl.blocksMoves(caller) || !validTarget(caller, target)
                || !caller.hasLineOfSight(target)) return 0;
        var nearby = caller.level().getEntitiesOfClass(Wolf.class, caller.getBoundingBox().inflate(24),
                other -> wildAdult(other) && other.distanceToSqr(caller) <= 24 * 24
                        && validTarget(other, target) && !BeastDazeControl.blocksMoves(other));
        int count = (int)nearby.stream().filter(w -> w.getTarget() == target).count();
        int recruited = 0;
        for (Wolf other : nearby.stream().sorted(LEADER_ORDER).toList()) {
            if (count >= MAX_COMBAT_PACK_SIZE) break;
            if (other.getTarget() != null && other.getTarget().isAlive()) continue;
            other.setTarget(target);
            if (target instanceof Player) {
                other.setPersistentAngerTarget(target.getUUID());
                other.startPersistentAngerTimer();
            }
            count++;
            recruited++;
        }
        return recruited;
    }

    /** Slots turn with the prey's travel direction. Larger packs add a rear guard and interception. */
    public static Vec3 formationSlot(Vec3 target, Vec3 travel, int size, int role) {
        if (size <= 1 || role == 0) return target;
        Vec3 forward = new Vec3(travel.x, 0, travel.z);
        forward = forward.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : forward.normalize();
        Vec3 side = new Vec3(-forward.z, 0, forward.x);
        if (size == 2) return target.add(side.scale(2.8)).add(forward.scale(1.2));
        if (role == 1) return target.add(side.scale(3.1)).add(forward.scale(1.5));
        if (role == 2) return target.add(side.scale(-3.1)).add(forward.scale(1.5));
        if (role == 3) return target.add(forward.scale(4.5));
        if (role == 4) return target.add(forward.scale(-3.5));
        double angle = (role - 5) * Math.PI * 2 / Math.max(1, size - 5) + Math.PI / 4;
        return target.add(forward.scale(Math.cos(angle) * 5)).add(side.scale(Math.sin(angle) * 5));
    }

    @SubscribeEvent public static void joined(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof Wolf wolf)) return;
        if (wolf.goalSelector.getAvailableGoals().stream().anyMatch(g -> g.getGoal() instanceof WolfPackGoal)) return;
        wolf.goalSelector.addGoal(3, new WolfPackGoal(wolf));
        wolf.goalSelector.addGoal(7, new WolfPackFollowGoal(wolf));
        wolf.targetSelector.addGoal(4, new WolfPackHuntGoal(wolf));
    }

    @SubscribeEvent public static void cluster(SpawnClusterSizeEvent event) {
        if (event.getEntity() instanceof Wolf) event.setSize(NATURAL_PACK_SIZE);
    }

    public static final class PackSpawnData extends Wolf.WolfPackData {
        public final UUID id = UUID.randomUUID();
        public PackSpawnData(Holder<WolfVariant> variant) { super(variant); }
    }

    @SubscribeEvent public static void naturalPack(FinalizeSpawnEvent event) {
        if (!(event.getEntity() instanceof Wolf wolf)
                || (event.getSpawnType() != MobSpawnType.NATURAL
                    && event.getSpawnType() != MobSpawnType.CHUNK_GENERATION)) return;
        PackSpawnData pack = event.getSpawnData() instanceof PackSpawnData existing ? existing
                : new PackSpawnData(event.getSpawnData() instanceof Wolf.WolfPackData vanilla ? vanilla.type
                        : WolfVariants.getSpawnVariant(wolf.registryAccess(), event.getLevel().getBiome(wolf.blockPosition())));
        event.setSpawnData(pack);
        wolf.getPersistentData().putUUID(PACK_ID, pack.id);
    }
}
