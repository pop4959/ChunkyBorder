package org.popcraft.chunkyborder.event.border;

import org.popcraft.chunky.event.Event;
import org.popcraft.chunky.platform.World;
import org.popcraft.chunky.shape.Shape;

public record BorderChangeEvent(World world, Shape shape) implements Event {
}
