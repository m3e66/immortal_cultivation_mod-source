package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.entity.NpcProfile;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import com.example.immortal_cultivation_mod.network.ModPayloads;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public final class IdentityItems {
    private static final String RECOVERY_TICKS = ImmortalCultivationMod.MODID + ".qi_recovery_ticks";
    private static final String RECOVERY_RATE = ImmortalCultivationMod.MODID + ".qi_recovery_rate";
    private static final String BUFF_PERCENT = ImmortalCultivationMod.MODID + ".sharpen_percent";
    private static final String BUFF_HITS = ImmortalCultivationMod.MODID + ".sharpen_hits";
    private static final String BUFF_LAST_HIT = ImmortalCultivationMod.MODID + ".sharpen_last_hit";
    public static final String PLAYER_IDENTITY = "ImmortalCultivationPlayerIdentity";
    public static final String FORGING_PROFICIENCY = "craft_forging";
    private static final String SWORD_SWINGS = "ImmortalCultivationSwordSwings";
    private static final String SABER_SWINGS = "ImmortalCultivationSaberSwings";
    private static final String MELEE_SWINGS = "ImmortalCultivationMeleeSwings";
    private static final String NPC_KILLS = "ImmortalCultivationNpcKills";
    private static final String WAS_SWINGING = "ImmortalCultivationWasSwinging";
    private static final String LAST_SWING_TIME = "ImmortalCultivationLastSwingTime";
    private static final int WEAPON_SWINGS_REQUIRED = 1_000;
    private static final int NPC_KILLS_REQUIRED = 50;
    private static final int CRAFT_PROFICIENCY_REQUIRED = 10;
    private static final String POISONED_FOOD = ImmortalCultivationMod.MODID + ".lethal_poison";

    private IdentityItems() {
    }

    public static final class BiguPillItem extends com.example.immortal_cultivation_mod.alchemy.LegacyPillItem {
        public BiguPillItem(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player,
                                                       InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (!level.isClientSide) {
                if (player instanceof ServerPlayer sp) com.example.immortal_cultivation_mod.fate.FateCrafting.consumedPill(sp, stack);
                player.getFoodData().setFoodLevel(20);
                player.getFoodData().setSaturation(20.0F);
                consumeOne(player, stack);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
    }

    public static final class QiRecoveryPillItem extends Item {
        private final int qiPerSecond;

        public QiRecoveryPillItem(Properties properties, int qiPerSecond) {
            super(properties);
            this.qiPerSecond = qiPerSecond;
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player,
                                                       InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.getPersistentData().putInt(RECOVERY_TICKS, 100);
                serverPlayer.getPersistentData().putInt(RECOVERY_RATE, com.example.immortal_cultivation_mod.fate.FateCrafting.pillRate(stack, qiPerSecond));
                com.example.immortal_cultivation_mod.fate.FateCrafting.consumedPill(serverPlayer, stack);
                consumeOne(player, stack);
                serverPlayer.displayClientMessage(Component.translatable(
                        "message." + ImmortalCultivationMod.MODID + ".qi_recovery_started", qiPerSecond), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
    }

    public static final class IdentityTokenItem extends Item {
        private final String identity;

        public IdentityTokenItem(Properties properties, String identity) {
            super(properties);
            this.identity = identity;
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (!level.isClientSide) {
                player.getPersistentData().putString(PLAYER_IDENTITY, identity);
                player.displayClientMessage(Component.translatable(
                        "message." + ImmortalCultivationMod.MODID + ".identity_set",
                        Component.translatable("screen." + ImmortalCultivationMod.MODID + ".npc.identity." + identity)), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
    }

    public static final class ProtectivePendantItem extends Item implements GeoItem, ICurioItem {
        private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

        public ProtectivePendantItem(Properties properties) {
            super(properties);
        }

        @Override
        public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
            consumer.accept(new GeoRenderProvider() {
                private ProtectiveJadePendantItemRenderer renderer;

                @Override
                public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                    if (renderer == null) {
                        renderer = new ProtectiveJadePendantItemRenderer();
                    }
                    return renderer;
                }
            });
        }

        @Override
        public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        }

        @Override
        public AnimatableInstanceCache getAnimatableInstanceCache() {
            return cache;
        }

        @Override
        public boolean canEquip(SlotContext slotContext, ItemStack stack) {
            return "waist_accessory".equals(slotContext.identifier());
        }
    }

    public static final class LethalPoisonItem extends Item {
        public LethalPoisonItem(Properties properties) {
            super(properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,
                                    TooltipFlag flag) {
            tooltip.add(Component.translatable(
                    "tooltip." + ImmortalCultivationMod.MODID + ".lethal_poison"));
        }
    }

    public static void copyPlayerIdentity(Player original, Player copy) {
        if (original.getPersistentData().contains(PLAYER_IDENTITY)) {
            copy.getPersistentData().putString(PLAYER_IDENTITY,
                    original.getPersistentData().getString(PLAYER_IDENTITY));
        }
        copyIntProgress(original, copy, SWORD_SWINGS);
        copyIntProgress(original, copy, SABER_SWINGS);
        copyIntProgress(original, copy, MELEE_SWINGS);
        copyIntProgress(original, copy, NPC_KILLS);
    }

    public static String ensurePlayerIdentity(Player player) {
        CompoundTag tag = player.getPersistentData();
        String identity = NpcProfile.normalizeIdentity(tag.getString(PLAYER_IDENTITY));
        if (identity.isBlank()) {
            identity = "wandering_cultivator";
        }
        tag.putString(PLAYER_IDENTITY, identity);
        return identity;
    }

    public static void tickIdentityProgress(ServerPlayer player) {
        boolean identityChanged = trackWeaponSwing(player);
        if (!identityChanged && player.tickCount % 20 == 0) {
            identityChanged = tryEarnSkillIdentity(player);
        }
        if (identityChanged) {
            ServerEvents.syncPlayerData(player);
        }
    }

    public static void recordNpcKill(ServerPlayer player) {
        if (!isWanderingCultivator(player)) {
            return;
        }
        int kills = increment(player.getPersistentData(), NPC_KILLS);
        if (kills >= NPC_KILLS_REQUIRED && setEarnedIdentity(player, "demonic_cultivator")) {
            ServerEvents.syncPlayerData(player);
        }
    }

    public static void recordSuccessfulForging(ServerPlayer player) {
        var data = ModAttachments.getData(player).withAddedMethodProficiency(FORGING_PROFICIENCY, 1);
        ModAttachments.setData(player, data);
        tryEarnSkillIdentity(player);
        ServerEvents.syncPlayerData(player);
    }

    private static boolean trackWeaponSwing(ServerPlayer player) {
        CompoundTag tag = player.getPersistentData();
        boolean swinging = player.swinging;
        int swingTime = player.swingTime;
        boolean startedSwing = swinging
                && (!tag.getBoolean(WAS_SWINGING) || swingTime < tag.getInt(LAST_SWING_TIME));
        tag.putBoolean(WAS_SWINGING, swinging);
        tag.putInt(LAST_SWING_TIME, swingTime);
        if (!startedSwing || !isWanderingCultivator(player)) {
            return false;
        }

        InteractionHand hand = player.swingingArm == null ? InteractionHand.MAIN_HAND : player.swingingArm;
        ItemStack weapon = player.getItemInHand(hand);
        if (!QiInfusedWeapon.isWeapon(weapon)) {
            return false;
        }

        int meleeSwings = increment(tag, MELEE_SWINGS);
        if (isSaberWeapon(weapon)) {
            int saberSwings = increment(tag, SABER_SWINGS);
            if (saberSwings >= WEAPON_SWINGS_REQUIRED
                    && setEarnedIdentity(player, "saber_cultivator")) {
                return true;
            }
        } else if (isSwordWeapon(weapon)) {
            int swordSwings = increment(tag, SWORD_SWINGS);
            if (swordSwings >= WEAPON_SWINGS_REQUIRED
                    && setEarnedIdentity(player, "sword_cultivator")) {
                return true;
            }
        }
        return meleeSwings >= WEAPON_SWINGS_REQUIRED
                && setEarnedIdentity(player, "body_cultivator");
    }

    private static boolean tryEarnSkillIdentity(ServerPlayer player) {
        if (!isWanderingCultivator(player)) {
            return false;
        }
        var data = ModAttachments.getData(player);
        if (data.methodProficiency(FORGING_PROFICIENCY) >= CRAFT_PROFICIENCY_REQUIRED) {
            return setEarnedIdentity(player, "artifact_cultivator");
        }
        if (data.spellProficiency(ModSpells.KONGSHI_SHU) >= CRAFT_PROFICIENCY_REQUIRED) {
            return setEarnedIdentity(player, "corpse_cultivator");
        }
        return false;
    }

    private static boolean setEarnedIdentity(ServerPlayer player, String identity) {
        if (!isWanderingCultivator(player)) {
            return false;
        }
        player.getPersistentData().putString(PLAYER_IDENTITY, identity);
        player.displayClientMessage(Component.translatable(
                "message." + ImmortalCultivationMod.MODID + ".identity_set",
                Component.translatable("screen." + ImmortalCultivationMod.MODID + ".npc.identity." + identity)), true);
        return true;
    }

    private static boolean isWanderingCultivator(Player player) {
        return "wandering_cultivator".equals(ensurePlayerIdentity(player));
    }

    private static boolean isSwordWeapon(ItemStack stack) {
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return stack.is(ItemTags.SWORDS)
                || path.contains("sword")
                || path.equals("jian")
                || path.endsWith("_jian");
    }

    private static boolean isSaberWeapon(ItemStack stack) {
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return path.equals("dao")
                || path.startsWith("dao_")
                || path.endsWith("_dao")
                || path.contains("saber");
    }

    private static int increment(CompoundTag tag, String key) {
        int value = tag.getInt(key);
        if (value < Integer.MAX_VALUE) {
            value++;
            tag.putInt(key, value);
        }
        return value;
    }

    private static void copyIntProgress(Player original, Player copy, String key) {
        if (original.getPersistentData().contains(key)) {
            copy.getPersistentData().putInt(key, original.getPersistentData().getInt(key));
        }
    }

    public static boolean tryUseProtectivePendant(ServerPlayer player) {
        ItemStack pendant = ItemStack.EMPTY;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack candidate = player.getInventory().getItem(slot);
            if (candidate.is(ModItems.PROTECTIVE_JADE_PENDANT.get())) {
                pendant = candidate;
                break;
            }
        }
        if (pendant.isEmpty()) {
            pendant = CuriosApi.getCuriosInventory(player)
                    .flatMap(handler -> handler.findFirstCurio(ModItems.PROTECTIVE_JADE_PENDANT.get()))
                    .filter(result -> "waist_accessory".equals(result.slotContext().identifier()))
                    .map(result -> result.stack())
                    .orElse(ItemStack.EMPTY);
        }
        if (pendant.isEmpty()) {
            return false;
        }
        if (!player.getAbilities().instabuild) {
            pendant.shrink(1);
        }
        player.setHealth(1.0F);
        player.removeAllEffects();
        player.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.HEAL,
                20 * 10, 4, false, true, true));
        PacketDistributor.sendToPlayer(player, new ModPayloads.ClientboundProtectivePendantActivationPayload());
        return true;
    }

    public static boolean poisonHeldFood(ServerPlayer player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return false;
        }
        ItemStack food = player.getMainHandItem();
        ItemStack poison = player.getOffhandItem();
        if (food.get(DataComponents.FOOD) == null || !poison.is(ModItems.LETHAL_POISON.get())) {
            return false;
        }
        ItemStack poisonedFood = food.copyWithCount(1);
        CompoundTag root = poisonedFood.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (root.getBoolean(POISONED_FOOD)) {
            return false;
        }
        root.putBoolean(POISONED_FOOD, true);
        poisonedFood.set(DataComponents.CUSTOM_DATA, CustomData.of(root));

        if (food.getCount() > 1) {
            ItemStack remainder = food.copy();
            remainder.shrink(1);
            player.setItemInHand(InteractionHand.MAIN_HAND, poisonedFood);
            player.getInventory().placeItemBackInInventory(remainder);
        } else {
            player.setItemInHand(InteractionHand.MAIN_HAND, poisonedFood);
        }
        consumeOne(player, poison);
        player.displayClientMessage(Component.translatable(
                "message." + ImmortalCultivationMod.MODID + ".food_poisoned"), true);
        return true;
    }

    public static void applyPoisonedFoodEffect(LivingEntity entity, ItemStack consumed) {
        CompoundTag root = consumed.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (root.getBoolean(POISONED_FOOD)) {
            entity.addEffect(new MobEffectInstance(ModEffects.SHIDU, 20 * 3, 4, false, true, true));
        }
    }

    public static void tickQiRecovery(ServerPlayer player) {
        CompoundTag tag = player.getPersistentData();
        int ticks = tag.getInt(RECOVERY_TICKS);
        if (ticks <= 0) {
            return;
        }
        tag.putInt(RECOVERY_TICKS, ticks - 1);
        if (player.tickCount % 20 != 0) {
            return;
        }
        var data = ModAttachments.getData(player);
        int maxQi = Math.max(0, CultivationLevels.getLevelDef(data.cultivationLevel()).maxQi() + data.maxQiBonus());
        if (maxQi > 0) {
            ModAttachments.setData(player, data.withQi(Math.min(maxQi, data.qi() + tag.getInt(RECOVERY_RATE))));
            ServerEvents.syncPlayerData(player);
        }
        if (ticks <= 1) {
            tag.remove(RECOVERY_TICKS);
            tag.remove(RECOVERY_RATE);
        }
    }

    public static boolean useWeaponCareStone(ServerPlayer player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return false;
        }
        ItemStack weapon = player.getMainHandItem();
        ItemStack stone = player.getOffhandItem();
        if (!QiInfusedWeapon.isWeapon(weapon) || stone.isEmpty()) {
            return false;
        }
        StoneAction action = stoneAction(stone);
        if (action == null || (action.swordOnly() && !isSword(weapon))
                || (action.saberOnly() && !isSaber(weapon))) {
            return false;
        }
        if (action.repair()) {
            if (!weapon.isDamageableItem() || !weapon.isDamaged()) {
                return false;
            }
            int repair = Math.max(1, weapon.getMaxDamage() * action.percent() / 100);
            weapon.setDamageValue(Math.max(0, weapon.getDamageValue() - repair));
        } else {
            CompoundTag tag = weapon.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            tag.putInt(BUFF_PERCENT, action.percent());
            tag.putInt(BUFF_HITS, 20);
            tag.remove(BUFF_LAST_HIT);
            weapon.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
        damageStone(stone);
        player.displayClientMessage(Component.translatable(
                "message." + ImmortalCultivationMod.MODID
                        + (action.repair() ? ".weapon_repaired" : ".weapon_sharpened"), action.percent()), true);
        return true;
    }

    public static float adjustSharpenedDamage(ItemStack weapon, net.minecraft.world.entity.LivingEntity target,
                                               float damage) {
        CompoundTag tag = weapon.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        int percent = tag.getInt(BUFF_PERCENT);
        int hits = tag.getInt(BUFF_HITS);
        if (percent <= 0 || hits <= 0) {
            return damage;
        }
        long gameTime = target.level().getGameTime();
        if (!tag.contains(BUFF_LAST_HIT) || tag.getLong(BUFF_LAST_HIT) != gameTime) {
            hits--;
            if (hits <= 0) {
                tag.remove(BUFF_PERCENT);
                tag.remove(BUFF_HITS);
                tag.remove(BUFF_LAST_HIT);
            } else {
                tag.putInt(BUFF_HITS, hits);
                tag.putLong(BUFF_LAST_HIT, gameTime);
            }
            weapon.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
        return damage * (1.0F + percent / 100.0F);
    }

    public static Component weaponBuffTooltip(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        int hits = tag.getInt(BUFF_HITS);
        int percent = tag.getInt(BUFF_PERCENT);
        return hits <= 0 || percent <= 0 ? Component.empty() : Component.translatable(
                "tooltip." + ImmortalCultivationMod.MODID + ".sharpened", percent, hits);
    }

    private static StoneAction stoneAction(ItemStack stack) {
        if (stack.is(ModItems.LOW_YANGJIAN_STONE.get())) return new StoneAction(1, true, false, false);
        if (stack.is(ModItems.MIDDLE_YANGJIAN_STONE.get())) return new StoneAction(5, true, false, false);
        if (stack.is(ModItems.HIGH_YANGJIAN_STONE.get())) return new StoneAction(10, true, false, false);
        if (stack.is(ModItems.LOW_SWORD_GRINDSTONE.get())) return new StoneAction(1, false, true, false);
        if (stack.is(ModItems.MIDDLE_SWORD_GRINDSTONE.get())) return new StoneAction(5, false, true, false);
        if (stack.is(ModItems.HIGH_SWORD_GRINDSTONE.get())) return new StoneAction(10, false, true, false);
        if (stack.is(ModItems.LOW_SABER_GRINDSTONE.get())) return new StoneAction(1, false, false, true);
        if (stack.is(ModItems.MIDDLE_SABER_GRINDSTONE.get())) return new StoneAction(5, false, false, true);
        if (stack.is(ModItems.HIGH_SABER_GRINDSTONE.get())) return new StoneAction(10, false, false, true);
        return null;
    }

    private static boolean isSword(ItemStack stack) {
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return path.contains("sword") || "taomu_sword".equals(path) || "copper_coin_sword".equals(path);
    }

    private static boolean isSaber(ItemStack stack) {
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return "dao".equals(path) || path.contains("saber");
    }

    private static void damageStone(ItemStack stone) {
        int nextDamage = stone.getDamageValue() + 1;
        if (nextDamage >= stone.getMaxDamage()) {
            stone.shrink(1);
        } else {
            stone.setDamageValue(nextDamage);
        }
    }

    private static void consumeOne(net.minecraft.world.entity.player.Player player, ItemStack stack) {
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }

    private record StoneAction(int percent, boolean repair, boolean swordOnly, boolean saberOnly) {
    }
}
