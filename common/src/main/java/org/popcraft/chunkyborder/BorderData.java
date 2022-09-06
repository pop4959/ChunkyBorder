package org.popcraft.chunkyborder;

import org.popcraft.chunky.Selection;
import org.popcraft.chunky.shape.Shape;
import org.popcraft.chunky.shape.ShapeFactory;

import java.io.Serializable;

public class BorderData implements Serializable {
    private String world;
    private double centerX, centerZ;
    private double radiusX, radiusZ;
    private String shape;
    private String wrap;
    private transient Shape border;

    public BorderData() {
    }

    public BorderData(final Selection selection) {
        this.world = selection.world().getName();
        this.centerX = selection.centerX();
        this.centerZ = selection.centerZ();
        this.radiusX = selection.radiusX();
        this.radiusZ = selection.radiusZ();
        this.shape = selection.shape();
    }

    public String getWorld() {
        return world;
    }

    public void setWorld(final String world) {
        this.world = world;
    }

    public double getCenterX() {
        return centerX;
    }

    public void setCenterX(final double centerX) {
        this.centerX = centerX;
    }

    public double getCenterZ() {
        return centerZ;
    }

    public void setCenterZ(final double centerZ) {
        this.centerZ = centerZ;
    }

    public double getRadiusX() {
        return radiusX;
    }

    public void setRadiusX(final double radiusX) {
        this.radiusX = radiusX;
    }

    public double getRadiusZ() {
        return radiusZ;
    }

    public void setRadiusZ(final double radiusZ) {
        this.radiusZ = radiusZ;
    }

    public String getShape() {
        return shape;
    }

    public void setShape(final String shape) {
        this.shape = shape;
    }

    public String getWrap() {
        return BorderWrapType.fromString(wrap).name().toLowerCase();
    }

    public void setWrap(final String wrap) {
        this.wrap = BorderWrapType.fromString(wrap).name().toLowerCase();
    }

    public BorderWrapType getWrapType() {
        return BorderWrapType.fromString(getWrap());
    }

    public Shape getBorder() {
        if (border == null) {
            this.border = ShapeFactory.getShape(asSelection().build(), false);
            this.shape = border.name();
            this.wrap = BorderWrapType.fromString(wrap).name().toLowerCase();
        }
        return border;
    }

    public void setBorder(final Shape border) {
        this.border = border;
    }

    public Selection.Builder asSelection() {
        return Selection.builder(null, null).center(centerX, centerZ).radiusX(radiusX).radiusZ(radiusZ).shape(shape);
    }
}
