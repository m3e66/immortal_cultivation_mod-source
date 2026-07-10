package com.example.immortal_cultivation_mod.event;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.BodyTypes;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.attachment.SpiritRoots;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.effect.PhotonEffects;
import com.example.immortal_cultivation_mod.entity.CultivatorCorpseEntity;
import com.example.immortal_cultivation_mod.entity.ModEntities;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.item.ForgingSystem;
import com.example.immortal_cultivation_mod.item.QiInfusedWeapon;
import com.example.immortal_cultivation_mod.network.ModPayloads;
import com.example.immortal_cultivation_mod.spell.ChangqingJue;
import com.example.immortal_cultivation_mod.spell.DielangShield;
import com.example.immortal_cultivation_mod.spell.FentianLifeRenewal;
import com.example.immortal_cultivation_mod.spell.Fengya;
import com.example.immortal_cultivation_mod.spell.GushiShield;
import com.example.immortal_cultivation_mod.spell.HantiBingqin;
import com.example.immortal_cultivation_mod.spell.HutiQi;
import com.example.immortal_cultivation_mod.spell.LightBeamAttack;
import com.example.immortal_cultivation_mod.spell.MichenZhang;
import com.example.immortal_cultivation_mod.spell.PlayerShieldManager;
import com.example.immortal_cultivation_mod.spell.SifangJie;
import com.example.immortal_cultivation_mod.spell.SlidingWater;
import com.example.immortal_cultivation_mod.spell.UndeadControl;
import com.example.immortal_cultivation_mod.spell.Weiya;
import com.example.immortal_cultivation_mod.spell.WindStep;
import com.example.immortal_cultivation_mod.spell.XuyingTa;
import com.example.immortal_cultivation_mod.spell.YufengJue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import top.theillusivec4.curios.api.CuriosApi;

@EventBusSubscriber(modid = ImmortalCultivationMod.MODID)
public class ServerEvents {
    private static final ResourceLocation CULTIVATION_MAX_HEALTH_MODIFIER =
            ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "cultivation_max_health");
    private static final String JINDAN_CHALLENGE_PENDING_TAG = "ImmortalCultivationPendingJindanBreakthrough";
    private static final String JINDAN_QUALITY_TAG = "ImmortalCultivationJindanQuality";
    private static final int AMBIENT_QI_SCAN_RADIUS = 48;
    private static final int AMBIENT_QI_PER_SPIRIT_VEIN_CENTER = 25;
    private static final int AMBIENT_QI_CACHE_TICKS = 100;
    private static final int THOUGHT_REGEN_INTERVAL_TICKS = 20 * 60;
    private static final Map<UUID, Vec3> MEDITATION_ANCHORS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> EARTH_ESCAPE_GRACE_TICKS = new ConcurrentHashMap<>();
    private static final Map<UUID, Vec3> QI_GATHERING_ANCHORS = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> SPIRIT_SIGHT_ACTIVE = new ConcurrentHashMap<>();
    private static final Map<UUID, FogReveal> FOG_REVEALS = new ConcurrentHashMap<>();
    private static final Map<UUID, AmbientQi> AMBIENT_QI_CACHE = new ConcurrentHashMap<>();
    private static final List<String> DEBUG_STATS = List.of(
            "cultivation_level",
            "age",
            "moral",
            "luck",
            "soul",
            "thoughts",
            "spirit_roots",
            "spirit_root_grade",
            "skill_points",
            "max_hp",
            "max_qi",
            "max_energy",
            "physical",
            "magic",
            "mental",
            "body_type"
    );
    private static final List<String> SETTABLE_DEBUG_STATS = List.of(
            "age",
            "moral",
            "luck",
            "soul",
            "thoughts",
            "skill_points",
            "max_hp",
            "max_qi",
            "max_energy",
            "physical",
            "magic",
            "mental"
    );
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("cultivationdebug")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("adjust")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("stat", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(DEBUG_STATS, builder))
                                        .then(Commands.argument("delta", IntegerArgumentType.integer())
                                                .executes(context -> adjustDebugStat(
                                                        context.getSource(),
                                                        EntityArgument.getPlayers(context, "targets"),
                                                        StringArgumentType.getString(context, "stat"),
                                                        IntegerArgumentType.getInteger(context, "delta")))))))
                .then(Commands.literal("set")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("stat", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(SETTABLE_DEBUG_STATS, builder))
                                        .then(Commands.argument("value", IntegerArgumentType.integer())
                                                .executes(context -> setDebugStat(
                                                        context.getSource(),
                                                        EntityArgument.getPlayers(context, "targets"),
                                                        StringArgumentType.getString(context, "stat"),
                                                        IntegerArgumentType.getInteger(context, "value"))))))));
    }

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
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        if (event.getEntity() instanceof ItemEntity itemEntity) {
            ItemStack tempered = ForgingSystem.temper(itemEntity.getItem());
            if (!tempered.isEmpty() && itemEntity.isInWaterOrBubble()) {
                itemEntity.setItem(tempered);
                if (itemEntity.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.SMOKE,
                            itemEntity.getX(), itemEntity.getY() + 0.15D, itemEntity.getZ(),
                            28, 0.25D, 0.18D, 0.25D, 0.025D);
                    serverLevel.sendParticles(ParticleTypes.POOF,
                            itemEntity.getX(), itemEntity.getY() + 0.15D, itemEntity.getZ(),
                            10, 0.18D, 0.12D, 0.18D, 0.01D);
                }
            }
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity living)) {
            return;
        }
        UndeadControl.tick(living);
        if (!living.hasEffect(ModEffects.DINGSHEN) && !living.hasEffect(ModEffects.FROZEN)) {
            return;
        }

        living.setDeltaMovement(Vec3.ZERO);
        living.fallDistance = 0.0F;
        living.resetFallDistance();
        living.hurtMarked = true;
        if (living instanceof Player player) {
            player.xxa = 0.0F;
            player.yya = 0.0F;
            player.zza = 0.0F;
            player.setSprinting(false);
        }
        if (living instanceof net.minecraft.world.entity.Mob mob) {
            mob.getNavigation().stop();
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;

        if (player instanceof ServerPlayer serverPlayer) {
            ModPayloads.ModPayloadsHandler.tickQueuedComboCasts(serverPlayer);
        }

        var data = ModAttachments.getData(player);

        var levelDef = CultivationLevels.getLevelDef(data.cultivationLevel());
        int maxQi = getEffectiveMaxQi(data, levelDef);
        applyLevelStats(player, data, levelDef);
        data = handleThoughtsTick(player, data);

        int maxAge = levelDef.maxAge() - data.agePenalty();
        if (maxAge <= 0) {
            if (player instanceof ServerPlayer serverPlayer) {
                spawnAgeExhaustedCorpse(serverPlayer);
            }
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

        if (!player.hasEffect(ModEffects.FROZEN_QI)
                && !com.example.immortal_cultivation_mod.spell.DuanliuKongdun.isInsideAny(player)
                && !CultivationLevels.isMortal(data.cultivationLevel()) && maxQi > 0 && data.qi() < maxQi && player.tickCount % 20 == 0) {
            AmbientQi ambientQi = getAmbientQi(player);
            int regen = Math.max(1, player.hasEffect(ModEffects.QI_GATHERING) ? maxQi * 5 / 100 : maxQi / 100);
            regen += ambientQi.value() / AMBIENT_QI_PER_SPIRIT_VEIN_CENTER;
            int forgedQiRegen = com.example.immortal_cultivation_mod.item.ForgingSystem.heldQiRegenBonusPercent(player);
            if (forgedQiRegen > 0) {
                regen += Math.max(1, regen * forgedQiRegen / 100);
            }
            ModAttachments.setData(player, data.withQi(Math.min(maxQi, data.qi() + regen)));
            data = ModAttachments.getData(player);
        }

        if (player instanceof ServerPlayer sp && SPIRIT_SIGHT_ACTIVE.containsKey(player.getUUID())) {
            handleSpiritSightTick(sp, data);
            data = ModAttachments.getData(player);
        }

        if (data.isMeditating() && player.tickCount % 20 == 0 && CultivationMethods.get(data.activeCultivationMethod()) != null) {
            int meditationSpeed = getMeditationSpeedMultiplier(player);
            data = data.withAddedMethodProficiency(data.activeCultivationMethod(), meditationSpeed);
            ModAttachments.setData(player, data);
        }

        if (data.isMeditating() && maxQi > 0 && player.tickCount % 20 == 0) {
            long need = CultivationLevels.getTotalQiNeeded(data.cultivationLevel());
            if (data.cultivationProgress() < need
                    && CultivationMethods.canGainProgress(data.activeCultivationMethod(), data.cultivationLevel())
                    && spendQiOrBlood(player, data, 10)) {
                data = ModAttachments.getData(player);
                int baseGain = SpiritRoots.cultivationProgressGain(data, 10);
                int meditationSpeed = getMeditationSpeedMultiplier(player);
                int progressGain = Math.max(1, baseGain * CultivationMethods.progressMultiplierPercent(data) / 100) * meditationSpeed;
                ModAttachments.setData(player,
                        data.withCultivationProgress(Math.min(need, data.cultivationProgress() + progressGain)));
            }
        }

        if (player.tickCount % 40 == 0 && player instanceof ServerPlayer sp) {
            syncPlayerData(sp);
        }

        if (player instanceof ServerPlayer sp) {
            WindStep.tick(sp);
            YufengJue.tick(sp);
            ChangqingJue.tick(sp);
            FentianLifeRenewal.tick(sp);
            HantiBingqin.tick(sp);
            MichenZhang.tick(sp);
            SifangJie.tick(sp);
            Weiya.tick(sp);
            XuyingTa.tick(sp);
            SlidingWater.tick(sp);
            DielangShield.tick(sp);
            QiInfusedWeapon.tickOverflow(sp);
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

    private static int getMeditationSpeedMultiplier(Player player) {
        return wearingJadePendant(player) ? 2 : 1;
    }

    private static boolean wearingJadePendant(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .flatMap(handler -> handler.findFirstCurio(ModItems.JADE_PENDANT.get()))
                .isPresent();
    }

    private static void spawnAgeExhaustedCorpse(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        CultivatorCorpseEntity corpse = ModEntities.CULTIVATOR_CORPSE.get().create(serverLevel);
        if (corpse == null) {
            return;
        }
        corpse.copyFrom(player);
        serverLevel.addFreshEntity(corpse);
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide && !(event.getEntity() instanceof Player)) {
            int amount = Math.max(1, (int) (event.getEntity().getMaxHealth() / 10.0F));
            while (amount > 0) {
                int count = Math.min(64, amount);
                event.getEntity().spawnAtLocation(new net.minecraft.world.item.ItemStack(ModItems.BLOOD.get(), count));
                amount -= count;
            }
        }
        if (event.getEntity() instanceof ServerPlayer player) {
            if (FentianLifeRenewal.tryCatchDeath(player)) {
                event.setCanceled(true);
                return;
            }
            MEDITATION_ANCHORS.remove(player.getUUID());
            QI_GATHERING_ANCHORS.remove(player.getUUID());
            EARTH_ESCAPE_GRACE_TICKS.remove(player.getUUID());
            SPIRIT_SIGHT_ACTIVE.remove(player.getUUID());
            player.removeEffect(ModEffects.SPIRIT_SIGHT);
            FOG_REVEALS.remove(player.getUUID());
            AMBIENT_QI_CACHE.remove(player.getUUID());
            WindStep.clear(player);
            YufengJue.clear(player);
            FentianLifeRenewal.clear(player);
            HantiBingqin.clear(player);
            DielangShield.clear(player);
            HutiQi.clear(player);
            GushiShield.clear(player);
            SifangJie.clear(player);
            PlayerShieldManager.clearAll(player);
            var data = ModAttachments.getData(player);
            var levelDef = CultivationLevels.getLevelDef(data.cultivationLevel());
            int maxQi = getEffectiveMaxQi(data, levelDef);
            var agedData = data.withAgePenalty(data.agePenalty() + 10).withQi(maxQi);
            ModAttachments.setData(player, agedData);
            if (levelDef.maxAge() - agedData.agePenalty() <= 0) {
                spawnAgeExhaustedCorpse(player);
                resetPlayer(player);
            } else {
                syncPlayerData(player);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            var data = ModAttachments.getData(sp);
            applyLevelStats(sp, data, CultivationLevels.getLevelDef(data.cultivationLevel()));
            sp.setHealth(sp.getMaxHealth());
            HantiBingqin.clear(sp);
            DielangShield.clear(sp);
            HutiQi.clear(sp);
            GushiShield.clear(sp);
            SifangJie.clear(sp);
            PlayerShieldManager.clearAll(sp);
            syncPlayerData(sp);
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
            sp.removeEffect(ModEffects.SPIRIT_SIGHT);
            FOG_REVEALS.remove(sp.getUUID());
            AMBIENT_QI_CACHE.remove(sp.getUUID());
            WindStep.clear(sp);
            YufengJue.clear(sp);
            FentianLifeRenewal.clear(sp);
            HantiBingqin.clear(sp);
            DielangShield.clear(sp);
            HutiQi.clear(sp);
            GushiShield.clear(sp);
            SifangJie.clear(sp);
            PlayerShieldManager.clearAll(sp);
            syncPlayerData(sp);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (ForgingSystem.isForgeMaterial(event.getItemStack())) {
            if (event.getItemStack().getCount() != 1) {
                player.displayClientMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".material_purity_single"), true);
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
                return;
            }
            ForgingSystem.ensurePurity(event.getItemStack(), player.tickCount + player.getId());
            player.displayClientMessage(ForgingSystem.purityTooltip(event.getItemStack()), true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }

        if (event.getItemStack().is(ModItems.ZHENHUN_BELL.get())) {
            if (UndeadControl.useBell(player)) {
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
            }
            return;
        }
        if (event.getItemStack().is(ModItems.YINHUN_GONG.get())) {
            if (UndeadControl.commandToLookTarget(player)) {
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
            }
            return;
        }
        if (event.getItemStack().is(ModItems.COFFIN.get()) && player.isShiftKeyDown()) {
            InteractionResult result = event.getItemStack().use(event.getLevel(), player, event.getHand()).getResult();
            if (result.consumesAction()) {
                event.setCancellationResult(result);
                event.setCanceled(true);
            }
            return;
        }

        Item item = event.getItemStack().getItem();
        int restoreAmount = magicRitualSpiritStoneRestoreAmount(BuiltInRegistries.ITEM.getKey(item));
        if (restoreAmount <= 0) {
            return;
        }

        var data = ModAttachments.getData(player);
        int maxQi = getEffectiveMaxQi(data, CultivationLevels.getLevelDef(data.cultivationLevel()));
        if (maxQi <= 0 || data.qi() >= maxQi) {
            return;
        }

        ModAttachments.setData(player, data.withQi(Math.min(maxQi, data.qi() + restoreAmount)));
        if (!player.getAbilities().instabuild) {
            event.getItemStack().shrink(1);
        }
        syncPlayerData(player);

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!event.getItemStack().is(ModItems.YINHUN_GONG.get())) {
            return;
        }
        UndeadControl.gongCommandToBlock(player, event.getPos().relative(event.getFace()));
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRightClickEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        InteractionResult coffinResult = com.example.immortal_cultivation_mod.item.CoffinItem.tryStore(player, event.getTarget(), event.getHand());
        if (coffinResult != InteractionResult.PASS) {
            event.setCancellationResult(coffinResult);
            event.setCanceled(true);
            return;
        }
        InteractionResult result = UndeadControl.interactEntity(player, event.getTarget(), event.getHand());
        if (result != InteractionResult.PASS) {
            event.setCancellationResult(result);
            event.setCanceled(true);
        }
    }

    public static int magicRitualSpiritStoneRestoreAmount(ResourceLocation itemKey) {
        if (itemKey == null || !"magic_ritual_mod".equals(itemKey.getNamespace())) {
            return 0;
        }
        return switch (itemKey.getPath()) {
            case "spirit_stone", "low_grade_spirit_stone", "low_spirit_stone", "lower_grade_spirit_stone" -> 10;
            case "middle_grade_spirit_stone", "medium_grade_spirit_stone", "mid_grade_spirit_stone", "middle_spirit_stone", "medium_spirit_stone" -> 100;
            case "high_grade_spirit_stone", "high_spirit_stone", "upper_grade_spirit_stone", "upper_spirit_stone" -> 1000;
            default -> 0;
        };
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if ((player.getMainHandItem().is(ModItems.YINHUN_GONG.get()) || player.getOffhandItem().is(ModItems.YINHUN_GONG.get()))
                    && event.getTarget() instanceof LivingEntity target
                    && UndeadControl.gongCommandAttack(player, target)) {
                // Let the normal punch still happen; the gong also broadcasts the attack command.
            }
            if (player.hasEffect(ModEffects.QI_GATHERING)) {
                event.setCanceled(true);
                return;
            }
            Vec3 target = event.getTarget().position().add(0.0D, event.getTarget().getBbHeight() * 0.5D, 0.0D);
            if (LightBeamAttack.shoot(player, target, player.isShiftKeyDown())) {
                event.setCanceled(true);
                return;
            }
            if (Fengya.release(player, target, player.isShiftKeyDown())) {
                event.setCanceled(true);
                return;
            }
            triggerLingbeng(player, event.getTarget().getX(), event.getTarget().getY(), event.getTarget().getZ());
            var data = ModAttachments.getData(player);
            if (data.physicalAttack() > 0) {
                event.getTarget().hurt(player.damageSources().playerAttack(player), data.physicalAttack());
            }
            QiInfusedWeapon.strike(player, event.getTarget());
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getEntity() instanceof Player clicker)) {
            return;
        }

        boolean releasingLightBeam = LightBeamAttack.hasWaiting(clicker);
        if (event.getLevel().isClientSide) {
            if (releasingLightBeam) {
                event.setCanceled(true);
            }
            return;
        }

        if (!(clicker instanceof ServerPlayer player)) {
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
        Vec3 target = Vec3.atCenterOf(pos);
        if (releasingLightBeam) {
            LightBeamAttack.shoot(player, target, player.isShiftKeyDown());
            event.setCanceled(true);
            return;
        }
        Vec3 hit = Vec3.atCenterOf(pos);
        if (event.getFace() != null) {
            hit = hit.add(Vec3.atLowerCornerOf(event.getFace().getNormal()).scale(0.55D));
        }
        if (Fengya.release(player, hit, player.isShiftKeyDown())) {
            event.setCanceled(true);
            return;
        }
        triggerLingbeng(player, hit.x, hit.y, hit.z);
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof LivingEntity target
                && UndeadControl.isUndeadServantType(target)
                && UndeadControl.ignoresFireAndSun(target)
                && (event.getSource().is(DamageTypes.IN_FIRE)
                || event.getSource().is(DamageTypes.ON_FIRE)
                || event.getSource().is(DamageTypes.LAVA)
                || event.getSource().is(DamageTypes.HOT_FLOOR))) {
            target.clearFire();
            target.setRemainingFireTicks(0);
            event.setCanceled(true);
            return;
        }
        if (event.getEntity() instanceof LivingEntity target
                && UndeadControl.isUndeadServantType(target)
                && UndeadControl.isAtLeastHopping(target)
                && event.getSource().is(DamageTypes.PLAYER_ATTACK)
                && event.getSource().getEntity() instanceof ServerPlayer player
                && !QiInfusedWeapon.hasQi(player.getMainHandItem())) {
            event.setCanceled(true);
            return;
        }
        if (event.getEntity() instanceof LivingEntity target
                && UndeadControl.isUndeadServantType(target)
                && event.getSource().is(DamageTypes.PLAYER_ATTACK)
                && event.getSource().getEntity() instanceof ServerPlayer player) {
            if (player.getMainHandItem().is(ModItems.TAOMU_SWORD.get())) {
                event.setAmount(event.getAmount() * 1.5F);
            } else if (player.getMainHandItem().is(ModItems.COPPER_COIN_SWORD.get())) {
                event.setAmount(event.getAmount() * 1.7F);
            }
        }
        if (event.getAmount() > 0.0F
                && event.getEntity() instanceof LivingEntity target
                && event.getSource().is(DamageTypes.PLAYER_ATTACK)
                && event.getSource().getEntity() instanceof ServerPlayer player) {
            ItemStack weapon = player.getMainHandItem();
            event.setAmount(com.example.immortal_cultivation_mod.item.ForgingSystem.adjustWeaponDamage(weapon, target, event.getAmount()));
            com.example.immortal_cultivation_mod.item.ForgingSystem.applyWeaponHitEffects(weapon, target);
        }
        if (event.getSource().is(DamageTypes.FALL)
                && event.getEntity() instanceof LivingEntity living
                && UndeadControl.isUndeadServantType(living)) {
            living.fallDistance = 0.0F;
            living.resetFallDistance();
            event.setCanceled(true);
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player
                && event.getSource().getEntity() instanceof LivingEntity attacker
                && UndeadControl.isOwnedByOwnerUuid(attacker, player.getUUID())) {
            if (attacker instanceof net.minecraft.world.entity.Mob mob) {
                mob.setTarget(null);
                mob.getNavigation().stop();
            }
            event.setCanceled(true);
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player
                && event.getSource().getEntity() instanceof LivingEntity attacker) {
            UndeadControl.defensiveCommand(player, attacker);
        }
        if (event.getAmount() > 0.0F
                && event.getEntity() instanceof LivingEntity target
                && event.getSource().getEntity() instanceof LivingEntity attacker
                && UndeadControl.isUndeadServantType(attacker)
                && UndeadControl.isAtLeastRank(attacker, "飞僵")) {
            target.addEffect(new MobEffectInstance(ModEffects.SHIDU, 20 * 5, 4, false, true, true), attacker);
        }
        if (event.getEntity() instanceof ServerPlayer player && player.hasEffect(ModEffects.QI_GATHERING)) {
            event.setAmount(event.getAmount() * 3.0F);
        }
        if (event.getEntity() instanceof ServerPlayer player) {
            float amount = DielangShield.absorb(player, event.getAmount());
            amount = GushiShield.absorb(player, event.getSource(), amount);
            amount = HutiQi.absorb(player, event.getSource(), amount);
            event.setAmount(amount);
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
        int cost = 85;
        if (!spendQiOrBlood(player, data, cost)) {
            player.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".not_enough_qi"));
            return;
        }

        var lingbeng = player.getEffect(ModEffects.LINGBENG);
        float radius = 2.0F + Math.min(9, lingbeng == null ? 0 : lingbeng.getAmplifier());
        player.removeEffect(ModEffects.LINGBENG);
        PhotonEffects.lingbengStop(player);
        if (player.level() instanceof ServerLevel serverLevel) {
            PhotonEffects.lingbengExplosion(serverLevel, x, y, z);
        }
        player.level().explode(player, x, y, z, radius, Level.ExplosionInteraction.BLOCK);
        syncPlayerData(player);
    }

    private static int adjustDebugStat(CommandSourceStack source, Collection<ServerPlayer> targets, String stat, int delta) {
        if (!DEBUG_STATS.contains(stat)) {
            source.sendFailure(Component.literal("Unknown cultivation debug stat: " + stat));
            return 0;
        }

        int changed = 0;
        for (ServerPlayer target : targets) {
            var data = ModAttachments.getData(target);
            var updated = adjustDebugData(data, stat, delta);
            if (!updated.equals(data)) {
                ModAttachments.setData(target, updated);
                syncPlayerData(target);
                changed++;
            }
        }

        int changedCount = changed;
        source.sendSuccess(() -> Component.literal("Adjusted " + stat + " by " + delta + " for " + changedCount + " player(s)."), true);
        return changed;
    }

    private static int setDebugStat(CommandSourceStack source, Collection<ServerPlayer> targets, String stat, int value) {
        if (!SETTABLE_DEBUG_STATS.contains(stat)) {
            source.sendFailure(Component.literal("Set only supports numeric debug stats: " + String.join(", ", SETTABLE_DEBUG_STATS)));
            return 0;
        }

        int changed = 0;
        for (ServerPlayer target : targets) {
            var data = ModAttachments.getData(target);
            var updated = setDebugData(data, stat, value);
            if (!updated.equals(data)) {
                ModAttachments.setData(target, updated);
                syncPlayerData(target);
                changed++;
            }
        }

        int changedCount = changed;
        source.sendSuccess(() -> Component.literal("Set " + stat + " to " + value + " for " + changedCount + " player(s)."), true);
        return changed;
    }

    private static ModAttachments.CultivationData handleThoughtsTick(Player player, ModAttachments.CultivationData data) {
        boolean wasEmpty = data.thoughts() <= 0;
        ModAttachments.CultivationData updated = data;

        if (data.thoughts() > ModAttachments.MAX_THOUGHTS) {
            updated = updated.withThoughts(ModAttachments.MAX_THOUGHTS);
        }

        if (player.tickCount % THOUGHT_REGEN_INTERVAL_TICKS == 0 && updated.thoughts() < ModAttachments.MAX_THOUGHTS) {
            int regen = Math.max(1, ModAttachments.MAX_THOUGHTS * 5 / 100);
            updated = updated.withThoughts(updated.thoughts() + regen);
        }

        if (!updated.equals(data)) {
            ModAttachments.setData(player, updated);
        }
        if (wasEmpty) {
            player.addEffect(new MobEffectInstance(ModEffects.DAZE, 40, 0, false, true, true));
        }
        return updated;
    }

    public static void adjustThoughts(Player player, int delta) {
        var data = ModAttachments.getData(player);
        var updated = data.withThoughts(data.thoughts() + delta);
        ModAttachments.setData(player, updated);
        if (updated.thoughts() <= 0) {
            player.addEffect(new MobEffectInstance(ModEffects.DAZE, 40, 0, false, true, true));
        }
        if (player instanceof ServerPlayer serverPlayer) {
            syncPlayerData(serverPlayer);
        }
    }

    private static ModAttachments.CultivationData adjustDebugData(ModAttachments.CultivationData data, String stat, int delta) {
        return switch (stat) {
            case "cultivation_level" -> data.withCultivationLevel(nextCultivationLevel(data.cultivationLevel(), delta));
            case "body_type" -> data.withBodyType(BodyTypes.next(data.bodyType(), delta));
            case "age" -> data.withAgePenalty(clamp(
                    data.agePenalty() - delta,
                    0,
                    Math.max(0, CultivationLevels.getLevelDef(data.cultivationLevel()).maxAge() - 1)
            ));
            case "moral" -> data.withMoral(clamp(data.moral() + delta, 0, 100));
            case "luck" -> data.withLuck(clamp(data.luck() + delta, 0, 100));
            case "soul" -> data.withSoul(clamp(data.soul() + delta, 0, 1000));
            case "thoughts" -> data.withThoughts(clamp(data.thoughts() + delta, 0, ModAttachments.MAX_THOUGHTS));
            case "spirit_roots" -> data.withSpiritRoots(SpiritRoots.nextRootSet(data.spiritRoots(), delta), data.spiritRootGrade());
            case "spirit_root_grade" -> data.withSpiritRoots(data.spiritRoots(), SpiritRoots.nextGrade(data.spiritRootGrade(), delta));
            case "skill_points", "max_hp", "max_qi", "max_energy", "physical", "magic", "mental" -> data.debugAdjustStat(stat, delta);
            default -> data;
        };
    }

    private static ModAttachments.CultivationData setDebugData(ModAttachments.CultivationData data, String stat, int value) {
        var levelDef = CultivationLevels.getLevelDef(data.cultivationLevel());
        return switch (stat) {
            case "age" -> data.withAgePenalty(clamp(levelDef.maxAge() - value, 0, Math.max(0, levelDef.maxAge() - 1)));
            case "moral" -> data.withMoral(clamp(value, 0, 100));
            case "luck" -> data.withLuck(clamp(value, 0, 100));
            case "soul" -> data.withSoul(clamp(value, 0, 1000));
            case "thoughts" -> data.withThoughts(clamp(value, 0, ModAttachments.MAX_THOUGHTS));
            case "skill_points", "max_hp", "max_qi", "max_energy", "physical", "magic", "mental" -> data.debugSetStat(stat, value, levelDef);
            default -> data;
        };
    }

    private static String nextCultivationLevel(String current, int direction) {
        List<String> values = CultivationLevels.allLevels();
        int index = values.indexOf(current);
        if (index < 0) {
            index = 0;
        }
        int next = Math.floorMod(index + (direction < 0 ? -1 : 1), values.size());
        return values.get(next);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
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
                        data.activeCultivationMethod(),
                        data.blood(),
                        data.knownSpells(),
                        data.spellProficiencies(),
                        data.methodProficiencies(),
                        data.isMeditating(),
                        data.skillPoints(),
                        data.maxHpBonus(),
                        data.maxQiBonus(),
                        data.maxEnergyBonus(),
                        data.physicalAttack(),
                        data.magicAttack(),
                        data.mentalAttack(),
                        data.yuqiControlAllMode()
                )
        );
        PlayerShieldManager.sync(player);
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
        long need = CultivationLevels.getTotalQiNeeded(data.cultivationLevel());

        if (CultivationMethods.isAtOrPastLimit(data.activeCultivationMethod(), data.cultivationLevel())) {
            player.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".method_limit_reached"));
            return;
        }

        if (data.cultivationProgress() < need && CultivationMethods.isBloodDemon(data.activeCultivationMethod()) && data.blood() >= 10) {
            long missing = need - data.cultivationProgress();
            long progressFromBlood = Math.min(missing, data.blood() / 10L);
            int bloodToUse = (int) (progressFromBlood * 10L);
            data = data.withBlood(data.blood() - bloodToUse)
                    .withCultivationProgress(Math.min(need, data.cultivationProgress() + progressFromBlood));
            ModAttachments.setData(player, data);
        }

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

        if (isZhujiToJindan(data.cultivationLevel(), next)) {
            player.getPersistentData().putBoolean(JINDAN_CHALLENGE_PENDING_TAG, true);
            PacketDistributor.sendToPlayer(player, new ModPayloads.ClientboundOpenJindanChallengePayload());
            return;
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

    public static void completeJindanBreakthrough(ServerPlayer player, float averageScale, int lessThanOne, int atOne) {
        var tag = player.getPersistentData();
        if (!tag.getBoolean(JINDAN_CHALLENGE_PENDING_TAG)) {
            return;
        }

        var data = ModAttachments.getData(player);
        String next = CultivationLevels.getNextStage(data.cultivationLevel());
        if (!isZhujiToJindan(data.cultivationLevel(), next)) {
            tag.remove(JINDAN_CHALLENGE_PENDING_TAG);
            return;
        }

        var levelDef = CultivationLevels.getLevelDef(data.cultivationLevel());
        int maxQi = getEffectiveMaxQi(data, levelDef);
        long need = CultivationLevels.getTotalQiNeeded(data.cultivationLevel());
        if (data.qi() < maxQi || data.cultivationProgress() < need) {
            player.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".breakthrough_requirements_missing"));
            syncPlayerData(player);
            return;
        }

        tag.remove(JINDAN_CHALLENGE_PENDING_TAG);
        String quality = jindanQuality(averageScale);
        tag.putString(JINDAN_QUALITY_TAG, quality);

        ModAttachments.setData(player,
                data.withCultivationLevel(next)
                        .withQi(0)
                        .withCultivationProgress(0)
                        .withMeditating(false)
                        .withAddedSkillPoints(10));

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0D, player.getZ(), 90, 0.8D, 0.9D, 0.8D, 0.05D);
            serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.0D, player.getZ(), 45, 0.6D, 0.8D, 0.6D, 0.08D);
        }

        player.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".breakthrough_success", next));
        player.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".jindan_quality",
                Component.translatable("jindan_quality." + ImmortalCultivationMod.MODID + "." + quality),
                String.format(java.util.Locale.ROOT, "%.2f", averageScale)));
        syncPlayerData(player);
    }

    private static boolean isZhujiToJindan(String current, String next) {
        return (CultivationLevels.REALM_ZHUJI + CultivationLevels.STAGE_LATE).equals(current)
                && (CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_EARLY).equals(next);
    }

    private static String jindanQuality(float averageScale) {
        float distance = Math.abs(averageScale - 1.0F);
        if (distance <= 0.01F) {
            return "jiuzhuan";
        }
        if (distance <= 0.05F) {
            return "yuanman";
        }
        if (distance <= 0.1F) {
            return "jipin";
        }
        if (distance <= 0.2F) {
            return "shangpin";
        }
        if (distance <= 0.3F) {
            return "zhongpin";
        }
        if (distance <= 0.5F) {
            return "xiapin";
        }
        return "liedan";
    }

    private static void applyLevelStats(Player player, ModAttachments.CultivationData data, CultivationLevels.LevelDef levelDef) {
        ImmortalCultivationMod.raiseMaxHealthAttributeLimit();
        int maxHp = getEffectiveMaxHp(data, levelDef);
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            double previousMax = maxHealth.getValue();
            if (maxHealth.getBaseValue() > 20.0D) {
                maxHealth.setBaseValue(20.0D);
            }
            double cultivationBonus = maxHp - 20.0D;
            AttributeModifier existingModifier = maxHealth.getModifier(CULTIVATION_MAX_HEALTH_MODIFIER);
            double existingBonus = existingModifier == null ? 0.0D : existingModifier.amount();
            if (Math.abs(existingBonus - cultivationBonus) > 0.001D) {
                maxHealth.removeModifier(CULTIVATION_MAX_HEALTH_MODIFIER);
                if (Math.abs(cultivationBonus) > 0.001D) {
                    maxHealth.addPermanentModifier(new AttributeModifier(
                            CULTIVATION_MAX_HEALTH_MODIFIER,
                            cultivationBonus,
                            AttributeModifier.Operation.ADD_VALUE
                    ));
                }
            }

            double currentMax = maxHealth.getValue();
            if (player.getHealth() > currentMax) {
                player.setHealth((float) currentMax);
            } else if (previousMax <= 1.0D && player.getHealth() <= 1.0F) {
                player.setHealth((float) currentMax);
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

        if (!spendQiOrBlood(sp, data, 8)) {
            sp.removeEffect(ModEffects.EARTH_ESCAPE);
            sp.noPhysics = false;
            sp.setNoGravity(false);
            EARTH_ESCAPE_GRACE_TICKS.remove(sp.getUUID());
            syncPlayerData(sp);
            return;
        }

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

    public static int getMaxBlood(Player player) {
        var data = ModAttachments.getData(player);
        return getEffectiveMaxQi(data, CultivationLevels.getLevelDef(data.cultivationLevel())) * 10;
    }

    public static boolean spendQiOrBlood(Player player, ModAttachments.CultivationData data, int cost) {
        if (data.qi() >= cost) {
            ModAttachments.setData(player, data.withQi(data.qi() - cost));
            return true;
        }
        int missing = cost - data.qi();
        if (CultivationMethods.isBloodDemon(data.activeCultivationMethod()) && data.blood() >= missing) {
            ModAttachments.setData(player, data.withQi(0).withBlood(data.blood() - missing));
            return true;
        }
        return false;
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
            player.removeEffect(ModEffects.SPIRIT_SIGHT);
            syncPlayerData(player);
            return;
        }

        var data = ModAttachments.getData(player);
        if (!spendQiOrBlood(player, data, 5)) {
            player.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".not_enough_qi"));
            return;
        }

        SPIRIT_SIGHT_ACTIVE.put(id, true);
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, false, false, true));
        player.addEffect(new MobEffectInstance(ModEffects.SPIRIT_SIGHT, 260, 0, false, false, true));
        syncPlayerData(player);
    }

    private static void handleSpiritSightTick(ServerPlayer player, ModAttachments.CultivationData data) {
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, false, false, true));
        player.addEffect(new MobEffectInstance(ModEffects.SPIRIT_SIGHT, 260, 0, false, false, true));
        if (player.tickCount % 20 != 0) {
            return;
        }

        if (!spendQiOrBlood(player, data, 5)) {
            SPIRIT_SIGHT_ACTIVE.remove(player.getUUID());
            player.removeEffect(MobEffects.NIGHT_VISION);
            player.removeEffect(ModEffects.SPIRIT_SIGHT);
            player.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".not_enough_qi"));
            syncPlayerData(player);
            return;
        }

        syncPlayerData(player);
    }

    public static int startFogMirrorReveal(ServerPlayer player) {
        List<BlockPos> positions = findFogEscapeBlocks(player, 32);
        if (!positions.isEmpty()) {
            FOG_REVEALS.put(player.getUUID(), new FogReveal(positions, 60));
        }
        return positions.size();
    }

    public static AmbientQi refreshAmbientQi(ServerPlayer player) {
        AmbientQi ambientQi = scanAmbientQi(player, AMBIENT_QI_SCAN_RADIUS, player.tickCount);
        AMBIENT_QI_CACHE.put(player.getUUID(), ambientQi);
        return ambientQi;
    }

    private static AmbientQi getAmbientQi(Player player) {
        AmbientQi cached = AMBIENT_QI_CACHE.get(player.getUUID());
        if (cached != null && player.tickCount - cached.lastScanTick() < AMBIENT_QI_CACHE_TICKS) {
            return cached;
        }

        AmbientQi ambientQi = scanAmbientQi(player, AMBIENT_QI_SCAN_RADIUS, player.tickCount);
        AMBIENT_QI_CACHE.put(player.getUUID(), ambientQi);
        return ambientQi;
    }

    private static AmbientQi scanAmbientQi(Player player, int radius, int tickCount) {
        int centers = countSpiritVeinCenters(player.level(), player.blockPosition(), radius);
        return new AmbientQi(centers * AMBIENT_QI_PER_SPIRIT_VEIN_CENTER, centers, tickCount);
    }

    private static int countSpiritVeinCenters(Level level, BlockPos center, int radius) {
        int count = 0;
        BlockPos min = center.offset(-radius, -radius, -radius);
        BlockPos max = center.offset(radius, radius, radius);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (level.isLoaded(pos) && isSpiritVeinCenter(level.getBlockState(pos))) {
                count++;
            }
        }
        return count;
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

    private static boolean isSpiritVeinCenter(net.minecraft.world.level.block.state.BlockState state) {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return key != null && "magic_ritual_mod".equals(key.getNamespace()) && "spirit_vein_center".equals(key.getPath());
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

    public record AmbientQi(int value, int spiritVeinCenters, int lastScanTick) {}

    private static void broadcastMeditationState(ServerPlayer player, boolean meditating) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                new ModPayloads.ClientboundMeditationStatePayload(player.getUUID(), meditating));
    }
}
