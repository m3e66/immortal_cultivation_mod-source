package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ServerLevelAccessor;

public final class BeastRealms {
    public static final String NBT_KEY = "BeastRealm";
    private static final ResourceLocation SCALE = ResourceLocation.fromNamespaceAndPath(
            ImmortalCultivationMod.MODID, "beast_realm_scale");
    private static final List<String> REALMS = List.of(CultivationLevels.REALM_MORTAL,
            CultivationLevels.REALM_LIANQI, CultivationLevels.REALM_ZHUJI, CultivationLevels.REALM_JINDAN,
            CultivationLevels.REALM_YUANYING, CultivationLevels.REALM_HUASHEN, CultivationLevels.REALM_LIANXU,
            CultivationLevels.REALM_HETI, CultivationLevels.REALM_DACHENG, CultivationLevels.REALM_DUJIE);
    private BeastRealms() { }

    public static int baseRealm(EntityType<?> type) {
        return switch (BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath()) {
            case "jadecoil_snake", "moonshade_horse", "russet_spirit_deer", "chestnut_spirit_cow", "field_mouse" -> 0;
            case "azure_spirit_silkworm", "green_patterned_spirit_devouring_rat", "red_backed_iron_scale_scorpion" -> 1;
            case "kongming_beetle", "thunderhorn_rock_badger", "frostspine_cave_bear",
                    "wind_feather_cliff_deer", "ink_armored_stone_devouring_tortoise" -> 2;
            case "crimson_scale_flame_lizard", "ember_feather_fire_owl", "vault_guardian" -> 3;
            case "crimson_sky_flame_flood_dragon", "blood_ape_demon_general", "phantom_veil_fox" -> 4;
            default -> -1;
        };
    }
    public static List<Integer> variants(EntityType<?> type) {
        if (baseRealm(type) == 0) return List.of(0);
        if (type == ModEntities.VAULT_GUARDIAN.get()) return List.of(2, 3);
        if (type == ModEntities.PHANTOM_VEIL_FOX.get()) return List.of(2, 3, 4);
        int base = baseRealm(type);
        return base < 0 ? List.of() : List.of(base - 1, base, base + 1);
    }
    public static int clamp(EntityType<?> type, int realm) {
        var variants = variants(type);
        if (variants.isEmpty()) throw new IllegalArgumentException("Not a realm-variant beast: " + type);
        return Math.clamp(realm, variants.getFirst(), variants.getLast());
    }
    public static int randomRealm(EntityType<?> type, RandomSource random) {
        var variants = variants(type);
        return variants.get(random.nextInt(variants.size()));
    }
    public static int naturalRealm(EntityType<?> type, ServerLevelAccessor world, BlockPos pos) {
        long seed = com.example.immortal_cultivation_mod.flame.FlameCraterShape.mix(
                pos.asLong() ^ world.getLevel().getSeed() ^ BuiltInRegistries.ENTITY_TYPE.getKey(type).hashCode());
        var variants = variants(type);
        return variants.get((int)Math.floorMod(seed, variants.size()));
    }
    public static String level(int realm) {
        int index = Math.clamp(realm, 0, 9);
        return REALMS.get(index) + (index == 0 ? "" : CultivationLevels.STAGE_MID);
    }
    public static Component prefix(int realm, Component name) {
        return Component.literal("[").append(CultivationLevels.realmDisplayName(REALMS.get(Math.clamp(realm, 0, 9))))
                .append("] ").append(name);
    }
    public static int savedRealm(EntityType<?> type, CompoundTag tag) {
        if (tag.contains(NBT_KEY)) return clamp(type, tag.getInt(NBT_KEY));
        return switch (tag.getString("id")) {
            case "immortal_cultivation_mod:zhuji_phantom_veil_fox", "immortal_cultivation_mod:zhuji_vault_guardian" -> 2;
            case "immortal_cultivation_mod:jindan_phantom_veil_fox", "immortal_cultivation_mod:jindan_vault_guardian" -> 3;
            default -> baseRealm(type);
        };
    }
    public static double powerScale(Mob mob) {
        if (!(mob instanceof RealmBeast beast)) return 1;
        return powerScale(beast.beastRealm(), baseRealm(mob.getType()));
    }
    public static double powerScale(int realm, int baseRealm) {
        return (double)CultivationLevels.getLevelDef(level(realm)).maxHp()
                / CultivationLevels.getLevelDef(level(baseRealm)).maxHp();
    }
    public static void applyAttributes(Mob mob) {
        float fraction = mob.getMaxHealth() <= 0 ? 1 : mob.getHealth() / mob.getMaxHealth();
        if (mob instanceof PhantomVeilFoxEntity || mob instanceof VaultGuardianEntity) {
            // Legacy saves may carry old base values and permanent realm modifiers.
            for (var attribute : List.of(Attributes.MAX_HEALTH, Attributes.ATTACK_DAMAGE,
                    Attributes.ARMOR, Attributes.ARMOR_TOUGHNESS)) {
                var instance = mob.getAttribute(attribute);
                if (instance != null) instance.removeModifier(SCALE);
            }
        }
        if (mob instanceof PhantomVeilFoxEntity fox) {
            var tier = fox.realmTier();
            mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(tier.maxHealth());
            mob.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(tier.attackDamage());
            mob.getAttribute(Attributes.ARMOR).setBaseValue(tier.armor());
            mob.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(tier.armorToughness());
            mob.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(tier.movementSpeed());
            mob.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(tier.knockbackResistance());
        } else if (mob instanceof VaultGuardianEntity guardian) {
            double scale = powerScale(mob);
            mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(
                    CultivationLevels.getLevelDef(guardian.beastCultivationLevel()).maxHp());
            mob.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(240 * scale);
            mob.getAttribute(Attributes.ARMOR).setBaseValue(42 * Math.sqrt(scale));
            mob.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(14 * Math.sqrt(scale));
            mob.getAttribute(Attributes.ATTACK_SPEED).setBaseValue(guardian.isJindanVariant() ? 3.6 : 3.2);
            mob.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(guardian.isJindanVariant() ? .28 : .23);
            mob.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(guardian.isJindanVariant() ? 38 : 32);
            mob.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(guardian.isJindanVariant() ? .96 : .82);
            mob.getAttribute(Attributes.STEP_HEIGHT).setBaseValue(guardian.isJindanVariant() ? 1.2 : 1.1);
        } else {
            double scale = powerScale(mob);
            for (var attribute : List.of(Attributes.MAX_HEALTH, Attributes.ARMOR, Attributes.ARMOR_TOUGHNESS)) {
                var instance = mob.getAttribute(attribute);
                if (instance == null) continue;
                instance.removeModifier(SCALE);
                if (scale != 1) instance.addPermanentModifier(new AttributeModifier(SCALE,
                        (attribute.equals(Attributes.MAX_HEALTH) ? scale : Math.sqrt(scale)) - 1,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            }
        }
        mob.setHealth(Math.max(0, fraction * mob.getMaxHealth()));
    }
}
