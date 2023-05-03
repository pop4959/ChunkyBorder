package org.popcraft.chunkyborder.platform;

import org.popcraft.chunkyborder.integration.MapIntegration;

import java.util.Optional;

public interface MapIntegrationLoader {
    Optional<MapIntegration> loadBlueMap();

    Optional<MapIntegration> loadDynmap();

    Optional<MapIntegration> loadPl3xMap();

    Optional<MapIntegration> loadSquaremap();
}
