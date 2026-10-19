package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Rank-three (Fabao) and higher weapons bind on their first accepted damaging hit. */
@EventBusSubscriber(modid = ImmortalCultivationMod.MODID)
public final class WeaponBinding {
    private static final String TAG = "immortal_cultivation_mod.weapon_binding";
    private record Hit(ItemStack weapon, DamageSource source) {}
    private static final ThreadLocal<Hit> ACTIVE = new ThreadLocal<>();
    private WeaponBinding() {}

    public static boolean eligible(ItemStack stack) {
        return WeaponRestrictionSystem.isWeapon(stack) && WeaponRestrictionSystem.tier(stack) >= 3;
    }
    private static CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound(TAG);
    }
    private static void save(ItemStack stack, CompoundTag value) {
        var root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        root.put(TAG, value); stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }
    /** Identity is not ownership. Misses, charging and equipping never bind a weapon. */
    public static void prepare(ItemStack stack) {
        if (!eligible(stack)) return;
        var value = data(stack);
        if (!value.hasUUID("weapon")) { value.putUUID("weapon", UUID.randomUUID()); save(stack, value); }
    }
    public static UUID owner(ItemStack stack) {
        var value = data(stack); return value.hasUUID("owner") ? value.getUUID("owner") : null;
    }
    private static WeaponBindingData.Owner resolve(ItemStack stack, LivingEntity actor) {
        if (stack.isEmpty()) return null;
        var value = data(stack);
        if (!value.hasUUID("weapon") && !value.hasUUID("owner")) return null;
        if (actor.level() instanceof ServerLevel level) {
            var registry = WeaponBindingData.get(level.getServer());
            var stored = value.hasUUID("weapon") ? registry.owner(value.getUUID("weapon")) : null;
            if (stored != null) {
                if (!stored.uuid().equals(owner(stack)) || !stored.name().equals(value.getString("name"))) {
                    value.putUUID("owner", stored.uuid()); value.putString("name", stored.name()); save(stack, value);
                }
                return stored;
            }
            if (value.hasUUID("weapon") && value.hasUUID("owner"))
                return registry.bind(value.getUUID("weapon"), new WeaponBindingData.Owner(value.getUUID("owner"), value.getString("name")));
        }
        return value.hasUUID("owner") ? new WeaponBindingData.Owner(value.getUUID("owner"), value.getString("name")) : null;
    }
    public static boolean canUse(ItemStack stack, LivingEntity actor) {
        var binding = resolve(stack, actor);
        return binding == null || actor instanceof Player && binding.uuid().equals(actor.getUUID());
    }
    public static boolean checkUse(ItemStack stack, LivingEntity actor) {
        if (canUse(stack, actor)) return true;
        if (actor instanceof ServerPlayer player) {
            var binding = resolve(stack, actor);
            long now = player.level().getGameTime();
            if (player.getPersistentData().getLong("WeaponBindingNoticeUntil") <= now) {
                player.displayClientMessage(Component.translatable("message.immortal_cultivation_mod.weapon_bound_other", binding.name()), true);
                player.getPersistentData().putLong("WeaponBindingNoticeUntil", now + 20);
            }
        }
        return false;
    }
    private static void bind(ItemStack stack, ServerPlayer player) {
        if (!eligible(stack) || !canUse(stack, player)) return;
        prepare(stack);
        var value = data(stack);
        var binding = WeaponBindingData.get(player.getServer()).bind(value.getUUID("weapon"),
                new WeaponBindingData.Owner(player.getUUID(), player.getGameProfile().getName()));
        value.putUUID("owner", binding.uuid()); value.putString("name", binding.name()); save(stack, value);
        player.containerMenu.broadcastChanges();
    }
    public static Component tooltip(ItemStack stack) {
        var value = data(stack);
        return value.hasUUID("owner")
                ? Component.translatable("tooltip.immortal_cultivation_mod.weapon_owner", value.getString("name")).withStyle(ChatFormatting.GOLD)
                : eligible(stack) ? Component.translatable("tooltip.immortal_cultivation_mod.weapon_unbound").withStyle(ChatFormatting.GRAY) : Component.empty();
    }
    public static boolean withWeapon(ItemStack weapon, DamageSource source, BooleanSupplier hit) {
        if (source.getEntity() instanceof LivingEntity actor && !checkUse(weapon, actor)) return false;
        Hit previous = ACTIVE.get(); ACTIVE.set(new Hit(weapon, source));
        try { return hit.getAsBoolean(); }
        finally { if (previous == null) ACTIVE.remove(); else ACTIVE.set(previous); }
    }
    private static ItemStack weapon(DamageSource source) {
        Hit scoped = ACTIVE.get();
        if (scoped != null && scoped.source() == source) return scoped.weapon();
        if (source.getDirectEntity() instanceof LivingEntity
                && !source.is(DamageTypes.PLAYER_ATTACK) && !source.is(DamageTypes.MOB_ATTACK)
                && !source.typeHolder().unwrapKey().map(key -> key.location().toString().equals("immortal_cultivation_mod:whip")).orElse(false)) return ItemStack.EMPTY;
        var weapon = source.getWeaponItem();
        return weapon == null ? ItemStack.EMPTY : weapon;
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void incoming(LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity actor && !actor.level().isClientSide
                && !checkUse(weapon(event.getSource()), actor)) event.setCanceled(true);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void damaged(LivingDamageEvent.Post event) {
        if (event.getNewDamage() > 0 && Float.isFinite(event.getNewDamage())
                && event.getSource().getEntity() instanceof ServerPlayer player) bind(weapon(event.getSource()), player);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void attack(AttackEntityEvent event) {
        var stack = event.getEntity().getMainHandItem();
        if (!checkUse(stack, event.getEntity())) event.setCanceled(true);
        else if (!event.getEntity().level().isClientSide) prepare(stack);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void start(LivingEntityUseItemEvent.Start event) {
        if (!checkUse(event.getItem(), event.getEntity())) event.setCanceled(true);
        else if (!event.getEntity().level().isClientSide) prepare(event.getItem());
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void rightItem(PlayerInteractEvent.RightClickItem event) {
        if (!checkUse(event.getItemStack(), event.getEntity())) { event.setCanceled(true); event.setCancellationResult(InteractionResult.FAIL); }
        else if (!event.getLevel().isClientSide) prepare(event.getItemStack());
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void rightBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!checkUse(event.getItemStack(), event.getEntity())) { event.setCanceled(true); event.setCancellationResult(InteractionResult.FAIL); }
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void rightEntity(PlayerInteractEvent.EntityInteract event) {
        if (!checkUse(event.getItemStack(), event.getEntity())) { event.setCanceled(true); event.setCancellationResult(InteractionResult.FAIL); }
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void rightEntitySpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!checkUse(event.getItemStack(), event.getEntity())) { event.setCanceled(true); event.setCancellationResult(InteractionResult.FAIL); }
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void breakBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!checkUse(event.getItemStack(), event.getEntity())) event.setCanceled(true);
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 10 != 0) return;
        for (var stack : player.getInventory().items) resolve(stack, player);
        for (var stack : player.getInventory().offhand) resolve(stack, player);
        if (player.isUsingItem() && !canUse(player.getUseItem(), player)) player.stopUsingItem();
    }
}
