package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.network.OuterSystemPayloads;
import com.example.immortal_cultivation_mod.network.ShopCatalogPayloads;
import com.example.immortal_cultivation_mod.client.OuterSystemClient;
import com.example.immortal_cultivation_mod.system.OuterSystemType;
import com.example.immortal_cultivation_mod.system.SystemShop;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

public class SystemScreen extends Screen {
    private static final float DEFAULT_UI_SCALE = 0.82F;
    private static final int SCREEN_MARGIN = 12;
    private static final int NORMAL_W = 480;
    private static final int NORMAL_H = 302;
    private static final int SHOP_W = 500;
    private static final int SHOP_H = 326;
    private static final int SHOP_ROWS = 7;
    private static final int SHOP_ROW_H = 24;
    private static final int SHOP_SCROLLBAR_W = 14;
    private static final int SHOP_CATEGORY_COLUMNS = 5;
    private static final int SHOP_CATEGORY_BUTTON_W = 88;
    private static final int SHOP_CATEGORY_BUTTON_H = 16;
    private static final int SHOP_CATEGORY_GAP = 4;
    private static final int SHOP_CATEGORY_ROW_GAP = 2;
    private static final int GACHA_W = 620;
    private static final int GACHA_H = 390;
    private static final int GACHA_ROWS = 7;
    private static final int GACHA_ROW_H = 29;

    private CompoundTag state;
    private final List<ShopEntry> catalog = new ArrayList<>();
    private final Map<String, ShopEntry> catalogById = new HashMap<>();
    private boolean catalogReady;
    private SystemGuiButton claimButton;
    private SystemGuiButton shopActionButton;
    private SystemGuiButton buyModeButton;
    private SystemGuiButton recycleModeButton;
    private SystemGuiButton requestModeButton;
    private SystemGuiButton gachaDrawButton;
    private SystemGuiButton gachaClaimButton;
    private final List<SystemGuiButton> categoryButtons =
            new ArrayList<>();
    private EditBox searchBox;
    private String shopSearch = "";
    private ShopMode shopMode = ShopMode.BUY;
    private ShopCategory shopCategory = ShopCategory.ALL;
    private int scrollOffset;
    private int gachaScrollOffset;
    private int selectedIndex = -1;
    private ItemStack hoveredStack = ItemStack.EMPTY;
    private final SystemWindowState windowState;

    public SystemScreen(CompoundTag state) {
        super(Component.translatable(
                "screen.immortal_cultivation_mod.system.title"));
        this.state = state.copy();
        catalogReady = !state.hasUUID("ShopCatalogId");
        this.windowState = new SystemWindowState(
                baseWindowWidth(), baseWindowHeight());
        loadCatalog();
    }

    public void updateState(CompoundTag updated) {
        CompoundTag merged = updated.copy();
        boolean sameCatalog = state.hasUUID("ShopCatalogId") && updated.hasUUID("ShopCatalogId")
                && state.getUUID("ShopCatalogId").equals(updated.getUUID("ShopCatalogId"));
        if (updated.hasUUID("ShopCatalogId")) {
            if (sameCatalog) {
                // Client-owned stock tables do not travel in metadata or get copied every tick.
                merged.put("ShopPurchases", state.getCompound("ShopPurchases"));
                merged.put("ShopUniqueBought", state.getList("ShopUniqueBought", Tag.TAG_STRING));
            } else {
                catalog.clear(); catalogById.clear(); selectedIndex = -1; catalogReady = false;
            }
        }
        state = merged;
        if (updated.contains("ShopCatalog", Tag.TAG_LIST)) loadCatalog();
        clampSelection();
        updateButtons();
    }

    public boolean acceptsShopTransfer(UUID id) {
        return isShop() && state.hasUUID("ShopCatalogId") && state.getUUID("ShopCatalogId").equals(id);
    }

    public void installShopCatalog(List<ShopCatalogPayloads.Entry> entries) {
        if (!isShop()) return;
        catalog.clear(); catalogById.clear();
        CompoundTag purchases = new CompoundTag(); ListTag unique = new ListTag();
        for (var encoded : entries) {
            Item item = BuiltInRegistries.ITEM.byId(encoded.itemId());
            if (item == null || item == Items.AIR) continue;
            String id = BuiltInRegistries.ITEM.getKey(item).toString();
            ShopEntry entry = new ShopEntry(id, item, encoded.price(), SystemShop.StockTier.values()[encoded.rarity()].id(),
                    SystemShop.Category.values()[encoded.category()].id());
            catalog.add(entry); catalogById.put(id, entry);
            if (encoded.purchased() > 0) purchases.putInt(id, encoded.purchased());
            if (encoded.uniqueBought()) unique.add(StringTag.valueOf(id));
        }
        state.put("ShopPurchases", purchases); state.put("ShopUniqueBought", unique);
        catalogReady = true; clampSelection(); updateButtons();
    }

    public void applyShopStock(List<ShopCatalogPayloads.Stock> updates) {
        if (!catalogReady || !isShop()) return;
        CompoundTag purchases = state.getCompound("ShopPurchases");
        ListTag unique = state.getList("ShopUniqueBought", Tag.TAG_STRING);
        for (var update : updates) {
            Item item = BuiltInRegistries.ITEM.byId(update.itemId());
            if (item == null || item == Items.AIR) continue;
            String id = BuiltInRegistries.ITEM.getKey(item).toString();
            if (!catalogById.containsKey(id)) continue;
            if (update.purchased() == 0) purchases.remove(id); else purchases.putInt(id, update.purchased());
            if (update.uniqueBought()) {
                if (!containsString(unique,id)) unique.add(StringTag.valueOf(id));
            } else {
                for (int i=unique.size()-1;i>=0;i--) if (unique.getString(i).equals(id)) unique.remove(i);
            }
        }
        state.put("ShopPurchases",purchases); state.put("ShopUniqueBought",unique); updateButtons();
    }

    @Override
    protected void init() {
        windowState.initializeCentered(desktopWidth(), desktopHeight());
        int x = left();
        int y = top();
        categoryButtons.clear();
        claimButton = null;
        shopActionButton = null;
        buyModeButton = null;
        recycleModeButton = null;
        requestModeButton = null;
        gachaDrawButton = null;
        gachaClaimButton = null;
        searchBox = null;
        if (windowState.isMinimized()) {
            return;
        }
        claimButton = addRenderableWidget(new SystemGuiButton(
                        x + baseWindowWidth() - 106,
                        y + baseWindowHeight() - 36,
                        86,
                        18,
                        Component.translatable(
                                "screen.immortal_cultivation_mod.system.claim"),
                        () -> PacketDistributor.sendToServer(
                                new OuterSystemPayloads.ServerboundClaimQuest())));
        if (isShop()) {
            initShopWidgets(x, y);
        } else if (isGacha()) {
            initGachaWidgets(x, y);
        }
        updateButtons();
    }

    private void initGachaWidgets(int x, int y) {
        int footerY = y + GACHA_H - 33;
        gachaDrawButton = addRenderableWidget(new SystemGuiButton(
                        x + 20,
                        footerY,
                        116,
                        18,
                        Component.empty(),
                        () -> PacketDistributor.sendToServer(
                                new OuterSystemPayloads.ServerboundGachaDraw())));
        gachaClaimButton = addRenderableWidget(new SystemGuiButton(
                        x + GACHA_W - 136,
                        footerY,
                        116,
                        18,
                        Component.translatable(
                                "screen.immortal_cultivation_mod.system.gacha.claim"),
                        () -> PacketDistributor.sendToServer(
                                new OuterSystemPayloads.ServerboundGachaClaim())));
    }

    private void initShopWidgets(int x, int y) {
        searchBox = new EditBox(
                font,
                x + 20,
                y + 43,
                210,
                18,
                Component.translatable(
                        "screen.immortal_cultivation_mod.system.shop.search"));
        searchBox.setHint(Component.translatable(
                "screen.immortal_cultivation_mod.system.shop.search"));
        searchBox.setBordered(false);
        searchBox.setTextColor(0xFF101010);
        searchBox.setTextColorUneditable(0xFF707070);
        searchBox.setValue(shopSearch);
        searchBox.setResponder(value -> {
            shopSearch = value;
            resetList();
        });
        addRenderableWidget(searchBox);

        ShopCategory[] categories = ShopCategory.values();
        for (int i = 0; i < categories.length; i++) {
            ShopCategory category = categories[i];
            int categoryX = x + 20 + (i % SHOP_CATEGORY_COLUMNS)
                    * (SHOP_CATEGORY_BUTTON_W + SHOP_CATEGORY_GAP);
            int categoryY = y + 68 + (i / SHOP_CATEGORY_COLUMNS)
                    * (SHOP_CATEGORY_BUTTON_H + SHOP_CATEGORY_ROW_GAP);
            SystemGuiButton button =
                    addRenderableWidget(new SystemGuiButton(
                            categoryX,
                            categoryY,
                            SHOP_CATEGORY_BUTTON_W,
                            SHOP_CATEGORY_BUTTON_H,
                            category.label(),
                            () -> {
                                shopCategory = category;
                                resetList();
                            }));
            categoryButtons.add(button);
        }

        int footerY = y + SHOP_H - 29;
        buyModeButton = addRenderableWidget(new SystemGuiButton(
                        x + 20,
                        footerY,
                        62,
                        16,
                        Component.empty(),
                        () -> setShopMode(ShopMode.BUY)));
        recycleModeButton = addRenderableWidget(new SystemGuiButton(
                        x + 86,
                        footerY,
                        70,
                        16,
                        Component.empty(),
                        () -> setShopMode(ShopMode.RECYCLE)));
        requestModeButton = addRenderableWidget(new SystemGuiButton(
                        x + 160,
                        footerY,
                        88,
                        16,
                        Component.empty(),
                        () -> setShopMode(ShopMode.REQUEST)));
        shopActionButton = addRenderableWidget(new SystemGuiButton(
                        x + SHOP_W - 108,
                        footerY,
                        88,
                        16,
                        Component.empty(),
                        this::performShopAction));
    }

    private void updateButtons() {
        if (claimButton != null) {
            boolean daily = OuterSystemType.DAILY_SIGN_IN.id().equals(
                    state.getString("Type"));
            claimButton.visible = daily;
            claimButton.active =
                    daily && state.getBoolean("QuestCompleted");
        }
        if (isGacha() && gachaDrawButton != null) {
            int freeDraws = state.getInt("GachaFreeDraws");
            int tickets = state.getInt("GachaTickets");
            gachaDrawButton.setMessage(Component.translatable(
                    freeDraws > 0
                            ? "screen.immortal_cultivation_mod.system.gacha.draw_free"
                            : "screen.immortal_cultivation_mod.system.gacha.draw_ticket"));
            gachaDrawButton.active = freeDraws > 0 || tickets > 0;
            gachaClaimButton.active = !gachaRewards().isEmpty();
            return;
        }
        if (!isShop() || shopActionButton == null) {
            return;
        }
        boolean buying = shopMode == ShopMode.BUY;
        searchBox.visible = buying;
        searchBox.setEditable(buying);
        for (int i = 0; i < categoryButtons.size(); i++) {
            SystemGuiButton button = categoryButtons.get(i);
            button.visible = buying;
            button.active = ShopCategory.values()[i] != shopCategory;
        }
        buyModeButton.setMessage(shopModeLabel(
                ShopMode.BUY, shopMode == ShopMode.BUY));
        recycleModeButton.setMessage(shopModeLabel(
                ShopMode.RECYCLE, shopMode == ShopMode.RECYCLE));
        requestModeButton.setMessage(shopModeLabel(
                ShopMode.REQUEST, shopMode == ShopMode.REQUEST));
        shopActionButton.setMessage(shopMode.actionLabel());
        shopActionButton.active = selectedActionAvailable();
    }

    private boolean selectedActionAvailable() {
        if (selectedIndex < 0 || selectedIndex >= currentListSize()) {
            return false;
        }
        return switch (shopMode) {
            case BUY -> {
                ShopEntry entry = filteredCatalog().get(selectedIndex);
                yield stock(entry) != 0
                        && state.getLong("ShopPoints") >= entry.price();
            }
            case RECYCLE -> true;
            case REQUEST ->
                    !requests().get(selectedIndex).complete();
        };
    }

    private Component shopModeLabel(ShopMode mode, boolean selected) {
        Component label = mode.label();
        return selected
                ? Component.literal("> ").append(label).append(" <")
                : label;
    }

    private void setShopMode(ShopMode mode) {
        shopMode = mode;
        resetList();
    }

    private void resetList() {
        scrollOffset = 0;
        selectedIndex = -1;
        updateButtons();
    }

    private void clampSelection() {
        int size = currentListSize();
        int maxScroll = Math.max(0, size - SHOP_ROWS);
        scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll);
        if (selectedIndex >= size) {
            selectedIndex = -1;
        }
    }

    private void performShopAction() {
        performShopAction(hasShiftDown());
    }

    private void performShopAction(boolean buyStack) {
        if (selectedIndex < 0) {
            return;
        }
        switch (shopMode) {
            case BUY -> {
                List<ShopEntry> entries = filteredCatalog();
                if (selectedIndex < entries.size()) {
                    PacketDistributor.sendToServer(
                            new OuterSystemPayloads.ServerboundShopBuy(
                                    entries.get(selectedIndex).id(), buyStack));
                }
            }
            case RECYCLE -> {
                List<InventoryEntry> entries = recyclableInventory();
                if (selectedIndex < entries.size()) {
                    PacketDistributor.sendToServer(
                            new OuterSystemPayloads.ServerboundShopRecycle(
                                    entries.get(selectedIndex).slot()));
                }
            }
            case REQUEST -> {
                List<RequestEntry> entries = requests();
                if (selectedIndex < entries.size()) {
                    PacketDistributor.sendToServer(
                            new OuterSystemPayloads.ServerboundShopRequest(
                                    entries.get(selectedIndex).index()));
                }
            }
        }
    }

    @Override
    public void render(
            GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xB4000711);
        float scale = uiScale();
        int uiMouseX = (int) Math.floor(mouseX / scale);
        int uiMouseY = (int) Math.floor(mouseY / scale);
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0F);

        int x = left();
        int y = top();
        enableWindowScissor(graphics);
        SystemGuiTextures.drawWindow(
                graphics,
                x,
                y,
                windowWidth(),
                windowState.renderedHeight());
        drawWindowTitle(
                graphics,
                Component.translatable(
                        OuterSystemType.byId(state.getString("Type"))
                                .translationKey()),
                x,
                y,
                windowWidth());

        if (windowState.isMinimized()) {
            graphics.disableScissor();
            graphics.pose().popPose();
            return;
        }
        if (!windowState.isMaximized()) {
            SystemGuiTextures.drawResizeGrip(
                    graphics, x, y, windowWidth(), windowHeight());
        }

        SystemWindowState.ContentLayout layout = contentLayout();
        int contentMouseX = (int) Math.floor(layout.virtualX(uiMouseX));
        int contentMouseY = (int) Math.floor(layout.virtualY(uiMouseY));
        graphics.pose().pushPose();
        graphics.pose().translate(
                layout.translationX(), layout.translationY(), 0.0F);
        graphics.pose().scale(layout.scale(), layout.scale(), 1.0F);

        hoveredStack = ItemStack.EMPTY;
        if (isShop()) {
            renderShop(graphics, x, y, contentMouseX, contentMouseY);
        } else if (isGacha()) {
            renderGacha(graphics, x, y, contentMouseX, contentMouseY);
        } else if (OuterSystemType.DAILY_SIGN_IN.id().equals(
                state.getString("Type"))) {
            renderDaily(graphics, x, y);
        } else {
            renderKillGrowth(graphics, x, y);
        }
        // Widgets are contained by ContentLayout already. AbstractWidget checks
        // hover coordinates against the scissor in screen space, whereas these
        // widget coordinates are in the fitted content space.
        graphics.disableScissor();
        super.render(
                graphics, contentMouseX, contentMouseY, partialTick);
        graphics.pose().popPose();
        graphics.pose().popPose();
        // Tooltips use GuiGraphics' real viewport bounds, so render them after
        // leaving both the content transform and the desktop transform.
        if (!hoveredStack.isEmpty()) {
            graphics.renderTooltip(font, hoveredStack, mouseX, mouseY);
        }
    }

    private void renderShop(
            GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        SystemGuiTextures.drawInsetPanel(
                graphics, x + 244, y + 41, SHOP_W - 262, 22);
        graphics.drawString(
                font,
                Component.translatable(
                        "screen.immortal_cultivation_mod.system.shop.points",
                        String.format(
                                Locale.ROOT,
                                "%,d",
                                state.getLong("ShopPoints"))),
                x + 250,
                y + 48,
                0xFF202020,
                false);
        int refreshSeconds = Math.max(
                0, state.getInt("ShopRefreshSeconds"));
        Component refresh = Component.translatable(
                "screen.immortal_cultivation_mod.system.shop.refresh",
                refreshSeconds / 60,
                String.format(Locale.ROOT, "%02d", refreshSeconds % 60));
        graphics.drawString(
                font,
                refresh,
                x + SHOP_W - 24 - font.width(refresh),
                y + 48,
                0xFF5B421B,
                false);
        if (shopMode == ShopMode.BUY && searchBox != null) {
            SystemGuiTextures.drawTextField(
                    graphics,
                    x + 18,
                    y + 41,
                    214,
                    22,
                    searchBox.isFocused());
        }
        int panelX = x + 18;
        int panelY = listTop();
        int panelW = SHOP_W - 36;
        int panelH = SHOP_ROWS * SHOP_ROW_H + 2;
        SystemGuiTextures.drawInsetPanel(
                graphics, panelX, panelY, panelW, panelH);

        if (!catalogReady) {
            graphics.drawString(font, Component.translatable("screen.immortal_cultivation_mod.system.shop.loading"),
                    panelX+8,panelY+8,0xFF202020,false);
            return;
        }

        switch (shopMode) {
            case BUY -> renderBuyRows(
                    graphics, panelX, panelY, mouseX, mouseY);
            case RECYCLE -> renderRecycleRows(
                    graphics, panelX, panelY, mouseX, mouseY);
            case REQUEST -> renderRequestRows(
                    graphics, panelX, panelY, mouseX, mouseY);
        }
        SystemGuiTextures.drawVerticalScrollbar(
                graphics,
                panelX + panelW - SHOP_SCROLLBAR_W - 2,
                panelY + 2,
                SHOP_SCROLLBAR_W,
                panelH - 4,
                scrollOffset,
                SHOP_ROWS,
                currentListSize());
    }

    private void renderGacha(
            GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        int panelY = y + 41;
        int panelH = GACHA_H - 96;
        int leftX = x + 18;
        int leftW = 250;
        int rightX = leftX + leftW + 12;
        int rightW = GACHA_W - 36 - leftW - 12;
        drawGachaPanel(graphics, leftX, panelY, leftW, panelH);
        drawGachaPanel(graphics, rightX, panelY, rightW, panelH);

        graphics.drawCenteredString(
                font,
                Component.translatable(
                        "screen.immortal_cultivation_mod.system.gacha.draws"),
                leftX + leftW / 2,
                panelY + 10,
                0xFF17325E);
        graphics.drawString(
                font,
                Component.translatable(
                        "screen.immortal_cultivation_mod.system.gacha.free",
                        state.getInt("GachaFreeDraws"),
                        3),
                leftX + 12,
                panelY + 31,
                0xFF202020,
                false);
        graphics.drawString(
                font,
                Component.translatable(
                        "screen.immortal_cultivation_mod.system.gacha.tickets",
                        state.getInt("GachaTickets")),
                leftX + 12,
                panelY + 47,
                0xFF202020,
                false);
        graphics.drawString(
                font,
                Component.translatable(
                        "screen.immortal_cultivation_mod.system.gacha.total",
                        state.getLong("GachaTotalDraws")),
                leftX + 12,
                panelY + 63,
                0xFF202020,
                false);

        graphics.drawString(
                font,
                Component.translatable(
                        "screen.immortal_cultivation_mod.system.gacha.rates"),
                leftX + 12,
                panelY + 87,
                0xFF17325E,
                false);
        String[] rarityIds = {
            "ordinary", "fine", "rare", "epic", "legendary", "divine"
        };
        String[] chances = {
            "60%", "25%", "10%", "4%", "0.9%", "0.1%"
        };
        for (int i = 0; i < rarityIds.length; i++) {
            int lineY = panelY + 105 + i * 15;
            Component rarity = gachaRarityName(rarityIds[i]);
            graphics.drawString(
                    font,
                    rarity,
                    leftX + 18,
                    lineY,
                    gachaRarityColor(rarityIds[i]),
                    false);
            graphics.drawString(
                    font,
                    chances[i],
                    leftX + leftW - 18 - font.width(chances[i]),
                    lineY,
                    0xFF303030,
                    false);
        }

        graphics.drawString(
                font,
                Component.translatable(
                        "screen.immortal_cultivation_mod.system.gacha.pity"),
                leftX + 12,
                panelY + 201,
                0xFF17325E,
                false);
        drawPity(
                graphics,
                leftX + 18,
                panelY + 219,
                "rare",
                state.getInt("GachaNoRare"),
                10);
        drawPity(
                graphics,
                leftX + 18,
                panelY + 234,
                "epic",
                state.getInt("GachaNoEpic"),
                50);
        drawPity(
                graphics,
                leftX + 18,
                panelY + 249,
                "legendary",
                state.getInt("GachaNoLegendary"),
                200);
        drawPity(
                graphics,
                leftX + 18,
                panelY + 264,
                "divine",
                state.getInt("GachaNoDivine"),
                500);

        List<GachaReward> rewards = gachaRewards();
        gachaScrollOffset = Mth.clamp(
                gachaScrollOffset,
                0,
                Math.max(0, rewards.size() - GACHA_ROWS));
        graphics.drawString(
                font,
                Component.translatable(
                        "screen.immortal_cultivation_mod.system.gacha.pending",
                        rewards.size()),
                rightX + 10,
                panelY + 10,
                0xFF17325E,
                false);
        int rowTop = panelY + 30;
        for (int row = 0; row < GACHA_ROWS; row++) {
            int index = gachaScrollOffset + row;
            if (index >= rewards.size()) {
                break;
            }
            GachaReward reward = rewards.get(index);
            int rowY = rowTop + row * GACHA_ROW_H;
            boolean hovered = mouseX >= rightX + 6
                    && mouseX < rightX + rightW - 22
                    && mouseY >= rowY
                    && mouseY < rowY + GACHA_ROW_H - 2;
            SystemGuiTextures.drawListRow(
                    graphics,
                    rightX + 6,
                    rowY,
                    rightW - 28,
                    GACHA_ROW_H - 1,
                    false,
                    hovered);
            graphics.renderItem(
                    reward.stack(), rightX + 11, rowY + 5);
            graphics.renderItemDecorations(
                    font, reward.stack(), rightX + 11, rowY + 5);
            drawTrimmed(
                    graphics,
                    reward.stack().getHoverName().getString(),
                    rightX + 35,
                    rowY + 9,
                    rightW - 135,
                    0xFF202020);
            Component rarity = gachaRarityName(reward.rarity());
            graphics.drawString(
                    font,
                    rarity,
                    rightX + rightW - 29 - font.width(rarity),
                    rowY + 9,
                    gachaRarityColor(reward.rarity()),
                    false);
            if (hovered) {
                hoveredStack = reward.stack();
            }
        }
        SystemGuiTextures.drawVerticalScrollbar(
                graphics,
                rightX + rightW - 19,
                rowTop,
                14,
                GACHA_ROWS * GACHA_ROW_H - 2,
                gachaScrollOffset,
                GACHA_ROWS,
                rewards.size());
        if (rewards.isEmpty()) {
            graphics.drawCenteredString(
                    font,
                    Component.translatable(
                            "screen.immortal_cultivation_mod.system.gacha.pending_empty"),
                    rightX + rightW / 2,
                    panelY + panelH / 2,
                    0xFF777777);
        } else if (rewards.size() > GACHA_ROWS) {
            String status = (gachaScrollOffset + 1)
                    + "-"
                    + Math.min(
                            rewards.size(),
                            gachaScrollOffset + GACHA_ROWS)
                    + "/"
                    + rewards.size();
            graphics.drawString(
                    font,
                    status,
                    rightX + rightW - 10 - font.width(status),
                    panelY + 10,
                    0xFF555555,
                    false);
        }

        String lastItem = state.getString("GachaLastItem");
        if (!lastItem.isEmpty()) {
            SystemGuiTextures.drawInsetPanel(
                    graphics,
                    x + 171,
                    y + GACHA_H - 37,
                    278,
                    22);
            Component last = Component.translatable(
                    "screen.immortal_cultivation_mod.system.gacha.last",
                    gachaRarityName(state.getString("GachaLastRarity")),
                    lastItem);
            drawTrimmed(
                    graphics,
                    last.getString(),
                    x + 179,
                    y + GACHA_H - 32,
                    270,
                    gachaRarityColor(
                            state.getString("GachaLastRarity")));
        }
    }

    private void drawGachaPanel(
            GuiGraphics graphics, int x, int y, int width, int height) {
        SystemGuiTextures.drawInsetPanel(
                graphics, x, y, width, height);
    }

    private void drawPity(
            GuiGraphics graphics,
            int x,
            int y,
            String rarity,
            int current,
            int maximum) {
        Component text = Component.translatable(
                "screen.immortal_cultivation_mod.system.gacha.pity_progress",
                gachaRarityName(rarity),
                Math.min(current, maximum),
                maximum);
        graphics.drawString(
                font, text, x, y, gachaRarityColor(rarity), false);
        SystemGuiTextures.drawProgressBar(
                graphics,
                x + 118,
                y,
                92,
                10,
                Math.min(current, maximum) / (float) maximum);
    }

    private List<GachaReward> gachaRewards() {
        List<GachaReward> rewards = new ArrayList<>();
        if (minecraft == null || minecraft.player == null) {
            return rewards;
        }
        ListTag pending =
                state.getList("GachaPending", Tag.TAG_COMPOUND);
        for (int i = 0; i < pending.size(); i++) {
            CompoundTag entry = pending.getCompound(i);
            ItemStack stack = ItemStack.parseOptional(
                    minecraft.player.registryAccess(),
                    entry.getCompound("Stack"));
            if (!stack.isEmpty()) {
                rewards.add(new GachaReward(
                        stack, entry.getString("Rarity")));
            }
        }
        return rewards;
    }

    private Component gachaRarityName(String rarity) {
        String safe = switch (rarity) {
            case "fine", "rare", "epic", "legendary", "divine" ->
                    rarity;
            default -> "ordinary";
        };
        return Component.translatable(
                "rarity.immortal_cultivation_mod.gacha." + safe);
    }

    private int gachaRarityColor(String rarity) {
        return switch (rarity) {
            case "fine" -> 0xFF278347;
            case "rare" -> 0xFF2477C4;
            case "epic" -> 0xFF8C3DC1;
            case "legendary" -> 0xFFD27A0B;
            case "divine" -> 0xFFE12525;
            default -> 0xFF5A5A5A;
        };
    }

    private void renderBuyRows(
            GuiGraphics graphics,
            int panelX,
            int panelY,
            int mouseX,
            int mouseY) {
        List<ShopEntry> entries = filteredCatalog();
        clampScroll(entries.size());
        for (int row = 0; row < SHOP_ROWS; row++) {
            int index = scrollOffset + row;
            if (index >= entries.size()) {
                break;
            }
            ShopEntry entry = entries.get(index);
            int rowY = panelY + 1 + row * SHOP_ROW_H;
            drawRowBackground(
                    graphics, panelX, rowY, index, mouseX, mouseY);
            ItemStack stack = entry.stack();
            graphics.renderItem(stack, panelX + 6, rowY + 4);
            graphics.renderItemDecorations(
                    font, stack, panelX + 6, rowY + 4);
            Component price = Component.translatable(
                    "screen.immortal_cultivation_mod.system.shop.price",
                    entry.price());
            Component stockLabel = stockText(entry);
        int stockX = panelX + SHOP_W - 45
                    - SHOP_SCROLLBAR_W
                    - font.width(stockLabel);
            drawTrimmed(
                    graphics,
                    stack.getHoverName().getString(),
                    panelX + 30,
                    rowY + 8,
                    205,
                    rowTextColor(index));
            graphics.drawString(
                    font,
                    price,
                    panelX + 250,
                    rowY + 8,
                    index == selectedIndex ? 0xFFFFFFFF : 0xFF744B00,
                    false);
            graphics.drawString(
                    font,
                    stockLabel,
                    stockX,
                    rowY + 8,
                    index == selectedIndex
                            ? 0xFFFFFFFF
                            : stock(entry) == 0 ? 0xFF9D2020 : 0xFF304F36,
                    false);
            if (isInsideRow(panelX, rowY, mouseX, mouseY)) {
                hoveredStack = stack;
            }
        }
        drawScrollStatus(graphics, panelX, panelY, entries.size());
    }

    private void renderRecycleRows(
            GuiGraphics graphics,
            int panelX,
            int panelY,
            int mouseX,
            int mouseY) {
        List<InventoryEntry> entries = recyclableInventory();
        clampScroll(entries.size());
        for (int row = 0; row < SHOP_ROWS; row++) {
            int index = scrollOffset + row;
            if (index >= entries.size()) {
                break;
            }
            InventoryEntry entry = entries.get(index);
            int rowY = panelY + 1 + row * SHOP_ROW_H;
            drawRowBackground(
                    graphics, panelX, rowY, index, mouseX, mouseY);
            graphics.renderItem(entry.stack(), panelX + 6, rowY + 4);
            graphics.renderItemDecorations(
                    font, entry.stack(), panelX + 6, rowY + 4);
            long points = Math.max(
                    1L,
                    (long) entry.catalogEntry().price()
                            * entry.stack().getCount() / 5L);
            Component value = Component.translatable(
                    "screen.immortal_cultivation_mod.system.shop.recycle_value",
                    points);
            drawTrimmed(
                    graphics,
                    entry.stack().getHoverName().getString(),
                    panelX + 30,
                    rowY + 8,
                    300,
                    rowTextColor(index));
            graphics.drawString(
                    font,
                    value,
                    panelX + SHOP_W - 45 - SHOP_SCROLLBAR_W - font.width(value),
                    rowY + 8,
                    index == selectedIndex ? 0xFFFFFFFF : 0xFF744B00,
                    false);
            if (isInsideRow(panelX, rowY, mouseX, mouseY)) {
                hoveredStack = entry.stack();
            }
        }
        drawScrollStatus(graphics, panelX, panelY, entries.size());
    }

    private void renderRequestRows(
            GuiGraphics graphics,
            int panelX,
            int panelY,
            int mouseX,
            int mouseY) {
        List<RequestEntry> entries = requests();
        clampScroll(entries.size());
        for (int row = 0; row < SHOP_ROWS; row++) {
            int index = scrollOffset + row;
            if (index >= entries.size()) {
                break;
            }
            RequestEntry entry = entries.get(index);
            int rowY = panelY + 1 + row * SHOP_ROW_H;
            drawRowBackground(
                    graphics, panelX, rowY, index, mouseX, mouseY);
            graphics.renderItem(entry.stack(), panelX + 6, rowY + 4);
            Component reward = entry.complete()
                    ? Component.translatable(
                            "screen.immortal_cultivation_mod.system.shop.request_complete")
                    : Component.translatable(
                            "screen.immortal_cultivation_mod.system.shop.request_reward",
                            entry.reward());
            drawTrimmed(
                    graphics,
                    entry.stack().getHoverName().getString()
                            + " x" + entry.count(),
                    panelX + 30,
                    rowY + 8,
                    300,
                    index == selectedIndex
                            ? 0xFFFFFFFF
                            : entry.complete() ? 0xFF777777 : 0xFF202020);
            graphics.drawString(
                    font,
                    reward,
                    panelX + SHOP_W - 45 - SHOP_SCROLLBAR_W - font.width(reward),
                    rowY + 8,
                    index == selectedIndex
                            ? 0xFFFFFFFF
                            : entry.complete() ? 0xFF617061 : 0xFF744B00,
                    false);
            if (isInsideRow(panelX, rowY, mouseX, mouseY)) {
                hoveredStack = entry.stack();
            }
        }
    }

    private void drawRowBackground(
            GuiGraphics graphics,
            int panelX,
            int rowY,
            int index,
            int mouseX,
            int mouseY) {
        boolean hovered = isInsideRow(panelX, rowY, mouseX, mouseY);
        SystemGuiTextures.drawListRow(
                graphics,
                panelX + 1,
                rowY,
                SHOP_W - 40 - SHOP_SCROLLBAR_W,
                SHOP_ROW_H,
                index == selectedIndex,
                hovered);
    }

    private int rowTextColor(int index) {
        return index == selectedIndex ? 0xFFFFFFFF : 0xFF202020;
    }

    private boolean isInsideRow(
            int panelX, int rowY, int mouseX, int mouseY) {
        return mouseX >= panelX + 1
                && mouseX < panelX + SHOP_W - 39 - SHOP_SCROLLBAR_W
                && mouseY >= rowY
                && mouseY < rowY + SHOP_ROW_H - 1;
    }

    private void drawScrollStatus(
            GuiGraphics graphics, int panelX, int panelY, int size) {
        if (size <= SHOP_ROWS) {
            return;
        }
        String status = (scrollOffset + 1)
                + "-"
                + Math.min(size, scrollOffset + SHOP_ROWS)
                + "/"
                + size;
        graphics.drawString(
                font,
                status,
                panelX + SHOP_W - 43 - SHOP_SCROLLBAR_W - font.width(status),
                panelY - 11,
                0xFF555555,
                false);
    }

    private void drawTrimmed(
            GuiGraphics graphics,
            String value,
            int x,
            int y,
            int maxWidth,
            int color) {
        String trimmed = font.plainSubstrByWidth(value, maxWidth);
        graphics.drawString(font, trimmed, x, y, color, false);
    }

    private Component stockText(ShopEntry entry) {
        int stock = stock(entry);
        if (stock < 0) {
            return Component.translatable(
                    "screen.immortal_cultivation_mod.system.shop.stock.unlimited");
        }
        if (stock == 0) {
            return Component.translatable(
                    "screen.immortal_cultivation_mod.system.shop.stock.sold_out");
        }
        return Component.translatable(
                "screen.immortal_cultivation_mod.system.shop.stock",
                stock);
    }

    private int stock(ShopEntry entry) {
        if ("unique".equals(entry.rarity())) {
            return containsString(
                    state.getList("ShopUniqueBought", Tag.TAG_STRING),
                    entry.id()) ? 0 : 1;
        }
        int maximum = SystemShop.dailyStock(
                entry.id(), entry.price(), state.getLong("ShopDay"));
        if (maximum < 0) {
            return -1;
        }
        return Math.max(
                0,
                maximum - state.getCompound("ShopPurchases")
                        .getInt(entry.id()));
    }

    private List<ShopEntry> filteredCatalog() {
        String needle = shopSearch.strip().toLowerCase(Locale.ROOT);
        return catalog.stream()
                .filter(entry -> shopCategory == ShopCategory.ALL
                        || shopCategory.id().equals(entry.category()))
                .filter(entry -> needle.isEmpty()
                        || entry.id().toLowerCase(Locale.ROOT).contains(needle)
                        || entry.stack().getHoverName().getString()
                                .toLowerCase(Locale.ROOT).contains(needle))
                .toList();
    }

    private List<InventoryEntry> recyclableInventory() {
        List<InventoryEntry> entries = new ArrayList<>();
        if (minecraft == null || minecraft.player == null) {
            return entries;
        }
        for (int slot = 0;
                slot < minecraft.player.getInventory().items.size();
                slot++) {
            ItemStack stack =
                    minecraft.player.getInventory().items.get(slot);
            if (stack.isEmpty()) {
                continue;
            }
            ResourceLocation id =
                    BuiltInRegistries.ITEM.getKey(stack.getItem());
            ShopEntry catalogEntry =
                    id == null ? null : catalogById.get(id.toString());
            if (catalogEntry != null && isClientRecycleEligible(stack)) {
                entries.add(new InventoryEntry(
                        slot, stack, catalogEntry));
            }
        }
        return entries;
    }

    private boolean isClientRecycleEligible(ItemStack stack) {
        if (stack.has(DataComponents.CUSTOM_NAME)
                || stack.has(DataComponents.CUSTOM_DATA)
                || stack.has(DataComponents.CONTAINER)
                || stack.has(DataComponents.BUNDLE_CONTENTS)) {
            return false;
        }
        if (stack.isDamageableItem()
                && stack.getDamageValue() * 4
                        > stack.getMaxDamage() * 3) {
            return false;
        }
        return stack.getComponentsPatch().entrySet().stream()
                .allMatch(entry ->
                        entry.getKey() == DataComponents.DAMAGE);
    }

    private List<RequestEntry> requests() {
        List<RequestEntry> entries = new ArrayList<>();
        ListTag requests =
                state.getList("ShopRequests", Tag.TAG_COMPOUND);
        for (int i = 0; i < requests.size(); i++) {
            CompoundTag request = requests.getCompound(i);
            String id = request.getString("Item");
            ShopEntry shopEntry = catalogById.get(id);
            if (shopEntry == null) {
                continue;
            }
            entries.add(new RequestEntry(
                    i,
                    shopEntry.stack(),
                    request.getInt("Count"),
                    request.getLong("Reward"),
                    request.getBoolean("Complete")));
        }
        return entries;
    }

    private void loadCatalog() {
        if (!state.contains("ShopCatalog", Tag.TAG_LIST)) {
            return;
        }
        catalog.clear();
        catalogById.clear();
        ListTag list = state.getList("ShopCatalog", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag encoded = list.getCompound(i);
            ResourceLocation id =
                    ResourceLocation.tryParse(encoded.getString("Id"));
            if (id == null) {
                continue;
            }
            Item item = BuiltInRegistries.ITEM.get(id);
            if (item == Items.AIR) {
                continue;
            }
            ShopEntry entry = new ShopEntry(
                    id.toString(),
                    item,
                    encoded.getInt("Price"),
                    encoded.getString("Rarity"),
                    encoded.getString("Category"));
            catalog.add(entry);
            catalogById.put(entry.id(), entry);
        }
    }

    private void clampScroll(int size) {
        scrollOffset = Mth.clamp(
                scrollOffset, 0, Math.max(0, size - SHOP_ROWS));
        if (selectedIndex >= size) {
            selectedIndex = -1;
        }
    }

    private int currentListSize() {
        if (!isShop()) {
            return 0;
        }
        return switch (shopMode) {
            case BUY -> filteredCatalog().size();
            case RECYCLE -> recyclableInventory().size();
            case REQUEST -> requests().size();
        };
    }

    private void renderKillGrowth(GuiGraphics graphics, int x, int y) {
        int panelX = x + 18;
        int panelY = y + 41;
        int panelW = NORMAL_W - 36;
        SystemGuiTextures.drawInsetPanel(
                graphics,
                panelX,
                panelY,
                panelW,
                NORMAL_H - 59);
        drawLabel(
                graphics,
                "screen.immortal_cultivation_mod.system.kill.heading",
                panelX + 12,
                panelY + 13,
                0xFF102060);
        drawLabel(
                graphics,
                "screen.immortal_cultivation_mod.system.kill.explanation",
                panelX + 12,
                panelY + 34,
                0xFF222222);

        long progress = state.getLong("Progress");
        long required = Math.max(
                1L, state.getLong("ProgressRequired"));
        float ratio = Mth.clamp(
                progress / (float) required, 0.0F, 1.0F);
        int barX = panelX + 12;
        int barY = panelY + 60;
        int barW = panelW - 24;
        SystemGuiTextures.drawProgressBar(
                graphics, barX, barY, barW, 17, ratio);
        graphics.drawCenteredString(
                font,
                progress + " / " + required,
                barX + barW / 2,
                barY + 4,
                0xFFFFFFFF);

        List<Component> lines = List.of(
                Component.translatable(
                        "screen.immortal_cultivation_mod.system.kill.same"),
                Component.translatable(
                        "screen.immortal_cultivation_mod.system.kill.stronger"),
                Component.translatable(
                        "screen.immortal_cultivation_mod.system.kill.boss"),
                Component.translatable(
                        "screen.immortal_cultivation_mod.system.kill.excluded"));
        int lineY = barY + 33;
        for (Component line : lines) {
            graphics.drawString(
                    font,
                    line,
                    panelX + 12,
                    lineY,
                    0xFF222222,
                    false);
            lineY += 19;
        }
    }

    private void renderDaily(GuiGraphics graphics, int x, int y) {
        int panelX = x + 18;
        int panelY = y + 41;
        int panelW = NORMAL_W - 36;
        SystemGuiTextures.drawInsetPanel(
                graphics,
                panelX,
                panelY,
                panelW,
                NORMAL_H - 59);
        drawLabel(
                graphics,
                "screen.immortal_cultivation_mod.system.daily.heading",
                panelX + 12,
                panelY + 12,
                0xFF102060);

        if (!state.getBoolean("QuestActive")) {
            long seconds = Math.max(
                    0L, state.getLong("QuestNextSeconds"));
            graphics.drawString(
                    font,
                    Component.translatable(
                            "screen.immortal_cultivation_mod.system.daily.wait",
                            seconds / 60L,
                            seconds % 60L),
                    panelX + 12,
                    panelY + 42,
                    0xFF222222,
                    false);
            return;
        }

        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(
                "screen.immortal_cultivation_mod.system.daily.target",
                Component.translatable(
                        state.getString("QuestTargetName"))));
        lines.add(Component.translatable(
                "screen.immortal_cultivation_mod.system.daily.kind."
                        + state.getString("QuestKind")));
        lines.add(Component.translatable(
                "screen.immortal_cultivation_mod.system.daily.coordinates",
                state.getInt("QuestX"),
                state.getInt("QuestY"),
                state.getInt("QuestZ")));
        lines.add(Component.translatable(
                "screen.immortal_cultivation_mod.system.daily.distance",
                state.getInt("QuestDistance"),
                state.getInt("QuestTier")));
        lines.add(Component.translatable(
                "screen.immortal_cultivation_mod.system.daily.reward",
                state.getString("QuestRewardSummary")));
        lines.add(Component.translatable(
                state.getBoolean("QuestCompleted")
                        ? "screen.immortal_cultivation_mod.system.daily.complete"
                        : "screen.immortal_cultivation_mod.system.daily.travel"));
        int lineY = panelY + 38;
        for (Component line : lines) {
            graphics.drawString(
                    font,
                    line,
                    panelX + 12,
                    lineY,
                    0xFF222222,
                    false);
            lineY += 23;
        }
    }

    private void drawLabel(
            GuiGraphics graphics,
            String key,
            int x,
            int y,
            int color) {
        graphics.drawString(
                font, Component.translatable(key), x, y, color, false);
    }

    private boolean isShop() {
        return OuterSystemType.SYSTEM_SHOP.id().equals(
                state.getString("Type"));
    }

    private boolean isGacha() {
        return OuterSystemType.DIVINE_GACHA.id().equals(
                state.getString("Type"));
    }

    private int baseWindowWidth() {
        return isShop() ? SHOP_W : isGacha() ? GACHA_W : NORMAL_W;
    }

    private int baseWindowHeight() {
        return isShop() ? SHOP_H : isGacha() ? GACHA_H : NORMAL_H;
    }

    private int windowWidth() {
        return windowState.width();
    }

    private int windowHeight() {
        return windowState.height();
    }

    private int left() {
        return windowState.x();
    }

    private int top() {
        return windowState.y();
    }

    private SystemWindowState.ContentLayout contentLayout() {
        return windowState.contentLayout(
                baseWindowWidth(), baseWindowHeight());
    }

    private void enableWindowScissor(GuiGraphics graphics) {
        float scale = uiScale();
        graphics.enableScissor(
                (int) Math.floor(windowState.x() * scale),
                (int) Math.floor(windowState.y() * scale),
                (int) Math.ceil(
                        (windowState.x() + windowState.width()) * scale),
                (int) Math.ceil(
                        (windowState.y() + windowState.renderedHeight())
                                * scale));
    }

    private void drawWindowTitle(
            GuiGraphics graphics,
            Component title,
            int windowX,
            int windowY,
            int windowWidth) {
        int textX = windowX + 13;
        int maximumWidth = Math.max(
                0,
                SystemGuiTextures.titleTextRight(windowX, windowWidth) - textX);
        String value = title.getString();
        String visible = font.plainSubstrByWidth(value, maximumWidth);
        if (!visible.equals(value) && maximumWidth > font.width("...")) {
            visible = font.plainSubstrByWidth(
                    value,
                    maximumWidth - font.width("...")) + "...";
        }
        graphics.drawString(
                font,
                visible,
                textX,
                windowY + 13,
                0xFFFFFFFF,
                true);
    }

    private float uiScale() {
        return (float) MenuViewport.fit(width - SCREEN_MARGIN, height - SCREEN_MARGIN,
                baseWindowWidth(), baseWindowHeight(), DEFAULT_UI_SCALE).scale();
    }

    @Override
    public void resize(net.minecraft.client.Minecraft minecraft, int width, int height) {
        windowState.endDrag();
        super.resize(minecraft, width, height);
    }

    private int desktopWidth() {
        return Math.max(baseWindowWidth(), (int) Math.floor(width / uiScale()));
    }

    private int desktopHeight() {
        return Math.max(baseWindowHeight(), (int) Math.floor(height / uiScale()));
    }

    private double uiMouseX(double mouseX) {
        return mouseX / uiScale();
    }

    private double uiMouseY(double mouseY) {
        return mouseY / uiScale();
    }

    private int listTop() {
        return top() + 101 + (categoryRowCount() - 1)
                * (SHOP_CATEGORY_BUTTON_H + SHOP_CATEGORY_ROW_GAP);
    }

    private static int categoryRowCount() {
        return Math.max(1, (ShopCategory.values().length + SHOP_CATEGORY_COLUMNS - 1)
                / SHOP_CATEGORY_COLUMNS);
    }

    @Override
    public boolean mouseClicked(
            double mouseX, double mouseY, int button) {
        double desktopMouseX = uiMouseX(mouseX);
        double desktopMouseY = uiMouseY(mouseY);
        if (!windowState.contains(desktopMouseX, desktopMouseY)) {
            if (searchBox != null) {
                searchBox.setFocused(false);
            }
            return false;
        }
        if (button == 0
                && SystemGuiTextures.isOverClose(
                        desktopMouseX,
                        desktopMouseY,
                        left(),
                        top(),
                        windowWidth())) {
            onClose();
            return true;
        }
        if (button == 0 && SystemGuiTextures.isOverMinimize(
                desktopMouseX,
                desktopMouseY,
                left(),
                top(),
                windowWidth())) {
            windowState.toggleMinimize();
            rebuildWidgets();
            return true;
        }
        if (button == 0 && SystemGuiTextures.isOverMaximize(
                desktopMouseX,
                desktopMouseY,
                left(),
                top(),
                windowWidth())) {
            windowState.toggleMaximize(desktopWidth(), desktopHeight());
            rebuildWidgets();
            return true;
        }
        if (windowState.beginDrag(
                desktopMouseX,
                desktopMouseY,
                button,
                desktopWidth(),
                desktopHeight())) {
            return true;
        }
        if (windowState.isMinimized()) {
            return false;
        }

        SystemWindowState.ContentLayout layout = contentLayout();
        double contentMouseX = layout.virtualX(desktopMouseX);
        double contentMouseY = layout.virtualY(desktopMouseY);
        if (super.mouseClicked(contentMouseX, contentMouseY, button)) {
            return true;
        }
        if (button == 0 && isGacha()) {
            int panelY = top() + 41;
            int leftX = left() + 18;
            int leftW = 250;
            int rightX = leftX + leftW + 12;
            int rightW = GACHA_W - 36 - leftW - 12;
            int maximum = Math.max(
                    0, gachaRewards().size() - GACHA_ROWS);
            int changed = clickedScrollbarOffset(
                    contentMouseX,
                    contentMouseY,
                    rightX + rightW - 19,
                    panelY + 30,
                    14,
                    GACHA_ROWS * GACHA_ROW_H - 2,
                    gachaScrollOffset,
                    maximum);
            if (changed >= 0) {
                gachaScrollOffset = changed;
                return true;
            }
        }
        if (!isShop() || button != 0) {
            return false;
        }
        int panelX = left() + 18;
        int panelY = listTop();
        int panelW = SHOP_W - 36;
        int panelH = SHOP_ROWS * SHOP_ROW_H + 2;
        int maximum = Math.max(0, currentListSize() - SHOP_ROWS);
        int changed = clickedScrollbarOffset(
                contentMouseX,
                contentMouseY,
                panelX + panelW - SHOP_SCROLLBAR_W - 2,
                panelY + 2,
                SHOP_SCROLLBAR_W,
                panelH - 4,
                scrollOffset,
                maximum);
        if (changed >= 0) {
            scrollOffset = changed;
            return true;
        }
        if (contentMouseX < panelX
                || contentMouseX >= panelX + panelW
                || contentMouseY < panelY
                || contentMouseY >= panelY + panelH) {
            return false;
        }
        int row = (int) ((contentMouseY - panelY - 1) / SHOP_ROW_H);
        int index = scrollOffset + row;
        if (row >= 0
                && row < SHOP_ROWS
                && index < currentListSize()) {
            selectedIndex = index;
            updateButtons();
            if (shopMode == ShopMode.BUY && hasShiftDown()) {
                performShopAction(true);
            }
            return true;
        }
        return false;
    }

    private int clickedScrollbarOffset(
            double mouseX,
            double mouseY,
            int x,
            int y,
            int width,
            int height,
            int current,
            int maximum) {
        if (mouseX < x || mouseX >= x + width
                || mouseY < y || mouseY >= y + height) {
            return -1;
        }
        int buttonHeight = Math.min(width, height / 3);
        if (mouseY < y + buttonHeight) {
            return Math.max(0, current - 1);
        }
        if (mouseY >= y + height - buttonHeight) {
            return Math.min(maximum, current + 1);
        }
        int trackHeight = Math.max(1, height - buttonHeight * 2);
        float ratio = (float) ((mouseY - y - buttonHeight) / trackHeight);
        return Mth.clamp(Math.round(ratio * maximum), 0, maximum);
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double scrollX,
            double scrollY) {
        double desktopMouseX = uiMouseX(mouseX);
        double desktopMouseY = uiMouseY(mouseY);
        if (!windowState.contains(desktopMouseX, desktopMouseY)
                || windowState.isMinimized()) {
            return false;
        }
        SystemWindowState.ContentLayout layout = contentLayout();
        double contentMouseX = layout.virtualX(desktopMouseX);
        double contentMouseY = layout.virtualY(desktopMouseY);
        if (!isShop()) {
            if (isGacha()) {
                int maximum = Math.max(
                        0, gachaRewards().size() - GACHA_ROWS);
                if (maximum > 0) {
                    gachaScrollOffset = Mth.clamp(
                            gachaScrollOffset
                                    - (int) Math.signum(scrollY),
                            0,
                            maximum);
                    return true;
                }
            }
            return super.mouseScrolled(
                    contentMouseX,
                    contentMouseY,
                    scrollX,
                    scrollY);
        }
        int maximum = Math.max(0, currentListSize() - SHOP_ROWS);
        if (maximum == 0) {
            return super.mouseScrolled(
                    contentMouseX,
                    contentMouseY,
                    scrollX,
                    scrollY);
        }
        scrollOffset = Mth.clamp(
                scrollOffset - (int) Math.signum(scrollY),
                0,
                maximum);
        return true;
    }

    @Override
    public boolean mouseReleased(
            double mouseX, double mouseY, int button) {
        if (windowState.isDragging()) {
            windowState.endDrag();
            return true;
        }
        double desktopMouseX = uiMouseX(mouseX);
        double desktopMouseY = uiMouseY(mouseY);
        SystemWindowState.ContentLayout layout = contentLayout();
        return super.mouseReleased(
                layout.virtualX(desktopMouseX),
                layout.virtualY(desktopMouseY),
                button);
    }

    @Override
    public boolean mouseDragged(
            double mouseX,
            double mouseY,
            int button,
            double dragX,
            double dragY) {
        double desktopMouseX = uiMouseX(mouseX);
        double desktopMouseY = uiMouseY(mouseY);
        if (button == 0 && windowState.dragTo(
                desktopMouseX,
                desktopMouseY,
                desktopWidth(),
                desktopHeight())) {
            rebuildWidgets();
            return true;
        }
        SystemWindowState.ContentLayout layout = contentLayout();
        float totalScale = uiScale() * layout.scale();
        return super.mouseDragged(
                layout.virtualX(desktopMouseX),
                layout.virtualY(desktopMouseY),
                button,
                dragX / totalScale,
                dragY / totalScale);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        double desktopMouseX = uiMouseX(mouseX);
        double desktopMouseY = uiMouseY(mouseY);
        SystemWindowState.ContentLayout layout = contentLayout();
        super.mouseMoved(
                layout.virtualX(desktopMouseX),
                layout.virtualY(desktopMouseY));
    }

    @Override
    public void onClose() {
        super.onClose();
    }

    @Override
    public void removed() {
        if (!OuterSystemClient.isTransitioning()) OuterSystemClient.clearShopCatalog();
        if (!OuterSystemClient.isTransitioning() && minecraft != null && minecraft.player != null) {
            PacketDistributor.sendToServer(
                    new OuterSystemPayloads.ServerboundClose());
        }
        super.removed();
    }

    @Override
    public void renderBackground(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static boolean containsString(
            ListTag list, String value) {
        for (int i = 0; i < list.size(); i++) {
            if (value.equals(list.getString(i))) {
                return true;
            }
        }
        return false;
    }

    private enum ShopMode {
        BUY("buy"),
        RECYCLE("recycle"),
        REQUEST("request");

        private final String id;

        ShopMode(String id) {
            this.id = id;
        }

        private Component label() {
            return Component.translatable(
                    "screen.immortal_cultivation_mod.system.shop.mode."
                            + id);
        }

        private Component actionLabel() {
            return Component.translatable(
                    "screen.immortal_cultivation_mod.system.shop.action."
                            + id);
        }
    }

    private enum ShopCategory {
        ALL("all"),
        PILL("pill"),
        FOOD("food"),
        MATERIAL("material"),
        TALISMAN("talisman"),
        WEAPON("weapon"),
        ACCESSORY("accessory"),
        SPELL("spell"),
        FORMATION("formation"),
        BLOCK("block");

        private final String id;

        ShopCategory(String id) {
            this.id = id;
        }

        private String id() {
            return id;
        }

        private Component label() {
            return Component.translatable(
                    "screen.immortal_cultivation_mod.system.shop.category."
                            + id);
        }
    }

    private record ShopEntry(
            String id,
            Item item,
            int price,
            String rarity,
            String category) {
        private ItemStack stack() {
            return new ItemStack(item);
        }
    }

    private record InventoryEntry(
            int slot, ItemStack stack, ShopEntry catalogEntry) {
    }

    private record RequestEntry(
            int index,
            ItemStack stack,
            int count,
            long reward,
            boolean complete) {
    }

    private record GachaReward(ItemStack stack, String rarity) {
    }
}
