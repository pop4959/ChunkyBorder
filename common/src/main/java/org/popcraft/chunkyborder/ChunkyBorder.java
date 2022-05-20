package org.popcraft.chunkyborder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.popcraft.chunky.Chunky;
import org.popcraft.chunky.event.EventBus;
import org.popcraft.chunky.platform.Player;
import org.popcraft.chunky.platform.World;
import org.popcraft.chunky.platform.util.Location;
import org.popcraft.chunky.platform.util.Vector2;
import org.popcraft.chunky.platform.util.Vector3;
import org.popcraft.chunky.shape.AbstractEllipse;
import org.popcraft.chunky.shape.AbstractPolygon;
import org.popcraft.chunky.shape.ShapeUtil;
import org.popcraft.chunky.util.TranslationKey;
import org.popcraft.chunky.util.Version;
import org.popcraft.chunkyborder.event.BlockPlaceEvent;
import org.popcraft.chunkyborder.event.CreatureSpawnEvent;
import org.popcraft.chunkyborder.event.PlayerQuitEvent;
import org.popcraft.chunkyborder.event.PlayerTeleportEvent;
import org.popcraft.chunkyborder.event.WorldLoadEvent;
import org.popcraft.chunkyborder.event.WorldUnloadEvent;
import org.popcraft.chunkyborder.integration.MapIntegration;
import org.popcraft.chunkyborder.platform.Config;
import org.popcraft.chunkyborder.platform.MapIntegrationLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

import static org.popcraft.chunky.util.Translator.translate;

public class ChunkyBorder {
    private static final Logger LOGGER = LogManager.getLogger(ChunkyBorder.class.getSimpleName());
    private final Chunky chunky;
    private final Config config;
    private final MapIntegrationLoader mapIntegrationLoader;
    private final List<MapIntegration> mapIntegrations = new ArrayList<>();
    private final Map<String, BorderData> borders;
    private final Map<UUID, PlayerData> players = new HashMap<>();
    private final Version version, targetVersion;

    public ChunkyBorder(Chunky chunky, Config config, MapIntegrationLoader mapIntegrationLoader) {
        this.chunky = chunky;
        this.config = config;
        this.mapIntegrationLoader = mapIntegrationLoader;
        this.borders = loadBorders();
        this.version = loadVersion();
        this.targetVersion = loadTargetVersion();
        subscribeEvents();
        ChunkyBorderProvider.register(this);
    }

    public void disable() {
        final List<MapIntegration> maps = getMapIntegrations();
        maps.forEach(MapIntegration::removeAllShapeMarkers);
        maps.clear();
        saveBorders();
        ChunkyBorderProvider.unregister();
    }

    private void subscribeEvents() {
        final EventBus eventBus = chunky.getEventBus();
        eventBus.subscribe(PlayerTeleportEvent.class, e -> {
            final Optional<BorderData> borderData = getBorder(e.getLocation().getWorld().getName());
            e.redirect(borderData.map(BorderData::getBorder)
                    .filter(border -> !border.isBounding(e.getLocation().getX(), e.getLocation().getZ()))
                    .filter(border -> !e.getPlayer().hasPermission("chunkyborder.bypass.move"))
                    .map(border -> {
                        e.setCancelled(e.isUsingEnderpearl() && config.preventEnderpearl());
                        final Vector2 center = Vector2.of(borderData.get().getCenterX(), borderData.get().getCenterZ());
                        final World world = e.getLocation().getWorld();
                        final Vector3 locationVector = e.getLocation().toVector();
                        final Vector2 to = Vector2.of(locationVector.getX(), locationVector.getZ());
                        final List<Vector2> intersections = new ArrayList<>();
                        if (border instanceof AbstractPolygon) {
                            final AbstractPolygon polygonBorder = (AbstractPolygon) border;
                            final List<Vector2> points = polygonBorder.points();
                            final int size = points.size();
                            for (int i = 0; i < size; ++i) {
                                final Vector2 p1 = points.get(i);
                                final Vector2 p2 = points.get(i == size - 1 ? 0 : i + 1);
                                ShapeUtil.intersection(center.getX(), center.getZ(), to.getX(), to.getZ(), p1.getX(), p1.getZ(), p2.getX(), p2.getZ()).ifPresent(intersections::add);
                            }
                        } else if (border instanceof AbstractEllipse) {
                            final AbstractEllipse ellipticalBorder = (AbstractEllipse) border;
                            final Vector2 radii = ellipticalBorder.radii();
                            final double angle = Math.atan2(to.getZ() - center.getX(), to.getX() - center.getZ());
                            intersections.add(ShapeUtil.pointOnEllipse(center.getX(), center.getZ(), radii.getX(), radii.getZ(), angle));
                        }
                        if (intersections.isEmpty()) {
                            return world.getSpawn();
                        }
                        final Vector3 centerDirection = new Vector3(center.getX() - to.getX(), 0, center.getZ() - to.getZ()).normalize().multiply(3);
                        Vector2 closest = intersections.get(0);
                        double shortestDistance = Double.MAX_VALUE;
                        for (final Vector2 intersection : intersections) {
                            final double distance = to.distanceSquared(intersection);
                            if (distance < shortestDistance && border.isBounding(intersection.getX() + centerDirection.getX(), intersection.getZ() + centerDirection.getZ())) {
                                closest = intersection;
                                shortestDistance = distance;
                            }
                        }
                        if (shortestDistance == Double.MAX_VALUE) {
                            return world.getSpawn();
                        }
                        final Location insideBorder = new Location(world, closest.getX(), 0, closest.getZ());
                        insideBorder.add(centerDirection);
                        insideBorder.setDirection(centerDirection);
                        insideBorder.setY(world.getElevation((int) insideBorder.getX(), (int) insideBorder.getZ()) + 1d);
                        final Player player = e.getPlayer();
                        if (config.useActionBar()) {
                            player.sendActionBar("custom_border_message");
                        } else {
                            player.sendMessage("custom_border_message");
                        }
                        getPlayerData(player.getUUID()).setLastLocation(insideBorder);
                        return insideBorder;
                    })
                    .orElse(null));
        });
        eventBus.subscribe(WorldLoadEvent.class, e -> getBorder(e.getWorld().getName())
                .map(BorderData::getBorder)
                .ifPresent(border -> mapIntegrations.forEach(mapIntegration -> mapIntegration.addShapeMarker(e.getWorld(), border))));
        eventBus.subscribe(WorldUnloadEvent.class, e -> getBorder(e.getWorld().getName())
                .map(BorderData::getBorder)
                .ifPresent(border -> mapIntegrations.forEach(mapIntegration -> mapIntegration.removeShapeMarker(e.getWorld()))));
        eventBus.subscribe(CreatureSpawnEvent.class, e -> e.setCancelled(getBorder(e.getLocation().getWorld().getName())
                .map(BorderData::getBorder)
                .map(border -> !border.isBounding(e.getLocation().getX(), e.getLocation().getZ()))
                .orElse(false)));
        eventBus.subscribe(BlockPlaceEvent.class, e -> {
            final Location location = e.getLocation();
            e.setCancelled(getBorder(location.getWorld().getName())
                    .map(BorderData::getBorder)
                    .map(border -> {
                        double x = ((int) location.getX()) + 0.5;
                        double z = ((int) location.getZ()) + 0.5;
                        return !border.isBounding(x, z) && !e.getPlayer().hasPermission("chunkyborder.bypass.place");
                    })
                    .orElse(false));
        });
        eventBus.subscribe(PlayerQuitEvent.class, e -> players.remove(e.getPlayer().getUUID()));
    }

    public boolean hasCompatibleChunkyVersion() {
        final Version currentVersion = chunky.getVersion();
        final Version requiredVersion = getTargetVersion();
        return currentVersion.isValid() && requiredVersion.isValid() && currentVersion.isHigherThanOrEqualTo(requiredVersion);
    }

    private Version loadVersion() {
        try (final InputStream input = getClass().getClassLoader().getResourceAsStream("version.properties")) {
            final Properties properties = new Properties();
            properties.load(input);
            return new Version(properties.getProperty("version"));
        } catch (IOException e) {
            return Version.INVALID;
        }
    }

    private Version loadTargetVersion() {
        try (final InputStream input = getClass().getClassLoader().getResourceAsStream("version.properties")) {
            final Properties properties = new Properties();
            properties.load(input);
            return new Version(properties.getProperty("target"));
        } catch (IOException e) {
            return Version.INVALID;
        }
    }

    public Chunky getChunky() {
        return chunky;
    }

    public Config getConfig() {
        return config;
    }

    public MapIntegrationLoader getMapIntegrationLoader() {
        return mapIntegrationLoader;
    }

    public List<MapIntegration> getMapIntegrations() {
        return mapIntegrations;
    }

    public Optional<BorderData> getBorder(String world) {
        return Optional.ofNullable(borders.get(world));
    }

    public Map<String, BorderData> getBorders() {
        return borders;
    }

    public Map<UUID, PlayerData> getPlayers() {
        return players;
    }

    public PlayerData getPlayerData(final UUID uuid) {
        this.players.computeIfAbsent(uuid, x -> new PlayerData(uuid));
        return players.get(uuid);
    }

    public Version getVersion() {
        return version;
    }

    public Version getTargetVersion() {
        return targetVersion;
    }

    public Map<String, BorderData> loadBorders() {
        try (FileReader fileReader = new FileReader(new File(config.getDirectory().toFile(), "borders.json"))) {
            Map<String, BorderData> loadedBorders = new Gson().fromJson(fileReader, new TypeToken<Map<String, BorderData>>() {
            }.getType());
            if (loadedBorders != null) {
                return loadedBorders;
            }
        } catch (IOException e) {
            LOGGER.warn(() -> translate(TranslationKey.BORDER_LOAD_FAILED));
        }
        return new HashMap<>();
    }

    public void saveBorders() {
        if (borders == null) {
            return;
        }
        try (FileWriter fileWriter = new FileWriter(new File(config.getDirectory().toFile(), "borders.json"))) {
            fileWriter.write(new GsonBuilder().setPrettyPrinting().create().toJson(borders));
        } catch (IOException e) {
            LOGGER.warn(() -> translate(TranslationKey.BORDER_SAVE_FAILED));
        }
    }
}
