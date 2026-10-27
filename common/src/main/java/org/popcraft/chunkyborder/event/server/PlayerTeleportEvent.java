package org.popcraft.chunkyborder.event.server;

import org.popcraft.chunky.platform.Player;
import org.popcraft.chunky.platform.util.Location;
import org.popcraft.chunkyborder.api.RedirectType;

import java.util.Optional;

public class PlayerTeleportEvent {
    private final Player player;
    private final Location location;
    private final RedirectType type;
    private final boolean prevented;
    private Location redirect;

    public PlayerTeleportEvent(final Player player, final Location location, final RedirectType type) {
        this(player, location, type, false);
    }

    public PlayerTeleportEvent(final Player player, final Location location, final RedirectType type, final boolean prevented) {
        this.player = player;
        this.location = location;
        this.type = type;
        this.prevented = prevented;
    }

    public Player getPlayer() {
        return player;
    }

    public Location getLocation() {
        return location;
    }

    public RedirectType getType() {
        return type;
    }

    public boolean isPrevented() {
        return prevented;
    }

    public Optional<Location> redirect() {
        return Optional.ofNullable(redirect);
    }

    public void redirect(final Location redirect) {
        this.redirect = redirect;
    }
}
