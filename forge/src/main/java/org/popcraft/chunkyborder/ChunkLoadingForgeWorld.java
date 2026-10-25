package org.popcraft.chunkyborder;

import net.minecraft.server.level.ServerLevel;
import org.popcraft.chunky.platform.ForgeWorld;

public class ChunkLoadingForgeWorld extends ForgeWorld {
    private final ServerLevel world;

    public ChunkLoadingForgeWorld(final ServerLevel world) {
        super(world);
        this.world = world;
    }

    @Override
    public int getElevation(final int x, final int z) {
        world.getChunk(x >> 4, z >> 4);
        return super.getElevation(x, z);
    }
}
