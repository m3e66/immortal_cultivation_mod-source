package com.example.immortal_cultivation_mod.system;

import java.util.HashSet;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_life_simulation")
@PrefixGameTestTemplate(false)
public final class LifeSimulationSpriteGameTests {
    private LifeSimulationSpriteGameTests() {}

    private static CompoundTag life() {
        CompoundTag life = new CompoundTag();
        life.putString("Name", "Sprite test cultivator");
        life.putString("Gender", "female");
        life.putUUID("SimulationId", new UUID(1, 2));
        life.putLong("Seed", 42);
        life.putInt("Realm", 4); life.putInt("Age", 30); life.putInt("Health", 100);
        return life;
    }

    @GameTest(template = "fate_test")
    public static void journalAppearanceFollowsNpcAcrossBattleSides(GameTestHelper helper) {
        CompoundTag life = life();
        CompoundTag npc = new CompoundTag();
        npc.putUUID("Id", new UUID(3, 4)); npc.putString("Name", "Healer");
        npc.putString("Occupation", "physician"); npc.putInt("Realm", 4); npc.putInt("Age", 30);
        npc.putBoolean("Alive", true); npc.putBoolean("Friend", true);
        npc.putInt("Affinity", 60); npc.putInt("Trust", 60);
        ListTag people = new ListTag(); people.add(npc); life.put("Relationships", people);
        String portrait = LifeSimulationSprites.person(npc).id();
        LifeSimulationBattle.start(life, new CompoundTag(), RandomSource.create(1));
        CompoundTag ally = life.getCompound("Battle").getList("Actors", Tag.TAG_COMPOUND).getCompound(1);
        helper.assertTrue(ally.getString("Sprite").equals(portrait), "Battle ally uses journal portrait");
        npc.putString("Occupation", "merchant"); npc.putInt("Realm", 7);
        helper.assertTrue(LifeSimulationSprites.person(npc).id().equals(portrait), "Saved face survives occupation and realm changes");
        life.remove("Battle");
        CompoundTag cast = new CompoundTag(); cast.putUUID("lead", npc.getUUID("Id"));
        CompoundTag story = new CompoundTag(); story.put("Cast", cast); life.put("Story", story);
        CompoundTag enemy = new CompoundTag(); enemy.putString("cast_role", "lead");
        enemy.putString("sprite", "cultivator_front"); enemy.putString("profile", "defensive");
        ListTag enemies = new ListTag(); enemies.add(enemy);
        CompoundTag definition = new CompoundTag(); definition.put("enemies", enemies);
        LifeSimulationBattle.start(life, definition, RandomSource.create(1));
        CompoundTag opponent = life.getCompound("Battle").getList("Actors", Tag.TAG_COMPOUND).getCompound(1);
        helper.assertTrue(opponent.getBoolean("Enemy") && opponent.getString("Sprite").equals(portrait), "Same NPC keeps face after switching sides");
        helper.assertTrue(LifeSimulationSprites.actor(life, opponent).pixels() == 90, "Battle uses entire 90 pixel texture");
        helper.assertTrue(life.getLong("Seed") == 42 && npc.getInt("Realm") == 7, "Art selection does not change gameplay values");
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void legacyBattleMigrationPreservesCombatAndIsIdempotent(GameTestHelper helper) {
        CompoundTag life = life();
        CompoundTag actor = new CompoundTag(); actor.putString("Id", "player");
        actor.putString("Sprite", "player_front"); actor.putDouble("Hp", 53.25);
        ListTag actors = new ListTag(); actors.add(actor);
        CompoundTag beast = new CompoundTag(); beast.putString("Id", "enemy1");
        beast.putString("Name", "Spirit beast"); beast.putString("Sprite", "beast_front"); actors.add(beast);
        CompoundTag anchor = new CompoundTag(); anchor.putString("Kind", "anchor");
        anchor.putString("Sprite", "storm_front"); actors.add(anchor);
        CompoundTag battle = new CompoundTag(); battle.putUUID("Id", new UUID(5, 6));
        battle.putInt("Revision", 8); battle.putInt("Reveal", 2); battle.put("Actors", actors);
        life.put("Battle", battle);
        LifeSimulationSprites.normalize(life);
        helper.assertTrue(actor.getString("Sprite").startsWith("h") && beast.getString("Sprite").startsWith("n"), "Old human and beast IDs migrate to new art");
        helper.assertTrue(actor.getDouble("Hp") == 53.25 && battle.getInt("Revision") == 8
                && battle.getInt("Reveal") == 2 && battle.getUUID("Id").equals(new UUID(5, 6)), "Migration preserves unfinished combat");
        helper.assertTrue(anchor.getString("Sprite").equals("storm_front"), "Migration leaves goal anchors intact");
        CompoundTag saved = life.copy(); LifeSimulationSprites.normalize(life);
        helper.assertTrue(saved.equals(life), "Repeated load preserves appearances and combat state");
        actor.putString("Id", "unknown"); actor.putString("Sprite", "../../missing-texture");
        helper.assertTrue(LifeSimulationSprites.actor(life, actor).pixels() == 90, "Invalid saved IDs use a real catalog texture");
        actor.putString("Sprite", "storm_front");
        helper.assertTrue(LifeSimulationSprites.actor(life, actor).pixels() == 64, "Weather retains correct legacy texture dimensions");
        CompoundTag memorials = new CompoundTag();
        for (String kind : java.util.List.of("memory", "remnant", "spirit", "hazard")) {
            CompoundTag retired = new CompoundTag();
            retired.putUUID("Id", new UUID(19, kind.hashCode()));
            retired.putString("Kind", kind); retired.putString("PortraitSprite", "spirit_front");
            helper.assertTrue(LifeSimulationSprites.HUMANS.contains(LifeSimulationSprites.person(retired).id()), "Retired spirit portrait resolves to current character art for " + kind);
            memorials.put(kind, retired);
        }
        life.put("NpcMemorials", memorials);
        LifeSimulationSprites.normalize(life);
        for (String key : memorials.getAllKeys()) helper.assertTrue(LifeSimulationSprites.HUMANS.contains(
                memorials.getCompound(key).getString("PortraitSprite")), "Saved memorial portrait upgrades permanently");
        CompoundTag upgraded = life.copy(); LifeSimulationSprites.normalize(life);
        helper.assertTrue(upgraded.equals(life), "Retired sprite upgrade is idempotent");
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void storyEnemiesAndSavedAnimationsUseNewCharacters(GameTestHelper helper) {
        CompoundTag life = life();
        CompoundTag definition = new CompoundTag();
        ListTag enemies = new ListTag();
        for (String sprite : java.util.List.of("cultivator_front", "healer_front", "spirit_front", "beast_front", "storm_front")) {
            CompoundTag enemy = new CompoundTag();
            enemy.putString("sprite", sprite); enemy.putString("name_key", sprite);
            enemy.putInt("realm", 4); enemy.putString("profile", "defensive"); enemies.add(enemy);
        }
        definition.put("enemies", enemies);
        LifeSimulationBattle.start(life, definition, RandomSource.create(7));
        CompoundTag battle = life.getCompound("Battle");
        ListTag actors = battle.getList("Actors", Tag.TAG_COMPOUND);
        for (int i = 1; i <= 3; i++) helper.assertTrue(LifeSimulationSprites.HUMANS.contains(actors.getCompound(i).getString("Sprite")), "Human and spectral story enemies use the new twenty-character set");
        helper.assertTrue(LifeSimulationSprites.CREATURES.contains(actors.getCompound(4).getString("Sprite")), "Beasts retain creature art");
        helper.assertTrue(actors.getCompound(5).getString("Sprite").equals("storm_front"), "Weather retains storm art");

        CompoundTag shadow = actors.getCompound(3);
        shadow.putString("Sprite", "spirit_front"); shadow.putString("Profile", "inner_shadow");
        helper.assertTrue(LifeSimulationSprites.actor(life, shadow).id().equals(LifeSimulationSprites.person(life).id()), "Inner shadow reflects simulated character");
        ListTag start = actors.copy();
        ListTag finalActors = actors.copy();
        start.getCompound(1).putString("Sprite", "cultivator_front");
        finalActors.getCompound(1).putString("Sprite", "spirit_front");
        finalActors.getCompound(1).putDouble("Hp", 12.5);
        CompoundTag frame = new CompoundTag(); frame.put("Actors", finalActors);
        frame.putString("Outcome", "hit"); frame.putInt("Total", 17);
        ListTag frames = new ListTag(); frames.add(frame);
        battle.put("RoundStartActors", start); battle.put("Frames", frames);
        CompoundTag before = battle.copy();
        LifeSimulationSprites.normalize(life);
        String expected = actors.getCompound(1).getString("Sprite");
        helper.assertTrue(start.getCompound(1).getString("Sprite").equals(expected)
                && finalActors.getCompound(1).getString("Sprite").equals(expected), "Saved round and action frames share current enemy appearance");
        // Removing only art fields leaves every committed value identical.
        CompoundTag after = battle.copy();
        for (CompoundTag copy : java.util.List.of(before, after)) {
            for (var tag : copy.getList("Actors", Tag.TAG_COMPOUND)) ((CompoundTag) tag).remove("Sprite");
            for (var tag : copy.getList("RoundStartActors", Tag.TAG_COMPOUND)) ((CompoundTag) tag).remove("Sprite");
            for (var savedFrame : copy.getList("Frames", Tag.TAG_COMPOUND))
                for (var tag : ((CompoundTag) savedFrame).getList("Actors", Tag.TAG_COMPOUND)) ((CompoundTag) tag).remove("Sprite");
        }
        helper.assertTrue(before.equals(after), "Appearance upgrade preserves all combat results and frame progress");
        CompoundTag saved = life.copy(); LifeSimulationSprites.normalize(life);
        helper.assertTrue(saved.equals(life), "Appearance migration is idempotent");
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void allThirtyNewAppearancesAreReachable(GameTestHelper helper) {
        var humans = new HashSet<String>(); var creatures = new HashSet<String>();
        for (int index = 0; index < 4096; index++) {
            CompoundTag person = new CompoundTag(); person.putUUID("Id", new UUID(7, index));
            humans.add(LifeSimulationSprites.person(person).id());
            person.putString("Kind", "beast"); creatures.add(LifeSimulationSprites.person(person).id());
        }
        helper.assertTrue(humans.equals(new HashSet<>(LifeSimulationSprites.HUMANS)), "All twenty humans can be selected");
        helper.assertTrue(creatures.equals(new HashSet<>(LifeSimulationSprites.CREATURES)), "All ten creatures can be selected");
        helper.succeed();
    }
}
