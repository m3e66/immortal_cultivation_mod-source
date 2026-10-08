package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.block.ModBlocks;
import com.example.immortal_cultivation_mod.combat.CombatStateManager;
import com.example.immortal_cultivation_mod.entity.CultivationNpc;
import com.example.immortal_cultivation_mod.entity.NpcProfile;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import com.example.immortal_cultivation_mod.network.ModPayloads;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import com.example.immortal_cultivation_mod.world.YinQiField;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public class MysteriousRingItem extends Item implements ICurioItem {
    public static final String RING_SLOT = "ring";
    public static final int MAX_ENERGY = 1_000;
    public static final int AWAKENING_QI = 1_000;
    private static final int ELDER_MESSAGE_DURATION_TICKS = 80;
    private static final int SYSTEM_MESSAGE_DURATION_TICKS = 60;
    private static final long SEQUENCE_INTERVAL_TICKS = 90L;
    private static final long AWAKENING_SEQUENCE_DELAY_TICKS = 130L;

    private static final String STATE_TAG = ImmortalCultivationMod.MODID + ".mysterious_ring";
    private static final String VERSION = "Version";
    private static final String AWAKE = "Awake";
    private static final String AWAKENING_PROGRESS = "AwakeningProgress";
    private static final String AWAKENING_STARTED = "AwakeningStarted";
    private static final String AWAKENING_PAUSED = "AwakeningPaused";
    private static final String AWAKENING_MILESTONES = "AwakeningMilestones";
    private static final String ENERGY = "Energy";
    private static final String FIRST_AWAKENING_DONE = "FirstAwakeningDone";
    private static final String FIRST_EQUIP_DONE = "FirstEquipDone";
    private static final String PENDING_ENERGY_ONE = "PendingEnergyOne";
    private static final String LAST_UNEQUIPPED = "LastUnequipped";
    private static final String EQUIPPED_AT = "EquippedAt";
    private static final String NEXT_AMBIENT = "NextAmbient";
    private static final String LAST_COMBAT_DIALOGUE = "LastCombatDialogue";
    private static final String USED_DIALOGUES = "UsedDialogues";
    private static final String SEQUENCE = "Sequence";
    private static final String SEQUENCE_INDEX = "SequenceIndex";
    private static final String SEQUENCE_NEXT = "SequenceNext";
    private static final String MEDITATING = "Meditating";
    private static final String QI_FULL_WARNED = "QiFullWarned";
    private static final String QI_LOW_WARNED = "QiLowWarned";
    private static final String BREAKTHROUGH_READY = "BreakthroughReady";
    private static final String PENDING_UNEQUIPPED_BREAKTHROUGH = "PendingUnequippedBreakthrough";
    private static final String IN_COMBAT = "InCombat";
    private static final String FIRST_BATTLE_DONE = "FirstBattleDone";
    private static final String DIMENSION = "Dimension";
    private static final String LAST_SUNRISE_DAY = "LastSunriseDay";
    private static final String WAS_THUNDERING = "WasThundering";
    private static final String WAS_NIGHT = "WasNight";
    private static final String HIGH_QI_WARNED = "HighQiWarned";
    private static final String CURSED_AREA_WARNED = "CursedAreaWarned";
    private static final String INVENTORY_WARNED = "InventoryWarned";
    private static final String LOW_HP_WARNED = "LowHpWarned";
    private static final String CRITICAL_HP_WARNED = "CriticalHpWarned";
    private static final String LAST_DAMAGE_ID = "LastDamageId";
    private static final String SAME_DAMAGE_COUNT = "SameDamageCount";
    private static final String LAST_DAMAGE_TIME = "LastDamageTime";
    private static final String LAST_LOW_TARGET = "LastLowTarget";
    private static final String LAST_BREAKTHROUGH_FAILURE = "LastBreakthroughFailure";
    private static final String BREAKTHROUGH_FAILURES = "BreakthroughFailures";
    private static final String PENDING_REWARDS = "PendingRewards";
    private static final String TELEPORT_COOLDOWN = "TeleportCooldown";
    private static final String TELEPORT_COUNT = "TeleportCount";
    private static final String LAST_TELEPORT_COST = "LastTeleportCost";
    private static final String TELEPORT_RECOVERED_SPOKEN = "TeleportRecoveredSpoken";
    private static final String TELEPORT_ORIGIN_DIMENSION = "TeleportOriginDimension";
    private static final String TELEPORT_ORIGIN_X = "TeleportOriginX";
    private static final String TELEPORT_ORIGIN_Y = "TeleportOriginY";
    private static final String TELEPORT_ORIGIN_Z = "TeleportOriginZ";
    private static final String TELEPORT_TIME = "TeleportTime";
    private static final String RETURN_WARNING_SPOKEN = "ReturnWarningSpoken";

    private static final String PLAYER_WAS_EQUIPPED =
            "ImmortalCultivationMysteriousRingEquipped";
    private static final int ABSORB_PER_SECOND = 10;
    private static final int ENERGY_REGEN_PER_SECOND = 1;
    private static final int COMBAT_DIALOGUE_COOLDOWN = 20 * 25;
    private static final int TELEPORT_COOLDOWN_TICKS = 20 * 60 * 10;
    private static final int AMBIENT_MIN_TICKS = 20 * 60 * 3;
    private static final int AMBIENT_EXTRA_TICKS = 20 * 60 * 2;
    private static final int RETURN_WARNING_TICKS = 20 * 60 * 5;
    private static final int TELEPORT_RADIUS = 5_000;
    private static final int SAFE_POSITION_ATTEMPTS = 24;
    private static final float NORMAL_COMBAT_DIALOGUE_CHANCE = 0.125F;

    public MysteriousRingItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return RING_SLOT.equals(slotContext.identifier());
    }

    @Override
    public ICurio.DropRule getDropRule(
            SlotContext slotContext,
            DamageSource source,
            boolean recentlyHit,
            ItemStack stack) {
        return ICurio.DropRule.ALWAYS_KEEP;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        CompoundTag state = readState(stack);
        return state.contains(VERSION);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        CompoundTag state = readState(stack);
        int value = state.getBoolean(AWAKE)
                ? getEnergy(state)
                : Mth.clamp(state.getInt(AWAKENING_PROGRESS), 0, AWAKENING_QI);
        return Math.round(13.0F * value / (float) MAX_ENERGY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return readState(stack).getBoolean(AWAKE) ? 0xE8B83F : 0x7764C8;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        CompoundTag state = readState(stack);
        if (!state.contains(VERSION) || !state.getBoolean(AWAKE)) {
            int progress = Mth.clamp(state.getInt(AWAKENING_PROGRESS), 0, AWAKENING_QI);
            tooltip.add(Component.translatable(
                    "tooltip." + ImmortalCultivationMod.MODID + ".mysterious_ring.sleeping")
                    .withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.translatable(
                    "tooltip." + ImmortalCultivationMod.MODID + ".mysterious_ring.awakening",
                    progress, AWAKENING_QI).withStyle(ChatFormatting.DARK_PURPLE));
            return;
        }
        tooltip.add(Component.translatable(
                "tooltip." + ImmortalCultivationMod.MODID + ".mysterious_ring.awake")
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable(
                "tooltip." + ImmortalCultivationMod.MODID + ".mysterious_ring.energy",
                getEnergy(state), MAX_ENERGY).withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("ring.ai.hint").withStyle(ChatFormatting.AQUA));
    }

    public static void tick(ServerPlayer player) {
        ItemStack equipped = findEquipped(player);
        ItemStack ring = equipped.isEmpty() ? findOwned(player) : equipped;
        boolean equippedNow = !equipped.isEmpty();
        boolean wasEquipped = player.getPersistentData().getBoolean(PLAYER_WAS_EQUIPPED);
        if (ring.isEmpty()) {
            if (wasEquipped) {
                player.getPersistentData().putBoolean(PLAYER_WAS_EQUIPPED, false);
            }
            return;
        }

        CompoundTag state = readState(ring);
        CompoundTag before = state.copy();
        long now = player.level().getGameTime();
        initialize(player, state, now);
        if (state.getBoolean(AWAKE)) {
            boolean missingProfile = !state.hasUUID("ElderId");
            ensureElder(player, state);
            if (missingProfile) saveState(ring, state);
        }
        handleEquipTransition(player, ring, state, equippedNow, wasEquipped, now);

        if (!state.getBoolean(AWAKE)) {
            if (player.tickCount % 20 == 0) {
                tickAwakening(player, state, equippedNow, now);
            }
            saveIfChanged(ring, before, state);
            return;
        }

        if (player.tickCount % 20 == 0) {
            tickEnergy(player, state, equippedNow);
            if (equippedNow) {
                tickConditions(player, state, now);
                deliverPendingRewards(player, state);
            }
        }
        if (equippedNow) {
            tickSequence(player, state, now);
        }
        saveIfChanged(ring, before, state);
    }

    public static boolean tryEmergencyTeleport(ServerPlayer player, DamageSource source,
                                               float incomingDamage) {
        if (com.example.immortal_cultivation_mod.pagoda.PagodaSystem.adminKill(player,source)) return false;
        if (incomingDamage <= 0.0F || NpcProfile.sparPartner(player) != null) {
            return false;
        }
        float predictedHealth = player.getHealth() - incomingDamage;
        float threshold = Math.max(1.0F, player.getMaxHealth() * 0.08F);
        if (predictedHealth > threshold && incomingDamage < player.getHealth()) {
            return false;
        }

        ItemStack ring = findEquippedAwake(player);
        if (ring.isEmpty()) {
            return false;
        }
        if (elderProfiles(player).rescueRefused(elderId(player, ring), player.getUUID(), player.server.overworld().getGameTime())) {
            return false;
        }
        CompoundTag state = readState(ring);
        long now = player.level().getGameTime();
        if (now < state.getLong(TELEPORT_COOLDOWN)) {
            return false;
        }
        int cost = teleportEnergyCost(player);
        if (getEnergy(state) - cost < 1) {
            speakRandom(player, state, "combat.hp15_unavailable", 3);
            return false;
        }
        if (isSpaceLocked(player)) {
            speakRandom(player, state, "teleport.blocked", 3);
            saveState(ring, state);
            return false;
        }

        ServerLevel level = player.serverLevel();
        BlockPos destination = findSafeTeleportPosition(player, level);
        if (destination == null) {
            speakRandom(player, state, "teleport.blocked", 3);
            saveState(ring, state);
            return false;
        }

        Vec3 origin = player.position();
        speakRandom(player, state, "teleport.before", 6);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                origin.x, origin.y + player.getBbHeight() * 0.5D, origin.z,
                80, 0.65D, 0.9D, 0.65D, 0.16D);
        level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS, 1.2F, 0.72F);

        setEnergy(state, getEnergy(state) - cost);
        state.putLong(TELEPORT_COOLDOWN, now + TELEPORT_COOLDOWN_TICKS);
        state.putInt(LAST_TELEPORT_COST, cost);
        state.putBoolean(TELEPORT_RECOVERED_SPOKEN, false);
        state.putString(TELEPORT_ORIGIN_DIMENSION, player.level().dimension().location().toString());
        state.putDouble(TELEPORT_ORIGIN_X, origin.x);
        state.putDouble(TELEPORT_ORIGIN_Y, origin.y);
        state.putDouble(TELEPORT_ORIGIN_Z, origin.z);
        state.putLong(TELEPORT_TIME, now);
        state.putBoolean(RETURN_WARNING_SPOKEN, false);

        int saves = state.getInt(TELEPORT_COUNT) + 1;
        state.putInt(TELEPORT_COUNT, saves);
        player.teleportTo(level,
                destination.getX() + 0.5D,
                destination.getY(),
                destination.getZ() + 0.5D,
                Set.of(),
                player.getYRot(),
                player.getXRot());
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        player.resetFallDistance();
        player.invulnerableTime = Math.max(player.invulnerableTime, 40);
        player.setHealth(Math.max(1.0F, player.getHealth()));
        player.clearFire();

        level.sendParticles(ParticleTypes.PORTAL,
                player.getX(), player.getY() + player.getBbHeight() * 0.5D, player.getZ(),
                100, 0.8D, 1.0D, 0.8D, 0.2D);
        level.sendParticles(ParticleTypes.END_ROD,
                player.getX(), player.getY() + 0.8D, player.getZ(),
                32, 0.45D, 0.55D, 0.45D, 0.08D);
        level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS, 1.2F, 1.05F);

        speakRandom(player, state, "teleport.after", 6);
        if (saves == 1) {
            speakElder(player, "teleport.first");
        } else {
            speakElder(player, "teleport.multiple", saves);
        }
        if (player.getRandom().nextFloat() < 0.08F) {
            speakRandom(player, state, "teleport.humorous", 3);
        }
        speakLowEnergyAfterUse(player, state);
        saveState(ring, state);
        return true;
    }

    public static void onPlayerDamaged(ServerPlayer player, LivingEntity attacker,
                                       DamageSource source, float damage) {
        ItemStack ring = findEquippedAwake(player);
        if (ring.isEmpty() || damage <= 0.0F) {
            return;
        }
        CompoundTag state = readState(ring);
        long now = player.level().getGameTime();
        boolean firstBattleDialogue = beginCombatDialogue(player, state);
        String damageId = source.getMsgId();
        if (damageId.equals(state.getString(LAST_DAMAGE_ID))
                && now - state.getLong(LAST_DAMAGE_TIME) <= 20 * 12) {
            int count = state.getInt(SAME_DAMAGE_COUNT) + 1;
            state.putInt(SAME_DAMAGE_COUNT, count);
            if (count == 3) {
                speakElder(player, "combat.same_attack");
            }
        } else {
            state.putString(LAST_DAMAGE_ID, damageId);
            state.putInt(SAME_DAMAGE_COUNT, 1);
        }
        state.putLong(LAST_DAMAGE_TIME, now);

        float healthRatio = player.getHealth() / Math.max(1.0F, player.getMaxHealth());
        if (healthRatio <= 0.15F && !state.getBoolean(CRITICAL_HP_WARNED)) {
            state.putBoolean(CRITICAL_HP_WARNED, true);
            int cost = teleportEnergyCost(player);
            if (getEnergy(state) - cost >= 1
                    && now >= state.getLong(TELEPORT_COOLDOWN)) {
                speakRandom(player, state, "combat.hp15_available", 3);
            } else {
                speakRandom(player, state, "combat.hp15_unavailable", 3);
            }
        } else if (healthRatio <= 0.30F && !state.getBoolean(LOW_HP_WARNED)) {
            state.putBoolean(LOW_HP_WARNED, true);
            speakRandom(player, state, "combat.hp30", 4);
        }

        if (attacker != null && !firstBattleDialogue) {
            int difference = realmIndex(attacker) - CultivationLevels.getRealmIndex(
                    ModAttachments.getData(player).cultivationLevel());
            if (difference >= 1) {
                maybeCombatDialogue(player, state, "combat.much_stronger", 4, now);
            } else if (difference == 0 && attacker.getMaxHealth() > player.getMaxHealth() * 1.25F) {
                maybeCombatDialogue(player, state, "combat.slightly_stronger", 3, now);
            } else {
                maybeCombatDialogue(player, state, "combat.enter", 4, now);
            }
        }
        saveState(ring, state);
    }

    public static void onPlayerDamagedEnemy(ServerPlayer player, LivingEntity target,
                                            float damage) {
        ItemStack ring = findEquippedAwake(player);
        if (ring.isEmpty() || damage <= 0.0F || target == player) {
            return;
        }
        CompoundTag state = readState(ring);
        long now = player.level().getGameTime();
        boolean firstBattleDialogue = beginCombatDialogue(player, state);
        if (target.getHealth() / Math.max(1.0F, target.getMaxHealth()) <= 0.15F) {
            String targetId = target.getUUID().toString();
            if (!targetId.equals(state.getString(LAST_LOW_TARGET))) {
                state.putString(LAST_LOW_TARGET, targetId);
                speakRandom(player, state, "combat.enemy_low", 3);
            }
        }

        int playerRealm = CultivationLevels.getRealmIndex(
                ModAttachments.getData(player).cultivationLevel());
        int targetRealm = realmIndex(target);
        if (firstBattleDialogue) {
            saveState(ring, state);
            return;
        }
        if (target.getMaxHealth() >= player.getMaxHealth() * 3.0F) {
            maybeCombatDialogue(player, state, "combat.boss", 4, now);
        } else if (targetRealm + 1 < playerRealm) {
            maybeCombatDialogue(player, state, "combat.far_weaker", 3, now);
        } else {
            maybeCombatDialogue(player, state, "combat.enter", 4, now);
        }
        saveState(ring, state);
    }

    public static void onBlockedAttack(ServerPlayer player, float incomingDamage) {
        ItemStack ring = findEquippedAwake(player);
        if (ring.isEmpty() || incomingDamage < player.getMaxHealth() * 0.12F) {
            return;
        }
        CompoundTag state = readState(ring);
        maybeCombatDialogue(player, state, "combat.blocked_huge", 3,
                player.level().getGameTime());
        saveState(ring, state);
    }

    public static void onEntityKilled(ServerPlayer player, LivingEntity target) {
        ItemStack ring = findEquippedAwake(player);
        if (ring.isEmpty() || target == player) {
            return;
        }
        CompoundTag state = readState(ring);
        long now = player.level().getGameTime();
        if (target instanceof PathfinderMob mob
                && target instanceof CultivationNpc npc
                && NpcProfile.FANREN.equals(npc.npcArchetype())) {
            maybeCombatDialogue(player, state, "behavior.harmless_kill", 3, now);
        } else {
            int playerRealm = CultivationLevels.getRealmIndex(
                    ModAttachments.getData(player).cultivationLevel());
            int targetRealm = realmIndex(target);
            maybeCombatDialogue(player, state,
                    targetRealm > playerRealm ? "combat.kill_stronger" : "combat.kill_equal",
                    targetRealm > playerRealm ? 4 : 3, now);
        }
        saveState(ring, state);
    }

    public static void onSpellCast(ServerPlayer player, String spellId) {
        ItemStack ring = findEquippedAwake(player);
        if (ring.isEmpty()) {
            return;
        }
        CompoundTag state = readState(ring);
        if (ModSpells.ZIBAO.equals(ModSpells.normalizeId(spellId))) {
            speakRandom(player, state, "combat.forbidden", 3);
        }
        saveState(ring, state);
    }

    public static void onSpellProficiencyIncreased(ServerPlayer player, int oldValue,
                                                    int newValue) {
        if (proficiencyTier(oldValue) == proficiencyTier(newValue)) {
            return;
        }
        ItemStack ring = findEquippedAwake(player);
        if (ring.isEmpty()) {
            return;
        }
        CompoundTag state = readState(ring);
        speakRandom(player, state, "learning.proficiency", 4);
        saveState(ring, state);
    }

    public static void onLearningStarted(ServerPlayer player) {
        ItemStack ring = findEquippedAwake(player);
        if (ring.isEmpty()) {
            return;
        }
        CompoundTag state = readState(ring);
        speakRandom(player, state, "learning.begin", 4);
        saveState(ring, state);
    }

    public static void onLearningFailed(ServerPlayer player) {
        ItemStack ring = findEquippedAwake(player);
        if (ring.isEmpty()) {
            return;
        }
        CompoundTag state = readState(ring);
        speakRandom(player, state, "learning.failed", 4);
        saveState(ring, state);
    }

    public static void onLearningSucceeded(ServerPlayer player) {
        ItemStack ring = findEquippedAwake(player);
        if (ring.isEmpty()) {
            return;
        }
        CompoundTag state = readState(ring);
        speakRandom(player, state, "learning.success", 4);
        saveState(ring, state);
    }

    public static void onBreakthroughAttemptPoor(ServerPlayer player) {
        ItemStack ring = findEquippedAwake(player);
        if (ring.isEmpty()) {
            return;
        }
        CompoundTag state = readState(ring);
        speakRandom(player, state, "cultivation.breakthrough_poor", 4);
        saveState(ring, state);
    }

    public static void onBreakthroughFailure(ServerPlayer player) {
        ItemStack ring = findEquippedAwake(player);
        if (ring.isEmpty()) {
            return;
        }
        CompoundTag state = readState(ring);
        long now = player.level().getGameTime();
        int failures = now - state.getLong(LAST_BREAKTHROUGH_FAILURE) <= 20 * 60 * 2
                ? state.getInt(BREAKTHROUGH_FAILURES) + 1 : 1;
        state.putLong(LAST_BREAKTHROUGH_FAILURE, now);
        state.putInt(BREAKTHROUGH_FAILURES, failures);
        if (failures >= 3) {
            speakElder(player, "cultivation.breakthrough_fail_repeated");
        } else {
            speakRandom(player, state, "cultivation.breakthrough_failed", 4);
        }
        saveState(ring, state);
    }

    public static void onBreakthroughSuccess(ServerPlayer player, String oldLevel,
                                             String newLevel) {
        ItemStack ring = findOwned(player);
        if (ring.isEmpty()) {
            return;
        }
        CompoundTag state = readState(ring);
        if (!state.getBoolean(AWAKE)) {
            return;
        }
        boolean equipped = !findEquipped(player).isEmpty();
        boolean major = CultivationLevels.getRealmIndex(oldLevel)
                != CultivationLevels.getRealmIndex(newLevel);
        enqueueReward(player, state, newLevel, major);
        if (!equipped) {
            state.putBoolean(PENDING_UNEQUIPPED_BREAKTHROUGH, true);
        } else if (state.getString(SEQUENCE).isBlank()) {
            speakBreakthrough(player, state, newLevel, major);
        } else {
            state.putBoolean(PENDING_UNEQUIPPED_BREAKTHROUGH, true);
        }
        saveState(ring, state);
    }

    private static void initialize(ServerPlayer player, CompoundTag state, long now) {
        if (state.contains(VERSION)) {
            return;
        }
        state.putInt(VERSION, 1);
        state.putBoolean(AWAKE, false);
        state.putInt(AWAKENING_PROGRESS, 0);
        state.putInt(ENERGY, 1);
        state.putLong(NEXT_AMBIENT, nextAmbientTime(player, now));
        state.putString(DIMENSION, player.level().dimension().location().toString());
        sendSystem(player, "obtained");
    }

    private static void tickAwakening(ServerPlayer player, CompoundTag state,
                                      boolean equipped, long now) {
        ModAttachments.CultivationData data = ModAttachments.getData(player);
        if (data.qi() <= 0) {
            if (!state.getBoolean(AWAKENING_PAUSED)) {
                state.putBoolean(AWAKENING_PAUSED, true);
                sendSystem(player, "no_qi");
            }
            return;
        }
        if (!state.getBoolean(AWAKENING_STARTED)) {
            state.putBoolean(AWAKENING_STARTED, true);
            sendSystem(player, "absorption_started");
        } else if (state.getBoolean(AWAKENING_PAUSED)) {
            state.putBoolean(AWAKENING_PAUSED, false);
            sendSystem(player, "absorption_resumed");
        }

        int oldProgress = Mth.clamp(state.getInt(AWAKENING_PROGRESS), 0, AWAKENING_QI);
        int amount = Math.min(ABSORB_PER_SECOND,
                Math.min(data.qi(), AWAKENING_QI - oldProgress));
        if (amount <= 0) {
            return;
        }
        ModAttachments.setData(player, data.withQi(data.qi() - amount));
        int progress = oldProgress + amount;
        state.putInt(AWAKENING_PROGRESS, progress);
        int milestones = state.getInt(AWAKENING_MILESTONES);
        if (oldProgress < 250 && progress >= 250 && (milestones & 1) == 0) {
            sendSystem(player, "awakening_25");
            milestones |= 1;
        }
        if (oldProgress < 500 && progress >= 500 && (milestones & 2) == 0) {
            sendSystem(player, "awakening_50");
            milestones |= 2;
        }
        if (oldProgress < 750 && progress >= 750 && (milestones & 4) == 0) {
            sendSystem(player, "awakening_75");
            milestones |= 4;
        }
        state.putInt(AWAKENING_MILESTONES, milestones);

        if (progress >= AWAKENING_QI) {
            state.putBoolean(AWAKE, true);
            ensureElder(player, state);
            saveState(findOwned(player), state);
            state.putInt(ENERGY, 1);
            state.putBoolean(PENDING_ENERGY_ONE, true);
            sendSystem(player, "awakening_complete");
            sendSystem(player, "awakening_status", 1, MAX_ENERGY);
            if (equipped) {
                startSequence(state, "awakening", now);
            }
        }
    }

    private static void handleEquipTransition(ServerPlayer player, ItemStack ring,
                                              CompoundTag state, boolean equipped,
                                              boolean wasEquipped, long now) {
        if (equipped == wasEquipped) {
            return;
        }
        player.getPersistentData().putBoolean(PLAYER_WAS_EQUIPPED, equipped);
        if (!state.getBoolean(AWAKE)) {
            return;
        }
        if (equipped) {
            state.putLong(EQUIPPED_AT, now);
            if (!state.getBoolean(FIRST_AWAKENING_DONE)) {
                startSequence(state, "awakening", now);
            } else if (!state.getBoolean(FIRST_EQUIP_DONE)) {
                startSequence(state, "first_equip", now);
            } else {
                long lastUnequipped = state.getLong(LAST_UNEQUIPPED);
                if (lastUnequipped > 0L && now - lastUnequipped >= 24_000L) {
                    speakRandom(player, state, "equip.long_unused", 3);
                } else {
                    speakRandom(player, state, "equip.normal", 6);
                }
                handleRememberedBreakthrough(player, state);
            }
        } else {
            long equippedFor = now - state.getLong(EQUIPPED_AT);
            if (equippedFor >= 0L && equippedFor <= 20 * 10) {
                speakElder(player, "remove.quick");
            } else {
                speakRandom(player, state, "remove.normal", 3);
            }
            state.putLong(LAST_UNEQUIPPED, now);
        }
        saveState(ring, state);
    }

    private static void tickEnergy(ServerPlayer player, CompoundTag state,
                                   boolean equipped) {
        int oldEnergy = getEnergy(state);
        if (oldEnergy < MAX_ENERGY) {
            setEnergy(state, oldEnergy + ENERGY_REGEN_PER_SECOND);
        }
        int energy = getEnergy(state);
        if (!equipped || energy == oldEnergy) {
            return;
        }
        if (oldEnergy < 100 && energy >= 100) {
            speakElder(player, "energy.100");
        }
        if (oldEnergy < 250 && energy >= 250) {
            speakElder(player, "energy.250");
        }
        if (oldEnergy < 500 && energy >= 500) {
            speakElder(player, "energy.500");
        }
        if (oldEnergy < 750 && energy >= 750) {
            speakElder(player, "energy.750");
        }
        if (oldEnergy < MAX_ENERGY && energy >= MAX_ENERGY) {
            speakElder(player, "energy.1000");
        }
        int lastCost = state.getInt(LAST_TELEPORT_COST);
        if (lastCost > 0 && oldEnergy <= lastCost && energy > lastCost
                && !state.getBoolean(TELEPORT_RECOVERED_SPOKEN)) {
            state.putBoolean(TELEPORT_RECOVERED_SPOKEN, true);
            speakElder(player, "energy.teleport_ready");
        }
    }

    private static void tickConditions(ServerPlayer player, CompoundTag state,
                                       long now) {
        ModAttachments.CultivationData data = ModAttachments.getData(player);
        boolean meditating = data.isMeditating();
        if (meditating && !state.getBoolean(MEDITATING)) {
            speakRandom(player, state, "cultivation.begin", 4);
        }
        state.putBoolean(MEDITATING, meditating);

        int maxQi = Math.max(1,
                CultivationLevels.getLevelDef(data.cultivationLevel()).maxQi()
                        + data.maxQiBonus());
        boolean qiNearlyFull = data.qi() >= Math.max(1, maxQi * 9 / 10);
        if (qiNearlyFull && !state.getBoolean(QI_FULL_WARNED)) {
            state.putBoolean(QI_FULL_WARNED, true);
            speakRandom(player, state, "cultivation.qi_full", 3);
        } else if (!qiNearlyFull && data.qi() < maxQi * 3 / 4) {
            state.putBoolean(QI_FULL_WARNED, false);
        }
        boolean qiLow = data.qi() <= Math.max(1, maxQi / 10);
        if (CombatStateManager.isInCombat(player)
                && qiLow && !state.getBoolean(QI_LOW_WARNED)) {
            state.putBoolean(QI_LOW_WARNED, true);
            speakRandom(player, state, "combat.qi_low", 3);
        } else if (!qiLow) {
            state.putBoolean(QI_LOW_WARNED, false);
        }

        long requiredProgress = CultivationLevels.getTotalQiNeeded(data.cultivationLevel());
        boolean ready = data.cultivationProgress() >= requiredProgress && data.qi() >= maxQi;
        if (ready && !state.getBoolean(BREAKTHROUGH_READY)) {
            state.putBoolean(BREAKTHROUGH_READY, true);
            speakRandom(player, state, "cultivation.breakthrough_ready", 3);
        } else if (!ready) {
            state.putBoolean(BREAKTHROUGH_READY, false);
        }

        float healthRatio = player.getHealth() / Math.max(1.0F, player.getMaxHealth());
        if (healthRatio > 0.40F) {
            state.putBoolean(LOW_HP_WARNED, false);
        }
        if (healthRatio > 0.25F) {
            state.putBoolean(CRITICAL_HP_WARNED, false);
        }

        boolean inCombat = CombatStateManager.isInCombat(player);
        boolean wasInCombat = state.getBoolean(IN_COMBAT);
        if (inCombat && !wasInCombat) {
            if (!beginCombatDialogue(player, state)) {
                maybeCombatDialogue(player, state, "combat.enter", 4, now);
            }
        } else if (!inCombat && wasInCombat) {
            maybeCombatDialogue(player, state, "combat.retreat", 3, now);
        }
        state.putBoolean(IN_COMBAT, inCombat);

        String dimension = player.level().dimension().location().toString();
        String previousDimension = state.getString(DIMENSION);
        if (!previousDimension.isBlank() && !previousDimension.equals(dimension)) {
            speakRandom(player, state, "exploration.dimension", 3);
        }
        state.putString(DIMENSION, dimension);

        boolean thundering = player.serverLevel().isThundering();
        if (thundering && !state.getBoolean(WAS_THUNDERING)) {
            speakRandom(player, state, "exploration.thunder", 3);
        }
        state.putBoolean(WAS_THUNDERING, thundering);

        long dayTime = player.serverLevel().getDayTime();
        long day = dayTime / 24_000L;
        long timeOfDay = Math.floorMod(dayTime, 24_000L);
        if (timeOfDay < 800L && state.getLong(LAST_SUNRISE_DAY) != day) {
            state.putLong(LAST_SUNRISE_DAY, day);
            speakRandom(player, state, "exploration.sunrise", 3);
        }
        boolean night = timeOfDay >= 13_000L && timeOfDay <= 23_000L;
        if (night && !state.getBoolean(WAS_NIGHT)) {
            speakRandom(player, state, "exploration.night", 3);
        }
        state.putBoolean(WAS_NIGHT, night);

        int ambientQi = ServerEvents.ambientQiValue(player);
        if (ambientQi >= 25 && !state.getBoolean(HIGH_QI_WARNED)) {
            state.putBoolean(HIGH_QI_WARNED, true);
            speakRandom(player, state,
                    ambientQi >= 50 ? "exploration.spirit_vein" : "exploration.high_qi", 3);
        } else if (ambientQi < 10) {
            state.putBoolean(HIGH_QI_WARNED, false);
        }
        int yinQi = YinQiField.sample(player.serverLevel(), player.blockPosition());
        if (yinQi >= 8 && !state.getBoolean(CURSED_AREA_WARNED)) {
            state.putBoolean(CURSED_AREA_WARNED, true);
            speakRandom(player, state, "exploration.cursed_area", 3);
        } else if (yinQi < 4) {
            state.putBoolean(CURSED_AREA_WARNED, false);
        }

        int occupied = 0;
        int inventorySlots = player.getInventory().getContainerSize();
        for (int slot = 0; slot < inventorySlots; slot++) {
            if (!player.getInventory().getItem(slot).isEmpty()) {
                occupied++;
            }
        }
        boolean inventoryNearlyFull = occupied >= inventorySlots - 3;
        if (inventoryNearlyFull && !state.getBoolean(INVENTORY_WARNED)) {
            state.putBoolean(INVENTORY_WARNED, true);
            speakRandom(player, state, "behavior.inventory_full", 3);
        } else if (!inventoryNearlyFull) {
            state.putBoolean(INVENTORY_WARNED, false);
        }

        tickReturnWarning(player, state, now);
        tickAmbient(player, state, now);
        if (state.getString(SEQUENCE).isBlank()) {
            handleRememberedBreakthrough(player, state);
        }
    }

    private static void tickAmbient(ServerPlayer player, CompoundTag state, long now) {
        long next = state.getLong(NEXT_AMBIENT);
        if (next <= 0L) {
            state.putLong(NEXT_AMBIENT, nextAmbientTime(player, now));
            return;
        }
        if (now < next || !state.getString(SEQUENCE).isBlank()) {
            return;
        }
        state.putLong(NEXT_AMBIENT, nextAmbientTime(player, now));
        if (player.getDeltaMovement().horizontalDistanceSqr() < 1.0E-4D
                && !player.isUsingItem()) {
            speakRandom(player, state, "behavior.idle", 4);
        } else {
            speakRandom(player, state, "ambient", 11);
        }
    }

    private static void tickReturnWarning(ServerPlayer player, CompoundTag state,
                                          long now) {
        long teleportTime = state.getLong(TELEPORT_TIME);
        if (teleportTime <= 0L || now - teleportTime > RETURN_WARNING_TICKS
                || state.getBoolean(RETURN_WARNING_SPOKEN)
                || !player.level().dimension().location().toString().equals(
                state.getString(TELEPORT_ORIGIN_DIMENSION))) {
            return;
        }
        Vec3 origin = new Vec3(
                state.getDouble(TELEPORT_ORIGIN_X),
                state.getDouble(TELEPORT_ORIGIN_Y),
                state.getDouble(TELEPORT_ORIGIN_Z));
        if (player.position().distanceToSqr(origin) <= 64.0D * 64.0D
                && now - teleportTime > 100L) {
            state.putBoolean(RETURN_WARNING_SPOKEN, true);
            speakRandom(player, state, "teleport.returned", 3);
        }
    }

    private static void startSequence(CompoundTag state, String sequence, long now) {
        if (!state.getString(SEQUENCE).isBlank()) {
            return;
        }
        state.putString(SEQUENCE, sequence);
        state.putInt(SEQUENCE_INDEX, 0);
        state.putLong(SEQUENCE_NEXT, now + ("awakening".equals(sequence)
                ? AWAKENING_SEQUENCE_DELAY_TICKS
                : 0L));
    }

    private static void tickSequence(ServerPlayer player, CompoundTag state,
                                     long now) {
        String sequence = state.getString(SEQUENCE);
        if (sequence.isBlank() || now < state.getLong(SEQUENCE_NEXT)) {
            return;
        }
        int count = "awakening".equals(sequence) ? 6 : 3;
        int index = state.getInt(SEQUENCE_INDEX);
        if (index < count) {
            speakElder(player, sequence + "." + (index + 1));
            state.putInt(SEQUENCE_INDEX, index + 1);
            state.putLong(SEQUENCE_NEXT, now + SEQUENCE_INTERVAL_TICKS);
            return;
        }
        if ("awakening".equals(sequence)) {
            state.putBoolean(FIRST_AWAKENING_DONE, true);
            state.putString(SEQUENCE, "first_equip");
            state.putInt(SEQUENCE_INDEX, 0);
            state.putLong(SEQUENCE_NEXT, now + 25L);
            return;
        }

        state.putBoolean(FIRST_EQUIP_DONE, true);
        state.remove(SEQUENCE);
        state.remove(SEQUENCE_INDEX);
        state.remove(SEQUENCE_NEXT);
        if (state.getBoolean(PENDING_ENERGY_ONE)) {
            state.putBoolean(PENDING_ENERGY_ONE, false);
            speakElder(player, "energy.1");
        }
        handleRememberedBreakthrough(player, state);
    }

    private static void handleRememberedBreakthrough(ServerPlayer player,
                                                     CompoundTag state) {
        if (!state.getBoolean(PENDING_UNEQUIPPED_BREAKTHROUGH)) {
            return;
        }
        state.putBoolean(PENDING_UNEQUIPPED_BREAKTHROUGH, false);
        speakElder(player, "cultivation.breakthrough_while_unequipped");
    }

    private static void speakBreakthrough(ServerPlayer player, CompoundTag state,
                                          String newLevel, boolean major) {
        if (!major) {
            speakRandom(player, state, "cultivation.minor_advance", 4);
            return;
        }
        String realm = CultivationLevels.getLevelDef(newLevel).realm();
        if (CultivationLevels.REALM_LIANQI.equals(realm)) {
            speakElder(player, "cultivation.major.lianqi");
        } else if (CultivationLevels.REALM_ZHUJI.equals(realm)) {
            speakElder(player, "cultivation.major.zhuji");
        } else if (CultivationLevels.REALM_JINDAN.equals(realm)) {
            speakElder(player, "cultivation.major.jindan");
        } else if (CultivationLevels.REALM_YUANYING.equals(realm)) {
            speakElder(player, "cultivation.major.yuanying");
        } else {
            speakElder(player, "cultivation.major.higher");
        }
    }

    private static void enqueueReward(ServerPlayer player, CompoundTag state,
                                      String newLevel, boolean major) {
        ModAttachments.CultivationData data = ModAttachments.getData(player);
        List<ModSpells.SpellDef> eligible = ModSpells.all().stream()
                .filter(spell -> newLevel.equals(spell.requiredLevel()))
                .filter(spell -> ModSpells.isRandomRewardEligible(spell.id()))
                .filter(spell -> !data.knownSpells().contains(spell.id()))
                .filter(spell -> elderProfiles(player).knows(elderId(player, findOwned(player)), spell.id()))
                .toList();
        String spellId = eligible.isEmpty()
                ? ""
                : eligible.get(elderChoice(player, eligible.size())).id();
        boolean noEligibleSpell = eligible.isEmpty();
        MaterialReward material = noEligibleSpell
                ? replacementMaterialReward(player, newLevel)
                : materialReward(player, newLevel, major);

        CompoundTag reward = new CompoundTag();
        reward.putString("Level", newLevel);
        reward.putBoolean("Major", major);
        reward.putBoolean("NoEligibleSpell", noEligibleSpell);
        reward.putString("Spell", spellId);
        reward.putString("Material", material.itemId());
        reward.putInt("Count", material.count());
        ListTag rewards = state.getList(PENDING_REWARDS, Tag.TAG_COMPOUND);
        rewards.add(reward);
        state.put(PENDING_REWARDS, rewards);
    }

    private static MaterialReward materialReward(ServerPlayer player, String level,
                                                 boolean major) {
        int realm = CultivationLevels.getRealmIndex(level);
        String itemId;
        int count;
        if (realm <= 1) {
            itemId = ImmortalCultivationMod.MODID + ":spirit_stone";
            count = elderRange(player, 8, 17);
        } else if (realm <= 3) {
            itemId = ImmortalCultivationMod.MODID + ":medium_spirit_stone";
            count = elderRange(player, major ? 6 : 3, major ? 13 : 8);
        } else {
            itemId = ImmortalCultivationMod.MODID + ":high_spirit_stone";
            count = elderRange(player, major ? 3 : 1, major ? 7 : 4);
        }
        return new MaterialReward(itemId, elderProfiles(player).reserveGift(
                elderId(player, findOwned(player)), itemId, count));
    }

    private static MaterialReward replacementMaterialReward(ServerPlayer player,
                                                            String level) {
        int realm = CultivationLevels.getRealmIndex(level);
        if (realm <= 1) {
            return new MaterialReward(
                    ImmortalCultivationMod.MODID + ":medium_spirit_stone",
                    reserveReplacement(player, "medium_spirit_stone", 4, 9));
        }
        return new MaterialReward(
                ImmortalCultivationMod.MODID + ":high_spirit_stone",
                reserveReplacement(player, "high_spirit_stone", realm <= 3 ? 2 : 5,
                        realm <= 3 ? 6 : 11));
    }

    private static void deliverPendingRewards(ServerPlayer player,
                                              CompoundTag state) {
        if (!state.getString(SEQUENCE).isBlank()) {
            return;
        }
        ListTag rewards = state.getList(PENDING_REWARDS, Tag.TAG_COMPOUND);
        if (rewards.isEmpty()) {
            return;
        }
        CompoundTag reward = rewards.getCompound(0);
        String queuedSpellId = ModSpells.normalizeId(reward.getString("Spell"));
        ModSpells.SpellDef queuedSpell = queuedSpellId.isBlank()
                ? null : ModSpells.get(queuedSpellId);
        ModAttachments.CultivationData currentData = ModAttachments.getData(player);
        if (queuedSpell != null
                && (currentData.knownSpells().contains(queuedSpellId)
                || ModSpells.isInnateKnown(queuedSpellId, currentData))) {
            MaterialReward replacement = replacementMaterialReward(
                    player, reward.getString("Level"));
            reward.putString("Material", replacement.itemId());
            reward.putInt("Count", replacement.count());
            reward.putString("Spell", "");
            reward.putBoolean("ReplacedKnownSpell", true);
            rewards.set(0, reward);
            state.put(PENDING_REWARDS, rewards);
        }
        String materialId = reward.getString("Material");
        int count = reward.getInt("Count");
        ResourceLocation materialKey = ResourceLocation.tryParse(materialId);
        Item material = materialKey == null ? Items.AIR
                : BuiltInRegistries.ITEM.get(materialKey);
        ItemStack materialStack = material == Items.AIR || count <= 0
                ? ItemStack.EMPTY : new ItemStack(material, count);
        if (!materialStack.isEmpty()) {
            player.getInventory().add(materialStack);
            elderProfiles(player).delivered(elderId(player, findOwned(player)), materialId,
                    count - materialStack.getCount());
            if (!materialStack.isEmpty()) {
                reward.putInt("Count", materialStack.getCount());
                rewards.set(0, reward);
                state.put(PENDING_REWARDS, rewards);
                if (!reward.getBoolean("InventoryWarned")) {
                    reward.putBoolean("InventoryWarned", true);
                    rewards.set(0, reward);
                    state.put(PENDING_REWARDS, rewards);
                    speakElder(player, "reward.inventory_full");
                }
                return;
            }
        }

        String spellId = ModSpells.normalizeId(reward.getString("Spell"));
        ModSpells.SpellDef spell = spellId.isBlank() ? null : ModSpells.get(spellId);
        ModAttachments.CultivationData data = ModAttachments.getData(player);
        boolean gaveSpell = false;
        boolean replacedKnownSpell = false;
        if (spell != null) {
            if (data.knownSpells().contains(spellId)
                    || ModSpells.isInnateKnown(spellId, data)) {
                replacedKnownSpell = true;
            } else {
                ArrayList<String> known = new ArrayList<>(data.knownSpells());
                known.add(spellId);
                ModAttachments.setData(player, data.withKnownSpells(List.copyOf(known)));
                ServerEvents.syncPlayerData(player);
                gaveSpell = true;
                elderProfiles(player).taught(elderId(player, findOwned(player)), spellId);
            }
        }

        if (gaveSpell && material != Items.AIR && count > 0) {
            speakElder(player, "reward.both", spell.name(), material.getDescription());
        } else if (gaveSpell) {
            speakRandomWithArg(player, state, "reward.spell", 5, spell.name());
        } else if (replacedKnownSpell || reward.getBoolean("ReplacedKnownSpell")) {
            speakElder(player, "reward.already_known");
        } else if (reward.getBoolean("NoEligibleSpell")) {
            speakElder(player, "reward.no_spells");
        } else if (material != Items.AIR && count > 0) {
            speakRandomWithArg(player, state, "reward.material", 4,
                    material.getDescription());
        } else {
            speakElder(player, "reward.no_spells");
        }
        rewards.remove(0);
        state.put(PENDING_REWARDS, rewards);
    }

    private static void speakLowEnergyAfterUse(ServerPlayer player,
                                               CompoundTag state) {
        int energy = getEnergy(state);
        if (energy == 1) {
            speakElder(player, "energy.exactly_one");
        } else if (energy < MAX_ENERGY / 5) {
            speakRandom(player, state, "energy.low", 3);
        }
    }

    private static void maybeCombatDialogue(ServerPlayer player, CompoundTag state,
                                            String pool, int count, long now) {
        if (!state.getString(SEQUENCE).isBlank()
                || now - state.getLong(LAST_COMBAT_DIALOGUE) < COMBAT_DIALOGUE_COOLDOWN
                || player.getRandom().nextFloat() >= NORMAL_COMBAT_DIALOGUE_CHANCE) {
            return;
        }
        state.putLong(LAST_COMBAT_DIALOGUE, now);
        speakRandom(player, state, pool, count);
    }

    private static boolean beginCombatDialogue(ServerPlayer player,
                                               CompoundTag state) {
        if (!CombatStateManager.isInCombat(player)
                || state.getBoolean(IN_COMBAT)) {
            return false;
        }
        state.putBoolean(IN_COMBAT, true);
        if (state.getBoolean(FIRST_BATTLE_DONE)) {
            return false;
        }
        state.putBoolean(FIRST_BATTLE_DONE, true);
        speakElder(player, "combat.first");
        return true;
    }

    private static void speakRandom(ServerPlayer player, CompoundTag state,
                                    String pool, int count) {
        speakRandomWithArg(player, state, pool, count);
    }

    private static void speakRandomWithArg(ServerPlayer player, CompoundTag state,
                                           String pool, int count, Object... args) {
        if (count <= 0 || count >= 31) {
            return;
        }
        CompoundTag used = state.getCompound(USED_DIALOGUES);
        int mask = used.getInt(pool);
        int fullMask = (1 << count) - 1;
        if ((mask & fullMask) == fullMask) {
            mask = 0;
        }
        int remaining = fullMask & ~mask;
        int pick = elderChoice(player, Integer.bitCount(remaining));
        int selected = 0;
        for (int index = 0; index < count; index++) {
            if ((remaining & (1 << index)) == 0) {
                continue;
            }
            if (pick-- == 0) {
                selected = index;
                break;
            }
        }
        used.putInt(pool, mask | (1 << selected));
        state.put(USED_DIALOGUES, used);
        speakElder(player, pool + "." + (selected + 1), args);
    }

    private static void speakElder(ServerPlayer player, String suffix,
                                   Object... args) {
        ItemStack ring = findOwned(player);
        String name = ring.isEmpty() || !readState(ring).getBoolean(AWAKE) ? "戒老"
                : elderProfiles(player).profile(elderId(player, ring)).getString("Name");
        PacketDistributor.sendToPlayer(player, new ModPayloads.ClientboundRingDialoguePayload(
                Component.literal("【" + name + "】").append(Component.translatable(
                        "dialogue." + ImmortalCultivationMod.MODID
                                + ".mysterious_ring." + suffix,
                        args)).withStyle(ChatFormatting.GOLD),
                true,
                ELDER_MESSAGE_DURATION_TICKS));
    }

    private static void sendSystem(ServerPlayer player, String suffix,
                                   Object... args) {
        PacketDistributor.sendToPlayer(player, new ModPayloads.ClientboundRingDialoguePayload(
                Component.translatable(
                        "message." + ImmortalCultivationMod.MODID
                                + ".mysterious_ring." + suffix,
                        args).withStyle(ChatFormatting.GRAY),
                false,
                SYSTEM_MESSAGE_DURATION_TICKS));
    }

    private static long nextAmbientTime(ServerPlayer player, long now) {
        return now + AMBIENT_MIN_TICKS
                + player.getRandom().nextInt(AMBIENT_EXTRA_TICKS + 1);
    }

    private static int teleportEnergyCost(ServerPlayer player) {
        return Math.max(1, Mth.ceil(player.getMaxHealth() * 0.01F));
    }

    private static boolean isSpaceLocked(ServerPlayer player) {
        BlockPos feet = player.blockPosition();
        return player.level().getBlockState(feet).is(ModBlocks.SUOKONG_BARRIER.get())
                || player.level().getBlockState(feet.above()).is(ModBlocks.SUOKONG_BARRIER.get());
    }

    private static BlockPos findSafeTeleportPosition(ServerPlayer player,
                                                     ServerLevel level) {
        int originX = player.getBlockX();
        int originZ = player.getBlockZ();
        for (int attempt = 0; attempt < SAFE_POSITION_ATTEMPTS; attempt++) {
            double angle = player.getRandom().nextDouble() * Math.PI * 2.0D;
            double radius = 256.0D + player.getRandom().nextDouble()
                    * (TELEPORT_RADIUS - 256.0D);
            int x = originX + Mth.floor(Math.cos(angle) * radius);
            int z = originZ + Mth.floor(Math.sin(angle) * radius);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos feet = new BlockPos(x, y, z);
            if (isSafeTeleportPosition(level, feet)) {
                return feet;
            }
        }
        return null;
    }

    private static boolean isSafeTeleportPosition(ServerLevel level, BlockPos feet) {
        if (!level.getWorldBorder().isWithinBounds(feet)
                || feet.getY() <= level.getMinBuildHeight() + 1
                || feet.getY() >= level.getMaxBuildHeight() - 2) {
            return false;
        }
        BlockPos floorPos = feet.below();
        BlockState floor = level.getBlockState(floorPos);
        BlockState body = level.getBlockState(feet);
        BlockState head = level.getBlockState(feet.above());
        if (!floor.getFluidState().isEmpty()
                || !body.getFluidState().isEmpty()
                || !head.getFluidState().isEmpty()
                || !floor.isFaceSturdy(level, floorPos, Direction.UP)
                || !body.getCollisionShape(level, feet).isEmpty()
                || !head.getCollisionShape(level, feet.above()).isEmpty()) {
            return false;
        }
        return !floor.is(Blocks.MAGMA_BLOCK)
                && !floor.is(Blocks.CACTUS)
                && !floor.is(Blocks.CAMPFIRE)
                && !floor.is(Blocks.SOUL_CAMPFIRE)
                && !floor.is(Blocks.FIRE)
                && !floor.is(Blocks.SOUL_FIRE)
                && !body.is(Blocks.POWDER_SNOW);
    }

    private static int realmIndex(LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            return CultivationLevels.getRealmIndex(
                    ModAttachments.getData(player).cultivationLevel());
        }
        if (entity instanceof PathfinderMob mob && entity instanceof CultivationNpc npc) {
            return CultivationLevels.getRealmIndex(
                    NpcProfile.realm(mob, npc.npcArchetype()));
        }
        return 0;
    }

    private static int proficiencyTier(int value) {
        if (value >= 10_000) {
            return 4;
        }
        if (value >= 1_000) {
            return 3;
        }
        if (value >= 500) {
            return 2;
        }
        return value >= 100 ? 1 : 0;
    }

    public static ItemStack findEquippedAwake(ServerPlayer player) {
        ItemStack stack = findEquipped(player);
        return !stack.isEmpty() && readState(stack).getBoolean(AWAKE)
                ? stack : ItemStack.EMPTY;
    }

    public static ItemStack findTalkRing(ServerPlayer player) {
        // Holding a ring chooses that elder; otherwise use the worn ring.
        for (var hand : net.minecraft.world.InteractionHand.values()) {
            ItemStack held = player.getItemInHand(hand);
            if (held.is(ModItems.MYSTERIOUS_RING.get()) && readState(held).getBoolean(AWAKE)) return held;
        }
        return findEquippedAwake(player);
    }

    public static ItemStack findConversationRing(ServerPlayer player, java.util.UUID elder) {
        ItemStack active = findTalkRing(player);
        if (!active.isEmpty() && (elder == null || elder.equals(elderId(player, active)))) return active;
        ItemStack worn = findEquippedAwake(player);
        if (!worn.isEmpty() && (elder == null || elder.equals(elderId(player, worn)))) return worn;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.MYSTERIOUS_RING.get()) && readState(stack).getBoolean(AWAKE)
                    && (elder == null || elder.equals(elderId(player, stack)))) return stack;
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.is(ModItems.MYSTERIOUS_RING.get()) && readState(stack).getBoolean(AWAKE)
                    && (elder == null || elder.equals(elderId(player, stack)))) return stack;
        }
        return ItemStack.EMPTY;
    }

    public static String elderLiveRingFacts(ServerPlayer player, ItemStack ring) {
        CompoundTag state = readState(ring);
        return "Stored ring energy=" + getEnergy(state) + "/" + MAX_ENERGY
                + "; emergency teleport cooldown remaining ticks="
                + Math.max(0, state.getLong(TELEPORT_COOLDOWN) - player.level().getGameTime())
                + "; emergency rescues=" + state.getInt(TELEPORT_COUNT) + ".";
    }

    private static ItemStack findEquipped(ServerPlayer player) {
        return CuriosApi.getCuriosInventory(player)
                .flatMap(handler -> handler.findFirstCurio(ModItems.MYSTERIOUS_RING.get()))
                .filter(result -> RING_SLOT.equals(result.slotContext().identifier()))
                .map(result -> result.stack())
                .orElse(ItemStack.EMPTY);
    }

    private static ItemStack findOwned(ServerPlayer player) {
        ItemStack equipped = findEquipped(player);
        if (!equipped.isEmpty()) {
            return equipped;
        }
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(ModItems.MYSTERIOUS_RING.get())) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static int getEnergy(CompoundTag state) {
        return Mth.clamp(state.getInt(ENERGY), 1, MAX_ENERGY);
    }

    private static java.util.UUID ensureElder(ServerPlayer player, CompoundTag state) {
        if (!state.hasUUID("ElderId")) state.putUUID("ElderId", java.util.UUID.randomUUID());
        java.util.UUID id = state.getUUID("ElderId");
        elderProfiles(player).profile(id);
        return id;
    }

    public static java.util.UUID elderId(ServerPlayer player, ItemStack ring) {
        CompoundTag state = readState(ring);
        if (ring.isEmpty() || !state.getBoolean(AWAKE)) throw new IllegalArgumentException("Ring must be awake");
        boolean needsId = !state.hasUUID("ElderId");
        java.util.UUID id = ensureElder(player, state);
        if (needsId) saveState(ring, state);
        return id;
    }

    private static com.example.immortal_cultivation_mod.ring.RingElderProfiles elderProfiles(ServerPlayer player) {
        return com.example.immortal_cultivation_mod.ring.RingElderProfiles.get(player.server);
    }

    private static int elderChoice(ServerPlayer player, int bound) {
        return elderProfiles(player).choose(elderId(player, findOwned(player)), bound);
    }

    private static int elderRange(ServerPlayer player, int min, int max) {
        return min + elderChoice(player, max - min);
    }

    private static int reserveReplacement(ServerPlayer player, String item, int min, int max) {
        return elderProfiles(player).reserveGift(elderId(player, findOwned(player)),
                ImmortalCultivationMod.MODID + ":" + item, elderRange(player, min, max));
    }

    private static void setEnergy(CompoundTag state, int energy) {
        state.putInt(ENERGY, Mth.clamp(energy, 1, MAX_ENERGY));
    }

    private static CompoundTag readState(ItemStack stack) {
        CompoundTag custom = stack.getOrDefault(
                DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return custom.contains(STATE_TAG, Tag.TAG_COMPOUND)
                ? custom.getCompound(STATE_TAG).copy()
                : new CompoundTag();
    }

    private static void saveIfChanged(ItemStack stack, CompoundTag before,
                                      CompoundTag state) {
        if (!before.equals(state)) {
            saveState(stack, state);
        }
    }

    private static void saveState(ItemStack stack, CompoundTag state) {
        CompoundTag custom = stack.getOrDefault(
                DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag previous = custom.getCompound(STATE_TAG);
        if (!state.hasUUID("ElderId") && previous.hasUUID("ElderId"))
            state.putUUID("ElderId", previous.getUUID("ElderId"));
        custom.put(STATE_TAG, state.copy());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(custom));
    }

    private record MaterialReward(String itemId, int count) {
    }
}
