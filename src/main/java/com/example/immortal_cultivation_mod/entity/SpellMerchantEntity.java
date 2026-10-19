package com.example.immortal_cultivation_mod.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A wandering spell-scroll trader using the shared NPC social, combat and trade systems. */
public final class SpellMerchantEntity extends ZhujiCultivatorEntity implements StockedCultivationMerchant {
    public SpellMerchantEntity(EntityType<? extends SpellMerchantEntity> type,Level level){super(type,level);}
    @Override public String npcArchetype(){return NpcProfile.SPELL_MERCHANT;}
    @Override public int stockSlotCount(){return SpellMerchantStock.OFFER_COUNT;}
    @Override public boolean sellsStock(ItemStack stack){return SpellMerchantStock.isSpell(stack);}
    @Override public void ensureMerchantStock(CompoundTag profile){SpellMerchantStock.ensure(this,profile);}
}
