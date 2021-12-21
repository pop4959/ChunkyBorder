package org.popcraft.chunkyborder.platform;

import java.nio.file.Path;

public interface Config {
    Path getDirectory();

    int version();

    long checkInterval();

    String message();

    boolean useActionBar();

    String effect();

    String sound();

    boolean preventMobSpawns();

    boolean preventEnderpearl();

    boolean blueMapEnabled();

    boolean dynmapEnabled();

    boolean squaremapEnabled();

    String label();

    boolean hideByDefault();

    String color();

    int weight();

    int priority();
}
