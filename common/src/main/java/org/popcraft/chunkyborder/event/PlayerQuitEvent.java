package org.popcraft.chunkyborder.event;

import org.popcraft.chunky.event.Event;
import org.popcraft.chunky.platform.Player;

public class PlayerQuitEvent implements Event {
    private final Player player;

    public PlayerQuitEvent(final Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }
}
