package org.popcraft.chunkyborder;

import org.bukkit.plugin.PluginManager;
import org.dynmap.DynmapCommonAPI;
import org.popcraft.chunkyborder.integration.BlueMapIntegration;
import org.popcraft.chunkyborder.integration.DynmapIntegration;
import org.popcraft.chunkyborder.integration.MapIntegration;
import org.popcraft.chunkyborder.integration.SquaremapIntegration;
import org.popcraft.chunkyborder.platform.MapIntegrationLoader;
import xyz.jpenilla.squaremap.api.Squaremap;
import xyz.jpenilla.squaremap.api.SquaremapProvider;

import java.util.Optional;

public class BukkitMapIntegrationLoader implements MapIntegrationLoader {
    private static final String BLUEMAP = "BlueMap";
    private static final String DYNMAP = "dynmap";
    private static final String SQUAREMAP = "squaremap";
    private final ChunkyBorderBukkit chunkyBorderBukkit;

    public BukkitMapIntegrationLoader(final ChunkyBorderBukkit chunkyBorderBukkit) {
        this.chunkyBorderBukkit = chunkyBorderBukkit;
    }

    @Override
    public Optional<MapIntegration> loadBlueMap() {
        final PluginManager pluginManager = chunkyBorderBukkit.getServer().getPluginManager();
        if (!pluginManager.isPluginEnabled(BLUEMAP)) {
            return Optional.empty();
        }
        return Optional.ofNullable(chunkyBorderBukkit.getServer().getPluginManager().getPlugin(BLUEMAP))
                .map(blueMap -> new BlueMapIntegration());
    }

    @Override
    public Optional<MapIntegration> loadDynmap() {
        final PluginManager pluginManager = chunkyBorderBukkit.getServer().getPluginManager();
        if (!pluginManager.isPluginEnabled(DYNMAP)) {
            return Optional.empty();
        }
        return Optional.ofNullable(chunkyBorderBukkit.getServer().getPluginManager().getPlugin(DYNMAP))
                .map(dynmap -> {
                    final DynmapCommonAPI dynmapAPI = (DynmapCommonAPI) dynmap;
                    return dynmapAPI.markerAPIInitialized() ? new DynmapIntegration(dynmapAPI) : null;
                });
    }

    @Override
    public Optional<MapIntegration> loadSquaremap() {
        final PluginManager pluginManager = chunkyBorderBukkit.getServer().getPluginManager();
        if (!pluginManager.isPluginEnabled(SQUAREMAP)) {
            return Optional.empty();
        }
        return Optional.ofNullable(pluginManager.getPlugin(SQUAREMAP))
                .map(squaremap -> {
                    try {
                        final Squaremap squaremapAPI = SquaremapProvider.get();
                        return new SquaremapIntegration(squaremapAPI);
                    } catch (IllegalStateException ignored) {
                        return null;
                    }
                });
    }
}
