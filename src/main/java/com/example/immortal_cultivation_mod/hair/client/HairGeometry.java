package com.example.immortal_cultivation_mod.hair.client;

import com.example.immortal_cultivation_mod.hair.*;
import com.google.gson.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.*;

/** Head-local geometry shared by the world layer and the styling preview. */
public final class HairGeometry {
    public static final ResourceLocation TEXTURE=id("textures/hair/locks.png");
    private static final ResourceLocation ORNAMENT_TEXTURE=id("textures/hair/strands.png");
    private static Map<String,Model> models;
    private record Box(ModelPart part,int tint,float shade) {}
    private record MotionSegment(List<Box> boxes,Vector3f center,float bend) {}
    private record Part(ModelPart node,boolean motion,List<Box> boxes,List<org.joml.Vector3f[]> collisionBoxes,List<MotionSegment> segments) {}
    private record Anchor(String part,float x,float y,float z) {}
    private record Model(Map<String,Part> parts,Map<String,Anchor> anchors) {}
    private static final ModelPart SKIN=skin();
    private HairGeometry() {}
    public static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod",path); }
    public static void reload() { models=null; }
    private static ModelPart skin() {
        MeshDefinition mesh=new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("head",CubeListBuilder.create().texOffs(0,0).addBox(-4,-8,-4,8,8,8)
                .texOffs(32,0).addBox(-4,-8,-4,8,8,8,new CubeDeformation(.5F)),PartPose.ZERO);
        return LayerDefinition.create(mesh,64,64).bakeRoot();
    }
    private static float n(JsonObject o,String key,int i) {return o.getAsJsonArray(key).get(i).getAsFloat();}
    private static List<MotionSegment> segment(List<Box> boxes) {
        if(boxes.size()<3)return List.of();
        var ordered=boxes.stream().map(b->b.part.y).sorted().toList();
        var levels=new ArrayList<Float>();
        for(float y:ordered)if(levels.isEmpty() || Math.abs(y-levels.getLast())>.05F)levels.add(y);
        if(levels.size()<3)return List.of();
        int count=Math.clamp(levels.size(),3,5);
        var groups=new ArrayList<List<Box>>(count);
        for(int i=0;i<count;i++)groups.add(new ArrayList<>());
        for(var box:boxes){
            int level=0;float best=Float.POSITIVE_INFINITY;
            for(int i=0;i<levels.size();i++){
                float distance=Math.abs(box.part.y-levels.get(i));
                if(distance<best){best=distance;level=i;}
            }
            int index=Math.min(count-1,level*count/levels.size());
            groups.get(index).add(box);
        }
        var result=new ArrayList<MotionSegment>(count);
        for(int i=0;i<count;i++){
            var group=groups.get(i);if(group.isEmpty())continue;
            var center=new Vector3f();
            for(var box:group)center.add(box.part.x,box.part.y,box.part.z);
            center.div(group.size());
            // The root still bends, but the tip receives the full hanging correction.
            float bend=.35F+.65F*(i+1F)/count;
            result.add(new MotionSegment(List.copyOf(group),center,bend));
        }
        return List.copyOf(result);
    }
    private static void load() {
        if(models!=null)return;
        var result=new HashMap<String,Model>();
        try(var reader=Minecraft.getInstance().getResourceManager().openAsReader(id("hair/models.json"))) {
            var json=JsonParser.parseReader(reader).getAsJsonObject();
            for(var entry:json.entrySet()) {
                JsonObject data=entry.getValue().getAsJsonObject();MeshDefinition mesh=new MeshDefinition();
                for(var value:data.getAsJsonArray("parts")) {
                    var p=value.getAsJsonObject();String name=p.get("name").getAsString();
                    var def=mesh.getRoot().addOrReplaceChild(name,CubeListBuilder.create(),PartPose.offsetAndRotation(
                            n(p,"pivot",0),n(p,"pivot",1),n(p,"pivot",2),
                            (float)Math.toRadians(n(p,"rotation",0)),(float)Math.toRadians(n(p,"rotation",1)),(float)Math.toRadians(n(p,"rotation",2))));
                    int i=0;
                    for(var box:p.getAsJsonArray("boxes")) {
                        var b=box.getAsJsonObject();
                        PartPose transform=b.has("pivot")?PartPose.offsetAndRotation(n(b,"pivot",0),n(b,"pivot",1),n(b,"pivot",2),
                                (float)Math.toRadians(n(b,"rotation",0)),(float)Math.toRadians(n(b,"rotation",1)),(float)Math.toRadians(n(b,"rotation",2))):PartPose.ZERO;
                        def.addOrReplaceChild("box"+i++,CubeListBuilder.create().texOffs(b.has("uv")?(int)n(b,"uv",0):0,b.has("uv")?(int)n(b,"uv",1):0).addBox(
                                n(b,"from",0),n(b,"from",1),n(b,"from",2),n(b,"size",0),n(b,"size",1),n(b,"size",2)),transform);
                    }
                }
                ModelPart root=LayerDefinition.create(mesh,64,64).bakeRoot();var parts=new LinkedHashMap<String,Part>();
                for(var value:data.getAsJsonArray("parts")) {
                    var p=value.getAsJsonObject();String name=p.get("name").getAsString();var node=root.getChild(name);
                    var boxes=new ArrayList<Box>();int i=0;
                    for(var box:p.getAsJsonArray("boxes")) {
                        var b=box.getAsJsonObject();boxes.add(new Box(node.getChild("box"+i++),b.get("color").getAsInt(),b.has("shade")?b.get("shade").getAsFloat():1));
                    }
                    boolean motion=p.get("motion").getAsBoolean();
                    parts.put(name,new Part(node,motion,boxes,HairLookAdjustment.boxes(p),motion&&HairLookAdjustment.isBackHair(name)?segment(boxes):List.of()));
                }
                var anchors=new HashMap<String,Anchor>();
                for(var e:data.getAsJsonObject("anchors").entrySet()) {
                    var a=e.getValue().getAsJsonObject();anchors.put(e.getKey(),new Anchor(a.get("part").getAsString(),n(a,"pos",0),n(a,"pos",1),n(a,"pos",2)));
                }
                result.put(entry.getKey(),new Model(parts,anchors));
            }
        } catch(Exception e) {throw new IllegalStateException("Unable to load hair geometry",e);}
        models=result;
    }
    private static void pose(Model m,float time,float movement,float headPitch,HairChestPose frame) {
        pose(m,time,movement,headPitch,frame,0);
    }
    private static void pose(Model m,float time,float movement,float headPitch,HairChestPose frame,float pressure) {
        for(var entry:m.parts.entrySet()) {
            var p=entry.getValue();
            p.node.resetPose();
            for(var box:p.boxes)box.part.resetPose();
            // Face-framing locks and side hair must remain attached to the head.
            if(p.motion && frame!=null && HairLookAdjustment.isBackHair(entry.getKey())){
                if(p.segments.size()>=3)poseSegments(p,frame);
                else frame.apply(p.node,p.collisionBoxes);
            }
            if(p.motion && pressure>0) {
                p.node.xRot -= pressure*(.16F+.025F*(float)Math.sin(time*.12F));
                p.node.zRot += Math.signum(p.node.x)*pressure*.12F;
            }
        }
    }
    /** Bend one logical back-hair lock as a short 3-5 section chain instead of one rigid chunk. */
    private static void poseSegments(Part part,HairChestPose frame){
        var target=frame.target(part.node,part.collisionBoxes);
        var bind=new Quaternionf().rotationZYX(part.node.zRot,part.node.yRot,part.node.xRot);
        var full=new Quaternionf().rotationZYX(target.z,target.y,target.x);
        var delta=new Quaternionf(bind).invert().mul(full).normalize();
        // +Y is down in model space. Rotating it by the complete hanging correction gives
        // the gravity direction in this head-local render space.
        var gravity=delta.transform(new Vector3f(0,1,0)).normalize();
        var previousBind=new Vector3f();
        var previousMoved=new Vector3f();
        for(int index=0;index<part.segments.size();index++){
            var segment=part.segments.get(index);
            var bend=new Quaternionf().identity().slerp(delta,segment.bend);
            var link=new Vector3f(segment.center).sub(previousBind);
            var movedLink=bend.transform(link,new Vector3f());
            float progress=(index+1F)/part.segments.size();
            // Keep the upper strand's authored shape, then increasingly let gravity win.
            // The final link points fully downward instead of preserving a diagonal tail.
            float settle=progress*progress*progress;
            var downLink=new Vector3f(gravity).mul(link.length());
            movedLink.lerp(downLink,settle);
            var movedCenter=new Vector3f(previousMoved).add(movedLink);
            for(var box:segment.boxes){
                var p=box.part;
                var offset=new Vector3f(p.x,p.y,p.z).sub(segment.center);
                bend.transform(offset);
                var moved=new Vector3f(movedCenter).add(offset);
                var boxBind=new Quaternionf().rotationZYX(p.zRot,p.yRot,p.xRot);
                // Remove local forward/side tilt toward the end so the visible bottom
                // section itself hangs vertically, not just its attachment points.
                var straightBind=new Quaternionf().rotationY(p.yRot);
                boxBind.slerp(straightBind,settle);
                var rotation=new Quaternionf(bend).mul(boxBind).getEulerAnglesZYX(new Vector3f());
                p.x=moved.x;p.y=moved.y;p.z=moved.z;
                p.xRot=rotation.x;p.yRot=rotation.y;p.zRot=rotation.z;
            }
            previousBind.set(segment.center);
            previousMoved.set(movedCenter);
        }
    }
    private static void draw(Model model,PoseStack poses,MultiBufferSource buffers,int light,int color,boolean compact,ResourceLocation texture) {
        var vertices=buffers.getBuffer(RenderType.entityCutoutNoCull(texture));
        for(var entry:model.parts.entrySet()) {
            var part=entry.getValue();
            if(compact && (part.motion || entry.getKey().startsWith("bun") || entry.getKey().startsWith("loop")))continue;
            poses.pushPose();part.node.translateAndRotate(poses);
            for(var box:part.boxes) {
                int tint=box.tint<0?color:box.tint;
                int red=Math.clamp(Math.round((tint>>16&255)*box.shade),0,255);
                int green=Math.clamp(Math.round((tint>>8&255)*box.shade),0,255);
                int blue=Math.clamp(Math.round((tint&255)*box.shade),0,255);
                box.part.render(poses,vertices,light,OverlayTexture.NO_OVERLAY,0xFF000000|red<<16|green<<8|blue);
            }
            poses.popPose();
        }
    }
    public static void render(PoseStack poses,MultiBufferSource buffers,int light,int style,int color,int accessory,
                              int helmetMode,boolean helmet,float time,float movement,float headPitch) {
        render(poses,buffers,light,style,color,accessory,helmetMode,helmet,time,movement,headPitch,null);
    }
    static void render(PoseStack poses,MultiBufferSource buffers,int light,int style,int color,int accessory,
                              int helmetMode,boolean helmet,float time,float movement,float headPitch,HairChestPose frame) {
        render(poses,buffers,light,style,color,accessory,helmetMode,helmet,time,movement,headPitch,frame,0);
    }
    static void render(PoseStack poses,MultiBufferSource buffers,int light,int style,int color,int accessory,
                              int helmetMode,boolean helmet,float time,float movement,float headPitch,HairChestPose frame,float pressure) {
        load();if(helmet && helmetMode==2)return;
        Model hair=style<0?null:models.get(HairStyle.values()[style].id());
        boolean compact=helmet && helmetMode==1;
        poses.pushPose();
        // Flatten the remaining crown and fringe under armor after omitting the long parts.
        if(compact)poses.scale(.88F,.96F,.88F);
        try {
        if(hair!=null){pose(hair,time,movement,headPitch,compact?null:frame,compact?0:pressure);draw(hair,poses,buffers,light,color,compact,TEXTURE);}
        if(accessory<0)return;
        String preferred=switch(accessory){case 0 -> "bun";case 1 -> "ponytail";case 2 -> "top";case 3 -> "left";default -> "braid";};
        Anchor anchor=hair==null?null:hair.anchors.get(preferred);
        if(anchor==null && hair!=null)anchor=hair.anchors.get(accessory==4?"ponytail":accessory==0?"top":"back");
        poses.pushPose();
        if(anchor!=null){
            var parent=hair.parts.get(anchor.part);
            if(compact && (parent.motion || anchor.part.startsWith("bun"))){poses.popPose();return;}
            parent.node.translateAndRotate(poses);poses.translate(anchor.x/16F,anchor.y/16F,anchor.z/16F);
        } else {
            // Separate attachment sites remain usable even with the hair slot empty.
            poses.translate(accessory==3?-.29:0,accessory==2||accessory==0?-.58:-.3,accessory==1||accessory==4?.29:0);
        }
        Model ornament=models.get(HairContent.ACCESSORY_IDS.get(accessory));pose(ornament,time,movement,0,null);
        draw(ornament,poses,buffers,light,0xFFFFFF,false,ORNAMENT_TEXTURE);poses.popPose();
        } finally {poses.popPose();}
    }
    public static void head(PoseStack pose,MultiBufferSource buffer,ResourceLocation texture,int light) {
        SKIN.render(pose,buffer.getBuffer(RenderType.entityCutoutNoCull(texture)),light,OverlayTexture.NO_OVERLAY,-1);
    }
}
