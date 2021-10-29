package org.popcraft.chunkyborder.platform;

import org.popcraft.chunky.integration.BlueMapIntegration;
import org.popcraft.chunky.integration.DynmapIntegration;
import org.popcraft.chunky.integration.Pl3xMapIntegration;

import java.util.Optional;

public interface MapIntegrationLoader {
    Optional<BlueMapIntegration> loadBlueMap();

    Optional<DynmapIntegration> loadDynmap();

    Optional<Pl3xMapIntegration> loadPl3xMap();
}
