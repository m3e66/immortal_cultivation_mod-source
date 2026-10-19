package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.entity.SpiritualLightEntity;
import com.example.immortal_cultivation_mod.entity.TalismanActivationEntity;
import com.example.immortal_cultivation_mod.entity.TalismanWardEntity;
import com.example.immortal_cultivation_mod.entity.ModelBonePartEntity;
import com.example.immortal_cultivation_mod.network.TalismanPayloads;
import com.example.immortal_cultivation_mod.spell.PlayerShieldManager;
import com.example.immortal_cultivation_mod.spiritual.TalismanMarkSystem;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Real, consumable effects shared by non-projectile cultivation talismans. */
public final class UtilityTalismanItem extends TalismanItem {
    private static final String QUIET_UNTIL =
            ImmortalCultivationMod.MODID + ":talisman_quiet_until";
    private static final String EVIL_WARD_UNTIL =
            ImmortalCultivationMod.MODID + ":talisman_evil_ward_until";
    private static final String THOUGHT_REGEN_UNTIL =
            ImmortalCultivationMod.MODID + ":talisman_thought_regen_until";
    private static final String SHIELD_ENTRIES =
            ImmortalCultivationMod.MODID + ":talisman_shields";
    private static final String SHIELD_BAR_SOURCE = "talisman_shields";
    private static final String THIN_GOLD_SOURCE="thin_gold_talisman",GOLDEN_BODY_SOURCE="golden_body_talisman";
    private static final String LUCK_WARD_ENTRIES =
            ImmortalCultivationMod.MODID + ":five_mountains_luck";
    public static final int LIGHT_ANCHOR_HOLD_TICKS = 8;
    private static final int LIGHT_USE_DURATION = 72_000;
    private static final int EVIL_WARD_TICKS = 5 * 60 * 20;
    private static final int THOUGHT_REGEN_TICKS = 3 * 60 * 20;
    private static final int FIVE_MOUNTAINS_TICKS = 10 * 60 * 20;
    private static final int MAX_FIVE_MOUNTAINS_STACKS = 3;

    private final Kind kind;

    public UtilityTalismanItem(Properties properties, Kind kind, Tier tier) {
        super(properties, tier);
        this.kind = kind;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
            java.util.List<net.minecraft.network.chat.Component> tooltip,
            net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (kind == Kind.FIVE_MOUNTAINS_WARD) {
            tooltip.add(net.minecraft.network.chat.Component.translatable(
                    "tooltip.immortal_cultivation_mod.five_mountains_luck")
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
        }
    }

    public static int activeLuckBonus(Player player) {
        var entries = player.getPersistentData().getList(LUCK_WARD_ENTRIES, Tag.TAG_COMPOUND);
        int total = 0;
        for (int i = 0; i < entries.size(); i++) total += Math.max(0, entries.getCompound(i).getInt("Bonus"));
        return total;
    }

    public static void copyLuckWard(Player original, Player replacement) {
        if (original.getPersistentData().contains(LUCK_WARD_ENTRIES, Tag.TAG_LIST)) {
            replacement.getPersistentData().put(LUCK_WARD_ENTRIES,
                    original.getPersistentData().getList(LUCK_WARD_ENTRIES, Tag.TAG_COMPOUND).copy());
        }
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (kind == Kind.DIM_LIGHT) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        if (kind.isMessage()) {
            if (player instanceof ServerPlayer serverPlayer) {
                TalismanPayloads.openMessageComposer(
                        serverPlayer, kind == Kind.THOUSAND_LI_MESSAGE);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        LivingEntity markedTarget = null;
        if (kind == Kind.MARK_ENTITY) {
            markedTarget = findMarkedTarget(level, player);
            if (markedTarget == null) {
                if (!level.isClientSide) {
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.translatable(
                                    "message.immortal_cultivation_mod.talisman.mark.no_target"),
                            true);
                }
                return InteractionResultHolder.fail(stack);
            }
        }
        if (kind == Kind.REPAIR_ITEM
                && !isRepairableHeldItem(player, hand)) {
            if (!level.isClientSide) {
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable(
                                "message.immortal_cultivation_mod.talisman.repair.no_target"),
                        true);
            }
            return InteractionResultHolder.fail(stack);
        }
        BlockPos voidEscapeTarget = null;
        if (kind == Kind.VOID_ESCAPE
                && level instanceof ServerLevel serverLevel
                && player instanceof ServerPlayer serverPlayer) {
            voidEscapeTarget = VoidEscapeTalismanLogic.findDestination(
                    serverLevel, serverPlayer);
            if (voidEscapeTarget == null) {
                VoidEscapeTalismanLogic.consumeFailureQi(serverPlayer);
                player.getCooldowns().addCooldown(this, 10);
                return InteractionResultHolder.fail(stack);
            }
        }
        if (level instanceof ServerLevel serverLevel
                && player instanceof ServerPlayer serverPlayer) {
            if (kind == Kind.FIVE_MOUNTAINS_WARD
                    && !canQueueFiveMountainsWard(
                    serverLevel, serverPlayer)) {
                return InteractionResultHolder.fail(stack);
            }
            activateAndConsume(serverLevel, serverPlayer, stack, voidEscapeTarget,
                    markedTarget);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (kind != Kind.DIM_LIGHT || context.getPlayer() == null) {
            return super.useOn(context);
        }
        context.getPlayer().startUsingItem(context.getHand());
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return kind == Kind.DIM_LIGHT ? LIGHT_USE_DURATION : 0;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public void releaseUsing(
            ItemStack stack, Level level, LivingEntity user, int timeLeft) {
        if (kind != Kind.DIM_LIGHT
                || !(level instanceof ServerLevel serverLevel)
                || !(user instanceof ServerPlayer player)) {
            return;
        }
        int heldTicks = Math.max(0, LIGHT_USE_DURATION - timeLeft);
        BlockPos anchor = null;
        if (heldTicks >= LIGHT_ANCHOR_HOLD_TICKS) {
            HitResult hit = player.pick(6.0D, 0.0F, false);
            if (hit instanceof BlockHitResult blockHit) {
                anchor = blockHit.getBlockPos().relative(blockHit.getDirection());
            }
        }
        activateAndConsume(serverLevel, player, stack, anchor, null);
    }

    private void activateAndConsume(
            ServerLevel level, ServerPlayer player, ItemStack stack,
            @Nullable BlockPos targetPos,
            @Nullable LivingEntity targetEntity) {
        TalismanActivationEntity.spawn(
                level, player, kind, targetPos, targetEntity);
        player.getCooldowns().addCooldown(this, 10);
        if (!player.getAbilities().instabuild) stack.shrink(1);
    }

    /** Called by the visible talisman after its floating/burning animation. */
    public static void applyActivatedEffect(
            ServerLevel level, ServerPlayer player, Kind kind,
            @Nullable BlockPos targetPos) {
        applyActivatedEffect(level, player, kind, targetPos, -1);
    }

    public static void applyActivatedEffect(
            ServerLevel level, ServerPlayer player, Kind kind,
            @Nullable BlockPos targetPos, int targetEntityId) {
        long now = level.getGameTime();
        switch (kind) {
            case CLEAN_DUST -> {
                player.clearFire();
                player.removeEffect(MobEffects.BLINDNESS);
                player.removeEffect(MobEffects.DARKNESS);
                ring(level, player, ParticleTypes.CLOUD, 0.55D, 16);
            }
            case DIM_LIGHT -> SpiritualLightEntity.spawn(level, player, targetPos);
            case MUFFLE_SOUND -> {
                player.getPersistentData().putLong(QUIET_UNTIL, now + 500L);
                ring(level, player, ParticleTypes.SMOKE, 2.5D, 18);
            }
            case THIN_GOLD_SHIELD -> addShield(
                    player, 200.0F + player.getMaxHealth() * 0.01F, 15 * 20,THIN_GOLD_SOURCE);
            case CALM_SOUL -> {
                player.getPersistentData().putLong(
                        EVIL_WARD_UNTIL, now + EVIL_WARD_TICKS);
                player.getPersistentData().putLong(
                        THOUGHT_REGEN_UNTIL, now + THOUGHT_REGEN_TICKS);
                player.removeEffect(MobEffects.DARKNESS);
                player.removeEffect(MobEffects.CONFUSION);
                ring(level, player, ParticleTypes.ENCHANTED_HIT, 1.0D, 18);
            }
            case SHORT_DIVINE_TRAVEL -> {
                // Potion display level = amplifier + one.
                player.addEffect(new MobEffectInstance(
                        MobEffects.MOVEMENT_SPEED, 30 * 20, 7));
                player.addEffect(new MobEffectInstance(
                        MobEffects.JUMP, 30 * 20, 2));
                ring(level, player, ParticleTypes.CLOUD, 1.5D, 18);
            }
            case STABILIZE_DISTURBANCE ->
                    TalismanWardEntity.spawnSuppression(level, player);
            case GOLDEN_BODY_SHIELD -> addShield(
                    player, 600.0F + player.getMaxHealth() * 0.01F, 25 * 20,GOLDEN_BODY_SOURCE);
            case FIVE_MOUNTAINS_WARD -> {
                if (addFiveMountainsLuck(player, now)) {
                    TalismanWardEntity.spawnMountains(
                            level, player, FIVE_MOUNTAINS_TICKS);
                }
            }
            case REPAIR_ITEM -> repairHeldItem(level, player);
            case MARK_ENTITY -> {
                Entity entity = targetEntityId < 0
                        ? null : level.getEntity(targetEntityId);
                if (entity instanceof LivingEntity target
                        && target.isAlive()
                        && player.distanceToSqr(target)
                                <= TalismanMarkSystem.RANGE
                                * TalismanMarkSystem.RANGE) {
                    TalismanMarkSystem.mark(player, target);
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.translatable(
                                    "message.immortal_cultivation_mod.talisman.mark.applied",
                                    target.getDisplayName()), true);
                }
            }
            case VOID_ESCAPE ->
                    VoidEscapeTalismanLogic.teleport(level, player, targetPos);
            case MANIFEST_WEAPON -> ManifestedWeaponItem.manifest(level, player);
            case VOICE_MESSAGE, THOUSAND_LI_MESSAGE -> {
                // Consumed only after target/range validation succeeds.
            }
        }
        level.playSound(null, player.blockPosition(),
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS,
                0.55F, 1.25F);
    }

    @Nullable
    private static LivingEntity findMarkedTarget(Level level, Player player) {
        double range = TalismanMarkSystem.RANGE;
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        HitResult blockHit = player.pick(range, 0.0F, false);
        Vec3 end = blockHit.getType() == HitResult.Type.MISS
                ? eye.add(look.scale(range)) : blockHit.getLocation();
        AABB bounds = player.getBoundingBox()
                .expandTowards(end.subtract(eye)).inflate(1.0D);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                level, player, eye, end, bounds,
                candidate -> ModelBonePartEntity.resolveLiving(candidate) != null
                        && candidate.isPickable()
                        && ModelBonePartEntity.resolveParent(candidate) != player
                        && !candidate.isSpectator());
        return hit == null ? null
                : ModelBonePartEntity.resolveLiving(hit.getEntity());
    }

    private static boolean isRepairableHeldItem(
            Player player, InteractionHand talismanHand) {
        InteractionHand targetHand = talismanHand == InteractionHand.MAIN_HAND
                ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack target = player.getItemInHand(targetHand);
        return target.isDamageableItem() && target.isDamaged();
    }

    private static void repairHeldItem(
            ServerLevel level, ServerPlayer player) {
        ItemStack target = repairTarget(player.getMainHandItem())
                ? player.getMainHandItem() : player.getOffhandItem();
        if (!repairTarget(target)) return;
        target.setDamageValue(0);
        level.sendParticles(ParticleTypes.ENCHANTED_HIT,
                player.getX(), player.getY() + player.getBbHeight() * 0.55D,
                player.getZ(), 22, 0.42D, 0.55D, 0.42D, 0.035D);
        level.playSound(null, player.blockPosition(),
                SoundEvents.ANVIL_USE, SoundSource.PLAYERS,
                0.55F, 1.65F);
    }

    private static boolean repairTarget(ItemStack stack) {
        return !stack.isEmpty()
                && !(stack.getItem() instanceof TalismanItem)
                && stack.isDamageableItem() && stack.isDamaged();
    }

    private static void addShield(
            ServerPlayer player, float amount, int durationTicks,String displaySource) {
        long now = player.serverLevel().getGameTime();
        ListTag shields = liveShieldEntries(player, now);
        CompoundTag entry = new CompoundTag();
        entry.putFloat("Amount", Math.max(0.0F, amount));
        entry.putFloat("Max", Math.max(0.0F, amount));
        entry.putLong("Expires", now + durationTicks);
        entry.putString("DisplaySource",displaySource);
        shields.add(entry);
        player.getPersistentData().put(SHIELD_ENTRIES, shields);
        syncShieldBar(player, shields);
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.immortal_cultivation_mod.talisman.shield", Math.round(amount)), true);
        ring(player.serverLevel(), player,
                ParticleTypes.ENCHANTED_HIT, 1.15D, 22);
    }

    /** Consumes oldest stackable talisman shields and returns remaining damage. */
    public static float absorbTalismanShields(
            ServerPlayer player, float incomingDamage) {
        return absorbTalismanShields(player, null, incomingDamage);
    }

    public static float absorbTalismanShields(
            ServerPlayer player, net.minecraft.world.damagesource.DamageSource source, float incomingDamage) {
        if (incomingDamage <= 0.0F) return incomingDamage;
        ListTag current = liveShieldEntries(
                player, player.serverLevel().getGameTime());
        if (current.isEmpty()) return incomingDamage;
        float multiplier = com.example.immortal_cultivation_mod.attachment.BodyTypeEffects.shieldMultiplier(source);
        float remaining = incomingDamage * multiplier;
        ListTag kept = new ListTag();
        for (int index = 0; index < current.size(); index++) {
            CompoundTag entry = current.getCompound(index);
            float amount = Math.max(0.0F, entry.getFloat("Amount"));
            if (remaining > 0.0F) {
                float absorbed = Math.min(remaining, amount);
                remaining -= absorbed;
                amount -= absorbed;
            }
            if (amount > 0.0001F) {
                entry.putFloat("Amount", amount);
                kept.add(entry);
            }
        }
        putOrRemove(player.getPersistentData(), SHIELD_ENTRIES, kept);
        syncShieldBar(player, kept);
        if (remaining < incomingDamage * multiplier) {
            player.serverLevel().sendParticles(ParticleTypes.ENCHANTED_HIT,
                    player.getX(), player.getY() + player.getBbHeight() * 0.55D,
                    player.getZ(), 8, 0.45D, 0.55D, 0.45D, 0.02D);
            player.serverLevel().playSound(null, player.blockPosition(),
                    SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS,
                    0.45F, 1.45F);
        }
        return Math.max(0.0F, remaining / multiplier);
    }

    private static ListTag liveShieldEntries(ServerPlayer player, long now) {
        CompoundTag persistent = player.getPersistentData();
        ListTag current = persistent.getList(SHIELD_ENTRIES, Tag.TAG_COMPOUND);
        ListTag kept = new ListTag();
        boolean changed = false;
        for (int index = 0; index < current.size(); index++) {
            CompoundTag entry = current.getCompound(index);
            if (entry.getLong("Expires") > now
                    && entry.getFloat("Amount") > 0.0F) {
                CompoundTag copy = entry.copy();
                if (!copy.contains("Max", Tag.TAG_ANY_NUMERIC)) {
                    copy.putFloat("Max", copy.getFloat("Amount"));
                    changed = true;
                }
                kept.add(copy);
            } else {
                changed = true;
            }
        }
        putOrRemove(persistent, SHIELD_ENTRIES, kept);
        if (changed) {
            syncShieldBar(player, kept);
        }
        return kept;
    }

    private static void syncShieldBar(
            ServerPlayer player, ListTag shields) {
        float[] amounts=new float[3],maxima=new float[3];
        for (int index = 0; index < shields.size(); index++) {
            CompoundTag entry = shields.getCompound(index);
            float current = Math.max(0.0F, entry.getFloat("Amount"));
            int kind=switch(entry.getString("DisplaySource")){case THIN_GOLD_SOURCE->1;case GOLDEN_BODY_SOURCE->2;default->0;};
            amounts[kind]+=current;
            maxima[kind]+=Math.max(current, entry.getFloat("Max"));
        }
        String[] sources={SHIELD_BAR_SOURCE,THIN_GOLD_SOURCE,GOLDEN_BODY_SOURCE};
        for(int i=0;i<sources.length;i++) {
            if(amounts[i]<=0||maxima[i]<=0)PlayerShieldManager.clear(player,sources[i]);
            else PlayerShieldManager.set(player,sources[i],amounts[i],maxima[i]);
        }
    }

    private static boolean addFiveMountainsLuck(
            ServerPlayer player, long now) {
        ListTag wards = liveLuckEntries(player, now, true);
        if (wards.size() >= MAX_FIVE_MOUNTAINS_STACKS) {
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("message.immortal_cultivation_mod.talisman.five_mountains_limit"), true);
            return false;
        }
        int bonus = wards.isEmpty() ? 10 : wards.size() == 1 ? 7 : 3;
        var data = ModAttachments.getData(player);
        bonus = Math.min(bonus, 100 - data.luck());
        if (bonus <= 0) return false;
        ModAttachments.setData(player, data.withLuck(data.luck() + bonus));
        CompoundTag entry = new CompoundTag();
        entry.putInt("Bonus", bonus);
        entry.putLong("Expires", now + FIVE_MOUNTAINS_TICKS);
        wards.add(entry);
        player.getPersistentData().put(LUCK_WARD_ENTRIES, wards);
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.immortal_cultivation_mod.talisman.five_mountains_gain", bonus), true);
        return true;
    }

    private static boolean canQueueFiveMountainsWard(
            ServerLevel level, ServerPlayer player) {
        int active = liveLuckEntries(
                player, level.getGameTime(), true).size();
        int pending = level.getEntitiesOfClass(
                TalismanActivationEntity.class,
                player.getBoundingBox().inflate(6.0D),
                paper -> paper.kind() == Kind.FIVE_MOUNTAINS_WARD
                        && !paper.effectApplied()
                        && paper.ownerUuid()
                        .filter(player.getUUID()::equals).isPresent())
                .size();
        int pendingBonus = 0;
        for (int i = active; i < active + pending; i++) pendingBonus += i == 0 ? 10 : i == 1 ? 7 : 3;
        if (ModAttachments.getData(player).luck() + pendingBonus >= 100) return false;
        if (active + pending < MAX_FIVE_MOUNTAINS_STACKS) {
            return true;
        }
        player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("message.immortal_cultivation_mod.talisman.five_mountains_limit"), true);
        return false;
    }

    private static ListTag liveLuckEntries(
            ServerPlayer player, long now, boolean applyExpiredPenalty) {
        CompoundTag persistent = player.getPersistentData();
        ListTag current = persistent.getList(
                LUCK_WARD_ENTRIES, Tag.TAG_COMPOUND);
        ListTag kept = new ListTag();
        int removedBonus = 0;
        int permanentLoss = 0;
        for (int index = 0; index < current.size(); index++) {
            CompoundTag entry = current.getCompound(index);
            if (entry.getLong("Expires") > now) {
                kept.add(entry.copy());
            } else if (applyExpiredPenalty) {
                removedBonus += Math.max(0, entry.getInt("Bonus"));
                permanentLoss += 5;
            }
        }
        putOrRemove(persistent, LUCK_WARD_ENTRIES, kept);
        if (removedBonus > 0 || permanentLoss > 0) {
            var data = ModAttachments.getData(player);
            ModAttachments.setData(player, data.withLuck(
                    data.luck() - removedBonus - permanentLoss));
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.immortal_cultivation_mod.talisman.five_mountains_expired", permanentLoss), true);
        }
        return kept;
    }

    private static void putOrRemove(
            CompoundTag persistent, String key, ListTag values) {
        if (values.isEmpty()) persistent.remove(key);
        else persistent.put(key, values);
    }

    public static boolean isMuffleActive(Player player) {
        return player.getPersistentData().getLong(QUIET_UNTIL)
                > player.level().getGameTime();
    }

    /** Hook used by the separated 邪祟 system when it is enabled again. */
    public static boolean suppressesEvilSpirits(Player player) {
        return player.getPersistentData().getLong(EVIL_WARD_UNTIL)
                > player.level().getGameTime();
    }

    /** Applies ongoing effects whose paper item has already been consumed. */
    public static void tickPlayer(ServerPlayer player) {
        long now = player.serverLevel().getGameTime();
        ListTag shields = liveShieldEntries(player, now);
        // Re-register persistent talisman shields after login/server reload and
        // keep each colored HUD source authoritative without sending a packet
        // every tick.
        if (player.tickCount % 20 == 0) {
            syncShieldBar(player, shields);
        }
        liveLuckEntries(player, now, true);
        if (suppressesEvilSpirits(player)) {
            player.removeEffect(MobEffects.DARKNESS);
            player.removeEffect(MobEffects.CONFUSION);
        }
        if (player.getPersistentData().getLong(THOUGHT_REGEN_UNTIL) > now
                && player.tickCount % 20 == 0) {
            var data = ModAttachments.getData(player);
            int amount = Math.max(1, data.maxThoughts() / 100);
            if (data.thoughts() < data.maxThoughts()) {
                ModAttachments.setData(player,
                        data.withThoughts(data.thoughts() + amount));
            }
        }
        if (player.tickCount % 10 == 0 && isMuffleActive(player)) {
            for (net.minecraft.world.entity.Mob mob :
                    player.serverLevel().getEntitiesOfClass(
                            net.minecraft.world.entity.Mob.class,
                            player.getBoundingBox().inflate(12.0D),
                            mob -> mob.getTarget() == player
                                    && mob.distanceToSqr(player) > 2.5D * 2.5D)) {
                mob.setTarget(null);
            }
        }
    }

    private static void ring(
            ServerLevel level, LivingEntity center,
            net.minecraft.core.particles.ParticleOptions particle,
            double radius, int count) {
        for (int index = 0; index < count; index++) {
            double angle = Math.PI * 2.0D * index / count;
            double x = center.getX() + Math.cos(angle) * radius;
            double z = center.getZ() + Math.sin(angle) * radius;
            level.sendParticles(particle, x,
                    center.getY() + center.getBbHeight() * 0.5D, z,
                    1, 0.02D, 0.05D, 0.02D, 0.005D);
        }
    }

    public enum Kind {
        CLEAN_DUST,
        DIM_LIGHT,
        MUFFLE_SOUND,
        THIN_GOLD_SHIELD,
        CALM_SOUL,
        SHORT_DIVINE_TRAVEL,
        VOICE_MESSAGE,
        STABILIZE_DISTURBANCE,
        GOLDEN_BODY_SHIELD,
        FIVE_MOUNTAINS_WARD,
        THOUSAND_LI_MESSAGE,
        REPAIR_ITEM,
        MARK_ENTITY,
        VOID_ESCAPE,
        MANIFEST_WEAPON;

        public boolean isMessage() {
            return this == VOICE_MESSAGE || this == THOUSAND_LI_MESSAGE;
        }

        public static Kind byId(int id) {
            Kind[] values = values();
            return values[Math.floorMod(id, values.length)];
        }
    }
}
