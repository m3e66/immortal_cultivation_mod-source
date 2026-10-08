package com.example.immortal_cultivation_mod.magicritual.block.custom;

import net.minecraft.util.StringRepresentable;

public enum ComboType implements StringRepresentable {
   FOG("fog"),
   INVERTED("inverted"),
   FORCEFIELD("forcefield");

   private final String name;

   private ComboType(String name) {
      this.name = name;
   }

   public String getSerializedName() {
      return this.name;
   }

   // $FF: synthetic method
   private static ComboType[] $values() {
      return new ComboType[]{FOG, INVERTED, FORCEFIELD};
   }
}
