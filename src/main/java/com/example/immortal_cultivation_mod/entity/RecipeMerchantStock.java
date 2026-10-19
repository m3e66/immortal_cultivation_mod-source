package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.item.CultivationItemRarity;
import com.example.immortal_cultivation_mod.item.ForgingSystem;
import com.example.immortal_cultivation_mod.item.ForgingYujianItem;
import com.example.immortal_cultivation_mod.item.HigherWeaponSpec;
import com.example.immortal_cultivation_mod.item.TalismanFormulaItem;
import com.example.immortal_cultivation_mod.item.TalismanRecipes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/** Physical, finite recipe stock. Purchases never silently regenerate sold slips. */
public final class RecipeMerchantStock {
    public static final int OFFER_COUNT = 12;
    private static final int VERSION = 1;
    private record Recipe(Item item, int grade) { }
    private static List<Recipe> recipes;

    private RecipeMerchantStock() { }

    public static String startingRealm(int seed) {
        int roll = Math.floorMod(seed / 173, 100);
        return roll < 45 ? CultivationLevels.REALM_LIANQI
                : roll < 80 ? CultivationLevels.REALM_ZHUJI
                : roll < 95 ? CultivationLevels.REALM_JINDAN : CultivationLevels.REALM_YUANYING;
    }

    static void ensure(RecipeMerchantEntity mob, CompoundTag profile) {
        int realm = CultivationLevels.getRealmIndex(profile.getString("Realm"));
        long day = Math.floorDiv(mob.level().getDayTime(), 24_000L);
        if (profile.getInt("MerchantStockVersion") == VERSION
                && profile.getInt("MerchantStockRealm") == realm
                && profile.getLong("MerchantStockDay") == day) return;

        List<ItemStack> inventory = NpcProfile.inventory(mob);
        // Reserve the last twelve slots for stock; personal equipment stays separate.
        int first = inventory.size() - OFFER_COUNT;
        for (int slot = first; slot < inventory.size(); slot++) {
            ItemStack existing = inventory.get(slot);
            if (!existing.isEmpty() && !isRecipe(existing)) {
                ItemStack retained = existing.copy();
                if (!NpcProfile.insertInventory(inventory.subList(0, first), retained)) {
                    mob.spawnAtLocation(retained);
                }
            }
        }
        var selected = select(realm, mob.getUUID().getMostSignificantBits()
                ^ mob.getUUID().getLeastSignificantBits() ^ day * 31L ^ realm * 1009L);
        for (int i = 0; i < OFFER_COUNT; i++) inventory.set(first + i, selected.get(i));
        NpcProfile.writeInventory(mob, profile, inventory);
        profile.putInt("MerchantStockVersion", VERSION);
        profile.putInt("MerchantStockRealm", realm);
        profile.putLong("MerchantStockDay", day);
        profile.putString("Skin", "recipe_merchant");
        profile.putString("Gender", NpcSkinCatalog.MALE);
    }

    public static boolean isRecipe(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof TalismanFormulaItem) {
            return TalismanRecipes.byId(TalismanFormulaItem.recipeId(stack)) != null;
        }
        return stack.getItem() instanceof ForgingYujianItem && ForgingSystem.recipe(stack) != null;
    }

    /** Recipe difficulty and existing item rarity both limit low-realm merchants. */
    public static int grade(ItemStack stack) {
        int rarity = CultivationItemRarity.of(stack).ordinal() + 1;
        if (stack.getItem() instanceof TalismanFormulaItem) {
            var recipe = TalismanRecipes.byId(TalismanFormulaItem.recipeId(stack));
            return recipe == null ? Integer.MAX_VALUE : Math.max(rarity, recipe.strengthTier());
        }
        var recipe = ForgingSystem.recipe(stack);
        if (recipe == null) return Integer.MAX_VALUE;
        var weapon = HigherWeaponSpec.find(recipe.id());
        int realm = weapon != null ? weapon.realm
                : recipe.averageTemp() < 4500 ? 1 : 2 + Math.max(0, (recipe.averageTemp() - 4500) / 2000);
        return Math.max(rarity, realm);
    }

    static List<ItemStack> select(int realm, long seed) {
        if (recipes == null) {
            recipes = BuiltInRegistries.ITEM.stream()
                    .filter(item -> isRecipe(item.getDefaultInstance()))
                    .sorted(Comparator.comparing(item -> BuiltInRegistries.ITEM.getKey(item).toString()))
                    .map(item -> new Recipe(item, grade(item.getDefaultInstance())))
                    .toList();
        }
        List<Recipe> pool = new ArrayList<>(recipes.stream()
                .filter(recipe -> recipe.grade() <= Math.max(1, realm)).toList());
        if (pool.size() < OFFER_COUNT) {
            throw new IllegalStateException("Not enough valid recipes for merchant realm " + realm);
        }
        int bestGrade = pool.stream().mapToInt(Recipe::grade).max().orElse(1);
        Random random = new Random(seed);
        List<ItemStack> result = new ArrayList<>();
        while (result.size() < OFFER_COUNT) {
            // Higher grades are favored while keeping a mixture of practical recipes.
            int total = pool.stream().mapToInt(item -> weight(item, bestGrade)).sum();
            int roll = random.nextInt(total);
            int chosen = 0;
            while ((roll -= weight(pool.get(chosen), bestGrade)) >= 0) chosen++;
            result.add(new ItemStack(pool.remove(chosen).item()));
        }
        return List.copyOf(result);
    }

    private static int weight(Recipe recipe, int bestGrade) {
        int difference = bestGrade - recipe.grade();
        return difference == 0 ? 32 : difference == 1 ? 8 : 1;
    }
}
