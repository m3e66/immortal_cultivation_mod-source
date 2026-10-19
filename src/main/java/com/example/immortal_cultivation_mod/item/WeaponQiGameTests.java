package com.example.immortal_cultivation_mod.item;

import java.util.ArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_mod")
@PrefixGameTestTemplate(false)
public final class WeaponQiGameTests {
    @GameTest(template = "fate_test")
    public static void externalQiWidthGrowsWithWeaponLevelAndStaysBounded(GameTestHelper helper) {
        var weapon = new ItemStack(Items.IRON_SWORD);
        double previous = 0;
        for (int tier = 0; tier <= WeaponRestrictionSystem.MAX_TIER; tier++) {
            WeaponRestrictionSystem.setTier(weapon, tier);
            double radius = com.example.immortal_cultivation_mod.combat.ExternalQiSize.radius(weapon, 1, false, false, false);
            helper.assertTrue(radius > previous && radius <= 32, "Higher weapon levels produce larger external Qi");
            previous = radius;
        }
        helper.assertTrue(com.example.immortal_cultivation_mod.combat.ExternalQiSize.radius(weapon, 2, false, false, false) == 32,
                "High-level gear and maximum scroll size respect the shared size cap");
        for (double radius : new double[]{1, 2, 8, 16, 32}) {
            helper.assertTrue(Math.abs(com.example.immortal_cultivation_mod.effect.ExternalQiVfxShape.halfThickness(radius)
                    - 4 * (.2 + radius * .025)) < .00001, "Blade body is four times thicker without extra reach");
        }
        for (var shape : new com.example.immortal_cultivation_mod.network.HigherWeaponPayloads.Visual[]{
                com.example.immortal_cultivation_mod.network.HigherWeaponPayloads.Visual.QI_RISING,
                com.example.immortal_cultivation_mod.network.HigherWeaponPayloads.Visual.QI_FALLING,
                com.example.immortal_cultivation_mod.network.HigherWeaponPayloads.Visual.QI_LEVEL,
                com.example.immortal_cultivation_mod.network.HigherWeaponPayloads.Visual.QI_CONE}) {
            helper.assertTrue(com.example.immortal_cultivation_mod.network.HigherWeaponPayloads.clampWidth(shape,48) == 32,
                    "Packet and renderer share the restored blade-length cap: " + shape);
        }
        var bracer = new ItemStack(ModItems.HEAVY_IRON_BRACER.get());
        WeaponRestrictionSystem.setTier(bracer, WeaponRestrictionSystem.MAX_TIER);
        helper.assertTrue(com.example.immortal_cultivation_mod.combat.ExternalQiSize.radius(bracer, 2, true, true, false) == 12
                && com.example.immortal_cultivation_mod.combat.ExternalQiSize.radius(bracer, 2, true, false, false) == 7,
                "Fist waves preserve their distinct maximum sizes");
        int previousBlocks = 0;
        for (float damage : new float[]{1, 10, 25, 100, 1000}) {
            int blocks = com.example.immortal_cultivation_mod.entity.WeaponQiSlashEntity.damageScaledBlockBudget(damage);
            helper.assertTrue(blocks > previousBlocks, "More released damage grants more terrain capacity");
            previousBlocks = blocks;
        }
        helper.assertTrue(com.example.immortal_cultivation_mod.entity.WeaponQiSlashEntity.damageScaledBlockBudget(Float.MAX_VALUE) == 65536,
                "Extreme damage retains a bounded lifetime block budget");
        helper.assertTrue(com.example.immortal_cultivation_mod.entity.WeaponQiSlashEntity.damageScaledBlockBudget(Float.NaN) == 0,
                "Invalid damage cannot grant terrain capacity");
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void oldAndNewWeaponsShareInjectionAutoChannelAndOverflow(GameTestHelper helper) {
        var player = new net.neoforged.neoforge.common.util.FakePlayer(helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "shared-qi"));
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        String realm = com.example.immortal_cultivation_mod.attachment.CultivationLevels.REALM_ZHUJI
                + com.example.immortal_cultivation_mod.attachment.CultivationLevels.STAGE_EARLY;
        com.example.immortal_cultivation_mod.attachment.ModAttachments.setData(player,
                com.example.immortal_cultivation_mod.attachment.ModAttachments.getData(player).withCultivationLevel(realm).withQi(1000000));
        int cost = Math.max(1, com.example.immortal_cultivation_mod.attachment.CultivationLevels.getLevelDef(realm).maxQi() / 100);
        var weapons = new ArrayList<Item>();
        weapons.add(Items.IRON_SWORD);
        for (var spec : HigherWeaponSpec.values()) weapons.add(BuiltInRegistries.ITEM.get(HigherWeaponItem.id(spec.id)));
        for (var item : weapons) {
            var stack = new ItemStack(item);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
            double limit = QiInfusedWeapon.storageLimit(stack);
            QiInfusedWeapon.setQi(stack, limit - .5);
            int before = com.example.immortal_cultivation_mod.attachment.ModAttachments.getData(player).qi();
            QiInfusedWeapon.inject(player);
            helper.assertTrue(Math.abs(QiInfusedWeapon.getQi(stack) - (limit - .5 + cost)) < .001,
                    "Manual injection uses the same amount and permits overflow for old and new weapons: " + item);
            helper.assertTrue(com.example.immortal_cultivation_mod.attachment.ModAttachments.getData(player).qi() == before - cost,
                    "Same injection cost: " + item);
            player.tickCount = 20;
            QiInfusedWeapon.tickOverflow(player);
            helper.assertTrue(stack.getDamageValue() == (int)Math.ceil((cost - .5) / 10),
                    "Same overflow durability loss: " + item);
            QiInfusedWeapon.setQi(stack, limit - .5);
            QiInfusedWeapon.setAutoChannel(player, true);
            QiInfusedWeapon.tickAutoChannel(player);
            helper.assertTrue(QiInfusedWeapon.getQi(stack) == limit, "Auto-channel stops at capacity: " + item);
            QiInfusedWeapon.setQi(stack, 0);
            var lines = new ArrayList<Component>();
            QiInfusedWeapon.appendStoredQiTooltips(stack, lines);
            helper.assertTrue(lines.isEmpty(), "Same hidden zero-Qi tooltip: " + item);
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void newWeaponsShowQiAndUseHundredToFiveConversion(GameTestHelper helper) {
        for (var spec : HigherWeaponSpec.values()) {
            var stack = new ItemStack(BuiltInRegistries.ITEM.get(HigherWeaponItem.id(spec.id)));
            var lines = new ArrayList<Component>();
            QiInfusedWeapon.appendStoredQiTooltips(stack, lines);
            helper.assertTrue(lines.isEmpty(), "Empty new weapons hide the Qi line exactly like old weapons: " + spec);
            QiInfusedWeapon.setQi(stack, 100);
            helper.assertTrue(Math.abs(QiInfusedWeapon.injectedQiDamage(stack) - 5F) < .0001F,
                    "100 Qi adds five damage: " + spec);
            lines.clear();
            QiInfusedWeapon.appendStoredQiTooltips(stack, lines);
            helper.assertTrue(lines.getFirst().getContents() instanceof TranslatableContents contents
                    && contents.getArgs()[0].equals("100") && contents.getArgs()[1].equals("5"),
                    "Tooltip shows actual stored Qi and bonus: " + spec);
            helper.assertTrue(lines.getFirst().getStyle().getColor().getValue() == net.minecraft.ChatFormatting.AQUA.getColor(),
                    "Injected Qi line uses the old weapon cyan color");
            var earlyLines = new ArrayList<Component>();
            stack.getItem().appendHoverText(stack, Item.TooltipContext.of(helper.getLevel()), earlyLines, TooltipFlag.NORMAL);
            helper.assertTrue(earlyLines.isEmpty(), "No duplicate Qi line above attack stats");
            var stats = Component.literal("Attack stats and advanced item information");
            earlyLines.add(stats);
            QiInfusedWeapon.setQi(stack, 270);
            QiInfusedWeapon.appendStoredQiTooltips(stack, earlyLines);
            helper.assertTrue(earlyLines.size() == 2 && earlyLines.getFirst() == stats
                    && earlyLines.getLast().getContents() instanceof TranslatableContents contents
                    && contents.getArgs()[0].equals("270") && contents.getArgs()[1].equals("13.5"),
                    "Qi follows vanilla information and uses the updated 270 Qi / +13.5 damage conversion");
            QiInfusedWeapon.setQi(stack, 100);
            var cast = WeaponSkillAttack.capture(stack);
            helper.assertTrue(Math.abs(cast.damage(100) - (QiInfusedWeapon.baseAttackDamage(stack) + 5)) < .001,
                    "Skill snapshot uses the same Qi conversion: " + spec);
            cast.consumeInjectedQi();
            cast.consumeInjectedQi();
            helper.assertTrue(QiInfusedWeapon.getQi(stack) == 90, "Existing Qi drain is paid once per cast");
        }
        var oldSword = new ItemStack(Items.IRON_SWORD);
        QiInfusedWeapon.setQi(oldSword, 250);
        helper.assertTrue(QiInfusedWeapon.injectedQiDamage(oldSword) == 12.5F,
                "Old weapons use the same fractional conversion");
        QiInfusedWeapon.setQi(oldSword, 1);
        helper.assertTrue(QiInfusedWeapon.format(QiInfusedWeapon.injectedQiDamage(oldSword)).equals("0.05"),
                "Small Qi bonuses remain visible");
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void playerExternalQiCollidesImmediately(GameTestHelper helper) {
        var player = new net.neoforged.neoforge.common.util.FakePlayer(helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "phased-qi"));
        player.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(4, 3, 3)));
        var sword = new ItemStack(Items.IRON_SWORD);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, sword);
        QiInfusedWeapon.setQi(sword, 100);
        QiInfusedWeapon.setOutwardQiRelease(player, true);
        com.example.immortal_cultivation_mod.combat.CombatStateManager.enterCombat(player);
        helper.assertTrue(com.example.immortal_cultivation_mod.entity.WeaponQiSlashEntity.release(player, 2, false),
                "Player releases external Qi normally");
        var shots = helper.getLevel().getEntitiesOfClass(com.example.immortal_cultivation_mod.entity.WeaponQiSlashEntity.class,
                player.getBoundingBox().inflate(3));
        helper.assertTrue(shots.size() == 1 && !shots.getFirst().forcefieldPassThrough(), "Fresh slash respects forcefields");
        var tag = new net.minecraft.nbt.CompoundTag();
        shots.getFirst().saveWithoutId(tag);
        helper.assertTrue(tag.getInt("LaunchPassThroughTicks") == 0, "No launch collision grace period");
        helper.assertTrue(QiInfusedWeapon.getQi(sword) == 90, "Pass-through does not change the release payment");
        shots.getFirst().discard(); player.discard(); helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void fanStoresAllQiInOneVisiblePool(GameTestHelper helper) {
        var fan = new ItemStack(ModItems.WIND_FEATHER_FAN.get());
        QiInfusedWeapon.setQi(fan, 1000);
        var lines = new ArrayList<Component>();
        QiInfusedWeapon.appendStoredQiTooltips(fan, lines);
        helper.assertTrue(lines.size() == 1 && lines.getFirst().getContents() instanceof TranslatableContents contents
                && contents.getArgs()[0].equals("1000") && contents.getArgs()[1].equals("50"),
                "Fan shows all Qi in the normal injected-Qi line");
        helper.assertTrue(QiInfusedWeapon.injectedQiDamage(fan) == 50F, "No Qi remains locked behind folding");
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void externalQiBreaksTerrainOnlyForEnabledHeavyAttacks(GameTestHelper helper) {
        var player = new net.neoforged.neoforge.common.util.FakePlayer(helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "heavy-qi-terrain"));
        player.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(4,3,3)));
        var sword = new ItemStack(Items.IRON_SWORD);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, sword);
        QiInfusedWeapon.setOutwardQiRelease(player,true);
        com.example.immortal_cultivation_mod.combat.CombatStateManager.enterCombat(player);
        for(boolean toggle : new boolean[]{false,true}) for(boolean heavy : new boolean[]{false,true}) {
            QiInfusedWeapon.setQi(sword,100);
            QiInfusedWeapon.setOutwardQiBlockBreaking(player,toggle);
            helper.assertTrue(com.example.immortal_cultivation_mod.entity.WeaponQiSlashEntity.release(player,0,false,heavy),
                    "Both light and heavy attacks still release external Qi");
            var shots=helper.getLevel().getEntitiesOfClass(com.example.immortal_cultivation_mod.entity.WeaponQiSlashEntity.class,
                    player.getBoundingBox().inflate(3));
            helper.assertTrue(shots.size()==1,"One released slash");
            helper.assertTrue(shots.getFirst().allowsTerrainBreaking()==(toggle&&heavy),
                    "Toggle alone never permits a light slash to destroy terrain");
            var tag=new net.minecraft.nbt.CompoundTag(); shots.getFirst().saveWithoutId(tag);
            helper.assertTrue(!tag.getBoolean("Cone"),"Heavy sword retains its normal slash shape");
            helper.assertTrue(QiInfusedWeapon.getQi(sword)==90,"Qi cost unchanged");
            shots.getFirst().discard();
        }
        player.discard();helper.succeed();
    }
}
