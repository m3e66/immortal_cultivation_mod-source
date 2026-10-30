package com.example.immortal_cultivation_mod.config;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.entity.XuanjinZhengEntity;
import com.example.immortal_cultivation_mod.item.QiInfusedWeapon;
import com.example.immortal_cultivation_mod.spell.UndeadControl;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Player choices restrict special terrain effects; they never bypass world or block protections. */
@EventBusSubscriber(modid = ImmortalCultivationMod.MODID)
public final class BlockBreakingSettings {
    private static final String TAG = "ImmortalCultivationBlockBreakingDisabled";
    private static final ThreadLocal<Source> SOURCE = new ThreadLocal<>();
    private static final ThreadLocal<Entity> COMMITTED_ACTOR = new ThreadLocal<>();

    public enum Category {
        ALL("all", 0), SPELLS("spells", 1), WEAPONS("weapons", 2), EXTERNAL_QI("external_qi", 3),
        FLAME_VESSELS("flame_vessels", 4), COMBAT_IMPACTS("combat_impacts", 5),
        COMPANIONS("companions", 6), TECHNIQUES("techniques", 7), UNDEAD_DOMAINS("undead_domains", 8),
        SPECIAL_ITEMS("special_items", 10);

        private final String id;
        private final int bit;
        Category(String id, int bitIndex) { this.id = id; bit = 1 << bitIndex; }
        public String id() { return id; }
        public int bit() { return bit; }
    }

    public record Source(Category category, Entity actor) { }
    private BlockBreakingSettings() { }

    public static int validMask() {
        int mask = 0;
        for (Category category : Category.values()) mask |= category.bit();
        return mask;
    }

    public static int disabledMask(ServerPlayer player) {
        var data = player.getPersistentData();
        if (data.contains(TAG)) return data.getInt(TAG) & validMask();
        return QiInfusedWeapon.outwardQiBlockBreakingEnabled(player) ? 0 : Category.EXTERNAL_QI.bit();
    }

    public static void setDisabledMask(ServerPlayer player, int disabledMask) {
        int mask = disabledMask & validMask();
        player.getPersistentData().putInt(TAG, mask);
        QiInfusedWeapon.setOutwardQiBlockBreaking(player, (mask & Category.EXTERNAL_QI.bit()) == 0);
    }

    public static boolean enabled(Entity actor, Category category) {
        ServerPlayer player = playerOwner(actor);
        if (player == null) return !hasPlayerOwner(actor);
        if (player.isRemoved() && COMMITTED_ACTOR.get() != player) return false;
        int mask = disabledMask(player);
        if ((mask & (Category.ALL.bit() | category.bit())) != 0) return false;
        if (projectileSource(actor) != player
                && (mask & Category.COMPANIONS.bit()) != 0) return false;
        return category != Category.EXTERNAL_QI || QiInfusedWeapon.outwardQiBlockBreakingEnabled(player);
    }

    /** Resolve attribution through projectiles and controlled creatures, including offline-owner guards. */
    public static ServerPlayer playerOwner(Entity actor) {
        Entity current = actor;
        for (int depth = 0; current != null && depth < 8; depth++) {
            if (current instanceof ServerPlayer player) return player;
            if (current instanceof Projectile projectile && projectile.getOwner() != current) {
                current = projectile.getOwner();
                continue;
            }
            UUID owner = ownerId(current);
            if (owner == null || !(current.level() instanceof ServerLevel level)) return null;
            Entity resolved = level.getEntity(owner);
            if (resolved == null) resolved = level.getServer().getPlayerList().getPlayer(owner);
            if (resolved == current) return null;
            current = resolved;
        }
        return null;
    }

    private static UUID ownerId(Entity actor) {
        if (actor instanceof XuanjinZhengEntity companion && !companion.isFree()) return companion.ownerId();
        if (actor instanceof OwnableEntity owned && owned.getOwnerUUID() != null) return owned.getOwnerUUID();
        CompoundTag data = actor.getPersistentData();
        if (data.hasUUID(UndeadControl.TAG_OWNER)) return data.getUUID(UndeadControl.TAG_OWNER);
        if (data.hasUUID(UndeadControl.TAG_NPC_SUMMON_OWNER)) return data.getUUID(UndeadControl.TAG_NPC_SUMMON_OWNER);
        var npc = data.getCompound("ImmortalCultivationNpc");
        if (npc.hasUUID("CommandOwner")) return npc.getUUID("CommandOwner");
        if (npc.hasUUID("FollowingPlayer")) return npc.getUUID("FollowingPlayer");
        return null;
    }

    private static boolean hasPlayerOwner(Entity actor) {
        Entity source = projectileSource(actor);
        return source != null && ownerId(source) != null;
    }

    private static Entity projectileSource(Entity actor) {
        Entity current = actor;
        for (int depth = 0; current instanceof Projectile projectile && depth < 8; depth++) {
            Entity owner = projectile.getOwner();
            if (owner == null || owner == current) return current;
            current = owner;
        }
        return current;
    }

    public static Source source(Category fallback, Entity actor) {
        Source current = SOURCE.get();
        return current == null ? new Source(fallback, actor)
                : new Source(current.category(), current.actor() == null ? actor : current.actor());
    }

    public static <T> T withSource(Category category, Entity actor, Supplier<T> action) {
        Source previous = SOURCE.get();
        SOURCE.set(new Source(category, actor));
        try { return action.get(); }
        finally { if (previous == null) SOURCE.remove(); else SOURCE.set(previous); }
    }

    public static void runWithSource(Category category, Entity actor, Runnable action) {
        withSource(category, actor, () -> { action.run(); return null; });
    }

    /** Finish a blast that already happened even if its player dies; settings and claims still apply. */
    public static <T> T withCommittedSource(Category category, Entity actor, Supplier<T> action) {
        Entity previous = COMMITTED_ACTOR.get();
        COMMITTED_ACTOR.set(actor);
        try { return withSource(category, actor, action); }
        finally { if (previous == null) COMMITTED_ACTOR.remove(); else COMMITTED_ACTOR.set(previous); }
    }

    public static void copy(Player original, Player clone) {
        if (original.getPersistentData().contains(TAG))
            clone.getPersistentData().putInt(TAG, original.getPersistentData().getInt(TAG) & validMask());
    }

    @SubscribeEvent public static void cloned(PlayerEvent.Clone event) { copy(event.getOriginal(), event.getEntity()); }
}
