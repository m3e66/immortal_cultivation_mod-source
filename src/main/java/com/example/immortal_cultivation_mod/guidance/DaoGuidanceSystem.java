package com.example.immortal_cultivation_mod.guidance;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.combat.CombatStateManager;
import com.example.immortal_cultivation_mod.entity.CultivationNpc;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.network.DaoGuidancePayloads;
import com.example.immortal_cultivation_mod.screen.DingMenu;
import com.example.immortal_cultivation_mod.screen.QiPouchMenu;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import com.example.immortal_cultivation_mod.spell.JindanCreativeFlight;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import top.theillusivec4.curios.api.CuriosApi;

import static com.example.immortal_cultivation_mod.guidance.DaoGuidanceContent.*;

/**
 * Persistent 引道 progression.  Objectives move only when their real gameplay
 * action is observed on the server; opening the guide never completes them.
 */
@EventBusSubscriber(modid = ImmortalCultivationMod.MODID)
public final class DaoGuidanceSystem {
    public static final String ROOT_TAG = "ImmortalCultivationDaoGuidance";
    private static final String INITIALIZED = "Initialized";
    private static final String DISCOVERED = "Discovered";
    private static final String COMPLETED = "Completed";
    private static final String OBJECTIVE_INDEX = "ObjectiveIndex";
    private static final String PROGRESS = "Progress";
    private static final String REWARDS = "Rewards";
    private static final String TRACKED = "Tracked";
    private static final String TRACKING_ENABLED = "TrackingEnabled";
    private static final String CHAPTER_ZERO_MIGRATED =
            "ChapterZeroMigrated";
    private static final String CAVE_TARGET_DIMENSION =
            "CaveTargetDimension";
    private static final String CAVE_TARGET_X = "CaveTargetX";
    private static final String CAVE_TARGET_Z = "CaveTargetZ";
    private static final String CAVE_SEARCH_AFTER = "CaveSearchAfter";
    private static final String CAVE_SEARCH_ATTEMPT = "CaveSearchAttempt";
    private static final String CAVE_SEARCH_ORIGIN_X = "CaveSearchOriginX";
    private static final String CAVE_SEARCH_ORIGIN_Z = "CaveSearchOriginZ";
    private static final int CAVE_SEARCH_RADIUS_CHUNKS = 256;
    private static final int CAVE_SEARCH_STRIDE_BLOCKS =
            CAVE_SEARCH_RADIUS_CHUNKS * 32;
    private static final long CAVE_SEARCH_RETRY_TICKS = 200L;
    private static final TagKey<Structure> TUTORIAL_CAVES = TagKey.create(
            Registries.STRUCTURE,
            ResourceLocation.fromNamespaceAndPath(
                    ImmortalCultivationMod.MODID, "tutorial_cave"));
    private static final ResourceLocation PATCHOULI_GUIDE_ITEM_ID =
            ResourceLocation.fromNamespaceAndPath("patchouli", "guide_book");
    private static final ResourceLocation PATCHOULI_BOOK_COMPONENT_ID =
            ResourceLocation.fromNamespaceAndPath("patchouli", "book");
    private static final ResourceLocation MAGIC_RITUAL_GUIDE_ID =
            ResourceLocation.fromNamespaceAndPath(
                    ImmortalCultivationMod.MODID, "guide_book");

    private static final Map<UUID, RuntimeState> RUNTIME = new HashMap<>();
    private static final Map<String, Integer> ACTION_TARGETS = DaoGuidanceContent.chapters().stream()
            .flatMap(chapter -> chapter.objectives().stream())
            .filter(objective -> objective.action().startsWith("guide_"))
            .collect(java.util.stream.Collectors.toUnmodifiableMap(
                    DaoGuidanceContent.Objective::action, DaoGuidanceContent.Objective::target, Math::max));
    private static final Map<UUID, Map<String, Long>> HINT_COOLDOWNS =
            new HashMap<>();
    private static final List<String> EQUIPMENT_CURIO_SLOTS = List.of(
            "hair", "hair_accessory", "head_accessory",
            "neck_accessory", "inner_clothes", "outer_clothes",
            "cloak", "back_accessory", "waist_accessory", "ring");

    private DaoGuidanceSystem() {
    }

    public static void record(ServerPlayer player, String action) {
        record(player, action, 1);
    }

    public static void record(ServerPlayer player, String action, int amount) {
        if (player == null || action == null || action.isBlank() || amount <= 0) {
            return;
        }
        CompoundTag state = read(player);
        boolean changed = initialize(state);
        if (action.startsWith("guide_")) {
            int target = ACTION_TARGETS.getOrDefault(action, 0);
            if (target == 0) return;
            CompoundTag observations = state.getCompound("Observed").copy();
            if (observations.getInt(action) >= target) return;
            observations.putInt(action, Math.min(target, observations.getInt(action) + amount));
            state.put("Observed", observations);
            changed = true;
        }
        changed |= discoverForAction(player, state, action);
        boolean progressChanged = false;
        String completedChapter = "";
        String unlockedChapter = "";

        CompoundTag discovered = state.getCompound(DISCOVERED).copy();
        CompoundTag completed = state.getCompound(COMPLETED).copy();
        CompoundTag indices = state.getCompound(OBJECTIVE_INDEX).copy();
        CompoundTag progress = state.getCompound(PROGRESS).copy();

        for (DaoGuidanceContent.Chapter chapter : DaoGuidanceContent.chapters()) {
            if (!discovered.getBoolean(chapter.id())
                    || completed.getBoolean(chapter.id())) {
                continue;
            }
            int index = Math.max(0, indices.getInt(chapter.id()));
            if (index >= chapter.objectives().size()) continue;
            DaoGuidanceContent.Objective objective = chapter.objectives().get(index);
            if (!objective.action().equals(action)) continue;

            int nextProgress = Math.min(objective.target(),
                    progress.getInt(chapter.id()) + amount);
            progress.putInt(chapter.id(), nextProgress);
            progressChanged = true;
            if (nextProgress < objective.target()) continue;

            index++;
            indices.putInt(chapter.id(), index);
            progress.putInt(chapter.id(), 0);
            if (index >= chapter.objectives().size()) {
                completed.putBoolean(chapter.id(), true);
                completedChapter = chapter.id();
                DaoGuidanceContent.Chapter next =
                        automaticNextChapter(chapter.id());
                if (next != null && !discovered.getBoolean(next.id())) {
                    discovered.putBoolean(next.id(), true);
                    unlockedChapter = next.id();
                    grantChapterStarter(player, state, next.id());
                }
                if (chapter.id().equals(state.getString(TRACKED))) {
                    state.putString(TRACKED,
                            next == null ? "" : next.id());
                }
            }
        }

        // A real action may have happened before its chapter became current
        // (for example taking the enlightenment pill before opening stats).
        // Reconcile only durable facts; transient actions such as spell casts
        // are never granted from menu state.
        ModAttachments.CultivationData currentData = ModAttachments.getData(player);
        for (DaoGuidanceContent.Chapter chapter : DaoGuidanceContent.chapters()) {
            if (!discovered.getBoolean(chapter.id())
                    || completed.getBoolean(chapter.id())) continue;
            int index = Math.max(0, indices.getInt(chapter.id()));
            while (index < chapter.objectives().size()
                    && (DaoGuidanceObservations.observed(state,
                    chapter.objectives().get(index).action(), chapter.objectives().get(index).target())
                    || durableFactSatisfied(
                    player, currentData,
                    chapter.objectives().get(index).action()))) {
                index++;
                indices.putInt(chapter.id(), index);
                progress.putInt(chapter.id(), 0);
                progressChanged = true;
            }
            if (index < chapter.objectives().size()) continue;
            completed.putBoolean(chapter.id(), true);
            completedChapter = chapter.id();
            DaoGuidanceContent.Chapter next =
                    automaticNextChapter(chapter.id());
            if (next != null && !discovered.getBoolean(next.id())) {
                discovered.putBoolean(next.id(), true);
                unlockedChapter = next.id();
                grantChapterStarter(player, state, next.id());
            }
            if (chapter.id().equals(state.getString(TRACKED))) {
                state.putString(TRACKED, next == null ? "" : next.id());
            }
        }

        if (progressChanged) {
            state.put(OBJECTIVE_INDEX, indices);
            state.put(PROGRESS, progress);
            state.put(COMPLETED, completed);
            state.put(DISCOVERED, discovered);
            changed = true;
        }
        if (!changed) return;
        write(player, state);
        sync(player);
        DaoGuidanceObservations.notice(player, action);
        if (!completedChapter.isBlank()
                && state.getBoolean(TRACKING_ENABLED)) {
            PacketDistributor.sendToPlayer(player,
                    new DaoGuidancePayloads.ClientboundDaoGuidanceNoticePayload(
                            "complete", completedChapter, unlockedChapter));
        }
    }

    public static void discover(ServerPlayer player, String chapterId) {
        if (DaoGuidanceContent.chapter(chapterId) == null) return;
        CompoundTag state = read(player);
        boolean changed = initialize(state);
        if (markDiscovered(state, chapterId)) {
            changed = true;
            grantChapterStarter(player, state, chapterId);
            if (state.getBoolean(TRACKING_ENABLED)) {
                PacketDistributor.sendToPlayer(player,
                        new DaoGuidancePayloads.ClientboundDaoGuidanceNoticePayload(
                                "unlock", chapterId, ""));
            }
        }
        if (changed) {
            write(player, state);
            sync(player);
        }
    }

    public static void track(ServerPlayer player, String chapterId) {
        CompoundTag state = read(player);
        boolean changed = initialize(state);
        if (chapterId.isBlank()) {
            if (state.getBoolean(TRACKING_ENABLED)) {
                state.putBoolean(TRACKING_ENABLED, false);
                changed = true;
            }
            if (!state.getString(TRACKED).isBlank()) {
                state.putString(TRACKED, "");
                changed = true;
            }
        } else if (DaoGuidanceContent.chapter(chapterId) != null
                && state.getCompound(DISCOVERED).getBoolean(chapterId)
                && !state.getCompound(COMPLETED).getBoolean(chapterId)) {
            if (!state.getBoolean(TRACKING_ENABLED)) {
                state.putBoolean(TRACKING_ENABLED, true);
                changed = true;
            }
            if (!state.getString(TRACKED).equals(chapterId)) {
                state.putString(TRACKED, chapterId);
                changed = true;
            }
        }
        if (changed) {
            write(player, state);
            sync(player);
        }
    }

    public static boolean isCurrentObjective(
            ServerPlayer player, String action) {
        CompoundTag state = read(player);
        for (DaoGuidanceContent.Chapter chapter : DaoGuidanceContent.chapters()) {
            if (!state.getCompound(DISCOVERED).getBoolean(chapter.id())
                    || state.getCompound(COMPLETED).getBoolean(chapter.id())) {
                continue;
            }
            int index = state.getCompound(OBJECTIVE_INDEX).getInt(chapter.id());
            if (index >= 0 && index < chapter.objectives().size()
                    && chapter.objectives().get(index).action().equals(action)) {
                return true;
            }
        }
        return false;
    }

    public static void hint(ServerPlayer player, String hintId) {
        if (player == null || hintId == null || hintId.isBlank()) return;
        CompoundTag state = read(player);
        if (!state.getBoolean(TRACKING_ENABLED)) return;
        long now = player.serverLevel().getGameTime();
        Map<String, Long> cooldowns = HINT_COOLDOWNS.computeIfAbsent(
                player.getUUID(), ignored -> new HashMap<>());
        if (cooldowns.getOrDefault(hintId, 0L) > now) return;
        cooldowns.put(hintId, now + 200L);
        PacketDistributor.sendToPlayer(player,
                new DaoGuidancePayloads.ClientboundDaoGuidanceNoticePayload(
                        "hint", hintId, ""));
    }

    public static void notice(ServerPlayer player, String hintId, Object... arguments) {
        if (player == null || !read(player).getBoolean(TRACKING_ENABLED)) return;
        long now = player.serverLevel().getGameTime();
        Map<String, Long> cooldowns = HINT_COOLDOWNS.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>());
        if (cooldowns.getOrDefault(hintId, 0L) > now) return;
        cooldowns.put(hintId, now + 200L);
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                "guidance." + ImmortalCultivationMod.MODID + ".hint." + hintId, arguments), true);
    }

    public static void sync(ServerPlayer player) {
        CompoundTag state = read(player);
        initialize(state);
        List<DaoGuidancePayloads.ChapterState> chapters =
                DaoGuidanceContent.chapters().stream().map(chapter -> {
                    boolean discovered = state.getCompound(DISCOVERED)
                            .getBoolean(chapter.id());
                    boolean complete = state.getCompound(COMPLETED)
                            .getBoolean(chapter.id());
                    return new DaoGuidancePayloads.ChapterState(
                            chapter.id(), discovered, complete,
                            state.getCompound(OBJECTIVE_INDEX)
                                    .getInt(chapter.id()),
                            state.getCompound(PROGRESS).getInt(chapter.id()));
                }).toList();
        PacketDistributor.sendToPlayer(player,
                new DaoGuidancePayloads.ClientboundDaoGuidanceSyncPayload(
                        state.getString(TRACKED), chapters,
                        state.getString(CAVE_TARGET_DIMENSION),
                        state.getInt(CAVE_TARGET_X),
                        state.getInt(CAVE_TARGET_Z)));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        CompoundTag state = read(player);
        boolean firstJoin = !state.getBoolean(INITIALIZED);
        boolean changed = initialize(state);
        changed |= migrateChapterZero(player, state);
        changed |= ensureCaveTarget(player, state);
        changed |= refreshAvailability(player, state, false);
        write(player, state);
        RUNTIME.put(player.getUUID(), RuntimeState.capture(player));
        sync(player);
        if (firstJoin) {
            PacketDistributor.sendToPlayer(player,
                    new DaoGuidancePayloads.ClientboundDaoGuidanceNoticePayload(
                            "unlock", ENLIGHTENMENT, ""));
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (!event.getOriginal().getPersistentData().contains(ROOT_TAG)) return;
        event.getEntity().getPersistentData().put(ROOT_TAG,
                event.getOriginal().getPersistentData()
                        .getCompound(ROOT_TAG).copy());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        RUNTIME.remove(id);
        HINT_COOLDOWNS.remove(id);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        RuntimeState runtime = RUNTIME.computeIfAbsent(
                player.getUUID(), ignored -> RuntimeState.capture(player));
        ModAttachments.CultivationData data = ModAttachments.getData(player);

        if (data.isMeditating() && data.qi() > runtime.qi) {
            record(player, MEDITATE_QI, data.qi() - runtime.qi);
        }
        if (!data.cultivationLevel().equals(runtime.level)) {
            int oldStage = CultivationLevels.getStageIndex(runtime.level);
            int newStage = CultivationLevels.getStageIndex(data.cultivationLevel());
            if (CultivationLevels.isMortal(runtime.level)
                    && !CultivationLevels.isMortal(data.cultivationLevel())) {
                record(player, ENTER_LIANQI);
            }
            if (newStage > oldStage) record(player, BREAKTHROUGH_SUCCESS);
            if (newStage >= stage(CultivationLevels.REALM_ZHUJI)) {
                discover(player, FOUNDATION);
                record(player, REACH_ZHUJI);
            }
            if (newStage >= stage(CultivationLevels.REALM_JINDAN)) {
                discover(player, JINDAN);
                record(player, REACH_JINDAN);
            }
        }
        if (data.knownSpells().size() > runtime.knownSpells) {
            record(player, LEARN_SPELL,
                    data.knownSpells().size() - runtime.knownSpells);
        }
        if (!data.activeCultivationMethod().equals(runtime.activeMethod)
                && CultivationMethods.get(data.activeCultivationMethod()) != null) {
            record(player, ACTIVATE_METHOD);
        }
        boolean equipped = hasRelevantEquipment(player);
        if (equipped && !runtime.equipped) {
            record(player, EQUIP_ITEM);
        }

        boolean flying = JindanCreativeFlight.isCultivationFlightActive(player)
                && !player.onGround();
        if (flying && !runtime.flying) record(player, START_FLIGHT);
        if (flying) {
            runtime.flightTicks++;
            if (runtime.flightTicks % 20 == 0) record(player, FLIGHT_TICK);
        } else {
            runtime.flightTicks = 0;
        }

        if (CombatStateManager.isInCombat(player)
                && atLeast(data.cultivationLevel(), CultivationLevels.REALM_ZHUJI)) {
            int maxQi = Math.max(1,
                    CultivationLevels.getLevelDef(data.cultivationLevel()).maxQi()
                            + data.maxQiBonus());
            if (data.qi() * 5 >= maxQi) {
                runtime.foundationControlTicks++;
                if (runtime.foundationControlTicks >= 1200) {
                    record(player, FOUNDATION_CONTROL);
                    runtime.foundationControlTicks = 0;
                }
            } else {
                runtime.foundationControlTicks = 0;
            }
        } else {
            runtime.foundationControlTicks = 0;
        }

        if (player.tickCount % 20 == 0) {
            DaoGuidanceObservations.tick(player);
            if (isCurrentObjective(player, FIND_ENLIGHTENMENT_PILL)
                    && hasItem(player, ModItems.ENLIGHTENMENT_PILL.get())) {
                record(player, FIND_ENLIGHTENMENT_PILL);
            }
            CompoundTag state = read(player);
            boolean changed = initialize(state);
            changed |= migrateChapterZero(player, state);
            changed |= ensureCaveTarget(player, state);
            changed |= refreshAvailability(player, state, true);
            if (changed) {
                write(player, state);
                sync(player);
            }
            observeOpenMenu(player, runtime);
            if (isBreakthroughReady(data)) record(player, BREAKTHROUGH_READY);
            if (isCurrentObjective(player, REACH_VEIN)
                    && isNearSpiritVein(player)) {
                record(player, REACH_VEIN);
            }
        }

        if (data.thoughts() > runtime.thoughts
                && read(player).getCompound("Observed").getInt("guide_thought_spent") > 0)
            DaoGuidanceObservations.record(player, "thought_restored");
        runtime.thoughts = data.thoughts();
        runtime.qi = data.qi();
        runtime.level = data.cultivationLevel();
        runtime.knownSpells = data.knownSpells().size();
        runtime.activeMethod = data.activeCultivationMethod();
        runtime.flying = flying;
        runtime.equipped = equipped;
    }

    @SubscribeEvent
    public static void onNpcInteraction(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getTarget() instanceof CultivationNpc) {
            discover(player, NPC_WORLD);
        }
    }

    @SubscribeEvent
    public static void onDamageApplied(LivingDamageEvent.Post event) {
        Entity source = event.getSource().getEntity();
        if (event.getNewDamage() > 0.0F
                && source instanceof ServerPlayer player
                && event.getSource().getDirectEntity() == player) {
            boolean heavy = CombatStateManager.isHeavyAttack(
                    event.getSource());
            if (heavy || CombatStateManager.isCombatWeapon(
                    player.getMainHandItem())) {
                discover(player, CULTIVATOR_COMBAT);
                record(player, WEAPON_ATTACK);
            }
            if (heavy) {
                record(player, WEAPON_TECHNIQUE);
            }
        }
        if (event.getEntity() instanceof ServerPlayer player
                && (event.getSource().getDirectEntity() instanceof Projectile
                || event.getBlockedDamage() > 0.0F)) {
            record(player, RECOGNIZE_ATTACK);
        }
    }

    private static void observeOpenMenu(
            ServerPlayer player, RuntimeState runtime) {
        String current = player.containerMenu.getClass().getName();
        if (current.equals(runtime.menuClass)) return;
        runtime.menuClass = current;
        if (player.containerMenu instanceof QiPouchMenu) {
            discover(player, STORAGE_EQUIPMENT);
            record(player, OPEN_POUCH);
        } else if (player.containerMenu instanceof DingMenu) {
            discover(player, FORGING);
            record(player, OPEN_DING);
        }
    }

    private static boolean refreshAvailability(
            ServerPlayer player, CompoundTag state, boolean notify) {
        boolean changed = false;
        ModAttachments.CultivationData data = ModAttachments.getData(player);
        if (!CultivationLevels.isMortal(data.cultivationLevel())) {
            changed |= markAndGrant(player, state, QI_ENTRY, notify);
        }
        if (!data.knownSpells().isEmpty()) {
            changed |= markAndGrant(player, state, FIRST_SPELLS, notify);
        }
        if (hasRelevantEquipment(player) || hasItemPath(player, "qi_pouch")) {
            changed |= markAvailable(player, state, STORAGE_EQUIPMENT, notify);
        }
        if (hasItemContaining(player, "ding")
                || hasItemContaining(player, "mold")) {
            changed |= markAvailable(player, state, FORGING, notify);
        }
        if (hasItemContaining(player, "yujian")) {
            changed |= markAvailable(player, state, JADE_SLIPS, notify);
        }
        if (hasMagicRitualGuide(player)
                || hasItemPath(player, "magic_center_block")
                || hasItemPath(player, "ritual_dagger")) {
            changed |= markAvailable(player, state, FORMATIONS, notify);
        }
        if (inventoryStacks(player).stream().anyMatch(stack ->
                com.example.immortal_cultivation_mod.magicritual.item.ModItems
                        .isFormationFlag(stack.getItem()))) {
            changed |= markAvailable(player, state, FLAGS, notify);
        }
        if (isBreakthroughReady(data)) {
            changed |= markAvailable(player, state, BREAKTHROUGH, notify);
        }
        if (atLeast(data.cultivationLevel(), CultivationLevels.REALM_ZHUJI)) {
            changed |= markAvailable(player, state, FOUNDATION, notify);
        }
        if (atLeast(data.cultivationLevel(), CultivationLevels.REALM_JINDAN)) {
            changed |= markAvailable(player, state, FLIGHT, notify);
            changed |= markAvailable(player, state, JINDAN, notify);
        }
        if (hasItemContaining(player, "coffin")
                || hasItemContaining(player, "corpse")
                || hasItemContaining(player, "shihun")) {
            changed |= markAvailable(player, state, CORPSE_PATH, notify);
        }
        if (state.getBoolean(TRACKING_ENABLED)
                && state.getString(TRACKED).isBlank()) {
            for (DaoGuidanceContent.Chapter chapter : DaoGuidanceContent.chapters()) {
                if (state.getCompound(DISCOVERED).getBoolean(chapter.id())
                        && !state.getCompound(COMPLETED).getBoolean(chapter.id())) {
                    state.putString(TRACKED, chapter.id());
                    changed = true;
                    break;
                }
            }
        }
        return changed;
    }

    private static boolean markAndGrant(
            ServerPlayer player, CompoundTag state, String chapterId,
            boolean notify) {
        boolean changed = markDiscovered(state, chapterId);
        if (changed) {
            grantChapterStarter(player, state, chapterId);
            sendUnlockNotice(player, state, chapterId, notify);
        }
        return changed;
    }

    private static boolean markAvailable(
            ServerPlayer player, CompoundTag state, String chapterId,
            boolean notify) {
        boolean changed = markDiscovered(state, chapterId);
        if (changed) sendUnlockNotice(player, state, chapterId, notify);
        return changed;
    }

    private static void sendUnlockNotice(
            ServerPlayer player, CompoundTag state,
            String chapterId, boolean notify) {
        if (!notify || !state.getBoolean(TRACKING_ENABLED)) return;
        PacketDistributor.sendToPlayer(player,
                new DaoGuidancePayloads.ClientboundDaoGuidanceNoticePayload(
                        "unlock", chapterId, ""));
    }

    private static boolean discoverForAction(
            ServerPlayer player, CompoundTag state, String action) {
        String chapterId = switch (action) {
            case FIND_ENLIGHTENMENT_PILL -> ENLIGHTENMENT;
            case OPEN_STATS, ENTER_LIANQI -> INITIAL_PATH;
            case MEDITATE_QI, ACTIVATE_METHOD -> QI_ENTRY;
            case LEARN_SPELL, EQUIP_SPELL, CAST_SPELL, CHARGED_SPELL -> FIRST_SPELLS;
            case ACTIVATE_SENSE, DETECT_HIDDEN_LIVING -> SPIRITUAL_SENSE;
            case WEAPON_ATTACK, WEAPON_TECHNIQUE,
                    MANAGE_QI, RECOGNIZE_ATTACK -> CULTIVATOR_COMBAT;
            case EQUIP_ITEM, OPEN_POUCH, POUCH_PLACE, POUCH_REMOVE -> STORAGE_EQUIPMENT;
            case DETECT_VEIN, REACH_VEIN -> SPIRIT_VEIN;
            case OPEN_DING, FORGE_MATERIAL, FORGE_MOLD, FORGE_HEAT,
                    FORGE_MELT, FORGE_COMPLETE -> FORGING;
            case READ_RECIPE_SLIP -> JADE_SLIPS;
            case PLACE_CENTER, BUILD_PATTERN, ACTIVATE_FORMATION -> FORMATIONS;
            case OPEN_FLAG_GUI, INSERT_FLAG -> FLAGS;
            case TALK_NPC, TRADE_NPC, ACCEPT_QUEST -> NPC_WORLD;
            case BREAKTHROUGH_READY, BREAKTHROUGH_ATTEMPT,
                    BREAKTHROUGH_SUCCESS -> BREAKTHROUGH;
            case REACH_ZHUJI, EXTERNAL_QI, FOUNDATION_CONTROL -> FOUNDATION;
            case START_FLIGHT, FLIGHT_TICK, RECALL_WEAPON -> FLIGHT;
            case REACH_JINDAN, JINDAN_SENSE, ENTER_INNER_SPACE -> JINDAN;
            case DISCOVER_CORPSE_PATH -> CORPSE_PATH;
            default -> "";
        };
        if (chapterId.isBlank()) {
            boolean changed = false;
            for (var chapter : DaoGuidanceExpansion.chapters()) {
                if (chapter.objectives().stream().anyMatch(o -> o.action().equals(action))) {
                    boolean added = markDiscovered(state, chapter.id());
                    changed |= added;
                    if (added) sendUnlockNotice(player, state, chapter.id(), true);
                }
            }
            return changed;
        }
        boolean changed = markDiscovered(state, chapterId);
        if (changed) {
            grantChapterStarter(player, state, chapterId);
            sendUnlockNotice(player, state, chapterId, true);
        }
        return changed;
    }

    /**
     * Only the immediately usable beginner lessons unlock in a fixed chain.
     * Every later system remains unknown until its item, realm, location, or
     * genuine gameplay action makes that lesson relevant.
     */
    private static DaoGuidanceContent.Chapter automaticNextChapter(
            String completedChapter) {
        String nextId = switch (completedChapter) {
            case ENLIGHTENMENT -> INITIAL_PATH;
            case INITIAL_PATH -> QI_ENTRY;
            case QI_ENTRY -> FIRST_SPELLS;
            case FIRST_SPELLS -> SPIRITUAL_SENSE;
            case SPIRITUAL_SENSE -> CULTIVATOR_COMBAT;
            default -> "";
        };
        return nextId.isBlank() ? null : DaoGuidanceContent.chapter(nextId);
    }

    private static boolean initialize(CompoundTag state) {
        if (state.getBoolean(INITIALIZED)) {
            boolean migrated = migrateExpansion(state);
            if (!state.contains(TRACKING_ENABLED)) {
                state.putBoolean(TRACKING_ENABLED, true);
                return true;
            }
            return migrated;
        }
        state.putBoolean(INITIALIZED, true);
        CompoundTag discovered = new CompoundTag();
        discovered.putBoolean(ENLIGHTENMENT, true);
        state.put(DISCOVERED, discovered);
        state.put(COMPLETED, new CompoundTag());
        state.put(OBJECTIVE_INDEX, new CompoundTag());
        state.put(PROGRESS, new CompoundTag());
        state.put(REWARDS, new CompoundTag());
        state.putString(TRACKED, ENLIGHTENMENT);
        state.putBoolean(TRACKING_ENABLED, true);
        state.putBoolean(CHAPTER_ZERO_MIGRATED, true);
        state.putInt("ExpansionVersion", 1);
        return true;
    }

    /** Existing indices refer to the old ordered objectives, not the inserted lessons. */
    private static boolean migrateExpansion(CompoundTag state) {
        if (state.getInt("ExpansionVersion") >= 1) return false;
        CompoundTag indices = state.getCompound(OBJECTIVE_INDEX).copy();
        if (!state.getCompound(COMPLETED).getBoolean(STORAGE_EQUIPMENT)
                && indices.getInt(STORAGE_EQUIPMENT) >= 1)
            indices.putInt(STORAGE_EQUIPMENT, indices.getInt(STORAGE_EQUIPMENT) + 1);
        if (!state.getCompound(COMPLETED).getBoolean(FORGING)
                && indices.getInt(FORGING) >= 3)
            indices.putInt(FORGING, indices.getInt(FORGING) + 2);
        state.put(OBJECTIVE_INDEX, indices);
        state.putInt("ExpansionVersion", 1);
        return true;
    }

    /** Client acknowledgement can only advance the discovered chapter's current reading. */
    public static void acknowledge(ServerPlayer player, String action) {
        if (!DaoGuidanceExpansion.isReading(action) || !isCurrentObjective(player, action)) return;
        record(player, action);
    }

    /**
     * Existing worlds already began at the old Chapter 0. Preserve completed
     * players, but place mortal beginners who still need a pill onto the new
     * opening chapter instead of silently skipping it.
     */
    private static boolean migrateChapterZero(
            ServerPlayer player, CompoundTag state) {
        if (state.getBoolean(CHAPTER_ZERO_MIGRATED)) return false;

        CompoundTag discovered = state.getCompound(DISCOVERED).copy();
        CompoundTag completed = state.getCompound(COMPLETED).copy();
        discovered.putBoolean(ENLIGHTENMENT, true);

        ModAttachments.CultivationData data = ModAttachments.getData(player);
        boolean alreadyFoundPill = hasItem(
                player, ModItems.ENLIGHTENMENT_PILL.get());
        boolean alreadyPastOpening = !CultivationLevels.isMortal(
                data.cultivationLevel())
                || completed.getBoolean(INITIAL_PATH)
                || alreadyFoundPill;
        if (alreadyPastOpening) {
            completed.putBoolean(ENLIGHTENMENT, true);
        } else if (state.getBoolean(TRACKING_ENABLED)) {
            state.putString(TRACKED, ENLIGHTENMENT);
        }

        state.put(DISCOVERED, discovered);
        state.put(COMPLETED, completed);
        state.putBoolean(CHAPTER_ZERO_MIGRATED, true);
        return true;
    }

    private static boolean ensureCaveTarget(
            ServerPlayer player, CompoundTag state) {
        if (!state.getCompound(DISCOVERED).getBoolean(ENLIGHTENMENT)
                || state.getCompound(COMPLETED).getBoolean(ENLIGHTENMENT)
                || !state.getString(CAVE_TARGET_DIMENSION).isBlank()) {
            return false;
        }

        ServerLevel overworld = player.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) return false;
        long now = overworld.getGameTime();
        BlockPos currentOrigin = player.serverLevel() == overworld
                ? player.blockPosition()
                : overworld.getSharedSpawnPos();

        // Older saves only stored a five-minute retry timestamp and searched
        // the same fixed area forever.  Give them a persistent outward search
        // immediately when they load this version.
        boolean changed = false;
        if (!state.contains(CAVE_SEARCH_ATTEMPT)) {
            state.putInt(CAVE_SEARCH_ATTEMPT, 0);
            state.putInt(CAVE_SEARCH_ORIGIN_X, currentOrigin.getX());
            state.putInt(CAVE_SEARCH_ORIGIN_Z, currentOrigin.getZ());
            state.putLong(CAVE_SEARCH_AFTER, now);
            changed = true;
        }

        long searchAfter = state.getLong(CAVE_SEARCH_AFTER);
        if (searchAfter - now > CAVE_SEARCH_RETRY_TICKS) {
            state.putLong(CAVE_SEARCH_AFTER, now);
            searchAfter = now;
            changed = true;
        }
        if (now < searchAfter) return changed;

        int attempt = Math.max(0, state.getInt(CAVE_SEARCH_ATTEMPT));
        BlockPos anchor = new BlockPos(
                state.getInt(CAVE_SEARCH_ORIGIN_X),
                currentOrigin.getY(),
                state.getInt(CAVE_SEARCH_ORIGIN_Z));
        BlockPos origin = caveSearchOrigin(anchor, attempt);
        BlockPos target = overworld.findNearestMapStructure(
                TUTORIAL_CAVES, origin, CAVE_SEARCH_RADIUS_CHUNKS, false);
        state.putLong(CAVE_SEARCH_AFTER,
                now + CAVE_SEARCH_RETRY_TICKS);
        state.putInt(CAVE_SEARCH_ATTEMPT,
                attempt == Integer.MAX_VALUE ? 0 : attempt + 1);
        if (target == null) return true;

        state.putString(CAVE_TARGET_DIMENSION,
                Level.OVERWORLD.location().toString());
        state.putInt(CAVE_TARGET_X, target.getX());
        state.putInt(CAVE_TARGET_Z, target.getZ());
        return true;
    }

    /**
     * Covers the world in adjoining fixed-size search squares.  Each failed
     * attempt advances one square along an outward spiral, so the tutorial
     * keeps reaching new terrain without making one enormous locator call on
     * the server thread.
     */
    private static BlockPos caveSearchOrigin(BlockPos anchor, int attempt) {
        if (attempt <= 0) return anchor;

        long ring = (long) Math.ceil(
                (Math.sqrt((double) attempt + 1.0D) - 1.0D) / 2.0D);
        long side = ring * 2L;
        long diameter = side + 1L;
        long offset = diameter * diameter - 1L - attempt;
        long gridX;
        long gridZ;
        if (offset < side) {
            gridX = ring - offset;
            gridZ = -ring;
        } else if (offset < side * 2L) {
            gridX = -ring;
            gridZ = -ring + offset - side;
        } else if (offset < side * 3L) {
            gridX = -ring + offset - side * 2L;
            gridZ = ring;
        } else {
            gridX = ring;
            gridZ = ring - offset + side * 3L;
        }
        return anchor.offset(
                Math.toIntExact(gridX * CAVE_SEARCH_STRIDE_BLOCKS),
                0,
                Math.toIntExact(gridZ * CAVE_SEARCH_STRIDE_BLOCKS));
    }

    private static boolean markDiscovered(CompoundTag state, String chapterId) {
        if (DaoGuidanceContent.chapter(chapterId) == null) return false;
        CompoundTag discovered = state.getCompound(DISCOVERED).copy();
        if (discovered.getBoolean(chapterId)) return false;
        discovered.putBoolean(chapterId, true);
        state.put(DISCOVERED, discovered);
        return true;
    }

    private static void grantChapterStarter(
            ServerPlayer player, CompoundTag state, String chapterId) {
        if (QI_ENTRY.equals(chapterId)) {
            ModAttachments.CultivationData data =
                    ModAttachments.getData(player);
            boolean alreadyHasMethod = CultivationMethods.get(
                    data.activeCultivationMethod()) != null
                            || data.methodProficiencies().containsKey(
                            CultivationMethods.BASIC_BREATHING);
            grantOnce(player, state, "basic_method",
                    ModItems.BASIC_BREATHING_METHOD.get(),
                    alreadyHasMethod);
        } else if (FIRST_SPELLS.equals(chapterId)) {
            boolean knowsWindBlade = ModAttachments.getData(player)
                    .knownSpells().stream()
                    .map(ModSpells::normalizeId)
                    .anyMatch(ModSpells.WIND_BLADE::equals);
            grantOnce(player, state, "wind_blade_scroll",
                    ModItems.WIND_BLADE_SCROLL.get(), knowsWindBlade);
        }
    }

    private static void grantOnce(
            ServerPlayer player, CompoundTag state, String rewardId,
            Item item, boolean alreadyLearned) {
        CompoundTag rewards = state.getCompound(REWARDS).copy();
        if (rewards.getBoolean(rewardId)) return;
        if (!alreadyLearned && !hasItem(player, item)) {
            ItemStack stack = new ItemStack(item);
            if (!player.getInventory().add(stack)) player.drop(stack, false);
        }
        rewards.putBoolean(rewardId, true);
        state.put(REWARDS, rewards);
    }

    private static boolean hasItem(ServerPlayer player, Item item) {
        return player.getInventory().items.stream().anyMatch(stack -> stack.is(item))
                || player.getInventory().offhand.stream().anyMatch(stack -> stack.is(item));
    }

    private static boolean hasItemPath(ServerPlayer player, String path) {
        return inventoryStacks(player).stream().anyMatch(stack -> {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            return id != null && ImmortalCultivationMod.MODID.equals(id.getNamespace())
                    && id.getPath().equals(path);
        });
    }

    private static boolean hasItemContaining(ServerPlayer player, String fragment) {
        return inventoryStacks(player).stream().anyMatch(stack -> {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            return id != null && ImmortalCultivationMod.MODID.equals(id.getNamespace())
                    && id.getPath().contains(fragment);
        });
    }

    private static boolean hasMagicRitualGuide(ServerPlayer player) {
        return inventoryStacks(player).stream()
                .anyMatch(DaoGuidanceSystem::isMagicRitualGuide);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean isMagicRitualGuide(ItemStack stack) {
        if (!PATCHOULI_GUIDE_ITEM_ID.equals(
                BuiltInRegistries.ITEM.getKey(stack.getItem()))) {
            return false;
        }
        DataComponentType<?> component =
                BuiltInRegistries.DATA_COMPONENT_TYPE.get(
                        PATCHOULI_BOOK_COMPONENT_ID);
        if (component == null) {
            return false;
        }
        Object bookId = stack.get((DataComponentType) component);
        return MAGIC_RITUAL_GUIDE_ID.equals(bookId);
    }

    private static List<ItemStack> inventoryStacks(ServerPlayer player) {
        return java.util.stream.Stream.concat(
                player.getInventory().items.stream(),
                player.getInventory().offhand.stream()).filter(stack -> !stack.isEmpty()).toList();
    }

    private static boolean hasRelevantEquipment(ServerPlayer player) {
        boolean vanillaArmor = !player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()
                || !player.getItemBySlot(EquipmentSlot.CHEST).isEmpty()
                || !player.getItemBySlot(EquipmentSlot.LEGS).isEmpty()
                || !player.getItemBySlot(EquipmentSlot.FEET).isEmpty();
        if (vanillaArmor) return true;
        return CuriosApi.getCuriosInventory(player).map(handler ->
                EQUIPMENT_CURIO_SLOTS.stream().anyMatch(slotId ->
                        handler.getStacksHandler(slotId).map(stacksHandler -> {
                            var stacks = stacksHandler.getStacks();
                            for (int slot = 0; slot < stacks.getSlots(); slot++) {
                                if (!stacks.getStackInSlot(slot).isEmpty()) {
                                    return true;
                                }
                            }
                            return false;
                        }).orElse(false))).orElse(false);
    }

    private static boolean isBreakthroughReady(
            ModAttachments.CultivationData data) {
        String next = CultivationLevels.getNextStage(data.cultivationLevel());
        if (next == null) return false;
        long required = CultivationLevels.getTotalQiNeeded(data.cultivationLevel());
        int maxQi = Math.max(1,
                CultivationLevels.getLevelDef(data.cultivationLevel()).maxQi()
                        + data.maxQiBonus());
        return data.cultivationProgress() >= required && data.qi() >= maxQi;
    }

    private static boolean durableFactSatisfied(
            ServerPlayer player,
            ModAttachments.CultivationData data,
            String action) {
        return switch (action) {
            case FIND_ENLIGHTENMENT_PILL -> hasItem(
                    player, ModItems.ENLIGHTENMENT_PILL.get());
            case ENTER_LIANQI -> !CultivationLevels.isMortal(
                    data.cultivationLevel());
            case ACTIVATE_METHOD -> CultivationMethods.get(
                    data.activeCultivationMethod()) != null;
            case LEARN_SPELL -> !data.knownSpells().isEmpty();
            case EQUIP_ITEM -> hasRelevantEquipment(player);
            case BREAKTHROUGH_READY -> isBreakthroughReady(data);
            case REACH_ZHUJI -> atLeast(data.cultivationLevel(),
                    CultivationLevels.REALM_ZHUJI);
            case REACH_JINDAN -> atLeast(data.cultivationLevel(),
                    CultivationLevels.REALM_JINDAN);
            default -> false;
        };
    }

    private static boolean isNearSpiritVein(ServerPlayer player) {
        BlockPos center = player.blockPosition();
        for (BlockPos position : BlockPos.betweenClosed(
                center.offset(-4, -4, -4), center.offset(4, 4, 4))) {
            if (player.serverLevel().getBlockState(position).is(
                    com.example.immortal_cultivation_mod.magicritual.block.ModBlocks
                            .SPIRIT_VEIN_CENTER.get())) {
                return true;
            }
        }
        return false;
    }

    private static int stage(String realm) {
        return CultivationLevels.getStageIndex(realm + CultivationLevels.STAGE_EARLY);
    }

    private static boolean atLeast(String level, String realm) {
        return CultivationLevels.getStageIndex(level) >= stage(realm);
    }

    private static CompoundTag read(ServerPlayer player) {
        return player.getPersistentData().contains(ROOT_TAG)
                ? player.getPersistentData().getCompound(ROOT_TAG).copy()
                : new CompoundTag();
    }

    private static void write(ServerPlayer player, CompoundTag state) {
        player.getPersistentData().put(ROOT_TAG, state.copy());
    }

    private static final class RuntimeState {
        private int qi;
        private int thoughts;
        private String level;
        private int knownSpells;
        private String activeMethod;
        private boolean flying;
        private int flightTicks;
        private int foundationControlTicks;
        private String menuClass;
        private boolean equipped;

        private RuntimeState(ServerPlayer player) {
            ModAttachments.CultivationData data = ModAttachments.getData(player);
            qi = data.qi();
            thoughts = data.thoughts();
            level = data.cultivationLevel();
            knownSpells = data.knownSpells().size();
            activeMethod = data.activeCultivationMethod();
            flying = JindanCreativeFlight.isCultivationFlightActive(player)
                    && !player.onGround();
            menuClass = player.containerMenu.getClass().getName();
            equipped = hasRelevantEquipment(player);
        }

        private static RuntimeState capture(ServerPlayer player) {
            return new RuntimeState(player);
        }
    }
}
