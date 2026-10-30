package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.entity.SoulEntity;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class YuanhunBottleItem extends Item {
    public static final double CAPACITY = 100.0D;
    private static final double TARGET_RANGE = 32.0D;
    private static final double AREA_RANGE = 15.0D;
    private static final String SOUL_AMOUNT = ImmortalCultivationMod.MODID + ".yuanhun_bottle_souls";
    private static final DustParticleOptions BOTTLE_DUST =
            new DustParticleOptions(new Vector3f(0.15F, 0.75F, 0.68F), 0.9F);

    public YuanhunBottleItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }
        int absorbed = player.isShiftKeyDown()
                ? absorbArea(serverPlayer, stack)
                : absorbLookedAt(serverPlayer, stack);
        if (absorbed <= 0) {
            if (getSoulAmount(stack) >= CAPACITY) {
                player.displayClientMessage(Component.translatable(
                        "message." + ImmortalCultivationMod.MODID + ".yuanhun_bottle.full"), true);
            } else {
                player.displayClientMessage(Component.translatable(
                        "message." + ImmortalCultivationMod.MODID + ".yuanhun_bottle.no_target"), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        com.example.immortal_cultivation_mod.event.CraftProficiency.award(serverPlayer,
                com.example.immortal_cultivation_mod.event.CraftProficiency.Skill.GHOST_CONTROL, absorbed);
        return InteractionResultHolder.success(stack);
    }

    private static int absorbLookedAt(ServerPlayer player, ItemStack stack) {
        SoulEntity soul = findLookedAtSoul(player);
        return soul != null && absorbSoul(player, stack, soul) ? 1 : 0;
    }

    private static int absorbArea(ServerPlayer player, ItemStack stack) {
        List<SoulEntity> souls = player.serverLevel().getEntitiesOfClass(
                SoulEntity.class, player.getBoundingBox().inflate(AREA_RANGE), Entity::isAlive);
        souls.sort(Comparator.comparingDouble(player::distanceToSqr));
        int absorbed = 0;
        for (SoulEntity soul : souls) {
            if (getSoulAmount(stack) >= CAPACITY) {
                break;
            }
            if (absorbSoul(player, stack, soul)) {
                absorbed++;
            }
        }
        return absorbed;
    }

    private static SoulEntity findLookedAtSoul(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 start = player.getEyePosition();
        Vec3 maximumEnd = start.add(player.getLookAngle().scale(TARGET_RANGE));
        HitResult blockHit = level.clip(new ClipContext(
                start, maximumEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 end = blockHit.getType() == HitResult.Type.MISS ? maximumEnd : blockHit.getLocation();
        SoulEntity best = null;
        double bestDistance = start.distanceToSqr(end);
        for (SoulEntity soul : level.getEntitiesOfClass(
                SoulEntity.class,
                player.getBoundingBox().expandTowards(player.getLookAngle().scale(TARGET_RANGE)).inflate(1.0D),
                Entity::isAlive)) {
            var intersection = soul.getBoundingBox().inflate(0.55D).clip(start, end);
            if (intersection.isEmpty()) {
                continue;
            }
            double distance = start.distanceToSqr(intersection.get());
            if (distance < bestDistance) {
                bestDistance = distance;
                best = soul;
            }
        }
        return best;
    }

    private static boolean absorbSoul(ServerPlayer player, ItemStack stack, SoulEntity soul) {
        double current = getSoulAmount(stack);
        double room = CAPACITY - current;
        if (room <= 1.0E-6D || !soul.isAlive()) {
            return false;
        }
        setSoulAmount(stack, current + Math.min(room, soul.soulValue()));
        renderSoulTrail(player.serverLevel(), soul.position().add(0.0D, 0.9D, 0.0D), player);
        soul.discard();
        return true;
    }

    private static void renderSoulTrail(ServerLevel level, Vec3 start, ServerPlayer player) {
        Vec3 end = player.getEyePosition().add(player.getLookAngle().scale(0.35D)).add(0.0D, -0.35D, 0.0D);
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length < 1.0E-4D) {
            return;
        }
        Vec3 direction = delta.scale(1.0D / length);
        for (double distance = 0.0D; distance <= length; distance += 0.4D) {
            Vec3 point = start.add(direction.scale(distance));
            level.sendParticles(ParticleTypes.SOUL, point.x, point.y, point.z,
                    1, 0.02D, 0.02D, 0.02D, 0.01D);
            level.sendParticles(BOTTLE_DUST, point.x, point.y, point.z,
                    1, 0.02D, 0.02D, 0.02D, 0.0D);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!(level instanceof ServerLevel serverLevel)
                || !(entity instanceof ServerPlayer player)
                || player.getOffhandItem() != stack
                || player.tickCount % 5 != 0) {
            return;
        }
        ItemStack weapon = player.getMainHandItem();
        if (!(weapon.getItem() instanceof SoulStorageWeapon storage)) {
            return;
        }
        double bottleSouls = getSoulAmount(stack);
        double weaponSouls = storage.storedSouls(weapon);
        double transfer = Math.min(
                bottleSouls, Math.max(0.0D, storage.soulCapacity(weapon) - weaponSouls));
        if (transfer <= 1.0E-6D) {
            return;
        }
        setSoulAmount(stack, bottleSouls - transfer);
        storage.setStoredSouls(weapon, weaponSouls + transfer);
        Vec3 point = player.getEyePosition().add(player.getLookAngle().scale(0.35D)).add(0.0D, -0.45D, 0.0D);
        serverLevel.sendParticles(BOTTLE_DUST, point.x, point.y, point.z,
                8, 0.25D, 0.2D, 0.25D, 0.03D);
        serverLevel.sendParticles(ParticleTypes.SOUL, point.x, point.y, point.z,
                3, 0.2D, 0.15D, 0.2D, 0.02D);
    }

    public static double getSoulAmount(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return Math.min(CAPACITY, Math.max(0.0D, tag.getDouble(SOUL_AMOUNT)));
    }

    public static void setSoulAmount(ItemStack stack, double amount) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        amount = Math.min(CAPACITY, Math.max(0.0D, amount));
        if (amount <= 1.0E-6D) {
            tag.remove(SOUL_AMOUNT);
        } else {
            tag.putDouble(SOUL_AMOUNT, amount);
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(
                "tooltip." + ImmortalCultivationMod.MODID + ".yuanhun_bottle.souls",
                ShihunfanItem.formatSoulAmount(getSoulAmount(stack)),
                ShihunfanItem.formatSoulAmount(CAPACITY)));
    }
}
