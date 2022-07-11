package org.popcraft.chunkyborder;

import net.fabricmc.api.ModInitializer;
import org.popcraft.chunkyborder.integration.DynmapCommonAPIProvider;

public class MainEntrypoint implements ModInitializer {
    @Override
    public void onInitialize() {
        try {
            Class.forName("org.dynmap.DynmapCommonAPI");
            new DynmapCommonAPIProvider();
        } catch (ClassNotFoundException ignored) {
            // Dynmap is not installed
        }
    }
}
