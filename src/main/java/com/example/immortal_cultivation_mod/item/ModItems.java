package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.attachment.SpiritRoots;
import com.example.immortal_cultivation_mod.block.ModBlocks;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.entity.FireballTalismanProjectileEntity;
import com.example.immortal_cultivation_mod.entity.ModelBonePartEntity;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ImmortalCultivationMod.MODID);

    public static final DeferredItem<Item> FIREBALL_SCROLL = registerItem("fireball_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.FIREBALL));

    public static final DeferredItem<Item> LINGBENG_SCROLL = registerItem("lingbeng_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.LINGBENG));

    public static final DeferredItem<Item> REGENERATION_SCROLL = registerItem("regeneration_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.REGENERATION));

    public static final DeferredItem<Item> BEAM_SCROLL = registerItem("beam_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.BEAM));

    public static final DeferredItem<Item> EARTH_ESCAPE_SCROLL = registerItem("earth_escape_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.EARTH_ESCAPE));

    public static final DeferredItem<Item> CLEANSE_SCROLL = registerItem("cleanse_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.CLEANSE));

    public static final DeferredItem<Item> QI_GATHERING_SCROLL = registerItem("qi_gathering_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.QI_GATHERING));

    public static final DeferredItem<Item> IGNITE_FLARE_SCROLL = registerItem("ignite_flare_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.IGNITE_FLARE));

    public static final DeferredItem<Item> SPIRIT_SIGHT_SCROLL = registerItem("spirit_sight_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.SPIRIT_SIGHT));

    public static final DeferredItem<Item> ZHENSHAN_PALM_SCROLL = registerItem("zhenshan_palm_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.ZHENSHAN_PALM));

    public static final DeferredItem<Item> LIGHT_BEAM_ATTACK_SCROLL = registerItem("light_beam_attack_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.LIGHT_BEAM_ATTACK));

    public static final DeferredItem<Item> DIELANG_SHIELD_SCROLL = registerItem("dielang_shield_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.DIELANG_SHIELD));

    public static final DeferredItem<Item> LINGZHI_BULLET_SCROLL = registerItem("lingzhi_bullet_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.LINGZHI_BULLET));

    public static final DeferredItem<Item> WIND_BLADE_SCROLL = registerItem("wind_blade_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.WIND_BLADE));

    public static final DeferredItem<Item> WIND_STEP_SCROLL = registerItem("wind_step_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.WIND_STEP));

    public static final DeferredItem<Item> YUFENG_JUE_SCROLL = registerItem("yufeng_jue_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.YUFENG_JUE));

    public static final DeferredItem<Item> SMOKE_ART_SCROLL = registerItem("smoke_art_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.SMOKE_ART));

    public static final DeferredItem<Item> HUTI_QI_SCROLL = registerItem("huti_qi_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.HUTI_QI));

    public static final DeferredItem<Item> MICHEN_ZHANG_SCROLL = registerItem("michen_zhang_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.MICHEN_ZHANG));

    public static final DeferredItem<Item> SLIDING_WATER_SCROLL = registerItem("sliding_water_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.SLIDING_WATER));


    public static final DeferredItem<Item> DINGSHEN_SCROLL = registerItem("dingshen_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.DINGSHEN));

    public static final DeferredItem<Item> YINLEI_JUE_SCROLL = registerItem("yinlei_jue_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.YINLEI_JUE));

    public static final DeferredItem<Item> WULEI_ZHENGFA_SCROLL = registerItem("wulei_zhengfa_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.WULEI_ZHENGFA));

    public static final DeferredItem<Item> LIUGUANG_JIANYING_SCROLL = registerItem("liuguang_jianying_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.LIUGUANG_JIANYING));

    public static final DeferredItem<Item> SIFANG_JIE_SCROLL = registerItem("sifang_jie_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.SIFANG_JIE));

    public static final DeferredItem<Item> GUSHI_SHIELD_SCROLL = registerItem("gushi_shield_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.GUSHI_SHIELD));

    public static final DeferredItem<Item> KONGSHI_SHU_SCROLL = registerItem("kongshi_shu_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.KONGSHI_SHU));

    public static final DeferredItem<Item> XUYING_TA_SCROLL = registerItem("xuying_ta_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.XUYING_TA));

    public static final DeferredItem<Item> DUANLIU_KONGDUN_SCROLL = registerItem("duanliu_kongdun_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.DUANLIU_KONGDUN));

    public static final DeferredItem<Item> YIHEN_CI_SCROLL = registerItem("yihen_ci_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.YIHEN_CI));

    public static final DeferredItem<Item> JINHUO_XINLIAO_SCROLL = registerItem("jinhuo_xinliao_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.JINHUO_XINLIAO));

    public static final DeferredItem<Item> SUIGUI_XINGYUN_SCROLL = registerItem("suigui_xingyun_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.SUIGUI_XINGYUN));

    public static final DeferredItem<Item> SHIXU_CHILIU_SCROLL = registerItem("shixu_chiliu_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.SHIXU_CHILIU));

    public static final DeferredItem<Item> ZHUJI_CULTIVATOR_SPAWN_EGG = registerItem("zhuji_cultivator_spawn_egg",
            () -> new SpawnEggItem(com.example.immortal_cultivation_mod.entity.ModEntities.ZHUJI_CULTIVATOR.get(), 0x3E2D45, 0x74B6FF, new Item.Properties()));

    public static final DeferredItem<Item> JINDAN_CULTIVATOR_SPAWN_EGG = registerItem("jindan_cultivator_spawn_egg",
            () -> new SpawnEggItem(com.example.immortal_cultivation_mod.entity.ModEntities.JINDAN_CULTIVATOR.get(), 0x4C3924, 0xF6C64A, new Item.Properties()));

    public static final DeferredItem<Item> LIUSAN_SPAWN_EGG = registerItem("liusan_spawn_egg",
            () -> new SpawnEggItem(com.example.immortal_cultivation_mod.entity.ModEntities.LIUSAN.get(), 0x102D1E, 0x9DBB78, new Item.Properties()));

    public static final DeferredItem<Item> FANREN_NPC_SPAWN_EGG = registerItem("fanren_npc_spawn_egg",
            () -> new SpawnEggItem(com.example.immortal_cultivation_mod.entity.ModEntities.FANREN_NPC.get(), 0x8B6B4A, 0xD8C59C, new Item.Properties()));

    public static final DeferredItem<Item> CRIMSON_SCALE_FLAME_LIZARD_SPAWN_EGG =
            registerItem("crimson_scale_flame_lizard_spawn_egg",
                    () -> new SpawnEggItem(
                            com.example.immortal_cultivation_mod.entity.ModEntities.CRIMSON_SCALE_FLAME_LIZARD.get(),
                            0x8A1E18, 0xFF8C24, new Item.Properties()));

    public static final DeferredItem<Item> CRIMSON_SKY_FLAME_FLOOD_DRAGON_SPAWN_EGG =
            registerItem("crimson_sky_flame_flood_dragon_spawn_egg",
                    () -> new SpawnEggItem(
                            com.example.immortal_cultivation_mod.entity.ModEntities
                                    .CRIMSON_SKY_FLAME_FLOOD_DRAGON.get(),
                            0x3A0704, 0xFF5A0A, new Item.Properties()));

    public static final DeferredItem<Item> BLOOD_APE_DEMON_GENERAL_SPAWN_EGG =
            registerItem("blood_ape_demon_general_spawn_egg",
                    () -> new SpawnEggItem(
                            com.example.immortal_cultivation_mod.entity.ModEntities
                                    .BLOOD_APE_DEMON_GENERAL.get(),
                            0x35100D, 0xB83228, new Item.Properties()));

    public static final DeferredItem<Item> AZURE_SPIRIT_SILKWORM_SPAWN_EGG =
            registerItem("azure_spirit_silkworm_spawn_egg",
                    () -> new SpawnEggItem(
                            com.example.immortal_cultivation_mod.entity.ModEntities.AZURE_SPIRIT_SILKWORM.get(),
                            0x66DDE0, 0xE8FFFF, new Item.Properties()));

    public static final DeferredItem<Item> ENLIGHTENMENT_PILL = registerItem("enlightenment_pill",
            () -> new EnlightenmentPillItem(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<Item> BREAKTHROUGH_PILL = registerItem("breakthrough_pill",
            () -> new Item(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<Item> SMALL_LEVEL_UP = registerItem("small_level_up",
            () -> new LevelUpItem(new Item.Properties().stacksTo(16), 100, false));

    public static final DeferredItem<Item> BIG_LEVEL_UP = registerItem("big_level_up",
            () -> new LevelUpItem(new Item.Properties().stacksTo(16), 1000, false));

    public static final DeferredItem<Item> BODY_TEMPERING_PILL = registerItem("body_tempering_pill",
            () -> new BodyCultivationItems.BodyTemperingPillItem(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<Item> BLOOD_QI_PILL = registerItem("blood_qi_pill",
            () -> new BodyCultivationItems.BloodQiPillItem(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<Item> QI_POUCH = registerItem("qi_pouch",
            () -> new QiPouchItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> DEBUG_STAT_EDITOR = registerItem("debug_stat_editor",
            () -> new DebugStatEditorItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> RESTRICTION_INSIGHT = registerItem("restriction_insight",
            () -> new RestrictionInsightItem(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<Item> FOG_REVEALING_MIRROR = registerItem("fog_revealing_mirror",
            () -> new FogRevealingMirrorItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> YINYANG_COMPASS = registerItem("yinyang_compass",
            () -> new YinYangCompassItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> ZHENSHI_TALISMAN = registerItem("zhenshi_talisman",
            () -> new Item(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<Item> ZHENSHI_SEAL_HELMET = registerItem("zhenshi_seal_helmet",
            () -> new Item(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> ZHENHUN_BELL = registerItem("zhenhun_bell",
            () -> new ZhenhunBellItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> YINHUN_GONG = registerItem("yinhun_gong",
            () -> new YinhunGongItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> FENGYAN_TALISMAN = registerItem("fengyan_talisman",
            () -> new Item(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<Item> FIREBALL_TALISMAN = registerItem("fireball_talisman",
            () -> new FireballTalismanItem(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<Item> BLOOD = registerItem("blood",
            () -> new BloodItem(new Item.Properties().stacksTo(64)));

    public static final DeferredItem<Item> BLOOD_CRYSTAL = registerItem("blood_crystal",
            () -> new Item(new Item.Properties().stacksTo(64)));

    public static final DeferredItem<Item> CHI_LIN = registerItem("chi_lin",
            () -> new Item(new Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> HUO_XI_BLOOD = registerItem("huo_xi_blood",
            () -> new Item(new Item.Properties().stacksTo(64)));

    public static final DeferredItem<Item> JINDAN_YAODAN = registerItem("jindan_yaodan",
            () -> new Item(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));

    public static final DeferredItem<Item> CHIYAN_BONE = registerItem("chiyan_bone",
            () -> new Item(new Item.Properties().stacksTo(64).rarity(Rarity.RARE)));

    public static final DeferredItem<Item> YUANYING_YAODAN = registerItem("yuanying_yaodan",
            () -> new Item(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC)));

    public static final DeferredItem<Item> YANJIAO_HORN = registerItem("yanjiao_horn",
            () -> new Item(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));

    public static final DeferredItem<Item> YAOYUAN_BONE = registerItem("yaoyuan_bone",
            () -> new Item(new Item.Properties().stacksTo(64).rarity(Rarity.RARE)));

    public static final DeferredItem<Item> XUESHA_HIDE = registerItem("xuesha_hide",
            () -> new Item(new Item.Properties().stacksTo(64).rarity(Rarity.RARE)));

    public static final DeferredItem<Item> NUOMI = registerItem("nuomi",
            () -> new BlockItem(ModBlocks.NUOMI_DUST.get(), new Item.Properties().stacksTo(64)));

    public static final DeferredItem<Item> NUOMI_SEED = registerItem("nuomi_seed",
            () -> new ItemNameBlockItem(ModBlocks.NUOMI_PLANT.get(), new Item.Properties().stacksTo(64)));

    public static final DeferredItem<Item> NUOMI_ZHOU = registerItem("nuomi_zhou",
            () -> new NuomiSoupItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> MODOU_LINE = registerItem("modou_line",
            () -> new ModouLineItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> TAOMU_SWORD = registerItem("taomu_sword",
            () -> new TaomuSwordItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> RED_STRING = registerItem("red_string",
            () -> new Item(new Item.Properties().stacksTo(64)));

    public static final DeferredItem<Item> COPPER_COIN = registerItem("copper_coin",
            () -> new Item(new Item.Properties().stacksTo(64)));

    public static final DeferredItem<Item> BIGU_PILL = registerItem("bigu_pill",
            () -> new IdentityItems.BiguPillItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> LOW_QI_RECOVERY_PILL = registerItem("low_qi_recovery_pill",
            () -> new IdentityItems.QiRecoveryPillItem(new Item.Properties().stacksTo(16), 10));
    public static final DeferredItem<Item> MIDDLE_QI_RECOVERY_PILL = registerItem("middle_qi_recovery_pill",
            () -> new IdentityItems.QiRecoveryPillItem(new Item.Properties().stacksTo(16), 100));
    public static final DeferredItem<Item> HIGH_QI_RECOVERY_PILL = registerItem("high_qi_recovery_pill",
            () -> new IdentityItems.QiRecoveryPillItem(new Item.Properties().stacksTo(16), 1000));
    public static final DeferredItem<Item> LOW_YANGJIAN_STONE = registerItem("low_yangjian_stone",
            () -> new Item(new Item.Properties().durability(16)));
    public static final DeferredItem<Item> MIDDLE_YANGJIAN_STONE = registerItem("middle_yangjian_stone",
            () -> new Item(new Item.Properties().durability(32)));
    public static final DeferredItem<Item> HIGH_YANGJIAN_STONE = registerItem("high_yangjian_stone",
            () -> new Item(new Item.Properties().durability(64)));
    public static final DeferredItem<Item> LOW_SWORD_GRINDSTONE = registerItem("low_sword_grindstone",
            () -> new Item(new Item.Properties().durability(16)));
    public static final DeferredItem<Item> MIDDLE_SWORD_GRINDSTONE = registerItem("middle_sword_grindstone",
            () -> new Item(new Item.Properties().durability(32)));
    public static final DeferredItem<Item> HIGH_SWORD_GRINDSTONE = registerItem("high_sword_grindstone",
            () -> new Item(new Item.Properties().durability(64)));
    public static final DeferredItem<Item> LOW_SABER_GRINDSTONE = registerItem("low_saber_grindstone",
            () -> new Item(new Item.Properties().durability(16)));
    public static final DeferredItem<Item> MIDDLE_SABER_GRINDSTONE = registerItem("middle_saber_grindstone",
            () -> new Item(new Item.Properties().durability(32)));
    public static final DeferredItem<Item> HIGH_SABER_GRINDSTONE = registerItem("high_saber_grindstone",
            () -> new Item(new Item.Properties().durability(64)));
    public static final DeferredItem<Item> FAMILY_TOKEN = registerItem("family_token",
            () -> new IdentityItems.IdentityTokenItem(new Item.Properties().stacksTo(1), "clan_heir"));
    public static final DeferredItem<Item> ROYAL_TOKEN = registerItem("royal_token",
            () -> new IdentityItems.IdentityTokenItem(new Item.Properties().stacksTo(1), "royal"));
    public static final DeferredItem<Item> PROTECTIVE_JADE_PENDANT = registerItem("protective_jade_pendant",
            () -> new IdentityItems.ProtectivePendantItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> LETHAL_POISON = registerItem("lethal_poison",
            () -> new IdentityItems.LethalPoisonItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> WANTED_ORDER = registerItem("wanted_order",
            () -> new WantedOrderItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> DISGUISE_MASK = registerItem("disguise_mask",
            () -> new DisguiseMaskItem(new Item.Properties()));
    public static final DeferredItem<Item> CORPSE_REFINING_SECRET = registerItem("corpse_refining_secret",
            () -> new CultivationMethodItem(new Item.Properties().stacksTo(1), CultivationMethods.JIUYOU_LIANSHI_JUE));

    public static final DeferredItem<Item> COPPER_COIN_SWORD = registerItem("copper_coin_sword",
            () -> new CopperCoinSwordItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> COFFIN = registerItem("coffin",
            () -> new CoffinItem(new Item.Properties()));

    public static final DeferredItem<Item> FLAG = registerItem("flag",
            () -> new FlagItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> CLOTH_SHEATH = registerItem("cloth_sheath",
            () -> new ClothSheathItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> SWORD_CASE = registerItem("sword_case",
            () -> new SwordCaseItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> XIUJIAN = registerItem("xiujian",
            () -> new XiujianItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> WHITE_WOOL_STRIP = woolStrip("white", DyeColor.WHITE);
    public static final DeferredItem<Item> ORANGE_WOOL_STRIP = woolStrip("orange", DyeColor.ORANGE);
    public static final DeferredItem<Item> MAGENTA_WOOL_STRIP = woolStrip("magenta", DyeColor.MAGENTA);
    public static final DeferredItem<Item> LIGHT_BLUE_WOOL_STRIP = woolStrip("light_blue", DyeColor.LIGHT_BLUE);
    public static final DeferredItem<Item> YELLOW_WOOL_STRIP = woolStrip("yellow", DyeColor.YELLOW);
    public static final DeferredItem<Item> LIME_WOOL_STRIP = woolStrip("lime", DyeColor.LIME);
    public static final DeferredItem<Item> PINK_WOOL_STRIP = woolStrip("pink", DyeColor.PINK);
    public static final DeferredItem<Item> GRAY_WOOL_STRIP = woolStrip("gray", DyeColor.GRAY);
    public static final DeferredItem<Item> LIGHT_GRAY_WOOL_STRIP = woolStrip("light_gray", DyeColor.LIGHT_GRAY);
    public static final DeferredItem<Item> CYAN_WOOL_STRIP = woolStrip("cyan", DyeColor.CYAN);
    public static final DeferredItem<Item> PURPLE_WOOL_STRIP = woolStrip("purple", DyeColor.PURPLE);
    public static final DeferredItem<Item> BLUE_WOOL_STRIP = woolStrip("blue", DyeColor.BLUE);
    public static final DeferredItem<Item> BROWN_WOOL_STRIP = woolStrip("brown", DyeColor.BROWN);
    public static final DeferredItem<Item> GREEN_WOOL_STRIP = woolStrip("green", DyeColor.GREEN);
    public static final DeferredItem<Item> RED_WOOL_STRIP = woolStrip("red", DyeColor.RED);
    public static final DeferredItem<Item> BLACK_WOOL_STRIP = woolStrip("black", DyeColor.BLACK);

    public static final DeferredItem<Item> BLINDFOLD = registerItem("blindfold",
            () -> new BlindfoldItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> BLINDFOLD2 = registerItem("blindfold2",
            () -> new Blindfold2Item(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> STRAW_HAT = registerItem("straw_hat",
            () -> new StrawHatItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> NOOB_HAT = registerItem("noob_hat",
            () -> new NoobHatItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> JADE_PENDANT = registerItem("jade_pendant",
            () -> new JadePendantItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> DAOFU_CHESTPLATE = registerItem("daofu_chestplate",
            () -> new DaofuChestplateItem(new Item.Properties()));

    public static final DeferredItem<Item> NEIJIA_CHESTPLATE = registerItem("neijia_chestplate",
            () -> new NeijiaChestplateItem(new Item.Properties()));

    public static final DeferredItem<Item> ZHONGYI = registerItem("zhongyi",
            () -> new ZhongyiItem(new Item.Properties()));

    public static final DeferredItem<Item> FLYING_SWORD = registerItem("flying_sword",
            () -> new FlyingSwordItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> FEIJIAN_YUJIAN = registerItem("feijian_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> DAO_YUJIAN = registerItem("dao_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> JINGTIE_SPEAR_YUJIAN = registerItem("jingtie_spear_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> HEAVY_IRON_BRACER_YUJIAN = registerItem("heavy_iron_bracer_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> THROWING_KNIFE_YUJIAN = registerItem("throwing_knife_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> FLYING_NEEDLE_YUJIAN = registerItem("flying_needle_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> ZHENPAN_YUJIAN = registerItem("zhenpan_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> BLANK_FORMATION_FLAG_YUJIAN = registerItem("blank_formation_flag_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> DURATION_FLAG_YUJIAN = registerItem("duration_flag_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> AMPLIFIER_FLAG_YUJIAN = registerItem("amplifier_flag_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> SPIRIT_GATHERING_FLAG_YUJIAN = registerItem("spirit_gathering_flag_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> HASTE_FLAG_YUJIAN = registerItem("haste_flag_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> QI_RETURN_FLAG_YUJIAN = registerItem("qi_return_flag_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> DAO = registerItem("dao",
            () -> new DaoItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> JINGTIE_SPEAR = registerItem("jingtie_spear",
            () -> new JingtieSpearItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> THROWING_KNIFE = registerItem("throwing_knife",
            () -> new ThrowingKnifeItem(new Item.Properties()
                    .stacksTo(1)));
    public static final DeferredItem<Item> FLYING_NEEDLE = registerItem("flying_needle",
            () -> new FlyingNeedleItem(new Item.Properties()
                    .stacksTo(1)));

    public static final DeferredItem<Item> TIEGU_SHAN = registerItem("tiegu_shan",
            () -> new TieguShanItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> FUCHEN = registerItem("fuchen",
            () -> new FuchenItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> WHIP = registerItem("whip",
            () -> new WhipItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> HEAVY_IRON_BRACER = registerItem("heavy_iron_bracer",
            () -> new HeavyIronBracerItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> YAOYUAN_FIST_BONE = registerItem("yaoyuan_fist_bone",
            () -> new HeavyIronBracerItem(
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC), 5_000, 500.0F));
    public static final DeferredItem<Item> HEAVY_IRON_BRACER_CHESTPLATE_DEBUG =
            registerItem("heavy_iron_bracer_chestplate_debug",
                    () -> new HeavyIronBracerChestplateItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> YAOYUAN_FIST_BONE_CHESTPLATE_DEBUG =
            registerItem("yaoyuan_fist_bone_chestplate_debug",
                    () -> new YaoyuanFistBoneChestplateItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> SWORD_MOLD = registerItem("sword_mold",
            () -> new WeaponMoldItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> SABER_MOLD = registerItem("saber_mold",
            () -> new WeaponMoldItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> SPEAR_MOLD = registerItem("spear_mold",
            () -> new WeaponMoldItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> WEAPON_MOLD = registerItem("weapon_mold",
            () -> new WeaponMoldItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> BASIC_BREATHING_METHOD = registerItem("basic_breathing_method",
            () -> new CultivationMethodItem(new Item.Properties().stacksTo(1), CultivationMethods.BASIC_BREATHING));

    public static final DeferredItem<Item> CLEAR_HEART_METHOD = registerItem("clear_heart_method",
            () -> new CultivationMethodItem(new Item.Properties().stacksTo(1), CultivationMethods.CLEAR_HEART));

    public static final DeferredItem<Item> BLOOD_DEMON_JINDAN_METHOD = registerItem("blood_demon_jindan_method",
            () -> new CultivationMethodItem(new Item.Properties().stacksTo(1), CultivationMethods.BLOOD_DEMON_JINDAN));

    public static final DeferredItem<Item> BLOOD_DEMON_YUANYING_METHOD = registerItem("blood_demon_yuanying_method",
            () -> new CultivationMethodItem(new Item.Properties().stacksTo(1), CultivationMethods.BLOOD_DEMON_YUANYING));

    public static final DeferredItem<Item> BLOOD_DEMON_HUASHEN_METHOD = registerItem("blood_demon_huashen_method",
            () -> new CultivationMethodItem(new Item.Properties().stacksTo(1), CultivationMethods.BLOOD_DEMON_HUASHEN));

    public static final DeferredItem<Item> REINCARNATION_TRUE_ART_METHOD = registerItem("reincarnation_true_art_method",
            () -> new CultivationMethodItem(new Item.Properties().stacksTo(1), CultivationMethods.REINCARNATION_TRUE_ART));

    public static final DeferredItem<Item> TUNTIAN_DEMON_ART_METHOD = registerItem("tuntian_demon_art_method",
            () -> new CultivationMethodItem(new Item.Properties().stacksTo(1), CultivationMethods.TUNTIAN_DEMON_ART));

    public static final DeferredItem<Item> POKONG_JUE_METHOD = registerItem("pokong_jue_method",
            () -> new CultivationMethodItem(new Item.Properties().stacksTo(1), CultivationMethods.POKONG_JUE));

    public static final DeferredItem<Item> CHANGQING_JUE_METHOD = registerItem("changqing_jue_method",
            () -> new CultivationMethodItem(new Item.Properties().stacksTo(1), CultivationMethods.CHANGQING_JUE));

    public static final DeferredItem<Item> FENTIAN_LIFE_RENEWAL_METHOD = registerItem("fentian_life_renewal_method",
            () -> new CultivationMethodItem(new Item.Properties().stacksTo(1), CultivationMethods.FENTIAN_LIFE_RENEWAL));

    public static final DeferredItem<Item> HANTI_BINGQIN_METHOD = registerItem("hanti_bingqin_method",
            () -> new CultivationMethodItem(new Item.Properties().stacksTo(1), CultivationMethods.HANTI_BINGQIN));

    public static final DeferredItem<Item> CUIGU_LIANSHEN_METHOD = registerItem("cuigu_lianshen_method",
            () -> new CultivationMethodItem(new Item.Properties().stacksTo(1), CultivationMethods.CUIGU_LIANSHEN_JUE));

    public static final DeferredItem<Item> LINGJIAO_SCALE = registerItem("lingjiao_scale",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> SHIHUNFAN = registerItem("shihunfan",
            () -> new ShihunfanItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> YUANHUN_BOTTLE = registerItem("yuanhun_bottle",
            () -> new YuanhunBottleItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> MYSTERIOUS_RING = registerItem("mysterious_ring",
            () -> new MysteriousRingItem(new Item.Properties()
                    .stacksTo(1)
                    .rarity(Rarity.EPIC)));

    public static final DeferredItem<Item> SYSTEM = registerItem("system",
            () -> new OuterSystemItem(new Item.Properties()
                    .stacksTo(1)
                    .rarity(Rarity.EPIC)));

    public static final DeferredItem<Item> LOTTERY_TICKET =
            registerItem("lottery_ticket",
                    () -> new Item(new Item.Properties()
                            .stacksTo(64)
                            .rarity(Rarity.RARE)));

    public static final DeferredItem<Item> GREAT_CREATION_SHARD =
            registerItem("great_creation_shard",
                    () -> new GreatCreationShardItem(new Item.Properties()
                            .stacksTo(16)
                            .rarity(Rarity.EPIC)));

    public static DeferredItem<Item> registerItem(String name, Supplier<Item> itemSupplier) {
        return ITEMS.register(name, itemSupplier);
    }

    private static DeferredItem<Item> woolStrip(String name, DyeColor color) {
        return registerItem(name + "_wool_strip",
                () -> new WoolStripItem(new Item.Properties().stacksTo(64), color));
    }

    public static Item woolStrip(DyeColor color) {
        return switch (color) {
            case WHITE -> WHITE_WOOL_STRIP.get();
            case ORANGE -> ORANGE_WOOL_STRIP.get();
            case MAGENTA -> MAGENTA_WOOL_STRIP.get();
            case LIGHT_BLUE -> LIGHT_BLUE_WOOL_STRIP.get();
            case YELLOW -> YELLOW_WOOL_STRIP.get();
            case LIME -> LIME_WOOL_STRIP.get();
            case PINK -> PINK_WOOL_STRIP.get();
            case GRAY -> GRAY_WOOL_STRIP.get();
            case LIGHT_GRAY -> LIGHT_GRAY_WOOL_STRIP.get();
            case CYAN -> CYAN_WOOL_STRIP.get();
            case PURPLE -> PURPLE_WOOL_STRIP.get();
            case BLUE -> BLUE_WOOL_STRIP.get();
            case BROWN -> BROWN_WOOL_STRIP.get();
            case GREEN -> GREEN_WOOL_STRIP.get();
            case RED -> RED_WOOL_STRIP.get();
            case BLACK -> BLACK_WOOL_STRIP.get();
        };
    }

    public static class SpellScrollItem extends Item {
        private final String spellId;

        public SpellScrollItem(Properties properties, String spellId) {
            super(properties);
            this.spellId = spellId;
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, net.minecraft.world.InteractionHand hand) {
            if (level.isClientSide) {
                com.example.immortal_cultivation_mod.client.ClientPayloadHandlers.openScrollLearning(spellId);
            }
            return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
        }

        @Override
        public Component getName(ItemStack stack) {
            return super.getName(stack).copy().withStyle(elementNameColor());
        }

        private ChatFormatting elementNameColor() {
            ModSpells.SpellDef spell = ModSpells.get(spellId);
            String element = spell == null ? "" : spell.element();
            return switch (element) {
                case SpiritRoots.METAL -> ChatFormatting.GOLD;
                case SpiritRoots.WOOD -> ChatFormatting.GREEN;
                case SpiritRoots.WATER -> ChatFormatting.AQUA;
                case SpiritRoots.FIRE -> ChatFormatting.RED;
                case SpiritRoots.EARTH -> ChatFormatting.YELLOW;
                case SpiritRoots.THUNDER -> ChatFormatting.LIGHT_PURPLE;
                case SpiritRoots.ICE -> ChatFormatting.BLUE;
                case SpiritRoots.WIND -> ChatFormatting.DARK_AQUA;
                case SpiritRoots.LIGHT -> ChatFormatting.WHITE;
                case SpiritRoots.DARK -> ChatFormatting.DARK_PURPLE;
                default -> ChatFormatting.GRAY;
            };
        }

        public String spellId() {
            return spellId;
        }
    }

    public static class FireballTalismanItem extends Item {
        private static final int FIREBALL_COUNT = 8;
        private static final double RANGE = 64.0D;
        private static final double SPEED = 2.0D;
        private static final double TOP_ARC_SPAWN_RADIUS = 1.25D;
        private static final double OUTWARD_CURVE_BLOCKS = 8.0D;
        private static final int RANDOM_FIRE_TICKS = 10;
        private static final int COOLDOWN_TICKS = 5;

        public FireballTalismanItem(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (!level.isClientSide) {
                Vec3 eye = player.getEyePosition();
                Vec3 look = player.getLookAngle().normalize();
                Vec3 flatLook = new Vec3(look.x, 0.0D, look.z);
                if (flatLook.lengthSqr() < 0.001D) {
                    flatLook = Vec3.directionFromRotation(0.0F, player.getYRot());
                } else {
                    flatLook = flatLook.normalize();
                }
                Vec3 origin = player.position().add(0.0D, player.getBbHeight() * 0.65D, 0.0D).add(flatLook.scale(1.5D));
                HitResult hit = player.pick(RANGE, 0.0F, false);
                Vec3 blockTarget = hit.getType() == HitResult.Type.MISS ? eye.add(look.scale(RANGE)) : hit.getLocation();
                EntityHitResult entityHit = pickEntity(player, eye, blockTarget);
                LivingEntity targetEntity = entityHit == null
                        ? null : ModelBonePartEntity.resolveLiving(entityHit.getEntity());
                Vec3 target = targetEntity == null ? blockTarget : targetEntity.position().add(0.0D, targetEntity.getBbHeight() * 0.55D, 0.0D);
                if (targetEntity == null && target.distanceToSqr(origin) < 16.0D) {
                    target = origin.add(look.scale(16.0D));
                }

                Vec3 forward = target.subtract(origin).normalize();
                Vec3 right = forward.cross(new Vec3(0.0D, 1.0D, 0.0D));
                if (right.lengthSqr() < 0.001D) {
                    right = new Vec3(1.0D, 0.0D, 0.0D);
                } else {
                    right = right.normalize();
                }
                for (int i = 0; i < FIREBALL_COUNT; i++) {
                    double arcAngle = Math.PI * i / (FIREBALL_COUNT - 1);
                    Vec3 arcDirection = right.scale(Math.cos(arcAngle))
                            .add(0.0D, Math.sin(arcAngle), 0.0D);
                    Vec3 outward = arcDirection.normalize().scale(OUTWARD_CURVE_BLOCKS);
                    Vec3 fireballOrigin = origin.add(arcDirection.scale(TOP_ARC_SPAWN_RADIUS));
                    int launchDelay = level.random.nextInt(RANDOM_FIRE_TICKS + 1);
                    FireballTalismanProjectileEntity fireball = new FireballTalismanProjectileEntity(level, player);
                    fireball.setPos(fireballOrigin.x, fireballOrigin.y, fireballOrigin.z);
                    fireball.configure(target, outward, SPEED, launchDelay, targetEntity);
                    fireball.setDeltaMovement(outward.normalize().scale(SPEED));
                    level.addFreshEntity(fireball);
                }

                player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        private EntityHitResult pickEntity(Player player, Vec3 eye, Vec3 blockTarget) {
            double maxDistance = Math.min(RANGE, eye.distanceTo(blockTarget));
            Vec3 end = eye.add(player.getLookAngle().normalize().scale(maxDistance));
            AABB search = player.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1.0D);
            return ProjectileUtil.getEntityHitResult(player.level(), player, eye, end, search,
                    entity -> ModelBonePartEntity.resolveLiving(entity) != null
                            && entity.isPickable()
                            && ModelBonePartEntity.resolveParent(entity) != player
                            && !entity.isSpectator());
        }
    }

    public static class EnlightenmentPillItem extends Item {
        public EnlightenmentPillItem(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, net.minecraft.world.InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (level.isClientSide) {
                com.example.immortal_cultivation_mod.client.ClientPayloadHandlers.openSpiritRootSelectionOrMessage(player,
                        Component.translatable("message." + ImmortalCultivationMod.MODID + ".enlightenment_already_cultivating"));
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
    }

    public static class LevelUpItem extends Item {
        private final int progressAmount;
        private final boolean fillToRequirement;

        public LevelUpItem(Properties properties, int progressAmount, boolean fillToRequirement) {
            super(properties);
            this.progressAmount = progressAmount;
            this.fillToRequirement = fillToRequirement;
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, net.minecraft.world.InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer sp) {
                var data = ModAttachments.getData(sp);
                long need = CultivationLevels.getTotalQiNeeded(data.cultivationLevel());
                long progress = fillToRequirement ? need : Math.min(need, data.cultivationProgress() + progressAmount);
                ModAttachments.setData(sp, data.withCultivationProgress(progress));
                if (!sp.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                sp.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".level_up_progress", progress, need));
                com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
    }

    public static class QiPouchItem extends Item {
        private static final int OPEN_COST = 10;

        public QiPouchItem(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (!level.isClientSide && player instanceof ServerPlayer sp) {
                var data = ModAttachments.getData(sp);
                if (!com.example.immortal_cultivation_mod.event.ServerEvents.spendQiOrBlood(sp, data, OPEN_COST)) {
                    sp.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".not_enough_qi"));
                    return InteractionResultHolder.fail(stack);
                }

                com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
                sp.openMenu(new SimpleMenuProvider(
                        (containerId, inventory, openedPlayer) ->
                                new com.example.immortal_cultivation_mod.screen.QiPouchMenu(containerId, inventory, stack),
                        Component.translatable("container." + ImmortalCultivationMod.MODID + ".qi_pouch")
                ));
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
    }

    public static class DebugStatEditorItem extends Item {
        public DebugStatEditorItem(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                com.example.immortal_cultivation_mod.network.ModPayloads.openDebugStatEditor(
                        serverPlayer, serverPlayer);
            }
            return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
        }

        @Override
        public InteractionResult interactLivingEntity(ItemStack stack, Player player,
                                                       net.minecraft.world.entity.LivingEntity target,
                                                       InteractionHand hand) {
            if (!player.isShiftKeyDown()) {
                return super.interactLivingEntity(stack, player, target, hand);
            }

            if (target instanceof Player) {
                if (!player.level().isClientSide
                        && player instanceof ServerPlayer viewer
                        && target instanceof ServerPlayer targetPlayer) {
                    com.example.immortal_cultivation_mod.network.ModPayloads.openDebugStatEditor(
                            viewer, targetPlayer);
                }
                return InteractionResult.sidedSuccess(player.level().isClientSide);
            }

            if (target instanceof net.minecraft.world.entity.PathfinderMob mob
                    && target instanceof com.example.immortal_cultivation_mod.entity.CultivationNpc) {
                if (!player.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
                    com.example.immortal_cultivation_mod.entity.NpcProfile.openDebugRelationship(
                            mob, serverPlayer);
                }
                return InteractionResult.sidedSuccess(player.level().isClientSide);
            }
            return super.interactLivingEntity(stack, player, target, hand);
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            Player player = context.getPlayer();
            Level level = context.getLevel();
            net.minecraft.core.BlockPos pos = context.getClickedPos();
            if (player == null || !player.isShiftKeyDown()
                    || !level.getBlockState(pos).is(ModBlocks.DUNWU_STELE.get())) {
                return super.useOn(context);
            }
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                    && level.getBlockEntity(pos)
                    instanceof com.example.immortal_cultivation_mod.block.GeoRockBlockEntity stele) {
                com.example.immortal_cultivation_mod.network.ModPayloads.openDunwuSteleEditor(
                        serverPlayer, pos, stele.steleSpellId());
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

    }

    public static class CultivationMethodItem extends Item {
        private final String methodId;

        public CultivationMethodItem(Properties properties, String methodId) {
            super(properties);
            this.methodId = methodId;
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (level.isClientSide) {
                com.example.immortal_cultivation_mod.client.ClientPayloadHandlers.openMethodLearning(methodId);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
    }

    public static class BloodItem extends Item {
        public BloodItem(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (!level.isClientSide && player instanceof ServerPlayer sp) {
                var data = ModAttachments.getData(sp);
                if (!CultivationMethods.isBloodDemon(data.activeCultivationMethod())) {
                    return InteractionResultHolder.fail(stack);
                }
                int maxBlood = com.example.immortal_cultivation_mod.event.ServerEvents.getMaxBlood(sp);
                if (data.blood() >= maxBlood) {
                    return InteractionResultHolder.fail(stack);
                }
                int requested = sp.isShiftKeyDown() ? stack.getCount() : 1;
                int consumed = Math.min(requested, maxBlood - data.blood());
                ModAttachments.setData(sp, data.withBlood(data.blood() + consumed));
                if (!sp.getAbilities().instabuild) {
                    stack.shrink(consumed);
                }
                com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
    }

    public static class FogRevealingMirrorItem extends Item {
        private static final int QI_COST = 50;

        public FogRevealingMirrorItem(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (!level.isClientSide && player instanceof ServerPlayer sp) {
                var data = ModAttachments.getData(sp);
                if (!com.example.immortal_cultivation_mod.event.ServerEvents.spendQiOrBlood(sp, data, QI_COST)) {
                    sp.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".not_enough_qi"));
                    return InteractionResultHolder.fail(stack);
                }

                int found = com.example.immortal_cultivation_mod.event.ServerEvents.startFogMirrorReveal(sp);
                var ambientQi = com.example.immortal_cultivation_mod.event.ServerEvents.refreshAmbientQi(sp);
                sp.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".fog_mirror_found",
                        found, ambientQi.value(), ambientQi.spiritVeinCenters()));
                com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
    }

}
