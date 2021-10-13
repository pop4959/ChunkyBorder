package org.popcraft.chunkyborder;

import org.bukkit.Location;

import java.util.Optional;
import java.util.UUID;

public class PlayerData {
    private final UUID playerId;
    private Location lastLocation;
    private boolean lastLocationValid = true;
    private boolean bypassing;

    public PlayerData(final UUID playerId) {
        this.playerId = playerId;
    }

    public UUID getPlayerId() {
        return playerId;
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
