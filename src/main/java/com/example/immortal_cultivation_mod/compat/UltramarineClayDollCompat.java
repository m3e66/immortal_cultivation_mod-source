package com.example.immortal_cultivation_mod.compat;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.network.ModPayloads;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Optional interaction support for Ultramarine's male and female clay dolls. */
@EventBusSubscriber(modid = ImmortalCultivationMod.MODID)
public final class UltramarineClayDollCompat {
    private static final ResourceLocation MALE_DOLL =
            ResourceLocation.fromNamespaceAndPath(
                    "ultramarine", "clay_doll_male");
    private static final ResourceLocation FEMALE_DOLL =
            ResourceLocation.fromNamespaceAndPath(
                    "ultramarine", "clay_doll_female");
    private static final ResourceLocation SMALL_SHRINE =
            ResourceLocation.fromNamespaceAndPath(
                    ImmortalCultivationMod.MODID, "small_shrine");
    private static final int SHRINE_DISCOVERY_CHUNK_RADIUS = 2;
    private static final float SCROLL_CHANCE = 0.05F;

    private UltramarineClayDollCompat() {
    }

    public static boolean isClayDoll(BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return MALE_DOLL.equals(id) || FEMALE_DOLL.equals(id);
    }

    /**
     * Returns loaded enchanted dolls inside the requested radius. Invalid stale
     * entries are discarded without force-loading their chunks.
     */
    public static List<BlockPos> enchantedDollsNear(
            ServerLevel level, BlockPos center, int radius) {
        DollData data = DollData.get(level);
        List<BlockPos> result = new ArrayList<>();
        List<Long> stale = new ArrayList<>();
        long radiusSquared = (long) radius * radius;
        for (long packed : data.dolls.keySet()) {
            BlockPos pos = BlockPos.of(packed);
            if (!level.isLoaded(pos)) {
                continue;
            }
            if (!isClayDoll(level.getBlockState(pos))) {
                stale.add(packed);
                continue;
            }
            long dx = (long) pos.getX() - center.getX();
            long dy = (long) pos.getY() - center.getY();
            long dz = (long) pos.getZ() - center.getZ();
            if (dx * dx + dy * dy + dz * dz <= radiusSquared) {
                result.add(pos.immutable());
            }
        }
        if (!stale.isEmpty()) {
            stale.forEach(data.dolls::remove);
            data.setDirty();
        }
        return result;
    }

    /**
     * Structure templates cannot store this enchantment because Ultramarine's
     * dolls have no block entity. Once a generated shrine is fully loaded,
     * record every original doll in the same saved data used by hand-enchanted
     * dolls. The processed-shrine marker prevents a broken or player-replaced
     * doll from being enchanted again later.
     */
    @SubscribeEvent
    public static void onShrinePlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player
                && player.tickCount % 20 == 0) {
            discoverGeneratedShrineDolls(
                    player.serverLevel(), player.blockPosition());
        }
    }

    private static void discoverGeneratedShrineDolls(
            ServerLevel level, BlockPos center) {
        Structure shrine = level.registryAccess()
                .registryOrThrow(Registries.STRUCTURE)
                .get(SMALL_SHRINE);
        if (shrine == null) {
            return;
        }

        DollData data = DollData.get(level);
        ChunkPos centerChunk = new ChunkPos(center);
        Set<Long> visitedStarts = new HashSet<>();
        for (int chunkX = centerChunk.x - SHRINE_DISCOVERY_CHUNK_RADIUS;
             chunkX <= centerChunk.x + SHRINE_DISCOVERY_CHUNK_RADIUS;
             chunkX++) {
            for (int chunkZ = centerChunk.z - SHRINE_DISCOVERY_CHUNK_RADIUS;
                 chunkZ <= centerChunk.z + SHRINE_DISCOVERY_CHUNK_RADIUS;
                 chunkZ++) {
                if (!level.hasChunk(chunkX, chunkZ)) {
                    continue;
                }
                for (StructureStart start : level.structureManager()
                        .startsForStructure(new ChunkPos(chunkX, chunkZ),
                                candidate -> candidate == shrine)) {
                    long shrineKey = start.getChunkPos().toLong();
                    if (!visitedStarts.add(shrineKey)
                            || data.hasProcessedShrine(shrineKey)
                            || !allChunksLoaded(level,
                            start.getBoundingBox())) {
                        continue;
                    }
                    enchantDollsInStructure(level, start, data);
                    data.markShrineProcessed(shrineKey);
                }
            }
        }
    }

    private static boolean allChunksLoaded(
            ServerLevel level, BoundingBox bounds) {
        int minChunkX = SectionPos.blockToSectionCoord(bounds.minX());
        int maxChunkX = SectionPos.blockToSectionCoord(bounds.maxX());
        int minChunkZ = SectionPos.blockToSectionCoord(bounds.minZ());
        int maxChunkZ = SectionPos.blockToSectionCoord(bounds.maxZ());
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                if (!level.hasChunk(chunkX, chunkZ)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void enchantDollsInStructure(
            ServerLevel level, StructureStart start, DollData data) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (StructurePiece piece : start.getPieces()) {
            BoundingBox bounds = piece.getBoundingBox();
            int minY = Math.max(bounds.minY(), level.getMinBuildHeight());
            int maxY = Math.min(bounds.maxY(),
                    level.getMaxBuildHeight() - 1);
            for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                        cursor.set(x, y, z);
                        if (isClayDoll(level.getBlockState(cursor))) {
                            data.enchant(cursor.immutable());
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity() instanceof ServerPlayer player)
                || !isClayDoll(level.getBlockState(event.getPos()))) {
            return;
        }

        BlockPos pos = event.getPos();
        DollData data = DollData.get(level);
        if (player.isShiftKeyDown() && ModPayloads.isDebugEyeEnabled(player)) {
            if (data.enchant(pos)) {
                level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE,
                        SoundSource.BLOCKS, 0.8F, 1.15F);
                player.displayClientMessage(Component.translatable(
                        "message." + ImmortalCultivationMod.MODID
                                + ".clay_doll.enchanted"), true);
            } else {
                String suffix = data.hasAttempted(pos)
                        ? ".clay_doll.already_used"
                        : ".clay_doll.already_enchanted";
                player.displayClientMessage(Component.translatable(
                        "message." + ImmortalCultivationMod.MODID + suffix), true);
            }
            consume(event);
            return;
        }

        if (!data.isEnchanted(pos)) {
            return;
        }
        if (!data.beginAttempt(pos)) {
            player.displayClientMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID
                            + ".clay_doll.already_used"), true);
            consume(event);
            return;
        }

        if (player.getRandom().nextFloat() < SCROLL_CHANCE) {
            ItemStack reward = randomSpellScroll(player);
            if (!reward.isEmpty()) {
                if (!player.getInventory().add(reward)) {
                    player.drop(reward, false);
                }
                level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP,
                        SoundSource.PLAYERS, 0.7F, 1.35F);
                player.displayClientMessage(Component.translatable(
                        "message." + ImmortalCultivationMod.MODID
                                + ".clay_doll.scroll_found",
                        reward.getHoverName()), true);
            } else {
                showFailedAttempt(player, level, pos);
            }
        } else {
            showFailedAttempt(player, level, pos);
        }
        consume(event);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!event.isCanceled()
                && event.getLevel() instanceof ServerLevel level
                && isClayDoll(event.getPlacedBlock())) {
            // A placed doll item is always normal, including one recovered by
            // breaking a previously enchanted doll.
            DollData.get(level).remove(event.getPos());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBroken(BlockEvent.BreakEvent event) {
        if (!event.isCanceled()
                && event.getLevel() instanceof ServerLevel level
                && isClayDoll(event.getState())) {
            DollData.get(level).remove(event.getPos());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        DollData data = DollData.get(level);
        for (BlockPos pos : event.getAffectedBlocks()) {
            if (isClayDoll(level.getBlockState(pos))) {
                data.remove(pos);
            }
        }
    }

    private static ItemStack randomSpellScroll(ServerPlayer player) {
        List<Item> scrolls = BuiltInRegistries.ITEM.stream()
                .filter(item -> item instanceof ModItems.SpellScrollItem)
                .toList();
        if (scrolls.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(scrolls.get(
                player.getRandom().nextInt(scrolls.size())));
    }

    private static void showFailedAttempt(
            ServerPlayer player, ServerLevel level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.DECORATED_POT_INSERT_FAIL,
                SoundSource.BLOCKS, 0.55F, 0.85F);
        player.displayClientMessage(Component.translatable(
                "message." + ImmortalCultivationMod.MODID
                        + ".clay_doll.no_scroll"), true);
    }

    private static void consume(PlayerInteractEvent.RightClickBlock event) {
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    private record DollState(boolean attempted) {
    }

    private static final class DollData extends SavedData {
        private static final String DATA_NAME =
                "immortal_cultivation_ultramarine_clay_dolls";
        private static final SavedData.Factory<DollData> FACTORY =
                new SavedData.Factory<>(DollData::new, DollData::load);

        private final Map<Long, DollState> dolls = new HashMap<>();
        private final Set<Long> processedShrines = new HashSet<>();

        private static DollData get(ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
        }

        private static DollData load(
                CompoundTag tag, HolderLookup.Provider registries) {
            DollData data = new DollData();
            ListTag entries = tag.getList("Dolls", Tag.TAG_COMPOUND);
            for (int index = 0; index < entries.size(); index++) {
                CompoundTag entry = entries.getCompound(index);
                if (entry.contains("Pos", Tag.TAG_LONG)) {
                    data.dolls.put(entry.getLong("Pos"),
                            new DollState(entry.getBoolean("Attempted")));
                }
            }
            for (long shrine : tag.getLongArray("ProcessedShrines")) {
                data.processedShrines.add(shrine);
            }
            return data;
        }

        private boolean enchant(BlockPos pos) {
            if (dolls.putIfAbsent(pos.asLong(), new DollState(false)) == null) {
                setDirty();
                return true;
            }
            return false;
        }

        private boolean isEnchanted(BlockPos pos) {
            return dolls.containsKey(pos.asLong());
        }

        private boolean hasAttempted(BlockPos pos) {
            DollState state = dolls.get(pos.asLong());
            return state != null && state.attempted();
        }

        private boolean beginAttempt(BlockPos pos) {
            long key = pos.asLong();
            DollState state = dolls.get(key);
            if (state == null || state.attempted()) {
                return false;
            }
            dolls.put(key, new DollState(true));
            setDirty();
            return true;
        }

        private boolean hasProcessedShrine(long shrineKey) {
            return processedShrines.contains(shrineKey);
        }

        private void markShrineProcessed(long shrineKey) {
            if (processedShrines.add(shrineKey)) {
                setDirty();
            }
        }

        private void remove(BlockPos pos) {
            if (dolls.remove(pos.asLong()) != null) {
                setDirty();
            }
        }

        @Override
        public CompoundTag save(
                CompoundTag tag, HolderLookup.Provider registries) {
            ListTag entries = new ListTag();
            dolls.forEach((position, state) -> {
                CompoundTag entry = new CompoundTag();
                entry.putLong("Pos", position);
                entry.putBoolean("Attempted", state.attempted());
                entries.add(entry);
            });
            tag.put("Dolls", entries);
            tag.putLongArray("ProcessedShrines", processedShrines.stream()
                    .mapToLong(Long::longValue)
                    .toArray());
            return tag;
        }
    }
}
