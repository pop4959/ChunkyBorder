package org.popcraft.chunkyborder;

import net.pl3x.map.api.Pl3xMap;
import net.pl3x.map.api.Pl3xMapProvider;
import org.dynmap.DynmapAPI;
import org.popcraft.chunky.integration.BlueMapIntegration;
import org.popcraft.chunky.integration.DynmapIntegration;
import org.popcraft.chunky.integration.Pl3xMapIntegration;
import org.popcraft.chunkyborder.platform.MapIntegrationLoader;

import java.util.Optional;

public class BukkitMapIntegrationLoader implements MapIntegrationLoader {
    private final ChunkyBorderBukkit chunkyBorderBukkit;

    public BukkitMapIntegrationLoader(ChunkyBorderBukkit chunkyBorderBukkit) {
        this.chunkyBorderBukkit = chunkyBorderBukkit;
    }

    @Override
    public Optional<BlueMapIntegration> loadBlueMap() {
        return Optional.ofNullable(chunkyBorderBukkit.getServer().getPluginManager().getPlugin("BlueMap"))
                .map(blueMap -> new BlueMapIntegration());
    }

    @Override
    public Optional<DynmapIntegration> loadDynmap() {
        return Optional.ofNullable(chunkyBorderBukkit.getServer().getPluginManager().getPlugin("dynmap"))
                .map(dynmap -> {
                    DynmapAPI dynmapAPI = (DynmapAPI) dynmap;
                    return dynmapAPI.markerAPIInitialized() ? new DynmapIntegration(dynmapAPI) : null;
                });
    }

    @Override
    public Optional<Pl3xMapIntegration> loadPl3xMap() {
        return Optional.ofNullable(chunkyBorderBukkit.getServer().getPluginManager().getPlugin("Pl3xMap"))
                .map(pl3xMap -> {
                    try {
                        Pl3xMap pl3xMapAPI = Pl3xMapProvider.get();
                        return new Pl3xMapIntegration(pl3xMapAPI);
                    } catch (IllegalStateException ignored) {
                        return null;
                    }
                });
    }
}
