package com.example.immortal_cultivation_mod.client.hud;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.attribute.ModAttributes;
import com.example.immortal_cultivation_mod.client.ClientData;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.spell.UndeadControl;
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
      if (mc.player != null && !mc.options.hideGui) {
         ModAttachments.CultivationData data = ClientData.cultivationData;
         if (data != null) {
            this.renderPostureBar(g, mc);
            this.renderHeavyAttackCharge(g, mc);
            CultivationLevels.LevelDef levelDef = CultivationLevels.getLevelDef(data.cultivationLevel());
            int maxQi = levelDef.maxQi() + data.maxQiBonus();
            if (maxQi > 0) {
               int barWidth = 81;
               int barHeight = 6;
               int x = g.guiWidth() / 2 + 10;
               int y = g.guiHeight() - 49;
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

               this.renderZombieRank(g, mc, data.activeCultivationMethod(), x, y, barWidth, barHeight);
               RenderSystem.setShaderColor(0.2F, 0.2F, 0.2F, 0.8F);
               g.blit(FILL, x, y, 0.0F, 0.0F, barWidth, barHeight, 1, 1);
               float ratio = Math.min(1.0F, (float)data.qi() / (float)maxQi);
               int fillW = (int)((float)barWidth * ratio);
               if (fillW > 0) {
                  float[] rgb = this.getQiColor(ratio);
                  RenderSystem.setShaderColor(rgb[0], rgb[1], rgb[2], 1.0F);
                  g.blit(FILL, x, y, 0.0F, 0.0F, fillW, barHeight, 1, 1);
               }

               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
               Component text = Component.translatable("hud.immortal_cultivation_mod.qi", new Object[]{data.qi(), maxQi});
               int textColor = this.getTextColor(ratio);
               g.drawString(mc.font, text, x + 1, y - 10, textColor, true);
               this.renderPreparedSpell(g, mc);
               this.renderPreparedCharge(g, mc);
               this.renderSwordCaseCharge(g, mc);
               this.renderShieldBar(g, mc);
            }
         }
      }
   }

   private void renderZombieRank(GuiGraphics g, Minecraft mc, String methodId, int x, int qiY, int barWidth, int barHeight) {
      if (ClientData.livingCorpse()) {
         boolean hasMethodBar = CultivationMethods.isDariBatiJue(methodId) || CultivationMethods.isGangtingXiaokongLu(methodId) || CultivationMethods.isChangqingJue(methodId) || CultivationMethods.isBloodDemon(methodId);
         int y = qiY - (hasMethodBar ? 32 : 16);
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

   private void renderHeavyAttackCharge(GuiGraphics g, Minecraft mc) {
      float progress = ClientData.heavyAttackChargeProgress();
      if (!(progress <= 0.0F)) {
         int barWidth = 102;
         int barHeight = 6;
         int x = g.guiWidth() / 2 - barWidth / 2;
         int y = g.guiHeight() - 67;
         int fill = Math.max(1, Math.round((float)barWidth * progress));
         Component label = Component.translatable("hud.immortal_cultivation_mod.heavy_attack", new Object[]{Math.round(progress * 100.0F)});
         g.fill(x - 5, y - 13, x + barWidth + 5, y + barHeight + 4, -1728053248);
         g.fill(x, y, x + barWidth, y + barHeight, -14410986);
         g.fill(x, y, x + fill, y + barHeight, -2599886);
         g.drawString(mc.font, label, x + barWidth / 2 - mc.font.width(label) / 2, y - 11, -14424, true);
      }
   }

   private void renderPreparedSpell(GuiGraphics g, Minecraft mc) {
      Component text = this.preparedSpellText();
      if (!text.getString().isEmpty()) {
         int textWidth = mc.font.width(text);
         int x = this.preparedSpellX(g, textWidth);
         int y = g.guiHeight() - 27;
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

   private void renderPreparedCharge(GuiGraphics g, Minecraft mc) {
      if (ClientData.preparedCharging()) {
         float progress = Math.max(0.0F, Math.min(1.0F, ClientData.preparedChargeScale() - 1.0F));
         int barWidth = 102;
         int barHeight = 6;
         int fill = Math.max(1, (int)((float)barWidth * progress));
         int x = g.guiWidth() / 2 - barWidth / 2;
         int barY = g.guiHeight() - 67;
         String percent = ClientData.preparedChargePercent() + "%";
         g.fill(x - 5, barY - 12, x + barWidth + 5, barY + barHeight + 4, -1728053248);
         g.fill(x, barY, x + barWidth, barY + barHeight, -14342875);
         g.fill(x, barY, x + fill, barY + barHeight, -12268289);
         g.drawString(mc.font, percent, x + barWidth / 2 - mc.font.width(percent) / 2, barY - 10, -5574913, true);
      }

   }

   private void renderSwordCaseCharge(GuiGraphics g, Minecraft mc) {
      float progress = ClientData.swordCaseChargeProgress();
      if (!(progress <= 0.0F)) {
         int barWidth = 120;
         int barHeight = 7;
         int x = g.guiWidth() / 2 - barWidth / 2;
         int y = g.guiHeight() - 82;
         int fill = Math.max(1, Math.round((float)barWidth * progress));
         Component label = Component.translatable("hud.immortal_cultivation_mod.sword_case_charge", new Object[]{Math.round(progress * 100.0F)});
         g.fill(x - 5, y - 13, x + barWidth + 5, y + barHeight + 4, -1728053248);
         g.fill(x, y, x + barWidth, y + barHeight, -14015208);
         g.fill(x, y, x + fill, y + barHeight, -1522597);
         g.drawString(mc.font, label, x + barWidth / 2 - mc.font.width(label) / 2, y - 11, -5984, true);
      }
   }

   private void renderShieldBar(GuiGraphics g, Minecraft mc) {
      float shield = ClientData.shieldAmount();
      float shieldMax = ClientData.shieldMax();
      if (!(shield <= 0.0F) && !(shieldMax <= 0.0F)) {
         int barWidth = 8;
         int barHeight = 44;
         int x = g.guiWidth() / 2 + 95;
         int y = g.guiHeight() - 45;
         float ratio = Math.min(1.0F, shield / shieldMax);
         int fillHeight = Math.max(1, (int)((float)barHeight * ratio));
         RenderSystem.setShaderColor(0.05F, 0.12F, 0.18F, 0.85F);
         g.blit(FILL, x, y, 0.0F, 0.0F, barWidth, barHeight, 1, 1);
         RenderSystem.setShaderColor(0.25F, 0.78F, 1.0F, 1.0F);
         g.blit(FILL, x, y + barHeight - fillHeight, 0.0F, 0.0F, barWidth, fillHeight, 1, 1);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         String text = String.valueOf(Math.round(shield));
         int textX = x + barWidth / 2 - mc.font.width(text) / 2;
         g.drawString(mc.font, text, textX, y - 10, -10035713, true);
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
