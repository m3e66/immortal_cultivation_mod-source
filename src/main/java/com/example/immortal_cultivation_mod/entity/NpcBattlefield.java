package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.magicritual.block.custom.MagicCenterBlockEntity;
import com.example.immortal_cultivation_mod.magicritual.block.custom.MagicCircleBlock;
import com.example.immortal_cultivation_mod.magicritual.center.CenterTargetPolicy;
import com.example.immortal_cultivation_mod.magicritual.center.MagicCenterTypeRegistry;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Visible prepared positions inform tactics; movement's collision/forcefield checks remain authoritative. */
final class NpcBattlefield {
    private record Snapshot(long at,List<MagicCenterBlockEntity> centers) {}
    private static final java.util.Map<PathfinderMob,Snapshot> CACHE=new java.util.WeakHashMap<>();
    private NpcBattlefield() {}
    static boolean visible(PathfinderMob npc,BlockPos pos) {
        var hit=npc.level().clip(new ClipContext(npc.getEyePosition(),Vec3.atCenterOf(pos),
                ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,npc));
        return hit.getType()==HitResult.Type.MISS||hit.getBlockPos().equals(pos);
    }
    private static List<MagicCenterBlockEntity> visibleCenters(PathfinderMob npc) {
        long now=npc.level().getGameTime();var cached=CACHE.get(npc);
        if(cached!=null&&cached.at()==now)return cached.centers();
        var centers=MagicCenterBlockEntity.getLoadedCentersNear(npc.level(),npc.blockPosition(),24).stream()
                .filter(c->c.isActive()&&Math.abs(c.getBlockPos().getY()-npc.getY())<12&&visible(npc,c.getBlockPos())).toList();
        CACHE.put(npc,new Snapshot(now,centers));return centers;
    }
    private static boolean refuge(PathfinderMob npc,MagicCenterBlockEntity center) {
        return center.getCenterTypeId().equals(MagicCenterTypeRegistry.SANCTUARY)
                &&CenterTargetPolicy.receivesBeneficialEffect(center,npc);
    }
    private static boolean hostile(PathfinderMob npc,MagicCenterBlockEntity center) {
        var type=center.getCenterTypeId();
        return CenterTargetPolicy.receivesHarmfulEffect(center,npc)&&(type.equals(MagicCenterTypeRegistry.SCORCHING)
                ||type.equals(MagicCenterTypeRegistry.FROST)||type.equals(MagicCenterTypeRegistry.DECAY)
                ||type.equals(MagicCenterTypeRegistry.THUNDER)||type.equals(MagicCenterTypeRegistry.INVERTED));
    }
    static Vec3 refuge(PathfinderMob npc) {
        return visibleCenters(npc).stream().filter(c->refuge(npc,c))
                .min(java.util.Comparator.comparingDouble(c->c.getBlockPos().distSqr(npc.blockPosition())))
                .map(c->Vec3.atBottomCenterOf(c.getBlockPos().above())).orElse(null);
    }
    static boolean needsFormationResponse(PathfinderMob npc,LivingEntity target) {
        for(var c:visibleCenters(npc)) {
            if(refuge(npc,c)&&npc.getHealth()<npc.getMaxHealth()*.6F)return true;
            if(hostile(npc,c)&&Vec3.atCenterOf(c.getBlockPos()).distanceTo(target.position())<c.getRadius()+3)return true;
        }
        return false;
    }
    static double positionUtility(PathfinderMob npc,Vec3 point) {
        double value=0;
        for(var c:visibleCenters(npc)) {
            double distance=Vec3.atCenterOf(c.getBlockPos()).distanceTo(point);
            if(distance>c.getRadius()+2)continue;
            value+=refuge(npc,c)?26:hostile(npc,c)?-100:-20;
        }
        for(var ally:npc.level().getEntitiesOfClass(PathfinderMob.class,npc.getBoundingBox().inflate(16),
                e->e!=npc&&e.isAlive()&&npc.hasLineOfSight(e)&&NpcProfile.areCombatAllies(npc,e))) {
            double distance=point.distanceTo(ally.position());
            value+=distance<2?-18:distance<9?5:0;
        }
        return value;
    }
    static boolean breakHostileCore(PathfinderMob npc,LivingEntity target) {
        if(NpcProfile.isSparring(npc)||!(npc.level() instanceof ServerLevel level)
                ||!level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING))return false;
        for(var c:visibleCenters(npc)) {
            if(!hostile(npc,c))continue;
            for(BlockPos pos:BlockPos.betweenClosed(npc.blockPosition().offset(-3,-1,-3),npc.blockPosition().offset(3,1,3))) {
                if(!(level.getBlockState(pos).getBlock() instanceof MagicCircleBlock)
                        ||pos.distSqr(c.getBlockPos())>(c.getRadius()+2)*(c.getRadius()+2)
                        ||Vec3.atCenterOf(pos).distanceTo(npc.getEyePosition())>3.5||!visible(npc,pos))continue;
                var p=NpcProfile.cultivationProfile(npc);long now=level.getGameTime();
                long key=pos.asLong();int strikes=p.getLong("FormationStrikePos")==key&&now-p.getLong("FormationStrikeAt")<80
                        ?p.getInt("FormationStrikes")+1:1;
                p.putLong("FormationStrikePos",key);p.putLong("FormationStrikeAt",now);p.putInt("FormationStrikes",strikes);
                npc.swing(InteractionHand.MAIN_HAND,true);
                level.levelEvent(2001,pos,net.minecraft.world.level.block.Block.getId(level.getBlockState(pos)));
                // Damage a visible pattern component, preserving the center's stored inventory and all protection rules.
                if(strikes>=3) {
                    p.putInt("FormationStrikes",0);
                    BeastMoveBlockBreaker.breakBox(level,npc,new AABB(pos),npc.getEyePosition(),1,8);
                }
                return true;
            }
        }
        return false;
    }
}
