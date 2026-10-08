package com.example.immortal_cultivation_mod.effect;

import com.example.immortal_cultivation_mod.network.SpellRibbonPayload;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import io.netty.buffer.Unpooled;
import java.util.HashSet;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_mod")
@PrefixGameTestTemplate(false)
public final class SpellRibbonGameTests {
    @GameTest(template="fate_test")
    public static void meditationStaysCompactAcrossLongSessions(GameTestHelper h){
        for(double age:new double[]{0,16,32,119,120,72000}){
            int[] count={0};
            MeditationVfxShapes.emit(age,new SpellRibbonShapes.Sink(){
                public void segment(Vec3 a,Vec3 b,double width,float alpha){
                    h.assertTrue(Double.isFinite(a.lengthSqr())&&Double.isFinite(b.lengthSqr())&&a.length()<2.6&&b.length()<2.6,"Meditation energy escaped its seated volume");
                    h.assertTrue(width>0&&width<.1&&Float.isFinite(alpha)&&alpha>=0&&alpha<=1,"Invalid meditation ribbon");count[0]++;
                }
                public void mote(Vec3 p,double radius,int tint,float alpha){h.assertTrue(p.length()<2.6&&radius>0&&radius<.1&&alpha>=0&&alpha<=1,"Invalid meditation mote");}
            },28);
            h.assertTrue(count[0]>100&&count[0]<400,"Meditation exceeds its geometry budget");
        }
        h.succeed();
    }
    @GameTest(template="fate_test",timeoutTicks=20)
    public static void missedSpellProjectileExpiresThroughTickEvent(GameTestHelper h){
        var projectile=new com.example.immortal_cultivation_mod.entity.WindBladeProjectileEntity(
                com.example.immortal_cultivation_mod.entity.ModEntities.WIND_BLADE_PROJECTILE.get(),h.getLevel());
        projectile.setPos(h.absoluteVec(new Vec3(1,12,1)));projectile.setNoGravity(true);
        projectile.tickCount=199;h.getLevel().addFreshEntity(projectile);
        h.runAfterDelay(4,()->{h.assertTrue(projectile.isRemoved(),"Missed wind blade must expire without hitting anything");h.succeed();});
    }
    @GameTest(template="fate_test",timeoutTicks=20)
    public static void waitingOrbDeadlineSurvivesReload(GameTestHelper h){
        var original=new com.example.immortal_cultivation_mod.entity.LightBeamProjectileEntity(
                com.example.immortal_cultivation_mod.entity.ModEntities.LIGHT_BEAM_PROJECTILE.get(),h.getLevel());
        original.tickCount=599;
        h.assertTrue(!com.example.immortal_cultivation_mod.entity.SpellProjectileLifetime.expire(original),"Orb expired early");
        var saved=original.saveWithoutId(new net.minecraft.nbt.CompoundTag());
        var reloaded=new com.example.immortal_cultivation_mod.entity.LightBeamProjectileEntity(
                com.example.immortal_cultivation_mod.entity.ModEntities.LIGHT_BEAM_PROJECTILE.get(),h.getLevel());
        reloaded.load(saved);
        h.runAfterDelay(3,()->{
            h.assertTrue(com.example.immortal_cultivation_mod.entity.SpellProjectileLifetime.expire(reloaded),"Reload reset the waiting orb lifetime");
            h.assertTrue(com.example.immortal_cultivation_mod.entity.SpellProjectileLifetime.maximumTicks(new net.minecraft.world.entity.projectile.Arrow(net.minecraft.world.entity.EntityType.ARROW,h.getLevel()))==0,"Vanilla arrows must keep their own lifecycle");
            h.succeed();
        });
    }
    @GameTest(template="fate_test")
    public static void lightBallIsRoundAndWindImpactEnds(GameTestHelper h){
        int[] faces={0},lateWind={0};double[] extents={0,0,0};
        SpellRibbonShapes.emit("light_beam_attack",16,new SpellRibbonShapes.Sink(){
            public void segment(Vec3 a,Vec3 b,double width,float alpha){h.assertTrue(a.length()<.6&&b.length()<.6,"Light Ball still contains a beam");}
            public void patch(Vec3 a,Vec3 b,Vec3 c,Vec3 d,int tint,float alpha){for(Vec3 p:java.util.List.of(a,b,c,d)){extents[0]=Math.max(extents[0],Math.abs(p.x));extents[1]=Math.max(extents[1],Math.abs(p.y));extents[2]=Math.max(extents[2],Math.abs(p.z));}faces[0]++;}
        },24);
        h.assertTrue(faces[0]>=100&&Math.abs(extents[0]-extents[1])<.02&&Math.abs(extents[1]-extents[2])<.02,"Light Ball needs a spherical surface");
        SpellRibbonShapes.emit("tianfeng_zhui",60,(a,b,w,alpha)->lateWind[0]++,24);
        h.assertTrue(lateWind[0]==0,"Wind impact must finish rather than repeat while fall damage is tracked");h.succeed();
    }
    @GameTest(template="fate_test")
    public static void allRequestedSpellsHaveDistinctBoundedAnimatedPaths(GameTestHelper h){
        h.assertTrue(SpellRibbonShapes.SPELLS.size()==30,"All thirty requested spell IDs are covered");
        var signatures=new HashSet<Long>();
        for(String id:SpellRibbonShapes.SPELLS){
            h.assertTrue(ModSpells.get(id)!=null,"Unknown gameplay spell: "+id);
            long first=signature(h,id,3),later=signature(h,id,9);
            h.assertTrue(first!=later,"No animation in "+id);
            h.assertTrue(signatures.add(first),"Duplicate spell silhouette: "+id);
        }
        h.succeed();
    }
    private static long signature(GameTestHelper h,String id,double time){
        long[] hash={7};int[] segments={0};
        SpellRibbonShapes.emit(id,time,(a,b,w,alpha)->{
            h.assertTrue(Double.isFinite(a.lengthSqr())&&Double.isFinite(b.lengthSqr()),"Nonfinite path: "+id);
            h.assertTrue(a.length()<35&&b.length()<35&&w>0&&w<1,"Unbounded geometry: "+id);
            h.assertTrue(alpha>=0&&alpha<=1,"Invalid opacity: "+id);
            hash[0]=hash[0]*31+a.hashCode();hash[0]=hash[0]*31+b.hashCode();segments[0]++;
        },24);
        h.assertTrue(segments[0]>12&&segments[0]<1000,"Missing/excessive geometry: "+id);
        return hash[0];
    }
    @GameTest(template="fate_test")
    public static void visualPacketRetainsDimensionAnchorAndShieldRadius(GameTestHelper h){
        var p=new SpellRibbonPayload(h.getLevel().dimension().location(),"yuangang_hujie",new Vec3(12,64,-8),new Vec3(0,0,1),42,10,7.5F);
        var buf=new RegistryFriendlyByteBuf(Unpooled.buffer(),h.getLevel().registryAccess());
        try{
            SpellRibbonPayload.CODEC.encode(buf,p);
            h.assertTrue(p.equals(SpellRibbonPayload.CODEC.decode(buf)),"Visual payload round trip lost anchor, duration or scale");
        }finally{buf.release();}
        h.succeed();
    }
    @GameTest(template="fate_test")
    public static void detailedSpellSurfacesAndSparksStayFinite(GameTestHelper h){
        for(String id:SpellRibbonShapes.SPELLS){
            int[] surfaces={0};
            SpellRibbonShapes.emit(id,23,new SpellRibbonShapes.Sink(){
                @Override public void segment(Vec3 a,Vec3 b,double width,float alpha){}
                @Override public void patch(Vec3 a,Vec3 b,Vec3 c,Vec3 d,int tint,float alpha){
                    for(Vec3 p:java.util.List.of(a,b,c,d))h.assertTrue(Double.isFinite(p.lengthSqr())&&p.length()<40,"Invalid surface in "+id);
                    h.assertTrue(Float.isFinite(alpha)&&alpha>=0&&alpha<=1,"Invalid surface opacity: "+id);surfaces[0]++;
                }
                @Override public void mote(Vec3 p,double radius,int tint,float alpha){
                    h.assertTrue(Double.isFinite(p.lengthSqr())&&p.length()<40&&radius>0&&radius<2,"Invalid spark in "+id);
                }
            },24);
            if(java.util.Set.of("pokong_jianhen","duanliu_kongdun","yuangang_hujie","sifang_jie","wind_blade","muling_shengxi","ignite_flare","jinhuo_xinliao").contains(id))
                h.assertTrue(surfaces[0]>0,"Rift, leaf, blade or barrier surface missing: "+id);
        }
        h.succeed();
    }
}
