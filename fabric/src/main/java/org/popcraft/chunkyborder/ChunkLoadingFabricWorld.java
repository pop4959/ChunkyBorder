package org.popcraft.chunkyborder;

import net.minecraft.server.level.ServerLevel;
import org.popcraft.chunky.platform.FabricWorld;

public class ChunkLoadingFabricWorld extends FabricWorld {
    private final ServerLevel world;

    public ChunkLoadingFabricWorld(final ServerLevel world) {
        super(world);
        this.world = world;
    }

    @Override
    public int getElevation(final int x, final int z) {
        world.getChunk(x >> 4, z >> 4);
        return super.getElevation(x, z);
    }
}
