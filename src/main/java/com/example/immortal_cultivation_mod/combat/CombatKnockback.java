package com.example.immortal_cultivation_mod.combat;

import com.example.immortal_cultivation_mod.config.BlockBreakingSettings;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.spell.SpellDestruction;
import com.example.immortal_cultivation_mod.entity.NpcProfile;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/** Server-confirmed impacts; no terrain damage from ordinary walking, falling or status ticks. */
@EventBusSubscriber(modid = ImmortalCultivationMod.MODID)
public final class CombatKnockback {
    private static final Map<LivingEntity, Impact> IMPACTS = new WeakHashMap<>();
    private static final class Impact {
        Entity attacker;
        Vec3 direction, previous, momentum=Vec3.ZERO;
        ServerLevel level;
        float damage;
        double launchStrength,lift;
        int blocks, age;
        boolean pending = true,blast;
    }
    private CombatKnockback() {}

    public static double strength(float damage) {
        return Float.isFinite(damage) && damage > 0 ? Math.min(2.8, .35 + Math.sqrt(damage) * .075) : 0;
    }
    public static int blockBudget(float damage) {
        return Float.isFinite(damage) && damage > 0 ? (int) Math.min(256, Math.ceil(damage * .8)) : 0;
    }
    public static double blastStrength(LivingEntity attacker,LivingEntity target,float damage) {
        if(!QiDomainSystem.blasting(attacker)||!Float.isFinite(damage)||damage<=0
                ||!QiDomainSystem.canAffect(attacker,target)||NpcProfile.areCombatAllies(attacker,target))return 0;
        double factor=QiActivationKnockback.realmFactor(attacker,target);
        double release=Math.clamp(QiDomainSystem.percent(attacker),100,200)/100.0;
        return Math.min(4,(1.65+Math.min(.45,Math.sqrt(damage)*.035))*factor*(.75+.25*release));
    }

    @SubscribeEvent
    public static void hit(LivingDamageEvent.Post event) {
        LivingEntity target = event.getEntity();
        var source = event.getSource();
        Entity attacker = source.getEntity();
        if (!(target.level() instanceof ServerLevel level) || attacker == null || attacker == target
                || !target.isAlive() || strength(event.getNewDamage()) == 0
                || (target instanceof ServerPlayer player && (player.isSpectator() || player.isCreative()))) return;
        // Ongoing burns and poison have no attacker; damage that carries an attacker is an impact.
        Vec3 direction = CombatFeedbackEvents.incomingDirection(target, source).multiply(1, 0, 1);
        if (direction.lengthSqr() < .0001) direction = attacker.getLookAngle().multiply(1, 0, 1);
        if (direction.lengthSqr() < .0001) direction = new Vec3(0, 0, 1);
        // Only direct punches grant a Blast Qi terrain impulse.
        if(attacker instanceof LivingEntity living&&QiDomainSystem.blasting(living)
                &&!QiBlastCombat.isFistImpact(living,event.getSource()))return;
        double blastStrength=attacker instanceof LivingEntity living?blastStrength(living,target,event.getNewDamage()):0;
        if(attacker instanceof LivingEntity living&&QiDomainSystem.blasting(living)&&blastStrength<=0)return;
        boolean blast=blastStrength>0;
        double launchStrength=blast?blastStrength:strength(event.getNewDamage());
        Impact impact = IMPACTS.get(target);
        if (impact != null && impact.pending && impact.launchStrength>=launchStrength && (impact.blast||!blast)) return;
        impact = new Impact();
        impact.attacker = attacker;
        impact.direction = direction.normalize();
        impact.previous = target.position();
        impact.level = level;
        impact.damage = event.getNewDamage();
        impact.blast=blast;impact.launchStrength=launchStrength;
        impact.lift=blast?Math.min(.95,.28+launchStrength*.19):0;
        impact.blocks = blast?(int)Math.min(256,64+launchStrength*32+Math.sqrt(impact.damage)*4):blockBudget(impact.damage);
        if (!BlockBreakingSettings.enabled(attacker,
                BlockBreakingSettings.Category.COMBAT_IMPACTS)) impact.blocks = 0;
        if (source.getDirectEntity() instanceof com.example.immortal_cultivation_mod.entity.WeaponQiSlashEntity slash
                && !slash.allowsTerrainBreaking()) impact.blocks = 0;
        else if (!blast && attacker instanceof ServerPlayer player
                && source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)
                && source.getDirectEntity() == player
                && (!CombatStateManager.isHeavyAttack(source)
                || !com.example.immortal_cultivation_mod.item.QiInfusedWeapon.outwardQiBlockBreakingEnabled(player)))
            impact.blocks = 0;
        IMPACTS.put(target, impact);
    }

    @SubscribeEvent
    public static void tick(EntityTickEvent.Pre event) {
        // Integrated clients share this static map. Client/server entities can compare equal by ID;
        // a client tick must never read or remove a queued authoritative server impact.
        if (!(event.getEntity() instanceof LivingEntity target) || !(target.level() instanceof ServerLevel)) return;
        Impact impact = IMPACTS.get(target);
        if (impact == null) return;
        if (!target.isAlive() || target.isRemoved() || target.level() != impact.level || ++impact.age > (impact.blast?35:20)
                || target.position().distanceToSqr(impact.previous) > 64
                || impact.attacker.isRemoved() || impact.attacker.level() != impact.level) {
            IMPACTS.remove(target); return;
        }
        // Once the launch has landed, residual velocity or AI walking cannot extend its terrain damage.
        if (!impact.pending && target.onGround() && target.getDeltaMovement().y <= 0) {
            IMPACTS.remove(target); return;
        }
        if (impact.pending) {
            // Apply after the attack finishes: vanilla player attacks can restore pre-hit motion.
            Vec3 before = target.getDeltaMovement();
            target.knockback(impact.launchStrength, -impact.direction.x, -impact.direction.z);
            if (target.getDeltaMovement().equals(before)) { IMPACTS.remove(target); return; }
            if(impact.blast) {
                // Respect knockback resistance/events while giving even airborne targets a real upward launch.
                Vec3 after=target.getDeltaMovement();
                double response=Math.clamp(after.subtract(before.scale(.5)).horizontalDistance()/impact.launchStrength,0,1);
                target.setDeltaMovement(after.x,Math.max(after.y,impact.lift*response),after.z);
                impact.momentum=target.getDeltaMovement();
                visualMotion(target,impact,target.position(),impact.momentum,
                        impact.level.getBlockState(target.blockPosition().below()),false);
            }
            impact.pending = false;
            synchronizeMotion(target);
        }
        Vec3 velocity = target.getDeltaMovement();
        if (!BlockBreakingSettings.enabled(impact.attacker,
                BlockBreakingSettings.Category.COMBAT_IMPACTS)) impact.blocks = 0;
        Vec3 travelled = target.position().subtract(impact.previous);
        impact.previous = target.position();
        double forwardSpeed = Math.max(velocity.dot(impact.direction), travelled.dot(impact.direction));
        if ((!impact.blast&&!(target instanceof ServerPlayer)) || impact.blocks <= 0) {
            if (impact.age > 2) IMPACTS.remove(target);
            return;
        }
        // A client may stop at a wall before its block update arrives. Retain only a brief, decaying
        // collision impulse; it is restored only after an actual obstructing block is removed.
        boolean stalled=impact.blast&&forwardSpeed<.08&&impact.age<12&&impact.momentum.dot(impact.direction)>.15;
        if (forwardSpeed < .08&&!stalled) { if (impact.age > 2) IMPACTS.remove(target); return; }
        Vec3 travel=impact.blast?(stalled?impact.momentum.scale(.8):velocity):impact.direction.scale(Math.min(2.8,forwardSpeed));
        if(impact.blast&&travel.horizontalDistance()<travelled.horizontalDistance())travel=travelled;
        // These attacks launch upward/outward. The later downward component is gravity, not
        // additional attack force: never sweep down into the floor beneath the victim.
        travel=new Vec3(travel.x,Math.max(0,travel.y),travel.z);
        if(travel.lengthSqr()>16)travel=travel.normalize().scale(4);
        // A narrow swept body first proves contact. Only then widen the impact around that contact.
        AABB sweep = target.getBoundingBox().deflate(.025)
                .expandTowards(travel);
        BlockPos contact = null;
        var contacts=impact.blast?new java.util.ArrayList<BlockPos>():null;
        var removedBlocks=impact.blast?new java.util.ArrayList<BlockPos>():null;
        double nearest = Double.MAX_VALUE;
        int probes = 0;
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(sweep.minX, sweep.minY, sweep.minZ),
                BlockPos.containing(sweep.maxX, sweep.maxY, sweep.maxZ))) {
            if (++probes > (impact.blast?256:128)) break;
            if (!impact.level.hasChunkAt(pos)) continue;
            if (belowFeet(impact.level, pos, target)) continue;
            var shape = impact.level.getBlockState(pos).getCollisionShape(impact.level, pos);
            boolean intersects = shape.toAabbs().stream().anyMatch(box -> box.move(pos).intersects(sweep));
            if(intersects&&contacts!=null)contacts.add(pos.immutable());
            double distance = Vec3.atCenterOf(pos).distanceToSqr(target.getBoundingBox().getCenter());
            if (intersects && distance < nearest) { contact = pos.immutable(); nearest = distance; }
        }
        if (contact == null) return;
        var contactState=impact.level.getBlockState(contact);
        int broken=0;
        if(impact.blast) {
            if(!SpellDestruction.hasBudget(impact.level))return;
            BlockPos first=contact;
            boolean removed=BlockBreakingSettings.withSource(BlockBreakingSettings.Category.COMBAT_IMPACTS,impact.attacker,
                    ()->SpellDestruction.breakNow(impact.level,first,false,impact.attacker));
            // An unbreakable/protected contact stops the flight; no hollowing out its surroundings.
            if(!removed){IMPACTS.remove(target);return;}
            impact.blocks--;broken++;removedBlocks.add(first);
            contacts.sort(java.util.Comparator.comparingDouble(pos->Vec3.atCenterOf(pos).distanceToSqr(target.getBoundingBox().getCenter())));
            // Clear the actual swept body corridor, including the head as it rises through a wall.
            for(BlockPos pos:contacts) {
                if(pos.equals(first))continue;
                if(broken>=32||impact.blocks<=0||!SpellDestruction.hasBudget(impact.level))break;
                if(!BlockBreakingSettings.withSource(BlockBreakingSettings.Category.COMBAT_IMPACTS,impact.attacker,
                        ()->SpellDestruction.breakNow(impact.level,pos,false,impact.attacker))){impact.blocks=0;break;}
                impact.blocks--;broken++;removedBlocks.add(pos);
            }
        }
        double radius = impact.blast?Math.min(2,.8+impact.launchStrength*.18):Math.min(2.5,.5+Math.sqrt(impact.damage)*.1);
        Vec3 center = Vec3.atCenterOf(contact);
        int extent = (int) Math.ceil(radius);
        for (BlockPos pos : BlockPos.betweenClosed(contact.offset(-extent, -extent, -extent),
                contact.offset(extent, extent, extent))) {
            if (broken >= 32 || impact.blocks <= 0 || !SpellDestruction.hasBudget(impact.level)) break;
            if (Vec3.atCenterOf(pos).distanceToSqr(center) > radius * radius || !impact.level.hasChunkAt(pos)) continue;
            // Widen a wall impact without removing the landing surface (including slabs).
            if (belowFeet(impact.level, pos, target)) continue;
            if (BlockBreakingSettings.withSource(
                    BlockBreakingSettings.Category.COMBAT_IMPACTS, impact.attacker,
                    () -> SpellDestruction.breakNow(impact.level, pos, false, impact.attacker))) {
                broken++; impact.blocks--;
                if(removedBlocks!=null)removedBlocks.add(pos.immutable());
            }
        }
        if(impact.blast&&broken>0) {
            visualMotion(target,impact,center,impact.direction,contactState,true);
            impact.momentum=stalled?impact.momentum.scale(.8):target.getDeltaMovement();
            if(stalled)target.setDeltaMovement(impact.momentum);
            // Send the opened path before reasserting motion so a player client does not collide with stale blocks.
            if(target instanceof ServerPlayer player) {
                for(BlockPos pos:removedBlocks)
                    player.connection.send(new net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket(impact.level,pos));
                synchronizeMotion(target);
            }
        }
    }

    private static boolean belowFeet(ServerLevel level, BlockPos pos, LivingEntity target) {
        double feet = target.getBoundingBox().minY + .025;
        if (pos.getY() + 1 <= feet) return true;
        var shape = level.getBlockState(pos).getCollisionShape(level, pos);
        return !shape.isEmpty() && pos.getY() + shape.max(net.minecraft.core.Direction.Axis.Y) <= feet;
    }

    private static void visualMotion(LivingEntity target,Impact impact,Vec3 point,Vec3 direction,
                                     net.minecraft.world.level.block.state.BlockState state,boolean collision) {
        if(!(impact.attacker instanceof LivingEntity owner))return;
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayersNear(impact.level,null,point.x,point.y,point.z,80,
                new com.example.immortal_cultivation_mod.network.QiAuraPayloads.Motion(impact.level.dimension().location(),target.getId(),target.getUUID(),
                        point.x,point.y,point.z,(float)direction.x,(float)direction.y,(float)direction.z,
                        MethodDomainStyle.of(QiDomainSystem.method(owner)).ordinal(),net.minecraft.world.level.block.Block.getId(state),collision));
    }
    private static void synchronizeMotion(LivingEntity target) {
        target.hasImpulse=true;target.hurtMarked=true;
        if(target instanceof ServerPlayer player)
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));
    }

    @SubscribeEvent public static void stop(ServerStoppedEvent event) { IMPACTS.clear(); }
}
