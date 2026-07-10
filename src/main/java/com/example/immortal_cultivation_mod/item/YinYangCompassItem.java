package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.network.ModPayloads;
import com.example.immortal_cultivation_mod.world.YinQiField;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

public class YinYangCompassItem extends Item {
    private static final int GRID_SIZE = 33;
    private static final int GRID_STEP = 16;

    public YinYangCompassItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            BlockPos center = player.blockPosition();
            List<Integer> values = YinQiField.sampleGrid(serverLevel, center, GRID_SIZE, GRID_STEP);
            List<Integer> qiValues = YinQiField.sampleQiGrid(serverLevel, center, GRID_SIZE, GRID_STEP);
            PacketDistributor.sendToPlayer(serverPlayer, new ModPayloads.ClientboundYinYangCompassPayload(
                    center.getX(),
                    center.getZ(),
                    GRID_STEP,
                    GRID_SIZE,
                    values,
                    qiValues
            ));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
