package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** A physical talisman formula. New formulas use one registered item per recipe. */
public final class TalismanFormulaItem extends Item {
    private static final String RECIPE_TAG = ImmortalCultivationMod.MODID + ":talisman_formula";
    private final String intrinsicRecipeId;

    /** Legacy blank formula constructor, retained so old data-tagged stacks still load. */
    public TalismanFormulaItem(Properties properties) {
        this(properties, "");
    }

    public TalismanFormulaItem(Properties properties, String recipeId) {
        super(properties);
        this.intrinsicRecipeId = recipeId == null ? "" : recipeId;
    }

    public static ItemStack create(String recipeId) {
        TalismanRecipes.Def recipe = TalismanRecipes.byId(recipeId);
        if (recipe == null) return new ItemStack(ModItems.TALISMAN_FORMULA.get());
        return new ItemStack(ModItems.talismanFormulaFor(recipe.id()));
    }

    public static String recipeId(ItemStack stack) {
        if (!(stack.getItem() instanceof TalismanFormulaItem formula)) return "";
        if (!formula.intrinsicRecipeId.isBlank()) return formula.intrinsicRecipeId;

        // Compatibility for worlds made before formulas became individual items.
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getString(RECIPE_TAG);
    }

    @Override
    public Component getName(ItemStack stack) {
        TalismanRecipes.Def recipe = TalismanRecipes.byId(recipeId(stack));
        if (recipe == null) return super.getName(stack);
        return Component.translatable("item." + ImmortalCultivationMod.MODID + ".talisman_formula.named",
                recipe.result().getHoverName());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        TalismanRecipes.Def recipe = TalismanRecipes.byId(recipeId(stack));
        if (recipe != null) {
            tooltip.add(Component.translatable(
                    "tooltip." + ImmortalCultivationMod.MODID + ".talisman_formula.requirements")
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("  ")
                    .append(Component.translatable(
                            "tooltip." + ImmortalCultivationMod.MODID + ".talisman_formula.master_tier",
                            recipe.masterTier()))
                    .withStyle(ChatFormatting.DARK_AQUA));
            tooltip.add(Component.literal("  ")
                    .append(Component.translatable(
                            "tooltip." + ImmortalCultivationMod.MODID + ".talisman_formula.paper_tier",
                            recipe.minPaperTier()))
                    .withStyle(ChatFormatting.DARK_AQUA));
            tooltip.add(Component.literal("  ")
                    .append(Component.translatable(
                            "tooltip." + ImmortalCultivationMod.MODID + ".talisman_formula.ink_tier",
                            recipe.minInkTier()))
                    .withStyle(ChatFormatting.DARK_AQUA));
            tooltip.add(Component.literal("  ")
                    .append(Component.translatable(
                            "tooltip." + ImmortalCultivationMod.MODID + ".talisman_formula.pattern",
                            Component.translatable(TalismanRecipes.patternNameKey(recipe))))
                    .withStyle(ChatFormatting.DARK_AQUA));
            tooltip.add(Component.translatable(
                    "tooltip." + ImmortalCultivationMod.MODID + ".talisman_formula.qi_cost",
                    recipe.qiCost()).withStyle(ChatFormatting.RED));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        TalismanRecipes.Def recipe = TalismanRecipes.byId(recipeId(stack));
        if (recipe == null || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.fail(stack);
        }
        ModAttachments.CultivationData data = ModAttachments.getData(serverPlayer);
        String key = TalismanRecipes.formulaKey(recipe.id());
        if (data.methodProficiencies().containsKey(key)) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID + ".talisman_formula.already_learned",
                    recipe.result().getHoverName()), true);
            return InteractionResultHolder.success(stack);
        }
        Map<String, Integer> learned = new HashMap<>(data.methodProficiencies());
        learned.put(key, 1);
        ModAttachments.setData(serverPlayer, data.withMethodProficiencies(learned));
        ServerEvents.syncPlayerData(serverPlayer);
        serverPlayer.displayClientMessage(Component.translatable(
                "message." + ImmortalCultivationMod.MODID + ".talisman_formula.learned",
                recipe.result().getHoverName()), true);
        // A formula remains as the physical reference used by the drawing table
        // until that individual talisman has been completed successfully ten times.
        return InteractionResultHolder.success(stack);
    }
}
