package org.popcraft.chunkyborder;

import net.minecraft.server.level.ServerLevel;
import org.popcraft.chunky.platform.NeoForgeWorld;

public class ChunkLoadingNeoForgeWorld extends NeoForgeWorld {
    private final ServerLevel world;

    public ChunkLoadingNeoForgeWorld(final ServerLevel world) {
        super(world);
        this.world = world;
    }

    @Override
    public int getElevation(final int x, final int z) {
        world.getChunk(x >> 4, z >> 4);
        return super.getElevation(x, z);
    }
}
