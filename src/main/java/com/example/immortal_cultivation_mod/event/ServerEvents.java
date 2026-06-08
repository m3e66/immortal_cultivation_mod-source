package com.example.immortal_cultivation_mod.event;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.effect.PhotonEffects;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.network.ModPayloads;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
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

    @SubscribeEvent
    public static void onPlayerTickPre(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;

        var data = ModAttachments.getData(player);

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
            player.resetFallDistance();
            player.hurtMarked = true;
        } else {
            MEDITATION_ANCHORS.remove(player.getUUID());
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

        if (!CultivationLevels.isMortal(data.cultivationLevel()) && maxQi > 0 && data.qi() < maxQi && player.tickCount % 20 == 0) {
            int regen = Math.max(1, maxQi / 100);
            ModAttachments.setData(player, data.withQi(Math.min(maxQi, data.qi() + regen)));
        }

        if (data.isMeditating() && maxQi > 0 && player.tickCount % 20 == 0) {
            int need = CultivationLevels.getTotalQiNeeded(data.cultivationLevel());
            if (data.qi() >= 10 && data.cultivationProgress() < need) {
                ModAttachments.setData(player,
                        data.withQi(data.qi() - 10)
                                .withCultivationProgress(data.cultivationProgress() + 10));
            }
        }

        if (player.tickCount % 40 == 0 && player instanceof ServerPlayer sp) {
            syncPlayerData(sp);
        }

        if (player instanceof ServerPlayer sp) {
            PhotonEffects.tick(sp);
        }
    }

    public static void resetPlayer(Player player) {
        var data = ModAttachments.CultivationData.createDefault();
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
            if (data.isMeditating()) {
                ModAttachments.setData(sp, data.withMeditating(false));
            }
            MEDITATION_ANCHORS.remove(sp.getUUID());
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

        var pos = event.getPos();
        triggerLingbeng(player, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
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
                        data.agePenalty(),
                        data.cultivationProgress(),
                        data.knownSpells(),
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
            if (movementSpeed.getBaseValue() != speed) {
                movementSpeed.setBaseValue(speed);
            }
        }
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
        ModAttachments.setData(player, data.withMeditating(newState));
        if (newState) {
            MEDITATION_ANCHORS.put(player.getUUID(), player.position());
            player.setDeltaMovement(Vec3.ZERO);
        } else {
            MEDITATION_ANCHORS.remove(player.getUUID());
        }
        player.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + (newState ? ".meditation_started" : ".meditation_stopped")));
        syncPlayerData(player);
    }
}
