package org.popcraft.chunkyborder.platform;

import java.nio.file.Path;

public interface Config {
    Path getDirectory();

    int version();

    long checkInterval();

    String message();

    default boolean hasMessage() {
        final String message = message();
        return message != null && !message.isBlank();
    }

    boolean useActionBar();

    String effect();

    default boolean hasEffect() {
        final String effect = effect();
        return effect != null && !effect.isBlank();
    }

    String sound();

    default boolean hasSound() {
        final String sound = sound();
        return sound != null && !sound.isBlank();
    }

    boolean preventMobSpawns();

    boolean preventEnderpearl();

    boolean preventChorusFruit();

    boolean visualizerEnabled();

    int visualizerRange();

    String visualizerColor();

    boolean blueMapEnabled();

    boolean dynmapEnabled();

    boolean pl3xmapEnabled();

    boolean squaremapEnabled();

    String label();

    boolean hideByDefault();

    String color();

    int weight();

    int priority();

    void reload();
}
