package org.popcraft.chunkyborder;

public final class ChunkyBorderProvider {
    private static ChunkyBorder instance;

    private ChunkyBorderProvider() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    public static ChunkyBorder get() {
        if (instance == null) {
            throw new IllegalStateException("ChunkyBorder is not loaded.");
        }
        return instance;
    }

    static void register(final ChunkyBorder instance) {
        ChunkyBorderProvider.instance = instance;
    }

    static void unregister() {
        ChunkyBorderProvider.instance = null;
    }
}
