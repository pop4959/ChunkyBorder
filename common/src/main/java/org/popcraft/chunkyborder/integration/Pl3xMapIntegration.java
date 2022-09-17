package org.popcraft.chunkyborder.integration;

import net.pl3x.map.Key;
import net.pl3x.map.Pl3xMap;
import net.pl3x.map.event.EventHandler;
import net.pl3x.map.event.EventListener;
import net.pl3x.map.event.world.WorldLoadedEvent;
import net.pl3x.map.markers.Point;
import net.pl3x.map.markers.layer.Layer;
import net.pl3x.map.markers.layer.SimpleLayer;
import net.pl3x.map.markers.layer.WorldBorderLayer;
import net.pl3x.map.markers.marker.Marker;
import net.pl3x.map.markers.option.Options;
import net.pl3x.map.util.Colors;
import net.pl3x.map.world.World;
import org.popcraft.chunky.platform.util.Vector2;
import org.popcraft.chunky.shape.AbstractEllipse;
import org.popcraft.chunky.shape.AbstractPolygon;
import org.popcraft.chunky.shape.Circle;
import org.popcraft.chunky.shape.Shape;
import org.popcraft.chunkyborder.BorderData;
import org.popcraft.chunkyborder.ChunkyBorder;
import org.popcraft.chunkyborder.ChunkyBorderProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Pl3xMapIntegration extends AbstractMapIntegration {
    private static final Key CHUNKY_KEY = Key.of("chunky");
    private final Pl3xMap pl3xMap;
    private final Map<World, Layer> defaultLayers = new HashMap<>();
    private boolean hideByDefault;
    private int priority;
    private Options markerOptions;

    public Pl3xMapIntegration(final Pl3xMap pl3xMap) {
        this.pl3xMap = pl3xMap;
        pl3xMap.getEventRegistry().register(new EventListener() {
            @EventHandler
            @SuppressWarnings("unused")
            public void onWorldLoaded(final WorldLoadedEvent event) {
                final ChunkyBorder chunkyBorder = ChunkyBorderProvider.get();
                chunkyBorder.getChunky().getServer().getWorld(event.getWorld().getName())
                        .ifPresent(world -> chunkyBorder.getBorder(world.getName())
                                .map(BorderData::getBorder)
                                .ifPresent(shape -> addShapeMarker(world, shape)));
            }
        });
    }

    @Override
    public void addShapeMarker(final org.popcraft.chunky.platform.World world, final Shape shape) {
        getWorld(world).ifPresent(pl3xmapWorld -> {
            final Marker<?> marker;
            if (shape instanceof final AbstractPolygon polygon) {
                marker = Marker.polyline(CHUNKY_KEY, polygon.points().stream()
                        .map(point -> Point.of(point.getX(), point.getZ()))
                        .toList()).loop();
            } else if (shape instanceof final AbstractEllipse ellipse) {
                final Vector2 center = ellipse.center();
                final Vector2 radii = ellipse.radii();
                if (ellipse instanceof Circle) {
                    marker = Marker.circle(CHUNKY_KEY, center.getX(), center.getZ(), radii.getX());
                } else {
                    marker = Marker.ellipse(CHUNKY_KEY, center.getX(), center.getZ(), radii.getX(), radii.getZ());
                }
            } else {
                return;
            }
            getLayer(pl3xmapWorld).clearMarkers().addMarker(CHUNKY_KEY, marker.setOptions(this.markerOptions));
        });
    }

    @Override
    public void removeShapeMarker(final org.popcraft.chunky.platform.World world) {
        getWorld(world).ifPresent(this::resetLayer);
    }

    @Override
    public void removeAllShapeMarkers() {
        this.pl3xMap.getWorldRegistry().entries().values().forEach(this::resetLayer);
    }

    @Override
    public void setOptions(final String label, final String color, final boolean hideByDefault, final int priority, final int weight) {
        super.setOptions(label, color, hideByDefault, priority, weight);
        this.hideByDefault = hideByDefault;
        this.priority = priority;
        this.markerOptions = Options.builder()
                .strokeWeight(this.weight)
                .strokeColor(Colors.setAlpha(0xFF, this.color))
                .fill(false)
                .tooltipContent(this.label)
                .tooltipSticky(true)
                .build();
    }

    private SimpleLayer getLayer(final World world) {
        final Layer worldBorderLayer = world.getLayerRegistry().unregister(WorldBorderLayer.KEY);
        if (!this.defaultLayers.containsKey(world)) {
            this.defaultLayers.put(world, worldBorderLayer);
        }
        Layer chunkyLayer = world.getLayerRegistry().get(CHUNKY_KEY);
        if (chunkyLayer == null) {
            chunkyLayer = new SimpleLayer(CHUNKY_KEY, () -> this.label)
                    .setDefaultHidden(this.hideByDefault)
                    .setPriority(1)
                    .setZIndex(this.priority);
            world.getLayerRegistry().register(chunkyLayer);
        }
        return (SimpleLayer) chunkyLayer;
    }

    private void resetLayer(final World world) {
        world.getLayerRegistry().unregister(CHUNKY_KEY);
        world.getLayerRegistry().register(this.defaultLayers.get(world));
        this.defaultLayers.remove(world);
    }

    private Optional<World> getWorld(final org.popcraft.chunky.platform.World world) {
        return Optional.ofNullable(this.pl3xMap.getWorldRegistry().get(world.getName())).filter(World::isEnabled);
    }
}
