package com.example.immortal_cultivation_mod.block;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import com.example.immortal_cultivation_mod.item.ForgingSystem;
import com.example.immortal_cultivation_mod.item.TalismanDrawingPatterns;
import com.example.immortal_cultivation_mod.item.TalismanFormulaItem;
import com.example.immortal_cultivation_mod.item.TalismanItem;
import com.example.immortal_cultivation_mod.item.TalismanMaterialItem;
import com.example.immortal_cultivation_mod.item.TalismanRecipes;
import com.example.immortal_cultivation_mod.screen.TalismanDrawingMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Server authority for talisman material validation, Qi payment, scoring, and output. */
public final class TalismanDrawingTableBlockEntity extends BlockEntity
        implements Container, MenuProvider, GeoBlockEntity {
    public static final int PAPER_SLOT = 0;
    public static final int INK_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;
    public static final int FORMULA_SLOT = 3;
    public static final int SLOT_COUNT = 4;
    public static final int OUTPUT_STACK_LIMIT = 16;

    public static final int RESULT_NONE = 0;
    public static final int RESULT_SUCCESS = 1;
    public static final int RESULT_INVALID_FORMULA = 2;
    public static final int RESULT_NOT_LEARNED = 3;
    public static final int RESULT_PAPER_TOO_WEAK = 4;
    public static final int RESULT_INK_TOO_WEAK = 5;
    public static final int RESULT_PROFICIENCY_TOO_LOW = 6;
    public static final int RESULT_NOT_ENOUGH_QI = 7;
    public static final int RESULT_INACCURATE_DRAWING = 8;
    public static final int RESULT_OUTPUT_BLOCKED = 9;
    public static final int RESULT_NOT_MASTERED = 10;
    public static final int RESULT_DRAWING_NOT_STARTED = 11;

    private static final float DRAWING_QUALITY_WEIGHT = 0.60F;
    private static final float PAPER_QUALITY_WEIGHT = 0.15F;
    private static final float INK_QUALITY_WEIGHT = 0.15F;
    private static final float QI_CONTROL_WEIGHT = 0.10F;
    private static final String DRAW_CONTROLLER = "drawing";
    private static final String DRAW_TRIGGER = "draw";
    private static final RawAnimation DRAW_ANIMATION = RawAnimation.begin()
            .thenPlay("animation.talisman_drawing_table.draw");
    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final Map<UUID, DrawingAttempt> activeDrawings = new HashMap<>();
    private final AnimatableInstanceCache animationCache =
            GeckoLibUtil.createInstanceCache(this);
    private int resultNonce;
    private int lastResult;
    private int lastAccuracy;
    private int lastQuality;

    public TalismanDrawingTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TALISMAN_DRAWING_TABLE.get(), pos, state);
    }

    /**
     * Commits the physical materials as soon as the first brush stroke begins.
     * Their effective quality is retained for the eventual result even when the
     * consumed stack becomes empty. Cancelling or failing does not refund them.
     */
    public void beginDrawing(ServerPlayer player, String recipeId) {
        if (level == null || level.isClientSide || !stillValid(player)) return;
        TalismanRecipes.Def recipe = TalismanRecipes.byId(recipeId);
        if (recipe == null) {
            rejectBeginning(player, RESULT_INVALID_FORMULA);
            return;
        }

        UUID playerId = player.getUUID();
        DrawingAttempt current = activeDrawings.get(playerId);
        if (current != null && current.recipeId().equals(recipe.id())) return;
        activeDrawings.remove(playerId);

        if (!recipe.id().equals(TalismanFormulaItem.recipeId(getItem(FORMULA_SLOT)))) {
            rejectBeginning(player, RESULT_NOT_LEARNED);
            return;
        }
        ItemStack paper = getItem(PAPER_SLOT);
        ItemStack ink = getItem(INK_SLOT);
        if (!TalismanRecipes.acceptsPaper(recipe, paper)) {
            rejectBeginning(player, RESULT_PAPER_TOO_WEAK);
            return;
        }
        if (!TalismanRecipes.acceptsInk(recipe, ink)) {
            rejectBeginning(player, RESULT_INK_TOO_WEAK);
            return;
        }

        ModAttachments.CultivationData data = ModAttachments.getData(player);
        if (!player.getAbilities().instabuild
                && TalismanRecipes.masterTier(data) < recipe.masterTier()) {
            rejectBeginning(player, RESULT_PROFICIENCY_TOO_LOW);
            return;
        }
        if (!getItem(OUTPUT_SLOT).isEmpty()) {
            rejectBeginning(player, RESULT_OUTPUT_BLOCKED);
            return;
        }
        if (!player.getAbilities().instabuild && data.qi() < recipe.qiCost()) {
            rejectBeginning(player, RESULT_NOT_ENOUGH_QI);
            return;
        }

        int seed = playerId.hashCode() ^ worldPosition.hashCode()
                ^ (int) level.getGameTime() ^ recipe.id().hashCode();
        ForgingSystem.ensurePurity(paper, seed);
        ForgingSystem.ensurePurity(ink, Integer.rotateLeft(seed, 13));
        int paperQuality = effectiveMaterialQuality(
                paper, recipe.minPaperTier());
        int inkQuality = effectiveMaterialQuality(
                ink, recipe.minInkTier());

        if (!player.getAbilities().instabuild) {
            paper.shrink(1);
            ink.shrink(1);
        }
        activeDrawings.put(playerId,
                new DrawingAttempt(recipe.id(), paperQuality, inkQuality));
        triggerAnim(DRAW_CONTROLLER, DRAW_TRIGGER);
        setChanged();
        player.containerMenu.broadcastChanges();
    }

    public void cancelDrawing(ServerPlayer player) {
        activeDrawings.remove(player.getUUID());
    }

    private void rejectBeginning(ServerPlayer player, int result) {
        activeDrawings.remove(player.getUUID());
        finishAttempt(result, 0, 0, player);
    }

    public void draw(ServerPlayer player, String recipeId, List<Integer> encodedTrace,
                     int qiControl, boolean instant) {
        if (level == null || level.isClientSide || !stillValid(player)) return;
        TalismanRecipes.Def recipe = TalismanRecipes.byId(recipeId);
        if (recipe == null) {
            activeDrawings.remove(player.getUUID());
            finishAttempt(RESULT_INVALID_FORMULA, 0, 0, player);
            return;
        }

        ModAttachments.CultivationData data = ModAttachments.getData(player);
        boolean mastered = TalismanRecipes.isMastered(data, recipe.id());
        if (instant && !mastered) {
            finishAttempt(RESULT_NOT_MASTERED, 0, 0, player);
            return;
        }

        DrawingAttempt drawingAttempt = null;
        ItemStack paper = ItemStack.EMPTY;
        ItemStack ink = ItemStack.EMPTY;
        if (instant) {
            paper = getItem(PAPER_SLOT);
            ink = getItem(INK_SLOT);
            if (!TalismanRecipes.acceptsPaper(recipe, paper)) {
                finishAttempt(RESULT_PAPER_TOO_WEAK, 0, 0, player);
                return;
            }
            if (!TalismanRecipes.acceptsInk(recipe, ink)) {
                finishAttempt(RESULT_INK_TOO_WEAK, 0, 0, player);
                return;
            }
        } else {
            if (!recipe.id().equals(TalismanFormulaItem.recipeId(getItem(FORMULA_SLOT)))) {
                activeDrawings.remove(player.getUUID());
                finishAttempt(RESULT_NOT_LEARNED, 0, 0, player);
                return;
            }
            drawingAttempt = activeDrawings.remove(player.getUUID());
            if (drawingAttempt == null || !drawingAttempt.recipeId().equals(recipe.id())) {
                finishAttempt(RESULT_DRAWING_NOT_STARTED, 0, 0, player);
                return;
            }
        }
        if (!player.getAbilities().instabuild
                && TalismanRecipes.masterTier(data) < recipe.masterTier()) {
            finishAttempt(RESULT_PROFICIENCY_TOO_LOW, 0, 0, player);
            return;
        }

        int accuracy = instant ? 100 : TalismanDrawingPatterns.score(recipe.id(), encodedTrace);
        if (accuracy < minimumDrawingAccuracy(recipe.strengthTier())) {
            finishAttempt(RESULT_INACCURATE_DRAWING, accuracy, 0, player);
            return;
        }
        ItemStack currentOutput = getItem(OUTPUT_SLOT);
        if (!currentOutput.isEmpty()
                && (!instant || currentOutput.getCount() >= OUTPUT_STACK_LIMIT)) {
            finishAttempt(RESULT_OUTPUT_BLOCKED, accuracy, 0, player);
            return;
        }
        if (!player.getAbilities().instabuild && data.qi() < recipe.qiCost()) {
            finishAttempt(RESULT_NOT_ENOUGH_QI, accuracy, 0, player);
            return;
        }

        int paperQuality;
        int inkPurity;
        if (instant) {
            int seed = player.getUUID().hashCode() ^ worldPosition.hashCode()
                    ^ (int) level.getGameTime() ^ recipe.id().hashCode();
            ForgingSystem.ensurePurity(paper, seed);
            ForgingSystem.ensurePurity(ink, Integer.rotateLeft(seed, 13));
            paperQuality = effectiveMaterialQuality(paper, recipe.minPaperTier());
            inkPurity = effectiveMaterialQuality(ink, recipe.minInkTier());
        } else {
            paperQuality = drawingAttempt.paperQuality();
            inkPurity = drawingAttempt.inkQuality();
        }
        int control = instant ? 100 : Mth.clamp(qiControl, 0, 100);
        int score = Mth.clamp(Math.round(
                accuracy * DRAWING_QUALITY_WEIGHT
                        + paperQuality * PAPER_QUALITY_WEIGHT
                        + inkPurity * INK_QUALITY_WEIGHT
                        + control * QI_CONTROL_WEIGHT), 1, 100);
        TalismanItem.Quality quality = qualityFor(score);

        ItemStack result = recipe.result();
        TalismanItem.setQuality(result, quality);
        TalismanItem.setDrawingData(result, recipe.id(), paperQuality, inkPurity,
                accuracy, control, score);
        if (!currentOutput.isEmpty()
                && (!ItemStack.isSameItemSameComponents(currentOutput, result)
                || currentOutput.getCount() + result.getCount() > OUTPUT_STACK_LIMIT)) {
            finishAttempt(RESULT_OUTPUT_BLOCKED, accuracy, 0, player);
            return;
        }

        ModAttachments.CultivationData updated = data;
        if (!player.getAbilities().instabuild) {
            if (instant) {
                paper.shrink(1);
                ink.shrink(1);
            }
            updated = updated.withQi(data.qi() - recipe.qiCost());
        }
        updated = updated.withAddedMethodProficiency(TalismanRecipes.DRAWING_PROFICIENCY,
                Math.max(1, recipe.strengthTier() * 4 + accuracy / 20));
        int successesBefore = TalismanRecipes.successCount(data, recipe.id());
        int masterySuccesses = TalismanRecipes.masterySuccesses(recipe);
        if (!instant) {
            updated = updated.withAddedMethodProficiency(TalismanRecipes.successKey(recipe.id()), 1);
        }
        if (updated != data) {
            ModAttachments.setData(player, updated);
            ServerEvents.syncPlayerData(player);
        }
        if (!instant && successesBefore < masterySuccesses
                && successesBefore + 1 >= masterySuccesses) {
            player.displayClientMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID + ".talisman_mastery.unlocked",
                    recipe.result().getHoverName()), true);
        }
        placeResult(result);
        if (instant) {
            triggerAnim(DRAW_CONTROLLER, DRAW_TRIGGER);
        }
        setChanged();
        playSuccessEffects(quality);
        finishAttempt(RESULT_SUCCESS, accuracy, quality.id(), player);
    }

    private void placeResult(ItemStack result) {
        ItemStack current = items.get(OUTPUT_SLOT);
        if (current.isEmpty()) {
            items.set(OUTPUT_SLOT, result);
        } else {
            current.grow(result.getCount());
        }
    }

    private static int effectiveMaterialQuality(ItemStack stack, int minimumTier) {
        return Mth.clamp(ForgingSystem.purity(stack)
                + (TalismanMaterialItem.tier(stack) - minimumTier) * 8, 1, 100);
    }

    private static int minimumDrawingAccuracy(int strengthTier) {
        return switch (Mth.clamp(strengthTier, 1, 6)) {
            case 1 -> 45;
            case 2 -> 50;
            case 3 -> 55;
            case 4 -> 60;
            case 5 -> 65;
            default -> 70;
        };
    }

    private static TalismanItem.Quality qualityFor(int score) {
        if (score < 50) return TalismanItem.Quality.LOW;
        if (score < 70) return TalismanItem.Quality.MID;
        if (score < 88) return TalismanItem.Quality.HIGH;
        return TalismanItem.Quality.SUPREME;
    }

    private void finishAttempt(int result, int accuracy, int quality, ServerPlayer player) {
        lastResult = result;
        lastAccuracy = Mth.clamp(accuracy, 0, 100);
        lastQuality = Mth.clamp(quality, 0, 3);
        resultNonce++;
        setChanged();
        player.containerMenu.broadcastChanges();
    }

    private void playSuccessEffects(TalismanItem.Quality quality) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        double x = worldPosition.getX() + 0.5D;
        double y = worldPosition.getY() + 1.05D;
        double z = worldPosition.getZ() + 0.5D;
        int count = 14 + quality.id() * 7;
        serverLevel.sendParticles(ParticleTypes.ENCHANT, x, y, z,
                count, 0.38D, 0.18D, 0.38D, 0.04D);
        serverLevel.sendParticles(ParticleTypes.FLAME, x, y + 0.12D, z,
                6 + quality.id() * 2, 0.18D, 0.12D, 0.18D, 0.01D);
        serverLevel.playSound(null, worldPosition, SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.BLOCKS, 0.8F, 1.15F + quality.id() * 0.08F);
    }

    public int data(int index) {
        return switch (index) {
            case 0 -> resultNonce;
            case 1 -> lastResult;
            case 2 -> lastAccuracy;
            case 3 -> lastQuality;
            default -> 0;
        };
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container." + ImmortalCultivationMod.MODID
                + ".talisman_drawing_table");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new TalismanDrawingMenu(containerId, inventory, this);
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) if (!stack.isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) setChanged();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize(stack)) stack.setCount(getMaxStackSize(stack));
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && (player.distanceToSqr(worldPosition.getX() + 0.5D,
                worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 64.0D
                || com.example.immortal_cultivation_mod.item.QiPouchWorld
                .isRemoteInteraction(player, level, worldPosition));
    }

    @Override
    public void clearContent() {
        activeDrawings.clear();
        items.clear();
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
        items.clear();
        ContainerHelper.loadAllItems(tag, items, registries);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(
                this,
                DRAW_CONTROLLER,
                0,
                state -> PlayState.STOP)
                .triggerableAnim(DRAW_TRIGGER, DRAW_ANIMATION));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    private record DrawingAttempt(String recipeId, int paperQuality, int inkQuality) {
    }
}
