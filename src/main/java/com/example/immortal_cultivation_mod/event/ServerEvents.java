package com.example.immortal_cultivation_mod.event;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.achievement.NoobAchievement;
import com.example.immortal_cultivation_mod.attachment.BodyTypes;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.attachment.SpiritRoots;
import com.example.immortal_cultivation_mod.combat.CombatStateManager;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.entity.CultivatorCorpseEntity;
import com.example.immortal_cultivation_mod.entity.CultivationNpc;
import com.example.immortal_cultivation_mod.entity.BloodDropEntity;
import com.example.immortal_cultivation_mod.entity.BloodApeDemonGeneralEntity;
import com.example.immortal_cultivation_mod.entity.PhantomVeilFoxEntity;
import com.example.immortal_cultivation_mod.entity.CrimsonScaleFlameLizardEntity;
import com.example.immortal_cultivation_mod.entity.CrimsonSkyFlameFloodDragonEntity;
import com.example.immortal_cultivation_mod.entity.EscapedYuanyingCoreEntity;
import com.example.immortal_cultivation_mod.entity.JindanEntity;
import com.example.immortal_cultivation_mod.entity.DefensePiercingProjectile;
import com.example.immortal_cultivation_mod.entity.ModEntities;
import com.example.immortal_cultivation_mod.entity.NpcProfile;
import com.example.immortal_cultivation_mod.entity.SoulEntity;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.item.BlackTortoiseHeartguardMirrorItem;
import com.example.immortal_cultivation_mod.item.CultivationAccessoryItem;
import com.example.immortal_cultivation_mod.item.FuchenItem;
import com.example.immortal_cultivation_mod.item.ForgingSystem;
import com.example.immortal_cultivation_mod.item.PhantomVeilFurItem;
import com.example.immortal_cultivation_mod.item.HeavyIronBracerItem;
import com.example.immortal_cultivation_mod.item.MysteriousRingItem;
import com.example.immortal_cultivation_mod.item.QiInfusedWeapon;
import com.example.immortal_cultivation_mod.item.RestrictionInsightItem;
import com.example.immortal_cultivation_mod.item.WhipItem;
import com.example.immortal_cultivation_mod.network.ModPayloads;
import com.example.immortal_cultivation_mod.particle.MethodQiParticleEffects;
import com.example.immortal_cultivation_mod.spell.BloodQiShield;
import com.example.immortal_cultivation_mod.spell.BodyCultivation;
import com.example.immortal_cultivation_mod.spell.ChangqingJue;
import com.example.immortal_cultivation_mod.spell.CultivationShieldSpells;
import com.example.immortal_cultivation_mod.spell.DielangShield;
import com.example.immortal_cultivation_mod.spell.DariBatiJue;
import com.example.immortal_cultivation_mod.spell.FentianLifeRenewal;
import com.example.immortal_cultivation_mod.spell.Fengya;
import com.example.immortal_cultivation_mod.spell.GushiShield;
import com.example.immortal_cultivation_mod.spell.GangtingXiaokongLu;
import com.example.immortal_cultivation_mod.spell.HanlanNingyuanJing;
import com.example.immortal_cultivation_mod.spell.XuanyueSuifengJue;
import com.example.immortal_cultivation_mod.spell.HantiBingqin;
import com.example.immortal_cultivation_mod.spell.HutiQi;
import com.example.immortal_cultivation_mod.spell.LightBeamAttack;
import com.example.immortal_cultivation_mod.spell.JindanCreativeFlight;
import com.example.immortal_cultivation_mod.spell.MichenZhang;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import com.example.immortal_cultivation_mod.spell.PlayerShieldManager;
import com.example.immortal_cultivation_mod.spell.SifangJie;
import com.example.immortal_cultivation_mod.spell.ShixuChiliu;
import com.example.immortal_cultivation_mod.spell.SlidingWater;
import com.example.immortal_cultivation_mod.spell.UndeadControl;
import com.example.immortal_cultivation_mod.spell.UndeadAbilitySystem;
import com.example.immortal_cultivation_mod.spell.Weiya;
import com.example.immortal_cultivation_mod.spell.WindStep;
import com.example.immortal_cultivation_mod.spell.XuyingTa;
import com.example.immortal_cultivation_mod.spell.YufengJue;
import com.example.immortal_cultivation_mod.system.LifeSimulationSystem;
import com.example.immortal_cultivation_mod.system.OuterSystemManager;
import com.example.immortal_cultivation_mod.world.JindanPocketWorlds;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
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
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import top.theillusivec4.curios.api.CuriosApi;

@EventBusSubscriber(modid = ImmortalCultivationMod.MODID)
public class ServerEvents {
    private static final int LIVING_CORPSE_RETALIATION_TICKS = 100;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingCorpseTargeted(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof Mob attacker)
                || attacker instanceof CultivationNpc
                || !(attacker instanceof Enemy)
                || !(event.getNewAboutToBeSetTarget() instanceof ServerPlayer target)
                || !CultivationAccessoryItem.isLivingCorpse(target)) {
            return;
        }
        boolean recentlyProvoked = attacker.getLastHurtByMob() == target
                && attacker.getLastHurtByMobTimestamp()
                        + LIVING_CORPSE_RETALIATION_TICKS >= attacker.tickCount;
        if (!recentlyProvoked) {
            event.setNewAboutToBeSetTarget(null);
        }
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        JindanPocketWorlds.loadSavedPockets(event.getServer());
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity().level().isClientSide) return;
        ItemStack output = event.getCrafting();
        if (output.getItem() instanceof com.example.immortal_cultivation_mod.item.TalismanItem) {
            com.example.immortal_cultivation_mod.item.TalismanItem.rollCraftedQuality(
                    output, event.getEntity().getRandom());
        }
        if (!output.is(ModItems.QI_POUCH.get())) return;
        int weightedPurity = 0;
        int totalWeight = 0;
        for (int slot = 0; slot < event.getInventory().getContainerSize(); slot++) {
            ItemStack ingredient = event.getInventory().getItem(slot);
            if (ingredient.isEmpty()
                    || !ForgingSystem.isPurityMaterial(ingredient)) continue;
            int ingredientPurity;
            if (ForgingSystem.hasDeterminedPurity(ingredient)) {
                ingredientPurity = ForgingSystem.purity(ingredient);
            } else {
                ResourceLocation id = BuiltInRegistries.ITEM
                        .getKey(ingredient.getItem());
                int seed = id.hashCode() * 31
                        + event.getEntity().getUUID().hashCode() * 17
                        + slot * 13;
                ingredientPurity = 1 + Math.floorMod(seed, 100);
            }
            int weight = Math.max(1, ingredient.getCount());
            weightedPurity += ingredientPurity * weight;
            totalWeight += weight;
        }
        if (totalWeight > 0) {
            com.example.immortal_cultivation_mod.item.QiPouchStorage
                    .setLatentPurity(output,
                            Math.round(weightedPurity / (float)totalWeight));
        }
    }

    @SubscribeEvent
    public static void onLevelTickPost(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            SlidingWater.tick(level);
        }
    }

    private static final ResourceLocation CULTIVATION_MAX_HEALTH_MODIFIER =
            ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "cultivation_max_health");
    private static final ResourceLocation LIVING_CORPSE_MAX_HEALTH_MODIFIER =
            ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "living_corpse_max_health");
    private static final ResourceLocation ULTRAMARINE_JADE_ID =
            ResourceLocation.fromNamespaceAndPath("ultramarine", "jade");
    private static final ResourceLocation CULTIVATION_ARMOR_MODIFIER =
            ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "cultivation_armor");
    private static final ResourceLocation HEAVY_IRON_BRACER_ARMOR_MODIFIER =
            ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "heavy_iron_bracer_armor");
    private static final ResourceLocation PATCHOULI_GUIDE_ITEM_ID =
            ResourceLocation.fromNamespaceAndPath("patchouli", "guide_book");
    private static final ResourceLocation PATCHOULI_BOOK_COMPONENT_ID =
            ResourceLocation.fromNamespaceAndPath("patchouli", "book");
    private static final ResourceLocation CULTIVATION_GUIDE_BOOK_ID =
            ResourceLocation.fromNamespaceAndPath(
                    ImmortalCultivationMod.MODID, "cultivation_manual");
    private static final ResourceLocation MAGIC_RITUAL_GUIDE_BOOK_ID =
            ResourceLocation.fromNamespaceAndPath(
                    ImmortalCultivationMod.MODID, "guide_book");
    private static final String JINDAN_CHALLENGE_PENDING_TAG = "ImmortalCultivationPendingJindanBreakthrough";
    private static final String JINDAN_QUALITY_TAG = "ImmortalCultivationJindanQuality";
    private static final String LAST_AGE_DAY_TAG = "ImmortalCultivationLastAgeDay";
    private static final String CULTIVATION_GUIDE_RECEIVED_TAG =
            "ImmortalCultivationGuideReceived";
    private static final String QI_FLOW_ACTIVE_UNTIL_TAG =
            "ImmortalCultivationQiFlowActiveUntil";
    private static final int QI_FLOW_AFTER_USE_TICKS = 30;
    private static final long TICKS_PER_INGAME_DAY = 24000L;
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
            "talent",
            "spirit_roots",
            "spirit_root_grade",
            "skill_points",
            "max_hp",
            "max_qi",
            "max_energy",
            "physical",
            "magic",
            "mental",
            "body_type",
            "restriction_hints",
            "simulation_attempts"
    );
    private static final List<String> SETTABLE_DEBUG_STATS = List.of(
            "age",
            "moral",
            "luck",
            "soul",
            "thoughts",
            "talent",
            "skill_points",
            "max_hp",
            "max_qi",
            "max_energy",
            "physical",
            "magic",
            "mental",
            "restriction_hints",
            "simulation_attempts"
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
                                                        IntegerArgumentType.getInteger(context, "value")))))))
                .then(Commands.literal("give")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.literal("simulation_attempts")
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(context -> giveSimulationAttempts(
                                                        context.getSource(),
                                                        EntityArgument.getPlayers(context, "targets"),
                                                        IntegerArgumentType.getInteger(context, "amount")))))))
                .then(Commands.literal("hints")
                        .then(Commands.literal("set")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(
                                                        0, RestrictionInsightItem.MAX_HINTS))
                                                .executes(context -> setRestrictionHints(
                                                        context.getSource(),
                                                        EntityArgument.getPlayers(context, "targets"),
                                                        IntegerArgumentType.getInteger(context, "amount")))))))
                .then(Commands.literal("unlock_all_spells")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> unlockAllSpells(
                                        context.getSource(),
                                        EntityArgument.getPlayers(context, "targets"))))));
    }

    @SubscribeEvent
    public static void onPlayerTickPre(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;

        if (player instanceof ServerPlayer serverPlayer) {
            HeavyIronBracerItem.syncOffhandCompanion(serverPlayer);
            CombatStateManager.lockMovement(serverPlayer);
            com.example.immortal_cultivation_mod.item.UtilityTalismanItem
                    .tickPlayer(serverPlayer);
            com.example.immortal_cultivation_mod.item.ManifestedWeaponItem
                    .tickPlayer(serverPlayer);
        }

        var data = ModAttachments.getData(player);
        if (player instanceof ServerPlayer serverPlayer) {
            ModPayloads.trackJindanRealmState(serverPlayer, data.cultivationLevel());
        }

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
            }
        } else {
            QI_GATHERING_ANCHORS.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onEntityTickPre(EntityTickEvent.Pre event) {
        if (!event.getEntity().level().isClientSide
                && event.getEntity() instanceof Projectile projectile) {
            CultivationShieldSpells.interceptProjectile(projectile);
        }
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        if (event.getEntity() instanceof Projectile projectile) {
            ShixuChiliu.holdIfFrozen(projectile);
        }
        if (event.getEntity() instanceof LivingEntity living && living.hasEffect(ModEffects.XINLIAO)) {
            living.setRemainingFireTicks(Math.max(living.getRemainingFireTicks(), 40));
        }
        if (event.getEntity() instanceof LivingEntity living) {
            HanlanNingyuanJing.tickDampVfx(living);
        }
        if (event.getEntity() instanceof PathfinderMob mob) {
            NpcProfile.clearAccidentalVanillaProfile(mob);
            XuanyueSuifengJue.tickNpc(mob);
        }
        if (event.getEntity() instanceof ItemEntity itemEntity) {
            if (com.example.immortal_cultivation_mod.item.ManifestedWeaponItem
                    .isManifested(itemEntity.getItem())) {
                com.example.immortal_cultivation_mod.item.ManifestedWeaponItem
                        .recoverDropped(itemEntity);
                return;
            }
            if (com.example.immortal_cultivation_mod.item.QiPouchStorage
                    .tickLinkedItemEntity(itemEntity)) {
                return;
            }
            if (HeavyIronBracerItem.isDebugStack(itemEntity.getItem())
                    || HeavyIronBracerItem.isDebugChestplate(itemEntity.getItem())) {
                itemEntity.discard();
                return;
            }
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
        UndeadAbilitySystem.tickEntityState(living);
        UndeadControl.tick(living);
        com.example.immortal_cultivation_mod.entity.FireballTalismanProjectileEntity
                .tickTalismanEffects(living);
        com.example.immortal_cultivation_mod.spell.SpiritSealSystem.tick(living);
        FuchenItem.tick(living);
        WhipItem.tickWallImpact(living);
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
            MethodQiParticleEffects.tick(serverPlayer);
            CombatStateManager.tick(serverPlayer);
            NoobAchievement.tick(serverPlayer);
            MysteriousRingItem.tick(serverPlayer);
            CultivationAccessoryItem.tickPlayer(serverPlayer);
            BlackTortoiseHeartguardMirrorItem.tick(serverPlayer);
            com.example.immortal_cultivation_mod.item.QiPouchWorld
                    .tickPlayer(serverPlayer);
        }

        var data = ModAttachments.getData(player);
        if (player instanceof ServerPlayer serverPlayer
                && CultivationAccessoryItem.isLivingCorpse(serverPlayer)
                && data.isMeditating()) {
            stopMeditating(serverPlayer, data);
            data = ModAttachments.getData(player);
        }

        var levelDef = CultivationLevels.getLevelDef(data.cultivationLevel());
        int maxQi = getEffectiveMaxQi(data, levelDef);
        applyLevelStats(player, data, levelDef);
        updateHeavyIronBracerArmor(player);
        data = handleThoughtsTick(player, data);
        if (player instanceof ServerPlayer sp) {
            data = handleDailyAgeTick(sp, data);
        }

        int maxAge = levelDef.maxAge() - data.agePenalty();
        if (maxAge <= 0) {
            if (player instanceof ServerPlayer serverPlayer) {
                if (CultivationAccessoryItem.isLivingCorpse(serverPlayer)
                        || CultivationAccessoryItem.tryBecomeLivingCorpse(serverPlayer)) {
                    return;
                }
                HeavyIronBracerItem.restoreStoredOffhand(serverPlayer);
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
                && !CultivationShieldSpells.blocksNaturalQiRegen(player)
                && !CultivationLevels.isMortal(data.cultivationLevel()) && maxQi > 0 && data.qi() < maxQi && player.tickCount % 20 == 0) {
            AmbientQi ambientQi = getAmbientQi(player);
            int regen = Math.max(1, player.hasEffect(ModEffects.QI_GATHERING) ? maxQi * 5 / 100 : maxQi / 100);
            regen = applyAmbientQiRegenBonus(regen, ambientQi);
            int forgedQiRegen = com.example.immortal_cultivation_mod.item.ForgingSystem.heldQiRegenBonusPercent(player);
            if (forgedQiRegen > 0) {
                regen += Math.max(1, regen * forgedQiRegen / 100);
            }
            int restrictionQiRegen = com.example.immortal_cultivation_mod.item.WeaponRestrictionSystem
                    .heldQiRegenBonusPercent(player);
            if (restrictionQiRegen > 0) {
                regen += Math.max(1, regen * restrictionQiRegen / 100);
            }
            regen = com.example.immortal_cultivation_mod.magicritual.center.CenterEffectSupport
                    .adjustNaturalQiRegen(player, regen);
            if (player instanceof ServerPlayer serverPlayer) {
                regen = ChangqingJue.adjustQiRegen(serverPlayer, regen);
            }
            regen = com.example.immortal_cultivation_mod.spell.SpiritSealSystem
                    .adjustNaturalQiRegen(player, regen);
            regen = UndeadAbilitySystem.adjustQiRegen(player, regen);
            regen = com.example.immortal_cultivation_mod.entity.CultivationBestiaryBeastEntity
                    .adjustQiRegeneration(player, regen);
            ModAttachments.setData(player, data.withQi(Math.min(maxQi, data.qi() + regen)));
            data = ModAttachments.getData(player);
        }

        if (player instanceof ServerPlayer sp && SPIRIT_SIGHT_ACTIVE.containsKey(player.getUUID())) {
            handleSpiritSightTick(sp, data);
            data = ModAttachments.getData(player);
        }

        if (data.isMeditating() && player.tickCount % 20 == 0 && CultivationMethods.get(data.activeCultivationMethod()) != null) {
            int meditationSpeed = getMeditationSpeedMultiplier(player)
                    * (player instanceof ServerPlayer serverPlayer
                    ? CultivationAccessoryItem.proficiencyMultiplier(serverPlayer) : 1);
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
                progressGain = com.example.immortal_cultivation_mod.alchemy.AlchemyEffects.cultivationGain(player, progressGain);
                ModAttachments.setData(player,
                        data.withCultivationProgress(Math.min(need, data.cultivationProgress() + progressGain)));
            }
        }

        if (player.tickCount % 40 == 0 && player instanceof ServerPlayer sp) {
            syncPlayerData(sp);
        }

        if (player instanceof ServerPlayer sp) {
            JindanTribulation.tick(sp);
            com.example.immortal_cultivation_mod.item.IdentityItems.tickIdentityProgress(sp);
            com.example.immortal_cultivation_mod.item.IdentityItems.tickQiRecovery(sp);
            WindStep.tick(sp);
            YufengJue.tick(sp);
            JindanCreativeFlight.tick(sp);
            ChangqingJue.tick(sp);
            FentianLifeRenewal.tick(sp);
            HantiBingqin.tick(sp);
            DariBatiJue.tick(sp);
            GangtingXiaokongLu.tick(sp);
            XuanyueSuifengJue.tick(sp);
            MichenZhang.tick(sp);
            SifangJie.tick(sp);
            Weiya.tick(sp);
            ShixuChiliu.tick(sp);
            XuyingTa.tick(sp);
            DielangShield.tick(sp);
            CultivationShieldSpells.tick(sp);
            QiInfusedWeapon.tickAutoChannel(sp);
            QiInfusedWeapon.tickOverflow(sp);
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
            CultivationShieldSpells.clear(sp);
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

    private static ModAttachments.CultivationData handleDailyAgeTick(ServerPlayer player, ModAttachments.CultivationData data) {
        long currentDay = player.server.overworld().getDayTime() / TICKS_PER_INGAME_DAY;
        var persistentData = player.getPersistentData();
        if (!persistentData.contains(LAST_AGE_DAY_TAG)) {
            persistentData.putLong(LAST_AGE_DAY_TAG, currentDay);
            return data;
        }

        long lastDay = persistentData.getLong(LAST_AGE_DAY_TAG);
        if (currentDay <= lastDay) {
            return data;
        }

        long passedDays = currentDay - lastDay;
        int ageLoss = (int) Math.min(Integer.MAX_VALUE, passedDays);
        ModAttachments.CultivationData updated = data.withAgePenalty(Math.max(0, data.agePenalty() + ageLoss));
        ModAttachments.setData(player, updated);
        persistentData.putLong(LAST_AGE_DAY_TAG, currentDay);
        return updated;
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

    private static void spawnNpcCorpse(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        ResourceLocation texture = npcCorpseTexture(entity);
        if (texture == null) {
            return;
        }
        CultivatorCorpseEntity corpse = ModEntities.CULTIVATOR_CORPSE.get().create(serverLevel);
        if (corpse == null) {
            return;
        }
        corpse.copyFromNpc(entity, entity.getName().getString(), texture);
        corpse.getPersistentData().putBoolean(UndeadControl.TAG_GRUDGE_ROLLED, true);
        corpse.getPersistentData().putBoolean(UndeadControl.TAG_HAS_GRUDGE, true);
        serverLevel.addFreshEntity(corpse);
    }

    private static ResourceLocation npcCorpseTexture(LivingEntity entity) {
        if (entity instanceof PathfinderMob mob && mob instanceof CultivationNpc npc) {
            return NpcProfile.skinTexture(mob, npc.npcArchetype());
        }
        return null;
    }

    private static void spawnSoul(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        SoulEntity soul = ModEntities.SOUL.get().create(serverLevel);
        if (soul == null) {
            return;
        }
        if (entity instanceof ServerPlayer player) {
            soul.copyFrom(player);
        } else if (entity instanceof PathfinderMob mob && mob instanceof CultivationNpc) {
            soul.copyFromNpc(entity, npcCorpseTexture(entity));
        } else {
            return;
        }
        serverLevel.addFreshEntity(soul);
    }

    /** Resolve revival before Luck, and before any death cleanup or kill rewards. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void preventProtectedDeath(LivingDeathEvent event) {
        if (com.example.immortal_cultivation_mod.pagoda.PagodaSystem.adminKill(event.getEntity(),event.getSource())) return;
        if (event.isCanceled() || event.getEntity().level().isClientSide) return;
        if (event.getEntity() instanceof ServerPlayer player
                && com.example.immortal_cultivation_mod.item.IdentityItems.tryUseProtectivePendant(player)) {
            event.setCanceled(true);
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player
                && !CultivationAccessoryItem.isLivingCorpse(player)) {
            var data = ModAttachments.getData(player);
            var levelDef = CultivationLevels.getLevelDef(data.cultivationLevel());
            var agedData = data.withAgePenalty(data.agePenalty() + 10)
                    .withQi(getEffectiveMaxQi(data, levelDef));
            if (levelDef.maxAge() - agedData.agePenalty() <= 0
                    && CultivationAccessoryItem.hasSoulAnchorPearl(player)) {
                ModAttachments.setData(player, agedData);
                if (CultivationAccessoryItem.tryBecomeLivingCorpse(player)) {
                    player.setHealth(1.0F);
                    event.setCanceled(true);
                    return;
                }
            }
        }
        if (event.getEntity() instanceof PathfinderMob mob
                && mob instanceof CultivationNpc
                && NpcProfile.tryUseRoyalPendant(mob)) {
            event.setCanceled(true);
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player && FentianLifeRenewal.tryCatchDeath(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.isCanceled()) return;
        if (event.getEntity() instanceof ServerPlayer player) {
            com.example.immortal_cultivation_mod.item.ManifestedWeaponItem
                    .removeAll(player);
            HeavyIronBracerItem.restoreStoredOffhand(player);
        }
        if (!event.getEntity().level().isClientSide
                && event.getSource().getEntity() instanceof ServerPlayer playerKiller) {
            MysteriousRingItem.onEntityKilled(playerKiller, event.getEntity());
        }
        if (!event.getEntity().level().isClientSide) {
            if (event.getSource().getEntity() instanceof LivingEntity killer
                    && UndeadControl.hasUndeadPowers(killer)) {
                float healing = event.getEntity().getMaxHealth() * 0.5F;
                if (killer instanceof ServerPlayer livingCorpse
                        && CultivationAccessoryItem.isLivingCorpse(livingCorpse)) {
                    CultivationAccessoryItem.healLivingCorpseFromUndeadAbility(
                            livingCorpse, healing);
                } else {
                    killer.heal(healing);
                }
            }
            if (event.getEntity() instanceof PathfinderMob mob
                    && mob instanceof CultivationNpc) {
                NpcProfile.releaseCorpseSummon(mob);
                LivingEntity killer = event.getSource().getEntity() instanceof LivingEntity living
                        ? living : null;
                if (killer instanceof ServerPlayer playerKiller) {
                    com.example.immortal_cultivation_mod.item.IdentityItems.recordNpcKill(playerKiller);
                    NpcProfile.grantWantedReward(mob, playerKiller);
                }
            }
            spawnNpcCorpse(event.getEntity());
        }
        if (!event.getEntity().level().isClientSide
                && !(event.getEntity() instanceof Player)
                && !(event.getEntity() instanceof CultivatorCorpseEntity)) {
            long amount = Math.max(1L, (long) (event.getEntity().getMaxHealth() / 10.0F));
            event.getEntity().level().addFreshEntity(new BloodDropEntity(
                    event.getEntity().level(),
                    event.getEntity().getX(),
                    event.getEntity().getY() + 0.35D,
                    event.getEntity().getZ(),
                    amount));
        }
        if (event.getEntity() instanceof ServerPlayer player) {
            NoobAchievement.recordDeath(player);
            CombatStateManager.clear(player);
            JindanTribulation.failOnDeath(player);
            JindanTribulation.clear(player);
            MEDITATION_ANCHORS.remove(player.getUUID());
            QI_GATHERING_ANCHORS.remove(player.getUUID());
            EARTH_ESCAPE_GRACE_TICKS.remove(player.getUUID());
            SPIRIT_SIGHT_ACTIVE.remove(player.getUUID());
            player.removeEffect(ModEffects.SPIRIT_SIGHT);
            FOG_REVEALS.remove(player.getUUID());
            AMBIENT_QI_CACHE.remove(player.getUUID());
            WindStep.clear(player);
            YufengJue.clear(player);
            ShixuChiliu.clear(player);
            FentianLifeRenewal.clear(player);
            HantiBingqin.clear(player);
            DielangShield.clear(player);
            BloodQiShield.clear(player);
            HutiQi.clear(player);
            GushiShield.clear(player);
            CultivationShieldSpells.clear(player);
            SifangJie.clear(player);
            BlackTortoiseHeartguardMirrorItem.clear(player);
            PlayerShieldManager.clearAll(player);
            var data = ModAttachments.getData(player);
            var levelDef = CultivationLevels.getLevelDef(data.cultivationLevel());
            int maxQi = getEffectiveMaxQi(data, levelDef);
            var agedData = data.withAgePenalty(data.agePenalty() + 10).withQi(maxQi);
            ModAttachments.setData(player, agedData);
            if (levelDef.maxAge() - agedData.agePenalty() <= 0) {
                CultivationAccessoryItem.clearLivingCorpse(player);
                spawnAgeExhaustedCorpse(player);
                spawnSoul(player);
                resetPlayer(player);
            } else {
                syncPlayerData(player);
            }
        }
        if (!event.getEntity().level().isClientSide
                && !(event.getEntity() instanceof ServerPlayer)
                && !event.getEntity().getPersistentData().getBoolean(
                com.example.immortal_cultivation_mod.item.ShihunfanItem.CAPTURED_SOUL_TAG)) {
            spawnSoul(event.getEntity());
        }
        restoreQiFromFormationKill(event);
        event.getEntity().getPersistentData().remove(
                com.example.immortal_cultivation_mod.item.ShihunfanItem.CAPTURED_SOUL_TAG);
    }

    private static void restoreQiFromFormationKill(LivingDeathEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel)
                || !(event.getSource().getEntity() instanceof ServerPlayer killer)
                || event.getEntity() == killer) {
            return;
        }
        int restored = 0;
        for (var center : com.example.immortal_cultivation_mod.magicritual.center
                .CenterEffectSupport.centersContaining(event.getEntity())) {
            if (!com.example.immortal_cultivation_mod.magicritual.center
                    .CenterTargetPolicy.isAuthorizedPlayer(center, killer)) {
                continue;
            }
            restored += center.getQiReturnAmount();
        }
        if (restored <= 0) {
            return;
        }
        var data = ModAttachments.getData(killer);
        var levelDef = CultivationLevels.getLevelDef(data.cultivationLevel());
        int maxQi = getEffectiveMaxQi(data, levelDef);
        if (maxQi <= 0 || data.qi() >= maxQi) {
            return;
        }
        int amount = Math.min(restored, maxQi - data.qi());
        ModAttachments.setData(killer, data.withQi(data.qi() + amount));
        syncPlayerData(killer);
        killer.displayClientMessage(Component.translatable(
                "message." + ImmortalCultivationMod.MODID + ".qi_return_flag.restored",
                amount), true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDrops(LivingDropsEvent event) {
        if (UndeadAbilitySystem.suppressDrops(event.getEntity())) {
            event.getDrops().clear();
            return;
        }
        capBeastDemonCoreDrop(event);
        if (!(event.getEntity() instanceof PathfinderMob mob)
                || !(mob instanceof CultivationNpc)) {
            return;
        }
        ItemStack stack = NpcProfile.randomInventoryDrop(mob);
        if (stack.isEmpty()) {
            return;
        }
        ItemEntity drop = new ItemEntity(mob.level(), mob.getX(), mob.getY() + 0.35D, mob.getZ(), stack);
        drop.setDefaultPickUpDelay();
        event.getDrops().add(drop);
    }

    @SubscribeEvent
    public static void onLivingExperienceDrop(LivingExperienceDropEvent event) {
        if (UndeadAbilitySystem.suppressDrops(event.getEntity())) {
            event.setDroppedExperience(0);
        }
    }

    private static void capBeastDemonCoreDrop(LivingDropsEvent event) {
        LivingEntity dead = event.getEntity();
        if (!(dead instanceof CrimsonScaleFlameLizardEntity)
                && !(dead instanceof CrimsonSkyFlameFloodDragonEntity)
                && !(dead instanceof BloodApeDemonGeneralEntity)
                && !(dead instanceof PhantomVeilFoxEntity)
                && !(dead instanceof EscapedYuanyingCoreEntity)) {
            return;
        }
        if (dead instanceof PhantomVeilFoxEntity fox) {
            for (ItemEntity drop : event.getDrops()) {
                ItemStack stack = drop.getItem();
                if (stack.is(ModItems.PHANTOM_VEIL_FUR.get())) {
                    PhantomVeilFurItem.setRealm(
                            stack, fox.cultivationRealm());
                }
            }
        }
        List<ItemEntity> demonCores = event.getDrops().stream()
                .filter(drop -> drop.getItem().is(ModItems.LIANQI_YAODAN.get())
                        || drop.getItem().is(ModItems.ZHUJI_YAODAN.get())
                        || drop.getItem().is(ModItems.JINDAN_YAODAN.get())
                        || drop.getItem().is(ModItems.YUANYING_YAODAN.get())
                        || drop.getItem().is(ModItems.HUASHEN_YAODAN.get())
                        || drop.getItem().is(ModItems.LIANXU_YAODAN.get())
                        || drop.getItem().is(ModItems.HETI_YAODAN.get())
                        || drop.getItem().is(ModItems.DACHENG_YAODAN.get())
                        || drop.getItem().is(ModItems.DUJIE_YAODAN.get()))
                .toList();
        if (demonCores.isEmpty()) {
            return;
        }
        demonCores.getFirst().getItem().setCount(1);
        if (demonCores.size() > 1) {
            event.getDrops().removeAll(demonCores.subList(1, demonCores.size()));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void keepNoobHatOnDeath(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        event.getDrops().removeIf(
                drop -> drop.getItem().is(ModItems.SYSTEM.get()));
        if (NoobAchievement.hasReward(player)) {
            event.getDrops().removeIf(
                    drop -> drop.getItem().is(ModItems.NOOB_HAT.get()));
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            var data = ModAttachments.getData(sp);
            applyLevelStats(sp, data, CultivationLevels.getLevelDef(data.cultivationLevel()));
            sp.setHealth(sp.getMaxHealth());
            ShixuChiliu.clear(sp);
            HantiBingqin.clear(sp);
            DielangShield.clear(sp);
            BloodQiShield.clear(sp);
            HutiQi.clear(sp);
            GushiShield.clear(sp);
            CultivationShieldSpells.clear(sp);
            SifangJie.clear(sp);
            BlackTortoiseHeartguardMirrorItem.clear(sp);
            PlayerShieldManager.clearAll(sp);
            syncPlayerData(sp);
            ModPayloads.syncDebugEyeState(sp);
            syncRestrictionHints(sp);
            CombatStateManager.clear(sp);
            NoobAchievement.ensureReward(sp);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getOriginal().getPersistentData().contains(NpcProfile.PLAYER_GENDER_TAG)) {
            event.getEntity().getPersistentData().putString(
                    NpcProfile.PLAYER_GENDER_TAG,
                    event.getOriginal().getPersistentData().getString(NpcProfile.PLAYER_GENDER_TAG));
        }
        com.example.immortal_cultivation_mod.item.IdentityItems.copyPlayerIdentity(
                event.getOriginal(), event.getEntity());
        BodyCultivation.copyPersistentData(event.getOriginal(), event.getEntity());
        JindanEntity.copyGeneratedChunkCount(event.getOriginal(), event.getEntity());
        QiInfusedWeapon.copyPersistentData(event.getOriginal(), event.getEntity());
        com.example.immortal_cultivation_mod.magicritual.item.custom.ZhenpanItem.copyPersistentData(
                event.getOriginal(), event.getEntity());
        JindanCreativeFlight.copyPersistentData(event.getOriginal(), event.getEntity());
        com.example.immortal_cultivation_mod.item.RestrictionInsightItem.copyHints(
                event.getOriginal(), event.getEntity());
        NoobAchievement.copyPersistentData(event.getOriginal(), event.getEntity());
        if (event.getOriginal().getPersistentData()
                .getBoolean(CULTIVATION_GUIDE_RECEIVED_TAG)) {
            event.getEntity().getPersistentData()
                    .putBoolean(CULTIVATION_GUIDE_RECEIVED_TAG, true);
        }
        if (event.getOriginal().getPersistentData().contains(JINDAN_QUALITY_TAG)) {
            event.getEntity().getPersistentData().putString(
                    JINDAN_QUALITY_TAG,
                    event.getOriginal().getPersistentData().getString(JINDAN_QUALITY_TAG));
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            JindanTribulation.clear(player);
            HeavyIronBracerItem.restoreStoredOffhand(player);
            ModPayloads.clearPendingLearning(player);
            ShixuChiliu.clear(player);
            CultivationShieldSpells.clear(player);
            CombatStateManager.onLogout(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            if (!JindanPocketWorlds.isJindanDimension(sp.level().dimension())) {
                ModPayloads.cleanupJindanClones(sp);
            }
            var data = ModAttachments.getData(sp);
            ModPayloads.trackJindanRealmState(sp, data.cultivationLevel());
            if (SpiritRoots.needsRandomRoots(data)) {
                var roots = SpiritRoots.random(sp.getRandom());
                data = data.withSpiritRoots(roots.roots(), roots.grade());
                ModAttachments.setData(sp, data);
            }
            if (data.talent() < 0.1F || data.talent() > 1.0F) {
                data = data.withTalent(ModAttachments.CultivationData.randomTalent());
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
            ShixuChiliu.clear(sp);
            FentianLifeRenewal.clear(sp);
            HantiBingqin.clear(sp);
            DielangShield.clear(sp);
            BloodQiShield.clear(sp);
            HutiQi.clear(sp);
            GushiShield.clear(sp);
            CultivationShieldSpells.clear(sp);
            SifangJie.clear(sp);
            BlackTortoiseHeartguardMirrorItem.clear(sp);
            PlayerShieldManager.clearAll(sp);
            com.example.immortal_cultivation_mod.item.IdentityItems.ensurePlayerIdentity(sp);
            JindanCreativeFlight.resetControlSync(sp);
            syncPlayerData(sp);
            ModPayloads.syncDebugEyeState(sp);
            syncRestrictionHints(sp);
            CombatStateManager.sync(sp, true);
            NoobAchievement.ensureReward(sp);
            ensureCultivationGuide(sp);
        }
    }

    /** Gives the main cultivation manual once per player save. */
    private static void ensureCultivationGuide(ServerPlayer player) {
        if (player.getPersistentData()
                .getBoolean(CULTIVATION_GUIDE_RECEIVED_TAG)) {
            return;
        }
        ItemStack guide = createCultivationGuide();
        if (guide.isEmpty()) {
            // Patchouli is optional. Leave the flag unset so installing it
            // later still grants the book on the player's next login.
            return;
        }
        if (!player.addItem(guide)) {
            player.drop(guide, false);
        }
        player.getPersistentData()
                .putBoolean(CULTIVATION_GUIDE_RECEIVED_TAG, true);
    }

    private static ItemStack createCultivationGuide() {
        return createPatchouliGuide(CULTIVATION_GUIDE_BOOK_ID);
    }

    /** Creates the real Patchouli magic-ritual guide for NPC formation kits. */
    public static ItemStack createMagicRitualGuide() {
        return createPatchouliGuide(MAGIC_RITUAL_GUIDE_BOOK_ID);
    }

    private static ItemStack createPatchouliGuide(ResourceLocation bookId) {
        Item guideItem = BuiltInRegistries.ITEM.get(PATCHOULI_GUIDE_ITEM_ID);
        if (!PATCHOULI_GUIDE_ITEM_ID.equals(
                BuiltInRegistries.ITEM.getKey(guideItem))) {
            return ItemStack.EMPTY;
        }
        DataComponentType<?> bookComponent =
                BuiltInRegistries.DATA_COMPONENT_TYPE.get(
                        PATCHOULI_BOOK_COMPONENT_ID);
        if (bookComponent == null
                || !PATCHOULI_BOOK_COMPONENT_ID.equals(
                        BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(bookComponent))) {
            return ItemStack.EMPTY;
        }
        ItemStack guide = new ItemStack(guideItem);
        setDynamicComponent(guide, bookComponent, bookId);
        return guide;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void setDynamicComponent(
            ItemStack stack,
            DataComponentType<?> component,
            Object value) {
        stack.set((DataComponentType) component, value);
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && JindanPocketWorlds.isJindanDimension(event.getFrom())
                && !JindanPocketWorlds.isJindanDimension(event.getTo())) {
            ModPayloads.cleanupJindanClones(player);
            ModPayloads.finishPendingJindanReset(player);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (CombatStateManager.interceptTeleportUse(
                player, event.getHand(), event.getItemStack())) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }

        if (com.example.immortal_cultivation_mod.item.IdentityItems.poisonHeldFood(player, event.getHand())) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }

        if (com.example.immortal_cultivation_mod.item.IdentityItems.useWeaponCareStone(player, event.getHand())) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }

        boolean refiningSpiritSilk = event.getItemStack().is(
                com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_SILKWORM_SILK.get())
                && !player.isShiftKeyDown();
        if (!refiningSpiritSilk
                && ForgingSystem.isPurityMaterial(event.getItemStack())
                && !ForgingSystem.hasFixedPurity(event.getItemStack())) {
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
        ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey(item);
        int restoreAmount = magicRitualSpiritStoneRestoreAmount(itemKey);
        if (restoreAmount <= 0) {
            return;
        }

        var data = ModAttachments.getData(player);
        int maxQi = getEffectiveMaxQi(data, CultivationLevels.getLevelDef(data.cultivationLevel()));
        if (maxQi <= 0 || data.qi() >= maxQi) {
            return;
        }

        ModAttachments.setData(player, data.withQi(Math.min(maxQi, data.qi() + restoreAmount)));
        if (!player.getAbilities().instabuild && !isInfiniteMagicRitualSpiritStone(itemKey)) {
            event.getItemStack().shrink(1);
        }
        syncPlayerData(player);

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onFoodFinished(LivingEntityUseItemEvent.Finish event) {
        com.example.immortal_cultivation_mod.item.IdentityItems.applyPoisonedFoodEffect(
                event.getEntity(), event.getItem());
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        boolean spiritVeinCenter = event.getLevel().getBlockState(event.getPos()).is(
                com.example.immortal_cultivation_mod.magicritual.block.ModBlocks.SPIRIT_VEIN_CENTER.get());
        if (spiritVeinCenter
                && event.getItemStack().getItem()
                instanceof com.example.immortal_cultivation_mod.item.WoolStripItem) {
            ItemStack formed = new ItemStack(
                    com.example.immortal_cultivation_mod.magicritual.item.ModItems.PATTERNED_CLOTH.get());
            if (!player.getAbilities().instabuild) {
                event.getItemStack().shrink(1);
            }
            if (!player.getInventory().add(formed)) {
                player.drop(formed, false);
            }
            if (event.getLevel() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.ENCHANT,
                        event.getPos().getX() + 0.5D, event.getPos().getY() + 0.8D,
                        event.getPos().getZ() + 0.5D,
                        20, 0.35D, 0.25D, 0.35D, 0.06D);
            }
            player.displayClientMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID + ".patterned_cloth.created"), true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }
        if (ULTRAMARINE_JADE_ID.equals(BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem()))
                && spiritVeinCenter) {
            ItemStack formed = new ItemStack(
                    com.example.immortal_cultivation_mod.magicritual.item.ModItems.ZHENWEN_JADE.get());
            int puritySeed = (int)(event.getPos().asLong()
                    ^ player.getUUID().getLeastSignificantBits()
                    ^ event.getLevel().getGameTime());
            ForgingSystem.ensurePurity(formed, puritySeed);
            if (!player.getAbilities().instabuild) {
                event.getItemStack().shrink(1);
            }
            if (!player.getInventory().add(formed)) {
                player.drop(formed, false);
            }
            if (event.getLevel() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.ENCHANT,
                        event.getPos().getX() + 0.5D, event.getPos().getY() + 0.7D,
                        event.getPos().getZ() + 0.5D,
                        28, 0.35D, 0.35D, 0.35D, 0.08D);
            }
            player.displayClientMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID + ".zhenwen_jade.created"), true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }
        if (event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND
                && HeavyIronBracerItem.isRealStack(event.getItemStack())) {
            HeavyIronBracerItem.leftHandPunch(player, null);
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
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
        if (event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND
                && HeavyIronBracerItem.isRealStack(event.getItemStack())) {
            HeavyIronBracerItem.leftHandPunch(player, event.getTarget());
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
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
        if (itemKey == null || !"immortal_cultivation_mod".equals(itemKey.getNamespace())) {
            return 0;
        }
        return switch (itemKey.getPath()) {
            case "spirit_stone", "low_grade_spirit_stone", "low_spirit_stone", "lower_grade_spirit_stone" -> 10;
            case "middle_grade_spirit_stone", "medium_grade_spirit_stone", "mid_grade_spirit_stone", "middle_spirit_stone", "medium_spirit_stone" -> 100;
            case "high_grade_spirit_stone", "high_spirit_stone", "upper_grade_spirit_stone", "upper_spirit_stone" -> 1000;
            case "infinite_spirit_stone" -> 10000;
            default -> 0;
        };
    }

    public static boolean isInfiniteMagicRitualSpiritStone(ResourceLocation itemKey) {
        return itemKey != null
                && "immortal_cultivation_mod".equals(itemKey.getNamespace())
                && "infinite_spirit_stone".equals(itemKey.getPath());
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity().getMainHandItem()
                .is(ModItems.FLYING_NEEDLE.get())) {
            event.setCanceled(true);
            return;
        }
        if (event.getEntity().isShiftKeyDown()
                && event.getEntity().getMainHandItem().is(ModItems.SHIHUNFAN.get())) {
            event.setCanceled(true);
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player) {
            LivingEntity sparPartner = NpcProfile.sparPartner(player);
            if (sparPartner != null && (NpcProfile.isSparCountdown(player)
                    || !sparPartner.getUUID().equals(event.getTarget().getUUID()))) {
                event.setCanceled(true);
                return;
            }
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
            if (player.getMainHandItem().getItem() instanceof FuchenItem
                    && !CombatStateManager.isInCombat(player)) {
                FuchenItem.playNormalAttackAnimation(player);
            }
            if (CombatStateManager.isCombatWeapon(player.getMainHandItem())
                    && !CombatStateManager.isHeavyAttackInProgress(player)) {
                CombatStateManager.broadcastSwordCombo(player);
            }
            triggerLingbeng(player, event.getTarget().getX(), event.getTarget().getY(), event.getTarget().getZ());
            var data = ModAttachments.getData(player);
            if (data.physicalAttack() > 0) {
                event.getTarget().hurt(player.damageSources().playerAttack(player), (float)(data.physicalAttack() * com.example.immortal_cultivation_mod.pagoda.GourdSystem.multiplier(player)));
            }
            QiInfusedWeapon.strike(player, event.getTarget());
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getEntity() instanceof Player clicker)) {
            return;
        }
        if (clicker.getMainHandItem()
                .is(ModItems.FLYING_NEEDLE.get())) {
            event.setCanceled(true);
            return;
        }
        if (clicker.isShiftKeyDown() && clicker.getMainHandItem().is(ModItems.SHIHUNFAN.get())) {
            event.setCanceled(true);
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

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onCultivationArmorIncomingDamage(LivingIncomingDamageEvent event) {
        if (com.example.immortal_cultivation_mod.pagoda.PagodaSystem.adminKill(event.getEntity(),event.getSource())) return;
        if (event.getEntity() instanceof LivingEntity target) {
            if ((target instanceof BloodApeDemonGeneralEntity
                    || target instanceof CrimsonSkyFlameFloodDragonEntity)
                    && event.getSource().getEntity() instanceof LivingEntity attacker
                    && UndeadControl.hasUndeadPowers(attacker)
                    && UndeadControl.isAtLeastRank(attacker, "伏尸")) {
                event.addReductionModifier(
                        DamageContainer.Reduction.ARMOR,
                        (container, vanillaReduction) -> 0.0F);
                return;
            }
            addCultivationArmorReduction(event, target);
        }
    }

    private static void addCultivationArmorReduction(
            LivingIncomingDamageEvent event, LivingEntity target) {
        if (event.getSource().is(DamageTypeTags.BYPASSES_ARMOR)) {
            return;
        }
        AttributeInstance armor = target.getAttribute(Attributes.ARMOR);
        if (armor == null) {
            return;
        }
        double cultivationArmor = cultivationArmorValue(target, armor);
        float defenseBypass = Math.max(
                DefensePiercingProjectile.defenseBypass(event.getSource()),
                XuanyueSuifengJue.armorPenetration(event.getSource().getEntity()));
        boolean armorPiercing = defenseBypass > 0.0F;
        if (cultivationArmor <= 0.0D && !armorPiercing) {
            return;
        }

        // Realm armor scales far beyond CombatRules' 20-point effective cap. Treat
        // cultivation armor as flat per-hit protection (up to vanilla's 80% limit),
        // while ordinary equipment keeps its familiar vanilla armor calculation.
        event.addReductionModifier(DamageContainer.Reduction.ARMOR, (container, vanillaReduction) -> {
            float incomingDamage = Math.max(0.0F, container.getNewDamage());
            if (incomingDamage <= 0.0F) {
                return 0.0F;
            }
            double totalArmor = Math.max(0.0D, armor.getValue());
            double equipmentArmor = Math.max(0.0D, totalArmor - cultivationArmor);
            double toughness = Math.max(0.0D, target.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
            double equipmentReduction = equipmentArmorReduction(
                    incomingDamage, equipmentArmor, toughness);
            float effectiveness = (float) Math.min(0.8D,
                    (cultivationArmor + equipmentReduction) / incomingDamage);
            ItemStack weapon = container.getSource().getWeaponItem();
            if (weapon != null && target.level() instanceof ServerLevel serverLevel) {
                effectiveness = EnchantmentHelper.modifyArmorEffectiveness(
                        serverLevel, weapon, target, container.getSource(), effectiveness);
            }
            float bypass = Math.max(
                    DefensePiercingProjectile.defenseBypass(container.getSource()),
                    XuanyueSuifengJue.armorPenetration(
                            container.getSource().getEntity()));
            if (bypass > 0.0F) {
                effectiveness *= 1.0F - bypass;
            }
            effectiveness = Math.max(0.0F, Math.min(0.8F, effectiveness));
            return incomingDamage * effectiveness;
        });
    }

    private static double cultivationArmorValue(LivingEntity target, AttributeInstance armor) {
        if (target instanceof com.example.immortal_cultivation_mod.entity.XuanjinZhengEntity) {
            return Math.max(0.0D, armor.getValue());
        }
        if (target instanceof Player) {
            AttributeModifier modifier = armor.getModifier(CULTIVATION_ARMOR_MODIFIER);
            return modifier == null ? 0.0D : Math.max(0.0D, modifier.amount());
        }
        return target instanceof CultivationNpc ? Math.max(0.0D, armor.getBaseValue()) : 0.0D;
    }

    private static double equipmentArmorReduction(
            float damage, double armor, double toughness) {
        if (damage <= 0.0F || armor <= 0.0D) {
            return 0.0D;
        }
        double effectiveArmor = armor - damage / (2.0D + toughness / 4.0D);
        double minimumArmor = Math.min(20.0D, armor * 0.2D);
        effectiveArmor = Math.max(minimumArmor, Math.min(20.0D, effectiveArmor));
        return damage * Math.max(0.0D, Math.min(0.8D, effectiveArmor / 25.0D));
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (com.example.immortal_cultivation_mod.pagoda.PagodaSystem.adminKill(event.getEntity(),event.getSource())) return;
        boolean shieldBreakerAttack = event.getSource().is(DamageTypes.PLAYER_ATTACK)
                && event.getSource().getEntity() instanceof ServerPlayer attacker
                && CultivationAccessoryItem.hasShieldBreaker(attacker);
        if (shieldBreakerAttack && event.getEntity().isBlocking()) {
            event.getEntity().stopUsingItem();
            if (event.getSource().getEntity() instanceof ServerPlayer wielder) { com.example.immortal_cultivation_mod.guidance.DaoGuidanceObservations.record(wielder, "ring_triggered"); }
        }
        event.setAmount(CultivationShieldSpells.reflectedDamage(event.getSource(), event.getAmount()));
        event.setAmount(DariBatiJue.adjustIncomingDamage(
                event.getEntity(), event.getSource(), event.getAmount()));
        event.setAmount(GangtingXiaokongLu.adjustIncomingDamage(
                event.getEntity(), event.getSource(), event.getAmount()));
        event.setAmount(XuanyueSuifengJue.adjustIncomingDamage(
                event.getEntity(), event.getSource(), event.getAmount()));
        if (event.getEntity() instanceof LivingEntity abilityTarget) {
            if (event.getSource().getEntity() instanceof LivingEntity abilityAttacker) {
                event.setAmount(UndeadAbilitySystem.adjustOutgoingDamage(
                        abilityAttacker, abilityTarget, event.getSource(), event.getAmount()));
            }
            event.setAmount(UndeadAbilitySystem.adjustIncomingDamage(
                    abilityTarget, event.getSource(), event.getAmount()));
        }
        if (event.getEntity() instanceof LivingEntity target) {
            long dashProtection = target.getPersistentData().getLong(
                    com.example.immortal_cultivation_mod.spell.ElementalTechniques
                            .LIGHTNING_DASH_PROTECTION_UNTIL);
            if (dashProtection >= target.level().getGameTime()) {
                float reduction = target.getPersistentData().getFloat(
                        com.example.immortal_cultivation_mod.spell.ElementalTechniques
                                .LIGHTNING_DASH_DAMAGE_REDUCTION);
                if (reduction <= 0.0F) {
                    reduction = 0.50F;
                }
                event.setAmount(event.getAmount()
                        * (1.0F - Math.min(0.95F, reduction)));
            }
            LivingEntity attacker = NpcProfile.damageAttacker(event.getSource());
            if (attacker instanceof CultivationNpc && !NpcProfile.isCombatEnemy(attacker, target)) {
                event.setCanceled(true);
                return;
            }
            LivingEntity attackerPartner = attacker == null ? null : NpcProfile.sparPartner(attacker);
            LivingEntity targetPartner = NpcProfile.sparPartner(target);
            if ((attackerPartner != null && NpcProfile.isSparCountdown(attacker))
                    || (targetPartner != null && NpcProfile.isSparCountdown(target))) {
                event.setCanceled(true);
                return;
            }
            if (attackerPartner != null && !attackerPartner.getUUID().equals(target.getUUID())) {
                event.setCanceled(true);
                return;
            }
            if (target instanceof PathfinderMob npc && target instanceof CultivationNpc
                    && attacker instanceof ServerPlayer player
                    && !NpcProfile.isSparPair(target, player)
                    && event.getAmount() > 0.0F) {
                NpcProfile.recordPlayerAttack(npc, player);
            }
            if (targetPartner != null && event.getAmount() > 0.0F) {
                float allowed = Math.max(0.0F, target.getHealth() - 1.0F);
                event.setAmount(Math.min(event.getAmount(), allowed));
                if (event.getAmount() <= 0.0F) {
                    NpcProfile.finishSpar(target, NpcProfile.damageAttacker(event.getSource()));
                    event.setCanceled(true);
                    return;
                }
            }
        }
        if (event.getEntity() instanceof LivingEntity target
                && UndeadControl.hasUndeadPowers(target)
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
                && UndeadControl.hasUndeadPowers(target)
                && UndeadControl.isAtLeastHopping(target)
                && event.getSource().getEntity() instanceof LivingEntity attacker
                && UndeadAbilitySystem.isDirectMelee(event.getSource(), attacker)
                && !QiInfusedWeapon.hasQi(attacker instanceof ServerPlayer skillOwner
                        ? com.example.immortal_cultivation_mod.item.WeaponSkillAttack.sourceWeapon(skillOwner, event.getSource())
                        : attacker.getMainHandItem())
                && !UndeadAbilitySystem.isUndeadQiMelee(event.getSource())) {
            event.setCanceled(true);
            return;
        }
        if (event.getEntity() instanceof LivingEntity target
                && UndeadControl.hasUndeadPowers(target)
                && event.getSource().is(DamageTypes.PLAYER_ATTACK)
                && event.getSource().getEntity() instanceof ServerPlayer player) {
            ItemStack sourceWeapon = com.example.immortal_cultivation_mod.item.WeaponSkillAttack.sourceWeapon(player, event.getSource());
            if (sourceWeapon.is(ModItems.TAOMU_SWORD.get())) {
                event.setAmount(event.getAmount() * 1.5F);
            } else if (sourceWeapon.is(ModItems.COPPER_COIN_SWORD.get())) {
                event.setAmount(event.getAmount() * 1.7F);
            }
        }
        if (!shieldBreakerAttack
                && event.getEntity() instanceof ServerPlayer player
                && CombatStateManager.blockIncomingAttack(
                player, event.getSource(), event.getAmount())) {
            MysteriousRingItem.onBlockedAttack(player, event.getAmount());
            if (event.getSource().getEntity() instanceof LivingEntity attacker) {
                UndeadControl.defensiveCommand(player, attacker);
                NpcProfile.defendDaoCompanion(player, attacker);
            }
            event.setCanceled(true);
            return;
        }
        if (event.getAmount() > 0.0F && CombatStateManager.isHeavyAttack(event.getSource())) {
            event.setAmount(event.getAmount() * 1.2F);
        }
        if (event.getAmount() > 0.0F
                && event.getEntity() instanceof LivingEntity target
                && event.getSource().is(DamageTypes.PLAYER_ATTACK)
                && event.getSource().getEntity() instanceof ServerPlayer player) {
            if (CultivationMethods.isCuiguLianshenJue(ModAttachments.getData(player).activeCultivationMethod())
                    && player.getRandom().nextFloat() < 0.15F) {
                event.setAmount(event.getAmount() * 1.3F);
            }
            ItemStack weapon = com.example.immortal_cultivation_mod.item.WeaponSkillAttack.sourceWeapon(player, event.getSource());
            event.setAmount(com.example.immortal_cultivation_mod.item.ForgingSystem.adjustWeaponDamage(weapon, target, event.getAmount()));
            event.setAmount(com.example.immortal_cultivation_mod.item.WeaponRestrictionSystem
                    .adjustWeaponDamage(weapon, target, event.getAmount()));
            event.setAmount(com.example.immortal_cultivation_mod.item.IdentityItems.adjustSharpenedDamage(
                    weapon, target, event.getAmount()));
            com.example.immortal_cultivation_mod.item.ForgingSystem.applyWeaponHitEffects(weapon, target);
            com.example.immortal_cultivation_mod.item.WeaponRestrictionSystem.applyWeaponHitEffects(weapon, target);
        }
        if (event.getAmount() > 0.0F
                && event.getEntity() instanceof LivingEntity target
                && event.getSource().getEntity() instanceof PathfinderMob attacker
                && attacker instanceof CultivationNpc
                && event.getSource().is(DamageTypes.MOB_ATTACK)
                && event.getSource().getDirectEntity() == attacker) {
            ItemStack weapon = attacker.getMainHandItem();
            if (!weapon.isEmpty() && QiInfusedWeapon.isWeapon(weapon)) {
                float qiDamage = QiInfusedWeapon.injectedQiDamage(weapon);
                event.setAmount(event.getAmount() + qiDamage);
                if (qiDamage > 0.0F) {
                    QiInfusedWeapon.consumeInjectedQiDamage(weapon, qiDamage);
                }
                event.setAmount(com.example.immortal_cultivation_mod.item.ForgingSystem
                        .adjustWeaponDamage(weapon, target, event.getAmount()));
                event.setAmount(com.example.immortal_cultivation_mod.item.WeaponRestrictionSystem
                        .adjustWeaponDamage(weapon, target, event.getAmount()));
                event.setAmount(com.example.immortal_cultivation_mod.item.IdentityItems
                        .adjustSharpenedDamage(weapon, target, event.getAmount()));
                com.example.immortal_cultivation_mod.item.ForgingSystem.applyWeaponHitEffects(weapon, target);
                com.example.immortal_cultivation_mod.item.WeaponRestrictionSystem.applyWeaponHitEffects(weapon, target);
            }
        }
        if (event.getSource().is(DamageTypes.FALL)
                && event.getEntity() instanceof LivingEntity living
                && UndeadControl.hasUndeadPowers(living)) {
            living.fallDistance = 0.0F;
            living.resetFallDistance();
            event.setCanceled(true);
            return;
        }
        if (event.getEntity() instanceof LivingEntity owner
                && event.getSource().getEntity() instanceof LivingEntity attacker
                && UndeadControl.stopAttackAgainstOwner(attacker, owner)) {
            event.setCanceled(true);
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player
                && event.getSource().getEntity() instanceof LivingEntity attacker) {
            UndeadControl.defensiveCommand(player, attacker);
            NpcProfile.defendDaoCompanion(player, attacker);
        }
        if (event.getAmount() > 0.0F
                && event.getEntity() instanceof LivingEntity target
                && event.getSource().getEntity() instanceof LivingEntity attacker
                && UndeadControl.hasUndeadPowers(attacker)
                && UndeadControl.isAtLeastRank(attacker, "飞僵")
                && UndeadAbilitySystem.isDirectMelee(event.getSource(), attacker)) {
            target.addEffect(new MobEffectInstance(ModEffects.SHIDU, 20 * 5, 4, false, true, true), attacker);
        }
        if (event.getEntity() instanceof LivingEntity player && player.hasEffect(ModEffects.QI_GATHERING)) {
            event.setAmount(event.getAmount() * 3.0F);
        }
        if (event.getEntity() instanceof ServerPlayer player
                && CombatStateManager.isPostureBroken(player)) {
            event.setAmount(event.getAmount() * 1.25F);
        }
        float fortunePiercing = com.example.immortal_cultivation_mod.fate.FateCombat.shieldPiercing(
                event.getSource(), event.getAmount());
        event.setAmount(event.getAmount() - fortunePiercing);
        if (event.getAmount() > 0.0F
                && event.getEntity() instanceof ServerPlayer player) {
            event.setAmount(BlackTortoiseHeartguardMirrorItem.protectFromIncomingDamage(
                    player, event.getSource(), event.getAmount()));
        }
        // NPC shields must see the same fully modified hit as player shields. Applying
        // Dielang inside Mob#hurt happened before this event, so stored weapon qi and
        // forging/restriction modifiers could bypass the shield or remain unconsumed.
        float shieldBreakerBonus = shieldBreakerAttack ? event.getAmount() * 0.30F : 0.0F;
        if (event.getAmount() > 0.0F && event.getEntity() instanceof LivingEntity target) {
            event.setAmount(CultivationShieldSpells.absorbDomain(
                    target, event.getSource(), event.getAmount()));
            if (shieldBreakerBonus > 0.0F) {
                shieldBreakerBonus = CultivationShieldSpells.absorbDomain(
                        target, event.getSource(), shieldBreakerBonus);
            }
        }
        if (event.getAmount() > 0.0F
                && event.getEntity() instanceof LivingEntity target
                && target instanceof CultivationNpc) {
            event.setAmount(DielangShield.absorb(target, event.getSource(), event.getAmount()));
            if (shieldBreakerBonus > 0.0F) {
                shieldBreakerBonus = DielangShield.absorb(
                        target, event.getSource(), shieldBreakerBonus);
            }
        }
        if (!(event.getEntity() instanceof ServerPlayer)) event.setAmount(event.getAmount() + fortunePiercing);
        if (event.getEntity() instanceof ServerPlayer player) {
            float beforeCultivationShields = event.getAmount();
            float amount = absorbPlayerCultivationShields(
                    player, event.getSource(), event.getAmount());
            if (shieldBreakerBonus > 0.0F) {
                absorbPlayerCultivationShields(player, event.getSource(), shieldBreakerBonus);
            }
            CombatStateManager.addBlockedPosture(
                    player, Math.max(0.0F, beforeCultivationShields - amount));
            amount = ChangqingJue.absorbWithLifeForce(player, amount) + fortunePiercing;
            if (amount > 0.0F) {
                CombatStateManager.cancelHeavyCharge(player);
            }
            if (amount > 0.0F && player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).isEmpty()) {
                ItemStack bracer = HeavyIronBracerItem.isRealStack(player.getMainHandItem())
                        ? player.getMainHandItem()
                        : HeavyIronBracerItem.isRealStack(player.getOffhandItem())
                                ? player.getOffhandItem() : ItemStack.EMPTY;
                if (!bracer.isEmpty()) {
                    damageHeldItem(bracer);
                }
            }
            event.setAmount(amount);
            if (amount > 0.0F
                    && MysteriousRingItem.tryEmergencyTeleport(
                    player, event.getSource(), amount)) {
                event.setCanceled(true);
                return;
            }
        }
        if (event.getEntity() instanceof LivingEntity target
                && NpcProfile.sparPartner(target) != null
                && event.getAmount() > 0.0F) {
            float allowed = Math.max(0.0F, target.getHealth() - 1.0F);
            event.setAmount(Math.min(event.getAmount(), allowed));
            if (event.getAmount() >= allowed) {
                NpcProfile.finishSpar(target, NpcProfile.damageAttacker(event.getSource()));
            }
            if (event.getAmount() <= 0.0F) {
                event.setCanceled(true);
            }
        }
    }

    private static float absorbPlayerCultivationShields(
            ServerPlayer player, DamageSource source, float amount) {
        amount = CultivationShieldSpells.absorbPersonal(player, source, amount);
        amount = DielangShield.absorb(player, source, amount);
        amount = GushiShield.absorb(player, source, amount);
        amount = HutiQi.absorb(player, source, amount);
        amount = BloodQiShield.absorb(player, source, amount);
        return com.example.immortal_cultivation_mod.item.UtilityTalismanItem
                .absorbTalismanShields(player, amount);
    }

    @SubscribeEvent
    public static void onLivingHeal(LivingHealEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && CultivationAccessoryItem.blocksHealing(player)) {
            event.setCanceled(true);
            return;
        }
        event.setAmount(event.getAmount() * UndeadAbilitySystem.healingMultiplier(event.getEntity()));
    }

    @SubscribeEvent
    public static void onDamageApplied(LivingDamageEvent.Post event) {
        if (event.getNewDamage() > 0.0F
                && event.getSource().getEntity() instanceof LivingEntity attacker) {
            CombatStateManager.recordHostileAction(attacker, event.getEntity());
            XuanyueSuifengJue.onMeleeDamageApplied(
                    attacker, event.getEntity(), event.getSource(), event.getNewDamage());
        }
        if (event.getEntity() instanceof ServerPlayer player) {
            CombatStateManager.applyPostDamage(
                    player, event.getSource(), event.getNewDamage(), event.getBlockedDamage());
            LivingEntity attacker = event.getSource().getEntity() instanceof LivingEntity living
                    ? living : null;
            MysteriousRingItem.onPlayerDamaged(
                    player, attacker, event.getSource(), event.getNewDamage());
        }
        if (event.getNewDamage() > 0.0F
                && event.getSource().getEntity() instanceof ServerPlayer player
                && event.getEntity() instanceof LivingEntity target) {
            MysteriousRingItem.onPlayerDamagedEnemy(player, target, event.getNewDamage());
            DariBatiJue.onMeleeDamageApplied(
                    player, target, event.getSource(), event.getNewDamage());
            GangtingXiaokongLu.onDamageApplied(
                    player, target, event.getSource(), event.getNewDamage());
        }
    }

    @SubscribeEvent
    public static void onKnockback(LivingKnockBackEvent event) {
        UndeadAbilitySystem.adjustKnockback(event.getEntity(), event);
        if (event.getEntity() instanceof ServerPlayer player) {
            event.setStrength(event.getStrength() * CultivationShieldSpells.knockbackMultiplier(player));
            if (CombatStateManager.isInCombat(player)) {
                CombatStateManager.damagePosture(player, 10.0F);
            }
        }
    }

    @SubscribeEvent
    public static void onShieldBlock(LivingShieldBlockEvent event) {
        if (event.getDamageSource().is(DamageTypes.PLAYER_ATTACK)
                && event.getDamageSource().getEntity() instanceof ServerPlayer attacker
                && CultivationAccessoryItem.hasShieldBreaker(attacker)) {
            com.example.immortal_cultivation_mod.guidance.DaoGuidanceObservations.record(attacker, "ring_triggered");
            event.setBlocked(false);
            event.setBlockedDamage(0.0F);
            event.getEntity().stopUsingItem();
        } else if (event.getEntity() instanceof ServerPlayer player && player.hasEffect(ModEffects.QI_GATHERING)) {
            event.setBlocked(false);
            event.setBlockedDamage(0.0F);
            player.stopUsingItem();
        } else if (event.getEntity() instanceof ServerPlayer player
                && CombatStateManager.isPostureBroken(player)) {
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
        if (!spendSpellQiOrBlood(player, data, cost)) {
            player.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".not_enough_qi"));
            return;
        }

        if (com.example.immortal_cultivation_mod.spell.CultivatorSpellExecution.releaseLingbeng(
                player, new Vec3(x, y, z))) {
            syncPlayerData(player);
        }
    }

    private static int adjustDebugStat(CommandSourceStack source, Collection<ServerPlayer> targets, String stat, int delta) {
        if (!DEBUG_STATS.contains(stat)) {
            source.sendFailure(Component.literal("Unknown cultivation debug stat: " + stat));
            return 0;
        }

        if ("restriction_hints".equals(stat)) {
            int changed = 0;
            for (ServerPlayer target : targets) {
                int before = RestrictionInsightItem.hintCount(target);
                int after = RestrictionInsightItem.adjustHintCount(target, delta);
                syncRestrictionHints(target);
                if (before != after) {
                    changed++;
                }
            }
            int changedCount = changed;
            source.sendSuccess(() -> Component.literal(
                    "Adjusted restriction_hints by " + delta + " for " + changedCount + " player(s)."), true);
            return changed;
        }

        if ("simulation_attempts".equals(stat)) {
            int changed = 0;
            for (ServerPlayer target : targets) {
                int before = LifeSimulationSystem.attempts(target);
                int after = LifeSimulationSystem.adjustAttempts(target, delta);
                OuterSystemManager.refreshScreen(target);
                if (before != after) {
                    changed++;
                }
            }
            int changedCount = changed;
            source.sendSuccess(() -> Component.literal(
                    "Adjusted simulation_attempts by " + delta + " for "
                            + changedCount + " player(s)."), true);
            return changed;
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

        if ("restriction_hints".equals(stat)) {
            return setRestrictionHints(source, targets, value);
        }

        if ("simulation_attempts".equals(stat)) {
            int changed = 0;
            for (ServerPlayer target : targets) {
                int before = LifeSimulationSystem.attempts(target);
                int after = LifeSimulationSystem.setAttempts(target, value);
                OuterSystemManager.refreshScreen(target);
                if (before != after) {
                    changed++;
                }
            }
            int changedCount = changed;
            source.sendSuccess(() -> Component.literal(
                    "Set simulation_attempts to "
                            + Math.max(0, Math.min(LifeSimulationSystem.MAX_ATTEMPTS, value))
                            + " for " + changedCount + " player(s)."), true);
            return changed;
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

    private static int setRestrictionHints(
            CommandSourceStack source, Collection<ServerPlayer> targets, int amount) {
        int clamped = Math.max(0, Math.min(RestrictionInsightItem.MAX_HINTS, amount));
        int changed = 0;
        for (ServerPlayer target : targets) {
            int before = RestrictionInsightItem.hintCount(target);
            RestrictionInsightItem.setHintCount(target, clamped);
            syncRestrictionHints(target);
            if (before != clamped) {
                changed++;
            }
        }
        int changedCount = changed;
        source.sendSuccess(() -> Component.literal(
                "Set restriction hints to " + clamped + " for " + changedCount + " player(s)."), true);
        return changed;
    }

    private static int giveSimulationAttempts(
            CommandSourceStack source,
            Collection<ServerPlayer> targets,
            int amount) {
        int changed = 0;
        for (ServerPlayer target : targets) {
            int before = LifeSimulationSystem.attempts(target);
            int after = LifeSimulationSystem.adjustAttempts(target, amount);
            OuterSystemManager.refreshScreen(target);
            if (before != after) {
                changed++;
            }
        }
        int changedCount = changed;
        source.sendSuccess(() -> Component.literal(
                "Gave " + amount + " simulation attempt(s) to "
                        + changedCount + " player(s)."), true);
        return changed;
    }

    private static int unlockAllSpells(
            CommandSourceStack source, Collection<ServerPlayer> targets) {
        List<String> registeredSpells = ModSpells.all().stream()
                .map(ModSpells.SpellDef::id)
                .map(ModSpells::normalizeId)
                .distinct()
                .sorted()
                .toList();
        int changed = 0;
        for (ServerPlayer target : targets) {
            ModAttachments.CultivationData data = ModAttachments.getData(target);
            ArrayList<String> known = new ArrayList<>(data.knownSpells());
            int before = known.size();
            for (String spellId : registeredSpells) {
                if (!known.contains(spellId)) {
                    known.add(spellId);
                }
            }
            if (known.size() != before) {
                ModAttachments.setData(target, data.withKnownSpells(List.copyOf(known)));
                syncPlayerData(target);
                changed++;
            }
        }
        int changedPlayers = changed;
        source.sendSuccess(() -> Component.literal(
                "Unlocked all " + registeredSpells.size() + " registered spells for "
                        + changedPlayers + " player(s)."), true);
        return changed;
    }

    private static void syncRestrictionHints(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player,
                new ModPayloads.ClientboundDebugHintCountPayload(
                        RestrictionInsightItem.hintCount(player)));
    }

    private static ModAttachments.CultivationData handleThoughtsTick(Player player, ModAttachments.CultivationData data) {
        ModAttachments.CultivationData updated = data;

        if (data.thoughts() > data.maxThoughts()) {
            updated = updated.withThoughts(data.maxThoughts());
        }

        if (player.tickCount % THOUGHT_REGEN_INTERVAL_TICKS == 0 && updated.thoughts() < updated.maxThoughts()) {
            int regen = Math.max(1, updated.maxThoughts() * 5 / 100);
            updated = updated.withThoughts(updated.thoughts() + regen);
        }

        if (!updated.equals(data)) {
            ModAttachments.setData(player, updated);
        }
        if (updated.thoughts() <= 0) {
            com.example.immortal_cultivation_mod.combat.ThoughtControlGuard.applyExhaustion(player);
        } else {
            com.example.immortal_cultivation_mod.combat.ThoughtControlGuard.releaseRecoveredControls(player);
        }
        return updated;
    }

    public static void adjustThoughts(Player player, int delta) {
        var data = ModAttachments.getData(player);
        var updated = data.withThoughts(data.thoughts() + delta);
        ModAttachments.setData(player, updated);
        if (updated.thoughts() <= 0) {
            com.example.immortal_cultivation_mod.combat.ThoughtControlGuard.applyExhaustion(player);
        } else if (delta > 0) {
            com.example.immortal_cultivation_mod.combat.ThoughtControlGuard.releaseRecoveredControls(player);
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
            case "thoughts" -> data.withThoughts(clamp(data.thoughts() + delta, 0, data.maxThoughts()));
            case "spirit_roots" -> data.withSpiritRoots(SpiritRoots.nextRootSet(data.spiritRoots(), delta), data.spiritRootGrade());
            case "spirit_root_grade" -> data.withSpiritRoots(data.spiritRoots(), SpiritRoots.nextGrade(data.spiritRootGrade(), delta));
            case "skill_points", "max_hp", "max_qi", "max_energy", "physical", "magic", "mental", "talent" ->
                    data.debugAdjustStat(stat, delta);
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
            case "thoughts" -> data.withThoughts(clamp(value, 0, data.maxThoughts()));
            case "skill_points", "max_hp", "max_qi", "max_energy", "physical", "magic", "mental", "talent" ->
                    data.debugSetStat(stat, value, levelDef);
            default -> data;
        };
    }

    private static String nextCultivationLevel(String current, int direction) {
            if (CultivationLevels.REALM_HALF_JINDAN.equals(current)) {
                return direction < 0 ? CultivationLevels.getPreviousStage(current) : CultivationLevels.getNextStage(current);
            }
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
        syncPlayerDataTo(player, player);
        PlayerShieldManager.sync(player);
    }

    public static void syncPlayerDataTo(ServerPlayer subject, ServerPlayer recipient) {
        var data = ModAttachments.getData(subject);
        var levelDef = CultivationLevels.getLevelDef(data.cultivationLevel());
        applyLevelStats(subject, data, levelDef);

        PacketDistributor.sendToPlayer(recipient,
                new ModPayloads.ClientboundSyncPlayerDataPayload(
                        data.qi(),
                        getEffectiveMaxQi(data, levelDef),
                        getEffectiveMaxHp(data, levelDef),
                        levelDef.maxAge(),
                        data.cultivationLevel(),
                        getJindanQuality(subject),
                        data.luck(),
                        data.moral(),
                        data.bodyType(),
                        data.soul(),
                        data.thoughts(),
                        data.talent(),
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
                        data.yuqiControlAllMode(),
                        com.example.immortal_cultivation_mod.item.IdentityItems
                                .ensurePlayerIdentity(subject),
                        CultivationAccessoryItem.isLivingCorpse(subject)
                )
        );
    }

    private static void damageHeldItem(ItemStack stack) {
        if (!stack.isDamageableItem()) {
            return;
        }
        int damage = stack.getDamageValue() + 1;
        if (damage >= stack.getMaxDamage()) {
            stack.shrink(1);
        } else {
            stack.setDamageValue(damage);
        }
    }

    private static void updateHeavyIronBracerArmor(Player player) {
        AttributeInstance armor = player.getAttribute(Attributes.ARMOR);
        if (armor == null) {
            return;
        }
        ItemStack chestplate = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        boolean active = (chestplate.isEmpty() || HeavyIronBracerItem.isDebugChestplate(chestplate))
                && HeavyIronBracerItem.isRealStack(player.getMainHandItem());
        AttributeModifier current = armor.getModifier(HEAVY_IRON_BRACER_ARMOR_MODIFIER);
        if (active && current == null) {
            armor.addTransientModifier(new AttributeModifier(
                    HEAVY_IRON_BRACER_ARMOR_MODIFIER,
                    6.0D,
                    AttributeModifier.Operation.ADD_VALUE));
        } else if (!active && current != null) {
            armor.removeModifier(HEAVY_IRON_BRACER_ARMOR_MODIFIER);
        }
    }

    public static void tryBreakthrough(ServerPlayer player) {
        if (JindanTribulation.isActive(player)) {
            player.sendSystemMessage(Component.translatable("message.immortal_cultivation_mod.jindan_trial_unfinished"));
            return;
        }
        if (CultivationAccessoryItem.isLivingCorpse(player)) {
            player.sendSystemMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID
                            + ".living_corpse_cannot_cultivate"));
            return;
        }
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
            MysteriousRingItem.onBreakthroughAttemptPoor(player);
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
            MysteriousRingItem.onBreakthroughAttemptPoor(player);
            player.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".breakthrough_requirements_missing"));
            return;
        }

        boolean needPill = CultivationLevels.needsBreakthroughPill(data.cultivationLevel());
        if (needPill) {
            boolean has = player.getInventory().items.stream().anyMatch(s -> s.is(ModItems.BREAKTHROUGH_PILL.get()));
            if (!has) {
                MysteriousRingItem.onBreakthroughAttemptPoor(player);
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
            PacketDistributor.sendToPlayer(player, new ModPayloads.ClientboundOpenJindanChallengePayload(
                    NoobAchievement.hasReward(player)));
            return;
        }

        if (player.getRandom().nextFloat() < Math.max(0, CultivationLevels.getBreakthroughFailChance(data.luck(), data.cultivationLevel())
                - com.example.immortal_cultivation_mod.alchemy.AlchemyEffects.bonus(player,
                com.example.immortal_cultivation_mod.alchemy.AlchemyChemistry.Effect.BREAKTHROUGH))) {
            ModAttachments.setData(player, data.withCultivationProgress(0));
            player.hurt(player.damageSources().magic(), Math.max(1.0F, getEffectiveMaxHp(data, levelDef) * 0.5F));
            player.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".breakthrough_failed"));
            MysteriousRingItem.onBreakthroughFailure(player);
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

        player.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".breakthrough_success", CultivationLevels.displayName(next)));
        MysteriousRingItem.onBreakthroughSuccess(
                player, data.cultivationLevel(), next);
        syncPlayerData(player);
        com.example.immortal_cultivation_mod.guidance.DaoGuidanceSystem.record(
                player, com.example.immortal_cultivation_mod.guidance.DaoGuidanceContent.BREAKTHROUGH_SUCCESS);
    }

    public static void completeJindanBreakthrough(ServerPlayer player, float averageScale, int lessThanOne, int atOne) {
        var tag = player.getPersistentData();
        if (CultivationAccessoryItem.isLivingCorpse(player)) {
            tag.remove(JINDAN_CHALLENGE_PENDING_TAG);
            player.sendSystemMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID
                            + ".living_corpse_cannot_cultivate"));
            return;
        }
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
        if (!Float.isFinite(averageScale) || averageScale < 0.0F) {
            tag.remove(JINDAN_CHALLENGE_PENDING_TAG);
            ModAttachments.setData(player,
                    data.withCultivationProgress(0)
                            .withMeditating(false));
            player.hurt(player.damageSources().magic(),
                    Math.max(1.0F, getEffectiveMaxHp(data, levelDef) * 0.5F));
            player.sendSystemMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID + ".breakthrough_failed"));
            MysteriousRingItem.onBreakthroughFailure(player);
            syncPlayerData(player);
            return;
        }

        int maxQi = getEffectiveMaxQi(data, levelDef);
        long need = CultivationLevels.getTotalQiNeeded(data.cultivationLevel());
        if (data.qi() < maxQi || data.cultivationProgress() < need) {
            MysteriousRingItem.onBreakthroughAttemptPoor(player);
            player.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".breakthrough_requirements_missing"));
            syncPlayerData(player);
            return;
        }

        tag.remove(JINDAN_CHALLENGE_PENDING_TAG);
        String quality = jindanQuality(averageScale);
        tag.putString(JINDAN_QUALITY_TAG, quality);

        var advancedData = data.withCultivationLevel(next)
                .withQi(0)
                .withCultivationProgress(0)
                .withMeditating(false)
                .withAddedSkillPoints(10);
        ModAttachments.setData(player, advancedData);
        applyLevelStats(player, advancedData, CultivationLevels.getLevelDef(next));
        player.setHealth(player.getMaxHealth());
        MysteriousRingItem.onBreakthroughSuccess(
                player, data.cultivationLevel(), next);
        syncPlayerData(player);
        com.example.immortal_cultivation_mod.guidance.DaoGuidanceSystem.record(
                player, com.example.immortal_cultivation_mod.guidance.DaoGuidanceContent.BREAKTHROUGH_SUCCESS);
        JindanTribulation.start(player, quality);
        if (data.cultivationLevel().equals(CultivationLevels.REALM_HALF_JINDAN)) {
            com.example.immortal_cultivation_mod.guidance.DaoGuidanceObservations.record(player, "core_retry");
        }
    }

    private static boolean isZhujiToJindan(String current, String next) {
        return ((CultivationLevels.REALM_ZHUJI + CultivationLevels.STAGE_LATE).equals(current)
                || CultivationLevels.REALM_HALF_JINDAN.equals(current))
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

    public static String getJindanQuality(Player player) {
        String quality = player.getPersistentData().getString(JINDAN_QUALITY_TAG);
        return switch (quality) {
            case "xiapin", "zhongpin", "shangpin", "jipin", "yuanman", "jiuzhuan" -> quality;
            default -> "liedan";
        };
    }

    public static void setJindanQuality(ServerPlayer player, String quality) {
        if (quality == null) {
            return;
        }
        String validated = switch (quality) {
            case "liedan", "xiapin", "zhongpin", "shangpin", "jipin", "yuanman", "jiuzhuan" -> quality;
            default -> "";
        };
        if (validated.isEmpty()) {
            return;
        }
        player.getPersistentData().putString(JINDAN_QUALITY_TAG, validated);
        syncPlayerData(player);
    }

    public static int getJindanChunkQiCost(Player player) {
        return switch (getJindanQuality(player)) {
            case "xiapin" -> 4_000;
            case "zhongpin" -> 3_000;
            case "shangpin" -> 2_000;
            case "jipin" -> 1_000;
            case "yuanman" -> 900;
            case "jiuzhuan" -> 800;
            default -> 5_000;
        };
    }

    private static void applyLevelStats(Player player, ModAttachments.CultivationData data, CultivationLevels.LevelDef levelDef) {
        ImmortalCultivationMod.raiseMaxHealthAttributeLimit();
        ImmortalCultivationMod.raiseArmorAttributeLimit();
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

            double livingCorpseBonus = UndeadControl.livingCorpseMaxHealthBonus(player);
            AttributeModifier existingLivingCorpseModifier =
                    maxHealth.getModifier(LIVING_CORPSE_MAX_HEALTH_MODIFIER);
            double existingLivingCorpseBonus = existingLivingCorpseModifier == null
                    ? 0.0D
                    : existingLivingCorpseModifier.amount();
            if (Math.abs(existingLivingCorpseBonus - livingCorpseBonus) > 0.001D) {
                maxHealth.removeModifier(LIVING_CORPSE_MAX_HEALTH_MODIFIER);
                if (livingCorpseBonus > 0.001D) {
                    maxHealth.addPermanentModifier(new AttributeModifier(
                            LIVING_CORPSE_MAX_HEALTH_MODIFIER,
                            livingCorpseBonus,
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

        AttributeInstance armor = player.getAttribute(Attributes.ARMOR);
        if (armor != null) {
            double cultivationArmor = levelDef.armor();
            AttributeModifier existingModifier = armor.getModifier(CULTIVATION_ARMOR_MODIFIER);
            double existingBonus = existingModifier == null ? 0.0D : existingModifier.amount();
            if (Math.abs(existingBonus - cultivationArmor) > 0.001D) {
                armor.removeModifier(CULTIVATION_ARMOR_MODIFIER);
                if (cultivationArmor > 0.0D) {
                    armor.addPermanentModifier(new AttributeModifier(
                            CULTIVATION_ARMOR_MODIFIER,
                            cultivationArmor,
                            AttributeModifier.Operation.ADD_VALUE
                    ));
                }
            }
        }

        AttributeInstance movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed != null) {
            double speed = 0.1D;
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
        return spendQiOrBlood(player, data, cost, false, false);
    }

    public static boolean spendSpellQiOrBlood(Player player, ModAttachments.CultivationData data, int cost) {
        return spendQiOrBlood(player, data, cost, false, true);
    }

    public static boolean spendSustainedQiOrBlood(
            Player player, ModAttachments.CultivationData data, int cost) {
        return spendQiOrBlood(player, data, cost, true, false);
    }

    private static boolean spendQiOrBlood(
            Player player, ModAttachments.CultivationData data,
            int cost, boolean sustained, boolean spell) {
        int sealedCost = sustained
                ? com.example.immortal_cultivation_mod.spell.SpiritSealSystem
                        .adjustSustainQiCost(player, Math.max(0, cost))
                : com.example.immortal_cultivation_mod.spell.SpiritSealSystem
                        .adjustSpellQiCost(player, Math.max(0, cost));
        int actualCost = com.example.immortal_cultivation_mod.magicritual.center.CenterEffectSupport
                .adjustSpellQiCost(player, sealedCost);
        double discount=spell&&actualCost>0
                ?com.example.immortal_cultivation_mod.alchemy.AlchemyEffects.spellDiscount(player):0;
        if(discount>0)actualCost=Math.max(1,(int)Math.ceil(actualCost*(1-discount)));
        if (CultivationMethods.isBloodDemon(data.activeCultivationMethod())) {
            if ((long) data.blood() + data.qi() < actualCost) {
                return false;
            }
            int bloodSpent = Math.min(data.blood(), actualCost);
            int qiSpent = actualCost - bloodSpent;
            ModAttachments.setData(player, data
                    .withBlood(data.blood() - bloodSpent)
                    .withQi(data.qi() - qiSpent));
            if(discount>0)com.example.immortal_cultivation_mod.alchemy.AlchemyEffects.spendSpellDiscount(player);
            markQiFlowActive(player, actualCost);
            com.example.immortal_cultivation_mod.spell.SpiritSealSystem
                    .onQiSpent(player, actualCost);
            return true;
        }
        if (data.qi() < actualCost) {
            return false;
        }
        ModAttachments.setData(player, data.withQi(data.qi() - actualCost));
        if(discount>0)com.example.immortal_cultivation_mod.alchemy.AlchemyEffects.spendSpellDiscount(player);
        markQiFlowActive(player, actualCost);
        com.example.immortal_cultivation_mod.spell.SpiritSealSystem
                .onQiSpent(player, actualCost);
        return true;
    }

    private static void markQiFlowActive(Player player, int cost) {
        if (cost <= 0 || player.level().isClientSide) return;
        player.getPersistentData().putLong(QI_FLOW_ACTIVE_UNTIL_TAG,
                player.level().getGameTime() + QI_FLOW_AFTER_USE_TICKS);
    }

    /** True while an entity is drawing ambient qi or has just spent qi. */
    public static boolean isDrawingOrUsingQi(LivingEntity entity) {
        if (entity == null || !entity.isAlive()) return false;
        if (entity.hasEffect(ModEffects.QI_GATHERING)) return true;
        if (entity instanceof CultivationNpc npc
                && npc.npcCastingTicks() > 0) return true;
        if (!(entity instanceof Player player)) return false;
        ModAttachments.CultivationData data = ModAttachments.getData(player);
        return data.isMeditating()
                || player instanceof ServerPlayer serverPlayer
                        && CombatStateManager.isPreparingSpell(serverPlayer)
                || player.getPersistentData().getLong(QI_FLOW_ACTIVE_UNTIL_TAG)
                        >= player.level().getGameTime();
    }

    public static boolean canAffordQiOrBlood(ModAttachments.CultivationData data, int cost) {
        int actualCost = Math.max(0, cost);
        if (CultivationMethods.isBloodDemon(data.activeCultivationMethod())) {
            return (long) data.blood() + data.qi() >= actualCost;
        }
        return data.qi() >= actualCost;
    }

    public static void handleToggleMeditate(ServerPlayer player) {
        var data = ModAttachments.getData(player);
        if (CultivationAccessoryItem.isLivingCorpse(player)) {
            stopMeditating(player, data);
            player.sendSystemMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID
                            + ".living_corpse_cannot_cultivate"));
            return;
        }
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
        } else {
            MEDITATION_ANCHORS.remove(player.getUUID());
            player.setPose(Pose.STANDING);
        }
        broadcastMeditationState(player, newState);
        player.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + (newState ? ".meditation_started" : ".meditation_stopped")));
        syncPlayerData(player);
    }

    private static void stopMeditating(
            ServerPlayer player,
            ModAttachments.CultivationData data) {
        if (!data.isMeditating()) {
            return;
        }
        ModAttachments.setData(player, data.withMeditating(false));
        MEDITATION_ANCHORS.remove(player.getUUID());
        player.setPose(Pose.STANDING);
        broadcastMeditationState(player, false);
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

    public static int ambientQiValue(Player player) {
        return getAmbientQi(player).value();
    }

    /** Ambient qi is a percentage bonus to natural regeneration, not a flat point bonus. */
    private static int applyAmbientQiRegenBonus(int baseRegen, AmbientQi ambientQi) {
        if (baseRegen <= 0 || ambientQi.value() < AMBIENT_QI_PER_SPIRIT_VEIN_CENTER) {
            return Math.max(0, baseRegen);
        }
        long multiplierPercent = 100L + ambientQi.value();
        long boosted = ((long)baseRegen * multiplierPercent + 99L) / 100L;
        return (int)Math.min(Integer.MAX_VALUE, boosted);
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
        if (key != null && "immortal_cultivation_mod".equals(key.getNamespace())) {
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
        return key != null && "immortal_cultivation_mod".equals(key.getNamespace()) && "spirit_vein_center".equals(key.getPath());
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
