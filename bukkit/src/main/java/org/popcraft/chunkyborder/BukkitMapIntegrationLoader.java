package org.popcraft.chunkyborder;

import org.bukkit.plugin.PluginManager;
import org.dynmap.DynmapAPI;
import org.popcraft.chunky.integration.BlueMapIntegration;
import org.popcraft.chunky.integration.DynmapIntegration;
import org.popcraft.chunky.integration.SquaremapIntegration;
import org.popcraft.chunkyborder.platform.MapIntegrationLoader;
import xyz.jpenilla.squaremap.api.Squaremap;
import xyz.jpenilla.squaremap.api.SquaremapProvider;

import java.util.Optional;

public class BukkitMapIntegrationLoader implements MapIntegrationLoader {
    private static final String BLUEMAP = "BlueMap";
    private static final String DYNMAP = "dynmap";
    private static final String SQUAREMAP = "squaremap";
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
    public Optional<SquaremapIntegration> loadSquaremap() {
        final PluginManager pluginManager = chunkyBorderBukkit.getServer().getPluginManager();
        if (!pluginManager.isPluginEnabled(SQUAREMAP)) {
            return Optional.empty();
        }
        return Optional.ofNullable(pluginManager.getPlugin(SQUAREMAP))
                .map(squaremap -> {
                    try {
                        Squaremap squaremapAPI = SquaremapProvider.get();
                        return new SquaremapIntegration(squaremapAPI);
                    } catch (IllegalStateException ignored) {
                        return null;
                    }
                });
    }
}
