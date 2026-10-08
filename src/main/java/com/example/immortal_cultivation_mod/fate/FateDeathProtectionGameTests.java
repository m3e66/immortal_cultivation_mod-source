package com.example.immortal_cultivation_mod.fate;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.item.TribulationDollItem;
import com.example.immortal_cultivation_mod.spell.FentianLifeRenewal;
import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;

@GameTestHolder(ImmortalCultivationMod.MODID)
@PrefixGameTestTemplate(false)
public final class FateDeathProtectionGameTests {
    private static ServerPlayer player(GameTestHelper helper) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "rescue-order-test"));
        BlockPos pos = helper.absolutePos(new BlockPos(8, 1, 8));
        player.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
        LuckSystem.change(player, 100 - LuckSystem.luck(player));
        return player;
    }

    private static LivingEntity attacker(GameTestHelper helper) {
        var attacker = helper.spawn(EntityType.ZOMBIE, new BlockPos(8, 1, 12));
        attacker.setNoAi(true);
        LuckSystem.change(attacker, -LuckSystem.luck(attacker));
        return attacker;
    }

    private static ItemStack equipDoll(ServerPlayer player, Item doll) {
        var stack = new ItemStack(doll);
        CuriosApi.getCuriosInventory(player).orElseThrow().getCurios().get("charm")
                .getStacks().setStackInSlot(0, stack);
        return stack;
    }

    private static LivingDeathEvent fatal(LivingEntity victim, LivingEntity attacker) {
        victim.setHealth(0);
        var event = new LivingDeathEvent(victim, victim.damageSources().mobAttack(attacker));
        NeoForge.EVENT_BUS.post(event);
        return event;
    }

    @GameTest(template = "fate_test")
    public static void bothDollsAreConsumedBeforePendantAndLuck(GameTestHelper h) {
        var attacker = attacker(h);
        for (var item : List.of(ModItems.TRIBULATION_DOLL.get(), ModItems.PURIFIED_TRIBULATION_DOLL.get())) {
            var player = player(h);
            var doll = equipDoll(player, item);
            var pendant = new ItemStack(ModItems.PROTECTIVE_JADE_PENDANT.get());
            player.getInventory().setItem(1, pendant);
            int age = ModAttachments.getData(player).agePenalty();
            h.assertTrue(fatal(player, attacker).isCanceled() && player.isAlive(), "The equipped doll saves its owner");
            h.assertTrue(doll.isEmpty() && pendant.getCount() == 1 && LuckSystem.luck(player) == 100,
                    "Exactly the doll is consumed; the pendant and Luck remain available");
            h.assertTrue(ModAttachments.getData(player).agePenalty() == age, "A substitution is not counted as death");
            h.assertTrue(fatal(player, attacker).isCanceled() && pendant.isEmpty() && LuckSystem.luck(player) == 100,
                    "The next lethal hit consumes the pendant before Luck");
            h.assertTrue(fatal(player, attacker).isCanceled() && LuckSystem.luck(player) >= 85 && LuckSystem.luck(player) <= 90,
                    "Luck rescues only after the equipment has been used, charging once");
            h.assertTrue(ModAttachments.getData(player).agePenalty() == age, "Luck rescue also avoids death penalties");
            player.discard();
        }
        attacker.discard();
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void unusableDollFallsBackToLuckWithoutConsumingIt(GameTestHelper h) {
        var player = player(h);
        var attacker = attacker(h);
        var doll = equipDoll(player, ModItems.TRIBULATION_DOLL.get());
        player.setPos(player.getX(), player.getY() + 64, player.getZ());
        h.assertTrue(fatal(player, attacker).isCanceled() && player.isAlive(), "Luck can rescue when no doll landing exists");
        h.assertTrue(doll.getCount() == 1 && LuckSystem.luck(player) >= 85 && LuckSystem.luck(player) <= 90,
                "An unusable doll is kept and only Luck is spent");
        player.discard(); attacker.discard(); h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void canceledDeathNeverSpendsAnotherDoll(GameTestHelper h) {
        var player = player(h);
        var attacker = attacker(h);
        var doll = equipDoll(player, ModItems.TRIBULATION_DOLL.get());
        var event = new LivingDeathEvent(player, player.damageSources().mobAttack(attacker));
        event.setCanceled(true);
        // Direct calls must honor cancellation as well as registered event dispatch.
        TribulationDollItem.preventDeath(event);
        LuckSystem.miracle(event);
        h.assertTrue(doll.getCount() == 1 && LuckSystem.luck(player) == 100,
                "Already rescued deaths never spend a doll or Luck again");
        player.discard(); attacker.discard(); h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void lifeRenewalRunsBeforeLuckAndDeathCleanup(GameTestHelper h) {
        var player = player(h);
        var attacker = attacker(h);
        ModAttachments.setData(player, ModAttachments.getData(player)
                .withActiveCultivationMethod(CultivationMethods.FENTIAN_LIFE_RENEWAL));
        int age = ModAttachments.getData(player).agePenalty();
        h.assertTrue(fatal(player, attacker).isCanceled() && player.isAlive() && FentianLifeRenewal.isPending(player),
                "Life renewal starts before Luck rescue");
        h.assertTrue(LuckSystem.luck(player) == 100 && ModAttachments.getData(player).agePenalty() == age,
                "Life renewal spends no Luck and applies no death penalty");
        player.setHealth(player.getMaxHealth());
        FentianLifeRenewal.clear(player);
        player.discard(); attacker.discard(); h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void normalPriorityProtectionRunsBeforeLuck(GameTestHelper h) {
        var player = player(h);
        var attacker = attacker(h);
        Consumer<LivingDeathEvent> protection = event -> {
            if (event.getEntity() == player) {
                player.setHealth(1);
                event.setCanceled(true);
            }
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, LivingDeathEvent.class, protection);
        try {
            h.assertTrue(fatal(player, attacker).isCanceled() && player.isAlive() && LuckSystem.luck(player) == 100,
                    "A normal-priority protection handler can save the victim without spending Luck");
        } finally {
            NeoForge.EVENT_BUS.unregister(protection);
            player.discard(); attacker.discard();
        }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void vanillaTotemIsUsedBeforeLuckOnActualLethalDamage(GameTestHelper h) {
        var victim = h.spawn(EntityType.COW, new BlockPos(8, 1, 8));
        var attacker = attacker(h);
        victim.setNoAi(true);
        LuckSystem.change(victim, 100 - LuckSystem.luck(victim));
        var totem = new ItemStack(Items.TOTEM_OF_UNDYING);
        victim.setItemSlot(EquipmentSlot.OFFHAND, totem);
        victim.hurt(victim.damageSources().mobAttack(attacker), 1000);
        h.assertTrue(victim.isAlive() && totem.isEmpty() && LuckSystem.luck(victim) == 100,
                "The vanilla lethal-damage path consumes a totem before Luck gets involved");
        victim.discard(); attacker.discard(); h.succeed();
    }
}
