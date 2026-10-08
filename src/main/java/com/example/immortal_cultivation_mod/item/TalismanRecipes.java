package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Supplier;

/** Server-authoritative talisman formula requirements. */
public final class TalismanRecipes {
    public static final String DRAWING_PROFICIENCY = "craft_talisman";
    public static final String FORMULA_PREFIX = "talisman_formula:";
    public static final String SUCCESS_PREFIX = "talisman_success:";
    public static final int BASE_MASTERY_SUCCESSES = 10;

    private static final int[] MASTER_THRESHOLDS = {
            0, 0, 100, 500, 1_000, 2_500, 5_000, 7_500, 10_000
    };

    private static final List<Def> RECIPES = List.of(
            def("qingchen_talisman", ModItems.QINGCHEN_TALISMAN, 1, 1, 1, 1, 15, "fuchen"),
            def("weiguang_talisman", ModItems.WEIGUANG_TALISMAN, 1, 1, 1, 1, 20, "juguang"),
            def("liansheng_talisman", ModItems.LIANSHENG_TALISMAN, 1, 1, 1, 1, 25, "shouyin"),
            def("fireball_talisman", ModItems.FIREBALL_TALISMAN, 1, 1, 1, 1, 35, "juhuo"),
            def("fengren_talisman", ModItems.FENGREN_TALISMAN, 1, 1, 1, 1, 40, "fengqie"),

            def("xinghuo_talisman", ModItems.XINGHUO_TALISMAN, 2, 2, 2, 2, 80, "fenhuo_xing"),
            def("bojin_hufu", ModItems.BOJIN_HUFU, 2, 2, 2, 2, 100, "bojia_hu"),
            def("chansi_kun_talisman", ModItems.CHANSI_KUN_TALISMAN, 2, 2, 2, 2, 120, "fusi"),
            def("duantu_shenxing_talisman", ModItems.DUANTU_SHENXING_TALISMAN, 2, 2, 2, 2, 110, "xingqi"),
            def("chuanyin_talisman", ModItems.CHUANYIN_TALISMAN, 2, 2, 2, 2, 90, "chuansheng"),
            def("zhenshi_talisman", ModItems.ZHENSHI_TALISMAN, 2, 2, 2, 2, 140, "zhenshi_gu"),
            def("fengyan_talisman", ModItems.FENGYAN_TALISMAN, 2, 2, 2, 2, 120, "bimu"),
            def("xiufu_talisman", ModItems.XIUFU_TALISMAN, 2, 2, 2, 2, 150, "xuhe"),

            def("bixie_anhun_talisman", ModItems.BIXIE_ANHUN_TALISMAN, 3, 2, 3, 3, 280, "dingshen_anhun"),
            def("zhenrao_talisman", ModItems.ZHENRAO_TALISMAN, 3, 2, 3, 3, 300, "zhenyu"),
            def("baofeng_talisman", ModItems.BAOFENG_TALISMAN, 3, 3, 3, 3, 360, "juya_baofeng"),
            def("huoyu_fenxie_talisman", ModItems.HUOYU_FENXIE_TALISMAN, 3, 3, 3, 3, 450, "huoyu"),
            def("wulei_xiaofa_talisman", ModItems.WULEI_XIAOFA_TALISMAN, 3, 3, 3, 3, 500, "wulei_yin"),
            def("biaoji_talisman", ModItems.BIAOJI_TALISMAN, 3, 2, 3, 3, 220, "liuyin_zhuiji"),
            def("dunkong_talisman", ModItems.DUNKONG_TALISMAN, 3, 3, 3, 3, 420, "dunkong"),
            def("huabing_talisman", ModItems.HUABING_TALISMAN, 3, 3, 3, 3, 400, "huabing"),

            def("jinguang_huti_talisman", ModItems.JINGUANG_HUTI_TALISMAN, 4, 3, 4, 4, 800, "chongguang_hushen"),
            def("fengling_talisman", ModItems.FENGLING_TALISMAN, 4, 3, 4, 4, 900, "suomai_fengling"),
            def("pozhang_kaishan_talisman", ModItems.POZHANG_KAISHAN_TALISMAN, 4, 3, 4, 4, 1_000, "xunxi_pozhang"),

            def("qianli_chuanxun_talisman", ModItems.QIANLI_CHUANXUN_TALISMAN, 5, 4, 5, 5, 1_500, "qianli_chuanshen"),

            def("xuanyan_fenjie_talisman", ModItems.XUANYAN_FENJIE_TALISMAN, 6, 5, 6, 6, 1_200, "xuanyan_fenjie"),
            def("wuyue_bie_talisman", ModItems.WUYUE_BIE_TALISMAN, 6, 5, 6, 6, 1_800, "wuyue_zhene"),
            def("jinkong_talisman", ModItems.JINKONG_TALISMAN, 6, 4, 5, 5, 2_400, "gu_jinkong")
    );

    private TalismanRecipes() {
    }

    private static Def def(String id, Supplier<? extends Item> output, int strengthTier,
                           int paperTier, int inkTier, int masterTier, int qiCost,
                           String patternId) {
        return new Def(id, output, strengthTier, paperTier, inkTier,
                masterTier, qiCost, patternId);
    }

    public static List<Def> all() {
        return RECIPES;
    }

    public static Def byId(String id) {
        if (id == null) return null;
        for (Def recipe : RECIPES) {
            if (recipe.id.equals(id)) return recipe;
        }
        return null;
    }

    public static int indexOf(String id) {
        for (int i = 0; i < RECIPES.size(); i++) {
            if (RECIPES.get(i).id.equals(id)) return i;
        }
        return -1;
    }

    public static String formulaKey(String recipeId) {
        return FORMULA_PREFIX + recipeId;
    }

    public static boolean isLearned(ModAttachments.CultivationData data, String recipeId) {
        return data != null && byId(recipeId) != null
                && data.methodProficiencies().containsKey(formulaKey(recipeId));
    }

    public static String successKey(String recipeId) {
        return SUCCESS_PREFIX + recipeId;
    }

    public static int successCount(ModAttachments.CultivationData data, String recipeId) {
        if (data == null || byId(recipeId) == null) return 0;
        return Math.max(0, data.methodProficiencies().getOrDefault(successKey(recipeId), 0));
    }

    /**
     * Manual successes needed before a recipe can be produced instantly.
     * Every strength tier adds another ten required successful drawings.
     */
    public static int masterySuccesses(Def recipe) {
        return BASE_MASTERY_SUCCESSES * Math.max(1, recipe == null ? 1 : recipe.strengthTier());
    }

    public static boolean isMastered(ModAttachments.CultivationData data, String recipeId) {
        Def recipe = byId(recipeId);
        return recipe != null && successCount(data, recipeId) >= masterySuccesses(recipe);
    }

    public static int masterTier(ModAttachments.CultivationData data) {
        int proficiency = data == null ? 0 : data.methodProficiency(DRAWING_PROFICIENCY);
        int tier = 1;
        for (int i = 2; i < MASTER_THRESHOLDS.length; i++) {
            if (proficiency >= MASTER_THRESHOLDS[i]) tier = i;
        }
        return tier;
    }

    public static int thresholdForMasterTier(int tier) {
        return MASTER_THRESHOLDS[Math.max(1, Math.min(8, tier))];
    }

    public static boolean acceptsPaper(Def recipe, ItemStack stack) {
        return recipe != null && TalismanMaterialItem.isPaper(stack)
                && TalismanMaterialItem.tier(stack) >= recipe.minPaperTier;
    }

    public static boolean acceptsInk(Def recipe, ItemStack stack) {
        return recipe != null && TalismanMaterialItem.isInk(stack)
                && TalismanMaterialItem.tier(stack) >= recipe.minInkTier;
    }

    public static String patternNameKey(Def recipe) {
        return "talisman_pattern." + ImmortalCultivationMod.MODID + "." + recipe.patternId;
    }

    public record Def(String id, Supplier<? extends Item> output, int strengthTier,
                      int minPaperTier, int minInkTier, int masterTier,
                      int qiCost, String patternId) {
        public ItemStack result() {
            return new ItemStack(output.get());
        }
    }
}
