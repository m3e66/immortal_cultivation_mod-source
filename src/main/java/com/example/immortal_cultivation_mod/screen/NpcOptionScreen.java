package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.client.ClientData;
import com.example.immortal_cultivation_mod.client.DaoGuidanceClient;
import com.example.immortal_cultivation_mod.client.NpcClientData;
import com.example.immortal_cultivation_mod.guidance.DaoGuidanceContent;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class NpcOptionScreen extends AbstractNpcScreen {
    private static final int INVENTORY_COLUMNS = 5;
    private static final int INVENTORY_VISIBLE_ROWS = 3;
    private static final int INVENTORY_VISIBLE_SLOTS = INVENTORY_COLUMNS * INVENTORY_VISIBLE_ROWS;
    private static final int INVENTORY_SLOT_SIZE = 30;
    private static final int INVENTORY_SLOT_GAP = 4;

    public enum Page {
        TALK("talk"),
        INTERACT("interact"),
        TRADE("trade"),
        QUEST("quest"),
        TEACH("teach"),
        INVENTORY("inventory"),
        ABOUT("about");

        private final String key;

        Page(String key) {
            this.key = key;
        }
    }

    private final Page page;
    private int npcSpellIndex;
    private int playerSpellIndex;
    private int playerMethodIndex;
    private int tradeScroll;
    private int inventoryScroll;
    private final List<NpcButton> guidanceButtons = new ArrayList<>();

    public NpcOptionScreen(NpcClientData data, Page page) {
        super(data, Component.translatable("screen." + ImmortalCultivationMod.MODID + ".npc.action." + page.key));
        this.page = page;
    }

    @Override
    protected void buildContent() {
        guidanceButtons.clear();
        addNpcButton(panelX() + panelWidth() - 78, panelY() + 8, 48, 17,
                Component.translatable("screen." + ImmortalCultivationMod.MODID + ".npc.back"),
                () -> minecraft.setScreen(new NpcInteractionScreen(data.withoutStatus())));

        int x = contentX();
        int y = contentY() + 31;
        int w = contentWidth();
        switch (page) {
            case TALK -> {
                boolean canTalk = data.hatred() < 50;
                NpcButton greet = addAction(x, y + 22, w,
                        "talk.greet", "TALK_GREET");
                NpcButton realm = addAction(x, y + 55, w,
                        "talk.realm", "TALK_REALM");
                NpcButton world = addAction(x, y + 88, w,
                        "talk.world", "TALK_WORLD");
                greet.active = canTalk;
                realm.active = canTalk;
                world.active = canTalk;
                guidanceButtons.add(greet);
                guidanceButtons.add(realm);
                guidanceButtons.add(world);
            }
            case INTERACT -> {
                int gap = 8;
                int half = (w - gap) / 2;
                addCompactAction(x, y + 12, w, "interact.gift", "INTERACT_GIFT");
                addCompactAction(x, y + 35, half, "interact.praise", "INTERACT_PRAISE");
                addCompactAction(x + half + gap, y + 35, half, "interact.joke", "INTERACT_JOKE");
                addCompactAction(x, y + 58, half, "interact.insult", "INTERACT_INSULT");
                addCompactAction(x + half + gap, y + 58, half, "interact.threaten", "INTERACT_THREATEN");
                NpcButton spar = addNpcButton(x, y + 81, half, 21,
                        tr("npc.interact.spar"), () -> {
                    sendAction("INTERACT_SPAR");
                    minecraft.setScreen(null);
                });
                guidanceButtons.add(spar);
                NpcButton sect = addCompactAction(x + half + gap, y + 81, half,
                        "interact.invite_sect", "INTERACT_INVITE_SECT");
                NpcButton master = addCompactAction(x, y + 104, half,
                        "interact.master", "INTERACT_MASTER");
                NpcButton disciple = addCompactAction(x + half + gap, y + 104, half,
                        "interact.disciple", "INTERACT_DISCIPLE");
                NpcButton daoCompanion = addCompactAction(x, y + 127, w,
                        "interact.dao_companion", "INTERACT_DAO_COMPANION");
                sect.active = data.trust() >= 50;
                master.active = data.trust() >= 75;
                disciple.active = data.trust() >= 75;
                daoCompanion.active = data.affection() >= 50;
            }
            case TRADE -> {
                tradeScroll = Mth.clamp(tradeScroll, 0, Math.max(0, data.tradeOffers().size() - 4));
                int visible = Math.min(4, data.tradeOffers().size() - tradeScroll);
                for (int i = 0; i < visible; i++) {
                    NpcClientData.NpcTradeOffer offer = data.tradeOffers().get(tradeScroll + i);
                    NpcButton buy = addNpcButton(x + w - 66, y + 6 + i * 40, 62, 22,
                            tr("npc.trade.buy"), () -> sendAction("TRADE_EXCHANGE", offer.offerIndex()));
                    buy.active = data.hatred() < 50;
                    guidanceButtons.add(buy);
                }
            }
            case QUEST -> {
                String key = data.questState() == 0 ? "npc.quest.accept" : "npc.quest.complete";
                NpcButton button = addNpcButton(x, y + 139, w, 27, tr(key),
                        () -> sendAction(data.questState() == 0 ? "QUEST_ACCEPT" : "QUEST_COMPLETE"));
                button.active = data.questState() < 2;
                if (data.questState() == 0) guidanceButtons.add(button);
            }
            case TEACH -> buildTeachingButtons(x, y, w);
            case INVENTORY -> buildInventoryButtons(x, y, w);
            case ABOUT -> {
            }
        }
    }

    private NpcButton addAction(int x, int y, int width, String key, String action) {
        return addNpcButton(x, y, width, 27, tr("npc." + key), () -> sendAction(action));
    }

    private NpcButton addCompactAction(int x, int y, int width, String key, String action) {
        return addNpcButton(x, y, width, 21, tr("npc." + key), () -> sendAction(action));
    }

    private void buildTeachingButtons(int x, int y, int w) {
        npcSpellIndex = clampIndex(npcSpellIndex, data.spells());
        playerSpellIndex = clampIndex(playerSpellIndex, data.playerSpells());
        playerMethodIndex = clampIndex(playerMethodIndex, data.playerMethods());
        int arrowWidth = 25;

        NpcButton npcPrev = addNpcButton(x, y + 30, arrowWidth, 21, Component.literal("<"), () -> {
            npcSpellIndex = previousIndex(npcSpellIndex, data.spells());
            init(minecraft, width, height);
        });
        NpcButton npcNext = addNpcButton(x + w - arrowWidth, y + 30, arrowWidth, 21, Component.literal(">"), () -> {
            npcSpellIndex = nextIndex(npcSpellIndex, data.spells());
            init(minecraft, width, height);
        });
        NpcButton learnSpell = addNpcButton(x + arrowWidth + 5, y + 30, w - arrowWidth * 2 - 10, 21,
                tr("npc.teach.learn_spell"), () -> sendAction("LEARN_SPELL", npcSpellIndex));
        npcPrev.active = !data.spells().isEmpty();
        npcNext.active = !data.spells().isEmpty();
        learnSpell.active = !data.spells().isEmpty()
                && data.trust() >= selectedSpellTrustRequired();

        NpcButton learnMethod = addNpcButton(x, y + 63, w, 22, tr("npc.teach.learn_method"),
                () -> sendAction("LEARN_METHOD"));
        learnMethod.active = data.method() != null && !data.method().isBlank()
                && data.trust() >= data.methodLearningTrustRequired();

        NpcButton playerPrev = addNpcButton(x, y + 112, arrowWidth, 21, Component.literal("<"), () -> {
            playerSpellIndex = previousIndex(playerSpellIndex, data.playerSpells());
            init(minecraft, width, height);
        });
        NpcButton playerNext = addNpcButton(x + w - arrowWidth, y + 112, arrowWidth, 21, Component.literal(">"), () -> {
            playerSpellIndex = nextIndex(playerSpellIndex, data.playerSpells());
            init(minecraft, width, height);
        });
        NpcButton teachSpell = addNpcButton(x + arrowWidth + 5, y + 112, w - arrowWidth * 2 - 10, 21,
                tr("npc.teach.teach_spell"), () -> sendAction("TEACH_NPC_SPELL", playerSpellIndex));
        playerPrev.active = !data.playerSpells().isEmpty();
        playerNext.active = !data.playerSpells().isEmpty();
        teachSpell.active = !data.playerSpells().isEmpty();

        NpcButton methodPrev = addNpcButton(x, y + 142, arrowWidth, 22, Component.literal("<"), () -> {
            playerMethodIndex = previousIndex(playerMethodIndex, data.playerMethods());
            init(minecraft, width, height);
        });
        NpcButton methodNext = addNpcButton(x + w - arrowWidth, y + 142, arrowWidth, 22, Component.literal(">"), () -> {
            playerMethodIndex = nextIndex(playerMethodIndex, data.playerMethods());
            init(minecraft, width, height);
        });
        NpcButton teachMethod = addNpcButton(x + arrowWidth + 5, y + 142, w - arrowWidth * 2 - 10, 22,
                tr("npc.teach.teach_method"), () -> sendAction("TEACH_NPC_METHOD", playerMethodIndex));
        boolean hasPlayerMethods = !data.playerMethods().isEmpty();
        methodPrev.active = hasPlayerMethods;
        methodNext.active = hasPlayerMethods;
        teachMethod.active = hasPlayerMethods;
    }

    private void buildInventoryButtons(int x, int y, int w) {
        inventoryScroll = Mth.clamp(inventoryScroll, 0, maxInventoryScroll());
        int gridWidth = INVENTORY_SLOT_SIZE * INVENTORY_COLUMNS
                + INVENTORY_SLOT_GAP * (INVENTORY_COLUMNS - 1);
        int gridX = x + (w - gridWidth) / 2;
        int visibleSlots = Math.min(INVENTORY_VISIBLE_SLOTS, data.inventory().size() - inventoryScroll);
        for (int i = 0; i < visibleSlots; i++) {
            int slot = inventoryScroll + i;
            int slotX = gridX + (i % INVENTORY_COLUMNS) * (INVENTORY_SLOT_SIZE + INVENTORY_SLOT_GAP);
            int slotY = y + 20 + (i / INVENTORY_COLUMNS) * (INVENTORY_SLOT_SIZE + INVENTORY_SLOT_GAP);
            NpcButton button = addNpcButton(slotX, slotY, INVENTORY_SLOT_SIZE, INVENTORY_SLOT_SIZE, Component.empty(),
                    () -> sendAction("INVENTORY_WITHDRAW", slot));
            button.active = data.canManageInventory() && !data.inventory().get(slot).empty();
        }
        NpcButton deposit = addNpcButton(x, y + 145, w, 23, tr("npc.inventory.deposit"),
                () -> sendAction("INVENTORY_DEPOSIT"));
        deposit.active = data.canManageInventory();
    }

    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        drawSectionTitle(graphics, tr("npc.action." + page.key), contentY());
        int x = contentX();
        int y = contentY() + 31;
        int w = contentWidth();
        switch (page) {
            case TALK -> drawWrapped(graphics, tr("npc.talk.description"), x, y, w, COLOR_MUTED, 3);
            case INTERACT -> {
                graphics.drawString(font, tr("npc.interact.social"), x, y + 2, COLOR_MUTED, false);
                boolean showDebug = ClientData.debugEyeEnabled()
                        && data.praiseChance() >= 0 && data.jokeChance() >= 0;
                if (showDebug) {
                    drawCenteredNoShadow(graphics,
                            tr("npc.debug_eye.social_chances", data.praiseChance(), data.jokeChance()),
                            x + w / 2,
                            y + 153,
                            COLOR_GOLD);
                }
                drawRelationLine(graphics, x, showDebug ? y + 168 : y + 158);
            }
            case TRADE -> renderTrade(graphics, x, y, w);
            case QUEST -> renderQuest(graphics, x, y, w);
            case TEACH -> renderTeaching(graphics, x, y, w);
            case INVENTORY -> renderInventoryText(graphics, x, y, w);
            case ABOUT -> renderAbout(graphics, x, y, w);
        }
    }

    private void renderTrade(GuiGraphics graphics, int x, int y, int w) {
        if (data.tradeOffers().isEmpty()) {
            drawCenteredNoShadow(graphics, tr("npc.trade.empty"), x + w / 2, y + 74, COLOR_MUTED);
            return;
        }
        int visible = Math.min(4, data.tradeOffers().size() - tradeScroll);
        for (int i = 0; i < visible; i++) {
            NpcClientData.NpcTradeOffer offer = data.tradeOffers().get(tradeScroll + i);
            int rowY = y + i * 40;
            graphics.fill(x, rowY, x + w, rowY + 34, 0xFFE5DDC6);
            graphics.renderOutline(x, rowY, w, 34, 0xFFB8A985);
            ItemStack cost = stack(offer.currencyItemId(), offer.price());
            ItemStack result = stack(offer.resultItemId(), offer.resultCount());
            graphics.renderItem(cost, x + 6, rowY + 9);
            graphics.renderItemDecorations(font, cost, x + 6, rowY + 9);
            graphics.drawString(font, Component.literal("->"), x + 27, rowY + 13, COLOR_MUTED, false);
            graphics.renderItem(result, x + 42, rowY + 9);
            graphics.renderItemDecorations(font, result, x + 42, rowY + 9);
            String tradeText = result.getHoverName().getString() + "  [" + offer.availableCount() + "]";
            graphics.drawString(font, font.plainSubstrByWidth(tradeText, w - 133), x + 63, rowY + 13, COLOR_INK, false);
        }
        if (data.tradeOffers().size() > 4) {
            drawCenteredNoShadow(graphics, tr("npc.trade.scroll"), x + w / 2, y + 164, COLOR_MUTED);
        }
    }

    private void renderQuest(GuiGraphics graphics, int x, int y, int w) {
        drawWrapped(graphics,
                Component.translatable("screen." + ImmortalCultivationMod.MODID + ".npc.quest." + data.questId()),
                x, y, w, COLOR_INK, 3);
        drawWrapped(graphics,
                Component.translatable("screen." + ImmortalCultivationMod.MODID
                        + ".npc.quest_dialogue." + data.questId()),
                x, y + 34, w, COLOR_MUTED, 2);
        ItemStack required = stack(data.questRequiredItem(), data.questRequired());
        ItemStack reward = stack(data.questRewardItem(), data.questRewardCount());
        int itemY = y + 66;
        graphics.renderItem(required, x + 4, itemY);
        graphics.renderItemDecorations(font, required, x + 4, itemY);
        graphics.drawString(font, tr("npc.quest.progress", data.questProgress(), data.questRequired()),
                x + 27, itemY + 5, COLOR_INK, false);
        graphics.renderItem(reward, x + w - 90, itemY);
        graphics.renderItemDecorations(font, reward, x + w - 90, itemY);
        graphics.drawString(font, tr("npc.quest.reward"), x + w - 67, itemY + 5, COLOR_MUTED, false);

        int barY = y + 98;
        graphics.fill(x, barY, x + w, barY + 9, 0xFFB7AD94);
        float ratio = data.questRequired() <= 0 ? 0.0F : (float) data.questProgress() / data.questRequired();
        graphics.fill(x + 1, barY + 1, x + 1 + Math.round((w - 2) * Mth.clamp(ratio, 0.0F, 1.0F)), barY + 8, COLOR_GREEN);
        graphics.renderOutline(x, barY, w, 9, 0xFF8E8062);
        graphics.drawString(font, tr("npc.quest.state." + data.questState()), x, barY + 17, COLOR_GREEN, false);
        if (!data.questRewardAvailable()) {
            graphics.drawString(font, tr("npc.quest.reward_missing"), x, barY + 31, 0xFF9A493E, false);
        }
    }

    private void renderCommandPage(GuiGraphics graphics, int x, int y, int w, String type) {
        drawWrapped(graphics, tr("npc." + type + ".description"), x, y, w, COLOR_INK, 5);
        drawCenteredNoShadow(graphics, tr("npc.command." + data.commandMode()), x + w / 2, y + 65, COLOR_GREEN);
    }

    private void renderTeaching(GuiGraphics graphics, int x, int y, int w) {
        graphics.drawString(font, tr("npc.teach.npc_spell"), x, y + 6, COLOR_MUTED, false);
        drawCenteredNoShadow(graphics, spellName(selected(data.spells(), npcSpellIndex)), x + w / 2, y + 54, COLOR_INK);
        graphics.drawString(font, tr("npc.teach.method", methodName(data.method())), x, y + 88, COLOR_INK, false);
        graphics.drawString(font, tr("npc.teach.your_spell"), x, y + 99, COLOR_MUTED, false);
        drawCenteredNoShadow(graphics, spellName(selected(data.playerSpells(), playerSpellIndex)), x + w / 2, y + 136, COLOR_INK);
        drawCenteredNoShadow(graphics, tr("npc.teach.your_method",
                        methodName(selected(data.playerMethods(), playerMethodIndex))),
                x + w / 2, y + 167, COLOR_INK);
        int selectedSpellTrust = selectedSpellTrustRequired();
        int spellTrustColor = data.trust() >= selectedSpellTrust ? COLOR_GREEN : 0xFF9A493E;
        int methodTrustColor = data.trust() >= data.methodLearningTrustRequired() ? COLOR_GREEN : 0xFF9A493E;
        graphics.drawString(font, tr("npc.teach.spell_trust", selectedSpellTrust),
                x, y + 178, spellTrustColor, false);
        graphics.drawString(font, tr("npc.teach.method_trust", data.methodLearningTrustRequired()),
                x, y + 189, methodTrustColor, false);
    }

    private void renderInventoryText(GuiGraphics graphics, int x, int y, int w) {
        if (data.canManageInventory()) {
            drawCenteredNoShadow(graphics, tr("npc.inventory.hint"), x + w / 2, y + 5, COLOR_MUTED);
        } else {
            drawCenteredNoShadow(graphics, tr("npc.inventory.require_trust", 50), x + w / 2, y + 5, 0xFF9A493E);
        }
        int firstSlot = data.inventory().isEmpty() ? 0 : inventoryScroll + 1;
        int lastSlot = Math.min(data.inventory().size(), inventoryScroll + INVENTORY_VISIBLE_SLOTS);
        drawCenteredNoShadow(graphics, Component.literal(firstSlot + "-" + lastSlot + " / " + data.inventory().size()),
                x + w / 2, y + 128, COLOR_MUTED);
    }

    @Override
    protected void renderForeground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        String action = switch (page) {
            case TALK -> DaoGuidanceContent.TALK_NPC;
            case INTERACT -> DaoGuidanceContent.WEAPON_ATTACK;
            case TRADE -> DaoGuidanceContent.TRADE_NPC;
            case QUEST -> DaoGuidanceContent.ACCEPT_QUEST;
            default -> "";
        };
        if (!action.isBlank() && DaoGuidanceClient.isCurrentAction(action)) {
            int pulse = 145 + (int) (Math.sin(
                    System.currentTimeMillis() / 180.0D) * 65.0D);
            int color = (Math.max(70, Math.min(230, pulse)) << 24)
                    | 0xF0C66B;
            for (NpcButton button : guidanceButtons) {
                if (button.active) {
                    graphics.renderOutline(button.getX() - 3,
                            button.getY() - 3,
                            button.getWidth() + 6,
                            button.getHeight() + 6, color);
                }
            }
        }
        if (page != Page.INVENTORY) return;
        int x = contentX();
        int y = contentY() + 31;
        int w = contentWidth();
        int gridWidth = INVENTORY_SLOT_SIZE * INVENTORY_COLUMNS
                + INVENTORY_SLOT_GAP * (INVENTORY_COLUMNS - 1);
        int gridX = x + (w - gridWidth) / 2;
        int visibleSlots = Math.min(INVENTORY_VISIBLE_SLOTS, data.inventory().size() - inventoryScroll);
        for (int i = 0; i < visibleSlots; i++) {
            NpcClientData.NpcInventorySlot slot = data.inventory().get(inventoryScroll + i);
            if (slot.empty()) {
                continue;
            }
            ItemStack stack = stack(slot.itemId(), slot.count());
            int itemX = gridX + (i % INVENTORY_COLUMNS) * (INVENTORY_SLOT_SIZE + INVENTORY_SLOT_GAP)
                    + (INVENTORY_SLOT_SIZE - 16) / 2;
            int itemY = y + 20 + (i / INVENTORY_COLUMNS) * (INVENTORY_SLOT_SIZE + INVENTORY_SLOT_GAP)
                    + (INVENTORY_SLOT_SIZE - 16) / 2;
            graphics.renderItem(stack, itemX, itemY);
            graphics.renderItemDecorations(font, stack, itemX, itemY);
        }
    }

    private int maxInventoryScroll() {
        int rows = (data.inventory().size() + INVENTORY_COLUMNS - 1) / INVENTORY_COLUMNS;
        return Math.max(0, (rows - INVENTORY_VISIBLE_ROWS) * INVENTORY_COLUMNS);
    }

    private void renderAbout(GuiGraphics graphics, int x, int y, int w) {
        int left = x;
        int right = x + w / 2 + 6;
        graphics.drawString(font, tr("npc.about.age", data.age()), left, y, COLOR_INK, false);
        graphics.drawString(font, tr("npc.about.reputation", data.reputation()), right, y, COLOR_INK, false);
        Component gender = Component.translatable(
                "screen." + ImmortalCultivationMod.MODID + ".npc.gender." + data.gender());
        graphics.drawString(font, tr("npc.about.gender", gender), left, y + 15, COLOR_INK, false);
        graphics.drawString(font, tr("npc.about.body", com.example.immortal_cultivation_mod.attachment.BodyTypes.displayName(data.bodyType())), right, y + 15, COLOR_INK, false);
        graphics.drawString(font, tr("npc.about.roots", data.spiritRoots().isEmpty() ? Component.literal("-") : com.example.immortal_cultivation_mod.attachment.SpiritRoots.elementsDisplayName(data.spiritRoots())),
                left, y + 30, COLOR_INK, false);
        graphics.drawString(font, tr("npc.about.health", Math.round(data.health()), Math.round(data.maxHealth())), left, y + 45, COLOR_INK, false);
        graphics.drawString(font, tr("npc.about.qi", data.qi(), data.maxQi()), right, y + 45, COLOR_INK, false);
        graphics.drawString(font, tr("npc.about.attack", trim(data.attack())), left, y + 60, COLOR_INK, false);
        graphics.drawString(font, tr("npc.about.armor", trim(data.armor())), right, y + 60, COLOR_INK, false);
        graphics.drawString(font, tr("npc.about.progress", data.cultivationProgress()), left, y + 75, COLOR_INK, false);
        graphics.drawString(font, tr("npc.about.method", methodName(data.method())), left, y + 90, COLOR_INK, false);

        String spells = data.spells().isEmpty() ? "-" : String.join(", ", data.spells().stream().map(this::spellNameString).toList());
        drawWrapped(graphics, tr("npc.about.spells", spells), left, y + 106, w, COLOR_INK, 3);

        int relationY = y + 142;
        drawRelationLine(graphics, left, relationY);
        Component family = Component.empty();
        for (String relative : data.family()) {
            if (!family.getSiblings().isEmpty()) family = family.copy().append("  ");
            String[] parts = relative.split("·", 2);
            String role = switch (parts[0]) {
                case "父亲" -> "father";
                case "母亲" -> "mother";
                case "同辈" -> "sibling";
                default -> null;
            };
            family = family.copy().append(role == null || parts.length < 2
                    ? Component.literal(relative) : tr("npc.family." + role, parts[1]));
        }
        if (data.family().isEmpty()) family = Component.literal("-");
        drawWrapped(graphics, tr("npc.about.family", family), left, relationY + 19, w, COLOR_INK, 3);
    }

    private void drawRelationLine(GuiGraphics graphics, int x, int y) {
        graphics.drawString(font,
                tr("npc.relation_summary", data.friendship(), data.trust(), data.affection(), data.hatred()),
                x, y, COLOR_GREEN, false);
    }

    private Component spellName(String id) {
        if (id == null || id.isBlank()) {
            return tr("npc.none");
        }
        var spell = ModSpells.get(id);
        return spell == null ? Component.literal(id) : spell.name();
    }

    private String spellNameString(String id) {
        return spellName(id).getString();
    }

    private Component methodName(String id) {
        if (id == null || id.isBlank()) {
            return tr("npc.none");
        }
        var method = CultivationMethods.get(id);
        return method == null ? Component.literal(id) : Component.translatable(method.nameKey());
    }

    private static String selected(List<String> values, int index) {
        return values.isEmpty() ? "" : values.get(clampIndex(index, values));
    }

    private static int clampIndex(int index, List<String> values) {
        return values.isEmpty() ? 0 : Mth.clamp(index, 0, values.size() - 1);
    }

    private static int previousIndex(int index, List<String> values) {
        return values.isEmpty() ? 0 : Math.floorMod(index - 1, values.size());
    }

    private static int nextIndex(int index, List<String> values) {
        return values.isEmpty() ? 0 : (index + 1) % values.size();
    }

    private int selectedSpellTrustRequired() {
        String spellId = selected(data.spells(), npcSpellIndex);
        return spellId.isBlank() ? 100 : data.spellLearningTrustRequirements().getOrDefault(spellId, 100);
    }

    private static ItemStack stack(String itemId, int count) {
        ResourceLocation key = ResourceLocation.tryParse(itemId);
        if (key == null) {
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.ITEM.get(key);
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item, Math.max(1, count));
    }

    private static String trim(float value) {
        return value == Math.round(value) ? Integer.toString(Math.round(value)) : String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private static Component tr(String key, Object... args) {
        return Component.translatable("screen." + ImmortalCultivationMod.MODID + "." + key, args);
    }

    @Override
    protected boolean shouldRenderStatus(String statusKey) {
        return switch (page) {
            case TALK -> statusKey.startsWith("npc.dialogue.")
                    || statusKey.equals("npc.status.hatred_refusal");
            case INTERACT -> statusKey.startsWith("npc.status.gift_")
                    || statusKey.startsWith("npc.status.praise")
                    || statusKey.startsWith("npc.status.joke_")
                    || statusKey.startsWith("npc.status.insult")
                    || statusKey.startsWith("npc.status.threat")
                    || statusKey.startsWith("npc.status.spar_")
                    || statusKey.startsWith("npc.status.sect_")
                    || statusKey.startsWith("npc.status.master_")
                    || statusKey.startsWith("npc.status.disciple_")
                    || statusKey.startsWith("npc.status.dao_companion_")
                    || statusKey.equals("npc.status.no_held_item")
                    || statusKey.equals("npc.status.inventory_full");
            case TRADE -> statusKey.startsWith("npc.status.trade_")
                    || statusKey.equals("npc.status.hatred_refusal");
            case QUEST -> statusKey.startsWith("npc.status.quest_");
            case TEACH -> statusKey.startsWith("npc.status.teaching_")
                    || statusKey.startsWith("npc.status.spell_")
                    || statusKey.startsWith("npc.status.method_")
                    || statusKey.startsWith("npc.status.npc_")
                    || statusKey.startsWith("npc.status.player_")
                    || statusKey.equals("npc.status.realm_too_low")
                    || statusKey.equals("npc.status.no_method");
            case INVENTORY -> statusKey.startsWith("npc.status.inventory_")
                    || statusKey.startsWith("npc.status.item_")
                    || statusKey.equals("npc.status.hatred_refusal")
                    || statusKey.equals("npc.status.no_held_item");
            case ABOUT -> false;
        };
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (page == Page.TRADE && data.tradeOffers().size() > 4) {
            int next = Mth.clamp(tradeScroll - (int) Math.signum(scrollY), 0, data.tradeOffers().size() - 4);
            if (next != tradeScroll) {
                tradeScroll = next;
                init(minecraft, width, height);
                return true;
            }
        }
        if (page == Page.INVENTORY && maxInventoryScroll() > 0) {
            int next = Mth.clamp(
                    inventoryScroll - (int) Math.signum(scrollY) * INVENTORY_COLUMNS,
                    0,
                    maxInventoryScroll());
            if (next != inventoryScroll) {
                inventoryScroll = next;
                init(minecraft, width, height);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(new NpcInteractionScreen(data.withoutStatus()));
    }
}
