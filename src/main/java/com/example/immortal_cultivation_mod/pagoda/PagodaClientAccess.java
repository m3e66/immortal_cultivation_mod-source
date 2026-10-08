package com.example.immortal_cultivation_mod.pagoda;

/** Client implementation is installed only by the client setup event. */
public final class PagodaClientAccess {
    public static java.util.function.Consumer<PagodaEntity> tick=e->{};
    public static void tick(PagodaEntity entity){tick.accept(entity);}
    private PagodaClientAccess(){}
}
