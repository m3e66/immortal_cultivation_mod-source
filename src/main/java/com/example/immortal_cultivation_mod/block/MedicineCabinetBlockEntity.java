package com.example.immortal_cultivation_mod.block;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.screen.MedicineDrawerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Persistent 3-slot storage for every individually animated cabinet drawer. */
public final class MedicineCabinetBlockEntity extends BlockEntity
        implements Container, GeoBlockEntity {
    public static final int SLOTS_PER_DRAWER = 3;
    public static final int MAX_DRAWERS = 27;
    public static final int MAX_SLOTS = MAX_DRAWERS * SLOTS_PER_DRAWER;

    private static final String ACTIVE_DRAWER_TAG = "ActiveDrawer";
    private final NonNullList<ItemStack> items =
            NonNullList.withSize(MAX_SLOTS, ItemStack.EMPTY);
    private final float[] drawerProgress = new float[MAX_DRAWERS];
    private final float[] previousDrawerProgress = new float[MAX_DRAWERS];
    private final AnimatableInstanceCache animationCache =
            GeckoLibUtil.createInstanceCache(this);
    private int activeDrawer = -1;

    public MedicineCabinetBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MEDICINE_CABINET.get(), pos, state);
    }

    public MedicineCabinetBlock.Variant variant() {
        return getBlockState().getBlock() instanceof MedicineCabinetBlock cabinet
                ? cabinet.variant()
                : MedicineCabinetBlock.Variant.SMALL;
    }

    public int activeDrawer() {
        return activeDrawer;
    }

    public void pullDrawer(int drawer) {
        if (level == null || level.isClientSide || !validDrawer(drawer)) return;
        activeDrawer = drawer;
        syncDrawerState();
        level.playSound(null, worldPosition, SoundEvents.BARREL_OPEN,
                SoundSource.BLOCKS, 0.7F, 0.86F);
    }

    public void openDrawer(ServerPlayer player, int drawer) {
        if (level == null || level.isClientSide || activeDrawer != drawer
                || !validDrawer(drawer) || !stillValid(player)) {
            return;
        }
        Container drawerInventory = new DrawerInventory(drawer);
        Component title = Component.translatable(
                "container." + ImmortalCultivationMod.MODID + ".medicine_drawer",
                drawer + 1);
        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, menuPlayer) -> new MedicineDrawerMenu(
                        containerId, inventory, drawerInventory, this, drawer),
                title));
        level.playSound(null, worldPosition, SoundEvents.CHEST_OPEN,
                SoundSource.BLOCKS, 0.55F, 1.12F);
    }

    public void closeDrawer(int drawer) {
        if (level == null || level.isClientSide || activeDrawer != drawer) return;
        activeDrawer = -1;
        syncDrawerState();
        level.playSound(null, worldPosition, SoundEvents.BARREL_CLOSE,
                SoundSource.BLOCKS, 0.65F, 0.92F);
    }

    private void syncDrawerState() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(),
                    Block.UPDATE_CLIENTS);
        }
    }

    private boolean validDrawer(int drawer) {
        return drawer >= 0 && drawer < variant().drawerCount();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state,
                                  MedicineCabinetBlockEntity cabinet) {
        for (int drawer = 0; drawer < MAX_DRAWERS; drawer++) {
            cabinet.previousDrawerProgress[drawer] = cabinet.drawerProgress[drawer];
            float target = drawer == cabinet.activeDrawer ? 1.0F : 0.0F;
            float current = cabinet.drawerProgress[drawer];
            current += (target - current) * 0.38F;
            if (Math.abs(target - current) < 0.006F) current = target;
            cabinet.drawerProgress[drawer] = current;
        }
    }

    public float drawerProgress(int drawer, float partialTick) {
        if (drawer < 0 || drawer >= MAX_DRAWERS) return 0.0F;
        return Mth.lerp(partialTick,
                previousDrawerProgress[drawer], drawerProgress[drawer]);
    }

    @Override
    public int getContainerSize() {
        return variant().slotCount();
    }

    @Override
    public boolean isEmpty() {
        for (int slot = 0; slot < getContainerSize(); slot++) {
            if (!items.get(slot).isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot >= 0 && slot < getContainerSize()
                ? items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot < 0 || slot >= getContainerSize()) return ItemStack.EMPTY;
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) setChanged();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return slot >= 0 && slot < getContainerSize()
                ? ContainerHelper.takeItem(items, slot) : ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= getContainerSize()) return;
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize(stack)) {
            stack.setCount(getMaxStackSize(stack));
        }
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(
                worldPosition.getX() + 0.5D,
                worldPosition.getY() + variant().blockHeight() * 0.5D,
                worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void clearContent() {
        for (int slot = 0; slot < items.size(); slot++) {
            items.set(slot, ItemStack.EMPTY);
        }
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int slot = 0; slot < items.size(); slot++) {
            items.set(slot, ItemStack.EMPTY);
        }
        ContainerHelper.loadAllItems(tag, items, registries);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt(ACTIVE_DRAWER_TAG, activeDrawer);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        activeDrawer = tag.getInt(ACTIVE_DRAWER_TAG);
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet,
                             HolderLookup.Provider registries) {
        handleUpdateTag(packet.getTag(), registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Drawer bones and their pull/push clips come from the MC Model Kit.
        // The chosen bone is blended from synced state in the GeoModel so a
        // different open drawer can smoothly close while the new one opens.
        controllers.add(new AnimationController<>(
                this, "medicine_drawers", 0, state -> PlayState.STOP));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    private final class DrawerInventory implements Container {
        private final int drawer;
        private final int firstSlot;

        private DrawerInventory(int drawer) {
            this.drawer = drawer;
            this.firstSlot = drawer * SLOTS_PER_DRAWER;
        }

        @Override
        public int getContainerSize() {
            return SLOTS_PER_DRAWER;
        }

        @Override
        public boolean isEmpty() {
            for (int slot = 0; slot < SLOTS_PER_DRAWER; slot++) {
                if (!getItem(slot).isEmpty()) return false;
            }
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            return MedicineCabinetBlockEntity.this.getItem(firstSlot + slot);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            return MedicineCabinetBlockEntity.this.removeItem(firstSlot + slot, amount);
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return MedicineCabinetBlockEntity.this.removeItemNoUpdate(firstSlot + slot);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            MedicineCabinetBlockEntity.this.setItem(firstSlot + slot, stack);
        }

        @Override
        public void setChanged() {
            MedicineCabinetBlockEntity.this.setChanged();
        }

        @Override
        public boolean stillValid(Player player) {
            return activeDrawer == drawer
                    && MedicineCabinetBlockEntity.this.stillValid(player);
        }

        @Override
        public void clearContent() {
            for (int slot = 0; slot < SLOTS_PER_DRAWER; slot++) {
                setItem(slot, ItemStack.EMPTY);
            }
        }
    }
}

