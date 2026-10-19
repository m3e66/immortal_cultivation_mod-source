package com.example.immortal_cultivation_mod.shentong;

import com.example.immortal_cultivation_mod.curse.AnnihilationCurse;
import com.example.immortal_cultivation_mod.network.ShentongPayloads;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.*;
import net.minecraft.server.network.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

/** Capture the actual packets delivered by the server's multiplayer proximity broadcaster. */
@GameTestHolder("immortal_cultivation_curse")
@PrefixGameTestTemplate(false)
public final class ShentongAudienceGameTests {
    @SuppressWarnings("unchecked") private static List<ServerPlayer> online(net.minecraft.server.MinecraftServer server){
        try{var field=net.minecraft.server.players.PlayerList.class.getDeclaredField("players");field.setAccessible(true);return (List<ServerPlayer>)field.get(server.getPlayerList());}
        catch(ReflectiveOperationException error){throw new AssertionError(error);}
    }
    private static final class Peer implements AutoCloseable {
        final ServerPlayer player;
        final EmbeddedChannel channel;
        final List<ShentongPayloads.Activated> received=new ArrayList<>();
        Peer(ServerLevel level,Vec3 pos){
            player=new ServerPlayer(level.getServer(),level,new GameProfile(UUID.randomUUID(),"calligraphy-peer"),ClientInformation.createDefault());
            var connection=new Connection(PacketFlow.SERVERBOUND);channel=new EmbeddedChannel(connection);
            new ServerGamePacketListenerImpl(level.getServer(),connection,player,CommonListenerCookie.createInitial(player.getGameProfile(),false)){
                @Override public void send(Packet<?> packet){if(packet instanceof ClientboundCustomPayloadPacket custom&&custom.payload() instanceof ShentongPayloads.Activated activation)received.add(activation);}
            };
            player.setPos(pos);online(level.getServer()).add(player);
        }
        @Override public void close(){online(player.server).remove(player);player.discard();channel.close();}
    }
    @GameTest(template="fate_test")
    public static void realCurseRescueReachesCasterAndNearbyButNotDistantOrOtherDimension(GameTestHelper h){
        var level=h.getLevel();Vec3 origin=Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(3,80,3)));
        ServerLevel other=level.getServer().getLevel(Level.NETHER);
        try(var caster=new Peer(level,origin);var near=new Peer(level,origin.add(16,0,0));var diagonal=new Peer(level,origin.add(20,0,20));
            var far=new Peer(level,origin.add(33,0,0));var above=new Peer(level,origin.add(0,33,0));var crossDimension=new Peer(other,origin)){
            AnnihilationCurse.acquire(caster.player);h.assertTrue(AnnihilationCurse.rescue(caster.player),"Real passive activation succeeds");
            for(var peer:List.of(caster,near,diagonal)){
                h.assertTrue(peer.received.size()==1,"Each nearby player, including caster, receives exactly one activation");
                var payload=peer.received.getFirst();h.assertTrue(payload.casterId().equals(caster.player.getUUID())&&payload.abilityId().equals(AnnihilationCurse.ABILITY),"Every viewer sees originating player's exact ability");
            }
            for(var peer:List.of(far,above,crossDimension))h.assertTrue(peer.received.isEmpty(),"Out-of-radius and other-dimension players receive no calligraphy");
        }
        h.succeed();
    }
    @GameTest(template="fate_test")
    public static void separateCastersKeepIdentityAndOrdinarySpellsRemainSilent(GameTestHelper h){
        var level=h.getLevel();Vec3 origin=Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(3,80,3)));
        try(var first=new Peer(level,origin);var second=new Peer(level,origin.add(5,0,0));var viewer=new Peer(level,origin.add(10,0,0))){
            ShentongActivations.announce(first.player,ModSpells.FIREBALL);
            h.assertTrue(viewer.received.isEmpty(),"Normal spells are not promoted into shentong");
            ShentongActivations.announce(first.player,AnnihilationCurse.ABILITY);ShentongActivations.announce(second.player,AnnihilationCurse.ABILITY);
            h.assertTrue(viewer.received.size()==2,"Nearby casts of same shentong both arrive");
            h.assertTrue(viewer.received.get(0).casterId().equals(first.player.getUUID())&&viewer.received.get(1).casterId().equals(second.player.getUUID()),"Caster identities survive simultaneous same-ability announcements");
        }
        h.succeed();
    }
}
