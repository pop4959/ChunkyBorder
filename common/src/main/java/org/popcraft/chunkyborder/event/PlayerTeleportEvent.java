package org.popcraft.chunkyborder.event;

import org.popcraft.chunky.event.Cancellable;
import org.popcraft.chunky.platform.Player;
import org.popcraft.chunky.platform.util.Location;

import java.util.Optional;

public class PlayerTeleportEvent extends Cancellable {
    private final Player player;
    private final Location location;
    private final boolean usingEnderpearl;
    private Location redirect;

    public PlayerTeleportEvent(Player player, Location location, boolean usingEnderpearl) {
        this.player = player;
        this.location = location;
        this.usingEnderpearl = usingEnderpearl;
    }

    public Player getPlayer() {
        return player;
    }

    public Location getLocation() {
        return location;
    }

    public boolean isUsingEnderpearl() {
        return usingEnderpearl;
    }

    public Optional<Location> redirect() {
        return Optional.ofNullable(redirect);
    }

    public void redirect(Location redirect) {
        this.redirect = redirect;
    }
}
