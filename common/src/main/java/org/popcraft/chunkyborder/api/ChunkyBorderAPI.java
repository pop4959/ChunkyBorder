package org.popcraft.chunkyborder.api;

import org.popcraft.chunkyborder.api.event.border.BorderChangeEvent;
import org.popcraft.chunkyborder.api.event.border.BorderRedirectEvent;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public interface ChunkyBorderAPI {
    int version();

    Optional<Border> getBorder(final String world);

    boolean addBorder(final String world, final String shape, final double centerX, final double centerZ, final double radiusX, final double radiusZ);

    boolean removeBorder(final String world);

    boolean setWrap(final String world, final String wrap);

    boolean isInsideBorder(final String world, final double x, final double z);

    boolean isBypassing(final UUID player);

    void setBypassing(final UUID player, final boolean bypassing);

    void onBorderChange(final Consumer<BorderChangeEvent> listener);

    void onBorderRedirect(final Consumer<BorderRedirectEvent> listener);
}
