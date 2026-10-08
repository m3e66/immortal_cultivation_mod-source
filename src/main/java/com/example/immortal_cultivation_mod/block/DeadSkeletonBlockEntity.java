package com.example.immortal_cultivation_mod.block;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.effect.NativeSpellEffects;
import com.example.immortal_cultivation_mod.spell.Weiya;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public final class DeadSkeletonBlockEntity extends RandomizableContainerBlockEntity
        implements GeoBlockEntity {
    public static final int SLOT_COUNT = 5;
    private static final String REALM_TAG = "CultivationRealm";
    private static final String DEFAULT_REALM =
            CultivationLevels.REALM_LIANQI + CultivationLevels.STAGE_EARLY;
    private static final int PRESSURE_INTERVAL_TICKS = 20;
    private static final RawAnimation POSE_FLAT = RawAnimation.begin()
            .thenLoop("animation.dead_skeleton.pose_0");
    private static final RawAnimation POSE_CURLED = RawAnimation.begin()
            .thenLoop("animation.dead_skeleton.pose_1");
    private static final RawAnimation POSE_FALLEN = RawAnimation.begin()
            .thenLoop("animation.dead_skeleton.pose_2");

    private String cultivationRealm = DEFAULT_REALM;
    private NonNullList<ItemStack> items =
            NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final AnimatableInstanceCache animationCache =
            GeckoLibUtil.createInstanceCache(this);

    public DeadSkeletonBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DEAD_SKELETON.get(), pos, state);
    }

    public String cultivationRealm() {
        return cultivationRealm;
    }

    /** Cycles cultivation stages only; a cultivator skeleton never becomes mortal. */
    public String cycleRealm() {
        List<String> levels = CultivationLevels.allLevels();
        int current = levels.indexOf(cultivationRealm);
        int next = current < 1 || current >= levels.size() - 1 ? 1 : current + 1;
        cultivationRealm = levels.get(next);
        setChangedAndSync();
        return cultivationRealm;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  DeadSkeletonBlockEntity skeleton) {
        if (!(level instanceof ServerLevel serverLevel)
                || serverLevel.getGameTime() % PRESSURE_INTERVAL_TICKS != 0L) {
            return;
        }
        NativeSpellEffects.deadSkeletonPressure(serverLevel, pos,
                CultivationLevels.getStageIndex(skeleton.cultivationRealm));
        Weiya.applyAmbientPressure(serverLevel, pos, skeleton.cultivationRealm);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable(
                "container." + ImmortalCultivationMod.MODID
                        + ".dead_skeleton");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new HopperMenu(containerId, inventory, this);
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    private void setChangedAndSync() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(),
                    net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString(REALM_TAG, cultivationRealm);
        if (!trySaveLootTable(tag)) {
            net.minecraft.world.ContainerHelper.saveAllItems(tag, items, registries);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        if (!tryLoadLootTable(tag)) {
            net.minecraft.world.ContainerHelper.loadAllItems(tag, items, registries);
        }
        String savedRealm = tag.getString(REALM_TAG);
        cultivationRealm = isCultivatorLevel(savedRealm) ? savedRealm : DEFAULT_REALM;
    }

    private static boolean isCultivatorLevel(String realm) {
        int index = CultivationLevels.allLevels().indexOf(realm);
        return index > 0;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putString(REALM_TAG, cultivationRealm);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        String syncedRealm = tag.getString(REALM_TAG);
        cultivationRealm = isCultivatorLevel(syncedRealm) ? syncedRealm : DEFAULT_REALM;
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
        controllers.add(new AnimationController<>(
                this,
                "corpse_pose",
                0,
                state -> state.setAndContinue(switch (
                        getBlockState().getValue(DeadSkeletonBlock.POSE)) {
                    case 1 -> POSE_CURLED;
                    case 2 -> POSE_FALLEN;
                    default -> POSE_FLAT;
                })));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}
