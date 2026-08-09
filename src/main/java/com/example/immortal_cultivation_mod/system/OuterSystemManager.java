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

    @SubscribeEvent
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
        UUID entityId = MANIFESTS.remove(player.getUUID());
        if (entityId != null && player.level() instanceof ServerLevel level) {
            Entity entity = level.getEntity(entityId);
            if (entity != null) {
                entity.discard();
            }
        }
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
                player, quest.getInt(TIER), quest.getLong(SEED));
        for (ItemStack reward : rewards) {
            if (!player.getInventory().add(reward.copy())) {
                player.drop(reward.copy(), false);
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
        SystemShop.buy(player, itemId);
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
        LifeSimulationSystem.handleAction(player, action);
        refreshOpenScreen(player);
    }

    public static void onBodyTypeChanged(ServerPlayer player) {
        ensureBinding(player);
    }

    private static void ensureBinding(ServerPlayer player) {
        CompoundTag state = OuterSystemData.read(player);
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
            OuterSystemType type = rerollSystem(
                    player.getRandom(),
                    state.getString(OuterSystemData.PREVIOUS_TYPE));
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
        PacketDistributor.sendToPlayer(
                player,
                new OuterSystemPayloads.ClientboundOpen(
                        createClientState(player, includeShopCatalog)));
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
                                player,
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
        long gain = Math.max(1L, Math.round(required * fraction));
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
        if (quest.getBoolean(ACTIVE)
                || System.currentTimeMillis() < quest.getLong(NEXT_AT)) {
            return;
        }
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

    private static List<ItemStack> rewards(
            ServerPlayer player, int tier, long seed) {
        RandomSource random = RandomSource.create(seed);
        List<ItemStack> rewards = new ArrayList<>();
        switch (Mth.clamp(tier, 1, 5)) {
            case 1 -> {
                rewards.add(new ItemStack(
                        ModItems.COPPER_COIN.get(), 12 + random.nextInt(13)));
                rewards.add(random.nextBoolean()
                        ? new ItemStack(Items.BREAD, 4 + random.nextInt(5))
                        : new ItemStack(
                                ModItems.LOW_QI_RECOVERY_PILL.get(),
                                1 + random.nextInt(2)));
            }
            case 2 -> {
                rewards.add(new ItemStack(
                        com.example.immortal_cultivation_mod.magicritual.item
                                .ModItems.SPIRIT_STONE.get(),
                        8 + random.nextInt(9)));
                rewards.add(random.nextBoolean()
                        ? eligibleScroll(player, random)
                        : new ItemStack(
                                ModItems.LOW_SWORD_GRINDSTONE.get(),
                                1 + random.nextInt(2)));
            }
            case 3 -> {
                rewards.add(new ItemStack(
                        ModItems.MIDDLE_QI_RECOVERY_PILL.get(),
                        2 + random.nextInt(3)));
                rewards.add(random.nextBoolean()
                        ? new ItemStack(ModItems.RESTRICTION_INSIGHT.get())
                        : new ItemStack(
                                ModBlocks.MOXUAN_IRON.get().asItem(),
                                2 + random.nextInt(3)));
            }
            case 4 -> {
                rewards.add(eligibleScroll(player, random));
                rewards.add(new ItemStack(
                        com.example.immortal_cultivation_mod.magicritual.item
                                .ModItems.MEDIUM_SPIRIT_STONE.get(),
                        12 + random.nextInt(13)));
                rewards.add(new ItemStack(
                        ModItems.LINGJIAO_SCALE.get(),
                        1 + random.nextInt(2)));
            }
            default -> {
                rewards.add(eligibleScroll(player, random));
                rewards.add(new ItemStack(
                        com.example.immortal_cultivation_mod.magicritual.item
                                .ModItems.HIGH_SPIRIT_STONE.get(),
                        3 + random.nextInt(4)));
                rewards.add(random.nextInt(100) == 0
                        ? new ItemStack(
                                ModItems.PROTECTIVE_JADE_PENDANT.get())
                        : new ItemStack(
                                random.nextBoolean()
                                        ? ModItems.FEIJIAN_YUJIAN.get()
                                        : ModItems.JINGTIE_SPEAR_YUJIAN.get()));
            }
        }
        rewards.removeIf(ItemStack::isEmpty);
        return rewards;
    }

    private static ItemStack eligibleScroll(
            ServerPlayer player, RandomSource random) {
        String level = ModAttachments.getData(player).cultivationLevel();
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
            ServerPlayer player, int tier, long seed) {
        return rewards(player, tier, seed).stream()
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
