package com.example.immortal_cultivation_mod.fate;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.block.ModBlocks;
import com.example.immortal_cultivation_mod.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import top.theillusivec4.curios.api.CuriosApi;

@EventBusSubscriber(modid = ImmortalCultivationMod.MODID)
public final class FateEncounters {
    private static final String[] OPPORTUNITIES = {
            "lost_pouch", "rare_herb", "wounded_beast", "merchant", "inheritance", "ore_deposit", "ancient_corpse", "forgotten_ruins"};
    private FateEncounters() {}
    public static void tick(ServerPlayer player) {
        if (player.isSpectator() || player.isCreative() || !player.isAlive() || player.tickCount % 20 != 0) return;
        var fate = LuckSystem.data(player);
        long now = player.serverLevel().getGameTime();
        if (fate.contains("Encounter")) {
            var encounter = fate.getCompound("Encounter");
            if (now > encounter.getLong("Expires")) fate.remove("Encounter");
            else if (encounter.getString("Dimension").equals(player.level().dimension().location().toString())) {
                BlockPos p = BlockPos.of(encounter.getLong("Pos"));
                if (player.distanceToSqr(p.getX(), p.getY(), p.getZ()) < 40 * 40 && player.serverLevel().hasChunkAt(p))
                    player.serverLevel().sendParticles(ParticleTypes.END_ROD, p.getX() + .5, p.getY() + 1.3, p.getZ() + .5,
                            2, .2, .4, .2, .01);
            }
        }
        if (!fate.contains("NextCheck")) { fate.putLong("NextCheck", now + 2400); return; }
        if (now < fate.getLong("NextCheck")) return;
        fate.putLong("NextCheck", now + 2400 + player.getRandom().nextInt(1200));
        // Standing at a farm cannot repeatedly roll opportunities or calamities.
        BlockPos current = player.blockPosition();
        if (fate.contains("LastExplore") && current.distSqr(BlockPos.of(fate.getLong("LastExplore"))) < 32 * 32) {
            DebugEyeDiagnostics.note(player, "check_skipped", DebugEyeDiagnostics.explorationGate(player));
            return;
        }
        fate.putLong("LastExplore", current.asLong());
        if (LuckSystem.karma(player) >= 100 && now >= fate.getLong("NextReckoning")) {
            if (misfortune(player, 2)) {
                LuckSystem.addKarma(player, -25);
                fate.putLong("NextReckoning", now + LuckSystem.DAY);
            }
            return; // Karmic reckonings never pass through avoidance or the luck coin.
        }
        int luck = LuckSystem.luck(player);
        double roll = player.getRandom().nextDouble();
        if (!fate.contains("Encounter") && DebugEyeDiagnostics.roll(player, "opportunity",
                LuckRules.opportunityChance(50), LuckRules.opportunityChance(luck), roll)) {
            opportunity(player); return;
        }
        if (fate.contains("Encounter")) DebugEyeDiagnostics.note(player, "pending_encounter");
        // Separate draw: Avoid Calamity never increases positive encounter odds.
        int severity = luck <= 10 && player.getRandom().nextFloat() < .10F ? 2
                : luck <= 25 && player.getRandom().nextFloat() < .25F ? 1 : 0;
        double base = LuckRules.misfortuneChance(luck);
        double guardedBase = FateTechniques.applyMisfortuneWard(player, base);
        double badRoll = player.getRandom().nextDouble();

        // 避祸诀 modifies only this negative-event roll. The opportunity roll above is untouched.
        if (FateTechniques.hasMisfortuneWard(player)) {
            DebugEyeDiagnostics.roll(player, "misfortune_ward", base, guardedBase, badRoll);
        }

        if (!DebugEyeDiagnostics.roll(
                player,
                "misfortune",
                LuckRules.misfortuneChance(50),
                guardedBase,
                badRoll)) return;

        DebugEyeDiagnostics.note(player, "severity", severity);

        // Existing 避劫术 can still protect the player. If both are active, they stack on the
        // already-reduced negative chance; positive opportunities are still unchanged.
        if (fate.getLong("AvoidUntil") > now) {
            DebugEyeDiagnostics.roll(
                    player,
                    "avoidance",
                    guardedBase,
                    LuckRules.avoidanceChance(guardedBase, severity),
                    badRoll);
        }
        if (fate.getLong("AvoidUntil") > now
                && badRoll >= LuckRules.avoidanceChance(guardedBase, severity)) {
            FateTechniques.ring(
                    player,
                    new net.minecraft.core.particles.DustParticleOptions(
                            new org.joml.Vector3f(.5F, .04F, .04F), .6F),
                    1,
                    0);
            return;
        }
        if (severity > 0 && consumeCoin(player)) { DebugEyeDiagnostics.note(player, "coin_used"); return; }
        misfortune(player, severity);
    }
    private static boolean consumeCoin(ServerPlayer player) {
        var coin = CuriosApi.getCuriosInventory(player).flatMap(h -> h.findFirstCurio(ModItems.FORTUNE_GUARD_COIN.get()));
        if (coin.isEmpty()) return false;
        coin.get().stack().shrink(1);
        com.example.immortal_cultivation_mod.guidance.DaoGuidanceSystem.notice(player, "accessory_triggered", ModItems.FORTUNE_GUARD_COIN.get().getDescription());
        player.serverLevel().sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 1, player.getZ(), 24, .4, .5, .4, .1);
        player.displayClientMessage(Component.translatable("luck.immortal_cultivation_mod.coin_breaks"), true);
        return true;
    }
    private static BlockPos site(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        for (int i = 0; i < 24; i++) {
            int x = player.getBlockX() + player.getRandom().nextInt(65) - 32;
            int z = player.getBlockZ() + player.getRandom().nextInt(65) - 32;
            if (Math.abs(x - player.getBlockX()) + Math.abs(z - player.getBlockZ()) < 16) continue;
            BlockPos column = new BlockPos(x, player.getBlockY(), z);
            if (!level.hasChunkAt(column)) continue;
            BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
            if (!level.getWorldBorder().isWithinBounds(pos) || !level.isEmptyBlock(pos) || !level.isEmptyBlock(pos.above())
                    || !level.getFluidState(pos.below()).isEmpty()
                    || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP)) continue;
            return pos;
        }
        return null;
    }
    private static void opportunity(ServerPlayer player) {
        var level = player.serverLevel();
        BlockPos pos = site(player);
        if (pos == null) { DebugEyeDiagnostics.note(player, "site_failed"); return; }
        int kind = player.getRandom().nextInt(OPPORTUNITIES.length);
        boolean major = LuckSystem.luck(player) >= 90 && player.getRandom().nextFloat() < .12F;
        if (major) kind = 4;
        int cost = major ? 15 : kind == 4 || kind == 7 ? 7 : 3;
        CompoundTag encounter = new CompoundTag();
        encounter.putLong("Pos", pos.asLong());
        encounter.putString("Dimension", level.dimension().location().toString());
        encounter.putLong("Expires", level.getGameTime() + LuckSystem.DAY);
        encounter.putString("Kind", OPPORTUNITIES[kind]);
        encounter.putString("Key", java.util.UUID.randomUUID().toString());
        encounter.putInt("Reward", major ? 5 : 2);
        if (kind == 1) {
            if (!ModBlocks.JIUYE_XIANLAN.get().defaultBlockState().canSurvive(level, pos)) {
                DebugEyeDiagnostics.note(player, "spawn_failed", "herb cannot survive here"); return;
            }
            level.setBlock(pos, ModBlocks.JIUYE_XIANLAN.get().defaultBlockState(), 3);
        } else if (kind == 2 || kind == 3) {
            Mob mob = kind == 2 ? EntityType.FOX.create(level) : EntityType.WANDERING_TRADER.create(level);
            if (mob == null) return;
            mob.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5, 0, 0);
            if (kind == 2) mob.setHealth(mob.getMaxHealth() * .25F);
            LuckSystem.data(mob).putUUID("EncounterOwner", player.getUUID());
            LuckSystem.data(mob).putString("EncounterKey", encounter.getString("Key"));
            if (!level.addFreshEntity(mob)) return;
        } else {
            if (!level.setBlock(pos, Blocks.CHEST.defaultBlockState(), 3)
                    || !(level.getBlockEntity(pos) instanceof ChestBlockEntity chest)) return;
            net.minecraft.world.item.Item treasure = kind == 5 ? ModItems.KONGMING_STONE.get()
                    : major ? ModItems.JINDAN_YAODAN.get() : ModItems.RESTRICTION_INSIGHT.get();
            chest.setItem(13, new ItemStack(treasure, 1));
            chest.setItem(9, new ItemStack(com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_STONE.get(), 8 + player.getRandom().nextInt(17)));
            chest.setChanged();
            if (kind == 4 || kind == 6 || kind == 7) {
                for (int i = 0; i < (major ? 3 : 1); i++) {
                    Zombie guard = EntityType.ZOMBIE.create(level);
                    if (guard == null) continue;
                    guard.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5, 0, 0);
                    guard.setPersistenceRequired();
                    guard.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(net.minecraft.world.item.Items.IRON_HELMET));
                    guard.setTarget(player);
                    guard.addTag("icm_fate_guard");
                    level.addFreshEntity(guard);
                }
            }
        }
        LuckSystem.data(player).put("Encounter", encounter);
        LuckSystem.change(player, -cost);
        DebugEyeDiagnostics.note(player, "encounter_created", OPPORTUNITIES[kind], pos.toShortString(), cost);
        player.sendSystemMessage(Component.translatable("luck.immortal_cultivation_mod.opportunity." + OPPORTUNITIES[kind]));
    }
    private static boolean misfortune(ServerPlayer player, int severity) {
        BlockPos pos = site(player);
        if (pos == null) { DebugEyeDiagnostics.note(player, "site_failed"); return false; }
        if (severity == 0) {
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 100, 0));
        } else {
            for (int i = 0; i < (severity == 2 ? 3 : 1); i++) {
                var enemy = EntityType.SKELETON.create(player.serverLevel());
                if (enemy == null) continue;
                enemy.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5, 0, 0);
                enemy.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(net.minecraft.world.item.Items.BOW));
                enemy.setTarget(player);
                player.serverLevel().addFreshEntity(enemy);
            }
        }
        player.sendSystemMessage(Component.translatable("luck.immortal_cultivation_mod.misfortune." + severity));
        DebugEyeDiagnostics.note(player, "misfortune_created", severity, pos.toShortString());
        return true;
    }
    private static void resolve(ServerPlayer player, BlockPos pos) {
        var tag = LuckSystem.data(player);
        if (!tag.contains("Encounter")) return;
        var encounter = tag.getCompound("Encounter");
        if (encounter.getLong("Pos") != pos.asLong()
                || !encounter.getString("Dimension").equals(player.level().dimension().location().toString())) return;
        LuckSystem.rewardOnce(player, "encounter_" + encounter.getString("Key"), encounter.getInt("Reward"));
        tag.remove("Encounter");
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void open(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getLevel().getBlockEntity(event.getPos()) instanceof ChestBlockEntity)
            resolve(player, event.getPos());
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void breakBlock(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        resolve(player, event.getPos());
        var sacred = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "fate_sacred"));
        if (event.getState().is(sacred)) { LuckSystem.change(player, -2); LuckSystem.addKarma(player, 5); }
    }
    @SubscribeEvent
    public static void helpBeast(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getTarget() instanceof Fox fox)
                || !event.getItemStack().is(net.minecraft.world.item.Items.SWEET_BERRIES)) return;
        var tag = LuckSystem.data(fox);
        if (!tag.hasUUID("EncounterOwner") || !tag.getUUID("EncounterOwner").equals(player.getUUID())) return;
        fox.heal(fox.getMaxHealth());
        if (!player.isCreative()) event.getItemStack().shrink(1);
        LuckSystem.rewardOnce(player, "beast_" + tag.getString("EncounterKey"), 3);
        tag.remove("EncounterOwner");
        LuckSystem.data(player).remove("Encounter");
        event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}
