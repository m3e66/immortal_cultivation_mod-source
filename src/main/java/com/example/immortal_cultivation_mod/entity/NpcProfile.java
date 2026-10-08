package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.attachment.SpiritRoots;
import com.example.immortal_cultivation_mod.block.ModBlocks;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import com.example.immortal_cultivation_mod.item.AttackTalismanItem;
import com.example.immortal_cultivation_mod.item.CultivationItemRarity;
import com.example.immortal_cultivation_mod.item.CultivationAccessoryItem;
import com.example.immortal_cultivation_mod.item.DisguiseMaskItem;
import com.example.immortal_cultivation_mod.item.ForgingSystem;
import com.example.immortal_cultivation_mod.item.IdentityItems;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.item.QiInfusedWeapon;
import com.example.immortal_cultivation_mod.item.ShihunfanItem;
import com.example.immortal_cultivation_mod.item.TalismanItem;
import com.example.immortal_cultivation_mod.item.WantedOrderItem;
import com.example.immortal_cultivation_mod.item.WeaponRestrictionSystem;
import com.example.immortal_cultivation_mod.network.ModPayloads;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import com.example.immortal_cultivation_mod.spell.UndeadControl;
import com.example.immortal_cultivation_mod.spell.UndeadAbilitySystem;
import com.example.immortal_cultivation_mod.system.SystemShopPrices;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class NpcProfile {
    public static final String FANREN = "fanren";
    public static final String LIANQI = "lianqi";
    public static final String ZHUJI = "zhuji";
    public static final String JINDAN = "jindan";

    private static final String TAG = "ImmortalCultivationNpc";
    private static final int PROFILE_VERSION = 19;
    private static final int INVENTORY_SIZE = 27;
    private static final int INVENTORY_TRUST_REQUIRED = 50;
    private static final int NPC_LEARNING_TRUST_REQUIRED = 10;
    private static final float NPC_INVENTORY_DROP_CHANCE = 0.65F;
    private static final long FOG_MIRROR_RECIPE_SALT = 0x464F474D495252L;
    private static final long YINYANG_COMPASS_RECIPE_SALT = 0x59494E59414E47L;
    private static final long SOUL_BOTTLE_RECIPE_SALT = 0x534F554C424F54L;
    private static final List<Integer> METHOD_LEARNING_TRUST_BY_REALM = List.of(
            0, 20, 30, 40, 50, 60, 70, 80, 90, 100);
    // One gift expectation per major realm, from Mortal through Tribulation.
    private static final List<Integer> GIFT_EXPECTATION_BY_REALM = List.of(
            1,
            10, 100, 1_000,
            10_000, 100_000, 500_000,
            1_000_000, 1_500_000, 10_000_000);
    private static final long INTERACTION_TIMEOUT_TICKS = 120L;
    private static final long SOCIAL_REFUSAL_TICKS = 1_200L;
    private static final long SOCIAL_CHANCE_RESET_TICKS = 24_000L;
    private static final long TICKS_PER_INGAME_DAY = 24_000L;
    private static final int THOUGHT_REGEN_INTERVAL_TICKS = 20 * 60;
    private static final String PLAYER_REPUTATION_TAG = "ImmortalCultivationNpcReputation";
    private static final String EQUIPPED_WEAPON_SLOT_TAG = "EquippedCombatWeaponSlot";
    public static final String PLAYER_GENDER_TAG = "ImmortalCultivationPlayerGender";
    private static final String PLAYER_SPAR_NPC_TAG = "ImmortalCultivationSparNpc";
    private static final String WEAPON_CARE_SPAR_REWARD_DAY =
            "WeaponCareSparRewardDay";
    private static final String NPC_OWNED_EFFECT_TAG = "ImmortalCultivationNpcOwnedAtLaunch";
    private static final String LOW_SPIRIT_STONE = "immortal_cultivation_mod:spirit_stone";
    private static final String MEDIUM_SPIRIT_STONE = "immortal_cultivation_mod:medium_spirit_stone";
    private static final String HIGH_SPIRIT_STONE = "immortal_cultivation_mod:high_spirit_stone";
    private static final String COPPER_COIN = ImmortalCultivationMod.MODID + ":copper_coin";
    private static final String HOTELKEEPER_SKIN = "hotelkeeper";
    private static final int HOTELKEEPER_KIT_VERSION = 2;
    private static final String GENDER_MALE = NpcSkinCatalog.MALE;
    private static final String GENDER_FEMALE = NpcSkinCatalog.FEMALE;
    private static final String CORPSE_SUMMON_USED = "CorpseSummonUsed";
    private static final String CORPSE_SUMMON_UUID = "CorpseSummonUuid";
    private static final String ROYAL_PENDANT_USED = "RoyalPendantUsed";
    private static final List<String> CULTIVATOR_IDENTITIES = List.of(
            "wandering_cultivator", "sword_cultivator", "saber_cultivator", "demonic_cultivator",
            "artifact_cultivator", "corpse_cultivator", "body_cultivator", "evil_cultivator",
            "talisman_cultivator", "formation_cultivator", "clan_heir", "royal", "wanted");
    private static final Map<String, List<QuestDef>> IDENTITY_QUESTS = createIdentityQuests();
    private static final Map<String, NpcTradePrice> NPC_TRADE_PRICES = Map.ofEntries(
            Map.entry("minecraft:bread", new NpcTradePrice(COPPER_COIN, 1)),
            Map.entry("minecraft:wheat", new NpcTradePrice(COPPER_COIN, 1)),
            Map.entry("minecraft:iron_ingot", new NpcTradePrice(COPPER_COIN, 3)),
            Map.entry("minecraft:gold_ingot", new NpcTradePrice(COPPER_COIN, 6)),
            Map.entry("minecraft:emerald", new NpcTradePrice(COPPER_COIN, 8)),
            Map.entry("minecraft:amethyst_shard", new NpcTradePrice(COPPER_COIN, 4)),
            Map.entry("minecraft:blaze_powder", new NpcTradePrice(COPPER_COIN, 8)),
            Map.entry("minecraft:ender_pearl", new NpcTradePrice(COPPER_COIN, 12)),
            Map.entry("minecraft:diamond", new NpcTradePrice(COPPER_COIN, 16)),
            Map.entry("minecraft:ghast_tear", new NpcTradePrice(COPPER_COIN, 20)),
            Map.entry("minecraft:golden_apple", new NpcTradePrice(COPPER_COIN, 32)),
            Map.entry("minecraft:netherite_ingot", new NpcTradePrice(COPPER_COIN, 48)),
            Map.entry("minecraft:nether_star", new NpcTradePrice(COPPER_COIN, 64)),
            Map.entry("minecraft:enchanted_golden_apple", new NpcTradePrice(COPPER_COIN, 128)),

            Map.entry(ImmortalCultivationMod.MODID + ":qingling_grass", new NpcTradePrice(LOW_SPIRIT_STONE, 1)),
            Map.entry(ImmortalCultivationMod.MODID + ":chiyan_flower", new NpcTradePrice(LOW_SPIRIT_STONE, 2)),
            Map.entry(ImmortalCultivationMod.MODID + ":ninglu_zhi", new NpcTradePrice(LOW_SPIRIT_STONE, 3)),
            Map.entry(ImmortalCultivationMod.MODID + ":jinwen_fruit", new NpcTradePrice(LOW_SPIRIT_STONE, 4)),
            Map.entry(ImmortalCultivationMod.MODID + ":xuanbing_lotus", new NpcTradePrice(LOW_SPIRIT_STONE, 6)),

            Map.entry(ImmortalCultivationMod.MODID + ":jingtie_motherstone", new NpcTradePrice(MEDIUM_SPIRIT_STONE, 3)),
            Map.entry(ImmortalCultivationMod.MODID + ":yunwen_silver_crystal", new NpcTradePrice(MEDIUM_SPIRIT_STONE, 4)),
            Map.entry(ImmortalCultivationMod.MODID + ":lingjiao_scale", new NpcTradePrice(MEDIUM_SPIRIT_STONE, 5)),
            Map.entry(ImmortalCultivationMod.MODID + ":bigu_pill", new NpcTradePrice(LOW_SPIRIT_STONE, 4)),
            Map.entry(ImmortalCultivationMod.MODID + ":middle_qi_recovery_pill", new NpcTradePrice(MEDIUM_SPIRIT_STONE, 6)),
            Map.entry(ImmortalCultivationMod.MODID + ":high_qi_recovery_pill", new NpcTradePrice(MEDIUM_SPIRIT_STONE, 16)),
            Map.entry(ImmortalCultivationMod.MODID + ":breakthrough_pill", new NpcTradePrice(MEDIUM_SPIRIT_STONE, 3)),
            Map.entry(ImmortalCultivationMod.MODID + ":blood_qi_pill", new NpcTradePrice(MEDIUM_SPIRIT_STONE, 3)),
            Map.entry(ImmortalCultivationMod.MODID + ":blood_crystal", new NpcTradePrice(LOW_SPIRIT_STONE, 3)),

            Map.entry(ImmortalCultivationMod.MODID + ":middle_yangjian_stone", new NpcTradePrice(MEDIUM_SPIRIT_STONE, 4)),
            Map.entry(ImmortalCultivationMod.MODID + ":high_yangjian_stone", new NpcTradePrice(HIGH_SPIRIT_STONE, 5)),
            Map.entry(ImmortalCultivationMod.MODID + ":middle_sword_grindstone", new NpcTradePrice(MEDIUM_SPIRIT_STONE, 4)),
            Map.entry(ImmortalCultivationMod.MODID + ":high_sword_grindstone", new NpcTradePrice(HIGH_SPIRIT_STONE, 5)),
            Map.entry(ImmortalCultivationMod.MODID + ":middle_saber_grindstone", new NpcTradePrice(MEDIUM_SPIRIT_STONE, 4)),
            Map.entry(ImmortalCultivationMod.MODID + ":high_saber_grindstone", new NpcTradePrice(HIGH_SPIRIT_STONE, 5)),

            Map.entry(ImmortalCultivationMod.MODID + ":zhenshi_talisman", new NpcTradePrice(LOW_SPIRIT_STONE, 32)),
            Map.entry(ImmortalCultivationMod.MODID + ":qi_pouch", new NpcTradePrice(LOW_SPIRIT_STONE, 64)),
            Map.entry(ImmortalCultivationMod.MODID + ":lethal_poison", new NpcTradePrice(LOW_SPIRIT_STONE, 2)),
            Map.entry(ImmortalCultivationMod.MODID + ":wanted_order", new NpcTradePrice(LOW_SPIRIT_STONE, 1)),
            Map.entry(ImmortalCultivationMod.MODID + ":shihunfan", new NpcTradePrice(HIGH_SPIRIT_STONE, 3)),

            Map.entry(ImmortalCultivationMod.MODID + ":fireball_scroll", new NpcTradePrice(LOW_SPIRIT_STONE, 6)),
            Map.entry(ImmortalCultivationMod.MODID + ":kongshi_shu_scroll", new NpcTradePrice(LOW_SPIRIT_STONE, 10)),

            Map.entry(ImmortalCultivationMod.MODID + ":basic_breathing_method", new NpcTradePrice(LOW_SPIRIT_STONE, 10)),
            Map.entry(ImmortalCultivationMod.MODID + ":clear_heart_method", new NpcTradePrice(LOW_SPIRIT_STONE, 10)),
            Map.entry(ImmortalCultivationMod.MODID + ":blood_demon_jindan_method", new NpcTradePrice(MEDIUM_SPIRIT_STONE, 32)),
            Map.entry(ImmortalCultivationMod.MODID + ":pokong_jue_method", new NpcTradePrice(MEDIUM_SPIRIT_STONE, 64)),
            Map.entry(ImmortalCultivationMod.MODID + ":cuigu_lianshen_method", new NpcTradePrice(HIGH_SPIRIT_STONE, 24)),

            Map.entry(ImmortalCultivationMod.MODID + ":feijian_yujian", new NpcTradePrice(MEDIUM_SPIRIT_STONE, 80)),
            Map.entry(ImmortalCultivationMod.MODID + ":dao_yujian", new NpcTradePrice(MEDIUM_SPIRIT_STONE, 80)),
            Map.entry(ImmortalCultivationMod.MODID + ":jingtie_spear_yujian", new NpcTradePrice(MEDIUM_SPIRIT_STONE, 80)),
            Map.entry(ImmortalCultivationMod.MODID + ":sword_mold", new NpcTradePrice(MEDIUM_SPIRIT_STONE, 60)),
            Map.entry(ImmortalCultivationMod.MODID + ":saber_mold", new NpcTradePrice(MEDIUM_SPIRIT_STONE, 60)),
            Map.entry(ImmortalCultivationMod.MODID + ":spear_mold", new NpcTradePrice(MEDIUM_SPIRIT_STONE, 60)),
            Map.entry(ImmortalCultivationMod.MODID + ":flying_sword", new NpcTradePrice(MEDIUM_SPIRIT_STONE, 12)),
            Map.entry(ImmortalCultivationMod.MODID + ":dao", new NpcTradePrice(MEDIUM_SPIRIT_STONE, 12)),
            Map.entry(ImmortalCultivationMod.MODID + ":jingtie_spear", new NpcTradePrice(MEDIUM_SPIRIT_STONE, 16)),
            Map.entry(ImmortalCultivationMod.MODID + ":taomu_sword", new NpcTradePrice(LOW_SPIRIT_STONE, 8)),
            Map.entry(ImmortalCultivationMod.MODID + ":copper_coin_sword", new NpcTradePrice(LOW_SPIRIT_STONE, 10))
    );
    private static volatile List<SystemPriceBenchmark> systemPriceBenchmarks;

    private static final List<NpcSkinCatalog.Skin> FANREN_SKINS =
            NpcSkinCatalog.MORTAL_SKINS;
    private static final List<NpcSkinCatalog.Skin> CULTIVATOR_SKINS =
            NpcSkinCatalog.CULTIVATOR_SKINS;

    private static final String[] SURNAMES = {
            "林", "叶", "沈", "韩", "萧", "莫", "秦", "苏", "江", "陆", "顾", "楚", "白", "宁", "温", "谢"
    };
    private static final String[] MALE_GIVEN_NAMES = {
            "清玄", "云舟", "长风", "子衿", "景行", "玄真", "星河", "凌霄",
            "承岳", "墨尘", "修远", "怀川"
    };
    private static final String[] FEMALE_GIVEN_NAMES = {
            "若兰", "明月", "青禾", "雪棠", "怀瑾", "知微", "雨眠", "昭宁",
            "清漪", "云舒", "灵溪", "含烟"
    };
    private static final String[] PERSONALITIES = {
            "cautious", "proud", "gentle", "wandering", "aggressive", "scholarly"
    };

    private NpcProfile() {
    }

    public static String normalizePlayerGender(String gender) {
        return GENDER_FEMALE.equals(gender) ? GENDER_FEMALE : GENDER_MALE;
    }

    public static String playerGender(Player player) {
        return normalizePlayerGender(player.getPersistentData().getString(PLAYER_GENDER_TAG));
    }

    public static void configureFixedCultivator(PathfinderMob mob, String archetype, String fixedName,
                                                 String fixedSkin, String fixedGender, String fixedRealm) {
        ensure(mob, archetype);
        CompoundTag profile = profile(mob);
        String previousRealm = profile.getString("Realm");
        boolean realmChanged = !fixedRealm.equals(previousRealm);

        profile.putString("Name", fixedName);
        profile.putString("Skin", fixedSkin);
        profile.putString("Gender", normalizeGender(fixedGender));
        profile.putString("Realm", fixedRealm);

        if (realmChanged) {
            migrateNpcLoadout(profile, archetype, mob.getUUID().hashCode());
        }

        int maxQi = CultivationLevels.getLevelDef(fixedRealm).maxQi();
        int currentQi = profile.getInt("Qi");
        profile.putInt("MaxQi", maxQi);
        profile.putInt("Qi", realmChanged ? maxQi : Mth.clamp(currentQi, 0, maxQi));
        syncMethodSpecialSpells(profile);
        refreshWantedOrder(mob, profile);

        if (mob instanceof CultivationNpc npc) {
            npc.setNpcSkinId(fixedSkin);
        }
        applyRealmStats(mob, fixedRealm, realmChanged);
        mob.setCustomName(Component.literal(fixedName));
        mob.setCustomNameVisible(false);
    }

    public static void ensureHotelkeeper(HotelNpcEntity mob) {
        ensure(mob, FANREN);
        CompoundTag profile = profile(mob);
        profile.putString("Skin", HOTELKEEPER_SKIN);
        profile.putString("Gender", GENDER_MALE);
        profile.putString("Identity", "hotelkeeper");

        if (profile.getInt("HotelkeeperKitVersion") < HOTELKEEPER_KIT_VERSION) {
            writeInventory(mob, profile,
                    hotelkeeperInventory(mob.getUUID().hashCode()));
            profile.putInt("HotelkeeperKitVersion", HOTELKEEPER_KIT_VERSION);
        }

        mob.setNpcSkinId(HOTELKEEPER_SKIN);
    }

    public static void ensure(PathfinderMob mob, String archetype) {
        if (!(mob instanceof CultivationNpc)) {
            clearAccidentalVanillaProfile(mob);
            return;
        }
        CompoundTag profile = profile(mob);
        boolean initialized = profile.getBoolean("Initialized");
        int previousVersion = profile.getInt("ProfileVersion");
        int seed = mob.getUUID().hashCode();

        if (!initialized) {
            NpcSkinCatalog.Skin skin = selectSkin(archetype, seed);
            String realm = defaultRealm(archetype, seed);
            String identity = selectIdentity(archetype, seed);
            List<String> roots = defaultRoots(archetype);
            String method = selectRandomMethod(archetype, realm, roots, seed);
            List<String> spells = selectRandomSpells(archetype, realm, roots, identity, method, seed);
            int maxQi = CultivationLevels.getLevelDef(realm).maxQi();
            profile.putBoolean("Initialized", true);
            profile.putString("Skin", skin.texture());
            profile.putString("Gender", skin.gender());
            profile.putString("Name", chineseName(seed, 0, skin.gender()));
            profile.putString("Personality", PERSONALITIES[Math.floorMod(seed / 13, PERSONALITIES.length)]);
            profile.putString("Identity", identity);
            profile.putString("Realm", realm);
            profile.putString("Method", method);
            profile.put("Methods", stringList(method.isBlank() ? List.of() : List.of(method)));
            profile.putString("BodyType", defaultBodyType(archetype));
            profile.putInt("Age", defaultAge(archetype) + Math.floorMod(seed, 30));
            profile.putInt("Reputation", defaultReputation(archetype));
            profile.putInt("Qi", maxQi);
            profile.putInt("MaxQi", maxQi);
            profile.putInt("Thoughts", ModAttachments.MAX_THOUGHTS);
            profile.putLong("CultivationProgress", defaultProgress(archetype));
            profile.put("SpiritRoots", stringList(roots));
            profile.put("Spells", stringList(spells));
            profile.put("KnownPlayers", new CompoundTag());
            profile.put("Relationships", new CompoundTag());
            profile.put("CompletedQuests", new ListTag());
            profile.put("Family", stringList(defaultFamily(seed)));
            writeInventory(mob, profile, defaultInventory(archetype, identity, seed));
            refreshWantedOrder(mob, profile);
        } else if (profile.getInt("ProfileVersion") < PROFILE_VERSION) {
            migrateProfile(mob, profile, archetype, seed);
        }

        if (!profile.contains("Thoughts")) {
            profile.putInt("Thoughts", ModAttachments.MAX_THOUGHTS);
        }
        profile.putInt("Thoughts", Mth.clamp(profile.getInt("Thoughts"), 0, ModAttachments.MAX_THOUGHTS));

        String normalizedIdentity = normalizeIdentity(profile.getString("Identity"));
        if (FANREN.equals(archetype)) {
            normalizedIdentity = mob instanceof HotelNpcEntity
                    ? "hotelkeeper"
                    : "village_mortal";
        } else if (!CULTIVATOR_IDENTITIES.contains(normalizedIdentity)) {
            normalizedIdentity = selectIdentity(archetype, seed);
        }
        profile.putString("Identity", normalizedIdentity);

        if (!mob.level().isClientSide) {
            ensureFirstQuestRewardStock(mob, profile);
            NpcPillStock.ensure(mob, profile, archetype);
        }
        syncMethodSpecialSpells(profile);
        refreshWantedOrder(mob, profile);
        profile.putInt("ProfileVersion", PROFILE_VERSION);
        if (!mob.level().isClientSide && mob instanceof CultivationNpc npc) {
            npc.setNpcSkinId(profile.getString("Skin"));
            applyRealmStats(mob, profile.getString("Realm"), !initialized || previousVersion < PROFILE_VERSION);
        }
        if (!mob.level().isClientSide && !mob.hasCustomName()) {
            mob.setCustomName(Component.literal(profile.getString("Name")));
            mob.setCustomNameVisible(false);
        }
    }

    /**
     * Removes profiles accidentally created on vanilla mobs by cultivation
     * method checks. A player-assigned custom name is preserved.
     */
    public static void clearAccidentalVanillaProfile(PathfinderMob mob) {
        if (mob instanceof CultivationNpc) {
            return;
        }
        CompoundTag persistentData = mob.getPersistentData();
        if (!persistentData.contains(TAG)) {
            return;
        }
        CompoundTag accidentalProfile = persistentData.getCompound(TAG);
        String generatedName = accidentalProfile.getString("Name");
        if (!generatedName.isBlank() && mob.hasCustomName()
                && generatedName.equals(mob.getCustomName().getString())) {
            mob.setCustomName(null);
            mob.setCustomNameVisible(false);
        }
        persistentData.remove(TAG);
    }

    private static void migrateProfile(PathfinderMob mob, CompoundTag profile, String archetype, int seed) {
        int previousVersion = profile.getInt("ProfileVersion");
        if (!profile.contains("Skin") || !profile.contains("Gender") || previousVersion < 5) {
            NpcSkinCatalog.Skin skin = selectSkin(archetype, seed);
            profile.putString("Skin", skin.texture());
            profile.putString("Gender", skin.gender());
        }
        profile.putString("Gender", normalizeGender(profile.getString("Gender")));
        if (previousVersion < 5 && !FANREN.equals(archetype)) {
            profile.putString("Realm", defaultRealm(archetype, seed));
        }
        String realm = profile.getString("Realm");
        int maxQi = CultivationLevels.getLevelDef(realm).maxQi();
        profile.putInt("MaxQi", maxQi);
        profile.putInt("Qi", Mth.clamp(profile.getInt("Qi"), 0, maxQi));
        String oldName = profile.getString("Name");
        boolean generatedName = !mob.hasCustomName() || mob.getName().getString().equals(oldName);
        if (generatedName || oldName.isBlank() || oldName.matches("[A-Za-z ]+")) {
            String newName = chineseName(seed, 0, profile.getString("Gender"));
            profile.putString("Name", newName);
            if (!mob.level().isClientSide) {
                mob.setCustomName(Component.literal(newName));
                mob.setCustomNameVisible(false);
            }
        }
        profile.putString("Personality", normalizePersonality(profile.getString("Personality")));
        String existingIdentity = normalizeIdentity(profile.getString("Identity"));
        profile.putString("Identity", CULTIVATOR_IDENTITIES.contains(existingIdentity)
                ? existingIdentity
                : selectIdentity(archetype, seed));
        if (!profile.contains("BodyType")) {
            profile.putString("BodyType", defaultBodyType(archetype));
        }
        if (!profile.contains("Family") || previousVersion < 5) {
            profile.put("Family", stringList(defaultFamily(seed)));
        }
        if (!profile.contains("Inventory")) {
            writeInventory(mob, profile, defaultInventory(archetype, profile.getString("Identity"), seed));
        } else if (previousVersion < 3) {
            mergeMissingDefaultInventory(mob, profile, archetype);
        }
        if (previousVersion < 16
                && ("talisman_cultivator".equals(profile.getString("Identity"))
                || "formation_cultivator".equals(profile.getString("Identity")))) {
            mergeMissingDefaultInventory(mob, profile, archetype);
        }
        if (previousVersion < 18
                && "corpse_cultivator".equals(profile.getString("Identity"))) {
            mergeMissingCorpseCultivatorItems(mob, profile, seed);
        }
        if (previousVersion < 19) {
            mergeMissingSpecialistForgingSlip(mob, profile, seed);
        }
        if (previousVersion < 15) {
            migrateNpcLoadout(profile, archetype, seed);
        } else {
            ensureKnownMethods(profile);
            chooseBestKnownMethod(profile);
        }
        if (previousVersion < 8) {
            CompoundTag relationships = profile.getCompound("Relationships");
            for (String key : relationships.getAllKeys()) {
                CompoundTag relation = relationships.getCompound(key);
                if (relation.getInt("Hatred") > 0) {
                    relation.putInt("Hatred", -relation.getInt("Hatred"));
                    relationships.put(key, relation);
                }
            }
            profile.put("Relationships", relationships);
        }
        if (previousVersion < 14) {
            CompoundTag relationships = profile.getCompound("Relationships");
            for (String key : relationships.getAllKeys()) {
                CompoundTag relation = relationships.getCompound(key);
                relation.putInt("Hatred", -relation.getInt("Hatred"));
                relationships.put(key, relation);
            }
            profile.put("Relationships", relationships);
        }
        if (previousVersion < 9 && FANREN.equals(archetype)) {
            List<ItemStack> inventory = inventory(mob);
            int convertedCoins = 0;
            for (int i = 0; i < inventory.size(); i++) {
                ItemStack stack = inventory.get(i);
                if (stack.is(item(LOW_SPIRIT_STONE))) {
                    convertedCoins += stack.getCount() * 4;
                    inventory.set(i, ItemStack.EMPTY);
                }
            }
            int currentCoins = countInventoryItem(inventory, item(COPPER_COIN));
            if (currentCoins < 24) {
                insertInventory(inventory, new ItemStack(item(COPPER_COIN), 24 - currentCoins + convertedCoins));
            } else if (convertedCoins > 0) {
                insertInventory(inventory, new ItemStack(item(COPPER_COIN), convertedCoins));
            }
            writeInventory(mob, profile, inventory);
        }
        if (previousVersion < 11) {
            mergeMissingDefaultInventory(mob, profile, archetype);
        }
    }

    public static void tickCultivation(PathfinderMob mob, String archetype) {
        ensure(mob, archetype);
        if (!mob.level().isClientSide) {
            tickControlState(mob);
            NpcCultivation.tick(mob);
        }
        if (mob.level().isClientSide || mob.tickCount % 20 != 0) {
            return;
        }

        CompoundTag profile = profile(mob);
        tickNpcAge(mob, profile);
        if (!mob.isAlive()) {
            return;
        }
        tickThoughts(mob, profile);
        chooseBestKnownMethod(profile);
        int maxQi = profile.getInt("MaxQi");
        int qiRegen = switch (archetype) {
            case JINDAN -> 18;
            case ZHUJI -> 8;
            case LIANQI -> 3;
            default -> 1;
        };
        if (mob.hasEffect(ModEffects.QI_GATHERING)) {
            qiRegen = Math.max(1, maxQi * 5 / 100);
        }
        qiRegen = com.example.immortal_cultivation_mod.magicritual.center.CenterEffectSupport
                .adjustNaturalQiRegen(mob, qiRegen);
        if (mob.hasEffect(ModEffects.SPIRIT_SEAL)) {
            qiRegen = Math.max(0, Mth.floor(qiRegen * 0.30D));
        }
        qiRegen = UndeadAbilitySystem.adjustQiRegen(mob, qiRegen);
        qiRegen = CultivationBestiaryBeastEntity.adjustQiRegeneration(mob, qiRegen);
        profile.putInt("Qi", (int) Math.min(maxQi, (long) profile.getInt("Qi") + qiRegen));
    }

    /** Internal training state shares the NPC profile's existing save/load lifecycle. */
    static CompoundTag cultivationProfile(PathfinderMob mob) { return profile(mob); }

    static double autonomousProgressPerSecond(CompoundTag profile) {
        String realm = profile.getString("Realm");
        String methodId = profile.getString("Method");
        if (!CultivationMethods.canGainProgress(methodId, realm)) return 0;
        double seconds = 1200.0 + 600.0 * Math.max(0, CultivationLevels.getRealmIndex(realm) - 1);
        int multiplier = methodMultiplierPercent(CultivationMethods.get(methodId),
                strings(profile.getList("SpiritRoots", 8)));
        return CultivationLevels.getTotalQiNeeded(realm) / seconds * multiplier / 100.0;
    }

    static boolean completeAutonomousBreakthrough(PathfinderMob mob, String expectedRealm) {
        CompoundTag profile = profile(mob);
        String realm = profile.getString("Realm");
        String next = CultivationLevels.getNextStage(realm);
        long required = CultivationLevels.getTotalQiNeeded(realm);
        if (!realm.equals(expectedRealm) || next == null
                || !CultivationMethods.canGainProgress(profile.getString("Method"), realm)
                || profile.getLong("CultivationProgress") < required) return false;
        var previous = CultivationLevels.getLevelDef(realm);
        var promoted = CultivationLevels.getLevelDef(next);
        profile.putString("Realm", next);
        profile.putLong("CultivationProgress", Math.min(CultivationLevels.getTotalQiNeeded(next),
                profile.getLong("CultivationProgress") - required));
        profile.putInt("MaxQi", promoted.maxQi());
        profile.putInt("Qi", promoted.maxQi());
        profile.putInt("Age", (int) Math.min(Integer.MAX_VALUE, Math.max(0L, profile.getInt("Age"))
                + Math.max(0, promoted.maxAge() - previous.maxAge())));
        chooseBestKnownMethod(profile);
        LinkedHashSet<String> spells = new LinkedHashSet<>(strings(profile.getList("Spells", 8)));
        spells.addAll(selectRandomSpells(archetypeOf(mob), next,
                strings(profile.getList("SpiritRoots", 8)), profile.getString("Identity"),
                profile.getString("Method"), mob.getUUID().hashCode()));
        profile.put("Spells", stringList(List.copyOf(spells)));
        applyRealmStats(mob, next, true);
        var attack = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) attack.setBaseValue(Math.max(attack.getBaseValue(),
                2.0 + CultivationLevels.getStageIndex(next) * 2.0));
        refreshWantedOrder(mob, profile);
        return true;
    }

    private static void tickThoughts(PathfinderMob mob, CompoundTag profile) {
        int current = Mth.clamp(profile.getInt("Thoughts"), 0, ModAttachments.MAX_THOUGHTS);
        boolean wasEmpty = current <= 0;
        if (mob.tickCount % THOUGHT_REGEN_INTERVAL_TICKS == 0 && current < ModAttachments.MAX_THOUGHTS) {
            int regen = Math.max(1, ModAttachments.MAX_THOUGHTS * 5 / 100);
            current = Math.min(ModAttachments.MAX_THOUGHTS, current + regen);
        }
        profile.putInt("Thoughts", current);
        if (wasEmpty) {
            mob.addEffect(new MobEffectInstance(ModEffects.DAZE, 40, 0, false, true, true));
        }
    }

    /** Performs the NPC's one-shot identity combat ability when chosen by its combat brain. */
    public static boolean tryUseIdentityCombatAbility(PathfinderMob mob) {
        CompoundTag profile = profile(mob);
        if (!"corpse_cultivator".equals(normalizeIdentity(profile.getString("Identity")))
                || profile.getBoolean(CORPSE_SUMMON_USED)
                || !(mob.getTarget() instanceof LivingEntity target)
                || !target.isAlive()
                || isSparPair(mob, target)
                || !(mob.level() instanceof ServerLevel serverLevel)) {
            return false;
        }

        Zombie zombie = EntityType.ZOMBIE.create(serverLevel);
        if (zombie == null) {
            return false;
        }

        Vec3 towardTarget = target.position().subtract(mob.position());
        if (towardTarget.horizontalDistanceSqr() < 0.01D) {
            towardTarget = mob.getLookAngle();
        }
        Vec3 side = new Vec3(-towardTarget.z, 0.0D, towardTarget.x).normalize()
                .scale(mob.getRandom().nextBoolean() ? 1.5D : -1.5D);
        zombie.moveTo(mob.getX() + side.x, mob.getY() + 0.1D, mob.getZ() + side.z,
                mob.getYRot(), 0.0F);
        UndeadControl.initializeNpcSummon(zombie, mob, profile.getString("Realm"), target);
        if (!serverLevel.addFreshEntity(zombie)) {
            return false;
        }

        profile.putBoolean(CORPSE_SUMMON_USED, true);
        profile.putUUID(CORPSE_SUMMON_UUID, zombie.getUUID());
        serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                zombie.getX(), zombie.getY() + 1.0D, zombie.getZ(),
                36, 0.45D, 0.8D, 0.45D, 0.035D);
        serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                zombie.getX(), zombie.getY() + 0.8D, zombie.getZ(),
                18, 0.35D, 0.55D, 0.35D, 0.02D);
        return true;
    }

    public static void releaseCorpseSummon(PathfinderMob mob) {
        if (mob.level() instanceof ServerLevel serverLevel) {
            UndeadControl.releaseNpcSummons(serverLevel, mob.getUUID());
        }
    }

    public static boolean tryUseRoyalPendant(PathfinderMob mob) {
        ensure(mob, archetypeOf(mob));
        CompoundTag profile = profile(mob);
        if (!"royal".equals(normalizeIdentity(profile.getString("Identity")))
                || profile.getBoolean(ROYAL_PENDANT_USED)) {
            return false;
        }

        List<ItemStack> inventory = inventory(mob);
        if (countInventoryItem(inventory, ModItems.PROTECTIVE_JADE_PENDANT.get()) <= 0) {
            return false;
        }

        removeInventoryItems(inventory, ModItems.PROTECTIVE_JADE_PENDANT.get(), 1);
        writeInventory(mob, profile, inventory);
        profile.putBoolean(ROYAL_PENDANT_USED, true);
        mob.setHealth(1.0F);
        mob.removeAllEffects();
        mob.addEffect(new MobEffectInstance(MobEffects.HEAL,
                20 * 10, 4, false, true, true));
        if (mob.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                    mob.getX(), mob.getY() + 1.0D, mob.getZ(),
                    40, 0.55D, 0.8D, 0.55D, 0.08D);
        }
        return true;
    }

    private static void tickNpcAge(PathfinderMob mob, CompoundTag profile) {
        if (!(mob.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        long currentDay = serverLevel.getServer().overworld().getDayTime() / TICKS_PER_INGAME_DAY;
        if (!profile.contains("LastAgeDay")) {
            profile.putLong("LastAgeDay", currentDay);
            return;
        }
        long lastDay = profile.getLong("LastAgeDay");
        if (currentDay <= lastDay) {
            return;
        }
        long passedDays = currentDay - lastDay;
        int ageLoss = (int) Math.min(Integer.MAX_VALUE, passedDays);
        int remainingAge = Math.max(0, profile.getInt("Age") - ageLoss);
        profile.putInt("Age", remainingAge);
        profile.putLong("LastAgeDay", currentDay);
        if (remainingAge <= 0) {
            mob.hurt(mob.damageSources().genericKill(), Float.MAX_VALUE);
        }
    }

    private static void tickControlState(PathfinderMob mob) {
        CompoundTag profile = profile(mob);
        if (profile.hasUUID("SparPlayer")) {
            Player player = mob.level().getPlayerByUUID(profile.getUUID("SparPlayer"));
            if (player == null || !player.isAlive() || player.level() != mob.level()
                    || mob.getHealth() <= 1.0F || player.getHealth() <= 1.0F) {
                endSpar(mob);
                return;
            }
            long now = mob.level().getGameTime();
            long countdownEnd = profile.getLong("SparCountdownEnd");
            if (countdownEnd > now) {
                mob.setTarget(null);
                mob.getNavigation().stop();
                Vec3 movement = mob.getDeltaMovement();
                mob.setDeltaMovement(0.0D, movement.y, 0.0D);
                int seconds = Math.max(1, (int) Math.ceil((countdownEnd - now) / 20.0D));
                if (seconds < profile.getInt("SparCountdownSecond")) {
                    profile.putInt("SparCountdownSecond", seconds);
                    player.displayClientMessage(Component.translatable(
                            "message." + ImmortalCultivationMod.MODID + ".spar_countdown", seconds), true);
                }
                return;
            }
            if (profile.contains("SparCountdownEnd")) {
                profile.remove("SparCountdownEnd");
                profile.remove("SparCountdownSecond");
                player.displayClientMessage(Component.translatable(
                        "message." + ImmortalCultivationMod.MODID + ".spar_go"), true);
            }
            mob.setNoAi(false);
            mob.setTarget(player);
            // Combat movement and attacks are owned by NpcCombatBrain so only one
            // main action can be issued per decision tick.
            return;
        }
        if (mob.level() instanceof ServerLevel serverLevel) {
            if (profile.hasUUID("RetaliationPlayer")) {
                Player retaliationTarget = serverLevel.getPlayerByUUID(profile.getUUID("RetaliationPlayer"));
                if (retaliationTarget instanceof ServerPlayer retaliationPlayer && retaliationPlayer.isAlive()
                        && shouldAttackPlayer(mob, retaliationPlayer)) {
                    mob.setTarget(retaliationPlayer);
                } else {
                    profile.remove("RetaliationPlayer");
                }
            }
            if (mob.tickCount % 10 == 0) {
                ServerPlayer hatedPlayer = null;
                double closest = 32.0D * 32.0D;
                boolean cultivator = !FANREN.equals(archetypeOf(mob));
                for (ServerPlayer candidate : serverLevel.players()) {
                    double distance = mob.distanceToSqr(candidate);
                    boolean exposedLivingCorpse = cultivator
                            && CultivationAccessoryItem.isLivingCorpse(candidate)
                            && !DisguiseMaskItem.isWearing(candidate)
                            && mob.getSensing().hasLineOfSight(candidate);
                    if (candidate.isAlive() && distance <= closest
                            && (exposedLivingCorpse
                            || relationship(mob, candidate).getInt("Hatred") >= 100)) {
                        hatedPlayer = candidate;
                        closest = distance;
                    }
                }
                if (hatedPlayer != null) {
                    mob.setTarget(hatedPlayer);
                }
            }
            if (mob.getTarget() instanceof ServerPlayer targetPlayer) {
                if (shouldAttackPlayer(mob, targetPlayer)) {
                    // Target ownership remains here; NpcCombatBrain owns the action.
                    return;
                }
                mob.setTarget(null);
            }
        }
        if (mob.getTarget() instanceof LivingEntity combatTarget && combatTarget.isAlive()) {
            // Following/stay navigation must not overwrite the one action selected by NpcCombatBrain.
            return;
        }
        if (profile.hasUUID("InteractingPlayer")) {
            Player player = mob.level().getPlayerByUUID(profile.getUUID("InteractingPlayer"));
            if (player == null || !player.isAlive() || player.level() != mob.level()
                    || mob.level().getGameTime() > profile.getLong("InteractionTimeout")) {
                releaseInteraction(mob);
            } else {
                mob.getNavigation().stop();
                Vec3 movement = mob.getDeltaMovement();
                mob.setDeltaMovement(0.0D, movement.y, 0.0D);
                mob.lookAt(player, 25.0F, 25.0F);
                return;
            }
        }

        if (profile.getBoolean("StayHere")) {
            double stayX = profile.getDouble("StayX");
            double stayY = profile.getDouble("StayY");
            double stayZ = profile.getDouble("StayZ");
            double distance = mob.distanceToSqr(stayX, stayY, stayZ);
            if (distance > 2.25D) {
                mob.getNavigation().moveTo(stayX, stayY, stayZ, 0.9D);
            } else {
                mob.getNavigation().stop();
                Vec3 movement = mob.getDeltaMovement();
                mob.setDeltaMovement(0.0D, movement.y, 0.0D);
            }
            return;
        }

        if (profile.hasUUID("FollowingPlayer") && mob.level() instanceof ServerLevel serverLevel) {
            Player owner = serverLevel.getPlayerByUUID(profile.getUUID("FollowingPlayer"));
            if (owner == null || !owner.isAlive()) {
                return;
            }
            double distance = mob.distanceToSqr(owner);
            if (distance > 32.0D * 32.0D) {
                mob.teleportTo(owner.getX() + 1.0D, owner.getY(), owner.getZ() + 1.0D);
                mob.getNavigation().stop();
            } else if (distance > 3.5D * 3.5D) {
                mob.getNavigation().moveTo(owner, 1.08D * chaseSpeedMultiplier(mob));
            } else {
                mob.getNavigation().stop();
            }
        }
    }

    public static void openInteraction(PathfinderMob mob, ServerPlayer player) {
        ensure(mob, archetypeOf(mob));
        CompoundTag relation = relationship(mob, player);
        long now = mob.level().getGameTime();
        if (relation.getInt("Hatred") >= 100
                || relation.getBoolean("PermanentEnemy") || relation.getLong("RefuseUntil") > now) {
            if (relation.getInt("Hatred") >= 100 || relation.getBoolean("PermanentEnemy")) {
                mob.setTarget(player);
            }
            player.sendSystemMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID + ".npc_refuses_interaction"));
            return;
        }
        if (!beginInteraction(mob, player)) {
            player.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".npc.busy"));
            return;
        }
        rememberPlayer(mob, player);
        sendProfile(mob, player, true, "");
    }

    private static boolean beginInteraction(PathfinderMob mob, ServerPlayer player) {
        CompoundTag profile = profile(mob);
        if (profile.hasUUID("InteractingPlayer") && !profile.getUUID("InteractingPlayer").equals(player.getUUID())
                && mob.level().getGameTime() <= profile.getLong("InteractionTimeout")) {
            return false;
        }
        profile.putUUID("InteractingPlayer", player.getUUID());
        profile.putLong("InteractionTimeout", mob.level().getGameTime() + INTERACTION_TIMEOUT_TICKS);
        if (!profile.getBoolean("InteractionForcedNoAi")) {
            profile.putBoolean("InteractionPreviousNoAi", mob.isNoAi());
            profile.putBoolean("InteractionForcedNoAi", true);
        }
        mob.getNavigation().stop();
        mob.setTarget(null);
        mob.setNoAi(true);
        Vec3 movement = mob.getDeltaMovement();
        mob.setDeltaMovement(0.0D, movement.y, 0.0D);
        return true;
    }

    public static boolean isInteracting(PathfinderMob mob) {
        return profile(mob).hasUUID("InteractingPlayer");
    }

    public static void handleAction(PathfinderMob mob, ServerPlayer player, String action, int index) {
        ensure(mob, archetypeOf(mob));
        if (action.startsWith("DEBUG_RELATION_")) {
            adjustDebugRelationship(mob, player, action.substring("DEBUG_RELATION_".length()), index);
            return;
        }
        if (!isInteractionOwner(mob, player)) {
            return;
        }
        if (mob.distanceToSqr(player) > 16.0D * 16.0D) {
            releaseInteraction(mob);
            return;
        }

        CompoundTag profile = profile(mob);
        profile.putLong("InteractionTimeout", mob.level().getGameTime() + INTERACTION_TIMEOUT_TICKS);
        if ("HEARTBEAT".equals(action)) {
            return;
        }
        if ("END".equals(action)) {
            releaseInteraction(mob);
            return;
        }

        String status = switch (action) {
            case "TALK_GREET" -> talk(mob, player, "greet");
            case "TALK_REALM" -> talk(mob, player, "realm");
            case "TALK_WORLD" -> talk(mob, player, "world");
            case "INTERACT_GIFT" -> giveGift(mob, player);
            case "INTERACT_PRAISE" -> performSocialAction(mob, player, SocialAction.PRAISE);
            case "INTERACT_JOKE" -> performSocialAction(mob, player, SocialAction.JOKE);
            case "INTERACT_INSULT" -> performSocialAction(mob, player, SocialAction.INSULT);
            case "INTERACT_THREATEN" -> performSocialAction(mob, player, SocialAction.THREATEN);
            case "INTERACT_SPAR" -> requestSpar(mob, player);
            case "INTERACT_INVITE_SECT" -> inviteToSect(mob, player);
            case "INTERACT_MASTER" -> formMasterDiscipleBond(mob, player, true);
            case "INTERACT_DISCIPLE" -> formMasterDiscipleBond(mob, player, false);
            case "INTERACT_DAO_COMPANION" -> formDaoCompanionBond(mob, player);
            case "TRADE_EXCHANGE" -> exchangeTrade(mob, player, index);
            case "QUEST_ACCEPT" -> acceptQuest(mob, player);
            case "QUEST_COMPLETE" -> completeQuest(mob, player);
            case "TOGGLE_FOLLOW" -> toggleFollow(mob, player);
            case "STAY" -> stayHere(mob, player);
            case "MOVE_FREELY" -> moveFreely(mob, player);
            case "SET_HOME" -> setHome(mob, player);
            case "GO_HOME" -> goHome(mob, player);
            case "LEARN_SPELL" -> learnSpell(mob, player, index);
            case "LEARN_METHOD" -> learnMethod(mob, player);
            case "TEACH_NPC_SPELL" -> teachNpcSpell(mob, player, index);
            case "TEACH_NPC_METHOD" -> teachNpcMethod(mob, player, index);
            case "INVENTORY_DEPOSIT" -> depositHeldItem(mob, player);
            case "INVENTORY_WITHDRAW" -> withdrawInventoryItem(mob, player, index);
            default -> "";
        };
        if (action.startsWith("TALK_") && !status.isBlank()) {
            com.example.immortal_cultivation_mod.guidance.DaoGuidanceSystem.record(
                    player,
                    com.example.immortal_cultivation_mod.guidance.DaoGuidanceContent.TALK_NPC);
        } else if ("TRADE_EXCHANGE".equals(action)
                && "npc.status.trade_complete".equals(status)) {
            com.example.immortal_cultivation_mod.guidance.DaoGuidanceSystem.record(
                    player,
                    com.example.immortal_cultivation_mod.guidance.DaoGuidanceContent.TRADE_NPC);
        } else if ("QUEST_ACCEPT".equals(action)
                && "npc.status.quest_accepted".equals(status)) {
            com.example.immortal_cultivation_mod.guidance.DaoGuidanceSystem.record(
                    player,
                    com.example.immortal_cultivation_mod.guidance.DaoGuidanceContent.ACCEPT_QUEST);
        }
        boolean retaliate = profile.getBoolean("SocialRetaliation");
        profile.remove("SocialRetaliation");
        boolean startSpar = profile.getBoolean("StartSpar");
        profile.remove("StartSpar");
        sendProfile(mob, player, false, status);
        if (startSpar) {
            releaseInteraction(mob);
            beginSpar(mob, player);
        } else if (retaliate) {
            releaseInteraction(mob);
            mob.setTarget(player);
        }
    }

    private static String requestSpar(PathfinderMob mob, ServerPlayer player) {
        if (isSparring(mob) || sparPartner(player) != null) {
            return "npc.status.spar_busy";
        }
        profile(mob).putBoolean("StartSpar", true);
        return "npc.status.spar_started";
    }

    private static void beginSpar(PathfinderMob mob, ServerPlayer player) {
        CompoundTag npcProfile = profile(mob);
        npcProfile.putUUID("SparPlayer", player.getUUID());
        npcProfile.putLong("SparCountdownEnd", mob.level().getGameTime() + 60L);
        npcProfile.putInt("SparCountdownSecond", 3);
        player.getPersistentData().putUUID(PLAYER_SPAR_NPC_TAG, mob.getUUID());
        mob.setNoAi(false);
        mob.setTarget(null);
        mob.getNavigation().stop();
        player.displayClientMessage(Component.translatable(
                "message." + ImmortalCultivationMod.MODID + ".spar_countdown", 3), true);
    }

    public static boolean isSparring(PathfinderMob mob) {
        return profile(mob).hasUUID("SparPlayer");
    }

    public static boolean isSparCountdown(net.minecraft.world.entity.LivingEntity entity) {
        net.minecraft.world.entity.LivingEntity partner = sparPartner(entity);
        PathfinderMob mob = entity instanceof PathfinderMob pathfinder && entity instanceof CultivationNpc
                ? pathfinder
                : partner instanceof PathfinderMob pathfinder && partner instanceof CultivationNpc ? pathfinder : null;
        return mob != null && profile(mob).getLong("SparCountdownEnd") > mob.level().getGameTime();
    }

    public static boolean handleDebugEditorInteraction(PathfinderMob mob, Player player, InteractionHand hand) {
        if (!player.isShiftKeyDown() || !player.getItemInHand(hand).is(ModItems.DEBUG_STAT_EDITOR.get())) {
            return false;
        }
        if (!player.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
            openDebugRelationship(mob, serverPlayer);
        }
        return true;
    }

    public static net.minecraft.world.entity.LivingEntity sparPartner(net.minecraft.world.entity.LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return null;
        }
        UUID partnerId = null;
        if (entity instanceof ServerPlayer player && player.getPersistentData().hasUUID(PLAYER_SPAR_NPC_TAG)) {
            partnerId = player.getPersistentData().getUUID(PLAYER_SPAR_NPC_TAG);
        } else if (entity instanceof PathfinderMob mob && mob instanceof CultivationNpc
                && profile(mob).hasUUID("SparPlayer")) {
            partnerId = profile(mob).getUUID("SparPlayer");
        }
        if (partnerId == null) {
            return null;
        }
        var partner = level.getEntity(partnerId);
        return partner instanceof net.minecraft.world.entity.LivingEntity living ? living : null;
    }

    public static boolean isSparPair(net.minecraft.world.entity.LivingEntity first,
                                     net.minecraft.world.entity.LivingEntity second) {
        net.minecraft.world.entity.LivingEntity partner = sparPartner(first);
        return partner != null && partner.getUUID().equals(second.getUUID());
    }

    public static void finishSpar(
            net.minecraft.world.entity.LivingEntity defeated,
            net.minecraft.world.entity.LivingEntity winner) {
        net.minecraft.world.entity.LivingEntity partner = sparPartner(defeated);
        ServerPlayer player = defeated instanceof ServerPlayer sp
                ? sp : partner instanceof ServerPlayer sp ? sp : null;
        PathfinderMob mob = defeated instanceof PathfinderMob pathfinder
                && defeated instanceof CultivationNpc
                ? pathfinder
                : partner instanceof PathfinderMob pathfinder
                        && partner instanceof CultivationNpc
                        ? pathfinder : null;
        if (player != null
                && mob != null
                && defeated.getUUID().equals(mob.getUUID())
                && winner != null
                && winner.getUUID().equals(player.getUUID())) {
            rewardWeaponCareSparVictory(mob, player);
        }
        endSpar(defeated);
    }

    private static void rewardWeaponCareSparVictory(
            PathfinderMob mob, ServerPlayer player) {
        String identity = normalizeIdentity(
                profile(mob).getString("Identity"));
        String suffix;
        if ("sword_cultivator".equals(identity)) {
            long day = mob.level().getGameTime() / TICKS_PER_INGAME_DAY;
            int choice = Math.floorMod(
                    mob.getUUID().hashCode()
                            ^ player.getUUID().hashCode()
                            ^ Long.hashCode(day),
                    3);
            suffix = choice == 0
                    ? "yangjian_stone" : "sword_grindstone";
        } else if ("saber_cultivator".equals(identity)) {
            suffix = "saber_grindstone";
        } else {
            return;
        }

        CompoundTag relation = relationship(mob, player);
        long rewardDay = mob.level().getGameTime() / TICKS_PER_INGAME_DAY + 1L;
        if (relation.getLong(WEAPON_CARE_SPAR_REWARD_DAY) == rewardDay) {
            player.displayClientMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID
                            + ".spar_weapon_care_reward_cooldown"), false);
            return;
        }

        int tier = weaponCareTier(mob);
        ItemStack reward = new ItemStack(item(tieredItemId(suffix, tier)));
        if (reward.isEmpty() || reward.is(Items.AIR)) {
            return;
        }
        Component rewardName = reward.getHoverName();
        giveToPlayer(player, reward);
        relation.putLong(WEAPON_CARE_SPAR_REWARD_DAY, rewardDay);
        saveRelationship(mob, player, relation);
        changeRelationship(mob, player, 2, 2, 1, 0);
        player.displayClientMessage(Component.translatable(
                "message." + ImmortalCultivationMod.MODID
                        + ".spar_weapon_care_reward",
                rewardName), false);
    }

    public static void endSpar(net.minecraft.world.entity.LivingEntity participant) {
        net.minecraft.world.entity.LivingEntity partner = sparPartner(participant);
        ServerPlayer player = participant instanceof ServerPlayer sp
                ? sp : partner instanceof ServerPlayer sp ? sp : null;
        PathfinderMob mob = participant instanceof PathfinderMob pathfinder && participant instanceof CultivationNpc
                ? pathfinder
                : partner instanceof PathfinderMob pathfinder && partner instanceof CultivationNpc ? pathfinder : null;
        if (player != null) {
            player.getPersistentData().remove(PLAYER_SPAR_NPC_TAG);
            player.setHealth(player.getMaxHealth());
        }
        if (mob != null) {
            profile(mob).remove("SparPlayer");
            profile(mob).remove("SparCountdownEnd");
            profile(mob).remove("SparCountdownSecond");
            mob.setTarget(null);
            mob.getNavigation().stop();
            mob.setHealth(mob.getMaxHealth());
        }
        if (player != null && mob != null) {
            player.displayClientMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID + ".spar_ended", mob.getName()), false);
        }
    }

    public static void defendDaoCompanion(ServerPlayer player, net.minecraft.world.entity.LivingEntity attacker) {
        if (!(player.level() instanceof ServerLevel level) || sparPartner(player) != null) {
            return;
        }
        AABB area = player.getBoundingBox().inflate(48.0D);
        for (PathfinderMob mob : level.getEntitiesOfClass(PathfinderMob.class, area,
                candidate -> candidate instanceof CultivationNpc && candidate.isAlive())) {
            CompoundTag npcProfile = profile(mob);
            if (!npcProfile.hasUUID("DaoCompanionPlayer")
                    || !npcProfile.getUUID("DaoCompanionPlayer").equals(player.getUUID())
                    || mob == attacker) {
                continue;
            }
            if (isInteracting(mob)) {
                releaseInteraction(mob);
            }
            mob.setNoAi(false);
            mob.setTarget(attacker);
            mob.getNavigation().stop();
        }
    }

    private static String inviteToSect(PathfinderMob mob, ServerPlayer player) {
        if (relationshipValue(mob, player, "Trust") < 50) {
            return "npc.status.sect_trust_required";
        }
        CompoundTag npcProfile = profile(mob);
        if (npcProfile.hasUUID("SectLeader") && !npcProfile.getUUID("SectLeader").equals(player.getUUID())) {
            return "npc.status.sect_already_joined";
        }
        npcProfile.putUUID("SectLeader", player.getUUID());
        CompoundTag relation = relationship(mob, player);
        relation.putBoolean("SectMember", true);
        saveRelationship(mob, player, relation);
        return "npc.status.sect_joined";
    }

    private static String formMasterDiscipleBond(PathfinderMob mob, ServerPlayer player, boolean npcIsMaster) {
        if (relationshipValue(mob, player, "Trust") < 75) {
            return "npc.status.master_trust_required";
        }
        CompoundTag relation = relationship(mob, player);
        relation.putBoolean("NpcIsMaster", npcIsMaster);
        relation.putBoolean("NpcIsDisciple", !npcIsMaster);
        saveRelationship(mob, player, relation);
        CompoundTag npcProfile = profile(mob);
        npcProfile.putUUID(npcIsMaster ? "DisciplePlayer" : "MasterPlayer", player.getUUID());
        npcProfile.remove(npcIsMaster ? "MasterPlayer" : "DisciplePlayer");
        return npcIsMaster ? "npc.status.master_formed" : "npc.status.disciple_formed";
    }

    private static String formDaoCompanionBond(PathfinderMob mob, ServerPlayer player) {
        CompoundTag relation = relationship(mob, player);
        if (relation.getInt("Affection") < 50) {
            return "npc.status.dao_companion_relation_required";
        }
        if (gender(mob, archetypeOf(mob)).equals(playerGender(player))) {
            return "npc.status.dao_companion_gender_required";
        }
        CompoundTag npcProfile = profile(mob);
        if (npcProfile.hasUUID("DaoCompanionPlayer")
                && !npcProfile.getUUID("DaoCompanionPlayer").equals(player.getUUID())) {
            return "npc.status.dao_companion_taken";
        }
        npcProfile.putUUID("DaoCompanionPlayer", player.getUUID());
        relation.putBoolean("DaoCompanion", true);
        relation.putInt("Affection", 100);
        saveRelationship(mob, player, relation);
        return "npc.status.dao_companion_formed";
    }

    private static boolean isInteractionOwner(PathfinderMob mob, Player player) {
        CompoundTag profile = profile(mob);
        return profile.hasUUID("InteractingPlayer") && profile.getUUID("InteractingPlayer").equals(player.getUUID());
    }

    private static void releaseInteraction(PathfinderMob mob) {
        CompoundTag profile = profile(mob);
        boolean previousNoAi = profile.getBoolean("InteractionPreviousNoAi");
        profile.remove("InteractingPlayer");
        profile.remove("InteractionTimeout");
        profile.remove("InteractionPreviousNoAi");
        profile.remove("InteractionForcedNoAi");
        mob.setNoAi(previousNoAi);
    }

    private static String talk(PathfinderMob mob, ServerPlayer player, String topic) {
        CompoundTag relation = relationship(mob, player);
        if (relation.getInt("Hatred") >= 50) {
            return "npc.status.hatred_refusal";
        }
        long now = mob.level().getGameTime();
        if (now - relation.getLong("LastTalkTime") >= 100L) {
            changeRelationship(mob, player, 1, 1, 0, 0);
            relation = relationship(mob, player);
            relation.putLong("LastTalkTime", now);
            saveRelationship(mob, player, relation);
        }
        String identity = normalizeIdentity(profile(mob).getString("Identity"));
        if (!identity.isBlank()) {
            return identityDialogue(mob, identity, topic);
        }
        String dialogueKey = "realm".equals(topic)
                ? "npc.dialogue.realm." + realmDialogueGroup(mob)
                : "npc.dialogue." + personality(mob, archetypeOf(mob)) + "." + topic;
        return randomDialogue(mob, dialogueKey);
    }

    private static String identityDialogue(PathfinderMob mob, String identity, String topic) {
        int first;
        int count;
        switch (topic) {
            case "realm" -> {
                first = 6;
                count = 5;
            }
            case "world" -> {
                first = 11;
                count = 5;
            }
            default -> {
                first = 0;
                count = 6;
            }
        }

        String lastTag = "LastIdentityDialogue_" + topic;
        CompoundTag npcProfile = profile(mob);
        int last = npcProfile.contains(lastTag) ? npcProfile.getInt(lastTag) : -1;
        int index = first + mob.getRandom().nextInt(count);
        if (count > 1 && index == last) {
            index = first + Math.floorMod(index - first + 1 + mob.getRandom().nextInt(count - 1), count);
        }
        npcProfile.putInt(lastTag, index);
        String dialogueIdentity = switch (identity) {
            case "talisman_cultivator", "formation_cultivator" -> "wandering_cultivator";
            default -> identity;
        };
        return "npc.dialogue.identity." + dialogueIdentity + "." + index;
    }

    private static String realmDialogueGroup(PathfinderMob mob) {
        int realmIndex = CultivationLevels.getRealmIndex(profile(mob).getString("Realm"));
        return switch (realmIndex) {
            case 1 -> "lianqi";
            case 2 -> "zhuji";
            case 3 -> "jindan";
            case 4 -> "yuanying";
            case 5, 6, 7, 8, 9 -> "higher";
            default -> "mortal";
        };
    }

    private static String giveGift(PathfinderMob mob, ServerPlayer player) {
        ItemStack held = heldStack(player);
        if (held.isEmpty()) {
            return "npc.status.no_held_item";
        }
        List<ItemStack> inventory = inventory(mob);
        ItemStack gift = held.copyWithCount(1);
        // Score the original gift before inventory stacking can consume this
        // temporary stack. Otherwise, a matching existing stack turns the
        // gift empty and makes it look like a value-1 present.
        GiftEvaluation evaluation = evaluateGift(mob, gift);
        if (!insertInventory(inventory, gift.copy())) {
            return "npc.status.inventory_full";
        }
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        writeInventory(mob, profile(mob), inventory);
        switch (evaluation.tier()) {
            case POOR -> changeRelationship(mob, player, -4, -3, -2, 3);
            case ACCEPTABLE -> changeRelationship(mob, player, 2, 1, 0, -1);
            case GOOD -> changeRelationship(mob, player, 6, 3, 2, -2);
            case EXCELLENT -> changeRelationship(mob, player, 12, 7, 5, -4);
        }
        return randomDialogue(mob, "npc.status.gift_" + evaluation.tier().key());
    }

    private static GiftEvaluation evaluateGift(PathfinderMob mob, ItemStack stack) {
        int quality = giftQuality(stack);
        int expected = giftExpectation(mob);
        GiftTier tier;
        if (quality < expected) {
            tier = GiftTier.POOR;
        } else if ((long) quality < (long) expected * 2L) {
            tier = GiftTier.ACCEPTABLE;
        } else if ((long) quality < (long) expected * 4L) {
            tier = GiftTier.GOOD;
        } else {
            tier = GiftTier.EXCELLENT;
        }
        return new GiftEvaluation(quality, expected, tier);
    }

    private static int giftQuality(ItemStack stack) {
        return Math.max(1, SystemShopPrices.price(stack));
    }

    private static int giftExpectation(PathfinderMob mob) {
        int realm = CultivationLevels.getRealmIndex(profile(mob).getString("Realm"));
        int index = Mth.clamp(realm, 0, GIFT_EXPECTATION_BY_REALM.size() - 1);
        return GIFT_EXPECTATION_BY_REALM.get(index);
    }

    private static String performSocialAction(PathfinderMob mob, ServerPlayer player, SocialAction action) {
        CompoundTag relation = relationship(mob, player);
        SocialCheck check = socialCheck(mob, player, relation, action);
        long now = mob.level().getGameTime();
        String actionId = action.name().toLowerCase(Locale.ROOT);
        boolean favorable = mob.getRandom().nextInt(100) < check.chance();
        int actionCount = relation.getInt(actionId + "Count") + 1;
        SocialOutcome outcome = socialOutcome(mob, player, relation, action, favorable, check, actionCount);

        relation.putInt("SocialFatigue", Math.min(8, check.fatigue() + 1));
        relation.putLong("LastSocialTime", now);
        relation.putString("LastSocialAction", actionId);
        relation.putInt("SocialRepeat", check.repeats());
        relation.putInt(actionId + "Count", actionCount);
        relation.putInt("Fear", Mth.clamp(relation.getInt("Fear") + outcome.fear(), 0, 100));
        relation.putInt("Grudge", Mth.clamp(relation.getInt("Grudge") + outcome.grudge(), 0, 100));
        applyRelationshipDelta(relation, outcome.friendship(), outcome.trust(), outcome.affection(), outcome.hatred());

        int futureHatred = relation.getInt("Hatred");
        if ((action == SocialAction.INSULT && actionCount >= 3)
                || (action == SocialAction.THREATEN && actionCount >= 2)) {
            relation.putLong("RefuseUntil", now + SOCIAL_REFUSAL_TICKS);
        }
        if ((action == SocialAction.INSULT && actionCount >= 8)
                || (action == SocialAction.THREATEN && actionCount >= 6)
                || futureHatred >= 100 || relation.getInt("Grudge") >= 90) {
            relation.putBoolean("PermanentEnemy", true);
        }
        saveRelationship(mob, player, relation);

        if (action == SocialAction.INSULT || action == SocialAction.THREATEN) {
            spreadSocialOffenseToAllies(mob, player, action == SocialAction.INSULT ? 3 : 5);
        }
        if (outcome.retaliate() || relation.getBoolean("PermanentEnemy")) {
            profile(mob).putBoolean("SocialRetaliation", true);
        }
        return randomDialogue(mob, outcome.statusKey());
    }

    private static SocialCheck socialCheck(PathfinderMob mob, ServerPlayer player, CompoundTag relation,
                                            SocialAction action) {
        long elapsed = Math.max(0L, mob.level().getGameTime() - relation.getLong("LastSocialTime"));
        boolean resetForNewDay = elapsed >= SOCIAL_CHANCE_RESET_TICKS;
        int fatigue = resetForNewDay
                ? 0
                : Math.max(0, relation.getInt("SocialFatigue") - (int) (elapsed / 1_200L));
        String actionId = action.name().toLowerCase(Locale.ROOT);
        int repeats = !resetForNewDay && actionId.equals(relation.getString("LastSocialAction"))
                ? relation.getInt("SocialRepeat") + 1
                : 0;
        int chance = action.baseChance()
                + personalitySocialModifier(personality(mob, archetypeOf(mob)), action)
                + relationshipSocialModifier(relation, action)
                + reputationSocialModifier(mob, player, action)
                + (action == SocialAction.THREATEN ? threatPowerModifier(mob, player) : 0)
                - fatigue * 7
                - repeats * 9;
        return new SocialCheck(Mth.clamp(chance, 5, 95), fatigue, repeats);
    }

    private static int socialSuccessChance(PathfinderMob mob, ServerPlayer player, SocialAction action) {
        return socialCheck(mob, player, relationship(mob, player), action).chance();
    }

    private static int reputationSocialModifier(PathfinderMob mob, ServerPlayer player, SocialAction action) {
        int playerReputation = playerReputation(player);
        return switch (action) {
            case PRAISE -> Mth.clamp(playerReputation / 8, -12, 12);
            case JOKE -> Mth.clamp(playerReputation / 12, -8, 8);
            case INSULT -> 0;
            case THREATEN -> Mth.clamp(
                    (playerReputation - profile(mob).getInt("Reputation")) / 4,
                    -20,
                    20);
        };
    }

    private static int playerReputation(ServerPlayer player) {
        int earnedReputation = player.getPersistentData().getInt(PLAYER_REPUTATION_TAG);
        int moralReputation = ModAttachments.getData(player).moral() - 50;
        return Mth.clamp(earnedReputation + moralReputation, -100, 100);
    }

    private static void addPlayerReputation(ServerPlayer player, int amount) {
        CompoundTag data = player.getPersistentData();
        data.putInt(PLAYER_REPUTATION_TAG,
                Mth.clamp(data.getInt(PLAYER_REPUTATION_TAG) + amount, -100, 100));
    }

    private static int threatPowerModifier(PathfinderMob mob, ServerPlayer player) {
        int playerStage = CultivationLevels.getStageIndex(ModAttachments.getData(player).cultivationLevel());
        int npcStage = CultivationLevels.getStageIndex(profile(mob).getString("Realm"));
        int realmDifference = Mth.clamp((playerStage - npcStage) * 7, -35, 35);

        double playerPower = player.getMaxHealth()
                + player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 8.0D
                + player.getAttributeValue(Attributes.ARMOR) * 4.0D;
        double npcPower = mob.getMaxHealth()
                + mob.getAttributeValue(Attributes.ATTACK_DAMAGE) * 8.0D
                + mob.getAttributeValue(Attributes.ARMOR) * 4.0D;
        double scale = Math.max(1.0D, Math.max(playerPower, npcPower));
        int combatDifference = Mth.clamp((int) Math.round((playerPower - npcPower) / scale * 30.0D), -30, 30);
        return realmDifference + combatDifference;
    }

    private static int personalitySocialModifier(String personality, SocialAction action) {
        return switch (action) {
            case PRAISE -> switch (personality) {
                case "proud" -> 25;
                case "gentle" -> 12;
                case "scholarly" -> 6;
                case "aggressive" -> -12;
                case "cautious" -> -6;
                default -> 0;
            };
            case JOKE -> switch (personality) {
                case "wandering" -> 25;
                case "gentle" -> 12;
                case "aggressive" -> 5;
                case "scholarly" -> -15;
                case "cautious" -> -10;
                case "proud" -> -5;
                default -> 0;
            };
            case INSULT -> switch (personality) {
                case "proud" -> 30;
                case "aggressive" -> 22;
                case "gentle" -> 10;
                case "scholarly" -> 5;
                case "cautious" -> -10;
                case "wandering" -> -15;
                default -> 0;
            };
            case THREATEN -> switch (personality) {
                case "cautious" -> 28;
                case "gentle" -> 16;
                case "scholarly" -> 6;
                case "wandering" -> -10;
                case "proud" -> -25;
                case "aggressive" -> -30;
                default -> 0;
            };
        };
    }

    private static int relationshipSocialModifier(CompoundTag relation, SocialAction action) {
        int friendship = relation.getInt("Friendship");
        int trust = relation.getInt("Trust");
        int affection = relation.getInt("Affection");
        int hatred = relation.getInt("Hatred");
        return switch (action) {
            case PRAISE, JOKE -> Mth.clamp((friendship + trust + affection - hatred) / 10, -20, 20);
            case INSULT -> Mth.clamp((hatred + Math.abs(friendship) / 2) / 7, -10, 20);
            case THREATEN -> Mth.clamp((hatred - trust) / 8, -20, 20);
        };
    }

    private static SocialOutcome socialOutcome(PathfinderMob mob, ServerPlayer player, CompoundTag relation,
                                               SocialAction action, boolean favorable, SocialCheck check,
                                               int actionCount) {
        return switch (action) {
            case PRAISE -> favorable
                    ? new SocialOutcome(4, 2, 1, -1, 0, -1, false, "npc.status.praised")
                    : new SocialOutcome(-1, -1, 0, 1, 0, 1, false, "npc.status.praise_failed");
            case JOKE -> favorable
                    ? new SocialOutcome(4, 1, 1, -1, 0, -1, false, "npc.status.joke_success")
                    : new SocialOutcome(-2, -1, 0, 1, 0, 1, false, "npc.status.joke_failed");
            case INSULT -> insultOutcome(mob, relation, favorable, actionCount);
            case THREATEN -> threatOutcome(mob, player, relation, favorable, check, actionCount);
        };
    }

    private static SocialOutcome insultOutcome(PathfinderMob mob, CompoundTag relation, boolean severe,
                                               int insultCount) {
        String personality = personality(mob, archetypeOf(mob));
        int sensitivity = switch (personality) {
            case "proud" -> 4;
            case "aggressive" -> 3;
            case "gentle", "scholarly" -> -1;
            case "cautious", "wandering" -> -2;
            default -> 0;
        };
        int bondPenalty = Math.max(0,
                (relation.getInt("Friendship") + relation.getInt("Trust") + relation.getInt("Affection")) / 60);
        if (!severe) {
            return new SocialOutcome(-2, -1, 0, 2, 0, 2, false, "npc.status.insult_ignored");
        }
        int hatred = Mth.clamp(7 + sensitivity + bondPenalty, 4, 16);
        boolean retaliate = insultCount >= 5
                || (insultCount >= 3 && ("proud".equals(personality) || "aggressive".equals(personality))
                && relation.getInt("Hatred") + hatred >= 40);
        return new SocialOutcome(
                -Mth.clamp(6 + sensitivity + bondPenalty, 3, 15),
                -Mth.clamp(4 + sensitivity + bondPenalty, 2, 12),
                -Math.max(1, 2 + bondPenalty),
                hatred,
                0,
                Math.max(3, hatred / 2),
                retaliate,
                retaliate ? "npc.status.insult_retaliation" : "npc.status.insulted");
    }

    private static SocialOutcome threatOutcome(PathfinderMob mob, ServerPlayer player, CompoundTag relation,
                                               boolean successful, SocialCheck check, int threatCount) {
        if (successful) {
            int playerStage = CultivationLevels.getStageIndex(ModAttachments.getData(player).cultivationLevel());
            int npcStage = CultivationLevels.getStageIndex(profile(mob).getString("Realm"));
            int fear = Mth.clamp(8 + Math.max(0, playerStage - npcStage) * 2 + check.repeats() * 2, 5, 30);
            boolean surrendered = relation.getInt("Fear") + fear >= 20 && surrenderOneItem(mob, player);
            return new SocialOutcome(-3, -8, -1, 5, fear, 3, false,
                    surrendered ? "npc.status.threatened_item" : "npc.status.threatened_cowed");
        }

        String personality = personality(mob, archetypeOf(mob));
        boolean proudRetaliation = "proud".equals(personality) || "aggressive".equals(personality);
        boolean retaliate = threatCount >= 3 || (check.chance() <= 30 && proudRetaliation);
        return new SocialOutcome(-8, -4, -2, 12, -3, 10 + check.repeats() * 2, retaliate,
                retaliate ? "npc.status.threat_retaliation" : "npc.status.threatened");
    }

    private static boolean surrenderOneItem(PathfinderMob mob, ServerPlayer player) {
        List<ItemStack> inventory = inventory(mob);
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack surrendered = stack.copyWithCount(1);
            stack.shrink(1);
            if (stack.isEmpty()) {
                inventory.set(i, ItemStack.EMPTY);
            }
            writeInventory(mob, profile(mob), inventory);
            giveToPlayer(player, surrendered);
            return true;
        }
        return false;
    }

    private static void spreadSocialOffenseToAllies(PathfinderMob mob, ServerPlayer player, int severity) {
        if (!(mob.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        for (PathfinderMob ally : serverLevel.getEntitiesOfClass(
                PathfinderMob.class,
                mob.getBoundingBox().inflate(24.0D),
                other -> other != mob && other instanceof CultivationNpc && isSocialAlly(mob, other))) {
            changeRelationship(ally, player, -severity, -Math.max(1, severity / 2), 0, Math.max(1, severity / 2));
        }
    }

    public static boolean areCombatAllies(LivingEntity first, LivingEntity second) {
        if (first == second || first.isAlliedTo(second) || second.isAlliedTo(first)) {
            return true;
        }
        if (UndeadControl.isOwnedByOwnerUuid(first, second.getUUID())
                || UndeadControl.isOwnedByOwnerUuid(second, first.getUUID())) {
            return true;
        }
        if (first instanceof PathfinderMob firstNpc && first instanceof CultivationNpc) {
            ensure(firstNpc, archetypeOf(firstNpc));
            CompoundTag firstProfile = profile(firstNpc);
            if (second instanceof Player player
                    && (matchesPlayerBond(firstProfile, "DaoCompanionPlayer", player)
                    || matchesPlayerBond(firstProfile, "FollowingPlayer", player)
                    || matchesPlayerBond(firstProfile, "SectLeader", player))) {
                return true;
            }
            if (second instanceof PathfinderMob secondNpc && second instanceof CultivationNpc) {
                ensure(secondNpc, archetypeOf(secondNpc));
                CompoundTag secondProfile = profile(secondNpc);
                return isSocialAlly(firstNpc, secondNpc)
                        || sameBond(firstProfile, secondProfile, "DaoCompanionPlayer")
                        || sameBond(firstProfile, secondProfile, "SectLeader");
            }
        }
        return second instanceof PathfinderMob
                && second instanceof CultivationNpc
                && areCombatAllies(second, first);
    }

    /** NPC projectiles pass through every non-combatant; player projectile behavior is unchanged. */
    public static boolean canNpcProjectileHit(Entity owner, Entity target) {
        Entity resolvedTarget = ModelBonePartEntity.resolveParent(target);
        if (owner instanceof CultivationNpc
                && owner instanceof LivingEntity livingOwner
                && resolvedTarget instanceof LivingEntity livingTarget) {
            return isCombatEnemy(livingOwner, livingTarget);
        }
        return true;
    }

    /** Persists that an effect was launched by an NPC so owner loss cannot make it fail open. */
    public static void markNpcOwnedEffect(Entity effect, Entity owner) {
        if (effect != null && owner instanceof CultivationNpc) {
            effect.getPersistentData().putBoolean(NPC_OWNED_EFFECT_TAG, true);
        }
    }

    /** True when an NPC-launched effect can no longer resolve its NPC owner. */
    public static boolean isOrphanedNpcEffect(Entity effect, Entity owner) {
        return effect != null
                && effect.getPersistentData().getBoolean(NPC_OWNED_EFFECT_TAG)
                && (!(owner instanceof CultivationNpc)
                || !(owner instanceof LivingEntity livingOwner)
                || !livingOwner.isAlive()
                || livingOwner.isRemoved()
                || livingOwner.level() != effect.level());
    }

    /** NPC effects hit combat enemies only and fail closed if their persisted owner disappears. */
    public static boolean canNpcEffectHit(Entity effect, Entity owner, Entity target) {
        if (isOrphanedNpcEffect(effect, owner)) {
            return false;
        }
        return canNpcProjectileHit(owner, target);
    }

    public static LivingEntity damageAttacker(DamageSource source) {
        if (source.getEntity() instanceof LivingEntity living) {
            return living;
        }
        return source.getDirectEntity() instanceof Projectile projectile
                && projectile.getOwner() instanceof LivingEntity owner ? owner : null;
    }

    public static boolean isNonCombatNpcDamage(DamageSource source, LivingEntity target) {
        LivingEntity attacker = damageAttacker(source);
        return attacker instanceof CultivationNpc && !isCombatEnemy(attacker, target);
    }

    /** Shared actor-neutral hostility rule for NPC-owned area and weapon effects. */
    public static boolean isCombatEnemy(LivingEntity attacker, LivingEntity target) {
        if (attacker == target || !target.isAlive() || areCombatAllies(attacker, target)) {
            return false;
        }
        if (!(attacker instanceof PathfinderMob mob) || !(attacker instanceof CultivationNpc)) {
            return !attacker.isAlliedTo(target);
        }
        LivingEntity primary = mob.getTarget();
        if (target == primary || target == mob.getLastHurtByMob() || isSparPair(attacker, target)) {
            return true;
        }
        if (primary != null && areCombatAllies(primary, target)) {
            return true;
        }
        if (target instanceof Mob other
                && (other.getTarget() == attacker || other.getLastHurtByMob() == attacker)) {
            return true;
        }
        return target instanceof ServerPlayer player && shouldAttackPlayer(mob, player);
    }

    private static boolean matchesPlayerBond(CompoundTag npcProfile, String key, Player player) {
        return npcProfile.hasUUID(key) && npcProfile.getUUID(key).equals(player.getUUID());
    }

    private static boolean sameBond(CompoundTag first, CompoundTag second, String key) {
        return first.hasUUID(key) && second.hasUUID(key)
                && first.getUUID(key).equals(second.getUUID(key));
    }

    private static boolean isSocialAlly(PathfinderMob first, PathfinderMob second) {
        ensure(second, archetypeOf(second));
        String firstIdentity = profile(first).getString("Identity");
        String secondIdentity = profile(second).getString("Identity");
        if (!firstIdentity.isBlank() && firstIdentity.equals(secondIdentity)) {
            return true;
        }
        String firstName = profile(first).getString("Name");
        String secondName = profile(second).getString("Name");
        return !firstName.isBlank() && !secondName.isBlank()
                && firstName.codePointAt(0) == secondName.codePointAt(0);
    }

    private static String randomDialogue(PathfinderMob mob, String baseKey) {
        int variants = baseKey.startsWith("npc.dialogue.") ? 6 : 3;
        int variant = mob.getRandom().nextInt(variants);
        return variant == 0 ? baseKey : baseKey + "." + variant;
    }

    private static String exchangeTrade(PathfinderMob mob, ServerPlayer player, int index) {
        if (relationshipValue(mob, player, "Hatred") >= 50) {
            return "npc.status.hatred_refusal";
        }
        List<TradeOffer> offers = tradeOffers(mob, player);
        if (index < 0 || index >= offers.size()) {
            return "npc.status.trade_invalid";
        }
        TradeOffer offer = offers.get(index);
        Item costItem = item(offer.currencyItemId());
        Item resultItem = item(offer.resultItemId());
        if (costItem == Items.AIR || resultItem == Items.AIR) {
            return "npc.status.trade_invalid";
        }
        if (countItem(player, costItem) < offer.price()) {
            return "npc.status.trade_missing";
        }
        List<ItemStack> inventory = inventory(mob);
        if (offer.inventorySlot() < 0 || offer.inventorySlot() >= inventory.size()) {
            return "npc.status.trade_sold_out";
        }
        ItemStack stock = inventory.get(offer.inventorySlot());
        if (stock.isEmpty() || stock.getItem() != resultItem || stock.getCount() < offer.resultCount()) {
            return "npc.status.trade_sold_out";
        }
        if (isIdentityProtectedTradeItem(mob, stock)) {
            return "npc.status.trade_invalid";
        }
        if (!player.getAbilities().instabuild) {
            removeItems(player, costItem, offer.price());
        }
        ItemStack purchased = stock.copyWithCount(offer.resultCount());
        stock.shrink(offer.resultCount());
        if (stock.isEmpty()) {
            inventory.set(offer.inventorySlot(), ItemStack.EMPTY);
        }
        insertInventory(inventory, new ItemStack(costItem, offer.price()));
        writeInventory(mob, profile(mob), inventory);
        giveToPlayer(player, purchased);
        changeRelationship(mob, player, 1, 1, 0, 0);
        return "npc.status.trade_complete";
    }

    private static String acceptQuest(PathfinderMob mob, ServerPlayer player) {
        CompoundTag relation = relationship(mob, player);
        if (relation.getInt("QuestState") != 0) {
            return "npc.status.quest_already_accepted";
        }
        relation.putInt("QuestState", 1);
        relation.putString("QuestId", quest(mob).id());
        saveRelationship(mob, player, relation);
        return "npc.status.quest_accepted";
    }

    private static String completeQuest(PathfinderMob mob, ServerPlayer player) {
        CompoundTag relation = relationship(mob, player);
        QuestDef quest = quest(mob);
        if (relation.getInt("QuestState") != 0 && !quest.id().equals(relation.getString("QuestId"))) {
            relation.putInt("QuestState", 0);
            relation.remove("QuestId");
            saveRelationship(mob, player, relation);
            return "npc.status.quest_not_accepted";
        }
        if (relation.getInt("QuestState") == 0) {
            return "npc.status.quest_not_accepted";
        }
        if (relation.getInt("QuestState") >= 2) {
            return "npc.status.quest_completed_before";
        }
        Item required = item(quest.requiredItemId());
        if (countItem(player, required) < quest.requiredCount()) {
            return "npc.status.quest_missing_items";
        }
        List<ItemStack> npcInventory = inventory(mob);
        Item reward = item(quest.rewardItemId());
        if (countInventoryItem(npcInventory, reward) < quest.rewardCount()) {
            return "npc.status.quest_reward_unavailable";
        }
        if (!player.getAbilities().instabuild) {
            removeItems(player, required, quest.requiredCount());
        }
        removeInventoryItems(npcInventory, reward, quest.rewardCount());
        insertInventory(npcInventory, new ItemStack(required, quest.requiredCount()));
        writeInventory(mob, profile(mob), npcInventory);
        giveItemsToPlayer(player, reward, quest.rewardCount());
        relation.putInt("QuestState", 2);
        saveRelationship(mob, player, relation);
        ListTag completed = profile(mob).getList("CompletedQuests", 8);
        completed.add(StringTag.valueOf(player.getUUID() + ":" + quest.id()));
        profile(mob).put("CompletedQuests", completed);
        changeRelationship(mob, player, 10, 8, 2, -5);
        addPlayerReputation(player, 5);
        com.example.immortal_cultivation_mod.fate.LuckSystem.rewardOnce(
                player, "npc_" + mob.getUUID() + "_" + quest.id(), 3);
        return "npc.status.quest_completed";
    }

    private static String followPlayer(PathfinderMob mob, ServerPlayer player) {
        CompoundTag profile = profile(mob);
        profile.putUUID("FollowingPlayer", player.getUUID());
        profile.putUUID("CommandOwner", player.getUUID());
        profile.putBoolean("StayHere", false);
        return "npc.status.following";
    }

    private static String toggleFollow(PathfinderMob mob, ServerPlayer player) {
        CompoundTag profile = profile(mob);
        if (profile.hasUUID("FollowingPlayer")
                && profile.getUUID("FollowingPlayer").equals(player.getUUID())) {
            profile.remove("FollowingPlayer");
            profile.remove("CommandOwner");
            profile.putBoolean("StayHere", false);
            mob.getNavigation().stop();
            return "npc.status.not_following";
        }
        return followPlayer(mob, player);
    }

    private static String stayHere(PathfinderMob mob, ServerPlayer player) {
        CompoundTag profile = profile(mob);
        profile.putUUID("CommandOwner", player.getUUID());
        profile.remove("FollowingPlayer");
        profile.putBoolean("StayHere", true);
        profile.putDouble("StayX", mob.getX());
        profile.putDouble("StayY", mob.getY());
        profile.putDouble("StayZ", mob.getZ());
        mob.getNavigation().stop();
        return "npc.status.staying";
    }

    private static String moveFreely(PathfinderMob mob, ServerPlayer player) {
        CompoundTag profile = profile(mob);
        profile.remove("FollowingPlayer");
        profile.remove("CommandOwner");
        profile.putBoolean("StayHere", false);
        mob.getNavigation().stop();
        return "npc.status.moving_freely";
    }

    private static String setHome(PathfinderMob mob, ServerPlayer player) {
        CompoundTag profile = profile(mob);
        profile.putUUID("CommandOwner", player.getUUID());
        profile.putBoolean("HasHome", true);
        profile.putDouble("HomeX", mob.getX());
        profile.putDouble("HomeY", mob.getY());
        profile.putDouble("HomeZ", mob.getZ());
        return "npc.status.home_set";
    }

    private static String goHome(PathfinderMob mob, ServerPlayer player) {
        CompoundTag profile = profile(mob);
        if (!profile.getBoolean("HasHome")) {
            return "npc.status.no_home";
        }
        profile.putUUID("CommandOwner", player.getUUID());
        profile.remove("FollowingPlayer");
        profile.putBoolean("StayHere", true);
        profile.putDouble("StayX", profile.getDouble("HomeX"));
        profile.putDouble("StayY", profile.getDouble("HomeY"));
        profile.putDouble("StayZ", profile.getDouble("HomeZ"));
        mob.getNavigation().stop();
        return "npc.status.going_home";
    }

    private static int spellLearningTrustRequired(PathfinderMob mob, String spellId) {
        CultivationLevels.LevelDef teacherLevel = CultivationLevels.getLevelDef(profile(mob).getString("Realm"));
        int stageAdjustment = switch (teacherLevel.stage()) {
            case CultivationLevels.STAGE_MID -> 2;
            case CultivationLevels.STAGE_LATE -> 5;
            default -> 0;
        };
        return Mth.clamp(ModSpells.npcLearningTrustRequired(spellId) + stageAdjustment, 0, 100);
    }

    private static int methodLearningTrustRequired(PathfinderMob mob) {
        return learningTrustRequired(profile(mob).getString("Realm"), METHOD_LEARNING_TRUST_BY_REALM);
    }

    private static int learningTrustRequired(String level, List<Integer> trustByRealm) {
        int realmIndex = Mth.clamp(CultivationLevels.getRealmIndex(level), 0, trustByRealm.size() - 1);
        CultivationLevels.LevelDef levelDef = CultivationLevels.getLevelDef(level);
        int stageAdjustment = switch (levelDef.stage()) {
            case CultivationLevels.STAGE_MID -> 2;
            case CultivationLevels.STAGE_LATE -> 5;
            default -> 0;
        };
        return Mth.clamp(trustByRealm.get(realmIndex) + stageAdjustment, 0, 100);
    }

    private static String learnSpell(PathfinderMob mob, ServerPlayer player, int index) {
        List<String> npcSpells = spellIds(mob, archetypeOf(mob));
        if (index < 0 || index >= npcSpells.size()) {
            return "npc.status.teaching_invalid";
        }
        String spellId = npcSpells.get(index);
        if (relationshipValue(mob, player, "Trust") < spellLearningTrustRequired(mob, spellId)) {
            return "npc.status.teaching_locked";
        }
        var spell = ModSpells.get(spellId);
        if (spell == null) {
            return "npc.status.teaching_invalid";
        }
        var data = ModAttachments.getData(player);
        if (data.knownSpells().contains(spellId) || ModSpells.isInnateKnown(spellId, data)) {
            return "npc.status.spell_known";
        }
        if (!ModSpells.meetsRequirement(data.cultivationLevel(), spell)) {
            return "npc.status.realm_too_low";
        }
        if (player.getAbilities().instabuild) {
            completeNpcLearning(player, mob.getId(), "spell", spellId);
            releaseInteraction(mob);
            return "npc.status.spell_learned";
        }
        ModPayloads.beginNpcLearning(player, mob.getId(), "spell", spellId, profile(mob).getString("Realm"));
        releaseInteraction(mob);
        return "npc.status.learning_started";
    }

    private static String learnMethod(PathfinderMob mob, ServerPlayer player) {
        if (relationshipValue(mob, player, "Trust") < methodLearningTrustRequired(mob)) {
            return "npc.status.teaching_locked";
        }
        String methodId = profile(mob).getString("Method");
        if (methodId.isBlank() || CultivationMethods.get(methodId) == null) {
            return "npc.status.no_method";
        }
        var data = ModAttachments.getData(player);
        if (methodId.equals(data.activeCultivationMethod())) {
            return "npc.status.method_known";
        }
        if (data.methodProficiencies().containsKey(methodId)) {
            ModAttachments.setData(player, data.withActiveCultivationMethod(methodId));
            ServerEvents.syncPlayerData(player);
            return "npc.status.method_learned";
        }
        if (player.getAbilities().instabuild) {
            completeNpcLearning(player, mob.getId(), "method", methodId);
            releaseInteraction(mob);
            return "npc.status.method_learned";
        }
        ModPayloads.beginNpcLearning(player, mob.getId(), "method", methodId, profile(mob).getString("Realm"));
        releaseInteraction(mob);
        return "npc.status.learning_started";
    }

    public static void completeNpcLearning(ServerPlayer player, int entityId, String learningType,
                                           String targetId) {
        if (!(player.level().getEntity(entityId) instanceof PathfinderMob mob)
                || !(mob instanceof CultivationNpc)) {
            return;
        }
        ensure(mob, archetypeOf(mob));
        ModAttachments.CultivationData data = ModAttachments.getData(player);
        if ("spell".equals(learningType)) {
            if (relationshipValue(mob, player, "Trust") < spellLearningTrustRequired(mob, targetId)
                    || !spellIds(mob, archetypeOf(mob)).contains(targetId)) {
                return;
            }
            ModSpells.SpellDef spell = ModSpells.get(targetId);
            if (spell == null
                    || data.knownSpells().contains(targetId)
                    || ModSpells.isInnateKnown(targetId, data)
                    || !ModSpells.meetsRequirement(data.cultivationLevel(), spell)) {
                return;
            }
            List<String> known = new ArrayList<>(data.knownSpells());
            known.add(targetId);
            ModAttachments.setData(player, data.withKnownSpells(known));
            changeRelationship(mob, player, 0, 1, 0, 0);
            player.sendSystemMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID + ".spell_learned", spell.name()));
            ServerEvents.syncPlayerData(player);
            return;
        }
        if (!"method".equals(learningType)
                || relationshipValue(mob, player, "Trust") < methodLearningTrustRequired(mob)
                || !targetId.equals(profile(mob).getString("Method"))
                || CultivationMethods.get(targetId) == null) {
            return;
        }
        ModAttachments.setData(player, rememberMethod(data, targetId).withActiveCultivationMethod(targetId));
        changeRelationship(mob, player, 0, 2, 0, 0);
        player.sendSystemMessage(Component.translatable(
                "message." + ImmortalCultivationMod.MODID + ".method_activated",
                Component.translatable(CultivationMethods.get(targetId).nameKey())));
        ServerEvents.syncPlayerData(player);
    }

    private static String teachNpcSpell(PathfinderMob mob, ServerPlayer player, int index) {
        if (relationshipValue(mob, player, "Trust") < NPC_LEARNING_TRUST_REQUIRED) {
            return "npc.status.teaching_locked";
        }
        List<String> playerSpells = ModAttachments.getData(player).knownSpells();
        if (index < 0 || index >= playerSpells.size()) {
            return "npc.status.teaching_invalid";
        }
        String spellId = ModSpells.normalizeId(playerSpells.get(index));
        ModSpells.SpellDef spell = ModSpells.get(spellId);
        if (spell == null || !NpcSpellcasting.isNpcSupportedSpell(spellId)) {
            return "npc.status.teaching_invalid";
        }
        if (!ModSpells.meetsRequirement(profile(mob).getString("Realm"), spell)) {
            return "npc.status.realm_too_low";
        }
        List<String> npcSpells = spellIds(mob, archetypeOf(mob));
        if (npcSpells.contains(spellId)) {
            return "npc.status.npc_spell_known";
        }
        npcSpells.add(spellId);
        profile(mob).put("Spells", stringList(npcSpells));
        changeRelationship(mob, player, 2, 2, 0, 0);
        return "npc.status.npc_spell_learned";
    }

    private static String teachNpcMethod(PathfinderMob mob, ServerPlayer player, int index) {
        if (relationshipValue(mob, player, "Trust") < NPC_LEARNING_TRUST_REQUIRED) {
            return "npc.status.teaching_locked";
        }
        List<String> methods = learnedMethodIds(ModAttachments.getData(player));
        if (index < 0 || index >= methods.size()) {
            return "npc.status.player_no_method";
        }
        String methodId = methods.get(index);
        CompoundTag profile = profile(mob);
        List<String> knownMethods = knownMethodIds(profile);
        if (knownMethods.contains(methodId)) {
            return "npc.status.npc_method_known";
        }
        knownMethods.add(methodId);
        profile.put("Methods", stringList(knownMethods));
        chooseBestKnownMethod(profile);
        changeRelationship(mob, player, 2, 3, 0, 0);
        return "npc.status.npc_method_learned";
    }

    private static ModAttachments.CultivationData rememberMethod(ModAttachments.CultivationData data, String methodId) {
        if (CultivationMethods.get(methodId) == null) {
            return data;
        }
        HashMap<String, Integer> methods = new HashMap<>(data.methodProficiencies());
        if (CultivationMethods.get(data.activeCultivationMethod()) != null) {
            methods.putIfAbsent(data.activeCultivationMethod(), 0);
        }
        methods.putIfAbsent(methodId, 0);
        return data.withMethodProficiencies(methods);
    }

    private static List<String> learnedMethodIds(ModAttachments.CultivationData data) {
        List<String> methods = new ArrayList<>();
        for (CultivationMethods.MethodDef method : CultivationMethods.allMethods()) {
            if (method.id().equals(data.activeCultivationMethod())
                    || data.methodProficiencies().containsKey(method.id())) {
                methods.add(method.id());
            }
        }
        return List.copyOf(methods);
    }

    private static String depositHeldItem(PathfinderMob mob, ServerPlayer player) {
        if (relationshipValue(mob, player, "Trust") < INVENTORY_TRUST_REQUIRED) {
            return "npc.status.inventory_locked";
        }
        ItemStack held = heldStack(player);
        if (held.isEmpty()) {
            return "npc.status.no_held_item";
        }
        List<ItemStack> inventory = inventory(mob);
        ItemStack deposited = held.copyWithCount(1);
        if (!insertInventory(inventory, deposited)) {
            return "npc.status.inventory_full";
        }
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        writeInventory(mob, profile(mob), inventory);
        return "npc.status.item_deposited";
    }

    private static String withdrawInventoryItem(PathfinderMob mob, ServerPlayer player, int index) {
        if (relationshipValue(mob, player, "Hatred") >= 50) {
            return "npc.status.hatred_refusal";
        }
        if (relationshipValue(mob, player, "Trust") < INVENTORY_TRUST_REQUIRED) {
            return "npc.status.inventory_locked";
        }
        syncEquippedCombatWeapon(mob);
        List<ItemStack> inventory = inventory(mob);
        if (index < 0 || index >= inventory.size() || inventory.get(index).isEmpty()) {
            return "npc.status.inventory_empty";
        }
        ItemStack stack = inventory.get(index);
        int withdrawnCount = stack.getCount();
        boolean withdrewEquipped = profile(mob).contains(EQUIPPED_WEAPON_SLOT_TAG)
                && profile(mob).getInt(EQUIPPED_WEAPON_SLOT_TAG) == index;
        inventory.set(index, ItemStack.EMPTY);
        writeInventory(mob, profile(mob), inventory);
        if (withdrewEquipped) {
            profile(mob).remove(EQUIPPED_WEAPON_SLOT_TAG);
            mob.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
        giveToPlayer(player, stack);
        return recordWithdrawal(mob, player, withdrawnCount)
                ? "npc.status.inventory_overused"
                : "npc.status.item_withdrawn";
    }

    private static boolean recordWithdrawal(PathfinderMob mob, ServerPlayer player, int withdrawnCount) {
        CompoundTag relation = relationship(mob, player);
        long now = mob.level().getGameTime();
        long windowStart = relation.getLong("WithdrawalWindowStart");
        if (!relation.contains("WithdrawalWindowStart") || now - windowStart > 600L) {
            relation.putLong("WithdrawalWindowStart", now);
            relation.putInt("WithdrawalItemCount", 0);
        }
        int total = relation.getInt("WithdrawalItemCount") + Math.max(1, withdrawnCount);
        relation.putInt("WithdrawalItemCount", total);
        boolean overused = total >= 8;
        if (overused) {
            relation.putInt("Trust", clampRelation(relation.getInt("Trust") - 25));
            relation.putLong("WithdrawalWindowStart", now);
            relation.putInt("WithdrawalItemCount", 0);
        }
        saveRelationship(mob, player, relation);
        return overused;
    }

    public static void recordPlayerAttack(PathfinderMob mob, ServerPlayer player) {
        ensure(mob, archetypeOf(mob));
        if (isSparPair(mob, player)) {
            return;
        }
        CompoundTag relation = relationship(mob, player);
        long now = mob.level().getGameTime();
        if (relation.getLong("LastPlayerAttackTick") == now) {
            return;
        }
        relation.putLong("LastPlayerAttackTick", now);
        if (relation.getLong("RetaliateUntil") > now) {
            relation.putLong("RetaliateUntil", now + 600L);
            saveRelationship(mob, player, relation);
            profile(mob).putUUID("RetaliationPlayer", player.getUUID());
            mob.setTarget(player);
            return;
        }
        long[] previousAttackTicks = relation.getLongArray("AttackTicks");
        long[] attackTicks = new long[previousAttackTicks.length + 1];
        int attacks = 0;
        for (long attackTick : previousAttackTicks) {
            if (now - attackTick <= 600L) {
                attackTicks[attacks++] = attackTick;
            }
        }
        attackTicks[attacks++] = now;
        if (attacks != attackTicks.length) {
            attackTicks = java.util.Arrays.copyOf(attackTicks, attacks);
        }
        relation.putLongArray("AttackTicks", attackTicks);
        int trust = relation.getInt("Trust");
        int requiredAttacks = trust >= 100 ? 10 : trust >= 50 ? 3 : 1;
        if (attacks >= requiredAttacks) {
            relation.putLong("RetaliateUntil", now + 600L);
            relation.remove("AttackTicks");
            relation.remove("AttackCount");
            relation.remove("AttackWindowStart");
            if (isInteracting(mob)) {
                releaseInteraction(mob);
            }
            mob.setNoAi(false);
            profile(mob).putUUID("RetaliationPlayer", player.getUUID());
            mob.setTarget(player);
        }
        saveRelationship(mob, player, relation);
    }

    public static boolean shouldAttackPlayer(PathfinderMob mob, ServerPlayer player) {
        CompoundTag relation = relationship(mob, player);
        return !FANREN.equals(archetypeOf(mob))
                && CultivationAccessoryItem.isLivingCorpse(player)
                && !DisguiseMaskItem.isWearing(player)
                || relation.getInt("Hatred") >= 100
                || relation.getBoolean("PermanentEnemy")
                || relation.getLong("RetaliateUntil") > mob.level().getGameTime();
    }

    public static void rememberPlayer(PathfinderMob mob, Player player) {
        ensure(mob, archetypeOf(mob));
        CompoundTag profile = profile(mob);
        CompoundTag knownPlayers = profile.getCompound("KnownPlayers");
        String key = player.getUUID().toString();
        CompoundTag memory = knownPlayers.getCompound(key);
        boolean firstMeeting = !memory.contains("MeetCount");
        memory.putString("Name", player.getName().getString());
        memory.putLong("LastMetGameTime", mob.level().getGameTime());
        memory.putInt("MeetCount", memory.getInt("MeetCount") + 1);
        knownPlayers.put(key, memory);
        profile.put("KnownPlayers", knownPlayers);

        CompoundTag relation = relationship(mob, player);
        if (firstMeeting && !relation.getBoolean("IdentityRelationshipBaseline")) {
            relation.putInt("Trust", 1);
        }
        saveRelationship(mob, player, relation);
    }

    private static CompoundTag relationship(PathfinderMob mob, Player player) {
        CompoundTag relationships = profile(mob).getCompound("Relationships");
        String key = player.getUUID().toString();
        CompoundTag relation = relationships.getCompound(key);
        if (!relation.contains("Initialized")) {
            RelationshipBaseline baseline = initialRelationship(
                    normalizeIdentity(profile(mob).getString("Identity")),
                    IdentityItems.ensurePlayerIdentity(player));
            relation.putBoolean("Initialized", true);
            relation.putInt("Friendship", baseline.friendship());
            relation.putInt("Trust", baseline.trust());
            relation.putInt("Affection", baseline.affection());
            relation.putInt("Hatred", baseline.hatred());
            relation.putBoolean("IdentityRelationshipBaseline", baseline.custom());
            relation.putInt("Fear", 0);
            relation.putInt("Grudge", 0);
            relation.putInt("QuestState", 0);
        }
        return relation;
    }

    private static RelationshipBaseline initialRelationship(String npcIdentity, String playerIdentity) {
        String npc = normalizeIdentity(npcIdentity);
        String player = normalizeIdentity(playerIdentity);
        if ("evil_cultivator".equals(npc) || "evil_cultivator".equals(player)) {
            return new RelationshipBaseline(0, -10, -10, 50, true);
        }
        if ("royal".equals(player)) {
            return "royal".equals(npc)
                    ? RelationshipBaseline.NORMAL
                    : new RelationshipBaseline(0, 5, 10, -5, true);
        }
        if ("clan_heir".equals(player)) {
            if ("clan_heir".equals(npc)) {
                return RelationshipBaseline.NORMAL;
            }
            if ("royal".equals(npc)) {
                return new RelationshipBaseline(0, 0, -5, 5, true);
            }
            return new RelationshipBaseline(0, 3, 5, -3, true);
        }
        if ("demonic_cultivator".equals(player)) {
            return new RelationshipBaseline(0, -10, -10, 50, true);
        }
        return switch (npc) {
            case "royal" -> "royal".equals(player)
                    ? RelationshipBaseline.NORMAL
                    : new RelationshipBaseline(0, -5, -10, 5, true);
            case "clan_heir" -> {
                if ("clan_heir".equals(player)) {
                    yield RelationshipBaseline.NORMAL;
                }
                if ("royal".equals(player)) {
                    yield new RelationshipBaseline(0, 0, 5, -5, true);
                }
                yield new RelationshipBaseline(0, -5, -10, 5, true);
            }
            case "wanted" -> "wanted".equals(player)
                    ? new RelationshipBaseline(0, -30, -10, 50, true)
                    : new RelationshipBaseline(0, -50, -10, 100, true);
            case "demonic_cultivator" -> new RelationshipBaseline(0, -10, -10, 50, true);
            default -> RelationshipBaseline.NORMAL;
        };
    }

    private static void saveRelationship(PathfinderMob mob, Player player, CompoundTag relation) {
        CompoundTag relationships = profile(mob).getCompound("Relationships");
        relationships.put(player.getUUID().toString(), relation);
        profile(mob).put("Relationships", relationships);
    }

    private static void changeRelationship(PathfinderMob mob, Player player, int friendship, int trust,
                                           int affection, int hatred) {
        CompoundTag relation = relationship(mob, player);
        applyRelationshipDelta(relation, friendship, trust, affection, hatred);
        saveRelationship(mob, player, relation);
    }

    private static void applyRelationshipDelta(CompoundTag relation, int friendship, int trust,
                                               int affection, int hatred) {
        relation.putInt("Friendship", clampRelation(relation.getInt("Friendship") + friendship));
        relation.putInt("Trust", clampRelation(relation.getInt("Trust") + trust));
        relation.putInt("Affection", clampRelation(relation.getInt("Affection") + affection));
        relation.putInt("Hatred", clampRelation(relation.getInt("Hatred") + hatred));
    }

    private static int clampRelation(int value) {
        return Mth.clamp(value, -100, 100);
    }

    public static void sendProfile(PathfinderMob mob, ServerPlayer player, boolean openMenu, String statusKey) {
        sendProfile(mob, player, openMenu, statusKey, false);
    }

    public static void openDebugRelationship(PathfinderMob mob, ServerPlayer player) {
        if (!player.hasPermissions(2) || mob.distanceToSqr(player) > 16.0D * 16.0D) {
            return;
        }
        sendProfile(mob, player, false, "", true);
    }

    private static void adjustDebugRelationship(PathfinderMob mob, ServerPlayer player, String stat, int delta) {
        if (!player.hasPermissions(2)
                || mob.distanceToSqr(player) > 16.0D * 16.0D
                || (!player.getMainHandItem().is(com.example.immortal_cultivation_mod.item.ModItems.DEBUG_STAT_EDITOR.get())
                && !player.getOffhandItem().is(com.example.immortal_cultivation_mod.item.ModItems.DEBUG_STAT_EDITOR.get()))) {
            return;
        }
        String key = switch (stat) {
            case "FRIENDSHIP" -> "Friendship";
            case "TRUST" -> "Trust";
            case "AFFECTION" -> "Affection";
            case "HATRED" -> "Hatred";
            default -> "";
        };
        if (key.isEmpty()) {
            return;
        }
        CompoundTag relation = relationship(mob, player);
        relation.putInt(key, clampRelation(relation.getInt(key) + Mth.clamp(delta, -100, 100)));
        saveRelationship(mob, player, relation);
        sendProfile(mob, player, false, "", true);
    }

    private static void sendProfile(PathfinderMob mob, ServerPlayer player, boolean openMenu, String statusKey,
                                    boolean debugRelationship) {
        ensure(mob, archetypeOf(mob));
        CompoundTag profile = profile(mob);
        CompoundTag relation = relationship(mob, player);
        QuestDef quest = quest(mob);
        if (relation.getInt("QuestState") != 0 && !quest.id().equals(relation.getString("QuestId"))) {
            relation.putInt("QuestState", 0);
            relation.remove("QuestId");
            saveRelationship(mob, player, relation);
        }
        ModAttachments.CultivationData playerData = ModAttachments.getData(player);
        CompoundTag data = new CompoundTag();
        data.putInt("EntityId", mob.getId());
        data.putBoolean("OpenMenu", openMenu);
        data.putBoolean("DebugRelationship", debugRelationship);
        data.putString("StatusKey", statusKey == null ? "" : statusKey);
        data.putString("Archetype", archetypeOf(mob));
        data.putString("Name", name(mob, archetypeOf(mob)));
        data.putString("Identity", profile.getString("Identity"));
        data.putString("Personality", profile.getString("Personality"));
        data.putString("Realm", profile.getString("Realm"));
        data.putString("BodyType", profile.getString("BodyType"));
        data.putString("Gender", profile.getString("Gender"));
        data.putInt("Age", profile.getInt("Age"));
        data.putInt("Reputation", profile.getInt("Reputation"));
        data.putInt("Qi", profile.getInt("Qi"));
        data.putInt("MaxQi", profile.getInt("MaxQi"));
        data.putInt("Thoughts", profile.getInt("Thoughts"));
        data.putInt("MaxThoughts", ModAttachments.MAX_THOUGHTS);
        data.putLong("CultivationProgress", profile.getLong("CultivationProgress"));
        data.putFloat("Health", mob.getHealth());
        data.putFloat("MaxHealth", mob.getMaxHealth());
        data.putFloat("Attack", (float) mob.getAttributeValue(Attributes.ATTACK_DAMAGE));
        data.putFloat("Armor", (float) mob.getAttributeValue(Attributes.ARMOR));
        data.put("SpiritRoots", profile.getList("SpiritRoots", 8).copy());
        data.putString("Method", profile.getString("Method"));
        data.put("Spells", profile.getList("Spells", 8).copy());
        data.put("PlayerSpells", stringList(playerData.knownSpells()));
        data.putString("PlayerMethod", playerData.activeCultivationMethod());
        data.put("PlayerMethods", stringList(learnedMethodIds(playerData)));
        data.putInt("Friendship", relation.getInt("Friendship"));
        data.putInt("Trust", relation.getInt("Trust"));
        data.putInt("Affection", relation.getInt("Affection"));
        data.putInt("Hatred", relation.getInt("Hatred"));
        boolean canUseDebugEye = player.hasPermissions(2);
        data.putInt("PraiseChance", canUseDebugEye
                ? socialSuccessChance(mob, player, SocialAction.PRAISE)
                : -1);
        data.putInt("JokeChance", canUseDebugEye
                ? socialSuccessChance(mob, player, SocialAction.JOKE)
                : -1);
        data.putBoolean("CanManageInventory", relation.getInt("Trust") >= INVENTORY_TRUST_REQUIRED
                && relation.getInt("Hatred") < 50);
        int methodLearningTrust = methodLearningTrustRequired(mob);
        List<String> npcSpells = spellIds(mob, archetypeOf(mob));
        CompoundTag spellTrustRequirements = new CompoundTag();
        for (String spellId : npcSpells) {
            spellTrustRequirements.putInt(spellId, spellLearningTrustRequired(mob, spellId));
        }
        data.put("SpellLearningTrustRequirements", spellTrustRequirements);
        data.putInt("MethodLearningTrustRequired", methodLearningTrust);
        data.putBoolean("CanTeach", relation.getInt("Trust") >= methodLearningTrust
                || npcSpells.stream().anyMatch(spellId -> relation.getInt("Trust")
                >= spellLearningTrustRequired(mob, spellId)));
        data.putString("CommandMode", commandMode(profile, player));
        data.putString("QuestId", quest.id());
        data.putInt("QuestState", relation.getInt("QuestState"));
        data.putString("QuestRequiredItem", quest.requiredItemId());
        data.putInt("QuestRequired", quest.requiredCount());
        data.putInt("QuestProgress", Math.min(quest.requiredCount(), countItem(player, item(quest.requiredItemId()))));
        data.putString("QuestRewardItem", quest.rewardItemId());
        data.putInt("QuestRewardCount", quest.rewardCount());
        data.putBoolean("QuestRewardAvailable",
                countInventoryItem(inventory(mob), item(quest.rewardItemId())) >= quest.rewardCount());
        data.put("Family", profile.getList("Family", 8).copy());

        ListTag inventoryData = new ListTag();
        List<ItemStack> inventory = inventory(mob);
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.get(i);
            CompoundTag slot = new CompoundTag();
            slot.putInt("Slot", i);
            if (!stack.isEmpty()) {
                slot.putString("Item", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
                slot.putInt("Count", stack.getCount());
            }
            inventoryData.add(slot);
        }
        data.put("Inventory", inventoryData);

        ListTag tradeData = new ListTag();
        List<TradeOffer> offers = tradeOffers(mob, player);
        for (int i = 0; i < offers.size(); i++) {
            TradeOffer offer = offers.get(i);
            CompoundTag encoded = new CompoundTag();
            encoded.putInt("OfferIndex", i);
            encoded.putInt("InventorySlot", offer.inventorySlot());
            encoded.putString("CurrencyItem", offer.currencyItemId());
            encoded.putInt("Price", offer.price());
            encoded.putString("ResultItem", offer.resultItemId());
            encoded.putInt("ResultCount", offer.resultCount());
            encoded.putInt("AvailableCount", offer.availableCount());
            tradeData.add(encoded);
        }
        data.put("TradeOffers", tradeData);
        PacketDistributor.sendToPlayer(player, new ModPayloads.ClientboundNpcProfilePayload(data));
    }

    private static String commandMode(CompoundTag profile, Player player) {
        if (profile.getBoolean("StayHere") && profile.hasUUID("CommandOwner")
                && profile.getUUID("CommandOwner").equals(player.getUUID())) {
            return "staying";
        }
        if (profile.hasUUID("FollowingPlayer") && profile.getUUID("FollowingPlayer").equals(player.getUUID())) {
            return "following";
        }
        return "none";
    }

    public static List<TradeOffer> tradeOffers(PathfinderMob mob) {
        return tradeOffers(mob, Integer.MAX_VALUE);
    }

    private static List<TradeOffer> tradeOffers(PathfinderMob mob, ServerPlayer player) {
        return tradeOffers(mob, relationshipValue(mob, player, "Trust"));
    }

    private static List<TradeOffer> tradeOffers(PathfinderMob mob, int trust) {
        List<ItemStack> inventory = inventory(mob);
        List<TradeOffer> offers = new ArrayList<>();
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.get(slot);
            if (stack.isEmpty()
                    || isCurrencyItem(stack.getItem())
                    || isIdentityProtectedTradeItem(mob, stack)
                    || requiresHighTrustTrade(stack) && trust < INVENTORY_TRUST_REQUIRED) {
                continue;
            }
            String resultId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            NpcTradePrice tradePrice = tradePriceFor(mob, stack);
            offers.add(new TradeOffer(slot, tradePrice.currencyItemId(), tradePrice.price(),
                    resultId, 1, stack.getCount()));
        }
        return List.copyOf(offers);
    }

    private static boolean requiresHighTrustTrade(ItemStack stack) {
        return stack.is(ModItems.ZHENHUN_BELL_YUJIAN.get())
                || stack.is(ModItems.YINHUN_GONG_YUJIAN.get())
                || stack.is(ModItems.FOG_REVEALING_MIRROR_YUJIAN.get())
                || stack.is(ModItems.YINYANG_COMPASS_YUJIAN.get())
                || stack.is(ModItems.YUANHUN_BOTTLE_YUJIAN.get());
    }

    private static boolean isIdentityProtectedTradeItem(PathfinderMob mob, ItemStack stack) {
        String identity = normalizeIdentity(profile(mob).getString("Identity"));
        return switch (identity) {
            case "body_cultivator" -> stack.is(ModItems.BODY_TEMPERING_PILL.get())
                    || stack.is(ModItems.HEAVY_IRON_BRACER.get());
            case "wanted" -> stack.is(ModItems.DISGUISE_MASK.get());
            case "sword_cultivator" -> stack.is(ModItems.FLYING_SWORD.get())
                    || stack.is(ModItems.SWORD_CASE.get());
            case "saber_cultivator" -> stack.is(ModItems.DAO.get());
            case "demonic_cultivator" -> isCultivationMethod(stack);
            case "artifact_cultivator" -> stack.is(ModBlocks.CU_TIE_XIAODING.get().asItem())
                    || stack.is(ModBlocks.QINGTONG_YINHUO_DING.get().asItem());
            case "corpse_cultivator" -> stack.is(ModItems.COFFIN.get())
                    || stack.is(ModItems.CORPSE_REFINING_SECRET.get())
                    || stack.is(ModItems.ZHENHUN_BELL.get())
                    || stack.is(ModItems.YINHUN_GONG.get());
            case "royal" -> stack.is(ModItems.ROYAL_TOKEN.get())
                    || stack.is(ModItems.PROTECTIVE_JADE_PENDANT.get())
                    || isCultivationMethod(stack);
            case "clan_heir" -> stack.is(ModItems.FAMILY_TOKEN.get());
            default -> false;
        };
    }

    private static boolean isCultivationMethod(ItemStack stack) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key != null
                && ImmortalCultivationMod.MODID.equals(key.getNamespace())
                && key.getPath().endsWith("_method");
    }

    private static QuestDef quest(PathfinderMob mob) {
        List<QuestDef> quests = questPool(mob);
        List<ItemStack> inventory = inventory(mob);
        List<QuestDef> fundedQuests = quests.stream()
                .filter(quest -> countInventoryItem(inventory, item(quest.rewardItemId())) >= quest.rewardCount())
                .toList();
        if (fundedQuests.isEmpty()) {
            return quests.getFirst();
        }
        String identity = normalizeIdentity(profile(mob).getString("Identity"));
        int seed = Integer.rotateLeft(mob.getUUID().hashCode(), 13) ^ identity.hashCode();
        return fundedQuests.get(Math.floorMod(seed, fundedQuests.size()));
    }

    private static List<QuestDef> questPool(PathfinderMob mob) {
        if (FANREN.equals(archetypeOf(mob))) {
            return List.of(new QuestDef(
                    "fanren_village_supplies", "minecraft:wheat", 12, COPPER_COIN, 6));
        }
        String identity = normalizeIdentity(profile(mob).getString("Identity"));
        if ("sword_cultivator".equals(identity)
                || "saber_cultivator".equals(identity)) {
            return weaponCareQuestPool(mob, identity);
        }
        return IDENTITY_QUESTS.getOrDefault(
                identity, IDENTITY_QUESTS.get("wandering_cultivator"));
    }

    private static List<QuestDef> weaponCareQuestPool(
            PathfinderMob mob, String identity) {
        String mod = ImmortalCultivationMod.MODID + ":";
        int tier = weaponCareTier(mob);
        String currency = tier >= 3
                ? HIGH_SPIRIT_STONE
                : tier == 2 ? MEDIUM_SPIRIT_STONE : LOW_SPIRIT_STONE;
        String nurturingMaterial = tier >= 3
                ? mod + "star_dust_stone"
                : tier == 2
                        ? mod + "yunwen_silver_crystal"
                        : "minecraft:amethyst_shard";
        int nurturingCount = tier >= 3 ? 3 : tier == 2 ? 2 : 4;
        String sharpeningMaterial = tier >= 2
                ? mod + "jingtie_motherstone" : "minecraft:flint";
        int sharpeningCount = tier >= 3 ? 6 : tier == 2 ? 2 : 8;

        if ("sword_cultivator".equals(identity)) {
            return List.of(
                    new QuestDef(
                            "sword_nourishment",
                            nurturingMaterial,
                            nurturingCount,
                            tieredItemId("yangjian_stone", tier),
                            1),
                    new QuestDef(
                            "sword_sharpening",
                            sharpeningMaterial,
                            sharpeningCount,
                            tieredItemId("sword_grindstone", tier),
                            1),
                    new QuestDef(
                            "sword_forging_materials",
                            mod + "jingtie_motherstone",
                            Math.max(1, tier * 2),
                            currency,
                            tier >= 3 ? 5 : tier == 2 ? 9 : 12)
            );
        }
        return List.of(
                new QuestDef(
                        "saber_sharpening",
                        sharpeningMaterial,
                        sharpeningCount,
                        tieredItemId("saber_grindstone", tier),
                        1),
                new QuestDef(
                        "saber_forging_materials",
                        mod + "jingtie_motherstone",
                        Math.max(1, tier * 2),
                        currency,
                        tier >= 3 ? 5 : tier == 2 ? 9 : 12),
                new QuestDef(
                        "saber_spiritual_tempering",
                        mod + "yunwen_silver_crystal",
                        Math.max(1, tier * 2),
                        currency,
                        tier >= 3 ? 6 : tier == 2 ? 11 : 14)
        );
    }

    private static int weaponCareTier(PathfinderMob mob) {
        String archetype = archetypeOf(mob);
        return JINDAN.equals(archetype)
                ? 3 : ZHUJI.equals(archetype) ? 2 : 1;
    }

    private static Map<String, List<QuestDef>> createIdentityQuests() {
        String mod = ImmortalCultivationMod.MODID + ":";
        return Map.ofEntries(
                Map.entry("wandering_cultivator", List.of(
                        new QuestDef("wandering_spirit_herb", mod + "qingling_grass", 8, LOW_SPIRIT_STONE, 11),
                        new QuestDef("wandering_alchemy_materials", mod + "chiyan_flower", 4, LOW_SPIRIT_STONE, 11),
                        new QuestDef("wandering_spiritual_medicine", mod + "ninglu_zhi", 3, LOW_SPIRIT_STONE, 12)
                )),
                Map.entry("sword_cultivator", List.of(
                        new QuestDef("sword_nourishment", "minecraft:amethyst_shard", 4, mod + "low_yangjian_stone", 1),
                        new QuestDef("sword_sharpening", "minecraft:flint", 8, mod + "low_sword_grindstone", 1),
                        new QuestDef("sword_forging_materials", mod + "jingtie_motherstone", 2, LOW_SPIRIT_STONE, 12)
                )),
                Map.entry("saber_cultivator", List.of(
                        new QuestDef("saber_sharpening", "minecraft:flint", 8, mod + "low_saber_grindstone", 1),
                        new QuestDef("saber_forging_materials", mod + "jingtie_motherstone", 2, LOW_SPIRIT_STONE, 12),
                        new QuestDef("saber_spiritual_tempering", mod + "yunwen_silver_crystal", 2, LOW_SPIRIT_STONE, 14)
                )),
                Map.entry("demonic_cultivator", List.of(
                        new QuestDef("demonic_blood_crystal", mod + "blood_crystal", 5, LOW_SPIRIT_STONE, 20),
                        new QuestDef("demonic_ritual", mod + "blood_crystal", 8, LOW_SPIRIT_STONE, 32),
                        new QuestDef("demonic_secret_trade", mod + "lethal_poison", 3, LOW_SPIRIT_STONE, 9)
                )),
                Map.entry("artifact_cultivator", List.of(
                        new QuestDef("artifact_refined_iron", mod + "jingtie_motherstone", 3, MEDIUM_SPIRIT_STONE, 12),
                        new QuestDef("artifact_cloud_silver", mod + "yunwen_silver_crystal", 3, MEDIUM_SPIRIT_STONE, 16),
                        new QuestDef("artifact_spirit_scale", mod + "lingjiao_scale", 2, MEDIUM_SPIRIT_STONE, 14)
                )),
                Map.entry("corpse_cultivator", List.of(
                        new QuestDef("corpse_suppress_qi", mod + "zhenshi_talisman", 3, LOW_SPIRIT_STONE, 30),
                        new QuestDef("corpse_ritual", mod + "blood_crystal", 6, LOW_SPIRIT_STONE, 24),
                        new QuestDef("corpse_refining_material", mod + "lethal_poison", 2, LOW_SPIRIT_STONE, 6)
                )),
                Map.entry("body_cultivator", List.of(
                        new QuestDef("body_blood_qi", mod + "blood_qi_pill", 3, MEDIUM_SPIRIT_STONE, 12),
                        new QuestDef("body_tempering", mod + "blood_crystal", 6, LOW_SPIRIT_STONE, 24),
                        new QuestDef("body_spiritual_medicine", mod + "jinwen_fruit", 3, LOW_SPIRIT_STONE, 16)
                )),
                Map.entry("clan_heir", List.of(
                        new QuestDef("clan_medicine", mod + "jinwen_fruit", 4, LOW_SPIRIT_STONE, 22),
                        new QuestDef("clan_rare_materials", mod + "lingjiao_scale", 2, MEDIUM_SPIRIT_STONE, 14),
                        new QuestDef("clan_commission", mod + "yunwen_silver_crystal", 2, MEDIUM_SPIRIT_STONE, 11)
                )),
                Map.entry("royal", List.of(
                        new QuestDef("royal_medicine", mod + "xuanbing_lotus", 3, LOW_SPIRIT_STONE, 25),
                        new QuestDef("royal_treasure", mod + "lingjiao_scale", 2, MEDIUM_SPIRIT_STONE, 15),
                        new QuestDef("royal_artifact", mod + "yunwen_silver_crystal", 4, MEDIUM_SPIRIT_STONE, 24)
                )),
                Map.entry("wanted", List.of(
                        new QuestDef("wanted_poison", mod + "lethal_poison", 4, LOW_SPIRIT_STONE, 12),
                        new QuestDef("wanted_blood_crystal", mod + "blood_crystal", 8, LOW_SPIRIT_STONE, 34),
                        new QuestDef("wanted_destroy_evidence", mod + "wanted_order", 3, LOW_SPIRIT_STONE, 5)
                ))
        );
    }

    private static void ensureFirstQuestRewardStock(PathfinderMob mob, CompoundTag profile) {
        QuestDef firstQuest = questPool(mob).getFirst();
        Item reward = item(firstQuest.rewardItemId());
        if (reward == Items.AIR) {
            return;
        }
        List<ItemStack> inventory = inventory(mob);
        int stocked = countInventoryItem(inventory, reward);
        boolean changed = false;
        while (stocked < firstQuest.rewardCount()) {
            int amount = Math.min(firstQuest.rewardCount() - stocked, reward.getDefaultMaxStackSize());
            insertInventory(inventory, new ItemStack(reward, amount));
            int updated = countInventoryItem(inventory, reward);
            if (updated <= stocked) {
                break;
            }
            changed = true;
            stocked = updated;
        }
        if (changed) {
            writeInventory(mob, profile, inventory);
        }
        profile.remove("QuestRewardStockedId");
    }

    private static boolean isCurrencyItem(Item item) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
        return key != null && ((ImmortalCultivationMod.MODID.equals(key.getNamespace())
                && "copper_coin".equals(key.getPath()))
                || ("immortal_cultivation_mod".equals(key.getNamespace())
                && ("spirit_stone".equals(key.getPath())
                || "medium_spirit_stone".equals(key.getPath())
                || "high_spirit_stone".equals(key.getPath()))));
    }

    private static NpcTradePrice tradePriceFor(PathfinderMob mob, ItemStack stack) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        NpcTradePrice listedPrice = key == null ? null : NPC_TRADE_PRICES.get(key.toString());
        if (listedPrice == null && key != null
                && ImmortalCultivationMod.MODID.equals(key.getNamespace())) {
            return systemShopTradePrice(mob, stack);
        }
        int price;
        if (listedPrice != null) {
            price = listedPrice.price();
        } else {
            price = switch (CultivationItemRarity.of(stack)) {
                case COMMON -> 2;
                case UNCOMMON -> 4;
                case RARE -> 8;
                case EPIC -> 16;
            };
            var food = stack.get(DataComponents.FOOD);
            if (food != null) {
                price = Math.max(price, Math.max(1, (food.nutrition() + 1) / 3));
            }
        }

        if (FANREN.equals(archetypeOf(mob))) {
            return new NpcTradePrice(COPPER_COIN, Mth.clamp(price, 1, 64));
        }
        if (listedPrice != null) {
            return listedPrice;
        }

        String currency = switch (CultivationItemRarity.of(stack)) {
            case COMMON -> COPPER_COIN;
            case UNCOMMON -> LOW_SPIRIT_STONE;
            case RARE -> MEDIUM_SPIRIT_STONE;
            case EPIC -> HIGH_SPIRIT_STONE;
        };
        return new NpcTradePrice(currency, Mth.clamp(price, 1, 64));
    }

    /**
     * Keeps newly introduced mod items in NPC inventories synchronized with the
     * canonical system-shop price list without requiring a second hard-coded
     * price entry. One trade uses the largest denomination that represents the
     * system value exactly.
     */
    private static NpcTradePrice systemShopTradePrice(PathfinderMob mob, ItemStack stack) {
        int systemPrice = Math.max(1, SystemShopPrices.price(stack));
        if (FANREN.equals(archetypeOf(mob))) {
            return new NpcTradePrice(COPPER_COIN, systemPrice);
        }
        if (systemPrice >= 1_000 && systemPrice % 1_000 == 0) {
            return new NpcTradePrice(HIGH_SPIRIT_STONE, systemPrice / 1_000);
        }
        if (systemPrice >= 100 && systemPrice % 100 == 0) {
            return new NpcTradePrice(MEDIUM_SPIRIT_STONE, systemPrice / 100);
        }
        if (systemPrice >= 10 && systemPrice % 10 == 0) {
            return new NpcTradePrice(LOW_SPIRIT_STONE, systemPrice / 10);
        }
        return new NpcTradePrice(COPPER_COIN, systemPrice);
    }

    public static int systemPointPrice(ItemStack stack) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (key == null) {
            return 0;
        }
        String itemId = key.toString();
        if (LOW_SPIRIT_STONE.equals(itemId)) {
            return 1;
        }
        if (MEDIUM_SPIRIT_STONE.equals(itemId)) {
            return 10;
        }
        if (HIGH_SPIRIT_STONE.equals(itemId)) {
            return 100;
        }
        if (COPPER_COIN.equals(itemId)) {
            return 1;
        }

        boolean vanilla = "minecraft".equals(key.getNamespace());
        if (vanilla
                && stack.has(DataComponents.FOOD)
                && !stack.is(Items.GOLDEN_APPLE)
                && !stack.is(Items.ENCHANTED_GOLDEN_APPLE)) {
            return 3;
        }

        NpcTradePrice listed = NPC_TRADE_PRICES.get(itemId);
        if (listed != null) {
            int listedPrice = Mth.clamp(
                    listed.price() * systemCurrencyValue(listed.currencyItemId()),
                    1,
                    1_000_000);
            if (!vanilla) {
                return listedPrice;
            }
            int comparablePrice = benchmarkSystemPointPrice(stack, key);
            return comparablePrice <= 0
                    ? listedPrice
                    : Math.min(listedPrice, discountedVanillaPrice(comparablePrice));
        }

        int benchmarkPrice = benchmarkSystemPointPrice(stack, key);
        if (benchmarkPrice > 0) {
            int price = vanilla
                    ? discountedVanillaPrice(benchmarkPrice)
                    : benchmarkPrice;
            if (vanilla && stack.has(DataComponents.FOOD)) {
                price = Math.max(3, price);
            }
            return price;
        }

        // Registry entries should always find a benchmark, but retain a stable
        // fallback for datapack or compatibility items with unusual metadata.
        int fallback = Mth.clamp(
                CultivationItemRarity.giftValue(stack), 1, 1_000_000);
        return vanilla ? discountedVanillaPrice(fallback) : fallback;
    }

    private static int discountedVanillaPrice(int comparableModPrice) {
        if (comparableModPrice <= 1) {
            return 1;
        }
        return Math.max(1, Math.min(comparableModPrice - 1, comparableModPrice / 2));
    }

    private static int benchmarkSystemPointPrice(ItemStack stack, ResourceLocation key) {
        int itemLevel = CultivationItemRarity.giftValue(stack);
        String group = systemPriceGroup(stack, key);
        List<SystemPriceBenchmark> benchmarks = systemPriceBenchmarks();

        int price = benchmarkPrice(benchmarks, itemLevel, group, true, true, true);
        if (price <= 0) {
            price = benchmarkPrice(benchmarks, itemLevel, group, false, true, true);
        }
        if (price <= 0) {
            price = benchmarkPrice(benchmarks, itemLevel, group, true, false, true);
        }
        if (price <= 0) {
            price = benchmarkPrice(benchmarks, itemLevel, group, false, false, true);
        }
        if (price <= 0) {
            price = benchmarkPrice(benchmarks, itemLevel, group, true, true, false);
        }
        if (price <= 0) {
            price = benchmarkPrice(benchmarks, itemLevel, group, false, true, false);
        }
        if (price <= 0) {
            price = benchmarkPrice(benchmarks, itemLevel, group, true, false, false);
        }
        if (price <= 0) {
            price = benchmarkPrice(benchmarks, itemLevel, group, false, false, false);
        }
        return Mth.clamp(price, 0, 1_000_000);
    }

    private static int benchmarkPrice(List<SystemPriceBenchmark> benchmarks, int itemLevel,
                                      String group, boolean sameGroup, boolean exactLevel,
                                      boolean modOnly) {
        int closestDistance = Integer.MAX_VALUE;
        List<Integer> prices = new ArrayList<>();
        for (SystemPriceBenchmark benchmark : benchmarks) {
            if (modOnly && !benchmark.modItem()) {
                continue;
            }
            if (sameGroup && !group.equals(benchmark.group())) {
                continue;
            }
            int distance = Math.abs(itemLevel - benchmark.itemLevel());
            if (exactLevel && distance != 0) {
                continue;
            }
            if (distance < closestDistance) {
                closestDistance = distance;
                prices.clear();
            }
            if (distance == closestDistance) {
                prices.add(benchmark.price());
            }
        }
        if (prices.isEmpty()) {
            return 0;
        }
        prices.sort(Integer::compareTo);
        int middle = prices.size() / 2;
        if ((prices.size() & 1) == 1) {
            return prices.get(middle);
        }
        return (int) (((long) prices.get(middle - 1) + prices.get(middle)) / 2L);
    }

    private static List<SystemPriceBenchmark> systemPriceBenchmarks() {
        List<SystemPriceBenchmark> current = systemPriceBenchmarks;
        if (current != null) {
            return current;
        }
        synchronized (NpcProfile.class) {
            if (systemPriceBenchmarks == null) {
                List<SystemPriceBenchmark> created = new ArrayList<>();
                for (Map.Entry<String, NpcTradePrice> entry : NPC_TRADE_PRICES.entrySet()) {
                    ResourceLocation id = ResourceLocation.tryParse(entry.getKey());
                    if (id == null) {
                        continue;
                    }
                    Item item = BuiltInRegistries.ITEM.get(id);
                    if (item == Items.AIR) {
                        continue;
                    }
                    ItemStack benchmarkStack = item.getDefaultInstance();
                    NpcTradePrice listed = entry.getValue();
                    int price = Mth.clamp(
                            listed.price() * systemCurrencyValue(listed.currencyItemId()),
                            1,
                            1_000_000);
                    created.add(new SystemPriceBenchmark(
                            CultivationItemRarity.giftValue(benchmarkStack),
                            systemPriceGroup(benchmarkStack, id),
                            price,
                            ImmortalCultivationMod.MODID.equals(id.getNamespace())));
                }
                systemPriceBenchmarks = List.copyOf(created);
            }
            return systemPriceBenchmarks;
        }
    }

    private static String systemPriceGroup(ItemStack stack, ResourceLocation key) {
        String path = key.getPath();
        if (isPillPath(path)) {
            return "pill";
        }
        if (path.endsWith("_scroll") || path.endsWith("_method") || path.contains("manual")) {
            return "spell";
        }
        if (stack.getItem() instanceof BlockItem) {
            return "block";
        }
        if (stack.getItem() instanceof ArmorItem || stack.isDamageableItem()) {
            return "weapon";
        }
        return "material";
    }

    private static boolean isPillPath(String path) {
        return path.equals("pill")
                || path.endsWith("_pill")
                || path.contains("potion")
                || path.contains("elixir");
    }

    private static int systemCurrencyValue(String currencyItemId) {
        if (MEDIUM_SPIRIT_STONE.equals(currencyItemId)) {
            return 10;
        }
        if (HIGH_SPIRIT_STONE.equals(currencyItemId)) {
            return 100;
        }
        return 1;
    }

    static List<ItemStack> inventory(PathfinderMob mob) {
        List<ItemStack> inventory = new ArrayList<>(INVENTORY_SIZE);
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            inventory.add(ItemStack.EMPTY);
        }
        ListTag list = profile(mob).getList("Inventory", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            int slot = entry.getInt("Slot");
            if (slot >= 0 && slot < INVENTORY_SIZE && entry.contains("Stack")) {
                inventory.set(slot, ItemStack.parseOptional(mob.registryAccess(), entry.getCompound("Stack")));
            }
        }
        return inventory;
    }

    static void writeInventory(PathfinderMob mob, CompoundTag profile, List<ItemStack> inventory) {
        ListTag list = new ListTag();
        for (int i = 0; i < Math.min(INVENTORY_SIZE, inventory.size()); i++) {
            ItemStack stack = inventory.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            CompoundTag entry = new CompoundTag();
            entry.putInt("Slot", i);
            entry.put("Stack", stack.saveOptional(mob.registryAccess()));
            list.add(entry);
        }
        profile.put("Inventory", list);
    }

    private static boolean insertInventory(List<ItemStack> inventory, ItemStack stack) {
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack existing = inventory.get(i);
            if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, stack)
                    && existing.getCount() < existing.getMaxStackSize()) {
                int moved = Math.min(stack.getCount(), existing.getMaxStackSize() - existing.getCount());
                existing.grow(moved);
                stack.shrink(moved);
                if (stack.isEmpty()) {
                    return true;
                }
            }
        }
        for (int i = 0; i < inventory.size(); i++) {
            if (inventory.get(i).isEmpty()) {
                inventory.set(i, stack.copy());
                return true;
            }
        }
        return false;
    }

    private static int countInventoryItem(List<ItemStack> inventory, Item item) {
        if (item == Items.AIR) {
            return 0;
        }
        int count = 0;
        for (ItemStack stack : inventory) {
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void removeInventoryItems(List<ItemStack> inventory, Item item, int count) {
        int remaining = count;
        for (int i = 0; i < inventory.size() && remaining > 0; i++) {
            ItemStack stack = inventory.get(i);
            if (!stack.is(item)) {
                continue;
            }
            int removed = Math.min(remaining, stack.getCount());
            stack.shrink(removed);
            remaining -= removed;
            if (stack.isEmpty()) {
                inventory.set(i, ItemStack.EMPTY);
            }
        }
    }

    private static boolean chance(Random random, int percent) {
        return random.nextInt(100) < Mth.clamp(percent, 0, 100);
    }

    private static int addInventoryItem(List<ItemStack> inventory, int slot, ItemStack stack) {
        if (slot >= inventory.size() || stack.isEmpty()) {
            return slot;
        }
        inventory.set(slot, stack);
        return slot + 1;
    }

    private static void mergeMissingDefaultInventory(PathfinderMob mob, CompoundTag profile, String archetype) {
        List<ItemStack> inventory = inventory(mob);
        int seed = mob.getUUID().hashCode();
        for (ItemStack defaultStack : defaultInventory(archetype, profile.getString("Identity"), seed)) {
            if (!defaultStack.isEmpty()
                    && countInventoryItem(inventory, defaultStack.getItem()) == 0) {
                insertInventory(inventory, defaultStack.copy());
            }
        }
        writeInventory(mob, profile, inventory);
    }

    private static void mergeMissingCorpseCultivatorItems(
            PathfinderMob mob, CompoundTag profile, int seed) {
        List<ItemStack> inventory = inventory(mob);
        if (countInventoryItem(inventory, ModItems.ZHENHUN_BELL.get()) == 0) {
            insertInventory(inventory, new ItemStack(ModItems.ZHENHUN_BELL.get(), 1));
        }
        if (countInventoryItem(inventory, ModItems.YINHUN_GONG.get()) == 0) {
            insertInventory(inventory, new ItemStack(ModItems.YINHUN_GONG.get(), 1));
        }
        if (hasRareCorpseForgingSlip(seed, true)
                && countInventoryItem(inventory, ModItems.ZHENHUN_BELL_YUJIAN.get()) == 0) {
            insertInventory(inventory, new ItemStack(ModItems.ZHENHUN_BELL_YUJIAN.get(), 1));
        }
        if (hasRareCorpseForgingSlip(seed, false)
                && countInventoryItem(inventory, ModItems.YINHUN_GONG_YUJIAN.get()) == 0) {
            insertInventory(inventory, new ItemStack(ModItems.YINHUN_GONG_YUJIAN.get(), 1));
        }
        writeInventory(mob, profile, inventory);
    }

    private static boolean hasRareCorpseForgingSlip(int seed, boolean bell) {
        long salt = bell ? 0x5A48454E48554E4CL : 0x59494E48554E474CL;
        return hasRareForgingSlip(seed, salt);
    }

    private static void mergeMissingSpecialistForgingSlip(
            PathfinderMob mob, CompoundTag profile, int seed) {
        Item slip;
        long salt;
        switch (normalizeIdentity(profile.getString("Identity"))) {
            case "artifact_cultivator" -> {
                slip = ModItems.FOG_REVEALING_MIRROR_YUJIAN.get();
                salt = FOG_MIRROR_RECIPE_SALT;
            }
            case "formation_cultivator" -> {
                slip = ModItems.YINYANG_COMPASS_YUJIAN.get();
                salt = YINYANG_COMPASS_RECIPE_SALT;
            }
            case "evil_cultivator" -> {
                slip = ModItems.YUANHUN_BOTTLE_YUJIAN.get();
                salt = SOUL_BOTTLE_RECIPE_SALT;
            }
            default -> {
                return;
            }
        }
        if (!hasRareForgingSlip(seed, salt)) {
            return;
        }
        List<ItemStack> inventory = inventory(mob);
        if (countInventoryItem(inventory, slip) == 0) {
            insertInventory(inventory, new ItemStack(slip, 1));
            writeInventory(mob, profile, inventory);
        }
    }

    private static boolean hasRareForgingSlip(int seed, long salt) {
        return new Random(((long) seed << 32) ^ salt).nextInt(100) < 2;
    }

    private static List<ItemStack> defaultInventory(String archetype, String identity, int seed) {
        List<ItemStack> inventory = new ArrayList<>();
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            inventory.add(ItemStack.EMPTY);
        }
        Random random = new Random(seed);
        int slot = 0;

        switch (archetype) {
            case JINDAN -> {
                slot = addInventoryItem(
                        inventory,
                        slot,
                        new ItemStack(item(HIGH_SPIRIT_STONE), random.nextInt(2, 5))
                );

                slot = addInventoryItem(
                        inventory,
                        slot,
                        new ItemStack(item(MEDIUM_SPIRIT_STONE), random.nextInt(8, 18))
                );

                if (chance(random, 85)) {
                    slot = addInventoryItem(
                            inventory,
                            slot,
                            new ItemStack(
                                    item(ImmortalCultivationMod.MODID + ":breakthrough_pill"),
                                    random.nextInt(1, 3)
                            )
                    );
                }

                if (chance(random, 65)) {
                    slot = addInventoryItem(
                            inventory,
                            slot,
                            new ItemStack(
                                    item(ImmortalCultivationMod.MODID + ":xuanbing_lotus"),
                                    random.nextInt(1, 4)
                            )
                    );
                }

                if (chance(random, 70)) {
                    slot = addInventoryItem(
                            inventory,
                            slot,
                            new ItemStack(
                                    item(ImmortalCultivationMod.MODID + ":jinwen_fruit"),
                                    random.nextInt(2, 6)
                            )
                    );
                }

                if (chance(random, 60)) {
                    slot = addInventoryItem(
                            inventory,
                            slot,
                            new ItemStack(
                                    item(ImmortalCultivationMod.MODID + ":ninglu_zhi"),
                                    random.nextInt(2, 6)
                            )
                    );
                }

                if (chance(random, 55)) {
                    slot = addInventoryItem(
                            inventory,
                            slot,
                            new ItemStack(
                                    item(ImmortalCultivationMod.MODID + ":jingtie_motherstone"),
                                    random.nextInt(2, 6)
                            )
                    );
                }

                if (chance(random, 35)) {
                    slot = addInventoryItem(
                            inventory,
                            slot,
                            new ItemStack(
                                    item(ImmortalCultivationMod.MODID + ":yunwen_silver_crystal"),
                                    random.nextInt(1, 4)
                            )
                    );
                }

            }

            case ZHUJI -> {
                slot = addInventoryItem(
                        inventory,
                        slot,
                        new ItemStack(item(MEDIUM_SPIRIT_STONE), random.nextInt(4, 8))
                );

                slot = addInventoryItem(
                        inventory,
                        slot,
                        new ItemStack(item(LOW_SPIRIT_STONE), random.nextInt(16, 33))
                );

                if (chance(random, 25)) {
                    slot = addInventoryItem(
                            inventory,
                            slot,
                            new ItemStack(
                                    item(ImmortalCultivationMod.MODID + ":breakthrough_pill"),
                                    1
                            )
                    );
                }

                if (chance(random, 80)) {
                    slot = addInventoryItem(
                            inventory,
                            slot,
                            new ItemStack(
                                    item(ImmortalCultivationMod.MODID + ":qingling_grass"),
                                    random.nextInt(4, 10)
                            )
                    );
                }

                if (chance(random, 65)) {
                    slot = addInventoryItem(
                            inventory,
                            slot,
                            new ItemStack(
                                    item(ImmortalCultivationMod.MODID + ":chiyan_flower"),
                                    random.nextInt(2, 7)
                            )
                    );
                }

                if (chance(random, 45)) {
                    slot = addInventoryItem(
                            inventory,
                            slot,
                            new ItemStack(
                                    item(ImmortalCultivationMod.MODID + ":ninglu_zhi"),
                                    random.nextInt(1, 4)
                            )
                    );
                }

                if (chance(random, 40)) {
                    slot = addInventoryItem(
                            inventory,
                            slot,
                            new ItemStack(
                                    item(ImmortalCultivationMod.MODID + ":jingtie_motherstone"),
                                    random.nextInt(1, 4)
                            )
                    );
                }

                if (chance(random, 20)) {
                    slot = addInventoryItem(
                            inventory,
                            slot,
                            new ItemStack(
                                    item(ImmortalCultivationMod.MODID + ":lingjiao_scale"),
                                    random.nextInt(2, 6)
                            )
                    );
                }
            }

            case LIANQI -> {
                slot = addInventoryItem(
                        inventory,
                        slot,
                        new ItemStack(item(LOW_SPIRIT_STONE), random.nextInt(8, 17))
                );

                if (chance(random, 65)) {
                    slot = addInventoryItem(
                            inventory,
                            slot,
                            new ItemStack(
                                    item(ImmortalCultivationMod.MODID + ":qingling_grass"),
                                    random.nextInt(2, 7)
                            )
                    );
                }

                if (chance(random, 40)) {
                    slot = addInventoryItem(
                            inventory,
                            slot,
                            new ItemStack(
                                    item(ImmortalCultivationMod.MODID + ":chiyan_flower"),
                                    random.nextInt(1, 4)
                            )
                    );
                }

                if (chance(random, 45)) {
                    slot = addInventoryItem(
                            inventory,
                            slot,
                            new ItemStack(item(ImmortalCultivationMod.MODID + ":bigu_pill"), random.nextInt(1, 3))
                    );
                }

                if (chance(random, 35)) {
                    addInventoryItem(
                            inventory,
                            slot,
                            new ItemStack(item(ImmortalCultivationMod.MODID + ":low_qi_recovery_pill"), 1)
                    );
                }
            }

            default -> {
                slot = addInventoryItem(
                        inventory,
                        slot,
                        new ItemStack(item(COPPER_COIN), random.nextInt(24, 49))
                );

                if (chance(random, 45)) {
                    slot = addInventoryItem(
                            inventory,
                            slot,
                            new ItemStack(Items.BREAD, random.nextInt(1, 5))
                    );
                }

                if (chance(random, 35)) {
                    slot = addInventoryItem(
                            inventory,
                            slot,
                            new ItemStack(Items.WHEAT, random.nextInt(2, 8))
                    );
                }

                if (chance(random, 70)) {
                    slot = addInventoryItem(
                            inventory,
                            slot,
                            new ItemStack(
                                    item(ImmortalCultivationMod.MODID + ":qingling_grass"),
                                    random.nextInt(2, 7)
                            )
                    );
                }

                if (chance(random, 35)) {
                    slot = addInventoryItem(
                            inventory,
                            slot,
                            new ItemStack(
                                    item(ImmortalCultivationMod.MODID + ":chiyan_flower"),
                                    random.nextInt(1, 4)
                            )
                    );
                }

                if (chance(random, 3)) {
                    addInventoryItem(
                            inventory,
                            slot,
                            new ItemStack(
                                    item(ImmortalCultivationMod.MODID + ":fireball_scroll"),
                                    1
                            )
                    );
                }
            }
        }

        addIdentityLoadout(inventory, archetype, identity, seed);

        return inventory;
    }

    private static List<ItemStack> hotelkeeperInventory(int seed) {
        List<ItemStack> inventory = new ArrayList<>();
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            inventory.add(ItemStack.EMPTY);
        }
        Random random = new Random(seed ^ 0x484F54454C4CL);
        insertInventory(inventory, new ItemStack(
                ModItems.COPPER_COIN.get(), random.nextInt(24, 49)));
        insertInventory(inventory, new ItemStack(
                ModItems.WINE.get(), random.nextInt(10, 17)));
        insertInventory(inventory, new ItemStack(
                ModItems.STEAMED_BUN.get(), random.nextInt(16, 25)));
        insertInventory(inventory, new ItemStack(
                ModItems.NOODLE_BOWL.get(), random.nextInt(8, 14)));
        insertInventory(inventory, new ItemStack(
                ModItems.BRAISED_PORK.get(), random.nextInt(6, 12)));
        insertInventory(inventory, new ItemStack(
                ModItems.ROAST_DUCK.get(), random.nextInt(4, 10)));
        insertInventory(inventory, new ItemStack(
                Items.BREAD, random.nextInt(8, 14)));
        insertInventory(inventory, new ItemStack(
                Items.COOKED_CHICKEN, random.nextInt(6, 11)));
        insertInventory(inventory, new ItemStack(
                Items.BAKED_POTATO, random.nextInt(8, 15)));
        return inventory;
    }

    private static void addIdentityLoadout(List<ItemStack> inventory, String archetype, String identity, int seed) {
        if (FANREN.equals(archetype)) {
            return;
        }
        int tier = JINDAN.equals(archetype) ? 3 : ZHUJI.equals(archetype) ? 2 : 1;
        int amount = tier == 3 ? 4 : tier == 2 ? 2 : 1;
        String spiritStone = tier == 3 ? HIGH_SPIRIT_STONE : tier == 2 ? MEDIUM_SPIRIT_STONE : LOW_SPIRIT_STONE;
        String qiPill = tieredItemId("qi_recovery_pill", tier);
        String yangjian = tieredItemId("yangjian_stone", tier);
        String swordStone = tieredItemId("sword_grindstone", tier);
        String saberStone = tieredItemId("saber_grindstone", tier);

        switch (identity) {
            case "wandering_cultivator" -> {
                insertInventory(inventory, new ItemStack(item(spiritStone), 8 * amount));
                insertInventory(inventory, new ItemStack(item(ImmortalCultivationMod.MODID + ":bigu_pill"), 2 * amount));
                insertInventory(inventory, new ItemStack(item(qiPill), amount));
                insertInventory(inventory, new ItemStack(item(ImmortalCultivationMod.MODID
                        + (tier == 3 ? ":clear_heart_method" : ":basic_breathing_method")), 1));
            }
            case "sword_cultivator" -> {
                insertInventory(inventory, npcForgedWeapon(false, tier, seed));
                insertInventory(inventory, new ItemStack(item(yangjian), 1));
                insertInventory(inventory, new ItemStack(item(swordStone), 1));
                insertInventory(inventory, new ItemStack(item(qiPill), amount));
            }
            case "saber_cultivator" -> {
                insertInventory(inventory, npcForgedWeapon(true, tier, seed));
                insertInventory(inventory, new ItemStack(item(saberStone), 1));
                insertInventory(inventory, new ItemStack(item(qiPill), amount));
            }
            case "demonic_cultivator" -> {
                insertInventory(inventory, new ItemStack(item(ImmortalCultivationMod.MODID + ":blood_crystal"), 3 * amount));
                insertInventory(inventory, new ItemStack(item(ImmortalCultivationMod.MODID + ":blood_demon_jindan_method"), 1));
                insertInventory(inventory, new ItemStack(item(qiPill), amount));
            }
            case "evil_cultivator" -> {
                insertInventory(inventory, new ItemStack(Items.BONE, 4 * amount));
                insertInventory(inventory, new ItemStack(ModItems.YUANHUN_BOTTLE.get(), 1));
                if (hasRareForgingSlip(seed, SOUL_BOTTLE_RECIPE_SALT)) {
                    insertInventory(inventory, new ItemStack(ModItems.YUANHUN_BOTTLE_YUJIAN.get(), 1));
                }
                insertInventory(inventory, evilCultivatorBanner(defaultRealm(archetype, seed)));
            }
            case "artifact_cultivator" -> {
                insertInventory(inventory, new ItemStack(ModBlocks.QINGTONG_YINHUO_DING.get().asItem(), 1));
                insertInventory(inventory, new ItemStack(ModBlocks.JINGTIE_MOTHERSTONE.get().asItem(), 3 * amount));
                insertInventory(inventory, new ItemStack(Items.IRON_INGOT, 8 * amount));
                insertInventory(inventory, new ItemStack(tier == 3 ? ModItems.DAO_YUJIAN.get() : ModItems.FEIJIAN_YUJIAN.get(), 1));
                if (hasRareForgingSlip(seed, FOG_MIRROR_RECIPE_SALT)) {
                    insertInventory(inventory, new ItemStack(ModItems.FOG_REVEALING_MIRROR_YUJIAN.get(), 1));
                }
                insertInventory(inventory, npcForgedMold(seed, tier));
            }
            case "corpse_cultivator" -> {
                insertInventory(inventory, new ItemStack(ModItems.ZHENSHI_TALISMAN.get(), 2 * amount));
                insertInventory(inventory, new ItemStack(ModItems.COFFIN.get(), 1));
                insertInventory(inventory, new ItemStack(ModItems.ZHENHUN_BELL.get(), 1));
                insertInventory(inventory, new ItemStack(ModItems.YINHUN_GONG.get(), 1));
                if (hasRareCorpseForgingSlip(seed, true)) {
                    insertInventory(inventory, new ItemStack(ModItems.ZHENHUN_BELL_YUJIAN.get(), 1));
                }
                if (hasRareCorpseForgingSlip(seed, false)) {
                    insertInventory(inventory, new ItemStack(ModItems.YINHUN_GONG_YUJIAN.get(), 1));
                }
                insertInventory(inventory, new ItemStack(ModItems.BLOOD_CRYSTAL.get(), 2 * amount));
                if (Math.floorMod(seed, 100) < 2) {
                    insertInventory(inventory, new ItemStack(ModItems.CORPSE_REFINING_SECRET.get(), 1));
                }
                insertInventory(inventory, new ItemStack(ModItems.KONGSHI_SHU_SCROLL.get(), 1));
            }
            case "body_cultivator" -> {
                insertInventory(inventory, new ItemStack(ModItems.HEAVY_IRON_BRACER.get(), 1));
                insertInventory(inventory, new ItemStack(ModItems.BODY_TEMPERING_PILL.get(), 1));
                insertInventory(inventory, new ItemStack(ModItems.BLOOD_QI_PILL.get(), amount));
                if (Math.floorMod(seed, 100) < 2) {
                    insertInventory(inventory, new ItemStack(ModItems.CUIGU_LIANSHEN_METHOD.get(), 1));
                }
            }
            case "talisman_cultivator" -> {
                Random talismanRandom = new Random(
                        ((long) seed << 32) ^ tier ^ 0x46555849554CL);
                insertInventory(inventory, new ItemStack(talismanPaper(tier), 16));
                insertInventory(inventory, new ItemStack(talismanInk(tier), 16));
                insertInventory(inventory, npcFinishedTalisman(
                        randomAttackTalisman(talismanRandom, tier), 16, tier));
                insertInventory(inventory, npcFinishedTalisman(
                        randomDefenseTalisman(talismanRandom, tier), 8, tier));
            }
            case "formation_cultivator" -> {
                Random formationRandom = new Random(
                        ((long) seed << 32) ^ tier ^ 0x5A48454E5855L);
                insertInventory(inventory, new ItemStack(
                        com.example.immortal_cultivation_mod.magicritual.item.ModItems.ZHENPAN.get(), 1));
                insertInventory(inventory, new ItemStack(
                        randomFormationFlag(formationRandom), 1));
                if (hasRareForgingSlip(seed, YINYANG_COMPASS_RECIPE_SALT)) {
                    insertInventory(inventory, new ItemStack(ModItems.YINYANG_COMPASS_YUJIAN.get(), 1));
                }
                insertInventory(inventory, new ItemStack(item(spiritStone), 16));
                insertInventory(inventory, ServerEvents.createMagicRitualGuide());
                insertInventory(inventory, new ItemStack(
                        com.example.immortal_cultivation_mod.magicritual.item.ModItems.RITUAL_DAGGER.get(), 1));
            }
            case "clan_heir" -> {
                insertInventory(inventory, new ItemStack(item(spiritStone), 16 * amount));
                insertInventory(inventory, new ItemStack(ModItems.FAMILY_TOKEN.get(), 1));
                insertInventory(inventory, npcForgedWeapon((seed & 1) == 0, tier, seed + 31));
                if (chance(new Random(seed + 1997), 60)) {
                    insertInventory(inventory, new ItemStack(ModItems.QI_POUCH.get(), 1));
                }
                insertInventory(inventory, new ItemStack(tier == 3
                        ? ModItems.POKONG_JUE_METHOD.get() : ModItems.CLEAR_HEART_METHOD.get(), 1));
            }
            case "royal" -> {
                insertInventory(inventory, new ItemStack(ModItems.ROYAL_TOKEN.get(), 1));
                insertInventory(inventory, new ItemStack(Items.GOLD_INGOT, 12 * amount));
                insertInventory(inventory, new ItemStack(Items.IRON_INGOT, 20 * amount));
                insertInventory(inventory, new ItemStack(ModItems.HIGH_QI_RECOVERY_PILL.get(), 3 * amount));
                insertInventory(inventory, npcForgedWeapon((seed & 1) == 0, 3, seed + 73));
                insertInventory(inventory, new ItemStack(ModItems.PROTECTIVE_JADE_PENDANT.get(), 1));
                insertInventory(inventory, new ItemStack(ModItems.POKONG_JUE_METHOD.get(), 1));
            }
            case "wanted" -> {
                insertInventory(inventory, new ItemStack(ModItems.DISGUISE_MASK.get(), 1));
                insertInventory(inventory, new ItemStack(Items.GOLD_INGOT, 3 * amount));
                insertInventory(inventory, new ItemStack(Items.EMERALD, 2 * amount));
                insertInventory(inventory, new ItemStack(ModItems.LETHAL_POISON.get(), amount));
                insertInventory(inventory, wantedWeapon(seed));
                insertInventory(inventory, WantedOrderItem.create(
                        chineseName(seed, 0, selectSkin(archetype, seed).gender()),
                        selectSkin(archetype, seed).texture(),
                        defaultRealm(archetype, seed),
                        spiritStone,
                        tier == 3 ? 16 : tier == 2 ? 24 : 32));
            }
            default -> {
            }
        }
    }

    private static Item talismanPaper(int tier) {
        return switch (tier) {
            case 3 -> ModItems.RED_VEINED_MULBERRY_PAPER.get();
            case 2 -> ModItems.LINGSANG_TALISMAN_PAPER.get();
            default -> ModItems.ORDINARY_YELLOW_TALISMAN_PAPER.get();
        };
    }

    private static Item talismanInk(int tier) {
        return switch (tier) {
            case 3 -> ModItems.MYSTERIOUS_SPIRIT_INK.get();
            case 2 -> ModItems.SPIRITUAL_CINNABAR_INK.get();
            default -> ModItems.MUNDANE_CINNABAR_INK.get();
        };
    }

    private static Item randomAttackTalisman(Random random, int tier) {
        List<Item> pool = new ArrayList<>(List.of(
                ModItems.FIREBALL_TALISMAN.get(),
                ModItems.FENGREN_TALISMAN.get()));
        if (tier >= 2) {
            pool.add(ModItems.XINGHUO_TALISMAN.get());
            pool.add(ModItems.CHANSI_KUN_TALISMAN.get());
        }
        if (tier >= 3) {
            pool.add(ModItems.BAOFENG_TALISMAN.get());
            pool.add(ModItems.WULEI_XIAOFA_TALISMAN.get());
            pool.add(ModItems.HUOYU_FENXIE_TALISMAN.get());
        }
        return pool.get(random.nextInt(pool.size()));
    }

    private static Item randomDefenseTalisman(Random random, int tier) {
        List<Item> pool = new ArrayList<>(List.of(
                ModItems.LIANSHENG_TALISMAN.get()));
        if (tier >= 2) {
            pool.add(ModItems.BOJIN_HUFU.get());
            pool.add(ModItems.DUANTU_SHENXING_TALISMAN.get());
        }
        if (tier >= 3) {
            pool.add(ModItems.BIXIE_ANHUN_TALISMAN.get());
            pool.add(ModItems.ZHENRAO_TALISMAN.get());
        }
        return pool.get(random.nextInt(pool.size()));
    }

    private static ItemStack npcFinishedTalisman(Item item, int count, int tier) {
        ItemStack stack = new ItemStack(item, count);
        TalismanItem.setQuality(stack, switch (tier) {
            case 3 -> TalismanItem.Quality.HIGH;
            case 2 -> TalismanItem.Quality.MID;
            default -> TalismanItem.Quality.LOW;
        });
        return stack;
    }

    private static Item randomFormationFlag(Random random) {
        List<Item> flags = List.of(
                com.example.immortal_cultivation_mod.magicritual.item.ModItems.DURATION_FLAG.get(),
                com.example.immortal_cultivation_mod.magicritual.item.ModItems.AMPLIFIER_FLAG.get(),
                com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_GATHERING_FLAG.get(),
                com.example.immortal_cultivation_mod.magicritual.item.ModItems.HASTE_FLAG.get(),
                com.example.immortal_cultivation_mod.magicritual.item.ModItems.QI_RETURN_FLAG.get());
        return flags.get(random.nextInt(flags.size()));
    }

    private static String tieredItemId(String suffix, int tier) {
        String prefix = tier >= 3 ? "high" : tier == 2 ? "middle" : "low";
        return ImmortalCultivationMod.MODID + ":" + prefix + "_" + suffix;
    }

    private static ItemStack npcForgedMold(int seed, int tier) {
        boolean saber = (seed & 1) == 0;
        Random random = new Random(seed * 31L + tier * 7_919L);
        ForgingSystem.ForgingRecipe recipe = ForgingSystem.recipeById(saber ? "dao" : "flying_sword");
        List<ItemStack> materials = new ArrayList<>();
        for (var required : recipe.required().entrySet()) {
            ItemStack stack = new ItemStack(required.getKey(), required.getValue());
            ForgingSystem.ensurePurity(stack, random.nextInt());
            materials.add(stack);
        }

        List<ForgingSystem.MaterialDef> bonusPool = new ArrayList<>(ForgingSystem.materials());
        int bonusTypes = tier >= 3 ? 2 + random.nextInt(3) : 1 + random.nextInt(2);
        for (int i = 0; i < bonusTypes && !bonusPool.isEmpty(); i++) {
            ForgingSystem.MaterialDef bonusDef = bonusPool.remove(random.nextInt(bonusPool.size()));
            int count = 1 + random.nextInt(tier >= 3 ? 3 : 2);
            ItemStack bonus = new ItemStack(bonusDef.item(), count);
            ForgingSystem.ensurePurity(bonus, random.nextInt());
            materials.add(bonus);
        }

        double temperature = recipe.averageTemp() + random.nextInt(1_201) - 600;
        return ForgingSystem.createMold(recipe, materials, temperature, tier >= 3 ? "ling_fire" : "normal_fire");
    }

    private static ItemStack npcForgedWeapon(boolean saber, int tier, int seed) {
        ItemStack mold = npcForgedMold(saber ? seed & ~1 : seed | 1, tier);
        ItemStack weapon = ForgingSystem.temper(mold);
        return weapon.isEmpty() ? new ItemStack(saber ? ModItems.DAO.get() : ModItems.FLYING_SWORD.get()) : weapon;
    }

    private static ItemStack wantedWeapon(int seed) {
        return switch (Math.floorMod(seed, 4)) {
            case 0 -> npcForgedWeapon(false, 2, seed + 11);
            case 1 -> npcForgedWeapon(true, 2, seed + 17);
            case 2 -> new ItemStack(ModItems.TAOMU_SWORD.get());
            default -> new ItemStack(ModItems.COPPER_COIN_SWORD.get());
        };
    }

    private static void refreshWantedOrder(PathfinderMob mob, CompoundTag profile) {
        if (!"wanted".equals(profile.getString("Identity"))) {
            return;
        }
        ItemStack order = createWantedOrder(mob);
        List<ItemStack> inventory = inventory(mob);
        boolean replaced = false;
        for (int i = 0; i < inventory.size(); i++) {
            if (inventory.get(i).is(ModItems.WANTED_ORDER.get())) {
                inventory.set(i, order.copy());
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            insertInventory(inventory, order);
        }
        writeInventory(mob, profile, inventory);
    }

    public static ItemStack createWantedOrder(PathfinderMob mob) {
        CompoundTag profile = profile(mob);
        WantedReward reward = wantedReward(profile.getString("Realm"));
        return WantedOrderItem.create(
                profile.getString("Name"),
                profile.getString("Skin"),
                profile.getString("Realm"),
                reward.itemId(),
                reward.count());
    }

    public static void grantWantedReward(PathfinderMob mob, ServerPlayer player) {
        if (!(mob instanceof CultivationNpc npc) || !"wanted".equals(identity(mob, npc.npcArchetype()))) {
            return;
        }
        WantedReward wantedReward = wantedReward(profile(mob).getString("Realm"));
        ItemStack reward = new ItemStack(item(wantedReward.itemId()), wantedReward.count());
        giveToPlayer(player, reward);
        player.displayClientMessage(Component.translatable(
                "message." + ImmortalCultivationMod.MODID + ".wanted_reward",
                reward.getCount(), reward.getHoverName()), false);
    }

    private static WantedReward wantedReward(String realm) {
        CultivationLevels.LevelDef level = CultivationLevels.getLevelDef(realm);
        return switch (level.realm()) {
            case CultivationLevels.REALM_DUJIE, CultivationLevels.REALM_DACHENG,
                 CultivationLevels.REALM_HETI, CultivationLevels.REALM_LIANXU,
                 CultivationLevels.REALM_HUASHEN -> new WantedReward(HIGH_SPIRIT_STONE, 64);
            case CultivationLevels.REALM_YUANYING -> new WantedReward(HIGH_SPIRIT_STONE, 32);
            case CultivationLevels.REALM_JINDAN -> new WantedReward(HIGH_SPIRIT_STONE, 16);
            case CultivationLevels.REALM_ZHUJI -> new WantedReward(MEDIUM_SPIRIT_STONE, 24);
            case CultivationLevels.REALM_LIANQI -> new WantedReward(LOW_SPIRIT_STONE, 48);
            default -> new WantedReward(LOW_SPIRIT_STONE, 16);
        };
    }

    public static ItemStack randomInventoryDrop(PathfinderMob mob) {
        ensure(mob, archetypeOf(mob));
        syncEquippedCombatWeapon(mob);
        if (mob.level().isClientSide || mob.getRandom().nextFloat() >= NPC_INVENTORY_DROP_CHANCE) {
            return ItemStack.EMPTY;
        }
        List<ItemStack> available = inventory(mob).stream().filter(stack -> !stack.isEmpty()).toList();
        if (available.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack source = available.get(mob.getRandom().nextInt(available.size()));
        int count = source.getMaxStackSize() == 1
                ? 1
                : 1 + mob.getRandom().nextInt(Math.min(3, source.getCount()));
        return source.copyWithCount(count);
    }

    private static ItemStack heldStack(ServerPlayer player) {
        if (!player.getMainHandItem().isEmpty()) {
            return player.getMainHandItem();
        }
        return player.getOffhandItem();
    }

    private static int countItem(ServerPlayer player, Item item) {
        if (item == Items.AIR) {
            return 0;
        }
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) count += stack.getCount();
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.is(item)) count += stack.getCount();
        }
        return count;
    }

    private static void removeItems(ServerPlayer player, Item item, int count) {
        int remaining = count;
        List<ItemStack> stacks = new ArrayList<>(player.getInventory().items);
        stacks.addAll(player.getInventory().offhand);
        for (ItemStack stack : stacks) {
            if (remaining <= 0) break;
            if (stack.is(item)) {
                int removed = Math.min(remaining, stack.getCount());
                stack.shrink(removed);
                remaining -= removed;
            }
        }
    }

    private static void giveToPlayer(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private static void giveItemsToPlayer(ServerPlayer player, Item item, int count) {
        if (item == Items.AIR || count <= 0) {
            return;
        }
        int remaining = count;
        while (remaining > 0) {
            int amount = Math.min(remaining, item.getDefaultMaxStackSize());
            giveToPlayer(player, new ItemStack(item, amount));
            remaining -= amount;
        }
    }

    private static Item item(String id) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        return key == null ? Items.AIR : BuiltInRegistries.ITEM.get(key);
    }

    public static void save(PathfinderMob mob, CompoundTag tag) {
        syncEquippedCombatWeapon(mob);
        tag.put(TAG, profile(mob).copy());
    }

    public static void load(PathfinderMob mob, CompoundTag tag) {
        if (tag.contains(TAG)) {
            mob.getPersistentData().put(TAG, tag.getCompound(TAG).copy());
        }
    }

    public static String name(PathfinderMob mob, String archetype) {
        ensure(mob, archetype);
        if (mob.hasCustomName()) {
            return mob.getName().getString();
        }
        return profile(mob).getString("Name");
    }

    public static String gender(PathfinderMob mob, String archetype) {
        ensure(mob, archetype);
        return normalizeGender(profile(mob).getString("Gender"));
    }

    public static ResourceLocation skinTexture(PathfinderMob mob, String archetype) {
        ensure(mob, archetype);
        String skin = mob instanceof CultivationNpc npc ? npc.getNpcSkinId() : "";
        if (skin == null || skin.isBlank()) {
            skin = profile(mob).getString("Skin");
        }
        if (skin.isBlank()) {
            skin = selectSkin(archetype, mob.getUUID().hashCode()).texture();
        }
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID,
                "textures/entity/" + skin + ".png");
    }

    public static String identity(PathfinderMob mob, String archetype) {
        ensure(mob, archetype);
        return profile(mob).getString("Identity");
    }

    public static String personality(PathfinderMob mob, String archetype) {
        ensure(mob, archetype);
        return normalizePersonality(profile(mob).getString("Personality"));
    }

    public static String realm(PathfinderMob mob, String archetype) {
        ensure(mob, archetype);
        return profile(mob).getString("Realm");
    }

    public static int qi(PathfinderMob mob, String archetype) {
        ensure(mob, archetype);
        return profile(mob).getInt("Qi");
    }

    public static int maxQi(PathfinderMob mob, String archetype) {
        ensure(mob, archetype);
        return profile(mob).getInt("MaxQi");
    }

    public static int thoughts(PathfinderMob mob, String archetype) {
        ensure(mob, archetype);
        return profile(mob).getInt("Thoughts");
    }

    public static int maxThoughts(PathfinderMob mob, String archetype) {
        ensure(mob, archetype);
        return ModAttachments.MAX_THOUGHTS;
    }

    public static boolean spendQi(PathfinderMob mob, String archetype, int amount) {
        ensure(mob, archetype);
        CompoundTag profile = profile(mob);
        int cost = Math.max(0, amount);
        int currentQi = profile.getInt("Qi");
        if (currentQi < cost) {
            return false;
        }
        profile.putInt("Qi", currentQi - cost);
        if (mob.hasEffect(ModEffects.SPIRIT_SEAL)
                && cost * 4 >= Math.max(1, profile.getInt("MaxQi"))) {
            mob.removeEffect(ModEffects.SPIRIT_SEAL);
        }
        return true;
    }

    public static void restoreQi(PathfinderMob mob, String archetype, int amount) {
        ensure(mob, archetype);
        CompoundTag profile = profile(mob);
        profile.putInt("Qi", Math.min(profile.getInt("MaxQi"),
                profile.getInt("Qi") + Math.max(0, amount)));
    }

    public static void drainQi(PathfinderMob mob, String archetype, int amount) {
        ensure(mob, archetype);
        CompoundTag profile = profile(mob);
        profile.putInt("Qi", Math.max(0, profile.getInt("Qi") - Math.max(0, amount)));
    }

    public static void adjustThoughts(PathfinderMob mob, String archetype, int delta) {
        ensure(mob, archetype);
        CompoundTag profile = profile(mob);
        int updated = Mth.clamp(profile.getInt("Thoughts") + delta, 0, ModAttachments.MAX_THOUGHTS);
        profile.putInt("Thoughts", updated);
        if (updated <= 0) {
            mob.addEffect(new MobEffectInstance(ModEffects.DAZE, 40, 0, false, true, true));
        }
    }

    public static long cultivationProgress(PathfinderMob mob, String archetype) {
        ensure(mob, archetype);
        return profile(mob).getLong("CultivationProgress");
    }

    public static void restoreCultivationProgress(PathfinderMob mob, String archetype, long amount) {
        ensure(mob, archetype);
        CompoundTag data = profile(mob);
        long maximum = CultivationLevels.getTotalQiNeeded(data.getString("Realm"));
        data.putLong("CultivationProgress", Math.min(maximum,
                data.getLong("CultivationProgress") + Math.max(0, amount)));
    }

    public static String spiritRootsDisplay(PathfinderMob mob, String archetype) {
        ensure(mob, archetype);
        return String.join("/", strings(profile(mob).getList("SpiritRoots", 8)));
    }

    public static List<String> spiritRootIds(PathfinderMob mob, String archetype) {
        ensure(mob, archetype);
        return List.copyOf(strings(profile(mob).getList("SpiritRoots", 8)));
    }

    public static String methodDisplay(PathfinderMob mob, String archetype) {
        ensure(mob, archetype);
        String method = profile(mob).getString("Method");
        if (method.isBlank()) {
            return "None";
        }
        var def = CultivationMethods.get(method);
        return def == null ? method : Component.translatable(def.nameKey()).getString();
    }

    public static String methodId(PathfinderMob mob, String archetype) {
        ensure(mob, archetype);
        return profile(mob).getString("Method");
    }

    public static List<String> spells(PathfinderMob mob, String archetype) {
        List<String> names = new ArrayList<>();
        for (String spellId : spellIds(mob, archetype)) {
            var def = ModSpells.get(spellId);
            names.add(def == null ? spellId : def.name().getString());
        }
        return names;
    }

    public static List<String> learnedSpellIds(PathfinderMob mob, String archetype) {
        return List.copyOf(spellIds(mob, archetype));
    }

    public static ItemStack combatWeapon(PathfinderMob mob) {
        ensure(mob, archetypeOf(mob));
        String identity = normalizeIdentity(profile(mob).getString("Identity"));
        List<ItemStack> candidates = inventory(mob).stream()
                .filter(NpcProfile::isStoredCombatWeapon)
                .toList();
        if (candidates.isEmpty()) {
            return ItemStack.EMPTY;
        }

        List<ItemStack> preferred = switch (identity) {
            case "body_cultivator" -> candidates.stream()
                    .filter(stack -> stack.is(ModItems.HEAVY_IRON_BRACER.get()))
                    .toList();
            case "sword_cultivator" -> candidates.stream()
                    .filter(NpcProfile::isSwordWeapon)
                    .toList();
            case "saber_cultivator" -> candidates.stream()
                    .filter(NpcProfile::isSaberWeapon)
                    .toList();
            default -> candidates;
        };
        if ("body_cultivator".equals(identity) && preferred.isEmpty()) {
            return ItemStack.EMPTY;
        }
        List<ItemStack> pool = preferred.isEmpty() ? candidates : preferred;
        ItemStack best = pool.stream()
                .max(Comparator.comparingDouble(QiInfusedWeapon::baseAttackDamage)
                        .thenComparing(NpcProfile::itemId))
                .orElse(ItemStack.EMPTY);
        if (best.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack copy = best.copy();
        copy.setCount(1);
        return copy;
    }

    public static void equipCombatWeapon(PathfinderMob mob) {
        ensure(mob, archetypeOf(mob));
        syncEquippedCombatWeapon(mob);
        ItemStack equipped = mob.getMainHandItem();
        if (!equipped.isEmpty()) {
            mob.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
            return;
        }
        ItemStack selected = combatWeapon(mob);
        if (!selected.isEmpty()) {
            List<ItemStack> stored = inventory(mob);
            int slot = findStoredWeaponSlot(stored, selected);
            if (slot >= 0) {
                profile(mob).putInt(EQUIPPED_WEAPON_SLOT_TAG, slot);
                mob.setItemSlot(EquipmentSlot.MAINHAND, selected.copy());
            }
        }
        // The equipped stack mirrors stored profile inventory; never drop both copies.
        mob.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    static void syncEquippedCombatWeapon(PathfinderMob mob) {
        ItemStack equipped = mob.getMainHandItem();
        CompoundTag npcProfile = profile(mob);
        List<ItemStack> stored = inventory(mob);
        if (npcProfile.contains(EQUIPPED_WEAPON_SLOT_TAG)) {
            int slot = npcProfile.getInt(EQUIPPED_WEAPON_SLOT_TAG);
            if (slot < 0 || slot >= stored.size()) {
                npcProfile.remove(EQUIPPED_WEAPON_SLOT_TAG);
            } else if (equipped.isEmpty()) {
                // Empty live hand means the tracked weapon broke or was consumed; delete its
                // profile mirror so it cannot be resurrected on the next decision.
                stored.set(slot, ItemStack.EMPTY);
                writeInventory(mob, npcProfile, stored);
                npcProfile.remove(EQUIPPED_WEAPON_SLOT_TAG);
                return;
            } else if (isStoredCombatWeapon(equipped)) {
                ItemStack liveCopy = equipped.copy();
                liveCopy.setCount(Math.max(1, stored.get(slot).getCount()));
                stored.set(slot, liveCopy);
                writeInventory(mob, npcProfile, stored);
                return;
            }
        }
        if (equipped.isEmpty() || !isStoredCombatWeapon(equipped)) {
            return;
        }
        ItemStack selected = combatWeapon(mob);
        int slot = findStoredWeaponSlot(stored, selected);
        if (slot < 0) {
            for (int index = 0; index < stored.size(); index++) {
                if (!stored.get(index).isEmpty() && stored.get(index).is(equipped.getItem())) {
                    slot = index;
                    break;
                }
            }
        }
        if (slot >= 0) {
            npcProfile.putInt(EQUIPPED_WEAPON_SLOT_TAG, slot);
            ItemStack liveCopy = equipped.copy();
            liveCopy.setCount(Math.max(1, stored.get(slot).getCount()));
            stored.set(slot, liveCopy);
            writeInventory(mob, npcProfile, stored);
        }
    }

    static boolean consumeEquippedArtifact(PathfinderMob mob) {
        syncEquippedCombatWeapon(mob);
        var p=profile(mob);
        if (!p.contains(EQUIPPED_WEAPON_SLOT_TAG)||mob.getMainHandItem().isEmpty())return false;
        var stored=inventory(mob);int slot=p.getInt(EQUIPPED_WEAPON_SLOT_TAG);
        if(slot<0||slot>=stored.size()||stored.get(slot).isEmpty())return false;
        stored.get(slot).shrink(1);writeInventory(mob,p,stored);
        p.remove(EQUIPPED_WEAPON_SLOT_TAG);mob.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);
        return true;
    }

    private static int findStoredWeaponSlot(List<ItemStack> stored, ItemStack selected) {
        if (selected.isEmpty()) {
            return -1;
        }
        for (int index = 0; index < stored.size(); index++) {
            ItemStack candidate = stored.get(index);
            ItemStack one = candidate.copy();
            one.setCount(1);
            if (ItemStack.isSameItemSameComponents(one, selected)) {
                return index;
            }
        }
        return -1;
    }

    private static boolean isStoredCombatWeapon(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.is(ModItems.SHIHUNFAN.get())
                || QiInfusedWeapon.isWeapon(stack) && !(stack.getItem() instanceof DiggerItem));
    }

    private static boolean isSwordWeapon(ItemStack stack) {
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return !isSaberWeapon(stack)
                && (path.contains("sword") || path.contains("jian") || path.contains("feijian"));
    }

    private static boolean isSaberWeapon(ItemStack stack) {
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return path.equals("dao") || path.contains("saber") || path.contains("blade");
    }

    private static String itemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    public static boolean knowsSpell(PathfinderMob mob, String archetype, String spellId) {
        String normalized = ModSpells.normalizeId(spellId);
        return spellIds(mob, archetype).stream()
                .map(ModSpells::normalizeId)
                .anyMatch(normalized::equals);
    }

    public static boolean tryUseSoulBanner(PathfinderMob mob, LivingEntity target) {
        if (!"evil_cultivator".equals(identity(mob, archetypeOf(mob)))) {
            return false;
        }
        ItemStack equipped = mob.getMainHandItem();
        if (equipped.is(ModItems.SHIHUNFAN.get())) {
            return ShihunfanItem.fireNpcBeam(mob, target, equipped);
        }
        List<ItemStack> inventory = inventory(mob);
        for (ItemStack stack : inventory) {
            if (stack.is(ModItems.SHIHUNFAN.get())
                    && ShihunfanItem.fireNpcBeam(mob, target, stack)) {
                writeInventory(mob, profile(mob), inventory);
                return true;
            }
        }
        return false;
    }

    public static boolean isTalismanCultivator(PathfinderMob mob) {
        return "talisman_cultivator".equals(
                identity(mob, archetypeOf(mob)));
    }

    /**
     * Uses and consumes one real offensive talisman from the NPC's stored
     * inventory. The item itself launches the same registered projectile used
     * by players, so all damage, impact effects, particles, and ownership rules
     * remain authoritative.
     */
    public static boolean tryUseAttackTalisman(
            PathfinderMob mob, LivingEntity target) {
        return tryUseAttackTalisman(mob,target,false);
    }

    public static boolean tryUseAttackTalisman(PathfinderMob mob,LivingEntity target,boolean emergency) {
        if (!emergency&&!isTalismanCultivator(mob)) {
            return false;
        }
        List<ItemStack> inventory = inventory(mob);
        if (inventory.isEmpty())return false;
        int start = mob.getRandom().nextInt(inventory.size());
        for (int offset = 0; offset < inventory.size(); offset++) {
            int slot = (start + offset) % inventory.size();
            ItemStack stack = inventory.get(slot);
            if (!(stack.getItem() instanceof AttackTalismanItem talisman)
                    || !talisman.launchForNpc(mob, target)) {
                continue;
            }
            stack.shrink(1);
            if (stack.isEmpty()) {
                inventory.set(slot, ItemStack.EMPTY);
            }
            writeInventory(mob, profile(mob), inventory);
            return true;
        }
        return false;
    }

    private static List<String> spellIds(PathfinderMob mob, String archetype) {
        ensure(mob, archetype);
        return new ArrayList<>(strings(profile(mob).getList("Spells", 8)));
    }

    public static int relationshipValue(PathfinderMob mob, Player player, String value) {
        ensure(mob, archetypeOf(mob));
        return relationship(mob, player).getInt(value);
    }

    public static int relationshipValue(PathfinderMob mob, String value) {
        ensure(mob, archetypeOf(mob));
        CompoundTag relationships = profile(mob).getCompound("Relationships");
        if (relationships.isEmpty()) {
            return 0;
        }
        for (String key : relationships.getAllKeys()) {
            CompoundTag relation = relationships.getCompound(key);
            if (relation.contains(value)) {
                return relation.getInt(value);
            }
        }
        return 0;
    }

    public static float fleeHealthBonus(PathfinderMob mob) {
        return switch (personality(mob, archetypeOf(mob))) {
            case "cautious", "scholarly" -> 0.12F;
            case "aggressive", "proud" -> -0.08F;
            default -> 0.0F;
        };
    }

    public static double chaseSpeedMultiplier(PathfinderMob mob) {
        return switch (personality(mob, archetypeOf(mob))) {
            case "aggressive", "proud" -> 1.15D;
            case "cautious" -> 0.9D;
            default -> 1.0D;
        };
    }

    private static CompoundTag profile(PathfinderMob mob) {
        CompoundTag data = mob.getPersistentData();
        if (!data.contains(TAG)) {
            data.put(TAG, new CompoundTag());
        }
        return data.getCompound(TAG);
    }

    private static String archetypeOf(PathfinderMob mob) {
        if (mob instanceof CultivationNpc npc) {
            return npc.npcArchetype();
        }
        return FANREN;
    }

    private static String selectIdentity(String archetype, int seed) {
        if (FANREN.equals(archetype)) {
            return "village_mortal";
        }
        return CULTIVATOR_IDENTITIES.get(Math.floorMod(seed / 43, CULTIVATOR_IDENTITIES.size()));
    }

    public static String normalizeIdentity(String identity) {
        if (identity == null || identity.isBlank()) {
            return "";
        }

        String normalized = identity.trim();
        String translationPrefix = "screen." + ImmortalCultivationMod.MODID + ".npc.identity.";
        if (normalized.startsWith(translationPrefix)) {
            normalized = normalized.substring(translationPrefix.length());
        }
        normalized = normalized.toLowerCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_');

        return switch (normalized) {
            case "散修", "wandering", "wandering_cultivator" -> "wandering_cultivator";
            case "剑修", "sword_cultivator" -> "sword_cultivator";
            case "刀修", "saber_cultivator", "blade_cultivator" -> "saber_cultivator";
            case "魔修", "demonic_cultivator" -> "demonic_cultivator";
            case "邪修", "evil_cultivator" -> "evil_cultivator";
            case "器修", "artifact_cultivator", "artifact_refiner" -> "artifact_cultivator";
            case "尸修", "corpse_cultivator" -> "corpse_cultivator";
            case "体修", "body_cultivator" -> "body_cultivator";
            case "符修", "talisman_cultivator", "talisman_master" -> "talisman_cultivator";
            case "阵修", "formation_cultivator", "formation_master" -> "formation_cultivator";
            case "世家子弟", "clan_heir" -> "clan_heir";
            case "皇族", "royal", "royalty" -> "royal";
            case "通缉犯", "wanted", "wanted_criminal" -> "wanted";
            case "凡人", "村中凡人", "mortal", "village_resident", "village_mortal" -> "village_mortal";
            case "掌柜", "客栈掌柜", "hotelkeeper", "innkeeper" -> "hotelkeeper";
            case "sect_zhuji", "wandering_jindan" -> normalized;
            default -> "";
        };
    }

    private static ItemStack evilCultivatorBanner(String realm) {
        ItemStack stack = new ItemStack(ModItems.SHIHUNFAN.get());
        int tier = Math.max(0, Math.min(
                WeaponRestrictionSystem.MAX_TIER,
                CultivationLevels.getRealmIndex(realm) - 2));
        WeaponRestrictionSystem.setTier(stack, tier);
        ShihunfanItem.setSoulAmount(stack, ShihunfanItem.maxSoulCapacity(stack) * 0.5D);
        return stack;
    }

    private static String defaultBodyType(String archetype) {
        return switch (archetype) {
            case JINDAN -> "玄灵体";
            case ZHUJI -> "清灵体";
            case LIANQI -> "\u521d\u7075\u4f53";
            default -> "凡体";
        };
    }

    private static String defaultRealm(String archetype, int seed) {
        String stage = switch (Math.floorMod(seed / 97, 3)) {
            case 0 -> CultivationLevels.STAGE_EARLY;
            case 1 -> CultivationLevels.STAGE_MID;
            default -> CultivationLevels.STAGE_LATE;
        };
        return switch (archetype) {
            case JINDAN -> CultivationLevels.REALM_JINDAN + stage;
            case ZHUJI -> CultivationLevels.REALM_ZHUJI + stage;
            case LIANQI -> CultivationLevels.REALM_LIANQI + stage;
            default -> CultivationLevels.REALM_MORTAL;
        };
    }

    private static int defaultAge(String archetype) {
        return switch (archetype) {
            case JINDAN -> 230;
            case ZHUJI -> 120;
            case LIANQI -> 60;
            default -> 24;
        };
    }

    private static int defaultReputation(String archetype) {
        return switch (archetype) {
            case JINDAN -> 45;
            case ZHUJI -> 18;
            case LIANQI -> 5;
            default -> 0;
        };
    }

    private static long defaultProgress(String archetype) {
        return switch (archetype) {
            case JINDAN -> 50_000L;
            case ZHUJI -> 9_000L;
            case LIANQI -> 600L;
            default -> 0L;
        };
    }

    private static List<String> defaultRoots(String archetype) {
        return switch (archetype) {
            case JINDAN -> List.of(SpiritRoots.THUNDER, SpiritRoots.WIND);
            case ZHUJI -> List.of(SpiritRoots.FIRE, SpiritRoots.WATER);
            case LIANQI -> List.of(SpiritRoots.METAL, SpiritRoots.WOOD);
            default -> List.of();
        };
    }

    private static List<String> legacyDefaultSpells(String archetype) {
        return switch (archetype) {
            case JINDAN -> List.of(ModSpells.QI_GATHERING, ModSpells.REGENERATION, ModSpells.YUFENG_JUE,
                    ModSpells.YINLEI_JUE, ModSpells.ZHENSHAN_PALM);
            case ZHUJI -> List.of(ModSpells.QI_GATHERING, ModSpells.REGENERATION, ModSpells.FIREBALL,
                    ModSpells.WIND_STEP, ModSpells.LINGBENG);
            case LIANQI -> List.of(ModSpells.QI_GATHERING, ModSpells.REGENERATION,
                    ModSpells.FIREBALL, ModSpells.WIND_STEP);
            default -> List.of();
        };
    }

    private static void migrateNpcLoadout(CompoundTag profile, String archetype, int seed) {
        if (FANREN.equals(archetype)) {
            profile.putString("Method", "");
            profile.put("Methods", new ListTag());
            profile.put("Spells", new ListTag());
            return;
        }

        String realm = profile.getString("Realm");
        List<String> roots = strings(profile.getList("SpiritRoots", 8));
        String generatedMethod = selectRandomMethod(archetype, realm, roots, seed);
        LinkedHashSet<String> methods = new LinkedHashSet<>();
        String previousMethod = profile.getString("Method");
        if (!previousMethod.isBlank() && !CultivationMethods.CLEAR_HEART.equals(previousMethod)
                && CultivationMethods.get(previousMethod) != null) {
            methods.add(previousMethod);
        }
        if (!generatedMethod.isBlank()) {
            methods.add(generatedMethod);
        }
        profile.put("Methods", stringList(List.copyOf(methods)));
        chooseBestKnownMethod(profile);

        LinkedHashSet<String> spells = new LinkedHashSet<>(selectRandomSpells(
                archetype,
                realm,
                roots,
                normalizeIdentity(profile.getString("Identity")),
                profile.getString("Method"),
                seed));
        List<String> legacySpells = legacyDefaultSpells(archetype);
        for (String oldSpell : strings(profile.getList("Spells", 8))) {
            String normalized = ModSpells.normalizeId(oldSpell);
            ModSpells.SpellDef spell = ModSpells.get(normalized);
            if (!legacySpells.contains(normalized)
                    && spell != null
                    && NpcSpellcasting.isNpcSupportedSpell(normalized)
                    && ModSpells.meetsRequirement(realm, spell)) {
                spells.add(normalized);
            }
        }
        profile.put("Spells", stringList(List.copyOf(spells)));
        chooseBestKnownMethod(profile);
    }

    private static String selectRandomMethod(String archetype, String realm, List<String> roots, int seed) {
        if (FANREN.equals(archetype)) {
            return "";
        }
        int currentStage = CultivationLevels.getStageIndex(realm);
        List<CultivationMethods.MethodDef> candidates = CultivationMethods.allMethods().stream()
                .filter(method -> CultivationLevels.getStageIndex(method.limitLevel()) >= currentStage)
                .sorted(Comparator.comparing(CultivationMethods.MethodDef::id))
                .toList();
        if (candidates.isEmpty()) {
            return CultivationMethods.CLEAR_HEART;
        }

        Random random = new Random(((long) seed << 32) ^ realm.hashCode() ^ 0x4E50434D4554484FL);
        int totalWeight = candidates.stream().mapToInt(method -> methodSelectionWeight(method, roots)).sum();
        int roll = random.nextInt(Math.max(1, totalWeight));
        for (CultivationMethods.MethodDef method : candidates) {
            roll -= methodSelectionWeight(method, roots);
            if (roll < 0) {
                return method.id();
            }
        }
        return candidates.getLast().id();
    }

    private static List<String> selectRandomSpells(String archetype, String realm, List<String> roots,
                                                     String identity, String methodId, int seed) {
        if (FANREN.equals(archetype)) {
            return List.of();
        }
        String normalizedIdentity = normalizeIdentity(identity);
        String combatRole = npcCombatRole(normalizedIdentity, methodId);
        Random random = new Random(((long) seed << 32)
                ^ realm.hashCode()
                ^ normalizedIdentity.hashCode()
                ^ Integer.rotateLeft(methodId.hashCode(), 11)
                ^ Integer.rotateLeft(combatRole.hashCode(), 19)
                ^ 0x4E50435350454C4CL);
        LinkedHashSet<String> selected = new LinkedHashSet<>();
        if (isRealmValidNpcSpell(ModSpells.QI_GATHERING, realm)) {
            selected.add(ModSpells.QI_GATHERING);
        }
        if (isRealmValidNpcSpell(ModSpells.REGENERATION, realm)) {
            selected.add(ModSpells.REGENERATION);
        }

        List<String> movement = NpcSpellcasting.movementSpellIds().stream()
                .filter(id -> isRealmValidNpcSpell(id, realm))
                .sorted()
                .toList();
        String movementSpell = chooseWeightedSpell(
                new ArrayList<>(movement), roots, normalizedIdentity, methodId, combatRole, random);
        if (!movementSpell.isBlank()) {
            selected.add(movementSpell);
        }

        List<String> attacks = NpcSpellcasting.attackSpellIds().stream()
                .filter(id -> isRealmValidNpcSpell(id, realm))
                .sorted()
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        int attackCount = 2;
        if (random.nextFloat() < 0.55F) {
            attackCount++;
        }
        if (random.nextFloat() < 0.25F) {
            attackCount++;
        }
        if (JINDAN.equals(archetype) && random.nextFloat() < 0.15F) {
            attackCount++;
        }
        for (int i = 0; i < attackCount && !attacks.isEmpty(); i++) {
            String attack = chooseWeightedSpell(
                    attacks, roots, normalizedIdentity, methodId, combatRole, random);
            if (attack.isBlank()) {
                break;
            }
            selected.add(attack);
            attacks.remove(attack);
        }

        List<String> fullSpellPool = ModSpells.all().stream()
                .map(ModSpells.SpellDef::id)
                .filter(id -> isRealmValidNpcSpell(id, realm))
                .filter(NpcSpellcasting::isNpcSupportedSpell)
                .filter(id -> !selected.contains(id))
                .sorted()
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        int extraSpellCount = 1;
        if (random.nextFloat() < 0.6F) {
            extraSpellCount++;
        }
        if (random.nextFloat() < 0.3F) {
            extraSpellCount++;
        }
        for (int i = 0; i < extraSpellCount && !fullSpellPool.isEmpty(); i++) {
            String extra = chooseWeightedSpell(
                    fullSpellPool, roots, normalizedIdentity, methodId, combatRole, random);
            if (extra.isBlank()) {
                break;
            }
            selected.add(extra);
            fullSpellPool.remove(extra);
        }

        if ((ZHUJI.equals(archetype) || LIANQI.equals(archetype))
                && isRealmValidNpcSpell(ModSpells.DIELANG_SHIELD, realm)) {
            selected.add(ModSpells.DIELANG_SHIELD);
        }
        return List.copyOf(selected);
    }

    private static String chooseWeightedSpell(List<String> candidates, List<String> roots, String identity,
                                               String methodId, String combatRole, Random random) {
        if (candidates.isEmpty()) {
            return "";
        }
        int totalWeight = candidates.stream()
                .mapToInt(id -> spellSelectionWeight(id, roots, identity, methodId, combatRole))
                .sum();
        int roll = random.nextInt(Math.max(1, totalWeight));
        for (String id : candidates) {
            roll -= spellSelectionWeight(id, roots, identity, methodId, combatRole);
            if (roll < 0) {
                return id;
            }
        }
        return candidates.getLast();
    }

    private static int spellSelectionWeight(String spellId, List<String> roots, String identity,
                                             String methodId, String combatRole) {
        ModSpells.SpellDef spell = ModSpells.get(spellId);
        if (spell == null) {
            return 1;
        }
        int weight = spell.element().isBlank() ? 2
                : (SpiritRoots.matchesAny(roots, spell.element()) ? 6 : 1);
        CultivationMethods.MethodDef method = CultivationMethods.get(methodId);
        if (method != null) {
            if ("any".equals(method.element()) || "none".equals(method.element())) {
                weight += 1;
            } else if (!spell.element().isBlank()
                    && SpiritRoots.matchesAny(
                    List.of(method.element().split(",")), spell.element())) {
                weight += 5;
            }
            if (method.bloodDemon() && isOneOf(spellId,
                    ModSpells.FIREBALL, ModSpells.IGNITE_FLARE, ModSpells.JINHUO_XINLIAO)) {
                weight += 3;
            }
        }
        weight += identitySpellBonus(identity, spellId);
        weight += roleSpellBonus(combatRole, spellId);
        return Math.max(1, weight);
    }

    private static String npcCombatRole(String identity, String methodId) {
        if (CultivationMethods.CUIGU_LIANSHEN_JUE.equals(methodId)) {
            return "brawler";
        }
        if (CultivationMethods.POKONG_JUE.equals(methodId)) {
            return "skirmisher";
        }
        if (CultivationMethods.CHANGQING_JUE.equals(methodId)
                || CultivationMethods.HANTI_BINGQIN.equals(methodId)
                || CultivationMethods.JIUYOU_LIANSHI_JUE.equals(methodId)
                || CultivationMethods.HANLAN_NINGYUAN_JING.equals(methodId)) {
            return "control";
        }
        if (CultivationMethods.XUANYUE_SUIFENG_JUE.equals(methodId)) {
            return "defender";
        }
        if (CultivationMethods.FENTIAN_LIFE_RENEWAL.equals(methodId)
                || methodId.startsWith("blood_demon_")) {
            return "pressure";
        }
        return switch (identity) {
            case "body_cultivator" -> "brawler";
            case "saber_cultivator", "demonic_cultivator" -> "pressure";
            case "sword_cultivator", "wanted" -> "skirmisher";
            case "artifact_cultivator", "corpse_cultivator" -> "control";
            case "formation_cultivator" -> "control";
            case "talisman_cultivator" -> "ranged";
            case "clan_heir", "royal" -> "defender";
            default -> "ranged";
        };
    }

    private static int identitySpellBonus(String identity, String spellId) {
        return switch (identity) {
            case "sword_cultivator" -> isOneOf(spellId,
                    ModSpells.LIUGUANG_JIANYING, ModSpells.WIND_BLADE) ? 7 : 0;
            case "saber_cultivator" -> isOneOf(spellId,
                    ModSpells.ZHENSHAN_PALM, ModSpells.JINHUO_XINLIAO, ModSpells.IGNITE_FLARE) ? 7 : 0;
            case "body_cultivator" -> isOneOf(spellId,
                    ModSpells.LINGBENG, ModSpells.ZHENSHAN_PALM, ModSpells.WIND_STEP) ? 8 : 0;
            case "artifact_cultivator" -> isOneOf(spellId,
                    ModSpells.LIGHT_BEAM_ATTACK, ModSpells.LINGZHI_BULLET, ModSpells.SUIGUI_XINGYUN) ? 6 : 0;
            case "corpse_cultivator" -> isOneOf(spellId,
                    ModSpells.SLIDING_WATER, ModSpells.LINGBENG, ModSpells.SUIGUI_XINGYUN) ? 6 : 0;
            case "demonic_cultivator", "evil_cultivator" -> isOneOf(spellId,
                    ModSpells.FIREBALL, ModSpells.IGNITE_FLARE,
                    ModSpells.YINLEI_JUE, ModSpells.JINHUO_XINLIAO) ? 5 : 0;
            case "wanted" -> isOneOf(spellId,
                    ModSpells.WIND_STEP, ModSpells.WIND_BLADE, ModSpells.LIUGUANG_JIANYING) ? 5 : 0;
            case "clan_heir", "royal" -> isOneOf(spellId,
                    ModSpells.DIELANG_SHIELD, ModSpells.WULEI_ZHENGFA, ModSpells.LIGHT_BEAM_ATTACK) ? 4 : 0;
            default -> 0;
        };
    }

    private static int roleSpellBonus(String combatRole, String spellId) {
        return switch (combatRole) {
            case "brawler" -> isOneOf(spellId,
                    ModSpells.LINGBENG, ModSpells.ZHENSHAN_PALM, ModSpells.WIND_STEP) ? 6 : 0;
            case "pressure" -> isOneOf(spellId,
                    ModSpells.FIREBALL, ModSpells.IGNITE_FLARE,
                    ModSpells.JINHUO_XINLIAO, ModSpells.YUFENG_JUE) ? 5 : 0;
            case "skirmisher" -> isOneOf(spellId,
                    ModSpells.WIND_BLADE, ModSpells.SLIDING_WATER,
                    ModSpells.LIUGUANG_JIANYING, ModSpells.WIND_STEP) ? 5 : 0;
            case "control" -> isOneOf(spellId,
                    ModSpells.SLIDING_WATER, ModSpells.WULEI_ZHENGFA,
                    ModSpells.SUIGUI_XINGYUN, ModSpells.DIELANG_SHIELD) ? 5 : 0;
            case "defender" -> isOneOf(spellId,
                    ModSpells.DIELANG_SHIELD, ModSpells.LIGHT_BEAM_ATTACK, ModSpells.YUFENG_JUE) ? 5 : 0;
            default -> isOneOf(spellId,
                    ModSpells.FIREBALL, ModSpells.LINGZHI_BULLET,
                    ModSpells.LIGHT_BEAM_ATTACK, ModSpells.YINLEI_JUE) ? 4 : 0;
        };
    }

    private static boolean isOneOf(String value, String... candidates) {
        for (String candidate : candidates) {
            if (candidate.equals(value)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRealmValidNpcSpell(String spellId, String realm) {
        ModSpells.SpellDef spell = ModSpells.get(spellId);
        return spell != null
                && ModSpells.meetsRequirement(realm, spell);
    }

    private static void ensureKnownMethods(CompoundTag profile) {
        if (profile.contains("Methods")) {
            return;
        }
        String activeMethod = profile.getString("Method");
        profile.put("Methods", stringList(activeMethod.isBlank() ? List.of() : List.of(activeMethod)));
    }

    private static List<String> knownMethodIds(CompoundTag profile) {
        ensureKnownMethods(profile);
        return new ArrayList<>(strings(profile.getList("Methods", 8)));
    }

    private static void chooseBestKnownMethod(CompoundTag profile) {
        ensureKnownMethods(profile);
        String realm = profile.getString("Realm");
        int currentStage = CultivationLevels.getStageIndex(realm);
        List<String> roots = strings(profile.getList("SpiritRoots", 8));
        List<CultivationMethods.MethodDef> available = knownMethodIds(profile).stream()
                .map(CultivationMethods::get)
                .filter(java.util.Objects::nonNull)
                .filter(method -> CultivationLevels.getStageIndex(method.limitLevel()) >= currentStage)
                .toList();
        boolean canTrain = available.stream().anyMatch(method -> CultivationMethods.canGainProgress(method.id(), realm));
        String best = available.stream()
                .filter(method -> !canTrain || CultivationMethods.canGainProgress(method.id(), realm))
                .max(Comparator.comparingInt(method -> methodScore(method, roots, currentStage)))
                .map(CultivationMethods.MethodDef::id)
                .orElse("");
        profile.putString("Method", best);
        syncMethodSpecialSpells(profile);
    }

    private static void syncMethodSpecialSpells(CompoundTag profile) {
        List<String> existing = strings(profile.getList("Spells", 8));
        LinkedHashSet<String> spells = new LinkedHashSet<>(existing);
        for (String methodId : knownMethodIds(profile)) {
            switch (methodId) {
                case CultivationMethods.REINCARNATION_TRUE_ART -> spells.add(ModSpells.ABSORB_CULTIVATION);
                case CultivationMethods.TUNTIAN_DEMON_ART -> spells.add(ModSpells.TUNTIAN);
                case CultivationMethods.POKONG_JUE -> spells.add(ModSpells.FENGYA);
                case CultivationMethods.CUIGU_LIANSHEN_JUE -> spells.add(ModSpells.CUIGU_LIANSHEN);
                case CultivationMethods.HANTI_BINGQIN -> {
                    spells.add(ModSpells.HANJING_SUOZHUA);
                    spells.add(ModSpells.SHUANGTIAN_QI);
                    spells.add(ModSpells.SUISHUANG_LINGXIAO);
                }
                case CultivationMethods.XUANYUE_SUIFENG_JUE -> {
                    spells.add(ModSpells.XUANYUE_BI);
                    spells.add(ModSpells.JINYAN_SHENGFENG);
                    spells.add(ModSpells.SHANSUO_ZHENYU);
                    spells.add(ModSpells.WANFENG_YUEZANG);
                }
                case CultivationMethods.HANLAN_NINGYUAN_JING -> {
                    spells.add(ModSpells.DONGLIU_SUO);
                    spells.add(ModSpells.CANGYUAN_HONGLIU);
                    spells.add(ModSpells.HANYUAN_FENGJIE);
                }
                case CultivationMethods.CHANGQING_JUE -> {
                    spells.add(ModSpells.WANMU_SHENG);
                    spells.add(ModSpells.QINGTENG_FU);
                }
                default -> {
                }
            }
        }
        if (spells.size() != existing.size()) {
            profile.put("Spells", stringList(List.copyOf(spells)));
        }
    }

    private static int methodSelectionWeight(CultivationMethods.MethodDef method, List<String> roots) {
        if (CultivationMethods.CUIGU_LIANSHEN_JUE.equals(method.id())
                || CultivationMethods.JIUYOU_LIANSHI_JUE.equals(method.id())) {
            return 1;
        }
        int rarityWeight = switch (method.tier()) {
            case "human" -> 14;
            case "earth" -> 7;
            case "heaven" -> 2;
            default -> 1;
        };
        int affinityWeight = ("any".equals(method.element()) || "none".equals(method.element()))
                ? 3 : (methodMatchesRoots(method, roots) ? 5 : 1);
        return Math.max(1, rarityWeight * affinityWeight);
    }

    private static int methodScore(CultivationMethods.MethodDef method, List<String> roots, int currentStage) {
        int multiplier = methodMultiplierPercent(method, roots);
        int tier = switch (method.tier()) {
            case "earth" -> 1;
            case "heaven" -> 2;
            case "immortal" -> 3;
            case "divine" -> 4;
            default -> 0;
        };
        int remainingStages = Math.max(0, CultivationLevels.getStageIndex(method.limitLevel()) - currentStage);
        int bloodPenalty = method.bloodDemon() ? 20_000 : 0;
        return multiplier * 1_000 + tier * 100 + remainingStages - bloodPenalty;
    }

    private static int methodMultiplierPercent(CultivationMethods.MethodDef method, List<String> roots) {
        if (method == null) {
            return 0;
        }
        if ("any".equals(method.element()) || "none".equals(method.element())) {
            return switch (method.tier()) {
                case "earth" -> 120;
                case "heaven" -> 150;
                case "immortal" -> 200;
                case "divine" -> 300;
                default -> 100;
            };
        }
        List<String> elements = List.of(method.element().split(","));
        if (roots.size() == 1 && elements.contains(roots.getFirst())) {
            return 200;
        }
        return roots.stream().anyMatch(elements::contains) ? 100 : 10;
    }

    private static boolean methodMatchesRoots(CultivationMethods.MethodDef method, List<String> roots) {
        if ("any".equals(method.element()) || "none".equals(method.element())) {
            return true;
        }
        List<String> elements = List.of(method.element().split(","));
        return roots.stream().anyMatch(elements::contains);
    }

    private static List<String> defaultFamily(int seed) {
        String surname = SURNAMES[Math.floorMod(seed, SURNAMES.length)];
        String siblingGender = Math.floorMod(seed / 43, 2) == 0 ? GENDER_MALE : GENDER_FEMALE;
        return List.of(
                "父亲·" + surname + givenName(seed, 3, GENDER_MALE),
                "母亲·" + chineseName(seed, 7, GENDER_FEMALE),
                "同辈·" + surname + givenName(seed, 11, siblingGender));
    }

    private static String chineseName(int seed, int offset, String gender) {
        String surname = SURNAMES[Math.floorMod(seed + offset * 31, SURNAMES.length)];
        return surname + givenName(seed, offset, gender);
    }

    private static String givenName(int seed, int offset, String gender) {
        String[] names = GENDER_FEMALE.equals(normalizeGender(gender))
                ? FEMALE_GIVEN_NAMES
                : MALE_GIVEN_NAMES;
        return names[Math.floorMod(seed / 7 + offset * 17, names.length)];
    }

    private static NpcSkinCatalog.Skin selectSkin(String archetype, int seed) {
        List<NpcSkinCatalog.Skin> skins = switch (archetype) {
            case JINDAN, ZHUJI, LIANQI -> CULTIVATOR_SKINS;
            default -> FANREN_SKINS;
        };
        return skins.get(Math.floorMod(seed / 31, skins.size()));
    }

    private static String normalizeGender(String gender) {
        return GENDER_FEMALE.equalsIgnoreCase(gender) ? GENDER_FEMALE : GENDER_MALE;
    }

    private static void applyRealmStats(PathfinderMob mob, String realm, boolean refillHealth) {
        ImmortalCultivationMod.raiseMaxHealthAttributeLimit();
        ImmortalCultivationMod.raiseArmorAttributeLimit();
        CultivationLevels.LevelDef levelDef = CultivationLevels.getLevelDef(realm);
        var maxHealth = mob.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            double targetMaxHealth = levelDef.maxHp();
            float oldMaxHealth = mob.getMaxHealth();
            boolean wasFullHealth = mob.getHealth() >= oldMaxHealth - 0.01F;
            if (Math.abs(maxHealth.getBaseValue() - targetMaxHealth) > 0.01D) {
                maxHealth.setBaseValue(targetMaxHealth);
            }
            if (refillHealth || wasFullHealth) {
                mob.setHealth(mob.getMaxHealth());
            } else if (mob.getHealth() > mob.getMaxHealth()) {
                mob.setHealth(mob.getMaxHealth());
            }
        }

        var armor = mob.getAttribute(Attributes.ARMOR);
        if (armor != null && Math.abs(armor.getBaseValue() - levelDef.armor()) > 0.01D) {
            armor.setBaseValue(levelDef.armor());
        }
    }

    private static String normalizePersonality(String personality) {
        String normalized = personality == null ? "" : personality.toLowerCase(Locale.ROOT).trim();
        return switch (normalized) {
            case "cautious", "proud", "gentle", "wandering", "aggressive", "scholarly" -> normalized;
            default -> "gentle";
        };
    }

    private static ListTag stringList(List<String> values) {
        ListTag list = new ListTag();
        for (String value : values) {
            list.add(StringTag.valueOf(value));
        }
        return list;
    }

    private static List<String> strings(ListTag list) {
        List<String> values = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            values.add(list.getString(i));
        }
        return values;
    }

    private record NpcTradePrice(String currencyItemId, int price) {
    }

    private record SystemPriceBenchmark(int itemLevel, String group, int price, boolean modItem) {
    }

    public record TradeOffer(int inventorySlot, String currencyItemId, int price, String resultItemId,
                             int resultCount, int availableCount) {
    }

    private record GiftEvaluation(int quality, int expected, GiftTier tier) {
    }

    private enum GiftTier {
        POOR("poor"),
        ACCEPTABLE("acceptable"),
        GOOD("good"),
        EXCELLENT("excellent");

        private final String key;

        GiftTier(String key) {
            this.key = key;
        }

        private String key() {
            return key;
        }
    }

    private enum SocialAction {
        PRAISE(65),
        JOKE(55),
        INSULT(60),
        THREATEN(45);

        private final int baseChance;

        SocialAction(int baseChance) {
            this.baseChance = baseChance;
        }

        private int baseChance() {
            return baseChance;
        }
    }

    private record SocialCheck(int chance, int fatigue, int repeats) {
    }

    private record SocialOutcome(int friendship, int trust, int affection, int hatred, int fear, int grudge,
                                 boolean retaliate, String statusKey) {
    }

    private record RelationshipBaseline(int friendship, int trust, int affection, int hatred, boolean custom) {
        private static final RelationshipBaseline NORMAL = new RelationshipBaseline(0, 0, 0, 0, false);
    }

    private record WantedReward(String itemId, int count) {
    }

    private record QuestDef(String id, String requiredItemId, int requiredCount, String rewardItemId, int rewardCount) {
    }

}
