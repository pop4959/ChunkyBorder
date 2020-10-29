package org.popcraft.chunkyborder;

import org.dynmap.DynmapAPI;
import org.popcraft.chunky.integration.DynmapIntegration;
import org.popcraft.chunky.integration.MapIntegration;

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
                    .ifPresent(dynmap -> mapIntegrations.add(new DynmapIntegration((DynmapAPI) dynmap)));
        }
        mapIntegrations.forEach(mapIntegration -> mapIntegration.setOptions(label, color, hideByDefault, priority, weight));
        chunkyBorder.getBorders().values().forEach(border -> {
            border.reinitializeBorder(chunkyBorder.isBorderChunkAligned());
            mapIntegrations.forEach(mapIntegration -> mapIntegration.addShapeMarker(chunkyBorder.getServer().getWorld(border.getWorld()), border.getBorder()));
        });
    }
}
