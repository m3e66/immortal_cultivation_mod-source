package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.attachment.SpiritRoots;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import com.example.immortal_cultivation_mod.system.SystemShop;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_npc_spell_pool")
@PrefixGameTestTemplate(false)
public final class NpcSpellPoolGameTests {
    @GameTest(template="fate_test")
    public static void probingEndsEvenWhenOpponentRevealsNoNewInformation(GameTestHelper h) {
        var plan=new NpcBattlePlan();plan.begin(java.util.UUID.randomUUID(),0,8,"cautious");
        var situation=new NpcBattlePlan.Situation(1,1,0,0,1,0,1,1,false,true,true,false,false,false,0,false);
        plan.update(situation,30,0);h.assertTrue(plan.probing(),"NPC still assesses the opponent initially");
        h.assertTrue(!plan.canSpend(3500,8000,8000,false),"NPC initially holds back an expensive unrevealed spell");
        plan.update(situation,400,0);h.assertTrue(!plan.probing(),"A quiet opponent cannot permanently lock expensive spells behind probing");
        h.assertTrue(plan.canSpend(3500,8000,8000,false),"Assessed opponent allows a large cast while preserving reserve");
        h.assertTrue(!plan.canSpend(3500,4000,8000,false),"Finishing assessment does not discard minimum Qi reserve");h.succeed();
    }
    @GameTest(template="fate_test")
    public static void largeSpellIsSelectedAndActuallyReleasedAgainstOneWeakerEnemy(GameTestHelper h)throws Exception {
        var npc=ModEntities.JINDAN_CULTIVATOR.get().create(h.getLevel());
        var enemy=net.minecraft.world.entity.EntityType.COW.create(h.getLevel());
        try {
            npc.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(2,30,2)));enemy.setPos(npc.position().add(10,0,0));
            enemy.setNoAi(true);enemy.setNoGravity(true);h.getLevel().addFreshEntity(enemy);
            NpcProfile.ensure(npc,NpcProfile.JINDAN);var p=NpcProfile.cultivationProfile(npc);
            p.putString("Realm",CultivationLevels.REALM_JINDAN+CultivationLevels.STAGE_LATE);
            p.putString("Method",CultivationMethods.BASIC_BREATHING);p.put("Methods",tags(CultivationMethods.BASIC_BREATHING));
            p.put("Spells",tags(ModSpells.FIREBALL,ModSpells.TIANYUN_FENCHENG));p.putInt("MaxQi",8000);p.putInt("Qi",8000);
            npc.setTarget(enemy);var brain=new NpcCombatBrain(npc);
            var field=NpcCombatBrain.class.getDeclaredField("battle");field.setAccessible(true);var plan=(NpcBattlePlan)field.get(brain);
            long now=h.getLevel().getGameTime();plan.begin(enemy.getUUID(),now-1000,8,"cautious");
            plan.update(new NpcBattlePlan.Situation(1,1,0,0,1,0,1,1,false,true,true,false,false,false,0,false),now,0);
            var observe=NpcCombatBrain.class.getDeclaredMethod("observe",net.minecraft.world.entity.LivingEntity.class);observe.setAccessible(true);
            var context=observe.invoke(brain,enemy);
            var select=NpcCombatBrain.class.getDeclaredMethod("selectSpell",context.getClass(),java.util.function.Predicate.class,boolean.class);select.setAccessible(true);
            String chosen=(String)select.invoke(brain,context,(java.util.function.Predicate<String>)NpcSpellcasting::isAttackSpell,false);
            h.assertTrue(chosen.equals(ModSpells.TIANYUN_FENCHENG),"Healthy, prepared NPC selects learned meteor spell over cheap fireball in a duel: "+chosen);
            var action=java.util.Arrays.stream(NpcCombatBrain.class.getDeclaredClasses()).filter(c->c.getSimpleName().equals("ActionKind")).findFirst().orElseThrow();
            Object attack=java.util.Arrays.stream(action.getEnumConstants()).filter(e->e.toString().equals("SPELL_ATTACK")).findFirst().orElseThrow();
            var cast=NpcCombatBrain.class.getDeclaredMethod("tryCast",net.minecraft.world.entity.LivingEntity.class,String.class,action);cast.setAccessible(true);
            h.assertTrue((boolean)cast.invoke(brain,enemy,chosen,attack),"Large spell enters real windup");
            var release=NpcCombatBrain.class.getDeclaredMethod("tickPendingCast",long.class);release.setAccessible(true);release.invoke(brain,now+30);
            h.assertTrue(NpcProfile.qi(npc,NpcProfile.JINDAN)<=8000-NpcSpellcasting.qiCost(npc,chosen),"Prepared spell actually executes and pays real Qi against a weaker single target");
            h.assertTrue(NpcCombatKnowledge.wasRevealed(npc,chosen),"Completed large cast is recorded as revealed");
        }finally{npc.discard();enemy.discard();}h.succeed();
    }
    private static ListTag tags(String... values) {
        ListTag result=new ListTag();for(String value:values)result.add(StringTag.valueOf(value));return result;
    }
    @SuppressWarnings("unchecked")
    private static List<String> generate(String realm,int seed)throws Exception {
        Method method=NpcProfile.class.getDeclaredMethod("selectRandomSpells",String.class,String.class,List.class,String.class,String.class,int.class);
        method.setAccessible(true);
        return (List<String>)method.invoke(null,NpcProfile.JINDAN,realm,List.of(SpiritRoots.THUNDER,SpiritRoots.WIND),
                "wandering_cultivator",CultivationMethods.BASIC_BREATHING,seed);
    }

    @GameTest(template="fate_test",timeoutTicks=600)
    public static void ordinaryPoolIsVariedAndContainsHighTierSpellsWithoutMethodArts(GameTestHelper h)throws Exception {
        Set<String> spells=new HashSet<>(),loadouts=new HashSet<>();
        String realm=CultivationLevels.REALM_JINDAN+CultivationLevels.STAGE_LATE;
        for(int seed=0;seed<256;seed++) {
            var generated=generate(realm,seed);spells.addAll(generated);loadouts.add(generated.toString());
            h.assertTrue(generated.equals(generate(realm,seed)),"Same NPC seed preserves its loadout");
            for(String id:generated)h.assertTrue(ModSpells.isRandomRewardEligible(id)
                    &&NpcSpellcasting.isNpcSupportedSpell(id)&&ModSpells.meetsRequirement(realm,ModSpells.get(id)),
                    "Random slots contain only realm-appropriate supported ordinary spells: "+id);
        }
        h.assertTrue(loadouts.size()>200&&spells.size()>30,"NPC seeds generate many distinct real spell sets");
        h.assertTrue(spells.containsAll(List.of(ModSpells.TIANYUN_FENCHENG,ModSpells.WULEI_HONGDING,ModSpells.WANREN_CHUANXIN)),
                "Higher-tier fire, thunder and metal spells remain obtainable on eligible NPCs");
        h.assertTrue(!spells.contains(ModSpells.SHANSUO_ZHENYU)&&!spells.contains(ModSpells.QINGTENG_FU),
                "Xuanyue and Changqing techniques no longer consume unrelated NPC random slots");
        h.succeed();
    }

    @GameTest(template="fate_test")
    public static void lowRealmsCannotRandomlyLearnHigherRealmSpells(GameTestHelper h)throws Exception {
        String realm=CultivationLevels.REALM_LIANQI+CultivationLevels.STAGE_EARLY;
        for(int seed=0;seed<48;seed++)for(String id:generate(realm,seed))
            h.assertTrue(ModSpells.meetsRequirement(realm,ModSpells.get(id)),"Realm gates survive loadout correction");
        h.succeed();
    }

    @GameTest(template="fate_test")
    public static void savedNpcDropsOrphanArtsButKeepsOrdinaryLearnedSpells(GameTestHelper h) {
        var npc=ModEntities.JINDAN_CULTIVATOR.get().create(h.getLevel());NpcProfile.ensure(npc,NpcProfile.JINDAN);
        var profile=NpcProfile.cultivationProfile(npc);
        profile.putString("Method",CultivationMethods.BASIC_BREATHING);profile.put("Methods",tags(CultivationMethods.BASIC_BREATHING));
        profile.put("Spells",tags(ModSpells.FIREBALL,ModSpells.TIANYUN_FENCHENG,ModSpells.SHANSUO_ZHENYU,ModSpells.QINGTENG_FU,ModSpells.WANMU_SHENG));
        String name=profile.getString("Name");var methods=profile.get("Methods").copy();
        NpcProfile.ensure(npc,NpcProfile.JINDAN);
        var known=NpcProfile.learnedSpellIds(npc,NpcProfile.JINDAN);
        h.assertTrue(known.equals(List.of(ModSpells.FIREBALL,ModSpells.TIANYUN_FENCHENG)),"Existing NPCs lose only unsupported method ownership, retaining ordinary learned spells");
        h.assertTrue(profile.getString("Name").equals(name)&&profile.get("Methods").equals(methods),"Repair does not reroll NPC identity or known methods");
        NpcProfile.ensure(npc,NpcProfile.JINDAN);
        h.assertTrue(known.equals(NpcProfile.learnedSpellIds(npc,NpcProfile.JINDAN)),"Repair is idempotent");npc.discard();h.succeed();
    }

    @GameTest(template="fate_test")
    public static void actualKnownMethodsKeepTheirLegitimateTechniques(GameTestHelper h) {
        var npc=ModEntities.JINDAN_CULTIVATOR.get().create(h.getLevel());NpcProfile.ensure(npc,NpcProfile.JINDAN);
        var profile=NpcProfile.cultivationProfile(npc);profile.putString("Method",CultivationMethods.XUANYUE_SUIFENG_JUE);
        profile.put("Methods",tags(CultivationMethods.XUANYUE_SUIFENG_JUE,CultivationMethods.CHANGQING_JUE));
        profile.put("Spells",tags(ModSpells.FIREBALL));NpcProfile.ensure(npc,NpcProfile.JINDAN);
        h.assertTrue(NpcProfile.learnedSpellIds(npc,NpcProfile.JINDAN).containsAll(List.of(ModSpells.FIREBALL,
                ModSpells.SHANSUO_ZHENYU,ModSpells.QINGTENG_FU,ModSpells.WANMU_SHENG)),"Legitimate known methods still supply their own arts");
        npc.discard();h.succeed();
    }

    @GameTest(template="fate_test")
    public static void shopProvidesHigherTierScrollsAndMethodManuals(GameTestHelper h) {
        var ids=SystemShop.catalog().stream().map(SystemShop.CatalogEntry::id).collect(java.util.stream.Collectors.toSet());
        for(String id:List.of("tianyun_fencheng_scroll","wulei_hongding_scroll","wanren_chuanxin_scroll",
                "dari_bati_method","gangting_xiaokong_method","xuanyue_suifeng_method","hanlan_ningyuan_method"))
            h.assertTrue(ids.contains("immortal_cultivation_mod:"+id),"Real purchasable catalog includes "+id);
        h.succeed();
    }

    @GameTest(template="fate_test")
    public static void teachingNpcCannotFalselyGrantAnUnownedMethodArt(GameTestHelper h)throws Exception {
        var npc=ModEntities.JINDAN_CULTIVATOR.get().create(h.getLevel());NpcProfile.ensure(npc,NpcProfile.JINDAN);
        var profile=NpcProfile.cultivationProfile(npc);
        profile.putString("Realm",CultivationLevels.REALM_JINDAN+CultivationLevels.STAGE_LATE);
        profile.putString("Method",CultivationMethods.BASIC_BREATHING);profile.put("Methods",tags(CultivationMethods.BASIC_BREATHING));
        profile.put("Spells",new ListTag());
        var teacher=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"spell-teacher"));
        com.example.immortal_cultivation_mod.attachment.ModAttachments.setData(teacher,
                com.example.immortal_cultivation_mod.attachment.ModAttachments.getData(teacher)
                        .withKnownSpells(List.of(ModSpells.SHANSUO_ZHENYU,ModSpells.FIREBALL)));
        var relation=new net.minecraft.nbt.CompoundTag();relation.putBoolean("Initialized",true);relation.putInt("Trust",100);
        profile.getCompound("Relationships").put(teacher.getUUID().toString(),relation);
        Method teach=NpcProfile.class.getDeclaredMethod("teachNpcSpell",net.minecraft.world.entity.PathfinderMob.class,net.minecraft.server.level.ServerPlayer.class,int.class);
        teach.setAccessible(true);
        h.assertTrue(teach.invoke(null,npc,teacher,0).equals("npc.status.teaching_invalid"),"Unowned method art is rejected instead of falsely reporting success");
        h.assertTrue(teach.invoke(null,npc,teacher,1).equals("npc.status.npc_spell_learned")
                &&NpcProfile.learnedSpellIds(npc,NpcProfile.JINDAN).contains(ModSpells.FIREBALL),"Ordinary player-taught spells still persist");
        teacher.discard();npc.discard();h.succeed();
    }
}
