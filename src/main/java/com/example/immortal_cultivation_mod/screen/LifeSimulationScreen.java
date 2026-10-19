package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.client.OuterSystemClient;
import com.example.immortal_cultivation_mod.network.OuterSystemPayloads;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import com.example.immortal_cultivation_mod.system.LifeSimulationWorldMap;
import com.example.immortal_cultivation_mod.system.LifeSimulationSystem;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

/** Windows-styled client view for the server-owned life simulation. */
public class LifeSimulationScreen extends Screen {
    private static final int WINDOW_W = 620;
    private static final int WINDOW_H = 390;
    private static final int MARGIN = 12;
    private static final float DEFAULT_SCALE = 0.82F;
    private static final int CONTENT_Y = 68;
    private static final int CONTENT_H = 274;
    private static final int HISTORY_VISIBLE_LINES = 14;
    private static final int TAB_WIDTH = 80;
    private static final int TAB_STEP = 83;
    private static final int FOOTER_Y = 350;

    private CompoundTag state;
    private final java.util.IdentityHashMap<ListTag, java.util.Map<Integer, List<FormattedCharSequence>>> historyLayouts =
            new java.util.IdentityHashMap<>();
    private net.minecraft.locale.Language historyLanguage;
    private net.minecraft.client.gui.Font historyFont;
    private Tab selectedTab = Tab.LIFE;
    private int selectedRelationship;
    private int historyScroll;
    private int diaryScroll;
    private int selectedMemory;
    private static final int RELATIONSHIP_VISIBLE = 6;
    private static final int RELATIONSHIP_ROW_HEIGHT = 38;
    private static final int RELATIONSHIP_ROWS_Y = 34;
    private static final int RELATIONSHIP_DETAIL_VISIBLE = 4;
    private LifeRelationshipFilter relationshipFilter = LifeRelationshipFilter.ALL;
    private int relationshipScroll;
    private int relationshipDetailScroll;
    private int cultivationScroll;
    private int methodScroll;
    private int assetScroll;
    private String inheritancePicker = "";
    private int inheritancePage;
    private boolean inheritanceWaiting;
    private long inheritanceWaitingRevision;
    private static final int INHERITANCE_PAGE_SIZE = 6;
    private MapLayer mapLayer = MapLayer.TERRAIN;
    private String travelMethod = "foot";
    private final SystemWindowState windowState =
            new SystemWindowState(WINDOW_W, WINDOW_H);
    private final List<SystemGuiButton> mainWindowButtons = new ArrayList<>();
    private final List<SystemGuiButton> detachedWindowButtons = new ArrayList<>();
    private WindowLayer widgetLayer = WindowLayer.MAIN;
    private Tab detachedTab;
    private SystemWindowState detachedWindow;
    private boolean detachedOnTop = true;
    private Tab lastClickedTab;
    private long lastTabClickMillis;

    public LifeSimulationScreen(CompoundTag state) {
        super(Component.translatable(
                "screen.immortal_cultivation_mod.life.title"));
        this.state = state.copy();
    }

    public void updateState(CompoundTag updated) {
        if (inheritanceWaiting && updated.getLong("LifeActionRevision") != inheritanceWaitingRevision) inheritanceWaiting = false;
        if (!life().getBoolean("Finished") && updated.getCompound("LifeSimulation").getBoolean("Finished")) {
            selectedTab = Tab.LIFE;
        }
        if (!java.util.Objects.equals(life().get("SimulationId"),
                updated.getCompound("LifeSimulation").get("SimulationId"))) {
            diaryScroll = 0;
            inheritancePicker = "";
            inheritanceWaiting = false;
        }
        state = updated.copy();
        if (life().getBoolean("RewardClaimed") || hasPendingInheritance(life())) inheritancePicker = "";
        historyLayouts.clear();
        clampScrolls();
        rebuildWidgets();
    }

    @Override
    protected void init() {
        historyLayouts.clear();
        mainWindowButtons.clear();
        detachedWindowButtons.clear();
        widgetLayer = WindowLayer.MAIN;
        windowState.initializeCentered(desktopWidth(), desktopHeight());
        CompoundTag life = life();
        if (life.getBoolean("Finished")) {
            detachedTab = null;
            detachedWindow = null;
        }

        if (!windowState.isMinimized()) {
            initMainWindowWidgets(left(), top(), life);
        }

        if (detachedTab != null
                && detachedWindow != null
                && hasGeneratedLife(life)
                && life.getBoolean("Active")
                && life.getBoolean("Alive")
                && !life.getBoolean("Finished")) {
            detachedWindow.initializeCentered(desktopWidth(), desktopHeight());
            if (!detachedWindow.isMinimized()) {
                widgetLayer = WindowLayer.DETACHED;
                try {
                    initTabWidgets(
                            detachedTab,
                            detachedWindow.x(),
                            detachedWindow.y(),
                            life);
                } finally {
                    widgetLayer = WindowLayer.MAIN;
                }
            }
        }
    }

    private void initMainWindowWidgets(
            int x, int y, CompoundTag life) {
        for (int i = 0; i < Tab.values().length; i++) {
            Tab tab = Tab.values()[i];
            SystemGuiButton tabButton = addWindowButton(new SystemGuiButton(
                    x + 19 + i * TAB_STEP,
                    y + 42,
                    TAB_WIDTH,
                    19,
                    tab == selectedTab
                            ? Component.literal("[").append(tab.label()).append("]")
                            : tab.label(),
                    () -> {
                        selectedTab = tab;
                        rebuildWidgets();
                    }));
            tabButton.active = tab != detachedTab;
        }

        if (!hasGeneratedLife(life)) {
            addActionButton(
                    x + WINDOW_W / 2 - 78,
                    y + 234,
                    156,
                    "screen.immortal_cultivation_mod.life.start",
                    "start",
                    life.getInt("Attempts") > 0);
            return;
        }
        if (selectedTab == Tab.RECORD) {
            initMemoryWidgets(x, y, life);
        }
        if (life.getBoolean("Finished")) {
            if (selectedTab == Tab.LIFE) {
                initFinishedWidgets(x, y, life);
            } else {
                addWindowButton(new SystemGuiButton(x + 478, y + FOOTER_Y, 124, 22,
                        Component.translatable("screen.immortal_cultivation_mod.life.view_inheritance"),
                        () -> { selectedTab = Tab.LIFE; rebuildWidgets(); }));
            }
            return;
        }
        if (!life.getBoolean("Active") || !life.getBoolean("Alive")) {
            addActionButton(
                    x + WINDOW_W / 2 - 78,
                    y + 234,
                    156,
                    "screen.immortal_cultivation_mod.life.new_life",
                    "start",
                    life.getInt("Attempts") > 0);
            return;
        }
        initTabWidgets(selectedTab, x, y, life);
        addWindowButton(new SystemGuiButton(x + 18, y + FOOTER_Y, 90, 22, Component.translatable("life.expansion.routine"),
                () -> OuterSystemClient.transitionTo(new LifePlanningScreen(state, false))));
        addWindowButton(new SystemGuiButton(x + 112, y + FOOTER_Y, 90, 22, Component.translatable("life.expansion.journal"),
                () -> OuterSystemClient.transitionTo(new LifePlanningScreen(state, true))));
        addActionButton(
                x + 510,
                y + FOOTER_Y,
                92,
                "screen.immortal_cultivation_mod.life.settle",
                "settle_confirm",
                true);
    }

    private void initTabWidgets(
            Tab tab, int x, int y, CompoundTag life) {
        if (tab == Tab.LIFE) {
            initLifeWidgets(x, y, life);
        } else if (tab == Tab.RELATIONSHIPS) {
            initRelationshipWidgets(x, y, life);
        } else if (tab == Tab.CULTIVATION) {
            initCultivationWidgets(x, y, life);
        } else if (tab == Tab.ACTIVITIES) {
            initActivityWidgets(x, y, life);
        } else if (tab == Tab.MAP) {
            initMapWidgets(x, y, life);
        } else if (tab == Tab.ASSETS) {
            initUpgradeWidgets(x, y, life);
        } else if (tab == Tab.RECORD && widgetLayer == WindowLayer.DETACHED) {
            initMemoryWidgets(x, y, life);
        }
    }

    private void initMapWidgets(int x, int y, CompoundTag life) {
        CompoundTag map = LifeSimulationWorldMap.state(life);
        for (int i = 0; i < MapLayer.values().length; i++) {
            MapLayer layer = MapLayer.values()[i];
            addWindowButton(new SystemGuiButton(
                    x + 21 + i * 61,
                    y + 70,
                    56,
                    16,
                    layer == mapLayer
                            ? Component.literal("[").append(layer.label()).append("]")
                            : layer.label(),
                    () -> {
                        mapLayer = layer;
                        rebuildWidgets();
                    }));
        }

        String selected = map.getString(
                LifeSimulationWorldMap.SELECTED_DESTINATION);
        for (LifeSimulationWorldMap.LocationDefinition location
                : LifeSimulationWorldMap.locations()) {
            if (!LifeSimulationWorldMap.discovered(map, location.id())) {
                continue;
            }
            int nodeX = mapNodeX(x, location) - 31;
            int nodeY = mapNodeY(y, location) - 7;
            Component name = Component.translatable(location.nameKey());
            if (location.id().equals(selected)) {
                name = Component.literal("[").append(name).append("]");
            }
            String destinationId = location.id();
            addWindowButton(new SystemGuiButton(
                    nodeX,
                    nodeY,
                    62,
                    15,
                    name,
                    () -> send("map:select:" + destinationId)));
        }

        for (int i = 0; i < LifeSimulationWorldMap.travelMethods().size(); i++) {
            LifeSimulationWorldMap.TravelMethodDefinition method =
                    LifeSimulationWorldMap.travelMethods().get(i);
            int column = i % 2;
            int row = i / 2;
            Component name = Component.translatable(method.nameKey());
            if (method.id().equals(travelMethod)) {
                name = Component.literal("[").append(name).append("]");
            }
            String methodId = method.id();
            SystemGuiButton button = addWindowButton(new SystemGuiButton(
                    x + 411 + column * 91,
                    y + 208 + row * 20,
                    86,
                    17,
                    name,
                    () -> {
                        travelMethod = methodId;
                        rebuildWidgets();
                    }));
            button.active = !LifeSimulationWorldMap.hasActiveJourney(life)
                    && !LifeSimulationWorldMap.hasPendingArrival(life);
        }

        if (LifeSimulationWorldMap.hasPendingArrival(life)) {
            addMapActionButton(x + 411, y + 286, 58, "arrival.stay",
                    "arrival:stay", true);
            addMapActionButton(x + 472, y + 286, 58, "arrival.settle",
                    "arrival:settle", life.getLong("Wealth") >= 100L);
            CompoundTag currentState = LifeSimulationWorldMap.locationState(
                    map, map.getString(LifeSimulationWorldMap.CURRENT_LOCATION));
            addMapActionButton(x + 533, y + 286, 58, "arrival.cave",
                    "arrival:cave", life.getInt("Realm") >= 7
                            && life.getLong("Wealth") >= 5_000L
                            && currentState.getInt("LocalReputation") >= 20);
            return;
        }

        if (!LifeSimulationWorldMap.hasActiveJourney(life)) {
            CompoundTag local = LifeSimulationWorldMap.locationState(
                    map, map.getString(LifeSimulationWorldMap.CURRENT_LOCATION));
            addMapActionButton(x + 411, y + 273, 86, "local_activity",
                    "local_activity", canAct(life));
            addMapActionButton(x + 502, y + 273, 89, "main_quest",
                    "quest", !life.contains("CurrentEvent", Tag.TAG_COMPOUND)
                            && com.example.immortal_cultivation_mod.system.LifeAuthoredStories.remaining(life)
                            && com.example.immortal_cultivation_mod.system.LifeAuthoredStories.conscious(life)
                            && life.getInt("Age")>=16);

            LifeSimulationWorldMap.TravelPreview preview =
                    LifeSimulationWorldMap.preview(
                            life,
                            map.getString(LifeSimulationWorldMap.SELECTED_DESTINATION),
                            travelMethod);
            addMapActionButton(x + 411, y + 297, 180, "travel",
                    "travel:" + travelMethod, preview.available());

            ListTag teleports = map.getList(
                    LifeSimulationWorldMap.REGISTERED_TELEPORTS, Tag.TAG_STRING);
            String current = map.getString(LifeSimulationWorldMap.CURRENT_LOCATION);
            addMapActionButton(x + 411, y + 321, 86, "register_teleport",
                    "register_teleport", local.getBoolean("TeleportationActive")
                            && !containsString(teleports, current)
                            && life.getLong("Wealth") >= 500L);
            addMapActionButton(x + 502, y + 321, 89, "buy_mount",
                    "buy_mount", !map.getBoolean("HasMount")
                            && life.getLong("Wealth") >= 800L);
        }
    }

    private void addMapActionButton(
            int x,
            int y,
            int width,
            String translationSuffix,
            String action,
            boolean active) {
        SystemGuiButton button = addWindowButton(new SystemGuiButton(
                x,
                y,
                width,
                18,
                Component.translatable(
                        "screen.immortal_cultivation_mod.life.map."
                                + translationSuffix),
                () -> send("map:" + action)));
        button.active = active;
    }

    private void initLifeWidgets(int x, int y, CompoundTag life) {
        if (!life.getCompound("CurrentEvent").isEmpty()) {
            addWindowButton(new SystemGuiButton(x+224,y+186,360,28,
                    Component.translatable("life.battle.title"),
                    ()->OuterSystemClient.transitionTo(new LifeEncounterScreen(state))));
        }
        if (life.getCompound("CurrentEvent").isEmpty()) {
            addActionButton(
                    x + 310,
                    y + FOOTER_Y,
                    90,
                    "screen.immortal_cultivation_mod.life.advance",
                    "advance",
                    canAdvance(life));
            boolean fast = hasUpgrade(life, "continuous_memory");
            addActionButton(
                    x + 406,
                    y + FOOTER_Y,
                    98,
                    "screen.immortal_cultivation_mod.life.advance_five",
                    "advance_5",
                    fast && canAdvance(life));
        }
        addActionButton(x+206,y+FOOTER_Y,98,"life.expansion.routine.run",
                life.hasUUID("SimulationId")?"routine:run:"+life.getUUID("SimulationId")+":"+life.getInt("Age"):"routine:run",
                LifeSimulationSystem.canRunPlannedYear(life));
        if (life.getInt("Age") == 0) {
            addActionButton(
                    x + 27,
                    y + 292,
                    166,
                    "screen.immortal_cultivation_mod.life.reroll",
                    "reroll",
                    !life.getBoolean("BirthRerolled")
                            && life.getInt("Insight") >= 30);
        }
    }

    private void initRelationshipWidgets(int x, int y, CompoundTag life) {
        addWindowButton(new SystemGuiButton(x + 24, y + CONTENT_Y + 5, 344, 22,
                relationshipFilter.label(), () -> {
                    relationshipFilter = relationshipFilter.next();
                    relationshipScroll = 0; relationshipDetailScroll = 0;
                    rebuildWidgets();
                }));
        ListTag relationships = life.getList("Relationships", Tag.TAG_COMPOUND);
        List<Integer> known = knownRelationshipIndices(life);
        if (known.isEmpty()) {
            return;
        }
        if (!known.contains(selectedRelationship)) selectedRelationship = known.getFirst();
        int baseX = x + 409;
        CompoundTag person = relationships.getCompound(selectedRelationship);
        boolean canInteract = canAct(life) && person.getBoolean("Alive") && !person.getBoolean("Captive");
        addRelationshipButton(baseX, y + 240, "talk", canInteract);
        addRelationshipButton(baseX, y + 265, "gift",
                canInteract && life.getLong("Wealth") >= 50L);
        addRelationshipButton(baseX, y + 290, "help", canInteract);
        String[] advanced = {"friend", "breakup", "protector", "nemesis"};
        for (int i = 0; i < advanced.length; i++) {
            String action = advanced[i];
            if (action.equals("friend") && LifeSimulationSystem.isParent(person)) continue;
            SystemGuiButton button = addWindowButton(new SystemGuiButton(baseX + i % 2 * 88, y + 184 + i / 2 * 24, 84, 20,
                    Component.translatable("life.expansion.npc." + action), () -> send("relationship:" + relationshipTarget() + ":" + action)));
            button.active = canInteract && (!action.equals("friend") || person.getInt("Affinity") >= 30)
                    && (!action.equals("protector") || person.getBoolean("Friend") && person.getInt("Trust") >= 40);
        }
    }

    private void addRelationshipButton(
            int x, int y, String action, boolean active) {
        SystemGuiButton button = addWindowButton(new SystemGuiButton(
                x,
                y,
                172,
                20,
                Component.translatable(
                        "screen.immortal_cultivation_mod.life.relationship."
                                + action),
                () -> send("relationship:" + relationshipTarget() + ":" + action)));
        button.active = active;
    }

    private String relationshipTarget() {
        ListTag people = life().getList("Relationships", Tag.TAG_COMPOUND);
        if (people.isEmpty()) return "-1";
        CompoundTag person = people.getCompound(Mth.clamp(selectedRelationship, 0, people.size() - 1));
        return person.hasUUID("Id") ? person.getUUID("Id").toString()
                : Integer.toString(person.contains("ServerIndex") ? person.getInt("ServerIndex") : selectedRelationship);
    }

    private void initCultivationWidgets(int x, int y, CompoundTag life) {
        List<String> actions = new ArrayList<>(List.of(
                "cultivate", "practice", "seclusion"));
        if (!"none".equals(life.getString("Sect"))) {
            actions.add("sect_mission");
        }
        int buttonY = y + 314;
        int buttonX = x + 218;
        int gap = 6;
        int availableWidth = 376;
        int buttonWidth = (availableWidth - gap * (actions.size() - 1))
                / actions.size();
        for (int i = 0; i < actions.size(); i++) {
            addActivityButton(
                    buttonX + i * (buttonWidth + gap),
                    buttonY,
                    buttonWidth,
                    actions.get(i),
                    life);
        }
    }

    private void initActivityWidgets(int x, int y, CompoundTag life) {
        String[] activities = LifeSimulationSystem.ACTIVITIES.stream().filter(action -> !action.equals("keep_oath")).toArray(String[]::new);
        for (int i = 0; i < activities.length; i++) {
            int column = i % 3;
            int row = i / 3;
            addActivityButton(
                    x + 36 + column * 184,
                    y + 106 + row * 38,
                    164,
                    activities[i],
                    life);
        }
    }

    private void addActivityButton(
            int x,
            int y,
            int width,
            String activity,
            CompoundTag life) {
        SystemGuiButton button = addWindowButton(new SystemGuiButton(
                x,
                y,
                width,
                22,
                Component.translatable(
                        "screen.immortal_cultivation_mod.life.activity."
                                + activity),
                () -> send("activity:" + activity)));
        button.active = activityAvailable(life, activity);
        if (!button.active) button.setTooltip(net.minecraft.client.gui.components.Tooltip.create(activityBlockedReason(life,activity)));
    }

    private Component activityBlockedReason(CompoundTag life,String activity) {
        String reason=LifeSimulationSystem.activityUnavailable(life,activity);
        return Component.translatable("life.expansion.activity_blocked."+reason);
    }

    private void initUpgradeWidgets(int x, int y, CompoundTag life) {
        String[] upgrades = {
                "danger_sense", "previous_luck", "family_blessing",
                "continuous_memory", "inheritance_expansion",
                "fate_not_end", "fate_retention"
        };
        int[] costs = {40, 60, 50, 80, 100, 120, 140};
        for (int i = 0; i < upgrades.length; i++) {
            String id = upgrades[i];
            int cost = costs[i];
            boolean owned = hasUpgrade(life, id);
            SystemGuiButton button = addWindowButton(new SystemGuiButton(
                    x + 394,
                    y + 110 + i * 34,
                    188,
                    20,
                    owned
                            ? Component.translatable(
                                    "screen.immortal_cultivation_mod.life.upgrade.owned")
                            : Component.translatable(
                                    "screen.immortal_cultivation_mod.life.upgrade.buy",
                                    cost),
                    () -> send("upgrade:" + id)));
            button.active = !owned && life.getInt("Insight") >= cost;
        }
    }

    private void initMemoryWidgets(int x, int y, CompoundTag life) {
        int count = life.getList("Memories", Tag.TAG_COMPOUND).size();
        SystemGuiButton previous = addWindowButton(new SystemGuiButton(
                x + 26, y + CONTENT_Y + 9, 28, 20, Component.literal("<"),
                () -> { selectedMemory++; historyScroll = 0; rebuildWidgets(); }));
        previous.active = selectedMemory < count;
        SystemGuiButton next = addWindowButton(new SystemGuiButton(
                x + WINDOW_W - 54, y + CONTENT_Y + 9, 28, 20, Component.literal(">"),
                () -> { selectedMemory--; historyScroll = 0; rebuildWidgets(); }));
        next.active = selectedMemory > 0;
    }

    private CompoundTag memoryLife(CompoundTag life) {
        ListTag memories = life.getList("Memories", Tag.TAG_COMPOUND);
        selectedMemory = Mth.clamp(selectedMemory, 0, memories.size());
        return selectedMemory == 0 ? life : memories.getCompound(memories.size() - selectedMemory);
    }

    private List<FormattedCharSequence> memoryLines(CompoundTag life) {
        return historyLines(memoryLife(life), WINDOW_W - 82);
    }

    private List<FormattedCharSequence> historyLines(CompoundTag life, int width) {
        ListTag history = life.getList("History", Tag.TAG_COMPOUND);
        if (history.isEmpty()) return List.of();
        var language = net.minecraft.locale.Language.getInstance();
        if (historyLanguage != language || historyFont != font) {
            historyLayouts.clear(); historyLanguage = language; historyFont = font;
        }
        var widths = historyLayouts.computeIfAbsent(history, ignored -> new java.util.HashMap<>());
        var cached = widths.get(width);
        if (cached != null) return cached;
        List<FormattedCharSequence> lines = new ArrayList<>();
        for (int i = 0; i < history.size(); i++) {
            CompoundTag entry = history.getCompound(i);
            lines.addAll(font.split(Component.translatable(
                    "screen.immortal_cultivation_mod.life.log_line", entry.getInt("Age"),
                    Component.translatable(entry.getString("Key"))), width));
        }
        List<FormattedCharSequence> layout = List.copyOf(lines);
        widths.put(width, layout);
        return layout;
    }

    private Component inheritanceDetail(CompoundTag preview) {
        String prefix = "screen.immortal_cultivation_mod.life.inherit.detail.";
        return switch (preview.getString("Kind")) {
            case "materials" -> {
                ResourceLocation id = ResourceLocation.tryParse(preview.getString("Item"));
                Item item = id == null ? Items.AIR : BuiltInRegistries.ITEM.get(id);
                Component name = item == Items.AIR ? Component.translatable("life.expansion.unknown_item") : item.getDescription();
                yield Component.translatable(prefix + "materials", name, preview.getInt("Count"));
            }
            case "progress" -> Component.translatable(prefix + "progress", preview.getLong("Gain"));
            case "spell", "proficiency" -> {
                ModSpells.SpellDef spell = ModSpells.get(preview.getString("Spell"));
                yield Component.translatable(prefix + preview.getString("Kind"),
                        spell == null ? Component.translatable("life.expansion.unknown_spell") : spell.name(), preview.getInt("Gain"));
            }
            case "blessing" -> Component.translatable(prefix + "blessing", preview.getInt("Minutes"));
            case "none" -> Component.translatable("life.expansion.inherit." + preview.getString("Reason"));
            default -> Component.translatable(prefix + "unknown");
        };
    }

    private void initFinishedWidgets(int x, int y, CompoundTag life) {
        if (!inheritancePicker.isEmpty() && !life.getBoolean("RewardClaimed") && !hasPendingInheritance(life)) {
            initInheritancePicker(x, y, life);
            return;
        }
        if (!life.getBoolean("RewardClaimed")) {
            CompoundTag pending = life.getCompound("PendingInheritance");
            boolean reserved = hasPendingInheritance(life);
            ListTag rewards = life.getList("InheritanceRewards", Tag.TAG_COMPOUND);
            int gap = 8;
            int buttonWidth = (WINDOW_W - 56 - gap * Math.max(0, rewards.size() - 1)) / Math.max(1, rewards.size());
            for (int i = 0; i < rewards.size(); i++) {
                CompoundTag reward = rewards.getCompound(i);
                boolean chosen = reserved && reward.getString("Id").equals(pending.getString("RewardId"));
                SystemGuiButton button = addWindowButton(new SystemGuiButton(
                        x + 28 + i * (buttonWidth + gap),
                        y + (reserved ? 295 : 300),
                        buttonWidth,
                        24,
                        !reserved && List.of("spell", "materials").contains(reward.getString("Id"))
                                ? Component.translatable("life.expansion.inherit.select." + reward.getString("Id"))
                                : rewardLabel(chosen ? pending.getCompound("Selected") : reward),
                        () -> {
                            String category = reward.getString("Id");
                            if (List.of("spell", "materials").contains(category)) {
                                inheritancePicker = category;
                                inheritancePage = reward.getInt("ChoiceIndex") / INHERITANCE_PAGE_SIZE;
                                rebuildWidgets();
                            } else send("inherit:" + category);
                        }));
                CompoundTag preview = chosen ? pending.getCompound("Receipt") : reward.getCompound("Preview");
                button.active = !reserved && !"none".equals(preview.getString("Kind"));
                Component tooltip = inheritanceDetail(preview);
                if (reward.getString("Id").equals("progress") && !"none".equals(preview.getString("Kind"))) {
                    tooltip = tooltip.copy().append("\n").append(Component.translatable("life.expansion.inherit.progress_source",
                            reward.getLong("SourceCultivation"), reward.getInt("Percent")))
                            .append("\n").append(Component.translatable("life.expansion.inherit.destination",
                                    CultivationLevels.displayName(preview.getString("DestinationLevel")),
                                    preview.getLong("DestinationProgress"),
                                    CultivationLevels.getTotalQiNeeded(preview.getString("DestinationLevel"))));
                }
                button.setTooltip(net.minecraft.client.gui.components.Tooltip.create(tooltip));
            }
            if (!reserved && rewards.stream().allMatch(t -> "none".equals(((CompoundTag)t).getCompound("Preview").getString("Kind")))) {
                addWindowButton(new SystemGuiButton(x + WINDOW_W / 2 - 90, y + FOOTER_Y, 180, 22,
                        Component.translatable("life.expansion.inherit.continue_empty"), () -> send("inherit:none")));
            }
            if (reserved) {
                SystemGuiButton retry = addWindowButton(new SystemGuiButton(x + WINDOW_W / 2 - 82, y + FOOTER_Y,
                        164, 22, Component.translatable("life.expansion.inherit.retry"),
                        () -> send("inherit:" + pending.getString("RewardId"))));
                retry.active = !pending.getString("RewardId").isBlank();
                retry.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("life.expansion.inherit.space")));
            }
        } else {
            addActionButton(
                    x + WINDOW_W / 2 - 82,
                    y + 306,
                    164,
                    "screen.immortal_cultivation_mod.life.new_life",
                    "start",
                    life.getInt("Attempts") > 0);
        }
    }

    private CompoundTag pickerReward(CompoundTag life) {
        for (var tag : life.getList("InheritanceRewards", Tag.TAG_COMPOUND)) {
            var reward = (CompoundTag) tag;
            if (reward.getString("Id").equals(inheritancePicker)) return reward;
        }
        return new CompoundTag();
    }

    private void initInheritancePicker(int x, int y, CompoundTag life) {
        var reward = pickerReward(life);
        var options = reward.getList("Choices", Tag.TAG_COMPOUND);
        int pages = Math.max(1, (options.size() + INHERITANCE_PAGE_SIZE - 1) / INHERITANCE_PAGE_SIZE);
        inheritancePage = Mth.clamp(inheritancePage, 0, pages - 1);
        for (int row = 0; row < INHERITANCE_PAGE_SIZE; row++) {
            int index = inheritancePage * INHERITANCE_PAGE_SIZE + row;
            if (index >= options.size()) break;
            var option = options.getCompound(index);
            boolean selected = index == reward.getInt("ChoiceIndex");
            Component label = inheritanceDetail(option.getCompound("Preview"));
            if (selected) label = Component.translatable("life.expansion.inherit.picker.selected", label);
            var choice = addWindowButton(new SystemGuiButton(x + 44, y + 113 + row * 27, WINDOW_W - 88, 24, label, () -> {
                if (inheritanceWaiting) return;
                inheritanceWaiting = true;
                inheritanceWaitingRevision = state.getLong("LifeActionRevision");
                send("inherit_select:" + inheritancePicker + ":" + index);
                rebuildWidgets();
            }));
            choice.active = !inheritanceWaiting;
            choice.setTooltip(net.minecraft.client.gui.components.Tooltip.create(inheritanceDetail(option.getCompound("Preview"))));
        }
        var previous = addWindowButton(new SystemGuiButton(x + 44, y + 282, 70, 20, Component.literal("<"), () -> {
            inheritancePage--; rebuildWidgets();
        }));
        previous.active = inheritancePage > 0 && !inheritanceWaiting;
        var next = addWindowButton(new SystemGuiButton(x + WINDOW_W - 114, y + 282, 70, 20, Component.literal(">"), () -> {
            inheritancePage++; rebuildWidgets();
        }));
        next.active = inheritancePage + 1 < pages && !inheritanceWaiting;
        var back = addWindowButton(new SystemGuiButton(x + 44, y + 348, 112, 24,
                Component.translatable("life.battle.back"), () -> { inheritancePicker = ""; rebuildWidgets(); }));
        back.active = !inheritanceWaiting;
        var confirm = addWindowButton(new SystemGuiButton(x + 170, y + 348, WINDOW_W - 214, 24,
                Component.translatable("life.expansion.inherit.picker.claim", inheritanceDetail(reward.getCompound("Preview"))),
                () -> {
                    if (inheritanceWaiting) return;
                    inheritanceWaiting = true;
                    inheritanceWaitingRevision = state.getLong("LifeActionRevision");
                    send("inherit:" + inheritancePicker);
                    rebuildWidgets();
                }));
        confirm.active = !inheritanceWaiting && !options.isEmpty() && !"none".equals(reward.getCompound("Preview").getString("Kind"));
        confirm.setTooltip(net.minecraft.client.gui.components.Tooltip.create(inheritanceDetail(reward.getCompound("Preview"))));
    }

    private void renderInheritancePicker(GuiGraphics graphics, int x, int y, CompoundTag life) {
        SystemGuiTextures.drawInsetPanel(graphics, x + 28, y + 76, WINDOW_W - 56, 260);
        var reward = pickerReward(life);
        int count = reward.getList("Choices", Tag.TAG_COMPOUND).size();
        graphics.drawCenteredString(font, Component.translatable("life.expansion.inherit.select." + inheritancePicker),
                x + WINDOW_W / 2, y + 88, 0xFF17325E);
        graphics.drawCenteredString(font, Component.translatable("life.expansion.inherit.picker.page",
                        inheritancePage + 1, Math.max(1, (count + INHERITANCE_PAGE_SIZE - 1) / INHERITANCE_PAGE_SIZE), count),
                x + WINDOW_W / 2, y + 287, 0xFF17325E);
        graphics.drawCenteredString(font, Component.translatable("life.expansion.inherit.picker.help"),
                x + WINDOW_W / 2, y + 315, 0xFF404040);
    }

    private boolean hasPendingInheritance(CompoundTag life) {
        return "pending".equals(life.getString("ClaimTransaction"))
                && life.contains("PendingInheritance", Tag.TAG_COMPOUND) && !life.getBoolean("RewardClaimed");
    }

    private Component rewardLabel(CompoundTag reward) {
        return switch (reward.getString("Id")) {
            case "progress" -> Component.translatable(
                    "screen.immortal_cultivation_mod.life.inherit.progress",
                    reward.getInt("Percent"));
            case "spell" -> {
                ModSpells.SpellDef spell = ModSpells.get(reward.getString("Spell"));
                yield spell == null ? Component.translatable("life.expansion.inherit.no_spell_title") : Component.translatable(
                        "screen.immortal_cultivation_mod.life.inherit.spell",
                        spell == null ? Component.translatable("life.expansion.unknown_spell") : spell.name());
            }
            case "blessing" -> Component.translatable(
                    "screen.immortal_cultivation_mod.life.inherit.blessing");
            default -> Component.translatable(
                    "screen.immortal_cultivation_mod.life.inherit.materials");
        };
    }

    private void addActionButton(
            int x,
            int y,
            int width,
            String translationKey,
            String action,
            boolean active) {
        SystemGuiButton button = addWindowButton(new SystemGuiButton(
                x,
                y,
                width,
                22,
                Component.translatable(translationKey),
                () -> send(action)));
        button.active = active;
    }

    private void send(String action) {
        if (action.equals("settle_confirm")) {
            com.example.immortal_cultivation_mod.client.OuterSystemClient.showLifeConfirmation(this, () -> send("settle"));
            return;
        }
        PacketDistributor.sendToServer(
                new OuterSystemPayloads.ServerboundLifeAction(action));
    }

    private SystemGuiButton addWindowButton(SystemGuiButton button) {
        SystemWindowState owner = widgetLayer == WindowLayer.DETACHED
                ? detachedWindow
                : windowState;
        if (owner != null) {
            SystemWindowState.ContentLayout layout = contentLayout(owner);
            button.setScaledBounds(
                    layout.mapX(button.getX()),
                    layout.mapY(button.getY()),
                    layout.mapWidth(button.getWidth()),
                    layout.mapHeight(button.getHeight()),
                    layout.scale());
        }
        addRenderableWidget(button);
        if (widgetLayer == WindowLayer.DETACHED) {
            detachedWindowButtons.add(button);
        } else {
            mainWindowButtons.add(button);
        }
        return button;
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
        CompoundTag life = life();
        boolean showDetached = hasVisibleDetachedWindow();
        if (showDetached && !detachedOnTop) {
            renderDetachedWindowLayer(
                    graphics, life, uiMouseX, uiMouseY, partialTick);
        }
        renderMainWindowLayer(
                graphics, life, uiMouseX, uiMouseY, partialTick);
        if (showDetached && detachedOnTop) {
            renderDetachedWindowLayer(
                    graphics, life, uiMouseX, uiMouseY, partialTick);
        }
        restoreButtonVisibility();
        graphics.pose().popPose();
        SystemGuiButton.keepTooltipOnScreen(this);
    }

    private void renderMainWindowLayer(
            GuiGraphics graphics,
            CompoundTag life,
            int mouseX,
            int mouseY,
            float partialTick) {
        int x = left();
        int y = top();
        enableWindowScissor(graphics, windowState);
        SystemGuiTextures.drawWindow(
                graphics,
                x,
                y,
                windowState.width(),
                windowState.renderedHeight());
        drawWindowTitle(
                graphics,
                Component.translatable(
                        "system.immortal_cultivation_mod.life_simulation"),
                x,
                y,
                windowState.width());

        if (windowState.isMinimized()) {
            renderButtonLayer(
                    WindowLayer.MAIN, graphics, mouseX, mouseY, partialTick);
            graphics.disableScissor();
            return;
        }
        if (!windowState.isMaximized()) {
            SystemGuiTextures.drawResizeGrip(
                    graphics, x, y, windowState.width(), windowState.height());
        }
        drawMainTitleDetails(graphics, life, x, y);
        SystemWindowState.ContentLayout layout = contentLayout(windowState);
        int contentMouseX = (int) Math.floor(layout.virtualX(mouseX));
        int contentMouseY = (int) Math.floor(layout.virtualY(mouseY));
        graphics.pose().pushPose();
        graphics.pose().translate(
                layout.translationX(), layout.translationY(), 0.0F);
        graphics.pose().scale(layout.scale(), layout.scale(), 1.0F);
        if (!hasGeneratedLife(life)) {
            renderEmpty(graphics, x, y, life);
        } else if (life.getBoolean("Finished") && selectedTab == Tab.LIFE) {
            renderFinished(graphics, x, y, life);
        } else if (!life.getBoolean("Active") && !life.getBoolean("Finished")) {
            renderEmpty(graphics, x, y, life);
        } else {
            renderTab(
                    graphics,
                    selectedTab,
                    x,
                    y,
                    life,
                    contentMouseX,
                    contentMouseY);
        }
        graphics.pose().popPose();
        renderButtonLayer(
                WindowLayer.MAIN, graphics, mouseX, mouseY, partialTick);
        graphics.disableScissor();
    }

    private void renderTab(
            GuiGraphics graphics,
            Tab tab,
            int x,
            int y,
            CompoundTag life,
            int mouseX,
            int mouseY) {
        switch (tab) {
            case LIFE -> renderLife(graphics, x, y, life);
            case RELATIONSHIPS ->
                    renderRelationships(graphics, x, y, life, mouseX, mouseY);
            case CULTIVATION -> renderCultivation(graphics, x, y, life);
            case ACTIVITIES -> renderActivities(graphics, x, y, life);
            case MAP -> renderMap(graphics, x, y, life);
            case ASSETS -> renderAssets(graphics, x, y, life);
            case RECORD -> renderRecord(graphics, x, y, life);
        }
    }

    private void renderMap(
            GuiGraphics graphics, int x, int y, CompoundTag life) {
        CompoundTag map = LifeSimulationWorldMap.state(life);
        int mapX = x + 18;
        int mapY = y + 89;
        int mapW = 380;
        int mapH = 253;
        int sideX = x + 405;
        int sideW = 197;
        SystemGuiTextures.drawInsetPanel(graphics, mapX, mapY, mapW, mapH);
        SystemGuiTextures.drawInsetPanel(graphics, sideX, y + 68, sideW, 274);

        for (LifeSimulationWorldMap.RouteDefinition route
                : LifeSimulationWorldMap.routes()) {
            if (!LifeSimulationWorldMap.routeUnlocked(map, route)) {
                continue;
            }
            LifeSimulationWorldMap.LocationDefinition first =
                    LifeSimulationWorldMap.location(route.first());
            LifeSimulationWorldMap.LocationDefinition second =
                    LifeSimulationWorldMap.location(route.second());
            if (first == null || second == null
                    || !LifeSimulationWorldMap.discovered(map, first.id())
                    || !LifeSimulationWorldMap.discovered(map, second.id())) {
                continue;
            }
            int color = mapLayer == MapLayer.TRADE && route.tradeRoute()
                    ? 0xFFD49B32
                    : 0xFF8C8B82;
            drawMapLine(
                    graphics,
                    mapNodeX(x, first),
                    mapNodeY(y, first),
                    mapNodeX(x, second),
                    mapNodeY(y, second),
                    color);
        }

        String current = map.getString(LifeSimulationWorldMap.CURRENT_LOCATION);
        String selected = map.getString(
                LifeSimulationWorldMap.SELECTED_DESTINATION);
        LifeSimulationWorldMap.LocationDefinition destination =
                LifeSimulationWorldMap.location(selected);
        CompoundTag local = destination == null
                ? new CompoundTag()
                : LifeSimulationWorldMap.locationState(map, selected);
        drawMapDetailLine(graphics, sideX + 10, y + 78, "current",
                locationName(current));
        drawMapDetailLine(graphics, sideX + 10, y + 94, "destination",
                locationName(selected));
        if (destination != null) {
            drawMapDetailLine(graphics, sideX + 10, y + 110, "terrain",
                    Component.translatable(destination.typeKey()));
            drawMapDetailLine(graphics, sideX + 10, y + 126, "faction",
                    translatedValue("screen.immortal_cultivation_mod.life.map.faction.", local.getString("ControllingFaction")));
            drawMapDetailLine(graphics, sideX + 10, y + 142, "qi",
                    Component.literal(String.valueOf(
                            local.getInt("SpiritualDensity"))));
            drawMapDetailLine(graphics, sideX + 10, y + 158, "danger",
                    Component.literal(String.valueOf(local.getInt("Danger"))));
            drawMapDetailLine(graphics, sideX + 10, y + 174, "reputation",
                    Component.literal(String.valueOf(
                            local.getInt("LocalReputation"))));
        }

        LifeSimulationWorldMap.TravelPreview preview = destination == null
                ? null
                : LifeSimulationWorldMap.preview(life, selected, travelMethod);
        if (preview != null && preview.reachable()) {
            Component cost = Component.translatable(
                    "screen.immortal_cultivation_mod.life.map.preview",
                    preview.actionPointCost(),
                    preview.danger(),
                    preview.wealthCost(),
                    preview.spiritStoneCost(),
                    preview.qiCost());
            drawTrimmed(graphics, cost, sideX + 10, y + 193,
                    sideW - 20, 0xFF303030);
        }

        if (LifeSimulationWorldMap.hasActiveJourney(life)) {
            CompoundTag journey = map.getCompound(
                    LifeSimulationWorldMap.ACTIVE_JOURNEY);
            drawCenteredWrapped(
                    graphics,
                    Component.translatable(
                            "screen.immortal_cultivation_mod.life.map.journey",
                            locationName(journey.getString("Destination")),
                            journey.getInt("RemainingSegments")),
                    sideX + sideW / 2,
                    y + 260,
                    sideW - 20,
                    0xFF17325E);
        } else if (LifeSimulationWorldMap.hasPendingArrival(life)) {
            drawCenteredWrapped(
                    graphics,
                    Component.translatable(
                            "screen.immortal_cultivation_mod.life.map.arrival_prompt"),
                    sideX + sideW / 2,
                    y + 258,
                    sideW - 20,
                    0xFF17325E);
        } else if (local.getBoolean("MainQuestCompleted")) {
            drawTrimmed(
                    graphics,
                    Component.translatable(
                            "screen.immortal_cultivation_mod.life.map.quest_complete",
                            mapEndingName(selected, local.getString("MainQuestEnding"))),
                    mapX + 9,
                    mapY + mapH - 32,
                    mapW - 18,
                    0xFF31533A);
        }

        graphics.drawString(
                font,
                Component.translatable(
                        "screen.immortal_cultivation_mod.life.map.layer."
                                + mapLayer.id),
                mapX + 9,
                mapY + mapH - 14,
                0xFF4B4A45,
                false);
    }

    private void drawMapDetailLine(
            GuiGraphics graphics,
            int x,
            int y,
            String id,
            Component value) {
        drawTrimmed(
                graphics,
                Component.translatable(
                        "screen.immortal_cultivation_mod.life.map.detail." + id,
                        value),
                x,
                y,
                177,
                0xFF202020);
    }

    private Component locationName(String id) {
        LifeSimulationWorldMap.LocationDefinition location =
                LifeSimulationWorldMap.location(id);
        return location == null
                ? Component.translatable("screen.immortal_cultivation_mod.life.none")
                : Component.translatable(location.nameKey());
    }

    private Component mapEndingName(String locationId, String endingId) {
        String key = "life.immortal_cultivation_mod.map.quest." + locationId + ".ending." + endingId + ".choice";
        return Component.translatable(I18n.exists(key) ? key : "screen.immortal_cultivation_mod.life.none");
    }

    private static int mapNodeX(
            int windowX, LifeSimulationWorldMap.LocationDefinition location) {
        return windowX + 39 + Math.round(location.mapX() * 1.04F);
    }

    private static int mapNodeY(
            int windowY, LifeSimulationWorldMap.LocationDefinition location) {
        return windowY + 109 + Math.round(location.mapY() * 0.90F);
    }

    private static void drawMapLine(
            GuiGraphics graphics,
            int x1,
            int y1,
            int x2,
            int y2,
            int color) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        if (steps <= 0) {
            return;
        }
        for (int i = 0; i <= steps; i += 2) {
            int x = Mth.lerpInt(i / (float) steps, x1, x2);
            int y = Mth.lerpInt(i / (float) steps, y1, y2);
            graphics.fill(x, y, x + 2, y + 2, color);
        }
    }

    private static boolean containsString(ListTag list, String value) {
        for (int i = 0; i < list.size(); i++) {
            if (value.equals(list.getString(i))) {
                return true;
            }
        }
        return false;
    }

    private void renderDetachedWindowLayer(
            GuiGraphics graphics,
            CompoundTag life,
            int mouseX,
            int mouseY,
            float partialTick) {
        if (detachedTab == null || detachedWindow == null) {
            return;
        }
        int x = detachedWindow.x();
        int y = detachedWindow.y();
        enableWindowScissor(graphics, detachedWindow);
        SystemGuiTextures.drawWindow(
                graphics,
                x,
                y,
                detachedWindow.width(),
                detachedWindow.renderedHeight());
        drawWindowTitle(
                graphics,
                detachedTab.label(),
                x,
                y,
                detachedWindow.width());
        if (!detachedWindow.isMinimized()) {
            if (!detachedWindow.isMaximized()) {
                SystemGuiTextures.drawResizeGrip(
                        graphics,
                        x,
                        y,
                        detachedWindow.width(),
                        detachedWindow.height());
            }
            SystemWindowState.ContentLayout layout = contentLayout(detachedWindow);
            int contentMouseX = (int) Math.floor(layout.virtualX(mouseX));
            int contentMouseY = (int) Math.floor(layout.virtualY(mouseY));
            graphics.pose().pushPose();
            graphics.pose().translate(
                    layout.translationX(), layout.translationY(), 0.0F);
            graphics.pose().scale(layout.scale(), layout.scale(), 1.0F);
            renderTab(
                    graphics,
                    detachedTab,
                    x,
                    y,
                    life,
                    contentMouseX,
                    contentMouseY);
            graphics.pose().popPose();
        }
        renderButtonLayer(
                WindowLayer.DETACHED,
                graphics,
                mouseX,
                mouseY,
                partialTick);
        graphics.disableScissor();
    }

    private void renderButtonLayer(
            WindowLayer layer,
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick) {
        setButtonLayerVisibility(layer);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void setButtonLayerVisibility(WindowLayer layer) {
        boolean showMain = layer == WindowLayer.MAIN;
        for (SystemGuiButton button : mainWindowButtons) {
            button.visible = showMain;
        }
        for (SystemGuiButton button : detachedWindowButtons) {
            button.visible = !showMain;
        }
    }

    private void restoreButtonVisibility() {
        for (SystemGuiButton button : mainWindowButtons) {
            button.visible = true;
        }
        for (SystemGuiButton button : detachedWindowButtons) {
            button.visible = true;
        }
    }

    private void renderEmpty(
            GuiGraphics graphics, int x, int y, CompoundTag life) {
        SystemGuiTextures.drawInsetPanel(
                graphics, x + 72, y + 92, WINDOW_W - 144, 128);
        drawCenteredWrapped(
                graphics,
                Component.translatable(
                        "screen.immortal_cultivation_mod.life.empty"),
                x + WINDOW_W / 2,
                y + 119,
                370,
                0xFF202020);
        graphics.drawCenteredString(
                font,
                Component.translatable(
                        "screen.immortal_cultivation_mod.life.next_attempt",
                        life.getLong("DaysUntilAttempt")),
                x + WINDOW_W / 2,
                y + 188,
                0xFF3B3B3B);
    }

    private void renderLife(
            GuiGraphics graphics, int x, int y, CompoundTag life) {
        renderProfile(graphics, x + 18, y + CONTENT_Y, 184, CONTENT_H, life);
        int panelX = x + 210;
        int panelW = 392;
        SystemGuiTextures.drawInsetPanel(
                graphics, panelX, y + CONTENT_Y, panelW, CONTENT_H);
        if(!life.getCompound("CurrentEvent").isEmpty()){
            drawLabel(graphics,"life.battle.title",panelX+12,y+CONTENT_Y+12,0xFF17325E);
            return;
        }
        drawLabel(graphics,"screen.immortal_cultivation_mod.life.diary",
                panelX+12,y+CONTENT_Y+12,0xFF17325E);
        renderHistory(graphics,life,panelX+12,y+CONTENT_Y+33,panelW-40,16);
    }

    private void renderProfile(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            CompoundTag life) {
        SystemGuiTextures.drawInsetPanel(graphics, x, y, width, height);
        graphics.drawCenteredString(
                font,
                Component.literal(life.getString("Name")),
                x + width / 2,
                y + 11,
                0xFF17325E);
        int lineY = y + 30;
        drawValue(graphics, "age", life.getInt("Age"), x + 10, lineY);
        lineY += 15;
        graphics.drawString(
                font,
                Component.translatable(
                        "screen.immortal_cultivation_mod.life.gender_value",
                        translatedValue("screen.immortal_cultivation_mod.life.gender.", life.getString("Gender"))),
                x + 10,
                lineY,
                0xFF202020,
                false);
        lineY += 15;
        drawTrimmed(
                graphics,
                Component.translatable(
                        "screen.immortal_cultivation_mod.life.birthplace_value",
                        translatedValue("screen.immortal_cultivation_mod.life.birthplace.", life.getString("Birthplace"))),
                x + 10,
                lineY,
                width - 20,
                0xFF202020);
        lineY += 20;
        String[] keys = {"Health", "Mentality", "Comprehension", "Charisma", "Luck"};
        String[] ids = {"health", "mentality", "comprehension", "charisma", "luck"};
        for (int i = 0; i < keys.length; i++) {
            String value = "Luck".equals(keys[i]) && !life.getBoolean("LuckRevealed")
                    ? "???" : Integer.toString(life.getInt(keys[i]));
            graphics.drawString(
                    font,
                    Component.translatable(
                            "screen.immortal_cultivation_mod.life.attribute."
                                    + ids[i]),
                    x + 10,
                    lineY,
                    0xFF303030,
                    false);
            graphics.drawString(font, value, x + 145, lineY, 0xFF202020, false);
            if (!"???".equals(value)) {
                SystemGuiTextures.drawProgressBar(
                        graphics,
                        x + 61,
                        lineY + 1,
                        76,
                        8,
                        life.getInt(keys[i]) / 100.0F);
            }
            lineY += 22;
        }
        graphics.drawString(
                font,
                Component.translatable(
                        "screen.immortal_cultivation_mod.life.actions",
                        life.getInt("AnnualActionPoints")),
                x + 10,
                y + height - 22,
                0xFF17325E,
                false);
    }

    private void renderRelationships(
            GuiGraphics graphics,
            int x,
            int y,
            CompoundTag life,
            int mouseX,
            int mouseY) {
        int listX = x + 18;
        int listY = y + CONTENT_Y;
        int listW = 372;
        SystemGuiTextures.drawInsetPanel(graphics, listX, listY, listW, CONTENT_H);
        ListTag relationships = life.getList("Relationships", Tag.TAG_COMPOUND);
        List<Integer> known = knownRelationshipIndices(life);
        if (!known.isEmpty() && !known.contains(selectedRelationship)) selectedRelationship = known.getFirst();
        int visible = RELATIONSHIP_VISIBLE;
        relationshipScroll = Mth.clamp(relationshipScroll, 0, Math.max(0, known.size() - visible));
        for (int row = 0; row < visible; row++) {
            int visibleIndex = relationshipScroll + row;
            if (visibleIndex >= known.size()) {
                break;
            }
            int index = known.get(visibleIndex);
            CompoundTag person = relationships.getCompound(index);
            int rowY = listY + RELATIONSHIP_ROWS_Y + row * RELATIONSHIP_ROW_HEIGHT;
            boolean hovered = mouseX >= listX + 6 && mouseX < listX + listW - 18
                    && mouseY >= rowY && mouseY < rowY + 35;
            SystemGuiTextures.drawListRow(
                    graphics,
                    listX + 6,
                    rowY,
                    listW - 28,
                    35,
                    index == selectedRelationship,
                    hovered);
            int color = index == selectedRelationship ? 0xFFFFFFFF : 0xFF202020;
            drawRelationshipPortrait(graphics, person, listX + 9, rowY + 1, 32);
            drawTrimmed(graphics, npcName(person), listX + 48, rowY + 5, 96,
                    color, index == selectedRelationship);
            drawTrimmed(
                    graphics,
                    Component.translatable(
                            "screen.immortal_cultivation_mod.life.relationship.row",
                            relationName(person.getString("Relation")),
                            person.getInt("Age"),
                            person.getInt("Affinity")),
                    listX + 150,
                    rowY + 5,
                    listW - 173,
                    color,
                    index == selectedRelationship);
            graphics.drawString(
                    font,
                    Component.translatable(person.getBoolean("Alive")
                            ? "screen.immortal_cultivation_mod.life.alive"
                            : "screen.immortal_cultivation_mod.life.deceased"),
                    listX + 48,
                    rowY + 20,
                    color,
                    index == selectedRelationship);
        }
        SystemGuiTextures.drawVerticalScrollbar(
                graphics,
                listX + listW - 17,
                listY + RELATIONSHIP_ROWS_Y,
                12,
                CONTENT_H - RELATIONSHIP_ROWS_Y - 6,
                relationshipScroll,
                visible,
                known.size());

        int detailX = x + 400;
        int detailW = 202;
        SystemGuiTextures.drawInsetPanel(
                graphics, detailX, listY, detailW, CONTENT_H);
        if (!known.isEmpty()) {
            CompoundTag person = relationships.getCompound(Mth.clamp(
                    selectedRelationship, 0, relationships.size() - 1));
            drawRelationshipPortrait(graphics, person, detailX + 8, listY + 8, 54);
            drawTrimmed(graphics, npcName(person), detailX + 68, listY + 12, detailW - 78, 0xFF17325E, false);
            drawTrimmed(graphics, Component.translatable(person.getBoolean("Alive")
                    ? "screen.immortal_cultivation_mod.life.alive" : "screen.immortal_cultivation_mod.life.deceased"),
                    detailX + 68, listY + 27, detailW - 78, 0xFF404040, false);
            drawTrimmed(graphics, realmName(person.getInt("Realm")), detailX + 68, listY + 42, detailW - 78, 0xFF404040, false);
            List<FormattedCharSequence> detailLines = relationshipDetailLines(person);
            relationshipDetailScroll = Mth.clamp(relationshipDetailScroll, 0, Math.max(0, detailLines.size() - RELATIONSHIP_DETAIL_VISIBLE));
            for (int row = 0; row < Math.min(RELATIONSHIP_DETAIL_VISIBLE, detailLines.size() - relationshipDetailScroll); row++) {
                graphics.drawString(font, detailLines.get(relationshipDetailScroll + row),
                        detailX + 12, listY + 66 + row * 13, 0xFF202020, false);
            }
            SystemGuiTextures.drawVerticalScrollbar(graphics, detailX + detailW - 17,
                    listY + 65, 12, 52, relationshipDetailScroll, RELATIONSHIP_DETAIL_VISIBLE, detailLines.size());
        } else graphics.drawString(font, Component.translatable("life.relationship.filter.empty"),
                listX + 12, listY + RELATIONSHIP_ROWS_Y + 8, 0xFF606060, false);
    }

    private void drawRelationshipPortrait(GuiGraphics graphics, CompoundTag person, int x, int y, int size) {
        var sprite = com.example.immortal_cultivation_mod.system.LifeSimulationSprites.person(person);
        int pixels = sprite.pixels();
        graphics.blit(sprite.texture(), x, y, size, size, 0, 0, pixels, pixels, pixels, pixels);
    }

    private List<FormattedCharSequence> relationshipDetailLines(CompoundTag person) {
        return font.split(Component.translatable(
                "screen.immortal_cultivation_mod.life.relationship.detail",
                translatedValue("screen.immortal_cultivation_mod.life.personality.", person.getString("Personality")),
                translatedValue("screen.immortal_cultivation_mod.life.occupation.", person.getString("Occupation")),
                person.getInt("Health"), person.getInt("Affinity")), 162);
    }

    private void renderCultivation(
            GuiGraphics graphics, int x, int y, CompoundTag life) {
        renderProfile(graphics, x + 18, y + CONTENT_Y, 184, CONTENT_H, life);
        int panelX = x + 210;
        int panelW = 392;
        SystemGuiTextures.drawInsetPanel(
                graphics, panelX, y + CONTENT_Y, panelW, CONTENT_H);
        graphics.drawCenteredString(
                font,
                realmName(life.getInt("Realm")),
                panelX + panelW / 2,
                y + CONTENT_Y + 18,
                0xFF17325E);
        drawLabel(graphics, "screen.immortal_cultivation_mod.life.cultivation_progress",
                panelX + 22, y + CONTENT_Y + 54, 0xFF202020);
        long progress = Math.max(0L, life.getLong("CultivationProgress"));
        long required = Math.max(
                1L, life.getLong("CultivationProgressRequired"));
        SystemGuiTextures.drawProgressBar(
                graphics,
                panelX + 22,
                y + CONTENT_Y + 72,
                panelW - 44,
                18,
                (float) Math.min(1.0D, progress / (double) required));
        graphics.drawCenteredString(
                font,
                String.format(Locale.ROOT, "%,d / %,d", progress, required),
                panelX + panelW / 2,
                y + CONTENT_Y + 77,
                0xFFFFFFFF);
        int detailY = y + CONTENT_Y + 105;
        int leftDetailX = panelX + 18;
        drawDetailLine(graphics, leftDetailX, detailY, "root",
                translatedValue("screen.immortal_cultivation_mod.life.root.", life.getString("SpiritualRoot")));
        drawDetailLine(graphics, leftDetailX, detailY + 18, "stability",
                Component.literal(Integer.toString(life.getInt("CultivationStability"))));
        drawDetailLine(graphics, leftDetailX, detailY + 36, "combat",
                Component.literal(Integer.toString(life.getInt("Combat"))));
        drawDetailLine(graphics, leftDetailX, detailY + 54, "lifespan",
                Component.literal(life.getInt("Age") + " / "
                        + life.getInt("MaximumLifespan")));
        drawDetailLine(graphics, leftDetailX, detailY + 72, "realm_hp",
                Component.literal(String.format(
                        Locale.ROOT, "%,d", life.getInt("RealmMaxHp"))));
        drawDetailLine(graphics, leftDetailX, detailY + 90, "realm_qi",
                Component.literal(String.format(
                        Locale.ROOT, "%,d", life.getInt("RealmMaxQi"))));

        CultivationMethods.MethodDef method = CultivationMethods.get(
                life.getString("ActiveMethod"));
        Component methodName = method == null
                ? Component.translatable(
                        "screen.immortal_cultivation_mod.life.none")
                : Component.translatable(method.nameKey());
        drawTrimmed(
                graphics,
                Component.translatable(
                        "screen.immortal_cultivation_mod.life.detail.method",
                        methodName),
                leftDetailX,
                detailY + 108,
                172,
                0xFF202020);

        int spellX = panelX + 209;
        graphics.drawString(
                font,
                Component.translatable(
                        "screen.immortal_cultivation_mod.life.learned_spells"),
                spellX,
                detailY,
                0xFF17325E,
                false);
        ListTag spells = life.getList("DiscoveredSpells", Tag.TAG_STRING);
        cultivationScroll = Mth.clamp(
                cultivationScroll, 0, Math.max(0, spells.size() - 4));
        for (int row = 0; row < 4; row++) {
            int index = cultivationScroll + row;
            if (index >= spells.size()) {
                break;
            }
            ModSpells.SpellDef spell = ModSpells.get(spells.getString(index));
            Component name = spell == null
                    ? Component.translatable("life.expansion.unknown_spell")
                    : spell.name();
            drawTrimmed(
                    graphics,
                    name,
                    spellX + 6,
                    detailY + 18 + row * 15,
                    145,
                    0xFF202020);
        }
        SystemGuiTextures.drawVerticalScrollbar(
                graphics,
                panelX + panelW - 17,
                detailY + 15,
                12,
                65,
                cultivationScroll,
                4,
                spells.size());

        graphics.drawString(
                font,
                Component.translatable(
                        "screen.immortal_cultivation_mod.life.learned_methods"),
                spellX,
                detailY + 82,
                0xFF17325E,
                false);
        ListTag methods = life.getList("LearnedMethods", Tag.TAG_STRING);
        methodScroll = Mth.clamp(methodScroll, 0, Math.max(0, methods.size() - 3));
        for (int row = 0; row < Math.min(3, methods.size() - methodScroll); row++) {
            String methodId = methods.getString(methodScroll + row);
            CultivationMethods.MethodDef learned = CultivationMethods.get(methodId);
            Component name = learned == null
                    ? Component.translatable("life.expansion.unknown_method")
                    : Component.translatable(learned.nameKey());
            drawTrimmed(
                    graphics,
                    name,
                    spellX + 6,
                    detailY + 100 + row * 15,
                    145,
                    0xFF202020);
        }
        SystemGuiTextures.drawVerticalScrollbar(graphics, panelX + panelW - 17,
                detailY + 97, 12, 45, methodScroll, 3, methods.size());
    }

    private void renderActivities(
            GuiGraphics graphics, int x, int y, CompoundTag life) {
        SystemGuiTextures.drawInsetPanel(
                graphics, x + 18, y + CONTENT_Y, WINDOW_W - 36, CONTENT_H);
        graphics.drawCenteredString(
                font,
                Component.translatable(
                        "screen.immortal_cultivation_mod.life.activities_header",
                        life.getInt("AnnualActionPoints")),
                x + WINDOW_W / 2,
                y + CONTENT_Y + 15,
                0xFF17325E);
        drawCenteredWrapped(
                graphics,
                Component.translatable(
                        "screen.immortal_cultivation_mod.life.activities_help"),
                x + WINDOW_W / 2,
                y + 295,
                520,
                0xFF4A4A4A);
    }

    private void renderAssets(
            GuiGraphics graphics, int x, int y, CompoundTag life) {
        int panelY = y + CONTENT_Y;
        SystemGuiTextures.drawInsetPanel(graphics, x + 18, panelY, 350, CONTENT_H);
        SystemGuiTextures.drawInsetPanel(graphics, x + 378, panelY, 224, CONTENT_H);
        int lineY = panelY + 12;
        drawDetailLine(graphics, x + 35, lineY, "wealth",
                Component.literal(String.format(Locale.ROOT, "%,d", life.getLong("Wealth"))));
        drawDetailLine(graphics, x + 35, lineY + 17, "occupation",
                translatedValue("screen.immortal_cultivation_mod.life.occupation.", life.getString("Occupation")));
        drawDetailLine(graphics, x + 35, lineY + 34, "sect",
                translatedValue("screen.immortal_cultivation_mod.life.sect.", life.getString("Sect")));
        drawDetailLine(graphics, x + 35, lineY + 51, "morality",
                Component.literal(Integer.toString(life.getInt("Morality"))));
        drawDetailLine(graphics, x + 35, lineY + 68, "reputation",
                Component.literal(Integer.toString(life.getInt("Reputation"))));
        drawDetailLine(graphics, x + 35, lineY + 85, "medical",
                Component.literal(Integer.toString(life.getInt("Medical"))));
        drawDetailLine(graphics, x + 35, lineY + 102, "alchemy",
                Component.literal(Integer.toString(life.getInt("Alchemy"))));
        drawDetailLine(graphics, x + 35, lineY + 119, "family",
                translatedValue("screen.immortal_cultivation_mod.life.family.", life.getString("Family")));

        ListTag inventory = life.getList("Inventory", Tag.TAG_COMPOUND);
        graphics.drawString(
                font,
                Component.translatable(
                        "screen.immortal_cultivation_mod.life.carried_items"),
                x + 35,
                lineY + 143,
                0xFF17325E,
                false);
        assetScroll = Mth.clamp(
                assetScroll, 0, Math.max(0, inventory.size() - 6));
        for (int row = 0; row < 6; row++) {
            int index = assetScroll + row;
            if (index >= inventory.size()) {
                break;
            }
            CompoundTag entry = inventory.getCompound(index);
            ResourceLocation id = ResourceLocation.tryParse(entry.getString("Id"));
            Item item = id == null ? Items.AIR : BuiltInRegistries.ITEM.get(id);
            Component itemName = item == Items.AIR
                    ? Component.translatable("life.expansion.unknown_item")
                    : Component.translatable(item.getDescriptionId());
            int itemY = lineY + 160 + row * 18;
            if (item != Items.AIR) {
                graphics.renderItem(new ItemStack(item), x + 36, itemY - 4);
            }
            drawTrimmed(
                    graphics,
                    itemName.copy().append(" x" + entry.getInt("Count")),
                    x + 56,
                    itemY,
                    278,
                    0xFF202020);
        }
        SystemGuiTextures.drawVerticalScrollbar(
                graphics,
                x + 346,
                lineY + 156,
                12,
                104,
                assetScroll,
                6,
                inventory.size());

        graphics.drawCenteredString(
                font,
                Component.translatable(
                        "screen.immortal_cultivation_mod.life.insight_shop",
                        life.getInt("Insight")),
                x + 490,
                panelY + 14,
                0xFF17325E);
        String[] upgrades = {
                "danger_sense", "previous_luck", "family_blessing",
                "continuous_memory", "inheritance_expansion",
                "fate_not_end", "fate_retention"
        };
        for (int i = 0; i < upgrades.length; i++) {
            drawTrimmed(
                    graphics,
                    Component.translatable(
                            "screen.immortal_cultivation_mod.life.upgrade."
                                    + upgrades[i]),
                    x + 394,
                    y + 99 + i * 34,
                    188,
                    hasUpgrade(life, upgrades[i]) ? 0xFF1F6A2D : 0xFF202020);
        }
    }

    private void renderRecord(
            GuiGraphics graphics, int x, int y, CompoundTag life) {
        int panelX = x + 18;
        int panelY = y + CONTENT_Y;
        SystemGuiTextures.drawInsetPanel(
                graphics, panelX, panelY, WINDOW_W - 36, CONTENT_H);
        CompoundTag memory = memoryLife(life);
        graphics.drawCenteredString(font, Component.translatable(
                "screen.immortal_cultivation_mod.life.memory_title", memory.getString("Name"),
                selectedMemory + 1, life.getList("Memories", Tag.TAG_COMPOUND).size() + 1),
                x + WINDOW_W / 2, panelY + 14, 0xFF17325E);
        Component summary = Component.translatable(
                "screen.immortal_cultivation_mod.life.memory_summary", memory.getInt("Age"),
                realmName(memory.getInt("HighestRealm")));
        if (!memory.getString("Ending").isBlank()) {
            summary = summary.copy().append(" · ").append(
                    translatedValue("screen.immortal_cultivation_mod.life.ending.", memory.getString("Ending")));
        }
        drawTrimmed(graphics, summary, panelX + 15, panelY + 32, WINDOW_W - 82, 0xFF202020);
        Component receipt = memory.contains("InheritanceReceipt", Tag.TAG_COMPOUND)
                ? Component.translatable("screen.immortal_cultivation_mod.life.memory_inheritance",
                        inheritanceDetail(memory.getCompound("InheritanceReceipt")))
                : Component.translatable(memory.getBoolean("RewardClaimed")
                        ? "screen.immortal_cultivation_mod.life.inherit.detail.unknown"
                        : "screen.immortal_cultivation_mod.life.memory_unclaimed");
        drawTrimmed(graphics, receipt, panelX + 15, panelY + 46, WINDOW_W - 82, 0xFF3B3B3B);
        List<FormattedCharSequence> lines = memoryLines(life);
        int start = Mth.clamp(historyScroll, 0, Math.max(0, lines.size() - HISTORY_VISIBLE_LINES));
        for (int i = start; i < Math.min(lines.size(), start + HISTORY_VISIBLE_LINES); i++) {
            graphics.drawString(font, lines.get(i), panelX + 15,
                    panelY + 64 + (i - start) * 14, 0xFF202020, false);
        }
        SystemGuiTextures.drawVerticalScrollbar(graphics, panelX + WINDOW_W - 57,
                panelY + 60, 12, CONTENT_H - 68, historyScroll, HISTORY_VISIBLE_LINES, lines.size());
        drawTrimmed(graphics, Component.translatable("screen.immortal_cultivation_mod.life.memory_hint"),
                x + 33, y + 327, WINDOW_W - 82, 0xFF3B3B3B);
    }

    private void renderHistory(
            GuiGraphics graphics, CompoundTag life, int x, int y, int width, int visible) {
        List<FormattedCharSequence> lines = historyLines(life, width);
        int maximum = Math.max(0, lines.size() - visible);
        diaryScroll = Mth.clamp(diaryScroll, 0, maximum);
        int start = Math.max(0, lines.size() - visible - diaryScroll);
        for (int i = start; i < Math.min(lines.size(), start + visible); i++) {
            graphics.drawString(font, lines.get(i), x, y + (i - start) * 14, 0xFF202020, false);
        }
        SystemGuiTextures.drawVerticalScrollbar(graphics, x + width + 4, y, 12,
                visible * 14, start, visible, lines.size());
    }

    private void renderFinished(
            GuiGraphics graphics, int x, int y, CompoundTag life) {
        if (!inheritancePicker.isEmpty() && !life.getBoolean("RewardClaimed") && !hasPendingInheritance(life)) {
            renderInheritancePicker(graphics, x, y, life);
            return;
        }
        int panelX = x + 54;
        int panelY = y + 76;
        int panelW = WINDOW_W - 108;
        SystemGuiTextures.drawInsetPanel(graphics, panelX, panelY, panelW, 210);
        graphics.drawCenteredString(
                font,
                Component.translatable(
                        "screen.immortal_cultivation_mod.life.ending_title"),
                x + WINDOW_W / 2,
                panelY + 15,
                0xFF17325E);
        graphics.drawCenteredString(
                font,
                translatedValue("screen.immortal_cultivation_mod.life.ending.", life.getString("Ending")),
                x + WINDOW_W / 2,
                panelY + 39,
                0xFF7A201D);
        int leftX = panelX + 16;
        int valueY = panelY + 69;
        drawSummary(graphics, leftX, valueY, "age", life.getInt("Age"));
        drawSummaryComponent(graphics, leftX, valueY + 22, "highest_realm",
                realmName(life.getInt("HighestRealm")));
        drawSummary(graphics, leftX, valueY + 44, "wealth", life.getLong("Wealth"));
        drawSummary(graphics, leftX, valueY + 66, "score", life.getLong("Score"));
        drawSummary(graphics, leftX, valueY + 88, "insight_earned",
                life.getInt("InsightEarned"));
        drawTrimmed(graphics, Component.translatable("life.expansion.score", life.getLong("ScoreRealm"), life.getLong("ScoreStories"),
                life.getLong("ScoreLegacy"), life.getLong("ScoreReputation")), panelX + 16, panelY + 176, panelW - 32, 0xFF17325E);
        ListTag rewards = life.getList("InheritanceRewards", Tag.TAG_COMPOUND);
        CompoundTag pending = life.getCompound("PendingInheritance");
        boolean reserved = hasPendingInheritance(life);
        int detailX = panelX + 244;
        for (int i = 0; i < rewards.size(); i++) {
            CompoundTag reward = rewards.getCompound(i);
            boolean chosen = reserved && reward.getString("Id").equals(pending.getString("RewardId"));
            CompoundTag preview = chosen ? pending.getCompound("Receipt") : reward.getCompound("Preview");
            drawTrimmed(graphics, rewardLabel(chosen ? pending.getCompound("Selected") : reward),
                    detailX, panelY + 65 + i * 28, 250, chosen ? 0xFF1F6A2D : 0xFF17325E);
            drawTrimmed(graphics, inheritanceDetail(preview),
                    detailX, panelY + 76 + i * 28, 250, 0xFF202020);
        }
        if (!reserved && life.contains("InheritanceReceipt", Tag.TAG_COMPOUND)) {
            drawTrimmed(graphics, Component.translatable(
                    "screen.immortal_cultivation_mod.life.memory_inheritance",
                    inheritanceDetail(life.getCompound("InheritanceReceipt"))),
                    panelX + 16, y + 334, panelW - 32, 0xFF1F6A2D);
        }
        Component claimInstruction = reserved
                ? Component.translatable("life.expansion.inherit.pending").append(": ").append(rewardLabel(pending.getCompound("Selected")))
                : Component.translatable(life.getBoolean("RewardClaimed")
                        ? "screen.immortal_cultivation_mod.life.reward_claimed"
                        : rewards.stream().allMatch(t -> "none".equals(((CompoundTag)t).getCompound("Preview").getString("Kind")))
                                ? "life.expansion.inherit.empty_instruction"
                                : "screen.immortal_cultivation_mod.life.choose_inheritance");
        graphics.drawCenteredString(
                font,
                claimInstruction,
                x + WINDOW_W / 2,
                panelY + 197,
                0xFF3B3B3B);
        if (reserved) {
            List<FormattedCharSequence> spaceLines = font.split(Component.translatable("life.expansion.inherit.space"), WINDOW_W - 56);
            for (int row = 0; row < Math.min(2, spaceLines.size()); row++) {
                graphics.drawCenteredString(font, spaceLines.get(row), x + WINDOW_W / 2, y + 323 + row * 12, 0xFF7A201D);
            }
        }
    }

    private void drawSummary(
            GuiGraphics graphics, int x, int y, String key, long value) {
        drawSummaryComponent(
                graphics,
                x,
                y,
                key,
                Component.literal(String.format(Locale.ROOT, "%,d", value)));
    }

    private void drawSummaryComponent(
            GuiGraphics graphics,
            int x,
            int y,
            String key,
            Component value) {
        // Reserve the reward column; long translated realm names fit in two lines.
        List<FormattedCharSequence> lines = font.split(Component.translatable(
                "screen.immortal_cultivation_mod.life.summary." + key, value), 214);
        for (int row = 0; row < Math.min(2, lines.size()); row++) {
            graphics.drawString(font, lines.get(row), x, y + row * 11, 0xFF202020, false);
        }
    }

    private void drawValue(
            GuiGraphics graphics, String key, int value, int x, int y) {
        graphics.drawString(
                font,
                Component.translatable(
                        "screen.immortal_cultivation_mod.life." + key,
                        value),
                x,
                y,
                0xFF202020,
                false);
    }

    private void drawDetailLine(
            GuiGraphics graphics,
            int x,
            int y,
            String key,
            Component value) {
        graphics.drawString(
                font,
                Component.translatable(
                        "screen.immortal_cultivation_mod.life.detail." + key,
                        value),
                x,
                y,
                0xFF202020,
                false);
    }

    private void drawLabel(
            GuiGraphics graphics, String key, int x, int y, int color) {
        graphics.drawString(
                font, Component.translatable(key), x, y, color, false);
    }

    private void drawWrapped(
            GuiGraphics graphics,
            Component text,
            int x,
            int y,
            int width,
            int color,
            int maximumLines) {
        List<FormattedCharSequence> lines = font.split(text, width);
        for (int i = 0; i < Math.min(maximumLines, lines.size()); i++) {
            graphics.drawString(font, lines.get(i), x, y + i * 13, color, false);
        }
    }

    private void drawCenteredWrapped(
            GuiGraphics graphics,
            Component text,
            int centerX,
            int y,
            int width,
            int color) {
        List<FormattedCharSequence> lines = font.split(text, width);
        for (int i = 0; i < lines.size(); i++) {
            graphics.drawCenteredString(font, lines.get(i), centerX, y + i * 14, color);
        }
    }

    private void drawTrimmed(
            GuiGraphics graphics,
            Component text,
            int x,
            int y,
            int width,
            int color) {
        drawTrimmed(graphics, text, x, y, width, color, false);
    }

    private void drawTrimmed(
            GuiGraphics graphics,
            Component text,
            int x,
            int y,
            int width,
            int color,
            boolean shadow) {
        List<FormattedCharSequence> lines = font.split(text, width);
        if (!lines.isEmpty()) {
            graphics.drawString(font, lines.getFirst(), x, y, color, shadow);
        }
    }

    private Component relationName(String relation) {
        String key = "screen.immortal_cultivation_mod.life.relation." + relation;
        if (I18n.exists(key)) {
            return Component.translatable(key);
        }
        return Component.translatable("screen.immortal_cultivation_mod.life.relation.unknown");
    }

    private Component translatedValue(String prefix, String id) {
        String key = prefix + id;
        return Component.translatable(I18n.exists(key) ? key : "screen.immortal_cultivation_mod.life.none");
    }

    private Component realmName(int realm) {
        if (realm <= 0) {
            return Component.translatable(
                    "screen.immortal_cultivation_mod.life.realm.mortal");
        }
        int major = Math.min(9, (realm - 1) / 3 + 1);
        int stage = Math.floorMod(realm - 1, 3);
        return Component.translatable(
                        "screen.immortal_cultivation_mod.life.realm." + major)
                .append(" ")
                .append(Component.translatable(
                        "screen.immortal_cultivation_mod.life.stage." + stage));
    }

    private CompoundTag life() {
        return state.getCompound("LifeSimulation");
    }

    private boolean hasGeneratedLife(CompoundTag life) {
        return life.contains("Name", Tag.TAG_STRING);
    }

    private boolean hasUpgrade(CompoundTag life, String id) {
        ListTag upgrades = life.getList("Upgrades", Tag.TAG_STRING);
        for (int i = 0; i < upgrades.size(); i++) {
            if (id.equals(upgrades.getString(i))) {
                return true;
            }
        }
        return false;
    }

    private boolean canAct(CompoundTag life) {
        return life.getBoolean("Active")
                && life.getBoolean("Alive")
                && !life.contains("CurrentEvent", Tag.TAG_COMPOUND)
                && !life.contains("Story", Tag.TAG_COMPOUND)
                && !life.contains("Battle", Tag.TAG_COMPOUND)
                && life.getInt("AnnualActionPoints") > 0;
    }

    private boolean canAdvance(CompoundTag life) {
        return life.getBoolean("Active") && life.getBoolean("Alive")
                && !life.getBoolean("Finished")
                && !life.contains("CurrentEvent", Tag.TAG_COMPOUND)
                && !life.contains("Story", Tag.TAG_COMPOUND)
                && !life.contains("Battle", Tag.TAG_COMPOUND);
    }

    private boolean activityAvailable(CompoundTag life, String activity) {
        return LifeSimulationSystem.activityUnavailable(life,activity).isBlank();
    }

    private Component npcName(CompoundTag person) {
        var name = person.getString("NameKey").isBlank() || !I18n.exists(person.getString("NameKey"))
                ? Component.literal(person.getString("Name")) : Component.translatable(person.getString("NameKey"));
        return person.getBoolean("Alive") ? name : name.copy().withStyle(style -> style.withColor(0xB43434));
    }

    private List<Integer> knownRelationshipIndices(CompoundTag life) {
        List<Integer> indices = new ArrayList<>();
        ListTag people = life.getList("Relationships", Tag.TAG_COMPOUND);
        for (int index = 0; index < people.size(); index++) {
            CompoundTag person = people.getCompound(index);
            if ((!person.contains("Observed") || person.getBoolean("Observed")) && relationshipFilter.matches(person)) indices.add(index);
        }
        return indices;
    }

    private void clampScrolls() {
        CompoundTag life = life();
        int historyLines = font == null ? memoryLife(life).getList("History", Tag.TAG_COMPOUND).size()
                : memoryLines(life).size();
        historyScroll = Mth.clamp(historyScroll, 0, Math.max(0, historyLines - HISTORY_VISIBLE_LINES));
        ListTag relationships = life.getList("Relationships", Tag.TAG_COMPOUND);
        List<Integer> known = knownRelationshipIndices(life);
        relationshipScroll = Mth.clamp(
                relationshipScroll, 0, Math.max(0, known.size() - RELATIONSHIP_VISIBLE));
        selectedRelationship = Mth.clamp(
                selectedRelationship, 0, Math.max(0, relationships.size() - 1));
        if (!known.isEmpty() && !known.contains(selectedRelationship)) selectedRelationship = known.getFirst();
    }

    private float uiScale() {
        float fit = Math.min(
                Math.max(1.0F, width - MARGIN) / WINDOW_W,
                Math.max(1.0F, height - MARGIN) / WINDOW_H);
        return Math.max(0.1F, Math.min(DEFAULT_SCALE, fit));
    }

    private SystemWindowState.ContentLayout contentLayout(
            SystemWindowState window) {
        return window.contentLayout(WINDOW_W, WINDOW_H);
    }

    private void enableWindowScissor(
            GuiGraphics graphics, SystemWindowState window) {
        float scale = uiScale();
        graphics.enableScissor(
                (int) Math.floor(window.x() * scale),
                (int) Math.floor(window.y() * scale),
                (int) Math.ceil((window.x() + window.width()) * scale),
                (int) Math.ceil(
                        (window.y() + window.renderedHeight()) * scale));
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

    private void drawMainTitleDetails(
            GuiGraphics graphics,
            CompoundTag life,
            int x,
            int y) {
        Component title = Component.translatable(
                "system.immortal_cultivation_mod.life_simulation");
        Component attempts = Component.translatable(
                "screen.immortal_cultivation_mod.life.attempts",
                life.getInt("Attempts"),
                3);
        Component insight = Component.translatable(
                "screen.immortal_cultivation_mod.life.insight",
                life.getInt("Insight"));
        int right = SystemGuiTextures.titleTextRight(
                x, windowState.width()) - 4;
        int titleWidth = Math.min(
                font.width(title), Math.max(0, right - (x + 13)));
        int titleRight = x + 13 + titleWidth + 10;
        int insightX = right - font.width(insight);
        int attemptsX = insightX - 12 - font.width(attempts);
        if (attemptsX >= titleRight) {
            graphics.drawString(
                    font, attempts, attemptsX, y + 14, 0xFFFFFFFF, true);
            graphics.drawString(
                    font, insight, insightX, y + 14, 0xFFFFFFFF, true);
            return;
        }
        int compactX = right - font.width(attempts);
        if (compactX >= titleRight) {
            graphics.drawString(
                    font, attempts, compactX, y + 14, 0xFFFFFFFF, true);
        }
    }

    private int desktopWidth() {
        return Math.max(WINDOW_W, (int) Math.floor(width / uiScale()));
    }

    private int desktopHeight() {
        return Math.max(WINDOW_H, (int) Math.floor(height / uiScale()));
    }

    private double uiMouseX(double mouseX) {
        return mouseX / uiScale();
    }

    private double uiMouseY(double mouseY) {
        return mouseY / uiScale();
    }

    private int left() {
        return windowState.x();
    }

    private int top() {
        return windowState.y();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        mouseX = uiMouseX(mouseX);
        mouseY = uiMouseY(mouseY);
        WindowLayer clickedWindow = windowAt(mouseX, mouseY);
        if (clickedWindow == null) {
            return false;
        }
        if (clickedWindow == WindowLayer.DETACHED) {
            detachedOnTop = true;
            return mouseClickedDetached(mouseX, mouseY, button);
        }
        if (detachedWindow != null) {
            detachedOnTop = false;
        }
        return mouseClickedMain(mouseX, mouseY, button);
    }

    private boolean mouseClickedMain(
            double mouseX, double mouseY, int button) {
        if (button == 0 && SystemGuiTextures.isOverClose(
                mouseX, mouseY, left(), top(), windowState.width())) {
            onClose();
            return true;
        }
        if (button == 0 && SystemGuiTextures.isOverMinimize(
                mouseX, mouseY, left(), top(), windowState.width())) {
            windowState.toggleMinimize();
            rebuildWidgets();
            return true;
        }
        if (button == 0 && SystemGuiTextures.isOverMaximize(
                mouseX, mouseY, left(), top(), windowState.width())) {
            windowState.toggleMaximize(desktopWidth(), desktopHeight());
            rebuildWidgets();
            return true;
        }
        if (windowState.beginDrag(
                mouseX, mouseY, button, desktopWidth(), desktopHeight())) {
            return true;
        }
        if (windowState.isMinimized()) {
            return false;
        }
        if (button == 0) {
            Tab clickedTab = tabAt(mouseX, mouseY);
            if (clickedTab != null) {
                long now = System.currentTimeMillis();
                if (clickedTab == lastClickedTab
                        && now - lastTabClickMillis <= 350L) {
                    toggleDetached(clickedTab);
                    lastClickedTab = null;
                    lastTabClickMillis = 0L;
                    return true;
                }
                lastClickedTab = clickedTab;
                lastTabClickMillis = now;
            }
        }
        if (clickButtonLayer(
                WindowLayer.MAIN, mouseX, mouseY, button)) {
            return true;
        }
        if (button == 0
                && selectedTab == Tab.RELATIONSHIPS
                && selectRelationshipAt(mouseX, mouseY, windowState)) {
            return true;
        }
        return false;
    }

    private boolean mouseClickedDetached(
            double mouseX, double mouseY, int button) {
        if (detachedWindow == null || detachedTab == null) {
            return false;
        }
        if (button == 0 && SystemGuiTextures.isOverClose(
                mouseX,
                mouseY,
                detachedWindow.x(),
                detachedWindow.y(),
                detachedWindow.width())) {
            redockTab();
            return true;
        }
        if (button == 0 && SystemGuiTextures.isOverMinimize(
                mouseX,
                mouseY,
                detachedWindow.x(),
                detachedWindow.y(),
                detachedWindow.width())) {
            detachedWindow.toggleMinimize();
            rebuildWidgets();
            return true;
        }
        if (button == 0 && SystemGuiTextures.isOverMaximize(
                mouseX,
                mouseY,
                detachedWindow.x(),
                detachedWindow.y(),
                detachedWindow.width())) {
            detachedWindow.toggleMaximize(desktopWidth(), desktopHeight());
            rebuildWidgets();
            return true;
        }
        if (detachedWindow.beginDrag(
                mouseX,
                mouseY,
                button,
                desktopWidth(),
                desktopHeight())) {
            return true;
        }
        if (detachedWindow.isMinimized()) {
            return false;
        }
        if (clickButtonLayer(
                WindowLayer.DETACHED, mouseX, mouseY, button)) {
            return true;
        }
        return button == 0
                && detachedTab == Tab.RELATIONSHIPS
                && selectRelationshipAt(
                        mouseX,
                        mouseY,
                        detachedWindow);
    }

    private boolean clickButtonLayer(
            WindowLayer layer,
            double mouseX,
            double mouseY,
            int button) {
        setButtonLayerVisibility(layer);
        try {
            return super.mouseClicked(mouseX, mouseY, button);
        } finally {
            restoreButtonVisibility();
        }
    }

    private WindowLayer windowAt(double mouseX, double mouseY) {
        boolean overMain = windowState.contains(mouseX, mouseY);
        boolean overDetached = hasVisibleDetachedWindow()
                && detachedWindow.contains(mouseX, mouseY);
        if (detachedOnTop) {
            if (overDetached) {
                return WindowLayer.DETACHED;
            }
            return overMain ? WindowLayer.MAIN : null;
        }
        if (overMain) {
            return WindowLayer.MAIN;
        }
        return overDetached ? WindowLayer.DETACHED : null;
    }

    private boolean hasVisibleDetachedWindow() {
        CompoundTag life = life();
        return detachedWindow != null
                && detachedTab != null
                && hasGeneratedLife(life)
                && life.getBoolean("Active")
                && !life.getBoolean("Finished");
    }

    private Tab tabAt(double mouseX, double mouseY) {
        SystemWindowState.ContentLayout layout = contentLayout(windowState);
        mouseX = layout.virtualX(mouseX);
        mouseY = layout.virtualY(mouseY);
        int firstX = left() + 19;
        int tabY = top() + 42;
        if (mouseY < tabY || mouseY >= tabY + 19) {
            return null;
        }
        for (int i = 0; i < Tab.values().length; i++) {
            int tabX = firstX + i * TAB_STEP;
            if (mouseX >= tabX && mouseX < tabX + TAB_WIDTH) {
                return Tab.values()[i];
            }
        }
        return null;
    }

    private void toggleDetached(Tab tab) {
        if (detachedTab == tab) {
            redockTab();
            return;
        }
        detachedTab = tab;
        detachedWindow = new SystemWindowState(WINDOW_W, WINDOW_H);
        detachedOnTop = true;
        detachedWindow.placeNextTo(
                windowState, desktopWidth(), desktopHeight());
        if (selectedTab == tab) {
            for (Tab candidate : Tab.values()) {
                if (candidate != tab) {
                    selectedTab = candidate;
                    break;
                }
            }
        }
        rebuildWidgets();
    }

    private void redockTab() {
        if (detachedTab != null) {
            selectedTab = detachedTab;
        }
        detachedTab = null;
        detachedWindow = null;
        rebuildWidgets();
    }

    private boolean selectRelationshipAt(
            double mouseX,
            double mouseY,
            SystemWindowState window) {
        SystemWindowState.ContentLayout layout = contentLayout(window);
        mouseX = layout.virtualX(mouseX);
        mouseY = layout.virtualY(mouseY);
        int x = window.x();
        int y = window.y();
        int listX = x + 24;
        int listY = y + CONTENT_Y + RELATIONSHIP_ROWS_Y;
        if (mouseX < listX || mouseX >= listX + 344
                || mouseY < listY || mouseY >= listY + RELATIONSHIP_VISIBLE * RELATIONSHIP_ROW_HEIGHT) {
            return false;
        }
        int row = (int) ((mouseY - listY) / RELATIONSHIP_ROW_HEIGHT);
        if ((mouseY - listY) % RELATIONSHIP_ROW_HEIGHT >= 35) return false;
        int visibleIndex = relationshipScroll + row;
        List<Integer> known = knownRelationshipIndices(life());
        if (visibleIndex >= known.size()) {
            return false;
        }
        selectedRelationship = known.get(visibleIndex);
        relationshipDetailScroll = 0;
        rebuildWidgets();
        return true;
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double scrollX,
            double scrollY) {
        mouseX = uiMouseX(mouseX);
        mouseY = uiMouseY(mouseY);
        WindowLayer scrollWindow = windowAt(mouseX, mouseY);
        if (scrollWindow == null) {
            return false;
        }
        Tab scrollTab = scrollWindow == WindowLayer.DETACHED
                ? detachedTab
                : selectedTab;
        SystemWindowState scrollOwner = scrollWindow == WindowLayer.DETACHED ? detachedWindow : windowState;
        SystemWindowState.ContentLayout scrollLayout = contentLayout(scrollOwner);
        double contentX = scrollLayout.virtualX(mouseX) - scrollOwner.x();
        double contentY = scrollLayout.virtualY(mouseY) - scrollOwner.y();
        if (scrollTab == Tab.LIFE && !inheritancePicker.isEmpty()) {
            int count = pickerReward(life()).getList("Choices", Tag.TAG_COMPOUND).size();
            if (!inheritanceWaiting) {
                inheritancePage = Mth.clamp(inheritancePage - (int) Math.signum(scrollY), 0,
                        Math.max(0, (count - 1) / INHERITANCE_PAGE_SIZE));
                rebuildWidgets();
            }
            return true;
        }
        if (scrollTab == Tab.LIFE && !life().getBoolean("Finished")
                && life().getCompound("CurrentEvent").isEmpty()) {
            int maximum = Math.max(0, historyLines(life(), 352).size() - 16);
            diaryScroll = Mth.clamp(diaryScroll + (int) Math.signum(scrollY), 0, maximum);
            return true;
        }
        if (scrollTab == Tab.RECORD) {
            int maximum = Math.max(0,
                    memoryLines(life()).size() - HISTORY_VISIBLE_LINES);
            historyScroll = Mth.clamp(
                    historyScroll - (int) Math.signum(scrollY), 0, maximum);
            return true;
        }
        if (scrollTab == Tab.RELATIONSHIPS) {
            ListTag people = life().getList("Relationships", Tag.TAG_COMPOUND);
            if (contentX >= 400 && contentY >= CONTENT_Y + 65 && contentY < 184 && !people.isEmpty()) {
                int maximum = Math.max(0, relationshipDetailLines(people.getCompound(
                        Mth.clamp(selectedRelationship, 0, people.size() - 1))).size() - RELATIONSHIP_DETAIL_VISIBLE);
                relationshipDetailScroll = Mth.clamp(relationshipDetailScroll - (int) Math.signum(scrollY), 0, maximum);
                return true;
            }
            int maximum = Math.max(0,
                    knownRelationshipIndices(life()).size() - RELATIONSHIP_VISIBLE);
            relationshipScroll = Mth.clamp(
                    relationshipScroll - (int) Math.signum(scrollY), 0, maximum);
            return true;
        }
        if (scrollTab == Tab.CULTIVATION) {
            if (contentX >= 419 && contentY >= 255) {
                int maximum = Math.max(0, life().getList("LearnedMethods", Tag.TAG_STRING).size() - 3);
                methodScroll = Mth.clamp(methodScroll - (int) Math.signum(scrollY), 0, maximum);
                return true;
            }
            int maximum = Math.max(0,
                    life().getList("DiscoveredSpells", Tag.TAG_STRING).size() - 4);
            cultivationScroll = Mth.clamp(
                    cultivationScroll - (int) Math.signum(scrollY), 0, maximum);
            return true;
        }
        if (scrollTab == Tab.ASSETS) {
            int maximum = Math.max(0,
                    life().getList("Inventory", Tag.TAG_COMPOUND).size() - 6);
            assetScroll = Mth.clamp(
                    assetScroll - (int) Math.signum(scrollY), 0, maximum);
            return true;
        }
        setButtonLayerVisibility(scrollWindow);
        try {
            return super.mouseScrolled(
                    mouseX, mouseY, scrollX, scrollY);
        } finally {
            restoreButtonVisibility();
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (detachedWindow != null && detachedWindow.isDragging()) {
            detachedWindow.endDrag();
            return true;
        }
        if (windowState.isDragging()) {
            windowState.endDrag();
            return true;
        }
        return super.mouseReleased(uiMouseX(mouseX), uiMouseY(mouseY), button);
    }

    @Override
    public boolean mouseDragged(
            double mouseX,
            double mouseY,
            int button,
            double dragX,
            double dragY) {
        double scaledMouseX = uiMouseX(mouseX);
        double scaledMouseY = uiMouseY(mouseY);
        if (detachedWindow != null
                && button == 0
                && detachedWindow.dragTo(
                        scaledMouseX,
                        scaledMouseY,
                        desktopWidth(),
                        desktopHeight())) {
            rebuildWidgets();
            return true;
        }
        if (button == 0 && windowState.dragTo(
                scaledMouseX,
                scaledMouseY,
                desktopWidth(),
                desktopHeight())) {
            rebuildWidgets();
            return true;
        }
        float scale = uiScale();
        return super.mouseDragged(
                scaledMouseX, scaledMouseY, button,
                dragX / scale, dragY / scale);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(uiMouseX(mouseX), uiMouseY(mouseY));
    }

    @Override
    public void removed() {
        if (!com.example.immortal_cultivation_mod.client.OuterSystemClient.isTransitioning()
                && minecraft != null && minecraft.player != null) {
            PacketDistributor.sendToServer(
                    new OuterSystemPayloads.ServerboundClose());
        }
        super.removed();
    }

    @Override
    public void renderBackground(
            GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private enum WindowLayer {
        MAIN,
        DETACHED
    }

    private enum Tab {
        LIFE("life"),
        RELATIONSHIPS("relationships"),
        CULTIVATION("cultivation"),
        ACTIVITIES("activities"),
        MAP("map"),
        ASSETS("assets"),
        RECORD("record");

        private final String id;

        Tab(String id) {
            this.id = id;
        }

        private Component label() {
            return Component.translatable(
                    "screen.immortal_cultivation_mod.life.tab." + id);
        }
    }

    private enum MapLayer {
        TERRAIN("terrain"),
        FACTION("faction"),
        QI("qi"),
        DANGER("danger"),
        TRADE("trade"),
        RUMOR("rumor");

        private final String id;

        MapLayer(String id) {
            this.id = id;
        }

        private Component label() {
            return Component.translatable(
                    "screen.immortal_cultivation_mod.life.map.layer." + id);
        }
    }
}
