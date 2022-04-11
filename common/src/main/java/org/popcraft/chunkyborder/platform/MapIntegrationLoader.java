package org.popcraft.chunkyborder.platform;

import org.popcraft.chunky.integration.MapIntegration;

import java.util.Optional;

public interface MapIntegrationLoader {
    Optional<MapIntegration> loadBlueMap();

    Optional<MapIntegration> loadDynmap();

    Optional<MapIntegration> loadSquaremap();
}
