package com.example.immortal_cultivation_mod.client.qi;

import com.example.immortal_cultivation_mod.combat.MethodDomainStyle;

/** The ten visual families share a form but have different flow and surface behavior. */
public record QiElementLook(int color,int highlight,int family,float speed,int art) {
    public QiElementLook(int color,int highlight,int family,float speed){this(color,highlight,family,speed,-1);}
    public int shaderCode(int coating){return family+coating+((art+1)<<6);}
    public static final int FIRE=0,WATER=1,WOOD=2,METAL=3,EARTH=4,LIGHTNING=5,ICE=6,WIND=7,LIGHT=8,DARK=9;
    public static QiElementLook of(MethodDomainStyle style) {
        var look=switch(style.kind) {
            case CLEAR -> new QiElementLook(style.color,style.secondary,WATER,.55F);
            case FIRE -> new QiElementLook(0xE94A30,0xFFE7A7,FIRE,1.3F);
            case TIDE -> new QiElementLook(0x12467E,0x83E5F5,WATER,.55F);
            case WOOD -> new QiElementLook(0x3AF649,0xA4FFE5,WOOD,.85F);
            case BONE -> new QiElementLook(0xE6D18D,0xFFFFEE,METAL,1.05F);
            case MOUNTAIN -> new QiElementLook(0xB38239,0xFFE1A0,EARTH,.55F);
            case STORM -> new QiElementLook(0x8660F2,0x65F4DB,LIGHTNING,1.7F);
            case ICE -> new QiElementLook(0x8BDCEA,0xEEFFFF,ICE,.65F);
            case WIND -> new QiElementLook(style.color,style.secondary,WIND,1.6F);
            case SOLAR -> new QiElementLook(0xFFCF72,0xFFFFEF,LIGHT,1.1F);
            case DEVOUR,NETHER,REINCARNATION -> new QiElementLook(style.color,style.secondary,DARK,.8F);
            case BLOOD,DRAIN,CROWN -> new QiElementLook(style.color,style.secondary,FIRE,1.2F);
            default -> new QiElementLook(style.color,style.secondary,WIND,1);
        };
        return new QiElementLook(look.color,look.highlight,look.family,look.speed,style.ordinal());
    }
}
