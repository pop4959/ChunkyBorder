package org.popcraft.chunkyborder.shape;

public class EllipseBorderShape implements BorderShape {
    private final double centerX;
    private final double centerZ;
    private final double radiusX;
    private final double radiusZ;

    public EllipseBorderShape(final double centerX, final double centerZ, final double radiusX, final double radiusZ) {
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.radiusX = radiusX;
        this.radiusZ = radiusZ;
    }

    public double getCenterX() {
        return centerX;
    }

    public double getCenterZ() {
        return centerZ;
    }

    public double getRadiusX() {
        return radiusX;
    }

    public double getRadiusZ() {
        return radiusZ;
    }
}
