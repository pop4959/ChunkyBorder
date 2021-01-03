package org.popcraft.chunkyborder;

import de.bluecolored.bluemap.api.BlueMapAPI;
import org.popcraft.chunky.integration.BlueMapIntegration;
import org.popcraft.chunky.integration.MapIntegration;

import java.util.List;
import java.util.Optional;

public class BlueMapWorkaround {
    public static void load(ChunkyBorder chunkyBorder, List<MapIntegration> mapIntegrations) {
        Optional.ofNullable(chunkyBorder.getServer().getPluginManager().getPlugin("BlueMap"))
                .ifPresent(blueMap -> {
                    BlueMapIntegration blueMapIntegration = new BlueMapIntegration(chunkyBorder.getChunky());
                    BlueMapAPI.registerListener(blueMapIntegration);
                    mapIntegrations.add(blueMapIntegration);
                });
    }
}
