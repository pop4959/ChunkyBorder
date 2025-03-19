package org.popcraft.chunkyborder.event.server;

import org.popcraft.chunky.event.Cancellable;
import org.popcraft.chunky.platform.Player;
import org.popcraft.chunky.platform.util.Location;

public class BlockBreakEvent extends Cancellable {
    private final Player player;
    private final Location location;

    public BlockBreakEvent(final Player player, final Location location) {
        this.player = player;
        this.location = location;
    }

    public Player getPlayer() {
        return player;
    }

    public Location getLocation() {
        return location;
    }
}
