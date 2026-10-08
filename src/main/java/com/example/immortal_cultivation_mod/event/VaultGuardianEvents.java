package com.example.immortal_cultivation_mod.event;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.entity.VaultGuardianEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Treasure and vault interactions that awaken nearby dormant constructs. */
@EventBusSubscriber(modid = ImmortalCultivationMod.MODID)
public final class VaultGuardianEvents {
    private static final double WAKE_RADIUS = 24.0D;

    private VaultGuardianEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onGuardianLoaded(EntityJoinLevelEvent event) {
        // Entity joins can run before the chunk reaches FULL. Saved links can
        // repair state immediately; new links must wait for the normal AI tick
        // so structure lookups cannot deadlock chunk loading.
        if (!event.isCanceled() && event.getLevel() instanceof ServerLevel level
                && event.getEntity() instanceof VaultGuardianEntity guardian
                && !guardian.getSmallSectInstanceKey().isEmpty()) {
            SmallSectProtection.registerGuardian(level, guardian);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player
                && player.tickCount % 20 == 0) {
            SmallSectProtection.ensureGuardianNear(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (event.isCanceled()
                || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (event instanceof BlockEvent.EntityMultiPlaceEvent multiPlace) {
            multiPlace.getReplacedBlockSnapshots().forEach(
                    snapshot -> SmallSectProtection.recordPlacedBlock(
                            level, snapshot.getPos()));
            return;
        }
        SmallSectProtection.recordPlacedBlock(level, event.getPos());
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity() instanceof ServerPlayer player)
                || !isProtectedTreasure(level, event.getPos(),
                        level.getBlockState(event.getPos()))) {
            return;
        }
        SmallSectProtection.awakenForInteraction(level, event.getPos(), player);
        awakenNearby(level, event.getPos(), player);
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (SmallSectProtection.preventBreaking(
                level, event.getPos(), player)) {
            event.setCanceled(true);
            return;
        }
        // A guardian close enough to hear a block being struck treats it as a
        // vault-wall attack, even if the block is not eventually broken.
        awakenNearby(level, event.getPos(), player);
    }

    @SubscribeEvent
    public static void onBlockBroken(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        if (SmallSectProtection.preventBreaking(
                level, event.getPos(), player)) {
            event.setCanceled(true);
            return;
        }
        awakenNearby(level, event.getPos(), player);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        event.getAffectedBlocks().removeIf(
                pos -> SmallSectProtection.isLocked(level, pos));
    }

    public static int awakenNearby(
            ServerLevel level, BlockPos center, ServerPlayer intruder) {
        AABB area = new AABB(center).inflate(WAKE_RADIUS, 10.0D, WAKE_RADIUS);
        int awakened = 0;
        for (VaultGuardianEntity guardian : level.getEntitiesOfClass(
                VaultGuardianEntity.class, area, VaultGuardianEntity::isAlive)) {
            guardian.activateAgainst(intruder);
            awakened++;
        }
        return awakened;
    }

    private static boolean isProtectedTreasure(
            ServerLevel level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof Container) {
            return true;
        }
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (id == null) {
            return false;
        }
        String path = id.getPath();
        return path.contains("chest")
                || path.contains("cabinet")
                || path.contains("vault")
                || path.contains("treasure")
                || path.contains("relic")
                || path.contains("pedestal")
                || path.contains("seal")
                || path.contains("stele")
                || path.contains("ding");
    }
}
