package org.popcraft.chunkyborder;

import net.pl3x.map.api.Pl3xMap;
import net.pl3x.map.api.Pl3xMapProvider;
import org.dynmap.DynmapAPI;
import org.popcraft.chunky.integration.DynmapIntegration;
import org.popcraft.chunky.integration.MapIntegration;
import org.popcraft.chunky.integration.Pl3xMapIntegration;

import java.util.List;
import java.util.Optional;

public class BorderInitializationTask implements Runnable {
    private ChunkyBorder chunkyBorder;

    public BorderInitializationTask(ChunkyBorder chunkyBorder) {
        this.chunkyBorder = chunkyBorder;
    }

    @Override
    public void run() {
        final String label = chunkyBorder.getConfig().getString("map-options.label", "World Border");
        final String color = chunkyBorder.getConfig().getString("map-options.color", "FF0000");
        final boolean hideByDefault = chunkyBorder.getConfig().getBoolean("map-options.hide-by-default", false);
        final int priority = chunkyBorder.getConfig().getInt("map-options.priority", 0);
        final int weight = chunkyBorder.getConfig().getInt("map-options.weight", 3);
        final List<MapIntegration> mapIntegrations = chunkyBorder.getMapIntegrations();
        if (chunkyBorder.getConfig().getBoolean("map-options.enable.bluemap", true)) {
            try {
                Class.forName("de.bluecolored.bluemap.api.BlueMapAPIListener");
                BlueMapWorkaround.load(chunkyBorder, mapIntegrations);
            } catch (ClassNotFoundException ignored) {
            }
        }
        if (chunkyBorder.getConfig().getBoolean("map-options.enable.dynmap", true)) {
            Optional.ofNullable(chunkyBorder.getServer().getPluginManager().getPlugin("dynmap"))
                    .ifPresent(dynmap -> {
                        DynmapAPI dynmapAPI = (DynmapAPI) dynmap;
                        try {
                            // This next line will throw an exception if Dynmap did not enable properly.
                            dynmapAPI.getMarkerAPI();
                            mapIntegrations.add(new DynmapIntegration(dynmapAPI));
                        } catch (NullPointerException ignored) {
                        }
                    });
        }
        if (chunkyBorder.getConfig().getBoolean("map-options.enable.pl3xmap", true)) {
            Optional.ofNullable(chunkyBorder.getServer().getPluginManager().getPlugin("Pl3xMap"))
                    .ifPresent(pl3xMap -> {
                        try {
                            Pl3xMap pl3xMapAPI = Pl3xMapProvider.get();
                            mapIntegrations.add(new Pl3xMapIntegration(pl3xMapAPI));
                        } catch (IllegalStateException ignored) {
                        }
                    });
        }
        mapIntegrations.forEach(mapIntegration -> mapIntegration.setOptions(label, color, hideByDefault, priority, weight));
        chunkyBorder.getBorders().values().forEach(border -> {
            border.reinitializeBorder(chunkyBorder.getChunky(), chunkyBorder.isBorderChunkAligned());
            if (border.getWorld() != null) {
                chunkyBorder.getChunky().getPlatform().getServer().getWorld(border.getWorld()).ifPresent(world -> mapIntegrations.forEach(mapIntegration -> mapIntegration.addShapeMarker(world, border.getBorder())));
            }
        });
    }
}
