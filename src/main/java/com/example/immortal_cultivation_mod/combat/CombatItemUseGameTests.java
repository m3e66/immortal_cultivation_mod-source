package com.example.immortal_cultivation_mod.combat;

import com.example.immortal_cultivation_mod.event.ServerEvents;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_qi_domain")
@PrefixGameTestTemplate(false)
public final class CombatItemUseGameTests {
    private static FakePlayer player(GameTestHelper h) {
        var player = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "combat-item-use"));
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(h.absoluteVec(new Vec3(2, 6000, 2)));
        return player;
    }

    @GameTest(template = "fate_test")
    public static void itemUseIsNotInterceptedInManualOrAutomaticCombat(GameTestHelper h) {
        var player = player(h);
        try {
            for (int mode = 0; mode < 3; mode++) {
                CombatStateManager.clear(player);
                if (mode == 1) CombatStateManager.toggleManualCombat(player);
                if (mode == 2) CombatStateManager.enterCombat(player);
                h.assertTrue(CombatStateManager.isInCombat(player) == (mode != 0), "Expected combat state selected");
                for (var item : new net.minecraft.world.item.Item[]{Items.ENDER_PEARL, Items.CHORUS_FRUIT, Items.SNOWBALL}) {
                    for (var hand : InteractionHand.values()) {
                        player.setItemInHand(hand, new ItemStack(item, 2));
                        var event = new PlayerInteractEvent.RightClickItem(player, hand);
                        ServerEvents.onRightClickItem(event);
                        h.assertTrue(!event.isCanceled(), "Combat does not intercept use of " + item + " in " + hand);
                        h.assertTrue(player.getItemInHand(hand).getCount() == 2, "Interaction hook does not consume or queue an item");
                    }
                }
            }
        } finally {
            CombatStateManager.onLogout(player);
            player.discard();
        }
        h.succeed();
    }

    @GameTest(template = "fate_test", timeoutTicks = 100)
    public static void pearlLaunchesImmediatelyAndNeverReplaysAfterThreeSeconds(GameTestHelper h) {
        var player = player(h);
        CombatStateManager.toggleManualCombat(player);
        var stack = new ItemStack(Items.ENDER_PEARL, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var event = new PlayerInteractEvent.RightClickItem(player, InteractionHand.MAIN_HAND);
        ServerEvents.onRightClickItem(event);
        h.assertTrue(!event.isCanceled(), "Pearl use reaches its normal implementation immediately");
        var result = stack.use(h.getLevel(), player, InteractionHand.MAIN_HAND);
        h.assertTrue(result.getResult().consumesAction() && stack.getCount() == 1, "Pearl throws and consumes one item on the same tick");
        h.assertTrue(!h.getLevel().getEntitiesOfClass(ThrownEnderpearl.class, player.getBoundingBox().inflate(4),
                pearl -> pearl.getOwner() == player).isEmpty(), "Pearl projectile exists without a channel wait");
        h.runAfterDelay(65, () -> {
            try {
                CombatStateManager.tick(player);
                h.assertTrue(stack.getCount() == 1, "No delayed replay consumes another pearl after three seconds");
                h.succeed();
            } finally {
                h.getLevel().getEntitiesOfClass(ThrownEnderpearl.class, player.getBoundingBox().inflate(150),
                        pearl -> pearl.getOwner() == player).forEach(ThrownEnderpearl::discard);
                CombatStateManager.onLogout(player);
                player.discard();
            }
        });
    }
}
