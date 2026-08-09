package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.item.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.EventHooks;

/** A single physical blood drop that can hold more than a vanilla ItemStack. */
public final class BloodDropEntity extends ItemEntity {
    private static final String TAG_AMOUNT = "BloodAmount";
    private static final EntityDataAccessor<Long> DATA_AMOUNT = SynchedEntityData.defineId(
            BloodDropEntity.class, EntityDataSerializers.LONG);

    public BloodDropEntity(EntityType<? extends BloodDropEntity> entityType, Level level) {
        super(entityType, level);
        refreshVisualStack();
    }

    public BloodDropEntity(Level level, double x, double y, double z, long amount) {
        this(ModEntities.BLOOD_DROP.get(), level);
        setPos(x, y, z);
        setAmount(amount);
        setDeltaMovement(
                random.nextDouble() * 0.2D - 0.1D,
                0.2D,
                random.nextDouble() * 0.2D - 0.1D);
        setDefaultPickUpDelay();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_AMOUNT, 1L);
    }

    public long getAmount() {
        return entityData.get(DATA_AMOUNT);
    }

    public void setAmount(long amount) {
        entityData.set(DATA_AMOUNT, Math.max(0L, amount));
        refreshVisualStack();
    }

    private void refreshVisualStack() {
        ItemStack visualStack = new ItemStack(ModItems.BLOOD.get());
        // Count one plus a max size of one prevents vanilla ItemEntity merging and renders one drop.
        visualStack.set(DataComponents.MAX_STACK_SIZE, 1);
        super.setItem(visualStack);
    }

    @Override
    public void playerTouch(Player player) {
        if (level().isClientSide || getAmount() <= 0L) {
            return;
        }

        var pickupResult = EventHooks.fireItemPickupPre(this, player).canPickup();
        if (pickupResult.isFalse() || (!pickupResult.isTrue() && hasPickUpDelay())) {
            return;
        }

        long remaining = getAmount();
        long pickedUp = 0L;
        int maxStackSize = ModItems.BLOOD.get().getDefaultMaxStackSize();
        while (remaining > 0L) {
            int offeredCount = (int) Math.min((long) maxStackSize, remaining);
            ItemStack offered = new ItemStack(ModItems.BLOOD.get(), offeredCount);
            ItemStack original = offered.copy();
            if (!player.getInventory().add(offered)) {
                break;
            }

            int transferred = offeredCount - offered.getCount();
            if (transferred <= 0) {
                break;
            }
            EventHooks.fireItemPickupPost(this, player, original);
            remaining -= transferred;
            pickedUp += transferred;
            if (!offered.isEmpty()) {
                break;
            }
        }

        if (pickedUp <= 0L) {
            return;
        }

        int reportedCount = (int) Math.min((long) Integer.MAX_VALUE, pickedUp);
        player.take(this, reportedCount);
        player.awardStat(Stats.ITEM_PICKED_UP.get(ModItems.BLOOD.get()), reportedCount);
        player.onItemPickup(this);
        if (remaining <= 0L) {
            discard();
        } else {
            setAmount(remaining);
        }
    }

    @Override
    public BloodDropEntity copy() {
        BloodDropEntity copy = new BloodDropEntity(ModEntities.BLOOD_DROP.get(), level());
        copy.restoreFrom(this);
        copy.setAmount(getAmount());
        return copy;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putLong(TAG_AMOUNT, getAmount());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        long loadedAmount = tag.contains(TAG_AMOUNT)
                ? tag.getLong(TAG_AMOUNT)
                : Math.max(1, getItem().getCount());
        setAmount(Math.max(1L, loadedAmount));
    }
}
