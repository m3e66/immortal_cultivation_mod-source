package com.example.immortal_cultivation_mod.system;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.BodyTypes;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.block.ModBlocks;
import com.example.immortal_cultivation_mod.entity.CultivationNpc;
import com.example.immortal_cultivation_mod.entity.ModEntities;
import com.example.immortal_cultivation_mod.entity.SystemManifestEntity;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.item.OuterSystemItem;
import com.example.immortal_cultivation_mod.network.OuterSystemPayloads;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = ImmortalCultivationMod.MODID)
public final class OuterSystemManager {
    private static final String RECENT_DAMAGE = "ImmortalOuterSystemDamage";
    private static final String ACTIVE = "Active";
    private static final String COMPLETED = "Completed";
    private static final String NEXT_AT = "NextAt";
    private static final String TARGET_X = "TargetX";
    private static final String TARGET_Y = "TargetY";
    private static final String TARGET_Z = "TargetZ";
    private static final String TARGET_NAME = "TargetName";
    private static final String KIND = "Kind";
    private static final String TIER = "Tier";
    private static final String SEED = "Seed";
    private static final String DIMENSION = "Dimension";
    private static final long QUEST_INTERVAL_MS = 20L * 60L * 1000L;
    private static final long REPEAT_PLAYER_KILL_MS = 20L * 60L * 1000L;
    private static final Set<UUID> OPEN_SCREENS = new HashSet<>();
    private static final Map<UUID, UUID> MANIFESTS = new HashMap<>();
    private static final Map<UUID, Boolean> LAST_EQUIPPED = new HashMap<>();

    private OuterSystemManager() {
    }

    @SubscribeEvent
    public static void onSystemPickup(ItemEntityPickupEvent.Post event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)
                || OuterSystemData.equipped(player).isPresent()) {
            return;
        }
        ItemStack pickedUp = event.getOriginalStack();
        if (!pickedUp.is(ModItems.SYSTEM.get())
                || pickedUp.getCount() <= event.getCurrentStack().getCount()) {
            return;
        }
        // Post runs only after a valid pickup, including delay, ownership and inventory checks.
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!ItemStack.isSameItemSameComponents(stack, pickedUp)) {
                continue;
            }
            bindFromItem(player, stack);
            return;
        }
    }

    /** Rolls, binds and consumes one system item, shared by pickup and right-click use. */
    public static boolean bindFromItem(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty() || !stack.is(ModItems.SYSTEM.get())
                || OuterSystemData.equipped(player).isPresent()) {
            return false;
        }
        OuterSystemData.ensureSlot(player);
        OuterSystemType type = OuterSystemType.random(player.getRandom());
        ItemStack bound = stack.copyWithCount(1);
        OuterSystemItem.setType(bound, type);
        if (!OuterSystemData.equip(player, bound)) {
            return false;
        }
        stack.shrink(1);
        player.getInventory().setChanged();
        CompoundTag state = OuterSystemData.read(player);
        state.putBoolean(OuterSystemData.PICKUP_BOUND, true);
        state.putBoolean(OuterSystemData.EVER_BOUND, true);
        state.putBoolean(OuterSystemData.DESTROYED, false);
        state.putBoolean(OuterSystemData.BOOT_SHOWN, true);
        state.putString(OuterSystemData.TYPE, type.id());
        OuterSystemData.save(player, state);
        PacketDistributor.sendToPlayer(player,
                new OuterSystemPayloads.ClientboundBoot(type.id()));
        sync(player);
        return true;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || player.tickCount % 20 != 0) {
            return;
        }
        ensureBinding(player);
        boolean equipped = OuterSystemData.equipped(player).isPresent();
        if (!LAST_EQUIPPED.containsKey(player.getUUID())
                || LAST_EQUIPPED.get(player.getUUID()) != equipped) {
            sync(player);
        }
        if (equipped
                && OuterSystemData.type(player)
                        == OuterSystemType.DAILY_SIGN_IN) {
            ensureQuest(player);
            checkQuestCompletion(player);
        } else if (equipped
                && OuterSystemData.type(player)
                        == OuterSystemType.SYSTEM_SHOP) {
            SystemShop.ensureDay(player);
        } else if (equipped
                && OuterSystemData.type(player)
                        == OuterSystemType.DIVINE_GACHA) {
            DivineGachaSystem.ensureDay(player);
        } else if (equipped
                && OuterSystemData.type(player)
                        == OuterSystemType.LIFE_SIMULATION) {
            LifeSimulationSystem.ensureDay(player);
        }
        if (OPEN_SCREENS.contains(player.getUUID())) {
            sendScreenState(player, false);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ensureBinding(player);
            sync(player);
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        OuterSystemData.copy(event.getOriginal(), event.getEntity());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            closeScreen(player);
            LAST_EQUIPPED.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        ServerPlayer attacker = attackingPlayer(event.getSource());
        if (event.getNewDamage() <= 0.0F
                || attacker == null
                || event.getEntity() == attacker) {
            return;
        }
        CompoundTag recent = event.getEntity().getPersistentData()
                .getCompound(RECENT_DAMAGE).copy();
        if (!recent.hasUUID("Player")
                || !attacker.getUUID().equals(recent.getUUID("Player"))
                || recent.getLong("At") + 200L
                        < attacker.serverLevel().getGameTime()) {
            recent = new CompoundTag();
            recent.putUUID("Player", attacker.getUUID());
            recent.putFloat("Damage", 0.0F);
        }
        recent.putFloat(
                "Damage", recent.getFloat("Damage") + event.getNewDamage());
        recent.putLong("At", attacker.serverLevel().getGameTime());
        event.getEntity().getPersistentData().put(RECENT_DAMAGE, recent);
    }

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        ServerPlayer killer = resolveKiller(event);
        if (killer == null
                || OuterSystemData.equipped(killer).isEmpty()
                || OuterSystemData.type(killer)
                        != OuterSystemType.KILL_GROWTH) {
            return;
        }
        creditKill(killer, event.getEntity());
    }

    public static boolean isScreenOpen(ServerPlayer player) {
        return OPEN_SCREENS.contains(player.getUUID());
    }

    public static void openScreen(ServerPlayer player) {
        if (OuterSystemData.equipped(player).isEmpty()) {
            sync(player);
            return;
        }
        if (OuterSystemData.type(player) == OuterSystemType.DAILY_SIGN_IN) {
            ensureQuest(player);
        } else if (OuterSystemData.type(player)
                == OuterSystemType.SYSTEM_SHOP) {
            SystemShop.ensureDay(player);
        } else if (OuterSystemData.type(player)
                == OuterSystemType.DIVINE_GACHA) {
            DivineGachaSystem.ensureDay(player);
        } else if (OuterSystemData.type(player)
                == OuterSystemType.LIFE_SIMULATION) {
            LifeSimulationSystem.ensureDay(player);
        }
        OPEN_SCREENS.add(player.getUUID());
        spawnManifest(player);
        sendScreenState(player, true);
    }

    public static void closeScreen(ServerPlayer player) {
        OPEN_SCREENS.remove(player.getUUID());
        SystemShopSync.clear(player.getUUID());
        UUID entityId = MANIFESTS.remove(player.getUUID());
        if (entityId != null && player.level() instanceof ServerLevel level) {
            Entity entity = level.getEntity(entityId);
            if (entity != null) {
                entity.discard();
            }
        }
    }

    /** Lifespan exhaustion ends this life's system, including item-bound state and progress. */
    public static void loseOnAgeExhaustion(ServerPlayer player) {
        CompoundTag cleared = new CompoundTag();
        // An undying body can retain its body type after lifespan runs out. Do not auto-rebind it.
        if (BodyTypes.OUTER_DEMON_BODY.equals(ModAttachments.getData(player).bodyType())) {
            cleared.putBoolean(OuterSystemData.DESTROYED, true);
        }
        boolean changed = !OuterSystemData.read(player).equals(cleared)
                || OuterSystemData.equipped(player).isPresent() || OPEN_SCREENS.contains(player.getUUID());
        UUID manifestId = MANIFESTS.get(player.getUUID());
        closeScreen(player);
        if (manifestId != null) {
            for (ServerLevel level : player.server.getAllLevels()) {
                Entity manifest = level.getEntity(manifestId);
                if (manifest != null) manifest.discard();
            }
        }
        OuterSystemData.removeEquipped(player);
        OuterSystemData.removeSlot(player);
        if (cleared.isEmpty()) player.getPersistentData().remove(OuterSystemData.ROOT_TAG);
        else OuterSystemData.save(player, cleared);
        if (changed) sync(player);
    }

    public static void destroySystem(SystemManifestEntity manifest) {
        UUID ownerId = manifest.ownerId().orElse(null);
        if (ownerId == null
                || !(manifest.level() instanceof ServerLevel level)) {
            return;
        }
        ServerPlayer owner = level.getServer().getPlayerList()
                .getPlayer(ownerId);
        if (owner == null) {
            return;
        }
        OPEN_SCREENS.remove(ownerId);
        MANIFESTS.remove(ownerId);
        OuterSystemData.removeEquipped(owner);
        OuterSystemData.removeSlot(owner);
        CompoundTag state = OuterSystemData.read(owner);
        state.putBoolean(OuterSystemData.DESTROYED, true);
        OuterSystemData.save(owner, state);
        manifest.spawnAtLocation(
                new ItemStack(ModItems.GREAT_CREATION_SHARD.get()));
        sync(owner);
    }

    public static void claimQuest(ServerPlayer player) {
        if (OuterSystemData.type(player)
                != OuterSystemType.DAILY_SIGN_IN) {
            return;
        }
        CompoundTag state = OuterSystemData.read(player);
        CompoundTag quest = state.getCompound(OuterSystemData.QUEST).copy();
        if (!quest.getBoolean(ACTIVE)
                || !quest.getBoolean(COMPLETED)) {
            return;
        }
        List<ItemStack> rewards = rewards(
                quest.getString("RewardRealm").isEmpty()
                        ? ModAttachments.getData(player).cultivationLevel() : quest.getString("RewardRealm"),
                quest.getInt(TIER), quest.getLong(SEED));
        for (ItemStack reward : rewards) {
            ItemStack remaining = reward.copy();
            player.getInventory().add(remaining);
            if (!remaining.isEmpty()) {
                player.drop(remaining, false);
            }
        }
        quest = new CompoundTag();
        quest.putLong(NEXT_AT, System.currentTimeMillis() + QUEST_INTERVAL_MS);
        state.put(OuterSystemData.QUEST, quest);
        OuterSystemData.save(player, state);
        player.displayClientMessage(Component.translatable(
                "message.immortal_cultivation_mod.system.quest_claimed"), true);
        sendScreenState(player, false);
    }

    public static void buyShopItem(ServerPlayer player, String itemId) {
        buyShopItem(player, itemId, false);
    }

    public static void buyShopItem(ServerPlayer player, String itemId,
                                   boolean buyStack) {
        SystemShop.buy(player, itemId, buyStack);
        refreshOpenScreen(player);
    }

    public static void recycleShopItem(
            ServerPlayer player, int inventorySlot) {
        SystemShop.recycle(player, inventorySlot);
        refreshOpenScreen(player);
    }

    public static void submitShopRequest(
            ServerPlayer player, int requestIndex) {
        SystemShop.submitRequest(player, requestIndex);
        refreshOpenScreen(player);
    }

    public static void drawGacha(ServerPlayer player) {
        DivineGachaSystem.draw(player);
        refreshOpenScreen(player);
    }

    public static void claimGachaRewards(ServerPlayer player) {
        DivineGachaSystem.claimPending(player);
        refreshOpenScreen(player);
    }

    public static void handleLifeAction(
            ServerPlayer player, String action) {
        if (!OPEN_SCREENS.contains(player.getUUID())
                || OuterSystemData.equipped(player).isEmpty()
                || OuterSystemData.type(player) != OuterSystemType.LIFE_SIMULATION) {
            return;
        }
        LifeSimulationSystem.handleAction(player, action);
        // A rejected action also acknowledges its request. Periodic refreshes keep this
        // revision unchanged, so clients cannot confuse an old snapshot with the response.
        CompoundTag state = OuterSystemData.read(player);
        long revision = state.getLong("LifeActionRevision");
        state.putLong("LifeActionRevision", revision == Long.MAX_VALUE ? 1L : revision + 1L);
        OuterSystemData.save(player, state);
        refreshOpenScreen(player);
    }

    public static void onBodyTypeChanged(ServerPlayer player) {
        ensureBinding(player);
    }

    /**
     * Returns the system selected in the debug editor, falling back to the
     * currently bound system when no debug preset has been chosen.
     */
    public static OuterSystemType debugSystemType(ServerPlayer player) {
        CompoundTag state = OuterSystemData.read(player);
        String debugType = state.getString(OuterSystemData.DEBUG_TYPE);
        if (!debugType.isBlank()) {
            return OuterSystemType.byId(debugType);
        }
        return OuterSystemData.equipped(player)
                .map(OuterSystemItem::getType)
                .orElseGet(() -> OuterSystemType.byId(
                        state.getString(OuterSystemData.TYPE)));
    }

    /**
     * Changes an existing bound system, or stores a preset for a future body-granted system.
     */
    public static void setDebugSystem(ServerPlayer player, OuterSystemType type) {
        if (type == null) {
            return;
        }

        CompoundTag state = OuterSystemData.read(player);
        var equipped = OuterSystemData.equipped(player);
        String previousType = equipped
                .map(OuterSystemItem::getType)
                .map(OuterSystemType::id)
                .orElse(state.getString(OuterSystemData.TYPE));
        state.putString(OuterSystemData.DEBUG_TYPE, type.id());
        state.putString(OuterSystemData.TYPE, type.id());
        state.putBoolean(OuterSystemData.DESTROYED, false);
        if (!previousType.isBlank() && !previousType.equals(type.id())) {
            state.putString(OuterSystemData.PREVIOUS_TYPE, previousType);
        }

        boolean outerBody = BodyTypes.OUTER_DEMON_BODY.equals(
                ModAttachments.getData(player).bodyType());
        if (outerBody || equipped.isPresent() || state.getBoolean(OuterSystemData.PICKUP_BOUND)) {
            closeScreen(player);
            OuterSystemData.ensureSlot(player);
            ItemStack replacement = equipped.map(ItemStack::copy)
                    .orElseGet(() -> OuterSystemItem.create(type));
            OuterSystemItem.setType(replacement, type);
            OuterSystemData.removeEquipped(player);
            if (OuterSystemData.equip(player, replacement)) {
                state.putBoolean(OuterSystemData.EVER_BOUND, true);
                state.putBoolean(OuterSystemData.BOOT_SHOWN, true);
                if (!outerBody) state.putBoolean(OuterSystemData.PICKUP_BOUND, true);
            }
        }
        OuterSystemData.save(player, state);
        sync(player);
    }

    private static void ensureBinding(ServerPlayer player) {
        CompoundTag state = OuterSystemData.read(player);
        if (state.getBoolean(OuterSystemData.PICKUP_BOUND)) {
            if (state.getBoolean(OuterSystemData.DESTROYED)) {
                OuterSystemData.removeEquipped(player);
                OuterSystemData.removeSlot(player);
                return;
            }
            OuterSystemData.ensureSlot(player);
            if (!player.getAbilities().instabuild
                    && OuterSystemData.equipped(player).isEmpty()) {
                OuterSystemData.equip(player, OuterSystemItem.create(
                        OuterSystemType.byId(state.getString(OuterSystemData.TYPE))));
            }
            return;
        }
        boolean outerBody = BodyTypes.OUTER_DEMON_BODY.equals(
                ModAttachments.getData(player).bodyType());
        if (!outerBody) {
            if (state.getBoolean(OuterSystemData.EVER_BOUND)
                    || OuterSystemData.equipped(player).isPresent()) {
                endBodyBinding(player, state);
            }
            return;
        }
        if (state.getBoolean(OuterSystemData.DESTROYED)) {
            OuterSystemData.removeEquipped(player);
            OuterSystemData.removeSlot(player);
            return;
        }
        if (outerBody && !state.getBoolean(OuterSystemData.EVER_BOUND)) {
            OuterSystemType type = state.getString(OuterSystemData.DEBUG_TYPE).isBlank()
                    ? rerollSystem(player.getRandom(),
                    state.getString(OuterSystemData.PREVIOUS_TYPE))
                    : OuterSystemType.byId(
                    state.getString(OuterSystemData.DEBUG_TYPE));
            OuterSystemData.ensureSlot(player);
            if (!OuterSystemData.equip(
                    player, OuterSystemItem.create(type))) {
                return;
            }
            state.putBoolean(OuterSystemData.EVER_BOUND, true);
            state.putBoolean(OuterSystemData.DESTROYED, false);
            state.putString(OuterSystemData.TYPE, type.id());
            state.putBoolean(OuterSystemData.BOOT_SHOWN, true);
            PacketDistributor.sendToPlayer(
                    player,
                    new OuterSystemPayloads.ClientboundBoot(type.id()));
            OuterSystemData.save(player, state);
            sync(player);
            return;
        }
        OuterSystemData.ensureSlot(player);
        if (outerBody
                && state.getBoolean(OuterSystemData.EVER_BOUND)
                && !state.getBoolean(OuterSystemData.DESTROYED)
                && !player.getAbilities().instabuild
                && OuterSystemData.equipped(player).isEmpty()) {
            OuterSystemData.equip(
                    player,
                    OuterSystemItem.create(OuterSystemType.byId(
                            state.getString(OuterSystemData.TYPE))));
        }
    }

    private static void endBodyBinding(
            ServerPlayer player, CompoundTag state) {
        String previousType = OuterSystemData.equipped(player)
                .map(OuterSystemItem::getType)
                .map(OuterSystemType::id)
                .orElse(state.getString(OuterSystemData.TYPE));
        closeScreen(player);
        OuterSystemData.removeEquipped(player);
        OuterSystemData.removeSlot(player);
        state.putBoolean(OuterSystemData.EVER_BOUND, false);
        state.putBoolean(OuterSystemData.DESTROYED, false);
        state.putBoolean(OuterSystemData.BOOT_SHOWN, false);
        state.remove(OuterSystemData.TYPE);
        if (!previousType.isBlank()) {
            state.putString(OuterSystemData.PREVIOUS_TYPE, previousType);
        }
        OuterSystemData.save(player, state);
        sync(player);
    }

    private static OuterSystemType rerollSystem(
            RandomSource random, String previousTypeId) {
        if (previousTypeId == null || previousTypeId.isBlank()) {
            return OuterSystemType.random(random);
        }
        OuterSystemType previous = OuterSystemType.byId(previousTypeId);
        OuterSystemType result;
        do {
            result = OuterSystemType.random(random);
        } while (result == previous);
        return result;
    }

    private static void sync(ServerPlayer player) {
        boolean equipped = OuterSystemData.equipped(player).isPresent();
        LAST_EQUIPPED.put(player.getUUID(), equipped);
        String type = equipped
                ? OuterSystemData.type(player).id()
                : OuterSystemData.read(player).getString(OuterSystemData.TYPE);
        PacketDistributor.sendToPlayer(
                player,
                new OuterSystemPayloads.ClientboundSync(type, equipped));
    }

    private static void spawnManifest(ServerPlayer player) {
        UUID existingId = MANIFESTS.get(player.getUUID());
        if (existingId != null
                && player.level() instanceof ServerLevel level
                && level.getEntity(existingId) instanceof SystemManifestEntity) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        SystemManifestEntity manifest =
                ModEntities.SYSTEM_MANIFEST.get().create(level);
        if (manifest == null) {
            return;
        }
        manifest.initialize(player, OuterSystemData.type(player));
        level.addFreshEntity(manifest);
        MANIFESTS.put(player.getUUID(), manifest.getUUID());
    }

    private static void refreshOpenScreen(ServerPlayer player) {
        if (OPEN_SCREENS.contains(player.getUUID())) {
            sendScreenState(player, false);
        }
    }

    public static void refreshScreen(ServerPlayer player) {
        refreshOpenScreen(player);
    }

    private static void sendScreenState(
            ServerPlayer player, boolean includeShopCatalog) {
        CompoundTag state = createClientState(player, includeShopCatalog);
        SystemShopSync.Update shopUpdate = null;
        if (OuterSystemData.type(player) == OuterSystemType.SYSTEM_SHOP) {
            shopUpdate = SystemShopSync.prepare(player, includeShopCatalog, state);
        } else {
            SystemShopSync.clear(player.getUUID());
        }
        // Only an explicit Open request may open a new client screen. An update
        // queued before Close must not bring the interface back after dismissal.
        state.putBoolean("OpenRequested", includeShopCatalog);
        PacketDistributor.sendToPlayer(
                player,
                new OuterSystemPayloads.ClientboundOpen(state));
        if (shopUpdate != null) shopUpdate.send(player);
    }

    private static CompoundTag createClientState(
            ServerPlayer player, boolean includeShopCatalog) {
        CompoundTag result = new CompoundTag();
        OuterSystemType type = OuterSystemData.type(player);
        result.putString("Type", type.id());
        var data = ModAttachments.getData(player);
        result.putLong("Progress", data.cultivationProgress());
        result.putLong(
                "ProgressRequired",
                CultivationLevels.getTotalQiNeeded(data.cultivationLevel()));

        if (type == OuterSystemType.DAILY_SIGN_IN) {
            CompoundTag quest = OuterSystemData.read(player)
                    .getCompound(OuterSystemData.QUEST);
            boolean active = quest.getBoolean(ACTIVE);
            result.putBoolean("QuestActive", active);
            result.putBoolean("QuestCompleted", quest.getBoolean(COMPLETED));
            result.putLong(
                    "QuestNextSeconds",
                    Math.max(0L, (quest.getLong(NEXT_AT)
                            - System.currentTimeMillis()) / 1000L));
            if (active) {
                result.putInt("QuestX", quest.getInt(TARGET_X));
                result.putInt("QuestY", quest.getInt(TARGET_Y));
                result.putInt("QuestZ", quest.getInt(TARGET_Z));
                result.putString(
                        "QuestTargetName", quest.getString(TARGET_NAME));
                result.putString("QuestKind", quest.getString(KIND));
                result.putInt("QuestTier", quest.getInt(TIER));
                int distance = (int) Math.round(Math.sqrt(
                        player.blockPosition().distSqr(new BlockPos(
                                quest.getInt(TARGET_X),
                                player.blockPosition().getY(),
                                quest.getInt(TARGET_Z)))));
                result.putInt("QuestDistance", distance);
                result.putString(
                        "QuestRewardSummary",
                        rewardSummary(
                                quest.getString("RewardRealm").isEmpty()
                                        ? ModAttachments.getData(player).cultivationLevel() : quest.getString("RewardRealm"),
                                quest.getInt(TIER),
                                quest.getLong(SEED)));
            }
        } else if (type == OuterSystemType.SYSTEM_SHOP) {
            SystemShop.writeClientState(
                    player, result, includeShopCatalog);
        } else if (type == OuterSystemType.DIVINE_GACHA) {
            DivineGachaSystem.writeClientState(player, result);
        } else if (type == OuterSystemType.LIFE_SIMULATION) {
            LifeSimulationSystem.writeClientState(player, result);
            result.putLong("LifeActionRevision", OuterSystemData.read(player).getLong("LifeActionRevision"));
        }
        return result;
    }

    private static ServerPlayer resolveKiller(LivingDeathEvent event) {
        ServerPlayer player = attackingPlayer(event.getSource());
        if (player != null) {
            return player;
        }
        CompoundTag recent = event.getEntity().getPersistentData()
                .getCompound(RECENT_DAMAGE);
        if (!recent.hasUUID("Player")
                || recent.getFloat("Damage")
                        < event.getEntity().getMaxHealth() * 0.2F
                || recent.getLong("At") + 200L
                        < event.getEntity().level().getGameTime()) {
            return null;
        }
        return event.getEntity().getServer().getPlayerList()
                .getPlayer(recent.getUUID("Player"));
    }

    private static ServerPlayer attackingPlayer(DamageSource source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return player;
        }
        if (source.getDirectEntity() instanceof Projectile projectile
                && projectile.getOwner() instanceof ServerPlayer player) {
            return player;
        }
        if (source.getDirectEntity() instanceof OwnableEntity owned
                && owned.getOwner() instanceof ServerPlayer player) {
            return player;
        }
        return null;
    }

    private static void creditKill(
            ServerPlayer killer, LivingEntity target) {
        if (target == killer
                || target instanceof ArmorStand
                || target instanceof Animal
                || target instanceof TamableAnimal tamable
                        && tamable.getOwnerUUID() != null) {
            return;
        }
        if (target instanceof ServerPlayer victim
                && isRepeatedPlayerKill(killer, victim)) {
            return;
        }

        var data = ModAttachments.getData(killer);
        long required = Math.max(
                1L,
                CultivationLevels.getTotalQiNeeded(
                        data.cultivationLevel()));
        double fraction = killFraction(killer, target);
        if (target instanceof WitherBoss
                || target instanceof EnderDragon) {
            fraction *= 2.0D;
        }
        fraction = Math.min(0.16D, fraction);
        if (fraction <= 0.0D) {
            return;
        }
        long gain = com.example.immortal_cultivation_mod.attachment.BodyTypeEffects.cultivationGain(killer, Math.max(1L, Math.round(required * fraction)));
        long progress = Math.min(
                required, data.cultivationProgress() + gain);
        ModAttachments.setData(
                killer, data.withCultivationProgress(progress));
        ServerEvents.syncPlayerData(killer);
        killer.displayClientMessage(Component.translatable(
                "message.immortal_cultivation_mod.system.kill_progress",
                gain), true);
    }

    private static double killFraction(
            ServerPlayer killer, LivingEntity target) {
        String targetLevel = null;
        if (target instanceof ServerPlayer player) {
            targetLevel = ModAttachments.getData(player).cultivationLevel();
        } else if (target instanceof CultivationNpc npc) {
            targetLevel = npc.npcRealm();
        }
        if (targetLevel != null) {
            int killerStage = CultivationLevels.getStageIndex(
                    ModAttachments.getData(killer).cultivationLevel());
            int targetStage = CultivationLevels.getStageIndex(targetLevel);
            int killerRealm = CultivationLevels.getRealmIndex(
                    ModAttachments.getData(killer).cultivationLevel());
            int targetRealm = CultivationLevels.getRealmIndex(targetLevel);
            if (targetRealm > killerRealm) {
                return 0.08D;
            }
            return switch (targetStage - killerStage) {
                case 2 -> 0.04D;
                case 1 -> 0.02D;
                case 0 -> 0.01D;
                case -1 -> 0.0025D;
                default -> 0.0005D;
            };
        }
        if (target.getType().getCategory() != MobCategory.MONSTER) {
            return 0.0D;
        }
        return 0.0005D + Math.min(
                0.0015D, target.getMaxHealth() / 100_000.0D);
    }

    private static boolean isRepeatedPlayerKill(
            ServerPlayer killer, ServerPlayer victim) {
        CompoundTag state = OuterSystemData.read(killer);
        long now = System.currentTimeMillis();
        boolean repeated = state.hasUUID("LastPlayerVictim")
                && victim.getUUID().equals(state.getUUID("LastPlayerVictim"))
                && now - state.getLong("LastPlayerKillAt")
                        < REPEAT_PLAYER_KILL_MS;
        state.putUUID("LastPlayerVictim", victim.getUUID());
        state.putLong("LastPlayerKillAt", now);
        OuterSystemData.save(killer, state);
        return repeated;
    }

    private static void ensureQuest(ServerPlayer player) {
        CompoundTag state = OuterSystemData.read(player);
        CompoundTag quest = state.getCompound(OuterSystemData.QUEST).copy();
        if (quest.getBoolean(ACTIVE)) {
            // Upgrade pending quests too; snapshot eligibility so preview and claim agree.
            if (!quest.contains("RewardRealm")) {
                quest.putString("RewardRealm", ModAttachments.getData(player).cultivationLevel());
                state.put(OuterSystemData.QUEST, quest);
                OuterSystemData.save(player, state);
            }
            return;
        }
        if (System.currentTimeMillis() < quest.getLong(NEXT_AT)) return;
        quest = generateQuest(player);
        state.put(OuterSystemData.QUEST, quest);
        OuterSystemData.save(player, state);
    }

    private static CompoundTag generateQuest(ServerPlayer player) {
        CompoundTag quest = new CompoundTag();
        RandomSource random = player.getRandom();
        ServerLevel level = player.serverLevel();
        BlockPos target = null;
        String targetName = "";
        String kind = "biome";

        if (random.nextFloat() < 0.35F) {
            List<StructureTarget> structures = List.of(
                    new StructureTarget(
                            StructureTags.ON_WOODLAND_EXPLORER_MAPS,
                            "screen.immortal_cultivation_mod.system.destination.woodland"),
                    new StructureTarget(
                            StructureTags.ON_OCEAN_EXPLORER_MAPS,
                            "screen.immortal_cultivation_mod.system.destination.ocean"),
                    new StructureTarget(
                            StructureTags.VILLAGE,
                            "screen.immortal_cultivation_mod.system.destination.village"),
                    new StructureTarget(
                            StructureTags.MINESHAFT,
                            "screen.immortal_cultivation_mod.system.destination.mineshaft"),
                    new StructureTarget(
                            StructureTags.RUINED_PORTAL,
                            "screen.immortal_cultivation_mod.system.destination.ruined_portal"));
            StructureTarget selected =
                    structures.get(random.nextInt(structures.size()));
            target = level.findNearestMapStructure(
                    selected.tag(), player.blockPosition(), 256, false);
            if (target != null) {
                targetName = selected.name();
                kind = "structure";
            }
        }

        if (target == null) {
            double distance = 500.0D + random.nextDouble() * 9_000.0D;
            double angle = random.nextDouble() * Math.PI * 2.0D;
            int x = player.getBlockX()
                    + Mth.floor(Math.cos(angle) * distance);
            int z = player.getBlockZ()
                    + Mth.floor(Math.sin(angle) * distance);
            int y = level.getSeaLevel();
            target = new BlockPos(x, y, z);
            targetName = level.getChunkSource().getGenerator()
                    .getBiomeSource()
                    .getNoiseBiome(
                            QuartPos.fromBlock(x),
                            QuartPos.fromBlock(y),
                            QuartPos.fromBlock(z),
                            level.getChunkSource().randomState().sampler())
                    .unwrapKey()
                    .map(key -> "biome."
                            + key.location().getNamespace()
                            + "."
                            + key.location().getPath())
                    .orElse(
                            "screen.immortal_cultivation_mod.system.destination.unknown");
        }

        int distance = (int) Math.round(Math.sqrt(
                player.blockPosition().distSqr(
                        new BlockPos(
                                target.getX(),
                                player.blockPosition().getY(),
                                target.getZ()))));
        int tier = tierForDistance(distance);
        quest.putBoolean(ACTIVE, true);
        quest.putBoolean(COMPLETED, false);
        quest.putInt(TARGET_X, target.getX());
        quest.putInt(TARGET_Y, target.getY());
        quest.putInt(TARGET_Z, target.getZ());
        quest.putString(TARGET_NAME, targetName);
        quest.putString(KIND, kind);
        quest.putInt(TIER, tier);
        quest.putInt("InitialDistance", distance);
        quest.putString("RewardRealm", ModAttachments.getData(player).cultivationLevel());
        quest.putLong(SEED, random.nextLong());
        quest.putString(DIMENSION, level.dimension().location().toString());
        return quest;
    }

    private static void checkQuestCompletion(ServerPlayer player) {
        CompoundTag state = OuterSystemData.read(player);
        CompoundTag quest = state.getCompound(OuterSystemData.QUEST).copy();
        if (!quest.getBoolean(ACTIVE)
                || quest.getBoolean(COMPLETED)
                || !player.level().dimension().location().toString()
                        .equals(quest.getString(DIMENSION))) {
            return;
        }
        double dx = player.getX() - quest.getInt(TARGET_X);
        double dz = player.getZ() - quest.getInt(TARGET_Z);
        if (dx * dx + dz * dz > 48.0D * 48.0D) {
            return;
        }
        quest.putBoolean(COMPLETED, true);
        state.put(OuterSystemData.QUEST, quest);
        OuterSystemData.save(player, state);
        player.displayClientMessage(Component.translatable(
                "message.immortal_cultivation_mod.system.quest_complete"), true);
    }

    private static int tierForDistance(int distance) {
        if (distance >= 8_000) {
            return 5;
        }
        if (distance >= 5_000) {
            return 4;
        }
        if (distance >= 2_500) {
            return 3;
        }
        if (distance >= 1_000) {
            return 2;
        }
        return 1;
    }

    static List<ItemStack> rewards(
            String realm, int tier, long seed) {
        RandomSource random = RandomSource.create(seed);
        List<ItemStack> rewards = new ArrayList<>();
        var stones = com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_STONE.get();
        var medium = com.example.immortal_cultivation_mod.magicritual.item.ModItems.MEDIUM_SPIRIT_STONE.get();
        var high = com.example.immortal_cultivation_mod.magicritual.item.ModItems.HIGH_SPIRIT_STONE.get();
        switch (Mth.clamp(tier, 1, 5)) {
            case 1 -> {
                rewards.add(new ItemStack(stones, 16 + random.nextInt(17)));
                rewards.add(new ItemStack(ModItems.LOW_QI_RECOVERY_PILL.get(), 4 + random.nextInt(5)));
                rewards.add(new ItemStack(ModItems.COPPER_COIN.get(), 64));
            }
            case 2 -> {
                rewards.add(new ItemStack(stones, 48 + random.nextInt(17)));
                rewards.add(new ItemStack(ModItems.LOW_QI_RECOVERY_PILL.get(), 8));
                rewards.add(eligibleScroll(realm, random));
            }
            case 3 -> {
                rewards.add(new ItemStack(medium, 8 + random.nextInt(5)));
                rewards.add(new ItemStack(ModItems.MIDDLE_QI_RECOVERY_PILL.get(), 6 + random.nextInt(5)));
                rewards.add(new ItemStack(ModItems.RESTRICTION_INSIGHT.get(), 2));
            }
            case 4 -> {
                rewards.add(new ItemStack(medium, 24 + random.nextInt(9)));
                rewards.add(eligibleScroll(realm, random));
                rewards.add(new ItemStack(ModItems.LINGJIAO_SCALE.get(), 4 + random.nextInt(3)));
            }
            default -> {
                rewards.add(new ItemStack(high, 8 + random.nextInt(5)));
                rewards.add(eligibleScroll(realm, random));
                rewards.add(new ItemStack(ModItems.RESTRICTION_INSIGHT.get(), 4));
                rewards.add(new ItemStack(random.nextBoolean()
                        ? ModItems.FEIJIAN_YUJIAN.get() : ModItems.JINGTIE_SPEAR_YUJIAN.get()));
                if (random.nextInt(100) == 0) rewards.add(new ItemStack(ModItems.PROTECTIVE_JADE_PENDANT.get()));
            }
        }
        rewards.removeIf(ItemStack::isEmpty);
        return rewards;
    }

    private static ItemStack eligibleScroll(
            String level, RandomSource random) {
        List<Item> eligible = BuiltInRegistries.ITEM.stream()
                .filter(item -> item instanceof ModItems.SpellScrollItem scroll
                        && ModSpells.get(scroll.spellId()) != null
                        && ModSpells.meetsRequirement(
                                level, ModSpells.get(scroll.spellId())))
                .sorted(Comparator.comparing(
                        item -> BuiltInRegistries.ITEM.getKey(item).toString()))
                .toList();
        if (eligible.isEmpty()) {
            return new ItemStack(ModItems.LOW_QI_RECOVERY_PILL.get(), 2);
        }
        return new ItemStack(eligible.get(random.nextInt(eligible.size())));
    }

    private static String rewardSummary(
            String realm, int tier, long seed) {
        return rewards(realm, tier, seed).stream()
                .map(stack -> stack.getHoverName().getString()
                        + " x" + stack.getCount())
                .reduce((left, right) -> left + ", " + right)
                .orElse("-");
    }

    private record StructureTarget(
            net.minecraft.tags.TagKey<net.minecraft.world.level.levelgen.structure.Structure>
                    tag,
            String name) {
    }
}
