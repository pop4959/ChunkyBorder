package org.popcraft.chunkyborder.integration;

import org.dynmap.DynmapCommonAPI;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.CircleMarker;
import org.dynmap.markers.MarkerDescription;
import org.dynmap.markers.MarkerSet;
import org.popcraft.chunky.platform.World;
import org.popcraft.chunky.platform.util.Vector2;
import org.popcraft.chunky.shape.AbstractEllipse;
import org.popcraft.chunky.shape.AbstractPolygon;
import org.popcraft.chunky.shape.Shape;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DynmapIntegration extends AbstractMapIntegration {
    private final MarkerSet markerSet;
    private final Map<String, MarkerDescription> markers;

    public DynmapIntegration(final DynmapCommonAPI dynmapAPI) {
        this.markerSet = dynmapAPI.getMarkerAPI().createMarkerSet("chunky.markerset", this.label, null, false);
        this.markers = new HashMap<>();
    }

    @Override
    public void addShapeMarker(final World world, final Shape shape) {
        removeShapeMarker(world);
        final String dynmapWorldName = adaptWorldName(world.getName());
        if (shape instanceof final AbstractPolygon polygon) {
            final List<Vector2> points = polygon.points();
            final int size = points.size();
            final double[] pointsX = new double[size];
            final double[] pointsZ = new double[size];
            for (int i = 0; i < size; ++i) {
                final Vector2 point = points.get(i);
                pointsX[i] = point.getX();
                pointsZ[i] = point.getZ();
            }
            final AreaMarker marker = markerSet.createAreaMarker(null, this.label, false, dynmapWorldName, pointsX, pointsZ, false);
            marker.setLineStyle(this.weight, 1f, color);
            marker.setFillStyle(0f, 0x000000);
            markers.put(world.getName(), marker);
        } else if (shape instanceof final AbstractEllipse ellipse) {
            final Vector2 center = ellipse.center();
            final Vector2 radii = ellipse.radii();
            final CircleMarker marker = markerSet.createCircleMarker(null, this.label, false, dynmapWorldName, center.getX(), world.getSeaLevel(), center.getZ(), radii.getX(), radii.getZ(), false);
            marker.setLineStyle(this.weight, 1f, color);
            marker.setFillStyle(0f, 0x000000);
            markers.put(world.getName(), marker);
        }
    }

    @Override
    public void removeShapeMarker(final World world) {
        final MarkerDescription marker = markers.remove(world.getName());
        if (marker != null) {
            marker.deleteMarker();
        }
    }

    @Override
    public void removeAllShapeMarkers() {
        if (markerSet != null) {
            markerSet.deleteMarkerSet();
        }
        markers.clear();
    }

    @Override
    public void setOptions(final String label, final String color, final boolean hideByDefault, final int priority, final int weight) {
        super.setOptions(label, color, hideByDefault, priority, weight);
        if (markerSet != null) {
            markerSet.setHideByDefault(hideByDefault);
            markerSet.setLayerPriority(priority);
        }
    }

    private String adaptWorldName(final String worldName) {
        return switch (worldName) {
            case "minecraft:overworld" -> "world";
            case "minecraft:the_nether" -> "DIM-1";
            case "minecraft:the_end" -> "DIM1";
            default -> worldName.indexOf(':') < 0 ? worldName : worldName.replace(':', '_');
        };
    }
}
