package org.popcraft.chunkyborder.shape;

import java.util.Arrays;

public class PolygonBorderShape implements BorderShape {
    private final double[] pointsX;
    private final double[] pointsZ;

    public PolygonBorderShape(final double[] pointsX, final double[] pointsZ) {
        this.pointsX = pointsX;
        this.pointsZ = pointsZ;
    }

    public double[] getPointsX() {
        return pointsX;
    }

    public double[] getPointsZ() {
        return pointsZ;
    }
}
