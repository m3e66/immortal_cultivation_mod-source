package com.example.immortal_cultivation_mod.effect;

import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.nio.file.Files;
import java.nio.file.Path;

import com.example.immortal_cultivation_mod.entity.IceFxAnchorEntity;
import com.example.immortal_cultivation_mod.entity.ModEntities;
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
import net.minecraft.world.phys.Vec3;
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
    private static final String ICE_1_FX = "immortal_cultivation_mod:ice1";
    private static final String ICE_2_FX = "immortal_cultivation_mod:ice2";
    private static final String FLYING_SWORD_FX = "immortal_cultivation_mod:flying_sword";
    private static final int BEAM_DURATION_TICKS = 20 * 10;
    private static final int LINGBENG_AURA_TICKS = 6;
    private static final double WATER_SHIELD_SPIN_RADIANS_PER_TICK = Math.PI / 10.0D;
    private static final Map<UUID, BeamRemoval> BEAM_REMOVALS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LINGBENG_AURAS = new ConcurrentHashMap<>();
    private static final Map<BlockEffectLocation, Long> LINGBENG_BLOCK_REMOVALS = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> FROST_FLIGHT_FIRST_PERSON = new ConcurrentHashMap<>();

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

    public static void flyingSwordProjectile(Entity projectile) {
        if (!ModList.get().isLoaded("photon") || !(projectile.level() instanceof ServerLevel level)) {
            return;
        }

        var source = projectile.createCommandSourceStack()
                .withPermission(4)
                .withPosition(projectile.position())
                .withSuppressedOutput();
        runPhotonCommand(level, source, String.format(Locale.ROOT,
                "photon fx %s entity @e[type=immortal_cultivation_mod:flying_sword,limit=1,sort=nearest]",
                FLYING_SWORD_FX));
    }

    public static void removeFlyingSwordProjectile(Entity projectile) {
        if (!ModList.get().isLoaded("photon") || !(projectile.level() instanceof ServerLevel level)) {
            return;
        }

        for (ServerPlayer viewer : level.players()) {
            sendRemoveEntityEffectToPlayer(viewer, FLYING_SWORD_FX, List.of(projectile));
        }
    }

    public static void liuguangJianyingProjectile(Entity projectile) {
        playFlyingSwordFxOnEntityType(projectile, "liuguang_jianying_projectile");
    }

    public static void removeLiuguangJianyingProjectile(Entity projectile) {
        if (!ModList.get().isLoaded("photon") || !(projectile.level() instanceof ServerLevel level)) {
            return;
        }

        for (ServerPlayer viewer : level.players()) {
            sendRemoveEntityEffectToPlayer(viewer, FLYING_SWORD_FX, List.of(projectile));
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

    public static void iceImpact(ServerLevel level, double x, double y, double z) {
        if (!ModList.get().isLoaded("photon")) {
            return;
        }
        IceFxAnchorEntity anchor = ModEntities.ICE_FX_ANCHOR.get().create(level);
        if (anchor == null) {
            return;
        }
        anchor.moveTo(x, y, z, 0.0F, 0.0F);
        level.addFreshEntity(anchor);
        String command = String.format(Locale.ROOT,
                "photon fx %s entity @e[type=immortal_cultivation_mod:ice_fx_anchor,limit=1,sort=nearest]",
                ICE_1_FX);
        runPhotonCommand(level, anchor.createCommandSourceStack()
                .withPermission(4)
                .withPosition(anchor.position())
                .withSuppressedOutput(), command);
    }

    public static void frostFlightStart(ServerPlayer player) {
        if (!ModList.get().isLoaded("photon")) {
            return;
        }
        sendFrostFlightEffectToViewers(player);
    }

    public static void frostFlightTrail(ServerPlayer player) {
        if (!ModList.get().isLoaded("photon")) {
            return;
        }
        if (player.tickCount % 40 == 0) {
            sendFrostFlightEffectToViewers(player);
        }
    }

    public static void frostFlightStop(ServerPlayer player) {
        if (!ModList.get().isLoaded("photon")) {
            return;
        }

        FROST_FLIGHT_FIRST_PERSON.remove(player.getUUID());
        for (ServerPlayer viewer : player.serverLevel().players()) {
            sendRemoveEntityEffectToPlayer(viewer, ICE_2_FX, List.of(player));
        }
    }

    public static void updateFrostFlightSelfView(ServerPlayer player, boolean firstPerson) {
        FROST_FLIGHT_FIRST_PERSON.put(player.getUUID(), firstPerson);
        if (!ModList.get().isLoaded("photon") || !player.hasEffect(ModEffects.FROST_FLIGHT)) {
            return;
        }
        if (firstPerson) {
            sendRemoveEntityEffectToPlayer(player, ICE_2_FX, List.of(player));
        } else {
            sendEntityEffectToPlayer(player, ICE_2_FX, List.of(player), frostFlightLegOffset());
        }
    }

    private static void sendFrostFlightEffectToViewers(ServerPlayer player) {
        Vec3 legOffset = frostFlightLegOffset();
        for (ServerPlayer viewer : player.serverLevel().players()) {
            if (viewer.getUUID().equals(player.getUUID())
                    && FROST_FLIGHT_FIRST_PERSON.getOrDefault(player.getUUID(), true)) {
                continue;
            }
            sendEntityEffectToPlayer(viewer, ICE_2_FX, List.of(player), legOffset);
        }
    }

    private static Vec3 frostFlightLegOffset() {
        return new Vec3(0.0D, -0.85D, 0.0D);
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
        removeWaterShield(player.serverLevel(), player);
        playWaterShield(player.serverLevel(), player, clamped, true);
    }

    public static void waterShield(ServerLevel level, Entity entity, int stack) {
        if (!ModList.get().isLoaded("photon")) {
            return;
        }

        int clamped = Math.max(1, Math.min(3, stack));
        removeWaterShield(level, entity);
        playWaterShield(level, entity, clamped, false);
    }

    private static void playWaterShield(ServerLevel level, Entity entity, int stack, boolean hideFromEntityPlayer) {
        String fx = WATER_SHIELD_FX_PREFIX + stack;
        double spin = (level.getGameTime() * WATER_SHIELD_SPIN_RADIANS_PER_TICK) % (Math.PI * 2.0D);
        for (ServerPlayer viewer : level.players()) {
            if (hideFromEntityPlayer && viewer.getUUID().equals(entity.getUUID())) {
                continue;
            }
            sendEntityEffectToPlayer(viewer, fx, List.of(entity), new Vec3(0.0D, spin, spin));
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

        removeWaterShield(player.serverLevel(), player);
    }

    public static void removeWaterShield(ServerLevel level, Entity entity) {
        if (!ModList.get().isLoaded("photon")) {
            return;
        }

        for (ServerPlayer viewer : level.players()) {
            for (int stack = 1; stack <= 3; stack++) {
                sendRemoveEntityEffectToPlayer(viewer, WATER_SHIELD_FX_PREFIX + stack, List.of(entity));
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

    private static void playOptionalSimpleBlockEffect(ServerLevel level, String fx, double x, double y, double z) {
        if (!ModList.get().isLoaded("photon") || !fxExists(level, fx)) {
            return;
        }
        playSimpleBlockEffect(level, fx, x, y, z);
    }

    private static void playOptionalBlockEffect(ServerLevel level, String fx, double x, double y, double z, double scale) {
        if (!ModList.get().isLoaded("photon") || !fxExists(level, fx)) {
            return;
        }
        playBlockEffect(level, fx, x, y, z, scale);
    }

    private static boolean fxExists(ServerLevel level, String fx) {
        ResourceLocation id = ResourceLocation.parse(fx);
        ResourceLocation file = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "fx/" + id.getPath() + ".fx");
        if (level.getServer().getResourceManager().getResource(file).isPresent()) {
            return true;
        }
        Path devResource = Path.of("src", "main", "resources", "assets", id.getNamespace(), "fx", id.getPath() + ".fx");
        return Files.exists(devResource);
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

    private static void playFlyingSwordFxOnEntityType(Entity entity, String entityTypePath) {
        if (!ModList.get().isLoaded("photon") || !(entity.level() instanceof ServerLevel level)) {
            return;
        }

        String command = String.format(Locale.ROOT,
                "photon fx %s entity @e[type=immortal_cultivation_mod:%s,limit=1,sort=nearest]",
                FLYING_SWORD_FX, entityTypePath);

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
        sendEntityEffectToPlayer(player, fx, entities, null);
    }

    private static void sendEntityEffectToPlayer(ServerPlayer player, String fx, List<Entity> entities, Vec3 rotation) {
        try {
            Object command = Class.forName("com.lowdragmc.photon.command.EntityEffectCommand").getConstructor().newInstance();
            command.getClass().getMethod("setLocation", ResourceLocation.class).invoke(command, ResourceLocation.parse(fx));
            command.getClass().getMethod("setEntities", List.class).invoke(command, entities);
            command.getClass().getMethod("setAllowMulti", boolean.class).invoke(command, rotation != null);
            if (rotation != null) {
                command.getClass().getMethod("setRotation", Vec3.class).invoke(command, rotation);
            }
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
