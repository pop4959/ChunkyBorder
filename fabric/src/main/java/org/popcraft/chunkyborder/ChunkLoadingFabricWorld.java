package org.popcraft.chunkyborder;

import net.minecraft.server.world.ServerWorld;
import org.popcraft.chunky.platform.FabricWorld;

public class ChunkLoadingFabricWorld extends FabricWorld {
    private final ServerWorld world;

    public ChunkLoadingFabricWorld(final ServerWorld world) {
        super(world);
        this.world = world;
    }

    @Override
    public int getElevation(final int x, final int z) {
        world.getChunk(x >> 4, z >> 4);
        return super.getElevation(x, z);
    }
}
