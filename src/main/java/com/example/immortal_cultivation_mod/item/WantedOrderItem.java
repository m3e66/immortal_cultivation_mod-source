package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.entity.CultivationNpc;
import com.example.immortal_cultivation_mod.entity.NpcProfile;
import com.example.immortal_cultivation_mod.network.ModPayloads;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.Comparator;
import java.util.Optional;

public class WantedOrderItem extends Item {
    private static final String NAME = "WantedName";
    private static final String SKIN = "WantedSkin";
    private static final String REALM = "WantedRealm";
    private static final String REWARD_ITEM = "RewardItem";
    private static final String REWARD_COUNT = "RewardCount";

    public WantedOrderItem(Properties properties) {
        super(properties);
    }

    public static ItemStack create(String name, String skin, String realm, String rewardItem, int rewardCount) {
        ItemStack stack = new ItemStack(ModItems.WANTED_ORDER.get());
        CompoundTag tag = new CompoundTag();
        tag.putString(NAME, name);
        tag.putString(SKIN, skin);
        tag.putString(REALM, realm);
        tag.putString(REWARD_ITEM, rewardItem);
        tag.putInt(REWARD_COUNT, Math.max(1, rewardCount));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            if (isIncomplete(tag)) {
                Optional<ItemStack> recovered = nearestWantedOrder(serverPlayer);
                if (recovered.isPresent()) {
                    tag = recovered.get().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                }
            }
            String rewardId = tag.getString(REWARD_ITEM);
            ResourceLocation rewardLocation = ResourceLocation.tryParse(rewardId);
            Item reward = rewardLocation == null ? Items.AIR : BuiltInRegistries.ITEM.get(rewardLocation);
            if (reward == Items.AIR) {
                reward = BuiltInRegistries.ITEM.get(ResourceLocation.parse("immortal_cultivation_mod:spirit_stone"));
            }
            String rewardKey = reward.getDescriptionId();
            String skin = tag.getString(SKIN);
            String name = tag.getString(NAME);
            String realm = tag.getString(REALM);
            PacketDistributor.sendToPlayer(serverPlayer, new ModPayloads.ClientboundOpenWantedOrderPayload(
                    skin.isBlank() ? "zhuji1" : skin,
                    name.isBlank() ? Component.translatable(
                            "screen." + ImmortalCultivationMod.MODID + ".wanted_unknown").getString() : name,
                    realm.isBlank() ? CultivationLevels.REALM_LIANQI + CultivationLevels.STAGE_EARLY : realm,
                    rewardKey,
                    Math.max(1, tag.getInt(REWARD_COUNT))));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private static boolean isIncomplete(CompoundTag tag) {
        return tag.getString(NAME).isBlank()
                || tag.getString(SKIN).isBlank()
                || tag.getString(REALM).isBlank()
                || tag.getString(REWARD_ITEM).isBlank()
                || tag.getInt(REWARD_COUNT) <= 0;
    }

    private static Optional<ItemStack> nearestWantedOrder(ServerPlayer player) {
        return player.level().getEntitiesOfClass(
                        PathfinderMob.class,
                        player.getBoundingBox().inflate(32.0D),
                        mob -> mob instanceof CultivationNpc npc
                                && "wanted".equals(NpcProfile.identity(mob, npc.npcArchetype())))
                .stream()
                .min(Comparator.comparingDouble(player::distanceToSqr))
                .map(NpcProfile::createWantedOrder);
    }
}
