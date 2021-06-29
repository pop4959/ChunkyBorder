package org.popcraft.chunkyborder;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Optional;

public class PlayerData {
    private final Player player;
    private Location lastLocation;
    private boolean lastLocationValid = true;
    private boolean bypassing;

    public PlayerData(final Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    public Optional<Location> getLastLocation() {
        return Optional.ofNullable(lastLocation);
    }

    public void setLastLocation(final Location lastLocation) {
        this.lastLocation = lastLocation;
    }

    public boolean isLastLocationValid() {
        return lastLocationValid;
    }

    public void setLastLocationValid(final boolean lastLocationValid) {
        this.lastLocationValid = lastLocationValid;
    }

    public boolean isBypassing() {
        return bypassing;
    }

    public void setBypassing(final boolean bypassing) {
        this.bypassing = bypassing;
    }
}
