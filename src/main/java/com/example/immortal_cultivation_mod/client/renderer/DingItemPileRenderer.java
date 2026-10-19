package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.block.DingBlockEntity;
import com.example.immortal_cultivation_mod.block.DingBasinVolume;
import com.example.immortal_cultivation_mod.block.DingPileLayout;
import com.example.immortal_cultivation_mod.block.GeoRockBlock;
import com.example.immortal_cultivation_mod.item.GeoRockBlockItem;
import com.example.immortal_cultivation_mod.item.GeoRockBlockItemModel;
import com.example.immortal_cultivation_mod.item.GeoRockBlockItemRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector4f;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;

/** Static, native-sized world models packed into a stable pile instead of inventory rows. */
final class DingItemPileRenderer {
    private final ItemRenderer itemRenderer;
    private final GeoRockBlockItemModel rockModel = new GeoRockBlockItemModel();
    private final Map<BakedModel, AABB> vanillaBounds = new IdentityHashMap<>();
    private final Map<BakedGeoModel, AABB> rockBounds = new IdentityHashMap<>();
    private final Map<DingBlockEntity, Pile> piles = new WeakHashMap<>();
    private Object resourceGeneration;

    DingItemPileRenderer(ItemRenderer itemRenderer) {
        this.itemRenderer = itemRenderer;
    }

    private record Piece(ItemStack stack, Vec3 offset, float yaw, float pitch, float roll, int seed, boolean clipped) {
        void orient(PoseStack pose) {
            pose.mulPose(Axis.YP.rotationDegrees(yaw));
            pose.mulPose(Axis.XP.rotationDegrees(pitch));
            pose.mulPose(Axis.ZP.rotationDegrees(roll));
        }
    }

    private record Pile(List<ItemStack> contents, int scale, List<Piece> pieces, AABB bounds, DingBasinVolume volume) { }

    void render(DingBlockEntity ding, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        Pile pile = pile(ding);
        pose.pushPose();
        pose.translate(.5, 0, .5);
        Matrix4f toBasin = new Matrix4f(pose.last().pose()).invert();
        for (Piece piece : pile.pieces()) {
            pose.pushPose();
            pose.translate(piece.offset().x, piece.offset().y, piece.offset().z);
            piece.orient(pose);
            // GROUND applies the model's authored world-item transform. No fitting or ding scale.
            if (piece.clipped()) {
                try (DingItemClippedBuffer cropped = new DingItemClippedBuffer(buffer, toBasin, pile.volume())) {
                    itemRenderer.renderStatic(piece.stack(), ItemDisplayContext.GROUND, light, overlay, pose, cropped,
                            ding.getLevel(), piece.seed());
                }
            } else {
                itemRenderer.renderStatic(piece.stack(), ItemDisplayContext.GROUND, light, overlay, pose, buffer,
                        ding.getLevel(), piece.seed());
            }
            pose.popPose();
        }
        pose.popPose();
    }

    AABB bounds(DingBlockEntity ding) {
        return pile(ding).bounds().move(ding.getBlockPos().getX() + .5, ding.getBlockPos().getY(),
                ding.getBlockPos().getZ() + .5);
    }

    private Pile pile(DingBlockEntity ding) {
        Object generation = Minecraft.getInstance().getModelManager().getMissingModel();
        if (generation != resourceGeneration) {
            piles.clear();
            vanillaBounds.clear();
            rockBounds.clear();
            resourceGeneration = generation;
        }
        int scale = ding.getBlockState().getValue(GeoRockBlock.SCALE);
        Pile existing = piles.get(ding);
        if (existing != null && existing.scale() == scale && unchanged(ding, existing.contents())) return existing;
        DingPileLayout layout = DingPileLayout.forModel(ding.modelName(), scale);
        List<ItemStack> contents = new ArrayList<>();
        List<Piece> pieces = new ArrayList<>();
        for (int slot = 0; slot < DingBlockEntity.SLOT_COUNT; slot++) {
            ItemStack stack = ding.getDisplayItem(slot);
            contents.add(stack.copy());
            // The recipe slip is a control input, not material inside the basin.
            if (slot == DingBlockEntity.YUJIAN_SLOT || stack.isEmpty()) continue;
            int seed = (int) (ding.getBlockPos().asLong() ^ slot * 7919L);
            BakedModel model = itemRenderer.getModel(stack, ding.getLevel(), null, seed);
            boolean flat = !model.isGui3d() && !model.isCustomRenderer();
            AABB nativeBounds = nativeBounds(stack, model);
            for (int copy = 0; copy < DingPileLayout.visibleCopies(stack.getCount()); copy++) {
                RandomSource random = RandomSource.create(seed + copy * 104729L);
                Piece piece = new Piece(stack.copyWithCount(1), Vec3.ZERO, random.nextFloat() * 360,
                        flat ? -90 : 0, (random.nextFloat() - .5F) * 10, seed, false);
                PoseStack orientation = new PoseStack();
                piece.orient(orientation);
                AABB rotated = transformBounds(nativeBounds, orientation);
                Vec3 offset = layout.place(rotated, seed + copy * 104729L);
                pieces.add(new Piece(piece.stack(), offset, piece.yaw(), piece.pitch(), piece.roll(), seed,
                        (model.isCustomRenderer() && !(stack.getItem() instanceof GeoRockBlockItem))
                                || !layout.volume().contains(rotated.move(offset))));
            }
        }
        Pile result = new Pile(contents, scale, pieces, layout.bounds(), layout.volume());
        piles.put(ding, result);
        return result;
    }

    private static boolean unchanged(DingBlockEntity ding, List<ItemStack> contents) {
        for (int slot = 0; slot < contents.size(); slot++) {
            if (!ItemStack.matches(contents.get(slot), ding.getDisplayItem(slot))) return false;
        }
        return true;
    }

    private AABB nativeBounds(ItemStack stack, BakedModel model) {
        PoseStack pose = new PoseStack();
        model.applyTransform(ItemDisplayContext.GROUND, pose, false);
        pose.translate(-.5, -.5, -.5);
        AABB raw;
        if (stack.getItem() instanceof GeoRockBlockItem rock) {
            BakedGeoModel geo = rockModel.getBakedModel(rockModel.getModelResource(rock));
            raw = rockBounds.computeIfAbsent(geo, GeoRockBlockItemRenderer::modelBounds);
            // GeoItemRenderer's native pre-render translation follows vanilla's centering.
            pose.translate(.5, .51, .5);
        } else {
            raw = vanillaBounds.computeIfAbsent(model, DingItemPileRenderer::quadBounds);
        }
        return transformBounds(raw, pose);
    }

    private static AABB quadBounds(BakedModel model) {
        double[] bounds = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
        for (int side = -1; side < 6; side++) {
            for (var quad : model.getQuads(null, side < 0 ? null : Direction.from3DDataValue(side), RandomSource.create(42))) {
                int[] vertices = quad.getVertices();
                int stride = vertices.length / 4;
                for (int vertex = 0; vertex < 4; vertex++) {
                    for (int axis = 0; axis < 3; axis++) {
                        double value = Float.intBitsToFloat(vertices[vertex * stride + axis]);
                        bounds[axis] = Math.min(bounds[axis], value);
                        bounds[axis + 3] = Math.max(bounds[axis + 3], value);
                    }
                }
            }
        }
        return Double.isFinite(bounds[0]) ? new AABB(bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5])
                : new AABB(0, 0, 0, 1, 1, 1);
    }

    private static AABB transformBounds(AABB bounds, PoseStack pose) {
        AABB result = null;
        for (int corner = 0; corner < 8; corner++) {
            Vector4f point = pose.last().pose().transform(new Vector4f(
                    (float) ((corner & 1) == 0 ? bounds.minX : bounds.maxX),
                    (float) ((corner & 2) == 0 ? bounds.minY : bounds.maxY),
                    (float) ((corner & 4) == 0 ? bounds.minZ : bounds.maxZ), 1));
            AABB vertex = new AABB(point.x(), point.y(), point.z(), point.x(), point.y(), point.z());
            result = result == null ? vertex : result.minmax(vertex);
        }
        return result;
    }
}
