package com.example.immortal_cultivation_mod.companion;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.entity.ModEntities;
import com.example.immortal_cultivation_mod.entity.XuanjinZhengEntity;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.UUID;

@GameTestHolder(ImmortalCultivationMod.MODID)
@PrefixGameTestTemplate(false)
public final class XuanjinGameTests {
    private static ServerPlayer player(GameTestHelper h) {
        var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"passenger-test"));
        BlockPos pos=h.absolutePos(new BlockPos(4,2,3)); p.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
        ModAttachments.setData(p,ModAttachments.getData(p).withCultivationLevel(CultivationLevels.REALM_LIANQI+CultivationLevels.STAGE_LATE).withQi(250));
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(ModItems.XUANJIN_SEAL.get()));
        return p;
    }
    private static XuanjinZhengEntity beast(GameTestHelper h) {
        var b=ModEntities.XUANJIN_ZHENG.get().create(h.getLevel());
        BlockPos pos=h.absolutePos(new BlockPos(5,2,5)); b.setPos(pos.getX(),pos.getY(),pos.getZ()); b.setNoAi(true); return b;
    }
    @GameTest(template="fate_test")
    public static void realmGatesAndCostsAreServerAuthoritative(GameTestHelper h) {
        var p=player(h); h.assertTrue(XuanjinCompanion.bind(p,p.getMainHandItem()),"Late Qi can awaken the cub");
        var s=XuanjinCompanion.state(p); s.putInt("Trust",100);
        h.assertTrue(XuanjinCompanion.execute(p,"unlock")==0 && s.getInt("Seal")==0 && s.getInt("Energy")==100,"Insufficient realm spends no energy");
        ModAttachments.setData(p,ModAttachments.getData(p).withCultivationLevel(CultivationLevels.REALM_ZHUJI+CultivationLevels.STAGE_EARLY));
        h.assertTrue(XuanjinCompanion.execute(p,"unlock")==1 && s.getInt("Seal")==1 && s.getInt("Energy")==0,"First seal consumes the full prior capacity");
        h.assertTrue(s.getFloat("Health")==250 && s.getInt("Freedom")==12,"Unlock applies the supplied stats and freedom");
        h.succeed();
    }
    @GameTest(template="fate_test")
    public static void allSealsRequireTrustAndFullEnergy(GameTestHelper h) {
        var p=player(h); XuanjinCompanion.bind(p,p.getMainHandItem());
        ModAttachments.setData(p,ModAttachments.getData(p).withCultivationLevel(CultivationLevels.REALM_HUASHEN+CultivationLevels.STAGE_EARLY));
        var s=XuanjinCompanion.state(p);
        int[] trust={40,55,70,80,90,100},energy={100,250,500,1000,1600,4000};
        for(int i=0;i<6;i++) {
            s.putInt("Trust",trust[i]-1);s.putInt("Energy",energy[i]);
            h.assertTrue(XuanjinCompanion.execute(p,"unlock")==0 && s.getInt("Seal")==i && s.getInt("Energy")==energy[i],
                    "Low trust blocks seal "+(i+1)+" without consuming energy");
            s.putInt("Trust",trust[i]);s.putInt("Energy",energy[i]-1);
            h.assertTrue(XuanjinCompanion.execute(p,"unlock")==0 && s.getInt("Seal")==i && s.getInt("Energy")==energy[i]-1,
                    "An incomplete energy bar blocks seal "+(i+1)+" without partial payment");
            s.putInt("Energy",energy[i]);
            h.assertTrue(XuanjinCompanion.execute(p,"unlock")==1 && s.getInt("Seal")==i+1 && s.getInt("Energy")==0 && s.getInt("Trust")==trust[i],
                    "Exact requirements unlock seal "+(i+1)+", consuming energy but preserving trust");
        }
        h.succeed();
    }
    @GameTest(template="fate_test")
    public static void copiedAndForeignLanternsCannotCreateAnotherBond(GameTestHelper h) {
        var p=player(h); XuanjinCompanion.bind(p,p.getMainHandItem());
        var bond=XuanjinCompanion.state(p).getUUID("Bond");
        h.assertTrue(XuanjinCompanion.bind(p,p.getMainHandItem().copy()),"Copy refers to the same authoritative bond");
        h.assertTrue(XuanjinCompanion.state(p).getUUID("Bond").equals(bond),"No duplicate bond created");
        h.assertTrue(!XuanjinCompanion.bind(p,new ItemStack(ModItems.XUANJIN_SEAL.get())),"A second blank lantern cannot create another companion");
        var other=player(h); h.assertTrue(!XuanjinCompanion.bind(other,p.getMainHandItem().copy()),"Other players cannot command a copied token");
        h.succeed();
    }
    @GameTest(template="fate_test")
    public static void feedingCannotBeSpammedOrResetCooldown(GameTestHelper h) {
        var p=player(h); XuanjinCompanion.bind(p,p.getMainHandItem());
        var s=XuanjinCompanion.state(p); s.putInt("Energy",0);
        h.assertTrue(XuanjinCompanion.execute(p,"feed")==1,"First willing Qi offering succeeds");
        int qi=ModAttachments.getData(p).qi(); int trust=s.getInt("Trust");
        h.assertTrue(XuanjinCompanion.execute(p,"feed")==0,"Second offering is cooling down");
        h.assertTrue(ModAttachments.getData(p).qi()==qi && s.getInt("Trust")==trust,"Spam grants neither trust nor energy");
        s.putInt("Freedom",80); XuanjinCompanion.recall(p); XuanjinCompanion.recall(p);
        h.assertTrue(s.getInt("Freedom")==80,"Recalling an inactive bond cannot farm obedience"); h.succeed();
    }
    @GameTest(template="fate_test")
    public static void finalStatsAndRoundTripPersistence(GameTestHelper h) {
        var summoned = beast(h); summoned.readAdditionalSaveData(new CompoundTag());
        h.assertTrue(summoned.isFree() && summoned.seal()==6,"A bare summon tag creates the free true form instead of an ownerless disappearing cub");
        var b=beast(h); b.free(XuanjinCompanion.fresh()); b.setHealth(43210);
        b.companionState().putInt("Energy",8345); b.companionState().putLong("Cooldown.claw",123456);
        var tag=new CompoundTag(); b.addAdditionalSaveData(tag); var loaded=beast(h); loaded.readAdditionalSaveData(tag);
        h.assertTrue(loaded.isFree() && loaded.seal()==6 && loaded.getMaxHealth()==50000,"True form survives save/reload");
        h.assertTrue(loaded.getHealth()==43210 && loaded.companionState().getInt("Energy")==8345,"Health and energy persist");
        h.assertTrue(loaded.companionState().getLong("Cooldown.claw")==123456,"Cooldown is not reset by reload");
        h.assertTrue(loaded.getAttributeValue(Attributes.ARMOR)==350 && Math.abs(loaded.getAttributeValue(Attributes.MOVEMENT_SPEED)-.42)<.001
                && loaded.getAttributeValue(Attributes.FOLLOW_RANGE)==32,"Armor, speed and follow attribute match the request"); h.succeed();
    }
    @GameTest(template="fate_test")
    public static void freeBeastTargetsPassiveCreaturesAndFormerOwner(GameTestHelper h) {
        var p=player(h); var b=beast(h); var cow=EntityType.COW.create(h.getLevel());
        b.bind(p,6);
        h.assertTrue(!XuanjinCombat.enemy(b,p) && !XuanjinCombat.enemy(b,cow),"A bound beast protects its owner and passive creatures");
        b.free(XuanjinCompanion.fresh());
        h.assertTrue(XuanjinCombat.enemy(b,p) && XuanjinCombat.enemy(b,cow),"Free means no owner or passive-mob exemption");
        h.assertTrue(b.ownerId()==null,"Final release permanently removes ownership"); h.succeed();
    }
    @GameTest(template="fate_test")
    public static void devourNeedsLooseEnergyAndNeverDrainsAllies(GameTestHelper h) {
        var b=beast(h); b.setHealth(10000); var s=b.companionState(); s.putInt("Seal",6); s.putInt("Energy",0);
        h.assertTrue(!XuanjinCombat.command(b,s,"devour",null),"Cannot heal from nothing");
        s.putBoolean("LooseEnergy",true);
        h.assertTrue(XuanjinCombat.command(b,s,"devour",null),"Loose energy feeds the body");
        h.assertTrue(b.getHealth()==15000 && s.getInt("Energy")==1000,"Exact true-form healing and energy");
        h.assertTrue(!XuanjinCombat.command(b,s,"devour",null),"Cannot reuse the same energy"); h.succeed();
    }
    @GameTest(template="fate_test")
    public static void vortexCapsAndPayoutAreSingleUse(GameTestHelper h) {
        var b=beast(h); b.setHealth(10000); var s=b.companionState(); s.putInt("Seal",6);
        h.assertTrue(XuanjinCombat.absorb(s,100000,.8F)==40000,"Total absorption capped at forty thousand");
        h.assertTrue(XuanjinCombat.absorb(s,10000,1)==0,"No capacity remains"); XuanjinCombat.finishVortex(s);
        h.assertTrue(XuanjinCombat.command(b,s,"swallow",null),"Stored vortex can be consumed");
        h.assertTrue(b.getHealth()==26000,"Healing is capped at sixteen thousand");
        h.assertTrue(!XuanjinCombat.command(b,s,"swallow",null) && s.getFloat("Absorbed")==0,"Payout cannot be duplicated"); h.succeed();
    }
    @GameTest(template="fate_test")
    public static void vortexDoesNotAbsorbMeleeOrAllHighRealmDamage(GameTestHelper h) {
        h.assertTrue(XuanjinRules.absorbFraction("physical",0)==0,"Melee is unaffected");
        h.assertTrue(XuanjinRules.absorbFraction("projectile",5)==.8F && XuanjinRules.absorbFraction("aoe",5)==.6F,"Spell and AoE fractions");
        h.assertTrue(XuanjinRules.absorbFraction("lightning",5)==1 && XuanjinRules.absorbFraction("lightning",6)==.5F,"Lianxu lightning cannot be completely absorbed");
        h.assertTrue(XuanjinRules.lightningEnergy(7000)==2000,"Ten thousand lightning example obeys energy gain cap"); h.succeed();
    }
    @GameTest(template="fate_test")
    public static void fullEnergyHysteresisAndAuraTiers(GameTestHelper h) {
        h.assertTrue(XuanjinRules.fullEnergy(false,10000) && !XuanjinRules.fullEnergy(false,9999),"Full state begins only at ten thousand");
        h.assertTrue(XuanjinRules.fullEnergy(true,7500) && !XuanjinRules.fullEnergy(true,7499),"Full state persists down to seventy-five percent");
        h.assertTrue(Math.abs(XuanjinRules.auraDamage(1,true)-.55)<.001 && XuanjinRules.auraDamage(6,true)==0,"Spirit bonus and Lianxu immunity");
        h.assertTrue(XuanjinRules.refusal(90,100)==0 && XuanjinRules.refusal(10,100)>.5,"Trust and freedom have separate consequences"); h.succeed();
    }
    @GameTest(template="fate_test")
    public static void finalReleaseRequiresExplicitLiveConfirmation(GameTestHelper h) {
        var p=player(h); XuanjinCompanion.bind(p,p.getMainHandItem()); var s=XuanjinCompanion.state(p); s.putInt("Seal",6);
        h.assertTrue(XuanjinCompanion.execute(p,"free_confirm")==0 && !s.getBoolean("Freed"),"A stale or forged confirmation cannot free the beast");
        h.assertTrue(XuanjinCompanion.execute(p,"free")==1 && !s.getBoolean("Freed"),"Opening the choice does not mutate the bond");
        h.assertTrue(s.getLong("FreeConfirmUntil")>h.getLevel().getGameTime(),"Confirmation expires after a bounded interval"); h.succeed();
    }
    @GameTest(template="fate_test")
    public static void nullShieldSourcesAndClawContextStayCompatible(GameTestHelper h) {
        h.assertTrue(com.example.immortal_cultivation_mod.entity.DefensePiercingProjectile.defenseBypass(null)==0,"Null shield damage has no piercing");
        h.assertTrue(com.example.immortal_cultivation_mod.entity.DefensePiercingProjectile.shieldMultiplier(null)==1,"Legacy shield calls keep their original multiplier");
        h.assertTrue(XuanjinCombat.penetration(h.getLevel().damageSources().magic())==0,"Unrelated magic cannot inherit claw penetration"); h.succeed();
    }
    @GameTest(template="fate_test")
    public static void possessionExpiresAndRemovesEveryModifier(GameTestHelper h) {
        var p=player(h); XuanjinCompanion.bind(p,p.getMainHandItem()); var s=XuanjinCompanion.state(p);
        s.putInt("Seal",6); s.putInt("Trust",100); s.putInt("Energy",10000);
        double armor=p.getAttributeValue(Attributes.ARMOR), speed=p.getAttributeValue(Attributes.MOVEMENT_SPEED);
        h.assertTrue(XuanjinCompanion.execute(p,"possess")==1,"Possession begins from a fully unlocked bond");
        h.assertTrue(s.getInt("Energy")==9000 && XuanjinCompanion.possessing(p),"Possession reserves its cost");
        h.assertTrue(s.getLong("PossessUntil")==p.level().getGameTime()+6000,"Possession lasts five full minutes");
        h.assertTrue(p.getAttributeValue(Attributes.ARMOR)==armor+350,"Possession armor is temporary");
        s.putInt("Energy",10000); XuanjinCombat.tick(p,s);
        h.assertTrue(p.getAttributeValue(Attributes.ARMOR)==armor+450,"Full-energy armor stacks during possession");
        s.putLong("PossessUntil",p.level().getGameTime());
        XuanjinCompanion.tick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(p));
        h.assertTrue(!XuanjinCompanion.possessing(p) && p.getAttributeValue(Attributes.ARMOR)==armor
                && p.getAttributeValue(Attributes.MOVEMENT_SPEED)==speed,"Ending possession removes both possession and full-energy bonuses");
        h.assertTrue(XuanjinCompanion.execute(p,"possess")==0,"Recall cannot reset possession cooldown"); h.succeed();
    }
    @GameTest(template="fate_test")
    public static void replacementLanternRecoversStateWithoutDuplicatingIt(GameTestHelper h) {
        var p=player(h); XuanjinCompanion.bind(p,p.getMainHandItem()); var s=XuanjinCompanion.state(p);
        var id=s.getUUID("Bond"); s.putInt("Trust",73); s.putLong("Cooldown.manifest",12345);
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        var replacement=new ItemStack(ModItems.XUANJIN_SEAL.get());
        h.assertTrue(XuanjinCompanion.bind(p,replacement),"A lost lantern can be replaced");
        h.assertTrue(s.getUUID("Bond").equals(id) && s.getInt("Trust")==73 && s.getLong("Cooldown.manifest")==12345,"Replacing the vessel does not reset the passenger"); h.succeed();
    }
    @GameTest(template="fate_test")
    public static void releasedHunterKeepsPursuingOutsideDetectionRadius(GameTestHelper h) {
        var b=beast(h); b.setNoAi(false); h.getLevel().addFreshEntity(b);
        var cow=h.spawn(EntityType.COW,new BlockPos(6,2,7)); cow.setNoAi(true); b.setTarget(cow);
        cow.setPos(b.getX()+40,b.getY(),b.getZ()); b.tick();
        h.assertTrue(b.getTarget()==cow,"Leaving thirty blocks does not drop an acquired target");
        var saved=new CompoundTag(); b.addAdditionalSaveData(saved);
        h.assertTrue(saved.hasUUID("HuntTarget") && saved.getUUID("HuntTarget").equals(cow.getUUID()),"Pursuit identity is saved across unloading");
        b.discard(); cow.discard(); h.succeed();
    }
    @GameTest(template="fate_test")
    public static void fiveTailsReallyDealFiveHitsAgainstOneEnemy(GameTestHelper h) {
        var b=beast(h); var target=beast(h); target.setPos(b.getX()+3,b.getY(),b.getZ());
        target.getAttribute(Attributes.ARMOR).setBaseValue(0); target.companionState().putInt("Energy",0);
        h.getLevel().addFreshEntity(target); float health=target.getHealth();
        h.assertTrue(XuanjinCombat.command(b,b.companionState(),"tails",null),"Tail attack finds a nearby target");
        h.assertTrue(Math.abs((health-target.getHealth())-10000)<.01,"Single target receives all five tail strikes despite invulnerability frames");
        target.discard(); h.succeed();
    }
    @GameTest(template="fate_test")
    public static void permanentReleaseConsumesVesselAndCannotBeRebound(GameTestHelper h) {
        var p=player(h); p.setPos(p.getX(),p.getY()+20,p.getZ());
        XuanjinCompanion.bind(p,p.getMainHandItem()); var s=XuanjinCompanion.state(p);
        s.putInt("Seal",6); s.putFloat("Health",50000); s.putInt("Trust",100);
        XuanjinCompanion.execute(p,"free");
        h.assertTrue(XuanjinCompanion.execute(p,"free_confirm")==1 && s.getBoolean("Freed"),"Confirmed release becomes terminal");
        h.assertTrue(p.getMainHandItem().is(net.minecraft.world.item.Items.LANTERN),"The possessed item becomes an ordinary lantern in place");
        h.assertTrue(!XuanjinCompanion.bind(p,new ItemStack(ModItems.XUANJIN_SEAL.get())),"New lanterns cannot undo the final choice");
        var released=h.getLevel().getEntitiesOfClass(XuanjinZhengEntity.class,p.getBoundingBox().inflate(10));
        h.assertTrue(released.size()==1 && released.getFirst().isFree() && XuanjinCombat.enemy(released.getFirst(),p),"One free beast is spawned and its former owner is prey");
        released.forEach(net.minecraft.world.entity.Entity::discard); h.succeed();
    }
    @GameTest(template="fate_test")
    public static void allSevenFormsHaveCompatiblePackagedAssets(GameTestHelper h) throws Exception {
        String base="/assets/immortal_cultivation_mod/";
        com.google.gson.JsonObject animations;
        try(var reader=new java.io.InputStreamReader(XuanjinGameTests.class.getResourceAsStream(base+"animations/entity/xuanjin_zheng.animation.json"),java.nio.charset.StandardCharsets.UTF_8)) {
            animations=com.google.gson.JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("animations");
        }
        h.assertTrue(animations.size()==10 && animations.has("animation.xuanjin_zheng.claw_right"),"All ten clips including the opposite-paw attack are packaged");
        for(int seal=0;seal<=6;seal++) {
            String suffix=seal<6?"_seal_"+seal:"";
            try(var reader=new java.io.InputStreamReader(XuanjinGameTests.class.getResourceAsStream(base+"geo/entity/xuanjin_zheng"+suffix+".geo.json"),java.nio.charset.StandardCharsets.UTF_8)) {
                var geo=com.google.gson.JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
                var names=new java.util.HashSet<String>();
                for(var bone:geo.getAsJsonArray("bones")) h.assertTrue(names.add(bone.getAsJsonObject().get("name").getAsString()),"Bone names are unique");
                for(var clip:animations.asMap().values()) h.assertTrue(names.containsAll(clip.getAsJsonObject().getAsJsonObject("bones").keySet()),"Every animation bone resolves in each sealed form");
                h.assertTrue(geo.getAsJsonObject("description").get("texture_width").getAsInt()==1792,"All stages use the matching atlas");
            }
        }
        for(String texture:java.util.List.of("xuanjin_zheng.png","xuanjin_zheng_glowmask.png"))
            try(var stream=new java.io.DataInputStream(XuanjinGameTests.class.getResourceAsStream(base+"textures/entity/"+texture))) {
                stream.skipNBytes(16); h.assertTrue(stream.readInt()==1792 && stream.readInt()==1792,"Texture and emissive atlas dimensions match");
            }
        h.assertTrue(h.getLevel().getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID,"xuanjin_seal")).isEmpty(),"The lantern has no crafting recipe"); h.succeed();
    }
    @GameTest(template="fate_test")
    public static void spellElementIsCapturedWithoutLeakingIntoLaterMelee(GameTestHelper h) {
        var p=player(h);
        com.example.immortal_cultivation_mod.magicritual.center.CenterEffectSupport.beginSpellCast(p,com.example.immortal_cultivation_mod.spell.ModSpells.FIREBALL);
        try {
            h.assertTrue(XuanjinCombat.element(p.damageSources().indirectMagic(p,p)).equals("fire"),"Synchronous magic retains its exact active spell element");
        } finally { com.example.immortal_cultivation_mod.magicritual.center.CenterEffectSupport.endSpellCast(); }
        h.assertTrue(XuanjinCombat.element(p.damageSources().playerAttack(p)).equals("physical"),"Later ordinary melee is not mislabeled as the last spell"); h.succeed();
    }
    @GameTest(template="fate_test")
    public static void spiritClawBonusesApplyOnceToRealShields(GameTestHelper h) {
        var b=beast(h); var target=ModEntities.FANREN_NPC.get().create(h.getLevel()); target.setPos(b.getX()+3,b.getY(),b.getZ()); target.setNoAi(true);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(50000); target.setHealth(50000);
        target.getAttribute(Attributes.ARMOR).setBaseValue(0); h.getLevel().addFreshEntity(target);
        com.example.immortal_cultivation_mod.spell.DielangShield.activateNext(target);
        float shield=com.example.immortal_cultivation_mod.spell.DielangShield.remaining(target);
        var probe = new ShieldProbe(target);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(probe);
        try {
            h.assertTrue(XuanjinCombat.command(b,b.companionState(),"claw",null),"Claw attacks a shielded target");
            // NpcProfile reapplies realm HP during damage handling; observe the real post-shield
            // hit instead of treating that independent HP reset as extra attack damage.
            h.assertTrue(Math.abs(probe.amount-(13500-shield))<.01,"Shielded target takes the 13,500 bonus exactly once; post-shield hit="+probe.amount+", shield="+shield);
        } finally {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(probe);
            com.example.immortal_cultivation_mod.spell.DielangShield.clear(target); target.discard();
        }
        h.succeed();
    }
    @GameTest(template="fate_test")
    public static void guiRejectsForgedStaleAndRepeatedActions(GameTestHelper h) {
        var p=player(h);XuanjinCompanion.bind(p,p.getMainHandItem());
        var s=XuanjinCompanion.state(p);s.putInt("Energy",0);int qi=ModAttachments.getData(p).qi();
        UUID session=XuanjinInterface.open(p);
        h.assertTrue(!XuanjinInterface.action(p,UUID.randomUUID(),"feed"),"Forged session cannot feed");
        h.assertTrue(!XuanjinInterface.action(p,session,"set_energy"),"Unknown GUI action is rejected");
        h.assertTrue(XuanjinInterface.action(p,session,"feed"),"Validated GUI feed uses the real server action");
        h.assertTrue(s.getInt("Energy")==40 && ModAttachments.getData(p).qi()==qi-20,"GUI cannot bypass Qi payment");
        h.assertTrue(!XuanjinInterface.action(p,session,"feed") && s.getInt("Trust")==25,"Repeated packets cannot grant extra trust");
        XuanjinInterface.action(p,session,"close");UUID reopened=XuanjinInterface.open(p);
        h.assertTrue(!session.equals(reopened) && !XuanjinInterface.action(p,session,"recall"),"A reopened screen rejects old clicks");
        XuanjinInterface.action(p,reopened,"close");h.succeed();
    }
    @GameTest(template="fate_test")
    public static void guiLosingLanternRevokesControl(GameTestHelper h) {
        var p=player(h);XuanjinCompanion.bind(p,p.getMainHandItem());UUID session=XuanjinInterface.open(p);
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        int qi=ModAttachments.getData(p).qi();
        h.assertTrue(!XuanjinInterface.action(p,session,"feed"),"Dropping the lantern revokes the screen session");
        h.assertTrue(ModAttachments.getData(p).qi()==qi && XuanjinCompanion.state(p).getInt("Trust")==20,"Revoked action changes no resources");
        h.succeed();
    }
    @GameTest(template="fate_test")
    public static void guiCancelDisarmsPermanentRelease(GameTestHelper h) {
        var p=player(h);XuanjinCompanion.bind(p,p.getMainHandItem());var s=XuanjinCompanion.state(p);
        s.putInt("Seal",6);s.putFloat("Health",50000);s.putInt("Energy",10000);
        UUID session=XuanjinInterface.open(p);
        h.assertTrue(XuanjinInterface.action(p,session,"free") && !s.getBoolean("Freed"),"Release opens confirmation without freeing the beast");
        h.runAfterDelay(5,()->{
            h.assertTrue(XuanjinInterface.action(p,session,"cancel_free") && s.getLong("FreeConfirmUntil")==0,"GUI cancel clears server confirmation");
        });
        h.runAfterDelay(10,()->{
            h.assertTrue(!XuanjinInterface.action(p,session,"free_confirm") && !s.getBoolean("Freed"),"Cancelled confirmation cannot be replayed");
            XuanjinInterface.action(p,session,"close");h.succeed();
        });
    }
    @GameTest(template="fate_test")
    public static void guiSnapshotIsDetachedFromBondState(GameTestHelper h) {
        var p=player(h);XuanjinCompanion.bind(p,p.getMainHandItem());
        var snapshot=XuanjinInterface.snapshot(p);snapshot.putInt("Trust",100);
        h.assertTrue(XuanjinCompanion.state(p).getInt("Trust")==20,"UI snapshot does not expose mutable authoritative NBT");
        h.assertTrue(!snapshot.contains("Bond") && !snapshot.contains("Body") && snapshot.getInt("Energy")==100,"Snapshot exposes display data only");
        h.succeed();
    }
    @GameTest(template="fate_test")
    public static void lanternRemembersViewAndSelectionWithoutCasting(GameTestHelper h) {
        var p=player(h);XuanjinCompanion.bind(p,p.getMainHandItem());var s=XuanjinCompanion.state(p);s.putInt("Seal",1);
        UUID session=XuanjinInterface.open(p);
        h.assertTrue(XuanjinInterface.action(p,session,"view_1_1"),"Technique page is saved");
        h.assertTrue(!XuanjinInterface.action(p,session,"select_free_confirm"),"Release can never be armed as a spell");
        h.assertTrue(!XuanjinInterface.action(p,session,"select_vortex"),"Locked spells cannot be selected");
        h.assertTrue(XuanjinInterface.action(p,session,"select_eyes"),"Unlocked spell can be selected");
        h.assertTrue(s.getLong("Cooldown.eyes")==0 && s.getInt("Freedom")==0,"Selecting does not cast or consume resources");
        XuanjinInterface.action(p,session,"close");UUID reopened=XuanjinInterface.open(p);
        var view=XuanjinInterface.snapshot(p);
        h.assertTrue(view.getInt("GuiTab")==1 && view.getInt("GuiPage")==1 && view.getString("SelectedTechnique").equals("eyes"),"Reopening restores page and selection");
        h.assertTrue(!XuanjinInterface.action(p,session,"select_claw"),"Old sessions cannot alter selection");
        XuanjinInterface.action(p,reopened,"close");h.succeed();
    }
    @GameTest(template="fate_test")
    public static void leftClickUsesSavedSpellAndRealCooldown(GameTestHelper h) {
        var p=player(h);XuanjinCompanion.bind(p,p.getMainHandItem());var s=XuanjinCompanion.state(p);s.putInt("Seal",1);
        UUID session=XuanjinInterface.open(p);XuanjinInterface.action(p,session,"select_eyes");
        h.assertTrue(!XuanjinInterface.castSelected(p),"Clicks within the menu cannot cast");
        XuanjinInterface.action(p,session,"close");
        h.assertTrue(XuanjinInterface.castSelected(p),"World left-click executes the selected spell");
        long cooldown=s.getLong("Cooldown.eyes");
        h.assertTrue(cooldown==p.level().getGameTime()+100 && s.getInt("Freedom")==1,"Cast applies the real spell cost and cooldown");
        h.assertTrue(!XuanjinInterface.castSelected(p),"Same-tick spam is rejected");
        h.runAfterDelay(5,()->{
            h.assertTrue(!XuanjinInterface.castSelected(p) && s.getLong("Cooldown.eyes")==cooldown && s.getInt("Freedom")==1,"Cooldown cannot be bypassed by another click");
            h.succeed();
        });
    }
    @GameTest(template="fate_test")
    public static void leftClickRequiresOwnHeldLantern(GameTestHelper h) {
        var p=player(h);XuanjinCompanion.bind(p,p.getMainHandItem());var s=XuanjinCompanion.state(p);
        s.putInt("Seal",1);s.putString("SelectedTechnique","eyes");var own=p.getMainHandItem();
        p.getInventory().items.set(1,own);p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(ModItems.XUANJIN_SEAL.get()));
        h.assertTrue(!XuanjinInterface.castSelected(p),"A blank lantern cannot cast through a different inventory token");
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,own);s.putInt("Seal",0);
        h.assertTrue(!XuanjinInterface.castSelected(p) && s.getLong("Cooldown.eyes")==0,"Cast rechecks the current seal");
        h.succeed();
    }
    @GameTest(template="fate_test")
    public static void largerSealFormsKeepMatchingCollisionBounds(GameTestHelper h) {
        var b=beast(h);float previous=0;
        for(int seal=0;seal<7;seal++) {
            b.setSeal(seal);float scale=XuanjinRules.stage(seal).scale();
            h.assertTrue(scale>previous,"Forms grow progressively");previous=scale;
            h.assertTrue(Math.abs(b.getBbHeight()-3.2F*scale)<.001 && Math.abs(b.getBbWidth()-2.2F*scale)<.001,"Server collision dimensions match the render scale");
        }
        h.assertTrue(XuanjinRules.stage(0).scale()>=.2F && XuanjinRules.stage(6).scale()==1,"Cub is larger and true form keeps its full size");h.succeed();
    }
    @GameTest(template="fate_test")
    public static void energyRegeneratesOncePerSecondAndCaps(GameTestHelper h) {
        var p=player(h);XuanjinCompanion.bind(p,p.getMainHandItem());var s=XuanjinCompanion.state(p);
        s.putInt("Seal",2);s.putInt("Energy",50);
        XuanjinCompanion.regenerate(p);XuanjinCompanion.regenerate(p);
        h.assertTrue(s.getInt("Energy")==55,"Second-seal energy restores five per second, not per call or item");
        h.runAfterDelay(20,()->{
            XuanjinCompanion.regenerate(p);h.assertTrue(s.getInt("Energy")==60,"Regeneration resumes after twenty ticks");
            s.putInt("Energy",499);s.remove("RegenAt");XuanjinCompanion.regenerate(p);
            h.assertTrue(s.getInt("Energy")==500,"Automatic energy never exceeds the seal capacity");h.succeed();
        });
    }
    @GameTest(template="fate_test")
    public static void missingOrFreedLanternDoesNotRegenerate(GameTestHelper h) {
        var p=player(h);XuanjinCompanion.bind(p,p.getMainHandItem());var s=XuanjinCompanion.state(p);s.putInt("Energy",0);
        var lantern=p.getMainHandItem();p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        XuanjinCompanion.regenerate(p);h.assertTrue(s.getInt("Energy")==0,"Lost lantern grants no passive energy");
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,lantern);s.putBoolean("Freed",true);
        XuanjinCompanion.regenerate(p);h.assertTrue(s.getInt("Energy")==0,"Permanent release stops energy generation");h.succeed();
    }
    @GameTest(template="fate_test")
    public static void basicAttackIsNotOverwrittenByAutomaticAbilities(GameTestHelper h) {
        var b=beast(h);var target=beast(h);target.setPos(b.getX()+2,b.getY(),b.getZ());
        h.getLevel().addFreshEntity(b);h.getLevel().addFreshEntity(target);b.setNoAi(false);b.setTarget(target);b.tick();
        h.assertTrue(b.isActing() && b.companionState().getInt("Combo")==1,"First melee strike starts an attack animation");
        h.assertTrue(b.companionState().getLong("Cooldown.claw")==0 && b.companionState().getLong("Cooldown.tails")==0,"Automatic abilities do not overwrite the new strike in the same tick");
        b.tick();h.assertTrue(b.isActing() && b.companionState().getInt("Combo")==1,"Attack continues across server ticks");
        b.discard();target.discard();h.succeed();
    }
    // The 34-block probes must stay inside their own loaded test area, not neighboring 16-block fixtures.
    @GameTest(template="roar_test")
    public static void roarAffectsOnlyTheForwardExpandingCone(GameTestHelper h) {
        var b=beast(h);b.setYRot(0);b.setXRot(0);var origin=b.getEyePosition();
        var targets=new java.util.ArrayList<net.minecraft.world.entity.animal.Cow>();
        double[][] offsets={{0,8},{12,28},{8,8},{0,-4},{0,34}};
        for(var offset:offsets) {
            var cow=EntityType.COW.create(h.getLevel());cow.setNoAi(true);
            cow.setPos(origin.x+offset[0],origin.y-cow.getBbHeight()*.5,origin.z+offset[1]);
            h.getLevel().addFreshEntity(cow);targets.add(cow);
        }
        h.assertTrue(XuanjinCombat.command(b,b.companionState(),"voice",null),"Actual roar command casts");
        for(int i=0;i<targets.size();i++)h.assertTrue(targets.get(i).hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN)==(i<2),
                "Only front and widening-cone targets are affected; target="+i);
        h.assertTrue(b.isActing() && b.companionState().getLong("Cooldown.voice")==h.getLevel().getGameTime()+150,"Roar starts animation and its 7.5-second cooldown");
        targets.forEach(net.minecraft.world.entity.Entity::discard);h.succeed();
    }
    @GameTest(template="fate_test")
    public static void roarConeFollowsVerticalAim(GameTestHelper h) {
        var origin=net.minecraft.world.phys.Vec3.ZERO;var up=new net.minecraft.world.phys.Vec3(0,1,0);
        h.assertTrue(XuanjinRules.inRoarCone(origin,up,new net.minecraft.world.phys.Vec3(2,8,0),0),"Upward aim rotates the expanding cone");
        h.assertTrue(!XuanjinRules.inRoarCone(origin,up,new net.minecraft.world.phys.Vec3(0,-3,0),0),"Roar does not extend backwards");h.succeed();
    }
    @GameTest(template="fate_test")
    public static void extendedClawHitsWithinRangeAndRejectsBeyondIt(GameTestHelper h) {
        for(int seal:new int[]{2,6}) {
            var b=beast(h);b.setSeal(seal);var state=b.companionState();state.putInt("Seal",seal);
            state.putInt("Energy",XuanjinRules.stage(seal).energy());
            var cow=EntityType.COW.create(h.getLevel());cow.setNoAi(true);
            cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(50000);cow.setHealth(50000);
            cow.setPos(b.getX()+10,b.getY(),b.getZ());h.getLevel().addFreshEntity(cow);
            h.assertTrue(XuanjinCombat.command(b,state,"claw",null),"Seal "+seal+" can project its claw beyond the old range");
            h.assertTrue(cow.getHealth()<50000 && cow.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN),
                    "Extended claw actually damages and slows its target");
            state.remove("Cooldown.claw");cow.setPos(b.getX()+XuanjinRules.CLAW_RANGE+1,b.getY(),b.getZ());
            int energy=state.getInt("Energy");
            h.assertTrue(!XuanjinCombat.command(b,state,"claw",null) && state.getInt("Energy")==energy,
                    "Out-of-range targets cannot consume energy or be hit");
            cow.discard();b.discard();
        }
        h.assertTrue(XuanjinRules.roarRadius(28)>XuanjinRules.roarRadius(8)*3,
                "Distant sonic rings and the matching hit cone grow substantially larger");h.succeed();
    }
    @GameTest(template="fate_test")
    public static void automaticClawUsesExtendedRange(GameTestHelper h) {
        var b=beast(h);var target=beast(h);target.setPos(b.getX()+10,b.getY(),b.getZ());
        h.getLevel().addFreshEntity(b);h.getLevel().addFreshEntity(target);b.setNoAi(false);b.setTarget(target);b.tick();
        h.assertTrue(b.isActing() && b.companionState().getLong("Cooldown.claw")>h.getLevel().getGameTime(),
                "Manifested companion uses the claw spell before entering melee distance");
        h.assertTrue(b.companionState().getInt("Combo")==0,"Extended spell range does not turn basic melee into a distant bite");
        b.discard();target.discard();h.succeed();
    }
    public static final class ShieldProbe {
        private final net.minecraft.world.entity.LivingEntity target;
        float amount = Float.NaN;
        ShieldProbe(net.minecraft.world.entity.LivingEntity target) { this.target = target; }
        @net.neoforged.bus.api.SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.LOWEST)
        public void hit(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
            if (event.getEntity() == target) amount=event.getAmount();
        }
    }
}
