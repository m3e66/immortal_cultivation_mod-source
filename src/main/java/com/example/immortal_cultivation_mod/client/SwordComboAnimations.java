package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.entity.FuchenBindingPlumeEntity;
import com.example.immortal_cultivation_mod.item.QiInfusedWeapon;
import com.example.immortal_cultivation_mod.item.HigherWeaponItem;
import com.example.immortal_cultivation_mod.network.CombatPayloads;
import com.example.immortal_cultivation_mod.network.HigherWeaponPayloads;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
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

public final class SwordComboAnimations {
    private static final ResourceLocation[] ANIMATION_IDS = {
            ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "sword_1"),
            ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "sword_2"),
            ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "sword_3")
    };
    private static final ResourceLocation[] PUNCH_ANIMATION_IDS = {
            ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "punch_1"),
            ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "punch_2"),
            ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "punch_3")
    };
    private static final ResourceLocation SPEAR_HEAVY_THRUST_ID =
            ResourceLocation.fromNamespaceAndPath(
                    ImmortalCultivationMod.MODID, "spear_heavy_thrust");
    private static final ResourceLocation WEAPON_BLOCK_ID =
            ResourceLocation.fromNamespaceAndPath(
                    ImmortalCultivationMod.MODID, "weapon_block");
    private static final ResourceLocation DUAL_WEAPON_BLOCK_ID =
            ResourceLocation.fromNamespaceAndPath(
                    ImmortalCultivationMod.MODID, "weapon_dual_block");
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
    private static final ResourceLocation GENERIC_SPELL_CAST_ID =
            ResourceLocation.fromNamespaceAndPath(
                    ImmortalCultivationMod.MODID, "spell_cast");
    private static final Map<String, ResourceLocation> SPELL_CAST_IDS = Map.ofEntries(
            Map.entry("yangyan_futi", ResourceLocation.fromNamespaceAndPath(
                    ImmortalCultivationMod.MODID, "yangyan_futi_cast")),
            Map.entry("dari_zhen", ResourceLocation.fromNamespaceAndPath(
                    ImmortalCultivationMod.MODID, "dari_zhen_cast")),
            Map.entry("lieyang_bu", ResourceLocation.fromNamespaceAndPath(
                    ImmortalCultivationMod.MODID, "lieyang_bu_cast")),
            Map.entry("rilun", ResourceLocation.fromNamespaceAndPath(
                    ImmortalCultivationMod.MODID, "rilun_cast")),
            Map.entry("jiyang_yike", ResourceLocation.fromNamespaceAndPath(
                    ImmortalCultivationMod.MODID, "jiyang_yike_cast")),
            Map.entry("leixiao_guanyun", ResourceLocation.fromNamespaceAndPath(
                    ImmortalCultivationMod.MODID, "leixiao_guanyun_cast")),
            Map.entry("jiuxiao_fenglei_yu", ResourceLocation.fromNamespaceAndPath(
                    ImmortalCultivationMod.MODID, "jiuxiao_fenglei_yu_cast")),
            Map.entry("xuanyue_bi", ResourceLocation.fromNamespaceAndPath(
                    ImmortalCultivationMod.MODID, "xuanyue_method_cast")),
            Map.entry("jinyan_shengfeng", ResourceLocation.fromNamespaceAndPath(
                    ImmortalCultivationMod.MODID, "xuanyue_method_cast")),
            Map.entry("shansuo_zhenyu", ResourceLocation.fromNamespaceAndPath(
                    ImmortalCultivationMod.MODID, "xuanyue_method_cast")),
            Map.entry("wanfeng_yuezang", ResourceLocation.fromNamespaceAndPath(
                    ImmortalCultivationMod.MODID, "xuanyue_method_cast")),
            Map.entry("dongliu_suo", ResourceLocation.fromNamespaceAndPath(
                    ImmortalCultivationMod.MODID, "hanlan_method_cast")),
            Map.entry("cangyuan_hongliu", ResourceLocation.fromNamespaceAndPath(
                    ImmortalCultivationMod.MODID, "hanlan_method_cast")),
            Map.entry("hanyuan_fengjie", ResourceLocation.fromNamespaceAndPath(
                    ImmortalCultivationMod.MODID, "hanlan_method_cast")));
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
        PlayerAnimationAccess.REGISTER_ANIMATION_EVENT.register(PlayerCombatAnimations::register);
    }

    public static void play(UUID playerId, int slashIndex) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        // Weapon rituals reuse cast keyframes without starting ClientData's static casting pose.
        // That pose is applied by PlayerModelMixin and would override the animated right arm.
        if (slashIndex == HigherWeaponPayloads.THUNDER_RITUAL_ANIMATION
                || slashIndex == HigherWeaponPayloads.STAR_RITUAL_ANIMATION) {
            playSpellCast(playerId, slashIndex == HigherWeaponPayloads.THUNDER_RITUAL_ANIMATION
                    ? "leixiao_guanyun" : "spell_cast");
            return;
        }
        var player = minecraft.level.getPlayerByUUID(playerId);
        if (slashIndex == CombatPayloads.FUCHEN_BIND_ANIMATION_START) {
            BINDING_PLAYERS.add(playerId);
            BINDING_MISSING_TICKS.put(playerId, 0);
            if (player instanceof AbstractClientPlayer clientPlayer) {
                ensureBindingPose(clientPlayer);
                if (clientPlayer.getMainHandItem().getItem() instanceof com.example.immortal_cultivation_mod.item.FuchenItem) {
                    WeaponSkillAnimations.play(clientPlayer,
                            com.example.immortal_cultivation_mod.network.WeaponUseAnimations.Type.FUCHEN);
                }
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

    public static void playSpellCast(UUID playerId, String spellId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        var player = minecraft.level.getPlayerByUUID(playerId);
        if (!(player instanceof AbstractClientPlayer clientPlayer)) {
            return;
        }
        ModifierLayer<IAnimation> layer = getOrCreateLayer(clientPlayer);
        PlayerCombatAnimations.get(clientPlayer).beginAttack();
        if (clientPlayer.getMainHandItem().getItem() instanceof HigherWeaponItem) {
            setFirstPersonBlockMode(clientPlayer, true);
        }
        ResourceLocation animationId = SPELL_CAST_IDS.getOrDefault(
                spellId == null ? "" : spellId, GENERIC_SPELL_CAST_ID);
        var animation = PlayerAnimationRegistry.getAnimation(animationId);
        if (animation == null && !GENERIC_SPELL_CAST_ID.equals(animationId)) {
            animation = PlayerAnimationRegistry.getAnimation(
                    GENERIC_SPELL_CAST_ID);
        }
        if (animation != null) {
            layer.setAnimation(animation.playAnimation());
        }
    }

    private static void play(AbstractClientPlayer player, int slashIndex) {
        ModifierLayer<IAnimation> layer = getOrCreateLayer(player);
        var weaponUse = com.example.immortal_cultivation_mod.network.WeaponUseAnimations.fromPacket(slashIndex);
        if (weaponUse != null) {
            WeaponSkillAnimations.play(player, weaponUse);
            return;
        }
        if (slashIndex == CombatPayloads.WHIP_HEAVY_ANIMATION) {
            WeaponSkillAnimations.play(player,
                    com.example.immortal_cultivation_mod.network.WeaponUseAnimations.Type.WHIP);
            return;
        }
        if (slashIndex == CombatPayloads.BLOCK_ANIMATION_STOP) {
            PlayerCombatAnimations.get(player).stopBlock();
            return;
        }
        if (slashIndex == CombatPayloads.BLOCK_ANIMATION_START) {
            WeaponSkillAnimations.stop(player);
            applyBlockPose(player);
            return;
        }
        PlayerCombatAnimations.get(player).beginAttack();
        setFirstPersonBlockMode(player, false);
        if (player.getMainHandItem().getItem() instanceof HigherWeaponItem weapon
                && switch (weapon.spec) {
                    case BLACK_IRON_GREATSWORD, MOON_CRESCENT_HALBERD, EARTHBREAKER_HAMMER -> true;
                    default -> false;
                }) {
            setFirstPersonBlockMode(player, true);
        }
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
        } else if (QiInfusedWeapon.isFistWeapon(player.getMainHandItem())) {
            setFirstPersonBlockMode(player, true);
            animationId = PUNCH_ANIMATION_IDS[
                    Math.floorMod(slashIndex, PUNCH_ANIMATION_IDS.length)];
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
        PlayerCombatAnimations.clean(minecraft.level);
        if (minecraft.level == null) {
            BINDING_PLAYERS.clear();
            BINDING_MISSING_TICKS.clear();
            return;
        }

        // Refresh a held guard after equipment sync, or if another action finished its clip.
        for (var player : PlayerCombatAnimations.players()) {
            var state = PlayerCombatAnimations.get(player);
            if (!player.isAlive()) {
                state.stopBlock();
            } else if (state.blockPose != null
                    && (state.blockPose != hasTwoWeapons(player) || !state.isActive())) {
                applyBlockPose(player);
            }
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

    private static boolean hasTwoWeapons(AbstractClientPlayer player) {
        return com.example.immortal_cultivation_mod.combat.CombatStateManager.isCombatWeapon(player.getMainHandItem())
                && com.example.immortal_cultivation_mod.combat.CombatStateManager.isCombatWeapon(player.getOffhandItem())
                && !QiInfusedWeapon.isFistWeapon(player.getMainHandItem())
                && !QiInfusedWeapon.isFistWeapon(player.getOffhandItem());
    }

    private static void applyBlockPose(AbstractClientPlayer player) {
        boolean dual = hasTwoWeapons(player);
        PlayerCombatAnimations.get(player).blockPose = dual;
        var layer = getOrCreateLayer(player);
        setFirstPersonBlockMode(player, true);
        var animation = PlayerAnimationRegistry.getAnimation(dual ? DUAL_WEAPON_BLOCK_ID : WEAPON_BLOCK_ID);
        if (animation != null) layer.setAnimation(animation.playAnimation());
    }

    public static boolean isDualBlocking(net.minecraft.world.entity.player.Player player) {
        return player instanceof AbstractClientPlayer clientPlayer
                && PlayerCombatAnimations.find(clientPlayer) != null
                && Boolean.TRUE.equals(PlayerCombatAnimations.find(clientPlayer).blockPose);
    }

    /** Static cultivation poses must yield to every active combat animation. */
    public static boolean isPlaying(net.minecraft.world.entity.player.Player player) {
        return player instanceof AbstractClientPlayer clientPlayer
                && PlayerCombatAnimations.find(clientPlayer) != null
                && PlayerCombatAnimations.find(clientPlayer).playing();
    }

    private static void ensureBindingPose(AbstractClientPlayer player) {
        ModifierLayer<IAnimation> layer = getOrCreateBindingLayer(player);
        IAnimation current = layer.getAnimation();
        if (PlayerCombatAnimations.get(player).bindingApplied
                && current != null
                && current.isActive()) {
            return;
        }
        var animation = PlayerAnimationRegistry.getAnimation(FUCHEN_BIND_ID);
        if (animation == null) {
            return;
        }
        layer.setAnimation(animation.playAnimation());
        PlayerCombatAnimations.get(player).bindingApplied = true;
    }

    private static void stopBindingPose(AbstractClientPlayer player) {
        var state = PlayerCombatAnimations.find(player);
        if (state != null) {
            state.binding.setAnimation(null);
            state.bindingApplied = false;
        }
    }

    private static ModifierLayer<IAnimation> getOrCreateLayer(AbstractClientPlayer player) {
        return PlayerCombatAnimations.get(player);
    }

    private static ModifierLayer<IAnimation> getOrCreateBindingLayer(AbstractClientPlayer player) {
        return PlayerCombatAnimations.get(player).binding;
    }

    private static void setFirstPersonBlockMode(AbstractClientPlayer player, boolean blocking) {
        PlayerCombatAnimations.get(player).firstPerson.setCurrentFirstPersonConfig(blocking
                ? FirstPersonModifier.FirstPersonConfigEnum.ENABLE_BOTH_ARMS
                : FirstPersonModifier.FirstPersonConfigEnum.ONLY_RIGHT_ARM_AND_ITEM);
    }
}
