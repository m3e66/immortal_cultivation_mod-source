package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.combat.CombatStateManager;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.Vector3f;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/** Consumed only when an attack is fatal and a safe substitute destination exists. */
@EventBusSubscriber(modid = ImmortalCultivationMod.MODID)
public final class TribulationDollItem extends Item implements ICurioItem {
    private static final DustParticleOptions THREAD =
            new DustParticleOptions(new Vector3f(0.85F, 0.035F, 0.08F), 0.65F);
    private static final List<Substitution> EFFECTS = new ArrayList<>();
    private final boolean purified;

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGHEST)
    public static void preventDeath(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
        if (!event.isCanceled() && event.getEntity() instanceof ServerPlayer player
                && trySubstitute(player, event.getSource())) {
            event.setCanceled(true);
        }
    }

    public TribulationDollItem(Properties properties, boolean purified) {
        super(properties);
        this.purified = purified;
    }

    @Override
    public boolean canEquip(SlotContext context, ItemStack stack) {
        return "charm".equals(context.identifier());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
            List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.immortal_cultivation_mod.tribulation_doll")
                .withStyle(ChatFormatting.GRAY));
        if (purified) tooltip.add(Component.translatable(
                "tooltip.immortal_cultivation_mod.tribulation_doll.cleanse").withStyle(ChatFormatting.GRAY));
    }

    public static boolean trySubstitute(ServerPlayer player, DamageSource source) {
        // Damage-over-time and environmental damage must never spend a doll.
        if (source.getEntity() == null || source.is(DamageTypes.FELL_OUT_OF_WORLD)
                || source.is(DamageTypes.GENERIC_KILL) || source.is(DamageTypes.FALL)
                || source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.DROWN)
                || source.is(DamageTypes.LAVA) || source.is(DamageTypes.ON_FIRE)
                || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        var equipped = CuriosApi.getCuriosInventory(player).flatMap(handler ->
                handler.findFirstCurio(stack -> stack.getItem() instanceof TribulationDollItem));
        if (equipped.isEmpty()) return false;
        Vec3 destination = safeDestination(player);
        if (destination == null) return false;

        ItemStack stack = equipped.get().stack();
        boolean cleanse = ((TribulationDollItem) stack.getItem()).purified;
        ItemStack visual = stack.copyWithCount(1);
        Vec3 origin = player.position();
        stack.shrink(1);
        player.stopRiding();
        player.removeEffect(ModEffects.DAZE);
        player.removeEffect(ModEffects.DINGSHEN);
        player.removeEffect(ModEffects.FROZEN);
        player.removeEffect(ModEffects.TECHNIQUE_SLOW);
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        FuchenItem.releaseBoundTarget(player);
        CombatStateManager.clear(player);
        if (cleanse) {
            for (var effect : List.copyOf(player.getActiveEffects())) {
                if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                    player.removeEffect(effect.getEffect());
                    break;
                }
            }
        }
        player.setHealth(player.getMaxHealth() * 0.125F);
        player.fallDistance = 0;
        player.setDeltaMovement(Vec3.ZERO);
        player.hurtMarked = true;
        player.invulnerableTime = 20;
        player.teleportTo(destination.x, destination.y, destination.z);

        ServerLevel level = player.serverLevel();
        Display.ItemDisplay doll = new Display.ItemDisplay(EntityType.ITEM_DISPLAY, level) {
            @Override
            public boolean shouldBeSaved() { return false; }
        };
        var displayData = new net.minecraft.nbt.CompoundTag();
        displayData.put("item", visual.save(level.registryAccess()));
        displayData.putString("item_display", "fixed");
        doll.load(displayData);
        doll.setPos(origin.add(0, 0.9, 0));
        doll.setYRot(player.getYRot());
        doll.setNoGravity(true);
        level.addFreshEntity(doll);
        level.sendParticles(THREAD, origin.x, origin.y + 0.9, origin.z, 90, 0.4, 0.8, 0.4, 0.08);
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM,
                new ItemStack(ModItems.RED_WOOL_STRIP.get())),
                origin.x, origin.y + 0.9, origin.z, 48, 0.3, 0.7, 0.3, 0.14);
        level.playSound(null, BlockPos.containing(origin), SoundEvents.TOTEM_USE,
                SoundSource.PLAYERS, 0.8F, 1.3F);
        EFFECTS.add(new Substitution(player, doll, visual, origin.add(0, 0.9, 0), level.getGameTime()));
        return true;
    }

    private static Vec3 safeDestination(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 origin = player.position();
        double phase = player.getRandom().nextDouble() * Math.PI * 2;
        for (int ring = 0; ring < 4; ring++) {
            double radius = 3 + ring;
            for (int angle = 0; angle < 24; angle++) {
                double radians = phase + angle * Math.PI / 12;
                for (int dy : new int[] {0, 1, -1, 2, -2}) {
                    Vec3 candidate = new Vec3(origin.x + Math.cos(radians) * radius,
                            Math.floor(origin.y) + dy, origin.z + Math.sin(radians) * radius);
                    double distance = candidate.distanceToSqr(origin);
                    if (distance < 9 || distance > 36) continue;
                    BlockPos feet = BlockPos.containing(candidate);
                    if (!level.hasChunkAt(feet) || !level.hasChunkAt(feet.below())
                            || !level.getWorldBorder().isWithinBounds(feet)
                            || !level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(),
                                    net.minecraft.core.Direction.UP)) continue;
                    var box = player.getBoundingBox().move(candidate.subtract(origin));
                    if (!level.getWorldBorder().isWithinBounds(box)
                            || !level.noCollision(player, box) || level.containsAnyLiquid(box)) continue;
                    boolean danger = BlockPos.betweenClosedStream(box.inflate(0.1, 0.1, 0.1))
                            .anyMatch(pos -> {
                                var state = level.getBlockState(pos);
                                return state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)
                                        || state.is(Blocks.CACTUS) || state.is(Blocks.MAGMA_BLOCK)
                                        || state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE)
                                        || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.POWDER_SNOW);
                            });
                    if (!danger) return candidate;
                }
            }
        }
        return null;
    }

    @SubscribeEvent
    public static void tickEffects(ServerTickEvent.Post event) {
        EFFECTS.removeIf(effect -> {
            ServerLevel level = (ServerLevel) effect.doll.level();
            long age = level.getGameTime() - effect.start;
            if (effect.player.hasDisconnected() || effect.player.level() != level || age >= 20) {
                effect.doll.discard();
                return true;
            }
            if (age == 6) {
                effect.doll.discard();
                level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, effect.visual),
                        effect.origin.x, effect.origin.y, effect.origin.z, 35, 0.2, 0.3, 0.2, 0.15);
                level.playSound(null, BlockPos.containing(effect.origin), SoundEvents.WOOD_BREAK,
                        SoundSource.PLAYERS, 1, 0.8F);
            }
            if (age >= 6) {
                level.sendParticles(ParticleTypes.SMOKE, effect.origin.x, effect.origin.y, effect.origin.z,
                        5, 0.2, 0.25, 0.2, 0.02);
                level.sendParticles(ParticleTypes.ASH, effect.origin.x, effect.origin.y, effect.origin.z,
                        7, 0.3, 0.3, 0.3, 0.02);
                if (age < 12) level.sendParticles(ParticleTypes.FLAME,
                        effect.origin.x, effect.origin.y, effect.origin.z, 4, 0.2, 0.2, 0.2, 0.01);
            }
            Vec3 end = effect.player.position().add(0, 0.8, 0);
            for (int i = 0; i <= 24; i++) {
                Vec3 point = effect.origin.lerp(end, i / 24.0);
                level.sendParticles(THREAD, point.x, point.y, point.z, 1, 0, 0, 0, 0);
            }
            return false;
        });
    }

    @SubscribeEvent
    public static void stop(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
        EFFECTS.forEach(effect -> effect.doll.discard());
        EFFECTS.clear();
    }

    private record Substitution(ServerPlayer player, Display.ItemDisplay doll, ItemStack visual, Vec3 origin, long start) {}
}
