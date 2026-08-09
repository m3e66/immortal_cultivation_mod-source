package com.example.immortal_cultivation_mod.magicritual.block.custom;

import com.example.immortal_cultivation_mod.magicritual.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

public class SpiritVeinCenterBlockEntity extends BlockEntity {
   private static final DustParticleOptions VEIN_PARTICLE = new DustParticleOptions(new Vector3f(0.0F, 1.0F, 1.0F), 5.0F);
   private static final int PARTICLE_INTERVAL_TICKS = 2;
   private static final int PARTICLE_COUNT = 10;
   private static final String TAG_TICK_COUNTER = "tick_counter";
   private static final String TAG_STORED_SPIRIT_STONES = "stored_spirit_stones";
   private static final String TAG_STORED_MEDIUM_SPIRIT_STONES = "stored_medium_spirit_stones";
   private static final String TAG_STORED_HIGH_SPIRIT_STONES = "stored_high_spirit_stones";
   private int tickCounter = 0;
   private int storedSpiritStones = 0;
   private int storedMediumSpiritStones = 0;
   private int storedHighSpiritStones = 0;

   public SpiritVeinCenterBlockEntity(BlockPos pos, BlockState state) {
      super(ModBlockEntities.SPIRIT_VEIN_CENTER_BE.get(), pos, state);
   }

   public static void tick(Level level, BlockPos pos, BlockState state, SpiritVeinCenterBlockEntity be) {
      if (level.isClientSide) {
         be.spawnParticles(level, pos);
      } else {
         be.tickCounter++;
         if (be.tickCounter >= 1200) {
            be.tickCounter = 0;
            Item generatedSpiritStone = be.getGeneratedSpiritStone(level);
            if (be.hasBlockAbove()) {
               be.storeSpiritStone(generatedSpiritStone);
               be.setChanged();
            } else {
               be.dropSpiritStones(generatedSpiritStone, 1);
            }
         }
      }
   }

   private void spawnParticles(Level level, BlockPos pos) {
      if (level.getGameTime() % 2L == 0L
         && level.getNearestPlayer((double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5, 32.0, false) != null) {
         for (int i = 0; i < 10; i++) {
            level.addParticle(VEIN_PARTICLE, (double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5, 0.0, 0.0, 0.0);
         }
      }
   }

   public boolean releaseStoredSpiritStones() {
      if (this.storedSpiritStones <= 0 && this.storedMediumSpiritStones <= 0 && this.storedHighSpiritStones <= 0) {
         return false;
      } else {
         this.dropSpiritStones((Item)ModItems.SPIRIT_STONE.get(), this.storedSpiritStones);
         this.dropSpiritStones((Item)ModItems.MEDIUM_SPIRIT_STONE.get(), this.storedMediumSpiritStones);
         this.dropSpiritStones((Item)ModItems.HIGH_SPIRIT_STONE.get(), this.storedHighSpiritStones);
         this.storedSpiritStones = 0;
         this.storedMediumSpiritStones = 0;
         this.storedHighSpiritStones = 0;
         this.setChanged();
         return true;
      }
   }

   private boolean hasBlockAbove() {
      return this.level != null && !this.level.getBlockState(this.worldPosition.above()).isAir();
   }

   private Item getGeneratedSpiritStone(Level level) {
      int roll = level.random.nextInt(100);
      if (roll == 0) {
         return (Item)ModItems.HIGH_SPIRIT_STONE.get();
      } else {
         return roll <= 10 ? (Item)ModItems.MEDIUM_SPIRIT_STONE.get() : (Item)ModItems.SPIRIT_STONE.get();
      }
   }

   private void storeSpiritStone(Item item) {
      if (item == ModItems.HIGH_SPIRIT_STONE.get()) {
         this.storedHighSpiritStones++;
      } else if (item == ModItems.MEDIUM_SPIRIT_STONE.get()) {
         this.storedMediumSpiritStones++;
      } else {
         this.storedSpiritStones++;
      }
   }

   private void dropSpiritStones(Item item, int count) {
      if (this.level != null && count > 0) {
         Containers.dropItemStack(
            this.level,
            (double)this.worldPosition.getX() + 0.5,
            (double)this.worldPosition.getY() + 1.0,
            (double)this.worldPosition.getZ() + 0.5,
            new ItemStack(item, count)
         );
      }
   }

   protected void saveAdditional(CompoundTag tag, Provider registries) {
      super.saveAdditional(tag, registries);
      tag.putInt("tick_counter", this.tickCounter);
      tag.putInt("stored_spirit_stones", this.storedSpiritStones);
      tag.putInt("stored_medium_spirit_stones", this.storedMediumSpiritStones);
      tag.putInt("stored_high_spirit_stones", this.storedHighSpiritStones);
   }

   protected void loadAdditional(CompoundTag tag, Provider registries) {
      super.loadAdditional(tag, registries);
      this.tickCounter = tag.getInt("tick_counter");
      this.storedSpiritStones = tag.getInt("stored_spirit_stones");
      this.storedMediumSpiritStones = tag.getInt("stored_medium_spirit_stones");
      this.storedHighSpiritStones = tag.getInt("stored_high_spirit_stones");
   }
}
