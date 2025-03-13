package com.duck.elliemcquinn.nethermaps;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod("nethermaps")
public class Main {

    public Main(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
    }
}
