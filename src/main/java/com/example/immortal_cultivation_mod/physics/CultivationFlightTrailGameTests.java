package com.example.immortal_cultivation_mod.physics;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.spell.JindanCreativeFlight;
import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.attachment.SpiritRoots;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.network.CultivationFlightPayloads;
import com.example.immortal_cultivation_mod.spell.CultivationFlightVisuals;
import com.example.immortal_cultivation_mod.spell.YufengJue;
import com.example.immortal_cultivation_mod.spell.HantiBingqin;
import com.example.immortal_cultivation_mod.spell.UndeadControl;
import com.example.immortal_cultivation_mod.entity.FlyingSwordEntity;
import com.example.immortal_cultivation_mod.entity.ModEntities;
import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ImmortalCultivationMod.MODID)
@PrefixGameTestTemplate(false)
public final class CultivationFlightTrailGameTests {
    @GameTest(template="fate_test")
    public static void trailRecordsTurnsAndFadesWhereMovementStopped(GameTestHelper h) {
        var path = new CultivationFlightTrail();
        for (int t=0;t<20;t++) path.sample(new Vec3(t*.8,5,0),t);
        for (int t=20;t<35;t++) path.sample(new Vec3(15.2,5,(t-19)*.8),t);
        var points = path.points(null);
        h.assertTrue(points.stream().anyMatch(p->p.x<8&&p.z==0) && points.getFirst().z>10,
                "Turning preserves the old world-space path instead of rotating a straight beam");
        h.assertTrue(path.length()>20,"Normal flight creates the long reference wake");
        Vec3 stop=points.getFirst();
        for (int t=35;t<=53;t++) path.sample(stop,t);
        h.assertTrue(path.opacity(53)==0,"Hovering cannot refresh a stationary glowing trail");
        h.assertTrue(path.points(null).getFirst().equals(stop),"A stopped wake stays where emitted");
        path.prune(100);h.assertTrue(path.size()==0,"Old samples are eventually removed");h.succeed();
    }

    @GameTest(template="fate_test")
    public static void trailBoundsAndTeleportDoNotCreateMapSpanningRibbons(GameTestHelper h) {
        var path=new CultivationFlightTrail();
        for (int t=0;t<300;t++)path.sample(new Vec3(t*2,0,0),t);
        h.assertTrue(path.size()<=CultivationFlightTrail.MAX_POINTS&&path.length()<=CultivationFlightTrail.MAX_LENGTH,
                "High-speed trails have bounded geometry and length");
        path.sample(new Vec3(9000,100,9000),300);
        h.assertTrue(path.size()==1&&path.opacity(300)==0,"Teleport starts a new wake without joining distant points");
        path.sample(new Vec3(Double.NaN,0,0),301);
        h.assertTrue(path.size()==0,"Invalid motion cannot reach the vertex buffer");h.succeed();
    }

    @GameTest(template="fate_test")
    public static void wakeVisibilityRequiresAContinuousNonCollapsedPath(GameTestHelper h) {
        var path = new CultivationFlightTrail();
        path.sample(new Vec3(0,6,0),0);
        path.sample(new Vec3(1,6,0),1);
        path.sample(new Vec3(2,6,1),2);
        var oldPoints = path.points(null);
        Vec3 destination = new Vec3(400,60,-300);
        h.assertTrue(!path.connectedTo(destination),
                "A teleported player cannot be hidden by an old wake before the next sample");
        h.assertTrue(path.points(destination).equals(oldPoints),
                "Interpolation cannot move the old wake's head across the teleport");

        path.sample(destination,3);
        h.assertTrue(path.connectedTo(destination) && !path.hasVisibleSegment(destination) && path.opacity(3)==0,
                "One fresh teleport sample cannot replace the visible player");
        Vec3 next = destination.add(1,0,0);
        path.sample(next,4);
        h.assertTrue(path.hasVisibleSegment(next) && path.opacity(4)>0,
                "Movement after the teleport starts a fresh visible wake");
        h.assertTrue(!path.hasVisibleSegment(destination) && path.points(destination).size()==1,
                "Interpolation onto the previous sample cannot hide a player behind a collapsed segment");

        Vec3 turn = next.add(0,0,1);
        path.sample(turn,5);
        h.assertTrue(path.hasVisibleSegment(next) && path.points(next).size()==2,
                "A longer turned wake remains visible when its interpolated head overlaps one sample");
        Vec3 invalid = new Vec3(Double.NaN,60,-300);
        h.assertTrue(!path.connectedTo(invalid) && path.points(invalid).equals(path.points(null)),
                "An invalid interpolated position never connects or warps the sampled path");
        h.succeed();
    }

    @GameTest(template="fate_test")
    public static void verticalFlightBasisStaysFiniteAndDoesNotFlip(GameTestHelper h) {
        Vec3 previous=null;
        for (int i=0;i<=80;i++) {
            double angle=i*Math.PI/80;
            Vec3 tangent=new Vec3(Math.cos(angle),Math.sin(angle),.001).normalize();
            Vec3 side=CultivationFlightTrail.side(tangent,previous);
            h.assertTrue(CultivationFlightTrail.finite(side)&&Math.abs(side.length()-1)<.00001&&Math.abs(side.dot(tangent))<.00001,
                    "Ribbon frame remains perpendicular through climb, apex and descent");
            if(previous!=null)h.assertTrue(previous.dot(side)>0,"Vertical motion does not invert the ribbon frame");
            previous=side;
        }
        h.succeed();
    }

    @GameTest(template="fate_test")
    public static void visualDescriptorPreservesWorldIdentityColorAndStop(GameTestHelper h) {
        var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),h.getLevel().registryAccess());
        try {
            for(float strength:new float[]{1.3F,0}) {
                var packet=new CultivationFlightPayloads.Trail(h.getLevel().dimension().location(),123,UUID.randomUUID(),0xD898FF,strength);
                CultivationFlightPayloads.Trail.CODEC.encode(buffer,packet);
                h.assertTrue(packet.equals(CultivationFlightPayloads.Trail.CODEC.decode(buffer)),
                        "Observers receive the same dimension, entity UUID, element color and stop signal");
            }
        } finally {buffer.release();}
        h.succeed();
    }

    @GameTest(template="fate_test")
    public static void onlyRealAirborneCultivationFlightReceivesElementalWake(GameTestHelper h) {
        var player=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"flight-visual-test"));
        player.setGameMode(GameType.SURVIVAL);player.setOnGround(false);
        h.assertTrue(!CultivationFlightVisuals.eligible(player),"Falling or walking never creates cultivation trails");
        player.addEffect(new MobEffectInstance(ModEffects.YUFENG_JUE,100));
        h.assertTrue(CultivationFlightVisuals.eligible(player),"Real cultivation flight activates the effect");
        ModAttachments.setData(player,ModAttachments.getData(player).withSpiritRoots(List.of(SpiritRoots.WOOD),SpiritRoots.GRADE_HEAVEN));
        h.assertTrue(CultivationFlightVisuals.color(player)==0x8AFF77,"Wood flight matches the green reference");
        ModAttachments.setData(player,ModAttachments.getData(player).withSpiritRoots(List.of(SpiritRoots.THUNDER),SpiritRoots.GRADE_HEAVEN));
        h.assertTrue(CultivationFlightVisuals.color(player)==0xD898FF,"Thunder flight matches the purple reference");
        player.addEffect(new MobEffectInstance(ModEffects.FROST_FLIGHT,100));
        h.assertTrue(CultivationFlightVisuals.color(player)==0x91E9FF,"Frost flight uses the icy-blue reference");
        player.setOnGround(true);h.assertTrue(!CultivationFlightVisuals.eligible(player),"Landing stops emission");
        player.setOnGround(false);player.setGameMode(GameType.CREATIVE);
        h.assertTrue(CultivationFlightVisuals.eligible(player),"Explicit flight spells still show their wake in Creative mode");
        player.removeAllEffects();
        h.assertTrue(!CultivationFlightVisuals.eligible(player),"Vanilla creative flight is excluded");h.succeed();
    }

    private static FakePlayer airbornePlayer(GameTestHelper h) {
        var player=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"flight-skill-test"));
        player.setGameMode(GameType.SURVIVAL);player.setOnGround(false);
        ModAttachments.setData(player,ModAttachments.getData(player).withQi(10000));
        return player;
    }

    @GameTest(template="fate_test")
    public static void windAndFrostSkillLifecyclesDriveTrailsWithoutJindanFlight(GameTestHelper h) {
        var p=airbornePlayer(h);
        try {
            YufengJue.activateTimed(p,100);
            int qi=ModAttachments.getData(p).qi();
            h.assertTrue(CultivationFlightVisuals.eligible(p),"Timed/talisman wind flight activates the shared wake");
            h.assertTrue(ModAttachments.getData(p).qi()==qi,"Visual eligibility never charges extra Qi");
            YufengJue.clear(p);
            h.assertTrue(!CultivationFlightVisuals.eligible(p),"Cancelling wind flight stops the wake");
            ModAttachments.setData(p,ModAttachments.getData(p).withActiveCultivationMethod(CultivationMethods.HANTI_BINGQIN).withQi(10000));
            HantiBingqin.toggleFrostFlight(p);
            h.assertTrue(CultivationFlightVisuals.eligible(p)&&CultivationFlightVisuals.color(p)==0x91E9FF,
                    "Frost glide uses the same ribbons and icy color without mayfly");
            HantiBingqin.clear(p);
            h.assertTrue(!CultivationFlightVisuals.eligible(p),"Residual slow falling after frost flight creates no wake");
        } finally {YufengJue.clear(p);HantiBingqin.clear(p);}
        h.succeed();
    }

    @GameTest(template="fate_test")
    public static void otherGrantedFlightNeedsActualFlightAndIgnoresOrdinaryFalling(GameTestHelper h) {
        var p=airbornePlayer(h);
        p.getAbilities().mayfly=true;
        h.assertTrue(!CultivationFlightVisuals.eligible(p),"Permission alone is not an active flight skill");
        p.getAbilities().flying=true;
        h.assertTrue(CultivationFlightVisuals.eligible(p),"Unlisted skills granting survival flight receive the same wake");
        p.setOnGround(true);
        h.assertTrue(!CultivationFlightVisuals.eligible(p),"Landing stops an unlisted skill's wake");
        p.setOnGround(false);p.getAbilities().flying=false;
        h.assertTrue(!CultivationFlightVisuals.eligible(p),"Falling after flight is disabled does not emit");
        p.setGameMode(GameType.SPECTATOR);p.addEffect(new MobEffectInstance(ModEffects.YUFENG_JUE,100));
        h.assertTrue(!CultivationFlightVisuals.eligible(p),"Spectator mode remains invisible to flight VFX");h.succeed();
    }

    @GameTest(template="fate_test")
    public static void swordFlightUsesPlayerGroundStateAndRetainsOrdinaryFlight(GameTestHelper h) {
        var p=new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(),h.getLevel(),
                new GameProfile(UUID.randomUUID(),"sword-flight-test"),net.minecraft.server.level.ClientInformation.createDefault());
        p.connection=airbornePlayer(h).connection;
        var pos=h.absolutePos(new net.minecraft.core.BlockPos(4,20,4));
        p.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
        ModAttachments.setData(p,ModAttachments.getData(p).withCultivationLevel(
                CultivationLevels.REALM_JINDAN+CultivationLevels.STAGE_EARLY).withQi(10000));
        h.assertTrue(FlyingSwordEntity.releaseControlledFromCase(p,java.util.List.of(
                new net.minecraft.world.item.ItemStack(com.example.immortal_cultivation_mod.item.ModItems.FLYING_SWORD.get())))==1,
                "Real owned blade released");
        h.assertTrue(FlyingSwordEntity.toggleRiding(p),"Sword assists ordinary creative-style flight");
        var sword=FlyingSwordEntity.flightSword(p);
        p.getAbilities().flying=true;p.setOnGround(false);
        p.getAbilities().setFlyingSpeed(1F);
        p.addEffect(new MobEffectInstance(ModEffects.YUFENG_JUE,100));
        p.addEffect(new MobEffectInstance(ModEffects.FROST_FLIGHT,100));
        h.assertTrue(!CultivationFlightVisuals.eligible(p)&&!CultivationFlightVisuals.shouldEmit(p,3)&&!p.isPassenger(),"Sword flight suppresses trails even at maximum speed with other flight effects");
        p.setOnGround(true);
        h.assertTrue(!CultivationFlightVisuals.eligible(p),"Grounded player does not emit flight wake");
        p.setOnGround(false);FlyingSwordEntity.toggleRiding(p);
        h.assertTrue(CultivationFlightVisuals.eligible(p),"Removing sword keeps ordinary Golden Core flight wake");
        p.removeEffect(ModEffects.YUFENG_JUE);p.removeEffect(ModEffects.FROST_FLIGHT);
        JindanCreativeFlight.setEnabled(p,false);
        h.assertTrue(!CultivationFlightVisuals.eligible(p),"Disabling flight ends wake");
        sword.discard();h.succeed();
    }

    @GameTest(template="fate_test")
    public static void npcFlightHasNoTrailWhileFlyingUndeadKeepsItsVisuals(GameTestHelper h) {
        var npc=ModEntities.LIANQI_CULTIVATOR.get().create(h.getLevel());
        npc.setOnGround(false);npc.addEffect(new MobEffectInstance(ModEffects.YUFENG_JUE,100));
        h.assertTrue(!CultivationFlightVisuals.eligible(npc)&&!CultivationFlightVisuals.shouldEmit(npc,3),"NPC wind flight never emits a trail");
        npc.addEffect(new MobEffectInstance(ModEffects.FROST_FLIGHT,100));
        h.assertTrue(!CultivationFlightVisuals.eligible(npc),"NPC frost flight cannot bypass trail suppression");
        npc.removeEffect(ModEffects.FROST_FLIGHT);
        npc.removeEffect(ModEffects.YUFENG_JUE);
        h.assertTrue(!CultivationFlightVisuals.eligible(npc),"An airborne NPC without its flight spell is only falling");
        var zombie=net.minecraft.world.entity.EntityType.ZOMBIE.create(h.getLevel());
        zombie.setOnGround(false);zombie.setNoGravity(true);
        zombie.getPersistentData().putLong(UndeadControl.TAG_GRUDGE,12500);
        h.assertTrue(CultivationFlightVisuals.eligible(zombie)&&CultivationFlightVisuals.color(zombie)==0xD898FF,
                "Flying-rank undead use the purple wake during real no-gravity flight");
        zombie.setNoGravity(false);
        h.assertTrue(!CultivationFlightVisuals.eligible(zombie),"An undead creature's rank alone does not create a flying wake");h.succeed();
    }

    @GameTest(template="fate_test")
    public static void trailsRequireFiveTimesFlightSpeedAndStopBelowIt(GameTestHelper h) {
        var p=airbornePlayer(h);p.getAbilities().mayfly=true;p.getAbilities().flying=true;
        for(float speed:new float[]{.05F,.10F,.25F,.4F,.499F}) {
            p.getAbilities().setFlyingSpeed(speed);
            h.assertTrue(!CultivationFlightVisuals.shouldEmit(p,3),"Fast displacement cannot bypass a selected speed below x10");
        }
        for(float speed:new float[]{.5F,.75F,1F}) {
            p.getAbilities().setFlyingSpeed(speed);
            h.assertTrue(CultivationFlightVisuals.shouldEmit(p,.6),"Trails start at exactly x10 and continue above it");
        }
        h.assertTrue(!CultivationFlightVisuals.shouldEmit(p,0)&&!CultivationFlightVisuals.shouldEmit(p,100)
                &&!CultivationFlightVisuals.shouldEmit(p,Double.NaN),"Hovering, teleports and invalid movement never emit");
        p.getAbilities().setFlyingSpeed(.225F);
        h.assertTrue(!CultivationFlightVisuals.shouldEmit(p,.6),"Reducing flight speed below x10 stops emission immediately");h.succeed();
    }

    @GameTest(template="fate_test")
    public static void frostKeepsSpeedThresholdWhileNpcFlightStaysHidden(GameTestHelper h) {
        var p=airbornePlayer(h);p.addEffect(new MobEffectInstance(ModEffects.FROST_FLIGHT,100));
        p.getAbilities().setFlyingSpeed(1F);
        var npc=ModEntities.LIANQI_CULTIVATOR.get().create(h.getLevel());
        npc.setOnGround(false);npc.addEffect(new MobEffectInstance(ModEffects.YUFENG_JUE,100));
        double threshold=CultivationFlightVisuals.MIN_TRAIL_TRAVEL_SPEED;
        h.assertTrue(!CultivationFlightVisuals.shouldEmit(p,threshold-.01),"Player glide below equivalent x10 stays quiet");
        for (double speed : new double[]{0, .001, .04, .12, threshold}) {
            h.assertTrue(!CultivationFlightVisuals.shouldEmit(npc,speed),"NPC flight never emits at any speed");
        }
        h.assertTrue(CultivationFlightVisuals.shouldEmit(p,threshold),"Actual fast player glide emits");
        for(var flyer:List.of(p,npc)) {
            flyer.setOnGround(true);
            h.assertTrue(!CultivationFlightVisuals.shouldEmit(flyer,threshold),"Speed alone never emits a grounded trail");
        }
        h.succeed();
    }
}
