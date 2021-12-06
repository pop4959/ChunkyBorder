package org.popcraft.chunkyborder;

import net.pl3x.map.api.Pl3xMap;
import net.pl3x.map.api.Pl3xMapProvider;
import org.bukkit.plugin.PluginManager;
import org.dynmap.DynmapAPI;
import org.popcraft.chunky.integration.BlueMapIntegration;
import org.popcraft.chunky.integration.DynmapIntegration;
import org.popcraft.chunky.integration.Pl3xMapIntegration;
import org.popcraft.chunkyborder.platform.MapIntegrationLoader;

import java.util.Optional;

public class BukkitMapIntegrationLoader implements MapIntegrationLoader {
    private static final String BLUEMAP = "BlueMap";
    private static final String DYNMAP = "dynmap";
    private static final String PL3XMAP = "Pl3xMap";
    private final ChunkyBorderBukkit chunkyBorderBukkit;

    public BukkitMapIntegrationLoader(ChunkyBorderBukkit chunkyBorderBukkit) {
        this.chunkyBorderBukkit = chunkyBorderBukkit;
    }

    @Override
    public Optional<BlueMapIntegration> loadBlueMap() {
        final PluginManager pluginManager = chunkyBorderBukkit.getServer().getPluginManager();
        if (!pluginManager.isPluginEnabled(BLUEMAP)) {
            return Optional.empty();
        }
        return Optional.ofNullable(chunkyBorderBukkit.getServer().getPluginManager().getPlugin(BLUEMAP))
                .map(blueMap -> new BlueMapIntegration());
    }

    @Override
    public Optional<DynmapIntegration> loadDynmap() {
        final PluginManager pluginManager = chunkyBorderBukkit.getServer().getPluginManager();
        if (!pluginManager.isPluginEnabled(DYNMAP)) {
            return Optional.empty();
        }
        return Optional.ofNullable(chunkyBorderBukkit.getServer().getPluginManager().getPlugin(DYNMAP))
                .map(dynmap -> {
                    DynmapAPI dynmapAPI = (DynmapAPI) dynmap;
                    return dynmapAPI.markerAPIInitialized() ? new DynmapIntegration(dynmapAPI) : null;
                });
    }

    @Override
    public Optional<Pl3xMapIntegration> loadPl3xMap() {
        final PluginManager pluginManager = chunkyBorderBukkit.getServer().getPluginManager();
        if (!pluginManager.isPluginEnabled(PL3XMAP)) {
            return Optional.empty();
        }
        return Optional.ofNullable(pluginManager.getPlugin(PL3XMAP))
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
