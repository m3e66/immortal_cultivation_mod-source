package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.entity.FlyingSwordEntity;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_weapon_binding")
@PrefixGameTestTemplate(false)
public final class WeaponBindingGameTests {
    @GameTest(template="fate_test")
    public static void ranksThreeThroughSevenBindToFirstDamageDealerAndRejectOthers(GameTestHelper h) {
        var a = player(h, "first-owner"); var b = player(h, "other-player");
        var target = target(h);
        for (int tier = 3; tier <= 7; tier++) {
            var stack = weapon(tier); a.setItemInHand(InteractionHand.MAIN_HAND, stack);
            h.assertTrue(WeaponBinding.owner(stack) == null, "New weapon is unbound");
            target.invulnerableTime = 0;
            h.assertTrue(target.hurt(a.damageSources().playerAttack(a), 2), "Owner deals actual damage");
            h.assertTrue(a.getUUID().equals(WeaponBinding.owner(stack)), "First damage binds rank " + tier);
            b.setItemInHand(InteractionHand.MAIN_HAND, stack);
            float health = target.getHealth(); target.invulnerableTime = 0;
            h.assertTrue(!target.hurt(b.damageSources().playerAttack(b), 2) && target.getHealth() == health,
                    "Another player, including creative, cannot damage with owner-bound weapon");
            h.assertTrue(a.getUUID().equals(WeaponBinding.owner(stack)), "Owner cannot be overwritten");
            h.assertTrue(WeaponBinding.canUse(stack, a), "Original owner remains allowed");
        }
        target.discard(); h.succeed();
    }

    @GameTest(template="fate_test")
    public static void missesInvulnerabilityAndUnrelatedSpellsDoNotBind(GameTestHelper h) {
        var a = player(h, "no-hit"); a.setGameMode(GameType.SURVIVAL); var stack = weapon(3); a.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var use = new PlayerInteractEvent.RightClickItem(a, InteractionHand.MAIN_HAND);
        NeoForge.EVENT_BUS.post(use);
        h.assertTrue(WeaponBinding.owner(stack) == null, "Using or charging does not bind");
        var target = target(h); target.setInvulnerable(true);
        target.hurt(a.damageSources().playerAttack(a), 10);
        h.assertTrue(WeaponBinding.owner(stack) == null, "Rejected damage does not bind");
        target.setInvulnerable(false); target.invulnerableTime = 0;
        var projectile = EntityType.SNOWBALL.create(h.getLevel()); projectile.setOwner(a);
        target.hurt(a.damageSources().indirectMagic(projectile, a), 2);
        h.assertTrue(WeaponBinding.owner(stack) == null, "Unrelated spell does not bind held weapon");
        target.discard(); projectile.discard(); h.succeed();
    }

    @GameTest(template="fate_test")
    public static void lowerRanksStayUnboundUntilUpgraded(GameTestHelper h) {
        var a = player(h, "lower-user"); var b = player(h, "rank-three-user"); var target = target(h);
        for (int tier = 0; tier < 3; tier++) {
            var stack = weapon(tier); a.setItemInHand(InteractionHand.MAIN_HAND, stack);
            target.invulnerableTime = 0; target.hurt(a.damageSources().playerAttack(a), 2);
            h.assertTrue(WeaponBinding.owner(stack) == null && WeaponBinding.canUse(stack, b), "Lower rank remains freely usable");
        }
        var stack = weapon(2); a.setItemInHand(InteractionHand.MAIN_HAND, stack);
        target.invulnerableTime = 0; target.hurt(a.damageSources().playerAttack(a), 2);
        WeaponRestrictionSystem.setTier(stack, 3); b.setItemInHand(InteractionHand.MAIN_HAND, stack);
        target.invulnerableTime = 0; target.hurt(b.damageSources().playerAttack(b), 2);
        h.assertTrue(b.getUUID().equals(WeaponBinding.owner(stack)), "First damage after reaching Fabao chooses owner");
        target.discard(); h.succeed();
    }

    @GameTest(template="fate_test")
    public static void transferredDelayedSkillBindsItsWeaponInsteadOfNewHeldItem(GameTestHelper h) {
        var a = player(h, "skill-caster"); var b = player(h, "recipient"); var original = weapon(3);
        a.setItemInHand(InteractionHand.MAIN_HAND, original);
        var skill = WeaponSkillAttack.capture(original);
        var transferred = original.copy(); original.setCount(0);
        b.setItemInHand(InteractionHand.MAIN_HAND, transferred);
        var unrelated = weapon(4); a.setItemInHand(InteractionHand.MAIN_HAND, unrelated);
        var target = target(h); var source = a.damageSources().playerAttack(a);
        h.assertTrue(skill.withWeapon(a, source, () -> target.hurt(source, 2)), "Delayed skill deals damage after transfer");
        h.assertTrue(!WeaponBinding.canUse(transferred, b) && WeaponBinding.canUse(transferred, a), "Transferred weapon follows canonical first-hit owner");
        h.assertTrue(a.getUUID().equals(WeaponBinding.owner(transferred)), "Owner synchronizes onto transferred stack");
        h.assertTrue(WeaponBinding.owner(unrelated) == null, "New held weapon is not incorrectly bound");
        target.discard(); h.succeed();
    }

    @GameTest(template="fate_test")
    public static void projectileDamageAndFlyingSwordRecallRetainBinding(GameTestHelper h) {
        var a = player(h, "projectile-owner"); var b = player(h, "projectile-other");
        var bow = new ItemStack(Items.BOW); WeaponRestrictionSystem.setTier(bow, 3);
        a.setItemInHand(InteractionHand.MAIN_HAND, bow); a.startUsingItem(InteractionHand.MAIN_HAND);
        var arrow = new Arrow(h.getLevel(), a, new ItemStack(Items.ARROW), bow);
        var target = target(h);
        target.hurt(a.damageSources().arrow(arrow, a), 2);
        h.assertTrue(!WeaponBinding.canUse(bow, b), "Arrow binds launch bow through stable weapon identity");
        var swordStack = weapon(3);
        var sword = new FlyingSwordEntity(h.getLevel(), a, a.position()); sword.setStoredSwordStack(swordStack);
        a.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY); target.invulnerableTime = 0;
        var source = a.damageSources().playerAttack(a);
        com.example.immortal_cultivation_mod.flame.FlameWeaponEffects.hurtWithWeapon(sword.getWeaponItem(), target, source, 2);
        var returned = sword.pouchRecallStack();
        h.assertTrue(a.getUUID().equals(WeaponBinding.owner(returned)) && !WeaponBinding.canUse(returned, b), "Recalled physical sword retains owner");
        sword.setOwner(b); sword.setOwnerUuid(b.getUUID());
        h.assertTrue(!sword.canBeControlledBy(b), "Flight controller identity cannot override bound owner");
        a.stopUsingItem(); sword.discard(); arrow.discard(); target.discard(); h.succeed();
    }

    @GameTest(template="fate_test")
    public static void ownershipSurvivesItemAndWorldDataSaveLoadAndRankChanges(GameTestHelper h) {
        var a = player(h, "saved-owner"); var b = player(h, "saved-other"); var stack = weapon(3);
        a.setItemInHand(InteractionHand.MAIN_HAND, stack); var target = target(h);
        target.hurt(a.damageSources().playerAttack(a), 2);
        var loaded = ItemStack.parseOptional(h.getLevel().registryAccess(), (CompoundTag)stack.saveOptional(h.getLevel().registryAccess()));
        h.assertTrue(a.getUUID().equals(WeaponBinding.owner(loaded)) && !WeaponBinding.canUse(loaded, b), "Item serialization preserves UUID ownership");
        var registry = WeaponBindingData.get(h.getLevel().getServer());
        var saved = registry.save(new CompoundTag(), h.getLevel().registryAccess());
        var restored = WeaponBindingData.load(saved, h.getLevel().registryAccess());
        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound("immortal_cultivation_mod.weapon_binding");
        h.assertTrue(a.getUUID().equals(restored.owner(data.getUUID("weapon")).uuid()), "World ownership registry survives save/load");
        WeaponRestrictionSystem.setTier(loaded, 2);
        h.assertTrue(!WeaponBinding.canUse(loaded, b), "Downgrading cannot remove established binding");
        h.assertTrue(!WeaponBinding.tooltip(loaded).getString().isEmpty(), "Bound weapon exposes owner tooltip");
        target.discard(); h.succeed();
    }

    @GameTest(template="fate_test")
    public static void foreignOwnerCannotUseSkillsInfuseQiOrLaunchSword(GameTestHelper h) {
        var a = player(h, "allowed-owner"); var b = player(h, "denied-user");
        var stack = new ItemStack(ModItems.BLACK_IRON_JIAN.get()); WeaponRestrictionSystem.setTier(stack, 3);
        a.setItemInHand(InteractionHand.MAIN_HAND, stack); var target = target(h);
        target.hurt(a.damageSources().playerAttack(a), 2);
        b.setItemInHand(InteractionHand.MAIN_HAND, stack); int qi = ModAttachments.getData(b).qi();
        var use = new PlayerInteractEvent.RightClickItem(b, InteractionHand.MAIN_HAND); NeoForge.EVENT_BUS.post(use);
        h.assertTrue(use.isCanceled(), "Foreign weapon right-click is rejected before item use");
        QiInfusedWeapon.inject(b); HigherWeaponCombat.cast(b, stack, (HigherWeaponItem)stack.getItem(), false);
        h.assertTrue(ModAttachments.getData(b).qi() == qi && QiInfusedWeapon.getQi(stack) == 0, "Rejected skill and Qi infusion spend no resources");
        h.assertTrue(!FlyingSwordEntity.releaseFromHand(b), "Bound sword cannot be launched by another player");
        var npc = EntityType.ZOMBIE.create(h.getLevel()); npc.setItemInHand(InteractionHand.MAIN_HAND, stack.copy());
        h.assertTrue(!WeaponBinding.canUse(npc.getMainHandItem(), npc), "NPC cannot use player-bound weapon");
        target.discard(); npc.discard(); h.succeed();
    }

    @GameTest(template="fate_test")
    public static void whipFuchenAndSoulBannerDamageBindTheirActualWeapons(GameTestHelper h) {
        var a = player(h, "special-owner"); var b = player(h, "special-other"); var target = target(h);
        var whip = new ItemStack(ModItems.WHIP.get()); WeaponRestrictionSystem.setTier(whip, 3);
        a.setItemInHand(InteractionHand.MAIN_HAND, whip);
        h.assertTrue(WhipItem.hitFromQiLine(a, target, 10, 0, false), "Whip custom damage is accepted");
        h.assertTrue(a.getUUID().equals(WeaponBinding.owner(whip)), "Custom whip damage binds whip");
        var fuchen = new ItemStack(ModItems.FUCHEN.get()); WeaponRestrictionSystem.setTier(fuchen, 3);
        a.setItemInHand(InteractionHand.MAIN_HAND, fuchen); target.invulnerableTime = 0;
        h.assertTrue(FuchenItem.startHeavyAttack(a), "Unbound Fuchen can start a spin");
        FuchenItem.tick(a);
        h.assertTrue(!WeaponBinding.canUse(fuchen, b), "Fuchen spin binds its captured weapon");
        FuchenItem.clear(a);
        var banner = new ItemStack(ModItems.SHIHUNFAN.get()); WeaponRestrictionSystem.setTier(banner, 3);
        var wave = com.example.immortal_cultivation_mod.entity.ModEntities.SHIHUNFAN_SOUL_WAVE.get().create(h.getLevel());
        wave.setBindingWeapon(banner); a.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        target.invulnerableTime = 0;
        target.hurt(a.damageSources().indirectMagic(wave, a), 2);
        h.assertTrue(!WeaponBinding.canUse(banner, b) && WeaponBinding.canUse(banner, a), "Delayed soul wave binds banner after caster changes hands");
        wave.discard(); target.discard(); h.succeed();
    }

    private static ItemStack weapon(int tier) {
        var stack = new ItemStack(Items.IRON_SWORD); WeaponRestrictionSystem.setTier(stack, tier); return stack;
    }
    private static FakePlayer player(GameTestHelper h, String name) {
        var player = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), name));
        player.setGameMode(GameType.CREATIVE);
        player.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(3.5, 2, 3.5)));
        ModAttachments.setData(player, ModAttachments.getData(player).withCultivationLevel(CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_EARLY).withQi(10000));
        return player;
    }
    private static net.minecraft.world.entity.animal.Cow target(GameTestHelper h) {
        var target = h.spawn(EntityType.COW, new BlockPos(5, 2, 3)); target.setNoAi(true); target.setNoGravity(true);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000000); target.setHealth(1000000); return target;
    }
}
