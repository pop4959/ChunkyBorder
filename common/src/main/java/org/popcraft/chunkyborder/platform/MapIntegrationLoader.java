package org.popcraft.chunkyborder.platform;

import org.popcraft.chunky.integration.BlueMapIntegration;
import org.popcraft.chunky.integration.DynmapIntegration;
import org.popcraft.chunky.integration.SquaremapIntegration;

import java.util.Optional;

public interface MapIntegrationLoader {
    Optional<BlueMapIntegration> loadBlueMap();

    Optional<DynmapIntegration> loadDynmap();

    Optional<SquaremapIntegration> loadSquaremap();
}
