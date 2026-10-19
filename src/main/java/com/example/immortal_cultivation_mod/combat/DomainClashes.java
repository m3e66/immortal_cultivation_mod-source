package com.example.immortal_cultivation_mod.combat;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.network.QiAuraPayloads;
import com.example.immortal_cultivation_mod.physics.DomainVolume;
import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;

/** Server-owned pair contacts, deterministic method contests and broken-domain recovery. */
public final class DomainClashes {
    public static final int CONTEST_TICKS=DomainVolume.CLASH_DURATION_TICKS,RECOVERY_TICKS=60;
    private record Key(UUID first,UUID second) { }
    private static final class Contact {
        final QiDomainSystem.Aura first,second;
        long started,seen,lastSend=Long.MIN_VALUE;
        int winner;
        Contact(QiDomainSystem.Aura a,QiDomainSystem.Aura b,long now){first=a;second=b;started=now;}
    }
    private static final Map<Key,Contact> CONTACTS=new HashMap<>();
    private DomainClashes() { }
    public static int limit(String method) {
        var definition=CultivationMethods.get(method);
        return definition==null?-1:CultivationLevels.getStageIndex(definition.limitLevel());
    }
    public static int compare(String first,int firstMastery,String second,int secondMastery) {
        int rank=Integer.compare(limit(first),limit(second));
        return rank!=0?rank:Integer.compare(Math.max(0,firstMastery),Math.max(0,secondMastery));
    }
    public static int compare(LivingEntity first,LivingEntity second) {
        return compare(QiDomainSystem.method(first),QiDomainSystem.mastery(first),QiDomainSystem.method(second),QiDomainSystem.mastery(second));
    }
    public static void tick(List<QiDomainSystem.Aura> actors) {
        for(var aura:actors)aura.contacts.clear();
        var ordered=actors.stream().filter(QiDomainSystem.Aura::domain).sorted(Comparator.comparing(a->a.owner.getUUID())).toList();
        record Rank(int limit,int mastery) { }
        var ranks=new IdentityHashMap<QiDomainSystem.Aura,Rank>();
        for(var aura:ordered)ranks.put(aura,new Rank(limit(aura.method),QiDomainSystem.mastery(aura.owner)));
        var broken=new HashSet<QiDomainSystem.Aura>();
        var defeats=new IdentityHashMap<QiDomainSystem.Aura,Contact>();
        for(int i=0;i<ordered.size();i++)for(int j=i+1;j<ordered.size();j++) {
            var a=ordered.get(i);var b=ordered.get(j);
            if(a.owner.level()!=b.owner.level()||!QiDomainSystem.canAffect(a.owner,b.owner)||!QiDomainSystem.canAffect(b.owner,a.owner)
                    ||a.owner.distanceToSqr(b.owner)>=4*DomainVolume.RADIUS*DomainVolume.RADIUS)continue;
            long now=a.owner.level().getGameTime();
            var key=new Key(a.owner.getUUID(),b.owner.getUUID());
            var contact=CONTACTS.computeIfAbsent(key,k->new Contact(a,b,now));
            var rankA=ranks.get(a);var rankB=ranks.get(b);
            int comparison=Integer.compare(rankA.limit,rankB.limit);
            if(comparison==0)comparison=Integer.compare(rankA.mastery,rankB.mastery);
            int winner=comparison==0?0:comparison>0?1:2;
            if(contact.winner!=winner){contact.winner=winner;contact.started=now;contact.lastSend=Long.MIN_VALUE;}
            contact.seen=now;
            if(winner!=0&&now-contact.started>=CONTEST_TICKS) {
                var loser=winner==1?b:a;var victor=winner==1?a:b;
                var previous=defeats.get(loser);
                var previousVictor=previous==null?null:previous.winner==1?previous.first:previous.second;
                if(previousVictor==null||ranks.get(victor).limit>ranks.get(previousVictor).limit
                        ||ranks.get(victor).limit==ranks.get(previousVictor).limit&&ranks.get(victor).mastery>ranks.get(previousVictor).mastery)
                    defeats.put(loser,contact);
                broken.add(loser);continue;
            }
            double age=now-contact.started;
            a.contacts.add(DomainVolume.contact(a.owner.position(),b.owner.position(),age,comparison,1));
            b.contacts.add(DomainVolume.contact(b.owner.position(),a.owner.position(),age,-comparison,-1));
            if(a.opponent==-1)a.opponent=b.owner.getId();
            if(b.opponent==-1)b.opponent=a.owner.getId();
            if(contact.lastSend==Long.MIN_VALUE||now-contact.lastSend>=5){send(contact,0);contact.lastSend=now;}
        }
        // Determine all winners before applying any destruction; iteration order cannot save a loser.
        for(var aura:broken){send(defeats.get(aura),1);QiDomainSystem.breakDomain(aura);}
        var iterator=CONTACTS.values().iterator();
        while(iterator.hasNext()) {
            var contact=iterator.next();
            if(!contact.first.domain()||!contact.second.domain()||contact.first.owner.level()!=contact.second.owner.level()
                    ||contact.seen!=contact.first.owner.level().getGameTime()) {send(contact,2);iterator.remove();}
        }
        if(!broken.isEmpty()) {
            // Rebuild without destroyed opponents so no stale clipped area survives this tick.
            for(var aura:actors){aura.contacts.clear();aura.opponent=-1;}
            for(var contact:CONTACTS.values()) {
                var a=contact.first;var b=contact.second;double age=a.owner.level().getGameTime()-contact.started;
                int sign=contact.winner==0?0:contact.winner==1?1:-1;
                a.contacts.add(DomainVolume.contact(a.owner.position(),b.owner.position(),age,sign,1));
                b.contacts.add(DomainVolume.contact(b.owner.position(),a.owner.position(),age,-sign,-1));
                a.opponent=b.owner.getId();b.opponent=a.owner.getId();
            }
        }
    }
    public static void remove(LivingEntity owner) {
        var iterator=CONTACTS.values().iterator();
        while(iterator.hasNext()) {var contact=iterator.next();if(contact.first.owner==owner||contact.second.owner==owner){send(contact,2);iterator.remove();}}
    }
    public static void clear(){CONTACTS.clear();}
    private static void send(Contact contact,int kind) {
        var a=contact.first.owner;var b=contact.second.owner;
        if(!(a.level() instanceof ServerLevel level))return;
        var midpoint=a.position().lerp(b.position(),.5);
        PacketDistributor.sendToPlayersNear(level,null,midpoint.x,midpoint.y,midpoint.z,112,
                new QiAuraPayloads.DomainClash(level.dimension().location(),a.getId(),a.getUUID(),b.getId(),b.getUUID(),
                        MethodDomainStyle.of(contact.first.method).ordinal(),MethodDomainStyle.of(contact.second.method).ordinal(),
                        contact.winner,DomainVolume.clashVisualAge(level.getGameTime()-contact.started),kind));
    }
}
