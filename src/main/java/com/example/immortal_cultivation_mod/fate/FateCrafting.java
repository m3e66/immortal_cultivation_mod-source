package com.example.immortal_cultivation_mod.fate;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.item.ForgingSystem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** Fortune is rolled only after normal crafting/forging validation has succeeded. */
@EventBusSubscriber(modid = ImmortalCultivationMod.MODID)
public final class FateCrafting {
    public static final double QUALITY_CHANCE = .035, MUTATION_CHANCE = .008, PATTERN_CHANCE = .002, EXTRA_PILL_CHANCE = .012;
    public static final String MUTATION = "icmFortuneMutation", PILL = "icmPillQuality", PATTERN = "icmNaturalPillPattern";
    public static final String[] MUTATIONS = {"feng_rui", "po_jia", "guan_ling", "lie_ling", "zhen_mai",
            "duan_shi", "zhui_ming", "shi_xue", "yin_ling", "zhan_hun", "mie_xie", "po_fa", "po_zhen",
            "zhen_shan", "beng_yue", "lian_feng", "xu_shi", "hui_feng", "can_ying", "ling_bao"};
    private FateCrafting() {}
    public static CompoundTag tags(ItemStack stack) { return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag(); }
    public static void save(ItemStack stack, CompoundTag tags) { stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tags)); }
    public static void afterForge(ServerPlayer player, ItemStack result, boolean weapon) {
        if (result.isEmpty()) return;
        var tags = tags(result);
        if (tags.getBoolean("icmFortuneForged")) return;
        tags.putBoolean("icmFortuneForged", true);
        double factor = LuckRules.rareMultiplier(LuckSystem.luck(player));
        var forged = tags.getCompound(ForgingSystem.TAG_FORGING);
        if (DebugEyeDiagnostics.roll(player, "forge_quality", QUALITY_CHANCE, QUALITY_CHANCE * factor, player.getRandom().nextDouble())) {
            int quality = forged.getInt("quality");
            for (int tier : new int[]{50, 70, 85, 95, 100}) {
                if (tier > quality) { forged.putInt("quality", tier); forged.putString("quality_name", ForgingSystem.qualityName(tier)); break; }
            }
        }
        if (weapon && DebugEyeDiagnostics.roll(player, "forge_mutation", MUTATION_CHANCE, MUTATION_CHANCE * factor, player.getRandom().nextDouble())) {
            tags.putString(MUTATION, MUTATIONS[player.getRandom().nextInt(MUTATIONS.length)]);
            LuckSystem.change(player, -2);
        }
        tags.put(ForgingSystem.TAG_FORGING, forged);
        save(result, tags);
    }
    public static boolean pill(ItemStack stack) {
        var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key.getNamespace().equals(ImmortalCultivationMod.MODID) && key.getPath().endsWith("_pill");
    }
    @SubscribeEvent
    public static void crafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !pill(event.getCrafting())) return;
        // The crafting event follows a valid recipe; Luck never accepts ingredients or creates a missing recipe.
        ItemStack result = event.getCrafting();
        int extraCount = afterAlchemy(player, result);
        if (extraCount > 0) {
            ItemStack extra = result.copyWithCount(extraCount);
            if (!player.getInventory().add(extra)) player.drop(extra, false);
        }
    }
    public static int afterAlchemy(ServerPlayer player, ItemStack result) {
        var tags = tags(result);
        if (tags.getBoolean("icmFortuneAlchemy")) return 0;
        tags.putBoolean("icmFortuneAlchemy", true);
        double factor = LuckRules.rareMultiplier(LuckSystem.luck(player));
        if (DebugEyeDiagnostics.roll(player, "pill_quality", QUALITY_CHANCE, QUALITY_CHANCE * factor, player.getRandom().nextDouble())) tags.putInt(PILL, Math.min(3, tags.getInt(PILL) + 1));
        if (DebugEyeDiagnostics.roll(player, "pill_pattern", PATTERN_CHANCE, PATTERN_CHANCE * factor, player.getRandom().nextDouble())) { tags.putBoolean(PATTERN, true); LuckSystem.change(player, -2); }
        save(result, tags);
        return DebugEyeDiagnostics.roll(player, "extra_pill", EXTRA_PILL_CHANCE, EXTRA_PILL_CHANCE * factor, player.getRandom().nextDouble()) ? 1 : 0;
    }
    public static int pillRate(ItemStack stack, int baseRate) { return Math.round(baseRate * (1 + .15F * tags(stack).getInt(PILL))); }
    public static void consumedPill(ServerPlayer player, ItemStack stack) {
        com.example.immortal_cultivation_mod.alchemy.PillToxicity.consumed(player,stack);
        com.example.immortal_cultivation_mod.guidance.DaoGuidanceObservations.record(player, "pill_used");
        if (stack.getItem() instanceof com.example.immortal_cultivation_mod.alchemy.LegacyPillItem) { com.example.immortal_cultivation_mod.guidance.DaoGuidanceObservations.record(player, "pill_effect"); }
        var tag = tags(stack);
        int quality = Math.max(0, Math.min(3, tag.getInt(PILL)));
        if (quality > 0) player.heal((float)(player.getMaxHealth() * .005F * quality * com.example.immortal_cultivation_mod.alchemy.PillToxicity.multiplier(player,"Restore")));
        if (tag.getBoolean(PATTERN)) {
            var stats = ModAttachments.getData(player);
            int max = stats.maxQi();
            ModAttachments.setData(player, stats.withQi(Math.min(max, stats.qi() + (int)(Math.max(1, max / 50)*com.example.immortal_cultivation_mod.alchemy.PillToxicity.multiplier(player,"Restore")))));
        }
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void drops(LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        int shift = (LuckSystem.luck(player) - 50) / 10;
        for (var drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            if (!ForgingSystem.isPurityMaterial(stack) || ForgingSystem.hasFixedPurity(stack)) continue;
            var tag = tags(stack);
            if (tag.getBoolean("icmFortunePurity")) continue;
            if (!ForgingSystem.hasDeterminedPurity(stack)) {
                // Drops remain unidentified and stackable. Apply fortune only when
                // the player identifies this material or a crafting process uses it.
                tag.putInt(ForgingSystem.TAG_PENDING_PURITY_LUCK, shift);
                tag.putBoolean("icmFortunePurity", true);
                save(stack, tag);
                continue;
            }
            int before = ForgingSystem.purity(stack);
            tag = tags(stack);
            tag.putInt(ForgingSystem.TAG_PURITY, Math.max(1, Math.min(100, ForgingSystem.purity(stack) + shift)));
            tag.putBoolean("icmFortunePurity", true);
            save(stack, tag);
            DebugEyeDiagnostics.note(player, "purity", stack.getHoverName(), before, ForgingSystem.purity(stack), shift);
        }
    }
    @SubscribeEvent
    public static void tooltip(ItemTooltipEvent event) {
        var tag = tags(event.getItemStack());
        if (tag.contains(MUTATION)) event.getToolTip().add(net.minecraft.network.chat.Component.translatable(
                "luck.immortal_cultivation_mod.mutation." + tag.getString(MUTATION)).withStyle(net.minecraft.ChatFormatting.GRAY));
        if (tag.getInt(PILL) > 0) event.getToolTip().add(net.minecraft.network.chat.Component.translatable(
                "luck.immortal_cultivation_mod.pill_quality", tag.getInt(PILL)).withStyle(net.minecraft.ChatFormatting.GRAY));
        if (tag.getBoolean(PATTERN)) event.getToolTip().add(net.minecraft.network.chat.Component.translatable(
                "luck.immortal_cultivation_mod.pill_pattern").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
