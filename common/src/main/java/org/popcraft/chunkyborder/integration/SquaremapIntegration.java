package org.popcraft.chunkyborder.integration;

import org.popcraft.chunky.platform.World;
import org.popcraft.chunky.platform.util.Vector2;
import org.popcraft.chunky.shape.AbstractEllipse;
import org.popcraft.chunky.shape.AbstractPolygon;
import org.popcraft.chunky.shape.Circle;
import org.popcraft.chunky.shape.Shape;
import xyz.jpenilla.squaremap.api.Key;
import xyz.jpenilla.squaremap.api.LayerProvider;
import xyz.jpenilla.squaremap.api.MapWorld;
import xyz.jpenilla.squaremap.api.Point;
import xyz.jpenilla.squaremap.api.Registry;
import xyz.jpenilla.squaremap.api.SimpleLayerProvider;
import xyz.jpenilla.squaremap.api.Squaremap;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import xyz.jpenilla.squaremap.api.marker.Marker;
import xyz.jpenilla.squaremap.api.marker.MarkerOptions;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class SquaremapIntegration extends AbstractMapIntegration {
    private static final Key WORLDBORDER_KEY = Key.of("squaremap-worldborder");
    private static final Key CHUNKY_KEY = Key.of("chunky");
    private final Squaremap squaremap;
    private final Map<String, LayerProvider> defaultProviders = new HashMap<>();
    private boolean hideByDefault;
    private int priority;

    public SquaremapIntegration(final Squaremap squaremap) {
        this.squaremap = squaremap;
    }

    @Override
    public void addShapeMarker(final World world, final Shape shape) {
        getWorldIdentifier(world).flatMap(squaremap::getWorldIfEnabled).ifPresent(squaremapWorld -> {
            final Registry<LayerProvider> layerRegistry = squaremapWorld.layerRegistry();
            if (layerRegistry.hasEntry(WORLDBORDER_KEY)) {
                defaultProviders.put(squaremapWorld.identifier().asString(), layerRegistry.get(WORLDBORDER_KEY));
                layerRegistry.unregister(WORLDBORDER_KEY);
            }
            if (!layerRegistry.hasEntry(CHUNKY_KEY)) {
                layerRegistry.register(CHUNKY_KEY, SimpleLayerProvider.builder(this.label)
                        .defaultHidden(hideByDefault)
                        .layerPriority(1)
                        .zIndex(priority)
                        .build());
            }
            final SimpleLayerProvider chunkyLayerProvider = (SimpleLayerProvider) layerRegistry.get(CHUNKY_KEY);
            chunkyLayerProvider.clearMarkers();
            final Marker marker;
            if (shape instanceof AbstractPolygon) {
                final AbstractPolygon polygon = (AbstractPolygon) shape;
                final List<Point> points = polygon.points().stream().map(point -> Point.of(point.getX(), point.getZ())).collect(Collectors.toList());
                final Vector2 lastPoint = polygon.points().get(0);
                points.add(Point.of(lastPoint.getX(), lastPoint.getZ()));
                marker = Marker.polyline(points);
            } else if (shape instanceof AbstractEllipse) {
                final AbstractEllipse ellipse = (AbstractEllipse) shape;
                final Vector2 center = ellipse.center();
                final Vector2 radii = ellipse.radii();
                final Point centerPoint = Point.of(center.getX(), center.getZ());
                if (ellipse instanceof Circle) {
                    marker = Marker.circle(centerPoint, radii.getX());
                } else {
                    marker = ellipse(centerPoint, radii.getX(), radii.getZ());
                }
            } else {
                return;
            }
            final MarkerOptions markerOptions = MarkerOptions.builder()
                    .stroke(true)
                    .strokeColor(new Color(this.color))
                    .strokeWeight(this.weight)
                    .fill(false)
                    .clickTooltip(this.label)
                    .build();
            marker.markerOptions(markerOptions);
            chunkyLayerProvider.addMarker(CHUNKY_KEY, marker);
        });
    }

    @Override
    public void removeShapeMarker(final World world) {
        getWorldIdentifier(world).flatMap(squaremap::getWorldIfEnabled).ifPresent(this::unregisterLayer);
    }

    @Override
    public void removeAllShapeMarkers() {
        squaremap.mapWorlds().forEach(this::unregisterLayer);
    }

    private void unregisterLayer(final MapWorld mapWorld) {
        final Registry<LayerProvider> layerRegistry = mapWorld.layerRegistry();
        if (!layerRegistry.hasEntry(WORLDBORDER_KEY)) {
            final LayerProvider defaultProvider = defaultProviders.get(mapWorld.identifier().asString());
            if (defaultProvider != null) {
                layerRegistry.register(WORLDBORDER_KEY, defaultProvider);
            }
        }
        if (layerRegistry.hasEntry(CHUNKY_KEY)) {
            ((SimpleLayerProvider) layerRegistry.get(CHUNKY_KEY)).clearMarkers();
            layerRegistry.unregister(CHUNKY_KEY);
        }
    }

    @Override
    public void setOptions(final String label, final String color, final boolean hideByDefault, final int priority, final int weight) {
        super.setOptions(label, color, hideByDefault, priority, weight);
        this.hideByDefault = hideByDefault;
        this.priority = priority;
    }

    private Marker ellipse(final Point center, final double radiusX, final double radiusZ) {
        final int numPoints = 360;
        final Point[] points = new Point[numPoints + 1];
        final double segmentAngle = 2 * Math.PI / numPoints;
        for (int i = 0; i < numPoints; ++i) {
            final double pointX = center.x() + Math.sin(segmentAngle * i) * radiusX;
            final double pointZ = center.z() + Math.cos(segmentAngle * i) * radiusZ;
            points[i] = Point.of(pointX, pointZ);
        }
        points[numPoints] = Point.of(center.x(), center.z() + radiusZ);
        return Marker.polyline(points);
    }

    private Optional<WorldIdentifier> getWorldIdentifier(final World world) {
        try {
            return Optional.of(WorldIdentifier.parse(world.getKey()));
        } catch (final IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
