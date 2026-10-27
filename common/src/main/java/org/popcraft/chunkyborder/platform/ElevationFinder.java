package org.popcraft.chunkyborder.platform;

import org.popcraft.chunky.platform.World;

import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;

public interface ElevationFinder {
    CompletableFuture<OptionalInt> findElevation(World world, int x, int y, int z);
}
