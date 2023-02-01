package org.popcraft.chunkyborder;

import org.popcraft.chunkyborder.integration.MapIntegration;
import org.popcraft.chunkyborder.platform.MapIntegrationLoader;

import java.util.List;

public class BorderInitializationTask implements Runnable {
    private final ChunkyBorder chunkyBorder;

    public BorderInitializationTask(final ChunkyBorder chunkyBorder) {
        this.chunkyBorder = chunkyBorder;
    }

    @Override
    public void run() {
        final String label = chunkyBorder.getConfig().label();
        final String color = chunkyBorder.getConfig().color();
        final boolean hideByDefault = chunkyBorder.getConfig().hideByDefault();
        final int priority = chunkyBorder.getConfig().priority();
        final int weight = chunkyBorder.getConfig().weight();
        final MapIntegrationLoader mapIntegrationLoader = chunkyBorder.getMapIntegrationLoader();
        final List<MapIntegration> mapIntegrations = chunkyBorder.getMapIntegrations();
        if (chunkyBorder.getConfig().blueMapEnabled()) {
            mapIntegrationLoader.loadBlueMap().ifPresent(mapIntegrations::add);
        }
        if (chunkyBorder.getConfig().dynmapEnabled()) {
            mapIntegrationLoader.loadDynmap().ifPresent(mapIntegrations::add);
        }
        if (chunkyBorder.getConfig().pl3xMapEnabled()) {
            mapIntegrationLoader.loadPl3xMap().ifPresent(mapIntegrations::add);
        }
        if (chunkyBorder.getConfig().squaremapEnabled()) {
            mapIntegrationLoader.loadSquaremap().ifPresent(mapIntegrations::add);
        }
        for (MapIntegration mapIntegration : mapIntegrations) {
            mapIntegration.setOptions(label, color, hideByDefault, priority, weight);
        }
        chunkyBorder.addBorders();
    }
}
