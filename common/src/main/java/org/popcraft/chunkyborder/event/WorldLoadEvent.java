package org.popcraft.chunkyborder.event;

import org.popcraft.chunky.platform.World;

public class WorldLoadEvent {
    final World world;

    public WorldLoadEvent(World world) {
        this.world = world;
    }

    public World getWorld() {
        return world;
    }
}
