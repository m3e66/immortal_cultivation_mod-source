package com.example.immortal_cultivation_mod.system;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

/** Shared portrait/battle appearances. Choosing art never consumes simulation randomness. */
public final class LifeSimulationSprites {
    public static final List<String> HUMANS = List.of(
            "h01_green_sword_cultivator", "h02_white_sword_cultivator", "h03_lavender_scholar",
            "h04_rust_swordswoman", "h05_herbal_alchemist", "h06_ochre_smith", "h07_teal_merchant",
            "h08_wandering_daoist", "h09_teal_senior_disciple", "h10_navy_disciple", "h11_guardian",
            "h12_archer", "h13_ice_cultivator", "h14_fire_cultivator", "h15_monk", "h16_musician",
            "h17_hermit", "h18_mountain_healer", "h19_spear_cultivator", "h20_elder");
    public static final List<String> CREATURES = List.of(
            "n01_white_fox", "n02_jade_deer", "n03_crimson_crane", "n04_black_tortoise",
            "n05_stone_bear", "n06_jade_mantis", "n07_crimson_lizard", "n08_violet_owl",
            "n09_golden_ape", "n10_azure_jiao");
    public static final List<String> MALE = List.of(HUMANS.get(0), HUMANS.get(2), HUMANS.get(5),
            HUMANS.get(6), HUMANS.get(7), HUMANS.get(9), HUMANS.get(10), HUMANS.get(14),
            HUMANS.get(16), HUMANS.get(18), HUMANS.get(19));
    public static final List<String> FEMALE = List.of(HUMANS.get(1), HUMANS.get(3), HUMANS.get(4),
            HUMANS.get(8), HUMANS.get(11), HUMANS.get(12), HUMANS.get(13), HUMANS.get(15), HUMANS.get(17));
    private static final Map<String, Sprite> CATALOG = catalog();
    private static final String PORTRAIT = "PortraitSprite";

    public record Sprite(String id, int pixels, boolean facesRight, ResourceLocation texture) {}

    private LifeSimulationSprites() {}

    private static Map<String, Sprite> catalog() {
        Map<String, Sprite> catalog = new LinkedHashMap<>();
        for (String id : HUMANS) add(catalog, id, 90);
        for (String id : CREATURES) add(catalog, id, 90);
        // Weather retains its own effect; all humanoid spirits use the current character set.
        add(catalog, "storm_front", 64);
        return Map.copyOf(catalog);
    }

    private static void add(Map<String, Sprite> catalog, String id, int pixels) {
        boolean facesRight = List.of("n02_jade_deer", "n03_crimson_crane", "n04_black_tortoise",
                "n05_stone_bear", "n08_violet_owl", "n09_golden_ape").contains(id);
        catalog.put(id, new Sprite(id, pixels, facesRight, ResourceLocation.fromNamespaceAndPath(
                "immortal_cultivation_mod", "textures/gui/life_battle/" + id + ".png")));
    }

    private static String identity(CompoundTag person) {
        if (person.hasUUID("Id")) return person.getUUID("Id").toString();
        if (person.hasUUID("SimulationId")) return person.getUUID("SimulationId").toString();
        return person.getString("NameKey") + ":" + person.getString("Name") + ":" + person.getLong("Seed");
    }

    private static Sprite pick(List<String> choices, String identity) {
        return CATALOG.get(choices.get(Math.floorMod(identity.hashCode(), choices.size())));
    }

    /** Explicit identity and family roles take priority over an old mismatched portrait. */
    public static String gender(CompoundTag person) {
        String value=person.getString("Gender");
        if(value.equals("female")||value.equals("male"))return value;
        value=person.getString("gender");
        if(value.equals("female")||value.equals("male"))return value;
        switch(person.getString("Relation")) {
            case "mother", "wife", "daughter", "sister" -> {return "female";}
            case "father", "husband", "son", "brother" -> {return "male";}
        }
        String saved=person.getString(PORTRAIT);
        if(saved.isBlank())saved=person.getString("Sprite");
        return FEMALE.contains(saved)?"female":MALE.contains(saved)?"male":Math.floorMod(identity(person).hashCode(),2)==0?"female":"male";
    }

    private static List<String> genderPool(String gender) {
        return gender.equals("female")?FEMALE:gender.equals("male")?MALE:HUMANS;
    }

    /** Read-only resolution also supports old lives which do not have a saved portrait yet. */
    public static Sprite person(CompoundTag person) {
        String key=identity(person),gender=gender(person);
        Sprite saved=CATALOG.get(person.getString(PORTRAIT));
        if(List.of("beast","creature").contains(person.getString("Kind")))
            return saved!=null&&CREATURES.contains(saved.id())?saved:pick(CREATURES,key);
        if(saved!=null&&saved.id().equals("storm_front"))return saved;
        List<String> pool=genderPool(gender);
        if(saved!=null&&pool.contains(saved.id()))return saved;
        List<String> occupation=switch(person.getString("Occupation")) {
            case "physician", "healer" -> List.of(HUMANS.get(4),HUMANS.get(17));
            case "herbalist", "alchemist" -> List.of(HUMANS.get(4));
            case "smith" -> List.of(HUMANS.get(5));
            case "merchant" -> List.of(HUMANS.get(6));
            case "scholar" -> List.of(HUMANS.get(2));
            case "guard", "guardian", "soldier" -> List.of(HUMANS.get(10));
            case "monk" -> List.of(HUMANS.get(14));
            case "musician" -> List.of(HUMANS.get(15));
            case "hermit" -> List.of(HUMANS.get(16));
            case "sect_elder" -> List.of(HUMANS.get(7),HUMANS.get(19));
            default -> pool;
        };
        List<String> suitable=occupation.stream().filter(pool::contains).toList();
        return pick(suitable.isEmpty()?pool:suitable,key);
    }

    /** Freeze the first server-side selection so occupation/realm changes do not swap faces. */
    public static String assignPerson(CompoundTag person) {
        String id = person(person).id();
        person.putString(PORTRAIT, id);
        if(HUMANS.contains(id))person.putString("Gender",FEMALE.contains(id)?"female":"male");
        return id;
    }

    /** Resolves legacy saved battle IDs too; linked NPCs use their journal appearance. */
    public static Sprite actor(CompoundTag life, CompoundTag actor) {
        String requested=actor.getString("Sprite");
        if(actor.getString("Id").equals("player")||actor.getString("Profile").equals("inner_shadow"))return person(life);
        Sprite explicit=CATALOG.get(requested);
        if(explicit!=null&&(CREATURES.contains(explicit.id())||explicit.id().equals("storm_front")))return explicit;
        if(actor.hasUUID("NpcId")) {
            for(var tag:life.getList("Relationships",Tag.TAG_COMPOUND)) {
                CompoundTag npc=(CompoundTag)tag;
                if(npc.hasUUID("Id")&&npc.getUUID("Id").equals(actor.getUUID("NpcId"))) {
                    if(requested.equals("beast_front")&&!List.of("beast","creature").contains(npc.getString("Kind")))
                        return pick(CREATURES,identity(npc));
                    return person(npc);
                }
            }
        }
        List<String> pool=genderPool(gender(actor));
        if(explicit!=null&&pool.contains(explicit.id()))return explicit;
        String key=identity(life)+":"+actor.getString("Name")+":"+actor.getString("Id");
        if(requested.equals("beast_front")||actor.getString("Profile").equals("beast"))return pick(CREATURES,key);
        if(requested.equals("healer_front")) {
            List<String> healers=List.of(HUMANS.get(4),HUMANS.get(17)).stream().filter(pool::contains).toList();
            if(!healers.isEmpty())return pick(healers,key);
        }
        return pick(pool,key);
    }

    /** Server assignment persists the same appearance in battle actors and linked NPCs. */
    public static void assignActor(CompoundTag life, CompoundTag actor, CompoundTag linked) {
        String resolved = actor(life, actor).id();
        if (!linked.isEmpty()) linked.putString(PORTRAIT, resolved);
        actor.putString("Sprite", resolved);
    }

    /** Persist appearances when loading a legacy life, including an unfinished battle. */
    public static void normalize(CompoundTag life) {
        if (life.getString("Name").isBlank()) return;
        assignPerson(life);
        for (var tag : life.getList("Relationships", Tag.TAG_COMPOUND)) assignPerson((CompoundTag) tag);
        CompoundTag memorials = life.getCompound("NpcMemorials");
        for (String id : memorials.getAllKeys()) assignPerson(memorials.getCompound(id));
        CompoundTag battle = life.getCompound("Battle");
        Map<String, String> appearances = new LinkedHashMap<>();
        for (var tag : battle.getList("Actors", Tag.TAG_COMPOUND)) {
            CompoundTag actor = (CompoundTag) tag;
            if (actor.getString("Kind").equals("anchor")) continue;
            CompoundTag linked = new CompoundTag();
            if (actor.hasUUID("NpcId")) {
                for (var npcTag : life.getList("Relationships", Tag.TAG_COMPOUND)) {
                    CompoundTag npc = (CompoundTag) npcTag;
                    if (npc.hasUUID("Id") && npc.getUUID("Id").equals(actor.getUUID("NpcId"))) linked = npc;
                }
            }
            assignActor(life, actor, linked);
            appearances.put(actor.getString("Id"), actor.getString("Sprite"));
        }
        for(var memory:life.getList("Memories",Tag.TAG_COMPOUND))normalize((CompoundTag)memory);
        normalizeSnapshot(life, battle.getList("RoundStartActors", Tag.TAG_COMPOUND), appearances);
        for (var tag : battle.getList("Frames", Tag.TAG_COMPOUND))
            normalizeSnapshot(life, ((CompoundTag) tag).getList("Actors", Tag.TAG_COMPOUND), appearances);
    }

    /** Animation snapshots keep the actor's appearance without changing saved combat results. */
    private static void normalizeSnapshot(CompoundTag life, ListTag actors,
                                          Map<String, String> appearances) {
        for (var tag : actors) {
            CompoundTag actor = (CompoundTag) tag;
            if (actor.getString("Kind").equals("anchor")) continue;
            String appearance = appearances.get(actor.getString("Id"));
            actor.putString("Sprite", appearance != null ? appearance : actor(life, actor).id());
        }
    }
}
