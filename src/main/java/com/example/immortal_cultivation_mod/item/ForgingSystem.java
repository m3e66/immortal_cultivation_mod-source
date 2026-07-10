package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.block.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return Math.max(1, Math.min(100, tag.getInt(TAG_PURITY)));
    }

    public static void ensurePurity(ItemStack stack, int seed) {
        if (!isForgeMaterial(stack)) {
            return;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(TAG_PURITY)) {
            int purity = 1 + Math.floorMod(seed * 31 + stack.getCount() * 17, 100);
            tag.putInt(TAG_PURITY, purity);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    public static Component purityTooltip(ItemStack stack) {
        if (!isForgeMaterial(stack)) {
            return Component.empty();
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
        return null;
    }

    public static boolean hasRequiredMaterials(ForgingRecipe recipe, List<ItemStack> inputs) {
        if (recipe == null) {
            return false;
        }
        Map<Item, Integer> counts = new LinkedHashMap<>();
        for (ItemStack stack : inputs) {
            if (!stack.isEmpty()) {
                counts.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
        }
        for (var required : recipe.required().entrySet()) {
            if (counts.getOrDefault(required.getKey(), 0) < required.getValue()) {
                return false;
            }
        }
        return true;
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
        data.putString("recipe", recipe.id());
        data.putInt("target_temp", recipe.averageTemp());
        data.putDouble("average_temp", averageTemp);
        data.putInt("quality", quality);
        data.putString("quality_name", qualityName(quality));
        data.putString("fire", fireType == null || fireType.isBlank() ? "normal_fire" : fireType);
        CompoundTag materialCounts = new CompoundTag();
        double purityTotal = 0.0D;
        int purityCount = 0;
        for (ItemStack stack : materials) {
            MaterialDef material = material(stack);
            if (material == null) {
                continue;
            }
            purityTotal += purity(stack) * stack.getCount();
            purityCount += stack.getCount();
            materialCounts.putInt(material.id(), materialCounts.getInt(material.id()) + stack.getCount());
            data.putInt(material.statKey(), data.getInt(material.statKey()) + stack.getCount());
        }
        data.put("materials", materialCounts);
        data.putDouble("average_purity", purityCount <= 0 ? 1.0D : purityTotal / purityCount);
        CompoundTag tag = mold.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.put(TAG_FORGING, data);
        mold.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return mold;
    }

    public static ItemStack temper(ItemStack mold) {
        if (!mold.is(ModItems.SWORD_MOLD.get()) && !mold.is(ModItems.SABER_MOLD.get())) {
            return ItemStack.EMPTY;
        }
        CompoundTag tag = mold.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag forging = tag.getCompound(TAG_FORGING);
        String recipe = forging.getString("recipe");
        if (!"flying_sword".equals(recipe) && !"dao".equals(recipe)) {
            return ItemStack.EMPTY;
        }
        forging.putString("tempering", "cool_water");
        tag.put(TAG_FORGING, forging);
        ItemStack weapon = new ItemStack("dao".equals(recipe) ? ModItems.DAO.get() : ModItems.FLYING_SWORD.get());
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
                qualityName(forging.getInt("quality")),
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
                qualityName(forging.getInt("quality")));
    }

    public static boolean hasForgingData(ItemStack stack) {
        return !stack.isEmpty() && stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().contains(TAG_FORGING);
    }

    public static boolean isForgedWeapon(ItemStack stack) {
        return (stack.is(ModItems.FLYING_SWORD.get()) || stack.is(ModItems.DAO.get())) && hasForgingData(stack);
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
        if (quality >= 100) return "Flawless";
        if (quality >= 95) return "Perfect";
        if (quality >= 85) return "Excellent";
        if (quality >= 70) return "Fine";
        if (quality >= 50) return "Normal";
        return "Crude";
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    public record MaterialDef(String id, Item item, String displayTrait, String statKey) {
    }

    public record ForgingRecipe(String id, ItemLike moldItem, int averageTemp, Map<Item, Integer> required) {
    }
}
