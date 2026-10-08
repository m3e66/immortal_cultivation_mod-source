package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;

public class HigherWeaponItem extends SwordItem {
    public final HigherWeaponSpec spec;
    public HigherWeaponItem(HigherWeaponSpec spec,Properties properties) {
        super(Tiers.IRON,properties.attributes(attributes(spec)));
        this.spec=spec;
    }
    public static ResourceLocation id(String value) {
        return ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod",value);
    }
    public static ItemAttributeModifiers attributes(HigherWeaponSpec spec) {
        float damage=spec.damage,speed=spec.speed;
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,new AttributeModifier(BASE_ATTACK_DAMAGE_ID,damage-1,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,new AttributeModifier(BASE_ATTACK_SPEED_ID,speed-4,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ENTITY_INTERACTION_RANGE,new AttributeModifier(id("higher_weapon_reach"),spec.reach-3,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND)
                .build();
    }
    public static CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
    }
    public static void save(ItemStack stack,CompoundTag tag) { stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag)); }
    public boolean ready(ServerPlayer player) {
        if(player.getCooldowns().isOnCooldown(this) || com.example.immortal_cultivation_mod.combat.CombatStateManager.isSpellcastingBlocked(player))return false;
        if(!player.isCreative() && CultivationLevels.getRealmIndex(ModAttachments.getData(player).cultivationLevel())<spec.realm) {
            player.displayClientMessage(Component.translatable("message.immortal_cultivation_mod.higher_weapon.realm",spec.realm==3?"金丹 / Golden Core":"筑基 / Foundation Establishment"),true);
            return false;
        }
        return true;
    }
    public boolean spend(ServerPlayer player,ItemStack stack,int qi,int cooldown) {
        if(!ready(player))return false;
        if(!ServerEvents.spendQiOrBlood(player,ModAttachments.getData(player),qi)) {
            player.displayClientMessage(Component.translatable("message.immortal_cultivation_mod.not_enough_qi"),true);
            return false;
        }
        player.getCooldowns().addCooldown(this,cooldown);
        stack.hurtAndBreak(1,player,EquipmentSlot.MAINHAND);
        ServerEvents.syncPlayerData(player);
        return true;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        ItemStack stack=player.getItemInHand(hand);
        if(hand!=InteractionHand.MAIN_HAND)return InteractionResultHolder.pass(stack);
        if(!level.isClientSide && WindFanQi.isFan(stack)) WindFanQi.migrateLegacyStack(stack);
        if(spec==HigherWeaponSpec.FLAME_PATTERN_DAO) {
            if(player instanceof ServerPlayer server && !ready(server))return InteractionResultHolder.fail(stack);
            player.startUsingItem(hand);return InteractionResultHolder.consume(stack);
        }
        if(player instanceof ServerPlayer server && ready(server))HigherWeaponCombat.cast(server,stack,this,false);
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
    @Override public void inventoryTick(ItemStack stack,Level level,net.minecraft.world.entity.Entity entity,int slot,boolean selected) {
        super.inventoryTick(stack,level,entity,slot,selected);
        if(!level.isClientSide && spec==HigherWeaponSpec.WIND_FEATHER_FAN)WindFanQi.migrateLegacyStack(stack);
    }
    @Override public boolean isFoil(ItemStack stack) {
        return super.isFoil(stack) || spec==HigherWeaponSpec.SEVEN_STAR_SWORD && SevenStarPattern.charges(stack)>=SevenStarPattern.COUNT;
    }
    @Override public void releaseUsing(ItemStack stack,Level level,LivingEntity user,int remaining) {
        if(spec==HigherWeaponSpec.FLAME_PATTERN_DAO && user instanceof ServerPlayer player)
            HigherWeaponCombat.cast(player,stack,this,getUseDuration(stack,user)-remaining>=30);
    }
    @Override public int getUseDuration(ItemStack stack,LivingEntity user) { return 72000; }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.NONE; }
}
