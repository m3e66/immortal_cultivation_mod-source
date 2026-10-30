package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.block.ModBlocks;
import com.example.immortal_cultivation_mod.flame.FlameKind;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ItemLike;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ForgingSystem {
    public static final String TAG_PURITY = ImmortalCultivationMod.MODID + ".purity";
    public static final String TAG_PENDING_PURITY_LUCK = ImmortalCultivationMod.MODID + ".pending_purity_luck";
    public static final String TAG_FORGING = ImmortalCultivationMod.MODID + ".forging";
    public static final int MAX_TEMP = 12000;
    /** Maximum physical ingredient stacks accepted by the Golden Core ding. */
    public static final int MAX_RECIPE_MATERIAL_SLOTS = 20;
    /** Every forging recipe consumes one complete stack of finite spirit stones. */
    public static final int RECIPE_SPIRIT_STONE_COUNT = 64;

    private static final List<MaterialDef> MATERIALS = List.of(
            new MaterialDef("jingtie_motherstone", ModBlocks.JINGTIE_MOTHERSTONE.get().asItem(), "Metal", "durability_percent"),
            new MaterialDef("qiannian_lingmu_heart", ModBlocks.QIANNIAN_LINGMU_HEART.get().asItem(), "Self-Repair", "self_repair_percent"),
            new MaterialDef("fugu_tai", ModBlocks.FUGU_TAI.get().asItem(), "Armor Erosion", "armor_erosion_percent"),
            new MaterialDef("yunwen_silver_crystal", ModBlocks.YUNWEN_SILVER_CRYSTAL.get().asItem(), "Qi Conductivity", "qi_damage_percent"),
            new MaterialDef("bisui_stone", ModBlocks.BISUI_STONE.get().asItem(), "Qi Regeneration", "qi_regen_percent"),
            new MaterialDef("hanpo_jade", ModBlocks.HANPO_JADE.get().asItem(), "Freeze Enhancement", "slowness_layers"),
            new MaterialDef("moxuan_iron", ModBlocks.MOXUAN_IRON.get().asItem(), "Armor Penetration", "armor_pen_percent"),
            new MaterialDef("liujin_sand", ModBlocks.LIUJIN_SAND.get().asItem(), "Forging Precision", "quality_bonus_percent"),
            new MaterialDef("lingjiao_scale", ModItems.LINGJIAO_SCALE.get(), "Dragon Pressure", "dragon_pressure"),
            new MaterialDef("star_dust_stone", ModBlocks.STAR_DUST_STONE.get().asItem(), "Star Essence", "star_essence"),
            new MaterialDef("chitong_sui", ModBlocks.CHITONG_SUI.get().asItem(), "Fire Storage", "fire_storage")
    );

    private ForgingSystem() {
    }

    public static List<MaterialDef> materials() {
        return MATERIALS;
    }

    public static boolean isForgeMaterial(ItemStack stack) {
        return material(stack) != null;
    }

    public static boolean isPurityMaterial(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return com.example.immortal_cultivation_mod.alchemy.AlchemyMenu.plantId(stack)!=null
                || isForgeMaterial(stack)
                || stack.getItem() instanceof TalismanMaterialItem
                || stack.is(ModItems.CHI_LIN.get())
                || stack.is(ModItems.HUO_XI_BLOOD.get())
                || stack.is(ModItems.LIANQI_YAODAN.get())
                || stack.is(ModItems.ZHUJI_YAODAN.get())
                || stack.is(ModItems.JINDAN_YAODAN.get())
                || stack.is(ModItems.CHIYAN_BONE.get())
                || stack.is(ModItems.YUANYING_YAODAN.get())
                || stack.is(ModItems.HUASHEN_YAODAN.get())
                || stack.is(ModItems.LIANXU_YAODAN.get())
                || stack.is(ModItems.HETI_YAODAN.get())
                || stack.is(ModItems.DACHENG_YAODAN.get())
                || stack.is(ModItems.DUJIE_YAODAN.get())
                || stack.is(ModItems.YANJIAO_HORN.get())
                || stack.is(ModItems.YAOYUAN_BONE.get())
                || stack.is(ModItems.XUESHA_HIDE.get())
                || stack.is(ModItems.PHANTOM_VEIL_FUR.get())
                || stack.is(ModItems.KONGMING_STONE.get())
                || stack.is(ModItems.ROCK_BADGER_HIDE.get())
                || stack.is(ModItems.THUNDER_PATTERNED_ROCK_PLATE.get())
                || stack.is(ModItems.THUNDER_HORN.get())
                || stack.is(ModItems.FROSTSPINE_HIDE.get())
                || stack.is(ModItems.SPIRIT_DEVOURING_RAT_HIDE.get())
                || stack.is(ModItems.SPIRIT_SCENT_WHISKER.get())
                || stack.is(ModItems.IRON_SCALE_PLATE.get())
                || stack.is(ModItems.CRIMSON_VENOM_SAC.get())
                || stack.is(ModItems.SCORPION_TAIL_STINGER.get())
                || stack.is(ModItems.WIND_FEATHER.get())
                || stack.is(ModItems.SPIRIT_DEER_ANTLER.get())
                || stack.is(ModItems.INK_ARMOR_PLATE.get())
                || stack.is(ModItems.MINERAL_MARROW.get())
                || stack.is(ModItems.PROFOUND_IRON_ARMOR_CORE.get())
                || stack.is(ModItems.EMBER_FEATHER.get())
                || stack.is(ModItems.FIRE_OWL_CLAW.get())
                || stack.is(ModItems.FLAME_RADIANCE_BONE.get())
                || stack.is(ModItems.MORTAL_BEAST_MEAT.get())
                || stack.is(ModItems.LIANQI_BEAST_MEAT.get())
                || stack.is(ModItems.ZHUJI_BEAST_MEAT.get())
                || stack.is(ModItems.JINDAN_BEAST_MEAT.get())
                || stack.is(ModItems.YUANYING_BEAST_MEAT.get())
                || stack.is(ModItems.HUASHEN_BEAST_MEAT.get())
                || stack.is(ModItems.LIANXU_BEAST_MEAT.get())
                || stack.is(ModItems.HETI_BEAST_MEAT.get())
                || stack.is(ModItems.DACHENG_BEAST_MEAT.get())
                || stack.is(ModItems.DUJIE_BEAST_MEAT.get())
                || stack.is(ModItems.COLD_MARROW.get())
                || stack.is(ModItems.XUANDUAN_IRON_SHARD.get())
                || stack.is(ModItems.SPIRIT_GUIDING_GEAR.get())
                || stack.is(ModItems.DAMAGED_FORGING_FURNACE_CORE.get())
                || stack.is(ModItems.LINGMU_CLASP.get())
                || stack.is(com.example.immortal_cultivation_mod.magicritual.item.ModItems.ZHENWEN_JADE.get())
                || stack.is(com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_SILKWORM_SILK.get())
                || stack.is(com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_GUIDING_THREAD.get())
                || stack.is(com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_STORAGE_CRYSTAL.get())
                || stack.is(com.example.immortal_cultivation_mod.magicritual.item.ModItems.FLOWING_SPIRIT_CRYSTAL.get())
                || stack.is(com.example.immortal_cultivation_mod.magicritual.item.ModItems.LINGMU_FLAGPOLE.get())
                || stack.is(com.example.immortal_cultivation_mod.magicritual.item.ModItems.PATTERNED_CLOTH.get())
                || fixedPurity(stack) > 0;
    }

    public static boolean hasFixedPurity(ItemStack stack) {
        return fixedPurity(stack) > 0;
    }

    private static int fixedPurity(ItemStack stack) {
        if (stack.is(com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_STONE.get())) {
            return 30;
        }
        if (stack.is(com.example.immortal_cultivation_mod.magicritual.item.ModItems.MEDIUM_SPIRIT_STONE.get())) {
            return 60;
        }
        if (stack.is(com.example.immortal_cultivation_mod.magicritual.item.ModItems.HIGH_SPIRIT_STONE.get())) {
            return 100;
        }
        return 0;
    }

    public static boolean isReusableAdditive(ItemStack stack) {
        return false;
    }

    public static MaterialDef material(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        for (MaterialDef def : MATERIALS) {
            if (stack.is(def.item())) {
                return def;
            }
        }
        return null;
    }

    public static int purity(ItemStack stack) {
        int fixed = fixedPurity(stack);
        if (fixed > 0) {
            return fixed;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return Math.max(1, Math.min(100, tag.getInt(TAG_PURITY)));
    }

    public static boolean hasDeterminedPurity(ItemStack stack) {
        if (!isPurityMaterial(stack)) {
            return false;
        }
        if (hasFixedPurity(stack)) {
            return true;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.contains(TAG_PURITY, Tag.TAG_INT);
    }

    public static void ensurePurity(ItemStack stack, int seed) {
        if (!isPurityMaterial(stack) || hasFixedPurity(stack)) {
            return;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(TAG_PURITY)) {
            int purity = 1 + Math.floorMod(seed * 31 + stack.getCount() * 17, 100);
            tag.putInt(TAG_PURITY, Math.clamp(purity + tag.getInt(TAG_PENDING_PURITY_LUCK), 1, 100));
            tag.remove(TAG_PENDING_PURITY_LUCK);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    public static void copyPurity(ItemStack source, ItemStack target) {
        if (!isPurityMaterial(source) || !isPurityMaterial(target) || hasFixedPurity(target)) {
            return;
        }
        CompoundTag tag = target.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(TAG_PURITY, purity(source));
        target.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static Component purityTooltip(ItemStack stack) {
        if (!isPurityMaterial(stack)) {
            return Component.empty();
        }
        if (!hasDeterminedPurity(stack)) {
            return Component.translatable("tooltip." + ImmortalCultivationMod.MODID + ".material_purity_unknown")
                    .withStyle(ChatFormatting.GRAY);
        }
        return Component.translatable("tooltip." + ImmortalCultivationMod.MODID + ".material_purity", purity(stack))
                .withStyle(ChatFormatting.AQUA);
    }

    public static Component shiftForBuffTooltip() {
        return Component.translatable("tooltip." + ImmortalCultivationMod.MODID + ".shift_for_material_buff")
                .withStyle(ChatFormatting.DARK_GRAY);
    }

    public static Component materialBuffTooltip(ItemStack stack) {
        MaterialDef def = material(stack);
        if (def == null) {
            return Component.empty();
        }
        return Component.translatable("tooltip." + ImmortalCultivationMod.MODID + ".material_buff." + def.id())
                .withStyle(ChatFormatting.GOLD);
    }

    public static ForgingRecipe recipe(ItemStack yujian) {
        if(yujian.getItem() instanceof com.example.immortal_cultivation_mod.alchemy.AlchemyFormulaItem formula)
            return recipeById("alchemy/"+formula.formula);
        String slipId=BuiltInRegistries.ITEM.getKey(yujian.getItem()).getPath();
        if(slipId.endsWith("_formula") && com.example.immortal_cultivation_mod.alchemy.AlchemyForgeRecipes.LEGACY_REQUIRED.containsKey(slipId.substring(0,slipId.length()-8)))
            return recipeById("legacy_alchemy/"+slipId.substring(0,slipId.length()-8));
        if(slipId.equals("jade_pill_bottle_yujian"))return recipeById("jade_pill_bottle");
        if(slipId.equals("qingtong_yinhuo_ding_yujian"))return recipeById("qingtong_yinhuo_ding");
        if(slipId.equals("chi_jin_sanyang_ding_yujian"))return recipeById("chi_jin_sanyang_ding");
        if(slipId.equals("crimson_flame_seed_yujian"))return recipeById("crimson_flame_seed");
        if(slipId.equals("dragon_flame_seed_yujian"))return recipeById("dragon_flame_seed");
        if(slipId.endsWith("_yujian")) {
            var higher=HigherWeaponRecipes.recipe(slipId.substring(0,slipId.length()-7));
            if(higher!=null)return higher;
        }
        if (yujian.is(ModItems.FORTUNE_MIRROR_YUJIAN.get())) return recipeById("fortune_mirror");
        if (yujian.is(ModItems.PRECISE_FORTUNE_MIRROR_YUJIAN.get())) return recipeById("precise_fortune_mirror");
        if (yujian.is(ModItems.FORTUNE_GUARD_COIN_YUJIAN.get())) return recipeById("fortune_guard_coin");
        if (yujian.is(ModItems.FORTUNE_LAMP_YUJIAN.get())) return recipeById("fortune_lamp");
        if (yujian.is(ModItems.TRIBULATION_DOLL_YUJIAN.get())) return recipeById("tribulation_doll");
        if (yujian.is(ModItems.PURIFIED_TRIBULATION_DOLL_YUJIAN.get())) return recipeById("purified_tribulation_doll");
        if (yujian.is(ModItems.FEIJIAN_YUJIAN.get())) {
            return recipeById("flying_sword");
        }
        if (yujian.is(ModItems.DAO_YUJIAN.get())) {
            return recipeById("dao");
        }
        if (yujian.is(ModItems.JINGTIE_SPEAR_YUJIAN.get())) {
            return recipeById("jingtie_spear");
        }
        if (yujian.is(ModItems.HEAVY_IRON_BRACER_YUJIAN.get())) {
            return recipeById("heavy_iron_bracer");
        }
        if (yujian.is(ModItems.THROWING_KNIFE_YUJIAN.get())) {
            return recipeById("throwing_knife");
        }
        if (yujian.is(ModItems.FLYING_NEEDLE_YUJIAN.get())) {
            return recipeById("flying_needle");
        }
        if (yujian.is(ModItems.TIEGU_SHAN_YUJIAN.get())) {
            return recipeById("tiegu_shan");
        }
        if (yujian.is(ModItems.FUCHEN_YUJIAN.get())) {
            return recipeById("fuchen");
        }
        if (yujian.is(ModItems.WHIP_YUJIAN.get())) {
            return recipeById("whip");
        }
        if (yujian.is(ModItems.SHIHUNFAN_YUJIAN.get())) {
            return recipeById("shihunfan");
        }
        if (yujian.is(ModItems.ZHENPAN_YUJIAN.get())) {
            return recipeById("zhenpan");
        }
        if (yujian.is(ModItems.BLANK_FORMATION_FLAG_YUJIAN.get())) {
            return recipeById("blank_formation_flag");
        }
        if (yujian.is(ModItems.DURATION_FLAG_YUJIAN.get())) {
            return recipeById("duration_flag");
        }
        if (yujian.is(ModItems.AMPLIFIER_FLAG_YUJIAN.get())) {
            return recipeById("amplifier_flag");
        }
        if (yujian.is(ModItems.SPIRIT_GATHERING_FLAG_YUJIAN.get())) {
            return recipeById("spirit_gathering_flag");
        }
        if (yujian.is(ModItems.HASTE_FLAG_YUJIAN.get())) {
            return recipeById("haste_flag");
        }
        if (yujian.is(ModItems.QI_RETURN_FLAG_YUJIAN.get())) {
            return recipeById("qi_return_flag");
        }
        if (yujian.is(ModItems.QI_POUCH_YUJIAN.get())) {
            return recipeById("qi_pouch");
        }
        if (yujian.is(ModItems.SWORD_CASE_YUJIAN.get())) {
            return recipeById("sword_case");
        }
        if (yujian.is(ModItems.BLOOD_FANG_PENDANT_YUJIAN.get())) {
            return recipeById("blood_fang_pendant");
        }
        if (yujian.is(ModItems.WATER_AVOIDING_PEARL_YUJIAN.get())) {
            return recipeById("water_avoiding_pearl");
        }
        if (yujian.is(ModItems.JADE_PENDANT_YUJIAN.get())) {
            return recipeById("jade_pendant");
        }
        if (yujian.is(ModItems.SHIELD_BREAKER_RING_YUJIAN.get())) {
            return recipeById("shield_breaker_ring");
        }
        if (yujian.is(ModItems.DISGUISE_MASK_YUJIAN.get())) {
            return recipeById("disguise_mask");
        }
        if (yujian.is(ModItems.BLACK_TORTOISE_HEARTGUARD_MIRROR_YUJIAN.get())) {
            return recipeById("black_tortoise_heartguard_mirror");
        }
        if (yujian.is(ModItems.ZHENHUN_BELL_YUJIAN.get())) {
            return recipeById("zhenhun_bell");
        }
        if (yujian.is(ModItems.YINHUN_GONG_YUJIAN.get())) {
            return recipeById("yinhun_gong");
        }
        if (yujian.is(ModItems.FOG_REVEALING_MIRROR_YUJIAN.get())) {
            return recipeById("fog_revealing_mirror");
        }
        if (yujian.is(ModItems.YINYANG_COMPASS_YUJIAN.get())) {
            return recipeById("yinyang_compass");
        }
        if (yujian.is(ModItems.YUANHUN_BOTTLE_YUJIAN.get())) {
            return recipeById("yuanhun_bottle");
        }
        return null;
    }

    public static ForgingRecipe recipeById(String id) {
        if ("qingtong_yinhuo_ding".equals(id)) return new ForgingRecipe(id,
                ModBlocks.QINGTONG_YINHUO_DING.get(), 2500,
                Map.of(ModBlocks.CU_TIE_XIAODING.get().asItem(), 1,
                        ModBlocks.CHITONG_SUI.get().asItem(), 7),
                true, RECIPE_SPIRIT_STONE_COUNT);
        if ("chi_jin_sanyang_ding".equals(id)) return new ForgingRecipe(id,
                ModBlocks.CHI_JIN_SANYANG_DING.get(), 6500,
                Map.of(ModBlocks.QINGTONG_YINHUO_DING.get().asItem(), 1,
                        ModBlocks.CHITONG_SUI.get().asItem(), 4,
                        ModBlocks.LIUJIN_SAND.get().asItem(), 4,
                        ModItems.JINDAN_YAODAN.get(), 3,
                        ModBlocks.JINGTIE_MOTHERSTONE.get().asItem(), 2),
                true, RECIPE_SPIRIT_STONE_COUNT, "zhen_fire");
        if ("crimson_flame_seed".equals(id)) return new ForgingRecipe(id, ModItems.CRIMSON_FLAME_SEED.get(), 2000,
                Map.of(ModItems.CHI_LIN.get(), 1, ModItems.HUO_XI_BLOOD.get(), 1), true, 8, "");
        if ("dragon_flame_seed".equals(id)) return new ForgingRecipe(id, ModItems.DRAGON_FLAME_SEED.get(), 8500,
                Map.of(ModItems.YUANYING_YAODAN.get(), 1, ModItems.YANJIAO_HORN.get(), 1), true, 64, "");
        if(WeaponShardSystem.REFORGE.equals(id))return new ForgingRecipe(id,ModItems.WEAPON_SHARD.get(),3500,Map.of(),true,128,"");
        var alchemy=com.example.immortal_cultivation_mod.alchemy.AlchemyForgeRecipes.recipe(id);
        if(alchemy!=null)return alchemy;
        var higher=HigherWeaponRecipes.recipe(id);
        if(higher!=null)return higher;
        if ("fortune_mirror".equals(id)) {
            return new ForgingRecipe(id, ModItems.FORTUNE_MIRROR.get(), 4800,
                    Map.of(ModBlocks.YUNWEN_SILVER_CRYSTAL.get().asItem(), 3,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.ZHENWEN_JADE.get(), 2,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_STORAGE_CRYSTAL.get(), 2,
                            ModItems.KONGMING_STONE.get(), 2,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_GUIDING_THREAD.get(), 3,
                            ModItems.JINDAN_YAODAN.get(), 2), true, RECIPE_SPIRIT_STONE_COUNT, "ling_fire");
        }
        if ("precise_fortune_mirror".equals(id)) {
            return new ForgingRecipe(id, ModItems.PRECISE_FORTUNE_MIRROR.get(), 6400,
                    Map.of(ModItems.FORTUNE_MIRROR.get(), 1,
                            ModBlocks.STAR_DUST_STONE.get().asItem(), 2,
                            ModBlocks.YUNWEN_SILVER_CRYSTAL.get().asItem(), 2,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_STORAGE_CRYSTAL.get(), 2,
                            ModItems.KONGMING_STONE.get(), 2,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_GUIDING_THREAD.get(), 2,
                            ModItems.YUANYING_YAODAN.get(), 2,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.ZHENWEN_JADE.get(), 1), true, RECIPE_SPIRIT_STONE_COUNT, "ling_fire");
        }
        if ("fortune_guard_coin".equals(id)) {
            return new ForgingRecipe(id, ModItems.FORTUNE_GUARD_COIN.get(), 5200,
                    Map.of(Items.COPPER_INGOT, 64,
                            ModBlocks.LIUJIN_SAND.get().asItem(), 3,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.ZHENWEN_JADE.get(), 3,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_GUIDING_THREAD.get(), 2,
                            ModBlocks.QIANNIAN_LINGMU_HEART.get().asItem(), 3,
                            ModItems.JINDAN_YAODAN.get(), 2), true, RECIPE_SPIRIT_STONE_COUNT, "ling_fire");
        }
        if ("fortune_lamp".equals(id)) {
            return new ForgingRecipe(id, ModBlocks.FORTUNE_LAMP_ITEM.get(), 6000,
                    Map.of(ModBlocks.QIANNIAN_LINGMU_HEART.get().asItem(), 2,
                            ModBlocks.YUNWEN_SILVER_CRYSTAL.get().asItem(), 2,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_STORAGE_CRYSTAL.get(), 2,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.FLOWING_SPIRIT_CRYSTAL.get(), 2,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_GUIDING_THREAD.get(), 3,
                            ModItems.YUANYING_YAODAN.get(), 2,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.ZHENWEN_JADE.get(), 1), true, RECIPE_SPIRIT_STONE_COUNT, "ling_fire");
        }
        if ("tribulation_doll".equals(id) || "purified_tribulation_doll".equals(id)) {
            boolean purified = "purified_tribulation_doll".equals(id);
            // Pictured recipe: beast essence is 128 Blood (two stacks).
            // Twelve purity materials + two Blood stacks + spirit stones = 15 slots.
            return new ForgingRecipe(id,
                    purified ? ModItems.PURIFIED_TRIBULATION_DOLL.get() : ModItems.TRIBULATION_DOLL.get(),
                    purified ? 5200 : 4200,
                    Map.of(ModBlocks.QIANNIAN_LINGMU_HEART.get().asItem(), 1,
                            ModItems.KONGMING_STONE.get(), 2,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_STORAGE_CRYSTAL.get(), 2,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.ZHENWEN_JADE.get(), 2,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_GUIDING_THREAD.get(), purified ? 1 : 4,
                            ModItems.BLOOD.get(), 128,
                            ModItems.JINDAN_YAODAN.get(), purified ? 4 : 1),
                    true, RECIPE_SPIRIT_STONE_COUNT, "ling_fire");
        }
        if ("flying_sword".equals(id)) {
            return new ForgingRecipe(
                    "flying_sword",
                    ModItems.SWORD_MOLD.get(),
                    3500,
                    Map.of(
                            ModBlocks.BISUI_STONE.get().asItem(), 3,
                            ModBlocks.JINGTIE_MOTHERSTONE.get().asItem(), 2
                    ));
        }
        if ("dao".equals(id)) {
            return new ForgingRecipe(
                    "dao",
                    ModItems.SABER_MOLD.get(),
                    3500,
                    Map.of(
                            ModBlocks.JINGTIE_MOTHERSTONE.get().asItem(), 2,
                            ModBlocks.MOXUAN_IRON.get().asItem(), 3
                    ));
        }
        if ("jingtie_spear".equals(id)) {
            return new ForgingRecipe(
                    "jingtie_spear",
                    ModItems.SPEAR_MOLD.get(),
                    3500,
                    Map.of(
                            ModBlocks.MOXUAN_IRON.get().asItem(), 3,
                            ModBlocks.JINGTIE_MOTHERSTONE.get().asItem(), 3
                    ));
        }
        if ("heavy_iron_bracer".equals(id)) {
            return new ForgingRecipe(
                    "heavy_iron_bracer",
                    ModItems.WEAPON_MOLD.get(),
                    3500,
                    Map.of(
                            ModBlocks.JINGTIE_MOTHERSTONE.get().asItem(), 1,
                            ModBlocks.MOXUAN_IRON.get().asItem(), 4
                    ));
        }
        if ("throwing_knife".equals(id)) {
            return new ForgingRecipe(
                    "throwing_knife",
                    ModItems.WEAPON_MOLD.get(),
                    3500,
                    Map.of(
                            ModBlocks.MOXUAN_IRON.get().asItem(), 2,
                            ModBlocks.JINGTIE_MOTHERSTONE.get().asItem(), 1
                    ));
        }
        if ("flying_needle".equals(id)) {
            return new ForgingRecipe(
                    "flying_needle",
                    ModItems.WEAPON_MOLD.get(),
                    3500,
                    Map.of(
                            ModBlocks.JINGTIE_MOTHERSTONE.get().asItem(), 1,
                            ModBlocks.MOXUAN_IRON.get().asItem(), 1
                    ));
        }
        if ("tiegu_shan".equals(id)) {
            return new ForgingRecipe(
                    "tiegu_shan",
                    ModItems.WEAPON_MOLD.get(),
                    3500,
                    Map.of(
                            ModBlocks.JINGTIE_MOTHERSTONE.get().asItem(), 2,
                            ModBlocks.MOXUAN_IRON.get().asItem(), 2
                    ));
        }
        if ("fuchen".equals(id)) {
            return new ForgingRecipe(
                    "fuchen",
                    ModItems.WEAPON_MOLD.get(),
                    2200,
                    Map.of(
                            ModBlocks.QIANNIAN_LINGMU_HEART.get().asItem(), 1,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_GUIDING_THREAD.get(), 3
                    ));
        }
        if ("whip".equals(id)) {
            return new ForgingRecipe(
                    "whip",
                    ModItems.WEAPON_MOLD.get(),
                    2400,
                    Map.of(
                            ModBlocks.ZIYUN_VINE.get().asItem(), 3,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_GUIDING_THREAD.get(), 2,
                            ModBlocks.JINGTIE_MOTHERSTONE.get().asItem(), 1
                    ));
        }
        if ("shihunfan".equals(id)) {
            return new ForgingRecipe(
                    "shihunfan",
                    ModItems.SHIHUNFAN.get(),
                    3200,
                    Map.of(
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.PATTERNED_CLOTH.get(), 2,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.LINGMU_FLAGPOLE.get(), 1,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_GUIDING_THREAD.get(), 2,
                            ModBlocks.SHIHUN_GRASS.get().asItem(), 1,
                            ModItems.BLOOD_CRYSTAL.get(), 1,
                            ModBlocks.HANPO_JADE.get().asItem(), 1,
                            ModItems.YUANYING_YAODAN.get(), 1,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.HIGH_SPIRIT_STONE.get(), 1
                    ),
                    true,
                    0,
                    "ling_fire");
        }
        if ("zhenpan".equals(id)) {
            return new ForgingRecipe(
                    "zhenpan",
                    com.example.immortal_cultivation_mod.magicritual.item.ModItems.ZHENPAN.get(),
                    1800,
                    Map.of(
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.ZHENWEN_JADE.get(), 3,
                            ModBlocks.JINGTIE_MOTHERSTONE.get().asItem(), 1,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_STORAGE_CRYSTAL.get(), 1,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.MEDIUM_SPIRIT_STONE.get(), 1
                    ),
                    true,
                    0);
        }
        if ("blank_formation_flag".equals(id)) {
            return new ForgingRecipe(
                    "blank_formation_flag",
                    com.example.immortal_cultivation_mod.magicritual.item.ModItems.BLANK_FORMATION_FLAG.get(),
                    1600,
                    Map.of(
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.PATTERNED_CLOTH.get(), 2,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_GUIDING_THREAD.get(), 2,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.ZHENWEN_JADE.get(), 1,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.LINGMU_FLAGPOLE.get(), 1
                    ),
                    true,
                    0);
        }
        if ("duration_flag".equals(id)) {
            return new ForgingRecipe(
                    "duration_flag",
                    com.example.immortal_cultivation_mod.magicritual.item.ModItems.DURATION_FLAG.get(),
                    1800,
                    Map.of(
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.BLANK_FORMATION_FLAG.get(), 1,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_STORAGE_CRYSTAL.get(), 2,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.ZHENWEN_JADE.get(), 1
                    ),
                    true,
                    0);
        }
        if ("amplifier_flag".equals(id)) {
            return new ForgingRecipe(
                    "amplifier_flag",
                    com.example.immortal_cultivation_mod.magicritual.item.ModItems.AMPLIFIER_FLAG.get(),
                    2200,
                    Map.of(
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.BLANK_FORMATION_FLAG.get(), 1,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.FLOWING_SPIRIT_CRYSTAL.get(), 2,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.ZHENWEN_JADE.get(), 2
                    ),
                    true,
                    0);
        }
        if ("spirit_gathering_flag".equals(id)) {
            return new ForgingRecipe(
                    "spirit_gathering_flag",
                    com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_GATHERING_FLAG.get(),
                    2100,
                    Map.of(
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.BLANK_FORMATION_FLAG.get(), 1,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_STORAGE_CRYSTAL.get(), 1,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.FLOWING_SPIRIT_CRYSTAL.get(), 1,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.ZHENWEN_JADE.get(), 2
                    ),
                    true,
                    0);
        }
        if ("haste_flag".equals(id)) {
            return new ForgingRecipe(
                    "haste_flag",
                    com.example.immortal_cultivation_mod.magicritual.item.ModItems.HASTE_FLAG.get(),
                    2100,
                    Map.of(
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.BLANK_FORMATION_FLAG.get(), 1,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.FLOWING_SPIRIT_CRYSTAL.get(), 2,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_GUIDING_THREAD.get(), 2
                    ),
                    true,
                    0);
        }
        if ("qi_return_flag".equals(id)) {
            return new ForgingRecipe(
                    "qi_return_flag",
                    com.example.immortal_cultivation_mod.magicritual.item.ModItems.QI_RETURN_FLAG.get(),
                    1900,
                    Map.of(
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.BLANK_FORMATION_FLAG.get(), 1,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_STORAGE_CRYSTAL.get(), 2,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_GUIDING_THREAD.get(), 1,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.ZHENWEN_JADE.get(), 1
                    ),
                    true,
                    0);
        }
        if ("qi_pouch".equals(id)) {
            return new ForgingRecipe(
                    "qi_pouch",
                    ModItems.QI_POUCH.get(),
                    1800,
                    Map.of(
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.PATTERNED_CLOTH.get(), 2,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_GUIDING_THREAD.get(), 2,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.ZHENWEN_JADE.get(), 1,
                            ModItems.KONGMING_STONE.get(), 1,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_STORAGE_CRYSTAL.get(), 1,
                            ModItems.LINGMU_CLASP.get(), 1
                    ),
                    true,
                    0);
        }
        if ("zhenhun_bell".equals(id)) {
            return new ForgingRecipe(
                    "zhenhun_bell",
                    ModItems.ZHENHUN_BELL.get(),
                    2400,
                    Map.of(
                            ModBlocks.JINGTIE_MOTHERSTONE.get().asItem(), 1,
                            ModBlocks.YUNWEN_SILVER_CRYSTAL.get().asItem(), 2,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.ZHENWEN_JADE.get(), 1,
                            ModItems.BLOOD_CRYSTAL.get(), 4
                    ),
                    true,
                    1,
                    "ling_fire");
        }
        if ("yinhun_gong".equals(id)) {
            return new ForgingRecipe(
                    "yinhun_gong",
                    ModItems.YINHUN_GONG.get(),
                    3200,
                    Map.of(
                            ModBlocks.MOXUAN_IRON.get().asItem(), 1,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_GUIDING_THREAD.get(), 1,
                            ModItems.BLOOD_CRYSTAL.get(), 2
                    ),
                    true,
                    0,
                    "ling_fire");
        }
        if ("fog_revealing_mirror".equals(id)) {
            return new ForgingRecipe(
                    "fog_revealing_mirror",
                    ModItems.FOG_REVEALING_MIRROR.get(),
                    2200,
                    Map.of(
                            ModBlocks.YUNWEN_SILVER_CRYSTAL.get().asItem(), 2,
                            ModBlocks.HANPO_JADE.get().asItem(), 1,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.ZHENWEN_JADE.get(), 1,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_STORAGE_CRYSTAL.get(), 1
                    ),
                    true,
                    0,
                    "ling_fire");
        }
        if ("yinyang_compass".equals(id)) {
            return new ForgingRecipe(
                    "yinyang_compass",
                    ModItems.YINYANG_COMPASS.get(),
                    2800,
                    Map.of(
                            ModBlocks.MOXUAN_IRON.get().asItem(), 1,
                            ModBlocks.HANPO_JADE.get().asItem(), 1,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.ZHENWEN_JADE.get(), 2,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_STORAGE_CRYSTAL.get(), 1
                    ),
                    true,
                    0,
                    "ling_fire");
        }
        if ("yuanhun_bottle".equals(id)) {
            return new ForgingRecipe(
                    "yuanhun_bottle",
                    ModItems.YUANHUN_BOTTLE.get(),
                    1800,
                    Map.of(
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_STORAGE_CRYSTAL.get(), 1,
                            ModBlocks.HANPO_JADE.get().asItem(), 1,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_GUIDING_THREAD.get(), 1,
                            ModItems.BLOOD_CRYSTAL.get(), 2
                    ),
                    true,
                    0,
                    "ling_fire");
        }
        if ("blood_fang_pendant".equals(id)) {
            return new ForgingRecipe(
                    "blood_fang_pendant",
                    ModItems.BLOOD_FANG_PENDANT.get(),
                    2600,
                    Map.of(
                            ModItems.BLOOD_CRYSTAL.get(), 2,
                            ModItems.XUESHA_HIDE.get(), 1,
                            ModItems.RED_STRING.get(), 1,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_GUIDING_THREAD.get(), 1,
                            ModItems.YANJIAO_HORN.get(), 1,
                            ModItems.HUO_XI_BLOOD.get(), 1,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.MEDIUM_SPIRIT_STONE.get(), 1
                    ),
                    true,
                    0,
                    "ling_fire");
        }
        if ("water_avoiding_pearl".equals(id)) {
            return new ForgingRecipe(
                    "water_avoiding_pearl",
                    ModItems.WATER_AVOIDING_PEARL.get(),
                    2000,
                    Map.of(
                            ModBlocks.BISUI_STONE.get().asItem(), 2,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.FLOWING_SPIRIT_CRYSTAL.get(), 1,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.ZHENWEN_JADE.get(), 1,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_GUIDING_THREAD.get(), 1
                    ),
                    true,
                    4,
                    "ling_fire");
        }
        if ("jade_pendant".equals(id)) {
            return new ForgingRecipe(
                    "jade_pendant",
                    ModItems.JADE_PENDANT.get(),
                    3600,
                    Map.of(
                            ModBlocks.HANPO_JADE.get().asItem(), 1,
                            ModItems.KONGMING_STONE.get(), 1,
                            ModBlocks.QIANNIAN_LINGMU_HEART.get().asItem(), 1,
                            ModBlocks.JIUYE_XIANLAN.get().asItem(), 1,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.ZHENWEN_JADE.get(), 1,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.FLOWING_SPIRIT_CRYSTAL.get(), 1,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_GUIDING_THREAD.get(), 1,
                            ModBlocks.YUNWEN_SILVER_CRYSTAL.get().asItem(), 1,
                            ModItems.JINDAN_YAODAN.get(), 1,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.HIGH_SPIRIT_STONE.get(), 1
                    ),
                    true,
                    0,
                    "ling_fire");
        }
        if ("shield_breaker_ring".equals(id)) {
            return new ForgingRecipe(
                    "shield_breaker_ring",
                    ModItems.SHIELD_BREAKER_RING.get(),
                    3000,
                    Map.of(
                            ModBlocks.MOXUAN_IRON.get().asItem(), 2,
                            ModBlocks.YUNWEN_SILVER_CRYSTAL.get().asItem(), 1,
                            ModBlocks.JINGTIE_MOTHERSTONE.get().asItem(), 1,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.ZHENWEN_JADE.get(), 1,
                            ModItems.IRON_SCALE_PLATE.get(), 1,
                            ModItems.JINDAN_YAODAN.get(), 1,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.MEDIUM_SPIRIT_STONE.get(), 1
                    ),
                    true,
                    0,
                    "ling_fire");
        }
        if ("disguise_mask".equals(id)) {
            return new ForgingRecipe(
                    "disguise_mask",
                    ModItems.DISGUISE_MASK.get(),
                    3200,
                    Map.of(
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.PATTERNED_CLOTH.get(), 1,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_GUIDING_THREAD.get(), 1,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.ZHENWEN_JADE.get(), 1,
                            ModItems.KONGMING_STONE.get(), 1,
                            ModItems.PHANTOM_VEIL_FUR.get(), 1,
                            ModItems.RED_STRING.get(), 1,
                            ModItems.BLACK_WOOL_STRIP.get(), 1,
                            ModBlocks.HANPO_JADE.get().asItem(), 1,
                            ModItems.YUANYING_YAODAN.get(), 1,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.HIGH_SPIRIT_STONE.get(), 1
                    ),
                    true,
                    0,
                    "ling_fire");
        }
        if ("black_tortoise_heartguard_mirror".equals(id)) {
            return new ForgingRecipe(
                    "black_tortoise_heartguard_mirror",
                    ModItems.BLACK_TORTOISE_HEARTGUARD_MIRROR.get(),
                    2800,
                    Map.of(
                            ModItems.INK_ARMOR_PLATE.get(), 2,
                            ModItems.PROFOUND_IRON_ARMOR_CORE.get(), 1,
                            ModBlocks.MOXUAN_IRON.get().asItem(), 1,
                            ModBlocks.HANPO_JADE.get().asItem(), 1,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.ZHENWEN_JADE.get(), 1,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.FLOWING_SPIRIT_CRYSTAL.get(), 1,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_GUIDING_THREAD.get(), 1,
                            ModItems.ZHUJI_YAODAN.get(), 1,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.MEDIUM_SPIRIT_STONE.get(), 1
                    ),
                    true,
                    0,
                    "ling_fire");
        }
        if ("sword_case".equals(id)) {
            return new ForgingRecipe(
                    "sword_case",
                    ModItems.SWORD_CASE.get(),
                    3600,
                    Map.of(
                            ModBlocks.JINGTIE_MOTHERSTONE.get().asItem(), 1,
                            ModBlocks.QIANNIAN_LINGMU_HEART.get().asItem(), 1,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_GUIDING_THREAD.get(), 1,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.ZHENWEN_JADE.get(), 1,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.FLOWING_SPIRIT_CRYSTAL.get(), 1,
                            ModItems.KONGMING_STONE.get(), 1,
                            ModItems.LINGMU_CLASP.get(), 1,
                            ModBlocks.MOXUAN_IRON.get().asItem(), 1,
                            ModBlocks.YUNWEN_SILVER_CRYSTAL.get().asItem(), 1,
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.HIGH_SPIRIT_STONE.get(), 1
                    ),
                    true,
                    0,
                    "ling_fire");
        }
        return null;
    }

    public static ItemLike resultItem(ForgingRecipe recipe) {
        if (recipe == null) {
            return Items.AIR;
        }
        if(HigherWeaponSpec.find(recipe.id())!=null)return HigherWeaponRecipes.item(recipe.id());
        if (recipe.directOutput()) {
            return recipe.moldItem();
        }
        return switch (recipe.id()) {
            case "flying_sword" -> ModItems.FLYING_SWORD.get();
            case "dao" -> ModItems.DAO.get();
            case "jingtie_spear" -> ModItems.JINGTIE_SPEAR.get();
            case "heavy_iron_bracer" -> ModItems.HEAVY_IRON_BRACER.get();
            case "throwing_knife" -> ModItems.THROWING_KNIFE.get();
            case "flying_needle" -> ModItems.FLYING_NEEDLE.get();
            case "tiegu_shan" -> ModItems.TIEGU_SHAN.get();
            case "fuchen" -> ModItems.FUCHEN.get();
            case "whip" -> ModItems.WHIP.get();
            default -> Items.AIR;
        };
    }

    /** Native weapon rank is fixed by its item type, not by later restriction breakthroughs. */
    private static int originalWeaponTemperature(String id, int fallback) {
        HigherWeaponSpec spec = HigherWeaponSpec.find(id);
        int realm = spec != null ? spec.realm : 0;
        return realm == 0 ? fallback : Math.clamp(4500 + (realm - 2) * 2000, 0, MAX_TEMP);
    }

    /** Reforging shares the original weapon's temperature even after its restriction tier changes. */
    public static int targetTemperature(ForgingRecipe recipe, List<ItemStack> materials) {
        if (recipe == null) return 0;
        if (WeaponShardSystem.REFORGE.equals(recipe.id())) {
            for (ItemStack material : materials) {
                if (!WeaponShardSystem.populated(material)) continue;
                // The serialized weapon is the source of truth; the shard's item label is display data.
                ResourceLocation original = ResourceLocation.tryParse(
                        WeaponShardSystem.data(material).getCompound("weapon").getString("id"));
                if (original != null && ImmortalCultivationMod.MODID.equals(original.getNamespace())) {
                    ForgingRecipe originalRecipe = recipeById(original.getPath());
                    if (originalRecipe != null && !WeaponShardSystem.REFORGE.equals(originalRecipe.id())) {
                        return originalRecipe.averageTemp();
                    }
                }
                break;
            }
        }
        return recipe.averageTemp();
    }

    public static boolean canUseFire(ForgingRecipe recipe, String fireType) {
        return canUseFire(recipe, fireType, List.of());
    }

    /** Every recipe accepts every supported heat source; temperature still determines quality. */
    public static boolean canUseFire(ForgingRecipe recipe, String fireType, List<ItemStack> materials) {
        return recipe != null;
    }

    /** Compatibility accessor: no recipe or shard rank requires a particular flame anymore. */
    public static boolean requiresHeavenlyFire(ForgingRecipe recipe, List<ItemStack> materials) {
        return false;
    }

    public static int qualityCeiling(String fireType) {
        return FlameKind.fromId(fireType) == FlameKind.HEAVENLY ? 125 : 100;
    }

    public static boolean hasRequiredMaterials(ForgingRecipe recipe, List<ItemStack> inputs) {
        if (recipe == null) {
            return false;
        }
        if(WeaponShardSystem.REFORGE.equals(recipe.id()))return WeaponShardSystem.canReforge(inputs);
        if(!WeaponShardSystem.validAdditives(inputs))return false;
        if(com.example.immortal_cultivation_mod.alchemy.AlchemyForgeRecipes.isPill(recipe.id()) && !com.example.immortal_cultivation_mod.alchemy.AlchemyChemistry.matches(
                com.example.immortal_cultivation_mod.alchemy.AlchemyChemistry.formula(recipe.id().substring(8)),com.example.immortal_cultivation_mod.alchemy.AlchemyForgeRecipes.herbs(inputs)))return false;
        Map<Item, Integer> counts = new LinkedHashMap<>();
        for (ItemStack stack : inputs) {
            if (stack.isEmpty()) {
                continue;
            }
            int available = canStackInForgingRecipe(stack) ? stack.getCount() : 1;
            counts.merge(stack.getItem(), available, Integer::sum);
        }
        for (var required : recipe.required().entrySet()) {
            if (counts.getOrDefault(required.getKey(), 0) < required.getValue()) {
                return false;
            }
        }
        int spiritStones = 0;
        for (Map.Entry<Item, Integer> available : counts.entrySet()) {
            ItemStack sample = new ItemStack(available.getKey());
            if (!isRecipeSpiritStone(sample)) {
                continue;
            }
            int reservedForExactIngredient =
                    recipe.required().getOrDefault(available.getKey(), 0);
            spiritStones += Math.max(0, available.getValue() - reservedForExactIngredient);
        }
        return spiritStones >= recipe.spiritStoneCount();
    }

    public static boolean isIngredientForRecipe(ForgingRecipe recipe, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (recipe == null) {
            return isForgeMaterial(stack)||WeaponShardSystem.populated(stack)||isRecipeSpiritStone(stack);
        }
        if(WeaponShardSystem.REFORGE.equals(recipe.id()))return WeaponShardSystem.populated(stack)||isRecipeSpiritStone(stack);
        if(WeaponShardSystem.populated(stack))return true;
        return com.example.immortal_cultivation_mod.alchemy.AlchemyForgeRecipes.isPill(recipe.id()) && com.example.immortal_cultivation_mod.alchemy.AlchemyMenu.plantId(stack)!=null
                || recipe.required().containsKey(stack.getItem())
                || recipe.spiritStoneCount() > 0 && isRecipeSpiritStone(stack)
                || !recipe.directOutput() && isForgeMaterial(stack);
    }

    public static boolean isRecipeSpiritStone(ItemStack stack) {
        return com.example.immortal_cultivation_mod.magicritual.item.ModItems
                .isSpiritStone(stack.getItem())
                && !stack.is(com.example.immortal_cultivation_mod.magicritual.item.ModItems
                .INFINITE_SPIRIT_STONE.get());
    }

    /**
     * Ingredients without an individual purity value may supply several recipe
     * units from one physical ding slot. Spirit stones are deliberately included:
     * each tier has one fixed purity, so stacking identical stones loses no data.
     */
    public static boolean canStackInForgingRecipe(ItemStack stack) {
        if (stack.isEmpty() || stack.getMaxStackSize() <= 1) {
            return false;
        }
        if (isRecipeSpiritStone(stack)) {
            return true;
        }
        return !isPurityMaterial(stack)
                && !forgingData(stack).contains("average_purity");
    }

    public static int meltTicks(List<ItemStack> inputs) {
        int longest = 20;
        for (ItemStack stack : inputs) {
            if (stack.isEmpty()) {
                continue;
            }
            if (isReusableAdditive(stack)) {
                continue;
            }
            longest = Math.max(longest, meltTicksForStack(stack));
        }
        return longest;
    }

    public static int meltTicksForStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return 20;
        }
        return Math.max(20, (101 - purity(stack)) * 20);
    }

    /**
     * The weighted average material purity is also the forge success percentage.
     * Each spirit-stone stack contributes one unit, regardless of its stone count.
     * A 100-purity batch is guaranteed; a 1-purity batch has a 1% chance.
     */
    public static double forgingSuccessChancePercent(List<ItemStack> materials) {
        return Math.max(1.0D, Math.min(100.0D, puritySummary(materials).average()));
    }

    public static ItemStack createMold(ForgingRecipe recipe, List<ItemStack> materials, double averageTemp, String fireType) {
        return createMold(recipe, materials, averageTemp, fireType, targetTemperature(recipe, materials));
    }

    /** A running batch keeps the target it saved, including when recipe defaults change after an update. */
    public static ItemStack createMold(ForgingRecipe recipe, List<ItemStack> materials, double averageTemp,
                                      String fireType, int targetTemperature) {
        return createMold(recipe, materials, averageTemp, fireType, targetTemperature, RandomSource.create());
    }

    public static ItemStack createMold(ForgingRecipe recipe, List<ItemStack> materials, double averageTemp,
                                      String fireType, int targetTemperature, RandomSource random) {
        ItemStack mold = com.example.immortal_cultivation_mod.alchemy.AlchemyForgeRecipes.isPill(recipe.id()) ? com.example.immortal_cultivation_mod.alchemy.AlchemyForgeRecipes.output(recipe.id(),materials) : new ItemStack(recipe.moldItem());
        int quality = quality(targetTemperature, averageTemp, materials);
        FlameKind flame = FlameKind.fromId(fireType);
        if (flame == FlameKind.EARTH) quality = Math.min(100, quality + 5);
        boolean pill = com.example.immortal_cultivation_mod.alchemy.AlchemyForgeRecipes.isPill(recipe.id());
        if (flame == FlameKind.HEAVENLY && !pill) quality = Math.min(qualityCeiling(fireType), quality + 25);
        CompoundTag data = new CompoundTag();
        data.putInt("schema_version", 2);
        data.putString("recipe", recipe.id());
        data.putInt("target_temp", targetTemperature);
        data.putDouble("average_temp", averageTemp);
        data.putInt("quality", quality);
        data.putString("quality_name", qualityName(quality));
        data.putString("fire", fireType == null || fireType.isBlank() ? "normal_fire" : fireType);
        if (quality > 100) data.putInt("flame_heavenly_quality_percent", quality - 100);
        CompoundTag materialCounts = new CompoundTag();
        Map<Item, Integer> ingredientCounts = new LinkedHashMap<>();
        PuritySummary purity = puritySummary(materials);
        for (ItemStack stack : materials) {
            if (stack.isEmpty()) {
                continue;
            }
            ingredientCounts.merge(stack.getItem(), stack.getCount(), Integer::sum);

            MaterialDef material = material(stack);
            if (recipe.directOutput() || material == null) {
                continue;
            }
            materialCounts.putInt(material.id(), materialCounts.getInt(material.id()) + stack.getCount());
            data.putInt(material.statKey(), data.getInt(material.statKey()) + stack.getCount());
        }
        data.put("materials", materialCounts);
        data.put("ingredients", ingredientList(ingredientCounts));
        data.putInt("purity_weight", (int)Math.min(Integer.MAX_VALUE, purity.weight()));
        data.putDouble("average_purity", purity.average());
        applyFlameBonuses(data, recipe, flame, random);
        CompoundTag tag = mold.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.put(TAG_FORGING, data);
        mold.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        if ("disguise_mask".equals(recipe.id())) {
            DisguiseMaskItem.setDisguiseRealm(
                    mold, weakestPhantomVeilFurRealm(materials));
        }
        WeaponShardSystem.inherit(mold,materials);
        if (recipe.id().startsWith("legacy_alchemy/")) {
            com.example.immortal_cultivation_mod.alchemy.FiveElementBalance.apply(mold,
                    com.example.immortal_cultivation_mod.alchemy.FiveElementBalance.totals(materials));
        }
        return mold;
    }

    private static void applyFlameBonuses(CompoundTag data, ForgingRecipe recipe, FlameKind flame, RandomSource random) {
        if (com.example.immortal_cultivation_mod.alchemy.AlchemyForgeRecipes.isPill(recipe.id())) return;
        switch (flame) {
            case CRIMSON -> {
                addStat(data, "flame_durability_percent", 5);
                addStat(data, "fire_storage", 1);
                addStat(data, "qi_damage_percent", 5);
            }
            case EARTH -> addStat(data, "quality_bonus_percent", 1);
            case WOOD -> {
                if (woodFlameCompatible(recipe)) data.putInt("material_loss_reduction_percent", 25);
            }
            case SOLAR -> {
                if (solarFlameCompatible(recipe)) {
                    addStat(data, "armor_pen_percent", 5);
                    addStat(data, "flame_sharpness_percent", 5);
                }
            }
            case NETHER -> {
                addStat(data, "flame_spirit_damage_percent", 5);
                addStat(data, "flame_soul_damage", 2);
            }
            case THUNDER -> {
                addStat(data, "tribulation_resistance_percent", 15);
                if (random.nextFloat() < .25F) addStat(data, "flame_lightning_damage_percent", 5);
            }
            case DRAGON -> {
                addStat(data, "qi_damage_percent", 20);
                addStat(data, "flame_fire_affinity_percent", 35);
                addStat(data, "fire_storage", 3);
                addStat(data, "flame_qi_burn_chance_percent", 25);
            }
            default -> {}
        }
    }

    private static void addStat(CompoundTag data, String key, int amount) {
        data.putInt(key, data.getInt(key) + amount);
    }

    /** Structural recipe material determines affinity; precious fittings do not turn a wooden weapon into metal. */
    public static boolean solarFlameCompatible(ForgingRecipe recipe) {
        if (recipe == null || !WeaponRestrictionSystem.isWeapon(new ItemStack(resultItem(recipe)))) return false;
        HigherWeaponSpec spec = HigherWeaponSpec.find(recipe.id());
        if (spec == HigherWeaponSpec.THUNDERWOOD_SWORD || spec == HigherWeaponSpec.SPIRIT_SILK_WHIP
                || spec == HigherWeaponSpec.WIND_FEATHER_FAN) return false;
        if (recipe.id().contains("wood") || recipe.id().contains("bamboo")) return false;
        long metal = 0, organic = 0;
        for (var entry : recipe.required().entrySet()) {
            String material = BuiltInRegistries.ITEM.getKey(entry.getKey()).getPath();
            int units = entry.getValue();
            if (material.equals("jingtie_motherstone") || material.equals("moxuan_iron")
                    || material.equals("chitong_sui") || material.endsWith("_ingot")
                    || material.endsWith("_nugget") || material.equals("iron_block")
                    || material.equals("gold_block") || material.equals("copper_block")) metal += units;
            if (material.contains("wood") || material.contains("lingmu") || material.contains("bamboo")
                    || material.contains("vine") || material.contains("silk") || material.contains("cloth")
                    || material.contains("feather") || material.contains("thread")) organic += units;
        }
        return metal > 0 && metal > organic;
    }

    /** Wood flames protect organic batches and bows, fans and staffs from material loss. */
    public static boolean woodFlameCompatible(ForgingRecipe recipe) {
        if (recipe == null || com.example.immortal_cultivation_mod.alchemy.AlchemyForgeRecipes.isPill(recipe.id())) return false;
        String id = recipe.id();
        if (id.contains("wood") || id.contains("bow") || id.contains("fan") || id.contains("shan")
                || id.contains("staff") || id.equals("fuchen")) return true;
        return recipe.required().keySet().stream().anyMatch(item -> {
            String path = BuiltInRegistries.ITEM.getKey(item).getPath();
            return path.contains("wood") || path.contains("lingmu") || path.contains("bamboo");
        });
    }

    /** Returned units are real ingredients already consumed by a failed eligible batch. */
    public static int woodFlameSalvageCount(ForgingRecipe recipe, String fireType, ItemStack consumed) {
        if (FlameKind.fromId(fireType) != FlameKind.WOOD || !woodFlameCompatible(recipe)
                || consumed.isEmpty() || isRecipeSpiritStone(consumed) || WeaponShardSystem.populated(consumed)) return 0;
        return (consumed.getCount() + 3) / 4;
    }

    private static String weakestPhantomVeilFurRealm(
            List<ItemStack> materials) {
        String weakestRealm = PhantomVeilFurItem.DEFAULT_REALM;
        int weakestIndex = Integer.MAX_VALUE;
        boolean foundFur = false;
        for (ItemStack stack : materials) {
            if (!stack.is(ModItems.PHANTOM_VEIL_FUR.get())) {
                continue;
            }
            int realmIndex = PhantomVeilFurItem.realmIndex(stack);
            if (!foundFur || realmIndex < weakestIndex) {
                weakestRealm = PhantomVeilFurItem.realm(stack);
                weakestIndex = realmIndex;
                foundFur = true;
            }
        }
        return weakestRealm;
    }

    public static ItemStack temper(ItemStack mold) {
        if (!mold.is(ModItems.SWORD_MOLD.get()) && !mold.is(ModItems.SABER_MOLD.get())
                && !mold.is(ModItems.SPEAR_MOLD.get()) && !mold.is(ModItems.WEAPON_MOLD.get())) {
            return ItemStack.EMPTY;
        }
        CompoundTag tag = mold.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag forging = tag.getCompound(TAG_FORGING);
        String recipe = forging.getString("recipe");
        if (!"flying_sword".equals(recipe) && !"dao".equals(recipe)
                && !"jingtie_spear".equals(recipe)
                && !"heavy_iron_bracer".equals(recipe)
                && !"throwing_knife".equals(recipe)
                && !"flying_needle".equals(recipe)
                && !"tiegu_shan".equals(recipe)
                && !"fuchen".equals(recipe)
                && !"whip".equals(recipe) && HigherWeaponSpec.find(recipe)==null) {
            return ItemStack.EMPTY;
        }
        forging.putString("tempering", "cool_water");
        tag.put(TAG_FORGING, forging);
        ItemStack weapon = new ItemStack(HigherWeaponSpec.find(recipe)!=null?HigherWeaponRecipes.item(recipe):switch (recipe) {
            case "dao" -> ModItems.DAO.get();
            case "jingtie_spear" -> ModItems.JINGTIE_SPEAR.get();
            case "heavy_iron_bracer" -> ModItems.HEAVY_IRON_BRACER.get();
            case "throwing_knife" -> ModItems.THROWING_KNIFE.get();
            case "flying_needle" -> ModItems.FLYING_NEEDLE.get();
            case "tiegu_shan" -> ModItems.TIEGU_SHAN.get();
            case "fuchen" -> ModItems.FUCHEN.get();
            case "whip" -> ModItems.WHIP.get();
            default -> ModItems.FLYING_SWORD.get();
        });
        weapon.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        applyForgedDurability(weapon);
        WeaponRestrictionSystem.finalizeInheritedLayers(weapon);
        return weapon;
    }

    public static Component moldTooltip(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(TAG_FORGING)) {
            return Component.empty();
        }
        CompoundTag forging = tag.getCompound(TAG_FORGING);
        return Component.translatable("tooltip." + ImmortalCultivationMod.MODID + ".mold_stats",
                qualityComponent(forging.getInt("quality")),
                format(forging.getDouble("average_temp")),
                format(forging.getDouble("average_purity")));
    }

    public static Component forgedWeaponTooltip(ItemStack stack) {
        CompoundTag forging = forgingData(stack);
        if (forging.isEmpty()) {
            return Component.empty();
        }
        return Component.translatable("tooltip." + ImmortalCultivationMod.MODID + ".forged_weapon_summary",
                recipeDisplayName(forging.getString("recipe")),
                qualityComponent(forging.getInt("quality")));
    }

    public static boolean hasForgingData(ItemStack stack) {
        return !stack.isEmpty() && stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().contains(TAG_FORGING);
    }

    public static boolean isForgedWeapon(ItemStack stack) {
        return (stack.getItem() instanceof HigherWeaponItem || stack.is(ModItems.FLYING_SWORD.get()) || stack.is(ModItems.DAO.get())
                || stack.is(ModItems.JINGTIE_SPEAR.get())
                || stack.is(ModItems.THROWING_KNIFE.get())
                || stack.is(ModItems.FLYING_NEEDLE.get())
                || stack.is(ModItems.TIEGU_SHAN.get())
                || stack.is(ModItems.FUCHEN.get())
                || stack.is(ModItems.WHIP.get())
                || stack.is(ModItems.HEAVY_IRON_BRACER.get())) && hasForgingData(stack);
    }

    public static boolean isForgedRitualArtifact(ItemStack stack) {
        return (stack.is(com.example.immortal_cultivation_mod.magicritual.item.ModItems.ZHENPAN.get())
                || stack.is(ModItems.QI_POUCH.get())
                || stack.is(ModItems.SHIHUNFAN.get())
                || stack.is(ModItems.BLOOD_FANG_PENDANT.get())
                || stack.is(ModItems.WATER_AVOIDING_PEARL.get())
                || stack.is(ModItems.JADE_PENDANT.get())
                || stack.is(ModItems.SHIELD_BREAKER_RING.get())
                || stack.is(ModItems.DISGUISE_MASK.get())
                || stack.is(ModItems.BLACK_TORTOISE_HEARTGUARD_MIRROR.get())
                || stack.is(com.example.immortal_cultivation_mod.magicritual.item.ModItems.BLANK_FORMATION_FLAG.get())
                || stack.is(com.example.immortal_cultivation_mod.magicritual.item.ModItems.DURATION_FLAG.get())
                || stack.is(com.example.immortal_cultivation_mod.magicritual.item.ModItems.AMPLIFIER_FLAG.get())
                || stack.is(com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_GATHERING_FLAG.get())
                || stack.is(com.example.immortal_cultivation_mod.magicritual.item.ModItems.HASTE_FLAG.get())
                || stack.is(com.example.immortal_cultivation_mod.magicritual.item.ModItems.QI_RETURN_FLAG.get()))
                && hasForgingData(stack);
    }

    public static boolean isFinishedForgedItem(ItemStack stack) {
        return isForgedWeapon(stack) || isForgedRitualArtifact(stack);
    }

    public static List<Component> forgedBuffTooltips(ItemStack stack) {
        List<Component> details = forgedDetailTooltips(stack);
        if (!details.isEmpty()) {
            return details;
        }
        return List.of();
    }

    public static List<Component> forgedDetailTooltips(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(TAG_FORGING)) {
            return List.of();
        }
        CompoundTag forging = tag.getCompound(TAG_FORGING);
        List<Component> tooltips = new ArrayList<>();
        tooltips.add(Component.translatable("tooltip." + ImmortalCultivationMod.MODID + ".forged.materials")
                .withStyle(ChatFormatting.GRAY));
        ListTag ingredients = forging.getList("ingredients", Tag.TAG_COMPOUND);
        if (!ingredients.isEmpty()) {
            for (int i = 0; i < ingredients.size(); i++) {
                CompoundTag ingredient = ingredients.getCompound(i);
                ResourceLocation itemId = ResourceLocation.tryParse(ingredient.getString("item"));
                if (itemId == null) {
                    continue;
                }
                Item item = BuiltInRegistries.ITEM.get(itemId);
                Component itemName = item == Items.AIR
                        && !itemId.equals(ResourceLocation.withDefaultNamespace("air"))
                        ? Component.literal(itemId.toString())
                        : Component.translatable(item.getDescriptionId());
                int count = Math.max(1, ingredient.getInt("count"));
                tooltips.add(Component.literal("  ")
                        .append(itemName)
                        .append(Component.literal(count > 1 ? " x" + count : ""))
                        .withStyle(ChatFormatting.DARK_AQUA));
            }
        } else {
            CompoundTag materialCounts = forging.getCompound("materials");
            for (MaterialDef def : MATERIALS) {
                int count = materialCounts.getInt(def.id());
                if (count > 0) {
                    tooltips.add(Component.literal("  ")
                            .append(Component.translatable(def.item().getDescriptionId()))
                            .append(Component.literal(count > 1 ? " x" + count : ""))
                            .withStyle(ChatFormatting.DARK_AQUA));
                }
            }
        }
        String fire = normalizedFire(forging.getString("fire"));
        tooltips.add(Component.translatable("tooltip." + ImmortalCultivationMod.MODID + ".forged.fire",
                FlameKind.fromId(fire).special() ? FlameKind.fromId(fire).displayName()
                        : Component.translatable("tooltip." + ImmortalCultivationMod.MODID + ".forged.fire." + fire))
                .withStyle(ChatFormatting.RED));
        if (!(stack.getItem() instanceof com.example.immortal_cultivation_mod.alchemy.AlchemyPillItem)
                && !(stack.getItem() instanceof com.example.immortal_cultivation_mod.alchemy.LegacyPillItem)) {
            String tempering = normalizedTempering(forging.getString("tempering"));
            tooltips.add(Component.translatable("tooltip." + ImmortalCultivationMod.MODID + ".forged.tempering",
                    Component.translatable("tooltip." + ImmortalCultivationMod.MODID + ".forged.tempering." + tempering))
                    .withStyle(ChatFormatting.BLUE));
        }
        tooltips.add(Component.translatable("tooltip." + ImmortalCultivationMod.MODID + ".forged.average_temp",
                format(forging.getDouble("average_temp"))).withStyle(ChatFormatting.GRAY));
        tooltips.add(Component.translatable("tooltip." + ImmortalCultivationMod.MODID + ".forged.average_purity",
                format(forging.getDouble("average_purity"))).withStyle(ChatFormatting.GRAY));
        addForgedBuff(tooltips, forging, "durability_percent", 5);
        addForgedBuff(tooltips, forging, "self_repair_percent", 1);
        addForgedBuff(tooltips, forging, "armor_erosion_percent", 1);
        addForgedBuff(tooltips, forging, "qi_damage_percent", 1);
        addForgedBuff(tooltips, forging, "qi_regen_percent", 1);
        addForgedBuff(tooltips, forging, "slowness_layers", 1);
        addForgedBuff(tooltips, forging, "armor_pen_percent", 1);
        addForgedBuff(tooltips, forging, "quality_bonus_percent", 5);
        addForgedBuff(tooltips, forging, "dragon_pressure", 1);
        addForgedBuff(tooltips, forging, "star_essence", 1);
        addForgedBuff(tooltips, forging, "fire_storage", 1);
        addForgedBuff(tooltips, forging, "flame_durability_percent", 1);
        addForgedBuff(tooltips, forging, "material_loss_reduction_percent", 1);
        addForgedBuff(tooltips, forging, "flame_sharpness_percent", 1);
        addForgedBuff(tooltips, forging, "flame_spirit_damage_percent", 1);
        addForgedBuff(tooltips, forging, "flame_soul_damage", 1);
        addForgedBuff(tooltips, forging, "tribulation_resistance_percent", 1);
        addForgedBuff(tooltips, forging, "flame_lightning_damage_percent", 1);
        addForgedBuff(tooltips, forging, "flame_qi_burn_chance_percent", 1);
        addForgedBuff(tooltips, forging, "flame_fire_affinity_percent", 1);
        addForgedBuff(tooltips, forging, "flame_heavenly_quality_percent", 1);
        return tooltips;
    }

    public static int forgedStat(ItemStack stack, String key) {
        CompoundTag forging = forgingData(stack);
        return forging.isEmpty() ? 0 : Math.max(0, forging.getInt(key));
    }

    public static float adjustWeaponDamage(ItemStack stack, LivingEntity target, float damage) {
        if (stack.isEmpty() || !hasForgingData(stack)) {
            return damage;
        }
        int armorPen = forgedStat(stack, "armor_pen_percent");
        if (armorPen > 0 && hasArmor(target)) {
            damage *= 1.0F + armorPen / 100.0F;
        }
        int starEssence = forgedStat(stack, "star_essence");
        if (starEssence > 0) {
            damage *= 1.0F + starEssence * 0.02F;
        }
        int flameDamage = forgedStat(stack, "flame_sharpness_percent")
                + forgedStat(stack, "flame_spirit_damage_percent") + forgedStat(stack, "flame_lightning_damage_percent")
                + forgedStat(stack, "flame_heavenly_quality_percent");
        if (flameDamage > 0) damage *= 1.0F + flameDamage / 100.0F;
        // Dragon-forged weapons draw power from an existing flame; ignition itself cannot
        // grant the first strike this bonus, and fire-immune creatures never supply it.
        int fireAffinity = forgedStat(stack, "flame_fire_affinity_percent");
        if (fireAffinity > 0 && target.isOnFire() && !target.fireImmune()) {
            damage *= 1.0F + fireAffinity / 100.0F;
        }
        return damage;
    }

    public static float adjustInjectedQiDamage(ItemStack stack, float damage) {
        int qiDamage = forgedStat(stack, "qi_damage_percent") + forgedStat(stack, "flame_heavenly_quality_percent");
        return qiDamage <= 0 ? damage : damage * (1.0F + qiDamage / 100.0F);
    }

    public static int heldQiRegenBonusPercent(Player player) {
        return forgedStat(player.getMainHandItem(), "qi_regen_percent")
                + forgedStat(player.getOffhandItem(), "qi_regen_percent");
    }

    public static void applyWeaponHitEffects(ItemStack stack, LivingEntity target) {
        if (stack.isEmpty() || !hasForgingData(stack)) {
            return;
        }
        int slowness = forgedStat(stack, "slowness_layers");
        if (slowness > 0) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, Math.max(0, slowness - 1), false, true, true));
        }
        int erosion = forgedStat(stack, "armor_erosion_percent");
        if (erosion > 0) {
            erodeArmor(target, erosion);
        }
        int dragonPressure = forgedStat(stack, "dragon_pressure");
        if (dragonPressure > 0) {
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, Math.max(0, dragonPressure - 1), false, true, true));
        }
        int fireStorage = forgedStat(stack, "fire_storage");
        if (fireStorage > 0) {
            target.igniteForSeconds(Math.max(1, fireStorage));
        }
        int soulDamage = forgedStat(stack, "flame_soul_damage");
        if (soulDamage > 0 && !target.level().isClientSide) {
            if (target instanceof Player player) {
                com.example.immortal_cultivation_mod.event.ServerEvents.adjustThoughts(player, -soulDamage);
            } else if (target instanceof net.minecraft.world.entity.PathfinderMob mob
                    && target instanceof com.example.immortal_cultivation_mod.entity.CultivationNpc npc) {
                com.example.immortal_cultivation_mod.entity.NpcProfile.adjustThoughts(mob, npc.npcArchetype(), -soulDamage);
            }
        }
    }

    /** Called after positive accepted damage; protected, blocked and canceled attacks never burn Qi. */
    public static void applyAcceptedWeaponHitEffects(ItemStack stack, LivingEntity target) {
        if (target.level().isClientSide || !target.isAlive() || !WeaponRestrictionSystem.isWeapon(stack)) return;
        int chance = Math.clamp(forgedStat(stack, "flame_qi_burn_chance_percent"), 0, 100);
        if (chance > 0 && target.getRandom().nextInt(100) < chance) {
            target.addEffect(new MobEffectInstance(
                    com.example.immortal_cultivation_mod.effect.ModEffects.QI_FIRE_BURN,
                    100, 1, false, false, true));
        }
    }

    public static void tickForgedInventory(Player player) {
        if (player.tickCount % 100 != 0) {
            return;
        }
        for (ItemStack stack : player.getInventory().items) {
            tickForgedStack(stack);
        }
        for (ItemStack stack : player.getInventory().offhand) {
            tickForgedStack(stack);
        }
    }

    public static void tickForgedStack(ItemStack stack) {
        applyForgedDurability(stack);
        int repairPercent = forgedStat(stack, "self_repair_percent");
        if (repairPercent <= 0 || !stack.isDamageableItem() || !stack.isDamaged()) {
            return;
        }
        int repair = Math.max(1, stack.getMaxDamage() * repairPercent / 100);
        stack.setDamageValue(Math.max(0, stack.getDamageValue() - repair));
    }

    public static void applyForgedDurability(ItemStack stack) {
        int durabilityPercent = forgedStat(stack, "durability_percent") + forgedStat(stack, "flame_durability_percent")
                + forgedStat(stack, "flame_heavenly_quality_percent");
        if (durabilityPercent <= 0 || !stack.isDamageableItem()) {
            return;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag forging = tag.getCompound(TAG_FORGING);
        if (forging.getBoolean("durability_applied")) {
            return;
        }
        int baseMax = stack.getMaxDamage();
        int boosted = Math.max(baseMax + 1, baseMax + baseMax * durabilityPercent / 100);
        stack.set(DataComponents.MAX_DAMAGE, boosted);
        forging.putBoolean("durability_applied", true);
        tag.put(TAG_FORGING, forging);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static void addForgedBuff(List<Component> tooltips, CompoundTag forging, String key, int multiplier) {
        int count = forging.getInt(key);
        if (count <= 0) {
            return;
        }
        tooltips.add(Component.translatable("tooltip." + ImmortalCultivationMod.MODID + ".forged_buff." + key, count * multiplier)
                .withStyle(ChatFormatting.GOLD));
    }

    private static CompoundTag forgingData(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound(TAG_FORGING);
    }

    private static ListTag ingredientList(Map<Item, Integer> ingredientCounts) {
        ListTag ingredients = new ListTag();
        for (Map.Entry<Item, Integer> entry : ingredientCounts.entrySet()) {
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(entry.getKey());
            if (itemId == null || entry.getValue() <= 0) {
                continue;
            }
            CompoundTag ingredient = new CompoundTag();
            ingredient.putString("item", itemId.toString());
            ingredient.putInt("count", entry.getValue());
            ingredients.add(ingredient);
        }
        return ingredients;
    }

    private static PurityContribution effectivePurity(ItemStack stack) {
        // Stones are a stack-sized recipe cost, not dozens of separate purity inputs.
        // Keep their consumed quantity intact for recipes and ingredient history.
        if (isRecipeSpiritStone(stack)) {
            return new PurityContribution(purity(stack), 1);
        }
        CompoundTag inherited = forgingData(stack);
        if (inherited.contains("average_purity")) {
            double purity = Math.max(1.0D, Math.min(100.0D, inherited.getDouble("average_purity")));
            if (stack.is(com.example.immortal_cultivation_mod.magicritual.item.ModItems.BLANK_FORMATION_FLAG.get())) {
                return new PurityContribution(purity, Math.max(1, stack.getCount()));
            }
            int inheritedWeight = inherited.contains("purity_weight")
                    ? Math.max(1, inherited.getInt("purity_weight"))
                    : 1;
            long combinedWeight = (long) inheritedWeight * Math.max(1, stack.getCount());
            return new PurityContribution(purity,
                    (int) Math.min(Integer.MAX_VALUE, combinedWeight));
        }
        if (isPurityMaterial(stack)) {
            return new PurityContribution(purity(stack), Math.max(1, stack.getCount()));
        }
        return new PurityContribution(0.0D, 0);
    }

    private static PuritySummary puritySummary(List<ItemStack> materials) {
        double total = 0.0D;
        long weight = 0L;
        for (ItemStack stack : materials) {
            if (stack.isEmpty()) {
                continue;
            }
            PurityContribution contribution = effectivePurity(stack);
            total += contribution.purity() * contribution.weight();
            weight += contribution.weight();
        }
        return new PuritySummary(total, weight);
    }

    private static String normalizedFire(String fire) {
        if ("zhen_fire".equals(fire)) {
            return "zhen_fire";
        }
        if ("ling_fire".equals(fire)) {
            return "ling_fire";
        }
        return FlameKind.fromId(fire).id();
    }

    private static String normalizedTempering(String tempering) {
        if ("cool_water".equals(tempering)) {
            return "cool_water";
        }
        return "not_tempered";
    }

    private static Component recipeDisplayName(String recipeId) {
        return Component.translatable("tooltip." + ImmortalCultivationMod.MODID + ".forged.recipe." + recipeId);
    }

    private static boolean hasArmor(LivingEntity target) {
        for (ItemStack armor : target.getArmorSlots()) {
            if (!armor.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static void erodeArmor(LivingEntity target, int percent) {
        for (ItemStack armor : target.getArmorSlots()) {
            if (armor.isEmpty() || !armor.isDamageableItem()) {
                continue;
            }
            int damage = Math.max(1, armor.getMaxDamage() * percent / 100);
            int next = armor.getDamageValue() + damage;
            if (next >= armor.getMaxDamage()) {
                armor.shrink(1);
            } else {
                armor.setDamageValue(next);
            }
            return;
        }
    }

    private static int quality(int targetTemp, double averageTemp, List<ItemStack> materials) {
        double diff = Math.abs(averageTemp - targetTemp);
        int tempQuality;
        if (diff <= 250.0D) {
            tempQuality = 90 + (int) Math.round((250.0D - diff) / 25.0D);
        } else if (averageTemp < targetTemp) {
            tempQuality = 60 - (int) Math.min(20, (diff - 250.0D) / 80.0D);
        } else {
            tempQuality = 30 - (int) Math.min(20, (diff - 250.0D) / 120.0D);
        }
        int bonus = 0;
        for (ItemStack stack : materials) {
            if (stack.is(ModBlocks.LIUJIN_SAND.get().asItem())) {
                bonus += stack.getCount() * 5;
            }
        }
        return Math.max(1, Math.min(100, tempQuality + bonus));
    }

    public static String qualityName(int quality) {
        return switch (qualityKey(quality)) {
            case "heavenly" -> "Heavenly";
            case "flawless" -> "Flawless";
            case "perfect" -> "Perfect";
            case "excellent" -> "Excellent";
            case "fine" -> "Fine";
            case "normal" -> "Normal";
            default -> "Crude";
        };
    }

    public static Component qualityComponent(int quality) {
        return Component.translatable("weapon_quality." + ImmortalCultivationMod.MODID + "." + qualityKey(quality));
    }

    private static String qualityKey(int quality) {
        if (quality > 100) return "heavenly";
        if (quality >= 100) return "flawless";
        if (quality >= 95) return "perfect";
        if (quality >= 85) return "excellent";
        if (quality >= 70) return "fine";
        if (quality >= 50) return "normal";
        return "crude";
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static int physicalRecipeSlots(ItemStack sample, int count) {
        if (count <= 0) {
            return 0;
        }
        if (!canStackInForgingRecipe(sample)) {
            return count;
        }
        return (count + sample.getMaxStackSize() - 1) / sample.getMaxStackSize();
    }

    private static int spiritStoneRecipeSlots(int count) {
        return physicalRecipeSlots(new ItemStack(
                com.example.immortal_cultivation_mod.magicritual.item.ModItems
                        .SPIRIT_STONE.get()), count);
    }

    public record MaterialDef(String id, Item item, String displayTrait, String statKey) {
    }

    private record PurityContribution(double purity, int weight) {
    }

    private record PuritySummary(double total, long weight) {
        private double average() {
            return weight <= 0L ? 1.0D : total / weight;
        }
    }

    public record ForgingRecipe(String id, ItemLike moldItem, int averageTemp,
                                Map<Item, Integer> required, boolean directOutput,
                                int spiritStoneCount, String requiredFireType) {
        public ForgingRecipe {
            averageTemp = originalWeaponTemperature(id, averageTemp);
            // Clear legacy recipe fire locks, including those shown by slips and JEI.
            requiredFireType = "";
            Map<Item, Integer> materialRequirements = new LinkedHashMap<>();
            for (Map.Entry<Item, Integer> ingredient : required.entrySet()) {
                if (!isRecipeSpiritStone(new ItemStack(ingredient.getKey()))) {
                    materialRequirements.put(ingredient.getKey(), ingredient.getValue());
                }
            }
            required = Map.copyOf(materialRequirements);
            spiritStoneCount = WeaponShardSystem.REFORGE.equals(id) ? 128
                    : "crimson_flame_seed".equals(id) ? 8 : RECIPE_SPIRIT_STONE_COUNT;
            int slots = spiritStoneRecipeSlots(spiritStoneCount);
            for (Map.Entry<Item, Integer> ingredient : required.entrySet()) {
                int count = ingredient.getValue();
                if (count <= 0) {
                    throw new IllegalArgumentException("Non-positive ingredient count for " + id);
                }
                slots += physicalRecipeSlots(
                        new ItemStack(ingredient.getKey()), count);
            }
            if (slots > MAX_RECIPE_MATERIAL_SLOTS) {
                throw new IllegalArgumentException(
                        "Forging recipe " + id + " needs " + slots
                                + " material slots; maximum is " + MAX_RECIPE_MATERIAL_SLOTS);
            }
        }

        public int ingredientSlotCount() {
            int slots = spiritStoneRecipeSlots(spiritStoneCount);
            for (Map.Entry<Item, Integer> ingredient : required.entrySet()) {
                slots += physicalRecipeSlots(
                        new ItemStack(ingredient.getKey()), ingredient.getValue());
            }
            return slots;
        }

        public ForgingRecipe(String id, ItemLike moldItem, int averageTemp,
                             Map<Item, Integer> required, boolean directOutput,
                             int spiritStoneCount) {
            this(id, moldItem, averageTemp, required, directOutput, spiritStoneCount, "");
        }

        public ForgingRecipe(String id, ItemLike moldItem, int averageTemp,
                             Map<Item, Integer> required) {
            this(id, moldItem, averageTemp, required, false, 0, "");
        }
    }
}
