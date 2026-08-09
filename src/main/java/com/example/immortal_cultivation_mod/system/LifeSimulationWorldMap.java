package com.example.immortal_cultivation_mod.system;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/** Persistent world-map, travel, and location-quest rules for simulated lives. */
public final class LifeSimulationWorldMap {
    public static final String ROOT = "WorldMap";
    public static final String CURRENT_LOCATION = "CurrentLocation";
    public static final String RESIDENCE = "Residence";
    public static final String SELECTED_DESTINATION = "SelectedDestination";
    public static final String ACTIVE_JOURNEY = "ActiveJourney";
    public static final String LOCATION_STATES = "LocationStates";
    public static final String DISCOVERED_LOCATIONS = "DiscoveredLocations";
    public static final String VISITED_LOCATIONS = "VisitedLocations";
    public static final String UNLOCKED_ROUTES = "UnlockedRoutes";
    public static final String WORLD_FLAGS = "WorldFlags";
    public static final String REGISTERED_TELEPORTS = "RegisteredTeleports";
    public static final String PENDING_ARRIVAL = "PendingArrival";

    private static final String CURRENT_EVENT = "CurrentEvent";
    private static final String INVENTORY = "Inventory";
    private static final int MAX_LOCAL_VALUE = 100;

    private static final List<LocationDefinition> LOCATIONS = List.of(
            location("tianheng_capital", "central", "city", "tianheng_dynasty",
                    230, 48, 0, 12, 2, 1, true, true, false),
            location("yunhai_market", "central", "floating_city", "merchant_alliance",
                    178, 126, 1, 12, 3, 2, true, true, false),
            location("jingyue_lake_city", "eastern_lakes", "lake_city", "mirror_moon_palace",
                    42, 126, 4, 15, 3, 2, true, true, false),
            location("wangu_swamp", "southern_swamp", "swamp", "wangu_tribe",
                    178, 188, 4, 12, 3, 4, false, false, false),
            location("tianlu_fire_domain", "western_firelands", "volcanic", "forge_alliance",
                    310, 68, 4, 15, 4, 4, false, true, false),
            location("guixu_islands", "eastern_sea", "ocean", "sea_clan",
                    18, 68, 1, 15, 3, 4, true, false, false),
            location("futian_academy", "northern_highlands", "academy", "futian_academy",
                    126, 48, 0, 12, 4, 1, true, true, false),
            location("wuxiang_temple", "central", "mountain_temple", "wuxiang_temple",
                    310, 126, 0, 27, 3, 1, false, true, false),
            location("jiuyou_ferry", "underworld", "underworld_ferry", "jiuyou_ferrymen",
                    178, 211, 0, 27, 4, 5, false, false, true),
            location("duanjie_city", "world_edge", "boundary_city", "boundary_guard",
                    178, 8, 10, 27, 5, 5, true, true, true));

    private static final List<RouteDefinition> ROUTES = List.of(
            route("tianheng_capital", "futian_academy", 1, 1, true),
            route("tianheng_capital", "yunhai_market", 1, 1, true),
            route("tianheng_capital", "tianlu_fire_domain", 2, 3, true),
            route("futian_academy", "guixu_islands", 2, 3, true),
            route("guixu_islands", "jingyue_lake_city", 1, 3, true),
            route("jingyue_lake_city", "yunhai_market", 1, 2, true),
            route("yunhai_market", "wuxiang_temple", 1, 1, true),
            route("yunhai_market", "wangu_swamp", 2, 4, true),
            route("wuxiang_temple", "tianlu_fire_domain", 2, 3, false),
            route("wangu_swamp", "jiuyou_ferry", 1, 5, false),
            route("tianheng_capital", "duanjie_city", 3, 5, false),
            route("futian_academy", "duanjie_city", 2, 4, false));

    private static final List<TravelMethodDefinition> TRAVEL_METHODS = List.of(
            new TravelMethodDefinition("foot", 0, 1),
            new TravelMethodDefinition("caravan", 1, -1),
            new TravelMethodDefinition("beast", 1, -1),
            new TravelMethodDefinition("flying_ship", 2, -2),
            new TravelMethodDefinition("artifact_flight", 2, 0),
            new TravelMethodDefinition("teleport", 3, -4));

    private static final Map<String, LocationDefinition> LOCATION_BY_ID =
            indexLocations();
    private static final Map<String, QuestDefinition> QUESTS = indexQuests();

    private LifeSimulationWorldMap() {
    }

    public static List<LocationDefinition> locations() {
        return LOCATIONS;
    }

    public static List<RouteDefinition> routes() {
        return ROUTES;
    }

    public static List<TravelMethodDefinition> travelMethods() {
        return TRAVEL_METHODS;
    }

    public static LocationDefinition location(String id) {
        return LOCATION_BY_ID.get(id);
    }

    public static CompoundTag state(CompoundTag life) {
        return life.getCompound(ROOT);
    }

    public static CompoundTag locationState(CompoundTag map, String locationId) {
        return map.getCompound(LOCATION_STATES).getCompound(locationId);
    }

    public static boolean discovered(CompoundTag map, String locationId) {
        return contains(map.getList(DISCOVERED_LOCATIONS, Tag.TAG_STRING), locationId);
    }

    public static boolean visited(CompoundTag map, String locationId) {
        return contains(map.getList(VISITED_LOCATIONS, Tag.TAG_STRING), locationId);
    }

    public static boolean routeUnlocked(CompoundTag map, RouteDefinition route) {
        return contains(map.getList(UNLOCKED_ROUTES, Tag.TAG_STRING), route.id());
    }

    public static boolean hasActiveJourney(CompoundTag life) {
        return state(life).contains(ACTIVE_JOURNEY, Tag.TAG_COMPOUND);
    }

    public static boolean hasPendingArrival(CompoundTag life) {
        return state(life).getBoolean(PENDING_ARRIVAL);
    }

    public static boolean blocksActivities(CompoundTag life) {
        return hasActiveJourney(life) || hasPendingArrival(life);
    }

    public static boolean blocksYearAdvance(CompoundTag life) {
        return hasPendingArrival(life);
    }

    public static void initialize(CompoundTag life) {
        CompoundTag map = new CompoundTag();
        String start = startLocation(life.getString("Birthplace"));
        map.putString(CURRENT_LOCATION, start);
        map.putString(RESIDENCE, start);
        map.putString(SELECTED_DESTINATION, start);
        map.putInt("TravelQi", maximumTravelQi(life));
        map.putBoolean("HasMount", false);
        map.putBoolean(PENDING_ARRIVAL, false);

        ListTag discovered = new ListTag();
        ListTag visited = new ListTag();
        ListTag routes = new ListTag();
        ListTag flags = new ListTag();
        ListTag teleports = new ListTag();
        for (LocationDefinition definition : LOCATIONS) {
            if (!definition.hidden()) {
                discovered.add(StringTag.valueOf(definition.id()));
            }
        }
        visited.add(StringTag.valueOf(start));
        LocationDefinition startDefinition = location(start);
        if (startDefinition != null && startDefinition.teleportation()) {
            teleports.add(StringTag.valueOf(start));
        }
        for (RouteDefinition route : ROUTES) {
            LocationDefinition first = location(route.first());
            LocationDefinition second = location(route.second());
            if (first != null && second != null
                    && !first.hidden() && !second.hidden()) {
                routes.add(StringTag.valueOf(route.id()));
            }
        }
        map.put(DISCOVERED_LOCATIONS, discovered);
        map.put(VISITED_LOCATIONS, visited);
        map.put(UNLOCKED_ROUTES, routes);
        map.put(WORLD_FLAGS, flags);
        map.put(REGISTERED_TELEPORTS, teleports);

        CompoundTag states = new CompoundTag();
        for (LocationDefinition definition : LOCATIONS) {
            states.put(definition.id(), initialLocationState(definition));
        }
        map.put(LOCATION_STATES, states);
        life.put(ROOT, map);
    }

    public static void normalize(CompoundTag life) {
        if (!life.contains(ROOT, Tag.TAG_COMPOUND)
                || state(life).getString(CURRENT_LOCATION).isBlank()) {
            initialize(life);
            return;
        }
        CompoundTag map = state(life).copy();
        putListIfMissing(map, DISCOVERED_LOCATIONS);
        putListIfMissing(map, VISITED_LOCATIONS);
        putListIfMissing(map, UNLOCKED_ROUTES);
        putListIfMissing(map, WORLD_FLAGS);
        putListIfMissing(map, REGISTERED_TELEPORTS);
        if (!map.contains(LOCATION_STATES, Tag.TAG_COMPOUND)) {
            map.put(LOCATION_STATES, new CompoundTag());
        }
        CompoundTag states = map.getCompound(LOCATION_STATES);
        for (LocationDefinition definition : LOCATIONS) {
            if (!states.contains(definition.id(), Tag.TAG_COMPOUND)) {
                states.put(definition.id(), initialLocationState(definition));
            } else {
                normalizeLocationState(states.getCompound(definition.id()), definition);
            }
        }
        map.put(LOCATION_STATES, states);
        if (map.getString(RESIDENCE).isBlank()) {
            map.putString(RESIDENCE, map.getString(CURRENT_LOCATION));
        }
        if (map.getString(SELECTED_DESTINATION).isBlank()) {
            map.putString(SELECTED_DESTINATION, map.getString(CURRENT_LOCATION));
        }
        int maximumQi = maximumTravelQi(life);
        if (!map.contains("TravelQi", Tag.TAG_INT)) {
            map.putInt("TravelQi", maximumQi);
        } else {
            map.putInt("TravelQi", Mth.clamp(map.getInt("TravelQi"), 0, maximumQi));
        }
        refreshUnlocks(life, map);
        life.put(ROOT, map);
    }

    public static void advanceYear(CompoundTag life, RandomSource random) {
        normalize(life);
        CompoundTag map = state(life).copy();
        map.putInt("TravelQi", maximumTravelQi(life));
        updateMovingMarket(map, random);
        if (random.nextFloat() < 0.32F) {
            applyDynamicEvent(life, map, random);
        }
        refreshUnlocks(life, map);
        life.put(ROOT, map);
    }

    /** Progresses a carried-over journey after annual action points refresh. */
    public static boolean progressJourney(CompoundTag life) {
        CompoundTag map = state(life).copy();
        if (!map.contains(ACTIVE_JOURNEY, Tag.TAG_COMPOUND)) {
            return false;
        }
        CompoundTag journey = map.getCompound(ACTIVE_JOURNEY).copy();
        int remaining = Math.max(0, journey.getInt("RemainingSegments"));
        int available = Math.max(0, life.getInt("AnnualActionPoints"));
        int spent = Math.min(remaining, available);
        remaining -= spent;
        life.putInt("AnnualActionPoints", available - spent);
        journey.putInt("RemainingSegments", remaining);
        map.put(ACTIVE_JOURNEY, journey);
        life.put(ROOT, map);
        if (remaining > 0) {
            LifeSimulationSystem.addLog(
                    life, "life.immortal_cultivation_mod.map.log.journey_continues");
            return true;
        }
        resolveJourneyArrival(life);
        return true;
    }

    public static boolean handleAction(
            ServerPlayer player,
            CompoundTag life,
            String action,
            RandomSource random) {
        normalize(life);
        if (action == null || action.isBlank()) {
            return false;
        }
        if (action.startsWith("select:")) {
            return selectDestination(life, action.substring(7));
        }
        if (action.startsWith("travel:")) {
            return startJourney(
                    player, life, action.substring(7), random);
        }
        if (action.startsWith("arrival:")) {
            return resolveArrivalMode(life, action.substring(8), random);
        }
        if (action.equals("quest")) {
            return startMainQuest(player, life);
        }
        if (action.equals("local_activity")) {
            return performLocalActivity(life, random);
        }
        if (action.equals("buy_mount")) {
            return buyMount(life);
        }
        if (action.equals("register_teleport")) {
            return registerTeleport(life);
        }
        if (action.equals("companion_next")) {
            return cycleCompanion(life);
        }
        return false;
    }

    public static boolean applyEventEffect(CompoundTag life, String effect) {
        if (effect == null || !effect.startsWith("worldmap:")) {
            return false;
        }
        String payload = effect.substring("worldmap:".length());
        if (payload.equals("arrive")) {
            arrive(life);
            return true;
        }
        if (payload.equals("delay")) {
            delayJourney(life);
            return true;
        }
        String[] parts = payload.split(":");
        if (parts.length == 0) {
            return true;
        }
        switch (parts[0]) {
            case "quest_end" -> {
                if (parts.length >= 3) {
                    completeQuest(life, parts[1], parts[2]);
                }
            }
            case "discover" -> {
                if (parts.length >= 2) {
                    discoverLocation(state(life), parts[1]);
                }
            }
            case "condition" -> {
                if (parts.length >= 3) {
                    mutableLocationState(life, parts[1])
                            .putString("Condition", parts[2]);
                }
            }
            case "faction" -> {
                if (parts.length >= 3) {
                    mutableLocationState(life, parts[1])
                            .putString("ControllingFaction", parts[2]);
                }
            }
            case "danger" -> adjustLocationValue(life, parts, "Danger", 0, 5);
            case "prosperity" -> adjustLocationValue(
                    life, parts, "Prosperity", 0, MAX_LOCAL_VALUE);
            case "reputation" -> adjustLocationValue(
                    life, parts, "LocalReputation", -100, 100);
            case "wanted" -> adjustLocationValue(
                    life, parts, "LocalWanted", 0, 100);
            case "teleport" -> {
                if (parts.length >= 3) {
                    mutableLocationState(life, parts[1])
                            .putBoolean("TeleportationActive", Boolean.parseBoolean(parts[2]));
                }
            }
            case "flag" -> {
                if (parts.length >= 2) {
                    addUnique(state(life).getList(WORLD_FLAGS, Tag.TAG_STRING), parts[1]);
                }
            }
            default -> {
            }
        }
        return true;
    }

    public static TravelPreview preview(
            CompoundTag life, String destinationId, String methodId) {
        normalize(life);
        CompoundTag map = state(life);
        LocationDefinition origin = location(map.getString(CURRENT_LOCATION));
        LocationDefinition destination = location(destinationId);
        TravelMethodDefinition method = travelMethod(methodId);
        if (origin == null || destination == null || method == null
                || origin.id().equals(destination.id())
                || !discovered(map, destination.id())) {
            return TravelPreview.unavailable();
        }
        PathResult path = shortestPath(map, origin.id(), destination.id());
        if (!path.reachable()) {
            return TravelPreview.unavailable();
        }
        int baseCost = Math.min(3, Math.max(1, path.cost()));
        int actionCost = switch (method.id()) {
            case "caravan", "beast" -> Math.max(1, baseCost - 1);
            case "flying_ship", "artifact_flight" -> 1;
            case "teleport" -> 0;
            default -> baseCost;
        };
        CompoundTag destinationState = locationState(map, destination.id());
        int localWanted = destinationState.getInt("LocalWanted");
        int companionSafety = selectedCompanion(life).isBlank() ? 0 : 1;
        int danger = Mth.clamp(
                path.danger() + destinationState.getInt("Danger") / 2
                        + localWanted / 25 + method.dangerModifier()
                        - companionSafety,
                0,
                5);
        long wealthCost = switch (method.id()) {
            case "caravan" -> 80L * baseCost;
            case "beast" -> 25L * baseCost;
            default -> 0L;
        };
        int spiritCost = switch (method.id()) {
            case "flying_ship" -> 10 * baseCost;
            case "teleport" -> 50 * baseCost;
            default -> 0;
        };
        int qiCost = "artifact_flight".equals(method.id())
                ? 150 * baseCost
                : 0;
        boolean available = methodAvailable(
                life, map, origin, destination, method, path)
                && life.getLong("Wealth") >= wealthCost
                && spiritStoneValue(life) >= spiritCost
                && map.getInt("TravelQi") >= qiCost;
        return new TravelPreview(
                true,
                available,
                baseCost,
                actionCost,
                danger,
                wealthCost,
                spiritCost,
                qiCost,
                path.tradeRoute());
    }

    public static int localCultivationPercent(CompoundTag life) {
        normalize(life);
        CompoundTag map = state(life);
        CompoundTag local = locationState(map, map.getString(CURRENT_LOCATION));
        return switch (Mth.clamp(local.getInt("SpiritualDensity"), 0, 4)) {
            case 0 -> 75;
            case 1 -> 100;
            case 2 -> 115;
            case 3 -> 135;
            default -> 165;
        };
    }

    public static String selectedCompanion(CompoundTag life) {
        CompoundTag map = state(life);
        return map.getString("SelectedCompanion");
    }

    private static boolean selectDestination(CompoundTag life, String destinationId) {
        LocationDefinition destination = location(destinationId);
        CompoundTag map = state(life).copy();
        if (destination == null || !discovered(map, destinationId)) {
            return false;
        }
        map.putString(SELECTED_DESTINATION, destinationId);
        life.put(ROOT, map);
        return true;
    }

    private static boolean startJourney(
            ServerPlayer player,
            CompoundTag life,
            String methodId,
            RandomSource random) {
        if (life.contains(CURRENT_EVENT, Tag.TAG_COMPOUND)
                || hasActiveJourney(life)
                || hasPendingArrival(life)) {
            return false;
        }
        CompoundTag map = state(life).copy();
        String destination = map.getString(SELECTED_DESTINATION);
        TravelPreview preview = preview(life, destination, methodId);
        if (!preview.available()) {
            return false;
        }
        if (preview.wealthCost() > 0L) {
            life.putLong("Wealth", life.getLong("Wealth") - preview.wealthCost());
        }
        if (preview.spiritStoneCost() > 0
                && !consumeSpiritStoneValue(life, preview.spiritStoneCost())) {
            return false;
        }
        map = state(life).copy();
        if (preview.qiCost() > 0) {
            map.putInt("TravelQi", map.getInt("TravelQi") - preview.qiCost());
        }

        CompoundTag journey = new CompoundTag();
        journey.putString("Origin", map.getString(CURRENT_LOCATION));
        journey.putString("Destination", destination);
        journey.putString("TravelMethod", methodId);
        journey.putInt("ActionPointCost", preview.actionPointCost());
        journey.putInt("DangerLevel", preview.danger());
        journey.putString("Companion", selectedCompanion(life));
        journey.putBoolean("Interrupted", false);
        journey.putBoolean("EventResolved", false);
        String eventId = chooseJourneyEvent(life, preview, methodId, random);
        journey.putString("GeneratedEvent", eventId);

        int availablePoints = Math.max(0, life.getInt("AnnualActionPoints"));
        int spent = Math.min(availablePoints, preview.actionPointCost());
        int remaining = preview.actionPointCost() - spent;
        life.putInt("AnnualActionPoints", availablePoints - spent);
        journey.putInt("RemainingSegments", remaining);
        map.put(ACTIVE_JOURNEY, journey);
        life.put(ROOT, map);
        LifeSimulationSystem.addLog(
                life,
                "life.immortal_cultivation_mod.map.log.depart."
                        + map.getString(CURRENT_LOCATION) + "." + destination);
        if (remaining > 0) {
            LifeSimulationSystem.addLog(
                    life, "life.immortal_cultivation_mod.map.log.journey_continues");
            return true;
        }
        resolveJourneyArrival(life);
        return true;
    }

    private static void resolveJourneyArrival(CompoundTag life) {
        CompoundTag map = state(life).copy();
        CompoundTag journey = map.getCompound(ACTIVE_JOURNEY).copy();
        if (!journey.getBoolean("EventResolved")
                && !journey.getString("GeneratedEvent").isBlank()) {
            life.put(ROOT, map);
            presentJourneyEvent(life, journey.getString("GeneratedEvent"));
            return;
        }
        arrive(life);
    }

    private static void presentJourneyEvent(CompoundTag life, String eventId) {
        CompoundTag event = new CompoundTag();
        event.putString("Id", "travel:" + eventId);
        event.putString(
                "TextKey",
                "life.immortal_cultivation_mod.map.travel_event." + eventId + ".event");
        event.putBoolean("TravelEvent", true);
        ListTag choices = new ListTag();
        switch (eventId) {
            case "collapsed_road" -> {
                choices.add(eventChoice(eventId, "help",
                        List.of("health-4", "reputation+4",
                                "worldmap:reputation:destination:4", "worldmap:arrive")));
                choices.add(eventChoice(eventId, "search",
                        List.of("comprehension+3", "wealth+80", "worldmap:arrive")));
                choices.add(eventChoice(eventId, "continue",
                        List.of("worldmap:arrive")));
                choices.add(eventChoice(eventId, "seize",
                        List.of("morality-8", "wealth+150",
                                "worldmap:wanted:destination:6", "worldmap:arrive")));
            }
            case "bandit_ambush" -> {
                choices.add(eventChoice(eventId, "fight",
                        List.of("health-7", "combat+5", "wealth+120", "worldmap:arrive")));
                choices.add(eventChoice(eventId, "pay",
                        List.of("wealth-100", "worldmap:arrive")));
                choices.add(eventChoice(eventId, "flee",
                        List.of("health-2", "mentality-2", "worldmap:arrive")));
                choices.add(eventChoice(eventId, "negotiate",
                        List.of("charisma+4", "reputation+2", "worldmap:arrive")));
            }
            case "spirit_storm" -> {
                choices.add(eventChoice(eventId, "shelter",
                        List.of("worldmap:delay")));
                choices.add(eventChoice(eventId, "cross",
                        List.of("health-8", "combat+3", "worldmap:arrive")));
                choices.add(eventChoice(eventId, "study",
                        List.of("comprehension+5", "mentality-4", "worldmap:arrive")));
                choices.add(eventChoice(eventId, "harvest",
                        List.of("health-4", "cultivation+900", "worldmap:arrive")));
            }
            case "illegal_cargo" -> {
                choices.add(eventChoice(eventId, "report",
                        List.of("morality+5", "reputation+5", "worldmap:arrive")));
                choices.add(eventChoice(eventId, "ignore",
                        List.of("worldmap:arrive")));
                choices.add(eventChoice(eventId, "join",
                        List.of("morality-5", "wealth+240", "worldmap:arrive")));
                choices.add(eventChoice(eventId, "confiscate",
                        List.of("combat+3", "wealth+160",
                                "worldmap:wanted:destination:4", "worldmap:arrive")));
            }
            case "flying_beast" -> {
                choices.add(eventChoice(eventId, "repel",
                        List.of("health-6", "combat+5", "worldmap:arrive")));
                choices.add(eventChoice(eventId, "evade",
                        List.of("mentality-2", "worldmap:arrive")));
                choices.add(eventChoice(eventId, "tame",
                        List.of("health-5", "charisma+4", "worldmap:flag:spirit_mount",
                                "worldmap:arrive")));
                choices.add(eventChoice(eventId, "follow",
                        List.of("comprehension+3", "worldmap:discover:guixu_islands",
                                "worldmap:arrive")));
            }
            default -> {
                choices.add(eventChoice("spatial_malfunction", "stabilize",
                        List.of("comprehension+3", "worldmap:arrive")));
                choices.add(eventChoice("spatial_malfunction", "endure",
                        List.of("health-5", "worldmap:arrive")));
                choices.add(eventChoice("spatial_malfunction", "abort",
                        List.of("worldmap:delay")));
                choices.add(eventChoice("spatial_malfunction", "observe",
                        List.of("mentality-3", "cultivation+600", "worldmap:arrive")));
            }
        }
        event.put("Choices", choices);
        life.put(CURRENT_EVENT, event);
    }

    private static CompoundTag eventChoice(
            String eventId, String choiceId, List<String> effects) {
        CompoundTag choice = new CompoundTag();
        String root = "life.immortal_cultivation_mod.map.travel_event."
                + eventId + "." + choiceId;
        choice.putString("Id", choiceId);
        choice.putString("TextKey", root + ".choice");
        choice.putString("ResultKey", root + ".result");
        choice.put("Effects", strings(effects));
        return choice;
    }

    private static void arrive(CompoundTag life) {
        CompoundTag map = state(life).copy();
        if (!map.contains(ACTIVE_JOURNEY, Tag.TAG_COMPOUND)) {
            return;
        }
        CompoundTag journey = map.getCompound(ACTIVE_JOURNEY);
        String destination = journey.getString("Destination");
        if (location(destination) == null) {
            map.remove(ACTIVE_JOURNEY);
            life.put(ROOT, map);
            return;
        }
        map.putString(CURRENT_LOCATION, destination);
        map.putString(SELECTED_DESTINATION, destination);
        map.putBoolean(PENDING_ARRIVAL, true);
        map.remove(ACTIVE_JOURNEY);
        discoverLocation(map, destination);
        addUnique(map.getList(VISITED_LOCATIONS, Tag.TAG_STRING), destination);
        discoverNeighbors(map, destination);
        refreshUnlocks(life, map);
        life.put(ROOT, map);
        LifeSimulationSystem.addLog(
                life, "life.immortal_cultivation_mod.map.log.arrive." + destination);
    }

    private static void delayJourney(CompoundTag life) {
        CompoundTag map = state(life).copy();
        if (!map.contains(ACTIVE_JOURNEY, Tag.TAG_COMPOUND)) {
            return;
        }
        CompoundTag journey = map.getCompound(ACTIVE_JOURNEY).copy();
        journey.putBoolean("EventResolved", true);
        journey.putBoolean("Interrupted", true);
        journey.putInt("RemainingSegments", 1);
        map.put(ACTIVE_JOURNEY, journey);
        life.put(ROOT, map);
    }

    private static boolean resolveArrivalMode(
            CompoundTag life, String mode, RandomSource random) {
        CompoundTag map = state(life).copy();
        if (!map.getBoolean(PENDING_ARRIVAL)) {
            return false;
        }
        String current = map.getString(CURRENT_LOCATION);
        CompoundTag local = map.getCompound(LOCATION_STATES).getCompound(current);
        switch (mode) {
            case "stay" -> {
                map.putBoolean(PENDING_ARRIVAL, false);
                map.putString("StayMode", "temporary");
            }
            case "settle" -> {
                long relocationCost = 100L
                        + life.getList("Relationships", Tag.TAG_COMPOUND).size() * 25L;
                if (life.getLong("Wealth") < relocationCost) {
                    return false;
                }
                life.putLong("Wealth", life.getLong("Wealth") - relocationCost);
                map.putBoolean(PENDING_ARRIVAL, false);
                map.putString(RESIDENCE, current);
                map.putString("StayMode", "resident");
                adjustRelationshipsForRelocation(life, random);
                LifeSimulationSystem.addLog(
                        life, "life.immortal_cultivation_mod.map.log.settle." + current);
            }
            case "cave" -> {
                if (life.getInt("Realm") < 7
                        || life.getLong("Wealth") < 5_000L
                        || local.getInt("LocalReputation") < 20) {
                    return false;
                }
                life.putLong("Wealth", life.getLong("Wealth") - 5_000L);
                local.putBoolean("OwnedProperty", true);
                local.putString("PropertyType", "cave_abode");
                local.putInt("Prosperity", Math.min(
                        MAX_LOCAL_VALUE, local.getInt("Prosperity") + 5));
                map.getCompound(LOCATION_STATES).put(current, local);
                map.putBoolean(PENDING_ARRIVAL, false);
                map.putString(RESIDENCE, current);
                map.putString("StayMode", "cave_abode");
                LifeSimulationSystem.addLog(
                        life, "life.immortal_cultivation_mod.map.log.cave." + current);
            }
            default -> {
                return false;
            }
        }
        life.put(ROOT, map);
        return true;
    }

    private static boolean startMainQuest(
            ServerPlayer player, CompoundTag life) {
        if (life.contains(CURRENT_EVENT, Tag.TAG_COMPOUND)
                || hasActiveJourney(life) || hasPendingArrival(life)) {
            return false;
        }
        CompoundTag map = state(life);
        String current = map.getString(CURRENT_LOCATION);
        QuestDefinition endingRules = QUESTS.get(current);
        if (endingRules == null) {
            return false;
        }
        CompoundTag local = mutableLocationState(life, current);
        if (local.getBoolean("MainQuestCompleted")) {
            return false;
        }
        String questId = "immortal_cultivation_mod:location_" + current;
        LifeSimulationQuests.Definition quest = LifeSimulationQuests.find(
                player.server.getResourceManager(), questId);
        if (quest == null) {
            return false;
        }
        CompoundTag saved = life.getCompound("QuestProgress")
                .getCompound(questId);
        String node = saved.getString("CurrentNode");
        if (node.isBlank()) {
            node = quest.startNode();
        }
        local.putInt("MainQuestStage", Math.max(1, saved.getList(
                "Path", Tag.TAG_STRING).size() + 1));
        return LifeSimulationSystem.openQuestNode(life, quest, node);
    }

    private static void completeQuest(
            CompoundTag life, String locationId, String endingId) {
        QuestDefinition quest = QUESTS.get(locationId);
        if (quest == null || quest.endings().stream()
                .noneMatch(ending -> ending.id().equals(endingId))) {
            return;
        }
        CompoundTag local = mutableLocationState(life, locationId);
        local.putBoolean("MainQuestCompleted", true);
        local.putString("MainQuestEnding", endingId);
        local.putInt("MainQuestStage", 4);
        local.putInt("LocalReputation", Mth.clamp(
                local.getInt("LocalReputation") + 12, -100, 100));
    }

    private static boolean performLocalActivity(
            CompoundTag life, RandomSource random) {
        if (life.contains(CURRENT_EVENT, Tag.TAG_COMPOUND)
                || blocksActivities(life)
                || life.getInt("AnnualActionPoints") <= 0) {
            return false;
        }
        CompoundTag map = state(life);
        String current = map.getString(CURRENT_LOCATION);
        switch (current) {
            case "tianheng_capital" -> {
                life.putLong("Wealth", life.getLong("Wealth") + random.nextInt(90, 181));
                LifeSimulationSystem.applyEffect(life, "reputation+3");
            }
            case "yunhai_market" -> {
                life.putLong("Wealth", life.getLong("Wealth") + random.nextInt(130, 251));
                LifeSimulationSystem.applyEffect(life, "comprehension+2");
            }
            case "jingyue_lake_city" -> {
                LifeSimulationSystem.applyEffect(life, "comprehension+4");
                LifeSimulationSystem.applyEffect(life, "mentality+3");
            }
            case "wangu_swamp" -> {
                LifeSimulationSystem.applyEffect(life, "medical+5");
                LifeSimulationSystem.applyEffect(life, "health-2");
            }
            case "tianlu_fire_domain" -> {
                LifeSimulationSystem.applyEffect(life, "alchemy+5");
                LifeSimulationSystem.applyEffect(life, "combat+3");
            }
            case "guixu_islands" -> {
                life.putLong("Wealth", life.getLong("Wealth") + random.nextInt(100, 221));
                LifeSimulationSystem.applyEffect(life, "combat+3");
            }
            case "futian_academy" -> LifeSimulationSystem.applyEffect(
                    life, "comprehension+6");
            case "wuxiang_temple" -> {
                LifeSimulationSystem.applyEffect(life, "mentality+6");
                LifeSimulationSystem.applyEffect(life, "medical+2");
            }
            case "jiuyou_ferry" -> {
                LifeSimulationSystem.applyEffect(life, "mentality-3");
                LifeSimulationSystem.applyEffect(life, "lifespan+2");
            }
            case "duanjie_city" -> {
                LifeSimulationSystem.applyEffect(life, "comprehension+5");
                LifeSimulationSystem.applyEffect(life, "combat+5");
            }
            default -> {
                return false;
            }
        }
        life.putInt("AnnualActionPoints", life.getInt("AnnualActionPoints") - 1);
        CompoundTag local = mutableLocationState(life, current);
        local.putInt("LocalReputation", Mth.clamp(
                local.getInt("LocalReputation") + 2, -100, 100));
        LifeSimulationSystem.addLog(
                life, "life.immortal_cultivation_mod.map.log.local_activity." + current);
        return true;
    }

    private static boolean buyMount(CompoundTag life) {
        CompoundTag map = state(life).copy();
        if (map.getBoolean("HasMount") || life.getLong("Wealth") < 800L
                || blocksActivities(life)) {
            return false;
        }
        life.putLong("Wealth", life.getLong("Wealth") - 800L);
        map.putBoolean("HasMount", true);
        life.put(ROOT, map);
        return true;
    }

    private static boolean registerTeleport(CompoundTag life) {
        CompoundTag map = state(life).copy();
        String current = map.getString(CURRENT_LOCATION);
        CompoundTag local = locationState(map, current);
        ListTag registered = map.getList(REGISTERED_TELEPORTS, Tag.TAG_STRING);
        if (!local.getBoolean("TeleportationActive")
                || contains(registered, current)
                || life.getLong("Wealth") < 500L) {
            return false;
        }
        life.putLong("Wealth", life.getLong("Wealth") - 500L);
        addUnique(registered, current);
        map.put(REGISTERED_TELEPORTS, registered);
        life.put(ROOT, map);
        return true;
    }

    private static boolean cycleCompanion(CompoundTag life) {
        ListTag relationships = life.getList("Relationships", Tag.TAG_COMPOUND);
        List<String> eligible = new ArrayList<>();
        for (int i = 0; i < relationships.size(); i++) {
            CompoundTag person = relationships.getCompound(i);
            if (person.getBoolean("Alive") && person.getInt("Affinity") >= 40) {
                eligible.add(person.getString("Name"));
            }
        }
        CompoundTag map = state(life).copy();
        if (eligible.isEmpty()) {
            map.putString("SelectedCompanion", "");
        } else {
            String current = map.getString("SelectedCompanion");
            int index = eligible.indexOf(current);
            map.putString("SelectedCompanion", eligible.get((index + 1) % eligible.size()));
        }
        life.put(ROOT, map);
        return true;
    }

    private static void applyDynamicEvent(
            CompoundTag life, CompoundTag map, RandomSource random) {
        List<LocationDefinition> mutable = LOCATIONS.stream()
                .filter(location -> discovered(map, location.id()))
                .filter(location -> !locationState(map, location.id())
                        .getBoolean("MainQuestCompleted"))
                .toList();
        if (mutable.isEmpty()) {
            return;
        }
        LocationDefinition target = mutable.get(random.nextInt(mutable.size()));
        CompoundTag local = locationState(map, target.id());
        String[] conditions = {
                "NORMAL", "PROSPEROUS", "WAR", "PLAGUE",
                "OCCUPIED", "SEALED", "HAUNTED", "ABANDONED"
        };
        String condition = conditions[random.nextInt(conditions.length)];
        local.putString("Condition", condition);
        switch (condition) {
            case "PROSPEROUS" -> {
                local.putInt("Prosperity", Math.min(
                        MAX_LOCAL_VALUE, local.getInt("Prosperity") + 15));
                local.putInt("Danger", Math.max(0, local.getInt("Danger") - 1));
            }
            case "WAR", "PLAGUE", "HAUNTED" ->
                    local.putInt("Danger", Math.min(5, local.getInt("Danger") + 1));
            case "OCCUPIED" -> local.putString(
                    "ControllingFaction", randomFaction(random));
            case "ABANDONED" -> local.putInt("Prosperity", Math.max(
                    0, local.getInt("Prosperity") - 20));
            default -> {
            }
        }
        map.getCompound(LOCATION_STATES).put(target.id(), local);
        LifeSimulationSystem.addLog(
                life,
                "life.immortal_cultivation_mod.map.dynamic."
                        + condition.toLowerCase());
    }

    private static void updateMovingMarket(CompoundTag map, RandomSource random) {
        CompoundTag market = locationState(map, "yunhai_market");
        int remaining = market.contains("MoveInYears", Tag.TAG_INT)
                ? market.getInt("MoveInYears") - 1
                : random.nextInt(3, 6);
        if (remaining <= 0) {
            String[] regions = {
                    "central", "eastern_lakes", "eastern_sea",
                    "northern_highlands", "western_firelands"
            };
            String current = market.getString("MovingRegion");
            String next = regions[random.nextInt(regions.length)];
            if (next.equals(current)) {
                next = regions[(random.nextInt(regions.length - 1) + 1)
                        % regions.length];
            }
            market.putString("MovingRegion", next);
            market.putInt("MoveInYears", random.nextInt(3, 6));
            market.putString("Condition", "MOVING");
        } else {
            market.putInt("MoveInYears", remaining);
        }
        map.getCompound(LOCATION_STATES).put("yunhai_market", market);
    }

    private static boolean methodAvailable(
            CompoundTag life,
            CompoundTag map,
            LocationDefinition origin,
            LocationDefinition destination,
            TravelMethodDefinition method,
            PathResult path) {
        return switch (method.id()) {
            case "foot" -> true;
            case "caravan" -> path.tradeRoute()
                    && !isRouteSuppressed(map, origin.id(), destination.id());
            case "beast" -> map.getBoolean("HasMount")
                    || contains(map.getList(WORLD_FLAGS, Tag.TAG_STRING), "spirit_mount");
            case "flying_ship" -> origin.flyingShipPort() && destination.flyingShipPort()
                    && !isSealed(map, origin.id()) && !isSealed(map, destination.id());
            case "artifact_flight" -> life.getInt("Realm") >= 4
                    && (knowsSpell(life, "yufeng_jue")
                    || knowsSpell(life, "yuqi_shu"));
            case "teleport" -> origin.teleportation() && destination.teleportation()
                    && locationState(map, origin.id()).getBoolean("TeleportationActive")
                    && locationState(map, destination.id()).getBoolean("TeleportationActive")
                    && contains(map.getList(REGISTERED_TELEPORTS, Tag.TAG_STRING), origin.id())
                    && contains(map.getList(REGISTERED_TELEPORTS, Tag.TAG_STRING), destination.id())
                    && !isSealed(map, origin.id()) && !isSealed(map, destination.id());
            default -> false;
        };
    }

    private static String chooseJourneyEvent(
            CompoundTag life,
            TravelPreview preview,
            String method,
            RandomSource random) {
        int chance = Mth.clamp(
                8 + preview.danger() * 12
                        + Math.max(0, currentLocalWanted(life)) / 3,
                5,
                78);
        if ("teleport".equals(method)) {
            chance = 3;
        }
        if (random.nextInt(100) >= chance) {
            return "";
        }
        if ("teleport".equals(method)) {
            return "spatial_malfunction";
        }
        if ("caravan".equals(method) && random.nextBoolean()) {
            return "illegal_cargo";
        }
        if (("flying_ship".equals(method) || "artifact_flight".equals(method))
                && random.nextBoolean()) {
            return random.nextBoolean() ? "spirit_storm" : "flying_beast";
        }
        String[] common = {"collapsed_road", "bandit_ambush", "spirit_storm"};
        return common[random.nextInt(common.length)];
    }

    private static int currentLocalWanted(CompoundTag life) {
        CompoundTag map = state(life);
        return locationState(map, map.getString(CURRENT_LOCATION)).getInt("LocalWanted");
    }

    private static PathResult shortestPath(
            CompoundTag map, String origin, String destination) {
        Map<String, Integer> distance = new HashMap<>();
        Map<String, Integer> danger = new HashMap<>();
        Map<String, Boolean> tradeOnly = new HashMap<>();
        Set<String> visited = new HashSet<>();
        distance.put(origin, 0);
        danger.put(origin, 0);
        tradeOnly.put(origin, true);
        while (true) {
            String current = distance.entrySet().stream()
                    .filter(entry -> !visited.contains(entry.getKey()))
                    .min(Comparator.comparingInt(Map.Entry::getValue))
                    .map(Map.Entry::getKey)
                    .orElse(null);
            if (current == null) {
                break;
            }
            if (current.equals(destination)) {
                return new PathResult(
                        true,
                        distance.get(current),
                        danger.getOrDefault(current, 0),
                        tradeOnly.getOrDefault(current, false));
            }
            visited.add(current);
            for (RouteDefinition route : ROUTES) {
                if (!routeUnlocked(map, route) || !route.touches(current)) {
                    continue;
                }
                String next = route.other(current);
                if (!discovered(map, next)) {
                    continue;
                }
                int candidate = distance.get(current) + route.actionCost();
                if (candidate < distance.getOrDefault(next, Integer.MAX_VALUE)) {
                    distance.put(next, candidate);
                    danger.put(next, Math.max(
                            danger.getOrDefault(current, 0), route.danger()));
                    tradeOnly.put(next,
                            tradeOnly.getOrDefault(current, true) && route.tradeRoute());
                }
            }
        }
        return PathResult.unreachable();
    }

    private static boolean isRouteSuppressed(
            CompoundTag map, String origin, String destination) {
        return isSealed(map, origin) || isSealed(map, destination)
                || "PLAGUE".equals(locationState(map, destination)
                .getString("Condition"));
    }

    private static boolean isSealed(CompoundTag map, String locationId) {
        String condition = locationState(map, locationId).getString("Condition");
        return "SEALED".equals(condition) || "DESTROYED".equals(condition);
    }

    private static void refreshUnlocks(CompoundTag life, CompoundTag map) {
        if (life.getInt("Health") <= 10
                || hasLifeFlag(life, "soul_travel")
                || contains(map.getList(WORLD_FLAGS, Tag.TAG_STRING), "underworld_invitation")) {
            discoverLocation(map, "jiuyou_ferry");
            unlockRoute(map, "wangu_swamp", "jiuyou_ferry");
        }
        int visitedCount = map.getList(VISITED_LOCATIONS, Tag.TAG_STRING).size();
        if (visitedCount >= 5) {
            addUnique(map.getList(WORLD_FLAGS, Tag.TAG_STRING), "western_return_rumor");
        }
        if (life.getInt("Realm") >= 10
                && visitedCount >= 6
                && contains(map.getList(WORLD_FLAGS, Tag.TAG_STRING), "western_return_rumor")) {
            discoverLocation(map, "duanjie_city");
            unlockRoute(map, "tianheng_capital", "duanjie_city");
            unlockRoute(map, "futian_academy", "duanjie_city");
        }
    }

    private static void discoverNeighbors(CompoundTag map, String locationId) {
        for (RouteDefinition route : ROUTES) {
            if (!route.touches(locationId)) {
                continue;
            }
            String neighbor = route.other(locationId);
            LocationDefinition definition = location(neighbor);
            if (definition != null && (!definition.hidden()
                    || discovered(map, neighbor))) {
                discoverLocation(map, neighbor);
                unlockRoute(map, route.first(), route.second());
            }
        }
    }

    private static void discoverLocation(CompoundTag map, String locationId) {
        if (location(locationId) == null) {
            return;
        }
        ListTag locations = map.getList(DISCOVERED_LOCATIONS, Tag.TAG_STRING);
        addUnique(locations, locationId);
        map.put(DISCOVERED_LOCATIONS, locations);
    }

    private static void unlockRoute(
            CompoundTag map, String first, String second) {
        RouteDefinition route = ROUTES.stream()
                .filter(candidate -> candidate.connects(first, second))
                .findFirst()
                .orElse(null);
        if (route == null) {
            return;
        }
        ListTag routes = map.getList(UNLOCKED_ROUTES, Tag.TAG_STRING);
        addUnique(routes, route.id());
        map.put(UNLOCKED_ROUTES, routes);
    }

    private static CompoundTag mutableLocationState(
            CompoundTag life, String locationId) {
        CompoundTag map = state(life);
        CompoundTag states = map.getCompound(LOCATION_STATES);
        CompoundTag local = states.getCompound(locationId);
        states.put(locationId, local);
        map.put(LOCATION_STATES, states);
        life.put(ROOT, map);
        return local;
    }

    private static void adjustLocationValue(
            CompoundTag life,
            String[] parts,
            String key,
            int minimum,
            int maximum) {
        if (parts.length < 3) {
            return;
        }
        String locationId = resolveLocationAlias(life, parts[1]);
        try {
            int delta = Integer.parseInt(parts[2]);
            CompoundTag local = mutableLocationState(life, locationId);
            local.putInt(key, Mth.clamp(local.getInt(key) + delta, minimum, maximum));
        } catch (NumberFormatException ignored) {
            // Invalid optional data cannot alter map state.
        }
    }

    private static String resolveLocationAlias(CompoundTag life, String id) {
        if (!"destination".equals(id)) {
            return id;
        }
        CompoundTag journey = state(life).getCompound(ACTIVE_JOURNEY);
        return journey.getString("Destination");
    }

    private static void adjustRelationshipsForRelocation(
            CompoundTag life, RandomSource random) {
        ListTag relationships = life.getList("Relationships", Tag.TAG_COMPOUND);
        for (int i = 0; i < relationships.size(); i++) {
            CompoundTag person = relationships.getCompound(i);
            if (!person.getBoolean("Alive")) {
                continue;
            }
            int delta = switch (person.getString("Relation")) {
                case "dao_companion", "child" -> random.nextBoolean() ? 2 : -3;
                case "father", "mother" -> random.nextInt(-4, 2);
                default -> random.nextInt(-2, 3);
            };
            person.putInt("Affinity", Mth.clamp(
                    person.getInt("Affinity") + delta, -100, 100));
            relationships.set(i, person);
        }
        life.put("Relationships", relationships);
    }

    private static boolean knowsSpell(CompoundTag life, String id) {
        return contains(life.getList("DiscoveredSpells", Tag.TAG_STRING), id);
    }

    private static boolean hasLifeFlag(CompoundTag life, String flag) {
        return contains(life.getList("Flags", Tag.TAG_STRING), flag);
    }

    private static int maximumTravelQi(CompoundTag life) {
        long scaled = 500L + Math.max(0, life.getInt("Realm")) * 750L;
        return (int) Math.min(Integer.MAX_VALUE, scaled);
    }

    private static int spiritStoneValue(CompoundTag life) {
        int total = 0;
        ListTag inventory = life.getList(INVENTORY, Tag.TAG_COMPOUND);
        for (int i = 0; i < inventory.size(); i++) {
            CompoundTag entry = inventory.getCompound(i);
            int value = spiritStoneUnitValue(entry.getString("Id"));
            if (value > 0) {
                total = Math.min(Integer.MAX_VALUE,
                        total + entry.getInt("Count") * value);
            }
        }
        return total;
    }

    private static boolean consumeSpiritStoneValue(
            CompoundTag life, int required) {
        int total = spiritStoneValue(life);
        if (total < required) {
            return false;
        }
        ListTag inventory = life.getList(INVENTORY, Tag.TAG_COMPOUND);
        for (int i = inventory.size() - 1; i >= 0; i--) {
            CompoundTag entry = inventory.getCompound(i);
            if (spiritStoneUnitValue(entry.getString("Id")) > 0) {
                inventory.remove(i);
            }
        }
        life.put(INVENTORY, inventory);
        int change = total - required;
        if (change >= 100) {
            LifeSimulationSystem.addSimulationItem(
                    life, "immortal_cultivation_mod:high_spirit_stone", change / 100);
            change %= 100;
        }
        if (change >= 10) {
            LifeSimulationSystem.addSimulationItem(
                    life, "immortal_cultivation_mod:medium_spirit_stone", change / 10);
            change %= 10;
        }
        if (change > 0) {
            LifeSimulationSystem.addSimulationItem(
                    life, "immortal_cultivation_mod:spirit_stone", change);
        }
        return true;
    }

    private static int spiritStoneUnitValue(String id) {
        return switch (id) {
            case "immortal_cultivation_mod:spirit_stone" -> 1;
            case "immortal_cultivation_mod:medium_spirit_stone" -> 10;
            case "immortal_cultivation_mod:high_spirit_stone" -> 100;
            default -> 0;
        };
    }

    private static TravelMethodDefinition travelMethod(String id) {
        return TRAVEL_METHODS.stream()
                .filter(method -> method.id().equals(id))
                .findFirst()
                .orElse(null);
    }

    private static CompoundTag initialLocationState(LocationDefinition definition) {
        CompoundTag state = new CompoundTag();
        state.putString("ControllingFaction", definition.defaultFaction());
        state.putString("Condition", "yunhai_market".equals(definition.id())
                ? "MOVING"
                : "NORMAL");
        state.putBoolean("TeleportationActive", definition.teleportation());
        state.putBoolean("MainQuestCompleted", false);
        state.putString("MainQuestEnding", "");
        state.putInt("MainQuestStage", 1);
        state.putInt("Prosperity", 50);
        state.putInt("Danger", definition.baseDanger());
        state.putInt("SpiritualDensity", definition.spiritualDensity());
        state.putInt("LocalReputation", 0);
        state.putInt("LocalWanted", 0);
        state.putBoolean("OwnedProperty", false);
        state.put("VisitedLandmarks", new ListTag());
        if ("yunhai_market".equals(definition.id())) {
            state.putString("MovingRegion", "central");
            state.putInt("MoveInYears", 3);
        }
        return state;
    }

    private static void normalizeLocationState(
            CompoundTag state, LocationDefinition definition) {
        if (!state.contains("ControllingFaction", Tag.TAG_STRING)) {
            state.putString("ControllingFaction", definition.defaultFaction());
        }
        if (!state.contains("Condition", Tag.TAG_STRING)) {
            state.putString("Condition", "NORMAL");
        }
        if (!state.contains("TeleportationActive", Tag.TAG_BYTE)) {
            state.putBoolean("TeleportationActive", definition.teleportation());
        }
        if (!state.contains("MainQuestStage", Tag.TAG_INT)) {
            state.putInt("MainQuestStage", 1);
        }
        if (!state.contains("Prosperity", Tag.TAG_INT)) {
            state.putInt("Prosperity", 50);
        }
        if (!state.contains("Danger", Tag.TAG_INT)) {
            state.putInt("Danger", definition.baseDanger());
        }
        if (!state.contains("SpiritualDensity", Tag.TAG_INT)) {
            state.putInt("SpiritualDensity", definition.spiritualDensity());
        }
        if (!state.contains("VisitedLandmarks", Tag.TAG_LIST)) {
            state.put("VisitedLandmarks", new ListTag());
        }
    }

    private static void putListIfMissing(CompoundTag tag, String key) {
        if (!tag.contains(key, Tag.TAG_LIST)) {
            tag.put(key, new ListTag());
        }
    }

    private static ListTag strings(List<String> values) {
        ListTag result = new ListTag();
        for (String value : values) {
            result.add(StringTag.valueOf(value));
        }
        return result;
    }

    private static void addUnique(ListTag list, String value) {
        if (!contains(list, value)) {
            list.add(StringTag.valueOf(value));
        }
    }

    private static boolean contains(ListTag list, String value) {
        for (int i = 0; i < list.size(); i++) {
            if (value.equals(list.getString(i))) {
                return true;
            }
        }
        return false;
    }

    private static String startLocation(String birthplace) {
        return switch (birthplace) {
            case "cloudriver_town" -> "yunhai_market";
            case "northwild_farm" -> "wuxiang_temple";
            case "redleaf_city" -> "tianlu_fire_domain";
            case "whitewater_hamlet" -> "jingyue_lake_city";
            case "mistmountain_pass" -> "futian_academy";
            default -> "tianheng_capital";
        };
    }

    private static String randomFaction(RandomSource random) {
        String[] factions = {
                "tianheng_dynasty", "azure_cloud_sect", "mystic_sword_sect",
                "hundred_flower_valley", "wangu_tribe", "sea_clan",
                "wandering_alliance", "demonic_forces"
        };
        return factions[random.nextInt(factions.length)];
    }

    private static LocationDefinition location(
            String id,
            String region,
            String terrain,
            String faction,
            int mapX,
            int mapY,
            int minimumRealm,
            int maximumRealm,
            int spiritualDensity,
            int danger,
            boolean flyingShipPort,
            boolean teleportation,
            boolean hidden) {
        return new LocationDefinition(
                id, region, terrain, faction, mapX, mapY,
                minimumRealm, maximumRealm, spiritualDensity, danger,
                flyingShipPort, teleportation, hidden);
    }

    private static RouteDefinition route(
            String first,
            String second,
            int actionCost,
            int danger,
            boolean tradeRoute) {
        return new RouteDefinition(
                first.compareTo(second) <= 0
                        ? first + "__" + second
                        : second + "__" + first,
                first,
                second,
                actionCost,
                danger,
                tradeRoute);
    }

    private static Map<String, LocationDefinition> indexLocations() {
        Map<String, LocationDefinition> result = new HashMap<>();
        for (LocationDefinition location : LOCATIONS) {
            result.put(location.id(), location);
        }
        return Map.copyOf(result);
    }

    private static Map<String, QuestDefinition> indexQuests() {
        Map<String, QuestDefinition> result = new HashMap<>();
        result.put("tianheng_capital", quest("tianheng_capital",
                ending("ninth_prince_restored", "",
                        "reputation+20", "morality+12",
                        "worldmap:condition:tianheng_capital:PROSPEROUS",
                        "worldmap:prosperity:tianheng_capital:25"),
                ending("dragon_vein_sacrifice", "",
                        "wealth+1000", "morality-25",
                        "worldmap:condition:tianheng_capital:NORMAL",
                        "worldmap:prosperity:tianheng_capital:10"),
                ending("puppet_throne", "",
                        "wealth+1800", "reputation+30", "morality-12",
                        "worldmap:condition:tianheng_capital:OCCUPIED"),
                ending("nameless_departure", "",
                        "morality+8", "friend:wandering_prince",
                        "worldmap:wanted:tianheng_capital:8"),
                ending("dragon_vein_to_people",
                        "comprehension>=85,reputation>=60,morality>=40",
                        "reputation+35", "morality+20",
                        "worldmap:faction:tianheng_capital:provincial_council",
                        "worldmap:condition:tianheng_capital:PROSPEROUS",
                        "worldmap:flag:commoner_spiritual_awakening")));
        result.put("yunhai_market", quest("yunhai_market",
                ending("cloud_city_rises", "", "wealth+1600", "reputation+18",
                        "worldmap:condition:yunhai_market:MOVING"),
                ending("market_falls", "", "morality+10", "reputation+12",
                        "worldmap:condition:yunhai_market:NORMAL",
                        "worldmap:flag:yunhai_ground_city"),
                ending("cloud_kun_journey", "", "reputation+20",
                        "worldmap:condition:yunhai_market:MOVING",
                        "worldmap:flag:cloud_kun_city"),
                ending("market_of_one", "", "wealth+3000", "morality-15",
                        "worldmap:condition:yunhai_market:OCCUPIED"),
                ending("save_all_twelve_hours",
                        "comprehension>=80,wealth>=3000,reputation>=50",
                        "reputation+35", "morality+20", "wealth+1000",
                        "worldmap:condition:yunhai_market:PROSPEROUS")));
        result.put("jingyue_lake_city", quest("jingyue_lake_city",
                ending("one_moon_above", "", "reputation+15",
                        "worldmap:condition:jingyue_lake_city:NORMAL"),
                ending("reflection_made_real", "", "comprehension+8",
                        "worldmap:condition:jingyue_lake_city:HAUNTED"),
                ending("twin_cities", "", "reputation+20", "mentality-8",
                        "worldmap:condition:jingyue_lake_city:PROSPEROUS",
                        "worldmap:flag:mirror_city_unlocked"),
                ending("shattered_mirror", "", "morality-5",
                        "worldmap:condition:jingyue_lake_city:DESTROYED",
                        "worldmap:danger:jingyue_lake_city:2"),
                ending("two_moons_reflect",
                        "comprehension>=85,mentality>=80",
                        "comprehension+12", "reputation+30",
                        "worldmap:condition:jingyue_lake_city:PROSPEROUS",
                        "worldmap:flag:stable_mirror_city")));
        result.put("wangu_swamp", quest("wangu_swamp",
                ending("twenty_year_bride", "", "medical+8", "reputation+12",
                        "worldmap:condition:wangu_swamp:NORMAL"),
                ending("mother_of_gu_falls", "", "combat+10", "reputation+18",
                        "worldmap:condition:wangu_swamp:HAUNTED",
                        "worldmap:danger:wangu_swamp:2"),
                ending("escape_with_bride", "", "friend:gu_bride", "morality+6",
                        "worldmap:wanted:wangu_swamp:15"),
                ending("body_of_gu_mother", "", "medical+15", "morality-15",
                        "worldmap:faction:wangu_swamp:player_gu_hive"),
                ending("shared_gu_covenant",
                        "medical>=60,morality>=40",
                        "medical+12", "reputation+30",
                        "worldmap:condition:wangu_swamp:PROSPEROUS")));
        result.put("tianlu_fire_domain", quest("tianlu_fire_domain",
                ending("furnace_fire_eternal", "", "alchemy+10", "wealth+1200",
                        "worldmap:condition:tianlu_fire_domain:PROSPEROUS"),
                ending("fire_spirit_returns", "", "morality+12", "comprehension+8",
                        "worldmap:condition:tianlu_fire_domain:ABANDONED",
                        "worldmap:flag:free_fire_spirit"),
                ending("one_artifact_for_city", "", "reputation+20", "wealth+1600",
                        "worldmap:condition:tianlu_fire_domain:NORMAL"),
                ending("heavenly_heart_claimed", "", "combat+12", "morality-10",
                        "worldmap:condition:tianlu_fire_domain:OCCUPIED"),
                ending("ten_thousand_hearths",
                        "alchemy>=60,reputation>=50",
                        "alchemy+15", "reputation+30",
                        "worldmap:condition:tianlu_fire_domain:PROSPEROUS")));
        result.put("guixu_islands", quest("guixu_islands",
                ending("people_return_today", "", "morality+8", "reputation+12"),
                ending("anchor_thirteenth_island", "", "reputation+20",
                        "worldmap:condition:guixu_islands:PROSPEROUS",
                        "worldmap:flag:thirteenth_island"),
                ending("borrow_the_tide", "", "cultivation+2500", "morality-8",
                        "worldmap:condition:guixu_islands:HAUNTED"),
                ending("island_sinks", "", "mentality-5",
                        "worldmap:condition:guixu_islands:ABANDONED"),
                ending("all_ships_return",
                        "luck>=75,comprehension>=80",
                        "reputation+30", "morality+18", "comprehension+10",
                        "worldmap:condition:guixu_islands:PROSPEROUS")));
        result.put("futian_academy", quest("futian_academy",
                ending("seventh_seat_returns", "", "comprehension+10", "mentality-4"),
                ending("leave_the_empty_seat", "", "wealth+800", "morality-5"),
                ending("burn_fate_register", "", "morality+12", "reputation+12",
                        "worldmap:flag:no_future_prediction"),
                ending("write_a_new_fate", "", "comprehension+15", "morality-12",
                        "worldmap:flag:fate_register_owned"),
                ending("person_outside_books",
                        "comprehension>=90,mentality>=75",
                        "friend:pei_zhao", "comprehension+12", "reputation+25")));
        result.put("wuxiang_temple", quest("wuxiang_temple",
                ending("desireless_buddha", "", "mentality+15", "charisma-5",
                        "worldmap:condition:wuxiang_temple:PROSPEROUS"),
                ending("desires_return", "", "morality+12", "mentality-4",
                        "worldmap:condition:wuxiang_temple:NORMAL"),
                ending("all_desires_in_heart", "", "comprehension+15", "mentality-15",
                        "worldmap:condition:wuxiang_temple:HAUNTED"),
                ending("formless_temple", "", "morality-8",
                        "worldmap:condition:wuxiang_temple:ABANDONED"),
                ending("all_beings_have_form",
                        "mentality>=85,morality>=50",
                        "mentality+12", "reputation+30", "morality+15",
                        "worldmap:condition:wuxiang_temple:PROSPEROUS",
                        "worldmap:flag:heart_demon_treatment")));
        result.put("jiuyou_ferry", quest("jiuyou_ferry",
                ending("living_return_to_shore", "", "lifespan-8", "friend:shen_nian",
                        "worldmap:reputation:jiuyou_ferry:12"),
                ending("name_in_the_ledger", "", "morality-8", "wealth+600",
                        "worldmap:reputation:jiuyou_ferry:8"),
                ending("seize_lifespan_return_life", "", "morality+18", "reputation+20",
                        "worldmap:condition:jiuyou_ferry:PROSPEROUS"),
                ending("new_ferryman", "", "reputation+15",
                        "worldmap:faction:jiuyou_ferry:player_ferryman",
                        "worldmap:flag:ferryman_authority"),
                ending("rewrite_death_dates",
                        "morality>=60,comprehension>=85",
                        "lifespan+15", "reputation+30",
                        "worldmap:teleport:jiuyou_ferry:true",
                        "worldmap:flag:safe_underworld_travel")));
        result.put("duanjie_city", quest("duanjie_city",
                ending("guard_boundary_forever", "", "reputation+25",
                        "worldmap:condition:duanjie_city:SEALED"),
                ending("boundary_gate_opens", "", "combat+15", "reputation+30",
                        "worldmap:condition:duanjie_city:PROSPEROUS",
                        "worldmap:flag:outer_map_unlocked"),
                ending("one_city_for_world", "", "morality-20", "reputation+20",
                        "worldmap:condition:duanjie_city:NORMAL",
                        "worldmap:flag:outer_map_unlocked"),
                ending("world_devourer_pact", "", "comprehension+15", "morality-15",
                        "worldmap:teleport:duanjie_city:true",
                        "worldmap:flag:devourer_contract"),
                ending("city_walks_between_realms",
                        "comprehension>=95,reputation>=70",
                        "comprehension+15", "reputation+35", "morality+15",
                        "worldmap:condition:duanjie_city:MOVING",
                        "worldmap:flag:outer_map_unlocked")));
        return Map.copyOf(result);
    }

    private static QuestDefinition quest(
            String locationId, EndingDefinition... endings) {
        return new QuestDefinition(locationId, List.of(endings));
    }

    private static EndingDefinition ending(
            String id, String condition, String... effects) {
        String normalizedCondition = condition;
        if (condition.contains(",")) {
            // Conditions joined with commas are handled as a conjunction below.
            normalizedCondition = "compound:" + condition;
        }
        return new EndingDefinition(id, normalizedCondition, List.of(effects));
    }

    private static boolean endingAvailable(
            CompoundTag life, EndingDefinition ending) {
        String condition = ending.condition();
        if (condition.isBlank()) {
            return true;
        }
        if (!condition.startsWith("compound:")) {
            return LifeSimulationSystem.matchesCondition(life, condition);
        }
        for (String part : condition.substring(9).split(",")) {
            if (!LifeSimulationSystem.matchesCondition(life, part)) {
                return false;
            }
        }
        return true;
    }

    public record LocationDefinition(
            String id,
            String region,
            String terrain,
            String defaultFaction,
            int mapX,
            int mapY,
            int minimumRealm,
            int maximumRealm,
            int spiritualDensity,
            int baseDanger,
            boolean flyingShipPort,
            boolean teleportation,
            boolean hidden) {
        public String nameKey() {
            return "screen.immortal_cultivation_mod.life.map.location." + id;
        }

        public String typeKey() {
            return nameKey() + ".type";
        }

        public String traitsKey() {
            return nameKey() + ".traits";
        }

        public String rumorKey() {
            return nameKey() + ".rumor";
        }
    }

    public record RouteDefinition(
            String id,
            String first,
            String second,
            int actionCost,
            int danger,
            boolean tradeRoute) {
        public boolean touches(String location) {
            return first.equals(location) || second.equals(location);
        }

        public String other(String location) {
            return first.equals(location) ? second : first;
        }

        public boolean connects(String one, String two) {
            return (first.equals(one) && second.equals(two))
                    || (first.equals(two) && second.equals(one));
        }
    }

    public record TravelMethodDefinition(
            String id, int speed, int dangerModifier) {
        public String nameKey() {
            return "screen.immortal_cultivation_mod.life.map.method." + id;
        }
    }

    public record TravelPreview(
            boolean reachable,
            boolean available,
            int baseDistance,
            int actionPointCost,
            int danger,
            long wealthCost,
            int spiritStoneCost,
            int qiCost,
            boolean tradeRoute) {
        private static TravelPreview unavailable() {
            return new TravelPreview(false, false, 0, 0, 0, 0L, 0, 0, false);
        }
    }

    private record PathResult(
            boolean reachable, int cost, int danger, boolean tradeRoute) {
        private static PathResult unreachable() {
            return new PathResult(false, 0, 0, false);
        }
    }

    private record QuestDefinition(
            String locationId, List<EndingDefinition> endings) {
    }

    private record EndingDefinition(
            String id, String condition, List<String> effects) {
    }
}
