package com.example.immortal_cultivation_mod.spell;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.attribute.ModAttributes;
import com.example.immortal_cultivation_mod.block.ModBlocks;
import com.example.immortal_cultivation_mod.entity.CultivatorCorpseEntity;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.world.YinQiField;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.GrowingPlantBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.WaterlilyBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class UndeadControl {
    public static final String TAG_TICKS = "icmUndeadTicks";
    public static final String TAG_GRUDGE_PROGRESS = "icmGrudgeProgress";
    public static final String TAG_GRUDGE = "icmGrudge";
    public static final String TAG_GRUDGE_ROLLED = "icmGrudgeRolled";
    public static final String TAG_HAS_GRUDGE = "icmHasGrudge";
    public static final String TAG_SEALED = "icmCorpseSeal";
    public static final String TAG_OWNER = "icmUndeadOwner";
    public static final String TAG_LAST_SEALED_TIME = "icmLastSealedTime";
    public static final String TAG_COMMAND_X = "icmCommandX";
    public static final String TAG_COMMAND_Y = "icmCommandY";
    public static final String TAG_COMMAND_Z = "icmCommandZ";
    public static final String TAG_HAS_COMMAND = "icmHasCommand";
    public static final String TAG_ATTACK_TARGET = "icmAttackTarget";
    public static final String TAG_NEXT_JUMP_TICK = "icmNextCommandJumpTick";
    public static final String TAG_NEXT_REPATH_TICK = "icmNextCommandRepathTick";
    public static final String TAG_REPEL_STUN_UNTIL = "icmRepelStunUntil";
    public static final String TAG_LAST_PATH_X = "icmLastPathX";
    public static final String TAG_LAST_PATH_Y = "icmLastPathY";
    public static final String TAG_LAST_PATH_Z = "icmLastPathZ";
    public static final String TAG_STUCK_TICKS = "icmPathStuckTicks";
    public static final String TAG_MODE = "icmUndeadMode";
    public static final String TAG_FENGYAN_SEALED = "icmFengyanSeal";
    public static final String MODE_NORMAL = "normal";
    public static final String MODE_ATTACK_HOSTILE = "attack_hostile";
    public static final String MODE_DEFENSIVE = "defensive";
    private static final int BELL_COOLDOWN_TICKS = 20 * 1;
    private static final int TICKS_PER_GRUDGE = 1000;
    private static final List<Rank> RANKS = List.of(
            new Rank(100L, "\u767d\u50f5", CultivationLevels.REALM_LIANQI + CultivationLevels.STAGE_EARLY, 10.0D),
            new Rank(500L, "\u9ed1\u50f5", CultivationLevels.REALM_LIANQI + CultivationLevels.STAGE_LATE, 30.0D),
            new Rank(2_500L, "\u8df3\u50f5", CultivationLevels.REALM_ZHUJI + CultivationLevels.STAGE_LATE, 50.0D),
            new Rank(12_500L, "\u98de\u50f5", CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_LATE, 200.0D),
            new Rank(62_500L, "\u6e38\u5c38", CultivationLevels.REALM_YUANYING + CultivationLevels.STAGE_LATE, 1_000.0D),
            new Rank(312_500L, "\u4f0f\u5c38", CultivationLevels.REALM_HUASHEN + CultivationLevels.STAGE_LATE, 10_000.0D),
            new Rank(1_562_500L, "\u4e0d\u5316\u9aa8", CultivationLevels.REALM_DACHENG + CultivationLevels.STAGE_LATE, 1_000_000.0D),
            new Rank(7_812_500L, "\u65f1\u9b43", CultivationLevels.REALM_DUJIE + CultivationLevels.STAGE_LATE, 5_000_000.0D)
    );

    private UndeadControl() {
    }

    public static boolean isUndeadServantType(Entity entity) {
        return entity instanceof Zombie || entity instanceof CultivatorCorpseEntity;
    }

    public static void tick(LivingEntity entity) {
        if (!isUndeadServantType(entity)) {
            return;
        }
        syncGrudgeAttributeIntoData(entity);
        if (isRepelStunned(entity)) {
            freeze(entity);
            return;
        }
        if (!hasGrudgeGrowth(entity)) {
            if (isSealed(entity)) {
                freeze(entity);
            } else {
                clearOwnerTarget(entity);
                tickCommand(entity);
            }
            return;
        }
        CompoundTag tag = entity.getPersistentData();
        double progress = tag.contains(TAG_GRUDGE_PROGRESS) ? tag.getDouble(TAG_GRUDGE_PROGRESS) : tag.getLong(TAG_TICKS);
        progress += grudgeGrowthMultiplier(entity);
        tag.putDouble(TAG_GRUDGE_PROGRESS, progress);
        long ticks = (long) Math.floor(progress);
        tag.putLong(TAG_TICKS, ticks);
        long grudge = ticks / TICKS_PER_GRUDGE;
        tag.putLong(TAG_GRUDGE, grudge);
        setBase(entity, ModAttributes.GRUDGE, grudge);
        applyRank(entity);
        tickRankPowers(entity);
        if (isSealed(entity)) {
            freeze(entity);
            return;
        }
        clearOwnerTarget(entity);
        tickCommand(entity);
    }

    public static InteractionResult interactEntity(ServerPlayer player, Entity target, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(target instanceof LivingEntity living) || !isUndeadServantType(target)) {
            return InteractionResult.PASS;
        }
        if (stack.is(ModItems.ZHENSHI_TALISMAN.get())) {
            seal(living);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.is(ModItems.BLOOD.get())) {
            return feedGrudgeItem(player, living, stack, 1L, 1_000L);
        }
        if (stack.is(ModItems.BLOOD_CRYSTAL.get())) {
            return feedGrudgeItem(player, living, stack, 5L, Long.MAX_VALUE);
        }
        if (!isOwnedBy(living, player)) {
            return InteractionResult.PASS;
        }
        if (stack.is(ModItems.FENGYAN_TALISMAN.get())) {
            applyFengyanSeal(player, living, stack);
            return InteractionResult.SUCCESS;
        }
        if (hand == InteractionHand.MAIN_HAND && stack.isEmpty() && isFengyanSealed(living)) {
            removeFengyanSeal(player, living);
            return InteractionResult.SUCCESS;
        }
        if (hand == InteractionHand.MAIN_HAND && stack.isEmpty() && player.isShiftKeyDown()) {
            return cycleMode(player, living);
        }
        if (hand == InteractionHand.MAIN_HAND && stack.isEmpty() && isSealed(living)
                && living.level().getGameTime() - living.getPersistentData().getLong(TAG_LAST_SEALED_TIME) > 2L) {
            unseal(living);
            if (!player.getInventory().add(new ItemStack(ModItems.ZHENSHI_TALISMAN.get()))) {
                player.drop(new ItemStack(ModItems.ZHENSHI_TALISMAN.get()), false);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    public static boolean useBell(ServerPlayer player) {
        if (player.getCooldowns().isOnCooldown(ModItems.ZHENHUN_BELL.get())) {
            return true;
        }
        int healed = 0;
        for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(5.0D),
                entity -> isOwnedBy(entity, player) && isUndeadServantType(entity))) {
            float amount = Math.max(1.0F, entity.getMaxHealth() * 0.05F);
            entity.heal(amount);
            healed++;
        }
        if (healed > 0) {
            player.getCooldowns().addCooldown(ModItems.ZHENHUN_BELL.get(), BELL_COOLDOWN_TICKS);
        }
        return healed > 0;
    }

    public static boolean commandToLookTarget(ServerPlayer player) {
        HitResult hit = player.pick(player.blockInteractionRange(), 0.0F, false);
        if (hit.getType() == HitResult.Type.BLOCK) {
            commandMove(player, hit.getLocation());
            return true;
        }
        return false;
    }

    public static boolean commandToBlock(ServerPlayer player, BlockPos pos) {
        commandMove(player, Vec3.atCenterOf(pos));
        return true;
    }

    public static boolean gongCommandToBlock(ServerPlayer player, BlockPos pos) {
        return commandAllNearbyMove(player, Vec3.atCenterOf(pos), 30.0D);
    }

    public static boolean gongCommandAttack(ServerPlayer player, LivingEntity target) {
        return commandAllNearbyAttack(player, target, 30.0D);
    }

    public static boolean castControl(ServerPlayer player) {
        EntityHitResult entityHit = pickEntity(player, player.entityInteractionRange(), UndeadControl::isUndeadServantType);
        if (entityHit == null || !(entityHit.getEntity() instanceof LivingEntity target)
                || !isUndeadServantType(target)) {
            return false;
        }
        return tame(player, target);
    }

    public static boolean tame(ServerPlayer player, LivingEntity target) {
        if (target.getPersistentData().hasUUID(TAG_OWNER) && !isOwnedBy(target, player)) {
            return false;
        }
        if (!canControl(player, target)) {
            return false;
        }
        releaseSealForControl(target);
        setOwner(target, player.getUUID());
        if (target instanceof CultivatorCorpseEntity corpse) {
            corpse.setRaised(true);
            corpse.setSealed(false);
        }
        if (target instanceof Mob mob) {
            mob.setNoAi(false);
            mob.setPersistenceRequired();
            mob.getNavigation().stop();
        }
        ensureActiveServantStats(target);
        return true;
    }

    public static boolean isOwnedByPlayer(LivingEntity entity, Player player) {
        return isOwnedBy(entity, player);
    }

    public static boolean defensiveCommand(ServerPlayer owner, LivingEntity attacker) {
        if (attacker.getUUID().equals(owner.getUUID())) {
            return false;
        }
        int count = 0;
        for (LivingEntity entity : nearbyOwnedUndead(owner, 30.0D)) {
            if (!isCommandBlocked(entity) && !entity.getPersistentData().getBoolean(TAG_HAS_COMMAND)
                    && MODE_DEFENSIVE.equals(mode(entity))) {
                applyAttackCommand(entity, attacker);
                count++;
            }
        }
        return count > 0;
    }

    public static boolean isSealed(LivingEntity entity) {
        if (entity instanceof CultivatorCorpseEntity corpse) {
            return corpse.isSealed();
        }
        return entity.getPersistentData().getBoolean(TAG_SEALED);
    }

    public static long grudge(LivingEntity entity) {
        return entity.getPersistentData().getLong(TAG_GRUDGE);
    }

    public static String levelFor(LivingEntity entity) {
        if (grudge(entity) < RANKS.getFirst().threshold()) {
            return CultivationLevels.REALM_MORTAL;
        }
        return rankFor(grudge(entity)).level();
    }

    public static boolean isAtLeastRank(LivingEntity entity, String rankName) {
        return grudge(entity) >= thresholdFor(rankName);
    }

    public static boolean isAtLeastHopping(LivingEntity entity) {
        return grudge(entity) >= thresholdFor("\u8df3\u50f5");
    }

    public static boolean ignoresFireAndSun(LivingEntity entity) {
        return grudge(entity) >= thresholdFor("\u9ed1\u50f5");
    }

    public static boolean isHigherThanRank(LivingEntity entity, String rankName) {
        return grudge(entity) > thresholdFor(rankName);
    }

    public static boolean ignoresNuomi(LivingEntity entity) {
        return grudge(entity) > thresholdFor("\u9ed1\u50f5");
    }

    public static boolean ignoresModouLine(LivingEntity entity) {
        return grudge(entity) > thresholdFor("\u98de\u50f5");
    }

    public static void stunFromRepel(LivingEntity entity, int ticks) {
        entity.getPersistentData().putLong(TAG_REPEL_STUN_UNTIL, entity.level().getGameTime() + ticks);
    }

    public static boolean isCommandBlocked(LivingEntity entity) {
        return isFengyanSealed(entity);
    }

    public static boolean isFengyanSealed(LivingEntity entity) {
        return entity.getPersistentData().getBoolean(TAG_FENGYAN_SEALED);
    }

    private static void seal(LivingEntity entity) {
        entity.getPersistentData().putBoolean(TAG_SEALED, true);
        entity.getPersistentData().putBoolean(TAG_FENGYAN_SEALED, false);
        entity.getPersistentData().putLong(TAG_LAST_SEALED_TIME, entity.level().getGameTime());
        entity.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.ZHENSHI_SEAL_HELMET.get()));
        if (entity instanceof CultivatorCorpseEntity corpse) {
            corpse.setRaised(true);
            corpse.setSealed(true);
        }
        if (entity instanceof Mob mob) {
            mob.setDropChance(EquipmentSlot.HEAD, 0.0F);
            mob.setNoAi(true);
            mob.getNavigation().stop();
        }
        freeze(entity);
    }

    private static void unseal(LivingEntity entity) {
        entity.getPersistentData().putBoolean(TAG_SEALED, false);
        entity.getPersistentData().putBoolean(TAG_FENGYAN_SEALED, false);
        if (isSealHelmet(entity.getItemBySlot(EquipmentSlot.HEAD))) {
            entity.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        }
        if (entity instanceof CultivatorCorpseEntity corpse) {
            corpse.setSealed(false);
        }
        if (entity instanceof Mob mob) {
            mob.setNoAi(false);
        }
    }

    private static void freeze(LivingEntity entity) {
        stopHorizontalMovement(entity);
        entity.hurtMarked = true;
        if (entity instanceof Mob mob) {
            mob.getNavigation().stop();
            mob.setTarget(null);
        }
    }

    private static boolean isRepelStunned(LivingEntity entity) {
        long until = entity.getPersistentData().getLong(TAG_REPEL_STUN_UNTIL);
        if (until <= 0L) {
            return false;
        }
        if (entity.level().getGameTime() <= until) {
            return true;
        }
        entity.getPersistentData().remove(TAG_REPEL_STUN_UNTIL);
        return false;
    }

    private static void tickCommand(LivingEntity entity) {
        if (!(entity instanceof Mob mob)) {
            return;
        }
        if (isCommandBlocked(entity)) {
            freeze(entity);
            return;
        }
        CompoundTag tag = entity.getPersistentData();
        if (tag.getBoolean(TAG_HAS_COMMAND)) {
            Vec3 pos = new Vec3(tag.getDouble(TAG_COMMAND_X), tag.getDouble(TAG_COMMAND_Y), tag.getDouble(TAG_COMMAND_Z));
            if (entity.position().distanceToSqr(pos) < 2.25D) {
                tag.putBoolean(TAG_HAS_COMMAND, false);
                resetPathStuck(tag, entity.position());
                clearCombatState(entity);
                mob.getNavigation().stop();
                stopHorizontalMovement(entity);
            } else {
                clearCombatState(entity);
                forceCommandMovement(entity, pos);
                navigateToPosition(mob, pos, tag);
                assistPathIfStuck(mob, pos, tag);
                faceToward(entity, pos);
            }
            return;
        }
        if (tag.hasUUID(TAG_ATTACK_TARGET) && entity.level() instanceof ServerLevel level) {
            Entity target = level.getEntity(tag.getUUID(TAG_ATTACK_TARGET));
            if (target instanceof LivingEntity living && living.isAlive()) {
                ensureActiveServantStats(entity);
                mob.setNoAi(false);
                mob.setTarget(living);
                navigateToTarget(mob, living, tag);
                faceToward(entity, living.position());
                assistPathIfStuck(mob, living.position(), tag);
                return;
            }
            tag.remove(TAG_ATTACK_TARGET);
        }
        if (MODE_ATTACK_HOSTILE.equals(mode(entity)) && entity.level() instanceof ServerLevel) {
            LivingEntity hostile = findNearbyHostile(entity);
            if (hostile != null) {
                applyAttackCommand(entity, hostile);
                return;
            }
        }
        if (isSealHelmet(entity.getItemBySlot(EquipmentSlot.HEAD))) {
            freeze(entity);
        }
    }

    private static void commandMove(ServerPlayer player, Vec3 pos) {
        for (LivingEntity entity : servants(player, 48.0D)) {
            if (isCommandBlocked(entity)) {
                continue;
            }
            applyMoveCommand(entity, pos);
        }
    }

    private static void commandAttack(ServerPlayer player, LivingEntity target) {
        if (target.getUUID().equals(player.getUUID())) {
            return;
        }
        for (LivingEntity entity : servants(player, 48.0D)) {
            if (isCommandBlocked(entity)) {
                continue;
            }
            applyAttackCommand(entity, target);
        }
    }

    private static boolean commandAllNearbyMove(ServerPlayer player, Vec3 pos, double range) {
        int count = 0;
        for (LivingEntity entity : nearbyOwnedUndead(player, range)) {
            if (isCommandBlocked(entity)) {
                continue;
            }
            applyMoveCommand(entity, pos);
            count++;
        }
        return count > 0;
    }

    private static void applyMoveCommand(LivingEntity entity, Vec3 pos) {
        releaseSealForControl(entity);
        CompoundTag tag = entity.getPersistentData();
        tag.putBoolean(TAG_HAS_COMMAND, true);
        tag.putDouble(TAG_COMMAND_X, pos.x);
        tag.putDouble(TAG_COMMAND_Y, pos.y);
        tag.putDouble(TAG_COMMAND_Z, pos.z);
        clearCombatState(entity);
        if (entity instanceof CultivatorCorpseEntity corpse) {
            corpse.setRaised(true);
            corpse.setSealed(false);
        }
        if (entity instanceof Mob mob) {
            ensureActiveServantStats(entity);
            mob.setNoAi(false);
            faceToward(entity, pos);
            resetPathStuck(tag, entity.position());
            forceCommandMovement(entity, pos);
            navigateToPosition(mob, pos, tag);
        }
    }

    private static void clearCombatState(LivingEntity entity) {
        entity.getPersistentData().remove(TAG_ATTACK_TARGET);
        entity.getPersistentData().remove("HurtBy");
        entity.getPersistentData().remove("HurtByTimestamp");
        entity.setLastHurtByMob(null);
        entity.setLastHurtMob(null);
        if (entity instanceof Mob mob) {
            mob.setTarget(null);
            mob.getNavigation().stop();
        }
        if (entity instanceof NeutralMob neutral) {
            neutral.setPersistentAngerTarget(null);
            neutral.setRemainingPersistentAngerTime(0);
        }
    }

    private static boolean commandAllNearbyAttack(ServerPlayer player, LivingEntity target, double range) {
        if (target.getUUID().equals(player.getUUID())) {
            return false;
        }
        int count = 0;
        for (LivingEntity entity : nearbyOwnedUndead(player, range)) {
            if (entity == target || isCommandBlocked(entity)) {
                continue;
            }
            applyAttackCommand(entity, target);
            count++;
        }
        return count > 0;
    }

    private static void applyAttackCommand(LivingEntity entity, LivingEntity target) {
        if (isCommandBlocked(entity)) {
            return;
        }
        releaseSealForControl(entity);
        entity.getPersistentData().putUUID(TAG_ATTACK_TARGET, target.getUUID());
        entity.getPersistentData().putBoolean(TAG_HAS_COMMAND, false);
        if (entity instanceof CultivatorCorpseEntity corpse) {
            corpse.setRaised(true);
            corpse.setSealed(false);
        }
        if (entity instanceof Mob mob) {
            ensureActiveServantStats(entity);
            mob.setNoAi(false);
            mob.setTarget(target);
            resetPathStuck(entity.getPersistentData(), entity.position());
            navigateToTarget(mob, target, entity.getPersistentData());
        }
    }

    private static List<LivingEntity> servants(ServerPlayer player, double range) {
        return player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range),
                entity -> isOwnedBy(entity, player) && isUndeadServantType(entity) && !isCommandBlocked(entity));
    }

    private static List<LivingEntity> nearbyOwnedUndead(ServerPlayer player, double range) {
        return player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range),
                entity -> isOwnedBy(entity, player) && isUndeadServantType(entity));
    }

    private static boolean canControl(ServerPlayer player, LivingEntity target) {
        int casterRealm = CultivationLevels.getRealmIndex(ModAttachments.getData(player).cultivationLevel());
        int targetRealm = CultivationLevels.getRealmIndex(levelFor(target));
        return targetRealm <= casterRealm + 1;
    }

    private static boolean isOwned(LivingEntity entity) {
        return entity.getPersistentData().hasUUID(TAG_OWNER);
    }

    private static boolean isOwnedBy(LivingEntity entity, Player player) {
        return entity.getPersistentData().hasUUID(TAG_OWNER)
                && entity.getPersistentData().getUUID(TAG_OWNER).equals(player.getUUID());
    }

    public static boolean isOwnedByOwnerUuid(LivingEntity entity, UUID owner) {
        return entity.getPersistentData().hasUUID(TAG_OWNER)
                && entity.getPersistentData().getUUID(TAG_OWNER).equals(owner);
    }

    private static void setOwner(LivingEntity entity, UUID owner) {
        entity.getPersistentData().putUUID(TAG_OWNER, owner);
        entity.getPersistentData().putBoolean(TAG_SEALED, false);
        if (!entity.getPersistentData().contains(TAG_MODE)) {
            entity.getPersistentData().putString(TAG_MODE, MODE_NORMAL);
        }
        if (entity instanceof CultivatorCorpseEntity corpse) {
            corpse.setRaised(true);
            corpse.setSealed(false);
        }
    }

    private static String mode(LivingEntity entity) {
        String mode = entity.getPersistentData().getString(TAG_MODE);
        if (MODE_ATTACK_HOSTILE.equals(mode) || MODE_DEFENSIVE.equals(mode)) {
            return mode;
        }
        return MODE_NORMAL;
    }

    private static InteractionResult cycleMode(ServerPlayer player, LivingEntity entity) {
        String next = switch (mode(entity)) {
            case MODE_NORMAL -> MODE_ATTACK_HOSTILE;
            case MODE_ATTACK_HOSTILE -> MODE_DEFENSIVE;
            default -> MODE_NORMAL;
        };
        entity.getPersistentData().putString(TAG_MODE, next);
        player.displayClientMessage(Component.literal("Mode: " + next), true);
        return InteractionResult.SUCCESS;
    }

    private static void applyRank(LivingEntity entity) {
        if (grudge(entity) < RANKS.getFirst().threshold()) {
            return;
        }
        Rank rank = rankFor(grudge(entity));
        var level = CultivationLevels.getLevelDef(rank.level());
        setBase(entity, Attributes.MAX_HEALTH, level.maxHp());
        setBase(entity, Attributes.ARMOR, Math.max(0.0D, CultivationLevels.getRealmIndex(rank.level()) * 4.0D));
        setBase(entity, Attributes.ATTACK_DAMAGE, rank.damage());
        setBase(entity, Attributes.MOVEMENT_SPEED, Math.min(0.38D, 0.20D + CultivationLevels.getRealmIndex(rank.level()) * 0.02D));
        setBase(entity, Attributes.FOLLOW_RANGE, 48.0D);
        if (entity.getHealth() > entity.getMaxHealth()) {
            entity.setHealth(entity.getMaxHealth());
        }
        entity.setCustomName(Component.literal(rank.name()));
        entity.setCustomNameVisible(true);
    }

    private static void tickRankPowers(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        long grudge = grudge(entity);
        if (grudge >= thresholdFor("\u767d\u50f5") && grudge < thresholdFor("\u9ed1\u50f5")) {
            tickWhiteStiffSunlight(level, entity);
        }
        if (grudge >= thresholdFor("\u9ed1\u50f5")) {
            entity.clearFire();
            entity.setRemainingFireTicks(0);
            setBaseAtLeast(entity, Attributes.MOVEMENT_SPEED, 0.32D);
        }
        if (grudge >= thresholdFor("\u98de\u50f5")) {
            tickFlyingMovement(entity);
        }
        if (grudge >= thresholdFor("\u4f0f\u5c38")) {
            YinQiField.updateAura(level, entity.getUUID(), entity.blockPosition(), yinAuraRadius(entity), 10, 60);
        }
        if (grudge >= thresholdFor("\u65f1\u9b43")) {
            dryNearbyLand(level, entity);
        }
    }

    private static int yinAuraRadius(LivingEntity entity) {
        long grudge = grudge(entity);
        if (grudge >= thresholdFor("\u65f1\u9b43")) {
            return 50;
        }
        if (grudge >= thresholdFor("\u4e0d\u5316\u9aa8")) {
            return 30;
        }
        return 10;
    }

    private static void tickWhiteStiffSunlight(ServerLevel level, LivingEntity entity) {
        if (level.isDay()
                && level.canSeeSky(entity.blockPosition())
                && level.getBrightness(net.minecraft.world.level.LightLayer.SKY, entity.blockPosition()) > 11
                && !entity.isInWaterRainOrBubble()) {
            entity.igniteForSeconds(4);
        }
    }

    private static void tickFlyingMovement(LivingEntity entity) {
        if (!(entity instanceof Mob mob) || mob.getTarget() == null || !mob.getTarget().isAlive()) {
            entity.setNoGravity(false);
            return;
        }
        LivingEntity target = mob.getTarget();
        double distance = entity.distanceTo(target);
        if (distance > 2.0D && distance < 40.0D) {
            entity.setNoGravity(true);
            Vec3 toward = target.getEyePosition().subtract(entity.position()).normalize();
            mob.getMoveControl().setWantedPosition(target.getX(), target.getEyeY(), target.getZ(), 1.35D);
            Vec3 current = entity.getDeltaMovement().scale(0.35D);
            Vec3 flight = toward.scale(0.34D);
            entity.setDeltaMovement(current.add(flight));
            entity.fallDistance = 0.0F;
            entity.hurtMarked = true;
        } else {
            entity.setNoGravity(false);
        }
    }

    private static void dryNearbyLand(ServerLevel level, LivingEntity entity) {
        BlockPos center = entity.blockPosition();
        int radius = 100;
        for (int i = 0; i < 384; i++) {
            int x = center.getX() + entity.getRandom().nextInt(201) - 100;
            int z = center.getZ() + entity.getRandom().nextInt(201) - 100;
            int dx = x - center.getX();
            int dz = z - center.getZ();
            if (dx * dx + dz * dz > radius * radius) {
                continue;
            }
            BlockPos column = new BlockPos(x, center.getY(), z);
            if (!level.isLoaded(column)) {
                continue;
            }
            int minY = level.getMinBuildHeight();
            int maxY = level.getMaxBuildHeight() - 1;
            for (int y = minY; y <= maxY; y++) {
                dryBlock(level, new BlockPos(x, y, z));
            }
        }
    }

    private static void dryBlock(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() && state.getFluidState().isEmpty()) {
            return;
        }
        if (state.getFluidState().is(FluidTags.WATER)) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            return;
        }
        if (state.is(Blocks.GRASS_BLOCK)) {
            level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
            return;
        }
        if (isPlantBlock(state)) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
    }

    private static boolean isPlantBlock(BlockState state) {
        Block block = state.getBlock();
        return state.is(BlockTags.LEAVES)
                || state.is(BlockTags.FLOWERS)
                || state.is(BlockTags.CROPS)
                || state.is(BlockTags.SAPLINGS)
                || state.is(BlockTags.CLIMBABLE)
                || state.is(Blocks.SHORT_GRASS)
                || state.is(Blocks.TALL_GRASS)
                || state.is(Blocks.FERN)
                || state.is(Blocks.LARGE_FERN)
                || state.is(Blocks.SEAGRASS)
                || state.is(Blocks.TALL_SEAGRASS)
                || state.is(Blocks.KELP)
                || state.is(Blocks.KELP_PLANT)
                || block instanceof BushBlock
                || block instanceof CropBlock
                || block instanceof StemBlock
                || block instanceof AttachedStemBlock
                || block instanceof GrowingPlantBlock
                || block instanceof LeavesBlock
                || block instanceof VineBlock
                || block instanceof CactusBlock
                || block instanceof SugarCaneBlock
                || block instanceof BambooStalkBlock
                || block instanceof WaterlilyBlock;
    }

    private static InteractionResult feedGrudgeItem(ServerPlayer player, LivingEntity entity, ItemStack stack,
                                                   long grudgePerItem, long maxGrudge) {
        long current = grudge(entity);
        if (current >= maxGrudge) {
            return InteractionResult.SUCCESS;
        }
        int itemsToUse = player.isShiftKeyDown() ? stack.getCount() : 1;
        if (maxGrudge != Long.MAX_VALUE) {
            long remainingItems = (maxGrudge - current + grudgePerItem - 1L) / grudgePerItem;
            itemsToUse = (int) Math.min(itemsToUse, remainingItems);
        }
        if (itemsToUse <= 0) {
            return InteractionResult.SUCCESS;
        }
        long added = grudgePerItem * itemsToUse;
        long next = maxGrudge == Long.MAX_VALUE ? current + added : Math.min(maxGrudge, current + added);
        CompoundTag tag = entity.getPersistentData();
        tag.putBoolean(TAG_GRUDGE_ROLLED, true);
        tag.putBoolean(TAG_HAS_GRUDGE, true);
        tag.putLong(TAG_GRUDGE, next);
        long ticks = Math.max(tag.getLong(TAG_TICKS), next * TICKS_PER_GRUDGE);
        tag.putLong(TAG_TICKS, ticks);
        tag.putDouble(TAG_GRUDGE_PROGRESS, Math.max(tag.getDouble(TAG_GRUDGE_PROGRESS), ticks));
        setBase(entity, ModAttributes.GRUDGE, next);
        applyRank(entity);
        if (!player.getAbilities().instabuild) {
            stack.shrink(itemsToUse);
        }
        return InteractionResult.SUCCESS;
    }

    private static boolean hasGrudgeGrowth(LivingEntity entity) {
        CompoundTag tag = entity.getPersistentData();
        if (entity instanceof CultivatorCorpseEntity) {
            tag.putBoolean(TAG_GRUDGE_ROLLED, true);
            tag.putBoolean(TAG_HAS_GRUDGE, true);
            return true;
        }
        if (!tag.getBoolean(TAG_GRUDGE_ROLLED)) {
            tag.putBoolean(TAG_GRUDGE_ROLLED, true);
            tag.putBoolean(TAG_HAS_GRUDGE, entity.getRandom().nextFloat() < 0.05F);
        }
        return tag.getBoolean(TAG_HAS_GRUDGE);
    }

    private static void syncGrudgeAttributeIntoData(LivingEntity entity) {
        AttributeInstance instance = entity.getAttribute(ModAttributes.GRUDGE);
        if (instance == null) {
            return;
        }
        long attributeGrudge = Math.max(0L, Math.round(instance.getBaseValue()));
        CompoundTag tag = entity.getPersistentData();
        long storedGrudge = tag.getLong(TAG_GRUDGE);
        if (attributeGrudge == storedGrudge) {
            return;
        }
        tag.putBoolean(TAG_GRUDGE_ROLLED, true);
        tag.putBoolean(TAG_HAS_GRUDGE, attributeGrudge > 0L || entity instanceof CultivatorCorpseEntity);
        tag.putLong(TAG_GRUDGE, attributeGrudge);
        long ticks = Math.max(0L, attributeGrudge * TICKS_PER_GRUDGE);
        tag.putLong(TAG_TICKS, ticks);
        tag.putDouble(TAG_GRUDGE_PROGRESS, ticks);
    }

    private static double grudgeGrowthMultiplier(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return 1.0D;
        }
        int yinQi = YinQiField.sample(serverLevel, entity.blockPosition());
        if (yinQi > 0) {
            return yinQi;
        }
        if (yinQi < 0) {
            return 1.0D / Math.abs(yinQi);
        }
        return 1.0D;
    }

    private static boolean isSealHelmet(ItemStack stack) {
        return stack.is(ModItems.ZHENSHI_SEAL_HELMET.get()) || stack.is(ModItems.ZHENSHI_TALISMAN.get());
    }

    private static void applyFengyanSeal(ServerPlayer player, LivingEntity entity, ItemStack stack) {
        entity.getPersistentData().putBoolean(TAG_FENGYAN_SEALED, true);
        entity.getPersistentData().remove(TAG_ATTACK_TARGET);
        entity.getPersistentData().putBoolean(TAG_HAS_COMMAND, false);
        entity.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.ZHENSHI_SEAL_HELMET.get()));
        freeze(entity);
        if (entity instanceof Mob mob) {
            mob.setNoAi(true);
            mob.setDropChance(EquipmentSlot.HEAD, 0.0F);
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }

    private static void removeFengyanSeal(ServerPlayer player, LivingEntity entity) {
        entity.getPersistentData().putBoolean(TAG_FENGYAN_SEALED, false);
        entity.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.ZHENSHI_SEAL_HELMET.get()));
        if (entity instanceof Mob mob && !isSealed(entity)) {
            mob.setNoAi(false);
        }
        if (!player.getInventory().add(new ItemStack(ModItems.FENGYAN_TALISMAN.get()))) {
            player.drop(new ItemStack(ModItems.FENGYAN_TALISMAN.get()), false);
        }
    }

    private static void releaseSealForControl(LivingEntity entity) {
        entity.getPersistentData().putBoolean(TAG_SEALED, false);
        if (entity instanceof CultivatorCorpseEntity corpse) {
            corpse.setSealed(false);
        }
        if (entity instanceof Mob mob) {
            mob.setNoAi(false);
            mob.getNavigation().stop();
        }
    }

    private static void setBase(LivingEntity entity, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double value) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null && Math.abs(instance.getBaseValue() - value) > 0.001D) {
            instance.setBaseValue(value);
        }
    }

    private static void ensureActiveServantStats(LivingEntity entity) {
        setBaseAtLeast(entity, Attributes.MOVEMENT_SPEED, 0.28D);
        setBaseAtLeast(entity, Attributes.FOLLOW_RANGE, 48.0D);
        setBaseAtLeast(entity, Attributes.ATTACK_DAMAGE, 3.0D);
        setBaseAtLeast(entity, Attributes.JUMP_STRENGTH, 0.70D);
        setBaseAtLeast(entity, Attributes.STEP_HEIGHT, 1.1D);
        setBaseAtLeast(entity, Attributes.WATER_MOVEMENT_EFFICIENCY, 0.35D);
        if (entity instanceof Mob mob) {
            mob.getNavigation().setCanFloat(true);
        }
    }

    private static void setBaseAtLeast(LivingEntity entity, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double value) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null && instance.getBaseValue() < value) {
            instance.setBaseValue(value);
        }
    }

    private static void stopHorizontalMovement(LivingEntity entity) {
        Vec3 movement = entity.getDeltaMovement();
        entity.setDeltaMovement(0.0D, entity.onGround() ? 0.0D : Math.min(movement.y, -0.08D), 0.0D);
    }

    private static Rank rankFor(long grudge) {
        Rank current = RANKS.getFirst();
        for (Rank rank : RANKS) {
            if (grudge >= rank.threshold()) {
                current = rank;
            }
        }
        return current;
    }

    private static long thresholdFor(String rankName) {
        for (Rank rank : RANKS) {
            if (rank.name().equals(rankName)) {
                return rank.threshold();
            }
        }
        return Long.MAX_VALUE;
    }

    private static EntityHitResult pickEntity(Player player, double range, Predicate<Entity> predicate) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(range));
        AABB search = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0D);
        return ProjectileUtil.getEntityHitResult(player.level(), player, eye, end, search,
                entity -> !entity.isSpectator() && entity.isPickable() && predicate.test(entity));
    }

    private static LivingEntity findNearbyHostile(LivingEntity entity) {
        return entity.level().getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(5.0D),
                target -> target.isAlive()
                        && target != entity
                        && target instanceof Enemy
                        && !isSameOwner(entity, target)
                        && !(target instanceof Player)).stream().findFirst().orElse(null);
    }

    private static boolean isSameOwner(LivingEntity first, LivingEntity second) {
        CompoundTag firstTag = first.getPersistentData();
        CompoundTag secondTag = second.getPersistentData();
        return firstTag.hasUUID(TAG_OWNER)
                && secondTag.hasUUID(TAG_OWNER)
                && firstTag.getUUID(TAG_OWNER).equals(secondTag.getUUID(TAG_OWNER));
    }

    private static void clearOwnerTarget(LivingEntity entity) {
        if (!(entity instanceof Mob mob) || mob.getTarget() == null || !entity.getPersistentData().hasUUID(TAG_OWNER)) {
            return;
        }
        if (mob.getTarget().getUUID().equals(entity.getPersistentData().getUUID(TAG_OWNER))) {
            mob.setTarget(null);
            mob.getNavigation().stop();
            entity.getPersistentData().remove(TAG_ATTACK_TARGET);
        }
    }

    private static void navigateToPosition(Mob mob, Vec3 pos, CompoundTag tag) {
        long gameTime = mob.level().getGameTime();
        if (gameTime < tag.getLong(TAG_NEXT_REPATH_TICK) && !mob.getNavigation().isDone()) {
            return;
        }
        mob.getNavigation().moveTo(pos.x, pos.y, pos.z, 1.2D);
        tag.putLong(TAG_NEXT_REPATH_TICK, gameTime + 10L);
    }

    private static void navigateToTarget(Mob mob, LivingEntity target, CompoundTag tag) {
        long gameTime = mob.level().getGameTime();
        if (gameTime < tag.getLong(TAG_NEXT_REPATH_TICK) && !mob.getNavigation().isDone()) {
            return;
        }
        mob.getNavigation().moveTo(target, 1.25D);
        tag.putLong(TAG_NEXT_REPATH_TICK, gameTime + 8L);
    }

    private static void assistPathIfStuck(Mob mob, Vec3 pos, CompoundTag tag) {
        LivingEntity entity = mob;
        double distance = entity.position().distanceToSqr(pos);
        if (distance < 2.25D || !entity.onGround()) {
            resetPathStuck(tag, entity.position());
            return;
        }

        Vec3 current = entity.position();
        Vec3 last = new Vec3(tag.getDouble(TAG_LAST_PATH_X), tag.getDouble(TAG_LAST_PATH_Y), tag.getDouble(TAG_LAST_PATH_Z));
        if (!tag.contains(TAG_LAST_PATH_X) || current.distanceToSqr(last) > 0.05D) {
            resetPathStuck(tag, current);
            return;
        }

        int stuckTicks = tag.getInt(TAG_STUCK_TICKS) + 1;
        tag.putInt(TAG_STUCK_TICKS, stuckTicks);
        boolean blocked = entity.horizontalCollision || mob.getNavigation().isDone();
        if (stuckTicks < 12 || !blocked) {
            return;
        }
        jumpToward(entity, pos, tag);
        tag.putInt(TAG_STUCK_TICKS, 0);
        tag.putLong(TAG_NEXT_REPATH_TICK, entity.level().getGameTime() + 6L);
    }

    private static void forceCommandMovement(LivingEntity entity, Vec3 pos) {
        if (stopForBarrierBelow(entity)) {
            return;
        }
        Vec3 delta = pos.subtract(entity.position());
        Vec3 horizontal = new Vec3(delta.x, 0.0D, delta.z);
        double horizontalSqr = horizontal.lengthSqr();
        if (horizontalSqr < 0.04D) {
            return;
        }
        Vec3 direction = horizontal.normalize();
        double speed = entity instanceof CultivatorCorpseEntity ? 0.34D : 0.26D;
        Vec3 current = entity.getDeltaMovement();
        double y = current.y;
        long gameTime = entity.level().getGameTime();
        boolean needsJump = entity.horizontalCollision || pos.y > entity.getY() + 0.6D;
        boolean commandHop = entity.onGround() && horizontalSqr > 1.0D && gameTime >= entity.getPersistentData().getLong(TAG_NEXT_JUMP_TICK);
        if (entity.onGround() && (needsJump || commandHop)) {
            y = 0.95D;
            entity.getPersistentData().putLong(TAG_NEXT_JUMP_TICK, gameTime + 12L);
        }
        entity.setDeltaMovement(direction.x * speed, y, direction.z * speed);
        entity.setNoGravity(false);
        entity.fallDistance = 0.0F;
        entity.hurtMarked = true;
        if (entity instanceof Mob mob) {
            mob.setAggressive(false);
            mob.getMoveControl().setWantedPosition(pos.x, pos.y, pos.z, 1.25D);
        }
    }

    private static boolean stopForBarrierBelow(LivingEntity entity) {
        int minX = (int) Math.floor(entity.getBoundingBox().minX + 0.001D);
        int maxX = (int) Math.floor(entity.getBoundingBox().maxX - 0.001D);
        int minZ = (int) Math.floor(entity.getBoundingBox().minZ + 0.001D);
        int maxZ = (int) Math.floor(entity.getBoundingBox().maxZ - 0.001D);
        int startY = (int) Math.floor(entity.getY() + 0.1D);
        int endY = Math.max(entity.level().getMinBuildHeight(), startY - 4);
        boolean stopped = false;
        for (int y = startY; y >= endY; y--) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (stopForBarrierAt(entity, new BlockPos(x, y, z))) {
                        stopped = true;
                    }
                }
            }
        }
        return stopped;
    }

    private static boolean stopForBarrierAt(LivingEntity entity, BlockPos pos) {
        BlockState state = entity.level().getBlockState(pos);
        if (state.is(ModBlocks.NUOMI_DUST.get())) {
            if (ignoresNuomi(entity)) {
                entity.level().removeBlock(pos, false);
                return false;
            }
            stunFromRepel(entity, 20);
            stopHorizontalMovement(entity);
            entity.hurtMarked = true;
            return true;
        }
        if (state.is(ModBlocks.MODOU_LINE.get())) {
            if (ignoresModouLine(entity)) {
                entity.level().removeBlock(pos, false);
                return false;
            }
            stunFromRepel(entity, 20);
            stopHorizontalMovement(entity);
            entity.hurtMarked = true;
            return true;
        }
        return false;
    }

    private static void resetPathStuck(CompoundTag tag, Vec3 pos) {
        tag.putDouble(TAG_LAST_PATH_X, pos.x);
        tag.putDouble(TAG_LAST_PATH_Y, pos.y);
        tag.putDouble(TAG_LAST_PATH_Z, pos.z);
        tag.putInt(TAG_STUCK_TICKS, 0);
    }

    private static void jumpToward(LivingEntity entity, Vec3 pos, CompoundTag tag) {
        long gameTime = entity.level().getGameTime();
        if (!entity.onGround()) {
            return;
        }
        if (gameTime < tag.getLong(TAG_NEXT_JUMP_TICK)) {
            return;
        }
        Vec3 delta = pos.subtract(entity.position());
        Vec3 horizontal = new Vec3(delta.x, 0.0D, delta.z);
        if (horizontal.lengthSqr() < 0.04D) {
            return;
        }
        Vec3 leap = horizontal.normalize().scale(0.42D);
        double vertical = pos.y > entity.getY() + 0.75D ? 0.95D : 0.72D;
        entity.setDeltaMovement(leap.x, vertical, leap.z);
        entity.hurtMarked = true;
        tag.putLong(TAG_NEXT_JUMP_TICK, gameTime + 20L);
    }

    private static void faceToward(LivingEntity entity, Vec3 pos) {
        Vec3 delta = pos.subtract(entity.position());
        if (delta.horizontalDistanceSqr() < 0.001D) {
            return;
        }
        float yaw = (float) (Math.atan2(delta.z, delta.x) * 180.0D / Math.PI) - 90.0F;
        entity.setYRot(yaw);
        entity.yBodyRot = yaw;
        entity.yBodyRotO = yaw;
        entity.yHeadRot = yaw;
        entity.yHeadRotO = yaw;
    }

    private record Rank(long threshold, String name, String level, double damage) {
    }
}
