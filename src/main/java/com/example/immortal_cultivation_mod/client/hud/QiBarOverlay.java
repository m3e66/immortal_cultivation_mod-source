package com.example.immortal_cultivation_mod.client.hud;

import com.example.immortal_cultivation_mod.client.hud.HudLayout;
import com.example.immortal_cultivation_mod.config.HudElement;
import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.attribute.ModAttributes;
import com.example.immortal_cultivation_mod.client.ClientData;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.spell.UndeadControl;
import com.example.immortal_cultivation_mod.spell.ShieldDisplay;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class QiBarOverlay implements LayeredDraw.Layer {
   public static final ResourceLocation HUD_ID = ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "qi_bar");
   public static final QiBarOverlay INSTANCE = new QiBarOverlay();
   private static final ResourceLocation FILL = ResourceLocation.fromNamespaceAndPath("autoforge_bricks", "textures/gui/fill_white.png");

   private QiBarOverlay() {
   }

   public void render(GuiGraphics g, DeltaTracker delta) {
      Minecraft mc = Minecraft.getInstance();
      ModAttachments.CultivationData data = ClientData.cultivationData;
      if (mc.player != null && !mc.options.hideGui) {
         if (data != null) {
            this.renderPostureBar(g, mc);
            int maxQi = data.maxQi();
            if (maxQi > 0) {
               int barWidth = 81;
               int barHeight = 6;
               int x = g.guiWidth() / 2 + 10;
               int y = g.guiHeight() - 49;
               this.renderMethodBars(g, mc, data, x, y, barWidth, barHeight);

               this.renderZombieRank(g, mc, data.activeCultivationMethod(), x, y, barWidth, barHeight);
               this.renderQiBar(g, mc, data, x, y, barWidth, barHeight);
               this.renderPreparedSpell(g, mc);
               this.renderShieldBar(g, mc);
            }
         }
      }
   }

   private void renderMethodBars(GuiGraphics g, Minecraft mc, ModAttachments.CultivationData data,
                                 int x, int y, int barWidth, int barHeight) {
      int maxQi = data.maxQi();
      var position = HudLayout.place(g, HudElement.METHOD, x, y - 26, Math.max(barWidth, 130), 16);
      x = position.x(); y = position.y() + 26;
      if (CultivationMethods.isDariBatiJue(data.activeCultivationMethod())) {
         int solarY = y - 16;
         float solar = CultivationMethods.solarPower(data);
         int solarCap = CultivationMethods.solarPowerCap(data);
         if (mc.player.hasEffect(ModEffects.XUYANG)) {
            solarCap = Math.min(solarCap, 50);
         }

         RenderSystem.setShaderColor(0.25F, 0.12F, 0.01F, 0.88F);
         g.blit(FILL, x, solarY, 0.0F, 0.0F, barWidth, barHeight, 1, 1);
         int solarW = (int)((float)barWidth * Math.min(1.0F, solar / 100.0F));
         if (solarW > 0) {
            RenderSystem.setShaderColor(1.0F, 0.62F, 0.05F, 1.0F);
            g.blit(FILL, x, solarY, 0.0F, 0.0F, solarW, barHeight, 1, 1);
         }

         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         g.drawString(mc.font, Component.translatable("hud.immortal_cultivation_mod.solar_power", new Object[]{Math.round(solar), solarCap}), x + 1, solarY - 10, -15283, true);
      }

      if (CultivationMethods.isGangtingXiaokongLu(data.activeCultivationMethod())) {
         int momentumY = y - 16;
         float momentum = CultivationMethods.fengleiMomentum(data);
         RenderSystem.setShaderColor(0.025F, 0.12F, 0.16F, 0.9F);
         g.blit(FILL, x, momentumY, 0.0F, 0.0F, barWidth, barHeight, 1, 1);
         int momentumW = Math.round((float)barWidth * momentum / 100.0F);
         if (momentumW > 0) {
            float thunderBlend = momentum / 100.0F;
            RenderSystem.setShaderColor(0.22F + thunderBlend * 0.18F, 0.82F - thunderBlend * 0.2F, 0.58F + thunderBlend * 0.42F, 1.0F);
            g.blit(FILL, x, momentumY, 0.0F, 0.0F, momentumW, barHeight, 1, 1);
         }

         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         g.drawString(mc.font, Component.translatable("hud.immortal_cultivation_mod.fenglei_momentum", new Object[]{Math.round(momentum)}), x + 1, momentumY - 10, momentum >= 100.0F ? -4654849 : -8984374, true);
      }

      if (CultivationMethods.isChangqingJue(data.activeCultivationMethod())) {
         int lifeY = y - 16;
         float lifeForce = CultivationMethods.changqingLifeForce(data);
         RenderSystem.setShaderColor(0.025F, 0.12F, 0.035F, 0.9F);
         g.blit(FILL, x, lifeY, 0.0F, 0.0F, barWidth, barHeight, 1, 1);
         int lifeWidth = Math.round((float)barWidth * lifeForce / 100.0F);
         if (lifeWidth > 0) {
            float flourishing = lifeForce / 100.0F;
            RenderSystem.setShaderColor(0.14F + flourishing * 0.26F, 0.58F + flourishing * 0.34F, 0.13F + flourishing * 0.12F, 1.0F);
            g.blit(FILL, x, lifeY, 0.0F, 0.0F, lifeWidth, barHeight, 1, 1);
         }

         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         boolean withering = lifeForce > 0.0F && mc.player.getHealth() < mc.player.getMaxHealth() * 0.4F;
         Component lifeLabel = !withering && !(lifeForce >= 60.0F) ? Component.translatable("hud.immortal_cultivation_mod.life_force", new Object[]{Math.round(lifeForce)}) : Component.translatable("hud.immortal_cultivation_mod.life_force_state", new Object[]{Math.round(lifeForce), Component.translatable("hud.immortal_cultivation_mod" + (withering ? ".state_withering" : ".state_flourishing"))});
         g.drawString(mc.font, lifeLabel, x + 1, lifeY - 10, withering ? -19110 : (lifeForce >= 80.0F ? -10134 : -8787858), true);
      }

      if (CultivationMethods.isBloodDemon(data.activeCultivationMethod())) {
         int maxBlood = maxQi * 10;
         int bloodY = y - 16;
         RenderSystem.setShaderColor(0.18F, 0.02F, 0.02F, 0.85F);
         g.blit(FILL, x, bloodY, 0.0F, 0.0F, barWidth, barHeight, 1, 1);
         float bloodRatio = Math.min(1.0F, (float)data.blood() / (float)maxBlood);
         int bloodW = (int)((float)barWidth * bloodRatio);
         if (bloodW > 0) {
            RenderSystem.setShaderColor(0.9F, 0.02F, 0.04F, 1.0F);
            g.blit(FILL, x, bloodY, 0.0F, 0.0F, bloodW, barHeight, 1, 1);
         }

         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         g.drawString(mc.font, Component.translatable("hud.immortal_cultivation_mod.blood", new Object[]{data.blood(), maxBlood}), x + 1, bloodY - 10, -48060, true);
      }

   }

   private void renderQiBar(GuiGraphics g, Minecraft mc, ModAttachments.CultivationData data,
                           int x, int y, int width, int height) {
      Component label = Component.translatable("hud.immortal_cultivation_mod.qi", data.qi(), data.maxQi());
      var position = HudLayout.place(g, HudElement.QI, x, y - 10, Math.max(width, mc.font.width(label) + 1), height + 10);
      x = position.x(); y = position.y() + 10;
      try {
         RenderSystem.setShaderColor(0.2F, 0.2F, 0.2F, 0.8F);
         g.blit(FILL, x, y, 0.0F, 0.0F, width, height, 1, 1);
         float ratio = Mth.clamp((float)data.qi() / data.maxQi(), 0, 1);
         int fillWidth = (int)(width * ratio);
         if (fillWidth > 0) {
            float[] rgb = this.getQiColor(ratio);
            RenderSystem.setShaderColor(rgb[0], rgb[1], rgb[2], 1);
            g.blit(FILL, x, y, 0.0F, 0.0F, fillWidth, height, 1, 1);
         }
         RenderSystem.setShaderColor(1, 1, 1, 1);
         Component text = Component.translatable("hud.immortal_cultivation_mod.qi", data.qi(), data.maxQi());
         g.drawString(mc.font, text, x + 1, y - 10, this.getTextColor(ratio), true);
      } finally {
         RenderSystem.setShaderColor(1, 1, 1, 1);
      }
   }

   private void renderZombieRank(GuiGraphics g, Minecraft mc, String methodId, int x, int qiY, int barWidth, int barHeight) {
      if (ClientData.livingCorpse()) {
         boolean hasMethodBar = CultivationMethods.isDariBatiJue(methodId) || CultivationMethods.isGangtingXiaokongLu(methodId) || CultivationMethods.isChangqingJue(methodId) || CultivationMethods.isBloodDemon(methodId);
         int y = qiY - (hasMethodBar ? 32 : 16);
         var position = HudLayout.place(g, HudElement.ZOMBIE, x, y - 10, Math.max(barWidth, 110), barHeight + 10);
         x = position.x(); y = position.y() + 10;
         long grudge = Math.max(0L, Math.round(mc.player.getAttributeValue(ModAttributes.GRUDGE)));
         long currentThreshold = UndeadControl.currentRankThreshold(grudge);
         long nextThreshold = UndeadControl.nextRankThreshold(grudge);
         float progress = nextThreshold < 0L ? 1.0F : Mth.clamp((float)(grudge - currentThreshold) / (float)Math.max(1L, nextThreshold - currentThreshold), 0.0F, 1.0F);
         RenderSystem.setShaderColor(0.1F, 0.07F, 0.08F, 0.9F);
         g.blit(FILL, x, y, 0.0F, 0.0F, barWidth, barHeight, 1, 1);
         int fillWidth = Math.round((float)barWidth * progress);
         if (fillWidth > 0) {
            RenderSystem.setShaderColor(0.45F, 0.62F, 0.49F, 1.0F);
            g.blit(FILL, x, y, 0.0F, 0.0F, fillWidth, barHeight, 1, 1);
         }

         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         Component rank = Component.translatable("zombie_rank.immortal_cultivation_mod." + UndeadControl.rankIdForGrudge(grudge));
         g.drawString(mc.font, Component.translatable("hud.immortal_cultivation_mod.zombie_rank", new Object[]{rank}), x + 1, y - 10, -5647441, true);
      }
   }

   private void renderPostureBar(GuiGraphics g, Minecraft mc) {
      if (ClientData.combatActive() && !(ClientData.maxPosture() <= 0.0F)) {
         int barWidth = 81;
         int barHeight = 6;
         Component selectedSpell = this.preparedSpellText();
         int selectedWidth = selectedSpell.getString().isEmpty() ? barWidth : mc.font.width(selectedSpell);
         int selectedX = this.preparedSpellX(g, selectedWidth);
         int x = Mth.clamp(selectedX + (selectedWidth - barWidth) / 2, 8, Math.max(8, g.guiWidth() - barWidth - 8));
         int y = g.guiHeight() - 45;
         var position = HudLayout.place(g, HudElement.POSTURE, x, y - 10, Math.max(barWidth, 100), barHeight + 10);
         x = position.x(); y = position.y() + 10;
         float ratio = Math.max(0.0F, Math.min(1.0F, ClientData.posture() / ClientData.maxPosture()));
         int fillWidth = Math.max(0, Math.round((float)barWidth * ratio));
         RenderSystem.setShaderColor(0.12F, 0.1F, 0.08F, 0.88F);
         g.blit(FILL, x, y, 0.0F, 0.0F, barWidth, barHeight, 1, 1);
         if (fillWidth > 0) {
            if (ClientData.postureBroken()) {
               RenderSystem.setShaderColor(0.75F, 0.08F, 0.06F, 1.0F);
            } else {
               RenderSystem.setShaderColor(0.95F, 0.58F, 0.12F, 1.0F);
            }

            g.blit(FILL, x, y, 0.0F, 0.0F, fillWidth, barHeight, 1, 1);
         }

         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         Component text = ClientData.postureBroken() ? Component.translatable("hud.immortal_cultivation_mod.posture_broken") : Component.translatable("hud.immortal_cultivation_mod.posture", new Object[]{Math.round(ClientData.posture()), Math.round(ClientData.maxPosture())});
         g.drawString(mc.font, text, x, y - 10, ClientData.postureBroken() ? -43691 : -15269, true);
      }
   }


   private void renderPreparedSpell(GuiGraphics g, Minecraft mc) {
      Component text = this.preparedSpellText();
      if (!text.getString().isEmpty()) {
         int textWidth = mc.font.width(text);
         int x = this.preparedSpellX(g, textWidth);
         int y = g.guiHeight() - 27;
         var position = HudLayout.place(g, HudElement.PREPARED_SPELL, x - 5, y - 3, textWidth + 10, 13);
         x = position.x() + 5; y = position.y() + 3;
         g.fill(x - 5, y - 3, x + textWidth + 5, y + 10, -1728053248);
         g.drawString(mc.font, text, x, y, -5574913, true);
      }
   }

   private Component preparedSpellText() {
      Component spellName = ClientData.preparedSpellName();
      if (spellName.getString().isEmpty()) {
         return Component.empty();
      } else {
         Component text = Component.translatable("hud.immortal_cultivation_mod.prepared_spell", new Object[]{spellName});
         int cooldownTicks = ClientData.preparedSpellCooldownTicks();
         if (cooldownTicks > 0) {
            String seconds = cooldownTicks % 20 == 0 ? Integer.toString(cooldownTicks / 20) : String.format(Locale.ROOT, "%.1f", (float)cooldownTicks / 20.0F);
            text = text.copy().append(Component.literal("  " + seconds + "s").withStyle(ChatFormatting.RED));
         }

         return text;
      }
   }

   private int preparedSpellX(GuiGraphics g, int textWidth) {
      return Math.min(g.guiWidth() / 2 + 112, g.guiWidth() - textWidth - 8);
   }



   private void renderShieldBar(GuiGraphics g, Minecraft mc) {
      float shield = ClientData.shieldAmount();
      float shieldMax = ClientData.shieldMax();
      if (!(shield <= 0.0F) && !(shieldMax <= 0.0F)) {
         int barWidth = 8;
         int barHeight = 44;
         int x = g.guiWidth() / 2 + 95;
         int y = g.guiHeight() - 45;
         int boxWidth = Math.max(barWidth, mc.font.width(String.valueOf(Math.round(shield))));
         var position = HudLayout.place(g, HudElement.SHIELD, x - (boxWidth - barWidth) / 2, y - 10, boxWidth, barHeight + 10);
         x = position.x() + (boxWidth - barWidth) / 2; y = position.y() + 10;
         var layers=ClientData.shieldLayers();
         if(layers.isEmpty())layers=java.util.List.of(new ShieldDisplay.Layer(ShieldDisplay.Kind.OTHER,Math.min(shield,shieldMax),shieldMax));
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         g.fill(x-1,y-1,x+barWidth+1,y+barHeight+1,0xD0101620);
         for(var segment:ShieldDisplay.segments(layers,barHeight)) {
            int color=segment.layer().kind().color,top=y+barHeight-segment.bottom()-segment.height();
            g.fill(x,top,x+barWidth,top+segment.height(),0x55000000|color);
            g.fill(x,top+segment.height()-segment.filled(),x+barWidth,top+segment.height(),0xFF000000|color);
         }
         String text = String.valueOf(Math.round(shield));
         int textX = x + barWidth / 2 - mc.font.width(text) / 2;
         int textColor=layers.size()==1?0xFF000000|layers.getFirst().kind().color:0xFFF2F4F8;
         g.drawString(mc.font, text, textX, y - 10, textColor, true);
      }
   }

   private float[] getQiColor(float ratio) {
      if (ratio > 0.5F) {
         return new float[]{0.33F, 1.0F, 0.33F};
      } else {
         return ratio > 0.25F ? new float[]{1.0F, 1.0F, 0.33F} : new float[]{1.0F, 0.33F, 0.33F};
      }
   }

   private int getTextColor(float ratio) {
      if (ratio > 0.5F) {
         return -11141291;
      } else {
         return ratio > 0.25F ? -171 : -43691;
      }
   }
}
