package com.example.immortal_cultivation_mod.event;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.item.DisguiseMaskItem;
import com.example.immortal_cultivation_mod.item.LayeredClothingSlots;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.ArmorHurtEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

@EventBusSubscriber(modid = ImmortalCultivationMod.MODID)
public final class LayeredClothingEvents {
    private static final ResourceLocation LAYERED_ARMOR =
            ResourceLocation.fromNamespaceAndPath(
                    ImmortalCultivationMod.MODID,
                    "layered_clothing_armor");

    private static final ResourceLocation LAYERED_TOUGHNESS =
            ResourceLocation.fromNamespaceAndPath(
                    ImmortalCultivationMod.MODID,
                    "layered_clothing_toughness");

    private static final Map<UUID, Integer> PENDING_ARMOR_DAMAGE =
            new ConcurrentHashMap<>();

    private LayeredClothingEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onIncomingDamage(
            LivingIncomingDamageEvent event) {
        if (!(event.getEntity()
                instanceof ServerPlayer player)) {
            return;
        }

        int durabilityDamage =
                (int)Math.max(
                        1.0F,
                        event.getAmount() / 4.0F);

        PENDING_ARMOR_DAMAGE.put(
                player.getUUID(),
                durabilityDamage);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onArmorHurt(
            ArmorHurtEvent event) {
        if (!(event.getEntity()
                instanceof ServerPlayer player)) {
            return;
        }

        Integer pending =
                PENDING_ARMOR_DAMAGE.remove(
                        player.getUUID());

        int durabilityDamage =
                event.getArmorMap()
                        .values()
                        .stream()
                        .mapToInt(entry ->
                                Math.max(
                                        0,
                                        (int)entry.originalDamage))
                        .max()
                        .orElse(0);

        if (durabilityDamage <= 0) {
            durabilityDamage =
                    pending == null
                            ? 1
                            : Math.max(1, pending);
        }

        DamageSource source =
                event.getDamageSource();

        EquippedCurio outer =
                findDamageableClothing(
                        player,
                        LayeredClothingSlots.OUTER_CLOTHES,
                        source);

        if (outer != null) {
            event.setNewDamage(
                    EquipmentSlot.CHEST,
                    0.0F);

            damageCurio(
                    player,
                    outer,
                    durabilityDamage);

            return;
        }

        ItemStack chestplate =
                event.getArmorItemStack(
                        EquipmentSlot.CHEST);

        boolean chestplateTakesDamage =
                !chestplate.isEmpty()
                        && chestplate.getItem()
                        instanceof ArmorItem
                        && chestplate.canBeHurtBy(source)
                        && event.getNewDamage(
                        EquipmentSlot.CHEST) > 0.0F;

        if (chestplateTakesDamage) {
            return;
        }

        EquippedCurio inner =
                findDamageableClothing(
                        player,
                        LayeredClothingSlots.INNER_CLOTHES,
                        source);

        if (inner != null) {
            damageCurio(
                    player,
                    inner,
                    durabilityDamage);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(
            PlayerTickEvent.Post event) {
        if (!(event.getEntity()
                instanceof ServerPlayer player)) {
            return;
        }

        ItemStack outer =
                getFirstEquipped(
                        player,
                        LayeredClothingSlots.OUTER_CLOTHES);

        ItemStack inner =
                getFirstEquipped(
                        player,
                        LayeredClothingSlots.INNER_CLOTHES);

        double armor =
                armorValue(outer)
                        + armorValue(inner);

        double toughness =
                toughnessValue(outer)
                        + toughnessValue(inner);

        updateAttribute(
                player.getAttribute(Attributes.ARMOR),
                LAYERED_ARMOR,
                armor);

        updateAttribute(
                player.getAttribute(
                        Attributes.ARMOR_TOUGHNESS),
                LAYERED_TOUGHNESS,
                toughness);

        ensureCurioMaskSkin(player);
    }

    private static void ensureCurioMaskSkin(
            ServerPlayer player) {
        CuriosApi.getCuriosInventory(player)
                .ifPresent(handler ->
                        handler.getStacksHandler(
                                LayeredClothingSlots.HEAD_ACCESSORY)
                                .ifPresent(stacksHandler -> {
                                    var stacks =
                                            stacksHandler.getStacks();

                                    for (int slot = 0;
                                         slot < stacks.getSlots();
                                         slot++) {
                                        ItemStack equipped =
                                                stacks.getStackInSlot(slot);

                                        if (!equipped.is(
                                                com.example.immortal_cultivation_mod.item.ModItems.DISGUISE_MASK.get())) {
                                            continue;
                                        }

                                        ItemStack updated =
                                                equipped.copy();

                                        if (DisguiseMaskItem
                                                .ensureDisguiseSkin(
                                                        updated,
                                                        player)) {
                                            handler.setEquippedCurio(
                                                    LayeredClothingSlots.HEAD_ACCESSORY,
                                                    slot,
                                                    updated);
                                        }

                                        return;
                                    }
                                }));
    }

    private static void updateAttribute(
            AttributeInstance attribute,
            ResourceLocation id,
            double amount) {
        if (attribute == null) {
            return;
        }

        AttributeModifier current =
                attribute.getModifier(id);

        if (amount <= 0.0D) {
            if (current != null) {
                attribute.removeModifier(id);
            }

            return;
        }

        if (current != null
                && Double.compare(
                current.amount(),
                amount) == 0) {
            return;
        }

        if (current != null) {
            attribute.removeModifier(id);
        }

        attribute.addTransientModifier(
                new AttributeModifier(
                        id,
                        amount,
                        AttributeModifier.Operation.ADD_VALUE));
    }

    private static double armorValue(
            ItemStack stack) {
        if (stack.getItem()
                instanceof ArmorItem armorItem) {
            return armorItem.getDefense();
        }

        return 0.0D;
    }

    private static double toughnessValue(
            ItemStack stack) {
        if (stack.getItem()
                instanceof ArmorItem armorItem) {
            return armorItem.getToughness();
        }

        return 0.0D;
    }

    private static ItemStack getFirstEquipped(
            ServerPlayer player,
            String slotId) {
        ItemStack[] result = {ItemStack.EMPTY};

        CuriosApi.getCuriosInventory(player)
                .ifPresent(handler ->
                        handler.getStacksHandler(slotId)
                                .ifPresent(stacksHandler -> {
                                    var stacks =
                                            stacksHandler.getStacks();

                                    for (int slot = 0;
                                         slot < stacks.getSlots();
                                         slot++) {
                                        ItemStack stack =
                                                stacks.getStackInSlot(slot);

                                        if (!stack.isEmpty()) {
                                            result[0] = stack;
                                            return;
                                        }
                                    }
                                }));

        return result[0];
    }

    private static EquippedCurio findDamageableClothing(
            ServerPlayer player,
            String slotId,
            DamageSource source) {
        EquippedCurio[] result = {null};

        CuriosApi.getCuriosInventory(player)
                .ifPresent(handler ->
                        handler.getStacksHandler(slotId)
                                .ifPresent(stacksHandler -> {
                                    var stacks =
                                            stacksHandler.getStacks();

                                    for (int slot = 0;
                                         slot < stacks.getSlots();
                                         slot++) {
                                        ItemStack stack =
                                                stacks.getStackInSlot(slot);

                                        if (!stack.isEmpty()
                                                && stack.isDamageableItem()
                                                && stack.canBeHurtBy(source)) {
                                            result[0] =
                                                    new EquippedCurio(
                                                            handler,
                                                            slotId,
                                                            slot,
                                                            stack);
                                            return;
                                        }
                                    }
                                }));

        return result[0];
    }

    private static void damageCurio(
            ServerPlayer player,
            EquippedCurio curio,
            int damage) {
        ItemStack damaged =
                curio.stack().copy();

        damaged.hurtAndBreak(
                damage,
                player,
                EquipmentSlot.CHEST);

        curio.handler().setEquippedCurio(
                curio.slotId(),
                curio.index(),
                damaged);
    }

    private record EquippedCurio(
            ICuriosItemHandler handler,
            String slotId,
            int index,
            ItemStack stack) {
    }
}
