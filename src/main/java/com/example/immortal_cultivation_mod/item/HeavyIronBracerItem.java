package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.combat.CombatStateManager;
import com.example.immortal_cultivation_mod.entity.ModelBonePartEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

public class HeavyIronBracerItem extends SwordItem implements GeoItem {
    @Override public Component getName(ItemStack stack) {
        if (!stack.is(ModItems.YAOYUAN_FIST_BONE.get())) return super.getName(stack);
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        int realm = tag.contains(com.example.immortal_cultivation_mod.entity.BeastRealms.NBT_KEY)
                ? tag.getInt(com.example.immortal_cultivation_mod.entity.BeastRealms.NBT_KEY) : 4;
        return com.example.immortal_cultivation_mod.entity.BeastRealms.prefix(realm, super.getName(stack));
    }
    private static final String DEBUG_STACK_TAG = "ImmortalCultivationHeavyIronBracerDebug";
    private static final String COMPANION_ACTIVE_TAG = "ImmortalCultivationHeavyIronBracerCompanionActive";
    private static final String STORED_OFFHAND_TAG = "ImmortalCultivationHeavyIronBracerStoredOffhand";
    private static final String CHESTPLATE_ACTIVE_TAG = "ImmortalCultivationHeavyIronBracerChestplateActive";
    private static final String STORED_CHESTPLATE_TAG = "ImmortalCultivationHeavyIronBracerStoredChestplate";
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public HeavyIronBracerItem(Properties properties) {
        this(properties, 2_032, 9.0F);
    }

    public HeavyIronBracerItem(Properties properties, int durability, float totalAttackDamage) {
        super(Tiers.NETHERITE, properties.durability(durability)
                .attributes(SwordItem.createAttributes(
                        Tiers.NETHERITE,
                        totalAttackDamage - Tiers.NETHERITE.getAttackDamageBonus(),
                        -2.4F)));
    }

    public static boolean isDebugStack(ItemStack stack) {
        if (!isBracerItem(stack)) {
            return false;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.getBoolean(DEBUG_STACK_TAG);
    }

    public static boolean isRealStack(ItemStack stack) {
        return isBracerItem(stack) && !isDebugStack(stack);
    }

    private static boolean isBracerItem(ItemStack stack) {
        return stack != null && (stack.is(ModItems.HEAVY_IRON_BRACER.get())
                || stack.is(ModItems.YAOYUAN_FIST_BONE.get()));
    }

    public static boolean isDebugChestplate(ItemStack stack) {
        return stack != null && (stack.is(ModItems.HEAVY_IRON_BRACER_CHESTPLATE_DEBUG.get())
                || stack.is(ModItems.YAOYUAN_FIST_BONE_CHESTPLATE_DEBUG.get()));
    }

    public static void syncOffhandCompanion(ServerPlayer player) {
        if (!isRealStack(player.getMainHandItem()) || !WeaponBinding.canUse(player.getMainHandItem(), player)) {
            restoreStoredOffhand(player);
            return;
        }

        syncChestplateCompanion(player);

        CompoundTag persistentData = player.getPersistentData();
        ItemStack offhand = player.getOffhandItem();
        if (isDebugStack(offhand)) {
            if (persistentData.getBoolean(COMPANION_ACTIVE_TAG)
                    && offhand.is(player.getMainHandItem().getItem())) {
                return;
            }
            player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            offhand = ItemStack.EMPTY;
        }
        if (!persistentData.getBoolean(COMPANION_ACTIVE_TAG)) {
            persistentData.putBoolean(COMPANION_ACTIVE_TAG, true);
            if (offhand.isEmpty()) {
                persistentData.remove(STORED_OFFHAND_TAG);
            } else {
                persistentData.put(STORED_OFFHAND_TAG, offhand.saveOptional(player.registryAccess()));
            }
        } else if (!offhand.isEmpty() && !isDebugStack(offhand)) {
            player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            returnToInventory(player, offhand);
        }

        removeDebugCopies(player, false);
        if (!isDebugStack(player.getOffhandItem())) {
            player.setItemSlot(EquipmentSlot.OFFHAND,
                    createDebugStack(player.getMainHandItem().getItem()));
        }
    }

    /** Keeps the registered dual-bracer presentation for NPC off-hand punches. */
    public static void syncNpcOffhandCompanion(PathfinderMob npc) {
        ItemStack offhand = npc.getOffhandItem();
        if (isRealStack(npc.getMainHandItem()) && WeaponBinding.canUse(npc.getMainHandItem(), npc)) {
            if (offhand.isEmpty() || isDebugStack(offhand)) {
                if (!isDebugStack(offhand)
                        || !offhand.is(npc.getMainHandItem().getItem())) {
                    npc.setItemSlot(EquipmentSlot.OFFHAND,
                            createDebugStack(npc.getMainHandItem().getItem()));
                }
                npc.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
            }
        } else if (isDebugStack(offhand)) {
            npc.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        }
    }

    public static void restoreStoredOffhand(ServerPlayer player) {
        restoreStoredChestplate(player);

        CompoundTag persistentData = player.getPersistentData();
        boolean companionWasActive = persistentData.getBoolean(COMPANION_ACTIVE_TAG);
        if (!companionWasActive) {
            if (isDebugStack(player.getOffhandItem())) {
                player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            }
            return;
        }
        removeDebugCopies(player, true);

        ItemStack currentOffhand = player.getOffhandItem();
        if (!currentOffhand.isEmpty()) {
            player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            returnToInventory(player, currentOffhand);
        }

        ItemStack stored = persistentData.contains(STORED_OFFHAND_TAG)
                ? ItemStack.parseOptional(player.registryAccess(), persistentData.getCompound(STORED_OFFHAND_TAG))
                : ItemStack.EMPTY;
        player.setItemSlot(EquipmentSlot.OFFHAND, stored);
        persistentData.remove(STORED_OFFHAND_TAG);
        persistentData.remove(COMPANION_ACTIVE_TAG);
    }

    private static void syncChestplateCompanion(ServerPlayer player) {
        CompoundTag persistentData = player.getPersistentData();
        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        Item expectedChestplate = debugChestplateFor(player.getMainHandItem());

        if (isDebugChestplate(chestplate)) {
            if (persistentData.getBoolean(CHESTPLATE_ACTIVE_TAG)
                    && chestplate.is(expectedChestplate)) {
                return;
            }
            player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            chestplate = ItemStack.EMPTY;
        }

        if (!persistentData.getBoolean(CHESTPLATE_ACTIVE_TAG)) {
            persistentData.putBoolean(CHESTPLATE_ACTIVE_TAG, true);
            if (chestplate.isEmpty()) {
                persistentData.remove(STORED_CHESTPLATE_TAG);
            } else {
                persistentData.put(STORED_CHESTPLATE_TAG,
                        chestplate.saveOptional(player.registryAccess()));
                player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            }
        } else if (!chestplate.isEmpty()) {
            player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            returnToInventory(player, chestplate);
        }

        removeDebugChestplateCopies(player, false);
        if (!player.getItemBySlot(EquipmentSlot.CHEST).is(expectedChestplate)) {
            player.setItemSlot(EquipmentSlot.CHEST, createDebugChestplate(expectedChestplate));
        }
    }

    private static void restoreStoredChestplate(ServerPlayer player) {
        CompoundTag persistentData = player.getPersistentData();
        boolean chestplateWasActive = persistentData.getBoolean(CHESTPLATE_ACTIVE_TAG);
        if (!chestplateWasActive) {
            if (isDebugChestplate(player.getItemBySlot(EquipmentSlot.CHEST))) {
                player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            }
            return;
        }

        removeDebugChestplateCopies(player, true);
        ItemStack currentChestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!currentChestplate.isEmpty()) {
            player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            returnToInventory(player, currentChestplate);
        }

        ItemStack stored = persistentData.contains(STORED_CHESTPLATE_TAG)
                ? ItemStack.parseOptional(player.registryAccess(),
                        persistentData.getCompound(STORED_CHESTPLATE_TAG))
                : ItemStack.EMPTY;
        player.setItemSlot(EquipmentSlot.CHEST, stored);
        persistentData.remove(STORED_CHESTPLATE_TAG);
        persistentData.remove(CHESTPLATE_ACTIVE_TAG);
    }

    private static ItemStack createDebugStack(Item item) {
        ItemStack stack = new ItemStack(item);
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putBoolean(DEBUG_STACK_TAG, true);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.CUSTOM_NAME,
                Component.translatable("item.immortal_cultivation_mod.heavy_iron_bracer_debug"));
        return stack;
    }

    private static Item debugChestplateFor(ItemStack mainHand) {
        return mainHand.is(ModItems.YAOYUAN_FIST_BONE.get())
                ? ModItems.YAOYUAN_FIST_BONE_CHESTPLATE_DEBUG.get()
                : ModItems.HEAVY_IRON_BRACER_CHESTPLATE_DEBUG.get();
    }

    private static ItemStack createDebugChestplate(Item chestplateItem) {
        ItemStack stack = new ItemStack(chestplateItem);
        stack.set(DataComponents.UNBREAKABLE, new Unbreakable(false));
        return stack;
    }

    private static void removeDebugCopies(ServerPlayer player, boolean includeOffhand) {
        for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
            if (isDebugStack(player.getInventory().items.get(slot))) {
                player.getInventory().items.set(slot, ItemStack.EMPTY);
            }
        }
        for (int slot = 0; slot < player.getInventory().armor.size(); slot++) {
            if (isDebugStack(player.getInventory().armor.get(slot))) {
                player.getInventory().armor.set(slot, ItemStack.EMPTY);
            }
        }
        if (includeOffhand && isDebugStack(player.getOffhandItem())) {
            player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        }
    }

    private static void removeDebugChestplateCopies(ServerPlayer player, boolean includeChestSlot) {
        for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
            if (isDebugChestplate(player.getInventory().items.get(slot))) {
                player.getInventory().items.set(slot, ItemStack.EMPTY);
            }
        }
        ItemStack equippedChestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        for (int slot = 0; slot < player.getInventory().armor.size(); slot++) {
            ItemStack stack = player.getInventory().armor.get(slot);
            if (isDebugChestplate(stack)
                    && (includeChestSlot || stack != equippedChestplate)) {
                player.getInventory().armor.set(slot, ItemStack.EMPTY);
            }
        }
        if (isDebugChestplate(player.getOffhandItem())) {
            player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        }
    }

    private static void returnToInventory(ServerPlayer player, ItemStack stack) {
        ItemStack remaining = stack.copy();
        player.getInventory().add(remaining);
        if (!remaining.isEmpty()) {
            player.drop(remaining, false);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND || !isRealStack(stack)) {
            return InteractionResultHolder.pass(stack);
        }
        if (level.isClientSide) {
            forceClientOffhandSwing(player);
        } else if (player instanceof ServerPlayer serverPlayer) {
            leftHandPunch(serverPlayer, findLookTarget(serverPlayer));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target,
                                                   InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !isRealStack(stack)) {
            return InteractionResult.PASS;
        }
        if (player.level().isClientSide) {
            forceClientOffhandSwing(player);
        } else if (player instanceof ServerPlayer serverPlayer) {
            leftHandPunch(serverPlayer, target);
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || context.getHand() != InteractionHand.MAIN_HAND
                || !isRealStack(context.getItemInHand())) {
            return InteractionResult.PASS;
        }
        if (context.getLevel().isClientSide) {
            forceClientOffhandSwing(player);
        } else if (player instanceof ServerPlayer serverPlayer) {
            leftHandPunch(serverPlayer, null);
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    public static void leftHandPunch(ServerPlayer player, Entity target) {
        if (target != null && target.isAttackable() && !target.isRemoved()) {
            player.attack(target);
        }
        resetOffhandSwing(player);
        player.swing(InteractionHand.OFF_HAND, true);
    }

    /** The same real off-hand bracer punch, generalized for cultivator NPCs. */
    public static boolean punchNpc(PathfinderMob attacker, Entity target, boolean heavy) {
        syncNpcOffhandCompanion(attacker);
        if (!isRealStack(attacker.getMainHandItem())
                || target == null || target == attacker || !target.isAlive()) {
            return false;
        }
        boolean landed = heavy
                ? CombatStateManager.performNpcHeavyAttack(attacker, target)
                : attacker.doHurtTarget(target);
        attacker.swing(InteractionHand.OFF_HAND, true);
        return landed;
    }

    public static void forceClientOffhandSwing(Player player) {
        resetOffhandSwing(player);
        player.swing(InteractionHand.OFF_HAND);
    }

    private static void resetOffhandSwing(Player player) {
        player.swinging = false;
        player.swingTime = -1;
        player.swingingArm = InteractionHand.OFF_HAND;
    }

    private static Entity findLookTarget(ServerPlayer player) {
        double range = player.entityInteractionRange();
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(range));
        AABB search = player.getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0D);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player.level(), player, start, end, search,
                entity -> entity.isPickable() && entity.isAttackable() && entity != player && !entity.isSpectator());
        return hit == null ? null : ModelBonePartEntity.resolveParent(hit.getEntity());
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private HeavyIronBracerItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (renderer == null) {
                    renderer = new HeavyIronBracerItemRenderer();
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

}
