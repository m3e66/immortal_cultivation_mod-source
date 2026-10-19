package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.alchemy.AlchemyChemistry;
import com.example.immortal_cultivation_mod.alchemy.AlchemyForgeRecipes;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.block.DingBlockEntity;
import com.example.immortal_cultivation_mod.block.ModBlocks;
import com.example.immortal_cultivation_mod.screen.DingMenu;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_mod")
@PrefixGameTestTemplate(false)
public final class ForgingTemperatureGameTests {
    private static final BlockPos FORGE_POS = new BlockPos(3, 2, 3);

    @GameTest(template = "fate_test")
    public static void nativeWeaponRecipesAndSlipsUseOriginalRealmHeat(GameTestHelper helper) {
        for (HigherWeaponSpec spec : HigherWeaponSpec.values()) {
            assertNativeRecipe(helper, spec.id, spec == HigherWeaponSpec.SEVEN_STAR_SWORD ? 6500 : 4500);
        }
        helper.assertTrue(ForgingSystem.canUseFire(ForgingSystem.recipeById("shihunfan"), "ling_fire")
                        && ForgingSystem.canUseFire(ForgingSystem.recipeById("shihunfan"), "zhen_fire"),
                "True fire can service older spirit-fire recipes without replacing spirit fire");
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void mortalWeaponsPillsAndAccessoriesKeepTheirRecipeHeat(GameTestHelper helper) {
        var temperatures = Map.ofEntries(
                Map.entry("dao", 3500), Map.entry("flying_sword", 3500),
                Map.entry("jingtie_spear", 3500), Map.entry("heavy_iron_bracer", 3500),
                Map.entry("throwing_knife", 3500), Map.entry("flying_needle", 3500),
                Map.entry("tiegu_shan", 3500), Map.entry("fuchen", 2200),
                Map.entry("whip", 2400), Map.entry("shihunfan", 3200),
                Map.entry("zhenpan", 1800), Map.entry("fortune_mirror", 4800),
                Map.entry("precise_fortune_mirror", 6400), Map.entry("fortune_guard_coin", 5200),
                Map.entry("fortune_lamp", 6000), Map.entry("tribulation_doll", 4200),
                Map.entry("purified_tribulation_doll", 5200), Map.entry("jade_pill_bottle", 1200));
        temperatures.forEach((id, temperature) -> {
            var recipe = ForgingSystem.recipeById(id);
            helper.assertTrue(recipe != null && recipe.averageTemp() == temperature
                            && ForgingSystem.targetTemperature(recipe, List.of()) == temperature,
                    "Existing recipe heat remains unchanged: " + id);
        });
        for (String id : List.of("dao", "flying_sword", "jingtie_spear", "heavy_iron_bracer",
                "throwing_knife", "flying_needle", "tiegu_shan", "fuchen", "whip")) {
            helper.assertTrue(ForgingSystem.canUseFire(ForgingSystem.recipeById(id), "normal_fire"),
                    "Ordinary fire still supports the existing Qi Refining weapon recipe: " + id);
        }
        for (String id : AlchemyForgeRecipes.LEGACY_REQUIRED.keySet()) {
            helper.assertTrue(ForgingSystem.recipeById("legacy_alchemy/" + id).averageTemp() == 1200,
                    "Legacy pill heat remains 1200: " + id);
        }
        for (var formula : AlchemyChemistry.FORMULAS) {
            helper.assertTrue(ForgingSystem.recipeById("alchemy/" + formula.id()).averageTemp() == 1200,
                    "Current pill heat remains 1200: " + formula.id());
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void actualMoldsRewardRealmHeatAndPenalizeMortalHeat(GameTestHelper helper) {
        for (String id : List.of("black_iron_jian", "seven_star_sword")) {
            int temperature = "seven_star_sword".equals(id) ? 6500 : 4500;
            var recipe = ForgingSystem.recipeById(id);
            var materials = materials(recipe);
            var correct = ForgingSystem.createMold(recipe, materials, temperature, "zhen_fire");
            var tooCold = ForgingSystem.createMold(recipe, materials, 3500, "zhen_fire");
            var correctData = forgingData(correct);
            var coldData = forgingData(tooCold);
            helper.assertTrue(correctData.getInt("target_temp") == temperature
                            && coldData.getInt("target_temp") == temperature
                            && correctData.getDouble("average_temp") == temperature
                            && coldData.getDouble("average_temp") == 3500,
                    "Actual mold metadata distinguishes native target from the heat used: " + id);
            helper.assertTrue(correctData.getInt("quality") == 100
                            && coldData.getInt("quality") < correctData.getInt("quality"),
                    "Mortal heat cannot earn perfect quality for a higher-realm weapon: " + id);
            var weapon = ForgingSystem.temper(correct);
            helper.assertTrue(weapon.is(HigherWeaponRecipes.item(id))
                            && forgingData(weapon).getInt("target_temp") == temperature
                            && forgingData(weapon).getInt("quality") == 100,
                    "Tempering produces the intended weapon and preserves its correct heat quality: " + id);
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void trueFireReachesHigherTargetsWhileQiRefiningFiresStopBelowFoundation(GameTestHelper helper) {
        try (var fixture = new Forger(helper)) {
            var recipe = ForgingSystem.recipeById("black_iron_jian");
            for (Block fire : List.of(Blocks.CAMPFIRE, ModBlocks.LING_FIRE.get())) {
                var ding = forge(helper, fire);
                fill(ding, recipe, materials(recipe));
                double qiBefore = ModAttachments.getData(fixture.player).qi();
                ding.startForging(fixture.player);
                helper.assertTrue(ding.data(4) == 0 && ModAttachments.getData(fixture.player).qi() == qiBefore
                                && ding.getItem(DingBlockEntity.MATERIAL_START).getCount() == 1,
                        "Lower fire cannot start a native Foundation weapon or consume its inputs");
            }
            for (String id : List.of("black_iron_jian", "seven_star_sword")) {
                int target = "seven_star_sword".equals(id) ? 6500 : 4500;
                int pulses = "seven_star_sword".equals(id) ? 26 : 18;
                var ding = forge(helper, ModBlocks.ZHEN_FIRE.get());
                recipe = ForgingSystem.recipeById(id);
                fill(ding, recipe, materials(recipe));
                ding.startForging(fixture.player);
                var menu = new DingMenu(0, fixture.player.getInventory(), ding);
                helper.assertTrue(menu.forging() && menu.temperature() == 0
                                && menu.targetTemperature() == target && menu.highTemp() == 10000
                                && menu.lowTemp() == 0 && menu.fireCost() == 40,
                        "True fire exposes the new range and the native target in the actual forge menu");
                double qiBefore = ModAttachments.getData(fixture.player).qi();
                for (int i = 0; i < pulses; i++) ding.heatPulse(fixture.player);
                helper.assertTrue(menu.temperature() == target
                                && ModAttachments.getData(fixture.player).qi() == qiBefore - pulses * 40,
                        "True fire reaches the exact target in " + pulses + " paid heat pulses: " + id);
                var saved = ding.saveWithFullMetadata(helper.getLevel().registryAccess());
                var loaded = reload(helper, saved);
                var restoredMenu = new DingMenu(1, fixture.player.getInventory(), loaded);
                helper.assertTrue(restoredMenu.forging() && restoredMenu.targetTemperature() == target
                                && restoredMenu.temperature() == target && restoredMenu.highTemp() == 10000,
                        "A saved active forge and its reopened menu preserve target and current heat");
                for (int i = 0; i < 40; i++) loaded.heatPulse(fixture.player);
                helper.assertTrue(restoredMenu.temperature() == 10000,
                        "True fire heat pulses clamp at 10000 instead of exceeding the fire ceiling");
            }
            for (Block fire : List.of(Blocks.CAMPFIRE, ModBlocks.LING_FIRE.get())) {
                boolean spirit = fire == ModBlocks.LING_FIRE.get();
                var ding = forge(helper, fire);
                recipe = ForgingSystem.recipeById("dao");
                fill(ding, recipe, materials(recipe));
                ding.startForging(fixture.player);
                var menu = new DingMenu(2, fixture.player.getInventory(), ding);
                int low = 0;
                int high = 4000;
                helper.assertTrue(menu.forging() && menu.lowTemp() == low && menu.highTemp() == high
                                && menu.fireCost() == (spirit ? 20 : 10),
                        "Qi Refining fires start at zero and retain their heat ceilings and Qi costs");
                ding.heatPulse(fixture.player);
                helper.assertTrue(menu.temperature() == low + 250,
                        "Both Qi Refining fires can heat their existing 3500-degree weapon recipes");
                for (int i = 0; i < 40; i++) ding.heatPulse(fixture.player);
                helper.assertTrue(menu.temperature() == high, "Original fire cannot acquire the true-fire ceiling");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void shardReforgeUsesSerializedNativeWeaponDespiteRestrictionRank(GameTestHelper helper) {
        var recipe = ForgingSystem.recipeById(WeaponShardSystem.REFORGE);
        helper.assertTrue(recipe.averageTemp() == 3500, "Generic shard recipe retains its fallback heat");
        for (String id : List.of("black_iron_jian", "seven_star_sword", "whip", "dao")) {
            int expected = switch (id) {
                case "seven_star_sword" -> 6500;
                case "whip" -> 2400;
                case "dao" -> 3500;
                default -> 4500;
            };
            var weapon = evolvedWeapon(helper, id);
            var shards = WeaponShardSystem.split(weapon, helper.getLevel().registryAccess());
            helper.assertTrue(ForgingSystem.targetTemperature(recipe, shards) == expected,
                    "An evolved weapon retains its original item recipe temperature: " + id);
            WeaponRestrictionSystem.debugSetTier(weapon, WeaponRestrictionSystem.MAX_TIER);
            shards = WeaponShardSystem.split(weapon, helper.getLevel().registryAccess());
            var first = shards.getFirst();
            first = ItemStack.parse(helper.getLevel().registryAccess(), first.save(helper.getLevel().registryAccess())).orElseThrow();
            var payload = WeaponShardSystem.data(first);
            payload.putString("item", "immortal_cultivation_mod:dao");
            setShardData(first, payload);
            shards.set(0, first);
            helper.assertTrue(shards.size() == WeaponRestrictionSystem.MAX_TIER
                            && ForgingSystem.targetTemperature(recipe, shards) == expected,
                    "Debug rank and a changed display label cannot override the serialized native weapon: " + id);
            if ("seven_star_sword".equals(id)) {
                try (var fixture = new Forger(helper)) {
                    var ding = forge(helper, ModBlocks.ZHEN_FIRE.get());
                    var inputs = reforgeBatch(shards);
                    helper.assertTrue(WeaponShardSystem.canReforge(inputs), "All serialized shards remain a valid complete set");
                    fill(ding, null, inputs);
                    ding.startForging(fixture.player);
                    helper.assertTrue(ding.data(4) == 1 && ding.data(1) == 6500,
                            "An actual automatic reforge resolves 6500 from the stored weapon, not generic recipe heat");
                    var saved = ding.saveWithFullMetadata(helper.getLevel().registryAccess());
                    var loaded = reload(helper, saved);
                    helper.assertTrue(new DingMenu(3, fixture.player.getInventory(), loaded).targetTemperature() == 6500,
                            "Dynamic shard target survives a save and reopened forge menu");
                }
            }
        }
        var unknown = WeaponShardSystem.split(evolvedWeapon(helper, "dao"), helper.getLevel().registryAccess()).getFirst();
        var payload = WeaponShardSystem.data(unknown);
        payload.getCompound("weapon").putString("id", "immortal_cultivation_mod:unknown_old_weapon");
        setShardData(unknown, payload);
        helper.assertTrue(WeaponShardSystem.populated(unknown)
                        && ForgingSystem.targetTemperature(recipe, List.of(unknown)) == 3500,
                "Unknown saved weapon types use the safe legacy fallback");
        payload.getCompound("weapon").remove("id");
        setShardData(unknown, payload);
        helper.assertTrue(ForgingSystem.targetTemperature(recipe, List.of(unknown)) == 3500,
                "Old shards lacking an original serialized item id use the legacy fallback");
        helper.succeed();
    }

    @GameTest(template = "fate_test", timeoutTicks = 50)
    public static void alreadyRunningForgeKeepsSavedTargetThroughCompletion(GameTestHelper helper) {
        var fixture = new Forger(helper);
        var recipe = ForgingSystem.recipeById("black_iron_jian");
        var ding = forge(helper, ModBlocks.ZHEN_FIRE.get());
        var inputs = materials(recipe);
        helper.assertTrue(ForgingSystem.forgingSuccessChancePercent(inputs) == 100,
                "The saved-batch fixture uses guaranteed-purity inputs rather than a random forge outcome");
        fill(ding, recipe, inputs);
        ding.startForging(fixture.player);
        var saved = ding.saveWithFullMetadata(helper.getLevel().registryAccess());
        saved.putInt("targetTemperature", 4300);
        saved.putString("activeFireType", "ling_fire");
        helper.setBlock(FORGE_POS.below(), ModBlocks.LING_FIRE.get());
        saved.putInt("temperature", 4300);
        saved.putInt("maxProgressTicks", 20);
        saved.putInt("progressTicks", 19);
        saved.putLong("temperatureSamples", 1);
        saved.putLong("temperatureTotal", 4300);
        var loaded = reload(helper, saved);
        helper.getLevel().removeBlockEntity(helper.absolutePos(FORGE_POS));
        helper.assertTrue(new DingMenu(4, fixture.player.getInventory(), loaded).targetTemperature() == 4300
                        && ForgingSystem.recipeById("black_iron_jian").averageTemp() == 4500,
                "Loading an old running batch must preserve its saved target even when recipe defaults differ");
        // Fake players are not in the world's nearby-player list. Finish between upkeep ticks.
        long delay = 21 - helper.getLevel().getGameTime() % 20;
        helper.runAfterDelay(delay, () -> {
            try {
                DingBlockEntity.serverTick(helper.getLevel(), loaded.getBlockPos(), loaded.getBlockState(), loaded);
                var result = loaded.getItem(DingBlockEntity.RESULT_SLOT);
                helper.assertTrue(result.is(ModItems.SWORD_MOLD.get()) && loaded.data(4) == 0,
                        "A loaded active batch completes through the real forge tick");
                var data = forgingData(result);
                helper.assertTrue(data.getInt("target_temp") == 4300
                                && data.getDouble("average_temp") == 4300 && data.getInt("quality") == 100
                                && loaded.data(0) <= 4000
                                && data.getString("fire").equals("ling_fire"),
                        "Saved target and recorded samples stay unchanged while live heat obeys the actual fire ceiling");
                helper.succeed();
            } finally {
                fixture.close();
            }
        });
    }

    private static void assertNativeRecipe(GameTestHelper helper, String id, int temperature) {
        var recipe = ForgingSystem.recipeById(id);
        var slipItem = HigherWeaponRecipes.item(id + "_yujian");
        var slipRecipe = ForgingSystem.recipe(new ItemStack(slipItem));
        helper.assertTrue(recipe != null && slipItem != Items.AIR && slipRecipe != null
                        && recipe.averageTemp() == temperature && slipRecipe.averageTemp() == temperature
                        && ForgingSystem.resultItem(recipe).asItem() == HigherWeaponRecipes.item(id)
                        && ForgingSystem.targetTemperature(recipe, List.of()) == temperature,
                "Native recipe and its registered slip agree on item and realm heat: " + id);
        var rebuilt = new ForgingSystem.ForgingRecipe(id, recipe.moldItem(), 1234,
                recipe.required(), recipe.directOutput(), recipe.spiritStoneCount(), recipe.requiredFireType());
        helper.assertTrue(rebuilt.averageTemp() == temperature, "Native heat is centralized for reconstructed recipes: " + id);
        helper.assertTrue(ForgingSystem.canUseFire(recipe, "zhen_fire")
                        && !ForgingSystem.canUseFire(recipe, "ling_fire")
                        && !ForgingSystem.canUseFire(recipe, "normal_fire"),
                "Fresh higher-realm weapons require true fire: " + id);
    }

    private static List<ItemStack> materials(ForgingSystem.ForgingRecipe recipe) {
        var result = new ArrayList<ItemStack>();
        recipe.required().forEach((item, count) -> {
            var sample = new ItemStack(item);
            int capacity = ForgingSystem.canStackInForgingRecipe(sample) ? sample.getMaxStackSize() : 1;
            for (int remaining = count; remaining > 0; remaining -= capacity) {
                var stack = new ItemStack(item, Math.min(capacity, remaining));
                var data = new CompoundTag();
                data.putInt(ForgingSystem.TAG_PURITY, 100);
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
                result.add(stack);
            }
        });
        for (int remaining = recipe.spiritStoneCount(); remaining > 0; remaining -= 64) {
            result.add(new ItemStack(com.example.immortal_cultivation_mod.magicritual.item.ModItems.HIGH_SPIRIT_STONE.get(),
                    Math.min(64, remaining)));
        }
        return result;
    }

    private static DingBlockEntity forge(GameTestHelper helper, Block fire) {
        helper.setBlock(FORGE_POS, Blocks.AIR);
        helper.setBlock(FORGE_POS.below().below(), Blocks.STONE);
        helper.setBlock(FORGE_POS.below(), fire);
        helper.setBlock(FORGE_POS, ModBlocks.QINGTONG_YINHUO_DING.get());
        return (DingBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(FORGE_POS));
    }

    private static void fill(DingBlockEntity ding, ForgingSystem.ForgingRecipe recipe, List<ItemStack> inputs) {
        if (inputs.size() > ding.materialSlotCount()) throw new AssertionError("Test recipe exceeds physical forge capacity");
        if (recipe != null) ding.setItem(DingBlockEntity.YUJIAN_SLOT, new ItemStack(HigherWeaponRecipes.item(recipe.id() + "_yujian")));
        for (int i = 0; i < inputs.size(); i++) ding.setItem(DingBlockEntity.MATERIAL_START + i, inputs.get(i).copy());
    }

    private static DingBlockEntity reload(GameTestHelper helper, CompoundTag saved) {
        var pos = helper.absolutePos(FORGE_POS);
        var loaded = new DingBlockEntity(pos, helper.getLevel().getBlockState(pos));
        loaded.loadWithComponents(saved, helper.getLevel().registryAccess());
        loaded.setLevel(helper.getLevel());
        return loaded;
    }

    private static CompoundTag forgingData(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound(ForgingSystem.TAG_FORGING);
    }

    private static ItemStack evolvedWeapon(GameTestHelper helper, String id) {
        var weapon = new ItemStack(HigherWeaponRecipes.item(id));
        var layers = new int[WeaponRestrictionSystem.RestrictionType.values().length];
        layers[WeaponRestrictionSystem.RestrictionType.QI_STORAGE.ordinal()] = 72;
        helper.assertTrue(WeaponRestrictionSystem.addRestrictions(weapon, layers) && WeaponRestrictionSystem.evolve(weapon),
                "Prepare a weapon with an actual completed restriction breakthrough: " + id);
        return weapon;
    }

    private static List<ItemStack> reforgeBatch(List<ItemStack> shards) {
        var result = new ArrayList<>(shards);
        result.add(new ItemStack(com.example.immortal_cultivation_mod.magicritual.item.ModItems.HIGH_SPIRIT_STONE.get(), 64));
        result.add(new ItemStack(com.example.immortal_cultivation_mod.magicritual.item.ModItems.HIGH_SPIRIT_STONE.get(), 64));
        return result;
    }

    private static void setShardData(ItemStack shard, CompoundTag payload) {
        var root = shard.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        root.put(WeaponShardSystem.TAG, payload);
        shard.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    private static final class Forger implements AutoCloseable {
        private final FakePlayer player;
        private final EmbeddedChannel channel;

        private Forger(GameTestHelper helper) {
            player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "heat-forger"));
            channel = new EmbeddedChannel(player.connection.getConnection());
            var pos = helper.absolutePos(FORGE_POS);
            player.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
            ModAttachments.setData(player, ModAttachments.getData(player).withQi(20000));
        }

        @Override
        public void close() {
            player.discard();
            channel.close();
        }
    }
}
