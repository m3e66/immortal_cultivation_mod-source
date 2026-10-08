package com.example.immortal_cultivation_mod.ring;

import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.combat.CombatStateManager;
import com.example.immortal_cultivation_mod.item.MysteriousRingItem;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.Comparator;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** A bounded, read-only server snapshot taken for each message; never relies on player claims. */
public final class RingSceneContext {
    private RingSceneContext() { }
    public static JsonObject snapshot(ServerPlayer player, ItemStack ring) {
        var level = player.serverLevel();
        var stats = ModAttachments.getData(player);
        JsonObject scene = new JsonObject();
        scene.addProperty("observedAtGameTick", level.getGameTime());
        scene.addProperty("dimension", level.dimension().location().toString());
        scene.addProperty("biome", level.getBiome(player.blockPosition()).unwrapKey().map(key -> key.location().toString()).orElse("unknown"));
        scene.addProperty("dayTime", Math.floorMod(level.getDayTime(), 24000));
        scene.addProperty("weather", level.isThundering() ? "thunderstorm" : level.isRaining() ? "rain" : "clear");
        scene.addProperty("rainAtPlayer", level.isRainingAt(player.blockPosition()));
        scene.addProperty("skyVisible", level.canSeeSky(player.blockPosition()));
        scene.addProperty("realm", stats.cultivationLevel());
        scene.addProperty("health", player.getHealth());
        scene.addProperty("maxHealth", player.getMaxHealth());
        scene.addProperty("qi", stats.qi());
        scene.addProperty("food", player.getFoodData().getFoodLevel());
        scene.addProperty("air", player.getAirSupply());
        scene.addProperty("onFire", player.isOnFire());
        scene.addProperty("inWater", player.isInWater());
        scene.addProperty("inLava", player.isInLava());
        scene.addProperty("onGround", player.onGround());
        scene.addProperty("fallDistance", player.fallDistance);
        scene.addProperty("dead", !player.isAlive());
        scene.addProperty("combatActive", CombatStateManager.isInCombat(player));
        scene.addProperty("mainHand", item(player.getMainHandItem()));
        scene.addProperty("offHand", item(player.getOffhandItem()));
        scene.addProperty("groundBlock", BuiltInRegistries.BLOCK.getKey(level.getBlockState(player.blockPosition().below()).getBlock()).toString());
        scene.addProperty("ring", MysteriousRingItem.elderLiveRingFacts(player, ring));
        scene.addProperty("knownSpells", stats.knownSpells().toString());
        JsonArray effects = new JsonArray();
        player.getActiveEffects().stream().limit(12).forEach(effect -> {
            JsonObject row = new JsonObject();
            row.addProperty("effect", BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value()).toString());
            row.addProperty("amplifier", effect.getAmplifier()); row.addProperty("remainingTicks", effect.getDuration());
            effects.add(row);
        });
        scene.add("effects", effects);
        JsonArray armor = new JsonArray();
        for (ItemStack stack : player.getArmorSlots()) armor.add(item(stack));
        scene.add("armor", armor);
        JsonArray nearby = new JsonArray();
        level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(24),
                entity -> entity != player && entity.isAlive() && player.distanceToSqr(entity) <= 24 * 24
                        && player.hasLineOfSight(entity)).stream()
                .sorted(Comparator.comparingDouble(player::distanceToSqr)).limit(8).forEach(entity -> {
                    JsonObject row = new JsonObject();
                    row.addProperty("type", BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
                    row.addProperty("name", shortText(entity.getName().getString()));
                    row.addProperty("distanceBlocks", Math.round(player.distanceTo(entity) * 10) / 10.0);
                    row.addProperty("health", entity.getHealth()); row.addProperty("maxHealth", entity.getMaxHealth());
                    row.addProperty("potentiallyHostile", entity instanceof Enemy);
                    row.addProperty("targetingPlayer", entity instanceof Mob mob && mob.getTarget() == player);
                    nearby.add(row);
                });
        scene.add("visibleEntitiesWithin24Blocks", nearby);
        HitResult looked = player.pick(16, 0, false);
        if (looked.getType() == HitResult.Type.BLOCK && looked instanceof BlockHitResult block) {
            scene.addProperty("lookedAtBlock", BuiltInRegistries.BLOCK.getKey(level.getBlockState(block.getBlockPos()).getBlock()).toString());
        }
        var attacker = player.getLastHurtByMob();
        if (attacker != null && player.tickCount - player.getLastHurtByMobTimestamp() <= 200)
            scene.addProperty("recentAttacker", BuiltInRegistries.ENTITY_TYPE.getKey(attacker.getType()).toString());
        return scene;
    }
    private static String item(ItemStack stack) { return stack.isEmpty() ? "empty" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(); }
    private static String shortText(String value) {
        String clean = value.replaceAll("[\\p{Cntrl}§]", "");
        return clean.substring(0, Math.min(80, clean.length()));
    }
}
