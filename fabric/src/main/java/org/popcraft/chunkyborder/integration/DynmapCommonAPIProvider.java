package org.popcraft.chunkyborder.integration;

import org.dynmap.DynmapCommonAPI;
import org.dynmap.DynmapCommonAPIListener;

import java.util.Optional;

public class DynmapCommonAPIProvider extends DynmapCommonAPIListener {
    private static DynmapCommonAPI instance;

    static {
        DynmapCommonAPIListener.register(new DynmapCommonAPIProvider());
    }

    public static Optional<DynmapCommonAPI> get() {
        return Optional.ofNullable(instance);
    }

    @Override
    public synchronized void apiEnabled(final DynmapCommonAPI dynmapCommonAPI) {
        instance = dynmapCommonAPI;
    }

    @Override
    public synchronized void apiDisabled(final DynmapCommonAPI api) {
        instance = null;
    }
}
