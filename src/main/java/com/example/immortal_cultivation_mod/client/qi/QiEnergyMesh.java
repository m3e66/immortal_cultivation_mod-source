package com.example.immortal_cultivation_mod.client.qi;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import com.example.immortal_cultivation_mod.physics.QiBodyFlamePhysics;

/** World-space flames; Reincarnation carries both currents on one surface. */
public final class QiEnergyMesh {
    public static long draws,reincarnationDraws,deformedBodyDraws;
    private static final double TAU = Math.PI * 2;

    private QiEnergyMesh() { }

    public static int draw(PoseStack pose, MultiBufferSource buffers, QiElementLook look, double time,
                           float surge, float alpha, Vec3 drag, boolean small) {
        if (alpha <= 0 || !QiAuraMaterial.ready() || !QiVfxQuality.takeForm()) return 0;
        int quality = QiVfxQuality.level();
        int rows = small ? (quality == 0 ? 12 : 18) : (quality == 0 ? 20 : 32);
        int cols = small ? (quality == 0 ? 16 : 24) : (quality == 0 ? 28 : 40);
        int layers = look.art()==5||look.art()==7 ? 1 : quality == 0 ? 2 : quality == 2 && !small ? 4 : 3;
        return render(pose, buffers, look, time, surge, alpha, drag, rows, cols, layers, quality,1,1,null);
    }

    public static int drawBody(PoseStack pose,MultiBufferSource buffers,QiElementLook look,double time,
                               float surge,float alpha,float radius,float height,QiBodyFlamePhysics.Snapshot deformation) {
        if(alpha<=0||!QiAuraMaterial.ready()||!QiVfxQuality.takeForm())return 0;
        int quality=QiVfxQuality.level(),rows=quality==0?20:32,cols=quality==0?28:40;
        int layers=look.art()==5||look.art()==7?1:quality==0?2:quality==2?4:3;
        deformedBodyDraws++;
        return render(pose,buffers,look,time,surge,alpha,Vec3.ZERO,rows,cols,layers,quality,radius,height,deformation);
    }

    /** Same shape, layers and shader as the body, with a separate budget for flexible weapons. */
    public static int drawWeapon(PoseStack pose, MultiBufferSource buffers, QiElementLook look,
                                 double time, float surge, float alpha, boolean compact) {
        if (alpha <= 0 || !QiAuraMaterial.ready()) return 0;
        int quality = QiVfxQuality.level();
        int rows = compact ? (quality == 0 ? 6 : 8) : (quality == 0 ? 12 : 18);
        int cols = compact ? (quality == 0 ? 8 : 12) : (quality == 0 ? 16 : 24);
        int layers = look.art()==5||look.art()==7 ? 1 : quality == 0 ? 2 : 3;
        if (!QiVfxQuality.takeCoating(rows * cols * layers)) return 0;
        return render(pose, buffers, look, time, surge, alpha, Vec3.ZERO, rows, cols, layers, quality,1,1,null);
    }

    private static int render(PoseStack pose, MultiBufferSource buffers, QiElementLook look, double time,
                              float surge, float alpha, Vec3 drag, int rows, int cols, int layers, int quality,
                              float bodyRadius,float bodyHeight,QiBodyFlamePhysics.Snapshot deformation) {
        for (int layer = 0; layer < layers; layer++) {
            // The low setting retains the core and its soft halo.
            int kind = quality == 0 && layer == 1 ? 2 : layer;
            VertexConsumer out = buffers.getBuffer(kind >= 2 ? QiAuraMaterial.HALO : QiAuraMaterial.ENERGY);
            double radius = kind == 1 ? .76 : kind == 2 ? 1.13 : kind == 3 ? 1.30 : 1;
            double height = kind == 1 ? .90 : kind == 2 ? 1.04 : kind == 3 ? 1.08 : 1;
            double phase = time * look.speed() + (kind == 1 ? 9 : 0);
            Vec3[][] points = new Vec3[rows + 1][cols];
            for (int r = 0; r <= rows; r++) {
                for (int c = 0; c < cols; c++) {
                    Vec3 p = QiEnergyShape.point(c * TAU / cols, r / (double) rows, phase, surge, drag,look.art());
                    Vec3 scaled=new Vec3(p.x*radius*bodyRadius,p.y*height*bodyHeight,p.z*radius*bodyRadius);
                    points[r][c]=deformation==null?scaled:deformation.deform(scaled,r/(double)rows);
                }
            }
            Vec3[][] normals = new Vec3[rows + 1][cols];
            for (int r = 0; r <= rows; r++) {
                for (int c = 0; c < cols; c++) {
                    Vec3 along = points[Math.min(rows, r + 1)][c].subtract(points[Math.max(0, r - 1)][c]);
                    Vec3 around = points[r][(c + 1) % cols].subtract(points[r][Math.floorMod(c - 1, cols)]);
                    Vec3 normal = along.cross(around).normalize();
                    normals[r][c] = normal.lengthSqr() < .1 ? new Vec3(0, r == 0 ? -1 : 1, 0) : normal;
                }
            }
            float opacity = alpha * QiVfxQuality.opacity();
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    vertex(pose, out, points, normals, r, c, rows, cols, kind, look, time, opacity);
                    vertex(pose, out, points, normals, r + 1, c, rows, cols, kind, look, time, opacity);
                    vertex(pose, out, points, normals, r + 1, c + 1, rows, cols, kind, look, time, opacity);
                    vertex(pose, out, points, normals, r, c + 1, rows, cols, kind, look, time, opacity);
                }
            }
        }
        draws++;
        if(look.art()==5)reincarnationDraws++;
        return rows * cols * layers;
    }

    private static void vertex(PoseStack pose, VertexConsumer out, Vec3[][] points, Vec3[][] normals,
                               int row, int column, int rows, int cols, int layer, QiElementLook look,
                               double time, float alpha) {
        Vec3 p = points[row][column % cols], n = normals[row][column % cols];
        int color=look.color();
        out.addVertex(pose.last(), (float) p.x, (float) p.y, (float) p.z)
                .setColor((color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F,
                        (color & 255) / 255F, alpha)
                .setUv(layer, row / (float) rows)
                .setOverlay((int) ((time % 1500) * 20) | (look.shaderCode(0) << 16))
                .setLight((int) (column * TAU / cols * 1000))
                .setNormal(pose.last(), (float) n.x, (float) n.y, (float) n.z);
    }
}
