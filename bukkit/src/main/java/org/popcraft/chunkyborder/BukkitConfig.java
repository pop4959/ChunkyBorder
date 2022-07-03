package org.popcraft.chunkyborder;

import org.popcraft.chunkyborder.platform.Config;

import java.nio.file.Path;

public class BukkitConfig implements Config {
    private final ChunkyBorderBukkit chunkyBorderBukkit;

    public BukkitConfig(final ChunkyBorderBukkit chunkyBorderBukkit) {
        this.chunkyBorderBukkit = chunkyBorderBukkit;
    }

    @Override
    public Path getDirectory() {
        return chunkyBorderBukkit.getDataFolder().toPath();
    }

    @Override
    public int version() {
        return chunkyBorderBukkit.getConfig().getInt("version", 0);
    }

    @Override
    public long checkInterval() {
        return chunkyBorderBukkit.getConfig().getLong("border-options.check-interval", 20);
    }

    @Override
    public String message() {
        return chunkyBorderBukkit.getConfig().getString("border-options.message", "&cYou have reached the edge of this world.");
    }

    @Override
    public boolean useActionBar() {
        return chunkyBorderBukkit.getConfig().getBoolean("border-options.use-action-bar", true);
    }

    @Override
    public String effect() {
        return chunkyBorderBukkit.getConfig().getString("border-options.effect", "ender_signal");
    }

    @Override
    public String sound() {
        return chunkyBorderBukkit.getConfig().getString("border-options.sound", "entity_enderman_teleport");
    }

    @Override
    public boolean preventMobSpawns() {
        return chunkyBorderBukkit.getConfig().getBoolean("border-options.prevent-mob-spawns", false);
    }

    @Override
    public boolean preventEnderpearl() {
        return chunkyBorderBukkit.getConfig().getBoolean("border-options.prevent-enderpearl", false);
    }

    @Override
    public boolean blueMapEnabled() {
        return chunkyBorderBukkit.getConfig().getBoolean("map-options.enable.bluemap", true);
    }

    @Override
    public boolean dynmapEnabled() {
        return chunkyBorderBukkit.getConfig().getBoolean("map-options.enable.dynmap", true);
    }

    @Override
    public boolean squaremapEnabled() {
        return chunkyBorderBukkit.getConfig().getBoolean("map-options.enable.squaremap", true);
    }

    @Override
    public String label() {
        return chunkyBorderBukkit.getConfig().getString("map-options.label", "World Border");
    }

    @Override
    public boolean hideByDefault() {
        return chunkyBorderBukkit.getConfig().getBoolean("map-options.hide-by-default", false);
    }

    @Override
    public String color() {
        return chunkyBorderBukkit.getConfig().getString("map-options.color", "FF0000");
    }

    @Override
    public int weight() {
        return chunkyBorderBukkit.getConfig().getInt("map-options.weight", 3);
    }

    @Override
    public int priority() {
        return chunkyBorderBukkit.getConfig().getInt("map-options.priority", 0);
    }
}
