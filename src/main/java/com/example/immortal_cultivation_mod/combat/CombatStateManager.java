package com.example.immortal_cultivation_mod.combat;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.entity.ModelBonePartEntity;
import com.example.immortal_cultivation_mod.entity.WeaponQiSlashEntity;
import com.example.immortal_cultivation_mod.item.FuchenItem;
import com.example.immortal_cultivation_mod.item.HeavyIronBracerItem;
import com.example.immortal_cultivation_mod.item.QiInfusedWeapon;
import com.example.immortal_cultivation_mod.item.TieguShanItem;
import com.example.immortal_cultivation_mod.item.ThrowingKnifeItem;
import com.example.immortal_cultivation_mod.item.WhipItem;
import com.example.immortal_cultivation_mod.item.XiujianItem;
import com.example.immortal_cultivation_mod.network.CombatPayloads;
import com.example.immortal_cultivation_mod.network.ModPayloads;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import com.example.immortal_cultivation_mod.spell.SolarTechniques;
import com.example.immortal_cultivation_mod.spell.UndeadAbilitySystem;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CombatStateManager {
    public static final int COMBAT_DURATION_TICKS = 20 * 15;
    public static final int POSTURE_BREAK_TICKS = 30;
    public static final int HEAVY_CHARGE_TICKS = 10;
    private static final int COMBO_RESET_TICKS = 24;
    private static final int DUPLICATE_ATTACK_WINDOW_TICKS = 3;

    private static final String ENABLED_TAG = "ImmortalCultivationCombatStateEnabled";
    private static final String AUTO_COMBAT_ON_HIT_TAG = "ImmortalCultivationAutoCombatOnHit";
    private static final String COMBAT_UNTIL_TAG = "ImmortalCultivationCombatUntil";
    private static final String MANUAL_COMBAT_TAG = "ImmortalCultivationManualCombat";
    private static final String POSTURE_TAG = "ImmortalCultivationPosture";
    private static final String POSTURE_BROKEN_UNTIL_TAG = "ImmortalCultivationPostureBrokenUntil";
    private static final Map<UUID, RecentHit> RECENT_HITS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> HEAVY_CHARGE_STARTS = new ConcurrentHashMap<>();
    private static final Map<UUID, SpellPreparation> SPELL_PREPARATIONS = new ConcurrentHashMap<>();
    private static final Map<UUID, SyncState> LAST_SYNC = new ConcurrentHashMap<>();
    private static final Map<UUID, ComboState> COMBO_STATES = new ConcurrentHashMap<>();
    private static final Set<UUID> BLOCKING_PLAYERS = ConcurrentHashMap.newKeySet();
    private record Strike(int targetId, boolean heavy, long impactAt, ItemStack weapon, int slashIndex, boolean heavyCone) {}
    private static final Map<UUID, Strike> STRIKES = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> RECOVERY = new ConcurrentHashMap<>();
    private static final ThreadLocal<UUID> HEAVY_ATTACKER = new ThreadLocal<>();

    private CombatStateManager() {
    }

    public static boolean isEnabled(ServerPlayer player) {
        return !player.getPersistentData().contains(ENABLED_TAG)
                || player.getPersistentData().getBoolean(ENABLED_TAG);
    }

    public static void setEnabled(ServerPlayer player, boolean enabled) {
        player.getPersistentData().putBoolean(ENABLED_TAG, enabled);
        if (!enabled) {
            clear(player);
        } else {
            sync(player, true);
        }
    }

    public static boolean isInCombat(ServerPlayer player) {
        return isEnabled(player)
                && (isManualCombat(player)
                || player.getPersistentData().getLong(COMBAT_UNTIL_TAG)
                > player.serverLevel().getGameTime());
    }

    public static boolean autoCombatOnHit(ServerPlayer player) {
        return !player.getPersistentData().contains(AUTO_COMBAT_ON_HIT_TAG)
                || player.getPersistentData().getBoolean(AUTO_COMBAT_ON_HIT_TAG);
    }

    public static void setAutoCombatOnHit(ServerPlayer player, boolean enabled) {
        boolean changed = autoCombatOnHit(player) != enabled;
        player.getPersistentData().putBoolean(AUTO_COMBAT_ON_HIT_TAG, enabled);
        if (changed && !enabled && !isManualCombat(player) && isInCombat(player)) {
            clear(player, false);
        }
    }

    public static boolean isManualCombat(ServerPlayer player) {
        return player.getPersistentData().getBoolean(MANUAL_COMBAT_TAG);
    }

    public static void toggleManualCombat(ServerPlayer player) {
        if (!isEnabled(player)) {
            player.displayClientMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID
                            + ".manual_combat_system_disabled"), true);
            return;
        }
        if (isManualCombat(player)) {
            clear(player);
            player.displayClientMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID
                            + ".manual_combat_disabled"), true);
            return;
        }

        boolean wasActive = isInCombat(player);
        player.getPersistentData().putBoolean(MANUAL_COMBAT_TAG, true);
        float max = maxPosture(player);
        if (!wasActive || !player.getPersistentData().contains(POSTURE_TAG)) {
            player.getPersistentData().putFloat(POSTURE_TAG, max);
        } else if (player.getPersistentData().getFloat(POSTURE_TAG) > max) {
            player.getPersistentData().putFloat(POSTURE_TAG, max);
        }
        sync(player, true);
        player.displayClientMessage(Component.translatable(
                "message." + ImmortalCultivationMod.MODID
                        + ".manual_combat_enabled"), true);
    }

    public static boolean isPostureBroken(ServerPlayer player) {
        return !ThoughtControlGuard.hasProtection(player) && isInCombat(player)
                && player.getPersistentData().getLong(POSTURE_BROKEN_UNTIL_TAG)
                > player.serverLevel().getGameTime();
    }

    public static void enterCombat(ServerPlayer player) {
        if (!isEnabled(player) || !player.isAlive() || player.isSpectator()) {
            return;
        }
        long gameTime = player.serverLevel().getGameTime();
        boolean wasActive = isInCombat(player);
        player.getPersistentData().putLong(COMBAT_UNTIL_TAG, gameTime + COMBAT_DURATION_TICKS);
        float max = maxPosture(player);
        if (!wasActive || !player.getPersistentData().contains(POSTURE_TAG)) {
            player.getPersistentData().putFloat(POSTURE_TAG, max);
        } else if (player.getPersistentData().getFloat(POSTURE_TAG) > max) {
            player.getPersistentData().putFloat(POSTURE_TAG, max);
        }
        sync(player, !wasActive);
    }

    public static void recordHostileAction(LivingEntity attacker, LivingEntity target) {
        if (attacker == target || !attacker.isAlive()) {
            return;
        }
        if (attacker instanceof ServerPlayer player) {
            enterCombat(player);
        }
        if (target instanceof ServerPlayer player && autoCombatOnHit(player)) {
            enterCombat(player);
        }
    }

    public static void tick(ServerPlayer player) {
        tickStrike(player);
        WhipItem.tickCombo(player);
        if (SPELL_PREPARATIONS.containsKey(player.getUUID())
                && isSpellPreparationBlocked(player)) {
            interruptSpellPreparation(player);
        }
        SpellPreparation preparation = SPELL_PREPARATIONS.get(player.getUUID());
        if (preparation != null
                && preparation.spellId().equals(ModSpells.DARI_ZHEN)) {
            SolarTechniques.tickRiyunCharge(
                    player,
                    Math.max(0L, player.serverLevel().getGameTime()
                            - preparation.startedAt()));
        }
        if (!isEnabled(player)) {
            clear(player, false);
            return;
        }
        if (BLOCKING_PLAYERS.contains(player.getUUID())
                && !canMaintainBlock(player)) {
            setBlocking(player, false);
        }
        if (!isInCombat(player)) {
            if (player.getPersistentData().contains(COMBAT_UNTIL_TAG)) {
                player.getPersistentData().remove(COMBAT_UNTIL_TAG);
                player.getPersistentData().remove(POSTURE_BROKEN_UNTIL_TAG);
                player.getPersistentData().putFloat(POSTURE_TAG, maxPosture(player));
                HEAVY_CHARGE_STARTS.remove(player.getUUID());
                sync(player, true);
            }
            return;
        }

        long gameTime = player.serverLevel().getGameTime();
        if (!isPostureBroken(player)
                && player.getPersistentData().contains(POSTURE_BROKEN_UNTIL_TAG)) {
            player.getPersistentData().remove(POSTURE_BROKEN_UNTIL_TAG);
        }
        if (!isPostureBroken(player) && gameTime % 20L == 0L) {
            float max = maxPosture(player);
            float current = currentPosture(player);
            setPosture(player, Math.min(max, current + max * 0.01F));
        }
        if (gameTime % 5L == 0L) {
            sync(player, false);
        }
    }

    public static void lockMovement(ServerPlayer player) {
        if (!isPostureBroken(player)) {
            return;
        }
        player.xxa = 0.0F;
        player.yya = 0.0F;
        player.zza = 0.0F;
        player.setSprinting(false);
        Vec3 movement = player.getDeltaMovement();
        player.setDeltaMovement(0.0D, Math.min(0.0D, movement.y), 0.0D);
        player.hurtMarked = true;
    }

    public static float maxPosture(ServerPlayer player) {
        ModAttachments.CultivationData data = ModAttachments.getData(player);
        return Math.max(1.0F,
                100.0F + data.physicalAttack() * 100.0F + player.getMaxHealth() * 0.5F);
    }

    public static float currentPosture(ServerPlayer player) {
        float max = maxPosture(player);
        if (!player.getPersistentData().contains(POSTURE_TAG)) {
            player.getPersistentData().putFloat(POSTURE_TAG, max);
            return max;
        }
        return Math.max(0.0F, Math.min(max, player.getPersistentData().getFloat(POSTURE_TAG)));
    }

    /** Read-only inspection must not initialize combat state. */
    public static float peekPosture(ServerPlayer player) {
        float max = maxPosture(player);
        return player.getPersistentData().contains(POSTURE_TAG)
                ? Math.max(0.0F, Math.min(max, player.getPersistentData().getFloat(POSTURE_TAG))) : max;
    }

    public static void damagePosture(ServerPlayer player, float amount) {
        if (!isInCombat(player) || isPostureBroken(player) || amount <= 0.0F) {
            return;
        }
        float remaining = Math.max(0.0F, currentPosture(player) - amount);
        setPosture(player, remaining);
        if (remaining <= 0.0F) {
            if (ThoughtControlGuard.resistForDuration(player, "posture_break", null, POSTURE_BREAK_TICKS)) {
                sync(player, true);
                return;
            }
            player.getPersistentData().putLong(
                    POSTURE_BROKEN_UNTIL_TAG,
                    player.serverLevel().getGameTime() + POSTURE_BREAK_TICKS);
            player.stopUsingItem();
            setBlocking(player, false);
            sync(player, true);
        }
    }

    public static void applyPostDamage(ServerPlayer target, DamageSource source,
                                       float damageTaken, float blockedDamage) {
        if ((damageTaken > 0.0F || blockedDamage > 0.0F)
                && SPELL_PREPARATIONS.containsKey(target.getUUID())
                && !ThoughtControlGuard.resist(target, "damage_interrupt", source.getEntity(), 2)) {
            interruptSpellPreparation(target);
        }
        if (!isInCombat(target)) {
            return;
        }
        float postureDamage = Math.max(0.0F, damageTaken) / 2.0F;
        postureDamage += Math.max(0.0F, blockedDamage) / 3.0F;
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            postureDamage += 30.0F;
        }

        Entity attackerEntity = source.getEntity();
        if (attackerEntity instanceof LivingEntity attacker) {
            long gameTime = target.serverLevel().getGameTime();
            RecentHit previous = RECENT_HITS.get(target.getUUID());
            if (previous != null
                    && previous.attacker().equals(attacker.getUUID())
                    && gameTime - previous.gameTime() <= 40L) {
                postureDamage += 5.0F;
            }
            RECENT_HITS.put(target.getUUID(), new RecentHit(attacker.getUUID(), gameTime));
            if (isBehind(target, attacker)) {
                postureDamage += 10.0F;
            }
            if (isHeavyAttack(source)) {
                postureDamage += Math.max(0.0F, damageTaken) * 0.2F;
            }
        }
        damagePosture(target, postureDamage);
    }

    private static boolean isBehind(LivingEntity target, LivingEntity attacker) {
        Vec3 facing = target.getLookAngle().multiply(1.0D, 0.0D, 1.0D).normalize();
        Vec3 towardAttacker = attacker.position().subtract(target.position())
                .multiply(1.0D, 0.0D, 1.0D)
                .normalize();
        return facing.lengthSqr() > 0.0D
                && towardAttacker.lengthSqr() > 0.0D
                && facing.dot(towardAttacker) < -0.35D;
    }

    public static void addBlockedPosture(ServerPlayer player, float blockedDamage) {
        damagePosture(player, Math.max(0.0F, blockedDamage) / 3.0F);
    }

    public static boolean isHeavyAttack(DamageSource source) {
        UUID heavyAttacker = HEAVY_ATTACKER.get();
        return heavyAttacker != null
                && source.getEntity() != null
                && heavyAttacker.equals(source.getEntity().getUUID());
    }

    public static boolean isHeavyAttackInProgress(ServerPlayer player) {
        UUID heavyAttacker = HEAVY_ATTACKER.get();
        return heavyAttacker != null && heavyAttacker.equals(player.getUUID());
    }

    public static boolean isPreparingHeavyAttack(ServerPlayer player) {
        return HEAVY_CHARGE_STARTS.containsKey(player.getUUID());
    }

    /** True only while server has a live preparation begun by the real prepared-cast input flow. */
    public static boolean isPreparingSpell(ServerPlayer player) {
        if (!SPELL_PREPARATIONS.containsKey(player.getUUID())) {
            return false;
        }
        if (isSpellPreparationBlocked(player)) {
            interruptSpellPreparation(player);
            return false;
        }
        return true;
    }

    public static void beginSpellPreparation(ServerPlayer player, String spellId) {
        String normalized = normalizeSpellId(spellId);
        if (normalized.isEmpty() || !ModSpells.requiresPreparation(normalized)) {
            cancelSpellPreparation(player);
            return;
        }
        if (isSpellPreparationBlocked(player)) {
            sendSpellPreparationInterrupt(player);
            return;
        }
        cancelHeavyCharge(player);
        setBlocking(player, false);
        SPELL_PREPARATIONS.put(player.getUUID(), new SpellPreparation(
                normalized, player.serverLevel().getGameTime()));
    }

    public static void cancelSpellPreparation(ServerPlayer player) {
        SPELL_PREPARATIONS.remove(player.getUUID());
    }

    /** Consumes matching preparation when a prepared spell reaches its real cast handler. */
    public static boolean finishSpellPreparation(
            ServerPlayer player,
            String spellId,
            float requestedChargeScale) {
        float charge = consumeSpellCharge(player, spellId, requestedChargeScale);
        return charge >= 1.0F && requestedChargeScale <= charge + 0.001F;
    }

    /** Returns server-earned charge, or zero when this cast has no valid preparation. */
    public static float consumeSpellCharge(
            ServerPlayer player,
            String spellId,
            float requestedChargeScale) {
        String normalized = normalizeSpellId(spellId);
        SpellPreparation preparation = SPELL_PREPARATIONS.remove(player.getUUID());
        if (normalized.isEmpty() || !Float.isFinite(requestedChargeScale) || requestedChargeScale < 1.0F) {
            return 0.0F;
        }
        if (!ModSpells.requiresPreparation(normalized)) {
            return requestedChargeScale <= 1.001F ? 1.0F : 0.0F;
        }
        if (preparation == null
                || !preparation.spellId().equals(normalized)
                || isSpellPreparationBlocked(player)) {
            return 0.0F;
        }
        long elapsed = Math.max(0L,
                player.serverLevel().getGameTime() - preparation.startedAt());
        long effectiveElapsed = Math.max(0L, (long) Math.floor(
                elapsed * UndeadAbilitySystem.spellChargeSpeedMultiplier(player)));
        float maximumCharge = 1.0F + Math.min(40L, effectiveElapsed + 2L) / 40.0F;
        // Client input and server ticks can drift under network/mod load. A valid
        // release still casts, but can never earn more charge than the server saw.
        return Math.min(requestedChargeScale, maximumCharge);
    }

    public static void interruptSpellPreparation(ServerPlayer player) {
        com.example.immortal_cultivation_mod.spell.SelfExplosionConfirmation.cancel(player);
        if (SPELL_PREPARATIONS.remove(player.getUUID()) != null) {
            sendSpellPreparationInterrupt(player);
        }
    }

    private static void sendSpellPreparationInterrupt(ServerPlayer player) {
        PacketDistributor.sendToPlayer(
                player, new ModPayloads.ClientboundInterruptSpellPreparationPayload());
    }

    private static boolean isSpellPreparationBlocked(ServerPlayer player) {
        return isSpellcastingBlocked(player)
                || player.hasEffect(ModEffects.QI_GATHERING);
    }

    public static boolean isSpellcastingBlocked(ServerPlayer player) {
        return !player.isAlive()||ColdTideFreeze.isFrozen(player)
                || player.isSpectator()
                || isPostureBroken(player)
                || player.hasEffect(ModEffects.DAZE)
                || player.hasEffect(ModEffects.DINGSHEN)
                || player.hasEffect(ModEffects.FROZEN)
                || player.hasEffect(ModEffects.FROZEN_QI);
    }

    private static String normalizeSpellId(String spellId) {
        if (spellId == null) {
            return "";
        }
        String normalized = ModSpells.normalizeId(spellId);
        return normalized.isBlank() || ModSpells.get(normalized) == null ? "" : normalized;
    }

    public static boolean isBlocking(ServerPlayer player) {
        return BLOCKING_PLAYERS.contains(player.getUUID()) && canMaintainBlock(player);
    }

    /** Runs a mob's normal equipped-weapon hit inside the same heavy-attack marker used by players. */
    public static boolean performNpcHeavyAttack(PathfinderMob attacker, Entity target) {
        if (target == null || target == attacker || !target.isAlive()) {
            return false;
        }
        HEAVY_ATTACKER.set(attacker.getUUID());
        try {
            return attacker.doHurtTarget(target);
        } finally {
            HEAVY_ATTACKER.remove();
        }
    }

    public static void beginHeavyCharge(ServerPlayer player) {
        if (!canPerformWeaponAttack(player)) {
            return;
        }
        interruptSpellPreparation(player);
        HEAVY_CHARGE_STARTS.put(player.getUUID(), player.serverLevel().getGameTime());
    }

    public static void cancelHeavyCharge(ServerPlayer player) {
        HEAVY_CHARGE_STARTS.remove(player.getUUID());
    }

    public static void setBlocking(ServerPlayer player, boolean blocking) {
        boolean nextBlocking = blocking && canMaintainBlock(player);
        boolean changed = nextBlocking
                ? BLOCKING_PLAYERS.add(player.getUUID())
                : BLOCKING_PLAYERS.remove(player.getUUID());
        if (!changed) {
            return;
        }
        cancelHeavyCharge(player);
        if (nextBlocking) {
            cancelStrike(player);
            interruptSpellPreparation(player);
        }
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                player,
                new CombatPayloads.ClientboundSwordComboAnimationPayload(
                        player.getUUID(),
                        nextBlocking
                                ? CombatPayloads.BLOCK_ANIMATION_START
                                : CombatPayloads.BLOCK_ANIMATION_STOP));
    }

    public static boolean blockIncomingAttack(
            ServerPlayer player, DamageSource source, float incomingDamage) {
        if (incomingDamage <= 0.0F
                || source.getEntity() == null
                || source.getEntity() == player
                || source.is(DamageTypeTags.BYPASSES_SHIELD)
                || !BLOCKING_PLAYERS.contains(player.getUUID())
                || !canMaintainBlock(player)) {
            return false;
        }
        if (source.getEntity() instanceof LivingEntity attacker) {
            recordHostileAction(attacker, player);
        }
        boolean heavy = isHeavyAttack(source);
        damagePosture(player, heavy
                ? Math.max(15.0F, incomingDamage * 0.9F)
                : Math.max(0.0F, incomingDamage) / 3.0F);
        ItemStack weapon = player.getMainHandItem();
        weapon.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        if (weapon.isEmpty()) {
            setBlocking(player, false);
        }
        // A heavy strike that empties posture guard-breaks and is allowed through.
        boolean blocked = !heavy || !isPostureBroken(player);
        if (blocked) CombatFeedbackEvents.blocked(player, source, incomingDamage);
        else CombatFeedbackEvents.shieldBreak(player, source, incomingDamage);
        return blocked;
    }

    public static void performNormalAttack(ServerPlayer player, int entityId) {
        HEAVY_CHARGE_STARTS.remove(player.getUUID());
        interruptSpellPreparation(player);
        if (!canPerformWeaponAttack(player)) {
            return;
        }
        QiDomainSystem.attack(player);
        executeNormalAttack(player, entityId);
    }

    private static void executeNormalAttack(ServerPlayer player, int entityId) {
        if (XiujianItem.canFire(player)) {
            if (XiujianItem.isHeld(player)) {
                XiujianItem.fireNeedle(player, false);
            }
            return;
        }
        if (player.getMainHandItem().getItem() instanceof WhipItem) {
            WhipItem.performNormalAttack(player);
            broadcastSwordCombo(player);
            player.swing(InteractionHand.MAIN_HAND, true);
            return;
        }
        queueStrike(player, entityId, false);
        broadcastSwordCombo(player);
    }

    public static void broadcastSwordCombo(ServerPlayer player) {
        broadcastSwordCombo(player, false);
    }

    private static void broadcastSwordCombo(ServerPlayer player, boolean heavyCone) {
        broadcastSwordCombo(player, heavyCone, heavyCone);
    }

    private static void broadcastSwordCombo(ServerPlayer player, boolean heavyCone, boolean heavyWeaponAttack) {
        if (!isInCombat(player) || !isCombatWeapon(player.getMainHandItem())) {
            return;
        }
        long gameTime = player.serverLevel().getGameTime();
        ComboState previous = COMBO_STATES.get(player.getUUID());
        if (previous != null
                && gameTime - previous.lastTick() <= DUPLICATE_ATTACK_WINDOW_TICKS) {
            return;
        }
        int slashIndex = previous == null || gameTime - previous.lastTick() > COMBO_RESET_TICKS
                ? 0
                : previous.nextSlash();
        COMBO_STATES.put(player.getUUID(), new ComboState(
                (slashIndex + 1) % 3, gameTime));
        if (QiInfusedWeapon.isFistWeapon(player.getMainHandItem())) {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                    player,
                    new CombatPayloads.ClientboundSwordComboAnimationPayload(
                            player.getUUID(), slashIndex));
            player.swing(InteractionHand.MAIN_HAND, true);
            releaseOrQueueQi(player, slashIndex, heavyCone, heavyWeaponAttack);
            return;
        }
        if (player.getMainHandItem().getItem() instanceof FuchenItem) {
            FuchenItem.playNormalAttackAnimation(player);
        }
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                player, new CombatPayloads.ClientboundSwordComboAnimationPayload(
                        player.getUUID(), slashIndex));
        releaseOrQueueQi(player, slashIndex, heavyCone, heavyWeaponAttack);
    }

    private static void releaseOrQueueQi(ServerPlayer player,int slash,boolean cone,boolean heavy) {
        Strike pending=STRIKES.get(player.getUUID());
        if(pending==null)WeaponQiSlashEntity.release(player,slash,cone,heavy);
        else STRIKES.put(player.getUUID(),new Strike(pending.targetId(),pending.heavy(),pending.impactAt(),pending.weapon(),slash,cone));
    }

    public static void performHeavyAttack(ServerPlayer player, int entityId, long packedBlockPos) {
        Long started = HEAVY_CHARGE_STARTS.remove(player.getUUID());
        long gameTime = player.serverLevel().getGameTime();
        if (started == null
                || gameTime - started < HEAVY_CHARGE_TICKS
                || !canPerformWeaponAttack(player)) {
            return;
        }

        QiDomainSystem.attack(player);
        if (XiujianItem.canFire(player)) {
            XiujianItem.fireNeedle(player, true);
            return;
        }

        if (player.getMainHandItem().getItem() instanceof FuchenItem) {
            if (FuchenItem.startHeavyAttack(player)) {
                broadcastFuchenHeavySpin(player);
                player.swing(InteractionHand.MAIN_HAND, true);
            }
            return;
        }

        if (player.getMainHandItem().getItem() instanceof ThrowingKnifeItem) {
            if (ThrowingKnifeItem.throwHeavy(player)) {
                broadcastThrowingKnifeHeavy(player);
                player.swing(InteractionHand.MAIN_HAND, true);
            }
            return;
        }

        if (player.getMainHandItem().getItem() instanceof WhipItem) {
            WhipItem.performHeavyAttack(player);
            broadcastWhipHeavy(player);
            player.swing(InteractionHand.MAIN_HAND, true);
            return;
        }

        queueStrike(player, entityId, true);
        if (QiInfusedWeapon.isFistWeapon(player.getMainHandItem())) {
            broadcastSwordCombo(player, true);
        } else if (QiInfusedWeapon.isSpear(player.getMainHandItem())) {
            broadcastSpearHeavyThrust(player);
        } else if (TieguShanItem.isClosed(player.getMainHandItem())) {
            broadcastSwordCombo(player, true);
        } else {
            broadcastSwordCombo(player, false, true);
        }
        player.swing(InteractionHand.MAIN_HAND, true);
    }

    private static void broadcastSpearHeavyThrust(ServerPlayer player) {
        COMBO_STATES.remove(player.getUUID());
        com.example.immortal_cultivation_mod.network.WeaponUseAnimations.play(player,
                com.example.immortal_cultivation_mod.network.WeaponUseAnimations.Type.SPEAR);
        WeaponQiSlashEntity.release(player, 0, true);
    }

    private static void broadcastFuchenHeavySpin(ServerPlayer player) {
        COMBO_STATES.remove(player.getUUID());
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                player,
                new CombatPayloads.ClientboundSwordComboAnimationPayload(
                        player.getUUID(),
                        CombatPayloads.FUCHEN_HEAVY_SPIN_ANIMATION));
    }

    private static void broadcastThrowingKnifeHeavy(ServerPlayer player) {
        COMBO_STATES.remove(player.getUUID());
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                player,
                new CombatPayloads.ClientboundSwordComboAnimationPayload(
                        player.getUUID(), 2));
    }

    private static void broadcastWhipHeavy(ServerPlayer player) {
        COMBO_STATES.remove(player.getUUID());
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                player,
                new CombatPayloads.ClientboundSwordComboAnimationPayload(
                        player.getUUID(),
                        CombatPayloads.WHIP_HEAVY_ANIMATION));
    }

    private static boolean canPerformWeaponAttack(ServerPlayer player) {
        return !isSpellcastingBlocked(player)
                && !STRIKES.containsKey(player.getUUID())
                && player.level().getGameTime() >= RECOVERY.getOrDefault(player.getUUID(), 0L)
                && com.example.immortal_cultivation_mod.item.WeaponBinding.checkUse(player.getMainHandItem(), player)
                && (canMaintainBlock(player) || XiujianItem.canFire(player))
                && !BLOCKING_PLAYERS.contains(player.getUUID());
    }

    private static boolean canMaintainBlock(ServerPlayer player) {
        return isInCombat(player)
                && !isSpellcastingBlocked(player)
                && isCombatWeapon(player.getMainHandItem())
                && com.example.immortal_cultivation_mod.item.WeaponBinding.canUse(player.getMainHandItem(), player);
    }

    private static void queueStrike(ServerPlayer player, int target, boolean heavy) {
        long now=player.level().getGameTime();
        STRIKES.put(player.getUUID(), new Strike(target,heavy,now+2,player.getMainHandItem().copy(),-1,false));
        RECOVERY.put(player.getUUID(),now+(heavy?14:8));
    }

    public static void cancelStrike(ServerPlayer player) {
        if (STRIKES.remove(player.getUUID()) != null)
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                new CombatPayloads.ClientboundSwordComboAnimationPayload(player.getUUID(),CombatPayloads.CANCEL_STRIKE_ANIMATION));
        CombatExchange.reset(player);
    }

    private static void tickStrike(ServerPlayer player) {
        Strike strike=STRIKES.get(player.getUUID());
        if(strike==null)return;
        if (!canMaintainBlock(player) || BLOCKING_PLAYERS.contains(player.getUUID())
                || !ItemStack.isSameItemSameComponents(strike.weapon(),player.getMainHandItem())) {
            cancelStrike(player); return;
        }
        if(player.level().getGameTime()<strike.impactAt())return;
        STRIKES.remove(player.getUUID());
        if(!attackEntity(player,strike.targetId(),strike.heavy()))CombatExchange.reset(player);
        if(strike.slashIndex()>=0) WeaponQiSlashEntity.release(player,strike.slashIndex(),strike.heavyCone(),strike.heavy());
    }

    private static boolean attackEntity(ServerPlayer player, int entityId, boolean heavy) {
        if (entityId < 0) {
            return false;
        }
        Entity hitTarget = player.serverLevel().getEntityOrPart(entityId);
        double range = player.entityInteractionRange() + 1.0D;
        if (hitTarget == null
                || hitTarget == player
                || !hitTarget.isAlive()
                || distanceToBoundsSqr(
                        player.getEyePosition(), hitTarget.getBoundingBox()) > range * range
                || !player.hasLineOfSight(hitTarget)) {
            return false;
        }
        Entity target = ModelBonePartEntity.resolveParent(hitTarget);
        if (target == player || !target.isAlive()) {
            return false;
        }
        if (heavy) {
            HEAVY_ATTACKER.set(player.getUUID());
        }
        float before = target instanceof LivingEntity living ? living.getHealth() : 0;
        try {
            player.attack(target);
        } finally {
            HEAVY_ATTACKER.remove();
        }
        return target instanceof LivingEntity living && living.getHealth() < before;
    }

    private static double distanceToBoundsSqr(Vec3 point, AABB bounds) {
        double dx = Math.max(Math.max(bounds.minX - point.x, 0.0D), point.x - bounds.maxX);
        double dy = Math.max(Math.max(bounds.minY - point.y, 0.0D), point.y - bounds.maxY);
        double dz = Math.max(Math.max(bounds.minZ - point.z, 0.0D), point.z - bounds.maxZ);
        return dx * dx + dy * dy + dz * dz;
    }

    public static boolean hurtAsHeavy(
            LivingEntity attacker,
            Entity target,
            DamageSource source,
            float damage) {
        HEAVY_ATTACKER.set(attacker.getUUID());
        try {
            return target.hurt(source, damage);
        } finally {
            HEAVY_ATTACKER.remove();
        }
    }

    public static boolean isCombatWeapon(ItemStack stack) {
        if (stack.isEmpty() || HeavyIronBracerItem.isDebugStack(stack)) {
            return false;
        }
        return HeavyIronBracerItem.isRealStack(stack)
                || stack.getItem() instanceof ThrowingKnifeItem
                || QiInfusedWeapon.isWeapon(stack);
    }

    public static void clear(ServerPlayer player) {
        clear(player, true);
    }

    private static void clear(ServerPlayer player, boolean clearSpellPreparation) {
        cancelStrike(player);
        RECOVERY.remove(player.getUUID());
        player.getPersistentData().remove(COMBAT_UNTIL_TAG);
        player.getPersistentData().remove(MANUAL_COMBAT_TAG);
        player.getPersistentData().remove(POSTURE_BROKEN_UNTIL_TAG);
        player.getPersistentData().putFloat(POSTURE_TAG, maxPosture(player));
        RECENT_HITS.remove(player.getUUID());
        HEAVY_CHARGE_STARTS.remove(player.getUUID());
        COMBO_STATES.remove(player.getUUID());
        if (clearSpellPreparation) {
            interruptSpellPreparation(player);
        }
        FuchenItem.clear(player);
        setBlocking(player, false);
        sync(player, true);
    }

    public static void onLogout(ServerPlayer player) {
        cancelStrike(player);
        RECOVERY.remove(player.getUUID());
        com.example.immortal_cultivation_mod.spell.SelfExplosionConfirmation.cancel(player);
        RECENT_HITS.remove(player.getUUID());
        HEAVY_CHARGE_STARTS.remove(player.getUUID());
        LAST_SYNC.remove(player.getUUID());
        COMBO_STATES.remove(player.getUUID());
        BLOCKING_PLAYERS.remove(player.getUUID());
        SPELL_PREPARATIONS.remove(player.getUUID());
        FuchenItem.clear(player);
        WhipItem.clearAnimationState(player);
    }

    public static void sync(ServerPlayer player, boolean force) {
        boolean active = isInCombat(player);
        float max = maxPosture(player);
        float posture = active ? currentPosture(player) : max;
        boolean broken = active && isPostureBroken(player);
        int remaining = active && !isManualCombat(player)
                ? (int) Math.max(0L, player.getPersistentData().getLong(COMBAT_UNTIL_TAG)
                - player.serverLevel().getGameTime())
                : 0;
        SyncState next = new SyncState(active, posture, max, remaining, broken);
        if (!force && next.equals(LAST_SYNC.get(player.getUUID()))) {
            return;
        }
        LAST_SYNC.put(player.getUUID(), next);
        PacketDistributor.sendToPlayer(player, new CombatPayloads.ClientboundCombatStatePayload(
                active, posture, max, remaining, broken));
    }

    private static void setPosture(ServerPlayer player, float posture) {
        player.getPersistentData().putFloat(
                POSTURE_TAG, Math.max(0.0F, Math.min(maxPosture(player), posture)));
    }

    private record RecentHit(UUID attacker, long gameTime) {
    }

    private record SpellPreparation(String spellId, long startedAt) {
    }

    private record ComboState(int nextSlash, long lastTick) {
    }

    private record SyncState(
            boolean active, float posture, float maxPosture, int remainingTicks, boolean broken) {
    }
}
