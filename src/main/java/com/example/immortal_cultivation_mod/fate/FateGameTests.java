package com.example.immortal_cultivation_mod.fate;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.entity.ModEntities;
import com.example.immortal_cultivation_mod.item.ForgingSystem;
import com.example.immortal_cultivation_mod.item.UtilityTalismanItem;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Registry and saved-state integration tests, run only by Minecraft's GameTest runner. */
@GameTestHolder(ImmortalCultivationMod.MODID)
@PrefixGameTestTemplate(false)
public final class FateGameTests {
    @GameTest(template = "fate_test")
    public static void harvestedPlantsAndOreMaterialsRemainUnidentified(GameTestHelper h) {
        var items=java.util.List.of(
                com.example.immortal_cultivation_mod.block.ModBlocks.QINGLING_GRASS.get().asItem(),
                com.example.immortal_cultivation_mod.block.ModBlocks.YUNWEN_SILVER_CRYSTAL.get().asItem(),
                net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                        net.minecraft.resources.ResourceLocation.parse("immortal_cultivation_mod:spirit_storage_crystal")));
        var drops=new java.util.ArrayList<net.minecraft.world.entity.item.ItemEntity>();
        for(var item:items){
            var stack=new net.minecraft.world.item.ItemStack(item);
            h.assertTrue(ForgingSystem.isPurityMaterial(stack),"Test drop is a purity-bearing plant or mineral");
            drops.add(new net.minecraft.world.entity.item.ItemEntity(h.getLevel(),0,0,0,stack));
        }
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.level.BlockDropsEvent(
                h.getLevel(),h.absolutePos(new BlockPos(2,2,2)),
                com.example.immortal_cultivation_mod.magicritual.block.ModBlocks.SPIRIT_ORE.get().defaultBlockState(),
                null,drops,null,net.minecraft.world.item.ItemStack.EMPTY));
        for(var drop:drops)h.assertTrue(!ForgingSystem.hasDeterminedPurity(drop.getItem()),"Block-drop pipeline must not assign material purity");
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void mobDropLuckWaitsForIdentificationAndAppliesOnce(GameTestHelper h) {
        var player=debugPlayer(h,false);
        LuckSystem.change(player,100-LuckSystem.luck(player));
        var stack=new net.minecraft.world.item.ItemStack(com.example.immortal_cultivation_mod.item.ModItems.HUO_XI_BLOOD.get());
        var drop=new net.minecraft.world.entity.item.ItemEntity(h.getLevel(),0,0,0,stack);
        var mob=new net.minecraft.world.entity.monster.Zombie(net.minecraft.world.entity.EntityType.ZOMBIE,h.getLevel());
        var event=new net.neoforged.neoforge.event.entity.living.LivingDropsEvent(mob,
                mob.damageSources().playerAttack(player),java.util.List.of(drop),true);
        player.getRandom().setSeed(12345);
        FateCrafting.drops(event);FateCrafting.drops(event);
        h.assertTrue(!ForgingSystem.hasDeterminedPurity(stack),"Fresh mob material drops remain unknown even with luck");
        h.assertTrue(player.getRandom().nextInt()==net.minecraft.util.RandomSource.create(12345).nextInt(),"Dropping unidentified material consumes no purity roll");
        ForgingSystem.ensurePurity(stack,123);
        int expected=Math.clamp(1+Math.floorMod(123*31+17,100)+5,1,100);
        h.assertTrue(ForgingSystem.purity(stack)==expected,"Saved killer luck modifies purity at identification exactly once");
        ForgingSystem.ensurePurity(stack,456);FateCrafting.drops(event);
        h.assertTrue(ForgingSystem.purity(stack)==expected&&!FateCrafting.tags(stack).contains(ForgingSystem.TAG_PENDING_PURITY_LUCK),"Known purity remains stable; deferred bonus consumed");
        player.discard();mob.discard();h.succeed();
    }
    private static ServerPlayer debugPlayer(GameTestHelper helper, boolean operator) {
        ServerPlayer player = new net.neoforged.neoforge.common.util.FakePlayer(helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "debug-eye-test")) {
            @Override public boolean hasPermissions(int level) { return operator && level <= 2; }
        };
        BlockPos pos = helper.absolutePos(new BlockPos(4, 2, 3));
        player.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
        return player;
    }
    @GameTest(template = "fate_test")
    public static void diagnosticsPreserveStateAndRandomness(GameTestHelper helper) {
        var player = debugPlayer(helper, true);
        LuckSystem.change(player, 100 - LuckSystem.luck(player));
        player.getPersistentData().putBoolean("ImmortalCultivationDebugEyeEnabled", true);
        var before = player.getPersistentData().copy();
        var cultivation = ModAttachments.getData(player);
        player.getRandom().setSeed(8492);
        var expected = net.minecraft.util.RandomSource.create(8492);
        var snapshot = DebugEyeDiagnostics.snapshot(player);
        helper.assertTrue(before.equals(player.getPersistentData()), "Snapshot does not mutate saved state");
        helper.assertTrue(cultivation.equals(ModAttachments.getData(player)), "Snapshot does not alter cultivation stats");
        helper.assertTrue(player.getRandom().nextDouble() == expected.nextDouble(), "Snapshot consumes no random draw");
        helper.assertTrue(DebugEyeDiagnostics.roll(player, "opportunity",
                LuckRules.opportunityChance(50), LuckRules.opportunityChance(100), .14), "Same draw succeeds only with Luck");
        var logged = DebugEyeDiagnostics.snapshot(player);
        helper.assertTrue(logged.events().size() == snapshot.events().size() + 2, "Actual roll and aggregate counters are visible");
        for (int i = 0; i < 100; i++) DebugEyeDiagnostics.note(player, "action", "test", i);
        helper.assertTrue(DebugEyeDiagnostics.snapshot(player).events().size() == 66, "Only latest 64 events plus counter and hint retained");
        var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            com.example.immortal_cultivation_mod.network.DebugEyePayloads.Snapshot.STREAM_CODEC.encode(buffer, logged);
            var decoded = com.example.immortal_cultivation_mod.network.DebugEyePayloads.Snapshot.STREAM_CODEC.decode(buffer);
            helper.assertTrue(decoded.tick() == logged.tick(), "Server timestamp survives packet encoding");
            for (int i = 0; i < logged.luck().size(); i++) helper.assertTrue(
                    logged.luck().get(i).getString().equals(decoded.luck().get(i).getString()),
                    "Luck row " + i + ": expected [" + logged.luck().get(i).getString() + "] received [" + decoded.luck().get(i).getString() + "]");
            // The component codec may simplify literal nodes; compare what the player actually reads.
            helper.assertTrue(decoded.luck().stream().map(net.minecraft.network.chat.Component::getString).toList()
                    .equals(logged.luck().stream().map(net.minecraft.network.chat.Component::getString).toList()), "Luck page survives packet encoding");
            helper.assertTrue(decoded.events().stream().map(net.minecraft.network.chat.Component::getString).toList()
                    .equals(logged.events().stream().map(net.minecraft.network.chat.Component::getString).toList()), "Roll history survives packet encoding");
        } finally { buffer.release(); }
        player.discard(); helper.succeed();
    }
    @GameTest(template = "fate_test")
    public static void diagnosticsRequirePermissionAndEye(GameTestHelper helper) {
        var unauthorized = debugPlayer(helper, false);
        unauthorized.getPersistentData().putBoolean("ImmortalCultivationDebugEyeEnabled", true);
        helper.assertTrue(!com.example.immortal_cultivation_mod.network.ModPayloads.isDebugEyeEnabled(unauthorized), "Client cannot bypass operator gate");
        DebugEyeDiagnostics.note(unauthorized, "action", "denied", "must not record");
        helper.assertTrue(DebugEyeDiagnostics.snapshot(unauthorized).events().size() == 1, "Unauthorized debug flag collects nothing");
        var operator = debugPlayer(helper, true);
        DebugEyeDiagnostics.roll(operator, "loot", .05, .08, .06);
        helper.assertTrue(DebugEyeDiagnostics.snapshot(operator).events().size() == 1, "Disabled eye collects nothing");
        operator.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        var reason = (net.minecraft.network.chat.contents.TranslatableContents)DebugEyeDiagnostics.explorationGate(operator).getContents();
        helper.assertTrue(reason.getKey().endsWith("gate.creative"), "Creative mode explicitly explains blocked exploration");
        operator.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        LuckSystem.data(operator).putLong("LastExplore", operator.blockPosition().asLong());
        reason = (net.minecraft.network.chat.contents.TranslatableContents)DebugEyeDiagnostics.explorationGate(operator).getContents();
        helper.assertTrue(reason.getKey().endsWith("gate.movement"), "Stationary players see the movement gate");
        unauthorized.discard(); operator.discard(); helper.succeed();
    }
    private static ServerPlayer player(GameTestHelper helper, int fortune) {
        ServerPlayer player = new net.neoforged.neoforge.common.util.FakePlayer(helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "fortune-test"));
        BlockPos pos = helper.absolutePos(new BlockPos(4, 2, 3));
        player.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
        player.setYRot(0); player.setXRot(0);
        ModAttachments.setData(player, ModAttachments.getData(player)
                .withCultivationLevel(CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_LATE).withQi(5000));
        LuckSystem.change(player, fortune - LuckSystem.luck(player));
        return player;
    }
    @GameTest(template = "fate_test")
    public static void dataAndRecipes(GameTestHelper helper) {
        helper.assertTrue(ModAttachments.CultivationData.createDefault().withLuck(120).luck() == 100, "Luck upper bound");
        helper.assertTrue(ModAttachments.CultivationData.createDefault().withLuck(-1).luck() == 0, "Luck lower bound");
        helper.assertTrue(ForgingSystem.recipeById("tribulation_doll").ingredientSlotCount() == 15, "Doll fits the bronze forge");
        helper.assertTrue(ForgingSystem.recipeById("purified_tribulation_doll").ingredientSlotCount() == 15, "Purified doll fits the bronze forge");
        helper.assertTrue(FateLootCondition.TYPE.get() != null, "Rare loot condition registered");
        String[] ids = {"fortune_mirror", "precise_fortune_mirror", "fortune_guard_coin", "fortune_lamp"};
        int[] slots = {15, 15, 15, 15};
        for (int i = 0; i < ids.length; i++) {
            var id = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, ids[i]);
            var slip = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, ids[i] + "_yujian"));
            var recipe = ForgingSystem.recipe(new net.minecraft.world.item.ItemStack(slip));
            helper.assertTrue(recipe != null && recipe.directOutput() && recipe.ingredientSlotCount() == slots[i], "Expensive forge slip: " + ids[i]);
            helper.assertTrue(ForgingSystem.resultItem(recipe).asItem() == net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id), "Correct artifact output");
            helper.assertTrue(recipe.spiritStoneCount() == 64
                            && ForgingSystem.canUseFire(recipe, "normal_fire")
                            && ForgingSystem.canUseFire(recipe, "ling_fire")
                            && ForgingSystem.canUseFire(recipe, "zhen_fire"),
                    "Spirit stone cost remains while every fire is accepted");
            helper.assertTrue(helper.getLevel().getRecipeManager().byKey(id).isEmpty(), "No crafting-table artifact recipe");
        }
        for (String tier : java.util.List.of("low", "middle", "high")) {
            var id = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, tier + "_qi_alchemy_formula");
            helper.assertTrue(helper.getLevel().getRecipeManager().byKey(id).isEmpty(), "Pill formulas cannot be crafted");
        }
        helper.succeed();
    }
    @GameTest(template = "fate_test")
    public static void theftConservesFortune(GameTestHelper helper) {
        ServerPlayer caster = player(helper, 54);
        var target = helper.spawn(ModEntities.ZHUJI_CULTIVATOR.get(), new BlockPos(4, 2, 7));
        target.setNoAi(true);
        LuckSystem.change(target, 82 - LuckSystem.luck(target));
        helper.assertTrue(FateTechniques.start(caster, FateTechniques.THEFT), "Valid theft begins");
        helper.assertTrue(ModAttachments.getData(caster).qi() == 4100, "900 Qi reserved");
        FateTechniques.tick(caster);
        helper.assertTrue(LuckSystem.luck(target) == 82, "No transfer before channel completes");
        LuckSystem.data(caster).getCompound("Channel").putLong("Start", helper.getLevel().getGameTime() - 120);
        FateTechniques.tick(caster);
        helper.assertTrue(LuckSystem.luck(target) == 71 && LuckSystem.luck(caster) == 65, "Exact 11-point transfer");
        helper.assertTrue(ModAttachments.getData(caster).qi() == 3200, "Exactly 1800 Qi paid");
        helper.assertTrue(LuckSystem.karma(caster) == 58, "Base Karma plus favored-target price");
        helper.assertTrue(LuckSystem.scarred(target), "Victim is protected by a seven-day scar");
        helper.assertTrue(!FateTechniques.start(caster, FateTechniques.THEFT), "Cannot bypass cooldown");
        caster.discard(); helper.succeed();
    }
    @GameTest(template = "fate_test")
    public static void interruptedTheft(GameTestHelper helper) {
        ServerPlayer caster = player(helper, 54);
        var target = helper.spawn(ModEntities.ZHUJI_CULTIVATOR.get(), new BlockPos(4, 2, 7));
        target.setNoAi(true);
        LuckSystem.change(target, 82 - LuckSystem.luck(target));
        helper.assertTrue(FateTechniques.start(caster, FateTechniques.THEFT), "Channel begins");
        target.setPos(target.getX(), target.getY(), target.getZ() + 20);
        FateTechniques.tick(caster);
        helper.assertTrue(LuckSystem.luck(target) == 82 && LuckSystem.luck(caster) == 54, "Interrupted theft changes no Luck");
        helper.assertTrue(ModAttachments.getData(caster).qi() == 4100, "Interruption costs 900 Qi");
        helper.assertTrue(LuckSystem.karma(caster) == 0 && !LuckSystem.scarred(target), "No Karma or scar on failure");
        helper.assertTrue(LuckSystem.data(caster).getLong("Cooldown_" + FateTechniques.THEFT)
                == helper.getLevel().getGameTime() + 1200, "Failure cooldown is 60 seconds");
        caster.discard(); helper.succeed();
    }
    @GameTest(template = "fate_test")
    public static void avoidanceCostsQiOnly(GameTestHelper helper) {
        ServerPlayer caster = player(helper, 50);
        helper.assertTrue(FateTechniques.start(caster, FateTechniques.AVOID), "Avoidance begins");
        helper.assertTrue(ModAttachments.getData(caster).qi() == 4500, "500 Qi paid");
        FateTechniques.tick(caster);
        helper.assertTrue(LuckSystem.data(caster).getLong("AvoidUntil") == 0, "No protection before channel completes");
        LuckSystem.data(caster).getCompound("Channel").putLong("Start", helper.getLevel().getGameTime() - 30);
        FateTechniques.tick(caster);
        helper.assertTrue(LuckSystem.data(caster).getLong("AvoidUntil") == helper.getLevel().getGameTime() + 2400,
                "Protection lasts 120 seconds");
        helper.assertTrue(LuckSystem.luck(caster) == 50 && LuckSystem.karma(caster) == 0, "No Luck or Karma payment");
        helper.assertTrue(!FateTechniques.start(caster, FateTechniques.AVOID), "Cooldown prevents recasting");
        caster.discard(); helper.succeed();
    }
    @GameTest(template = "fate_test")
    public static void rescueRequiresOnlyStrictLuckAdvantage(GameTestHelper helper) {
        var victim = helper.spawn(ModEntities.ZHUJI_CULTIVATOR.get(), new BlockPos(8, 2, 8));
        var attacker = helper.spawn(ModEntities.ZHUJI_CULTIVATOR.get(), new BlockPos(8, 2, 10));
        victim.setNoAi(true); attacker.setNoAi(true);
        LuckSystem.change(attacker, 50 - LuckSystem.luck(attacker));
        LuckSystem.change(victim, 50 - LuckSystem.luck(victim));
        var rejected = new net.neoforged.neoforge.event.entity.living.LivingDeathEvent(victim,
                helper.getLevel().damageSources().mobAttack(attacker));
        LuckSystem.miracle(rejected);
        helper.assertTrue(!rejected.isCanceled() && LuckSystem.luck(victim) == 50, "Equal Luck cannot rescue");
        LuckSystem.change(victim, 1);
        var rescued = new net.neoforged.neoforge.event.entity.living.LivingDeathEvent(victim,
                helper.getLevel().damageSources().mobAttack(attacker));
        LuckSystem.miracle(rescued);
        helper.assertTrue(rescued.isCanceled(), "One-point advantage guarantees rescue");
        helper.assertTrue(LuckSystem.luck(victim) >= 36 && LuckSystem.luck(victim) <= 41, "Rescue consumes 10–15 Luck");
        helper.assertTrue(Math.abs(victim.getHealth() / victim.getMaxHealth() - .125F) < .001F, "Rescue restores 12.5 percent health");
        var spentAdvantage = new net.neoforged.neoforge.event.entity.living.LivingDeathEvent(victim,
                helper.getLevel().damageSources().mobAttack(attacker));
        LuckSystem.miracle(spentAdvantage);
        helper.assertTrue(!spentAdvantage.isCanceled(), "Later attacks compare current Luck after payment");
        LuckSystem.change(attacker, -LuckSystem.luck(attacker));
        LuckSystem.change(victim, 1 - LuckSystem.luck(victim));
        LuckSystem.data(victim).putLong("RescueUntil", helper.getLevel().getGameTime() + 2400);
        var lowLuck = new net.neoforged.neoforge.event.entity.living.LivingDeathEvent(victim,
                helper.getLevel().damageSources().mobAttack(attacker));
        LuckSystem.miracle(lowLuck);
        helper.assertTrue(lowLuck.isCanceled() && LuckSystem.luck(victim) == 0, "Luck 1 beats Luck 0 despite old saved cooldown and insufficient Luck for the full fee");
        helper.assertTrue(!LuckSystem.data(victim).contains("RescueUntil"), "Old cooldown is cleared on rescue");
        victim.discard(); attacker.discard();
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void rescueRepeatsWithoutLandingOrCooldown(GameTestHelper helper) {
        var victim = helper.spawn(net.minecraft.world.entity.EntityType.ZOMBIE, new BlockPos(8, 2, 8));
        var attacker = helper.spawn(ModEntities.ZHUJI_CULTIVATOR.get(), new BlockPos(8, 2, 10));
        victim.setNoAi(true); attacker.setNoAi(true);
        victim.setPos(victim.getX(), victim.getY() + 64, victim.getZ());
        var origin = victim.position();
        LuckSystem.change(victim, 100 - LuckSystem.luck(victim));
        LuckSystem.change(attacker, -LuckSystem.luck(attacker));
        helper.assertTrue(LuckSystem.safeEscape(victim, attacker.position()) == null, "Test arena has no safe landing near the victim");
        for (int i = 0; i < 3; i++) {
            int before = LuckSystem.luck(victim);
            victim.setHealth(0);
            var event = new net.neoforged.neoforge.event.entity.living.LivingDeathEvent(victim,
                    helper.getLevel().damageSources().mobAttack(attacker));
            LuckSystem.miracle(event);
            helper.assertTrue(event.isCanceled() && victim.isAlive(), "Every lethal hit is rescued while Luck remains higher");
            helper.assertTrue(victim.position().equals(origin), "Missing landing restores health without unsafe teleportation");
            helper.assertTrue(before - LuckSystem.luck(victim) >= 10 && before - LuckSystem.luck(victim) <= 15,
                    "Each rescue pays its own cost");
            helper.assertTrue(Math.abs(victim.getHealth() / victim.getMaxHealth() - .125F) < .001F, "Repeated rescue restores health");
        }
        int before = LuckSystem.luck(victim);
        var protectedElsewhere = new net.neoforged.neoforge.event.entity.living.LivingDeathEvent(victim,
                helper.getLevel().damageSources().mobAttack(attacker));
        protectedElsewhere.setCanceled(true);
        LuckSystem.miracle(protectedElsewhere);
        helper.assertTrue(before == LuckSystem.luck(victim), "Earlier protection does not also consume Luck");
        victim.discard(); attacker.discard(); helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void ordinaryMobProjectilesRescueButEnvironmentalDeathsDoNot(GameTestHelper helper) {
        var victim = player(helper, 100);
        var attacker = helper.spawn(net.minecraft.world.entity.EntityType.SKELETON, new BlockPos(8, 2, 10));
        attacker.setNoAi(true);
        LuckSystem.change(attacker, -LuckSystem.luck(attacker));
        var arrow = net.minecraft.world.entity.EntityType.ARROW.create(helper.getLevel());
        arrow.setOwner(attacker);
        victim.setHealth(0);
        var event = new net.neoforged.neoforge.event.entity.living.LivingDeathEvent(victim,
                helper.getLevel().damageSources().arrow(arrow, attacker));
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);
        helper.assertTrue(event.isCanceled() && victim.isAlive(), "Registered rescue handler uses an ordinary mob projectile's owner");
        for (var environmental : java.util.List.of(helper.getLevel().damageSources().fellOutOfWorld(),
                helper.getLevel().damageSources().fall(), helper.getLevel().damageSources().lava(),
                helper.getLevel().damageSources().genericKill())) {
            int before = LuckSystem.luck(victim);
            var death = new net.neoforged.neoforge.event.entity.living.LivingDeathEvent(victim, environmental);
            LuckSystem.miracle(death);
            helper.assertTrue(!death.isCanceled() && before == LuckSystem.luck(victim), "Environmental/admin deaths do not trigger combat rescue");
        }
        victim.discard(); attacker.discard(); arrow.discard(); helper.succeed();
    }
    @GameTest(template = "fate_test")
    public static void mirrorsCannotEquipAndLegacyStacksAreReturned(GameTestHelper helper) {
        var player = player(helper, 50);
        var curios = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).orElseThrow();
        var charm = curios.getCurios().get("charm");
        helper.assertTrue(charm != null && charm.getStacks().getSlots() >= 1, "Player has a charm slot");
        var context = new top.theillusivec4.curios.api.SlotContext("charm", player, 0, false, true);
        var tag = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("curios", "charm"));
        for (var item : java.util.List.of(com.example.immortal_cultivation_mod.item.ModItems.FORTUNE_MIRROR.get(),
                com.example.immortal_cultivation_mod.item.ModItems.PRECISE_FORTUNE_MIRROR.get())) {
            var stack = new net.minecraft.world.item.ItemStack(item);
            stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                    net.minecraft.network.chat.Component.literal("Saved mirror"));
            var behavior = (com.example.immortal_cultivation_mod.item.FortuneArtifactItem)item;
            helper.assertTrue(!stack.is(tag) && !behavior.canEquip(context, stack), "Mirrors are excluded by both charm tags and equipment validation");
            player.getInventory().clearContent();
            charm.getStacks().setStackInSlot(0, stack.copy());
            behavior.curioTick(context, stack);
            behavior.curioTick(context, stack);
            helper.assertTrue(charm.getStacks().getStackInSlot(0).isEmpty(), "Legacy mirror is removed from equipment");
            int returned = player.getInventory().items.stream()
                    .filter(found -> net.minecraft.world.item.ItemStack.isSameItemSameComponents(found, stack))
                    .mapToInt(net.minecraft.world.item.ItemStack::getCount).sum();
            helper.assertTrue(returned == 1, "Exactly one mirror returns with its saved components; repeated ticks do not duplicate it");
        }
        var coin = new net.minecraft.world.item.ItemStack(com.example.immortal_cultivation_mod.item.ModItems.FORTUNE_GUARD_COIN.get());
        helper.assertTrue(coin.is(tag) && ((com.example.immortal_cultivation_mod.item.FortuneArtifactItem)coin.getItem()).canEquip(context, coin),
                "The fortune guard coin remains equippable");
        player.discard(); helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void temporaryLuckExpires(GameTestHelper helper) {
        ServerPlayer player = player(helper, 60);
        CompoundTag ward = new CompoundTag(); ward.putInt("Bonus", 10); ward.putLong("Expires", 0);
        ListTag wards = new ListTag(); wards.add(ward);
        player.getPersistentData().put(ImmortalCultivationMod.MODID + ":five_mountains_luck", wards);
        UtilityTalismanItem.tickPlayer(player);
        helper.assertTrue(LuckSystem.luck(player) == 45, "Ten temporary points end and five-point penalty remains");
        UtilityTalismanItem.tickPlayer(player);
        helper.assertTrue(LuckSystem.luck(player) == 45, "Expiry is applied exactly once");
        player.discard(); helper.succeed();
    }
}
