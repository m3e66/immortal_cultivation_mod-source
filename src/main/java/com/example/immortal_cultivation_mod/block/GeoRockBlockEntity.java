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
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class GeoRockBlockEntity extends BlockEntity implements GeoBlockEntity {
    private static final String STELE_SPELL_TAG = "SteleSpell";
    private static final String QINGTONG_DING_MODEL = "qingtong_yinhuo_ding";
    private static final String CU_TIE_DING_MODEL = "cu_tie_xiaoding";
    private static final String SANYANG_DING_MODEL = "chi_jin_sanyang_ding";
    private String dingAnimationMode = "";
    private RawAnimation dingAnimation = RawAnimation.begin().thenLoop("idle");
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
                    String model = modelName();
                    if (!QINGTONG_DING_MODEL.equals(model) && !CU_TIE_DING_MODEL.equals(model)
                            && !SANYANG_DING_MODEL.equals(model)) {
                        return PlayState.STOP;
                    }
                    return state.setAndContinue(updateDingAnimation());
                }));
    }

    RawAnimation updateDingAnimation() {
        String desired = "idle";
        if (this instanceof DingBlockEntity ding) {
            if (ding.hasLid() && ding.isLidOpen()) desired = "open";
            else if (ding.isForging()) desired = "forge";
            else if (ding.isFinishingAnimation()) desired = "finish";
            else if (ding.hasFireBelow()) desired = "ignite";
        }
        if (!desired.equals(dingAnimationMode)) {
            RawAnimation next = RawAnimation.begin();
            // thenPlay uses the JSON loop type; both lid clips hold their last frame.
            // Closing must finish so the queued heat/forge animation can run.
            if (dingAnimationMode.equals("open")) next.then("close", Animation.LoopType.PLAY_ONCE);
            if (desired.equals("open")) next.thenPlayAndHold("open");
            else if (desired.equals("finish")) next.thenPlay("finish");
            else next.thenLoop(desired);
            dingAnimation = next;
            dingAnimationMode = desired;
        }
        return dingAnimation;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

}
