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
import net.pl3x.map.markers.option.Fill;
import net.pl3x.map.markers.option.Options;
import net.pl3x.map.markers.option.Stroke;
import net.pl3x.map.markers.option.Tooltip;
import net.pl3x.map.util.Colors;
import net.pl3x.map.world.World;
import org.popcraft.chunky.platform.util.Vector2;
import org.popcraft.chunky.shape.AbstractEllipse;
import org.popcraft.chunky.shape.AbstractPolygon;
import org.popcraft.chunky.shape.Circle;
import org.popcraft.chunky.shape.Shape;
import org.popcraft.chunkyborder.BorderData;
import org.popcraft.chunkyborder.ChunkyBorderProvider;

import java.util.Optional;
import java.util.stream.Collectors;

public class Pl3xMapIntegration extends AbstractMapIntegration {
    private static final Key CHUNKY_KEY = Key.of("chunky");
    private final Pl3xMap pl3xMap;
    private Layer defaultLayer;
    private Layer chunkyLayer;
    private Options markerOptions;

    public Pl3xMapIntegration(final Pl3xMap pl3xMap) {
        this.pl3xMap = pl3xMap;

        pl3xMap.getEventRegistry().register(new EventListener() {
            @EventHandler
            public void onWorldLoaded(WorldLoadedEvent event) {
                // re-add markers on world load (happens on pl3xmap reload, etc)
                ChunkyBorderProvider.get().getChunky().getServer().getWorld(event.getWorld().getName())
                        .ifPresent(world -> ChunkyBorderProvider.get().getBorder(world.getName()).map(BorderData::getBorder)
                                .ifPresent(shape -> addShapeMarker(world, shape)));
            }
        });
    }

    @Override
    public void addShapeMarker(final org.popcraft.chunky.platform.World world, final Shape shape) {
        getWorld(world).ifPresent(pl3xmapWorld -> {
            final Marker marker;
            if (shape instanceof final AbstractPolygon polygon) {
                marker = Marker.polyline(polygon.points().stream()
                        .map(point -> Point.of(point.getX(), point.getZ()))
                        .collect(Collectors.toList())).loop();
            } else if (shape instanceof final AbstractEllipse ellipse) {
                final Vector2 center = ellipse.center();
                final Vector2 radii = ellipse.radii();
                if (ellipse instanceof Circle) {
                    marker = Marker.circle(center.getX(), center.getZ(), radii.getX());
                } else {
                    marker = Marker.ellipse(center.getX(), center.getZ(), radii.getX(), radii.getZ());
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
        pl3xMap.getWorldRegistry().entries().values().forEach(this::resetLayer);
    }

    private SimpleLayer getLayer(final World world) {
        final Layer defaultLayer = world.getLayerRegistry().get(WorldBorderLayer.KEY);
        if (defaultLayer != null) {
            this.defaultLayer = defaultLayer;
            world.getLayerRegistry().unregister(WorldBorderLayer.KEY);
        }
        return (SimpleLayer) world.getLayerRegistry().getOrRegister(CHUNKY_KEY, this.chunkyLayer);
    }

    private void resetLayer(final World world) {
        world.getLayerRegistry().unregister(CHUNKY_KEY);
        world.getLayerRegistry().register(defaultLayer);
    }

    @Override
    public void setOptions(final String label, final String color, final boolean hideByDefault, final int priority, final int weight) {
        super.setOptions(label, color, hideByDefault, priority, weight);
        this.markerOptions = new Options(new Stroke(this.weight, Colors.setAlpha(0xFF, this.color)), new Fill(false), new Tooltip(this.label), null);
        this.chunkyLayer = new SimpleLayer(CHUNKY_KEY, () -> this.label)
                .setDefaultHidden(hideByDefault)
                .setPriority(1).setZIndex(priority);
    }

    private Optional<World> getWorld(org.popcraft.chunky.platform.World world) {
        return Optional.ofNullable(pl3xMap.getWorldRegistry().get(world.getName())).filter(World::isEnabled);
    }
}
