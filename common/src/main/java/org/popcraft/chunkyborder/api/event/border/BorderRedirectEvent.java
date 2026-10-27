package org.popcraft.chunkyborder.api.event.border;

import org.popcraft.chunky.event.Event;
import org.popcraft.chunkyborder.api.RedirectType;

import java.util.UUID;

public record BorderRedirectEvent(UUID player, String world, double fromX, double fromY, double fromZ, double toX, double toY, double toZ, RedirectType type) implements Event {
}
