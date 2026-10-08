package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.entity.FlyingSwordEntity;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Ephemeral spirit sword made by 化兵符; never becomes a permanent item. */
public final class ManifestedWeaponItem extends FlyingSwordItem {
    private static final String OWNER = ImmortalCultivationMod.MODID + ":manifest_owner";
    private static final String EXPIRES = ImmortalCultivationMod.MODID + ":manifest_expires";
    private static final String BORN = ImmortalCultivationMod.MODID + ":manifest_born";
    private static final DustParticleOptions WEAPON_QI = new DustParticleOptions(
            new Vector3f(0.52F, 0.92F, 1.0F), 0.72F);

    public ManifestedWeaponItem(Properties properties) {
        // Reuse the real flying-sword item data and weapon implementation.  Only
        // the temporary ownership/lifetime restrictions below are additional.
        super(properties.stacksTo(1));
    }

    public static void manifest(ServerLevel level, ServerPlayer player) {
        ItemStack weapon = new ItemStack(ModItems.MANIFESTED_TALISMAN_SWORD.get());
        bindToOwner(weapon, player);

        if (player.getMainHandItem().isEmpty()) {
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, weapon);
        } else if (!player.getInventory().add(weapon)) {
            // A manifested weapon cannot become a dropped item; replace the
            // talisman hand only when the inventory is completely full.
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, weapon);
        }
        emitFormation(level, player);
    }

    /** Gives a newly created manifested sword a valid owner and lifetime. */
    public static void bindToOwner(ItemStack weapon, ServerPlayer player) {
        if (!isManifested(weapon)) return;
        ServerLevel level = player.serverLevel();
        CompoundTag root = new CompoundTag();
        root.putUUID(OWNER, player.getUUID());
        root.putLong(BORN, level.getGameTime());
        root.putLong(EXPIRES, level.getGameTime() + 25L * 20L);
        weapon.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    private static void emitFormation(ServerLevel level, ServerPlayer player) {
        Vec3 look = player.getLookAngle().normalize();
        Vec3 right = look.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() < 0.001D) right = new Vec3(1.0D, 0.0D, 0.0D);
        right = right.normalize();
        Vec3 handle = player.getEyePosition().add(look.scale(0.75D))
                .add(right.scale(0.22D)).add(0.0D, -0.34D, 0.0D);
        // First show only the translucent spiritual skeleton. Inventory ticks
        // fill it from hilt to point during the next few moments.
        for (int point = 0; point < 26; point += 4) {
            double t = point / 25.0D;
            Vec3 sample = handle.add(look.scale(t * 1.65D));
            level.sendParticles(WEAPON_QI, sample.x, sample.y, sample.z,
                    1, 0.018D, 0.018D, 0.018D, 0.004D);
            if (point % 4 == 0) {
                level.sendParticles(ParticleTypes.ENCHANTED_HIT,
                        sample.x, sample.y, sample.z,
                        1, 0.015D, 0.015D, 0.015D, 0.0D);
            }
        }
        level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_RETURN,
                SoundSource.PLAYERS, 0.7F, 1.65F);
    }

    public static boolean isManifested(ItemStack stack) {
        return stack.is(ModItems.MANIFESTED_TALISMAN_SWORD.get());
    }

    /** Removes a manifested sword even while it is using the real flying-sword entity system. */
    public static boolean expireFlyingSword(FlyingSwordEntity sword) {
        ItemStack stack = sword.getItem();
        if (!isManifested(stack) || !(sword.level() instanceof ServerLevel level)) {
            return false;
        }
        CompoundTag root = stack.getOrDefault(
                DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (level.getGameTime() < root.getLong(EXPIRES)) {
            return false;
        }
        level.sendParticles(WEAPON_QI,
                sword.getX(), sword.getY() + 0.25D, sword.getZ(),
                28, 0.35D, 0.35D, 0.35D, 0.025D);
        level.sendParticles(ParticleTypes.ENCHANTED_HIT,
                sword.getX(), sword.getY() + 0.25D, sword.getZ(),
                14, 0.3D, 0.3D, 0.3D, 0.02D);
        level.playSound(null, sword.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 0.55F, 0.62F);
        sword.ejectPassengers();
        sword.discard();
        return true;
    }

    @Override
    public void inventoryTick(
            ItemStack stack, Level level, Entity entity, int slotId, boolean selected) {
        super.inventoryTick(stack, level, entity, slotId, selected);
        if (level.isClientSide || !(entity instanceof Player player)) return;
        CompoundTag root = stack.getOrDefault(
                DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        boolean wrongOwner = !root.hasUUID(OWNER)
                || !root.getUUID(OWNER).equals(player.getUUID());
        long now = level.getGameTime();
        long expires = root.getLong(EXPIRES);
        if (wrongOwner || now >= expires) {
            dissolve((ServerLevel) level, player, stack);
        } else if (now - root.getLong(BORN) <= 12L) {
            emitFormationProgress((ServerLevel) level, player,
                    Mth.clamp((now - root.getLong(BORN)) / 12.0D, 0.0D, 1.0D));
        } else if (expires - now <= 20L && (player.tickCount & 1) == 0) {
            emitTipToHiltDissolve((ServerLevel) level, player,
                    Mth.clamp(1.0D - (expires - now) / 20.0D, 0.0D, 1.0D));
        } else if (selected && player.tickCount % 4 == 0) {
            Vec3 hand = player.getEyePosition().add(player.getLookAngle().scale(0.65D))
                    .add(0.0D, -0.42D, 0.0D);
            ((ServerLevel) level).sendParticles(WEAPON_QI,
                    hand.x, hand.y, hand.z, 1,
                    0.04D, 0.04D, 0.04D, 0.002D);
        }
    }

    private static void emitFormationProgress(
            ServerLevel level, Player player, double progress) {
        Vec3 look = player.getLookAngle().normalize();
        Vec3 right = look.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() < 0.001D) right = new Vec3(1.0D, 0.0D, 0.0D);
        Vec3 handle = player.getEyePosition().add(look.scale(0.75D))
                .add(right.normalize().scale(0.22D)).add(0.0D, -0.34D, 0.0D);
        int filledPoints = Math.max(1, (int) Math.ceil(progress * 25.0D));
        for (int point = Math.max(0, filledPoints - 5); point <= filledPoints; point++) {
            double t = Mth.clamp(point / 25.0D, 0.0D, 1.0D);
            Vec3 sample = handle.add(look.scale(t * 1.65D));
            level.sendParticles(point == filledPoints
                            ? ParticleTypes.ENCHANTED_HIT : WEAPON_QI,
                    sample.x, sample.y, sample.z, 1,
                    0.012D, 0.012D, 0.012D, 0.002D);
        }
    }

    private static void emitTipToHiltDissolve(
            ServerLevel level, Player player, double progress) {
        Vec3 look = player.getLookAngle().normalize();
        Vec3 origin = player.getEyePosition().add(look.scale(0.72D))
                .add(0.0D, -0.36D, 0.0D);
        double alongBlade = (1.0D - progress) * 1.65D;
        Vec3 point = origin.add(look.scale(alongBlade));
        level.sendParticles(WEAPON_QI, point.x, point.y, point.z,
                5, 0.08D, 0.08D, 0.08D, 0.028D);
        level.sendParticles(ParticleTypes.ENCHANTED_HIT,
                point.x, point.y, point.z,
                2, 0.05D, 0.05D, 0.05D, 0.01D);
    }

    private static void dissolve(ServerLevel level, LivingEntity holder, ItemStack stack) {
        level.sendParticles(WEAPON_QI,
                holder.getX(), holder.getY() + holder.getBbHeight() * 0.62D,
                holder.getZ(), 28, 0.35D, 0.55D, 0.35D, 0.025D);
        level.sendParticles(ParticleTypes.ENCHANTED_HIT,
                holder.getX(), holder.getY() + holder.getBbHeight() * 0.62D,
                holder.getZ(), 14, 0.3D, 0.45D, 0.3D, 0.02D);
        level.playSound(null, holder.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 0.55F, 0.62F);
        stack.setCount(0);
    }

    /** Returns any attempted container insertion to its owner and removes expired copies. */
    public static void tickPlayer(ServerPlayer player) {
        for (Slot slot : player.containerMenu.slots) {
            ItemStack stack = slot.getItem();
            if (!isManifested(stack) || slot.container == player.getInventory()) continue;
            slot.set(ItemStack.EMPTY);
            if (!player.getInventory().add(stack)) stack.setCount(0);
            player.containerMenu.broadcastChanges();
        }
    }

    public static void removeAll(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (isManifested(player.getInventory().getItem(slot))) {
                player.getInventory().setItem(slot, ItemStack.EMPTY);
            }
        }
    }

    /** A drop attempt returns the spirit weapon to its recorded owner. */
    public static void recoverDropped(ItemEntity entity) {
        ItemStack stack = entity.getItem();
        if (!isManifested(stack) || !(entity.level() instanceof ServerLevel level)) return;
        CompoundTag root = stack.getOrDefault(
                DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        ServerPlayer owner = root.hasUUID(OWNER)
                ? level.getServer().getPlayerList().getPlayer(root.getUUID(OWNER))
                : null;
        if (owner != null && level.getGameTime() < root.getLong(EXPIRES)) {
            ItemStack returned = stack.copy();
            if (!owner.getInventory().add(returned)
                    && owner.getMainHandItem().isEmpty()) {
                owner.setItemInHand(
                        net.minecraft.world.InteractionHand.MAIN_HAND, returned);
            }
        }
        entity.discard();
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return false;
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(
            ItemStack stack, TooltipContext context,
            List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(
                "tooltip." + ImmortalCultivationMod.MODID
                        + ".manifested_weapon.temporary").withColor(0x86EAF3));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
