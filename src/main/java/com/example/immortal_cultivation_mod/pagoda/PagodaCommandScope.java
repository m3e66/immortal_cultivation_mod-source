package com.example.immortal_cultivation_mod.pagoda;

public final class PagodaCommandScope {
    private static final ThreadLocal<Boolean> ADMIN=ThreadLocal.withInitial(()->false);
    public static boolean admin(){return ADMIN.get();}
    public static boolean set(boolean value){boolean old=ADMIN.get();ADMIN.set(value);return old;}
    private PagodaCommandScope(){}
}
