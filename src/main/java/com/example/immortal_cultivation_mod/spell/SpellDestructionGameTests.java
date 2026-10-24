package com.example.immortal_cultivation_mod.spell;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_mod")
@PrefixGameTestTemplate(false)
public final class SpellDestructionGameTests {
    @GameTest(template="npc_combat_arena")
    public static void externalQiQueueKeepsItsBlockPolicy(GameTestHelper h) throws Exception {
        var level = h.getLevel();
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.get(level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "QiBlockTest"));
        player.setPos(h.absoluteVec(new Vec3(8, 6, 8)));
        BlockPos obsidian = h.absolutePos(new BlockPos(12, 6, 12));
        BlockPos ding = obsidian.offset(1, 0, 0);
        BlockPos bedrock = obsidian.offset(2, 0, 0);
        BlockPos barrier = obsidian.offset(3, 0, 0);
        level.setBlock(obsidian, Blocks.OBSIDIAN.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(ding, com.example.immortal_cultivation_mod.block.ModBlocks.CU_TIE_XIAODING.get().defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(bedrock, Blocks.BEDROCK.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(barrier, com.example.immortal_cultivation_mod.block.ModBlocks.SUOKONG_BARRIER.get().defaultBlockState(), Block.UPDATE_CLIENTS);
        h.assertTrue(!SpellDestruction.canBreakBlock(level, obsidian, player)
                        && SpellDestruction.canBreakBlock(level, ding, player),
                "Spells can break machinery while retaining obsidian restrictions");
        h.assertTrue(SpellDestruction.canExternalQiBreakBlock(level, obsidian, player)
                        && SpellDestruction.canExternalQiBreakBlock(level, ding, player),
                "External Qi accepts obsidian and mod machinery");
        h.assertTrue(!SpellDestruction.canExternalQiBreakBlock(level, bedrock, player)
                        && !SpellDestruction.canExternalQiBreakBlock(level, barrier, player),
                "Bedrock and unbreakable mod barriers remain protected");
        // Force the normal per-tick probe limit, exercising deferred removal rather than only immediate cuts.
        SpellDestruction.hasBudget(level);
        var probes = SpellDestruction.class.getDeclaredField("probes");
        probes.setAccessible(true);
        int previous = probes.getInt(null);
        try {
            probes.setInt(null, SpellDestruction.MAX_PROBES);
            h.assertTrue(SpellDestruction.breakExternalQiBlock(level, obsidian, false, player)
                            && SpellDestruction.breakExternalQiBlock(level, ding, false, player),
                    "Qi terrain and machinery cuts are queued");
            h.assertTrue(!level.getBlockState(obsidian).isAir() && !level.getBlockState(ding).isAir(),
                    "Queued blocks stay intact until work is processed");
        } finally {
            probes.setInt(null, previous);
        }
        h.runAfterDelay(3, () -> {
            h.assertTrue(level.getBlockState(obsidian).isAir() && level.getBlockState(ding).isAir(),
                    "Deferred cuts destroy terrain and machinery");
            h.assertTrue(level.getBlockState(bedrock).is(Blocks.BEDROCK), "Bedrock survives");
            player.discard(); h.succeed();
        });
    }

    @GameTest(template="fate_test")
    public static void beastDestructionAmplifiesOnlyGoldenCoreAndHigher(GameTestHelper h) {
        var level=h.getLevel();
        var lizard=com.example.immortal_cultivation_mod.entity.ModEntities.CRIMSON_SCALE_FLAME_LIZARD.get().create(level);
        var ape=com.example.immortal_cultivation_mod.entity.ModEntities.BLOOD_APE_DEMON_GENERAL.get().create(level);
        var bear=com.example.immortal_cultivation_mod.entity.ModEntities.FROSTSPINE_CAVE_BEAR.get().create(level);
        var lowFox=com.example.immortal_cultivation_mod.entity.ModEntities.PHANTOM_VEIL_FOX.get().create(level);
        lowFox.setBeastRealm(2);
        var highFox=com.example.immortal_cultivation_mod.entity.ModEntities.PHANTOM_VEIL_FOX.get().create(level);
        h.assertTrue(com.example.immortal_cultivation_mod.entity.BeastMoveBlockBreaker.blockLimit(lizard,24)==1536,
                "Golden Core attacks get sixty-four times the old solid-block budget");
        h.assertTrue(com.example.immortal_cultivation_mod.entity.BeastMoveBlockBreaker.blockLimit(ape,24)==2304
                &&com.example.immortal_cultivation_mod.entity.BeastMoveBlockBreaker.blockLimit(highFox,24)==2304,
                "Nascent Soul beasts get ninety-six times the old budget");
        h.assertTrue(com.example.immortal_cultivation_mod.entity.BeastMoveBlockBreaker.blockLimit(bear,24)==24
                &&com.example.immortal_cultivation_mod.entity.BeastMoveBlockBreaker.blockLimit(lowFox,24)==24,
                "Foundation beasts retain their existing destruction limits");
        h.assertTrue(com.example.immortal_cultivation_mod.entity.BeastMoveBlockBreaker.blockLimit(ape,Integer.MAX_VALUE)==16384,
                "Large move budgets are capped without integer overflow");
        var rule=level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING);
        boolean previous=rule.get();rule.set(false,level.getServer());
        try {
            h.assertTrue(com.example.immortal_cultivation_mod.entity.BeastMoveBlockBreaker.breakSphere(level,lizard,
                    h.absoluteVec(new Vec3(2,2,2)),2,2,24,8)==0,"Mob griefing still disables enhanced terrain damage");
        } finally {rule.set(previous,level.getServer());}
        h.succeed();
    }

    @GameTest(template="npc_combat_arena",timeoutTicks=160)
    public static void highRealmBeastImpactsExcavateLargeVolumesAndKeepProtectedBlocks(GameTestHelper h) {
        var level=h.getLevel();
        var beasts=new net.minecraft.world.entity.Mob[]{
                com.example.immortal_cultivation_mod.entity.ModEntities.CRIMSON_SCALE_FLAME_LIZARD.get().create(level),
                com.example.immortal_cultivation_mod.entity.ModEntities.BLOOD_APE_DEMON_GENERAL.get().create(level)};
        BlockPos[] centers={h.absolutePos(new BlockPos(14,70,14)),h.absolutePos(new BlockPos(14,100,14))};
        for(int i=0;i<2;i++) {
            BlockPos center=centers[i];
            for(BlockPos pos:BlockPos.betweenClosed(center.offset(-8,-6,-8),center.offset(8,6,8)))
                level.setBlock(pos,Blocks.STONE.defaultBlockState(),Block.UPDATE_CLIENTS);
            level.setBlock(center,Blocks.BEDROCK.defaultBlockState(),Block.UPDATE_CLIENTS);
            level.setBlock(center.offset(1,0,0),Blocks.CHEST.defaultBlockState(),Block.UPDATE_CLIENTS);
            level.setBlock(center.offset(-1,0,0),Blocks.WATER.defaultBlockState(),Block.UPDATE_CLIENTS);
            beasts[i].setPos(Vec3.atCenterOf(center).add(0,15,0));
            com.example.immortal_cultivation_mod.entity.BeastMoveBlockBreaker.breakSphere(level,beasts[i],Vec3.atCenterOf(center),2.4,1.8,24,6);
        }
        h.succeedWhen(()->{
            int[] removed=new int[2];
            for(int i=0;i<2;i++) {
                BlockPos center=centers[i];
                for(BlockPos pos:BlockPos.betweenClosed(center.offset(-8,-6,-8),center.offset(8,6,8)))
                    if(level.getBlockState(pos).isAir())removed[i]++;
                h.assertTrue(level.getBlockState(center).is(Blocks.BEDROCK),"Bedrock survives beast impacts");
                h.assertTrue(level.getBlockState(center.offset(1,0,0)).isAir(),"Beast impacts destroy containers");
                h.assertTrue(level.getBlockState(center.offset(-1,0,0)).isAir(),"Water clears inside the enlarged impact");
            }
            h.assertTrue(removed[0]>200,"Golden Core impact removes hundreds of blocks instead of twenty-four: "+removed[0]);
            h.assertTrue(removed[1]>removed[0]*1.5,"Nascent Soul impact leaves a substantially larger crater: "+removed[1]);
        });
    }

    @GameTest(template="fate_test")
    public static void impactCoversFootprintWithoutSkippingOrRepeatingBlocks(GameTestHelper h) {
        AABB bounds=new AABB(-16,-12,-16,16,12,16);
        var first=new SpellFractureTraversal(bounds,1234);
        var repeat=new SpellFractureTraversal(bounds,1234);
        var different=new SpellFractureTraversal(bounds,48763);
        var seen=new java.util.HashSet<Long>();
        int differentOrder=0,deepReturns=0;
        double furthest=0;
        while(first.hasNext()) {
            BlockPos pos=first.next().immutable();
            h.assertTrue(bounds.contains(Vec3.atCenterOf(pos)),"Traversal stays inside spell bounds");
            h.assertTrue(seen.add(pos.asLong()),"Each block visited exactly once");
            h.assertTrue(pos.equals(repeat.next()),"Seed produces repeatable impact order");
            if(!pos.equals(different.next()))differentOrder++;
            double distance=Vec3.atCenterOf(pos).length();
            if(seen.size()<6000 && distance+5<furthest)deepReturns++;
            furthest=Math.max(furthest,distance);
        }
        h.assertTrue(seen.size()==32*24*32,"Every block remains reachable in the impact");
        h.assertTrue(differentOrder>1000,"Separate impacts have different growth patterns");
        h.assertTrue(deepReturns>100,"Impact visits the entire footprint without an outward sweep");
        h.succeed();
    }

    @GameTest(template="fate_test")
    public static void fracturedCraterHasSolidCoreAndUnevenEdges(GameTestHelper h) {
        Vec3 center=new Vec3(.5,.5,.5);
        int differences=0;
        for(int x=-12;x<=12;x++)for(int z=-12;z<=12;z++) {
            var pos=new BlockPos(x,0,z);
            boolean inside=SpellDestruction.fractured(pos,center,12,12,5678);
            h.assertTrue(x*x+z*z>=36 || inside,"Dense center must not turn into scattered holes");
            if(inside!=SpellDestruction.fractured(new BlockPos(-x,0,-z),center,12,12,5678))differences++;
            h.assertTrue(inside==SpellDestruction.fractured(pos,center,12,12,5678),"Stable shape across ticks");
        }
        h.assertTrue(differences>10,"Crater rim is asymmetric, not a perfect circle");
        for(int x=-12;x<=12;x++)for(int z=-12;z<=12;z++) {
            boolean entered=false,left=false;
            for(int y=-12;y<=12;y++) {
                boolean inside=SpellDestruction.fractured(new BlockPos(x,y,z),center,12,12,5678);
                h.assertTrue(!inside || !left,"Crater columns have no internal ledges or floating pockets");
                if(inside)entered=true;
                else if(entered)left=true;
                h.assertTrue(!inside || y>=-8,"Impact forms a shallow bowl rather than a deep sphere");
            }
        }
        h.succeed();
    }

    @GameTest(template="fate_test",timeoutTicks=240)
    public static void denseOverlappingAttacksAreBoundedAndPreserveProtectedBlocks(GameTestHelper h) {
        var level=h.getLevel();
        BlockPos min=h.absolutePos(new BlockPos(2,30,2));
        BlockPos max=min.offset(11,11,11);
        for(BlockPos pos:BlockPos.betweenClosed(min,max))level.setBlock(pos,Blocks.STONE.defaultBlockState(),Block.UPDATE_CLIENTS);
        BlockPos bedrock=min.offset(6,6,6),chest=min.offset(7,6,6);
        level.setBlock(bedrock,Blocks.BEDROCK.defaultBlockState(),Block.UPDATE_CLIENTS);
        level.setBlock(chest,Blocks.CHEST.defaultBlockState(),Block.UPDATE_CLIENTS);
        Vec3 center=Vec3.atLowerCornerOf(min).add(6,6,6);
        AABB bounds=new AABB(Vec3.atLowerCornerOf(min),Vec3.atLowerCornerOf(max.offset(1,1,1)));
        int[] completed={0},broken={0};
        long started=level.getGameTime();
        for(int i=0;i<2;i++)SpellDestruction.area(level,null,bounds,center,10000,10,
                pos->SpellDestruction.fractured(pos,center,8,8,98765),null,
                count->{completed[0]++;broken[0]+=count;});
        h.assertTrue(level.getBlockState(min.offset(5,5,5)).is(Blocks.STONE),"Cast only queues terrain work");
        h.onEachTick(()->{
            var stats=SpellDestruction.stats(level);
            h.assertTrue(stats.broken()<=SpellDestruction.MAX_BREAKS+131072,"Only one complete impact can overrun the scheduling budget");
            h.assertTrue(stats.queuedJobs()<=SpellDestruction.MAX_JOBS,"Impact queue remains bounded");
            if(completed[0]<2)return;
            h.assertTrue(broken[0]>700 && broken[0]<1728,"Large solid volume clears without duplicate rewards");
            h.assertTrue(level.getBlockState(bedrock).is(Blocks.BEDROCK),"Unbreakable blocks survive");
            h.assertTrue(level.getBlockState(chest).isAir(),"Containers inside the impact break");
            System.out.println("Spell destruction dense-volume check: "+broken[0]+" blocks in "+(level.getGameTime()-started)+" ticks");
            h.succeed();
        });
    }
    @GameTest(template="fate_test",timeoutTicks=240)
    public static void largeVolumeAndDeferredSupportUpdates(GameTestHelper h) {
        var level=h.getLevel();
        BlockPos min=h.absolutePos(new BlockPos(0,100,0));
        BlockPos max=min.offset(23,23,23);
        for(BlockPos pos:BlockPos.betweenClosed(min,max))level.setBlock(pos,Blocks.STONE.defaultBlockState(),Block.UPDATE_CLIENTS);
        BlockPos support=min.offset(0,24,0),torch=support.above();
        level.setBlock(support,Blocks.STONE.defaultBlockState(),Block.UPDATE_CLIENTS);
        level.setBlock(torch,Blocks.TORCH.defaultBlockState(),Block.UPDATE_CLIENTS);
        SpellDestruction.breakNow(level,support,false,null);
        long started=level.getGameTime();
        int[] broken={-1};
        long[] firstRemoval={-1};
        SpellDestruction.area(level,null,new AABB(Vec3.atLowerCornerOf(min),Vec3.atLowerCornerOf(max.offset(1,1,1))),
                Vec3.atCenterOf(min),20000,10,pos->true,pos->{
                    if(firstRemoval[0]<0)firstRemoval[0]=level.getGameTime();
                    h.assertTrue(firstRemoval[0]==level.getGameTime(),"Entire large impact completes in one server tick");
                },count->broken[0]=count);
        h.onEachTick(()->{
            if(broken[0]<0 || !level.getBlockState(torch).isAir())return;
            h.assertTrue(broken[0]==13824,"Whole dense volume clears without skipped blocks");
            System.out.println("Spell destruction large-volume check: "+broken[0]+" blocks in "+(level.getGameTime()-started)+" ticks");
            h.succeed();
        });
    }
    @GameTest(template="fate_test",timeoutTicks=100)
    public static void impactClearsAllWaterBeyondSolidLimit(GameTestHelper h) {
        var level=h.getLevel();
        BlockPos min=h.absolutePos(new BlockPos(0,70,0));
        BlockPos max=min.offset(7,3,7);
        for(BlockPos pos:BlockPos.betweenClosed(min,max)) {
            var state=(pos.getX()&1)==0?Blocks.WATER.defaultBlockState():
                    Blocks.WATER.defaultBlockState().setValue(net.minecraft.world.level.block.LiquidBlock.LEVEL,4);
            level.setBlock(pos,state,Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
        }
        BlockPos slab=min.offset(3,1,3),stone=min.offset(2,1,2),outside=max.east();
        level.setBlock(slab,Blocks.OAK_SLAB.defaultBlockState().setValue(
                net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED,true),Block.UPDATE_CLIENTS);
        level.setBlock(stone,Blocks.STONE.defaultBlockState(),Block.UPDATE_CLIENTS);
        level.setBlock(outside,Blocks.WATER.defaultBlockState(),Block.UPDATE_CLIENTS);
        SpellDestruction.area(level,null,new AABB(Vec3.atLowerCornerOf(min),Vec3.atLowerCornerOf(max.offset(1,1,1))),
                Vec3.atCenterOf(min),1,10,pos->true,null,count->{
                    h.assertTrue(count==1,"Water must not consume the solid-block budget");
                    for(BlockPos pos:BlockPos.betweenClosed(min,max))
                        h.assertTrue(!level.getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER),
                                "Source, flowing, and waterlogged water all clear in the same impact");
                    h.assertTrue(level.getFluidState(outside).is(net.minecraft.tags.FluidTags.WATER),"Water outside impact stays intact");
                    h.succeed();
                });
    }
}
