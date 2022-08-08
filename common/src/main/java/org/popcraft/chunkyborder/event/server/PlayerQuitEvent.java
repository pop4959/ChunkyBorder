package org.popcraft.chunkyborder.event.server;

import org.popcraft.chunky.event.Event;
import org.popcraft.chunky.platform.Player;

public record PlayerQuitEvent(Player player) implements Event {
}
