package com.example.immortal_cultivation_mod.physics;

import com.example.immortal_cultivation_mod.entity.ModEntities;
import com.example.immortal_cultivation_mod.animation.SpiritPuppetAnimations;
import com.example.immortal_cultivation_mod.animation.SpiritPuppetAnimations.Clip;
import com.example.immortal_cultivation_mod.training.TrainingDummyGameTests;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import javax.imageio.ImageIO;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import software.bernie.geckolib.animatable.GeoEntity;

@GameTestHolder("immortal_cultivation_puppet")
@PrefixGameTestTemplate(false)
public final class SpiritPuppetGameTests {
    @GameTest(template="fate_test")
    public static void allAnimationTracksMatchRuntimeAndLeaveClothPhysical(GameTestHelper h)throws Exception {
        var clips=asset("animations/entity/spirit_puppet.animation.json").getAsJsonObject("animations");
        h.assertTrue(clips.size()==18,"Complete set of 18 runtime animations");
        for(Clip expected:Clip.values()) {
            var clip=clips.getAsJsonObject(expected.name);h.assertTrue(clip!=null,"Controller clip exists: "+expected.name);
            h.assertTrue(Math.abs(clip.get("animation_length").getAsDouble()-expected.seconds)<.0001,"Runtime timing matches authored clip");
            var tracks=clip.getAsJsonObject("bones");
            for(var row:tracks.entrySet()) {
                h.assertTrue(!row.getKey().contains("_flex_"),"Body clips never replace live cloth simulation");
                for(var channel:row.getValue().getAsJsonObject().entrySet()) {
                    var value=channel.getValue();
                    if(value.isJsonObject()) {
                        com.google.gson.JsonArray first=null,last=null;
                        for(var key:value.getAsJsonObject().entrySet()) {
                            double time=Double.parseDouble(key.getKey());
                            h.assertTrue(time>=0&&time<=expected.seconds+.00001,"All keyframes lie within clip length");
                            var vector=key.getValue().getAsJsonArray();if(first==null)first=vector;last=vector;
                            for(var v:vector)h.assertTrue(Double.isFinite(v.getAsDouble()),"Finite animation component");
                        }
                        if(expected.loop)for(int i=0;i<3;i++)h.assertTrue(Math.abs(first.get(i).getAsDouble()-last.get(i).getAsDouble())<.002,"Loop joins without a pose snap");
                    }else for(var v:value.getAsJsonArray())h.assertTrue(Double.isFinite(v.getAsDouble()),"Finite constant channel");
                }
            }
        }
        h.succeed();
    }
    @GameTest(template="fate_test")
    public static void actionsInterruptRecoverAndRepeatWithoutSticking(GameTestHelper h) {
        var state=new SpiritPuppetAnimations();
        h.assertTrue(select(state,0,true,true,false,false,0,0,0,0,0,0).clip()==Clip.IDLE,"Ground idle");
        h.assertTrue(select(state,1,true,true,false,false,0,.12,0,0,0,0).clip()==Clip.WALK,"Walk");
        h.assertTrue(select(state,2,true,true,false,false,0,.3,0,0,0,0).clip()==Clip.RUN,"Run");
        h.assertTrue(select(state,3,true,true,false,false,0,-.1,0,0,0,0).clip()==Clip.WALK_BACK,"Backstep");
        h.assertTrue(select(state,4,true,true,false,false,0,0,-.1,0,0,0).clip()==Clip.STRAFE_LEFT,"Left strafe");
        h.assertTrue(select(state,5,true,true,false,false,0,0,.1,0,0,0).clip()==Clip.STRAFE_RIGHT,"Right strafe");
        h.assertTrue(select(state,6,true,false,false,false,.2,0,0,0,0,0).clip()==Clip.JUMP,"Jump ascent");
        h.assertTrue(select(state,6.5,true,false,false,false,-.2,0,0,0,0,0).clip()==Clip.FALL,"Falling");
        h.assertTrue(select(state,7,true,true,false,false,0,0,0,0,0,0).clip()==Clip.LAND,"Landing after airborne movement");
        h.assertTrue(select(state,8,true,false,true,false,0,0,0,0,0,0).clip()==Clip.FLY,"Flight");
        h.assertTrue(select(state,9,true,false,false,true,0,0,0,0,0,0).clip()==Clip.SWIM,"Water overrides flight and falling");
        h.assertTrue(select(state,10,true,true,false,false,0,0,0,0,.1F,0).clip()==Clip.ATTACK_RIGHT,"Main hand strike");
        select(state,10.2,true,true,false,false,0,0,0,0,.8F,0);
        h.assertTrue(select(state,10.3,true,true,false,false,0,0,0,0,.1F,0).restart(),"Repeated same-hand strike restarts its windup");
        state.update(new SpiritPuppetAnimations.Snapshot(11,true,true,false,false,0,0,0,false,0,0,true,0));
        h.assertTrue(state.update(new SpiritPuppetAnimations.Snapshot(11.1,true,true,false,false,0,0,0,false,0,.1F,true,0)).clip()==Clip.ATTACK_LEFT,"Offhand strike");
        h.assertTrue(select(state,12,true,true,false,false,0,0,0,0,0,30).clip()==Clip.CAST_CHARGE,"Casting starts with charge");
        h.assertTrue(select(state,12.6,true,true,false,false,0,0,0,0,0,18).clip()==Clip.CAST_HOLD,"Long cast sustains seal");
        h.assertTrue(select(state,13,true,true,false,false,0,0,0,0,0,0).clip()==Clip.CAST_RELEASE,"Cast finishes with release");
        h.assertTrue(select(state,13.6,true,true,false,false,0,0,0,0,0,0).clip()==Clip.IDLE,"Casting does not leave arms stuck");
        h.assertTrue(select(state,14,true,true,false,false,0,0,0,10,0,0).clip()==Clip.HURT,"Hit interrupts other actions");
        select(state,14.1,true,true,false,false,0,0,0,7,0,0);
        h.assertTrue(select(state,14.2,true,true,false,false,0,0,0,10,0,0).restart(),"A second hit restarts recoil");
        h.assertTrue(select(state,15,true,true,false,false,0,0,0,0,0,0).clip()==Clip.IDLE,"Recoil recovers");
        h.assertTrue(select(state,16,false,true,false,false,0,.3,0,10,.5F,12).clip()==Clip.DEATH,"Death overrides every action");
        h.succeed();
    }
    private static SpiritPuppetAnimations.Selection select(SpiritPuppetAnimations state,double time,boolean alive,boolean ground,boolean fly,boolean water,double dy,double forward,double right,int hurt,float swing,int cast) {
        return state.update(new SpiritPuppetAnimations.Snapshot(time,alive,ground,fly,water,dy,forward,right,false,hurt,swing,false,cast));
    }
    @GameTest(template="fate_test")
    public static void packagedClothChainsMatchActualRig(GameTestHelper h)throws Exception {
        var geometry=asset("geo/entity/spirit_puppet.geo.json").getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        var bones=new HashMap<String,JsonObject>();
        for(var entry:geometry.getAsJsonArray("bones")){var b=entry.getAsJsonObject();h.assertTrue(bones.put(b.get("name").getAsString(),b)==null,"Unique puppet bone");}
        // GeckoLib eagerly parses every JSON under geo; custom rig data belongs elsewhere.
        h.assertTrue(SpiritPuppetGameTests.class.getResource("/assets/immortal_cultivation_mod/geo/entity/spirit_puppet.cloth.json")==null,"Cloth settings cannot be mistaken for geometry during client reload");
        var chains=asset("physics/spirit_puppet.cloth.json").getAsJsonArray("chains");
        h.assertTrue(chains.size()==15,"All 15 cloth strips and tassels participate in physics");
        for(var entry:chains) {
            var chain=entry.getAsJsonObject();String parent=chain.get("root").getAsString();
            h.assertTrue(bones.containsKey(parent),"Cloth root exists");
            h.assertTrue(chain.getAsJsonArray("nodes").size()==chain.getAsJsonArray("bones").size()+1,"Particle count matches articulated segments");
            for(var node:chain.getAsJsonArray("nodes"))for(var value:node.getAsJsonArray())h.assertTrue(Double.isFinite(value.getAsDouble()),"Finite rest point");
            for(var name:chain.getAsJsonArray("bones")) {
                var bone=bones.get(name.getAsString());h.assertTrue(bone!=null&&bone.get("parent").getAsString().equals(parent),"Continuous cloth bone hierarchy");parent=name.getAsString();
            }
        }
        h.assertTrue(!bones.containsKey("sword")&&bones.containsKey("finger_l_index_base")&&bones.containsKey("thumb_r_tip"),"Sword-free articulated hands retained");
        try(var atlas=SpiritPuppetGameTests.class.getResourceAsStream("/assets/immortal_cultivation_mod/textures/entity/spirit_puppet.png");
            var glow=SpiritPuppetGameTests.class.getResourceAsStream("/assets/immortal_cultivation_mod/textures/entity/spirit_puppet_glowmask.png")) {
            var image=ImageIO.read(atlas);var mask=ImageIO.read(glow);
            h.assertTrue(image.getWidth()==128&&image.getHeight()==128&&mask.getWidth()==128&&mask.getHeight()==128,"Base and glow atlases share UV dimensions");
            for(var bone:bones.values())if(bone.has("cubes"))for(var cube:bone.getAsJsonArray("cubes"))
                for(var face:cube.getAsJsonObject().getAsJsonObject("uv").entrySet()) {
                    var tile=face.getValue().getAsJsonObject();
                    for(int i=0;i<2;i++){double start=tile.getAsJsonArray("uv").get(i).getAsDouble(),end=start+tile.getAsJsonArray("uv_size").get(i).getAsDouble();
                        h.assertTrue(Math.min(start,end)>=0&&Math.max(start,end)<=128,"Polished atlas retains valid face UVs");}
                }
        }
        h.succeed();
    }
    @GameTest(template="fate_test")
    public static void dummyUsesPuppetWithoutChangingItsPhysicalSize(GameTestHelper h) {
        var dummy=ModEntities.TRAINING_DUMMY.get().create(h.getLevel());
        try {
            h.assertTrue(dummy instanceof GeoEntity&&dummy.getAnimatableInstanceCache()!=null,"Dummy has GeckoLib animation instance");
            h.assertTrue(Math.abs(dummy.getBbWidth()-.6)<.0001&&Math.abs(dummy.getBbHeight()-1.9)<.0001,"Original dummy collision dimensions retained");
            dummy.beginNpcCast(12);h.assertTrue(dummy.npcCastingTicks()==12,"Casting visual still uses synchronized dummy state");
            dummy.endNpcCast();h.assertTrue(dummy.npcCastingTicks()==0,"Casting can stop normally");
            dummy.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            for(int i=0;i<4;i++)dummy.aiStep();
            h.assertTrue(dummy.getAttackAnim(1)>0,"Pathfinder dummy advances its melee swing clock");
            for(int i=0;i<8;i++)dummy.aiStep();
            h.assertTrue(!dummy.swinging&&dummy.getAttackAnim(1)==0,"Melee swing clock finishes and can restart");
        }finally{dummy.discard();}
        h.succeed();
    }
    @GameTest(template="fate_test")
    public static void dummyKnowledgeAndOwnerStillSurviveReload(GameTestHelper h) {
        TrainingDummyGameTests.exactKnowledgeDoesNotRandomizeAcrossProfileChecksAndReload(h);
    }
    private static JsonObject asset(String path)throws Exception {
        try(var stream=SpiritPuppetGameTests.class.getResourceAsStream("/assets/immortal_cultivation_mod/"+path);
            var reader=new InputStreamReader(stream,StandardCharsets.UTF_8)){return JsonParser.parseReader(reader).getAsJsonObject();}
    }
}
