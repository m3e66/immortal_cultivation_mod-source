package com.example.immortal_cultivation_mod.animation;

import software.bernie.geckolib.animation.RawAnimation;

/** Per-entity animation timing. Only visual state is kept here; combat rules are unchanged. */
public final class SpiritPuppetAnimations {
    public enum Clip {
        IDLE("idle",3.2,true), WALK("walk",1.15,true), RUN("run",.72,true),
        WALK_BACK("walk_back",1.3,true), STRAFE_LEFT("strafe_left",1.05,true), STRAFE_RIGHT("strafe_right",1.05,true),
        JUMP("jump",.35,false), FALL("fall",.9,true), LAND("land",.35,false),
        FLY("fly",1.8,true), SWIM("swim",1.2,true),
        ATTACK_RIGHT("attack_right",.6,false), ATTACK_LEFT("attack_left",.6,false),
        CAST_CHARGE("cast_charge",.55,false), CAST_HOLD("cast_hold",1.6,true), CAST_RELEASE("cast_release",.45,false),
        HURT("hurt",.4,false), DEATH("death",.85,false);

        public final String name;
        public final double seconds;
        public final boolean loop;
        public final RawAnimation animation;
        Clip(String suffix,double seconds,boolean loop) {
            this.name="animation.spirit_puppet."+suffix;this.seconds=seconds;this.loop=loop;
            var raw=RawAnimation.begin();
            this.animation=loop?raw.thenLoop(name):suffix.equals("death")||suffix.equals("jump")?raw.thenPlayAndHold(name):raw.thenPlay(name);
        }
    }
    public record Snapshot(double time,boolean alive,boolean grounded,boolean flight,boolean water,
                           double verticalSpeed,double forwardSpeed,double rightSpeed,boolean sprinting,
                           int hurtTime,float swingProgress,boolean leftAttack,int castingTicks) {}
    public record Selection(Clip clip,boolean restart,double speed) {}
    private boolean initialized,grounded,casting;
    private int lastHurt;
    private float lastSwing;
    private double lastTime,airborneAt=-1000,hurtAt=-1000,attackAt=-1000,castAt=-1000,releaseAt=-1000,landAt=-1000;
    private Clip attack=Clip.ATTACK_RIGHT,current=Clip.IDLE;

    public Clip current(){return current;}

    public Selection update(Snapshot s) {
        if(initialized&&s.time<lastTime)initialized=false;
        if(!initialized) {
            grounded=s.grounded;casting=false;lastHurt=0;lastSwing=0;
            airborneAt=hurtAt=attackAt=castAt=releaseAt=landAt=-1000;
        }
        boolean hurt=s.hurtTime>lastHurt;
        boolean swing=s.swingProgress>0&&(lastSwing<=0||s.swingProgress+.1F<lastSwing);
        if(hurt)hurtAt=s.time;
        if(swing){attackAt=s.time;attack=s.leftAttack?Clip.ATTACK_LEFT:Clip.ATTACK_RIGHT;}
        if(s.castingTicks>0&&!casting)castAt=s.time;
        if(s.castingTicks==0&&casting)releaseAt=s.time;
        if(initialized&&grounded&&!s.grounded)airborneAt=s.time;
        if(initialized&&!grounded&&s.grounded&&s.time-airborneAt>.1)landAt=s.time;
        Clip next;
        double speed=1;
        if(!s.alive)next=Clip.DEATH;
        else if(s.time-hurtAt<Clip.HURT.seconds)next=Clip.HURT;
        else if(s.castingTicks>0)next=s.time-castAt<Clip.CAST_CHARGE.seconds?Clip.CAST_CHARGE:Clip.CAST_HOLD;
        else if(s.time-releaseAt<Clip.CAST_RELEASE.seconds)next=Clip.CAST_RELEASE;
        else if(s.time-attackAt<attack.seconds)next=attack;
        else if(s.water)next=Clip.SWIM;
        else if(s.flight&&!s.grounded)next=Clip.FLY;
        else if(!s.grounded)next=s.verticalSpeed>.035?Clip.JUMP:Clip.FALL;
        else if(s.time-landAt<Clip.LAND.seconds)next=Clip.LAND;
        else {
            double horizontal=Math.hypot(s.forwardSpeed,s.rightSpeed);
            if(horizontal<.012)next=Clip.IDLE;
            else if(s.forwardSpeed<-.45*horizontal)next=Clip.WALK_BACK;
            else if(Math.abs(s.rightSpeed)>Math.abs(s.forwardSpeed)*1.3)next=s.rightSpeed>0?Clip.STRAFE_RIGHT:Clip.STRAFE_LEFT;
            else next=s.sprinting||horizontal>.22?Clip.RUN:Clip.WALK;
            double strideSpeed=switch(next){case RUN->.13;case WALK_BACK->.04;case STRAFE_LEFT,STRAFE_RIGHT->.05;default->.055;};
            if(next!=Clip.IDLE)speed=Math.clamp(horizontal/strideSpeed,.65,2.2);
        }
        boolean restart=next==current&&(next==Clip.HURT&&hurt||(next==Clip.ATTACK_LEFT||next==Clip.ATTACK_RIGHT)&&swing);
        current=next;grounded=s.grounded;casting=s.castingTicks>0;lastHurt=s.hurtTime;lastSwing=s.swingProgress;lastTime=s.time;initialized=true;
        return new Selection(next,restart,speed);
    }
}
