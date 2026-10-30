package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.item.ShihunfanItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class ShihunfanEntity extends Entity implements GeoEntity {
    private static final double ABSORB_RADIUS = 15.0D;
    private static final RawAnimation SUCK_ANIMATION =
            RawAnimation.begin().thenPlay("animation.shihunfan.suck");
    private static final DustParticleOptions DARK_DUST =
            new DustParticleOptions(new Vector3f(0.14F, 0.01F, 0.20F), 1.15F);
    private static final DustParticleOptions GHOST_CYAN =
            new DustParticleOptions(new Vector3f(0.11F, 0.43F, 0.47F), 0.95F);
    private static final DustParticleOptions GHOST_GREEN =
            new DustParticleOptions(new Vector3f(0.40F, 0.68F, 0.47F), 0.85F);
    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID =
            SynchedEntityData.defineId(ShihunfanEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Float> SOUL_AMOUNT =
            SynchedEntityData.defineId(ShihunfanEntity.class, EntityDataSerializers.FLOAT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private ItemStack storedStack = ItemStack.EMPTY;

    public ShihunfanEntity(EntityType<? extends ShihunfanEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
    }

    public static ShihunfanEntity create(Level level, ServerPlayer owner, Vec3 pos, ItemStack stack) {
        ShihunfanEntity entity = ModEntities.SHIHUNFAN.get().create(level);
        if (entity == null) {
            return null;
        }
        entity.entityData.set(OWNER_UUID, Optional.of(owner.getUUID()));
        entity.storedStack = stack.is(ModItems.SHIHUNFAN.get())
                ? stack.copyWithCount(1)
                : new ItemStack(ModItems.SHIHUNFAN.get());
        entity.entityData.set(SOUL_AMOUNT, (float) ShihunfanItem.getSoulAmount(entity.storedStack));
        entity.moveTo(pos.x, pos.y, pos.z, owner.getYRot(), 0.0F);
        return entity;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(OWNER_UUID, Optional.empty());
        builder.define(SOUL_AMOUNT, 0.0F);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            entityData.set(OWNER_UUID, Optional.of(tag.getUUID("Owner")));
        }
        storedStack = tag.contains("StoredItem")
                ? ItemStack.parseOptional(registryAccess(), tag.getCompound("StoredItem"))
                : new ItemStack(ModItems.SHIHUNFAN.get());
        if (!storedStack.is(ModItems.SHIHUNFAN.get())) {
            storedStack = new ItemStack(ModItems.SHIHUNFAN.get());
        }
        double amount = Math.min(
                ShihunfanItem.maxSoulCapacity(storedStack), Math.max(0.0D, tag.getDouble("SoulAmount")));
        ShihunfanItem.setSoulAmount(storedStack, amount);
        entityData.set(SOUL_AMOUNT, (float) amount);
        setNoGravity(true);
        noPhysics = true;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        entityData.get(OWNER_UUID).ifPresent(uuid -> tag.putUUID("Owner", uuid));
        ensureStoredStack();
        ShihunfanItem.setSoulAmount(storedStack, entityData.get(SOUL_AMOUNT));
        tag.put("StoredItem", storedStack.saveOptional(registryAccess()));
        tag.putDouble("SoulAmount", entityData.get(SOUL_AMOUNT));
    }

    @Override
    public void tick() {
        super.tick();
        setNoGravity(true);
        noPhysics = true;
        setDeltaMovement(Vec3.ZERO);
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (tickCount % 4 == 0) {
            float density = (float) Math.min(1.0D, Math.sqrt(
                    entityData.get(SOUL_AMOUNT) / Math.max(1.0D, ShihunfanItem.maxSoulCapacity(ensureStoredStack()))));
            serverLevel.sendParticles(DARK_DUST, getX(), getY() + 1.25D, getZ(),
                    4 + Math.round(density * 12.0F), 0.5D, 0.8D, 0.5D, 0.02D);
            serverLevel.sendParticles(ParticleTypes.WITCH, getX(), getY() + 1.25D, getZ(),
                    2 + Math.round(density * 5.0F), 0.4D, 0.7D, 0.4D, 0.012D);
            serverLevel.sendParticles(density > 0.5F ? GHOST_GREEN : GHOST_CYAN,
                    getX(), getY() + 1.25D, getZ(), 2 + Math.round(density * 6.0F),
                    0.42D, 0.72D, 0.42D, 0.015D);
            if (density > 0.15F) {
                serverLevel.sendParticles(ParticleTypes.SOUL, getX(), getY() + 1.25D, getZ(),
                        1 + Math.round(density * 5.0F), 0.45D, 0.75D, 0.45D, 0.018D);
                serverLevel.sendParticles(ParticleTypes.ASH, getX(), getY() + 1.25D, getZ(),
                        2 + Math.round(density * 7.0F), 0.55D, 0.85D, 0.55D, 0.02D);
            }
        }
        if (tickCount % 10 == 1) {
            absorbNearbySouls(serverLevel);
        }
    }

    private void absorbNearbySouls(ServerLevel level) {
        List<SoulEntity> souls = level.getEntitiesOfClass(
                SoulEntity.class, getBoundingBox().inflate(ABSORB_RADIUS), Entity::isAlive);
        if (souls.isEmpty()) {
            return;
        }
        double total = entityData.get(SOUL_AMOUNT);
        double capacity = ShihunfanItem.maxSoulCapacity(ensureStoredStack());
        if (total >= capacity) {
            return;
        }
        int captured = 0;
        triggerAnim("main", "suck");
        for (SoulEntity soul : souls) {
            if (total >= capacity) {
                break;
            }
            total = Math.min(capacity, total + soul.soulValue());
            renderSoulTrail(level, soul.position().add(0.0D, 0.9D, 0.0D));
            soul.discard();
            captured++;
        }
        entityData.set(SOUL_AMOUNT, (float) total);
        ShihunfanItem.setSoulAmount(storedStack, total);
        var owner = entityData.get(OWNER_UUID).map(level.getServer().getPlayerList()::getPlayer).orElse(null);
        if (owner != null && captured > 0)
            com.example.immortal_cultivation_mod.event.CraftProficiency.award(owner, com.example.immortal_cultivation_mod.event.CraftProficiency.Skill.GHOST_CONTROL, captured);
    }

    private void renderSoulTrail(ServerLevel level, Vec3 start) {
        Vec3 end = position().add(0.0D, 1.35D, 0.0D);
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length < 1.0E-4D) {
            return;
        }
        Vec3 direction = delta.scale(1.0D / length);
        for (double distance = 0.0D; distance <= length; distance += 0.45D) {
            Vec3 point = start.add(direction.scale(distance));
            level.sendParticles(ParticleTypes.SOUL, point.x, point.y, point.z,
                    1, 0.03D, 0.03D, 0.03D, 0.01D);
            level.sendParticles(DARK_DUST, point.x, point.y, point.z,
                    1, 0.025D, 0.025D, 0.025D, 0.0D);
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        Optional<UUID> owner = entityData.get(OWNER_UUID);
        if (owner.isPresent() && !owner.get().equals(player.getUUID())) {
            return InteractionResult.CONSUME;
        }
        ItemStack stack = ensureStoredStack().copyWithCount(1);
        ShihunfanItem.setSoulAmount(stack, entityData.get(SOUL_AMOUNT));
        if (!serverPlayer.getInventory().add(stack)) {
            serverPlayer.drop(stack, false);
        }
        discard();
        return InteractionResult.SUCCESS;
    }

    private ItemStack ensureStoredStack() {
        if (!storedStack.is(ModItems.SHIHUNFAN.get())) {
            storedStack = new ItemStack(ModItems.SHIHUNFAN.get());
        }
        return storedStack;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 4,
                        state -> PlayState.STOP)
                .triggerableAnim("suck", SUCK_ANIMATION));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
