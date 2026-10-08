package com.example.immortal_cultivation_mod.externalpower;

import java.util.LinkedHashMap;
import java.util.List;
import com.example.immortal_cultivation_mod.externalpower.ExternalAttackProfile.*;

/** Logarithmic damage evidence combined with independent attack and attacker evidence. */
public final class ExternalAttackAnalyzer {
    private static final double[] SCORES = {0,.5,1,2,3,4,5,6,7,8};
    private final LinkedHashMap<Key, Cached> cache = new LinkedHashMap<>(64,.75F,true);
    private ExternalPowerConfig.Settings previousSettings;
    private record Cached(Observation baseline, Estimate estimate, int observations) {}
    public static double damageScore(double damage, List<Double> thresholds) {
        if (Double.isNaN(damage) || damage <= thresholds.getFirst()) return 0;
        for (int i=1;i<thresholds.size();i++) if (damage<=thresholds.get(i)) {
            double fraction=Math.log(damage/thresholds.get(i-1))/Math.log(thresholds.get(i)/thresholds.get(i-1));
            return SCORES[i-1]+fraction*(SCORES[i]-SCORES[i-1]);
        }
        return Math.min(9, 8+Math.log10(damage/thresholds.getLast()));
    }
    public static Estimate estimate(Observation o, ExternalPowerConfig.Settings s, int observations) {
        var t=o.traits();
        double damage=damageScore(Math.max(o.raw(),o.burstDamage()),s.thresholds());
        double actor=Math.max(0,Math.log10(Math.max(20,o.maxHealth())/20))*.75
                +Math.max(0,Math.log10(Math.max(4,o.attackDamage())/4))*.65;
        double radius=Math.max(o.explosionRadius(),o.blockRadius());
        double blast=t.explosion() && radius>0 ? Math.max(0,Math.log(radius/2)/Math.log(2)) : 0;
        if(t.explosion() && radius>0 && o.explosionDistance()>=0) {
            // Falloff reduces actual damage already. A distant sample should not hide the source's size.
            double exposure=Math.clamp(1-o.explosionDistance()/(radius*2),.1,1);
            blast=Math.max(blast,damage*.55+Math.log10(1/exposure)*.2);
        }
        String path=o.key().damageType().getPath();
        boolean nuclear=t.explosion() && (path.contains("nuclear")||path.contains("nuke")||path.contains("antimatter"));
        if(nuclear) blast=Math.max(3,blast); // Generic semantic clue, never a fixed tier for a particular mod.
        double score=Math.max(damage*.55,Math.max(actor,blast));
        score+=t.magic()?.35:0;score+=t.projectile()?.15:0;score+=t.lightning()?.5:0;
        if(t.explosion())score+=.25;
        if(o.burstHits()>1)score+=Math.min(.6,Math.log10(o.burstHits())*.35);
        int evidence=(t.projectile()?1:0)+(t.magic()?1:0)+(t.explosion()?1:0)+(t.lightning()?1:0)
                +(o.maxHealth()>0?1:0)+(radius>0?2:0)+(t.specialKill()?1:0);
        double confidence=Math.min(.95,.4+evidence*.085+Math.min(4,observations-1)*.025);
        // An opaque million-damage number is not proof of transcendent power.
        if(evidence==0)score=Math.min(score,2);
        else if(confidence<.6 && radius<=0 && actor<3)score=Math.min(score,4);
        int power=Math.clamp((int)Math.round(score),s.minimum(),s.maximum());
        double penetration=(t.bypassArmor()?s.armor():0)
                +(t.bypassEffects()||t.bypassEnchantments()?s.effects():0)
                +(t.bypassInvulnerability()?s.invulnerability():0)+(t.specialKill()?2:0);
        // Penetration describes defense bypass. Vanilla already applies those tags; do not count them again as destructive power.
        return new Estimate(damage,penetration,power,power+(int)Math.ceil(penetration),confidence);
    }
    public Estimate analyze(Observation o, ExternalPowerConfig.Settings settings) {
        if(!settings.equals(previousSettings)){cache.clear();previousSettings=settings;}
        Cached old=cache.get(o.key());
        int count=old==null?1:Math.min(1000,old.observations()+1);
        boolean changed=old==null || count<=4 || !old.baseline().traits().equals(o.traits())
                || different(old.baseline().burstDamage(),o.burstDamage())
                || different(old.baseline().raw(),o.raw()) || different(old.baseline().maxHealth(),o.maxHealth())
                || different(old.baseline().attackDamage(),o.attackDamage())
                || different(old.baseline().explosionRadius(),o.explosionRadius())
                || different(old.baseline().blockRadius(),o.blockRadius())
                || (old.baseline().burstHits()==1)!=(o.burstHits()==1);
        Estimate result=changed?estimate(o,settings,count):old.estimate();
        cache.put(o.key(),new Cached(changed?o:old.baseline(),result,count));
        if(cache.size()>512)cache.pollFirstEntry();
        return result;
    }
    private static boolean different(double a,double b) {
        return (a<=0)!=(b<=0) || Math.abs(a-b)>Math.max(1,Math.abs(a))*.5;
    }
    public void clear(){cache.clear();previousSettings=null;}
    int size(){return cache.size();}
}
