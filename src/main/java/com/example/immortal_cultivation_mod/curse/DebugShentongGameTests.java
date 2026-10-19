package com.example.immortal_cultivation_mod.curse;

import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.network.DebugShentongPayloads;
import com.example.immortal_cultivation_mod.shentong.ShentongRegistry;
import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_curse")
@PrefixGameTestTemplate(false)
public final class DebugShentongGameTests {
    private static FakePlayer player(GameTestHelper h) {
        var p = new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"debug-shentong"));
        p.setGameMode(GameType.SURVIVAL);
        ModAttachments.setData(p,ModAttachments.getData(p).withAddedMaxHpBonus(80));
        ServerEvents.syncPlayerData(p); p.setHealth(p.getMaxHealth());
        return p;
    }
    private static boolean edit(FakePlayer editor, FakePlayer target, String field, int value) {
        return DebugShentongEditor.edit(editor,target,AnnihilationCurse.ABILITY,field,value);
    }

    @GameTest(template="fate_test")
    public static void debugGrantAndRemovalChangeRealPassiveAndPreserveOtherSpells(GameTestHelper h) {
        var editor = player(h); var target = player(h);
        try {
            editor.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(ModItems.DEBUG_STAT_EDITOR.get()));
            ModAttachments.setData(target,ModAttachments.getData(target).withKnownSpells(List.of("fireball","wind_step")));
            h.assertTrue(edit(editor,target,"add",0),"Debug editor grants special ability to selected target");
            h.assertTrue(AnnihilationCurse.owned(target)&&AnnihilationCurse.canRescue(target),"Grant activates real passive ownership");
            h.assertTrue(!AnnihilationCurse.owned(editor),"Editing another target never grants ability to editor");
            h.assertTrue(AnnihilationCurse.rescue(target),"Granted passive performs its real rescue");
            int cooldown = AnnihilationCurse.cooldown(target);
            edit(editor,target,"add",0);
            h.assertTrue(AnnihilationCurse.cooldown(target)==cooldown,"Repeated grant preserves existing cooldown");
            float hp = target.getHealth();
            h.assertTrue(edit(editor,target,"remove",0),"Remove is accepted");
            h.assertTrue(!AnnihilationCurse.owned(target)&&!AnnihilationCurse.canRescue(target)
                    &&!AnnihilationCurse.healingLocked(target)&&AnnihilationCurse.coverage(target)==0
                    &&AnnihilationCurse.blackHp(target)==0&&AnnihilationCurse.cooldown(target)==0,
                    "Removal clears passive, curse, black HP, healing lock and cooldown");
            h.assertTrue(!target.getPersistentData().contains(AnnihilationCurse.TAG)&&target.getHealth()==hp,
                    "Removal deletes saved ability state without changing current HP");
            h.assertTrue(ModAttachments.getData(target).knownSpells().equals(List.of("fireball","wind_step")),
                    "Ordinary spells remain intact");
        } finally {editor.discard();target.discard();}
        h.succeed();
    }

    @GameTest(template="fate_test")
    public static void debugEditsClampPersistAndSyncSelectedTarget(GameTestHelper h) {
        var p = player(h);
        try {
            p.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(ModItems.DEBUG_STAT_EDITOR.get()));
            edit(p,p,"add",0);
            edit(p,p,"cooldown",Integer.MAX_VALUE);
            h.assertTrue(AnnihilationCurse.cooldown(p)==AnnihilationCurse.RESCUE_COOLDOWN,"Cooldown clamps before tick conversion");
            edit(p,p,"cooldown",45); edit(p,p,"coverage",37); edit(p,p,"black_hp",25);
            var snapshot = DebugShentongPayloads.snapshot(p);
            var ability = snapshot.abilities().stream().filter(a -> a.id().equals(AnnihilationCurse.ABILITY)).findFirst().orElseThrow();
            var values = ability.fields().stream().collect(java.util.stream.Collectors.toMap(DebugShentongPayloads.FieldState::id,DebugShentongPayloads.FieldState::value));
            h.assertTrue(snapshot.targetId().equals(p.getUUID())&&ability.owned()&&values.get("cooldown")==45
                    &&values.get("coverage")==37&&values.get("black_hp")==25&&values.get("healing_lock")==1,
                    "Editor snapshot reports actual target values and units");
            var saved = p.saveWithoutId(new CompoundTag());
            var restored = player(h);
            try {
                restored.load(saved);
                h.assertTrue(AnnihilationCurse.owned(restored)&&AnnihilationCurse.cooldown(restored)==900
                        &&Math.abs(AnnihilationCurse.coverage(restored)-.37F)<.001,
                        "Ability edits survive player save/load");
            } finally {restored.discard();}
            edit(p,p,"healing_lock",0);
            h.assertTrue(!AnnihilationCurse.healingLocked(p)&&AnnihilationCurse.blackHp(p)==0,"Unlocking healing clears black HP marker");
            edit(p,p,"coverage",1000); h.assertTrue(AnnihilationCurse.coverage(p)==1,"Coverage upper bound");
            edit(p,p,"coverage",-1); edit(p,p,"cooldown",-1);
            h.assertTrue(AnnihilationCurse.coverage(p)==0&&AnnihilationCurse.cooldown(p)==0,"Negative values clamp to zero");
        } finally {p.discard();}
        h.succeed();
    }

    @GameTest(template="fate_test")
    public static void invalidAndUnauthorizedDebugEditsCannotMutateState(GameTestHelper h) {
        var editor = player(h); var target = player(h);
        try {
            var before = target.getPersistentData().copy();
            h.assertTrue(!edit(editor,target,"add",0)&&before.equals(target.getPersistentData()),"Missing debug item denies grant without creating state");
            editor.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(ModItems.DEBUG_STAT_EDITOR.get()));
            h.assertTrue(!DebugShentongEditor.edit(editor,target,"fireball","add",0),"Special tab cannot edit ordinary spells");
            h.assertTrue(!edit(editor,target,"coverage",50)&&before.equals(target.getPersistentData()),"Unowned abilities cannot be configured");
            edit(editor,target,"add",0); before=target.getPersistentData().copy();
            h.assertTrue(!edit(editor,target,"forged_field",1)&&before.equals(target.getPersistentData()),"Unknown fields cannot write arbitrary NBT");
            editor.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);
            h.assertTrue(!edit(editor,target,"remove",0)&&AnnihilationCurse.owned(target),"Putting down debug item revokes edit permission");
        } finally {editor.discard();target.discard();}
        h.succeed();
    }
    @GameTest(template="fate_test")
    public static void registeredFutureAbilitiesExposeOwnControlsAndStayIndependent(GameTestHelper h) {
        String id="debug_future_shentong", tag="DebugFutureShentongTest";
        if (ShentongRegistry.get(id)==null) ShentongRegistry.register(new ShentongRegistry.Definition(
                id,"debug.test.future_shentong",p -> p.getPersistentData().contains(tag),
                p -> { if (!p.getPersistentData().contains(tag)) p.getPersistentData().put(tag,new CompoundTag()); },
                p -> p.getPersistentData().remove(tag),p -> {},List.of(
                new ShentongRegistry.Field("power","debug.test.power",-5,10,false,
                        p -> p.getPersistentData().getCompound(tag).getInt("Power"),
                        (p,v) -> p.getPersistentData().getCompound(tag).putInt("Power",v)),
                new ShentongRegistry.Field("active","debug.test.active",0,1,true,
                        p -> p.getPersistentData().getCompound(tag).getBoolean("Active")?1:0,
                        (p,v) -> p.getPersistentData().getCompound(tag).putBoolean("Active",v!=0)))));
        var editor=player(h); var target=player(h);
        try {
            editor.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(ModItems.DEBUG_STAT_EDITOR.get()));
            edit(editor,target,"add",0);
            h.assertTrue(DebugShentongEditor.edit(editor,target,id,"add",0),"Registered future type can be granted without editor changes");
            DebugShentongEditor.edit(editor,target,id,"power",Integer.MIN_VALUE);
            h.assertTrue(target.getPersistentData().getCompound(tag).getInt("Power")==-5,"Future field lower bound supports negative values");
            DebugShentongEditor.edit(editor,target,id,"power",Integer.MAX_VALUE);
            DebugShentongEditor.edit(editor,target,id,"active",1);
            var snapshot=DebugShentongPayloads.snapshot(target);
            var future=snapshot.abilities().stream().filter(a -> a.id().equals(id)).findFirst().orElseThrow();
            h.assertTrue(future.owned()&&future.fields().size()==2&&future.fields().getFirst().value()==10
                    &&future.fields().getFirst().min()==-5&&future.fields().get(1).toggle()&&future.fields().get(1).value()==1,
                    "Snapshot discovers distinct fields and limits from future registration");
            var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());
            try {
                DebugShentongPayloads.State.CODEC.encode(buffer,snapshot);
                h.assertTrue(snapshot.equals(DebugShentongPayloads.State.CODEC.decode(buffer)),"Dynamic ability metadata and values survive packet roundtrip");
            } finally { buffer.release(); }
            var before=target.getPersistentData().copy();
            h.assertTrue(!DebugShentongEditor.edit(editor,target,id,"coverage",50)&&before.equals(target.getPersistentData()),
                    "Fields from another ability cannot mutate future ability");
            DebugShentongEditor.edit(editor,target,id,"remove",0);
            h.assertTrue(!target.getPersistentData().contains(tag)&&AnnihilationCurse.owned(target),"Removing one ability preserves other ability");
            DebugShentongEditor.edit(editor,target,id,"add",0);
            edit(editor,target,"remove",0);
            h.assertTrue(target.getPersistentData().contains(tag)&&!AnnihilationCurse.owned(target),"Removal remains independent in both directions");
        } finally { editor.discard(); target.discard(); }
        h.succeed();
    }

    @GameTest(template="fate_test")
    public static void playerDetailsOnlyExposeOwnedAbilitiesAndCurrentStateWithoutDebugTool(GameTestHelper h) {
        var player=player(h); var other=player(h);
        try {
            ModAttachments.setData(player,ModAttachments.getData(player).withKnownSpells(List.of("fireball")));
            var before=player.getPersistentData().copy();
            h.assertTrue(com.example.immortal_cultivation_mod.network.ShentongPayloads.snapshot(player).abilities().isEmpty()
                    &&before.equals(player.getPersistentData()),"Ordinary spells do not expose button or create ability state");
            AnnihilationCurse.acquire(other);
            h.assertTrue(com.example.immortal_cultivation_mod.network.ShentongPayloads.snapshot(player).abilities().isEmpty(),"Another player's ability is not included");
            AnnihilationCurse.acquire(player);
            AnnihilationCurse.state(player).putInt("Cooldown",900);
            AnnihilationCurse.state(player).putFloat("Coverage",.37F);
            var snapshot=com.example.immortal_cultivation_mod.network.ShentongPayloads.snapshot(player);
            var ability=snapshot.abilities().getFirst();
            h.assertTrue(snapshot.targetId().equals(player.getUUID())&&snapshot.abilities().size()==1
                    &&ability.id().equals(AnnihilationCurse.ABILITY)&&ability.owned(),"Self inspection works without debug tool and only includes owned special abilities");
            h.assertTrue(ability.fields().stream().anyMatch(f -> f.id().equals("cooldown")&&f.value()==45)
                    &&ability.fields().stream().anyMatch(f -> f.id().equals("coverage")&&f.value()==37),"Displayed player stats reflect real server values");
            var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());
            try {
                var details=new com.example.immortal_cultivation_mod.network.ShentongPayloads.Details(snapshot);
                com.example.immortal_cultivation_mod.network.ShentongPayloads.Details.CODEC.encode(buffer,details);
                h.assertTrue(details.equals(com.example.immortal_cultivation_mod.network.ShentongPayloads.Details.CODEC.decode(buffer)),"Normal details payload roundtrip preserves stats");
            } finally { buffer.release(); }
            player.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(ModItems.DEBUG_STAT_EDITOR.get()));
            edit(player,player,"remove",0);
            h.assertTrue(com.example.immortal_cultivation_mod.network.ShentongPayloads.snapshot(player).abilities().isEmpty(),"Removing last ability hides normal player entry");
        } finally { player.discard(); other.discard(); }
        h.succeed();
    }

}
