package com.example.immortal_cultivation_mod.system;

import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.item.UtilityTalismanItem;
import com.example.immortal_cultivation_mod.network.ModPayloads;
import com.example.immortal_cultivation_mod.spell.*;
import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("immortal_cultivation_huti_qi")
@PrefixGameTestTemplate(false)
public final class ShieldDisplayGameTests {
    @GameTest(template="fate_test") public static void realStackedShieldsKeepTheirColorsThroughDamageAndClearing(GameTestHelper h) {
        try(var f=new LifeSimulationGameTests.Player(h)) {
            var p=f.player;ModAttachments.setData(p,ModAttachments.getData(p).withQi(100000));
            try {
                h.assertTrue(HutiQi.cast(p)&&DielangShield.activateNext(p)&&GushiShield.cast(p),"Actual shield activations succeed");BloodQiShield.grant(p);
                var layers=PlayerShieldManager.layers(p);
                h.assertTrue(layers.size()==4&&layers.stream().map(l->l.kind().color).distinct().count()==4,"Four actual stacked shields keep four colors");
                HutiQi.absorb(p,p.damageSources().generic(),8);
                h.assertTrue(PlayerShieldManager.layers(p).stream().anyMatch(l->l.kind()==ShieldDisplay.Kind.HUTI_QI&&l.amount()==12&&l.max()==20),"Damaged shield keeps its identity and original capacity");
                DielangShield.clear(p);
                h.assertTrue(PlayerShieldManager.layers(p).size()==3&&PlayerShieldManager.layers(p).stream().noneMatch(l->l.kind()==ShieldDisplay.Kind.DIELANG),"Clearing one shield removes only its colored section");
                h.assertTrue(Math.abs(ShieldDisplay.total(PlayerShieldManager.layers(p),false)-PlayerShieldManager.totalAmount(p))<.001,"Colored display preserves real aggregate amount");
            } finally {HutiQi.clear(p);DielangShield.clear(p);GushiShield.clear(p);BloodQiShield.clear(p);PlayerShieldManager.clearAll(p);}
        }h.succeed();
    }
    @GameTest(template="fate_test") public static void allShieldSourcesHaveUniqueStableColorsAndDomainsMergeByType(GameTestHelper h) {
        try(var f=new LifeSimulationGameTests.Player(h)) {
            String[] sources={"huti_qi","dielang_shield","gushi_shield","blood_qi_pill",ModSpells.XUANYAN_HUTI,ModSpells.LIUFENG_XIEJIN,
                    ModSpells.XUESHA_HUSHEN,ModSpells.JINGLING_FANZHAO,ModSpells.YUANGANG_HUJIE+":"+UUID.randomUUID(),
                    "black_tortoise_heartguard_mirror","undead_bone_growth","thin_gold_talisman","golden_body_talisman","talisman_shields","unknown_addon_shield"};
            try {
                for(var source:sources)PlayerShieldManager.set(f.player,source,10,20);
                var layers=PlayerShieldManager.layers(f.player);
                h.assertTrue(layers.size()==sources.length&&layers.stream().map(l->l.kind().color).distinct().count()==sources.length,"Every registered shield source has a unique color");
                PlayerShieldManager.set(f.player,ModSpells.YUANGANG_HUJIE+":"+UUID.randomUUID(),30,50);
                var domain=PlayerShieldManager.layers(f.player).stream().filter(l->l.kind()==ShieldDisplay.Kind.DOMAIN).findFirst().orElseThrow();
                h.assertTrue(domain.amount()==40&&domain.max()==70,"Two caster UUIDs keep one consistent barrier color and exact summed capacity");
                PlayerShieldManager.set(f.player,"huti_qi",Float.NaN,20);
                h.assertTrue(PlayerShieldManager.layers(f.player).stream().noneMatch(l->l.kind()==ShieldDisplay.Kind.HUTI_QI),"Invalid values cannot poison shield display");
            }finally{PlayerShieldManager.clearAll(f.player);}
        }h.succeed();
    }
    @GameTest(template="fate_test") public static void shieldPacketIsBoundedAndRoundTripsEveryColor(GameTestHelper h) {
        var b=new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());
        try {
            var layers=new ArrayList<ShieldDisplay.Layer>();for(var kind:ShieldDisplay.Kind.values())layers.add(new ShieldDisplay.Layer(kind,12,30));
            var packet=new ModPayloads.ClientboundShieldDataPayload(0,0,layers);layers.clear();
            ModPayloads.ClientboundShieldDataPayload.STREAM_CODEC.encode(b,packet);
            h.assertTrue(b.readableBytes()<160&&packet.equals(ModPayloads.ClientboundShieldDataPayload.STREAM_CODEC.decode(b)),"All shield identities fit below 160 bytes and round-trip exactly");
            h.assertTrue(packet.amount()==12*ShieldDisplay.Kind.values().length,"Packet snapshots cannot be changed by mutable caller list");
            b.clear();b.writeFloat(0);b.writeFloat(0);b.writeVarInt(1000000);boolean rejected=false;
            try{ModPayloads.ClientboundShieldDataPayload.STREAM_CODEC.decode(b);}catch(IllegalArgumentException expected){rejected=true;}
            h.assertTrue(rejected,"Unbounded layer counts rejected before allocating");
            b.clear();b.writeFloat(1);b.writeFloat(1);b.writeVarInt(1);b.writeByte(255);b.writeFloat(1);b.writeFloat(1);rejected=false;
            try{ModPayloads.ClientboundShieldDataPayload.STREAM_CODEC.decode(b);}catch(IllegalArgumentException expected){rejected=true;}
            h.assertTrue(rejected,"Unknown color identity rejected");
            var repeated=new ShieldDisplay.Layer(ShieldDisplay.Kind.WIND,1,2);rejected=false;
            try{new ModPayloads.ClientboundShieldDataPayload(2,4,List.of(repeated,repeated));}catch(IllegalArgumentException expected){rejected=true;}
            h.assertTrue(rejected,"Duplicate types cannot overflow HUD layout");
        }finally{b.release();}h.succeed();
    }
    @GameTest(template="fate_test") public static void mixedShieldLayoutKeepsTinySourcesVisibleWithoutOverflow(GameTestHelper h) {
        var layers=new ArrayList<ShieldDisplay.Layer>();
        for(var kind:ShieldDisplay.Kind.values())layers.add(new ShieldDisplay.Layer(kind,kind==ShieldDisplay.Kind.DOMAIN?1000000:1,kind==ShieldDisplay.Kind.DOMAIN?1000000:20));
        for(int height:new int[]{44,60,100}) {
            var sections=ShieldDisplay.segments(layers,height);int end=0;
            h.assertTrue(sections.size()==layers.size(),"All sources keep a visible section");
            for(var section:sections) {
                h.assertTrue(section.height()>=2&&section.filled()>=1&&section.filled()<=section.height()&&section.bottom()>=end,"Tiny shields remain visible beside huge barrier without overlap");
                end=section.bottom()+section.height();
            }
            h.assertTrue(end==height,"Sections fit existing HUD bounds exactly");
        }
        var huge=new ShieldDisplay.Layer(ShieldDisplay.Kind.BLOOD,Float.MAX_VALUE/30,Float.MAX_VALUE);
        h.assertTrue(ShieldDisplay.segments(List.of(huge),44).getFirst().filled()<44,"Finite extreme shield values do not overflow to a full bar");h.succeed();
    }
    @GameTest(template="fate_test") public static void talismanTypesPersistSeparatelyAndKeepOldestFirstAbsorption(GameTestHelper h) {
        try(var f=new LifeSimulationGameTests.Player(h)) {
            var p=f.player;String key="immortal_cultivation_mod:talisman_shields";
            try {
                UtilityTalismanItem.applyActivatedEffect(h.getLevel(),p,UtilityTalismanItem.Kind.THIN_GOLD_SHIELD,null);
                UtilityTalismanItem.applyActivatedEffect(h.getLevel(),p,UtilityTalismanItem.Kind.GOLDEN_BODY_SHIELD,null);
                var entries=p.getPersistentData().getList(key,Tag.TAG_COMPOUND);float thin=entries.getCompound(0).getFloat("Amount");
                var old=new CompoundTag();old.putFloat("Amount",17);old.putFloat("Max",20);old.putLong("Expires",h.getLevel().getGameTime()+500);entries.add(old);
                p.getPersistentData().put(key,entries.copy());PlayerShieldManager.clearAll(p);p.tickCount=20;UtilityTalismanItem.tickPlayer(p);
                var layers=PlayerShieldManager.layers(p);
                h.assertTrue(layers.size()==3&&layers.stream().anyMatch(l->l.kind()==ShieldDisplay.Kind.THIN_GOLD_TALISMAN)
                        &&layers.stream().anyMatch(l->l.kind()==ShieldDisplay.Kind.GOLDEN_BODY_TALISMAN)
                        &&layers.stream().anyMatch(l->l.kind()==ShieldDisplay.Kind.LEGACY_TALISMAN),"Reload rebuilds both talisman colors and preserves untyped old saves");
                float before=PlayerShieldManager.totalAmount(p);float left=UtilityTalismanItem.absorbTalismanShields(p,p.damageSources().generic(),thin+1);
                h.assertTrue(left==0&&Math.abs(PlayerShieldManager.totalAmount(p)-(before-thin-1))<.01,"Original oldest-first shield absorption stays exact");
                h.assertTrue(PlayerShieldManager.layers(p).stream().noneMatch(l->l.kind()==ShieldDisplay.Kind.THIN_GOLD_TALISMAN),"Broken thin-gold section clears without recoloring remaining shields");
                entries=p.getPersistentData().getList(key,Tag.TAG_COMPOUND);for(int i=0;i<entries.size();i++)entries.getCompound(i).putLong("Expires",h.getLevel().getGameTime()-1);
                UtilityTalismanItem.tickPlayer(p);h.assertTrue(PlayerShieldManager.layers(p).isEmpty(),"Expiry clears all corresponding colors");
            }finally{p.getPersistentData().remove(key);PlayerShieldManager.clearAll(p);}
        }h.succeed();
    }
}
