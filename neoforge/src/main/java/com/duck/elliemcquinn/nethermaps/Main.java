package com.duck.elliemcquinn.nethermaps;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod("nethermaps")
public class Main {

    // todo: Unsure of which sides the mod needs to be loaded on, update config type once known.
    public Main(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        // todo: Kind of lost interest for the moment, see MapItemSavedData.tickCarriedBy for where to inject, should remove the dimension check and then somehow update the players location on map if the scaling is different.
    }
}
