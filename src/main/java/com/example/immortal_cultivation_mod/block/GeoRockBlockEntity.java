package com.example.immortal_cultivation_mod.block;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class GeoRockBlockEntity extends BlockEntity implements GeoBlockEntity {
    private static final String STELE_SPELL_TAG = "SteleSpell";
    private static final String QINGTONG_DING_MODEL = "qingtong_yinhuo_ding";
    private static final RawAnimation QINGTONG_DING_IDLE =
            RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation QINGTONG_DING_IGNITE =
            RawAnimation.begin().thenLoop("ignite");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private String steleSpellId = "";

    public GeoRockBlockEntity(BlockPos pos, BlockState blockState) {
        this(ModBlockEntities.GEO_ROCK.get(), pos, blockState);
    }

    protected GeoRockBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public String modelName() {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(getBlockState().getBlock());
        if (ImmortalCultivationMod.MODID.equals(key.getNamespace())) {
            return key.getPath();
        }
        return "jingtie_motherstone";
    }

    public String steleSpellId() {
        return steleSpellId;
    }

    public void setSteleSpellId(String spellId) {
        steleSpellId = spellId == null ? "" : spellId;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!steleSpellId.isBlank()) {
            tag.putString(STELE_SPELL_TAG, steleSpellId);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        steleSpellId = tag.getString(STELE_SPELL_TAG);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(
                this,
                "qingtong_ding",
                0,
                state -> {
                    if (!QINGTONG_DING_MODEL.equals(modelName())) {
                        return PlayState.STOP;
                    }
                    return state.setAndContinue(
                            this instanceof DingBlockEntity ding
                                    && ding.hasFireBelow()
                                    ? QINGTONG_DING_IGNITE
                                    : QINGTONG_DING_IDLE);
                }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

}
