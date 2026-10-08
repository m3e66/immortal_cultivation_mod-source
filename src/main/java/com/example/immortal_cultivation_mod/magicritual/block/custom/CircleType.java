package com.example.immortal_cultivation_mod.magicritual.block.custom;

import net.minecraft.util.StringRepresentable;

public enum CircleType implements StringRepresentable {
   ATTACK("attack"),
   DEFENSE("defense"),
   REGENERATION("regeneration");

   private final String name;

   private CircleType(String name) {
      this.name = name;
   }

   public String getSerializedName() {
      return this.name;
   }

   // $FF: synthetic method
   private static CircleType[] $values() {
      return new CircleType[]{ATTACK, DEFENSE, REGENERATION};
   }
}
