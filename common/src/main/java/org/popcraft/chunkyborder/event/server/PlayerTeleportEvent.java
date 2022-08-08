package org.popcraft.chunkyborder.event.server;

import org.popcraft.chunky.platform.Player;
import org.popcraft.chunky.platform.util.Location;

import java.util.Optional;

public class PlayerTeleportEvent {
    private final Player player;
    private final Location location;
    private Location redirect;

    public PlayerTeleportEvent(final Player player, final Location location) {
        this.player = player;
        this.location = location;
    }

    public Player getPlayer() {
        return player;
    }

    public Location getLocation() {
        return location;
    }

    public Optional<Location> redirect() {
        return Optional.ofNullable(redirect);
    }

    public void redirect(final Location redirect) {
        this.redirect = redirect;
    }
}
