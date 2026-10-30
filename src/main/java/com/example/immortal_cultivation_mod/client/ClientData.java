package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.attribute.ModAttributes;
import com.example.immortal_cultivation_mod.config.BlockBreakingSettings;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import com.example.immortal_cultivation_mod.spell.UndeadAbilitySystem;
import com.example.immortal_cultivation_mod.spell.ShieldDisplay;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClientData {
    public static final int MAX_WHEEL_SPELLS = 8;
    public static ModAttachments.CultivationData cultivationData = ModAttachments.CultivationData.createDefault();
    public static String playerIdentity = "wandering_cultivator";
    public static String jindanQuality = "liedan";
    private static final Set<UUID> MEDITATING_PLAYERS = ConcurrentHashMap.newKeySet();
    private static final ConcurrentHashMap<UUID, Integer> CASTING_PLAYERS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Integer> SPELL_COOLDOWNS = new ConcurrentHashMap<>();
    private static final LinkedHashSet<String> SELECTED_WHEEL_SPELLS = new LinkedHashSet<>();
    private static boolean wheelSelectionLoaded = false;
    private static boolean wheelSelectionFileExists = false;
    private static String wheelSelectionStorageKey = "";
    private static String wheelAvailabilityStorageKey = "";
    private static Set<String> availableWheelSpells = Set.of();
    private static ModAttachments.CultivationData authoritativeWheelData;
    private static String authoritativeWheelStorageKey = "";
    private static float shieldAmount = 0.0F;
    private static float shieldMax = 0.0F;
    private static List<ShieldDisplay.Layer> shieldLayers=List.of();
    private static boolean yuqiControlAllMode = false;
    private static boolean yinYangCompassQiMode = false;
    private static boolean autoChannelWeaponQi = false;
    private static boolean outwardQiRelease = false;
    private static boolean outwardQiBlockBreaking = true;
    private static int blockBreakingDisabledMask;
    private static boolean outwardQiAutoFit = true;
    private static boolean jindanCreativeFlight = false;
    private static boolean jindanFlightControlActive = false;
    private static boolean combatStateEnabled = true;
    private static boolean autoCombatOnHit = true;
    private static boolean chinesePoemDescriptions = false;
    private static boolean formationPlateQiFirst = false;
    private static boolean showQiUseParticles = true;
    private static boolean flyingSwordAutoRecall = true;
    private static boolean showSpiritualSenseQiCurrents = true;
    private static boolean droughtZombieDomainEnabled = true;
    private static boolean clientTogglesLoaded = false;
    private static String preparedSpellId = "";
    private static String preparedSpellStorageKey = "";
    private static String legacyPreparedSpellId = "";
    private static float preparedChargeScale = 1.0F;
    private static boolean preparedCharging = false;
    private static boolean debugEyeEnabled = false;
    private static float swordCaseChargeProgress = 0.0F;
    private static int restrictionHints = 0;
    private static boolean combatActive = false;
    private static float posture = 0.0F;
    private static float maxPosture = 0.0F;
    private static int combatRemainingTicks = 0;
    private static boolean postureBroken = false;
    private static float heavyAttackChargeProgress = 0.0F;
    private static String outerSystemType = "";
    private static boolean outerSystemEquipped = false;
    private static boolean livingCorpse = false;

    /** Clear transient server data without deleting this world's saved wheel preferences. */
    public static void resetSession() {
        cultivationData = ModAttachments.CultivationData.createDefault();
        playerIdentity = "wandering_cultivator";
        jindanQuality = "liedan";
        MEDITATING_PLAYERS.clear();
        CASTING_PLAYERS.clear();
        SPELL_COOLDOWNS.clear();
        synchronized (SELECTED_WHEEL_SPELLS) {
            SELECTED_WHEEL_SPELLS.clear();
            wheelSelectionLoaded = false;
            wheelSelectionFileExists = false;
            wheelSelectionStorageKey = "";
            wheelAvailabilityStorageKey = "";
            availableWheelSpells = Set.of();
            authoritativeWheelData = null;
            authoritativeWheelStorageKey = "";
        }
        preparedSpellId = "";
        preparedSpellStorageKey = "";
        resetPreparedCharge();
        resetSwordCaseCharge();
        updateShield(0, 0);
        updateCombatState(false, 0, 0, 0, false);
        updateOuterSystem("", false);
        setLivingCorpse(false);
        setDebugEyeEnabled(false);
        setRestrictionHints(0);
        jindanFlightControlActive = false;
    }

    public static boolean livingCorpse() {
        return livingCorpse;
    }

    public static void setLivingCorpse(boolean value) {
        livingCorpse = value;
    }

    public static boolean isInnateSpellKnown(String spellId) {
        if (ModSpells.isInnateKnown(spellId, cultivationData)) {
            return true;
        }
        var player = Minecraft.getInstance().player;
        long grudge = player == null ? 0L : Math.max(0L,
                Math.round(player.getAttributeValue(ModAttributes.GRUDGE)));
        return UndeadAbilitySystem.canPlayerUseAbility(livingCorpse, grudge, spellId);
    }

    public static void updateOuterSystem(String type, boolean equipped) {
        outerSystemType = type == null ? "" : type;
        outerSystemEquipped = equipped;
    }

    public static String outerSystemType() {
        return outerSystemType;
    }

    public static boolean outerSystemEquipped() {
        return outerSystemEquipped;
    }

    public static boolean debugEyeEnabled() {
        return debugEyeEnabled;
    }

    public static int restrictionHints() {
        return restrictionHints;
    }

    public static void setRestrictionHints(int hints) {
        restrictionHints = Math.max(0, hints);
    }

    public static void updateSwordCaseCharge(int ticks, int maxTicks) {
        swordCaseChargeProgress = maxTicks <= 0 ? 0.0F
                : Math.max(0.0F, Math.min(1.0F, ticks / (float) maxTicks));
    }

    public static void resetSwordCaseCharge() {
        swordCaseChargeProgress = 0.0F;
    }

    public static float swordCaseChargeProgress() {
        return swordCaseChargeProgress;
    }

    public static void setDebugEyeEnabled(boolean enabled) {
        debugEyeEnabled = enabled;
        if (!enabled) DebugEyeClient.clear();
    }

    public static void updateMeditationState(UUID playerId, boolean meditating) {
        if (meditating) {
            MEDITATING_PLAYERS.add(playerId);
        } else {
            MEDITATING_PLAYERS.remove(playerId);
        }
    }

    public static boolean isPlayerMeditating(UUID playerId) {
        return MEDITATING_PLAYERS.contains(playerId);
    }

    public static void beginCasting(UUID playerId) {
        CASTING_PLAYERS.put(playerId, 18);
    }

    public static boolean isPlayerCasting(UUID playerId) {
        Integer ticks = CASTING_PLAYERS.get(playerId);
        return ticks != null && ticks > 0;
    }

    public static void tickClientAnimations() {
        CASTING_PLAYERS.replaceAll((id, ticks) -> ticks - 1);
        CASTING_PLAYERS.entrySet().removeIf(entry -> entry.getValue() <= 0);
        SPELL_COOLDOWNS.replaceAll((id, ticks) -> ticks - 1);
        SPELL_COOLDOWNS.entrySet().removeIf(entry -> entry.getValue() <= 0);
        if (combatRemainingTicks > 0) {
            combatRemainingTicks--;
        }
    }

    public static void updateCombatState(
            boolean active, float currentPosture, float maximumPosture,
            int remainingTicks, boolean broken) {
        combatActive = active;
        posture = Math.max(0.0F, currentPosture);
        maxPosture = Math.max(0.0F, maximumPosture);
        combatRemainingTicks = Math.max(0, remainingTicks);
        postureBroken = broken;
        if (!active) {
            resetHeavyAttackCharge();
        }
    }

    public static boolean combatActive() {
        return combatActive;
    }

    public static float posture() {
        return posture;
    }

    public static float maxPosture() {
        return maxPosture;
    }

    public static int combatRemainingTicks() {
        return combatRemainingTicks;
    }

    public static boolean postureBroken() {
        return postureBroken && cultivationData.thoughts() <= 0;
    }

    public static void updateHeavyAttackCharge(int ticks, int maximumTicks) {
        heavyAttackChargeProgress = maximumTicks <= 0
                ? 0.0F
                : Math.max(0.0F, Math.min(1.0F, ticks / (float) maximumTicks));
    }

    public static void resetHeavyAttackCharge() {
        heavyAttackChargeProgress = 0.0F;
    }

    public static float heavyAttackChargeProgress() {
        return heavyAttackChargeProgress;
    }

    public static void startSpellCooldown(String spellId, int ticks) {
        String normalized = normalizeWheelId(spellId);
        if (!isValidWheelId(normalized) || ticks <= 0) {
            return;
        }
        SPELL_COOLDOWNS.put(normalized, ticks);
    }

    public static int spellCooldownTicks(String spellId) {
        String normalized = normalizeWheelId(spellId);
        return Math.max(0, SPELL_COOLDOWNS.getOrDefault(normalized, 0));
    }

    public static String preparedSpellId() {
        ensurePreparedSpellLoaded();
        if (preparedSpellId == null || preparedSpellId.isBlank() || !isValidWheelId(preparedSpellId)
                || !cultivationData.knownSpells().stream().map(ModSpells::normalizeId).anyMatch(preparedSpellId::equals)
                && !isInnateSpellKnown(preparedSpellId)) {
            return "";
        }
        return preparedSpellId;
    }

    public static void setPreparedSpellId(String spellId) {
        if (!ClientWorldSpellStorage.ready()) return;
        ensurePreparedSpellLoaded();
        String normalized = normalizeWheelId(spellId);
        preparedSpellId = isValidWheelId(normalized) ? normalized : "";
        savePreparedSpell();
    }

    public static Component preparedSpellName() {
        String spellId = preparedSpellId();
        if (spellId.isEmpty()) {
            return Component.empty();
        }
        ModSpells.SpellDef spell = ModSpells.get(spellId);
        return spell == null ? Component.empty() : spell.name();
    }

    public static int preparedSpellCooldownTicks() {
        String spellId = preparedSpellId();
        return spellCooldownTicks(spellId);
    }

    public static void updatePreparedCharge(float chargeScale, boolean charging) {
        preparedChargeScale = Math.max(1.0F, Math.min(2.0F, chargeScale));
        preparedCharging = charging;
    }

    public static void resetPreparedCharge() {
        updatePreparedCharge(1.0F, false);
    }

    public static float preparedChargeScale() {
        return preparedChargeScale;
    }

    public static int preparedChargePercent() {
        return Math.round(preparedChargeScale * 100.0F);
    }

    public static boolean preparedCharging() {
        return preparedCharging;
    }

    public static void updateShield(float amount, float max) {
        updateShield(amount,max,List.of());
    }

    public static void updateShield(float amount,float max,List<ShieldDisplay.Layer> layers) {
        shieldLayers=List.copyOf(layers);
        if(!shieldLayers.isEmpty()){amount=ShieldDisplay.total(shieldLayers,false);max=ShieldDisplay.total(shieldLayers,true);}
        if(!Float.isFinite(amount)||!Float.isFinite(max)){amount=0;max=0;}
        shieldAmount = Math.max(0.0F, amount);
        shieldMax = Math.max(0.0F, max);
        if (shieldAmount <= 0.0F || shieldMax <= 0.0F) {
            shieldAmount = 0.0F;
            shieldMax = 0.0F;
            shieldLayers=List.of();
        }
    }

    public static List<ShieldDisplay.Layer> shieldLayers(){return shieldLayers;}

    public static float shieldAmount() {
        return shieldAmount;
    }

    public static float shieldMax() {
        return shieldMax;
    }

    public static boolean yuqiControlAllMode() {
        ensureClientTogglesLoaded();
        return yuqiControlAllMode;
    }

    public static boolean toggleYuqiControlAllMode() {
        ensureClientTogglesLoaded();
        yuqiControlAllMode = !yuqiControlAllMode;
        saveClientToggles();
        return yuqiControlAllMode;
    }

    public static void setYuqiControlAllMode(boolean allMode) {
        ensureClientTogglesLoaded();
        yuqiControlAllMode = allMode;
        saveClientToggles();
    }

    public static boolean yinYangCompassQiMode() {
        ensureClientTogglesLoaded();
        return yinYangCompassQiMode;
    }

    public static void setYinYangCompassQiMode(boolean qiMode) {
        ensureClientTogglesLoaded();
        yinYangCompassQiMode = qiMode;
        saveClientToggles();
    }

    public static boolean autoChannelWeaponQi() {
        ensureClientTogglesLoaded();
        return autoChannelWeaponQi;
    }

    public static void setAutoChannelWeaponQi(boolean enabled) {
        ensureClientTogglesLoaded();
        autoChannelWeaponQi = enabled;
        saveClientToggles();
    }

    public static boolean outwardQiRelease() {
        ensureClientTogglesLoaded();
        return outwardQiRelease;
    }

    public static void setOutwardQiRelease(boolean enabled) {
        ensureClientTogglesLoaded();
        outwardQiRelease = enabled;
        saveClientToggles();
    }

    public static boolean outwardQiBlockBreaking() {
        return blockBreaking(BlockBreakingSettings.Category.EXTERNAL_QI);
    }

    public static void setOutwardQiBlockBreaking(boolean enabled) {
        setBlockBreaking(BlockBreakingSettings.Category.EXTERNAL_QI, enabled);
    }

    /** Stored category choice; the master switch does not erase individual choices. */
    public static boolean blockBreaking(BlockBreakingSettings.Category category) {
        ensureClientTogglesLoaded();
        return (blockBreakingDisabledMask & category.bit()) == 0;
    }

    public static void setBlockBreaking(BlockBreakingSettings.Category category, boolean enabled) {
        ensureClientTogglesLoaded();
        if (enabled) blockBreakingDisabledMask &= ~category.bit();
        else blockBreakingDisabledMask |= category.bit();
        blockBreakingDisabledMask &= BlockBreakingSettings.validMask();
        outwardQiBlockBreaking = (blockBreakingDisabledMask
                & BlockBreakingSettings.Category.EXTERNAL_QI.bit()) == 0;
        saveClientToggles();
    }

    public static int blockBreakingDisabledMask() {
        ensureClientTogglesLoaded();
        return blockBreakingDisabledMask;
    }

    public static boolean outwardQiAutoFit() {
        ensureClientTogglesLoaded();
        return outwardQiAutoFit;
    }

    public static void setOutwardQiAutoFit(boolean enabled) {
        ensureClientTogglesLoaded();
        outwardQiAutoFit = enabled;
        saveClientToggles();
    }

    public static boolean jindanCreativeFlight() {
        ensureClientTogglesLoaded();
        return jindanCreativeFlight;
    }

    public static void setJindanCreativeFlight(boolean enabled) {
        ensureClientTogglesLoaded();
        jindanCreativeFlight = enabled;
        saveClientToggles();
    }

    public static boolean jindanFlightControlActive() {
        return jindanFlightControlActive;
    }

    public static void setJindanFlightControlActive(boolean active) {
        jindanFlightControlActive = active;
    }

    public static boolean combatStateEnabled() {
        ensureClientTogglesLoaded();
        return combatStateEnabled;
    }

    public static void setCombatStateEnabled(boolean enabled) {
        ensureClientTogglesLoaded();
        combatStateEnabled = enabled;
        saveClientToggles();
    }

    public static boolean autoCombatOnHit() {
        ensureClientTogglesLoaded();
        return autoCombatOnHit;
    }

    public static void setAutoCombatOnHit(boolean enabled) {
        ensureClientTogglesLoaded();
        autoCombatOnHit = enabled;
        saveClientToggles();
    }

    public static boolean chinesePoemDescriptions() {
        ensureClientTogglesLoaded();
        return chinesePoemDescriptions;
    }

    public static void setChinesePoemDescriptions(boolean enabled) {
        ensureClientTogglesLoaded();
        chinesePoemDescriptions = enabled;
        saveClientToggles();
    }

    public static boolean formationPlateQiFirst() {
        ensureClientTogglesLoaded();
        return formationPlateQiFirst;
    }

    public static void setFormationPlateQiFirst(boolean qiFirst) {
        ensureClientTogglesLoaded();
        formationPlateQiFirst = qiFirst;
        saveClientToggles();
    }

    public static boolean showQiUseParticles() {
        ensureClientTogglesLoaded();
        return showQiUseParticles;
    }
    public static boolean flyingSwordAutoRecall(){ensureClientTogglesLoaded();return flyingSwordAutoRecall;}
    public static void setFlyingSwordAutoRecall(boolean enabled){ensureClientTogglesLoaded();flyingSwordAutoRecall=enabled;saveClientToggles();}

    public static void setShowQiUseParticles(boolean enabled) {
        ensureClientTogglesLoaded();
        showQiUseParticles = enabled;
        saveClientToggles();
    }

    public static boolean showSpiritualSenseQiCurrents() {
        ensureClientTogglesLoaded();
        return showSpiritualSenseQiCurrents;
    }

    public static void setShowSpiritualSenseQiCurrents(boolean enabled) {
        ensureClientTogglesLoaded();
        showSpiritualSenseQiCurrents = enabled;
        saveClientToggles();
    }

    public static boolean droughtZombieDomainEnabled() {
        ensureClientTogglesLoaded();
        return droughtZombieDomainEnabled;
    }

    public static void setDroughtZombieDomainEnabled(boolean enabled) {
        ensureClientTogglesLoaded();
        droughtZombieDomainEnabled = enabled;
        saveClientToggles();
    }

    public static boolean isSpellVisibleInWheel(String spellId) {
        ensureWheelSelectionLoaded();
        synchronized (SELECTED_WHEEL_SPELLS) {
            return SELECTED_WHEEL_SPELLS.contains(normalizeWheelId(spellId));
        }
    }

    public static void toggleSpellVisibleInWheel(String spellId) {
        if (!ClientWorldSpellStorage.ready()) return;
        ensureWheelSelectionLoaded();
        String normalized = normalizeWheelId(spellId);
        if (!isValidWheelId(normalized)) {
            return;
        }
        synchronized (SELECTED_WHEEL_SPELLS) {
            if (wheelSelectionStorageKey.equals(wheelAvailabilityStorageKey)
                    && !availableWheelSpells.contains(normalized)) return;
            if (!SELECTED_WHEEL_SPELLS.remove(normalized) && SELECTED_WHEEL_SPELLS.size() < MAX_WHEEL_SPELLS) {
                SELECTED_WHEEL_SPELLS.add(normalized);
            }
            saveWheelSelection();
        }
    }

    public static int selectedWheelSpellCount() {
        ensureWheelSelectionLoaded();
        synchronized (SELECTED_WHEEL_SPELLS) {
            return SELECTED_WHEEL_SPELLS.size();
        }
    }

    public static List<String> selectedWheelSpellIds() {
        ensureWheelSelectionLoaded();
        synchronized (SELECTED_WHEEL_SPELLS) {
            return List.copyOf(SELECTED_WHEEL_SPELLS);
        }
    }

    public static int selectedWheelSpellIndex(String spellId) {
        ensureWheelSelectionLoaded();
        String normalized = normalizeWheelId(spellId);
        synchronized (SELECTED_WHEEL_SPELLS) {
            int index = 0;
            for (String selected : SELECTED_WHEEL_SPELLS) {
                if (selected.equals(normalized)) {
                    return index;
                }
                index++;
            }
        }
        return -1;
    }

    public static boolean canMoveWheelSpell(String spellId, int offset) {
        int index = selectedWheelSpellIndex(spellId);
        if (index < 0) {
            return false;
        }
        int target = index + offset;
        return target >= 0 && target < selectedWheelSpellCount();
    }

    public static void moveWheelSpell(String spellId, int offset) {
        if (!ClientWorldSpellStorage.ready()) return;
        ensureWheelSelectionLoaded();
        String normalized = normalizeWheelId(spellId);
        if (offset == 0) {
            return;
        }
        synchronized (SELECTED_WHEEL_SPELLS) {
            ArrayList<String> ordered = new ArrayList<>(SELECTED_WHEEL_SPELLS);
            int index = ordered.indexOf(normalized);
            if (index < 0) {
                return;
            }
            int target = Math.max(0, Math.min(ordered.size() - 1, index + offset));
            if (target == index) {
                return;
            }
            ordered.remove(index);
            ordered.add(target, normalized);
            SELECTED_WHEEL_SPELLS.clear();
            SELECTED_WHEEL_SPELLS.addAll(ordered);
            saveWheelSelection();
        }
    }

    /** Only a local-player server sync can authorize pruning saved preferences. */
    public static void reconcileWheelSelectionFromServer() {
        if (!ClientWorldSpellStorage.ready()) return;
        authoritativeWheelData = cultivationData;
        authoritativeWheelStorageKey = ClientWorldSpellStorage.storageKey();
        reconcileWheelSelection(cultivationData.knownSpells());
    }

    public static void reconcileWheelSelection(List<String> knownSpells) {
        // Login defaults and another player's debug preview are not a spell-forgetting event.
        if (!ClientWorldSpellStorage.ready() || cultivationData != authoritativeWheelData
                || !ClientWorldSpellStorage.storageKey().equals(authoritativeWheelStorageKey)) return;
        ensureWheelSelectionLoaded();
        synchronized (SELECTED_WHEEL_SPELLS) {
            Set<String> known = ConcurrentHashMap.newKeySet();
            for (String spell : knownSpells) {
                known.add(ModSpells.normalizeId(spell));
            }
            for (String spellId : ModSpells.innateSpellIds()) {
                if (isInnateSpellKnown(spellId)) {
                    known.add(spellId);
                }
            }
            known.removeIf(spell -> !isValidWheelId(spell));
            // Undead slots deliberately survive temporary grudge loss; method grants do not.
            var retained = new LinkedHashSet<>(known);
            retained.addAll(UndeadAbilitySystem.playerAbilityIds());
            availableWheelSpells = Set.copyOf(retained);
            wheelAvailabilityStorageKey = wheelSelectionStorageKey;
            boolean changed = SELECTED_WHEEL_SPELLS.removeIf(spell -> !availableWheelSpells.contains(spell));
            if (!wheelSelectionFileExists && SELECTED_WHEEL_SPELLS.isEmpty()) {
                for (String spell : knownSpells) {
                    if (SELECTED_WHEEL_SPELLS.size() >= MAX_WHEEL_SPELLS) {
                        break;
                    }
                    String normalized = ModSpells.normalizeId(spell);
                    if (known.contains(normalized)) {
                        changed |= SELECTED_WHEEL_SPELLS.add(normalized);
                    }
                }
                for (String spellId : ModSpells.innateSpellIds()) {
                    if (SELECTED_WHEEL_SPELLS.size() < MAX_WHEEL_SPELLS && known.contains(spellId)) {
                        changed |= SELECTED_WHEEL_SPELLS.add(spellId);
                    }
                }
            }
            if (changed) saveWheelSelection();
            ensurePreparedSpellLoaded();
            if (!preparedSpellId.isEmpty() && !availableWheelSpells.contains(preparedSpellId)) {
                preparedSpellId = "";
                resetPreparedCharge();
                savePreparedSpell();
            }
        }
    }

    private static void ensureWheelSelectionLoaded() {
        if (!ClientWorldSpellStorage.ready()) return;
        String storageKey = ClientWorldSpellStorage.storageKey();
        if (wheelSelectionLoaded && storageKey.equals(wheelSelectionStorageKey)) {
            return;
        }
        synchronized (SELECTED_WHEEL_SPELLS) {
            if (wheelSelectionLoaded && storageKey.equals(wheelSelectionStorageKey)) {
                return;
            }
            SELECTED_WHEEL_SPELLS.clear();
            wheelSelectionStorageKey = storageKey;
            Path path = wheelSelectionPath();
            Path sourcePath = path;
            boolean migrateLegacy = false;
            if (!Files.exists(sourcePath)) {
                Path legacyPath = Minecraft.getInstance().gameDirectory.toPath()
                        .resolve("config")
                        .resolve(ImmortalCultivationMod.MODID + "_spell_wheel.txt");
                if (Files.exists(legacyPath)) {
                    sourcePath = legacyPath;
                    migrateLegacy = true;
                }
            }
            wheelSelectionFileExists = Files.exists(sourcePath);
            if (wheelSelectionFileExists) {
                try {
                    for (String line : Files.readAllLines(sourcePath, StandardCharsets.UTF_8)) {
                        String normalized = normalizeWheelId(line.trim());
                        if (!normalized.isEmpty() && isValidWheelId(normalized) && SELECTED_WHEEL_SPELLS.size() < MAX_WHEEL_SPELLS) {
                            SELECTED_WHEEL_SPELLS.add(normalized);
                        }
                    }
                } catch (IOException ignored) {
                    SELECTED_WHEEL_SPELLS.clear();
                }
            }
            wheelSelectionLoaded = true;
            // A reload must not resurrect slots discarded by the latest authoritative data.
            if (storageKey.equals(wheelAvailabilityStorageKey)
                    && SELECTED_WHEEL_SPELLS.removeIf(spell -> !availableWheelSpells.contains(spell))) {
                saveWheelSelection();
            }
            if (migrateLegacy) {
                saveWheelSelection();
            }
        }
    }

    private static void saveWheelSelection() {
        if (!ClientWorldSpellStorage.ready()) return;
        Path path = wheelSelectionPath();
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, SELECTED_WHEEL_SPELLS, StandardCharsets.UTF_8);
            wheelSelectionFileExists = true;
        } catch (IOException ignored) {
        }
    }

    private static Path wheelSelectionPath() {
        return ClientWorldSpellStorage.directory().resolve("spell_wheel.txt");
    }

    private static void ensurePreparedSpellLoaded() {
        if (!ClientWorldSpellStorage.ready()) return;
        ensureClientTogglesLoaded();
        String storageKey = ClientWorldSpellStorage.storageKey();
        if (storageKey.equals(preparedSpellStorageKey)) {
            return;
        }
        preparedSpellStorageKey = storageKey;
        preparedSpellId = "";
        Path path = ClientWorldSpellStorage.directory().resolve("prepared_spell.txt");
        if (!Files.exists(path)) {
            String legacy = normalizeWheelId(legacyPreparedSpellId);
            if (isValidWheelId(legacy)) {
                preparedSpellId = legacy;
                savePreparedSpell();
            }
            return;
        }
        try {
            String saved = normalizeWheelId(Files.readString(path, StandardCharsets.UTF_8).trim());
            preparedSpellId = isValidWheelId(saved) ? saved : "";
        } catch (IOException ignored) {
            preparedSpellId = "";
        }
    }

    private static void savePreparedSpell() {
        if (!ClientWorldSpellStorage.ready()) return;
        Path path = ClientWorldSpellStorage.directory().resolve("prepared_spell.txt");
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, preparedSpellId == null ? "" : preparedSpellId, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    private static void ensureClientTogglesLoaded() {
        if (clientTogglesLoaded) {
            return;
        }
        Path path = clientTogglesPath();
        if (Files.exists(path)) {
            Properties properties = new Properties();
            try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                properties.load(reader);
                yuqiControlAllMode = Boolean.parseBoolean(properties.getProperty("yuqiControlAllMode", "false"));
                yinYangCompassQiMode = Boolean.parseBoolean(properties.getProperty("yinYangCompassQiMode", "false"));
                autoChannelWeaponQi = Boolean.parseBoolean(properties.getProperty("autoChannelWeaponQi", "false"));
                outwardQiRelease = Boolean.parseBoolean(
                        properties.getProperty("outwardQiRelease", "false"));
                outwardQiAutoFit = Boolean.parseBoolean(properties.getProperty("outwardQiAutoFit", "true"));
                outwardQiBlockBreaking = Boolean.parseBoolean(
                        properties.getProperty("outwardQiBlockBreaking", "true"));
                blockBreakingDisabledMask = outwardQiBlockBreaking ? 0
                        : BlockBreakingSettings.Category.EXTERNAL_QI.bit();
                try {
                    blockBreakingDisabledMask = Integer.parseInt(properties.getProperty(
                            "blockBreakingDisabledMask", Integer.toString(blockBreakingDisabledMask)))
                            & BlockBreakingSettings.validMask();
                } catch (NumberFormatException ignored) {
                    // Preserve the older External Qi preference if the new value is invalid.
                }
                outwardQiBlockBreaking = (blockBreakingDisabledMask
                        & BlockBreakingSettings.Category.EXTERNAL_QI.bit()) == 0;
                jindanCreativeFlight = Boolean.parseBoolean(
                        properties.getProperty("jindanCreativeFlight", "false"));
                combatStateEnabled = Boolean.parseBoolean(
                        properties.getProperty("combatStateEnabled", "true"));
                autoCombatOnHit = Boolean.parseBoolean(properties.getProperty("autoCombatOnHit", "true"));
                chinesePoemDescriptions = Boolean.parseBoolean(
                        properties.getProperty("chinesePoemDescriptions", "false"));
                formationPlateQiFirst = Boolean.parseBoolean(
                        properties.getProperty("formationPlateQiFirst", "false"));
                showQiUseParticles = Boolean.parseBoolean(
                        properties.getProperty("showQiUseParticles", "true"));
                flyingSwordAutoRecall=Boolean.parseBoolean(properties.getProperty("flyingSwordAutoRecall","true"));
                showSpiritualSenseQiCurrents = Boolean.parseBoolean(
                        properties.getProperty(
                                "showSpiritualSenseQiCurrents", "true"));
                droughtZombieDomainEnabled = Boolean.parseBoolean(
                        properties.getProperty("droughtZombieDomainEnabled", "true"));
                legacyPreparedSpellId = properties.getProperty("preparedSpellId", "");
            } catch (IOException ignored) {
            }
        }
        clientTogglesLoaded = true;
    }

    private static void saveClientToggles() {
        Path path = clientTogglesPath();
        Properties properties = new Properties();
        properties.setProperty("yuqiControlAllMode", Boolean.toString(yuqiControlAllMode));
        properties.setProperty("yinYangCompassQiMode", Boolean.toString(yinYangCompassQiMode));
        properties.setProperty("autoChannelWeaponQi", Boolean.toString(autoChannelWeaponQi));
        properties.setProperty("outwardQiAutoFit", Boolean.toString(outwardQiAutoFit));
        properties.setProperty("outwardQiRelease", Boolean.toString(outwardQiRelease));
        properties.setProperty("outwardQiBlockBreaking", Boolean.toString(outwardQiBlockBreaking));
        properties.setProperty("blockBreakingDisabledMask", Integer.toString(blockBreakingDisabledMask));
        properties.setProperty("jindanCreativeFlight", Boolean.toString(jindanCreativeFlight));
        properties.setProperty("combatStateEnabled", Boolean.toString(combatStateEnabled));
        properties.setProperty("autoCombatOnHit", Boolean.toString(autoCombatOnHit));
        properties.setProperty("chinesePoemDescriptions", Boolean.toString(chinesePoemDescriptions));
        properties.setProperty("formationPlateQiFirst", Boolean.toString(formationPlateQiFirst));
        properties.setProperty("showQiUseParticles", Boolean.toString(showQiUseParticles));
        properties.setProperty("flyingSwordAutoRecall",Boolean.toString(flyingSwordAutoRecall));
        properties.setProperty("showSpiritualSenseQiCurrents",
                Boolean.toString(showSpiritualSenseQiCurrents));
        properties.setProperty("droughtZombieDomainEnabled",
                Boolean.toString(droughtZombieDomainEnabled));
        try {
            Files.createDirectories(path.getParent());
            try (var writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                properties.store(writer, "Immortal Cultivation client toggles");
            }
        } catch (IOException ignored) {
        }
    }

    private static Path clientTogglesPath() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config")
                .resolve(ImmortalCultivationMod.MODID + "_client_toggles.properties");
    }

    private static String normalizeWheelId(String id) {
        return ModSpells.normalizeId(id);
    }

    private static boolean isValidWheelId(String id) {
        return ModSpells.get(id) != null;
    }
}
