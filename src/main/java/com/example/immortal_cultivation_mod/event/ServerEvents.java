package com.example.immortal_cultivation_mod.event;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.attachment.SpiritRoots;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.effect.PhotonEffects;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.network.ModPayloads;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = ImmortalCultivationMod.MODID)
public class ServerEvents {
    private static final ResourceLocation MAGIC_RITUAL_SPIRIT_STONE =
            ResourceLocation.fromNamespaceAndPath("magic_ritual_mod", "spirit_stone");
    private static final Map<UUID, Vec3> MEDITATION_ANCHORS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> EARTH_ESCAPE_GRACE_TICKS = new ConcurrentHashMap<>();
    private static final Map<UUID, Vec3> QI_GATHERING_ANCHORS = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> SPIRIT_SIGHT_ACTIVE = new ConcurrentHashMap<>();
    private static final Map<UUID, FogReveal> FOG_REVEALS = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onPlayerTickPre(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;

        var data = ModAttachments.getData(player);

        if (player.hasEffect(ModEffects.EARTH_ESCAPE)) {
            player.noPhysics = true;
            player.setNoGravity(true);
            player.fallDistance = 0.0F;
            player.resetFallDistance();
        }

        if (data.isMeditating()) {
            Vec3 anchor = MEDITATION_ANCHORS.computeIfAbsent(player.getUUID(), ignored -> player.position());
            player.xxa = 0;
            player.yya = 0;
            player.zza = 0;
            Vec3 movement = player.getDeltaMovement();
            if (movement.lengthSqr() != 0) {
                player.setDeltaMovement(Vec3.ZERO);
            }
            if (player.distanceToSqr(anchor) > 0.0001D) {
                player.teleportTo(anchor.x, anchor.y, anchor.z);
            }
            player.setSprinting(false);
            player.setPose(Pose.SITTING);
            player.resetFallDistance();
            player.hurtMarked = true;
        } else {
            MEDITATION_ANCHORS.remove(player.getUUID());
        }

        if (player.hasEffect(ModEffects.QI_GATHERING)) {
            if (player.isUsingItem()) {
                player.stopUsingItem();
            }
            Vec3 anchor = QI_GATHERING_ANCHORS.computeIfAbsent(player.getUUID(), ignored -> player.position());
            if (player.distanceToSqr(anchor) > 0.01D) {
                player.removeEffect(ModEffects.QI_GATHERING);
                QI_GATHERING_ANCHORS.remove(player.getUUID());
                if (player instanceof ServerPlayer sp) {
                    PhotonEffects.meditatingStop(sp);
                }
            }
        } else {
            QI_GATHERING_ANCHORS.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;

        var data = ModAttachments.getData(player);

        var levelDef = CultivationLevels.getLevelDef(data.cultivationLevel());
        int maxQi = getEffectiveMaxQi(data, levelDef);
        applyLevelStats(player, data, levelDef);

        int maxAge = levelDef.maxAge() - data.agePenalty();
        if (maxAge <= 0) {
            resetPlayer(player);
            return;
        }

        if (player.hasEffect(ModEffects.EARTH_ESCAPE)) {
            handleEarthEscapeTick(player, data, levelDef);
            data = ModAttachments.getData(player);
        } else if (player.noPhysics && !player.isSpectator()) {
            player.noPhysics = false;
            player.setNoGravity(false);
            EARTH_ESCAPE_GRACE_TICKS.remove(player.getUUID());
        }

        if (!CultivationLevels.isMortal(data.cultivationLevel()) && maxQi > 0 && data.qi() < maxQi && player.tickCount % 20 == 0) {
            int regen = Math.max(1, player.hasEffect(ModEffects.QI_GATHERING) ? maxQi * 5 / 100 : maxQi / 100);
            ModAttachments.setData(player, data.withQi(Math.min(maxQi, data.qi() + regen)));
            data = ModAttachments.getData(player);
        }

        if (player instanceof ServerPlayer sp && SPIRIT_SIGHT_ACTIVE.containsKey(player.getUUID())) {
            handleSpiritSightTick(sp, data);
            data = ModAttachments.getData(player);
        }

        if (data.isMeditating() && maxQi > 0 && player.tickCount % 20 == 0) {
            int need = CultivationLevels.getTotalQiNeeded(data.cultivationLevel());
            if (data.qi() >= 10 && data.cultivationProgress() < need) {
                int progressGain = SpiritRoots.cultivationProgressGain(data, 10);
                ModAttachments.setData(player,
                        data.withQi(data.qi() - 10)
                                .withCultivationProgress(Math.min(need, data.cultivationProgress() + progressGain)));
            }
        }

        if (player.tickCount % 40 == 0 && player instanceof ServerPlayer sp) {
            syncPlayerData(sp);
        }

        if (player instanceof ServerPlayer sp) {
            PhotonEffects.tick(sp);
            tickFogReveal(sp);
            if (data.isMeditating() && player.tickCount % 40 == 0) {
                broadcastMeditationState(sp, true);
            }
        }
    }

    public static void resetPlayer(Player player) {
        var data = ModAttachments.CultivationData.createDefault();
        if (player instanceof ServerPlayer sp) {
            var roots = SpiritRoots.random(sp.getRandom());
            data = data.withSpiritRoots(roots.roots(), roots.grade());
        }
        ModAttachments.setData(player, data);
        applyLevelStats(player, data, CultivationLevels.getLevelDef(data.cultivationLevel()));
        if (player instanceof ServerPlayer sp) {
            syncPlayerData(sp);
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MEDITATION_ANCHORS.remove(player.getUUID());
            QI_GATHERING_ANCHORS.remove(player.getUUID());
            EARTH_ESCAPE_GRACE_TICKS.remove(player.getUUID());
            SPIRIT_SIGHT_ACTIVE.remove(player.getUUID());
            FOG_REVEALS.remove(player.getUUID());
            var data = ModAttachments.getData(player);
            int maxQi = getEffectiveMaxQi(data, CultivationLevels.getLevelDef(data.cultivationLevel()));
            ModAttachments.setData(player, data.withAgePenalty(data.agePenalty() + 10).withQi(maxQi));
            syncPlayerData(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            var data = ModAttachments.getData(sp);
            if (SpiritRoots.needsRandomRoots(data)) {
                var roots = SpiritRoots.random(sp.getRandom());
                data = data.withSpiritRoots(roots.roots(), roots.grade());
                ModAttachments.setData(sp, data);
            }
            if (data.isMeditating()) {
                ModAttachments.setData(sp, data.withMeditating(false));
            }
            MEDITATION_ANCHORS.remove(sp.getUUID());
            QI_GATHERING_ANCHORS.remove(sp.getUUID());
            EARTH_ESCAPE_GRACE_TICKS.remove(sp.getUUID());
            SPIRIT_SIGHT_ACTIVE.remove(sp.getUUID());
            FOG_REVEALS.remove(sp.getUUID());
            syncPlayerData(sp);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        Item item = event.getItemStack().getItem();
        if (!MAGIC_RITUAL_SPIRIT_STONE.equals(BuiltInRegistries.ITEM.getKey(item))) {
            return;
        }

        var data = ModAttachments.getData(player);
        int maxQi = getEffectiveMaxQi(data, CultivationLevels.getLevelDef(data.cultivationLevel()));
        if (maxQi <= 0 || data.qi() >= maxQi) {
            return;
        }

        ModAttachments.setData(player, data.withQi(Math.min(maxQi, data.qi() + 10)));
        if (!player.getAbilities().instabuild) {
            event.getItemStack().shrink(1);
        }
        syncPlayerData(player);

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (player.hasEffect(ModEffects.QI_GATHERING)) {
                event.setCanceled(true);
                return;
            }
            triggerLingbeng(player, event.getTarget().getX(), event.getTarget().getY(), event.getTarget().getZ());
            var data = ModAttachments.getData(player);
            if (data.physicalAttack() > 0) {
                event.getTarget().hurt(player.damageSources().playerAttack(player), data.physicalAttack());
            }
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (player.hasEffect(ModEffects.QI_GATHERING)) {
            event.setCanceled(true);
            return;
        }

        if (player.hasEffect(ModEffects.EARTH_ESCAPE)) {
            event.setCanceled(true);
            return;
        }

        var pos = event.getPos();
        triggerLingbeng(player, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.hasEffect(ModEffects.QI_GATHERING)) {
            event.setAmount(event.getAmount() * 3.0F);
        }
    }

    @SubscribeEvent
    public static void onShieldBlock(LivingShieldBlockEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.hasEffect(ModEffects.QI_GATHERING)) {
            event.setBlocked(false);
            event.setBlockedDamage(0.0F);
            player.stopUsingItem();
        }
    }

    private static void triggerLingbeng(ServerPlayer player, double x, double y, double z) {
        if (!player.hasEffect(ModEffects.LINGBENG)) {
            return;
        }

        var data = ModAttachments.getData(player);
        int cost = 50;
        if (data.qi() < cost) {
            player.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".not_enough_qi"));
            return;
        }

        var lingbeng = player.getEffect(ModEffects.LINGBENG);
        ModAttachments.setData(player, data.withQi(data.qi() - cost));
        float radius = 2.0F + Math.min(9, lingbeng == null ? 0 : lingbeng.getAmplifier());
        player.removeEffect(ModEffects.LINGBENG);
        if (player.level() instanceof ServerLevel serverLevel) {
            PhotonEffects.lingbengExplosion(serverLevel, x, y, z);
        }
        player.level().explode(player, x, y, z, radius, Level.ExplosionInteraction.BLOCK);
        syncPlayerData(player);
    }

    public static void syncPlayerData(ServerPlayer player) {
        var data = ModAttachments.getData(player);
        var levelDef = CultivationLevels.getLevelDef(data.cultivationLevel());
        applyLevelStats(player, data, levelDef);

        PacketDistributor.sendToPlayer(player,
                new ModPayloads.ClientboundSyncPlayerDataPayload(
                        data.qi(),
                        getEffectiveMaxQi(data, levelDef),
                        getEffectiveMaxHp(data, levelDef),
                        levelDef.maxAge(),
                        data.cultivationLevel(),
                        data.luck(),
                        data.moral(),
                        data.bodyType(),
                        data.soul(),
                        data.thoughts(),
                        data.spiritRoots(),
                        data.spiritRootGrade(),
                        data.agePenalty(),
                        data.cultivationProgress(),
                        data.knownSpells(),
                        data.isMeditating(),
                        data.skillPoints(),
                        data.maxHpBonus(),
                        data.maxQiBonus(),
                        data.maxEnergyBonus(),
                        data.physicalAttack(),
                        data.magicAttack(),
                        data.mentalAttack()
                )
        );
    }

    public static void tryBreakthrough(ServerPlayer player) {
        var data = ModAttachments.getData(player);

        String next = CultivationLevels.getNextStage(data.cultivationLevel());
        if (next == null) {
            player.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".max_level"));
            return;
        }

        var levelDef = CultivationLevels.getLevelDef(data.cultivationLevel());
        int maxQi = getEffectiveMaxQi(data, levelDef);
        int need = CultivationLevels.getTotalQiNeeded(data.cultivationLevel());

        if (data.qi() < maxQi || data.cultivationProgress() < need) {
            player.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".breakthrough_requirements_missing"));
            return;
        }

        boolean needPill = CultivationLevels.needsBreakthroughPill(data.cultivationLevel());
        if (needPill) {
            boolean has = player.getInventory().items.stream().anyMatch(s -> s.is(ModItems.BREAKTHROUGH_PILL.get()));
            if (!has) {
                player.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".need_breakthrough_pill"));
                return;
            }

            for (var s : player.getInventory().items) {
                if (s.is(ModItems.BREAKTHROUGH_PILL.get())) {
                    s.shrink(1);
                    break;
                }
            }
        }

        if (player.getRandom().nextFloat() < CultivationLevels.getBreakthroughFailChance(data.luck(), data.cultivationLevel())) {
            ModAttachments.setData(player, data.withCultivationProgress(0));
            player.hurt(player.damageSources().magic(), Math.max(1.0F, getEffectiveMaxHp(data, levelDef) * 0.5F));
            player.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".breakthrough_failed"));
            syncPlayerData(player);
            return;
        }

        ModAttachments.setData(player,
                data.withCultivationLevel(next)
                        .withQi(0)
                        .withCultivationProgress(0)
                        .withMeditating(false)
                        .withAddedSkillPoints(10));

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0D, player.getZ(), 60, 0.8D, 0.9D, 0.8D, 0.05D);
            serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.0D, player.getZ(), 30, 0.6D, 0.8D, 0.6D, 0.08D);
        }

        player.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".breakthrough_success", next));
        syncPlayerData(player);
    }

    private static void applyLevelStats(Player player, ModAttachments.CultivationData data, CultivationLevels.LevelDef levelDef) {
        int maxHp = getEffectiveMaxHp(data, levelDef);
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            double previousMax = maxHealth.getBaseValue();
            if (previousMax != maxHp) {
                maxHealth.setBaseValue(maxHp);
            }

            if (player.getHealth() > maxHp) {
                player.setHealth(maxHp);
            } else if (previousMax <= 1.0D && player.getHealth() <= 1.0F) {
                player.setHealth(maxHp);
            }
        }

        AttributeInstance movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed != null) {
            double speed = 0.1D + (data.maxEnergyBonus() / 40) * 0.005D;
            if (player.hasEffect(ModEffects.EARTH_ESCAPE)) {
                speed += 0.08D;
            }
            if (movementSpeed.getBaseValue() != speed) {
                movementSpeed.setBaseValue(speed);
            }
        }
    }

    private static void handleEarthEscapeTick(Player player, ModAttachments.CultivationData data, CultivationLevels.LevelDef levelDef) {
        player.noPhysics = true;
        player.fallDistance = 0.0F;
        player.resetFallDistance();

        int grace = EARTH_ESCAPE_GRACE_TICKS.merge(player.getUUID(), 1, Integer::sum);
        if (!(player instanceof ServerPlayer sp) || player.tickCount % 20 != 0) {
            return;
        }

        sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1, false, false, true));

        if (grace > 20 && !isInsideSolidBlock(player)) {
            sp.removeEffect(ModEffects.EARTH_ESCAPE);
            sp.noPhysics = false;
            sp.setNoGravity(false);
            EARTH_ESCAPE_GRACE_TICKS.remove(sp.getUUID());
            syncPlayerData(sp);
            return;
        }

        if (data.qi() < 5) {
            sp.removeEffect(ModEffects.EARTH_ESCAPE);
            sp.noPhysics = false;
            sp.setNoGravity(false);
            EARTH_ESCAPE_GRACE_TICKS.remove(sp.getUUID());
            syncPlayerData(sp);
            return;
        }

        ModAttachments.setData(sp, data.withQi(data.qi() - 5));
        syncPlayerData(sp);
    }

    private static boolean isInsideSolidBlock(Player player) {
        BlockPos min = BlockPos.containing(player.getX() - 0.3D, player.getY(), player.getZ() - 0.3D);
        BlockPos max = BlockPos.containing(player.getX() + 0.3D, player.getY() + player.getBbHeight() * 0.8D, player.getZ() + 0.3D);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (player.level().getBlockState(pos).isSuffocating(player.level(), pos)) {
                return true;
            }
        }
        return false;
    }

    private static int getEffectiveMaxHp(ModAttachments.CultivationData data, CultivationLevels.LevelDef levelDef) {
        return Math.max(1, levelDef.maxHp() + data.maxHpBonus());
    }

    private static int getEffectiveMaxQi(ModAttachments.CultivationData data, CultivationLevels.LevelDef levelDef) {
        return Math.max(1, levelDef.maxQi() + data.maxQiBonus());
    }

    public static void handleToggleMeditate(ServerPlayer player) {
        var data = ModAttachments.getData(player);
        boolean newState = !data.isMeditating();
        if (newState && !player.onGround()) {
            player.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".meditation_need_ground"));
            return;
        }
        ModAttachments.setData(player, data.withMeditating(newState));
        if (newState) {
            MEDITATION_ANCHORS.put(player.getUUID(), player.position());
            player.setDeltaMovement(Vec3.ZERO);
            player.setPose(Pose.SITTING);
            if (player.level() instanceof ServerLevel serverLevel) {
                PhotonEffects.meditatingStart(serverLevel, player);
            }
        } else {
            MEDITATION_ANCHORS.remove(player.getUUID());
            player.setPose(Pose.STANDING);
            PhotonEffects.meditatingStop(player);
        }
        broadcastMeditationState(player, newState);
        player.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + (newState ? ".meditation_started" : ".meditation_stopped")));
        syncPlayerData(player);
    }

    public static void startQiGathering(ServerPlayer player) {
        QI_GATHERING_ANCHORS.put(player.getUUID(), player.position());
    }

    public static void toggleSpiritSight(ServerPlayer player) {
        UUID id = player.getUUID();
        if (SPIRIT_SIGHT_ACTIVE.remove(id) != null) {
            player.removeEffect(MobEffects.NIGHT_VISION);
            syncPlayerData(player);
            return;
        }

        var data = ModAttachments.getData(player);
        if (data.qi() < 5) {
            player.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".not_enough_qi"));
            return;
        }

        SPIRIT_SIGHT_ACTIVE.put(id, true);
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, false, false, true));
        syncPlayerData(player);
    }

    private static void handleSpiritSightTick(ServerPlayer player, ModAttachments.CultivationData data) {
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, false, false, true));
        if (player.tickCount % 20 != 0) {
            return;
        }

        if (data.qi() < 5) {
            SPIRIT_SIGHT_ACTIVE.remove(player.getUUID());
            player.removeEffect(MobEffects.NIGHT_VISION);
            player.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".not_enough_qi"));
            syncPlayerData(player);
            return;
        }

        ModAttachments.setData(player, data.withQi(data.qi() - 5));
        syncPlayerData(player);
    }

    public static int startFogMirrorReveal(ServerPlayer player) {
        List<BlockPos> positions = findFogEscapeBlocks(player, 32);
        if (!positions.isEmpty()) {
            FOG_REVEALS.put(player.getUUID(), new FogReveal(positions, 60));
        }
        return positions.size();
    }

    private static List<BlockPos> findFogEscapeBlocks(ServerPlayer player, int radius) {
        List<BlockPos> positions = new ArrayList<>();
        BlockPos center = player.blockPosition();
        BlockPos min = center.offset(-radius, -radius, -radius);
        BlockPos max = center.offset(radius, radius, radius);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (isFogEscapeBlock(player.level().getBlockState(pos))) {
                positions.add(pos.immutable());
            }
        }
        return positions;
    }

    private static boolean isFogEscapeBlock(net.minecraft.world.level.block.state.BlockState state) {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (key != null && "magic_ritual_mod".equals(key.getNamespace())) {
            String path = key.getPath();
            if ("fogescapeblock".equals(path) || "fog_escape_block".equals(path) || "combo_block_fog".equals(path)) {
                return true;
            }
            if ("combo_block".equals(path)) {
                for (var property : state.getProperties()) {
                    if ("combo_type".equals(property.getName()) && "fog".equals(String.valueOf(state.getValue(property)))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static void tickFogReveal(ServerPlayer player) {
        FogReveal reveal = FOG_REVEALS.get(player.getUUID());
        if (reveal == null) {
            return;
        }

        if (reveal.ticksLeft() <= 0) {
            FOG_REVEALS.remove(player.getUUID());
            return;
        }

        if (player.level() instanceof ServerLevel serverLevel && player.tickCount % 4 == 0) {
            for (BlockPos pos : reveal.positions()) {
                double x = pos.getX() + 0.5D;
                double y = pos.getY() + 0.5D;
                double z = pos.getZ() + 0.5D;
                serverLevel.sendParticles(ParticleTypes.END_ROD, x, y + 0.6D, z, 6, 0.45D, 0.45D, 0.45D, 0.02D);
                serverLevel.sendParticles(ParticleTypes.CLOUD, x, y, z, 3, 0.35D, 0.35D, 0.35D, 0.01D);
            }
        }

        FOG_REVEALS.put(player.getUUID(), new FogReveal(reveal.positions(), reveal.ticksLeft() - 1));
    }

    private record FogReveal(List<BlockPos> positions, int ticksLeft) {}

    private static void broadcastMeditationState(ServerPlayer player, boolean meditating) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                new ModPayloads.ClientboundMeditationStatePayload(player.getUUID(), meditating));
    }
}
