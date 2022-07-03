package org.popcraft.chunkyborder.event;

import org.popcraft.chunky.platform.World;

public class WorldUnloadEvent {
    final World world;

    public WorldUnloadEvent(final World world) {
        this.world = world;
    }

    public World getWorld() {
        return world;
    }
}
