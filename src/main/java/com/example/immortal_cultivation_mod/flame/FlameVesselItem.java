package com.example.immortal_cultivation_mod.flame;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.entity.ModEntities;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** One essence per vessel; firing spends Qi, releasing transfers its identity back to the world. */
public final class FlameVesselItem extends Item {
    public static final String DATA = ImmortalCultivationMod.MODID + ".flame_vessel";
    public static final int SHOT_COOLDOWN = 20;
    public static final int FULL_CHARGE_TICKS = ChargedFlameShape.LAUNCH;
    public static final int CHARGE_VFX_DELAY_TICKS = 6;

    public FlameVesselItem(Properties properties) { super(properties.stacksTo(1)); }

    public static FlameKind flame(ItemStack stack) {
        if (!(stack.getItem() instanceof FlameVesselItem)) return null;
        CompoundTag data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound(DATA);
        FlameKind kind = FlameKind.fromId(data.getString("kind"));
        return kind.special() && kind.id().equals(data.getString("kind")) ? kind : null;
    }

    public static UUID identity(ItemStack stack) {
        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound(DATA);
        return data.hasUUID("id") ? data.getUUID("id") : null;
    }

    public static void fill(ItemStack stack, FlameKind kind, UUID identity) {
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (kind == null || !kind.special()) tag.remove(DATA);
        else {
            var data = new CompoundTag();
            data.putString("kind", kind.id());
            data.putUUID("id", identity == null ? UUID.randomUUID() : identity);
            tag.put(DATA, data);
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static boolean permitted(ServerPlayer player, ItemStack stack, BlockPos pos) {
        return player.isAlive() && !player.isSpectator() && player.getAbilities().mayBuild
                && (stack == player.getMainHandItem() || stack == player.getOffhandItem())
                && player.serverLevel().hasChunkAt(pos) && player.serverLevel().isInWorldBounds(pos)
                && player.serverLevel().getWorldBorder().isWithinBounds(pos)
                && player.serverLevel().mayInteract(player, pos)
                && player.mayUseItemAt(pos, Direction.UP, stack);
    }

    public static boolean capture(ServerPlayer player, ItemStack stack, SpiritFlameEntity entity) {
        if (flame(stack) != null || !(stack.getItem() instanceof FlameVesselItem) || stack.getCount() != 1
                || !entity.isAlive() || !entity.kind().special() || entity.level() != player.level()
                || player.distanceToSqr(entity) > 36 || !permitted(player, stack, entity.blockPosition())) return false;
        Vec3 end = entity.position().add(0, .45, 0);
        var hit = player.level().clip(new ClipContext(player.getEyePosition(), end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.MISS && hit.getLocation().distanceToSqr(end) > .09) return false;
        FlameKind kind = entity.kind();
        fill(stack, kind, entity.getUUID());
        entity.discard();
        com.example.immortal_cultivation_mod.guidance.DaoGuidanceObservations.record(player, "flame_captured");
        transferParticles(player.serverLevel(), end, player.getEyePosition().add(player.getLookAngle().scale(.3)), kind);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.BOTTLE_FILL_DRAGONBREATH,
                SoundSource.PLAYERS, .7F, 1.2F);
        player.displayClientMessage(Component.translatable("message.immortal_cultivation_mod.flame_vessel.capture", kind.displayName()), true);
        player.getInventory().setChanged();
        return true;
    }

    public static boolean release(ServerPlayer player, ItemStack stack, Vec3 position) {
        FlameKind kind = flame(stack);
        BlockPos pos = BlockPos.containing(position);
        if (kind == null || player.distanceToSqr(position) > 36 || !permitted(player, stack, pos)
                || !player.level().getBlockState(pos).getCollisionShape(player.level(), pos).isEmpty()
                || !player.level().getBlockState(pos.above()).getCollisionShape(player.level(), pos.above()).isEmpty()) return false;
        var entity = new SpiritFlameEntity(ModEntities.SPIRIT_FLAME.get(), player.level());
        entity.setKind(kind);
        UUID id = identity(stack);
        if (id != null) entity.setUUID(id);
        entity.setPos(position);
        // addFreshEntity can reject duplicate UUIDs; retain the vessel in that case.
        if (!player.serverLevel().addFreshEntity(entity)) return false;
        fill(stack, null, null);
        com.example.immortal_cultivation_mod.guidance.DaoGuidanceObservations.record(player, "flame_released");
        transferParticles(player.serverLevel(), player.getEyePosition(), position.add(0, .45, 0), kind);
        player.serverLevel().playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, .6F, .8F);
        player.getInventory().setChanged();
        return true;
    }

    public static void transferParticles(ServerLevel level, Vec3 start, Vec3 end, FlameKind kind) {
        Vec3 delta = end.subtract(start);
        int count = Math.clamp((int)(delta.length() * 6), 8, 36);
        for (int i = 0; i <= count; i++) {
            Vec3 point = start.add(delta.scale((double)i / count));
            level.sendParticles(FlameParticles.type(kind), point.x, point.y, point.z, 1, .03, .03, .03, .015);
        }
    }

    public static boolean shoot(ServerPlayer player, ItemStack stack) {
        return shoot(player, stack, 0);
    }

    private static boolean shoot(ServerPlayer player, ItemStack stack, float charge) {
        FlameKind kind = flame(stack);
        if (kind == null || player.isSpectator() || !player.isAlive()
                || (stack != player.getMainHandItem() && stack != player.getOffhandItem())
                || player.getCooldowns().isOnCooldown(stack.getItem())) return false;
        int cost = chargedShotCost(kind, charge);
        var data = ModAttachments.getData(player);
        if (!player.isCreative() && data.qi() < cost) {
            player.displayClientMessage(Component.translatable("message.immortal_cultivation_mod.flame_vessel.qi", cost), true);
            return false;
        }
        var shot = new FlameShotEntity(ModEntities.FLAME_SHOT.get(), player.level());
        shot.setOwner(player);
        shot.setKind(kind);
        shot.setCharge(charge);
        Vec3 origin = player.getEyePosition();
        Vec3 desired = charge > 0 ? chargeOrigin(player, charge * FULL_CHARGE_TICKS)
                : origin.add(player.getLookAngle().scale(.35)).add(0, -.1, 0);
        var obstruction = player.level().clip(new ClipContext(origin, desired,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 center = obstruction.getType() == HitResult.Type.MISS ? desired
                : obstruction.getLocation().subtract(player.getLookAngle().scale(.08));
        shot.setPos(center.add(0, -shot.getBbHeight() * .5, 0));
        shot.shootFromRotation(player, player.getXRot(), player.getYRot(), 0,
                charge > 0 ? ChargedFlameShape.flightSpeed(charge, 0) : 1.4F, .4F);
        if (!player.serverLevel().addFreshEntity(shot)) return false;
        com.example.immortal_cultivation_mod.guidance.DaoGuidanceObservations.record(player, "flame_shot");
        if (charge > 0) { com.example.immortal_cultivation_mod.guidance.DaoGuidanceObservations.record(player, "giant_shot"); }
        if (!player.isCreative()) {
            ModAttachments.setData(player, data.withQi(data.qi() - cost));
            ServerEvents.syncPlayerData(player);
        }
        player.getCooldowns().addCooldown(stack.getItem(), SHOT_COOLDOWN + Math.round(charge * 20));
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE,
                SoundSource.PLAYERS, .6F + charge * .5F, 1.1F - charge * .4F);
        return true;
    }

    /** A shot spends 10% of the flame grade's base Qi capacity, independent of its holder. */
    public static int shotCost(FlameKind kind) {
        int maximumQi = CultivationLevels.getLevelDef(kind.damageCultivationLevel()).maxQi();
        return (int) ((maximumQi + 9L) / 10L);
    }

    public static float chargeFraction(int heldTicks) {
        return Math.clamp((heldTicks - 4) / (float) (FULL_CHARGE_TICKS - 4), 0, 1);
    }

    public static int chargedShotCost(FlameKind kind, float charge) {
        return (int) Math.ceil(shotCost(kind) * (1 + 2 * Math.clamp(charge, 0, 1)));
    }

    public static boolean canAffordCharge(FlameKind kind, int heldTicks, int qi, boolean creative) {
        return kind != null && (creative || qi >= chargedShotCost(kind, chargeFraction(heldTicks)));
    }

    public static boolean chargeEffectsReady(FlameKind kind, int heldTicks, int qi, boolean creative) {
        return heldTicks >= CHARGE_VFX_DELAY_TICKS && canAffordCharge(kind, heldTicks, qi, creative);
    }

    @Override public int getUseDuration(ItemStack stack, LivingEntity user) { return 72000; }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.NONE; }

    @Override public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int remaining) {
        if (user instanceof ServerPlayer player && player.getUseItem() == stack) {
            shoot(player, stack, chargeFraction(getUseDuration(stack, user) - remaining));
        }
    }

    @Override public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        FlameKind kind = flame(stack);
        if (kind == null || !user.isAlive() || user.isSpectator()) {
            user.stopUsingItem();
            return;
        }
        int held = getUseDuration(stack, user) - remaining;
        if (level instanceof ServerLevel server && user instanceof ServerPlayer player) {
            if (!canAffordCharge(kind, held, ModAttachments.getData(player).qi(), player.isCreative())) {
                player.stopUsingItem();
                player.displayClientMessage(Component.translatable("message.immortal_cultivation_mod.flame_vessel.qi",
                        chargedShotCost(kind, chargeFraction(held))), true);
                return;
            }
            if (held >= CHARGE_VFX_DELAY_TICKS) { com.example.immortal_cultivation_mod.guidance.DaoGuidanceObservations.record(player, "flame_charged"); }
            if (held >= FULL_CHARGE_TICKS) {
                if (player.getUseItem() == stack) shoot(player, stack, 1);
                player.stopUsingItem();
                return;
            }
            if (held == CHARGE_VFX_DELAY_TICKS || held == 30 || held == 70 || held == 87) {
                var sound = held == 70 ? (kind == FlameKind.NETHER ? SoundEvents.GHAST_SCREAM : SoundEvents.BEACON_POWER_SELECT)
                        : kind == FlameKind.NETHER ? SoundEvents.SOUL_ESCAPE.value()
                        : SoundEvents.BEACON_AMBIENT;
                server.playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS,
                        .25F + held / 180F, .55F + held / 180F);
            }
        }
    }

    /** Shared palm anchor grows forward with the projectile, leaving room for its expanding shell. */
    public static Vec3 chargeOrigin(LivingEntity user, float ticks) {
        Vec3 forward = user.getLookAngle();
        Vec3 right = forward.cross(new Vec3(0, 1, 0));
        if (right.lengthSqr() < .001) right = new Vec3(1, 0, 0);
        int side = user.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT ? 1 : -1;
        if (user.getUsedItemHand() == InteractionHand.OFF_HAND) side = -side;
        return user.getEyePosition().add(forward.scale(.75 + ChargedFlameShape.radius(ticks) * 1.15))
                .add(right.normalize().scale(side * .22)).add(0, -.2, 0);
    }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (flame(stack) == null) return InteractionResultHolder.pass(stack);
        if (!player.isShiftKeyDown()) {
            if (!player.isAlive() || player.isSpectator() || player.getCooldowns().isOnCooldown(this))
                return InteractionResultHolder.fail(stack);
            if (!level.isClientSide && !player.isCreative()
                    && ModAttachments.getData(player).qi() < shotCost(flame(stack))) {
                player.displayClientMessage(Component.translatable("message.immortal_cultivation_mod.flame_vessel.qi",
                        shotCost(flame(stack))), true);
                return InteractionResultHolder.fail(stack);
            }
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (!(player instanceof ServerPlayer server)) return InteractionResultHolder.fail(stack);
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(4));
        var hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 position = hit.getType() == HitResult.Type.BLOCK
                ? hit.getLocation().add(hit.getDirection().getNormal().getX() * .6,
                    hit.getDirection().getNormal().getY() * .6, hit.getDirection().getNormal().getZ() * .6)
                : player.position().add(player.getLookAngle().multiply(2, 0, 2)).add(0, .2, 0);
        boolean success = release(server, stack, position);
        return success ? InteractionResultHolder.consume(stack) : InteractionResultHolder.fail(stack);
    }

    @Override public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        return player == null ? InteractionResult.PASS : use(context.getLevel(), player, context.getHand()).getResult();
    }

    @Override public InteractionResult interactLivingEntity(ItemStack stack, Player player,
            net.minecraft.world.entity.LivingEntity target, InteractionHand hand) { return InteractionResult.PASS; }

    @Override public Component getName(ItemStack stack) {
        FlameKind kind = flame(stack);
        return kind == null ? super.getName(stack)
                : Component.translatable("item.immortal_cultivation_mod.flame_vessel.filled", kind.displayName());
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        FlameKind kind = flame(stack);
        if (kind == null) tooltip.add(Component.translatable("tooltip.immortal_cultivation_mod.flame_vessel.empty").withStyle(ChatFormatting.GRAY));
        else {
            tooltip.add(kind.displayName().copy().withStyle(style -> style.withColor(kind.color())));
            tooltip.add(Component.translatable("tooltip.immortal_cultivation_mod.flame_vessel.power", kind.power()).withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.translatable("tooltip.immortal_cultivation_mod.flame_vessel.stability", kind.stability()).withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.translatable("tooltip.immortal_cultivation_mod.flame_vessel.grade", kind.maxGrade()).withStyle(ChatFormatting.YELLOW));
            tooltip.add(Component.translatable("tooltip.immortal_cultivation_mod.flame_vessel.heat", kind.minTemperature(), kind.maxTemperature()).withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.translatable("tooltip.immortal_cultivation_mod.flame_vessel.combat",
                    damageText(kind.shotDamage()), damageText(kind.contactDamagePerSecond())).withStyle(ChatFormatting.RED));
            tooltip.add(Component.translatable("tooltip.immortal_cultivation_mod.flame_vessel.bonus." + kind.id()).withStyle(ChatFormatting.DARK_AQUA));
            tooltip.add(Component.translatable("tooltip.immortal_cultivation_mod.flame_vessel.controls",
                    shotCost(kind), chargedShotCost(kind, 1)).withStyle(ChatFormatting.GRAY));
        }
    }

    private static String damageText(float damage) {
        String text = String.format(java.util.Locale.ROOT, "%.1f", damage);
        return text.endsWith(".0") ? text.substring(0, text.length() - 2) : text;
    }
}
