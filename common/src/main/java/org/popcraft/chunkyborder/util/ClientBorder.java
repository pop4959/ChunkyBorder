package org.popcraft.chunkyborder.util;

import org.popcraft.chunky.platform.util.Vector2;
import org.popcraft.chunky.shape.AbstractEllipse;
import org.popcraft.chunky.shape.AbstractPolygon;
import org.popcraft.chunky.shape.Shape;
import org.popcraft.chunkyborder.shape.BorderShape;
import org.popcraft.chunkyborder.shape.EllipseBorderShape;
import org.popcraft.chunkyborder.shape.PolygonBorderShape;

import java.util.List;

public record ClientBorder(String worldKey, BorderShape borderShape) {
    public ClientBorder(final String worldKey, final Shape shape) {
        this(worldKey, convertShapeToBorderShape(shape));
    }

    private static BorderShape convertShapeToBorderShape(final Shape shape) {
        if (shape instanceof final AbstractPolygon polygon) {
            final List<Vector2> points = polygon.points();
            final int numPoints = points.size();
            final double[] pointsX = new double[numPoints];
            final double[] pointsZ = new double[numPoints];
            for (int i = 0; i < numPoints; i++) {
                final Vector2 point = points.get(i);
                pointsX[i] = point.getX();
                pointsZ[i] = point.getZ();
            }
            return new PolygonBorderShape(pointsX, pointsZ);
        }
        if (shape instanceof final AbstractEllipse ellipse) {
            final Vector2 center = ellipse.center();
            final Vector2 radii = ellipse.radii();
            return new EllipseBorderShape(center.getX(), center.getZ(), radii.getX(), radii.getZ());
        }
        return null;
    }
}
