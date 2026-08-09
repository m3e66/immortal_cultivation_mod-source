package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.block.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
    public static final String TAG_FORGING = ImmortalCultivationMod.MODID + ".forging";
    public static final int MAX_TEMP = 10000;

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
        return isForgeMaterial(stack)
                || stack.is(ModItems.CHI_LIN.get())
                || stack.is(ModItems.HUO_XI_BLOOD.get())
                || stack.is(ModItems.JINDAN_YAODAN.get())
                || stack.is(ModItems.CHIYAN_BONE.get())
                || stack.is(ModItems.YUANYING_YAODAN.get())
                || stack.is(ModItems.YANJIAO_HORN.get())
                || stack.is(ModItems.YAOYUAN_BONE.get())
                || stack.is(ModItems.XUESHA_HIDE.get())
                || stack.is(com.example.immortal_cultivation_mod.magicritual.item.ModItems.ZHENWEN_JADE.get())
                || stack.is(com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_SILKWORM_SILK.get())
                || stack.is(com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_GUIDING_THREAD.get())
                || stack.is(com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_STORAGE_CRYSTAL.get())
                || stack.is(com.example.immortal_cultivation_mod.magicritual.item.ModItems.FLOWING_SPIRIT_CRYSTAL.get())
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
            tag.putInt(TAG_PURITY, purity);
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
        return null;
    }

    public static ForgingRecipe recipeById(String id) {
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
        if ("zhenpan".equals(id)) {
            return new ForgingRecipe(
                    "zhenpan",
                    com.example.immortal_cultivation_mod.magicritual.item.ModItems.ZHENPAN.get(),
                    1800,
                    Map.of(
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems.ZHENWEN_JADE.get(), 4,
                            ModBlocks.JINGTIE_MOTHERSTONE.get().asItem(), 1
                    ),
                    true,
                    128);
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
        return null;
    }

    public static boolean hasRequiredMaterials(ForgingRecipe recipe, List<ItemStack> inputs) {
        if (recipe == null) {
            return false;
        }
        Map<Item, Integer> counts = new LinkedHashMap<>();
        int spiritStones = 0;
        for (ItemStack stack : inputs) {
            if (stack.isEmpty()) {
                continue;
            }
            counts.merge(stack.getItem(), stack.getCount(), Integer::sum);
            if (isRecipeSpiritStone(stack)) {
                spiritStones += stack.getCount();
            }
        }
        for (var required : recipe.required().entrySet()) {
            if (counts.getOrDefault(required.getKey(), 0) < required.getValue()) {
                return false;
            }
        }
        return spiritStones >= recipe.spiritStoneCount();
    }

    public static boolean isIngredientForRecipe(ForgingRecipe recipe, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (recipe == null || !recipe.directOutput()) {
            return isForgeMaterial(stack);
        }
        return recipe.required().containsKey(stack.getItem())
                || recipe.spiritStoneCount() > 0 && isRecipeSpiritStone(stack);
    }

    public static boolean isRecipeSpiritStone(ItemStack stack) {
        return com.example.immortal_cultivation_mod.magicritual.item.ModItems
                .isSpiritStone(stack.getItem())
                && !stack.is(com.example.immortal_cultivation_mod.magicritual.item.ModItems
                .INFINITE_SPIRIT_STONE.get());
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

    public static ItemStack createMold(ForgingRecipe recipe, List<ItemStack> materials, double averageTemp, String fireType) {
        ItemStack mold = new ItemStack(recipe.moldItem());
        int quality = quality(recipe.averageTemp(), averageTemp, materials);
        CompoundTag data = new CompoundTag();
        data.putInt("schema_version", 2);
        data.putString("recipe", recipe.id());
        data.putInt("target_temp", recipe.averageTemp());
        data.putDouble("average_temp", averageTemp);
        data.putInt("quality", quality);
        data.putString("quality_name", qualityName(quality));
        data.putString("fire", fireType == null || fireType.isBlank() ? "normal_fire" : fireType);
        CompoundTag materialCounts = new CompoundTag();
        Map<Item, Integer> ingredientCounts = new LinkedHashMap<>();
        double purityTotal = 0.0D;
        int purityWeight = 0;
        for (ItemStack stack : materials) {
            if (stack.isEmpty()) {
                continue;
            }
            ingredientCounts.merge(stack.getItem(), stack.getCount(), Integer::sum);
            PurityContribution contribution = effectivePurity(stack);
            purityTotal += contribution.purity() * contribution.weight();
            purityWeight += contribution.weight();

            MaterialDef material = material(stack);
            if (recipe.directOutput() || material == null) {
                continue;
            }
            materialCounts.putInt(material.id(), materialCounts.getInt(material.id()) + stack.getCount());
            data.putInt(material.statKey(), data.getInt(material.statKey()) + stack.getCount());
        }
        data.put("materials", materialCounts);
        data.put("ingredients", ingredientList(ingredientCounts));
        data.putInt("purity_weight", purityWeight);
        data.putDouble("average_purity", purityWeight <= 0 ? 1.0D : purityTotal / purityWeight);
        CompoundTag tag = mold.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.put(TAG_FORGING, data);
        mold.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return mold;
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
                && !"flying_needle".equals(recipe)) {
            return ItemStack.EMPTY;
        }
        forging.putString("tempering", "cool_water");
        tag.put(TAG_FORGING, forging);
        ItemStack weapon = new ItemStack(switch (recipe) {
            case "dao" -> ModItems.DAO.get();
            case "jingtie_spear" -> ModItems.JINGTIE_SPEAR.get();
            case "heavy_iron_bracer" -> ModItems.HEAVY_IRON_BRACER.get();
            case "throwing_knife" -> ModItems.THROWING_KNIFE.get();
            case "flying_needle" -> ModItems.FLYING_NEEDLE.get();
            default -> ModItems.FLYING_SWORD.get();
        });
        weapon.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        applyForgedDurability(weapon);
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
        return (stack.is(ModItems.FLYING_SWORD.get()) || stack.is(ModItems.DAO.get())
                || stack.is(ModItems.JINGTIE_SPEAR.get())
                || stack.is(ModItems.THROWING_KNIFE.get())
                || stack.is(ModItems.FLYING_NEEDLE.get())
                || stack.is(ModItems.HEAVY_IRON_BRACER.get())) && hasForgingData(stack);
    }

    public static boolean isForgedRitualArtifact(ItemStack stack) {
        return (stack.is(com.example.immortal_cultivation_mod.magicritual.item.ModItems.ZHENPAN.get())
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
                Component.translatable("tooltip." + ImmortalCultivationMod.MODID + ".forged.fire." + fire))
                .withStyle(ChatFormatting.RED));
        String tempering = normalizedTempering(forging.getString("tempering"));
        tooltips.add(Component.translatable("tooltip." + ImmortalCultivationMod.MODID + ".forged.tempering",
                Component.translatable("tooltip." + ImmortalCultivationMod.MODID + ".forged.tempering." + tempering))
                .withStyle(ChatFormatting.BLUE));
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
        return damage;
    }

    public static float adjustInjectedQiDamage(ItemStack stack, float damage) {
        int qiDamage = forgedStat(stack, "qi_damage_percent");
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
        int durabilityPercent = forgedStat(stack, "durability_percent");
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

    private static String normalizedFire(String fire) {
        if ("ling_fire".equals(fire)) {
            return "ling_fire";
        }
        return "normal_fire";
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

    public record MaterialDef(String id, Item item, String displayTrait, String statKey) {
    }

    private record PurityContribution(double purity, int weight) {
    }

    public record ForgingRecipe(String id, ItemLike moldItem, int averageTemp,
                                Map<Item, Integer> required, boolean directOutput,
                                int spiritStoneCount) {
        public ForgingRecipe(String id, ItemLike moldItem, int averageTemp,
                             Map<Item, Integer> required) {
            this(id, moldItem, averageTemp, required, false, 0);
        }
    }
}
