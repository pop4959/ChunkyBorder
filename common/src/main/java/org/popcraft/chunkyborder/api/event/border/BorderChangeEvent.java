package org.popcraft.chunkyborder.api.event.border;

import org.popcraft.chunky.event.Event;
import org.popcraft.chunkyborder.api.Border;

public record BorderChangeEvent(String world, Border border) implements Event {
}
