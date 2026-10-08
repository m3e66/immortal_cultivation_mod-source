package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.alchemy.AlchemyChemistry;
import com.example.immortal_cultivation_mod.alchemy.AlchemyEffects;
import com.example.immortal_cultivation_mod.alchemy.PillData;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import java.util.Map;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ImmortalCultivationMod.MODID)
@PrefixGameTestTemplate(false)
public final class NpcBattleReferenceGameTests {
    private static ItemStack healingPills(double amount, int ticks) {
        return PillData.create(new AlchemyChemistry.Batch("huichun", 2, Map.of(),
                Map.of(AlchemyChemistry.Effect.HEALTH, new AlchemyChemistry.Dose(amount, ticks)),
                90, 0, "common", false));
    }

    private static void givePills(LianqiCultivatorEntity npc, ItemStack stack) {
        var inventory = NpcProfile.inventory(npc);
        inventory.set(0, stack);
        NpcProfile.writeInventory(npc, NpcProfile.cultivationProfile(npc), inventory);
        npc.setHealth(npc.getMaxHealth() * .1F);
    }

    @GameTest(template = "npc_combat_arena")
    public static void realExternalQiIsDetectedAndDepartingOrOwnedQiIsIgnored(GameTestHelper h) {
        var defender = NpcCombatGameTests.npc(h);
        defender.setPos(h.absoluteVec(new Vec3(12.5, 2, 12.5)));
        defender.setNoAi(true);
        h.getLevel().addFreshEntity(defender);
        var enemy = EntityType.ZOMBIE.create(h.getLevel());
        enemy.setNoAi(true);
        enemy.setPos(defender.position().add(6, 0, 0));
        h.getLevel().addFreshEntity(enemy);
        h.assertTrue(WeaponQiSlashEntity.releaseNpc(enemy, defender, 24, 10, .5, "metal", 0, false),
                "Fixture releases the real non-Projectile external Qi entity");
        h.assertTrue(NpcCombatReactions.projectile(defender) instanceof WeaponQiSlashEntity,
                "A real incoming Qi blade is recognized before contact");
        for (var slash : h.getLevel().getEntitiesOfClass(WeaponQiSlashEntity.class, defender.getBoundingBox().inflate(12))) {
            slash.forcefieldBounce(slash.forcefieldMovement().reverse());
        }
        h.assertTrue(NpcCombatReactions.projectile(defender) == null, "Departing Qi does not trigger defense");
        defender.setTarget(enemy);
        h.assertTrue(WeaponQiSlashEntity.releaseNpc(defender, enemy, 24, 10, .5, "metal", 0, false),
                "Defender releases its own Qi");
        for (var slash : h.getLevel().getEntitiesOfClass(WeaponQiSlashEntity.class, defender.getBoundingBox().inflate(12))) {
            if (slash.forcefieldOwner() == defender) slash.forcefieldBounce(slash.forcefieldMovement().reverse());
        }
        h.assertTrue(NpcCombatReactions.projectile(defender) == null, "Owned Qi stays excluded even when reflected toward caster");
        defender.discard(); enemy.discard(); h.succeed();
    }

    @GameTest(template = "npc_combat_arena")
    public static void urgentProjectileInterruptsNavigationWithoutRepeatingDodge(GameTestHelper h) {
        var defender = NpcCombatGameTests.npc(h);
        defender.setPos(h.absoluteVec(new Vec3(12.5, 2, 12.5)));
        defender.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        var enemy = EntityType.ZOMBIE.create(h.getLevel());
        enemy.setNoAi(true); enemy.setPos(defender.position().add(7, 0, 0));
        h.getLevel().addFreshEntity(enemy); defender.setTarget(enemy);
        var brain = new NpcCombatBrain(defender);
        brain.tick();
        h.assertTrue(defender.getNavigation().isInProgress(), "Ordinary pursuit has already begun");
        var arrow = EntityType.ARROW.create(h.getLevel());
        arrow.setOwner(enemy); arrow.setNoGravity(true);
        arrow.setPos(defender.position().add(4, 1, 0)); arrow.setDeltaMovement(-1, 0, 0);
        h.getLevel().addFreshEntity(arrow);
        brain.tick();
        var state = defender.getPersistentData().getCompound("ImmortalCultivationNpcCombatState");
        h.assertTrue("react_to_threat".equals(state.getString("Intent")) && Math.abs(defender.getDeltaMovement().z) > .2,
                "Urgent defense preempts navigation and its pending decision delay");
        long ready = state.getLong("Reaction"); Vec3 motion = defender.getDeltaMovement();
        brain.tick();
        h.assertTrue(ready == state.getLong("Reaction") && motion.equals(defender.getDeltaMovement()),
                "Committed dodge does not restart each tick");
        arrow.discard(); enemy.discard(); h.succeed();
    }

    @GameTest(template = "npc_combat_arena", timeoutTicks = 60)
    public static void healingPillPreparesThenConsumesExactBatchOnce(GameTestHelper h) {
        var npc = NpcCombatGameTests.npc(h);
        npc.setPos(h.absoluteVec(new Vec3(12.5, 2, 12.5)));
        givePills(npc, healingPills(7, 0));
        float before = npc.getHealth(); var brain = new NpcCombatBrain(npc);
        brain.tick();
        h.assertTrue(npc.getHealth() == before && NpcProfile.inventory(npc).get(0).getCount() == 2,
                "Beginning preparation gives neither free healing nor premature item consumption");
        h.runAfterDelay(13, () -> {
            brain.tick();
            h.assertTrue(Math.abs(npc.getHealth() - before - npc.getMaxHealth()*.07) < .001 && NpcProfile.inventory(npc).get(0).getCount() == 1,
                    "Release uses the saved pill's exact rolled health dose and consumes one item");
            brain.tick();
            h.assertTrue(Math.abs(npc.getHealth() - before - npc.getMaxHealth()*.07) < .001 && NpcProfile.inventory(npc).get(0).getCount() == 1,
                    "Repeated ticks cannot apply the completed action twice");
            h.succeed();
        });
    }

    @GameTest(template = "npc_combat_arena", timeoutTicks = 60)
    public static void damageAndSealingCancelHealingWithoutSpendingPill(GameTestHelper h) {
        var npc = NpcCombatGameTests.npc(h);
        npc.setPos(h.absoluteVec(new Vec3(12.5, 2, 12.5)));
        givePills(npc, healingPills(9, 0));
        float before = npc.getHealth(); var brain = new NpcCombatBrain(npc);
        brain.tick(); brain.recordDamage(npc.damageSources().generic(), 1);
        npc.addEffect(new MobEffectInstance(ModEffects.SPIRIT_SEAL, 40));
        h.runAfterDelay(13, () -> {
            brain.tick();
            h.assertTrue(npc.getHealth() == before && NpcProfile.inventory(npc).get(0).getCount() == 2,
                    "Interrupted preparation has no delayed healing or item loss");
            h.assertTrue(!NpcCombatResources.available(npc, NpcCombatResources.Purpose.HEALTH, null),
                    "A sealed NPC cannot restart pill preparation");
            h.succeed();
        });
    }

    @GameTest(template = "npc_combat_arena", timeoutTicks = 60)
    public static void sustainedNpcPillUsesSameDoseClockAsPlayer(GameTestHelper h) {
        var npc = NpcCombatGameTests.npc(h);
        npc.setPos(h.absoluteVec(new Vec3(12.5, 2, 12.5))); givePills(npc, healingPills(8, 4));
        var brain = new NpcCombatBrain(npc); float before = npc.getHealth(); brain.tick();
        h.startSequence().thenIdle(13).thenExecute(() -> {
            brain.tick();
            h.assertTrue(npc.getHealth() == before && AlchemyEffects.hasActive(npc, AlchemyChemistry.Effect.HEALTH),
                    "Timed pill starts its real dose without immediate healing");
        }).thenExecuteAfter(1, brain::tick).thenExecuteAfter(1, brain::tick)
                .thenExecuteAfter(1, brain::tick).thenExecuteAfter(1, brain::tick).thenExecute(() -> {
                h.assertTrue(Math.abs(npc.getHealth() - before - npc.getMaxHealth()*.08) < .001,
                        "Saved dose heals its total amount over the specified duration");
                h.assertTrue(!AlchemyEffects.hasActive(npc, AlchemyChemistry.Effect.HEALTH), "Dose expires normally");
        }).thenSucceed();
    }

    @GameTest(template = "fate_test")
    public static void spellValueAccountsForVisibleResistanceRangeAndPreparation(GameTestHelper h) {
        var enemy = EntityType.ZOMBIE.create(h.getLevel());
        double ordinary = NpcCombatBrain.tacticalSpellScore(enemy, ModSpells.FIREBALL, 4, false);
        double vulnerableWindup = NpcCombatBrain.tacticalSpellScore(enemy, ModSpells.FIREBALL, 4, true);
        double distant = NpcCombatBrain.tacticalSpellScore(enemy, ModSpells.FIREBALL, 64, false);
        enemy.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 100));
        double resisted = NpcCombatBrain.tacticalSpellScore(enemy, ModSpells.FIREBALL, 4, false);
        h.assertTrue(resisted < ordinary - 100 && vulnerableWindup < ordinary && distant < ordinary,
                "Visible fire defense, exposed preparation and poor range lower attack value");
        h.succeed();
    }

    @GameTest(template = "npc_combat_arena")
    public static void beastUsesOwnDefenseAndCannotStackMeleeDuringIt(GameTestHelper h) {
        var beast = ModEntities.INK_ARMORED_STONE_DEVOURING_TORTOISE.get().create(h.getLevel());
        beast.setPos(h.absoluteVec(new Vec3(12.5, 2, 12.5)));
        var enemy = EntityType.ZOMBIE.create(h.getLevel());
        enemy.setNoAi(true); enemy.setPos(beast.position().add(6, 0, 0)); h.getLevel().addFreshEntity(enemy);
        var arrow = EntityType.ARROW.create(h.getLevel());
        arrow.setOwner(enemy); arrow.setNoGravity(true);
        arrow.setPos(beast.position().add(4, 1, 0)); arrow.setDeltaMovement(-1, 0, 0); h.getLevel().addFreshEntity(arrow);
        h.assertTrue(beast.reactToIncomingAttack() && beast.action() == CultivationBestiaryBeastEntity.ACTION_QUATERNARY,
                "Shared trajectory scan selects tortoise's existing shell defense");
        h.assertTrue(!beast.doHurtTarget(enemy) && !beast.reactToIncomingAttack(),
                "Active defense cannot overlap a melee hit or another reaction");
        arrow.discard(); enemy.discard(); h.succeed();
    }

    @GameTest(template = "npc_combat_arena")
    public static void boxedBeastCannotEvadeWithoutDestination(GameTestHelper h) {
        var beast = ModEntities.GREEN_PATTERNED_SPIRIT_DEVOURING_RAT.get().create(h.getLevel());
        beast.setPos(h.absoluteVec(new Vec3(12.5, 2, 12.5)));
        for (int x = 11; x <= 13; x++) for (int z = 11; z <= 13; z++) {
            h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            if (x != 12 || z != 12) for (int y = 2; y <= 4; y++)
                h.setBlock(new BlockPos(x, y, z), Blocks.STONE);
        }
        var enemy = EntityType.ZOMBIE.create(h.getLevel()); enemy.setPos(beast.position().add(4, 0, 0));
        var arrow = EntityType.ARROW.create(h.getLevel()); arrow.setOwner(enemy); arrow.setPos(enemy.position());
        float before = beast.getHealth();
        h.assertTrue(beast.hurt(beast.damageSources().arrow(arrow, enemy), 5) && beast.getHealth() < before,
                "A failed shadow-dodge route leaves the rat vulnerable rather than canceling damage");
        h.succeed();
    }
}
