package com.example.immortal_cultivation_mod.world;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.entity.CultivationNpc;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;

/** Realm-gated lunar gravity used by the Cosmic Void dimension. */
@EventBusSubscriber(modid = ImmortalCultivationMod.MODID)
public final class CosmicVoidGravity {
    public static final ResourceKey<Level> DIMENSION = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(
                    ImmortalCultivationMod.MODID, "cosmic_void"));

    /** The Moon's surface gravity is approximately one sixth of Earth's. */
    public static final double GRAVITY_SCALE = 1.0D / 6.0D;

    private CosmicVoidGravity() {
    }

    /** Called from the Entity gravity mixin on both logical sides. */
    public static double adjustGravity(
            Entity entity, double normalGravity) {
        if (normalGravity == 0.0D) {
            return normalGravity;
        }
        return normalGravity * gravityScale(entity);
    }

    public static boolean isAffected(Entity entity) {
        return gravityScale(entity) != 1.0D;
    }

    private static double gravityScale(Entity entity) {
        if (entity == null
                || !entity.level().dimension().equals(DIMENSION)) {
            return 1.0D;
        }
        if (entity instanceof ItemEntity) {
            return GRAVITY_SCALE;
        }
        if (!(entity instanceof LivingEntity living)) {
            return 1.0D;
        }
        return isYuanyingOrLower(living) ? GRAVITY_SCALE : 1.0D;
    }

    public static boolean isYuanyingOrLower(LivingEntity living) {
        if (living instanceof Player player) {
            String realm = ModAttachments.getData(player)
                    .cultivationLevel();
            return CultivationLevels.getRealmIndex(realm)
                    <= CultivationLevels.getRealmIndex(
                            CultivationLevels.REALM_YUANYING);
        }

        if (living instanceof CultivationNpc) {
            // Every currently registered cultivation NPC archetype is Jindan
            // or lower, so all of them receive lunar gravity.
            return true;
        }

        // Animals, monsters, and other non-cultivators count as mortal.
        return true;
    }

    /** Fall impact follows the same realm-specific gravity scale. */
    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        double scale = gravityScale(event.getEntity());
        if (scale != 1.0D) {
            event.setDamageMultiplier((float) (
                    event.getDamageMultiplier() * scale));
        }
    }
}
