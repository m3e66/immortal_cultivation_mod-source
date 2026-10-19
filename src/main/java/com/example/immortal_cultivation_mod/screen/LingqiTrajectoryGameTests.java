package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_learning_sessions")
@PrefixGameTestTemplate(false)
public final class LingqiTrajectoryGameTests {
    private static final List<String> REALMS = List.of(CultivationLevels.REALM_MORTAL,
            CultivationLevels.REALM_LIANQI, CultivationLevels.REALM_ZHUJI, CultivationLevels.REALM_JINDAN,
            CultivationLevels.REALM_YUANYING, CultivationLevels.REALM_HUASHEN, CultivationLevels.REALM_LIANXU,
            CultivationLevels.REALM_HETI, CultivationLevels.REALM_DACHENG, CultivationLevels.REALM_DUJIE);

    private static String level(int rank) {
        return rank == 0 ? REALMS.get(rank) : REALMS.get(rank) + CultivationLevels.STAGE_EARLY;
    }

    private static ModSpells.SpellDef highTechnique() {
        return ModSpells.all().stream().filter(spell -> CultivationLevels.getRealmIndex(spell.requiredLevel()) >= 5
                || (spell.requiredLevel().isBlank() && List.of("heaven", "immortal", "divine").contains(spell.tier())))
                .findFirst().orElseThrow();
    }

    @GameTest(template = "fate_test")
    public static void realmGapDeterminesSpellAndMethodDifficulty(GameTestHelper h) {
        h.assertTrue(LingqiTrajectoryConfig.learningPuzzleRealmRank(level(3),level(3))==2,
                "Golden Core learning Golden Core gets Foundation puzzle");
        h.assertTrue(LingqiTrajectoryConfig.learningPuzzleRealmRank(level(3),level(5))==4,
                "Golden Core learning Spirit Transformation gets Nascent Soul puzzle");
        for(int player=0;player<=9;player++)for(int content=1;content<=9;content++) {
            int expected=Math.max(1,Math.min(9,2+content-player));
            h.assertTrue(LingqiTrajectoryConfig.learningPuzzleRealmRank(level(player),level(content))==expected,
                    "Major realm gap alone determines tier, player="+player+", content="+content);
        }
        h.assertTrue(LingqiTrajectoryConfig.learningPuzzleRealmRank(null,level(7))==1,"Missing learner uses safe beginner fallback");
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void sameRealmSpellsUseFoundationMechanicsAndKeepElements(GameTestHelper h) {
        var spell=ModSpells.all().stream().filter(value -> CultivationLevels.getRealmIndex(value.requiredLevel())==3).findFirst().orElseThrow();
        var generated=LingqiTrajectoryConfig.generate("spell",spell.id(),5000,1,1,false,level(3),42);
        var config=generated.config();
        h.assertTrue(config.realm().equals(CultivationLevels.REALM_ZHUJI)&&config.width()==6,
                "Same-realm spell uses real Foundation board");
        h.assertTrue(config.timedRequirements().isEmpty()&&config.movingHazards().isEmpty()&&config.branch()==null,
                "No higher-realm mechanics leak into Foundation puzzle");
        h.assertTrue(config.requiredElements().stream().allMatch(element -> List.of(spell.element().split(",")).contains(element)),
                "Realm scaling preserves spell elements");
        h.assertTrue(LingqiTrajectoryConfig.hasGuaranteedRoute(config,generated.route()),"Scaled puzzle retains executable solution");
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void everyCappedLearningProfileStillHasExecutableSolution(GameTestHelper h) {
        int checked = 0;
        for (String spell : List.of(ModSpells.FIREBALL, ModSpells.WULEI_ZHENGFA, highTechnique().id()))
            for (int realm = 0; realm <= 9; realm++) for (float talent : new float[]{0.15F, 0.65F, 1})
                for (boolean assisted : new boolean[]{false, true}) for (long seed : new long[]{0, 17, 2489}) {
                    int points = Math.round(5_000 / talent);
                    var generated = LingqiTrajectoryConfig.generate("spell", spell, points, 1, talent, assisted, level(realm), seed);
                    h.assertTrue(LingqiTrajectoryConfig.hasGuaranteedRoute(generated.config(), generated.route()),
                            "Capped route can complete within step, stability, strength, gate, timer and hazard rules: "
                                    + spell + " learner=" + realm + " talent=" + talent + " assisted=" + assisted + " seed=" + seed);
                    h.assertTrue(generated.config().width() <= 9 && generated.config().height() <= 9,
                            "Puzzle remains inside existing board layout");
                    checked++;
                }
        h.assertTrue(checked == 540, "All 540 capped realm/talent/assistance/seed combinations checked");
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void lowerTierSpellDoesNotBecomeHarderForAdvancedPlayer(GameTestHelper h) {
        for (String spell : List.of(ModSpells.FIREBALL, ModSpells.YINLEI_JUE, ModSpells.WULEI_ZHENGFA)) {
            var early = LingqiTrajectoryConfig.create("spell", spell, 5_000, 1, 1, false, level(2), 17);
            var advanced = LingqiTrajectoryConfig.create("spell", spell, 5_000, 1, 1, false, level(9), 17);
            h.assertTrue(REALMS.indexOf(advanced.realm())<=REALMS.indexOf(early.realm()), "Advancing never makes a lower-tier spell harder: " + spell);
        }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void everyRegisteredSpellAndMethodUsesRealmGapForEveryLearner(GameTestHelper h) {
        int checked=0;
        for(var spell:ModSpells.all()) {
            String required=spell.requiredLevel();
            int content;
            if(required==null||required.isBlank()) {
                content=switch(spell.tier()) {
                    case "earth" -> 3;
                    case "heaven" -> 5;
                    case "immortal", "divine" -> 6;
                    default -> 1;
                };
            }else content=Math.max(1,CultivationLevels.getRealmIndex(required));
            for(int player=0;player<=9;player++) {
                var generated=LingqiTrajectoryConfig.generate("spell",spell.id(),5000,1,1,false,level(player),17);
                int expected=Math.max(1,Math.min(9,2+content-player));
                h.assertTrue(generated.config().realm().equals(REALMS.get(expected)),
                        "Every spell follows realm gap: "+spell.id()+", learner="+player);
                h.assertTrue(LingqiTrajectoryConfig.hasGuaranteedRoute(generated.config(),generated.route()),
                        "Registered spell remains solvable: "+spell.id()+", learner="+player);
                checked++;
            }
        }
        for(var method:CultivationMethods.allMethods())for(int player=0;player<=9;player++) {
            var generated=LingqiTrajectoryConfig.generate("method",method.id(),5000,1,1,false,level(player),17);
            int content=Math.max(1,CultivationLevels.getRealmIndex(method.limitLevel()));
            int expected=LingqiTrajectoryConfig.isStarterMethod(method.id()) ? 1 : Math.max(1,Math.min(9,2+content-player));
            h.assertTrue(generated.config().realm().equals(REALMS.get(expected)),
                    "Every method follows realm gap: "+method.id()+", learner="+player);
            h.assertTrue(LingqiTrajectoryConfig.hasGuaranteedRoute(generated.config(),generated.route()),
                    "Registered method remains solvable: "+method.id()+", learner="+player);
            checked++;
        }
        h.assertTrue(checked==(ModSpells.all().size()+CultivationMethods.allMethods().size())*10,
                "Entire spell and method catalog checked across all ten learner realms");
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void cultivationMethodsUseSameRealmGapRules(GameTestHelper h) {
        for (var method : CultivationMethods.allMethods()) {
            var early = LingqiTrajectoryConfig.create("method", method.id(), 5_000, 1, 1, false, level(0), 17);
            var advanced = LingqiTrajectoryConfig.create("method", method.id(), 5_000, 1, 1, false, level(9), 17);
            int expected=LingqiTrajectoryConfig.isStarterMethod(method.id()) ? 1 : LingqiTrajectoryConfig.learningPuzzleRealmRank(level(0),method.limitLevel());
            h.assertTrue(early.realm().equals(REALMS.get(expected)), "Method uses relative tier: " + method.id());
            h.assertTrue(advanced.realm().equals(CultivationLevels.REALM_LIANQI), "High-realm learner gets beginner method puzzle: " + method.id());
            int content=CultivationLevels.getRealmIndex(method.limitLevel());
            if(content>0 && !LingqiTrajectoryConfig.isStarterMethod(method.id())){
                var same=LingqiTrajectoryConfig.generate("method",method.id(),5000,1,1,false,level(content),17);
                h.assertTrue(same.config().realm().equals(CultivationLevels.REALM_ZHUJI)&&same.config().width()==6, "Same-realm method uses Foundation rules: "+method.id());
                h.assertTrue(LingqiTrajectoryConfig.hasGuaranteedRoute(same.config(),same.route()),"Method has executable solution");
                if(content>=3){
                    var higher=LingqiTrajectoryConfig.generate("method",method.id(),5000,1,1,false,level(content-2),17);
                    h.assertTrue(higher.config().realm().equals(CultivationLevels.REALM_YUANYING)&&higher.config().width()==8,"Two-realm method gap uses Nascent Soul layout: "+method.id());
                    h.assertTrue(LingqiTrajectoryConfig.hasGuaranteedRoute(higher.config(),higher.route()),"Two-realm method remains solvable");
                }
            }
        }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void starterMethodsStaySimpleEvenWithLowTalent(GameTestHelper h) {
        for(String method:List.of(CultivationMethods.BASIC_BREATHING,CultivationMethods.CLEAR_HEART))
            for(int learner=0;learner<=9;learner++)for(float talent:new float[]{.15F,1F})
                for(long seed:new long[]{0,17,2489}) {
                    var generated=LingqiTrajectoryConfig.generate("method",method,Math.round(5000/talent),1,talent,false,level(learner),seed);
                    var config=generated.config();
                    h.assertTrue(config.realm().equals(CultivationLevels.REALM_LIANQI)&&config.width()==5&&config.height()==5,
                            "Both starter methods always use small beginner boards");
                    h.assertTrue(config.idealSteps()==10&&config.maximumSteps()>=16&&config.startingStability()==100,
                            "Short route, six spare moves and full stability remain available at low talent");
                    h.assertTrue(config.directionalEdges().isEmpty()&&config.collapsingEdges().isEmpty()
                                    &&config.timedRequirements().isEmpty()&&config.movingHazards().isEmpty()
                                    &&config.linkedPairs().isEmpty()&&config.switchGates().isEmpty()&&config.branch()==null,
                            "Starter learning has no advanced puzzle mechanisms");
                    h.assertTrue(LingqiTrajectoryConfig.hasGuaranteedRoute(config,generated.route()),"Beginner puzzle remains solvable");
                }
        h.succeed();
    }
}
