package org.popcraft.chunkyborder.event;

import org.popcraft.chunky.event.Cancellable;
import org.popcraft.chunky.platform.util.Location;

public class CreatureSpawnEvent extends Cancellable {
    private final Location location;

    public CreatureSpawnEvent(final Location location) {
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }
}
