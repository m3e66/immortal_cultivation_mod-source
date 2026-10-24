package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.client.model.SilverMistWolfModel;
import com.example.immortal_cultivation_mod.entity.SilverMistWolfEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public final class SilverMistWolfRenderer extends BeastGeoRenderer<SilverMistWolfEntity> {
    public SilverMistWolfRenderer(EntityRendererProvider.Context context){super(context,new SilverMistWolfModel());shadowRadius=.65F;}
    @Override protected float getDeathMaxRotation(SilverMistWolfEntity wolf){return 0;}
}
