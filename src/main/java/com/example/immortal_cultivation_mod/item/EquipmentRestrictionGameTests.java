package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.event.EquipmentRestrictionEvents;
import com.example.immortal_cultivation_mod.screen.RestrictionTableMenu;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;
import static com.example.immortal_cultivation_mod.item.WeaponRestrictionSystem.*;

@GameTestHolder("immortal_cultivation_mod")
@PrefixGameTestTemplate(false)
public final class EquipmentRestrictionGameTests {
    @GameTest(template = "fate_test")
    public static void beastAttackTypesMatchArmorRestrictions(GameTestHelper helper) {
        var player = player(helper);
        var beast = com.example.immortal_cultivation_mod.entity.ModEntities.EMBER_FEATHER_FIRE_OWL.get().create(helper.getLevel());
        var projectile = com.example.immortal_cultivation_mod.entity.ModEntities.CULTIVATION_BEAST_PROJECTILE.get().create(helper.getLevel());
        var patch = com.example.immortal_cultivation_mod.entity.ModEntities.CULTIVATION_BEAST_FIRE_PATCH.get().create(helper.getLevel());
        try {
            var armor = new ItemStack(Items.IRON_CHESTPLATE);
            int[] additions = layers(RestrictionType.PROTECTION, 20);
            additions[RestrictionType.PROJECTILE_PROTECTION.ordinal()] = 20;
            additions[RestrictionType.FIRE_PROTECTION.ordinal()] = 20;
            helper.assertTrue(addRestrictions(armor, additions), "Armor accepts matching defensive restrictions");
            player.setItemSlot(EquipmentSlot.CHEST, armor);
            helper.assertTrue(Math.abs(armorDamageMultiplier(player, player.damageSources().mobAttack(beast)) - .95F) < .001F,
                    "Ordinary beast melee receives general protection only");
            for (var kind : com.example.immortal_cultivation_mod.entity.CultivationBeastProjectileEntity.Kind.values()) {
                projectile.configure(beast, kind, 100);
                float expected = kind == com.example.immortal_cultivation_mod.entity.CultivationBeastProjectileEntity.Kind.FIRE_FEATHER ? .75F : .85F;
                helper.assertTrue(Math.abs(armorDamageMultiplier(player, player.damageSources().indirectMagic(projectile, beast)) - expected) < .001F,
                        "Beast projectile receives general/projectile and matching fire defense: " + kind);
            }
            helper.assertTrue(Math.abs(armorDamageMultiplier(player, player.damageSources().indirectMagic(patch, beast)) - .85F) < .001F,
                    "Beast fire patches receive fire protection");
            helper.assertTrue(Math.abs(armorDamageMultiplier(player, player.damageSources().indirectMagic(beast, beast)) - .85F) < .001F,
                    "Fire owl magic receives fire protection");
        } finally { player.discard(); beast.discard(); projectile.discard(); patch.discard(); }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void armorBreakingBeastHitKeepsItsProtection(GameTestHelper helper) throws ReflectiveOperationException {
        var beast = com.example.immortal_cultivation_mod.entity.ModEntities.EMBER_FEATHER_FIRE_OWL.get().create(helper.getLevel());
        float[] losses = new float[2];
        for (int i = 0; i < 2; i++) {
            var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "armor-hit-test")) {
                @Override public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source) { return false; }
            };
            try {
                // Fake players never tick away ServerPlayer's spawn immunity. Clear it only in this fixture.
                var spawnImmunity = net.minecraft.server.level.ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
                spawnImmunity.setAccessible(true);
                spawnImmunity.setInt(player, 0);
                player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000);
                player.setHealth(1000);
                var armor = new ItemStack(Items.IRON_CHESTPLATE);
                if (i == 1) helper.assertTrue(addRestrictions(armor, layers(RestrictionType.PROTECTION, 40)), "Test armor restricted");
                armor.setDamageValue(armor.getMaxDamage() - 1);
                player.setItemSlot(EquipmentSlot.CHEST, armor);
                helper.assertTrue(player.hurt(player.damageSources().mobAttack(beast), 100), "Real beast-source hit applies");
                helper.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST).isEmpty(), "Hit breaks worn armor before health damage");
                losses[i] = 1000 - player.getHealth();
            } finally { player.discard(); }
        }
        beast.discard();
        helper.assertTrue(losses[0] > 0 && Math.abs(losses[1] - losses[0] * .9F) < .01F,
                "Broken armor still reduces its breaking hit by the advertised ten percent");
        helper.succeed();
    }

    private static int[] layers(RestrictionType type, int count) {
        int[] result = new int[RestrictionType.values().length];
        result[type.ordinal()] = count;
        return result;
    }

    private static FakePlayer player(GameTestHelper helper) {
        return new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "restriction-test"));
    }

    @GameTest(template = "fate_test")
    public static void equipmentEligibilityAndServerValidation(GameTestHelper helper) {
        var player = player(helper);
        try {
            var menu = new RestrictionTableMenu(0, player.getInventory());
            for (var item : new net.minecraft.world.item.Item[]{Items.DIAMOND_HELMET, Items.IRON_CHESTPLATE,
                    Items.LEATHER_LEGGINGS, Items.NETHERITE_BOOTS, Items.BOW, Items.CROSSBOW, Items.TRIDENT,
                    Items.IRON_SWORD, Items.IRON_AXE, ModItems.DOUPENG.get(), ModItems.SEVEN_STAR_SWORD.get()}) {
                var stack = new ItemStack(item);
                helper.assertTrue(menu.getSlot(0).mayPlace(stack), "Table accepts " + item);
                helper.assertTrue(availableTypes(stack).size() <= 7, "Visible choices fit the table");
            }
            helper.assertTrue(!menu.getSlot(0).mayPlace(new ItemStack(Items.APPLE)), "Food rejected");
            var armor = new ItemStack(Items.IRON_CHESTPLATE);
            helper.assertTrue(!addRestrictions(armor, layers(RestrictionType.SHARPNESS, 1)), "Armor rejects attack restriction");
            helper.assertTrue(!addRestrictions(new ItemStack(Items.IRON_SWORD), layers(RestrictionType.PROTECTION, 1)), "Weapon rejects defense restriction");
            int[] overflow = layers(RestrictionType.STURDY, Integer.MAX_VALUE);
            overflow[RestrictionType.PROTECTION.ordinal()] = Integer.MAX_VALUE;
            helper.assertTrue(!validAdditions(armor, overflow), "Overflow cannot bypass capacity");
            helper.assertTrue(!validAdditions(armor, layers(RestrictionType.STURDY, -1)), "Negative layers rejected");
            helper.assertTrue(tierProgress(armor) == 0 && !hasRestrictions(armor), "Rejected selections leave item untouched");
        } finally { player.discard(); }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void armorEvolutionKeepsDurabilityAndProtection(GameTestHelper helper) {
        var armor = new ItemStack(Items.IRON_CHESTPLATE);
        int base = armor.getMaxDamage();
        armor.setDamageValue(17);
        int[] additions = layers(RestrictionType.STURDY, 36);
        additions[RestrictionType.PROTECTION.ordinal()] = 36;
        helper.assertTrue(addRestrictions(armor, additions), "Armor accepts mixed defensive layers");
        helper.assertTrue(armor.getMaxDamage() == (int)Math.ceil(base * 1.36), "Durability increases");
        helper.assertTrue(evolve(armor), "Armor evolves at 72 layers");
        helper.assertTrue(effectiveCount(armor, RestrictionType.PROTECTION) == 36 && tierProgress(armor) == 0,
                "Merged protection persists");
        helper.assertTrue(addRestrictions(armor, layers(RestrictionType.STURDY, 10)), "Next tier accepts layers");
        helper.assertTrue(armor.getMaxDamage() == (int)Math.ceil(base * 1.46) && armor.getDamageValue() == 17,
                "Durability uses original base without repairing or compounding");
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void defensesOnlyApplyWhenWornAndMatchDamage(GameTestHelper helper) {
        var player = player(helper);
        try {
            var chest = new ItemStack(Items.IRON_CHESTPLATE);
            addRestrictions(chest, layers(RestrictionType.PROTECTION, 40));
            player.getInventory().setItem(9, chest);
            helper.assertTrue(armorDamageMultiplier(player, player.damageSources().magic()) == 1, "Carried armor gives no defense");
            player.getInventory().setItem(9, ItemStack.EMPTY);
            player.setItemSlot(EquipmentSlot.CHEST, chest);
            helper.assertTrue(Math.abs(armorDamageMultiplier(player, player.damageSources().magic()) - .9F) < .001F, "Worn protection reduces damage");
            var cloak = new ItemStack(ModItems.DOUPENG.get());
            addRestrictions(cloak, layers(RestrictionType.FIRE_PROTECTION, 40));
            var curios = CuriosApi.getCuriosInventory(player).orElseThrow();
            curios.getStacksHandler("cloak").orElseThrow().getCosmeticStacks().setStackInSlot(0, cloak.copy());
            helper.assertTrue(Math.abs(armorDamageMultiplier(player, player.damageSources().inFire()) - .9F) < .001F, "Cosmetic armor gives no defense");
            curios.setEquippedCurio("cloak", 0, cloak);
            helper.assertTrue(Math.abs(armorDamageMultiplier(player, player.damageSources().inFire()) - .7F) < .001F, "Worn cloak stacks matching fire defense");
            helper.assertTrue(Math.abs(armorDamageMultiplier(player, player.damageSources().fall()) - .9F) < .001F, "Fire defense does not reduce falls");
            helper.assertTrue(armorDamageMultiplier(player, player.damageSources().fellOutOfWorld()) == 1, "Void damage bypasses defenses");
            player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            curios.setEquippedCurio("cloak", 0, ItemStack.EMPTY);
            helper.assertTrue(armorDamageMultiplier(player, player.damageSources().inFire()) == 1, "Unequipping removes defense immediately");
        } finally { player.discard(); }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void rangedRestrictionsUseLaunchWeaponAfterEvolution(GameTestHelper helper) {
        var player = player(helper);
        try {
            var bow = new ItemStack(Items.BOW);
            addRestrictions(bow, layers(RestrictionType.SHARPNESS, 72));
            helper.assertTrue(evolve(bow), "Bow evolves");
            var arrow = new Arrow(helper.getLevel(), player, new ItemStack(Items.ARROW), bow.copy());
            player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.APPLE));
            var source = player.damageSources().arrow(arrow, player);
            var event = new LivingIncomingDamageEvent(player, new DamageContainer(source, 100));
            EquipmentRestrictionEvents.onProjectileDamage(event);
            helper.assertTrue(Math.abs(event.getAmount() - 172F) < .01F, "Merged sharpness stays on arrow after switching weapons");
            arrow.discard();
        } finally { player.discard(); }
        helper.succeed();
    }
}

