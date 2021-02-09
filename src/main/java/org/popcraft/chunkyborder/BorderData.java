package org.popcraft.chunkyborder;

import org.bukkit.Bukkit;
import org.popcraft.chunky.Selection;
import org.popcraft.chunky.shape.Shape;
import org.popcraft.chunky.shape.ShapeFactory;

public class BorderData {
    private String world;
    private int centerX, centerZ;
    private int radiusX, radiusZ;
    private String shape;
    private boolean wrap;
    private transient Shape border;

    public BorderData() {
    }

    public BorderData(Selection selection) {
        this.world = selection.world == null ? Bukkit.getWorlds().get(0).getName() : selection.world.getName();
        this.centerX = selection.centerX;
        this.centerZ = selection.centerZ;
        this.radiusX = selection.radiusX;
        this.radiusZ = selection.radiusZ;
        this.shape = selection.shape;
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

    public boolean isWrap() {
        return wrap;
    }

    public void setWrap(boolean wrap) {
        this.wrap = wrap;
    }

    public Shape getBorder() {
        if (border == null) {
            Selection selection = new Selection();
            selection.centerX = centerX;
            selection.centerZ = centerZ;
            selection.radiusX = radiusX;
            selection.radiusZ = radiusZ;
            selection.shape = shape;
            this.border = ShapeFactory.getShape(selection, ChunkyBorder.isChunkAligned());
            this.shape = border.name();
        }
        return border;
    }

    public void setBorder(Shape border) {
        this.border = border;
    }
}
