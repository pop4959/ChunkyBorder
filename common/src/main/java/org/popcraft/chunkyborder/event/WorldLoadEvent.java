package org.popcraft.chunkyborder.event;

import org.popcraft.chunky.event.Event;
import org.popcraft.chunky.platform.World;

public record WorldLoadEvent(World world) implements Event {
}
