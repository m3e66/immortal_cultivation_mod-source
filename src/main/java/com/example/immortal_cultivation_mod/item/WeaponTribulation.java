package com.example.immortal_cultivation_mod.item;

import java.util.*;
import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.item.WeaponTribulationData.Trial;
import com.example.immortal_cultivation_mod.network.ModPayloads;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

/** Only the server advances trials. The actual stack, not a saved copy, is the weapon. */
@EventBusSubscriber(modid = ImmortalCultivationMod.MODID)
public final class WeaponTribulation {
    public static final String TAG = "immortal_cultivation_mod.weapon_tribulation";
    static final float MAX_STRIKE_DAMAGE = 128_000F;
    private static final DustParticleOptions PALE = new DustParticleOptions(new Vector3f(.65F, .82F, 1F), 1.3F);
    private static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1F, .75F, .18F), 1.2F);
    private record Ticket(ServerLevel level, BlockPos position) {}
    private static final Map<UUID, Ticket> TICKETS = new HashMap<>();
    private WeaponTribulation() {}

    public static UUID token(ItemStack stack) {
        var t = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound(TAG);
        return t.hasUUID("id") ? t.getUUID("id") : null;
    }
    public static boolean active(ItemStack stack) { return token(stack) != null; }
    private static void mark(ItemStack stack, UUID id) {
        var root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (id == null) root.remove(TAG);
        else { var t = new CompoundTag(); t.putUUID("id", id); root.put(TAG, t); }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    /** Uses resolved attack modifiers, attack speed, real durability, Qi and restriction effects. */
    public static double power(ItemStack stack) {
        double attack = QiInfusedWeapon.baseAttackDamage(stack)
                * (1 + WeaponRestrictionSystem.count(stack, WeaponRestrictionSystem.RestrictionType.SHARPNESS) / 100D);
        int flameDamage = ForgingSystem.forgedStat(stack, "flame_sharpness_percent")
                + ForgingSystem.forgedStat(stack, "flame_spirit_damage_percent")
                + ForgingSystem.forgedStat(stack, "flame_lightning_damage_percent")
                + ForgingSystem.forgedStat(stack, "flame_heavenly_quality_percent");
        attack *= 1 + flameDamage / 100D;
        // Trial power includes the weapon's actual maximum fire-conditioned attack potential.
        attack *= 1 + ForgingSystem.forgedStat(stack, "flame_fire_affinity_percent") / 100D;
        double speedAdd = 0, speedBase = 0, speedTotal = 1, otherModifiers = 0;
        for (var entry : stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).modifiers()) {
            if (entry.slot() != EquipmentSlotGroup.MAINHAND && entry.slot() != EquipmentSlotGroup.ANY) continue;
            var modifier = entry.modifier();
            if (entry.attribute().is(Attributes.ATTACK_SPEED)) {
                switch (modifier.operation()) {
                    case ADD_VALUE -> speedAdd += modifier.amount();
                    case ADD_MULTIPLIED_BASE -> speedBase += modifier.amount();
                    case ADD_MULTIPLIED_TOTAL -> speedTotal *= 1 + modifier.amount();
                }
            } else if (!entry.attribute().is(Attributes.ATTACK_DAMAGE)) otherModifiers += Math.abs(modifier.amount()) * 2;
        }
        double speed = Math.clamp((4 + speedAdd) * (1 + speedBase) * speedTotal, .1, 20);
        double restrictions = 0;
        for (var type : WeaponRestrictionSystem.RestrictionType.values())
            if (type != WeaponRestrictionSystem.RestrictionType.SHARPNESS && type != WeaponRestrictionSystem.RestrictionType.STURDY)
                restrictions += WeaponRestrictionSystem.effectiveCount(stack, type) * .15;
        double enchantments = 0;
        for (var entry : stack.getOrDefault(DataComponents.ENCHANTMENTS, net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY).entrySet())
            enchantments += entry.getIntValue() * 2;
        double result = attack * speed * 4 + Math.max(0, stack.getMaxDamage()) * .02
                + Math.max(0, QiInfusedWeapon.getQi(stack)) / 100D + restrictions + enchantments + otherModifiers
                + ForgingSystem.forgedStat(stack, "flame_soul_damage") * 2
                + ForgingSystem.forgedStat(stack, "flame_qi_burn_chance_percent") * .5;
        return Double.isFinite(result) ? Math.clamp(result, 1, 1_000_000) : 1_000_000;
    }
    public static int strikeCount(double power) { return Math.clamp(6 + (int)(Math.log1p(power) / 2), 6, 16); }
    public static int duration(double power) { return Math.clamp(200 + strikeCount(power) * 35 + (int)(Math.sqrt(power) * 4), 200, 2400); }
    /** Twice the previous stat-based damage, before the breakthrough rank multiplier. */
    public static float strikeDamage(double power) { return (float)Math.clamp(4 + Math.sqrt(power) * 1.30, 4, 2000); }

    /** Each higher 72-layer breakthrough doubles the stat-based lightning damage. */
    public static float strikeDamage(double power, int originalTier) {
        double safePower = Double.isFinite(power) ? Math.max(0, power) : 1_000_000;
        int multiplier = 1 << Math.clamp(originalTier, 0, WeaponRestrictionSystem.MAX_TIER - 1);
        return Math.clamp(strikeDamage(safePower) * multiplier, 4, MAX_STRIKE_DAMAGE);
    }

    /** Caller transfers the table stack only after this returns true. Held stacks remain held. */
    public static boolean start(ServerPlayer player, ItemStack stack) {
        if (!WeaponRestrictionSystem.isWeapon(stack) || !stack.isDamageableItem() || stack.getCount() != 1
                || WeaponRestrictionSystem.tierProgress(stack) != 72 || WeaponRestrictionSystem.tier(stack) >= 7 || active(stack)) return false;
        var data = WeaponTribulationData.get(player.server);
        if (data.trials.size() >= 64 || data.trials.values().stream().anyMatch(t -> t.owner.equals(player.getUUID()))) return false;
        var trial = new Trial(UUID.randomUUID(), player.getUUID(), player.serverLevel().dimension().location(), player.position());
        trial.originalTier = WeaponRestrictionSystem.tier(stack); trial.power = power(stack);
        trial.strikes = strikeCount(trial.power); trial.duration = duration(trial.power);
        trial.damage = strikeDamage(trial.power, trial.originalTier);
        com.example.immortal_cultivation_mod.guidance.DaoGuidanceObservations.record(player, "weapon_trial_started");
        mark(stack, trial.id); data.trials.put(trial.id, trial); data.setDirty();
        if (player.getMainHandItem() != stack && player.getOffhandItem() != stack) {
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++)
                if (player.getInventory().getItem(slot) == stack) player.getInventory().setItem(slot, ItemStack.EMPTY);
            spawn(player.serverLevel(), trial, stack, player.position().add(0, .8, 0));
        }
        player.sendSystemMessage(Component.translatable("message.immortal_cultivation_mod.weapon_trial.begin", trial.strikes, trial.duration / 20));
        overlay(player, 0, trial.duration, .65F);
        return true;
    }

    private static ItemEntity spawn(ServerLevel level, Trial trial, ItemStack stack, Vec3 position) {
        var entity = new ItemEntity(level, position.x, position.y, position.z, stack);
        entity.setDefaultPickUpDelay(); entity.setTarget(trial.owner); entity.setUnlimitedLifetime(); entity.setInvulnerable(true);
        level.addFreshEntity(entity); trial.entity = entity.getUUID(); updatePosition(trial, level, position);
        return entity;
    }
    private static void updatePosition(Trial trial, ServerLevel level, Vec3 position) {
        trial.dimension = level.dimension().location(); trial.position = position;
    }
    private static void ticket(Trial trial, MinecraftServer server, boolean needed) {
        var old = TICKETS.get(trial.id); var position = BlockPos.containing(trial.position);
        var level = server.getLevel(ResourceKey.create(Registries.DIMENSION, trial.dimension));
        if (old != null && (!needed || old.level != level || !new ChunkPos(old.position).equals(new ChunkPos(position)))) releaseTicket(trial.id);
        if (needed && level != null) {
            var existing = TICKETS.get(trial.id);
            var anchor = existing != null ? existing.position : position;
            // PORTAL tickets expire after 300 ticks unless refreshed; long trials must retain custody.
            level.getChunkSource().addRegionTicket(TicketType.PORTAL, new ChunkPos(anchor), 2, anchor);
            if (existing == null) { level.getChunk(anchor); TICKETS.put(trial.id, new Ticket(level, anchor)); }
        }
    }
    private static void releaseTicket(UUID id) {
        var ticket = TICKETS.remove(id);
        if (ticket != null) ticket.level.getChunkSource().removeRegionTicket(TicketType.PORTAL, new ChunkPos(ticket.position), 2, ticket.position);
    }
    /** Inventory custody is valid; only main/off hand intercepts lightning with player health. */
    private record Target(ItemStack stack, ServerPlayer carrier, ItemEntity item, boolean held) {}
    private static Target find(MinecraftServer server, Trial trial) {
        for (var player : server.getPlayerList().getPlayers()) {
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                var stack = player.getInventory().getItem(slot);
                if (!trial.id.equals(token(stack))) continue;
                if (player.isAlive()) {
                    // Vanilla pickup uses the first free slot, which need not be the selected hand.
                    trial.entity = null;
                    trial.groundOnly = false;
                    return new Target(stack, player, null,
                            player.getMainHandItem() == stack || player.getOffhandItem() == stack);
                }
                player.getInventory().setItem(slot, ItemStack.EMPTY);
                return new Target(stack, null, spawn(player.serverLevel(), trial, stack, player.position().add(0, .5, 0)), false);
            }
            var stack = player.containerMenu.getCarried();
            if (trial.id.equals(token(stack))) {
                if (player.isAlive()) {
                    trial.entity = null;
                    trial.groundOnly = false;
                    return new Target(stack, player, null, false);
                }
                player.containerMenu.setCarried(ItemStack.EMPTY);
                return new Target(stack, null, spawn(player.serverLevel(), trial, stack, player.position().add(0, .5, 0)), false);
            }
        }
        for (var level : server.getAllLevels()) for (var entity : level.getAllEntities()) {
            if (entity instanceof ItemEntity item && item.isAlive() && trial.id.equals(token(item.getItem()))) {
                trial.entity = item.getUUID(); return new Target(item.getItem(), null, item, false);
            }
        }
        return null;
    }

    @SubscribeEvent public static void tick(ServerTickEvent.Post event) { tick(event.getServer()); }
    public static void tick(MinecraftServer server) {
        var data = WeaponTribulationData.get(server);
        if (server.getTickCount() % 20 == 0) cleanOrphans(server, data);
        for (var trial : new ArrayList<>(data.trials.values())) {
            ticket(trial, server, trial.entity != null);
            var target = find(server, trial);
            if (target == null) {
                // Load grace also covers entity transfer; absence cannot count as surviving lightning.
                if (++trial.missingTicks >= 200) finish(server, trial, null, false);
                continue;
            }
            trial.missingTicks = 0;
            var level = target.carrier != null ? target.carrier.serverLevel() : (ServerLevel)target.item.level();
            var position = target.carrier != null ? target.carrier.position() : target.item.position();
            updatePosition(trial, level, position); ticket(trial, server, target.item != null);
            if (target.item != null) { target.item.setUnlimitedLifetime(); target.item.setInvulnerable(true); }
            if (WeaponRestrictionSystem.tier(target.stack) != trial.originalTier || WeaponRestrictionSystem.tierProgress(target.stack) != 72) {
                finish(server, trial, target, false); continue;
            }
            trial.age++;
            if (trial.age % 5 == 0) clouds(level, position, trial);
            int due = 60 + (trial.hits + 1) * (trial.duration - 100) / trial.strikes;
            if (trial.hits < trial.strikes && trial.age >= due) {
                trial.hits++; lightning(level, position, trial);
                if (target.held) {
                    overlay(target.carrier, 1, 7, .5F); target.carrier.hurt(level.damageSources().lightningBolt(),
                            trial.damage * flameResistanceMultiplier(target.stack));
                    if (!target.carrier.isAlive()) { data.setDirty(); continue; }
                } else if (!damageWeapon(target.stack)) {
                    shatter(level, position, target.stack);
                    if (target.item != null) target.item.discard();
                    finish(server, trial, null, false); continue;
                } else if (target.item != null) target.item.setItem(target.stack.copy());
            }
            if (trial.age >= trial.duration && trial.hits >= trial.strikes) finish(server, trial, target, true);
        }
        if (!data.trials.isEmpty()) data.setDirty();
    }
    private static void cleanOrphans(MinecraftServer server, WeaponTribulationData data) {
        for (var player : server.getPlayerList().getPlayers()) {
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                var stack = player.getInventory().getItem(slot); var id = token(stack);
                if (id != null && !data.trials.containsKey(id)) mark(stack, null);
            }
            var stack = player.containerMenu.getCarried(); var id = token(stack);
            if (id != null && !data.trials.containsKey(id)) mark(stack, null);
        }
        for (var level : server.getAllLevels()) for (var entity : level.getAllEntities()) {
            if (!(entity instanceof ItemEntity item)) continue;
            var id = token(item.getItem());
            if (id != null && !data.trials.containsKey(id)) {
                mark(item.getItem(), null); item.setItem(item.getItem().copy());
                item.setInvulnerable(false); item.setExtendedLifetime();
            }
        }
    }
    /** Ten percent before forged resistance, rounded up; direct wear cannot double-drop shards. */
    public static boolean damageWeapon(ItemStack stack) {
        int max = stack.getMaxDamage(), damage = stack.getDamageValue();
        int resistance = flameResistancePercent(stack);
        long wear = Math.max(1, ((long)max * (100 - resistance) + 999) / 1000);
        long next = (long)damage + wear;
        if (next >= max) return false;
        stack.setDamageValue((int)next); return true;
    }

    /** Only a genuine forged resistance bonus changes the default rank damage and ten-percent wear. */
    public static float flameResistanceMultiplier(ItemStack stack) {
        int resistance = flameResistancePercent(stack);
        return resistance == 0 ? 1 : 1 - resistance / 100F;
    }

    private static int flameResistancePercent(ItemStack stack) {
        return Math.clamp(ForgingSystem.forgedStat(stack, "tribulation_resistance_percent"), 0, 50);
    }
    private static void shatter(ServerLevel level, Vec3 position, ItemStack stack) {
        mark(stack, null);
        var shards = WeaponShardSystem.split(stack, level.registryAccess());
        if (shards.isEmpty()) shards = List.of(new ItemStack(ModItems.WEAPON_SHARD.get()));
        for (var shard : shards) { var entity = new ItemEntity(level, position.x, position.y, position.z, shard); entity.setDefaultPickUpDelay(); level.addFreshEntity(entity); }
        stack.setCount(0);
        level.playSound(null, BlockPos.containing(position), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.4F, .65F);
    }
    private static void finish(MinecraftServer server, Trial trial, Target target, boolean success) {
        boolean advanced = false;
        if (target != null) {
            mark(target.stack, null);
            if (success) advanced = WeaponRestrictionSystem.evolve(target.stack);
            if (target.item != null) {
                target.item.setItem(target.stack.copy()); target.item.setInvulnerable(false); target.item.setExtendedLifetime();
            }
            if (advanced) successEffects(target.carrier != null ? target.carrier.serverLevel() : (ServerLevel)target.item.level(), trial.position);
        }
        var owner = server.getPlayerList().getPlayer(trial.owner);
        if (owner != null) {
            if (advanced && !trial.ownerDied && owner.isAlive()) { com.example.immortal_cultivation_mod.guidance.DaoGuidanceObservations.record(owner, "weapon_trial_survived"); }
            if (advanced && !trial.ownerDied && owner.isAlive()
                    && owner.serverLevel().dimension().location().equals(trial.dimension)) {
                com.example.immortal_cultivation_mod.flame.FlameAcquisition.onPowerfulTribulationSurvived(
                        owner, trial.position, trial.id, trial.originalTier, trial.power);
            }
            overlay(owner, 5, 0, 0);
            owner.sendSystemMessage(Component.translatable("message.immortal_cultivation_mod.weapon_trial." + (advanced ? "success" : "failure")));
        }
        var data = WeaponTribulationData.get(server); data.trials.remove(trial.id); data.setDirty(); releaseTicket(trial.id);
    }
    static void abort(MinecraftServer server, UUID id) {
        var trial = WeaponTribulationData.get(server).trials.get(id);
        if (trial != null) finish(server, trial, find(server, trial), false);
    }

    /** Called before vanilla death inventory handling, including keepInventory worlds. */
    public static void forceDrop(ServerPlayer player, boolean death) {
        var data = WeaponTribulationData.get(player.server);
        if (death) for (var active : data.trials.values()) {
            if (active.owner.equals(player.getUUID())) active.ownerDied = true;
        }
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            var stack = player.getInventory().getItem(slot); var trial = data.trials.get(token(stack));
            if (trial == null) continue;
            player.getInventory().setItem(slot, ItemStack.EMPTY); forceDrop(player, stack, trial, death);
        }
        var carried = player.containerMenu.getCarried(); var trial = data.trials.get(token(carried));
        if (trial != null) { player.containerMenu.setCarried(ItemStack.EMPTY); forceDrop(player, carried, trial, death); }
        data.setDirty();
    }
    private static void forceDrop(ServerPlayer player, ItemStack stack, Trial trial, boolean death) {
        trial.groundOnly = true;
        if (death && trial.hits > 0) trial.hits--; // Fatal strike must also be endured by the dropped weapon.
        spawn(player.serverLevel(), trial, stack, player.position().add(0, .5, 0));
        overlay(player, 5, 0, 0);
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void death(LivingDeathEvent event) {
        if (!event.isCanceled() && event.getEntity() instanceof ServerPlayer player) forceDrop(player, true);
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) forceDrop(player, false);
    }
    @SubscribeEvent public static void dimensionChanged(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var data = WeaponTribulationData.get(player.server);
        for (var trial : data.trials.values()) {
            if (trial.id.equals(token(player.getMainHandItem())) || trial.id.equals(token(player.getOffhandItem()))) {
                updatePosition(trial, player.serverLevel(), player.position());
                overlay(player, 0, Math.max(1, trial.duration - trial.age), .65F); data.setDirty();
            }
        }
    }
    @SubscribeEvent public static void stopping(net.neoforged.neoforge.event.server.ServerStoppingEvent event) {
        for (var player : event.getServer().getPlayerList().getPlayers()) forceDrop(player, false);
    }
    @SubscribeEvent public static void stopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) { TICKETS.clear(); }
    private static void overlay(ServerPlayer player, int event, int duration, float strength) {
        PacketDistributor.sendToPlayer(player, new ModPayloads.ClientboundJindanTribulationPayload(event, duration, strength));
    }
    private static void clouds(ServerLevel level, Vec3 position, Trial trial) {
        double radius = 2 + Math.min(6, Math.log1p(trial.power));
        level.sendParticles(ParticleTypes.LARGE_SMOKE, position.x, position.y + 7, position.z, 8, radius, .5, radius, .005);
        for (int i = 0; i < 8; i++) {
            double angle = trial.age * .025 + i * Math.PI / 4;
            level.sendParticles(PALE, position.x + Math.cos(angle) * radius, position.y + 7 + Math.sin(angle * 3) * .4,
                    position.z + Math.sin(angle) * radius, 1, .05, .1, .05, 0);
        }
    }
    private static void lightning(ServerLevel level, Vec3 position, Trial trial) {
        com.example.immortal_cultivation_mod.flame.FlameAcquisition.onWeaponLightning(level, position);
        var bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) { bolt.moveTo(position); bolt.setVisualOnly(true); level.addFreshEntity(bolt); }
        level.playSound(null, BlockPos.containing(position), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 2F, .7F);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, position.x, position.y + .6, position.z, 40, .8, 1.5, .8, .15);
        level.sendParticles(ParticleTypes.FLASH, position.x, position.y + .6, position.z, 1, 0, 0, 0, 0);
    }
    private static void successEffects(ServerLevel level, Vec3 position) {
        level.sendParticles(GOLD, position.x, position.y + .5, position.z, 100, 1.5, 1.3, 1.5, .08);
        level.sendParticles(ParticleTypes.END_ROD, position.x, position.y + .5, position.z, 55, 1, 1.5, 1, .05);
        level.playSound(null, BlockPos.containing(position), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.5F, .8F);
    }
}
