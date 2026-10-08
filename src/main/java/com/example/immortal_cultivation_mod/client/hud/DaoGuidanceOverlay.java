package com.example.immortal_cultivation_mod.client.hud;

import com.example.immortal_cultivation_mod.client.DaoGuidanceClient;
import com.example.immortal_cultivation_mod.client.ModKeyMappings;
import com.example.immortal_cultivation_mod.guidance.DaoGuidanceContent;
import com.example.immortal_cultivation_mod.network.DaoGuidancePayloads;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class DaoGuidanceOverlay implements LayeredDraw.Layer {
   public static final ResourceLocation HUD_ID = ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "dao_guidance");
   public static final DaoGuidanceOverlay INSTANCE = new DaoGuidanceOverlay();
   private static final int WIDTH = 224;

   private DaoGuidanceOverlay() {
   }

   public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.player != null && !minecraft.options.hideGui) {
         renderTracked(graphics, minecraft);
         renderNotice(graphics, minecraft);
      }
   }

   private static void renderTracked(GuiGraphics graphics, Minecraft minecraft) {
      String tracked = DaoGuidanceClient.trackedChapter();
      DaoGuidanceContent.Chapter chapter = DaoGuidanceContent.chapter(tracked);
      if (chapter != null) {
         DaoGuidancePayloads.ChapterState state = DaoGuidanceClient.state(tracked);
         if (state.discovered() && !state.complete() && state.objectiveIndex() < chapter.objectives().size()) {
            DaoGuidanceContent.Objective objective = (DaoGuidanceContent.Objective)chapter.objectives().get(state.objectiveIndex());
            int x = 10;
            int y = 34;
            boolean caveObjective = "find_enlightenment_pill".equals(objective.action());
            int height = objective.target() <= 1 && !caveObjective ? 49 : 61;
            graphics.fill(x, y, x + 224, y + height, -1609032165);
            graphics.renderOutline(x, y, 224, height, -1147438013);
            Component title = Component.translatable("hud.immortal_cultivation_mod.dao_guidance.title", new Object[]{Component.translatable(DaoGuidanceContent.chapterKey(tracked, "name"))});
            graphics.drawString(minecraft.font, title, x + 8, y + 7, -1517919, true);
            Component objectiveName = Component.translatable(DaoGuidanceContent.objectiveKey(tracked, objective.id(), "name"));
            graphics.drawString(minecraft.font, objectiveName, x + 8, y + 22, -922400, true);
            if (objective.target() > 1) {
               int progress = Math.min(objective.target(), state.progress());
               int barX = x + 8;
               int barY = y + 36;
               int barWidth = 208;
               graphics.fill(barX, barY, barX + barWidth, barY + 7, -1439548876);
               graphics.fill(barX, barY, barX + barWidth * progress / objective.target(), barY + 7, -580483472);
               Component amount = Component.literal(progress + " / " + objective.target());
               graphics.drawString(minecraft.font, amount, x + 224 - 8 - minecraft.font.width(amount), y + 47, -2567483, false);
            } else if (caveObjective) {
               graphics.drawString(minecraft.font, caveGuidance(minecraft), x + 8, y + 36, -4467003, false);
            }

            Component key = ((KeyMapping)ModKeyMappings.OPEN_DAO_GUIDANCE.get()).getTranslatedKeyMessage();
            Component open = Component.translatable("hud.immortal_cultivation_mod.dao_guidance.open", new Object[]{key});
            graphics.drawString(minecraft.font, open, x + 8, y + height - 11, -5593961, false);
         }
      }
   }

   private static Component caveGuidance(Minecraft minecraft) {
      String key = "hud.immortal_cultivation_mod.dao_guidance.cave_";
      if (!DaoGuidanceClient.hasCaveTarget()) {
         return Component.translatable(key + "searching");
      } else {
         return minecraft.level != null && DaoGuidanceClient.caveTargetDimension().equals(minecraft.level.dimension().location().toString()) ? Component.translatable(key + "target", new Object[]{DaoGuidanceClient.caveTargetDirection(minecraft.player.getX(), minecraft.player.getZ()), DaoGuidanceClient.caveTargetDistance(minecraft.player.getX(), minecraft.player.getZ())}) : Component.translatable(key + "overworld");
      }
   }

   private static void renderNotice(GuiGraphics graphics, Minecraft minecraft) {
      int ticks = DaoGuidanceClient.noticeTicks();
      if (ticks > 0) {
         String kind = DaoGuidanceClient.noticeKind();
         float fade = Mth.clamp((float)ticks / 20.0F, 0.0F, 1.0F);
         int alpha = Mth.clamp(Math.round(fade * 220.0F), 0, 220);
         if ("complete".equals(kind)) {
            String chapterId = DaoGuidanceClient.noticeId();
            int centerX = graphics.guiWidth() / 2;
            int y = Math.max(48, graphics.guiHeight() / 4);
            graphics.fill(centerX - 154, y - 9, centerX + 154, y + 44, alpha << 24 | 1515545);
            Component heading = Component.translatable("hud.immortal_cultivation_mod.dao_guidance.complete");
            graphics.drawCenteredString(minecraft.font, heading, centerX, y, alpha << 24 | 16046469);
            Component learned = Component.translatable("hud.immortal_cultivation_mod.dao_guidance.learned", new Object[]{Component.translatable(DaoGuidanceContent.chapterKey(chapterId, "name"))});
            graphics.drawCenteredString(minecraft.font, learned, centerX, y + 17, alpha << 24 | 15854816);
            String next = DaoGuidanceClient.noticeNextId();
            if (!next.isBlank()) {
               Component nextText = Component.translatable("hud.immortal_cultivation_mod.dao_guidance.next", new Object[]{Component.translatable(DaoGuidanceContent.chapterKey(next, "name"))});
               graphics.drawCenteredString(minecraft.font, nextText, centerX, y + 30, alpha << 24 | 13025458);
            }

         } else {
            String id = DaoGuidanceClient.noticeId();
            String var10000 = "hint".equals(kind) ? "hint." : "unlock.";
            String messageKey = "guidance.immortal_cultivation_mod." + var10000 + id;
            Component message = "hint".equals(kind) && "qi_insufficient".equals(id) ? Component.translatable(messageKey, new Object[]{((KeyMapping)ModKeyMappings.OPEN_DAO_GUIDANCE.get()).getTranslatedKeyMessage()}) : Component.translatable(messageKey);
            int width = Math.min(300, minecraft.font.width(message) + 24);
            int x = (graphics.guiWidth() - width) / 2;
            int y = graphics.guiHeight() - 78;
            graphics.fill(x, y, x + width, y + 27, alpha << 24 | 1515545);
            graphics.renderOutline(x, y, width, 27, alpha << 24 | 10189891);
            graphics.drawCenteredString(minecraft.font, message, graphics.guiWidth() / 2, y + 9, alpha << 24 | 15854816);
         }
      }
   }
}
