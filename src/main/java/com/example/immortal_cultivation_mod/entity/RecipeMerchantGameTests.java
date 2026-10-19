package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.system.SystemShopPrices;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@GameTestHolder("immortal_cultivation_recipe_merchant")
@PrefixGameTestTemplate(false)
public final class RecipeMerchantGameTests {
    private static final List<String> REALMS = List.of(CultivationLevels.REALM_LIANQI,
            CultivationLevels.REALM_ZHUJI, CultivationLevels.REALM_JINDAN, CultivationLevels.REALM_YUANYING,
            CultivationLevels.REALM_HUASHEN, CultivationLevels.REALM_LIANXU, CultivationLevels.REALM_HETI,
            CultivationLevels.REALM_DACHENG, CultivationLevels.REALM_DUJIE);

    private static RecipeMerchantEntity merchant(GameTestHelper helper, int realm) {
        var mob = ModEntities.RECIPE_MERCHANT.get().create(helper.getLevel());
        helper.assertTrue(mob != null, "Merchant entity creates with registered attributes");
        mob.setNoAi(true);
        NpcProfile.ensure(mob, NpcProfile.MERCHANT);
        NpcProfile.cultivationProfile(mob).putString("Realm", REALMS.get(realm - 1) + CultivationLevels.STAGE_EARLY);
        NpcProfile.ensure(mob, NpcProfile.MERCHANT);
        return mob;
    }

    @GameTest(template = "fate_test")
    public static void everyRealmCarriesTwelveDifferentUsableRecipes(GameTestHelper helper) {
        for (int realm = 1; realm <= 9; realm++) {
            var mob = merchant(helper, realm);
            var offers = NpcProfile.tradeOffers(mob);
            helper.assertTrue(offers.size() == 12, "Twelve visible recipe offers at realm " + realm);
            var ids = new HashSet<String>();
            for (var offer : offers) {
                ItemStack stock = NpcProfile.inventory(mob).get(offer.inventorySlot());
                helper.assertTrue(RecipeMerchantStock.isRecipe(stock), "Every offer maps to a real usable recipe");
                helper.assertTrue(RecipeMerchantStock.grade(stock) <= realm, "Recipe respects merchant realm");
                helper.assertTrue(ids.add(offer.resultItemId()), "Recipes are distinct");
            }
            helper.assertTrue(mob.getNpcSkinId().equals("recipe_merchant"), "Supplied merchant skin selected");
            helper.assertTrue(mob.npcIdentity().equals(NpcProfile.MERCHANT), "Merchant identity retained");
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void higherRealmsCarryRarerRecipes(GameTestHelper helper) {
        double low = 0, high = 0;
        for (int seed = 0; seed < 24; seed++) {
            for (var stack : RecipeMerchantStock.select(1, seed)) low += RecipeMerchantStock.grade(stack);
            for (var stack : RecipeMerchantStock.select(4, seed)) high += RecipeMerchantStock.grade(stack);
        }
        helper.assertTrue(high > low * 1.5, "Higher-realm stock has substantially higher average recipe grade");
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void tradePricesExactlyMatchSystemValues(GameTestHelper helper) {
        var mob = merchant(helper, 4);
        for (var offer : NpcProfile.tradeOffers(mob)) {
            int unit = currencyValue(offer.currencyItemId());
            var stock = NpcProfile.inventory(mob).get(offer.inventorySlot());
            helper.assertTrue((long) offer.price() * unit == SystemShopPrices.price(stock),
                    "Currency conversion preserves canonical system cost for " + offer.resultItemId());
        }
        helper.succeed();
    }

    private static int currencyValue(String id) {
        if (id.endsWith(":high_spirit_stone")) return 1000;
        if (id.endsWith(":medium_spirit_stone")) return 100;
        if (id.endsWith(":spirit_stone")) return 10;
        if (id.endsWith(":copper_coin")) return 1;
        throw new AssertionError("Unexpected currency: " + id);
    }

    @GameTest(template = "fate_test")
    public static void purchaseChargesPlayerAndDoesNotRegenerateSoldStock(GameTestHelper helper) {
        var mob = merchant(helper, 1);
        var player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "RecipeBuyer"), ClientInformation.createDefault());
        var offer = NpcProfile.tradeOffers(mob).getFirst();
        var before = NpcProfile.cultivationProfile(mob).getList("Inventory", 10).copy();
        helper.assertTrue(NpcProfile.exchangeTrade(mob, player, 0).equals("npc.status.trade_missing"),
                "Unfunded purchase rejected");
        helper.assertTrue(before.equals(NpcProfile.cultivationProfile(mob).getList("Inventory", 10)),
                "Rejected purchase leaves inventory unchanged");
        var currency = BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(offer.currencyItemId()));
        helper.assertTrue(currency != Items.AIR, "Trade currency registered");
        player.getInventory().setItem(0, new ItemStack(currency, offer.price()));
        helper.assertTrue(NpcProfile.exchangeTrade(mob, player, 0).equals("npc.status.trade_complete"), "Real purchase succeeds");
        helper.assertTrue(player.getInventory().items.stream().noneMatch(s -> s.is(currency)), "Full exact payment consumed");
        helper.assertTrue(player.getInventory().items.stream().anyMatch(s -> !s.isEmpty()
                && BuiltInRegistries.ITEM.getKey(s.getItem()).toString().equals(offer.resultItemId())), "Buyer receives recipe");
        NpcProfile.ensure(mob, NpcProfile.MERCHANT);
        helper.assertTrue(NpcProfile.tradeOffers(mob).size() == 11, "Sold recipe stays sold until restock");
        helper.assertTrue(NpcProfile.tradeOffers(mob).stream().noneMatch(o -> o.resultItemId().equals(offer.resultItemId())),
                "No duplicate offer or immediate regeneration");
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void savedStockSurvivesReloadAndRestocksOnlyWhenDue(GameTestHelper helper) {
        var mob = merchant(helper, 3);
        var offers = NpcProfile.tradeOffers(mob);
        var inventory = NpcProfile.inventory(mob);
        inventory.set(offers.getFirst().inventorySlot(), ItemStack.EMPTY);
        NpcProfile.writeInventory(mob, NpcProfile.cultivationProfile(mob), inventory);
        var saved = new CompoundTag();
        NpcProfile.save(mob, saved);
        var loaded = ModEntities.RECIPE_MERCHANT.get().create(helper.getLevel());
        NpcProfile.load(loaded, saved);
        NpcProfile.ensure(loaded, NpcProfile.MERCHANT);
        helper.assertTrue(NpcProfile.tradeOffers(loaded).equals(NpcProfile.tradeOffers(mob)), "Reload keeps exact stock and sold slot");
        var profile = NpcProfile.cultivationProfile(loaded);
        var stored = NpcProfile.inventory(loaded);
        stored.set(offers.getFirst().inventorySlot(), new ItemStack(Items.DIAMOND, 7));
        NpcProfile.writeInventory(loaded, profile, stored);
        profile.putLong("MerchantStockDay", Math.floorDiv(helper.getLevel().getDayTime(), 24000L) - 1);
        NpcProfile.ensure(loaded, NpcProfile.MERCHANT);
        helper.assertTrue(NpcProfile.tradeOffers(loaded).size() == 12, "Next-day restock restores twelve recipes");
        helper.assertTrue(NpcProfile.inventory(loaded).stream().filter(s -> s.is(Items.DIAMOND))
                .mapToInt(ItemStack::getCount).sum() == 7, "Restocking preserves non-recipe items in sold stock slots");
        profile.putString("Realm", CultivationLevels.REALM_LIANQI + CultivationLevels.STAGE_EARLY);
        NpcProfile.ensure(loaded, NpcProfile.MERCHANT);
        helper.assertTrue(NpcProfile.inventory(loaded).stream().filter(RecipeMerchantStock::isRecipe)
                .allMatch(s -> RecipeMerchantStock.grade(s) <= 1), "Realm downgrade refreshes stock eligibility");
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void merchantDialogueExistsInEnglishAndChinese(GameTestHelper helper) throws Exception {
        var mob = merchant(helper, 2);
        for (String locale : List.of("en_us", "zh_cn")) {
            String path = "/assets/immortal_cultivation_mod/lang/" + locale + ".json";
            try (var input = RecipeMerchantGameTests.class.getResourceAsStream(path)) {
                helper.assertTrue(input != null, "Locale resource exists");
                var lang = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
                for (String topic : List.of("personal", "realm", "world")) {
                    String previous = "";
                    for (int i = 0; i < 24; i++) {
                        String reply = NpcProfile.identityDialogue(mob, NpcProfile.MERCHANT, topic);
                        helper.assertTrue(reply.startsWith("npc.dialogue.identity.recipe_merchant."), "Merchant has role-specific dialogue");
                        helper.assertTrue(!reply.equals(previous), "Dialogue avoids immediate repetition");
                        helper.assertTrue(lang.has("screen.immortal_cultivation_mod." + reply), "Dialogue translated in " + locale);
                        previous = reply;
                    }
                }
            }
        }
        helper.succeed();
    }
}
