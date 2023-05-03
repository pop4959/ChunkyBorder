package org.popcraft.chunkyborder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.popcraft.chunkyborder.platform.Config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class FabricConfig implements Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path savePath;
    private ConfigModel configModel = new ConfigModel();

    public FabricConfig(final Path savePath) {
        this.savePath = savePath;
        reload();
    }

    @Override
    public Path getDirectory() {
        return savePath.getParent();
    }

    @Override
    public int version() {
        return Optional.ofNullable(configModel.version).orElse(0);
    }

    @Override
    public long checkInterval() {
        return Optional.ofNullable(configModel.borderOptions.checkInterval).orElse(20);
    }

    @Override
    public String message() {
        return Optional.ofNullable(configModel.borderOptions.message).orElse("&cYou have reached the edge of this world.");
    }

    @Override
    public boolean useActionBar() {
        return Optional.ofNullable(configModel.borderOptions.useActionBar).orElse(true);
    }

    @Override
    public String effect() {
        return Optional.ofNullable(configModel.borderOptions.effect).orElse("2003");
    }

    @Override
    public String sound() {
        return Optional.ofNullable(configModel.borderOptions.sound).orElse("entity.enderman.teleport");
    }

    @Override
    public boolean preventMobSpawns() {
        return Optional.ofNullable(configModel.borderOptions.preventMobSpawns).orElse(false);
    }

    @Override
    public boolean preventEnderpearl() {
        return Optional.ofNullable(configModel.borderOptions.preventEnderpearl).orElse(false);
    }

    @Override
    public boolean preventChorusFruit() {
        return Optional.ofNullable(configModel.borderOptions.preventChorusFruit).orElse(false);
    }

    @Override
    public boolean visualizerEnabled() {
        return Optional.ofNullable(configModel.borderOptions.visualizerEnabled).orElse(false);
    }

    @Override
    public int visualizerRange() {
        return Optional.ofNullable(configModel.borderOptions.visualizerRange).orElse(8);
    }

    @Override
    public String visualizerColor() {
        return Optional.ofNullable(configModel.borderOptions.visualizerColor).orElse("20A0FF");
    }

    @Override
    public boolean blueMapEnabled() {
        return configModel.mapOptions.enable.getOrDefault("bluemap", true);
    }

    @Override
    public boolean dynmapEnabled() {
        return configModel.mapOptions.enable.getOrDefault("dynmap", true);
    }

    @Override
    public boolean pl3xmapEnabled() {
        return configModel.mapOptions.enable.getOrDefault("pl3xmap", true);
    }

    @Override
    public boolean squaremapEnabled() {
        return configModel.mapOptions.enable.getOrDefault("squaremap", true);
    }

    @Override
    public String label() {
        return Optional.ofNullable(configModel.mapOptions.label).orElse("World Border");
    }

    @Override
    public boolean hideByDefault() {
        return Optional.ofNullable(configModel.mapOptions.hideByDefault).orElse(false);
    }

    @Override
    public String color() {
        return Optional.ofNullable(configModel.mapOptions.color).orElse("FF0000");
    }

    @Override
    public int weight() {
        return Optional.ofNullable(configModel.mapOptions.weight).orElse(3);
    }

    @Override
    public int priority() {
        return Optional.ofNullable(configModel.mapOptions.priority).orElse(0);
    }

    @Override
    public void reload() {
        if (!Files.exists(this.savePath)) {
            save();
        }
        try (final Reader reader = Files.newBufferedReader(savePath)) {
            configModel = GSON.fromJson(reader, ConfigModel.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void save() {
        try {
            Files.createDirectories(savePath.getParent());
        } catch (IOException e) {
            e.printStackTrace();
        }
        try (final Writer writer = Files.newBufferedWriter(savePath)) {
            GSON.toJson(configModel, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unused")
    private static class ConfigModel {
        private Integer version = 1;
        private BorderOptions borderOptions = new BorderOptions();
        private MapOptions mapOptions = new MapOptions(List.of("bluemap", "dynmap", "squaremap"));

        public Integer getVersion() {
            return version;
        }

        public void setVersion(final Integer version) {
            this.version = version;
        }

        public BorderOptions getBorderOptions() {
            return borderOptions;
        }

        public void setBorderOptions(final BorderOptions borderOptions) {
            this.borderOptions = borderOptions;
        }

        public MapOptions getMapOptions() {
            return mapOptions;
        }

        public void setMapOptions(final MapOptions mapOptions) {
            this.mapOptions = mapOptions;
        }
    }

    @SuppressWarnings("unused")
    private static class BorderOptions {
        private Integer checkInterval = 20;
        private String message = "&cYou have reached the edge of this world.";
        private Boolean useActionBar = true;
        private String effect = "2003";
        private String sound = "entity.enderman.teleport";
        private Boolean preventMobSpawns = false;
        private Boolean preventEnderpearl = false;
        private Boolean preventChorusFruit = false;
        private Boolean visualizerEnabled = false;
        private Integer visualizerRange = 8;
        private String visualizerColor = "20A0FF";

        public Integer getCheckInterval() {
            return checkInterval;
        }

        public void setCheckInterval(final Integer checkInterval) {
            this.checkInterval = checkInterval;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(final String message) {
            this.message = message;
        }

        public Boolean getUseActionBar() {
            return useActionBar;
        }

        public void setUseActionBar(final Boolean useActionBar) {
            this.useActionBar = useActionBar;
        }

        public String getEffect() {
            return effect;
        }

        public void setEffect(final String effect) {
            this.effect = effect;
        }

        public String getSound() {
            return sound;
        }

        public void setSound(final String sound) {
            this.sound = sound;
        }

        public Boolean getPreventMobSpawns() {
            return preventMobSpawns;
        }

        public void setPreventMobSpawns(final Boolean preventMobSpawns) {
            this.preventMobSpawns = preventMobSpawns;
        }

        public Boolean getPreventEnderpearl() {
            return preventEnderpearl;
        }

        public void setPreventEnderpearl(final Boolean preventEnderpearl) {
            this.preventEnderpearl = preventEnderpearl;
        }

        public Boolean getPreventChorusFruit() {
            return preventChorusFruit;
        }

        public void setPreventChorusFruit(final Boolean preventChorusFruit) {
            this.preventChorusFruit = preventChorusFruit;
        }

        public Boolean getVisualizerEnabled() {
            return visualizerEnabled;
        }

        public void setVisualizerEnabled(final Boolean visualizerEnabled) {
            this.visualizerEnabled = visualizerEnabled;
        }

        public Integer getVisualizerRange() {
            return visualizerRange;
        }

        public void setVisualizerRange(final Integer visualizerRange) {
            this.visualizerRange = visualizerRange;
        }

        public String getVisualizerColor() {
            return visualizerColor;
        }

        public void setVisualizerColor(final String visualizerColor) {
            this.visualizerColor = visualizerColor;
        }
    }

    @SuppressWarnings("unused")
    private static class MapOptions {
        private Map<String, Boolean> enable = new HashMap<>();
        private String label = "World Border";
        private Boolean hideByDefault = false;
        private String color = "FF0000";
        private Integer weight = 3;
        private Integer priority = 0;

        public MapOptions() {
        }

        public MapOptions(final List<String> enabled) {
            for (final String toEnable : enabled) {
                enable.put(toEnable, true);
            }
        }

        public Map<String, Boolean> getEnable() {
            return enable;
        }

        public void setEnable(final Map<String, Boolean> enable) {
            this.enable = enable;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(final String label) {
            this.label = label;
        }

        public Boolean getHideByDefault() {
            return hideByDefault;
        }

        public void setHideByDefault(final Boolean hideByDefault) {
            this.hideByDefault = hideByDefault;
        }

        public String getColor() {
            return color;
        }

        public void setColor(final String color) {
            this.color = color;
        }

        public Integer getWeight() {
            return weight;
        }

        public void setWeight(final Integer weight) {
            this.weight = weight;
        }

        public Integer getPriority() {
            return priority;
        }

        public void setPriority(final Integer priority) {
            this.priority = priority;
        }
    }
}
