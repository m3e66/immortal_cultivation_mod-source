package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.client.NpcClientData;

public interface NpcDataScreen {
    int npcEntityId();

    void updateNpcData(NpcClientData data);
}
