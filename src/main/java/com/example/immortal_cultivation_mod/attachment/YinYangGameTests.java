package com.example.immortal_cultivation_mod.attachment;

import com.example.immortal_cultivation_mod.alchemy.AlchemyChemistry;
import com.example.immortal_cultivation_mod.alchemy.PillData;
import com.example.immortal_cultivation_mod.entity.ModEntities;
import com.example.immortal_cultivation_mod.entity.NpcProfile;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import java.util.List;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_yin_yang")
@PrefixGameTestTemplate(false)
public final class YinYangGameTests {
    private YinYangGameTests() {
    }

    @GameTest(template = "fate_test")
    public static void savedPlayerRootsAndBodyMigrateWithoutLosingProgress(GameTestHelper h) {
        CompoundTag old = new CompoundTag();
        old.putInt("qi", 317);
        old.putLong("cultivationProgress", 81234567890L);
        old.putString("bodyType", "光灵体");
        old.putString("spiritRootGrade", SpiritRoots.GRADE_HEAVEN);
        ListTag roots = new ListTag();
        for (String root : List.of("光", "暗", SpiritRoots.WATER)) roots.add(StringTag.valueOf(root));
        old.put("spiritRoots", roots);
        var data = ModAttachments.CultivationData.CODEC.parse(NbtOps.INSTANCE, old).getOrThrow();
        h.assertTrue(data.spiritRoots().equals(List.of(SpiritRoots.YANG, SpiritRoots.YIN, SpiritRoots.WATER))
                && data.bodyType().equals(BodyTypes.MORTAL_BODY), "Saved roots become Yang/Yin; retired elemental body becomes mortal");
        h.assertTrue(data.qi() == 317 && data.cultivationProgress() == 81234567890L
                && data.spiritRootGrade().equals(SpiritRoots.GRADE_HEAVEN), "Migration preserves resource, progress and grade");
        var saved = ModAttachments.CultivationData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow();
        h.assertTrue(ModAttachments.CultivationData.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow().equals(data),
                "Migrated save round trip cannot reroll or duplicate roots");
        h.assertTrue(BodyTypes.sanitize("暗灵体").equals(BodyTypes.MORTAL_BODY)
                && SpiritRoots.migrateRootList(List.of(SpiritRoots.NONE)).equals(List.of(SpiritRoots.NONE)),
                "Retired Yin body becomes mortal and rootless players remain rootless");
        for (String retired : List.of("金灵体", "木灵体", "水灵体", "火灵体", "土灵体", "雷灵体",
                "风灵体", "冰灵体", "阳灵体", "阴灵体", "光灵体", "暗灵体")) {
            old.putString("bodyType", retired);
            var migrated = ModAttachments.CultivationData.CODEC.parse(NbtOps.INSTANCE, old).getOrThrow();
            h.assertTrue(migrated.equals(data), "Each retired body migrates without changing any other player data: " + retired);
        }
        h.assertTrue(BodyTypes.normalize(BodyTypes.VOID_BODY).equals(BodyTypes.VOID_BODY)
                && BodyTypes.normalize(BodyTypes.OUTER_DEMON_BODY).equals(BodyTypes.OUTER_DEMON_BODY)
                && BodyTypes.normalize("玄灵体").equals("玄灵体"), "Remaining bodies and unrelated NPC physiques retain saved identity");
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void spellAndMethodMatchingKeepsBothAffinities(GameTestHelper h) {
        var yang = ModSpells.get(ModSpells.LIGHT_BEAM_ATTACK);
        var yin = ModSpells.get(ModSpells.ABSORB_CULTIVATION);
        h.assertTrue(yang.element().equals(SpiritRoots.YANG) && yin.element().equals(SpiritRoots.YIN),
                "Spell definitions use new affinities");
        for (var spell : ModSpells.all())
            h.assertTrue(!spell.element().contains("光") && !spell.element().contains("暗"), "Every spell affinity uses new names");
        for (var method : CultivationMethods.allMethods())
            h.assertTrue(!method.element().contains("光") && !method.element().contains("暗"), "Every method affinity uses new names");
        var data = ModAttachments.CultivationData.CODEC.parse(NbtOps.INSTANCE, minimalSave()).getOrThrow()
                .withSpiritRoots(List.of("光", "暗"), SpiritRoots.GRADE_EARTH);
        h.assertTrue(SpiritRoots.damageMultiplier(data, yang) == 2 && SpiritRoots.effectDuration(data, yin, 60) == 120,
                "Renamed roots retain matching spell damage and duration bonuses");
        h.assertTrue(CultivationMethods.get(CultivationMethods.REINCARNATION_TRUE_ART).element().equals(SpiritRoots.YIN)
                && SpiritRoots.elementMatches(CultivationMethods.get(CultivationMethods.DARI_BATI_JUE).element(), SpiritRoots.YANG),
                "Yin methods and mixed Fire/Yang methods match new roots");
        h.assertTrue(SpiritRoots.matchesAny(List.of("光"), SpiritRoots.YANG)
                && SpiritRoots.matchesAny(List.of(SpiritRoots.YIN), "暗")
                && !SpiritRoots.matchesAny(List.of(SpiritRoots.YIN), SpiritRoots.YANG),
                "Legacy aliases work without making opposite roots compatible");
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void savedNpcProfileMigratesAndStaysStable(GameTestHelper h) {
        var npc = ModEntities.LIANQI_CULTIVATOR.get().create(h.getLevel());
        NpcProfile.ensure(npc, NpcProfile.LIANQI);
        var profile = npc.getPersistentData().getCompound("ImmortalCultivationNpc");
        ListTag roots = new ListTag();
        roots.add(StringTag.valueOf("暗"));
        roots.add(StringTag.valueOf("光"));
        profile.put("SpiritRoots", roots);
        profile.putString("BodyType", "暗灵体");
        long progress = profile.getLong("CultivationProgress");
        NpcProfile.ensure(npc, NpcProfile.LIANQI);
        h.assertTrue(NpcProfile.spiritRootIds(npc, NpcProfile.LIANQI).equals(List.of(SpiritRoots.YIN, SpiritRoots.YANG))
                && profile.getString("BodyType").equals(BodyTypes.MORTAL_BODY), "Existing NPC roots migrate and retired body becomes mortal");
        h.assertTrue(profile.getLong("CultivationProgress") == progress, "NPC migration preserves progress");
        NpcProfile.ensure(npc, NpcProfile.LIANQI);
        h.assertTrue(NpcProfile.spiritRootsDisplay(npc, NpcProfile.LIANQI).equals("阴/阳"),
                "Repeated profile reads display new names without rerolling");
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void oldPillElementsKeepPotencyAndNewBatchesUseYinYang(GameTestHelper h) {
        var formula = AlchemyChemistry.formula("qingling");
        var batch = new AlchemyChemistry.Batch("qingling", 1,
                Map.of(AlchemyChemistry.Element.YANG, 8., AlchemyChemistry.Element.YIN, 12.),
                formula.effects(), 70, 0, "common", false);
        var pill = PillData.create(batch);
        var fresh = PillData.raw(pill).getCompound("Elements");
        h.assertTrue(fresh.getDouble("YANG") == 8 && fresh.getDouble("YIN") == 12
                && !fresh.contains("LIGHT") && !fresh.contains("DARK"), "New crafted pills save canonical element names");
        var custom = pill.get(DataComponents.CUSTOM_DATA).copyTag();
        var saved = custom.getCompound(PillData.KEY).getCompound("Elements");
        saved.remove("YANG"); saved.remove("YIN");
        saved.putDouble("LIGHT", 8); saved.putDouble("DARK", 12);
        custom.putString("Unrelated", "preserve");
        pill.set(DataComponents.CUSTOM_DATA, CustomData.of(custom));
        for (int i = 0; i < 3; i++) {
            var read = PillData.raw(pill).getCompound("Elements");
            h.assertTrue(read.getDouble("YANG") == 8 && read.getDouble("YIN") == 12
                    && !read.contains("LIGHT") && !read.contains("DARK"), "Old pills migrate once per copied read without inflating potency");
        }
        h.assertTrue(PillData.effects(pill).equals(formula.effects())
                && pill.get(DataComponents.CUSTOM_DATA).copyTag().getString("Unrelated").equals("preserve"),
                "Effects and unrelated saved item data remain intact");
        h.assertTrue(AlchemyChemistry.conflict(Map.of(AlchemyChemistry.Element.YANG, 20., AlchemyChemistry.Element.YIN, 12.)) == 12,
                "Yang/Yin retain opposing-element alchemy conflict");
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void newRootAndBodyRollsUseOnlyNewNames(GameTestHelper h) {
        var random = RandomSource.create(1928);
        boolean yang = false, yin = false;
        int mortalBodies = 0;
        boolean voidBody = false, outerBody = false;
        for (int i = 0; i < 2000; i++) {
            var roots = SpiritRoots.random(random).roots();
            yang |= roots.contains(SpiritRoots.YANG); yin |= roots.contains(SpiritRoots.YIN);
            h.assertTrue(!roots.contains("光") && !roots.contains("暗"), "New roots never roll old names");
            String body = BodyTypes.random(random);
            h.assertTrue(BodyTypes.VALUES.contains(body), "Every roll uses an available body");
            if (body.equals(BodyTypes.MORTAL_BODY)) mortalBodies++;
            voidBody |= body.equals(BodyTypes.VOID_BODY);
            outerBody |= body.equals(BodyTypes.OUTER_DEMON_BODY);
            h.assertTrue(!body.equals("光灵体") && !body.equals("暗灵体"), "New bodies never roll old names");
        }
        h.assertTrue(yang && yin && SpiritRoots.ALL_ELEMENTS.size() == 10, "Both renamed roots keep their place in generation");
        h.assertTrue(BodyTypes.VALUES.size() == 21
                && mortalBodies > 1600 && mortalBodies < 1980 && voidBody && outerBody,
                "Mortal bodies dominate random rolls while special bodies remain obtainable");
        h.assertTrue(BodyTypes.next("金灵体", 1).equals(BodyTypes.CRIMSON_FLAME)
                && BodyTypes.next(BodyTypes.MORTAL_BODY, -1).equals(BodyTypes.SOLAR_DIVINE),
                "Debug cycling skips every retired elemental body");
        h.assertTrue(SpiritRoots.nextRootSet(List.of("光"), 1).equals(List.of(SpiritRoots.YIN)),
                "Debug root cycling recognizes legacy saves");
        h.succeed();
    }

    private static CompoundTag minimalSave() {
        var tag = new CompoundTag(); tag.putInt("qi", 0); return tag;
    }
}
