package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.combat.RealmKillProtection;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_mod")
@PrefixGameTestTemplate(false)
public final class RealmKillProtectionGameTests {
    @GameTest(template="fate_test")
    public static void huashenThresholdAndSelfKillCommand(GameTestHelper h) throws Exception {
        var player=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"realm-kill-test"));
        for(String realm:CultivationLevels.allLevels()) {
            ModAttachments.setData(player,ModAttachments.CultivationData.createDefault().withCultivationLevel(realm));
            h.assertTrue(RealmKillProtection.protects(player)==(CultivationLevels.getRealmIndex(realm)>=5),
                    "Only Huashen and higher gain command immunity: "+realm);
        }
        ModAttachments.setData(player,ModAttachments.CultivationData.createDefault()
                .withCultivationLevel(CultivationLevels.REALM_HUASHEN+CultivationLevels.STAGE_EARLY));
        float health=player.getHealth();
        var source=h.getLevel().getServer().createCommandSourceStack().withLevel(h.getLevel())
                .withEntity(player).withSuppressedOutput();
        int result=h.getLevel().getServer().getCommands().getDispatcher().execute("kill",source);
        h.assertTrue(result==0&&player.isAlive()&&player.getHealth()==health,
                "Actual /kill self command leaves Huashen health and life intact");
        player.discard();h.succeed();
    }
    @GameTest(template="npc_combat_arena")
    public static void mixedKillSelectorSkipsHighRealmOnly(GameTestHelper h) throws Exception {
        var npc=h.spawn(ModEntities.JINDAN_CULTIVATOR.get(),new BlockPos(4,4,4));
        npc.setNoAi(true);npc.npcRealm();
        NpcProfile.cultivationProfile(npc).putString("Realm",CultivationLevels.REALM_HUASHEN+CultivationLevels.STAGE_EARLY);
        var pig=h.spawn(EntityType.PIG,new BlockPos(6,4,4));pig.setNoAi(true);
        String tag="realm_kill_"+UUID.randomUUID().toString().replace("-","");
        npc.addTag(tag);pig.addTag(tag);
        var source=h.getLevel().getServer().createCommandSourceStack().withLevel(h.getLevel()).withSuppressedOutput();
        int result=h.getLevel().getServer().getCommands().getDispatcher().execute("kill @e[tag="+tag+"]",source);
        h.assertTrue(result==1&&npc.isAlive()&&!pig.isAlive(),
                "Selector kills ordinary targets, skips Huashen NPC and counts only eligible targets");
        npc.discard();pig.discard();h.succeed();
    }
}
