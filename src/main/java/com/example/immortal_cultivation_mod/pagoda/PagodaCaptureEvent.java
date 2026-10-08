package com.example.immortal_cultivation_mod.pagoda;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/** Protection/claim integrations can veto capture without manufacturing damage. */
public final class PagodaCaptureEvent extends Event implements ICancellableEvent {
    public final ServerPlayer owner;
    public final LivingEntity target;
    public PagodaCaptureEvent(ServerPlayer owner,LivingEntity target){this.owner=owner;this.target=target;}
}
