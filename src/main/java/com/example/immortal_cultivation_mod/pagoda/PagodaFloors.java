package com.example.immortal_cultivation_mod.pagoda;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import net.minecraft.world.entity.LivingEntity;

/** Floor one is Lianqi; floor nine is Dujie. Owners are visitors, not prisoners. */
public final class PagodaFloors {
    public static int unlocked(String realm){return Math.clamp(CultivationLevels.getRealmIndex(realm),1,9);}
    public static int floor(double y){return Math.clamp((int)((y-64)/9),0,8);}
    public static long occupancy(PagodaData.Tower tower,int floor){return tower.prisoners.values().stream().filter(p->!p.visitor&&p.floor==floor).count();}
    public static int available(PagodaData.Tower tower,LivingEntity entity){
        if(tower.gourd)return hasRoom(tower)?0:-1;
        int preferred=Math.clamp(com.example.immortal_cultivation_mod.externalpower.ExternalPowerTargets.realm(entity)-1,0,tower.unlockedFloors-1);
        for(int offset=0;offset<tower.unlockedFloors;offset++){
            int floor=(preferred+offset)%tower.unlockedFloors;
            if(occupancy(tower,floor)<10)return floor;
        }
        return -1;
    }
    public static boolean hasRoom(PagodaData.Tower tower){
        if(tower.gourd)return tower.prisoners.size()<PagodaConfig.MAX_PRISONERS.get();
        for(int i=0;i<tower.unlockedFloors;i++)if(occupancy(tower,i)<10)return true;
        return false;
    }
    private PagodaFloors(){}
}
