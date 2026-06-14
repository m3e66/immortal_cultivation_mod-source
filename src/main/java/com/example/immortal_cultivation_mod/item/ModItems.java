package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
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

    public static final DeferredItem<Item> SLIDING_WATER_SCROLL = registerItem("sliding_water_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.SLIDING_WATER));

    public static final DeferredItem<Item> DINGSHEN_SCROLL = registerItem("dingshen_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.DINGSHEN));

    public static final DeferredItem<Item> YINLEI_JUE_SCROLL = registerItem("yinlei_jue_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.YINLEI_JUE));

    public static final DeferredItem<Item> WULEI_ZHENGFA_SCROLL = registerItem("wulei_zhengfa_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.WULEI_ZHENGFA));

    public static final DeferredItem<Item> ENLIGHTENMENT_PILL = registerItem("enlightenment_pill",
            () -> new EnlightenmentPillItem(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<Item> BREAKTHROUGH_PILL = registerItem("breakthrough_pill",
            () -> new Item(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<Item> SMALL_LEVEL_UP = registerItem("small_level_up",
            () -> new LevelUpItem(new Item.Properties().stacksTo(16), 100, false));

    public static final DeferredItem<Item> BIG_LEVEL_UP = registerItem("big_level_up",
            () -> new LevelUpItem(new Item.Properties().stacksTo(16), 0, true));

    public static final DeferredItem<Item> QI_POUCH = registerItem("qi_pouch",
            () -> new QiPouchItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> DEBUG_STAT_EDITOR = registerItem("debug_stat_editor",
            () -> new DebugStatEditorItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> FOG_REVEALING_MIRROR = registerItem("fog_revealing_mirror",
            () -> new FogRevealingMirrorItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> BLOOD = registerItem("blood",
            () -> new BloodItem(new Item.Properties().stacksTo(64)));

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

    public static final DeferredItem<Item> LINGJIAO_SCALE = registerItem("lingjiao_scale",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> JIUHU = registerItem("jiuhu",
            () -> new com.example.immortal_cultivation_mod.item.JiuhuItem(new Item.Properties().stacksTo(1)));

    public static DeferredItem<Item> registerItem(String name, Supplier<Item> itemSupplier) {
        return ITEMS.register(name, itemSupplier);
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
                net.minecraft.client.Minecraft.getInstance().setScreen(
                        new com.example.immortal_cultivation_mod.screen.ScrollLearningScreen(spellId));
            }
            return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
        }

        @Override
        public Component getName(ItemStack stack) {
            return super.getName(stack).copy().withStyle(elementNameColor());
        }

        private ChatFormatting elementNameColor() {
            return switch (spellId) {
                case ModSpells.FIREBALL, ModSpells.IGNITE_FLARE -> ChatFormatting.RED;
                case ModSpells.REGENERATION -> ChatFormatting.GREEN;
                case ModSpells.CLEANSE, ModSpells.DIELANG_SHIELD, ModSpells.SLIDING_WATER -> ChatFormatting.AQUA;
                case ModSpells.EARTH_ESCAPE, ModSpells.ZHENSHAN_PALM, ModSpells.DINGSHEN -> ChatFormatting.YELLOW;
                case ModSpells.LINGBENG, ModSpells.YINLEI_JUE, ModSpells.WULEI_ZHENGFA,
                     ModSpells.ABSORB_CULTIVATION, ModSpells.TUNTIAN -> ChatFormatting.LIGHT_PURPLE;
                case ModSpells.BEAM, ModSpells.SPIRIT_SIGHT, ModSpells.LIGHT_BEAM_ATTACK -> ChatFormatting.WHITE;
                case ModSpells.QI_GATHERING, ModSpells.LINGZHI_BULLET, ModSpells.WIND_BLADE,
                     ModSpells.WIND_STEP, ModSpells.YUFENG_JUE, ModSpells.SMOKE_ART -> ChatFormatting.DARK_AQUA;
                default -> ChatFormatting.GRAY;
            };
        }
    }

    public static class EnlightenmentPillItem extends Item {
        public EnlightenmentPillItem(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, net.minecraft.world.InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer sp) {
                var data = ModAttachments.getData(sp);
                if (CultivationLevels.isMortal(data.cultivationLevel())) {
                    ModAttachments.setData(sp, data.withCultivationLevel(CultivationLevels.REALM_LIANQI + CultivationLevels.STAGE_EARLY));
                    if (!sp.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                    sp.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".enlightenment_success"));
                    com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
                } else {
                    sp.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".enlightenment_already_cultivating"));
                }
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
            if (level.isClientSide) {
                net.minecraft.client.Minecraft.getInstance().setScreen(
                        new com.example.immortal_cultivation_mod.screen.DebugStatEditorScreen());
            }
            return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
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
                net.minecraft.client.Minecraft.getInstance().setScreen(
                        new com.example.immortal_cultivation_mod.screen.MethodLearningScreen(methodId));
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
                    sp.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".blood_need_method"));
                    return InteractionResultHolder.fail(stack);
                }
                int maxBlood = com.example.immortal_cultivation_mod.event.ServerEvents.getMaxBlood(sp);
                if (data.blood() >= maxBlood) {
                    return InteractionResultHolder.fail(stack);
                }
                ModAttachments.setData(sp, data.withBlood(Math.min(maxBlood, data.blood() + 1)));
                if (!sp.getAbilities().instabuild) {
                    stack.shrink(1);
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
