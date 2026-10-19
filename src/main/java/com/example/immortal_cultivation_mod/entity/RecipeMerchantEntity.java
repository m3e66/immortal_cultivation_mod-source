package com.example.immortal_cultivation_mod.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Wandering recipe trader using the shared cultivation, social and combat systems. */
public final class RecipeMerchantEntity extends ZhujiCultivatorEntity implements StockedCultivationMerchant {
    public RecipeMerchantEntity(EntityType<? extends RecipeMerchantEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public String npcArchetype() {
        return NpcProfile.MERCHANT;
    }

    @Override public int stockSlotCount(){return RecipeMerchantStock.OFFER_COUNT;}
    @Override public boolean sellsStock(net.minecraft.world.item.ItemStack stack){return RecipeMerchantStock.isRecipe(stack);}
    @Override public void ensureMerchantStock(net.minecraft.nbt.CompoundTag profile){RecipeMerchantStock.ensure(this,profile);}
}
