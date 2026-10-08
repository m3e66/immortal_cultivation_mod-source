package com.example.immortal_cultivation_mod.companion;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.entity.ModEntities;
import com.example.immortal_cultivation_mod.entity.XuanjinZhengEntity;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import com.example.immortal_cultivation_mod.item.ModItems;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import java.util.List;
import java.util.UUID;

/** The player is the authoritative owner of a bond; item copies cannot duplicate a beast. */
@EventBusSubscriber(modid = ImmortalCultivationMod.MODID)
public final class XuanjinCompanion {
    public static final String KEY = "ImmortalXuanjinPassenger";
    private static final ResourceLocation POSSESSION = ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "xuanjin_possession");
    public static final List<String> COMMANDS = List.of("status", "manifest", "recall", "eyes", "claw", "reach", "voice", "feed", "unlock", "devour", "tails", "vortex", "swallow", "return", "refine", "possess", "dash", "free", "free_confirm");
    private XuanjinCompanion() {}

    public static CompoundTag state(Player player) {
        var data = player.getPersistentData();
        if (!data.contains(KEY)) data.put(KEY, new CompoundTag());
        return data.getCompound(KEY);
    }
    public static CompoundTag fresh() {
        var s = new CompoundTag();
        s.putUUID("Bond", UUID.randomUUID());
        s.putInt("Trust", 20); s.putInt("Freedom", 0);
        s.putFloat("Health", 120); s.putInt("Energy", 100);
        return s;
    }
    public static boolean possessing(Player p) {
        return state(p).getLong("PossessUntil") > p.level().getGameTime() && !state(p).getBoolean("Freed");
    }
    public static boolean isToken(ItemStack stack, Player player) {
        if (!stack.is(ModItems.XUANJIN_SEAL.get())) return false;
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        var s = state(player);
        return tag.hasUUID("XuanjinOwner") && tag.getUUID("XuanjinOwner").equals(player.getUUID())
                && tag.hasUUID("XuanjinBond") && s.hasUUID("Bond") && tag.getUUID("XuanjinBond").equals(s.getUUID("Bond"));
    }
    public static boolean carrying(Player p) {
        for (var stack : p.getInventory().items) if (isToken(stack, p)) return true;
        for (var stack : p.getInventory().offhand) if (isToken(stack, p)) return true;
        return false;
    }
    public static boolean bind(ServerPlayer p, ItemStack stack) {
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.hasUUID("XuanjinOwner")) {
            if (!isToken(stack, p)) { tell(p, "foreign"); return false; }
            if (state(p).getBoolean("Freed")) { inert(p, stack); tell(p, "gone"); return false; }
            return true;
        }
        boolean existing = state(p).hasUUID("Bond");
        if (existing && (carrying(p) || state(p).getBoolean("Freed") || possessing(p)
                || state(p).getLong("ManifestUntil") > p.level().getGameTime())) { tell(p, "already_bound"); return false; }
        if (!existing && CultivationLevels.getStageIndex(ModAttachments.getData(p).cultivationLevel()) < 3) { tell(p, "first_realm"); return false; }
        if (!existing) p.getPersistentData().put(KEY, fresh());
        tag.putUUID("XuanjinOwner", p.getUUID()); tag.putUUID("XuanjinBond", state(p).getUUID("Bond"));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        tell(p, existing ? "rebound" : "lore.0");
        return true;
    }
    private static void inert(ServerPlayer p, ItemStack stack) {
        stack.shrink(1);
        var lantern = new ItemStack(Items.LANTERN);
        if (!p.getInventory().add(lantern)) p.drop(lantern, false);
    }
    public static void tell(ServerPlayer p, String key, Object... args) {
        Component message=Component.translatable("xuanjin." + key,args);
        if(!XuanjinInterface.feedback(p,message))p.displayClientMessage(message,true);
    }
    public static void menu(ServerPlayer p) {
        XuanjinInterface.open(p);
    }
    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        var root = Commands.literal("xuanjin").executes(ctx -> execute(ctx.getSource().getPlayerOrException(), "status"));
        for (String command : COMMANDS) root.then(Commands.literal(command)
                .executes(ctx -> execute(ctx.getSource().getPlayerOrException(), command)));
        event.getDispatcher().register(root);
    }
    public static XuanjinZhengEntity body(ServerPlayer p) {
        var s = state(p);
        if (!s.hasUUID("Body")) return null;
        for (ServerLevel level : p.server.getAllLevels()) {
            if (level.getEntity(s.getUUID("Body")) instanceof XuanjinZhengEntity beast && !beast.isFree()) return beast;
        }
        return null;
    }
    public static LivingEntity actor(ServerPlayer p) { return possessing(p) ? p : body(p); }
    public static boolean ready(CompoundTag s, String action, long now) { return s.getLong("Cooldown." + action) <= now; }
    public static void cooldown(CompoundTag s, String action, long now) { s.putLong("Cooldown." + action, now + XuanjinRules.cooldownTicks(action)); }
    public static boolean spend(CompoundTag s, int cost) {
        if (s.getInt("Energy") < cost) return false;
        s.putInt("Energy", s.getInt("Energy") - cost); return true;
    }
    public static void energy(CompoundTag s, int gain) {
        s.putInt("Energy", Math.clamp(s.getInt("Energy") + gain, 0, XuanjinRules.stage(s.getInt("Seal")).energy()));
    }
    public static void release(CompoundTag s, int freedom) { s.putInt("Freedom", Math.min(100, s.getInt("Freedom") + freedom)); }

    public static int execute(ServerPlayer p, String command) {
        if (!p.isAlive() || p.isSpectator() || !carrying(p) || state(p).getBoolean("Freed")) { tell(p, "need_lantern"); return 0; }
        var s = state(p); long now = p.level().getGameTime(); int stage = s.getInt("Seal");
        if (command.equals("status")) { menu(p); return 1; }
        if (command.equals("recall")) { recall(p); return 1; }
        if (command.equals("feed")) return feed(p);
        if (command.equals("unlock")) return unlock(p);
        if (command.equals("free")) {
            if (stage < 6) { tell(p, "locked"); return 0; }
            s.putLong("FreeConfirmUntil", now + 200);
            menu(p);
            tell(p,"free_warning");
            return 1;
        }
        if (command.equals("free_confirm")) {
            if (stage < 6 || s.getLong("FreeConfirmUntil") <= now) { tell(p, "confirm_expired"); return 0; }
            if (s.getLong("RecoverUntil") > now || s.getFloat("Health") <= 0) { tell(p, "recovering"); return 0; }
            if (s.hasUUID("Body") && body(p) == null && s.getLong("ManifestUntil") > now) { tell(p, "active"); return 0; }
            var beast = body(p);
            if (beast == null) { beast = spawn(p, false); if (beast == null) return 0; }
            endPossession(p); beast.free(s.copy());
            s.putBoolean("Freed", true); s.remove("Body"); s.remove("FreeConfirmUntil");
            for (int i=0;i<p.getInventory().items.size();i++) if (isToken(p.getInventory().items.get(i),p)) p.getInventory().items.set(i,new ItemStack(Items.LANTERN));
            for (int i=0;i<p.getInventory().offhand.size();i++) if (isToken(p.getInventory().offhand.get(i),p)) p.getInventory().offhand.set(i,new ItemStack(Items.LANTERN));
            tell(p, "freed"); return 1;
        }
        if (command.equals("manifest")) {
            if (!ready(s, command, now) || possessing(p) || s.getLong("ManifestUntil") > now) { tell(p, "active"); return 0; }
            if (s.getLong("RecoverUntil") > now || s.getFloat("Health") <= 0) { tell(p, "recovering"); return 0; }
            if (!obeys(p)) return 0;
            var beast = spawn(p, true); if (beast == null) return 0;
            release(s, stage == 6 ? 8 : 2); cooldown(s,command,now); return 1;
        }
        if (command.equals("possess")) {
            if (stage < 6) { tell(p, "locked"); return 0; }
            if (possessing(p) || s.getLong("ManifestUntil") > now || !ready(s, command, now) || s.getLong("RecoverUntil") > now) { tell(p, "active"); return 0; }
            if (!obeys(p)) return 0;
            if (!spend(s, 1000)) { tell(p, "energy_low"); return 0; }
            s.putLong("PossessUntil", now + XuanjinRules.POSSESSION_TICKS); cooldown(s,command,now); release(s, 10);
            s.putString("PossessDimension", p.level().dimension().location().toString());
            applyPossession(p); tell(p, "possessed"); return 1;
        }
        int needed = switch (command) { case "eyes" -> 1; case "claw", "reach" -> 2; case "voice" -> 4; case "devour" -> 5; default -> 6; };
        if (stage < needed) { tell(p, "locked"); return 0; }
        if (!ready(s, command, now)) { tell(p, "cooldown", Math.max(1, (s.getLong("Cooldown." + command) - now + 19) / 20)); return 0; }
        if (!obeys(p)) return 0;
        if (command.equals("eyes")) { eyes(p);XuanjinVisuals.emit(p,com.example.immortal_cultivation_mod.network.XuanjinPayloads.EffectKind.SIGHT,24); cooldown(s,command,now); release(s, 1); return 1; }
        if (command.equals("reach")) {
            if (p.pick(6,0,false) instanceof net.minecraft.world.phys.BlockHitResult hit
                    && p.level().mayInteract(p,hit.getBlockPos())) {
                // Use the normal server interaction path, including protection/event hooks.
                var result = p.gameMode.useItemOn(p,p.serverLevel(),p.getMainHandItem(),InteractionHand.MAIN_HAND,hit);
                if (result.consumesAction()) { XuanjinVisuals.emit(p,com.example.immortal_cultivation_mod.network.XuanjinPayloads.EffectKind.DASH,hit.getLocation(),12);cooldown(s,command,now); release(s,1); return 1; }
            }
            tell(p,"reach_failed"); return 0;
        }
        LivingEntity actor = actor(p);
        // Claws and voice can project directly from the seal without summoning a body.
        if (actor == null && (command.equals("claw") || command.equals("voice"))) actor = p;
        if (actor == null) { tell(p, "manifest_first"); return 0; }
        boolean done = XuanjinCombat.command(actor, s, command, p);
        if (done) release(s, 1); else tell(p, "unavailable");
        return done ? 1 : 0;
    }
    private static boolean obeys(ServerPlayer p) {
        var s = state(p); long now = p.level().getGameTime();
        if (s.getLong("RefusedUntil") > now) { tell(p, "refusal"); return false; }
        if (p.getRandom().nextFloat() < XuanjinRules.refusal(s.getInt("Trust"), s.getInt("Freedom"))) {
            s.putLong("RefusedUntil", now + 100); tell(p, "refusal"); return false;
        }
        return true;
    }
    private static int unlock(ServerPlayer p) {
        var s = state(p); int next = s.getInt("Seal") + 1;
        if (next > 6) { tell(p, "all_seals"); return 0; }
        if (body(p) != null || possessing(p) || s.getLong("ManifestUntil") > p.level().getGameTime()) { tell(p, "recall_first"); return 0; }
        if (CultivationLevels.getStageIndex(ModAttachments.getData(p).cultivationLevel()) < XuanjinRules.stage(next).realmStage()) {
            tell(p, "realm_required", Component.translatable("xuanjin.realm." + next)); return 0;
        }
        int trust = XuanjinRules.unlockTrust(next), cost = XuanjinRules.unlockEnergy(next);
        if (s.getInt("Trust") < trust || !spend(s, cost)) { tell(p, "unlock_cost", trust, cost); return 0; }
        s.putInt("Seal", next); s.putFloat("Health", XuanjinRules.stage(next).health());
        release(s, 12); tell(p, "lore." + next); menu(p); return 1;
    }
    private static int feed(ServerPlayer p) {
        var s = state(p); long now = p.level().getGameTime();
        if (!ready(s, "feed", now)) { tell(p, "cooldown", (s.getLong("Cooldown.feed") - now + 19) / 20); return 0; }
        // Broken spirit energy is given willingly; feeding never drains a living ally.
        var data = ModAttachments.getData(p);
        int cost = Math.max(20, XuanjinRules.stage(s.getInt("Seal")).energy() / 10);
        if (data.qi() < cost) { tell(p, "feed_qi", cost); return 0; }
        ModAttachments.setData(p, data.withQi(data.qi() - cost)); ServerEvents.syncPlayerData(p);
        energy(s, cost * 2); s.putInt("Trust", Math.min(100, s.getInt("Trust") + 5));
        if (s.getLong("RecoverUntil") <= now) {
            float heal = XuanjinRules.stage(s.getInt("Seal")).health() * .20F;
            var b = body(p); if (b != null) b.heal(heal);
            else s.putFloat("Health", Math.min(XuanjinRules.stage(s.getInt("Seal")).health(), s.getFloat("Health") + heal));
        }
        cooldown(s,"feed",now); tell(p, "fed"); return 1;
    }
    private static XuanjinZhengEntity spawn(ServerPlayer p, boolean timed) {
        var s = state(p); var beast = ModEntities.XUANJIN_ZHENG.get().create(p.serverLevel());
        if (beast == null) return null;
        beast.bind(p, s.getInt("Seal"));
        Vec3 facing = p.getLookAngle().multiply(1, 0, 1).normalize();
        boolean found = false;
        for (int turn = 0; turn < 8 && !found; turn++) {
            Vec3 offset = facing.yRot((float)(turn * Math.PI / 4)).scale(3 + beast.getBbWidth());
            for (int dy = 0; dy <= 3; dy++) {
                beast.moveTo(p.getX() + offset.x, p.getY() + dy, p.getZ() + offset.z, p.getYRot(), 0);
                if (p.level().noCollision(beast) && p.level().getWorldBorder().isWithinBounds(beast.getBoundingBox())) { found = true; break; }
            }
        }
        if (!found) { tell(p, "space"); return null; }
        if (!p.serverLevel().addFreshEntity(beast)) return null;
        XuanjinVisuals.emit(beast,com.example.immortal_cultivation_mod.network.XuanjinPayloads.EffectKind.MANIFEST,30);
        beast.setHealth(Math.max(1, s.getFloat("Health")));
        s.putUUID("Body", beast.getUUID()); s.putLong("ManifestUntil", p.level().getGameTime() + (timed ? 300 : 1));
        tell(p, "manifested"); return beast;
    }
    public static void recall(ServerPlayer p) {
        var s = state(p); var beast = body(p);
        boolean active = beast != null || possessing(p) || s.getLong("ManifestUntil") > p.level().getGameTime();
        if (beast != null) { s.putFloat("Health", beast.getHealth()); beast.discard(); }
        s.remove("Body"); s.putLong("ManifestUntil", 0); endPossession(p);
        if (active) s.putInt("Freedom", Math.max(0, s.getInt("Freedom") - 1));
        tell(p, "recalled");
    }
    public static void endPossession(ServerPlayer p) {
        state(p).putLong("PossessUntil", 0);
        for (var attribute : List.of(Attributes.ARMOR, Attributes.MOVEMENT_SPEED)) {
            var value = p.getAttribute(attribute); if (value != null) value.removeModifier(POSSESSION);
        }
        XuanjinCombat.clearFullBonuses(p);
    }
    private static void applyPossession(ServerPlayer p) {
        var armor = p.getAttribute(Attributes.ARMOR); var speed = p.getAttribute(Attributes.MOVEMENT_SPEED);
        if (armor != null && !armor.hasModifier(POSSESSION)) armor.addTransientModifier(new AttributeModifier(POSSESSION, 350, AttributeModifier.Operation.ADD_VALUE));
        if (speed != null && !speed.hasModifier(POSSESSION)) speed.addTransientModifier(new AttributeModifier(POSSESSION, .50, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }
    public static void eyes(ServerPlayer p) {
        int count = 0;
        for (var e : p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(24), e -> XuanjinCombat.enemy(p, e))) {
            if (p.distanceToSqr(e) > 24 * 24) continue;
            e.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.GLOWING, 200)); count++;
            if (count <= 3) tell(p,"weakness",e.getDisplayName(),(int)e.getHealth(),(int)e.getAttributeValue(Attributes.ARMOR),
                    e.getActiveEffects().stream().filter(effect -> !effect.getEffect().value().isBeneficial()).count());
        }
        BlockPos origin = p.blockPosition(); int secrets = 0;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-8,-4,-8), origin.offset(8,4,8))) {
            if (!p.level().hasChunkAt(pos)) continue;
            var block = p.level().getBlockState(pos);
            String id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block.getBlock()).getPath();
            if (p.level().getBlockEntity(pos) instanceof RandomizableContainerBlockEntity
                    || p.level().getBlockEntity(pos) instanceof com.example.immortal_cultivation_mod.magicritual.block.custom.MagicCenterBlockEntity
                    || id.contains("formation") || id.contains("restriction") || id.contains("trap") || id.contains("curse")
                    || id.contains("tripwire") || id.contains("spawner")) {
                if (secrets++ < 8) tell(p, "trace", pos.getX(), pos.getY(), pos.getZ());
                p.serverLevel().sendParticles(p, ParticleTypes.SOUL, true, pos.getX()+.5, pos.getY()+1, pos.getZ()+.5, 3, .1, .1, .1, 0);
            }
        }
        tell(p, "sight", count, secrets);
        if (state(p).getInt("Freedom") >= 40) tell(p, "agenda");
    }
    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !state(p).hasUUID("Bond")) return;
        var s = state(p); long now = p.level().getGameTime();
        if (s.getBoolean("Freed")) return;
        if (possessing(p)) {
            if (!p.isAlive() || !carrying(p) || !p.level().dimension().location().toString().equals(s.getString("PossessDimension"))) endPossession(p);
            else { applyPossession(p); XuanjinCombat.tick(p, s); if (now % 4 == 0) p.serverLevel().sendParticles(ParticleTypes.SOUL_FIRE_FLAME,p.getX(),p.getY()+1,p.getZ(),3,.4,.6,.4,0); }
        } else endPossession(p);
        if (now % 20 != 0) return;
        regenerate(p);
        if (s.getLong("VortexUntil") > 0 && s.getLong("VortexUntil") <= now) XuanjinCombat.finishVortex(s);
        if (s.getLong("ManifestUntil") > 0 && (s.getLong("ManifestUntil") <= now || !p.isAlive() || !carrying(p))) recall(p);
        if (s.getLong("RecoverUntil") > 0 && s.getLong("RecoverUntil") <= now) { s.remove("RecoverUntil"); s.putFloat("Health", XuanjinRules.stage(s.getInt("Seal")).health() * .25F); }
        if (carrying(p) && now >= s.getLong("AdviceAt")) {
            s.putLong("AdviceAt", now + 2400);
            if (s.getInt("Freedom") >= 35) tell(p, s.getInt("Trust") >= 60 ? "advice" : "agenda");
        }
    }
    /** Online regeneration belongs to the bond, so extra lantern copies cannot multiply it. */
    public static void regenerate(ServerPlayer p) {
        var s=state(p);long now=p.level().getGameTime();
        if(!p.isAlive() || p.isSpectator() || s.getBoolean("Freed") || !carrying(p))return;
        if(s.contains("RegenAt") && now-s.getLong("RegenAt")<20 && now>=s.getLong("RegenAt"))return;
        s.putLong("RegenAt",now);energy(s,XuanjinRules.regeneration(s.getInt("Seal")));
    }
    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {
        if (event.getOriginal().getPersistentData().contains(KEY)) {
            var saved = event.getOriginal().getPersistentData().getCompound(KEY).copy();
            saved.remove("Body"); saved.putLong("ManifestUntil", 0); saved.putLong("PossessUntil", 0);
            event.getEntity().getPersistentData().put(KEY, saved);
        }
    }
    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && state(p).hasUUID("Bond") && !state(p).getBoolean("Freed")) recall(p);
    }
}
