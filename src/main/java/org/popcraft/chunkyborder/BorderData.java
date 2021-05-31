package org.popcraft.chunkyborder;

import org.bukkit.Bukkit;
import org.popcraft.chunky.Selection;
import org.popcraft.chunky.platform.BukkitWorld;
import org.popcraft.chunky.platform.World;
import org.popcraft.chunky.shape.Shape;
import org.popcraft.chunky.shape.ShapeFactory;

import java.util.Optional;

public class BorderData {
    private String world;
    private double centerX, centerZ;
    private double radiusX, radiusZ;
    private String shape;
    private boolean wrap;
    private transient Shape border;

    public BorderData() {
    }

    public BorderData(Selection selection) {
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

    public void setWorld(String world) {
        this.world = world;
    }

    public double getCenterX() {
        return centerX;
    }

    public void setCenterX(double centerX) {
        this.centerX = centerX;
    }

    public double getCenterZ() {
        return centerZ;
    }

    public void setCenterZ(double centerZ) {
        this.centerZ = centerZ;
    }

    public double getRadiusX() {
        return radiusX;
    }

    public void setRadiusX(double radiusX) {
        this.radiusX = radiusX;
    }

    public double getRadiusZ() {
        return radiusZ;
    }

    public void setRadiusZ(double radiusZ) {
        this.radiusZ = radiusZ;
    }

    public String getShape() {
        return shape;
    }

    public void setShape(String shape) {
        this.shape = shape;
    }

    public boolean isWrap() {
        return wrap;
    }

    public void setWrap(boolean wrap) {
        this.wrap = wrap;
    }

    public Shape getBorder() {
        if (border == null) {
            this.border = ShapeFactory.getShape(asSelection().build(), ChunkyBorder.isChunkAligned());
            this.shape = border.name();
        }
        return border;
    }

    public void setBorder(Shape border) {
        this.border = border;
    }

    public Selection.Builder asSelection() {
        final World borderWorld = Optional.ofNullable(Bukkit.getWorld(world)).map(BukkitWorld::new).orElse(null);
        return Selection.builder(borderWorld).center(centerX, centerZ).radiusX(radiusX).radiusZ(radiusZ).shape(shape);
    }
}
