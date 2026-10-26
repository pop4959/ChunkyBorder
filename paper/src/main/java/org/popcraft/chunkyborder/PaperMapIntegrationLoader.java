package org.popcraft.chunkyborder;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.PluginManager;
import org.dynmap.DynmapCommonAPI;
import org.popcraft.chunky.platform.World;
import org.popcraft.chunkyborder.integration.BlueMapIntegration;
import org.popcraft.chunkyborder.integration.DynmapIntegration;
import org.popcraft.chunkyborder.integration.MapIntegration;
import org.popcraft.chunkyborder.integration.Pl3xMapIntegration;
import org.popcraft.chunkyborder.integration.SquaremapIntegration;
import org.popcraft.chunkyborder.platform.MapIntegrationLoader;
import xyz.jpenilla.squaremap.api.Squaremap;
import xyz.jpenilla.squaremap.api.SquaremapProvider;

import java.util.Optional;

public class PaperMapIntegrationLoader implements MapIntegrationLoader {
    private static final String BLUEMAP = "BlueMap";
    private static final String DYNMAP = "dynmap";
    private static final String PL3XMAP = "Pl3xMap";
    private static final String SQUAREMAP = "squaremap";
    private final ChunkyBorderPaper chunkyBorderPaper;

    public PaperMapIntegrationLoader(final ChunkyBorderPaper chunkyBorderPaper) {
        this.chunkyBorderPaper = chunkyBorderPaper;
    }

    @Override
    public Optional<MapIntegration> loadBlueMap() {
        final PluginManager pluginManager = chunkyBorderPaper.getServer().getPluginManager();
        if (!pluginManager.isPluginEnabled(BLUEMAP)) {
            return Optional.empty();
        }
        return Optional.ofNullable(chunkyBorderPaper.getServer().getPluginManager().getPlugin(BLUEMAP))
                .map(blueMap -> new BlueMapIntegration() {
                    @Override
                    protected String worldName(final World world) {
                        return bukkitName(world);
                    }
                });
    }

    @Override
    public Optional<MapIntegration> loadDynmap() {
        final PluginManager pluginManager = chunkyBorderPaper.getServer().getPluginManager();
        if (!pluginManager.isPluginEnabled(DYNMAP)) {
            return Optional.empty();
        }
        return Optional.ofNullable(chunkyBorderPaper.getServer().getPluginManager().getPlugin(DYNMAP))
                .map(dynmap -> {
                    final DynmapCommonAPI dynmapAPI = (DynmapCommonAPI) dynmap;
                    return dynmapAPI.markerAPIInitialized() ? new DynmapIntegration(dynmapAPI) {
                        @Override
                        protected String worldName(final World world) {
                            return bukkitName(world);
                        }
                    } : null;
                });
    }

    @Override
    public Optional<MapIntegration> loadPl3xMap() {
        final PluginManager pluginManager = chunkyBorderPaper.getServer().getPluginManager();
        if (!pluginManager.isPluginEnabled(PL3XMAP)) {
            return Optional.empty();
        }
        return Optional.ofNullable(pluginManager.getPlugin(PL3XMAP))
                .map(pl3xmap -> new Pl3xMapIntegration() {
                    @Override
                    protected String worldName(final World world) {
                        return bukkitName(world);
                    }
                });
    }

    @Override
    public Optional<MapIntegration> loadSquaremap() {
        final PluginManager pluginManager = chunkyBorderPaper.getServer().getPluginManager();
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

    private static String bukkitName(final World world) {
        final NamespacedKey key = NamespacedKey.fromString(world.getKey());
        final org.bukkit.World bukkitWorld = key == null ? null : Bukkit.getWorld(key);
        return bukkitWorld == null ? world.getName() : bukkitWorld.getName();
    }
}
