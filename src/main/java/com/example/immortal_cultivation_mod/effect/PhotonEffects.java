package com.example.immortal_cultivation_mod.effect;

import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.PacketDistributor;

public final class PhotonEffects {
    private static final String FIREBALL_FX = "immortal_cultivation_mod:fireball";
    private static final String LINGBENG_FX = "immortal_cultivation_mod:lingbeng";
    private static final String BEAM_FX = "immortal_cultivation_mod:beam";
    private static final String BALL_FX = "immortal_cultivation_mod:ball";
    private static final String PUDDLE_FX = "immortal_cultivation_mod:puddle";
    private static final String MEDITATING_FX = "immortal_cultivation_mod:meditating";
    private static final String WEIYA_FX = "immortal_cultivation_mod:rage3";
    private static final String WATER_SHIELD_FX_PREFIX = "immortal_cultivation_mod:water";
    private static final int BEAM_DURATION_TICKS = 20 * 10;
    private static final int LINGBENG_AURA_TICKS = 6;
    private static final Map<UUID, BeamRemoval> BEAM_REMOVALS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LINGBENG_AURAS = new ConcurrentHashMap<>();
    private static final Map<BlockEffectLocation, Long> LINGBENG_BLOCK_REMOVALS = new ConcurrentHashMap<>();

    private PhotonEffects() {
    }

    public static void fireballProjectile(Entity projectile) {
        if (projectile.level() instanceof ServerLevel serverLevel) {
            playEntityEffect(serverLevel, projectile);
        }
    }

    public static void lightBeamProjectile(Entity projectile) {
        if (projectile.level() instanceof ServerLevel serverLevel) {
            playLightBeamEntityEffect(serverLevel, projectile);
        }
    }

    public static void lingbengExplosion(ServerLevel level, double x, double y, double z) {
        playSimpleBlockEffect(level, LINGBENG_FX, x, y, z);
        LINGBENG_BLOCK_REMOVALS.put(new BlockEffectLocation(level.dimension(), x, y, z), level.getGameTime() + LINGBENG_AURA_TICKS);
    }

    public static void lingbengStart(ServerPlayer player) {
        if (!ModList.get().isLoaded("photon")) {
            return;
        }

        LINGBENG_AURAS.put(player.getUUID(), player.level().getGameTime() + LINGBENG_AURA_TICKS);
        String command = String.format(Locale.ROOT, "photon fx %s entity @s", LINGBENG_FX);
        runPhotonCommand(player.serverLevel(), player.createCommandSourceStack().withPermission(4).withSuppressedOutput(), command);
    }

    public static void lingbengStop(ServerPlayer player) {
        if (!ModList.get().isLoaded("photon")) {
            return;
        }

        LINGBENG_AURAS.remove(player.getUUID());
        for (ServerPlayer viewer : player.serverLevel().players()) {
            sendRemoveEntityEffectToPlayer(viewer, LINGBENG_FX, List.of(player));
        }
    }

    public static void puddle(ServerLevel level, double x, double y, double z) {
        playSimpleBlockEffect(level, PUDDLE_FX, x, y, z);
    }

    public static void removePuddle(ServerLevel level, BlockPos pos) {
        removeBlockEffect(level, pos);
    }

    public static void beamSpell(ServerLevel level, ServerPlayer caster) {
        beamSpell(level, caster, BEAM_DURATION_TICKS);
    }

    public static void beamSpell(ServerLevel level, ServerPlayer caster, int durationTicks) {
        if (!ModList.get().isLoaded("photon")) {
            return;
        }

        List<Entity> entities = level.getEntities(caster, new AABB(caster.blockPosition()).inflate(128.0D), entity -> true);
        entities.add(caster);
        sendEntityEffectToPlayer(caster, BEAM_FX, entities);
        BEAM_REMOVALS.put(caster.getUUID(), new BeamRemoval(level.getGameTime() + durationTicks, entities));
    }

    public static void meditatingStart(ServerLevel level, ServerPlayer player) {
        if (!ModList.get().isLoaded("photon")) {
            return;
        }

        String command = String.format(Locale.ROOT, "photon fx %s entity @s", MEDITATING_FX);
        runPhotonCommand(level, player.createCommandSourceStack().withPermission(4).withSuppressedOutput(), command);
    }

    public static void waterShield(ServerPlayer player, int stack) {
        if (!ModList.get().isLoaded("photon")) {
            return;
        }

        int clamped = Math.max(1, Math.min(3, stack));
        removeWaterShield(player);
        String fx = WATER_SHIELD_FX_PREFIX + clamped;
        for (ServerPlayer viewer : player.serverLevel().players()) {
            sendEntityEffectToPlayer(viewer, fx, List.of(player));
        }
    }

    public static void weiyaStart(ServerPlayer player) {
        if (!ModList.get().isLoaded("photon")) {
            return;
        }

        for (ServerPlayer viewer : player.serverLevel().players()) {
            sendEntityEffectToPlayer(viewer, WEIYA_FX, List.of(player));
        }
    }

    public static void weiyaStop(ServerPlayer player) {
        if (!ModList.get().isLoaded("photon")) {
            return;
        }

        for (ServerPlayer viewer : player.serverLevel().players()) {
            sendRemoveEntityEffectToPlayer(viewer, WEIYA_FX, List.of(player));
        }
    }

    public static void removeWaterShield(ServerPlayer player) {
        if (!ModList.get().isLoaded("photon")) {
            return;
        }

        for (ServerPlayer viewer : player.serverLevel().players()) {
            for (int stack = 1; stack <= 3; stack++) {
                sendRemoveEntityEffectToPlayer(viewer, WATER_SHIELD_FX_PREFIX + stack, List.of(player));
            }
        }
    }

    public static void meditatingStop(ServerPlayer player) {
        if (!ModList.get().isLoaded("photon")) {
            return;
        }

        for (ServerPlayer viewer : player.serverLevel().players()) {
            sendRemoveEntityEffectToPlayer(viewer, MEDITATING_FX, List.of(player));
        }
    }

    public static void tick(ServerPlayer player) {
        Long lingbengAuraEnd = LINGBENG_AURAS.get(player.getUUID());
        if (lingbengAuraEnd != null && (!player.hasEffect(ModEffects.LINGBENG) || player.level().getGameTime() >= lingbengAuraEnd)) {
            lingbengStop(player);
        }
        tickLingbengBlockRemovals(player);

        BeamRemoval removal = BEAM_REMOVALS.get(player.getUUID());
        if (removal == null || player.level().getGameTime() < removal.removeAtGameTime()) {
            return;
        }

        BEAM_REMOVALS.remove(player.getUUID());
        sendRemoveEntityEffectToPlayer(player, BEAM_FX, removal.entities());
    }

    private static void tickLingbengBlockRemovals(ServerPlayer player) {
        long gameTime = player.level().getGameTime();
        ResourceKey<Level> dimension = player.level().dimension();
        LINGBENG_BLOCK_REMOVALS.entrySet().removeIf(entry -> {
            if (gameTime < entry.getValue() || !entry.getKey().dimension().equals(dimension)) {
                return false;
            }
            removeBlockEffect(player.serverLevel(), entry.getKey().x(), entry.getKey().y(), entry.getKey().z());
            return true;
        });
    }

    private static void playBlockEffect(ServerLevel level, String fx, double x, double y, double z, double scale) {
        if (!ModList.get().isLoaded("photon")) {
            return;
        }

        String command = String.format(Locale.ROOT,
                "photon fx %s block %s %s %s %s 0 true true false",
                fx, vec(x, y, z), vec(0, 0, 0), vec(0, 0, 0), vec(scale, scale, scale));

        var source = level.getServer().createCommandSourceStack()
                .withPermission(4)
                .withPosition(new net.minecraft.world.phys.Vec3(x, y, z))
                .withSuppressedOutput();
        runPhotonCommand(level, source, command);
    }

    private static void playSimpleBlockEffect(ServerLevel level, String fx, double x, double y, double z) {
        if (!ModList.get().isLoaded("photon")) {
            return;
        }

        String command = String.format(Locale.ROOT, "photon fx %s block ~ ~ ~", fx);
        var source = level.getServer().createCommandSourceStack()
                .withPermission(4)
                .withPosition(new net.minecraft.world.phys.Vec3(x, y, z))
                .withSuppressedOutput();
        runPhotonCommand(level, source, command);
    }

    private static void playEntityEffect(ServerLevel level, Entity entity) {
        if (!ModList.get().isLoaded("photon")) {
            return;
        }

        String command = String.format(Locale.ROOT,
                "photon fx %s entity @e[type=immortal_cultivation_mod:fireball_projectile,limit=1,sort=nearest]",
                FIREBALL_FX);

        var source = entity.createCommandSourceStack()
                .withPermission(4)
                .withPosition(entity.position())
                .withSuppressedOutput();
        runPhotonCommand(level, source, command);
    }

    private static void playLightBeamEntityEffect(ServerLevel level, Entity entity) {
        if (!ModList.get().isLoaded("photon")) {
            return;
        }

        String command = String.format(Locale.ROOT,
                "photon fx %s entity @e[type=immortal_cultivation_mod:light_beam_projectile,limit=1,sort=nearest]",
                BALL_FX);

        var source = entity.createCommandSourceStack()
                .withPermission(4)
                .withPosition(entity.position())
                .withSuppressedOutput();
        runPhotonCommand(level, source, command);
    }

    private static void runPhotonCommand(ServerLevel level, net.minecraft.commands.CommandSourceStack source, String command) {
        if (!ModList.get().isLoaded("photon")) {
            return;
        }

        try {
            level.getServer().getCommands().performPrefixedCommand(source, command);
        } catch (RuntimeException ignored) {
        }
    }

    private static void sendEntityEffectToPlayer(ServerPlayer player, String fx, List<Entity> entities) {
        try {
            Object command = Class.forName("com.lowdragmc.photon.command.EntityEffectCommand").getConstructor().newInstance();
            command.getClass().getMethod("setLocation", ResourceLocation.class).invoke(command, ResourceLocation.parse(fx));
            command.getClass().getMethod("setEntities", List.class).invoke(command, entities);
            command.getClass().getMethod("setAllowMulti", boolean.class).invoke(command, false);
            PacketDistributor.sendToPlayer(player, (CustomPacketPayload) command);
        } catch (ReflectiveOperationException | ClassCastException ignored) {
        }
    }

    private static void sendRemoveEntityEffectToPlayer(ServerPlayer player, String fx, List<Entity> entities) {
        try {
            Object command = Class.forName("com.lowdragmc.photon.command.RemoveEntityEffectCommand").getConstructor().newInstance();
            command.getClass().getMethod("setEntities", List.class).invoke(command, entities);
            command.getClass().getMethod("setForce", boolean.class).invoke(command, true);
            command.getClass().getMethod("setLocation", ResourceLocation.class).invoke(command, ResourceLocation.parse(fx));
            PacketDistributor.sendToPlayer(player, (CustomPacketPayload) command);
        } catch (ReflectiveOperationException | ClassCastException ignored) {
        }
    }

    private static void removeBlockEffect(ServerLevel level, BlockPos pos) {
        removeBlockEffect(level, pos.getX(), pos.getY(), pos.getZ());
    }

    private static void removeBlockEffect(ServerLevel level, double x, double y, double z) {
        if (!ModList.get().isLoaded("photon")) {
            return;
        }

        String command = "photon fx remove block ~ ~ ~";
        var source = level.getServer().createCommandSourceStack()
                .withPermission(4)
                .withPosition(new net.minecraft.world.phys.Vec3(x, y, z))
                .withSuppressedOutput();
        runPhotonCommand(level, source, command);
    }

    private static String vec(double x, double y, double z) {
        return String.format(Locale.ROOT, "%.3f %.3f %.3f", x, y, z);
    }

    private record BeamRemoval(long removeAtGameTime, List<Entity> entities) {
    }

    private record BlockEffectLocation(ResourceKey<Level> dimension, double x, double y, double z) {
    }
}
