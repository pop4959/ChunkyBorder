package org.popcraft.chunkyborder;

import org.bukkit.Bukkit;
import org.popcraft.chunky.Selection;
import org.popcraft.chunky.shape.Shape;
import org.popcraft.chunky.shape.ShapeFactory;

public class BorderData {
    private transient Shape border;
    private String world;
    private int centerX, centerZ;
    private int radiusX, radiusZ;
    private String shape;

    public BorderData() {
    }

    public BorderData(Selection selection, boolean alignToChunk) {
        this.border = ShapeFactory.getShape(selection, alignToChunk);
        this.world = selection.world.getName();
        this.centerX = selection.centerX;
        this.centerZ = selection.centerZ;
        this.radiusX = selection.radiusX;
        this.radiusZ = selection.radiusZ;
        this.shape = selection.shape;
    }

    public void reinitializeBorder(boolean alignToChunk) {
        Selection selection = new Selection();
        selection.world = Bukkit.getWorld(world);
        selection.centerX = this.centerX;
        selection.centerZ = this.centerZ;
        selection.radiusX = this.radiusX;
        selection.radiusZ = this.radiusZ;
        selection.shape = this.shape;
        this.border = ShapeFactory.getShape(selection, alignToChunk);
    }

    public Shape getBorder() {
        return border;
    }

    public void setBorder(Shape border) {
        this.border = border;
    }

    public String getWorld() {
        return world;
    }

    public void setWorld(String world) {
        this.world = world;
    }

    public int getCenterX() {
        return centerX;
    }

    public void setCenterX(int centerX) {
        this.centerX = centerX;
    }

    public int getCenterZ() {
        return centerZ;
    }

    public void setCenterZ(int centerZ) {
        this.centerZ = centerZ;
    }

    public int getRadiusX() {
        return radiusX;
    }

    public void setRadiusX(int radiusX) {
        this.radiusX = radiusX;
    }

    public int getRadiusZ() {
        return radiusZ;
    }

    public void setRadiusZ(int radiusZ) {
        this.radiusZ = radiusZ;
    }

    public String getShape() {
        return shape;
    }

    public void setShape(String shape) {
        this.shape = shape;
    }
}
