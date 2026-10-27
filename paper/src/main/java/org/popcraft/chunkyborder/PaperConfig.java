package org.popcraft.chunkyborder;

import org.popcraft.chunkyborder.platform.Config;

import java.nio.file.Path;

public class PaperConfig implements Config {
    private final ChunkyBorderPaper chunkyBorderPaper;

    public PaperConfig(final ChunkyBorderPaper chunkyBorderPaper) {
        this.chunkyBorderPaper = chunkyBorderPaper;
    }

    @Override
    public Path getDirectory() {
        return chunkyBorderPaper.getDataFolder().toPath();
    }

    @Override
    public int version() {
        return chunkyBorderPaper.getConfig().getInt("version", 0);
    }

    @Override
    public long checkInterval() {
        return chunkyBorderPaper.getConfig().getLong("border-options.check-interval", 20);
    }

    @Override
    public String message() {
        return chunkyBorderPaper.getConfig().getString("border-options.message", "&cYou have reached the edge of this world.");
    }

    @Override
    public boolean useActionBar() {
        return chunkyBorderPaper.getConfig().getBoolean("border-options.use-action-bar", true);
    }

    @Override
    public String effect() {
        return chunkyBorderPaper.getConfig().getString("border-options.effect", "ender_signal");
    }

    @Override
    public String sound() {
        return chunkyBorderPaper.getConfig().getString("border-options.sound", "entity_enderman_teleport");
    }

    @Override
    public boolean preventMobSpawns() {
        return chunkyBorderPaper.getConfig().getBoolean("border-options.prevent-mob-spawns", false);
    }

    @Override
    public boolean preventEnderpearl() {
        return chunkyBorderPaper.getConfig().getBoolean("border-options.prevent-enderpearl", false);
    }

    @Override
    public boolean preventChorusFruit() {
        return chunkyBorderPaper.getConfig().getBoolean("border-options.prevent-chorus-fruit", false);
    }

    @Override
    public boolean preventEndGateway() {
        return chunkyBorderPaper.getConfig().getBoolean("border-options.prevent-end-gateway", false);
    }

    @Override
    public boolean visualizerEnabled() {
        return chunkyBorderPaper.getConfig().getBoolean("border-options.visualizer-enabled", false);
    }

    @Override
    public int visualizerRange() {
        return chunkyBorderPaper.getConfig().getInt("border-options.visualizer-range", 8);
    }

    @Override
    public String visualizerColor() {
        return chunkyBorderPaper.getConfig().getString("border-options.visualizer-color", "20A0FF");
    }

    @Override
    public boolean blueMapEnabled() {
        return chunkyBorderPaper.getConfig().getBoolean("map-options.enable.bluemap", true);
    }

    @Override
    public boolean dynmapEnabled() {
        return chunkyBorderPaper.getConfig().getBoolean("map-options.enable.dynmap", true);
    }

    @Override
    public boolean pl3xmapEnabled() {
        return chunkyBorderPaper.getConfig().getBoolean("map-options.enable.pl3xmap", true);
    }

    @Override
    public boolean squaremapEnabled() {
        return chunkyBorderPaper.getConfig().getBoolean("map-options.enable.squaremap", true);
    }

    @Override
    public String label() {
        return chunkyBorderPaper.getConfig().getString("map-options.label", "World Border");
    }

    @Override
    public boolean hideByDefault() {
        return chunkyBorderPaper.getConfig().getBoolean("map-options.hide-by-default", false);
    }

    @Override
    public String color() {
        return chunkyBorderPaper.getConfig().getString("map-options.color", "FF0000");
    }

    @Override
    public int weight() {
        return chunkyBorderPaper.getConfig().getInt("map-options.weight", 3);
    }

    @Override
    public int priority() {
        return chunkyBorderPaper.getConfig().getInt("map-options.priority", 0);
    }

    @Override
    public void reload() {
        chunkyBorderPaper.reloadConfig();
    }
}
