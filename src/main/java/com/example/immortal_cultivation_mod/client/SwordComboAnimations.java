package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.entity.FuchenBindingPlumeEntity;
import com.example.immortal_cultivation_mod.network.CombatPayloads;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.layered.modifier.FirstPersonModifier;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

public final class SwordComboAnimations {
    private static final ResourceLocation[] ANIMATION_IDS = {
            ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "sword_1"),
            ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "sword_2"),
            ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "sword_3")
    };
    private static final ResourceLocation SPEAR_HEAVY_THRUST_ID =
            ResourceLocation.fromNamespaceAndPath(
                    ImmortalCultivationMod.MODID, "spear_heavy_thrust");
    private static final ResourceLocation WEAPON_BLOCK_ID =
            ResourceLocation.fromNamespaceAndPath(
                    ImmortalCultivationMod.MODID, "weapon_block");
    private static final ResourceLocation FUCHEN_HEAVY_SPIN_ID =
            ResourceLocation.fromNamespaceAndPath(
                    ImmortalCultivationMod.MODID, "fuchen_heavy_spin");
    private static final ResourceLocation FUCHEN_BIND_ID =
            ResourceLocation.fromNamespaceAndPath(
                    ImmortalCultivationMod.MODID, "fuchen_bind");
    private static final ResourceLocation XIUJIAN_FIRE_RIGHT_ID =
            ResourceLocation.fromNamespaceAndPath(
                    ImmortalCultivationMod.MODID, "xiujian_fire_right");
    private static final ResourceLocation XIUJIAN_FIRE_LEFT_ID =
            ResourceLocation.fromNamespaceAndPath(
                    ImmortalCultivationMod.MODID, "xiujian_fire_left");
    private static final ResourceLocation WHIP_HEAVY_ID =
            ResourceLocation.fromNamespaceAndPath(
                    ImmortalCultivationMod.MODID, "whip_heavy");
    private static final int BIND_LAYER_PRIORITY = 990;
    private static final int LAYER_PRIORITY = 1000;
    private static final Map<AbstractClientPlayer, ModifierLayer<IAnimation>> LAYERS =
            new WeakHashMap<>();
    private static final Map<AbstractClientPlayer, ModifierLayer<IAnimation>> BIND_LAYERS =
            new WeakHashMap<>();
    private static final Map<AbstractClientPlayer, Boolean> BIND_POSE_APPLIED =
            new WeakHashMap<>();
    private static final Map<AbstractClientPlayer, FirstPersonModifier> FIRST_PERSON_MODIFIERS =
            new WeakHashMap<>();
    private static final Set<UUID> BINDING_PLAYERS = new HashSet<>();
    private static final Map<UUID, Integer> BINDING_MISSING_TICKS = new HashMap<>();
    private static boolean registered;

    private SwordComboAnimations() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        PlayerAnimationAccess.REGISTER_ANIMATION_EVENT.register((player, animationStack) -> {
            ModifierLayer<IAnimation> bindLayer = createBindingLayer();
            animationStack.addAnimLayer(BIND_LAYER_PRIORITY, bindLayer);
            BIND_LAYERS.put(player, bindLayer);
            ModifierLayer<IAnimation> layer = createLayer(player);
            animationStack.addAnimLayer(LAYER_PRIORITY, layer);
            LAYERS.put(player, layer);
        });
    }

    public static void play(UUID playerId, int slashIndex) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        var player = minecraft.level.getPlayerByUUID(playerId);
        if (slashIndex == CombatPayloads.FUCHEN_BIND_ANIMATION_START) {
            BINDING_PLAYERS.add(playerId);
            BINDING_MISSING_TICKS.put(playerId, 0);
            if (player instanceof AbstractClientPlayer clientPlayer) {
                ensureBindingPose(clientPlayer);
            }
            return;
        }
        if (slashIndex == CombatPayloads.FUCHEN_BIND_ANIMATION_STOP) {
            BINDING_PLAYERS.remove(playerId);
            BINDING_MISSING_TICKS.remove(playerId);
            if (player instanceof AbstractClientPlayer clientPlayer) {
                stopBindingPose(clientPlayer);
            }
            return;
        }
        if (player instanceof AbstractClientPlayer clientPlayer) {
            play(clientPlayer, slashIndex);
        }
    }

    private static void play(AbstractClientPlayer player, int slashIndex) {
        ModifierLayer<IAnimation> layer = getOrCreateLayer(player);
        if (slashIndex == CombatPayloads.BLOCK_ANIMATION_STOP) {
            layer.setAnimation(null);
            setFirstPersonBlockMode(player, false);
            return;
        }
        if (slashIndex == CombatPayloads.BLOCK_ANIMATION_START) {
            setFirstPersonBlockMode(player, true);
            var animation = PlayerAnimationRegistry.getAnimation(WEAPON_BLOCK_ID);
            if (animation != null) {
                layer.setAnimation(animation.playAnimation());
            }
            return;
        }
        setFirstPersonBlockMode(player, false);
        ResourceLocation animationId;
        if (slashIndex == CombatPayloads.SPEAR_HEAVY_THRUST_ANIMATION) {
            animationId = SPEAR_HEAVY_THRUST_ID;
        } else if (slashIndex == CombatPayloads.FUCHEN_HEAVY_SPIN_ANIMATION) {
            animationId = FUCHEN_HEAVY_SPIN_ID;
        } else if (slashIndex == CombatPayloads.XIUJIAN_FIRE_RIGHT_ANIMATION) {
            animationId = XIUJIAN_FIRE_RIGHT_ID;
        } else if (slashIndex == CombatPayloads.XIUJIAN_FIRE_LEFT_ANIMATION) {
            setFirstPersonBlockMode(player, true);
            animationId = XIUJIAN_FIRE_LEFT_ID;
        } else if (slashIndex == CombatPayloads.WHIP_HEAVY_ANIMATION) {
            animationId = WHIP_HEAVY_ID;
        } else {
            animationId = ANIMATION_IDS[Math.floorMod(slashIndex, ANIMATION_IDS.length)];
        }
        var animation = PlayerAnimationRegistry.getAnimation(animationId);
        if (animation != null) {
            layer.setAnimation(animation.playAnimation());
        }
    }

    public static void tickBindingPoses() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            clearBindingPoses();
            return;
        }

        Set<UUID> visibleBindings = new HashSet<>();
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof FuchenBindingPlumeEntity plume)
                    || !(plume.getOwnerEntity() instanceof AbstractClientPlayer owner)) {
                continue;
            }
            UUID ownerId = owner.getUUID();
            visibleBindings.add(ownerId);
            BINDING_PLAYERS.add(ownerId);
            BINDING_MISSING_TICKS.put(ownerId, 0);
            ensureBindingPose(owner);
        }

        for (UUID playerId : new HashSet<>(BINDING_PLAYERS)) {
            if (visibleBindings.contains(playerId)) {
                continue;
            }
            int missingTicks = BINDING_MISSING_TICKS.merge(
                    playerId, 1, Integer::sum);
            var player = minecraft.level.getPlayerByUUID(playerId);
            if (missingTicks <= 10) {
                if (player instanceof AbstractClientPlayer clientPlayer) {
                    ensureBindingPose(clientPlayer);
                }
                continue;
            }
            BINDING_PLAYERS.remove(playerId);
            BINDING_MISSING_TICKS.remove(playerId);
            if (player instanceof AbstractClientPlayer clientPlayer) {
                stopBindingPose(clientPlayer);
            }
        }
    }

    private static void ensureBindingPose(AbstractClientPlayer player) {
        ModifierLayer<IAnimation> layer = getOrCreateBindingLayer(player);
        IAnimation current = layer.getAnimation();
        if (Boolean.TRUE.equals(BIND_POSE_APPLIED.get(player))
                && current != null
                && current.isActive()) {
            return;
        }
        var animation = PlayerAnimationRegistry.getAnimation(FUCHEN_BIND_ID);
        if (animation == null) {
            return;
        }
        layer.setAnimation(animation.playAnimation());
        BIND_POSE_APPLIED.put(player, true);
    }

    private static void stopBindingPose(AbstractClientPlayer player) {
        ModifierLayer<IAnimation> layer = BIND_LAYERS.get(player);
        if (layer != null) {
            layer.setAnimation(null);
        }
        BIND_POSE_APPLIED.remove(player);
    }

    private static void clearBindingPoses() {
        for (ModifierLayer<IAnimation> layer : BIND_LAYERS.values()) {
            layer.setAnimation(null);
        }
        BIND_POSE_APPLIED.clear();
        BINDING_PLAYERS.clear();
        BINDING_MISSING_TICKS.clear();
    }

    private static ModifierLayer<IAnimation> getOrCreateLayer(AbstractClientPlayer player) {
        ModifierLayer<IAnimation> layer = LAYERS.get(player);
        if (layer != null) {
            return layer;
        }
        layer = createLayer(player);
        PlayerAnimationAccess.getPlayerAnimLayer(player).addAnimLayer(LAYER_PRIORITY, layer);
        LAYERS.put(player, layer);
        return layer;
    }

    private static ModifierLayer<IAnimation> getOrCreateBindingLayer(
            AbstractClientPlayer player) {
        ModifierLayer<IAnimation> layer = BIND_LAYERS.get(player);
        if (layer != null) {
            return layer;
        }
        layer = createBindingLayer();
        PlayerAnimationAccess.getPlayerAnimLayer(player)
                .addAnimLayer(BIND_LAYER_PRIORITY, layer);
        BIND_LAYERS.put(player, layer);
        return layer;
    }

    private static ModifierLayer<IAnimation> createBindingLayer() {
        ModifierLayer<IAnimation> layer = new ModifierLayer<>();
        FirstPersonModifier firstPerson = new FirstPersonModifier();
        firstPerson.setCurrentFirstPersonMode(FirstPersonMode.THIRD_PERSON_MODEL);
        firstPerson.setCurrentFirstPersonConfig(
                FirstPersonModifier.FirstPersonConfigEnum.ONLY_RIGHT_ARM_AND_ITEM);
        layer.addModifierLast(firstPerson);
        return layer;
    }

    private static ModifierLayer<IAnimation> createLayer(AbstractClientPlayer player) {
        ModifierLayer<IAnimation> layer = new ModifierLayer<>();
        FirstPersonModifier firstPerson = new FirstPersonModifier();
        firstPerson.setCurrentFirstPersonMode(FirstPersonMode.THIRD_PERSON_MODEL);
        firstPerson.setCurrentFirstPersonConfig(
                FirstPersonModifier.FirstPersonConfigEnum.ONLY_RIGHT_ARM_AND_ITEM);
        layer.addModifierLast(firstPerson);
        FIRST_PERSON_MODIFIERS.put(player, firstPerson);
        return layer;
    }

    private static void setFirstPersonBlockMode(
            AbstractClientPlayer player, boolean blocking) {
        FirstPersonModifier modifier = FIRST_PERSON_MODIFIERS.get(player);
        if (modifier != null) {
            modifier.setCurrentFirstPersonConfig(blocking
                    ? FirstPersonModifier.FirstPersonConfigEnum.ENABLE_BOTH_ARMS
                    : FirstPersonModifier.FirstPersonConfigEnum.ONLY_RIGHT_ARM_AND_ITEM);
        }
    }
}
