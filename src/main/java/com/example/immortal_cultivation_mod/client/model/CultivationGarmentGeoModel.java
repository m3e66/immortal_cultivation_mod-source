package com.example.immortal_cultivation_mod.client.model;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.item.CultivationGarmentItem;
import com.example.immortal_cultivation_mod.client.RealmPressureClient;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.model.HumanoidModel;
import org.joml.Vector3f;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import java.util.WeakHashMap;

/** Independent animation for the flowing cape and the hanging hat veil. */
public final class CultivationGarmentGeoModel extends DefaultedItemGeoModel<CultivationGarmentItem> {
    private final boolean cloak;
    private final boolean veil;
    private final WeakHashMap<LivingEntity, Spring> springs = new WeakHashMap<>();
    private LivingEntity wearer;
    private float partial;
    private HumanoidModel<?> posedModel;
    private float clothSway, clothHem, clothTurn, clothTime;
    private final WeakHashMap<LivingEntity, FlowingCape.Motion> capes = new WeakHashMap<>();
    private FlowingCape.Motion capeMotion;

    public CultivationGarmentGeoModel(String name) {
        super(ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "armor/" + name));
        cloak = name.equals("doupeng");
        veil = name.equals("veiled_douli");
    }

    public void prepare(LivingEntity entity, float partialTick, HumanoidModel<?> model) {
        wearer = entity;
        partial = partialTick;
        posedModel = model;
    }

    @Override
    public void setCustomAnimations(CultivationGarmentItem item, long instanceId,
                                    AnimationState<CultivationGarmentItem> state) {
        super.setCustomAnimations(item, instanceId, state);
        if ((!cloak && !veil) || wearer == null) return;
        double now = wearer.tickCount + partial;
        clothTime=(float)now;
        // Position deltas also work for remote players whose velocity may be zero.
        double dx = wearer.getX() - wearer.xo;
        double dz = wearer.getZ() - wearer.zo;
        double speed = Math.min(.8, Math.max(Math.hypot(dx, dz),
                wearer.getDeltaMovement().horizontalDistance()));
        speed = Math.min(.8, speed + RealmPressureClient.strengthAt(wearer.position()) * .18);
        if(cloak) {
            capeMotion=capes.computeIfAbsent(wearer,ignored -> new FlowingCape.Motion(now));
            capeMotion.advance(now,(float)speed,Mth.wrapDegrees(wearer.yBodyRot-wearer.yBodyRotO));
            return;
        }
        Spring spring=springs.computeIfAbsent(wearer,ignored -> new Spring(now));
        double dt=Math.max(0,Math.min((now-spring.time)/20,.1));
        spring.time=now;
        double target = Math.min(.32, speed * 1.1)
                + Math.sin(now * .42) * Math.min(.025, speed * .12);
        if (wearer.isCrouching()) target += .05;
        double turn = Mth.clamp(Mth.wrapDegrees(wearer.yBodyRot - wearer.yBodyRotO) * .009, -.13, .13);
        double lift = Mth.clamp((wearer.getY() - wearer.yo) * .16, -.04, .07);
        // Substeps make the damped spring stable at low frame rates.
        int steps = Math.max(1, (int)Math.ceil(dt / .01));
        double step = dt / steps;
        for (int i = 0; i < steps; i++) {
            spring.velocity += ((target + lift - spring.angle) * 48 - spring.velocity * 10) * step;
            spring.angle = Mth.clamp(spring.angle + spring.velocity * step, -.05, .4);
            double hemTarget=spring.angle*.65;
            spring.hemVelocity += ((hemTarget - spring.hem) * 32 - spring.hemVelocity * 8) * step;
            spring.hem = Mth.clamp(spring.hem + spring.hemVelocity * step, -.08, .28);
            spring.turnVelocity += ((turn - spring.turn) * 40 - spring.turnVelocity * 9) * step;
            spring.turn = Mth.clamp(spring.turn + spring.turnVelocity * step, -.15, .15);
        }
        clothSway = (float)spring.angle;
        clothHem = (float)spring.hem;
        clothTurn = (float)spring.turn;

    }

    public Vector3f deform(Vector3f position) {
        if ((!cloak && !veil) || posedModel == null) return position;
        float x = position.x * 16, y = position.y * 16, z = position.z * 16;
        if (veil) {
            // Shared continuous displacement keeps adjoining veil panels connected.
            // Hat and brim stay rigid; displacement vanishes at the stitched top.
            float drop = Math.max(0, 32 - y);
            float bend = drop * drop / 20F;
            position.z += bend * clothSway * .55F / 16;
            position.x += bend * clothTurn * .65F / 16;
            return position;
        }
        if(capeMotion!=null) {
            FlowingCape.Point point=FlowingCape.sample(x,y,capeMotion.amount(),capeMotion.turn(),clothTime);
            position.set(point.x()/16,point.y()/16,point.z()/16);
        }
        return position;
    }

    public boolean hasClothPhysics() { return cloak || veil; }

    private static final class Spring {
        double time, angle, velocity, hem, hemVelocity, turn, turnVelocity;
        Spring(double time) { this.time = time; }
    }
}
