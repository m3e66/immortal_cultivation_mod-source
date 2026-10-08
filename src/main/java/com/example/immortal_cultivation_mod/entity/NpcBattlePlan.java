package com.example.immortal_cultivation_mod.entity;

import java.util.UUID;
import net.minecraft.util.Mth;

/** Encounter-local mentality and utility inputs. Enemy values must come from observations. */
public final class NpcBattlePlan {
    public enum Mode { NORMAL, SURVIVAL, DEATH_FRENZY }
    public enum Phase { OBSERVE, PROBE, COUNTER, PRESSURE, FINISH }
    public enum Momentum { DOMINATING, ADVANTAGE, EVEN, DISADVANTAGE, COLLAPSING }
    public record Situation(float health,float qi,int debuffs,int allies,int enemies,int realmGap,
                            float enemyHealth,float enemyQi,boolean opening,boolean escapeRoute,
                            boolean escapeAbility,boolean healing,boolean defense,boolean trapped,
                            float recentDamage,boolean sparring) {}
    private UUID opponent;
    private long began,blockedSince=-1,lastFailure=-1,lastSafe=-1;
    private int escapeFailures;
    private Mode mode=Mode.NORMAL;
    private Phase phase=Phase.OBSERVE;
    private Momentum momentum=Momentum.EVEN;
    private float confidence=1;
    private float experience;
    private String personality="cautious";

    public void begin(UUID enemy,long now,int selfRealm,String personality) {
        if(enemy.equals(opponent))return;
        reset();opponent=enemy;began=now;
        this.personality=personality;
        experience=Mth.clamp(selfRealm/13F,0,1);
    }
    public void reset() {
        opponent=null;mode=Mode.NORMAL;phase=Phase.OBSERVE;momentum=Momentum.EVEN;
        confidence=1;blockedSince=lastFailure=lastSafe=-1;escapeFailures=0;
    }
    public void failedEscape(long now) {
        if(lastFailure<0 || now-lastFailure>=12){escapeFailures++;lastFailure=now;}
    }
    public void escaped(long now) {escapeFailures=0;blockedSince=-1;lastSafe=now;}
    public void reassess(long now) {began=now;phase=Phase.OBSERVE;}
    public void update(Situation s,long now,int observations) {
        float caution=switch(personality){case "cautious","cowardly"->.12F;case "proud","aggressive"->-.08F;default->0;};
        float risk=s.health()*.40F+s.qi()*.15F+(s.escapeRoute()?.22F:0)+(s.escapeAbility()?.30F:0)
                +(s.healing()?.16F:0)+(s.defense()?.08F:0)+Math.min(.16F,s.allies()*.04F)
                -Math.max(0,s.enemies()-s.allies()-1)*.07F-Math.max(0,s.realmGap())*.045F
                -s.debuffs()*.04F-(s.trapped()?.16F:0)-Math.min(.32F,s.recentDamage()*.7F)-caution;
        confidence=Mth.clamp(risk,0,1);
        boolean unfavorable=s.health()<.24F+caution || s.qi()<.10F || s.realmGap()>=3 && s.allies()<s.enemies()+1
                || confidence<.28F || s.debuffs()>=3 || s.recentDamage()>.32F;
        // A failed navigation sample is not a death sentence. Repeated failed exits and a sustained
        // lack of affordable healing/movement are required, including for the 40%-HP trapped case.
        boolean cornered=!s.escapeRoute()&&!s.escapeAbility()&&!s.healing();
        if(cornered && unfavorable){if(blockedSince<0)blockedSince=now;}else blockedSince=-1;
        boolean lethal=s.health()<.18F || s.health()<.5F && (s.realmGap()>=4 || s.recentDamage()>.28F || s.trapped());
        boolean doomed=!s.sparring() && lethal && confidence<=.12F && cornered
                && blockedSince>=0 && now-blockedSince>=60 && escapeFailures>=3;
        if(doomed)mode=Mode.DEATH_FRENZY;
        else if(mode==Mode.DEATH_FRENZY && (s.escapeRoute()||s.escapeAbility()||s.healing()||!lethal))mode=Mode.SURVIVAL;
        else if(mode!=Mode.DEATH_FRENZY) {
            if(unfavorable)mode=Mode.SURVIVAL;
            else if(mode==Mode.SURVIVAL && confidence>.52F && s.health()>.38F && s.qi()>.18F)mode=Mode.NORMAL;
        }
        float advantage=(s.health()-s.enemyHealth())*.65F+(s.qi()-Math.max(.3F,s.enemyQi()))*.2F
                -s.realmGap()*.06F+(s.allies()-s.enemies()+1)*.06F+(s.opening()?.27F:0);
        momentum=advantage>.40F?Momentum.DOMINATING:advantage>.15F?Momentum.ADVANTAGE:
                advantage<-.40F?Momentum.COLLAPSING:advantage<-.15F?Momentum.DISADVANTAGE:Momentum.EVEN;
        long probeTicks=(long)(60+experience*100+(personality.equals("cautious")?60:personality.equals("aggressive")?-30:0));
        phase=s.enemyHealth()<=.20F?Phase.FINISH:s.opening()||advantage>.15F&&observations>=3?Phase.PRESSURE:
                now-began<20?Phase.OBSERVE:now-began<probeTicks || observations<3?Phase.PROBE:Phase.COUNTER;
    }
    public Mode mode(){return mode;}
    public Phase phase(){return phase;}
    public Momentum momentum(){return momentum;}
    public float confidence(){return confidence;}
    public int escapeFailures(){return escapeFailures;}
    public float experience(){return experience;}
    public boolean probing(){return phase==Phase.OBSERVE||phase==Phase.PROBE;}
    public boolean pressure(){return phase==Phase.PRESSURE||phase==Phase.FINISH;}
    public boolean frenzy(){return mode==Mode.DEATH_FRENZY;}
    public boolean loyal(){return personality.equals("gentle")||personality.equals("loyal");}
    public boolean cunning(){return personality.equals("scholarly")||personality.equals("scheming")||personality.equals("wandering");}
    public float reserveFraction(){return mode!=Mode.NORMAL?0:.12F+experience*.13F+(personality.equals("cautious")?.08F:0);}
    public double revealPenalty(double costFraction,boolean revealed,boolean emergency) {
        if(mode!=Mode.NORMAL||emergency||revealed)return 0;
        return costFraction<.05?0:(costFraction>=.18?45:18)*(probing()?1.5:pressure()?.3:1)*(0.5+experience);
    }
    public boolean canSpend(int cost,int qi,int maxQi,boolean emergency) {
        if(cost>qi)return false;
        return frenzy()||mode==Mode.SURVIVAL||emergency||cost==0
                || qi-cost>=maxQi*reserveFraction() && (cost<qi*.4 || pressure());
    }
}
