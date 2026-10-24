package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.item.ModTabs;
import com.example.immortal_cultivation_mod.mixin.CreativeModeInventoryScreenAccessor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Adds a second, vertical level of navigation to the mod's creative tabs.
 * Main tabs stay in Minecraft's normal tab row; these buttons filter the
 * selected main tab without registering fake extra creative tabs.
 */
public final class CreativeSubtabs {
    private static final int BUTTON_SIZE = 20;
    private static final int ARROW_HEIGHT = 14;
    private static final int VISIBLE_SUBTABS = 5;
    private static final int PANEL_GAP = 2;

    private static final Map<CreativeModeInventoryScreen, State> STATES = new WeakHashMap<>();
    private static final Map<ResourceLocation, Integer> SELECTED_BY_TAB = new HashMap<>();
    private static final Map<ResourceLocation, Integer> OFFSET_BY_TAB = new HashMap<>();

    private static final Set<String> WEAPONS = Set.of(
            "taomu_sword", "copper_coin_sword", "flying_sword", "dao",
            "jingtie_spear", "throwing_knife", "flying_needle", "tiegu_shan",
            "fuchen", "whip", "heavy_iron_bracer", "yaoyuan_fist_bone", "shihunfan",
            "ritual_dagger", "black_iron_jian", "black_iron_greatsword", "moon_crescent_halberd", "spirit_silk_whip", "soul_hook", "wind_feather_fan", "thunderwood_sword", "flame_pattern_dao", "earthbreaker_hammer", "seven_star_sword");
    private static final Set<String> TOOLS = Set.of(
            "modou_line", "fog_revealing_mirror", "yinyang_compass", "zhenhun_bell",
            "yinhun_gong", "yuanhun_bottle",
            "low_yangjian_stone", "middle_yangjian_stone", "high_yangjian_stone",
            "low_sword_grindstone", "middle_sword_grindstone", "high_sword_grindstone",
            "low_saber_grindstone", "middle_saber_grindstone", "high_saber_grindstone");
    private static final Set<String> WEAPON_RECIPES = Set.of(
            "sword_mold", "saber_mold", "spear_mold", "weapon_mold",
            "qingtong_yinhuo_ding_yujian", "chi_jin_sanyang_ding_yujian",
            "feijian_yujian", "dao_yujian", "jingtie_spear_yujian",
            "heavy_iron_bracer_yujian", "throwing_knife_yujian", "flying_needle_yujian",
            "tiegu_shan_yujian", "fuchen_yujian", "whip_yujian",
            "zhenhun_bell_yujian", "yinhun_gong_yujian", "fog_revealing_mirror_yujian",
            "yinyang_compass_yujian", "yuanhun_bottle_yujian", "black_iron_jian_yujian", "black_iron_greatsword_yujian", "moon_crescent_halberd_yujian", "spirit_silk_whip_yujian", "soul_hook_yujian", "wind_feather_fan_yujian", "thunderwood_sword_yujian", "flame_pattern_dao_yujian", "earthbreaker_hammer_yujian", "seven_star_sword_yujian");
    private static final Set<String> ACCESSORY_HEAD = Set.of(
            "blindfold", "blindfold2", "straw_hat", "noob_hat", "disguise_mask", "coin_mask", "miansha", "jiangpa", "futou", "wusha_hat", "song_wusha_hat", "chanfu_jin", "embroidered_forehead_band", "veiled_douli");
    private static final Set<String> ACCESSORY_BOOTS = Set.of("fangkou_qiaojian_lv");
    private static final Set<String> ACCESSORY_INNER = Set.of("zhongyi", "shenyi", "hufu");
    private static final Set<String> ACCESSORY_OUTER = Set.of("daofu_chestplate", "yunjian", "cultivation_fapao", "yuanling_pao");
    private static final Set<String> ACCESSORY_CLOAK = Set.of("doupeng", "hechang");
    private static final Set<String> ACCESSORY_BACK = Set.of(
            "coffin", "flag", "cloth_sheath", "sword_sheath", "dao_sheath", "sword_case", "xiujian");
    private static final Set<String> ACCESSORY_WAIST = Set.of(
            "jade_pendant", "protective_jade_pendant", "black_tortoise_heartguard_mirror",
            "zhenhun_bell", "yinhun_gong");
    private static final Set<String> ACCESSORY_SYSTEM = Set.of("system");
    private static final Set<String> ACCESSORY_HAIR = Set.of(
            "male_short_layered_hair", "male_long_cultivation_hair", "male_high_ponytail_hair",
            "male_messy_medium_hair", "male_slicked_back_hair", "male_half_up_long_hair",
            "male_warrior_ponytail_hair", "male_loose_shoulder_length_hair", "male_topknot_hair",
            "male_long_sect_disciple_hair", "female_long_straight_hair", "female_high_ponytail_hair",
            "female_twin_ponytail_hair", "female_half_up_cultivation_hair", "female_long_side_bangs_hair",
            "female_braided_long_hair", "female_double_bun_hair", "female_low_ponytail_hair",
            "female_flowing_shoulder_length_hair", "female_immortal_cultivator_hair");
    private static final Set<String> ACCESSORY_HAIR_ACCESSORIES = Set.of(
            "cultivation_hairpin", "hair_ribbon", "hair_crown", "jade_hair_ornament", "decorative_hair_beads");
    private static final Set<String> SPIRIT_STONES = Set.of(
            "spirit_stone", "medium_spirit_stone", "high_spirit_stone", "infinite_spirit_stone");
    private static final Set<String> HERBS = Set.of(
            "qingling_grass_seeds", "chiyan_flower_seeds", "ninglu_zhi_seeds", "xuanbing_lotus_seeds",
            "ziyun_vine_seeds", "shihun_grass_seeds", "fugu_tai_seeds", "huanxia_si_seeds", "spirit_farmland",
            "qingling_grass", "chiyan_flower", "ninglu_zhi", "xuanbing_lotus",
            "ziyun_vine", "jinwen_fruit", "shihun_grass", "jiuye_xianlan", "fugu_tai", "huanxia_si",
            "lingquan_leaf", "ningling_flower", "harmonizing_herb", "catalyst_blossom");
    private static final Set<String> BLOCK_PLANTS = Set.of(
            "spirit_farmland",
            "nuomi_plant", "qingling_grass", "chiyan_flower", "ninglu_zhi", "xuanbing_lotus",
            "ziyun_vine", "jinwen_fruit", "shihun_grass", "jiuye_xianlan",
            "lingmu", "lingmu_log", "lingmu_leaves", "fugu_tai", "huanxia_si");
    private static final Set<String> BLOCK_ORES = Set.of(
            "qiannian_lingmu_heart", "jingtie_motherstone", "yunwen_silver_crystal",
            "chitong_sui", "hanpo_jade", "moxuan_iron", "liujin_sand",
            "bisui_stone", "star_dust_stone", "void_stone", "spirit_ore");
    private static final Set<String> BLOCK_TABLES = Set.of(
            "cu_tie_xiaoding", "qingtong_yinhuo_ding", "chi_jin_sanyang_ding", "restriction_table",
            "talisman_drawing_table", "medicine_cabinet_small", "medicine_cabinet_large");
    private static final Set<String> CULTIVATOR_EGGS = Set.of(
            "lianqi_cultivator_spawn_egg", "zhuji_cultivator_spawn_egg",
            "jindan_cultivator_spawn_egg",
            "fanren_npc_spawn_egg", "hotel_npc_spawn_egg");

    private CreativeSubtabs() {
    }

    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof CreativeModeInventoryScreen screen)) {
            return;
        }

        State state = new State(screen);
        STATES.put(screen, state);
        event.addListener(state.up);
        for (SubtabButton button : state.buttons) {
            event.addListener(button);
        }
        event.addListener(state.down);
        state.sync(true);
    }

    public static void onScreenRenderPre(ScreenEvent.Render.Pre event) {
        if (!(event.getScreen() instanceof CreativeModeInventoryScreen screen)) {
            return;
        }
        State state = STATES.get(screen);
        if (state != null) {
            state.sync(false);
        }
    }

    private static List<Subtab> definitionsFor(CreativeModeTab tab) {
        if (tab == ModTabs.IMMORTAL_TALISMANS_TAB.get()) {
            return List.of(
                    subtab("talisman.material", () -> ModItems.ORDINARY_YELLOW_TALISMAN_PAPER.get().getDefaultInstance(),
                            path -> isTalismanMaterial(path)),
                    subtab("talisman.talisman", () -> ModItems.ZHENSHI_TALISMAN.get().getDefaultInstance(),
                            path -> !isTalismanMaterial(path) && !isTalismanRecipe(path)),
                    recipeSubtab("talisman.recipes", CreativeSubtabs::isTalismanRecipe));
        }
        if (tab == ModTabs.IMMORTAL_PILLS_TAB.get()) {
            return List.of(
                    subtab("pills.pills", () -> ModItems.BREAKTHROUGH_PILL.get().getDefaultInstance(),
                            path -> !HERBS.contains(path) && !isPillRecipe(path)),
                    subtab("pills.herbs", () -> com.example.immortal_cultivation_mod.block.ModBlocks.QINGLING_GRASS
                            .get().asItem().getDefaultInstance(), HERBS::contains),
                    recipeSubtab("pills.recipes", CreativeSubtabs::isPillRecipe));
        }
        if (tab == ModTabs.IMMORTAL_CULTIVATION_TAB.get()) {
            return List.of(
                    subtab("cultivation.methods", () -> ModItems.BASIC_BREATHING_METHOD.get().getDefaultInstance(),
                            CreativeSubtabs::isCultivationMethod),
                    subtab("cultivation.spell_scrolls", () -> ModItems.FIREBALL_SCROLL.get().getDefaultInstance(),
                            path -> path.endsWith("_scroll")));
        }
        if (tab == ModTabs.IMMORTAL_WEAPONS_TAB.get()) {
            return List.of(
                    subtab("weapons.weapons", () -> ModItems.FLYING_SWORD.get().getDefaultInstance(), WEAPONS::contains),
                    subtab("weapons.tools", () -> ModItems.FOG_REVEALING_MIRROR.get().getDefaultInstance(),
                            TOOLS::contains),
                    recipeSubtab("weapons.recipes", WEAPON_RECIPES::contains));
        }
        if (tab == ModTabs.IMMORTAL_ACCESSORIES_TAB.get()) {
            Set<String> customSlotItems = new HashSet<>();
            customSlotItems.addAll(ACCESSORY_HEAD);
            customSlotItems.addAll(ACCESSORY_BOOTS);
            customSlotItems.addAll(ACCESSORY_INNER);
            customSlotItems.addAll(ACCESSORY_OUTER);
            customSlotItems.addAll(ACCESSORY_CLOAK);
            customSlotItems.addAll(ACCESSORY_BACK);
            customSlotItems.addAll(ACCESSORY_WAIST);
            customSlotItems.addAll(ACCESSORY_SYSTEM);
            customSlotItems.addAll(ACCESSORY_HAIR);
            customSlotItems.addAll(ACCESSORY_HAIR_ACCESSORIES);
            return List.of(
                    subtab("accessory.hair", () -> Items.STRING.getDefaultInstance(), ACCESSORY_HAIR::contains),
                    subtab("accessory.hair_accessory", () -> ModItems.RED_STRING.get().getDefaultInstance(), ACCESSORY_HAIR_ACCESSORIES::contains),
                    subtab("accessory.head_accessory", () -> ModItems.STRAW_HAT.get().getDefaultInstance(), ACCESSORY_HEAD::contains),
                    subtab("accessory.neck_accessory", () -> Items.CHAIN.getDefaultInstance(), path -> false),
                    subtab("accessory.inner_clothes", () -> ModItems.ZHONGYI.get().getDefaultInstance(), ACCESSORY_INNER::contains),
                    subtab("accessory.outer_clothes", () -> ModItems.DAOFU_CHESTPLATE.get().getDefaultInstance(), ACCESSORY_OUTER::contains),
                    subtab("accessory.boots", () -> ModItems.FANGKOU_QIAOJIAN_LV.get().getDefaultInstance(), ACCESSORY_BOOTS::contains),
                    subtab("accessory.cloak", () -> ModItems.DOUPENG.get().getDefaultInstance(), ACCESSORY_CLOAK::contains),
                    subtab("accessory.back_accessory", () -> ModItems.COFFIN.get().getDefaultInstance(), ACCESSORY_BACK::contains),
                    subtab("accessory.waist_accessory", () -> ModItems.JADE_PENDANT.get().getDefaultInstance(),
                            ACCESSORY_WAIST::contains),
                    subtab("accessory.system", () -> ModItems.SYSTEM.get().getDefaultInstance(), ACCESSORY_SYSTEM::contains),
                    subtab("accessory.others", () -> ModItems.QI_POUCH.get().getDefaultInstance(),
                            path -> !customSlotItems.contains(path)));
        }
        if (tab == ModTabs.IMMORTAL_MAGIC_CIRCLE_TAB.get()) {
            return List.of(
                    subtab("magic_circle.spirit_stones",
                            () -> com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_STONE
                                    .get().getDefaultInstance(), SPIRIT_STONES::contains),
                    subtab("magic_circle.zhenpan_flags",
                            () -> com.example.immortal_cultivation_mod.magicritual.item.ModItems.ZHENPAN
                                    .get().getDefaultInstance(),
                            path -> !SPIRIT_STONES.contains(path) && !path.endsWith("_yujian")),
                    recipeSubtab("magic_circle.recipes", path -> path.endsWith("_yujian")));
        }
        if (tab == ModTabs.IMMORTAL_BLOCKS_TAB.get()) {
            Set<String> categorized = new HashSet<>();
            categorized.addAll(BLOCK_PLANTS);
            categorized.addAll(BLOCK_ORES);
            categorized.addAll(BLOCK_TABLES);
            return List.of(
                    subtab("blocks.plants", () -> com.example.immortal_cultivation_mod.block.ModBlocks.QINGLING_GRASS
                            .get().asItem().getDefaultInstance(), BLOCK_PLANTS::contains),
                    subtab("blocks.ores", () -> com.example.immortal_cultivation_mod.block.ModBlocks.JINGTIE_MOTHERSTONE
                            .get().asItem().getDefaultInstance(), BLOCK_ORES::contains),
                    subtab("blocks.tables", () -> com.example.immortal_cultivation_mod.block.ModBlocks.TALISMAN_DRAWING_TABLE
                            .get().asItem().getDefaultInstance(), BLOCK_TABLES::contains),
                    subtab("blocks.others", () -> com.example.immortal_cultivation_mod.block.ModBlocks.DUNWU_STELE
                            .get().asItem().getDefaultInstance(), path -> !categorized.contains(path)));
        }
        if (tab == ModTabs.IMMORTAL_SPAWN_EGGS_TAB.get()) {
            return List.of(
                    subtab("spawn_eggs.cultivator", () -> ModItems.LIANQI_CULTIVATOR_SPAWN_EGG
                            .get().getDefaultInstance(), CULTIVATOR_EGGS::contains),
                    subtab("spawn_eggs.beast", () -> ModItems.THUNDERHORN_ROCK_BADGER_SPAWN_EGG
                            .get().getDefaultInstance(), path -> !CULTIVATOR_EGGS.contains(path)));
        }
        return List.of();
    }

    private static boolean isPillRecipe(String path) {
        return path.endsWith("_formula") || path.equals("jade_pill_bottle_yujian");
    }

    private static boolean isTalismanMaterial(String path) {
        return path.equals("ordinary_yellow_talisman_paper")
                || path.equals("red_veined_mulberry_paper")
                || path.endsWith("_talisman_paper")
                || path.endsWith("_ink");
    }

    private static boolean isTalismanRecipe(String path) {
        return path.equals("talisman_formula") || path.endsWith("_formula");
    }

    private static boolean isCultivationMethod(String path) {
        return path.endsWith("_method")
                || path.equals("corpse_refining_secret");
    }

    private static Subtab subtab(String key, Supplier<ItemStack> fallbackIcon, Predicate<String> matcher) {
        return new Subtab(key, fallbackIcon, matcher, false);
    }

    private static Subtab recipeSubtab(String key, Predicate<String> matcher) {
        return new Subtab(key, () -> ModItems.FEIJIAN_YUJIAN.get().getDefaultInstance(), matcher, true);
    }

    private static String itemPath(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
    }

    private record Subtab(String key, Supplier<ItemStack> fallbackIcon, Predicate<String> matcher,
                          boolean fixedIcon) {
        private Component label() {
            return Component.translatable("creative_subtab." + ImmortalCultivationMod.MODID + "." + key);
        }

        private boolean accepts(ItemStack stack) {
            return matcher.test(itemPath(stack));
        }
    }

    private static final class State {
        private final CreativeModeInventoryScreen screen;
        private final ArrowButton up;
        private final ArrowButton down;
        private final List<SubtabButton> buttons = new ArrayList<>(VISIBLE_SUBTABS);
        private CreativeModeTab currentTab;
        private List<Subtab> subtabs = List.of();
        private int selected;
        private int offset;

        private State(CreativeModeInventoryScreen screen) {
            this.screen = screen;
            int x = screen.getGuiLeft() - BUTTON_SIZE - PANEL_GAP;
            int y = screen.getGuiTop() + 2;
            this.up = new ArrowButton(x, y, true, this);
            for (int slot = 0; slot < VISIBLE_SUBTABS; slot++) {
                buttons.add(new SubtabButton(x, y + ARROW_HEIGHT + slot * BUTTON_SIZE, slot, this));
            }
            this.down = new ArrowButton(
                    x,
                    y + ARROW_HEIGHT + VISIBLE_SUBTABS * BUTTON_SIZE,
                    false,
                    this);
        }

        private void sync(boolean force) {
            CreativeModeTab selectedTab = CreativeModeInventoryScreenAccessor
                    .immortalCultivation$getSelectedTab();
            List<Subtab> definitions = definitionsFor(selectedTab);
            if (selectedTab != currentTab) {
                currentTab = selectedTab;
                subtabs = definitions;
                ResourceLocation tabId = tabId(selectedTab);
                selected = Mth.clamp(SELECTED_BY_TAB.getOrDefault(tabId, 0), 0,
                        Math.max(0, subtabs.size() - 1));
                offset = Mth.clamp(OFFSET_BY_TAB.getOrDefault(tabId, 0), 0, maxOffset());
                keepSelectionVisible();
                force = true;
            }

            boolean shown = !subtabs.isEmpty();
            up.visible = shown;
            down.visible = shown;
            up.active = shown && offset > 0;
            down.active = shown && offset < maxOffset();
            for (int slot = 0; slot < buttons.size(); slot++) {
                SubtabButton button = buttons.get(slot);
                int index = offset + slot;
                button.visible = shown && index < subtabs.size();
                button.active = button.visible;
                if (button.visible) {
                    button.setSubtab(index, subtabs.get(index), iconFor(index));
                }
            }

            if (shown && (force || !contentsMatch())) {
                applySelected();
            }
        }

        private ResourceLocation tabId(CreativeModeTab tab) {
            return BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
        }

        private int maxOffset() {
            return Math.max(0, subtabs.size() - VISIBLE_SUBTABS);
        }

        private void keepSelectionVisible() {
            if (selected < offset) {
                offset = selected;
            } else if (selected >= offset + VISIBLE_SUBTABS) {
                offset = selected - VISIBLE_SUBTABS + 1;
            }
            offset = Mth.clamp(offset, 0, maxOffset());
        }

        private void moveOffset(int direction) {
            offset = Mth.clamp(offset + direction, 0, maxOffset());
            OFFSET_BY_TAB.put(tabId(currentTab), offset);
            sync(false);
        }

        private void select(int index) {
            if (index < 0 || index >= subtabs.size()) {
                return;
            }
            selected = index;
            SELECTED_BY_TAB.put(tabId(currentTab), selected);
            applySelected();
            sync(false);
        }

        private boolean isSelected(int index) {
            return selected == index;
        }

        private List<ItemStack> selectedItems() {
            if (subtabs.isEmpty()) {
                return List.of();
            }
            Subtab subtab = subtabs.get(selected);
            return currentTab.getDisplayItems().stream()
                    .filter(subtab::accepts)
                    .toList();
        }

        private ItemStack iconFor(int index) {
            Subtab subtab = subtabs.get(index);
            if (subtab.fixedIcon()) {
                return subtab.fallbackIcon().get().copy();
            }
            return currentTab.getDisplayItems().stream()
                    .filter(subtab::accepts)
                    .findFirst()
                    .map(ItemStack::copy)
                    .orElseGet(() -> subtab.fallbackIcon().get());
        }

        private boolean contentsMatch() {
            List<ItemStack> desired = selectedItems();
            List<ItemStack> actual = screen.getMenu().items;
            if (desired.size() != actual.size()) {
                return false;
            }
            for (int i = 0; i < desired.size(); i++) {
                if (!ItemStack.matches(desired.get(i), actual.get(i))) {
                    return false;
                }
            }
            return true;
        }

        private void applySelected() {
            screen.getMenu().items.clear();
            screen.getMenu().items.addAll(selectedItems());
            ((CreativeModeInventoryScreenAccessor) screen)
                    .immortalCultivation$setScrollOffset(0.0F);
            screen.getMenu().scrollTo(0.0F);
        }
    }

    private static final class SubtabButton extends AbstractButton {
        private final int visibleSlot;
        private final State state;
        private int subtabIndex;
        private ItemStack icon = ItemStack.EMPTY;

        private SubtabButton(int x, int y, int visibleSlot, State state) {
            super(x, y, BUTTON_SIZE, BUTTON_SIZE, Component.empty());
            this.visibleSlot = visibleSlot;
            this.state = state;
        }

        private void setSubtab(int subtabIndex, Subtab subtab, ItemStack icon) {
            this.subtabIndex = subtabIndex;
            this.icon = icon;
            setMessage(subtab.label());
            setTooltip(Tooltip.create(subtab.label()));
        }

        @Override
        public void onPress() {
            state.select(state.offset + visibleSlot);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean selected = state.isSelected(subtabIndex);
            int border = selected ? 0xFFF1C76B : isHoveredOrFocused() ? 0xFFF4F4F4 : 0xFF777777;
            int fill = selected ? 0xE0413520 : 0xE0202020;
            graphics.fill(getX(), getY(), getX() + width, getY() + height, border);
            graphics.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, fill);
            graphics.renderItem(icon, getX() + 2, getY() + 2);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    private static final class ArrowButton extends AbstractButton {
        private final boolean up;
        private final State state;

        private ArrowButton(int x, int y, boolean up, State state) {
            super(x, y, BUTTON_SIZE, ARROW_HEIGHT,
                    Component.translatable("creative_subtab." + ImmortalCultivationMod.MODID
                            + (up ? ".scroll_up" : ".scroll_down")));
            this.up = up;
            this.state = state;
            setTooltip(Tooltip.create(getMessage()));
        }

        @Override
        public void onPress() {
            state.moveOffset(up ? -1 : 1);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int border = isHoveredOrFocused() && active ? 0xFFF4F4F4 : 0xFF686868;
            int fill = active ? 0xE0282828 : 0xD0161616;
            graphics.fill(getX(), getY(), getX() + width, getY() + height, border);
            graphics.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, fill);
            int color = active ? 0xFFFFFFFF : 0xFF777777;
            graphics.drawCenteredString(Minecraft.getInstance().font, up ? "▲" : "▼",
                    getX() + width / 2, getY() + 3, color);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
