package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
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
                case ModSpells.CLEANSE -> ChatFormatting.AQUA;
                case ModSpells.EARTH_ESCAPE -> ChatFormatting.YELLOW;
                case ModSpells.LINGBENG -> ChatFormatting.LIGHT_PURPLE;
                case ModSpells.BEAM, ModSpells.SPIRIT_SIGHT -> ChatFormatting.WHITE;
                case ModSpells.QI_GATHERING -> ChatFormatting.DARK_AQUA;
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
                int need = CultivationLevels.getTotalQiNeeded(data.cultivationLevel());
                int progress = fillToRequirement ? need : Math.min(need, data.cultivationProgress() + progressAmount);
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
                if (data.qi() < OPEN_COST) {
                    sp.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".not_enough_qi"));
                    return InteractionResultHolder.fail(stack);
                }

                ModAttachments.setData(sp, data.withQi(data.qi() - OPEN_COST));
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
                if (data.qi() < QI_COST) {
                    sp.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".not_enough_qi"));
                    return InteractionResultHolder.fail(stack);
                }

                ModAttachments.setData(sp, data.withQi(data.qi() - QI_COST));
                int found = com.example.immortal_cultivation_mod.event.ServerEvents.startFogMirrorReveal(sp);
                sp.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".fog_mirror_found", found));
                com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
    }

}
