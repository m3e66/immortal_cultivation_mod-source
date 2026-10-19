package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.entity.ModEntities;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;

public final class TrainingDummyItem extends Item {
    public TrainingDummyItem(Properties properties){super(properties);}
    @Override public InteractionResult useOn(UseOnContext context) {
        var level=context.getLevel();var player=context.getPlayer();if(player==null)return InteractionResult.PASS;
        if(!level.isClientSide) {
            var dummy=ModEntities.TRAINING_DUMMY.get().create(level);if(dummy==null)return InteractionResult.FAIL;
            Vec3 p=Vec3.atBottomCenterOf(context.getClickedPos().relative(context.getClickedFace()));
            dummy.moveTo(p.x,p.y,p.z,player.getYRot()+180,0);dummy.controller=player.getUUID();dummy.ensureProfile();
            if(!level.getWorldBorder().isWithinBounds(dummy.blockPosition())||!level.noCollision(dummy))return InteractionResult.FAIL;
            level.addFreshEntity(dummy);if(!player.isCreative())context.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
