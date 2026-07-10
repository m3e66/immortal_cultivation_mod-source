package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.spell.UndeadControl;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public class CoffinItem extends ArmorItem implements GeoItem, ICurioItem {
    private static final String TAG_ENTITY = "StoredUndead";
    private static final String TAG_ENTITY_ID = "StoredEntityId";
    private static final String TAG_LAST_STORE_TICK = "LastStoreTick";

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public CoffinItem(Properties properties) {
        super(ArmorMaterials.DIAMOND, Type.CHESTPLATE, properties.stacksTo(1));
    }

    public static InteractionResult tryStore(ServerPlayer player, Entity target, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown() || !stack.is(ModItems.COFFIN.get())) {
            return InteractionResult.PASS;
        }
        if (!(target instanceof LivingEntity living) || !UndeadControl.isUndeadServantType(living)
                || !UndeadControl.isOwnedByPlayer(living, player) || hasStoredEntity(stack)) {
            return InteractionResult.PASS;
        }
        if (target.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        CompoundTag entityTag = new CompoundTag();
        target.saveWithoutId(entityTag);
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        entityTag.putString("id", id.toString());

        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        root.put(TAG_ENTITY, entityTag);
        root.putString(TAG_ENTITY_ID, id.toString());
        root.putLong(TAG_LAST_STORE_TICK, player.level().getGameTime());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        target.discard();
        player.displayClientMessage(Component.literal("Stored undead"), true);
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown() || !hasStoredEntity(stack)) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide && player instanceof ServerPlayer sp && level instanceof ServerLevel serverLevel) {
            if (recentlyStored(stack, level.getGameTime())) {
                return InteractionResultHolder.success(stack);
            }
            if (releaseStoredEntity(serverLevel, sp, stack)) {
                return InteractionResultHolder.success(stack);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private CoffinItemRenderer renderer;
            private CoffinArmorRenderer armorRenderer;

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (renderer == null) {
                    renderer = new CoffinItemRenderer();
                }
                return renderer;
            }

            @Override
            public <T extends LivingEntity> HumanoidModel<?> getGeoArmorRenderer(T livingEntity, ItemStack itemStack,
                                                                                 EquipmentSlot equipmentSlot,
                                                                                 HumanoidModel<T> original) {
                if (armorRenderer == null) {
                    armorRenderer = new CoffinArmorRenderer();
                }
                return armorRenderer;
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
        return "back_accessory".equals(slotContext.identifier());
    }

    private static boolean hasStoredEntity(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().contains(TAG_ENTITY);
    }

    private static boolean recentlyStored(ItemStack stack, long gameTime) {
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return root.contains(TAG_LAST_STORE_TICK) && gameTime - root.getLong(TAG_LAST_STORE_TICK) < 5L;
    }

    private static boolean releaseStoredEntity(ServerLevel level, ServerPlayer player, ItemStack stack) {
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!root.contains(TAG_ENTITY)) {
            return false;
        }
        CompoundTag entityTag = root.getCompound(TAG_ENTITY);
        Optional<Entity> loaded = EntityType.create(entityTag, level);
        if (loaded.isEmpty()) {
            return false;
        }
        Entity entity = loaded.get();
        Vec3 pos = player.position().add(player.getLookAngle().multiply(1.8D, 0.0D, 1.8D));
        entity.moveTo(pos.x, player.getY(), pos.z, player.getYRot(), 0.0F);
        level.addFreshEntity(entity);
        root.remove(TAG_ENTITY);
        root.remove(TAG_ENTITY_ID);
        root.remove(TAG_LAST_STORE_TICK);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        player.displayClientMessage(Component.literal("Released undead"), true);
        return true;
    }
}
