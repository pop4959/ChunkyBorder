package org.popcraft.chunkyborder.event.border;

import org.popcraft.chunky.platform.Player;
import org.popcraft.chunky.platform.util.Location;

public record BorderWrapEvent(Player player, Location from, Location to) {
}
