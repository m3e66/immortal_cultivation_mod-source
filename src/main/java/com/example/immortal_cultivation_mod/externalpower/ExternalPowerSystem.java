package com.example.immortal_cultivation_mod.externalpower;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.externalpower.ExternalAttackProfile.*;
import com.example.immortal_cultivation_mod.mixin.ExternalExplosionAccessor;
import com.mojang.logging.LogUtils;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.slf4j.Logger;

@EventBusSubscriber(modid=ImmortalCultivationMod.MODID)
public final class ExternalPowerSystem {
    static final Logger LOG=LogUtils.getLogger();
    private static final String OWN=ImmortalCultivationMod.MODID;
    private static final ExternalAttackAnalyzer ANALYZER=new ExternalAttackAnalyzer();
    private static final WeakHashMap<LivingEntity,LinkedHashMap<ExternalAttackBurst.Key,ExternalAttackBurst>> BURSTS=new WeakHashMap<>();
    private static final WeakHashMap<DamageSource,ExplosionEvidence> EXPLOSIONS=new WeakHashMap<>();
    private static final StackWalker CALLERS=StackWalker.getInstance();
    private record ExplosionEvidence(ResourceLocation dimension,long tick,Vec3 center,double radius,double blockRadius,boolean cultivation) {}
    private ExternalPowerSystem() {}

    @SubscribeEvent public static void started(ServerStartedEvent event){ExternalPowerOverrides.reload(true);}
    @SubscribeEvent public static void stopped(ServerStoppedEvent event){clear();ExternalPowerOverrides.clear();}
    @SubscribeEvent public static void unloaded(LevelEvent.Unload event){if(!event.getLevel().isClientSide())clear();}
    private static void clear(){ANALYZER.clear();BURSTS.clear();EXPLOSIONS.clear();}

    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void explosion(ExplosionEvent.Detonate event) {
        if(event.getLevel().isClientSide || !ExternalPowerConfig.ENABLED.get())return;
        var explosion=event.getExplosion();
        var source=((ExternalExplosionAccessor)explosion).externalPower$damageSource();
        Vec3 center=explosion.center();
        double blocks=0;
        var affected=event.getAffectedBlocks();
        int stride=Math.max(1,(affected.size()+255)/256);
        for(int i=0;i<affected.size();i+=stride)blocks=Math.max(blocks,Vec3.atCenterOf(affected.get(i)).distanceTo(center));
        if(EXPLOSIONS.size()>=256)EXPLOSIONS.remove(EXPLOSIONS.keySet().iterator().next());
        EXPLOSIONS.put(source,new ExplosionEvidence(event.getLevel().dimension().location(),event.getLevel().getGameTime(),
                center,explosion.radius(),blocks,cultivationOriginOnStack()));
    }

    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void damage(LivingDamageEvent.Pre event) {
        var target=event.getEntity();
        if(target.level().isClientSide || !ExternalPowerConfig.ENABLED.get() || event.getNewDamage()<=0 || Float.isNaN(event.getNewDamage()))return;
        int realm=ExternalPowerTargets.realm(target);
        if(realm<0)return;
        var settings=ExternalPowerConfig.read();
        DamageSource source=event.getSource();
        ResourceLocation type=type(source);
        boolean melee=source.is(DamageTypes.PLAYER_ATTACK)||source.is(DamageTypes.MOB_ATTACK)||source.is(DamageTypes.MOB_ATTACK_NO_AGGRO);
        boolean projectile=source.is(DamageTypeTags.IS_PROJECTILE)||source.getDirectEntity() instanceof Projectile;
        // A held modded sword does not make vanilla thorns, poison, or burning into that weapon's attack.
        ItemStack weapon=melee||projectile?source.getWeaponItem():null;
        if((weapon==null||weapon.isEmpty()) && melee && source.getEntity() instanceof LivingEntity living)weapon=living.getMainHandItem();
        ResourceLocation item=weapon==null||weapon.isEmpty()?null:BuiltInRegistries.ITEM.getKey(weapon.getItem());
        ResourceLocation attacker=entityId(source.getEntity()),direct=entityId(source.getDirectEntity());
        String mod=resolveMod(type,attacker,direct,item,settings.vanilla());
        if(mod==null)return;
        ExplosionEvidence explosion=EXPLOSIONS.get(source);
        long now=target.level().getGameTime();
        if(explosion!=null && (!explosion.dimension().equals(target.level().dimension().location()) || now-explosion.tick()>2 || now<explosion.tick()))explosion=null;
        if(explosion!=null && explosion.cultivation())return;
        // Vanilla source factories are also used by this mod's ownerless spell/effect damage.
        // Only this ambiguous path needs stack provenance; attributed external namespaces stay cheap.
        if(type.getNamespace().equals("minecraft") && cultivationOriginOnStack())return;
        String path=type.getPath();
        boolean blast=source.is(DamageTypeTags.IS_EXPLOSION)||explosion!=null;
        boolean magic=source.is(DamageTypeTags.WITCH_RESISTANT_TO)||path.contains("magic")||path.contains("spell")||path.contains("arcane")||path.contains("soul");
        boolean bypassInvulnerability=source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
        boolean specialKill=path.contains("kill")&&!path.contains("skill")||path.contains("annihilat")||path.contains("disintegrat")
                ||bypassInvulnerability && event.getOriginalDamage()>=Float.MAX_VALUE*.5F;
        var traits=new Traits(blast,projectile,melee,magic,source.is(DamageTypeTags.IS_FIRE),source.is(DamageTypeTags.IS_LIGHTNING),
                source.getEntity()==null&&source.getDirectEntity()==null,source.is(DamageTypeTags.BYPASSES_ARMOR),
                source.is(DamageTypeTags.BYPASSES_EFFECTS)||source.is(DamageTypeTags.BYPASSES_RESISTANCE),
                source.is(DamageTypeTags.BYPASSES_ENCHANTMENTS),bypassInvulnerability,specialKill);
        var key=new Key(mod,type,attacker,direct,item,projectile?direct:null);
        var burstKey=new ExternalAttackBurst.Key(key,uuid(source.getEntity()),uuid(source.getDirectEntity()));
        if(BURSTS.size()>=2048 && !BURSTS.containsKey(target))BURSTS.remove(BURSTS.keySet().iterator().next());
        var history=BURSTS.computeIfAbsent(target,ignored->new LinkedHashMap<>());
        history.entrySet().removeIf(e->now-e.getValue().lastTick()>settings.burstWindow()*4L);
        var burst=ExternalAttackBurst.observe(history.get(burstKey),now,event.getOriginalDamage(),settings.burstWindow());
        history.put(burstKey,burst);
        if(history.size()>16)history.pollFirstEntry();
        LivingEntity living=source.getEntity() instanceof LivingEntity l?l:null;
        double health=living==null?0:living.getMaxHealth();
        double attack=living==null||living.getAttribute(Attributes.ATTACK_DAMAGE)==null?0:living.getAttributeValue(Attributes.ATTACK_DAMAGE);
        var observation=new Observation(key,traits,event.getOriginalDamage(),event.getNewDamage(),health,attack,
                explosion==null?-1:explosion.radius(),explosion==null?-1:explosion.center().distanceTo(target.position()),
                explosion==null?-1:explosion.blockRadius(),burst.hits(),burst.totalDamage(),burst.hitsPerSecond());
        Estimate estimate=ANALYZER.analyze(observation,settings);
        ExternalPowerOverrides.reload(false);
        var override=ExternalPowerOverrides.get(type);
        if(override!=null)estimate=applyOverride(estimate,override);
        var profile=ExternalAttackProfile.from(observation,estimate,override!=null);
        double multiplier=settings.multiplier(profile.estimatedPowerTier()-realm);
        float adjusted=adjustDamage(event.getNewDamage(),multiplier);
        event.setNewDamage(adjusted);
        if(settings.debug())LOG.info(debug(profile,realm,multiplier,adjusted));
    }

    static Estimate applyOverride(Estimate estimate,ExternalPowerOverrides.Override override) {
        return new Estimate(estimate.damageScore(),estimate.penetrationScore(),override.powerTier(),
                override.penetrationTier()==null?override.powerTier()+(int)Math.ceil(estimate.penetrationScore()):override.penetrationTier(),1);
    }
    public static float adjustDamage(float damage,double multiplier){return multiplier==0?0:(float)(damage*multiplier);}
    /** Cultivation ownership exclusions precede even manual overrides and vanilla opt-in. */
    public static String resolveMod(ResourceLocation damage,ResourceLocation attacker,ResourceLocation direct,ResourceLocation item,boolean vanilla) {
        if(owned(damage)||owned(attacker)||owned(direct)||owned(item))return null;
        if(external(damage))return damage.getNamespace();
        if(external(direct))return direct.getNamespace();
        if(external(item))return item.getNamespace();
        if(external(attacker))return attacker.getNamespace();
        return vanilla?"minecraft":null;
    }
    private static boolean owned(ResourceLocation id){return id!=null&&id.getNamespace().equals(OWN);}
    private static boolean external(ResourceLocation id){return id!=null&&!id.getNamespace().equals("minecraft")&&!owned(id);}
    private static ResourceLocation entityId(Entity entity){return entity==null?null:BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());}
    private static UUID uuid(Entity entity){return entity==null?null:entity.getUUID();}
    private static ResourceLocation type(DamageSource source){return source.typeHolder().unwrapKey().map(k->k.location()).orElse(ResourceLocation.withDefaultNamespace("unregistered"));}
    private static boolean cultivationOriginOnStack() {
        return CALLERS.walk(frames->{
            boolean action=false;
            for(var frame:frames.toList()) {
                String name=frame.getClassName(),method=frame.getMethodName();
                if(name.equals("net.minecraft.world.entity.LivingEntity") && (method.equals("hurt")||method.equals("actuallyHurt"))
                        ||name.equals("net.minecraft.world.level.Explosion")&&method.equals("explode")) {action=true;continue;}
                if(!action||method.equals("hurt")||method.equals("actuallyHurt")||method.equals("isInvulnerableTo"))continue;
                if(name.startsWith("net.minecraft.")||name.startsWith("net.neoforged.")||name.startsWith("java.")
                        ||name.startsWith("jdk.")||name.startsWith("org.spongepowered.")||name.startsWith("com.mojang.")
                        ||name.startsWith("com.example.immortal_cultivation_mod.externalpower."))continue;
                // Inspect the closest non-engine caller, not a distant NPC tick underneath an external trap.
                return name.startsWith("com.example.immortal_cultivation_mod.");
            }
            return false;
        });
    }
    static String debug(ExternalAttackProfile p,int realm,double multiplier,float adjusted) {
        return "=== EXTERNAL ATTACK ANALYSIS ===\nMod: "+p.modId()+"\nDamage Source: "+p.damageType()
                +"\nRaw Damage: "+p.rawDamage()+"\nFinal vanilla damage (before absorption): "+p.finalVanillaDamage()
                +"\nExplosion: "+p.explosion()+"\nProjectile: "+p.projectile()+"\nMelee: "+p.melee()+"\nMagic: "+p.magic()
                +"\nFire: "+p.fire()+"\nLightning: "+p.lightning()+"\nEnvironmental: "+p.environmental()
                +"\nBypass Armor: "+p.bypassArmor()+"\nBypass Effects: "+p.bypassEffects()+"\nBypass Enchantments: "+p.bypassEnchantments()
                +"\nBypass Invulnerability: "+p.bypassInvulnerability()+"\nSpecial kill evidence: "+p.specialKill()
                +"\nAttacker: "+p.attackerEntity()+"\nDirect entity: "+p.directEntity()+"\nWeapon: "+p.weaponItem()
                +"\nAttacker max health / attack damage: "+p.attackerMaxHealth()+" / "+p.attackerAttackDamage()
                +"\nExplosion radius / distance / observed block radius (-1 = unknown): "+p.explosionRadius()+" / "+p.explosionDistance()+" / "+p.observedBlockRadius()
                +"\nBurst hits / damage / hits per second: "+p.burstHits()+" / "+p.burstDamage()+" / "+p.hitsPerSecond()
                +"\nDamage Score: "+p.damageScore()+"\nPenetration Score: "+p.penetrationScore()
                +"\nEstimated Power Tier: "+p.estimatedPowerTier()+"\nPenetration Tier: "+p.penetrationTier()+"\nConfidence: "+p.confidence()
                +"\nManual override: "+p.manualOverride()+"\nTarget Realm: "+ExternalPowerTargets.name(realm)+"\nRealm Tier: "+realm
                +"\nTier Difference: "+(p.estimatedPowerTier()-realm)+"\nDamage Multiplier: x"+multiplier
                +"\nOriginal Damage: "+p.rawDamage()+"\nAdjusted Damage: "+adjusted+"\n================================";
    }
}
