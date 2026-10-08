package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.fate.LuckSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class FateAlchemyFormulaItem extends Item {
    public static final String PROFICIENCY = "craft_alchemy";
    public FateAlchemyFormulaItem(Properties properties, int tier) { super(properties); }
    @Override public InteractionResult useOn(UseOnContext context) {
        return com.example.immortal_cultivation_mod.alchemy.AlchemyContent.FORMULAS.get("qingling").get().useOn(context);
    }
    public static void tick(ServerPlayer player) {
        var fate = LuckSystem.data(player);
        if (!fate.contains("Alchemy")) return;
        var state = fate.getCompound("Alchemy");
        BlockPos furnacePos = BlockPos.of(state.getLong("Furnace"));
        var level = player.serverLevel();
        boolean valid = player.isAlive() && state.getString("Dimension").equals(level.dimension().location().toString())
                && player.distanceToSqr(furnacePos.getCenter()) <= 64 && level.hasChunkAt(furnacePos);
        if (valid) {
            var block = level.getBlockState(furnacePos);
            valid = block.is(Blocks.FURNACE) && block.getValue(BlockStateProperties.LIT);
        }
        if (valid && level.getGameTime() - state.getLong("Started") < 200) return;
        int tier = state.getInt("Tier");
        fate.remove("Alchemy");
        var stats = ModAttachments.getData(player);
        // Skill, not Luck, determines whether the batch succeeds.
        double skillChance = Math.min(1, .80 + stats.methodProficiency(PROFICIENCY) * .005);
        boolean success = valid && tier >= 0 && tier <= 2 && stats.methodProficiency(PROFICIENCY) >= tier * 10
                && com.example.immortal_cultivation_mod.fate.DebugEyeDiagnostics.roll(player, "alchemy_success",
                        skillChance, skillChance, player.getRandom().nextDouble());
        if (!valid) com.example.immortal_cultivation_mod.fate.DebugEyeDiagnostics.note(player, "action", "Alchemy", "interrupted: furnace, range, dimension, or life check failed");
        var formula=com.example.immortal_cultivation_mod.alchemy.AlchemyChemistry.formula("qingling");
        ItemStack output=success
                ?com.example.immortal_cultivation_mod.alchemy.PillData.create(
                    com.example.immortal_cultivation_mod.alchemy.AlchemyChemistry.brew(formula,formula.required()))
                :new ItemStack(com.example.immortal_cultivation_mod.item.ModItems.ASHES.get(),formula.yield());
        if (!player.getInventory().add(output)) player.drop(output, false);
        // Practice improves even a failed valid batch, but invalid/interrupted attempts do not.
        if (valid) ModAttachments.setData(player, ModAttachments.getData(player).withAddedMethodProficiency(PROFICIENCY, 1));
        com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(player);
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context,
            java.util.List<net.minecraft.network.chat.Component> lines, net.minecraft.world.item.TooltipFlag flag) {
        com.example.immortal_cultivation_mod.alchemy.AlchemyContent.FORMULAS.get("qingling").get()
                .appendHoverText(stack,context,lines,flag);
    }
}
