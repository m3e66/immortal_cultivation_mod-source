package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_mod")
@PrefixGameTestTemplate(false)
public final class HigherWeaponInteractionGameTests {
    @GameTest(template = "fate_test")
    public static void fanCastsWithoutShiftToggle(GameTestHelper helper) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "fan-controls"));
        player.setGameMode(GameType.SURVIVAL);
        ModAttachments.setData(player, ModAttachments.getData(player)
                .withCultivationLevel(CultivationLevels.REALM_ZHUJI + CultivationLevels.STAGE_EARLY).withQi(1000));
        var stack = new ItemStack(ModItems.WIND_FEATHER_FAN.get());
        var item = (HigherWeaponItem) stack.getItem();
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        item.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(ModAttachments.getData(player).qi() == 980, "New fan casts immediately without unfolding");
        player.getCooldowns().removeCooldown(item);
        player.setShiftKeyDown(true);
        item.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(ModAttachments.getData(player).qi() == 960, "Sneaking uses ordinary wind blade instead of toggling");
        helper.assertTrue(!HigherWeaponItem.data(stack).contains("WindFanOpen"), "No fold state is created");
        helper.assertTrue(QiInfusedWeapon.baseAttackDamage(stack) == 11, "Fan retains its usable form's damage");
        helper.succeed();
    }
}
