package com.example.immortal_cultivation_mod.physics;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.List;

/** Swept contacts against block collision boxes, including thin/slab/stair geometry. */
public final class WhipContacts implements WhipRope.Contact {
    private final List<AABB> boxes;
    public WhipContacts(List<AABB> boxes){this.boxes=List.copyOf(boxes);}
    @Override public Vec3 project(Vec3 from,Vec3 to,double radius) {
        Vec3 result=to;
        for(AABB raw:boxes) {
            AABB box=raw.inflate(radius);
            var hit=box.clip(from,result);
            if(!box.contains(from) && hit.isPresent()) {
                Vec3 delta=result.subtract(from);double length=delta.length();
                if(length>1e-9)result=hit.get().subtract(delta.scale(.0001/length));
            }
            if(box.contains(result)) {
                double[] d={result.x-box.minX,box.maxX-result.x,result.y-box.minY,box.maxY-result.y,result.z-box.minZ,box.maxZ-result.z};
                int side=0;for(int i=1;i<6;i++)if(d[i]<d[side])side=i;
                result=switch(side) {
                    case 0->new Vec3(box.minX-.0001,result.y,result.z);
                    case 1->new Vec3(box.maxX+.0001,result.y,result.z);
                    case 2->new Vec3(result.x,box.minY-.0001,result.z);
                    case 3->new Vec3(result.x,box.maxY+.0001,result.z);
                    case 4->new Vec3(result.x,result.y,box.minZ-.0001);
                    default->new Vec3(result.x,result.y,box.maxZ+.0001);
                };
            }
        }
        return result;
    }
}
