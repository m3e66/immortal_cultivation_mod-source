package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.entity.AsteroidEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.slf4j.Logger;

public final class AsteroidRenderer extends EntityRenderer<AsteroidEntity> {
   private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "textures/entity/asteroid.png");
   private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "models/entity/asteroid.obj");
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int MINIMUM_BLOCK_LIGHT = 7;
   private final Mesh mesh;
   private final Map<AsteroidEntity, CachedGeometry> geometryCache = new WeakHashMap();

   public AsteroidRenderer(EntityRendererProvider.Context context) {
      super(context);
      this.shadowRadius = 0.0F;
      this.mesh = loadObj();
   }

   public void render(AsteroidEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      if (this.mesh == null) {
         super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
      } else {
         CachedGeometry geometry = this.geometry(entity);
         poseStack.pushPose();
         float diameter = entity.diameterBlocks();
         poseStack.translate((double)0.0F, (double)diameter * (double)0.5F, (double)0.0F);
         poseStack.mulPose(Axis.YP.rotationDegrees(entity.spinYDegrees(partialTick)));
         poseStack.mulPose(Axis.XP.rotationDegrees(entity.spinXDegrees(partialTick)));
         poseStack.mulPose(Axis.ZP.rotationDegrees(entity.spinZDegrees(partialTick)));
         poseStack.scale(diameter * 0.5F, diameter * 0.5F, diameter * 0.5F);
         PoseStack.Pose pose = poseStack.last();
         Matrix4f poseMatrix = pose.pose();
         int modelLight = LightTexture.pack(Math.max(7, LightTexture.block(packedLight)), LightTexture.sky(packedLight));
         VertexConsumer vertices = buffer.getBuffer(RenderType.entitySolid(TEXTURE));

         for(Face face : this.mesh.faces()) {
            for(int emittedCorner = 0; emittedCorner < 4; ++emittedCorner) {
               int corner = Math.min(emittedCorner, 2);
               ObjVertex reference = (ObjVertex)this.mesh.vertices().get(face.indices()[corner]);
               int positionIndex = reference.positionIndex();
               Vector3f position = geometry.positions()[positionIndex];
               Vector3f normal = geometry.normals()[this.mesh.weldIndices()[positionIndex]];
               vertices.addVertex(poseMatrix, position.x, position.y, position.z).setColor(255, 255, 255, 255).setUv(reference.uv().x, 1.0F - reference.uv().y).setOverlay(OverlayTexture.NO_OVERLAY).setLight(modelLight).setNormal(pose, normal.x, normal.y, normal.z);
            }
         }

         poseStack.popPose();
         super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
      }
   }

   private CachedGeometry geometry(AsteroidEntity entity) {
      List<AsteroidEntity.Crater> craters = entity.craters();
      CachedGeometry cached = (CachedGeometry)this.geometryCache.get(entity);
      if (cached != null && cached.craterState() == craters) {
         return cached;
      } else {
         float[] innerEdges = new float[craters.size()];
         float[] outerEdges = new float[craters.size()];

         for(int index = 0; index < craters.size(); ++index) {
            float visualRadius = Math.max(0.24F, ((AsteroidEntity.Crater)craters.get(index)).angularRadius());
            innerEdges[index] = (float)Math.cos((double)visualRadius);
            outerEdges[index] = (float)Math.cos((double)(visualRadius * 1.18F));
         }

         Vector3f[] positions = new Vector3f[this.mesh.positions().size()];

         for(int index = 0; index < positions.length; ++index) {
            positions[index] = deform((Vector3f)this.mesh.positions().get(index), craters, innerEdges, outerEdges);
         }

         Vector3f[] normals = new Vector3f[this.mesh.weldCount()];

         for(int index = 0; index < normals.length; ++index) {
            normals[index] = new Vector3f();
         }

         for(Face face : this.mesh.faces()) {
            int firstIndex = ((ObjVertex)this.mesh.vertices().get(face.indices()[0])).positionIndex();
            int secondIndex = ((ObjVertex)this.mesh.vertices().get(face.indices()[1])).positionIndex();
            int thirdIndex = ((ObjVertex)this.mesh.vertices().get(face.indices()[2])).positionIndex();
            Vector3f faceNormal = (new Vector3f(positions[secondIndex])).sub(positions[firstIndex]).cross((new Vector3f(positions[thirdIndex])).sub(positions[firstIndex]));
            if (!(faceNormal.lengthSquared() < 1.0E-10F)) {
               normals[this.mesh.weldIndices()[firstIndex]].add(faceNormal);
               normals[this.mesh.weldIndices()[secondIndex]].add(faceNormal);
               normals[this.mesh.weldIndices()[thirdIndex]].add(faceNormal);
            }
         }

         for(Vector3f normal : normals) {
            if (normal.lengthSquared() > 1.0E-10F) {
               normal.normalize();
            } else {
               normal.set(0.0F, 1.0F, 0.0F);
            }
         }

         CachedGeometry result = new CachedGeometry(craters, positions, normals);
         this.geometryCache.put(entity, result);
         return result;
      }
   }

   private static Vector3f deform(Vector3f original, List<AsteroidEntity.Crater> craters, float[] innerEdges, float[] outerEdges) {
      float radius = original.length();
      float directionX = original.x / radius;
      float directionY = original.y / radius;
      float directionZ = original.z / radius;
      float depression = 0.0F;
      float rim = 0.0F;

      for(int index = 0; index < craters.size(); ++index) {
         AsteroidEntity.Crater crater = (AsteroidEntity.Crater)craters.get(index);
         float alignment = directionX * crater.x() + directionY * crater.y() + directionZ * crater.z();
         float innerEdge = innerEdges[index];
         float outerEdge = outerEdges[index];
         if (!(alignment <= outerEdge)) {
            if (alignment >= innerEdge) {
               float local = (alignment - innerEdge) / (1.0F - innerEdge);
               float smooth = local * local * (3.0F - 2.0F * local);
               depression += crater.depth() * smooth;
            } else {
               float edge = (alignment - outerEdge) / (innerEdge - outerEdge);
               rim = Math.max(rim, (float)Math.sin((double)edge * Math.PI) * crater.depth() * 0.14F);
            }
         }
      }

      float changedRadius = radius * Math.max(0.52F, 1.0F - depression + rim);
      return new Vector3f(directionX * changedRadius, directionY * changedRadius, directionZ * changedRadius);
   }

   private static Mesh loadObj() {
      List<Vector3f> positions = new ArrayList();
      List<Vector2f> uvs = new ArrayList();
      List<ObjVertex> vertices = new ArrayList();
      List<Face> faces = new ArrayList();

      try {
         BufferedReader reader = new BufferedReader(new InputStreamReader(Minecraft.getInstance().getResourceManager().getResourceOrThrow(MODEL).open(), StandardCharsets.UTF_8));

         String line;
         try {
            while((line = reader.readLine()) != null) {
               String[] parts = line.trim().split("\\s+");
               if (parts.length >= 2) {
                  if (parts[0].equals("v") && parts.length >= 4) {
                     positions.add(new Vector3f(Float.parseFloat(parts[1]), Float.parseFloat(parts[2]), Float.parseFloat(parts[3])));
                  } else if (parts[0].equals("vt") && parts.length >= 3) {
                     uvs.add(new Vector2f(Float.parseFloat(parts[1]), Float.parseFloat(parts[2])));
                  } else if (parts[0].equals("f") && parts.length >= 4) {
                     int[] faceVertices = new int[parts.length - 1];

                     for(int index = 1; index < parts.length; ++index) {
                        String[] references = parts[index].split("/");
                        int positionIndex = objIndex(references[0], positions.size());
                        Vector2f uv = references.length > 1 && !references[1].isEmpty() ? (Vector2f)uvs.get(objIndex(references[1], uvs.size())) : new Vector2f();
                        faceVertices[index - 1] = vertices.size();
                        vertices.add(new ObjVertex(positionIndex, new Vector2f(uv)));
                     }

                     for(int index = 1; index < faceVertices.length - 1; ++index) {
                        faces.add(new Face(new int[]{faceVertices[0], faceVertices[index], faceVertices[index + 1]}));
                     }
                  }
               }
            }
         } catch (Throwable var13) {
            try {
               reader.close();
            } catch (Throwable var12) {
               var13.addSuppressed(var12);
            }

            throw var13;
         }

         reader.close();
      } catch (RuntimeException | IOException exception) {
         LOGGER.error("Could not load asteroid OBJ", exception);
         return null;
      }

      float maximumRadius = 0.0F;

      for(Vector3f position : positions) {
         maximumRadius = Math.max(maximumRadius, position.length());
      }

      if (!(maximumRadius < 1.0E-6F) && !faces.isEmpty()) {
         float normalization = 1.0F / maximumRadius;

         for(Vector3f position : positions) {
            position.mul(normalization);
         }

         Map<PositionKey, Integer> weldGroups = new HashMap();
         int[] weldIndices = new int[positions.size()];

         for(int index = 0; index < positions.size(); ++index) {
            Vector3f position = (Vector3f)positions.get(index);
            PositionKey key = new PositionKey(Math.round(position.x * 100000.0F), Math.round(position.y * 100000.0F), Math.round(position.z * 100000.0F));
            weldIndices[index] = (Integer)weldGroups.computeIfAbsent(key, (ignored) -> weldGroups.size());
         }

         return new Mesh(List.copyOf(positions), List.copyOf(vertices), List.copyOf(faces), weldIndices, weldGroups.size());
      } else {
         LOGGER.error("Asteroid OBJ did not contain a usable mesh");
         return null;
      }
   }

   private static int objIndex(String value, int size) {
      int parsed = Integer.parseInt(value);
      return parsed > 0 ? parsed - 1 : size + parsed;
   }

   public ResourceLocation getTextureLocation(AsteroidEntity entity) {
      return TEXTURE;
   }

   private static record PositionKey(int x, int y, int z) {
   }

   private static record ObjVertex(int positionIndex, Vector2f uv) {
   }

   private static record Face(int[] indices) {
   }

   private static record Mesh(List<Vector3f> positions, List<ObjVertex> vertices, List<Face> faces, int[] weldIndices, int weldCount) {
   }

   private static record CachedGeometry(List<AsteroidEntity.Crater> craterState, Vector3f[] positions, Vector3f[] normals) {
   }
}
